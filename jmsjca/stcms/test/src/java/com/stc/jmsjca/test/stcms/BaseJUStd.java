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
 * $RCSfile: BaseJUStd.java,v $
 * $Revision: 1.3 $
 * $Date: 2007-01-21 17:52:12 $
 *
 * Copyright 2003-2007 Sun Microsystems, Inc. All Rights Reserved.  
 */

package com.stc.jmsjca.test.stcms;

import com.stc.jmsjca.core.JConnectionFactory;
import com.stc.jmsjca.core.Options;
import com.stc.jmsjca.core.RAJMSActivationSpec;
import com.stc.jmsjca.core.RAJMSObjectFactory;
import com.stc.jmsjca.core.RAJMSResourceAdapter;
import com.stc.jmsjca.core.TxMgr;
import com.stc.jmsjca.core.WMessageIn;
import com.stc.jmsjca.core.WQueueSession;
import com.stc.jmsjca.core.XConnectionRequestInfo;
import com.stc.jmsjca.core.XDefaultConnectionManager;
import com.stc.jmsjca.core.XMCFQueueXA;
import com.stc.jmsjca.core.XMCFTopicXA;
import com.stc.jmsjca.core.XManagedConnection;
import com.stc.jmsjca.core.XManagedConnectionFactory;
import com.stc.jmsjca.core.XXid;
import com.stc.jmsjca.jcacontainer.MDBFactory;
import com.stc.jmsjca.jcacontainer.XBootstrapContext;
import com.stc.jmsjca.jcacontainer.XMessageEndpointFactory;
import com.stc.jmsjca.jcacontainer.XWorkManager;
import com.stc.jmsjca.stcms.RASTCMSActivationSpec;
import com.stc.jmsjca.stcms.RASTCMSObjectFactory;
import com.stc.jmsjca.stcms.RASTCMSResourceAdapter;
import com.stc.jmsjca.test.core.Passthrough;
import com.stc.jmsjca.test.core.XTestBase;
import com.stc.jmsjca.util.Str;

import javax.jms.BytesMessage;
import javax.jms.Connection;
import javax.jms.ConnectionConsumer;
import javax.jms.ConnectionFactory;
import javax.jms.ConnectionMetaData;
import javax.jms.DeliveryMode;
import javax.jms.Destination;
import javax.jms.ExceptionListener;
import javax.jms.JMSException;
import javax.jms.MapMessage;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageListener;
import javax.jms.MessageProducer;
import javax.jms.ObjectMessage;
import javax.jms.Queue;
import javax.jms.QueueBrowser;
import javax.jms.QueueConnection;
import javax.jms.QueueConnectionFactory;
import javax.jms.QueueReceiver;
import javax.jms.QueueSender;
import javax.jms.QueueSession;
import javax.jms.ServerSessionPool;
import javax.jms.Session;
import javax.jms.StreamMessage;
import javax.jms.TemporaryQueue;
import javax.jms.TemporaryTopic;
import javax.jms.TextMessage;
import javax.jms.Topic;
import javax.jms.TopicConnectionFactory;
import javax.jms.TopicSubscriber;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.resource.ResourceException;
import javax.resource.spi.ActivationSpec;
import javax.resource.spi.BootstrapContext;
import javax.resource.spi.ConnectionManager;
import javax.resource.spi.endpoint.MessageEndpointFactory;
import javax.resource.spi.security.PasswordCredential;
import javax.resource.spi.work.WorkManager;
import javax.security.auth.Subject;
import javax.transaction.HeuristicMixedException;
import javax.transaction.HeuristicRollbackException;
import javax.transaction.InvalidTransactionException;
import javax.transaction.NotSupportedException;
import javax.transaction.RollbackException;
import javax.transaction.Synchronization;
import javax.transaction.SystemException;
import javax.transaction.Transaction;
import javax.transaction.TransactionManager;
import javax.transaction.xa.XAException;
import javax.transaction.xa.XAResource;
import javax.transaction.xa.Xid;

import java.io.File;
import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.Map.Entry;

/**
 *
 * Unit tests not using the J2EE container, specific for STCMS.
 * This inherits from XTestBase so that it will include all generic tests.
 * See Base
 *
 * @author Frank Kieviet
 * @version 1.0
 */
public class BaseJUStd extends XTestBase {
//    private static Logger sLog = Logger.getLogger(BaseJUStd.class);

    /**
     * Constructor
     */
    public BaseJUStd() {
        this(null);
    }
    
    public void tearDown() throws Exception {
        TxMgr.setUnitTestTxMgr(null);
        super.tearDown();
    }

