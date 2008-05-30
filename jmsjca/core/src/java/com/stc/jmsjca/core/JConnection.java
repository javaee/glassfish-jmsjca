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
import com.stc.jmsjca.util.Logger;
import com.stc.jmsjca.util.NoProxyWrapper;

import javax.jms.Connection;
import javax.jms.ConnectionConsumer;
import javax.jms.ConnectionMetaData;
import javax.jms.Destination;
import javax.jms.ExceptionListener;
import javax.jms.JMSException;
import javax.jms.Queue;
import javax.jms.QueueConnection;
import javax.jms.QueueSession;
import javax.jms.ServerSessionPool;
import javax.jms.Session;
import javax.jms.TemporaryQueue;
import javax.jms.TemporaryTopic;
import javax.jms.Topic;
import javax.jms.TopicConnection;
import javax.jms.TopicSession;
import javax.transaction.Synchronization;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Manages a javax.jms.Connection handed out to the application. Such a connection does
 * NOT represent a true javax.jms.Connection; rather it is used as a factory: if the
 * application creates a Session, this empty holder object will obtain a managed
 * connection. A managed connection represents a Session object.
 *
 * @author Frank Kieviet
 * @version $Revision: 1.7 $
 */
public class JConnection extends NoProxyWrapper implements QueueConnection, TopicConnection, Connection {
    private static Logger sLog = Logger.getLogger(JConnection.class);
    private XManagedConnectionFactory mManagedConnectionFactory;
    private javax.resource.spi.ConnectionManager mConnectionManager;
    private Class mConnectionClass;
    private String mUsername;
    private String mPassword;
    private boolean mStarted;
    private List mSessions = new ArrayList(); // type: WSession
    private String mClientID;
    private List mTemporaryDestinations = new ArrayList(); // type: temporary destination

    private static final Localizer LOCALE = Localizer.get();

    /**
     * Constructor
     *
     * @param managedConnectionFactory XManagedConnectionFactory
     * @param connectionManager ConnectionManager
     * @param connectionClass Class
     * @param username String
     * @param password String
     */
    public JConnection(XManagedConnectionFactory managedConnectionFactory,
        javax.resource.spi.ConnectionManager connectionManager,
        Class connectionClass, String username, String password) {

        mManagedConnectionFactory = managedConnectionFactory;
        mConnectionManager = connectionManager;
        mConnectionClass = connectionClass;
        mUsername = username;
        mPassword = password;

        init(mConnectionClass, "");
    }

    /**
     * Called when the application calls createQueueSession(), createSession(), or
     * createTopicSession().
     *
     * @param sessionClass Class
     * @param transacted boolean
     * @param acknowledgeMode int
     * @throws JMSException failure
     * @return Session
     */
    private Session createSessionByApplication(Class sessionClass, boolean transacted,
        int acknowledgeMode) throws JMSException {
        int orgAckmode = acknowledgeMode;

        // Ensure ackmode is set correctly
        if (transacted) {
            acknowledgeMode = Session.SESSION_TRANSACTED;
            orgAckmode = Session.SESSION_TRANSACTED;
        } else {
            // Running within an appserver, the transacted attribute should be ignored
            // and should be assumed to be transacted.
            if (mManagedConnectionFactory.getOptionIgnoreNonTx()) {
                transacted = true;
                acknowledgeMode = Session.SESSION_TRANSACTED;
            } else if (acknowledgeMode == 0) {
                // Following "correction" is done in RI as well
                acknowledgeMode = Session.AUTO_ACKNOWLEDGE;
                orgAckmode = Session.AUTO_ACKNOWLEDGE;
            }
        }

        IWSession w = null;
        try {
            // Get password if not overridden in createConnection(string,string)
            String[] useridpassword = mManagedConnectionFactory.
                getUserIdAndPasswordAndUrl(mUsername, mPassword);
            
            XConnectionRequestInfo descr = new XConnectionRequestInfo(mConnectionClass,
                sessionClass, useridpassword[0], useridpassword[1], useridpassword[2],
                mClientID, transacted, acknowledgeMode);
            Object obj = mConnectionManager.
                allocateConnection(mManagedConnectionFactory, descr);
            w = (IWSession) obj;
            w.getReference().setConnection(this);
            w.getReference().getJSession().setSpecifiedAcknowledgeMode(orgAckmode);
            if (mStarted) {
                // Exception may happen here for some JMS servers if they are disconnected
                w.getReference().getJSession().start();
            }
            mSessions.add(w);

            return w;
        } catch (Exception ex) {
            // Avoid connection leaks
            if (w != null) {
                try {
                    w.close();
                } catch (Throwable ignore) {
                    // ignore
                }
            }

            throw Exc.jmsExc(LOCALE.x("E034: Could not create session {0}: {1}", 
                sessionClass.getName(), ex), ex);
        }
    }
    
