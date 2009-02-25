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
import com.stc.jmsjca.test.core.EndToEndBase;
import com.stc.jmsjca.test.core.JMSProvider;
import com.stc.jmsjca.test.core.Passthrough;
import com.stc.jmsjca.test.core.QueueEndToEnd;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.Session;

/**
 * Required:
 *
 * @author fkieviet
 * @version $Revision: 1.7 $
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
        final String FACTURL = "jms/stcms1";

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
                public Message createMessage(Session s, Class type) throws JMSException {
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
     * @see com.stc.jmsjca.test.core.EndToEndBase#getJMSProvider()
     */
    public JMSProvider getJMSProvider() {
        return new StcmsProvider();
    }
}
