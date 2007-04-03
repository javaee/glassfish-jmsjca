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

import com.stc.jmsjca.localization.Localizer;
import com.stc.jmsjca.util.Logger;
import com.stc.jmsjca.util.Utility;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageProducer;
import javax.jms.QueueSession;
import javax.jms.Session;
import javax.jms.TopicSession;
import javax.resource.spi.UnavailableException;
import javax.resource.spi.endpoint.MessageEndpoint;
import javax.transaction.Transaction;
import javax.transaction.xa.XAResource;

import java.lang.reflect.Method;
import java.util.Properties;

/**
 * Baseclass and interface definition of a delivery-strategy. The connector provides
 * several ways of getting messages out of the JMS server and delivering them to the MDBs:
 * using a connection consumer (concurrent delivery), using a single listener (serial
 * delivery) and using multiple queue-receivers (concurrent delivery, queues only).
 *
 * @author fkieviet
 * @version $Revision: 1.5 $
 */
public abstract class Delivery {
    private static Logger sLog = Logger.getLogger(Delivery.class);

    /**
     * retry interval to check when containers have exited
     */
    public static final long DESTROY_RETRY_INTERVAL_MS = 500;

    /**
     * Do not log destruction progress more than every xx ms.
     */
    public static final long DESTROY_LOG_INTERVAL_MS = 15000;

    /**
     * Property name for copying messages to DLQ
     */
    public static final String REDELIVERYCOUNT = "JMS_Sun_JMSJCA_RedeliveryCount";
    
    /**
     * Property name for copying messages to DLQ
     */
    public static final String ORIGINALDESTINATIONNAME = "JMS_Sun_JMSJCA_OriginalDestinationName";
    
    /**
     * Property name for copying messages to DLQ
     */
    public static final String ORIGINALDESTINATIONTYPE = "JMS_Sun_JMSJCA_OriginalDestinationType";
    
    /**
     * Property name for copying messages to DLQ
     */
    public static final String ORIGINALTIMESTAMP = "JMS_Sun_JMSJCA_OriginalTimestamp";
    
    /**
     * Property name for copying messages to DLQ
     */
    public static final String SUBSCRIBERNAME = "JMS_Sun_JMSJCA_SubscriberName";
    
    /**
     * Property name for copying messages to DLQ
     */
    public static final String CONTEXTNAME = "JMS_Sun_JMSJCA_ContextName";

    /**
     * The activation object
     */
    protected Activation mActivation;

    /**
     * Runtime statistics
     */
    protected DeliveryStats mStats;
    
    /**
     * The onMessage() method
     */
    protected Method mMethod;
    private boolean mIsXA;
    private static final long MAX_CREATE_ENDPOINT_TIME = 20000;
    private static final long CREATE_ENDPOINT_RETRY_DELAY = 1000;
    private RedeliveryHandler mRedeliveryChecker;
    private TxMgr mTxMgr;
    private Object mTxMgrCacheLock = new Object();
    
    private static final Localizer LOCALE = Localizer.get();

    /**
     * Holds and caches a JMS connection, session etc for the dead letter queue. Since
     * the derived delivery classes may have multiple threads, multiple of these
     * objects may be created for each Delivery. This object is passed as an opaque
     * object to the RedeliveryHandler who will pass it back to the move-method; in that
     * sense this object is like a cookie for the move operation.
     */
    public class ConnectionForMove {
        private boolean mIsTopic;
        private Connection mConn;
        private Session mSession;
        private Destination mDest;
        private String mDestName;
        private MessageProducer mProducer;
        private boolean mBusy;
        private boolean mDelayedCommit;
        private boolean mNeedsCommit;
        
        /**
         * Constructor
         */
        public ConnectionForMove() {
        }
        
        /**
         * Closes any allocated resources.
         */
        public void destroy() {
            if (mConn != null) {
                try {
                    mConn.close();
                } catch (JMSException ignore) {
                    // ignore
                }
                mConn = null;
                mSession = null;
                mDest = null;
                mDestName = null;
                mProducer = null;
            }
        }
        
