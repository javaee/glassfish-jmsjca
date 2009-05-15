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

package com.stc.jmsjca.test.wmq;

import com.stc.jmsjca.container.EmbeddedDescriptor;
import com.stc.jmsjca.core.RAJMSResourceAdapter;
import com.stc.jmsjca.test.core.EndToEndBase;
import com.stc.jmsjca.test.core.JMSProvider;
import com.stc.jmsjca.test.core.Passthrough;
import com.stc.jmsjca.test.core.BaseTestCase.JMSTestEnv;
import com.stc.jmsjca.test.core.EndToEndBase.ConnectorConfig;
import com.stc.jmsjca.wmq.RAWMQResourceAdapter;

import java.util.Properties;

/**
 *
 * @author  fkieviet
 * @version $Revision: 1.8 $
 */
public class WMQProvider extends JMSProvider {
    public static final String PROPNAME_HOST = "jmsjca.jmsimpl.wmq.host";
    public static final String PROPNAME_PORT = "jmsjca.jmsimpl.wmq.port";
    public static final String PROPNAME_USERID = "jmsjca.jmsimpl.wmq.userid";
    public static final String PROPNAME_PASSWORD = "jmsjca.jmsimpl.wmq.password";
    public static final String QUEUEMANAGER = "jmsjca.jmsimpl.wmq.queuemanager";

    /**
     * @see com.stc.jmsjca.test.core.JMSProvider
     * #changeDD(com.stc.jmsjca.container.EmbeddedDescriptor, com.stc.jmsjca.test.core.BaseTestCase.JMSTestEnv)
     */
    @Override
    public EmbeddedDescriptor changeDD(EmbeddedDescriptor dd, JMSTestEnv test)
        throws Exception {
        
        // Update first RA
        ConnectorConfig cc = (ConnectorConfig) dd.new ResourceAdapter(EndToEndBase.RAXML)
                .createConnector(ConnectorConfig.class);
        cc.setConnectionURL(WMQPassthrough.getConnectionUrl(test.getJmsServerProperties()));

        // Update second RA
        cc = (ConnectorConfig) dd.new ResourceAdapter(EndToEndBase.RAXML1)
                .createConnector(ConnectorConfig.class);
        cc.setConnectionURL(WMQPassthrough.getConnectionUrl(test.getJmsServerProperties()));

        // Commit
        dd.update();
        
        return dd;
    }

    /**
     * Provides a hook to plug in provider specific client IDs
     * @param proposedClientId String
     * @return String 
     */
    @Override
    public String getClientId(String proposedClientId) {
        return proposedClientId;
    }

    /**
     * @see com.stc.jmsjca.test.core.JMSProvider#createPassthrough(java.util.Properties)
     */
    @Override
    public Passthrough createPassthrough(Properties serverProperties) {
        WMQPassthrough passthrough = new WMQPassthrough(serverProperties, this);
        passthrough.setCommitSize(10);
        return passthrough;
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
        return "wmq";
    }

    /**
     * @see com.stc.jmsjca.test.core.JMSProvider#isMsgPrefixOK()
     */
    @Override
    public boolean isMsgPrefixOK() {
        return false;
    }

    @Override
    public RAJMSResourceAdapter createRA() {
        return new RAWMQResourceAdapter();
    }
}
