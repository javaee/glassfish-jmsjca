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
import com.stc.jmsjca.core.Delivery;
import com.stc.jmsjca.core.Options;
import com.stc.jmsjca.core.SyncDelivery;
import com.stc.jmsjca.test.core.Passthrough.QueueDest;
import com.stc.jmsjca.test.core.Passthrough.QueueSource;
import com.stc.jmsjca.test.core.Passthrough.TopicDest;

import javax.jms.BytesMessage;
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
import javax.jms.QueueSession;
import javax.jms.Session;
import javax.jms.StreamMessage;
import javax.jms.TextMessage;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

/**
 * Required: test.server.properties = path to properties file containing server
 * config test.ear.path = path to ear file to be tested
 * 
 * Example for Eclipse:
 * -Dtest.server.properties=../../R1/logicalhost/testsettings.properties
 * -Dtest.ear.path=rastcms/test/rastcms-test.ear with working directory
 * ${workspace_loc:e-jmsjca/build}
 * 
 * @author fkieviet
 * @version $Revision: 1.7 $
 */
public abstract class QueueEndToEnd extends EndToEndBase {

    /**
     * Queue to queue XA on in, XA on out CC-mode
     * 
     * @throws Throwable
     */
    public void testContainerManaged() throws Throwable {
        EmbeddedDescriptor dd = getDD();
        dd.findElementByText(EJBDD, "XContextName").setText("j-testQQXAXA");
        dd.update();

        // Deploy
        Container c = createContainer();
        Passthrough p = createPassthrough(mServerProperties);

        try {
            if (c.isDeployed(mTestEar.getAbsolutePath())) {
                c.undeploy(mTestEarName);
            }
            p.clearQ1Q2Q3();

            Passthrough.QueueSource source = p.new QueueSource("Queue1");
            source.connect();
            source.drain();

            c.redeployModule(mTestEar.getAbsolutePath());
            p.passFromQ1ToQ2();
            c.undeploy(mTestEarName);
            p.get("Queue1").assertEmpty();

            if (!isFastTest()) {
                c.deployModule(mTestEar.getAbsolutePath());
                p.passFromQ1ToQ2();
                c.undeploy(mTestEarName);
                p.get("Queue1").assertEmpty();
            }
        } finally {
            Container.safeClose(c);
            Passthrough.safeClose(p);
        }
    }

    /**
     * Queue to queue Bean managed serial-mode call close in midst of UT
     * 
     * @throws Throwable
     */
    public void testCloseInTranactionXA() throws Throwable {
        EmbeddedDescriptor dd = getDD();
        dd.findElementByName(EJBDD, "transaction-type").setText("Bean");
        dd.findElementByName(EJBDD, "trans-attribute").setText("NotSupported");
        dd.findElementByText(EJBDD, "cc").setText("serial");
        dd.findElementByText(EJBDD, "testQQXAXA").setText("testQQBM1");
        dd.findElementByText(EJBDD, "XContextName").setText("j-testQQBM1");
        dd.update();

        // Deploy
        Container c = createContainer();
        Passthrough p = createPassthrough(mServerProperties);

        try {
            if (c.isDeployed(mTestEar.getAbsolutePath())) {
                c.undeploy(mTestEarName);
            }
            p.clearQ1Q2Q3();

            c.redeployModule(mTestEar.getAbsolutePath());
            p.passFromQ1ToQ2();
            c.undeploy(mTestEarName);
            p.get("Queue1").assertEmpty();

            if (!isFastTest()) {
                c.deployModule(mTestEar.getAbsolutePath());
                p.passFromQ1ToQ2();
                c.undeploy(mTestEarName);
                p.get("Queue1").assertEmpty();
            }
        } finally {
            Container.safeClose(c);
            Passthrough.safeClose(p);
        }
    }

    /**
     * Queue to queue Bean managed serial-mode
     * 
     * @throws Throwable
     */
    public void testBeanManaged() throws Throwable {
        EmbeddedDescriptor dd = getDD();
        dd.findElementByName(EJBDD, "transaction-type").setText("Bean");
        dd.findElementByName(EJBDD, "trans-attribute").setText("NotSupported");
        dd.findElementByText(EJBDD, "cc").setText("serial");
        dd.findElementByText(EJBDD, "testQQXAXA").setText("testQQBM2");
        dd.findElementByText(EJBDD, "XContextName").setText("j-testQQBM2a");
        dd.update();

        // Deploy
        Container c = createContainer();
        Passthrough p = createPassthrough(mServerProperties);
        try {
            p.setStrictOrder(true);
            c.redeployModule(mTestEar.getAbsolutePath());
            p.passFromQ1ToQ2();
            c.undeploy(mTestEarName);
            p.get("Queue1").assertEmpty();
        } finally {
            Container.safeClose(c);
            Passthrough.safeClose(p);
        }
    }

    /**
     * Queue to queue Bean managed serial-mode
     * 
     * @throws Throwable
     */
    public void testBeanManagedRBAllocateOutsideOfTx() throws Throwable {
        EmbeddedDescriptor dd = getDD();

        dd.findElementByName(EJBDD, "transaction-type").setText("Bean");
        dd.findElementByName(EJBDD, "trans-attribute").setText("NotSupported");
        dd.findElementByText(EJBDD, "cc").setText("serial");
        dd.findElementByText(EJBDD, "testQQXAXA").setText("testBeanManagedRBAllocateOutsideOfTx");
        dd.findElementByText(EJBDD, "XContextName").setText("j-testQQBM2b");
        dd.update();

        // Deploy
        Container c = createContainer();
        Passthrough p = createPassthrough(mServerProperties);
        try {
            p.setStrictOrder(true);
            c.redeployModule(mTestEar.getAbsolutePath());
            p.passFromQ1ToQ2();
            c.undeploy(mTestEarName);
            p.get("Queue1").assertEmpty();
        } finally {
            Container.safeClose(c);
            Passthrough.safeClose(p);
        }
    }

    /**
     * Queue to queue Bean managed serial-mode
     * 
     * @throws Throwable
     */
    public void testBeanManagedRBAllocateInTx() throws Throwable {
        EmbeddedDescriptor dd = getDD();

        dd.findElementByName(EJBDD, "transaction-type").setText("Bean");
        dd.findElementByName(EJBDD, "trans-attribute").setText("NotSupported");
        dd.findElementByText(EJBDD, "cc").setText("serial");
        dd.findElementByText(EJBDD, "testQQXAXA").setText("testBeanManagedRBAllocateInTx");
        dd.findElementByText(EJBDD, "XContextName").setText("j-testQQBM2b");
        dd.update();

        // Deploy
        Container c = createContainer();
        Passthrough p = createPassthrough(mServerProperties);
        try {
            p.setStrictOrder(true);
            c.redeployModule(mTestEar.getAbsolutePath());
            p.passFromQ1ToQ2();
            c.undeploy(mTestEarName);
            p.get("Queue1").assertEmpty();
        } finally {
            Container.safeClose(c);
            Passthrough.safeClose(p);
        }
    }

    /**
     * Queue to queue Bean managed serial-mode RA mode = LocalTransaction
     * 
     * @throws Throwable
     */
    public void testCloseInTransactionLT() throws Throwable {
        EmbeddedDescriptor dd = getDD();
        dd.findElementByName(EJBDD, "transaction-type").setText("Bean");
        dd.findElementByName(EJBDD, "trans-attribute").setText("NotSupported");
        dd.findElementByText(EJBDD, "cc").setText("serial");
        dd.findElementByText(EJBDD, "testQQXAXA").setText("testQQBM1");
        dd.findElementByText(RAXML, "XATransaction").setText("LocalTransaction");
        dd.findElementByText(EJBDD, "XContextName").setText("j-testQQBM2");
        dd.update();

        // Deploy
        Container c = createContainer();
        Passthrough p = createPassthrough(mServerProperties);
        try {
            c.redeployModule(mTestEar.getAbsolutePath());
            p.passFromQ1ToQ2();
            c.undeploy(mTestEarName);
            p.get("Queue1").assertEmpty();
        } finally {
            Container.safeClose(c);
            Passthrough.safeClose(p);
        }
    }

