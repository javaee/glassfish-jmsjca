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

package com.stc.jmsjca.test.jndi;

import com.stc.jms.client.STCJMS;
import com.stc.jms.client.STCQueue;
import com.stc.jms.client.STCXAConnectionFactory;
import com.stc.jms.client.STCXAQueueConnectionFactory;
import com.stc.jms.client.STCXATopicConnectionFactory;
import com.stc.jmsjca.core.XMCFQueueXA;
import com.stc.jmsjca.core.XMCFTopicXA;
import com.stc.jmsjca.core.XMCFUnifiedXA;
import com.stc.jmsjca.core.XManagedConnectionFactory;
import com.stc.jmsjca.jndi.RAJNDIResourceAdapter;
import com.stc.jmsjca.test.core.XTestBase;

import javax.jms.ConnectionFactory;
import javax.jms.JMSException;
import javax.jms.Queue;
import javax.jms.QueueConnection;
import javax.jms.QueueConnectionFactory;
import javax.jms.QueueSession;
import javax.jms.Session;
import javax.jms.TopicConnectionFactory;
import javax.jms.XAQueueConnectionFactory;
import javax.naming.Context;
import javax.naming.InitialContext;

import java.io.File;
import java.util.Properties;

/**
 * Unit tests
 * See Base
 *
 *
 * @author Frank Kieviet
 * @version 1.0
 */
public class TestJUStd extends XTestBase {
//    private static Logger sLog = Logger.getLogger(TestJUStd.class);

    /**
     * Constructor
     */
    public TestJUStd() {
        this(null);
    }

    /**
     * Constructor
     *
     * @param name junit test name
     */
    public TestJUStd(String name) {
        super(name);
    }

