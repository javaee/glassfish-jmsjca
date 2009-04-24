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
import com.stc.jmsjca.util.Str;

import javax.jms.ConnectionMetaData;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageListener;
import javax.jms.MessageProducer;
import javax.jms.Queue;
import javax.jms.QueueReceiver;
import javax.jms.QueueSender;
import javax.jms.QueueSession;
import javax.jms.Session;
import javax.jms.TemporaryQueue;
import javax.jms.TemporaryTopic;
import javax.jms.Topic;
import javax.jms.TopicPublisher;
import javax.jms.TopicSession;
import javax.jms.TopicSubscriber;
import javax.transaction.xa.XAResource;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Manages a Session; it holds a wrapper (WSession) and manages the JMS Session object
 * through a SessionConnection object. The application will call methods on the wrapper;
 * the wrapper will delegate these calls to the JMS Session object, and some of them to
 * the JSession.
 * A JSession always has exactly ONE SessionConnection object; this object interfaces with
 * the JMS runtime client.
 *
 * @author Frank Kieviet
 * @version $Revision: 1.13 $
 */
public class JSession {
    private static Logger sLog = Logger.getLogger(JSession.class);
    private SessionConnection mSessionConnection;
    private List mPooledProducers;
    private List mActiveProducers;
    private List mActiveConsumers;
    private XManagedConnection mManagedConnection;
    private int mCtExceptions;
    private Exception mFirstException;
    private Class mSessionClass;
    private int mSpecifiedAcknowledgeMode;

    private static final Localizer LOCALE = Localizer.get();

    /**
     * Constructor
     *
     * @param xa boolean
     * @param isTransacted boolean
     * @param acknowledgeMode int
     * @param sessionClass Class
     * @param mc XManagedConnection
     * @throws JMSException failure
     */
    public JSession(boolean xa, boolean isTransacted, int acknowledgeMode,
        Class sessionClass, XManagedConnection mc) throws JMSException {
        mManagedConnection = mc;
        mSessionClass = sessionClass;

        // Collect information to create SessionConnection
        RAJMSObjectFactory objfact = mc.getManagedConnectionFactory().getObjFactory();
        XConnectionRequestInfo descr = mc.getDescription();
        RAJMSResourceAdapter ra = (RAJMSResourceAdapter) mc.getManagedConnectionFactory().
            getResourceAdapter();

        // Create SessionConnection
        mSessionConnection = objfact.createSessionConnection(
            mc.getManagedConnectionFactory().getConnectionFactory(xa, descr),
            objfact, ra, mc, descr, xa,
            isTransacted, acknowledgeMode, sessionClass);

        mPooledProducers = new ArrayList();
        mActiveProducers = new ArrayList();
        mActiveConsumers = new ArrayList();
    }

    /**
     * Notification when a msg is sent so that the transactional state flag can be updated
     */
    public void onSend() {
        mManagedConnection.onSuccessfulOperation();
    }

    /**
     * Notification when a msg is received so that the transactional state flag can
     * be updated
     * 
     * @param msg msg that was received
     */
    public void onReceived(Message msg) {
        mManagedConnection.onSuccessfulOperation();
    }

    /**
     * Wraps a temporary destination so that the delete-method can be intercepted;
     * no effect on other destinations
     * 
     * @param destination Destination
     * @param connection JConnection
     * @return Destination
     */
    public Destination wrap(Destination destination, JConnection connection) {
        if (destination instanceof TemporaryQueue
            && !(destination instanceof WTemporaryQueue)) {
            return new WTemporaryQueue((TemporaryQueue) destination, connection);
        } else if (destination instanceof TemporaryTopic
            && !(destination instanceof WTemporaryTopic)) {
            return new WTemporaryTopic((TemporaryTopic) destination, connection);
        } else {
            return destination;
        }
    }

    /**
     * Called by commit/rollback so that the transactional state can be reset
     */
    public void onTransactionStateClean() {
    }

