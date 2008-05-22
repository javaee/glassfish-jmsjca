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

import com.stc.jmsjca.core.Delivery;
import com.stc.jmsjca.core.EndOfBatchMessage;
import com.stc.jmsjca.core.JConnectionFactory;
import com.stc.jmsjca.core.Options;
import com.stc.jmsjca.core.Unwrappable;
import com.stc.jmsjca.core.WMessageIn;
import com.stc.jmsjca.util.Logger;

import javax.ejb.EJBException;
import javax.ejb.MessageDrivenBean;
import javax.ejb.MessageDrivenContext;
import javax.jms.BytesMessage;
import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.MapMessage;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.MessageProducer;
import javax.jms.ObjectMessage;
import javax.jms.Queue;
import javax.jms.QueueConnection;
import javax.jms.QueueConnectionFactory;
import javax.jms.QueueReceiver;
import javax.jms.QueueSender;
import javax.jms.QueueSession;
import javax.jms.Session;
import javax.jms.StreamMessage;
import javax.jms.TemporaryQueue;
import javax.jms.TextMessage;
import javax.jms.Topic;
import javax.jms.TopicConnection;
import javax.jms.TopicConnectionFactory;
import javax.jms.TopicPublisher;
import javax.jms.TopicSession;
import javax.management.MBeanServer;
import javax.management.ObjectName;
import javax.naming.Binding;
import javax.naming.InitialContext;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Random;

/**
 * The MDB for SendEar and PublishEar. This MDB performs various tests; which
 * test is invoked is determined by an environment setting.
 *
 * @author fkieviet
 * @version $Revision: 1.7 $
 */
public class TestMessageBean implements MessageDrivenBean, MessageListener {
    private transient MessageDrivenContext mMdc = null;
    private boolean mInited = true;
    private InitialContext mCtx;
   

    static final Logger sLog = Logger.getLogger(TestMessageBean.class);
    private static Logger sContextEnter = Logger.getLogger("com.stc.EnterContext");
    private static Logger sContextExit = Logger.getLogger("com.stc.ExitContext");

    /**
     * Default constructor. Creates a bean. Required by EJB spec.
     */
    public TestMessageBean() {
        sLog.infoNoloc("MDB constructor");
    }

    /**
     * Sets the context for the bean.
     * @param mdc the message-driven-bean context.
     */
    public void setMessageDrivenContext(MessageDrivenContext mdc) {
        sLog.infoNoloc("In SimpleMessageBean.setMessageDrivenContext()");
        this.mMdc = mdc;
    }

    /**
     * Creates a bean. Required by EJB spec.
     */
    public void ejbCreate() {
        sLog.infoNoloc("In SimpleMessageBean.ejbCreate()");
        try {
            mCtx = new InitialContext();
        } catch (NamingException ex) {
            sLog.fatalNoloc(ex.getMessage(), ex);
        }
    }

    /**
     * Removes the bean. Required by EJB spec.
     */
    public void ejbRemove() {
        sLog.infoNoloc("In SimpleMessageBean.remove()");
    }

//    private void logEnv(String path) {
//        if (sLog.isDebugEnabled()) {
//            sLog.debug("logEnv");
//        }
//
//        try {
//            InitialContext ctx = new InitialContext();
//            for (NamingEnumeration e = ctx.list(path); e.hasMore(); ) {
//                Object o = e.next();
//                if (sLog.isDebugEnabled()) {
//                    sLog.debug("path=[" + path + "] o=[" + o + "]");
//                }
//
//            }
//        } catch (NamingException ex) {
//            sLog.errorNoloc("LogEnv failure: " + ex, ex);
//        }
//    }

    private void explore(InitialContext ctx, String from,
        PrintWriter out) throws Exception {
        NamingEnumeration e = ctx.listBindings(from);
        if (e == null) {
            out.println("[" + from + "]: no binding list");
        } else {
            for (; e.hasMore(); ) {
                Binding binding = (Binding) e.next();
                String path = from.equals("") ? binding.getName()
                    : from + "/" + binding.getName();

                if (binding.getObject().toString().startsWith(
                    "com.sun.enterprise.naming.TransientContext")) {
                    out.println(path + " [+]");
                    explore(ctx, path, out);
                } else {
                    out.println(path + " (" + binding.getObject().getClass().getName()
                        + ")");
                }
            }
        }
    }

    private String jndiTree(String r) {
        StringWriter s = new StringWriter(1000);
        PrintWriter out = new PrintWriter(s);
        try {
            out.println("JNDI TREE of " + r + ":");
            explore(new InitialContext(), r, out);
        } catch (Exception ex) {
            out.println("Exception while exploring JNDI: " + ex);
            ex.printStackTrace(out);
        }
        return s.toString();
    }

    private void checkInit() {
        if (!mInited) {
            sLog.infoNoloc(jndiTree(""));
            sLog.infoNoloc(jndiTree("java:"));
            sLog.infoNoloc(jndiTree("java:comp"));
            sLog.infoNoloc(jndiTree("java:comp/env"));
            mInited = true;
        }
    }

    /**
     * Called by the RA
     *
     * @param message incoming message.
     */
    public void onMessage(javax.jms.Message message) {
        if (sLog.isDebugEnabled()) {
            sLog.debug("In onMessage()");
        }

        try {
            String fname = (String) mCtx.lookup("java:comp/env/Test");
            if (fname.equals("see message")) {
                fname = message.getStringProperty("methodname");
            }
            if (sLog.isDebugEnabled()) {
                sLog.debug("Looking up function name " + fname + " (will set context)");
            }
            try {
                sContextEnter.infoNoloc(fname);
                if (!mInited) {
                    sLog.infoNoloc("First call on method " + fname + " for this MDB");
                    checkInit();
                    mInited = true;
                }

                if ("testPerf1".equals(fname)) {
                    testPerf1(message);
                } else if ("throwExceptionBMT".equals(fname)) {
                    throwExceptionBMT(message);
                } else {
                    Method meth = this.getClass().getMethod(fname, new Class[] { javax.jms.Message.class });
                    try {
                        meth.invoke(this, new Object[] { message });
                    } catch (InvocationTargetException e1) {
                        throw e1.getTargetException();
                    }
                }
                if (sLog.isDebugEnabled()) {
                    sLog.debug("Function " + fname + " called; no exc. thrown");
                }
            } finally {
                sContextExit.infoNoloc(fname);
            }
        } catch (IntentionalException e) {
            // don't log
            throw new EJBException(
                "Intentional exception is being rethrown: " + e, e);
        } catch (Exception e) {
            sLog.errorNoloc(".onMessage() encountered an exception: " + e, e);
            throw new EJBException(
                "SimpleMessageBean.onMessage() encountered an exception: " + e, e);
        } catch (Throwable e) {
            sLog.errorNoloc(".onMessage() encountered an exception: " + e, e);
            throw new EJBException(
                "SimpleMessageBean.onMessage() encountered an unexpected throwable: " + e, new Exception(e));
        }
    }

    /**
     * Sends from Queue1 to Queue2
     *
     * @param message Message
     */
    public void testPerf1(javax.jms.Message message) {
        QueueConnection conn = null;
        try {
            QueueConnectionFactory fact = (QueueConnectionFactory) mCtx
                .lookup("java:comp/env/queuefact");
            conn = fact.createQueueConnection();
            QueueSession s = conn.createQueueSession(true,
                Session.AUTO_ACKNOWLEDGE);
            Queue dest = s.createQueue("Queue2");
            QueueSender prod = s.createSender(dest);
            prod.send(message);
        } catch (Exception e) {
            sLog.errorNoloc("testPerf1 failure: " + e, e);
            throw new EJBException("Failed: " + e, e);
        } finally {
            if (conn != null) {
                try {
                    conn.close();
                } catch (JMSException ignore) {
                }
            }
        }
    }

    /**
     * Sends from Queue1 to Queue2 with optional delay
     *
     * @param message Message
     */
    public void testQQXAXA(javax.jms.Message message) {
        try {
            int delay = 0;

            QueueConnection conn = null;
            try {
                if (message.propertyExists("delay")) {
                    delay = message.getIntProperty("delay");
                }

                QueueConnectionFactory fact = (QueueConnectionFactory) mCtx
                    .lookup("java:comp/env/queuefact");
                conn = fact.createQueueConnection();
                QueueSession s = conn.createQueueSession(true,
                    Session.AUTO_ACKNOWLEDGE);
                Queue dest = s.createQueue("Queue2");
                QueueSender prod = s.createSender(dest);
                prod.send(message);

                if (sLog.isDebugEnabled()) {
                    sLog.debug("Msg sent to " + dest.getQueueName());
                }
            } catch (Exception ex) {
                sLog.errorNoloc("XYZ failure: " + ex, ex);
            } finally {
                if (conn != null) {
                    try {
                        conn.close();
                    } catch (JMSException ignore) {
                    }
                }
            }
            if (delay != 0) {
                try {
                    Thread.sleep(delay);
                } catch (InterruptedException ex) {
                    // ignore
                }
            }
        } catch (Exception e) {
            sLog.errorNoloc("Failed: " + e, e);
            throw new EJBException("Failed: " + e, e);
        }
    }

