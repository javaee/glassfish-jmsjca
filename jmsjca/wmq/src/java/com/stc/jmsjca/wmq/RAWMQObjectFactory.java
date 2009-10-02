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

import com.stc.jmsjca.core.AdminDestination;
import com.stc.jmsjca.core.Options;
import com.stc.jmsjca.core.RAJMSActivationSpec;
import com.stc.jmsjca.core.RAJMSObjectFactory;
import com.stc.jmsjca.core.RAJMSResourceAdapter;
import com.stc.jmsjca.core.SessionConnection;
import com.stc.jmsjca.core.XConnectionRequestInfo;
import com.stc.jmsjca.core.XManagedConnection;
import com.stc.jmsjca.core.XManagedConnectionFactory;
import com.stc.jmsjca.util.ClassLoaderHelper;
import com.stc.jmsjca.util.ConnectionUrl;
import com.stc.jmsjca.util.Exc;
import com.stc.jmsjca.util.Logger;
import com.stc.jmsjca.util.Str;
import com.stc.jmsjca.util.UrlParser;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Session;
import javax.jms.XASession;
import javax.transaction.xa.XAResource;

import java.lang.reflect.Method;
import java.util.Properties;

/**
 * Encapsulates most of the specific traits of the Wave message server.
 * ConnectionURL: wmq://host:port
 * 
 * @version $Revision: 1.21 $
 * @author cye
 */
public class RAWMQObjectFactory extends RAJMSObjectFactory implements java.io.Serializable {
    private static Logger sLog = Logger.getLogger(RAWMQObjectFactory.class);
    
    /**
     * The option name for BROKERDURSUBQUEUE
     */
    public static final String BROKERDURSUBQUEUE = "BrokerDurSubQueue";

    /**
     * Protocol
     */
    private static final String PROTOCOL6 = "wmq";
    
    /**
     * SSL Protocol
     */
    private static final String SPROTOCOL6 = "wmqs";
    
    /**
     * Protocol
     */
    private static final String PROTOCOL5 = "wmq5";
    
    /**
     * SSL Protocol
     */
    private static final String SPROTOCOL5 = "wmqs5";
    
    /**
     * WebSphere MQ Queue Manager Name
     */        
    public static final String QUEUEMANAGER = "QueueManager";

    /**
     * WebSphere MQ transport type
     */            
    public static final String TRANSPORTTYPE = "TransportType"; 
    
    /**
     * WebSphere MQ Channel
     */            
    public static final String CHANNEL = "Channel";
 
    /**
     * WebSphere MQ Default Transport Type
     */            
    private static final String DEFAULT_TRANSPORTTYPE = "JMSC_MQJMS_TP_CLIENT_MQ_TCPIP";
    
    /*
     * Connection Protocol Url Prefixes
     */
    private static final String[] URL_PREFIXES = new String[] {
            PROTOCOL6 + "://",
            SPROTOCOL6 + "://",
            PROTOCOL5 + "://",
            SPROTOCOL5 + "://",
        };
    
    /*
     * Connection Transport Types
     */
    private static final String[] TRANSPORT_TYPES = new String[] {
            "JMSC.MQJMS_TP_BINDINGS_MQ",
            "JMSC_MQJMS_TP_CLIENT_MQ_TCPIP",
        };

    private static final Localizer LOCALE = Localizer.get();    
    
    /** 
     * General property prefix
     *
     * The com.ibm.mq.jms.MQConnectionFactory has a large list of properties
     * of type boolean, int, long, String, URL which can be set.  The setter can
     * be invoked by specifying a connection property named using the following
     * convention: remove the 'set' prefix from the name of the setter method
     * and prepend 'WMQ_'.
     *
     * Examples:
     *
     *  setMessageRetention(int)       : WMQ_MessageRetention=1
     *  setSecurityExit(String)        : WMQ_SecurityExit=wmq.exits.MySecurityExit
     *  setSparseSubscriptions(boolean): WMQ_SparseSubscriptions=true
     *
     * Note:
     *
     *  A "E841: Failure to set connection factory properties" JMSException  will
     *  be thrown if the setter method does not exist.
     *
     */
    private static final String WMQ_GENERAL_PROPERTY = "WMQ_";

