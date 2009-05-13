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
import com.stc.jmsjca.util.Logger;
import com.stc.jmsjca.util.Str;

import org.jdom.Namespace;

import java.io.File;
import java.net.URL;
import java.util.Properties;

import junit.framework.TestResult;

/**
 * @author fkieviet
 * @version $Revision: 1.11 $
 */
public abstract class EndToEndBase extends BaseTestCase implements BaseTestCase.JMSTestEnv {
    protected File mTestEar;
    protected File mTestEarOrg;
    protected String mTestEarName;
    protected String mContainerID;

    public final static String RAXML = "ratest.rar#/META-INF/ra.xml";
    public final static String RAXML1 = "ratest1.rar#/META-INF/ra.xml";
    public final static String EJBDD = "mdbtest.jar#/META-INF/ejb-jar.xml";
    public final static String MBEAN = "com.stc.jmsjca:name=ActivationMBean,type=ActivationMBean";
    public final static String RAMMBEAN = "com.stc.jmsjca:name=RAMBean,type=RAMBean";
    public final static String RAMMBEAN2 = "com.stc.jmsjca:name=RAMBean2,type=RAMBean2";

    public final static String RTS_ID = "rts";
    public final static String RTS_ALT_ID = "bare-rts";
    public final static String SJSAS_ID = "sjsas";
    public final static String GLASSFISH_ID = "glassfish";
    public final static String WL_ID = "wl";
    public final static String WAS_ID = "was";
    public final static String JBOSS_ID = "jboss";
    
    public final static String JMSMX = "jmsmx";

    
    /**
     * Represents the STCMS activation spec
     */
    public interface ActivationConfig {
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
    
    public static String CONTAINERID = "jmsjca.container.id";
    
    /**
     * Runs the test case except if a similarly named method prefixed with
     * skip_ or disabled_ exists.
     */
    @Override
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
     * @return the JMS implementation specific factory object
     */
    public abstract JMSProvider getJMSProvider();
    
    /**
     * Loads the deployment descriptors after modification; derived classes can override
     * this method to do more general customization of the dd
     * 
     * @return
     * @throws Exception
     */
    final public EmbeddedDescriptor getDD() throws Exception {
        EmbeddedDescriptor dd = new EmbeddedDescriptor(mTestEarOrg, mTestEar);
        ConnectorConfig cc = (ConnectorConfig) dd.new ResourceAdapter(RAXML).createConnector(ConnectorConfig.class);
        cc.setUserName(getJMSProvider().getUserName(mServerProperties));
        cc.setPassword(Str.pwencode(getJMSProvider().getPassword(mServerProperties)));
        cc.setMBeanObjectName(RAMMBEAN);

        cc = (ConnectorConfig) dd.new ResourceAdapter(RAXML1).createConnector(ConnectorConfig.class);
        cc.setUserName(getJMSProvider().getUserName(mServerProperties));
        cc.setPassword(getJMSProvider().getPassword(mServerProperties));
        cc.setMBeanObjectName(RAMMBEAN2);
        
        // Apply JMS Provider specific changes
        getJMSProvider().changeDD(dd, this);

        return dd;
    }
    
    /**
     * Returns the container id where the test will run in
     * 
     * @return rts, sjsas, ...
     */
    public String getContainerID() {
        if (mContainerID == null) {
            String containerid = System.getProperty(CONTAINERID, null);
            if (containerid == null) {
                containerid = GLASSFISH_ID;
                Logger.getLogger(this.getClass()).warnNoloc(
                    "System property [" + CONTAINERID + "] is not set; reverting to \"" + containerid + "\"");
            }
            mContainerID = containerid;
        }
        return mContainerID;
    }
    
    /**
     * Returns a container specific property
     * 
     * @param name name of the property
     * @param default_ default value
     * @return container property
     * @throws Exception 
     */
    public int getContainerProperty(String name, int default_) throws Exception {
        return Integer.parseInt(getServerProperties().getProperty("jmsjca." 
            + getContainerID() + "." + name, "" + default_));
    }
    
    /**
     * Creates a container based on the system property jmsjca. container. id
     * 
     * @return container
     */
    public Container createContainer() throws Exception {
        String containerid = getContainerID();
        
        Class<?> clz = null;
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
        container.setProperties(mServerProperties);
        
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

    public final Passthrough createPassthrough(Properties serverProperties) {
        return getJMSProvider().createPassthrough(serverProperties);
    }
    
    /**
     * Loads config
     *
     * @throws Exception
     */
    @Override
    public void setUp() throws Exception {
        super.setUp();
        
        // Get marker file, and from there the root directory
        URL markerURL = this.getClass().getClassLoader().getResource("rootmarker");
        if (markerURL == null) {
            throw new Exception("Ensure that the build directory is in the classpath. " +
            		"This directory should contain the file 'rootmarker'. " +
            		"This directory is used to locate the EAR files.");
        }
        File marker = new File(markerURL.getPath());
        File root = marker.getParentFile();
        String module = getJMSProvider().getProviderID();
        mTestEar = new File(root, "ra" + module + "/test/ratest-test.ear"); 
        mTestEarName = Container.getModuleName(mTestEar.getName());
        mTestEarOrg = new File(mTestEar.getAbsolutePath() + ".1");
        
        if (!mTestEarOrg.exists()) {
            throw new Exception("EAR file " + mTestEarOrg.getAbsolutePath() + " does not " +
            		"exist. Run the Ant script to generate the EAR file.");
        }
    }

    public static Namespace J2EENS = Namespace.getNamespace("http://java.sun.com/xml/ns/j2ee");

    /**
     * Represents relevant properties of the STCMS connector in ra.xml
     */
    public interface ConnectorConfig {
        void setUserName(String u) throws Exception;
        void setPassword(String p) throws Exception;
        void setConnectionURL(String u) throws Exception;
        String getConnectionURL() throws Exception;
        void setOptions(String o) throws Exception;
        String getOptions() throws Exception;
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
