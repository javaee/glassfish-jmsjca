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

import com.stc.jms.util.XidImpl;
import com.stc.jmsjca.core.Activation;
import com.stc.jmsjca.core.DeliveryStats;
import com.stc.jmsjca.core.XMCFQueueXA;
import com.stc.jmsjca.core.XManagedConnectionFactory;
import com.stc.jmsjca.stcms.RASTCMSActivationSpec;
import com.stc.jmsjca.stcms.RASTCMSResourceAdapter;
import com.stc.jmsjca.util.XAssert;

import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.Queue;
import javax.jms.QueueConnection;
import javax.jms.QueueConnectionFactory;
import javax.jms.QueueSender;
import javax.jms.QueueSession;
import javax.jms.Session;
import javax.resource.ResourceException;
import javax.resource.spi.BootstrapContext;
import javax.resource.spi.ManagedConnection;
import javax.resource.spi.XATerminator;
import javax.resource.spi.endpoint.MessageEndpoint;
import javax.resource.spi.endpoint.MessageEndpointFactory;
import javax.resource.spi.work.WorkManager;
import javax.transaction.xa.XAException;
import javax.transaction.xa.XAResource;
import javax.transaction.xa.Xid;

import java.lang.reflect.Method;

import junit.framework.TestCase;

/**
 * Required:
 * test.server.properties = path to properties file containing server config
 * test.ear.path          = path to ear file to be tested
 *
 * @author fkieviet
 * @version $Revision: 1.6 $
 */
public class Perf2 extends TestCase {
    final int N = 100000;

    /**
     * TIMER
     */
    public class Timer {
        long t0 = System.currentTimeMillis();
        long te;
        public void stop() {
            te = System.currentTimeMillis();
        }

        public String results(long n) {
            long t = te - t0;
            return (te - t0) + " ms; " + (n * 1000 / t) + " msg/sec "
                + ((float) t / (double) n) + " ms/msg (" + n + " messages)";
        }
    }

    /**
     * Tool
     */
    protected static ManagedConnection getManagedConnection(Session s) {
        com.stc.jmsjca.core.WSession ws = (com.stc.jmsjca.core.WSession) s;
        com.stc.jmsjca.core.JSession js = ws.getJSession();
        ManagedConnection mc = js.getManagedConnection();
        return mc;
    }

    /**
     * Measures sending only
     *
     * @throws Throwable on failure of the test
     */
    public void test100() throws Throwable {
        XManagedConnectionFactory x = new XMCFQueueXA();
        RASTCMSResourceAdapter ra = new RASTCMSResourceAdapter();
        x.setResourceAdapter(ra);
        ra.setConnectionURL("stcms://x:1?com.stc.jms.mock=true&com.stc.jms.nmsg=20000");
        x.setProducerPooling(Boolean.toString(true));

        // Factory to be used by the application (done by the Application server)
        QueueConnectionFactory qf = (QueueConnectionFactory) x.createConnectionFactory();


        for (int k = 0; k < 3; k++) {
            Timer t = new Timer();
            for (int i = 0; i < N; i++) {
                QueueConnection conn = qf.createQueueConnection("a", "b");
                QueueSession session = conn.createQueueSession(false, Session.AUTO_ACKNOWLEDGE);

                Queue dest = session.createQueue("x");
                XAResource xa = getManagedConnection(session).getXAResource();
                QueueSender producer = session.createSender(dest);
                Xid xid = new com.stc.jms.util.XidImpl();
                xa.start(xid, XAResource.TMNOFLAGS);
                producer.send(session.createTextMessage("T1"));
                xa.end(xid, XAResource.TMSUCCESS);
                xa.commit(xid, true);
                producer.close();

                conn.close();
            }
            t.stop();
            System.out.println("Send (JMSJCA)   :  " + t.results(N));
        }
    }

    /**
     * Not used
     */
    public static class XBootstrapContext implements BootstrapContext {
        public WorkManager getWorkManager() {
            XAssert.notImplemented();
            return null;
        }

        public XATerminator getXATerminator() {
            XAssert.notImplemented();
            return null;
        }

        public java.util.Timer createTimer() {
            XAssert.notImplemented();
            return null;
        }
    }

    /**
     * ReceiveOnly
     */
    public static class ReceiveOnlyMEP implements MessageEndpoint, MessageListener {
        private XAResource mXA;
        private Xid mXid;

        public ReceiveOnlyMEP(XAResource xa) {
            mXA = xa;
        }

        public void beforeDelivery(Method method) {
            try {
                mXid = new XidImpl();
                mXA.start(mXid, XAResource.TMNOFLAGS);
            } catch (XAException ex) {
                ex.printStackTrace();
            }
        }

