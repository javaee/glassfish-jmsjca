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

package com.stc.jmsjca.core;

import com.stc.jmsjca.localization.Localizer;
import com.stc.jmsjca.util.Exc;
import com.stc.jmsjca.util.Logger;
import com.stc.jmsjca.util.Str;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.resource.ResourceException;
import javax.resource.spi.InvalidPropertyException;

import java.util.Enumeration;
import java.util.Properties;

/**
 * Encapsulates the configuration of a MessageEndpoint.
 * 
 * Parts of this implementation are based on Sun IMQ
 * 
 * @author Frank Kieviet
 * @version $Revision: 1.10 $
 */
public abstract class RAJMSActivationSpec implements javax.resource.spi.ActivationSpec,
    javax.resource.spi.ResourceAdapterAssociation, java.io.Serializable {
    private static Logger sLog = Logger.getLogger(RAJMSActivationSpec.class);

    /** String constants used to map standard values to JMS equivalents */
    private static final String AUTOACKNOWLEDGE = "Auto-acknowledge";
    private static final String DUPSOKACKNOWLEDGE = "Dups-ok-acknowledge";
    /**
     * Durable
     */
    public static final String DURABLE = "Durable";
    /**
     * NonDurable
     */
    public static final String NONDURABLE = "NonDurable";
    /**
     * Serial mode
     */
    public static final int DELIVERYCONCURRENCY_SERIAL = 0;
    /**
     * ConnectionConsumer mode
     */
    public static final int DELIVERYCONCURRENCY_CC = 1;
    /**
     * MultiReceiver mode
     */
    public static final int DELIVERYCONCURRENCY_MR = 2;
    /**
     * MultiReceiver mode
     */
    public static final int DELIVERYCONCURRENCY_SYNC = 3;

    /**
     * To give meaningful names to modes
     */
    public static final String[] DELIVERYCONCURRENCY_STRS = new String[] {
        "serial", "cc", "multi-receiver", "sync" };

    /**
     * Name of a queue
     */
    protected static final String QUEUE = "javax.jms.Queue";

    /**
     * Topic
     */
    protected static final String TOPIC = "javax.jms.Topic";

    /** The resource adapter instance that this instance is bound to */
    private javax.resource.spi.ResourceAdapter mRA;

    // ActivationSpec attributes recommended for JMS RAs
    private String mDestinationType;
    private String mDestination;
    private String mMessageSelector;
    private String mAcknowledgeMode = AUTOACKNOWLEDGE;
    private String mSubscriptionDurability = NONDURABLE;
    private String mClientId;
    private String mSubscriptionName;
    private String mMBeanName;
    private String mContextName;
    private String mOptionsStr;
    private String mRedeliveryActions = "3:25; 5:50; 10:100; 20:1000; 50:5000";
    private String mConfigurationTemplate;
    private String mProjectInfo;

    // Provider specific attributes

    /** The Maximum endpoint pool size that will be used */
    private int mEndpointPoolMaxSize = 15;

    /** The Steady endpoint pool size that will be used */
    private int mEndpointPoolSteadySize = 10;

    /** The endpoint pool re-size count that will be used */
    private int mEndpointPoolResizeCount = 1;

    /** The endpoint pool re-size timeout in seconds that will be used */
    private int mEndpointPoolResizeTimeout = 5;

    /**
     * The Maximum # of endpoint delivery attempts for endpoints that throw a
     * Exception
     */
    private int mEndpointExceptionRedeliveryAttempts = 1;

    /**
     * The interval for endpoint delivery attempts for endpoints that throw a
     * Exception
     */
    private int mEndpointExceptionRedeliveryInterval = 100;

    /** ContextClassLoader for the onMessage Thread */
    private transient ClassLoader mContextClassLoader;

    private int mDeliveryConcurrencyMode;
    private String mConnectionURL;
    private String mUsername;
    private String mPassword;
    private int mBatchSize;
    private String mHoldUntilAckMode;

    private static final Localizer LOCALE = Localizer.get();

    /**
     * ActivationSpec must provide a default constructor
     */
    public RAJMSActivationSpec() {
    }

    /**
     * Checks to see if the specified name can be used in the JMS provider as a
     * destination name
     * 
     * @param name String
     * @return boolean
     */
    public abstract boolean isValidDestinationName(String name);

    /**
     * Returns true if this is a null or empty string
     * 
     * @param s String
     * @return boolean
     */
    protected boolean empty(String s) {
        return s == null || s.length() == 0;
    }

    /**
     * Throws an exception if the specified string is empty
     * 
     * @param name String
     * @param toTest String
     */
    private void assertNotEmpty(String name, String toTest) {
        if (empty(toTest)) {
            throw Exc.illarg(LOCALE.x("E135: {0} cannot be empty", name));
        }
    }

    // ActivationSpec interface defined methods

    /**
     * Validates the configuration of this ActivationSpec instance
     * ActivationSpec instance.
     * 
     * @throws InvalidPropertyException If this activation spec instance has any
     *             invalid property
     */
    public void validate() throws InvalidPropertyException {
        if (!isValidDestinationName(mDestination)) {
            throw Exc.invprop(LOCALE.x("E136: Invalid destination: {0}", mDestination));
        }

        // Check subscription
        if (TOPIC.equals(mDestinationType) && DURABLE.equals(mSubscriptionDurability)) {
            if (empty(mSubscriptionName)) {
                throw Exc.invprop(LOCALE.x("E137: Missing value for subscription name"));
            }
        }

        // Require valid values of endpoint properties
        if (mEndpointExceptionRedeliveryInterval < 1) {
            throw Exc.invprop(LOCALE.x(
                "E138: {0} is not a valid value for ''{1}''"
                    , Integer.toString(mEndpointExceptionRedeliveryInterval), "endpointExceptionRedeliveryInterval"));
        }
        if (mEndpointExceptionRedeliveryAttempts < 0) {
            throw Exc.invprop(LOCALE.x(
                "E138: {0} is not a valid value for ''{1}''"
                    , Integer.toString(mEndpointExceptionRedeliveryAttempts), "endpointExceptionRedeliveryAttempts"));
        }
        if (mEndpointPoolResizeTimeout < 1) {
            throw Exc.invprop(LOCALE.x(
                "E138: {0} is not a valid value for ''{1}''"
                    , Integer.toString(mEndpointPoolResizeTimeout), "endpointPoolResizeTimeout"));
        }
        if (mEndpointPoolResizeCount < 1) {
            throw Exc.invprop(LOCALE.x(
                "E138: {0} is not a valid value for ''{1}''"
                    , Integer.toString(mEndpointPoolResizeCount), "endpointPoolResizeCount"));
        }
        if (mEndpointPoolMaxSize < 1) {
            throw Exc.invprop(LOCALE.x(
                "E138: {0} is not a valid value for ''{1}''"
                    , Integer.toString(mEndpointPoolMaxSize), "endpointPoolMaxSize"));
        }
        if (mEndpointPoolSteadySize < 0) {
            throw Exc.invprop(LOCALE.x(
                "E138: {0} is not a valid value for ''{1}''"
                    , Integer.toString(mEndpointPoolSteadySize), "endpointPoolSteadySize"));
        }
        try {
            RedeliveryHandler.parse(mRedeliveryActions, "", mDestinationType);
        } catch (Exception e) {
            throw Exc.invprop(LOCALE.x(
                "E144: Invalid value for ''redeliveryHandling'' ({0}): {1}", mRedeliveryActions, e), e);
        }
    }

    /**
     * Sets the Resource Adapter Javabean that is associated with this
     * ActivationSpec instance.
     * 
     * @param ra The ResourceAdapter Javabean
     * @throws ResourceException failure
     */
    public void setResourceAdapter(javax.resource.spi.ResourceAdapter ra)
        throws ResourceException {
        synchronized (this) {
            if (mRA != null) {
                throw Exc.rsrcExc(LOCALE.x("E145: Cannot change resource adaptor association "
                    + "once set."));
            }
            if (!(ra instanceof RAJMSResourceAdapter)) {
                throw Exc.rsrcExc(LOCALE.x("E146: Invalid class {0}; must be instance of {1)", 
                    ra.getClass(), RAJMSResourceAdapter.class));
            }
            mRA = ra;
        }
    }

    /**
     * Gets the Resource Adapter Javabean that is associated with this
     * ActivationSpec instance.
     * 
     * @return The ResourceAdapter Javabean
     */
    public javax.resource.spi.ResourceAdapter getResourceAdapter() {
        return mRA;
    }

    // ActivationSpec Javabean configuration methods
    // These Methods can throw java.lang.RuntimeException or subclasses

    /**
     * Sets the type of the destination for the MessageEndpoint consumer
     * 
     * @param destinationType The destination type valid values are
     *            "javax.jms.Queue" and "javax.jms.Topic"
     */
    public void setDestinationType(String destinationType) {
        if (empty(destinationType)) {
            throw Exc.illarg(LOCALE.x("E147: ''destinationType'' can not be null"));
        }

        // Must be javax.jms.Queue or Topic
        if (QUEUE.equals(destinationType) || TOPIC.equals(destinationType)) {
            mDestinationType = destinationType;
        } else {
            throw Exc.illarg(LOCALE.x("E138: {0} is not a valid value for ''{1}''"
                , destinationType, "destinationType"));
        }
    }

    /**
     * Gets the type of the destination for the MessageEndpoint consumer
     * 
     * @return The destination type values are "javax.jms.Queue" or
     *         "javax.jms.Topic"
     */
    public String getDestinationType() {
        return mDestinationType;
    }

    /**
     * Sets the name of the destination for the MessageEndpoint consumer
     * 
     * @param destination The destination name
     */
    public void setDestination(String destination) {
        if (!isValidDestinationName(destination)) {
            throw Exc.illarg(LOCALE.x("E138: {0} is not a valid value for ''{1}''"
                , destination, "destinationName"));
        }
        mDestination = destination;
    }

    /**
     * Gets the name of the destination for the MessageEndpoint consumer
     * 
     * @return The destination name
     */
    public String getDestination() {
        return mDestination;
    }

    /**
     * Sets the message selector for the MessageEndpoint consumer
     * 
     * @param messageSelector The selector
     */
    public void setMessageSelector(String messageSelector) {
        mMessageSelector = messageSelector;
    }

    /**
     * Gets the message selector for the MessageEndpoint consumer
     * 
     * @return The message selector
     */
    public String getMessageSelector() {
        return mMessageSelector;
    }

    /**
     * Sets the acknowledgement mode for the MessageEndpoint consumer
     * 
     * @param acknowledgeMode The acknowledgement mode valid values are
     *            "Auto-acknowledge" and "Dups-ok-acknowledge"
     */
    public void setAcknowledgeMode(String acknowledgeMode) {
        assertNotEmpty("acknowledgeMode", acknowledgeMode);

        // Must be Auto-acknowledge or Dups-ok-acknowledge
        if (AUTOACKNOWLEDGE.equals(acknowledgeMode)
            || DUPSOKACKNOWLEDGE.equals(acknowledgeMode)) {
            this.mAcknowledgeMode = acknowledgeMode;
        } else {
            throw Exc.illarg(LOCALE.x("E150: Invalid value for ''acknowledgeMode'' ({0})"
                + "; must be {1} or {2}", acknowledgeMode, AUTOACKNOWLEDGE, DUPSOKACKNOWLEDGE));
        }
    }

    /**
     * Gets the acknowledgement mode for the MessageEndpoint consumer
     * 
     * @return The acknowledgement mode one of either "Auto-acknowledge" or
     *         "Dups-ok-acknowledge" or null
     */
    public String getAcknowledgeMode() {
        return mAcknowledgeMode;
    }

    /**
     * Sets the subscription durability for the MessageEndpoint consumer
     * 
     * @param subscriptionDurability The durability mode valid values are
     *            "Durable" and "NonDurable"
     */
    public void setSubscriptionDurability(String subscriptionDurability) {
        assertNotEmpty("subscriptionDurability", subscriptionDurability);

        if (DURABLE.equals(subscriptionDurability)
            || NONDURABLE.equals(subscriptionDurability)) {
            this.mSubscriptionDurability = subscriptionDurability;
        } else {
            throw Exc.illarg(LOCALE.x("E151: Invalid value for ''subscriptionDurability'' ({0})"
                + "; must be {1} or {2}", subscriptionDurability, DURABLE, NONDURABLE));
        }
    }

    /**
     * Gets the subscription durability for the MessageEndpoint consumer
     * 
     * @return The subscription durability one of either "Durable" or
     *         "NonDurable" or null
     */
    public String getSubscriptionDurability() {
        return mSubscriptionDurability;
    }

    /**
     * Sets the client identifier for the MessageEndpoint consumer
     * 
     * @param clientId The client identifier
     */
    public void setClientId(String clientId) {
        mClientId = clientId;
    }

    /**
     * Return the client identifier for the MessageEndpoint consumer
     * 
     * @return The client identifier
     */
    public String getClientId() {
        return mClientId;
    }

    /**
     * Sets the subscription name for the MessageEndpoint consumer
     * 
     * @param subscriptionName The name of the subscription
     */
    public void setSubscriptionName(String subscriptionName) {
        mSubscriptionName = subscriptionName;
    }

    /**
     * Returns the subscription name for the MessageEndpoint consumer
     * 
     * @return The name of the subscription
     */
    public String getSubscriptionName() {
        return mSubscriptionName;
    }

    // Provider specific values

    /**
     * Sets the endpointPoolMaxSize for the MessageEndpoint consumer
     * 
     * @param endpointPoolMaxSize The endpointPoolMaxSize
     */
    public void setEndpointPoolMaxSize(Integer endpointPoolMaxSize) {
        if (endpointPoolMaxSize.intValue() < 1) {
            throw Exc.illarg(LOCALE.x("E138: {0} is not a valid value for ''{1}''"
                , endpointPoolMaxSize, "endpointPoolMaxSize"));
        }
        mEndpointPoolMaxSize = endpointPoolMaxSize.intValue();
    }

    /**
     * Sets the endpointPoolMaxSize for the MessageEndpoint consumer
     * 
     * @param endpointPoolMaxSize The endpointPoolMaxSize
     */
    public void setEndpointPoolMaxSize(String endpointPoolMaxSize) {
        int iEndpointPoolMaxSize = Integer.parseInt(endpointPoolMaxSize);
        if (iEndpointPoolMaxSize < 1) {
            throw Exc.illarg(LOCALE.x("E138: {0} is not a valid value for ''{1}''"
                , endpointPoolMaxSize, "endpointPoolMaxSize"));
        }
        mEndpointPoolMaxSize = iEndpointPoolMaxSize;
    }
    /**
     * Returns the endpointPoolMaxSize for the MessageEndpoint consumer
     * 
     * @return The endpointPoolMaxSize
     */
    public Integer getEndpointPoolMaxSize() {
        return new Integer(mEndpointPoolMaxSize);
    }

    /**
     * Sets the endpointPoolSteadySize for the MessageEndpoint consumer
     * 
     * @param endpointPoolSteadySize The endpointPoolSteadySize
     */
    public void setEndpointPoolSteadySize(String endpointPoolSteadySize) {
        int iEndpointPoolSteadySize = Integer.parseInt(endpointPoolSteadySize);
        if (iEndpointPoolSteadySize < 0) {
            throw Exc.illarg(LOCALE.x("E138: {0} is not a valid value for ''{1}''"
                , endpointPoolSteadySize, "endpointPoolSteadySize"));
        }
        mEndpointPoolSteadySize = iEndpointPoolSteadySize;
    }

    /**
     * Returns the endpointPoolSteadySize for the MessageEndpoint consumer
     * 
     * @return The endpointPoolSteadySize
     */
    public int getEndpointPoolSteadySize() {
        return mEndpointPoolSteadySize;
    }

    /**
     * Sets the endpointPoolResizeCount for the MessageEndpoint consumer
     * 
     * @param endpointPoolResizeCount The endpointPoolResizeCount
     */
    public void setEndpointPoolResizeCount(String endpointPoolResizeCount) {
        int iEndpointPoolResizeCount = Integer.parseInt(endpointPoolResizeCount);
        if (iEndpointPoolResizeCount < 1) {
            throw Exc.illarg(LOCALE.x("E138: {0} is not a valid value for ''{1}''"
                , endpointPoolResizeCount, "endpointPoolResizeCount"));
        }
        this.mEndpointPoolResizeCount = iEndpointPoolResizeCount;
    }

    /**
     * Returns the endpointPoolResizeCount for the MessageEndpoint consumer
     * 
     * @return The endpointPoolResizeCount
     */
    public int getEndpointPoolResizeCount() {
        return mEndpointPoolResizeCount;
    }

    /**
     * Sets the endpointPoolResizeTimeout for the MessageEndpoint consumer
     * 
     * @param endpointPoolResizeTimeout The endpointPoolResizeTimeout
     */
    public void setEndpointPoolResizeTimeout(String endpointPoolResizeTimeout) {
        int iEndpointPoolResizeTimeout = Integer.parseInt(endpointPoolResizeTimeout);
        if (iEndpointPoolResizeTimeout < 1) {
            throw Exc.illarg(LOCALE.x("E138: {0} is not a valid value for ''{1}''"
                , endpointPoolResizeTimeout, "endpointPoolResizeTimeout"));
        }
        mEndpointPoolResizeTimeout = iEndpointPoolResizeTimeout;
    }

    /**
     * Returns the endpointPoolResizeTimeout for the MessageEndpoint consumer
     * 
     * @return The endpointPoolResizeTimeout
     */
    public int getEndpointPoolResizeTimeout() {
        return mEndpointPoolResizeTimeout;
    }

    /**
     * Sets the maximum number of Redelivery attempts to an Endpoint that throws
     * an Exception. This enables the RA to stop endlessly delivering messages
     * to an Endpoint that repeatedly throws an Exception
     * 
     * @param endpointExceptionRedeliveryAttempts The maximum number of
     *            Redelivery attempts
     */
    public void setEndpointExceptionRedeliveryAttempts(
        int endpointExceptionRedeliveryAttempts) {
        if (endpointExceptionRedeliveryAttempts < 0) {
            throw Exc.illarg(LOCALE.x("E138: {0} is not a valid value for ''{1}''"
                , Integer.toString(endpointExceptionRedeliveryAttempts)
                , "endpointExceptionRedeliveryAttempts")); 
        }
        mEndpointExceptionRedeliveryAttempts = endpointExceptionRedeliveryAttempts;
    }

    /**
     * Returns the the maximum number of Redelivery attempts to an Endpoint that
     * throws an Exception. This enables the RA to stop endlessly delivering
     * messages to an Endpoint that repeatedly throws an Exception
     * 
     * @return The maximum number of Redelivery attempts to an Endpoint.
     */
    public int getEndpointExceptionRedeliveryAttempts() {
        return mEndpointExceptionRedeliveryAttempts;
    }

    /**
     * Sets the interval for Redelivery attempts to an Endpoint that throws an
     * Exception.
     * 
     * @param endpointExceptionRedeliveryInterval The maximum number of
     *            Redelivery attempts
     */
    public void setEndpointExceptionRedeliveryInterval(
        int endpointExceptionRedeliveryInterval) {
        if (endpointExceptionRedeliveryInterval < 1) {
            throw Exc.illarg(LOCALE.x("E138: {0} is not a valid value for ''{1}''"
                , Integer.toString(endpointExceptionRedeliveryInterval)
                , "endpointExceptionRedeliveryInterval"));
        }
        mEndpointExceptionRedeliveryInterval = endpointExceptionRedeliveryInterval;
    }

    /**
     * Returns the interval for Redelivery attempts to an Endpoint that throws
     * an Exception.
     * 
     * @return The interval for Redelivery attempts to an Endpoint.
     */
    public int getEndpointExceptionRedeliveryInterval() {
        return mEndpointExceptionRedeliveryInterval;
    }

    /**
     * Sets the ContextClassLoader to be used for the MessageEndpoint consumer
     * 
     * @param contextClassLoader The contextClassLoader
     */
    public void setContextClassLoader(ClassLoader contextClassLoader) {
        mContextClassLoader = contextClassLoader;
    }

    /**
     * Returns the contextClassLoader used for the MessageEndpoint consumer
     * 
     * @return The contextClassLoader
     */
    public ClassLoader getContextClassLoader() {
        return mContextClassLoader;
    }

    /**
     * Sets delivery to serial (no connection consumer or multiple receivers)
     * 
     * @param mode True or False
     */
    public void setConcurrencyMode(String mode) {
        for (int i = 0; i < DELIVERYCONCURRENCY_STRS.length; i++) {
            if (DELIVERYCONCURRENCY_STRS[i].equalsIgnoreCase(mode)) {
                mDeliveryConcurrencyMode = i;
                return;
            }
        }

        String values = "";
        for (int i = 0; i < DELIVERYCONCURRENCY_STRS.length; i++) {
            values += (i != 0 ? ", " : "") + DELIVERYCONCURRENCY_STRS[i];
        }

        throw Exc.illarg(LOCALE.x("E139: {0} is not a valid value for ''{1}''; must be one of {2}"
            , mode, "concurrencyMode", values));
    }

    /**
     * Returns whether delivery is serial (no connection consumer or multiple
     * receivers)
     * 
     * @return boolean
     */
    public String getConcurrencyMode() {
        return DELIVERYCONCURRENCY_STRS[mDeliveryConcurrencyMode];
    }

    /**
     * getDeliveryConcurrencyMode
     * 
     * @return int
     */
    public int getInternalDeliveryConcurrencyMode() {
        return mDeliveryConcurrencyMode;
    }

    /**
     * setConnectionURL
     * 
     * @param url String
     */
    public void setConnectionURL(String url) {
        mConnectionURL = url;
    }

    /**
     * getConnectionURL
     * 
     * @return String
     */
    public String getConnectionURL() {
        return mConnectionURL;
    }

    /**
     * setUserName
     * 
     * @param username String
     */
    public void setUserName(String username) {
        mUsername = username;
    }

    /**
     * getUserName
     * 
     * @return String
     */
    public String getUserName() {
        return mUsername;
    }

    /**
     * setPassword
     * 
     * @param password String
     */
    public void setPassword(String password) {
        mPassword = password;
    }

    /**
     * getPassword
     * 
     * @return String
     */
    public String getClearTextPassword() {
        return Str.pwdecode(mPassword);
    }

    /**
     * getPassword
     * 
     * @return String
     */
    public String getPassword() {
        return mPassword;
    }

    /**
     * getContextName
     * 
     * @return String
     */
    public String getContextName() {
        return mContextName;
    }

    /**
     * setContextName
     * 
     * @param name String
     */
    public void setContextName(String name) {
        mContextName = name;
    }

    /**
     * getMBeanName
     * 
     * @return String
     */
    public String getMBeanName() {
        return mMBeanName;
    }

    /**
     * setMBeanName
     * 
     * @param name String
     */
    public void setMBeanName(String name) {
        mMBeanName = name;
    }
    
    /**
     * Sets the number of messages that should be delivered to the MDB in one transaction
     * 
     * @param size int
     */
    public void setBatchSize(String size) {
        mBatchSize = Integer.parseInt(size);
    }
    
    /**
     * Sets the number of messages that should be delivered to the MDB in one transaction
     * 
     * @param size int
     */
    public void setBatchSize(Integer size) {
        mBatchSize = size.intValue();
    }
    
    /**
     * Returns the number of messages that should be delivered to the MDB in one 
     * transaction
     * 
     * @return int
     */
    public Integer getBatchSize() {
        return new Integer(mBatchSize);
    }
    
    /**
     * Sets the holdUntilAck mode; this will hold the session (and transaction) until
     * the acknowledge/recover method is called on the message
     * 
     * @param huaMode mode, see header
     */
    public void setHoldUntilAck(String huaMode) {
        mHoldUntilAckMode = huaMode;
    }
    
    /**
     * @return the holdUntilAck mode
     */
    public String getHoldUntilAck() {
        return mHoldUntilAckMode;
    }

    /**
     * Sets the options; options are used to pass to the provider's connection
     * factory.
     * 
     * @param options String
     */
    public void setOptions(String options) {
        mOptionsStr = options;
    }

    /**
     * Accessor for mOptionsStr
     * 
     * @return options
     */
    public String getOptions() {
        return mOptionsStr;
    }
    
    /**
     * Gets the delays for a a repeatedly redelivered message
     * 
     * @return delays
     */
    public String getRedeliveryHandling() {
        return mRedeliveryActions;
    }
    
    /**
     * Sets the delays for a repeatedly redelivered message.
     * 
     * The delays string is of the following form:
     * entry := index:delay 
     * delays := entry[;entry]*
     * 
     * Example:
     * 1:10;5:100;10:5000;15:move(queue:dlq$);
     * 
     * This means a delay of 10 ms on the first to fourth redelivery, 100 ms on the 5th
     * to 9th redelivery, and 5 seconds for the 10th redelivery and upwards. On the 15th
     * redelivery the message is moved to a queue with the name dlqOriginalQueueName
     * 
     * @param delays String
     */
    public void setRedeliveryHandling(String delays) {
        mRedeliveryActions = delays;
    }
    
    /**
     * Getter for ConfigurationTemplate; for CAPS only, not used by JMSJCA
     *
     * @return String
     */
    public String getConfigurationTemplate() {
        return mConfigurationTemplate;
    }

    /**
     * Setter for ConfigurationTemplate, for CAPS only -- not used for JMSJCA
     *
     * @param configurationTemplate The configuration template to set.
     */
    public void setConfigurationTemplate(String configurationTemplate) {
        mConfigurationTemplate = configurationTemplate;
    }
    
    /**
     * Getter for ProjectInfo; for CAPS only, not used by JMSJCA
     *
     * @return String
     */
    public String getProjectInfo() {
        return mProjectInfo;
    }

    /**
     * Setter for ProjectInfo, for CAPS only -- not used for JMSJCA
     *
     * @param projectInfo The project info to set.
     */
    public void setProjectInfo(String projectInfo) {
        mProjectInfo = projectInfo;
    }

    /**
     * toString
     * 
     * @return String
     */
    @Override
    public String toString() {
        return getClass().getName();
    }

    /**
     * toString
     * 
     * @return String
     */
    public String dumpConfiguration() {
        return "ActivationSpec configuration=\n"
            + "\tDestinationType                     =" + mDestinationType + "\n"
            + "\tDestination                         =" + mDestination + "\n"
            + "\tMessageSelector                     =" + mMessageSelector + "\n"
            + "\tAcknowledgeMode                     =" + mAcknowledgeMode + "\n"
            + "\tDelivery concurrency mode           ="
            + DELIVERYCONCURRENCY_STRS[mDeliveryConcurrencyMode] + "\n"
            + "\tSubscriptionDurability              =" + mSubscriptionDurability + "\n"
            + "\tClientId                            =" + mClientId + "\n"
            + "\tSubscriptionName                    =" + mSubscriptionName + "\n"
            + "\tEndpointPoolMaxSize                 =" + mEndpointPoolMaxSize + "\n"
            + "\tEndpointPoolSteadySize              =" + mEndpointPoolSteadySize + "\n"
            + "\tEndpointPoolResizeCount             =" + mEndpointPoolResizeCount + "\n"
            + "\tEndpointPoolResizeTimeout           =" + mEndpointPoolResizeTimeout
            + "\n" + "\tEndpointExceptionRedeliveryAttempts ="
            + mEndpointExceptionRedeliveryAttempts + "\n"
            + "\tEndpointExceptionRedeliveryInterval ="
            + mEndpointExceptionRedeliveryInterval + "\n"
            + "\tConnectionURL                       =" + mConnectionURL + "\n"
            + "\tUserName                            =" + mUsername + "\n"
            + "\tPassword                            =" + Str.password(mPassword) + "\n"
            + "\tContextName                         =" + mContextName + "\n"
            + "\tMBeanName                           =" + mMBeanName + "\n"
            + "\tOptionsStr                          =" + mOptionsStr + "\n"
            + "\tRedeliveryHandling                  =" + mRedeliveryActions;
    }
    
    /**
     * Checks for a java.util.Properties object in JNDI and if found, overrides the 
     * configuration of the spec with the one in the Properties object.
     * 
     * @return true if the RA spec was changed
     */
    public boolean overrideRASpecFromJNDI() {
        boolean changed = false;
        if (!Str.empty(getContextName())) {
            Context ctx = null;            
            String jndiName = "capsenv/CM/" + getContextName();
            if (sLog.isDebugEnabled()) {
                sLog.debug("Lookup in JNDI: " + jndiName);
            }
            try {
                // Lookup object
                ctx = new InitialContext();
                Object o = ctx.lookup(jndiName);
                if (sLog.isDebugEnabled()) {
                    sLog.debug("Found in JNDI using [" + jndiName + "]: " + o);
                }
                
                // Cast
                if (!(o instanceof Properties)) {
                    throw Exc.exc(LOCALE.x("E191: Incompatible object specified: the object must of type ''{0}'' "
                        + "but the object bound to ''{1}'' is of type ''{2}''", Properties.class.getName()
                        , jndiName, o.getClass().getName()));
                }

                // Change values                
                Properties p = (Properties) o;
                Enumeration<?> e = p.propertyNames();

                // Replace the "key=<key>" to "key=''"
                while (e.hasMoreElements()) {
                    String key = (String) e.nextElement();
                    String value = p.getProperty(key);
                    if (value != null && value.equals("<" + key + ">")) {
                        p.setProperty(key, "");
                    }
                }
                
                setConcurrencyMode(p.getProperty("ConcurrencyMode", getConcurrencyMode()));
                setDestination(p.getProperty("Destination", getDestination()));
                setDestinationType(p.getProperty("DestinationType", getDestinationType()));
                setSubscriptionDurability(p.getProperty("SubscriptionDurability", getSubscriptionDurability()));
                setSubscriptionName(p.getProperty("SubscriptionName", getSubscriptionName()));
                setClientId(p.getProperty("ClientId", getClientId()));
                setEndpointPoolMaxSize(p.getProperty("EndpointPoolMaxSize", getEndpointPoolMaxSize().toString()));
                setMBeanName(p.getProperty("MBeanName", getMBeanName()));
                setMessageSelector(p.getProperty("MessageSelector", getMessageSelector()));
                setRedeliveryHandling(p.getProperty("RedeliveryHandling", getRedeliveryHandling()));
                
                changed = true;
            } catch (NamingException e) {
                if (sLog.isDebugEnabled()) {
                    sLog.debug("Could not find " + jndiName + ": " + e, e);
                }
            } catch (Exception e) {
                sLog.warn(LOCALE.x("E193: The configuration overrides bound to JNDI name " 
                    + "''{0}'' could not be fully applied; some default values will be used instead. " 
                    + "The error was: {1}", jndiName, e), e);
            } finally {
                if (ctx != null) {
                    try {
                        ctx.close();
                    } catch (Exception ignore) {
                        // ignore
                    }
                }
            }
        }
        
        return changed;
    }
}
