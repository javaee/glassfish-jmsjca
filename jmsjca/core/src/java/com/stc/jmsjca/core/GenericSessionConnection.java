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

import javax.jms.Connection;
import javax.jms.ConnectionMetaData;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Session;
import javax.transaction.xa.XAResource;

import java.util.Properties;

/**
 * Manages a connection/session for a generic JMS provider: no optimizations are done; no
 * dependencies are there outside of the JMS spec.
 *
 * @author Frank Kieviet
 * @version $Revision: 1.11 $
 */
public class GenericSessionConnection extends SessionConnection {
    private static final Localizer LOCALE = Localizer.get();
    private static Logger sLog = Logger.getLogger(GenericSessionConnection.class.getName());

    /**
     * mConnection
     */
    protected Connection mConnection;
    private Session mSession;
    private boolean mIsXA;
    private boolean mIsTransacted;
    private int mAcknowledgMode;
    private Class<?> mSessionClass;
    private XConnectionRequestInfo mDescr;
    private RAJMSResourceAdapter mRA;

    /**
     * mManagedConnection
     */
    protected XManagedConnection mMC;
    
    private Object mConFact;
    private RAJMSObjectFactory mObjFact;

    /**
     * Constructor
     *
     * @param connectionFactory Object
     * @param objfact RAJMSObjectFactory
     * @param ra RAJMSResourceAdapter
     * @param mc XManagedConnection
     * @param descr XConnectionRequestInfo
     * @param isXa boolean
     * @param isTransacted boolean
     * @param acknowledgmentMode int
     * @param sessionClass Class
     * @throws JMSException failure
     */
    public GenericSessionConnection(Object connectionFactory, RAJMSObjectFactory objfact,
        RAJMSResourceAdapter ra, XManagedConnection mc, XConnectionRequestInfo descr,
        boolean isXa, boolean isTransacted, int acknowledgmentMode, Class<?> sessionClass)
        throws JMSException {
        
        try {
            mConFact = connectionFactory;
            mObjFact = objfact;
            mRA = ra;
            mMC = mc;
            mDescr = descr;
            mSessionClass = sessionClass;
            mIsXA = isXa;
            mIsTransacted = isTransacted;
            mAcknowledgMode = acknowledgmentMode;

            mConnection = mObjFact.createConnection(mConFact, mDescr.getDomain(isXa),
                null, mRA, mc.getUserid(), mc.getPassword());
            if (mDescr.getClientID() != null) {
                mObjFact.setClientID(mConnection, mDescr.getClientID());
            }
            mSession = mObjFact.createSession(mConnection, isXa, sessionClass, mRA, null,
                isTransacted, acknowledgmentMode);

            sLog.debug("GenericSessionConnection created");
        } catch (JMSException e) {
            Exc.checkLinkedException(e);
            if (sLog.isDebugEnabled()) {
                sLog.debug("GenericSessionCreation failed: " + e, e);
            }
            throw e;
        }
    }

    /**
     * Returns the JMS session so that the application can delegate calls to it.
     *
     * @return Session
     * @throws JMSException failure
     */
    @Override
    public Session getJmsSession() throws JMSException {
        return mObjFact.getNonXASession(mSession, mIsXA, mSessionClass);
    }

    /**
     * start (javax.jms.Connection.start())
     *
     * @throws JMSException failure
     */
    @Override
    public void start() throws JMSException {
        mConnection.start();
    }

    /**
     * stop (javax.jms.Connection.stop())
     *
     * @throws JMSException failure
     */
    @Override
    public void stop() throws JMSException {
        if (mConnection != null) {
            mConnection.stop();
        }
    }

    /**
     * Returns the XAResource
     *
     * @return XAResource
     * @throws JMSException failure
     */
    @Override
    public XAResource getXAResource() throws JMSException {
        if (!mIsXA) {
            throw Exc.jmsExc(LOCALE.x("E127: Logic fault: cannot return XAResource from non-XA session"));
        }
        return mObjFact.getXAResource(true, mSession);
    }

    /**
     * Destroys the connection/session. Is called by JSession when the JSession has no use
     * for this anymore.
     *
     * @throws JMSException failure
     */
    @Override
    public void destroy() throws JMSException {
        sLog.debug("Destroying GenericSessionConnection");
        
        Exception sessionException = null;
        Exception connectionException = null;
        
        // Closing the session is necessary for MQSeries: for that JMS server it's
        // not sufficient to just close the connection.
        if (mSession != null) {
            try {
                mSession.close();
                mSession = null;
            } catch (Exception e) {
                Exc.checkLinkedException(e);
                sessionException = e;
                if (sLog.isDebugEnabled()) {
                    sLog.debug("Session could not be closed: " + e, e);
                }
            }
        }
        
        // Close connection
        if (mConnection != null) {
            try {
                mConnection.close();
                mConnection = null;
            } catch (Exception e) {
                Exc.checkLinkedException(e);
                connectionException = e;
                if (sLog.isDebugEnabled()) {
                    sLog.debug("Connection could not be closed: " + e, e);
                }
            }
        }
        
        // Handle exceptions
        if (sessionException != null || connectionException != null) {
            // Propagate one exception, preference for session
            Exception trace = sessionException != null ? sessionException : connectionException;
            
            // Record both exceptions
            String msgExc = "session exception=[" + sessionException + "]; connection exception=[" + connectionException + "]";
            
            throw Exc.jmsExc(LOCALE.x("E094: This {0} could not be closed properly: {1}", 
                this.getClass().getName(), msgExc), trace);
        }
    }

    /**
     * getConnectionMetaData
     *
     * @return ConnectionMetaData
     * @throws JMSException on failure
     */
    @Override
    public ConnectionMetaData getConnectionMetaData() throws JMSException {
        return mConnection.getMetaData();
    }

    /**
     * setClientID
     *
     * @param clientID String
     * @throws JMSException on failure
     */
    @Override
    public void setClientID(String clientID) throws JMSException {
        mConnection.setClientID(clientID);
    }

    /**
     * getTransacted
     *
     * @return boolean true if transacted
     */
    @Override
    public boolean getTransacted() {
        return mIsTransacted;
    }

    /**
     * getAcknowledgeMode
     *
     * @return int ack mode
     */
    @Override
    public int getAcknowledgeMode() {
        return mAcknowledgMode;
    }

    /**
     * isXA
     *
     * @return boolean
     */
    @Override
    public boolean isXA() {
        return mIsXA;
    }

    /**
     * @see com.stc.jmsjca.core.SessionConnection#checkGeneric(javax.jms.Destination)
     */
    @Override
    public Destination checkGeneric(Destination d) throws JMSException {
        return mObjFact.checkGeneric(d);
    }

    /**
     * Getter for objFact
     *
     * @return RAJMSObjectFactory
     */
    public RAJMSObjectFactory getObjFact() {
        return mObjFact;
    }

    /**
     * Getter for rA
     *
     * @return RAJMSResourceAdapter
     */
    public RAJMSResourceAdapter getRA() {
        return mRA;
    }

    /**
     * @see com.stc.jmsjca.core.SessionConnection#createDestination(boolean, java.lang.String, java.util.Properties)
     */
    @Override
    public Destination createDestination(boolean isTopic, String name, Properties options) throws JMSException {
        return mObjFact.createDestination(mSession, mIsXA, isTopic, null, mMC.getManagedConnectionFactory(), 
            mRA, name, options, mSessionClass);
    }
}

