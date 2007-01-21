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
 * $RCSfile: Semaphore.java,v $
 * $Revision: 1.3 $
 * $Date: 2007-01-21 17:51:52 $
 *
 * Copyright 2003-2007 Sun Microsystems, Inc. All Rights Reserved.  
 */

package com.stc.jmsjca.util;

/**
 * Copied from Doug Lea / Concurrent: the Semaphore class
 *
 * @version $Revision: 1.3 $
 * @author Doug Lea
 */
public class Semaphore {
    /** current number of available permits **/
    protected long permits_;

    /**
     * Create a Semaphore with the given initial number of permits. Using a seed of one
     * makes the semaphore act as a mutual exclusion lock. Negative seeds are also
     * allowed, in which case no acquires will proceed until the number of releases has
     * pushed the number of permits past 0.
     *
     * @param initialPermits long
     */
    public Semaphore(long initialPermits) {
        permits_ = initialPermits;
    }

    /**
     * Wait until a permit is available, and take one *
     *
     * @throws InterruptedException propagated
     */
    public void acquire() throws InterruptedException {
        if (Thread.interrupted()) {
            throw new InterruptedException();
        }
        synchronized (this) {
            try {
                while (permits_ <= 0) {
                    wait();
                }
                --permits_;
            } catch (InterruptedException ex) {
                notify();
                throw ex;
            }
        }
    }

    /**
     * Wait at most msecs millisconds for a permit. *
     *
     * @param msecs long
     * @throws InterruptedException propagated
     * @return boolean
     */
    public boolean attempt(long msecs) throws InterruptedException {
        if (Thread.interrupted()) {
            throw new InterruptedException();
        }

        synchronized (this) {
            if (permits_ > 0) {
                --permits_;
                return true;
            } else if (msecs <= 0) {
                return false;
            } else {
                try {
                    long startTime = System.currentTimeMillis();
                    long waitTime = msecs;

                    for (;;) {
                        wait(waitTime);
                        if (permits_ > 0) {
                            --permits_;
                            return true;
                        } else {
                            waitTime = msecs - (System.currentTimeMillis()
                                - startTime);
                            if (waitTime <= 0) {
                                return false;
                            }
                        }
                    }
                } catch (InterruptedException ex) {
                    notify();
                    throw ex;
                }
            }
        }
    }

    /**
     * Returns the current number of permits *
     *
     * @return long
     */
    public synchronized long peek() {
        return permits_;
    }

    /** Release a permit **/
    public synchronized void release() {
        ++permits_;
        notify();
    }

    /**
     * Release N permits. <code>release(n)</code> is equivalent in effect to: <pre>
     *    for (int i = 0; i < n; ++i) release();
     *  </pre>
     *
     * <p> But may be more efficient in some semaphore implementations.
     *
     * @param n long
     */
    public synchronized void release(long n) {
        if (n < 0) {
            throw new IllegalArgumentException("Negative argument");
        }

        permits_ += n;
        for (long i = 0; i < n; ++i) {
            notify();
        }
    }

    /**
     * Return the current number of available permits. Returns an accurate, but possibly
     * unstable value, that may change immediately after returning.
     *
     * @return long
     */
    public synchronized long permits() {
        return permits_;
    }
}