    public WireCount getConnectionCount() {
        return new WireCount() {
            private int s0 = getWireCount();;

            public void check(int sessions, int producers, int consumers) {
                int n = sessions + producers + consumers;
                int now = getWireCount();
                if (s0 + n != now) {
                    throw new RuntimeException("Assertion failure: invalid wire count "
                        + "s0 + exp != now; now=" + now + "; exp=" + n + "; s0=" + s0
                        + " // expected: " + n + "; found: " + (now - s0));
                }
            }

            public void check(int n) {
                int now = getWireCount();
                if (s0 + n != now) {
                    throw new RuntimeException("Assertion failure: invalid wire count "
                        + "s0 + exp != now; now=" + now + "; exp=" + n + "; s0=" + s0
                        + " // expected: " + n + "; found: " + (now - s0));
                }
            }
        };
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
     * Gets connection properties including protocol to the default test server.
     *
     * @return properties
     */
    protected Properties getConnectionProperties() {
        Properties ret = new Properties();

        ret.put(STCJMS.ProtocolMgr.Host, mServerProperties.
            getProperty("host"));
        ret.put(STCJMS.ProtocolMgr.Port, mServerProperties.getProperty(
            "stcms.instance.port"));

        ret.setProperty(STCJMS.SessionOptions.autoCommitXA, "true");

        return ret;
    }

    public void init(boolean producerPooling) throws Throwable {
        InitialContext ctx = getContext();

        // Use shared RA (this will be cloned for each MCF)
        RAJNDIResourceAdapter ra = new RAJNDIResourceAdapter();
        ra.setInitialContextFactory(getProviderClass());
        ra.setProviderUrl(getUrl());
        ra.setQueueConnectionFactoryJndiName(jndinameQueueDelegateXA);
        ra.setTopicConnectionFactoryJndiName(jndinameTopicDelegateXA);
        ra.setUnifiedConnectionFactoryJndiName(jndinameUnifiedDelegateXA);

        // Create concreate connection factories and bind them into JNDI
        {
            // Create a concrete provider factory (will be used by the managed connection factory)
            Properties p = getConnectionProperties();
            ctx.rebind(jndinameQueueDelegateXA, new STCXAQueueConnectionFactory(p));
            ctx.rebind(jndinameTopicDelegateXA, new STCXATopicConnectionFactory(p));
            ctx.rebind(jndinameUnifiedDelegateXA, new STCXAConnectionFactory(p));
            ctx.rebind("Queue1", new STCQueue("Queue1"));
        }

        // Create MCFs, get the CF that will be used by the application and bind that
        // CF into JNDI
        {
            // QUEUE
            XManagedConnectionFactory x = new XMCFQueueXA();
            x.setResourceAdapter(ra);
            x.setProducerPooling(Boolean.toString(producerPooling));
            QueueConnectionFactory f = (QueueConnectionFactory) x.createConnectionFactory();
            ctx.rebind(appjndiQueue, f);
        }


        {
            // TOPIC
            XManagedConnectionFactory x = new XMCFTopicXA();
            x.setResourceAdapter(ra);
            x.setProducerPooling(Boolean.toString(producerPooling));
            TopicConnectionFactory f = (TopicConnectionFactory) x.createConnectionFactory();
            ctx.rebind(appjndiTopic, f);
        }


        {
            // UNIFIED
            XManagedConnectionFactory x = new XMCFUnifiedXA();
            x.setResourceAdapter(ra);
            x.setProducerPooling(Boolean.toString(producerPooling));
            ConnectionFactory f = (ConnectionFactory) x.createConnectionFactory();
            ctx.rebind(appjndiUnified, f);
        }
    }

    /**
     * @see com.stc.jmsjca.test.core.XTestBase#getXAQueueConnectionFactory()
     * @throws JMSException propagatd 
     */
    public XAQueueConnectionFactory getXAQueueConnectionFactory() throws JMSException {
        return new com.stc.jms.client.STCXAQueueConnectionFactory(getConnectionProperties());
    }

    public static String USERID = "stc";
    public static String PASSWORD = "stc";

    private static long sTime = System.currentTimeMillis();
    private static long sUniquifier;

    /**
     * Generates a unique name
     *
     * @return name
     */
    public String generateName() {
        synchronized (TestJUStd.class) {
            return "JMSJCA-" + this.getClass() + sTime + "-" + sUniquifier++;
        }
    }

    /**
     * Returns the number of created wires minus the number of closed wires
     *
     * @return wire count
     */
    protected int getWireCount() {
        return com.stc.jms.sockets.Wire.sObjectCount;
    }

    public void testTemporaryTopicConnectionClosesTestRA() {
        // Invalid test for JNDI
        // Should work for STCMS only
    }

    public void testDummy() {

    }
    
    /**
     * When NoXA is specified, XA should not be used
     *
     * @throws Throwable on failure of the test
     */
    public void testNoXA() throws Throwable {
        init(true, true);
        
        RAJNDIResourceAdapter ra = new RAJNDIResourceAdapter();
        ra.setConnectionURL("jndi://");
        String host = mServerProperties.getProperty("host");
        String port = mServerProperties.getProperty("stcms.instance.port");
        
        
        ra.setOptions(
                "java.naming.factory.initial=com.stc.jms.jndispi.InitialContextFactory\r\n" + 
                "java.naming.provider.url=stcms://" + host + ":" + port + "\r\n" + 
                "java.naming.security.principal=Administrator\r\n" + 
                "java.naming.security.credentials=STC\r\n" + 
                "JMSJCA.TopicCF=connectionfactories/topicconnectionfactory\r\n" + 
                "JMSJCA.QueueCF=connectionfactories/queueconnectionfactory\r\n" + 
                "JMSJCA.UnifiedCF=connectionfactories/connectionfactory\r\n" + 
                "JMSJCA.NoXA=true");
        
        XMCFQueueXA mcf = new XMCFQueueXA();
        mcf.setResourceAdapter(ra);
        QueueConnectionFactory fact = (QueueConnectionFactory) mcf.createConnectionFactory();
        assertTrue(fact != null);
        
        QueueConnection c = fact.createQueueConnection();
        QueueSession s = c.createQueueSession(false, Session.AUTO_ACKNOWLEDGE);
        s.close();
        c.close();
        getConnectionManager(fact).clear();
    }
    
    /**
     * Properties should be able to be put on a single line
     *
     * @throws Throwable on failure of the test
     */
    public void testSingleLine() throws Throwable {
        init(true, true);
        
        RAJNDIResourceAdapter ra = new RAJNDIResourceAdapter();
        ra.setConnectionURL("jndi://");
        String host = mServerProperties.getProperty("host");
        String port = mServerProperties.getProperty("stcms.instance.port");
        
        String sep = "!";
        
        ra.setOptions(
                "JMSJCA.sep=!" + 
                "java.naming.factory.initial=com.stc.jms.jndispi.InitialContextFactory" + sep + 
                "java.naming.provider.url=stcms://" + host + ":" + port + "/" +  sep +
                "java.naming.security.principal=Administrator" +  sep +
                "java.naming.security.credentials=STC" +  sep +
                "JMSJCA.TopicCF=connectionfactories/topicconnectionfactory" + sep + 
                "JMSJCA.QueueCF=connectionfactories/queueconnectionfactory" + sep +
                "JMSJCA.UnifiedCF=connectionfactories/connectionfactory" + sep +
                "JMSJCA.NoXA=true");
        
        XMCFQueueXA mcf = new XMCFQueueXA();
        mcf.setResourceAdapter(ra);
        QueueConnectionFactory fact = (QueueConnectionFactory) mcf.createConnectionFactory();
        assertTrue(fact != null);
        
        QueueConnection c = fact.createQueueConnection();
        QueueSession s = c.createQueueSession(false, Session.AUTO_ACKNOWLEDGE);
        s.close();
        c.close();
        getConnectionManager(fact).clear();
    }

    public void testCreateQueue() throws Throwable {
        init(true, true);
        
        RAJNDIResourceAdapter ra = new RAJNDIResourceAdapter();
        ra.setConnectionURL("jndi://");
        String host = mServerProperties.getProperty("host");
        String port = mServerProperties.getProperty("stcms.instance.port");
        
        
        ra.setOptions(
                "java.naming.factory.initial=com.stc.jms.jndispi.InitialContextFactory\r\n" + 
                "java.naming.provider.url=stcms://" + host + ":" + port + "\r\n" + 
                "java.naming.security.principal=Administrator\r\n" + 
                "java.naming.security.credentials=STC\r\n" +
                "com.stc.jms.jndispi.disconnected=true\r\n" +
                "JMSJCA.TopicCF=connectionfactories/xatopicconnectionfactory\r\n" + 
                "JMSJCA.QueueCF=connectionfactories/xaqueueconnectionfactory\r\n" + 
                "JMSJCA.UnifiedCF=connectionfactories/xaconnectionfactory");
        
        XMCFQueueXA mcf = new XMCFQueueXA();
        mcf.setResourceAdapter(ra);
        QueueConnectionFactory fact = (QueueConnectionFactory) mcf.createConnectionFactory();
        assertTrue(fact != null);
        
        QueueConnection c = fact.createQueueConnection();
        QueueSession s = c.createQueueSession(false, Session.AUTO_ACKNOWLEDGE);
        
        // Queue creation
        Queue q = s.createQueue("jndi://queues/Queue1");
        assertTrue(q instanceof STCQueue);
        assertTrue(q.getQueueName().equals("Queue1"));

        // Queue caching
        Queue q2 = s.createQueue("jndi://queues/Queue1");
        assertTrue(q instanceof STCQueue);
        assertTrue(q.getQueueName().equals("Queue1"));
        assertTrue(q2 == q);
        
        // Normal creation
        q = s.createQueue("Queue1");
        assertTrue(q instanceof STCQueue);
        assertTrue(q.getQueueName().equals("Queue1"));
        
        // Global lookup
        try {
            // Should fail: does not exist
            q = s.createQueue("lookup://Queue1");
            throw new Throwable("Did not throw");
        } catch (Exception e) {
            // Expected
        }
        
        s.close();
        c.close();
        getConnectionManager(fact).clear();
    }
}
