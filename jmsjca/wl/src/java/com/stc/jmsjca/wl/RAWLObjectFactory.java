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
 * $RCSfile: RAWLObjectFactory.java,v $
 * $Revision: 1.3 $
 * $Date: 2007-01-21 17:52:24 $
 *
 * Copyright 2003-2007 Sun Microsystems, Inc. All Rights Reserved.  
 */

package com.stc.jmsjca.wl;

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
 * @version $Revision: 1.3 $
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
     * com.sun.jndi.url cannot serve invocations of non-standard CORBA object in weblogic
     * and ensures that proper protocol is used.
     */
    private static final String JNDI_WEBLOGIC_PROTOCOL_PACKAGES = "weblogic.corba.j2ee.naming.url:com.sun.jndi.url";
    private static final String FACTORY_NAME = "weblogic.jms.XAConnectionFactory";

    private static final String[] URL_PREFIXES = new String[] {
        PROT_T3 + "://",
    };
//    private static final String[] PROTOCOLS = new String[] {
//        PROT_T3,
//    };
    
    /**
     * @see com.stc.jmsjca.core.RAJMSObjectFactory#adjustDeliveryMode(int, boolean)
     */
    public int adjustDeliveryMode(int mode, boolean xa) {
        int newMode = mode;
        if (mode != RAJMSActivationSpec.DELIVERYCONCURRENCY_SYNC) {
            // && mode != RAJMSActivationSpec.DELIVERYCONCURRENCY_SERIAL) {
            newMode = RAJMSActivationSpec.DELIVERYCONCURRENCY_SYNC;
            sLog.warn("Current delivery mode "
                    + RAJMSActivationSpec.DELIVERYCONCURRENCY_STRS[mode]
                    + " not supported; switching to "
                    + RAJMSActivationSpec.DELIVERYCONCURRENCY_STRS[newMode]);
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
        throw new RuntimeException("Unknown class " + sessionClass);
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
     * Tool function to invoke a method on a class without introducing compile time
     * dependencies
     * 
     * @param classname
     * @param methodName
     * @param types
     * @param args
     * @return
     * @throws Exception
     */
    private Object invoke(String classname, String methodName, Class[] types,
        Object[] args) throws Exception {
        try {
            Class c = Class.forName(classname);
            Method m = c.getMethod(methodName, types);
            return m.invoke(null, args);
        } catch (Exception e) {
            throw new Exception("Cannot invoke method " + methodName + " on " + classname
                + ": " + e, e);
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
    private Object getJndiObject(UrlParser url, String objectName) throws JMSException {
        return getJndiObject("corbaname:iiop:1.2@" + url.getHost() + ":" + url.getPort()
            + '#' + objectName);        
    }
    
    /**
     * Looks up an object in JNDI if the prefetch is not specified; if it is specified
     * it simply returns the prefetch.
     *
     * @param name jndi name
     * @param prefetch to return immediately if not null
     * @return non-null object
     * @throws JMSException on error
     */
    private Object getJndiObject(String name) throws JMSException {
        if (sLog.isDebugEnabled()) {
            sLog.debug("Looking up JNDI object " + name);
        }

        if (name == null || name.length() == 0) {
            throw new JMSException("The JNDI name is null");
        }

        InitialContext ctx = null;
        try {
            final Properties prop = (Properties) invoke(
                "com.sun.enterprise.util.ORBManager", "getSeOrbInitProperties",
                new Class[] {}, new Object[] {});
            prop.put(Context.URL_PKG_PREFIXES, JNDI_WEBLOGIC_PROTOCOL_PACKAGES);
            ctx = new InitialContext(prop);
            return ctx.lookup(name);
        } catch (Exception e) {
            throw Exc.jmsExc("Could not find JNDI object by name [" + name + "]: " + e, e);
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

        switch (domain) {
        case XConnectionRequestInfo.DOMAIN_QUEUE_NONXA:
        case XConnectionRequestInfo.DOMAIN_QUEUE_XA:
        case XConnectionRequestInfo.DOMAIN_TOPIC_NONXA:
        case XConnectionRequestInfo.DOMAIN_TOPIC_XA:
        case XConnectionRequestInfo.DOMAIN_UNIFIED_NONXA:
        case XConnectionRequestInfo.DOMAIN_UNIFIED_XA:
            Object o = getJndiObject(url, FACTORY_NAME);
            return (ConnectionFactory) o; 
        default:
            throw new JMSException("Logic fault: invalid domain " + domain);
        }
    }

    /**
     * This is called for inbound. Destinations are looked up in JNDI with an
     * optional prefix.
     * 
     * @see com.stc.jmsjca.core.RAJMSObjectFactory#createDestination(javax.jms.Session,
     *      boolean, boolean, com.stc.jmsjca.core.RAJMSActivationSpec,
     *      com.stc.jmsjca.core.RAJMSResourceAdapter, java.lang.String)
     */
    public Destination createDestination(Session sess, boolean isXA, boolean isTopic,
        RAJMSActivationSpec activationSpec, RAJMSResourceAdapter ra,
        String destName) throws JMSException {
        
        // Get the connection properties
        Properties p = new Properties();
        UrlParser url = (UrlParser) getProperties(p, ra, activationSpec, null, null);
        
        // JNDI object name
        String prefix = p.getProperty(RAWLResourceAdapter.PROP_PREFIX, "");
        if (prefix.length() > 0 && !prefix.endsWith("/")) {
            prefix += "/";
        }
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
        if (isTopic && RAJMSActivationSpec.DURABLE.equals(spec.getSubscriptionDurability())) {
            // Ensure a clientID will be set
            String newClientId = spec.getClientId();
            boolean auto = false;
            if (newClientId == null || newClientId.length() == 0) {
                newClientId = "CLIENTID-" + spec.getSubscriptionName();
                auto = false;
            }
            
            String currentClientId = connection.getClientID();
            if (currentClientId == null || currentClientId.length() == 0) {
                // Set it
                setClientID(connection, newClientId);
            } else {
                if (newClientId.equals(currentClientId)) {
                    // ok: already set
                } else {
                    if (auto) {
                        // Apparently the clientID was specified in the connection 
                        // factory, and the user did not specify a clientid
                    } else {
                        sLog.warn("ClientID is already set to [" + currentClientId
                            + "]; cannot set to [" + newClientId + "] as required in "
                            + "activationspec [" + spec + "]");
                    }
                }
            }
        }
    }

    /**
     * @see com.stc.jmsjca.core.RAJMSObjectFactory#getJMSServerType()
     */
    public String getJMSServerType() {
        return "WL";
    }
}
