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
import com.stc.jmsjca.test.core.Passthrough;
import com.stc.jmsjca.test.core.QueueEndToEnd;
import com.stc.jmsjca.test.core.TcpProxy;

import java.net.InetAddress;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;

/**
 * Required:
 * test.server.properties = path to properties file containing server config
 * test.ear.path          = path to ear file to be tested
 *
 * Example for Eclipse:
 *     -Dtest.server.properties=../../R1/logicalhost/testsettings.properties -Dtest.ear.path=rastcms/test/rastcms-test.ear
 * with working directory
 *     ${workspace_loc:e-jmsjca/build}/..
 *
 * @author fkieviet
 * @version $Revision: 1.1.1.2 $
 */
public class SpecialFeaturesEar1 extends StcmsEndToEnd {
    
    private String getConnectionURL(String suffix) {
        if (getContainerID().equals("rts")) {
            return "stcmss://" + suffix;
        } else {
            return "stcmss://" + getJmsServerProperties().getProperty("host") 
                + ":" 
                + getJmsServerProperties().getProperty("stcms.instance.ssl.port")
                + suffix;
        }
    }
    
    /**
     * Queue to queue, using SSL
     * XA on in, XA on out
     * CC-mode
     *
     * @throws Throwable
     */
    public void testContainerManagedSsl() throws Throwable {
        EmbeddedDescriptor dd = getDD();

        StcmsConnector cc = (StcmsConnector) dd.new ResourceAdapter(RAXML).createConnector(StcmsConnector.class);
        cc.setUserName(mServerProperties.getProperty("admin.user"));
        cc.setPassword(mServerProperties.getProperty("admin.password"));
        cc.setConnectionURL(getConnectionURL(""));

        QueueEndToEnd.StcmsActivation spec = (QueueEndToEnd.StcmsActivation) dd.new ActivationConfig(EJBDD,"mdbtest").createActivation(QueueEndToEnd.StcmsActivation.class);
        spec.setContextName("j-testQQXAXA");
        spec.setConcurrencyMode("serial");

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
     * Queue to queue, using SSL
     * XA on in, XA on out
     * CC-mode
     *
     * @throws Throwable
     */
    public void testContainerManagedSslTrustAllAuth() throws Throwable {
        EmbeddedDescriptor dd = getDD();

        StcmsConnector cc = (StcmsConnector) dd.new ResourceAdapter(RAXML).createConnector(StcmsConnector.class);
        cc.setUserName(mServerProperties.getProperty("admin.user"));
        cc.setPassword(mServerProperties.getProperty("admin.password"));
        cc.setConnectionURL(
            getConnectionURL("?com.stc.jms.ssl.authenticationmode=TrustAll"));
        
        QueueEndToEnd.StcmsActivation spec = (QueueEndToEnd.StcmsActivation) dd.new ActivationConfig(EJBDD,"mdbtest").createActivation(QueueEndToEnd.StcmsActivation.class);
        spec.setContextName("j-testQQXAXA");

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
     * Queue to queue, using SSL
     * XA on in, XA on out
     * CC-mode
     *
     * @throws Throwable
     */
    public void testContainerManagedSslServerAuthPW_RTS_ONLY() throws Throwable {
        EmbeddedDescriptor dd = getDD();

        StcmsConnector cc = (StcmsConnector) dd.new ResourceAdapter(RAXML).createConnector(StcmsConnector.class);
        cc.setUserName(mServerProperties.getProperty("admin.user"));
        cc.setPassword(mServerProperties.getProperty("admin.password"));
        cc.setConnectionURL(
            getConnectionURL("?com.stc.jms.ssl.authenticationmode=Authenticate"));
        
        QueueEndToEnd.StcmsActivation spec = (QueueEndToEnd.StcmsActivation) dd.new ActivationConfig(EJBDD,"mdbtest").createActivation(QueueEndToEnd.StcmsActivation.class);
        spec.setContextName("j-testQQXAXA");

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
     * Queue to queue, using SSL
     * XA on in, XA on out
     * CC-mode
     *
     * @throws Throwable
     */
    public void testContainerManagedSslServerAuth_RTS_ONLY() throws Throwable {
        EmbeddedDescriptor dd = getDD();

        StcmsConnector cc = (StcmsConnector) dd.new ResourceAdapter(RAXML).createConnector(StcmsConnector.class);
        cc.setUserName(mServerProperties.getProperty("admin.user"));
        cc.setPassword(mServerProperties.getProperty("admin.password"));
        cc.setConnectionURL(
            getConnectionURL("?com.stc.jms.ssl.authenticationmode=Authenticate"));

        QueueEndToEnd.StcmsActivation spec = (QueueEndToEnd.StcmsActivation) dd.new ActivationConfig(EJBDD,"mdbtest").createActivation(QueueEndToEnd.StcmsActivation.class);
        spec.setContextName("j-testQQXAXA");

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
     * Queue to queue, using SSL
     * XA on in, XA on out
     * CC-mode
     *
     * @throws Throwable
     */
    public void testContainerManagedSpecialUrl_RTS_ONLY() throws Throwable {
        EmbeddedDescriptor dd = getDD();

        StcmsConnector cc = (StcmsConnector) dd.new ResourceAdapter(RAXML).createConnector(StcmsConnector.class);
        cc.setUserName(mServerProperties.getProperty("admin.user"));
        cc.setPassword(mServerProperties.getProperty("admin.password"));
        cc.setConnectionURL(
            getConnectionURL("?com.stc.jms.ssl.authenticationmode=Authenticate"));

        QueueEndToEnd.StcmsActivation spec = (QueueEndToEnd.StcmsActivation) dd.new ActivationConfig(EJBDD,"mdbtest").createActivation(QueueEndToEnd.StcmsActivation.class);
        spec.setContextName("j-sendTo2SpecialUrl");
        spec.setConcurrencyMode("cc");

        dd.findElementByText(EJBDD, "--special--url--").setText("stcms://");
        dd.findElementByText(EJBDD, "testQQXAXA").setText("sendTo2SpecialUrl");

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
     * Queue to queue, using SSL
     * XA on in, XA on out
     * CC-mode
     *
     * @throws Throwable
     */
    public void testContainerManagedSpecialUrl2_RTS_ONLY() throws Throwable {
        EmbeddedDescriptor dd = getDD();

        StcmsConnector cc = (StcmsConnector) dd.new ResourceAdapter(RAXML).createConnector(StcmsConnector.class);
        cc.setUserName(mServerProperties.getProperty("admin.user"));
        cc.setPassword(mServerProperties.getProperty("admin.password"));
        cc.setConnectionURL(
            getConnectionURL("?com.stc.jms.ssl.authenticationmode=Authenticate"));

        QueueEndToEnd.StcmsActivation spec = (QueueEndToEnd.StcmsActivation) dd.new ActivationConfig(EJBDD,"mdbtest").createActivation(QueueEndToEnd.StcmsActivation.class);
        spec.setContextName("j-sendTo2SpecialUrl");
        spec.setConcurrencyMode("cc");

        dd.findElementByText(EJBDD, "--special--url--").setText("stcms://");
        dd.findElementByText(EJBDD, "testQQXAXA").setText("sendTo2SpecialUrlMix");

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
     * Queue to queue, using the feature of specifying the URL in the 
     * username parameter of the createConnection() method. Uses a proxy
     * to verify that the special URL was indeed used.
     * 
     * @throws Throwable
     */
    public void testContainerManagedSpecialUrl3() throws Throwable {
        EmbeddedDescriptor dd = getDD();

        // Setup proxy
        TcpProxy proxy = new TcpProxy(mServerProperties.getProperty("host"), 
            Integer.parseInt(mServerProperties.getProperty("stcms.instance.port")));

        StcmsConnector cc = (StcmsConnector) dd.new ResourceAdapter(RAXML).createConnector(StcmsConnector.class);
        cc.setUserName(mServerProperties.getProperty("admin.user"));
        cc.setPassword(mServerProperties.getProperty("admin.password"));

        QueueEndToEnd.StcmsActivation spec = (QueueEndToEnd.StcmsActivation) dd.new ActivationConfig(EJBDD,"mdbtest").createActivation(QueueEndToEnd.StcmsActivation.class);
        spec.setContextName("j-sendTo2SpecialUrl");
        spec.setConcurrencyMode("cc");

        // special URL / username:
        InetAddress localh = InetAddress.getLocalHost();
        String url = "stcms://" + localh.getHostAddress() + ":" + proxy.getPort();
        
        dd.findElementByText(EJBDD, "--special--url--").setText(url);
        dd.findElementByText(EJBDD, "testQQXAXA").setText("sendTo2SpecialUrl");

        dd.update();

        // Deploy
        Container c = createContainer();
        Passthrough p = createPassthrough(mServerProperties);
        try {
            c.redeployModule(mTestEar.getAbsolutePath());
            int n0 = proxy.getNPassThroughsCreated();
            p.setNMessagesToSend(20);
            p.passFromQ1ToQ2();
            int n1 = proxy.getNPassThroughsCreated();
            System.out.println("Wires created after passthrough: " + (n1 - n0));
            assertTrue(n1 > n0);
            c.undeploy(mTestEarName);
            p.get("Queue1").assertEmpty();
        } finally {
            Container.safeClose(c);
            Passthrough.safeClose(p);
            proxy.done();
        }
    }

    public interface JmsMgt {
        List getQueues() throws Exception;
        List getQueueMsgPropertiesList(String queueName, long minseq, int nMsgs) throws Exception;
        List getQueuesWithHeaders() throws Exception;
        boolean isServerReady() throws Exception;
        void setUserId(String userName);
        void setPassword(String password);
        String Q_QUEUE_NAME = "QUEUE_NAME";
        String Q_FIRST_ENQUEUE_TIME = "FIRST_ENQUEUE_TIME";
        String Q_LAST_ENQUEUE_TIME = "LAST_ENQUEUE_TIME";
        String Q_CURRENT_RECEIVERS = "CURRENT_RECEIVERS";
        String Q_MESSAGE_COUNT = "MESSAGE_COUNT";
        String Q_COMMITTED_MESSAGES = "COMMITTED_MESSAGES";
        String Q_MAX_SEQ = "MAX_SEQ";
        String Q_MIN_SEQ = "MIN_SEQ";
    }
    
    /**
     * Given a list of properties-sets, find the properties set that contains an
     * entry identifier=toFind, or throw an exception if not found.
     * 
     * @param ps
     * @param identifier
     * @param toFind
     * @return
     * @throws Exception
     */
    public static Properties find(List ps, String identifier, String toFind) throws Exception {
        for (Iterator iter = ps.iterator(); iter.hasNext();) {
            Properties element = (Properties) iter.next();
            if (toFind.equals(element.get(identifier))) {
                return element;
            }
        }
        throw new Exception("Item " + toFind + " not found.");
    }

    /**
     * Tests the MBean functionality wrt jms server mgt
     * 
     * Uses a passthrough proxy to assert that an external remote or global mbean
     * is used.
     * 
     * @throws Throwable
     */
    public void dotestMgt(boolean local) throws Throwable {
        // Setup proxy
        TcpProxy proxy = new TcpProxy(mServerProperties.getProperty("host"), 
            Integer.parseInt(mServerProperties.getProperty("stcms.instance.ssl.port")));
        
        // Modify DDs
        EmbeddedDescriptor dd = getDD();
        dd.findElementByText(EJBDD, "cc").setText("cc");
        dd.findElementByText(EJBDD, "testQQXAXA").setText("sleepABit");
        dd.findElementByText(EJBDD, "XContextName").setText("j-testconcurrency");
        StcmsConnector cc = (StcmsConnector) dd.new ResourceAdapter(RAXML)
            .createConnector(StcmsConnector.class);
        cc.setUserName(mServerProperties.getProperty("admin.user"));
        cc.setPassword(mServerProperties.getProperty("admin.password"));
        if (local) {
            cc.setConnectionURL("stcmss://");
        } else {
            InetAddress localh = InetAddress.getLocalHost();
            cc.setConnectionURL("stcmss://" + localh.getHostAddress() + ":" + proxy.getPort());
        }
        cc.setMBeanObjectName(RAMMBEAN);

        dd.update();
        
        int N = 100;

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
            
            // Suspend so that the number of msgs can be counted in Queue1
            ActivationMBean mbean = (ActivationMBean) c.getMBeanProxy(MBEAN,
                ActivationMBean.class);
            
            assertTrue(mbean.echoPrimitives(3, 2) == 5);
            
            int n = mbean.xgetNMessages();
            assertTrue(n == 0);
            boolean suspended = mbean.xgetSuspended();
            assertTrue(!suspended);
            mbean.stopService();

            // Send messages
            source.sendBatch(N, 0, "");
            
            int n0 = proxy.getNPassThroughsCreated();

            for (int type = 0; type < 1; type++) {
                JmsMgt jmsmgt;
                if (type == 0) {
                    // Test activation route
                    ActivationMBean actmbean = (ActivationMBean) c.getMBeanProxy(MBEAN,
                        ActivationMBean.class);
                    String oname = actmbean.getJMSServerMBean();
                    assertTrue(oname != null);
                    jmsmgt = (JmsMgt) c.getMBeanProxy(oname, JmsMgt.class);
                } else {
                    // Test RA route
                    RAMBean rambean = (RAMBean) c.getMBeanProxy(RAMMBEAN,
                        RAMBean.class);
                    String oname = rambean.getJMSServerMBean();
                    assertTrue(oname != null);
                    jmsmgt = (JmsMgt) c.getMBeanProxy(oname, JmsMgt.class);
                }
                
                assertTrue(jmsmgt.isServerReady());
                
                List queues = jmsmgt.getQueues();
                assertTrue(queues.contains("Queue1"));
                assertTrue(queues.contains("Queue2"));
                
                List queueprops = jmsmgt.getQueuesWithHeaders();
                Properties stats = find(queueprops, JmsMgt.Q_QUEUE_NAME, "Queue1");
                int nmsg = Integer.parseInt(stats.getProperty(JmsMgt.Q_MESSAGE_COUNT));
                assertTrue(nmsg == N || nmsg == (N - 1));
                stats = find(queueprops, JmsMgt.Q_QUEUE_NAME, "Queue2");
                assertEquals(Integer.toString(0), stats.get(JmsMgt.Q_MESSAGE_COUNT));
                int n1 = proxy.getNPassThroughsCreated();
                if (local) {
                    assertTrue(n1 == n0);
                } else {
                    assertTrue(n1 >= (n0 + 2));
                    System.out.println("Additional wires: " + (n1 - n0));
                }
            }
            
            mbean.startService();
            dest.readback(N, 0);
            c.undeploy(mTestEarName);
            p.get("Queue2").assertEmpty();
        } finally {
            Container.safeClose(c);
            Passthrough.safeClose(p);
            proxy.done();
        }
    }

    /**
     * Tests the MBean functionality wrt jms server mgt
     * 
     * @throws Throwable
     */
    public void testMgtLocal_RTS_ONLY() throws Throwable {
        dotestMgt(true);
    }

    /**
     * Tests the MBean functionality wrt jms server mgt
     * 
     * @throws Throwable
     */
    public void testMgtRemote() throws Throwable {
        dotestMgt(false);
    }
}
