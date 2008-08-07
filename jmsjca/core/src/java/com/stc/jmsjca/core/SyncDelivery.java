/*
 * The contents of this file are subject to the terms of the Common Development and Distribution License
 * (the "License"). You may not use this file except in compliance with the License.
 *
 * You can obtain a copy of the license at https://glassfish.dev.java.net/public/CDDLv1.0.html.
 * See the License for the specific language governing permissions and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL HEADER in each file and include the License file at
 * https://glassfish.dev.java.net/public/CDDLv1.0.html. If applicable add the following below this
 * CDDL HEADER, with the fields enclosed by brackets "[]" replaced with your own identifying
 * information: Portions Copyright [year] [name of copyright owner]
 */
/*
 * Copyright 2003-2007 Sun Microsystems, Inc. All Rights Reserved.
 */

package com.stc.jmsjca.core;

import com.stc.jmsjca.localization.LocalizedString;
import com.stc.jmsjca.localization.Localizer;
import com.stc.jmsjca.util.Exc;
import com.stc.jmsjca.util.Logger;
import com.stc.jmsjca.util.Semaphore;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.QueueSession;
import javax.jms.TopicSession;
import javax.resource.spi.endpoint.MessageEndpoint;
import javax.transaction.Status;
import javax.transaction.Transaction;
import javax.transaction.xa.XAResource;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;

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
 * Error handling:
 * for() {
 *    beforeDelivery()
 *    m = receive()
 *    deliver()
 *    afterDelivery()
 * }
 *
 * beforeDelivery()
 *     fails: rethrow
 * m = receive()
 *     fails: rethrow
 * deliver()
 *     fails: setRBO
 *         fails: rethrow
 * afterDelivery()
 *    fails: rethrow
 * 
 * TODO: Looks like a msg is only wrapped when HUA mode is on; this is a bug
 * 
 * @author fkieviet
 */
public class SyncDelivery extends Delivery {
    /**
     * receive-timeout
     */
    private int TIMEOUT = 10000; // will be overridden in constructor
    /**
     * batch will be "truncated" if no messages were received for x milliseconds
     */
    public static final int TIMEOUTBATCH = 100;
    
    private static Logger sLog = Logger.getLogger(SyncDelivery.class);
    private static Logger sContextEnter = Logger.getLogger("com.stc.EnterContext");
    private static Logger sContextExit = Logger.getLogger("com.stc.ExitContext");
    private javax.jms.Connection mConnection;
    private LocalizedString mContextName;
    private int mNThreads;
    private List mWorkers = new ArrayList();
    private boolean mIsStopped = true;
    private Object mIsStoppedLock = new Object();
    
    private static final Localizer LOCALE = Localizer.get();

