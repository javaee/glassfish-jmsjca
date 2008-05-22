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
import javax.jms.Topic;
import javax.jms.TopicSubscriber;

/**
 * See WConsumer
 *
 * @author Frank Kieviet
 * @version $Revision: 1.6 $
 */
public class WTopicSubscriber extends WMessageConsumer implements TopicSubscriber {
    private TopicSubscriber mDelegate;

    /**
     * WTopicSubscriber
     *
     * @param mgr JConsumer
     * @param mc TopicSubscriber
     */
    public WTopicSubscriber(JConsumer mgr, TopicSubscriber mc) {
        super(mgr, mc);
        mDelegate = mc;
    }

    /**
     * getTopic
     *
     * @throws JMSException on failure
     * @return Topic
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

    /**
     * getNoLocal
     *
     * @throws JMSException on failure
     * @return boolean
     */
    public boolean getNoLocal() throws JMSException {
        if (mMgr == null) {
            invokeOnClosed();
        }
        try {
            return mDelegate.getNoLocal();
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
        super.close();
        mDelegate = null;
    }
}