        private Connection getConnection(boolean isTopic) throws JMSException {
            if (mConn != null) {
                if (isTopic != mIsTopic) {
                    throw new JMSException(LOCALE.x("E007: Internal fault: cannot change messaging " +
                            "domain after connection has been created.").toString());
                }
            } else {
                mIsTopic = isTopic;
                RAJMSObjectFactory o = mActivation.getObjectFactory();
                ConnectionFactory fact = o.createConnectionFactory(
                    XConnectionRequestInfo.guessDomain(mActivation.isXA(), mIsTopic),
                    mActivation.getRA(),
                    mActivation.getActivationSpec(),
                    null,
                    null);
                mConn = o.createConnection(
                    fact,
                    XConnectionRequestInfo.guessDomain(mActivation.isXA(), mIsTopic),
                    mActivation.getActivationSpec(),
                    mActivation.getRA(),
                    mActivation.getUserName() == null ? mActivation.getRA().getUserName() : mActivation.getUserName(),
                    mActivation.getPassword() == null ? mActivation.getRA().getPassword() : mActivation.getPassword());
            }
            return mConn;
        }
        
        private Session getSession(boolean isTopic) throws JMSException {
            if (mSession != null) {
                // Check isTopic is still same
                getConnection(isTopic);
            } else {
                RAJMSObjectFactory o = mActivation.getObjectFactory();
                mSession = o.createSession(getConnection(isTopic), mActivation.isXA(),
                    mIsTopic ? TopicSession.class : QueueSession.class,
                        mActivation.getRA(), mActivation.getActivationSpec(), true,
                        javax.jms.Session.SESSION_TRANSACTED);
            }
            return mSession;
        }

        private Destination getDestination(boolean isTopic, String destname) throws JMSException {
            if (mDest != null) {
                getConnection(isTopic);
                if (!destname.equals(mDestName)) {
                    throw new JMSException("Cannot change destination");
                }
            } else {
                RAJMSObjectFactory o = mActivation.getObjectFactory();
                mDest = o.createDestination(getSession(isTopic),
                    mActivation.isXA(), isTopic, mActivation.getActivationSpec(), null,
                    mActivation.getRA(), destname);
                mDestName = destname;
            }
            return mDest;
        }

        private MessageProducer getProducer(boolean isTopic, String destname) throws JMSException {
            getConnection(isTopic);
            if (mProducer == null) {
                RAJMSObjectFactory o = mActivation.getObjectFactory();
              mProducer = o.createMessageProducer(getSession(isTopic),
              mActivation.isXA(), isTopic, getDestination(isTopic, destname), mActivation.getRA());
            }
            return mProducer;
        }

        /**
         * @param b true if the object is to be set to its busy state
         */
        public void setBusy(boolean b) {
            mBusy = b;
        }

        /**
         * Allows the user of this object to determine if the object was used in a normal
         * way or if it may have been involved in a connection type of error
         * 
         * @return true if busy
         */
        public boolean isBusy() {
            return mBusy;
        }
        
        /**
         * Commits a non-XA transation; should be called just before the commit() on
         * the receiving session
         * 
         * @param commit commit or rollback
         * @throws JMSException propagated
         */
        public void nonXACommit(boolean commit) throws JMSException {
            if (mSession != null && mNeedsCommit) {
                if (commit) {
                    mSession.commit();
                } else {
                    mSession.rollback();
                }
            }
            mNeedsCommit = false;
        }

        /**
         * Called when a message is moved
         * 
         * @throws JMSException propagated
         */
        public void setNeedsNonXACommit() throws JMSException {
            if (!mDelayedCommit) {
                //TODO: REMOVE
                mSession.commit();
                mBusy = false;
                mNeedsCommit = false;
            } else {
                mNeedsCommit = true;
            }
        }
        
        /**
         * Temporary: marks this connection as commit by the caller of deliver()
         */
        public void setDelayedCommit() {
            //TODO make default
            mDelayedCommit = true;
        }
    }
    
    /**
     * Creates a new ConnectionForMove object that can be used by derived Delivery 
     * classes. They typically call it once for each thread / container that is part
     * of that Delivery.
     * 
     * @return new instance
     */
    public ConnectionForMove createConnectionForMove() {
        return new ConnectionForMove();
    }
    
    private class DeliveryActions extends RedeliveryHandler {

        public DeliveryActions(RAJMSActivationSpec spec, DeliveryStats stats, int lookbackSize) {
            super(spec, stats, lookbackSize);
        }

