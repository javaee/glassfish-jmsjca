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
 * $RCSfile: PseudoXAResource.java,v $
 * $Revision: 1.3 $
 * $Date: 2007-01-21 17:51:41 $
 *
 * Copyright 2003-2007 Sun Microsystems, Inc. All Rights Reserved.  
 */

package com.stc.jmsjca.core;

import com.stc.jmsjca.util.Exc;
import com.stc.jts.jtsxa.LastAgentResource;

import javax.jms.JMSException;
import javax.jms.Session;
import javax.transaction.xa.XAException;
import javax.transaction.xa.XAResource;
import javax.transaction.xa.Xid;


/**
 * This is an XAResource that uses a transacted session internally to provide pseudo-XA
 * functionality. It can be used by specific adapters, e.g. WebLogic to emulate XA
 * functionality where it really is not there. E.g. WebLogic does not provide XA support
 * for clients, it only provides this support when running within the WebLogic server.
 * 
 * This XAResource being only "pseudo", it does nothing in the prepare method or the 
 * recover method. Hence, reliability can not be guaranteed. Adapters using this class
 * should add a notice of this fact to the end-user documentation.
 *
 * @author fkieviet
 * @version $Revision: 1.3 $
 */
public class PseudoXAResource implements LastAgentResource {
    private Session mSession;
    
    /**
     * Constructor
     * 
     * @param s session to operate on
     * @throws JMSException on failure
     */
    public PseudoXAResource(Session s) throws JMSException {
        mSession = s;
        if (!mSession.getTransacted()) {
            throw new JMSException("The session should be transacted.");
        }
    }

    /**
     * @see javax.transaction.xa.XAResource#commit(javax.transaction.xa.Xid, boolean)
     */
    public void commit(Xid arg0, boolean arg1) throws XAException {
        try {
            mSession.commit();
        } catch (JMSException e) {
            throw Exc.xaExc(XAException.XAER_RMERR, e);
        }
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
        try {
            mSession.rollback();
        } catch (JMSException e) {
            throw Exc.xaExc(XAException.XAER_RMERR, e);
        }
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
        return PSEUDO_XA_MIN;
    }
}