    /**
     * Called by MC.cleanup(): should remove all client dependent state
     */
    public void cleanup() {
        boolean producersInvalidated = false;

        // Close producers (add them to cache if necessary)
        for (Iterator it = mActiveProducers.iterator(); it.hasNext();/*-*/) {
            JProducer p = (JProducer) it.next();

            if (mManagedConnection.useProducerPooling()
                && p.canBePooled()
                && !producersInvalidated) {
                p.virtualClose();
                mPooledProducers.add(p);
            } else {
                p.physicalClose();
            }
            it.remove();
        }

        // Close consumers
        for (Iterator it = mActiveConsumers.iterator(); it.hasNext();/*-*/) {
            JConsumer m = (JConsumer) it.next();
            try {
                m.physicalClose();
            } catch (Exception ex) {
                sLog.warn(LOCALE.x("E038: The consumer of type {0} could not be closed " 
                    + "properly: {1}", m.getClass().getName(), ex), ex);
            }
            it.remove();
        }
    }

    /**
     * Called by the application server through the ManagedConnection; the session will be
     * discarded after this call, so all connections etc need to be cleaned up
     * permanently.
     *
     * @throws JMSException failure
     */
    public void destroy() throws JMSException {
        mActiveConsumers.clear();
        mActiveProducers.clear();
        mPooledProducers.clear();
        if (mSessionConnection != null) {
            mSessionConnection.destroy();
            mSessionConnection = null;
        }
    }

    /**
     * Returns the JMSSession that communicates with the server; is called whenever the
     * application calls a JMS function on the JMS Session that it has a wrapper of. The
     * returned session is guaranteed to be of the correct type (XA, ...)
     *
     * @return Object javax.jms.Session
     * @throws JMSException failure
     */
    public Object getDelegate() throws JMSException {
        return mSessionConnection.getJmsSession();
    }

    /**
     * Called by the MC which in turn is called by the appserver to return the XAResource
     *
     * @throws JMSException failure
     * @return XAResource
     */
    public XAResource getXAResource() throws JMSException {
        // Ensure type is actuated
        getDelegate();
        return mSessionConnection.getXAResource();
    }

    /**
     * setClientID
     *
     * @param clientID String
     * @throws JMSException on failure
     */
    public void setClientID(String clientID) throws JMSException {
        getDelegate();
        mSessionConnection.setClientID(clientID);
    }

    /**
     * Called when the application calls close() on a producer
     *
     * @param p producer
     */
    public void notifyProducerClosedByApplication(JProducer p) {
        mActiveProducers.remove(p);
        if (!mManagedConnection.useProducerPooling() || !p.canBePooled()) {
            p.physicalClose();
        } else {
            p.virtualClose();
            addToPool(p);
        }
    }

    /**
     * Called when the application calls close() on a consumer
     *
     * @param p producer
     */
    public void notifyConsumerClosedByApplication(JConsumer p) {
        mActiveConsumers.remove(p);
        p.physicalClose();
    }

    /**
     * getConnectionMetaData (called from JConnection)
     *
     * @throws JMSException on failure
     * @return ConnectionMetaData
     */
    public ConnectionMetaData getConnectionMetaData() throws JMSException {
        getDelegate();
        return mSessionConnection.getConnectionMetaData();
    }

    /**
     * Called when the application calls the JMS method "start()" on the JMS Connection
     * object that it has.
     *
     * @throws JMSException failure
     */
    public void start() throws JMSException {
        getDelegate();
        mSessionConnection.start();
    }

    /**
     * Called when the application calls the JMS method "stop()" on the JMS Connection
     * object that it has.
     *
     * @throws JMSException failure
     */
    public void stop() throws JMSException {
        mSessionConnection.stop();
    }

    /**
     * Tries to locate a session from the pool with the specified signature. Returns
     * null if not found.
     *
     * @param signature specifies characteristics of the session (transacted, etc)
     * @return null if not found
     */
    private JProducer getFromPool(String signature) {
        JProducer ret = null;
        for (Iterator it = mPooledProducers.iterator(); it.hasNext();/*-*/) {
            JProducer p = (JProducer) it.next();
            if (p.getSignature().equals(signature)) {
                ret = p;
                it.remove();
                break;
            }
        }
        return ret;
    }

