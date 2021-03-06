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
import javax.jms.TemporaryQueue;

/**
 * A wrapper around a javax.jms.TemporaryQueue; this wrapper is given out to the 
 * application code.
 *
 * @author Frank Kieviet
 * @version $Revision: 1.7 $
 */
public class WTemporaryQueue implements TemporaryQueue, Unwrappable, LimitationJConnection {
    private TemporaryQueue mDelegate;
    private JConnection mConnection;
    
    /**
     * Constructor
     * 
     * @param delegate delegate
     * @param session
     * @param connection JConnection
     */
    public WTemporaryQueue(TemporaryQueue delegate, JConnection connection) {
        mDelegate = delegate;
        mConnection = connection;
    }

    /**
     * @see javax.jms.TemporaryQueue#delete()
     */
    public void delete() throws JMSException {
        mDelegate.delete();
        mConnection.removeTemporaryDestination(mDelegate);
    }

    /**
     * @see javax.jms.Queue#getQueueName()
     */
    public String getQueueName() throws JMSException {
        return mDelegate.getQueueName();
    }

    /**
     * @see com.stc.jmsjca.core.Unwrappable#getWrappedObject()
     */
    public Object getWrappedObject() {
        return mDelegate;
    }
    
    /**
     * @return JConnection
     */
    public JConnection getConnection() {
        return mConnection;
    }
}
