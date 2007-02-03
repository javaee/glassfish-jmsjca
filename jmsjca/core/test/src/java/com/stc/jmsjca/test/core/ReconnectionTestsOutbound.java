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
import com.stc.jmsjca.util.UrlParser;

/**
 * Tests reconnection scenarios. Works only with a server that has
 * a single tcp/ip connection that can be led through a proxy. 
 * 
 * @author fkieviet
 * @version $Revision: 1.1.1.3 $
 */
public abstract class ReconnectionTestsOutbound extends EndToEndBase {
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

    /**
     * Nothing
     */
    public void testDummy() {
        
    }
    
    /**
     * Template class to run disconnection tests
     * 
     * @author fkieviet
     */
    public abstract class XTest0 {
        public void modifyDD(EmbeddedDescriptor dd, QueueEndToEnd.StcmsActivation spec) throws Exception {
        }

        public abstract String getOnMessageMethod();

        public abstract void test(TcpProxyNIO proxy, Passthrough p, Container c) throws Exception;
    }

    /**
     * Kills connections a large number of times to test for resource pool exhaustion
     * 
     * @author fkieviet
     */
    public abstract class XTestExhaustion extends XTest0 {
        public void test(TcpProxyNIO proxy, Passthrough p, Container c) throws Exception {
            int N = isFastTest() ? 2 : 50;
            p.setNMessagesToSend(N);
            p.passFromQ1ToQ2();
            
            System.out.println(N + " msgs processed");
            
            for (int i = 0; i < N; i++) {
                // Break communications
                System.out.println("Kill #" + i);
                proxy.killAllConnections();
                
                // Continue
                System.out.println("Resuming...");
                p.setNMessagesToSend(1);
                p.passFromQ1ToQ2();
            }
        }
    }
    
