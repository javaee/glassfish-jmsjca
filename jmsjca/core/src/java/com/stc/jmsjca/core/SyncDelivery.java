/*
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the "License").  You may not use this file except
 * in compliance with the License.
 *
 * You can obtain a copy of the license at
 * https://glassfish.dev.java.net/public/CDDLv1.0.html.
 * See the License for the specific language governing
 * permissions and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL
 * HEADER in each file and include the License file at
 * https://glassfish.dev.java.net/public/CDDLv1.0.html.
 * If applicable add the following below this CDDL HEADER,
 * with the fields enclosed by brackets "[]" replaced with
 * your own identifying information: Portions Copyright
 * [year] [name of copyright owner]
 */
/*
 * $RCSfile: SyncDelivery.java,v $
 * $Revision: 1.1.1.2 $
 * $Date: 2007-01-21 07:52:44 $
 *
 * Copyright 2003-2007 Sun Microsystems, Inc. All Rights Reserved.  
 */

package com.stc.jmsjca.core;

import com.stc.jmsjca.util.Logger;
import com.stc.jmsjca.util.Semaphore;

import javax.jms.BytesMessage;
import javax.jms.JMSException;
import javax.jms.MapMessage;
import javax.jms.Message;
import javax.jms.ObjectMessage;
import javax.jms.QueueSession;
import javax.jms.StreamMessage;
import javax.jms.TextMessage;
import javax.jms.TopicSession;
import javax.resource.spi.endpoint.MessageEndpoint;
import javax.transaction.Status;
import javax.transaction.Transaction;
import javax.transaction.xa.XAResource;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * This delivery class is used for those JMS servers that do not support
 * asynchronous modes with XA. The problem with those servers is that an XA
 * transaction needs to be started before the message is received, which is
 * impossible with asynchronous message listeners. As a workaround, this
 * delivery class uses a synchronous receive() call in an infinite loop:
 * <pre>
 * xastart() 
 * m = receive(timeout) 
 * if (m != null) process(m) 
 * xaend() 
 * commit()
 * </pre>
 * 
 * <p>There can be multiple of these loops active at the same time. Ofcourse this
 * will only work for queues, and not for topics. The object that deals with
 * this loop is called a worker.
 * 
 * <p>Threading model: each worker has its own dedicated thread. These threads are
 * created by the RAR; they do not share the appserver's threadpool. If an
 * activation is setup with 32 MDBs, there will be 32 workers.
 * 
 * <p>Workers can exit in one of two ways: an exception during polling, or an
 * orderly shutdown.
 * 
 * <p>Synchronization:
 * <ul>
 * <li>there is a global isStopped flag; this flag is
 * synchronized (A)</li> 
 * <li>start and stop are synchronized so that if they can be
 * called from multiple threads (B)</li> 
 * <li>isStopped is synchronized separately from
 * start/stop: lock(A) or lock(B)->lock(A); this should not happen:
 * lock(A)->lock(B)</li> 
 * <li>each Worker has a flag if it is running; this flag is
 * synchronized (D)</li>
 * </ul>
 * </pre>
 * 
 * @author fkieviet
 */
public class SyncDelivery extends Delivery {
    /**
     * receive-timeout
     */
    public static final int TIMEOUT = 3000;
    /**
     * batch will be "truncated" if no messages were received for x milliseconds
     */
    public static final int TIMEOUTBATCH = 100;
    
    private static Logger sLog = Logger.getLogger(SyncDelivery.class);
    private static Logger sContextEnter = Logger.getLogger("com.stc.EnterContext");
    private static Logger sContextExit = Logger.getLogger("com.stc.ExitContext");
    private javax.jms.Connection mConnection;
    private String mContextName;
    private int mNThreads;
    private List mWorkers = new ArrayList();
    private boolean mIsStopped = true;
    private Object mIsStoppedLock = new Object();
    private int mBatchSize;
    private boolean mHoldUntilAck;
    private TxMgr mTxMgr;
    
