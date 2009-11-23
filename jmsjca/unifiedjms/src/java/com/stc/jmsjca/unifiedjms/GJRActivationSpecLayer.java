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

package com.stc.jmsjca.unifiedjms;


/**
 * Introduces a layer of GJR properties that are only defined for the activationspec
 * 
 * @author fkieviet
 */
public abstract class GJRActivationSpecLayer extends GJRActivationSpecCommonLayer {
    private boolean isUsed;
    /**
     * @see com.stc.jmsjca.unifiedjms.GJRCommonCfg#hasGenericJMSRAProperties()
     */
    @Override
    public boolean hasGenericJMSRAProperties() {
        return isUsed || super.hasGenericJMSRAProperties();
    }

    private String cfJndiName;
    private String destJndiName;
    private String destProperties;
    private String dmType = "javax.jms.Destination";
    private String clientId;
    private int redeliveryAttempts;
    private int redeliveryInterval;
    private int reconnectAttempts;
    private int reconnectInterval;
    private int maxPoolSize = 8;
    private int maxWaitTime = 3;
    private boolean isDmd;
    private String dmClassName;
    private String dmJndiName;
    private String dmCfJndiName;
    private String dmProperties;
    private String dmCfProperties;
    private int endpointReleaseTimeout = 180;
    private boolean shareclientid;
    /* START of properties for Load balancing topics */
    private int instanceCount = 1;
    private boolean loadBalance = true;    
    private String mCurrentInstance = "0";
    private int mCurrentInstanceNo;
    private String mMessageSelector = "";
    private String mInstanceClientId;
    private static final String SELECTOR_PROPERTY = "com.sun.genericra.loadbalancing.selector";    
    private static final String INSTANCENO_PROPERTY = "com.sun.genericra.loadbalancing.instance.id";    
    private static final String INSTANCE_CLIENTID_PROPERTY = "com.sun.genericra.loadbalancing.instance.clientid";
    /* END of properties for load balancing */
    
    /*Sync consumer props*/
    private int ackTimeOut = 2;
    
    /**
     * @param waitTime
     */
    public void setMaxWaitTime(int waitTime) {
        isUsed = true;
        this.maxWaitTime = waitTime;
    }

    public int getMaxWaitTime() {
        return this.maxWaitTime;
    }

    /**
     * @param interval
     */
    public void setRedeliveryInterval(int interval) {
        isUsed = true;
        this.redeliveryInterval = interval;
    }

    public int getRedeliveryInterval() {
        return this.redeliveryInterval;
    }

    /**
     * @param attempts
     */
    public void setRedeliveryAttempts(int attempts) {
        isUsed = true;
        this.redeliveryAttempts = attempts;
    }    
    
    public int getRedeliveryAttempts() {
        return this.redeliveryAttempts;
    }
    
/* Following methods have been added for implementing topic lo
 * balancing.
 * BEGIN
 */
    /**
     * @param instancecount
     */
    public void setInstanceCount(int instancecount) {
        isUsed = true;
        this.instanceCount = instancecount;
    }    
    
    /**
     * @return
     */
    public int getInstanceCount() {
        return this.instanceCount;
    }
    
    /**
     * @param loadbalance
     */
    public void setLoadBalancingRequired(boolean loadbalance) {
        isUsed = true;
        this.loadBalance = loadbalance;
    }    
    
    public boolean getLoadBalancingRequired() {
        return this.loadBalance;
    }
    
    /**
     * Instace Id and load balancing selector cannot be configured through
     * the activation spec, but they are here because these seemes a logical place
     * to put them.
     * These have to be configured as jvm properties and can be unique for
     * different instances in a cluster
     */
    public int getInstanceID() {
        try {
            mCurrentInstance = System.getProperty(INSTANCENO_PROPERTY, "0");
            mCurrentInstanceNo = Integer.parseInt(mCurrentInstance.trim());
        } catch (Exception e) {
            mCurrentInstanceNo = 0;
        }  
        return this.mCurrentInstanceNo;
    }
    
    /**
     * @return
     */
    public String getInstanceClientId() {
        try {
            mInstanceClientId = System.getProperty(INSTANCE_CLIENTID_PROPERTY);
        } catch (Exception e) {
            ;
        }
        return mInstanceClientId;
    }        
    
    /**
     * @return
     */
    public String getLoadBalancingSelector() {
        try {
            mMessageSelector = System.getProperty(SELECTOR_PROPERTY, "");        
        } catch (Exception e) {
            mMessageSelector = "";
        }      
        return this.mMessageSelector;
    }
    
    /* END
     */
    /**
     * @param interval
     */
    public void setReconnectInterval(int interval) {
        isUsed = true;
        this.reconnectInterval = interval;
    }

