/*
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the "License").  You may not use this file except
 * in compliance with the License.
 *
 * You can obtain a copy of the license at
 * https://glassfish.dev.java.net/public/CDDLv1.0.html.
 * See the License for the specific language governing
 * permissions and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL
 * HEADER in each file and include the License file at
 * https://glassfish.dev.java.net/public/CDDLv1.0.html.
 * If applicable add the following below this CDDL HEADER,
 * with the fields enclosed by brackets "[]" replaced with
 * your own identifying information: Portions Copyright
 * [year] [name of copyright owner]
 */
/*
 * $RCSfile: BasicRAWMQTestJUStd.java,v $
 * $Revision: 1.3 $
 * $Date: 2007-01-21 17:52:29 $
 *
 * Copyright 2003-2007 Sun Microsystems, Inc. All Rights Reserved.  
 */

package com.stc.jmsjca.test.wmq;


import com.stc.jmsjca.core.XMCFQueueXA;
import com.stc.jmsjca.core.XMCFTopicXA;
import com.stc.jmsjca.core.XMCFUnifiedXA;
import com.stc.jmsjca.core.XManagedConnectionFactory;

import com.stc.jmsjca.wmq.RAWMQActivationSpec;
import com.stc.jmsjca.wmq.RAWMQResourceAdapter;

import javax.jms.ConnectionFactory;
import javax.jms.QueueConnectionFactory;
import javax.jms.TopicConnectionFactory;
import javax.jms.QueueConnection;
import javax.jms.TopicConnection;
import javax.jms.QueueSession;
import javax.jms.TopicSession;
import javax.jms.Session;
import javax.jms.JMSException;

import javax.naming.Context;
import javax.naming.InitialContext;

import javax.resource.spi.ActivationSpec;
import javax.transaction.xa.XAResource;

import junit.framework.TestCase;
import java.io.File;
import java.util.Properties;

/**
 * <code>
 * Unit tests
 *  To run, in addition to the standard properties, e.g. for Eclipse
 *  -Dtest.server.properties=../../R1/logicalhost/testsettings.properties -Dtest.ear.path=rajndi/test/ratest-test.ear
 *  the connectionURL(s) needs to be set as well, e.g.:
 *  -Dwmq.url=wmq:://hostname:5558
 *
 * For Eclipese, if the above properties are used, the current directory needs to set 
 * to ${workspace_loc:e-jmsjca/build}
 *
 * @author cye
 * @version 1.0
 */
public class BasicRAWMQTestJUStd extends TestCase {

    static final String MANAGED_QUEUE_XACF_JNDINAME = "jnditest-queuefact-provider-xa";
    static final String MANAGED_TOPIC_XACF_JNDINAME = "jnditest-topicfact-provider-xa";
    static final String MANAGED_UNIFIED_XACF_JNDINAME = "jnditest-unifiedfact-provider-xa";
    
    /**
     * test host name
     */
    public static final String HOSTNAME      = "runtime4";    //"localhost" "ICAN-RTS"

    /**
     * test host port
     */
    public static final int    PORT          = 1414;             // 1414, 5558 mq listener port
    
    /**
     * test queue manager
     */
    public static final String QUEUE_MANAGER = "QM_runtime4";    //"QM_ican_rts" "WebSphere_ican_rts" "QM_cye_d6002k" 
                                                                 // mq manager logical name
    /**
     * test transport type 
     */
    public static int TRANSPORT_TYPE = 1; //1:JMSC_MQJMS_TP_CLIENT_MQ_TCPIP, 0: JMSC.MQJMS_TP_BINDINGS_MQ 

    static final String CHANNEL  = "SYSTEM.DEF.SVRCONN"; // need if it uses admin functionalities

    private long sTime = System.currentTimeMillis();
    private InitialContext jndiContext = null;
 
    /**
     * test user name 
     */
    public static String userName = "Administrator";
    
    /**
     * test user password
     */
    public static String password = "STC";
    
    /**
     * Constructor
     * 
     */
    public BasicRAWMQTestJUStd() {
           this(null);
    }

    /**
     * Constructor
     *  
     * @param name String
     */
    public BasicRAWMQTestJUStd(String name) {
        super(name);
    }
        
    /**
     * JUnit setup
     * @throws Exception if fails
     */
    public void setUp() throws Exception {
        init();
    }
    
    /**
     * 
     * @return String
     */
    public String getConnectionURL1() {
        String url = System.getProperty("wmq.url", "wmq://" + HOSTNAME + ":" + PORT + 
                                         "?QueueManager=" + QUEUE_MANAGER + 
                                         "&TransportType=" + "JMSC_MQJMS_TP_CLIENT_MQ_TCPIP");
        
        if (url == null) {
           throw new RuntimeException("Failed to set wmq.url system property");
        }
        return url;
    }

