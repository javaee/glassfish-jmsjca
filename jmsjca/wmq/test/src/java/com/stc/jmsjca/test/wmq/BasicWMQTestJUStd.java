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
 * $RCSfile: BasicWMQTestJUStd.java,v $
 * $Revision: 1.1.1.1 $
 * $Date: 2007-01-21 07:52:12 $
 *
 * Copyright 2003-2007 Sun Microsystems, Inc. All Rights Reserved.  
 */

package com.stc.jmsjca.test.wmq;

import com.ibm.mq.MQC;
import com.ibm.mq.MQEnvironment;
import com.ibm.mq.MQException;
import com.ibm.mq.MQQueue;
import com.ibm.mq.MQQueueManager;
import com.ibm.mq.MQXid;
import com.ibm.mq.jms.MQConnectionFactory;
import com.ibm.mq.jms.MQQueueConnectionFactory;
import com.ibm.mq.jms.MQTopicConnectionFactory;
import com.ibm.mq.jms.MQXAConnectionFactory;
import com.ibm.mq.jms.MQXAQueueConnectionFactory;
import com.ibm.mq.jms.MQXATopicConnectionFactory;
import com.stc.jmsjca.wmq.WMQConnectionUrl;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageListener;
import javax.jms.Queue;
import javax.jms.QueueConnection;
import javax.jms.QueueConnectionFactory;
import javax.jms.QueueReceiver;
import javax.jms.QueueSender;
import javax.jms.QueueSession;
import javax.jms.Session;
import javax.jms.TextMessage;
import javax.jms.Topic;
import javax.jms.TopicConnection;
import javax.jms.TopicConnectionFactory;
import javax.jms.TopicPublisher;
import javax.jms.TopicSession;
import javax.jms.TopicSubscriber;
import javax.jms.XAQueueConnection;
import javax.jms.XAQueueConnectionFactory;
import javax.jms.XAQueueSession;
import javax.jms.XATopicConnection;
import javax.jms.XATopicConnectionFactory;
import javax.jms.XATopicSession;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.transaction.xa.XAResource;

import java.io.File;
import java.util.Properties;

import junit.framework.TestCase;


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
public class BasicWMQTestJUStd extends TestCase {

    static final String QUEUE_CF_JNDINAME = "wmq-queue-connection-factory";
    static final String TOPIC_CF_JNDINAME = "wmq-topic-connection-factory";
    static final String UNIFIED_CF_JNDINAME = "wmq-unified-connection-factory";
  
    static final String QUEUE_XACF_JNDINAME = "wmq-queue-xa-connection-factory";
    static final String TOPIC_XACF_JNDINAME = "wmq-topic-xa-connection-factory";
    static final String UNIFIED_XACF_JNDINAME = "wmq-unified-xa-connection-factory";

    private String HOSTNAME      = "runtime4";       //"10.18.73.56" "localhost" "ICAN-RTS"
    private int    PORT          = 1414;             // 1414, 5558 mq listener port
    private String QUEUE_MANAGER = "QM_runtime4";    //"QM_ican_rts ""WebSphere_ican_rts" "QM_cye_d6002k" 
                                                     // mq manager logical name
    static final String CHANNEL        = "SYSTEM.DEF.SVRCONN"; // need if it uses admin functionalities
    static final int    TRANSPORT_TYPE = 1;                    //1:JMSC_MQJMS_TP_CLIENT_MQ_TCPIP, 0: JMSC.MQJMS_TP_BINDINGS_MQ 
    static final String CLIENT_ID      = BasicWMQTestJUStd.class.getName(); 
    
    private static long sTime = System.currentTimeMillis();
    private InitialContext jndiContext = null;
      
    public class QueueMessageListener implements MessageListener { 
        
       QueueSession mSession = null;
       QueueSender  mProducer = null;;
       Message mMsg = null;
       
       QueueMessageListener(QueueConnection conn, Queue dest) {
          //try {
          // mSession = conn.createQueueSession(true, Session.AUTO_ACKNOWLEDGE);
          // mProducer = mSession.createSender(dest);
          //} catch(JMSException e) {
          //    e.printStackTrace();
          //}
       }
      
