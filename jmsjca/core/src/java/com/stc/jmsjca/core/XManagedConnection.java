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
import com.stc.jmsjca.util.InterceptorChain;
import com.stc.jmsjca.util.Logger;
import com.stc.jmsjca.util.Str;

import javax.jms.JMSException;
import javax.jms.QueueConnection;
import javax.jms.QueueSession;
import javax.jms.Session;
import javax.resource.ResourceException;
import javax.resource.spi.ConnectionEvent;
import javax.resource.spi.ConnectionEventListener;
import javax.resource.spi.ConnectionRequestInfo;
import javax.resource.spi.LocalTransaction;
import javax.resource.spi.ManagedConnection;
import javax.resource.spi.ManagedConnectionMetaData;
import javax.resource.spi.security.PasswordCredential;
import javax.security.auth.Subject;
import javax.transaction.xa.XAResource;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * <p>This represents a connection the JMS server; it holds a physical connection to the
 * JMS server.</p>
 *
 * <p>From the spec: ManagedConnection instance represents a physical connection to the
 * underlying EIS. A ManagedConnection instance provides access to a pair of interfaces:
 * javax.transaction.xa.XAResource and javax.resource.spi.LocalTransaction. XAResource
 * interface is used by the transaction manager to associate and dissociate a transaction
 * with the underlying EIS resource manager instance and to perform two-phase commit
 * protocol. The ManagedConnection interface is not directly used by the transaction
 * manager. More details on the XAResource interface are described in the JTA
 * specification. The LocalTransaction interface is used by the application server to
 * manage local transactions. End spec.</p>
 *
 * @author Frank Kieviet
 * @version $Revision: 1.11 $
 */
public class XManagedConnection implements ManagedConnection {
    private static Logger sLog = Logger.getLogger(XManagedConnection.class);
    private List<ConnectionEventListener> mConnectionEventListeners;
    private List<WSession> mHandles = new ArrayList<WSession>();
    private transient PrintWriter mLogWriter;
    private XConnectionRequestInfo mConnectionDescription;
    private XManagedConnectionFactory mManagedConnectionFactory;
    private JSession mJSession;
    private XAResource mXAResource;
    private LocalTransaction mLocalTransaction;
    private XManagedConnectionMetaData mMetaData;
    private transient long mCreatedAt = System.currentTimeMillis();
    private String mUserid;
    private String mPassword;
    private long mLastUsedSuccessfullyAt;
    private InterceptorChain mInterceptorChain;

    private static final Localizer LOCALE = Localizer.get();

    /**
     * Constructor
     *
     * @param mcf XManagedConnectionFactory
     * @param subject Subject
     * @param descr XConnectionRequestInfo
     * @throws ResourceException failure
     */
    public XManagedConnection(XManagedConnectionFactory mcf, Subject subject,
        XConnectionRequestInfo descr) throws javax.resource.ResourceException {
        mManagedConnectionFactory = mcf;
        
        // Instantiate interceptors early to avoid connection leaks
        try {
            mInterceptorChain = mcf.getInterceptorChainBuilder().create();
        } catch (Exception e) {
            throw Exc.rsrcExc(LOCALE.x("E219: Could not instantiate interceptors: {0}", e), e);
        }
        
        if (descr == null) {
            // This may be null during XA recovery; create a default one that will
            // work for XA recovery (just to create a connection)
            descr = new XConnectionRequestInfo(
                    QueueConnection.class, QueueSession.class, 
                    null, null, 
                    null, null, true, Session.AUTO_ACKNOWLEDGE);
        }

        mConnectionDescription = descr;
        
        boolean xa = !mManagedConnectionFactory.getOptionNoXA();
        
        String[] uidpw = mcf.getEffectiveUseridAndPassword(descr, subject);
        mUserid = uidpw[0];
        mPassword = uidpw[1];
        
        // Create session
        try {
            mJSession = new JSession(xa, descr.getTransacted(), descr.getAcknowledgeMode(),
                descr.getSessionClass(), this);
        } catch (JMSException ex) {
            throw Exc.rsrcExc(LOCALE.x("E084: Failed to create session: {0}", ex), ex);
        }

        // Get XAResource
        if (xa) {
            try {
                mXAResource = mJSession.getXAResource();
                
                if (mManagedConnectionFactory.isOverrideIsSameRM()) {
                    mXAResource = new WXAResourceNoIsSameRM(mXAResource);
                }
            } catch (JMSException ex) {
                if (mJSession != null) {
                    try {
                        mJSession.destroy();
                    } catch (JMSException ignore) {
                        // ignore
                    }
                }
                throw Exc.rsrcExc(LOCALE.x("E085: Could not obtain XAResource: {0}", ex), ex);
            }
        } else if (descr.getTransacted()) {
                try {
                    mXAResource = new PseudoXAResource((Session) mJSession.getDelegate());
                } catch (JMSException e) {
                    try {
                        mJSession.destroy();
                    } catch (JMSException ignore) {
                        // ignore
                    }
                    throw Exc.rsrcExc(LOCALE.x("E086: Could not create pseudo XAResource: {0}", e), e);
                }
        } else {
            mXAResource = new PseudoXAResourceNOP(); 
        }

        // Set LocalTransaction
        if (xa) {
            mLocalTransaction = new XLocalTransactionXA(this);
        } else if (descr.getTransacted()) {
            mLocalTransaction = new XLocalTransaction(this);
        } else {
            mLocalTransaction = new XLocalTransactionNOP();
        }
        
        mLastUsedSuccessfullyAt = System.currentTimeMillis();
    }