    /**
     * Queue to queue using unified CMT serial-mode RA mode of unified =
     * LocalTransaction
     * 
     * Will throw this: RAR5029:Unexpected exception while registering component
     * java.lang.IllegalStateException: cannot add non-XA Resource to global JTS
     * transaction.
     * 
     * @throws Throwable
     */
    public void invalid_testMixCMTWithLTResource() throws Throwable {
        EmbeddedDescriptor dd = getDD();
        dd.findElementByText(EJBDD, "cc").setText("serial");
        dd.findElementByText(EJBDD, "testQQXAXA").setText("sendTo2UsingUnified");
        dd.findElementByText(RAXML1, "XATransaction").setText("LocalTransaction");
        dd.findElementByText(EJBDD, "XContextName").setText("sendTo2UsingUnified");
        StcmsConnector cc = (StcmsConnector) dd.new ResourceAdapter(RAXML)
            .createConnector(StcmsConnector.class);
        cc.setUserName(mServerProperties.getProperty("admin.user"));
        cc.setPassword(mServerProperties.getProperty("admin.password"));
        dd.update();

        // Deploy
        Container c = createContainer();
        Passthrough p = createPassthrough(mServerProperties);
        try {
            c.redeployModule(mTestEar.getAbsolutePath());
            p.passFromQ1ToQ2();
            c.undeploy(mTestEarName);
            p.get("Queue1").assertEmpty();
        } finally {
            Container.safeClose(c);
            Passthrough.safeClose(p);
        }
    }

    /**
     * Queue to queue + queue Bean managed serial-mode XA
     * 
     * What it will test: connection sharing
     * 
     * @throws Throwable
     */
    public void testSharedXAResources() throws Throwable {
        EmbeddedDescriptor dd = getDD();
        dd.findElementByName(EJBDD, "transaction-type").setText("Bean");
        dd.findElementByName(EJBDD, "trans-attribute").setText("NotSupported");
        dd.findElementByText(EJBDD, "cc").setText("serial");
        dd.findElementByText(EJBDD, "testQQXAXA").setText("sendTo2And3CloseAtEnd");
        dd.findElementByText(EJBDD, "XContextName").setText("j-testMultiXAResourcesA");
        dd.update();

        Container c = createContainer();
        Passthrough p = createPassthrough(mServerProperties);
        try {
            p.setStrictOrder(true);
            c.redeployModule(mTestEar.getAbsolutePath());
            p.passFromQ1ToQ2andQ3();
            c.undeploy(mTestEarName);
            p.get("Queue1").assertEmpty();
        } finally {
            Container.safeClose(c);
            Passthrough.safeClose(p);
        }
    }

    /**
     * Queue to queue + queue Bean managed serial-mode Local Transaction
     * 
     * What it will test: connection sharing
     * 
     * @throws Throwable
     */
    public void testSharedLTResources() throws Throwable {
        EmbeddedDescriptor dd = getDD();
        dd.findElementByName(EJBDD, "transaction-type").setText("Bean");
        dd.findElementByName(EJBDD, "trans-attribute").setText("NotSupported");
        dd.findElementByText(EJBDD, "cc").setText("serial");
        dd.findElementByText(EJBDD, "testQQXAXA").setText("sendTo2And3CloseAtEnd");
        dd.findElementByText(EJBDD, "XContextName").setText("j-testSharedLTResourcesA");
        dd.findElementByText(RAXML, "XATransaction").setText("LocalTransaction");
        dd.update();

        Container c = createContainer();
        Passthrough p = createPassthrough(mServerProperties);
        try {
            p.setStrictOrder(true);
            c.redeployModule(mTestEar.getAbsolutePath());
            p.passFromQ1ToQ2andQ3();
            c.undeploy(mTestEarName);
            p.get("Queue1").assertEmpty();
        } finally {
            Container.safeClose(c);
            Passthrough.safeClose(p);
        }
    }

    /**
     * Queue to queue + queue Bean managed serial-mode XA
     * 
     * What it will test: connection reuse within same transaction
     * 
     * @throws Throwable
     */
    public void testReuseXAResources() throws Throwable {
        EmbeddedDescriptor dd = getDD();
        dd.findElementByName(EJBDD, "transaction-type").setText("Bean");
        dd.findElementByName(EJBDD, "trans-attribute").setText("NotSupported");
        dd.findElementByText(EJBDD, "cc").setText("serial");
        dd.findElementByText(EJBDD, "testQQXAXA").setText("sendTo2And3CloseImmediately");
        dd.findElementByText(EJBDD, "XContextName").setText("j-testMultiResourcesB");
        dd.update();

        Container c = createContainer();
        Passthrough p = createPassthrough(mServerProperties);
        try {
            p.setStrictOrder(true);
            c.redeployModule(mTestEar.getAbsolutePath());
            p.passFromQ1ToQ2andQ3();
            c.undeploy(mTestEarName);
            p.get("Queue1").assertEmpty();
        } finally {
            Container.safeClose(c);
            Passthrough.safeClose(p);
        }
    }

    /**
     * Queue to queue + queue Bean managed serial-mode LocalTransaction
     * 
     * What it will test: connection reuse within same transaction
     * 
     * @throws Throwable
     */
    public void testReuseLTResources() throws Throwable {
        EmbeddedDescriptor dd = getDD();
        dd.findElementByName(EJBDD, "transaction-type").setText("Bean");
        dd.findElementByName(EJBDD, "trans-attribute").setText("NotSupported");
        dd.findElementByText(EJBDD, "cc").setText("serial");
        dd.findElementByText(EJBDD, "testQQXAXA").setText("sendTo2And3CloseImmediately");
        dd.findElementByText(EJBDD, "XContextName").setText("j-testMultiResourcesB");
        dd.findElementByText(RAXML, "XATransaction").setText("LocalTransaction");
        dd.update();

        Container c = createContainer();
        Passthrough p = createPassthrough(mServerProperties);
        try {
            p.setStrictOrder(true);
            c.redeployModule(mTestEar.getAbsolutePath());
            p.passFromQ1ToQ2andQ3();
            c.undeploy(mTestEarName);
            p.get("Queue1").assertEmpty();
        } finally {
            Container.safeClose(c);
            Passthrough.safeClose(p);
        }
    }

