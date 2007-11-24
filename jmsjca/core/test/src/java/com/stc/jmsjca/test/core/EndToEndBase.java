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
import com.stc.jmsjca.util.Logger;
import com.stc.jmsjca.container.EmbeddedDescriptor;

import org.jdom.Namespace;

import javax.jms.Connection;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Properties;

import junit.framework.TestResult;

/**
 * Required:
 * test.server.properties = path to properties file containing server config
 * test.ear.path          = path to ear file to be tested
 *
 * @author fkieviet
 * @version $Revision: 1.5 $
 */
public abstract class EndToEndBase extends BaseTestCase {
    /**
     * Properties of the JMS server
     */
    protected Properties mServerProperties;

    /**
     * Properties of the Container
     */
    protected Properties mContainerProperties;
    
    protected File mTestEar;
    protected File mTestEarOrg;
    protected String mTestEarName;

    protected final static String RAXML = "ratest.rar#/META-INF/ra.xml";
    protected final static String RAXML1 = "ratest1.rar#/META-INF/ra.xml";
    protected final static String EJBDD = "mdbtest.jar#/META-INF/ejb-jar.xml";
    protected final static String MBEAN = "com.stc.jmsjca:name=ActivationMBean,type=ActivationMBean";
    protected final static String RAMMBEAN = "com.stc.jmsjca:name=RAMBean,type=RAMBean";
    protected final static String RAMMBEAN2 = "com.stc.jmsjca:name=RAMBean2,type=RAMBean2";

    public final static String RTS_ID = "rts";
    public final static String RTS_ALT_ID = "bare-rts";
    public final static String SJSAS_ID = "sjsas";
    public final static String GLASSFISH_ID = "glassfish";
    public final static String WL_ID = "wl";
    public final static String WAS_ID = "was";
    public final static String JBOSS_ID = "jboss";

    
    /**
     * Represents the STCMS activation spec
     */
    public interface StcmsActivation {
        public void setConcurrencyMode(String m);
        public void setContextName(String m);
        public void setDestination(String v);
        public void setDestinationType(String v);
        public void setSubscriptionDurability(String v);
        public void setSubscriptionName(String v);
        public void setClientId(String v);
        public void setConnectionURL(String url);
        public void setEndpointPoolMaxSize(String n);
        public void setRedeliveryHandling(String actions);
        public void setBatchSize(String batchSize);
        public void setHoldUntilAck(String hua);
        public void setMessageSelector(String s);
    }
    
    public static String CONTAINERID = "test.container.id";
    
    /**
     * Runs the test case except if a similarly named method prefixed with
     * skip_ or disabled_ exists.
     */
    public void run(TestResult result) {
        if (!getContainerID().equals(RTS_ID) && getName().indexOf("RTS_ONLY") > 0) {
            System.out.println("*** Skipping " + getName() + ": not an RTS container");
        } else if (!getContainerID().equals(WL_ID) && getName().indexOf("WL_ONLY") > 0) {
                System.out.println("*** Skipping " + getName() + ": not a WL container");
        } else {
            System.out.println("*** Now running test " + getName());
            super.run(result);
        }
    }

    
    /**
     * Loads the deployment descriptors after modification; derived classes can override
     * this method to do more general customization of the dd
     * 
     * @return
     * @throws Exception
     */
    public EmbeddedDescriptor getDD() throws Exception {
        EmbeddedDescriptor dd = new EmbeddedDescriptor(mTestEarOrg, mTestEar);
        StcmsConnector cc = (StcmsConnector) dd.new ResourceAdapter(RAXML).createConnector(StcmsConnector.class);
        cc.setUserName(mServerProperties.getProperty("admin.user"));
        cc.setPassword(mServerProperties.getProperty("admin.password"));
        cc.setMBeanObjectName(RAMMBEAN);

        cc = (StcmsConnector) dd.new ResourceAdapter(RAXML1).createConnector(StcmsConnector.class);
        cc.setUserName(mServerProperties.getProperty("admin.user"));
        cc.setPassword(mServerProperties.getProperty("admin.password"));
        cc.setMBeanObjectName(RAMMBEAN2);

        return dd;
    }
    
    private String mContainerID;
    
    /**
     * Returns the container id where the test will run in
     * 
     * @return rts, sjsas, ...
     */
    public String getContainerID() {
        if (mContainerID == null) {
            String containerid = System.getProperty(CONTAINERID, null);
            if (containerid == null) {
                Logger.getLogger(this.getClass()).warnNoloc(
                    "System property [" + CONTAINERID + "] is not set; reverting to rts");
                containerid = RTS_ID;
            }
            mContainerID = containerid;
        }
        return mContainerID;
    }
    