    /**
     * Passes a batch of msgs, breaks the connections, passes another batch of msgs.
     * Repeats.
     * 
     * @author fkieviet
     */
    public abstract class XTestReconnect extends XTestExhaustion {
        public void test(TcpProxyNIO proxy, Passthrough p, Container c) throws Exception {
            int iters = isFastTest() ? 1 : 2;
            for (int i = 1; i <= iters; i++) {
                int N = 100;
                p.setNMessagesToSend(N);
                p.setBatchId(-i);
                p.passFromQ1ToQ2();
                
                System.out.println(N + " msgs processed");
                
                // Kill all
                proxy.killAllConnections();
                
                System.out.println("Resuming");
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
    public void doReconnectTest(XTestExhaustion t) throws Throwable {
        // Setup proxy
        UrlParser realUrl = new UrlParser(getConnectionUrl());
        TcpProxyNIO proxy = new TcpProxyNIO(realUrl.getHost(), realUrl.getPort()); 
        
        EmbeddedDescriptor dd = getDD();

        // Proxy url
        String proxyUrl = createConnectionUrl("localhost", proxy.getPort());
        StcmsConnector cc = (StcmsConnector) dd.new ResourceAdapter(RAXML)
            .createConnector(StcmsConnector.class);
        cc.setConnectionURL(proxyUrl);
        cc = (StcmsConnector) dd.new ResourceAdapter(RAXML1)
            .createConnector(StcmsConnector.class);
        cc.setConnectionURL(proxyUrl);

        // Other DD changes
        dd.findElementByText(EJBDD, "testQQXAXA").setText(t.getOnMessageMethod());
        dd.findElementByText(EJBDD, "XContextName").setText(t.getOnMessageMethod());

        QueueEndToEnd.StcmsActivation spec = (QueueEndToEnd.StcmsActivation) dd
        .new ActivationConfig(EJBDD,"mdbtest")
        .createActivation(QueueEndToEnd.StcmsActivation.class);
        spec.setConnectionURL(realUrl.toString());
        spec.setConcurrencyMode("serial");
        
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
     * Tests connection pool leaks if an exception is thrown during the enlistment 
     * in CMT
     * 
     * @throws Throwable
     */
    public void testReconnectOutLeakXACMT() throws Throwable {
        doReconnectTest(new XTestExhaustion() {
            public String getOnMessageMethod() {
                 return "reconnectOutXA";
            }

            public void test(TcpProxyNIO proxy, Passthrough p, Container c) throws Exception {
                super.test(proxy, p, c);
            }
        });
    }

    /**
     * Tests connection pool leaks if an exception is thrown during the enlistment
     * of an XA resource after the connection has been created. This is done using
     * BMT.
     * 
     * @throws Throwable
     */
    public void testReconnectOutLeakXABMT() throws Throwable {
        doReconnectTest(new XTestExhaustion() {
            public void modifyDD(EmbeddedDescriptor dd, QueueEndToEnd.StcmsActivation spec) throws Exception {
                dd.findElementByName(EJBDD, "transaction-type").setText("Bean");
                dd.findElementByName(EJBDD, "trans-attribute").setText("NotSupported");
            }

            public String getOnMessageMethod() {
                return "reconnectOutBMTXA";
            }

            public void test(TcpProxyNIO proxy, Passthrough p, Container c) throws Exception {
                super.test(proxy, p, c);
            }
        });
    }
  
    /**
     * Tests reconnection when the failure happens during enlistment in CMT;
     * serial mode
     * 
     * @throws Throwable
     */
    public void testReconnectOutXASerial() throws Throwable {
        doReconnectTest(new XTestReconnect() {
            public String getOnMessageMethod() {
                return "reconnectOutXA";
            }
            
            public void modifyDD(EmbeddedDescriptor dd, QueueEndToEnd.StcmsActivation spec) throws Exception {
                spec.setConcurrencyMode("serial");
            }
        });
    }
    
    /**
     * Tests reconnection when the failure happens during enlistment in CMT;
     * CC mode
     * 
     * @throws Throwable
     */
    public void testReconnectOutXACC() throws Throwable {
        doReconnectTest(new XTestReconnect() {
            public String getOnMessageMethod() {
                return "reconnectOutXA";
            }
            
            public void modifyDD(EmbeddedDescriptor dd, QueueEndToEnd.StcmsActivation spec) throws Exception {
                spec.setConcurrencyMode("cc");
            }
        });
    }
  
    /**
     * Tests reconnection when the failure happens during normal operation; this tests
     * the pool cleaner or the periodic check of connections when they are used from 
     * the pool.
     * CC mode
     * 
     * @throws Throwable
     */
    public void testReconnectOutNoTxCC() throws Throwable {
        doReconnectTest(new XTestReconnect() {
            public String getOnMessageMethod() {
                return "reconnectOutNoTx";
            }
            
            public void modifyDD(EmbeddedDescriptor dd, QueueEndToEnd.StcmsActivation spec) throws Exception {
                spec.setConcurrencyMode("cc");
                dd.findElementByText(RAXML, "XATransaction").setText("NoTransaction");

                Outbound out = (Outbound) dd.new ResourceAdapter(RAXML)
                .createConnector(Outbound.class);
                out.setOptions("JMSJCA.NoXA=true\nJMSJCA.IgnoreTx=false\n");
            }
        });
    }
    
    /**
     * Tests reconnection when the failure happens during normal operation; this tests
     * the pool cleaner or the periodic check of connections when they are used from 
     * the pool.
     * Serial mode
     * 
     * @throws Throwable
     */
    public void testReconnectOutNoTxSerial() throws Throwable {
        doReconnectTest(new XTestReconnect() {
            public String getOnMessageMethod() {
                return "reconnectOutNoTx";
            }
            
            public void modifyDD(EmbeddedDescriptor dd, QueueEndToEnd.StcmsActivation spec) throws Exception {
                spec.setConcurrencyMode("serial");
                dd.findElementByText(RAXML, "XATransaction").setText("NoTransaction");

                Outbound out = (Outbound) dd.new ResourceAdapter(RAXML)
                .createConnector(Outbound.class);
                out.setOptions("JMSJCA.NoXA=true\nJMSJCA.IgnoreTx=false\n");
            }
        });
    }
    
    /**
     * Tests reconnection when the failure happens during enlistment. The enlistment
     * happens after the connection is created.
     * 
     * Serial mode
     * 
     * @throws Throwable
     */
    public void testreconnectOutLocalTxBMTLateEnlistmentSerial() throws Throwable {
        doReconnectTest(new XTestReconnect() {
            public String getOnMessageMethod() {
                return "reconnectOutLocalTxBMTLateEnlistment";
            }
            
            public void modifyDD(EmbeddedDescriptor dd, QueueEndToEnd.StcmsActivation spec) throws Exception {
                spec.setConcurrencyMode("serial");
                dd.findElementByText(RAXML, "XATransaction").setText("LocalTransaction");
                dd.findElementByName(EJBDD, "transaction-type").setText("Bean");
                dd.findElementByName(EJBDD, "trans-attribute").setText("NotSupported");
            }
        });
    }

    /**
     * Tests reconnection when the failure happens during enlistment. The enlistment
     * happens when the connection is created.
     * 
     * Serial mode
     * 
     * @throws Throwable
     */
    public void testreconnectOutLocalTxBMTEarlyEnlistmentSerial() throws Throwable {
        doReconnectTest(new XTestReconnect() {
            public String getOnMessageMethod() {
                return "reconnectOutLocalTxBMTEarlyEnlistment";
            }
            
            public void modifyDD(EmbeddedDescriptor dd, QueueEndToEnd.StcmsActivation spec) throws Exception {
                spec.setConcurrencyMode("serial");
                dd.findElementByText(RAXML, "XATransaction").setText("LocalTransaction");
                dd.findElementByName(EJBDD, "transaction-type").setText("Bean");
                dd.findElementByName(EJBDD, "trans-attribute").setText("NotSupported");
            }
        });
    }
    /**
     * Tests reconnection when the failure happens during enlistment. The enlistment
     * happens after the connection is created.
     * 
     * Serial mode
     * 
     * @throws Throwable
     */
    public void testreconnectOutLocalTxBMTLateEnlistmentCC() throws Throwable {
        doReconnectTest(new XTestReconnect() {
            public String getOnMessageMethod() {
                return "reconnectOutLocalTxBMTLateEnlistment";
            }
            
            public void modifyDD(EmbeddedDescriptor dd, QueueEndToEnd.StcmsActivation spec) throws Exception {
                spec.setConcurrencyMode("cc");
                dd.findElementByText(RAXML, "XATransaction").setText("LocalTransaction");
                dd.findElementByName(EJBDD, "transaction-type").setText("Bean");
                dd.findElementByName(EJBDD, "trans-attribute").setText("NotSupported");
            }
        });
    }

    /**
     * Tests reconnection when the failure happens during enlistment. The enlistment
     * happens when the connection is created.
     * 
     * Serial mode
     * 
     * @throws Throwable
     */
    public void testreconnectOutLocalTxBMTEarlyEnlistmentCC() throws Throwable {
        doReconnectTest(new XTestReconnect() {
            public String getOnMessageMethod() {
                return "reconnectOutLocalTxBMTEarlyEnlistment";
            }
            
            public void modifyDD(EmbeddedDescriptor dd, QueueEndToEnd.StcmsActivation spec) throws Exception {
                spec.setConcurrencyMode("cc");
                dd.findElementByText(RAXML, "XATransaction").setText("LocalTransaction");
                dd.findElementByName(EJBDD, "transaction-type").setText("Bean");
                dd.findElementByName(EJBDD, "trans-attribute").setText("NotSupported");
            }
        });
    }

    /**
     * Tests connections that always return invalid
     * 
     * Serial mode
     * 
     * @throws Throwable
     */
    public void testAlwaysInvalid() throws Throwable {
        EmbeddedDescriptor dd = getDD();

        // Proxy url
//        StcmsConnector cc = (StcmsConnector) dd.new ResourceAdapter(RAXML)
//            .createConnector(StcmsConnector.class);
//        cc = (StcmsConnector) dd.new ResourceAdapter(RAXML1)
//            .createConnector(StcmsConnector.class);

        // Other DD changes
        dd.findElementByText(EJBDD, "testQQXAXA").setText("reconnectOutAlwaysInvalid");
        dd.findElementByText(EJBDD, "XContextName").setText("reconnectOutAlwaysInvalid");

        QueueEndToEnd.StcmsActivation spec = (QueueEndToEnd.StcmsActivation) dd
        .new ActivationConfig(EJBDD,"mdbtest")
        .createActivation(QueueEndToEnd.StcmsActivation.class);
        spec.setConcurrencyMode("serial");
        
        dd.update();
        
        Container c = createContainer();
        Passthrough p = createPassthrough(mServerProperties);
        try {
            c.redeployModule(mTestEar.getAbsolutePath());
            
            int N = 100;
            p.setNMessagesToSend(N);
            p.setBatchId(2);
            p.passFromQ1ToQ2();

            c.undeploy(mTestEarName);
            p.get("Queue1").assertEmpty();
        } finally {
            Container.safeClose(c);
            Passthrough.safeClose(p);
        }
    }
}