    /**
     * Constructor 
     * 
     * @param a Activation
     * @param stats DeliveryStats
     */
    public SyncDelivery(Activation a, DeliveryStats stats) {
        super(a, stats);
        
        if (a.getActivationSpec().getDeliveryConcurrencyMode() == 
            RAJMSActivationSpec.DELIVERYCONCURRENCY_SERIAL) {
            mNThreads = 1;
        } else if (a.getActivationSpec().getDestinationType().equals(
                javax.jms.Topic.class.getName())) {            
            mNThreads = 1;
        } else {
            mNThreads = 
                a.getActivationSpec().getEndpointPoolMaxSize().intValue();            
        }
        
        if (sLog.isDebugEnabled()) {
            sLog.debug("number of endpoints specified to be " + mNThreads);
        }
    
        // Batch
        mBatchSize = a.getActivationSpec().getBatchSize();
        
        // HUA mode
        String huaMode = a.getActivationSpec().getHoldUntilAck();
        if (huaMode != null && huaMode.length() > 0) {
            if ("TRUE".equalsIgnoreCase(huaMode) || "1".equals(huaMode)) {
                mHoldUntilAck = true;
            }
        }

        // Get TxMgr
        if ((mBatchSize > 1 || mHoldUntilAck) && isXA()) {
            try {
                mTxMgr = getTxMgr();
            } catch (Exception e) {
                if (mHoldUntilAck) {
                    throw new RuntimeException("Could not obtain TxMgr which is crucial for HUA mode: " + e, e);
                }
                sLog.warn("TxMgr could not be obtained: " + e, e);
            }
        }
    }

    /**
     * @see com.stc.jmsjca.core.Delivery#deactivate()
     */
    public void deactivate() {
        stop();
    }
    
    /**
     * @see com.stc.jmsjca.core.Delivery#start()
     */
    public synchronized void start() throws JMSException {
        // Guard against redundant calls to start
        synchronized (mIsStoppedLock) {
            if (!mIsStopped) {
                return;
            }
            mIsStopped = false;
        }
        
        if (mConnection != null) {
            throw new JMSException("Logic fault: connection not null");
        }
        try {
            RAJMSObjectFactory o = mActivation.getObjectFactory();
            javax.jms.ConnectionFactory fact = o.createConnectionFactory(
                XConnectionRequestInfo.guessDomain(mActivation.isXA(), mActivation.isTopic()),
                mActivation.getRA(),
                mActivation.getActivationSpec(),
                null,
                null);
            mConnection = o.createConnection(
                fact,
                XConnectionRequestInfo.guessDomain(mActivation.isXA(), mActivation.isTopic()),
                mActivation.getActivationSpec(),
                mActivation.getRA(),
                mActivation.getUserName() == null ? mActivation.getRA().getUserName() : mActivation.getUserName(),
                mActivation.getPassword() == null ? mActivation.getRA().getPassword() : mActivation.getPassword());
            o.setClientID(mConnection, 
                mActivation.isTopic(), 
                mActivation.getActivationSpec(), 
                mActivation.getRA());
            mContextName = getActivation().getActivationSpec().getContextName();
            mConnection.start();
            
            
            // Create sync workers
            try {
                for (int i = 0; i < mNThreads; i++) {
                    SyncWorker w = new SyncWorker("JMSJCA sync #" + i + "("
                        + mActivation.getActivationSpec().getDestination() + ")");
                    w.init();
                    // Do not start worker here yet
                    mWorkers.add(w);
                }
            } catch (JMSException e) {
                throw e;
            }
    
            // Start sync workers
            for (Iterator iter = mWorkers.iterator(); iter.hasNext();) {
                SyncWorker w = (SyncWorker) iter.next();
                w.start();
            }
        } catch (JMSException e) {
            stop();
            throw e;
        }
    }
    