    /**
     * From the spec:
     * Application server calls this method to force any cleanup on the ManagedConnection
     * instance.
     *
     * The method ManagedConnection.cleanup initiates a cleanup of the any
     * client-specific state as maintained by a ManagedConnection instance. The cleanup
     * should invalidate all connection handles that had been created using this
     * ManagedConnection instance. Any attempt by an application component to use the
     * connection handle after cleanup of the underlying ManagedConnection should result
     * in an exception.
     *
     * The cleanup of ManagedConnection is always driven by an application server. An
     * application server should not invoke ManagedConnection.cleanup when there is an
     * uncompleted transaction (associated with a ManagedConnection instance) in progress.
     *
     * The invocation of ManagedConnection.cleanup method on an already cleaned-up
     * connection should not throw an exception.
     *
     * The cleanup of ManagedConnection instance resets its client specific state and
     * prepares the connection to be put back in to a connection pool. The cleanup
     * method should not cause resource adapter to close the physical pipe and reclaim
     * system resources associated with the physical connection.
     * End spec
     *
     * @throws ResourceException if any exception has occurred during processing,
     * an exception is thrown so that the connection will not be reused. The application
     * server should catch the exception and call destroy() and release the connection
     * from its local pool (interpretation of the spec).
     */
    public void cleanup() throws ResourceException {
        if (sLog.isDebugEnabled()) {
            sLog.debug("Cleaning up connection");
        }

        // Make the producers/consumers look like they are closed
        if (mJSession != null) {
            mJSession.cleanup();
        }

        // Make all session handles look like they are closed
        for (int i = 0; i < mHandles.size(); i++) {
            WSession w = mHandles.get(i);
            w.setClosed();
        }
        mHandles.clear();

        mMetaData = null;
              
        getManagedConnectionFactory().getObjFactory().cleanup(this);
                
    }

    /**
     * Returns if this connection is invalid and needs to be destroyed; called by
     * getInvalidConnections() by the application server.
     *
     * @return boolean
     */
    public boolean isInvalid() {
        if (mManagedConnectionFactory.isTestModeInvalidConnections()) {
            return true;
        }
        
        if (getManagedConnectionFactory().getObjFactory().isInvalid(this)) {
            return true;
        }

        // Test exception condition
        if (mJSession != null && mJSession.hasExceptionOccurred()) {
            if (sLog.isDebugEnabled()) {
                sLog.debug("isInvalid=false: one or more exceptions occurred "
                    + "during the use of this connection, and hence should not be "
                    + "reused. There were " + mJSession.getExceptionCount() + " exceptions "
                    + " in total. The first exception was: " + mJSession.getFirstException(),
                    mJSession.getFirstException());
            }
            return true;
        }
        
        // Test staleness
        if (mJSession != null && (System.currentTimeMillis() - getLastSuccessfullyUsedAt()) 
            > mManagedConnectionFactory.internalGetIdleTimeout()) {
            if (sLog.isDebugEnabled()) {
                sLog.debug("Stale connection: will be destroyed. Age: " 
                    + (System.currentTimeMillis() - getLastSuccessfullyUsedAt()));
            }
            return true;
        }
        return false;
    }
    
    /**
     * Called after every successful send and receive operation so that the MC can keep
     * track of when the connection was used successfully the last time.
     */
    public void onSuccessfulOperation() {
        mLastUsedSuccessfullyAt = System.currentTimeMillis();
    }
    
