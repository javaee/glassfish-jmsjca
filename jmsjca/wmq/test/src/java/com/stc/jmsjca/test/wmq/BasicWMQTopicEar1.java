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

import com.stc.jmsjca.test.core.Passthrough;
import com.stc.jmsjca.test.core.TopicEndToEnd;
import com.stc.jmsjca.container.EmbeddedDescriptor;

import java.io.File;
import java.util.Properties;

/**
 * Required:
 * test.server.properties = path to properties file containing server config
 * test.ear.path          = path to ear file to be tested
 *
 * Example for Eclipse:
 *     -Dtest.server.properties=../../R1/logicalhost/testsettings.properties 
 *     -Dtest.ear.path=rastcms/test/rastcms-test.ear
 * with working directory
 *     ${workspace_loc:e-jmsjca/build}
 *
 * @author   jmsjca team
 * @version $Revision: 1.1.1.2 $
 */
public class BasicWMQTopicEar1 extends TopicEndToEnd {
    
    /**
     * When running in Eclipse, allows to interrupt the test before any other tests are run.
     * REMOVE THIS
     * 
     * @throws InterruptedException if fails
     */
    public void testDummy() throws InterruptedException {
        Thread.sleep(1000);
    }

    /**
     * Provides a hook to plug in provider specific client IDs
     * @param proposedClientId String
     * @return String 
     */
    public String getClientId(String proposedClientId) {
        return proposedClientId;
    }

    /**
     * Called before the test
     * This code is duplicated between each passthrough test unfortunately
     * 
     * @throws Exception if fails
     */
    public void setUp() throws Exception {
        super.setUp();

        mServerProperties.setProperty("jmsjca.test.commitsize", Integer.toString(10));
        mServerProperties.setProperty("jmsjca.test.mNMsgsToSend", Integer.toString(10));

        //System.setProperty("JMSJCA.NoXA","true");

        // Update the original EAR file
        File tempfile = new File(mTestEarOrg.getAbsolutePath() + ".wmq");

        // Update first RA
        EmbeddedDescriptor dd = new EmbeddedDescriptor(mTestEarOrg, tempfile);
        StcmsConnector cc = (StcmsConnector) dd.new ResourceAdapter(RAXML)
                .createConnector(StcmsConnector.class);
        cc.setConnectionURL(WMQPassthrough.getConnectionUrl());

        // Update second RA
        cc = (StcmsConnector) dd.new ResourceAdapter(RAXML1)
                .createConnector(StcmsConnector.class);
        cc.setConnectionURL(WMQPassthrough.getConnectionUrl());

        // Commit
        dd.update();
        mTestEarOrg = tempfile;
    }

    /**
     * @param serverProperties Properties
     * @return Passthrough
     */
    public Passthrough createPassthrough(Properties serverProperties) {
        return new WMQPassthrough(serverProperties);
    }
    
    /**
     * Topic to queue
     * XA on in, XA on out
     * serial-mode
     * Non-durable
     *
     * @throws Throwable
     */    
    public void xtestNonDurableTopicToQueueSerial() throws Throwable {
    }
    
    /**
     * Topic to queue
     * XA on in, XA on out
     * cc
     * Non-durable
     *
     * @throws Throwable
     */    
    public void xtestNonDurableTopicToQueueCC() throws Throwable {
    }    
    
    /**
     * Topic to queue
     * XA on in, XA on out
     * serial-mode
     * Durable
     *
     * @throws Throwable
     */
    public void xtestDurableTopicToQueueSerial() throws Throwable {
    }
    
    /**
     * Topic to queue
     * XA on in, XA on out
     * serial-mode
     * Durable
     *
     * @throws Throwable
     */
    public void xtestDurableTopicToQueueCC() throws Throwable {
    }
    
    /**
     * Topic to topic
     * XA on in, XA on out
     * serial-mode
     * Durable
     *
     * @throws Throwable
     */
    public void xtestDurableTopicToTopicSerial() throws Throwable {
    }
    
    /**
     * Topic to topic
     * XA on in, XA on out
     * serial-mode
     * Durable
     *
     * @throws Throwable
     */
    public void xtestDurableTopicToTopicCC() throws Throwable {
    }
    
}
