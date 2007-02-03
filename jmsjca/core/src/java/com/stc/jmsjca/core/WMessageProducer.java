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

import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageProducer;

/**
 * A wrapper around a javax.jms.MessageConsumer; this wrapper is given out to the
 * application code. The Application will call methods in this wrapper; the
 * wrapper will delegate the calls to the physical connection. Some of the
 * calls will be treated specially, such as the close() method.
 *
 * @author Frank Kieviet
 * @version $Revision: 1.4 $
 */
public class WMessageProducer implements MessageProducer {
    /**
     * The JProducer that owns this wrapper
     */
    protected JProducer mMgr;
    private MessageProducer mDelegate;

    /**
     * Constructor
     *
     * @param mgr JProducer
     * @param delegate MessageProducer
     */
    public WMessageProducer(JProducer mgr, MessageProducer delegate) {
        mMgr = mgr;
        mDelegate = delegate;
    }

    /**
     * invokeOnClosed
     *
     * @throws javax.jms.IllegalStateException always
     */
    public void invokeOnClosed() throws javax.jms.IllegalStateException {
        throw new javax.jms.IllegalStateException("This MessageProducer is closed");
    }

    /**
     * Marks as closed
     */
    public void setClosed() {
        mMgr = null;
        mDelegate = null;
    }

    /**
     * setDisableMessageID
     *
     * @param value boolean
     * @throws JMSException on failure
     */
    public void setDisableMessageID(boolean value) throws JMSException {
        if (mMgr == null) {
            invokeOnClosed();
        }
        try {
            mDelegate.setDisableMessageID(value);
        } catch (JMSException e) {
            mMgr.exceptionOccurred(e);
            throw e;
        }
    }

    /**
     * getDisableMessageID
     *
     * @throws JMSException on failure
     * @return boolean
     */
    public boolean getDisableMessageID() throws JMSException {
        if (mMgr == null) {
            invokeOnClosed();
        }
        try {
            return mDelegate.getDisableMessageID();
        } catch (JMSException e) {
            mMgr.exceptionOccurred(e);
            throw e;
        }
    }

    /**
     * setDisableMessageTimestamp
     *
     * @param value boolean
     * @throws JMSException on failure
     */
    public void setDisableMessageTimestamp(boolean value) throws JMSException {
        if (mMgr == null) {
            invokeOnClosed();
        }
        try {
            mDelegate.setDisableMessageTimestamp(value);
        } catch (JMSException e) {
            mMgr.exceptionOccurred(e);
            throw e;
        }
    }

    /**
     * getDisableMessageTimestamp
     *
     * @throws JMSException on failure
     * @return boolean
     */
    public boolean getDisableMessageTimestamp() throws JMSException {
        if (mMgr == null) {
            invokeOnClosed();
        }
        try {
            return mDelegate.getDisableMessageTimestamp();
        } catch (JMSException e) {
            mMgr.exceptionOccurred(e);
            throw e;
        }
    }

    /**
     * setDeliveryMode
     *
     * @param deliveryMode int
     * @throws JMSException on failure
     */
    public void setDeliveryMode(int deliveryMode) throws JMSException {
        if (mMgr == null) {
            invokeOnClosed();
        }
        try {
            mDelegate.setDeliveryMode(deliveryMode);
        } catch (JMSException e) {
            mMgr.exceptionOccurred(e);
            throw e;
        }
    }

    /**
     * getDeliveryMode
     *
     * @throws JMSException on failure
     * @return int
     */
    public int getDeliveryMode() throws JMSException {
        if (mMgr == null) {
            invokeOnClosed();
        }
        try {
            return mDelegate.getDeliveryMode();
        } catch (JMSException e) {
            mMgr.exceptionOccurred(e);
            throw e;
        }
    }

    /**
     * setPriority
     *
     * @param defaultPriority int
     * @throws JMSException on failure
     */
    public void setPriority(int defaultPriority) throws JMSException {
        if (mMgr == null) {
            invokeOnClosed();
        }
        try {
            mDelegate.setPriority(defaultPriority);
        } catch (JMSException e) {
            mMgr.exceptionOccurred(e);
            throw e;
        }
    }

