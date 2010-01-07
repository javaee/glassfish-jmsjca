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

package com.stc.jmsjca.stcms453;

import com.stc.jmsjca.core.Activation;
import com.stc.jmsjca.core.RAJMSActivationSpec;
import com.stc.jmsjca.core.RAJMSObjectFactory;
import com.stc.jmsjca.core.RAJMSResourceAdapter;
import com.stc.jmsjca.core.XConnectionRequestInfo;
import com.stc.jmsjca.core.XManagedConnectionFactory;
import com.stc.jmsjca.util.ClassLoaderHelper;
import com.stc.jmsjca.util.ConnectionUrl;
import com.stc.jmsjca.util.Exc;
import com.stc.jmsjca.util.UrlParser;

import javax.jms.ConnectionFactory;
import javax.jms.JMSException;
import javax.jms.Session;
import javax.transaction.xa.XAException;
import javax.transaction.xa.XAResource;
import javax.transaction.xa.Xid;

import java.util.Properties;

/**
 * Encapsulates the configuration of a MessageEndpoint.
 *
 * @author Frank Kieviet
 * @version $Revision: 1.9 $
 */
public class RASTCMS453ObjectFactory extends RAJMSObjectFactory implements java.io.Serializable {
    private static final Localizer LOCALE = Localizer.get();
    /**
     * Protocol without SSL
     */
    public static final String PROT = "stcms453";

    /**
     * @see com.stc.jmsjca.core.RAJMSObjectFactory#adjustDeliveryMode(int, boolean)
     */
    @Override
    public int adjustDeliveryMode(int mode, boolean xa) {
        int newMode = mode;
        if (mode != RAJMSActivationSpec.DELIVERYCONCURRENCY_SYNC) {
            newMode = RAJMSActivationSpec.DELIVERYCONCURRENCY_SYNC;
//            sLog.warn("Current delivery mode ["
//                + RAJMSActivationSpec.DELIVERYCONCURRENCY_STRS[mode]
//                + "] not supported; switching to ["
//                + RAJMSActivationSpec.DELIVERYCONCURRENCY_STRS[newMode] + "]");
        }
        return newMode;
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
        boolean hasChanged = false;
        UrlParser url = (UrlParser) aurl;

        // protocol
        if (PROT.equals(url.getProtocol())) {
            // ...
        } else {
            throw Exc.jmsExc(LOCALE.x("E306: Invalid protocol [{0}]:"
                + " should be ''stcms453''",  url.getProtocol()));
        }

        // Check port
        int port = url.getPort();
        if (port <= 0) {
            throw Exc.jmsExc(LOCALE.x("E307: No port specified in URL [{0}]", url));
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
        // Get the connection properties
        Properties p = new Properties();
        try {
            UrlParser url = (UrlParser) getProperties(p, resourceAdapter, activationSpec, fact, overrideUrl);

            // Port
            int port = url.getPort();
            p.setProperty("com.seebeyond.jms.sockets.ServerHost", url.getHost());
            p.setProperty("com.seebeyond.jms.sockets.ServerPort", Integer.toString(port));
        } catch (Exception ex) {
            JMSException tothrow = new JMSException("Invalid url: " + ex);
            tothrow.initCause(ex);
            throw tothrow;
        }

        String classname;
        switch (domain) {
            case XConnectionRequestInfo.DOMAIN_QUEUE_NONXA:
                classname = "com.seebeyond.jms.client.STCQueueConnectionFactory";
                break;
            case XConnectionRequestInfo.DOMAIN_QUEUE_XA:
                classname = "com.seebeyond.jms.client.STCXAQueueConnectionFactory";
                break;
            case XConnectionRequestInfo.DOMAIN_TOPIC_NONXA:
                classname = "com.seebeyond.jms.client.STCTopicConnectionFactory";
                break;
            case XConnectionRequestInfo.DOMAIN_TOPIC_XA:
                classname = "com.seebeyond.jms.client.STCXATopicConnectionFactory";
                break;
            case XConnectionRequestInfo.DOMAIN_UNIFIED_NONXA:
            case XConnectionRequestInfo.DOMAIN_UNIFIED_XA:
                throw Exc.jmsExc(LOCALE.x("E308: Unified domain not supported in 4.5.3"));
            default:
                throw Exc.jmsExc(LOCALE.x("E309: Logic fault: invalid domain {0}", Integer.toString(domain)));
        }
        
        Class<?> clazz;
        try {
            clazz = ClassLoaderHelper.loadClass(classname);
            return (ConnectionFactory) clazz.getConstructor(
                new Class[] {Properties.class}).newInstance(new Object[] {p});
        } catch (Exception e) {
            throw Exc.jmsExc(LOCALE.x("E300: Failed to instantiate connection factory: {0}", e), e);
        }
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
            if (url.startsWith(PROT)) {
                return true;
            }
        }
        return false;
    }

