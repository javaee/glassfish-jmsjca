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

package com.stc.jmsjca.test.unifiedjms;

import com.stc.jmsjca.container.EmbeddedDescriptor;
import com.stc.jmsjca.core.RAJMSResourceAdapter;
import com.stc.jmsjca.test.core.JMSProvider;
import com.stc.jmsjca.test.core.Passthrough;
import com.stc.jmsjca.test.core.BaseTestCase.JMSTestEnv;

import java.util.Properties;

/**
 * Delegates all methods except getProviderID() to the delegate
 * 
 * @author fkieviet
 */
public class UnifiedProvider extends JMSProvider {
    JMSProvider delegate;
    
    public UnifiedProvider(String providerClassName) throws Exception {
        delegate = (JMSProvider) (Class.forName(providerClassName)).newInstance();
    }
    
    /**
     * OVERRIDDEN!
     */
    @Override
    public String getProviderID() {
        return "unifiedjms";
    }

    
    /**
     * @see com.stc.jmsjca.test.core.JMSProvider#changeDD(com.stc.jmsjca.container.EmbeddedDescriptor, com.stc.jmsjca.test.core.BaseTestCase.JMSTestEnv)
     */
    @Override
    public EmbeddedDescriptor changeDD(EmbeddedDescriptor dd, JMSTestEnv test)
        throws Exception {
        return delegate.changeDD(dd, test);
    }

    /**
     * @see com.stc.jmsjca.test.core.JMSProvider#createConnectionUrl(java.lang.String, int)
     */
    @Override
    public String createConnectionUrl(String host, int port) {
        return delegate.createConnectionUrl(host, port);
    }

    /**
     * @see com.stc.jmsjca.test.core.JMSProvider#createLocalPassthrough(java.util.Properties)
     */
    @Override
    public Passthrough createLocalPassthrough(Properties serverProperties) {
        return delegate.createLocalPassthrough(serverProperties);
    }

    /**
     * @see com.stc.jmsjca.test.core.JMSProvider#createPassthrough(java.util.Properties)
     */
    @Override
    public Passthrough createPassthrough(Properties serverProperties) {
        return delegate.createPassthrough(serverProperties);
    }

    /**
     * @see com.stc.jmsjca.test.core.JMSProvider#createRA()
     */
    @Override
    public RAJMSResourceAdapter createRA() {
        return delegate.createRA();
    }

    /**
     * @see com.stc.jmsjca.test.core.JMSProvider#getClientId(java.lang.String)
     */
    @Override
    public String getClientId(String suggestedClientID) {
        return delegate.getClientId(suggestedClientID);
    }

    /**
     * @see com.stc.jmsjca.test.core.JMSProvider#getConnectionUrl(com.stc.jmsjca.test.core.BaseTestCase.JMSTestEnv)
     */
    @Override
    public String getConnectionUrl(JMSTestEnv test) {
        return delegate.getConnectionUrl(test);
    }

    /**
     * @see com.stc.jmsjca.test.core.JMSProvider#getPassword(java.util.Properties)
     */
    @Override
    public String getPassword(Properties serverProperties) {
        return delegate.getPassword(serverProperties);
    }

    /**
     * @see com.stc.jmsjca.test.core.JMSProvider#getUserName(java.util.Properties)
     */
    @Override
    public String getUserName(Properties serverProperties) {
        return delegate.getUserName(serverProperties);
    }

    /**
     * @see com.stc.jmsjca.test.core.JMSProvider#isMsgPrefixOK()
     */
    @Override
    public boolean isMsgPrefixOK() {
        return delegate.isMsgPrefixOK();
    }
}
