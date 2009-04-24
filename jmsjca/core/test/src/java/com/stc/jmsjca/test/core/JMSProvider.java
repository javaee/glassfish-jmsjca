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

package com.stc.jmsjca.test.core;

import com.stc.jmsjca.container.EmbeddedDescriptor;

import java.util.Properties;

/**
 * Abstracts out the JMS Provider specific test functionality
 * 
 * @author fkieviet
 */
public abstract class JMSProvider {
    /**
     * Called after the DD is parsed and updated with the basic settings; allows for
     * provider specific changes to the DD.
     * 
     * @param dd DD
     * @return updated DD
     * @throws Exception on failure
     */
    public abstract EmbeddedDescriptor changeDD(EmbeddedDescriptor dd, BaseTestCase.JMSTestEnv test) throws Exception;
    
    /**
     * Creates a JMS Provider specific passthrough
     * 
     * @param serverProperties
     * @return new PassThrough
     */
    public abstract Passthrough createPassthrough(Properties serverProperties);

    /**
     * Provides a hook to plug in provider specific client IDs
     * 
     * @param suggestedClientID
     * @return clientid
     */
    public abstract String getClientId(String suggestedClientID);
    
    /**
     * Obtains a connection URL
     * 
     * @param test env
     * @return URL
     */
    public abstract String getConnectionUrl(BaseTestCase.JMSTestEnv test);
    
    public String createConnectionUrl(String host, int port) {
        throw new IllegalStateException("Not implemented in " + this);
    }

    /**
     * Returns the password from the properties set
     */
    public abstract String getPassword(Properties serverProperties);

    /**
     * Returns the username from the properties set
     */
    public abstract String getUserName(Properties serverProperties);

    /**
     * @return stcms, sunone, wl, etc.
     */
    public abstract String getProviderID();

    /**
     * @return true if the msg may have properties that start with JMS
     */
    public boolean isMsgPrefixOK() {
        return true;
    }
}