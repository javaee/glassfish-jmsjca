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

import com.stc.jmsjca.container.Container;
import com.stc.jmsjca.container.EmbeddedDescriptor;
import com.stc.jmsjca.core.Options;
import com.stc.jmsjca.core.RAJMSActivationSpec;
import com.stc.jmsjca.core.RAJMSResourceAdapter;
import com.stc.jmsjca.core.XMCFQueueXA;
import com.stc.jmsjca.core.XMCFTopicXA;
import com.stc.jmsjca.core.XMCFUnifiedXA;
import com.stc.jmsjca.core.XManagedConnectionFactory;
import com.stc.jmsjca.util.InterceptorInfo;
import com.stc.jmsjca.util.InterceptorLoader;
import com.stc.jmsjca.util.SampleInterceptor;

import javax.interceptor.AroundInvoke;
import javax.interceptor.InvocationContext;
import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.MessageProducer;
import javax.jms.ObjectMessage;
import javax.jms.QueueConnection;
import javax.jms.QueueConnectionFactory;
import javax.jms.QueueSender;
import javax.jms.QueueSession;
import javax.jms.Session;
import javax.jms.TopicConnection;
import javax.jms.TopicConnectionFactory;
import javax.jms.TopicPublisher;
import javax.jms.TopicSession;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.Set;

/**
 * Tests interceptors
 * 
 * @author fkieviet
 */
public abstract class InterceptorTests extends EndToEndBase {
    
    public static class Interceptor1 {
        public static Map<String, Integer> sInvocationCount = new HashMap<String, Integer>();
        
        @AroundInvoke
        public Object onMessage(InvocationContext ctx) throws Exception {
            try {
                String m = ctx.getMethod().getName() + ctx.getParameters().length;
                synchronized (sInvocationCount) {
                    Integer count = sInvocationCount.get(m);
                    sInvocationCount.put(m, count == null ? 1 : count + 1);
                }
                return ctx.proceed();
            } finally {
            }
        }
    }
    
    public static class Interceptor2 {
        public static Map<String, Integer> sInvocationCount = new HashMap<String, Integer>();

        @AroundInvoke
        public Object intercept(InvocationContext ctx) throws Exception {
            try {
                String m = ctx.getMethod().getName() + ctx.getParameters().length;
                synchronized (sInvocationCount) {
                    Integer count = sInvocationCount.get(m);
                    sInvocationCount.put(m, count == null ? 1 : count + 1);
                }
                return ctx.proceed();
            } finally {
            }
        }
    }
    
    public static class Interceptor3 {
        private static Object sInterceptor;
        private static Method sMethod;
        
        public static void set(Object interceptor) throws Exception {
            if (interceptor == null) {
                sMethod = null;
            } else {
                sMethod = InterceptorLoader.getInterceptor(interceptor.getClass());
            }
            sInterceptor = interceptor;
        }
        
        @AroundInvoke
        public Object intercept(InvocationContext ctx) throws Exception {
            if (sInterceptor != null) {
                return sMethod.invoke(sInterceptor, ctx);
            }
            return ctx.proceed();
        }
    }
    
    /**
     * Saves the message id of an inbound message on an outbound message if the
     * inbound message was intercepted and if the outbound message is sent on the
     * same thread as the inbound message was received. This matches a simple 
     * queue-to-queue scenario: an MDB receives a msg and in the MDB another message
     * is sent. 
     * 
     * @author fkieviet
     */
    public static class IDCopier {
        // Inbound and outbound are using different interceptor instances, so
        // use a threadlocal to remember the input message
        private static ThreadLocal<Message> inputMessage = new ThreadLocal<Message>();
        
