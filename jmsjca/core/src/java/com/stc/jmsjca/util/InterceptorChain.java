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

import javax.interceptor.InvocationContext;
import javax.jms.JMSException;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
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
    private static final Localizer LOCALE = Localizer.get();

    /**
     * Constructor
     * 
     * @param interceptors
     * @param endpoint
     * @param endpointMethod
     */
    public InterceptorChain(List<InterceptorInstance> interceptors) {
        super();
        mInterceptors = interceptors;
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
        private Map<String, Object> mContextData;
        private Method mEndpointMethod;
        private Object mEndpoint;

        /**
         * Constructor
         * 
         * @param it live iterator that is going over the list of iterators
         * @param args method arguments (the JMS Message)
         */
        public XInvocationContext(Iterator<InterceptorInstance> it, Object endpoint
            , Method endpointMethod, Map<String, Object> contextData, Object[] args) {
            mIterator = it;
            mEndpointMethod = endpointMethod;
            mEndpoint = endpoint;
            mContextData = contextData;
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
            try {
                if (mIterator.hasNext()) {
                    return mIterator.next().invoke(this);
                } else {
                    return mEndpointMethod.invoke(mEndpoint, mParameters);
                }
            } catch (InvocationTargetException e) {
                if (e.getTargetException() instanceof Exception) {
                    throw (Exception) e.getTargetException();
                }
                throw e;
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
     * @param endpoint target endpoint object
     * @param endpointMethod to invoke on target endpoint object
     * @param contextData contextdata passed to interceptors
     * @param args parameters to pass to the final endpoint
     * @return return value of the method on the final endpoint
     * @throws Exception propagated
     */
    public Object invoke(Object endpoint, Method endpointMethod
        , Map<String, Object> contextData, Object... args) throws Exception {
        Iterator<InterceptorInstance> it = mInterceptors.iterator();
        XInvocationContext ctx = new XInvocationContext(it, endpoint, endpointMethod, contextData, args);
        try {
            return ctx.proceed();
        } catch (InvocationTargetException e) {
            if (e.getTargetException() instanceof Exception) {
                throw (Exception) e.getTargetException();
            }
            throw e;
        }
    }

    /**
     * JMS convenience method: throws only JMSExceptions
     * @see invoke
     */
    public Object invokeJMS(Object endpoint, Method endpointMethod
        , Map<String, Object> contextData, Object... args) throws JMSException {
        try {
            return invoke(endpoint, endpointMethod, contextData, args);
        } catch (RuntimeException e) {
            throw e;
        } catch (JMSException e) {
            throw e;
        } catch (Exception e) {
            throw Exc.jmsExc(LOCALE.x("E218: The interceptor threw an exception: {0}", e), e);
        }
    }
}
