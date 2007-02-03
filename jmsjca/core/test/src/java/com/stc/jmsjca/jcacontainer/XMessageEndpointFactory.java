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

package com.stc.jmsjca.jcacontainer;

import javax.resource.spi.UnavailableException;
import javax.resource.spi.endpoint.MessageEndpoint;
import javax.resource.spi.endpoint.MessageEndpointFactory;
import javax.transaction.TransactionManager;
import javax.transaction.xa.XAResource;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

public class XMessageEndpointFactory implements MessageEndpointFactory {
    private Method mMethod;
    private Class mItf;
    private MDBFactory mFact; 
    private boolean mTransacted;
    private TransactionManager mTxMgr;
    
    public XMessageEndpointFactory(Method m, Class itf, MDBFactory fact, TransactionManager txmgr) {
        mMethod = m;
        mItf = itf;
        mFact = fact;
        mTransacted = txmgr != null;
        mTxMgr = txmgr;
    }

    /**
     * @see javax.resource.spi.endpoint.MessageEndpointFactory#createEndpoint(javax.transaction.xa.XAResource)
     */
    public MessageEndpoint createEndpoint(XAResource xar) throws UnavailableException {
        // TODO: add pooling
        Object target = mFact.createMDB();
        
        XMessageEndpoint ep = new XMessageEndpoint(this, xar, target, mMethod, mTxMgr);
        MessageEndpoint ret = (MessageEndpoint) Proxy.newProxyInstance(
            mItf.getClassLoader(), new Class[] { mItf, MessageEndpoint.class }, ep);
        
        return ret;
    }

    /**
     * @see javax.resource.spi.endpoint.MessageEndpointFactory#isDeliveryTransacted(java.lang.reflect.Method)
     */
    public boolean isDeliveryTransacted(Method arg0) throws NoSuchMethodException {
        return mTransacted;
    }
    
    public void release(XMessageEndpoint mep) {
        //TODO: pooling
    }
}
