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
import javax.jms.QueueSession;
import javax.jms.Session;
import javax.jms.StreamMessage;
import javax.jms.TemporaryQueue;
import javax.jms.TemporaryTopic;
import javax.jms.TextMessage;
import javax.jms.Topic;
import javax.jms.TopicSession;
import javax.jms.TopicSubscriber;
import javax.jms.XAQueueSession;
import javax.jms.XASession;
import javax.jms.XATopicSession;
import javax.transaction.xa.XAResource;

import java.io.Serializable;


/**
 * Wraps around a transacted session to make it look like an XASession; this can be
 * used by adapters that don't support XA, such as WebLogic.
 *
 * @author fkieviet
 * @version $Revision: 1.5 $
 */
public class PseudoXASession implements XASession, XAQueueSession, XATopicSession {
    private Session mSession;
    private PseudoXAResource mXAResource;
    

    /**
     * Constructor
     * 
     * @param session session
     * @throws JMSException On failure
     */
    public PseudoXASession(Session session) throws JMSException {
        mSession = session;
        mXAResource = new PseudoXAResource(mSession);
    }

    /**
     * @see javax.jms.XASession#getSession()
     */
    public Session getSession() throws JMSException {
        return mSession;
    }

    /**
     * @see javax.jms.XASession#getXAResource()
     */
    public XAResource getXAResource() {
        return mXAResource;
    }

    /**
     * @see javax.jms.Session#close()
     */
    public void close() throws JMSException {
        mSession.close();
    }

    /**
     * @see javax.jms.Session#commit()
     */
    public void commit() throws JMSException {
        throw new JMSException("Invalid call");
    }

    /**
     * @see javax.jms.Session#createBrowser(javax.jms.Queue, java.lang.String)
     */
    public QueueBrowser createBrowser(Queue arg0, String arg1) throws JMSException {
        return mSession.createBrowser(arg0, arg1);
    }

    /**
     * @see javax.jms.Session#createBrowser(javax.jms.Queue)
     */
    public QueueBrowser createBrowser(Queue arg0) throws JMSException {
        return mSession.createBrowser(arg0);
    }

    /**
     * @see javax.jms.Session#createBytesMessage()
     */
    public BytesMessage createBytesMessage() throws JMSException {
        return mSession.createBytesMessage();
    }

    /**
     * @see javax.jms.Session#createConsumer(javax.jms.Destination, java.lang.String, boolean)
     */
    public MessageConsumer createConsumer(Destination arg0, String arg1, boolean arg2) throws JMSException {
        return mSession.createConsumer(arg0, arg1, arg2);
    }

    /**
     * @see javax.jms.Session#createConsumer(javax.jms.Destination, java.lang.String)
     */
    public MessageConsumer createConsumer(Destination arg0, String arg1) throws JMSException {
        return mSession.createConsumer(arg0, arg1);
    }

    /**
     * @see javax.jms.Session#createConsumer(javax.jms.Destination)
     */
    public MessageConsumer createConsumer(Destination arg0) throws JMSException {
        return mSession.createConsumer(arg0);
    }

    /**
     * @see javax.jms.Session#createDurableSubscriber(javax.jms.Topic, java.lang.String, java.lang.String, boolean)
     */
    public TopicSubscriber createDurableSubscriber(Topic arg0, String arg1, String arg2,
        boolean arg3) throws JMSException {
        return mSession.createDurableSubscriber(arg0, arg1, arg2, arg3);
    }

    /**
     * @see javax.jms.Session#createDurableSubscriber(javax.jms.Topic, java.lang.String)
     */
    public TopicSubscriber createDurableSubscriber(Topic arg0, String arg1) throws JMSException {
        return mSession.createDurableSubscriber(arg0, arg1);
    }

    /**
     * @see javax.jms.Session#createMapMessage()
     */
    public MapMessage createMapMessage() throws JMSException {
        return mSession.createMapMessage();
    }

    /**
     * @see javax.jms.Session#createMessage()
     */
    public Message createMessage() throws JMSException {
        return mSession.createMessage();
    }

    /**
     * @see javax.jms.Session#createObjectMessage()
     */
    public ObjectMessage createObjectMessage() throws JMSException {
        return mSession.createObjectMessage();
    }

    /**
     * @see javax.jms.Session#createObjectMessage(java.io.Serializable)
     */
    public ObjectMessage createObjectMessage(Serializable arg0) throws JMSException {
        return mSession.createObjectMessage(arg0);
    }

    /**
     * @see javax.jms.Session#createProducer(javax.jms.Destination)
     */
    public MessageProducer createProducer(Destination arg0) throws JMSException {
        return mSession.createProducer(arg0);
    }

    /**
     * @see javax.jms.Session#createQueue(java.lang.String)
     */
    public Queue createQueue(String arg0) throws JMSException {
        return mSession.createQueue(arg0);
    }

    /**
     * @see javax.jms.Session#createStreamMessage()
     */
    public StreamMessage createStreamMessage() throws JMSException {
        return mSession.createStreamMessage();
    }

    /**
     * @see javax.jms.Session#createTemporaryQueue()
     */
    public TemporaryQueue createTemporaryQueue() throws JMSException {
        return mSession.createTemporaryQueue();
    }

    /**
     * @see javax.jms.Session#createTemporaryTopic()
     */
    public TemporaryTopic createTemporaryTopic() throws JMSException {
        return mSession.createTemporaryTopic();
    }

    /**
     * @see javax.jms.Session#createTextMessage()
     */
    public TextMessage createTextMessage() throws JMSException {
        return mSession.createTextMessage();
    }

    /**
     * @see javax.jms.Session#createTextMessage(java.lang.String)
     */
    public TextMessage createTextMessage(String arg0) throws JMSException {
        return mSession.createTextMessage(arg0);
    }

    /**
     * @see javax.jms.Session#createTopic(java.lang.String)
     */
    public Topic createTopic(String arg0) throws JMSException {
        return mSession.createTopic(arg0);
    }

    /**
     * @see javax.jms.Session#getAcknowledgeMode()
     */
    public int getAcknowledgeMode() throws JMSException {
        return mSession.getAcknowledgeMode();
    }

    /**
     * @see javax.jms.Session#getMessageListener()
     */
    public MessageListener getMessageListener() throws JMSException {
        return mSession.getMessageListener();
    }

    /**
     * @see javax.jms.Session#getTransacted()
     */
    public boolean getTransacted() throws JMSException {
        return mSession.getTransacted();
    }

    /**
     * @see javax.jms.Session#recover()
     */
    public void recover() throws JMSException {
        throw new JMSException("Invalid call");
    }

    /**
     * @see javax.jms.Session#rollback()
     */
    public void rollback() throws JMSException {
        throw new JMSException("Invalid call");
    }

    /**
     * @see javax.jms.Session#run()
     */
    public void run() {
        mSession.run();
    }

    /**
     * @see javax.jms.Session#setMessageListener(javax.jms.MessageListener)
     */
    public void setMessageListener(MessageListener arg0) throws JMSException {
        mSession.setMessageListener(arg0);
    }

    /**
     * @see javax.jms.Session#unsubscribe(java.lang.String)
     */
    public void unsubscribe(String arg0) throws JMSException {
        mSession.unsubscribe(arg0);
    }

    /**
     * @see javax.jms.XAQueueSession#getQueueSession()
     */
    public QueueSession getQueueSession() throws JMSException {
        return (QueueSession) mSession;
    }

    /**
     * @see javax.jms.XATopicSession#getTopicSession()
     */
    public TopicSession getTopicSession() throws JMSException {
        return (TopicSession) mSession;
    }
}
