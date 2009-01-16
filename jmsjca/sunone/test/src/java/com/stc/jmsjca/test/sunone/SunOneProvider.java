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

package com.stc.jmsjca.test.sunone;


import com.stc.jmsjca.container.EmbeddedDescriptor;
import com.stc.jmsjca.test.core.EndToEndBase;
import com.stc.jmsjca.test.core.JMSProvider;
import com.stc.jmsjca.test.core.Passthrough;
import com.stc.jmsjca.test.core.BaseTestCase.JMSTestEnv;
import com.stc.jmsjca.test.core.EndToEndBase.ConnectorConfig;

import java.util.Properties;

/**
 *
 * @author fkieviet
 * @version $Revision: 1.2 $
 */
public class SunOneProvider extends JMSProvider {
    public static final String PROPNAME_HOST = "jmsjca.jmsimpl.sunone.host";
    public static final String PROPNAME_PORT = "jmsjca.jmsimpl.sunone.port";
    public static final String PROPNAME_USERID = "jmsjca.jmsimpl.sunone.userid";
    public static final String PROPNAME_PASSWORD = "jmsjca.jmsimpl.sunone.password";

    /**
     * @param dd
     * @param test
     * @return
     * @throws Exception
     */
    public EmbeddedDescriptor changeDD(EmbeddedDescriptor dd, JMSTestEnv test)
        throws Exception {
        ConnectorConfig cc = (ConnectorConfig) dd.new ResourceAdapter(EndToEndBase.RAXML).createConnector(ConnectorConfig.class);
        cc.setConnectionURL("mq://" + test.getJmsServerProperties().getProperty(SunOneProvider.PROPNAME_HOST) + ":" + test.getJmsServerProperties().getProperty(SunOneProvider.PROPNAME_PORT));

        cc = (ConnectorConfig) dd.new ResourceAdapter(EndToEndBase.RAXML1).createConnector(ConnectorConfig.class);
        cc.setConnectionURL("mq://" + test.getJmsServerProperties().getProperty(SunOneProvider.PROPNAME_HOST) + ":" + test.getJmsServerProperties().getProperty(SunOneProvider.PROPNAME_PORT));

        return dd;
    }

    /**
     * Provides a hook to plug in provider specific client IDs
     * 
     * @return clientId
     */
    public String getClientId(String proposedClientId) {
        return proposedClientId;
    }

    /**
     * @param serverProperties
     * @return
     */
    public Passthrough createPassthrough(Properties serverProperties) {
        SunOnePassthrough sunOnePassthrough = new SunOnePassthrough(serverProperties, this);
        sunOnePassthrough.setCommitSize(1);
        return sunOnePassthrough;
    }

    /**
     * @see com.stc.jmsjca.test.core.JMSProvider#createConnectionUrl(java.lang.String, int)
     */
    public String createConnectionUrl(String host, int port) {
        return "mq://" + host + ":" + port;
    }

    /**
     * @see com.stc.jmsjca.test.core.JMSProvider#getConnectionUrl(com.stc.jmsjca.test.core.BaseTestCase.JMSTestEnv)
     */
    public String getConnectionUrl(JMSTestEnv test) {
        String host = test.getJmsServerProperties().getProperty(PROPNAME_HOST);
        int port = Integer.parseInt(test.getJmsServerProperties().getProperty(PROPNAME_PORT));
        return createConnectionUrl(host, port);
    }

    /**
     * @see com.stc.jmsjca.test.core.JMSProvider#getPassword(java.util.Properties)
     */
    public String getPassword(Properties serverProperties) {
        return serverProperties.getProperty(PROPNAME_PASSWORD);
    }

    /**
     * @see com.stc.jmsjca.test.core.JMSProvider#getUserName(java.util.Properties)
     */
    public String getUserName(Properties serverProperties) {
        return serverProperties.getProperty(PROPNAME_USERID);
    }
}
