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

import java.io.Serializable;
import javax.jms.BytesMessage;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.MapMessage;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageListener;
import javax.jms.MessageProducer;
import javax.jms.ObjectMessage;
import javax.jms.Queue;
import javax.jms.QueueBrowser;
import javax.jms.Session;
import javax.jms.StreamMessage;
import javax.jms.TemporaryQueue;
import javax.jms.TemporaryTopic;
import javax.jms.TextMessage;
import javax.jms.Topic;
import javax.jms.TopicSubscriber;

/**
 * A wrapper around a javax.jms.Session; this wrapper is given out to the application
 * code. The Application will call methods in this wrapper; the wrapper will delegate the
 * calls to the JSession
 *
 * @author Frank Kieviet
 * @version $Revision: 1.9 $
 */
public class WSession implements IWSession {
    private static final Localizer LOCALE = Localizer.get();

    /**
     * The currently associated owner of this wrapper
     */
    protected JSession mMgr;
    private JSession mOwner;
    private JConnection mCon;

    /**
     * WSession
     *
     * @param mgr JSession
     */
    public WSession(JSession mgr) {
        mMgr = mgr;
        mOwner = mgr;
    }

    /**
     * Gets the owner of this wrapper
     *
     * @return JSession
     */
    public JSession getJSession() {
        return mOwner;
    }

    /**
     * setJSession
     *
     * @param j JSession
     */
    public void setJSession(JSession j) {
        mOwner = j;
        mMgr = j;
    }

    /**
     * setConnection
     *
     * @param c JConnection
     */
    public void setConnection(JConnection c) {
        mCon = c;
    }
    
    /**
     * Returns the connection that created this session
     * 
     * @return JConnection
     */
    public JConnection getConnection() {
        return mCon;
    }
    
    /**
     * Checks if a destination belongs to the connection that created this session
     * 
     * @param dest destination to check
     * @throws JMSException if the connection does not match the connection that created
     * this session
     */
    public void checkTemporaryDestinationOwnership(Destination dest) throws JMSException {
        if (dest instanceof LimitationJConnection) {
            LimitationJConnection limited = (LimitationJConnection) dest;
            if (limited.getConnection() != getConnection()) {
                throw Exc.jmsExc(LOCALE.x("E154: Temporary destination can only be used in the same connection"));
            }
        }
    }

    /**
     * Throws an exception
     *
     * @throws javax.jms.IllegalStateException always
     */
    public void invokeOnClosed() throws javax.jms.IllegalStateException {
        throw Exc.illstate(LOCALE.x("E153: This {0} is closed", "Session"));
    }

    /**
     * Marks the wrapper as closed
     */
    public void setClosed() {
        mMgr = null;
    }

    /**
     * Throws an exception when closed
     *
     * @throws javax.jms.IllegalStateException when closed
     */
    protected void checkClosed() throws javax.jms.IllegalStateException {
        if (mMgr == null) {
            invokeOnClosed();
        }
    }

    /**
     * createBytesMessage
     *
     * @return BytesMessage
     * @throws JMSException on failure
     */
    public BytesMessage createBytesMessage() throws JMSException {
        checkClosed();
        try {
            return new WBytesMessageOut(((Session) mMgr.getDelegate()).createBytesMessage());
        } catch (JMSException e) {
            mMgr.exceptionOccurred(e);
            throw e;
        }
    }

    /**
     * createMapMessage
     *
     * @return MapMessage
     * @throws JMSException on failure
     */
    public MapMessage createMapMessage() throws JMSException {
        checkClosed();
        try {
            return new WMapMessageOut(((Session) mMgr.getDelegate()).createMapMessage());
        } catch (JMSException e) {
            mMgr.exceptionOccurred(e);
            throw e;
        }
    }

    /**
     * createMessage
     *
     * @return Message
     * @throws JMSException on failure
     */
    public Message createMessage() throws JMSException {
        checkClosed();
        try {
            return new WMessageOut(((Session) mMgr.getDelegate()).createMessage());
        } catch (JMSException e) {
            mMgr.exceptionOccurred(e);
            throw e;
        }
    }

    /**
     * createObjectMessage
     *
     * @return ObjectMessage
     * @throws JMSException on failure
     */
    public ObjectMessage createObjectMessage() throws JMSException {
        checkClosed();
        try {
            return new WObjectMessageOut(((Session) mMgr.getDelegate()).createObjectMessage());
        } catch (JMSException e) {
            mMgr.exceptionOccurred(e);
            throw e;
        }
    }

