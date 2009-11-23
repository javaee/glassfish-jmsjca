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

import com.stc.jmsjca.core.AdminDestination;

/**
 * A layer in the inheritance hierarchy that adds GJR properties to the
 * AdminDestination
 * 
 * KEEP IMPLEMENTATION OF THIS CLASS IN SYNC BETWEEN THESE CLASSES:
 * @see GJRActivationSpecCommonLayer
 * @see GJRAdminDestCommonLayer
 * @see GJRMCFCommonLayer
 * @see GJRResourceAdapterCommonLayer
 * See comments in:
 * @see GJRCommonCfg
 * 
 * @author fkieviet
 */
public abstract class GJRAdminDestCommonLayer extends AdminDestination implements GJRCommonCfg {

    // private static final Localizer L = Localizer.get();
    private boolean isUsed;
    private String rmPolicy;
    private Boolean supportsXA;
    private String jndiProperties;

    private Boolean usefirstxaforredelivery;
    private boolean enableMonitoring;

    private String xAConnectionFactoryClassName;
    private String xATopicConnectionFactoryClassName;
    private String xAQueueConnectionFactoryClassName;
    private String cfClassName;
    private String queueCFClassName;
    private String topicCFClassName;

    private int mDBDeploymentRetryInterval;
    private int mDBDeploymentRetryAttempt;
    private String setterMethodName;
    private String cfProperties;
    private String deliveryConcurrencyMode;
    private String deliveryType;
    private String topicClassName;
    private String destinationClassName;
    private String queueClassName;
    private String providerIntegrationMode;

    /**
     * @see com.stc.jmsjca.unifiedjms.GJRCommonCfg#hasGenericJMSRAProperties()
     */
    public boolean hasGenericJMSRAProperties() {
        return isUsed;
    }

    /**
     * @see com.stc.jmsjca.unifiedjms.GJRCommonCfg#getRMPolicy()
     */
    public String getRMPolicy() {
        return rmPolicy;
    }

    /**
     * @see com.stc.jmsjca.unifiedjms.GJRCommonCfg#setRMPolicy(java.lang.String)
     */
    public void setRMPolicy(String policy) {
        isUsed = true;
        this.rmPolicy = policy;
    }

    static final String ISSAMERMOVERRIDE = "OnePerPhysicalConnection";

    /**
     * @see com.stc.jmsjca.unifiedjms.GJRCommonCfg#setSupportsXA(boolean)
     */
    public void setSupportsXA(boolean supportsXA) {
        isUsed = true;
        this.supportsXA = Boolean.valueOf(supportsXA);
    }

    /**
     * @see com.stc.jmsjca.unifiedjms.GJRCommonCfg#getSupportsXA()
     */
    public boolean getSupportsXA() {
        return supportsXA == null ? false : supportsXA.booleanValue();
    }

    /**
     * @see com.stc.jmsjca.unifiedjms.GJRCommonCfg#getSupportsXA()
     */
    public Boolean getSupportsXA3() {
        return supportsXA;
    }

    /**
     * @see com.stc.jmsjca.unifiedjms.GJRCommonCfg#setJndiProperties(java.lang.String)
     */
    public void setJndiProperties(String props) {
        isUsed = true;
        jndiProperties = props;
    }

    /**
     * @see com.stc.jmsjca.unifiedjms.GJRCommonCfg#getJndiProperties()
     */
    public String getJndiProperties() {
        return jndiProperties;
    }

    /**
     * @see com.stc.jmsjca.unifiedjms.GJRCommonCfg#setConnectionFactoryClassName(java.lang.String)
     */
    public void setConnectionFactoryClassName(String className) {
        isUsed = true;
        this.cfClassName = className;
    }

    /**
     * @see com.stc.jmsjca.unifiedjms.GJRCommonCfg#getConnectionFactoryClassName()
     */
    public String getConnectionFactoryClassName() {
        return this.cfClassName;
    }

    /**
     * @see com.stc.jmsjca.unifiedjms.GJRCommonCfg#getDeliveryConcurrencyMode()
     */
    public String getDeliveryConcurrencyMode() {
        return deliveryConcurrencyMode;
    }

