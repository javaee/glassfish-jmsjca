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

package com.stc.jmsjca.wave;

import com.stc.jmsjca.core.RAJMSActivationSpec;
import com.stc.jmsjca.core.RAJMSObjectFactory;
import com.stc.jmsjca.core.RAJMSResourceAdapter;
import com.stc.jmsjca.core.SessionConnection;
import com.stc.jmsjca.core.XConnectionRequestInfo;
import com.stc.jmsjca.core.XManagedConnection;
import com.stc.jmsjca.core.XManagedConnectionFactory;
import com.stc.jmsjca.core.XXid;
import com.stc.jmsjca.util.ClassLoaderHelper;
import com.stc.jmsjca.util.ConnectionUrl;
import com.stc.jmsjca.util.Exc;
import com.stc.jmsjca.util.Logger;
import com.stc.jmsjca.util.Str;
import com.stc.jmsjca.util.UrlParser;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.JMSException;

import java.lang.reflect.Method;
import java.util.Properties;

/**
 * Encapsulates most of the specific traits of the Wave message server.
 * Format of the URL: see WaveUrlParser
 * The ConnectionURL is parsed, all properties are collected (left to right)
 * and the urls are reconstructed and passed to Wave.
 *  
 * @author misc
 * @version $Revision: 1.10 $
 */
public class RAWaveObjectFactory extends RAJMSObjectFactory implements java.io.Serializable {
    private static Logger sLog = Logger.getLogger(RAWaveObjectFactory.class);
    /**
     * Protocol 1
     */
    public static final String PROT_STREAM = "stream";

    /**
     * Protocol 2 
     */
    public static final String PROT_TCP = "tcp";

    /**
     * Protocol 3 
     */
    public static final String PROT_SSL = "ssl";
    
    /**
     * Protocol 4 
     */
    public static final String PROT_HTTP = "http";
    

    private static final String[] URL_PREFIXES = new String[] {
        PROT_STREAM + "://",
        PROT_TCP + "://",
        PROT_SSL + "://",
        PROT_HTTP + "://",
    };
    private static final String[] PROTOCOLS = new String[] {
        PROT_STREAM,
        PROT_TCP,
        PROT_SSL,
        PROT_HTTP,
    };
    
    /**
     * Default container MBean
     * NB this matches the waveadmin.mbean.name property used in LocalStrings.properties
     * in jmsmx
     */
    public static final String WAVECONTAINERMBEAN 
    = "com.sun.appserv:type=messaging-server-admin-mbean,jmsservertype=wave,name=JMS_Grid_IQ_Manager";
    
    private static final Localizer LOCALE = Localizer.get();
    
    /**
     * Creates a provider specific UrlParser
     * 
     * @param s connectionURL to parse
     * @return parser
     */
    @Override
    public ConnectionUrl createConnectionUrl(String s) {
        return new WaveUrlParser(s);
    }
    
    /**
     * Opportunity to clean up connection before it is returned to the pool
     * For example, it may be necessary to unset clientID
     * 
     * In current version we explicitly close connection so that it can not be reused
     *
     * @param con XManagedConnection
     */
    @Override
    public void cleanup(XManagedConnection con) {
        try {
            if (sLog.isDebugEnabled()) {
                sLog.debug("cleanup");
            }

            Object defaultSession = con.getJSession().getDelegate();
            Object defaultConnection = defaultSession.getClass().getMethod(
                "getConnection", new Class[] {}).invoke(defaultSession, new Object[] {});

            Boolean isClosed = (Boolean) defaultConnection.getClass().getMethod(
                "isClosed", new Class[] {}).invoke(defaultConnection, new Object[] {}); 
            if (isClosed.booleanValue()) {
                if (sLog.isDebugEnabled()) {
                    sLog.debug("connection already closed");
                }
                return;
            }
            String clientID = (String) defaultConnection.getClass().getMethod(
                "getClientID", new Class[] {}).invoke(defaultConnection, new Object[] {}); 
            if (clientID != null && clientID.trim().length() > 0) {
                if (clientID.startsWith("ID:")) {
                    sLog.debug(" not closing cleaned up connection with clientID set to "
                        + clientID);

                } else {
                    sLog.debug("closing cleaned up connection with clientID set to "
                        + clientID);

                    if (sLog.isDebugEnabled()) {
                        sLog.debug("closing cleaned up connection with clientID set to "
                            + clientID);
                    }
                    defaultConnection.getClass().getMethod(
                        "close", new Class[0]).invoke(defaultConnection, new Object[0]); 
                }
            }
        } catch (Exception e) {
            sLog.error(LOCALE.x("E901: Error in clean up: {0}", e), e);
        }
    }
    
