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

package com.stc.jmsjca.wl;

import com.stc.jmsjca.core.AdminDestination;
import com.stc.jmsjca.core.DestinationCacheEntry;
import com.stc.jmsjca.core.Options;
import com.stc.jmsjca.core.PseudoXASession;
import com.stc.jmsjca.core.RAJMSActivationSpec;
import com.stc.jmsjca.core.RAJMSObjectFactory;
import com.stc.jmsjca.core.RAJMSResourceAdapter;
import com.stc.jmsjca.core.SessionConnection;
import com.stc.jmsjca.core.XConnectionRequestInfo;
import com.stc.jmsjca.core.XManagedConnection;
import com.stc.jmsjca.core.XManagedConnectionFactory;
import com.stc.jmsjca.util.Exc;
import com.stc.jmsjca.util.Logger;
import com.stc.jmsjca.util.Str;
import com.stc.jmsjca.util.UrlParser;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.QueueConnection;
import javax.jms.QueueSession;
import javax.jms.Session;
import javax.jms.TopicConnection;
import javax.jms.TopicSession;
import javax.jms.XAConnection;
import javax.jms.XAQueueConnection;
import javax.jms.XATopicConnection;
import javax.naming.Context;
import javax.naming.InitialContext;

import java.lang.reflect.Method;
import java.util.Hashtable;
import java.util.Properties;

/**
 * Object factory for WebLogic.
 * 
 * Connection factories and destinations can only be accessed in WebLogic through JNDI.
 * The JNDI used by WebLogic is incompatible with the CORBA-EE version in RTS; this was
 * only made to work with some fixes in RTS.
 * 
 * WebLogic does not support XA on the client side; however it does expose an XA
 * connection factory; it is this factory that is used.
 * 
 * @author fkieviet
 * @version $Revision: 1.11 $
 */
public class RAWLObjectFactory extends RAJMSObjectFactory implements java.io.Serializable {
    private static Logger sLog = Logger.getLogger(RAWLObjectFactory.class);
    /**
     * Protocol 1
     */
    public static final String PROT_T3 = "t3";

    /**
     * Required to create an initial context
     */
    public static final String JNDI_FACTORY = "weblogic.jndi.WLInitialContextFactory";
    
    /**
     * The connection factory to be used (XA) 
     */
    public static final String DEFAULT_XACF = "weblogic.jms.XAConnectionFactory";
    
    /**
     * The IS has a special patch for Weblogic CORBA communication; this is the class
     * on which a special method needs to be called
     */
    public static final String IS_ORBCLASS = "com.sun.enterprise.util.ORBManager";

    /**
     * The IS has a special patch for Weblogic CORBA communication; this is the class
     * on which a special method needs to be called
     */
    public static final String IS_ORBCLASS2 = "com.stc.corba.any.UtilDelegate";

    /**
     * The IS has a special patch for Weblogic CORBA communication; this is the method
     * that needs to be called for creating an InitialContext
     */
    public static final String IS_ORBMETHOD = "getSeOrbInitProperties";
    
    /**
     * The IS has a special patch for Weblogic CORBA communication; this is the method
     * that needs to be called before and after a CORBA call may result from calling 
     * a method on a JMS object 
     */
    public static final String IS_ORBMETHOD_ISON = "supportsSE";
    
    /**
     * com.sun.jndi.url cannot serve invocations of non-standard CORBA object in weblogic
     * and ensures that proper protocol is used.
     */
    private static final String JNDI_WEBLOGIC_PROTOCOL_PACKAGES = "weblogic.corba.j2ee.naming.url:com.sun.jndi.url";
    private static final String DEFAULT_FACTORY_NAME = "weblogic.jms.XAConnectionFactory";
    private static final String FACTORY_PROPERTYNAME = "JMSJCA.WLFACTORY";

    private static final String[] URL_PREFIXES = new String[] {
        PROT_T3 + "://",
    };

    private static Localizer LOCALE = Localizer.get();
    
    private transient Method mSpecialISORBMethod;
    private transient Method mSpecialISORBMethodIsOn;
    
