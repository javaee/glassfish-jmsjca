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

package com.stc.jmsjca.sunone;

import com.stc.jmsjca.core.RAJMSActivationSpec;
import com.stc.jmsjca.core.RAJMSObjectFactory;
import com.stc.jmsjca.core.RAJMSResourceAdapter;
import com.stc.jmsjca.core.XConnectionRequestInfo;
import com.stc.jmsjca.core.XManagedConnectionFactory;
import com.stc.jmsjca.util.ClassLoaderHelper;
import com.stc.jmsjca.util.ConnectionUrl;
import com.stc.jmsjca.util.Exc;
import com.stc.jmsjca.util.Logger;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.JMSException;
import javax.management.MBeanServer;
import javax.management.ObjectName;

import java.lang.reflect.Method;
import java.util.Enumeration;
import java.util.Properties;

/**
 * Encapsulates most of the specific traits of the Wave message server.
 * Format of the URL: see WaveUrlParser
 * The ConnectionURL is parsed, all properties are collected (left to right)
 * and the urls are reconstructed and passed to Wave.
 * 
 * @author misc
 * @version $Revision: 1.15 $
 */
public class RASunOneObjectFactory extends RAJMSObjectFactory implements java.io.Serializable {
    private static Logger sLog = Logger.getLogger(RASunOneObjectFactory.class);
    private static final Localizer LOCALE = Localizer.get(); 
        
    /**
     * @see com.stc.jmsjca.core.RAJMSObjectFactory#adjustDeliveryMode(int, boolean)
     */
    @Override
    public int adjustDeliveryMode(int mode, boolean xa) {
        //return super.adjustDeliveryMode(mode, xa);
        // Sun JMQ 3.6, 3.7 and 3.7 UR1 which JCAPS 5.1.x support have some problem with 
        // serial mode, basically serial mode is asynchronous messaging delivery mode, 
        // the session delivers a message to messagelistener's onMessage. But it is not 
        // clear that if tx starts before or after a messages is deliveried to onMessage method.
        // Based on JCA 1.5, the JCA expects tx starts after a messages is deliveried to onMessage.
        // Because of this un-clearness, sync mode is used instead serial mode in JCAPS 5.1.x.  
        // JCAPS 6 supports AS 9.1 and Sun JMQ 4.1, the problem could be gone. A stress test will 
        // be applied in this case. If there is a problem, can work with Sun JMQ team to fix in a corect way. 
        int newMode = mode;
        if (mode == RAJMSActivationSpec.DELIVERYCONCURRENCY_SERIAL) {
            newMode = RAJMSActivationSpec.DELIVERYCONCURRENCY_SYNC;
        }
        return newMode;        
    }
    
    /**
     * Creates a provider specific UrlParser
     * 
     * @param s connectionURL to parse
     * @return parser
     */
    @Override
    public ConnectionUrl createConnectionUrl(String s) {
        return new SunOneUrlParser(s);
    }
    
    private String getInternalKey(String hostOrUsername) {
        if (hostOrUsername != null 
            && hostOrUsername.startsWith("(") 
            && hostOrUsername.endsWith(")") 
            && hostOrUsername.length() > 2) {
            String key = hostOrUsername.substring(1, hostOrUsername.length() - 1);
            return key;
        }
        return null;
    }
    
    private static final class ConnectionValues {
        private String host;
        private String port;
        private String username;
        private String password;
        
        private ConnectionValues() {
        }
    }

    private ConnectionValues getConnectionValues(String key) throws Exception {
        Class<?> c = Class.forName("com.sun.enterprise.admin.common.MBeanServerFactory");
        Method m = c.getMethod("getMBeanServer", new Class[] {});
        MBeanServer mBeanServer = (MBeanServer) m.invoke(null, new Object[0]);
        ObjectName objName = new ObjectName("com.sun.appserv:type=jms-host,name=" 
            + key + ",config=server-config,category=config");
        
        ConnectionValues ret = new ConnectionValues();
        
        ret.host = (String) mBeanServer.getAttribute(objName, "host");   
        ret.port = (String) mBeanServer.getAttribute(objName, "port");
        ret.password = (String) mBeanServer.getAttribute(objName, "admin-password");
        ret.username = (String) mBeanServer.getAttribute(objName, "admin-user-name");
        
        return ret;
    }


