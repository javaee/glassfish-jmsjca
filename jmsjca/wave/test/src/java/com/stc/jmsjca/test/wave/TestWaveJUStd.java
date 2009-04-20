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

package com.stc.jmsjca.test.wave;

import com.spirit.wave.WaveProfile;
import com.spirit.wave.jms.WaveXAConnectionFactory;
import com.spirit.wave.jms.WaveXAQueueConnectionFactory;
import com.spirit.wave.jms.WaveXATopicConnectionFactory;
import com.stc.jmsjca.core.Options;
import com.stc.jmsjca.core.RAJMSResourceAdapter;
import com.stc.jmsjca.core.XMCFQueueXA;
import com.stc.jmsjca.core.XMCFTopicXA;
import com.stc.jmsjca.core.XMCFUnifiedXA;
import com.stc.jmsjca.core.XManagedConnectionFactory;
import com.stc.jmsjca.test.core.JMSProvider;
import com.stc.jmsjca.test.core.XTestBase;
import com.stc.jmsjca.util.Semaphore;
import com.stc.jmsjca.util.UrlParser;
import com.stc.jmsjca.wave.RAWaveObjectFactory;
import com.stc.jmsjca.wave.RAWaveResourceAdapter;
import com.stc.jmsjca.wave.WaveUrlParser;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageListener;
import javax.jms.MessageProducer;
import javax.jms.Queue;
import javax.jms.QueueConnection;
import javax.jms.QueueConnectionFactory;
import javax.jms.QueueSession;
import javax.jms.Session;
import javax.jms.TopicConnection;
import javax.jms.TopicConnectionFactory;
import javax.jms.XAQueueConnectionFactory;
import javax.naming.Context;
import javax.naming.InitialContext;

import java.io.File;
import java.util.Properties;

/**
 * <code>
 * Unit tests
*  See Base
 *
 * @author 
 * @version 1.0
 */
public class TestWaveJUStd extends XTestBase {

    /**
     * Constructor
     */
    public TestWaveJUStd() {
        this(null);
    }

    /**
     * Constructor
     *
     * @param name junit test name
     */
    public TestWaveJUStd(String name) {
        super(name);
    }
    
