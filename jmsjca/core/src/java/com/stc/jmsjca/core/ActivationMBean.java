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
 * $RCSfile: ActivationMBean.java,v $
 * $Revision: 1.3 $
 * $Date: 2007-01-21 17:51:38 $
 *
 * Copyright 2003-2007 Sun Microsystems, Inc. All Rights Reserved.  
 */

package com.stc.jmsjca.core;

import com.stc.jmsjca.util.ConnectionUrl;
import com.stc.jmsjca.util.UrlParser;

import javax.jms.JMSException;
import javax.jms.Queue;
import javax.jms.Topic;

import java.util.Properties;

/**
 * Each activation can have an MBean associated with it; this MBean provides access to
 * statistical information as well as control over start() and stop() of the JMS
 * connection, and through that control over message delivery to the MDB.
 *
 * @author fkieviet
 * @version $Revision: 1.3 $
 */
public class ActivationMBean extends CommonMBean implements EmManagementInterface {
    //private static Logger sLog = Logger.getLogger(ActivationMBean.class);
    private Activation mActivation;

    /**
     * ActivationMBean
     *
     * @param a Activation
     */
    public ActivationMBean(Activation a) {
        super(a != null ? a.getObjectFactory() : null, "JMSJCA Activation MBean");
        mActivation = a;
    }
    
    /**
     * MBean getter
     * 
     * @return String
     */
    public String getStats() {
        return mActivation.getStats().toString();    
    }
    
    /**
     * Exposes this attribute as an MBean attribute
     * 
     * @return String
     */
    public String mbaStats() {
        return "Consistent stats dump";
    }

    /**
     * MBean getter
     * 
     * @return Integer
     */
    public Integer getNMessages() {
        return new Integer(mActivation.getStats().getNMessages());
    }
    
    /**
     * Exposes this attribute as an MBean attribute
     * 
     * @return String
     */
    public String mbaNMessages() {
        return "Number of messages delivered";
    }
    
    /**
     * MBean getter
     * 
     * @return Boolean
     */
    public Boolean getSuspended() {
        return new Boolean(mActivation.isStopped());
    }
    
    /**
     * MBean Attribute information (meta data)
     * 
     * @return attribute description
     */
    public String mbaSuspended() {
        return "Suspended state";
    }
    
    /**
     * MBean getter
     * 
     * @return String
     */
    public String getActivationSpec() {
        return mActivation.getActivationSpec().dumpConfiguration();
    }
    
    /**
     * MBean Attribute information (meta data)
     * 
     * @return attribute description
     */
    public String mbaActivationSpec() {
        return "Printout of the activation spec used";
    }
    
    /**
     * MBean getter
     * 
     * @return String
     */
    public String getResourceAdapter() {
        return mActivation.getRA().dumpConfiguration();
    }
    
    /**
     * MBean Attribute information (meta data)
     * 
     * @return attribute description
     */
    public String mbaResourceAdapter() { 
        return "Printout of the resource adapter properties";
    };
    
    /**
     * MBean getter
     * 
     * @return Integer
     */
    public Integer getNActiveEndpoints() {
        return new Integer(mActivation.getStats().getNActiveEndpoints());
    }
    
    /**
     * MBean Attribute information (meta data)
     * 
     * @return attribute description
     */
    public String mbaNActiveEndpoints() {
        return "Number of active endpoints";
    }
    
    /**
     * MBean getter
     * 
     * @return Integer
     */
    public Integer getNTotalEndpoints() {
        return new Integer(mActivation.getStats().getNTotalEndpoints());
    }
    
    /**
     * MBean Attribute information (meta data)
     * 
     * @return attribute description
     */
    public String mbaNTotalEndpoints() {
        return "Total number of endpoints";
    }
    
    /**
     * MBean getter
     * 
     * @return Integer
     */
    public Integer getNHighestActiveEndpoints() {
        return new Integer(mActivation.getStats().getNHighestEndpoints());
    }
    
    /**
     * MBean Attribute information (meta data)
     * 
     * @return attribute description
     */
    public String mbaNHighestActiveEndpoints() {
        return "Highest number of active endpoints reached";
    }
    
    /**
     * Number of endpoints in the delivery
     * 
     * @return int
     */
    public Integer getNConfiguredEndpoints() {
        return new Integer(mActivation.dumpNumberConfiguredEndpoints());
    }
    
    /**
     * MBean Attribute information (meta data)
     * 
     * @return attribute description
     */
    public String mbaNConfiguredEndpoints() {
        return "The max number of endpoints the Delivery Object will use";
    }
    