    /**
     * Pools a producer
     *
     * @param p JProducer
     */
    private void addToPool(JProducer p) {
        mPooledProducers.add(p);
    }

    /**
     * Creates a producer
     */
    public interface ProducerCreator {

        /**
         * createProducer
         *
         * @throws JMSException failure
         * @return MessageProducer
         */
        public MessageProducer createProducer() throws JMSException;
    }

    /**
     * Tool function: creates a session or gets one from the pool
     *
     * @param signature summarizes the characteristics of the session
     * @param producerClass the return class
     * @param isTemp boolean
     * @param creator little object factory that creates a new session if necessary
     * @param connection JConnection
     * @return new or reused session
     * @throws JMSException on failure
     */
    public MessageProducer createProducer(String signature, Class producerClass,
        boolean isTemp, ProducerCreator creator, JConnection connection) throws JMSException {
        JProducer wrapped = null;

        // Actuate
        getDelegate();

        // Try to get from pool
        if (mManagedConnection.useProducerPooling()) {
            wrapped = getFromPool(signature);
        }

        // Create new one if necessary
        if (wrapped == null) {
            MessageProducer producer = creator.createProducer();
            wrapped = new JProducer(producerClass, this, producer, signature, isTemp);
        }

        mActiveProducers.add(wrapped);
        
        wrapped.setConnection(connection);

        return (MessageProducer) wrapped.getWrapper();
    }

    /**
     * For testing only
     *
     * @return XManagedConnection
     */
    public XManagedConnection getManagedConnection() {
        return mManagedConnection;
    }


    /**
     * Called when an invocation exception has occurred
     *
     * @param ex Throwable
     */
    public void exceptionOccurred(Throwable ex) {
        Exception exception;
        if (ex instanceof Exception) {
            exception = (Exception) ex;
        } else {
            exception = new Exception("Runtime exception: " + ex);
            Exc.setCause(exception, ex);
        }
        
        if (ex instanceof JMSException) {
            Exc.checkLinkedException(ex);
        }

        if (mFirstException == null) {
            mFirstException = exception;
        }
        mCtExceptions++;

        mManagedConnection.notifyConnectionErrorOccured(exception);
    }

    /**
     * True if any exception has occurred
     *
     * @return true if occurred
     */
    public boolean hasExceptionOccurred() {
        return mCtExceptions > 0;
    }

    /**
     * Returns the first exception that was thrown on any of the intercepted calls.
     *
     * @return java.lang.Exception
     */
    public Exception getFirstException() {
        return mFirstException;
    }

    /**
     * Returns the total count of exceptions that occurred on any intercepted call.
     *
     * @return number of exceptions
     */
    public int getExceptionCount() {
        return mCtExceptions;
    }

    /**
     * Creates a new wrapper and invalidates any existing current wrapper.
     *
     * @return WSession
     */
    public WSession createHandle() {
        if (mSessionClass == javax.jms.Session.class) {
            return new WSession(this);
        } else if (mSessionClass == javax.jms.QueueSession.class) {
            return new WQueueSession(this);
        } else if (mSessionClass == javax.jms.TopicSession.class) {
            return new WTopicSession(this);
        } else {
            throw Exc.rtexc(LOCALE.x("E131: Unknown class: {0}", mSessionClass));
        }
    }

    /**
     * To pass the CTS, this will solve the following problem: getAcknowledgeMode()
     * should return the value specified when the session was created, irrespective of
     * the fact that the ack mode is irrelevant because the session was set to transacted
     * e.g.
     * Session sess = conn.createSession(false, Session.AUTO_ACKNOWLEDGE);
     * sess.getAcknowledgeMode() should return Session.AUTO_ACKNOWLEDGE
     *
     * @param orgAckmode int
     */
    public void setSpecifiedAcknowledgeMode(int orgAckmode) {
        mSpecifiedAcknowledgeMode = orgAckmode;
    }

    // INTERCEPTED METHODS