    /**
     * Checks the validity of the URL; adjusts the port number if necessary
     * 
     * @param aurl UrlParser
     * @throws javax.jms.JMSException on format failure
     * @return boolean true if the url specified url object was changed by this
     *         validation
     */
    @Override
    public boolean validateAndAdjustURL(ConnectionUrl aurl) throws JMSException {
        SunOneUrlParser urlParser = (SunOneUrlParser) aurl;
        urlParser.validate();
        
        boolean hasChanged = false;
        
        // Adjust for (host) notation
        SunOneConnectionUrl[] urls = urlParser.getConnectionUrls();
        for (int i = 0; i < urls.length; i++) {
            SunOneConnectionUrl url = urls[i];
            if (url.getProtocol().equals(SunOneUrlParser.PROT_MQ)) {
                String host = url.getHost();
                String key = getInternalKey(host);
                if (key != null) {
                    ConnectionValues v;
                    try {
                        v = getConnectionValues(key);
                    } catch (Exception e) {
                        throw Exc.jmsExc(LOCALE.x("E307: Could not obtain connection info " 
                            + "for {0} (from [{1}]): {2}", key, url, e));
                    }
                    url.setHost(v.host);
                    url.setPort(Integer.parseInt(v.port));
                    hasChanged = true;
                }
            }
        }
        
        return hasChanged;
    }
    
    /**
     * @see com.stc.jmsjca.core.RAJMSObjectFactory#createConnection(java.lang.Object, int, 
     * com.stc.jmsjca.core.RAJMSActivationSpec, com.stc.jmsjca.core.RAJMSResourceAdapter, 
     * java.lang.String, java.lang.String)
     */
    @Override
    public Connection createConnection(Object fact, int domain,
        RAJMSActivationSpec activationSpec, RAJMSResourceAdapter ra, String username,
        String password) throws JMSException {
        
        String key = getInternalKey(username);
        if (key != null) {
            ConnectionValues v;
            try {
                v = getConnectionValues(key);
            } catch (Exception e) {
                throw Exc.jmsExc(LOCALE.x("E307: Could not obtain connection info " 
                    + "for {0} (from [{1}]): {2}", key, username, e));
            }
            username = v.username;
            password = v.password;
        }
        
        return super.createConnection(fact, domain, activationSpec, ra, username, password);
    }    

