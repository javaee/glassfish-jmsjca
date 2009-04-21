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

import com.stc.jmsjca.core.TxMgr;
import com.stc.jmsjca.core.XConnectionRequestInfo;
import com.stc.jmsjca.core.XMCFQueueXA;
import com.stc.jmsjca.core.XMCFTopicXA;
import com.stc.jmsjca.core.XManagedConnectionFactory;
import com.stc.jmsjca.test.core.JMSProvider;
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
import java.util.Properties;

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
    public JMSProvider getJMSProvider() {
        return new WMQProvider();
    }

    /**
     * Constructor
     */
    public BaseJUStd() {
        this(null);
    }
    
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

    public WireCount getConnectionCount() {
        return new WireCount() {
            public void check(int sessions, int producers, int consumers) {
            }

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

    /**
     * Gets connection URL to the default test server; contains host, but not port.
     * Port should be obtained from a system property.
     *
     * @return URL
     */
    protected String getConnectionURL() {
        return "wmq://" + mServerProperties.getProperty(WMQProvider.PROPNAME_HOST, "<wmq host not set>") + ":"
        + mServerProperties.getProperty(WMQProvider.PROPNAME_PORT, "<wmq port not set>") + "?" 
        + RAWMQObjectFactory.QUEUEMANAGER + "=" + mServerProperties.getProperty(WMQProvider.QUEUEMANAGER, "<wmq queuemgr not set>")
        ;
        
    }

    public void init(boolean producerPooling) throws Throwable {
        InitialContext ctx = getContext();

        // QUEUE
        {
            // Managed factory; this is normally done by the Application Server
            XManagedConnectionFactory x = new XMCFQueueXA();
            RAWMQResourceAdapter ra = new RAWMQResourceAdapter();
            x.setResourceAdapter(ra);
            ra.setConnectionURL(getConnectionURL());
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
            ra.setConnectionURL(getConnectionURL());
            x.setProducerPooling(Boolean.toString(producerPooling));
            ra.setUserName(getJMSProvider().getUserName(mServerProperties));
            ra.setPassword(getJMSProvider().getPassword(mServerProperties));

            // Factory to be used by the application (done by the Application server)
            TopicConnectionFactory f = (TopicConnectionFactory) x.createConnectionFactory();
            ctx.rebind(appjndiTopic, f);
        }
    }
    
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
    public void testXAOutOfTx() {
    }
    
    /**
     * MQSeries does NOT allow messages to be sent outside of a transaction on an XASession
     */
    public void testCTSFailure5() {
    }

    /**
     * MQSeries does not support XA and CC
     */
    public void skip_testXACCBatch() throws Throwable {
    }
    
    
    /**
     * @see com.stc.jmsjca.test.core.XTestBase#getXAQueueConnectionFactory()
     * @throws JMSException propagatd 
     */
    public XAQueueConnectionFactory getXAQueueConnectionFactory() throws JMSException {
        RAWMQObjectFactory o = new RAWMQObjectFactory();
        
        return (XAQueueConnectionFactory) o.createConnectionFactory(XConnectionRequestInfo.DOMAIN_QUEUE_XA, null, null, null, getConnectionURL());
    }
}