    /**
     * @see com.stc.jmsjca.unifiedjms.GJRCommonCfg#setDeliveryConcurrencyMode(java.lang.String)
     */
    public void setDeliveryConcurrencyMode(String mode) {
        isUsed = true;
        deliveryConcurrencyMode = mode;
    }

    /**
     * @see com.stc.jmsjca.unifiedjms.GJRCommonCfg#getDeliveryType()
     */
    public String getDeliveryType() {
        return this.deliveryType;
    }

    /**
     * @see com.stc.jmsjca.unifiedjms.GJRCommonCfg#setDeliveryType(java.lang.String)
     */
    public void setDeliveryType(String delivery) {
        isUsed = true;
        deliveryType = delivery;
    }

    /**
     * @see com.stc.jmsjca.unifiedjms.GJRCommonCfg#setQueueConnectionFactoryClassName(java.lang.String)
     */
    public void setQueueConnectionFactoryClassName(String className) {
        isUsed = true;
        this.queueCFClassName = className;
    }

    /**
     * @see com.stc.jmsjca.unifiedjms.GJRCommonCfg#getQueueConnectionFactoryClassName()
     */
    public String getQueueConnectionFactoryClassName() {
        return this.queueCFClassName;
    }

    /**
     * @see com.stc.jmsjca.unifiedjms.GJRCommonCfg#setTopicConnectionFactoryClassName(java.lang.String)
     */
    public void setTopicConnectionFactoryClassName(String className) {
        isUsed = true;
        this.topicCFClassName = className;
    }

    /**
     * @see com.stc.jmsjca.unifiedjms.GJRCommonCfg#getTopicConnectionFactoryClassName()
     */
    public String getTopicConnectionFactoryClassName() {
        return this.topicCFClassName;
    }

    /**
     * @see com.stc.jmsjca.unifiedjms.GJRCommonCfg#setTopicClassName(java.lang.String)
     */
    public void setTopicClassName(String className) {
        isUsed = true;
        this.topicClassName = className;
    }

    /**
     * @see com.stc.jmsjca.unifiedjms.GJRCommonCfg#getTopicClassName()
     */
    public String getTopicClassName() {
        return this.topicClassName;
    }

    /**
     * @see com.stc.jmsjca.unifiedjms.GJRCommonCfg#setUnifiedDestinationClassName(java.lang.String)
     */
    public void setUnifiedDestinationClassName(String className) {
        isUsed = true;
        this.destinationClassName = className;
    }

    /**
     * @see com.stc.jmsjca.unifiedjms.GJRCommonCfg#getUnifiedDestinationClassName()
     */
    public String getUnifiedDestinationClassName() {
        return this.destinationClassName;
    }

    /**
     * @see com.stc.jmsjca.unifiedjms.GJRCommonCfg#setQueueClassName(java.lang.String)
     */
    public void setQueueClassName(String className) {
        isUsed = true;
        this.queueClassName = className;
    }

    /**
     * @see com.stc.jmsjca.unifiedjms.GJRCommonCfg#getQueueClassName()
     */
    public String getQueueClassName() {
        return this.queueClassName;
    }

    /**
     * @see com.stc.jmsjca.unifiedjms.GJRCommonCfg#setProviderIntegrationMode(java.lang.String)
     */
    public void setProviderIntegrationMode(String mode) {
        isUsed = true;
        this.providerIntegrationMode = mode;
    }

    /**
     * @see com.stc.jmsjca.unifiedjms.GJRCommonCfg#getProviderIntegrationMode()
     */
    public String getProviderIntegrationMode() {
        return this.providerIntegrationMode;
    }

    /**
     * @see com.stc.jmsjca.unifiedjms.GJRCommonCfg#setConnectionFactoryProperties(java.lang.String)
     */
    public void setConnectionFactoryProperties(String props) {
        isUsed = true;
        this.cfProperties = props;
    }

    /**
     * @see com.stc.jmsjca.unifiedjms.GJRCommonCfg#getConnectionFactoryProperties()
     */
    public String getConnectionFactoryProperties() {
        return this.cfProperties;
    }

    /**
     * @see com.stc.jmsjca.unifiedjms.GJRCommonCfg#setCommonSetterMethodName(java.lang.String)
     */
    public void setCommonSetterMethodName(String methodName) {
        isUsed = true;
        this.setterMethodName = methodName;
    }

