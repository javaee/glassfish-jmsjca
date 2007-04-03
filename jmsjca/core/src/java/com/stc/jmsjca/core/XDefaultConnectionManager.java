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
import com.stc.jmsjca.util.FIFOSemaphore;
import com.stc.jmsjca.util.Logger;
import com.stc.jmsjca.util.Semaphore;
import com.stc.jmsjca.util.Utility;

import javax.resource.ResourceException;
import javax.resource.spi.ConnectionEvent;
import javax.resource.spi.ConnectionEventListener;
import javax.resource.spi.ConnectionManager;
import javax.resource.spi.ConnectionRequestInfo;
import javax.resource.spi.ManagedConnection;
import javax.resource.spi.ManagedConnectionFactory;
import javax.resource.spi.ValidatingManagedConnectionFactory;
import javax.security.auth.Subject;
import javax.transaction.Synchronization;
import javax.transaction.Transaction;
import javax.transaction.TransactionManager;
import javax.transaction.xa.XAResource;

import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

/**
 * <p>This manages connection in a managed or non-managed environment, e.g. in the 
 * case of JBI this is used inside the application server but outside of the context
 * of EJBs.</p>
 *
 * <pre>This connection manager has the following features:
 * sizing features:
 * - blocking behavior with timeout exception
 * - steady size, minimum size, maximum size
 * connection validity
 * - periodically checks for failed connections
 * transaction enlistment 
 * - enlists in transaction if one is present
 * - keeps an enlisted connection open
 * - reuses an enlisted idle connection
 * 
 * Wrt transactions, the following cases can be distinguished
 * 
 * Case 1:
 * =======
 * 1 begin
 * 2 create
 *     creates connection / gets from idle pool
 *     enlists in tx
 * 3 close
 *     cleanup
 *     put in txidle pool
 * 4 commit
 *     put in idle pool
 *     
 * Case 2:
 * =======
 * 1 create
 * 2 begin
 * 3 commit
 * 4 close
 * 
 * Case 3:
 * =======
 * 1 begin
 * 2 create
 *     create connection / get from idle pool
 *     enlist in tx
 * 3 commit
 * 4 close
 *     return to idle pool
 *     
 * Case 4:
 * =======
 * 1 create
 * 2 begin
 * 3 close
 * 4 commit
 * 
 * Case 5:
 * =======
 * 1 begin
 * 2 create
 *     create connection / get from idle pool
 *     enlist in tx
 * 3 close
 *     cleanup
 *     put in txidle pool (do NOT delist)
 * 4 create
 *     get from txidle pool
 *     do NOT enlist in tx
 * 5 close
 *     cleanup
 *     put in txidle pool (do NOT delist)
 * 6 commit
 *     return to idle pool
 *  
 * Case 2 and case 4 are not supported (yet); they rely on lazy enlistment
 *   
 *                  +-----------------------<-----------------------+
 *                 |                                               |  
 *                 |                                               |  
 *            ==========                                           |  
 *    +--yes-- has idle? ---no--------> limit? ------yes--------> wait
 *    |       ==========                  |                           
 *    |                                   |                           
 *    |                                  no                           
 *    |                                   |                           
 *  match?--no-->grow? -yes-> CREATE <----+                           
 *    |            |             ^  \                                 
 *    |            |             |   \                                
 *    |            |             |    \                               
 *    |            +--no----> DESTROY  \                              
 *    |                          ^      \                             
 *    |                          |      |                             
 *  VALID?---no------------------+      |                             
 *    |                                 |                             
 *    |                                 |                             
 *    |                                 |                             
 *    +---yes-----------------------> done                            
 *                                                                  
 * All is synchronized except VALID, CREATE and DESTROY (all caps)
 * 
 * The total number of available connections (created or not created; always smaller than
 * or equal to MAX) is maintained in a semaphore; this semaphore is used to make
 * threads block if there are no connections available. To make sure that blocked threads
 * get a connection in the order that they requested a connection, a fair semaphore is
 * used. This is the main reason for using a semaphore: otherwise the complexity of 
 * building a fair semaphore would have to be done in the connection manager.
 * The total number of created connections is maintained by mCurrentPoolsize: this is 
 * necessary so that connections can be created outside of the synchronized block. Hence
 * the size of the all-map is not a proper measure of how many connections are (about 
 * to be created). Idle connections are kept in an idle pool. In use connections are not 
 * tracked separately.
 * 
 * With counts being kept in separate places (the semaphore, maps), the implementation is
 * tricky: extreme care must be taken that all variables remain their consistency.
 * 
 * </pre>
 *
 * @author Frank Kieviet
 * @version $Revision: 1.1.1.4 $
 */