    /**
     * Registers a temporary destination so that it can be deleted upon calling close()
     * on the connection
     * 
     * @param dest temp dest
     * @throws JMSException on logic fault
     */
    public void addTemporaryDestination(Destination dest) throws JMSException {
        if (dest instanceof TemporaryQueue || dest instanceof TemporaryTopic) {
            mTemporaryDestinations.add(dest);
        } else {
            throw Exc.jmsExc(LOCALE.x("E128: Destination [{0}] is not a temporary destination.", dest));
        }
    }

    /**
     * Called when a temporary destination is deleted
     *
     * @param dest TemporaryTopic
     */
    public void removeTemporaryDestination(Destination dest) {
        boolean removed = mTemporaryDestinations.remove(dest);
        if (!removed) {
            sLog.warn(LOCALE.x("E035: Unexpected: ''.delete()'' was called on a "
                + "temporary destination that "
                + "was not known to the connection. Perhaps the temporary destination was "
                + "already deleted. The temporary destination is: [{0}]", dest));
        }
    }
    
    private void deferTempDestDeletion() {
        TxMgr txmgr = mManagedConnectionFactory.getTxMgr();
        try {
            txmgr.register(new Synchronization() {
                public void beforeCompletion() {
                }
                
                public void afterCompletion(int status) {
                    if (sLog.isDebugEnabled()) {
                        sLog.debug("afterCompletion(" + status
                            + "); now invoking deferred "
                            + "deletions of temporary destinations");
                    }
                    deleteTemporaryDestinations();
                }
            });
        } catch (Exception e) {
            sLog.warn(LOCALE.x("E036: Could not defer deletion of temporary destinations: {0}", e), e);
        }
    }

    private boolean isContainerTransactionInProgress() {
        if (mManagedConnectionFactory.getOptionBypassRA()
            || mManagedConnectionFactory.getOptionClientContainer()) {
            return false;
        }
        
        TxMgr txmgr = mManagedConnectionFactory.getTxMgr();
        if (txmgr == null) {
            return false;
        } else {
            return txmgr.isInTransaction();
        }
    }

    /**
     * Deletes all temporary destinations 
     */
    public void deleteTemporaryDestinations() {
        for (Iterator iter = mTemporaryDestinations.iterator(); iter.hasNext();) {
            Destination dest = (Destination) iter.next();
            try {
                if (dest instanceof TemporaryQueue) {
                    ((TemporaryQueue) dest).delete();
                } else {
                    ((TemporaryTopic) dest).delete();
                }
            } catch (JMSException e) {
                sLog.warn(LOCALE.x("E037: Temporary destination {0} could not be " 
                    + "deleted: {1}", dest, e), e);
            }
        }
        mTemporaryDestinations.clear();
    }
 

    // INTERCEPTED METHODS