    /**
     * createConnectionFactory
     * 
     * @param domain boolean
     * @param resourceAdapter boolean
     * @param activationSpec RAJMSActivationSpec
     * @param fact RAJMSResourceAdapter
     * @param overrideUrl override URL: don't use URL from RA, CF, or activation
     *            spec (may be null)
     * @return ConnectionFactory
     * @throws JMSException failure
     */
    @Override
    public ConnectionFactory createConnectionFactory(int domain,
        RAJMSResourceAdapter resourceAdapter, RAJMSActivationSpec activationSpec,
        XManagedConnectionFactory fact, String overrideUrl) throws JMSException {
    
        Object basicConnectionFactory;
        
        // Get the connection properties
        Properties p = new Properties();
        SunOneUrlParser urlParser = (SunOneUrlParser) getProperties(p, resourceAdapter, activationSpec, fact, overrideUrl);
    
        try {
            switch (domain) {
            case XConnectionRequestInfo.DOMAIN_QUEUE_NONXA:
                basicConnectionFactory = ClassLoaderHelper.loadClass("com.sun.messaging.QueueConnectionFactory").newInstance();
                break;
            case XConnectionRequestInfo.DOMAIN_QUEUE_XA:
                basicConnectionFactory = ClassLoaderHelper.loadClass("com.sun.messaging.XAQueueConnectionFactory").newInstance();
                break;
            case XConnectionRequestInfo.DOMAIN_TOPIC_NONXA:
                basicConnectionFactory = ClassLoaderHelper.loadClass("com.sun.messaging.TopicConnectionFactory").newInstance();
                break;
            case XConnectionRequestInfo.DOMAIN_TOPIC_XA:
                basicConnectionFactory = ClassLoaderHelper.loadClass("com.sun.messaging.XATopicConnectionFactory").newInstance();
                break;
            case XConnectionRequestInfo.DOMAIN_UNIFIED_NONXA:
                basicConnectionFactory = ClassLoaderHelper.loadClass("com.sun.messaging.ConnectionFactory").newInstance();
                break;
            case XConnectionRequestInfo.DOMAIN_UNIFIED_XA:
                basicConnectionFactory = ClassLoaderHelper.loadClass("com.sun.messaging.XAConnectionFactory").newInstance();
                break;
            default:
                throw Exc.jmsExc(LOCALE.x("E304: Logic fault: invalid domain {0}", Integer.toString(domain)));
            }
        } catch (Exception e) {
            throw Exc.jmsExc(LOCALE.x("E300: Could not load or instantiate connection factory class: {0}", e), e);
        }
        
        urlParser.getQueryProperties(p);

//        BasicConnectionFactory b = (BasicConnectionFactory) Undep.create(
//            BasicConnectionFactory.class, basicConnectionFactory);

        Method setProperty;
        
        try {
            setProperty = basicConnectionFactory.getClass().getMethod("setProperty",
                new Class[] {String.class, String.class });
            setProperty.invoke(basicConnectionFactory,
                new Object[] {"imqAddressList", urlParser.getSunOneUrlSet()});
            setProperty.invoke(basicConnectionFactory,
                new Object[] {"imqConnectionFlowLimitEnabled", "true"});
//        b.setProperty("imqAddressList", urlParser.getSunOneUrlSet());
//        b.setProperty("imqConnectionFlowLimitEnabled", "true");
        } catch (Exception e) {
            throw Exc.jmsExc(LOCALE.x("E301: Failed to configure connection factory: {0}", e), e);
        } 
        

        if (sLog.isDebugEnabled()) {
            sLog.debug("sjsmq Normal AddressList: " + urlParser.getSunOneUrlSet());
        }
                
        // Connection Factory Properties
        // imqAddressListBehavior
        // imqAdressListIterations
        // imqPingInterval
        // imqReconnectiEnabled
        // imqReconnectAttempts
        // imqReconnectInterval
        // imqSSLIsHostTrusted
        Enumeration<?> names = p.propertyNames();
        while (names.hasMoreElements()) {
            String name = (String) names.nextElement();
            if (name.startsWith("imq")) {
                String value = p.getProperty(name);
                try {
                    setProperty.invoke(basicConnectionFactory,
                        new Object[] {name, value});
                } catch (Exception e) {
                    throw Exc.jmsExc(LOCALE.x("Failed to configure connection factory: {0}", e), e);
                } 
//                b.setProperty(name, value);
            }
        }
        return (javax.jms.ConnectionFactory) basicConnectionFactory;
    }
    
    /**
     * Returns true if the specified string may be a recognised URL
     * 
     * @param url String
     * @return true if may be URL
     */
    @Override
    public boolean isUrl(String url) {
        if (url != null && url.length() > 0) {
            for (int i = 0; i < SunOneUrlParser.URL_PREFIXES.length; i++) {
                if (url.startsWith(SunOneUrlParser.URL_PREFIXES[i])) {
                    return true;
                }
            }
        }
        return false;
    }
    