    /**
     * Constructor 
     */
    public RAWLObjectFactory() {
        try {
            Class c = Class.forName(IS_ORBCLASS);
            mSpecialISORBMethod = c.getMethod(IS_ORBMETHOD, new Class[] {});
            c = Class.forName(IS_ORBCLASS2);
            mSpecialISORBMethodIsOn = c.getMethod(IS_ORBMETHOD_ISON, new Class[] {});
        } catch (Exception e) {
            // ignore
        }
    }
      
    /**
     * @see com.stc.jmsjca.core.RAJMSObjectFactory#adjustDeliveryMode(int, boolean)
     */
    public int adjustDeliveryMode(int mode, boolean xa) {
        int newMode = mode;
        if (mode != RAJMSActivationSpec.DELIVERYCONCURRENCY_SYNC) {
            // && mode != RAJMSActivationSpec.DELIVERYCONCURRENCY_SERIAL) {
            newMode = RAJMSActivationSpec.DELIVERYCONCURRENCY_SYNC;
            sLog.warn(LOCALE.x("E820: Delivery mode ''{0}'' not supported; "
                + " switching to ''{1}''.", 
                RAJMSActivationSpec.DELIVERYCONCURRENCY_STRS[mode],
                RAJMSActivationSpec.DELIVERYCONCURRENCY_STRS[newMode]));
        }
        return newMode;
    }

    /**
     * @see com.stc.jmsjca.core.RAJMSObjectFactory#createSessionConnection(
     * java.lang.Object, com.stc.jmsjca.core.RAJMSObjectFactory, 
     * com.stc.jmsjca.core.RAJMSResourceAdapter, 
     * com.stc.jmsjca.core.XManagedConnection, 
     * com.stc.jmsjca.core.XConnectionRequestInfo, boolean, boolean, int, java.lang.Class)
     */
    public SessionConnection createSessionConnection(Object connectionFactory,
        RAJMSObjectFactory objfact, RAJMSResourceAdapter ra,
        XManagedConnection mc, XConnectionRequestInfo descr,
        boolean isXa, boolean isTransacted, int acknowledgmentMode, Class sessionClass)
        throws JMSException {

        return new WLSessionConnection(connectionFactory, objfact, ra,
            mc, descr, isXa, isTransacted, acknowledgmentMode,
            sessionClass);
    }

    /**
     * @see com.stc.jmsjca.core.RAJMSObjectFactory#createSession(javax.jms.Connection, 
     * boolean, java.lang.Class, com.stc.jmsjca.core.RAJMSResourceAdapter, 
     * com.stc.jmsjca.core.RAJMSActivationSpec, boolean, int)
     */
    public Session createSession(Connection conn, boolean isXA, Class sessionClass,
        RAJMSResourceAdapter ra, RAJMSActivationSpec activationSpec, boolean transacted,
        int ackmode) throws JMSException {
        if (isXA) {
            if (sessionClass == TopicSession.class) {
                return new PseudoXASession(((XATopicConnection) conn).createTopicSession(
                    true, Session.SESSION_TRANSACTED));
            } else if (sessionClass == QueueSession.class) {
                return new PseudoXASession(((XAQueueConnection) conn).createQueueSession(
                    true, Session.SESSION_TRANSACTED));
            } else if (sessionClass == Session.class) {
                return new PseudoXASession(((XAConnection) conn).createSession(
                    true, Session.SESSION_TRANSACTED));
            }
        } else {
            if (sessionClass == TopicSession.class) {
                return ((TopicConnection) conn).createTopicSession(transacted, ackmode);
            } else if (sessionClass == QueueSession.class) {
                return ((QueueConnection) conn).createQueueSession(transacted, ackmode);
            } else if (sessionClass == Session.class) {
                return ((Connection) conn).createSession(transacted, ackmode);
            }
        }
        throw Exc.rtexc(LOCALE.x("E824: Unknown class {0}", sessionClass));
    }

    /**
     * Tool function: closes a context w/o exception
     *
     * @param ctx context to close
     */
    public static void safeClose(InitialContext ctx) {
        if (ctx != null) {
            try {
                ctx.close();
            } catch (Exception e) {
                // ignore
            }
        }
    }
    