    /**
     * Queue to queue + queue; closes within UT Bean managed serial-mode
     * LocalTransaction
     * 
     * QCF = Local transaction; allocate-close UCF = XATransaction;
     * allocate-close No sharing can take place since different resources.
     * 
     * works: dd.findElementByText(RAXML,
     * "XATransaction").setText("XATransaction"); dd.findElementByText(RAXML1,
     * "XATransaction").setText("XATransaction");
     * 
     * does not work:
     * 
     * dd.findElementByText(RAXML, "XATransaction").setText("XATransaction");
     * dd.findElementByText(RAXML1,
     * "XATransaction").setText("LocalTransaction");
     * 
     * dd.findElementByText(RAXML, "XATransaction").setText("LocalTransaction");
     * dd.findElementByText(RAXML1, "XATransaction").setText("XATransaction");
     * 
     * dd.findElementByText(RAXML, "XATransaction").setText("LocalTransaction");
     * dd.findElementByText(RAXML1,
     * "XATransaction").setText("LocalTransaction");
     * 
     * XA,XA=>OK XA,LT=>java.lang.IllegalStateException: cannot add non-XA
     * Resource to global JTS transaction.
     * LT,XA=>java.lang.IllegalStateException: Local transaction already has 1
     * non-XA Resource: cannot add more resources. LT,LT=>RAR5029:Unexpected
     * exception while registering component java.lang.IllegalStateException:
     * Local transaction already has 1 non-XA Resource: cannot add more
     * resources.
     * 
     * In all cases there is a connection leak, i.e. Caused by:
     * javax.ejb.EJBException: SimpleMessageBean.onMessage() encountered an
     * exception: javax.jms.JMSException: Could not create queue connection:
     * Error in allocating a connection. Cause: In-use connections equal
     * max-pool-size and expired max-wait-time. Cannot allocate more
     * connections.; nested exception is: javax.jms.JMSException: Could not
     * create queue connection: Error in allocating a connection. Cause: In-use
     * connections equal max-pool-size and expired max-wait-time. Cannot
     * allocate more connections.
     * 
     * 
     * @throws Throwable
     */
    public void testMultiNonSharedResources() throws Throwable {
        EmbeddedDescriptor dd = getDD();
        dd.findElementByName(EJBDD, "transaction-type").setText("Bean");
        dd.findElementByName(EJBDD, "trans-attribute").setText("NotSupported");
        dd.findElementByText(EJBDD, "cc").setText("serial");
        dd.findElementByText(EJBDD, "testQQXAXA").setText("sendTo2And3Mix");
        dd.findElementByText(EJBDD, "XContextName").setText("j-testLocalTransaction");
        dd.findElementByText(RAXML, "XATransaction").setText("XATransaction");
        dd.findElementByText(RAXML1, "XATransaction").setText("XATransaction");
        dd.update();

        Container c = createContainer();
        Passthrough p = createPassthrough(mServerProperties);
        try {
            p.setStrictOrder(true);
            c.redeployModule(mTestEar.getAbsolutePath());
            p.passFromQ1ToQ2andQ3();
            c.undeploy(mTestEarName);
            p.get("Queue1").assertEmpty();
        } finally {
            Container.safeClose(c);
            Passthrough.safeClose(p);
        }
    }

    /**
     * Queue to queue + queue; closes in UT, but prevents reuse of connection
     * Bean managed serial-mode XA
     * 
     * What it will test: two XA resources in same transaction
     * 
     * @throws Throwable
     */
    public void testMultiXA() throws Throwable {
        EmbeddedDescriptor dd = getDD();
        dd.findElementByName(EJBDD, "transaction-type").setText("Bean");
        dd.findElementByName(EJBDD, "trans-attribute").setText("NotSupported");
        dd.findElementByText(EJBDD, "cc").setText("serial");
        dd.findElementByText(EJBDD, "testQQXAXA").setText("sendTo2And3PreventSharing");
        dd.findElementByText(EJBDD, "XContextName").setText(
            "j-testConnectionSharingFailure");
        dd.update();

        Container c = createContainer();
        Passthrough p = createPassthrough(mServerProperties);
        try {
            p.setStrictOrder(true);
            c.redeployModule(mTestEar.getAbsolutePath());
            p.passFromQ1ToQ2andQ3();
            c.undeploy(mTestEarName);
            p.get("Queue1").assertEmpty();
        } finally {
            Container.safeClose(c);
            Passthrough.safeClose(p);
        }
    }

    /**
     * 
     * @throws Throwable
     */
    public void doTestSuspend(int repetitions, String mode) throws Throwable {
        EmbeddedDescriptor dd = getDD();
        dd.findElementByText(EJBDD, "cc").setText(mode);
        dd.findElementByText(EJBDD, "XContextName").setText("j-testmbean");
        dd.update();

        Container c = createContainer();
        Passthrough p = createPassthrough(mServerProperties);
        try {
            if (c.isDeployed(mTestEar.getAbsolutePath())) {
                c.undeploy(Container.getModuleName(mTestEar.getAbsolutePath()));
            }

            // Setup passthrough
            Passthrough.QueueSource source = p.new QueueSource("Queue1");
            Passthrough.QueueDest dest = p.new QueueDest("Queue2");
            source.connect();
            dest.connect();
            source.drain();
            dest.drain();
            dest.close();
            Thread.sleep(500);
            
            // Deploy
            c.deployModule(mTestEar.getAbsolutePath());

            ActivationMBean mbean = (ActivationMBean) c.getMBeanProxy(MBEAN,
                ActivationMBean.class);

            // No messages should be processed
            int n00 = mbean.xgetNMessages();
            assertTrue(n00 == 0);

            for (int repeat = 0; repeat < repetitions; repeat++) {
                boolean suspended = mbean.xgetSuspended();
                assertTrue(!suspended);
                
                int nstart = mbean.xgetNMessages();

                final int N = 750;

                // Send messages
                source.sendBatch(N, 0, "");

                // Wait for some messages to be processed
                int n = 0;
                for (int k = 0; k < 40; k++) {
                    n = mbean.xgetNMessages() - nstart;
                    if (n > 0) {
                        break;
                    }
                    Thread.sleep(250);
                }

                assertTrue(n > 0);
                assertTrue(n != N);

                // Invoke stop
                mbean.stopService();
                                
                int n0 = mbean.xgetNMessages() - nstart;
                System.out.println(n0 + " msg processed after suspend");
                assertTrue(n0 != N);

                // Assure flow has stopped
                suspended = mbean.xgetSuspended();
                assertTrue(suspended);
                Thread.sleep(1000);
                n = mbean.xgetNMessages() - nstart;
                assertTrue(n == n0);
                assertTrue(n != N);

                // Resume
                mbean.startService();

                // Assure flow has resumed
                suspended = mbean.xgetSuspended();
                assertTrue(!suspended);

                for (int k = 0; k < 60; k++) {
                    n = mbean.xgetNMessages();
                    if (n != n0) {
                        break;
                    }
                    Thread.sleep(500);
                }

                System.out.println(n + " msgs processed after unsuspend");
                assertTrue(n != n0);

                // Assure readback results
                dest.connect();
                dest.readback(N, 0);
                dest.close();
                Thread.sleep(500);                
            }

            c.undeploy(mTestEarName);
            p.get("Queue1").assertEmpty();
        } finally {
            Container.safeClose(c);
            Passthrough.safeClose(p);
        }
    }

    /**
     * @throws Throwable
     */
    public void testSuspendCC() throws Throwable {
        doTestSuspend(isFastTest() ? 5 : 50, "cc");
    }

    /**
     * @throws Throwable
     */
    public void testSuspendSerial() throws Throwable {
        doTestSuspend(isFastTest() ? 5 : 50, "serial");
    }

