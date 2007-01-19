/*
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the "License").  You may not use this file except
 * in compliance with the License.
 *
 * You can obtain a copy of the license at
 * https://glassfish.dev.java.net/public/CDDLv1.0.html.
 * See the License for the specific language governing
 * permissions and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL
 * HEADER in each file and include the License file at
 * https://glassfish.dev.java.net/public/CDDLv1.0.html.
 * If applicable add the following below this CDDL HEADER,
 * with the fields enclosed by brackets "[]" replaced with
 * your own identifying information: Portions Copyright
 * [year] [name of copyright owner]
 */
/*
 * $RCSfile: QueueSunOneEar1.java,v $
 * $Revision: 1.1.1.1 $
 * $Date: 2007-01-19 22:28:53 $
 *
 * Copyright 2003-2007 Sun Microsystems, Inc. All Rights Reserved.  
 */

package com.stc.jmsjca.test.sunone;

import com.stc.jmsjca.container.EmbeddedDescriptor;
import com.stc.jmsjca.test.core.Passthrough;
import com.stc.jmsjca.test.core.QueueEndToEnd;

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
 * @version $Revision: 1.1.1.1 $
 */
public class QueueSunOneEar1 extends QueueEndToEnd {

    /**
     * @see com.stc.jmsjca.test.core.EndToEndBase#getDD()
     */
    public EmbeddedDescriptor getDD() throws Exception {
        EmbeddedDescriptor dd = super.getDD();

        StcmsConnector cc = (StcmsConnector) dd.new ResourceAdapter(RAXML).createConnector(StcmsConnector.class);
        cc.setConnectionURL("mq://" + mServerProperties.getProperty("host") + ":" + mServerProperties.getProperty("stcms.instance.port"));

        cc = (StcmsConnector) dd.new ResourceAdapter(RAXML1).createConnector(StcmsConnector.class);
        cc.setConnectionURL("mq://" + mServerProperties.getProperty("host") + ":" + mServerProperties.getProperty("stcms.instance.port"));

        return dd;
    }

    /**
     * @see com.stc.jmsjca.test.core.EndToEndBase#createPassthrough(java.util.Properties)
     */
    public Passthrough createPassthrough(Properties serverProperties) {
        SunOnePassthrough sunOnePassthrough = new SunOnePassthrough(serverProperties);
        sunOnePassthrough.setCommitSize(1);
        return sunOnePassthrough;
    }
    
    /**
     * JMQ does not adhere to this:
     * tempdest = s.createTempQueue();
     * tempdest.delete();
     * tempdest.delete(); <-- does not throw
     */
    public void disabled_testA() {
    }

    /**
     * JMQ does not adhere to this:
     * tempdest = s.createTempQueue();
     * tempdest.delete();
     * tempdest.delete(); <-- does not throw
     * 
     * Disabled: N2 and N3
     */
    public void testRequestReply0() throws Throwable {
        dotest0(new String[] { "requestReply0", "requestReply1", "requestReply2",
            "requestReplyN1"  });
    }
}