    /**
     * getPriority
     *
     * @throws JMSException on failure
     * @return int
     */
    public int getPriority() throws JMSException {
        if (mMgr == null) {
            invokeOnClosed();
        }
        try {
            return mDelegate.getPriority();
        } catch (JMSException e) {
            mMgr.exceptionOccurred(e);
            throw e;
        }
    }

    /**
     * setTimeToLive
     *
     * @param timeToLive long
     * @throws JMSException on failure
     */
    public void setTimeToLive(long timeToLive) throws JMSException {
        if (mMgr == null) {
            invokeOnClosed();
        }
        try {
            mDelegate.setTimeToLive(timeToLive);
        } catch (JMSException e) {
            mMgr.exceptionOccurred(e);
            throw e;
        }
    }

    /**
     * getTimeToLive
     *
     * @throws JMSException on failure
     * @return long
     */
    public long getTimeToLive() throws JMSException {
        if (mMgr == null) {
            invokeOnClosed();
        }
        try {
            return mDelegate.getTimeToLive();
        } catch (JMSException e) {
            mMgr.exceptionOccurred(e);
            throw e;
        }
    }

    /**
     * getDestination
     *
     * @throws JMSException on failure
     * @return Destination
     */
    public Destination getDestination() throws JMSException {
        if (mMgr == null) {
            invokeOnClosed();
        }
        try {
            Destination ret = mDelegate.getDestination();
            ret = mMgr.getSession().wrap(ret, mMgr.getConnection());
            return ret;
        } catch (JMSException e) {
            mMgr.exceptionOccurred(e);
            throw e;
        }
    }

    /**
     * send
     *
     * @param message Message
     * @throws JMSException on failure
     */
    public void send(Message message) throws JMSException {
        if (mMgr == null) {
            invokeOnClosed();
        }
        try {
            if (message instanceof Unwrappable) {
                message = (Message) ((Unwrappable) message).getWrappedObject();
            }
            mDelegate.send(message);
            mMgr.onSend();
        } catch (JMSException e) {
            mMgr.exceptionOccurred(e);
            throw e;
        }
    }

    /**
     * send
     *
     * @param message Message
     * @param deliveryMode int
     * @param priority int
     * @param timeToLive long
     * @throws JMSException on failure
     */
    public void send(Message message, int deliveryMode, int priority,
        long timeToLive) throws JMSException {
        if (mMgr == null) {
            invokeOnClosed();
        }
        try {
            if (message instanceof Unwrappable) {
                message = (Message) ((Unwrappable) message).getWrappedObject();
            }
            mDelegate.send(message, deliveryMode, priority, timeToLive);
            mMgr.onSend();
        } catch (JMSException e) {
            mMgr.exceptionOccurred(e);
            throw e;
        }
    }

    /**
     * send
     *
     * @param destination Destination
     * @param message Message
     * @throws JMSException on failure
     */
    public void send(Destination destination, Message message) throws JMSException {
        if (mMgr == null) {
            invokeOnClosed();
        }
        if (destination instanceof Unwrappable) {
            destination = (Destination) ((Unwrappable) destination).getWrappedObject();
        }
        if (message instanceof Unwrappable) {
            message = (Message) ((Unwrappable) message).getWrappedObject();
        }
        try {
            mDelegate.send(destination, message);
            mMgr.onSend();
        } catch (JMSException e) {
            mMgr.exceptionOccurred(e);
            throw e;
        }
    }

    /**
     * send
     *
     * @param destination Destination
     * @param message Message
     * @param deliveryMode int
     * @param priority int
     * @param timeToLive long
     * @throws JMSException on failure
     */
    public void send(Destination destination, Message message, int deliveryMode,
        int priority, long timeToLive) throws JMSException {
        if (mMgr == null) {
            invokeOnClosed();
        }
        if (destination instanceof Unwrappable) {
            destination = (Destination) ((Unwrappable) destination).getWrappedObject();
        }
        if (message instanceof Unwrappable) {
            message = (Message) ((Unwrappable) message).getWrappedObject();
        }
        try {
            mDelegate.send(destination, message, deliveryMode, priority, timeToLive);
            mMgr.onSend();
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
        if (mMgr != null) {
            mMgr.close();
            mMgr = null;
            mDelegate = null;
        }
    }
}
