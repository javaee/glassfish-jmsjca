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

/**
 * A load class utility
 * 
 * @author Vivek
 */
public final class ClassLoaderHelper {
    private ClassLoaderHelper() {
    }
    
    /**
     * Loads a class through the specified classloader or -if that fails- through 
     * the context classloader
     * 
     * @param name classname to load, @See Class.forName()
     * @param initialize @See Class.forName()
     * @param cls @See Class.forName()
     * @return Class<?> object
     * @throws ClassNotFoundException propagated
     */
    public static Class<?> loadClass(String name, boolean initialize, ClassLoader cls) throws ClassNotFoundException {
        try {
            return Class.forName(name, initialize, cls);
        } catch (ClassNotFoundException ex) {
            ClassLoader threadCls = Thread.currentThread().getContextClassLoader();
            if (threadCls != null) {
                return Class.forName(name, initialize, threadCls);
            } else {
                throw ex;
            }
        }
        
    }

    /**
     * @see loadClass(String name, boolean initialize, ClassLoader cls)
     */
    public static Class<?> loadClass(String name, ClassLoader cls) throws ClassNotFoundException {
        return loadClass(name, true, cls);
    }

    /**
     * @see loadClass(String name, boolean initialize, ClassLoader cls)
     */
    public static Class<?> loadClass(String name) throws ClassNotFoundException {
        return loadClass(name, true, ClassLoaderHelper.class.getClassLoader());
    }
    
    /**
     * @see loadClass(String name, boolean initialize, ClassLoader cls)
     */
    public static Class<?> loadClass(String name,  boolean initialize) throws ClassNotFoundException {
        return loadClass(name, initialize, ClassLoaderHelper.class.getClassLoader());
    }
}
