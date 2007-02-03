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

import com.stc.jmsjca.core.XMCFQueueXA;
import com.stc.jmsjca.core.XMCFTopicXA;
import com.stc.jmsjca.core.XMCFUnifiedXA;
import com.stc.jmsjca.core.XManagedConnectionFactory;
import com.stc.jmsjca.sunone.RASunOneResourceAdapter;
import com.stc.jmsjca.sunone.SunOneUrlParser;
import com.stc.jmsjca.test.core.XTestBase;
import com.stc.jmsjca.util.Semaphore;

import javax.jms.BytesMessage;
import javax.jms.Connection;
import javax.jms.ConnectionConsumer;
import javax.jms.ConnectionFactory;
import javax.jms.Destination;
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
import javax.jms.ServerSession;
import javax.jms.ServerSessionPool;
import javax.jms.Session;
import javax.jms.StreamMessage;
import javax.jms.TemporaryQueue;
import javax.jms.TemporaryTopic;
import javax.jms.TextMessage;
import javax.jms.Topic;
import javax.jms.TopicConnection;
import javax.jms.TopicConnectionFactory;
import javax.jms.TopicSubscriber;
import javax.jms.XAQueueConnection;
import javax.jms.XAQueueSession;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.transaction.xa.XAException;
import javax.transaction.xa.XAResource;
import javax.transaction.xa.Xid;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * <code>
 * Unit tests
*  See Base
 *
 * For Eclipese, if the above properties are used, the current directory needs to set 
 * to ${workspace_loc:e-jmsjca/build}
 *
 * @author 
 * @version 1.0
 */
public class TestSunOneJUStd extends XTestBase {
    Logger sLog = Logger.getLogger(TestSunOneJUStd.class.getName());

    /**
     * Constructor
     */
    public TestSunOneJUStd() {
        this(null);
    }

    /**
     * Constructor
     *
     * @param name junit test name
     */
    public TestSunOneJUStd(String name) {
        super(name);
    }
    
