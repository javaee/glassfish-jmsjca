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
import com.stc.jmsjca.core.TxMgr;
import com.stc.jmsjca.core.XMCFQueueXA;
import com.stc.jmsjca.core.XMCFTopicXA;
import com.stc.jmsjca.core.XMCFUnifiedXA;
import com.stc.jmsjca.core.XManagedConnectionFactory;
import com.stc.jmsjca.test.core.Passthrough.QueueDest;
import com.stc.jmsjca.test.core.Passthrough.QueueSource;
import com.stc.jmsjca.util.InterceptorInfo;
import com.stc.jmsjca.util.InterceptorLoader;
import com.stc.jmsjca.util.Logger;
import com.stc.jmsjca.util.SampleInterceptor;

import javax.ejb.EJBException;
import javax.interceptor.AroundInvoke;
import javax.interceptor.InvocationContext;
import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.MessageProducer;
import javax.jms.ObjectMessage;
import javax.jms.Queue;
import javax.jms.QueueConnection;
import javax.jms.QueueConnectionFactory;
import javax.jms.QueueSender;
import javax.jms.QueueSession;
import javax.jms.Session;
import javax.jms.TopicConnection;
import javax.jms.TopicConnectionFactory;
import javax.jms.TopicPublisher;
import javax.jms.TopicSession;
import javax.management.MBeanServer;
import javax.management.ObjectName;
import javax.naming.Context;
import javax.naming.InitialContext;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Tests interceptors
 * 
 * @author fkieviet
 */
public abstract class InterceptorTests extends EndToEndBase {
//    private static Logger sLog = Logger.getLogger(InterceptorTests.class.getName());
    
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
        HashMap<Class<?>, InterceptorInfo> interceptors = InterceptorLoader.getInterceptors(Options.Interceptor.DEFAULT_SERVICENAME);
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
            p = getJMSProvider().createLocalPassthrough(mServerProperties);

            // RA
            ra = getJMSProvider().createRA();
            ra.setConnectionURL(getJMSProvider().getConnectionUrl(InterceptorTests.this));
            ra.setUserName(getJMSProvider().getUserName(mServerProperties));
            ra.setPassword(getJMSProvider().getPassword(mServerProperties));

            // Spec
            spec = ra.createObjectFactory(ra, null, null).createActivationSpec();
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
        Passthrough p = getJMSProvider().createLocalPassthrough(mServerProperties);

        // RA
        RAJMSResourceAdapter ra = getJMSProvider().createRA();
        ra.setConnectionURL(getJMSProvider().getConnectionUrl(this));
        ra.setUserName(getJMSProvider().getUserName(mServerProperties));
        ra.setPassword(getJMSProvider().getPassword(mServerProperties));

        // Spec
        RAJMSActivationSpec spec = ra.createObjectFactory(ra, null, null).createActivationSpec();
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
    
    /**
     * Converts a textual table to a list of maps. The rows are \r\n separated. The 
     * first row must be the header row. The maps returned use the header cell names
     * as the key, and the value as the value.
     * 
     * @param inp
     * @return
     */
    List<Map<String, Integer>> convertTable(String inp) {
        String[] lines = inp.split("\r\n");
        List<Map<String, Integer>> ret = new ArrayList<Map<String, Integer>>();
        
        String header = lines[0];
        String[] columnNames = header.split("\\s+");
        for (int i = 1; i < lines.length; i++) {
            String[] columns = lines[i].split("\\s+");
            if (columns.length > 0) {
                Map<String, Integer> values = new HashMap<String, Integer>();
                for (int j = 0; j < columnNames.length; j++) {
                    values.put(columnNames[j], Integer.parseInt(columns[j]));
                }
                ret.add(values);
            }
        }
        return ret;
    }
    
