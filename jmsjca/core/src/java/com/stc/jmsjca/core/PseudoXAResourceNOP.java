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

import com.stc.jts.jtsxa.LastAgentResource;

import javax.transaction.xa.XAException;
import javax.transaction.xa.XAResource;
import javax.transaction.xa.Xid;


/**
 * This is an XAResource that doesn't do anything; it can be used in a non-transacted
 * non-xa mode where the RA is still marked as XAResource.
 *
 * @author fkieviet
 * @version $Revision: 1.6 $
 */
public class PseudoXAResourceNOP implements LastAgentResource {
    /**
     * Constructor
     */
    public PseudoXAResourceNOP() {
    }

    /**
     * @see javax.transaction.xa.XAResource#commit(javax.transaction.xa.Xid, boolean)
     */
    public void commit(Xid arg0, boolean arg1) throws XAException {
    }

    /**
     * @see javax.transaction.xa.XAResource#end(javax.transaction.xa.Xid, int)
     */
    public void end(Xid arg0, int arg1) throws XAException {
        // Nothing
    }

    /**
     * @see javax.transaction.xa.XAResource#forget(javax.transaction.xa.Xid)
     */
    public void forget(Xid arg0) throws XAException {
        // Nothing
    }

    /**
     * @see javax.transaction.xa.XAResource#getTransactionTimeout()
     */
    public int getTransactionTimeout() throws XAException {
        return Integer.MAX_VALUE;
    }

    /**
     * @see javax.transaction.xa.XAResource#isSameRM(javax.transaction.xa.XAResource)
     */
    public boolean isSameRM(XAResource arg0) throws XAException {
        return false;
    }

    /**
     * @see javax.transaction.xa.XAResource#prepare(javax.transaction.xa.Xid)
     */
    public int prepare(Xid arg0) throws XAException {
        // Pretend that we know for sure that commit/rollback will succeed
        return XA_OK;
    }

    /**
     * @see javax.transaction.xa.XAResource#recover(int)
     */
    public Xid[] recover(int arg0) throws XAException {
        return new Xid[0];
    }

    /**
     * @see javax.transaction.xa.XAResource#rollback(javax.transaction.xa.Xid)
     */
    public void rollback(Xid arg0) throws XAException {
    }

    /**
     * @see javax.transaction.xa.XAResource#setTransactionTimeout(int)
     */
    public boolean setTransactionTimeout(int arg0) throws XAException {
        // Not supported
        return false;
    }

    /**
     * @see javax.transaction.xa.XAResource#start(javax.transaction.xa.Xid, int)
     */
    public void start(Xid arg0, int arg1) throws XAException {
        // Nothing
    }

    /**
     * @see com.stc.jts.jtsxa.LastAgentResource#lastAgentPreferenceLevel()
     */
    public int lastAgentPreferenceLevel() {
        return PSEUDO_XA_MAX;
    }
}