    /**
     * Stops message delivery and will not return until all work containers have 
     * exited, and until all threads have seized.
     */
    private synchronized void stop() {
        // Guard against redundant calls to stop
        synchronized (mIsStoppedLock) {
            if (mIsStopped) {
                return;
            }
            mIsStopped = true;
        }

        if (sLog.isDebugEnabled()) {
            sLog.debug("Delivery.stop() -- begin");
        }

        // Stop connection: will cause receive() to return null
        try {
            if (mConnection != null) {
                mConnection.stop();
            }
        } catch (Exception ex) {
            sLog.warn("Unexpected exception stopping JMS connection: " + ex, ex);
        }

        // Wait until all workers that were processing a msg are finished
        long tlog = System.currentTimeMillis() + DESTROY_LOG_INTERVAL_MS;
        for (;;) {
            // Try to destroy all WorkContainers; count the number of failures
            if (sLog.isDebugEnabled()) {
                sLog.debug("Trying to destroy all Workers");
            }
            for (Iterator it = mWorkers.iterator(); it.hasNext();/*-*/) {
                SyncWorker w = (SyncWorker) it.next();
                if (!w.isRunning()) {
                    w.close();
                    it.remove();
                }
            }

            // Wait if not all were destroyed
            if (mWorkers.isEmpty()) {
                if (sLog.isDebugEnabled()) {
                    sLog.debug("All work containers were destroyed successfully");
                }
                break;
            } else {
                if (System.currentTimeMillis() > tlog) {
                    sLog.info("Stopping connector; waiting for work containers to "
                        + "exit; there are " + mWorkers.size()
                        + " containers that are still active; activation=" + mActivation);
                    tlog = System.currentTimeMillis() + DESTROY_LOG_INTERVAL_MS;
                }

                // Wait a bit
                if (sLog.isDebugEnabled()) {
                    sLog.debug(mWorkers.size() + " Worker(s) were (was) not destroyed... waiting");
                }
                try {
                    Thread.sleep(DESTROY_RETRY_INTERVAL_MS);
                } catch (Exception ex) {
                    // ignore
                }
            }
        }

        // All threads have exited now... close JMS resources
        try {
            if (mConnection != null) {
                mConnection.close();
            }
        } catch (Exception ex) {
            sLog.warn("Unexpected exception closing JMS Connection: " + ex, ex);
        }
        mConnection = null;

        if (sLog.isDebugEnabled()) {
            sLog.debug("Delivery.stop() -- complete");
        }
    }
    
    private abstract class Coordinator extends AckHandler {
        public abstract void setRollbackOnly();

        public abstract void setRollbackOnly(Exception e);
        
        public abstract void ack(boolean isRollbackOnly, Message m) throws JMSException;
        
        public abstract boolean isRollbackOnly();

        public abstract void msgDelivered(boolean wasDelivered);

        public abstract void waitForAcks() throws InterruptedException;

        public abstract boolean needsToDiscardEndpoint();
        
        public abstract void setNeedsToDiscardEndpoint();

        public abstract int getNMsgsDelivered();
    }
    
    private class NonHUACoordinator extends Coordinator {
        private boolean mIsRollbackOnly;
        private boolean mNeedsToDiscardEndpoint;
        private int mNMsgsDelivered;
        
        public void setRollbackOnly() {
            mIsRollbackOnly = true;
        }
        
        public void setRollbackOnly(Exception e) {
            if (e != null) {
                setRollbackOnly();
                mNeedsToDiscardEndpoint = true;
            }
        }
        
        public void ack(boolean isRollbackOnly, Message m) throws JMSException {
        }
        
        public boolean isRollbackOnly() {
            return mIsRollbackOnly;
        }
        
        public void msgDelivered(Exception e) {
        }
        
        public void msgDelivered(boolean wasDelivered) {
            if (wasDelivered) {
                mNMsgsDelivered++;
            }
        }
        
        public void waitForAcks() throws InterruptedException {
        }

        public boolean needsToDiscardEndpoint() {
            return mNeedsToDiscardEndpoint;
        }

        public int getNMsgsDelivered() {
            return mNMsgsDelivered;
        }

