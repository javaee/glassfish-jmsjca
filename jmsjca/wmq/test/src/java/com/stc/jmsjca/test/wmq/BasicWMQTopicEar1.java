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
import com.stc.jmsjca.test.core.TopicEndToEnd;
import com.stc.jmsjca.test.wl.WLProvider;

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
 * @version $Revision: 1.6 $
 */
public class BasicWMQTopicEar1 extends TopicEndToEnd {
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

    /**
     * @see com.stc.jmsjca.test.core.EndToEndBase#getJMSProvider()
     */
    public JMSProvider getJMSProvider() {
        return new WLProvider();
    }
}