    /**
     * Special: does NOT close the physical connection, but notifies the managed
     * connection that the connection is closed and hence that the connection can be
     * returned to the pool.
     *
     * @throws JMSException on failure
     */
    public void close() throws JMSException {
        // Close all sessions (application level-close)
        while (!mSessions.isEmpty()) {
            Session s = (Session) mSessions.get(0);
            s.close();
        }
        
        // Delete all temporary destinations now unless there is a transaction in 
        // progress, in which case it needs to be deferred until the transaction has 
        // been committed.
        if (!mTemporaryDestinations.isEmpty()) {
            if (isContainerTransactionInProgress()) {
                deferTempDestDeletion();
            } else {
                deleteTemporaryDestinations();
            }
        }

        // Mark this connection as closed (calls on any other method than close will
        // cause an exception)
        createNewWrapper();
    }

    /**
     * Mandated by JMS interface
     *
     * @param transacted boolean
     * @param acknowledgeMode int
     * @return javax.jms.QueueSession
     * @throws JMSException on failure
     */
    public QueueSession createQueueSession(boolean transacted, int acknowledgeMode)
        throws JMSException {
        return (QueueSession) createSessionByApplication(javax.jms.QueueSession.class,
            transacted, acknowledgeMode);
    }

    /**
     * Mandated by JMS interface
     *
     * @param transacted boolean
     * @param acknowledgeMode int
     * @return javax.jms.TopicSession
     * @throws JMSException on failure
     */
    public TopicSession createTopicSession(boolean transacted, int acknowledgeMode)
        throws JMSException {
        return (TopicSession) createSessionByApplication(javax.jms.TopicSession.class,
            transacted, acknowledgeMode);
    }

    /**
     * Mandated by JMS interface
     *
     * @param transacted boolean
     * @param acknowledgeMode int
     * @return javax.jms.Session
     * @throws JMSException on failure
     */
    public Session createSession(boolean transacted, int acknowledgeMode)
        throws JMSException {
        return (Session) createSessionByApplication(javax.jms.Session.class, transacted,
            acknowledgeMode);
    }

    /**
     * getClientID
     *
     * @return String
     */
    public String getClientID() {
        return mClientID;
    }

    /**
     * setClientID
     *
     * @param clientID String
     * @throws JMSException on failure
     */
    public void setClientID(String clientID) throws JMSException {
        if (clientID == null || clientID.length() == 0) {
            throw new javax.jms.InvalidClientIDException(
                LOCALE.x("E121: Client ID should be a non-empty string").toString());
        }
        if (mClientID != null) {
            throw new javax.jms.IllegalStateException(
                LOCALE.x("E122: The client ID already configured").toString());
        }
        mClientID = clientID;
        for (Iterator it = mSessions.iterator(); it.hasNext();/*-*/) {
            IWSession s = (IWSession) it.next();
            JSession j = s.getReference().getJSession();
            j.setClientID(clientID);
        }
    }

    /**
     * getMetaData
     *
     * @return ConnectionMetaData
     * @throws JMSException on failure
     */
    public ConnectionMetaData getMetaData() throws JMSException {
        ConnectionMetaData ret;
        if (!mSessions.isEmpty()) {
            IWSession s = (IWSession) mSessions.get(0);
            ret = s.getReference().getJSession().getConnectionMetaData();
        } else {
            IWSession s = (IWSession) createSessionByApplication(javax.jms.QueueSession.class, true,
                Session.SESSION_TRANSACTED);
            ret = s.getReference().getJSession().getConnectionMetaData();
            s.close();
        }

        return ret;
    }

    /**
     * getExceptionListener
     *
     * @return ExceptionListener
     */
    public ExceptionListener getExceptionListener() {
        return null;
    }

    /**
     * setExceptionListener
     *
     * @param exceptionListener ExceptionListener
     * @throws JMSException on failure
     */
    public void setExceptionListener(ExceptionListener exceptionListener) throws
        JMSException {
        throw Exc.jmsExc(LOCALE.x("E129: ExceptionListeners cannot be set in a JCA 1.5 connection"));
    }