    /**
     * Sends from Queue1 to Queue2 with optional delay
     *
     * @param message Message
     */
    public void testQQXAXALoop(javax.jms.Message message) {
        try {
            QueueConnection conn = null;
            try {
                QueueConnectionFactory fact = (QueueConnectionFactory) mCtx
                    .lookup("java:comp/env/queuefact");
                conn = fact.createQueueConnection();
                QueueSession s = conn.createQueueSession(true,
                    Session.AUTO_ACKNOWLEDGE);
                Queue dest = s.createQueue("Queue1");
                QueueSender prod = s.createSender(dest);
                prod.send(message);
            } catch (Exception ex) {
                sLog.errorNoloc("XYZ failure: " + ex, ex);
            } finally {
                if (conn != null) {
                    try {
                        conn.close();
                    } catch (JMSException ignore) {
                    }
                }
            }
        } catch (Exception e) {
            sLog.errorNoloc("Failed: " + e, e);
            throw new EJBException("Failed: " + e, e);
        }
    }

    /**
     * Sends from Queue1 to Queue2 with optional delay
     *
     * @param message Message
     */
    public void sendTo2SpecialUrl(javax.jms.Message message) {
        try {
            QueueConnection conn = null;
            try {
                QueueConnectionFactory fact = (QueueConnectionFactory) mCtx
                    .lookup("java:comp/env/queuefact");
                String specialUrl = (String) mCtx.lookup("java:comp/env/specialurl");
                conn = fact.createQueueConnection(specialUrl, null);
                QueueSession s = conn.createQueueSession(true,
                    Session.AUTO_ACKNOWLEDGE);
                Queue dest = s.createQueue("Queue2");
                QueueSender prod = s.createSender(dest);
                prod.send(message);

                if (sLog.isDebugEnabled()) {
                    sLog.debug("Msg sent to " + dest.getQueueName());
                }
            } catch (Exception ex) {
                sLog.errorNoloc("XYZ failure: " + ex, ex);
            } finally {
                safeClose(conn);
            }
        } catch (Exception e) {
            sLog.errorNoloc("Failed: " + e, e);
            throw new EJBException("Failed: " + e, e);
        }
    }

    private int mMsgCounter;

    /**
     * Sends from Queue1 to Queue2 with random special URL
     *
     * @param message Message
     */
    public void sendTo2SpecialUrlMix(javax.jms.Message message) {
        try {
            QueueConnection conn = null;
            try {
                QueueConnectionFactory fact = (QueueConnectionFactory) mCtx
                    .lookup("java:comp/env/queuefact");

                mMsgCounter++;

                String specialUrl = null;
                if (mMsgCounter % 2 == 0) {
                    specialUrl = (String) mCtx.lookup("java:comp/env/specialurl");
                }
                conn = fact.createQueueConnection(specialUrl, null);
                QueueSession s = conn.createQueueSession(true,
                    Session.AUTO_ACKNOWLEDGE);
                Queue dest = s.createQueue("Queue2");
                QueueSender prod = s.createSender(dest);
                prod.send(message);

                if (sLog.isDebugEnabled()) {
                    sLog.debug("Msg sent to " + dest.getQueueName());
                }
            } catch (Exception ex) {
                sLog.errorNoloc("XYZ failure: " + ex, ex);
            } finally {
                safeClose(conn);
            }
        } catch (Exception e) {
            sLog.errorNoloc("Failed: " + e, e);
            throw new EJBException("Failed: " + e, e);
        }
    }

    /**
     * Sends from Queue1 to Queue2 with optional delay
     *
     * @param message Message
     */
    public void testTTXAXA(javax.jms.Message message) {
        try {
            TopicConnection conn = null;
            try {
                TopicConnectionFactory fact = (TopicConnectionFactory) mCtx
                    .lookup("java:comp/env/topicfact");
                conn = fact.createTopicConnection();
                TopicSession s = conn.createTopicSession(true,
                    Session.AUTO_ACKNOWLEDGE);
                Topic dest = s.createTopic("Topic2");
                TopicPublisher prod = s.createPublisher(dest);
                prod.publish(message);

                if (sLog.isDebugEnabled()) {
                    sLog.debug("Msg sent to " + dest.getTopicName());
                }
            } catch (Exception ex) {
                sLog.errorNoloc("XYZ failure: " + ex, ex);
            } finally {
                if (conn != null) {
                    try {
                        conn.close();
                    } catch (JMSException ignore) {
                    }
                }
            }
        } catch (Exception e) {
            sLog.errorNoloc("Failed: " + e, e);
            throw new EJBException("Failed: " + e, e);
        }
    }

    /**
     * Sends from Queue1 to Queue2 with optional delay
     *
     * @param message Message
     */
    public void testTQXAXA(javax.jms.Message message) {
        try {
            QueueConnection conn = null;
            try {
                QueueConnectionFactory fact = (QueueConnectionFactory) mCtx
                .lookup("java:comp/env/queuefact");
                conn = fact.createQueueConnection();
                QueueSession s = conn.createQueueSession(true,
                Session.AUTO_ACKNOWLEDGE);
                Queue dest = s.createQueue("Queue2");
                QueueSender prod = s.createSender(dest);
                prod.send(message);
                
                if (sLog.isDebugEnabled()) {
                    sLog.debug("Msg sent to " + dest.getQueueName());
                }
            } catch (Exception ex) {
                sLog.errorNoloc("XYZ failure: " + ex, ex);
            } finally {
                if (conn != null) {
                    try {
                        conn.close();
                    } catch (JMSException ignore) {
                    }
                }
            }
        } catch (Exception e) {
            sLog.errorNoloc("Failed: " + e, e);
            throw new EJBException("Failed: " + e, e);
        }
    }

    /**
     * Uses a UT to send to Queue2; closes within UT
     *
     * @param message Message
     * @throws Exception
     */
    public void testQQBM1(javax.jms.Message message) throws Exception {
        QueueConnection conn = null;
        try {
            QueueConnectionFactory fact = (QueueConnectionFactory) mCtx
                .lookup("java:comp/env/queuefact");

            sLog.debug("Starting UT");

            // UT
            mMdc.getUserTransaction().begin();
            conn = fact.createQueueConnection();
            QueueSession s = conn.createQueueSession(true,
                Session.AUTO_ACKNOWLEDGE);
            Queue dest = s.createQueue("Queue2");
            QueueSender prod = s.createSender(dest);
            prod.send(message);
            safeClose(conn);

            sLog.debug("Commit UT");
            mMdc.getUserTransaction().commit();
        } finally {
            safeClose(conn);
        }
    }

    /**
     * Uses a UT to send to Queue2; closes outside of UT
     *
     * @param message Message
     * @throws Exception
     */
    public void testQQBM2(javax.jms.Message message) throws Exception {
        QueueConnection conn = null;
        try {
            QueueConnectionFactory fact = (QueueConnectionFactory) mCtx
                .lookup("java:comp/env/queuefact");

            sLog.debug("Starting UT");

            // UT
            mMdc.getUserTransaction().begin();
            conn = fact.createQueueConnection();
            QueueSession s = conn.createQueueSession(true,
                Session.AUTO_ACKNOWLEDGE);
            Queue dest = s.createQueue("Queue2");
            QueueSender prod = s.createSender(dest);
            prod.send(message);
            sLog.debug("Commit UT");
            mMdc.getUserTransaction().commit();
        } finally {
            safeClose(conn);
        }
    }

    /**
     * Test rollback with connection allocated inside of TX
     *
     * @param message Message
     * @throws Exception
     */
    public void testBeanManagedRBAllocateOutsideOfTx(javax.jms.Message message) throws Exception {
        QueueConnection conn = null;
        try {

            QueueConnectionFactory fact = (QueueConnectionFactory) mCtx
            .lookup("java:comp/env/queuefact");

            conn = fact.createQueueConnection();
            QueueSession s = conn.createQueueSession(false, Session.AUTO_ACKNOWLEDGE);            

            // Do rollback
            {
                mMdc.getUserTransaction().begin();
                Queue dest = s.createQueue("Queue2");
                QueueSender prod = s.createSender(dest);
                prod.send(s.createTextMessage("Should not have been sent"));
                mMdc.getUserTransaction().rollback();
            }            

            // Do commit
            {
                mMdc.getUserTransaction().begin();
                Queue dest = s.createQueue("Queue2");
                QueueSender prod = s.createSender(dest);
                prod.send(message);
                mMdc.getUserTransaction().commit();
                s.close();
            }
        } finally {
            safeClose(conn);
        }
    }

    /**
     * Test XAsession rollback with connection allocated outside of TX
     *
     * @param message Message
     * @throws Exception
     */    
    public void testXASessionRBAllocateOutsideOfTx(javax.jms.Message message) throws Exception {
        QueueConnection conn = null;
        try {

            QueueConnectionFactory fact = (QueueConnectionFactory) mCtx
            .lookup("java:comp/env/queuefact");

            conn = fact.createQueueConnection();
            QueueSession ss = conn.createQueueSession(false, Session.AUTO_ACKNOWLEDGE);                    
            mMdc.getUserTransaction().begin();
            Queue dest = ss.createQueue("Queue2");
            QueueSender prod = ss.createSender(dest);
            prod.send(message);
            mMdc.getUserTransaction().commit();
            ss.close();                
            
        } finally {
            safeClose(conn);
        }
    }

    /**
     * Test XAsession commit with connection allocated outside of TX
     *
     * @param message Message
     * @throws Exception
     */    
    public void testXASessionCommitAllocateOutsideOfTx(javax.jms.Message message) throws Exception {
        QueueConnection conn = null;
        try {

            QueueConnectionFactory fact = (QueueConnectionFactory) mCtx
            .lookup("java:comp/env/queuefact");
            conn = fact.createQueueConnection();
            
            // auto commit
            QueueSession ss = conn.createQueueSession(false, Session.AUTO_ACKNOWLEDGE);                    
            Queue dest = ss.createQueue("Queue2");
            QueueSender prod = ss.createSender(dest);
            prod.send(message);
            ss.close();                
        } finally {
            safeClose(conn);
        }
    }
    
