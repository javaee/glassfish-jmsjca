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
 * $RCSfile: GenericSessionConnection.java,v $
 * $Revision: 1.3 $
 * $Date: 2007-01-21 17:51:40 $
 *
 * Copyright 2003-2007 Sun Microsystems, Inc. All Rights Reserved.  
 */

package com.stc.jmsjca.core;

import javax.jms.Connection;
import javax.jms.ConnectionMetaData;
import javax.jms.JMSException;
import javax.jms.Queue;
import javax.jms.Session;
import javax.jms.Topic;
import javax.transaction.xa.XAResource;

/**
 * Manages a connection/session for a generic JMS provider: no optimizations are done; no
 * dependencies are there outside of the JMS spec.
 *
 * @author Frank Kieviet
 * @version $Revision: 1.3 $
 */
public class GenericSessionConnection extends SessionConnection {
//    private static Logger sLog = Logger.getLogger(GenericSessionConnection.class);

    /**
     * mConnection
     */
    protected Connection mConnection;
    private Session mSession;
    private boolean mIsXA;
    private boolean mIsTransacted;
    private int mAcknowledgMode;
    private Class mSessionClass;
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
        RAJMSResourceAdapter ra, XManagedConnection mc,
        XConnectionRequestInfo descr, boolean isXa,
        boolean isTransacted, int acknowledgmentMode, Class sessionClass)
        throws JMSException {

        mConFact = connectionFactory;
        mObjFact = objfact;
        mRA = ra;
        mMC = mc;
        mDescr = descr;
        mSessionClass = sessionClass;
        mIsXA = isXa;
        mIsTransacted = isTransacted;
        mAcknowledgMode = acknowledgmentMode;

        mConnection = mObjFact.createConnection(mConFact, mDescr.getDomain(isXa), null,
            mRA, mc.getUserid(), mc.getPassword());
        if (mDescr.getClientID() != null) {
            mObjFact.setClientID(mConnection, mDescr.getClientID());
        }
        mSession = mObjFact.createSession(mConnection, isXa, sessionClass, mRA, null, isTransacted,
            acknowledgmentMode);
    }

    /**
     * Returns the JMS session so that the application can delegate calls to it.
     *
     * @return Session
     * @throws JMSException failure
     */
    public Session getJmsSession() throws JMSException {
        return mObjFact.getNonXASession(mSession, mIsXA, mSessionClass);
    }

    /**
     * start (javax.jms.Connection.start())
     *
     * @throws JMSException failure
     */
    public void start() throws JMSException {
        mConnection.start();
    }

    /**
     * stop (javax.jms.Connection.stop())
     *
     * @throws JMSException failure
     */
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
    public XAResource getXAResource() throws JMSException {
        if (!mIsXA) {
            throw new JMSException("Logic fault: cannot return XAResource from non-XA session");
        }
        return mObjFact.getXAResource(true, mSession);
    }

    /**
     * Destroys the connection/session. Is called by JSession when the JSession has no use
     * for this anymore.
     *
     * @throws JMSException failure
     */
    public void destroy() throws JMSException {
        if (mConnection != null) {
            mConnection.close();
        }
        mConnection = null;
    }

    /**
     * getConnectionMetaData
     *
     * @return ConnectionMetaData
     * @throws JMSException on failure
     */
    public ConnectionMetaData getConnectionMetaData() throws JMSException {
        return mConnection.getMetaData();
    }

    /**
     * setClientID
     *
     * @param clientID String
     * @throws JMSException on failure
     */
    public void setClientID(String clientID) throws JMSException {
        mConnection.setClientID(clientID);
    }

    /**
     * getTransacted
     *
     * @return boolean true if transacted
     */
    public boolean getTransacted() {
        return mIsTransacted;
    }

    /**
     * getAcknowledgeMode
     *
     * @return int ack mode
     */
    public int getAcknowledgeMode() {
        return mAcknowledgMode;
    }

    /**
     * isXA
     *
     * @return boolean
     */
    public boolean isXA() {
        return mIsXA;
    }

    /**
     * @see com.stc.jmsjca.core.SessionConnection#createQueue(java.lang.String)
     */
    public Queue createQueue(String name) throws JMSException {
        return mObjFact.getNonXASession(mSession, mIsXA, mSessionClass).createQueue(name);
    }

    /**
     * @see com.stc.jmsjca.core.SessionConnection#createTopic(java.lang.String)
     */
    public Topic createTopic(String name) throws JMSException {
        return mObjFact.getNonXASession(mSession, mIsXA, mSessionClass).createTopic(name);
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
}