    /**
     * Check if the connection is closed (
     *
     * @param con XManagedConnection
     * @return boolean true if the coonnection is invalid
     */
    @Override
    public boolean isInvalid(XManagedConnection con) {
        boolean isInvalid = true;
        try {
            if (sLog.isDebugEnabled()) {
                sLog.debug("isInvalid");
            }

            Object defaultSession = con.getJSession().getDelegate();
            Object defaultConnection = defaultSession.getClass().getMethod(
                "getConnection", new Class[] {}).invoke(defaultSession, new Object[] {});

            Boolean isClosed = (Boolean) defaultConnection.getClass().getMethod(
                "isClosed", new Class[] {}).invoke(defaultConnection, new Object[] {}); 
            isInvalid = isClosed.booleanValue();
        } catch (Exception e) {
            sLog.warn(LOCALE.x("E900: Error in isInvalid: {0}", e), e);
        }
        return isInvalid;
    }
    
    /**
     * Checks the validity of the URL
     * 
     * @param aurl UrlParser
     * @throws javax.jms.JMSException on format failure
     * @return boolean true if the url specified url object was changed by this
     *         validation
     */
    @Override
    public boolean validateAndAdjustURL(ConnectionUrl aurl) throws JMSException {
        boolean hasChanged = false;
        
        WaveUrlParser waveParser = (WaveUrlParser) aurl;
        UrlParser[] parsers = waveParser.getUrlParsers();
        if (parsers.length == 0) {
            throw Exc.jmsExc(LOCALE.x("E905: URL should be a comma delimited set of URLs"));
        }
        
        for (int j = 0; j < parsers.length; j++) {
            UrlParser url = parsers[j];
            boolean protOk = false;
            for (int i = 0; i < PROTOCOLS.length; i++) {
                if (PROTOCOLS[i].equals(url.getProtocol())) {
                    protOk = true;
                    break;
                }
            }
            if (!protOk) {
                throw Exc.jmsExc(LOCALE.x("E906: Invalid protocol [{0}]: should be one of [{1}]"
                    , url.getProtocol(), Str.concat(PROTOCOLS, ", ")));
            }
        }

        return hasChanged;
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

        // Obtain URL
        String urlstr = resourceAdapter.getConnectionURL();
        if (activationSpec != null && !Str.empty(activationSpec.getConnectionURL())) {
            urlstr = activationSpec.getConnectionURL();
        }
        if (fact != null && !Str.empty(fact.getConnectionURL())) {
            urlstr = fact.getConnectionURL();
        }
        
        String clientID = null;
        if (fact != null && !Str.empty(fact.getClientId())) {            
            clientID = fact.getClientId();
            
            if (sLog.isDebugEnabled()) {
                sLog.debug("createConnectionFactory: setting clientID to " + clientID);
            }
        }

        // need to ensure that this URL isn't an indirect reference, i.e.
        // actually an LDAP URL where the real value is bound
        String realUrl = resourceAdapter.lookUpLDAP(urlstr);

        Properties profileprops = new Properties(); 

        try {
            WaveUrlParser url = new WaveUrlParser(realUrl);
            validateAndAdjustURL(url);
            
            // Set up the properties we want to set in the WaveProfile
         
            
            // Set certain client properties
            // these override JMS Grid's own defaults
            // but will themselves be overridden by any specified in the URL
            
            // we want to effectively disable JMS Grid's connection retry mechanism 
            // and rely on the JMSJCA adaptor's instead
            profileprops.setProperty("defaultConnectionRetries", "1");
            profileprops.setProperty("defaultConnectionRetriesTimeout", "1");
            
            // add the properties that were appended to the URL
            url.getQueryProperties(profileprops);

            // add the properties to define the driver and messageChannels
            profileprops.setProperty("driverNames", "SpiritWave"); 
            profileprops.setProperty("SpiritWave.messageChannels", url.getWaveUrlSet());
       //     profileprops.setProperty("sharedConnection", "true");
             
            if (clientID != null) {
               profileprops.setProperty("clientID", clientID);
            }
            
   //         profile.buildProfile(profileprops);
        } catch (Exception ex) {
            JMSException tothrow = new JMSException("Invalid url " + realUrl + ": " + ex);
            tothrow.initCause(ex);
            throw tothrow;
        }

        String classname;
        switch (domain) {
            case XConnectionRequestInfo.DOMAIN_QUEUE_NONXA:
                classname = "com.spirit.wave.jms.WaveQueueConnectionFactory";
                break;
            case XConnectionRequestInfo.DOMAIN_QUEUE_XA:
                classname = "com.spirit.wave.jms.WaveXAQueueConnectionFactory";
                break;
            case XConnectionRequestInfo.DOMAIN_TOPIC_NONXA:
                classname = "com.spirit.wave.jms.WaveTopicConnectionFactory";
                break;
            case XConnectionRequestInfo.DOMAIN_TOPIC_XA:
                classname = "com.spirit.wave.jms.WaveXATopicConnectionFactory";
                break;
            case XConnectionRequestInfo.DOMAIN_UNIFIED_NONXA:
                classname = "com.spirit.wave.jms.WaveConnectionFactory";
                break;
            case XConnectionRequestInfo.DOMAIN_UNIFIED_XA:
                classname = "com.spirit.wave.jms.WaveXAConnectionFactory";
                break;
            default:
                throw Exc.jmsExc(LOCALE.x("E309: Logic fault: invalid domain {0}", Integer.toString(domain)));
        }
        
        try {
            Class<?> clazz = ClassLoaderHelper.loadClass(classname);
            return (ConnectionFactory) clazz.getConstructor(
                new Class[] {Properties.class}).newInstance(
                    new Object[] {profileprops});
        } catch (Exception e) {
            throw Exc.jmsExc(LOCALE.x("E902: Failed to instantiate connection factory: {0}", e), e);
        }
    }
    