    /**
     * Test autocommit with connection allocated outside of TX
     *
     * @param message Message
     * @throws Exception
     */
    public void testBeanManagedAutoCommitAllocateOutsideOfTx(javax.jms.Message message) throws Exception {
        QueueConnection conn = null;
        try {

            QueueConnectionFactory fact = (QueueConnectionFactory) mCtx
            .lookup("java:comp/env/queuefact");

            conn = fact.createQueueConnection();
            QueueSession s = conn.createQueueSession(false, Session.AUTO_ACKNOWLEDGE);
            // Do commit
            {
                mMdc.getUserTransaction().begin();
                Queue dest = s.createQueue("Queue2");
                QueueSender prod = s.createSender(dest);
                prod.send(message);
                s.close();
            }
        } finally {
            safeClose(conn);
        }
    }
    
    /**
     * Test rollback with connection allocated inside of TX
     *
     * @param message Message
     * @throws Exception
     */
    public void testBeanManagedRBAllocateInTx(javax.jms.Message message) throws Exception {
        QueueConnection conn = null;
        try {

            QueueConnectionFactory fact = (QueueConnectionFactory) mCtx
            .lookup("java:comp/env/queuefact");

            conn = fact.createQueueConnection();
            QueueSession s;
            
            // Do commit
            {
                mMdc.getUserTransaction().begin();
                s = conn.createQueueSession(false, Session.AUTO_ACKNOWLEDGE);
                Queue dest = s.createQueue("Queue2");
                QueueSender prod = s.createSender(dest);
                prod.send(message);
                mMdc.getUserTransaction().commit();
                s.close();
            }

            // Do rollback
            {
                mMdc.getUserTransaction().begin();
                s = conn.createQueueSession(false, Session.AUTO_ACKNOWLEDGE);
                Queue dest = s.createQueue("Queue2");
                QueueSender prod = s.createSender(dest);
                prod.send(s.createTextMessage("Should not have been sent"));
                mMdc.getUserTransaction().rollback();
                s.close();
            }
            
        } finally {
            safeClose(conn);
        }
    }

    /**
     * Uses a UT to send to Queue2 AND Queue3; closes outside of UT
     *
     * @param message Message
     * @throws Exception
     */
    public void sendTo2And3CloseAtEnd(javax.jms.Message message) throws Exception {
        QueueConnection conn1 = null;
        QueueConnection conn2 = null;
        try {
            QueueConnectionFactory fact = (QueueConnectionFactory) mCtx
                .lookup("java:comp/env/queuefact");

            mMdc.getUserTransaction().begin();
            {
                conn1 = fact.createQueueConnection();
                QueueSession s = conn1.createQueueSession(true,
                    Session.AUTO_ACKNOWLEDGE);
                Queue dest = s.createQueue("Queue2");
                QueueSender prod = s.createSender(dest);
                prod.send(message);
            }
            {
                conn2 = fact.createQueueConnection();
                QueueSession s = conn2.createQueueSession(true,
                    Session.AUTO_ACKNOWLEDGE);
                Queue dest = s.createQueue("Queue3");
                QueueSender prod = s.createSender(dest);
                prod.send(message);
            }
            mMdc.getUserTransaction().commit();
        } finally {
            safeClose(conn1);
            safeClose(conn2);
        }
    }

    /**
     * Uses a UT to send to Queue2 AND Queue3; closes inside of UT
     *
     * @param message Message
     * @throws Exception
     */
    public void sendTo2And3CloseImmediately(javax.jms.Message message) throws Exception {
        QueueConnection conn1 = null;
        QueueConnection conn2 = null;
        try {
            QueueConnectionFactory fact = (QueueConnectionFactory) mCtx
                .lookup("java:comp/env/queuefact");

            mMdc.getUserTransaction().begin();
            {
                conn1 = fact.createQueueConnection();
                QueueSession s = conn1.createQueueSession(true,
                    Session.AUTO_ACKNOWLEDGE);
                Queue dest = s.createQueue("Queue2");
                QueueSender prod = s.createSender(dest);
                prod.send(message);
                safeClose(conn1);
            }
            {
                conn2 = fact.createQueueConnection();
                QueueSession s = conn2.createQueueSession(true,
                    Session.AUTO_ACKNOWLEDGE);
                Queue dest = s.createQueue("Queue3");
                QueueSender prod = s.createSender(dest);
                prod.send(message);
                safeClose(conn2);
            }
            mMdc.getUserTransaction().commit();
        } finally {
            safeClose(conn1);
            safeClose(conn2);
        }
    }

    private void safeClose(Connection c) {
        if (c != null) {
            try {
                c.close();
            } catch (Exception e) {
                // ignore
            }
        }
    }

    /**
     * Uses a UT to send to Queue2 AND Queue3; closes inside of UT
     *
     * @param message Message
     * @throws Exception
     */
    public void sendTo2And3PreventSharing(javax.jms.Message message) throws Exception {
        QueueConnection conn1 = null;
        Connection conn2 = null;
        try {
            QueueConnectionFactory qfact = (QueueConnectionFactory) mCtx
                .lookup("java:comp/env/queuefact");
            ConnectionFactory ufact = (ConnectionFactory) mCtx
                .lookup("java:comp/env/unifiedfact");

            mMdc.getUserTransaction().begin();
            {
                conn1 = qfact.createQueueConnection();
                QueueSession s = conn1.createQueueSession(true,
                    Session.AUTO_ACKNOWLEDGE);
                Queue dest = s.createQueue("Queue2");
                QueueSender prod = s.createSender(dest);
                prod.send(message);
                safeClose(conn1);
            }
            {
                conn2 = ufact.createConnection();
                Session s = conn2.createSession(true,
                    Session.AUTO_ACKNOWLEDGE);
                Queue dest = s.createQueue("Queue3");
                MessageProducer prod = s.createProducer(dest);
                prod.send(message);
                safeClose(conn2);
            }
            mMdc.getUserTransaction().commit();
        } finally {
            safeClose(conn1);
            safeClose(conn2);
        }
    }

    /**
     * Uses a UT to send to Queue2 AND Queue3; closes inside of UT; use a mix of
     * session types to prevent sharing
     *
     * @param message Message
     * @throws Exception
     */
    public void sendTo2And3Mix(javax.jms.Message message) throws Exception {
        QueueConnection conn1 = null;
        Connection conn2 = null;
        try {
            QueueConnectionFactory qfact = (QueueConnectionFactory) mCtx
                .lookup("java:comp/env/queuefact");
            ConnectionFactory ufact = (ConnectionFactory) mCtx
                .lookup("java:comp/env/unifiedfact");

            mMdc.getUserTransaction().begin();
            {
                conn1 = qfact.createQueueConnection();
                QueueSession s = conn1.createQueueSession(true,
                    Session.AUTO_ACKNOWLEDGE);
                Queue dest = s.createQueue("Queue2");
                QueueSender prod = s.createSender(dest);
                prod.send(message);
                safeClose(conn1);
            }
            {
                conn2 = ufact.createConnection();
                Session s = conn2.createSession(true,
                    Session.AUTO_ACKNOWLEDGE);
                Queue dest = s.createQueue("Queue3");
                MessageProducer prod = s.createProducer(dest);
                prod.send(message);
                safeClose(conn2);
            }
            mMdc.getUserTransaction().commit();
        } finally {
            safeClose(conn1);
            safeClose(conn2);
        }
    }

    /**
     * Sends to Queue2 using a unified CF
     *
     * @param message Message
     * @throws Exception
     */
    public void sendTo2UsingUnified(javax.jms.Message message) throws Exception {
        QueueConnection conn1 = null;
        Connection conn2 = null;
        try {
            ConnectionFactory ufact = (ConnectionFactory) mCtx
                .lookup("java:comp/env/unifiedfact");

            conn2 = ufact.createConnection();
            Session s = conn2.createSession(true, Session.AUTO_ACKNOWLEDGE);
            Queue dest = s.createQueue("Queue2");
            MessageProducer prod = s.createProducer(dest);
            prod.send(message);
        } finally {
            safeClose(conn1);
            safeClose(conn2);
        }
    }

    /**
     * Sleeps x ms for each message (used to test concurrency)
     *
     * @param message Message
     * @throws Exception
     */
    public void sleepABit(javax.jms.Message message) throws Exception {
        QueueConnection conn1 = null;
        try {
            QueueConnectionFactory qfact = (QueueConnectionFactory) mCtx
                .lookup("java:comp/env/queuefact");

            conn1 = qfact.createQueueConnection();
            QueueSession s = conn1.createQueueSession(true,
                Session.AUTO_ACKNOWLEDGE);
            Queue dest = s.createQueue("Queue2");
            QueueSender prod = s.createSender(dest);
            
            sLog.infoNoloc("Processing msg [" + ((TextMessage) message).getText() + "]");

            Thread.sleep(100);

            prod.send(message);
            safeClose(conn1);
        } finally {
            safeClose(conn1);
        }
    }

    /**
     * Sleeps x ms for each message (used to test concurrency)
     *
     * @param message Message
     * @throws Exception
     */
    public void sleepSometimes(javax.jms.Message message) throws Exception {
        QueueConnection conn1 = null;
        try {
            QueueConnectionFactory qfact = (QueueConnectionFactory) mCtx
                .lookup("java:comp/env/queuefact");

            conn1 = qfact.createQueueConnection();
            QueueSession s = conn1.createQueueSession(true,
                Session.AUTO_ACKNOWLEDGE);
            Queue dest = s.createQueue("Queue2");
            QueueSender prod = s.createSender(dest);
            
            sLog.infoNoloc("Processing msg [" + ((TextMessage) message).getText() + "]");

            if (shouldThrow()) {
                Thread.sleep(100);
            }

            prod.send(message);
            safeClose(conn1);
        } finally {
            safeClose(conn1);
        }
    }

