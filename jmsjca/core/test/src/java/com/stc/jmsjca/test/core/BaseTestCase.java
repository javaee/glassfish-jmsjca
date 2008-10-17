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

package com.stc.jmsjca.test.core;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import junit.framework.TestCase;
import junit.framework.TestResult;

/**
 * Adds functionality to skip tests
 *
 * @author fkieviet
 * @version $Revision: 1.6 $
 */
public abstract class BaseTestCase extends TestCase {
    private List mAsyncErrors = new ArrayList();
    
    public interface JMSTestEnv {
        String getContainerID();
        Properties getJmsServerProperties();
    }
    
    public BaseTestCase() {
        
    }
    
    public BaseTestCase(String name) {
        super(name);
    }
    
    /**
     * @param t error
     */
    public void setAsyncError(Throwable t) {
        synchronized (mAsyncErrors) {
            mAsyncErrors.add(t);
        }
    }
    
    /**
     * @throws Exception if there were any async exceptions
     */
    public void assertNoAsyncErrors() throws Exception {
        synchronized (mAsyncErrors) {
            if (!mAsyncErrors.isEmpty()) {
                throw new Exception("There were " + mAsyncErrors.size()
                    + " async exceptions. The first is: " + mAsyncErrors.get(0),
                    (Throwable) mAsyncErrors.get(0));
            }
        }
    }
    
    /**
     * Runs the test case except if a similarly named method prefixed with
     * skip_ or disabled_ exists.
     */
    public void run(TestResult result) {
        try {
            getClass().getMethod("skip_" + getName(), new Class[] {});
            // Specially marked method apparently exists; skip it
            return;
        } catch (Exception ignore) {
        }

        try {
            // Specially marked method apparently exists; skip it
            getClass().getMethod("disabled_" + getName(), new Class[] {});
            return;
        } catch (Exception ignore) {
        }

        // Apparently specially marked method does not exist; execute it.
        super.run(result);
    }
    
    public boolean isFastTest() {
        return System.getProperty("fasttest", "true").equalsIgnoreCase("true");
    }
}