public class XDefaultConnectionManager implements ConnectionManager, RAStopListener {
    private static Logger sLog = Logger.getLogger(XDefaultConnectionManager.class);
    private ConnectionEventListener mConnectionEventListener;
    private Map mAll = new IdentityHashMap();  // key: managedconnection, value=ConnectionState
    // Keeps track of connections created and about to be created.
    // May not be equal to mAll.size() while a connection is being created
    private int mCurrentPoolsize; 
    // Keeps track of available connections, either in the idle pool or wrt connections not created
    // Does not include connections in the tx-idle pool
    private Semaphore mSemaphore;
    // The semaphore does not have a release-all method; consequently we're keeping track
    // of threads that are waiting so that the semaphore can be released by the proper
    // amount.
    private int mWaiters;
    private Map mIdle = new IdentityHashMap();  // key: managedconnection, value=null
    private Map mIdleEnlisted = new IdentityHashMap(); // key=Transaction, value=Set of managedconnection
    private Subject mTestSubject; // for testing purposes
    private XManagedConnectionFactory mFact;
    private int mMaxSize = 32;
    private int mMinSize = 0;
    private int mTimeout = -1;
    private boolean mIsInitialized;
    private boolean mStopped;
    private boolean mXATxFailureLogged;
    
    private static final Localizer LOCALE = Localizer.get();

    /**
     * Constructor
     *
     * @param managedConnectionFactory mcf
     */
    public XDefaultConnectionManager(XManagedConnectionFactory managedConnectionFactory) {
        mConnectionEventListener = new Listener();
        mFact = managedConnectionFactory;
        mFact.getRAJMSResourceAdapter().addStopListener(this);
    }
   
    /**
     * Connection pool properties may be set late in the construction process, so 
     * do lazy initialization
     */
    private synchronized void init() {
        if (mIsInitialized) {
            return;
        }

        Properties p = mFact.getOptionsAsProperties();
        mMaxSize = Utility.getIntProperty(p, Options.Out.POOL_MAXSIZE, mMaxSize);
        mMinSize = Utility.getIntProperty(p, Options.Out.POOL_MINSIZE, mMinSize);
        mTimeout = Utility.getIntProperty(p, Options.Out.POOL_TIMEOUT, mTimeout);
        if (mTimeout == 0) {
            mTimeout = Integer.MAX_VALUE;
        }
        if (mTimeout < 0) {
            mTimeout = 0;
        }
        
        mSemaphore = new FIFOSemaphore(mMaxSize);
        mIsInitialized = true;
    }
    
    /**
     * Gets the transaction manager
     */
    private TransactionManager getTransactionManager() throws Exception {
        if (mFact.getTxMgr() != null) {
            return mFact.getTxMgr().getTransactionManager();
        }
        return null;
    }
    
    /**
     * Gets the transaction manager IF APPLICABLE
     */
    private Transaction getXATx() throws Exception {
        if (!mFact.getOptionNoXA()) {
            TransactionManager txmgr = getTransactionManager();
            if (txmgr != null) {
                return txmgr.getTransaction();
            }
        }
        return null;
    }
    