    private static Random sRandom = new Random();

    /**
     * Returns true in about 20% of the time
     *
     * @return boolean
     */
    private static synchronized boolean shouldThrow() {
        return sRandom.nextInt(10) > 8;
    }
    
    private class IntentionalException extends Exception {
        public IntentionalException(String msg) {
            super(msg);
        }
    }

    /**
     * Random exception
     *
     * @param message Message
     * @throws Exception
     */
    public void throwExceptionCMT(javax.jms.Message message) throws Exception {
        QueueConnection conn1 = null;
        try {
            QueueConnectionFactory qfact = (QueueConnectionFactory) mCtx
                .lookup("java:comp/env/queuefact");

            conn1 = qfact.createQueueConnection();
            QueueSession s = conn1.createQueueSession(true,
                Session.AUTO_ACKNOWLEDGE);
            Queue dest = s.createQueue("Queue2");
            QueueSender prod = s.createSender(dest);
            prod.send(message);

            if (shouldThrow()) {
                throw new IntentionalException("Random exception to force rollback");
            }
        } finally {
            safeClose(conn1);
        }
    }

    /**
     * Random rollback
     *
     * @param message Message
     * @throws Exception
     */
    public void rollbackCMT(javax.jms.Message message) throws Exception {
        QueueConnection conn1 = null;
        try {
            QueueConnectionFactory qfact = (QueueConnectionFactory) mCtx
                .lookup("java:comp/env/queuefact");

            conn1 = qfact.createQueueConnection();
            QueueSession s = conn1.createQueueSession(true,
                Session.AUTO_ACKNOWLEDGE);
            Queue dest = s.createQueue("Queue2");
            QueueSender prod = s.createSender(dest);
            prod.send(message);

            if (shouldThrow()) {
                mMdc.setRollbackOnly();

                dest = s.createQueue("Queue3");
                prod = s.createSender(dest);
                prod.send(s.createTextMessage("Should have been rolled back"));
            }
        } finally {
            safeClose(conn1);
        }
    }

    /**
     * Random rollback
     *
     * @param message Message
     * @throws Exception
     */
    public void testReplyToIsNotWrapped(javax.jms.Message message) throws Exception {
        QueueConnection conn1 = null;
        try {
            QueueConnectionFactory qfact = (QueueConnectionFactory) mCtx
                .lookup("java:comp/env/queuefact");

            conn1 = qfact.createQueueConnection();
            QueueSession s = conn1.createQueueSession(true,
                Session.AUTO_ACKNOWLEDGE);
            Queue dest = s.createQueue("Queue2");
            QueueSender prod = s.createSender(dest);
            
            Message m = s.createTextMessage();
            TemporaryQueue tempq = s.createTemporaryQueue();
            m.setJMSReplyTo(tempq);
            
            Destination destback = m.getJMSReplyTo();
            
            if (destback instanceof Unwrappable) {
                // ERROR!
                s.createSender(s.createQueue("Queue3")).send(message);
            }
            
            prod.send(message);
        } finally {
            safeClose(conn1);
        }
    }

    public void throwExceptionBMT(javax.jms.Message message) throws Exception {
        QueueConnection conn = null;
        try {
            QueueConnectionFactory fact = (QueueConnectionFactory) mCtx
                .lookup("java:comp/env/queuefact");

            if (shouldThrow()) {
                sLog.infoNoloc("WILL NOW THROW INTENTIONAL EXCEPTION");
                throw new IntentionalException("Random exception to force rollback.");
            }
            mMdc.getUserTransaction().begin();
            conn = fact.createQueueConnection();
            QueueSession s = conn.createQueueSession(true,
                Session.AUTO_ACKNOWLEDGE);
            Queue dest = s.createQueue("Queue2");
            QueueSender prod = s.createSender(dest);
            prod.send(message);
            mMdc.getUserTransaction().commit();
        } finally {
            safeClose(conn);
        }
    }
  
    public void throwExceptionInBMT(javax.jms.Message message) throws Exception {
        QueueConnection conn = null;
        try {
            QueueConnectionFactory fact = (QueueConnectionFactory) mCtx
                .lookup("java:comp/env/queuefact");

            mMdc.getUserTransaction().begin();
            conn = fact.createQueueConnection();
            QueueSession s = conn.createQueueSession(true,
                Session.AUTO_ACKNOWLEDGE);
            Queue dest = s.createQueue("Queue2");
            QueueSender prod = s.createSender(dest);
            prod.send(message);
            if (shouldThrow()) {
                mMdc.getUserTransaction().rollback();
                sLog.infoNoloc("WILL NOW THROW INTENTIONAL EXCEPTION");
                throw new IntentionalException("Random exception to force rollback.");
            }            
            mMdc.getUserTransaction().commit();
        } finally {
            safeClose(conn);
        }
    }
    /**
     * Passes from Q1 to Q2 assuming XA; the connection failure will apear during
     * enlisting the XAResource
     */
    public void reconnectOutXA(javax.jms.Message message) throws Exception {
        QueueConnection conn = null;
        try {
            sLog.infoNoloc("Message received " + ((TextMessage) message).getText());

            QueueConnectionFactory fact = (QueueConnectionFactory) mCtx
                .lookup("java:comp/env/queuefact");

            conn = fact.createQueueConnection();
            QueueSession s = conn.createQueueSession(true,
                Session.AUTO_ACKNOWLEDGE);
            Queue dest = s.createQueue("Queue2");
            QueueSender prod = s.createSender(dest);
            
            prod.send(message);
            
            sLog.infoNoloc("Message [" + ((TextMessage) message).getText() + "] sent (but not committed)");
        } catch (JMSException e) {
            // Expected
            throw e;
        } finally {
            safeClose(conn);
        }
    }

    /**
     * Passes from Q1 to Q2 assuming XA and BMT; the connection failure will apear during
     * enlisting the XAResource *after* creating the connection
     */
    public void reconnectOutBMTXA(javax.jms.Message message) throws Exception {
        QueueConnection conn = null;
        try {
            QueueConnectionFactory fact = (QueueConnectionFactory) mCtx
                .lookup("java:comp/env/queuefact");

            conn = fact.createQueueConnection();
            QueueSession s = conn.createQueueSession(true,
                Session.AUTO_ACKNOWLEDGE);
            Queue dest = s.createQueue("Queue2");

            // Enlist; this will likely cause an exception on reused connections
            mMdc.getUserTransaction().begin();
            
            QueueSender prod = s.createSender(dest);
            prod.send(message);

            mMdc.getUserTransaction().commit();
        } catch (JMSException e) {
            // Expected
            throw e;
        } finally {
            safeClose(conn);
        }
    }

    /**
     * Simply passes from Q1 to Q2 assuming a NoTransaction connection
     */
    public void reconnectOutNoTx(javax.jms.Message message) throws Exception {
        QueueConnection conn = null;
        try {
//            sLog.infoNoloc("Message received " + ((TextMessage) message).getText());

            // Sleep to avoid spinning too fast when the connection is failing and 
            // is waiting for the 5 sec timeout check
            Thread.sleep(50);

            QueueConnectionFactory fact = (QueueConnectionFactory) mCtx
                .lookup("java:comp/env/queuefact");
            conn = fact.createQueueConnection();
            QueueSession s = conn.createQueueSession(false,
                Session.AUTO_ACKNOWLEDGE);
            Queue dest = s.createQueue("Queue2");
            QueueSender prod = s.createSender(dest);
            
            prod.send(message);

//            sLog.infoNoloc("Message [" + ((TextMessage) message).getText() + "] sent (and committed)");
        } catch (JMSException e) {
            // Expected
            throw e;
        } finally {
            safeClose(conn);
        }
    }
    
    /**
     * Simply passes from Q1 to Q2 assuming a LocalTransaction connection; 
     * must be BMT to avoid exception due to non-xa resource in transaction
     */
    public void reconnectOutLocalTxBMTLateEnlistment(javax.jms.Message message) throws Exception {
        QueueConnection conn = null;
        try {
//            sLog.infoNoloc("Message received " + ((TextMessage) message).getText());

            QueueConnectionFactory fact = (QueueConnectionFactory) mCtx
                .lookup("java:comp/env/queuefact");
            conn = fact.createQueueConnection();
            QueueSession s = conn.createQueueSession(false,
                Session.AUTO_ACKNOWLEDGE);
            Queue dest = s.createQueue("Queue2");
            
            // Enlist; this will likely cause an exception on reused connections
            mMdc.getUserTransaction().begin();
            
            QueueSender prod = s.createSender(dest);
            prod.send(message);

            mMdc.getUserTransaction().commit();

//            sLog.infoNoloc("Message [" + ((TextMessage) message).getText() + "] sent (and committed)");
        } catch (JMSException e) {
            // Expected
            throw e;
        } finally {
            safeClose(conn);
        }
    }
    