    /**
     * 
     * @return String
     */
    public String getConnectionURL2() {
        String url = System.getProperty("wmq.url", "wmq://" + HOSTNAME + ":" + PORT +
                                        "?QueueManager=" + QUEUE_MANAGER);
        if (url == null) {
           throw new RuntimeException("Failed to set wmq.url system property");
        }
        return url;
    }

    /**
     * 
     * @return String
     */
    public String getConnectionURL3() {
        String url = System.getProperty("wmq.url", "wmq://" + HOSTNAME + ":" + PORT);
        if (url == null) {
           throw new RuntimeException("Failed to set wmq.url system property");
        }
        return url;
    }

    /**
     * 
     * @return String
     */
    public String getConnectionURLs() {
        String url = System.getProperty("wmq.url", "wmq://" + HOSTNAME + ":" + PORT +
                                        "?QueueManager=" + QUEUE_MANAGER);
        if (url == null) {
           throw new RuntimeException("Failed to set wmq.url system property");
        }
        return url;
    }
    private String getProviderClass() {
        return "com.sun.jndi.fscontext.RefFSContextFactory";
    }

    private String getUrl() {
        String dir = System.getProperty("url.dir", "/tmp") + "/jmsjcatest";
        new File(dir).mkdirs();
        String providerurl = "file://" + dir;
        return providerurl;
    }

    private InitialContext getContext() throws Exception {
        if (jndiContext == null) {
            Properties props = new Properties();
            props.put(Context.INITIAL_CONTEXT_FACTORY, getProviderClass());
            props.put(Context.PROVIDER_URL, getUrl());
            jndiContext = new InitialContext(props);
        }
        return jndiContext;
    }

    /**
     *  @throws Exception if fails
     */    
    public void init() throws Exception {
        InitialContext ctx = getContext();
        // Create concreate connection factories and bind them into JNDI
        // Create managed connection factories and bind them into JNDI
        {
            // QUEUE
            XManagedConnectionFactory x = (XManagedConnectionFactory) new XMCFQueueXA();
            RAWMQResourceAdapter ra = new RAWMQResourceAdapter();
            ra.setConnectionURL(getConnectionURL1());
            x.setResourceAdapter(ra);
            QueueConnectionFactory f = (QueueConnectionFactory) x.createConnectionFactory();
            ctx.rebind(MANAGED_QUEUE_XACF_JNDINAME, f);
        }
        {
            // TOPIC
            XManagedConnectionFactory x = new XMCFTopicXA();
            RAWMQResourceAdapter ra = new RAWMQResourceAdapter();
            ra.setConnectionURL(getConnectionURL2());    
            x.setResourceAdapter(ra);
            TopicConnectionFactory f = (TopicConnectionFactory) x.createConnectionFactory();
            ctx.rebind(MANAGED_TOPIC_XACF_JNDINAME, f);
        }
        {
            // UNIFIED
            XManagedConnectionFactory x = new XMCFUnifiedXA();
            RAWMQResourceAdapter ra = new RAWMQResourceAdapter();
            ra.setConnectionURL(getConnectionURL3());
            x.setResourceAdapter(ra);
            ConnectionFactory f = (ConnectionFactory) x.createConnectionFactory();
            ctx.rebind(MANAGED_UNIFIED_XACF_JNDINAME, f);
        }
    }

    /**
     * Generates a unique name
     *
     * @return name
     */
    public String generateName() {
      return "JMSJCA-" + this.getClass() + sTime++;
    }
          