    /**
     * Tests delivery stats using an elaborate scenario of messages being rolled back
     * or moved or processed.
     * 
     * @throws Throwable
     */
    public void testDeliveryStats3() throws Throwable {
        Passthrough p = getJMSProvider().createLocalPassthrough(mServerProperties);
        
        String scenario = "invocation   message rc  DC  BC  DCSLBC  BCSLDC  redeliverystate action\r\n" + 
        		"0   0   0   0   0   0   0   0   0\r\n" + 
        		"1   1   0   1   0   1   0   0   1\r\n" + 
        		"2   1   1   1   0   1   0   1   0\r\n" + 
        		"3   2   0   2   0   2   0   0   0\r\n" + 
        		"4   3   0   3   0   3   0   0   0\r\n" + 
        		"5   4   0   4   0   4   0   0   2\r\n" + 
        		"-1  4   1   4   0   4   0   0   -1\r\n" + 
        		"6   5   0   4   1   0   1   0   2\r\n" + 
        		"-1  5   1   4   1   0   1   0   -1\r\n" + 
        		"7   6   0   4   2   0   2   0   2\r\n" + 
        		"-1  6   1   4   2   0   2   0   -1\r\n" + 
        		"8   7   0   4   3   0   3   0   0\r\n" + 
        		"9   8   0   5   3   1   0   0   0\r\n" + 
        		"10  9   0   6   3   2   0   0   2\r\n" + 
        		"-1  9   1   6   3   2   0   0   -1\r\n" + 
        		"11  10  0   6   4   0   1   0   2\r\n" + 
        		"-1  10  1   6   4   0   1   0   -1\r\n" + 
        		"12  11  0   6   5   0   2   0   2\r\n" + 
        		"-1  11  1   6   5   0   2   0   -1\r\n" + 
        		"13  12  0   6   6   0   3   0   2\r\n" + 
        		"-1  12  1   6   6   0   4   0   -1\r\n" + 
        		"14  13  0   6   7   0   4   0   1\r\n" + 
        		"15  13  1   6   7   0   4   1   1\r\n" + 
        		"16  13  2   6   7   0   4   1   4\r\n" + 
        		"17  13  1   0   0   0   0   0   0\r\n" + 
        		"18  14  0   1   0   0   0   0   0\r\n" + 
        		"";
        
        final List<Map<String, Integer>> steps = convertTable(scenario);

        // RA
        RAJMSResourceAdapter ra = getJMSProvider().createRA();
        ra.setConnectionURL(getJMSProvider().getConnectionUrl(this));
        ra.setUserName(getJMSProvider().getUserName(mServerProperties));
        ra.setPassword(getJMSProvider().getPassword(mServerProperties));

        // Spec
        RAJMSActivationSpec spec = ra.createObjectFactory(ra, null, null).createActivationSpec();
        spec.setUserName(p.getUserid());
        spec.setPassword(p.getPassword());
        spec.setConcurrencyMode("sync");
        spec.setDestinationType(javax.jms.Queue.class.getName());
        spec.setDestination(p.getQueue1Name());
        spec.setEndpointPoolMaxSize("1");
        spec.setMBeanName("JMSJCA-TEST:aa=bbb");

        // MCF
        XManagedConnectionFactory mcf = new XMCFQueueXA();
        mcf.setResourceAdapter(ra);
        final QueueConnectionFactory fact = (QueueConnectionFactory) mcf.createConnectionFactory(null);
        
        // An MDB
        final AtomicInteger idx = new AtomicInteger();
        final Iterator<Map<String, Integer>> stepper = steps.iterator();
        final StringBuilder results = new StringBuilder();
        final MessageListener mdb = new MessageListener() {
            @SuppressWarnings("unchecked")
            public void onMessage(final Message msg) {
                Connection c = null;
                try {
                    
                    int i = idx.getAndIncrement();
                    
                    Map<String, Long> stats = (Map<String, Long>) msg.getObjectProperty(Options.MessageProperties.DELIVERYSTATS);
                    Map<String, Integer> step = steps.get(steps.size() - 1);
                    while (stepper.hasNext()) {
                        step = stepper.next();
                        if (step.get("invocation") >= 0) {
                            break;
                        }
                    }
                    
                    String st = msg.getStringProperty(Options.MessageProperties.USER_ROLLBACK_DATA_PREFIX + "_xx");
                    
                    String thisResult = "inv=" + i + "/" + step.get("invocation") 
                        + " msg=" + msg.getIntProperty("idx") + "/" + step.get("message") 
                        + " rc=" + msg.getObjectProperty(Options.MessageProperties.REDELIVERYCOUNT) + "/" + step.get("rc")
                        + " DC=" + stats.get("DC") + "/" + step.get("DC")
                        + " BC=" + stats.get("BC") + "/" + step.get("BC")
                        + " DCSLBC=" + stats.get("DCSLBC") + "/" + step.get("DCSLBC")
                        + " BCSLDC=" + stats.get("BCSLDC") + "/" + step.get("BCSLDC")
                        + " redeliverystate = " + (st == null ? "0" : st)
                        + "/" + step.get("redeliverystate")
                        //+ " stats=" + InterceptorTests.this.toString(stats)
                        + "\r\n";
                    System.out.print(thisResult);
                    results.append(thisResult);

                    if (step.get("action") == 1) {
                        // rollback
                        TxMgr.getUnitTestTxMgr().setRollbackOnly();
                        msg.setStringProperty(Options.MessageProperties.USER_ROLLBACK_DATA_PREFIX + "_xx", "1");
                        System.out.println("rollback msg " + msg.getIntProperty("idx"));
                    } else if (step.get("action") == 2) {
                        // rollback and setup for move on next redelivery
                        TxMgr.getUnitTestTxMgr().setRollbackOnly();
                        msg.setStringProperty(Options.MessageProperties.REDELIVERY_HANDLING, "1:move(queue:Queue3)");
                        msg.setStringProperty(Options.MessageProperties.USER_ROLLBACK_DATA_PREFIX + "_xx", "1");
                        System.out.println("rollback/move msg " + msg.getIntProperty("idx"));
                    } else if (step.get("action") == 4) {
                        // rollback and setup for move on next redelivery
                        TxMgr.getUnitTestTxMgr().setRollbackOnly();
                        msg.setStringProperty(Options.MessageProperties.STOP_CONNECTOR, "Test shutdown");
                        System.out.println("rollback/shutdown msg " + msg.getIntProperty("idx"));
                        new Thread("restart") {
                            @Override
                            public void run() {
                                for (int i = 0; i < 10; i++) {
                                    try {
                                        if (i != 0) {
                                            Thread.sleep(1000);
                                        }
                                        MBeanServer mbs = (MBeanServer) msg.getObjectProperty(Options.MessageProperties.MBEANSERVER);
                                        String name = msg.getStringProperty(Options.MessageProperties.MBEANNAME);
                                        ObjectName mbeanName = new ObjectName(name);
                                        mbs.invoke(mbeanName, "start", null, null);
                                        System.out.println("Restarted");
                                        return;
                                    } catch (Exception e) {
                                        System.out.println("Failed to start: " + e.getMessage());
                                    }
                                }
                                System.out.println("*** Failed to restart");
                            }
                        }.start();
                    } else {
                        System.out.println("commit msg " + msg.getIntProperty("idx"));
                    }
                    
                    c = fact.createConnection(); 
                    Session s = c.createSession(false, Session.AUTO_ACKNOWLEDGE);
                    // Copy the message so that the properties are no longer readonly
                    Message outmsg = TestMessageBean.copy(msg, s);
                    s.createProducer(s.createQueue("Queue2")).send(outmsg);
                    c.close();
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    safeClose(c);
                }
            }
        };

        try {
            p.setNMessagesToSend(50);
            p.clearQ1Q2Q3();
            
            MockContainer c = new MockContainer(mdb);
            ra.start(c.getBootstrapContext());
            ra.endpointActivation(c.getMessageEndpointFactory(), spec);

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

                @Override
                public void checkResults(long t0, List<String> failures, int nReceived,
                    int nExpected, int[] readbackCount, int[] readbackOrder,
                    int multiplier, boolean strictOrder) throws Exception {
                }
            });
            p.setNMessagesToSend(15);

