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

import javax.transaction.HeuristicMixedException;
import javax.transaction.HeuristicRollbackException;
import javax.transaction.InvalidTransactionException;
import javax.transaction.NotSupportedException;
import javax.transaction.RollbackException;
import javax.transaction.SystemException;
import javax.transaction.Transaction;
import javax.transaction.TransactionManager;

import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * A simple rudimentary transaction manager for unit testing
 * 
 * @author fkieviet
 */
public class TestTransactionManager implements TransactionManager {
    private IdentityHashMap<Thread, TestTransaction> mActiveTransactions = new IdentityHashMap<Thread, TestTransaction>();
    
    /**
     * @see javax.transaction.TransactionManager#begin()
     */
    public void begin() throws NotSupportedException, SystemException {
        if (getTransaction() != null) {
            throw new RuntimeException("Tx already started");
        }
        TestTransaction tx = new TestTransaction(this);
        mActiveTransactions.put(Thread.currentThread(), tx);
    }
    
    private void check(boolean test) {
        if (!test) {
            throw new RuntimeException("Assertion failed");
        }
    }

    public void removeTx(TestTransaction tx) {
        if (mActiveTransactions.get(Thread.currentThread()) == tx) {
            check(mActiveTransactions.remove(Thread.currentThread()) != null);
        } else {
            boolean found = false;
            for (Iterator<Map.Entry<Thread, TestTransaction>> iter = mActiveTransactions.entrySet().iterator(); iter.hasNext();) {
                Map.Entry<Thread, TestTransaction> element = iter.next();
                if (element.getValue() == tx) {
                    iter.remove();
                    found = true;
                    break;
                }
            }
            check(found);
        }
    }

    /**
     * @see javax.transaction.TransactionManager#commit()
     */
    public void commit() throws RollbackException, HeuristicMixedException, HeuristicRollbackException, SecurityException, IllegalStateException, SystemException {
        getTransaction().commit();
    }

    public int getStatus() throws SystemException {
        throw new RuntimeException("Not implemented");
    }

    /**
     * @see javax.transaction.TransactionManager#getTransaction()
     */
    public Transaction getTransaction() throws SystemException {
        return mActiveTransactions.get(Thread.currentThread());
    }

    /**
     * @see javax.transaction.TransactionManager#resume(javax.transaction.Transaction)
     */
    public void resume(Transaction tx1) throws InvalidTransactionException, IllegalStateException, SystemException {
        TestTransaction tx = (TestTransaction) tx1;
        tx.unsuspendAll();
        mActiveTransactions.put(Thread.currentThread(), tx);
    }

    /**
     * @see javax.transaction.TransactionManager#rollback()
     */
    public void rollback() throws IllegalStateException, SecurityException, SystemException {
        getTransaction().rollback();
        mActiveTransactions.remove(Thread.currentThread());
    }

    /**
     * @see javax.transaction.TransactionManager#setRollbackOnly()
     */
    public void setRollbackOnly() throws IllegalStateException, SystemException {
        getTransaction().setRollbackOnly();
    }

    public void setTransactionTimeout(int arg0) throws SystemException {
        throw new RuntimeException("Not implemented");
    }

    /**
     * @see javax.transaction.TransactionManager#suspend()
     */
    public Transaction suspend() throws SystemException {
        TestTransaction tx = (TestTransaction) getTransaction();
        if (tx == null) {
            throw new RuntimeException("No transaction");
        }
        mActiveTransactions.remove(Thread.currentThread());
        tx.suspendEnlisted();
        return tx;
    }
}