    /**
     * 
     * @throws Throwable
     */
    public void testConcurrency() throws Throwable {
        int CUR = getMaxConcurrency();
        int N = 1000;

        EmbeddedDescriptor dd = getDD();
        dd.findElementByText(EJBDD, "cc").setText("cc");
        dd.findElementByText(EJBDD, "testQQXAXA").setText("sleepABit");
        dd.findElementByText(EJBDD, "XContextName").setText("j-testconcurrency");
        dd.findElementByText(EJBDD, "10").setText(Integer.toString(CUR));

        dd.update();

        Container c = createContainer();
        Passthrough p = createPassthrough(mServerProperties);
        try {
            if (c.isDeployed(mTestEar.getAbsolutePath())) {
                c.undeploy(Container.getModuleName(mTestEar.getAbsolutePath()));
            }

            // Setup passthrough
            Passthrough.QueueSource source = p.new QueueSource("Queue1");
            Passthrough.QueueDest dest = p.new QueueDest("Queue2");
            source.connect();
            dest.connect();
            source.drain();
            dest.drain();

            // Deploy
            c.deployModule(mTestEar.getAbsolutePath());

            ActivationMBean mbean = (ActivationMBean) c.getMBeanProxy(MBEAN,
                ActivationMBean.class);

            // No messages should be processed
            int n = mbean.xgetNMessages();
            assertTrue(n == 0);
            assertEquals(0, mbean.xgetNHighestActiveEndpoints());
            // assertEquals(0, mbean.xgetNTotalEndpoints());
            assertEquals(0, mbean.xgetNActiveEndpoints());

            // Send messages
            source.sendBatch(N, 0, "");

            long t0 = System.currentTimeMillis();

            // Wait for some messages to be processed
            for (int k = 0; k < 100; k++) {
                n = mbean.xgetNMessages();
                if (n > N / 2) {
                    break;
                }
                Thread.sleep(1000);
            }

            assertTrue("Timed out waiting for messages to be processed", (n > 0));

            // note that the following test will fail unless the wave server
            // parameter
            // maxQueuePresend is increased from its default of 10 to be greater
            // than CUR
            int nh = mbean.xgetNHighestActiveEndpoints();
            int nt = mbean.xgetNTotalEndpoints();

            assertEquals(CUR, nt);
            assertEquals(CUR, nh);

            // Assure readback results
            dest.readback(N, 0);

            long dt = System.currentTimeMillis() - t0;
            System.out.println("Total time = " + dt + "; " + dt / N + " ms/msg; " + CUR
                * dt / N + " ms/msg*thr; CUR=" + CUR);

            c.undeploy(mTestEarName);
            p.get("Queue1").assertEmpty();
        } finally {
            Container.safeClose(c);
            Passthrough.safeClose(p);
        }
    }

    /**
     * @return the maximum number of concurrent receivers that the server will
     *         support effectively
     */
    protected int getMaxConcurrency() {
        return 32;
    }

    /**
     * Test that an application can be undeployed whilst it is still processing
     * messages without hanging or exception (CC mode)
     * 
     * Also test that it can be subsequently redeployed without hanging,
     * exception losing messages or duplicate messages
     * 
     * @throws Throwable
     */
    public void testUndeployWhenAppProcessingMessagesCC() throws Throwable {
        String concurrencyMode = "cc";
        doTestUndeployWhenAppProcessingMessages(concurrencyMode);
    }

    /**
     * Test that an application can be undeployed whilst it is still processing
     * messages without hanging or exception (serial mode)
     * 
     * Also test that it can be subsequently redeployed without hanging,
     * exception losing messages or duplicate messages
     * 
     * @throws Throwable
     */
    public void testUndeployWhenAppProcessingMessagesSerial() throws Throwable {
        String concurrencyMode = "serial";
        doTestUndeployWhenAppProcessingMessages(concurrencyMode);
    }

    /**
     * Test that an application can be undeployed whilst it is still processing
     * messages without hanging or exception, using the specified
     * concurrencyMode
     * 
     * Also test that it can be subsequently redeployed without hanging,
     * exception losing messages or duplicate messages
     * 
     * @throws Throwable
     */
    private void doTestUndeployWhenAppProcessingMessages(String concurrencyMode)
        throws Exception, JMSException, InterruptedException {
        int N = 1000;

        EmbeddedDescriptor dd = getDD();
        dd.findElementByText(EJBDD, "cc").setText(concurrencyMode);
        dd.findElementByText(EJBDD, "testQQXAXA").setText("sleepABit");
        dd.findElementByText(EJBDD, "XContextName").setText("j-testconcurrency");
        dd.update();

        Container c = createContainer();
        Passthrough p = createPassthrough(mServerProperties);
        try {
            if (c.isDeployed(mTestEar.getAbsolutePath())) {
                c.undeploy(Container.getModuleName(mTestEar.getAbsolutePath()));
            }

            // Setup passthrough
            Passthrough.QueueSource source = p.new QueueSource("Queue1");
            Passthrough.QueueDest dest = p.new QueueDest("Queue2");
            source.connect();
            dest.connect();
            source.drain();
            dest.drain();

            // Deploy
            c.deployModule(mTestEar.getAbsolutePath());

            // Send messages
            source.sendBatch(N, 0, "");

            // Wait for the first 10 messages to be processed
            for (int k = 0; k < 100; k++) {
                if (dest.queueSize() > 10) {
                    break;
                }
                Thread.sleep(500);
            }

            assertTrue("Timed out waiting for messages to be processed", (dest
                .queueSize() > 0));

            // now undeploy the application even though it is still processing
            // messages
            c.undeploy(mTestEarName);
            
            // Turn off sleep (speeds up the test significantly)
            dd.findElementByText(EJBDD, "sleepABit").setText("testQQXAXA");
            dd.update();

            // to check that the redeploy was successful, try to redeploy it
            c.deployModule(mTestEar.getAbsolutePath());

            // check that not all messages have been processed yet
            // if this test fails then this is an innefective test and we need
            // to increase N
            assertTrue("Not all messages processed yet" + N, dest.queueSize() < N);

            // now consume messages until all are processed
            // this ensures that the undeploy/redeploy didn't lose any messages
            // or cause any duplicates
            dest.readback(N, 0);

            // now undeploy for the last time
            c.undeploy(mTestEarName);

        } finally {
            Container.safeClose(c);
            Passthrough.safeClose(p);
        }
    }

    /**
     * Queue to queue MDB throws runtime exception in about 20% of the cases
     * 
     * @throws Throwable
     */
    public void testExceptionCMTSerial() throws Throwable {
        EmbeddedDescriptor dd = getDD();
        dd.findElementByText(EJBDD, "cc").setText("serial");
        dd.findElementByText(EJBDD, "testQQXAXA").setText("throwExceptionCMT");
        dd.findElementByText(EJBDD, "XContextName").setText("throwExceptionCMT");
        dd.update();

        Container c = createContainer();
        Passthrough p = createPassthrough(mServerProperties);
        try {
            p.setStrictOrder(true);
            c.redeployModule(mTestEar.getAbsolutePath());
            p.passFromQ1ToQ2();
            c.undeploy(mTestEarName);
            p.get("Queue1").assertEmpty();
        } finally {
            Container.safeClose(c);
            Passthrough.safeClose(p);
        }
    }

    /**
     * Queue to queue MDB throws runtime exception in about 20% of the cases
     * 
     * @throws Throwable
     */
    public void testExceptionCMTCC() throws Throwable {
        EmbeddedDescriptor dd = getDD();
        dd.findElementByText(EJBDD, "cc").setText("cc");
        dd.findElementByText(EJBDD, "testQQXAXA").setText("throwExceptionCMT");
        dd.findElementByText(EJBDD, "XContextName").setText("throwExceptionCMT");
        dd.update();

        Container c = createContainer();
        Passthrough p = createPassthrough(mServerProperties);
        try {
            c.redeployModule(mTestEar.getAbsolutePath());
            p.passFromQ1ToQ2();
            c.undeploy(mTestEarName);
            p.get("Queue1").assertEmpty();
        } finally {
            Container.safeClose(c);
            Passthrough.safeClose(p);
        }
    }