    /**
     * Simply passes from Q1 to Q2 assuming a LocalTransaction connection; 
     * must be BMT to avoid exception due to non-xa resource in transaction
     */
    public void reconnectOutLocalTxBMTEarlyEnlistment(javax.jms.Message message) throws Exception {
        QueueConnection conn = null;
        try {
            sLog.infoNoloc("Message received " + ((TextMessage) message).getText());

            // Enlist; this will likely cause an exception on reused connections
            mMdc.getUserTransaction().begin();
            
            QueueConnectionFactory fact = (QueueConnectionFactory) mCtx
                .lookup("java:comp/env/queuefact");
            conn = fact.createQueueConnection();
            QueueSession s = conn.createQueueSession(false,
                Session.AUTO_ACKNOWLEDGE);
            Queue dest = s.createQueue("Queue2");
            
            QueueSender prod = s.createSender(dest);
            prod.send(message);

            mMdc.getUserTransaction().commit();

            sLog.infoNoloc("Message [" + ((TextMessage) message).getText() + "] sent (and committed)");
        } catch (JMSException e) {
            // Expected
            throw e;
        } finally {
            safeClose(conn);
        }
    }
    
//    private TransactionManager getTxMgr() {
//        try {
//            Class c1 = Class.forName("com.sun.enterprise.Switch");
//            Method m1 = c1.getMethod("getSwitch", new Class[0]);
//            Object theswitch = m1.invoke(null, new Object[0]);
//            Method m2 = c1.getMethod("getTransactionManager", new Class[0]);
//            Object ret = m2.invoke(theswitch, new Object[0]);
//            return (TransactionManager) ret;
//        } catch (Exception e) {
//            sLog.fatalNoloc("txmgr not found: " + e, e);
//            return null;
//        }
//    }
    
    private void assertTrue(boolean b) throws Exception {
        if (!b) {
            throw new Exception("Assertion failed");
        }
    }
    
    private boolean isTempDestValid(TemporaryQueue tempdest) throws Exception {
        boolean ok = true;
        try {
            // Should throw
            tempdest.delete();
        } catch (JMSException expected) {
            ok = false;
        }
        return ok;
    }        
            
    public void testTempDestClosed(javax.jms.Message message) throws Exception {
        QueueConnection conn = null;
        try {

            QueueConnectionFactory fact = (QueueConnectionFactory) mCtx
            .lookup("java:comp/env/queuefact");

            // COMMIT
            // Temp dest should be invalid after close
            {
                mMdc.getUserTransaction().begin();
                conn = fact.createQueueConnection();
                QueueSession s = conn.createQueueSession(false, Session.AUTO_ACKNOWLEDGE);
                TemporaryQueue dest = s.createTemporaryQueue();
                QueueSender prod = s.createSender(dest);
                prod.send(message);
                conn.close();
                mMdc.getUserTransaction().commit();
                
                assertTrue(!isTempDestValid(dest));
            }
            
            // Temp dest should be valid before close after commit
            {
                mMdc.getUserTransaction().begin();
                conn = fact.createQueueConnection();
                QueueSession s = conn.createQueueSession(false, Session.AUTO_ACKNOWLEDGE);
                TemporaryQueue dest = s.createTemporaryQueue();
                QueueSender prod = s.createSender(dest);
                prod.send(message);
                mMdc.getUserTransaction().commit();
                assertTrue(isTempDestValid(dest));
                conn.close();
            }
            
            // Temp dest should be invalid after close outside of commit
            {
                mMdc.getUserTransaction().begin();
                conn = fact.createQueueConnection();
                QueueSession s = conn.createQueueSession(false, Session.AUTO_ACKNOWLEDGE);
                TemporaryQueue dest = s.createTemporaryQueue();
                QueueSender prod = s.createSender(dest);
                prod.send(message);
                mMdc.getUserTransaction().commit();
                conn.close();
                assertTrue(!isTempDestValid(dest));
            }

            // ROLLBACK
            // Temp dest should be invalid after close
            {
                mMdc.getUserTransaction().begin();
                conn = fact.createQueueConnection();
                QueueSession s = conn.createQueueSession(false, Session.AUTO_ACKNOWLEDGE);
                TemporaryQueue dest = s.createTemporaryQueue();
                QueueSender prod = s.createSender(dest);
                prod.send(message);
                conn.close();
                mMdc.getUserTransaction().rollback();
                
                assertTrue(!isTempDestValid(dest));
            }
            
            // Temp dest should be valid before close after commit
            {
                mMdc.getUserTransaction().begin();
                conn = fact.createQueueConnection();
                QueueSession s = conn.createQueueSession(false, Session.AUTO_ACKNOWLEDGE);
                TemporaryQueue dest = s.createTemporaryQueue();
                QueueSender prod = s.createSender(dest);
                prod.send(message);
                mMdc.getUserTransaction().rollback();
                assertTrue(isTempDestValid(dest));
                conn.close();
            }

            // Temp dest should be invalid after close outside of commit
            {
                mMdc.getUserTransaction().begin();
                conn = fact.createQueueConnection();
                QueueSession s = conn.createQueueSession(false, Session.AUTO_ACKNOWLEDGE);
                TemporaryQueue dest = s.createTemporaryQueue();
                QueueSender prod = s.createSender(dest);
                prod.send(message);
                mMdc.getUserTransaction().rollback();
                conn.close();
                assertTrue(!isTempDestValid(dest));
            }

            // All tests apparently succeeded
            mMdc.getUserTransaction().begin();
            conn = fact.createQueueConnection();
            QueueSession s = conn.createQueueSession(false, Session.AUTO_ACKNOWLEDGE);
            Queue dest = s.createQueue("Queue2");
            QueueSender prod = s.createSender(dest);
            prod.send(message);
            mMdc.getUserTransaction().commit();
        } catch (Exception e) {
            sLog.debug("Test error: " + e, e);
        } finally {
            safeClose(conn);
        }
    }
    
    private Message copy(Message message, Session s) throws JMSException {
        Message m1;
        if (message instanceof TextMessage) {
            m1 = s.createTextMessage(((TextMessage) message).getText() + " forward to QueueReplier ");
        } else if (message instanceof BytesMessage) {
            m1 = s.createBytesMessage();
        } else if (message instanceof ObjectMessage) {
            m1 = s.createObjectMessage();
        } else if (message instanceof MapMessage) {
            m1 = s.createMapMessage();
        } else if (message instanceof StreamMessage) {
            m1 = s.createStreamMessage();
        } else {
            m1 = s.createMessage();
        }

        for (Enumeration iter = message.getPropertyNames(); iter.hasMoreElements();) {
            String name = (String) iter.nextElement();
            Object o = message.getObjectProperty(name);
            if (o instanceof Integer) {
                m1.setIntProperty(name, ((Integer) o).intValue());
            } else if (o instanceof Long) {
                m1.setLongProperty(name, ((Long) o).longValue());
            } else if (o instanceof String) {
                m1.setStringProperty(name, ((String) o).toString());
            }
        }
        
        return m1;
    }
    
    /**
     * Temp dest tests: normal request/reply sequence; closes the consumer/producer only,
     * uses shared session
     * 
     * @param message
     */
    public void requestReply0(Message message) {
        QueueConnection conn = null;
        try {

            QueueConnectionFactory fact = (QueueConnectionFactory) mCtx
            .lookup("java:comp/env/queuefact");

            conn = fact.createQueueConnection();
            QueueSession s;
            TemporaryQueue tempdest;
            
            // Send request
            {
                mMdc.getUserTransaction().begin();
                s = conn.createQueueSession(false, Session.AUTO_ACKNOWLEDGE);                
                tempdest = s.createTemporaryQueue();
                Queue dest = s.createQueue("QueueReplier");
                QueueSender prod = s.createSender(dest);
                message = copy(message, s);
                message.setJMSReplyTo(tempdest);
                prod.send(message);
                
                // For 453: creation of the receiver will actually create the temp dest
                s.createReceiver(tempdest);
                
                mMdc.getUserTransaction().commit();
                prod.close();
            }
            
            // Get reply
            {
                conn.start();
                mMdc.getUserTransaction().begin();
                QueueReceiver cons = s.createReceiver(tempdest);
                message = cons.receive(10000);
                mMdc.getUserTransaction().commit();
                cons.close();
            }
            
            // Forward to Queue2
            if (message != null ){
                mMdc.getUserTransaction().begin();
                Queue dest = s.createQueue("Queue2");
                QueueSender prod = s.createSender(dest);
                prod.send(message);
                mMdc.getUserTransaction().commit();
                prod.close();
            }
        } catch (Exception e) {
            sLog.errorNoloc("Test error: " + e, e);
        } finally {
            safeClose(conn);
        }
    }

    /**
     * Temp dest tests: normal request/reply sequence; closes the consumer/producer only,
     * uses shared session
     * 
     * @param message
     */
    public void requestReply1(Message message) {
        QueueConnection conn = null;
        try {

            QueueConnectionFactory fact = (QueueConnectionFactory) mCtx
            .lookup("java:comp/env/queuefact");

            conn = fact.createQueueConnection();
            QueueSession s;
            TemporaryQueue tempdest;

            // Consumer should be alive while request is sent and processed
            QueueReceiver cons;            
            
            // Send request
            {
                mMdc.getUserTransaction().begin();
                s = conn.createQueueSession(false, Session.AUTO_ACKNOWLEDGE);
                tempdest = s.createTemporaryQueue();
                Queue dest = s.createQueue("QueueReplier");
                QueueSender prod = s.createSender(dest);
                message = copy(message, s);
                message.setJMSReplyTo(tempdest);
                prod.send(message);

                // For 453: creation of the receiver will actually create the temp dest
                cons = s.createReceiver(tempdest);

                mMdc.getUserTransaction().commit();
                prod.close();
            }
            
            // Get reply
            {
                conn.start();
                mMdc.getUserTransaction().begin();
                message = cons.receive(10000);
                mMdc.getUserTransaction().commit();
                cons.close();
            }
            
            // TEMP DEST SHOULD STILL BE GOOD
            try {
                tempdest.delete();
            } catch (JMSException unexpected) {
                sLog.errorNoloc("Unexpected: " + unexpected, unexpected);
                message = null;
            }
            
            // Forward to Queue2
            if (message != null ){
                mMdc.getUserTransaction().begin();
                Queue dest = s.createQueue("Queue2");
                QueueSender prod = s.createSender(dest);
                prod.send(message);
                mMdc.getUserTransaction().commit();
                prod.close();
            }
        } catch (Exception e) {
            sLog.errorNoloc("Test error: " + e, e);
        } finally {
            safeClose(conn);
        }
    }

