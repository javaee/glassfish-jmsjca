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

import java.lang.reflect.Method;
import java.net.URL;

/**
 * Represents a single interceptor description
 * 
 * @author fkieviet
 */
public class InterceptorInfo {
    private String classname;
    private String url;
    private int iLine;
    private ClassLoader loader;
    private Class<?> clazz;
    private Method method;
    
    /**
     * Constructor with the origin information
     * 
     * @param name
     * @param url
     * @param line
     * @param loader
     */
    public InterceptorInfo(String name, URL url, int line, ClassLoader loader) {
        super();
        this.classname = name;
        this.url = url.toExternalForm();
        iLine = line;
        this.loader = loader;
    }

    /**
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((loader == null) ? 0 : loader.hashCode());
        result = prime * result + ((classname == null) ? 0 : classname.hashCode());
        return result;
    }

    /**
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        InterceptorInfo other = (InterceptorInfo) obj;
        if (loader == null) {
            if (other.loader != null) {
                return false;
            }
        } else if (!loader.equals(other.loader)) {
            return false;
        }
        if (classname == null) { 
            if (other.classname != null) {
                return false;
            }
        } else if (!classname.equals(other.classname)) {
            return false;
        }
        return true;
    }
    
    @Override
    public String toString() {
        return classname + " (" + url + ":" + iLine + "@(" + loader + ")";
    }

    /**
     * Getter for clazz
     *
     * @return Class<?>
     */
    public final Class<?> getClazz() {
        return clazz;
    }

    /**
     * Getter for method
     *
     * @return Method
     */
    public final Method getMethod() {
        return method;
    }

    /**
     * Getter for classname
     *
     * @return String
     */
    public final String getClassname() {
        return classname;
    }

    /**
     * Setter for clazz
     *
     * @param clazz Class<?>The clazz to set.
     */
    public final void setClazz(Class<?> clazz) {
        this.clazz = clazz;
    }

    /**
     * Setter for method
     *
     * @param method MethodThe method to set.
     */
    public final void setMethod(Method method) {
        this.method = method;
    }
}