    /**
     * Special: does NOT close the physical connection, but notifies the managed
     * connection that the connection is closed and hence that the connection can be
     * returned to the pool.
     *
     * @throws JMSException failure
     * @param w WSession
     */
    public void close(WSession w) throws JMSException {
        Exception closeException = null;
        try {
            stop();
        } catch (Exception e) {
            closeException = e;
        }
        
        // The expectation is that cleanup() will be called by the MC as a result of 
        // the application server calling cleanup() on the MC. This is expectation is 
        // that the application server will do that upon receiving the close event.
        // It appears that's not the case in GF v2.1, so do it here right now.
        cleanup();
        
        // Notify the application server
        mManagedConnection.notifyClosedByApplicationConnection(w);
        
        if (closeException != null) {
            throw Exc.jmsExc(LOCALE.x(
                "E094: This {0} could not be closed properly: {1}", w,
                closeException), closeException);            
        }
    }

    /**
     * Override, from JMS api
     *
     * @return obj
     * @throws JMSException on failure
     * @param queue Queue
     * @param connection JConnection
     */
    public QueueReceiver createReceiver(Queue queue, JConnection connection) throws JMSException {
        QueueReceiver delegate = ((QueueSession) getDelegate()).createReceiver(queue);
        JConsumer wrapped = new JConsumer(QueueReceiver.class, this, delegate, null, connection);
        mActiveConsumers.add(wrapped);
        return (QueueReceiver) wrapped.getWrapper();
    }

    /**
     * Override, from JMS api
     *
     * @return obj
     * @throws JMSException on failure
     * @param queue Queue
     * @param messageSelector String
     * @param connection JConnection
     */
    public QueueReceiver createReceiver(Queue queue, String messageSelector,
        JConnection connection) throws JMSException {
        QueueReceiver delegate = ((QueueSession) getDelegate()).createReceiver(queue, messageSelector);
        JConsumer wrapped = new JConsumer(QueueReceiver.class, this, delegate, null, connection);
        mActiveConsumers.add(wrapped);
        return (QueueReceiver) wrapped.getWrapper();
    }

    /**
     * Override, from JMS api
     *
     * @return obj
     * @throws JMSException on failure
     * @param queue Queue
     * @param connection JConnection
     */
    public QueueSender createSender(final Queue queue, JConnection connection) 
    throws JMSException {
        return (QueueSender) createProducer("QueueSender:" + (queue != null ? queue.getQueueName() : "-"),
            QueueSender.class,
            queue != null && queue instanceof TemporaryQueue,
            new ProducerCreator() {
            public MessageProducer createProducer() throws JMSException {
                return ((QueueSession) getDelegate()).createSender(queue);
            }
        }, connection);
    }

    /**
     * Override, from JMS api
     *
     * @param topic Topic
     * @param connection JConnection
     * @return javax.jms.TopicPublisher
     * @throws JMSException failure
     */
    public TopicPublisher createPublisher(final Topic topic, JConnection connection)
    throws JMSException {
        return (TopicPublisher) createProducer("TopicPublisher:" + (topic != null ? topic.getTopicName() : "-"),
            TopicPublisher.class,
            topic != null && topic instanceof TemporaryTopic,
            new ProducerCreator() {
            public MessageProducer createProducer() throws JMSException {
                return ((TopicSession) getDelegate()).createPublisher(topic);
            }
        }, connection);
    }

    /**
     * Override, from JMS api
     *
     * @param topic Topic
     * @param connection JConnection
     * @return javax.jms.TopicSubscriber
     * @throws JMSException failure
     */
    public TopicSubscriber createSubscriber(Topic topic, JConnection connection) throws JMSException {
        TopicSubscriber delegate = ((TopicSession) getDelegate()).createSubscriber(topic);
        JConsumer wrapped = new JConsumer(TopicSubscriber.class, this, delegate, null, connection);
        mActiveConsumers.add(wrapped);
        return (TopicSubscriber) wrapped.getWrapper();
    }

    /**
     * Override, from JMS api
     *
     * @param topic Topic
     * @param messageSelector String
     * @param noLocal boolean
     * @param connection JConnection
     * @return javax.jms.TopicSubscriber
     * @throws JMSException failure
     */
    public TopicSubscriber createSubscriber(Topic topic, String messageSelector,
        boolean noLocal, JConnection connection) throws JMSException {
        TopicSubscriber delegate = ((TopicSession) getDelegate()).createSubscriber(topic, messageSelector, noLocal);
        JConsumer wrapped = new JConsumer(TopicSubscriber.class, this, delegate, null, connection);
        mActiveConsumers.add(wrapped);
        return (TopicSubscriber) wrapped.getWrapper();
    }

