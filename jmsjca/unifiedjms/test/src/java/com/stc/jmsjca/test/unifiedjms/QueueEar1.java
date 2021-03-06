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

package com.stc.jmsjca.test.unifiedjms;

import com.stc.jmsjca.container.Container;
import com.stc.jmsjca.container.EmbeddedDescriptor;
import com.stc.jmsjca.container.EmbeddedDescriptor.ResourceAdapter;
import com.stc.jmsjca.test.core.EndToEndBase;
import com.stc.jmsjca.test.core.JMSProvider;
import com.stc.jmsjca.test.core.Passthrough;
import com.stc.jmsjca.test.core.QueueEndToEnd;
import com.stc.jmsjca.util.Str;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.Session;

/**
 * Required:
 *
 * @author fkieviet
 * @version $Revision: 1.9 $
 */
public class QueueEar1 extends EndToEndBase {
    
    /**
     * Queue to queue XA on in, XA on out CC-mode
     * 
     * @throws Throwable
     */
    public void testContainerManaged() throws Throwable {
        EmbeddedDescriptor dd = getDD();
        dd.findElementByText(EJBDD, "XContextName").setText("j-testQQXAXA");
        dd.update();

        // Deploy
        Container c = createContainer();
        Passthrough p = createPassthrough(mServerProperties);

        try {
            if (c.isDeployed(mTestEar.getAbsolutePath())) {
                c.undeploy(mTestEarName);
            }
            p.clearQ1Q2Q3();

            Passthrough.QueueSource source = p.new QueueSource("Queue1");
            source.connect();
            source.drain();

            c.redeployModule(mTestEar.getAbsolutePath());
            p.passFromQ1ToQ2();
            c.undeploy(mTestEarName);
            p.get("Queue1").assertEmpty();

            if (!isFastTest()) {
                c.deployModule(mTestEar.getAbsolutePath());
                p.passFromQ1ToQ2();
                c.undeploy(mTestEarName);
                p.get("Queue1").assertEmpty();
            }
        } finally {
            Container.safeClose(c);
            Passthrough.safeClose(p);
        }
    }

    public void testXActivationSpecDelegation() throws Throwable {
        // TODO: ensure connection factory tied to jms/stcms1
        final String FACTURL = "jms/tx/jmq1";

        EmbeddedDescriptor dd = getDD();

        ConnectorConfig cc = (ConnectorConfig) dd.new ResourceAdapter(RAXML).createConnector(ConnectorConfig.class);
        cc.setConnectionURL("lookup://" + FACTURL);

        QueueEndToEnd.ActivationConfig spec = (QueueEndToEnd.ActivationConfig) dd.new ActivationSpec(EJBDD,"mdbtest").createActivation(QueueEndToEnd.ActivationConfig.class);
        spec.setContextName("j-testQQXAXA");
        spec.setConcurrencyMode("serial");
        
        dd.findElementByText(EJBDD, "testQQXAXA").setText("testGlobalFact");

        dd.update();

        // Deploy
        Container c = createContainer();
        Passthrough p = createPassthrough(mServerProperties);
        try {
            c.redeployModule(mTestEar.getAbsolutePath());
            p.setMessageGenerator(new Passthrough.MessageGenerator() {
                @Override
                public Message createMessage(Session s, Class<?> type) throws JMSException {
                    Message ret = super.createMessage(s, type);
                    ret.setStringProperty("cf", FACTURL);
                    return ret;
                }
            });
            p.passFromQ1ToQ2();
            c.undeploy(mTestEarName);
            p.get("Queue1").assertEmpty();
        } finally {
            Container.safeClose(c);
            Passthrough.safeClose(p);
        }
    }
    
    /**
     * Queue to queue XA on in, XA on out CC-mode
     * 
     * @throws Throwable
     */
    public void testGenericJMSRABeanProv() throws Throwable {
        EmbeddedDescriptor dd = getDD();
        dd.findElementByText(EJBDD, "XContextName").setText("j-GJR-bean");
        
        
        ResourceAdapter ra = dd.new ResourceAdapter(RAXML);
        ra.setParam("ConnectionURL", "");
        ra.setParam("UserName", "");
        ra.setParam("Password", "");
        ra.setParam("Options", "");

        ra.setParam("ProviderIntegrationMode", "javabean");
        ra.setParam("CommonSetterMethodName", "setProperty");

        ra.setParam("ConnectionFactoryClassName", "com.sun.messaging.ConnectionFactory");
        ra.setParam("QueueConnectionFactoryClassName", "com.sun.messaging.QueueConnectionFactory");
        ra.setParam("TopicConnectionFactoryClassName", "com.sun.messaging.TopicConnectionFactory");
        ra.setParam("XAConnectionFactoryClassName", "com.sun.messaging.XAConnectionFactory");
        ra.setParam("XAQueueConnectionFactoryClassName", "com.sun.messaging.XAQueueConnectionFactory");
        ra.setParam("XATopicConnectionFactoryClassName", "com.sun.messaging.XATopicConnectionFactory");

        ra.setParam("QueueClassName", "com.sun.messaging.Queue");
        ra.setParam("TopicClassName", "com.sun.messaging.Topic");

        ra.setParam("SupportsXA", "true");
        ra.setParam("LogLevel", "info");
        ra.setParam("DeliveryType", "Synchronous");
        ra.setParam("UserName", "guest");
        ra.setParam("Password", "guest");
        
        dd.update();

        // Deploy
        Container c = createContainer();
        Passthrough p = createPassthrough(mServerProperties);

        try {
            if (c.isDeployed(mTestEar.getAbsolutePath())) {
                c.undeploy(mTestEarName);
            }
            p.clearQ1Q2Q3();

            Passthrough.QueueSource source = p.new QueueSource("Queue1");
            source.connect();
            source.drain();

            c.redeployModule(mTestEar.getAbsolutePath());
            p.passFromQ1ToQ2();
            c.undeploy(mTestEarName);
            p.get("Queue1").assertEmpty();

            if (!isFastTest()) {
                c.deployModule(mTestEar.getAbsolutePath());
                p.passFromQ1ToQ2();
                c.undeploy(mTestEarName);
                p.get("Queue1").assertEmpty();
            }
        } finally {
            Container.safeClose(c);
            Passthrough.safeClose(p);
        }
    }

    public static final String PROVIDERCLASSNAME = "jmsjca.jmsimpl.unified.provider";

    /**
     * @see com.stc.jmsjca.test.core.EndToEndBase#getJMSProvider()
     */
    @Override
    public JMSProvider getJMSProvider() {
        String providerClassName = mServerProperties.getProperty(PROVIDERCLASSNAME);
        if (Str.empty(providerClassName)) {
            throw new RuntimeException("Provider classname not set. Set property " 
                + PROVIDERCLASSNAME + " to the appropriate classname.");
        }
        try {
            return new UnifiedProvider(providerClassName);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
