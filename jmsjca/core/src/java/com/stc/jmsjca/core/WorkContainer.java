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
 * $RCSfile: WorkContainer.java,v $
 * $Revision: 1.3 $
 * $Date: 2007-01-21 17:51:48 $
 *
 * Copyright 2003-2007 Sun Microsystems, Inc. All Rights Reserved.  
 */

package com.stc.jmsjca.core;

import com.stc.jmsjca.core.Delivery.ConnectionForMove;
import com.stc.jmsjca.util.Logger;

import javax.jms.Connection;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.Session;
import javax.resource.ResourceException;
import javax.resource.spi.endpoint.MessageEndpoint;
import javax.transaction.xa.XAResource;

import java.lang.reflect.Method;

/**
 * A Work item that can be executed by the WorkManager; it contains enough information to
 * execute the work, i.e the message that needs to be delivered, and the messageEndpoint
 * to which the message needs to be delivered. Hence, this assumes that the
 * MessageEndpoint has already been created and assigned to this WorkContainer for use.
 * After work is done, it will call back into the originating Delivery to notify
 *
 * @author fkieviet
 * @version $Revision: 1.3 $
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
    private boolean mIsRunning;
    private ConnectionForMove mMessageMoveConnection;
    private boolean mEnlistInRun;
    private XAResource mXA;
    private Delivery.MDB mMDB;
    private boolean mHasBadEndpoint; 

    /**
     * Constructor
     *
     * @param delivery Delivery
     * @param endpoint MessageEndpoint
     * @param method Method
     * @param session Session
     * @param conn connection used to create
     * @param enlistInRun set to true to enlist the resource in the run() method rather 
     *   than the onMessage method (required for JMQ)
     * @param xa XAResource
     */
    public WorkContainer(CCDelivery delivery, MessageEndpoint endpoint, Method method,
        javax.jms.Session session, Connection conn, boolean enlistInRun, XAResource xa) {
        mDelivery = delivery;
        mEndpoint = endpoint;
        mSession = session;
        mMessageMoveConnection = this.mDelivery.createConnectionForMove();        
        mEnlistInRun = enlistInRun;
        mXA = xa;
        mMDB = mDelivery.new MDB(mXA);
    }

    /**
     * Tries to destroy the work container; this will not succeed if the work container is
     * currently running.
     *
     * @return boolean
     */
    public boolean destroy() {
        boolean isRunning;
        synchronized (mStateLock) {
            isRunning = mIsRunning;
        }

        if (isRunning) {
            return false;
        } else {
            doDestroy();
            return true;
        }
    }
    
    /**
     * Destroys unconditionally
     */
    public void doDestroy() {
        if (sLog.isDebugEnabled()) {
            sLog.debug("Releasing endpoint");
        }
        
        mMessageMoveConnection.destroy();

        mDelivery.release(mEndpoint);
        mEndpoint = null;
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
        mHasBadEndpoint = false;
    }
    
    boolean hasBadEndpoint() {
        return mHasBadEndpoint;
    }
    
    /**
     * @return XAResource (may be null)
     */
    XAResource getXAResource() {
        return mXA;
    }
    
    private void beforeRun() {
        if (mEnlistInRun) {
            try {
                if (mDelivery.isXA()) {
                    mEndpoint.beforeDelivery(mDelivery.mMethod);
                }
            } catch (Exception e) {
                throw new Delivery.BeforeDeliveryException(e);
            }
        }
    }

    private void afterRun() {
        if (mEnlistInRun) {
            try {
                if (mDelivery.isXA()) {
                    mEndpoint.afterDelivery();
                }
            } catch (ResourceException e) {
                throw new Delivery.AfterDeliveryException(e);
            }
        }
    }
    
    /**
     * @see java.lang.Runnable#run()
     */
    public void run() {
        String contextName = mDelivery.getActivation().getActivationSpec().getContextName();
        if (contextName != null) {
            sContextEnter.info(contextName);
        }
        try {
            synchronized (mStateLock) {
                mIsRunning = true;
            }

            if (sLog.isDebugEnabled()) {
                sLog.debug("Running WorkContainer");
            }


            if (mEndpoint == null) {
                if (sLog.isDebugEnabled()) {
                    sLog.debug(
                        "Endpoint is null; RA probably shutting down. Message is skipped");
                }
            } else {
                beforeRun();
                mSession.run();
                afterRun();
            }
        } catch (Error e) {
            sLog.warn("Unexpected error encountered while executing a JMS CC-session: " + e, e);
        } catch (RuntimeException e) {
            sLog.warn("Unexpected exception encountered while executing a JMS CC-session: " + e, e);
        } finally {
            mDelivery.workDone(this);

            synchronized (mStateLock) {
                mIsRunning = false;
            }

            if (contextName != null) {
                sContextExit.info(contextName);
            }
        }
    }

    /**
     * @see javax.resource.spi.work.Work#release()
     */
    public void release() {
        if (sLog.isDebugEnabled()) {
            sLog.debug("WorkContainer.release(): attempting to destroy WorkContainer, "
                + "scalled by Application Server");
        }

        destroy();
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
        if (sLog.isDebugEnabled()) {
            sLog.debug("WorkContainer.onMessage() -- start");
        }

        RuntimeException e = mDelivery.deliver(mMessageMoveConnection, mEndpoint, message, mEnlistInRun, mMDB);

        if (sLog.isDebugEnabled()) {
            sLog.debug("WorkContainer.onMessage() -- end");
        }
        
        if (e != null) {
            mHasBadEndpoint = true;
        }

        if (mDelivery.isXA()) {
            if (e != null && e instanceof Delivery.BeforeDeliveryException) {
                throw e;
            }
        } else {
            if (e == null) {
                try {
                    mSession.commit();
                } catch (JMSException ex) {
                    sLog.error("The message could not be committed: " + ex, ex);
                }
            } else {
                try {
                    mSession.rollback();
                } catch (JMSException ex) {
                    sLog.error("The message could not be rolled back: " + ex, ex);
                }
            }
        }
    }
}
