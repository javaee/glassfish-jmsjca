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

import com.stc.jmsjca.util.Logger;
import javax.resource.ResourceException;
import javax.resource.spi.ConnectionManager;

/**
 * <p>This acts mainly as a marker class so that IS can keep the different factories
 * apart; this is necessary to have multiple connection factories in one rar file.</p>
 *
 * @author Frank Kieviet
 * @version $Revision: 1.1.1.3 $
 */
public class XMCFTopicXA extends XManagedConnectionFactory {
    private static Logger sLog = Logger.getLogger(XMCFTopicXA.class);

    /**
     * <p>From the spec:</p>
     * <p>Creates a Connection Factory instance. The Connection Factory instance gets
     * initialized with the passed ConnectionManager. In the managed scenario,
     * ConnectionManager is provided by the application server. (end spec)</p>
     *
     * <p>This is typically called by the application server to expose connection
     * factories that the application can use.</p>
     *
     * @param cxManager ConnectionManager to be associated with created EIS connection
     * factory instance
     * @return a new connection factory that can be used by the application
     * @throws ResourceException failure
     */
    public Object createConnectionFactory(ConnectionManager cxManager) throws ResourceException {
        if (sLog.isDebugEnabled()) {
            sLog.debug("Creating new ConnectionFactory");
        }

        return new JConnectionFactoryTopicXA(this, cxManager);
    }
}
