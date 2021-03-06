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
import com.stc.jmsjca.stcms.RASTCMSActivationSpec;
import com.stc.jmsjca.stcms.RASTCMSObjectFactory;
import com.stc.jmsjca.stcms.RASTCMSResourceAdapter;
import com.stc.jmsjca.test.core.JMSProvider;
import com.stc.jmsjca.test.core.MockContainer;
import com.stc.jmsjca.test.core.Passthrough;
import com.stc.jmsjca.test.core.XTestBase;
import com.stc.jmsjca.util.Str;
import com.stc.jmsjca.util.UrlParser;

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
import javax.jms.XAQueueConnectionFactory;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.resource.ResourceException;
import javax.resource.spi.ActivationSpec;
import javax.resource.spi.ConnectionManager;
import javax.resource.spi.security.PasswordCredential;
import javax.security.auth.Subject;
import javax.transaction.SystemException;
import javax.transaction.Transaction;
import javax.transaction.xa.XAResource;

import java.io.File;
import java.io.Serializable;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

/**
 *
 * Unit tests not using the J2EE container, specific for STCMS.
 * This inherits from XTestBase so that it will include all generic tests.
 * See Base
 * 
 * Eclipse usage: set working directory to ${workspace_loc:jmsjca/build} and arguments:
 *
 * @author Frank Kieviet
 * @version 1.0
 */
@SuppressWarnings("unchecked")
public class BaseJUStd extends XTestBase {
    /**
     * @see com.stc.jmsjca.test.core.XTestBase#getJMSProvider()
     */
    @Override
    public JMSProvider getJMSProvider() {
        return new StcmsProvider();
    }

    /**
     * Constructor
     */
    public BaseJUStd() {
        this(null);
    }
    
    /**
     * Constructor
     *
     * @param name junit test name
     */
    public BaseJUStd(String name) {
        super(name);
    }

    @Override
    public WireCount getConnectionCount() {
        return new WireCount() {
            private int s0 = getWireCount();;

            @Override
            public void check(int sessions, int producers, int consumers) {
                int n = sessions + producers + consumers;
                int now = getWireCount();
                if (s0 + n != now) {
                    throw new RuntimeException("Assertion failure: invalid wire count "
                        + "s0 + exp != now; now=" + now + "; exp=" + n + "; s0=" + s0
                        + " // expected: " + n + "; found: " + (now - s0));
                }
            }

            @Override
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
     * Gets connection URL to the default test server; contains host, but not port.
     * Port should be obtained from a system property.
     *
     * @return URL
     */
    protected String getConnectionURL() {
        System.setProperty(RASTCMSObjectFactory.PORTPROP,
            mServerProperties.getProperty(StcmsProvider.PROPNAME_PORT));
        System.setProperty(RASTCMSObjectFactory.PORTSSLPROP,
                mServerProperties.getProperty(StcmsProvider.PROPNAME_SSLPORT));
        return "stcms://" + mServerProperties.getProperty(StcmsProvider.PROPNAME_HOST);
    }

    /**
     * @return host and port in URL form
     */
    protected String getFullConnectionUrl() {
        return assembleConnectionUrl(mServerProperties.getProperty(StcmsProvider.PROPNAME_HOST), Integer
            .parseInt(mServerProperties.getProperty(StcmsProvider.PROPNAME_PORT)));
    }

    /**
     * @param server server
     * @param port port
     * @return url with server and port
     */
    public String assembleConnectionUrl(String server, int port) {
        return "stcms://" + server + ":" + port;
    }
    
    /**
     * Gets connection URL for the alternative server connection
     *
     * @return URL
     */
    protected String getConnectionURL2() {
        return "stcmss://" + mServerProperties.getProperty(StcmsProvider.PROPNAME_HOST);
    }

    @Override
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
            ra.setUserName(getJMSProvider().getUserName(mServerProperties));
            ra.setPassword(getJMSProvider().getPassword(mServerProperties));

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
            ra.setUserName(getJMSProvider().getUserName(mServerProperties));
            ra.setPassword(getJMSProvider().getPassword(mServerProperties));

            // Factory to be used by the application (done by the Application server)
            TopicConnectionFactory f = (TopicConnectionFactory) x.createConnectionFactory();
            ctx.rebind(appjndiTopic, f);
        }
    }

