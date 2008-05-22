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
import com.stc.jmsjca.core.Options;
import com.stc.jmsjca.test.core.Passthrough;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;

/**
 * Tests FIFO mode concurrency in stcms
 *
 * @author fkieviet
 * @version $Revision: 1.6 $
 */
public class FifoEar1 extends StcmsEndToEnd {
    
    /**
     * MBean interface to properties configuration
     */
    public interface PropMBean {
        Properties getConfiguration() throws Exception;
        void setConfiguration(Properties p) throws Exception;
        String FIFO = "STCMS.Queue.Mode";
        String NAME = "com.sun.appserv:type=messaging-server-config-mbean,jmsservertype=stcms,name=Sun_SeeBeyond_JMS_IQ_Manager";
    }
    
    /**
     * MBean interface to restarting
     */
    public interface RestartMBean {
        public void restart() throws Exception;
        String NAME = "com.sun.appserv:j2eeType=J2EEServer,name=server,category=runtime";
    }
    
    private static final String PROTCONC = "FIFO-Test-Queue.Read";
    private static final String PROTFULLSER = "FIFO-Test-Queue.Mode2";
    
    /**
     * Checks for the FIFO settings to be there in the stcms properties file; changes
     * these properties and restarts the server if necessary.
     * 
     * @throws Exception
     */
    private void checkFifoConfig() throws Exception {
        Container c = createContainer();
        PropMBean mb = (PropMBean) c.getMBeanProxy(PropMBean.NAME, PropMBean.class);
        Properties p = mb.getConfiguration();
        String f = p.getProperty(PropMBean.FIFO, "");
        System.out.println("Check property: [" + f + "]");
        Map config = parseFifoConf(f);
        if (config.get(PROTCONC) == null || config.get(PROTFULLSER) == null) {
            // Change config
            config.put(PROTCONC, new Integer(1));
            config.put(PROTFULLSER, new Integer(2));
            f = createFifoConfig(config);
            p.setProperty(PropMBean.FIFO, f);
            System.out.println("Setting property: " + f);
            mb.setConfiguration(p);
            
            // Need to restart
            RestartMBean rmb = (RestartMBean) c.getMBeanProxy(RestartMBean.NAME, RestartMBean.class);
            try {
                rmb.restart();
            } catch (Exception e) {
                System.out.println("Restart ex: " + e);
            }
            Thread.sleep(50000);
            
            for (;;) {
                try {
                    Thread.sleep(5000);
                    c = createContainer();
                    mb = (PropMBean) c.getMBeanProxy(PropMBean.NAME, PropMBean.class);
                    p = mb.getConfiguration();
                    System.out.println("Connection re-established");
                    break;
                } catch (Exception e) {
                    System.out.println("Connection not yet established");
                }
            }
        }
        c.close();
    }
    
    /**
     * Checks order and concurrency by sleeping sometimes (will guarantee 
     * "disordering" in case of concurrency
     * 
     * @throws Throwable
     */
    public void testProtectedConcurrentNoEx_RTS_ONLY() throws Throwable {
        checkFifoConfig();
        protectedConcurrentTest("sleepSometimes", 20, false, 1000);
    }

    /**
     * Checks order and concurrency by sleeping
     * 
     * @throws Throwable
     */
    public void testProtectedConcurrentConc_RTS_ONLY() throws Throwable {
        checkFifoConfig();
        protectedConcurrentTest("sleepABit", 20, true, 300);
    }

    /**
     * Checks for deadlock issues in case of rollback
     * 
     * @throws Throwable
     */
    public void testProtectedConcurrentEx_RTS_ONLY() throws Throwable {
        checkFifoConfig();
        protectedConcurrentTest("throwExceptionCMT", 1, true, 1000);
    }

    /**
     * Checks for deadlock issues in case of rollback
     * 
     * @throws Throwable
     */
    public void testSerialEx_RTS_ONLY() throws Throwable {
        checkFifoConfig();
        fullySerializedTest("throwExceptionCMT", 1000);
    }

    /**
     * Checks concurrency and order
     * 
     * @throws Throwable
     */
    public void testSerialNoEx_RTS_ONLY() throws Throwable {
        checkFifoConfig();
        fullySerializedTest("sleepSometimes", 1000);
    }

    /**
     * Checks concurrency and order
     * 
     * @throws Throwable
     */
    public void testSerialNoExConc_RTS_ONLY() throws Throwable {
        checkFifoConfig();
        fullySerializedTest("sleepABit", 50);
    }

