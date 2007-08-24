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
import com.stc.jmsjca.test.core.Passthrough.TopicDest;

import javax.jms.Session;
import javax.jms.Topic;
import javax.jms.TopicConnection;
import javax.jms.TopicConnectionFactory;
import javax.jms.TopicSession;

/**
 * Required:
 * test.server.properties = path to properties file containing server config
 * test.ear.path          = path to ear file to be tested
 *
 * Example for Eclipse:
 *     -Dtest.server.properties=../../R1/logicalhost/testsettings.properties -Dtest.ear.path=rastcms/test/rastcms-test.ear
 * with working directory
 *     ${workspace_loc:e-jmsjca/build}
 *
 * @author fkieviet
 * @version $Revision: 1.6 $
 */
abstract public class TopicEndToEnd extends EndToEndBase {
    /**
     * Provides a hook to plug in provider specific client IDs
     * 
     * @return clientId
     */
    public abstract String getClientId(String proposedClientId);

    public void waitUntilRunning(Container c) throws Exception {
        ActivationMBean mbean = (ActivationMBean) c.getMBeanProxy(MBEAN,
            ActivationMBean.class);
        for (;;) {
            String str = mbean.xgetStatus();
            System.out.println("Waiting until application is deployed and running; connected status: " + str);
            if (str.equals(com.stc.jmsjca.core.ActivationMBean.CONNECTED)) {
                break;
            }
            Thread.sleep(1000);
        }
    }
    
    /**
     * Topic to queue
     * XA on in, XA on out
     * serial-mode
     * Non-durable
     *
     * @throws Throwable
     */
    public void testNonDurableTopicToQueueSerial() throws Throwable {
        EmbeddedDescriptor dd = getDD();

        StcmsActivation spec = (StcmsActivation) dd.new ActivationConfig(EJBDD,
                "mdbtest").createActivation(StcmsActivation.class);
        spec.setContextName("j-testTTXAXA");
        spec.setConcurrencyMode("serial");
        spec.setDestination("Topic1");
        spec.setDestinationType(javax.jms.Topic.class.getName());
        dd.update();

        // Deploy
        Container c = createContainer();
        Passthrough p = createPassthrough(mServerProperties);
        p.drainQ2();
        
        try {
            if (c.isDeployed(mTestEar.getAbsolutePath())) {
                c.undeploy(mTestEarName);
            }

            int iters = isFastTest() ? 1 : 2;
            for (int i = 0; i < iters; i++) {
                p.setBatchId(80 + i);
                c.deployModule(mTestEar.getAbsolutePath());
                waitUntilRunning(c);
                p.passFromT1ToQ2();
                c.undeploy(mTestEarName);
                p.assertQ2Empty();
            }
        } finally {
            Container.safeClose(c);
            Passthrough.safeClose(p);
        }
    }

    /**
     * Topic to queue
     * XA on in, XA on out
     * cc
     * Non-durable
     *
     * @throws Throwable
     */
    public void testNonDurableTopicToQueueCC() throws Throwable {
        EmbeddedDescriptor dd = getDD();
        StcmsActivation spec = (StcmsActivation) dd.new ActivationConfig(
                EJBDD, "mdbtest").createActivation(StcmsActivation.class);
        spec.setContextName("j-testTTXAXA");
        spec.setDestination("Topic1");
        spec.setDestinationType(javax.jms.Topic.class.getName());
        dd.update();

        // Deploy
        Container c = createContainer();
        Passthrough p = createPassthrough(mServerProperties);
        p.drainQ2();
        
        try {
            if (c.isDeployed(mTestEar.getAbsolutePath())) {
                c.undeploy(mTestEarName);
            }

            int iters = isFastTest() ? 1 : 2;
            for (int i = 0; i < iters; i++) {
                p.setBatchId(90 + i);
                c.deployModule(mTestEar.getAbsolutePath());
                waitUntilRunning(c);
                p.passFromT1ToQ2();
                c.undeploy(mTestEarName);
                p.assertQ2Empty();
            }
        } finally {
            Container.safeClose(c);
            Passthrough.safeClose(p);
        }
    }
    