    /**
     * starts all sessions and automatically start sessions created from now on
     *
     * @throws JMSException on error
     */
    public void start() throws JMSException {
        mStarted = true;
        for (Iterator iter = mSessions.iterator(); iter.hasNext();/*-*/) {
            IWSession s = (IWSession) iter.next();
            s.getReference().getJSession().start();
        }
    }

    /**
     * Stops all sessions
     *
     * @throws JMSException on error
     */
    public void stop() throws JMSException {
        mStarted = false;
        for (Iterator iter = mSessions.iterator(); iter.hasNext();/*-*/) {
            IWSession s = (IWSession) iter.next();
            s.getReference().getJSession().stop();
        }
    }

    /**
     * createConnectionConsumer
     *
     * @param destination Destination
     * @param string String
     * @param serverSessionPool ServerSessionPool
     * @param int3 int
     * @return ConnectionConsumer
     * @throws JMSException Always: illegal
     */
    public ConnectionConsumer createConnectionConsumer(Destination destination,
        String string, ServerSessionPool serverSessionPool, int int3) throws JMSException {
        throw Exc.jmsExc(LOCALE.x("E130: Connection consumers cannot be used with a JCA 1.5 connection"));
    }

    /**
     * createDurableConnectionConsumer
     *
     * @param topic Topic
     * @param string String
     * @param string2 String
     * @param serverSessionPool ServerSessionPool
     * @param int4 int
     * @return ConnectionConsumer
     * @throws JMSException Always: illegal
     */
    public ConnectionConsumer createDurableConnectionConsumer(Topic topic, String string,
        String string2, ServerSessionPool serverSessionPool, int int4) throws JMSException {
        throw Exc.jmsExc(LOCALE.x("E130: Connection consumers cannot be used with a JCA 1.5 connection"));
    }

    /**
     * createConnectionConsumer
     *
     * @param queue Queue
     * @param string String
     * @param serverSessionPool ServerSessionPool
     * @param int3 int
     * @return ConnectionConsumer
     * @throws JMSException Always: illegal
     */
    public ConnectionConsumer createConnectionConsumer(Queue queue, String string,
        ServerSessionPool serverSessionPool, int int3) throws JMSException {
        throw Exc.jmsExc(LOCALE.x("E130: Connection consumers cannot be used with a JCA 1.5 connection"));
    }

    /**
     * createConnectionConsumer
     *
     * @param topic Topic
     * @param string String
     * @param serverSessionPool ServerSessionPool
     * @param int3 int
     * @throws JMSException Always: illegal
     * @return ConnectionConsumer
     */
    public ConnectionConsumer createConnectionConsumer(Topic topic, String string,
        ServerSessionPool serverSessionPool, int int3) throws JMSException {
        throw Exc.jmsExc(LOCALE.x("E130: Connection consumers cannot be used with a JCA 1.5 connection"));
    }

    /**
     * Called when a session is closed by the application, i.e. close() on the session was
     * called.
     *
     * @param s session
     */
    public void notifyWSessionClosedByApplication(Object s) {
    	for(Iterator iter = mSessions.iterator(); iter.hasNext();){
    		if(((IWSession)iter.next()).getReference() == s){
    			iter.remove();
    			break;
    		}
    	}
        //mSessions.remove(s);
    }

    /**
     * Creates a new wrapper and invalidates any existing current wrapper.
     */
    public void createNewWrapper() {
        if (getWrapper() != null) {
            ((WConnection) getWrapper()).setClosed();
        }

        if (getItfClass() == javax.jms.Connection.class) {
            setWrapper(new WConnection(this));
        } else if (getItfClass() == javax.jms.QueueConnection.class) {
            setWrapper(new WQueueConnection(this));
        } else if (getItfClass() == javax.jms.TopicConnection.class) {
            setWrapper(new WTopicConnection(this));
        } else {
            throw Exc.rtexc(LOCALE.x("E131: Unknown class: {0}", getItfClass()));
        }
    }

    /**
     * physicalClose
     */
    public void physicalClose() {
        throw new IllegalStateException(LOCALE.x("E132: Invalid call").toString());
    }
}
