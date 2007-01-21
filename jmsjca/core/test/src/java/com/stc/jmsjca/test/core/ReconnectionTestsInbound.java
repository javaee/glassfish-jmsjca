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
 * $RCSfile: ReconnectionTestsInbound.java,v $
 * $Revision: 1.1.1.2 $
 * $Date: 2007-01-21 07:52:44 $
 *
 * Copyright 2003-2007 Sun Microsystems, Inc. All Rights Reserved.  
 */

package com.stc.jmsjca.test.core;

import com.stc.jmsjca.container.Container;
import com.stc.jmsjca.container.EmbeddedDescriptor;
import com.stc.jmsjca.util.UrlParser;

/**
 * Tests reconnection scenarios. Works only with a server that has
 * a single tcp/ip connection that can be led through a proxy. 
 * 
 * @author fkieviet
 * @version $Revision: 1.1.1.2 $
 */
public abstract class ReconnectionTestsInbound extends EndToEndBase {
    /**
     * Returns the JMS server's connection URL
     * 
     * @return
     */
    public abstract String getConnectionUrl();
    
    /**
     * Composes a connection URL based on server and port
     * 
     * @param server
     * @param port
     * @return
     */
    public abstract String createConnectionUrl(String server, int port);

    // To test:
    // Connection failure in running application (CC, serial, sync)
    // Connection failure upon deployment (CC, serial, sync)
    // Undeployment of application that is reconnecting
    
    
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
        public void modifyDD(EmbeddedDescriptor dd, QueueEndToEnd.StcmsActivation spec) throws Exception {
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

    /**
     * Template method that takes a template class to run the tests
     * 
     * The template method is assumed to senda number of messages from Q1 to Q2 
     * while disrupting the connection. The outbound connections are led through 
     * a proxy. The test template can interrupt this proxy at strategic points.
     * Connections should be re-established and no connections should leak.
     * 
     * @throws Throwable
     */
    public void doReconnectTest(XTestInbound t) throws Throwable {
        // Setup proxy
        UrlParser realUrl = new UrlParser(getConnectionUrl());
        TcpProxyNIO proxy = new TcpProxyNIO(realUrl.getHost(), realUrl.getPort()); 
        
        EmbeddedDescriptor dd = getDD();

        // Proxy url
        String proxyUrl = createConnectionUrl("localhost", proxy.getPort());
        QueueEndToEnd.StcmsActivation spec = (QueueEndToEnd.StcmsActivation) dd.new ActivationConfig(
            EJBDD, "mdbtest").createActivation(QueueEndToEnd.StcmsActivation.class);
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
