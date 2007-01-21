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
 * $RCSfile: XMessageEndpoint.java,v $
 * $Revision: 1.1.1.2 $
 * $Date: 2007-01-21 07:52:44 $
 *
 * Copyright 2003-2007 Sun Microsystems, Inc. All Rights Reserved.  
 */

package com.stc.jmsjca.jcacontainer;

import com.stc.jmsjca.util.Exc;

import javax.resource.ResourceException;
import javax.resource.spi.endpoint.MessageEndpoint;
import javax.transaction.TransactionManager;
import javax.transaction.xa.XAResource;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class XMessageEndpoint implements MessageEndpoint, InvocationHandler {
    private XAResource mXAResource;
    private XMessageEndpointFactory mEPF;
    private Object mTarget;
    private Method mMethod;
    private TransactionManager mTxMgr;
    
    public XMessageEndpoint(XMessageEndpointFactory epf, XAResource xar, Object target, Method method, TransactionManager txmgr) {
        mXAResource = xar;
        mEPF = epf;
        mTarget = target;
        mMethod = method;
        mTxMgr = txmgr;
    }

    /**
     * @see javax.resource.spi.endpoint.MessageEndpoint#beforeDelivery(java.lang.reflect.Method)
     */
    public void beforeDelivery(Method arg0) throws NoSuchMethodException, ResourceException {
        if (mXAResource != null && mTxMgr != null) {
            try {
                mTxMgr.begin();
                mTxMgr.getTransaction().enlistResource(mXAResource);
            } catch (Exception e) {
                throw Exc.rsrcExc("Tx start failed: " + e, e);
            }
        }
    }

    /**
     * @see javax.resource.spi.endpoint.MessageEndpoint#afterDelivery()
     */
    public void afterDelivery() throws ResourceException {
        if (mXAResource != null && mTxMgr != null) {
            try {
                mTxMgr.getTransaction().delistResource(mXAResource, XAResource.TMSUCCESS);
                mTxMgr.commit();
            } catch (Exception e) {
                throw Exc.rsrcExc("Tx start failed: " + e, e);
            }
        }
    }

    /**
     * @see javax.resource.spi.endpoint.MessageEndpoint#release()
     */
    public void release() {
        mEPF.release(this);
    }
    
    /**
     * @return associated XAResource (may be null)
     */
    public XAResource getXAResource() {
        return mXAResource;
    }
    
    private Object invokeTarget(Object[] args) throws IllegalArgumentException, IllegalAccessException, InvocationTargetException {
        return mMethod.invoke(mTarget, args);
    }

    /**
     * @see java.lang.reflect.InvocationHandler#invoke(java.lang.Object, java.lang.reflect.Method, java.lang.Object[])
     */
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        try {
            if (method.equals(mMethod)) {
                return invokeTarget(args);
            } else {
                return method.invoke(this, args);
            }
        } catch (InvocationTargetException e) {
            throw e.getCause();
        }
    }
}