    public int getReconnectInterval() {
        return this.reconnectInterval;
    }

    /**
     * @param attempts
     */
    /**
     * @param attempts
     */
    public void setReconnectAttempts(int attempts) {
        isUsed = true;
        this.reconnectAttempts = attempts;
    }

    public int getReconnectAttempts() {
        return this.reconnectAttempts;
    }

    /**
     * @param clientId
     */
    public void setClientID(String clientId) {
        isUsed = true;
        this.clientId = clientId;
    }

    public String getClientID() {
        return this.clientId;
    }

    /**
     * @param name
     */
    public void setConnectionFactoryJndiName(String name) {
        isUsed = true;
        this.cfJndiName = name;
    }

    public String getConnectionFactoryJndiName() {
        return this.cfJndiName;
    }

    /**
     * @param name
     */
    public void setDestinationJndiName(String name) {
        isUsed = true;
        this.destJndiName = name;
    }

    public String getDestinationJndiName() {
        return this.destJndiName;
    }

    /**
     * @param properties
     */
    public void setDestinationProperties(String properties) {
        isUsed = true;
        this.destProperties = properties;
    }

    public String getDestinationProperties() {
        return this.destProperties;
    }

    /**
     * @param maxPoolSize
     */
    public void setMaxPoolSize(int maxPoolSize) {
        isUsed = true;
        this.maxPoolSize = maxPoolSize;
    }

    public int getMaxPoolSize() {
        return this.maxPoolSize;
    }

    /**
     * @param isDmd
     */
    public void setSendBadMessagesToDMD(boolean isDmd) {
        isUsed = true;
        this.isDmd = isDmd;
    }

    public boolean getSendBadMessagesToDMD() {
        return this.isDmd;
    }

    /**
     * @param jndiName
     */
    public void setDeadMessageJndiName(String jndiName) {
        isUsed = true;
        this.dmJndiName = jndiName;
    }

    /**
     * @param p
     */
    public void setDeadMessageConnectionFactoryProperties(String p) {
        isUsed = true;
        this.dmCfProperties = p;
    }

    public String getDeadMessageConnectionFactoryProperties() {
        return this.dmCfProperties;
    }

    /**
     * @param jndiName
     */
    public void setDeadMessageConnectionFactoryJndiName(String jndiName) {
        isUsed = true;
        this.dmCfJndiName = jndiName;
    }

    public String getDeadMessageConnectionFactoryJndiName() {
        return this.dmCfJndiName;
    }

    /**
     * @param jndiName
     */
    public void setDeadMessageDestinationJndiName(String jndiName) {
        isUsed = true;
        this.dmJndiName = jndiName;
    }

    public String getDeadMessageDestinationJndiName() {
        return this.dmJndiName;
    }

    /**
     * @param className
     */
    public void setDeadMessageDestinationClassName(String className) {
        isUsed = true;
        this.dmClassName = className;
    }

    public String getDeadMessageDestinationClassName() {
        return this.dmClassName;
    }

    /**
     * @param dmdProps
     */
    public void setDeadMessageDestinationProperties(String dmdProps) {
        isUsed = true;
        this.dmProperties = dmdProps;
    }

    public String getDeadMessageDestinationProperties() {
        return this.dmProperties;
    }

    public String getDeadMessageDestinationType() {
        return this.dmType;
    }

    /**
     * @param dmType
     */
    public void setDeadMessageDestinationType(String dmType) {
        isUsed = true;
        this.dmType = dmType;
    }

    /**
     * @param secs
     */
    public void setEndpointReleaseTimeout(int secs) {
        isUsed = true;
        this.endpointReleaseTimeout = secs;
    }

    public int getEndpointReleaseTimeout() {
        return this.endpointReleaseTimeout;
    }
    
    public boolean getShareClientid() {
        return this.shareclientid;
    }
    
    /**
     * @param genclientid
     */
    public void setShareClientid(boolean genclientid) {
        isUsed = true;
        this.shareclientid = genclientid;
    }    

    /**
     * Holds value of property applicationName.
     */
    private String applicationName;

    /**
     * Getter for property applicationName.
     * @return Value of property applicationName.
     */
    public String getApplicationName() {
        return this.applicationName;
    }

    /**
     * Setter for property applicationName.
     * @param applicationName New value of property applicationName.
     */
    public void setApplicationName(String applicationName) {
        isUsed = true;
        this.applicationName = applicationName;
    }
    
    /**
     * @param timeout
     */
    public void setAckTimeOut(int timeout) {
        isUsed = true;
        ackTimeOut = timeout;
    }
    
    public int getAckTimeOut() {
        return ackTimeOut;
    }
}
