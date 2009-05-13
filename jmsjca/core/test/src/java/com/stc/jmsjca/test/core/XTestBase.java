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

package com.stc.jmsjca.test.core;

import com.stc.jmsjca.core.AdminQueue;
import com.stc.jmsjca.core.Options;
import com.stc.jmsjca.core.TxMgr;
import com.stc.jmsjca.core.XXid;
import com.stc.jmsjca.localization.LocalizedString;
import com.stc.jmsjca.util.Exc;
import com.stc.jmsjca.util.Semaphore;
import com.stc.jmsjca.util.XAssert;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.DeliveryMode;
import javax.jms.InvalidClientIDException;
import javax.jms.JMSException;
import javax.jms.JMSSecurityException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageListener;
import javax.jms.MessageProducer;
import javax.jms.Queue;
import javax.jms.QueueConnection;
import javax.jms.QueueConnectionFactory;
import javax.jms.QueueReceiver;
import javax.jms.QueueSender;
import javax.jms.QueueSession;
import javax.jms.ServerSession;
import javax.jms.ServerSessionPool;
import javax.jms.Session;
import javax.jms.TemporaryTopic;
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
import javax.resource.ResourceException;
import javax.resource.spi.LocalTransaction;
import javax.resource.spi.ManagedConnection;
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
import java.io.InputStream;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * <code>
 * Unit tests
 *
 130q With session pooling turned on, when a connection closes, it needs to apparently close all the sessions; a closed session should behave as such.
 140q With session pooling turned on, sessions are pooled: calling close() on a session does not really close the session
 145q With session pooling turned off, when a session closes, it should close the session
 150q With session pooling turned off, when a connection closes, it needs to close all the sessions.
 160t A session obtained from a pool should have a correct signature
 170q A session will not be returned to the pool if any exception happened in any of the sessions
 200t Calling close on a producer should not close the producer, but return it to the pool
 210t Calling close on a session should not close any producers but return them to the pool
 215t Calling close on a session without producer pooling should close producers
 216t Calling close on a session without session pooling should close producers
 220t Calling close on a connection should not close any producers but return them to the pool
 225t Calling close on a connection w/o session pooling should close all producers
 230t Calling close on a producer without pooling should close it
 240t Calling close on a session without pooling should close the producers
 250t State of producer should be reset on calling close()
 260t Producers on temporary destinations are not cached
 300q Calling close on a pooled session should close the subscribers
 310q Calling close on a non-pooled session should close the subscribers
 320q Calling close on a connection with session pooling should close the subscribers
 330q Calling close on a connection without session pooling should close the subscribers
 500q XA: Normal JMS behavior should still work with Default Connection Manager
 500t XA: Normal JMS behavior should still work with Default Connection Manager
 510t XA: getXAResource should fail on a non-XA connection
 515q XAResource should not be wrapped (isSameRM() should still behave the same)
 518q createSession() after getXAResource() should return the same session
 520q XA: createSession() can be called only once before getXAResource() __OR__ once after getXAResource():
 - this should work: create, getXAResource
 - this should work: getXAResource create
 - this should fail: create, create, getXAResource (fail)
 - this should fail: create, getXAResource, create (fail)
 - this should fail: getXAResource create, create (fail)
 530q XA: w/o session pooling, close() on a session should not close the session if getXAResource() was called
 - this should work: create, getXAResource, close, getXAResource().commit()
 - this should work: getXAResource create, close(), getXAResource().commit()
 - this should fail: create, close(), getXAResource().commit()- this should work: create, close, create, getXAResource
 - this should work: create, getXAResource, close, create; sessions should be the same
 600t if a session is used for CC, it should not be reused
 700q A session will not be returned to the pool if any exception happened
 710q cleanup() should throw an exception if an exception was thrown on the connection, or any of the objects created by the connection.

 - 800q XA should be transparent: XA should be used automatically when getXAResource() is called by container
 - 900q cleanup() should not close session ever
 *
 * </code>
 *
 * @author Frank Kieviet
 * @version 1.0
 */
@SuppressWarnings("unchecked")
abstract public class XTestBase extends BaseTestCase {
    private static Logger sLog = Logger.getLogger(XTestBase.class.getName());


    /**
     * @see com.stc.jmsjca.test.core.BaseTestCase#setUp()
     */
    @Override
    public void setUp() throws Exception {
        super.setUp();
        USERID = getJMSProvider().getUserName(mServerProperties);
        PASSWORD = getJMSProvider().getPassword(mServerProperties);
        TxMgr.setUnitTestTxMgr(new TestTransactionManager());
    }

    /**
     * @see junit.framework.TestCase#tearDown()
     */
    @Override
    public void tearDown() throws Exception {
        super.tearDown();
    }

    /**
     * @return the JMS implementation specific factory object
     */
    public abstract JMSProvider getJMSProvider();

    /**
     * Tool function: ensures that the specified stream is closed
     *
     * @param stream to close; maybe null
     */
    public static void safeClose(InputStream stream) {
        if (stream != null) {
            try {
                stream.close();
            } catch (Exception ex) {
                // ignore
            }
        }
    }
    /**
     * Constructor
     */
    public XTestBase() {
        this(null);
    }

