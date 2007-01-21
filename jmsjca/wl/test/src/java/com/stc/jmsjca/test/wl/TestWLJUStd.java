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
 * $RCSfile: TestWLJUStd.java,v $
 * $Revision: 1.1.1.1 $
 * $Date: 2007-01-21 07:52:12 $
 *
 * Copyright 2003-2007 Sun Microsystems, Inc. All Rights Reserved.  
 */

package com.stc.jmsjca.test.wl;

import com.stc.jmsjca.test.core.EndToEndBase;
import com.stc.jmsjca.test.core.Passthrough;
import com.stc.jmsjca.wl.RAWLObjectFactory;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.Queue;
import javax.jms.QueueConnection;
import javax.jms.QueueConnectionFactory;
import javax.jms.QueueSender;
import javax.jms.QueueSession;
import javax.jms.Session;
import javax.jms.TextMessage;
import javax.jms.Topic;
import javax.jms.XAConnectionFactory;
import javax.jms.XAQueueConnection;
import javax.jms.XAQueueConnectionFactory;
import javax.jms.XAQueueSession;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import java.util.Hashtable;
import java.util.Iterator;
import java.util.Properties;
import java.util.Map.Entry;

/**
 * <code>
 * Unit tests
*  See Base
 *
 * @author fkieviet 
 * @version 1.0
 */
public class TestWLJUStd extends EndToEndBase {
    public final static String JNDI_FACTORY = "weblogic.jndi.WLInitialContextFactory";

    public static String getJNDIUrl() {
        String url = System.getProperty("wl.url");
        if (url == null) {
            throw new RuntimeException("System property [wl.url] was not found, " +
                    "should be set to WebLogic server, e.g. t3://blue:7001");
        }
        return url;
    }
    
    public static String getConnectionUrl() {
        String url = getJNDIUrl();
        return url;
    }

    public static InitialContext getInitialContext() throws Exception {
        Hashtable env = new Hashtable();
        env.put(Context.INITIAL_CONTEXT_FACTORY, JNDI_FACTORY);
        env.put(Context.PROVIDER_URL, getJNDIUrl());
        System.out.println("Connecting to " + getJNDIUrl());
        InitialContext ctx = new InitialContext(env);
        return ctx;
    }

    public void test001() throws Throwable {
        Context ctx = getInitialContext();

        QueueConnectionFactory qconFactory = (QueueConnectionFactory) ctx.lookup("weblogic.jms.XAConnectionFactory");
        QueueConnection qcon = qconFactory.createQueueConnection();
        QueueSession qsession = qcon.createQueueSession(false, Session.AUTO_ACKNOWLEDGE);
        Queue queue = (Queue) ctx.lookup("Queue1");
//        Queue queue = (Queue) qsession.createQueue("examplesJMSServer/QueueX");

        QueueSender qsender = qsession.createSender(queue);
        TextMessage msg = qsession.createTextMessage("hello");
        qsender.send(msg);
        qcon.start();
        qcon.close();
    }
    
    public void xxxxxxxxxxxxxxxx_test001a() throws Throwable {
        Context ctx = getInitialContext();

        QueueConnectionFactory qconFactory = (QueueConnectionFactory) ctx.lookup("weblogic.jms.XAConnectionFactory");
        QueueConnection qcon = qconFactory.createQueueConnection();
        QueueSession qsession = qcon.createQueueSession(false, Session.AUTO_ACKNOWLEDGE);
        Queue queue = (Queue) ctx.lookup("examplesJMSServer/Queue1");
        queue = (Queue) ctx.lookup("examplesJMSServer/Queue2");
        queue = (Queue) ctx.lookup("examplesJMSServer/Queue3");
//        Queue queue = (Queue) qsession.createQueue("examplesJMSServer/QueueX");

        QueueSender qsender = qsession.createSender(queue);
        TextMessage msg = qsession.createTextMessage("hello");
        qsender.send(msg);
        qcon.start();
        qcon.close();
    }

    public void test002() throws Throwable {
        Context ctx = getInitialContext();
        
        XAQueueConnectionFactory qconFactory = (XAQueueConnectionFactory) ctx.lookup("weblogic.jms.XAConnectionFactory");
        XAQueueConnection qcon = qconFactory.createXAQueueConnection();
        XAQueueSession xaqsession = qcon.createXAQueueSession();
        
        for (Iterator it = System.getProperties().entrySet().iterator(); it.hasNext(); ) {
            Entry e = (Entry) it.next();
            System.out.println(e.getKey() + "=" + e.getValue());
        }
        
        try {
            xaqsession.getXAResource(); // Fails with IllegalStateException <055059>
            throw new Throwable("Exception expected");
        } catch (Exception ignore) {
            // ignore
        }
        qcon.close();

        //        Xid xid = new XXid();
//        xar.start(xid, XAResource.TMNOFLAGS);
//        
//        Queue queue = (Queue) ctx.lookup("Queue1");
//        QueueSender qsender = xaqsession.getQueueSession().createSender(queue);
//        TextMessage msg = xaqsession.createTextMessage("hello");
//        qsender.send(msg);
//        
//        xar.end(xid, XAResource.TMSUCCESS);
//        xar.commit(xid, true);
//        
//        qcon.start();
    }
    
//    public void test003() throws Throwable {
//        Context ctx = getInitialContext();
//
//        QueueConnectionFactory qconFactory = (QueueConnectionFactory) ctx.lookup("corbaname:iiop:1.2@blue:7001#weblogic.jms.XAConnectionFactory");
//        QueueConnection qcon = qconFactory.createQueueConnection(null, null);
//        QueueSession qsession = qcon.createQueueSession(false, Session.AUTO_ACKNOWLEDGE);
//        Queue queue = (Queue) ctx.lookup("Queue1");
//        qcon.close();
//    }
    
    public void test004() throws Throwable {
        Passthrough p = createPassthrough(mServerProperties);
        Passthrough.QueueSource source = p.new QueueSource("Queue1");
        source.connect();
        source.drain();
        p.close();
    }
    
    public void test005() throws Throwable {
        Object o = lookup("weblogic.jms.ConnectionFactory");
        ConnectionFactory f = (ConnectionFactory) o;
        assertTrue(f != null);
    }
    
    public void testClientID() throws Throwable {
        Object o = lookup("weblogic.jms.XAConnectionFactory");
        XAConnectionFactory f = (XAConnectionFactory) o;
        assertTrue(f != null);
        
        Connection c = null; 
        try {
            c = f.createXAConnection();
            c.setClientID("xyz");
            Session s = c.createSession(true, Session.SESSION_TRANSACTED);
            Topic t = (Topic) lookup("Topic1");
//            Topic t = s.createTopic("examples-jms!examplesJMSServer/Topic1");
            s.createDurableSubscriber(t, "x");

            c.close();
            
            c = f.createXAConnection();
            c.setClientID("xyz");
            s = c.createSession(true, Session.SESSION_TRANSACTED);
            s.unsubscribe("x");
        } finally {
            safeClose(c);
        }
    }
    
    private Object lookup(String name) throws NamingException, Exception {
        InitialContext ctx = null;
        try {
            ctx = getInitialContext();
            return ctx.lookup(name);
        } finally {
            RAWLObjectFactory.safeClose(ctx);
        }
    }

    public Passthrough createPassthrough(Properties serverProperties) {
        return new WLPassthrough(new Properties());
    }
}
