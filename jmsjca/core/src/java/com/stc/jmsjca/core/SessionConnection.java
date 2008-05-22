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

import javax.jms.ConnectionMetaData;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Queue;
import javax.jms.Session;
import javax.jms.Topic;
import javax.transaction.xa.XAResource;

/**
 * A provider specific class that takes care of interfacing with the JMS Server or JMS
 * Runtime client. It is created and managed by JSession. Originally it is used to solve
 * the problem that a JSession can change type after creation: a session is typically
 * created as transacted, but may change to XA when the application server asks for an
 * XAResource. However, in the current implementation it is assumed that the connection is
 * always XA, unless specifically indicated that the connector is not used within a
 * container and hence does not use XA.
 *
 * @author Frank Kieviet
 * @version $Revision: 1.6 $
 */
public abstract class SessionConnection {
//    private static Logger sLog = Logger.getLogger(SessionConnection.class);

    /**
     * Returns the JMS session so that the application can delegate calls to it.
     *
     * @return Session
     * @throws JMSException failure 
     */
    public abstract Session getJmsSession() throws JMSException;

    /**
     * start (javax.jms.Connection.start())
     *
     * @throws JMSException failure
     */
    public abstract void start() throws JMSException;

    /**
     * stop (javax.jms.Connection.stop())
     *
     * @throws JMSException failure
     */
    public abstract void stop() throws JMSException;

    /**
     * Returns the XAResource
     *
     * @return XAResource
     * @throws JMSException failure
     */
    public abstract XAResource getXAResource() throws JMSException;

    /**
     * Destroys the connection/session. Is called by JSession when the JSession has no use
     * for this anymore.
     *
     * @throws JMSException failure
     */
    public abstract void destroy() throws JMSException;

    /**
     * getConnectionMetaData
     *
     * @throws JMSException on failure
     * @return ConnectionMetaData
     */
    public abstract ConnectionMetaData getConnectionMetaData() throws JMSException;

    /**
     * setClientID
     *
     * @param clientID String
     * @throws JMSException on failure
     */
    public abstract void setClientID(String clientID) throws JMSException;

    /**
     * Returns true if the session is transacted
     *
     * @return boolean true if transacted
     */
    public abstract boolean getTransacted();

    /**
     * getAcknowledgeMode
     *
     * @return boolean
     */
    public abstract int getAcknowledgeMode();

    /**
     * isXA
     *
     * @return boolean
     */
    public abstract boolean isXA();

    /**
     * Creates a destination; used for interceptors such as WebLogic
     * 
     * @param name destination name
     * @return destination
     * @throws JMSException on failure
     */
    public abstract Queue createQueue(String name) throws JMSException;

    /**
     * Creates a destination; used for interceptors such as WebLogic
     * 
     * @param name destination name
     * @return destination
     * @throws JMSException on failure
     */
    public abstract Topic createTopic(String name) throws JMSException;

    /**
     * Creates a JMS client specific destination based on an administrative object
     * 
     * @param dest administrative object
     * @return JMS client specific destination
     * @throws JMSException propagated
     */
    public abstract Destination createDestination(AdminDestination dest) throws JMSException;
    
    /**
     * Converts optionally from a genericra destination to an admin destination
     * 
     * @param d destination to inspect
     * @return admin destination or same destination
     * @throws JMSException on conversion failure
     */
    public abstract Destination checkGeneric(Destination d) throws JMSException;
}
