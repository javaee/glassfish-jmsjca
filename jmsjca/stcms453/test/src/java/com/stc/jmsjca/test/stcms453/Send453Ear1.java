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

package com.stc.jmsjca.test.stcms453;

import com.stc.jmsjca.test.core.JMSProvider;
import com.stc.jmsjca.test.core.QueueEndToEnd;

/**
 * 
 * 
 * Sample current directory settings:
 * ${workspace_loc:jmsjca/build}
 *
 * @author fkieviet
 * @author cye
 * @version $Revision: 1.6 $
 */
public class Send453Ear1 extends QueueEndToEnd {
    /**
     * Test OK
     * @see com.stc.jmsjca.test.core.QueueEndToEnd#testBeanManaged()
     */
    public void testBeanManaged() throws Throwable {
        super.testBeanManaged();
    }

    /**
     * TEST OK
     * @see com.stc.jmsjca.test.core.QueueEndToEnd#testCloseInTranactionXA()
     */
    public void testCloseInTranactionXA() throws Throwable {
        super.testCloseInTranactionXA();
    }

    /**
     * TEST OK
     * @see com.stc.jmsjca.test.core.QueueEndToEnd#testCloseInTransactionLT()
     */
    public void testCloseInTransactionLT() throws Throwable {
        super.testCloseInTransactionLT();
    }

    /**
     * Test OK
     * @see com.stc.jmsjca.test.core.QueueEndToEnd#testConcurrency()
     */
    public void testConcurrency() throws Throwable {
        super.testConcurrency();
    }

    /**
     * TEST OK
     * @see com.stc.jmsjca.test.core.QueueEndToEnd#testContainerManaged()
     */
    public void testContainerManaged() throws Throwable {
        super.testContainerManaged();
    }

    /**
     * Test OK
     * @see com.stc.jmsjca.test.core.QueueEndToEnd#testExceptionBMTCC()
     */
    public void testExceptionBMTCC() throws Throwable {
        super.testExceptionBMTCC();
    }

    /**
     * TEST OK
     * @see com.stc.jmsjca.test.core.QueueEndToEnd#testExceptionBMTSerial()
     */
    public void testExceptionBMTSerial() throws Throwable {
        super.testExceptionBMTSerial();
    }

    /**
     * Test OK
     * @see com.stc.jmsjca.test.core.QueueEndToEnd#testExceptionCMTCC()
     */
    public void testExceptionCMTCC() throws Throwable {
        super.testExceptionCMTCC();
    }

    /**
     * TEST OK
     * @see com.stc.jmsjca.test.core.QueueEndToEnd#testExceptionCMTSerial()
     */
    public void testExceptionCMTSerial() throws Throwable {
        super.testExceptionCMTSerial();
    }

    /**
     * Exception UNIFIED DOMAIN NOT SUPPORTED
     *
     * @see com.stc.jmsjca.test.core.QueueEndToEnd#testMultiNonSharedResources()
     */
    public void disabled_testMultiNonSharedResources() throws Throwable {
    }

    /**
     * Exception: UNIFIED DOMAIN NOT SUPPORTED
     *
     * @see com.stc.jmsjca.test.core.QueueEndToEnd#testMultiXA()
     */
    public void disabled_testMultiXA() throws Throwable {
    }

    /**
     * TEST OK
     * @see com.stc.jmsjca.test.core.QueueEndToEnd#testReuseLTResources()
     */
    public void testReuseLTResources() throws Throwable {
        super.testReuseLTResources();
    }

    /**
     * FAILS:
     * Context=sendTo2And3CloseImmediately;|RAR5029:Unexpected exception while registering component
     * javax.transaction.SystemException: Failed to enlist resource com.seebeyond.jms.client.STCXAResource@ec459a: javax.transaction.xa.XAException: End has been called already, and no more start again
     *
     * Caused by: javax.transaction.xa.XAException: End has been called already, and no more start again
     * at com.seebeyond.jms.client.STCXAResource.start(STCXAResource.java:583)
     *
     * @see com.stc.jmsjca.test.core.QueueEndToEnd#testReuseXAResources()
     */
    public void disabled_testReuseXAResources() throws Throwable {
    }

    /**
     * TEST OK
     * @see com.stc.jmsjca.test.core.QueueEndToEnd#testSharedLTResources()
     */
    public void testSharedLTResources() throws Throwable {
        super.testSharedLTResources();
    }

    /**
     * TEST OK
     * @see com.stc.jmsjca.test.core.QueueEndToEnd#testSharedXAResources()
     */
    public void testSharedXAResources() throws Throwable {
        super.testSharedXAResources();
    }

    /**
     * TEST OK
     * @see com.stc.jmsjca.test.core.QueueEndToEnd#testSuspendCC()
     */
    public void testSuspendCC() throws Throwable {
        super.testSuspendCC();
    }
    
    /**
     * TEST OK
     * @see com.stc.jmsjca.test.core.QueueEndToEnd#testSuspendSerial()
     */
    public void testSuspendSerial() throws Throwable {
        super.testSuspendSerial();
    }
    
    /**
     * Queue browser is not implemented
     */
    public void disabled_testUndeployWhenAppProcessingMessagesCC() throws Throwable {
    }
        
    /**
     * Queue browser is not implemented
     */
    public void disabled_testUndeployWhenAppProcessingMessagesSerial() throws Throwable {
    }
    
    /**
     * Problem: calling tempdest.delete() on a deleted destination should throw an 
     * exception. It doesn't. Disabled "requestReplyN2", "requestReplyN3".
     */
    public void skip_testRequestReplyN2() throws Throwable {
    }
    
    /**
     * Problem: calling tempdest.delete() on a deleted destination should throw an 
     * exception. It doesn't. Disabled "requestReplyN2", "requestReplyN3".
     */
    public void skip_testRequestReplyN3() throws Throwable {
    }

    /**
     * Problem: calling tempdest.delete() on a deleted destination should throw an 
     * exception. It doesn't.
     */
    public void disabled_testA() {
        
    }
    
    /**
     * This test requires JMS 1.1 
     */
    public void disabled_testNoTransaction() {
        
    }

    /**
     * @see com.stc.jmsjca.test.core.EndToEndBase#getJMSProvider()
     */
    public JMSProvider getJMSProvider() {
        return new Stcms453Provider();
    }
}
