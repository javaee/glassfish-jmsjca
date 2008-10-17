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
import com.stc.jmsjca.test.core.EndToEndBase;
import com.stc.jmsjca.test.core.JMSProvider;

import org.jdom.Element;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.Queue;
import javax.jms.QueueConnection;
import javax.jms.QueueConnectionFactory;
import javax.jms.QueueSender;
import javax.jms.QueueSession;
import javax.jms.Session;
import javax.naming.InitialContext;

/**
 * Required:
 * test.server.properties = path to properties file containing server config
 * test.ear.path          = path to ear file to be tested
 *
 * @author fkieviet
 * @version $Revision: 1.5 $
 */
public class Perf1 extends EndToEndBase {
    /**
     * @see com.stc.jmsjca.test.core.EndToEndBase#getJMSProvider()
     */
    public JMSProvider getJMSProvider() {
        return new StcmsProvider();
    }

    /**
     * Empty onMessage
     */
    public static class MDB1 extends StcmsPerfMDB.Executor {
        public void init(InitialContext ctx) {}
        public void onMessage(Message message, InitialContext ctx) {
        }
    }

    /**
     * Nothing cached
     */
    public static class MDB2 extends StcmsPerfMDB.Executor {
        public void init(InitialContext ctx) {}
        public void onMessage(Message message, InitialContext ctx) throws Exception {
            QueueConnection conn = null;
            try {
                QueueConnectionFactory fact = (QueueConnectionFactory) ctx
                    .lookup("java:comp/env/queuefact");
                conn = fact.createQueueConnection();
                QueueSession s = conn.createQueueSession(true,
                    Session.AUTO_ACKNOWLEDGE);
                Queue dest = s.createQueue("Queue2");
                QueueSender prod = s.createSender(dest);
                prod.send(message);
            } finally {
                if (conn != null) {
                    try {
                        conn.close();
                    } catch (JMSException ignore) {
                    }
                }
            }
        }
    }

    /**
     * Only lookup qcf
     */
    public static class MDB3 extends StcmsPerfMDB.Executor {
        public void init(InitialContext ctx) {}
        public void onMessage(Message message, InitialContext ctx) throws Exception {
            QueueConnection conn = null;
            try {
                ctx.lookup("java:comp/env/queuefact");
            } finally {
                if (conn != null) {
                    try {
                        conn.close();
                    } catch (JMSException ignore) {
                    }
                }
            }
        }
    }

    /**
     * Cache QCF
     */
    public static class MDB4 extends StcmsPerfMDB.Executor {
        QueueConnectionFactory fact;
        public void init(InitialContext ctx) throws Exception {
            fact = (QueueConnectionFactory) ctx.lookup("java:comp/env/queuefact");
        }
        public void onMessage(Message message, InitialContext ctx) throws Exception {
            QueueConnection conn = null;
            try {
                conn = fact.createQueueConnection();
                QueueSession s = conn.createQueueSession(true,
                    Session.AUTO_ACKNOWLEDGE);
                Queue dest = s.createQueue("Queue2");
                QueueSender prod = s.createSender(dest);
                prod.send(message);
            } finally {
                safeClose(conn);
            }
        }
    }

    /**
     * Runs a full benchmark
     *
     * @param listener Class
     * @throws Throwable
     */
    public void exec(Class listener) throws Throwable {
        int N = 20000;

        // Adjust DD
        EmbeddedDescriptor dd = getDD();
        Element e = dd.findParentNode(RAXML,
            "/connector/resourceadapter/config-property/config-property-name",
            "ConnectionURL", 1).getChild("config-property-value", J2EENS);
        e.setText("stcms://localhost?" + com.stc.jms.client.STCJMS.Mock.mock + "=true&"
            + com.stc.jms.client.STCJMS.Mock.nmsg + "=" + N);

        dd.findElementByText(EJBDD, "cc").setText("cc");
        dd.findElementByText(RAXML, "XATransaction").setText("XATransaction");
        dd.findElementByText(EJBDD, "XContextName").setText("mock1");
        dd.findElementByText(EJBDD, "com.stc.jmsjca.test.stcms.StcmsMessageBean")
            .setText("com.stc.jmsjca.test.stcms.StcmsPerfMDB");

        dd.findElementByText(EJBDD, "testQQXAXA").setText(listener.getName());

        e = dd.findParentNode(EJBDD,
            "/ejb-jar/enterprise-beans/message-driven/activation-config/activation-config-property/activation-config-property-name",
            "MBeanName", 1).getChild("activation-config-property-value", J2EENS);
        e.setText(MBEAN);


        dd.update();

        // Deploy
        Container c = createContainer();
        try {
            c.redeployModule(mTestEar.getAbsolutePath());
            for (;;) {
                Thread.sleep(2000);
                Integer n = (Integer) c.getAttribute(MBEAN, "NMessages");
                System.out.println("Processed: " + n + " out of " + N);
                if (n.intValue() == N) {
                    break;
                }
            }
            System.out.println(c.getAttribute(MBEAN, "Stats"));
        } finally {
            Container.safeClose(c);
        }
    }

    /**
     *
     * @throws Throwable
     */
    public void testPerf1() throws Throwable {
        exec(MDB1.class);
    }

    public void testPerf2() throws Throwable {
        exec(MDB2.class);
    }

    public void testPerf3() throws Throwable {
        exec(MDB3.class);
    }

    public void testPerf4() throws Throwable {
        exec(MDB4.class);
    }
}
