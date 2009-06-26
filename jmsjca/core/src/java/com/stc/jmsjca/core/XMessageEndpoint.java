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

package com.stc.jmsjca.core;

import com.stc.jmsjca.localization.Localizer;
import com.stc.jmsjca.util.Exc;
import com.stc.jmsjca.util.InterceptorChain;

import javax.jms.Message;
import javax.resource.spi.endpoint.MessageEndpoint;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

/**
 * Encapsulates an endpoint plus interceptors
 * 
 * @author fkieviet
 */
public class XMessageEndpoint {
    private static final Localizer LOCALE = Localizer.get();
    private MessageEndpoint mEndpoint;
    private InterceptorChain mInterceptorChain;
    private Method mTargetMethod;
    
    /**
     * Constructor
     * 
     * @param targetEndpoint
     * @param interceptorChain may be null
     */
    public XMessageEndpoint(MessageEndpoint targetEndpoint, Method targetMethod, InterceptorChain interceptorChain) {
        mEndpoint = targetEndpoint;
        mTargetMethod = targetMethod;
        mInterceptorChain = interceptorChain;
    }
    
    /**
     * Delivers to endpoint, through the interceptor chain if there is any, or directly
     * 
     * @param m
     */
    public void onMessage(Message m) {
        if (mInterceptorChain == null) {
            ((javax.jms.MessageListener) mEndpoint).onMessage(m);
        } else {
            try {
                // Create context data map
                Map<String, Object> contextData = new HashMap<String, Object>();
                contextData.put(Options.Interceptor.KEY_MESSAGE, m);
                
                mInterceptorChain.invoke(mEndpoint, mTargetMethod, contextData, m);
            } catch (RuntimeException e) {
                throw e;
            } catch (Exception e) {
                throw Exc.rtexc(LOCALE.x("E216: a checked exception was thrown from an interceptor: {0}", e), e);
            }
        }
    }

    /**
     * Delegates the MessagEndpoint#release() method 
     */
    public void release() {
        mEndpoint.release();
    }

    /**
     * @return endpoint (never null)
     */
    public MessageEndpoint getEndpoint() {
        return mEndpoint;
    }
}

