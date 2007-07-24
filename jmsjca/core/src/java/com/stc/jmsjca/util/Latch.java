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
 * Copied from Doug Lea / Concurrent: the Latch class
 * 
 * A latch is a boolean condition that is set at most once, ever. Once a single
 * release is issued, all acquires will pass.
 * <p>
 * <b>Sample usage.</b> Here are a set of classes that use a latch as a start
 * signal for a group of worker threads that are created and started beforehand,
 * and then later enabled.
 * 
 * <pre>
 *  class Worker implements Runnable {
 *    private final Latch startSignal;
 *    Worker(Latch l) { startSignal = l; }
 *     public void run() {
 *       startSignal.acquire();
 *       doWork();
 *    }
 *    void doWork() { ... }
 *  }
 * 
 *  class Driver { // ...
 *    void main() {
 *      Latch go = new Latch();
 *      for (int i = 0; i &lt; N; ++i) // make threads
 *        new Thread(new Worker(go)).start();
 *      doSomethingElse();         // don't let run yet
 *      go.release();              // let all threads proceed
 *    }
 *    
 *  @version $Revision: 1.1 $
 *  @author Doug Lea
 */

public class Latch {
    private boolean latched_ = false;

    /*
     * This could use double-check, but doesn't. If the latch is being used as
     * an indicator of the presence or state of an object, the user would not
     * necessarily get the memory barrier that comes with synch that would be
     * needed to correctly use that object. This would lead to errors that users
     * would be very hard to track down. So, to be conservative, we always use
     * synch.
     */

    /**
     * @throws InterruptedException propagated
     */
    public void acquire() throws InterruptedException {
        if (Thread.interrupted()) {
            throw new InterruptedException();
        }
        synchronized (this) {
            while (!latched_) {
                wait();
            }
        }
    }

    /**
     * Tries to pass the latch
     * 
     * @param msecs delay
     * @return true if passed
     * @throws InterruptedException propagated
     */
    public boolean attempt(long msecs) throws InterruptedException {
        if (Thread.interrupted()) {
            throw new InterruptedException();
        }
        synchronized (this) {
            if (latched_) {
                return true;
            } else if (msecs <= 0) {
                return false;
            } else {
                long waitTime = msecs;
                long start = System.currentTimeMillis();
                for (;;) {
                    wait(waitTime);
                    if (latched_) {
                        return true;
                    } else {
                        waitTime = msecs - (System.currentTimeMillis() - start);
                        if (waitTime <= 0) {
                            return false;
                        }
                    }
                }
            }
        }
    }

    /** 
     * Enable all current and future acquires to pass 
     */
    public synchronized void release() {
        latched_ = true;
        notifyAll();
    }
}