    /**
     * Gets the transaction manager IF APPLICABLE without throwing an exception
     */
    private Transaction getXATxNoExc() {
        try {
            return getXATx();
        } catch (Exception e) {
            if (!mXATxFailureLogged) {
                sLog.warn(LOCALE.x("E067: Could not get hold of transaction or transaction manager: {0}", e), e);
                mXATxFailureLogged = true;
            }
            return null;
        }
    }
    
    /**
     * @see com.stc.jmsjca.core.RAStopListener#stop()
     */
    public void stop() {
        ManagedConnection[] mcsToDestroy;
        synchronized (this) {
            // Get all idle connections for destruction
            mcsToDestroy = (ManagedConnection[]) mIdle.keySet().toArray(new ManagedConnection[mIdle.size()]);
            mIdle.clear();
            
            // Notify waiters about stopped state
            mStopped = true;
            if (mSemaphore != null) {
                mSemaphore.release(mWaiters);
            }
        }
        
        // Destroy connections outside of synchronization block
        for (int i = 0; i < mcsToDestroy.length; i++) {
            destroyAndAdjust(mcsToDestroy[i]);
        }
    }

    /**
     * Returns a matching connection that is idle and is associated with the specified 
     * transaction or null if none found. Adjusts pool parameters.
     */
    private synchronized ManagedConnection findIdleConnectionInTxAndAdjust(
        ManagedConnectionFactory managedConnectionFactory,
        ConnectionRequestInfo connectionRequestInfo, Transaction tx) throws ResourceException {
        
        ManagedConnection mc = null;

        Set candidates = (Set) mIdleEnlisted.get(tx);
        
        if (candidates != null) {
            mc = managedConnectionFactory.matchManagedConnections(candidates, mTestSubject,
                connectionRequestInfo);
            if (mc != null) {
                candidates.remove(mc);
                if (candidates.isEmpty()) {
                    mIdleEnlisted.remove(tx);
                }
            }
        }
        
        return mc;
    }

    /**
     * Finds a matching connection in the idle pool and returns it; adjusts pool
     * parameters. Must be called from a synchronized method. Returns null if none found. 
     */
    private ManagedConnection findIdleConnectionAndAdjust(
        ManagedConnectionFactory mcf, ConnectionRequestInfo descr) throws ResourceException {

        ManagedConnection mc = null;
        Set candidates = mIdle.keySet();
        mc = mcf.matchManagedConnections(candidates, mTestSubject, descr);
        if (mc != null) {
            mIdle.remove(mc);
        }

        return mc;
    }
    
    /**
     * Checks whether a connection is invalid; will NOT throw an exception
     */
    private boolean isInvalid(ManagedConnectionFactory mcf, ManagedConnection mc) {
        boolean ret = false;
        if (mcf == mFact || mcf instanceof javax.resource.spi.ValidatingManagedConnectionFactory) {
            ValidatingManagedConnectionFactory vmcf = (ValidatingManagedConnectionFactory) mcf;

            // check if connection is valid
            Set validtest = new HashSet();
            validtest.add(mc);
            try {
                Set invalid = vmcf.getInvalidConnections(validtest);
                ret = invalid.size() > 0;
            } catch (ResourceException e) {
                sLog.warn(LOCALE.x("E068: Unexpected error while checking a connection for validity: {0}", e), e);
                ret = true;
            }
        }
        return ret;
    }
    
