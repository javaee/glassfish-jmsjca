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

package com.stc.jmsjca.test.stcms;

import com.stc.jmsjca.container.EmbeddedDescriptor;
import com.stc.jmsjca.test.core.BaseTestCase;
import com.stc.jmsjca.test.core.EndToEndBase;
import com.stc.jmsjca.test.core.JMSProvider;
import com.stc.jmsjca.test.core.Passthrough;
import com.stc.jmsjca.test.core.EndToEndBase.ConnectorConfig;

import java.util.Properties;

/**
 *
 * @author fkieviet
 * @version $Revision: 1.1 $
 */
public class StcmsProvider extends JMSProvider {

    /**
     * @see com.stc.jmsjca.test.core.JMSProvider
     * #changeDD(com.stc.jmsjca.container.EmbeddedDescriptor, com.stc.jmsjca.test.core.BaseTestCase.JMSTestEnv)
     */
    public EmbeddedDescriptor changeDD(EmbeddedDescriptor dd, BaseTestCase.JMSTestEnv test)
        throws Exception {
        // The ra.xml does not contain a URL by default, which is fine in
        // the case
        // the tests are running within RTS.
        if (!test.getContainerID().equals("rts")) {
            String url = "stcms://"
                + test.getJmsServerProperties().getProperty("host") + ":"
                + test.getJmsServerProperties().getProperty("stcms.instance.port");

            ConnectorConfig cc = (ConnectorConfig) dd.new ResourceAdapter(EndToEndBase.RAXML)
                .createConnector(ConnectorConfig.class);
            cc.setConnectionURL(url);

            cc = (ConnectorConfig) dd.new ResourceAdapter(EndToEndBase.RAXML1)
                .createConnector(ConnectorConfig.class);
            cc.setConnectionURL(url);
        }
        return dd;
    }

    /**
     * Provides a hook to plug in provider specific client IDs
     * 
     * @return clientId
     */
    public String getClientId(String proposedClientId) {
        return "";
    }

    /**
     * @see com.stc.jmsjca.test.core.JMSProvider
     * #createPassthrough(java.util.Properties)
     */
    public Passthrough createPassthrough(Properties serverProperties) {
        return new StcmsPassthrough(serverProperties);
    }

    /**
     * @see com.stc.jmsjca.test.core.JMSProvider#createConnectionUrl(java.lang.String, int)
     */
    public String createConnectionUrl(String host, int port) {
        return "stcms://" + host + ":" + port;
    }
}