    /**
     * Override, from JMS api
     *
     * @param topic Topic
     * @param name String
     * @param connection JConnection
     * @return javax.jms.TopicSubscriber
     * @throws JMSException failure
     */
    public TopicSubscriber createDurableSubscriber(Topic topic, String name,
        JConnection connection) throws JMSException {
        TopicSubscriber delegate = ((Session) getDelegate()).createDurableSubscriber(topic, name);
        JConsumer wrapped = new JConsumer(TopicSubscriber.class, this, delegate, null, connection);
        mActiveConsumers.add(wrapped);
        return (TopicSubscriber) wrapped.getWrapper();
    }

    /**
     * Override, from JMS api
     *
     * @param topic Topic
     * @param name String
     * @param messageSelector String
     * @param noLocal boolean
     * @param connection JConnection
     * @return javax.jms.TopicSubscriber
     * @throws JMSException failure
     */
    public TopicSubscriber createDurableSubscriber(Topic topic, String name,
        String messageSelector, boolean noLocal, JConnection connection) throws JMSException {
        TopicSubscriber delegate = ((Session) getDelegate()).createDurableSubscriber(
            topic, name, messageSelector, noLocal);
        JConsumer wrapped = new JConsumer(TopicSubscriber.class, this, delegate, null, connection);
        mActiveConsumers.add(wrapped);
        return (TopicSubscriber) wrapped.getWrapper();
    }

    /**
     * See JMS api 1.1
     *
     * @param destination Destination
     * @param connection JConnection
     * @throws JMSException delegated
     * @return MessageProducer
     */
    public MessageProducer createProducer(final Destination destination, 
        JConnection connection) throws JMSException {
        String name = "-";
        if (destination != null) {
            if (destination instanceof Queue) {
                name = ((Queue) destination).getQueueName();
            } else {
                name = ((Topic) destination).getTopicName();
            }
        }
        return (MessageProducer) createProducer("MessageProducer:" + name,
            MessageProducer.class,
            destination != null && (destination instanceof TemporaryQueue
            || destination instanceof TemporaryTopic),
            new ProducerCreator() {
            public MessageProducer createProducer() throws JMSException {
                return ((Session) getDelegate()).createProducer(destination);
            }
        }, connection);
    }

    /**
     * See JMS api 1.1
     *
     * @param destination Destination
     * @param connection JConnection
     * @throws JMSException failure
     * @return MessageConsumer
     */
    public MessageConsumer createConsumer(Destination destination, JConnection connection) throws JMSException {
        MessageConsumer delegate = ((Session) getDelegate()).createConsumer(destination);
        JConsumer wrapped = new JConsumer(MessageConsumer.class, this, delegate, null, connection);
        mActiveConsumers.add(wrapped);
        return (MessageConsumer) wrapped.getWrapper();
    }

    /**
     * See JMS api 1.1
     *
     * @param destination Destination
     * @param messageSelector String
     * @param connection JConnection
     * @throws JMSException failure
     * @return MessageConsumer
     */
    public MessageConsumer createConsumer(Destination destination,
        String messageSelector, JConnection connection) throws JMSException {
        MessageConsumer delegate = ((Session) getDelegate()).createConsumer(destination,
            messageSelector);
        JConsumer wrapped = new JConsumer(MessageConsumer.class, this, delegate, null, connection);
        mActiveConsumers.add(wrapped);
        return (MessageConsumer) wrapped.getWrapper();
    }

    /**
     * See JMS api 1.1
     *
     * @param destination Destination
     * @param messageSelector String
     * @param noLocal boolean
     * @param connection JConnection
     * @throws JMSException failure
     * @return MessageConsumer
     */
    public MessageConsumer createConsumer(Destination destination,
        String messageSelector,  boolean noLocal, JConnection connection) throws JMSException {
        MessageConsumer delegate = ((Session) getDelegate()).createConsumer(destination,
            messageSelector, noLocal);
        JConsumer wrapped = new JConsumer(MessageConsumer.class, this, delegate, null, connection);
        mActiveConsumers.add(wrapped);
        return (MessageConsumer) wrapped.getWrapper();
    }