    /**
     * <p>From the JCA spec:</p>
     * <p>The method allocateConnection gets called by the resource adapter's connection
     * factory instance. This lets connection factory instance (provided by the resource
     * adapter) pass a connection request to the ConnectionManager instance.
     * The connectionRequestInfo parameter represents information specific to the
     * resource adapter for handling of the connection request. (end spec)</p>
     *
     * <p>This will try to see if there is a connection in the pool that can service
     * the request, if not it will create a new managed connection.</p>
     *
     * @param mcf managedConnectionFactory
     * @param descr how to create the connection
     * @return connection handle with an EIS specific connection interface.
     * @throws ResourceException on failure
     */
    public Object allocateConnection(ManagedConnectionFactory mcf,
        ConnectionRequestInfo descr) throws ResourceException {
        
        init();
        
        // Try to get from tx; this connection doesn't count against the semaphore
        Object ret = tryObtainConnectionFromTx(mcf, descr);
        
        if (ret == null) {
            // OBTAIN SEMAPHORE
            boolean acquired;
            try {
                synchronized (this) {
                    mWaiters++;
                    if (mStopped) {
                        throw new ResourceException("Resource adapter was stopped");
                    }
                }
                acquired = mSemaphore.attempt(mTimeout);
            } catch (InterruptedException e) {
                throw new ResourceException("Interrupted");
            } finally {
                synchronized (this) {
                    mWaiters--;
                }
            }
            
            // TIMEOUT
            if (!acquired) {
                throw new ResourceException("Connection could not be acquired in the "
                + "configured time of " + mTimeout + " ms; " + mMaxSize + " connections "
                + "are in use");
            }
            
            // OBTAIN CONNECTION
            try {
                ret = obtainConnection(mcf, descr);
            } catch (ResourceException e) {
                // Semaphore was acquired, so in the case an exception was not obtained
                // the semaphore should be reset to its original position
                mSemaphore.release();
                throw e;
            }
        }
        return ret;
    }

    /**
     * Returns a valid connection or throws an exception
     */
    private Object obtainConnection(ManagedConnectionFactory mcf,
        ConnectionRequestInfo descr) throws ResourceException {     

        if (sLog.isDebugEnabled()) {
            sLog.debug("Allocating connection using " + mcf + "; request=" + descr);
        }

        try {
            ManagedConnection mc = null;
            boolean connectionNeedsToBeCreated = false;
            boolean connectionNeedsToBeValidated = false;
            ManagedConnection toDestroy = null;
            
            Object ret = null;
            
            // Find out what needs to be done in a synchronized block, then outside
            // of the synchronized block peform those (potentially lengthy!) operations.
            synchronized (this) {
                if (mStopped) {
                    throw new ResourceException("RA is stopped");
                }

                boolean done = false;

                // Try to reuse from idle pool
                // May throw; is ok: no state change
                mc = findIdleConnectionAndAdjust(mcf, descr);
                if (mc != null) {
                    connectionNeedsToBeValidated = true;
                    done = true;
                }

                // Can the pool grow? 
                if (!done && mCurrentPoolsize < mMaxSize) {
                    connectionNeedsToBeCreated = true;
                    // Adjust pool size now, i.e. before creation
                    mCurrentPoolsize++;
                    done = true;
                }

                // Can and need to sacrifice an idle connection?
                if (!done && !mIdle.isEmpty()) {
                    toDestroy = (ManagedConnection) mIdle.keySet().iterator().next();
                    mIdle.remove(toDestroy);
                    // substitute the idle one by a new one (no change to size setting)
                    mAll.remove(toDestroy);
                    connectionNeedsToBeCreated = true;
                    done = true;
                }

                if (!done) {
                    throw new Exception("Logic exception (current pool size=" + mCurrentPoolsize
                        + "; semaphore=" + mSemaphore.peek() + ")");
                }
            }

            // Validate connection (outside of synchronized block)
            if (connectionNeedsToBeValidated) {
                if (isInvalid(mcf, mc)) {
                    toDestroy = mc;
                    mAll.remove(mc);
                    connectionNeedsToBeCreated = true;
                }
            }
            
            // Destroy a connection if required (outside of synchronized block)
            if (toDestroy != null) {
                try {
                    toDestroy.destroy();
                } catch (Exception ex) {
                    sLog.error(LOCALE.x("E069: Unexpected exception when destroying a connection "
                    + "(connection={0}): {1}", toDestroy, ex), ex);
                }
            }

            // Create a connection if required (outside of synchronized block)
            if (connectionNeedsToBeCreated) {
                try {
                    mc = mcf.createManagedConnection(mTestSubject, descr);
                    synchronized (this) {
                        mAll.put(mc, new ConnectionState());
                        // Note: size was already adjusted
                    }
                } catch (Exception e) {
                    // Size was already adjusted, so make sure that now that a connection
                    // cannot be created, the size is put back
                    synchronized (this) {
                        mCurrentPoolsize--;
                    }
                    throw e;
                }
            }

            if (mc != null) {
                // Get notified of events
                try {
                    mc.addConnectionEventListener(mConnectionEventListener);

                    // Enlist transaction
                    Transaction tx = getXATxNoExc();
                    if (tx != null) {
                        tx.enlistResource(mc.getXAResource());
                        // Need to delist from the tx before prepare/commit/rollback
                        tx.registerSynchronization(new TxDelister(mc, tx));
                    }

                    // Return the connection handle to the client (the wrapper with the proper
                    // interface)
                    ret = mc.getConnection(mTestSubject, descr);
                } catch (Exception e) {
                    // Destroy connection; size was already adjusted, so make sure that 
                    // now that a connection cannot be created, the size is put back
                    synchronized (this) {
                        mAll.remove(mc);
                        mCurrentPoolsize--;
                    }
                    try {
                        mc.destroy();
                    } catch (Exception e2) {
                        sLog.warn(LOCALE.x("E070: Connection could not be setup or enlisted: [{0}]"
                            + "... and could not be destroyed properly either: [{1}]"
                            + " stacktrace of destruction problem follows in next " 
                            + "log entry.", e, e2), e);
                        sLog.warn(LOCALE.x("E071: Destruction exception: {0}", e2), e2);
                    }
                    throw e;
                }
            }
            
            return ret;
        } catch (Exception ex) {
            throw Exc.rsrcExc(LOCALE.x("E073: Could not allocate connection: {0}", ex), ex);
        }
    }
    
