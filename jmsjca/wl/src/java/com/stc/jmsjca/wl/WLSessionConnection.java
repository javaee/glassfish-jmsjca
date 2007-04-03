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

package com.stc.jmsjca.wl;

import com.stc.jmsjca.core.DestinationCacheEntry;
import com.stc.jmsjca.core.GenericSessionConnection;
import com.stc.jmsjca.core.RAJMSObjectFactory;
import com.stc.jmsjca.core.RAJMSResourceAdapter;
import com.stc.jmsjca.core.XConnectionRequestInfo;
import com.stc.jmsjca.core.XManagedConnection;

import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Queue;
import javax.jms.Topic;

/**
 * Provides some WebLogic specific features:
 * - looks up destinations in JNDI
 * - pools destinations
 *
 * @author Frank Kieviet
 * @version $Revision: 1.5 $
 */
public class WLSessionConnection extends GenericSessionConnection {
//    private static Logger sLog = Logger.getLogger(WLSessionConnection.class);

    /**
     * Constructor
     *
     * @param connectionFactory Object
     * @param objfact RAJMSObjectFactory
     * @param ra RAJMSResourceAdapter
     * @param managedConnection XManagedConnection
     * @param descr XConnectionRequestInfo
     * @param isXa boolean
     * @param isTransacted boolean
     * @param acknowledgmentMode int
     * @param sessionClass Class
     * @throws JMSException failure
     */
    public WLSessionConnection(Object connectionFactory, RAJMSObjectFactory objfact,
        RAJMSResourceAdapter ra, XManagedConnection managedConnection,
        XConnectionRequestInfo descr, boolean isXa,
        boolean isTransacted, int acknowledgmentMode, Class sessionClass)
        throws JMSException {

        super(connectionFactory, objfact, ra, managedConnection, descr, isXa,
            isTransacted, acknowledgmentMode, sessionClass);
    }

    /**
     * @see com.stc.jmsjca.core.GenericSessionConnection#createQueue(java.lang.String)
     */
    public Queue createQueue(String name) throws JMSException {
        DestinationCacheEntry d = mMC.getManagedConnectionFactory().getQueueCache().get(name);
        synchronized (d) {
            if (d.get() == null) {
                Destination dest = getObjFact().createDestination(getJmsSession(), false,
                    false, null, mMC.getManagedConnectionFactory(), getRA(), name);
                d.set(dest);
            }
        }
        return (Queue) d.get();
    }

    /**
     * @see com.stc.jmsjca.core.GenericSessionConnection#createTopic(java.lang.String)
     */
    public Topic createTopic(String name) throws JMSException {
        DestinationCacheEntry d = mMC.getManagedConnectionFactory().getTopicCache().get(name);
        synchronized (d) {
            if (d.get() == null) {
                Destination dest = getObjFact().createDestination(getJmsSession(), false,
                    false, null, mMC.getManagedConnectionFactory(), getRA(), name);
                d.set(dest);
            }
        }
        return (Topic) d.get();
    }
}
