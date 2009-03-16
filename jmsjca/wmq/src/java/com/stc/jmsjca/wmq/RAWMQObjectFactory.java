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

package com.stc.jmsjca.wmq;

import com.stc.jmsjca.core.RAJMSActivationSpec;
import com.stc.jmsjca.core.RAJMSObjectFactory;
import com.stc.jmsjca.core.RAJMSResourceAdapter;
import com.stc.jmsjca.core.XConnectionRequestInfo;
import com.stc.jmsjca.core.XManagedConnectionFactory;
import com.stc.jmsjca.util.ClassLoaderHelper;
import com.stc.jmsjca.util.ConnectionUrl;
import com.stc.jmsjca.util.Exc;
import com.stc.jmsjca.util.Logger;
import com.stc.jmsjca.util.Str;
import com.stc.jmsjca.util.UrlParser;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.JMSException;
import javax.jms.Session;
import javax.jms.XASession;
import javax.transaction.xa.XAResource;

import java.util.Properties;

/**
 * Encapsulates most of the specific traits of the Wave message server.
 * ConnectionURL: wmq://host:port
 * 
 * @version $Revision: 1.9 $
 * @author cye
 */
public class RAWMQObjectFactory extends RAJMSObjectFactory implements java.io.Serializable {
    private static Logger sLog = Logger.getLogger(RAWMQObjectFactory.class);
    
    /**
     * Protocol
     */
    private static final String PROTOCOL = "wmq";
    
    /**
     * SSL Protocol
     */
    private static final String SPROTOCOL = "wmqs";
    
    /**
     * Port
     */    
    private static final String PORT = "Port";
    
    /**
     * Hostname
     */    
    private static final String HOSTNAME = "HostName";

    /**
     * WebSphere MQ Queue Manager Name
     */        
    private static final String QUEUEMANAGER = "QueueManager";

    /**
     * WebSphere MQ transport type
     */            
    private static final String TRANSPORTTYPE = "TransportType";
    
    /**
     * WebSphere MQ Channel
     */            
    private static final String CHANNEL = "Channel";
 
    /**
     * WebSphere MQ Client Id
     */            
    private static final String CLIENTID = "ClientId";
    
    /**
     * WebSphere MQ Default Client Id
     */            
    private static final String DEFAULT_CLIENTID = "SeeBeyond RAWMQ";
        
    /**
     * WebSphere MQ Default Transport Type
     */            
    private static final String DEFAULT_TRANSPORTTYPE = "JMSC_MQJMS_TP_CLIENT_MQ_TCPIP";
    
    /**
     * WebSphere MQ Server port 
     */                
    private static final String PORTPROPERTY = "WMQ.Server.Port";
    
    /**
     * WebSphere MQ Server hostname 
     */                
    private static final String HOSTPROPERTY = "WMQ.Server.Host";
 
    /*
     * Connection Protocol Url Prefixes
     */
    private static final String[] URL_PREFIXES = new String[] {
            PROTOCOL + "://",
            SPROTOCOL + "://",
        };
    
    /*
     * Connection Transport Types
     */
    private static final String[] TRANSPORT_TYPES = new String[] {
            "JMSC.MQJMS_TP_BINDINGS_MQ",
            "JMSC_MQJMS_TP_CLIENT_MQ_TCPIP",
        };

    private static Localizer LOCALE = Localizer.get();    
    