    /**
     * Temp dest tests: normal request/reply sequence; closes the session
     * 
     * @param message
     */
    public void requestReply2(Message message) {
        QueueConnection conn = null;
        try {

            QueueConnectionFactory fact = (QueueConnectionFactory) mCtx
            .lookup("java:comp/env/queuefact");

            conn = fact.createQueueConnection();
            TemporaryQueue tempdest;
            
            // Consumer should be alive while request is sent and processed
            QueueReceiver cons;            
            
            // Send request
            {
                mMdc.getUserTransaction().begin();
                QueueSession s = conn.createQueueSession(false, Session.AUTO_ACKNOWLEDGE);
                tempdest = s.createTemporaryQueue();
                cons = s.createReceiver(tempdest);
                Queue dest = s.createQueue("QueueReplier");
                QueueSender prod = s.createSender(dest);
                message = copy(message, s);
                message.setJMSReplyTo(tempdest);
                prod.send(message);
                mMdc.getUserTransaction().commit();
            
                // Get reply
                conn.start();
                mMdc.getUserTransaction().begin();
                message = cons.receive(10000);
                mMdc.getUserTransaction().commit();
                s.close();
            }
            
            // TEMP DEST SHOULD STILL BE GOOD
            try {
                tempdest.delete();
            } catch (JMSException unexpected) {
                sLog.errorNoloc("Unexpected: " + unexpected, unexpected);
                message = null;
            }
            
            // Forward to Queue2
            if (message != null ){
                QueueSession s = conn.createQueueSession(false, Session.AUTO_ACKNOWLEDGE);
                mMdc.getUserTransaction().begin();
                Queue dest = s.createQueue("Queue2");
                QueueSender prod = s.createSender(dest);
                prod.send(message);
                mMdc.getUserTransaction().commit();
                s.close();
            }
        } catch (Exception e) {
            sLog.errorNoloc("Test error: " + e, e);
        } finally {
            safeClose(conn);
        }
    }

    /**
     * Temp dest tests:
     * s.close() does not invalidate tempdest (only conn.close should do that) 
     * 
     * @param message
     */
    public void requestReplyN1(Message message) {
        QueueConnection conn = null;
        try {

            QueueConnectionFactory fact = (QueueConnectionFactory) mCtx
            .lookup("java:comp/env/queuefact");

            conn = fact.createQueueConnection();
            TemporaryQueue tempdest;

            // Send request
            {
                mMdc.getUserTransaction().begin();
                QueueSession s = conn.createQueueSession(false, Session.AUTO_ACKNOWLEDGE);
                tempdest = s.createTemporaryQueue();
                QueueReceiver cons = s.createReceiver(tempdest);
                Queue dest = s.createQueue("QueueReplier");
                QueueSender prod = s.createSender(dest);
                message = copy(message, s);
                message.setJMSReplyTo(tempdest);
                prod.send(message);
                mMdc.getUserTransaction().commit();

                // Get reply
                mMdc.getUserTransaction().begin();
                conn.start();
                message = cons.receive(10000);
                mMdc.getUserTransaction().commit();
                
                // Close session (should not invalidate tempdest)
                s.close();
            }
            
            // Delete after session is closed
            {
                // * DELETE TEMP DEST *
                tempdest.delete();
            }
            
            // Forward to Queue2
            if (message != null ){
                QueueSession s = conn.createQueueSession(false, Session.AUTO_ACKNOWLEDGE);
                mMdc.getUserTransaction().begin();
                Queue dest = s.createQueue("Queue2");
                QueueSender prod = s.createSender(dest);
                prod.send(message);
                mMdc.getUserTransaction().commit();
                s.close();
            }
        } catch (Exception e) {
            sLog.errorNoloc("Test error: " + e, e);
        } finally {
            safeClose(conn);
        }
    }

    /**
     * Temp dest tests:
     * Conn.close() should invalidate tempdest
     *  
     * ut.begin
     * create temp
     * conn.close
     * <temp is invalid>
     * ut.commit
     * 
     * @param message
     */
    public void requestReplyN2(Message message) {
        QueueConnection conn = null;
        try {

            QueueConnectionFactory fact = (QueueConnectionFactory) mCtx
            .lookup("java:comp/env/queuefact");

            conn = fact.createQueueConnection();
            TemporaryQueue tempdest;
            QueueReceiver cons;
            
            // Send request
            {
                mMdc.getUserTransaction().begin();
                QueueSession s = conn.createQueueSession(false, Session.AUTO_ACKNOWLEDGE);
                tempdest = s.createTemporaryQueue();
                cons = s.createReceiver(tempdest);
                Queue dest = s.createQueue("QueueReplier");
                QueueSender prod = s.createSender(dest);
                message = copy(message, s);
                message.setJMSReplyTo(tempdest);
                prod.send(message);
                mMdc.getUserTransaction().commit();
            
                // Get reply
                conn.start();
                mMdc.getUserTransaction().begin();
                message = cons.receive(10000);
                
                // * CLOSE CONNECTION *
                conn.close();
                
                mMdc.getUserTransaction().commit();
            }

            // < TEMPDEST INVALID >>
            {
                conn.close();
                try {
                    tempdest.delete();
                    // FAIL!
                    message = null;
                } catch (JMSException expected) {
                    
                }
            }
            
            // * REOPEN CONNECTION */
            conn = fact.createQueueConnection();
            
            // Forward to Queue2
            if (message != null ){
                QueueSession s = conn.createQueueSession(false, Session.AUTO_ACKNOWLEDGE);
                mMdc.getUserTransaction().begin();
                Queue dest = s.createQueue("Queue2");
                QueueSender prod = s.createSender(dest);
                prod.send(message);
                mMdc.getUserTransaction().commit();
                s.close();
            }
        } catch (Exception e) {
            sLog.errorNoloc("Test error: " + e, e);
        } finally {
            safeClose(conn);
        }
    }
    
    /**
     * Temp dest tests:
     *  
     * ut.begin
     * create temp
     * ut.commit
     * close
     * <temp is invalid>
     * 
     * @param message
     */
    public void requestReplyN3(Message message) {
        QueueConnection conn = null;
        try {

            QueueConnectionFactory fact = (QueueConnectionFactory) mCtx
            .lookup("java:comp/env/queuefact");

            conn = fact.createQueueConnection();
            TemporaryQueue tempdest;
            QueueReceiver cons;
            
            // Send request
            {
                mMdc.getUserTransaction().begin();
                QueueSession s = conn.createQueueSession(false, Session.AUTO_ACKNOWLEDGE);
                tempdest = s.createTemporaryQueue();
                cons = s.createReceiver(tempdest);
                Queue dest = s.createQueue("QueueReplier");
                QueueSender prod = s.createSender(dest);
                message = copy(message, s);
                message.setJMSReplyTo(tempdest);
                prod.send(message);
                mMdc.getUserTransaction().commit();
                
                // Get reply
                conn.start();
                mMdc.getUserTransaction().begin();
                message = cons.receive(10000);
                mMdc.getUserTransaction().commit();
                s.close();

                // * CLOSE CONNECTION *
                conn.close();
            }

            // < TEMPDEST INVALID >>
            {
                conn.close();
                try {
                    tempdest.delete();
                    // FAIL!
                    message = null;
                } catch (JMSException expected) {
                    
                }
            }
            
            // * REOPEN CONNECTION */
            conn = fact.createQueueConnection();
            
            // Forward to Queue2
            if (message != null ){
                QueueSession s = conn.createQueueSession(false, Session.AUTO_ACKNOWLEDGE);
                mMdc.getUserTransaction().begin();
                Queue dest = s.createQueue("Queue2");
                QueueSender prod = s.createSender(dest);
                prod.send(message);
                mMdc.getUserTransaction().commit();
                s.close();
            }
        } catch (Exception e) {
            sLog.errorNoloc("Test error: " + e, e);
        } finally {
            safeClose(conn);
        }
    }

    public void requestReply(Message message) {
        QueueConnection conn = null;
        try {

            QueueConnectionFactory fact = (QueueConnectionFactory) mCtx
            .lookup("java:comp/env/queuefact");

            conn = fact.createQueueConnection();
            TemporaryQueue tempdest;
            QueueReceiver cons;
            
            // Send request
            {
                mMdc.getUserTransaction().begin();
                QueueSession s = conn.createQueueSession(false, Session.AUTO_ACKNOWLEDGE);
                tempdest = s.createTemporaryQueue();
                cons = s.createReceiver(tempdest);
                Queue dest = s.createQueue("QueueReplier");
                QueueSender prod = s.createSender(dest);
                prod.send(message);
                mMdc.getUserTransaction().commit();

                // Get reply
                mMdc.getUserTransaction().begin();
                message = cons.receive(10000);
                mMdc.getUserTransaction().commit();
                s.close();
            }
            
            // Forward to Queue2
            {
                mMdc.getUserTransaction().begin();
                QueueSession s = conn.createQueueSession(false, Session.AUTO_ACKNOWLEDGE);
                Queue dest = s.createQueue("Queue2");
                QueueSender prod = s.createSender(dest);
                prod.send(message);
                mMdc.getUserTransaction().commit();
                s.close();
            }
        } catch (Exception e) {
            sLog.debug("Test error: " + e, e);
        } finally {
            safeClose(conn);
        }
    }
            
