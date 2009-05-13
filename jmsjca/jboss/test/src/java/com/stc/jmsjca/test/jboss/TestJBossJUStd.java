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

package com.stc.jmsjca.test.jboss;

import com.stc.jmsjca.core.XXid;
import com.stc.jmsjca.test.core.EndToEndBase;
import com.stc.jmsjca.test.core.JMSProvider;

import javax.jms.Queue;
import javax.jms.QueueConnection;
import javax.jms.QueueConnectionFactory;
import javax.jms.QueueSender;
import javax.jms.QueueSession;
import javax.jms.Session;
import javax.jms.TextMessage;
import javax.jms.TopicConnectionFactory;
import javax.jms.XAConnectionFactory;
import javax.jms.XAQueueConnection;
import javax.jms.XAQueueConnectionFactory;
import javax.jms.XAQueueSession;
import javax.jms.XATopicConnectionFactory;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.transaction.xa.XAResource;
import javax.transaction.xa.Xid;

import java.util.Date;
import java.util.Hashtable;

/**
 * <code>
 * Unit tests
*  See Base
 *
 * @author fkieviet 
 * @version 1.0
 */
public class TestJBossJUStd extends EndToEndBase {
    public final static String JNDI_FACTORY = "org.jnp.interfaces.NamingContextFactory";
    public final static String PKGS = "org.jboss.naming:org.jnp.interfaces";
    public final static String FACT = "UIL2XAConnectionFactory";
    

    /**
     * @see com.stc.jmsjca.test.core.EndToEndBase#getJMSProvider()
     */
    @Override
    public JMSProvider getJMSProvider() {
        return new JBossProvider();
    }
    
    public static String getJNDIUrl() {
        String url = System.getProperty("jboss.url");
        if (url == null) {
            throw new RuntimeException("System property [jboss.url] was not found, " +
                    "should be set to JBoss server, e.g. jnp://blue:1099");
        }
        return url;
    }
    
    public static String getConnectionUrl() {
        String url = getJNDIUrl();
        return url;
    }

    public static Context getInitialContext() throws Exception {
        Hashtable<String, String> env = new Hashtable<String, String>();
        env.put(Context.INITIAL_CONTEXT_FACTORY, JNDI_FACTORY);
        env.put(Context.PROVIDER_URL, getJNDIUrl());
        env.put(Context.URL_PKG_PREFIXES, PKGS);
        InitialContext ctx = new InitialContext(env);
        return ctx;
    }


    public void test001() throws Throwable {
        Context ctx = getInitialContext();

        QueueConnectionFactory qconFactory = (QueueConnectionFactory) ctx.lookup(FACT);
        System.out.println("fact = " + qconFactory);
        System.out.println("fact instance of   tcf = " + (qconFactory instanceof TopicConnectionFactory));
        System.out.println("fact instance of xaqcf = " + (qconFactory instanceof XAQueueConnectionFactory));
        System.out.println("fact instance of xatcf = " + (qconFactory instanceof XATopicConnectionFactory));
        System.out.println("fact instance of  xacf = " + (qconFactory instanceof XAConnectionFactory));

        QueueConnection qcon = qconFactory.createQueueConnection();
        QueueSession qsession = qcon.createQueueSession(false, Session.AUTO_ACKNOWLEDGE);
        qcon.start();

        Queue queue = (Queue) ctx.lookup("queue/Queue1");
        QueueSender qsender = qsession.createSender(queue);
        TextMessage msg = qsession.createTextMessage("hello Queue1 " + (new Date(System.currentTimeMillis())));
        qsender.send(msg);
    
        queue = qsession.createQueue("Queue2");
        qsender = qsession.createSender(queue);
        msg = qsession.createTextMessage("hello Queue2 " + (new Date(System.currentTimeMillis())));
        qsender.send(msg);
    }
    
    public void test002() throws Throwable {
        Context ctx = getInitialContext();
        XAQueueConnectionFactory qconFactory = (XAQueueConnectionFactory) ctx.lookup(FACT);
        ctx.close();

        XAQueueConnection qcon = qconFactory.createXAQueueConnection();
        XAQueueSession xaqsession = qcon.createXAQueueSession();
        
        XAResource xar = xaqsession.getXAResource();
        Xid xid = new XXid();
        xar.start(xid, XAResource.TMNOFLAGS);
        
        Queue queue = xaqsession.createQueue("Queue1");
        QueueSender qsender = xaqsession.getQueueSession().createSender(queue);
        TextMessage msg = xaqsession.createTextMessage("msg sent in XA");
        qsender.send(msg);
        
        xar.end(xid, XAResource.TMSUCCESS);
        xar.commit(xid, true);
        
        qcon.start();
    }
}