    /**
     * @see com.stc.jmsjca.core.RAJMSObjectFactory#adjustDeliveryMode(int, boolean)
     */
    @Override
    public int adjustDeliveryMode(int mode, boolean xa) {
        int newMode = mode;
        if (xa) {
            if (mode != RAJMSActivationSpec.DELIVERYCONCURRENCY_SYNC) {
                newMode = RAJMSActivationSpec.DELIVERYCONCURRENCY_SYNC;
                sLog.warn(LOCALE.x("E820: Delivery mode {0} is not supported; "
                    + " using {1} instead.", 
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
    @Override
    public ConnectionUrl getProperties(Properties p, RAJMSResourceAdapter ra, RAJMSActivationSpec spec,
           XManagedConnectionFactory fact, String overrideUrl) throws JMSException {

        UrlParser url = (UrlParser) super.getProperties(p, ra, spec, fact, overrideUrl);
        validateAndAdjustURL(url);
        
        // WMQ5 does not support XA
        if (url.getProtocol().equals(PROTOCOL5) || url.getProtocol().equals(SPROTOCOL5)) {
            p.setProperty(Options.NOXA, Boolean.TRUE.toString());
        }

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
    @Override
    public ConnectionFactory createConnectionFactory(int domain,
           RAJMSResourceAdapter resourceAdapter, RAJMSActivationSpec activationSpec,
           XManagedConnectionFactory fact, String overrideUrl) throws JMSException {

        Properties p = new Properties();
        UrlParser url = (UrlParser) getProperties(p, resourceAdapter, activationSpec, fact, overrideUrl);
        
        boolean is5 = url.getProtocol().equals(PROTOCOL5) || url.getProtocol().equals(SPROTOCOL5);

        String qMgr = p.getProperty(QUEUEMANAGER, getQueueManager(url.getHost()));

        String transType = p.getProperty(TRANSPORTTYPE, DEFAULT_TRANSPORTTYPE);
        
        String channelName = p.getProperty(CHANNEL);

        String tosetTransportType;
        if (TRANSPORT_TYPES[0].equals(transType)) {
            tosetTransportType = "0";
        } else if (TRANSPORT_TYPES[1].equals(transType)) {
            tosetTransportType = "1";
        } else {
            tosetTransportType = "1"; 
        }            
        
        ConnectionFactory cf = null;
        try {
            switch (domain) {
                case XConnectionRequestInfo.DOMAIN_QUEUE_NONXA:
                cf = (ConnectionFactory) ClassLoaderHelper.loadClass(
                    "com.ibm.mq.jms.MQQueueConnectionFactory").newInstance();
                break;
            case XConnectionRequestInfo.DOMAIN_QUEUE_XA:
                if (is5) {
                    throw Exc.jmsExc(LOCALE.x("E843: Logic fault: trying to use XA on WMQ5"));
                }
                cf = (ConnectionFactory) ClassLoaderHelper.loadClass(
                    "com.ibm.mq.jms.MQXAQueueConnectionFactory").newInstance();
                break;
            case XConnectionRequestInfo.DOMAIN_TOPIC_NONXA:
                cf = (ConnectionFactory) ClassLoaderHelper.loadClass(
                    "com.ibm.mq.jms.MQTopicConnectionFactory").newInstance();
                break;
            case XConnectionRequestInfo.DOMAIN_TOPIC_XA:
                if (is5) {
                    throw Exc.jmsExc(LOCALE.x("E843: Logic fault: trying to use XA on WMQ5"));
                }
                cf = (ConnectionFactory) ClassLoaderHelper.loadClass(
                    "com.ibm.mq.jms.MQXATopicConnectionFactory").newInstance();
                break;
            case XConnectionRequestInfo.DOMAIN_UNIFIED_NONXA:
                cf = (ConnectionFactory) ClassLoaderHelper.loadClass(
                    "com.ibm.mq.jms.MQConnectionFactory").newInstance();
                break;
            case XConnectionRequestInfo.DOMAIN_UNIFIED_XA:
                if (is5) {
                    throw Exc.jmsExc(LOCALE.x("E843: Logic fault: trying to use XA on WMQ5"));
                }
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
        Class<?> clazz = cf.getClass();
        try {
            clazz.getMethod("setHostName", new Class[] {String.class}).invoke(
                cf, new Object[] {url.getHost()});

            clazz.getMethod("setPort", new Class[] {int.class}).invoke(
                cf, new Object[] {Integer.valueOf(url.getPort())});

            clazz.getMethod("setQueueManager", new Class[] {String.class}).invoke(
                cf, new Object[] {qMgr});

            clazz.getMethod("setTransportType", new Class[] {int.class}).invoke(
                cf, new Object[] {Integer.valueOf(tosetTransportType)});

            if (channelName != null) {
                clazz.getMethod("setChannel", new Class[] {String.class}).invoke(cf,
                    new Object[] {channelName});
            }

            // Set general properties
            String status = null; // to provide better exception handling
            try {
                for (java.util.Enumeration<?> e = p.propertyNames(); e.hasMoreElements();) {
                    String key = (String) e.nextElement();
                    if (key.startsWith(WMQ_GENERAL_PROPERTY)) {
                        try {
                            String value = p.getProperty(key);
                            status = key + "=" + value;
                            invokeSetter(cf, key.substring(WMQ_GENERAL_PROPERTY.length()), value);
                        } catch (IndexOutOfBoundsException ex) {
                            // do nothing
                            if (sLog.isDebugEnabled()) {
                                sLog.errorNoloc("Suppressed exception: " + ex,  ex);
                            }
                        }
                    }
                }
            } catch (Exception e) {
                if (status != null) {
                    throw Exc.exc(LOCALE.x("E844: An exception occurred while " 
                        + "processing connection URL property {0}: {1}", status, e), e);
                } else {
                    throw e;
                }
            }
        } catch (Exception e) {
            throw Exc.jmsExc(LOCALE.x("E841: Failure to set connection factory properties: {0}", e), e);
        }
        return cf;
    } 

    /**
     * Invokes the connection factory's setter method
     *
     * @param cf connection factory
     * @param key name of property
     * @param value value of property
     * @throws Exception on invalid property name or type conversion error
     */
    private void invokeSetter(ConnectionFactory cf, String key, String value)
            throws Exception {
        String setter = "set" + key;
        Method[] methods = cf.getClass().getMethods();
        for (int i = 0; i < methods.length; i++) {
            Method method = methods[i];
            if (method.getName().equalsIgnoreCase(setter)) {
                Class<?>[] params = method.getParameterTypes();
                if (params.length == 1) {
                    Object object = value;
                    Class<?> param = params[0];
                    if (param != String.class) {
                        // We need to perform a type conversion using valueOf(String)
                        if (param.isPrimitive()) {
                            // Unfortunately there's no easy way to do this
                            if (param == Integer.TYPE) {
                                object = Integer.valueOf(value);
                            } else if (param == Boolean.TYPE) {
                                object = Boolean.valueOf(value);
                            } else if (param == Long.TYPE) {
                                object = Long.valueOf(value);
                            } else if (param == Short.TYPE) {
                                object = Short.valueOf(value);
                            } else if (param == Float.TYPE) {
                                object = Float.valueOf(value);
                            } else if (param == Double.TYPE) {
                                object = Double.valueOf(value);
                            } else if (param == Character.TYPE) {
                                object = new Character(value.charAt(0));
                            } else if (param == Byte.TYPE) {
                                object = Byte.valueOf(value);
                            } else {
                                throw new Exception("Failed to convert property from String to " + param.getName());
                            }
                        } else {
                            // Try valueOf(String) method
                            try {
                                object = param.getMethod("valueOf", new Class[] {String.class})
                                .invoke(param.newInstance(), new Object[] {value});
                            } catch (java.lang.NoSuchMethodException e) {
                                // No valueOf method so look for a constructor taking a String
                                try {
                                    object = param.getConstructor(new Class[] {String.class}).newInstance(new Object[] {value});
                                } catch (java.lang.NoSuchMethodException e1) {
                                    throw new Exception("Failed to convert property from String to " + param.getName());
                                }
                            }
                        }
                    }
                    if (sLog.isDebugEnabled()) {
                        sLog.debug("Invoking MQConnectionFactory." + setter + "(" + value + ")");
                    }
                    method.invoke(cf, new Object[] {object});
                    return;
                }
            }
        }
        throw new Exception("Could not resolve connection factory setter " + setter);
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
    @Override
    public XAResource getXAResource(boolean isXA, Session s) {
        return !isXA ? null : new WMQXAResource(((XASession) s).getXAResource());        
    }

    /**
     * Gets JMSServerType 
     * @return String
     */    
    @Override
    public String getJMSServerType() {
        return "WMQ";
    }

    /**
     * @see com.stc.jmsjca.core.RAJMSObjectFactory#setClientID(javax.jms.Connection, 
     *   boolean, com.stc.jmsjca.core.RAJMSActivationSpec, com.stc.jmsjca.core.RAJMSResourceAdapter)
     */
    @Override
    public void setClientID(Connection connection, boolean isTopic,
            RAJMSActivationSpec spec, RAJMSResourceAdapter ra) throws JMSException {
        setClientIDIfNotSpecified(connection, isTopic, spec, ra);
    }

    /**
     * @see com.stc.jmsjca.core.RAJMSObjectFactory#isMsgPrefixOK()
     * 
     * WMQ does not allow message properties to be named beginning with JMS_
     */
    @Override
    public boolean isMsgPrefixOK() {
        return false;
    }

    /**
     * @see com.stc.jmsjca.core.RAJMSObjectFactory#createSessionConnection(
     * java.lang.Object, com.stc.jmsjca.core.RAJMSObjectFactory, 
     * com.stc.jmsjca.core.RAJMSResourceAdapter, 
     * com.stc.jmsjca.core.XManagedConnection, 
     * com.stc.jmsjca.core.XConnectionRequestInfo, boolean, boolean, int, java.lang.Class)
     */
    @Override
    public SessionConnection createSessionConnection(Object connectionFactory,
        RAJMSObjectFactory objfact, RAJMSResourceAdapter ra,
        XManagedConnection mc, XConnectionRequestInfo descr,
        boolean isXa, boolean isTransacted, int acknowledgmentMode, Class<?> sessionClass)
        throws JMSException {

        return new WMQSessionConnection(connectionFactory, objfact, ra,
            mc, descr, isXa, isTransacted, acknowledgmentMode,
            sessionClass);
    }
    
    /**
     * createDestination()
     *
     * @param sess Session
     * @param isXA boolean
     * @param isTopic boolean
     * @param activationSpec RAJMSActivationSpec
     * @param fact MCF
     * @param ra RAJMSResourceAdapter
     * @param destName String
     * @param options Options
     * @param sessionClass domain
     * @return Destination
     * @throws JMSException failure
     */
    @Override
    public Destination createDestination(Session sess, boolean isXA, boolean isTopic,
        RAJMSActivationSpec activationSpec, XManagedConnectionFactory fact,  RAJMSResourceAdapter ra,
        String destName, Properties options, Class<?> sessionClass) throws JMSException {
        
        if (sLog.isDebugEnabled()) {
            sLog.debug("createDestination(" + destName + ")");
        }

        // Check for lookup:// destination: this may return an admin destination 
        Destination ret = adminDestinationLookup(destName);
        
        // Check if this is a GenericJMSRA destination, if so this will return a JMSJCA admin destination
        ret = checkGeneric(ret);
        
        // Unwrap admin destination if necessary
        Properties options2 = null;
        if (ret != null && ret instanceof AdminDestination) {
            AdminDestination admindest = (AdminDestination) ret;
            destName = admindest.retrieveCheckedName();
            options2 = admindest.retrieveProperties();
            
            if (sLog.isDebugEnabled()) {
                sLog.debug(ret + " is an admin object: embedded name: " + destName);
            }
            ret = null;
        }
        
        // Needs to parse jmsjca:// format?
        Properties options3 = new Properties();
        if (ret == null && destName.startsWith(Options.Dest.PREFIX)) {
            UrlParser u = new UrlParser(destName);
            options3 = u.getQueryProperties();

            // Reset name from options
            if (Str.empty(options3.getProperty(Options.Dest.NAME))) {
                throw Exc.jmsExc(LOCALE.x("E207: The specified destination string [{0}] does not " 
                    + "specify a destination name. Destination names are specified using " 
                    + "the ''name'' key, e.g. ''jmsjca://?name=Queue1''.", 
                    options3.getProperty(Options.Dest.ORIGINALNAME)));
            }
            destName = options3.getProperty(Options.Dest.NAME);
        }
        
        // Create if necessary
        if (ret == null) {
            if (sLog.isDebugEnabled()) {
                sLog.debug("Creating " + destName + " using createQueue()/createTopic()");
            }
            if (!isTopic) {
                ret = getNonXASession(sess, isXA, sessionClass).createQueue(destName);
            } else {
                ret = getNonXASession(sess, isXA, sessionClass).createTopic(destName);
            }
        }
        
        // Call setBrokerDurSubQueue()
        String dursubqueue = null;
        if (options3 != null) {
            dursubqueue = options3.getProperty(BROKERDURSUBQUEUE);            
        }
        if (options2 != null && dursubqueue == null) {
            dursubqueue = options2.getProperty(BROKERDURSUBQUEUE);            
        }
        if (options != null && dursubqueue == null) {
            dursubqueue = options.getProperty(BROKERDURSUBQUEUE);            
        }
        if (dursubqueue != null) {
            try {
                Method m = ret.getClass().getMethod("setBrokerDurSubQueue", new Class[] {String.class });
                m.invoke(ret, new Object[] {dursubqueue });
            } catch (Exception e) {
                Exc.jmsExc(LOCALE.x("E842: Could not set the broker durable " 
                    + "subscriber subqueue [{0}] on topic [{1}]: {2}",
                    dursubqueue, destName, e), e);
            }
        }
        
        return ret;
    }

    @Override
    public RAJMSActivationSpec createActivationSpec() {
        return new RAWMQActivationSpec();
    }
}