    /**
     * @return the timestamp of when the MC was used last successfully
     */
    public long getLastSuccessfullyUsedAt() {
        return mLastUsedSuccessfullyAt;
    }
    
//    public boolean isStale(long limit) {
//        if (mLastUsedSuccessfullyAt < limit) {
//            if (sLog.isDebugEnabled()) {
//                sLog.debug("Connection was used successfully "
//                    + (System.currentTimeMillis() - mLastUsedSuccessfullyAt)
//                    + " ms ago: stale connection.");
//            }
//            return true;
//        }
//        return false;
//    }
//    
//    public void sendConnectionErrorEvent() {
//        ConnectionEvent ev = new ConnectionEvent(this, ConnectionEvent.CONNECTION_ERROR_OCCURRED);
//        distributeEvent(ev);
//    }

    /**
     * <p>From the spec: Destroys the physical connection to the underlying resource
     * manager. To manage the size of the connection pool, an application server can
     * explictly call ManagedConnection.destroy to destroy a physical connection. A
     * resource adapter should destroy all allocated system resources for this
     * ManagedConnection instance when the method destroy is called. End spec</p>
     *
     * @throws ResourceException failure
     */
    public void destroy() throws ResourceException {
        if (mJSession != null) {
            if (sLog.isDebugEnabled()) {
                sLog.debug("Destroying connection");
            }

            try {
                mJSession.destroy();
            } catch (JMSException ex) {
                LocalizedString msg = LOCALE.x("E087: Error while closing session: {0}", ex);
                sLog.warn(msg);
                throw Exc.rsrcExc(msg, ex);
            } finally {
                mJSession = null;
            }
        }
        mManagedConnectionFactory.notifyMCDestroyed(this);
    }

    /**
     * <p> Returns an javax.transaction.xa.XAresource instance. An application server
     * enlists this XAResource instance with the Transaction Manager if the
     * ManagedConnection instance is being used in a JTA transaction that is being
     * coordinated by the Transaction Manager </p>
     *
     * @return javax.transaction.xa.XAResource
     * @throws ResourceException failure
     */
    public XAResource getXAResource() throws ResourceException {
        return mXAResource;
    }

    /**
     * Returns an javax.resource.spi.LocalTransaction instance. The LocalTransaction
     * interface is used by the container to manage local transactions for a RM instance.
     *
     * @return javax.resource.spi.LocalTransaction
     * @throws ResourceException failure
     */
    public LocalTransaction getLocalTransaction() throws ResourceException {
        return mLocalTransaction;
    }

    /**
     * Gets the metadata information for this connection's underlying EIS resource manager
     * instance. The ManagedConnectionMetaData interface provides information about the
     * underlying EIS instance associated with the ManagedConenction instance.
     *
     * @return javax.resource.spi.ManagedConnectionMetaData
     */
    public ManagedConnectionMetaData getMetaData() {
        if (mMetaData == null) {
            mMetaData = new XManagedConnectionMetaData(this);
        }
        return mMetaData;
    }

    /**
     * <p>From the spec: Creates a new connection handle for the underlying physical
     * connection represented by the ManagedConnection instance. This connection handle is
     * used by the application code to refer to the underlying physical connection. This
     * connection handle is associated with its ManagedConnection instance in a resource
     * adapter implementation specific way. The ManagedConnection uses the Subject and
     * additional ConnectionRequest Info (which is specific to resource adapter and opaque
     * to application server) to set the state of the physical connection. </p> This will
     * be called after creation of the managed connection, AND it will be called afer
     * fetching the managed connection from the pool. Unclear: will it be called in the
     * case of connection sharing?
     *
     * @param subject Subject
     * @param connectionRequestInfo ConnectionRequestInfo
     * @return java.lang.Object
     * @throws ResourceException failure
     */
    public Object getConnection(Subject subject,
        ConnectionRequestInfo connectionRequestInfo) throws ResourceException {
        if (getManagedConnectionFactory().getObjFactory().isInvalid(this)) {
            throw Exc.rsrcExc(LOCALE.x("E194: Invalid Connection "));
        }

        // Check credentials
        PasswordCredential pc = mManagedConnectionFactory.getPasswordCredential(subject);
        if (pc != null && 
            (!Str.isEqual(pc.getUserName(), getUserid()) 
                || !Str.isEqual(Str.pwdecode(new String(pc.getPassword())), Str.pwdecode(getPassword())))) {
            throw new javax.resource.spi.SecurityException(LOCALE.x("E164: Invalid subject {0}"
                , subject).toString());
        }

        // Create a new handle
        WSession h = mJSession.createHandle();
        mHandles.add(h);

        return h;
    }