    /**
     * Constructor 
     * 
     * @param a Activation
     * @param stats DeliveryStats
     * @throws Exception on failure
     */
    public SyncDelivery(Activation a, DeliveryStats stats) throws Exception {
        super(a, stats);
        try{
            Properties p = new Properties();
            a.getObjectFactory().getProperties(p, a.getRA(), a.getActivationSpec(),null, null);
            String receivetimeout = p.getProperty(Options.In.RECEIVE_TIMEOUT);
            TIMEOUT = Integer.parseInt(receivetimeout);
        }catch(Exception ignore){
        }
        
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
            sLog.debug("RECEIVE TIMEOUT of endpoints specified to be " + TIMEOUT);
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
            throw Exc.jmsExc(LOCALE.x("E148: Logic fault: connection not null"));
        }
        try {
            RAJMSObjectFactory o = mActivation.getObjectFactory();
            javax.jms.ConnectionFactory fact = o.createConnectionFactory(
                getDomain(),
                mActivation.getRA(),
                mActivation.getActivationSpec(),
                null,
                null);
            mConnection = o.createConnection(
                fact,
                getDomain(),
                mActivation.getActivationSpec(),
                mActivation.getRA(),
                mActivation.getUserName() == null 
                ? mActivation.getRA().getUserName() : mActivation.getUserName(),
                mActivation.getPassword() == null 
                ? mActivation.getRA().getClearTextPassword() : mActivation.getPassword());
            o.setClientID(mConnection, 
                mActivation.isTopic(), 
                mActivation.getActivationSpec(), 
                mActivation.getRA());
            mContextName = LocalizedString.valueOf(getActivation().getActivationSpec().getContextName()); 
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
            sLog.warn(LOCALE.x("E058: Unexpected exception stopping JMS connection: {0}", ex), ex);
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
                    sLog.info(LOCALE.x("E059: Stopping message delivery; waiting for work containers to "
                        + "finish processing messages; there are {0} containers " 
                        + "that are still active; activation=[{1}].", 
                        Integer.toString(mWorkers.size()), mActivation));
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
            sLog.warn(LOCALE.x("E060: Unexpected exception closing JMS Connection: {0}", ex), ex);
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
    
    /**
     * @return TopicSession or QueueSession class, can be overriden for TopicToQueue
     *  delivery wich requires a unified session
     */
    protected Class getSessionClass() {
        return mActivation.isTopic() ? TopicSession.class : QueueSession.class;        
    }
    
    /**
     * @return one of XConnectionRequestInfo.DOMAIN_XXX; can be overridden for TopicToQueue
     *  delivery which requires a unified connection 
     */
    protected int getDomain() {
        return XConnectionRequestInfo.guessDomain(mActivation.isCMT() && !mActivation.isXAEmulated(), 
            mActivation.isTopic());        
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
                mActivation.isCMT() && !mActivation.isXAEmulated(),
                getSessionClass(),
                mActivation.getRA(),
                mActivation.getActivationSpec(),
                true,
                javax.jms.Session.SESSION_TRANSACTED);
            javax.jms.Destination dest = o.createDestination(
                mSess,
                mActivation.isCMT() && !mActivation.isXAEmulated(),
                mActivation.isTopic(),
                mActivation.getActivationSpec(),
                null,
                mActivation.getRA(),
                mActivation.getActivationSpec().getDestination());
            mCons = o.createMessageConsumer(
                mSess,
                mActivation.isCMT() && !mActivation.isXAEmulated(),
                mActivation.isTopic(),
                dest,
                mActivation.getActivationSpec(),
                mActivation.getRA());
            if (mActivation.isCMT()) {
                if (mActivation.isXAEmulated()) {
                    mXA = new PseudoXAResource(mSess);
                } else {
                    mXA = mActivation.getObjectFactory().getXAResource(true, mSess);
                }
            }
            mMDB = new Delivery.MDB(mXA);
            mMessageMoveConnection = createConnectionForMove();
            mMessageMoveConnection.setDelayedCommit();
        }
        