       public void onMessage(Message msg) {
           try {
             mMsg = msg;
             TextMessage textmsg = (TextMessage)msg;
             System.out.println("onMessage : received a message:" + textmsg.getText());
             if( mSession != null) { 
                 mProducer.send(msg);
                 mSession.commit();
             }
           } catch (JMSException e) {
               e.printStackTrace();
           }
       }
       
       public void close() {
           try {
               if(mSession != null) {
                 mProducer.close();
                 mSession.close();
               }
           }catch(JMSException e) {
               e.printStackTrace();
           }
       }
       
       public Message getReturnMessage() {
           return mMsg;
       }
    }
    
    /**
     * Constructor
     * 
     */
    public BasicWMQTestJUStd() {
           this(null);
    }

    /**
     * Constructor
     *  
     * @param name String
     */
    public BasicWMQTestJUStd(String name) {
        super(name);
    }
        
    /**
     * JUnit setup
     * @throws Exception if fails
     */
    public void setUp() throws Exception {
        init();
    }

    void checkSyncpoint(String queueName) {
        
        MQQueueManager queueManager = null;
        
        try {
            MQEnvironment.hostname = HOSTNAME;
            MQEnvironment.port = PORT;
            MQEnvironment.channel = CHANNEL;        
            queueManager = new MQQueueManager(QUEUE_MANAGER);
            
            if ( queueManager.getSyncpointAvailability() == MQC.MQSP_AVAILABLE) {
                
                System.out.println("MQC.MQSP_AVAILABLE");
            } else {
                System.out.println("MQC.MQSP_NOT_AVAILABLE");
            }
      
            queueManager.disconnect();
        } catch (MQException e) {
            if (queueManager != null) {
                try {
                    queueManager.disconnect();
                } catch (MQException ee) {
                    //ignore
                }
            }
            e.printStackTrace();
        } 
    }
    
    boolean verifyConnectionPool(int activeConnections, String queueName) {
        
        MQQueueManager queueManager = null;
        boolean ret = false;
        
        try {
            MQEnvironment.hostname = HOSTNAME;
            MQEnvironment.port = PORT;
            MQEnvironment.channel = CHANNEL;        
            queueManager = new MQQueueManager(QUEUE_MANAGER);
            
            if ( queueManager.getSyncpointAvailability() == MQC.MQSP_AVAILABLE) {
                
                System.out.println("MQC.MQSP_AVAILABLE");
            } else {
                System.out.println("MQC.MQSP_NOT_AVAILABLE");
            }
            
            MQQueue q = queueManager.accessQueue(queueName, MQC.MQOO_INQUIRE, null, null, null);
            
            if (q.getOpenInputCount() == activeConnections) {
                ret = true;
            } 
            q.getOpenOutputCount();
            
            queueManager.disconnect();
        } catch (MQException e) {
            if (queueManager != null) {
                try {
                    queueManager.disconnect();
                } catch (MQException ee) {
                    //ignore
                }
            }
            e.printStackTrace();
        } 
        return ret;
    }
    
    /**
     * 
     * @return String
     */
    public String getTestDestinationName() {
        return BasicWMQTestJUStd.class.getName() + sTime++;
    }
    
