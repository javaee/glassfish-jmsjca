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
import javax.jms.Topic;
import javax.jms.TopicPublisher;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

/**
 * See WProducer
 *
 * @author Frank Kieviet
 * @version $Revision: 1.8 $
 */
public class WTopicPublisher extends WMessageProducer implements TopicPublisher {
    private TopicPublisher mDelegate;
    private InterceptorChain mInterceptorChain;

    /**
     * WTopicPublisher
     *
     * @param mgr JProducer
     * @param delegate TopicPublisher
     */
    public WTopicPublisher(JProducer mgr, TopicPublisher delegate) {
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
     * getTopic
     *
     * @return Topic
     * @throws JMSException on failure
     */
    public Topic getTopic() throws JMSException {
        if (mMgr == null) {
            invokeOnClosed();
        }
        try {
            Topic ret = mDelegate.getTopic();
            ret = (Topic) mMgr.getSession().wrap(ret, mMgr.getConnection());
            return ret; 
        } catch (JMSException e) {
            mMgr.exceptionOccurred(e);
            throw e;
        }
    }

    private static final Method PUBLISH1 = InterceptorUtil.getMethod(TopicPublisher.class, "publish", Message.class);

    /**
     * publish
     *
     * @param message Message
     * @throws JMSException on failure
     */
    public void publish(Message message) throws JMSException {
        if (mMgr == null) {
            invokeOnClosed();
        }
        try {
            if (message instanceof Unwrappable) {
                message = (Message) ((Unwrappable) message).getWrappedObject();
            }
            if (mInterceptorChain == null) {
                mDelegate.publish(message);
            } else {
                Map<String, Object> contextData = new HashMap<String, Object>();
                mInterceptorChain.invokeJMS(mDelegate, PUBLISH1, contextData, message);
            }
            mMgr.onSend();
        } catch (JMSException e) {
            mMgr.exceptionOccurred(e);
            throw e;
        }
    }

    private static final Method PUBLISH4 = InterceptorUtil.getMethod(TopicPublisher.class, "publish"
        , Message.class, int.class, int.class, long.class);

    /**
     * publish
     *
     * @param message Message
     * @param int1 int
     * @param int2 int
     * @param long3 long
     * @throws JMSException on failure
     */
    public void publish(Message message, int int1, int int2, long long3) throws JMSException {
        if (mMgr == null) {
            invokeOnClosed();
            mMgr.onSend();
        }
        try {
            if (message instanceof Unwrappable) {
                message = (Message) ((Unwrappable) message).getWrappedObject();
            }
            if (mInterceptorChain == null) {
                mDelegate.publish(message, int1, int2, long3);
            } else {
                Map<String, Object> contextData = new HashMap<String, Object>();
                mInterceptorChain.invokeJMS(mDelegate, PUBLISH4, contextData, message, int1, int2, long3);
            }
        } catch (JMSException e) {
            mMgr.exceptionOccurred(e);
            throw e;
        }
    }

    private static final Method PUBLISH2 = InterceptorUtil.getMethod(TopicPublisher.class, "publish"
        , Topic.class, Message.class);

    /**
     * publish
     *
     * @param topic Topic
     * @param message Message
     * @throws JMSException on failure
     */
    public void publish(Topic topic, Message message) throws JMSException {
        if (mMgr == null) {
            invokeOnClosed();
        }
        try {
            if (topic instanceof AdminDestination && ((AdminDestination) topic).isTopic()) {
                topic = (Topic) mMgr.getSession().createDestination((AdminDestination) topic);
            }
            if (topic instanceof Unwrappable) {
                topic = (Topic) ((Unwrappable) topic).getWrappedObject();
            }
            if (message instanceof Unwrappable) {
                message = (Message) ((Unwrappable) message).getWrappedObject();
            }
            if (mInterceptorChain == null) {
                mDelegate.publish(topic, message);
            } else {
                Map<String, Object> contextData = new HashMap<String, Object>();
                mInterceptorChain.invokeJMS(mDelegate, PUBLISH2, contextData, topic, message);
            }
            mMgr.onSend();
        } catch (JMSException e) {
            mMgr.exceptionOccurred(e);
            throw e;
        }
    }

    private static final Method PUBLISH5 = InterceptorUtil.getMethod(TopicPublisher.class, "publish"
        , Topic.class, Message.class, int.class, int.class, long.class);

    /**
     * publish
     *
     * @param topic Topic
     * @param message Message
     * @param int2 int
     * @param int3 int
     * @param long4 long
     * @throws JMSException on failure
     */
    public void publish(Topic topic, Message message, int int2, int int3, long long4) throws JMSException {
        if (mMgr == null) {
            invokeOnClosed();
        }
        if (topic instanceof AdminDestination && ((AdminDestination) topic).isTopic()) {
            topic = (Topic) mMgr.getSession().createDestination((AdminDestination) topic);
        }
        if (topic instanceof Unwrappable) {
            topic = (Topic) ((Unwrappable) topic).getWrappedObject();
        }
        if (message instanceof Unwrappable) {
            message = (Message) ((Unwrappable) message).getWrappedObject();
        }
        try {
            if (mInterceptorChain == null) {
                mDelegate.publish(topic, message, int2, int3, long4);
            } else {
                Map<String, Object> contextData = new HashMap<String, Object>();
                mInterceptorChain.invokeJMS(mDelegate, PUBLISH5, contextData, topic, message, int2, int3, long4);
            }
            mMgr.onSend();
        } catch (JMSException e) {
            mMgr.exceptionOccurred(e);
            throw e;
        }
    }
}
