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

package com.stc.jmsjca.wmq;

import com.stc.jmsjca.core.GenericSessionConnection;
import com.stc.jmsjca.core.RAJMSObjectFactory;
import com.stc.jmsjca.core.RAJMSResourceAdapter;
import com.stc.jmsjca.core.XConnectionRequestInfo;
import com.stc.jmsjca.core.XManagedConnection;
import com.stc.jmsjca.util.Exc;

import javax.jms.JMSException;
import javax.jms.Queue;
import javax.jms.Topic;

import java.lang.reflect.Method;
import java.util.Properties;

/**
 * Provides some MQSeries specific features:
 * - Destination creation
 *
 * @author Frank Kieviet
 * @version $Revision: 1.1 $
 */
public class WMQSessionConnection extends GenericSessionConnection {
    private static Localizer LOCALE = Localizer.get();
    
    /**
     * The option name for BROKERDURSUBQUEUE
     */
    public static final String BROKERDURSUBQUEUE = "BrokerDurSubQueue";

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
    public WMQSessionConnection(Object connectionFactory, RAJMSObjectFactory objfact,
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
    public Queue createQueue(String name, Properties options) throws JMSException {
        return super.createQueue(name, options);
    }

    /**
     * @see com.stc.jmsjca.core.GenericSessionConnection#createTopic(java.lang.String)
     */
    public Topic createTopic(String name, Properties options) throws JMSException {
        Topic t = super.createTopic(name, options);
        
        if (options != null) {
            String dursubqueue = options.getProperty(BROKERDURSUBQUEUE);
            if (dursubqueue != null) {
                try {
                    Method m = t.getClass().getMethod("setBrokerDurSubQueue", new Class[] {String.class });
                    m.invoke(t, new Object[] {dursubqueue });
                } catch (Exception e) {
                    Exc.jmsExc(LOCALE.x("E842: Could not set the broker durable " 
                        + "subscriber subqueue [{0}] on topic [{1}]: {2}",
                        dursubqueue, name, e), e);
                }
            }
        }
        
        return t;
    }
}
