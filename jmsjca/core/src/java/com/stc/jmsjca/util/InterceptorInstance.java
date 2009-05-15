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

import javax.interceptor.InvocationContext;

import java.lang.reflect.InvocationTargetException;

/**
 * Represents a single interceptor instance
 * 
 * @author fkieviet
 */
public class InterceptorInstance {
    private InterceptorInfo mInfo;
    private Object mInterceptor;
    
    /**
     * Creates a new instance; instantiates an object of the specified interceptor class
     * 
     * @param info
     * @throws InstantiationException
     * @throws IllegalAccessException
     */
    public InterceptorInstance(InterceptorInfo info) throws InstantiationException, IllegalAccessException {
        mInfo = info;
        mInterceptor = mInfo.getClazz().newInstance();
    }
    
    /**
     * Invokes the interceptor
     * 
     * @param ctx
     * @return
     * @throws IllegalArgumentException
     * @throws IllegalAccessException
     * @throws InvocationTargetException
     */
    public Object invoke(InvocationContext ctx) throws IllegalArgumentException, 
    IllegalAccessException, InvocationTargetException {
        return mInfo.getMethod().invoke(mInterceptor, ctx);
    }
}

