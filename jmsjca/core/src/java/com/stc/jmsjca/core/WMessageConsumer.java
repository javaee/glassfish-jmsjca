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

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageListener;

/**
 * A wrapper around a javax.jms.MessageConsumer; this wrapper is given out to the
 * application code. The Application will call methods in this wrapper; the
 * wrapper will delegate the calls to the physical connection. Some of the
 * calls will be treated specially, such as the close() method.
 *
 * @author Frank Kieviet
 * @version $Revision: 1.4 $
 */
public class WMessageConsumer implements MessageConsumer {
    /**
     * The JConsumer that owns this wrapper
     */
    protected JConsumer mMgr;
    private MessageConsumer mDelegate;

    /**
     * Constructor
     *
     * @param mgr JConsumer
     * @param mc MessageConsumer
     */
    public WMessageConsumer(JConsumer mgr, MessageConsumer mc) {
        mMgr = mgr;
        mDelegate = mc;
    }

    /**
     * invokeOnClosed
     *
     * @throws javax.jms.IllegalStateException always
     */
    public void invokeOnClosed() throws javax.jms.IllegalStateException {
        throw new javax.jms.IllegalStateException("This MessageConsumer is closed");
    }

    /**
     * Marks as closed
     */
    public void setClosed() {
        mMgr = null;
        mDelegate = null;
    }

    /**
     * getMessageSelector
     *
     * @throws JMSException on failure
     * @return String
     */
    public String getMessageSelector() throws JMSException {
        if (mMgr == null) {
            invokeOnClosed();
        }
        try {
            return mDelegate.getMessageSelector();
        } catch (JMSException e) {
            mMgr.exceptionOccurred(e);
            throw e;
        }
    }

    /**
     * getMessageListener
     *
     * @throws JMSException on failure
     * @return MessageListener
     */
    public MessageListener getMessageListener() throws JMSException {
        if (mMgr == null) {
            invokeOnClosed();
        }
        try {
            return mDelegate.getMessageListener();
        } catch (JMSException e) {
            mMgr.exceptionOccurred(e);
            throw e;
        }
    }

    /**
     * setMessageListener
     *
     * @param listener MessageListener
     * @throws JMSException on failure
     */
    public void setMessageListener(MessageListener listener) throws JMSException {
        if (mMgr == null) {
            invokeOnClosed();
        }
        try {
            mDelegate.setMessageListener(listener);
        } catch (JMSException e) {
            mMgr.exceptionOccurred(e);
            throw e;
        }
    }

    /**
     * receive
     *
     * @throws JMSException on failure
     * @return Message
     */
    public Message receive() throws JMSException {
        if (mMgr == null) {
            invokeOnClosed();
        }
        try {
            return mMgr.onReceived(mDelegate.receive());
        } catch (JMSException e) {
            mMgr.exceptionOccurred(e);
            throw e;
        }
    }

    /**
     * receive
     *
     * @param timeout long
     * @throws JMSException on failure
     * @return Message
     */
    public Message receive(long timeout) throws JMSException {
        if (mMgr == null) {
            invokeOnClosed();
        }
        try {
            return mMgr.onReceived(mDelegate.receive(timeout));
        } catch (JMSException e) {
            mMgr.exceptionOccurred(e);
            throw e;
        }
    }

    /**
     * receiveNoWait
     *
     * @throws JMSException on failure
     * @return Message
     */
    public Message receiveNoWait() throws JMSException {
        if (mMgr == null) {
            invokeOnClosed();
        }
        try {
            return mMgr.onReceived(mDelegate.receiveNoWait());
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