        protected void delayMessageDelivery(Message m, Encounter e, long delay) {
            if (delay == 0) {
                return;
            }
            if (delay % 1000 == 0) {
                sLog.info(LOCALE.x("E025: Message with msgid=[{0}] was seen {1}"
                    + " times. Message delivery will be delayed for {2} ms.", 
                    e.getMsgid(), Integer.toString(e.getNEncountered()), new Long(delay)));
            }
            mActivation.sleepAndMonitorStatus(delay);
        }

        protected void deleteMessage(Message m, Encounter e) {
            sLog.info(LOCALE.x("E026: Message with msgid=[{0}] was seen {1} times. It "
                + " will be acknowledged without being delivered.", 
                e.getMsgid(), Integer.toString(e.getNEncountered())));
        }

        protected void move(Message m, Encounter e, boolean isTopic, 
            String destinationName, Object cookie) throws Exception {
            ConnectionForMove x = (ConnectionForMove) cookie;
            
            if (x.isBusy()) {
                x.destroy();
            }
            x.setBusy(true);
            
            Session s = x.getSession(isTopic);
            
            // Enlist resource if necessary
            XAResource xa = null;
            if (mActivation.isXA()) {
                xa = mActivation.getObjectFactory().getXAResource(
                    mActivation.isXA(), s);
                getTxMgr().getTransactionManager().getTransaction().enlistResource(xa);
                // Note: MUST delist lateron!
            }
            
            Exception copyException = null;
            
            if (!mActivation.shouldRedirectRatherThanForward()) {
                // Try to COPY the message
                try {
                    Message newMsg = mActivation.getObjectFactory().copyMessage(m, s, 
                        mActivation.isXA(), isTopic, mActivation.getRA());
                    
                    // Add diagnostics info to msg
                    RAJMSActivationSpec spec = mActivation.getActivationSpec();
                    newMsg.setIntProperty(REDELIVERYCOUNT, e.getNEncountered());
                    newMsg.setStringProperty(ORIGINALDESTINATIONNAME, spec.getDestination());
                    newMsg.setStringProperty(ORIGINALDESTINATIONTYPE, spec.getDestinationType());
                    newMsg.setLongProperty(ORIGINALTIMESTAMP, m.getJMSTimestamp());
                    if (RAJMSActivationSpec.DURABLE.equals(spec.getSubscriptionDurability())) {
                        newMsg.setStringProperty(SUBSCRIBERNAME
                            , mActivation.getActivationSpec().getSubscriptionName());
                    }
                    if (spec.getContextName() != null && spec.getContextName().length() > 0) {
                        newMsg.setStringProperty(CONTEXTNAME, spec.getContextName());
                    }
                    
                    // Send msg
                    MessageProducer prod = x.getProducer(isTopic, destinationName);
                    mActivation.getObjectFactory().send(isTopic, prod, newMsg, 
                        m.getJMSPriority(), m.getJMSDeliveryMode());
                    sLog.info(LOCALE.x("E027: Message with msgid=[{0}] was seen {1}"
                        + " times. It will be forwarded (moved) to {2} {3}"  
                        + " with msgid [{4}]", 
                        e.getMsgid(), Integer.toString(e.getNEncountered()), 
                        (isTopic ? "topic" : "queue"), destinationName, 
                        newMsg.getJMSMessageID()));
                } catch (Exception ex) {
                    copyException = ex;
                }
            }
            
            // If copying failed, try to redirect the message
            if (mActivation.shouldRedirectRatherThanForward() || copyException != null) {
                try {
                    // Redirect msg
                    MessageProducer prod = x.getProducer(isTopic, destinationName);
                    mActivation.getObjectFactory().send(isTopic, prod, m, 
                        m.getJMSPriority(), m.getJMSDeliveryMode());
                    if (mActivation.shouldRedirectRatherThanForward()) {
                        sLog.info(LOCALE.x("E028: Message with msgid=[{0}] was seen {1}"
                            + " times. It will be redirected to {2} {3}." , 
                            e.getMsgid(), Integer.toString(e.getNEncountered()), 
                            (isTopic ? "topic" : "queue"), destinationName)); 
                    } else {
                        sLog.info(LOCALE.x("E029: Message with msgid=[{0}] was seen {1} "
                            + "times. It will be redirected to {2} {3}. An attempt was "
                            + "made to forward the message with additional information "
                            + "in the message''s properties, but this attempt was "
                            + "unsuccessful due to the following error: [{4}].", 
                            e.getMsgid(), Integer.toString(e.getNEncountered()), 
                            (isTopic ? "topic" : "queue"), destinationName), copyException); 
                    }
                    copyException = null;
                } catch (Exception ex) {
                    copyException = new Exception("Redirect failed due to " + ex
                        + "; forward failed due to " + copyException, ex);
                }
            }
            
            // Delist (MUST delist as not to currupt the state of the xaresource)
            if (xa != null) {
                getTxMgr().getTransactionManager().getTransaction().delistResource(xa,
                    XAResource.TMSUCCESS);
            } else {
                x.setNeedsNonXACommit();
            }
            
            if (copyException != null) {
                throw copyException;
            }
            
            x.setBusy(false);
        }
    }
    
