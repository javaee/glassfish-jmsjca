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

import com.stc.jmsjca.localization.LocalizedString;
import com.stc.jmsjca.localization.Localizer;
import com.stc.jmsjca.util.Exc;
import com.stc.jmsjca.util.InterceptorChainBuilder;
import com.stc.jmsjca.util.Logger;
import com.stc.jmsjca.util.Str;
import com.stc.jmsjca.util.Utility;

import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Queue;
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
 * @version $Revision: 1.22 $
 */
public class Activation extends ActivationBase {
    private static Logger sLog = Logger.getLogger(Activation.class);
    private RAJMSResourceAdapter mRA;
    private MessageEndpointFactory mEndpointFactory;
    private RAJMSActivationSpec mSpec;
    private Method mOnMessageMethod;
    private DeliveryStats mStats;
    private Delivery mDelivery;
    private boolean mIsCMT;
    private boolean mIsXAEmulated;
    private boolean mIsTopic;
    private boolean mIsDurable;
    private boolean mMinimalReconnectLogging;
    private boolean mMinimalReconnectLoggingDurSub;
    private ActivationMBean mActivationMBean;
    private ObjectName mServerMgtMBeanName;
    private int mDeliveryMode;
    private boolean mOverrideIsSameRM;
    private volatile Destination mPublishedDestination;

    private static final Localizer LOCALE = Localizer.get();
    
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
    private boolean mWrapAlways;
    private RAJMSObjectFactory mObjFactory;
    private String mURL;
    private boolean mStopByConnectorInProgress;
    private Exception mFailureAtStart;