        /**
         * Closes the allocated resources. Must be called after the thread has ended.
         */
        private void close() {
            if (mSess != null) {
                try {
                    mSess.close();
                } catch (JMSException e) {
                    sLog.warn(LOCALE.x("E061: Non-critical failure to close a message consumer: {0}", e), e);
                }
                mSess = null;
            }
            if (mCons != null) {
                try {
                    mCons.close();
                } catch (JMSException e) {
                    sLog.warn(LOCALE.x("E061: Non-critical failure to close a message consumer: {0}", e), e);
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
            beforeDelivery(result, mEndpoint, true);
            
            // The MDB may move the transaction to a different thread
            Transaction tx = null;
            if (mHoldUntilAck) {
                tx = getTransaction(true);
            }
            
            Message m = mCons.receive(TIMEOUT);
            if (m != null) {
                if (mHoldUntilAck) {
                    m = wrapMsg(m).setBatchSize(mBatchSize, coord, -1);
                }
                deliverToEndpoint(result, mMessageMoveConnection, mEndpoint, m, true);
                coord.msgDelivered(result.getOnMessageSucceeded());
                coord.setRollbackOnly(result.getException());
            }
            
            coord.waitForAcks();
            
            if (!result.getBeforeDeliveryFailed()) {
                // If the transaction was moved to a different thread, take it back
                if (mHoldUntilAck && getTransaction(true) == null) {
                    getTxMgr().resume(tx);
                }

                if (mHoldUntilAck && coord.isRollbackOnly()) {
                    result.setRollbackOnly(true);
                }
            }
            
            afterDelivery(result, mMessageMoveConnection, mEndpoint, mMDB, true);
        }
        
        private void runOnceBatchXA(Coordinator coord) throws Exception {
            // XA Mode
            DeliveryResults lastResult = new DeliveryResults();
            beforeDelivery(lastResult, mEndpoint, true);

            Transaction tx = getTransaction(mHoldUntilAck);

            for (int i = 0; i < mBatchSize; i++) {
                Message m = mCons.receive(i == 0 ? TIMEOUT : TIMEOUTBATCH);                
                if (m == null) {
                    break;
                } else {
                    if (mHoldUntilAck) {
                        m = wrapMsg(m).setBatchSize(mBatchSize, coord, coord.getNMsgsDelivered());
                    }
                    lastResult.resetDeliveryState();
                    deliverToEndpoint(lastResult, mMessageMoveConnection, mEndpoint, m, true);
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
                if (mHoldUntilAck) {
                    m = wrapMsg(m).setBatchSize(mBatchSize, coord, coord.getNMsgsDelivered());
                }
                lastResult.resetDeliveryState();
                deliverToEndpoint(lastResult, mMessageMoveConnection, mEndpoint, m, true);
                coord.msgDelivered(lastResult.getOnMessageSucceeded());
                coord.setRollbackOnly(lastResult.getException());
                
                // Wait until all msgs were acknowledged
                if (mHoldUntilAck) {
                    coord.waitForAcks();
                }
            }
            
            // If the transaction was moved to a different thread, take it back
            if (mHoldUntilAck && getTransaction(true) == null) {
                getTxMgr().resume(tx);
            }

            // Assure rollback if necessary before committing the transaction
            if (mHoldUntilAck && coord.isRollbackOnly()) {
                txSetRollbackOnly(lastResult, true);
            }
            
            // End transaction
            afterDelivery(lastResult, mMessageMoveConnection, mEndpoint, mMDB, true);
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
                    if (mHoldUntilAck) {
                        m = wrapMsg(m).setBatchSize(mBatchSize, coord, coord.getNMsgsDelivered());
                    }
                    lastResult.reset();
                    deliverToEndpoint(lastResult, mMessageMoveConnection, mEndpoint, m, true);
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
                if (mHoldUntilAck) {
                    m = wrapMsg(m).setBatchSize(mBatchSize, coord, coord.getNMsgsDelivered());
                }
                lastResult.reset();
                deliverToEndpoint(lastResult, mMessageMoveConnection, mEndpoint, m, true);
                coord.msgDelivered(lastResult.getOnMessageSucceeded());
                coord.setRollbackOnly(lastResult.getException());
                
                coord.waitForAcks();
                
                if (coord.isRollbackOnly()) {
                    lastResult.setRollbackOnly(true);
                }
                afterDeliveryNoXA(lastResult, mSess, mMessageMoveConnection, mEndpoint);
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
                if (mHoldUntilAck) {
                    m = wrapMsg(m).setBatchSize(mBatchSize, coord, -1);
                }
                
                // Deliver
                DeliveryResults result = new DeliveryResults(); 
                deliverToEndpoint(result, mMessageMoveConnection, mEndpoint, m, true);

                // Increment counter if msg was delivered
                coord.msgDelivered(result.getOnMessageSucceeded());
                coord.setRollbackOnly(result.getException());
                
                // Wait for ack() to be called if applicable
                coord.waitForAcks();

                // Commit/rollback
                if (coord.isRollbackOnly()) {
                    result.setRollbackOnly(true);
                }
                afterDeliveryNoXA(result, mSess, mMessageMoveConnection, mEndpoint);

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
                        mEndpoint = createMessageEndpoint(mXA, mSess);
                    }
                    if (mEndpoint == null) {
                        throw Exc.exc(LOCALE.x("E143: No endpoint was created, "
                            + "possibly because the RA may be shutting down"));
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
                    release(mEndpoint);
                    mEndpoint = null;
                    mActivation.distress(ex);
                    break;
                } catch (Throwable ex) {
                    release(mEndpoint);
                    mEndpoint = null;
                    mActivation.distress(Exc.exc(LOCALE.x("E190: Caught unexpected Throwable: {0}", ex), ex));
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
