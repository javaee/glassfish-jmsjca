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

package com.stc.jmsjca.test.jboss;

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
 * @version $Revision: 1.1 $
 */
public class JBossProvider extends JMSProvider {

    public EmbeddedDescriptor changeDD(EmbeddedDescriptor dd, JMSTestEnv test)
    throws Exception {
        ConnectorConfig cc = (ConnectorConfig) dd.new ResourceAdapter(EndToEndBase.RAXML).createConnector(ConnectorConfig.class);
        cc.setConnectionURL(TestJBossJUStd.getConnectionUrl());
        cc.setUserName("");
        cc.setPassword("");

        cc = (ConnectorConfig) dd.new ResourceAdapter(EndToEndBase.RAXML1).createConnector(ConnectorConfig.class);
        cc.setConnectionURL(TestJBossJUStd.getConnectionUrl());
        cc.setUserName("");
        cc.setPassword("");

        return dd;
    }

    /**
     * @see com.stc.jmsjca.test.core.JMSProvider#createPassthrough(java.util.Properties)
     */
    public Passthrough createPassthrough(Properties serverProperties) {
        return new JBossPassthrough(serverProperties);
    }

    /**
     * Provides a hook to plug in provider specific client IDs
     * 
     * @return clientId
     */
    public String getClientId(String proposedClientId) {
        return proposedClientId;
    }
}