        @AroundInvoke
        public Object intercept(InvocationContext ctx) throws Exception {
            if (ctx.getMethod().getName().equals("onMessage")) {
                // For inbound, save the message so that it can be used later for outbound
                try {
                    inputMessage.set((Message) ctx.getParameters()[0]);
                    return ctx.proceed();
                } finally {
                    inputMessage.set(null);
                }
            } else {
                // For outbound, try to copy the inbound message id to the outbound message
                // before it is sent
                if (inputMessage.get() != null) {
                    Message outputMessage = null;
                    for (Object o : ctx.getParameters()) {
                        if (o instanceof Message) {
                            outputMessage = (Message) o;
                        }
                    }
                    if (outputMessage != null) {
                        try {
                            outputMessage.setStringProperty("OriginalID"
                                , inputMessage.get().getJMSMessageID());
                        } catch (JMSException ignore) {
                            System.out.println(ignore);
                            // message may not be writable
                        }
                    }
                }
                return ctx.proceed();
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
        Set<InterceptorInfo> interceptors = InterceptorLoader.getInterceptors(Options.Interceptor.DEFAULT_SERVICENAME);
        assertTrue(interceptors.size() == 3);
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
     * Sends messages from Queue1 to Queue2 and Queue3, and counts the number of 
     * times the interceptors are triggered for particular methods.
     * 
     * @author fkieviet
     */
    public abstract class MockTestQueue <T> implements MessageListener {
        Passthrough p;
        RAJMSResourceAdapter ra;
        RAJMSActivationSpec spec;
        
        T fact;
        
        public MockTestQueue() throws Exception {
            p = getJMSProvider().createPassthrough(mServerProperties);

            // RA
            ra = getJMSProvider().createRA();
            ra.setConnectionURL(getJMSProvider().getConnectionUrl(InterceptorTests.this));
            ra.setUserName(getJMSProvider().getUserName(mServerProperties));
            ra.setPassword(getJMSProvider().getPassword(mServerProperties));

            // Spec
            spec = ra.createObjectFactory(ra.getConnectionURL()).createActivationSpec();
            spec.setUserName(p.getUserid());
            spec.setPassword(p.getPassword());
            spec.setConcurrencyMode("sync");
            spec.setDestinationType(javax.jms.Queue.class.getName());
            spec.setDestination(p.getQueue1Name());
            spec.setEndpointPoolMaxSize("1");
        }

        public abstract void onMessage(Message m);
        
        public abstract void setup() throws Exception;

        public void doTest() throws Exception {
            try {
                setup();
                MockContainer c = new MockContainer(this);
                ra.start(c.getBootstrapContext());
                ra.endpointActivation(c.getMessageEndpointFactory(), spec);
                p.setNMessagesToSend(50);
                p.clearQ1Q2Q3();

                Interceptor1.sInvocationCount.clear();
                Interceptor2.sInvocationCount.clear();

                p.passFromQ1ToQ2();
                ra.endpointDeactivation(c.getMessageEndpointFactory(), spec);
                p.get(p.getQueue1Name()).assertEmpty();
                assertTrue(Interceptor1.sInvocationCount.get("onMessage1") == p.getNMessagesToSend());
                assertTrue(Interceptor2.sInvocationCount.get("onMessage1") == p.getNMessagesToSend());
                verify();
            } finally {
                Passthrough.safeClose(p);
                if (fact != null) {
                    getConnectionManager((ConnectionFactory) fact).clearAll();
                }
            }
        }
        
        public abstract void verify();
    }
    
    /**
     * Queue1 to Topic2
     * 
     * @author fkieviet
     */
    public abstract class MockTestTopic<T> extends MockTestQueue<T> {
        public MockTestTopic() throws Exception {
            super();
        }
        
        @Override
        public void doTest() throws Exception {
            try {
                setup();
                MockContainer c = new MockContainer(this);
                ra.start(c.getBootstrapContext());
                ra.endpointActivation(c.getMessageEndpointFactory(), spec);
                p.setNMessagesToSend(50);
                p.clearQ1Q2Q3();

                Interceptor1.sInvocationCount.clear();
                Interceptor2.sInvocationCount.clear();

                p.passFromQ1ToT2();
                ra.endpointDeactivation(c.getMessageEndpointFactory(), spec);
                p.get(p.getTopic2Name()).assertEmpty();
                assertTrue(Interceptor1.sInvocationCount.get("onMessage1") == p.getNMessagesToSend());
                assertTrue(Interceptor2.sInvocationCount.get("onMessage1") == p.getNMessagesToSend());
                verify();
            } finally {
                Passthrough.safeClose(p);
                if (fact != null) {
                    getConnectionManager((ConnectionFactory) fact).clearAll();
                }
            }
        }
    }    
    
    public static com.stc.jmsjca.core.XDefaultConnectionManager getConnectionManager(javax.jms.ConnectionFactory f) {
        com.stc.jmsjca.core.JConnectionFactory ff = (com.stc.jmsjca.core.JConnectionFactory) f;
        return (com.stc.jmsjca.core.XDefaultConnectionManager) ff.getConnectionManager();
    }
    
    
    /**
     * Tests the MessageProducer interceptors
     * 
     * @throws Exception
     */
    public void testMessageProducer() throws Exception {
        MockTestQueue<ConnectionFactory> m = new MockTestQueue<ConnectionFactory>() {
            @Override
            public void verify() {
                assertTrue(Interceptor1.sInvocationCount.get("send1") == p.getNMessagesToSend() / 5 * 2);
                assertTrue(Interceptor2.sInvocationCount.get("send1") == p.getNMessagesToSend() / 5 * 2);
                assertTrue(Interceptor2.sInvocationCount.get("send2") == p.getNMessagesToSend() / 5);
                assertTrue(Interceptor2.sInvocationCount.get("send4") == p.getNMessagesToSend() / 5);
                assertTrue(Interceptor2.sInvocationCount.get("send5") == p.getNMessagesToSend() / 5);
            }
            
            @Override
            public void setup() throws Exception {
                XManagedConnectionFactory mcf = new XMCFUnifiedXA();
                mcf.setResourceAdapter(ra);
                fact = (ConnectionFactory) mcf.createConnectionFactory(null);
            }
            
            int testno;

            @Override
            public void onMessage(Message msg) {
                Connection c = null;
                try {
                    c = fact.createConnection(); 
                    Session s = c.createSession(false, Session.AUTO_ACKNOWLEDGE);
                    
                    if (++testno == 5) {
                        testno = 0;
                    }
                    switch (testno) {
                    case 0: {
                        // send1
                        MessageProducer p = s.createProducer(s.createQueue("Queue2"));  
                        p.send(msg);
                        break;
                    }
                    case 1: {
                        // send2
                        MessageProducer p = s.createProducer(null);  
                        p.send(s.createQueue("Queue2"), msg);
                        break;
                    }
                    case 2: {
                        // send1
                        MessageProducer p = s.createProducer(s.createQueue("Queue2"));  
                        p.send(msg);
                        break;
                    }
                    case 3: {
                        // send4
                        MessageProducer p = s.createProducer(s.createQueue("Queue2"));  
                        p.send(msg, p.getDeliveryMode(), p.getPriority(), p.getTimeToLive());
                        break;
                    }
                    case 4: {
                        // send5
                        MessageProducer p = s.createProducer(null);  
                        p.send(s.createQueue("Queue2"), msg, p.getDeliveryMode(), p.getPriority(), p.getTimeToLive());
                        break;
                    }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    safeClose(c);
                }
            }
        };
        
        m.doTest();
    }
    
    /**
     * Tests the QueueSender interceptors
     * 
     * @throws Exception
     */
    public void testQueueSender() throws Exception {
        MockTestQueue<QueueConnectionFactory> m = new MockTestQueue<QueueConnectionFactory>() {
            @Override
            public void verify() {
                assertTrue(Interceptor1.sInvocationCount.get("send1") == p.getNMessagesToSend() / 5 * 2);
                assertTrue(Interceptor2.sInvocationCount.get("send1") == p.getNMessagesToSend() / 5 * 2);
                assertTrue(Interceptor2.sInvocationCount.get("send2") == p.getNMessagesToSend() / 5);
                assertTrue(Interceptor2.sInvocationCount.get("send4") == p.getNMessagesToSend() / 5);
                assertTrue(Interceptor2.sInvocationCount.get("send5") == p.getNMessagesToSend() / 5);
            }
            
            @Override
            public void setup() throws Exception {
                XManagedConnectionFactory mcf = new XMCFQueueXA();
                mcf.setResourceAdapter(ra);
                fact = (QueueConnectionFactory) mcf.createConnectionFactory(null);
            }
            
            int testno;

            @Override
            public void onMessage(Message msg) {
                QueueConnection c = null;
                try {
                    c = fact.createQueueConnection(); 
                    QueueSession s = c.createQueueSession(false, Session.AUTO_ACKNOWLEDGE);
                    
                    if (++testno == 5) {
                        testno = 0;
                    }
                    switch (testno) {
                    case 0: {
                        // send1
                        QueueSender p = s.createSender(s.createQueue("Queue2"));  
                        p.send(msg);
                        break;
                    }
                    case 1: {
                        // send2
                        QueueSender p = s.createSender(null);  
                        p.send(s.createQueue("Queue2"), msg);
                        break;
                    }
                    case 2: {
                        // send1
                        MessageProducer p = s.createProducer(s.createQueue("Queue2"));  
                        p.send(msg);
                        break;
                    }
                    case 3: {
                        // send4
                        QueueSender p = s.createSender(s.createQueue("Queue2"));  
                        p.send(msg, p.getDeliveryMode(), p.getPriority(), p.getTimeToLive());
                        break;
                    }
                    case 4: {
                        // send5
                        QueueSender p = s.createSender(null);  
                        p.send(s.createQueue("Queue2"), msg, p.getDeliveryMode(), p.getPriority(), p.getTimeToLive());
                        break;
                    }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    safeClose(c);
                }
            }
        };
        
        m.doTest();
    }
    
    /**
     * Tests the Publisher interceptors
     * 
     * @throws Exception
     */
    public void testPublisher() throws Exception {
        MockTestTopic<TopicConnectionFactory> m = new MockTestTopic<TopicConnectionFactory>() {
            @Override
            public void verify() {
                assertTrue(Interceptor1.sInvocationCount.get("publish1") == p.getNMessagesToSend() / 5);
                assertTrue(Interceptor2.sInvocationCount.get("publish1") == p.getNMessagesToSend() / 5);
                assertTrue(Interceptor2.sInvocationCount.get("publish2") == p.getNMessagesToSend() / 5);
                assertTrue(Interceptor2.sInvocationCount.get("publish4") == p.getNMessagesToSend() / 5);
                assertTrue(Interceptor2.sInvocationCount.get("publish5") == p.getNMessagesToSend() / 5);
                assertTrue(Interceptor2.sInvocationCount.get("send5") == p.getNMessagesToSend() / 5);
            }
            
            @Override
            public void setup() throws Exception {
                XManagedConnectionFactory mcf = new XMCFTopicXA();
                mcf.setResourceAdapter(ra);
                fact = (TopicConnectionFactory) mcf.createConnectionFactory(null);
            }

            int testno = 0;

            @Override
            public void onMessage(Message msg) {
                TopicConnection c = null;
                try {
                    c = fact.createTopicConnection(); 
                    TopicSession s = c.createTopicSession(false, Session.AUTO_ACKNOWLEDGE);
                    
                    
                    if (++testno == 5) {
                        testno = 0;
                    }
                    switch (testno) {
                    case 0: {
                        // publish1
                        TopicPublisher p = s.createPublisher(s.createTopic("Topic2"));  
                        p.publish(msg);
                        break;
                    }
                    case 1: {
                        // publish2
                        TopicPublisher p = s.createPublisher(null);  
                        p.publish(s.createTopic("Topic2"), msg);
                        break;
                    }
                    case 2: {
                        // publish4
                        TopicPublisher p = s.createPublisher(s.createTopic("Topic2"));  
                        p.publish(msg, p.getDeliveryMode(), p.getPriority(), p.getTimeToLive());
                        break;
                    }
                    case 3: {
                        // publish5
                        TopicPublisher p = s.createPublisher(null);  
                        p.publish(s.createTopic("Topic2"), msg, p.getDeliveryMode(), p.getPriority(), p.getTimeToLive());
                        break;
                    }
                    case 4: {
                        // send5
                        TopicPublisher p = s.createPublisher(null);  
                        p.send(s.createTopic("Topic2"), msg, p.getDeliveryMode(), p.getPriority(), p.getTimeToLive());
                        break;
                    }
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    safeClose(c);
                }
            }
        };
        
        m.doTest();
    }
    
    
    
    
    /**
     * Tests the default interceptor scenario (two interceptors) in a mock container
     * 
     * @throws Throwable
     */
    public void testInterceptorDemo() throws Throwable {
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
                Connection c = null;
                try {
                    c = fact.createConnection(); 
                    Session s = c.createSession(false, Session.AUTO_ACKNOWLEDGE);
                    // Copy the message so that the properties are no longer readonly
                    msg = TestMessageBean.copy(msg, s);
                    s.createProducer(s.createQueue("Queue2")).send(msg);
                    c.close();
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    safeClose(c);
                }
            }
        };

        try {
            MockContainer c = new MockContainer(mdb);
            ra.start(c.getBootstrapContext());
            ra.endpointActivation(c.getMessageEndpointFactory(), spec);
            p.setNMessagesToSend(50);
            p.clearQ1Q2Q3();
            
            Interceptor1.sInvocationCount.clear();
            Interceptor2.sInvocationCount.clear();
            Interceptor3.set(new IDCopier());

            p.setMessageGenerator(new Passthrough.MessageGenerator() {
                @Override
                public String checkMessage(Message m, int i, int batch) throws JMSException {
                    if (m.getStringProperty("OriginalID") == null) {
                        return "No OriginalID";
                    }
                    return null;
                }
            });
            p.passFromQ1ToQ2();
            ra.endpointDeactivation(c.getMessageEndpointFactory(), spec);
            p.get(p.getQueue1Name()).assertEmpty();
        } finally {
            Passthrough.safeClose(p);
            Interceptor3.set(null);

        }
    }
    
//    /**
//     * Queue to queue XA on in, XA on out CC-mode
//     * 
//     * @throws Throwable
//     */
//    public void testInAS() throws Throwable {
//        EmbeddedDescriptor dd = getDD();
//        dd.findElementByText(EJBDD, "testQQXAXA").setText("testInterceptor1");
//        dd.findElementByText(EJBDD, "XContextName").setText("j-testQQXAXA");
//        ConnectorConfig x = (ConnectorConfig) dd.new ResourceAdapter(RAXML).createConnector(ConnectorConfig.class);
//        String url = x.getConnectionURL();
//        url = url + (url.indexOf('?') > 0 ? "&" : "?") + Options.Interceptor.SERVICENAME + "=" + "jmsjca-private.test.interceptor";
//        x.setConnectionURL(url);
//        dd.update();
//
//        // Deploy
//        Container c = createContainer();
//        Passthrough p = createPassthrough(mServerProperties);
//
//        try {
//            if (c.isDeployed(mTestEar.getAbsolutePath())) {
//                c.undeploy(mTestEarName);
//            }
//            p.clearQ1Q2Q3();
//
//            c.redeployModule(mTestEar.getAbsolutePath());
//            p.passFromQ1ToQ2();
//            c.undeploy(mTestEarName);
//            p.get(p.getQueue1Name()).assertEmpty();
//        } finally {
//            Container.safeClose(c);
//            Passthrough.safeClose(p);
//        }
//    }
//    
    /**
     * Tests the basic properties of the nbound context
     * 
     * @author fkieviet
     */
    public static class ContextTestInbound  implements SampleInterceptor.Executor, Serializable {
        public void run() throws Exception {
            InvocationContext ctx = SampleInterceptor.getInboundContext();
            
            if (ctx == null) {
                throw new Exception("No context set on thread local");
            }

                // Check data
            if (ctx.getContextData() == null) {
                throw new Exception("Context data =" + ctx.getContextData());
            }
            if (ctx.getContextData().get(Options.Interceptor.KEY_MESSAGE) == null) {
                throw new Exception("Context data message missing ");
            }
            if (!(ctx.getContextData().get(Options.Interceptor.KEY_MESSAGE) instanceof javax.jms.Message)) {
                throw new Exception("Context data message invalid type");
            }

            // Check parameters
            if (ctx.getParameters().length != 1) {
                throw new Exception("Parameters=" + ctx.getParameters().length);
            }
            if (ctx.getParameters()[0] == null || !(ctx.getParameters()[0] instanceof javax.jms.Message)) {
                throw new Exception("Parameters[0]=" + ctx.getParameters()[0]);
            }
        }
    }
    
    /**
     * Tests in the application server the proper invocation of the inbound interceptor
     *  
     * @throws Throwable
     */
    public void testContextInbound() throws Throwable {
        runInboundInterceptor(ContextTestInbound.class, SampleInterceptor.TEST_IN_EXEC);
    }
    

    
    /**
     * Tests the basic properties of the nbound context
     * 
     * @author fkieviet
     */
    public static class ThrowExceptionTest  implements SampleInterceptor.Executor, Serializable {
        private static Random sRandom = new Random();

        private static synchronized boolean sample(int outOfHundred) {
            return sRandom.nextInt(100) < outOfHundred;
        }

        public void run() throws Exception {
            if (sample(20)) {
                if (sample(50)) {
                    throw new RuntimeException("Testing interceptors with RuntimeExceptions");
                } else {
                    throw new Exception("Testing interceptors checked Exceptions");
                }
            }
        }
    }
    
    /**
     * Throws an exception from the inbound interceptor
     * 
     * @throws Exception
     */
    public void testThrowExceptionInbound() throws Exception {
        runInboundInterceptor(ThrowExceptionTest.class, SampleInterceptor.TEST_IN_EXEC);
    }
    
    /**
     * Throws an exception from the outbound interceptor
     * 
     * @throws Exception
     */
    public void testThrowExceptionOutbound() throws Exception {
        runInboundInterceptor(ThrowExceptionTest.class, SampleInterceptor.TEST_OUT_EXEC);
    }
    
    /**
     * Sends msgs through and executes each message in the inbound interceptor
     * 
     * @param testClass
     * @throws Exception
     */
    public void runInboundInterceptor(final Class<?> testClass, final String propertyName) throws Exception {
        EmbeddedDescriptor dd = getDD();
        dd.findElementByText(EJBDD, "testQQXAXA").setText("testInterceptor1");
        dd.findElementByText(EJBDD, "XContextName").setText("testInterceptor1");

        dd.findElementByText(EJBDD, "cc").setText("serial");

        ConnectorConfig x = (ConnectorConfig) dd.new ResourceAdapter(RAXML).createConnector(ConnectorConfig.class);
        String url = x.getConnectionURL();
        url = url + (url.indexOf('?') > 0 ? "&" : "?") + Options.Interceptor.SERVICENAME + "=" + Options.Interceptor.TEST_SVC;
        x.setConnectionURL(url);
        dd.update();

        // Deploy
        Container c = createContainer();
        Passthrough p = createPassthrough(mServerProperties);

        try {
            if (c.isDeployed(mTestEar.getAbsolutePath())) {
                c.undeploy(mTestEarName);
            }
            p.clearQ1Q2Q3();
            p.setMsgType(ObjectMessage.class);
            p.setMessageGenerator(new Passthrough.MessageGenerator() {
                @Override
                public Message createMessage(Session s, Class<?> type)
                    throws JMSException {
                    ObjectMessage ret = (ObjectMessage) super.createMessage(s, type);
                    try {
                        ret.setObject((Serializable) testClass.newInstance());
                    } catch (Exception e) {
                        throw new JMSException("" + e);
                    }
                    ret.setBooleanProperty(propertyName, true);
                    return ret;
                }
            });

            c.redeployModule(mTestEar.getAbsolutePath());
            p.passFromQ1ToQ2();
            c.undeploy(mTestEarName);
            p.get(p.getQueue1Name()).assertEmpty();
        } finally {
            Container.safeClose(c);
            Passthrough.safeClose(p);
        }
    }

    /**
     * Queue to queue XA on in, XA on out CC-mode
     * 
     * @throws Throwable
     */
    public void skip_testContainerManaged() throws Throwable {
        EmbeddedDescriptor dd = getDD();
        dd.findElementByText(EJBDD, "testQQXAXA").setText("testFailall");
        dd.findElementByText(EJBDD, "XContextName").setText("j-testQQXAXA");
//        dd.findElementByText(EJBDD, "cc").setText("serial");
        
        ConnectorConfig x = (ConnectorConfig) dd.new ResourceAdapter(RAXML).createConnector(ConnectorConfig.class);
        String url = x.getConnectionURL();
//        url = url + (url.indexOf('?') > 0 ? "&" : "?") + Options.Interceptor.SERVICENAME + "=" + "jmsjca-private.test.interceptor";
        x.setConnectionURL(url);
        dd.update();

        // Deploy
        Container c = createContainer();
        Passthrough p = createPassthrough(mServerProperties);

        try {
            if (c.isDeployed(mTestEar.getAbsolutePath())) {
                c.undeploy(mTestEarName);
            }
            p.clearQ1Q2Q3();
            
            p.setNMessagesToSend(1000);

            c.redeployModule(mTestEar.getAbsolutePath());
            p.passFromQ1ToQ2();
            c.undeploy(mTestEarName);
            p.get(p.getQueue1Name()).assertEmpty();
        } finally {
            Container.safeClose(c);
            Passthrough.safeClose(p);
        }
    }
    
    
    // Tests to add:
    // -------------
    // Classloading test when adding the interceptor to the EAR
    // Tests without XA (thread context classloader!)
}