    /**
     * createObjectMessage
     *
     * @param serializable Serializable
     * @return ObjectMessage
     * @throws JMSException on failure
     */
    public ObjectMessage createObjectMessage(Serializable serializable) throws
        JMSException {
        checkClosed();
        try {
            return new WObjectMessageOut(((Session) mMgr.getDelegate()).createObjectMessage(serializable));
        } catch (JMSException e) {
            mMgr.exceptionOccurred(e);
            throw e;
        }
    }

    /**
     * createStreamMessage
     *
     * @return StreamMessage
     * @throws JMSException on failure
     */
    public StreamMessage createStreamMessage() throws JMSException {
        checkClosed();
        try {
            return new WStreamMessageOut(((Session) mMgr.getDelegate()).createStreamMessage());
        } catch (JMSException e) {
            mMgr.exceptionOccurred(e);
            throw e;
        }
    }

    /**
     * createTextMessage
     *
     * @return TextMessage
     * @throws JMSException on failure
     */
    public TextMessage createTextMessage() throws JMSException {
        checkClosed();
        try {
            return new WTextMessageOut(((Session) mMgr.getDelegate()).createTextMessage());
        } catch (JMSException e) {
            mMgr.exceptionOccurred(e);
            throw e;
        }
    }

    /**
     * createTextMessage
     *
     * @param string String
     * @return TextMessage
     * @throws JMSException on failure
     */
    public TextMessage createTextMessage(String string) throws JMSException {
        checkClosed();
        try {
            return new WTextMessageOut(((Session) mMgr.getDelegate()).createTextMessage(string));
        } catch (JMSException e) {
            mMgr.exceptionOccurred(e);
            throw e;
        }
    }

    /**
     * getTransacted
     *
     * @return boolean
     * @throws JMSException on failure
     */
    public boolean getTransacted() throws JMSException {
        checkClosed();
        try {
            return ((Session) mMgr.getDelegate()).getTransacted();
        } catch (JMSException e) {
            mMgr.exceptionOccurred(e);
            throw e;
        }
    }

    /**
     * getAcknowledgeMode
     *
     * @return int
     * @throws JMSException on failure
     */
    public int getAcknowledgeMode() throws JMSException {
        checkClosed();
        try {
            return mMgr.getAcknowledgeMode();
        } catch (JMSException e) {
            mMgr.exceptionOccurred(e);
            throw e;
        }
    }

    /**
     * commit
     *
     * @throws JMSException on failure
     */
    public void commit() throws JMSException {
        checkClosed();
        try {
            mMgr.commit(this);
        } catch (JMSException e) {
            mMgr.exceptionOccurred(e);
            throw e;
        }
    }

    /**
     * rollback
     *
     * @throws JMSException on failure
     */
    public void rollback() throws JMSException {
        checkClosed();
        try {
            mMgr.rollback(this);
        } catch (JMSException e) {
            mMgr.exceptionOccurred(e);
            throw e;
        }
    }

    /**
     * close
     *
     * @throws JMSException on failure
     */
    public void close() throws JMSException {
        Exception closeException = null;
        if (mMgr != null) {
            try {
                mMgr.close(this);
            } catch (Exception e) {
                closeException = e;
            }
            mMgr = null;
        }
        mCon.notifyWSessionClosedByApplication(this);
        
        if (closeException != null) {
            throw Exc.jmsExc(LOCALE.x(
                "E094: This {0} could not be closed properly: {1}", Session.class.getName(),
                closeException), closeException);            
        }
    }

    /**
     * recover
     *
     * @throws JMSException on failure
     */
    public void recover() throws JMSException {
        checkClosed();
        try {
            mMgr.recover();
        } catch (JMSException e) {
            mMgr.exceptionOccurred(e);
            throw e;
        }
    }

    /**
     * getMessageListener
     *
     * @return MessageListener
     * @throws JMSException on failure
     */
    public MessageListener getMessageListener() throws JMSException {
        checkClosed();
        try {
            return ((Session) mMgr.getDelegate()).getMessageListener();
        } catch (JMSException e) {
            mMgr.exceptionOccurred(e);
            throw e;
        }
    }

    /**
     * setMessageListener
     *
     * @param messageListener MessageListener
     * @throws JMSException on failure
     */
    public void setMessageListener(MessageListener messageListener) throws JMSException {
        checkClosed();
        try {
            ((Session) mMgr.getDelegate()).setMessageListener(messageListener);
        } catch (JMSException e) {
            mMgr.exceptionOccurred(e);
            throw e;
        }
    }