        public void setNeedsToDiscardEndpoint() {
            mNeedsToDiscardEndpoint = true;
            
        }
    }

    private class HUACoordinator extends Coordinator {
        private Semaphore mSemaphore = new Semaphore(0);
        private int mNAcksToExpect;
        private boolean mIsRollbackOnly;
        private boolean mNeedsToDiscardEndpoint;
        private int mNMsgsDelivered;
        
        public synchronized void setRollbackOnly() {
            mIsRollbackOnly = true;
        }
        
        public void setRollbackOnly(Exception e) {
            if (e != null) {
                setRollbackOnly();
                mNeedsToDiscardEndpoint = true;
            }
        }
        
        public void ack(boolean isRollbackOnly, Message m) throws JMSException {
            if (isRollbackOnly) {
                setRollbackOnly();
            }
            mSemaphore.release();
        }
        
        public synchronized boolean isRollbackOnly() {
            return mIsRollbackOnly;
        }
        
        public void msgDelivered(boolean wasDelivered) {
            if (wasDelivered) {
                mNAcksToExpect++;
                mNMsgsDelivered++;
            }
        }
        
        public void waitForAcks() throws InterruptedException {
            done: for (int i = 0; i < mNAcksToExpect; i++) {
                for (;;) {
                    // TODO: find better way to wait both on stop signal and semaphore
                    if (mSemaphore.attempt(500)) {
                        break;
                    }
                    if (isStopped()) {
                        setRollbackOnly();
                        break done;
                    }
                }
            }
        }

        public boolean needsToDiscardEndpoint() {
            return mNeedsToDiscardEndpoint;
        }

        public int getNMsgsDelivered() {
            return mNMsgsDelivered;
        }

        public void setNeedsToDiscardEndpoint() {
            mNeedsToDiscardEndpoint = true;
        }
    }
    
    private Coordinator newCoord() {
        if (mHoldUntilAck) { 
            return new HUACoordinator();
        } else {
            return new NonHUACoordinator();
        }
    }
    
    private class SyncWorker extends Thread {
        private javax.jms.MessageConsumer mCons;
        private javax.jms.Session mSess;
        private XAResource mXA;
        private MDB mMDB;
        private MessageEndpoint mEndpoint;
        private boolean mRunning;
        private ConnectionForMove mMessageMoveConnection;
        
        /**
         * Constructor
         * 
         * @param name threadname
         */
        public SyncWorker(String name) {
            super(name);
        }
        
        /**
         * Sets up the objects needed for execution, but not the endpoint since this
         * may not be ready for allocation yet.
         * 
         * @throws Exception
         */
        public void init() throws JMSException {
            RAJMSObjectFactory o = mActivation.getObjectFactory();
            mSess = o.createSession(
                mConnection,
                mActivation.isXA(),
                mActivation.isTopic() ? TopicSession.class : QueueSession.class,
                mActivation.getRA(),
                mActivation.getActivationSpec(),
                true,
                javax.jms.Session.SESSION_TRANSACTED);
            javax.jms.Destination dest = o.createDestination(
                mSess,
                mActivation.isXA(),
                mActivation.isTopic(),
                mActivation.getActivationSpec(),
                mActivation.getRA(),
                mActivation.getActivationSpec().getDestination());
            mCons = o.createMessageConsumer(
                mSess,
                mActivation.isXA(),
                mActivation.isTopic(),
                dest,
                mActivation.getActivationSpec(),
                mActivation.getRA());
            mXA = mActivation.getObjectFactory().getXAResource(
                mActivation.isXA(), mSess);
            mMDB = new Delivery.MDB(mXA);
            mMessageMoveConnection = createConnectionForMove();
            mMessageMoveConnection.setDelayedCommit();
        }
        
        /**
         * Closes the allocated resources. Must be called after the thread has ended.
         */
        private void close() {
            if (mCons != null) {
                try {
                    mCons.close();
                } catch (JMSException e) {
                    sLog.warn("Non-critical failure to close a message consumer: " + e, e);
                }
                mCons = null;
            }
            
            mMessageMoveConnection.destroy();
            
            release(mEndpoint);
            mEndpoint = null;
        }
        
