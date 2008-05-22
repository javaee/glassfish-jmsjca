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
import com.stc.jmsjca.util.Exc;
import com.stc.jmsjca.util.Logger;
import com.stc.jmsjca.util.Utility;
import com.stc.jmsjca.util.XAssert;

import javax.jms.BytesMessage;
import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.MapMessage;
import javax.jms.Message;
import javax.jms.MessageProducer;
import javax.jms.ObjectMessage;
import javax.jms.QueueSession;
import javax.jms.Session;
import javax.jms.StreamMessage;
import javax.jms.TextMessage;
import javax.jms.TopicSession;
import javax.resource.spi.UnavailableException;
import javax.resource.spi.endpoint.MessageEndpoint;
import javax.transaction.Transaction;
import javax.transaction.TransactionManager;
import javax.transaction.xa.XAResource;

import java.lang.reflect.Method;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;

/**
 * Baseclass and interface definition of a delivery-strategy. The connector provides
 * several ways of getting messages out of the JMS server and delivering them to the MDBs:
 * using a connection consumer (concurrent delivery), using a single listener (serial
 * delivery) and using multiple queue-receivers (concurrent delivery, queues only).
 *
 * @author fkieviet
 * @version $Revision: 1.8 $
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
    
    /**
     * Batch size
     */
    protected int mBatchSize;

    /**
     * HUA 
     */
    protected boolean mHoldUntilAck;

    private static final long MAX_CREATE_ENDPOINT_TIME = 20000;
    private static final long CREATE_ENDPOINT_RETRY_DELAY = 1000;
    private RedeliveryHandler mRedeliveryChecker;
    private TransactionManager mTxMgr;
    private Object mTxMgrCacheLock = new Object();
    private boolean mTxFailureLoggedOnce;
    private IdentityHashMap mThreadsCurrentlyInOnMessage = new IdentityHashMap();
    
    
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
                    throw Exc.jmsExc(LOCALE.x("E007: Internal fault: cannot change messaging " +
                            "domain after connection has been created."));
                }
            } else {
                mIsTopic = isTopic;
                RAJMSObjectFactory o = mActivation.getObjectFactory();
                ConnectionFactory fact = o.createConnectionFactory(
                    XConnectionRequestInfo.guessDomain(mActivation.isCMT() && !mActivation.isXAEmulated(), mIsTopic),
                    mActivation.getRA(),
                    mActivation.getActivationSpec(),
                    null,
                    null);
                mConn = o.createConnection(
                    fact,
                    XConnectionRequestInfo.guessDomain(mActivation.isCMT() && !mActivation.isXAEmulated(), mIsTopic),
                    mActivation.getActivationSpec(),
                    mActivation.getRA(),
                    mActivation.getUserName() == null 
                    ? mActivation.getRA().getUserName() : mActivation.getUserName(),
                    mActivation.getPassword() == null 
                    ? mActivation.getRA().getClearTextPassword() : mActivation.getPassword());
            }
            return mConn;
        }
        
        private Session getSession(boolean isTopic) throws JMSException {
            if (mSession != null) {
                // Check isTopic is still same
                getConnection(isTopic);
            } else {
                RAJMSObjectFactory o = mActivation.getObjectFactory();
                mSession = o.createSession(getConnection(isTopic)
                    , mActivation.isCMT() && !mActivation.isXAEmulated()
                    , mIsTopic ? TopicSession.class : QueueSession.class
                    , mActivation.getRA()
                    , mActivation.getActivationSpec()
                    , true
                    , javax.jms.Session.SESSION_TRANSACTED);
            }
            return mSession;
        }

        private Destination getDestination(boolean isTopic, String destname) throws JMSException {
            if (mDest != null) {
                getConnection(isTopic);
                if (!destname.equals(mDestName)) {
                    throw Exc.jmsExc(LOCALE.x("E033:Cannot change destination"));
                }
            } else {
                RAJMSObjectFactory o = mActivation.getObjectFactory();
                mDest = o.createDestination(getSession(isTopic),
                    mActivation.isCMT() && !mActivation.isXAEmulated(), isTopic, mActivation.getActivationSpec(), null,
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
                    mActivation.isCMT() && !mActivation.isXAEmulated(), 
                    isTopic, 
                    getDestination(isTopic, destname), 
                    mActivation.getRA());
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
            // TODO: setbusy???
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
                + "will be acknowledged without being delivered.", 
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
            if (mActivation.isCMT()) {
                if (mActivation.isXAEmulated()) {
                    xa = new PseudoXAResource(s);
                } else {
                    xa = mActivation.getObjectFactory().getXAResource(true, s);
                }
                getTransaction(true).enlistResource(xa);
                // Note: MUST delist lateron!
            }
            
            Exception copyException = null;
            
            if (!mActivation.shouldRedirectRatherThanForward()) {
                // Try to COPY the message
                try {
                    Message newMsg = mActivation.getObjectFactory().copyMessage(m, s, 
                        mActivation.isCMT() && !mActivation.isXAEmulated(), isTopic, mActivation.getRA());
                    
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
                    newMsg.setStringProperty(Options.MessageProperties.ORIGINAL_MSGID, e.getMsgid());
                    String correlationId = null;
                    try {
                        correlationId = m.getJMSCorrelationID();
                    } catch (JMSException ignore) {
                        // ignore
                    }
                    newMsg.setStringProperty(Options.MessageProperties.ORIGINAL_CORRELATIONID, correlationId);
                    
                    if (spec.getClientId() != null) {
                        newMsg.setStringProperty(Options.MessageProperties.ORIGINAL_CLIENTID, spec.getClientId());
                    }
                    
                    // Copy stateful redelivery properties
                    Map statefulRedeliveryProperties = e.getStatefulRedeliveryProperties();
                    for (Iterator iterator = statefulRedeliveryProperties.entrySet().iterator(); iterator.hasNext();) {
                        Map.Entry kv = (Map.Entry) iterator.next();
                        newMsg.setStringProperty((String) kv.getKey(), (String) kv.getValue());
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
                            (isTopic ? "topic" : "queue"), destinationName, copyException)
                            , copyException); 
                    }
                    copyException = null;
                } catch (Exception ex) {
                    copyException = new Exception("Redirect failed due to " + ex
                        + "; forward failed due to " + copyException, ex);
                }
            }
            
            // Delist (MUST delist as not to currupt the state of the xaresource)
            if (xa != null) {
                getTxMgr().getTransaction().delistResource(xa, XAResource.TMSUCCESS);
            } else {
                x.setNeedsNonXACommit();
            }
            
            if (copyException != null) {
                throw copyException;
            }
            
            x.setBusy(false);
        }

        protected void stopConnector(String s) {
            mActivation.stopConnectorByMDB(s);
        }
    }
    
    /**
     * Constructor
     *
     * @param a Activation
     * @param stats DeliveryStats
     * @throws Exception on failure
     */
    public Delivery(Activation a, DeliveryStats stats) throws Exception {
        mActivation = a;
        mStats = stats;
        mMethod = mActivation.getOnMessageMethod();
        mRedeliveryChecker = new DeliveryActions(a.getActivationSpec(), mStats, 5000);

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
        if (mActivation.isCMT()) {
            Properties p = new Properties();
            mActivation.getObjectFactory().getProperties(p, mActivation.getRA(), 
                mActivation.getActivationSpec(), null, null);
            String txMgrLocatorClass = p.getProperty(Options.TXMGRLOCATOR, TxMgr.class.getName());
            txMgrLocatorClass = Utility.getSystemProperty(Options.TXMGRLOCATOR, txMgrLocatorClass);
            try {
                Class c = Class.forName(txMgrLocatorClass, false, this.getClass().getClassLoader()); 
                TxMgr txmgr = (TxMgr) c.newInstance();
                txmgr.init(p);
                mTxMgr = txmgr.getTransactionManager();
            } catch (Exception e) {
                throw Exc.rsrcExc(LOCALE.x("E057: TxMgr could not be obtained: {0}", e), e);
            }
        }
    }

    /**
     * Creates a MessageEndpoint
     *
     * @return MessageEndpoint, will return null in case of shutdown
     * @param xa XAResource
     * @param s session; this is used in the unusual case of TopicToQueueDelivery
     * @throws Exception on failure
     */
    protected MessageEndpoint createMessageEndpoint(XAResource xa, Session s) throws Exception {
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
                    throw Exc.rsrcExc(LOCALE.x("E120: Failed to create endpoint... "
                        + "giving up. Last exception: {0}", ex1), ex1);
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

            try {
                mep.release();
            } catch (RuntimeException e) {
                sLog.warn(LOCALE.x("E197: Release of endpoint failed unexpectedly: {0}", e), e);
            }
            mStats.removeMessageEndpoint();
        }
    }
    
    /**
     * Encapsulation of an endpoint
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
        private boolean mBeforeDeliveryFailed;
        private boolean mAfterDeliveryFailed;
        private boolean mOnMessageFailed;
        private Exception mException;
        private boolean mOnMessageWasCalled;
        private int mNOnMessageWasCalled;
        private boolean mOnMessageWasBypassed;
        private boolean mOnMessageSucceeded;
        private boolean mIsRollbackOnly;
        
        /**
         * Clears the state
         */
        public void reset() {
            mShouldDiscardEndpoint = false;
            mBeforeDeliveryFailed = false;
            mAfterDeliveryFailed = false;
            mIsRollbackOnly = false;
            mOnMessageFailed = false;
            mException = null;
            mOnMessageSucceeded = false;
            mOnMessageWasCalled = false;
            mNOnMessageWasCalled = 0;
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
            if (onMessageWasCalled) {
                mNOnMessageWasCalled++;
            }
        }

        /**
         * Getter for isRollbackOnly
         *
         * @return boolean
         */
        public boolean getIsRollbackOnly() {
            return mIsRollbackOnly;
        }

        /**
         * Setter for isRollbackOnly
         *
         * @param isRollbackOnly booleanThe isRollbackOnly to set.
         */
        public void setRollbackOnly(boolean isRollbackOnly) {
            mIsRollbackOnly = isRollbackOnly;
        }

        /**
         * Getter for nOnMessageWasCalled
         *
         * @return int
         */
        public int getNOnMessageWasCalled() {
            return mNOnMessageWasCalled;
        }
    }
    
    /**
     * Wraps a message for HUA mode
     * 
     * @param msgToWrap msg
     * @return wrapped message if HUA mode is active, unwrapped otherwise
     * @throws JMSException on failure
     */
    protected WMessageIn wrapMsg(Message msgToWrap) throws JMSException {
        // Protect against unnecessary double wrapping
        if (msgToWrap instanceof WMessageIn) {
            return (WMessageIn) msgToWrap;
        }
        
        WMessageIn ret = null;
        
        // Check for multiple interfaces
        int nItf = 0;

        if (msgToWrap instanceof TextMessage) {
            nItf++;
            ret = new WTextMessageIn((TextMessage) msgToWrap);
        } 
        if (msgToWrap instanceof BytesMessage) {
            nItf++;
            ret = new WBytesMessageIn((BytesMessage) msgToWrap);
        } 
        if (msgToWrap instanceof MapMessage) {
            nItf++;
            ret = new WMapMessageIn((MapMessage) msgToWrap);
        } 
        if (msgToWrap instanceof ObjectMessage) {
            nItf++;
            ret = new WObjectMessageIn((ObjectMessage) msgToWrap);
        } 
        if (msgToWrap instanceof StreamMessage) {
            nItf++;
            ret = new WStreamMessageIn((StreamMessage) msgToWrap);
        }
        
        // None found
        if (ret == null) {
            nItf++;
            ret = new WMessageIn(msgToWrap);
        }
        
        if (nItf > 1) {
            throw Exc.jmsExc(LOCALE.x("E032: Cannot determine message type: the message " 
                + "implements multiple interfaces."));
        }
        
        ret.setActivation(mActivation);
        
        return ret;
    }

    /**
     * Sets the tx (if any) to rollback only
     * 
     * @param result status object
     * @param rethrow if true, will rethrow tx exceptions
     * @throws Exception tx exceptions
     */
    protected void txSetRollbackOnly(DeliveryResults result, boolean rethrow) throws Exception {
        result.setRollbackOnly(true);
        if (mActivation.isCMT()) {
            try {
                getTransaction(true).setRollbackOnly();
            } catch (Exception e) {
                result.setShouldDiscardEndpoint(true);
                if (rethrow) {
                    throw e;
                } else {
                    sLog.error(LOCALE.x("E201: Failed to mark transaction for RollbackOnly: {0}", e), e);
                }
            }
        }
    }
    
    /**
     * Wraps the beforeDelivery() call
     * 
     * @param result state
     * @param target MEP
     * @param rethrowSystemException throw instead of log system failures (tx mgr only)
     * @throws Exception tx exceptions
     */
    public void beforeDelivery(DeliveryResults result, MessageEndpoint target, 
        boolean rethrowSystemException) throws Exception {
        if (mActivation.isCMT()) {
            try {
                target.beforeDelivery(mMethod);
            } catch (Exception e) {
                result.setBeforeDeliveryFailed(true);
                result.setShouldDiscardEndpoint(true);
                result.setException(e);
                result.setRollbackOnly(true);
                try {
                    if (rethrowSystemException) {
                        throw e;
                    } else {
                        sLog.error(LOCALE.x("E198: Transaction initialization failed " 
                            + "unexpectedly: {0}", e), e);
                    }
                } finally {
                    txSetRollbackOnly(result, rethrowSystemException);
                }
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
     * @param rethrowSystemException throw instead of log system failures (tx mgr only)
     * @throws Exception tx exceptions 
     */
    public void afterDelivery(DeliveryResults result, ConnectionForMove connectionForMove, 
        MessageEndpoint target, Delivery.MDB mdb, boolean rethrowSystemException) throws Exception {

        if (!mActivation.isCMT() || result.getBeforeDeliveryFailed()) {
            return;
        }

        if (!result.getOnMessageWasBypassed()) {
            try {
                if (result.getIsRollbackOnly()) {
                    getTransaction(true).setRollbackOnly();
                }
                target.afterDelivery();
            } catch (Exception e) {
                result.setAfterDeliveryFailed(true);
                result.setShouldDiscardEndpoint(true);
                result.setException(e);
                if (rethrowSystemException) {
                    throw e;
                } else {
                    sLog.error(LOCALE.x("E199: Transaction completion unexpectedly failed: {0}", e), e);
                }
            }
        } else {
            try {
                // Weblogic does not allow afterDelivery() to be called without
                // having called onMessage(). This is required when a message
                // is sent to a DLQ. In that case, commit the transaction
                // manually and mark the endpoint for release so that the
                // container will not reuse it to avoid inconsistent states.
                result.setShouldDiscardEndpoint(true);
                Transaction tx = getTransaction(true);
                tx.delistResource(mdb.getXAResource(), XAResource.TMSUCCESS);
                tx.commit();
            } catch (Exception e) {
                result.setAfterDeliveryFailed(true);
                result.setShouldDiscardEndpoint(true);
                result.setException(e);
                if (rethrowSystemException) {
                    throw e;
                } else {
                    sLog.error(LOCALE.x("E200: the transaction could not be committed: {0}", e), e);
                }
            }
        }
    }
    
    
    
    /**
     * Wraps the afterDelivery() call
     * 
     * @param result state
     * @param session Session
     * @param connectionForMove DLQ
     * @param target MEP
     */
    public void afterDeliveryNoXA(DeliveryResults result, javax.jms.Session session, 
        ConnectionForMove connectionForMove, MessageEndpoint target) {

        if (mActivation.isCMT()) {
            return;
        }
    
        // Deal with MoveConnection
        if (result.getIsRollbackOnly()) {
            try {
                connectionForMove.nonXACommit(false);
            } catch (JMSException ex) {
                sLog.error(LOCALE.x("E097: The message sent as part of redelivery handling " 
                    + "could not be rolled back: {0}", ex), ex);
            }
        } else {
            try {
                connectionForMove.nonXACommit(true);
            } catch (JMSException ex) {
                result.setRollbackOnly(true);
                sLog.error(LOCALE.x("E098: The message sent as part of redelivery handling " 
                    + "could not be committed. The receiving of the message will be rolled "
                    + "back. The error was: {0}", ex), ex);
            }
        }
        
        // Commit/rollback session
        if (!result.getIsRollbackOnly()) {
            try {
                session.commit();
            } catch (JMSException ex) {
                sLog.error(LOCALE.x("E065: The message could not be committed: {0}", ex), ex);
            }
        } else {
            try {
                session.rollback();
            } catch (JMSException ex) {
                sLog.error(LOCALE.x("E066: The message could not be rolled back: {0}", ex), ex);
            }
        }
    }

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
     * @param rethrowSystemExceptions throw instead of log system failures (tx mgr only)
     * @throws Exception rollback exceptions
     */
    public void deliverToEndpoint(DeliveryResults result, ConnectionForMove connectionForMove, MessageEndpoint target,
        javax.jms.Message m, boolean rethrowSystemExceptions) throws Exception {
        if (sLog.isDebugEnabled()) {
            sLog.debug("Delivering message to endpoint");
        }
        
        // Stats
        mStats.aboutToDeliverMessage();
        
        boolean mustSetRollback = false;
        try {
            if (mActivation.shouldWrapAlways()) {
                m = wrapMsg(m);
            }
            boolean shouldDeliver = mRedeliveryChecker.shouldDeliver(connectionForMove, m);
            
            if (!shouldDeliver) {
                result.setOnMessageWasBypassed(true);
            } else {
                try {
                    registerThreadAsInOnMessage(true);
                    result.setOnMessageWasCalled(true);
                    ((javax.jms.MessageListener) target).onMessage(m);
                    result.setOnMessageSucceeded(true);
                } catch (RuntimeException ex) {
                    Exc.fixup(ex);
                    sLog.warn(LOCALE.x("E031: The entity the message was sent to for "
                        + "processing, threw an exception. The message will be "
                        + "rolled back. Exception: [{0}]", ex), ex);
                    result.setOnMessageFailed(true);
                    result.setException(ex);
                    result.setShouldDiscardEndpoint(true);
                    mRedeliveryChecker.rememberException(ex, m);
                    result.setRollbackOnly(true);
                    mustSetRollback = true;
                } finally {
                    registerThreadAsInOnMessage(false);
                }
            }
        } catch (Exception ex) {
            sLog.warn(LOCALE.x("E030: An unexpected exception was encountered " 
                + "processing a message. Exception: {0}", ex), ex);
        } finally {
            // Stats
            mStats.messageDelivered();
        }
        
        if (mustSetRollback) { 
            txSetRollbackOnly(result, rethrowSystemExceptions);
        }
    }
    
    private void registerThreadAsInOnMessage(boolean register) {
        synchronized (mThreadsCurrentlyInOnMessage) {
            if (register) {
                Object verify = mThreadsCurrentlyInOnMessage.put(Thread.currentThread(), "");
                XAssert.xassert(verify == null);
            } else {
                Object verify = mThreadsCurrentlyInOnMessage.remove(Thread.currentThread());
                XAssert.xassert(verify != null);
            }
        }
    }
    
    /**
     * @return true if this method is called from onMessage in an MDB belonging to 
     * this Delivery
     */
    public boolean isThisCalledFromOnMessage() {
        synchronized (mThreadsCurrentlyInOnMessage) {
            return mThreadsCurrentlyInOnMessage.containsKey(Thread.currentThread());
        }        
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
    public TransactionManager getTxMgr() throws Exception {
        return mTxMgr;
    }

    /**
     * Returns the current transaction
     * 
     * @param rethrowSystemExceptions throw instead of log
     * @return tx
     */
    protected Transaction getTransaction(boolean rethrowSystemExceptions) {
        Transaction tx = null;
        try {
            tx = mTxMgr.getTransaction();
        } catch (Exception e) {
            if (rethrowSystemExceptions) {
                throw Exc.rtexc(LOCALE.x("E062: Failed to obtain handle to transaction: {0}", e), e);
            }
            synchronized (mTxMgrCacheLock) {
                if (!mTxFailureLoggedOnce) {
                    mTxFailureLoggedOnce = true;
                    sLog.error(LOCALE.x("E062: Failed to obtain handle to transaction: {0}", e), e);
                }
            }
        }
        return tx;
    }
}
