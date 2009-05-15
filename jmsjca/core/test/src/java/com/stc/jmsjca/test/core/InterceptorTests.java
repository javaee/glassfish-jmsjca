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

import com.stc.jmsjca.core.RAJMSActivationSpec;
import com.stc.jmsjca.core.RAJMSResourceAdapter;
import com.stc.jmsjca.core.XMCFQueueXA;
import com.stc.jmsjca.core.XManagedConnectionFactory;
import com.stc.jmsjca.util.InterceptorInfo;
import com.stc.jmsjca.util.InterceptorLoader;

import javax.interceptor.AroundInvoke;
import javax.interceptor.InvocationContext;
import javax.jms.Connection;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.QueueConnectionFactory;
import javax.jms.Session;

import java.lang.reflect.Method;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Tests interceptors
 * 
 * @author fkieviet
 */
public abstract class InterceptorTests extends EndToEndBase {
    
    public static class Interceptor1 {
        public static AtomicInteger sInvocationCount = new AtomicInteger();
        
        @AroundInvoke
        public Object onMessage(InvocationContext ctx) throws Exception {
            System.out.println("Interceptor 1 enter");
            try {
                sInvocationCount.incrementAndGet();
                return ctx.proceed();
            } finally {
                System.out.println("Interceptor 1 exit");
            }
        }
    }
    
    public static class Interceptor2 {
        public static AtomicInteger sInvocationCount = new AtomicInteger();
        
        @AroundInvoke
        public Object intercept(InvocationContext ctx) throws Exception {
            System.out.println("Interceptor 2 enter");
            try {
                sInvocationCount.incrementAndGet();
                return ctx.proceed();
            } finally {
                System.out.println("Interceptor 2 exit");
            }
        }
    }
    
    /**
     * Tests finding the interceptor method
     * 
     * @throws Exception
     */
    public void testAnnot1() throws Exception {
        Method interceptor = InterceptorLoader.getInterceptor(Interceptor1.class);
        assertTrue(interceptor.equals(Interceptor1.class.getMethod("onMessage", InvocationContext.class)));
    }
    
    /**
     * Test loading the default interceptor
     * 
     * @throws Exception
     */
    public void testLoader() throws Exception {
        Set<InterceptorInfo> interceptors = InterceptorLoader.getInterceptors();
        assertTrue(interceptors.size() == 2);
    }
    
    /**
     * Tries to load a non existing interceptor
     * 
     * @throws Throwable
     */
    public void testLoaderFault() throws Throwable {
        try {
            InterceptorLoader.getInterceptors("jmsjca.faulty1");
            throw new Throwable("Not thrown");
        } catch (Exception expected) {
            // ignore
        }
    }
    
    /**
     * Tests the default interceptor scenario (two interceptors) in a mock container
     * 
     * @throws Throwable
     */
    public void testInterceptorAdapter() throws Throwable {
        Passthrough p = getJMSProvider().createPassthrough(mServerProperties);

        // RA
        RAJMSResourceAdapter ra = getJMSProvider().createRA();
        ra.setConnectionURL(getJMSProvider().getConnectionUrl(this));
        ra.setUserName(getJMSProvider().getUserName(mServerProperties));
        ra.setPassword(getJMSProvider().getPassword(mServerProperties));

        // Spec
        RAJMSActivationSpec spec = ra.createObjectFactory(ra.getConnectionURL()).createActivationSpec();
        spec.setUserName(p.getUserid());
        spec.setPassword(p.getPassword());
        spec.setConcurrencyMode("sync");
        spec.setDestinationType(javax.jms.Queue.class.getName());
        spec.setDestination(p.getQueue1Name());
        spec.setEndpointPoolMaxSize("1");

        // MCF
        XManagedConnectionFactory mcf = new XMCFQueueXA();
        mcf.setResourceAdapter(ra);
        final QueueConnectionFactory fact = (QueueConnectionFactory) mcf.createConnectionFactory(null);
        
        // An MDB
        final MessageListener mdb = new MessageListener() {
            public void onMessage(Message msg) {
                try {
                    System.out.println("Final endpoint");
                    Connection c = fact.createConnection(); 
                    Session s = c.createSession(false, Session.AUTO_ACKNOWLEDGE);
                    s.createProducer(s.createQueue("Queue2")).send(msg);
                    c.close();
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
            
            Interceptor1.sInvocationCount.set(0);
            Interceptor2.sInvocationCount.set(0);
            
            p.passFromQ1ToQ2();
            ra.endpointDeactivation(c.getMessageEndpointFactory(), spec);
            p.get(p.getQueue1Name()).assertEmpty();
            
            assertTrue(Interceptor1.sInvocationCount.get() == p.getNMessagesToSend());
            assertTrue(Interceptor2.sInvocationCount.get() == p.getNMessagesToSend());
        } finally {
            Passthrough.safeClose(p);
        }
    }
    
    // Tests to add:
    // -------------
    // Throw exceptions
    // Context data
    // Sending msg from the interceptor
    // Different configurations specified in the URL
    // Classloading test when adding the interceptor to the EAR
    
}
