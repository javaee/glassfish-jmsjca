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
 * $RCSfile: XLocalTransactionXA.java,v $
 * $Revision: 1.1.1.1 $
 * $Date: 2007-01-19 22:54:17 $
 *
 * Copyright 2003-2007 Sun Microsystems, Inc. All Rights Reserved.  
 */

package com.stc.jmsjca.core;

import com.stc.jmsjca.util.Exc;
import com.stc.jmsjca.util.Logger;
import javax.resource.ResourceException;
import javax.transaction.xa.XAResource;

/**
 * A Local Transaction implementation that uses an XA session underneath.
 *
 * @author Frank Kieviet
 * @version $Revision: 1.1.1.1 $
 */
public class XLocalTransactionXA implements javax.resource.spi.LocalTransaction {
    private static Logger sLog = Logger.getLogger(XLocalTransactionXA.class);
    private XManagedConnection mManagedConnection;
    private XXid mXid;

    /**
     * Constructor
     *
     * @param managedConnection XManagedConnection
     */
    public XLocalTransactionXA(XManagedConnection managedConnection) {
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

        mXid = new XXid();
        try {
            mManagedConnection.getXAResource().start(mXid, XAResource.TMNOFLAGS);
        } catch (Exception ex) {
            throw Exc.rsrcExc("XAResource failed on start(): " + ex, ex);
        }
    }

    /**
     * commit
     *
     * @throws ResourceException on JMSException
     */
    public void commit() throws ResourceException {
        if (mXid == null) {
            throw new ResourceException("Cannot call commit() before calling begin()");
        }

        try {
            mManagedConnection.getXAResource().end(mXid, XAResource.TMSUCCESS);
            mManagedConnection.getXAResource().commit(mXid, true);
        } catch (Exception ex) {
            throw Exc.rsrcExc("XAResource failed on commit(): " + ex, ex);
        }
    }

    /**
     * rollback
     *
     * @throws ResourceException on JMSException
     */
    public void rollback() throws ResourceException {
        if (mXid == null) {
            throw new ResourceException("Cannot call rollback() before calling begin()");
        }

        try {
            mManagedConnection.getXAResource().end(mXid, XAResource.TMSUCCESS);
            mManagedConnection.getXAResource().rollback(mXid);
        } catch (Exception ex) {
            throw Exc.rsrcExc("XAResource failed on commit(): " + ex, ex);
        }
    }
}
