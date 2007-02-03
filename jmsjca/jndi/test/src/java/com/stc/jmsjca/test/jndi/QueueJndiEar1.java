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
import com.stc.jmsjca.test.core.Passthrough;
import com.stc.jmsjca.test.core.QueueEndToEnd;
import com.stc.jmsjca.util.Str;

import javax.naming.Context;

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
 * @version $Revision: 1.4 $
 */
public class QueueJndiEar1 extends QueueEndToEnd {

    public static EmbeddedDescriptor getDDjndi(EmbeddedDescriptor dd, EndToEndBase test) throws Exception {
        // Set JNDI properties
        Properties p = new Properties();
        p.setProperty(Context.INITIAL_CONTEXT_FACTORY,
                InitialContextFactory.class.getName());
        p.setProperty(Context.PROVIDER_URL, "stcms://"
                + test.getJmsServerProperties().getProperty("host") + ":"
                + test.getJmsServerProperties().getProperty("stcms.instance.port"));
        p.setProperty(Context.SECURITY_PRINCIPAL, test.getJmsServerProperties()
                .getProperty("admin.user"));
        p.setProperty(Context.SECURITY_CREDENTIALS, test.getJmsServerProperties()
                .getProperty("admin.password"));
        p.setProperty(RAJNDIResourceAdapter.QUEUECF,
                "connectionfactories/xaqueueconnectionfactory");
        p.setProperty(RAJNDIResourceAdapter.TOPICCF,
                "connectionfactories/xatopicconnectionfactory");
        p.setProperty(RAJNDIResourceAdapter.UNIFIEDCF,
                "connectionfactories/xaconnectionfactory");

        // Update first RA
        StcmsConnector cc = (StcmsConnector) dd.new ResourceAdapter(RAXML)
                .createConnector(StcmsConnector.class);
        cc.setConnectionURL("jndi://");
        cc.setOptions(Str.serializeProperties(p));

        // Update second RA
        cc = (StcmsConnector) dd.new ResourceAdapter(RAXML1)
                .createConnector(StcmsConnector.class);
        cc.setConnectionURL("jndi://");
        cc.setOptions(Str.serializeProperties(p));

        return dd;
        
    }

    public EmbeddedDescriptor getDD() throws Exception {
        EmbeddedDescriptor dd = super.getDD();
        dd = QueueJndiEar1.getDDjndi(dd, this);
        return dd;
    }

    public Passthrough createPassthrough(Properties serverProperties) {
        return new JndiPassthrough(serverProperties);
    }
    
    public void disabled_testNoTransaction() {
        
    }
}
