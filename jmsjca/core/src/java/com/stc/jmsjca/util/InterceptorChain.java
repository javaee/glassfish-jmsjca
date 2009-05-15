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

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Represents a full interceptor chain. A different chain is built for each endpoint and 
 * is reused for all messages ever sent to that endpoint.
 * 
 * @author fkieviet
 */
public class InterceptorChain {
    private List<InterceptorInstance> mInterceptors;
    private Object mEndpoint;
    private Method mEndpointMethod;

    /**
     * Constructor
     * 
     * @param interceptors
     * @param endpoint
     * @param endpointMethod
     */
    public InterceptorChain(List<InterceptorInstance> interceptors, Object endpoint, Method endpointMethod) {
        super();
        mInterceptors = interceptors;
        mEndpoint = endpoint;
        mEndpointMethod = endpointMethod;
    }

    /**
     * The invocation context that is passed around to the interceptors. A new 
     * context is used for each message. 
     * 
     * @author fkieviet
     */
    public class XInvocationContext implements InvocationContext {
        private Iterator<InterceptorInstance> mIterator;
        private Object[] mParameters;
        private Map<String, Object> mContextData = new HashMap<String, Object>();

        /**
         * Constructor
         * 
         * @param it live iterator that is going over the list of iterators
         * @param args method arguments (the JMS Message)
         */
        public XInvocationContext(Iterator<InterceptorInstance> it, Object[] args) {
            mIterator = it;
            mParameters = args;
        }

        public Map<String, Object> getContextData() {
            return mContextData;
        }

        /**
         * @see javax.interceptor.InvocationContext#getMethod()
         */
        public Method getMethod() {
            return mEndpointMethod;
        }

        /**
         * @see javax.interceptor.InvocationContext#getParameters()
         */
        public Object[] getParameters() {
            return mParameters;
        }

        /**
         * @see javax.interceptor.InvocationContext#getTarget()
         */
        public Object getTarget() {
            return mEndpoint;
        }

        /**
         * @see javax.interceptor.InvocationContext#proceed()
         */
        public Object proceed() throws Exception {
            // Call the next interceptor, or the final endpoint
            if (mIterator.hasNext()) {
                return mIterator.next().invoke(this);
            } else {
                return mEndpointMethod.invoke(mEndpoint, mParameters);
            }
        }

        /**
         * @see javax.interceptor.InvocationContext#setParameters(java.lang.Object[])
         */
        public void setParameters(Object[] parameters) {
            mParameters = parameters;
        }
    }

    /**
     * Invoke the final endpoint through the chain of interceptors
     * 
     * @param args parameters to pass to the final endpoint
     * @return return value of the method on the final endpoint
     * @throws Exception propagated
     */
    public Object invoke(Object... args) throws Exception {
        Iterator<InterceptorInstance> it = mInterceptors.iterator();
        XInvocationContext ctx = new XInvocationContext(it, args);
        return ctx.proceed();
    }
}