    /**
     * Looks up an object in WebLogic's JNDI
     * 
     * @param url host and port
     * @param objectName jndi name
     * @return object name
     * @throws JMSException on failure
     */
    private Object getJndiObject(UrlParser url, String name) throws JMSException {
        if (sLog.isDebugEnabled()) {
            sLog.debug("Looking up JNDI object " + name);
        }

        if (name == null || name.length() == 0) {
            throw Exc.jmsExc(LOCALE.x("E401: The JNDI name is null"));
        }

        InitialContext ctx = null;
        try {
            if (mSpecialISORBMethod != null) {
                // Works on IS only
                if (mSpecialISORBMethodIsOn != null) {
                    Boolean isEnabled = (Boolean) mSpecialISORBMethodIsOn.invoke(null, new Object[0]);
                    if (!isEnabled.booleanValue()) {
                        throw Exc.rsrcExc(LOCALE.x("E823: CORBA-SE needs to be enabled on" 
                            + " this server. Please change the value of the <se-orb enabled=\"false\"/>"
                            + " to <se-orb enabled=\"true\"/> in the configuration file of "
                            + " the Integration Server (logicalhost/is/domains/<domain-name>"
                            + "/config/domain.xml) and restart the server."));
                    }
                }
                final Properties prop = (Properties) mSpecialISORBMethod.invoke(null, new Object[0]);
                prop.put(Context.URL_PKG_PREFIXES, JNDI_WEBLOGIC_PROTOCOL_PACKAGES);
                ctx = new InitialContext(prop);
                return ctx.lookup("corbaname:iiop:1.2@" + url.getHost() + ":" + url.getPort()
                    + '#' + name); 
            } else {
                // Will be executed on other application servers than the IS
                Hashtable env = new Hashtable();
                env.put(Context.INITIAL_CONTEXT_FACTORY, JNDI_FACTORY);
                env.put(Context.PROVIDER_URL, "t3://" + url.getHost() + ":" + url.getPort());
                ctx = new InitialContext(env);
                return ctx.lookup(name);
            }
        } catch (Exception e) {
            throw Exc.jmsExc(LOCALE.x("E821: Could not find JNDI object by name [{0}]: {1}", name, e), e);
        } finally {
            safeClose(ctx);
        }
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
            RAJMSResourceAdapter resourceAdapter,
            RAJMSActivationSpec activationSpec, XManagedConnectionFactory fact,
            String overrideUrl) throws JMSException {
        // Get the connection properties
        Properties p = new Properties();
        UrlParser url = (UrlParser) getProperties(p, resourceAdapter, activationSpec, fact, overrideUrl);
        String factoryname = p.getProperty(FACTORY_PROPERTYNAME, DEFAULT_FACTORY_NAME);

        switch (domain) {
        case XConnectionRequestInfo.DOMAIN_QUEUE_NONXA:
        case XConnectionRequestInfo.DOMAIN_QUEUE_XA:
        case XConnectionRequestInfo.DOMAIN_TOPIC_NONXA:
        case XConnectionRequestInfo.DOMAIN_TOPIC_XA:
        case XConnectionRequestInfo.DOMAIN_UNIFIED_NONXA:
        case XConnectionRequestInfo.DOMAIN_UNIFIED_XA:
            Object o = getJndiObject(url, factoryname);
            return (ConnectionFactory) o; 
        default:
            throw Exc.jmsExc(LOCALE.x("E402: Logic fault: invalid domain {0}", Integer.toString(domain)));
        }
    }

