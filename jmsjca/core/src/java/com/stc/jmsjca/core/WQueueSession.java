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
 * $RCSfile: WQueueSession.java,v $
 * $Revision: 1.1.1.2 $
 * $Date: 2007-01-21 07:52:44 $
 *
 * Copyright 2003-2007 Sun Microsystems, Inc. All Rights Reserved.  
 */

package com.stc.jmsjca.core;

import javax.jms.JMSException;
import javax.jms.Queue;
import javax.jms.QueueReceiver;
import javax.jms.QueueSender;
import javax.jms.QueueSession;

/**
 * A wrapper around a javax.jms.Session; this wrapper is given out to the application
 * code. The Application will call methods in this wrapper; the wrapper will delegate the
 * calls to the JSession.
 *
 * @author Frank Kieviet
 * @version $Revision: 1.1.1.2 $
 */
public class WQueueSession extends WSession implements QueueSession {
    /**
     * WQueueSession
     *
     * @param mgr JSession
     */
    public WQueueSession(JSession mgr) {
        super(mgr);
    }

    /**
     * createReceiver
     *
     * @param queue Queue
     * @return QueueReceiver
     * @throws JMSException on failure
     */
    public QueueReceiver createReceiver(Queue queue) throws JMSException {
        checkClosed();
        checkTemporaryDestinationOwnership(queue);
        if (queue instanceof Unwrappable) {
            queue = (Queue) ((Unwrappable) queue).getWrappedObject();
        }
        try {
            return mMgr.createReceiver(queue, getConnection());
        } catch (JMSException e) {
            mMgr.exceptionOccurred(e);
            throw e;
        }
    }

    /**
     * createReceiver
     *
     * @param queue Queue
     * @param string String
     * @return QueueReceiver
     * @throws JMSException on failure
     */
    public QueueReceiver createReceiver(Queue queue, String string) throws JMSException {
        checkClosed();
        checkTemporaryDestinationOwnership(queue);
        if (queue instanceof Unwrappable) {
            queue = (Queue) ((Unwrappable) queue).getWrappedObject();
        }
        try {
            return mMgr.createReceiver(queue, string, getConnection());
        } catch (JMSException e) {
            mMgr.exceptionOccurred(e);
            throw e;
        }
    }

    /**
     * createSender
     *
     * @param queue Queue
     * @return QueueSender
     * @throws JMSException on failure
     */
    public QueueSender createSender(Queue queue) throws JMSException {
        checkClosed();
        if (queue instanceof Unwrappable) {
            queue = (Queue) ((Unwrappable) queue).getWrappedObject();
        }
        try {
            return mMgr.createSender(queue, getConnection());
        } catch (JMSException e) {
            mMgr.exceptionOccurred(e);
            throw e;
        }
    }
}