    /**
     * run
     */
    public void run() {
        if (mMgr == null) {
            throw Exc.rtexc(LOCALE.x("E160: Illegal call: run() is not supported"));
        }
    }
    
    /**
     * createProducer
     *
     * @param destination Destination
     * @return MessageProducer
     * @throws JMSException on failure
     */
    public MessageProducer createProducer(Destination destination) throws JMSException {
        checkClosed();
        destination = mMgr.checkGeneric(destination);
        if (destination instanceof AdminDestination) {
            destination = mMgr.createDestination((AdminDestination) destination); 
        }
        if (destination instanceof Unwrappable) {
            destination = (Destination) ((Unwrappable) destination).getWrappedObject();
        }
        try {
            return mMgr.createProducer(destination, mCon);
        } catch (JMSException e) {
            mMgr.exceptionOccurred(e);
            throw e;
        }
    }

    /**
     * createConsumer
     *
     * @param destination Destination
     * @return MessageConsumer
     * @throws JMSException on failure
     */
    public MessageConsumer createConsumer(Destination destination) throws JMSException {
        checkClosed();
        checkTemporaryDestinationOwnership(destination);
        destination = mMgr.checkGeneric(destination);
        if (destination instanceof AdminDestination) {
            destination = mMgr.createDestination((AdminDestination) destination); 
        }
        if (destination instanceof Unwrappable) {
            destination = (Destination) ((Unwrappable) destination).getWrappedObject();
        }
        try {
            return mMgr.createConsumer(destination, getConnection());
        } catch (JMSException e) {
            mMgr.exceptionOccurred(e);
            throw e;
        }
    }

    /**
     * createConsumer
     *
     * @param destination Destination
     * @param string String
     * @return MessageConsumer
     * @throws JMSException on failure
     */
    public MessageConsumer createConsumer(Destination destination,
        String string) throws JMSException {
        checkClosed();
        checkTemporaryDestinationOwnership(destination);
        destination = mMgr.checkGeneric(destination);
        if (destination instanceof AdminDestination) {
            destination = mMgr.createDestination((AdminDestination) destination); 
        }
        if (destination instanceof Unwrappable) {
            destination = (Destination) ((Unwrappable) destination).getWrappedObject();
        }
        try {
            return ((Session) mMgr.getDelegate()).createConsumer(destination, string);
        } catch (JMSException e) {
            mMgr.exceptionOccurred(e);
            throw e;
        }
    }

    /**
     * createConsumer
     *
     * @param destination Destination
     * @param string String
     * @param boolean2 boolean
     * @return MessageConsumer
     * @throws JMSException on failure
     */
    public MessageConsumer createConsumer(Destination destination, String string,
        boolean boolean2) throws JMSException {
        checkClosed();
        checkTemporaryDestinationOwnership(destination);
        destination = mMgr.checkGeneric(destination);
        if (destination instanceof AdminDestination) {
            destination = mMgr.createDestination((AdminDestination) destination); 
        }
        if (destination instanceof Unwrappable) {
            destination = (Destination) ((Unwrappable) destination).getWrappedObject();
        }
        try {
            return mMgr.createConsumer(destination, string, boolean2, getConnection());
        } catch (JMSException e) {
            mMgr.exceptionOccurred(e);
            throw e;
        }
    }
    
    /**
     * createQueue
     *
     * @param string String
     * @return Queue
     * @throws JMSException on failure
     */
    public Queue createQueue(String string) throws JMSException {
        checkClosed();
        try {
            return mMgr.createQueue(string);
        } catch (JMSException e) {
            mMgr.exceptionOccurred(e);
            throw e;
        }
    }

    /**
     * createTopic
     *
     * @param string String
     * @return Topic
     * @throws JMSException on failure
     */
    public Topic createTopic(String string) throws JMSException {
        checkClosed();
        try {
            return mMgr.createTopic(string);
        } catch (JMSException e) {
            mMgr.exceptionOccurred(e);
            throw e;
        }
    }

