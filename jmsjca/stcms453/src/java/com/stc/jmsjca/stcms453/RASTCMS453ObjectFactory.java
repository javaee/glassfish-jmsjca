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

import java.util.Properties;

/**
 * Encapsulates the configuration of a MessageEndpoint.
 *
 * @author Frank Kieviet
 * @version $Revision: 1.6 $
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
        
        Class clazz;
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
    public String getJMSServerType() {
        return "STCMS453";
    }
    
    /**
     * @see com.stc.jmsjca.core.RAJMSObjectFactory#shouldUseProducerPooling()
     */
    public boolean shouldUseProducerPooling() {
        return true;
    }
}
