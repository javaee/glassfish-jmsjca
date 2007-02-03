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

import com.stc.jmsjca.localization.Localizer;
import com.stc.jmsjca.util.Exc;
import com.stc.jmsjca.util.Logger;
import javax.resource.ResourceException;

/**
 * Local Transaction that uses a transacted session underneath
 *
 * @author Frank Kieviet
 * @version $Revision: 1.1.1.3 $
 */
public class XLocalTransaction implements javax.resource.spi.LocalTransaction {
    private static Logger sLog = Logger.getLogger(XLocalTransaction.class);
    private XManagedConnection mManagedConnection;

    private static final Localizer LOCALE = Localizer.get();

    /**
     * Constructor
     *
     * @param managedConnection XManagedConnection
     */
    public XLocalTransaction(XManagedConnection managedConnection) {
        mManagedConnection = managedConnection;
    }

    /**
     * begin
     *
     * @throws ResourceException never
     */
    public void begin() throws ResourceException {
        if (sLog.isDebugEnabled()) {
            sLog.debug("begin() called");
        }
        // NOP
    }

    /**
     * commit
     *
     * @throws ResourceException on JMSException
     */
    public void commit() throws ResourceException {
        try {
            mManagedConnection.getJSession().commit(null);
        } catch (Exception ex) {
            throw Exc.rsrcExc(LOCALE.x("E077: Failed to commit local transaction: {0}", ex), ex);
        }
    }

    /**
     * rollback
     *
     * @throws ResourceException on JMSException
     */
    public void rollback() throws ResourceException {
        try {
            mManagedConnection.getJSession().rollback(null);
        } catch (Exception ex) {
            throw Exc.rsrcExc(LOCALE.x("E078: Failed to rollback local transaction: {0}", ex), ex);
        }
    }
}
