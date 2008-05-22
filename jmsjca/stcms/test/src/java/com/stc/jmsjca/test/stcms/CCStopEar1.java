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

package com.stc.jmsjca.test.stcms;

import com.stc.jmsjca.container.Container;
import com.stc.jmsjca.container.EmbeddedDescriptor;
import com.stc.jmsjca.core.EmManagementInterface;
import com.stc.jmsjca.core.Options;
import com.stc.jmsjca.test.core.Passthrough;
import com.stc.jmsjca.test.stcms.SpecialFeaturesEar1.JmsMgt;

import java.util.List;
import java.util.Properties;

/**
 *
 * @author fkieviet
 * @version $Revision: 1.7 $
 */
public class CCStopEar1 extends StcmsEndToEnd {
    
    private int getQueueStatsNMsgs(JmsMgt jmsmgt, String queuename) throws Exception {
        long t0 = System.currentTimeMillis();
        List queueprops = jmsmgt.getQueuesWithHeaders(0);
        System.out.println("Got getQueuesWithHeaders after " + (System.currentTimeMillis() - t0) + " ms, with " + queueprops.size() + " queues");
        Properties stats = SpecialFeaturesEar1.find(queueprops, JmsMgt.Q_QUEUE_NAME, queuename);
        int nmsg = Integer.parseInt(stats.getProperty(JmsMgt.Q_MESSAGE_COUNT));
        return nmsg;
    }
    
    private void validate(JmsMgt jmsmgt) throws Exception {
        assertTrue(jmsmgt.isServerReady());

        List queues = jmsmgt.getQueues();
        assertTrue(queues.contains("Queue1"));
        assertTrue(queues.contains("Queue2"));
    }

    /**
     * Tests to see if the queueviewer statistics are correct when suspending
     * an MDB.
     *
     * @throws Throwable on failure
     */
    public void testSuspend() throws Throwable {
        EmbeddedDescriptor dd = getDD();
        dd.findElementByText(EJBDD, "XContextName").setText("j-testmbean");
        dd.update();

        Container c = createContainer();
        Passthrough p = createPassthrough(mServerProperties);
        try {
            // Undeploy
            if (c.isDeployed(mTestEar.getAbsolutePath())) {
                c.undeploy(Container.getModuleName(mTestEar.getAbsolutePath()));
            }

            // Send batch of msgs; wait until some have been processed
            int N = 1000;
            p.prepareQ1ToQ2();

            c.deployModule(mTestEar.getAbsolutePath());

            // Interrogate queue statistics: should be empty
            ActivationMBean actmbean = (ActivationMBean) c.getMBeanProxy(MBEAN,
                ActivationMBean.class);
            JmsMgt jmsmgt = (JmsMgt) c.getMBeanProxy(actmbean.getJMSServerMBean(), JmsMgt.class);
            validate(jmsmgt);
            int n1 = getQueueStatsNMsgs(jmsmgt, "Queue1");
            assertTrue(n1 == 0);
            int n2 = getQueueStatsNMsgs(jmsmgt, "Queue2");
            assertTrue(n2 == 0);

            // Send msgs and wait until some have been processed
            p.get("Queue1").sendBatch(N, 0, "x");
            p.get("Queue2").waitUntil(N/10);

            // Suspend
            System.out.println("Suspend");
            actmbean.stopService();

            // Interrogate queue statistics
            n1 = getQueueStatsNMsgs(jmsmgt, "Queue1");
            n2 = getQueueStatsNMsgs(jmsmgt, "Queue2");
            System.out.println("n1 = " + n1 + "; n2 = " + n2 + "; N = " + N);
            assertTrue(n1 + n2 == N);

            // Resume
            System.out.println("Unsuspend");
            actmbean.startService();
            p.readFromQ2();

            // Interrogate queue statistics
            n1 = getQueueStatsNMsgs(jmsmgt, "Queue1");
            n2 = getQueueStatsNMsgs(jmsmgt, "Queue2");
            System.out.println("n1 = " + n1 + "; n2 = " + n2 + "; N = " + N);
            assertTrue(n1 == 0);
            assertTrue(n2 == 0);

            // Done
            c.undeploy(mTestEarName);
            p.get("Queue1").assertEmpty();
        } finally {
            Container.safeClose(c);
            Passthrough.safeClose(p);
        }
    }