    private Object tryObtainConnectionFromTx(ManagedConnectionFactory mcf,
        ConnectionRequestInfo descr) throws ResourceException {     

        if (sLog.isDebugEnabled()) {
            sLog.debug("Allocating connection using " + mcf + "; request=" + descr);
        }

        try {
            ManagedConnection mc = null;
            Transaction tx = getXATxNoExc();
            Object ret = null;

            synchronized (this) {
                // Try to reuse idle in transaction
                if (tx != null) {
                    mc = findIdleConnectionInTxAndAdjust(mcf, descr, tx);
                }
            }

            if (mc != null) {
                // Get notified of events
                mc.addConnectionEventListener(mConnectionEventListener);

                // Return the connection handle to the client (the wrapper with the proper
                // interface)
                ret = mc.getConnection(mTestSubject, descr);
            }
            
            return ret;
        } catch (Exception ex) {
            throw Exc.rsrcExc(LOCALE.x("E073: Could not allocate connection: {0}", ex), ex);
        }
    }
    
    /**
     * Destroys a connection and removes it from the global list
     */
    private void destroyAndAdjust(ManagedConnection mc) {
        // Destroy
        try {
            mc.destroy();
        } catch (Exception ex) {
            sLog.error(LOCALE.x("E074: Unexpected exception destroying a connection: {0}", ex), ex);
        }
        
        // Unregister
        synchronized (this) {
            mAll.remove(mc);
        }
        
        mSemaphore.release();
    }
    
    private class TxDelister implements Synchronization {
        private ManagedConnection mMC;
        private Transaction mTX;

        public TxDelister(ManagedConnection mc, Transaction tx) {
            mMC = mc;
            mTX = tx;
        }

        /**
         * @see javax.transaction.Synchronization#afterCompletion(int)
         */
        public void afterCompletion(int status) {
        }