    /**
     * Queue to queue MDB throws runtime exception in about 20% of the cases
     * 
     * @throws Throwable
     */
    public void testExceptionBMTCC() throws Throwable {
        EmbeddedDescriptor dd = getDD();
        dd.findElementByName(EJBDD, "transaction-type").setText("Bean");
        dd.findElementByName(EJBDD, "trans-attribute").setText("NotSupported");
        dd.findElementByText(EJBDD, "cc").setText("cc");
        dd.findElementByText(EJBDD, "testQQXAXA").setText("throwExceptionBMT");
        dd.findElementByText(EJBDD, "XContextName").setText("testExceptionBMTCC");
        dd.update();

        Container c = createContainer();
        Passthrough p = createPassthrough(mServerProperties);
        try {
            c.redeployModule(mTestEar.getAbsolutePath());
            p.passFromQ1ToQ2();
            c.undeploy(mTestEarName);
            p.get("Queue1").assertEmpty();
        } finally {
            Container.safeClose(c);
            Passthrough.safeClose(p);
        }
    }

    /**
     * Queue to queue MDB throws runtime exception in about 20% of the cases
     * 
     * @throws Throwable
     */
    public void testExceptionBMTSerial() throws Throwable {
        EmbeddedDescriptor dd = getDD();
        dd.findElementByName(EJBDD, "transaction-type").setText("Bean");
        dd.findElementByName(EJBDD, "trans-attribute").setText("NotSupported");
        dd.findElementByText(EJBDD, "cc").setText("serial");
        dd.findElementByText(EJBDD, "testQQXAXA").setText("throwExceptionBMT");
        dd.findElementByText(EJBDD, "XContextName").setText("throwExceptionBMT");
        dd.update();

        Container c = createContainer();
        Passthrough p = createPassthrough(mServerProperties);
        try {
            p.setStrictOrder(true);
            c.redeployModule(mTestEar.getAbsolutePath());
            p.passFromQ1ToQ2();
            c.undeploy(mTestEarName);
            p.get("Queue1").assertEmpty();
        } finally {
            Container.safeClose(c);
            Passthrough.safeClose(p);
        }
    }
    
    /**
     * Queue to queue MDB throws runtime exception in about 20% of the cases
     * 
     * @throws Throwable
     */
    public void testExceptionBMTSync() throws Throwable {
        EmbeddedDescriptor dd = getDD();
        dd.findElementByName(EJBDD, "transaction-type").setText("Bean");
        dd.findElementByName(EJBDD, "trans-attribute").setText("NotSupported");
        dd.findElementByText(EJBDD, "cc").setText("sync");
        dd.findElementByText(EJBDD, "testQQXAXA").setText("throwExceptionBMT");
        dd.findElementByText(EJBDD, "XContextName").setText("throwExceptionBMT");
        dd.update();

        Container c = createContainer();
        Passthrough p = createPassthrough(mServerProperties);
        try {
            c.redeployModule(mTestEar.getAbsolutePath());
            p.passFromQ1ToQ2();
            c.undeploy(mTestEarName);
            p.get("Queue1").assertEmpty();
        } finally {
            Container.safeClose(c);
            Passthrough.safeClose(p);
        }
    }
    
    /**
     * A simple queue replier: listens on a queue and replies to each incoming message
     * 
     * @author fkieviet
     */
    public static class QueueReplier implements MessageListener {
        private QueueConnection mConn;
//        private QueueSender mProd;
        private QueueSession mSess;
        private int mCtProcessed;
        private List mErrors = new ArrayList();

        public QueueReplier(QueueConnectionFactory fact, String userid, String password,
            String dest) throws JMSException {

            mConn = fact.createQueueConnection(userid, password);
            mSess = mConn.createQueueSession(true, 0);
            Queue dest1 = mSess.createQueue(dest);
//            Queue voiddest = mSess.createQueue("voidqueue");
            QueueReceiver sub = mSess.createReceiver(dest1);
//            mProd = mSess.createSender(voiddest);
            sub.setMessageListener(this);
            mConn.start();
        }