    /**
     * createDurableSubscriber
     *
     * @param topic Topic
     * @param string String
     * @return TopicSubscriber
     * @throws JMSException on failure
     */
    public TopicSubscriber createDurableSubscriber(Topic topic,
        String string) throws JMSException {
        checkClosed();
        checkTemporaryDestinationOwnership(topic);
        if (topic instanceof AdminDestination && ((AdminDestination) topic).isTopic()) {
            topic = (Topic) mMgr.createDestination((AdminDestination) topic); 
        }
        if (topic instanceof Unwrappable) {
            topic = (Topic) ((Unwrappable) topic).getWrappedObject();
        }
        try {
            return mMgr.createDurableSubscriber(topic, string, getConnection());
        } catch (JMSException e) {
            mMgr.exceptionOccurred(e);
            throw e;
        }
    }

    /**
     * createDurableSubscriber
     *
     * @param topic Topic
     * @param string String
     * @param string2 String
     * @param boolean3 boolean
     * @return TopicSubscriber
     * @throws JMSException on failure
     */
    public TopicSubscriber createDurableSubscriber(Topic topic, String string,
        String string2, boolean boolean3) throws JMSException {
        checkClosed();
        checkTemporaryDestinationOwnership(topic);
        if (topic instanceof AdminDestination && ((AdminDestination) topic).isTopic()) {
            topic = (Topic) mMgr.createDestination((AdminDestination) topic); 
        }
        if (topic instanceof Unwrappable) {
            topic = (Topic) ((Unwrappable) topic).getWrappedObject();
        }
        try {
            return mMgr.createDurableSubscriber(topic, string, string2, boolean3, getConnection());
        } catch (JMSException e) {
            mMgr.exceptionOccurred(e);
            throw e;
        }
    }

    /**
     * createBrowser
     *
     * @param queue Queue
     * @return QueueBrowser
     * @throws JMSException on failure
     */
    public QueueBrowser createBrowser(Queue queue) throws JMSException {
        checkClosed();
        if (queue instanceof AdminDestination && ((AdminDestination) queue).isQueue()) {
            queue = (Queue) mMgr.createDestination((AdminDestination) queue); 
        }
        if (queue instanceof Unwrappable) {
            queue = (Queue) ((Unwrappable) queue).getWrappedObject();
        }
        try {
            return ((Session) mMgr.getDelegate()).createBrowser(queue);
        } catch (JMSException e) {
            mMgr.exceptionOccurred(e);
            throw e;
        }
    }

    /**
     * createBrowser
     *
     * @param queue Queue
     * @param string String
     * @return QueueBrowser
     * @throws JMSException on failure
     */
    public QueueBrowser createBrowser(Queue queue, String string) throws JMSException {
        checkClosed();
        if (queue instanceof AdminDestination && ((AdminDestination) queue).isQueue()) {
            queue = (Queue) mMgr.createDestination((AdminDestination) queue); 
        }
        if (queue instanceof Unwrappable) {
            queue = (Queue) ((Unwrappable) queue).getWrappedObject();
        }
        try {
            return ((Session) mMgr.getDelegate()).createBrowser(queue, string);
        } catch (JMSException e) {
            mMgr.exceptionOccurred(e);
            throw e;
        }
    }

    /**
     * createTemporaryQueue
     *
     * @return TemporaryQueue
     * @throws JMSException on failure
     */
    public TemporaryQueue createTemporaryQueue() throws JMSException {
        checkClosed();
        try {
            TemporaryQueue ret = ((Session) mMgr.getDelegate()).createTemporaryQueue();
            getConnection().addTemporaryDestination(ret);
            ret = new WTemporaryQueue(ret, getConnection());
            return ret;
        } catch (JMSException e) {
            mMgr.exceptionOccurred(e);
            throw e;
        }
    }

    /**
     * createTemporaryTopic
     *
     * @return TemporaryTopic
     * @throws JMSException on failure
     */
    public TemporaryTopic createTemporaryTopic() throws JMSException {
        checkClosed();
        try {
            TemporaryTopic ret = ((Session) mMgr.getDelegate()).createTemporaryTopic();
            getConnection().addTemporaryDestination(ret);
            ret = new WTemporaryTopic(ret, getConnection());
            return ret;
        } catch (JMSException e) {
            mMgr.exceptionOccurred(e);
            throw e;
        }
    }

    /**
     * unsubscribe
     *
     * @param string String
     * @throws JMSException on failure
     */
    public void unsubscribe(String string) throws JMSException {
        checkClosed();
        try {
            ((Session) mMgr.getDelegate()).unsubscribe(string);
        } catch (JMSException e) {
            mMgr.exceptionOccurred(e);
            throw e;
        }
    }

    /**
     * @see com.stc.jmsjca.core.IWSession#getReference()
     */
    public WSession getReference() {
        return this;
    }
}
