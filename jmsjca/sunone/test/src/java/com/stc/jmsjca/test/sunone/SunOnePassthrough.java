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

package com.stc.jmsjca.test.sunone;


import com.stc.jmsjca.sunone.SunOneUrlParser;
import com.stc.jmsjca.test.core.JMSProvider;
import com.stc.jmsjca.test.core.Passthrough;

import javax.jms.JMSException;
import javax.jms.QueueConnectionFactory;
import javax.jms.Session;
import javax.jms.TopicConnection;
import javax.jms.TopicConnectionFactory;
import javax.jms.TopicSession;

import java.util.Properties;

/**
 *
 * @author fkieviet
 * @version $Revision: 1.6 $
 */
public class SunOnePassthrough extends Passthrough {
    private Properties mServerProperties;

    public SunOnePassthrough(Properties server, JMSProvider provider) {
        super(server, provider);
        mServerProperties = server;
    }
    
    public static int getPort(Properties serverProperties) {
        int port = Integer.parseInt(serverProperties.getProperty(
            SunOneProvider.PROPNAME_PORT, null));
        return port;
    }
    
    public static String getHost(Properties serverProperties) {
        return serverProperties.getProperty(SunOneProvider.PROPNAME_HOST);        
    }

    private String getConnectionUrl() {
        return "mq://" + getHost(mServerProperties) + ":" + getPort(mServerProperties);
    }

    public boolean isDurableSubscriberPresent(String topic, String subname) throws Exception {
        return true;
    }

    @Override
    public void removeDurableSubscriber(String clientID, String t12, String subscriptionName) throws Exception {
        TopicConnectionFactory cf = createTopicConnectionFactory();
        TopicConnection conn = cf.createTopicConnection(getUserid(), getPassword());
        conn.setClientID(clientID);
        TopicSession sess = conn.createTopicSession(false, Session.CLIENT_ACKNOWLEDGE);
        try {
            sess.unsubscribe(subscriptionName);
        } catch (JMSException ignore) {
            // ignore
        }
        sess.close();
        conn.close();
    }
    
    /**
     * Creates a factory based on the URL
     * 
     * @param url url
     * @return fact
     * @throws JMSException propagated
     */
    public static TopicConnectionFactory createTopicConnectionFactory(String url) throws JMSException {
        SunOneUrlParser urlParser = new SunOneUrlParser(url);

        TopicConnectionFactory sunOneTopicConnectionFactory = new com.sun.messaging.TopicConnectionFactory();
        com.sun.messaging.BasicConnectionFactory basicConnectionFactory = (com.sun.messaging.BasicConnectionFactory) sunOneTopicConnectionFactory;
        
        basicConnectionFactory.setProperty("imqAddressList", urlParser.getSunOneUrlSet());
        
        return sunOneTopicConnectionFactory;
    }

    /**
     * @see com.stc.jmsjca.test.core.Passthrough#createTopicConnectionFactory()
     */
    @Override
    public TopicConnectionFactory createTopicConnectionFactory() throws JMSException {
        return createTopicConnectionFactory(getConnectionUrl());
    }

    /**
     * Creates a factory based on the URL
     * 
     * @param url url
     * @return fact
     * @throws JMSException propagated
     */
    public static QueueConnectionFactory createQueueConnectionFactory(String url) throws JMSException {
        SunOneUrlParser urlParser = new SunOneUrlParser(url);
        
        QueueConnectionFactory sunOneQueueConnectionFactory = new com.sun.messaging.QueueConnectionFactory();
        com.sun.messaging.BasicConnectionFactory basicConnectionFactory = (com.sun.messaging.BasicConnectionFactory) sunOneQueueConnectionFactory;
       
        basicConnectionFactory.setProperty("imqAddressList", urlParser.getSunOneUrlSet());
        
        return sunOneQueueConnectionFactory;
    }

    /**
     * @see com.stc.jmsjca.test.core.Passthrough#createQueueConnectionFactory()
     */
    @Override
    public QueueConnectionFactory createQueueConnectionFactory() throws JMSException {
        return createQueueConnectionFactory(getConnectionUrl());
    }

    
}
