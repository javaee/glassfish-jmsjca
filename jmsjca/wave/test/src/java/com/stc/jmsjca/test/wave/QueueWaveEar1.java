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

import com.stc.jmsjca.container.EmbeddedDescriptor;
import com.stc.jmsjca.test.core.Passthrough;
import com.stc.jmsjca.test.core.QueueEndToEnd;

import java.io.File;
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
 * @author fkieviet, cye
 * @version $Revision: 1.7 $
 */
public class QueueWaveEar1 extends QueueEndToEnd {

    String testContainerId = null;
    
    /**
     * When running in Eclipse, allows to interrupt the test before any other tests are run.
     * REMOVE THIS
     * 
     * @throws InterruptedException
     */
    public void testDummy() throws InterruptedException {
        Thread.sleep(10);
    }
    
    /**
     * @see com.stc.jmsjca.test.core.QueueEndToEnd#getMaxConcurrency()
     */
    protected int getMaxConcurrency() {
        // set to the value of maxQueuePresend 
        return 10;
    }
    
    /**
     * Called before the test
     * This code is duplicated between each passthrough test unfortunately
     */
    public void setUp() throws Exception {
        super.setUp();

        mServerProperties.setProperty("jmsjca.test.commitsize", Integer.toString(1));

        // To speed up testsing, change number of send messages
        // mServerProperties.setProperty("jmsjca.test.mNMsgsToSend", Integer.toString(100));
        
        testContainerId = System.getProperty("test.container.id");
      
        // Update the original EAR file
        File tempfile = new File(mTestEarOrg.getAbsolutePath() + ".wave");

        // Update first RA
        EmbeddedDescriptor dd = new EmbeddedDescriptor(mTestEarOrg, tempfile);
        StcmsConnector cc = (StcmsConnector) dd.new ResourceAdapter(RAXML)
                .createConnector(StcmsConnector.class);
        cc.setConnectionURL(TestWaveJUStd.getConnectionUrl());

        // Update second RA
        cc = (StcmsConnector) dd.new ResourceAdapter(RAXML1)
                .createConnector(StcmsConnector.class);
        cc.setConnectionURL(TestWaveJUStd.getConnectionUrl());

        // Commit
        dd.update();
        mTestEarOrg = tempfile;
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

    public Passthrough createPassthrough(Properties serverProperties) {
        return new WavePassthrough(serverProperties);
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
}
