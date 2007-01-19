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
 * $RCSfile: XBootstrapContext.java,v $
 * $Revision: 1.1.1.1 $
 * $Date: 2007-01-19 22:48:00 $
 *
 * Copyright 2003-2007 Sun Microsystems, Inc. All Rights Reserved.  
 */

package com.stc.jmsjca.jcacontainer;

import javax.resource.spi.BootstrapContext;
import javax.resource.spi.UnavailableException;
import javax.resource.spi.XATerminator;
import javax.resource.spi.work.WorkManager;

import java.util.Timer;

public class XBootstrapContext implements BootstrapContext {
    private WorkManager mWorkMgr;
    
    public XBootstrapContext(WorkManager workmgr) {
        mWorkMgr = workmgr;
    }

    /**
     * @see javax.resource.spi.BootstrapContext#getWorkManager()
     */
    public WorkManager getWorkManager() {
        return mWorkMgr;
    }

    /**
     * @see javax.resource.spi.BootstrapContext#getXATerminator()
     */
    public XATerminator getXATerminator() {
        // TODO Auto-generated method stub
        return null;
    }

    /**
     * @see javax.resource.spi.BootstrapContext#createTimer()
     */
    public Timer createTimer() throws UnavailableException {
        // TODO Auto-generated method stub
        return null;
    }
}