            p.close();

            QueueSource source = p.new QueueSource(p.getQueue1Name());
            QueueDest dest = p.new QueueDest(p.getQueue2Name());
            QueueDest dlq = p.new QueueDest(p.getQueue3Name());
            source.connect();
            dest.connect();
            dest.drain();
            dlq.connect();
            dlq.drain();
            source.sendBatch(15, 0, "");
            dest.readback(8, 0);
            dlq.readback(7, 0);
            
            ra.endpointDeactivation(c.getMessageEndpointFactory(), spec);
            p.get(p.getQueue1Name()).assertEmpty();
            
            assertEquals(results.toString(), "inv=0/0 msg=0/0 rc=0/0 DC=0/0 BC=0/0 DCSLBC=0/0 BCSLDC=0/0 redeliverystate = 0/0\r\n" + 
            		"inv=1/1 msg=1/1 rc=0/0 DC=1/1 BC=0/0 DCSLBC=1/1 BCSLDC=0/0 redeliverystate = 0/0\r\n" + 
            		"inv=2/2 msg=1/1 rc=1/1 DC=1/1 BC=0/0 DCSLBC=1/1 BCSLDC=0/0 redeliverystate = 1/1\r\n" + 
            		"inv=3/3 msg=2/2 rc=0/0 DC=2/2 BC=0/0 DCSLBC=2/2 BCSLDC=0/0 redeliverystate = 0/0\r\n" + 
            		"inv=4/4 msg=3/3 rc=0/0 DC=3/3 BC=0/0 DCSLBC=3/3 BCSLDC=0/0 redeliverystate = 0/0\r\n" + 
            		"inv=5/5 msg=4/4 rc=0/0 DC=4/4 BC=0/0 DCSLBC=4/4 BCSLDC=0/0 redeliverystate = 0/0\r\n" + 
            		"inv=6/6 msg=5/5 rc=0/0 DC=4/4 BC=1/1 DCSLBC=0/0 BCSLDC=1/1 redeliverystate = 0/0\r\n" + 
            		"inv=7/7 msg=6/6 rc=0/0 DC=4/4 BC=2/2 DCSLBC=0/0 BCSLDC=2/2 redeliverystate = 0/0\r\n" + 
            		"inv=8/8 msg=7/7 rc=0/0 DC=4/4 BC=3/3 DCSLBC=0/0 BCSLDC=3/3 redeliverystate = 0/0\r\n" + 
            		"inv=9/9 msg=8/8 rc=0/0 DC=5/5 BC=3/3 DCSLBC=1/1 BCSLDC=0/0 redeliverystate = 0/0\r\n" + 
            		"inv=10/10 msg=9/9 rc=0/0 DC=6/6 BC=3/3 DCSLBC=2/2 BCSLDC=0/0 redeliverystate = 0/0\r\n" + 
            		"inv=11/11 msg=10/10 rc=0/0 DC=6/6 BC=4/4 DCSLBC=0/0 BCSLDC=1/1 redeliverystate = 0/0\r\n" + 
            		"inv=12/12 msg=11/11 rc=0/0 DC=6/6 BC=5/5 DCSLBC=0/0 BCSLDC=2/2 redeliverystate = 0/0\r\n" + 
            		"inv=13/13 msg=12/12 rc=0/0 DC=6/6 BC=6/6 DCSLBC=0/0 BCSLDC=3/3 redeliverystate = 0/0\r\n" + 
            		"inv=14/14 msg=13/13 rc=0/0 DC=6/6 BC=7/7 DCSLBC=0/0 BCSLDC=4/4 redeliverystate = 0/0\r\n" + 
            		"inv=15/15 msg=13/13 rc=1/1 DC=6/6 BC=7/7 DCSLBC=0/0 BCSLDC=4/4 redeliverystate = 1/1\r\n" + 
            		"inv=16/16 msg=13/13 rc=2/2 DC=6/6 BC=7/7 DCSLBC=0/0 BCSLDC=4/4 redeliverystate = 1/1\r\n" + 
            		"inv=17/17 msg=13/13 rc=1/1 DC=0/0 BC=0/0 DCSLBC=0/0 BCSLDC=0/0 redeliverystate = 0/0\r\n" + 
            		"inv=18/18 msg=14/14 rc=0/0 DC=1/1 BC=0/0 DCSLBC=1/0 BCSLDC=0/0 redeliverystate = 0/0\r\n" + 
            		"");
            
        } finally {
            Passthrough.safeClose(p);
            Interceptor3.set(null);

        }
    }
    
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
        runInterceptor(ContextTestInbound.class, SampleInterceptor.TEST_IN_EXEC, "testInterceptor1");
    }
    
    /**
     * Tests the basic properties of the nbound context
     * 
     * @author fkieviet
     */
    public static class DeliveryStatsTestInbound  implements SampleInterceptor.Executor, Serializable {
        @SuppressWarnings("unchecked")
        public void run() throws Exception {
            InvocationContext ctx = SampleInterceptor.getInboundContext();
            
            if (ctx == null) {
                throw new Exception("No context set on thread local");
            }
            Message m = (Message) ctx.getContextData().get(Options.Interceptor.KEY_MESSAGE);
            
            Map<String, Long> stats = (Map<String, Long>) m.getObjectProperty(Options.MessageProperties.DELIVERYSTATS);
            
            StringBuilder buf = new StringBuilder();
            for (String s: stats.keySet()) {
                buf.append(s + "=" + stats.get(s) + "; ");
            }
//            Logger.getLogger(this.getClass()).infoNoloc("stats: " + buf);
            
            if (stats.get(Options.Stats.BYPASS_COMMITS) == null) {
                throw new Exception("No BC found");
            }
            if (!stats.get(Options.Stats.BYPASS_COMMITS).toString().equals("0")) {
                throw new Exception("BC != null: " + stats.get(Options.Stats.BYPASS_COMMITS));
            }
            
            Destination dest = (Destination) m.getObjectProperty(Options.MessageProperties.INBOUND_DESTINATION);
            if (dest == null) {
                throw new Exception("INBOUND DEST IS NULL");
            }
        }
    }
    
    /**
     * Tests delivery stats using a simple scenario
     *  
     * @throws Throwable
     */
    public void testDeliveryStats() throws Throwable {
        runInterceptor(DeliveryStatsTestInbound.class, SampleInterceptor.TEST_IN_EXEC, "testInterceptor1");
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
        runInterceptor(ThrowExceptionTest.class, SampleInterceptor.TEST_IN_EXEC, "testInterceptor1");
    }
    
    /**
     * Throws an exception from the outbound interceptor
     * 
     * @throws Exception
     */
    public void testThrowExceptionOutbound() throws Exception {
        runInterceptor(ThrowExceptionTest.class, SampleInterceptor.TEST_OUT_EXEC, "testInterceptor1");
    }
    
    /**
     * Sends msgs through and executes each message in the inbound interceptor
     * 
     * @param testClass
     * @throws Exception
     */
    public void runInterceptor(final Class<?> testClass, final String inboundOrOutbound, String testName) throws Exception {
        EmbeddedDescriptor dd = getDD();
        dd.findElementByText(EJBDD, "testQQXAXA").setText(testName);
        dd.findElementByText(EJBDD, "XContextName").setText(testName);

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
                    ret.setBooleanProperty(inboundOrOutbound, true);
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
    
    public static class SendUsingGlobalPool implements SampleInterceptor.Executor, Serializable {
        private static Logger sLog = Logger.getLogger(SendUsingGlobalPool.class.getName());

        public static void safeClose(Connection c) {
            if (c != null) {
                try {
                    c.close();
                } catch (Exception ex) {
                    // ignore
                }
            }
        }
        
        public static void safeClose(Context c) {
            if (c != null) {
                try {
                    c.close();
                } catch (Exception ex) {
                    // ignore
                }
            }
        }
        
        public void run() throws Exception {
            try {
                Connection conn = null;
                Context ctx = new InitialContext();
                try {
                    Message message = (Message) SampleInterceptor.getInboundContext().getParameters()[0];
                    
                    ConnectionFactory fact = (ConnectionFactory) ctx.lookup("jms/tx/default");
                    conn = fact.createConnection();
                    Session s = conn.createSession(true, Session.AUTO_ACKNOWLEDGE);
                    Queue dest = s.createQueue("Queue2");
                    MessageProducer prod = s.createProducer(dest);
                    prod.send(message);
                } finally {
                    safeClose(ctx);
                    safeClose(conn);
                }
            } catch (JMSException e) {
                sLog.errorNoloc("Failed: " + e, e);
                throw new EJBException("Failed: " + e, e);
            }
        }
    }

    /**
     * Demoes sending a message from an inbound interceptor using a global connection pool
     * 
     * POOL HAS TO BE CONFIGURED IN THE APPLICATION SERVER BEFORE TEST CAN BE RUN!
     * 
     * @throws Throwable
     */
    public void skip_testGlobalInterceptorUsingGlobalPool() throws Throwable {
        runInterceptor(SendUsingGlobalPool.class, SampleInterceptor.TEST_IN_EXEC, "testDoNotSend");
    }


    // Tests to add:
    // -------------
    // Classloading test when adding the interceptor to the EAR
    // Tests without XA (thread context classloader!)
}
