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

package com.stc.jmsjca.test.wmq;

import com.ibm.mq.jms.MQQueueConnectionFactory;
import com.ibm.mq.jms.MQTopicConnectionFactory;
import com.stc.jmsjca.test.core.JMSProvider;
import com.stc.jmsjca.test.core.Passthrough;
import com.stc.jmsjca.util.Exc;
import com.stc.jmsjca.wmq.RAWMQObjectFactory;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.QueueConnectionFactory;
import javax.jms.Session;
import javax.jms.TextMessage;
import javax.jms.Topic;
import javax.jms.TopicConnection;
import javax.jms.TopicConnectionFactory;
import javax.jms.TopicSession;
import javax.jms.TopicSubscriber;

import java.util.Properties;
import java.util.logging.Level;

/**
 * Passthrough for MQSeries
 * 
 * @author  fkieviet (rewrite April 2009)
 * @version $Revision: 1.9 $
 */
public class WMQPassthrough extends Passthrough {
    public static final String PROPNAME_HOST = "jmsjca.jmsimpl.wmq.host";
    public static final String PROPNAME_PORT = "jmsjca.jmsimpl.wmq.port";
    public static final String PROPNAME_USERID = "jmsjca.jmsimpl.wmq.userid";
    public static final String PROPNAME_PASSWORD = "jmsjca.jmsimpl.wmq.password";
    public static final String PROPNAME_QUEUEMANAGER = "jmsjca.jmsimpl.wmq.queuemanager";

    private Properties mServerProperties;

    public static int TRANSPORT_TYPE = 1; //1:JMSC_MQJMS_TP_CLIENT_MQ_TCPIP, 0: JMSC.MQJMS_TP_BINDINGS_MQ 
    static final String CHANNEL  = "SYSTEM.DEF.SVRCONN"; // need if it uses admin functionalities

    /**
     * @param server Properties
     */
    public WMQPassthrough(Properties server, JMSProvider provider) {
        super(server, provider);
        mServerProperties = server;
    }
    
    public static int getPort(Properties serverProperties) {
        int port = Integer.parseInt(serverProperties.getProperty(PROPNAME_PORT, null));
        return port;
    }
    
    public static String getHost(Properties serverProperties) {return serverProperties.getProperty(PROPNAME_HOST);        
    }
    
    public static String getQueueManager(Properties serverProperties) {
        return serverProperties.getProperty(PROPNAME_QUEUEMANAGER);        
    }
    
    /**
     * @see com.stc.jmsjca.test.core.Passthrough#removeDurableSubscriber(java.lang.String, java.lang.String, java.lang.String)
     */
    @Override
    public void removeDurableSubscriber(String clientId, String dest, String subname) throws Exception {
        TopicConnectionFactory cf = createTopicConnectionFactory();
        TopicConnection conn = cf.createTopicConnection(getUserid(), getPassword());
        conn.setClientID(clientId);
        
        TopicSession sess = conn.createTopicSession(true, Session.SESSION_TRANSACTED);
        
        // Drain
        Topic t = sess.createTopic(dest);
        TopicSubscriber sub = sess.createDurableSubscriber(t, dest);
        int nDrained = 0;
        for (;;) {
            Message m = sub.receive(getDrainTimeout());
            if (m == null) {
                break;
            }
            nDrained++;
            if (nDrained % getCommitSize() == 0) {
                sess.commit();
            }
            if (nDrained % 100 == 0) {
                System.out.println(nDrained + " messages were drained from " + dest);
            }
            if (nDrained < 100) {
                System.out.print("Drained " + m.getClass());
                if (m instanceof TextMessage) {
                    System.out.print(" Payload: " + ((TextMessage) m).getText());
                }
                System.out.println();
            }
        }
        if (nDrained != 0) {
            System.out.println("Total of " + nDrained + " messages were drained from " + dest);
        }
        sess.commit();
        sub.close();
        
        // Unsubscribe
        try {
            sess.unsubscribe(subname);
        } catch (Exception e) {
            Exc.checkLinkedException(e);
            java.util.logging.Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Failed to unsubscribe: " + e, e);
        }
        sess.close();
        conn.close();
    }

    /**
     * @see com.stc.jmsjca.test.core.Passthrough#createTopicConnectionFactory()
     */
    @Override
    public TopicConnectionFactory createTopicConnectionFactory() throws JMSException {        
        MQTopicConnectionFactory cf = new MQTopicConnectionFactory();        
        cf.setHostName(getHost(mServerProperties));
        cf.setPort(getPort(mServerProperties));
        cf.setQueueManager(getQueueManager(mServerProperties));
        cf.setTransportType(TRANSPORT_TYPE);
        cf.setChannel(CHANNEL);   
//        cf.setClientID(getJMSProvider().getClientId(getDurableTopic1Name1() + "clientID"));
        return cf;
    }

    /**
     * @see com.stc.jmsjca.test.core.Passthrough#createQueueConnectionFactory()
     */
    @Override
    public QueueConnectionFactory createQueueConnectionFactory() throws JMSException {
        MQQueueConnectionFactory cf = new MQQueueConnectionFactory();        
        cf.setHostName(getHost(mServerProperties));
        cf.setPort(getPort(mServerProperties));
        cf.setQueueManager(getQueueManager(mServerProperties));
        cf.setTransportType(TRANSPORT_TYPE);
        cf.setChannel(CHANNEL);                
        return cf;
    }

    public static String getConnectionUrl(Properties p) {
        return "wmq://" + getHost(p) + ":" + getPort(p) + "?" + RAWMQObjectFactory.QUEUEMANAGER + "=" + getQueueManager(p);
    }
}