    /**
     * @see com.stc.jmsjca.test.core.XTestBase#getJMSProvider()
     */
    public JMSProvider getJMSProvider() {
        return new WaveProvider();
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

    private InitialContext getContext() throws Throwable {
        Properties props = new Properties();
        props.put(Context.INITIAL_CONTEXT_FACTORY, getProviderClass());
        props.put(Context.PROVIDER_URL, getUrl());
        return new InitialContext(props);
    }

    protected static String jndinameTopicDelegateXA = "jnditest-topicfact-provider-xa";
    protected static String jndinameQueueDelegateXA = "jnditest-queuefact-provider-xa";
    protected static String jndinameUnifiedDelegateXA = "jnditest-unifiedfact-provider-xa";

    /**
     * Creates connection properties to server
     *
     * @return populated profile
     * @throws JMSException on failure
     */
    public static WaveProfile createWaveProfile(String url) throws JMSException {
        Properties profileprops = new Properties();
        profileprops.setProperty("driverNames", "SpiritWave");
        profileprops.setProperty("SpiritWave.messageChannels", url);
        WaveProfile profile = new WaveProfile();
        profile.buildProfile(profileprops);
        return profile;
    }

    public void init(boolean producerPooling) throws Throwable {
        InitialContext ctx = getContext();

        // Create concreate connection factories and bind them into JNDI
        {
            // Create a concrete provider factory (will be used by the managed connection factory)
            WaveProfile p = createWaveProfile(getConnectionUrl());
            ctx.rebind(jndinameQueueDelegateXA, new WaveXAQueueConnectionFactory(p));
            ctx.rebind(jndinameTopicDelegateXA, new WaveXATopicConnectionFactory(p));
            ctx.rebind(jndinameUnifiedDelegateXA, new WaveXAConnectionFactory(p));
        }

        // Create MCFs, get the CF that will be used by the application and bind that
        // CF into JNDI
        {
            // QUEUE
            XManagedConnectionFactory x = new XMCFQueueXA();
            RAWaveResourceAdapter ra = new RAWaveResourceAdapter();
            ra.setConnectionURL(getConnectionUrl());
            x.setResourceAdapter(ra);
            x.setProducerPooling(Boolean.toString(producerPooling));
            QueueConnectionFactory f = (QueueConnectionFactory) x.createConnectionFactory();
            ctx.rebind(appjndiQueue, f);
        }


        {
            // TOPIC
            XManagedConnectionFactory x = new XMCFTopicXA();
            RAWaveResourceAdapter ra = new RAWaveResourceAdapter();
            ra.setConnectionURL(getConnectionUrl());
            x.setResourceAdapter(ra);
            x.setProducerPooling(Boolean.toString(producerPooling));
            TopicConnectionFactory f = (TopicConnectionFactory) x.createConnectionFactory();
            ctx.rebind(appjndiTopic, f);
        }


        {
            // UNIFIED
            XManagedConnectionFactory x = new XMCFUnifiedXA();
            RAWaveResourceAdapter ra = new RAWaveResourceAdapter();
            ra.setConnectionURL(getConnectionUrl());
            x.setResourceAdapter(ra);
            x.setProducerPooling(Boolean.toString(producerPooling));
            ConnectionFactory f = (ConnectionFactory) x.createConnectionFactory();
            ctx.rebind(appjndiUnified, f);
        }
    }

    private String getConnectionUrl() {
        return ((WaveProvider) getJMSProvider()).getConnectionUrl(mServerProperties);
    }

    public XAQueueConnectionFactory getXAQueueConnectionFactory() throws JMSException {
        WaveProfile p = createWaveProfile(getConnectionUrl());
        return new WaveXAQueueConnectionFactory(p);
    }

    protected void setClientID(Connection con) throws JMSException {
        con.setClientID("X");
    }

    public WireCount getConnectionCount() {
        return new WireCount() {
            public void check(int sessions, int producers, int consumers) {
            }

            public void check(int n) {
            }
        };
    }

    /**
     * Purpose: Should be able to create multiple connections with the 
     * same clientID, but only one can be active at the same time. Since the 
     * connections are pooled, the same connection should be reused.
     *
     * @throws Throwable on failure of the test
     */
    public void testClientID() throws Throwable {
        init(true, true);

        InitialContext ctx = getContext();
        TopicConnectionFactory f = (TopicConnectionFactory) ctx.lookup(appjndiTopic);
        
        TopicConnection conn1 = f.createTopicConnection(USERID, PASSWORD);
        conn1.setClientID("x-clientid");
        conn1.createTopicSession(true, 0);
        conn1.close();

        TopicConnection conn2 = f.createTopicConnection(USERID, PASSWORD);
        conn2.setClientID("x-clientid");
        conn2.createTopicSession(true, 0);
        conn2.close();
        
        getConnectionManager(f).clear();
    }

    /**
     * Ensure that createConnection() doesn't end up in createConnection(null, null) if 
     * BypassRA is specified
     */
    public void testBypassRA() throws Throwable {
        InitialContext ctx = getContext();
        QueueConnectionFactory f = (QueueConnectionFactory) ctx.lookup(appjndiQueue);
        
//        System.setSecurityManager(new java.rmi.RMISecurityManager() {
//            public void checkPropertyAccess(String key) {
//            }
//
//            public void checkAccess(ThreadGroup t) {
//                super.checkAccess(t);
//                checkPermission(new java.lang.RuntimePermission("modifyThreadGroup"));
//            }
//             
//             public void checkPackageAccess(final String pkgname) {
//            // Remove this once 1.2.2 SecurityManager/ClassLoader bug is fixed.
//            if(!pkgname.startsWith("sun."))
//                super.checkPackageAccess(pkgname);
//             }
//
//             public void checkExit(int status) {
//                 // Verify exit permission
//                 super.checkExit(status);
//             }
//            
//        });

        // FOR THIS TEST, COMPLETELY BYPASS THE RA
//        getRA(f).setOptions(Options.Out.BYPASSRA + "=true\r\n" + Options.NOXA+ "=true");
        getRA(f).setOptions(Options.NOXA + "=true\r\n" + Options.Out.IGNORETX + "=false");

        try {
            String name = getQueue1Name();
            {
                QueueConnection conn = f.createQueueConnection();
                QueueSession s = conn.createQueueSession(false, Session.AUTO_ACKNOWLEDGE);
                Queue q = s.createQueue(name);
                s.createSender(q).send(s.createTextMessage());
                conn.close();
            }
            
            {
                QueueConnection conn = f.createQueueConnection(USERID, PASSWORD);
                QueueSession s = conn.createQueueSession(false, Session.AUTO_ACKNOWLEDGE);
                Queue q = s.createQueue(name);
                s.createSender(q).send(s.createTextMessage());
                conn.close();
            }
            
            {
                QueueConnection conn = f.createQueueConnection(null, null);
                QueueSession s = conn.createQueueSession(false, Session.AUTO_ACKNOWLEDGE);
                Queue q = s.createQueue(name);
                s.createSender(q).send(s.createTextMessage());
                conn.close();
            }

            clearQueue(name, 3);

            {
                QueueConnection conn = f.createQueueConnection(null, null);
                QueueSession s = conn.createQueueSession(true, Session.SESSION_TRANSACTED);
                Queue q = s.createQueue(name);
                s.createSender(q).send(s.createTextMessage());
                s.commit();
                conn.close();
            }

            {
                QueueConnection conn = f.createQueueConnection(null, null);
                QueueSession s = conn.createQueueSession(true, Session.SESSION_TRANSACTED);
                Queue q = s.createQueue(name);
                s.createSender(q).send(s.createTextMessage());
                s.rollback();
                conn.close();
            }
            clearQueue(name, 1);

        } finally {
            getConnectionManager(f).clear();
        }
    }
    
    // Disable this test. Exception is no longer thrown because closing conn1 
    // will now close the JMSGrid connection.
    // Previously, it would not be closed but cleaned up and held in a pool.  
    
    /**
     * Purpose: Should be able to create multiple connections with the 
     * same clientID, but only one can be active at the same time. 
     * THEY HAVE TO BE OF THE SAME TYPE 
     *
     * @throws Throwable on failure of the test
     */
    public void xxxtestClientIDFail() throws Throwable {
        init(true, true);
        
        InitialContext ctx = getContext();
        TopicConnectionFactory f = (TopicConnectionFactory) ctx.lookup(appjndiTopic);
        
        try {
            Connection conn1 = f.createConnection(USERID, PASSWORD);
            conn1.setClientID("x-clientid");
            conn1.createSession(true, 0);
            conn1.close();
            
            TopicConnection conn2 = f.createTopicConnection(USERID, PASSWORD);
            conn2.setClientID("x-clientid");
            conn2.createTopicSession(true, 0);
            conn2.close();
            
            throw new Throwable("Didn't throw");
        } catch (JMSException e) {
            // Expected
        } finally {
            getConnectionManager(f).clear();
        }
    }
    
    public void testURLOverride() throws Throwable {
        String url = "tcp://server1:1888/?imqaaa=33&imqaaa=bbb&username=a&password=b,tcp://server2:9999/?mqp1=1&mqp2=2&username=c&password=d";
        WaveUrlParser u = new WaveUrlParser(url);
        UrlParser[] urls = u.getUrlParsers();
        assertTrue(urls.length == 2);
        assertTrue(urls[0].getHost().equals("server1"));
        assertTrue(urls[1].getHost().equals("server2"));
        assertTrue(urls[0].getPort() == 1888);
        assertTrue(urls[1].getPort() == 9999);
        System.out.println(u.toString());
        assertTrue(url.equals(u.toString()));
    }

    public void testUrlProperties() throws Throwable {
        RAWaveObjectFactory fact = new RAWaveObjectFactory();
        Properties p = new Properties();
        RAJMSResourceAdapter ra = new RAWaveResourceAdapter();
        fact.getProperties(p, ra, null, null, "tcp://server1:1888/?imqaaa=33&imqaaa=bbb&username=a&password=b,tcp://server2:9999/?mqp1=1&mqp2=2&username=c&password=d");        
        p.getProperty("username").equals("c");
        p.getProperty("password").equals("d");
        p.getProperty("mqp1").equals("1");
        p.getProperty("imqaaa").equals("33");
    }

    /**
     * Grid does not implement batch size in CC
     */
    public void disabled_testXACCBatch() throws Throwable {
        // JMS-Grid only loads one message to a serversession.
        // JMS-Grid connection consumer does not support batch messages
    }
    
    /**
     * To reproduce a concurrency issue...
     * 
     * @throws Throwable
     */
    public void x_testUnifiedStop() throws Throwable {
        WaveProfile p = createWaveProfile(getConnectionUrl());
        WaveXAConnectionFactory cf = new WaveXAConnectionFactory(p);
        for (int j = 0; j < 1000; j++) {
            System.out.println("X");
            Connection c = cf.createConnection(USERID, PASSWORD);
            c.start();
            final Exception[] exc = new Exception[1];

            final int N = 32;
            final Semaphore sem = new Semaphore(N);
            for (int i = 0; i < N; i++) {
                final Session s = c.createSession(true, Session.SESSION_TRANSACTED);
                MessageConsumer mc = s.createConsumer(s.createQueue("Queue1"));
                final MessageProducer prod = s.createProducer(s.createQueue("Queue2"));
                mc.setMessageListener(new MessageListener() {
                    public void onMessage(Message arg0) {
                        try {
                            sem.acquire();
                            prod.send(arg0);
                            s.rollback();
                        } catch (Exception e) {
                            e.printStackTrace();
                            exc[0] = e;
                        } finally {
                            sem.release();
                        }
                    }
                });
                s.createProducer(s.createQueue("Queue1")).send(s.createTextMessage("hi"));
                s.commit();
            }

            Thread.sleep(1000);
            c.stop();
            for (int i = 0; i < N; i++) {
                sem.acquire();
            }
            c.close();
        }         
    }
    
    public void disabled_test160tMixInsideACC() {}
    // public void disabled_test160tMixTxMgr() {}
    // public void test500t() {}
    // public void test800t1() {}
}
