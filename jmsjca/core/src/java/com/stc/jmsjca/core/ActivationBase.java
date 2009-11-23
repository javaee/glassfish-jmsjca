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

import com.stc.jmsjca.util.Logger;

import javax.resource.spi.endpoint.MessageEndpointFactory;

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
 * @version $Revision: 1.6 $
 */
public abstract class ActivationBase {
    /**
     * Log category for logging enter contexts
     */
    private static Logger sContextEnter = Logger.getLogger("com.stc.EnterContext");
    
    /**
     * Log category for logging exit contexts
     */
    private static Logger sContextExit = Logger.getLogger("com.stc.ExitContext");

    private RAJMSResourceAdapter mRA;
    private MessageEndpointFactory mEndpointFactory;
    private RAJMSActivationSpec mSpec;

    /**
     * All states in string format (for diagnostics)
     */
    public static final String[] STATES = new String[] {"Disconnected", "Connecting", "Connected", "Disconnecting" };

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
    
    private RAJMSObjectFactory mObjFactory;

    /**
     * Activation constructor
     *
     * @param ra RAJMSResourceAdapter
     * @param epf MessageEndpointFactory
     * @param spec RAJMSActivationSpec
     */
    public ActivationBase(RAJMSResourceAdapter ra, MessageEndpointFactory epf,
        RAJMSActivationSpec spec) {
        mRA = ra;
        mEndpointFactory = epf;
        mSpec = spec;
        String url = spec.getConnectionURL();
        if (url == null || url.length() == 0) {
            url = ra.getConnectionURL();
        }
        mObjFactory = ra.createObjectFactory(ra, spec, null);
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
    public abstract void activate() throws Exception;

    
    /**
     * Halts message delivery
     * Should NOT throw an exception
     */
    public abstract void deactivate();
    
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
    @Override
    public String toString() {
        return getName();
    }

    /**
     * @return a human friendly name
     */
    public abstract String getName();
    
    /**
     * Enters logging context
     */
    public void enterContext() {
        if (mSpec != null && mSpec.getContextName() != null && mSpec.getContextName().length() > 0) {
            sContextEnter.debug(mSpec.getContextName());
        }
    }
    
    /**
     * Exits logging context
     */
    public void exitContext() {
        if (mSpec != null && mSpec.getContextName() != null && mSpec.getContextName().length() > 0) {
            sContextExit.debug(mSpec.getContextName());
        }
    }
}