    /**
     * Returns a string representation of the Delivery Object
     * 
     * @return String
     */
    public String getDeliveryMode() {
        return mActivation.dumpDelivery();
    }
    
    /**
     * MBean Attribute information (meta data)
     * 
     * @return attribute description
     */
    public String mbaDeliveryMode() {
        return "String representation of the Delivery Object";
    }
    
    /**
     * Resets the statistics
     */
    public void resetStats() {
        mActivation.getStats().reset();
    }
    
    /**
     * Meta data
     * 
     * @return operation description
     */
    public String[] mbmresetStats() {
        return new String[] {"Resets the statistics"};
    }
    
    /**
     * Stops the service -- required interface by EM
     * 
     * @throws JMSException propagated
     */
    public void stopService() throws JMSException {
        mActivation.stop();
    }
    
    /**
     * MBean Method information (meta data)
     * 
     * @return method description
     */
    public String[] mbmstopService() {
        return new String[] {
            "Suspends message delivery"
        };
    }
    
    /**
     * Starts the service -- required interface by EM
     * 
     * @throws Exception propagated
     */
    public void startService() throws Exception {
        mActivation.start();
    }
    
    /**
     * MBean Method information (meta data)
     * 
     * @return method description
     */
    public String[] mbmstartService() {
        return new String[] {
            "Unsuspends message delivery"
        };
    }
    
    /**
     * For testing only
     * 
     * @param a an int
     * @param b long
     * @return sum of a and b
     */
    public int echoPrimitives(int a, long b) {
        return (int) (a + b);
    }
    
    /**
     * MBean Method information (meta data)
     * 
     * @param a an int
     * @param b long
     * @return method description
     */
    public String[] mbmechoPrimitives(int a, long b) {
        return new String[] {"For testing: returns the sum of (a) and (b)"
            , "a primitive int (a)", "a primitive long (b)"};
    }
    
    /**
     * @see com.stc.jmsjca.core.EmManagementInterface#start()
     */
    public String[] mbmstart() {
        return new String[] {"Required management method for the EmManagementInterface; " 
            + "starts message delivery if message delivery was stopped" };
    }

    /**
     * @see com.stc.jmsjca.core.EmManagementInterface#start()
     */
    public void start() throws Exception {
        mActivation.start();
    }

    /**
     * @see com.stc.jmsjca.core.EmManagementInterface#restart()
     */
    public String[] mbmrestart() {
        return new String[] {"Required management method for the EmManagementInterface; " 
            + "stops message delivery and then restarts message delivery again" };
    }

    /**
     * @see com.stc.jmsjca.core.EmManagementInterface#restart()
     */
    public void restart() throws Exception {
        mActivation.stop();
        mActivation.start();
    }

    /**
     * @see com.stc.jmsjca.core.EmManagementInterface#stop()
     */
    public String[] mbmstop() {
        return new String[] {"Required management method for the EmManagementInterface; " 
            + "stops message delivery." };
    }

    /**
     * @see com.stc.jmsjca.core.EmManagementInterface#stop()
     */
    public void stop() throws Exception {
        mActivation.stop();
    }
    
    /**
     * Status
     */
    public static final String DISCONNECTED = "Down";

    /**
     * Status
     */
    public static final String CONNECTED = "Up";

    /**
     * Status
     */
    public static final String CONNECTING = "Connecting";

    /**
     * Status
     */
    public static final String DISCONNECTING = "Disconnecting";

    /**
     * @see com.stc.jmsjca.core.EmManagementInterface#getStatus()
     */
    public String[] mbmgetStatus() {
        return new String[] {"Required management method for the EmManagementInterface; " 
            + "returns the current status. Possible values are: "
            + DISCONNECTED + " (the connector is not connected to the server); "
            + CONNECTED + " (the connector is connected to the server and can " 
            + "receive messages); "
            + CONNECTING + " (the connector is trying to connect to the server, " 
            + "this may take several seconds/minutes, it may take forever if " 
            + "the connector cannot connect to the server at all); "
            + DISCONNECTING + " (indicates that the connector is closing down" 
            + " connections to the server and is waiting for all outstanding "
            + " work to complete)"
        };
    }

    /**
     * @see com.stc.jmsjca.core.EmManagementInterface#getStatus()
     */
    public String getStatus() throws Exception {
        int state = mActivation.getState();
        if (state == Activation.DISCONNECTED) {
            return DISCONNECTED;
        } else if (state == Activation.DISCONNECTING) {
            return DISCONNECTING;
        } else if (state == Activation.CONNECTED) {
            return CONNECTED;
        } else if (state == Activation.CONNECTING) {
            return CONNECTING;
        } else {
            return "UNKNOWN";
        }
    }
    