    /**
     * Constructor
     *
     * @param a Activation
     * @param stats DeliveryStats
     */
    public Delivery(Activation a, DeliveryStats stats) {
        mActivation = a;
        mStats = stats;
        mMethod = mActivation.getOnMessageMethod();
        mIsXA = mActivation.isXA();
        mRedeliveryChecker = new DeliveryActions(a.getActivationSpec(), mStats, 5000);
    }

    /**
     * Creates a MessageEndpoint
     *
     * @return MessageEndpoint, will return null in case of shutdown
     * @param xa XAResource
     * @throws Exception on failure
     */
    protected MessageEndpoint createMessageEndpoint(XAResource xa) throws Exception {
        if (sLog.isDebugEnabled()) {
            sLog.debug("Creating message endpoint");
        }

        MessageEndpoint ret = null;
        long start = System.currentTimeMillis();
        for (;;) {
            if (mActivation.isStopping()) {
                if (sLog.isDebugEnabled()) {
                    sLog.debug("Aborting message end point creation: stopping");
                }

                break;
            }

            try {
                ret = mActivation.getMessageEndpointFactory().createEndpoint(xa);
            } catch (UnavailableException ex1) {
                long now = System.currentTimeMillis();
                if (now - start > MAX_CREATE_ENDPOINT_TIME) {
                    // FAIL
                    throw new Exception(
                        "Failed to create endpoint... giving up. Last exception: " + ex1,
                        ex1);
                } else {
                    try {
                        Thread.sleep(CREATE_ENDPOINT_RETRY_DELAY);
                    } catch (InterruptedException ex2) {
                        // Ignore
                    }
                }
            }
            if (ret != null) {
                mStats.addMessageEndpoint();
                break;
            }
        }

        return ret;
    }
    
    /**
     * Releases a message endpoint
     * 
     * @param mep endpoint to release; null is OK
     */
    protected void release(MessageEndpoint mep) {
        if (mep != null) {
            if (sLog.isDebugEnabled()) {
                sLog.debug("Releasing endpoint");
            }

            mep.release();
            mStats.removeMessageEndpoint();
        }
    }
    
    /**
     * Wraps an exception thrown in BeforeDelivery
     */
    public static class BeforeDeliveryException extends RuntimeException {
        /**
         * Constructor
         * 
         * @param e Exception caught
         */
        public BeforeDeliveryException(Exception e) {
            super("The application server threw an exception in beforeDelivery(): " + e.getMessage(), e);
        }
    }

    /**
     * Wraps an exception thrown in AfterDelivery
     */
    public static class AfterDeliveryException extends RuntimeException {
        /**
         * Constructor
         * 
         * @param e Exception caught
         */
        public AfterDeliveryException(Exception e) {
            super("The application server threw an exception in afterDelivery(): " + e.getMessage(), e);
        }
    }
    
    /**
     * Encapsulation of an endpoint
     * TODO refactor the deliver() method below.
     */
    public class MDB {
        private XAResource mXA;
        
        /**
         * Constructor
         * 
         * @param xa XAResource
         */
        public MDB(XAResource xa) {
            mXA = xa;
        }

        /**
         * @return XAResource
         */
        public XAResource getXAResource() {
            return mXA;
        }
        
    }
    