        /**
         * @see javax.transaction.Synchronization#beforeCompletion()
         */
        public void beforeCompletion() {
            // Delist from transaction
            try {
                mTX.delistResource(mMC.getXAResource(), XAResource.TMSUCCESS);
            } catch (Exception e) {
                sLog.warn(LOCALE.x("E075: XAResource could not be delisted ({0}): {1}", mMC, e), e);
            }
        }
    }

    private class TxDeferredRelease implements Synchronization {
        private ManagedConnection mMC;

        public TxDeferredRelease(ManagedConnection mc, Transaction tx) {
            mMC = mc;
        }

        /**
         * @see javax.transaction.Synchronization#afterCompletion(int)
         */
        public void afterCompletion(int status) {
            ManagedConnection toBeDestroyed = null;
            synchronized (XDefaultConnectionManager.this) {
                ConnectionState state = (ConnectionState) mAll.get(mMC);
                state.setTxDeferredReleaseRegistered(false);
                if (state.isBad() || mStopped) {
                    toBeDestroyed = mMC;
                } else {
                    mIdle.put(mMC, null);
                    mSemaphore.release();
                }
            }
            
            if (toBeDestroyed != null) {
                destroyAndAdjust(mMC);
            }
        }

        /**
         * @see javax.transaction.Synchronization#beforeCompletion()
         */
        public void beforeCompletion() {
        }
    }
    
    /**
     * Called (through the connection listener) when THE APPLICATION calls close() on the
     * connection
     */
    private void connectionClosed(final ManagedConnection mc) {
        // Transaction?
        Transaction tx = getXATxNoExc();

        if (tx == null) {
            // No transaction: simply cleanup and add to pool
            ManagedConnection toDestroy = null;
            try {
                mc.cleanup();
                synchronized (this) {
                    if (mStopped) {
                        toDestroy = mc;
                    } else {
                        mIdle.put(mc, null);
                        mSemaphore.release();
                    }
                }
            } catch (Exception e) {
                if (sLog.isDebugEnabled()) {
                    sLog.debug("Caught exception in cleanup(); connection will be destroyed: " + e, e);
                }
                toDestroy = mc;
            }
            
            if (toDestroy != null) {
                destroyAndAdjust(mc);
            } else {
                if (sLog.isDebugEnabled()) {
                    sLog.debug("Connection closed event received; added connection " + mc + " to idle-pool");
                }
            }
        } else {
            // Should not close or put in idle, but add to tx-idle pool
            Exception cleanupException = null;
            try {
                mc.cleanup();
            } catch (Exception e) {
                cleanupException = e;
                if (sLog.isDebugEnabled()) {
                    sLog.debug("Unexpected error in cleanup: " + e, e);
                }
            }
            
            // Put in TxIdle pool
            boolean isAlreadyRegistered;
            synchronized (this) {
                ConnectionState state = (ConnectionState) mAll.get(mc);

                // Update state if cleanup failed
                if (!state.isBad()) {
                    state.setBad(cleanupException);
                }
                
                // Add to idleInTx pool
                Set idleInTx = (Set) mIdleEnlisted.get(tx);
                if (idleInTx == null) {
                    idleInTx = new HashSet();
                    mIdleEnlisted.put(tx, idleInTx);
                }
                idleInTx.add(mc);
                
                // Figure out if tx listener is required
                isAlreadyRegistered = state.isTxDeferredReleaseRegistered();
                if (!isAlreadyRegistered) {
                    state.setTxDeferredReleaseRegistered(true);
                }
            }
            
            // Add behavior when commit() is called
            if (!isAlreadyRegistered) {
                try {
                    tx.registerSynchronization(new TxDeferredRelease(mc, tx));
                } catch (Exception e) {
                    sLog.error(LOCALE.x("E076: Synchronization registration failed: {0}", e), e);
                }
            }
        }
    }
    
