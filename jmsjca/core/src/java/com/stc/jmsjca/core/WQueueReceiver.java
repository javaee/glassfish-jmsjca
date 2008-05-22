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

import javax.jms.JMSException;
import javax.jms.Queue;
import javax.jms.QueueReceiver;

/**
 * A wrapper around a javax.jms.MessageConsumer; this wrapper is given out to the
 * application code. The Application will call methods in this wrapper; the
 * wrapper will delegate the calls to the physical connection. Some of the
 * calls will be treated specially, such as the close() method.
 *
 * @author Frank Kieviet
 * @version $Revision: 1.6 $
 */
public class WQueueReceiver extends WMessageConsumer implements QueueReceiver {
    private QueueReceiver mDelegate;

    /**
     * Constructor
     *
     * @param mgr JConsumer
     * @param mc QueueReceiver
     */
    public WQueueReceiver(JConsumer mgr, QueueReceiver mc) {
        super(mgr, mc);
        mDelegate = mc;
    }

    /**
     * getQueue
     *
     * @return Queue
     * @throws JMSException on failure
     */
    public Queue getQueue() throws JMSException {
        if (mMgr == null) {
            invokeOnClosed();
        }
        try {
            Queue ret = mDelegate.getQueue();
            ret = (Queue) mMgr.getSession().wrap(ret, mMgr.getConnection());
            return ret;
        } catch (JMSException e) {
            mMgr.exceptionOccurred(e);
            throw e;
        }
    }

    /**
     * close
     *
     * @throws JMSException on failure
     */
    public void close() throws JMSException {
        super.close();
        mDelegate = null;
    }
}
