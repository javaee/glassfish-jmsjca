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
import com.stc.jmsjca.util.NoProxyWrapper;

import javax.jms.DeliveryMode;
import javax.jms.JMSException;
import javax.jms.MessageProducer;
import javax.jms.QueueSender;
import javax.jms.TopicPublisher;

/**
* Manages a Producer; it holds a wrapper (WProducer) and manages the JMS Producer object.
* The application will call methods on the wrapper; the wrapper will delegate these calls
* to the JMS Producer object, and some of them to the JConsumer.
 *
 * @author Frank Kieviet
 * @version $Revision: 1.8 $
 */
public class JProducer extends NoProxyWrapper {
    private static Logger sLog = Logger.getLogger(JProducer.class);
    private JSession mSession;
    private MessageProducer mDelegate;
    private boolean mIsTemp;
    private JConnection mConnection;

    private static final Localizer LOCALE = Localizer.get();

    /**
     * Constructor
     *
     * @param itf Class
     * @param session JSession
     * @param delegate MessageProducer
     * @param signature String
     * @param isTemp boolean
     */
    public JProducer(Class<?> itf, JSession session, MessageProducer delegate,
        String signature, boolean isTemp) {
        mSession = session;
        mDelegate = delegate;
        mIsTemp = isTemp;
        init(itf, signature);
    }
    
    /**
     * Provides access to the JSession object
     * 
     * @return JSession
     */
    public JSession getSession() {
        return mSession;
    }

    /**
     * Creates a new wrapper and invalidates any existing current wrapper.
     */
    @Override
    public void createNewWrapper() {
        if (getWrapper() != null) {
            ((WMessageProducer) getWrapper()).setClosed();
        }

        if (getItfClass() == javax.jms.MessageProducer.class) {
            setWrapper(new WMessageProducer(this, mDelegate));
        } else if (getItfClass() == javax.jms.TopicPublisher.class) {
            setWrapper(new WTopicPublisher(this, (TopicPublisher) mDelegate));
        } else if (getItfClass() == javax.jms.QueueSender.class) {
            setWrapper(new WQueueSender(this, (QueueSender) mDelegate));
        } else {
            throw Exc.rtexc(LOCALE.x("E131: Unknown class: {0}", getItfClass()));
        }
    }

    /**
     * Marks the state as CLOSED
     */
    public void virtualClose() {
        createNewWrapper();

        // Reset state
        try {
            mDelegate.setDeliveryMode(DeliveryMode.PERSISTENT);
            mDelegate.setDisableMessageID(false);
            mDelegate.setDisableMessageTimestamp(false);
            mDelegate.setPriority(4);
            mDelegate.setTimeToLive(0);
        } catch (JMSException ex) {
            sLog.warn(LOCALE.x("E040: An exception occurred resetting the client state "
                + "of this {0}: {1}", getItfClass(), ex), ex);
        }
    }

    /**
     * Returns true if this producer can be pooled. Producers on temporary destinations
     * cannot be pooled.
     *
     * @return true if poolable
     */
    public boolean canBePooled() {
        return !mIsTemp && !hasExceptionOccurred();
    }

    /**
     * Called when an invocation exception has occurred
     *
     * @param ex Throwable
     */
    @Override
    public void exceptionOccurred(Throwable ex) {
        super.exceptionOccurred(ex);
        if (mSession != null) {
            mSession.exceptionOccurred(ex);
        }
    }

    /**
     * Close the physical object
     */
    @Override
    public void physicalClose() {
        createNewWrapper();
        try {
            mDelegate.close();
        } catch (JMSException ex) {
            sLog.warn(LOCALE.x("E094: This {0} could not be closed properly: {1}", getItfClass(),
                ex), ex);
        }
    }

    /**
     * Notification when a msg is sent so that the transactional state flag can be updated
     */
    public void onSend() {
        if (mSession != null) {
            mSession.onSend();
        }
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
            mSession.notifyProducerClosedByApplication(this);
        }
    }

    /**
     * Sets the connection that was used to obtain the session that was used to obtain
     * this producer
     * 
     * @param connection JConnection
     */
    public void setConnection(JConnection connection) {
        mConnection = connection;
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