    private static final String TYPE = "destination.type";
    private static final String NAME = "destination.name";
    private static final String SUBNAME = "subscriber.name";
    private static final String DURABILITY = "subscriber.durability";
    private static final String CONCURRENCY = "concurrency";
    private static final String SELECTOR = "selector";
    private static final String URL = "url";
    private static final String HOST = "server";
    private static final String PORT = "port";

    /**
     * @see com.stc.jmsjca.core.EmManagementInterface#getProperties()
     */
    public String[] mbmgetProperties() {
        return new String[] {"Required management method for the EmManagementInterface; " 
            + "returns properties set describing the activation spec. Property names are:"
            + TYPE + " = either " + Queue.class.getName() + " or " + Topic.class.getName() + "; "
            + NAME + " = name of queue or topic receiving messages from; "
            + SUBNAME + " = subscriber name; " 
            + DURABILITY + " = either " + RAJMSActivationSpec.DURABLE + " or " + RAJMSActivationSpec.NONDURABLE + "; "
            + SELECTOR + " = selector; "
            + URL + " = url of server connected to; "
            + HOST + " = hostname of server connected to (may be absent for URLs that do not describe a server/url); "
            + PORT + " = port at server connected to; "};
    }
    
    private void put(Properties p, String key, String value) {
        if (value != null && value.length() != 0) {
            p.put(key, value);
        }
    }

    /**
     * @see com.stc.jmsjca.core.EmManagementInterface#getProperties()
     */
    public Properties getProperties() {
        Properties ret = new Properties();
        put(ret, TYPE, mActivation.getActivationSpec().getDestinationType());
        put(ret, NAME, mActivation.getActivationSpec().getDestination());
        put(ret, SUBNAME, mActivation.getActivationSpec().getSubscriptionName());
        put(ret, DURABILITY, mActivation.getActivationSpec().getSubscriptionDurability());
        put(ret, CONCURRENCY, 
            RAJMSActivationSpec.DELIVERYCONCURRENCY_STRS[mActivation.getDeliveryMode()]);
        put(ret, URL, mActivation.getActivationSpec().getConnectionURL());
        put(ret, SELECTOR, mActivation.getActivationSpec().getMessageSelector());
        
        // Obtain server/port; this will only work for some message server 
        // implementations, i.e. those that use a simple URLParser. Wave uses a compound
        // URL and this will not work.
        try {
            ConnectionUrl url = mActivation.getObjectFactory().getProperties(
                new Properties(), mActivation.getRA(), mActivation.getActivationSpec(), 
                null, null);
            if (url instanceof UrlParser) {
                UrlParser parser = (UrlParser) url;
                put(ret, HOST, parser.getHost());
                put(ret, PORT, Integer.toString(parser.getPort()));
            }
        } catch (JMSException e) {
            // ignore
        }
        return ret;
    }

    /**
     * @see com.stc.jmsjca.core.EmManagementInterface#isStartable()
     */
    public String[] mbmisStartable() {
        return new String[] {"Required management method for the EmManagementInterface; " 
            + "will return false for this MBean" };
    }

    /**
     * @see com.stc.jmsjca.core.EmManagementInterface#isStartable()
     */
    public Boolean isStartable() {
        return Boolean.TRUE;
    }

    /**
     * @see com.stc.jmsjca.core.EmManagementInterface#isRestartable()
     */
    public String[] mbmisRestartable() {
        return new String[] {"Required management method for the EmManagementInterface; " 
            + "returns false for this MBean." };
    }

    /**
     * @see com.stc.jmsjca.core.EmManagementInterface#isRestartable()
     */
    public Boolean isRestartable() {
        return Boolean.TRUE;
    }

    /**
     * @see com.stc.jmsjca.core.EmManagementInterface#isStoppable()
     */
    public String[] mbmisStoppable() {
        return new String[] {"Required management method for the EmManagementInterface; " 
            + "returns false for this mbean" };
    }

    /**
     * @see com.stc.jmsjca.core.EmManagementInterface#isStoppable()
     */
    public Boolean isStoppable() {
        return Boolean.TRUE;
    }

    /**
     * @see com.stc.jmsjca.core.CommonMBean#main(java.lang.String[])
     */
    public static void main(String[] args) {
        try {
            new ActivationMBean(null).mbeanTest();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