    /**
     * Encapsulates the result of a call to deliver
     * 
     * @author fkieviet
     */
    public static class DeliveryResults {
        private boolean mShouldDiscardEndpoint;
        private boolean mShouldNotCallAfterDelivery;
        private boolean mBeforeDeliveryFailed;
        private boolean mAfterDeliveryFailed;
        private boolean mOnMessageFailed;
        private Exception mException;
        private boolean mOnMessageWasCalled;
        private boolean mOnMessageWasBypassed;
        private boolean mOnMessageSucceeded;
        
        /**
         * Clears the state
         */
        public void reset() {
            mShouldDiscardEndpoint = false;
            mShouldNotCallAfterDelivery = false;
            mBeforeDeliveryFailed = false;
            mAfterDeliveryFailed = false;
            mOnMessageFailed = false;
            mException = null;
            mOnMessageSucceeded = false;
            mOnMessageWasCalled = false;
            mOnMessageWasBypassed = false;
        }
        
        /**
         * Clears all state except state concerning transactions
         */
        public void resetDeliveryState() {
            mOnMessageFailed = false;
            mException = null;
            mOnMessageSucceeded = false;
            mOnMessageWasCalled = false;
            mOnMessageWasBypassed = false;
        }
        
        /**
         * Getter for onMessageSucceeded
         *
         * @return boolean
         */
        public boolean getOnMessageSucceeded() {
            return mOnMessageSucceeded;
        }
        /**
         * Setter for onMessageSucceeded
         *
         * @param onMessageSucceeded booleanThe onMessageSucceeded to set.
         */
        public void setOnMessageSucceeded(boolean onMessageSucceeded) {
            mOnMessageSucceeded = onMessageSucceeded;
        }
        /**
         * Getter for afterDeliveryFailed
         *
         * @return boolean
         */
        public boolean getAfterDeliveryFailed() {
            return mAfterDeliveryFailed;
        }
        /**
         * Setter for afterDeliveryFailed
         *
         * @param afterDeliveryFailed booleanThe afterDeliveryFailed to set.
         */
        public void setAfterDeliveryFailed(boolean afterDeliveryFailed) {
            mAfterDeliveryFailed = afterDeliveryFailed;
        }
        /**
         * Getter for beforeDeliveryFailed
         *
         * @return boolean
         */
        public boolean getBeforeDeliveryFailed() {
            return mBeforeDeliveryFailed;
        }
        /**
         * Setter for beforeDeliveryFailed
         *
         * @param beforeDeliveryFailed booleanThe beforeDeliveryFailed to set.
         */
        public void setBeforeDeliveryFailed(boolean beforeDeliveryFailed) {
            mBeforeDeliveryFailed = beforeDeliveryFailed;
        }
        /**
         * Getter for exception
         *
         * @return Exception
         */
        public Exception getException() {
            return mException;
        }
        /**
         * Setter for exception
         *
         * @param exception ExceptionThe exception to set.
         */
        public void setException(Exception exception) {
            mException = exception;
        }
        /**
         * Getter for onMessageFailed
         *
         * @return boolean
         */
        public boolean getOnMessageFailed() {
            return mOnMessageFailed;
        }
        /**
         * Setter for onMessageFailed
         *
         * @param onMessageFailed booleanThe onMessageFailed to set.
         */
        public void setOnMessageFailed(boolean onMessageFailed) {
            mOnMessageFailed = onMessageFailed;
        }
        /**
         * Getter for shouldDiscardEndpoint
         *
         * @return boolean
         */
        public boolean getShouldDiscardEndpoint() {
            return mShouldDiscardEndpoint;
        }
        /**
         * Setter for shouldDiscardEndpoint
         *
         * @param shouldDiscardEndpoint booleanThe shouldDiscardEndpoint to set.
         */
        public void setShouldDiscardEndpoint(boolean shouldDiscardEndpoint) {
            mShouldDiscardEndpoint = shouldDiscardEndpoint;
        }
        /**
         * Getter for shouldNotCallAfterDelivery
         *
         * @return boolean
         */
        public boolean getShouldNotCallAfterDelivery() {
            return mShouldNotCallAfterDelivery;
        }
        /**
         * Setter for shouldNotCallAfterDelivery
         *
         * @param shouldNotCallAfterDelivery booleanThe shouldNotCallAfterDelivery to set.
         */
        public void setShouldNotCallAfterDelivery(boolean shouldNotCallAfterDelivery) {
            mShouldNotCallAfterDelivery = shouldNotCallAfterDelivery;
        }

