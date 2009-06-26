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

import com.stc.jmsjca.util.InterceptorChain;
import com.stc.jmsjca.util.InterceptorUtil;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.Queue;
import javax.jms.QueueSender;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

/**
 * A wrapper around a javax.jms.MessageConsumer; this wrapper is given out to the
 * application code. The Application will call methods in this wrapper; the
 * wrapper will delegate the calls to the physical connection. Some of the
 * calls will be treated specially, such as the close() method.
 *
 * @author Frank Kieviet
 * @version $Revision: 1.8 $
 */
public class WQueueSender extends WMessageProducer implements QueueSender {
    private QueueSender mDelegate;
    private InterceptorChain mInterceptorChain;

    /**
     * WQueueSender
     *
     * @param mgr JProducer
     * @param delegate QueueSender
     */
    public WQueueSender(JProducer mgr, QueueSender delegate) {
        super(mgr, delegate);
        mDelegate = delegate;
        mInterceptorChain =  mMgr.getSession().getManagedConnection().getInterceptorChain();
    }

    /**
     * close
     *
     * @throws JMSException on failure
     */
    @Override
    public void close() throws JMSException {
        super.close();
        mDelegate = null;
    }

    /**
     * getQueue
     *
     * @return Queue
     * @throws JMSException on failure
     */
    public Queue getQueue() throws JMSException {
        if (mMgr == null) {
            invokeOnClosed();
        }
        try {
            Queue ret = mDelegate.getQueue();
            ret = (Queue) mMgr.getSession().wrap(ret, mMgr.getConnection());
            return ret;
        } catch (JMSException e) {
            mMgr.exceptionOccurred(e);
            throw e;
        }
    }

    private static final Method SEND2 = InterceptorUtil.getMethod(QueueSender.class, "send", Queue.class, Message.class);

    /**
     * send
     *
     * @param queue Queue
     * @param message Message
     * @throws JMSException on failure
     */
    public void send(Queue queue, Message message) throws JMSException {
        if (mMgr == null) {
            invokeOnClosed();
        }
        queue = (Queue) mMgr.getSession().checkGeneric(queue);
        if (queue instanceof AdminQueue) {
            queue = (Queue) mMgr.getSession().createDestination((AdminDestination) queue); 
        }
        if (queue instanceof Unwrappable) {
            queue = (Queue) ((Unwrappable) queue).getWrappedObject();
        }
        if (message instanceof Unwrappable) {
            message = (Message) ((Unwrappable) message).getWrappedObject();
        }
        try {
            if (mInterceptorChain == null) {
                mDelegate.send(queue, message);
            } else {
                Map<String, Object> contextData = new HashMap<String, Object>();
                mInterceptorChain.invokeJMS(mDelegate, SEND2, contextData, queue, message);
            }
            mMgr.onSend();
        } catch (JMSException e) {
            mMgr.exceptionOccurred(e);
            throw e;
        }
    }

    private static final Method SEND5 = InterceptorUtil.getMethod(QueueSender.class, "send", Queue.class, Message.class
        , int.class, int.class, long.class);

    /**
     * send
     *
     * @param queue Queue
     * @param message Message
     * @param int2 int
     * @param int3 int
     * @param long4 long
     * @throws JMSException on failure
     */
    public void send(Queue queue, Message message, int int2, int int3, long long4) throws JMSException {
        if (mMgr == null) {
            invokeOnClosed();
        }
        queue = (Queue) mMgr.getSession().checkGeneric(queue);
        if (queue instanceof AdminQueue) {
            queue = (Queue) mMgr.getSession().createDestination((AdminDestination) queue); 
        }
        if (queue instanceof Unwrappable) {
            queue = (Queue) ((Unwrappable) queue).getWrappedObject();
        }
        if (message instanceof Unwrappable) {
            message = (Message) ((Unwrappable) message).getWrappedObject();
        }
        try {
            if (mInterceptorChain == null) {
                mDelegate.send(queue, message, int2, int3, long4);
            } else {
                Map<String, Object> contextData = new HashMap<String, Object>();
                mInterceptorChain.invokeJMS(mDelegate, SEND5, contextData, queue, message, int2, int3, long4);
            }
            mMgr.onSend();
        } catch (JMSException e) {
            mMgr.exceptionOccurred(e);
            throw e;
        }
    }
}