    /**
     * @see com.stc.jmsjca.test.core.XTestBase#getXAQueueConnectionFactory()
     * @throws JMSException propagatd 
     */
    @Override
    public XAQueueConnectionFactory getXAQueueConnectionFactory() throws JMSException {
        UrlParser p = new UrlParser(getFullConnectionUrl());
        return new com.stc.jms.client.STCXAQueueConnectionFactory(p.getHost(), p.getPort());
    }
    

    /**
     * Returns the number of created wires minus the number of closed wires
     *
     * @return wire count
     */
    protected int getWireCount() {
        return com.stc.jms.sockets.Wire.sObjectCount - com.stc.jms.sockets.WirePool.sObjectCount;
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

        // Spec2 is identical
        RASTCMSActivationSpec spec2 = new RASTCMSActivationSpec();
        spec2.setConnectionURL(getConnectionURL());

        // Spec3 uses different URL
        RASTCMSActivationSpec spec3 = new RASTCMSActivationSpec();
        spec3.setConnectionURL(getConnectionURL2());

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

        @Override
        public ConnectionFactory createConnectionFactory(int domain,
            RAJMSResourceAdapter resourceAdapter, RAJMSActivationSpec activationSpec,
            XManagedConnectionFactory fact, String overrideUrl) throws JMSException {

            return new QueueCF();
        }

        @Override
        public boolean isUrl(String url) {
            return false;
        }

        @Override
        public String getJMSServerType() {
            return "Test";
        }

        @Override
        public RAJMSActivationSpec createActivationSpec() {
            return null;
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
        ra.setUserName(getJMSProvider().getUserName(mServerProperties));
        ra.setPassword(getJMSProvider().getPassword(mServerProperties));
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
        String dest = getQueue1Name();
        clearQueue(dest, -1);
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
        String dest = getQueue1Name();
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
        String dest = getQueue1Name();
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
        final String dest = getQueue1Name();
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
            @Override
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
        final String dest = getQueue1Name();
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
            @Override
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
        
        // Wait until the thread that was just started is running
        // (not safe -- could be improved)
        Thread.sleep(500);
        
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
        // Configure
        Properties options = new Properties();
        options.setProperty(Options.Out.POOL_MAXSIZE, "1");
        
        // Create
        QueueConnectionFactory f = createCMQF(options);

        // Commit one
        String dest = getQueue1Name();
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
        // Configure
        Properties options = new Properties();
        options.setProperty(Options.Out.POOL_MAXSIZE, "2");
        
        // Create
        QueueConnectionFactory f = createCMQF(options);

        // Save state
        WireCount w0 = getConnectionCount();
        
        // Commit one
        String dest = getQueue1Name();
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
        // Configure
        Properties options = new Properties();
        options.setProperty(Options.Out.POOL_MAXSIZE, "2");
        
        // Create
        QueueConnectionFactory f = createCMQF(options);

        // Save state
        WireCount w0 = getConnectionCount();
        
        // Commit one
        String dest = getQueue1Name();
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
        String dest = getQueue1Name();
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
                    @Override
                    public XAResource getXAResource() {
                        return null;
                    }
                };
            }
        };
        
        // Set failure mode
        getConnectionManager(f).getMCF().testSetAllocator(allocator);

        // Set txmgr for testing
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
        String dest = getQueue1Name();
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
                    @Override
                    public boolean isInvalid() {
                        return invalid[0];
                    }
                };
            }
        };
        
        // Set failure mode
        getConnectionManager(f).getMCF().testSetAllocator(allocator);

        // Verify that works correctly
        String dest = getQueue1Name();
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
                    @Override
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
        TxMgr.getUnitTestTxMgr().begin();
        
        invalid[0] = true;
        
        // cleanup exception should not disrupt
        String dest = getQueue1Name();
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

    /**
     * Without batch and with HUA, use a different thread to process a message
     * 
     * @throws Throwable
     */
    public void testHUATxTakenAway() throws Throwable {
        Passthrough p = getJMSProvider().createPassthrough(mServerProperties);

        // RA
        RASTCMSResourceAdapter ra = new RASTCMSResourceAdapter();
        ra.setConnectionURL(getConnectionURL());
        ra.setUserName(getJMSProvider().getUserName(mServerProperties));
        ra.setPassword(getJMSProvider().getPassword(mServerProperties));

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

        final MockContainer c = new MockContainer(null);
        
        // An MDB
        final MessageListener mdb = new MessageListener() {
            boolean once;
            public void onMessage(final Message msg) {
                try {
                    final Transaction tx = c.getTransactionManager().suspend();
                    new Thread() {
                        @Override
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
        Passthrough p = getJMSProvider().createPassthrough(mServerProperties);

        // RA
        RAJMSResourceAdapter ra = new RASTCMSResourceAdapter();
        ra.setConnectionURL(getConnectionURL());
        ra.setUserName(getJMSProvider().getUserName(mServerProperties));
        ra.setPassword(getJMSProvider().getPassword(mServerProperties));

        // Spec
        RAJMSActivationSpec spec = new RASTCMSActivationSpec();
        spec.setUserName(p.getUserid());
        spec.setPassword(p.getPassword());
        spec.setConcurrencyMode("sync");
        spec.setDestinationType(javax.jms.Queue.class.getName());
        spec.setDestination(p.getQueue1Name());
        spec.setBatchSize("5");
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
            MockContainer c = new MockContainer(mdb);
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
        Passthrough p = getJMSProvider().createPassthrough(mServerProperties);

        // RA
        RASTCMSResourceAdapter ra = new RASTCMSResourceAdapter();
        ra.setConnectionURL(getConnectionURL());
        ra.setUserName(getJMSProvider().getUserName(mServerProperties));
        ra.setPassword(getJMSProvider().getPassword(mServerProperties));

        // Spec
        RASTCMSActivationSpec spec = new RASTCMSActivationSpec();
        spec.setUserName(p.getUserid());
        spec.setPassword(p.getPassword());
        spec.setConcurrencyMode("sync");
        spec.setDestinationType(javax.jms.Queue.class.getName());
        spec.setDestination(p.getQueue1Name());
        spec.setBatchSize("3");
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
                    setAsyncError(e);
                }
            }
        };

        try {
            MockContainer c = new MockContainer(mdb);
            ra.start(c.getBootstrapContext());
            ra.endpointActivation(c.getMessageEndpointFactory(), spec);
            p.setNMessagesToSend(150);
            p.clearQ1Q2Q3();
            p.setTimeout(300000);
            p.passFromQ1ToQ2();
            ra.endpointDeactivation(c.getMessageEndpointFactory(), spec);
            p.get(p.getQueue1Name()).assertEmpty();
            assertNoAsyncErrors();
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
        Passthrough p = getJMSProvider().createPassthrough(mServerProperties);

        // RA
        RASTCMSResourceAdapter ra = new RASTCMSResourceAdapter();
        ra.setConnectionURL(getConnectionURL());
        ra.setUserName(getJMSProvider().getUserName(mServerProperties));
        ra.setPassword(getJMSProvider().getPassword(mServerProperties));

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
            MockContainer c = new MockContainer(mdb);
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
        Passthrough p = getJMSProvider().createPassthrough(mServerProperties);

        // RA
        RASTCMSResourceAdapter ra = new RASTCMSResourceAdapter();
        ra.setConnectionURL(getConnectionURL());
        ra.setUserName(getJMSProvider().getUserName(mServerProperties));
        ra.setPassword(getJMSProvider().getPassword(mServerProperties));

        // Spec
        RASTCMSActivationSpec spec = new RASTCMSActivationSpec();
        spec.setUserName(p.getUserid());
        spec.setPassword(p.getPassword());
        spec.setConcurrencyMode("sync");
        spec.setDestinationType(javax.jms.Queue.class.getName());
        spec.setDestination(p.getQueue1Name());
        spec.setBatchSize("5");
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
            MockContainer c = new MockContainer(mdb);
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
