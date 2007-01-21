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
 * $RCSfile: JConnectionFactoryTopicXA.java,v $
 * $Revision: 1.3 $
 * $Date: 2007-01-21 17:51:40 $
 *
 * Copyright 2003-2007 Sun Microsystems, Inc. All Rights Reserved.  
 */

package com.stc.jmsjca.core;

import javax.jms.XAConnection;
import javax.resource.spi.ConnectionManager;

/**
 * <p>Marker class; see JConnectionFactory</p>
 *
 * @author Frank Kieviet
 * @version $Revision: 1.3 $
 */
public class JConnectionFactoryTopicXA extends JConnectionFactory
    implements javax.jms.TopicConnectionFactory,
    java.io.Serializable,
    javax.resource.Referenceable, javax.naming.spi.ObjectFactory {

    /**
     * Constructor
     */
    public JConnectionFactoryTopicXA() {
    }

    /**
     * Constructor
     *
     * @param managedConnectionFactory The JCA internal factory that will create the
     * physical connections
     * @param connectionManager Application Server provided connection manager; may be
     * null in an unmanaged environment
     */
    public JConnectionFactoryTopicXA(XManagedConnectionFactory managedConnectionFactory,
        ConnectionManager connectionManager) {
        super(managedConnectionFactory, connectionManager);
    }

    /**
     * createXAConnection
     *
     * @return XAConnection
     */
    public XAConnection createXAConnection() {
        return null;
    }

    /**
     * createXAConnection
     *
     * @param string String
     * @param string1 String
     * @return XAConnection
     */
    public XAConnection createXAConnection(String string, String string1) {
        return null;
    }
}