    private class Listener implements ConnectionEventListener, java.io.Serializable {
        public void connectionClosed(ConnectionEvent connectionEvent) {
            ManagedConnection mc = (ManagedConnection) connectionEvent.getSource();
            mc.removeConnectionEventListener(this);
            XDefaultConnectionManager.this.connectionClosed(mc);
        }

        public void localTransactionStarted(ConnectionEvent connectionEvent) {
        }

        public void localTransactionCommitted(ConnectionEvent connectionEvent) {
        }

        public void localTransactionRolledback(ConnectionEvent connectionEvent) {
        }

        public void connectionErrorOccurred(ConnectionEvent connectionEvent) {
        }
    }
    
    private class ConnectionState {
        private Exception mBad;
        private boolean mTxDeferredReleaseRegistered;
        public void setBad(Exception e) {
            mBad = e;
        }
        public void setTxDeferredReleaseRegistered(boolean isRegistered) {
            mTxDeferredReleaseRegistered = isRegistered;
        }
        public boolean isTxDeferredReleaseRegistered() {
            return mTxDeferredReleaseRegistered;
        }
        public boolean isBad() {
            return mBad != null;
        }
    }
    
    /**
     * @return the associated RA
     */
    public RAJMSResourceAdapter getRAJMSResourceAdapter() {
        return mFact.getRAJMSResourceAdapter();
    }
    
    /**
     * @return the associated MCF
     */
    public XManagedConnectionFactory getMCF() {
        return mFact;
    }

    
    ///// TEST METHODS ///////////////////////////////////////////////////////////////////
    
    /**
     * For testing only: puts the specified connection in the pool
     *
     * @param mc ManagedConnection
     */
    public void testAddToPool(ManagedConnection mc) {
        mIdle.put(mc, null);
    }
    
    /**
     * Assuming that all connections have been returned to the pool, this checks if
     * the separate counters are consistent
     */
    public void testIdleConsistency() {
        if (mSemaphore.peek() != mIdle.size() + (mMaxSize - mAll.size())) {
            throw new RuntimeException("Inconsistent: semaphore=" + mSemaphore.peek()
                + ", idle=" + mIdle.size() + ", max=" + mMaxSize + ",all=" + mAll.size());
        }
    }

    /**
     * Tests if the basic premise of consistency is met 
     */
    public void testConsistency() {
        if (mStopped) {
            return;
        }
        init();
        if (mSemaphore.peek() > mMaxSize || mAll.size() > mMaxSize) {
            throw new RuntimeException("Inconsistent: semaphore=" + mSemaphore.peek()
                + ", idle=" + mIdle.size() + ", max=" + mMaxSize + ",all=" + mAll.size());
        }
    }

    /**
     * clear; for testing
     *
     * @throws ResourceException failure
     */
    public void clear() throws ResourceException {
        testConsistency();
        for (Iterator iter = mIdle.keySet().iterator(); iter.hasNext();/*-*/) {
            ManagedConnection mc = (ManagedConnection) iter.next();
            mc.destroy();
        }
        mIdle.clear();
    }

    /**
     * clear; for testing
     *
     * @throws ResourceException failure
     */
    public void clearAll() throws ResourceException {
        testConsistency();
        for (Iterator iter = mAll.keySet().iterator(); iter.hasNext();/*-*/) {
            ManagedConnection mc = (ManagedConnection) iter.next();
            mc.destroy();
        }
        mAll.clear();
        mIdle.clear();
        mCurrentPoolsize = 0;
    }

    /**
     * Sets the subject for testing purposes
     * 
     * @param s Subject
     */
    public void setSubject(Subject s) {
        mTestSubject = s;
    }

    /**
     * Destroys invalid connections
     * 
     * @throws ResourceException failure
     */
    public void cleanInvalid() throws ResourceException {
        for (Iterator iter = mIdle.keySet().iterator(); iter.hasNext();/*-*/) {
            XManagedConnection mc = (XManagedConnection) iter.next();
            if (mc.isInvalid()) {
                mc.destroy();
            }
        }
    }
}