    /**
     * Sets the clientID for durable subscribers if not already set. If not set in the 
     * spec, it will be set to CLIENTID-<durable name> since Wave always requires a
     * clientID to be set for durable subscribers
     * 
     * @param connection connection
     * @param isTopic isTopic
     * @param spec activation spec
     * @param ra ra
     * @throws JMSException on failure
     */
    @Override
    public void setClientID(Connection connection, boolean isTopic,
        RAJMSActivationSpec spec, RAJMSResourceAdapter ra) throws JMSException {
        if (isTopic && RAJMSActivationSpec.DURABLE.equals(spec.getSubscriptionDurability())) {
            // Ensure a clientID will be set
            String newClientId = spec.getClientId();
            if (newClientId == null || newClientId.length() == 0) {
                newClientId = "CLIENTID-" + spec.getSubscriptionName();
            }
            
            String currentClientId = connection.getClientID();
            if (currentClientId == null || currentClientId.length() == 0) {
                // Set it
                setClientID(connection, newClientId);
            } else {
                if (newClientId.equals(currentClientId)) {
                    // ok: already set
                } else {
                    sLog.warn(LOCALE.x("E303: ClientID is already set to [{0}]; "   
                        + "cannot set to [{1}] as required in "
                        + "activationspec [{3}]", currentClientId, newClientId, spec)); 
                }
            }
        }
    }
    
    /**
     * @see com.stc.jmsjca.core.RAJMSObjectFactory#getServerMgtMBean(
     * com.stc.jmsjca.core.RAJMSResourceAdapter,
     * com.stc.jmsjca.core.RAJMSActivationSpec)
     */
    @Override
    public Object getServerMgtMBean(RAJMSResourceAdapter ra, RAJMSActivationSpec spec)
    throws JMSException {
        Object ret = null;
        
        // Determine URL -> connection properties
        Properties p = new Properties();
        SunOneUrlParser urlParser = (SunOneUrlParser) getProperties(p, ra, spec, null, null);
        validateAndAdjustURL(urlParser);
        
        // Determine username and password
        String username = spec == null ? null : spec.getUserName();
        if (username == null) {
            username = ra.getUserName();
        }
        String password = spec == null ? null : spec.getClearTextPassword();
        if (password == null) {
            password = ra.getClearTextPassword();
        }
        
        // Configure properties to send to MBean
        Properties connectionprops = new Properties();
        connectionprops.setProperty("imqAddressList", urlParser.getSunOneUrlAdminSet());            

        if (sLog.isDebugEnabled()) {
            sLog.debug("sjsmq administration AddressList: " + urlParser.getSunOneUrlAdminSet());
        }
        
        try {
            // Instantiate mbean
            Class<?> c = Class.forName("com.stc.jmsmx.sjsmq.ExternalSJSMQMBean");
            Object mbean = c.newInstance();
            // Initialize this object using this method:
            Class<?>[] signatures = {Properties.class, String.class, String.class };
            Object[] args = {connectionprops, username, password};
            Method method = c.getMethod("setConnectInfo", signatures);
            method.invoke(mbean, args);
            ret = mbean;
        } catch (ClassNotFoundException e) {
            sLog.debug(LOCALE.x("E302: Error instantiating or configuring MBean for "
                + "external SJS MQ server management: {0}", e));
        } catch (Exception e) {
            sLog.info(LOCALE.x("E302: Error instantiating or configuring MBean for "
                + "external SJS MQ server management: {0}", e));
        }
        
        return ret;
    }

    /**
     * @see com.stc.jmsjca.core.RAJMSObjectFactory#getJMSServerType()
     */
    @Override
    public String getJMSServerType() {
        return "SUNONE";
    }
    
    @Override
    public RAJMSActivationSpec createActivationSpec() {
        return new RASunOneActivationSpec();
    }

    /**
     * @see com.stc.jmsjca.core.RAJMSObjectFactory#shouldUseProducerPooling()
     */
    @Override
    public boolean shouldUseProducerPooling() {
        // Significant performance difference when using pooling
        return true;
    }

}
