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

package com.stc.jmsjca.test.wl;

import com.stc.jmsjca.localization.LocalizedString;
import com.stc.jmsjca.test.core.JMSProvider;
import com.stc.jmsjca.test.core.Passthrough;
import com.stc.jmsjca.util.Exc;

import javax.jms.JMSException;
import javax.jms.Queue;
import javax.jms.QueueConnectionFactory;
import javax.jms.Session;
import javax.jms.Topic;
import javax.jms.TopicConnection;
import javax.jms.TopicConnectionFactory;
import javax.jms.TopicSession;

import java.util.Properties;

/**
 *
 * @author fkieviet
 * @version $Revision: 1.10 $
 */
public class WLPassthrough extends Passthrough {
    private WLProvider mProvider;
    private Properties mProperties;

    public WLPassthrough(Properties server, JMSProvider provider) {
        super(server, provider);
        mProvider = (WLProvider) provider;
        mProperties = server;
    }

    /**
     * @see com.stc.jmsjca.test.core.Passthrough#createTopicConnectionFactory()
     */
    @Override
    public TopicConnectionFactory createTopicConnectionFactory() throws JMSException {
        try {
            return (TopicConnectionFactory) mProvider.getInitialContext(mProperties).lookup(
                "weblogic.jms.XAConnectionFactory");
        } catch (Exception e) {
            throw Exc.jmsExc(LocalizedString.valueOf("Cannot create cf: " + e), e);
        }
    }

    /**
     * @see com.stc.jmsjca.test.core.Passthrough#createQueueConnectionFactory()
     */
    @Override
    public QueueConnectionFactory createQueueConnectionFactory() throws JMSException {
        return (QueueConnectionFactory) createTopicConnectionFactory();
    }

    /**
     * @see com.stc.jmsjca.test.core.Passthrough#removeDurableSubscriber(java.lang.String, java.lang.String, java.lang.String)
     */
    @Override
    public void removeDurableSubscriber(String clientID, String destname,
        String subscriptionName) throws Exception {
        TopicConnectionFactory cf = createTopicConnectionFactory();
        TopicConnection conn = cf.createTopicConnection();
        conn.setClientID(clientID);
        TopicSession sess = conn.createTopicSession(false, Session.CLIENT_ACKNOWLEDGE);
//        Topic t = createTopic(sess, destname);
//        // WL throws an exception if the durable subscriber does not exist: create it first
//        sess.createDurableSubscriber(t, subscriptionName);
        try {
            sess.unsubscribe(subscriptionName);
        } catch (Exception e) {
            // ignore:
            // WL throws an exception if the durable subscriber does not exist
        }
        sess.close();
        conn.close(); 
    }

    @Override
    public Queue createQueue(Session s, String name) throws JMSException {
        try {
            return (Queue) mProvider.getInitialContext(mProperties).lookup(name);
        } catch (Exception e) {
            throw Exc.jmsExc(LocalizedString.valueOf("Cannot find queue " + name + ": " + e), e);
        }
    }

    @Override
    public Topic createTopic(Session s, String name) throws JMSException {
        try {
            return (Topic) mProvider.getInitialContext(mProperties).lookup(name);
        } catch (Exception e) {
            throw Exc.jmsExc(LocalizedString.valueOf("Cannot find topic " + name + ": " + e), e);
        }
    }
}
