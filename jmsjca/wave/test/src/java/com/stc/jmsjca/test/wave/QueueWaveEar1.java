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

package com.stc.jmsjca.test.wave;

import com.stc.jmsjca.test.core.JMSProvider;
import com.stc.jmsjca.test.core.QueueEndToEnd;

/**
 *
 * @author fkieviet, cye
 * @version $Revision: 1.9 $
 */
public class QueueWaveEar1 extends QueueEndToEnd {

    String testContainerId = null;
    
    /**
     * @see com.stc.jmsjca.test.core.QueueEndToEnd#getMaxConcurrency()
     */
    protected int getMaxConcurrency() {
        // set to the value of maxQueuePresend 
        return 10;
    }
    
    
    /**
     * Grid does not implement batch size in CC
     */
    public void skip_testBatchXACC() throws Throwable {        
    }
    
    /**
     * Grid does not implement batch size in CC
     */
    public void skip_testBatchXARBCC() throws Throwable {
    }
    
    /**
     * Grid does not implement batch size in CC
     */
    public void skip_testBatchUTCC() throws Throwable {        
    }
    
    /**
     * Grid does not implement batch size in CC
     */
    public void skip_testBatchXAHUACC() throws Throwable {
    }
    
    /**
     * Grid does not implement batch size in CC
     */
    public void skip_testBatchXAHUARBCC() throws Throwable {
    }

    public void testBatchXACC() throws Throwable {
        if (testContainerId == null || !testContainerId.equals("wl")) {
            super.testBatchXACC();
        }  // else skip the test 
        // between beforeDelivery and afterDelivery, endpoint on message
        // can not be invoked more than once. The patch is supported in WLS
    }    
    public void testBatchUTCC() throws Throwable {
        if (testContainerId == null || !testContainerId.equals("wl")) {
            super.testBatchUTCC();
        }
        // between beforeDelivery and afterDelivery, endpoint on message
        // can not be invoked more than once. The patch is supported in WLS
    }    
    public void testBatchXAHUARBCC() throws Throwable {
        if (testContainerId == null || !testContainerId.equals("wl")) {
            super.testBatchXAHUARBCC();
        } // else skip the test
        // between beforeDelivery and afterDelivery, endpoint on message
        // can not be invoked more than once. The patch is supported in WLS
    }        
    public void testBatchXAHUACC() throws Throwable {
        if (testContainerId == null || !testContainerId.equals("wl")) {
            super.testBatchXAHUACC();
        } // else skip the test
        // between beforeDelivery and afterDelivery, endpoint on message
        // can not be invoked more than once. The patch is supported in WLS
    }    
    public void testBatchXARBCC() throws Throwable {
        if (testContainerId == null || !testContainerId.equals("wl")) {
            super.testBatchXARBCC();
        } // else skip the test
        // between beforeDelivery and afterDelivery, endpoint on message
        // can not be invoked more than once. The patch is supported in WLS
    }    
    public void testBatchXA() throws Throwable {
        if (testContainerId == null || !testContainerId.equals("wl")) {
            super.testBatchXA();
        } // else skip the test
        // between beforeDelivery and afterDelivery, endpoint on message
        // can not be invoked more than once. The patch is supported in WLS
    }    
    public void testBatchUT() throws Throwable {
        if (testContainerId == null || !testContainerId.equals("wl")) {
            super.testBatchUT();
        } // else skip the test
        // between beforeDelivery and afterDelivery, endpoint on message
        // can not be invoked more than once. The patch is supported in WLS
    }    
    public void testBatchXAHUA() throws Throwable {
        if (testContainerId == null || !testContainerId.equals("wl")) {
            super.testBatchXAHUA();
        } // else skip the test
        // between beforeDelivery and afterDelivery, endpoint on message
        // can not be invoked more than once. The patch is supported in WLS
    }    
    public void testBatchXAHUARB() throws Throwable {
        if (testContainerId == null || !testContainerId.equals("wl")) {
            super.testBatchXAHUARB();
        } // else skip the test
        // between beforeDelivery and afterDelivery, endpoint on message
        // can not be invoked more than once. The patch is supported in WLS
    }
    public void testBatchXARB() throws Throwable {
        if (testContainerId == null || !testContainerId.equals("wl")) {
            super.testBatchXARB();
        } // else skip the test
        // between beforeDelivery and afterDelivery, endpoint on message
        // can not be invoked more than once. The patch is supported in WLS
    }                
    public void testBeanManagedRBAllocateOutsideOfTx() throws Throwable {
        if (testContainerId == null || !testContainerId.equals("wl")) {
            super.testBeanManagedRBAllocateOutsideOfTx();
        } // else skip the test
        // This test does not work with WLS
        // If XASession is allocated out of TX before getUserTransaction().begin()
        // A message will be autocomitted after it's been sent. It will not be rolled back.
        // The reason is that XAsession is created outside of TX, it is not associated any
        // with and enlisted to any global transaction manager, it is not assocated with any
        // ejb. That behavior is different glassfish.        
    }


    /**
     * @see com.stc.jmsjca.test.core.EndToEndBase#getJMSProvider()
     */
    public JMSProvider getJMSProvider() {
        return new WaveProvider();
    }
}
