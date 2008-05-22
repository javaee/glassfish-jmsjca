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

package com.stc.jmsjca.jboss;

import com.stc.jmsjca.core.RAJMSActivationSpec;
import com.stc.jmsjca.core.RAJMSObjectFactory;
import com.stc.jmsjca.core.RAJMSResourceAdapter;
import com.stc.jmsjca.core.XConnectionRequestInfo;
import com.stc.jmsjca.core.XManagedConnectionFactory;
import com.stc.jmsjca.util.Exc;
import com.stc.jmsjca.util.Logger;
import com.stc.jmsjca.util.Str;
import com.stc.jmsjca.util.UrlParser;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.JMSException;
import javax.naming.Context;
import javax.naming.InitialContext;

import java.util.Properties;

/**
 * 
 * @author fkieviet
 * @version $Revision: 1.6 $
 */
public class RAJBossObjectFactory extends RAJMSObjectFactory implements java.io.Serializable {
    private static Logger sLog = Logger.getLogger(RAJBossObjectFactory.class);
    /**
     * Protocol 1
     */
    public static final String PROT_JBOSS = "jboss";

    /**
     * Required to create an initial context
     */
    public static final String JNDI_FACTORY = "org.jnp.interfaces.NamingContextFactory";
    
    /**
     * package prefix for initial context
     */
    public static final String PKGS = "org.jboss.naming:org.jnp.interfaces";

    /**
     * The connection factory to be used (XA) 
     */
    public static final String DEFAULT_XACF = "UIL2XAConnectionFactory";

    private static final String[] URL_PREFIXES = new String[] {
        PROT_JBOSS + "://",
    };

    private static final Localizer LOCALE = Localizer.get();

    //    private static final String[] PROTOCOLS = new String[] {
//        PROT_JBOSS,
//    };
    
    /**
     * @see com.stc.jmsjca.core.RAJMSObjectFactory#adjustDeliveryMode(int, boolean)
     * 
     * Serial delivery + XA-rollback does not work
     * Serial delivery + non-transacted rollback through throw exception does not work
     * Hence, replace all serial delivery with SYNC
     */
    public int adjustDeliveryMode(int mode, boolean xa) {
        int newMode = mode;
        if (mode == RAJMSActivationSpec.DELIVERYCONCURRENCY_SERIAL) {
            newMode = RAJMSActivationSpec.DELIVERYCONCURRENCY_SYNC;
        }
        return newMode;
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
     * Looks up an object in JNDI if the prefetch is not specified; if it is specified
     * it simply returns the prefetch.
     *
     * @param name jndi name
     * @param prefetch to return immediately if not null
     * @return non-null object
     * @throws JMSException on error
     */
    private Object getJndiObject(Properties p, String name) throws JMSException {
        if (sLog.isDebugEnabled()) {
            sLog.debug("Looking up JNDI object " + name);
        }

        if (name == null || name.length() == 0) {
            throw Exc.jmsExc(LOCALE.x("E202: The JNDI name is null"));
        }

        InitialContext ctx = null;
        try {
            if (p.get(Context.INITIAL_CONTEXT_FACTORY) == null) {
                ctx = new InitialContext();
            } else {
                ctx = new InitialContext(p);
            }
            return ctx.lookup(name);
        } catch (Exception e) {
            throw Exc.jmsExc(LOCALE.x("E201: Could not find JNDI object by " +
                    "name [{0}]; properties={2}: {1}", name, e, Str.serializeProperties(p)), e);
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
        String urlstr = "jnp" + "://" + url.getHost() + ":" + url.getPort();
        
        Properties q = new Properties();
        q.setProperty(Context.INITIAL_CONTEXT_FACTORY, JNDI_FACTORY);
        q.setProperty(Context.PROVIDER_URL, urlstr);
        q.setProperty(Context.URL_PKG_PREFIXES, PKGS);
        
        //TODO: username/passsword
        
        // JNDI object name
        String name = p.getProperty(RAJBossResourceAdapter.PROP_XACF, DEFAULT_XACF);
        
        switch (domain) {
        case XConnectionRequestInfo.DOMAIN_QUEUE_NONXA:
        case XConnectionRequestInfo.DOMAIN_QUEUE_XA:
        case XConnectionRequestInfo.DOMAIN_TOPIC_NONXA:
        case XConnectionRequestInfo.DOMAIN_TOPIC_XA:
        case XConnectionRequestInfo.DOMAIN_UNIFIED_NONXA:
        case XConnectionRequestInfo.DOMAIN_UNIFIED_XA:
            Object o = getJndiObject(q, name);
            return (ConnectionFactory) o; 
        default:
            throw Exc.jmsExc(LOCALE.x("E204: Logic fault: invalid domain {0}", Integer.toString(domain)));
        }
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
                if (spec.getClientId().equals(currentClientId)) {
                    // ok: already set
                } else {
                    sLog.warn(LOCALE.x("E200: ClientID is already set to [{0}]" 
                        + "; cannot set to [{1}] as required in "
                        + "activationspec [{2}]", currentClientId, spec.getClientId(), spec)); 
                }
            }
        }
    }

    /**
     * @see com.stc.jmsjca.core.RAJMSObjectFactory#getJMSServerType()
     */
    public String getJMSServerType() {
        return "JBOSS";
    }
}