    /**
     * 
     * @return String
     */
    public String getConnectionUrl() {
        String url = System.getProperty("wmq.url", "wmq://" + HOSTNAME + ":" + PORT);
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

    public String getQueueManager(String hostStr) {

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
     *  @throws Exception if fails
     */    
    public void init() throws Exception {
        InitialContext ctx = getContext();
        {
            String url = System.getProperty("wmq.url", "wmq://" + HOSTNAME + ":" + PORT);
            if (url == null) {
               throw new RuntimeException("Failed to set wmq.url system property");
            }
                   
            WMQConnectionUrl wurl = new WMQConnectionUrl(url);
            Properties p = new Properties();
            wurl.getQueryProperties(p);         
                        
            QUEUE_MANAGER = p.getProperty("QueueManager", getQueueManager(wurl.getUrlParser().getHost()));                        
            HOSTNAME = wurl.getUrlParser().getHost();
            PORT = wurl.getUrlParser().getPort();            
        }
        // Create concreate connection factories and bind them into JNDI
        {
            //non-xa
            MQConnectionFactory mf = (MQConnectionFactory) new MQQueueConnectionFactory();
            mf.setHostName(HOSTNAME);
            mf.setPort(PORT);
            mf.setQueueManager(QUEUE_MANAGER);
            mf.setTransportType(TRANSPORT_TYPE);
            mf.setPollingInterval(100);
            ctx.rebind(QUEUE_CF_JNDINAME, mf);
        
            mf = (MQConnectionFactory) new MQTopicConnectionFactory();
            mf.setHostName(HOSTNAME);
            mf.setPort(PORT);
            mf.setQueueManager(QUEUE_MANAGER);
            mf.setTransportType(TRANSPORT_TYPE);
            mf.setClientID(CLIENT_ID);
            mf.setPollingInterval(100);
            ctx.rebind(TOPIC_CF_JNDINAME, mf);
        
            //xa       
            mf = (MQConnectionFactory) new MQXAQueueConnectionFactory();        
            mf.setHostName(HOSTNAME);
            mf.setPort(PORT);
            mf.setQueueManager(QUEUE_MANAGER);
            mf.setTransportType(TRANSPORT_TYPE);
            mf.setChannel(CHANNEL);
            mf.setPollingInterval(100);
            mf.setSyncpointAllGets(false);
            ctx.rebind(QUEUE_XACF_JNDINAME, mf);
            
            mf = (MQConnectionFactory) new MQXATopicConnectionFactory();
            mf.setHostName(HOSTNAME);
            mf.setPort(PORT);
            mf.setQueueManager(QUEUE_MANAGER);
            mf.setTransportType(TRANSPORT_TYPE);  
            mf.setChannel(CHANNEL);
            mf.setPollingInterval(100);
            //mf.setSyncpointAllGets(false);      
            ctx.rebind(TOPIC_XACF_JNDINAME, mf);
            
            mf = (MQConnectionFactory) new MQXAConnectionFactory();
            mf.setHostName(HOSTNAME);
            mf.setPort(PORT);
            mf.setQueueManager(QUEUE_MANAGER);
            mf.setTransportType(TRANSPORT_TYPE);
            mf.setPollingInterval(100);
            //mf.setSyncpointAllGets(false);            
            ctx.rebind(UNIFIED_XACF_JNDINAME, mf);            
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
    
    void drain(String queueName) {
  
        try {
            QueueConnectionFactory qfc = (QueueConnectionFactory) (getContext().lookup(QUEUE_CF_JNDINAME));
            QueueConnection conn = qfc.createQueueConnection("Administrator", "STC");
 
            QueueSession sess = conn.createQueueSession(true, Session.AUTO_ACKNOWLEDGE);
            
            Queue dest = sess.createQueue("queue:///" + queueName);
            QueueReceiver consumer = sess.createReceiver(dest);
            conn.start();
            
            int count = 0;
            while(true) {
                Message msg = consumer.receive(5000);
                if (msg != null) {
                    sess.commit();
                    count++;
                } else {
                    break;
                }
            }
            System.out.println("drained " + count + " messages from queue:" + queueName);
            
            consumer.close();
            sess.close();
            conn.close();
        } catch (Exception ignore) {
        }
    }
    
    /**
     * test000: xa queue domain
     */
    public void test000() {
        
        System.out.println("test000...");   
        
        XAQueueConnection conn = null;      
        XAQueueSession xasess = null;
        QueueSession sess = null;
        XAResource xa = null;
        try {
            XAQueueConnectionFactory qfc = 
                (XAQueueConnectionFactory) (getContext().lookup(QUEUE_XACF_JNDINAME));

            conn = qfc.createXAQueueConnection("Administrator", "STC");
            
            xasess = conn.createXAQueueSession();

            sess = xasess.getQueueSession();
            xa = xasess.getXAResource();

            //create logical queue testqueue1 from MQ Explorer first 
            Queue dest = sess.createQueue("queue:///testqueue2");

            drain("testqueue2");
            
            QueueReceiver consumer = sess.createReceiver(dest);
            QueueSender producer = sess.createSender(dest);

            long id = System.currentTimeMillis();
            Message msg = (Message) sess.createTextMessage("This is a test000 message:" + id);

            conn.start();
           
            //Here is where is the problem is. MQSeries JMS classes dont provide a way to get the transaction id.
            javax.transaction.xa.Xid xd1 = new MQXid((int) id, "aGlobalTxn".getBytes(), "JMSbranchRemote".getBytes());
            xa.start(xd1, XAResource.TMNOFLAGS);
            producer.send(msg);
            xa.end(xd1, XAResource.TMSUCCESS);
            xa.prepare(xd1);
            xa.commit(xd1, false);

            javax.transaction.xa.Xid xd2 = new MQXid((int) id, "aGlobalTxn".getBytes(), "JMSbranchRemote".getBytes());
            xa.start(xd2, XAResource.TMNOFLAGS);
            msg = consumer.receive();

            // test out
            XAQueueSession oxasess = conn.createXAQueueSession();
            QueueSession osess = oxasess.getQueueSession();
            XAResource oxa = oxasess.getXAResource();
            //create logical queue testqueue1 from MQ Explorer first 
            Queue odest = osess.createQueue("queue:///testqueue3");
            QueueSender oproducer = osess.createSender(odest);
            long oid = System.currentTimeMillis();
            javax.transaction.xa.Xid oxd = new MQXid((int) oid, "aGlobalTxn".getBytes(), "JMSbranchRemote".getBytes());
            
            //test out
            //oxa.start(oxd, XAResource.TMJOIN);a MQ bug ?
            oxa.start(oxd, XAResource.TMNOFLAGS); 
            oproducer.send(msg);
            oxa.end(oxd, XAResource.TMSUCCESS);
            oxa.prepare(oxd);
            oxa.commit(oxd, false);
 
            xa.end(xd2, XAResource.TMSUCCESS);
            xa.prepare(xd2);
            xa.commit(xd2, false);

 
            if (msg != null && (msg instanceof TextMessage)) {
                String txt = ((TextMessage) msg).getText();
                if (txt.equals("This is a test000 message:" + id)) {
                    System.out.println("test000 ok: " + txt);
                } else {
                    System.out.println(txt + " is not correct: " + "This is a test000 message:" + id);
                    throw new JMSException("received a wrong text message."); 
                }
            } else {
                throw new JMSException("received a wrong message type or no message received."); 
            }
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
     * test001: queue domain
     */
    public void test001() {
        
        System.out.println("test001...");   
        
        QueueConnection conn = null;
        QueueSession sess = null;
        try {
            QueueConnectionFactory qfc = (QueueConnectionFactory) (getContext().lookup(QUEUE_CF_JNDINAME));
            conn = qfc.createQueueConnection("Administrator", "STC");
            conn.start();

            if (!verifyConnectionPool(0, "testqueue1")) {                
                sess.close();                
                assertTrue(false);
            }
            
            drain("testqueue1");
            
            sess = conn.createQueueSession(true, Session.AUTO_ACKNOWLEDGE);
            //create logical queue testqueue1 from MQ Explorer first 
            Queue dest = sess.createQueue("queue:///testqueue1");
            QueueSender producer = sess.createSender(dest);
            QueueReceiver consumer = sess.createReceiver(dest);

            if (!verifyConnectionPool(1, "testqueue1")) {                
                sess.close();                
                assertTrue(false);
            }
            
            long id = System.currentTimeMillis();
            Message msg = (Message) sess.createTextMessage("This is a test001 message:" + id);

            producer.send(msg);
            sess.commit();

            msg = consumer.receive();
            sess.commit();

            if (msg != null && (msg instanceof TextMessage)) {
                String txt = ((TextMessage) msg).getText();
                if (txt.equals("This is a test001 message:" + id)) {
                    System.out.println("test001 ok: " + txt);
                } else {
                    System.out.println(txt + " is not correct: " + "This is a test001 message:" + id);
                    throw new JMSException("received a wrong text message."); 
                }
            } else {
                throw new JMSException("received a wrong message type or no message received.");
            }
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
     * test001: queue domain
     */
    public void test010() {
        
        System.out.println("test010...");   
        
        QueueConnection conn = null;
        QueueSession sess1 = null;
        QueueSession sess2 = null;
        
        try {
            QueueConnectionFactory qfc = (QueueConnectionFactory) (getContext().lookup(QUEUE_CF_JNDINAME));
            conn = qfc.createQueueConnection("Administrator", "STC");
 
            sess1 = conn.createQueueSession(true, Session.AUTO_ACKNOWLEDGE);
            sess2 = conn.createQueueSession(false, Session.AUTO_ACKNOWLEDGE);
            
            drain("testqueue1");
            
            //create logical queue testqueue1 from MQ Explorer first 
            Queue dest = sess1.createQueue("queue:///testqueue1");
            QueueSender producer = sess1.createSender(dest);
            
            QueueReceiver consumer = sess2.createReceiver(dest);
            Queue rdest = sess2.createQueue("queue:///Queue1");
            QueueMessageListener listener = new QueueMessageListener(conn, rdest);
            consumer.setMessageListener(listener);
            conn.start();
            
            long id = System.currentTimeMillis();
            Message msg = (Message) sess1.createTextMessage("This is a test010 message:" + id);

            producer.send(msg);
            sess1.commit();

            while(listener.getReturnMessage()==null) {
                Thread.sleep(10);
            }
          
            msg = listener.getReturnMessage();
            listener.close();
            
            if (msg != null && (msg instanceof TextMessage)) {
                String txt = ((TextMessage) msg).getText();
                if (txt.equals("This is a test010 message:" + id)) {
                    System.out.println("test010 ok: " + txt);
                } else {
                    System.out.println(txt + " is not correct: " + "This is a test010 message:" + id);
                    throw new JMSException("received a wrong text message."); 
                }
            } else {
                throw new JMSException("received a wrong message type or no message received.");
            }
        } catch (JMSException e) {
            e.printStackTrace();
            assertTrue(false);            
        } catch (Exception e) {
            e.printStackTrace();
            assertTrue(false);            
        } finally {
            try {
                if (sess2 != null) {
                    sess2.close();
                    sess2 = null;
                }
                if (sess2 != null) {
                    sess2.close();
                    sess2 = null;
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
     * test020: xa queue domain
     */
    public void xtest020() {
        
        System.out.println("test020...");   
        
        XAQueueConnection conn2 = null;
        XAQueueConnection conn1 = null;
        XAQueueSession xasess1 = null;
        QueueSession sess1 = null;
        QueueSession sess2 = null;
       
        try {         
            MQXAQueueConnectionFactory qfc = 
                (MQXAQueueConnectionFactory) (getContext().lookup(QUEUE_XACF_JNDINAME));

            qfc.setSyncpointAllGets(true);
            if (qfc.getSyncpointAllGets()) {
                System.out.println("Syncpoint");
            } else {
                System.out.println("not Syncpoint");
            }
            
        
            conn1= qfc.createXAQueueConnection("Administrator", "STC");
            xasess1 = conn1.createXAQueueSession();
            sess1 = xasess1.getQueueSession();
            
            //create logical queue testqueue1 from MQ Explorer first 
            Queue dest = sess1.createQueue("queue:///Queue1");
            long id = System.currentTimeMillis();
            Message msg = (Message) sess1.createTextMessage("This is a test020 message:" + id);

            MessageConsumer consumer = sess1.createConsumer(dest);	
            Queue rdest = sess1.createQueue("queue:///rtestqueue2");          
            QueueMessageListener listener = new QueueMessageListener(conn1, rdest);
            consumer.setMessageListener(listener);

            
            conn1.start();
            
            while(listener.getReturnMessage()==null) {
                Thread.sleep(10);
            }
          
            msg = listener.getReturnMessage();
            listener.close();
                       
            if (msg != null && (msg instanceof TextMessage)) {
                String txt = ((TextMessage) msg).getText();
                    System.out.println("test020 ok: " + txt);
            } else {
                throw new JMSException("received a wrong message type or no message received.");
            }
          
        } catch (JMSException e) {
            e.printStackTrace();
            assertTrue(false);            
        } catch (Exception e) {
            e.printStackTrace();
            assertTrue(false);            
        } finally {
            try {
                if (sess1 != null) {
                    sess1.close();
                    sess1 = null;
                }
                if (sess2 != null) {
                    sess2.close();
                    sess2 = null;
                }
                if (conn2 != null) {
                    conn2.close();
                    conn2 = null;
                }
                if (conn1 != null) {
                    conn1.close();
                    conn1 = null;
                }
                
            } catch (JMSException e) {
                //ignore
            }
        }
    }

    /**
     * test002: xa queue domain
     */
    public void test002() {
        
        System.out.println("test002...");   
        
        XAQueueConnection conn = null;
        XAQueueSession xasess = null;
        QueueSession sess = null;
        XAResource xa = null;
        try {
            XAQueueConnectionFactory qfc = 
                (XAQueueConnectionFactory) (getContext().lookup(QUEUE_XACF_JNDINAME));

            conn = qfc.createXAQueueConnection("Administrator", "STC");
            xasess = conn.createXAQueueSession();

            sess = xasess.getQueueSession();
            xa = xasess.getXAResource();

            //create logical queue testqueue1 from MQ Explorer first 
            Queue dest = sess.createQueue("queue:///testqueue2");

            drain("testqueue2");
            
            QueueReceiver consumer = sess.createReceiver(dest);
            QueueSender producer = sess.createSender(dest);

            long id = System.currentTimeMillis();
            Message msg = (Message) sess.createTextMessage("This is a test002 message:" + id);

            conn.start();

            //Here is where is the problem is. MQSeries JMS classes dont provide a way to get the transaction id.
            javax.transaction.xa.Xid xd1 = new MQXid((int) id, "aGlobalTxn".getBytes(), "JMSbranchRemote".getBytes());
            xa.start(xd1, XAResource.TMNOFLAGS);
            producer.send(msg);
            xa.end(xd1, XAResource.TMSUCCESS);
            xa.prepare(xd1);
            xa.commit(xd1, false);

            javax.transaction.xa.Xid xd2 = new MQXid((int) id, "aGlobalTxn".getBytes(), "JMSbranchRemote".getBytes());
            xa.start(xd2, XAResource.TMNOFLAGS);
            msg = consumer.receive();
            xa.end(xd2, XAResource.TMSUCCESS);
            xa.commit(xd2, true);

            if (msg != null && (msg instanceof TextMessage)) {
                String txt = ((TextMessage) msg).getText();
                if (txt.equals("This is a test002 message:" + id)) {
                    System.out.println("test002 ok: " + txt);
                } else {
                    System.out.println(txt + " is not correct: " + "This is a test002 message:" + id);
                    throw new JMSException("received a wrong text message."); 
                }
            } else {
                throw new JMSException("received a wrong message type or no message received."); 
            }
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
     * test003: topic domain
     *
     */
    public void test003() {
        
        System.out.println("test003..."); 
        
        TopicConnection conn = null;
        TopicSession sess = null;
        try {
            TopicConnectionFactory tfc = (TopicConnectionFactory) (getContext().lookup(TOPIC_CF_JNDINAME));
            conn = tfc.createTopicConnection("Administrator", "STC");
            conn.start();

            sess = conn.createTopicSession(true, Session.AUTO_ACKNOWLEDGE);
            //create logical queue testqueue1 from MQ Explorer first 
            Topic dest = sess.createTopic("topic:///testtopic1");
            TopicSubscriber consumer = sess.createSubscriber(dest);
            TopicPublisher producer = sess.createPublisher(dest);

            long id = System.currentTimeMillis();
            Message msg = (Message) sess.createTextMessage("This is a test003 message:" + id);

            producer.send(msg);
            sess.commit();

            msg = consumer.receive();
            sess.commit();

            if (msg != null && (msg instanceof TextMessage)) {
                String txt = ((TextMessage) msg).getText();
                if (txt.equals("This is a test003 message:" + id)) {
                    System.out.println("test003 ok: " + txt);
                } else {
                    System.out.println(txt + " is not correct: " + "This is a test003 message:" + id);
                    throw new JMSException("received a wrong text message."); 
                }
            } else {
                throw new JMSException("received a wrong message type or no message received.");
            }
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
     * test004: xa topic domain
     *
     */
    public void test004() {
        
        System.out.println("test004...");   
        
         XATopicConnection conn = null;
         XATopicSession xasess = null;
         TopicSession sess = null;
         XAResource xa = null;
         try {
             XATopicConnectionFactory tfc = 
                 (XATopicConnectionFactory) (getContext().lookup(TOPIC_XACF_JNDINAME));

             conn = tfc.createXATopicConnection("Administrator", "STC");
             xasess = conn.createXATopicSession();

             sess = xasess.getTopicSession();
             xa = xasess.getXAResource();

             //create logical queue testqueue1 from MQ Explorer first
             Topic dest = sess.createTopic("topic:///testtopic2");

             TopicSubscriber consumer = sess.createSubscriber(dest);
             TopicPublisher producer = sess.createPublisher(dest);

             conn.start();

             long id = System.currentTimeMillis();
             Message msg = (Message) sess.createTextMessage("This is a test004 message:" + id);

             javax.transaction.xa.Xid xd1 = new MQXid((int) id, "aGlobalTxn".getBytes(),  "JMSbranchRemote".getBytes());
             xa.start(xd1, XAResource.TMNOFLAGS);
             producer.publish(msg);
             xa.end(xd1, XAResource.TMSUCCESS);
             xa.commit(xd1, true);

             javax.transaction.xa.Xid xd2 = new MQXid((int) id, "aGlobalTxn".getBytes(),  "JMSbranchRemote".getBytes());
             xa.start(xd2, XAResource.TMNOFLAGS);
             msg = consumer.receive();
             xa.end(xd2, XAResource.TMSUCCESS);
             xa.commit(xd2, true);

             if (msg != null && (msg instanceof TextMessage)) {
                 String txt = ((TextMessage) msg).getText();
                 if (txt.equals("This is a test004 message:" + id)) {
                     System.out.println("test004 ok: " + txt);
                 } else {
                     System.out.println(txt + " is not correct: " + "This is a test004 message:" + id);
                     throw new JMSException("received a wrong text message."); 
                 }
             } else {
                 throw new JMSException("received a wrong message type or no message received.");
             }
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
     * test005: topic domain
     *
     */
    public void test005() {
        
        System.out.println("test005...");   
        
        TopicConnection conn = null;
        TopicSession sess = null;
        try {
            TopicConnectionFactory tfc = (TopicConnectionFactory) (getContext().lookup(TOPIC_CF_JNDINAME));
            conn = tfc.createTopicConnection("Administrator", "STC");
            conn.start();

            sess = conn.createTopicSession(true, Session.AUTO_ACKNOWLEDGE);
            //create logical queue testqueue1 from MQ Explorer first
            String topicName = getTestDestinationName();
            Topic dest = sess.createTopic("topic:///" + topicName);
            TopicSubscriber consumer = sess.createDurableSubscriber(dest, this.getClass().getName());
            TopicPublisher producer = sess.createPublisher(dest);

            long id = System.currentTimeMillis();
            Message msg = (Message) sess.createTextMessage("This is a test005-1 message:" + id);

            producer.send(msg);
            sess.commit();

            msg = consumer.receive();
            sess.commit();

            if (msg != null && (msg instanceof TextMessage)) {
                String txt = ((TextMessage) msg).getText();
                if (txt.equals("This is a test005-1 message:" + id)) {
                    System.out.println("test005-1 ok: " + txt);
                } else {
                    System.out.println(txt + " is not correct: " + "This is a test005-1 message:" + id);
                    throw new JMSException("received a wrong text message."); 
                }
            } else {
                throw new JMSException("received a wrong message type or no message received.");
            }

            consumer.close();            
            sess.unsubscribe(this.getClass().getName());
            producer.close();
            sess.close();
            
            
            sess = conn.createTopicSession(true, Session.AUTO_ACKNOWLEDGE);
            producer = sess.createPublisher(dest);

            id = System.currentTimeMillis();
            msg = (Message) sess.createTextMessage("This is a test005-2 message:" + id);

            producer.send(msg);
            sess.commit();
            
            consumer = sess.createDurableSubscriber(dest, this.getClass().getName());            
            msg = consumer.receive(5000);            
            assertTrue(msg == null);
            
            System.out.println("test005-2 ok");
            
            consumer.close();            
            sess.unsubscribe(this.getClass().getName());
            producer.close();
            
            System.out.println("last test done");
            
        } catch (JMSException e) {
            e.printStackTrace();
            assertTrue(false);
        } catch (Exception e) {
            e.printStackTrace();
            assertTrue(false);            
        } finally {
            try {
                if (sess != null) {
                    sess.unsubscribe(this.getClass().getName());                    
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

}    