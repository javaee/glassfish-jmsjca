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
import javax.jms.TopicPublisher;
import javax.jms.TopicSession;
import javax.jms.TopicSubscriber;

/**
 * See WSession
 *
 * @author Frank Kieviet
 * @version $Revision: 1.1.1.3 $
 */
public class WTopicSession extends WSession implements TopicSession {
    /**
     * WTopicSession
     *
     * @param mgr JSession
     */
    public WTopicSession(JSession mgr) {
        super(mgr);
    }

    /**
     * createSubscriber
     *
     * @param topic Topic
     * @return TopicSubscriber
     * @throws JMSException on failure
     */
    public TopicSubscriber createSubscriber(Topic topic) throws JMSException {
        checkClosed();
        checkTemporaryDestinationOwnership(topic);
        if (topic instanceof Unwrappable) {
            topic = (Topic) ((Unwrappable) topic).getWrappedObject();
        }
        try {
            return mMgr.createSubscriber(topic, getConnection());
        } catch (JMSException e) {
            mMgr.exceptionOccurred(e);
            throw e;
        }
    }

    /**
     * createSubscriber
     *
     * @param topic Topic
     * @param string String
     * @param boolean2 boolean
     * @return TopicSubscriber
     * @throws JMSException on failure
     */
    public TopicSubscriber createSubscriber(Topic topic, String string, boolean boolean2) throws JMSException {
        checkClosed();
        checkTemporaryDestinationOwnership(topic);
        if (topic instanceof Unwrappable) {
            topic = (Topic) ((Unwrappable) topic).getWrappedObject();
        }
        try {
            return mMgr.createSubscriber(topic, string, boolean2, getConnection());
        } catch (JMSException e) {
            mMgr.exceptionOccurred(e);
            throw e;
        }
    }

    /**
     * createPublisher
     *
     * @param topic Topic
     * @return TopicPublisher
     * @throws JMSException on failure
     */
    public TopicPublisher createPublisher(Topic topic) throws JMSException {
        checkClosed();
        if (topic instanceof Unwrappable) {
            topic = (Topic) ((Unwrappable) topic).getWrappedObject();
        }
        try {
            return mMgr.createPublisher(topic, getConnection());
        } catch (JMSException e) {
            mMgr.exceptionOccurred(e);
            throw e;
        }
    }
}