    /**
     * Checks the validity of the URL; adjusts the port number if necessary
     *  using system properties
     * @param connectionUrl ConnectionUrl
     * @throws JMSException on format failure
     * @return boolean true if the url specified url object was changed by this
     *         validation
     */
    public boolean validateAndAdjustURL(ConnectionUrl connectionUrl) throws JMSException {
        boolean hasChanged = false;
        UrlParser url = (UrlParser) connectionUrl;

        // protocol
        if (!PROTOCOL.equals(url.getProtocol())) {
            throw Exc.jmsExc(LOCALE.x("E306: Invalid protocol [{0}]:"
                + " should be ''{1}''",  url.getProtocol(), PROTOCOL));
        }

        // Check port
        int port = url.getPort();
        if (port <= 0) {
            String s = System.getProperty(PORTPROPERTY);
            if (s != null) {
                port = Integer.parseInt(s);
                url.setPort(port);
                hasChanged = true;
            } else {
                throw new JMSException("No port specified in URL [" + url
                    + "], and also not available in System property [" + PORTPROPERTY
                    + "]");
            }
        }

        // Check host
        String host = url.getHost();
        if ("".equals(host)) {
            String s = System.getProperty(HOSTPROPERTY);
            if (s != null) {
                url.setHost(s);                
            } else {
                url.setHost("localhost");
            }
            hasChanged = true;
        }
        
        return hasChanged;
    }

    /**
     * @see com.stc.jmsjca.core.RAJMSObjectFactory#adjustDeliveryMode(int, boolean)
     */
    public int adjustDeliveryMode(int mode, boolean xa) {
        int newMode = mode;
        if (xa) {
            if (mode != RAJMSActivationSpec.DELIVERYCONCURRENCY_SYNC) {
                newMode = RAJMSActivationSpec.DELIVERYCONCURRENCY_SYNC;
                sLog.warn(LOCALE.x("E820: Current delivery mode {0} not supported; "
                    + " not supported; switching to {1}", 
                    RAJMSActivationSpec.DELIVERYCONCURRENCY_STRS[mode],
                    RAJMSActivationSpec.DELIVERYCONCURRENCY_STRS[newMode]));
            }
        }
        return newMode;
    }
    
    /**
     * 
     * @param hostStr String
     * @return String 
     */
    public String getQueueManager(String hostStr) {

        if (!Character.isLetter(hostStr.charAt(0))) {
            try {
                java.net.InetAddress inetAdd =
                    java.net.InetAddress.getByName(hostStr);
                hostStr = inetAdd.getHostName();
            } catch (java.net.UnknownHostException uhe) {
                hostStr = "localhost";
            }            
        }                
        String uHostStr = hostStr.split("\\.") == null ? hostStr : hostStr.split("\\.")[0];;        
        return "QM_" + uHostStr.replace('-', '_').toLowerCase();
    }
    
    /**
     * Gets the connection type properties and connection URL
     *
     * @param p properties to fill in
     * @param ra resouce adapter
     * @param spec activation spec
     * @param fact factory
     * @param overrideUrl optional URL specified in createConnection(URL, password)
     * @return url parser
     * @throws JMSException on incorrect URL
     */
    public ConnectionUrl getProperties(Properties p, RAJMSResourceAdapter ra, RAJMSActivationSpec spec,
           XManagedConnectionFactory fact, String overrideUrl) throws JMSException {

        UrlParser url = (UrlParser) super.getProperties(p, ra, spec, fact, overrideUrl);

        validateAndAdjustURL(url);

        p.setProperty(HOSTNAME, url.getHost());        
        p.setProperty(PORT, Integer.toString(url.getPort()));
        
        //other properties
        String clientId = DEFAULT_CLIENTID;
        if (spec != null && !Str.empty(spec.getClientId())) {
            clientId = spec.getClientId();
        }        
        p.setProperty(CLIENTID, clientId);
        
        //Default Queue Manager
        //DEFAULT_QUEUEMANAGER = getQueueManager(url.getHost());        
        
        return url;
    }
    