        /**
         * Getter for onMessageWasBypassed
         *
         * @return boolean
         */
        public boolean getOnMessageWasBypassed() {
            return mOnMessageWasBypassed;
        }

        /**
         * Setter for onMessageWasBypassed
         *
         * @param onMessageWasBypassed booleanThe onMessageWasBypassed to set.
         */
        public void setOnMessageWasBypassed(boolean onMessageWasBypassed) {
            mOnMessageWasBypassed = onMessageWasBypassed;
        }

        /**
         * Getter for onMessageWasCalled
         *
         * @return boolean
         */
        public boolean getOnMessageWasCalled() {
            return mOnMessageWasCalled;
        }

        /**
         * Setter for onMessageWasCalled
         *
         * @param onMessageWasCalled booleanThe onMessageWasCalled to set.
         */
        public void setOnMessageWasCalled(boolean onMessageWasCalled) {
            mOnMessageWasCalled = onMessageWasCalled;
        }
    }
    
    /**
     * Wraps the beforeDelivery() call
     * 
     * @param result state
     * @param target MEP
     */
    public void beforeDelivery(DeliveryResults result, MessageEndpoint target) {
        if (mIsXA) {
            try {
                target.beforeDelivery(mMethod);
            } catch (Exception e) {
                result.setBeforeDeliveryFailed(true);
                result.setShouldDiscardEndpoint(true);
                result.setShouldNotCallAfterDelivery(true);
                result.setException(e);
            }
        }
    }
    
    /**
     * Wraps the afterDelivery() call
     * 
     * @param result state
     * @param connectionForMove DLQ
     * @param target MEP
     * @param mdb wrapper
     */
    public void afterDelivery(DeliveryResults result, ConnectionForMove connectionForMove, 
        MessageEndpoint target, Delivery.MDB mdb) {

        if (!mIsXA || result.getShouldNotCallAfterDelivery()) {
            return;
        }

        try {
            if (!result.getOnMessageWasBypassed()) {
                target.afterDelivery();
            } else {
                // Weblogic does not allow afterDelivery() to be called without
                // having called onMessage(). This is required when a message
                // is sent to a DLQ. In that case, commit the transaction
                // manually and mark the endpoint for release so that the
                // container will not reuse it to avoid inconsistent states.
                result.setShouldDiscardEndpoint(true);
                Transaction tx = getTxMgr().getTransactionManager().getTransaction();
                tx.delistResource(mdb.getXAResource(), XAResource.TMSUCCESS);
                tx.commit();
            }

        } catch (Exception e) {
            result.setAfterDeliveryFailed(true);
            result.setShouldDiscardEndpoint(true);
            result.setException(e);
        }
    }
    
//    /**
//     * Delivers the message to the specified MessageEndpoint; can be called both from CC
//     * and serial delivery. In the case of CC, this is called by a worker thread. If an
//     * exception is thrown in the MDB, it will be returned.
//     *
//     * @param result Result
//     * @param connectionForMove provides a point to get access through to the JMS connection that
//     * can be used to move a message
//     * @param target MessageEndpoint
//     * @param m Message
//     * @param noBeforeAfterDelivery set to true to bypass before/after delivery
//     * @param mdb MDB
//     */
//    public void deliverEx(DeliveryResults result, ConnectionForMove connectionForMove, MessageEndpoint target,
//        javax.jms.Message m, boolean noBeforeAfterDelivery, MDB mdb) {
//        if (sLog.isDebugEnabled()) {
//            sLog.debug("Delivering message to endpoint");
//        }
//        
//        RuntimeException mdbEx = null;
//        
//        // Stats
//        mStats.aboutToDeliverMessage();
//        
//        try {
//            // Container transaction mgt
//            try {
//                if (mIsXA && !noBeforeAfterDelivery) {
//                    target.beforeDelivery(mMethod);
//                }
//            } catch (Exception e) {
//                mdbEx = new BeforeDeliveryException(e);
//                result.setBeforeDeliveryFailed(true);
//                result.setException(mdbEx);
//                throw mdbEx;
//            }
//
//            boolean shouldDeliver = mRedeliveryChecker.shouldDeliver(connectionForMove, m);
//            
//            if (shouldDeliver) {
//                try {
//                    ((javax.jms.MessageListener) target).onMessage(m);
//                    result.setOnMessageSucceeded(true);
//                } catch (RuntimeException ex) {
//                    String msg =
//                        "An unexpected exception was encountered delivering a message to an "
//                        + "endpoint. The message will be rolled back. Exception: [" + ex
//                        + "]";
//                    sLog.warn(msg, ex);
//                    mdbEx = ex;
//                    result.setOnMessageFailed(true);
//                    result.setException(mdbEx);
//                }
//            }
//
//            // Stats
//            mStats.messageDelivered();
//
//            // Container transaction mgt
//            try {
//                if (mIsXA) {
//                    if (!shouldDeliver) {
//                        // Weblogic does not allow afterDelivery() to be called without
//                        // having called onMessage(). This is required when a message
//                        // is sent to a DLQ. In that case, commit the transaction
//                        // manually and mark the endpoint for release so that the
//                        // container will not reuse it to avoid inconsistent states.
//                        Transaction tx = getTxMgr().getTransactionManager().getTransaction();
//                        tx.delistResource(mdb.getXAResource(), XAResource.TMSUCCESS);
//                        tx.commit();
//                    }
//                    
//                    if (!noBeforeAfterDelivery && shouldDeliver) {
//                        target.afterDelivery();
//                    }
//                }
//            } catch (Exception e) {
//                mdbEx = new AfterDeliveryException(e); 
//                throw mdbEx;
//            }
//            
//            // See above: mark for release if afterDelivery was not called
//            if (mIsXA && !shouldDeliver && mdbEx == null) {
//                mdbEx = new RuntimeException("MDB should be discarded");
//                result.setShouldDiscardEndpoint(true);
//                result.setShouldNotCallAfterDelivery(true);
//                result.setException(mdbEx);
//            }
//        } catch (Exception ex) {
//            String msg =
//                "An unexpected exception was encountered processing a message. Exception: "
//                + ex;
//            sLog.warn(msg, ex);
//        }
//    }