    /**
     * Simply passes from Q1 to Q2, and sets the always-invalid test attribute to true
     * before it allocates a connection 
     */
    public void reconnectOutAlwaysInvalid(javax.jms.Message message) throws Exception {
        QueueConnection conn = null;
        try {
            QueueConnectionFactory fact = (QueueConnectionFactory) mCtx
                .lookup("java:comp/env/queuefact");

            // Set test mode
            ((JConnectionFactory) fact).testGetMCF().testSetModeInvalidConnections(true);
            
            conn = fact.createQueueConnection();
            QueueSession s = conn.createQueueSession(false,
                Session.AUTO_ACKNOWLEDGE);
            Queue dest = s.createQueue("Queue2");
            
            QueueSender prod = s.createSender(dest);
            prod.send(message);
        } catch (JMSException e) {
            // Expected
            throw e;
        } finally {
            safeClose(conn);
        }
    }
    
    /**
     * Always performs a rollback
     * @param m
     * @throws Exception
     */
    public void rollback(Message m) throws Exception {
        mMdc.setRollbackOnly();    
    }

    /**
     * Always performs a rollback
     * @param m
     * @throws Exception
     */
    public void throwException(Message m) throws Exception {
        throw new IntentionalException("Intentional: to force rollback");    
    }
    
    private void setPropsForRollback(Message m) throws JMSException {
        m.setStringProperty(Options.MessageProperties.USER_ROLLBACK_DATA_PREFIX + "1", "x");
        m.setStringProperty(Options.MessageProperties.USER_ROLLBACK_DATA_PREFIX + "2", "y");
        
        String ctid = Options.MessageProperties.USER_ROLLBACK_DATA_PREFIX + "ct";
        Integer ct = (Integer) m.getObjectProperty(Delivery.REDELIVERYCOUNT);
        if (ct.intValue() == 0) {
            m.setStringProperty(ctid, ct.toString());
        } else {
            m.setStringProperty(ctid, m.getStringProperty(ctid) + "," + m.getObjectProperty(Delivery.REDELIVERYCOUNT));
        }
        
        String h = m.getStringProperty(Options.MessageProperties.REDELIVERY_HANDLING);
        h = h.replaceAll("queue", "topic");
        m.setStringProperty(Options.MessageProperties.REDELIVERY_HANDLING, h);
    }
    
    /**
     * Always performs a rollback
     * @param m
     * @throws Exception
     */
    public void rollbackSetProps(Message m) throws Exception {
        setPropsForRollback(m);
        mMdc.setRollbackOnly();    
    }
    
    public void stopDelivery(Message m) throws Exception {
        testQQXAXA(m);
        m.setStringProperty(Options.MessageProperties.STOP_CONNECTOR, "Test: stopDelivery()");
    }
    
    public void stopDeliveryThroughAlert(Message m) throws Exception {
        testQQXAXA(m);
        
        // Invoke stop directly
        MBeanServer srv = (MBeanServer) m.getObjectProperty(Options.MessageProperties.MBEANSERVER);
        ObjectName oname = new ObjectName(m.getStringProperty(Options.MessageProperties.MBEANNAME));
        srv.invoke(oname, "stop", new Object[0], new String[0]);
    }
        
    
    /**
     * Always performs a rollback
     * @param m
     * @throws Exception
     */
    public void throwExceptionSetProps(Message m) throws Exception {
        setPropsForRollback(m);
        throw new IntentionalException("Intentional: to force rollback");    
    }
    
    /**
     * Uses a UT that is really ignored underneath
     *
     * @param message Message
     * @throws Exception
     */
    public void testNoXANonTransacted(javax.jms.Message message) throws Exception {
        QueueConnection conn = null;
        try {
            QueueConnectionFactory fact = (QueueConnectionFactory) mCtx
                .lookup("java:comp/env/queuefact");

            sLog.debug("Starting UT");

            // UT
            mMdc.getUserTransaction().begin();
            conn = fact.createQueueConnection();
            QueueSession s = conn.createQueueSession(false,
                Session.AUTO_ACKNOWLEDGE); // arguments should be honored
            Queue dest = s.createQueue("Queue2");
            QueueSender prod = s.createSender(dest);
            prod.send(message);
            safeClose(conn);
            sLog.debug("Commit UT");
            mMdc.getUserTransaction().rollback(); // msg should have been sent anyways
        } finally {
            safeClose(conn);
        }
    }
    
    /**
     * Uses a UT that is really ignored underneath
     *
     * @param message Message
     * @throws Exception
     */
    public void testNoXATransacted(javax.jms.Message message) throws Exception {
        QueueConnection conn = null;
        try {
            QueueConnectionFactory fact = (QueueConnectionFactory) mCtx
                .lookup("java:comp/env/queuefact");

            sLog.debug("Starting UT");

            // UT
            mMdc.getUserTransaction().begin();
            conn = fact.createQueueConnection();
            QueueSession s = conn.createQueueSession(true,
                Session.AUTO_ACKNOWLEDGE); // arguments should ignored
            Queue dest = s.createQueue("Queue2");
            QueueSender prod = s.createSender(dest);
            prod.send(message);
            s.rollback(); // really should rollback
            
            prod.send(message);
            // no session.commit() here: ut.commit() should take of that
            
            safeClose(conn);
            sLog.debug("Commit UT");
            mMdc.getUserTransaction().commit(); // msg should have been sent anyways
        } finally {
            safeClose(conn);
        }
    }
    
    public void Q2XAAndQ3NoTrans(Message message) throws Throwable {
        Connection conn = null;
        try {
            {
                QueueConnectionFactory fact = (QueueConnectionFactory) mCtx
                .lookup("java:comp/env/queuefact");
                conn = fact.createQueueConnection();
                QueueSession s = ((QueueConnection) conn).createQueueSession(true,
                    Session.AUTO_ACKNOWLEDGE);
                Queue dest = s.createQueue("Queue2");
                QueueSender prod = s.createSender(dest);
                prod.send(message);
                conn.close();
            }
            {
                ConnectionFactory ufact = (ConnectionFactory) mCtx
                .lookup("java:comp/env/unifiedfact");
                conn = ufact.createConnection();
                Session s = conn.createSession(false,
                    Session.AUTO_ACKNOWLEDGE);
                Queue dest = s.createQueue("Queue3");
                MessageProducer prod = s.createProducer(dest);
                prod.send(message);
            }
            
        } finally {
            if (conn != null) {
                try {
                    conn.close();
                } catch (JMSException ignore) {
                }
            }
        }
    }

    public void batch(Message message) throws Throwable {
        Connection conn = null;
        try {
            QueueConnectionFactory fact = (QueueConnectionFactory) mCtx
            .lookup("java:comp/env/queuefact");
            conn = fact.createQueueConnection();
            QueueSession s = ((QueueConnection) conn).createQueueSession(true,
                Session.AUTO_ACKNOWLEDGE);
            
            if (message.getObjectProperty(EndOfBatchMessage.KEY_ENDOFBATCH) != null) {
                Queue dest = s.createQueue("Queue3");
                QueueSender prod = s.createSender(dest);
                prod.send(s.createTextMessage("endofbatch"));
            } else {
                Queue dest = s.createQueue("Queue2");
                QueueSender prod = s.createSender(dest);
                prod.send(message);
            }
            conn.close();
        } finally {
            if (conn != null) {
                try {
                    conn.close();
                } catch (JMSException ignore) {
                }
            }
        }
    }

    public void batchUT(Message message) throws Throwable {
        Connection conn = null;
        try {
            mMdc.getUserTransaction().begin();
            QueueConnectionFactory fact = (QueueConnectionFactory) mCtx
            .lookup("java:comp/env/queuefact");
            conn = fact.createQueueConnection();
            QueueSession s = ((QueueConnection) conn).createQueueSession(true,
                Session.AUTO_ACKNOWLEDGE);
            
            if (message.getObjectProperty(EndOfBatchMessage.KEY_ENDOFBATCH) != null) {
                Queue dest = s.createQueue("Queue3");
                QueueSender prod = s.createSender(dest);
                prod.send(s.createTextMessage("endofbatch"));
            } else {
                Queue dest = s.createQueue("Queue2");
                QueueSender prod = s.createSender(dest);
                prod.send(message);
            }
            mMdc.getUserTransaction().commit();
            conn.close();
        } finally {
            if (conn != null) {
                try {
                    conn.close();
                } catch (JMSException ignore) {
                }
            }
        }
    }

    public void batchRollback(Message message) throws Throwable {
        Connection conn = null;
        try {
            QueueConnectionFactory fact = (QueueConnectionFactory) mCtx
            .lookup("java:comp/env/queuefact");
            conn = fact.createQueueConnection();
            QueueSession s = ((QueueConnection) conn).createQueueSession(true,
                Session.AUTO_ACKNOWLEDGE);
            
            if (message.getObjectProperty(EndOfBatchMessage.KEY_ENDOFBATCH) != null) {
                Queue dest = s.createQueue("Queue3");
                QueueSender prod = s.createSender(dest);
                prod.send(s.createTextMessage("endofbatch"));
                if (shouldThrow()) {
                    throw new IntentionalException("BatchRollback");
                }
            } else {
                Queue dest = s.createQueue("Queue2");
                QueueSender prod = s.createSender(dest);
                prod.send(message);
            }
            conn.close();
        } finally {
            if (conn != null) {
                try {
                    conn.close();
                } catch (JMSException ignore) {
                }
            }
        }
    }