    /**
     * Topic to queue
     * XA on in, XA on out
     * serial-mode
     * Durable
     *
     * @throws Throwable
     */
    public void testDurableTopicToQueueSerial() throws Throwable {
        Passthrough p = createPassthrough(mServerProperties);
               
        EmbeddedDescriptor dd = getDD();
        StcmsActivation spec = (StcmsActivation) dd.new ActivationConfig(EJBDD, "mdbtest").createActivation(StcmsActivation.class);
        spec.setContextName("j-testTTXAXA");
        spec.setDestination(p.getTopic1Name());
        spec.setDestinationType(javax.jms.Topic.class.getName());
        spec.setConcurrencyMode("serial");
        spec.setSubscriptionDurability("Durable");
        String subscriptionName = p.getDurableTopic1Name();
        spec.setSubscriptionName(subscriptionName);
        String clientID = getClientId(p.getDurableTopic1Name() + "clientID");
        spec.setClientId(clientID);
        dd.update();

        // Deploy
        Container c = createContainer();
        
        try {
            if (c.isDeployed(mTestEar.getAbsolutePath())) {
                c.undeploy(mTestEarName);
            }
     
            p.removeDurableSubscriber(clientID, p.getTopic1Name(), subscriptionName);
            p.drainQ2();
            
            int iters = isFastTest() ? 1 : 2;
            for (int i = 0; i < iters; i++) {
                p.setBatchId(100 + i);
                
                // deploy bean to create a durable subscription then undeploy it
                c.deployModule(mTestEar.getAbsolutePath());
                waitUntilRunning(c);
                c.undeploy(mTestEarName);
                
                // send messages to T1 - these should be stored in the durable subscription
                p.sendToT1();
                
                // now redeploy the bean 
                // this should then receive the messages from the durable subscription
                // and sends them on to Q2
                c.deployModule(mTestEar.getAbsolutePath());
                waitUntilRunning(c);
                p.readFromQ2(); 
                
                c.undeploy(mTestEarName);
                //p.removeDurableSubscriber(clientID, Passthrough.T1,subscriptionName);
            }
            
        } finally {
            Container.safeClose(c);
            Passthrough.safeClose(p);
        }
    }
    
    /**
     * Topic to queue
     * XA on in, XA on out
     * serial-mode
     * Durable
     *
     * @throws Throwable
     */
    public void testDurableTopicToQueueCC() throws Throwable {
        Passthrough p = createPassthrough(mServerProperties);
        EmbeddedDescriptor dd = getDD();
        StcmsActivation spec = (StcmsActivation) dd.new ActivationConfig(
            EJBDD, "mdbtest").createActivation(StcmsActivation.class);
        spec.setContextName("j-testTTXAXA");
        spec.setDestination(p.getTopic1Name());
        spec.setDestinationType(javax.jms.Topic.class.getName());
        
        spec.setSubscriptionDurability("Durable");
        String subscriptionName = p.getDurableTopic1Name();
        spec.setSubscriptionName(p.getDurableTopic1Name());
        String clientId = getClientId(p.getDurableTopic1Name() + "clientID");
        spec.setClientId(clientId);
        dd.update();
 
        // Deploy
        Container c = createContainer();
 
        try {
            int iters = isFastTest() ? 1 : 2;
            for (int i = 0; i < iters; i++) {
                if (c.isDeployed(mTestEar.getAbsolutePath())) {
                    c.undeploy(mTestEarName);
                }
                p.removeDurableSubscriber(clientId, p.getTopic1Name(), subscriptionName);
                p.drainQ2();
                p.setBatchId(110 + i);
                // deploy bean to create a durable subscription then undeploy it
                c.deployModule(mTestEar.getAbsolutePath());
                waitUntilRunning(c);
                c.undeploy(mTestEarName);
                assertTrue(p.drainQ2() == 0);
                
                // send messages to T1 - these should be stored in the durable subscription
                p.sendToT1();
                p.get(p.getTopic1Name()).close();
                
                // now redeploy the bean 
                // this should then receive the messages from the durable subscription
                // and sends them on to Q2
                c.deployModule(mTestEar.getAbsolutePath());
                waitUntilRunning(c);
                p.readFromQ2(); 
                
                c.undeploy(mTestEarName);
                //p.removeDurableSubscriber(p.getTopic1Name(),subscriptionName);
            }
            
        } finally {
            Container.safeClose(c);
            Passthrough.safeClose(p);
        }
    }
    