    /**
     * From the spec: Used by the container to change the association of an
     * application-level connection handle with a ManagedConneciton instance. The
     * container should find the right ManagedConnection instance and call the
     * associateConnection method. The resource adapter is required to implement the
     * associateConnection method. The method implementation for a ManagedConnection
     * should dissociate the connection handle (passed as a parameter) from its currently
     * associated ManagedConnection and associate the new connection handle with itself.
     * End spec
     *
     * @param handle is an application handle, i.e. the proxy object produced by a
     *   XJWrapper.
     * @throws ResourceException failure
     */
    public void associateConnection(Object handle) throws ResourceException {
        WSession w = (WSession) handle;
        w.getJSession().getManagedConnection().disassociateConnection(w);
        w.setJSession(mJSession);
        mHandles.add(w);

        if (sLog.isDebugEnabled()) {
            sLog.debug("Associating connection with " + handle);
        }
    }

    /**
     * (Not a spec-method) Called by associateConnection() to diassociate the managed
     * connection from the handle
     *
     * @param w WSession
     * @throws ResourceException failure
     */
    public void disassociateConnection(WSession w) throws ResourceException {
        if (!mHandles.remove(w)) {
            throw Exc.rsrcExc(LOCALE.x("E165: Handle {0}"
                + " is not known in this managed connection", w));
        }
    }

    /**
     * getDescription
     *
     * @return XConnectionRequestInfo
     */
    public XConnectionRequestInfo getDescription() {
        return mConnectionDescription;
    }

    /**
     * Called by the connection handle when the application closes the handle. This
     * notifies this instance that the handle is closed, and that the physical connection
     * is free for reuse.
     *
     * @param s connection handle
     * @throws JMSException failure
     */
    public void notifyClosedByApplicationConnection(WSession s) throws JMSException {
        if (sLog.isDebugEnabled()) {
            sLog.debug("Application is closing the connection handle");
        }
        
        // Forget about the handle
        mHandles.remove(s);
        
        // Notify the connection manager
        ConnectionEvent ev = new ConnectionEvent(this, ConnectionEvent.CONNECTION_CLOSED);
        ev.setConnectionHandle(s);
        distributeEvent(ev);
    }

    /**
     * Called when a JSession does a commit (Transacted mode)
     *
     * @param s JSession
     */
    public void notifyCommit(WSession s) {
        if (sLog.isDebugEnabled()) {
            sLog.debug("Application is has invoked commit");
        }

        // Notify the connection manager
        ConnectionEvent ev = new ConnectionEvent(this, ConnectionEvent.LOCAL_TRANSACTION_COMMITTED);
        ev.setConnectionHandle(s);
        distributeEvent(ev);
    }

    /**
     * Called when a JSession does a rollback
     *
     * @param s JSession
     */
    public void notifyRollback(WSession s) {
        // Notify the connection manager
        ConnectionEvent ev = new ConnectionEvent(this, ConnectionEvent.LOCAL_TRANSACTION_ROLLEDBACK);
        ev.setConnectionHandle(s);
        distributeEvent(ev);
    }
    
    /**
     * Called when an connection error occurs.
     *
     * @param ex exception related to this error
     */
    public void notifyConnectionErrorOccured(Exception ex) {
        if (sLog.isDebugEnabled()) {
            sLog.debug("Error occured in the connection", ex);
        }
    }

    /**
     * Returns the managed connection factory that created this managed connection
     *
     * @return s/e
     */
    public XManagedConnectionFactory getManagedConnectionFactory() {
        return mManagedConnectionFactory;
    }

    /**
     * Adds a connection event listener to the ManagedConnection instance.
     * The registered ConnectionEventListener instances are notified of connection close
     * and error events, also of local transaction related events on the Managed Connection.
     *
     * @param connectionEventListener a new ConnectionEventListener to be registered
     */
    public synchronized void addConnectionEventListener(ConnectionEventListener connectionEventListener) {
        if (mConnectionEventListeners == null) {
            mConnectionEventListeners = new ArrayList<ConnectionEventListener>();
        }
        mConnectionEventListeners.add(connectionEventListener);
    }

