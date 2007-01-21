/*
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the "License").  You may not use this file except
 * in compliance with the License.
 *
 * You can obtain a copy of the license at
 * https://glassfish.dev.java.net/public/CDDLv1.0.html.
 * See the License for the specific language governing
 * permissions and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL
 * HEADER in each file and include the License file at
 * https://glassfish.dev.java.net/public/CDDLv1.0.html.
 * If applicable add the following below this CDDL HEADER,
 * with the fields enclosed by brackets "[]" replaced with
 * your own identifying information: Portions Copyright
 * [year] [name of copyright owner]
 */
/*
 * $RCSfile: BaseTestCase.java,v $
 * $Revision: 1.3 $
 * $Date: 2007-01-21 17:51:57 $
 *
 * Copyright 2003-2007 Sun Microsystems, Inc. All Rights Reserved.  
 */

package com.stc.jmsjca.test.core;

import junit.framework.TestCase;
import junit.framework.TestResult;

/**
 * Adds functionality to skip tests
 *
 * @author fkieviet
 * @version $Revision: 1.3 $
 */
public abstract class BaseTestCase extends TestCase {
    
    public BaseTestCase() {
        
    }
    
    public BaseTestCase(String name) {
        super(name);
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
