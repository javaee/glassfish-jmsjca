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
import com.stc.jmsjca.wmq.WMQConnectionUrl;

import javax.jms.JMSException;
import javax.jms.QueueConnectionFactory;
import javax.jms.Session;
import javax.jms.TopicConnection;
import javax.jms.TopicConnectionFactory;
import javax.jms.TopicSession;

import java.util.Properties;

/**
 *
 * @author  cye
 * @version $Revision: 1.5 $
 */
public class WMQPassthrough extends Passthrough {

    static private String HOSTNAME      = "runtime4";       
    static private int    PORT          = 1414;
    static private String QUEUE_MANAGER = "QM_runtime4";
    /**
     * @param server Properties
     */
    public WMQPassthrough(Properties server, JMSProvider provider) {
        super(server, provider);
    }
    
    /**
     * Get QueueManager from url
     * @param hostStr
     * @return
     */
    public static String getQueueManager(String hostStr) {
        if (!Character.isLetter(hostStr.charAt(0))) {
            try {
                java.net.InetAddress inetAdd =
                    java.net.InetAddress.getByName(hostStr);
                hostStr = inetAdd.getHostName();
            } catch (java.net.UnknownHostException uhe) {
                hostStr = "localhost";
            }            
        }                
        String uHostStr = hostStr.split("\\.") == null ? hostStr : hostStr.split("\\.")[0];;        
        return "QM_" + uHostStr.replace('-', '_').toLowerCase();
    }
    /**
     * Get connectionUrl from system property
     * @return String
     */
    public static String getConnectionUrl() {
        String url = System.getProperty("wmq.url");
        if ( url == null) {
             url = new String("wmq://" + BasicRAWMQTestJUStd.HOSTNAME + ":" 
                + BasicRAWMQTestJUStd.PORT + "?QueueManager=" + BasicRAWMQTestJUStd.QUEUE_MANAGER);             
        }
        WMQConnectionUrl wurl = new WMQConnectionUrl(url);
        Properties p = new Properties();
        wurl.getQueryProperties(p);                          
        QUEUE_MANAGER = p.getProperty("QueueManager", getQueueManager(wurl.getUrlParser().getHost()));                        
        HOSTNAME = wurl.getUrlParser().getHost();
        PORT = wurl.getUrlParser().getPort();            
        return url;        
    }
    
    /**
     * @param topic String
     * @param subname String
     * @return boolean 
     */
    public boolean isDurableSubscriberPresent(String topic, String subname) {
        return true;
    }
   
    public void removeDurableSubscriber(String clientId, String dest, String subname) throws Exception {

        TopicConnectionFactory cf = createTopicConnectionFactory();
        TopicConnection conn = cf.createTopicConnection(BasicRAWMQTestJUStd.userName, BasicRAWMQTestJUStd.password);
        TopicSession sess = conn.createTopicSession(false, Session.CLIENT_ACKNOWLEDGE);
        sess.unsubscribe(subname);
        sess.close();
        conn.close();
    }

    /**
     * @see com.stc.jmsjca.test.core.Passthrough#createTopicConnectionFactory()
     */
    public TopicConnectionFactory createTopicConnectionFactory() throws JMSException {        
        MQTopicConnectionFactory cf = new MQTopicConnectionFactory();        
        cf.setHostName(HOSTNAME);
        cf.setPort(PORT);
        cf.setQueueManager(QUEUE_MANAGER);
        cf.setTransportType(BasicRAWMQTestJUStd.TRANSPORT_TYPE);
        cf.setChannel(BasicRAWMQTestJUStd.CHANNEL);   
        cf.setClientID(getDurableTopic1Name() + "clientID");
        return cf;
    }

    /**
     * @see com.stc.jmsjca.test.core.Passthrough#createQueueConnectionFactory()
     */
    public QueueConnectionFactory createQueueConnectionFactory() throws JMSException {
        MQQueueConnectionFactory cf = new MQQueueConnectionFactory();        
        cf.setHostName(HOSTNAME);
        cf.setPort(PORT);
        cf.setQueueManager(QUEUE_MANAGER);
        cf.setTransportType(BasicRAWMQTestJUStd.TRANSPORT_TYPE);
        cf.setChannel(BasicRAWMQTestJUStd.CHANNEL);                
        return cf;
    }
}