    /**
     * Tests the shutting down of an activation by an MDB by manipulation of the message's 
     * properties.
     *
     * @throws Throwable on failure
     */
    public void testStopFromMDB() throws Throwable {
        dotestStop("stopDelivery");
    }

    /**
     * Tests the shutting down of an activation by an MDB by invoking the MBean from the
     * MDB directly
     *
     * @throws Throwable on failure
     */
    public void testStopFromMDBThroughAlert() throws Throwable {
        dotestStop("stopDeliveryThroughAlert");
    }

    /**
     * Tests that the activation is automatically stopped after deployment and that at 
     * least one message was passed before that happened.
     * 
     * @param methodname TestMessageBean method
     * @throws Throwable on failure
     */
    public void dotestStop(String methodname) throws Throwable {
        EmbeddedDescriptor dd = getDD();
        dd.findElementByText(EJBDD, "XContextName").setText("j-testmbean");
        dd.findElementByText(EJBDD, "testQQXAXA").setText(methodname);

        StcmsConnector x = (StcmsConnector) dd.new ResourceAdapter(RAXML)
        .createConnector(StcmsConnector.class);
        String url = x.getConnectionURL();
        url = url + "?" + Options.In.OPTION_REDELIVERYWRAP + "=1";
        x.setConnectionURL(url);

        dd.update();

        Container c = createContainer();
        Passthrough p = createPassthrough(mServerProperties);
        try {
            // Undeploy
            if (c.isDeployed(mTestEar.getAbsolutePath())) {
                c.undeploy(Container.getModuleName(mTestEar.getAbsolutePath()));
            }

            // Send batch of msgs; wait until some have been processed
            int N = 1000;
            p.prepareQ1ToQ2();
            p.get("Queue2").close();

            c.deployModule(mTestEar.getAbsolutePath());

            // Interrogate queue statistics: should be empty
            ActivationMBean actmbean = (ActivationMBean) c.getMBeanProxy(MBEAN,
                ActivationMBean.class);
            JmsMgt jmsmgt = (JmsMgt) c.getMBeanProxy(actmbean.getJMSServerMBean(), JmsMgt.class);
            validate(jmsmgt);
            int n1 = getQueueStatsNMsgs(jmsmgt, "Queue1");
            assertTrue(n1 == 0);
            int n2 = getQueueStatsNMsgs(jmsmgt, "Queue2");
            assertTrue(n2 == 0);

            // Send msgs; the MDB will shut itself down
            p.get("Queue1").sendBatch(N, 0, "x");

            // Readback status until the status is disconnected
            boolean ok = false;
            for (int i = 0; i < 100; i++) {
                String status = actmbean.xgetStatus();
                if (EmManagementInterface.DISCONNECTED.equals(status)) {
                    ok = true;
                    break;
                }
                System.out.println("Waiting for DISCONNECTED");
                Thread.sleep(1000);
            }
            assertTrue(ok);

            // Interrogate queue statistics checking for message loss
            n1 = getQueueStatsNMsgs(jmsmgt, "Queue1");
            n2 = getQueueStatsNMsgs(jmsmgt, "Queue2");
            System.out.println("n1 = " + n1 + "; n2 = " + n2 + "; N = " + N);
            assertTrue(n1 > 0);
            assertTrue(n1 + n2 == N);

            // Done; drain and check
            int n1a = p.get("Queue1").drain();
            ((Passthrough.QueueDest) p.get("Queue2")).connect();
            int n2a = p.get(p.getQueue2Name()).drain();
            System.out.println("n1 = " + n1a + "; n2 = " + n2a + "; N = " + N);
            assertTrue(n1a == n1);
            assertTrue(n1a + n2a == N);

            // Redundant test using queue statistics
            n1 = getQueueStatsNMsgs(jmsmgt, "Queue1");
            n2 = getQueueStatsNMsgs(jmsmgt, "Queue2");
            System.out.println("n1 = " + n1 + "; n2 = " + n2 + "; N = " + N);
            assertTrue(n1 == 0);
            assertTrue(n2 == 0);

            c.undeploy(mTestEarName);
        } finally {
            Container.safeClose(c);
            Passthrough.safeClose(p);
        }
    }
}
