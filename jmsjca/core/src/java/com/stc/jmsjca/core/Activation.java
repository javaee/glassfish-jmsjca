/*
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the "License").  You may not use this file except
 * in compliance with the License.
 *
 * You can obtain a copy of the license at
 * https://glassfish.dev.java.net/public/CDDLv1.0.html.
 * See the License for the specific language governing
 * permissions and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL
 * HEADER in each file and include the License file at
 * https://glassfish.dev.java.net/public/CDDLv1.0.html.
 * If applicable add the following below this CDDL HEADER,
 * with the fields enclosed by brackets "[]" replaced with
 * your own identifying information: Portions Copyright
 * [year] [name of copyright owner]
 */
/*
 * $RCSfile: Activation.java,v $
 * $Revision: 1.3 $
 * $Date: 2007-01-21 17:51:38 $
 *
 * Copyright 2003-2007 Sun Microsystems, Inc. All Rights Reserved.  
 */

package com.stc.jmsjca.core;

import com.stc.jmsjca.util.Logger;
import com.stc.jmsjca.util.Str;
import com.stc.jmsjca.util.Utility;

import javax.management.MBeanServer;
import javax.management.ObjectName;
import javax.resource.spi.endpoint.MessageEndpointFactory;

import java.lang.reflect.Method;
import java.util.Properties;

/**
 * This represents one single endpoint-activation; it handles all activities associated
 * with an endpoint-activation together with the Delivery class.
 *
 * <P>An activation is identified by its MessageEndpointFactory and its associated
 * ActivationSpec.
 * 
 * Threading model: 
 * - app sever mgt thread calls activate, deactivate
 * - mbean treads call suspend/unsuspend
 * - jms thread calls distress()
 * 
 * States: DISCONNECTED, CONNECTING, CONNECTED, DISCONNECTING
 * The connector is DISCONNECTED when created. Calling activate() will call start()
 * which will start a new thread that will try to connect to the JMS server. The state
 * is CONNECTING. This is done in a new thread to allow for JMS servers that are not
 * up yet. This thread will retry until success, after which the state is 
 * CONNECTED. Calling stop() will cause the state to change to DISCONNECTING. Both 
 * CONNECTING and DISCONNECTING may take a long time.
 * 
 * These are the state transitions:
 * disconnected --activate--> connecting -------ok------> connected
 *              <----stop----            <--distress----- 
 * 
 * disconnected <-----ok----- disconnecting <----stop---- connected
 * 
 * start(): exit CONNECTED || CONNECTING
 * - if disconnected : start
 * - if connecting   : ignore
 * - if connected    : ignore
 * - if disconnecting: fail
 * stop(): exit DISCONNECTED              
 * - if disconnected : ignore
 * - if connecting   : set request flag; wait for status change
 * - if connected    : stop
 * - if disconnecting: wait for status change
 * distress(): exit CONNECTING
 * - if disconnected : fail
 * - if connecting   : ignore
 * - if connected    : stop, start
 * - if disconnecting: ignore
 *
 * @author fkieviet
 * @version $Revision: 1.3 $
 */
public class Activation {
    private static Logger sLog = Logger.getLogger(Activation.class);
    private RAJMSResourceAdapter mRA;
    private MessageEndpointFactory mEndpointFactory;
    private RAJMSActivationSpec mSpec;
    private Method mOnMessageMethod;
    private DeliveryStats mStats;
    private Delivery mDelivery;
    private boolean mIsXA;
    private boolean mIsTopic;
    private boolean mIsDurable;
    private ActivationMBean mActivationMBean;
    private ObjectName mServerMgtMBeanName;
    private int mDeliveryMode;
    private String mName;
    
    /**
     * All states in string format (for diagnostics)
     */
    private static final String[] STATES = new String[] {"Disconnected", "Connecting", "Connected", "Disconnecting" };
    /**
     * The connector is disconnected
     */
    public static final int DISCONNECTED = 0;
    
    /**
     * The connector is in the process of connecting, either physically or as part of
     * a retry mechanism 
     */
    public static final int CONNECTING = 1;
    
    /**
     * The connector is connected and may be delivering messages 
     */
    public static final int CONNECTED = 2;
    