    private static List sMsgList = new ArrayList();
    private static Thread sHelperThread;
    
    private void postRequest(Message m, OnDoneHandler h) {
        synchronized (sMsgList) {
            sMsgList.add(h);
            if (sHelperThread == null) {
                sHelperThread = new Thread() {
                    public void run() {
                        for (;;) {
                            OnDoneHandler h;
                            // Get a msg; exit if nothing to do
                            synchronized (sMsgList) {
                                if (sMsgList.isEmpty()) {
                                    sHelperThread = null;
                                    break;
                                } else {
                                    h = (OnDoneHandler) sMsgList.get(0);
                                }
                            }

                            // "process" the message
                            if (h != null) {
                                try {
                                    h.onDone(false);
                                    
                                    // Remove msg from list
                                    synchronized (sMsgList) {
                                        sMsgList.remove(0);
                                    }
                                } catch (Exception e) {
                                    sLog.fatalNoloc("Unexpected: " + e, e);
                                }
                            }
                        }
                    }
                };
                sHelperThread.start();
            }
        }
    }

    // An imaginary callback handler
    public interface OnDoneHandler {
        void onDone(boolean failed) throws Exception;
    };
    
    // The onMessage method
    public void onMessagex(final Message message) {
        try {
            postRequest(message, new OnDoneHandler() {
                public void onDone(boolean failed) throws Exception {
                    if (failed) { 
                        message.setBooleanProperty("JMSJCA.setRollbackOnly", true);
                    }
                    message.acknowledge();
                }
            });
        } catch (Exception e) {
            // Posting failed; rollback
            try {
                message.setBooleanProperty("JMSJCA.setRollbackOnly", true);
                message.acknowledge();
            } catch (JMSException e1) {
                throw new RuntimeException(e1);
            }
        }
    }    
    
    public void onMessagexx(final Message message) {
        try {
            if (message.getObjectProperty("JMSJCA.EndOfBatch") != null) {
                // End of batch
                message.acknowledge();
            } else {
                // Message in middle of batch 
                try {
                    postRequest(message, new OnDoneHandler() {
                        public void onDone(boolean failed) throws Exception {
                            if (failed) { 
                                message.setBooleanProperty("JMSJCA.setRollbackOnly", true);
                            }
                            message.acknowledge();
                        }
                    });
                } catch (Exception e) {
                    // Posting failed; rollback
                    message.setBooleanProperty("JMSJCA.setRollbackOnly", true);
                    message.acknowledge();
                }
            }
        } catch (JMSException e) {
            throw new RuntimeException(e);
        }
    }    

    public void batchHoldUntilAck(final Message message) throws Throwable {
        Connection conn = null;
        
        try {
            QueueConnectionFactory fact = (QueueConnectionFactory) mCtx
            .lookup("java:comp/env/queuefact");
            conn = fact.createQueueConnection();
            QueueSession s = ((QueueConnection) conn).createQueueSession(true,
                Session.AUTO_ACKNOWLEDGE);
            
            if (message.getObjectProperty(EndOfBatchMessage.KEY_ENDOFBATCH) != null) {
                // Normal msg
                Queue dest = s.createQueue("Queue3");
                QueueSender prod = s.createSender(dest);
                prod.send(s.createTextMessage("endofbatch"));
            } else {
                // Done
                Queue dest = s.createQueue("Queue2");
                QueueSender prod = s.createSender(dest);
                prod.send(message);
            }
            
            if (!shouldThrow()) {
                message.acknowledge();
            } else {
                new Thread() {
                    public void run() {
                        try {
                            message.acknowledge();
                        } catch (JMSException e) {
                            sLog.fatalNoloc("ack exception " + e, e);
                        }
                    }
                }.start();
            }
            
            conn.close();
        } finally {
            if (conn != null) {
                try {
                    conn.close();
                } catch (JMSException ignore) {
                }
            }
        }
    }

    public void batchHoldUntilAckRollback(final Message message) throws Throwable {
        Connection conn = null;
        
        try {
            QueueConnectionFactory fact = (QueueConnectionFactory) mCtx
            .lookup("java:comp/env/queuefact");
            conn = fact.createQueueConnection();
            QueueSession s = ((QueueConnection) conn).createQueueSession(true,
                Session.AUTO_ACKNOWLEDGE);
            
            if (message.getObjectProperty(EndOfBatchMessage.KEY_ENDOFBATCH) != null) {
                Queue dest = s.createQueue("Queue3");
                QueueSender prod = s.createSender(dest);
                prod.send(s.createTextMessage("endofbatch"));
            } else {
                Queue dest = s.createQueue("Queue2");
                QueueSender prod = s.createSender(dest);
                prod.send(message);
            }
            
            if (!shouldThrow()) {
                if (shouldThrow()) {
                    message.setBooleanProperty(WMessageIn.SETROLLBACKONLY, true);
                    Queue dest = s.createQueue("Queue4");
                    QueueSender prod = s.createSender(dest);
                    prod.send(s.createTextMessage("should have been rolled back"));
                }
                message.acknowledge();
            } else {
                new Thread() {
                    public void run() {
                        try {
                            if (shouldThrow()) {
                                message.setBooleanProperty(WMessageIn.SETROLLBACKONLY, true);
                            }
                            message.acknowledge();
                        } catch (JMSException e) {
                            sLog.fatalNoloc("ack exception " + e, e);
                        }
                    }
                }.start();
            }
            
            conn.close();
        } finally {
            if (conn != null) {
                try {
                    conn.close();
                } catch (JMSException ignore) {
                }
            }
        }
    }
    
    public void batchHUAUT(final Message message) throws Throwable {
        Connection conn = null;
        
        try {
            mMdc.getUserTransaction().begin();
            
            QueueConnectionFactory fact = (QueueConnectionFactory) mCtx
            .lookup("java:comp/env/queuefact");
            conn = fact.createQueueConnection();
            QueueSession s = ((QueueConnection) conn).createQueueSession(true,
                Session.AUTO_ACKNOWLEDGE);
            
            if (message.getObjectProperty(EndOfBatchMessage.KEY_ENDOFBATCH) != null) {
                // Normal msg
                Queue dest = s.createQueue("Queue3");
                QueueSender prod = s.createSender(dest);
                prod.send(s.createTextMessage("endofbatch"));
            } else {
                // Done
                Queue dest = s.createQueue("Queue2");
                QueueSender prod = s.createSender(dest);
                prod.send(message);
            }
            
            if (!shouldThrow()) {
                message.acknowledge();
            } else {
                new Thread() {
                    public void run() {
                        try {
                            message.acknowledge();
                        } catch (JMSException e) {
                            sLog.fatalNoloc("ack exception " + e, e);
                        }
                    }
                }.start();
            }

            mMdc.getUserTransaction().commit();
            conn.close();
        } finally {
            if (conn != null) {
                try {
                    conn.close();
                } catch (JMSException ignore) {
                }
            }
        }
    }
    
    public void batchHUAUTRB(final Message message) throws Throwable {
        Connection conn = null;
        
        try {
            mMdc.getUserTransaction().begin();
            
            final boolean rollback = shouldThrow();
            
            QueueConnectionFactory fact = (QueueConnectionFactory) mCtx
            .lookup("java:comp/env/queuefact");
            conn = fact.createQueueConnection();
            QueueSession s = ((QueueConnection) conn).createQueueSession(true,
                Session.AUTO_ACKNOWLEDGE);
            
            if (message.getObjectProperty(EndOfBatchMessage.KEY_ENDOFBATCH) != null) {
                // Normal msg
                Queue dest = s.createQueue("Queue3");
                QueueSender prod = s.createSender(dest);
                prod.send(s.createTextMessage("endofbatch"));
            } else {
                // Done
                Queue dest = s.createQueue("Queue2");
                QueueSender prod = s.createSender(dest);
                prod.send(message);
            }
            
            if (!shouldThrow()) {
                if (rollback) {
                    message.setBooleanProperty(WMessageIn.SETROLLBACKONLY, true);
                }
                message.acknowledge();
            } else {
                new Thread() {
                    public void run() {
                        try {
                            if (rollback) {
                                message.setBooleanProperty(WMessageIn.SETROLLBACKONLY, true);
                            }
                            message.acknowledge();
                        } catch (JMSException e) {
                            sLog.fatalNoloc("ack exception " + e, e);
                        }
                    }
                }.start();
            }

            if (rollback) {
                mMdc.getUserTransaction().rollback();
            } else {
                mMdc.getUserTransaction().commit();
            }
            conn.close();
        } finally {
            if (conn != null) {
                try {
                    conn.close();
                } catch (JMSException ignore) {
                }
            }
        }
    }

    /**
     * Sends from Queue1 to Queue2; the output queue cf is looked in JNDI
     *
     * @param message Message
     */
    public void testGlobalFact(javax.jms.Message message) {
        Connection conn = null;
        try {
            ConnectionFactory fact = (ConnectionFactory) mCtx
            .lookup(message.getStringProperty("cf"));
            conn = fact.createConnection();
            Session s = conn.createSession(true,
                Session.AUTO_ACKNOWLEDGE);
            Queue dest = s.createQueue("Queue2");
            MessageProducer prod = s.createProducer(dest);
            prod.send(message);

            if (sLog.isDebugEnabled()) {
                sLog.debug("Msg sent to " + dest.getQueueName());
            }
        } catch (Exception e) {
            sLog.errorNoloc("Failed: " + e, e);
            throw new EJBException("Failed: " + e, e);
        } finally {
            if (conn != null) {
                try {
                    conn.close();
                } catch (JMSException ignore) {
                }
            }
        }
    }
}
