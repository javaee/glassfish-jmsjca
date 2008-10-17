package com.stc.jmsjca.test.core;

import com.stc.jmsjca.container.EmbeddedDescriptor;

import java.util.Properties;

/**
 * Abstracts out the JMS Provider specific test functionality
 * 
 * @author fkieviet
 */
public abstract class JMSProvider {
    /**
     * Called after the DD is parsed and updated with the basic settings; allows for
     * provider specific changes to the DD.
     * 
     * @param dd DD
     * @return updated DD
     * @throws Exception on failure
     */
    public abstract EmbeddedDescriptor changeDD(EmbeddedDescriptor dd, BaseTestCase.JMSTestEnv test) throws Exception;
    
    /**
     * Creates a JMS Provider specific passthrough
     * 
     * @param serverProperties
     * @return new PassThrough
     */
    public abstract Passthrough createPassthrough(Properties serverProperties);

    /**
     * Provides a hook to plug in provider specific client IDs
     * 
     * @param suggestedClientID
     * @return clientid
     */
    public abstract String getClientId(String suggestedClientID);
    
    /**
     * Obtains a connection URL
     * 
     * @param test env
     * @return URL
     */
    public String getConnectionUrl(BaseTestCase.JMSTestEnv test) {
        String host = test.getJmsServerProperties().getProperty("host");
        int port = Integer.parseInt(test.getJmsServerProperties().getProperty("stcms.instance.port"));
        return createConnectionUrl(host, port);
    }
    
    public String createConnectionUrl(String host, int port) {
        throw new IllegalStateException("Not implemented in " + this);
    }
}