    /**
     * @see com.stc.jmsjca.core.RAJMSObjectFactory#createDestination(javax.jms.Session, boolean, 
     * boolean, com.stc.jmsjca.core.RAJMSActivationSpec, com.stc.jmsjca.core.XManagedConnectionFactory, 
     * com.stc.jmsjca.core.RAJMSResourceAdapter, java.lang.String, java.util.Properties, java.lang.Class)
     */
    public Destination createDestination(Session sess, boolean isXA, boolean isTopic,
        RAJMSActivationSpec activationSpec, XManagedConnectionFactory fact,  RAJMSResourceAdapter ra,
        String destName, Properties options, Class sessionClass) throws JMSException {
        
        if (sLog.isDebugEnabled()) {
            sLog.debug("createDestination(" + destName + ")");
        }

        if (Str.empty(destName)) {
            throw Exc.jmsExc(LOCALE.x("E095: The destination should not be empty or null"));
        }

        // Check for lookup:// destination: this may return an admin destination 
        Destination ret = adminDestinationLookup(destName);
        
        // Check if this is a GenericJMSRA destination, if so this will return a JMSJCA admin destination
        ret = checkGeneric(ret);
        
        // Unwrap admin destination if necessary
        if (ret != null && ret instanceof AdminDestination) {
            // Ignore properties and use name
            AdminDestination admindest = (AdminDestination) ret;
            destName = admindest.retrieveCheckedName();
            
            if (sLog.isDebugEnabled()) {
                sLog.debug(ret + " is an admin object: embedded name: " + destName);
            }
            ret = null;
        }
        
        // Needs to parse jmsjca:// format?
        if (ret == null && destName.startsWith(Options.Dest.PREFIX)) {
            Properties otherOptions = new Properties();
            UrlParser u = new UrlParser(destName);
            otherOptions = u.getQueryProperties();

            // Reset name from options
            if (Str.empty(otherOptions.getProperty(Options.Dest.NAME))) {
                throw Exc.jmsExc(LOCALE.x("E207: The specified destination string [{0}] does not " 
                    + "specify a destination name. Destination names are specified using " 
                    + "the ''name'' key, e.g. ''jmsjca://?name=Queue1''.", 
                    otherOptions.getProperty(Options.Dest.ORIGINALNAME)));
            }
            destName = otherOptions.getProperty(Options.Dest.NAME);
        }
        
        // Create if necessary
        if (ret == null) {
            if (sLog.isDebugEnabled()) {
                sLog.debug(destName + " is a jndi object: looking up [" + destName + "]");
            }
            
            // Check cache
            if (fact == null) {
                ret = (Destination) lookupDestination(activationSpec, fact, ra, destName, null, null);
            } else {
                DestinationCacheEntry d = isTopic 
                ? fact.getTopicCache().get(destName) : fact.getQueueCache().get(destName);
                synchronized (d) {
                    ret = d.get();
                    if (ret == null) {
                        ret = (Destination) lookupDestination(activationSpec, fact, ra, destName, null, null);
                        d.set(ret);
                    }
                }
            }
        }

        return ret;
    }
    
    private Destination lookupDestination(
        RAJMSActivationSpec activationSpec, XManagedConnectionFactory fact,  RAJMSResourceAdapter ra,
        String destName, Properties options1, Properties options2) throws JMSException {
        
        // Get the connection properties
        Properties options3 = new Properties();
        UrlParser url = (UrlParser) getProperties(options3, ra, activationSpec, fact, null);

        // Get the prefix to construct a full name
        String prefix = null;
        if (options1 != null) {
            prefix = options1.getProperty(RAWLResourceAdapter.PROP_PREFIX);
        }
        if (prefix == null && options2 != null) {
            prefix = options2.getProperty(RAWLResourceAdapter.PROP_PREFIX);
        }
        if (prefix == null) {
            prefix = options3.getProperty(RAWLResourceAdapter.PROP_PREFIX);
        }
        
        // Not found? Default to empty string: assume that the specified name is the full name
        if (prefix == null) {
            prefix = "";
        }
        
        // Add slash if appropriate
        if (prefix.length() > 0 && !prefix.endsWith("/")) {
            prefix += "/";
        }
        
        // To lookup:
        String name = prefix + destName;

        return (Destination) getJndiObject(url, name);
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
     * Sets the clientID for durable subscribers if not already set. If not set in the 
     * spec, it will be set to CLIENTID-<durable name> since WebLogic always requires a
     * clientID to be set for durable subscribers unless specified in the connection 
     * factory.
     * 
     * @param connection connection
     * @param isTopic isTopic
     * @param spec activation spec
     * @param ra ra
     * @throws JMSException on failure
     */
    public void setClientID(Connection connection, boolean isTopic,
        RAJMSActivationSpec spec, RAJMSResourceAdapter ra) throws JMSException {
        setClientIDIfNotSpecified(connection, isTopic, spec, ra);
    }

    /**
     * @see com.stc.jmsjca.core.RAJMSObjectFactory#getJMSServerType()
     */
    public String getJMSServerType() {
        return "WL";
    }

    /**
     * If a connection failure occurs, the connection factory should be looked up 
     * again.
     * 
     * @see com.stc.jmsjca.core.RAJMSObjectFactory#shouldCacheConnectionFactories()
     */
    public boolean shouldCacheConnectionFactories() {
        return false;
    }
}