    /**
     * Override, from JMS api
     *
     * @param listener MessageListener
     * @throws JMSException failure
     */
    public void setMessageListener(MessageListener listener) throws JMSException {
        throw Exc.jmsExc(LOCALE.x("E129: ExceptionListeners cannot be set in a JCA 1.5 connection"));
    }

    /**
     * See JMS
     *
     * @throws JMSException if the JMS provider fails to commit the transaction due to
     *   some internal error.
     * @param w WSession
     */
    public void commit(WSession w) throws JMSException {
        ((Session) getDelegate()).commit();
        if (w != null) {
            mManagedConnection.notifyCommit(w);
        }
        onTransactionStateClean();
    }

    /**
     * See JMS
     *
     * @throws JMSException if the JMS provider fails to commit the transaction due to
     *   some internal error.
     * @param w WSession
     */
    public void rollback(WSession w) throws JMSException {
        ((Session) getDelegate()).rollback();
        if (w != null) {
            mManagedConnection.notifyRollback(w);
        }
        onTransactionStateClean();
    }

    /**
     * see JMS
     * see setSpecifiedAcknowledgeMode()
     *
     * @return int
     * @throws JMSException on failure
     */
    public int getAcknowledgeMode() throws JMSException {
        return mSpecifiedAcknowledgeMode;
    }

    /**
     * recover
     *
     * @throws JMSException on failure
     */
    public void recover() throws JMSException {
        if (mSessionConnection.isXA()) {
            // Special behavior (required by CTS)
            // Ignore
        } else {
            ((Session) getDelegate()).recover();
        }
    }

    /**
     * Creates a destination; used for interceptors such as WebLogic
     * 
     * @param name destination name
     * @return destination
     * @throws JMSException on failure
     */
    public Topic createTopic(String name) throws JMSException {
        Destination ret = getDestination(name, false);
        if (ret == null) {
            return mSessionConnection.createTopic(name);
        } if (ret instanceof AdminDestination) {
            return (Topic) createDestination((AdminDestination) ret);
        } else {
            return (Topic) ret;
        }
    }

    /**
     * Creates a destination; used for interceptors such as WebLogic
     * 
     * @param name destination name
     * @return destination
     * @throws JMSException on failure
     */
    public Queue createQueue(String name) throws JMSException  {
        Destination ret = getDestination(name, true);
        if (ret == null) {
            return mSessionConnection.createQueue(name);
        } if (ret instanceof AdminDestination) {
            return (Queue) createDestination((AdminDestination) ret);
        } else {
            return (Queue) ret;
        }
    }
    
    private Destination getDestination(String name, boolean isQueue) throws JMSException {
        if (Str.empty(name)) {
            throw Exc.jmsExc(LOCALE.x("E095: The destination should not be empty or null"));
        }
        Destination ret = null; 
        if (name.startsWith("jndi://")) {
            if (isQueue) {
                AdminQueue d = new AdminQueue();
                d.setName(name);
                ret = d;
            } else {
                AdminTopic d = new AdminTopic();
                d.setName(name);
                ret = d;
            }
        } else {
            ret = mManagedConnection.getManagedConnectionFactory().getObjFactory().adminDestinationLookup(name);
        }
        return ret;
    }
    
    /**
     * Creates a JMS client specific destination based on an administrative object
     * 
     * @param dest administrative object
     * @return JMS client specific destination
     * @throws JMSException propagated
     */
    public Destination createDestination(AdminDestination dest) throws JMSException {
        return mSessionConnection.createDestination(dest);
    }
    
    /**
     * Converts optionally from a genericra destination to an admin destination
     * 
     * @param d destination to inspect
     * @return admin destination or same destination
     * @throws JMSException on conversion failure
     */
    public Destination checkGeneric(Destination d) throws JMSException {
        return mSessionConnection.checkGeneric(d);
    }
}
