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

package com.stc.jmsjca.util;

/**
 * Copied from Doug Lea / Concurrent
 * 
 */
/*
 * File: QueuedSemaphore.java
 * 
 * Originally written by Doug Lea and released into the public domain. This may
 * be used for any purposes whatsoever without acknowledgment. Thanks for the
 * assistance and support of Sun Microsystems Labs, and everyone contributing,
 * testing, and using this code.
 * 
 * History: Date Who What 11Jun1998 dl Create public version 5Aug1998 dl
 * replaced int counters with longs 24Aug1999 dl release(n): screen arguments
 */

/**
 * Abstract base class for semaphores relying on queued wait nodes.
 * <p>[<a
 * href="http://gee.cs.oswego.edu/dl/classes/EDU/oswego/cs/dl/util/concurrent/intro.html">
 * Introduction to this package. </a>]
 * 
 * @version $Revision: 1.1.1.3 $
 * @author Doug Lea
 */
public abstract class QueuedSemaphore extends Semaphore {

    /**
     * undocumented
     */
    protected final WaitQueue wq_;

    QueuedSemaphore(WaitQueue q, long initialPermits) {
        super(initialPermits);
        wq_ = q;
    }

    /**
     * @see com.stc.jmsjca.util.Semaphore#acquire()
     */
    public void acquire() throws InterruptedException {
        if (Thread.interrupted()) {
            throw new InterruptedException();
        }
        if (precheck()) {
            return;
        }
        WaitQueue.WaitNode w = new WaitQueue.WaitNode();
        w.doWait(this);
    }

    /**
     * @see com.stc.jmsjca.util.Semaphore#attempt(long)
     */
    public boolean attempt(long msecs) throws InterruptedException {
        if (Thread.interrupted()) {
            throw new InterruptedException();
        }
        if (precheck()) {
            return true;
        }
        if (msecs <= 0) {
            return false;
        }

        WaitQueue.WaitNode w = new WaitQueue.WaitNode();
        return w.doTimedWait(this, msecs);
    }

    /**
     * @return undocumented
     */
    protected synchronized boolean precheck() {
        boolean pass = (permits_ > 0);
        if (pass) {
            --permits_;
        }
        return pass;
    }

    /**
     * @param w undocumented
     * @return undocumented
     */
    protected synchronized boolean recheck(WaitQueue.WaitNode w) {
        boolean pass = (permits_ > 0);
        if (pass) {
            --permits_;
        } else {
            wq_.insert(w);
        }
        return pass;
    }

    /**
     * @return undocumented
     */
    protected synchronized WaitQueue.WaitNode getSignallee() {
        WaitQueue.WaitNode w = wq_.extract();
        if (w == null) {
            ++permits_; // if none, inc permits for new arrivals
        }
        return w;
    }

    /**
     * @see com.stc.jmsjca.util.Semaphore#release()
     */
    public void release() {
        for (;;) {
            WaitQueue.WaitNode w = getSignallee();
            if (w == null) {
                return; // no one to signal
            }
            if (w.signal()) {
                return; // notify if still waiting, else skip
            }
        }
    }

    /**
     * @see com.stc.jmsjca.util.Semaphore#release(long)
     */
    public void release(long n) {
        if (n < 0) {
            throw new IllegalArgumentException("Negative argument");
        }

        for (long i = 0; i < n; ++i) {
            release();
        }
    }

    /**
     * Base class for internal queue classes for semaphores, etc. Relies on
     * subclasses to actually implement queue mechanics
     */
    protected abstract static class WaitQueue {
        /**
         * @param w undocumented
         */
        /**
         * @param w undocumented
         */
        protected abstract void insert(WaitNode w); // assumed not to block

        /**
         * @return undocumented
         */
        protected abstract WaitNode extract(); // should return null if empty

        /**
         * undocumented
         */
        protected static class WaitNode {
            /**
             * undocumented
             */
            boolean waiting = true;
            WaitNode next = null;

            /**
             * @return undocumented
             */
            protected synchronized boolean signal() {
                boolean signalled = waiting;
                if (signalled) {
                    waiting = false;
                    notify();
                }
                return signalled;
            }

            /**
             * @param sem undocumented
             * @param msecs undocumented
             * @return undocumented
             * @throws InterruptedException undocumented
             */
            protected synchronized boolean doTimedWait(QueuedSemaphore sem, long msecs)
                throws InterruptedException {
                if (sem.recheck(this) || !waiting) {
                    return true;
                } else if (msecs <= 0) {
                    waiting = false;
                    return false;
                } else {
                    long waitTime = msecs;
                    long start = System.currentTimeMillis();

                    try {
                        for (;;) {
                            wait(waitTime);
                            if (!waiting) { // definitely signalled
                                return true;
                            } else {
                                waitTime = msecs - (System.currentTimeMillis() - start);
                                if (waitTime <= 0) { // timed out
                                    waiting = false;
                                    return false;
                                }
                            }
                        }
                    } catch (InterruptedException ex) {
                        if (waiting) { // no notification
                            waiting = false; // invalidate for the signaller
                            throw ex;
                        } else { // thread was interrupted after it was
                            // notified
                            Thread.currentThread().interrupt();
                            return true;
                        }
                    }
                }
            }

            /**
             * @param sem sem
             * @throws InterruptedException interrupted
             */
            protected synchronized void doWait(QueuedSemaphore sem)
                throws InterruptedException {
                if (!sem.recheck(this)) {
                    try {
                        while (waiting) {
                            wait();
                        }
                    } catch (InterruptedException ex) {
                        if (waiting) { // no notification
                            waiting = false; // invalidate for the signaller
                            throw ex;
                        } else { // thread was interrupted after it was
                                    // notified
                            Thread.currentThread().interrupt();
                            return;
                        }
                    }
                }
            }
        }
    }
}
