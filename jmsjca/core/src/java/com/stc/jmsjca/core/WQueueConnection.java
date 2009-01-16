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

import javax.jms.ConnectionConsumer;
import javax.jms.JMSException;
import javax.jms.Queue;
import javax.jms.QueueConnection;
import javax.jms.QueueSession;
import javax.jms.ServerSessionPool;

/**
 * A wrapper around a javax.jms.MessageConsumer; this wrapper is given out to the
 * application code. The Application will call methods in this wrapper; the
 * wrapper will delegate the calls to the physical connection. Some of the
 * calls will be treated specially, such as the close() method.
 *
 * @author Frank Kieviet
 * @version $Revision: 1.7 $
 */
public class WQueueConnection extends WConnection implements QueueConnection {

    /**
     * WQueueConnection
     *
     * @param mgr JConnection
     */
    public WQueueConnection(JConnection mgr) {
        super(mgr);
    }

    /**
     * createQueueSession
     *
     * @param boolean0 boolean
     * @param int1 int
     * @return QueueSession
     * @throws JMSException on failure
     */
    public QueueSession createQueueSession(boolean boolean0, int int1) throws JMSException {
        if (mMgr == null) {
            invokeOnClosed();
        }
        return mMgr.createQueueSession(boolean0, int1);
    }

    /**
     * createConnectionConsumer
     *
     * @param queue Queue
     * @param string String
     * @param serverSessionPool ServerSessionPool
     * @param int3 int
     * @return ConnectionConsumer
     * @throws JMSException always
     */
    public ConnectionConsumer createConnectionConsumer(Queue queue, String string,
        ServerSessionPool serverSessionPool, int int3) throws JMSException {
        if (mMgr == null) {
            invokeOnClosed();
        }
        return mMgr.createConnectionConsumer(queue, string, serverSessionPool, int3);
    }
}
