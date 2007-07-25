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

package com.stc.jmsjca.jndi;

import com.stc.jmsjca.core.AdminDestination;
import com.stc.jmsjca.core.AdminQueue;
import com.stc.jmsjca.core.AdminTopic;
import com.stc.jmsjca.core.DestinationCache;
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
 * Provides some JNDI specific features:
 * - looks up destinations in JNDI
 * - pools destinations
 *
 * @author Frank Kieviet
 * @version $Revision: 1.2 $
 */
public class JNDISessionConnection extends GenericSessionConnection {

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
    public JNDISessionConnection(Object connectionFactory, RAJMSObjectFactory objfact,
        RAJMSResourceAdapter ra, XManagedConnection managedConnection,
        XConnectionRequestInfo descr, boolean isXa,
        boolean isTransacted, int acknowledgmentMode, Class sessionClass)
        throws JMSException {

        super(connectionFactory, objfact, ra, managedConnection, descr, isXa,
            isTransacted, acknowledgmentMode, sessionClass);
    }

    /**
     * @see com.stc.jmsjca.core.GenericSessionConnection#createDestination(com.stc.jmsjca.core.AdminDestination)
     */
    public Destination createDestination(AdminDestination dest) throws JMSException {
        RAJNDIObjectFactory o = (RAJNDIObjectFactory) getObjFact();
        Destination ret = null;
        String name = dest.getName();
        if (!name.startsWith(RAJNDIObjectFactory.JNDI_PREFIX)) {
            // Not a JNDI name
            if (dest instanceof AdminQueue) {
                ret = super.createQueue(name);
            } else {
                ret = super.createTopic(name);
            }
        } else {
            // Lookup JNDI name in cache
            name = name.substring(RAJNDIObjectFactory.JNDI_PREFIX.length());
            DestinationCache cache = dest instanceof AdminQueue 
            ? mMC.getManagedConnectionFactory().getQueueCache() 
                : mMC.getManagedConnectionFactory().getTopicCache();

            DestinationCacheEntry d = cache.get(name);
            synchronized (d) {
                Destination realdest; 
                if (d.get() == null) {
                    // Cache miss: lookup in JNDI
                    realdest = (Destination) o.getJndiObject(getRA(), 
                        null, mMC.getManagedConnectionFactory(), null, name);
                    // Bind in cache
                    d.set(realdest);
                }
                ret = d.get();
            }
        }
        
        return ret;
    }

    /**
     * @see com.stc.jmsjca.core.SessionConnection#createQueue(java.lang.String)
     */
    public Queue createQueue(String name) throws JMSException {
        AdminQueue admindest = new AdminQueue();
        admindest.setName(name);
        return (Queue) createDestination(admindest);
    }

    /**
     * @see com.stc.jmsjca.core.SessionConnection#createTopic(java.lang.String)
     */
    public Topic createTopic(String name) throws JMSException {
        AdminTopic admindest = new AdminTopic();
        admindest.setName(name);
        return (Topic) createDestination(admindest);
    }

}
