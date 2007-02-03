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
import com.stc.jmsjca.test.core.Passthrough;
import com.stc.jmsjca.test.core.TopicEndToEnd;
import com.stc.jmsjca.container.EmbeddedDescriptor;

/**
 * Required:
 * test.server.properties = path to properties file containing server config
 * test.ear.path          = path to ear file to be tested
 *
 * Example for Eclipse:
 *     -Dtest.server.properties=../../R1/logicalhost/testsettings.properties
 *          -Dtest.ear.path=rastcms/test/rastcms-test.ear
 * with working directory
 *     ${workspace_loc:e-jmsjca/build}/..
 *
 * @author fkieviet
 * @version $Revision: 1.4 $
 */
public class Loop extends StcmsEndToEnd {
    public void testLoop1() throws Throwable {
        String url = System.getProperty("loop.url", "");
        if (url.equals("mock")) {
            doMockLoop();
        } else if (url.equals("mocknotx")) {
                doMockLoopNoTx();
        } else {
            doRealLoop();
        }
    }
    
    private void doLoop() throws Throwable {
        long duration = 10;
        try {
            duration = Long.parseLong(System.getProperty("loop.duration.minutes"));
        } catch (NumberFormatException ignore) {
            // ignore
        }
        
        if (duration > 0) {
            // Deploy
            Container c = createContainer();
            Passthrough p = createPassthrough(mServerProperties);
            try {
                if (c.isDeployed(mTestEar.getAbsolutePath())) {
                    c.undeploy(mTestEar.getAbsolutePath());
                }
                c.deployModule(mTestEar.getAbsolutePath());
                
                TopicEndToEnd.ActivationMBean mbean = (TopicEndToEnd.ActivationMBean) c
                .getMBeanProxy(MBEAN, TopicEndToEnd.ActivationMBean.class);
                
                // Wait 1 minute
                System.out.println("Waiting for 1 minute");
                Thread.sleep(60 * 1000);
                System.out.println("Stats=" + mbean.xgetStats());
                mbean.resetStats();
                
                // Wait
                System.out.println("Waiting for " + duration 
                    + " minutes (to change set 'loop.duration.minutes')");
                Thread.sleep(duration * 60 * 1000);
                
                System.out.println("Stats=" + mbean.xgetStats());
                
                mbean.stopService();
                
                // Done
                c.undeploy(mTestEarName);
            } finally {
                Container.safeClose(c);
                Passthrough.safeClose(p);
            }
        }
    }

    public void doMockLoop() throws Throwable {
        // Setup DD
        EmbeddedDescriptor dd = getDD();
        dd.findElementByText(EJBDD, "XContextName").setText("j-testQQXAXA");
        dd.findElementByText(EJBDD, "testQQXAXA").setText("testQQXAXALoop");

        StcmsConnector cc = (StcmsConnector) dd.new ResourceAdapter(RAXML).createConnector(StcmsConnector.class);
        cc.setConnectionURL("stcms://localhost:9999?com.stc.jms.mock=true");

        StcmsActivation a = (StcmsActivation) dd.new ActivationConfig(EJBDD, "mdbtest").createActivation(StcmsActivation.class);
        a.setConcurrencyMode(System.getProperty("loop.concurrency", "serial"));
        a.setEndpointPoolMaxSize(System.getProperty("loop.endpoints", "10"));
        a.setConnectionURL("stcms://localhost:9999?com.stc.jms.mock=true");

        dd.update();
        
        // Run
        doLoop();
    }

    public void doMockLoopNoTx() throws Throwable {
        // Setup DD
        EmbeddedDescriptor dd = getDD();
        dd.findElementByText(EJBDD, "XContextName").setText("j-testQQXAXA");
        dd.findElementByText(EJBDD, "testQQXAXA").setText("testQQXAXALoop");
        dd.findElementByText(RAXML, "XATransaction").setText("NoTransaction");

        StcmsConnector cc = (StcmsConnector) dd.new ResourceAdapter(RAXML).createConnector(StcmsConnector.class);
        cc.setConnectionURL("stcms://localhost:2222?com.stc.jms.mock=true");
        cc.setOptions("JMSJCA.NoXA=true\r\nJMSJCA.IgnoreTx=false");

        StcmsActivation a = (StcmsActivation) dd.new ActivationConfig(EJBDD, "mdbtest").createActivation(StcmsActivation.class);
        a.setConcurrencyMode(System.getProperty("loop.concurrency", "serial"));
        a.setEndpointPoolMaxSize(System.getProperty("loop.endpoints", "10"));
        a.setConnectionURL("stcms://localhost:9999?com.stc.jms.mock=true");

        dd.update();
        
        // Run
        doLoop();
    }

    /**
     * Queue to queue
     * XA on in, XA on out
     * CC-mode
     *
     * @throws Throwable on failure
     */
    public void doRealLoop() throws Throwable {
        EmbeddedDescriptor dd = getDD();
        dd.findElementByText(EJBDD, "XContextName").setText("j-testQQXAXA");
        dd.findElementByText(EJBDD, "testQQXAXA").setText("testQQXAXALoop");
        StcmsConnector cc = (StcmsConnector) dd.new ResourceAdapter(RAXML).createConnector(StcmsConnector.class);
        cc.setUserName(mServerProperties.getProperty("admin.user"));
        cc.setPassword(mServerProperties.getProperty("admin.password"));
        String url = System.getProperty("loop.url", null);
        if (url != null) {
            System.out.println("Setting url to " + url);
            cc.setConnectionURL(url);
        }
        dd.update();

        long duration = 10;
        int N = 1;
        try {
            duration = Long.parseLong(System.getProperty("loop.duration.minutes"));
        } catch (NumberFormatException ignore) {
            // ignore
        }
        try {
            N = Integer.parseInt(System.getProperty("loop.nmsgs"));
        } catch (NumberFormatException ignore) {
            // ignore
        }

        // Deploy
        Container c = createContainer();
        Passthrough p = createPassthrough(mServerProperties);
        try {
            if (c.isDeployed(mTestEar.getAbsolutePath())) {
                c.undeploy(mTestEar.getAbsolutePath());
            }

            // Clear source
            Passthrough.QueueSource source = p.new QueueSource("Queue1");
            source.connect();
            source.drain();

            // Seed with N msg
            System.out.println("Sending " + N + " msgs; set 'loop.nmsgs' to change");
            source.sendBatch(N, 0, "");

            // Let MDB loop
            c.deployModule(mTestEar.getAbsolutePath());

            // Wait
            System.out.println("Waiting for " + duration + " minutes (to change set 'loop.duration.minutes')");
            Thread.sleep(duration * 60 * 1000);

            TopicEndToEnd.ActivationMBean mbean = (TopicEndToEnd.ActivationMBean) c
                    .getMBeanProxy(MBEAN, TopicEndToEnd.ActivationMBean.class);

            System.out.println("N=" + mbean.xgetNMessages());

            // Done
            c.undeploy(mTestEarName);

            // Verify no msg lost or produced
            Passthrough.QueueDest dest = p.new QueueDest("Queue1");
            dest.connect();
            dest.readback(N, 0);
        } finally {
            Container.safeClose(c);
            Passthrough.safeClose(p);
        }
        // 1 min: N=23573
        // 10 min: N=209975
        // 30 min: N=705928
        // 30 min: N=699457
        // 30 min: N=694189
    }

}
