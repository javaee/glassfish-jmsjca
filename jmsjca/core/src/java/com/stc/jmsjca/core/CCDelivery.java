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
 * $RCSfile: CCDelivery.java,v $
 * $Revision: 1.3 $
 * $Date: 2007-01-21 17:51:38 $
 *
 * Copyright 2003-2007 Sun Microsystems, Inc. All Rights Reserved.  
 */

package com.stc.jmsjca.core;

import com.stc.jmsjca.util.Logger;
import com.stc.jmsjca.util.Semaphore;

import javax.jms.ConnectionConsumer;
import javax.jms.JMSException;
import javax.jms.QueueSession;
import javax.jms.ServerSession;
import javax.jms.Session;
import javax.jms.TopicSession;
import javax.resource.spi.endpoint.MessageEndpoint;
import javax.resource.spi.work.WorkException;
import javax.resource.spi.work.WorkManager;
import javax.transaction.xa.XAResource;

import java.util.ArrayList;
import java.util.Iterator;

/**
 * <P>A strategy for concurrent delivery using connection consumer. The threading model is
 * as follows: there is a Service-thread, JMS-thread, Work-threads.
 *
 * <P> The Service-thread creates the Activation and its delivery and will invoke a
 * shutdown (see later).
 *
 * <P> The JMS-thread is created as a result of the activation by the JMS provider; it
 * will call getServerSession() and WorkContainer.start(); the latter will result in the
 * work being scheduled in the application server.
 *
 * <P> The Work-thread is directed by the application server and calls run() which
 * eventually will call javax.jms.MessageListener.onMessage().
 *
 * <P> Shutdown: this is invoked from the Service-thread; it will deactivate all
 * WorkContainers. Then it will close the JMS connection; thereafter it is guaranteed that
 * there is no JMS-thread or Work-thread anymore.
 *
 * @author fkieviet
 * @version $Revision: 1.3 $
 */
