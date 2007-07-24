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

import com.stc.jmsjca.test.core.Passthrough;
import com.stc.jmsjca.test.core.QueueEndToEnd;
import com.stc.jmsjca.container.EmbeddedDescriptor;
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
 * @author fkieviet
 * @version $Revision: 1.5 $
 */
public class QueueWaveEar1 extends QueueEndToEnd {

    public void testBatchUTCC() throws Throwable {
        // TODO Auto-generated method stub
        throw new RuntimeException("Batch processing not supported for ConnectionConsumer");
        
    }

    public void testBatchXACC() throws Throwable {
        // TODO Auto-generated method stub
        throw new RuntimeException("Batch processing not supported for ConnectionConsumer");
        
    }

    public void testBatchXAHUACC() throws Throwable {
        // TODO Auto-generated method stub
        throw new RuntimeException("Batch processing not supported for ConnectionConsumer");
        
    }

    public void testBatchXAHUARBCC() throws Throwable {
        // TODO Auto-generated method stub
        throw new RuntimeException("Batch processing not supported for ConnectionConsumer");
        
    }

    public void testBatchXARBCC() throws Throwable {
        // TODO Auto-generated method stub
        throw new RuntimeException("Batch processing not supported for ConnectionConsumer");
        
    }

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

    public Passthrough createPassthrough(Properties serverProperties) {
        return new WavePassthrough(serverProperties);
    }
}