    /**
     * Topic to topic
     * XA on in, XA on out
     * serial-mode
     * Durable
     *
     * @throws Throwable
     */
    public void testDurableTopicToTopicSerial() throws Throwable {
        EmbeddedDescriptor dd = getDD();
        Passthrough p = createPassthrough(mServerProperties);
        StcmsActivation spec = (StcmsActivation) dd.new ActivationConfig(EJBDD, "mdbtest").createActivation(StcmsActivation.class);
        spec.setContextName("j-testTTXAXA");
        spec.setDestination(p.getTopic1Name());
        spec.setDestinationType(javax.jms.Topic.class.getName());
        spec.setConcurrencyMode("serial");
        spec.setSubscriptionDurability("Durable");
        String subscriptionName = p.getDurableTopic1Name();
        spec.setSubscriptionName(subscriptionName);
        String clientId = getClientId(p.getDurableTopic1Name() + "clientID");
        spec.setClientId(clientId);
        dd.findElementByText(EJBDD, "testQQXAXA").setText("testTTXAXA");
        dd.update();

        // Deploy
        Container c = createContainer();
        try {
            
            int iters = isFastTest() ? 1 : 2;
            for (int i = 0; i < iters; i++) {
                p.setBatchId(120 + i);
                
                if (c.isDeployed(mTestEar.getAbsolutePath())) {
                    c.undeploy(mTestEarName);
                }
                
                p.removeDurableSubscriber(clientId, p.getTopic1Name(), subscriptionName);
                
                // deploy bean to create a durable subscription then undeploy it
                c.deployModule(mTestEar.getAbsolutePath());
                waitUntilRunning(c);
                c.undeploy(mTestEarName);
                
                // send messages to T1 - these should be stored in the durable subscription
                p.sendToT1();
                
                // now redeploy the bean 
                // this should then receive the messages from the durable subscription
                // and sends them on to T2
                TopicDest dest = p.subscribeToT2();
                c.deployModule(mTestEar.getAbsolutePath());
                waitUntilRunning(c);
                p.readFromTopic(dest);
                p.assertT1Empty();
                
                c.undeploy(mTestEarName);
                //p.removeDurableSubscriber(p.getTopic1Name(),subscriptionName);
            }
        } finally {
            Container.safeClose(c);
            Passthrough.safeClose(p);
        }
    }
    
    /**
     * Topic to topic
     * XA on in, XA on out
     * serial-mode
     * Durable
     *
     * @throws Throwable
     */
    public void testDurableTopicToTopicCC() throws Throwable {
        EmbeddedDescriptor dd = getDD();
        Passthrough p = createPassthrough(mServerProperties);
        StcmsActivation spec = (StcmsActivation) dd.new ActivationConfig(EJBDD, "mdbtest").createActivation(StcmsActivation.class);
        spec.setContextName("j-testTTXAXA");
        spec.setDestination(p.getTopic1Name());
        spec.setDestinationType(javax.jms.Topic.class.getName());
        spec.setConcurrencyMode("cc");
        spec.setSubscriptionDurability("Durable");
        String subscriptionName = p.getDurableTopic1Name();
        spec.setSubscriptionName(p.getDurableTopic1Name());
        String clientId = getClientId(p.getDurableTopic1Name() + "clientID");
        spec.setClientId(clientId);
        dd.findElementByText(EJBDD, "testQQXAXA").setText("testTTXAXA");
        dd.update();

        // Deploy
        Container c = createContainer();
        try {
            int iters = isFastTest() ? 1 : 2;
            for (int i = 0; i < iters; i++) {
                p.setBatchId(130 + i);
                p.removeDurableSubscriber(clientId, p.getTopic1Name(), subscriptionName);
                if (c.isDeployed(mTestEar.getAbsolutePath())) {
                    c.undeploy(mTestEarName);
                }

                // deploy bean to create a durable subscription then undeploy it
                c.deployModule(mTestEar.getAbsolutePath());
                waitUntilRunning(c);
                c.undeploy(mTestEarName);
                
                // send messages to T1 - these should be stored in the durable subscription
                p.sendToT1();
                
                // now redeploy the bean 
                // this should then receive the messages from the durable subscription
                // and sends them on to T2
                TopicDest dest = p.subscribeToT2();
                c.deployModule(mTestEar.getAbsolutePath());
                waitUntilRunning(c);
                p.readFromTopic(dest);
                p.assertT1Empty();
                
                c.undeploy(mTestEarName);
                //p.removeDurableSubscriber(p.getTopic1Name(),subscriptionName);
            }
            
        } finally {
            Container.safeClose(c);
            Passthrough.safeClose(p);
        }
    }
    