    /**
     * Activation constructor
     *
     * @param ra RAJMSResourceAdapter
     * @param epf MessageEndpointFactory
     * @param spec RAJMSActivationSpec
     */
    public Activation(RAJMSResourceAdapter ra, MessageEndpointFactory epf,
        RAJMSActivationSpec spec) {
        super(ra, epf, spec);
        mRA = ra;
        mEndpointFactory = epf;
        mSpec = spec;
        String url = spec.getConnectionURL();
        if (url == null || url.length() == 0) {
            url = ra.getConnectionURL();
        }
        mURL = url;
        mObjFactory = ra.createObjectFactory(ra, spec, null);
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
    @Override
    public RAJMSObjectFactory getObjectFactory() {
        return mObjFactory;
    }

    /**
     * Starts message delivery
     * 
     * @throws Exception on failure
     */
    @Override
    public void activate() throws Exception {
        try {
            // Cache the onMessage-method
            try {
                Class<?> msgListenerClass = javax.jms.MessageListener.class;
                Class<?>[] paramTypes = {javax.jms.Message.class };
                mOnMessageMethod = msgListenerClass.getMethod("onMessage", paramTypes);
            } catch (NoSuchMethodException ex) {
                LocalizedString msg = LOCALE.x("E008: {0}: could not locate onMessage() function: {1}", getName(), ex);
                sLog.fatal(msg, ex);
                throw new RuntimeException(msg.toString(), ex);
            }

            // Determine if XA, topic, and cache, support Non-XA
            mIsCMT = mEndpointFactory.isDeliveryTransacted(mOnMessageMethod);
            
            // Override properties
            Properties p = new Properties();
            getObjectFactory().getProperties(p, getRA(), getActivationSpec(),
                null, null);
            if (mIsCMT) {
                boolean forceBMT = Utility.isTrue(p.getProperty(Options.FORCE_BMT), false);
                forceBMT = Utility.getSystemProperty(Options.FORCE_BMT, forceBMT);
                mIsCMT = !forceBMT;
            }
            
            mIsXAEmulated = Utility.getSystemProperty(Options.NOXA, Utility.isTrue(p.getProperty(Options.NOXA), false));
            mOverrideIsSameRM = Utility.isTrue(p.getProperty(Options.OVERRIDEISSAMERM), false);
            // Extract options for redelivery handling
            mRedeliveryRedirect = Utility.isTrue(p.getProperty(Options.In.OPTION_REDIRECT), false);
            mWrapAlways = "1".equals(p.getProperty(Options.In.OPTION_REDELIVERYWRAP, "1"));
            String redeliveryHandling = p.getProperty(Options.In.OPTION_REDELIVERYHANDLING
                , mSpec.getRedeliveryHandling());
            RedeliveryHandler.parse(redeliveryHandling, mSpec.getDestination(), mSpec.getDestinationType());
            mSpec.setRedeliveryHandling(redeliveryHandling);
            mMinimalReconnectLogging = "1".equals(p.getProperty(Options.In.OPTION_MINIMAL_RECONNECT_LOGGING, "0"));
            mMinimalReconnectLoggingDurSub = "1".equals(p.getProperty(
                Options.In.OPTION_MINIMAL_RECONNECT_LOGGING_DURSUB, "0"));
            
            mIsTopic = RAJMSActivationSpec.TOPIC.equals(mSpec.getDestinationType());
            mIsDurable = RAJMSActivationSpec.DURABLE.equals(
                mSpec.getSubscriptionDurability());
            if (sLog.isDebugEnabled()) {
                sLog.debug("CMT: " + mIsCMT + "; isTopic: " + mIsTopic + "; isDurable: "
                    + mIsDurable + "; isXAEmulation: " + mIsXAEmulated);
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
            mDeliveryMode = mSpec.getInternalDeliveryConcurrencyMode();
            mDeliveryMode = getObjectFactory().adjustDeliveryMode(mDeliveryMode, mIsCMT && !mIsXAEmulated);
            
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
                throw new Exception(LOCALE.x("E118: Internal error: " 
                    + "Invalid state: cannot call start() when state is DISCONNECTING").toString());
            }
            default: throw new IllegalStateException("State=" + mState);
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
                    sLog.warn(LOCALE.x("E011: [{0}]: stop() operation was " +
                            "interrupted; state is now {1}", getName(), STATES[mState]));
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
                sLog.warn(LOCALE.x("E012: [{0}]: inconsistency error: the following exception was encountered "
                    + "while the connector is in DISCONNECTED mode: {1}", getName(), ex), ex);
                return;
            } else if (mState == CONNECTING) {
                mFailureAtStart = ex;
//                sLog.warn(LOCALE.x("E013: [{0}]: the following exception was encountered while initiating or "
//                    + "during message delivery: [{1}]; adapter is already in reconnect mode.", getName(), ex), ex);
                return;
            } else if (mState == CONNECTED) {
                sLog.warn(LOCALE.x("E014: [{0}]: the following exception was encountered while initiating or "
                    + "during message delivery: [{1}]; attempts will be made to (re-)start message delivery "
                    + "(auto reconnect mode).", getName(), ex), ex);
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
     * Stops from an MDB; starts a new thread to invoke the stop.
     * This method returns almost immediately.
     * 
     * @param msg the reason for the shutdown
     */
    public void stopConnectorByMDB(String msg) {
        // Avoid starting multiple threads that all will call stop()
        synchronized (mLock) {
            if (mStopByConnectorInProgress) {
                return;
            }
            mStopByConnectorInProgress = true;
        }
        
        try {
            sLog.warn(LOCALE.x("E114: [{0}]: the MDB requested a shutdown of the connector. " 
                + "No messages will be delivered until message delivery is restarted. "
                + "The reason for the shutdown is: {1}", getName(), msg));

            Thread t = new Thread("JMSJCA shutdown by MDB") {
                @Override
                public void run() {
                    try {
                        if (sLog.isDebugEnabled()) {
                            sLog.debug("Starting deactivation by MDB");
                        }
                        Activation.this.stop();
                        if (sLog.isDebugEnabled()) {
                            sLog.debug("Deactivation by MDB finished");
                        }
                    } finally {
                        synchronized (mLock) {
                            mStopByConnectorInProgress = false;
                        }
                    }
                }
            };
            t.start();
        } catch (RuntimeException e) {
            synchronized (mLock) {
                mStopByConnectorInProgress = false;
            }
            throw e;
        }
    }

    /**
     * Creates a delivery
     * 
     * @return new delivery
     * @throws Exception propagated
     */
    protected Delivery createDelivery() throws Exception { 
        return getObjectFactory().createDelivery(mDeliveryMode, this, mStats);
    }
    
    /**
     * Internal async start method. Called from a dedicated starting thread.
     * This method may take a (very) long time. It will first stop delivery if 
     * necessary, and then start until starting has succeeded.
     */
    private void asyncStart() {
        enterContext();
        try {
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
                        mDelivery = createDelivery();
                        
                        // Problems may occur right after start: capture those in a flag
                        // Reset the flag first
                        synchronized (mLock) {
                            mFailureAtStart = null;
                        }
                        
                        // Start... async errors may occur during or right after start()
                        mStats.resetDeliveryStats();
                        mDelivery.start();

                        // Give some time for immediate async errors to occur
                        // Since this is a thread dedicated to starting, this is not
                        // wastful. This will limit the cycle time of a situation where 
                        // where an internalDistress systematically happens immediately 
                        // after start.
                        Thread.sleep(500);
                        
                        synchronized (mLock) {
                            // Check for async errors during or right after start 
                            if (mFailureAtStart != null) {
                                throw mFailureAtStart;
                            }
                            sLog.info(LOCALE.x("E015: [{0}]: message delivery initiation was successful.", getName()));
                            setState(CONNECTED);
                        }
                        break;
                    } catch (Exception e) {
                        Exc.checkLinkedException(e);
                        if (mDelivery != null) {
                            mDelivery.deactivate();
                            mDelivery = null;
                        }
                        int dt = attempt < dts.length ? dts[attempt] : dts[dts.length - 1];
                        logDeliveryInitiationException(attempt + 1, dt, e);
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
                    sLog.info(LOCALE.x("E017: [{0}]: message delivery initiation attempt was interrupted", getName()));
                    break;
                }
            }
        } finally {
            exitContext();
        }
    }

    /**
     * Logs a failure when the delivery initiation attempt fails
     * 
     * @param attemptPlusOne one-based attempt index
     * @param dt time to wait until next attempt
     * @param e exception
     */
    protected void logDeliveryInitiationException(int attemptPlusOne, int dt, Exception e) {
        if (!mMinimalReconnectLogging && !mMinimalReconnectLoggingDurSub) {
            sLog.warn(LOCALE.x("E016: [{0}]: message delivery initiation failed " +
                "(attempt #{1}); will retry in {2} seconds. " +
                "The error was: {3}", 
                getName(), Integer.toString(attemptPlusOne), Integer.toString(dt), e), e);
        } else {
            if (attemptPlusOne > 1) {
                // Ignore repeated errors
            } else {
                Throwable ex = e instanceof Exc.ConsumerCreationException ? e.getCause() : e;
                if (e instanceof Exc.ConsumerCreationException && mMinimalReconnectLoggingDurSub) {
                    sLog.info(LOCALE.x("E204: [{0}]: message delivery could not be initiated due to " +
                        "a failure to create the subscriber. Assuming that this deployment is on a node in " + 
                        "a cluster, there is likely another cluster node already receiving messages from " +
                        "this subscriber. The subscriber creation attempt will be retried " +
                        "periodically to detect when the active subscriber disconnects. " +
                        "Subsequent unsuccessful attempts " +
                        "to subscribe will not be logged. The subscriber could not created because of the " +
                        "following error: {3}", 
                        getName(), Integer.toString(attemptPlusOne), Integer.toString(dt), ex), ex);
                } else {
                    sLog.error(LOCALE.x("E205: [{0}]: message delivery could not be initiated but will " +
                        "be retried periodically. Subsequent unsuccessful attempts " +
                        "will not be logged. The error was: {3}", 
                        getName(), Integer.toString(attemptPlusOne), Integer.toString(dt), ex), ex);
                }
            }
        }
    }

    /**
     * Suspends message delivery
     */
    public void stop() {
        Delivery d = mDelivery;
        if (d != null && d.isThisCalledFromOnMessage()) {
            stopConnectorByMDB("<unspecified>");
        } else {
            internalStop();
        }
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
    @Override
    public void deactivate() {
        try {
            internalStop();
        } catch (RuntimeException e) {
            sLog.warn(LOCALE.x("E018: Unexpected exception in endpoint deactivation: {0}", e), e);
        }
        try {
            killMBean();
        } catch (RuntimeException e) {
            sLog.warn(LOCALE.x("E019: Unexpected exception in undeploying MBean " 
                + "during endpoint deactivation: {0}", e), e);
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
    public boolean isCMT() {
        return mIsCMT;
    }
    
    /**
     * @return true if XA is emulated, i.e. NoXA is set
     */
    public boolean isXAEmulated() {
        return mIsXAEmulated;
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
                sLog.warn(LOCALE.x(
                    "E020: [{0}]: exception on unregistering server MBean [{1}]: {2}",
                    getName(), mServerMgtMBeanName, e), e);
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
    @Override
    public RAJMSResourceAdapter getRA() {
        return mRA;
    }

    /**
     * Returns the MessageEndpointFactory
     *
     * @return MessageEndpointFactory
     */
    @Override
    public MessageEndpointFactory getMessageEndpointFactory() {
        return mEndpointFactory;
    }

    /**
     * Returns the activation spec used to create this activation
     *
     * @return RAJMSActivationSpec
     */
    @Override
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
    @Override
    public boolean is(MessageEndpointFactory epf, RAJMSActivationSpec spec) {
        return mEndpointFactory.equals(epf) && mSpec.equals(spec);
    }

    /**
     * toString
     *
     * @return String
     */
    @Override
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
        String ret = mRA.getClearTextPassword();
        if (!Str.empty(mSpec.getUserName())) {
            ret = mSpec.getClearTextPassword();
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
    @Override
    public String getName() {
        String consumertype;
        if (Queue.class.getName().equals(mSpec.getDestinationType())) {
            consumertype = "QueueReceiver";
        } else {
            if (RAJMSActivationSpec.DURABLE.equals(mSpec.getSubscriptionDurability())) {
                consumertype = RAJMSActivationSpec.DURABLE + " TopicSubscriber(" + mSpec.getSubscriptionName() + ")"; 
            } else {
                consumertype = RAJMSActivationSpec.NONDURABLE + " TopicSubscriber"; 
            }
        }
        
        String deliveryType = Integer.toString(mDeliveryMode);
        if (mDeliveryMode >= 0 && mDeliveryMode < RAJMSActivationSpec.DELIVERYCONCURRENCY_STRS.length) {
            deliveryType = RAJMSActivationSpec.DELIVERYCONCURRENCY_STRS[mDeliveryMode];
        }
        
        // Compute selector string in form of "(selector)"
        String selector = "";
        try {
            selector = getObjectFactory().getMessageSelector(getRA(), mSpec);
        } catch (JMSException ignore) {
            // ignore
        }
        if (!Str.empty(selector)) {
            selector = "(" + selector + ")";
        } else {
            selector = "";
        }
        
        String contextname = "";
        if (!Str.empty(mSpec.getContextName())) {
            contextname = "{" + mSpec.getContextName() + "}";
        }
        
        String interceptorStr = "";
        InterceptorChainBuilder interceptors = null;
        synchronized (mLock) {
            if (mDelivery != null) {
                interceptors = mDelivery.getInterceptors();
            }
        }
        if (interceptors != null && interceptors.hasInterceptors()) {
            interceptorStr = " <<" + interceptors + ">>";
        }
        
        return deliveryType + "-" + consumertype + "(" + mSpec.getDestination() + ")" 
        + selector + contextname + " @ [" + mURL + "]" + interceptorStr; 
    }
    
    /**
     * @return true if redelivery handling is to redirect rather than forward
     */
    public boolean shouldRedirectRatherThanForward() {
        return mRedeliveryRedirect;
    }
    
    /**
     * @return true if the message should always be wrapped for stateful redelivery
     */
    public boolean shouldWrapAlways() {
        return mWrapAlways;
    }

    /**
     * @return true if the XAResource needs to be wrapped to override isSameRM()
     */
    public boolean isOverrideIsSameRM() {
        return mOverrideIsSameRM;
    }

    /**
     * Called by the delivery to let the activation know which destination object
     * is actually used for reading messages. This destination can then be shared
     * with the inbound message wrapper.
     * 
     * @param dest
     */
    public void publishInboundDestination(Destination dest) {
        mPublishedDestination = dest;
    }
    
    /**
     * @return the destination as it was published by the Delivery. This may be null
     * during startup, and the object identity may change during the lifecycle of the 
     * activation. Its purpose is to make the destination object available to the 
     * message wrapper.
     */
    public Destination getPublishedDestination() {
        return mPublishedDestination;
    }
}
