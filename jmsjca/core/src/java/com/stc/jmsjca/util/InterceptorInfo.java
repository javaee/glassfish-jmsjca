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

/**
 * Represents a single interceptor description.
 * 
 * Identity (for hashcode and equals) depends on the class only
 * 
 * @author fkieviet
 */
public class InterceptorInfo {
    private String svcDescriptorURL;
    private int iLine;
    private Class<?> clazz;
    private Method method;
    
    /**
     * Constructor with the origin information
     * 
     * @param name
     * @param svcDescriptorURL
     * @param line
     * @param loader
     */
    public InterceptorInfo(Class<?> clazz, String svcDescriptorURL, int line) {
        super();
        this.clazz = clazz;
        this.svcDescriptorURL = svcDescriptorURL;
        iLine = line;
    }

    @Override
    public String toString() {
        return clazz.getName() + " (" + svcDescriptorURL + ":" + iLine + ")";
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
        return clazz.getName();
    }

    /**
     * Setter for method
     *
     * @param method MethodThe method to set.
     */
    public final void setMethod(Method method) {
        this.method = method;
    }

    /**
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((clazz == null) ? 0 : clazz.hashCode());
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
        if (clazz == null) {
            if (other.clazz != null) {
                return false;
            }
        } else if (!clazz.equals(other.clazz)) {
            return false;
        }
        return true;
    }

    /**
     * Getter for svcDescriptorURL
     *
     * @return String
     */
    public final String getSvcDescriptorURL() {
        return svcDescriptorURL;
    }

    /**
     * Getter for iLine
     *
     * @return int
     */
    public final int getLine() {
        return iLine;
    }
}

