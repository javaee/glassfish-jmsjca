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

package com.stc.jmsjca.jndi;

import com.stc.jmsjca.core.AdminDestination;
import com.stc.jmsjca.core.RAJMSActivationSpec;
import com.stc.jmsjca.core.RAJMSObjectFactory;
import com.stc.jmsjca.core.RAJMSResourceAdapter;
import com.stc.jmsjca.core.SessionConnection;
import com.stc.jmsjca.core.XConnectionRequestInfo;
import com.stc.jmsjca.core.XManagedConnection;
import com.stc.jmsjca.core.XManagedConnectionFactory;
import com.stc.jmsjca.util.ConnectionUrl;
import com.stc.jmsjca.util.Exc;
import com.stc.jmsjca.util.Logger;

import javax.jms.ConnectionFactory;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.QueueConnectionFactory;
import javax.jms.QueueSession;
import javax.jms.Session;
import javax.jms.TopicConnectionFactory;
import javax.jms.TopicSession;
import javax.jms.XAQueueConnectionFactory;
import javax.jms.XAQueueSession;
import javax.jms.XATopicConnectionFactory;
import javax.jms.XATopicSession;
import javax.naming.Context;
import javax.naming.InitialContext;

import java.io.Serializable;
import java.util.Properties;

/**
 * For JNDI provider
 *
 * @author Frank Kieviet
 * @version $Revision: 1.6 $
 */
public class RAJNDIObjectFactory extends RAJMSObjectFactory implements Serializable {
    private static Logger sLog = Logger.getLogger(RAJNDIObjectFactory.class);

    /**
     * Used to mark destinations as lookup in JNDI
     */
    public static final String JNDI_PREFIX = "jndi://";

    private static final String[] URL_PREFIXES = new String[] {
            "jndi://"
    };

    private static final Localizer LOCALE = Localizer.get();