        /**
         * @see java.lang.Thread#start()
         */
        public void start() {
            // Mark as running
            synchronized (this) {
                mRunning = true;
            }
            
            // Start the thread
            super.start();
        }
        
        private void runOnceStdXA(Coordinator coord) throws Exception {
            DeliveryResults result = new DeliveryResults();

            // XA Mode
            beforeDelivery(result, mEndpoint);
            if (result.getBeforeDeliveryFailed()) {
                throw result.getException();
            }
            
            // The MDB may move the transaction to a different thread
            Transaction tx = null;
            if (mHoldUntilAck) {
                tx = getTransaction(true);
            }
            
            Message m = mCons.receive(TIMEOUT);
            if (m != null) {
                m = mHoldUntilAck ? wrapMsg(m, coord, -1) : m;
                deliverToEndpoint(result, mMessageMoveConnection, mEndpoint, m);
                coord.msgDelivered(result.getOnMessageSucceeded());
                coord.setRollbackOnly(result.getException());
            }
            
            coord.waitForAcks();
            
            if (!result.getShouldNotCallAfterDelivery()) {
                // If the transaction was moved to a different thread, take it back
                if (mHoldUntilAck && getTransaction(true) == null) {
                    getTxMgr().getTransactionManager().resume(tx);
                }

                if (mHoldUntilAck && coord.isRollbackOnly()) {
                    getTransaction(true).setRollbackOnly();
                }
            }
            
            afterDelivery(result, mMessageMoveConnection, mEndpoint, mMDB);
            if (result.getAfterDeliveryFailed()) {
                throw result.getException();
            }

            if (result.getShouldDiscardEndpoint()) {
                coord.setNeedsToDiscardEndpoint();
            }
        }
        
        private boolean mTxFailureLoggedOnce;
        
        private Transaction getTransaction(boolean mustSucceed) {
            if (mTxMgr != null) {
                try {
                    return mTxMgr.getTransactionManager().getTransaction();
                } catch (Exception e) {
                    if (mustSucceed) {
                        throw new RuntimeException("Failed to obtain handle to transaction: " + e, e);
                    }
                    synchronized (mIsStoppedLock) {
                        if (!mTxFailureLoggedOnce) {
                            mTxFailureLoggedOnce = true;
                            sLog.error("Failed to obtain handle to transaction: " + e, e);
                        }
                    }
                }
            }
            return null;
        }
        
        private Message wrapMsg(Message toCopy, AckHandler ack, int iBatch) throws JMSException {
            WMessageIn ret;
            
            // Check for multiple interfaces
            int nItf = 0;

            if (toCopy instanceof TextMessage) {
                nItf++;
                ret = new WTextMessageIn((TextMessage) toCopy, ack, iBatch);
            } else if (toCopy instanceof BytesMessage) {
                nItf++;
                ret = new WBytesMessageIn((BytesMessage) toCopy, ack, iBatch);
            } else if (toCopy instanceof MapMessage) {
                nItf++;
                ret = new WMapMessageIn((MapMessage) toCopy, ack, iBatch);
            } else if (toCopy instanceof ObjectMessage) {
                nItf++;
                ret = new WObjectMessageIn((ObjectMessage) toCopy, ack, iBatch);
            } else if (toCopy instanceof StreamMessage) {
                nItf++;
                ret = new WStreamMessageIn((StreamMessage) toCopy, ack, iBatch);
            } else {
                nItf++;
                ret = new WMessageIn(toCopy, ack, iBatch);
            }
            
            if (nItf > 1) {
                throw new JMSException("Cannot determine message type: the message " 
                    + "implements multiple interfaces.");
            }
            
            ret.setBatchSize(mBatchSize);

            return ret;
        }
        