    /**
     * The connector is in the process of disconnecting 
     */
    public static final int DISCONNECTING = 3;
    
    private Object mLock = new Object();
    private int mState = DISCONNECTED;
    private boolean mXConnectingInterruptRequest;
    private boolean mRedeliveryRedirect;
    private RAJMSObjectFactory mObjFactory;

    /**
     * Activation constructor
     *
     * @param ra RAJMSResourceAdapter
     * @param epf MessageEndpointFactory
     * @param spec RAJMSActivationSpec
     */
    public Activation(RAJMSResourceAdapter ra, MessageEndpointFactory epf,
        RAJMSActivationSpec spec) {
        mRA = ra;
        mEndpointFactory = epf;
        mSpec = spec;
        String url = spec.getConnectionURL();
        if (url == null || url.length() == 0) {
            url = ra.getConnectionURL();
        }
        mObjFactory = ra.createObjectFactory(url);
        mName = spec.getDestinationType() + " [" + spec.getDestination() + "] on [" 
        + url + "]"; 
    }
    
    /**
     * Returns the effective delivery mode
     * 
     * @return int
     */
    public int getDeliveryMode() {
        return mDeliveryMode;
    }
    
    /**
     * Sleeps for a set number of milliseconds or until the status changes
     * 
     * @param sleepInMilliseconds how long to sleep
     */
    public void sleepAndMonitorStatus(long sleepInMilliseconds) {
        synchronized (mLock) {
            try {
                if (mState != CONNECTED) {
                    return;
                }
                mLock.wait(sleepInMilliseconds);
            } catch (InterruptedException ignore) {
                // ignore
            }
        }
    }

    /**
     * getObjectFactory
     *
     * @return RAJMSObjectFactory
     */
    public RAJMSObjectFactory getObjectFactory() {
        return mObjFactory;
    }

    /**
     * Starts message delivery
     * 
     * @throws Exception on failure
     */
    public void activate() throws Exception {
        try {
            // Cache the onMessage-method
            try {
                Class msgListenerClass = javax.jms.MessageListener.class;
                Class[] paramTypes = {javax.jms.Message.class };
                mOnMessageMethod = msgListenerClass.getMethod("onMessage", paramTypes);
            } catch (NoSuchMethodException ex) {
                sLog.fatal(mName + ": could not locate onMessage() function: " + ex, ex);
                throw new RuntimeException(
                    "Could not locate onMessage() function: " + ex, ex);
            }

            // Determine if XA, topic, and cache, support Non-XA
            mIsXA = mEndpointFactory.isDeliveryTransacted(mOnMessageMethod);
            
            // Override properties
            Properties p = new Properties();
            getObjectFactory().getProperties(p, getRA(), getActivationSpec(),
                null, null);
            if (mIsXA) {
                boolean noXA = Utility.isTrue(p.getProperty(Options.NOXA), false);
                noXA = Utility.getSystemProperty(Options.NOXA, noXA);
                mIsXA = !noXA;
            }
            
            // Extract options for redelivery handling
            mRedeliveryRedirect = Utility.isTrue(p.getProperty(Options.In.OPTION_REDIRECT), false);
            String redeliveryHandling = p.getProperty(Options.In.OPTION_REDELIVERYHANDLING
                , mSpec.getRedeliveryHandling());
            RedeliveryHandler.parse(redeliveryHandling, mSpec.getDestination(), mSpec.getDestinationType());
            mSpec.setRedeliveryHandling(redeliveryHandling);
            
            mIsTopic = RAJMSActivationSpec.TOPIC.equals(mSpec.getDestinationType());
            mIsDurable = RAJMSActivationSpec.DURABLE.equals(
                mSpec.getSubscriptionDurability());
            if (sLog.isDebugEnabled()) {
                sLog.debug("XA: " + mIsXA + "; isTopic: " + mIsTopic + "; isDurable: "
                    + mIsDurable);
            }

            // Create MBean
            if (getActivationSpec().getMBeanName() != null
                && getActivationSpec().getMBeanName().length() != 0) {
                ObjectName mbeanName = new ObjectName(getActivationSpec().getMBeanName());
                MBeanServer mbeanServer = getRA().getMBeanServer();
                mActivationMBean = getObjectFactory().createActivationMBean(this);
                mbeanServer.registerMBean(mActivationMBean, mbeanName);
                if (sLog.isDebugEnabled()) {
                    sLog.debug("Registered MBean [" + mActivationMBean + "] in server ["
                        + mbeanServer.getDefaultDomain() + "] using name [" + mbeanName
                        + "]");
                }

                // Setup JMS Server mbean
                Object serverMgtMBean = getObjectFactory().getServerMgtMBean(
                    getRA(), getActivationSpec());
                if (sLog.isDebugEnabled()) {
                    sLog.debug("Server mgt mbean=" + serverMgtMBean);
                }
                if (serverMgtMBean == null) {
                    // MBean feature not supported
                } else if (serverMgtMBean instanceof String) {
                    // Refers to an existing mbean name
                    mActivationMBean.setJmsServerMBean((String) serverMgtMBean);
                } else {
                    // Refers to a newly created mbean; need to register
                    String name = getActivationSpec().getMBeanName() + ",servermgt=true";
                    if (sLog.isDebugEnabled()) {
                        sLog.debug("Registering server mgt mbean with name " + name);
                    }
                    ObjectName mgtmbeanName = new ObjectName(name);
                    mbeanServer.registerMBean(serverMgtMBean, mgtmbeanName);
                    mServerMgtMBeanName = mgtmbeanName;
                    mActivationMBean.setJmsServerMBean(name);
                    if (sLog.isDebugEnabled()) {
                        sLog.debug("Registered server mgt mbean with name " + name);
                    }
                }
            }

            mStats = new DeliveryStats();

            // Create delivery
            String overridemode = p.getProperty(Options.In.OPTION_CONCURRENCYMODE, null);
            if (overridemode != null) {
                mSpec.setConcurrencyMode(overridemode);
            } 
            mDeliveryMode = mSpec.getDeliveryConcurrencyMode();
            mDeliveryMode = getObjectFactory().adjustDeliveryMode(mDeliveryMode, mIsXA);
            
            internalStart();
        } catch (Exception e) {
            killMBean();
            throw e;
        }
    }
    
