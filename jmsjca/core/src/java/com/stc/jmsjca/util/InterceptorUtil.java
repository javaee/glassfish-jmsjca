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
 * Generic utilities used for interceptors
 * 
 * @author fkieviet
 */
public final class InterceptorUtil {
    private InterceptorUtil() {
    }
    
    /**
     * An alternative to getMethod that doesn't throw checked exceptions
     * 
     * @param clazz class to look up on
     * @param name name of the method
     * @param parameters signature
     * @return method
     */
    public static Method getMethod(Class<?> clazz, String name, Class<?>... parameters) {
        try {
            return clazz.getMethod(name, parameters);
        } catch (Exception e) {
            throw new RuntimeException("Method not found: " + e, e);
        }
    }
}
