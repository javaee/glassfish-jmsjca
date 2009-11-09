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
import com.stc.jmsjca.util.UrlParser;

import javax.jms.Session;
import javax.jms.Topic;
import javax.jms.TopicConnection;
import javax.jms.TopicConnectionFactory;
import javax.jms.TopicSession;

/**
 * Tests reconnection scenarios. Works only with a server that has
 * a single tcp/ip connection that can be led through a proxy. 
 * 
 * @author fkieviet
 * @version $Revision: 1.11 $
 */
public abstract class ReconnectionTestsInbound extends EndToEndBase {
    // To test:
    // Connection failure in running application (CC, serial, sync)
    // Connection failure upon deployment (CC, serial, sync)
    // Undeployment of application that is reconnecting
    
    /**
     * Whether to use a TCP proxy to monitor reconnections.
     * Subclasses should override this when a proxy would not be possible
     * 
     * @return   
     */
    protected boolean useProxy(){
        return true;
    }
    
    
    /**
     * Template class to run disconnection tests
     * 
     * @author fkieviet
     */
    public abstract class XTestInbound {
        private String mDeliveryMode;
        public XTestInbound(String deliveryMode) {
            mDeliveryMode = deliveryMode;
        }
        public void modifyDD(EmbeddedDescriptor dd, QueueEndToEnd.ActivationConfig spec) throws Exception {
            spec.setConcurrencyMode(mDeliveryMode);
        }

        public abstract void test(TcpProxyNIO proxy, Passthrough p, Container c) throws Exception;
    }
    
    /**
     * Sends a number of msgs through the system; breaks all connections briefly; sends
     * another batch of messages through the system
     * 
     * @author fkieviet
     */
    public class XTestInboundDisruptOnce extends XTestInbound {
        public XTestInboundDisruptOnce(String deliveryMode) {
            super(deliveryMode);
        }
        @Override
        public void test(TcpProxyNIO proxy, Passthrough p, Container c) throws Exception {
            int N = 100;
            for (int i = 1; i <= 1; i++) {
                p.setNMessagesToSend(N);
                p.setBatchId(-i);
                p.passFromQ1ToQ2();
                
                System.out.println(N + " msgs processed");
                
                // Kill all
                proxy.close();
                Thread.sleep(5000);
                proxy.restart();
                
                System.out.println("Resuming");
                p.setBatchId(i);
                p.passFromQ1ToQ2();
            }
        }        
    }
    
    /**
     * Tests a brief disruption in inbound connectivity causing the adapter to go
     * into distress mode; then while in this mode undeploys the app; then
     * re-establishes connectivity and redeploys the app
     * 
     * @author fkieviet
     */
    public class XTestInboundUndeploy extends XTestInbound {
        public XTestInboundUndeploy(String deliveryMode) {
            super(deliveryMode);
        }
        @Override
        public void test(TcpProxyNIO proxy, Passthrough p, Container c) throws Exception {
            int N = 100;
            for (int i = 1; i <= 1; i++) {
                p.setNMessagesToSend(N);
                p.setBatchId(-i);
                p.passFromQ1ToQ2();
                
                System.out.println(N + " msgs processed");
                
                // Kill all 
                proxy.close();
                
                // State should now be SUSPENDED and unsuspend should throw
//                ActivationMBean mbean = (ActivationMBean) c.getMBeanProxy(MBEAN,
//                    ActivationMBean.class);
//                
//                boolean isSuspended = false;
//                for (int k = 0; k < 100; k++) {
//                    isSuspended = mbean.xgetSuspended();
//                    if (isSuspended) {
//                        break;
//                    }
//                    Thread.sleep(200);
//                }
//                assertTrue(isSuspended);
//
//                try {
//                    mbean.startService();
//                    throw new Exception("Exception expected");
//                } catch (MBeanException expected) {
//                }
                
                // Undeploy
                c.undeploy(mTestEarName);
                System.out.println("Undeployed");
                
                // Redeploy
                proxy.restart();
                c.deployModule(mTestEar.getAbsolutePath());
                
                System.out.println("Resuming");
                p.setBatchId(i);
                p.passFromQ1ToQ2();
            }
        }        
    }

    /**
     * Tests a brief disruption in inbound connectivity causing the adapter to go
     * into distress mode; then while in this mode undeploys the app; then
     * re-establishes connectivity and redeploys the app
     * 
     * @author fkieviet
     */
    public class XTestInboundRedeploy extends XTestInbound {
        public XTestInboundRedeploy(String deliveryMode) {
            super(deliveryMode);
        }
        @Override
        public void test(TcpProxyNIO proxy, Passthrough p, Container c) throws Exception {
            int N = 100;
            for (int i = 1; i <= 1; i++) {
                p.setNMessagesToSend(N);
                p.setBatchId(-i);
                p.passFromQ1ToQ2();
                
                System.out.println(N + " msgs processed");
                
                // Kill all and redeploy
                proxy.close();
                c.redeployModule(mTestEar.getAbsolutePath());
                System.out.println("Redeployed");
                
                // Redeploy
                System.out.println("Resuming");
                proxy.restart();
                p.setBatchId(i);
                p.passFromQ1ToQ2();
            }
        }        
    }