	public String getConnectionUrl() {
        return "mq://" + mServerProperties.getProperty("host") + ":"
            + mServerProperties.getProperty("stcms.instance.port");
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

    
    public void init(boolean producerPooling) throws Throwable {
        InitialContext ctx = getContext();

        // Create concreate connection factories and bind them into JNDI
        {
            // Create a concrete provider factory (will be used by the managed connection factory)
            SunOneUrlParser urlParser = new SunOneUrlParser(getConnectionUrl());
            
            com.sun.messaging.XAQueueConnectionFactory xaQueueConnectionFactory = new com.sun.messaging.XAQueueConnectionFactory();
            xaQueueConnectionFactory.setProperty("imqAddressList", urlParser.getSunOneUrlSet());
            
            com.sun.messaging.XATopicConnectionFactory xaTopicConnectionFactory = new com.sun.messaging.XATopicConnectionFactory();
            xaTopicConnectionFactory.setProperty("imqAddressList", urlParser.getSunOneUrlSet());
            
            com.sun.messaging.XAConnectionFactory connectionFactory = new com.sun.messaging.XAConnectionFactory();
            connectionFactory.setProperty("imqAddressList", urlParser.getSunOneUrlSet());
            
            ctx.rebind(jndinameQueueDelegateXA, xaQueueConnectionFactory);
            ctx.rebind(jndinameTopicDelegateXA, xaTopicConnectionFactory);
            ctx.rebind(jndinameUnifiedDelegateXA, connectionFactory);
        }

        // Create MCFs, get the CF that will be used by the application and bind that
        // CF into JNDI
        {
            // QUEUE
            XManagedConnectionFactory x = new XMCFQueueXA();
            RASunOneResourceAdapter ra = new RASunOneResourceAdapter();
            ra.setConnectionURL(getConnectionUrl());
            x.setResourceAdapter(ra);
            x.setProducerPooling(Boolean.toString(producerPooling));
            QueueConnectionFactory f = (QueueConnectionFactory) x.createConnectionFactory();
            ctx.rebind(appjndiQueue, f);
        }


        {
            // TOPIC
            XManagedConnectionFactory x = new XMCFTopicXA();
            RASunOneResourceAdapter ra = new RASunOneResourceAdapter();
            ra.setConnectionURL(getConnectionUrl());
            x.setResourceAdapter(ra);
            x.setProducerPooling(Boolean.toString(producerPooling));
            TopicConnectionFactory f = (TopicConnectionFactory) x.createConnectionFactory();
            ctx.rebind(appjndiTopic, f);
        }


        {
            // UNIFIED
            XManagedConnectionFactory x = new XMCFUnifiedXA();
            RASunOneResourceAdapter ra = new RASunOneResourceAdapter();
            ra.setConnectionURL(getConnectionUrl());
            x.setResourceAdapter(ra);
            x.setProducerPooling(Boolean.toString(producerPooling));
            ConnectionFactory f = (ConnectionFactory) x.createConnectionFactory();
            ctx.rebind(appjndiUnified, f);
        }
    }

    private static long sTime = System.currentTimeMillis();
    private static long sUniquifier;

    protected void setClientID(Connection con) throws JMSException {
        con.setClientID("X");
    }
    /**
     * Generates a unique name
     *
     * @return name
     */
    public String generateName() {
        synchronized (TestSunOneJUStd.class) {
            return "JMSJCA" + sTime + sUniquifier++;
        }
    }

    public WireCount getConnectionCount() {
        return new WireCount() {
            public void check(int sessions, int producers, int consumers) {
            }

            public void check(int n) {
            }
        };
    }
    
    public static final class XXid implements Xid, Serializable {
        static long counter = 0;
        private int formatId = 987654;
        private byte[] branchQualifier;
        private byte[] globalTransactionId;

        static synchronized long incrCounter() {
            return ++counter;
        }

        public XXid() {
            branchQualifier = new byte[8];
            long uid = (System.currentTimeMillis() << 12) + (++counter & 0xFFF);
            for (int i = 0; i < 8; i++) {
                branchQualifier[i] = (byte) uid;
                uid >>= 8;
            }
            globalTransactionId = branchQualifier;
        }
        public byte[] getGlobalTransactionId() {
            return globalTransactionId;
        }
        public void setGlobalTransactionId(byte[] value) {
            globalTransactionId = value;
        }
        public byte[] getBranchQualifier() {
            return branchQualifier;
        }
        public void setBranchQualifier(byte[] value) {
            branchQualifier = value;
        }
        public int getFormatId() {
            return formatId;
        }
        public void setFormatId(int value) {
            formatId = value;
        }
        public int hashCode() {
            int result = 0;
            for (int i = 0; i < branchQualifier.length; i++) {
                result += (result << 3) + branchQualifier[i];
            }
            for (int i = 0; i < globalTransactionId.length; i++) {
                result += (result << 3) + globalTransactionId[i];
            }
            return result;
        }
        public boolean equals(Object that) {
            if (this == that) {
                return true;
            }
            if (!(that instanceof XXid)) {
                return false;
            }
            XXid thatXid = (XXid) that;
            for (int i = 0; i < branchQualifier.length; i++) {
                if (branchQualifier[i] != thatXid.branchQualifier[i]) {
                    return false;
                }
            }
            for (int i = 0; i < globalTransactionId.length; i++) {
                if (globalTransactionId[i] != thatXid.globalTransactionId[i]) {
                    return false;
                }
            }
            return true;
        }
    }

    public class XACCQueueSession implements XAQueueSession {
        XAQueueSession delegate;

        public XACCQueueSession(XAQueueSession delegate) {
            this.delegate = delegate;
        }
        
        public void close() throws JMSException {
            delegate.close();
        }

        public void commit() throws JMSException {
            delegate.commit();
        }

        public QueueBrowser createBrowser(Queue arg0, String arg1) throws JMSException {
            return delegate.createBrowser(arg0, arg1);
        }

        public QueueBrowser createBrowser(Queue arg0) throws JMSException {
            return delegate.createBrowser(arg0);
        }

        public BytesMessage createBytesMessage() throws JMSException {
            return delegate.createBytesMessage();
        }

        public MessageConsumer createConsumer(Destination arg0, String arg1, boolean arg2) throws JMSException {
            return delegate.createConsumer(arg0, arg1, arg2);
        }

        public MessageConsumer createConsumer(Destination arg0, String arg1) throws JMSException {
            return delegate.createConsumer(arg0, arg1);
        }

        public MessageConsumer createConsumer(Destination arg0) throws JMSException {
            return delegate.createConsumer(arg0);
        }

        public TopicSubscriber createDurableSubscriber(Topic arg0, String arg1, String arg2, boolean arg3) throws JMSException {
            return delegate.createDurableSubscriber(arg0, arg1, arg2, arg3);
        }

        public TopicSubscriber createDurableSubscriber(Topic arg0, String arg1) throws JMSException {
            return delegate.createDurableSubscriber(arg0, arg1);
        }

        public MapMessage createMapMessage() throws JMSException {
            return delegate.createMapMessage();
        }

        public Message createMessage() throws JMSException {
            return delegate.createMessage();
        }

        public ObjectMessage createObjectMessage() throws JMSException {
            return delegate.createObjectMessage();
        }

        public ObjectMessage createObjectMessage(Serializable arg0) throws JMSException {
            return delegate.createObjectMessage(arg0);
        }

        public MessageProducer createProducer(Destination arg0) throws JMSException {
            return delegate.createProducer(arg0);
        }

        public Queue createQueue(String arg0) throws JMSException {
            return delegate.createQueue(arg0);
        }

        public StreamMessage createStreamMessage() throws JMSException {
            return delegate.createStreamMessage();
        }

        public TemporaryQueue createTemporaryQueue() throws JMSException {
            return delegate.createTemporaryQueue();
        }

        public TemporaryTopic createTemporaryTopic() throws JMSException {
            return delegate.createTemporaryTopic();
        }

        public TextMessage createTextMessage() throws JMSException {
            return delegate.createTextMessage();
        }

        public TextMessage createTextMessage(String arg0) throws JMSException {
            return delegate.createTextMessage(arg0);
        }

        public Topic createTopic(String arg0) throws JMSException {
            return delegate.createTopic(arg0);
        }

        public int getAcknowledgeMode() throws JMSException {
            return delegate.getAcknowledgeMode();
        }

        public MessageListener getMessageListener() throws JMSException {
            return delegate.getMessageListener();
        }

        public QueueSession getQueueSession() throws JMSException {
            return delegate.getQueueSession();
        }

        public Session getSession() throws JMSException {
            return delegate.getSession();
        }

        public boolean getTransacted() throws JMSException {
            return delegate.getTransacted();
        }

        public XAResource getXAResource() {
            return delegate.getXAResource();
        }

        public void recover() throws JMSException {
            delegate.recover();
        }

        public void rollback() throws JMSException {
            delegate.rollback();
        }

        public void run() {
            delegate.run();
        }

        public void setMessageListener(MessageListener arg0) throws JMSException {
            delegate.setMessageListener(arg0);
        }

        public void unsubscribe(String arg0) throws JMSException {
            delegate.unsubscribe(arg0);
        }

    }
    
    public void testXACC() throws Throwable {
        // Connection factory
        SunOneUrlParser urlParser = new SunOneUrlParser(getConnectionUrl());
        com.sun.messaging.XAQueueConnectionFactory fact = new com.sun.messaging.XAQueueConnectionFactory();
        fact.setProperty("imqAddressList", urlParser.getSunOneUrlSet());
        
        // Send a msg to Queue1
        Queue dest;
        {
            final QueueConnection conn = fact.createQueueConnection(USERID, PASSWORD);
            QueueSession session = conn.createQueueSession(false, Session.AUTO_ACKNOWLEDGE);
            dest = session.createQueue("Queue1");
            QueueSender producer = session.createSender(dest);
            TextMessage msg1 = session.createTextMessage("Msg 1 for Q1");
            producer.send(msg1);
            conn.close();
        }
        
        // Define msg listener; notifies a semaphore to notify the main thread
        final Semaphore sync = new Semaphore(0);

        // Define server session pool; each invokation will create a new session
        final XAQueueConnection mdbconn = fact.createXAQueueConnection(USERID, PASSWORD);
        ServerSessionPool pool = new ServerSessionPool() {
            public ServerSession getServerSession() throws JMSException {
                return new ServerSession() {
                    XAQueueSession xs;
                    XXid xid;
                    public Session getSession() throws JMSException {
                        try {
                            xs = mdbconn.createXAQueueSession();
                            xs.getQueueSession().setMessageListener(new MessageListener() {
                                public void onMessage(Message msg) {
                                    // Do real work
                                    sLog.info("Message received in CC Listener (application)");
                                    sync.release();
                                }
                            });
                            return xs;
                        } catch (Exception e) {
                            sLog.log(Level.SEVERE, "Unexpected in getSession(): " + e, e);;
                            throw new JMSException("Error: " + e);
                        } 
                    }
                    public void start() throws JMSException {
                        new Thread() {
                            public void run() {
                                try {
                                    xid = new XXid();
                                    xs.getXAResource().start(xid, XAResource.TMNOFLAGS);
                                    xs.run();
                                    xs.getXAResource().end(xid, XAResource.TMSUCCESS);
                                    xs.getXAResource().commit(xid, true);
                                    xs.close();
                                } catch (Exception e) {
                                    sLog.log(Level.SEVERE, "Unexpected " + e, e);
                                }
                            }
                        }.start();
                    }
                };
            }
        };

        // Create connection consumer and start
        mdbconn.createConnectionConsumer(dest, null, pool, 1);
        mdbconn.start();
        assertTrue(sync.attempt(2000));
        Thread.sleep(5000);
        mdbconn.close();
    }
    
    public void testNonXACC() throws Throwable {
        // Connection factory
        SunOneUrlParser urlParser = new SunOneUrlParser(getConnectionUrl());
        com.sun.messaging.XAQueueConnectionFactory fact = new com.sun.messaging.XAQueueConnectionFactory();
        fact.setProperty("imqAddressList", urlParser.getSunOneUrlSet());
        
        // Send a msg to Queue1
        Queue dest;
        {
            final QueueConnection conn = fact.createQueueConnection(USERID, PASSWORD);
            QueueSession session = conn.createQueueSession(false, Session.AUTO_ACKNOWLEDGE);
            dest = session.createQueue("Queue1");
            QueueSender producer = session.createSender(dest);
            TextMessage msg1 = session.createTextMessage("Msg 1 for Q1");
            producer.send(msg1);
            conn.close();
        }
        
        // Define msg listener; notifies a semaphore to notify the main thread
        final Semaphore sync = new Semaphore(0);
        final MessageListener msgListener = new MessageListener() {
            public void onMessage(Message msg) {
                sLog.info("Message received in CC Listener (application)");
                sync.release();
            }
        };

        // Define server session pool; each invokation will create a new session
        final QueueConnection mdbconn = fact.createQueueConnection(USERID, PASSWORD);
        ServerSessionPool pool = new ServerSessionPool() {
            public ServerSession getServerSession() throws JMSException {
                return new ServerSession() {
                    Session xs;
                    public Session getSession() throws JMSException {
                        xs = mdbconn.createQueueSession(false, Session.AUTO_ACKNOWLEDGE);
                        xs.setMessageListener(msgListener);
                        return xs;
                    }
                    public void start() throws JMSException {
                        new Thread() {
                            public void run() {
                                try {
                                    xs.run();
                                    xs.close();
                                } catch (Exception e) {
                                    sLog.log(Level.SEVERE, "Unexpected " + e, e);
                                }
                            }
                        }.start();
                    }
                };
            }
        };

        // Create connection consumer and start
        mdbconn.createConnectionConsumer(dest, null, pool, 1);
        mdbconn.start();
        assertTrue(sync.attempt(2000));
        mdbconn.close();
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
     * Purpose: Should be able to create multiple connections with the 
     * same clientID, but only one can be active at the same time. 
     * THEY HAVE TO BE OF THE SAME TYPE 
     *
     * @throws Throwable on failure of the test
     */
    public void testClientIDFail() throws Throwable {
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
    
    /**
     * JMQ does not throw an exception on msg.acknowledge if the msg was received
     * on an xasession outside of an xatransaction
     */
    public void disabled_testCtsFailure4() {
        
    }
    
    /**
     * Behaves as a counting semaphore: allows one thread to wait for the counter
     * to reach a particular value
     * 
     * @author fkieviet
     */
    private static class ConditionVar {
        private int mCount;
        private int mMaxSeen;
        private int mTriggerAt;
       
        /**
         * Increments the counter
         */
        public synchronized void inc() {
            mCount++;
            if (mCount > mMaxSeen) {
                mMaxSeen = mCount;
            }
            if (mTriggerAt == mCount) {
                this.notifyAll();
            }
        }
        
        /**
         * Increments the counter
         */
        public synchronized void dec() {
            mCount--;
            if (mTriggerAt == mCount) {
                this.notifyAll();
            }
        }
        
        /**
         * @return current value of counter
         */
        public synchronized int current() {
            return mCount;
        }
        
        /**
         * @return maximum value seen
         */
        public synchronized int getMax() {
            return mMaxSeen;
        }
        
        /**
         * Blocks until the specified counter value has been reached
         * 
         * @param value block until value reached
         * @param timeout max wait time
         * @throws Exception on failure
         */
        public synchronized void waitForDown(int value, long timeout) throws Exception {
            mTriggerAt = value;
            for (;;) {
                this.wait(timeout);
                if (mTriggerAt <= mCount) {
                    break;
                } else {
                    throw new Exception("Timeout");
                }
            }
        }

        /**
         * Blocks until the specified counter value has been reached
         * 
         * @param value block until value reached
         * @param timeout max wait time
         * @throws Exception on failure
         */
        public synchronized void waitForUp(int value, long timeout) throws Exception {
            mTriggerAt = value;
            for (;;) {
                if (mCount >= mTriggerAt) {
                    break;
                }
                this.wait(timeout);
                if (mCount >= mTriggerAt) {
                    break;
                } else {
                    throw new Exception("Timeout");
                }
            }
        }
    }

    /**
     * Asserts that a close() or a stop() on a connection with a connection consumer
     * immediately stops the flow of messages
     * 
     * @param testStop
     * @throws Throwable
     */
    public int doTestXACCStopClose(boolean testStop, final long onMsgDelay) throws Throwable {
        // Connection factory
        SunOneUrlParser urlParser = new SunOneUrlParser(getConnectionUrl());
        com.sun.messaging.XAQueueConnectionFactory fact = new com.sun.messaging.XAQueueConnectionFactory();
        fact.setProperty("imqAddressList", urlParser.getSunOneUrlSet());
        
        // Msgs to process
        final int NMESSAGES = 1000;
        
        // Assure that Queue1 is empty
        {
            final QueueConnection conn = fact.createQueueConnection(USERID, PASSWORD);
            conn.start();
            QueueSession session = conn.createQueueSession(false, Session.AUTO_ACKNOWLEDGE);
            QueueReceiver r = session.createReceiver(session.createQueue("Queue1"));
            int nDrained = 0;
            for (;;) {
                Message m = r.receive(250);
                if (m == null) {
                    break;
                }
                nDrained++;
            }
            System.out.println(nDrained + " msgs drained");
            conn.close();
        }
        
        // Populate Queue1
        Queue dest;
        {
            final QueueConnection conn = fact.createQueueConnection(USERID, PASSWORD);
            QueueSession session = conn.createQueueSession(false, Session.AUTO_ACKNOWLEDGE);
            dest = session.createQueue("Queue1");
            QueueSender producer = session.createSender(dest);
            for (int i = 0; i < NMESSAGES; i++) {
                TextMessage msg1 = session.createTextMessage("Msg 1 for Q1");
                producer.send(msg1);
            }
            conn.close();
        }
        
        // Keeps track of how many msgs were received
        final ConditionVar sync = new ConditionVar();
        final ConditionVar inUse = new ConditionVar();
        final ConditionVar isDead = new ConditionVar();

        // Define server session pool: this is a fixed sized pool with precreated 
        // sessions and blocking behavior
        final XAQueueConnection mdbconn = fact.createXAQueueConnection(USERID, PASSWORD);
        ServerSessionPool pool = new ServerSessionPool() {
            final int POOLSIZE = 32;
            Semaphore mInPool = new Semaphore(POOLSIZE);
            ArrayList mPool = new ArrayList(POOLSIZE);
            boolean mInitialized;
            
            /**
             * Populates the pool
             */
            synchronized void init() {
                if (!mInitialized) {
                    for (int i = 0; i < POOLSIZE; i++) {
                        mPool.add(newServerSession());
                    }
                    mInitialized = true;
                }
            }
            
            /**
             * @return a new server session
             */
            ServerSession newServerSession() {
                return new ServerSession() {
                    XAQueueSession xs;
                    XXid xid;
                    ServerSession xthis = this;
                    
                    /**
                     * Creates a new JMS session
                     * 
                     * @see javax.jms.ServerSession#getSession()
                     */
                    public Session getSession() throws JMSException {
                        try {
                            if (xs == null) {
                                xs = mdbconn.createXAQueueSession();
                                xs.getQueueSession().setMessageListener(new MessageListener() {
                                    public void onMessage(Message msg) {
                                        // Do real work
                                        sync.inc();
                                        
                                        if (onMsgDelay != 0) {
                                            try {
                                                Thread.sleep(onMsgDelay);
                                            } catch (InterruptedException e) {
                                                // ignore
                                            }
                                        }
                                    }
                                });
                            }
                            return xs;
                        } catch (Exception e) {
                            sLog.log(Level.SEVERE, "Unexpected in getSession(): " + e, e);;
                            throw new JMSException("Error: " + e);
                        } 
                    }

                    /**
                     * Called by the JMS client runtime to indicate the container should
                     * process the messages. By lack of a container, this creates a new
                     * thread that does the processing.
                     * 
                     * @see javax.jms.ServerSession#start()
                     */
                    public void start() throws JMSException {
                        new Thread() {
                            /**
                             * Processes the msgs; handles XA
                             * 
                             * @see java.lang.Runnable#run()
                             */
                            public void run() {
                                try {
//                                    /////////////
//                                    try {
//                                        Thread.sleep(20);
//                                    } catch (InterruptedException e1) {
//                                        e1.printStackTrace();
//                                    }
//                                    ////////////
                                    xid = new XXid();
                                    xs.getXAResource().start(xid, XAResource.TMNOFLAGS);
                                    xs.run();
                                    xs.getXAResource().end(xid, XAResource.TMSUCCESS);
                                    xs.getXAResource().commit(xid, true);
                                    returnToPool(xthis);
                                } catch (Exception e) {
                                    sLog.log(Level.SEVERE, "Unexpected " + e, e);
                                }
                            }
                        }.start();
                    }
                };
            }
            
            /**
             * @see javax.jms.ServerSessionPool#getServerSession()
             */
            public ServerSession getServerSession() throws JMSException {
                ///////////
                synchronized (this) {
                    if (isDead.current() == 1) {
                        throw new JMSException("Consumer has already shut down");
                    }
                    inUse.inc();
                }
                ///////////

                init();
                try {
                    if (!mInPool.attempt(60000)) {
                        inUse.dec();
                        throw new Exception("Timeout");
                    }
                } catch (Exception e) {
                    throw new JMSException("getServerSession failure: " + e);
                }
                synchronized (this) {
                    return (ServerSession) mPool.remove(mPool.size() - 1);
                }
            }
            
            /**
             * Returns a server session to the pool
             * 
             * @param s ServerSession
             */
            void returnToPool(ServerSession s) {
                synchronized(this) {
                    mPool.add(s);
                }
                mInPool.release();
                inUse.dec();
            }
        };

        // ---- TEST STARTS HERE ----
        ConnectionConsumer ccx = mdbconn.createConnectionConsumer(dest, null, pool, 1);
        mdbconn.start();
        
        // Wait until message processing is fully going
        sync.waitForUp(NMESSAGES / 2, 30000);
        
        // Stop() and close() should work, and should immediately stop the message flow
        if (testStop) {
            ccx.close();
            isDead.inc();
            inUse.waitForDown(0, 3000);
//            mdbconn.stop();
        } else {
            mdbconn.close();
        }
        
        // Check how many messages were processed when stop() or close() returned control
        // to the caller. Note that this there's still a time window between stop/close
        // in which messages may be processed that will go unnoticed
        int nReceivedAfterStopped = sync.current();
        
        // Wait a bit
        Thread.sleep(500);
        
        // There should be no messages that have been processed since stop() and close()
        // returned.
        int nReceivedAfterStoppedLater = sync.current();
        System.out.println("Received after stopped: " + nReceivedAfterStopped + "; later: " + nReceivedAfterStoppedLater);

        // Close connection (this should NOT cause any exceptions in the log)
        mdbconn.close();

        // Drain remaining messages
        int nDrained = 0;
        {
            final QueueConnection conn = fact.createQueueConnection(USERID, PASSWORD);
            conn.start();
            QueueSession session = conn.createQueueSession(false, Session.AUTO_ACKNOWLEDGE);
            QueueReceiver r = session.createReceiver(session.createQueue("Queue1"));
            for (;;) {
                Message m = r.receive(250);
                if (m == null) {
                    break;
                }
                nDrained++;
            }
            System.out.println(nDrained + " msgs drained");
            conn.close();
        }
        
        assertTrue("Received after stopped: " + nReceivedAfterStoppedLater + "; drained: " + nDrained, 
            nReceivedAfterStoppedLater + nDrained == NMESSAGES);

        assertTrue("Received after stopped: " + nReceivedAfterStopped + "; later: " + nReceivedAfterStoppedLater, 
            nReceivedAfterStopped == nReceivedAfterStoppedLater);
        
        return inUse.getMax();
    }
    
    /**
     * Checks that stop() immediately stops the flow of messages to the connection 
     * consumer
     * 
     * @throws Throwable
     */
    public void testXACCStop() throws Throwable {
        // Note that since this is a timing problem, it may be necessary to execute
        // the test a number of times to see the failure
        for (int i = 0; i < 3; i++) {
            doTestXACCStopClose(true, 0);
        }
    }

    /**
     * Max concurrency
     * 
     * @throws Throwable
     */
    public void testXACCMaxConc() throws Throwable {
        int n = doTestXACCStopClose(true, 100);
        System.out.println("xxxxxxxxxxxxxxxxxxx n = " + n);
        assertTrue(n > 30);
    }

    /**
     * Checks that a connection consumer connection can be closed during message
     * processing
     * 
     * @throws Throwable
     */
    public void __testXACCClose() throws Throwable {
        doTestXACCStopClose(false, 0);
    }

    /**
     * Asserts that a close() or a stop() on a connection with a message listener
     * immediately stops the flow of messages
     * 
     * @param testStop
     * @throws Throwable
     */
    public void doTestXAStopClose(boolean testStop) throws Throwable {
        // Connection factory
        SunOneUrlParser urlParser = new SunOneUrlParser(getConnectionUrl());
        com.sun.messaging.XAQueueConnectionFactory fact = new com.sun.messaging.XAQueueConnectionFactory();
        fact.setProperty("imqAddressList", urlParser.getSunOneUrlSet());
        
        // Msgs to process
        final int NMESSAGES = 1000;
        
        // Assure that Queue1 is empty
        {
            final QueueConnection conn = fact.createQueueConnection(USERID, PASSWORD);
            conn.start();
            QueueSession session = conn.createQueueSession(false, Session.AUTO_ACKNOWLEDGE);
            QueueReceiver r = session.createReceiver(session.createQueue("Queue1"));
            int nDrained = 0;
            for (;;) {
                Message m = r.receive(250);
                if (m == null) {
                    break;
                }
                nDrained++;
            }
            System.out.println(nDrained + " msgs drained");
            conn.close();
        }
        
        // Populate Queue1
        Queue dest;
        {
            final QueueConnection conn = fact.createQueueConnection(USERID, PASSWORD);
            QueueSession session = conn.createQueueSession(false, Session.AUTO_ACKNOWLEDGE);
            dest = session.createQueue("Queue1");
            QueueSender producer = session.createSender(dest);
            for (int i = 0; i < NMESSAGES; i++) {
                TextMessage msg1 = session.createTextMessage("Msg 1 for Q1");
                producer.send(msg1);
            }
            conn.close();
        }
        
        // Keeps track of how many msgs were received
        final ConditionVar sync = new ConditionVar();

        // Define server session pool: this is a fixed sized pool with precreated 
        // sessions and blocking behavior
        final XAQueueConnection mdbconn = fact.createXAQueueConnection(USERID, PASSWORD);
        final XAQueueSession xs = mdbconn.createXAQueueSession();
        QueueReceiver recv = xs.getQueueSession().createReceiver(dest);
        recv.setMessageListener(new MessageListener() {

            public void onMessage(Message arg0) {
                XXid xid;
                try {
                    xid = new XXid();
                    xs.getXAResource().start(xid, XAResource.TMNOFLAGS);
                    sync.inc();
                    xs.getXAResource().end(xid, XAResource.TMSUCCESS);
                    xs.getXAResource().commit(xid, true);
                } catch (Exception e) {
                    sLog.log(Level.SEVERE, "Unexpected in onMessage(): " + e, e);;
                    throw new RuntimeException("Error: " + e);
                } 
            }
            
        });
        
        // ---- TEST STARTS HERE ----
        mdbconn.start();
        
        // Wait until message processing is fully going
        sync.waitForUp(NMESSAGES / 2, 30000);
        
        // Stop() and close() should work, and should immediately stop the message flow
        if (testStop) {
            mdbconn.stop();
        } else {
            mdbconn.close();
        }
        
        // Check how many messages were processed when stop() or close() returned control
        // to the caller. Note that this there's still a time window between stop/close
        // in which messages may be processed that will go unnoticed
        int nReceivedAfterStopped = sync.current();
        
        // Wait a bit
        Thread.sleep(500);
        
        // There should be no messages that have been processed since stop() and close()
        // returned.
        int nReceivedAfterStoppedLater = sync.current();
        System.out.println("Received after stopped: " + nReceivedAfterStopped + "; later: " + nReceivedAfterStoppedLater);

        // Close connection (this should NOT cause any exceptions in the log)
        mdbconn.close();

        // Drain remaining messages
        int nDrained = 0;
        {
            final QueueConnection conn = fact.createQueueConnection(USERID, PASSWORD);
            conn.start();
            QueueSession session = conn.createQueueSession(false, Session.AUTO_ACKNOWLEDGE);
            QueueReceiver r = session.createReceiver(session.createQueue("Queue1"));
            for (;;) {
                Message m = r.receive(250);
                if (m == null) {
                    break;
                }
                nDrained++;
            }
            System.out.println(nDrained + " msgs drained");
            conn.close();
        }
        
        assertTrue("Received after stopped: " + nReceivedAfterStoppedLater + "; drained: " + nDrained, 
            nReceivedAfterStoppedLater + nDrained == NMESSAGES);

        assertTrue("Received after stopped: " + nReceivedAfterStopped + "; later: " + nReceivedAfterStoppedLater, 
            nReceivedAfterStopped == nReceivedAfterStoppedLater);
    }

    /**
     * Checks that stop() immediately stops the flow of messages to the serial listener
     * 
     * @throws Throwable
     */
    public void __testXASerialStop() throws Throwable {
        // Note that since this is a timing problem, it may be necessary to execute
        // the test a number of times to see the failure
        for (int i = 0; i < 10; i++) {
            doTestXAStopClose(true);
        }
    }

    /**
     * Checks that a connection consumer connection can be closed during message
     * processing
     * 
     * @throws Throwable
     */
    public void __testXASerialClose() throws Throwable {
        doTestXAStopClose(false);
    }

    /**
     * Implements the stop procedure for XA and connection consumers as proposed by
     * George: close the connection consumer, wait until all server sessions have been
     * returned, and close the connection.
     * 
     * This test tests message loss when going through the shut down procedure while
     * processing messages. Messages are all rolled back five times, and then a wait
     * is invoked.
     * 
     * @param testStop
     * @throws Throwable
     */
    public int doTestXACCStopCloseRolback() throws Throwable {
        final int POOLSIZE = 32;

        // Connection factory
        SunOneUrlParser urlParser = new SunOneUrlParser(getConnectionUrl());
        com.sun.messaging.XAQueueConnectionFactory fact = new com.sun.messaging.XAQueueConnectionFactory();
        fact.setProperty("imqAddressList", urlParser.getSunOneUrlSet());
        
        // Msgs to process
        final int NMESSAGES = 100;
        
        // Assure that Queue1 is empty
        {
            final QueueConnection conn = fact.createQueueConnection(USERID, PASSWORD);
            conn.start();
            QueueSession session = conn.createQueueSession(false, Session.AUTO_ACKNOWLEDGE);
            QueueReceiver r = session.createReceiver(session.createQueue("Queue1"));
            int nDrained = 0;
            for (;;) {
                Message m = r.receive(250);
                if (m == null) {
                    break;
                }
                nDrained++;
            }
            System.out.println(nDrained + " msgs drained");
            conn.close();
        }
        
        // Populate Queue1
        Queue dest;
        {
            final QueueConnection conn = fact.createQueueConnection(USERID, PASSWORD);
            QueueSession session = conn.createQueueSession(false, Session.AUTO_ACKNOWLEDGE);
            dest = session.createQueue("Queue1");
            QueueSender producer = session.createSender(dest);
            for (int i = 0; i < NMESSAGES; i++) {
                TextMessage msg1 = session.createTextMessage("Msg 1 for Q1");
                producer.send(msg1);
            }
            conn.close();
        }
        
        // Keeps track of how many msgs were received
        final ConditionVar inUse = new ConditionVar();
        final ConditionVar isDead = new ConditionVar();
        final ConditionVar waiting = new ConditionVar();
        final ConditionVar endWaiter = new ConditionVar();
        final ConditionVar rolledback = new ConditionVar();

        // Define server session pool: this is a fixed sized pool with precreated 
        // sessions and blocking behavior
        final XAQueueConnection mdbconn = fact.createXAQueueConnection(USERID, PASSWORD);
        ServerSessionPool pool = new ServerSessionPool() {
            Semaphore mInPool = new Semaphore(POOLSIZE);
            ArrayList mPool = new ArrayList(POOLSIZE);
            boolean mInitialized;
            
            /**
             * Populates the pool
             */
            synchronized void init() {
                if (!mInitialized) {
                    for (int i = 0; i < POOLSIZE; i++) {
                        mPool.add(newServerSession());
                    }
                    mInitialized = true;
                }
            }
            
            /**
             * @return a new server session
             */
            ServerSession newServerSession() {
                return new ServerSession() {
                    XAQueueSession xs;
                    XXid xid;
                    ServerSession xthis = this;
                    Map msgids = new HashMap();
                    
                    /**
                     * Creates a new JMS session
                     * 
                     * @see javax.jms.ServerSession#getSession()
                     */
                    public Session getSession() throws JMSException {
                        try {
                            if (xs == null) {
                                xs = mdbconn.createXAQueueSession();
                                xs.getQueueSession().setMessageListener(new MessageListener() {
                                    public void onMessage(Message msg) {
                                        try {
                                            xs.getXAResource().end(xid, XAResource.TMSUCCESS);
                                        } catch (XAException e1) {
                                            e1.printStackTrace();
                                        }
                                        
                                        String msgid = null;
                                        try {
                                            msgid = msg.getJMSMessageID();
                                        } catch (JMSException e1) {
                                            e1.printStackTrace();
                                        }
                                        
                                        // Find out if a msg needs to be delayed: a msg
                                        // needs to be delayed if it has been rolled
                                        // back 5 times
                                        int[] cnt;
                                        synchronized (msgids) {
                                            cnt = (int[]) msgids.get(msgid); 
                                        }
                                        if (cnt == null) {
                                            cnt = new int[1];
                                            synchronized (msgids) {
                                                msgids.put(msgid, cnt);
                                            }
                                        }
                                        cnt[0]++;
                                        if (cnt[0] == 5) {
                                            waiting.inc();
                                            try {
                                                endWaiter.waitForUp(1, 30000);
                                            } catch (Exception e) {
                                                e.printStackTrace();
                                            }
                                        }

                                        try {
                                            xs.getXAResource().start(xid, XAResource.TMJOIN);
                                        } catch (XAException e) {
                                            e.printStackTrace();
                                        }
                                    }
                                });
                            }
                            return xs;
                        } catch (Exception e) {
                            sLog.log(Level.SEVERE, "Unexpected in getSession(): " + e, e);;
                            throw new JMSException("Error: " + e);
                        } 
                    }

                    /**
                     * Called by the JMS client runtime to indicate the container should
                     * process the messages. By lack of a container, this creates a new
                     * thread that does the processing.
                     * 
                     * @see javax.jms.ServerSession#start()
                     */
                    public void start() throws JMSException {
                        new Thread() {
                            /**
                             * Processes the msgs; handles XA
                             * 
                             * @see java.lang.Runnable#run()
                             */
                            public void run() {
                                try {
                                    xid = new XXid();
                                    xs.getXAResource().start(xid, XAResource.TMNOFLAGS);
                                    xs.run();
                                    xs.getXAResource().end(xid, XAResource.TMSUCCESS);
                                    xs.getXAResource().rollback(xid);
                                    rolledback.inc();
                                    returnToPool(xthis);
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }
                        }.start();
                    }
                };
            }
            
            /**
             * @see javax.jms.ServerSessionPool#getServerSession()
             */
            public ServerSession getServerSession() throws JMSException {
                synchronized (this) {
                    if (isDead.current() == 1) {
                        throw new JMSException("Consumer has already shut down");
                    }
                    inUse.inc();
                }

                init();
                try {
                    if (!mInPool.attempt(60000)) {
                        inUse.dec();
                        throw new Exception("Timeout");
                    }
                } catch (Exception e) {
                    throw new JMSException("getServerSession failure: " + e);
                }
                synchronized (this) {
                    return (ServerSession) mPool.remove(mPool.size() - 1);
                }
            }
            
            /**
             * Returns a server session to the pool
             * 
             * @param s ServerSession
             */
            void returnToPool(ServerSession s) {
                synchronized(this) {
                    mPool.add(s);
                }
                mInPool.release();
                inUse.dec();
            }
        };

        // ---- TEST STARTS HERE ----
        ConnectionConsumer ccx = mdbconn.createConnectionConsumer(dest, null, pool, 1);
        mdbconn.start();
        
        // Wait until message processing is fully going
        if (false) {
            // The test passes if all the threads are stuck waiting
            waiting.waitForUp(POOLSIZE, 30000);
        } else {
            // The test fails if close() is called while processing
            rolledback.waitForUp(5, 30000);
        }
        System.out.println("All threads are waiting: " + waiting.current());
        
        ccx.close();
        isDead.inc();
        System.out.println("Triggering end");
        endWaiter.inc();
        System.out.println("Waiting for inuse=0");
        inUse.waitForDown(0, 3000);
        System.out.println("Closing connection");
        mdbconn.close();
        System.out.println("Connection closed");
        
        // Check indoubt
        {
            final XAQueueConnection conn = fact.createXAQueueConnection(USERID, PASSWORD);
            conn.start();
            XAQueueSession session = conn.createXAQueueSession();
            Xid[] xids = session.getXAResource().recover(XAResource.TMSTARTRSCAN);
            System.out.println("Recover: " + xids.length);
            conn.close();
        }

        // Drain remaining messages
        int nDrained = 0;
        {
            final QueueConnection conn = fact.createQueueConnection(USERID, PASSWORD);
            conn.start();
            QueueSession session = conn.createQueueSession(false, Session.AUTO_ACKNOWLEDGE);
            QueueReceiver r = session.createReceiver(session.createQueue("Queue1"));
            for (;;) {
                Message m = r.receive(250);
                if (m == null) {
                    break;
                }
                nDrained++;
            }
            System.out.println(nDrained + " msgs drained");
            conn.close();
        }
        
        System.out.println("Rolledback: " + rolledback.current());
        
        assertTrue("Msg found: " + nDrained, nDrained == NMESSAGES);
        
        return inUse.getMax();
    }
    
    public void testXACCStopCloseRolback() throws Throwable {
        doTestXACCStopCloseRolback();
    }
}
