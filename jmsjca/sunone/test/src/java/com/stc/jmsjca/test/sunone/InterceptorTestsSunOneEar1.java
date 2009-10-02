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

package com.stc.jmsjca.test.sunone;

import com.stc.jmsjca.test.core.InterceptorTests;
import com.stc.jmsjca.test.core.JMSProvider;
import com.sun.messaging.jmq.jmsclient.runtime.BrokerInstance;

/**
 * @author fkieviet
 */
public class InterceptorTestsSunOneEar1 extends InterceptorTests {

    /**
     * @see com.stc.jmsjca.test.core.EndToEndBase#getJMSProvider()
     */
    @Override
    public JMSProvider getJMSProvider() {
        return new SunOneProvider();
    }
    
    // Embedded broker instance, used only when testing direct mode
    private BrokerInstance brokerInstance;

    public void setUp() throws Exception {
        
        if (((SunOneProvider) getJMSProvider()).isDirect()) {
            brokerInstance=DirectModeSupport.startEmbeddedbroker(getServerProperties());
        }
        
        super.setUp();
    }


    public void tearDown() throws Exception {
        
        if (((SunOneProvider) getJMSProvider()).isDirect()){
            DirectModeSupport.shutdownEmbeddedBroker(brokerInstance);
        }
        
        super.tearDown();
    }
    
}