    /**
     * @see com.stc.jmsjca.core.RAJMSObjectFactory#getJMSServerType()
     */
    @Override
    public String getJMSServerType() {
        return "STCMS453";
    }
    
    /**
     * @see com.stc.jmsjca.core.RAJMSObjectFactory#shouldUseProducerPooling()
     */
    @Override
    public boolean shouldUseProducerPooling() {
        return true;
    }

    @Override
    public RAJMSActivationSpec createActivationSpec() {
        return new RASTCMS453ActivationSpec();
    }

    
    /**
     * @see com.stc.jmsjca.core.RAJMSObjectFactory#getXAResource(com.stc.jmsjca.core.Activation, boolean, javax.jms.Session)
     * 
     * Intercepts exceptions on start() on the XAResource. In such event, a reconnect 
     * is started.
     * 
     * In the case the connection with the JMS server is lost the receive() operation 
     * in 453 will not throw an exception. As a result, a connection failure is not
     * detected and automatic reconnect is not happening. 
     * 
     * Before calling receive(), JMSJCA will call beforeDelivery() which will call
     * XAResource.start(). This will throw an exception in the case of a connection 
     * failure. However, this exception is not propagated back to JMSJCA through the 
     * beforeDelivery() method.
     * 
     * The wrapper will intercept the exception and do the restart at that moment.
     */
    @Override
    public XAResource getXAResource(final Activation activation, boolean isXA, Session sess) throws JMSException {
        final XAResource unwrapped = getXAResource(isXA, sess);
        if (unwrapped == null) {
            return null;
        }
        
        XAResource panicWrapper = new XAResource() {

            public void commit(Xid xid, boolean onePhase) throws XAException {
                unwrapped.commit(xid, onePhase);
            }

            public void end(Xid xid, int flags) throws XAException {
                unwrapped.end(xid, flags);
            }

            public void forget(Xid xid) throws XAException {
                unwrapped.forget(xid);
            }

            public int getTransactionTimeout() throws XAException {
                return unwrapped.getTransactionTimeout();
            }

            /**
             * @see javax.transaction.xa.XAResource#isSameRM(javax.transaction.xa.XAResource)
             * 
             */
            public boolean isSameRM(XAResource arg0) throws XAException {
                return false;
            }

            public int prepare(Xid xid) throws XAException {
                return unwrapped.prepare(xid);
            }

            public Xid[] recover(int flag) throws XAException {
                return unwrapped.recover(flag);
            }

            public void rollback(Xid xid) throws XAException {
                unwrapped.rollback(xid);
            }

            public boolean setTransactionTimeout(int timeout) throws XAException {
                return unwrapped.setTransactionTimeout(timeout);
            }

            public void start(Xid xid, int flags) throws XAException {
                try {
                    unwrapped.start(xid, flags);
                } catch (Exception e) {
                    activation.distress(e);
                }
            }
        };
        
        return panicWrapper;
    }    
}