    /**
     * Constructor
     *
     * @param name junit test name
     */
    public XTestBase(String name) {
        super(name);
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

    protected static String appjndiQueue = "test-fact-application-queue";
    protected static String appjndiTopic = "test-fact-application-topic";
    protected static String appjndiUnified = "test-fact-application-unified";

    abstract public void init(boolean producerPooling) throws Throwable;
    
    abstract public XAQueueConnectionFactory getXAQueueConnectionFactory() throws JMSException;
    
    public void init(boolean voidSessionPooling, boolean producerPooling) throws Throwable {
        init(producerPooling);
    }

    public String USERID;
    public String PASSWORD;

    
    protected void setClientID(Connection con) throws JMSException {
        // nothing
    }

//    private static long sTime = System.currentTimeMillis();
//    private static long sUniquifier;
//    /**
//     * Generates a unique name
//     *
//     * @return name
//     */
//    public String generateName() {
//        synchronized (TestSunOneJUStd.class) {
//            return "JMSJCA" + sTime + sUniquifier++;
//        }
//    }

    public String getQueue1Name() throws Throwable {
        String dest = getJMSProvider().createPassthrough(mServerProperties).getQueue1Name();
        clearQueue(dest, -2);
        return dest;
    }
    
    public String getTopic1Name() throws Throwable {
        String dest = getJMSProvider().createPassthrough(mServerProperties).getTopic1Name();
        return dest;
    }
    
    public String getDur1Name(String topicName) throws Throwable {
        String sub = getJMSProvider().createPassthrough(mServerProperties).getDurableTopic1Name1();
        clearTopic(sub, topicName, false);
        return sub;
    }
    
    public String getDur2Name(String topicName) throws Throwable {
        String sub = getJMSProvider().createPassthrough(mServerProperties).getDurableTopic1Name2();
        clearTopic(sub, topicName, false);
        return sub;
    }
    
    public String getDur3Name(String topicName) throws Throwable {
        String sub = getJMSProvider().createPassthrough(mServerProperties).getDurableTopic1Name3();
        clearTopic(sub, topicName, false);
        return sub;
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
     * Purpose: Test connection creation<br>
     * Assertion: Creating a connection should not throw; closing twice is fine;
     * calling other functions on a closed connection should throw.<br>
     * Strategy: create a connection<br>
     *
     * @throws Throwable on failure of the test
     */
    public void test100() throws Throwable {
        init(true);

        // This is how the client would normally create connections
        InitialContext ctx = getContext();
        QueueConnectionFactory f = (QueueConnectionFactory) ctx.lookup(appjndiQueue);
        QueueConnection conn = f.createQueueConnection(USERID, PASSWORD);
        conn.close();

        // Closing twice is fine
        conn.close();

        try {
            conn.start();
            throw new Throwable("Exception not thrown");
        } catch (JMSException e) {
            // ignore
        }

        // Recreate a connection
        conn = f.createQueueConnection(USERID, PASSWORD);
        conn.close();

        // Reauthenticate a connection (should create a new connection)
        conn = f.createQueueConnection(USERID, "X");
        conn.close();

        getConnectionManager(f).clear();
    }

    /**
     * Purpose: Test connection creation<br>
     * Assertion: Creating a connection should not throw; closing twice is fine;
     * calling other functions on a closed connection should throw.<br>
     * Strategy: create a connection<br>
     *
     * @throws Throwable on failure of the test
     */
    public void test101() throws Throwable {
        init(true);

        // This is how the client would normally create connections
        InitialContext ctx = getContext();
        TopicConnectionFactory f = (TopicConnectionFactory) ctx.lookup(appjndiTopic);
        TopicConnection conn = f.createTopicConnection(USERID, PASSWORD);
        conn.close();

        // Closing twice is fine
        conn.close();

        try {
            conn.start();
            throw new Throwable("Exception not thrown");
        } catch (JMSException e) {
            // ignore
        }

        // Recreate a connection
        conn = f.createTopicConnection(USERID, PASSWORD);
        conn.close();

        // Reauthenticate a connection (should create a new connection)
        conn = f.createTopicConnection(USERID, "X");
        conn.close();

        getConnectionManager(f).clear();
    }

    public abstract WireCount getConnectionCount();

    /**
     * Purpose: Test connection creation with additional session creation<br>
     * Assertion: <br>
     * Strategy: <br>
     *
     * @throws Throwable on failure of the test
     */
    public void test110q() throws Throwable {
        init(true);

        // This is how the client would normally create connections
        InitialContext ctx = getContext();
        QueueConnectionFactory f = (QueueConnectionFactory) ctx.lookup(appjndiQueue);
        QueueConnection conn = f.createQueueConnection(USERID, PASSWORD);

        WireCount s0 = getConnectionCount();

        QueueSession s = conn.createQueueSession(true, 0);

        // Force type actuation
        s.getTransacted();

        s0.check(1);

        // Connection shouldn't close
        conn.close();
        s0.check(1);

        // Recreate a connection
        conn = f.createQueueConnection(USERID, PASSWORD);
        s = conn.createQueueSession(true, 0);
        // Force type actuation
        s.getTransacted();

        s0.check(1);
        conn.close();
        s0.check(1);

        getConnectionManager(f).clear();

        s0.check(0);
    }

    /**
     * Purpose: Test connection creation with additional session creation<br>
     * Assertion: <br>
     * Strategy: <br>
     *
     * @throws Throwable on failure of the test
     */
    public void test110qAuth() throws Throwable {
        init(true);

        // This is how the client would normally create connections
        InitialContext ctx = getContext();
        QueueConnectionFactory f = (QueueConnectionFactory) ctx.lookup(appjndiQueue);
        QueueConnection conn = f.createQueueConnection(USERID, PASSWORD);

        WireCount s0 = getConnectionCount();

        QueueSession s = conn.createQueueSession(true, 0);
        s.getTransacted(); // actuate type

        s0.check(1);

        // Connection shouldn't close
        conn.close();
        s0.check(1);

        // Reuse a connection
        conn = f.createQueueConnection(USERID, PASSWORD);
        s = conn.createQueueSession(true, 0);
        s.getTransacted(); // actuate type
        s0.check(1);
        conn.close();

        // Reauthenticate a connection (should create a new connection)
        conn = f.createQueueConnection(USERID, "X3216546151");
        boolean authError;
        try {
            s = conn.createQueueSession(true, 0);
            s.getTransacted(); // actuate type
            authError = false;
        } catch (JMSSecurityException ex) {
            authError = true;
        }

        if (!authError) {
            s0.check(2);
            conn.close();
            s0.check(2);
        }

        getConnectionManager(f).clear();
        s0.check(0);
    }

    /**
     * Purpose: Test connection creation with additional session creation<br>
     * Assertion: <br>
     * Strategy: <br>
     *
     * @throws Throwable on failure of the test
     */
    public void test110t() throws Throwable {
        init(true);

        // This is how the client would normally create connections
        InitialContext ctx = getContext();
        TopicConnectionFactory f = (TopicConnectionFactory) ctx.lookup(appjndiTopic);
        TopicConnection conn = f.createTopicConnection(USERID, PASSWORD);

        WireCount s0 = getConnectionCount();

        TopicSession s = conn.createTopicSession(true, 0);
        s.getTransacted(); // actuate type

        s0.check(1);

        // Connection shouldn't close
        conn.close();
        s0.check(1);

        // Recreate a connection
        conn = f.createTopicConnection(USERID, PASSWORD);
        s = conn.createTopicSession(true, 0);
        s.getTransacted(); // actuate type
        s0.check(1);
        conn.close();

        getConnectionManager(f).clear();
        s0.check(0);
    }

    /**
     * Purpose: Test connection creation with additional session creation<br>
     * Assertion: <br>
     * Strategy: <br>
     *
     * @throws Throwable on failure of the test
     */
    public void test110tAuth() throws Throwable {
        init(true);

        // This is how the client would normally create connections
        InitialContext ctx = getContext();
        TopicConnectionFactory f = (TopicConnectionFactory) ctx.lookup(appjndiTopic);
        TopicConnection conn = f.createTopicConnection(USERID, PASSWORD);

        WireCount s0 = getConnectionCount();

        TopicSession s = conn.createTopicSession(true, 0);
        s.getTransacted(); // actuate type

        s0.check(1);

        // Connection shouldn't close
        conn.close();
        s0.check(1);

        // Recreate a connection
        conn = f.createTopicConnection(USERID, PASSWORD);
        s = conn.createTopicSession(true, 0);
        s.getTransacted(); // actuate type
        s0.check(1);
        conn.close();

        // Reauthenticate a connection (should create a new connection)
        conn = f.createTopicConnection(USERID, "X3216546151");
        boolean authError;
        try {
            s = conn.createTopicSession(true, 0);
            s.getTransacted(); // actuate type
            authError = false;
        } catch (JMSSecurityException ex) {
            authError = true;
        }

        if (!authError) {
            s0.check(2);
            conn.close();
            s0.check(2);
        }

        getConnectionManager(f).clear();
        s0.check(0);
    }

    /**
     * Purpose: Pooling of resources (connection, session, or producer)<br>
     * Assertion: With session pooling turned on, when a connection closes, it needs to
     * apparently close all the sessions; a closed session should behave as such.<br>
     * Strategy: <br>
     *
     * @throws Throwable on failure of the test
     */
    public void test130q() throws Throwable {
        init(true);

        // This is how the client would normally create connections
        InitialContext ctx = getContext();
        QueueConnectionFactory f = (QueueConnectionFactory) ctx.lookup(appjndiQueue);
        QueueConnection conn = f.createQueueConnection(USERID, PASSWORD);

        WireCount s0 = getConnectionCount();

        QueueSession s = conn.createQueueSession(true, 0);
        s.getTransacted(); // actuate type

        s0.check(1);

        // Connection shouldn't close
        conn.close();
        s0.check(1);

        // Session should act like it is closed
        try {
            s.createTemporaryQueue();
            throw new Throwable("Exception expected");
        } catch (JMSException ex1) {
        }

        // Create a new session
        conn = f.createQueueConnection(USERID, PASSWORD);
        s = conn.createQueueSession(true, 0);
        s.getTransacted(); // actuate type
        s0.check(1);
        s.createQueue(getQueue1Name());

        conn.close();

        getConnectionManager(f).clear();
        s0.check(0);
    }

    /**
     * Purpose: Test session pooling<br>
     * Assertion: With session pooling turned on, sessions are pooled: calling close() on
     * a session does not really close the session<br>
     * Strategy: <br>
     *
     * @throws Throwable on failure of the test
     */
    public void test140q() throws Throwable {
        init(true);

        // This is how the client would normally create connections
        InitialContext ctx = getContext();
        QueueConnectionFactory f = (QueueConnectionFactory) ctx.lookup(appjndiQueue);
        QueueConnection conn = f.createQueueConnection(USERID, PASSWORD);

        WireCount s0 = getConnectionCount();

        QueueSession s = conn.createQueueSession(true, 0);
        s.getTransacted(); // actuate type

        s0.check(1);

        // Connection shouldn't close
        s.close();
        s0.check(1);

        // Session should act like it is closed
        try {
            s.createTemporaryQueue();
            throw new Throwable("Exception expected");
        } catch (JMSException ex1) {
        }

        // Create a new session
        s = conn.createQueueSession(true, 0);
        s.getTransacted(); // actuate type
        s0.check(1);
        s.createQueue(getQueue1Name());

        conn.close();
        s0.check(1);

        getConnectionManager(f).clear();
        s0.check(0);
    }

    /**
     * Purpose: Test session pooling<br>
     * Assertion: With session pooling turned off, when a session closes, it should close
     * the session<br>
     * Strategy: <br>
     *
     * CHANGED: SESSIONS ARE ALWAYS POOLED
     *
     * @throws Throwable on failure of the test
     */
    public void test145q() throws Throwable {
        init(false, true);

        // This is how the client would normally create connections
        InitialContext ctx = getContext();
        QueueConnectionFactory f = (QueueConnectionFactory) ctx.lookup(appjndiQueue);
        QueueConnection conn = f.createQueueConnection(USERID, PASSWORD);

        WireCount s0 = getConnectionCount();

        QueueSession s = conn.createQueueSession(true, 0);
        s.getTransacted(); // actuate type

        s0.check(1);

        // Session should not close
        s.close();
        s0.check(1);
        // destroy should close session
        getConnectionManager(f).clear();
        s0.check(0);

        // Session should act like it is closed
        try {
            s.createTemporaryQueue();
            throw new Throwable("Exception expected");
        } catch (JMSException ex1) {
        }

        // Create a new session
        s = conn.createQueueSession(true, 0);
        s.getTransacted(); // actuate type
        s0.check(1);
        s.createQueue(getQueue1Name());

        conn.close();

        s0.check(1);

        // destroy should close session
        getConnectionManager(f).clear();
        s0.check(0);
    }

    /**
     * Purpose: Pooling of resources (connection, session, or producer)<br>
     * Assertion: With session pooling turned off, when a connection closes, it needs to
     * close all the sessions.<br>
     * Strategy: <br>
     *
     * CHANGED: SESSIONS ARE ALWAYS POOLED
     *
     * @throws Throwable on failure of the test
     */
    public void test150q() throws Throwable {
        init(false, true);

        // This is how the client would normally create connections
        InitialContext ctx = getContext();
        QueueConnectionFactory f = (QueueConnectionFactory) ctx.lookup(appjndiQueue);
        QueueConnection conn = f.createQueueConnection(USERID, PASSWORD);

        WireCount s0 = getConnectionCount();

        QueueSession s = conn.createQueueSession(true, 0);
        s.getTransacted(); // actuate type

        s0.check(1);

        // Connection should NOT close
        conn.close();
        s0.check(1);

        // Session should act like it is closed
        try {
            s.createTemporaryQueue();
            throw new Throwable("Exception expected");
        } catch (JMSException ex1) {
        }

        getConnectionManager(f).clear();
        s0.check(0);

        // Create a new session
        conn = f.createQueueConnection(USERID, PASSWORD);
        s = conn.createQueueSession(true, 0);
        s.getTransacted(); // actuate type
        s0.check(1);
        s.createQueue(getQueue1Name());

        conn.close();

        getConnectionManager(f).clear();
        s0.check(0);

        getConnectionManager(f).clear();
        s0.check(0);
    }

    /**
     * Purpose: Pooling of resources (connection, session, or producer)<br>
     * Assertion: A session obtained from a pool should have a correct signature<br>
     * Strategy: <br>
     *
     * INVALID: SESSIONS OF DIFFERENT SIGNATURES WILL BE REUSED
     *
     * @throws Throwable on failure of the test
     */
    public void invalid_test160t() throws Throwable {
        init(true);

        // This is how the client would normally create connections
        InitialContext ctx = getContext();
        TopicConnectionFactory f = (TopicConnectionFactory) ctx.lookup(appjndiTopic);
        TopicConnection conn = f.createTopicConnection(USERID, PASSWORD);

        WireCount s0 = getConnectionCount();

        TopicSession s = conn.createTopicSession(true, 0);
        s.getTransacted(); // actuate type

        s0.check(1);

        // Connection shouldn't close
        conn.close();
        s0.check(1);

        // Recreate a connection
        conn = f.createTopicConnection(USERID, PASSWORD);

        // Create session with different signature
        s = conn.createTopicSession(false, Session.AUTO_ACKNOWLEDGE);
        s.getTransacted(); // actuate type
        s0.check(2);

        // Session with same signature should be cached
        s = conn.createTopicSession(true, 0);
        s.getTransacted(); // actuate type
        s0.check(2);
        conn.close();

        s0.check(2);
        conn.close();
    }

    /**
     * Purpose: Pooling of resources (connection, session, or producer)<br>
     * Assertion: A session obtained from a pool should have a correct signature<br>
     * Strategy: create non-transacted; close; create transacted; same connection
     * should be reused; both sessions should work properly<br>
     *
     * USE SIMULATED INSIDE ACC
     *
     * @throws Throwable on failure of the test
     */
    public void test160tMixInsideACC() throws Throwable {
        init(true);

        // This is how the client would normally create connections
        InitialContext ctx = getContext();
        TopicConnectionFactory f = (TopicConnectionFactory) ctx.lookup(appjndiTopic);
        TopicConnection conn = f.createTopicConnection(USERID, PASSWORD);
        setClientID(conn);

        getRA(f).setOptions(Options.Out.CLIENTCONTAINER + "=true");

        WireCount w = getConnectionCount();
        WireCount w0 = w;
        String destName = getTopic1Name();
        String subname1 = getDur1Name(destName);
        String subname2 = getDur2Name(destName);
        String subname3 = getDur3Name(destName);

        TopicSession s = conn.createTopicSession(true, 0);
        s.getTransacted(); // actuate type
        w.check(1, 0, 0);
        {
            Topic dest = s.createTopic(destName);
            TopicPublisher prod = s.createPublisher(dest);
            w.check(1, 1, 0);
            s.createDurableSubscriber(dest, subname1);
            s.createDurableSubscriber(dest, subname2);
            s.createDurableSubscriber(dest, subname3);
            w.check(1, 1, 3);
            TextMessage m = s.createTextMessage("X");
            prod.send(m);
            s.commit();

            // Connection shouldn't close; including producer
            conn.close();
            w.check(1, 1, 0);
        }

        // Connection shouldn't close; including producer
        conn.close();
        w.check(1, 1, 0);

        // Recreate a connection
        conn = f.createTopicConnection(USERID, PASSWORD);
        
        // Create session with different signature; should create NEW connection
        w = getConnectionCount();

        s = conn.createTopicSession(false, Session.AUTO_ACKNOWLEDGE);
        s.getTransacted(); // actuate type
        s.createTextMessage("void");

        // Producer should have closed when switching types
        w.check(1, 0, 0);

        // Session with same signature should be cached
        {
            Topic dest = s.createTopic(destName);
            TopicPublisher prod = s.createPublisher(dest);
            w.check(1, 1, 0);
            TextMessage m = s.createTextMessage("X");
            prod.publish(m);
            // Uses transacted twin, and producer on there
            w.check(2, 2, 0);

            conn.close();

            // cons should close
            w.check(2, 2, 0);
        }

        // Recreate a connection
        conn = f.createTopicConnection(USERID, PASSWORD);
        
        // Create session with same signature
        s = conn.createTopicSession(false, Session.AUTO_ACKNOWLEDGE);
        s.getTransacted(); // actuate type
        s.createTextMessage("void");

        // Producer should have closed when switching types
        w.check(2, 2, 0);


        // Session with same signature should be cached
        {
            Topic dest = s.createTopic(destName);
            TopicPublisher prod = s.createPublisher(dest); // should reuse publisher!
            w.check(2, 2, 0);
            TextMessage m = s.createTextMessage("X");
            w.check(2, 2, 0);
            prod.publish(m);
            w.check(2, 2, 0);

            // Verify type is autocommit
            try {
                s.commit();
                throw new Throwable("Not thrown");
            } catch (JMSException expected) {
            }

            conn.close();

            // cons should close; with exception should close all
            getConnectionManager(f).cleanInvalid();
            w.check(0, 0, 0);
        }

        w.check(0, 0, 0);
        conn.close();

        getConnectionManager(f).clear();

        w0.check(0, 0, 0);

        int n = clearTopic(subname1, destName, true);
        assertTrue(n == 3);
        n = clearTopic(subname2, destName, true);
        assertTrue(n == 3);
        n = clearTopic(subname3, destName, true);
        assertTrue(n == 3);
    }

    /**
     * Purpose: Pooling of resources (connection, session, or producer)<br>
     * Assertion: A session obtained from a pool should have a correct signature<br>
     * Strategy: create non-transacted; close; create transacted; same connection
     * should be reused; both sessions should work properly<br>
     *
     * SIMULATED USE IN TXMGR ENVIRONMENT; CONSEQUENCES: SESSION CREATION ATTRIBUTES
     * WILL BE IGNORED
     *
     * @throws Throwable on failure of the test
     */
    public void test160tMixTxMgr() throws Throwable {
        init(true);

        // This is how the client would normally create connections
        InitialContext ctx = getContext();
        TopicConnectionFactory f = (TopicConnectionFactory) ctx.lookup(appjndiTopic);
        TopicConnection conn = f.createTopicConnection(USERID, PASSWORD);
        setClientID(conn);

        WireCount w = getConnectionCount();
        String destName = getTopic1Name();
        String subname1 = getDur1Name(destName);
        String subname2 = getDur2Name(destName);
        String subname3 = getDur3Name(destName);
        clearTopic(subname1, destName, false);
        clearTopic(subname2, destName, false);
        clearTopic(subname3, destName, false);

        TopicSession s = conn.createTopicSession(true, 0);
        
        s.getTransacted(); // actuate type
        w.check(1, 0, 0);
        {
            Topic dest = s.createTopic(destName);
            TopicPublisher prod = s.createPublisher(dest);
            w.check(1, 1, 0);
            TopicSubscriber cons = s.createDurableSubscriber(dest, subname1);
            assertTrue(cons != null);
            w.check(1, 1, 1);
            TextMessage m = s.createTextMessage("X");
            getManagedConnection(s).getLocalTransaction().begin();
            prod.send(m);
            getManagedConnection(s).getLocalTransaction().commit();

            // Connection shouldn't close; including producer
            conn.close();
            w.check(1, 1, 0);

        }

        // Connection shouldn't close; including producer
        conn.close();
        w.check(1, 1, 0);

        // Recreate a connection
        conn = f.createTopicConnection(USERID, PASSWORD);
        setClientID(conn);

        // Create session with different signature (signature is IGNORED)
        s = conn.createTopicSession(false, Session.AUTO_ACKNOWLEDGE);
        s.getTransacted(); // actuate type
        s.createTextMessage("void");

        // Session with same signature should be cached
        {
            Topic dest = s.createTopic(destName);
            getManagedConnection(s).getLocalTransaction().begin();
            TopicPublisher prod = s.createPublisher(dest);
            w.check(1, 1, 0);
            TopicSubscriber cons = s.createDurableSubscriber(dest, subname2);
            assertTrue(cons != null);
            w.check(1, 1, 1);
            TextMessage m = s.createTextMessage("X");
            w.check(1, 1, 1);
            prod.publish(m);
            getManagedConnection(s).getLocalTransaction().commit();

            // Will NOT use transacted twin because session attributes are ignored
            w.check(1, 1, 1);

            conn.close();

            // cons should close
            w.check(1, 1, 0);
        }

        // Recreate a connection
        conn = f.createTopicConnection(USERID, PASSWORD);
        setClientID(conn);

        // Create session with same signature (attributes are IGNORED)
        s = conn.createTopicSession(false, Session.AUTO_ACKNOWLEDGE);
        s.getTransacted(); // actuate type
        s.createTextMessage("void");

        // Producer should not have closed when switching types because it had no effect
        w.check(1, 1, 0);


        // Session with same signature should be cached
        {
            getManagedConnection(s).getLocalTransaction().begin();
            Topic dest = s.createTopic(destName);
            TopicPublisher prod = s.createPublisher(dest); // should reuse publisher!
            w.check(1, 1, 0);
            TopicSubscriber cons = s.createDurableSubscriber(dest, subname3);
            assertTrue(cons != null);
            w.check(1, 1, 1);
            TextMessage m = s.createTextMessage("X");
            w.check(1, 1, 1);
            prod.publish(m);
            getManagedConnection(s).getLocalTransaction().commit();
            w.check(1, 1, 1);

            conn.close();
        }

        w.check(1, 1, 0);
        conn.close();

        getConnectionManager(f).clear();

        w.check(0, 0, 0);

        int n = clearTopic(subname1, destName, true);
        assertTrue(n == 3);
        n = clearTopic(subname2, destName, true);
        assertTrue("n=" + n, n == 2);
        n = clearTopic(subname3, destName, true);
        assertTrue(n == 1);
    }

    /**
     * Purpose: Pooling of resources (connection, session, or producer)<br>
     * Assertion: A session will not be returned to the pool if any exception happened in
     * any of the sessions
     * Strategy: <br>
     *
     * @throws Throwable on failure of the test
     */
    public void test170q() throws Throwable {
        init(true);

        // This is how the client would normally create connections
        InitialContext ctx = getContext();
        QueueConnectionFactory f = (QueueConnectionFactory) ctx.lookup(appjndiQueue);
        QueueConnection conn = f.createQueueConnection(USERID, PASSWORD);

        WireCount s0 = getConnectionCount();

        QueueSession s = conn.createQueueSession(true, 0);
        s.getTransacted(); // actuate type

        s0.check(1);

        // Cause an exception
        try {
            s.createQueue(null);
            throw new Throwable("Exception expected");
        } catch (JMSException ex) {
        }

        // Connection should close due to the exception
        s.close();
        getConnectionManager(f).cleanInvalid();
        s0.check(0);

        // Session should act like it is closed
        try {
            s.createTemporaryQueue();
            throw new Throwable("Exception expected");
        } catch (JMSException ex1) {
        }

        conn.close();

        getConnectionManager(f).clear();
        s0.check(0);
    }

    /**
     * Purpose: Pooling of resources (connection, session, or producer)<br>
     * Assertion:    Calling close on a producer should not close the producer, but
     * return it to the pool
     * Strategy: <br>
     *
     * @throws Throwable on failure of the test
     */
    public void test200t() throws Throwable {
        init(true);

        // This is how the client would normally create connections
        InitialContext ctx = getContext();
        TopicConnectionFactory f = (TopicConnectionFactory) ctx.lookup(appjndiTopic);
        TopicConnection conn = f.createTopicConnection(USERID, PASSWORD);

        WireCount s0 = getConnectionCount();

        TopicSession s = conn.createTopicSession(true, 0);
        s.getTransacted(); // actuate type

        s0.check(1);

        Topic t = s.createTopic(getTopic1Name());

        TopicPublisher p = s.createPublisher(t);
        s0.check(2);

        // Close not should close
        p.close();
        s0.check(2);

        // Should act "closed"
        try {
            p.getTopic();
            throw new Throwable("Exception not thrown");
        } catch (JMSException ex) {
            // expected
        }

        // Should reuse
        p = s.createPublisher(t);
        s0.check(2);

        // Close not should close
        p.close();
        s0.check(2);

        // Different topic:
        t = s.createTopic("y");
        p = s.createPublisher(t);
        s0.check(3);

        // Done
        conn.close();
        s0.check(3);

        getConnectionManager(f).clear();
        s0.check(0);
    }

    /**
     * Purpose: Pooling of resources (connection, session, or producer)<br>
     * Assertion: Calling close on a session should not close any producers but return
     * them to the pool
     * Strategy: <br>
     *
     * @throws Throwable on failure of the test
     */
    public void test210t() throws Throwable {
        init(true);

        // This is how the client would normally create connections
        InitialContext ctx = getContext();
        TopicConnectionFactory f = (TopicConnectionFactory) ctx.lookup(appjndiTopic);
        TopicConnection conn = f.createTopicConnection(USERID, PASSWORD);

        WireCount s0 = getConnectionCount();

        TopicSession s = conn.createTopicSession(true, 0);
        s.getTransacted(); // actuate type

        s0.check(1);

        Topic t = s.createTopic(getTopic1Name());

        TopicPublisher p = s.createPublisher(t);
        s0.check(2);

        // Close not should close
        s.close();
        s0.check(2);

        // Should act "closed"
        try {
            p.getTopic();
            throw new Throwable("Exception not thrown");
        } catch (JMSException ex) {
            // expected
        }

        // Should reuse
        s = conn.createTopicSession(true, 0);
        s.getTransacted(); // actuate type
        p = s.createPublisher(t);
        s0.check(2);

        // Close not should close
        s.close();
        s = conn.createTopicSession(true, 0);
        s.getTransacted(); // actuate type
        s0.check(2);

        // Different topic:
        t = s.createTopic("y");
        p = s.createPublisher(t);
        s0.check(3);

        // Done
        conn.close();
        s0.check(3);

        getConnectionManager(f).clear();
        s0.check(0);
    }

    /**
     * Purpose: Pooling of resources (connection, session, or producer)<br>
     * Assertion: Calling close on a session without producer pooling should close producers
     * Strategy: <br>
     *
     * @throws Throwable on failure of the test
     */
    public void test215t() throws Throwable {
        init(false);

        // This is how the client would normally create connections
        InitialContext ctx = getContext();
        TopicConnectionFactory f = (TopicConnectionFactory) ctx.lookup(appjndiTopic);
        TopicConnection conn = f.createTopicConnection(USERID, PASSWORD);

        WireCount s0 = getConnectionCount();

        TopicSession s = conn.createTopicSession(true, 0);
        s.getTransacted(); // actuate type

        s0.check(1);

        Topic t = s.createTopic(getTopic1Name());

        TopicPublisher p = s.createPublisher(t);
        s0.check(2);

        // Close should close
        s.close();
        s0.check(1);

        // Should act "closed"
        try {
            p.getTopic();
            throw new Throwable("Exception not thrown");
        } catch (JMSException ex) {
            // expected
        }

        // Should reuse
        s = conn.createTopicSession(true, 0);
        s.getTransacted(); // actuate type
        p = s.createPublisher(t);
        s0.check(2);

        // Close should close
        s.close();
        s = conn.createTopicSession(true, 0);
        s.getTransacted(); // actuate type
        s0.check(1);

        // Done
        conn.close();
        s0.check(1);

        getConnectionManager(f).clear();
        s0.check(0);
    }

    /**
     * Purpose: Pooling of resources (connection, session, or producer)<br>
     * Assertion: Calling close on a session without session pooling should close producers
     * Strategy: <br>
     *
     * @throws Throwable on failure of the test
     */
    public void test216t() throws Throwable {
        init(false);

        // This is how the client would normally create connections
        InitialContext ctx = getContext();
        TopicConnectionFactory f = (TopicConnectionFactory) ctx.lookup(appjndiTopic);
        TopicConnection conn = f.createTopicConnection(USERID, PASSWORD);

        WireCount s0 = getConnectionCount();

        TopicSession s = conn.createTopicSession(true, 0);
        s.getTransacted(); // actuate type

        s0.check(1);

        Topic t = s.createTopic(getTopic1Name());

        TopicPublisher p = s.createPublisher(t);
        s0.check(2);

        // Close should close
        conn.close();
        s0.check(1);

        // Should act "closed"
        try {
            p.getTopic();
            throw new Throwable("Exception not thrown");
        } catch (JMSException ex) {
            // expected
        }

        // Should reuse
        conn = f.createTopicConnection(USERID, PASSWORD);
        s = conn.createTopicSession(true, 0);
        s.getTransacted(); // actuate type
        p = s.createPublisher(t);
        s0.check(2);

        // Close should close
        conn.close();
        conn = f.createTopicConnection(USERID, PASSWORD);
        s = conn.createTopicSession(true, 0);
        s.getTransacted(); // actuate type
        s0.check(1);

        // Done
        conn.close();
        s0.check(1);

        getConnectionManager(f).clear();
        s0.check(0);
    }

    /**
     * Purpose: Pooling of resources (connection, session, or producer)<br>
     * Assertion: Calling close on a connection should not close any producers but return
     * them to the pool
     * Strategy: <br>
     *
     * @throws Throwable on failure of the test
     */
    public void test220t() throws Throwable {
        init(true);

        // This is how the client would normally create connections
        InitialContext ctx = getContext();
        TopicConnectionFactory f = (TopicConnectionFactory) ctx.lookup(appjndiTopic);
        TopicConnection conn = f.createTopicConnection(USERID, PASSWORD);

        WireCount s0 = getConnectionCount();

        TopicSession s = conn.createTopicSession(true, 0);
        s.getTransacted(); // actuate type

        s0.check(1);

        Topic t = s.createTopic(getTopic1Name());

        TopicPublisher p = s.createPublisher(t);
        s0.check(2);

        // Close not should close
        conn.close();
        s0.check(2);

        // Should act "closed"
        try {
            p.getTopic();
            throw new Throwable("Exception not thrown");
        } catch (JMSException ex) {
            // expected
        }

        // Should reuse
        conn = f.createTopicConnection(USERID, PASSWORD);
        s = conn.createTopicSession(true, 0);
        s.getTransacted(); // actuate type
        p = s.createPublisher(t);
        s0.check(2);

        // Close not should close
        conn.close();
        conn = f.createTopicConnection(USERID, PASSWORD);
        s = conn.createTopicSession(true, 0);
        s.getTransacted(); // actuate type
        s0.check(2);

        // Different topic:
        t = s.createTopic("y");
        p = s.createPublisher(t);
        s0.check(3);

        // Done
        conn.close();
        s0.check(3);

        getConnectionManager(f).clear();
        s0.check(0);
    }

    protected com.stc.jmsjca.core.XDefaultConnectionManager getConnectionManager(javax.jms.ConnectionFactory f) {
        com.stc.jmsjca.core.JConnectionFactory ff = (com.stc.jmsjca.core.JConnectionFactory) f;
        return (com.stc.jmsjca.core.XDefaultConnectionManager) ff.getConnectionManager();
    }

    protected com.stc.jmsjca.core.RAJMSResourceAdapter getRA(javax.jms.ConnectionFactory f) {
        com.stc.jmsjca.core.JConnectionFactory ff = (com.stc.jmsjca.core.JConnectionFactory) f;
        return ff.getRA();
    }

    protected com.stc.jmsjca.core.XDefaultConnectionManager getExtConnectionManager(javax.jms.ConnectionFactory f) {
        com.stc.jmsjca.core.JConnectionFactory ff = (com.stc.jmsjca.core.JConnectionFactory) f;
        return (com.stc.jmsjca.core.XDefaultConnectionManager) ff.getConnectionManager();
    }

    /**
     * Purpose: Pooling of resources (connection, session, or producer)<br>
     * Assertion: Calling close on a connection w/o session pooling should close all
     * producers
     * Strategy: <br>
     *
     * @throws Throwable on failure of the test
     */
    public void test225t() throws Throwable {
        init(false, true);

        // This is how the client would normally create connections
        InitialContext ctx = getContext();
        TopicConnectionFactory f = (TopicConnectionFactory) ctx.lookup(appjndiTopic);
        TopicConnection conn = f.createTopicConnection(USERID, PASSWORD);

        WireCount s0 = getConnectionCount();


        TopicSession s = conn.createTopicSession(true, 0);
        s.getTransacted(); // actuate type

        s0.check(1);

        Topic t = s.createTopic(getTopic1Name());

        TopicPublisher p = s.createPublisher(t);
        s0.check(2);

        // Close doesn't close session
        conn.close();
        // ... but clear does
        getConnectionManager(f).clear();

        s0.check(0);

        // Should act "closed"
        try {
            p.getTopic();
            throw new Throwable("Exception not thrown");
        } catch (JMSException ex) {
            // expected
        }

        // Recreate
        conn = f.createTopicConnection(USERID, PASSWORD);
        s = conn.createTopicSession(true, 0);
        s.getTransacted(); // actuate type
        p = s.createPublisher(t);
        s0.check(2);

        // Close doesn't close
        conn.close();
        s0.check(2);
        conn = f.createTopicConnection(USERID, PASSWORD);
        // ... but clear does
        getConnectionManager(f).clear();
        s0.check(0);

        s = conn.createTopicSession(true, 0);
        s.getTransacted(); // actuate type
        s0.check(1);

        // Done
        conn.close();
        getConnectionManager(f).clear();
        s0.check(0);

        getConnectionManager(f).clear();
        s0.check(0);
    }

    /**
     * Purpose: Pooling of resources (connection, session, or producer)<br>
     * Assertion: Calling close on a producer without pooling should close it
     * Strategy: <br>
     *
     * @throws Throwable on failure of the test
     */
    public void test230t() throws Throwable {
        init(false);

        // This is how the client would normally create connections
        InitialContext ctx = getContext();
        TopicConnectionFactory f = (TopicConnectionFactory) ctx.lookup(appjndiTopic);
        TopicConnection conn = f.createTopicConnection(USERID, PASSWORD);

        WireCount s0 = getConnectionCount();

        TopicSession s = conn.createTopicSession(true, 0);
        s.getTransacted(); // actuate type

        s0.check(1);

        Topic t = s.createTopic(getTopic1Name());

        TopicPublisher p = s.createPublisher(t);
        s0.check(2);

        // Close should close
        p.close();
        s0.check(1);

        // Should act "closed"
        try {
            p.getTopic();
            throw new Throwable("Exception not thrown");
        } catch (JMSException ex) {
            // expected
        }

        // Done
        conn.close();
        s0.check(1);

        getConnectionManager(f).clear();
        s0.check(0);
    }

    /**
     * Purpose: Pooling of resources (connection, session, or producer)<br>
     * Assertion: Calling close on a session without pooling should close the producers
     * Strategy: <br>
     *
     * @throws Throwable on failure of the test
     */
    public void test240t() throws Throwable {
        init(true, false);

        // This is how the client would normally create connections
        InitialContext ctx = getContext();
        TopicConnectionFactory f = (TopicConnectionFactory) ctx.lookup(appjndiTopic);
        TopicConnection conn = f.createTopicConnection(USERID, PASSWORD);

        WireCount s0 = getConnectionCount();

        TopicSession s = conn.createTopicSession(true, 0);
        s.getTransacted(); // actuate type

        s0.check(1);

        Topic t = s.createTopic(getTopic1Name());

        TopicPublisher p = s.createPublisher(t);
        s0.check(2);

        // Session close should prodcuer
        s.close();
        s0.check(1);

        // Should act "closed"
        try {
            p.getTopic();
            throw new Throwable("Exception not thrown");
        } catch (JMSException ex) {
            // expected
        }

        // Done
        conn.close();
        s0.check(1);

        getConnectionManager(f).clear();
        s0.check(0);
    }

    /**
     * Purpose: Pooling of resources (connection, session, or producer)<br>
     * Assertion: State of producer should be reset on calling close()
     * Strategy: <br>
     *
     * @throws Throwable on failure of the test
     */
    public void test250t() throws Throwable {
        init(true, true);

        // This is how the client would normally create connections
        InitialContext ctx = getContext();
        TopicConnectionFactory f = (TopicConnectionFactory) ctx.lookup(appjndiTopic);
        TopicConnection conn = f.createTopicConnection(USERID, PASSWORD);

        WireCount s0 = getConnectionCount();

        TopicSession s = conn.createTopicSession(true, 0);
        s.getTransacted(); // actuate type

        s0.check(1);

        Topic t = s.createTopic(getTopic1Name());

        TopicPublisher p = s.createPublisher(t);
        s0.check(2);

        assertTrue(p.getDeliveryMode() == DeliveryMode.PERSISTENT);
        assertTrue(p.getDisableMessageID() == false);
        assertTrue(p.getDisableMessageTimestamp() == false);
        assertTrue(p.getPriority() == 4);
        assertTrue(p.getTimeToLive() == 0);

        p.setDeliveryMode(DeliveryMode.NON_PERSISTENT);
        p.setDisableMessageID(true);
        p.setDisableMessageTimestamp(true);
        p.setPriority(3);
        p.setTimeToLive(1);

        // Session close should not close prodcuer
        s.close();
        s0.check(2);

        // Should act "closed"
        try {
            p.getTopic();
            throw new Throwable("Exception not thrown");
        } catch (JMSException ex) {
            // expected
        }

        // Recreate
        s = conn.createTopicSession(true, 0);
        s.getTransacted(); // actuate type
        p = s.createPublisher(t);
        s0.check(2);

        assertTrue(p.getDeliveryMode() == DeliveryMode.PERSISTENT);
        assertTrue(p.getDisableMessageID() == false);
        assertTrue(p.getDisableMessageTimestamp() == false);
        assertTrue(p.getPriority() == 4);
        assertTrue(p.getTimeToLive() == 0);

        // Done
        conn.close();
        s0.check(2);

        getConnectionManager(f).clear();
        s0.check(0);
    }

    /**
     * Purpose: Pooling of resources (connection, session, or producer)<br>
     * Assertion: Producers on temporary destinations are not cached
     * Strategy: <br>
     *
     * @throws Throwable on failure of the test
     */
    public void test260t() throws Throwable {
        init(true, true);

        // This is how the client would normally create connections
        InitialContext ctx = getContext();
        TopicConnectionFactory f = (TopicConnectionFactory) ctx.lookup(appjndiTopic);
        TopicConnection conn = f.createTopicConnection(USERID, PASSWORD);

        WireCount s0 = getConnectionCount();

        TopicSession s = conn.createTopicSession(true, 0);
        s.getTransacted(); // actuate type

        s0.check(1);

        Topic t = s.createTemporaryTopic();

        TopicPublisher p = s.createPublisher(t);
        s0.check(2);

        // Session close should producer cuz it's temporary
        s.close();
        s0.check(1);

        // Should act "closed"
        try {
            p.getTopic();
            throw new Throwable("Exception not thrown");
        } catch (JMSException ex) {
            // expected
        }

        // Done
        conn.close();

        getConnectionManager(f).clear();
        s0.check(0);
    }

    /**
     * Purpose: Pooling of resources (connection, session, or producer)<br>
     * Assertion: Calling close on a pooled session should close the subscribers
     * Strategy: <br>
     *
     * @throws Throwable on failure of the test
     */
    public void test300q() throws Throwable {
        init(true, true);

        // This is how the client would normally create connections
        InitialContext ctx = getContext();
        QueueConnectionFactory f = (QueueConnectionFactory) ctx.lookup(appjndiQueue);
        QueueConnection conn = f.createQueueConnection(USERID, PASSWORD);

        WireCount s0 = getConnectionCount();

        QueueSession s = conn.createQueueSession(true, 0);
        s.getTransacted(); // actuate type

        s0.check(1);

        Queue dest = s.createQueue(getQueue1Name());

        QueueReceiver cons = s.createReceiver(dest);
        s0.check(2);

        // Session close should close consumer
        s.close();
        s0.check(1);

        // Should act "closed"
        try {
            cons.getQueue();
            throw new Throwable("Exception not thrown");
        } catch (JMSException ex) {
            // expected
        }

        // Done
        conn.close();
        s0.check(1);

        getConnectionManager(f).clear();
        s0.check(0);
    }

    /**
     * Purpose: Pooling of resources (connection, session, or producer)<br>
     * Assertion: Calling close on a non-pooled session should close the subscribers
     * Strategy: <br>
     *
     * CHANGED: sessions are always pooled
     *
     * @throws Throwable on failure of the test
     */
    public void test310q() throws Throwable {
        init(false, true);

        // This is how the client would normally create connections
        InitialContext ctx = getContext();
        QueueConnectionFactory f = (QueueConnectionFactory) ctx.lookup(appjndiQueue);
        QueueConnection conn = f.createQueueConnection(USERID, PASSWORD);

        WireCount s0 = getConnectionCount();

        QueueSession s = conn.createQueueSession(true, 0);
        s.getTransacted(); // actuate type

        s0.check(1);

        Queue dest = s.createQueue(getQueue1Name());

        QueueReceiver cons = s.createReceiver(dest);
        s0.check(2);

        // Session close should close consumer
        s.close();
        s0.check(1);

        // Should act "closed"
        try {
            cons.getQueue();
            throw new Throwable("Exception not thrown");
        } catch (JMSException ex) {
            // expected
        }

        s0.check(1);
        conn.close();
        s0.check(1);

        getConnectionManager(f).clear();
        s0.check(0);

        getConnectionManager(f).clear();
        s0.check(0);
    }

    /**
     * Purpose: Pooling of resources (connection, session, or producer)<br>
     * Assertion: Calling close on a connection with session pooling should close the subscribers
     * Strategy: <br>
     *
     * @throws Throwable on failure of the test
     */
    public void test320q() throws Throwable {
        init(true, true);

        // This is how the client would normally create connections
        InitialContext ctx = getContext();
        QueueConnectionFactory f = (QueueConnectionFactory) ctx.lookup(appjndiQueue);
        QueueConnection conn = f.createQueueConnection(USERID, PASSWORD);

        WireCount s0 = getConnectionCount();

        QueueSession s = conn.createQueueSession(true, 0);
        s.getTransacted(); // actuate type

        s0.check(1);

        Queue dest = s.createQueue(getQueue1Name());

        QueueReceiver cons = s.createReceiver(dest);
        s0.check(2);

        // Connection close should close consumer
        conn.close();
        s0.check(1);

        // Should act "closed"
        try {
            cons.getQueue();
            throw new Throwable("Exception not thrown");
        } catch (JMSException ex) {
            // expected
        }

        // Done
        conn.close();
        s0.check(1);

        getConnectionManager(f).clear();
        s0.check(0);
    }

    /**
     * Purpose: Pooling of resources (connection, session, or producer)<br>
     * Assertion: Calling close on a connection without session pooling should close the subscribers
     * Strategy: <br>
     *
     * CHANGED: sessions are always pooled
     *
     * @throws Throwable on failure of the test
     */
    public void test330q() throws Throwable {
        init(false, true);

        // This is how the client would normally create connections
        InitialContext ctx = getContext();
        QueueConnectionFactory f = (QueueConnectionFactory) ctx.lookup(appjndiQueue);
        QueueConnection conn = f.createQueueConnection(USERID, PASSWORD);

        WireCount s0 = getConnectionCount();

        QueueSession s = conn.createQueueSession(true, 0);
        s.getTransacted(); // actuate type

        s0.check(1);

        Queue dest = s.createQueue(getQueue1Name());

        QueueReceiver cons = s.createReceiver(dest);
        s0.check(2);

        // Connection close should close consumer, but not session
        conn.close();
        s0.check(1);

        // Should act "closed"
        try {
            cons.getQueue();
            throw new Throwable("Exception not thrown");
        } catch (JMSException ex) {
            // expected
        }

        // Done
        s0.check(1);
        conn.close();
        s0.check(1);

        getConnectionManager(f).clear();
        s0.check(0);

        getConnectionManager(f).clear();
        s0.check(0);
    }

    public void setNoXA(ConnectionFactory f) {
        getRA(f).setOptions(Options.NOXA + "=true");
    }

    /**
     * Purpose: Test transaction<br>
     * Assertion: <br>
     * Strategy: <br>
     *
     * @throws Throwable on failure of the test
     */
    public void test120() throws Throwable {
        init(true, true);

        // This is how the client would normally create connections
        InitialContext ctx = getContext();
        QueueConnectionFactory f = (QueueConnectionFactory) ctx.lookup(appjndiQueue);
        setNoXA(f);
        QueueConnection conn = f.createQueueConnection(USERID, PASSWORD);

        WireCount s0 = getConnectionCount();

        QueueSession s = conn.createQueueSession(true, 0);
        s.getTransacted(); // actuate type
        Message m = s.createTextMessage("x");
        Queue dest = s.createQueue(getQueue1Name());
        QueueSender prod = s.createSender(dest);
        prod.send(m);
        s.commit();
        conn.close();

        getConnectionManager(f).clear();
        s0.check(0);
    }

    /**
     * Purpose: Test transaction (local transaction)<br>
     * Assertion: <br>
     * Strategy: <br>
     *
     * @throws Throwable on failure of the test
     */
    public void test120LT() throws Throwable {
        init(true, true);

        // This is how the client would normally create connections
        InitialContext ctx = getContext();
        QueueConnectionFactory f = (QueueConnectionFactory) ctx.lookup(appjndiQueue);
        QueueConnection conn = f.createQueueConnection(USERID, PASSWORD);

        WireCount s0 = getConnectionCount();

        QueueSession s = conn.createQueueSession(true, 0);
        s.getTransacted(); // actuate type
        ManagedConnection mc = getManagedConnection(s);
        LocalTransaction lt = mc.getLocalTransaction();

        lt.begin();
        Message m = s.createTextMessage("x");
        Queue dest = s.createQueue(getQueue1Name());
        QueueSender prod = s.createSender(dest);
        prod.send(m);
        lt.commit();
        conn.close();

        getConnectionManager(f).clear();
        s0.check(0);
    }

    /**
     * Purpose: Test transaction (local transaction)<br>
     * Assertion: <br>
     * Strategy: <br>
     *
     * @throws Throwable on failure of the test
     */
    public void test120LTNoXA() throws Throwable {
        init(true, true);

        // This is how the client would normally create connections
        InitialContext ctx = getContext();
        QueueConnectionFactory f = (QueueConnectionFactory) ctx.lookup(appjndiQueue);
        setNoXA(f);
        QueueConnection conn = f.createQueueConnection(USERID, PASSWORD);

        WireCount s0 = getConnectionCount();

        QueueSession s = conn.createQueueSession(true, 0);
        s.getTransacted(); // actuate type
        ManagedConnection mc = getManagedConnection(s);
        LocalTransaction lt = mc.getLocalTransaction();

        lt.begin();
        Message m = s.createTextMessage("x");
        Queue dest = s.createQueue(getQueue1Name());
        QueueSender prod = s.createSender(dest);
        prod.send(m);
        lt.commit();
        conn.close();

        getConnectionManager(f).clear();
        s0.check(0);
    }

    /**
     * Reads all msgs in a queue
     *
     * @param queuename name of the queue
     * @throws Exception on any failure
     */
    public int clearQueue(String queuename, int expected) throws Throwable {
        QueueConnection conn = null;
        try {
            InitialContext ctx = getContext();
            QueueConnectionFactory f = (QueueConnectionFactory) ctx.lookup(appjndiQueue);
            getRA(f).setOptions(Options.NOXA + "=true");
            conn = f.createQueueConnection(USERID, PASSWORD);

            QueueSession sess = conn.createQueueSession(true, 0);
            sess.getTransacted(); // actuate type
            QueueReceiver recv = sess.createReceiver(sess.createQueue(queuename));
            conn.start();
            int ct = 0;
            int commit = 0;
            int commitsize = getJMSProvider().createPassthrough(mServerProperties).getCommitSize();
            if (expected == 0) {
                if (recv.receive(DONTEXPECT) != null) {
                    throw new Exception("Found msg where none expected");
                }
            } else if (expected == -1) {
                while (recv.receive(EXPECTWITHIN) != null) {
                    ct++;
                    commit++;
                    if (commit > commitsize) {
                        sess.commit();
                        commit = 0;
                    }
                }
            } else if (expected == -2) {
                while (recv.receive(DONTEXPECT) != null) {
                    ct++;
                    commit++;
                    if (commit > commitsize) {
                        sess.commit();
                        commit = 0;
                    }
                }
            } else {
                for (int i = 0; i < expected; i++) {
                    if (recv.receive(EXPECTWITHIN) != null) {
                        ct++;
                        commit++;
                        if (commit > commitsize) {
                            sess.commit();
                            commit = 0;
                        }
                    }
                }
                if (recv.receive(DONTEXPECT) != null) {
                    ct++;
                    commit++;
                    if (commit > commitsize) {
                        sess.commit();
                        commit = 0;
                    }
                }
            }
            
            
            sess.commit();
            conn.stop();
            sess.close();
            getConnectionManager(f).clear();
            
            if (ct != expected && expected >= 0) {
                throw new Exception("Found " + ct + " messages where " + expected + " expected");
            }
            
            return ct;
        } finally {
            if (conn != null) {
                conn.close();
            }
        }
    }

    protected static int EXPECTWITHIN = 2500;
    private static int DONTEXPECT = 500;


    /**
     * Reads all msgs in a topic
     *
     * @param queuename name of the topic
     * @throws Exception on any failure
     */
    public int clearTopic(String durableName, String topicName, boolean expect) throws Throwable {
        TopicConnection conn = null;
        try {
            InitialContext ctx = getContext();
            TopicConnectionFactory f = (TopicConnectionFactory) ctx.lookup(appjndiTopic);
            setNoXA(f);
            conn = f.createTopicConnection(USERID, PASSWORD);
            setClientID(conn);
            TopicSession sess = conn.createTopicSession(true, 0);
            sess.getTransacted(); // actuate type
            TopicSubscriber recv = sess.createDurableSubscriber(sess.createTopic(
                topicName), durableName);
            conn.start();
            int ct = 0;
            int commit = 0;
            int commitsize = getJMSProvider().createPassthrough(mServerProperties).getCommitSize();
            while (recv.receive(expect ? EXPECTWITHIN : DONTEXPECT) != null) {
                ct++;
                commit++;
                if (commit > commitsize) {
                    sess.commit();
                    commit = 0;
                }
            }
            sess.commit();
            
            recv.close();
            sess.unsubscribe(durableName);
            
            conn.stop();
            sess.close();
            getConnectionManager(f).clear();
            return ct;
        } finally {
            if (conn != null) {
                conn.close();
            }
        }
    }

    /**
     * Batch publishes n messages into the specified topic
     *
     * @param topic
     * @param n
     * @param m
     * @throws JMSException
     */
    public void createDurableSubscriber(String topic,
        String durableName) throws Throwable {
        InitialContext ctx = getContext();
        TopicConnectionFactory f = (TopicConnectionFactory) ctx.lookup(appjndiTopic);
        TopicConnection c = f.createTopicConnection(USERID, PASSWORD);
        setClientID(c);
        TopicSession s = c.createTopicSession(true, 0);
        s.getTransacted(); // actuate type
        Topic dest = s.createTopic(topic);
        TopicSubscriber sub = s.createDurableSubscriber(dest, durableName);
        assertTrue(sub != null);
        c.close();
        getConnectionManager(f).clear();
    }

    /**
     * Purpose: XA<br>
     * Assertion: Normal JMS behavior should still work with Default Connection Manager<br>
     * Strategy: Create XA session, send msg, commit, read back<br>
     * 
     * Tests only ONE xaresource
     *
     * @throws Throwable on failure of the test
     */
    public void test500q1() throws Throwable {
        init(false, true);

        // This is how the client would normally create connections
        InitialContext ctx = getContext();
        QueueConnectionFactory f = (QueueConnectionFactory) ctx.lookup(appjndiQueue);
        QueueConnection conn1 = f.createQueueConnection(USERID, PASSWORD);

        // First
        QueueSession sess1 = conn1.createQueueSession(true, 0);
        Queue dest = sess1.createQueue(getQueue1Name());
        XAResource xa1 = getManagedConnection(sess1).getXAResource();

        QueueSender prod1 = sess1.createSender(dest);

        // Start tran
        Xid xid1 = new XXid();

        xa1.start(xid1, XAResource.TMNOFLAGS);

        // Send in both
        prod1.send(sess1.createTextMessage("1aaa"));

        // Commit
        xa1.end(xid1, XAResource.TMSUCCESS);

        int s1 = xa1.prepare(xid1);
        assertEquals(XAResource.XA_OK, s1);
        xa1.commit(xid1, false);

        conn1.close();

        getConnectionManager(f).clear();

        // Check
        clearQueue(dest.getQueueName(), 1);

        getConnectionManager(f).clear();
    }

    /**
     * Purpose: XA<br>
     * Assertion: Normal JMS behavior should still work with Default Connection Manager<br>
     * Strategy: Create XA sessions, send msg, commit, read back<br>
     *
     * CHANGED: XASESSION NO LONGER VISIBLE
     *
     * @throws Throwable on failure of the test
     */
    public void test500q() throws Throwable {
        init(false, true);

        // This is how the client would normally create connections
        InitialContext ctx = getContext();
        QueueConnectionFactory f = (QueueConnectionFactory) ctx.lookup(appjndiQueue);
        QueueConnection conn1 = f.createQueueConnection(USERID, PASSWORD);

        // First
        QueueSession sess1 = conn1.createQueueSession(true, 0);
        Queue dest = sess1.createQueue(getQueue1Name());
        XAResource xa1 = getManagedConnection(sess1).getXAResource();

        // Second
        QueueConnection conn2 = f.createQueueConnection(USERID, PASSWORD);
        QueueSession sess2 = conn2.createQueueSession(true, Session.AUTO_ACKNOWLEDGE);
        XAResource xa2 = getManagedConnection(sess2).getXAResource();

        QueueSender prod1 = sess1.createSender(dest);
        QueueSender prod2 = sess2.createSender(dest);

        // Start tran
        Xid xid1 = new XXid();
        Xid xid2 = new XXid();

        xa1.start(xid1, XAResource.TMNOFLAGS);
        xa2.start(xid2, XAResource.TMNOFLAGS);

        // Send in both
        prod1.send(sess1.createTextMessage("1"));
        prod2.send(sess2.createTextMessage("2"));

        // Commit
        xa1.end(xid1, XAResource.TMSUCCESS);
        xa2.end(xid2, XAResource.TMSUCCESS);

        int s1 = xa1.prepare(xid1);
        assertEquals(XAResource.XA_OK, s1);
        int s2 = xa2.prepare(xid2);
        assertEquals(XAResource.XA_OK, s2);
        xa1.commit(xid1, false);
        xa2.commit(xid2, false);

        conn1.close();
        conn2.close();

        getConnectionManager(f).clear();

        // Check
        clearQueue(dest.getQueueName(), 2);

        getConnectionManager(f).clear();
    }

    /**
     * Purpose: XA<br>
     * Assertion: Normal JMS behavior should still work with Default Connection Manager<br>
     * Strategy: Create XA sessions, send msg, commit, read back<br>
     *
     * @throws Throwable on failure of the test
     */
    public void test500t() throws Throwable {
        init(false, true);

        String destname = getTopic1Name();
        String subname = getDur1Name(destname);
        createDurableSubscriber(destname, subname);

        // This is how the client would normally create connections
        InitialContext ctx = getContext();
        TopicConnectionFactory f = (TopicConnectionFactory) ctx.lookup(appjndiTopic);
        TopicConnection conn1 = f.createTopicConnection(USERID, PASSWORD);

        // First
        TopicSession sess1 = conn1.createTopicSession(true, 0);
        sess1.getTransacted(); // actuate type
        Topic dest = sess1.createTopic(destname);
        XAResource xa1 = getManagedConnection(sess1).getXAResource();
        TopicPublisher prod1 = sess1.createPublisher(dest);

        // Second
        TopicConnection conn2 = f.createTopicConnection(USERID, PASSWORD);
        TopicSession sess2 = conn2.createTopicSession(true, Session.AUTO_ACKNOWLEDGE);
        sess1.getTransacted(); // actuate type
        XAResource xa2 = getManagedConnection(sess2).getXAResource();
        TopicPublisher prod2 = sess2.createPublisher(dest);

        // Start tran
        Xid xid1 = new XXid();
        Xid xid2 = new XXid();

        xa1.start(xid1, XAResource.TMNOFLAGS);
        xa2.start(xid2, XAResource.TMNOFLAGS);

        // Send in both
        prod1.publish(sess1.createTextMessage("1"));
        prod2.publish(sess2.createTextMessage("2"));

        // Commit
        xa1.end(xid1, XAResource.TMSUCCESS);
        xa1.prepare(xid1);
        xa1.commit(xid1, false);

        if (xid1 != xid2) {
            xa2.end(xid2, XAResource.TMSUCCESS);
            xa2.prepare(xid2);
            xa2.commit(xid2, false);
        }

        conn1.close();
        conn2.close();

        getConnectionManager(f).clear();

        // Check
        int n = clearTopic(subname, dest.getTopicName(), true);
        assertTrue(n == 2);
    }

    protected ManagedConnection getManagedConnection(Connection c) {
        XAssert.notImplemented();
        return null;
    }

    protected ManagedConnection getManagedConnection(Session s) {
        com.stc.jmsjca.core.WSession ws = (com.stc.jmsjca.core.WSession) s;
        com.stc.jmsjca.core.JSession js = ws.getJSession();
        ManagedConnection mc = js.getManagedConnection();
        return mc;
    }

    /**
     * Purpose: XA<br>
     * Assertion: XA: getXAResource should fail on a non-XA connection<br>
     * Strategy: Create<br>
     *
     * INVALID: XA CONNECTIONS NOW SAME AS NON-XA
     *
     * @throws Throwable on failure of the test
     */
    public void invalid_test510q() throws Throwable {
        init(false, true);

        // This is how the client would normally create connections
        InitialContext ctx = getContext();
        QueueConnectionFactory f = (QueueConnectionFactory) ctx.lookup(appjndiQueue);
        QueueConnection conn1 = f.createQueueConnection(USERID, PASSWORD);

        ManagedConnection mc = getManagedConnection(conn1);

        try {
            XAResource r1 = mc.getXAResource();
            assertTrue(r1 != null); // nonsense
            throw new Throwable("Exception expected");
        } catch (ResourceException ex) {
            // ok
        }

        conn1.close();
    }

    /**
     * Purpose: XA<br>
     * Assertion: XAResource should not be wrapped (isSameRM() should still behave the same)
     * Strategy: getXAResouce(), create / create(), getXAResource()<br>
     *
     * @throws Throwable on failure of the test
     */
    public void test515q() throws Throwable {
        init(false, true);

        {
            InitialContext ctx = getContext();
            QueueConnectionFactory f = (QueueConnectionFactory) ctx.lookup(
                appjndiQueue);
            QueueConnection conn1 = f.createQueueConnection(USERID, PASSWORD);

            QueueSession sess1 = conn1.createQueueSession(true, 0);
            Queue dest = sess1.createQueue(getQueue1Name());
            XAResource xa2 = getManagedConnection(sess1).getXAResource();
            QueueSender prod1 = sess1.createSender(dest);
            assertTrue(prod1 != null);
            assertTrue(!Proxy.isProxyClass(xa2.getClass()));
            assertTrue(!xa2.getClass().getName().startsWith("com.stc.jmsjca"));
            conn1.close();
            getConnectionManager(f).clear();
        }
    }

    /**
     * Purpose: XA<br>
     * Assertion: createSession() after getXAResource() should return the same XAResource<br>
     * Strategy: getXAResouce(), create / create(), getXAResource()<br>
     *
     * @throws Throwable on failure of the test
     */
    public void test518q() throws Throwable {
        init(false, true);

        XAResource xa1;

        InitialContext ctx = getContext();
        QueueConnectionFactory f = (QueueConnectionFactory) ctx.lookup(
            appjndiQueue);

        {
            QueueConnection conn1 = f.createQueueConnection(USERID, PASSWORD);

            QueueSession sess1 = conn1.createQueueSession(true, 0);
            xa1 = getManagedConnection(sess1).getXAResource();
            Queue dest = sess1.createQueue(getQueue1Name());
            QueueSender prod1 = sess1.createSender(dest);
            assertTrue(prod1 != null);
            conn1.close();
        }

        {
            QueueConnection conn1 = f.createQueueConnection(USERID, PASSWORD);

            QueueSession sess1 = conn1.createQueueSession(true, 0);
            XAResource xa2 = getManagedConnection(sess1).getXAResource();
            Queue dest = sess1.createQueue(getQueue1Name());
            QueueSender prod1 = sess1.createSender(dest);
            assertTrue(prod1 != null);
            assertTrue(xa1 == xa2);
            conn1.close();
        }
        getConnectionManager(f).clear();
    }

    /**
     * Purpose: XA<br>
     * Assertion:
     *   520q XA: createSession() can be called only once before getXAResource() __OR__ once after getXAResource():
     *   - this should work: create, getXAResource
     *   - this should work: getXAResource create
     *   - this should fail: create, create, getXAResource (fail)
     *   - this should fail: create, getXAResource, create (fail)
     *   - this should fail: getXAResource create, create (fail)
     * Strategy: getXAResouce(), create / create(), getXAResource()<br>
     *
     * INVALID: SESSIONS ARE NOW "CONNECTIONS"
     *
     * @throws Throwable on failure of the test
     */
    public void invalid_test520q() throws Throwable {
        init(true, true);
        InitialContext ctx = getContext();
        XAQueueConnectionFactory f = (XAQueueConnectionFactory) ctx.lookup(appjndiQueue);

        //   - this should work: create, getXAResource
        {
            XAQueueConnection c = f.createXAQueueConnection(USERID, PASSWORD);

            XAQueueSession sess1 = c.createXAQueueSession();
            XAResource xa1 = sess1.getXAResource();
            assertTrue(xa1 != null);
            XAResource xa2 = getManagedConnection(c).getXAResource();
            assertTrue(xa2 != null);
            c.close();
        }

        //   - this should work: getXAResource create
        {
            XAQueueConnection c = f.createXAQueueConnection(USERID, PASSWORD);
            XAResource xa2 = getManagedConnection(c).getXAResource();
            assertTrue(xa2 != null);
            XAQueueSession sess1 = c.createXAQueueSession();
            XAResource xa1 = sess1.getXAResource();
            assertTrue(xa1 != null);
            c.close();
        }

        //   - this should fail: create, create, getXAResource (fail)
        {
            XAQueueConnection c = f.createXAQueueConnection(USERID, PASSWORD);
            XAQueueSession sess1 = c.createXAQueueSession();
            assertTrue(sess1 != null);
            XAQueueSession sess2 = c.createXAQueueSession();
            assertTrue(sess2 != null);

            try {
                XAResource xa2 = getManagedConnection(c).getXAResource();
                assertTrue(xa2 != null);
                throw new Throwable("exception expected");
            } catch (ResourceException ex) {
                // ok
            }
            c.close();
        }

        //   - this should fail: create, getXAResource, create (fail)
        {
            XAQueueConnection c = f.createXAQueueConnection(USERID, PASSWORD);
            XAQueueSession sess1 = c.createXAQueueSession();
            assertTrue(sess1 != null);
            XAResource xa2 = getManagedConnection(c).getXAResource();
            assertTrue(xa2 != null);

            try {
                XAQueueSession sess2 = c.createXAQueueSession();
                assertTrue(sess2 != null);
                throw new Throwable("exception expected");
            } catch (JMSException ex) {
                // ok
            }
            c.close();
        }

        //   - this should fail: getXAResource create, create (fail)
        {
            XAQueueConnection c = f.createXAQueueConnection(USERID, PASSWORD);
            XAResource xa2 = getManagedConnection(c).getXAResource();
            assertTrue(xa2 != null);

            XAQueueSession sess1 = c.createXAQueueSession();
            assertTrue(sess1 != null);

            try {
                XAQueueSession sess2 = c.createXAQueueSession();
                assertTrue(sess2 != null);
                throw new Throwable("exception expected");
            } catch (JMSException ex) {
                // ok
            }
            c.close();
        }
    }

    /**
     * Purpose: XA<br>
     * Assertion:
     * 530q XA: w/o session pooling, close() on a session should not close the session if getXAResource() was called
     * - this should work: create, getXAResource, close, getXAResource().commit()
     * - this should work: getXAResource create, close(), getXAResource().commit()
     * - this should fail: create, close(), getXAResource().commit()
     * - this should work: create, close, create, getXAResource
     * - this should work: create, getXAResource, close, create; sessions should be the same
     * Strategy: getXAResouce(), create / create(), getXAResource()<br>
     *
     * INVALID: APP SERVER HAS CONTROL OVER CLOSING
     *
     * @throws Throwable on failure of the test
     */
    public void invalid_test530q() throws Throwable {
        init(false, true);
        InitialContext ctx = getContext();
        XAQueueConnectionFactory f = (XAQueueConnectionFactory) ctx.lookup(appjndiQueue);

        //   - this should work: create, getXAResource, close, getXAResource().commit()
        {
            XAQueueConnection c = f.createXAQueueConnection(USERID, PASSWORD);

            XAQueueSession sess1 = c.createXAQueueSession();
            XAResource xa1 = sess1.getXAResource();
            XAResource xa2 = getManagedConnection(c).getXAResource();
            assertTrue(xa1 == xa2);

            // Start tran
            Xid xid1 = new XXid();
            xa1.start(xid1, XAResource.TMNOFLAGS);
            Queue dest = sess1.getQueueSession().createQueue(getQueue1Name());
            QueueSender prod1 = sess1.getQueueSession().createSender(dest);
            prod1.send(sess1.createTextMessage("1"));

            // Close
            sess1.close();

            // Commit
            xa1.end(xid1, XAResource.TMSUCCESS);
            xa1.prepare(xid1);
            xa1.commit(xid1, false);

            // Check
            clearQueue(dest.getQueueName(), 1);

            c.close();
        }

        //   - this should work: getXAResource, create, close(), getXAResource().commit()
        {
            XAQueueConnection c = f.createXAQueueConnection(USERID, PASSWORD);

            XAResource xa2 = getManagedConnection(c).getXAResource();
            XAQueueSession sess1 = c.createXAQueueSession();
            XAResource xa1 = sess1.getXAResource();
            assertTrue(xa1 == xa2);

            // Start tran
            Xid xid1 = new XXid();
            xa1.start(xid1, XAResource.TMNOFLAGS);
            Queue dest = sess1.getQueueSession().createQueue(getQueue1Name());
            QueueSender prod1 = sess1.getQueueSession().createSender(dest);
            prod1.send(sess1.createTextMessage("1"));

            // Close
            sess1.close();

            // Commit
            xa1.end(xid1, XAResource.TMSUCCESS);
            xa1.prepare(xid1);
            xa1.commit(xid1, false);

            // Check
            clearQueue(dest.getQueueName(), 1);

            c.close();
        }

        //   - this should fail: create, close(), getXAResource().commit()
        {
            XAQueueConnection c = f.createXAQueueConnection(USERID, PASSWORD);

            XAQueueSession sess1 = c.createXAQueueSession();
            XAResource xa1 = sess1.getXAResource();

            // Start tran
            Xid xid1 = new XXid();
            xa1.start(xid1, XAResource.TMNOFLAGS);
            Queue dest = sess1.getQueueSession().createQueue(getQueue1Name());
            QueueSender prod1 = sess1.getQueueSession().createSender(dest);
            prod1.send(sess1.createTextMessage("1"));

            // Close
            sess1.close();

            // Commit
            try {
                xa1.end(xid1, XAResource.TMSUCCESS);
                xa1.prepare(xid1);
                xa1.commit(xid1, false);
                throw new Throwable("exception expected");
            } catch (XAException ex) {
                // ok
            }

            // Check
            clearQueue(dest.getQueueName(), 0);

            c.close();
        }

        //   - this should work: create, close, create, getXAResource
        {
            XAQueueConnection c = f.createXAQueueConnection(USERID, PASSWORD);

            XAResource xa2 = getManagedConnection(c).getXAResource();
            XAQueueSession sess1 = c.createXAQueueSession();
            sess1.close();
            sess1 = c.createXAQueueSession();

            XAResource xa1 = sess1.getXAResource();
            assertTrue(xa1 == xa2);

            // Start tran
            Xid xid1 = new XXid();
            xa1.start(xid1, XAResource.TMNOFLAGS);
            Queue dest = sess1.getQueueSession().createQueue(getQueue1Name());
            QueueSender prod1 = sess1.getQueueSession().createSender(dest);
            prod1.send(sess1.createTextMessage("1"));

            // Close
            sess1.close();

            // Commit
            xa1.end(xid1, XAResource.TMSUCCESS);
            xa1.prepare(xid1);
            xa1.commit(xid1, false);

            // Check
            clearQueue(dest.getQueueName(), 1);

            c.close();
        }

        //   - this should work: create, getXAResource, close, create; sessions should be the same
        {
            XAQueueConnection c = f.createXAQueueConnection(USERID, PASSWORD);

            XAResource xa2 = getManagedConnection(c).getXAResource();
            XAQueueSession sess1 = c.createXAQueueSession();

            XAResource xa1 = sess1.getXAResource();
            assertTrue(xa1 == xa2);

            sess1.close();

            sess1 = c.createXAQueueSession();

            XAResource xa3 = sess1.getXAResource();
            assertTrue(xa1 == xa3);

            xa2 = getManagedConnection(c).getXAResource();
            assertTrue(xa1 == xa2);

            c.close();
        }
    }

    /**
     * Purpose: CC<br>
     * Assertion: if a session is used for CC, it should not be reused<br>
     * Strategy: <br>
     *
     * INVALID: CONNECTION CONSUMER CANNOT BE USED
     *
     * @throws Throwable on failure of the test
     */
    public void invalid_test600t() throws Throwable {
        init(true, true);

        InitialContext ctx = getContext();
        TopicConnectionFactory f = (TopicConnectionFactory) ctx.lookup(appjndiTopic);
        TopicConnection conn = f.createTopicConnection(USERID, PASSWORD);

        WireCount s0 = getConnectionCount();

        TopicSession s = conn.createTopicSession(true, 0);
        s.getTransacted(); // actuate type
        s.setMessageListener(null);

        s0.check(1);

        // Connection should close
        conn.close();
        s0.check(0);

        // Recreate a connection
        conn = f.createTopicConnection(USERID, PASSWORD);
        s = conn.createTopicSession(true, 0);
        s.getTransacted(); // actuate type
        s0.check(1);
        conn.close();
        s0.check(1);
    }

    /**
     * Purpose: Pooling of resources (connection, session, or producer)<br>
     * Assertion: A session will not be returned to the pool if any exception happened
     * Strategy: <br>
     *
     * @throws Throwable on failure of the test
     */
    public void test700q() throws Throwable {
        init(true, true);

        // This is how the client would normally create connections
        InitialContext ctx = getContext();
        QueueConnectionFactory f = (QueueConnectionFactory) ctx.lookup(appjndiQueue);
        QueueConnection conn = f.createQueueConnection(USERID, PASSWORD);

        WireCount s0 = getConnectionCount();
        QueueSession s = conn.createQueueSession(true, 0);
        s.getTransacted(); // actuate type
        s0.check(1);
        Queue dest = s.createQueue(getQueue1Name());

        // Don't reuse producer
        {
            QueueSender prod = s.createSender(dest);
            s0.check(2);

            // Cause exception
            try {
                prod.send(null);
                throw new Throwable("no throw");
            } catch (JMSException ex2) {
            } catch (NullPointerException ex3) {
            }

            prod.close();
            s0.check(1);
        }

        // Don't reuse session
        s.close();
        getConnectionManager(f).cleanInvalid();
        s0.check(0);

        conn.close();
        getConnectionManager(f).clear();
        s0.check(0);
    }

    /**
     * Purpose: Pooling of resources (connection, session, or producer)<br>
     * Assertion: 710q cleanup() should throw an exception if an exception was thrown on the connection, or any of the objects created by the connection.
     * Strategy: <br>
     *
     * @throws Throwable on failure of the test
     */
    public void test710q() throws Throwable {
        init(true, true);

        // This is how the client would normally create connections
        InitialContext ctx = getContext();
        QueueConnectionFactory f = (QueueConnectionFactory) ctx.lookup(appjndiQueue);
        QueueConnection conn = f.createQueueConnection(USERID, PASSWORD);

        WireCount s0 = getConnectionCount();
        QueueSession s = conn.createQueueSession(true, 0);
        s.getTransacted(); // actuate type
        s0.check(1);
        Queue dest = s.createQueue(getQueue1Name());

        // Cause exception in producer
        {
            QueueSender prod = s.createSender(dest);
            s0.check(2);

            // Cause exception
            try {
                prod.send(null);
                throw new Throwable("no throw");
            } catch (JMSException ex2) {
            } catch (NullPointerException ex3) {
            }

            prod.close();
            s0.check(1);
        }

        // Don't reuse connection
        conn.close();
        getConnectionManager(f).cleanInvalid();
        s0.check(0);
        getConnectionManager(f).clear();
        s0.check(0);
    }

    /**
     * Purpose: Validate bug 63127
     * Assertion: if (obj == obj) obj.equals(obj) should also be true
     * Strategy: <br>
     *
     * @throws Throwable on failure of the test
     */
    public void test_bug63127() throws Throwable {
        init(true, true);

        InitialContext ctx = getContext();
        QueueConnectionFactory f = (QueueConnectionFactory) ctx.lookup(appjndiQueue);

        QueueConnection conn = f.createQueueConnection(USERID, PASSWORD);

        // Calls toString on Wrapper
        String s = conn.toString();
        assertTrue(s != null);

        // Calls equals on Wrapper
        assertTrue(conn.equals(conn));

        conn.close();
        getConnectionManager(f).clear();
    }

    /**
     * Purpose: XA should be transparent<br>
     * Assertion: XA is used when getXAResource() is called by container<br>
     * Strategy: Create session, getXAResource(), send msg, commit, read back<br>
     *
     * @throws Throwable on failure of the test
     */
    public void failed_____test800q() throws Throwable {
        init(false, true);

        InitialContext ctx = getContext();
        XAQueueConnectionFactory f = (XAQueueConnectionFactory) ctx.lookup(appjndiQueue);

        // This will get an object that can turn into an XA connection, or a non-XA connection
        QueueConnection conn1 = f.createQueueConnection(USERID, PASSWORD);
        assertTrue(!(conn1 instanceof XAQueueConnection));

        // Which one is determined by a call to getXAResource()
        ManagedConnection mc = getManagedConnection(conn1);
        XAResource xa1 = mc.getXAResource();

        // First
        QueueSession sess1 = conn1.createQueueSession(false, Session.AUTO_ACKNOWLEDGE);
        Queue dest = sess1.createQueue(getQueue1Name());
        QueueSender prod1 = sess1.createSender(dest);

        // Second
        QueueConnection conn2 = f.createQueueConnection(USERID, PASSWORD);
        XAResource xa2 = getManagedConnection(conn2).getXAResource();
        QueueSession sess2 = conn2.createQueueSession(false, Session.AUTO_ACKNOWLEDGE);
        QueueSender prod2 = sess2.createSender(dest);

        // Start tran
        Xid xid1 = new XXid();
        Xid xid2 = new XXid();

        xa1.start(xid1, XAResource.TMNOFLAGS);
        xa2.start(xid2, XAResource.TMNOFLAGS);

        // Send in both
        prod1.send(sess1.createTextMessage("1"));
        prod2.send(sess2.createTextMessage("2"));

        // Not committed yet, so msgs should NOT be there
        clearQueue(dest.getQueueName(), 0);

        // Commit
        xa1.end(xid1, XAResource.TMSUCCESS);
        xa1.prepare(xid1);
        xa1.commit(xid1, false);

        xa2.end(xid2, XAResource.TMSUCCESS);
        xa2.prepare(xid2);
        xa2.commit(xid2, false);

// TBD: move up
        conn1.close();
        conn2.close();

        // Check: msgs were committed, so now should be there
        clearQueue(dest.getQueueName(), 2);
    }

//    /**
//     * Purpose: XA should be transparent<br>
//     * Assertion: createQueueSession works identical to createXAQueueSession(); this tests
//     * the STCMS provider<br>
//     * Strategy: Create session, getXAResource(), send msg, commit, read back<br>
//     *
//     * INVALID: ONLY WORKS WITH MANAGED CONNECTIONS
//     *
//     * @throws Throwable on failure of the test
//     */
//    public void invalid_test800q0() throws Throwable {
//        Properties p = getConnectionProperties();
//        STCXAQueueConnectionFactory f = new STCXAQueueConnectionFactory(p);
//        t800qx(f);
//    }

    private void t800qx(QueueConnectionFactory f) throws Throwable {
        {
            QueueConnection conn1 = f.createQueueConnection(USERID, PASSWORD);
            QueueSession sess1 = conn1.createQueueSession(true, 0);
            Queue dest = sess1.createQueue(getQueue1Name());
            XAResource xa1 = getManagedConnection(sess1).getXAResource();
            QueueSender prod1 = sess1.createSender(dest);

            Xid xid1 = new XXid();
            xa1.start(xid1, XAResource.TMNOFLAGS);
            prod1.send(sess1.createTextMessage("1"));
            xa1.end(xid1, XAResource.TMSUCCESS);
            xa1.prepare(xid1);
            xa1.commit(xid1, false);

            conn1.close();

            clearQueue(dest.getQueueName(), 1);
        }

        {
            QueueConnection conn1 = f.createQueueConnection(USERID, PASSWORD);
            QueueSession sess1 = conn1.createQueueSession(false, Session.AUTO_ACKNOWLEDGE);
            Queue dest = sess1.createQueue(getQueue1Name());
            XAResource xa1 = getManagedConnection(sess1).getXAResource();
            QueueSender prod1 = sess1.createSender(dest);

            Xid xid1 = new XXid();
            xa1.start(xid1, XAResource.TMNOFLAGS);
            prod1.send(sess1.createTextMessage("1"));
            xa1.end(xid1, XAResource.TMSUCCESS);
            xa1.prepare(xid1);
            xa1.commit(xid1, false);

            conn1.close();

            clearQueue(dest.getQueueName(), 1);
        }
    }

    /**
     * Purpose: XA should be transparent<br>
     * Assertion: createQueueSession works identical to createXAQueueSession(); <br>
     * Strategy: Create session, getXAResource(), send msg, commit, read back<br>
     *
     * @throws Throwable on failure of the test
     */
    public void test800q1() throws Throwable {
        init(false, true);

        InitialContext ctx = getContext();
        QueueConnectionFactory f = (QueueConnectionFactory) ctx.lookup(appjndiQueue);
        t800qx(f);
        getConnectionManager(f).clear();
    }

    /**
     * Purpose: XA should be transparent<br>
     * Assertion: createTopicSession works identical to createXAQueueSession(); this tests
     * the STCMS provider<br>
     * Strategy: Create session, getXAResource(), send msg, commit, read back<br>
     *
     * INVALID: ONLY WORKS FOR MANAGED CONNECTIONS
     *
     * @throws Throwable on failure of the test
     */
//    public void invalid_test800t0() throws Throwable {
//        Properties p = getConnectionProperties();
//        STCXATopicConnectionFactory f = new STCXATopicConnectionFactory(p);
//        t800tx(f);
//    }

    /**
     * Purpose: XA should be transparent<br>
     * Assertion: createTopicSession works identical to createXAQueueSession(); this tests
     * the STCMS provider<br>
     * Strategy: Create session, getXAResource(), send msg, commit, read back<br>
     *
     * @throws Throwable on failure of the test
     */
    public void test800t1() throws Throwable {
        init(false, true);
        InitialContext ctx = getContext();
        TopicConnectionFactory f = (TopicConnectionFactory) ctx.lookup(appjndiTopic);
        t800tx(f);
    }

    private void t800tx(TopicConnectionFactory f) throws Throwable {
        String destname = getTopic1Name();
        String subname = getDur1Name(destname);
        Topic dest;
        {
            TopicConnection conn1 = f.createTopicConnection(USERID, PASSWORD);
            setClientID(conn1);
            TopicSession sess1 = conn1.createTopicSession(true, 0);
            sess1.getTransacted(); // actuate type
            XAResource xa1 = getManagedConnection(sess1).getXAResource();
            dest = sess1.createTopic(destname);
            sess1.createDurableSubscriber(dest, subname);
            TopicPublisher prod1 = sess1.createPublisher(dest);

            Xid xid1 = new XXid();
            xa1.start(xid1, XAResource.TMNOFLAGS);
            prod1.publish(sess1.createTextMessage("1"));
            xa1.end(xid1, XAResource.TMSUCCESS);
            xa1.prepare(xid1);
            xa1.commit(xid1, false);

            conn1.close();
        }

        {
            TopicConnection conn1 = f.createTopicConnection(USERID, PASSWORD);
            setClientID(conn1);
            TopicSession sess1 = conn1.createTopicSession(false, Session.AUTO_ACKNOWLEDGE);
            sess1.getTransacted(); // actuate type
            XAResource xa1 = getManagedConnection(sess1).getXAResource();
            sess1.createDurableSubscriber(dest, subname);
            TopicPublisher prod1 = sess1.createPublisher(dest);

            Xid xid1 = new XXid();
            xa1.start(xid1, XAResource.TMNOFLAGS);
            prod1.publish(sess1.createTextMessage("1"));
            xa1.end(xid1, XAResource.TMSUCCESS);
            xa1.prepare(xid1);
            xa1.commit(xid1, false);

            conn1.close();

        }
        getConnectionManager(f).clear();
        
        int n = clearTopic(subname, dest.getTopicName(), true);
        assertTrue(n == 2);
    }

    /**
     * Purpose: XA should be transparent<br>
     * Assertion: getTopicSession() should return the same object<br>
     * Strategy: <br>
     *
     * INVALID: XA IS INVISIBLE TO CLIENT APP
     *
     * @throws Throwable on failure of the test
     */
    public void invalid_test801t() throws Throwable {
        init(false, true);
        InitialContext ctx = getContext();
        XATopicConnectionFactory f = (XATopicConnectionFactory) ctx.lookup(appjndiTopic);
        XATopicConnection conn1 = f.createXATopicConnection(USERID, PASSWORD);
        XATopicSession sess1 = conn1.createXATopicSession();
        assertTrue(sess1.getTopicSession() == sess1);
        conn1.close();
    }

    /**
     * Purpose: XA should be transparent<br>
     * Assertion: getQueueSession() should return the same object<br>
     * Strategy: <br>
     *
     * INVALID: XA IS INVISIBLE TO CLIENT APP
     *
     * @throws Throwable on failure of the test
     */
    public void invalid_test801q() throws Throwable {
        init(false, true);
        InitialContext ctx = getContext();
        XAQueueConnectionFactory f = (XAQueueConnectionFactory) ctx.lookup(appjndiQueue);
        XAQueueConnection conn1 = f.createXAQueueConnection(USERID, PASSWORD);
        XAQueueSession sess1 = conn1.createXAQueueSession();
        assertTrue(sess1.getQueueSession() == sess1);
        conn1.close();
    }


    /**
     * Purpose: simple send/receive<br>
     * Assertion: 
     * Strategy: <br>
     *
     * @throws Throwable on failure of the test
     */
    public void testReceive() throws Throwable {
        init(true, true);

        // This is how the client would normally create connections
        InitialContext ctx = getContext();
        QueueConnectionFactory f = (QueueConnectionFactory) ctx.lookup(appjndiQueue);
        setNoXA(f);

        Queue dest;

        // First send two msgs
        {
            QueueConnection conn = f.createQueueConnection(USERID, PASSWORD);

            QueueSession s = conn.createQueueSession(true, 0);
            Message m = s.createTextMessage("x");
            dest = s.createQueue(getQueue1Name());
            QueueSender prod = s.createSender(dest);
            prod.send(m);
            prod.send(m);
            s.commit();
            conn.close();
        }

        // Receive first msg
        {
            QueueConnection conn = f.createQueueConnection(USERID, PASSWORD);
            QueueSession s = conn.createQueueSession(true, 0);
            QueueReceiver cons = s.createReceiver(dest);

            conn.start();
            Message m = cons.receive(EXPECTWITHIN);
            assertTrue(m != null);

            s.commit();
            s.close();
            conn.close();
        }

        // Receive second msg
        {
            QueueConnection conn = f.createQueueConnection(USERID, PASSWORD);
            QueueSession s = conn.createQueueSession(true, 0);
            s.getTransacted(); // actuate type
            QueueReceiver cons = s.createReceiver(dest);

            conn.start();
            Message m = cons.receive(EXPECTWITHIN);
            assertTrue(m != null);
            s.commit();
            s.close();
            conn.close();
        }

        clearQueue(dest.getQueueName(), 0);

        getConnectionManager(f).clear();
    }

    /**
     * Purpose: start/stop of connections<br>
     * Assertion: must be possible to receive msgs; start/stop state should be reset
     * after connection closing<br>
     * Strategy: <br>
     *
     * @throws Throwable on failure of the test
     */
    public void testReceive1() throws Throwable {
        init(true, true);

        WireCount s0 = getConnectionCount();

        // This is how the client would normally create connections
        InitialContext ctx = getContext();
        QueueConnectionFactory f = (QueueConnectionFactory) ctx.lookup(appjndiQueue);
        setNoXA(f);

        Queue dest;

        // First send two msgs
        {
            QueueConnection conn = f.createQueueConnection(USERID, PASSWORD);

            QueueSession s = conn.createQueueSession(true, 0);
            s.getTransacted(); // actuate type
            Message m = s.createTextMessage("x");
            dest = s.createQueue(getQueue1Name());
            QueueSender prod = s.createSender(dest);
            prod.send(m);
            prod.send(m);
            s.commit();
            conn.close();

            // Return to pool
            s0.check(2);
        }

        // Receive first msg
        {
            s0.check(2);
            QueueConnection conn = f.createQueueConnection(USERID, PASSWORD);

            QueueSession s = conn.createQueueSession(true, 0);
            s0.check(2);
            s.getTransacted(); // actuate type
            QueueReceiver cons = s.createReceiver(dest);
            s0.check(3);

            // Connection not started
            Message m = cons.receive(EXPECTWITHIN);
            assertTrue(m == null);

            // Connection started
            conn.start();
            m = cons.receive(EXPECTWITHIN);
            assertTrue(m != null);

            s.commit();
            conn.close();
            s0.check(2);
        }

        // Receive second msg
        {
            s0.check(2);
            QueueConnection conn = f.createQueueConnection(USERID, PASSWORD);

            QueueSession s = conn.createQueueSession(true, 0);
            s0.check(2);
            s.getTransacted(); // actuate type
            QueueReceiver cons = s.createReceiver(dest);
            s0.check(3);

            // Connection not started
            Message m = cons.receive(EXPECTWITHIN);
            assertTrue(m == null);

            // Connection started
            conn.start();
            m = cons.receive(EXPECTWITHIN);
            assertTrue(m != null);

            s.commit();
            conn.close();
            s0.check(2);
        }

        clearQueue(dest.getQueueName(), 0);

        getConnectionManager(f).clear();
        s0.check(0);
    }

    /**
     * Purpose: start/stop of connections<br>
     * Assertion: must be possible to receive msgs; start/stop state should be reset
     * after session closing<br>
     * Strategy: <br>
     *
     * @throws Throwable on failure of the test
     */
    public void testReceive2() throws Throwable {
        init(true, true);

        WireCount s0 = getConnectionCount();

        // This is how the client would normally create connections
        InitialContext ctx = getContext();
        QueueConnectionFactory f = (QueueConnectionFactory) ctx.lookup(appjndiQueue);
        setNoXA(f);

        Queue dest;

        // First send two msgs
        {
            QueueConnection conn = f.createQueueConnection(USERID, PASSWORD);

            QueueSession s = conn.createQueueSession(true, 0);
            s.getTransacted(); // actuate type
            Message m = s.createTextMessage("x");
            dest = s.createQueue(getQueue1Name());
            QueueSender prod = s.createSender(dest);
            prod.send(m);
            prod.send(m);
            s.commit();
            conn.close();

            // Return to pool
            s0.check(2);
        }

        // Receive first msg
        {
            s0.check(2);
            QueueConnection conn = f.createQueueConnection(USERID, PASSWORD);

            QueueSession s = conn.createQueueSession(true, 0);
            s0.check(2);
            s.getTransacted(); // actuate type
            QueueReceiver cons = s.createReceiver(dest);
            s0.check(3);

            // Connection not started
            Message m = cons.receive(EXPECTWITHIN);
            assertTrue(m == null);

            // Connection started
            conn.start();
            m = cons.receive(EXPECTWITHIN);
            assertTrue(m != null);

            s.commit();
            s.close();
            conn.close();
            s0.check(2);
        }

        // Receive second msg
        {
            s0.check(2);
            QueueConnection conn = f.createQueueConnection(USERID, PASSWORD);

            QueueSession s = conn.createQueueSession(true, 0);
            s0.check(2);
            s.getTransacted(); // actuate type
            QueueReceiver cons = s.createReceiver(dest);
            s0.check(3);

            // Connection not started
            Message m = cons.receive(EXPECTWITHIN);
            assertTrue(m == null);

            // Connection started
            conn.start();
            m = cons.receive(EXPECTWITHIN);
            assertTrue(m != null);

            s.commit();
            s.close();
            conn.close();
            s0.check(2);
        }

        clearQueue(dest.getQueueName(), 0);

        getConnectionManager(f).clear();
        s0.check(0);
    }

    /**
     * Purpose: start/stop of connections<br>
     * Assertion: must be possible to receive msgs; start/stop state should be reset
     * after session closing / no closing of connection<br>
     * Strategy: <br>
     *
     * @throws Throwable on failure of the test
     */
    public void testReceive3() throws Throwable {
        init(true, true);

        WireCount s0 = getConnectionCount();

        // This is how the client would normally create connections
        InitialContext ctx = getContext();
        QueueConnectionFactory f = (QueueConnectionFactory) ctx.lookup(appjndiQueue);
        setNoXA(f);

        Queue dest;
        QueueConnection conn = f.createQueueConnection(USERID, PASSWORD);

        // First send two msgs
        {

            QueueSession s = conn.createQueueSession(true, 0);
            s.getTransacted(); // actuate type
            Message m = s.createTextMessage("x");
            dest = s.createQueue(getQueue1Name());
            QueueSender prod = s.createSender(dest);
            prod.send(m);
            prod.send(m);
            s.commit();
            s.close();
            s0.check(2);
        }

        // Receive first msg
        {
            s0.check(2);

            QueueSession s = conn.createQueueSession(true, 0);
            s0.check(2);
            s.getTransacted(); // actuate type
            QueueReceiver cons = s.createReceiver(dest);
            s0.check(3);

            // Connection not started
            Message m = cons.receive(EXPECTWITHIN);
            assertTrue(m == null);

            // Connection started
            conn.start();
            m = cons.receive(EXPECTWITHIN);
            assertTrue(m != null);

            s.commit();
            s.close();
            s0.check(2);
        }

        // Receive second msg
        {
            s0.check(2);

            QueueSession s = conn.createQueueSession(true, 0);
            s0.check(2);
            s.getTransacted(); // actuate type
            QueueReceiver cons = s.createReceiver(dest);
            s0.check(3);

            // Connection still started
            conn.start();
            Message m = cons.receive(EXPECTWITHIN);
            assertTrue(m != null);

            s.commit();
            s.close();
            conn.close();
            s0.check(2);
        }

        clearQueue(dest.getQueueName(), 0);

        getConnectionManager(f).clear();
        s0.check(0);
    }

    public abstract class WireCount {

        public abstract void check(int sessions, int producers, int consumers);

        public abstract void check(int total);
    }

    /**
     * Purpose: <br>
     * Assertion: non transacted session should still be supported in client container<br>
     * Strategy: <br>
     *
     * @throws Throwable on failure of the test
     */
    public void testNonTx1() throws Throwable {
        try {
            System.setProperty(Options.Out.CLIENTCONTAINER,
                "true");
            init(true, true);

            WireCount w = getConnectionCount();

            // This is how the client would normally create connections
            InitialContext ctx = getContext();
            QueueConnectionFactory f = (QueueConnectionFactory) ctx.lookup(appjndiQueue);


            Queue dest;

            // First send two msgs
            {
                QueueConnection conn = f.createQueueConnection(USERID, PASSWORD);

                QueueSession s = conn.createQueueSession(false, Session.AUTO_ACKNOWLEDGE);
                // NO TYPE ACTUATION!
                Message m = s.createTextMessage("x");
                w.check(1, 0, 0);
                dest = s.createQueue(getQueue1Name());
                QueueSender prod = s.createSender(dest);
                w.check(1, 1, 0);
                prod.send(m);
                w.check(2, 2, 0); // transacted twin

                prod.send(m);
                conn.close();

                // Return to pool
                w.check(2, 2, 0);
            }

            // Receive first msg
            {
                w.check(2, 2, 0);
                QueueConnection conn = f.createQueueConnection(USERID, PASSWORD);

                QueueSession s = conn.createQueueSession(false, Session.AUTO_ACKNOWLEDGE);
                w.check(2, 2, 0);
                QueueReceiver cons = s.createReceiver(dest);
                w.check(2, 2, 1);

                // Connection not started
                Message m = cons.receive(EXPECTWITHIN);
                assertTrue(m == null);

                // Connection started
                conn.start();
                m = cons.receive(EXPECTWITHIN);
                assertTrue(m != null);

                conn.close();
                w.check(2, 2, 0);
            }

            // Receive second msg
            {
                w.check(2, 2, 0);
                QueueConnection conn = f.createQueueConnection(USERID, PASSWORD);

                QueueSession s = conn.createQueueSession(false, Session.AUTO_ACKNOWLEDGE);
                w.check(2, 2, 0);
                QueueReceiver cons = s.createReceiver(dest);
                w.check(2, 2, 1);

                // Connection not started
                Message m = cons.receive(EXPECTWITHIN);
                assertTrue(m == null);

                // Connection started
                conn.start();
                m = cons.receive(EXPECTWITHIN);
                assertTrue(m != null);

                conn.close();
                w.check(2, 2, 0);
            }

            clearQueue(dest.getQueueName(), 0);

            getConnectionManager(f).clear();
            w.check(0, 0, 0);
        } finally {
            System.setProperty(Options.Out.CLIENTCONTAINER,
                "false");
        }
    }

    /**
     * Purpose: <br>
     * Assertion: sort-of non transacted session should still be supported when
     * running in app server, and when msg is sent outside of a transaction.<br>
     * Strategy: <br>
     *
     * @throws Throwable on failure of the test
     */
    public void testNonTx2() throws Throwable {
        init(true, true);

        WireCount w = getConnectionCount();

        // This is how the client would normally create connections
        InitialContext ctx = getContext();
        QueueConnectionFactory f = (QueueConnectionFactory) ctx.lookup(appjndiQueue);

        Queue dest;
        String uniquepayload = "x" + System.currentTimeMillis();

        // First send two msgs
        {
            QueueConnection conn = f.createQueueConnection(USERID, PASSWORD);

            QueueSession s = conn.createQueueSession(false, Session.AUTO_ACKNOWLEDGE);
            getManagedConnection(s).getXAResource();

            Message m = s.createTextMessage(uniquepayload);
            w.check(1, 0, 0);
            dest = s.createQueue(getQueue1Name());
            QueueSender prod = s.createSender(dest);
            w.check(1, 1, 0);
            // Send first
            prod.send(m);
            w.check(1, 1, 0);

            // Send second
            prod.send(m);
            conn.close();

            // Return to pool
            w.check(1, 1, 0);
        }

        // Receive first msg
        {
            w.check(1, 1, 0);
            QueueConnection conn = f.createQueueConnection(USERID, PASSWORD);

            QueueSession s = conn.createQueueSession(false, Session.AUTO_ACKNOWLEDGE);
            getManagedConnection(s).getXAResource();
            w.check(1, 1, 0);
            QueueReceiver cons = s.createReceiver(dest);
            w.check(1, 1, 1);

            // Connection not started
            Message m = cons.receive(EXPECTWITHIN);
            assertTrue(m == null);

            // Connection started
            conn.start();
            m = cons.receive(EXPECTWITHIN);
            assertTrue(m != null);
            assertTrue(((TextMessage) m).getText().equals(uniquepayload));

            conn.close();
            w.check(1, 1, 0);
        }

        // Receive second msg
        {
            w.check(1, 1, 0);
            QueueConnection conn = f.createQueueConnection(USERID, PASSWORD);

            QueueSession s = conn.createQueueSession(false, Session.AUTO_ACKNOWLEDGE);
            getManagedConnection(s).getXAResource();
            w.check(1, 1, 0);
            QueueReceiver cons = s.createReceiver(dest);
            w.check(1, 1, 1);

            // Connection not started
            Message m = cons.receive(EXPECTWITHIN);
            assertTrue(m == null);

            // Connection started
            conn.start();
            m = cons.receive(EXPECTWITHIN);
            assertTrue(m != null);

            conn.close();
            w.check(1, 1, 0);
        }

        clearQueue(dest.getQueueName(), 0);

        getConnectionManager(f).clear();
        w.check(0, 0, 0);
    }

    /**
     * Purpose: CTS results<br>
     * Strategy: <br>
     *
     * @throws Throwable on failure of the test
     */
    public void testCtsFailure1() throws Throwable {
        init(true, true);

        // This is how the client would normally create connections
        InitialContext ctx = getContext();
        QueueConnectionFactory f = (QueueConnectionFactory) ctx.lookup(appjndiQueue);
        QueueConnection conn = f.createQueueConnection(USERID, PASSWORD);

        WireCount s0 = getConnectionCount();

        QueueSession s = conn.createQueueSession(true, 0);
        s.getTransacted(); // actuate type

        s0.check(1);

        Queue dummy = null;
        QueueReceiver   qReceiver = null;
        try {
            qReceiver = s.createReceiver(dummy, "TEST");
            assertTrue(qReceiver != null);
            throw new Throwable("Exception not thrown");
        } catch (JMSException expected) {

        }

        conn.close();

        getConnectionManager(f).clear();
        s0.check(0);
    }

    /**
     * Purpose: CTS results<br>
     * Strategy: <br>
     *
     * @throws Throwable on failure of the test
     */
    public void testCtsFailure2() throws Throwable {
        init(true, true);

        // This is how the client would normally create connections
        InitialContext ctx = getContext();
        QueueConnectionFactory f = (QueueConnectionFactory) ctx.lookup(appjndiQueue);
        QueueConnection conn = f.createQueueConnection(USERID, PASSWORD);

        boolean       pass = true;
        Topic         myTopic = null;


        WireCount s0 = getConnectionCount();

        QueueSession s = conn.createQueueSession(true, 0);
        s.getTransacted(); // actuate type


        try {
            s.createDurableSubscriber(myTopic, "cts");
            pass = false;
        } catch (javax.jms.IllegalStateException ex) {
        }

        try {
            s.createDurableSubscriber(myTopic, "cts", "TEST = 'test'", false);
        } catch (javax.jms.IllegalStateException ex) {
        }

        try {
            s.createTemporaryTopic();
        } catch (javax.jms.IllegalStateException ex) {
        }

        try {
            s.createTopic("foo");
        } catch (javax.jms.IllegalStateException ex) {
        }

        try {
            s.unsubscribe("foo");
        } catch (javax.jms.IllegalStateException ex) {
        }
        
        assertTrue(pass);

        conn.close();

        getConnectionManager(f).clear();
        s0.check(0);
    }

    private void logMsg(String msg) {

    }

    private void logTrace(String msg) {

    }


    /**
     * Purpose: CTS results<br>
     * Strategy: <br>
     *
     * @throws Throwable on failure of the test
     */
    public void testCtsFailure3() throws Throwable {
        init(true, true);

        TextMessage messageSent = null;
        TextMessage messageReceived = null;
        String testName = "sendQueueTest2";
        String testMessage = "Just a test from sendQueueTest2";
        MessageProducer msgproducer = null;


        InitialContext ctx = getContext();
        ConnectionFactory f = (ConnectionFactory) ctx.lookup(appjndiQueue);
        setNoXA(f);
        Connection conn = f.createConnection(USERID, PASSWORD);
        Session s = conn.createSession(true, 0);

        Topic defaultDest = s.createTopic(getTopic1Name());
        MessageConsumer defaultConsumer = s.createConsumer(defaultDest);


        // set up test tool for Queue
        msgproducer = s.createProducer((Queue)null);
        conn.start();

        logMsg("Creating 1 message");
        messageSent = s.createTextMessage();
        messageSent.setText(testMessage);
        messageSent.setStringProperty("COM_SUN_JMS_TESTNAME", testName);

        logMsg("Sending message");
        msgproducer.send(defaultDest, messageSent);

        s.commit();

        logMsg("Receiving message");
        messageReceived = (TextMessage) defaultConsumer.receive(EXPECTWITHIN);
        if (messageReceived == null) {
            throw new Exception("didn't get any message");
        }

        // Check to see if correct message received
        if (!messageReceived.getText().equals(messageSent.getText())) {
            throw new Exception("didn't get the right message");
        }

        s.commit();

        conn.close();

        getConnectionManager(f).clear();
    }


    /**
     * Purpose: CTS results<br>
     * Strategy: <br>
     *
     * @throws Throwable on failure of the test
     */
    public void testCtsFailure4() throws Throwable {
        init(true, true);

        InitialContext ctx = getContext();
        QueueConnectionFactory f = (QueueConnectionFactory) ctx.lookup(appjndiQueue);
        QueueConnection conn = f.createQueueConnection(USERID, PASSWORD);

        TextMessage messageSent = null;
        TextMessage messageReceived = null;
        QueueSession qSession = null;
        QueueReceiver qReceiver = null;
        QueueSender qSender = null;

        qSession = conn.createQueueSession(false,
            Session.CLIENT_ACKNOWLEDGE);
        // What this does: new XAQueueConnectionFactory().createXAQueueSession();

        Queue dest = qSession.createQueue(getQueue1Name());
        qReceiver = qSession.createReceiver(dest);
        qSender = qSession.createSender(dest);
        conn.start();

        messageSent = qSession.createTextMessage();
        messageSent.setText("Message from closedQueueConnectionAckTest");
        messageSent.setStringProperty("COM_SUN_JMS_TESTNAME",
            "closedQueueConnectionAckTest");

        // Note that no transaction is started; expected auto-ack behavior
        qSender.send(messageSent);

        messageReceived = (TextMessage) qReceiver.receive(EXPECTWITHIN);
        qReceiver.close();
        qSender.close();
        qSession.close();
        conn.close();

        try {
            if (messageReceived == null) {
                assertTrue(false);
            } else {
                messageReceived.acknowledge();
                assertTrue(false);
            }
        } catch (javax.jms.IllegalStateException is) {
        }
    }

    private static class TestUtil {
        public static void logTrace(String s) {
        }
        public static void logMsg(String s) {
        }
        public static void logErr(String s, Throwable e) {
        }
        public static void logErr(String s) {
        }
    }

    /*
     * @testName: temporaryTopicConnectionClosesTest
     *
     * @assertion_ids: JMS:SPEC:155; JMS:JAVADOC:93;
     *
     * @test_Strategy: Create temporary topic and then close the connection. Verify
     * that the temporary topic closes by trying to send a
     * message to it. The test also sends a blank message to
     * the temporary topic to verify that it is working.
     */
    public void testTemporaryTopicConnectionClosesTest() throws Throwable {
        boolean passed = false;
        InitialContext ctx = getContext();
        TopicConnectionFactory f = (TopicConnectionFactory) ctx.lookup(appjndiTopic);

        // FOR THIS TEST, COMPLETELY BYPASS THE RA
        getRA(f).setOptions(Options.Out.BYPASSRA + "=true");

        try {
            TopicConnection conn = f.createTopicConnection(USERID, PASSWORD);
            TopicSession s = conn.createTopicSession(false, Session.AUTO_ACKNOWLEDGE);

            // create the TemporaryTopic
            TestUtil.logTrace("Creating TemporaryTopic");
            TemporaryTopic  tempT = s.createTemporaryTopic();

            // open a new connection, create Session and Sender
            TestUtil.logTrace("Creating new Connection");
            TopicConnection newTConn = f.createTopicConnection(USERID, PASSWORD);

            TestUtil.logTrace("Create new Session");
            TopicSession    newTSess = newTConn.createTopicSession(false, Session.AUTO_ACKNOWLEDGE);

            TestUtil.logTrace("Create new sender for TemporaryTopic");
            TopicPublisher  newTPublisher = newTSess.createPublisher(tempT);

            // send message to verify TemporaryTopic exists so far
            TestUtil.logTrace("Send message to TemporaryTopic");
            TextMessage tMsg = newTSess.createTextMessage();

            tMsg.setText("test message");
            tMsg.setStringProperty("COM_SUN_JMS_TESTNAME", "temporaryTopicConnectionClosesTest");
            TestUtil.logTrace("TextMessage created. Now publishing");
            newTPublisher.publish(tMsg);

            // close the connection
            TestUtil.logTrace("Close original Connection");
            conn.close();

            // send message to verify TemporaryTopic no longer exists
            TestUtil.logTrace("Send second message to TemporaryTopic. Should fail.");
            try {
                Message tempM = newTSess.createMessage();

                tempM.setStringProperty("COM_SUN_JMS_TESTNAME",
                                        "temporaryTopicConnectionClosesTest");
                newTPublisher.publish(tempM);
            } catch (JMSException e) {
                TestUtil.logMsg("Received expected JMSException");
                TestUtil.logErr("Exception thrown: ", e);
                passed = true;
            }

            // close new connection
            TestUtil.logTrace("Close new TopicConnection");
            newTConn.close();

            // throw exception if test failed
            assertTrue(passed);
        } finally {
            getConnectionManager(f).clear();
        }
    }

    /*
     * @testName: temporaryTopicConnectionClosesTest
     *
     * @assertion_ids: JMS:SPEC:155; JMS:JAVADOC:93;
     *
     * @test_Strategy: Create temporary topic and then close the connection. Verify
     * that the temporary topic closes by trying to send a
     * message to it. The test also sends a blank message to
     * the temporary topic to verify that it is working.
     */
    public void testTemporaryTopicConnectionClosesTestRA() throws Throwable {
        init(true, true);

        boolean passed = false;
        InitialContext ctx = getContext();
        TopicConnectionFactory f = (TopicConnectionFactory) ctx.lookup(appjndiTopic);

        // FOR THIS TEST, TURN ON TEMPORARY DESTINATION CLEANING
        getRA(f).setOptions(Options.Out.STRICT + "=true");

        try {
            TopicConnection conn = f.createTopicConnection(USERID, PASSWORD);
            TopicSession s = conn.createTopicSession(false, Session.AUTO_ACKNOWLEDGE);

            // create the TemporaryTopic
            TestUtil.logTrace("Creating TemporaryTopic");
            TemporaryTopic  topic = s.createTemporaryTopic();

            // open a new connection, create Session and Sender
            TestUtil.logTrace("Creating new Connection");
            TopicConnection newTConn = f.createTopicConnection(USERID, PASSWORD);

            TestUtil.logTrace("Create new Session");
            TopicSession    newTSess = newTConn.createTopicSession(false, Session.AUTO_ACKNOWLEDGE);

            TestUtil.logTrace("Create new sender for TemporaryTopic");
            TopicPublisher  newTPublisher = newTSess.createPublisher(topic);

            // send message to verify TemporaryTopic exists so far
            TestUtil.logTrace("Send message to TemporaryTopic");
            TextMessage tMsg = newTSess.createTextMessage();

            XAResource xa1 = getManagedConnection(newTSess).getXAResource();
            Xid xid = new XXid();
            xa1.start(xid, XAResource.TMNOFLAGS);
            
            tMsg.setText("test message");
            tMsg.setStringProperty("COM_SUN_JMS_TESTNAME", "temporaryTopicConnectionClosesTest");
            TestUtil.logTrace("TextMessage created. Now publishing");
            newTPublisher.publish(tMsg);
            
            xa1.end(xid, XAResource.TMSUCCESS);
            xa1.commit(xid, true);

            // close the connection
            TestUtil.logTrace("Close original Connection");
            conn.close();

            // send message to verify TemporaryTopic no longer exists
            TestUtil.logTrace("Send second message to TemporaryTopic. Should fail.");
            try {
                Message tempM = newTSess.createMessage();

                tempM.setStringProperty("COM_SUN_JMS_TESTNAME",
                                        "temporaryTopicConnectionClosesTest");
                newTPublisher.publish(tempM);
            } catch (JMSException e) {
                TestUtil.logMsg("Received expected JMSException");
                TestUtil.logErr("Exception thrown: ", e);
                passed = true;
            }

            // close new connection
            TestUtil.logTrace("Close new TopicConnection");
            newTConn.close();

            // throw exception if test failed
            assertTrue(passed);
        } catch (JMSException e) {
            throw Exc.rsrcExc(new LocalizedString("Test failure: " + e), e);
        } finally {
            getConnectionManager(f).clear();
        }
    }

    /**
      * @testName:   changeClientIDQueueTest
      *
      * @assertion_ids: JMS:JAVADOC:272;  JMS:SPEC:90; JMS:SPEC:93; JMS:JAVADOC:514; JMS:JAVADOC:512;
      *                 JMS:JAVADOC:650; JMS:JAVADOC:651;
      *
      * @test_Strategy: First make sure the clientID is set, then reset it.
      *                 Verify that the IllegalStateException is thrown.
      *                 1. use a QueueConnectionFactory that has no ClientID set,
      *                    then call setClientID twice;
      */

     public void testChangeClientIDQueueTest() throws Throwable {
         boolean         pass  = false;
         QueueConnection qc = null;

         InitialContext ctx = getContext();
         QueueConnectionFactory f = (QueueConnectionFactory) ctx.lookup(appjndiQueue);
         try {
             qc = f.createQueueConnection(USERID, PASSWORD);
             QueueSession s = qc.createQueueSession(false, Session.AUTO_ACKNOWLEDGE);
             assertTrue(s != null);
             try {
                 logTrace("Setting clientID!");
                 qc.setClientID("ctstest");

                 logTrace("Resetting clientID!");
                 qc.setClientID("changeIt");

                 TestUtil.logErr("Failed: No exception on ClientID reset");
             } catch (InvalidClientIDException e) {
                 TestUtil.logErr("Incorrect exception received: ", e);
             } catch (javax.jms.IllegalStateException ee) {
                 TestUtil.logTrace("Expected Exception received: " + ee.getMessage());
                 pass = true;
             } catch (Exception eee) {
                 TestUtil.logErr("Incorrect exception received: ", eee);
             }
             if (pass == false) {
                throw new Exception("Did not receive expected IllegalStateException");
             }
         } finally {
             getConnectionManager(f).clear();
         }
     }

     public void testCommitAckMsgQueueTest() throws Throwable {
         InitialContext ctx = getContext();
         QueueConnectionFactory f = (QueueConnectionFactory) ctx.lookup(appjndiQueue);
         try {
             TextMessage     mSent = null;
             TextMessage     mReceived = null;
             QueueSession    qSess = null;
             QueueSender     qSender = null;
             QueueReceiver   qRec = null;
             String          msg = "test message for commitAckMsgTest";

             QueueConnection qc = f.createQueueConnection(USERID, PASSWORD);
             setNoXA(f);

             // close default session and create tx CLIENT_ACK session
             qSess = qc.createQueueSession(true, 0);
             Queue dest = qSess.createQueue(getQueue1Name());
             qSender = qSess.createSender(dest);
             qRec = qSess.createReceiver(dest);
             qc.start();

             // send message
             logTrace("Send first message");
             mSent = qSess.createTextMessage();
             mSent.setBooleanProperty("lastMessage", false);
             mSent.setText(msg);
             mSent.setStringProperty("COM_SUN_JMS_TESTNAME", "commitAckMsgQueueTest1");
             qSender.send(mSent);
             qSess.commit();

             logTrace("Send second message");
             mSent.setBooleanProperty("lastMessage", true);
             qSender.send(mSent);
             qSess.commit();

             logTrace("Message sent. Receive with tx session, do not call acknowledge().");
             mReceived = (TextMessage) qRec.receive(EXPECTWITHIN);
             if (mReceived == null) {
                 logMsg("Did not receive message!");
                 throw new Exception("Did not receive message first time");
             }
             logTrace("Received message: \"" + mReceived.getText() + "\"");

             // commit and close session
             logTrace("Call commit() without calling acknowledge().");
             qSess.commit();
             logTrace("Close session and create new one.");
             qSess.close();
// create new (non-tx) session
             qSess = qc.createQueueSession(false,
                     Session.AUTO_ACKNOWLEDGE);
             qSender = qSess.createSender(dest);
             qRec = qSess.createReceiver(dest);

             // check for messages; should receive second message
             mReceived = (TextMessage) qRec.receive(EXPECTWITHIN);
             qc.close();
             if (mReceived == null) {
                 logMsg("Did not receive message!");
                 throw new Exception("Did not receive expected message");
             } else if (mReceived.getBooleanProperty("lastMessage") == false) {
                 logMsg("Received orignal message again. Was not acknowledged by commit().");
                 throw new Exception("Message not acknowledged by commit");
             } else if (mReceived.getBooleanProperty("lastMessage") == true) {
                 logMsg("Pass: received proper message");
             }
         } finally {
             getConnectionManager(f).clear();
         }
     }

    /*
      * @testName:      transactionRollbackOnSessionCloseReceiveTopicTest
      *
      * @assertion_ids: JMS:SPEC:104; JMS:SPEC:166; JMS:SPEC:167;
      *
      * @test_Strategy: Use the default topic session, subscriber and publisher.
      * Set up an additional tx topic session and subscriber.
      * Send and receive a transacted message.
      * Send another transacted message, but close the session after receive()
      * with no commit.  Verify that the message is not received by receiving
      * again with new session.
      */
     public void transactionRollbackOnSessionCloseReceiveTopicTest() throws Throwable {
         InitialContext ctx = getContext();
         TopicConnectionFactory f = (TopicConnectionFactory) ctx.lookup(appjndiTopic);

         try {
             TextMessage messageSent = null;
             TextMessage messageReceived = null;
             String subscriptionName = "TxTopicTestSubscription";

             TopicConnection conn1 = f.createTopicConnection(USERID, PASSWORD);
             TopicConnection conn2 = f.createTopicConnection(USERID, PASSWORD);

             // Send on 1, receive on 1; then send on 1, receive on 1, close 1, receive on 2


             TopicSession newTopicSess = conn2.createTopicSession(true, 0);
             TopicSession s1 = conn1.createTopicSession(true, 0);

             Topic dest = newTopicSess.createTopic(getTopic1Name());

             TopicPublisher prod1 = s1.createPublisher(dest);

             TopicSubscriber newSubscriber =
                 newTopicSess.createDurableSubscriber(dest,
                 subscriptionName);

             logTrace("Start connection");
             conn1.start();
             conn2.start();

             // send on 1 and receive message on 2
             messageSent = s1.createTextMessage();
             messageSent.setBooleanProperty("lastMessage", false);
             messageSent.setText("transaction message test");
             messageSent.setStringProperty("COM_SUN_JMS_TESTNAME",
                 "transactionRollbackOnSessionCloseReceiveTopicTest");
             prod1.publish(messageSent);
             s1.commit();

             messageReceived =
                 (TextMessage) newSubscriber.receive(EXPECTWITHIN);
             newTopicSess.commit();

             // Check to see if correct message received
             if (messageReceived.getText().equals(messageSent.getText())) {
                 logTrace("Message text: \"" + messageReceived.getText() + "\"");
                 logTrace("Received correct message");
             } else {
                 throw new Exception("didn't get the right message");
             }


             // Send another message
             messageSent.setBooleanProperty("lastMessage", true);
             prod1.publish(messageSent);
             s1.commit();

             // receive, close the session without doing the commit.
             logTrace("Receive message and call close()");
             messageReceived = (TextMessage) newSubscriber.receive(EXPECTWITHIN);
             newTopicSess.close();


             logTrace("Create new session and attempt to receive message");
             newTopicSess = conn2.createTopicSession(true, 0);
             newSubscriber =
                 newTopicSess.createDurableSubscriber(dest,
                 subscriptionName);
             messageReceived = (TextMessage) newSubscriber.receive(EXPECTWITHIN);
             newTopicSess.commit();

             newSubscriber.close();
             newTopicSess.unsubscribe(subscriptionName);

             // check message
             if (messageReceived == null) {
                 throw new Exception("Fail: Should have received message");
             } else if (messageReceived.getBooleanProperty("lastMessage") == true) {
                 logTrace("Pass: received message again, previous tx was rolled back");
             }
         } finally {
             getConnectionManager(f).clear();
         }
     }

     /*
      * @testName:      rollbackRecoverTopicTest
      *
      * @assertion_ids: JMS:SPEC:130;
      *
      * @test_Strategy: Create tx_session. Receive one message from a topic and
      * call rollback. Attempt to receive message again.
      */
     public void rollbackRecoverTopicTest() throws Throwable {
         InitialContext ctx = getContext();
         TopicConnectionFactory f = (TopicConnectionFactory) ctx.lookup(appjndiTopic);

         try {
             TextMessage     mSent = null;
             TextMessage     mReceived = null;
             TopicSession    tSess = null;
             TopicPublisher  tPub = null;
             TopicSubscriber tSub = null;
             String          msg = "test message for rollbackRecoverTest";

             // close default session and create tx CLIENT_ACK session
             TopicConnection conn1 = f.createTopicConnection(USERID, PASSWORD);
             tSess = conn1.createTopicSession(true, 0);
             Topic dest = tSess.createTopic(getTopic1Name());
             tPub = tSess.createPublisher(dest);
             tSub = tSess.createSubscriber(dest);
             tSess.close();

             tSess = conn1.createTopicSession(true, 0);
             tPub = tSess.createPublisher(dest);
             tSub = tSess.createSubscriber(dest);

             conn1.start();

             // send message
             mSent = tSess.createTextMessage();
             mSent.setText(msg);
             mSent.setStringProperty("COM_SUN_JMS_TESTNAME", "rollbackRecoverTopicTest");
             tPub.publish(mSent);
             tSess.commit();

             // receive message
             logTrace("Message sent. Receive with tx session, do not acknowledge.");
             mReceived = (TextMessage) tSub.receive(EXPECTWITHIN);
             if (mReceived == null) {
                 logMsg("Did not receive message!");
                 throw new Exception("Did not receive message first time");
             }
             logTrace("Received message: \"" + mReceived.getText() + "\"");

             // rollback session
             logTrace("Call rollback() without acknowledging message.");
             tSess.rollback();

             // check for messages; should receive one
             logTrace("Attempt to receive message again");
             mReceived = (TextMessage) tSub.receive(EXPECTWITHIN);
             if (mReceived == null) {
                 logMsg("Did not receive message!");
                 throw new Exception("Did not receive expected message");
             }
             if (mReceived.getText().equals(msg)) {
                 logMsg("Received orignal message again. Was not acknowledged.");
             } else {
                 throw new Exception("Received unexpected message");
             }
         } finally {
             getConnectionManager(f).clear();
         }
     }

     /*
       * @testName:   changeClientIDQueueTest
       *
       * @assertion_ids: JMS:JAVADOC:272;  JMS:SPEC:90; JMS:SPEC:93; JMS:JAVADOC:514; JMS:JAVADOC:512;
       *                 JMS:JAVADOC:650; JMS:JAVADOC:651;
       *
       * @test_Strategy: First make sure the clientID is set, then reset it.
       *                 Verify that the IllegalStateException is thrown.
       *                 1. use a QueueConnectionFactory that has no ClientID set,
       *                    then call setClientID twice;
       */

    public void testChangeClientIDQueueTest2() throws Throwable {
        InitialContext ctx = getContext();
        QueueConnectionFactory f = (QueueConnectionFactory) ctx.lookup(appjndiQueue);
        boolean pass = false;
        QueueConnection qc = null;

        try {
            try {
                qc = f.createQueueConnection(USERID, PASSWORD);

                logTrace("Setting clientID!");
                qc.setClientID("ctstest");

                logTrace("Resetting clientID!");
                qc.setClientID("changeIt");

                TestUtil.logErr("Failed: No exception on ClientID reset");
            } catch (InvalidClientIDException e) {
                TestUtil.logErr("Incorrect exception received: ", e);
            } catch (javax.jms.IllegalStateException ee) {
                TestUtil.logTrace("Expected Exception received: " + ee.getMessage());
                pass = true;
            } catch (Exception eee) {
                TestUtil.logErr("Incorrect exception received: ", eee);
            }
            if (pass == false) {
                throw new Exception("Did not receive expected IllegalStateException");
            }
        } finally {
            getConnectionManager(f).clear();
        }
    }

    /**
     * Purpose: XA<br>
     * Assertion: Should be possible to call close on session in mid transaction<br>
     * Strategy: Create XA sessions, send msg, commit, read back<br>
     *
     *
     * @throws Throwable on failure of the test
     */
    public void testDanglingXARecv() throws Throwable {
        init(true, true);

        WireCount w = getConnectionCount();

        // This is how the client would normally create connections
        InitialContext ctx = getContext();
        QueueConnectionFactory f = (QueueConnectionFactory) ctx.lookup(appjndiQueue);
        Queue dest;

        // SEND
        {
            QueueConnection conn1 = f.createQueueConnection(USERID, PASSWORD);
            QueueSession sess1 = conn1.createQueueSession(true, 0);
            dest = sess1.createQueue(getQueue1Name());
            w.check(1, 0, 0);
            XAResource xa1 = getManagedConnection(sess1).getXAResource();
            QueueSender prod1 = sess1.createSender(dest);
            w.check(1, 1, 0);

            // Start tran
            Xid xid1 = new XXid();

            xa1.start(xid1, XAResource.TMNOFLAGS);

            // Send
            prod1.send(sess1.createTextMessage("1"));
            conn1.close();
            w.check(1, 1, 0);

            // Commit
            xa1.end(xid1, XAResource.TMSUCCESS);
            xa1.prepare(xid1);
            xa1.commit(xid1, false);
        }

        // RECEIVE
        {
            QueueConnection conn1 = f.createQueueConnection(USERID, PASSWORD);
            QueueSession sess1 = conn1.createQueueSession(true, 0);
            w.check(1, 1, 0);
            XAResource xa1 = getManagedConnection(sess1).getXAResource();
            QueueReceiver prod1 = sess1.createReceiver(dest);
            w.check(1, 1, 1);
            conn1.start();

            // Start tran
            Xid xid1 = new XXid();

            xa1.start(xid1, XAResource.TMNOFLAGS);

            // Send
            Message m = prod1.receive(EXPECTWITHIN);
            assertTrue(m != null);
            conn1.close();
            w.check(1, 1, 1);

            // Commit
            xa1.end(xid1, XAResource.TMSUCCESS);
            xa1.prepare(xid1);
            xa1.commit(xid1, false);
            w.check(1, 1, 0);
        }

        getConnectionManager(f).clear();

        // Check
        clearQueue(dest.getQueueName(), 0);

        getConnectionManager(f).clear();
        w.check(0, 0, 0);
    }

    /**
     * Purpose: XA<br>
     * Assertion: an xa session should auto-commit sent messages when not within a
     * transaction<br>
     *
     * @throws Throwable on failure of the test
     */
    public void testXAOutOfTx() throws Throwable {
        init(false, true);

        // This is how the client would normally create connections
        InitialContext ctx = getContext();
        QueueConnectionFactory f = (QueueConnectionFactory) ctx.lookup(appjndiQueue);
        QueueConnection conn1 = f.createQueueConnection(USERID, PASSWORD);

        // Session etc.
        QueueSession sess1 = conn1.createQueueSession(false, 0);
        Queue dest = sess1.createQueue(getQueue1Name());
        XAResource xa1 = getManagedConnection(sess1).getXAResource();
        QueueSender prod1 = sess1.createSender(dest);

        // Send with rollback
        {
            // Start tran
            Xid xid1 = new XXid();
            xa1.start(xid1, XAResource.TMNOFLAGS);

            // Send
            prod1.send(sess1.createTextMessage("1"));

            // Rollback
            xa1.end(xid1, XAResource.TMSUCCESS);
            xa1.prepare(xid1);
            xa1.rollback(xid1);

            clearQueue(dest.getQueueName(), 0);
        }

        // Send with commit
        {
            // Start tran
            Xid xid1 = new XXid();
            xa1.start(xid1, XAResource.TMNOFLAGS);

            // Send
            prod1.send(sess1.createTextMessage("1"));

            // Commit
            xa1.end(xid1, XAResource.TMSUCCESS);
            xa1.prepare(xid1);
            xa1.commit(xid1, false);

            clearQueue(dest.getQueueName(), 1);
        }

        // Send outside of tx
        {
            prod1.send(sess1.createTextMessage("1"));
            clearQueue(dest.getQueueName(), 1);
        }

        // Send with commit
        {
            // Start tran
            Xid xid1 = new XXid();
            xa1.start(xid1, XAResource.TMNOFLAGS);

            // Send
            prod1.send(sess1.createTextMessage("1"));

            // Commit
            xa1.end(xid1, XAResource.TMSUCCESS);
            xa1.prepare(xid1);
            xa1.commit(xid1, false);

            clearQueue(dest.getQueueName(), 1);
        }

        conn1.close();

        getConnectionManager(f).clear();
    }

    /**
     * Purpose: XA retention: WAS does not call getXAResource() more than once ever<br>
     * Assertion: <br>
     * Strategy: create session, use xaresource and store xaresource, close, create
     * new, use the stored xaresource<br>
     *
     * @throws Throwable on failure of the test
     */
    public void testCacheXAResource() throws Throwable {
        init(false, true);

        // This is how the client would normally create connections
        InitialContext ctx = getContext();
        QueueConnectionFactory f = (QueueConnectionFactory) ctx.lookup(appjndiQueue);

        XAResource xa1;

        WireCount w = getConnectionCount();

        Queue dest;

        // First
        {
            QueueConnection conn1 = f.createQueueConnection(USERID, PASSWORD);
            QueueSession sess1 = conn1.createQueueSession(true, 0);
            dest = sess1.createQueue(getQueue1Name());
            xa1 = getManagedConnection(sess1).getXAResource();
            QueueSender prod1 = sess1.createSender(dest);

            // Start tran
            Xid xid1 = new XXid();
            xa1.start(xid1, XAResource.TMNOFLAGS);

            prod1.send(sess1.createTextMessage("1"));

            w.check(1, 1, 0);

            // Client app closes
            conn1.close();

            w.check(1, 1, 0);

            // Commit
            xa1.end(xid1, XAResource.TMSUCCESS);
            xa1.prepare(xid1);
            xa1.commit(xid1, false);

            // Check that msg was sent
            clearQueue(dest.getQueueName(), 1);
        }

        // Client repeats:
        {
            QueueConnection conn1 = f.createQueueConnection(USERID, PASSWORD);
            w.check(1, 1, 0);

            QueueSession sess1 = conn1.createQueueSession(true, 0);
            // Do NOT call getXAResource()
            QueueSender prod1 = sess1.createSender(dest);

            // Start tran
            Xid xid1 = new XXid();
            xa1.start(xid1, XAResource.TMNOFLAGS);

            prod1.send(sess1.createTextMessage("1"));

            w.check(1, 1, 0);

            // Client app closes
            conn1.close();

            w.check(1, 1, 0);

            // Commit
            xa1.end(xid1, XAResource.TMSUCCESS);
            xa1.prepare(xid1);
            xa1.commit(xid1, false);

            // Check that msg was sent
            clearQueue(dest.getQueueName(), 1);
        }

        getConnectionManager(f).clear();

        w.check(0, 0, 0);
    }

    /**
     * Purpose: Connection reuse within same transaction (default connection manager)
     * Assertion: <br>
     * Strategy: create session, use it, close, create another one (should reuse open
     * session within this transaction), commit<br>
     *
     * @throws Throwable on failure of the test
     */
    public void testWAS1() throws Throwable {
        init(false, true);

        // This is how the client would normally create connections
        InitialContext ctx = getContext();
        QueueConnectionFactory f = (QueueConnectionFactory) ctx.lookup(appjndiQueue);

        XAResource xa1;

        WireCount w = getConnectionCount();

        {
            QueueConnection conn1 = f.createQueueConnection(USERID, PASSWORD);
            QueueSession sess1 = conn1.createQueueSession(true, 0);
            Queue dest = sess1.createQueue(getQueue1Name());
            xa1 = getManagedConnection(sess1).getXAResource();
            QueueSender prod1 = sess1.createSender(dest);

            // Start tran
            Xid xid1 = new XXid();
            xa1.start(xid1, XAResource.TMNOFLAGS);

            prod1.send(sess1.createTextMessage("1"));

            w.check(1, 1, 0);

            // Client app closes
            sess1.close();

            w.check(1, 1, 0);

            // Reuse same connection within same transaction
            sess1 = conn1.createQueueSession(true, 0);
            prod1 = sess1.createSender(dest);
            prod1.send(sess1.createTextMessage("1"));

            w.check(1, 1, 0);

            conn1.close();

            w.check(1, 1, 0);

            // Commit
            xa1.end(xid1, XAResource.TMSUCCESS);
            xa1.prepare(xid1);
            xa1.commit(xid1, false);

            // Check that msg was sent
            clearQueue(dest.getQueueName(), 2);
        }


        getConnectionManager(f).clear();

        w.check(0, 0, 0);
    }

    /**
     * Purpose: Connection reuse within same transaction where the app server does NOT
     * call cleanup upon closing the connection by the application.
     * Assertion: <br>
     * Strategy: Create session; pretend to close (i.e. return to pool only);
     * create session (= reuse open session)<br>
     *
     * THIS SIMULATES WAS6
     *
     * @throws Throwable on failure of the test
     */
    public void testWAS2() throws Throwable {
        init(false, true);

        // This is how the client would normally create connections
        InitialContext ctx = getContext();
        QueueConnectionFactory f = (QueueConnectionFactory) ctx.lookup(appjndiQueue);

        XAResource xa1;
        ManagedConnection m;

        WireCount w = getConnectionCount();

        QueueConnection conn1 = f.createQueueConnection(USERID, PASSWORD);
        Xid xid1 = new XXid();
        Queue dest;

        // First
        {
            QueueSession sess1 = conn1.createQueueSession(true, 0);
            dest = sess1.createQueue(getQueue1Name());
            xa1 = getManagedConnection(sess1).getXAResource();
            QueueSender prod1 = sess1.createSender(dest);
            m = getManagedConnection(sess1);

            // Start tran
            xa1.start(xid1, XAResource.TMNOFLAGS);

            prod1.send(sess1.createTextMessage("1"));

            // Simulate CLOSE by making session available for reuse
            getConnectionManager(f).testAddToPool(m);

            w.check(1, 1, 0);
        }

        // Client repeats:
        {
            w.check(1, 1, 0);
            QueueSession sess1 = conn1.createQueueSession(true, 0);
            w.check(1, 1, 0);
            QueueSender prod1 = sess1.createSender(dest);
            // Will NOT reuse open producer (currently in use)
            w.check(1, 2, 0);

            prod1.send(sess1.createTextMessage("1"));

            w.check(1, 2, 0);

            // Client app closes
            conn1.close();

            w.check(1, 2, 0);

            // Commit
            xa1.end(xid1, XAResource.TMSUCCESS);
            xa1.prepare(xid1);
            xa1.commit(xid1, false);

            // Check that msg was sent
            clearQueue(dest.getQueueName(), 2);
        }

        getConnectionManager(f).clear();

        w.check(0, 0, 0);
    }

    public void testCTSFailure5() throws Throwable {
        init(false, true);

        // This is how the client would normally create connections
        InitialContext ctx = getContext();
        QueueConnectionFactory f = (QueueConnectionFactory) ctx.lookup(appjndiQueue);

        XAResource xa1;

        WireCount w = getConnectionCount();

        for (int i = 0; i < 3; i++) {
            QueueConnection conn1 = f.createQueueConnection(USERID, PASSWORD);
            Queue tempQ;
            QueueReceiver qReceiver;
            Message msg;
            QueueSender qSender;

            QueueSession session = conn1.createQueueSession(false,
                Session.AUTO_ACKNOWLEDGE);
            xa1 = getManagedConnection(session).getXAResource();
            assertTrue(xa1 != null);

            tempQ = session.createTemporaryQueue();

            qReceiver = session.createReceiver(tempQ);

            // SEND
            msg = session.createMessage();
            msg.setBooleanProperty("second_message", false);
            qSender = session.createSender(tempQ);
            qSender.send(msg);

            // SEND
            msg = session.createMessage();
            msg.setBooleanProperty("second_message", true);
            qSender = session.createSender(tempQ);
            qSender.send(msg); // <<< FAILS ON SECOND ITERATION

            // RECEIVE
            qReceiver.receive(EXPECTWITHIN);

            conn1.close();
        }

        getConnectionManager(f).clear();
        w.check(0, 0, 0);
    }

    /**
     * tempQueueNotConsumableTest
     * 
     * Purpose: CTS tempQueueNotConsumableTest<br>
     * Assertion: a temp dest should not be usable in a different connection<br>
     *
     * @throws Throwable on failure of the test
     */
    public void testTempQueueNotConsumableTest() throws Throwable {
        init(false, true);

        // This is how the client would normally create connections
        InitialContext ctx = getContext();
        QueueConnectionFactory f = (QueueConnectionFactory) ctx.lookup(appjndiQueue);

        WireCount w = getConnectionCount();

        Queue tempqueue;
        {
            // Create a temp dest in conn1
            QueueConnection conn1 = f.createQueueConnection(USERID, PASSWORD);
            QueueSession sess1 = conn1.createQueueSession(false, 0);
            tempqueue = sess1.createTemporaryQueue();
            sess1.createReceiver(tempqueue);
            w.check(1, 0, 1);

            // Close conn1
            conn1.close();
            w.check(2, 0, 0);
        }

        {
            // Create conn2; this should reuse conn1 underneath; reusing the
            // temp dest
            // should fail
            QueueConnection conn2 = f.createQueueConnection(USERID, PASSWORD);
            QueueSession sess1 = conn2.createQueueSession(false, 0);
            try {
                sess1.createReceiver(tempqueue);
                throw new Exception("Exception expected");
            } catch (JMSException expected) {
                // expected
            }
            try {
                sess1.createReceiver(tempqueue, "a = 1");
                throw new Exception("Exception expected");
            } catch (JMSException expected) {
                // expected
            }
            try {
                sess1.createConsumer(tempqueue);
                throw new Exception("Exception expected");
            } catch (JMSException expected) {
                // expected
            }
            try {
                sess1.createConsumer(tempqueue, "a = 1");
                throw new Exception("Exception expected");
            } catch (JMSException expected) {
                // expected
            }
            try {
                sess1.createConsumer(tempqueue, "a = 1", true);
                throw new Exception("Exception expected");
            } catch (JMSException expected) {
                // expected
            }
            w.check(2, 0, 0);
            conn2.close();
        }

        // Cleanup
        getConnectionManager(f).clear();
        w.check(0, 0, 0);
    }

    /**
     * tempTopicNotConsumableTest
     * 
     * Purpose: CTS tempQueueNotConsumableTest<br>
     * Assertion: a temp dest should not be usable in a different connection<br>
     *
     * @throws Throwable on failure of the test
     */
    public void testTempTopicNotConsumableTest() throws Throwable {
        init(false, true);

        // This is how the client would normally create connections
        InitialContext ctx = getContext();
        TopicConnectionFactory f = (TopicConnectionFactory) ctx.lookup(appjndiTopic);

        WireCount w = getConnectionCount();

        Topic temptopic;
        {
            // Create a temp dest in conn1
            TopicConnection conn1 = f.createTopicConnection(USERID, PASSWORD);
            TopicSession sess1 = conn1.createTopicSession(false, 0);
            temptopic = sess1.createTemporaryTopic();
            sess1.createSubscriber(temptopic);
            w.check(1, 0, 1);

            // Close conn1
            conn1.close();
            w.check(2, 0, 0);
        }

        {
            // Create conn2; this should reuse conn1 underneath; reusing the
            // temp dest
            // should fail
            TopicConnection conn2 = f.createTopicConnection(USERID, PASSWORD);
            TopicSession sess1 = conn2.createTopicSession(false, 0);
            try {
                sess1.createSubscriber(temptopic);
                throw new Exception("Exception expected");
            } catch (JMSException expected) {
                // expected
            }
            try {
                sess1.createSubscriber(temptopic, "a = 1", true);
                throw new Exception("Exception expected");
            } catch (JMSException expected) {
                // expected
            }
            try {
                sess1.createConsumer(temptopic);
                throw new Exception("Exception expected");
            } catch (JMSException expected) {
                // expected
            }
            try {
                sess1.createConsumer(temptopic, "a = 1");
                throw new Exception("Exception expected");
            } catch (JMSException expected) {
                // expected
            }
            try {
                sess1.createConsumer(temptopic, "a = 1", true);
                throw new Exception("Exception expected");
            } catch (JMSException expected) {
                // expected
            }
            try {
                sess1.createDurableSubscriber(temptopic, "name");
                throw new Exception("Exception expected");
            } catch (JMSException expected) {
                // expected
            }
            try {
                sess1.createDurableSubscriber(temptopic, "name", "a=1", true);
                throw new Exception("Exception expected");
            } catch (JMSException expected) {
                // expected
            }
            w.check(2, 0, 0);
            conn2.close();
        }

        // Cleanup
        getConnectionManager(f).clear();
        w.check(0, 0, 0);
    }

    /**
     *
     *
     * @throws Throwable on failure of the test
     */
    public void testAdminQueue() throws Throwable {
        init(true, true);

        WireCount w = getConnectionCount();

        // This is how the client would normally create connections
        InitialContext ctx = getContext();
        QueueConnectionFactory f = (QueueConnectionFactory) ctx.lookup(appjndiQueue);
        AdminQueue dest = new AdminQueue();
        dest.setName("Queue1");
        clearQueue("Queue1", -1);

        // SEND
        {
            QueueConnection conn1 = f.createQueueConnection(USERID, PASSWORD);
            QueueSession sess1 = conn1.createQueueSession(true, 0);
            w.check(1, 0, 0);
            XAResource xa1 = getManagedConnection(sess1).getXAResource();
            QueueSender prod1 = sess1.createSender(dest);
            w.check(1, 1, 0);

            // Start tran
            Xid xid1 = new XXid();

            xa1.start(xid1, XAResource.TMNOFLAGS);

            // Send
            prod1.send(sess1.createTextMessage("1"));
            conn1.close();
            w.check(1, 1, 0);

            // Commit
            xa1.end(xid1, XAResource.TMSUCCESS);
            xa1.prepare(xid1);
            xa1.commit(xid1, false);
        }

        // RECEIVE
        {
            QueueConnection conn1 = f.createQueueConnection(USERID, PASSWORD);
            QueueSession sess1 = conn1.createQueueSession(true, 0);
            w.check(1, 1, 0);
            XAResource xa1 = getManagedConnection(sess1).getXAResource();
            QueueReceiver prod1 = sess1.createReceiver(dest);
            w.check(1, 1, 1);
            conn1.start();

            // Start tran
            Xid xid1 = new XXid();

            xa1.start(xid1, XAResource.TMNOFLAGS);

            // Send
            Message m = prod1.receive(EXPECTWITHIN);
            assertTrue(m != null);
            conn1.close();
            w.check(1, 1, 1);

            // Commit
            xa1.end(xid1, XAResource.TMSUCCESS);
            xa1.prepare(xid1);
            xa1.commit(xid1, false);
            w.check(1, 1, 0);
        }

        getConnectionManager(f).clear();

        // Check
        clearQueue(dest.getQueueName(), 0);

        getConnectionManager(f).clear();
        w.check(0, 0, 0);
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
     * Verify that batch size in createConnectionConsumer() works
     * 
     * @throws Throwable
     */
    public void testXACCBatch() throws Throwable {
        // Connection factory
        XAQueueConnectionFactory fact = getXAQueueConnectionFactory();
        
        final int N = 1000;
        final int K = 21;
        final int[] max = new int[1];
        
        // Send msgs to Queue1
        Queue dest;
        {
            final QueueConnection conn = fact.createQueueConnection(USERID, PASSWORD);
            QueueSession session = conn.createQueueSession(false, Session.AUTO_ACKNOWLEDGE);
            dest = session.createQueue(getQueue1Name());
            QueueSender producer = session.createSender(dest);
            for (int i = 0; i < N; i++) {
                TextMessage msg1 = session.createTextMessage("Msg " + i + " for Q1");
                producer.send(msg1);
            }
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
                    int nMsgs;
                    public Session getSession() throws JMSException {
                        try {
                            xs = mdbconn.createXAQueueSession();
                            xs.getQueueSession().setMessageListener(new MessageListener() {
                                public void onMessage(Message msg) {
                                    nMsgs++;
                                }
                            });
                            return xs;
                        } catch (Exception e) {
                            setAsyncError(e);
                            throw new JMSException("Error: " + e);
                        } 
                    }
                    public void start() throws JMSException {
                        new Thread() {
                            @Override
                            public void run() {
                                try {
                                    xid = new XXid();
                                    xs.getXAResource().start(xid, XAResource.TMNOFLAGS);
                                    xs.run();
                                    synchronized (max) {
                                        if (max[0] < nMsgs) {
                                            max[0] = nMsgs;
                                        }
                                    }
                                    xs.getXAResource().end(xid, XAResource.TMSUCCESS);
                                    xs.getXAResource().commit(xid, true);
                                    for (int i = 0; i < nMsgs; i++) {
                                        sync.release();
                                    }
                                    xs.close();
                                } catch (Exception e) {
                                    sLog.log(Level.SEVERE, "Unexpected " + e, e);
                                    setAsyncError(e);
                                }
                            }
                        }.start();
                    }
                };
            }
        };

        // Create connection consumer and start
        mdbconn.createConnectionConsumer(dest, null, pool, K);
        mdbconn.start();
        for (int i = 0; i < N; i++) {
            assertTrue(sync.attempt(2000));
        }
        mdbconn.close();
        
        if (max[0] != K) {
            System.out.println(max[0] + " != " + K);
        }
        assertTrue(max[0] == K);
        
        assertNoAsyncErrors();
    }


    /**
     * Implements the stop procedure for XA and connection consumers as proposed by
     * George: close the connection consumer, wait until all server sessions have been
     * returned, and close the connection. Calls to getServerSession() during the 
     * shutdown procedure should throw an execption. 
     * 
     * This test tests message loss when going through the shut down procedure while
     * processing messages. Messages are all rolled back five times, and then a wait
     * is invoked. In the end there should be as many messages in the queue as was
     * sent to this queue.
     */
    public int doTestXACCStopCloseRolback() throws Throwable {
        final int POOLSIZE = 32;

        // Connection factory
        XAQueueConnectionFactory fact = getXAQueueConnectionFactory();
        
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
                                        // Delist (simulates going from MDB to SLSB)
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

                                        // Re-enlist (simulates returning from SLSB to MDB)
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
                            @Override
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
                        try {
                            isDead.waitForUp(2, 100000);
                        } catch (Exception e) {
                            throw new JMSException("Timeout1");
                        }
                    }

                    if (isDead.current() == 2) {
                        throw new JMSException("EXPECTED: Cannot return ServerSession: isDead=2");
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
        mdbconn.createConnectionConsumer(dest, null, pool, 1);
        mdbconn.start();
        
        // Wait until message processing is fully going
        rolledback.waitForUp(5, 30000);
        System.out.println("All threads are waiting: " + waiting.current());
        
        // Shutdown procedure:
        // 1: getServerSession() blocks
        isDead.inc();
        
        // 2: wait until all sessions have returned
        System.out.println("Triggering end");
        endWaiter.inc();
        System.out.println("Waiting for inuse=0");
        inUse.waitForDown(0, 3000);
        
        // 3: getServerSession() unblocks and returns an exception
        isDead.inc();
        
        // 4: close the connection
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
                Message m = r.receive(1000);
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

    /**
     * Purpose: simple send/receive<br>
     * Assertion: 
     * Strategy: <br>
     *
     * @throws Throwable on failure of the test
     */
    public void testRequestReplyTopic() throws Throwable {
        init(true, true);

        // This is how the client would normally create connections
        InitialContext ctx = getContext();
        TopicConnectionFactory f = (TopicConnectionFactory) ctx.lookup(appjndiTopic);
        setNoXA(f);

        // First send a request msg
        TopicSubscriber receiver; 
        Topic dest;
        TopicSession s;
        TopicConnection conn = f.createTopicConnection(USERID, PASSWORD);
        conn.start();

        TopicConnection conn2 = f.createTopicConnection(USERID, PASSWORD);
        conn2.start();
        TopicSession s2 = conn2.createTopicSession(true, 0);
        dest = s2.createTopic("TOPICX1");
        TopicSubscriber cons = s2.createSubscriber(dest);
        
        {
            s = conn.createTopicSession(true, 0);
            Message m = s.createTextMessage("x");
            TemporaryTopic temporaryTopic = s.createTemporaryTopic();
            m.setJMSReplyTo(temporaryTopic);
            TopicPublisher prod = s.createPublisher(dest);
            receiver = s.createSubscriber(temporaryTopic);
            prod.send(m);
            s.commit();
        }
        
        // Receive request and send two replies
        {
            Message m = cons.receive(EXPECTWITHIN);
            assertTrue(m != null);
            
            Topic tempdest = (Topic) m.getJMSReplyTo();
            TopicPublisher sender = s2.createPublisher(tempdest);
            sender.send(s2.createTextMessage("Reply1"));
            sender.send(s2.createTextMessage("Reply2"));

            s2.commit();
            s2.close();
            conn2.close();
        }

        // Receive reply
        {
            Message m1 = receiver.receive(EXPECTWITHIN);
            assertTrue(m1 != null);
            Message m2 = receiver.receive(EXPECTWITHIN);
            assertTrue(m2 != null);
            s.commit();
            s.close();
            conn.close();
        }

        getConnectionManager(f).clear();
    }
}