    /**
     * createConnectionFactory
     *
     * @param domain boolean
     * @param resourceAdapter boolean
     * @param activationSpec RAJMSActivationSpec
     * @param fact RAJMSResourceAdapter
     * @param overrideUrl override URL: don't use URL from RA, CF, or activation spec (may be null)
     * @return ConnectionFactory
     * @throws JMSException failure
     */
    public ConnectionFactory createConnectionFactory(int domain,
           RAJMSResourceAdapter resourceAdapter, RAJMSActivationSpec activationSpec,
           XManagedConnectionFactory fact, String overrideUrl) throws JMSException {

        ConnectionFactory cf = null;
    
        // Obtain URL
        String urlstr = overrideUrl;
        if (urlstr == null && resourceAdapter != null && !Str.empty(resourceAdapter.getConnectionURL())) {
            urlstr = resourceAdapter.getConnectionURL();
        }
        if (urlstr == null && activationSpec != null && !Str.empty(activationSpec.getConnectionURL())) {
            urlstr = activationSpec.getConnectionURL();
        }
        if (urlstr == null && fact != null && !Str.empty(fact.getConnectionURL())) {
            urlstr = fact.getConnectionURL();
        }

        // need to ensure that this URL isn't an indirect reference, i.e.
        // actually an LDAP URL where the real value is bound
        String realUrl = resourceAdapter.lookUpLDAP(urlstr);
        
        String clientId = DEFAULT_CLIENTID;
        if (activationSpec != null && !Str.empty(activationSpec.getClientId())) {
            clientId = activationSpec.getClientId();
        }

        Properties cfp = new Properties();
        try {
            WMQConnectionUrl url = new WMQConnectionUrl(realUrl);
            validateAndAdjustURL(url.getUrlParser());
            Properties p = new Properties();
            url.getQueryProperties(p);         
                        
            String qMgr = p.getProperty(QUEUEMANAGER, getQueueManager(url.getUrlParser().getHost()));
                        
            String transType = p.getProperty(TRANSPORTTYPE, DEFAULT_TRANSPORTTYPE);            
            if (TRANSPORT_TYPES[0].equals(transType)) {
                cfp.setProperty(TRANSPORTTYPE, "0");
            } else if (TRANSPORT_TYPES[1].equals(transType)) {
                cfp.setProperty(TRANSPORTTYPE, "1");
            } else {
                cfp.setProperty(TRANSPORTTYPE, "1"); 
            }            
            cfp.setProperty(QUEUEMANAGER, qMgr);
            cfp.setProperty(HOSTNAME, url.getUrlParser().getHost());
            cfp.setProperty(PORT, Integer.toString(url.getUrlParser().getPort()));
            cfp.setProperty(CLIENTID, clientId);
            String channelName = p.getProperty(CHANNEL);
            if (channelName != null) {
                cfp.setProperty(CHANNEL, channelName);
            }
        } catch (Exception ex) {
            JMSException e = new JMSException("Invalid url " + realUrl);
            e.initCause(ex);
            throw e;
        }
        
        try {
            switch (domain) {
                case XConnectionRequestInfo.DOMAIN_QUEUE_NONXA:
                cf = (ConnectionFactory) ClassLoaderHelper.loadClass(
                    "com.ibm.mq.jms.MQQueueConnectionFactory").newInstance();
                break;
            case XConnectionRequestInfo.DOMAIN_QUEUE_XA:
                cf = (ConnectionFactory) ClassLoaderHelper.loadClass(
                    "com.ibm.mq.jms.MQXAQueueConnectionFactory").newInstance();
                break;
            case XConnectionRequestInfo.DOMAIN_TOPIC_NONXA:
                cf = (ConnectionFactory) ClassLoaderHelper.loadClass(
                    "com.ibm.mq.jms.MQXATopicConnectionFactory").newInstance();
                break;
            case XConnectionRequestInfo.DOMAIN_TOPIC_XA:
                cf = (ConnectionFactory) ClassLoaderHelper.loadClass(
                    "com.ibm.mq.jms.MQXATopicConnectionFactory").newInstance();
                break;
            case XConnectionRequestInfo.DOMAIN_UNIFIED_NONXA:
                cf = (ConnectionFactory) ClassLoaderHelper.loadClass(
                    "com.ibm.mq.jms.MQConnectionFactory").newInstance();
                break;
            case XConnectionRequestInfo.DOMAIN_UNIFIED_XA:
                cf = (ConnectionFactory) ClassLoaderHelper.loadClass(
                    "com.ibm.mq.jms.MQXAConnectionFactory").newInstance();
                break;
            default:
                throw Exc.jmsExc(LOCALE.x("E309: Logic fault: invalid domain {0}", Integer.toString(domain)));
            }
        } catch (Exception ex) {
            throw Exc.jmsExc(LOCALE.x("E840: MQ connection factory instantiation failure: {0}", ex), ex);
        }
        
        //Set MQ properties
        Class clazz = cf.getClass();
        try {
            clazz.getMethod("setHostName", new Class[] {String.class}).invoke(
                cf, new Object[] {cfp.getProperty(HOSTNAME)});

            clazz.getMethod("setPort", new Class[] {int.class}).invoke(
                cf, new Object[] {Integer.valueOf(cfp.getProperty(PORT))});

            clazz.getMethod("setQueueManager", new Class[] {String.class}).invoke(
                cf, new Object[] {cfp.getProperty(QUEUEMANAGER)});

            clazz.getMethod("setTransportType", new Class[] {int.class}).invoke(
                cf, new Object[] {Integer.valueOf(cfp.getProperty(TRANSPORTTYPE))});

//            clazz.getMethod("setClientID", new Class[] {String.class}).invoke(
//                cf, new Object[] {cfp.getProperty(CLIENTID)});

            String channelName = cfp.getProperty(CHANNEL);
            if (channelName != null) {
                clazz.getMethod("setChannel", new Class[] {String.class}).invoke(cf,
                    new Object[] {channelName});
            }
        } catch (Exception e) {
            throw Exc.jmsExc(LOCALE.x("E841: Failure to set connection factory properties: {0}", e), e);
        }
//        ((MQConnectionFactory) cf).setHostName(cfp.getProperty(HOSTNAME));
//        ((MQConnectionFactory) cf).setPort(Integer.valueOf(cfp.getProperty(PORT)).intValue());
//        ((MQConnectionFactory) cf).setQueueManager(cfp.getProperty(QUEUEMANAGER));      
//        ((MQConnectionFactory) cf).setTransportType(Integer.valueOf(cfp.getProperty(TRANSPORTTYPE)).intValue());
//        ((MQConnectionFactory) cf).setClientID(cfp.getProperty(CLIENTID));                      
//        }
        return cf;
    } 
    
