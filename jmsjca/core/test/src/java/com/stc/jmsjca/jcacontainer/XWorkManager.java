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
 * $RCSfile: XWorkManager.java,v $
 * $Revision: 1.1.1.1 $
 * $Date: 2007-01-19 22:48:00 $
 *
 * Copyright 2003-2007 Sun Microsystems, Inc. All Rights Reserved.  
 */

package com.stc.jmsjca.jcacontainer;

import com.stc.jmsjca.jcacontainer.concurrent.Executor;
import com.stc.jmsjca.jcacontainer.concurrent.PooledExecutor;
import com.stc.jmsjca.jcacontainer.concurrent.LinkedQueue;

import javax.resource.spi.work.ExecutionContext;
import javax.resource.spi.work.Work;
import javax.resource.spi.work.WorkException;
import javax.resource.spi.work.WorkListener;
import javax.resource.spi.work.WorkManager;

public class XWorkManager implements WorkManager {
    private Executor mPool;
    
    public XWorkManager(int poolsize) {
        mPool = new PooledExecutor(new LinkedQueue(), poolsize);
    }
    
    /**
     * @see javax.resource.spi.work.WorkManager#doWork(javax.resource.spi.work.Work)
     */
    public void doWork(Work arg0) throws WorkException {
        throw new IllegalStateException("Not implemented");
    }

    /**
     * @see javax.resource.spi.work.WorkManager#doWork(javax.resource.spi.work.Work, long, javax.resource.spi.work.ExecutionContext, javax.resource.spi.work.WorkListener)
     */
    public void doWork(Work arg0, long arg1, ExecutionContext arg2, WorkListener arg3) throws WorkException {
        throw new IllegalStateException("Not implemented");
    }

    /**
     * @see javax.resource.spi.work.WorkManager#startWork(javax.resource.spi.work.Work)
     */
    public long startWork(Work arg0) throws WorkException {
        throw new IllegalStateException("Not implemented");
    }

    /**
     * @see javax.resource.spi.work.WorkManager#startWork(javax.resource.spi.work.Work, long, javax.resource.spi.work.ExecutionContext, javax.resource.spi.work.WorkListener)
     */
    public long startWork(Work arg0, long arg1, ExecutionContext arg2, WorkListener arg3) throws WorkException {
        throw new IllegalStateException("Not implemented");
    }

    /**
     * @see javax.resource.spi.work.WorkManager#scheduleWork(javax.resource.spi.work.Work)
     */
    public void scheduleWork(Work work) throws WorkException {
        try {
            mPool.execute(work);
        } catch (InterruptedException e) {
            throw new WorkException(e);
        }
    }

    /**
     * @see javax.resource.spi.work.WorkManager#scheduleWork(javax.resource.spi.work.Work, long, javax.resource.spi.work.ExecutionContext, javax.resource.spi.work.WorkListener)
     */
    public void scheduleWork(Work arg0, long arg1, ExecutionContext arg2, WorkListener arg3) throws WorkException {
        throw new IllegalStateException("Not implemented");
    }
}
