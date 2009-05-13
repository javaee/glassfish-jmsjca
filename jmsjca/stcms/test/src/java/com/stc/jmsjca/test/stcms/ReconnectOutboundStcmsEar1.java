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
import com.stc.jmsjca.test.core.JMSProvider;
import com.stc.jmsjca.test.core.Passthrough;
import com.stc.jmsjca.test.core.QueueEndToEnd;
import com.stc.jmsjca.test.core.ReconnectionTestsOutbound;
import com.stc.jmsjca.test.core.TcpProxyNIO;

/**
 * Tests STCMS reconnects on outbound connections
 *
 * @author fkieviet
 * @version $Revision: 1.9 $
 */
public class ReconnectOutboundStcmsEar1 extends ReconnectionTestsOutbound {
    
    /**
     * @see com.stc.jmsjca.test.core.EndToEndBase#getJMSProvider()
     */
    @Override
    public JMSProvider getJMSProvider() {
        return new StcmsProvider();
    }

    /**
     * Kills the last connection; this is most likely the producer TCP/IP connection.
     * 
     * WORKS FOR STCMS ONLY
     * 
     * @throws Throwable
     */
    public void testReconnectOutSendKillProducer() throws Throwable {
        doReconnectTest(new XTestExhaustion() {
            @Override
            public String getOnMessageMethod() {
                return "reconnectOutXA";
            }
            
            // MUST BE SERIAL
            @Override
            public void modifyDD(EmbeddedDescriptor dd, QueueEndToEnd.ActivationConfig spec) throws Exception {
                spec.setConcurrencyMode("serial");
            }

            @Override
            public void test(TcpProxyNIO proxy, Passthrough p, Container c) throws Exception {
                int N = 50;
                p.setNMessagesToSend(N);
                p.passFromQ1ToQ2();
                
                System.out.println(N + " msgs processed");
                
                // Kill producer connection (lucky shot)
                System.out.println("Killing producer");
                proxy.killLastConnection();
                
                System.out.println("Resuming");
                p.passFromQ1ToQ2();
            }
        });
    }
}
