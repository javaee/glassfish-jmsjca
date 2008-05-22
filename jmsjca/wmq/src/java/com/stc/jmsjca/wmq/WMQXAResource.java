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

package com.stc.jmsjca.wmq;

import javax.transaction.xa.XAException;
import javax.transaction.xa.XAResource;
import javax.transaction.xa.Xid;

/**
 * A wrapper of MQXAResource
 * 
 * @version $Revision: 1.5 $
 * @author cye
 */
public class WMQXAResource implements XAResource {
    private  XAResource mXAResourceImpl;
    
  /**
   * WMQXAResource constructor
   *
   * @param xa XAResource
   */
    public WMQXAResource(XAResource xa) {
        mXAResourceImpl = xa;
    }
    
   /**
    * commit
    * @param  xid Xid
    * @param  onePhase boolean
    * @throws XAException on failure
    */
    public void commit(Xid xid,
        boolean onePhase) throws XAException {
        mXAResourceImpl.commit(xid, onePhase);
    }
    
   /**
    * end
    * @param  xid Xid
    * @param  flags int
    * @throws XAException on failure
    */    
    public void end(Xid xid,
        int flags) throws XAException {
        mXAResourceImpl.end(xid, flags);
    }
    
   /**
    * forget
    * @param  xid Xid
    * @throws XAException on failure
    */
    public void forget(Xid xid) throws XAException {
        mXAResourceImpl.forget(xid);
    }
   
   /**
    * get transaction timeout
    * @return int
    * @throws XAException on failure
    */    
    public int getTransactionTimeout() throws XAException {
        return mXAResourceImpl.getTransactionTimeout();
    }
    
   /**
    * is same RM
    * @param  xares XAResource
    * @return boolean
    * @throws XAException on failure
    */        
    public boolean isSameRM(XAResource xares) throws XAException {
        return mXAResourceImpl.isSameRM(xares);
    }
    
   /**
    * prepare
    * @param  xid Xid
    * @return int
    * @throws XAException on failure
    */         
    public int prepare(Xid xid) throws XAException {
        return mXAResourceImpl.prepare(xid);
    }

   /**
    * receover
    * @param  flag int
    * @return Xid[]
    * @throws XAException on failure
    */       
    public Xid[] recover(int flag) throws XAException {
        return mXAResourceImpl.recover(flag);
    }
  
   /**
    * rollback
    * @param  xid Xid
    * @throws XAException on failure
    */     
    public void rollback(Xid xid) throws XAException {
        mXAResourceImpl.rollback(xid);
    }
   
   /**
    * set transaction timeout
    * @param  seconds int
    * @return boolean
    * @throws XAException on failure
    */     
    public boolean setTransactionTimeout(int seconds) throws XAException {
        return mXAResourceImpl.setTransactionTimeout(seconds);
    }

   /**
    * start
    * @param  xid Xid
    * @param  flags int
    * @throws XAException on failure
    */    
    public void start(Xid xid,
        int flags) throws XAException {
        mXAResourceImpl.start(xid, flags);
    }

}    