    /**
     * Constructor
     *
     * @param name junit test name
     */
    public BaseJUStd(String name) {
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

    /**
     * Gets connection URL to the default test server.
     *
     * @return URL
     */
    protected String getConnectionURL() {
        System.setProperty(RASTCMSObjectFactory.PORTPROP,
            mServerProperties.getProperty("stcms.instance.port"));
        System.setProperty(RASTCMSObjectFactory.PORTSSLPROP,
                mServerProperties.getProperty("stcms.instance.ssl.port"));
        return "stcms://" + mServerProperties.getProperty("host");
    }

    protected String xxgetConnectionUrl() {
        return xxcreateConnectionUrl(mServerProperties.getProperty("host"), Integer
            .parseInt(mServerProperties.getProperty("stcms.instance.port")));
    }

    public String xxcreateConnectionUrl(String server, int port) {
        return "stcms://" + server + ":" + port;
    }
    
    /**
     * Gets connection URL for the alternative server connection
     *
     * @return URL
     */
    protected String getConnectionURL2() {
        return "stcmss://" + mServerProperties.getProperty("host");
    }

    public void init(boolean producerPooling) throws Throwable {
        InitialContext ctx = getContext();

        // QUEUE
        {
            // Managed factory; this is normally done by the Application Server
            XManagedConnectionFactory x = new XMCFQueueXA();
            RASTCMSResourceAdapter ra = new RASTCMSResourceAdapter();
            x.setResourceAdapter(ra);
            ra.setConnectionURL(getConnectionURL());
            x.setProducerPooling(Boolean.toString(producerPooling));

            // Factory to be used by the application (done by the Application server)
            QueueConnectionFactory f = (QueueConnectionFactory) x.createConnectionFactory();
            ctx.rebind(appjndiQueue, f);
        }

        // TOPIC
        {
            // Managed factory; this is normally done by the Application Server
            XManagedConnectionFactory x = new XMCFTopicXA();
            RASTCMSResourceAdapter ra = new RASTCMSResourceAdapter();
            x.setResourceAdapter(ra);
            ra.setConnectionURL(getConnectionURL());
            x.setProducerPooling(Boolean.toString(producerPooling));

            // Factory to be used by the application (done by the Application server)
            TopicConnectionFactory f = (TopicConnectionFactory) x.createConnectionFactory();
            ctx.rebind(appjndiTopic, f);
        }
    }

    private static long sTime = System.currentTimeMillis();
    private static long sUniquifier;

    /**
     * Generates a unique name
     *
     * @return name
     */
    public String generateName() {
        synchronized (BaseJUStd.class) {
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

    /**
     * Purpose: Test connection creation with additional session creation<br>
     * Assertion: <br>
     * Strategy: <br>
     *
     * @throws Throwable on failure of the test
     */
    public void test110qEmbeddedUrl() throws Throwable {
        init(true, true);

        // This is how the client would normally create connections
        InitialContext ctx = getContext();
        QueueConnectionFactory f = (QueueConnectionFactory) ctx.lookup(appjndiQueue);
        QueueConnection conn = f.createQueueConnection(USERID, PASSWORD);

        int s0 = getWireCount();

        QueueSession s = conn.createQueueSession(true, 0);
        s.getTransacted(); // actuate type

        assertTrue(getWireCount() == s0 + 1);

        // Connection shouldn't close
        conn.close();
        assertTrue(getWireCount() == s0 + 1);

        // Reuse this connection
        conn = f.createQueueConnection(USERID, PASSWORD);
        s = conn.createQueueSession(true, 0);
        s.getTransacted(); // actuate type
        assertTrue(getWireCount() == s0 + 1);
        conn.close();

        String uid = getConnectionURL2() + "?username=" + USERID;

        // Reauthenticate a connection (should create a new connection)
        {
            conn = f.createQueueConnection(uid, PASSWORD);

            s = conn.createQueueSession(true, 0);
            s.getTransacted(); // actuate type

            assertTrue(getWireCount() == s0 + 2);
            conn.close();
            assertTrue(getWireCount() == s0 + 2);
        }

        // Reuse this same connection
        {
            conn = f.createQueueConnection(uid, PASSWORD);

            s = conn.createQueueSession(true, 0);
            s.getTransacted(); // actuate type

            assertTrue(getWireCount() == s0 + 2);
            conn.close();
            assertTrue(getWireCount() == s0 + 2);
        }

        uid = getConnectionURL2() + "?username=" + USERID + "&password=" + PASSWORD;

        // Reauthenticate a connection (should create a new connection)
        {
            conn = f.createQueueConnection(uid, null);

            s = conn.createQueueSession(true, 0);
            s.getTransacted(); // actuate type

            assertTrue(getWireCount() == s0 + 3);
            conn.close();
            assertTrue(getWireCount() == s0 + 3);
        }

        // Reuse this same connection
        {
            conn = f.createQueueConnection(uid, null);

            s = conn.createQueueSession(true, 0);
            s.getTransacted(); // actuate type

            assertTrue(getWireCount() == s0 + 3);
            conn.close();
            assertTrue(getWireCount() == s0 + 3);
        }

        getConnectionManager(f).clear();
        assertTrue(getWireCount() == s0 + 0);
    }

    public void testDummy() {

    }

    /**
     * Purpose:
     * Assertion:
     * Strategy: <br>
     *
     * @throws Throwable on failure of the test
     */
    public void testRecover1() throws Throwable {
        init(true, true);

        int s0 = getWireCount();

        // This is how the client would normally create connections
        InitialContext ctx = getContext();
        QueueConnectionFactory f = (QueueConnectionFactory) ctx.lookup(appjndiQueue);

        // Spec1
        RASTCMSActivationSpec spec1 = new RASTCMSActivationSpec();
        spec1.setConnectionURL(getConnectionURL());
        spec1.setUserName(mServerProperties.getProperty("admin.user"));
        spec1.setPassword(mServerProperties.getProperty("admin.password"));

        // Spec2 is identical
        RASTCMSActivationSpec spec2 = new RASTCMSActivationSpec();
        spec2.setConnectionURL(getConnectionURL());
        spec2.setUserName(mServerProperties.getProperty("admin.user"));
        spec2.setPassword(mServerProperties.getProperty("admin.password"));

        // Spec3 uses different URL
        RASTCMSActivationSpec spec3 = new RASTCMSActivationSpec();
        spec3.setConnectionURL(getConnectionURL2());
        spec3.setUserName(mServerProperties.getProperty("admin.user"));
        spec3.setPassword(mServerProperties.getProperty("admin.password"));

        ActivationSpec[] specs = new ActivationSpec[] {
            spec1,
            spec2,
            spec3,
        };

        // Get xas
        com.stc.jmsjca.core.RAJMSResourceAdapter ra = getRA(f);
        XAResource[] xas = ra.getXAResources(specs);

        // Should be 2, and two wires
        assertTrue(xas.length == 2);
        assertTrue(getWireCount() == s0 + 2);

        // Stop should close connections
        ra.stop();

        assertTrue(getWireCount() == s0);
    }
    
    // Principal mapping assertions to test:
    // 1) specified subject overrides null-userid/password in createConnection(uid, pw)
    // 2) specified subject is used in matching
    // 3) specified userid/password in createConnection(uid, pw) overrides MCF
    // 4) specified userid/password are used in matching
    // 5) specified subject overrides userid/password in ra
    // 6) specified subject is used in matching
    
    /**
     * A test queue connection factory to test userid and passsword propagation
     */
    public static class QueueCF implements QueueConnectionFactory {

        public QueueConnection createQueueConnection() throws JMSException {
            return createQueueConnection(null, null);
        }

        public QueueConnection createQueueConnection(String arg0, String arg1) throws JMSException {
            return new QueueCon(arg0, arg1);
        }

        public Connection createConnection() throws JMSException {
            return createQueueConnection(null, null);
        }

        public Connection createConnection(String arg0, String arg1) throws JMSException {
            return createQueueConnection(arg0, arg1);
        }
        
    }
    
    /**
     * Test queue connection to test userid and passsword propagation
     */
    public static class QueueCon implements QueueConnection {
        private String mUid;
        private String mPass;
        
        public QueueCon(String uid, String pw) {
            mUid = uid;
            mPass = pw;
        }

        public QueueSession createQueueSession(boolean arg0, int arg1) throws JMSException {
            return new QueueSess(mUid, mPass);
        }

        public ConnectionConsumer createConnectionConsumer(Queue arg0, String arg1, ServerSessionPool arg2, int arg3) throws JMSException {
            return null;
        }

        public Session createSession(boolean arg0, int arg1) throws JMSException {
            return new QueueSess(mUid, mPass);
        }

        public String getClientID() throws JMSException {
            return null;
        }

        public void setClientID(String arg0) throws JMSException {
        }

        public ConnectionMetaData getMetaData() throws JMSException {
            return null;
        }

        public ExceptionListener getExceptionListener() throws JMSException {
            return null;
        }

        public void setExceptionListener(ExceptionListener arg0) throws JMSException {
        }

        public void start() throws JMSException {
        }

        public void stop() throws JMSException {
        }

        public void close() throws JMSException {
        }

        public ConnectionConsumer createConnectionConsumer(Destination arg0, String arg1, ServerSessionPool arg2, int arg3) throws JMSException {
            return null;
        }

        public ConnectionConsumer createDurableConnectionConsumer(Topic arg0, String arg1, String arg2, ServerSessionPool arg3, int arg4) throws JMSException {
            return null;
        }
        
    }
    
    /**
     * Test queue session to test userid and passsword propagation
     */
    public static class QueueSess implements QueueSession {
        private String mUid;
        private String mPass;
        
        public QueueSess(String uid, String pw) {
            mUid = uid;
            mPass = pw;
        }
        
        public String getUserid() {
            return mUid;
        }
        
        public String getPassword() {
            return mPass;
        }

        public Queue createQueue(String arg0) throws JMSException {
            return null;
        }

        public QueueReceiver createReceiver(Queue arg0) throws JMSException {
            return null;
        }

        public QueueReceiver createReceiver(Queue arg0, String arg1) throws JMSException {
            return null;
        }

        public QueueSender createSender(Queue arg0) throws JMSException {
            return null;
        }

        public QueueBrowser createBrowser(Queue arg0) throws JMSException {
            return null;
        }

        public QueueBrowser createBrowser(Queue arg0, String arg1) throws JMSException {
            return null;
        }

        public TemporaryQueue createTemporaryQueue() throws JMSException {
            return null;
        }

        public BytesMessage createBytesMessage() throws JMSException {
            return null;
        }

        public MapMessage createMapMessage() throws JMSException {
            return null;
        }

        public Message createMessage() throws JMSException {
            return null;
        }

        public ObjectMessage createObjectMessage() throws JMSException {
            return null;
        }

        public ObjectMessage createObjectMessage(Serializable arg0) throws JMSException {
            return null;
        }

        public StreamMessage createStreamMessage() throws JMSException {
            return null;
        }

        public TextMessage createTextMessage() throws JMSException {
            return null;
        }

        public TextMessage createTextMessage(String arg0) throws JMSException {
            return null;
        }

        public boolean getTransacted() throws JMSException {
            return true;
        }

        public int getAcknowledgeMode() throws JMSException {
            return 0;
        }

        public void commit() throws JMSException {
        }

        public void rollback() throws JMSException {
        }

        public void close() throws JMSException {
        }

        public void recover() throws JMSException {
        }

        public MessageListener getMessageListener() throws JMSException {
            return null;
        }

        public void setMessageListener(MessageListener arg0) throws JMSException {
        }

        public void run() {
        }

        public MessageProducer createProducer(Destination arg0) throws JMSException {
            return null;
        }

        public MessageConsumer createConsumer(Destination arg0) throws JMSException {
            return null;
        }

        public MessageConsumer createConsumer(Destination arg0, String arg1) throws JMSException {
            return null;
        }

        public MessageConsumer createConsumer(Destination arg0, String arg1, boolean arg2) throws JMSException {
            return null;
        }

        public Topic createTopic(String arg0) throws JMSException {
            return null;
        }

        public TopicSubscriber createDurableSubscriber(Topic arg0, String arg1) throws JMSException {
            return null;
        }

        public TopicSubscriber createDurableSubscriber(Topic arg0, String arg1, String arg2, boolean arg3) throws JMSException {
            return null;
        }

        public TemporaryTopic createTemporaryTopic() throws JMSException {
            return null;
        }

        public void unsubscribe(String arg0) throws JMSException {
        }
    }

    /**
     * Test object factory to test userid and passsword propagation
     */
    public static class TestObjectFactory extends RAJMSObjectFactory implements
        java.io.Serializable {

        public ConnectionFactory createConnectionFactory(int domain,
            RAJMSResourceAdapter resourceAdapter, RAJMSActivationSpec activationSpec,
            XManagedConnectionFactory fact, String overrideUrl) throws JMSException {

            return new QueueCF();
        }

        public boolean isUrl(String url) {
            return false;
        }

        public String getJMSServerType() {
            return "Test";
        }
    }
    
    /**
     * Tests userid and password propagation in createConnection() and proper matching
     * of connections taking userid and password into account.
     * 
     * @throws Throwable on test failure
     */
    public void testPrincipalMappingBase() throws Throwable {
        RASTCMSResourceAdapter ra = new RASTCMSResourceAdapter(new TestObjectFactory());
        ra.setOptions(Options.NOXA + "=true");
        ra.setUserName("rauid");
        ra.setPassword("rapw");
        XManagedConnectionFactory mcf = new XMCFQueueXA();
        mcf.setResourceAdapter(ra);

        QueueConnectionFactory fact = (QueueConnectionFactory) mcf.createConnectionFactory();
        QueueSess qs3;
        
        // Create
        {
            QueueConnection con = fact.createQueueConnection("uid1", "pw1");
            QueueSession qs1 = con.createQueueSession(false, Session.AUTO_ACKNOWLEDGE);
            WQueueSession qs2 = (WQueueSession) qs1;
            qs3 = (QueueSess) qs2.getJSession().getDelegate();
            assertSame("uid1", qs3.getUserid());
            assertSame("pw1", qs3.getPassword());
            con.close();
        }
        
        // Should reuse
        {
            QueueConnection con = fact.createQueueConnection("uid1", "pw1");
            QueueSession qs1 = con.createQueueSession(false, Session.AUTO_ACKNOWLEDGE);
            WQueueSession qs2 = (WQueueSession) qs1;
            QueueSess qs3a = (QueueSess) qs2.getJSession().getDelegate();
            assertTrue(qs3 == qs3a);
            con.close();
        }
        
        // Should not reuse
        {
            QueueConnection con = fact.createQueueConnection("uid1", "pw2");
            QueueSession qs1 = con.createQueueSession(false, Session.AUTO_ACKNOWLEDGE);
            WQueueSession qs2 = (WQueueSession) qs1;
            QueueSess qs3b = (QueueSess) qs2.getJSession().getDelegate();
            assertTrue(qs3 != qs3b);
            con.close();
        }
    }

    /**
     * Tests userid and password propagation in createConnection() and proper matching
     * of connections taking userid and password into account.
     * 
     * Uses no-arg variety of createConnection()
     * 
     * @throws Throwable on test failure
     */
    public void testPrincipalMappingBase1() throws Throwable {
        RASTCMSResourceAdapter ra = new RASTCMSResourceAdapter(new TestObjectFactory());
        ra.setUserName("rauid");
        ra.setPassword("rapw");
        ra.setOptions(Options.NOXA + "=true");
        XManagedConnectionFactory mcf = new XMCFQueueXA();
        mcf.setResourceAdapter(ra);

        QueueConnectionFactory fact = (QueueConnectionFactory) mcf.createConnectionFactory();
        QueueSess qs3;
        
        // Create
        {
            QueueConnection con = fact.createQueueConnection();
            QueueSession qs1 = con.createQueueSession(false, Session.AUTO_ACKNOWLEDGE);
            WQueueSession qs2 = (WQueueSession) qs1;
            qs3 = (QueueSess) qs2.getJSession().getDelegate();
            assertSame("rauid", qs3.getUserid());
            assertSame("rapw", qs3.getPassword());
            con.close();
        }
        
        // Should reuse
        {
            QueueConnection con = fact.createQueueConnection();
            QueueSession qs1 = con.createQueueSession(false, Session.AUTO_ACKNOWLEDGE);
            WQueueSession qs2 = (WQueueSession) qs1;
            QueueSess qs3a = (QueueSess) qs2.getJSession().getDelegate();
            assertTrue(qs3 == qs3a);
            con.close();
        }
        
        // Should not reuse
        {
            QueueConnection con = fact.createQueueConnection("uid1", "pw2");
            QueueSession qs1 = con.createQueueSession(false, Session.AUTO_ACKNOWLEDGE);
            WQueueSession qs2 = (WQueueSession) qs1;
            QueueSess qs3b = (QueueSess) qs2.getJSession().getDelegate();
            assertTrue(qs3 != qs3b);
            con.close();
        }
    }

    /**
     * Tests userid and password propagation in createConnection() and proper matching
     * of connections taking userid and password into account.
     * 
     * 1) specified subject overrides null-userid/password in createConnection(uid, pw)
     * 2) specified subject is used in matching
     * 
     * @throws Throwable on test failure
     */
    public void testPrincipalMapping1() throws Throwable {
        RASTCMSResourceAdapter ra = new RASTCMSResourceAdapter(new TestObjectFactory());
        ra.setUserName("rauid");
        ra.setPassword("rapw");
        ra.setOptions(Options.NOXA + "=true");
        XManagedConnectionFactory mcf = new XMCFQueueXA();
        mcf.setResourceAdapter(ra);

        QueueConnectionFactory fact = (QueueConnectionFactory) mcf.createConnectionFactory();
        JConnectionFactory fact1 = (JConnectionFactory) fact;

        PasswordCredential cred = new PasswordCredential("cuid1", "cuidpw1".toCharArray());
        cred.setManagedConnectionFactory(mcf);
        Set creds = new HashSet();
        creds.add(cred);
        Subject s = new Subject(false, new HashSet(), new HashSet(), creds);

        XDefaultConnectionManager cm = (XDefaultConnectionManager) fact1.getConnectionManager();
        cm.setSubject(s);

        QueueSess qs3;
        
        // Create
        {
            QueueConnection con = fact.createQueueConnection();
            QueueSession qs1 = con.createQueueSession(false, Session.AUTO_ACKNOWLEDGE);
            WQueueSession qs2 = (WQueueSession) qs1;
            qs3 = (QueueSess) qs2.getJSession().getDelegate();
            assertEquals("cuid1", qs3.getUserid());
            assertEquals("cuidpw1", qs3.getPassword());
            con.close();
        }
        
        // Should reuse
        {
            QueueConnection con = fact.createQueueConnection();
            QueueSession qs1 = con.createQueueSession(false, Session.AUTO_ACKNOWLEDGE);
            WQueueSession qs2 = (WQueueSession) qs1;
            QueueSess qs3a = (QueueSess) qs2.getJSession().getDelegate();
            assertTrue(qs3 == qs3a);
            con.close();
        }
        
        // Should not reuse
        {
            cm.setSubject(null);
            QueueConnection con = fact.createQueueConnection("uid1", "pw2");
            QueueSession qs1 = con.createQueueSession(false, Session.AUTO_ACKNOWLEDGE);
            WQueueSession qs2 = (WQueueSession) qs1;
            QueueSess qs3b = (QueueSess) qs2.getJSession().getDelegate();
            assertTrue(qs3 != qs3b);
            con.close();
        }
    }

    /**
     * Tests userid and password propagation in createConnection() and proper matching
     * of connections taking userid and password into account.
     * 
     * 1) specified subject overrides null-userid/password in createConnection(uid, pw)
     * 2) specified subject is used in matching
     * 
     * @throws Throwable on test failure
     */
    public void testPrincipalMapping2() throws Throwable {
        RASTCMSResourceAdapter ra = new RASTCMSResourceAdapter(new TestObjectFactory());
        ra.setUserName("rauid");
        ra.setPassword("rapw");
        ra.setOptions(Options.NOXA + "=true");
        XManagedConnectionFactory mcf = new XMCFQueueXA();
        mcf.setResourceAdapter(ra);

        QueueConnectionFactory fact = (QueueConnectionFactory) mcf.createConnectionFactory();
        JConnectionFactory fact1 = (JConnectionFactory) fact;

        PasswordCredential cred = new PasswordCredential("cuid1", "cuidpw1".toCharArray());
        cred.setManagedConnectionFactory(mcf);
        Set creds = new HashSet();
        creds.add(cred);
        Subject s = new Subject(false, new HashSet(), new HashSet(), creds);

        XDefaultConnectionManager cm = (XDefaultConnectionManager) fact1.getConnectionManager();
        cm.setSubject(s);

        QueueSess qs3;
        
        // Create
        {
            QueueConnection con = fact.createQueueConnection();
            QueueSession qs1 = con.createQueueSession(false, Session.AUTO_ACKNOWLEDGE);
            WQueueSession qs2 = (WQueueSession) qs1;
            qs3 = (QueueSess) qs2.getJSession().getDelegate();
            assertEquals("cuid1", qs3.getUserid());
            assertEquals("cuidpw1", qs3.getPassword());
            con.close();
        }
        
        // Should reuse
        {
            QueueConnection con = fact.createQueueConnection();
            QueueSession qs1 = con.createQueueSession(false, Session.AUTO_ACKNOWLEDGE);
            WQueueSession qs2 = (WQueueSession) qs1;
            QueueSess qs3a = (QueueSess) qs2.getJSession().getDelegate();
            assertTrue(qs3 == qs3a);
            con.close();
        }
        
        // Should not reuse
        {
            cm.setSubject(null);
            QueueConnection con = fact.createQueueConnection("uid1", "pw2");
            QueueSession qs1 = con.createQueueSession(false, Session.AUTO_ACKNOWLEDGE);
            WQueueSession qs2 = (WQueueSession) qs1;
            QueueSess qs3b = (QueueSess) qs2.getJSession().getDelegate();
            assertTrue(qs3 != qs3b);
            con.close();
        }

        cred = new PasswordCredential("cuid2", "cuidpw2".toCharArray());
        cred.setManagedConnectionFactory(mcf);
        creds = new HashSet();
        creds.add(cred);
        s = new Subject(false, new HashSet(), new HashSet(), creds);

        // Should not reuse
        {
            cm.setSubject(s);
            QueueConnection con = fact.createQueueConnection("uid1", "pw2");
            QueueSession qs1 = con.createQueueSession(false, Session.AUTO_ACKNOWLEDGE);
            WQueueSession qs2 = (WQueueSession) qs1;
            QueueSess qs3b = (QueueSess) qs2.getJSession().getDelegate();
            assertTrue(qs3 != qs3b);
            con.close();
        }
    }
    
    public QueueConnectionFactory createCMQF(Properties options) throws ResourceException {
        RASTCMSResourceAdapter ra = new RASTCMSResourceAdapter();
        XManagedConnectionFactory mcf = new XMCFQueueXA();
        mcf.setResourceAdapter(ra);
        mcf.setOptions(Str.serializeProperties(options));
        ra.setConnectionURL(getConnectionURL());
        ConnectionManager cm = null; // new XExtendedConnectionManager(mcf);
        QueueConnectionFactory f = (QueueConnectionFactory) mcf.createConnectionFactory(cm);
        return f;
    }
    
    /**
     * Area: Extended connection manager
     * Test: base test case
     * Assertion: CM can be used to send a msg (basic) 
     * 
     * @throws Throwable on failure
     */
    public void testCMBase() throws Throwable {
        // Configure
        Properties options = new Properties();
        options.setProperty(Options.Out.POOL_MAXSIZE, "2");
        
        // Save state
        WireCount w0 = getConnectionCount();
        
        // Create
        QueueConnectionFactory f = createCMQF(options);

        // Do work
        String dest = generateName();
        QueueConnection c = f.createQueueConnection();
        QueueSession s = c.createQueueSession(false, Session.AUTO_ACKNOWLEDGE);
        s.createSender(s.createQueue(dest)).send(s.createTextMessage(dest));
        c.close();
        
        // Check
        clearQueue(dest, 1);
        
        // Check connection states
        w0.check(1, 0, 0);
        getExtConnectionManager(f).clear();
        w0.check(0, 0, 0);
    }

    /**
     * Area: Extended connection manager
     * Test: ra.stop() should clear pool
     * Assertion: All connections are cleared after calling ra.stop() 
     * 
     * @throws Throwable on failure
     */
    public void testCMStop() throws Throwable {
        // Configure
        Properties options = new Properties();
        options.setProperty(Options.Out.POOL_MAXSIZE, "2");
        
        // Save state
        WireCount w0 = getConnectionCount();
        
        // Create
        QueueConnectionFactory f = createCMQF(options);

        // Do work
        String dest = generateName();
        {
            QueueConnection c = f.createQueueConnection();
            QueueSession s = c.createQueueSession(false, Session.AUTO_ACKNOWLEDGE);
            s.createSender(s.createQueue(dest)).send(s.createTextMessage(dest));
            c.close();
        }
        
        // Check
        clearQueue(dest, 1);
        
        // Check connection states
        w0.check(1, 0, 0);
        getRA(f).stop();
        w0.check(0, 0, 0);

        try {
            QueueConnection c = f.createQueueConnection();
            QueueSession s = c.createQueueSession(false, Session.AUTO_ACKNOWLEDGE);
            s.createSender(s.createQueue(dest)).send(s.createTextMessage(dest));
            c.close();
            throw new Throwable("Did not throw");
        } catch (Exception expected) {
            // ok
        }
    }

    /**
     * Area: Extended connection manager
     * Test: blocking behavior
     * Assertion: should throw exception if request cannot be satisfied 
     * 
     * @throws Throwable on failure
     */
    public void testCMBlocking() throws Throwable {
        // Configure
        Properties options = new Properties();
        options.setProperty(Options.Out.POOL_MAXSIZE, "2");
        options.setProperty(Options.Out.POOL_TIMEOUT, "1");
        
        // Save state
        WireCount w0 = getConnectionCount();
        
        // Create
        QueueConnectionFactory f = createCMQF(options);

        // Do work
        String dest = generateName();
        {
            QueueConnection c = f.createQueueConnection();
            QueueSession s = c.createQueueSession(false, Session.AUTO_ACKNOWLEDGE);
            s.createSender(s.createQueue(dest)).send(s.createTextMessage(dest), DeliveryMode.NON_PERSISTENT, 4, 0);
            // Do not close
        }
        {
            QueueConnection c = f.createQueueConnection();
            QueueSession s = c.createQueueSession(false, Session.AUTO_ACKNOWLEDGE);
            s.createSender(s.createQueue(dest)).send(s.createTextMessage(dest));
            // Do not close
        }
        
        // Check
        w0.check(2, 2, 0);
        clearQueue(dest, 2);
        
        // Should throw exception now for 3rd connection
        try {
            QueueConnection c = f.createQueueConnection();
            QueueSession s = c.createQueueSession(false, Session.AUTO_ACKNOWLEDGE);
            s.createSender(s.createQueue(dest)).send(s.createTextMessage(dest));
            // Do not close
            
            throw new Throwable("Did not throw");
        } catch (Exception expected) {
            // ok
        }

        // Check connection states
        w0.check(2, 2, 0);
        getExtConnectionManager(f).clearAll();
        w0.check(0, 0, 0);
    }

    /**
     * Area: Extended connection manager
     * Test: blocking behavior
     * Assertion: returning connection unblocks waiting thread 
     * 
     * @throws Throwable on failure
     */
    public void testCMBlockingUnblock() throws Throwable {
        // Configure
        Properties options = new Properties();
        options.setProperty(Options.Out.POOL_MAXSIZE, "2");
        options.setProperty(Options.Out.POOL_TIMEOUT, "0");
        
        // Save state
        WireCount w0 = getConnectionCount();
        
        // Create
        final QueueConnectionFactory f = createCMQF(options);

        // Do work
        final String dest = generateName();
        QueueConnection cs;
        {
            cs = f.createQueueConnection();
            QueueSession s = cs.createQueueSession(false, Session.AUTO_ACKNOWLEDGE);
            s.createSender(s.createQueue(dest)).send(s.createTextMessage(dest), DeliveryMode.NON_PERSISTENT, 4, 0);
            // Do not close
        }
        {
            QueueConnection c = f.createQueueConnection();
            QueueSession s = c.createQueueSession(false, Session.AUTO_ACKNOWLEDGE);
            s.createSender(s.createQueue(dest)).send(s.createTextMessage(dest));
            // Do not close
        }
        
        // Check
        w0.check(2, 2, 0);
        clearQueue(dest, 2);
        
        final Exception[] exc = new Exception[1];
        
        // Block
        new Thread() {
            public void run() {
                try {
                    QueueConnection c = f.createQueueConnection();
                    QueueSession s = c.createQueueSession(false, Session.AUTO_ACKNOWLEDGE);
                    s.createSender(s.createQueue(dest)).send(s.createTextMessage(dest));
                    // don't close: wirecount will remain the same
                } catch (Exception expected) {
                    exc[0] = expected;
                }
            }
        }.start();
        
        // Check connection states
        w0.check(2, 2, 0);
        
        // No msgs until a connection is closed
        clearQueue(dest, 0);
        clearQueue(dest, 0);
        
        // Release a connection
        cs.close();
        
        if (exc[0] != null) {
            throw exc[0];
        }
        
        // Msg now should be there
        clearQueue(dest, 1);
        w0.check(2, 2, 0);
        
        getExtConnectionManager(f).clearAll();
        w0.check(0, 0, 0);
    }
    
    /**
     * Area: Extended connection manager
     * Test: blocking behavior
     * Assertion: ra.stop() unblocks all threads 
     * 
     * @throws Throwable on failure
     */
    public void testCMStopUnblocks() throws Throwable {
        // Configure
        Properties options = new Properties();
        options.setProperty(Options.Out.POOL_MAXSIZE, "2");
        options.setProperty(Options.Out.POOL_TIMEOUT, "0");
        
        // Save state
        WireCount w0 = getConnectionCount();
        
        // Create
        final QueueConnectionFactory f = createCMQF(options);

        // Do work
        final String dest = generateName();
        QueueConnection cs1;
        QueueConnection cs2;
        {
            cs1 = f.createQueueConnection();
            QueueSession s = cs1.createQueueSession(false, Session.AUTO_ACKNOWLEDGE);
            s.createSender(s.createQueue(dest)).send(s.createTextMessage(dest), DeliveryMode.NON_PERSISTENT, 4, 0);
            // Do not close
        }
        {
            cs2 = f.createQueueConnection();
            QueueSession s = cs2.createQueueSession(false, Session.AUTO_ACKNOWLEDGE);
            s.createSender(s.createQueue(dest)).send(s.createTextMessage(dest));
            // Do not close
        }
        
        // Check
        w0.check(2, 2, 0);
        clearQueue(dest, 2);
        
        final Exception[] exc = new Exception[1];
        
        // Block
        new Thread() {
            public void run() {
                try {
                    QueueConnection c = f.createQueueConnection();
                    QueueSession s = c.createQueueSession(false, Session.AUTO_ACKNOWLEDGE);
                    s.createSender(s.createQueue(dest)).send(s.createTextMessage(dest));
                    // don't close: wirecount will remain the same
                } catch (Exception expected) {
                    exc[0] = expected;
                }
            }
        }.start();
        
        // Check connection states
        w0.check(2, 2, 0);
        
        // No msgs until a connection is closed
        clearQueue(dest, 0);
        clearQueue(dest, 0);
        
        // Release a connection
        getRA(f).stop();
        
        if (exc[0] != null) {
            throw exc[0];
        }
        
        // Msg should NOT be there
        clearQueue(dest, 0);
        w0.check(2, 2, 0);
        
        cs1.close();
        w0.check(1, 1, 0);

        cs2.close();
        w0.check(0, 0, 0);
        
        getExtConnectionManager(f).clearAll();
        w0.check(0, 0, 0);
    }
    
    /**
     * Area: Extended connection manager
     * Test: auto enlist case 3: begin; create; close; commit;
     * Assertion:  
     * 
     * @throws Throwable on failure
     */
    public void testCMXABase() throws Throwable {
        // Set txmgr for testing
        TxMgr.setUnitTestTxMgr(new TestTransactionManager());
        
        // Configure
        Properties options = new Properties();
        options.setProperty(Options.Out.POOL_MAXSIZE, "1");
        
        // Create
        QueueConnectionFactory f = createCMQF(options);

        // Commit one
        String dest = generateName();
        {
            TxMgr.getUnitTestTxMgr().begin();
            QueueConnection c = f.createQueueConnection();
            QueueSession s = c.createQueueSession(false, Session.AUTO_ACKNOWLEDGE);
            s.createSender(s.createQueue(dest)).send(s.createTextMessage(dest));
            TxMgr.getUnitTestTxMgr().commit();
            c.close();
        }
        
        // Check
        clearQueue(dest, 1);
        
        // Rollback one
        {
            TxMgr.getUnitTestTxMgr().begin();
            QueueConnection c = f.createQueueConnection();
            QueueSession s = c.createQueueSession(false, Session.AUTO_ACKNOWLEDGE);
            s.createSender(s.createQueue(dest)).send(s.createTextMessage(dest));
            TxMgr.getUnitTestTxMgr().rollback();
            c.close();
        }
        
        // Check
        clearQueue(dest, 0);
        
        getExtConnectionManager(f).testIdleConsistency();
        getExtConnectionManager(f).clear();
    }
    
    /**
     * Area: Extended connection manager
     * Test: auto enlist case 1: begin; create; close; commit;
     * Assertion:  
     * 
     * @throws Throwable on failure
     */
    public void testCMXACloseInTx() throws Throwable {
        // Set txmgr for testing
        TxMgr.setUnitTestTxMgr(new TestTransactionManager());
        
        // Configure
        Properties options = new Properties();
        options.setProperty(Options.Out.POOL_MAXSIZE, "2");
        
        // Create
        QueueConnectionFactory f = createCMQF(options);

        // Save state
        WireCount w0 = getConnectionCount();
        
        // Commit one
        String dest = generateName();
        {
            TxMgr.getUnitTestTxMgr().begin();
            QueueConnection c1 = f.createQueueConnection();
            QueueSession s1 = c1.createQueueSession(false, Session.AUTO_ACKNOWLEDGE);
            s1.createSender(s1.createQueue(dest)).send(s1.createTextMessage(dest));
            w0.check(1, 1, 0);
            c1.close();
            w0.check(1, 1, 0);
            
            // Connection cannot be reused
            Transaction tx = TxMgr.getUnitTestTxMgr().suspend();
            QueueConnection c2 = f.createQueueConnection();
            QueueSession s2 = c2.createQueueSession(false, Session.AUTO_ACKNOWLEDGE);
            s2.createSender(s2.createQueue(dest)).send(s2.createTextMessage(dest));
            w0.check(2, 2, 0);
            // Close does return to pool
            c2.close();
            // Note: producer pooling is off
            w0.check(2, 1, 0);
            
            TxMgr.getUnitTestTxMgr().resume(tx);
            TxMgr.getUnitTestTxMgr().commit();

            w0.check(2, 0, 0);
        }
        
        // Check
        clearQueue(dest, 2);
        
        // Rollback one
        {
            TxMgr.getUnitTestTxMgr().begin();
            QueueConnection c1 = f.createQueueConnection();
            QueueSession s1 = c1.createQueueSession(false, Session.AUTO_ACKNOWLEDGE);
            s1.createSender(s1.createQueue(dest)).send(s1.createTextMessage(dest));
            w0.check(2, 1, 0);
            c1.close();
            w0.check(2, 1, 0);
            
            // Connection cannot be reused
            Transaction tx = TxMgr.getUnitTestTxMgr().suspend();
            QueueConnection c2 = f.createQueueConnection();
            QueueSession s2 = c2.createQueueSession(false, Session.AUTO_ACKNOWLEDGE);
            s2.createSender(s2.createQueue(dest)).send(s2.createTextMessage(dest));
            w0.check(2, 2, 0);
            // Close does return to pool
            c2.close();
            // Note: producer pooling is off
            w0.check(2, 1, 0);
            
            TxMgr.getUnitTestTxMgr().resume(tx);
            TxMgr.getUnitTestTxMgr().rollback();
        }
        
        // Check
        clearQueue(dest, 1);
        
        getExtConnectionManager(f).testIdleConsistency();
        getExtConnectionManager(f).clear();
    }
    
    /**
     * Area: Extended connection manager
     * Test: auto enlist case 5: begin; create; close; create; close; commit;
     * Assertion:  
     * 
     * @throws Throwable on failure
     */
    public void testCMXACloseInTxReuse() throws Throwable {
        // Set txmgr for testing
        TxMgr.setUnitTestTxMgr(new TestTransactionManager());
        
        // Configure
        Properties options = new Properties();
        options.setProperty(Options.Out.POOL_MAXSIZE, "2");
        
        // Create
        QueueConnectionFactory f = createCMQF(options);

        // Save state
        WireCount w0 = getConnectionCount();
        
        // Commit one
        String dest = generateName();
        {
            TxMgr.getUnitTestTxMgr().begin();
            QueueConnection c1 = f.createQueueConnection();
            QueueSession s1 = c1.createQueueSession(false, Session.AUTO_ACKNOWLEDGE);
            s1.createSender(s1.createQueue(dest)).send(s1.createTextMessage(dest));
            w0.check(1, 1, 0);
            c1.close();
            w0.check(1, 1, 0);
            
            // Connection should be reused
            for (int i = 0; i < 5; i++) {
                QueueConnection c2 = f.createQueueConnection();
                QueueSession s2 = c2.createQueueSession(false, Session.AUTO_ACKNOWLEDGE);
                s2.createSender(s2.createQueue(dest)).send(s2.createTextMessage(dest));
                // Note: producer pooling is off
                w0.check(1, 1 + 1 + i, 0);
                c2.close();
                w0.check(1, 1 + 1 + i, 0);
            }
            
            TxMgr.getUnitTestTxMgr().commit();
        }
        
        // Check
        clearQueue(dest, 6);
        
        // Rollback one
        {
            TxMgr.getUnitTestTxMgr().begin();
            QueueConnection c1 = f.createQueueConnection();
            QueueSession s1 = c1.createQueueSession(false, Session.AUTO_ACKNOWLEDGE);
            s1.createSender(s1.createQueue(dest)).send(s1.createTextMessage(dest));
            w0.check(1, 1, 0);
            c1.close();
            w0.check(1, 1, 0);
            
            // Connection should be reused
            for (int i = 0; i < 5; i++) {
                QueueConnection c2 = f.createQueueConnection();
                QueueSession s2 = c2.createQueueSession(false, Session.AUTO_ACKNOWLEDGE);
                s2.createSender(s2.createQueue(dest)).send(s2.createTextMessage(dest));
                // Note: producer pooling is off
                w0.check(1, 1 + 1 + i, 0);
                c2.close();
                w0.check(1, 1 + 1 + i, 0);
            }
            
            TxMgr.getUnitTestTxMgr().rollback();
        }
        
        // Check
        clearQueue(dest, 0);
        
        getExtConnectionManager(f).testIdleConsistency();
        getExtConnectionManager(f).clear();
    }
    
//    /**
//     * Area: Extended connection manager
//     * Test: base test case
//     * Assertion: CM can recover from connection failure 
//     * 
//     * @throws Throwable on failure
//     */
//    public void testCMConnectionFailure() throws Throwable {
//        // Configure
//        Properties options = new Properties();
//        options.setProperty(Options.Out.POOL_MAXSIZE, "2");
//        
//        // Save state
//        WireCount w0 = getConnectionCount();
//        
//        // Create
//        QueueConnectionFactory f = createCMQF(options);
//        UrlParser realUrl = new UrlParser(getConnectionUrl());
//        TcpProxyNIO proxy = new TcpProxyNIO(realUrl.getHost(), realUrl.getPort());
//        String proxyUrl = createConnectionUrl("localhost", proxy.getPort());
//        getConnectionManager(f).getRAJMSResourceAdapter().setConnectionURL(proxyUrl);
//        
//
//        // Do work
//        String dest = generateName();
//        QueueConnection c = f.createQueueConnection();
//        QueueSession s = c.createQueueSession(false, Session.AUTO_ACKNOWLEDGE);
//        s.createSender(s.createQueue(dest)).send(s.createTextMessage(dest));
//        c.close();
//        
//        clearQueue(dest, 1);
//        proxy.killAllConnections();
//        
//        try {
//        c = f.createQueueConnection();
//        s = c.createQueueSession(false, Session.AUTO_ACKNOWLEDGE);
//        s.createSender(s.createQueue(dest)).send(s.createTextMessage(dest));
//        c.close();
//        } catch (Exception e) {
//            
//        }
//        
//        // Check
//        clearQueue(dest, 1);
//        
//        // Check connection states
//        w0.check(1, 0, 0);
//        getExtConnectionManager(f).clear();
//        w0.check(0, 0, 0);
//        
//        assertTrue(proxy.getConnectionsCreated() > 0);
//        
//        TcpProxyNIO.safeClose(proxy);
//    }

    /**
     * Area: Extended connection manager
     * Test: base test case
     * Assertion: Connection creation failure 
     * 
     * @throws Throwable on failure
     */
    public void testCMConnectionFailureCreate() throws Throwable {
        // Configure
        Properties options = new Properties();
        options.setProperty(Options.Out.POOL_MAXSIZE, "2");
        
        // Save state
        WireCount w0 = getConnectionCount();
        
        // Create
        QueueConnectionFactory f = createCMQF(options);
        
        XManagedConnectionFactory.TestAllocator allocator = new XManagedConnectionFactory.TestAllocator() {

            public XManagedConnection createConnection(XManagedConnectionFactory factory, Subject subject, XConnectionRequestInfo descr) throws ResourceException {
                throw new ResourceException("Intentional");
            }
        };
        
        // Set failure mode
        getConnectionManager(f).getMCF().testSetAllocator(allocator);
        
        // Cause failures
        for (int i = 0; i < 5; i++) {
            try {
                QueueConnection c = f.createQueueConnection();
                c.createQueueSession(false, Session.AUTO_ACKNOWLEDGE);
                throw new Throwable("Did not throw");
            } catch (Exception expected) {
                // ok
            }
        }
        
        // Reset failure mode
        getConnectionManager(f).getMCF().testSetAllocator(null);
        
        // Verify that works correctly
        String dest = generateName();
        QueueConnection c = f.createQueueConnection();
        QueueSession s = c.createQueueSession(false, Session.AUTO_ACKNOWLEDGE);
        s.createSender(s.createQueue(dest)).send(s.createTextMessage(dest));
        c.close();
        
        // Check
        clearQueue(dest, 1);
        
        // Check connection states
        w0.check(1, 0, 0);
        getExtConnectionManager(f).testIdleConsistency();
        getExtConnectionManager(f).clear();
        w0.check(0, 0, 0);
    }

    /**
     * Area: Extended connection manager
     * Test: base test case
     * Assertion: Connection enlistment failure 
     * 
     * @throws Throwable on failure
     */
    public void testCMConnectionFailureEnlist() throws Throwable {
        // Configure
        Properties options = new Properties();
        options.setProperty(Options.Out.POOL_MAXSIZE, "2");
        
        // Save state
        WireCount w0 = getConnectionCount();
        
        // Create
        QueueConnectionFactory f = createCMQF(options);
        
        XManagedConnectionFactory.TestAllocator allocator = new XManagedConnectionFactory.TestAllocator() {

            public XManagedConnection createConnection(XManagedConnectionFactory factory, Subject subject, XConnectionRequestInfo descr) throws ResourceException {
                return new XManagedConnection(factory, subject, descr) {
                    public XAResource getXAResource() {
                        return null;
                    }
                };
            }
        };
        
        // Set failure mode
        getConnectionManager(f).getMCF().testSetAllocator(allocator);

        // Set txmgr for testing
        TxMgr.setUnitTestTxMgr(new TestTransactionManager());
        TxMgr.getUnitTestTxMgr().begin();
        
        
        // Cause failures
        for (int i = 0; i < 5; i++) {
            try {
                QueueConnection c = f.createQueueConnection();
                c.createQueueSession(false, Session.AUTO_ACKNOWLEDGE);
                throw new Throwable("Did not throw");
            } catch (Exception expected) {
                // ok
            }
        }
        
        // Reset failure mode
        getConnectionManager(f).getMCF().testSetAllocator(null);
        
        // Verify that works correctly
        String dest = generateName();
        QueueConnection c = f.createQueueConnection();
        QueueSession s = c.createQueueSession(false, Session.AUTO_ACKNOWLEDGE);
        s.createSender(s.createQueue(dest)).send(s.createTextMessage(dest));
        c.close();
        
        TxMgr.getUnitTestTxMgr().commit();
        
        // Check
        clearQueue(dest, 1);
        
        // Check connection states
        w0.check(1, 0, 0);
        getExtConnectionManager(f).testIdleConsistency();
        getExtConnectionManager(f).clear();
        w0.check(0, 0, 0);
    }

    /**
     * Area: Extended connection manager
     * Test: base test case
     * Assertion: Connection reuse failure (returns invalid) 
     * 
     * @throws Throwable on failure
     */
    public void testCMConnectionFailureReuse() throws Throwable {
        // Configure
        Properties options = new Properties();
        options.setProperty(Options.Out.POOL_MAXSIZE, "2");
        
        // Save state
        WireCount w0 = getConnectionCount();
        
        // Create
        QueueConnectionFactory f = createCMQF(options);
        
        final boolean invalid[] = new boolean[1];
        
        XManagedConnectionFactory.TestAllocator allocator = new XManagedConnectionFactory.TestAllocator() {
            public XManagedConnection createConnection(XManagedConnectionFactory factory, Subject subject, XConnectionRequestInfo descr) throws ResourceException {
                return new XManagedConnection(factory, subject, descr) {
                    public boolean isInvalid() {
                        return invalid[0];
                    }
                };
            }
        };
        
        // Set failure mode
        getConnectionManager(f).getMCF().testSetAllocator(allocator);

        // Verify that works correctly
        String dest = generateName();
        {
        QueueConnection c = f.createQueueConnection();
        QueueSession s = c.createQueueSession(false, Session.AUTO_ACKNOWLEDGE);
        s.createSender(s.createQueue(dest)).send(s.createTextMessage(dest));
        c.close();
        }
       
        clearQueue(dest, 1);
        
        // Make reuse false
        invalid[0] = true;
        
        // Verify that works correctly
        {
        QueueConnection c = f.createQueueConnection();
        QueueSession s = c.createQueueSession(false, Session.AUTO_ACKNOWLEDGE);
        s.createSender(s.createQueue(dest)).send(s.createTextMessage(dest));
        c.close();
        }
        
        // Check
        clearQueue(dest, 1);
        
        // Check connection states
        w0.check(1, 0, 0);
        getExtConnectionManager(f).testIdleConsistency();
        getExtConnectionManager(f).clear();
        w0.check(0, 0, 0);
    }
    
    /**
     * Area: Extended connection manager
     * Test: base test case
     * Assertion: Connection enlistment failure 
     * 
     * @throws Throwable on failure
     */
    public void testCMConnectionFailureCleanup() throws Throwable {
        // Configure
        Properties options = new Properties();
        options.setProperty(Options.Out.POOL_MAXSIZE, "2");
        
        // Save state
        WireCount w0 = getConnectionCount();
        
        // Create
        QueueConnectionFactory f = createCMQF(options);

        final boolean invalid[] = new boolean[1];
        
        XManagedConnectionFactory.TestAllocator allocator = new XManagedConnectionFactory.TestAllocator() {
            public XManagedConnection createConnection(XManagedConnectionFactory factory, Subject subject, XConnectionRequestInfo descr) throws ResourceException {
                return new XManagedConnection(factory, subject, descr) {
                    public void cleanup() throws ResourceException {
                        if (invalid[0]) {
                            throw new ResourceException("Intentional");
                        } else {
                            super.cleanup();
                        }
                    }
                };
            }
        };
        
        // Set failure mode
        getConnectionManager(f).getMCF().testSetAllocator(allocator);

        // Set txmgr for testing
        TxMgr.setUnitTestTxMgr(new TestTransactionManager());
        TxMgr.getUnitTestTxMgr().begin();
        
        invalid[0] = true;
        
        // cleanup exception should not disrupt
        String dest = generateName();
        for (int i = 0; i < 5; i++) {
            QueueConnection c = f.createQueueConnection();
            QueueSession s = c.createQueueSession(false, Session.AUTO_ACKNOWLEDGE);
            s.createSender(s.createQueue(dest)).send(s.createTextMessage(dest));
            c.close();
        }
        
        
        TxMgr.getUnitTestTxMgr().commit();

        // connection should be destroyed 
        w0.check(0, 0, 0);
        
        // Check
        clearQueue(dest, 5);
        
        invalid[0] = false;

        // Verify that works correctly
        {
            QueueConnection c = f.createQueueConnection();
            QueueSession s = c.createQueueSession(false, Session.AUTO_ACKNOWLEDGE);
            s.createSender(s.createQueue(dest)).send(s.createTextMessage(dest));
            c.close();
        }
        
        clearQueue(dest, 1);

        // Check connection states
        w0.check(1, 0, 0);
        getExtConnectionManager(f).testIdleConsistency();
        getExtConnectionManager(f).clear();
        w0.check(0, 0, 0);
    }

    public class Container {
        MessageListener mMDB;
        private XWorkManager mWorkManager;
        private XBootstrapContext mBC;
        private MDBFactory mMDBFact;
        private TestTransactionManager mTxMgr;
        private MessageEndpointFactory mMEF;
        private boolean mNoTx;

        public Container(MessageListener mdb) {
            mMDB = mdb;
        }
        
        public void setMDB(MessageListener mdb) {
            mMDB = mdb;
        }
        
        public WorkManager getWorkManager() {
            if (mWorkManager == null) {
                mWorkManager = new XWorkManager(1);
            }
            return mWorkManager;
        }
        
        public BootstrapContext getBootstrapContext() {
            if (mBC == null) {
                mBC = new XBootstrapContext(getWorkManager());
            }
            return mBC;
        }
        
        public MDBFactory getMDBFactory() { 
            if (mMDBFact == null) {
                mMDBFact = new MDBFactory() {
                    public Object createMDB() {
                        return mMDB;
                    }
                };
            }
            return mMDBFact;
        }
        
        public TestTransactionManager getTransactionManager() {
            if (mTxMgr == null && !mNoTx) {
                mTxMgr = new TestTransactionManager();
                TxMgr.setUnitTestTxMgr(mTxMgr);
            }
            return mTxMgr;
        }
        
        public MessageEndpointFactory getMessageEndpointFactory() {
            if (mMEF == null) {
                Method m;
                try {
                    m = MessageListener.class.getMethod("onMessage",
                        new Class[] { javax.jms.Message.class });
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
                
                mMEF = new XMessageEndpointFactory(m, MessageListener.class, getMDBFactory(), getTransactionManager());
            }
            return mMEF;
        }

        public void setNoTx() {
            mNoTx = true;
        }
        
    }
    
    /**
     * Without batch and with HUA, use a different thread to process a message
     * 
     * @throws Throwable
     */
    public void testHUATxTakenAway() throws Throwable {
        Passthrough p = new StcmsPassthrough(mServerProperties);

        // RA
        RASTCMSResourceAdapter ra = new RASTCMSResourceAdapter();
        ra.setConnectionURL(getConnectionURL());

        // Spec
        RASTCMSActivationSpec spec = new RASTCMSActivationSpec();
        spec.setUserName(p.getUserid());
        spec.setPassword(p.getPassword());
        spec.setConcurrencyMode("sync");
        spec.setDestinationType(javax.jms.Queue.class.getName());
        spec.setDestination(p.getQueue1Name());
        spec.setEndpointPoolMaxSize("1");
        spec.setHoldUntilAck("1");

        // MCF
        XManagedConnectionFactory mcf = new XMCFQueueXA();
        mcf.setResourceAdapter(ra);
        final QueueConnectionFactory fact = (QueueConnectionFactory) mcf.createConnectionFactory(null);

        final Container c = new Container(null);
        
        // An MDB
        final MessageListener mdb = new MessageListener() {
            boolean once;
            public void onMessage(final Message msg) {
                try {
                    final Transaction tx = c.getTransactionManager().suspend();
                    new Thread() {
                        public void run() {
                            try {
                                System.out.println(((TextMessage) msg).getText());
                                c.getTransactionManager().resume(tx);
                                Connection c = fact.createConnection(); 
                                Session s = c.createSession(false, Session.AUTO_ACKNOWLEDGE);
                                s.createProducer(s.createQueue("Queue2")).send(msg);
                                c.close();
                                if (!once) {
                                    msg.setBooleanProperty("JMSJCA.setRollbackOnly", true);
                                    once = true;
                                }
                                msg.acknowledge();
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    }.start();
                } catch (SystemException e1) {
                    e1.printStackTrace();
                }
            }
        };
        c.setMDB(mdb);

        try {
            ra.start(c.getBootstrapContext());
            ra.endpointActivation(c.getMessageEndpointFactory(), spec);
            p.setNMessagesToSend(5);
            p.clearQ1Q2Q3();
            p.passFromQ1ToQ2();
            ra.endpointDeactivation(c.getMessageEndpointFactory(), spec);
            p.get(p.getQueue1Name()).assertEmpty();
        } finally {
            Passthrough.safeClose(p);
        }
    }
    
    /**
     * Process HUA and batch with rollback
     * 
     * @throws Throwable
     */
    public void testHUABatchXA() throws Throwable {
        Passthrough p = new StcmsPassthrough(mServerProperties);

        // RA
        RASTCMSResourceAdapter ra = new RASTCMSResourceAdapter();
        ra.setConnectionURL(getConnectionURL());

        // Spec
        RASTCMSActivationSpec spec = new RASTCMSActivationSpec();
        spec.setUserName(p.getUserid());
        spec.setPassword(p.getPassword());
        spec.setConcurrencyMode("sync");
        spec.setDestinationType(javax.jms.Queue.class.getName());
        spec.setDestination(p.getQueue1Name());
        spec.setBatchSize(5);
        spec.setEndpointPoolMaxSize("1");
        spec.setHoldUntilAck("1");

        // MCF
        XManagedConnectionFactory mcf = new XMCFQueueXA();
        mcf.setResourceAdapter(ra);
        final QueueConnectionFactory fact = (QueueConnectionFactory) mcf.createConnectionFactory(null);
        
        // An MDB
        final MessageListener mdb = new MessageListener() {
            boolean once;
            public void onMessage(Message msg) {
                try {
                    if (msg.getObjectProperty("JMSJCA.EndOfBatch") == null) {
                        System.out.println(((TextMessage) msg).getText());
                        Connection c = fact.createConnection(); 
                        Session s = c.createSession(false, Session.AUTO_ACKNOWLEDGE);
                        s.createProducer(s.createQueue("Queue2")).send(msg);
                        c.close();

                        if (!once) {
                            msg.setBooleanProperty("JMSJCA.setRollbackOnly", true);
                            once = true;
                        }
                    } else {
                        System.out.println("End of batch");
                    }

                    msg.acknowledge();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        };

        try {
            Container c = new Container(mdb);
            ra.start(c.getBootstrapContext());
            ra.endpointActivation(c.getMessageEndpointFactory(), spec);
            p.setNMessagesToSend(50);
            p.clearQ1Q2Q3();
            p.passFromQ1ToQ2();
            ra.endpointDeactivation(c.getMessageEndpointFactory(), spec);
            p.get(p.getQueue1Name()).assertEmpty();
        } finally {
            Passthrough.safeClose(p);
        }
    }

    /**
     * Process HUA and batch with a DLQ
     * 
     * @throws Throwable
     */
    public void testHUABatchDlqXA() throws Throwable {
        Passthrough p = new StcmsPassthrough(mServerProperties);

        // RA
        RASTCMSResourceAdapter ra = new RASTCMSResourceAdapter();
        ra.setConnectionURL(getConnectionURL());

        // Spec
        RASTCMSActivationSpec spec = new RASTCMSActivationSpec();
        spec.setUserName(p.getUserid());
        spec.setPassword(p.getPassword());
        spec.setConcurrencyMode("sync");
        spec.setDestinationType(javax.jms.Queue.class.getName());
        spec.setDestination(p.getQueue1Name());
        spec.setBatchSize(3);
        spec.setEndpointPoolMaxSize("1");
        spec.setHoldUntilAck("1");
        spec.setRedeliveryHandling("1:move(queue:Queue2)");

        // MCF
        XManagedConnectionFactory mcf = new XMCFQueueXA();
        mcf.setResourceAdapter(ra);
        final QueueConnectionFactory fact = (QueueConnectionFactory) mcf.createConnectionFactory(null);

        // An MDB
        final MessageListener mdb = new MessageListener() {
            int k;
            public void onMessage(Message msg) {
                try {
                    int ibatch = ((Integer) msg.getObjectProperty(WMessageIn.IBATCH)).intValue();
                    int size = ((Integer) msg.getObjectProperty(WMessageIn.BATCHSIZE)).intValue();
                    if (++k == size) {
                        k = 0;
                    }
                    if (msg.getObjectProperty("JMSJCA.EndOfBatch") == null) {
                        Connection c = fact.createConnection(); 
                        Session s = c.createSession(false, Session.AUTO_ACKNOWLEDGE);
                        s.createProducer(s.createQueue("Queue2")).send(msg);
                        c.close();

                        if (ibatch == k) {
                            msg.setBooleanProperty("JMSJCA.setRollbackOnly", true);
                            System.out.println("ROLLBACK:");
                        }
                        System.out.println(((TextMessage) msg).getText() + "; " + ibatch);
                    } else {
//                        if (ibatch != 0) {
//                            msg.setBooleanProperty("JMSJCA.setRollbackOnly", true);
//                            System.out.println("ROLLBACK EOB:");
//                        }
                        System.out.println("End of batch" + "; " + ibatch);
                    }
                    msg.acknowledge();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        };

        try {
            Container c = new Container(mdb);
            ra.start(c.getBootstrapContext());
            ra.endpointActivation(c.getMessageEndpointFactory(), spec);
            p.setNMessagesToSend(150);
            p.clearQ1Q2Q3();
            p.setTimeout(300000);
            p.passFromQ1ToQ2();
            ra.endpointDeactivation(c.getMessageEndpointFactory(), spec);
            p.get(p.getQueue1Name()).assertEmpty();
        } finally {
            Passthrough.safeClose(p);
        }
    }

    /**
     * Process HUA and batch with a DLQ
     * 
     * @throws Throwable
     */
    public void testDlqXA() throws Throwable {
        Passthrough p = new StcmsPassthrough(mServerProperties);

        // RA
        RASTCMSResourceAdapter ra = new RASTCMSResourceAdapter();
        ra.setConnectionURL(getConnectionURL());

        // Spec
        RASTCMSActivationSpec spec = new RASTCMSActivationSpec();
        spec.setUserName(p.getUserid());
        spec.setPassword(p.getPassword());
        spec.setConcurrencyMode("sync");
        spec.setDestinationType(javax.jms.Queue.class.getName());
        spec.setDestination(p.getQueue1Name());
        spec.setEndpointPoolMaxSize("1");
        spec.setRedeliveryHandling("2:move(queue:Queue2)");

        // MCF
        XManagedConnectionFactory mcf = new XMCFQueueXA();
        mcf.setResourceAdapter(ra);

        // An MDB
        final MessageListener mdb = new MessageListener() {
            public void onMessage(Message msg) {
                try {
                    TxMgr.getUnitTestTxMgr().setRollbackOnly();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        };

        try {
            Container c = new Container(mdb);
            ra.start(c.getBootstrapContext());
            ra.endpointActivation(c.getMessageEndpointFactory(), spec);
            p.setNMessagesToSend(150);
            p.clearQ1Q2Q3();
            p.setTimeout(300000);
            p.passFromQ1ToQ2();
            ra.endpointDeactivation(c.getMessageEndpointFactory(), spec);
            p.get(p.getQueue1Name()).assertEmpty();
        } finally {
            Passthrough.safeClose(p);
        }
    }

    /**
     * Process HUA and batch with a DLQ
     * 
     * @throws Throwable
     */
    public void testHUABatchDlq() throws Throwable {
        Passthrough p = new StcmsPassthrough(mServerProperties);

        // RA
        RASTCMSResourceAdapter ra = new RASTCMSResourceAdapter();
        ra.setConnectionURL(getConnectionURL());

        // Spec
        RASTCMSActivationSpec spec = new RASTCMSActivationSpec();
        spec.setUserName(p.getUserid());
        spec.setPassword(p.getPassword());
        spec.setConcurrencyMode("sync");
        spec.setDestinationType(javax.jms.Queue.class.getName());
        spec.setDestination(p.getQueue1Name());
        spec.setBatchSize(5);
        spec.setEndpointPoolMaxSize("1");
        spec.setHoldUntilAck("1");
        spec.setRedeliveryHandling("3:move(queue:Queue2)");

        // An MDB
        final MessageListener mdb = new MessageListener() {
            int k;
            public void onMessage(Message msg) {
                try {
                    int ibatch = ((Integer) msg.getObjectProperty(WMessageIn.IBATCH)).intValue();
                    int size = ((Integer) msg.getObjectProperty(WMessageIn.BATCHSIZE)).intValue();
                    if (++k == size) {
                        k = 0;
                    }
                    if (msg.getObjectProperty("JMSJCA.EndOfBatch") == null) {
                        System.out.println(((TextMessage) msg).getText() + "; " + ibatch);
                        if (ibatch == k) {
                            msg.setBooleanProperty("JMSJCA.setRollbackOnly", true);
                        }
                    } else {
                        if (ibatch != 0) {
                            msg.setBooleanProperty("JMSJCA.setRollbackOnly", true);
                        }
                        System.out.println("End of batch" + "; " + ibatch);
                    }
                    msg.acknowledge();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        };

        try {
            Container c = new Container(mdb);
            c.setNoTx();
            ra.start(c.getBootstrapContext());
            ra.endpointActivation(c.getMessageEndpointFactory(), spec);
            p.setNMessagesToSend(100);
            p.clearQ1Q2Q3();
            p.setTimeout(300000);
            p.passFromQ1ToQ2();
            ra.endpointDeactivation(c.getMessageEndpointFactory(), spec);
            p.get(p.getQueue1Name()).assertEmpty();
        } finally {
            Passthrough.safeClose(p);
        }
    }

    public static class ResourceHolder {
        private XXid mXid;
        
        public Xid getXid() {
            return mXid;
        }

        public void setXid(XXid xid) {
            mXid = xid;
        }
    }

    /**
     * A simple rudimentary transaction for unit testing
     * 
     * @author fkieviet
     */
    public static class TestTransaction implements Transaction {
        private Map mAllResources = new IdentityHashMap();
        private List mEnlistedResources = new ArrayList();
        private List mSuspendedResources = new ArrayList();
        private List mSynchronizations = new ArrayList();
        private boolean mRollbackOnly;
        private TestTransactionManager mTxMgr;
        
        public TestTransaction(TestTransactionManager txmgr) {
            mTxMgr = txmgr;
        }

        /**
         * @see javax.transaction.Transaction#commit()
         */
        public void commit() throws RollbackException, HeuristicMixedException, HeuristicRollbackException, SecurityException, IllegalStateException, SystemException {
            // syn.before
            for (Iterator syns = mSynchronizations.iterator(); syns.hasNext();) {
                Synchronization syn = (Synchronization) syns.next();
                syn.beforeCompletion();
            }
            
            Exception failed = null;
            
            // prepare
            for (Iterator xars = mAllResources.entrySet().iterator(); xars.hasNext();) {
                Map.Entry x = (Entry) xars.next();
                XAResource xar = (XAResource) x.getKey();
                ResourceHolder h = (ResourceHolder) x.getValue();
                try {
                    xar.prepare(h.getXid());
                } catch (XAException e) {
                    failed = e;
                    mRollbackOnly = true;
                }
            }
            
            // commit/rollback
            for (Iterator xars = mAllResources.entrySet().iterator(); xars.hasNext();) {
                Map.Entry x = (Entry) xars.next();
                XAResource xar = (XAResource) x.getKey();
                ResourceHolder h = (ResourceHolder) x.getValue();
                try {
                    if (mRollbackOnly) {
                        xar.rollback(h.getXid()); 
                    } else {
                        xar.commit(h.getXid(), false);
                    }
                } catch (XAException e) {
                    throw new RuntimeException("commit failed; transaction left in doubt", e);
                }
            }

            // syn.before
            for (Iterator syns = mSynchronizations.iterator(); syns.hasNext();) {
                Synchronization syn = (Synchronization) syns.next();
                syn.afterCompletion(mRollbackOnly ? XAResource.TMFAIL : XAResource.TMSUCCESS);
            }
            
            mTxMgr.removeTx(this);

            if (failed != null) {
                throw new RuntimeException("prepare failed", failed);
            }
        }
        
        private void check(boolean test) {
            if (!test) {
                throw new RuntimeException("Assertion failed");
            }
        }

        /**
         * @see javax.transaction.Transaction#delistResource(javax.transaction.xa.XAResource, int)
         */
        public boolean delistResource(XAResource xar, int flag) throws IllegalStateException, SystemException {
            check(flag == XAResource.TMSUCCESS);
            boolean removed = mEnlistedResources.remove(xar);
            ResourceHolder h = (ResourceHolder) mAllResources.get(xar);
            check(removed);
            try {
                xar.end(h.getXid(), flag);
            } catch (XAException e) {
                throw new RuntimeException("delist failed", e);
            }
            return true;
        }

        /**
         * @see javax.transaction.Transaction#enlistResource(javax.transaction.xa.XAResource)
         */
        public boolean enlistResource(XAResource xar) throws RollbackException, IllegalStateException, SystemException {
            mAllResources.remove(xar);
            
            ResourceHolder h = new ResourceHolder();
            h.setXid(new XXid());
            
            try {
                xar.start(h.getXid(), XAResource.TMNOFLAGS);
            } catch (XAException e) {
                throw new RuntimeException("enlist failed", e);
            }
            mAllResources.put(xar, h);
            mEnlistedResources.add(xar);
            return true;
        }

        /**
         * @see javax.transaction.Transaction#getStatus()
         */
        public int getStatus() throws SystemException {
            return mRollbackOnly ? XAResource.TMSUCCESS : XAResource.TMFAIL;
        }

        /**
         * @see javax.transaction.Transaction#registerSynchronization(javax.transaction.Synchronization)
         */
        public void registerSynchronization(Synchronization syn) throws RollbackException, IllegalStateException, SystemException {
            mSynchronizations.add(syn);
        }

        /**
         * @see javax.transaction.Transaction#rollback()
         */
        public void rollback() throws IllegalStateException, SystemException {
            mRollbackOnly = true;
            try {
                commit();
            } catch (Exception e) {
                throw new RuntimeException("rollback failed", e);
            }
        }

        /**
         * @see javax.transaction.Transaction#setRollbackOnly()
         */
        public void setRollbackOnly() throws IllegalStateException, SystemException {
            mRollbackOnly = true;
        }

        public void suspendEnlisted() throws SystemException {
            XAResource[] xars = (XAResource[]) mEnlistedResources.toArray(new XAResource[mEnlistedResources.size()]);
            for (int i = 0; i < xars.length; i++) {
                try {
                    ResourceHolder h = (ResourceHolder) mAllResources.get(xars[i]);
                    xars[i].end(h.getXid(), XAResource.TMSUSPEND);
                    mSuspendedResources.add(xars[i]);
                    mEnlistedResources.remove(xars[i]);
                } catch (XAException e) {
                    throw new RuntimeException("end failed", e);
                }
            }
        }

        public void unsuspendAll() throws SystemException {
            XAResource[] xars = (XAResource[]) mSuspendedResources.toArray(new XAResource[mSuspendedResources.size()]);
            for (int i = 0; i < xars.length; i++) {
                try {
                    ResourceHolder h = (ResourceHolder) mAllResources.get(xars[i]);
                    xars[i].start(h.getXid(), XAResource.TMRESUME);
                    mSuspendedResources.remove(xars[i]);
                    mEnlistedResources.add(xars[i]);
                } catch (XAException e) {
                    throw new RuntimeException("start failed", e);
                }
            }
        }
    }

    /**
     * A simple rudimentary transaction manager for unit testing
     * 
     * @author fkieviet
     */
    public static class TestTransactionManager implements TransactionManager {
        private IdentityHashMap mActiveTransactions = new IdentityHashMap();
        
        /**
         * @see javax.transaction.TransactionManager#begin()
         */
        public void begin() throws NotSupportedException, SystemException {
            if (getTransaction() != null) {
                throw new RuntimeException("Tx already started");
            }
            TestTransaction tx = new TestTransaction(this);
            mActiveTransactions.put(Thread.currentThread(), tx);
        }
        
        private void check(boolean test) {
            if (!test) {
                throw new RuntimeException("Assertion failed");
            }
        }

        public void removeTx(TestTransaction tx) {
            if (mActiveTransactions.get(Thread.currentThread()) == tx) {
                check(mActiveTransactions.remove(Thread.currentThread()) != null);
            } else {
                boolean found = false;
                for (Iterator iter = mActiveTransactions.entrySet().iterator(); iter.hasNext();) {
                    Map.Entry element = (Map.Entry) iter.next();
                    if (element.getValue() == tx) {
                        iter.remove();
                        found = true;
                        break;
                    }
                }
                check(found);
            }
        }

        /**
         * @see javax.transaction.TransactionManager#commit()
         */
        public void commit() throws RollbackException, HeuristicMixedException, HeuristicRollbackException, SecurityException, IllegalStateException, SystemException {
            getTransaction().commit();
        }

        public int getStatus() throws SystemException {
            throw new RuntimeException("Not implemented");
        }

        /**
         * @see javax.transaction.TransactionManager#getTransaction()
         */
        public Transaction getTransaction() throws SystemException {
            return (Transaction) mActiveTransactions.get(Thread.currentThread());
        }

        /**
         * @see javax.transaction.TransactionManager#resume(javax.transaction.Transaction)
         */
        public void resume(Transaction tx1) throws InvalidTransactionException, IllegalStateException, SystemException {
            TestTransaction tx = (TestTransaction) tx1;
            tx.unsuspendAll();
            mActiveTransactions.put(Thread.currentThread(), tx);
        }

        /**
         * @see javax.transaction.TransactionManager#rollback()
         */
        public void rollback() throws IllegalStateException, SecurityException, SystemException {
            getTransaction().rollback();
            mActiveTransactions.remove(Thread.currentThread());
        }

        /**
         * @see javax.transaction.TransactionManager#setRollbackOnly()
         */
        public void setRollbackOnly() throws IllegalStateException, SystemException {
            getTransaction().setRollbackOnly();
        }

        public void setTransactionTimeout(int arg0) throws SystemException {
            throw new RuntimeException("Not implemented");
        }

        /**
         * @see javax.transaction.TransactionManager#suspend()
         */
        public Transaction suspend() throws SystemException {
            TestTransaction tx = (TestTransaction) getTransaction();
            if (tx == null) {
                throw new RuntimeException("No transaction");
            }
            mActiveTransactions.remove(Thread.currentThread());
            tx.suspendEnlisted();
            return tx;
        }
    }
    
//    public void testUnified1() throws Throwable {
//        // Configure
//        Properties options = new Properties();
//        options.setProperty(Options.Out.POOL_MAXSIZE, "2");
//        
//        // Save state
//        WireCount w0 = getConnectionCount();
//        
//        // Create
//        RAUnifiedResourceAdapter ra = new RAUnifiedResourceAdapter();
//        XManagedConnectionFactory mcf = new XMCFQueueXA();
//        mcf.setResourceAdapter(ra);
//        mcf.setOptions(Str.serializeProperties(options));
//        ra.setConnectionURL(getConnectionURL());
//        QueueConnectionFactory f = (QueueConnectionFactory) mcf.createConnectionFactory(null);
//
//        // Do work
//        String dest = generateName();
//        QueueConnection c = f.createQueueConnection();
//        QueueSession s = c.createQueueSession(false, Session.AUTO_ACKNOWLEDGE);
//        s.createSender(s.createQueue(dest)).send(s.createTextMessage(dest));
//        c.close();
//        
//        // Check
//        clearQueue(dest, 1);
//        
//        // Check connection states
//        w0.check(1, 0, 0);
//        getExtConnectionManager(f).clear();
//        w0.check(0, 0, 0);
//        
//    }
}
