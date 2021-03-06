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


import com.stc.jmsjca.container.EmbeddedDescriptor;
import com.stc.jmsjca.core.RAJMSResourceAdapter;
import com.stc.jmsjca.sunone.RASunOneResourceAdapter;
import com.stc.jmsjca.test.core.EndToEndBase;
import com.stc.jmsjca.test.core.JMSProvider;
import com.stc.jmsjca.test.core.Passthrough;
import com.stc.jmsjca.test.core.BaseTestCase.JMSTestEnv;
import com.stc.jmsjca.test.core.EndToEndBase.ConnectorConfig;

import java.util.Properties;

/**
 *
 * @author fkieviet
 * @version $Revision: 1.9 $
 */
public class SunOneProvider extends JMSProvider {
    public static final String PROPNAME_HOST = "jmsjca.jmsimpl.sunone.host";
    public static final String PROPNAME_PORT = "jmsjca.jmsimpl.sunone.port";
    public static final String PROPNAME_USERID = "jmsjca.jmsimpl.sunone.userid";
    public static final String PROPNAME_PASSWORD = "jmsjca.jmsimpl.sunone.password";
    public static final String PROPNAME_IMQHOME = "jmsjca.jmsimpl.sunone.imqhome";
    
    /**
     * @return true if directmode tests need to be tested
     */
    public boolean isDirect() {
        return "direct".equals(System.getProperty("jmsjca.jmsimpl.subid", ""));
    }

    /**
     * @param dd
     * @param test
     * @return
     * @throws Exception
     */
    @Override
    public EmbeddedDescriptor changeDD(EmbeddedDescriptor dd, JMSTestEnv test)
    throws Exception {
        ConnectorConfig cc = (ConnectorConfig) dd.new ResourceAdapter(EndToEndBase.RAXML).createConnector(ConnectorConfig.class);
        cc.setConnectionURL(getConnectionUrl(test));

        cc = (ConnectorConfig) dd.new ResourceAdapter(EndToEndBase.RAXML1).createConnector(ConnectorConfig.class);
        cc.setConnectionURL(getConnectionUrl(test));

        return dd;
    }

    /**
     * Provides a hook to plug in provider specific client IDs
     * 
     * @return clientId
     */
    @Override
    public String getClientId(String proposedClientId) {
        return proposedClientId;
    }

    /**
     * Creates a JMS Provider specific passthrough
     * which will be used to test code that is processing messages in an application server
     * 
     * @param serverProperties
     * @return new PassThrough
     */
    @Override
    public Passthrough createPassthrough(Properties serverProperties) {
        
        // configure the Passthrough to use TCP to communicate with the MQ broker
        // even if the MDB is using direct mode
        // because the MDB is running in a separate JVM
       
        SunOnePassthrough sunOnePassthrough = new SunOnePassthrough(serverProperties, false, this);
        sunOnePassthrough.setCommitSize(1);
        return sunOnePassthrough;
    }
    
    /**
     * Creates a JMS Provider specific passthrough 
     * which will be used to test some code that is processing messages in the this JVM
     * rather than in an application server
     * 
     * @param serverProperties
     * @return new PassThrough
     */
    @Override
    public Passthrough createLocalPassthrough(Properties serverProperties) {
        
        // if this is a direct mode test then the Passthrough can use direct mode to communicate with the MQ broker
        // since the code being tested is running in the same JVM as this code
        
        SunOnePassthrough sunOnePassthrough = new SunOnePassthrough(serverProperties, isDirect(), this);
        sunOnePassthrough.setCommitSize(1);
        return sunOnePassthrough;
    }

    /**
     * @see com.stc.jmsjca.test.core.JMSProvider#createConnectionUrl(java.lang.String, int)
     */
    @Override
    public String createConnectionUrl(String host, int port) {
        return "mq://" + host + ":" + port + (isDirect() ? "/direct" : "");
    }

    /**
     * @see com.stc.jmsjca.test.core.JMSProvider#getConnectionUrl(com.stc.jmsjca.test.core.BaseTestCase.JMSTestEnv)
     */
    @Override
    public String getConnectionUrl(JMSTestEnv test) {
        if (isDirect()){
            return "mq://localhost/direct";
        } else {
            String host = test.getJmsServerProperties().getProperty(PROPNAME_HOST);
            int port = Integer.parseInt(test.getJmsServerProperties().getProperty(PROPNAME_PORT));
            return createConnectionUrl(host, port);
        }
    }

    /**
     * @see com.stc.jmsjca.test.core.JMSProvider#getPassword(java.util.Properties)
     */
    @Override
    public String getPassword(Properties serverProperties) {
        return serverProperties.getProperty(PROPNAME_PASSWORD);
    }

    /**
     * @see com.stc.jmsjca.test.core.JMSProvider#getUserName(java.util.Properties)
     */
    @Override
    public String getUserName(Properties serverProperties) {
        return serverProperties.getProperty(PROPNAME_USERID);
    }

    /**
     * @see com.stc.jmsjca.test.core.JMSProvider#getProviderID()
     */
    @Override
    public String getProviderID() {
        return "sunone";
    }

    @Override
    public RAJMSResourceAdapter createRA() {
        return new RASunOneResourceAdapter();
    }
}