    /**
     * Delivers the message to the specified MessageEndpoint; can be called both from CC
     * and serial delivery. In the case of CC, this is called by a worker thread. If an
     * exception is thrown in the MDB, it will be returned.
     *
     * @param result Result
     * @param connectionForMove provides a point to get access through to the JMS connection that
     * can be used to move a message
     * @param target MessageEndpoint
     * @param m Message
     */
    public void deliverToEndpoint(DeliveryResults result, ConnectionForMove connectionForMove, MessageEndpoint target,
        javax.jms.Message m) {
        if (sLog.isDebugEnabled()) {
            sLog.debug("Delivering message to endpoint");
        }
        
        // Stats
        mStats.aboutToDeliverMessage();
        
        try {
            boolean shouldDeliver = mRedeliveryChecker.shouldDeliver(connectionForMove, m);
            
            if (!shouldDeliver) {
                result.setOnMessageWasBypassed(true);
            } else {
                try {
                    result.setOnMessageWasCalled(true);
                    ((javax.jms.MessageListener) target).onMessage(m);
                    result.setOnMessageSucceeded(true);
                } catch (RuntimeException ex) {
                    sLog.warn(LOCALE.x("E031: The entity the message was sent to for "
                        + "processing, threw an exception. The message will be "
                        + "rolled back. Exception: [{0}]", ex), ex);
                    result.setOnMessageFailed(true);
                    result.setException(ex);
                }
            }
        } catch (Exception ex) {
            sLog.warn(LOCALE.x("E030: An unexpected exception was encountered " 
                + "processing a message. Exception: {0}", ex), ex);
        } finally {
            // Stats
            mStats.messageDelivered();
        }
    }
    