    public void testDistributedSubscriberStandardFailoverCC() throws Throwable {
        doTestDistributedSubscriberStandardFailover("cc");
    }
    
    public void testDistributedSubscriberStandardFailoverSerial() throws Throwable {
        doTestDistributedSubscriberStandardFailover("serial");
    }
    
    public void testDistributedSubscriberStandardFailoverSync() throws Throwable {
        doTestDistributedSubscriberStandardFailover("sync");
    }
    
    /**
     * Tests failover scenarios for durable subscribers: creates an external durable
     * subscriber and deploys. The connection should fail and should retry automatically.
     * After a while, the external subscriber is closed, and the activation should
     * succeed. 
     * 
     * @param mode concurrency
     * @throws Throwable
     */
    /**
     * @param mode
     * @throws Throwable
     */
    public void doTestDistributedSubscriberStandardFailover(String mode) throws Throwable {
        
        TcpProxyNIO proxy = null;
        String url = null;
        if (useProxy()){
            // Setup proxy
            UrlParser realUrl = new UrlParser(getJMSProvider().getConnectionUrl(this));
            proxy = new TcpProxyNIO(realUrl.getHost(), realUrl.getPort()); 
            url = getJMSProvider().createConnectionUrl("localhost", proxy.getPort());
        } else {
            url = getJMSProvider().getConnectionUrl(this);
        }


        Passthrough p = createPassthrough(mServerProperties);
               
        EmbeddedDescriptor dd = getDD();
        ActivationConfig spec = (ActivationConfig) dd.new ActivationSpec(EJBDD, "mdbtest").createActivation(ActivationConfig.class);
        spec.setContextName("j-testTTXAXA");
        spec.setDestination(p.getTopic1Name());
        spec.setDestinationType(javax.jms.Topic.class.getName());
        spec.setConcurrencyMode(mode);
        spec.setSubscriptionDurability("Durable");
        String subscriptionName = p.getDurableTopic1Name1();
        spec.setSubscriptionName(subscriptionName);
        String clientID = getJMSProvider().getClientId(p.getDurableTopic1Name1() + "clientID");
        spec.setClientId(clientID);

        spec.setConnectionURL(url + "?" + Options.In.OPTION_MINIMAL_RECONNECT_LOGGING_DURSUB 
            + "=1&com.stc.jms.socketpooling=FALSE");

        dd.update();

        // Deploy
        Container c = createContainer();
        TopicConnection conn = null;

        try {
            if (c.isDeployed(mTestEar.getAbsolutePath())) {
                c.undeploy(mTestEarName);
            }
            // Create a durable subscriber
            TopicConnectionFactory f = p.createTopicConnectionFactory();
            conn = f.createTopicConnection(p.getUserid(), p.getPassword());
            if (clientID != null && clientID.length() != 0) {
                conn.setClientID(clientID);
            }
            TopicSession sess = conn.createTopicSession(true, Session.SESSION_TRANSACTED);
            Topic t = sess.createTopic(p.getTopic1Name());
            sess.createDurableSubscriber(t, p.getDurableTopic1Name1());
     
            p.drainQ2();
            
            int iters = isFastTest() ? 1 : 2;
            for (int i = 0; i < iters; i++) {
                p.setBatchId(710 + i);
                
                c.redeployModule(mTestEar.getAbsolutePath());
                
                // Send messages; cannot be received yet
                p.sendToT1();
                
                // Wait for reconnections
                Thread.sleep(10000);
                if (useProxy()){
                    assertTrue("con=" + proxy.getConnectionsOpen(), proxy.getConnectionsOpen() >= 0);
                    assertTrue("c-con=" + proxy.getConnectionsCreated(), proxy.getConnectionsCreated() >= 3);
                }
                p.assertQ2Empty();
                
                // Now enable reconnection
                conn.close();
                
                // Readback
                p.readFromQ2(); 
                
                c.undeploy(mTestEarName);
                
                if (useProxy()){
                    assertTrue("con=" + proxy.getConnectionsOpen(), proxy.getConnectionsOpen() == 0);
                }
            }
        } finally {
            Container.safeClose(c);
            Passthrough.safeClose(p);
            Passthrough.safeClose(conn);
            if (useProxy()){
                proxy.close();
            }
        }
    }

