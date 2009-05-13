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

import com.stc.jmsjca.container.Container;
import com.stc.jmsjca.container.EmbeddedDescriptor;
import com.stc.jmsjca.test.core.JMSProvider;
import com.stc.jmsjca.test.core.Passthrough;
import com.stc.jmsjca.test.core.TopicEndToEnd;

/**
 * WMQ Tests for topics
 */
public class BasicWMQTopicEar1 extends TopicEndToEnd {
    /**
     * @see com.stc.jmsjca.test.core.EndToEndBase#getJMSProvider()
     */
    @Override
    public JMSProvider getJMSProvider() {
        return new WMQProvider();
    }

    /**
     * Topic to queue
     * XA on in, XA on out
     * serial-mode
     * Durable
     * Uses setBrokerDurSubQueue()
     *
     * @throws Throwable
     */
    public void testSetBrokerDurSubQueue() throws Throwable {
        Passthrough p = createPassthrough(mServerProperties);
               
        EmbeddedDescriptor dd = getDD();
        ActivationConfig spec = (ActivationConfig) dd.new ActivationSpec(EJBDD, "mdbtest").createActivation(ActivationConfig.class);
        spec.setContextName("setBrokerDurSubQueue");
        p.setTopic1Name("SportsTopic1");
        // If not using ".*" at the end of the name, the queue has to be created in MQSeries manually.
        spec.setDestination("jmsjca://?name=" + p.getTopic1Name() + "&BrokerDurSubQueue=SYSTEM.JMS.D.BASEBALL.*");
        spec.setDestinationType(javax.jms.Topic.class.getName());
        spec.setConcurrencyMode("cc");
        spec.setSubscriptionDurability("Durable");
        p.setDurableTopic1Name("BASEBALLSUB");
        String subscriptionName = p.getDurableTopic1Name1();
        spec.setSubscriptionName(subscriptionName);
        String clientID = getJMSProvider().getClientId(p.getDurableTopic1Name1() + "clientID");
        spec.setClientId(clientID);
        dd.update();

        // Deploy
        Container c = createContainer();
        
        try {
            if (c.isDeployed(mTestEar.getAbsolutePath())) {
                c.undeploy(mTestEarName);
            }

            p.removeDurableSubscriber(clientID, p.getTopic1Name(), subscriptionName);
            
            p.setBatchId(900);

            // deploy bean to create a durable subscription then undeploy it
            c.deployModule(mTestEar.getAbsolutePath());
            waitUntilRunning(c);
            c.undeploy(mTestEarName);

            p.drainQ2();

            // send messages to T1 - these should be stored in the durable subscription
            p.sendToT1();

            // now redeploy the bean 
            // this should then receive the messages from the durable subscription
            // and sends them on to Q2
            c.deployModule(mTestEar.getAbsolutePath());
            waitUntilRunning(c);
            p.readFromQ2(); 

            c.undeploy(mTestEarName);
        } finally {
            Container.safeClose(c);
            Passthrough.safeClose(p);
        }
    }
    
}
