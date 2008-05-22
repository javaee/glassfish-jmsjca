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

import javax.jms.Connection;
import javax.jms.ConnectionConsumer;
import javax.jms.ConnectionMetaData;
import javax.jms.Destination;
import javax.jms.ExceptionListener;
import javax.jms.JMSException;
import javax.jms.ServerSessionPool;
import javax.jms.Session;
import javax.jms.Topic;

/**
 * A wrapper around a javax.jms.Connection; this wrapper is given out to the
 * application code. The Application will call methods in this wrapper; the
 * wrapper will delegate the calls to the JConnection.
 *
 * @author Frank Kieviet
 * @version $Revision: 1.6 $
 */
public class WConnection implements Connection {
    private static final Localizer LOCALE = Localizer.get();

    /**
     * The JConnection that owns this Wrapper
     */
    protected JConnection mMgr;

    /**
     * Constructor
     *
     * @param mgr JConnection
     */
    public WConnection(JConnection mgr) {
        mMgr = mgr;
    }

    /**
     * invokeOnClosed
     *
     * @throws javax.jms.IllegalStateException always
     */
    public void invokeOnClosed() throws javax.jms.IllegalStateException {
        throw Exc.illstate(LOCALE.x("E153: This {0} is closed", "connection"));
    }

    /**
     * Marks this wrapper as closed
     */
    public void setClosed() {
        mMgr = null;
    }

    /**
     * For testing only
     *
     * @return JConnection
     */
    public JConnection getJConnection() {
        return mMgr;
    }

    /**
     * createSession
     *
     * @param boolean0 boolean
     * @param int1 int
     * @return Session
     * @throws JMSException on failure
     */
    public Session createSession(boolean boolean0, int int1) throws JMSException {
        if (mMgr == null) {
            invokeOnClosed();
        }
        return mMgr.createSession(boolean0, int1);
    }

    /**
     * getClientID
     *
     * @return String
     * @throws JMSException on failure
     */
    public String getClientID() throws JMSException {
        if (mMgr == null) {
            invokeOnClosed();
        }
        return mMgr.getClientID();
    }

    /**
     * setClientID
     *
     * @param string String
     * @throws JMSException on failure
     */
    public void setClientID(String string) throws JMSException {
        if (mMgr == null) {
            invokeOnClosed();
        }
        mMgr.setClientID(string);
    }

    /**
     * getMetaData
     *
     * @return ConnectionMetaData
     * @throws JMSException on failure
     */
    public ConnectionMetaData getMetaData() throws JMSException {
        if (mMgr == null) {
            invokeOnClosed();
        }
        return mMgr.getMetaData();
    }

    /**
     * getExceptionListener
     *
     * @return ExceptionListener
     * @throws JMSException on failure
     */
    public ExceptionListener getExceptionListener() throws JMSException {
        if (mMgr == null) {
            invokeOnClosed();
        }
        return mMgr.getExceptionListener();
    }

    /**
     * setExceptionListener
     *
     * @param exceptionListener ExceptionListener
     * @throws JMSException on failure
     */
    public void setExceptionListener(ExceptionListener exceptionListener) throws
        JMSException {
        if (mMgr == null) {
            invokeOnClosed();
        }
        mMgr.setExceptionListener(exceptionListener);
    }

    /**
     * start
     *
     * @throws JMSException on failure
     */
    public void start() throws JMSException {
        if (mMgr == null) {
            invokeOnClosed();
        }
        mMgr.start();
    }

    /**
     * stop
     *
     * @throws JMSException on failure
     */
    public void stop() throws JMSException {
        if (mMgr == null) {
            invokeOnClosed();
        }
        mMgr.stop();
    }

    /**
     * createConnectionConsumer
     *
     * @param destination Destination
     * @param string String
     * @param serverSessionPool ServerSessionPool
     * @param int3 int
     * @return ConnectionConsumer
     * @throws JMSException on failure
     */
    public ConnectionConsumer createConnectionConsumer(Destination destination,
        String string, ServerSessionPool serverSessionPool, int int3) throws JMSException {
        if (mMgr == null) {
            invokeOnClosed();
        }
        return mMgr.createConnectionConsumer(destination, string, serverSessionPool, int3);
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
     * @throws JMSException on failure
     */
    public ConnectionConsumer createDurableConnectionConsumer(Topic topic, String string,
        String string2, ServerSessionPool serverSessionPool, int int4) throws JMSException {
        if (mMgr == null) {
            invokeOnClosed();
        }
        return createDurableConnectionConsumer(topic, string, string2, serverSessionPool, int4);
    }

    /**
     * close
     *
     * @throws JMSException on failure
     */
    public void close() throws JMSException {
        if (mMgr == null) {
            // allowed
        } else {
            mMgr.close();
        }
    }
}
