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
import com.stc.jmsjca.util.Logger;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.QueueSession;
import javax.jms.TopicSession;
import javax.transaction.xa.XAResource;

/**
 * A strategy for serial delivery
 *
 * @author fkieviet
 * @version $Revision: 1.16 $
 */
public class SerialDelivery extends Delivery implements MessageListener,
    javax.jms.ExceptionListener {
    private static Logger sLog = Logger.getLogger(SerialDelivery.class);
    private javax.jms.Connection mConnection;
    private javax.jms.Session mSession;
    private XMessageEndpoint mEndpoint;
    private XAResource mXA;
    private RAJMSObjectFactory mObjFactory;
    private ConnectionForMove mMessageMoveConnection;
    private Delivery.MDB mMDB;
    private DeliveryResults mResult = new DeliveryResults();

    private static final Localizer LOCALE = Localizer.get();

    /**
     * Constructor
     *
     * @param a Activation
     * @param stats DeliveryStats
     * @throws Exception on failure 
     */
    public SerialDelivery(Activation a, DeliveryStats stats) throws Exception {
        super(a, stats);
    }

    /**
     * Starts delivery
     *
     * <P>__Called from initialization thread__
     *
     * @throws Exception Any error, e.g. connection errors to the JMS.
     */
    @Override
    public void start() throws Exception {
        mObjFactory = mActivation.getObjectFactory();
        final int domain = XConnectionRequestInfo.guessDomain(
            mActivation.isCMT() && !mActivation.isXAEmulated(), mActivation.isTopic());
        javax.jms.ConnectionFactory fact = mObjFactory.createConnectionFactory(
            domain,
            mActivation.getRA(),
            mActivation.getActivationSpec(),
            null,
            null);
        mConnection = mObjFactory.createConnection(
            fact,
            domain,
            mActivation.getActivationSpec(),
            mActivation.getRA(),
            mActivation.getUserName() == null ? mActivation.getRA().getUserName() : mActivation.getUserName(),
            mActivation.getPassword() == null ? mActivation.getRA().getClearTextPassword() : mActivation.getPassword());
        mObjFactory.setClientID(mConnection, 
            mActivation.isTopic(), 
            mActivation.getActivationSpec(), 
            mActivation.getRA());
        mSession = mObjFactory.createSession(
            mConnection,
            mActivation.isCMT() && !mActivation.isXAEmulated(),
            mActivation.isTopic() ? TopicSession.class : QueueSession.class,
            mActivation.getRA(),
            mActivation.getActivationSpec(),
            true,
            javax.jms.Session.SESSION_TRANSACTED);
        createDLQDest();
        javax.jms.Destination dest = mObjFactory.createDestination(
            mSession,
            mActivation.isCMT() && !mActivation.isXAEmulated(),
            mActivation.isTopic(),
            mActivation.getActivationSpec(),
            null,
            mActivation.getRA(),
            mActivation.getActivationSpec().getDestination(), 
            null,
            mActivation.isTopic() ? TopicSession.class : QueueSession.class);
        javax.jms.MessageConsumer cons = mObjFactory.createMessageConsumer(
            mSession,
            mActivation.isCMT() && !mActivation.isXAEmulated(),
            mActivation.isTopic(),
            dest,
            mActivation.getActivationSpec(),
            mActivation.getRA());
        cons.setMessageListener(
            mActivation.getObjectFactory().getMessagePreprocessor(
                this, mActivation.isCMT() && !mActivation.isXAEmulated()));

        if (mActivation.isCMT()) {
            if (mActivation.isXAEmulated()) {
                mXA = new PseudoXAResource(mSession);
            } else {
                mXA = mActivation.getObjectFactory().getXAResource(true, mSession);
            }
        }
        
        mMDB = new Delivery.MDB(mXA);

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
    @Override
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
            if (mSession != null) {
                mSession.close();
            }
        } catch (Exception ex) {
            sLog.warn(LOCALE.x("E087: Error while closing session: {0}", ex), ex);
        }
        
        try {
            if (mConnection != null) {
                mConnection.close();
            }
        } catch (Exception ex) {
            sLog.warn(LOCALE.x("E055: Unexpected exception closing JMS connection: {0}", ex), ex);
        }
        
        if (mMessageMoveConnection != null) {
            mMessageMoveConnection.destroy();
        }

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
     * onMessage() {
     *    beforeDelivery()
     *    deliver()
     *    afterDelivery()
     * }
     * 
     * before()
     *     fails: rethrow
     * deliver()
     *     fails: setRBO
     *         fails: rethrow
     * after()
     *     fails: rethrow
     * 
     *
     * @param m Message
     */
    public void onMessage(Message m) {
        try {
            mActivation.enterContext();
            if (sLog.isDebugEnabled()) {
                sLog.debug("Delivering message to endpoint");
            }

            // Lazy constuction of endpoint
            if (mEndpoint == null) {
                try {
                    mEndpoint = createMessageEndpoint(mXA, mSession);
                    if (mEndpoint == null) {
                        throw Exc.exc(LOCALE.x("E143: No endpoint was created, "
                            + "possibly because the RA may be shutting down"));
                    }
                } catch (Exception ex1) {
                    LocalizedString msg = LOCALE.x("E056: Failure to create an endpoint to deliver message to; "
                        + "delivery attempt aborted. Exception: {0}", ex1);
                    sLog.warn(msg, ex1);
                    throw Exc.rtexc(msg, ex1);
                }
            }

            mResult.reset();
            try {
                beforeDelivery(mResult, mEndpoint, true);
                deliverToEndpoint(mResult, mMessageMoveConnection, mEndpoint, m, true);
                afterDelivery(mResult, mMessageMoveConnection, mEndpoint, mMDB, true);
                afterDeliveryNoXA(mResult, mSession, mMessageMoveConnection, mEndpoint);
            } catch (Exception e) {
                mActivation.distress(e);
                throw Exc.rtexc(LOCALE.x("E196: An unexpected exception happened in "
                    + "the receiving or processing of a message. The exception will be propagated "
                    + "to the JMS Client Runtime to ensure that the message delivery will be rolled back. "
                    + "The exception was: {0}", e), e); 
            } finally {
                // Discard endpoint on any error
                if (mResult.getShouldDiscardEndpoint()) {
                    release(mEndpoint);
                    mEndpoint = null;
                }
            }
        } finally {
            mActivation.exitContext();
        }
    }

    /**
     * @see com.stc.jmsjca.core.Delivery#getConfiguredEndpoints()
     */
    @Override
    public int getConfiguredEndpoints() {
        return 1;
    }
}
