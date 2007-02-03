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

import com.stc.jmsjca.container.Container;
import com.stc.jmsjca.container.EmbeddedDescriptor;
import com.stc.jmsjca.test.core.EndToEndBase;
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
 * @version $Revision: 1.1.1.2 $
 */
public class SendEar1 extends QueueEndToEnd {
    
    public static EmbeddedDescriptor getDDstcms(EmbeddedDescriptor dd, EndToEndBase test) throws Exception {
        // The ra.xml does not contain a URL by default, which is fine in the case
        // the tests are running within RTS.
        if (!test.getContainerID().equals("rts")) {
            String url = "stcms://" + test.getJmsServerProperties().getProperty("host") + ":"
                + test.getJmsServerProperties().getProperty("stcms.instance.port");

            StcmsConnector cc = (StcmsConnector) dd.new ResourceAdapter(RAXML)
                .createConnector(StcmsConnector.class);
            cc.setConnectionURL(url);

            cc = (StcmsConnector) dd.new ResourceAdapter(RAXML1)
                .createConnector(StcmsConnector.class);
            cc.setConnectionURL(url);
        }
        return dd;
        
    }

    public EmbeddedDescriptor getDD() throws Exception {
        EmbeddedDescriptor dd = super.getDD();
        dd = SendEar1.getDDstcms(dd, this);
        return dd;
    }
    
    
    public void testDummy() throws Throwable {
        Thread.sleep(2000);

        // Deploy
        Container c = createContainer();
        try {
            if (c.isDeployed(mTestEar.getAbsolutePath())) {
                c.undeploy(mTestEarName);
            }
        } finally {
            Container.safeClose(c);
        }
    }
    
    public Passthrough createPassthrough(Properties serverProperties) {
        return new StcmsPassthrough(serverProperties);
    }
    
//    public void skip_testContainerManaged() throws Throwable {}
//    public void skip_testCloseInTranactionXA() throws Throwable {}
//    public void skip_testBeanManaged() throws Throwable {}
//    public void skip_testBeanManagedRBAllocateInTx() throws Throwable {}
//    public void skip_testCloseInTransactionLT() throws Throwable {}
//    public void skip_testSharedXAResources() throws Throwable {}
//    public void skip_testSharedLTResources() throws Throwable {}
//    public void skip_testReuseXAResources() throws Throwable {}
//    public void skip_testReuseLTResources() throws Throwable {}
//    public void skip_testSuspendCC() throws Throwable {}
//    public void skip_testSuspendSerial() throws Throwable {}
//    public void skip_testConcurrency() throws Throwable {}
//    public void skip_testUndeployWhenAppProcessingMessagesCC() throws Throwable {}
//    public void skip_testUndeployWhenAppProcessingMessagesSerial() throws Throwable {}
//    public void skip_testExceptionCMTSerial() throws Throwable {}
//    public void skip_testExceptionBMTSync() throws Throwable {}    
//    public void skip_testExceptionCMTCC() throws Throwable {}
//    public void skip_testExceptionBMTCC() throws Throwable {}
//    public void skip_testExceptionBMTSerial() throws Throwable {}
//    public void skip_testRequestReply0() throws Throwable {}
//    public void skip_testRequestReply1() throws Throwable {}
//    public void skip_testRequestReply2() throws Throwable {}
//    public void skip_testRequestReplyN1() throws Throwable {}
//    public void skip_testRequestReplyN2() throws Throwable {}
//    public void skip_testRequestReplyN3() throws Throwable {}
//    public void skip_testA() throws Throwable {}
//    public void skip_testDlqMoveSerialXA() throws Throwable {}
//    public void skip_testDlqMoveCCXA() throws Throwable {}
//    public void skip_testDlqMoveSerialBMT() throws Throwable {}
//    public void skip_testDlqMoveCCBMT() throws Throwable {}
//    public void skip_testDlqUndeploy() throws Throwable {}
//    public void skip_testBatchXA() throws Throwable {}
//    public void skip_testBatchXARB() throws Throwable {}
//    public void skip_testBatchUT() throws Throwable {}
//    public void skip_testBatchXAHUA() throws Throwable {}
//    public void skip_testBatchXAHUARB() throws Throwable {}
//    public void skip_testHUAXA() throws Throwable {}
//    public void skip_testHUAXARB() throws Throwable {}
//    public void skip_testHUAUT() throws Throwable {}
//    public void skip_testHUAUTRB() throws Throwable {}
                     
// CLASSCAST EXCEPTIONS:    
//    public void skip_testMultiNonSharedResources() {}
//    public void skip_testMultiXA() {}
//    public void skip_testNoTransaction() throws Throwable {}

// Weblogic bug?    
//    public void skip_testBeanManagedRBAllocateOutsideOfTx() throws Throwable {}

}
