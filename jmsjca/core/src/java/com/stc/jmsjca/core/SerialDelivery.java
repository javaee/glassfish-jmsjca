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
import com.stc.jmsjca.util.Logger;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.QueueSession;
import javax.jms.TopicSession;
import javax.resource.spi.endpoint.MessageEndpoint;
import javax.transaction.xa.XAResource;

/**
 * A strategy for serial delivery
 *
 * @author fkieviet
 * @version $Revision: 1.7 $
 */
public class SerialDelivery extends Delivery implements MessageListener,
    javax.jms.ExceptionListener {
    private static Logger sLog = Logger.getLogger(SerialDelivery.class);
    private static Logger sContextEnter = Logger.getLogger("com.stc.EnterContext");
    private static Logger sContextExit = Logger.getLogger("com.stc.ExitContext");
    private javax.jms.Connection mConnection;
    private javax.jms.Session mSession;
    private MessageEndpoint mEndpoint;
    private LocalizedString mContextName;
    private XAResource mXA;
    private RAJMSObjectFactory mObjFactory;
    private ConnectionForMove mMessageMoveConnection;
    private Delivery.MDB mMDB;
    private Delivery.DeliveryResults mResult = new DeliveryResults();

    private static final Localizer LOCALE = Localizer.get();

    /**
     * Constructor
     *
     * @param a Activation
     * @param stats DeliveryStats
     */
    public SerialDelivery(Activation a, DeliveryStats stats) {
        super(a, stats);
    }

    /**
     * Starts delivery
     *
     * <P>__Called from initialization thread__
     *
     * @throws Exception Any error, e.g. connection errors to the JMS.
     */
    public void start() throws Exception {
        mObjFactory = mActivation.getObjectFactory();
        javax.jms.ConnectionFactory fact = mObjFactory.createConnectionFactory(
            XConnectionRequestInfo.guessDomain(mActivation.isXA(), mActivation.isTopic()),
            mActivation.getRA(),
            mActivation.getActivationSpec(),
            null,
            null);
        mConnection = mObjFactory.createConnection(
            fact,
            XConnectionRequestInfo.guessDomain(mActivation.isXA(), mActivation.isTopic()),
            mActivation.getActivationSpec(),
            mActivation.getRA(),
            mActivation.getUserName() == null ? mActivation.getRA().getUserName() : mActivation.getUserName(),
            mActivation.getPassword() == null ? mActivation.getRA().getPassword() : mActivation.getPassword());
        mObjFactory.setClientID(mConnection, 
            mActivation.isTopic(), 
            mActivation.getActivationSpec(), 
            mActivation.getRA());
        mSession = mObjFactory.createSession(
            mConnection,
            mActivation.isXA(),
            mActivation.isTopic() ? TopicSession.class : QueueSession.class,
            mActivation.getRA(),
            mActivation.getActivationSpec(),
            true,
            javax.jms.Session.SESSION_TRANSACTED);
        javax.jms.Destination dest = mObjFactory.createDestination(
            mSession,
            mActivation.isXA(),
            mActivation.isTopic(),
            mActivation.getActivationSpec(),
            null,
            mActivation.getRA(),
            mActivation.getActivationSpec().getDestination());
        javax.jms.MessageConsumer cons = mObjFactory.createMessageConsumer(
            mSession,
            mActivation.isXA(),
            mActivation.isTopic(),
            dest,
            mActivation.getActivationSpec(),
            mActivation.getRA());
        cons.setMessageListener(
            mActivation.getObjectFactory().getMessagePreprocessor(
                this, mActivation.isXA()));

        mXA = mActivation.getObjectFactory().getXAResource(
            mActivation.isXA(), mSession);
        
        mMDB = new Delivery.MDB(mXA);

        mContextName = LocalizedString.valueOf(getActivation().getActivationSpec().getContextName());
        
        mConnection.setExceptionListener(this);
        
        mMessageMoveConnection = createConnectionForMove();

        mConnection.start();
    }

    /**
     * Releases any resources associated with delivery. This will try to destroy all
     * WorkContainers. Destroying a WorkContainer may fail if it is in the middle of
     * processing a message. In that case, this method will wait a bit and try again.
     *
     * <P>__Is called from service thread__
     */
    public void deactivate() {
        if (sLog.isDebugEnabled()) {
            sLog.debug("SerialDelivery.deactivate() -- begin");
        }

        try {
            if (mConnection != null) {
                mConnection.stop();
            }
        } catch (Exception ex) {
            sLog.warn(LOCALE.x("E054: Unexpected exception stopping JMS connection: {0}.", ex), ex);
        }

        // JMS thread now guaranteed to have exited
        if (mEndpoint != null) {
            release(mEndpoint);
            mEndpoint = null;
        }

        try {
            if (mConnection != null) {
                mConnection.close();
            }
        } catch (Exception ex) {
            sLog.warn(LOCALE.x("E055: Unexpected exception closing JMS connection: {0}", ex), ex);
        }
        
        mMessageMoveConnection.destroy();

        if (sLog.isDebugEnabled()) {
            sLog.debug("SerialDelivery.deactivate() -- complete");
        }
    }
    
    /**
     * onException
     * 
     * @param ex JMSException
     */
    public void onException(JMSException ex) {
        mActivation.distress(ex);
    }

    /**
     * workDone
     *
     * @param w WorkContainer
     */
    public void workDone(WorkContainer w) {
    }

    /**
     * onMessage
     *
     * @param m Message
     */
    public void onMessage(Message m) {
        try {
            if (mContextName != null) {
                sContextEnter.info(mContextName);
            }
            if (sLog.isDebugEnabled()) {
                sLog.debug("Delivering message to endpoint");
            }

            // Lazy constuction of endpoint
            if (mEndpoint == null) {
                try {
                    mEndpoint = createMessageEndpoint(mXA, mSession);
                    if (mEndpoint == null) {
                        throw new Exception("No endpoint created; RA shutting down?");
                    }
                } catch (Exception ex1) {
                    LocalizedString msg = LOCALE.x("E056: Failure to create an endpoint to deliver message to; "
                        + "delivery attempt aborted. Exception: {0}", ex1);
                    sLog.warn(msg, ex1);
                    throw new RuntimeException(msg.toString(), ex1);
                }
            }

            mResult.reset();
            beforeDelivery(mResult, mEndpoint);
            deliverToEndpoint(mResult, mMessageMoveConnection, mEndpoint, m);
            afterDelivery(mResult, mMessageMoveConnection, mEndpoint, mMDB);
            afterDeliveryNoXA(mResult, mSession, mMessageMoveConnection, mEndpoint, mMDB);
            
            // Disard of endpoint if exception was thrown
            if (mResult.getShouldDiscardEndpoint()) {
                release(mEndpoint);
                mEndpoint = null;
            }
        } finally {
            if (mContextName != null) {
                sContextExit.info(mContextName);
            }
        }
    }

    /**
     * @see com.stc.jmsjca.core.Delivery#getConfiguredEndpoints()
     */
    public int getConfiguredEndpoints() {
        return 1;
    }
}