    /**
     * Topic to queue
     * XA on in, XA on out
     * serial-mode
     * Non-durable
     *
     * @throws Throwable
     */
    public void xtestTopicNonDurableSerial() throws Throwable {
        EmbeddedDescriptor dd = getDD();
        StcmsActivation spec = (StcmsActivation) dd.new ActivationConfig(EJBDD,
                "mdbtest").createActivation(StcmsActivation.class);
        spec.setContextName("j-testTTXAXA");
        spec.setConcurrencyMode("serial");
        spec.setDestination("Topic1");
        spec.setDestinationType(javax.jms.Topic.class.getName());
        dd.update();

        // Deploy
        Container c = createContainer();
        Passthrough p = createPassthrough(mServerProperties);
        try {
            if (c.isDeployed(mTestEar.getAbsolutePath())) {
                c.undeploy(mTestEarName);
            }

            int iters = isFastTest() ? 1 : 2;
            for (int i = 0; i < iters; i++) {
                p.setBatchId(140 + i);
                c.deployModule(mTestEar.getAbsolutePath());
                waitUntilRunning(c);
                p.passFromT1ToQ2();
                c.undeploy(mTestEarName);
                p.get(p.getTopic1Name()).assertEmpty();
            }
        } finally {
            Container.safeClose(c);
            Passthrough.safeClose(p);
        }
    }

    /**
     * Topic to queue
     * XA on in, XA on out
     * cc
     * Non-durable
     *
     * @throws Throwable
     */
    public void xtestTopicNonDurableCC() throws Throwable {
        EmbeddedDescriptor dd = getDD();
        StcmsActivation spec = (StcmsActivation) dd.new ActivationConfig(
                EJBDD, "mdbtest").createActivation(StcmsActivation.class);
        spec.setContextName("j-testTTXAXA");
        spec.setDestination("Topic1");
        spec.setDestinationType(javax.jms.Topic.class.getName());
        dd.update();

        // Deploy
        Container c = createContainer();
        Passthrough p = createPassthrough(mServerProperties);
        try {
            if (c.isDeployed(mTestEar.getAbsolutePath())) {
                c.undeploy(mTestEarName);
            }

            int iters = isFastTest() ? 1 : 2;
            for (int i = 0; i < iters; i++) {
                p.setBatchId(150 + i);
                c.deployModule(mTestEar.getAbsolutePath());
                waitUntilRunning(c);
                p.passFromT1ToQ2();
                c.undeploy(mTestEarName);
                p.get(p.getTopic1Name()).assertEmpty();
            }
        } finally {
            Container.safeClose(c);
            Passthrough.safeClose(p);
        }
    }    

    /**
     * Topic to queue
     * XA on in, XA on out
     * serial-mode
     * Durable
     *
     * @throws Throwable
     */
    public void testDistributedSubscriberToQueueCMT() throws Throwable {
        dotestDistributedSubscriberToQueueSerial(false);
    }
    
    /**
     * Topic to queue
     * XA on in, XA on out
     * serial-mode
     * Durable
     *
     * @throws Throwable
     */
    public void testDistributedSubscriberToQueueBMT() throws Throwable {
        dotestDistributedSubscriberToQueueSerial(true);
    }
    