    /**
     * Tool function: closes a context w/o exception
     *
     * @param ctx context to close
     */
    private void safeClose(InitialContext ctx) {
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
     * @throws ResourceException on error
     */
    Object getJndiObject(Properties p, String name) throws JMSException {
        if (sLog.isDebugEnabled()) {
            sLog.debug("Looking up JNDI object " + name);
        }

        if (name == null || name.length() == 0) {
            throw new JMSException("The JNDI name is null");
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
            throw Exc.jmsExc(LOCALE.x("E400: Could not find JNDI object by name [{0}]: {1}", name, e), e);
        } finally {
            safeClose(ctx);
        }
    }

    /**
     * Gets a jndi object
     *
     * @param resourceAdapter boolean
     * @param activationSpec RAJMSActivationSpec
     * @param fact RAJMSResourceAdapter
     * @param overrideUrl override URL: don't use URL from RA, CF, or activation spec (may be null)
     * @param name jndi name
     * @return ConnectionFactory
     * @throws JMSException failure
     */
    public Object getJndiObject(RAJMSResourceAdapter resourceAdapter,
        RAJMSActivationSpec activationSpec, XManagedConnectionFactory fact,
        String overrideUrl, String name) throws JMSException {

        // Get the connection properties
        Properties p = new Properties();
        getProperties(p, resourceAdapter, activationSpec, fact, overrideUrl);
        
        return getJndiObject(p, name);
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
        getProperties(p, resourceAdapter, activationSpec, fact, overrideUrl);

        switch (domain) {
        case XConnectionRequestInfo.DOMAIN_QUEUE_NONXA:
            return (QueueConnectionFactory) getJndiObject(p,
                p.getProperty(RAJNDIResourceAdapter.QUEUECF));
        case XConnectionRequestInfo.DOMAIN_QUEUE_XA:
            return (XAQueueConnectionFactory) getJndiObject(p,
                    p.getProperty(RAJNDIResourceAdapter.QUEUECF));
        case XConnectionRequestInfo.DOMAIN_TOPIC_NONXA:
            return (TopicConnectionFactory) getJndiObject(p,
                p.getProperty(RAJNDIResourceAdapter.TOPICCF));
        case XConnectionRequestInfo.DOMAIN_TOPIC_XA:
            return (XATopicConnectionFactory) getJndiObject(p,
                    p.getProperty(RAJNDIResourceAdapter.TOPICCF));
        case XConnectionRequestInfo.DOMAIN_UNIFIED_NONXA:
        case XConnectionRequestInfo.DOMAIN_UNIFIED_XA:
            return (ConnectionFactory) getJndiObject(p,
                    p.getProperty(RAJNDIResourceAdapter.UNIFIEDCF));
        default:
            throw new JMSException("Logic fault: invalid domain " + domain);
        }
    }

    /**
     * Gets the connection type properties and connection URL
     * 
     * @param p properties to fill in
     * @param resourceAdapter resouce adapter
     * @param spec activation spec
     * @param fact factory
     * @param overrideUrl optional URL specified in createConnection(URL, password)
     * @return url parser
     * @throws JMSException on incorrect URL
     */
    public ConnectionUrl getProperties(Properties p, RAJMSResourceAdapter resourceAdapter, RAJMSActivationSpec spec,
            XManagedConnectionFactory fact, String overrideUrl) throws JMSException {
        ConnectionUrl url = super.getProperties(p, resourceAdapter, spec, fact, overrideUrl);

        if (resourceAdapter instanceof RAJNDIResourceAdapter) {
            RAJNDIResourceAdapter ra = (RAJNDIResourceAdapter) resourceAdapter;
            // Get properties from RA
            if (ra.getQueueConnectionFactoryJndiName() != null) {
                p.setProperty(RAJNDIResourceAdapter.QUEUECF, ra.getQueueConnectionFactoryJndiName());
            }
            if (ra.getTopicConnectionFactoryJndiName() != null) {
                p.setProperty(RAJNDIResourceAdapter.TOPICCF, ra.getTopicConnectionFactoryJndiName());
            }
            if (ra.getUnifiedConnectionFactoryJndiName() != null) {
                p.setProperty(RAJNDIResourceAdapter.UNIFIEDCF, ra.getUnifiedConnectionFactoryJndiName());
            }
            if (ra.getInitialContextFactory() != null) {
                p.setProperty(Context.INITIAL_CONTEXT_FACTORY, ra.getInitialContextFactory());
            }
            if (ra.getProviderUrl() != null) {
                p.setProperty(Context.PROVIDER_URL, ra.getProviderUrl());
            }
        }

        return url;
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

        return new JNDISessionConnection(connectionFactory, objfact, ra,
            mc, descr, isXa, isTransacted, acknowledgmentMode,
            sessionClass);
    }
    
    /**
     * @see com.stc.jmsjca.core.RAJMSObjectFactory#createDestination(javax.jms.Session, 
     * boolean, boolean, com.stc.jmsjca.core.RAJMSActivationSpec, 
     * com.stc.jmsjca.core.XManagedConnectionFactory, 
     * com.stc.jmsjca.core.RAJMSResourceAdapter, java.lang.String)
     */
    public Destination createDestination(Session sess, boolean isXA, boolean isTopic,
        RAJMSActivationSpec activationSpec, XManagedConnectionFactory fact,  RAJMSResourceAdapter ra,
        String destName) throws JMSException {
        
        // Check for local JNDI
        if (sLog.isDebugEnabled()) {
            sLog.debug("createDestination(" + destName + ")");
        }
        Destination ret = adminDestinationLookup(destName);
        
        // Unwrap admin destination
        if (ret != null && ret instanceof AdminDestination) {
            destName = ((AdminDestination) ret).getName();
            if (sLog.isDebugEnabled()) {
                sLog.debug(ret + " is an admin object: embedded name: " + destName);
            }
            ret = null;
        }
        
        // Check for jndi://
        if (ret == null && destName.startsWith(JNDI_PREFIX)) {
            String name = destName.substring(JNDI_PREFIX.length());
            if (sLog.isDebugEnabled()) {
                sLog.debug(destName + " is a jndi object: looking up [" + name + "]");
            }
            ret = (Destination) getJndiObject(ra, activationSpec, fact, null, name);
        }
        
        // Create if necessary
        if (ret == null) {
            if (sLog.isDebugEnabled()) {
                sLog.debug("Creating " + destName + " using createQueue()/createTopic()");
            }
            if (isXA) {
                if (isTopic) {
                    ret = ((XATopicSession) sess).getTopicSession().createTopic(destName);
                } else {
                    ret = ((XAQueueSession) sess).getQueueSession().createQueue(destName);
                }
            } else {
                if (isTopic) {
                    ret = sess.createTopic(destName);
                } else {
                    ret = sess.createQueue(destName);
                }
            }
        }
        
        return ret;
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
     * @see com.stc.jmsjca.core.RAJMSObjectFactory#getJMSServerType()
     */
    public String getJMSServerType() {
        return "GENERIC";
    }
}
