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

package com.stc.jmsjca.test.wl;

import com.stc.jmsjca.container.EmbeddedDescriptor;
import com.stc.jmsjca.core.RAJMSResourceAdapter;
import com.stc.jmsjca.test.core.EndToEndBase;
import com.stc.jmsjca.test.core.JMSProvider;
import com.stc.jmsjca.test.core.Passthrough;
import com.stc.jmsjca.test.core.BaseTestCase.JMSTestEnv;
import com.stc.jmsjca.test.core.EndToEndBase.ConnectorConfig;
import com.stc.jmsjca.wl.RAWLResourceAdapter;

import javax.naming.Context;
import javax.naming.InitialContext;

import java.util.Hashtable;
import java.util.Properties;

/**
 *
 * @author fkieviet
 * @version $Revision: 1.7 $
 */
public class WLProvider extends JMSProvider {
    public static final String PROPNAME_HOST = "jmsjca.jmsimpl.wl.host";
    public static final String PROPNAME_PORT = "jmsjca.jmsimpl.wl.port";
    public static final String PROPNAME_USERID = "jmsjca.jmsimpl.wl.userid";
    public static final String PROPNAME_PASSWORD = "jmsjca.jmsimpl.wl.password";

    public final static String JNDI_FACTORY = "weblogic.jndi.WLInitialContextFactory";

    /**
     * @see com.stc.jmsjca.test.core.JMSProvider#createPassthrough(java.util.Properties)
     */
    @Override
    public Passthrough createPassthrough(Properties serverProperties) {
        return new WLPassthrough(serverProperties, this);
    }
    
    @Override
    public Passthrough createLocalPassthrough(Properties serverProperties) {
        return createPassthrough(serverProperties);
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
     * @see com.stc.jmsjca.test.core.JMSProvider
     * #changeDD(com.stc.jmsjca.container.EmbeddedDescriptor, com.stc.jmsjca.test.core.BaseTestCase.JMSTestEnv)
     */
    @Override
    public EmbeddedDescriptor changeDD(EmbeddedDescriptor dd, JMSTestEnv test)
        throws Exception {
        ConnectorConfig cc = (ConnectorConfig) dd.new ResourceAdapter(EndToEndBase.RAXML).createConnector(ConnectorConfig.class);
        cc.setConnectionURL(getConnectionUrl(test));
        cc.setUserName("");
        cc.setPassword("");

        cc = (ConnectorConfig) dd.new ResourceAdapter(EndToEndBase.RAXML1).createConnector(ConnectorConfig.class);
        cc.setConnectionURL(getConnectionUrl(test));
        cc.setUserName("");
        cc.setPassword("");

        return dd;
    }

    /**
     * @see com.stc.jmsjca.test.core.JMSProvider#getConnectionUrl(com.stc.jmsjca.test.core.BaseTestCase.JMSTestEnv)
     */
    @Override
    public String getConnectionUrl(JMSTestEnv test) {
        return getConnectionUrl(test.getJmsServerProperties());
    }

    public String getConnectionUrl(Properties p) {
        String host = p.getProperty(PROPNAME_HOST);
        int port = Integer.parseInt(p.getProperty(PROPNAME_PORT));
        return createConnectionUrl(host, port);
    }

    public InitialContext getInitialContext(JMSTestEnv test) throws Exception {
        return getInitialContext(test.getJmsServerProperties());
    }

    public InitialContext getInitialContext(Properties p) throws Exception {
        Hashtable<String, String> env = new Hashtable<String, String>();
        env.put(Context.INITIAL_CONTEXT_FACTORY, JNDI_FACTORY);
        env.put(Context.PROVIDER_URL, getConnectionUrl(p));
        InitialContext ctx = new InitialContext(env);
        return ctx;
    }

    /**
     * @see com.stc.jmsjca.test.core.JMSProvider#createConnectionUrl(java.lang.String, int)
     */
    @Override
    public String createConnectionUrl(String host, int port) {
        return "t3://" + host + ":" + port;
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
        return "wl";
    }

    @Override
    public RAJMSResourceAdapter createRA() {
        return new RAWLResourceAdapter();
    }
}
