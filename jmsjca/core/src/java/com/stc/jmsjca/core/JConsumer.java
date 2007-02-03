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
import com.stc.jmsjca.util.NoProxyWrapper;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.QueueReceiver;
import javax.jms.TopicSubscriber;

/**
 * Manages a Consumer; it holds a wrapper (WConsumer) and manages the JMS Consumer object.
 * The application will call methods on the wrapper; the wrapper will delegate these calls
 * to the JMS Consumer object, and some of them to the JConsumer.
 *
 * @author Frank Kieviet
 * @version $Revision: 1.1.1.3 $
 */
public class JConsumer extends NoProxyWrapper {
    private static Logger sLog = Logger.getLogger(JConsumer.class);
    private JSession mSession;
    private MessageConsumer mDelegate;
    private JConnection mConnection;

    private static final Localizer LOCALE = Localizer.get();

    /**
     * Constructor
     *
     * @param itf Class
     * @param session JSession
     * @param delegate MessageConsumer
     * @param signature String
     * @param connection JConnection that was used to create the session that created
     * this consumer
     */
    public JConsumer(Class itf, JSession session, MessageConsumer delegate, String signature, JConnection connection) {
        mSession = session;
        mDelegate = delegate;
        mConnection = connection;
        init(itf, signature);
    }
    
    /**
     * Provides access to the session object
     * 
     * @return JSession session
     */
    public JSession getSession() {
        return mSession;
    }

    /**
     * Creates a new wrapper and invalidates any existing current wrapper.
     */
    public void createNewWrapper() {
        if (getWrapper() != null) {
            ((WMessageConsumer) getWrapper()).setClosed();
        }

        if (getItfClass() == javax.jms.MessageConsumer.class) {
            setWrapper(new WMessageConsumer(this, (MessageConsumer) mDelegate));
        } else if (getItfClass() == javax.jms.TopicSubscriber.class) {
            setWrapper(new WTopicSubscriber(this, (TopicSubscriber) mDelegate));
        } else if (getItfClass() == javax.jms.QueueReceiver.class) {
            setWrapper(new WQueueReceiver(this, (QueueReceiver) mDelegate));
        } else {
            throw new RuntimeException("Unknown class " + getItfClass());
        }
    }

    /**
     * Close the physical object
     */
    public void physicalClose() {
        createNewWrapper();
        try {
            mDelegate.close();
        } catch (JMSException ex) {
            sLog.warn(LOCALE.x("E039: This {0} could not be closed properly: {1}", 
                getItfClass(), ex), ex);
        }
    }

    /**
     * Called when an invocation exception has occurred
     *
     * @param ex Throwable
     */
    public void exceptionOccurred(Throwable ex) {
        super.exceptionOccurred(ex);
        if (mSession != null) {
            mSession.exceptionOccurred(ex);
        }
    }

    /**
     * Notification when a msg is sent so that the transactional state flag can be updated
     *
     * @param message Message either null or not null
     * @return Message same as input
     */
    public Message onReceived(Message message) {
        if (mSession != null) {
            mSession.onReceived(message);
        }
        return message;
    }


    // INTERCEPTED METHODS

    /**
     * Special: does NOT close the physical connection, but notifies the managed
     * connection that the connection is closed and hence that the connection can be
     * returned to the pool.
     *
     * @throws JMSException failure
     */
    public void close() throws JMSException {
        if (mDelegate != null) {
            mSession.notifyConsumerClosedByApplication(this);
        }
    }

    /**
     * Returns the connection that was used to obtain the session that was used to obtain
     * this producer
     * 
     * @return JConnection
     */
    public JConnection getConnection() {
        return mConnection;
    }
}