public class CCDelivery extends Delivery implements javax.jms.ServerSessionPool,
    javax.jms.ExceptionListener {
    private static Logger sLog = Logger.getLogger(CCDelivery.class);

    private WorkManager mWorkManager;
    private ArrayList mEmptyWorkContainers = new ArrayList();
    private ArrayList mAllWorkContainers = new ArrayList();
    private Object mStateLock = new Object();
    private int mNMaxWorkContainers;
    private Semaphore mEmptyWorkContainerSemaphore = new Semaphore(0);

    private javax.jms.Connection mConnection;
    private ConnectionConsumer mCC;

    private boolean mEnlistInRun;
    private int mNServerSessionsGivenOut;
    private Object mCountLock = new Object();

    /**
     * Constructor
     *
     * @param a Activation
     * @param stats DeliveryStats
     */
    public CCDelivery(Activation a, DeliveryStats stats) {
        super(a, stats);
        mNMaxWorkContainers = 
            a.getActivationSpec().getEndpointPoolMaxSize().intValue();
        mWorkManager = a.getRA().getBootstrapCtx().getWorkManager();
        mEnlistInRun = !a.getObjectFactory().canCCEnlistInOnMessage();
    }

    /**
     * Starts delivery
     *
     * <P>__Called from initialization thread__
     *
     * @throws Exception Any error, e.g. connection errors to the JMS.
     */
    public void start() throws Exception {
        RAJMSObjectFactory o = mActivation.getObjectFactory();
        javax.jms.ConnectionFactory fact = o.createConnectionFactory(
            XConnectionRequestInfo.guessDomain(mActivation.isXA(), mActivation.isTopic()),
            mActivation.getRA(),
            mActivation.getActivationSpec(),
            null,
            null);
        mConnection = o.createConnection(
            fact,
            XConnectionRequestInfo.guessDomain(mActivation.isXA(), mActivation.isTopic()),
            mActivation.getActivationSpec(),
            mActivation.getRA(),
            mActivation.getUserName() == null ? mActivation.getRA().getUserName() : mActivation.getUserName(),
            mActivation.getPassword() == null ? mActivation.getRA().getPassword() : mActivation.getPassword());
        o.setClientID(mConnection, 
            mActivation.isTopic(), 
            mActivation.getActivationSpec(), 
            mActivation.getRA());
        javax.jms.Session sess = o.createSession(
            mConnection,
            mActivation.isXA(),
            mActivation.isTopic() ? TopicSession.class : QueueSession.class,
            mActivation.getRA(),
            mActivation.getActivationSpec(),
            false,
            javax.jms.Session.AUTO_ACKNOWLEDGE);
        javax.jms.Destination dest = o.createDestination(
            sess,
            mActivation.isXA(),
            mActivation.isTopic(),
            mActivation.getActivationSpec(),
            mActivation.getRA(),
            mActivation.getActivationSpec().getDestination());
        sess.close();
        mCC = o.createConnectionConsumer(
            mConnection,
            mActivation.isXA(),
            mActivation.isTopic(),
            mActivation.isDurable(),
            mActivation.getActivationSpec(),
            mActivation.getRA(),
            dest,
            mActivation.getActivationSpec().getSubscriptionName(),
            mActivation.getActivationSpec().getMessageSelector(),
            this);

        mConnection.setExceptionListener(this);
        mConnection.start();
    }

    /**
     * Returns a new or recycled WorkContainer; the WorkContainer contains a JMS session
     * that will be filled by the JMS provider with a JMS message so that it can be
     * processed in a different thread later.
     *
     * @return ServerSession
     * @throws JMSException propagated, closed
     */
    public ServerSession getServerSession() throws JMSException {
        WorkContainer ret = null;
        try {
            ret = getEmptyWorkContainer();
        } catch (Exception ex) {
            String msg = "Unexpected failure to obtain an empty work container to "
                + "process JMS messages. The exception was: " + ex;
            JMSException jex = new JMSException(msg);
            jex.initCause(ex);
            onException(jex);
            throw new RuntimeException(msg, ex);
        }

        // For JMQ-like shutdown, we should not return a serversession if the 
        // connection consumer was closed; also need to keep track of the number
        // of server sessions given out
        if (mEnlistInRun) {
            boolean isclosed;
            synchronized (mCountLock) {
                isclosed = mCC == null;
                if (!isclosed) {
                    mNServerSessionsGivenOut++;
                }
            }
            if (isclosed) {
                addEmptyWorkContainer(ret);
                throw new JMSException("The connection consumer was closed");
            }
        }
        
        return ret;
    }

    /**
     * Schedules work for execution by the application server; called by a work
     * container when the JMS provider calls start() on the ServerSession.
     *
     * @param work WorkContainer
     */
    public void scheduleWork(WorkContainer work) {
        try {
            mWorkManager.scheduleWork(work);
        } catch (WorkException ex) {
            String msg = "Unexpected failure scheduling work to "
                + "process JMS messages. The exception was: " + ex;
            JMSException jex = new JMSException(msg);
            jex.initCause(ex);
            onException(jex);
        }
    }

    /**
     * Adds a workcontainer to the idle pool.
     * __Is called from RA thread__
     *
     * @param w WorkContainer
     * @param isNew boolean
     */
    private void addEmptyWorkContainer(WorkContainer w) {
        synchronized (mStateLock) {
            mEmptyWorkContainers.add(w);
            mEmptyWorkContainerSemaphore.release(1);
        }
    }

    private static void safeClose(Session s) {
        if (s != null) {
            try {
                s.close();
            } catch (JMSException ignore) {
                // ignore
            }
        }
    }

    /**
     * Grows the pool if the pool is empty and if the maximum number of entries has not
     * been exceeded.
     *
     * @throws Exception In case the endpoint cannot be created
     */
    private void growPoolIfNecessary() throws Exception {
        // Should grow the pool?
        int nTotalWorkContainers = mAllWorkContainers.size();
        boolean shouldGrow = (nTotalWorkContainers < mNMaxWorkContainers)
            && (mEmptyWorkContainerSemaphore.peek() == 0);

        // Grow pool if necessary
        if (shouldGrow) {
            Session s = null;
            try {
                if (sLog.isDebugEnabled()) {
                    sLog.debug("Growing pool; current size=" + nTotalWorkContainers
                        + "; max="
                        + mNMaxWorkContainers);
                }

                s = mActivation.getObjectFactory().createSession(
                    mConnection,
                    mActivation.isXA(),
                    mActivation.isTopic() ? TopicSession.class : QueueSession.class,
                    mActivation.getRA(),
                    mActivation.getActivationSpec(),
                    true, // In case of BMT, rollback should happen by message listener
                    Session.AUTO_ACKNOWLEDGE);
                
                XAResource xa = mActivation.getObjectFactory().getXAResource(
                    mActivation.isXA(), s);

                MessageEndpoint m = createMessageEndpoint(xa);

                if (m == null) {
                    // Stopping
                    safeClose(s);
                } else {
                    WorkContainer w = new WorkContainer(this, m,
                        mActivation.getOnMessageMethod(), s, mConnection, mEnlistInRun, xa);
                    s.setMessageListener(mActivation.getObjectFactory().
                        getMessagePreprocessor(w, mActivation.isXA()));
                    addEmptyWorkContainer(w);
                    mAllWorkContainers.add(w);
                }
            } catch (Exception ex) {
                safeClose(s);
                throw ex;
            }
        }
    }

    /**
     * Gets a workcontainer from the pool; will block until one is available OR until the
     * RA is shutting down, in which case an exception is thrown. Obtained Workcontainers
     * should be returned to the pool by the caller.
     *
     * <P>__Is called from JMS thread__
     *
     * @return not null
     * @throws Exception in case of shutdown, or in case of failure.
     */
    private WorkContainer getEmptyWorkContainer() throws Exception {
        WorkContainer ret = null;

        growPoolIfNecessary();

        // Get WorkContainer from pool while checking for shutdown
        for (;;) {
            if (!mEmptyWorkContainerSemaphore.attempt(1000)) {
                // No dice; check for shutdown
                if (mActivation.isStopping()) {
                    if (sLog.isDebugEnabled()) {
                        sLog.debug("getEmptyWorkContainer(): Stopping waiting for "
                            + "WorkContainer; throwing exception");
                    }

                    throw new Exception("Connector is shutting down");
                }
                if (sLog.isDebugEnabled()) {
                    sLog.debug("getEmptyWorkContainer(): still waiting");
                }
            } else {
                synchronized (mStateLock) {
                    ret = (WorkContainer) mEmptyWorkContainers.remove(mEmptyWorkContainers.size() - 1);
                }
                
                // Check if endpoint needs to be refreshed
                if (ret.hasBadEndpoint()) {
                    release(ret.getEndpoint());
                    ret.setEndpoint(createMessageEndpoint(ret.getXAResource()));
                }

                if (sLog.isDebugEnabled()) {
                    sLog.debug("getEmptyWorkContainer(): succeeded: " + ret);
                }

                break;
            }
        }

        return ret;
    }

    /**
     * Called by the WorkContainer when it has finished work
     *
     * @param w WorkContainer
     */
    public void workDone(WorkContainer w) {
        synchronized (mCountLock) {
            mNServerSessionsGivenOut--;
        }
        addEmptyWorkContainer(w);
    }
    
    private void waitUntilAllWorkContainersAreDestroyed() {
        // Wait until all work containers are destroyed
        long tlog = System.currentTimeMillis() + DESTROY_LOG_INTERVAL_MS;
        for (;;) {
            // Try to destroy all WorkContainers; count the number of failures
            if (sLog.isDebugEnabled()) {
                sLog.debug("Trying to destroy all WorkContainer-s");
            }
            int nNotDestroyed = 0;
            for (Iterator it = mAllWorkContainers.iterator(); it.hasNext();/*-*/) {
                WorkContainer w = (WorkContainer) it.next();
                if (!w.destroy()) {
                    nNotDestroyed++;
                }
            }

            // Wait if not all were destroyed
            if (nNotDestroyed == 0) {
                if (sLog.isDebugEnabled()) {
                    sLog.debug("All work containers were destroyed successfully");
                }
                break;
            } else {
                if (System.currentTimeMillis() > tlog) {
                    sLog.info("Deactivating connector; waiting for work containers to "
                        + "exit; there are " + nNotDestroyed
                        + " containers that are still active; activation=" + mActivation);
                    tlog = System.currentTimeMillis() + DESTROY_LOG_INTERVAL_MS;
                }

                // Wait a bit
                if (sLog.isDebugEnabled()) {
                    sLog.debug(nNotDestroyed
                        + " WorkContainer(s) were (was) not destroyed... waiting");
                }
                try {
                    Thread.sleep(DESTROY_RETRY_INTERVAL_MS);
                } catch (Exception ex) {
                    // ignore
                }
            }
        }
    }

    private void waitUntilAllServerSessionsAreReturned() {
        long tlog = System.currentTimeMillis() + DESTROY_LOG_INTERVAL_MS;
        for (;;) {
            int nOutstandingServerSessions;
            synchronized (mCountLock) {
                nOutstandingServerSessions = mNServerSessionsGivenOut;
            }
            
            // Wait if not all were destroyed
            if (nOutstandingServerSessions == 0) {
                if (sLog.isDebugEnabled()) {
                    sLog.debug("All server sessions were returned");
                }
                break;
            } else {
                if (System.currentTimeMillis() > tlog) {
                    sLog.info("Deactivating connector; waiting for server sessions to "
                        + "be returned; there are " + nOutstandingServerSessions
                        + " server sessions that are still in use; activation=" + mActivation);
                    tlog = System.currentTimeMillis() + DESTROY_LOG_INTERVAL_MS;
                }
                
                // Wait a bit
                if (sLog.isDebugEnabled()) {
                    sLog.debug(nOutstandingServerSessions
                        + " serversession(s) were (was) not destroyed... waiting");
                }
                try {
                    Thread.sleep(DESTROY_RETRY_INTERVAL_MS);
                } catch (Exception ex) {
                    // ignore
                }
            }
        }
    }
    
    private void closeConnection() {
        try {
            if (mConnection != null) {
                mConnection.close();
            }
        } catch (Exception ex) {
            sLog.warn("Unexpected exception closing JMS connection: " + ex, ex);
        }
        mConnection = null;
    }
    
    private void closeCC() {
        try {
            if (mCC != null) {
                mCC.close();
            }
        } catch (Exception ex) {
            sLog.warn("Unexpected exception closing JMS connection consumer: " + ex, ex);
        }
        synchronized (mCountLock) {
            mCC = null;
        }
    }
    
//    private void deactivateJMQ() {
//        
//    }
//    
//    private void deactivateSTCMS() {
//        // Stop connection
//        try {
//            if (mConnection != null) {
//                mConnection.stop();
//            }
//        } catch (Exception ex) {
//            sLog.warn("Unexpected exception stopping JMS connection consumer: " + ex, ex);
//        }
//        
//        // getServerSession() will no longer be called
//        // serverSession.start() will no longer be called
//        // run() may still be called
//        
//        // Wait until all containers have been returned
//        waitUntilAllWorkContainersAreDestroyed();
//        
//        closeCC();
//
//        // Close connection; this will call stop(); all containers will be "disabled"
//        closeConnection();
//    }
    
    private void deactivateGeneral() {
        // Close CC (used to be necessary for STCMS; still required for JMQ)
        closeCC();
        
        // For JMQ we need to ensure that there are no outstanding server sessions anymore
        if (mEnlistInRun) {
            waitUntilAllServerSessionsAreReturned();
        }

        // Close connection; this will call stop(); all containers will be "disabled"
        closeConnection();

        // Wait until all containers have been returned
        waitUntilAllWorkContainersAreDestroyed();
    }

    /**
     * Releases any resources associated with delivery. This will try to destroy
     * all WorkContainers. Destroying a WorkContainer may fail if it is in the
     * middle of processing a message. In that case, this method will wait a bit
     * and try again.
     * 
     * <P>
     * __Is called from service thread__
     */
    public void deactivate() {
        if (sLog.isDebugEnabled()) {
            sLog.debug("CCDelivery.deactivate() -- begin");
        }

        deactivateGeneral();

        // All threads have exited now
        mEmptyWorkContainers.clear();
        mAllWorkContainers.clear();
        mEmptyWorkContainerSemaphore = new Semaphore(0);

        if (sLog.isDebugEnabled()) {
            sLog.debug("CCDelivery.deactivate() -- complete");
        }
    }

    /**
     * Per ExceptionListener interface; called when there is a connection problem
     *
     * @param ex JMSException
     */
    public void onException(JMSException ex) {
        mActivation.distress(ex);
    }

    /**
     * @see com.stc.jmsjca.core.Delivery#getConfiguredEndpoints()
     */
    public int getConfiguredEndpoints() {
        return mNMaxWorkContainers;
    }
}
