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

import com.stc.jmsjca.core.Options;

import javax.interceptor.AroundInvoke;
import javax.interceptor.InvocationContext;
import javax.jms.Message;
import javax.jms.ObjectMessage;

/**
 * Used to test JMSJCA; this class is also packaged as part of the production RAR. It 
 * is normally not used -- it is only used if specified as such in the options.
 * 
 * @author fkieviet
 */
public class SampleInterceptor {
    /**
     * For testing Interceptors: an object msg with this boolean msg property set to 
     * true will result in executing that message on an inbound interceptor 
     */
    public static final String TEST_IN_EXEC = Options.MessageProperties.MSG_PROP_PREFIX + "_test_exec_inbound";

    /**
     * For testing Interceptors: an object msg with this boolean msg property set to 
     * true will result in executing that message on an outbound interceptor 
     */
    public static final String TEST_OUT_EXEC = Options.MessageProperties.MSG_PROP_PREFIX + "_test_exec_outbound";

    
    private static ThreadLocal<InvocationContext> exportedCtx = new ThreadLocal<InvocationContext>();
    
    /**
     * @return exported context
     */
    public static InvocationContext getInboundContext() {
        return exportedCtx.get();
    }

    /**
     * For testing: a Runnable that can throw an exception
     * 
     * @author fkieviet
     */
    public interface Executor {
        /**
         * Similar to Runnable.run()
         * 
         * @throws Exception for testing
         */
        void run() throws Exception;
    }

    /**
     * @param ctx see interceptor prototype
     * @return see interceptor prototype
     * @throws Exception propagated
     */
    @AroundInvoke
    public Object onMessage(InvocationContext ctx) throws Exception {
        if (ctx.getMethod().getName().equals("onMessage")) {
            try {
                // Set context
                exportedCtx.set(ctx);

                // If executable message, execute it
                Message m = (Message) ctx.getParameters()[0];
                if (m instanceof ObjectMessage && m.propertyExists(TEST_IN_EXEC) && m.getBooleanProperty(TEST_IN_EXEC)) {
                    Object o = ((ObjectMessage) m).getObject(); 
                    ((Executor) o).run();
                }
                
                return ctx.proceed();
            } finally {
                exportedCtx.set(null);
            }
        } else {
            // If executable message, execute it
            Message m = (Message) ctx.getParameters()[0];
            if (m instanceof ObjectMessage && m.propertyExists(TEST_OUT_EXEC) && m.getBooleanProperty(TEST_OUT_EXEC)) {
                Object o = ((ObjectMessage) m).getObject(); 
                ((Executor) o).run();
            }

            return ctx.proceed();
        }
    }
}