    /**
     * Template method that takes a template class to run the tests
     * 
     * The template method is assumed to send a number of messages from Q1 to Q2 
     * while disrupting the connection. The inbound connections are led through 
     * a proxy. The test template can interrupt this proxy at strategic points.
     * Connections should be re-established and no connections should leak.
     * 
     * @throws Throwable
     */
    public void doReconnectTest(XTestInbound t) throws Throwable {
        // Setup proxy
        UrlParser realUrl = new UrlParser(getJMSProvider().getConnectionUrl(this));
        TcpProxyNIO proxy = new TcpProxyNIO(realUrl.getHost(), realUrl.getPort()); 
        
        EmbeddedDescriptor dd = getDD();

        // Proxy url
        String proxyUrl = getJMSProvider().createConnectionUrl("localhost", proxy.getPort());
        QueueEndToEnd.ActivationConfig spec = (QueueEndToEnd.ActivationConfig) dd.new ActivationSpec(
            EJBDD, "mdbtest").createActivation(QueueEndToEnd.ActivationConfig.class);
        spec.setConnectionURL(proxyUrl);
        spec.setConcurrencyMode("serial");

        // Other DD changes
        dd.findElementByText(EJBDD, "XContextName").setText("InboundReconnect");

        
        t.modifyDD(dd, spec);
        
        dd.update();
        
        Container c = createContainer();
        Passthrough p = createPassthrough(mServerProperties);
        try {
            c.redeployModule(mTestEar.getAbsolutePath());
            
            t.test(proxy, p, c);

            c.undeploy(mTestEarName);
            p.get("Queue1").assertEmpty();
        } finally {
            Container.safeClose(c);
            Passthrough.safeClose(p);
            TcpProxyNIO.safeClose(proxy);
        }
    }
    
    /**
     * Tests a brief disruption in inbound connectivity while the application is running
     * 
     * @throws Throwable on failure
     */
    public void testRunningSerial() throws Throwable {
        XTestInbound t = new XTestInboundDisruptOnce("serial");
        
        doReconnectTest(t);
    }

    /**
     * Tests a brief disruption in inbound connectivity while the application is running
     * 
     * @throws Throwable on failure
     */
    public void testRunningCC() throws Throwable {
        XTestInbound t = new XTestInboundDisruptOnce("cc");
        doReconnectTest(t);
    }

    /**
     * Tests a brief disruption in inbound connectivity while the application is running
     * 
     * @throws Throwable on failure
     */
    public void testRunningSync() throws Throwable {
        XTestInbound t = new XTestInboundDisruptOnce("sync");
        doReconnectTest(t);
    }
    
    /**
     * Tests a brief disruption in inbound connectivity causing the adapter to go
     * into distress mode; then while in this mode undeploys the app; then
     * re-establishes connectivity and redeploys the app
     * 
     * @throws Throwable on failure
     */
    public void testUndeploySerial() throws Throwable {
        XTestInbound t = new XTestInboundUndeploy("serial");
        doReconnectTest(t);
    }

    /**
     * Tests a brief disruption in inbound connectivity causing the adapter to go
     * into distress mode; then while in this mode undeploys the app; then
     * re-establishes connectivity and redeploys the app
     * 
     * @throws Throwable on failure
     */
    public void testUndeployCC() throws Throwable {
        XTestInbound t = new XTestInboundUndeploy("cc");
        doReconnectTest(t);
    }

    /**
     * Tests a brief disruption in inbound connectivity causing the adapter to go
     * into distress mode; then while in this mode undeploys the app; then
     * re-establishes connectivity and redeploys the app
     * 
     * @throws Throwable on failure
     */
    public void testUndeploySync() throws Throwable {
        XTestInbound t = new XTestInboundUndeploy("sync");
        doReconnectTest(t);
    }

    /**
     * Tests a brief disruption in inbound connectivity causing the adapter to go
     * into distress mode; then while in this mode redeploys the app; then
     * re-establishes connectivity and continues the test
     * 
     * @throws Throwable on failure
     */
    public void testRedeploySerial() throws Throwable {
        XTestInbound t = new XTestInboundRedeploy("serial");
        doReconnectTest(t);
    }
    
    /**
     * Tests a brief disruption in inbound connectivity causing the adapter to go
     * into distress mode; then while in this mode redeploys the app; then
     * re-establishes connectivity and continues the test
     * 
     * @throws Throwable on failure
     */
    public void testRedeployCC() throws Throwable {
        XTestInbound t = new XTestInboundRedeploy("cc");
        doReconnectTest(t);
    }
    
    /**
     * Tests a brief disruption in inbound connectivity causing the adapter to go
     * into distress mode; then while in this mode redeploys the app; then
     * re-establishes connectivity and continues the test
     * 
     * @throws Throwable on failure
     */
    public void testRedeploySync() throws Throwable {
        XTestInbound t = new XTestInboundRedeploy("sync");
        doReconnectTest(t);
    }
}