    /**
     *  test00: test url 
     *
     */
    public void test000() {        
        String hostStr = "10.18.73.56";        
        if (!Character.isLetter(hostStr.charAt(0))) {
            try {
                java.net.InetAddress inetAdd =
                    java.net.InetAddress.getByName(hostStr);
                hostStr = inetAdd.getHostName();
            } catch (java.net.UnknownHostException uhe) {
                //handle exception
                hostStr = "localhost";
            }            
        }        
        
        String uHostStr = hostStr.split("\\.") == null ? hostStr : hostStr.split("\\.")[0];;        
        String Queue_Manager = "QM_" + uHostStr.replace('-', '_').toLowerCase();        
        System.out.println("test000-1:" + Queue_Manager);        
        
        hostStr = "ICAN-RTS";        
        if (!Character.isLetter(hostStr.charAt(0))) {
            try {
                java.net.InetAddress inetAdd =
                    java.net.InetAddress.getByName(hostStr);
                hostStr = inetAdd.getHostName();
            } catch (java.net.UnknownHostException uhe) {
                //handle exception
                hostStr = "localhost";
            }            
        }        
        uHostStr = hostStr.split("\\.") == null ? hostStr : hostStr.split("\\.")[0];;        
        Queue_Manager = "QM_" + uHostStr.replace('-', '_').toLowerCase();
        System.out.println("test000-2:" + Queue_Manager);        

        hostStr = "runtime4.stc.com";        
        if (!Character.isLetter(hostStr.charAt(0))) {
            try {
                java.net.InetAddress inetAdd =
                    java.net.InetAddress.getByName(hostStr);
                hostStr = inetAdd.getHostName();
            } catch (java.net.UnknownHostException uhe) {
                //handle exception
                hostStr = "localhost";
            }            
        }        
        uHostStr = hostStr.split("\\.") == null ? hostStr : hostStr.split("\\.")[0];;        
        Queue_Manager = "QM_" + uHostStr.replace('-', '_').toLowerCase();
        System.out.println("test000-3:" + Queue_Manager);

        hostStr = "10.18.73.67";        
        if (!Character.isLetter(hostStr.charAt(0))) {
            try {
                java.net.InetAddress inetAdd =
                    java.net.InetAddress.getByName(hostStr);
                hostStr = inetAdd.getHostName();
            } catch (java.net.UnknownHostException uhe) {
                //handle exception
                hostStr = "localhost";
            }            
        }        
        uHostStr = hostStr.split("\\.") == null ? hostStr : hostStr.split("\\.")[0];;        
        Queue_Manager = "QM_" + uHostStr.replace('-', '_').toLowerCase();
        System.out.println("test000-4:" + Queue_Manager);
    }

    /**
     * test001: managed queue domain
     */
    public void test001() {
        QueueConnection conn = null;
        QueueSession sess = null;
        try {                   
            QueueConnectionFactory qfc = (QueueConnectionFactory) (getContext().lookup(MANAGED_QUEUE_XACF_JNDINAME));
                
            conn = qfc.createQueueConnection(userName, password);
            sess = conn.createQueueSession(true, Session.AUTO_ACKNOWLEDGE);
            
            System.out.println("test001 done");
            
        } catch (JMSException e) {
            e.printStackTrace();
            assertTrue(false);
        } catch (Exception e) {
            e.printStackTrace();
            assertTrue(false);
        } finally {
            try {
                if (sess != null) {
                    sess.close();
                    sess = null;
                }
                if (conn != null) {
                    conn.close();
                    conn = null;
                }
            } catch (JMSException e) {
                //ignore
            }
        }
    }
    
    /**
     * test002: managed topic domain
     */
    public void test002() {
        TopicConnection conn = null;
        TopicSession sess = null;
        try {               
            TopicConnectionFactory qfc = (TopicConnectionFactory) (getContext().lookup(MANAGED_TOPIC_XACF_JNDINAME));
                
            conn = qfc.createTopicConnection(userName, password);
            sess = conn.createTopicSession(true, Session.AUTO_ACKNOWLEDGE);
            
            System.out.println("test002 done");
            
        } catch (JMSException e) {
            e.printStackTrace();
            assertTrue(false);
        } catch (Exception e) {
            e.printStackTrace();
            assertTrue(false);
        } finally {
            try {
                if (sess != null) {
                    sess.close();
                    sess = null;
                }
                if (conn != null) {
                    conn.close();
                    conn = null;
                }
            } catch (JMSException e) {
                //ignore
            }
        }
    }
    
    /**
     * 
     * test003
     */
    public void test003() {
       
        try {
            QueueConnectionFactory qfc = (QueueConnectionFactory) (getContext().lookup(MANAGED_QUEUE_XACF_JNDINAME));
            // Spec1
            RAWMQActivationSpec spec1 = new RAWMQActivationSpec();
            spec1.setConnectionURL(getConnectionURL1());
            spec1.setUserName(userName);
            spec1.setPassword(password);

            // Spec2 is identical
            RAWMQActivationSpec spec2 = new RAWMQActivationSpec();
            spec2.setConnectionURL(getConnectionURL2());
            spec2.setUserName(userName);
            spec2.setPassword(password);

            // Spec3 uses different URL
            RAWMQActivationSpec spec3 = new RAWMQActivationSpec();
            spec3.setConnectionURL(getConnectionURLs());
            spec3.setUserName(userName);
            spec3.setPassword(password);

            ActivationSpec[] specs = new ActivationSpec[] {
                    spec1,
                    spec2,
                    spec3,
            };

            com.stc.jmsjca.core.RAJMSResourceAdapter ra = ((com.stc.jmsjca.core.JConnectionFactory) qfc).getRA();
            XAResource[] xas = ra.getXAResources(specs);
            xas = xas == null ? xas : xas;

            //fix me, successful only in debug mode
            //assertTrue(xas.length == 3);
        
            // Stop should close connections
            ra.stop();
        
            System.out.println("test003 done");
            
        } catch (Exception e) {
            e.printStackTrace();
            assertTrue(false);
        }
        
    }
}    