        private void runOnceBatchXA(Coordinator coord) throws Exception {
            // XA Mode
            DeliveryResults lastResult = new DeliveryResults();
            beforeDelivery(lastResult, mEndpoint);
            if (lastResult.getShouldDiscardEndpoint()) {
                throw lastResult.getException();
            }

            Transaction tx = getTransaction(mHoldUntilAck);

            boolean msgsWereReceived = false;
            for (int i = 0; i < mBatchSize; i++) {
                Message m = mCons.receive(i == 0 ? TIMEOUT : TIMEOUTBATCH);
                if (m == null) {
                    break;
                } else {
                    m = mHoldUntilAck ? wrapMsg(m, coord, coord.getNMsgsDelivered()) : m;
                    lastResult.resetDeliveryState();
                    deliverToEndpoint(lastResult, mMessageMoveConnection, mEndpoint, m);
                    msgsWereReceived = true;
                    coord.msgDelivered(lastResult.getOnMessageSucceeded());
                    coord.setRollbackOnly(lastResult.getException());
                    
                    // If rollback-only, don't continue processing remainder of batch 
                    if (coord.isRollbackOnly() || (tx != null && tx.getStatus() == Status.STATUS_MARKED_ROLLBACK)) {
                        break;
                    }
                }
            }
        
            if (coord.getNMsgsDelivered() > 0) {
                // Msgs were delivered; signal end of batch
                Message m = new EndOfBatchMessage();
                m = mHoldUntilAck ? wrapMsg(m, coord, coord.getNMsgsDelivered()) : m;
                lastResult.resetDeliveryState();
                deliverToEndpoint(lastResult, mMessageMoveConnection, mEndpoint, m);
                coord.msgDelivered(lastResult.getOnMessageSucceeded());
                coord.setRollbackOnly(lastResult.getException());
                
                // Wait until all msgs were acknowledged
                if (mHoldUntilAck) {
                    coord.waitForAcks();
                }
            }
            
            if (msgsWereReceived && !lastResult.getShouldNotCallAfterDelivery()) {
                // If the transaction was moved to a different thread, take it back
                if (mHoldUntilAck && getTransaction(true) == null) {
                    getTxMgr().getTransactionManager().resume(tx);
                }

                // Assure rollback if necessary before committing the transaction
                if (mHoldUntilAck && coord.isRollbackOnly()) {
                    getTransaction(true).setRollbackOnly();
                }
            }

            // End transaction
            afterDelivery(lastResult, mMessageMoveConnection, mEndpoint, mMDB);
            if (lastResult.getShouldDiscardEndpoint()) {
                throw lastResult.getException();
            }
            
            if (lastResult.getShouldDiscardEndpoint()) {
                coord.setNeedsToDiscardEndpoint();
            }
        }
        
        private void runOnceBatchNoXA(Coordinator coord) throws Exception {
            // Transacted mode

            boolean msgsWereDelivered = false;
            DeliveryResults lastResult = new DeliveryResults();

            // Read and deliver batch
            for (int i = 0; i < mBatchSize; i++) {
                Message m = mCons.receive(i == 0 ? TIMEOUT : TIMEOUTBATCH);
                if (m == null) {
                    break;
                } else {
                    msgsWereDelivered = true;
                    m = mHoldUntilAck ? wrapMsg(m, coord, coord.getNMsgsDelivered()) : m;
                    lastResult.reset();
                    deliverToEndpoint(lastResult, mMessageMoveConnection, mEndpoint, m);
                    coord.msgDelivered(lastResult.getOnMessageSucceeded());
                    coord.setRollbackOnly(lastResult.getException());
                    
                    if (coord.isRollbackOnly()) {
                        // Do not get more messages in this batch
                        break;
                    }
                }
            }

            // Commit/rollback and end-of-batch notification
            if (msgsWereDelivered) {
                // Msgs were delivered; signal end of batch
                Message m = new EndOfBatchMessage();
                m = mHoldUntilAck ? wrapMsg(m, coord, coord.getNMsgsDelivered()) : m;
                lastResult.reset();
                deliverToEndpoint(lastResult, mMessageMoveConnection, mEndpoint, m);
                coord.msgDelivered(lastResult.getOnMessageSucceeded());
                coord.setRollbackOnly(lastResult.getException());
                
                coord.waitForAcks();
                
                if (!coord.isRollbackOnly()) {
                    mMessageMoveConnection.nonXACommit(true);
                    mSess.commit();
                } else {
                    mMessageMoveConnection.nonXACommit(false);
                    mSess.rollback();
                }
            }
            
            if (lastResult.getShouldDiscardEndpoint()) {
                coord.setNeedsToDiscardEndpoint();
            }
        }