    /**
     * Delivers the message to the specified MessageEndpoint; can be called both from CC
     * and serial delivery. In the case of CC, this is called by a worker thread. If an
     * exception is thrown in the MDB, it will be returned.
     *
     * @param connectionForMove provides a point to get access through to the JMS connection that
     * can be used to move a message
     * @param target MessageEndpoint
     * @param m Message
     * @param noBeforeAfterDelivery set to true to bypass before/after delivery
     * @param mdb MDB
     * @return RuntimeException
     */
    public RuntimeException deliver(ConnectionForMove connectionForMove, MessageEndpoint target,
        javax.jms.Message m, boolean noBeforeAfterDelivery, MDB mdb) {
        if (sLog.isDebugEnabled()) {
            sLog.debug("Delivering message to endpoint");
        }
        
        RuntimeException mdbEx = null;
        
        // Stats
        mStats.aboutToDeliverMessage();
        
        try {
            // Container transaction mgt
            try {
                if (mIsXA && !noBeforeAfterDelivery) {
                    target.beforeDelivery(mMethod);
                }
            } catch (Exception e) {
                mdbEx = new BeforeDeliveryException(e);
                throw mdbEx;
            }

            boolean shouldDeliver = mRedeliveryChecker.shouldDeliver(connectionForMove, m);
            
            if (shouldDeliver) {
                try {
                    ((javax.jms.MessageListener) target).onMessage(m);
                } catch (RuntimeException ex) {
                    sLog.warn(LOCALE.x("E031: The entity the message was sent to for "
                        + "processing, threw an exception. The message will be "
                        + "rolled back. Exception: [{0}]", ex), ex);
                    mdbEx = ex;
                }
            }

            // Stats
            mStats.messageDelivered();

            // Container transaction mgt
            try {
                if (mIsXA) {
                    if (!shouldDeliver) {
                        // Weblogic does not allow afterDelivery() to be called without
                        // having called onMessage(). This is required when a message
                        // is sent to a DLQ. In that case, commit the transaction
                        // manually and mark the endpoint for release so that the
                        // container will not reuse it to avoid inconsistent states.
                        Transaction tx = getTxMgr().getTransactionManager().getTransaction();
                        tx.delistResource(mdb.getXAResource(), XAResource.TMSUCCESS);
                        tx.commit();
                    }
                    
                    if (!noBeforeAfterDelivery && shouldDeliver) {
                        target.afterDelivery();
                    }
                }
            } catch (Exception e) {
                mdbEx = new AfterDeliveryException(e); 
                throw mdbEx;
            }
            
            // See above: mark for release if afterDelivery was not called
            if (mIsXA && !shouldDeliver && mdbEx == null) {
                mdbEx = new RuntimeException("MDB should be discarded");
            }
        } catch (Exception ex) {
            sLog.warn(LOCALE.x("E032: An unexpected exception occurred while processing "
                + "a message. Exception: {0}", ex), ex);
        }

        return mdbEx;
    }

    /**
     * Releases any resources associated with delivery.
     * HAS TO BE CALLED FROM THE RA THREAD
     */
    public abstract void deactivate();

    /**
     * Starts delivery __Called from initialization thread__
     *
     * @throws Exception on failure
     */
    public abstract void start() throws Exception;

    /**
     * getActivation
     *
     * @return Activation
     */
    public Activation getActivation() {
        return mActivation;
    }

    /**
     * getStats
     *
     * @return Object
     */
    public DeliveryStats getStats() {
        return mStats;
    }

    /**
     * isXA
     *
     * @return boolean
     */
    public final boolean isXA() {
        return mIsXA;
    }
    
    /**
     * The actual max number of endpoints being used 
     * 
     * @return int
     */
    public abstract int getConfiguredEndpoints();
    
    /**
     * Provides access to the J2EE transaction manager
     * 
     * @return tx manager, or null if not accessible (e.g. unknown container)
     * @throws Exception on failure
     */
    public TxMgr getTxMgr() throws Exception {
        synchronized (mTxMgrCacheLock) {
            if (mTxMgr == null) {
                Properties p = new Properties();
                mActivation.getObjectFactory().getProperties(p, mActivation.getRA(), 
                    mActivation.getActivationSpec(), null, null);
                String mTxMgrLocatorClass = p.getProperty(Options.TXMGRLOCATOR, TxMgr.class.getName());
                mTxMgrLocatorClass = Utility.getSystemProperty(Options.TXMGRLOCATOR, mTxMgrLocatorClass);
                try {
                    Class c = Class.forName(mTxMgrLocatorClass, false, this.getClass().getClassLoader()); 
                    TxMgr txmgr = (TxMgr) c.newInstance();
                    txmgr.init(p);
                    mTxMgr = txmgr;
                } catch (Exception e) {
                    sLog.warn(LOCALE.x("E088: Transaction manager locator cannot be initialized: {0}", e), e);
                }
            }
            return mTxMgr;
        }
    }
}