    /**
     * Removes an already registered connection event listener from the ManagedConnection
     * instance.
     *
     * @param connectionEventListener already registered connection event listener to be removed
     */
    public synchronized void removeConnectionEventListener(ConnectionEventListener
        connectionEventListener) {
        for (Iterator<ConnectionEventListener> it = mConnectionEventListeners.iterator(); it.hasNext();/*-*/) {
            ConnectionEventListener o = it.next();
            if (connectionEventListener == o) {
                it.remove();
            }
        }
    }

    /**
     * Sets the log writer for this ManagedConnection instance. The log writer is a
     * character output stream to which all logging and tracing messages for this
     * ManagedConnection instance will be printed. Application Server manages the
     * association of output stream with the ManagedConnection instance based on the
     * connection pooling requirements. When a ManagedConnection object is initially
     * created, the default log writer associated with this instance is obtained from the
     * ManagedConnectionFactory. An application server can set a log writer specific to
     * this ManagedConnection to log/trace this instance using setLogWriter method.
     *
     * @param printWriter PrintWriter
     */
    public void setLogWriter(PrintWriter printWriter) {
        mLogWriter = printWriter;
    }

    /**
     * Gets the log writer for this ManagedConnection instance. The log writer is a
     * character output stream to which all logging and tracing messages for this
     * ManagedConnection instance will be printed. ConnectionManager manages the
     * association of output stream with the ManagedConnection instance based on the
     * connection pooling requirements. The Log writer associated with a ManagedConnection
     * instance can be one set as default from the ManagedConnectionFactory (that created
     * this connection) or one set specifically for this instance by the application
     * server.
     *
     * @return java.io.PrintWriter
     */
    public PrintWriter getLogWriter() {
        return mLogWriter;
    }

    /**
     * Distributes an event to all event listeners
     *
     * @param event the event to be distributed
     */
    private void distributeEvent(ConnectionEvent event) {
        int type = event.getId();

        if (sLog.isDebugEnabled()) {
            sLog.debug("Sending connection event: " + type);
        }

        ConnectionEventListener[] listeners;
        synchronized (this) {
            listeners = mConnectionEventListeners.
                toArray(new ConnectionEventListener[] {});
        }

        for (int i = 0; i < listeners.length; i++) {
            switch (type) {
                case ConnectionEvent.CONNECTION_CLOSED:
                    listeners[i].connectionClosed(event);
                    break;
                case ConnectionEvent.LOCAL_TRANSACTION_STARTED:
                    listeners[i].localTransactionStarted(event);
                    break;
                case ConnectionEvent.LOCAL_TRANSACTION_COMMITTED:
                    listeners[i].localTransactionCommitted(event);
                    break;
                case ConnectionEvent.LOCAL_TRANSACTION_ROLLEDBACK:
                    listeners[i].localTransactionRolledback(event);
                    break;
                case ConnectionEvent.CONNECTION_ERROR_OCCURRED:
                    listeners[i].connectionErrorOccurred(event);
                    break;
                default:
                    throw Exc.illarg(LOCALE.x("E167: Illegal eventType: {0}",
                        Integer.toString(type)));
            }
        }
    }

    /**
     * Returns true if producer pooling is used
     *
     * @return true if on
     */
    public boolean useProducerPooling() {
        return mManagedConnectionFactory.isProducerPoolingOn();
    }

    /**
     * getJSession
     *
     * @return JSession
     */
    public JSession getJSession() {
        if (mJSession == null) {
            throw Exc.rtexc(LOCALE.x("E166: Cannot obtain JSession: mSession is null"));
        }
        return mJSession;
    }
    
    /**
     * For diagnostics purposes: dumps the runtime state of this MC
     * 
     * @return human readable string
     */
    public String dumpMCInfo() {
        StringBuffer ret = new StringBuffer();
        ret.append("Class: " + getClass() + ";\n");
        ret.append("Lifetime: " + (System.currentTimeMillis() - mCreatedAt) + " ms;\n");
        if (mJSession != null) {
            ret.append("Exception count: " + mJSession.getExceptionCount() + "\n");
        }
        ret.append("Last successfully used at: " + mLastUsedSuccessfullyAt + " ("
            + (System.currentTimeMillis() - mLastUsedSuccessfullyAt) + " ms ago)");
        
        return ret.toString();
    }

    /**
     * @return the effective password for this managed connection
     */
    public String getPassword() {
        return mPassword;
    }

    /**
     * @return the effective password
     */
    public String getUserid() {
        return mUserid;
    }

    /**
     * @return interceptor chain (can be null)
     */
    public InterceptorChain getInterceptorChain() {
        return mInterceptorChain;
    }
}
