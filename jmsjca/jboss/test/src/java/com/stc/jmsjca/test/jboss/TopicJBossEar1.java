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

import com.stc.jmsjca.test.core.Passthrough;
import com.stc.jmsjca.test.core.TopicEndToEnd;
import com.stc.jmsjca.container.EmbeddedDescriptor;

import java.util.Properties;

/**
 * Required:
 * test.server.properties = path to properties file containing server config
 * test.ear.path          = path to ear file to be tested
 *
 * Example for Eclipse:
 *     -Dtest.server.properties=../../R1/logicalhost/testsettings.properties -Dtest.ear.path=rastcms/test/rastcms-test.ear
 * with working directory
 *     ${workspace_loc:e-jmsjca/build}
 *
 * @author fkieviet
 * @author cye
 * @version $Revision: 1.1.1.2 $
 */
public class TopicJBossEar1 extends TopicEndToEnd {
        
    /**
     * @see com.stc.jmsjca.test.core.EndToEndBase#getDD()
     */
    public EmbeddedDescriptor getDD() throws Exception {
        EmbeddedDescriptor dd = super.getDD();
        StcmsConnector cc = (StcmsConnector) dd.new ResourceAdapter(RAXML).createConnector(StcmsConnector.class);
        cc.setConnectionURL(TestJBossJUStd.getConnectionUrl());
        cc.setUserName("");
        cc.setPassword("");

        cc = (StcmsConnector) dd.new ResourceAdapter(RAXML1).createConnector(StcmsConnector.class);
        cc.setConnectionURL(TestJBossJUStd.getConnectionUrl());
        cc.setUserName("");
        cc.setPassword("");

        return dd;
    }
    

    public Passthrough createPassthrough(Properties serverProperties) {
        return new JBossPassthrough(new Properties());
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