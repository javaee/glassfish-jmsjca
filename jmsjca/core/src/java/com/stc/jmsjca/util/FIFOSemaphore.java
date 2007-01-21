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
 * $RCSfile: FIFOSemaphore.java,v $
 * $Revision: 1.3 $
 * $Date: 2007-01-21 17:51:51 $
 *
 * Copyright 2003-2007 Sun Microsystems, Inc. All Rights Reserved.  
 */

/*
 File: FIFOSemaphore.java

 Originally written by Doug Lea and released into the public domain.
 This may be used for any purposes whatsoever without acknowledgment.
 Thanks for the assistance and support of Sun Microsystems Labs,
 and everyone contributing, testing, and using this code.

 History:
 Date       Who                What
 11Jun1998  dl               Create public version
 */

package com.stc.jmsjca.util;

/**
 * A First-in/First-out implementation of a Semaphore. Waiting requests will be
 * satisified in the order that the processing of those requests got to a
 * certain point. If this sounds vague it is meant to be. FIFO implies a logical
 * timestamping at some point in the processing of the request. To simplify
 * things we don't actually timestamp but simply store things in a FIFO queue.
 * Thus the order in which requests enter the queue will be the order in which
 * they come out. This order need not have any relationship to the order in
 * which requests were made, nor the order in which requests actually return to
 * the caller. These depend on Java thread scheduling which is not guaranteed to
 * be predictable (although JVMs tend not to go out of their way to be unfair).
 * <p>[<a
 * href="http://gee.cs.oswego.edu/dl/classes/EDU/oswego/cs/dl/util/concurrent/intro.html">
 * Introduction to this package. </a>]
 * 
 * @author Doug Lea
 */
public class FIFOSemaphore extends QueuedSemaphore {

    /**
     * Create a Semaphore with the given initial number of permits. Using a seed
     * of one makes the semaphore act as a mutual exclusion lock. Negative seeds
     * are also allowed, in which case no acquires will proceed until the number
     * of releases has pushed the number of permits past 0.
     * 
     * @param initialPermits undocumented
     */
    public FIFOSemaphore(long initialPermits) {
        super(new FIFOWaitQueue(), initialPermits);
    }

    /**
     * Simple linked list queue used in FIFOSemaphore. Methods are not
     * synchronized; they depend on synch of callers
     */

    protected static class FIFOWaitQueue extends WaitQueue {
        /**
         * undocumented
         */
        protected WaitNode head_ = null;
        /**
         * undocumented
         */
        protected WaitNode tail_ = null;

        /**
         * @see com.stc.jmsjca.util.QueuedSemaphore.WaitQueue
         *   #insert(com.stc.jmsjca.util.QueuedSemaphore.WaitQueue.WaitNode)
         */
        protected void insert(WaitNode w) {
            if (tail_ == null) {
                head_ = w;
                tail_ = w;
            } else {
                tail_.next = w;
                tail_ = w;
            }
        }

        /**
         * @see com.stc.jmsjca.util.QueuedSemaphore.WaitQueue#extract()
         */
        protected WaitNode extract() {
            if (head_ == null) {
                return null;
            } else {
                WaitNode w = head_;
                head_ = w.next;
                if (head_ == null) {
                    tail_ = null;
                }
                w.next = null;
                return w;
            }
        }
    }

}