    /**
     * Returns the statistics gathering object associated with this activation
     *
     * @return DeliveryStats
     */
    public DeliveryStats getStats() {
        return mStats;
    }
    
    
    // ===== Main Control functions: start, stop, distress =====
    
    private void setState(int state) {
        synchronized (mLock) {
            mState = state;
            mLock.notifyAll();
        }
    }
    
    /**
     * Publishes the state
     * 
     * @return current state (DISCONNECTED, etc)
     */
    public int getState() {
        synchronized (mLock) {
            return mState;
        }
    }
    
    /**
     * Internal start method: performs an async start. This method returns almost
     * immediately.
     * 
     * When this method returns, the state is CONNECTING
     * 
     * @throws Exception
     */
    private void internalStart() throws Exception {
        synchronized (mLock) {
            switch (mState) {
            case DISCONNECTED:
                new Thread(new Runnable() {
                    public void run() {
                        asyncStart();
                    }
                }, "JMSJCA connect").start();
                mXConnectingInterruptRequest = false;
                setState(CONNECTING);
                break;
            case CONNECTING:
                // ignore
                break;
            case CONNECTED:
                // ignore
                break;
            case DISCONNECTING: {
                throw new Exception(
                    "Invalid state: cannot call start() when state is DISCONNECTING");
            }
            }
        }
    }
    

    /**
     * Internal stop method
     * 
     * When this method returns, the state is DISCONNECTED
     */
    private void internalStop() {
        for (;;) {
            synchronized (mLock) {
                try {
                    if (mState == DISCONNECTED) {
                        return;
                    } else if (mState == CONNECTING) {
                        mXConnectingInterruptRequest = true;
                        mLock.wait();
                    } else if (mState == CONNECTED) {
                        setState(DISCONNECTING); 
                        break;
                    } else if (mState == DISCONNECTING) {
                        mLock.wait();
                    }
                } catch (InterruptedException e) {
                    sLog.warn(mName + ": stop() operation was interrupted; state is now " 
                        + STATES[mState]);
                    return;
                }
            }
        }

        // Perform deactivation (may take a long time)
        mDelivery.deactivate();
        mDelivery = null;
        
        synchronized (mLock) {
            setState(DISCONNECTED);
        }
    }
    
