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
import com.stc.jmsjca.util.ConnectionUrl;
import com.stc.jmsjca.util.Exc;
import com.stc.jmsjca.util.Logger;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.JMSException;

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
 * @version $Revision: 1.4 $
 */
public class RASunOneObjectFactory extends RAJMSObjectFactory implements
java.io.Serializable {
    private static Logger sLog = Logger.getLogger(RASunOneObjectFactory.class);
    private static Localizer LOCALE = Localizer.get(); 
        
    /**
     * @see com.stc.jmsjca.core.RAJMSObjectFactory#adjustDeliveryMode(int, boolean)
     */
    public int adjustDeliveryMode(int mode, boolean xa) {
        int newMode = mode;
        if (mode == RAJMSActivationSpec.DELIVERYCONCURRENCY_SERIAL) {
            newMode = RAJMSActivationSpec.DELIVERYCONCURRENCY_SYNC;
        }

//            if (mode != RAJMSActivationSpec.DELIVERYCONCURRENCY_SYNC) {
//            newMode = RAJMSActivationSpec.DELIVERYCONCURRENCY_SYNC;
//            sLog.warn("Current delivery mode ["
//                + RAJMSActivationSpec.DELIVERYCONCURRENCY_STRS[mode]
//                + "] not supported; switching to ["
//                + RAJMSActivationSpec.DELIVERYCONCURRENCY_STRS[newMode]
//                + "]");
//        }
        return newMode;
        }

    /**
     * @see com.stc.jmsjca.core.RAJMSObjectFactory#canCCEnlistInOnMessage()
     */
    public boolean canCCEnlistInOnMessage() {
        return false;
    }

    /**
     * Creates a provider specific UrlParser
     * 
     * @param s connectionURL to parse
     * @return parser
     */
    public ConnectionUrl createConnectionUrl(String s) {
        return new SunOneUrlParser(s);
    }
    
    /**
     * Checks the validity of the URL; adjusts the port number if necessary
     * 
     * @param aurl UrlParser
     * @throws javax.jms.JMSException on format failure
     * @return boolean true if the url specified url object was changed by this
     *         validation
     */
    public boolean validateAndAdjustURL(ConnectionUrl aurl) throws JMSException {
        SunOneUrlParser urlParser = (SunOneUrlParser) aurl;
        return urlParser.validate();
    }
    
    public interface BasicConnectionFactory {
        void setProperty(String name, String value);
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
                basicConnectionFactory = Class.forName("com.sun.messaging.QueueConnectionFactory").newInstance();
                break;
            case XConnectionRequestInfo.DOMAIN_QUEUE_XA:
                basicConnectionFactory = Class.forName("com.sun.messaging.XAQueueConnectionFactory").newInstance();
                break;
            case XConnectionRequestInfo.DOMAIN_TOPIC_NONXA:
                basicConnectionFactory = Class.forName("com.sun.messaging.TopicConnectionFactory").newInstance();
                break;
            case XConnectionRequestInfo.DOMAIN_TOPIC_XA:
                basicConnectionFactory = Class.forName("com.sun.messaging.XATopicConnectionFactory").newInstance();
                break;
            case XConnectionRequestInfo.DOMAIN_UNIFIED_NONXA:
                basicConnectionFactory = Class.forName("com.sun.messaging.ConnectionFactory").newInstance();
                break;
            case XConnectionRequestInfo.DOMAIN_UNIFIED_XA:
                basicConnectionFactory = Class.forName("com.sun.messaging.XAConnectionFactory").newInstance();
                break;
            default:
                throw new JMSException("Logic fault: invalid domain " + domain);
            }
        } catch (Exception e) {
            throw Exc.jmsExc(LOCALE.x("E600: Could not load or instantiate connection factory class: {0}", e), e);
        }
        
        urlParser.getQueryProperties(p);

//        BasicConnectionFactory b = (BasicConnectionFactory) Undep.create(
//            BasicConnectionFactory.class, basicConnectionFactory);

        Method setProperty;
        
        try {
            setProperty = basicConnectionFactory.getClass().getMethod("setProperty",
                new Class[] { String.class, String.class });
            setProperty.invoke(basicConnectionFactory,
                new Object[] {"imqAddressList", urlParser.getSunOneUrlSet()});
            setProperty.invoke(basicConnectionFactory,
                new Object[] {"imqConnectionFlowLimitEnabled", "true"});
//        b.setProperty("imqAddressList", urlParser.getSunOneUrlSet());
//        b.setProperty("imqConnectionFlowLimitEnabled", "true");
        } catch (Exception e) {
            throw Exc.jmsExc(LOCALE.x("E601: Failed to configure connection factory: {0}", e), e);
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
        Enumeration names = p.propertyNames();
        while (names.hasMoreElements()) {
            String name = (String) names.nextElement();
            if (name.startsWith("imq")) {
                String value = (String) p.getProperty(name);
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
                    sLog.warn(LOCALE.x("E603: ClientID is already set to [{0}]; "   
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
        String password = spec == null ? null : spec.getPassword();
        if (password == null) {
            password = ra.getPassword();
        }
        
        // Configure properties to send to MBean
        Properties connectionprops = new Properties();
        connectionprops.setProperty("imqAddressList", urlParser.getSunOneUrlAdminSet());            

        if (sLog.isDebugEnabled()) {
            sLog.debug("sjsmq administration AddressList: " + urlParser.getSunOneUrlAdminSet());
        }
        
        try {
            // Instantiate mbean
            Class c = Class.forName("com.stc.jmsmx.sjsmq.ExternalSJSMQMBean");
            Object mbean = c.newInstance();
            // Initialize this object using this method:
            Class[] signatures = {Properties.class, String.class, String.class };
            Object[] args = {connectionprops, username, password};
            Method method = c.getMethod("setConnectInfo", signatures);
            method.invoke(mbean, args);
            ret = mbean;
        } catch (Exception e) {
            throw Exc.jmsExc(LOCALE.x("E602: Error instantiating or configuring MBean for "
                + "external SJS MQ server management: {0}", e), e);
        }
        
        return ret;
    }

    /**
     * @see com.stc.jmsjca.core.RAJMSObjectFactory#getJMSServerType()
     */
    public String getJMSServerType() {
        return "SUNONE";
    }
}
