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

import com.stc.jmsjca.core.RAJMSResourceAdapter;
import com.stc.jmsjca.core.RAJMSObjectFactory;
import com.stc.jmsjca.core.TxMgr;
import com.stc.jmsjca.core.XConnectionRequestInfo;
import com.stc.jmsjca.core.XMCFQueueXA;
import com.stc.jmsjca.core.XMCFTopicXA;
import com.stc.jmsjca.core.XManagedConnectionFactory;
import com.stc.jmsjca.test.core.XTestBase;
import com.stc.jmsjca.wmq.RAWMQObjectFactory;
import com.stc.jmsjca.wmq.RAWMQResourceAdapter;

import javax.jms.Connection;
import javax.jms.JMSException;
import javax.jms.QueueConnectionFactory;
import javax.jms.TopicConnectionFactory;
import javax.jms.XAQueueConnectionFactory;
import javax.naming.Context;
import javax.naming.InitialContext;

import java.io.File;
import java.net.URL;
import java.util.Properties;

import com.ibm.mq.jms.MQConnectionFactory;

/**
 *
 * Unit tests not using the J2EE container, specific for STCMS.
 * This inherits from XTestBase so that it will include all generic tests.
 * See Base
 * 
 * Eclipse usage: set working directory to ${workspace_loc:jmsjca/build} and arguments:
 *
 * @author Frank Kieviet
 * @version 1.0
 */
public class BaseJUStd extends XTestBase {
    /**
     * @see com.stc.jmsjca.test.core.XTestBase#getJMSProvider()
     */
    @Override
    public WMQProvider getJMSProvider() {
        return new WMQProvider();
    }

    /**
     * Constructor
     */
    public BaseJUStd() {
        this(null);
    }
    
    @Override
    public void tearDown() throws Exception {
        TxMgr.setUnitTestTxMgr(null);
        super.tearDown();
    }

    /**
     * Constructor
     *
     * @param name junit test name
     */
    public BaseJUStd(String name) {
        super(name);
    }

    @Override
    public WireCount getConnectionCount() {
        return new WireCount() {
            @Override
            public void check(int sessions, int producers, int consumers) {
            }

            @Override
            public void check(int n) {
            }
        };
    }

    private String getProviderClass() {
        return "com.sun.jndi.fscontext.RefFSContextFactory";
    }

    private String getUrl() {
        String dir = System.getProperty("url.dir", "/tmp") + "/jmsjcatest";
        new File(dir).mkdirs();

        String providerurl = "file://" + dir;
        return providerurl;
    }

    private InitialContext getContext() throws Throwable {
        Properties props = new Properties();
        props.put(Context.INITIAL_CONTEXT_FACTORY, getProviderClass());
        props.put(Context.PROVIDER_URL, getUrl());
        return new InitialContext(props);
    }

    @Override
    public void init(boolean producerPooling) throws Throwable {
        InitialContext ctx = getContext();

        // QUEUE
        {
            // Managed factory; this is normally done by the Application Server
            XManagedConnectionFactory x = new XMCFQueueXA();
            RAWMQResourceAdapter ra = new RAWMQResourceAdapter();
            x.setResourceAdapter(ra);
            ra.setConnectionURL(getJMSProvider().getConnectionUrl(mServerProperties));
            x.setProducerPooling(Boolean.toString(producerPooling));
            ra.setUserName(getJMSProvider().getUserName(mServerProperties));
            ra.setPassword(getJMSProvider().getPassword(mServerProperties));

            // Factory to be used by the application (done by the Application server)
            QueueConnectionFactory f = (QueueConnectionFactory) x.createConnectionFactory();
            ctx.rebind(appjndiQueue, f);
        }

        // TOPIC
        {
            // Managed factory; this is normally done by the Application Server
            XManagedConnectionFactory x = new XMCFTopicXA();
            RAWMQResourceAdapter ra = new RAWMQResourceAdapter();
            x.setResourceAdapter(ra);
            ra.setConnectionURL(getJMSProvider().getConnectionUrl(mServerProperties));
            x.setProducerPooling(Boolean.toString(producerPooling));
            ra.setUserName(getJMSProvider().getUserName(mServerProperties));
            ra.setPassword(getJMSProvider().getPassword(mServerProperties));

            // Factory to be used by the application (done by the Application server)
            TopicConnectionFactory f = (TopicConnectionFactory) x.createConnectionFactory();
            ctx.rebind(appjndiTopic, f);
        }
    }
    
    @Override
    protected void setClientID(Connection con) throws JMSException {
        con.setClientID("myclientid");
    }
    
    
    /**
     * XAResource wrapper IS supplied for MQSeries
     */
    public void skip_test515q() throws Throwable {
    }
    
    /**
     * MQSeries does NOT allow messages to be sent outside of a transaction on an XASession
     */
    public void skip_testNonTx2() {
    }
    
    /**
     * MQSeries does NOT allow messages to be sent outside of a transaction on an XASession
     */
    public void skip_testCtsFailure4() {
    }

    /**
     * MQSeries does NOT allow messages to be sent outside of a transaction on an XASession
     */
    @Override
    public void testXAOutOfTx() {
    }
    
    /**
     * MQSeries does NOT allow messages to be sent outside of a transaction on an XASession
     */
    @Override
    public void testCTSFailure5() {
    }

    /**
     * MQSeries does not support XA and CC
     */
    public void skip_testXACCBatch() throws Throwable {
    }
    
