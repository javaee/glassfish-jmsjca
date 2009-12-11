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

package com.stc.jmsjca.test.core;

import com.stc.jmsjca.core.XXid;

import javax.transaction.HeuristicMixedException;
import javax.transaction.HeuristicRollbackException;
import javax.transaction.RollbackException;
import javax.transaction.Status;
import javax.transaction.Synchronization;
import javax.transaction.SystemException;
import javax.transaction.Transaction;
import javax.transaction.xa.XAException;
import javax.transaction.xa.XAResource;
import javax.transaction.xa.Xid;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

/**
 * A simple rudimentary transaction for unit testing
 * 
 * @author fkieviet
 */
public class TestTransaction implements Transaction {
    private Map<XAResource, ResourceHolder> mAllResources = new IdentityHashMap<XAResource, ResourceHolder>();
    private List<XAResource> mEnlistedResources = new ArrayList<XAResource>();
    private List<XAResource> mSuspendedResources = new ArrayList<XAResource>();
    private List<Synchronization> mSynchronizations = new ArrayList<Synchronization>();
    private boolean mRollbackOnly;
    private TestTransactionManager mTxMgr;
    
    public TestTransaction(TestTransactionManager txmgr) {
        mTxMgr = txmgr;
    }

    public static class ResourceHolder {
        private XXid mXid;
        
        public Xid getXid() {
            return mXid;
        }

        public void setXid(XXid xid) {
            mXid = xid;
        }
    }

    /**
     * @see javax.transaction.Transaction#commit()
     */
    public void commit() throws RollbackException, HeuristicMixedException, HeuristicRollbackException, SecurityException, IllegalStateException, SystemException {
        // syn.before
        for (Iterator<Synchronization> syns = mSynchronizations.iterator(); syns.hasNext();) {
            Synchronization syn = syns.next();
            syn.beforeCompletion();
        }
        
        Exception failed = null;
        
        // prepare
        for (Iterator<Entry<XAResource, ResourceHolder>> xars = mAllResources.entrySet().iterator(); xars.hasNext();) {
            Map.Entry<XAResource, ResourceHolder> x = xars.next();
            XAResource xar = x.getKey();
            ResourceHolder h = x.getValue();
            try {
                xar.prepare(h.getXid());
            } catch (XAException e) {
                failed = e;
                mRollbackOnly = true;
            }
        }
        
        // commit/rollback
        for (Iterator<Entry<XAResource, ResourceHolder>> xars = mAllResources.entrySet().iterator(); xars.hasNext();) {
            Map.Entry<XAResource, ResourceHolder> x = xars.next();
            XAResource xar = x.getKey();
            ResourceHolder h = x.getValue();
            try {
                if (mRollbackOnly) {
                    xar.rollback(h.getXid()); 
                } else {
                    xar.commit(h.getXid(), false);
                }
            } catch (XAException e) {
                throw new RuntimeException("commit failed; transaction left in doubt", e);
            }
        }

        // syn.after
        for (Iterator<Synchronization> syns = mSynchronizations.iterator(); syns.hasNext();) {
            Synchronization syn = syns.next();
            syn.afterCompletion(mRollbackOnly ? Status.STATUS_ROLLEDBACK : Status.STATUS_COMMITTED);
        }
        
        mTxMgr.removeTx(this);

        if (failed != null) {
            throw new RuntimeException("prepare failed", failed);
        }
    }
    
    private void check(boolean test) {
        if (!test) {
            throw new RuntimeException("Assertion failed");
        }
    }

    /**
     * @see javax.transaction.Transaction#delistResource(javax.transaction.xa.XAResource, int)
     */
    public boolean delistResource(XAResource xar, int flag) throws IllegalStateException, SystemException {
        check(flag == XAResource.TMSUCCESS);
        boolean removed = mEnlistedResources.remove(xar);
        ResourceHolder h = mAllResources.get(xar);
        check(removed);
        try {
            xar.end(h.getXid(), flag);
        } catch (XAException e) {
            throw new RuntimeException("delist failed", e);
        }
        return true;
    }

    /**
     * @see javax.transaction.Transaction#enlistResource(javax.transaction.xa.XAResource)
     */
    public boolean enlistResource(XAResource xar) throws RollbackException, IllegalStateException, SystemException {
        mAllResources.remove(xar);
        
        ResourceHolder h = new ResourceHolder();
        h.setXid(new XXid());
        
        try {
            xar.start(h.getXid(), XAResource.TMNOFLAGS);
        } catch (XAException e) {
            throw new RuntimeException("enlist failed", e);
        }
        mAllResources.put(xar, h);
        mEnlistedResources.add(xar);
        return true;
    }

    /**
     * @see javax.transaction.Transaction#getStatus()
     */
    public int getStatus() throws SystemException {
        return mRollbackOnly ? XAResource.TMSUCCESS : XAResource.TMFAIL;
    }

    /**
     * @see javax.transaction.Transaction#registerSynchronization(javax.transaction.Synchronization)
     */
    public void registerSynchronization(Synchronization syn) throws RollbackException, IllegalStateException, SystemException {
        mSynchronizations.add(syn);
    }

    /**
     * @see javax.transaction.Transaction#rollback()
     */
    public void rollback() throws IllegalStateException, SystemException {
        mRollbackOnly = true;
        try {
            commit();
        } catch (Exception e) {
            throw new RuntimeException("rollback failed", e);
        }
    }

    /**
     * @see javax.transaction.Transaction#setRollbackOnly()
     */
    public void setRollbackOnly() throws IllegalStateException, SystemException {
        mRollbackOnly = true;
    }

    public void suspendEnlisted() throws SystemException {
        XAResource[] xars = mEnlistedResources.toArray(new XAResource[mEnlistedResources.size()]);
        for (int i = 0; i < xars.length; i++) {
            try {
                ResourceHolder h = mAllResources.get(xars[i]);
                xars[i].end(h.getXid(), XAResource.TMSUSPEND);
                mSuspendedResources.add(xars[i]);
                mEnlistedResources.remove(xars[i]);
            } catch (XAException e) {
                throw new RuntimeException("end failed", e);
            }
        }
    }

    public void unsuspendAll() throws SystemException {
        XAResource[] xars = mSuspendedResources.toArray(new XAResource[mSuspendedResources.size()]);
        for (int i = 0; i < xars.length; i++) {
            try {
                ResourceHolder h = mAllResources.get(xars[i]);
                xars[i].start(h.getXid(), XAResource.TMRESUME);
                mSuspendedResources.remove(xars[i]);
                mEnlistedResources.add(xars[i]);
            } catch (XAException e) {
                throw new RuntimeException("start failed", e);
            }
        }
    }
}

