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

import com.stc.jmsjca.core.Delivery.ConnectionForMove;
import com.stc.jmsjca.core.Delivery.DeliveryResults;
import com.stc.jmsjca.localization.LocalizedString;
import com.stc.jmsjca.localization.Localizer;
import com.stc.jmsjca.util.Logger;
import com.stc.jmsjca.util.Semaphore;

import javax.jms.Connection;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.Session;
import javax.resource.spi.endpoint.MessageEndpoint;
import javax.transaction.Transaction;
import javax.transaction.xa.XAResource;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
 * A Work item that can be executed by the WorkManager; it contains enough information to
 * execute the work, i.e the message that needs to be delivered, and the messageEndpoint
 * to which the message needs to be delivered. Hence, this assumes that the
 * MessageEndpoint has already been created and assigned to this WorkContainer for use.
 * After work is done, it will call back into the originating Delivery to notify
 *
 * @author fkieviet
 * @version $Revision: 1.5 $
 */
public class WorkContainer implements javax.resource.spi.work.Work,
    javax.jms.ServerSession, javax.jms.MessageListener {

    private static Logger sLog = Logger.getLogger(WorkContainer.class);
    private static Logger sContextEnter = Logger.getLogger("com.stc.EnterContext");
    private static Logger sContextExit = Logger.getLogger("com.stc.ExitContext");
    private javax.jms.Session mSession;
    private MessageEndpoint mEndpoint;
    private CCDelivery mDelivery;
    private Object mStateLock = new Object();
    private static final int STATE_IDLE = 0;
    private static final int STATE_RUNNING = 1;
    private static final int STATE_DESTROYED = 2;
    private static final int STATE_DESTROYED_SUB_ALREADY_DESTROYED = 3;
    private int mState;
    private ConnectionForMove mMessageMoveConnection;
    private Delivery.MDB mMDB;
    private LocalizedString mContextName;
    private List mMsgs;
    private DeliveryResults mResult = new DeliveryResults();

    private static final Localizer LOCALE = Localizer.get();

    /**
     * Constructor
     *
     * @param delivery Delivery
     * @param endpoint MessageEndpoint
     * @param method Method
     * @param session Session
     * @param conn connection used to create
     * @param mdb mdb
     */
    public WorkContainer(CCDelivery delivery, MessageEndpoint endpoint, Method method,
        javax.jms.Session session, Connection conn, Delivery.MDB mdb) {
        mDelivery = delivery;
        mEndpoint = endpoint;
        mSession = session;
        mMessageMoveConnection = this.mDelivery.createConnectionForMove();        
        mMDB = mdb;
        mContextName = LocalizedString.valueOf(
            mDelivery.getActivation().getActivationSpec().getContextName());        
    }

    /**
     * Tries to destroy the work container; this will not succeed if the work container is
     * currently running.
     *
     * @return true if destroyed or already destroyed
     */
    public boolean destroy() {
        int state = setState(STATE_DESTROYED);
        if (state == STATE_DESTROYED_SUB_ALREADY_DESTROYED) {
            return true;
        } else if (state != STATE_DESTROYED) {
            return false;
        } else {
            mMessageMoveConnection.destroy();
            mDelivery.release(mEndpoint);
            mEndpoint = null;
            return true;
        }
    }
    
    /**
     * @return MEP
     */
    MessageEndpoint getEndpoint() {
        return mEndpoint;
    }
    
    /**
     * @param mep MEP
     */
    void setEndpoint(MessageEndpoint mep) {
        mEndpoint = mep;
        mResult.setShouldDiscardEndpoint(false);
    }
    
    boolean hasBadEndpoint() {
        return mResult.getShouldDiscardEndpoint();
    }
    
    /**
     * @return XAResource (may be null)
     */
    XAResource getXAResource() {
        return mMDB.getXAResource();
    }

    private int setState(int newState) {
        synchronized (mStateLock) {
            switch (mState) {
            case STATE_DESTROYED:
                if (newState == STATE_RUNNING) {
                    // Don't do anything
                    return mState;
                } else if (newState == STATE_DESTROYED) {
                    return STATE_DESTROYED_SUB_ALREADY_DESTROYED;
                } else {
                    break;
                }
            case STATE_RUNNING:
                if (newState == STATE_DESTROYED) {
                    return mState;
                } else if (newState == STATE_IDLE) {
                    mState = STATE_IDLE;
                    return mState;
                } else {
                    break;
                }
            case STATE_IDLE:
                if (newState == STATE_DESTROYED) {
                    mState = STATE_DESTROYED;
                    return mState;
                } else if (newState == STATE_RUNNING) {
                    mState = STATE_RUNNING;
                    return mState;
                } else {
                    break;
                }
            }

            throw new RuntimeException("Invalid state transition: " + mState + " to " + newState + " on " + this);
        }
    }
    
    /**
     * @see java.lang.Runnable#run()
     */
    public void run() {
        if (mContextName != null) {
            sContextEnter.info(mContextName);
        }

        int state = STATE_IDLE;
        boolean deliveryComplete = true;

        try {
            if (sLog.isDebugEnabled()) {
                sLog.debug("Running WorkContainer");
            }
            
            state = setState(STATE_RUNNING);
            if (state == STATE_DESTROYED) {
                sLog.debug("Shutting down... skipped");
            } else {
                mDelivery.beforeDelivery(mResult, mEndpoint);
                mResult.reset();
                mMsgs = new ArrayList();
                mSession.run();
                deliveryComplete = deliver();
                mMsgs = null;
                if (deliveryComplete) {
                    mDelivery.afterDelivery(mResult, mMessageMoveConnection, mEndpoint, mMDB);
                    mDelivery.afterDeliveryNoXA(mResult, mSession, mMessageMoveConnection, mEndpoint, mMDB);
                }
            }
        } catch (Error e) {
            sLog.warn(LOCALE.x("E063: Unexpected error encountered while executing a JMS CC-session: {0}", e), e);
        } catch (RuntimeException e) {
            sLog.warn(LOCALE.x("E064: Unexpected exception encountered while executing a JMS CC-session: {0}", e), e);
        } finally {
sLog.infoNoloc("run(): deliverycomplete=" + deliveryComplete);            
            if (deliveryComplete) {
                setState(STATE_IDLE);
                mDelivery.workDone(this);
            }
            if (mContextName != null) {
                sContextExit.info(mContextName);
            }
        }
    }

    /**
     * @see javax.resource.spi.work.Work#release()
     */
    public void release() {
    }

    /**
     * @see javax.jms.ServerSession#getSession()
     */
    public Session getSession() {
        if (sLog.isDebugEnabled()) {
            sLog.debug("getSession(): " + mSession);
        }

        return mSession;
    }

    /**
     * @see javax.jms.ServerSession#start()
     */
    public void start() {
        if (sLog.isDebugEnabled()) {
            sLog.debug("WorkContainer.start(): scheduling work for application server");
        }

        mDelivery.scheduleWork(this);
    }

    /**
     * @see javax.jms.MessageListener#onMessage(javax.jms.Message)
     */
    public void onMessage(Message message) {
        mMsgs.add(message);
    }
    
    private boolean deliver() {
        if (sLog.isDebugEnabled()) {
            sLog.debug("WorkContainer.deliver() -- start");
        }
        boolean deliveryComplete = true;
        SC sc = new SC();
        
        // Deliver messages
        for (int i = 0, n = mMsgs.size(); i < n; i++) {
            Message message = (Message) mMsgs.get(i);
            message = wrapMsg(message, sc, mResult.getNOnMessageWasCalled(), mResult);
            mResult.resetDeliveryState();
            mDelivery.deliverToEndpoint(mResult, mMessageMoveConnection, mEndpoint, message);
            if (mResult.getOnMessageFailed()) {
                break;
            }
        }
        
        // Deliver end of batch message
        if (mDelivery.mBatchSize > 1 &&  mResult.getNOnMessageWasCalled() > 0) {
            // Msgs were delivered; signal end of batch
            Message m = new EndOfBatchMessage();
            m = wrapMsg(m, sc, mResult.getNOnMessageWasCalled(), mResult);
            mResult.resetDeliveryState();
            mDelivery.deliverToEndpoint(mResult, mMessageMoveConnection, mEndpoint, m);
        }
        
        // HUA
        if (mDelivery.mHoldUntilAck && mResult.getNOnMessageWasCalled() > 0) {
            sc.arm(mResult.getNOnMessageWasCalled());
//            deliveryComplete = false;
        }
        
        if (sLog.isDebugEnabled()) {
            sLog.debug("WorkContainer.deliver() -- end");
        }

        return deliveryComplete;
    }
    
    /**
     * @param msgToWrap msg to wrap
     * @param ack ack handler
     * @param iBatch identifies the msg with an index into the batch
     * @param result  
     * @return original or wrapped msg
     */
    private Message wrapMsg(Message msgToWrap, AckHandler ack, int iBatch, DeliveryResults result) {
        try {
            return mDelivery.wrapMsg(msgToWrap, ack, iBatch);
        } catch (Exception e) {
            result.setRollbackOnly(true);
            result.setException(e);
            mDelivery.mActivation.distress(e);
            return msgToWrap;
        }
    }
    
    private class SC extends AckHandler {
        private int mAcksExpected;
        private int mAcksReceived;
        private boolean mIsRollbackOnly;
        private Transaction mTx;
        private Semaphore mSemaphore = new Semaphore(0);

        public synchronized void ack(boolean isRollbackOnly, Message m) throws JMSException {
            if (isRollbackOnly) {
                mIsRollbackOnly = true;
            }

            mAcksReceived++;

            if (mAcksReceived == mAcksExpected) {
                mSemaphore.release();
            }
        }
        
        public void arm(int acksExpected) {
            if (mDelivery.isXA()) {
                mTx = mDelivery.getTransaction(true);
            }

            synchronized (this) {
                mAcksExpected = acksExpected;
                if (mAcksReceived == mAcksExpected) {
                    mSemaphore.release();
                }
            }
            
            try {
                mSemaphore.acquire();
            } catch (InterruptedException e) {
                sLog.error(LOCALE.x("E099: HUA was interrupted"));
                Thread.interrupted();
            }
            
            if (mIsRollbackOnly) {
                mResult.setRollbackOnly(true);
            }

            // If the transaction was moved to a different thread, take it back
            try {
                if (!mResult.getShouldNotCallAfterDelivery() && mDelivery.isXA()) {
                    if (mDelivery.mHoldUntilAck && mDelivery.getTransaction(true) == null) {
                        mDelivery.getTxMgr().getTransactionManager().resume(mTx);
                    }

                    if (mDelivery.mHoldUntilAck && mIsRollbackOnly) {
                        mDelivery.getTransaction(true).setRollbackOnly();
                    }
                }
            } catch (Exception e) {
                sLog.error(LOCALE.x("E100: Could not restore transaction: {0}", e), e);
            }
        }
    }
}