    /**
     * Creates a provider specific sessionConnection
     *
     * @param connectionFactory Object
     * @param objfact RAJMSObjectFactory
     * @param ra RAJMSResourceAdapter
     * @param mc XManagedConnection
     * @param descr XConnectionRequestInfo
     * @param isXa boolean
     * @param isTransacted boolean
     * @param acknowledgmentMode int
     * @param sessionClass Class
     * @throws JMSException failure
     * @return SessionConnection
     */
    @Override
    public SessionConnection createSessionConnection(Object connectionFactory,
        RAJMSObjectFactory objfact, RAJMSResourceAdapter ra,
        XManagedConnection mc,
        XConnectionRequestInfo descr, boolean isXa, boolean isTransacted,
        int acknowledgmentMode, Class<?> sessionClass)
    throws JMSException {
  
        SessionConnection result = super.createSessionConnection(connectionFactory,
            objfact, ra, mc, descr, isXa, isTransacted, acknowledgmentMode, sessionClass);
        if (descr.getClientID() == null) {
            setClientID(ra, result);
        }
        return result;
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
     * Gets the client id prefix from the resource adapter object. Note that that object
     * may not necessarily be the RAWaveResourceAdapter in case the CombinedRA is used
     * 
     * @param ra resource adapter (RACombined or RAWave)
     * @return String, may be null
     */
    public String getClientIDPrefix(RAJMSResourceAdapter ra) {
        if (ra instanceof RAWaveResourceAdapter) {
            return ((RAWaveResourceAdapter) ra).getClientIDPrefix();
        }
        // TODO: allow for property to be set in RA and retrieve value from there
        return null;
    }

    /**
     * Sets the clientID for for an inbound connection  
     * to a value based on the wave-specific ClientIDPrefix property in the resource adaptor
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
        // See Work ticket 96445
        
        String suffix = null;
        if (isTopic && RAJMSActivationSpec.DURABLE.equals(spec.getSubscriptionDurability())) {
            // a durable subscriber can only be active in one location so no need to append a GUID
            suffix = "";
        } else {
            // need to append a guid to ensure that all connections have unique
            // clientID values
            suffix = "_" + XXid.toHex(new XXid().getGlobalTransactionId());
        }
       
        String currentClientId = connection.getClientID();
        String resourceAdaptorClientID = getClientIDPrefix(ra);
        
        if (currentClientId == null || currentClientId.length() == 0) {
            // clientID not already set in connection
            if (resourceAdaptorClientID == null || resourceAdaptorClientID.length() == 0) {
                // clientID not set in resource adaptor either
                if (spec.getSubscriptionDurability().equals(RAJMSActivationSpec.DURABLE)) {
                    // if durable subscriber must make sure that clientID is set
                    setClientID(connection, "CLIENTID-" + spec.getSubscriptionName());
                } else {
                    // if clientID not set in resource adaptor then leave it unset
                }
            } else {
                // clientID set in resource adaptor - use this (append a guid if required)
                setClientID(connection, resourceAdaptorClientID + suffix);
            }
        } else {
            // clientID already set in connection
            if (resourceAdaptorClientID == null || resourceAdaptorClientID.length() == 0) {
                // clientID not set in resource adaptor - fine, we'll stick with the existing value
            } else {
                // clientID set in resource adaptor as well: compare it with the value we would use otherwise 
                // (i.e. with guid appended if required)
                if (currentClientId.equals(resourceAdaptorClientID + suffix)) {
                    // clientID in resource adaptor matches existing value - fine
                } else {
                    // resource adaptor defines a different client ID than connection: ignore it and give a warning
                    sLog.warn(LOCALE.x("E903: ClientID is already set to [{0}]; cannot set to [{1}]"
                        + " as required in resource adaptor", currentClientId, resourceAdaptorClientID + suffix)); 
                }
            }
        }
    }
    
    /**
     * Sets the clientID for for an outbound connection  
     * to a value based on the wave-specific ClientIDPrefix property in the resource adaptor
     * 
     * Assumes the clientID has not already been set (or if it has then that it should be overridden)
     * 
     * @param ra resource adapter
     * @param sc SessionConnection
     * @throws JMSException on failure
     */
    public void setClientID(RAJMSResourceAdapter ra, SessionConnection sc)
        throws JMSException {  
        // See Work ticket 96445
        
        // need to append a guid to ensure that all connections have unique clientID values
    
        
        
        
   /*
       // Disable automatically setting client id as it prevents the effective use of 
       // connection pooling
       // Only one active connection can have a given clientID. To enforce this, when old connections are 
       // cleaned up and returned to the pool, a check is made to see if the clientID is set. 
       // If so, the connection is closed to prevent a duplicate. 
        
            
       String suffix = "_" + GUID.getUniqueID();

        String resourceAdaptorClientID = getClientIDPrefix(ra);

        if (resourceAdaptorClientID != null && resourceAdaptorClientID.length() > 0) {
            sc.setClientID(resourceAdaptorClientID + suffix);
        }
   */     
    }   

    /**
     * @see com.stc.jmsjca.core.RAJMSObjectFactory#getJMSServerType()
     */
    @Override
    public String getJMSServerType() {
        return "WAVE";
    }
    
    /**
     * This returns an mbean that can be used to manage the JMS server that this RA is 
     * associated with. It can either return (1) null if this feature is not supported,
     * (2) a string that represents the ObjectName of an mbean that already exists, or
     * (3) an object that is a newly created mbean. In the latter case, the caller is
     * responsible for registering the object in an mbean server and cleaning it up.
     * 
     * @param ra the resource adapter
     * @param spec an optional activation spec (may be null)
     * @return null, a string, or an mbean object.
     * @throws JMSException on failure
     */
    @Override
    public Object getServerMgtMBean(RAJMSResourceAdapter ra, RAJMSActivationSpec spec)
    throws JMSException {
        Object ret = null;
    
        sLog.debug("At start of getServerMgtMBean");
        
        /** ------------------------------------------------------------- **/
        // at the time of writing this option is not available as the IS 
        // does not manage the JMS Grid lifecycle, which would include the
        // creation of this bean. Hence commented out.
        /** ------------------------------------------------------------- **/
        
        // option (2) see if there is already an MBean available to the daemon
        /*try {
            // Determine URL
            Properties p = new Properties();
            UrlParser url = (UrlParser) getProperties(p, ra, spec, null, null);
            validateAndAdjustURL(url);
            
            // not sure how this should go for Wave - for STCMS we know
            // that if our URL is localhost and port matches the system property
            // for the TCP, or SSL port if it is set, then there will already be 
            // an MBean and we can return its name - WAVECONTAINERMBEAN
            
            // do something similar for JMS Grid
            
            if (url.getHost().equals("localhost")
                || url.getHost().equals("127.0.0.1")) {
                if (url.getPort() == 50607) {
                    ret = WAVECONTAINERMBEAN;
                }
            }            
        } catch (Exception e) {
            sLog.warn(
                "Non-critical error: could not determine URL for ServerMBean: " + e, e);
        }*/
    
        // option (3) create a new MBean 
        if (ret == null) {
            // Determine URL -> connection properties
            sLog.debug("getServerMgtMBean - using option 3");
            Properties p = new Properties();
            ConnectionUrl url = getProperties(p, ra, spec, null, null);
            validateAndAdjustURL(url);
            Properties connectionProperties = new Properties();
            
            // for preference connect via tcp, but if no URL available
            // use SSL, then lastly http
            UrlParser tcpUrl = getTcpConnection(url);
            if (tcpUrl != null) {
                connectionProperties.setProperty("hostname", tcpUrl.getHost());
                connectionProperties.setProperty("port", Integer.toString(tcpUrl.getPort()));
                connectionProperties.setProperty("protocol", "tcp");
            } else {
                UrlParser sslUrl = getSslConnection(url);
                if (sslUrl != null) {
                    connectionProperties.setProperty("hostname", sslUrl.getHost());
                    connectionProperties.setProperty("port", Integer.toString(sslUrl.getPort()));
                    connectionProperties.setProperty("protocol", "ssl");   
                } else {
                    // this ought to work - the earlier validateAndAdjustURL()
                    // call will have thrown an exception if there isn't a URL
                    // with one of tcp/ssl/http available
                    UrlParser httpUrl = getHttpConnection(url);
                    connectionProperties.setProperty("hostname", httpUrl.getHost());
                    connectionProperties.setProperty("port", Integer.toString(httpUrl.getPort()));
                    connectionProperties.setProperty("protocol", "http");
                }
            }
            
            // Determine username and password
            String username = spec == null ? null : spec.getUserName();
            if (username == null) {
                username = ra.getUserName();
            }
            String password = spec == null ? null : spec.getClearTextPassword();
            if (password == null) {
                password = ra.getClearTextPassword();
            }
            
            try {
                // Instantiate mbean
                Class<?> c = Class.forName("com.stc.jmsmx.wave.ExternalWaveMBean");
                Object waveMBean = c.newInstance();
                // Initialize this object using this method:
                // public void setConnectInfo(Properties connectionProperties, 
                // String username, String password) {
                Class<?>[] signatures = {Properties.class, String.class,
                    String.class };
                Object[] args = {connectionProperties, username, password};
                Method method = c.getMethod("setConnectInfo", signatures);
                method.invoke(waveMBean, args);
                ret = waveMBean;
            } catch (Exception e) {
                sLog.info(LOCALE.x("E904: Error instantiating or configuring MBean for "
                    + "external JMS Grid daemon management: {0}", e));
//                throw Exc.jmsExc(LOCALE.x("E904: Error instantiating or configuring MBean for "
//                    + "external JMS Grid daemon management: {0}", e), e);
            }
        }
        
        // option (1) we can't support this method at the moment...
        return ret;
    }
    
    /**
     * Finds a tcp connection to use for the MBean
     * @param url unparsed url
     * @return parser or null if none found
     */
    private UrlParser getTcpConnection(ConnectionUrl url) {
        WaveUrlParser waveParser = (WaveUrlParser) url;
        UrlParser[] parsers = waveParser.getUrlParsers();        
        UrlParser result = null;
        
        // the URL has already been validated - so we know there 
        // is at least one
        for (int i = 0; i < parsers.length; i++) {
            UrlParser candidate = parsers[i];
            
            if (candidate.getProtocol().equals(PROT_TCP)) {
                result = candidate;
                break;
            }
        }
        
        return result;
    }

    /**
     * Finds an ssl connection to use for the MBean - we'll 
     * only do this if we've established there is no tcp
     * connection.
     * @param url unparsed url
     * @return parser or null if none found
     */
    private UrlParser getSslConnection(ConnectionUrl url) {
        WaveUrlParser waveParser = (WaveUrlParser) url;
        UrlParser[] parsers = waveParser.getUrlParsers();
        UrlParser result = null;
        
        // the URL has already been validated - so we know there 
        // is at least one
        for (int i = 0; i < parsers.length; i++) {
            UrlParser candidate = parsers[i];
            
            if (candidate.getProtocol().equals(PROT_SSL)) {
                result = candidate;
                break;
            }
        }
        
        return result;
    }
    
    /**
     * Finds an http connection to use for the MBean - we'll 
     * only do this if we've established there is no tcp nor ssl
     * connection.
     * @param url
     * @return
     */
    private UrlParser getHttpConnection(ConnectionUrl url) {
        WaveUrlParser waveParser = (WaveUrlParser) url;
        UrlParser[] parsers = waveParser.getUrlParsers();
        UrlParser result = null;
        
        // the URL has already been validated - so we know there 
        // is at least one
        for (int i = 0; i < parsers.length; i++) {
            UrlParser candidate = parsers[i];
            
            if (candidate.getProtocol().equals(PROT_HTTP)) {
                result = candidate;
                break;
            }
        }
        
        return result;
    }

    @Override
    public RAJMSActivationSpec createActivationSpec() {
        return new RAWaveActivationSpec();
    }
}
