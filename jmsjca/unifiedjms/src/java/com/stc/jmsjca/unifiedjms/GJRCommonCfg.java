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
 * Provides the same configuration bean values so that JMSJCA can replace GenericJMSRA
 * without migration. In GenericJMSRA, this is not an interface but a baseclass. The
 * RA, the MCF, the activation spec, and the admin destination extend this baseclass.
 * 
 * Because of the lack of multiple inheritance in Java, and the fact that JMSJCA should
 * not add this adapter specific stuff as a baseclass, the RA, the MCF, the activation
 * spec and the admin destination implement this interface. Unfortunately, this results 
 * in code duplication of the implementation of this interface.
 * 
 *  To keep the code duplication easy to maintain, the implementation of this interface
 *  is done in a separate class (or layer) in the inheritance hierarcy.
 * 
 * @author fkieviet
 */
public interface GJRCommonCfg {
    /**
     * NOT PART OF THE CONFIGURATION
     */
    String ISSAMERMOVERRIDE = "OnePerPhysicalConnection";

    /**
     * NOT PART OF THE CONFIGURATION
     * 
     * @return
     */
    boolean hasGenericJMSRAProperties();
    
    /**
     * @return
     */
    String getRMPolicy();

    /**
     * @param policy
     */
    void setRMPolicy(String policy);

    /**
     * @param supportsXA
     */
    void setSupportsXA(boolean supportsXA);

    /**
     * @return
     */
    boolean getSupportsXA();  

    /**
     * @return
     */
    Boolean getSupportsXA3();  

    /**
     * @param props
     */
    void setJndiProperties(String props);  

    /**
     * Gets the JNDI propeties.
     *
     * @return  Properties separated by ":".
     */
    String getJndiProperties();  

    /**
     * Sets the connection factory class name.
     *
     * @param className  class name.
     */
    void setConnectionFactoryClassName(String className);  

    /**
     * Gets the connection factory class name.
     *
     * @return  connection factory class name.
     */
    String getConnectionFactoryClassName();  

    /**
     * @return
     */
    String getDeliveryConcurrencyMode();  

    /**
     * @param mode
     */
    void setDeliveryConcurrencyMode(String mode);  

    /**
     * @return
     */
    String getDeliveryType();  

    /**
     * @param delivery
     */
    void setDeliveryType(String delivery);  

    /**
     * @param className
     */
    void setQueueConnectionFactoryClassName(String className);  

    /**
     * @return
     */
    String getQueueConnectionFactoryClassName();  

    /**
     * @param className
     */
    void setTopicConnectionFactoryClassName(String className);  

    /**
     * @return
     */
    String getTopicConnectionFactoryClassName();  

    /**
     * @param className
     */
    void setTopicClassName(String className);  

    /**
     * @return
     */
    String getTopicClassName();  

    /**
     * @param className
     */
    void setUnifiedDestinationClassName(String className);  

    /**
     * @return
     */
    String getUnifiedDestinationClassName();  

    /**
     * @param className
     */
    void setQueueClassName(String className);  

    /**
     * @return
     */
    String getQueueClassName();  

    /**
     * @param mode
     */
    void setProviderIntegrationMode(String mode);  

    /**
     * @return
     */
    String getProviderIntegrationMode();  

    /**
     * @param props
     */
    void setConnectionFactoryProperties(String props);  

    /**
     * @return
     */
    String getConnectionFactoryProperties();  

    /**
     * @param methodName
     */
    void setCommonSetterMethodName(String methodName);  

    /**
     * @return
     */
    String getCommonSetterMethodName();  

    /**
     * @return
     */
    int getMDBDeploymentRetryAttempt();  

    /**
     * @param retryAttempt
     */
    void setMDBDeploymentRetryAttempt(int retryAttempt);  

    /**
     * @return
     */
    int getMDBDeploymentRetryInterval();  

    /**
     * @param retryInterval
     */
    void setMDBDeploymentRetryInterval(int retryInterval);  

    /**
     * @return
     */
    String getXAConnectionFactoryClassName();  

    /**
     * @param connectionFactoryClassName
     */
    void setXAConnectionFactoryClassName(String connectionFactoryClassName);  

    /**
     * @return
     */
    String getXAQueueConnectionFactoryClassName();  

    /**
     * @param queueConnectionFactoryClassName
     */
    void setXAQueueConnectionFactoryClassName(String queueConnectionFactoryClassName);  

    /**
     * @return
     */
    String getXATopicConnectionFactoryClassName();  

    /**
     * @param topicConnectionFactoryClassName
     */
    void setXATopicConnectionFactoryClassName(String topicConnectionFactoryClassName);  

    /**
     * @param monitor
     */
    void setMonitoring(boolean monitor);  

    /**
     * @return
     */
    boolean getMonitoring();  

    /**
     * @return
     */
    boolean getUseFirstXAForRedelivery();  

    /**
     * @param usefirstxa
     */
    void setUseFirstXAForRedelivery(boolean usefirstxa);  
}
