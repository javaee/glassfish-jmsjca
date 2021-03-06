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

import com.stc.jmsjca.localization.Localizer;

import javax.management.MBeanException;
import javax.management.ReflectionException;


/**
 * This is a concrete implementation of MBeanHelper 
 * {@see com.stc.jmsjca.util.MBeanHelper}
 * 
 * This adds the following functionality: sets the context classloader before calling
 * invoke.
 *
 * @author fkieviet
 * @version $Revision: 1.6 $
 */
public abstract class MBeanBase extends MBeanHelper {
//    private static Logger sLog = Logger.getLogger(MBeanBase.class);
    private String mDescription;
    private ClassLoader mContextClassloader;
    private boolean mGetContextClassLoaderFailure;
    private static final Localizer LOCALE = Localizer.get();
    
    /**
     * Constructor
     * 
     * @param description String
     */
    public MBeanBase(String description) {
        mDescription = description;
        try {
            mContextClassloader = Thread.currentThread().getContextClassLoader();
        } catch (SecurityException e) {
            mGetContextClassLoaderFailure = true;
        }
    }
    
    /**
     * @see com.stc.jmsjca.util.MBeanHelper#getMBeanDescription()
     */
    @Override
    protected String getMBeanDescription() {
        return mDescription;
    }
        
    /**
     * @see com.stc.jmsjca.util.MBeanHelper#getMetaObject()
     */
    @Override
    protected Object getMetaObject() {
        return this;
    }
    
    /**
     * @see com.stc.jmsjca.util.MBeanHelper#getDelegate()
     */
    @Override
    protected Object getDelegate() {
        return this;
    }

    /**
     * @see com.stc.jmsjca.util.MBeanHelper#invoke(java.lang.String, java.lang.Object[], java.lang.String[])
     */
    @Override
    public Object invoke(String opName, Object[] opArgs, String[] sig) throws MBeanException, ReflectionException {
        boolean cclIsSet = false;
        ClassLoader ccl = null;
        boolean cclSetFailure = false;
        try {
            // Set CCL
            if (mContextClassloader != null) {
                try {
                    ccl = Thread.currentThread().getContextClassLoader();
                    Thread.currentThread().setContextClassLoader(mContextClassloader);
                    cclIsSet = true;
                } catch (Exception e) {
                    cclSetFailure = true;
                }
            }
        
            // Invoke
            try {
                return super.invoke(opName, opArgs, sig);
            } catch (MBeanException e) {
                if (cclSetFailure || mGetContextClassLoaderFailure) {
                    throw new MBeanException(Exc.rtexc(LOCALE.x("E170: Invocation error " +
                            "(the error may be due to a failure to set the " +
                            "ContextClassLoader -- check permissions): {0}", e), e));
                } else {
                    throw e;
                }
            } catch (ReflectionException e) {
                throw e;
            }
        } finally {
            // Reset CCL
            if (cclIsSet) {
                Thread.currentThread().setContextClassLoader(ccl);
            }
        }
    }
    
    
}