    /**
     * Creates a container based on the system property test.container.id
     * 
     * @return container
     */
    public Container createContainer() throws Exception {
        String containerid = getContainerID();
        
        Class clz = null;
        if (RTS_ID.equals(containerid) || RTS_ALT_ID.equals(containerid)) {
            clz = Class.forName("com.stc.jmsjca.container.rts.RTSContainer");
        } else if (WL_ID.equals(containerid)) {
            clz = Class.forName("com.stc.jmsjca.container.wl.WLContainer");
        } else if (JBOSS_ID.equals(containerid)) {
            clz = Class.forName("com.stc.jmsjca.container.jboss.JBossContainer");            
        } else if (WAS_ID.equals(containerid)) {
            clz = Class.forName("com.stc.jmsjca.container.was.WASContainer");            
        } else if (SJSAS_ID.equals(containerid)) {
            clz = Class.forName("com.stc.jmsjca.container.sjsas.SJSASContainer");
        } else if (GLASSFISH_ID.equals(containerid)) {
            clz = Class.forName("com.stc.jmsjca.container.glassfish.GlassFishContainer");
        } else {
            throw new Exception(
                "Cannot create container: unknown value for system-property ["
                    + CONTAINERID + "]: " + containerid);
        }
        
        Container container = (Container) clz.newInstance();
        container.setProperties(mContainerProperties);
        
        return container;
    }

    /**
     * Represents the interface of the MBean created by an activation
     */
    public interface ActivationMBean {
        void startService() throws Exception;
        void stopService() throws Exception;
        int xgetNMessages() throws Exception;
        boolean xgetSuspended() throws Exception;
        int xgetNActiveEndpoints() throws Exception;
        int xgetNTotalEndpoints() throws Exception;
        int xgetNHighestActiveEndpoints() throws Exception;
        String xgetStatus() throws Exception;
        String getJMSServerMBean() throws Exception;
        int echoPrimitives(int a, long b) throws Exception;
        String xgetStats() throws Exception;
        void resetStats() throws Exception;
    }
    
    public interface RAMBean {
        String getJMSServerMBean() throws Exception;
        String getJMSServerType() throws Exception;
    }

    /**
     * Tool function: ensures that the specified stream is closed
     *
     * @param stream to close; maybe null
     */
    public static void safeClose(InputStream stream) {
        if (stream != null) {
            try {
                stream.close();
            } catch (Exception ex) {
                // ignore
            }
        }
    }

    /**
     * Tool function: ensures that the specified stream is closed
     *
     * @param stream to close; maybe null
     */
    public static void safeClose(Connection c) {
        if (c != null) {
            try {
                c.close();
            } catch (Exception ex) {
                // ignore
            }
        }
    }

    public abstract Passthrough createPassthrough(Properties serverProperties);

    /**
     * Tool function: ensures that the specified stream is closed
     *
     * @param stream to close; maybe null
     */
    public static void safeClose(OutputStream stream) {
        if (stream != null) {
            try {
                stream.close();
            } catch (Exception ex) {
                // ignore
            }
        }
    }
    
    private Properties load(String systemPropertiesPropertyName) throws Exception {
        // The JMS server properties
        String propfile = System.getProperty(systemPropertiesPropertyName, null);

        if (propfile == null) {
            throw new Exception("Property [" + systemPropertiesPropertyName + "] not defined");
        }

        // Load test.server.properties
        InputStream in = null;
        try {
            Properties p = new Properties();
            in = new FileInputStream(propfile);
            p.load(in);
            return p;
        } finally {
            safeClose(in);
        }
    }
    
    /**
     * Loads config
     *
     * @throws Exception
     */
    public void setUp() throws Exception {
        // JMS Server properties
        final String jmsPropName = "test.server.properties";
        mServerProperties = load(jmsPropName);
        
        // Container properties
        final String containerPropName = "test.container.properties"; 
        if (System.getProperty(containerPropName) == null) {
            Logger.getLogger(this.getClass()).warnNoloc(
                "System property [" + containerPropName + "] is not set; reverting to ["
                    + jmsPropName + "]");
            mContainerProperties = mServerProperties;
        } else {
            mContainerProperties = load("test.container.properties");
        }
        
        // Set test constants wrt EAR file
        String testear = System.getProperty("test.ear.path", null);
        if (testear == null) {
            throw new Exception("test.ear.path not defined");
        }
        
        String containerid = getContainerID();
        
        if (JBOSS_ID.equals(containerid)) {
            mTestEar = new File(testear);
            mTestEarName = mTestEar.getAbsolutePath();
            mTestEarOrg = new File(mTestEar.getAbsolutePath() + ".1");
        } else {
            mTestEar = new File(testear);
            mTestEarName = Container.getModuleName(mTestEar.getName());
            mTestEarOrg = new File(mTestEar.getAbsolutePath() + ".1");            
        }
        
    }

    public static Namespace J2EENS = Namespace.getNamespace("http://java.sun.com/xml/ns/j2ee");

    /**
     * Represents relevant properties of the STCMS connector in ra.xml
     */
    public interface StcmsConnector {
        void setUserName(String u) throws Exception;
        void setPassword(String p) throws Exception;
        void setConnectionURL(String u) throws Exception;
        String getConnectionURL() throws Exception;
        void setOptions(String o) throws Exception;
        void setMBeanObjectName(String o) throws Exception;
    }

    /**
     * Represents relevant properties of the STCMS connector in ra.xml
     */
    public interface Outbound {
        void setUserName(String u) throws Exception;
        void setPassword(String p) throws Exception;
        void setConnectionURL(String u) throws Exception;
        void setOptions(String o) throws Exception;
        void setProducerPooling(boolean b) throws Exception;
    }

    /**
     * Provides access to the JMS server properties; they may be the same as the 
     * container properties in case of RTS
     * 
     * @return properties
     */
    public Properties getJmsServerProperties() {
        return mServerProperties;    
    }
}