    /**
     * Internal distress method.
     * 
     * When this method returns, the state is DISCONNECTING or CONNECTING
     * 
     * Called during activation if start() fails, or by the JMS exception handler of
     * a Delivery object in a started resource adapter
     * 
     * This method returns almost immediately.
     * 
     * @param ex the exception that was encountered
     */
    private void internalDistress(Exception ex) {
        synchronized (mLock) {
            if (mState == DISCONNECTED) {
                sLog.warn(mName + ": inconsistency error: the following exception was encountered "
                    + "while the connector is in DISCONNECTED mode: " + ex, ex);
                return;
            } else if (mState == CONNECTING) {
                sLog.warn(mName + ": the following exception was encountered while initiating or "
                    + "during message delivery: [" + ex
                    + "]; adapter is already in reconnect mode.", ex);
                return;
            } else if (mState == CONNECTED) {
                sLog.warn(mName + ": the following exception was encountered while initiating or "
                    + "during message delivery: [" + ex
                    + "]; attempts will be made to (re-)start message delivery "
                    + "(auto reconnect mode).", ex);
                setState(DISCONNECTING);
                // Asynchronously stop and start
                new Thread(new Runnable() {
                    public void run() {
                        asyncStart();
                    }
                }, "JMSJCA reconnect").start();
                return;
            } else if (mState == DISCONNECTING) {
                // ignore
                return;
            }
        }
    }
    