    /**
     * WMQ5 doesn't support XA; this test uses XA directly
     */
    public boolean shouldRun_testXACCStopCloseRolback() throws Throwable {
        System.out.println("This is WMQ5? " + getJMSProvider().is5());
        return !getJMSProvider().is5();
    }
    
    /**
     * @see com.stc.jmsjca.test.core.XTestBase#getXAQueueConnectionFactory()
     * @throws JMSException propagatd 
     */
    @Override
    public XAQueueConnectionFactory getXAQueueConnectionFactory() throws JMSException {
        RAWMQObjectFactory o = new RAWMQObjectFactory();
        String url = getJMSProvider().getConnectionUrl(mServerProperties);
        RAWMQResourceAdapter ra = new RAWMQResourceAdapter();
        return (XAQueueConnectionFactory) o.createConnectionFactory(XConnectionRequestInfo.DOMAIN_QUEUE_XA, ra, null, null, url);
    }

    /**
     * 
     * testGeneralProperties
     *
     * Tests the setting of general boolean/int/long/String/URL MQConnectionFactory
     * properties via the connection URL
     *
     */
    public void testGeneralProperties() {
        if (!getJMSProvider().is5()) {
            try {
                // Verify setting of boolean, int, long, String and URL general properties
                RAJMSResourceAdapter ra = new RAWMQResourceAdapter();
                ra.setConnectionURL("wmq://testGeneralProperties:1234?WMQ_CCDTURL=file%3A%2F%2F%2Fpath&WMQ_SecurityExit=testGeneralProperties&WMQ_SparseSubscriptions=false&WMQ_StatusRefreshInterval=1234&WMQ_CleanupInterval=54321");
                RAJMSObjectFactory fact = ra.createObjectFactory(null);
                MQConnectionFactory mqcf = (MQConnectionFactory) fact.createConnectionFactory(
                        XConnectionRequestInfo.DOMAIN_TOPIC_XA, ra, null, null, null);

                assertTrue("Failed to set host", mqcf.getHostName().equals("testGeneralProperties"));
                assertTrue("Failed to set port", mqcf.getPort() == 1234);
                assertTrue("Failed to set boolean property", mqcf.getSparseSubscriptions() == false);
                assertTrue("Failed to set int property: " + mqcf.getStatusRefreshInterval(), mqcf.getStatusRefreshInterval() == 1234);
                assertTrue("Failed to set String property: " + mqcf.getSecurityExit(), mqcf.getSecurityExit() != null && mqcf.getSecurityExit().equals("testGeneralProperties"));
                assertTrue("Failed to set URL property: " + mqcf.getCCDTURL(), mqcf.getCCDTURL() != null && mqcf.getCCDTURL().equals(new URL("file:///path")));
                assertTrue("Failed to set long property: " + mqcf.getCleanupInterval(), mqcf.getCleanupInterval() == 54321);

                ra.setConnectionURL("wmq://pompey:4321?WMQ_SeCuRiTyExIt=GeneralPropertiestest&WMQ_SparseSubscriptions=true&WMQ_StatusRefreshInterval=4321");
                mqcf = (MQConnectionFactory) fact.createConnectionFactory(
                        XConnectionRequestInfo.DOMAIN_TOPIC_XA, ra, null, null, null);            

                assertTrue("Failed to set host", mqcf.getHostName().equals("pompey"));
                assertTrue("Failed to set port", mqcf.getPort() == 4321);
                assertTrue("Failed to set boolean property" , mqcf.getSparseSubscriptions() == true);
                assertTrue("Failed to set int property: " + mqcf.getStatusRefreshInterval(), mqcf.getStatusRefreshInterval() == 4321);
                assertTrue("Failed to set String property: " + mqcf.getSecurityExit(), mqcf.getSecurityExit() != null && mqcf.getSecurityExit().equals("GeneralPropertiestest"));
            } catch (Exception e) {
                e.printStackTrace();
                assertTrue("Unexpected exception thrown", false);
            }

            // Verify E841 is thrown for invalid general properties
            try {
                RAJMSResourceAdapter ra = new RAWMQResourceAdapter();
                ra.setConnectionURL("wmq://testGeneralProperties:1234?WMQ_Securityxit=testGeneralProperties&WMQ_SparseSubscriptions=false&WMQ_StatusRefreshInterval=1234");
                RAJMSObjectFactory fact = ra.createObjectFactory(null);
                fact.createConnectionFactory(
                        XConnectionRequestInfo.DOMAIN_TOPIC_XA, ra, null, null, null);
                assertTrue("RAWMQ-E841 was not thrown for setSecurityxit", false);
            } catch (Exception e) {
                e.printStackTrace();
                assertTrue("Unexpected exception thrown", e.getMessage().startsWith("RAWMQ-E841"));
            }

            // Verify E841 is thrown for invalid int properties
            try {
                RAJMSResourceAdapter ra = new RAWMQResourceAdapter();
                ra.setConnectionURL("wmq://testGeneralProperties:1234?WMQ_SecurityExit=testGeneralProperties&WMQ_SparseSubscriptions=false&WMQ_StatusRefreshInterval=STRING");
                RAJMSObjectFactory fact = ra.createObjectFactory(null);
                fact.createConnectionFactory(
                        XConnectionRequestInfo.DOMAIN_TOPIC_XA, ra, null, null, null);
                assertTrue("RAWMQ-E841 was not thrown for an invalid int property", false);
            } catch (Exception e) {
                e.printStackTrace();
                assertTrue("Unexpected exception thrown", e.getMessage().startsWith("RAWMQ-E841"));
            }
        }
        System.out.println("testGeneralProperties completed successfully");
    }
}