    /**
     * Returns true if the specified string may be a recognised URL
     *
     * @param url String
     * @return true if may be URL
     */
    public boolean isUrl(String url) {
        if (url != null && url.length() > 0) {
            for (int i = 0; i < URL_PREFIXES.length; i++) {
                if (url.startsWith(URL_PREFIXES[i])) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Gets the XAResource out of a Session; returns null if the specified XA flag is
     * false.
     *
     * @param isXA boolean
     * @param s Session
     * @return XAResource
     */
    public XAResource getXAResource(boolean isXA, Session s) {
        return !isXA ? null : new WMQXAResource(((XASession) s).getXAResource());        
    }

    /**
     * Gets JMSServerType 
     * @return String
     */    
    public String getJMSServerType() {
        return "WMQ";
    }

    /**
     * @see com.stc.jmsjca.core.RAJMSObjectFactory#setClientID(javax.jms.Connection, 
     *   boolean, com.stc.jmsjca.core.RAJMSActivationSpec, com.stc.jmsjca.core.RAJMSResourceAdapter)
     */
    public void setClientID(Connection connection, boolean isTopic,
            RAJMSActivationSpec spec, RAJMSResourceAdapter ra) throws JMSException {
        setClientIDIfNotSpecified(connection, isTopic, spec, ra);
    }
}
