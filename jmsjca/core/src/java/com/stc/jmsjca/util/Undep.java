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

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;


/**
 * Tool to avoid compile time dependencies without having to resort to extensive
 * reflection code
 *
 * @author Frank Kieviet
 * @version $Revision: 1.6 $
 */
public final class Undep implements InvocationHandler {
    private Class<?> mItf;
    private Object mTarget;
    private String mClassname;
        
    private Undep(Class<?> itf, Object target) {
        mTarget = target;
        mItf = itf;
        
        if (mTarget == null) {
            try {
                Field f = mItf.getDeclaredField("NAME");
                mClassname = (String) f.get(null);
            } catch (Exception e) {
                throw new RuntimeException("Classname declaration could not be found or accessed: " + e, e);
            }
        }
    }

    /**
     * @see java.lang.reflect.InvocationHandler#invoke(java.lang.Object, java.lang.reflect.Method, java.lang.Object[])
     */
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        if (method.getName().equals("constructor")) {
            if (mTarget != null) {
                throw new RuntimeException("Object already exists");
            }
            Class<?> c = Class.forName(mClassname);
            Class<?>[] argtypes = method.getParameterTypes();
            // TODO: do argument type conversions
            Constructor<?> constructor = c.getConstructor(argtypes);
            // TODO: do argument conversions
            mTarget = constructor.newInstance(args);
            return null;
        } else {
            Class<?>[] argtypes = method.getParameterTypes();
            // TODO: do argument type conversions
            Method m = mTarget.getClass().getMethod(method.getName(), argtypes);
            
            // TODO: do argument conversions
            return m.invoke(mTarget, args);
        }
    }
    
    /**
     * Creates a new wrapper
     * 
     * @param itf interface to return
     * @param cl classloader to use
     * @return proxy with specified interface
     */
    public static Object create(Class<?> itf, ClassLoader cl) {
        return Proxy.newProxyInstance(cl,
            new Class[] {itf}, new Undep(itf, null));
    }

    /**
     * Creates a new wrapper
     * 
     * @param itf interface to return
     * @param target object to wrap
     * @return proxy with specified interface
     */
    public static Object create(Class<?> itf, Object target) {
        return Proxy.newProxyInstance(target.getClass().getClassLoader(),
            new Class[] {itf}, new Undep(itf, target));
    }
}