        private Message copy(Message message, Session s) throws JMSException {
            Message m1;
            if (message instanceof TextMessage) {
                m1 = s.createTextMessage(((TextMessage) message).getText() + " reply from QueueReplier ");
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

        public void onMessage(Message m) {
            mCtProcessed++;
            Queue replyTo;
            Message reply;
            try {
                replyTo = (Queue) m.getJMSReplyTo();
                reply = copy(m, mSess);
                
//                mProd.send(replyTo, reply);
                //TODO JMQ does not support anonymous producers
                MessageProducer prod = mSess.createSender(replyTo);
                prod.send(reply);
                
                mSess.commit();
                prod.close();
            } catch (Exception e) {
                System.out.println("OnMessage error: " + e);
                e.printStackTrace(System.out);
                mErrors.add(e);
            }
        }
        
        public static void safeClose(QueueReplier r) {
            if (r != null && r.mConn != null) {
                try {
                    r.mConn.close();
                } catch (JMSException ignore) {
                }
            }
        }

        public int getNProcessed() {
            return mCtProcessed;
        }

        public void resetNProcessed() {
            mCtProcessed = 0;
            
        }
    }
    
    public void dotest0(String[] methodnames) throws Throwable {
        EmbeddedDescriptor dd = getDD();
        QueueEndToEnd.StcmsActivation spec = (QueueEndToEnd.StcmsActivation) dd.new ActivationConfig(
            EJBDD, "mdbtest").createActivation(QueueEndToEnd.StcmsActivation.class);
        spec.setConcurrencyMode("serial");

        dd.findElementByName(EJBDD, "transaction-type").setText("Bean");
        dd.findElementByName(EJBDD, "trans-attribute").setText("NotSupported");
        dd.findElementByText(EJBDD, "testQQXAXA").setText("see message");
        dd.findElementByText(EJBDD, "XContextName").setText("tempdest");

        dd.update();
        
        Container c = createContainer();
        Passthrough p = createPassthrough(mServerProperties);
        QueueReplier r = null;
        try {
            p.drainQueue("Queue1");
            p.drainQueue("QueueReplier");
            c.redeployModule(mTestEar.getAbsolutePath());
            r = new QueueReplier(p.createQueueConnectionFactory(),
                p.getUserid(), p.getPassword(), "QueueReplier");
            int N = 300;
            p.setNMessagesToSend(N);
            
            for (int i = 0; i < methodnames.length; i++) {
                System.out.println("Executing " + methodnames[i]);
                p.setMethodname(methodnames[i]);
                try {
                    p.passFromQ1ToQ2Mix();
                    assertTrue("" + r.getNProcessed() + " != " + N, r.getNProcessed() == N);
                } catch (Exception e) {
                    // Add methodname to failure:
                    throw new Exception("Failure in method " + methodnames[i] + ": " + e, e);
                }
                r.resetNProcessed();
            }

            c.undeploy(mTestEarName);
            p.get("Queue1").assertEmpty();
        } finally {
            Container.safeClose(c);
            Passthrough.safeClose(p);
            QueueReplier.safeClose(r);
        }
    }
    
    public void testRequestReply0() throws Throwable {
        dotest0(new String[] { "requestReply0", "requestReply1", "requestReply2",
            "requestReplyN1", "requestReplyN2", "requestReplyN3" });
    }

//    public void testRequestReply0() throws Throwable {
//        dotest0(new String[] {"requestReply0"});
//    }
//    
//    public void testRequestReply1() throws Throwable {
//        dotest0(new String[] {"requestReply1"});
//    }
//    
//    public void testRequestReply2() throws Throwable {
//        dotest0(new String[] {"requestReply2"});
//    }
//    
//    public void testRequestReplyN1() throws Throwable {
//        dotest0(new String[] {"requestReplyN1"});
//    }
//    
//    public void testRequestReplyN2() throws Throwable {
//        dotest0(new String[] {"requestReplyN2"});
//    }
//    
//    public void testRequestReplyN3() throws Throwable {
//        dotest0(new String[] {"requestReplyN3"});
//    }
    
    // To test:
    // BMT
    // ut.begin
    // createconn, create tempdest, prod(tempdest), close
    // ut.commit
    // ut.begin
    // createconn, prod(tempdest)<<should fail, close
    // ut.commit
    
    // should not leave temp dests open
    
    public void testA() throws Throwable {
        EmbeddedDescriptor dd = getDD();
        QueueEndToEnd.StcmsActivation spec = (QueueEndToEnd.StcmsActivation) dd.new ActivationConfig(
            EJBDD, "mdbtest").createActivation(QueueEndToEnd.StcmsActivation.class);
        spec.setConcurrencyMode("serial");

        dd.findElementByName(EJBDD, "transaction-type").setText("Bean");
        dd.findElementByName(EJBDD, "trans-attribute").setText("NotSupported");
        dd.findElementByText(EJBDD, "testQQXAXA").setText("testTempDestClosed");
        dd.findElementByText(EJBDD, "XContextName").setText("tempdest");

        dd.update();
        
        Container c = createContainer();
        Passthrough p = createPassthrough(mServerProperties);
        try {
            c.redeployModule(mTestEar.getAbsolutePath());
            p.setNMessagesToSend(5);
            p.passFromQ1ToQ2();

            c.undeploy(mTestEarName);
            p.get("Queue1").assertEmpty();
        } finally {
            Container.safeClose(c);
            Passthrough.safeClose(p);
        }
    }
    
    private void doTestDlq(boolean xa, boolean cc) throws Throwable {
        EmbeddedDescriptor dd = getDD();
        QueueEndToEnd.StcmsActivation spec = (QueueEndToEnd.StcmsActivation) dd.new ActivationConfig(
            EJBDD, "mdbtest").createActivation(QueueEndToEnd.StcmsActivation.class);
        spec.setConcurrencyMode(cc ? "cc" : "serial");
        
        if (!xa) {
            dd.findElementByName(EJBDD, "transaction-type").setText("Bean");
            dd.findElementByName(EJBDD, "trans-attribute").setText("NotSupported");
        }
        
        Passthrough p = createPassthrough(mServerProperties);

        String handling = "4:1;5:move(topic:" + p.getTopic2Name() + ")";
        
        // Test both ways of setting the redelivery handling
        if (cc) {
            spec.setRedeliveryHandling(handling);
        } else {
            StcmsConnector x = (StcmsConnector) dd.new ResourceAdapter(RAXML)
            .createConnector(StcmsConnector.class);
            String url = x.getConnectionURL();
            url = url + "?" + Options.In.OPTION_REDELIVERYHANDLING + "=" + handling;
            x.setConnectionURL(url);
        }

        dd.findElementByText(EJBDD, "testQQXAXA").setText(xa ? "rollback" : "throwException");
        dd.findElementByText(EJBDD, "XContextName").setText("dlq" + xa + cc);

        dd.update();
        
        Container c = createContainer();
        try {
            c.redeployModule(mTestEar.getAbsolutePath());
            p.setNMessagesToSend(100);
            p.passFromQ1ToT2();
            
            QueueSource q1 = (QueueSource) p.get(p.getQueue1Name());
            TopicDest t2 = (TopicDest) p.get(p.getTopic2Name());
            

            // Send object msg
            {
                Message m1 = q1.getSession().createObjectMessage(new String[] { "a", "b"});
                m1.setStringProperty("s1", "S1");
                m1.setIntProperty("int1", 2);
                m1.setLongProperty("long1", 3);
                q1.send(m1);
                q1.getSession().commit();
                
                // Read back object msg
                ObjectMessage m2 = (ObjectMessage) t2.receive(p.getTimeout());
                assertTrue(m2 != null);
                
                // Validate payload
                String[] payload = (String[]) m2.getObject();
                assertTrue(payload[0].equals("a"));
                assertTrue(payload[1].equals("b"));
                
                // Validate extra props
                assertTrue(m2.getIntProperty(Delivery.REDELIVERYCOUNT) == 5);
                assertTrue(m2.getStringProperty(Delivery.ORIGINALDESTINATIONNAME).equals(p.getQueue1Name()));
                assertTrue(m2.getStringProperty(Delivery.ORIGINALDESTINATIONTYPE).equals(Queue.class.getName()));
                assertTrue(m2.getLongProperty(Delivery.ORIGINALTIMESTAMP) == m1.getJMSTimestamp());

                // Validate user props
                assertTrue(m2.getStringProperty("s1").equals("S1"));
                assertTrue(m2.getIntProperty("int1") == 2);
                assertTrue(m2.getLongProperty("long1") == 3);
            }

            // Send Bytes msg
            {
                BytesMessage m1 = q1.getSession().createBytesMessage();
                m1.writeBoolean(true);
                m1.writeBytes(new byte[] { 2, 3});
                q1.send(m1);
                q1.getSession().commit();
                
                // Read back object msg
                BytesMessage m2 = (BytesMessage) t2.receive(p.getTimeout());
                assertTrue(m2 != null);
                assertTrue(m2.readBoolean());
                
                byte[] rb = new byte[3]; 
                assertTrue(m2.readBytes(rb) == 2);
                assertTrue(rb[0] == 2);
                assertTrue(rb[1] == 3);
            }
            
            // Send Map msg
            {
                MapMessage m1 = q1.getSession().createMapMessage();
                m1.setBoolean("b1", true);
                m1.setBytes("bs", new byte[] { 2, 3});
                q1.send(m1);
                q1.getSession().commit();
                
                // Read back object msg
                MapMessage m2 = (MapMessage) t2.receive(p.getTimeout());
                assertTrue(m2 != null);
                assertTrue(m2.getBoolean("b1"));
                
                byte[] rb = m2.getBytes("bs"); 
                assertTrue(rb[0] == 2);
                assertTrue(rb[1] == 3);
            }
            
            // Send Stream msg
            {
                StreamMessage m1 = q1.getSession().createStreamMessage();
                m1.writeBoolean(true);
                m1.writeBytes(new byte[] { 2, 3});
                q1.send(m1);
                q1.getSession().commit();
                
                // Read back object msg
                StreamMessage m2 = (StreamMessage) t2.receive(p.getTimeout());
                assertTrue(m2 != null);
                assertTrue(m2.readBoolean());
                
                byte[] rb = new byte[3]; 
                assertTrue(m2.readBytes(rb) == 2);
                assertTrue(rb[0] == 2);
                assertTrue(rb[1] == 3);
            }
            
            c.undeploy(mTestEarName);
            p.get("Queue1").assertEmpty();
        } finally {
            Container.safeClose(c);
            Passthrough.safeClose(p);
        }
    }


    public void testDlqMoveSerialXA() throws Throwable {
        doTestDlq(true, false);
    }

    public void testDlqMoveCCXA() throws Throwable {
        doTestDlq(true, true);
    }

    public void testDlqMoveSerialBMT() throws Throwable {
        doTestDlq(false, false);
    }

    public void testDlqMoveCCBMT() throws Throwable {
        doTestDlq(false, true);
    }
    
    public void testDlqUndeploy() throws Throwable {
        EmbeddedDescriptor dd = getDD();
        QueueEndToEnd.StcmsActivation spec = (QueueEndToEnd.StcmsActivation) dd.new ActivationConfig(
            EJBDD, "mdbtest").createActivation(QueueEndToEnd.StcmsActivation.class);
        spec.setConcurrencyMode("cc");
        
        spec.setRedeliveryHandling("5:" + Integer.MAX_VALUE);

        dd.findElementByText(EJBDD, "testQQXAXA").setText("rollback");
        dd.findElementByText(EJBDD, "XContextName").setText("dlqUndeploy");

        dd.update();
        
        Container c = createContainer();
        Passthrough p = createPassthrough(mServerProperties);
        try {
            final int N = 100;
            if (c.isDeployed(mTestEar.getAbsolutePath())) {
                c.undeploy(mTestEarName);
            }
            p.clearQ1Q2Q3();
            p.setNMessagesToSend(N);
            p.passToQ1();
            
            c.deployModule(mTestEar.getAbsolutePath());

            ActivationMBean mbean = (ActivationMBean) c.getMBeanProxy(MBEAN,
                ActivationMBean.class);

            // Make sure that all MDBs are in fact used
            int n = 0;
            for (int k = 0; k < 40; k++) {
                n = mbean.xgetNActiveEndpoints();
                if (n >= 5) {
                    break;
                }
                Thread.sleep(250);
            }
            
            System.out.println("Undeploying with " + n + " beans sleeping");
            
            // In the midst of processing, undeploy
            c.undeploy(mTestEarName);

            QueueDest dest = p.new QueueDest(p.getQueue1Name());
            dest.connect();
            dest.readback(N, 0);
        } finally {
            Container.safeClose(c);
            Passthrough.safeClose(p);
        }
    }

    public void testDlqDelete() throws Throwable {
        EmbeddedDescriptor dd = getDD();
        QueueEndToEnd.StcmsActivation spec = (QueueEndToEnd.StcmsActivation) dd.new ActivationConfig(
            EJBDD, "mdbtest").createActivation(QueueEndToEnd.StcmsActivation.class);
        spec.setConcurrencyMode("cc");
        
        spec.setRedeliveryHandling("1:delete");

        dd.findElementByText(EJBDD, "testQQXAXA").setText("rollback");
        dd.findElementByText(EJBDD, "XContextName").setText("dlqDelete");

        dd.update();
        
        Container c = createContainer();
        Passthrough p = createPassthrough(mServerProperties);
        try {
            final int N = 1;
            if (c.isDeployed(mTestEar.getAbsolutePath())) {
                c.undeploy(mTestEarName);
            }
            p.clearQ1Q2Q3();
            p.setNMessagesToSend(N);
            p.passToQ1();
            
            c.deployModule(mTestEar.getAbsolutePath());

            ActivationMBean mbean = (ActivationMBean) c.getMBeanProxy(MBEAN,
                ActivationMBean.class);

            // Make sure that a msg was processed
            int n = 0;
            for (int k = 0; k < 40; k++) {
                n = mbean.xgetNMessages();
                if (n >= N) {
                    break;
                }
                Thread.sleep(250);
            }
            Thread.sleep(250);
            
            c.undeploy(mTestEarName);

            QueueDest dest = p.new QueueDest(p.getQueue1Name());
            dest.connect();
            int nleft = dest.drain();
            assertTrue(nleft == 0);
        } finally {
            Container.safeClose(c);
            Passthrough.safeClose(p);
        }
    }

    // To test:
    // check msg redirect instead of creating
    

    /**
     * Assert that a connection can be used in auto-ack mode
     */
    public void testNoTransaction() throws Throwable {
        EmbeddedDescriptor dd = getDD();
        dd.findElementByText(EJBDD, "testQQXAXA").setText("Q2XAAndQ3NoTrans");
        dd.findElementByText(EJBDD, "XContextName").setText("Q2XAAndQ3NoTrans");
        dd.findElementByText(RAXML1, "XATransaction").setText("NoTransaction");
        StcmsConnector cc = (StcmsConnector) dd.new ResourceAdapter(RAXML1)
        .createConnector(StcmsConnector.class);
        cc.setOptions("JMSJCA.NoXA=true\r\nJMSJCA.IgnoreTx=false");
        dd.update();

        // Deploy
        Container c = createContainer();
        Passthrough p = createPassthrough(mServerProperties);

        try {
            if (c.isDeployed(mTestEar.getAbsolutePath())) {
                c.undeploy(mTestEarName);
            }
            p.clearQ1Q2Q3();

            Passthrough.QueueSource source = p.new QueueSource("Queue1");
            source.connect();
            source.drain();

            c.redeployModule(mTestEar.getAbsolutePath());
            p.passFromQ1ToQ2andQ3();
            c.undeploy(mTestEarName);
            p.get("Queue1").assertEmpty();
        } finally {
            Container.safeClose(c);
            Passthrough.safeClose(p);
        }
    }
    
    /**
     * Assert that a connection can be used in no-xa, transacted mode
     */
    public void testNoXATransacted() throws Throwable {
        EmbeddedDescriptor dd = getDD();
        dd.findElementByName(EJBDD, "transaction-type").setText("Bean");
        dd.findElementByName(EJBDD, "trans-attribute").setText("NotSupported");

        dd.findElementByText(EJBDD, "cc").setText("serial");
        dd.findElementByText(EJBDD, "testQQXAXA").setText("testNoXATransacted");
        dd.findElementByText(EJBDD, "XContextName").setText("j-testNoXATransacted");

        StcmsConnector cc = (StcmsConnector) dd.new ResourceAdapter(RAXML)
        .createConnector(StcmsConnector.class);
        String url = cc.getConnectionURL();
        if (url.indexOf('?') < 0) {
            url += "?";
        }
        url += "JMSJCA.NoXA=true";
        cc.setConnectionURL(url);
        dd.update();

        // Deploy
        Container c = createContainer();
        Passthrough p = createPassthrough(mServerProperties);

        try {
            if (c.isDeployed(mTestEar.getAbsolutePath())) {
                c.undeploy(mTestEarName);
            }
            p.clearQ1Q2Q3();

            Passthrough.QueueSource source = p.new QueueSource("Queue1");
            source.connect();
            source.drain();

            c.redeployModule(mTestEar.getAbsolutePath());
            p.passFromQ1ToQ2();
            c.undeploy(mTestEarName);
            p.get("Queue1").assertEmpty();
        } finally {
            Container.safeClose(c);
            Passthrough.safeClose(p);
        }
    }

    /**
     * Assert that a connection can be used in no-xa, transacted mode
     */
    public void testNoXANonTransacted() throws Throwable {
        EmbeddedDescriptor dd = getDD();
        dd.findElementByName(EJBDD, "transaction-type").setText("Bean");
        dd.findElementByName(EJBDD, "trans-attribute").setText("NotSupported");

        dd.findElementByText(EJBDD, "cc").setText("serial");
        dd.findElementByText(EJBDD, "testQQXAXA").setText("testNoXANonTransacted");
        dd.findElementByText(EJBDD, "XContextName").setText("j-testNoXANonTransacted");

        StcmsConnector cc = (StcmsConnector) dd.new ResourceAdapter(RAXML)
        .createConnector(StcmsConnector.class);
        String url = cc.getConnectionURL();
        if (url.indexOf('?') < 0) {
            url += "?";
        }
        url += "JMSJCA.NoXA=true&JMSJCA.IgnoreTx=false";
        cc.setConnectionURL(url);
        dd.update();

        // Deploy
        Container c = createContainer();
        Passthrough p = createPassthrough(mServerProperties);

        try {
            if (c.isDeployed(mTestEar.getAbsolutePath())) {
                c.undeploy(mTestEarName);
            }
            p.clearQ1Q2Q3();

            Passthrough.QueueSource source = p.new QueueSource("Queue1");
            source.connect();
            source.drain();

            c.redeployModule(mTestEar.getAbsolutePath());
            p.passFromQ1ToQ2();
            c.undeploy(mTestEarName);
            p.get("Queue1").assertEmpty();
        } finally {
            Container.safeClose(c);
            Passthrough.safeClose(p);
        }
    }

    /*
     * [] HUA [] Batch [] XA [] Rollback [] Stop
     * 24 methods 
     * 
     *        x                                   ok  testBatchUT
     *        x              x                        testBatchUTRB
     *        x         x                         ok  testBatchXA
     *        x         x    x                    ok  testBatchXARB
     *  x     x         x                         ok  testBatchXAHUA
     *  x     x         x    x                    ok  testBatchXAHUARB
     *  x     x                                       testBatchXAHUA
     *  x     x              x                        testBatchXAHUARB
     *  x                                         ok  testHUAUT      
     *  x                    x                    ok  testHUAUTRB      
     *  x               x                         ok  testXAHUA
     *  x               x    x                    ok  testXAHUARB
     *  
     */
    
    /**
     * Batch, XA, sync, no-rollback
     */
    public void testBatchXA() throws Throwable {
        dotestBatch("batch", 50, false, "false", "sync");
    }

    /**
     * Batch, XA, cc, no-rollback
     */
    public void testBatchXACC() throws Throwable {
        dotestBatch("batch", 50, false, "false", "cc");
    }

    /**
     * Batch, XA, sync, rollback
     */
    public void testBatchXARB() throws Throwable {
        dotestBatch("batchRollback", 3, false, "false", "sync");
    }
    
    /**
     * Batch, XA, cc, rollback
     */
    public void testBatchXARBCC() throws Throwable {
        dotestBatch("batchRollback", 3, false, "false", "cc");
    }
    
    /**
     * Batch, NonXA, sync, no-rollback
     */
    public void testBatchUT() throws Throwable {
        dotestBatch("batchUT", 50, true, "false", "sync");
    }

    /**
     * Batch, NonXA, cc, no-rollback
     */
    public void testBatchUTCC() throws Throwable {
        dotestBatch("batchUT", 50, true, "false", "cc");
    }

    /**
     * Batch, XA, sync, no-rollback, HUA
     */
    public void testBatchXAHUA() throws Throwable {
        dotestBatch("batchHoldUntilAck", 50, false, "true", "sync");
    }
    
    /**
     * Batch, XA, cc, no-rollback, HUA
     */
    public void testBatchXAHUACC() throws Throwable {
        dotestBatch("batchHoldUntilAck", 50, false, "true", "cc");
    }
    
    /**
     * Batch, XA, sync, rollback, HUA
     */
    public void testBatchXAHUARB() throws Throwable {
        dotestBatch("batchHoldUntilAckRollback", 3, false, "true", "sync");
    }
    
    /**
     * Batch, XA, cc, rollback, HUA
     */
    public void testBatchXAHUARBCC() throws Throwable {
        dotestBatch("batchHoldUntilAckRollback", 3, false, "true", "cc");
    }
    
    /**
     * No Batch, XA, sync, no-rollback, HUA
     */
    public void testHUAXA() throws Throwable {
        dotestBatch("batchHoldUntilAck", 0, false, "true", "sync");
    }
    
    /**
     * No Batch, XA, cc, no-rollback, HUA
     */
    public void testHUAXACC() throws Throwable {
        dotestBatch("batchHoldUntilAck", 0, false, "true", "cc");
    }
    
    /**
     * No Batch, XA, sync, rollback, HUA
     */
    public void testHUAXARB() throws Throwable {
        dotestBatch("batchHoldUntilAckRollback", 0, false, "true", "sync");
    }
    
    /**
     * No Batch, XA, cc, rollback, HUA
     */
    public void testHUAXARBCC() throws Throwable {
        dotestBatch("batchHoldUntilAckRollback", 0, false, "true", "cc");
    }
    
    /**
     * No Batch, UT, sync, no-rollback, HUA
     */
    public void testHUAUT() throws Throwable {
        dotestBatch("batchHUAUT", 0, true, "true", "sync");
    }
    
    /**
     * No Batch, UT, cc, no-rollback, HUA
     */
    public void testHUAUTCC() throws Throwable {
        dotestBatch("batchHUAUT", 0, true, "true", "cc");
    }
    
    /**
     * No Batch, UT, sync, no-rollback, HUA
     */
    public void testHUAUTRB() throws Throwable {
        dotestBatch("batchHUAUTRB", 0, true, "true", "sync");
    }
    
    /**
     * No Batch, UT, sync, no-rollback, HUA
     */
    public void testHUAUTRBCC() throws Throwable {
        dotestBatch("batchHUAUTRB", 0, true, "true", "cc");
    }
    
    // More HUA tests:
    // undeploy in middle
    // undeploy with messages outstanding
    
    

    /**
     * Tests various aspects of Batch and HUA by:
     * - passing msgs from Q1 to Q2
     * - each batch (i.e. end of batch msg) will result in a msg in Q3
     * - rollbacks cause messages to be sent to Q4, which are rolled back so Q4 should remain empty
     */
    public void dotestBatch(String mname, int batchsize, boolean ut, String holdUntilAck, String concMode) throws Throwable {
        EmbeddedDescriptor dd = getDD();
        dd.findElementByText(EJBDD, "testQQXAXA").setText(mname);
        dd.findElementByText(EJBDD, "XContextName").setText(mname);
        
        if (ut) {
            dd.findElementByName(EJBDD, "transaction-type").setText("Bean");
            dd.findElementByName(EJBDD, "trans-attribute").setText("NotSupported");
        }
        
        QueueEndToEnd.StcmsActivation spec = (QueueEndToEnd.StcmsActivation) dd.new ActivationConfig(
            EJBDD, "mdbtest").createActivation(QueueEndToEnd.StcmsActivation.class);
        spec.setConcurrencyMode(concMode);        
        spec.setBatchSize(Integer.toString(batchsize));
        spec.setHoldUntilAck(holdUntilAck);
        
        dd.update();

        Container c = createContainer();
        Passthrough p = createPassthrough(mServerProperties);
        try {
            if (c.isDeployed(mTestEar.getAbsolutePath())) {
                c.undeploy(mTestEarName);
            }
            p.clearQ1Q2Q3Q4();

            int N = 1000;
            p.setNMessagesToSend(N);

            c.redeployModule(mTestEar.getAbsolutePath());
            p.passFromQ1ToQ2();
            
            if (ut) {
                // Make sure that the endOfBatch messages are also delivered properly
                Thread.sleep(SyncDelivery.TIMEOUTBATCH);
            }
            
            Thread.sleep(750);//bad
                        
            c.undeploy(mTestEarName);
            Passthrough.QueueDest q3 = p.new QueueDest(p.getQueue3Name()); 
            q3.connect();
            int nbatches = q3.drain(); 

            Passthrough.QueueDest q4 = p.new QueueDest(p.getQueue4Name()); 
            q4.connect();
            int ninvalidRollbacks = q4.drain(); 
            
            System.out.println("Batches: " + nbatches);
            int n2 = p.get(p.getQueue2Name()).drain();
            System.out.println("n in queue2 = " + n2);            
            p.get(p.getQueue1Name()).assertEmpty();            
            if (batchsize > 0) {
                assertTrue("No EofBatch were found", nbatches > 0);
            }
            assertTrue("Msgs are delivered one msg per batch", nbatches < N);
            assertTrue(n2 == 0);
            assertTrue("Invalid rollbacks", ninvalidRollbacks == 0);
        } finally {
            Container.safeClose(c);
            Passthrough.safeClose(p);
        }
    }
    
    public void checkErrorQueue(Passthrough p) throws Exception {
        Passthrough.QueueDest q4 = p.new QueueDest(p.getQueue4Name()); 
        q4.connect();
        int nErrors = q4.drain();
        assertTrue("There were " + nErrors + " errors in the error queue", nErrors == 0);
    }
}