    /**
     * @see com.stc.jmsjca.unifiedjms.GJRCommonCfg#getCommonSetterMethodName()
     */
    public String getCommonSetterMethodName() {
        return this.setterMethodName;
    }

    /**
     * @see com.stc.jmsjca.unifiedjms.GJRCommonCfg#getMDBDeploymentRetryAttempt()
     */
    public int getMDBDeploymentRetryAttempt() {
        return mDBDeploymentRetryAttempt;
    }

    /**
     * @see com.stc.jmsjca.unifiedjms.GJRCommonCfg#setMDBDeploymentRetryAttempt(int)
     */
    public void setMDBDeploymentRetryAttempt(int retryAttempt) {
        isUsed = true;
        mDBDeploymentRetryAttempt = retryAttempt;
    }

    /**
     * @see com.stc.jmsjca.unifiedjms.GJRCommonCfg#getMDBDeploymentRetryInterval()
     */
    public int getMDBDeploymentRetryInterval() {
        return mDBDeploymentRetryInterval;
    }

    /**
     * @see com.stc.jmsjca.unifiedjms.GJRCommonCfg#setMDBDeploymentRetryInterval(int)
     */
    public void setMDBDeploymentRetryInterval(int retryInterval) {
        isUsed = true;
        mDBDeploymentRetryInterval = retryInterval;
    }

    /**
     * @see com.stc.jmsjca.unifiedjms.GJRCommonCfg#getXAConnectionFactoryClassName()
     */
    public String getXAConnectionFactoryClassName() {
        return this.xAConnectionFactoryClassName;
    }

    /**
     * @see com.stc.jmsjca.unifiedjms.GJRCommonCfg#setXAConnectionFactoryClassName(java.lang.String)
     */
    public void setXAConnectionFactoryClassName(String connectionFactoryClassName) {
        isUsed = true;
        xAConnectionFactoryClassName = connectionFactoryClassName;
    }

    /**
     * @see com.stc.jmsjca.unifiedjms.GJRCommonCfg#getXAQueueConnectionFactoryClassName()
     */
    public String getXAQueueConnectionFactoryClassName() {
        return this.xAQueueConnectionFactoryClassName;
    }

    /**
     * @see com.stc.jmsjca.unifiedjms.GJRCommonCfg#setXAQueueConnectionFactoryClassName(java.lang.String)
     */
    public void setXAQueueConnectionFactoryClassName(String queueConnectionFactoryClassName) {
        isUsed = true;
        xAQueueConnectionFactoryClassName = queueConnectionFactoryClassName;
    }

    /**
     * @see com.stc.jmsjca.unifiedjms.GJRCommonCfg#getXATopicConnectionFactoryClassName()
     */
    public String getXATopicConnectionFactoryClassName() {
        return this.xATopicConnectionFactoryClassName;
    }

    /**
     * @see com.stc.jmsjca.unifiedjms.GJRCommonCfg#setXATopicConnectionFactoryClassName(java.lang.String)
     */
    public void setXATopicConnectionFactoryClassName(String topicConnectionFactoryClassName) {
        isUsed = true;
        xATopicConnectionFactoryClassName = topicConnectionFactoryClassName;
    }

    /**
     * @see com.stc.jmsjca.unifiedjms.GJRCommonCfg#setMonitoring(boolean)
     */
    public void setMonitoring(boolean monitor) {
        isUsed = true;
        enableMonitoring = monitor;
    }

    /**
     * @see com.stc.jmsjca.unifiedjms.GJRCommonCfg#getMonitoring()
     */
    public boolean getMonitoring() {
        return enableMonitoring;
    }

    /**
     * @see com.stc.jmsjca.unifiedjms.GJRCommonCfg#getUseFirstXAForRedelivery()
     */
    public boolean getUseFirstXAForRedelivery() {
        if (usefirstxaforredelivery != null) {
            return usefirstxaforredelivery.booleanValue();
        } else {
            return false;
        }
    }

    /**
     * @see com.stc.jmsjca.unifiedjms.GJRCommonCfg#setUseFirstXAForRedelivery(boolean)
     */
    public void setUseFirstXAForRedelivery(boolean usefirstxa) {
        isUsed = true;
        usefirstxaforredelivery = Boolean.valueOf(usefirstxa);
    }
}
