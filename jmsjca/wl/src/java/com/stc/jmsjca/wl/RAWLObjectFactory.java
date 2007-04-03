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
import javax.jms.QueueConnectionFactory;
import javax.jms.QueueSession;
import javax.jms.Session;
import javax.jms.TopicConnection;
import javax.jms.TopicConnectionFactory;
import javax.jms.TopicSession;
import javax.jms.XAConnection;
import javax.jms.XAConnectionFactory;
import javax.jms.XAQueueConnection;
import javax.jms.XAQueueConnectionFactory;
import javax.jms.XATopicConnection;
import javax.jms.XATopicConnectionFactory;
import javax.naming.Context;
import javax.naming.InitialContext;

import java.lang.reflect.InvocationTargetException;
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
 * @version $Revision: 1.5 $
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
    public static final String IS_ORBMETHOD_MARKTHREAD = "requireSe";
    
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
    private transient Method mSpecialISORBMethodMarkThread;
    
    /**
     * Constructor 
     */
    public RAWLObjectFactory() {
        try {
            Class c = Class.forName(IS_ORBCLASS);
            mSpecialISORBMethod = c.getMethod(IS_ORBMETHOD, new Class[] {});
            c = Class.forName(IS_ORBCLASS2);
            mSpecialISORBMethodMarkThread = c.getMethod(IS_ORBMETHOD_MARKTHREAD, new Class[] {boolean.class});
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
            sLog.warn(LOCALE.x("E820: Current delivery mode {0} not supported; "
                + " not supported; switching to {1}", 
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
            throw new JMSException("The JNDI name is null");
        }

        InitialContext ctx = null;
        try {
            if (mSpecialISORBMethod != null) {
                try {
                    // Works on IS only
                    armCORBA(true);
                    final Properties prop = (Properties) mSpecialISORBMethod.invoke(null, new Object[0]);
                    prop.put(Context.URL_PKG_PREFIXES, JNDI_WEBLOGIC_PROTOCOL_PACKAGES);
                    ctx = new InitialContext(prop);
                    return ctx.lookup("corbaname:iiop:1.2@" + url.getHost() + ":" + url.getPort()
                        + '#' + name); 
                } finally {
                    armCORBA(false);
                }
            } else {
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
        RAJMSActivationSpec activationSpec, XManagedConnectionFactory fact, RAJMSResourceAdapter ra,
        String destName) throws JMSException {
        
        // Get the connection properties
        Properties p = new Properties();
        UrlParser url = (UrlParser) getProperties(p, ra, activationSpec, fact, null);
        
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
                        sLog.warn(LOCALE.x("E822: ClientID is already set to [{0}]" 
                            + "; cannot set to [{1}] as required in "
                            + "activationspec [{2}]", currentClientId, newClientId, spec));
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
    
    private static final Object[] PUSH = new Object[] {Boolean.TRUE};
    private static final Object[] POP = new Object[] {Boolean.FALSE};
    
    /**
     * Marks the thread as CORBA SE
     * 
     * @param ispush true to mark, false to unmark
     * @throws JMSException on failure
     */
    private void armCORBA(boolean ispush) throws JMSException {
        if (mSpecialISORBMethodMarkThread != null) {
            try {
                mSpecialISORBMethodMarkThread.invoke(null, ispush ? PUSH : POP);
            } catch (Exception e) {
                throw Exc.jmsExc(LOCALE.x("E823: CORBA push/pop failure: {0}", e), e);
            }
        }
    }
    
    /**
     * Need to mark the thread as a CORBA SE call
     * 
     * @see com.stc.jmsjca.core.RAJMSObjectFactory#createConnection(java.lang.Object, 
     *  int, com.stc.jmsjca.core.RAJMSActivationSpec, com.stc.jmsjca.core.RAJMSResourceAdapter, 
     *  java.lang.String, java.lang.String)
     */
    public Connection createConnection(Object fact, int domain,
        RAJMSActivationSpec activationSpec, RAJMSResourceAdapter ra, String username,
        String password) throws JMSException {
        armCORBA(true);
        try {
            return super.createConnection(fact, domain, activationSpec, ra, username, password);
        } finally {
            armCORBA(false);
        }
    }
}
