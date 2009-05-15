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
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Builds interceptor chains
 * 
 * @author fkieviet
 */
public class InterceptorChainBuilder {
    private List<InterceptorInfo> mInfos;

    /**
     * Constructor
     * 
     * @param interceptors
     */
    public InterceptorChainBuilder(Collection<InterceptorInfo> interceptors) {
        mInfos = new ArrayList<InterceptorInfo>();
        mInfos.addAll(interceptors);
    }

    /**
     * Called whenever a new endpoint is created: creates a new interceptor chain
     * 
     * @param endpoint
     * @param endpointMethod
     * @return new chain
     * @throws IllegalAccessException 
     * @throws InstantiationException 
     */
    public InterceptorChain create(Object endpoint, Method endpointMethod) throws InstantiationException, IllegalAccessException {
        List<InterceptorInstance> instances = new ArrayList<InterceptorInstance>();
        for (InterceptorInfo info : mInfos) {
            InterceptorInstance inst = new InterceptorInstance(info);
            instances.add(inst);
        }
        return new InterceptorChain(instances, endpoint, endpointMethod);
    }
    
    /**
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return "Interceptors: {" + Str.concat(mInfos, "}, {") + "}"; 
    }

    /**
     * @return true if there are any interceptors
     */
    public boolean hasInterceptors() {
        return !mInfos.isEmpty();
    }

}