        private void runOnceStdNoXA(Coordinator coord) throws Exception {
            // Transacted mode
            
            // Receive
            Message m = mCons.receive(TIMEOUT);
            
            if (m != null) {
                // Optionally wrap for ack() call
                m = mHoldUntilAck ? wrapMsg(m, coord, -1) : m;
                
                // Deliver
                DeliveryResults result = new DeliveryResults(); 
                deliverToEndpoint(result, mMessageMoveConnection, mEndpoint, m);

                // Increment counter if msg was delivered
                coord.msgDelivered(result.getOnMessageSucceeded());
                coord.setRollbackOnly(result.getException());
                
                // Wait for ack() to be called if applicable
                coord.waitForAcks();
                
                // Commit/rollback
                if (!coord.isRollbackOnly()) {
                    mMessageMoveConnection.nonXACommit(true);
                    mSess.commit();
                } else {
                    mMessageMoveConnection.nonXACommit(false);
                    mSess.rollback();
                }

                if (result.getShouldDiscardEndpoint()) {
                    coord.setNeedsToDiscardEndpoint();
                }
            }
        }
        
        /**
         * Called by a separate Thread: polls the receiver until it is time to exit
         * or if an exception occurs.
         * 
         * @see java.lang.Runnable#run()
         */
        public void run() {
            if (mContextName != null) {
                sContextEnter.info(mContextName);
            }
            
            for (;;) {
                try {
                    // Get EndPoint
                    if (mEndpoint == null) {
                        mEndpoint = createMessageEndpoint(mXA);
                    }
                    if (mEndpoint == null) {
                        throw new Exception("No endpoint created; RA shutting down?");
                    }

                    // Run single receive-onMessage() loop
                    Coordinator coord = newCoord();
                    if (mXA != null) {
                        if (mBatchSize > 1) {
                            runOnceBatchXA(coord);
                        } else {
                            runOnceStdXA(coord);
                        }
                    } else {
                        if (mBatchSize > 1) {
                            runOnceBatchNoXA(coord);
                        } else {
                            runOnceStdNoXA(coord);
                        }
                    }
                    
                    // Discard MDB if it threw an exception (required for WebLogic)
                    if (coord.needsToDiscardEndpoint()) {
                        release(mEndpoint);
                        mEndpoint = null;
                    }
                    
                    // Need to stop?
                    synchronized (mIsStoppedLock) {
                        if (mIsStopped) {
                            break;
                        }
                    }
                } catch (Exception ex) {
                    mActivation.distress(ex);
                    break;
                } catch (Throwable ex) {
                    mActivation.distress(new Exception("Unexpected Throwable: " + ex, ex));
                    break;
                }
            }

            close();

            if (mContextName != null) {
                sContextExit.info(mContextName);
            }

            synchronized (this) {
                mRunning = false;
            }
        }
        
        /**
         * Indicates if the worker is running
         * 
         * @return boolean
         */
        public synchronized boolean isRunning() {
            return mRunning;
        }
    }

    /**
     * Indicates if this object has been stopped
     * 
     * @return true if the state is stopped
     */
    public boolean isStopped() {
        synchronized (mIsStoppedLock) {
            return mIsStopped;
        }
    }

    /**
     * @see com.stc.jmsjca.core.Delivery#getConfiguredEndpoints()
     */
    public int getConfiguredEndpoints() {
        return mNThreads;
    }
}