    /**
     * Topic to queue
     * BMT/CMT
     * serial-mode
     * Durable
     *
     * @throws Throwable
     */
    public void dotestDistributedSubscriberToQueueSerial(boolean bmt) throws Throwable {
        Passthrough p = createPassthrough(mServerProperties);
               
        EmbeddedDescriptor dd = getDD();
        if (bmt) {
            dd.findElementByName(EJBDD, "transaction-type").setText("Bean");
            dd.findElementByName(EJBDD, "trans-attribute").setText("NotSupported");
        }
        StcmsActivation spec = (StcmsActivation) dd.new ActivationConfig(EJBDD, "mdbtest").createActivation(StcmsActivation.class);
        spec.setContextName("j-testTTXAXA");
        spec.setDestination(p.getTopic1Name());
        spec.setDestinationType(javax.jms.Topic.class.getName());
        spec.setConcurrencyMode("cc");
        spec.setSubscriptionDurability("Durable");
        String subscriptionName = p.getDurableTopic1Name();
        spec.setSubscriptionName(Options.Subname.PREFIX + "?"
            + Options.Subname.SUBSCRIBERNAME + "=" + subscriptionName + "&"
            + Options.Subname.DISTRIBUTION_TYPE + "=1");
        String clientID = getClientId(p.getDurableTopic1Name() + "clientID");
        spec.setClientId(clientID);

//        StcmsConnector cc = (StcmsConnector) dd.new ResourceAdapter(RAXML1)
//        .createConnector(StcmsConnector.class);
//        cc.setOptions("JMSJCA.NoXA=true\r\nJMSJCA.IgnoreTx=true");
        
        dd.update();

        // Deploy
        Container c = createContainer();
        
        try {
            if (c.isDeployed(mTestEar.getAbsolutePath())) {
                c.undeploy(mTestEarName);
            }
     
            p.removeDurableSubscriber(clientID, p.getTopic1Name(), subscriptionName);
            p.drainQ2();
            
            int iters = isFastTest() ? 1 : 2;
            for (int i = 0; i < iters; i++) {
                p.setBatchId(100 + i);
                
                // deploy bean to create a durable subscription then undeploy it
                c.deployModule(mTestEar.getAbsolutePath());
                waitUntilRunning(c);
                c.undeploy(mTestEarName);
                
                // send messages to T1 - these should be stored in the durable subscription
                p.sendToT1();
                
                // now redeploy the bean 
                // this should then receive the messages from the durable subscription
                // and sends them on to Q2
                c.deployModule(mTestEar.getAbsolutePath());
                waitUntilRunning(c);
                p.readFromQ2(); 
                
                c.undeploy(mTestEarName);
                //p.removeDurableSubscriber(clientID, Passthrough.T1,subscriptionName);
            }
            
        } finally {
            Container.safeClose(c);
            Passthrough.safeClose(p);
        }
    }

    public void testDistributedSubscriberSecondNode() throws Throwable {
        Passthrough p = createPassthrough(mServerProperties);
               
        EmbeddedDescriptor dd = getDD();
        StcmsActivation spec = (StcmsActivation) dd.new ActivationConfig(EJBDD, "mdbtest").createActivation(StcmsActivation.class);
        spec.setContextName("j-testTTXAXA");
        spec.setDestination(p.getTopic1Name());
        spec.setDestinationType(javax.jms.Topic.class.getName());
        spec.setConcurrencyMode("cc");
        spec.setSubscriptionDurability("Durable");
        String subscriptionName = p.getDurableTopic1Name();
        spec.setSubscriptionName(Options.Subname.PREFIX + "?"
            + Options.Subname.SUBSCRIBERNAME + "=" + subscriptionName + "&"
            + Options.Subname.DISTRIBUTION_TYPE + "=1&"
            + Options.Subname.QUEUENAME + "=" + p.getQueue1Name());
        String clientID = getClientId(p.getDurableTopic1Name() + "clientID");
        spec.setClientId(clientID);
        dd.update();

        // Deploy
        Container c = createContainer();
        
        // Create a durable subscriber
        TopicConnectionFactory f = p.createTopicConnectionFactory();
        TopicConnection conn = f.createTopicConnection(p.getUserid(), p.getPassword());
        if (clientID != null && clientID.length() != 0) {
            conn.setClientID(clientID);
        }
        TopicSession sess = conn.createTopicSession(true, Session.SESSION_TRANSACTED);
        Topic t = sess.createTopic(p.getTopic1Name());
        sess.createDurableSubscriber(t, p.getDurableTopic1Name());

        try {
            if (c.isDeployed(mTestEar.getAbsolutePath())) {
                c.undeploy(mTestEarName);
            }
     
            p.drainQ2();
          
            
            int iters = isFastTest() ? 1 : 2;
            for (int i = 0; i < iters; i++) {
                p.setBatchId(100 + i);
                
                c.redeployModule(mTestEar.getAbsolutePath());
                waitUntilRunning(c);
                p.passFromQ1ToQ2(); 
                
                c.undeploy(mTestEarName);
                //p.removeDurableSubscriber(clientID, Passthrough.T1,subscriptionName);
            }
            
        } finally {
            Container.safeClose(c);
            Passthrough.safeClose(p);
            conn.close();
        }
    }
}
