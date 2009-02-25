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

import com.stc.jmsjca.test.core.JMSProvider;
import com.stc.jmsjca.test.core.QueueEndToEnd;

/**
 *
 * @author  cye
 * @version $Revision: 1.7 $
 */
public class BasicWMQQueueEar1 extends QueueEndToEnd {

    /**
     * When running in Eclipse, allows to interrupt the test before any other tests are run.
     * REMOVE THIS
     * 
     * @throws InterruptedException if alis
     */
    public void testDummy() throws InterruptedException {
        Thread.sleep(1000);
    }

    public void xtestContainerManaged() throws Throwable {        
    }
    
    public void xtestCloseInTranactionXA() throws Throwable {
    }
    
    public void xtestBeanManaged() throws Throwable {
    }
    
    public void xtestCloseInTransactionLT() throws Throwable {
    }

    public void invalid_testMixCMTWithLTResource() throws Throwable {
    }
    
    public void xtestSharedXAResources() throws Throwable {
    }

    public void xtestSharedLTResources() throws Throwable {        
    }
    
    public void xtestReuseXAResources() throws Throwable {
    }
    
    public void xtestReuseLTResources() throws Throwable {
    }
    
    public void testMultiNonSharedResources() throws Throwable {
    // a bug
    }
    
    public void testMultiXA() throws Throwable {
    // a bug
    }
    
    public void xtestMBean() throws Throwable {
    }
    
    public void testConcurrency() throws Throwable {
    //a bug
    }
    
    public void xtestExceptionCMTSerial() throws Throwable {
    }
    
    public void xtestExceptionCMTCC() throws Throwable {
    }
    
    public void xtestExceptionBMTCC() throws Throwable {
    }
    
    public void xtestExceptionBMTSerial() throws Throwable {
    }
    
    public void testExceptionBMTSerial2() throws Throwable {
    }

    /**
     * @see com.stc.jmsjca.test.core.EndToEndBase#getJMSProvider()
     */
    public JMSProvider getJMSProvider() {
        return new WMQProvider();
    }
}