        public void afterDelivery() {
            try {
                mXA.end(mXid, XAResource.TMSUCCESS);
                mXA.commit(mXid, true);
            } catch (XAException ex) {
                ex.printStackTrace();
            }
        }

        public void release() {
        }

        public void onMessage(Message m) {
        }
    }

    public static class ReceiveOnlyMEPF implements MessageEndpointFactory {
        public MessageEndpoint createEndpoint(XAResource xa) {
            return new ReceiveOnlyMEP(xa);
        }

        public boolean isDeliveryTransacted(Method method) {
            return true;
        }
    }

    /**
     * Passthrough
     */
    public static class PassthroughMEP implements MessageEndpoint, MessageListener {
        private XAResource mXA;
        private Xid mXid;
        QueueConnectionFactory mQF;

        public PassthroughMEP(XAResource xa, QueueConnectionFactory qf) {
            mXA = xa;
            mQF = qf;
        }

        public void beforeDelivery(Method method) {
            try {
                mXid = new XidImpl();
                mXA.start(mXid, XAResource.TMNOFLAGS);
            } catch (XAException ex) {
                ex.printStackTrace();
            }
        }

        public void afterDelivery() {
            try {
                mXA.end(mXid, XAResource.TMSUCCESS);
                mXA.commit(mXid, true);
            } catch (XAException ex) {
                ex.printStackTrace();
            }
        }

        public void release() {
        }

        public void onMessage(Message m) {
            try {
                QueueConnection conn = mQF.createQueueConnection("a", "b");
                QueueSession session = conn.createQueueSession(false,
                    Session.AUTO_ACKNOWLEDGE);

                Queue dest = session.createQueue("x");
                XAResource xa = getManagedConnection(session).getXAResource();
                xa.start(mXid, XAResource.TMJOIN);
                QueueSender producer = session.createSender(dest);
                producer.send(session.createTextMessage("T1"));
                xa.end(mXid, XAResource.TMSUCCESS);
                producer.close();

                conn.close();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    public static class PassthroughMEPF implements MessageEndpointFactory {
        QueueConnectionFactory mQF;

        public PassthroughMEPF() throws ResourceException {
            XManagedConnectionFactory x = new XMCFQueueXA();
            RASTCMSResourceAdapter ra = new RASTCMSResourceAdapter();
            x.setResourceAdapter(ra);
            ra.setConnectionURL(
                "stcms://x:1?com.stc.jms.mock=true&com.stc.jms.nmsg=20000");
            x.setProducerPooling(Boolean.toString(true));

            mQF = (QueueConnectionFactory) x.createConnectionFactory();
        }


        public MessageEndpoint createEndpoint(XAResource xa) {
            return new PassthroughMEP(xa, mQF);
        }

        public boolean isDeliveryTransacted(Method method) {
            return true;
        }
    }

    /**
     * Sets up a mockup server and waits until N messages have been processed
     *
     * @param mepf MessageEndpointFactory
     * @throws Throwable
     */
    public void server(MessageEndpointFactory mepf) throws Throwable {
        XManagedConnectionFactory x = new XMCFQueueXA();
        RASTCMSResourceAdapter ra = new RASTCMSResourceAdapter();
        x.setResourceAdapter(ra);
        ra.setConnectionURL("stcms://x:1?com.stc.jms.mock=true&com.stc.jms.nmsg=" + N);
        x.setProducerPooling(Boolean.toString(true));
        XBootstrapContext bc = new XBootstrapContext();
        ra.start(bc);
        RASTCMSActivationSpec spec = new RASTCMSActivationSpec();
        spec.setConcurrencyMode("serial");
        spec.setDestination("Q");
        spec.setDestinationType(Queue.class.getName());
//        spec.setContextName("X");

        ra.endpointActivation(mepf, spec);

        Activation a = (Activation) ra.findActivation(mepf, spec, false);
        DeliveryStats stats = a.getStats();
        for (;;) {
            Thread.sleep(1000);
            int n = stats.getNMessages();
            System.out.println(n + " out of " + N);
            if (n == N) {
                break;
            }
        }
        System.out.println(stats.toString());
        ra.endpointDeactivation(mepf, spec);
        ra.stop();
    }

    public void testReceiveOnly() throws Throwable {
        server(new ReceiveOnlyMEPF());
    }

    public void testPassthrough() throws Throwable {
        server(new PassthroughMEPF());
    }

    public static void main(String[] args) {
        try {
            new Perf2().testPassthrough();
            new Perf2().testPassthrough();
        } catch (Throwable ex) {
            ex.printStackTrace();
        }
    }
}
