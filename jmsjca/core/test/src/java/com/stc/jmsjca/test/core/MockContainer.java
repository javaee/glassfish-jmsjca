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

import com.stc.jmsjca.core.TxMgr;
import com.stc.jmsjca.jcacontainer.MDBFactory;
import com.stc.jmsjca.jcacontainer.XBootstrapContext;
import com.stc.jmsjca.jcacontainer.XMessageEndpointFactory;
import com.stc.jmsjca.jcacontainer.XWorkManager;

import javax.jms.MessageListener;
import javax.resource.spi.BootstrapContext;
import javax.resource.spi.endpoint.MessageEndpointFactory;
import javax.resource.spi.work.WorkManager;

import java.lang.reflect.Method;

public class MockContainer {
    MessageListener mMDB;
    private XWorkManager mWorkManager;
    private XBootstrapContext mBC;
    private MDBFactory mMDBFact;
    private TestTransactionManager mTxMgr;
    private MessageEndpointFactory mMEF;
    private boolean mNoTx;

    public MockContainer(MessageListener mdb) {
        mMDB = mdb;
    }
    
    public void setMDB(MessageListener mdb) {
        mMDB = mdb;
    }
    
    public WorkManager getWorkManager() {
        if (mWorkManager == null) {
            mWorkManager = new XWorkManager(1);
        }
        return mWorkManager;
    }
    
    public BootstrapContext getBootstrapContext() {
        if (mBC == null) {
            mBC = new XBootstrapContext(getWorkManager());
        }
        return mBC;
    }
    
    public MDBFactory getMDBFactory() { 
        if (mMDBFact == null) {
            mMDBFact = new MDBFactory() {
                @Override
                public Object createMDB() {
                    return mMDB;
                }
            };
        }
        return mMDBFact;
    }
    
    public TestTransactionManager getTransactionManager() {
        if (mTxMgr == null && !mNoTx) {
            mTxMgr = new TestTransactionManager();
            TxMgr.setUnitTestTxMgr(mTxMgr);
        }
        return mTxMgr;
    }
    
    public MessageEndpointFactory getMessageEndpointFactory() {
        if (mMEF == null) {
            Method m;
            try {
                m = MessageListener.class.getMethod("onMessage",
                    new Class[] { javax.jms.Message.class });
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            
            mMEF = new XMessageEndpointFactory(m, MessageListener.class, getMDBFactory(), getTransactionManager());
        }
        return mMEF;
    }

    public void setNoTx() {
        mNoTx = true;
    }
    
}

