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

package com.stc.jmsjca.stcms;

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
import com.stc.jmsjca.util.UrlParser;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.JMSException;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Properties;

/**
 * Encapsulates the configuration of a MessageEndpoint.
 * 
 * @author Frank Kieviet
 * @version $Revision: 1.12 $
 */
public class RASTCMSObjectFactory extends RAJMSObjectFactory implements
    java.io.Serializable {
    private static Logger sLog = Logger.getLogger(RASTCMSObjectFactory.class);
    private static final Localizer LOCALE = Localizer.get();
    
    /**
     * Name under which the STCMS port number should be available if no port
     * number is specified in the URL
     */
    public static final String PORTPROP = "STCMS.Server.Port";

    /**
     * Name under which the STCMS port number should be available if no port
     * number is specified in the URL
     */
    public static final String PORTSSLPROP = "STCMS.Server.sPort";

    /**
     * Protocol without SSL
     */
    public static final String PROT_NON_SSL = "stcms";

    /**
     * Protocol with SSL
     */
    public static final String PROT_SSL = "stcmss";

    /**
     * Protocol with LDAP
     */
    public static final String PROT_LDAP = "ldap";

    private static final String[] URL_PREFIXES = new String[] {PROT_NON_SSL + "://",
        PROT_SSL + "://", PROT_LDAP + "://" };

    private static final String SSL_AUTHENTICATION_MODE = "com.stc.jms.ssl.authenticationmode";
    private static final String SSL_AUTHENTICATION_MODE_TRUSTALL = "TrustAll";
    private static final String HOST = "com.stc.jms.sockets.ServerHost";
    private static final String PORT = "com.stc.jms.sockets.ServerPort";
    private static final String AUTOCOMMITXA = "com.stc.jms.autocommitxa";
    private static final String STRICTPERSISTENCE = "com.stc.jms.strictPersistence";

    private static final Localizer LOCALIZER = Localizer.get();
    
    private String getCAPSKey(String hostOrUsername) {
        if (hostOrUsername != null 
            && hostOrUsername.startsWith("(") 
            && hostOrUsername.endsWith(")") 
            && hostOrUsername.length() > 2) {
            // CAPS 6 integration: use System properties that were put there by the 
            // STCMS lifecycle listener
            String key = hostOrUsername.substring(1, hostOrUsername.length() - 1);
            return key;
        }
        return null;
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
        UrlParser url = (UrlParser) aurl;
        boolean hasChanged = false;
        boolean isSSL = false;

        // protocol
        if (PROT_NON_SSL.equals(url.getProtocol())) {
            // ...
        } else if (PROT_SSL.equals(url.getProtocol())) {
            isSSL = true;
            // ...
        } else if (PROT_LDAP.equals(url.getProtocol())) {
            // ...
            return false;
        } else {
            throw Exc.jmsExc(LOCALE.x("E306: Invalid protocol [{0}]:"
                + " should be ''stcms'' or ''stcmss''",  url.getProtocol()));
        }

        // For CAPS6 integration: if the host is of the form (key), this will try to 
        // lookup the port number in the System properties, which was put in there
        // by the STCMS lifecycle listener.
        String capsKey = getCAPSKey(url.getHost());
        if (capsKey != null) {
            url.setHost("localhost");
            hasChanged = true;

            String propname = "STCMS-" + capsKey + (isSSL ? "-SSLPORT" : "-PORT");
            String s = System.getProperty(propname);
            if (s != null) {
                int port = Integer.parseInt(s);
                url.setPort(port);
            } else {
                throw Exc.jmsExc(LOCALE.x("E307: No port specified in URL [{0}]"
                    + ", and also not available in System property [{1}]", url, propname));
            }
        }

        // Check port
        int port = url.getPort();
        if (port <= 0) {
            String propname = isSSL ? PORTSSLPROP : PORTPROP;
            String s = System.getProperty(propname);
            if (s != null) {
                port = Integer.parseInt(s);
                // Adjust URL
                url.setPort(port);
                hasChanged = true;
            } else {
                throw Exc.jmsExc(LOCALE.x("E307: No port specified in URL [{0}]"
                    + ", and also not available in System property [{1}]", url, propname));
            }
        }

        // Check host
        String host = url.getHost();
        if ("".equals(host)) {
            url.setHost("localhost");
            hasChanged = true;
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
        
        String capsKey = getCAPSKey(username);
        if (capsKey != null) {
            String token = System.getProperty("STCMS-" + capsKey + "-TOKEN");
            if (token != null) {
                username = "File://" + username + "?admin?trusted";
                password = token;

                if (sLog.isDebugEnabled()) {
                    sLog.debug("Overriding username " + username + " and password with system properties");
                }
            }
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

        // Get the connection properties
        Properties p = new Properties();
        UrlParser url = (UrlParser) getProperties(p, resourceAdapter, activationSpec, fact, overrideUrl);
        
        // Add STCMS specific connection properties to this properties set
        int port = url.getPort();
        p.setProperty(HOST, url.getHost());
        p.setProperty(PORT, Integer.toString(port));
        
        // When using SSL, use TrustAll unless otherwise specified
        if (PROT_SSL.equals(url.getProtocol())) {
            if (p.get(SSL_AUTHENTICATION_MODE) == null) {
                p.setProperty(SSL_AUTHENTICATION_MODE, SSL_AUTHENTICATION_MODE_TRUSTALL);
            }
        }

        // Other options
        p.setProperty(AUTOCOMMITXA, "true");

        if (fact != null && fact.getOptionStrict()) {
            p.setProperty(STRICTPERSISTENCE, "true");
        }

        String classname;
        switch (domain) {
        case XConnectionRequestInfo.DOMAIN_QUEUE_NONXA:
            classname = "com.stc.jms.client.STCQueueConnectionFactory";
            break;
        case XConnectionRequestInfo.DOMAIN_QUEUE_XA:
            classname = "com.stc.jms.client.STCXAQueueConnectionFactory";
            break;
        case XConnectionRequestInfo.DOMAIN_TOPIC_NONXA:
            classname = "com.stc.jms.client.STCTopicConnectionFactory";
            break;
        case XConnectionRequestInfo.DOMAIN_TOPIC_XA:
            classname = "com.stc.jms.client.STCXATopicConnectionFactory";
            break;
        case XConnectionRequestInfo.DOMAIN_UNIFIED_NONXA:
            classname = "com.stc.jms.client.STCConnectionFactory";
            break;
        case XConnectionRequestInfo.DOMAIN_UNIFIED_XA:
            classname = "com.stc.jms.client.STCXAConnectionFactory";
            break;
        default:
            throw Exc.jmsExc(LOCALIZER.x("E308: Logic fault: invalid domain {0}", Integer.toString(domain)));
        }
        
        try {
            Class<?> clazz = ClassLoaderHelper.loadClass(classname);
            Constructor<?> constructor = clazz.getConstructor(new Class[] {Properties.class});
            return (ConnectionFactory) constructor.newInstance(new Object[] {p});
        } catch (Exception e) {
            throw Exc.jmsExc(LOCALIZER.x("E301: Could not instantiate STCMS connection factory: {0}", e), e);
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
        XManagedConnection mc, XConnectionRequestInfo descr,
        boolean isXa, boolean isTransacted, int acknowledgmentMode, Class<?> sessionClass)
        throws JMSException {

        return new RASTCMSSessionConnection(connectionFactory, objfact, ra,
            mc, descr, isXa, isTransacted, acknowledgmentMode,
            sessionClass);
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
     * Default container MBean
     */
    public static final String STCMSCONTAINERMBEAN 
    = "com.sun.appserv:type=messaging-server-admin-mbean,jmsservertype=stcms,name=Sun_SeeBeyond_JMS_IQ_Manager";

    /**
     * @see com.stc.jmsjca.core.RAJMSObjectFactory#getServerMgtMBean(com.stc.jmsjca.core.RAJMSResourceAdapter,
     *      com.stc.jmsjca.core.RAJMSActivationSpec)
     */
    @Override
    public Object getServerMgtMBean(RAJMSResourceAdapter ra, RAJMSActivationSpec spec)
        throws JMSException {
        Object ret = null;

        try {
            // Determine URL
            Properties p = new Properties();
            UrlParser url = (UrlParser) getProperties(p, ra, spec, null, null);
            validateAndAdjustURL(url);

            if (System.getProperty(PORTPROP, null) != null) {
                // Check if MBean can be used that is by default installed (the
                // STCMSCONTAINERMBEAN)
                // The STCMSCONTAINERMBEAN can only be used if it points to the
                // same server as this RA is configured to use; the URL can be
                // found using localhost+ system properties
                // If the URL points to the same server, it's ok to use the
                // STCMSCONTAINERMBEAN instead of creating a new one
                if (url.getHost().equals("localhost")
                    || url.getHost().equals("127.0.0.1")) {
                    // Determine port
                    boolean isSSL = PROT_SSL.equals(url.getProtocol());
                    String propname = isSSL ? PORTSSLPROP : PORTPROP;
                    String s = System.getProperty(propname);
                    int port = s != null ? Integer.parseInt(s) : url.getPort() + 1;
                    if (url.getPort() == port) {
                        // URL points to the same server as the preinstalled
                        // MBean points to
                        ret = STCMSCONTAINERMBEAN;
                    }
                }
            }
        } catch (Exception e) {
            sLog.warn(LOCALIZER.x("E300: Non-critical error: could not determine URL for ServerMBean: {0}", e), e);
        }

        if (ret == null) {
            // Determine URL -> connection properties
            Properties p = new Properties();
            UrlParser url = (UrlParser) getProperties(p, ra, spec, null, null);

            Properties connectionProperties = new Properties();
            connectionProperties.setProperty(HOST, url.getHost());
            connectionProperties.setProperty(PORT, Integer.toString(url.getPort()));
            // When using SSL, use TrustAll unless otherwise specified
            if (PROT_SSL.equals(url.getProtocol())) {
                if (p.get(SSL_AUTHENTICATION_MODE) == null) {
                    connectionProperties.setProperty(SSL_AUTHENTICATION_MODE,
                        SSL_AUTHENTICATION_MODE_TRUSTALL);
                } else {
                    connectionProperties.setProperty(SSL_AUTHENTICATION_MODE, 
                        p.getProperty(SSL_AUTHENTICATION_MODE));
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
                Class<?> c = Class.forName("com.stc.jmsmx.stcms.ExternalStcmsMBean");
                Object stcmsMBean = c.newInstance();
                // Initialize this object using this method:
                // public void setConnectInfo(Properties connectionProperties, 
                // String username, String password) {
                Class<?>[] signatures = {Properties.class, String.class,
                    String.class };
                Object[] args = {connectionProperties, username, password};
                Method method = c.getMethod("setConnectInfo", signatures);
                method.invoke(stcmsMBean, args);
                ret = stcmsMBean;
            } catch (Exception e) {
                sLog.info(LOCALIZER.x("E302: Error instantiating or configuring MBean for "
                    + "external JMS server management: {0}", e));
//                throw Exc.jmsExc(LOCALIZER.x("E302: Error instantiating or configuring MBean for "
//                    + "external JMS server management: {0}", e), e);
            }
        }

        return ret;
    }

    /**
     * @see com.stc.jmsjca.core.RAJMSObjectFactory#getJMSServerType()
     */
    @Override
    public String getJMSServerType() {
        return "STCMS";
    }

    @Override
    public RAJMSActivationSpec createActivationSpec() {
        return new RASTCMSActivationSpec();
    }
}
