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

package com.stc.jmsjca.test.jndi;

import com.stc.jms.jndispi.InitialContextFactory;
import com.stc.jmsjca.container.EmbeddedDescriptor;
import com.stc.jmsjca.jndi.RAJNDIResourceAdapter;
import com.stc.jmsjca.test.core.EndToEndBase;
import com.stc.jmsjca.test.core.JMSProvider;
import com.stc.jmsjca.test.core.Passthrough;
import com.stc.jmsjca.test.core.BaseTestCase.JMSTestEnv;
import com.stc.jmsjca.test.core.EndToEndBase.ConnectorConfig;
import com.stc.jmsjca.util.Str;

import javax.naming.Context;

import java.util.Properties;

/**
 *
 * @author fkieviet
 * @version $Revision: 1.4 $
 */
public class JndiProvider extends JMSProvider {
    public static final String PROPNAME_HOST = "jmsjca.jmsimpl.jndi.host";
    public static final String PROPNAME_PORT = "jmsjca.jmsimpl.jndi.port";
    public static final String PROPNAME_USERID = "jmsjca.jmsimpl.jndi.userid";
    public static final String PROPNAME_PASSWORD = "jmsjca.jmsimpl.jndi.password";

    /**
     * @see com.stc.jmsjca.test.core.JMSProvider
     *   #changeDD(com.stc.jmsjca.container.EmbeddedDescriptor, com.stc.jmsjca.test.core.BaseTestCase.JMSTestEnv)
     */
    @Override
    public EmbeddedDescriptor changeDD(EmbeddedDescriptor dd, JMSTestEnv test)
        throws Exception {
        // Set JNDI properties
        Properties p = new Properties();
        p.setProperty(Context.INITIAL_CONTEXT_FACTORY,
                InitialContextFactory.class.getName());
        p.setProperty(Context.PROVIDER_URL, "stcms://"
                + test.getJmsServerProperties().getProperty(JndiProvider.PROPNAME_HOST) + ":"
                + test.getJmsServerProperties().getProperty(JndiProvider.PROPNAME_PORT));
        p.setProperty(Context.SECURITY_PRINCIPAL, test.getJmsServerProperties()
                .getProperty(JndiProvider.PROPNAME_USERID));
        p.setProperty(Context.SECURITY_CREDENTIALS, test.getJmsServerProperties()
                .getProperty(JndiProvider.PROPNAME_PASSWORD));
        p.setProperty(RAJNDIResourceAdapter.QUEUECF,
                "connectionfactories/xaqueueconnectionfactory");
        p.setProperty(RAJNDIResourceAdapter.TOPICCF,
                "connectionfactories/xatopicconnectionfactory");
        p.setProperty(RAJNDIResourceAdapter.UNIFIEDCF,
                "connectionfactories/xaconnectionfactory");
        p.setProperty("com.stc.jms.autocommitxa", "true");

        // Update first RA
        ConnectorConfig cc = (ConnectorConfig) dd.new ResourceAdapter(EndToEndBase.RAXML)
                .createConnector(ConnectorConfig.class);
        cc.setConnectionURL("jndi://");
        cc.setOptions(Str.serializeProperties(p));

        // Update second RA
        cc = (ConnectorConfig) dd.new ResourceAdapter(EndToEndBase.RAXML1)
                .createConnector(ConnectorConfig.class);
        cc.setConnectionURL("jndi://");
        cc.setOptions(Str.serializeProperties(p));

        return dd;
    }

    /**
     * Provides a hook to plug in provider specific client IDs
     * 
     * @return clientId
     */
    @Override
    public String getClientId(String proposedClientId) {
        return "";
    }

    /**
     * @see com.stc.jmsjca.test.core.JMSProvider#createPassthrough(java.util.Properties)
     */
    @Override
    public Passthrough createPassthrough(Properties serverProperties) {
        return new JndiPassthrough(serverProperties, this);
    }

    /**
     * @see com.stc.jmsjca.test.core.JMSProvider#getConnectionUrl(com.stc.jmsjca.test.core.BaseTestCase.JMSTestEnv)
     */
    @Override
    public String getConnectionUrl(JMSTestEnv test) {
        String host = test.getJmsServerProperties().getProperty(PROPNAME_HOST);
        int port = Integer.parseInt(test.getJmsServerProperties().getProperty(PROPNAME_PORT));
        return createConnectionUrl(host, port);
    }

    /**
     * @see com.stc.jmsjca.test.core.JMSProvider#getPassword(java.util.Properties)
     */
    @Override
    public String getPassword(Properties serverProperties) {
        return serverProperties.getProperty(PROPNAME_PASSWORD);
    }

    /**
     * @see com.stc.jmsjca.test.core.JMSProvider#getUserName(java.util.Properties)
     */
    @Override
    public String getUserName(Properties serverProperties) {
        return serverProperties.getProperty(PROPNAME_USERID);
    }

    /**
     * @see com.stc.jmsjca.test.core.JMSProvider#getProviderID()
     */
    @Override
    public String getProviderID() {
        return "jndi";
    }
}