    /**
     * Internal async start method. Called from a dedicated starting thread.
     * This method may take a (very) long time. It will first stop delivery if 
     * necessary, and then start until starting has succeeded.
     */
    private void asyncStart() {
        int state;
        synchronized (mLock) {
            state = mState;
        }
        
        if (state == DISCONNECTING) {
            // Creator of the thread must have set the state to DISCONNECTING;
            // perform deactivation (may take a long time)
            mDelivery.deactivate();
            mDelivery = null;
            setState(CONNECTING);
        }
        
        int[] dts = {1, 2, 5, 5, 10};
        int attempt = 0;
        long tryAgainAt = 0;

        for (;;) {
            // Check for interruption
            synchronized (mLock) {
                if (mXConnectingInterruptRequest) {
                    mXConnectingInterruptRequest = false;
                    setState(DISCONNECTED);
                    return;
                }
            }
            
            // Attempt initiation
            if (System.currentTimeMillis() > tryAgainAt) {
                try {
                    mDelivery = getObjectFactory().createDelivery(mDeliveryMode, this, mStats);
                    mDelivery.start();
                    sLog.info(mName + ": message delivery initiation was successful.");
                    setState(CONNECTED);
                    break;
                } catch (Exception e) {
                    mDelivery = null;
                    int dt = attempt < dts.length ? dts[attempt] : dts[dts.length - 1];
                    sLog.warn(mName + ": message delivery initiation failed (attempt #" + (attempt  + 1) 
                        + "); will retry in " + dt
                        + " seconds. The error was: " + e);
                    tryAgainAt = System.currentTimeMillis() + dt * 1000;
                    attempt++;
                }
            }
            
            // Check for interruption
            synchronized (mLock) {
                if (mXConnectingInterruptRequest) {
                    mXConnectingInterruptRequest = false;
                    setState(DISCONNECTED);
                    return;
                }
            }

            // Sleep a bit
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                sLog.info(mName + ": message delivery initiation attempt was interrupted");
                break;
            }
        }
    }

    /**
     * Suspends message delivery
     */
    public void stop() {
        internalStop();
    }

    /**
     * Unsuspends message delivery
     *
     * @throws Exception propagated from activation
     */
    public void start() throws Exception {
        internalStart();
    }
    
    /**
     * Halts message delivery
     * Should NOT throw an exception
     */
    public void deactivate() {
        try {
            internalStop();
        } catch (RuntimeException e) {
            sLog.warn("Unexpected exception in endpoint deactivation: " + e, e);
        }
        try {
            killMBean();
        } catch (RuntimeException e) {
            sLog.warn("Unexpected exception in endpoint deactivation: " + e, e);
        }
    }
    
    /**
     * Called during activation if start() fails, or by the JMS exception handler of
     * a Delivery object in a started resource adapter
     * 
     * @param ex the exception that was encountered
     */
    public void distress(Exception ex) {
        internalDistress(ex);
    }
    
    /**
     * Indicates if the delivery is stopped
     * 
     * @return true if stopped
     */
    public boolean isStopped() {
        synchronized (mLock) {
            return mState == DISCONNECTED;
        }
    }
    
    /**
     * Indicates if delivery is in the process of being stopped
     * 
     * @return true if stopping
     */
    public boolean isStopping() {
        synchronized (mLock) {
            return mState == DISCONNECTED || mState == DISCONNECTING;
        }
    }

    /**
     * Get the method used to deliver messages
     *
     * @return Method
     */
    public Method getOnMessageMethod() {
        return mOnMessageMethod;
    }

    /**
     * Returns true if this is an XA setup
     *
     * @return boolean
     */
    public boolean isXA() {
        return mIsXA;
    }

    /**
     * Returns true if this pub/sub
     *
     * @return boolean
     */
    public boolean isTopic() {
        return mIsTopic;
    }

    /**
     * Returns true if this is a durable subscriber; unspecified if isTopic returns false
     *
     * @return boolean
     */
    public boolean isDurable() {
        return mIsDurable;
    }

    private void killMBean() {
        // Get rid of server mbean if a new one was registered just for this activation
        if (mServerMgtMBeanName != null) {
            try {
                getRA().getMBeanServer().unregisterMBean(mServerMgtMBeanName);
            } catch (Exception e) {
                sLog.warn(mName + ": exception on unregistering server mbean: " + e, e);
            }
            mServerMgtMBeanName = null;
        }
        
        // Get rid of normal activation mbean
        if (mActivationMBean != null) {
            mActivationMBean.destroy();
            mActivationMBean = null;
        }
    }

    /**
     * Returns the RA
     *
     * @return FarkResourceAdapter
     */
    public RAJMSResourceAdapter getRA() {
        return mRA;
    }

    /**
     * Returns the MessageEndpointFactory
     *
     * @return MessageEndpointFactory
     */
    public MessageEndpointFactory getMessageEndpointFactory() {
        return mEndpointFactory;
    }

    /**
     * Returns the activation spec used to create this activation
     *
     * @return RAJMSActivationSpec
     */
    public RAJMSActivationSpec getActivationSpec() {
        return mSpec;
    }

    /**
     * Activations are identified by spec and EPF. This method checks to see if the
     * specified spec and EPF would identify this activation.
     *
     * @param epf MessageEndpointFactory
     * @param spec JavaMailActivationSpec
     * @return boolean
     */
    public boolean is(MessageEndpointFactory epf, RAJMSActivationSpec spec) {
        return mEndpointFactory.equals(epf) && mSpec.equals(spec);
    }

    /**
     * toString
     *
     * @return String
     */
    public String toString() {
        return getName();
    }

    /**
     * getUserName
     *
     * @return String
     */
    public String getUserName() {
        String ret = mRA.getUserName();
        if (!Str.empty(mSpec.getUserName())) {
            ret = mSpec.getUserName();
        }

        return ret;
    }

    /**
     * getPassword
     *
     * @return String
     */
    public String getPassword() {
        String ret = mRA.getPassword();
        if (!Str.empty(mSpec.getUserName())) {
            ret = mSpec.getPassword();
        }

        return ret;
    }

    /**
     * For diagnostics, returns the number of endpoints
     * 
     * @return number of endpoints, -1 if none
     */
    public int dumpNumberConfiguredEndpoints() {
        Delivery d = mDelivery;
        if (d != null) {
            return d.getConfiguredEndpoints();
        } else {
            return -1;
        }
    }

    /**
     * For diagnostics, dumps the delivery object
     * 
     * @return String dump
     */
    public String dumpDelivery() {
        Delivery d = mDelivery;
        if (d != null) {
            return d.toString();
        } else {
            return "No delivery object";
        }
    }
    
    /**
     * @return a human friendly name
     */
    public String getName() {
        return mName;
    }
    
    /**
     * @return true if redelivery handling is to redirect rather than forward
     */
    public boolean shouldRedirectRatherThanForward() {
        return mRedeliveryRedirect;
    }
}
