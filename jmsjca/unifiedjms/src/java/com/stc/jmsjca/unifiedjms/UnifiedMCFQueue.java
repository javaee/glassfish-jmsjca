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

package com.stc.jmsjca.unifiedjms;

import com.stc.jmsjca.core.JConnectionFactoryQueueXA;
import com.stc.jmsjca.util.Logger;

import javax.resource.ResourceException;
import javax.resource.spi.ConnectionManager;

/**
 * A specialization for Unified, necessitated by compatibility with GJR which adds a
 * lot of configuration properties  
 *
 * @author Frank Kieviet
 * @version $Revision$
 */
public class UnifiedMCFQueue extends UnifiedMCFBase {
    private static Logger sLog = Logger.getLogger(UnifiedMCFQueue.class);

    /**
     * @see javax.resource.spi.ManagedConnectionFactory#createConnectionFactory(javax.resource.spi.ConnectionManager)
     */
    public Object createConnectionFactory(ConnectionManager cxManager) throws ResourceException {
        if (sLog.isDebugEnabled()) {
            sLog.debug("Creating new ConnectionFactory");
        }

        return new JConnectionFactoryQueueXA(this, cxManager);
    }
}