    /**
     * Parses a Fifo configuration string
     * 
     * @param val
     * @return map: key=queue name; value=0,1,2 (Integer)
     * @throws Exception 
     */
    private Map parseFifoConf(String val) throws Exception {
        Map ret = new HashMap();
        
        if (!val.startsWith("{") || !val.endsWith("}")) {
            throw new Exception("Invalid value: [" + val + "]");
        }
        val = val.substring(1, val.length() - 1);
        
        String[] items = val.split(";");
        for (int i = 0; i < items.length; i++) {
            String item = items[i];
            if (item.length() == 0) {
                continue;
            }
            String[] parts = item.split(":");
            if (parts.length == 2) {
                String destname = parts[0];
                String mode = parts[1];
                ret.put(destname, new Integer(Integer.parseInt(mode)));
            }
        }
        return ret;
    }
    
    /**
     * Creates a configuration string of the form {Q1:1;Q2:2}
     * 
     * @param m Map key=destname; value=mode
     * @return
     */
    private String createFifoConfig(Map m) {
        StringBuffer ret = new StringBuffer();
        ret.append("{");
        int k = 0;
        for (Iterator iter = m.entrySet().iterator(); iter.hasNext();) {
            Map.Entry element = (Map.Entry) iter.next();
            if (k++ != 0) {
                ret.append(";");
            }
            ret.append(element.getKey() + ":" + element.getValue());
        }
        ret.append("}");
        return ret.toString();
    }
    
    /**
     * Checks protected concurrent
     * 1) concurrent processing
     * 2) order is maintained
     * 3) immune to exceptions
     * Note: exceptions can only be dealt with in sync mode
     */
    private void protectedConcurrentTest(String methodname, int nMDBs, boolean testconc, int nmsgs) throws Throwable {
        EmbeddedDescriptor dd = getDD();
        dd.findElementByText(EJBDD, "testQQXAXA").setText(methodname);
        StcmsActivation spec = (StcmsActivation) dd.new ActivationConfig(EJBDD,
        "mdbtest").createActivation(StcmsActivation.class);
        spec.setContextName("ProtConc" + methodname);
        spec.setDestination(PROTCONC);
        spec.setConnectionURL("stcms://?" + Options.In.OPTION_CONCURRENCYMODE + "=sync");
        spec.setEndpointPoolMaxSize("" + nMDBs);
        dd.update();

        Container c = createContainer();
        Passthrough p = createPassthrough(mServerProperties);
        p.setQueue1Name(PROTCONC);
        p.setStrictOrder(true);
        p.setNMessagesToSend(nmsgs);
        try {
            c.redeployModule(mTestEar.getAbsolutePath());
            p.passFromQ1ToQ2();
            
            if (testconc) {
                ActivationMBean act = (ActivationMBean) c.getMBeanProxy(MBEAN,
                    ActivationMBean.class);
                int nMDBsFound = act.xgetNHighestActiveEndpoints();
                if (nMDBsFound != nMDBs) {
                    throw new Exception("Concurrency was " + nMDBsFound + "; expected " + nMDBs);
                }
            }
            
            c.undeploy(mTestEarName);
            p.get(p.getQueue1Name()).assertEmpty();
        } finally {
            Container.safeClose(c);
            Passthrough.safeClose(p);
        }
    }

    /**
     * Checks fully serialized:
     * 1) order should be maintained
     * 2) (even) with CC, there should be one MDB active at any time
     * 3) immune to exceptions
     */
    private void fullySerializedTest(String methodname, int nmsgs) throws Throwable {
        EmbeddedDescriptor dd = getDD();
        dd.findElementByText(EJBDD, "testQQXAXA").setText(methodname);
        StcmsActivation spec = (StcmsActivation) dd.new ActivationConfig(EJBDD,
        "mdbtest").createActivation(StcmsActivation.class);
        spec.setContextName("ProtConc" + methodname);
        spec.setDestination(PROTFULLSER);
        dd.update();

        Container c = createContainer();
        Passthrough p = createPassthrough(mServerProperties);
        p.setQueue1Name(PROTFULLSER);
        p.setStrictOrder(true);
        p.setNMessagesToSend(nmsgs);
        try {
            c.redeployModule(mTestEar.getAbsolutePath());
            p.passFromQ1ToQ2();
            
            ActivationMBean act = (ActivationMBean) c.getMBeanProxy(MBEAN,
                ActivationMBean.class);
            int nMDBsFound = act.xgetNHighestActiveEndpoints();
            System.out.println(nMDBsFound + " MDBs");
            if (nMDBsFound != 1) {
                throw new Exception("Concurrency was " + nMDBsFound + "; expected " + 1);
            }
            
            c.undeploy(mTestEarName);
            p.get(p.getQueue1Name()).assertEmpty();
        } finally {
            Container.safeClose(c);
            Passthrough.safeClose(p);
        }
    }
}
