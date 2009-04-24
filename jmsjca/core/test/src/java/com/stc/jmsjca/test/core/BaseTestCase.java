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

import javax.jms.Connection;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import junit.framework.TestCase;
import junit.framework.TestResult;

/**
 * Adds functionality to skip tests
 *
 * @author fkieviet
 * @version $Revision: 1.9 $
 */
public abstract class BaseTestCase extends TestCase {
    private List mAsyncErrors = new ArrayList();
    /**
     * Properties of the JMS server and container
     */
    protected Properties mServerProperties;
    
    public void setUp() throws Exception {
        getServerProperties();
    }
    
    /**
     * @return server properties (JMS server and container)
     */
    public Properties getServerProperties() throws Exception {
        if (mServerProperties == null) {
            // Default location:
            String default_ = System.getProperty("user.home") + File.separator + "jmsjca.properties";

            // Override property:
            final String jmsPropName = "jmsjca.properties.path";

            String file = System.getProperty(jmsPropName, default_);

            // Load jmsjca.properties.path
            InputStream in = null;
            try {
                Properties p = new Properties();
                in = new FileInputStream(file);
                p.load(in);
                mServerProperties = p;
            } finally {
                safeClose(in);
            }
        } 
        return mServerProperties;

    }
    
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
     * Tool function: ensures that the specified stream is closed
     *
     * @param stream to close; maybe null
     */
    public static void safeClose(InputStream stream) {
        if (stream != null) {
            try {
                stream.close();
            } catch (Exception ex) {
                // ignore
            }
        }
    }

    /**
     * Tool function: ensures that the specified stream is closed
     *
     * @param stream to close; maybe null
     */
    public static void safeClose(Connection c) {
        if (c != null) {
            try {
                c.close();
            } catch (Exception ex) {
                // ignore
            }
        }
    }

    /**
     * Tool function: ensures that the specified stream is closed
     *
     * @param stream to close; maybe null
     */
    public static void safeClose(OutputStream stream) {
        if (stream != null) {
            try {
                stream.close();
            } catch (Exception ex) {
                // ignore
            }
        }
    }
//    protected Properties load(String systemPropertiesPropertyName, String default_) throws Exception {
//        // The JMS server properties
//        String propfile = System.getProperty(systemPropertiesPropertyName, default_);
//
//        if (propfile == null) {
//            throw new Exception("Property [" + systemPropertiesPropertyName + "] not defined");
//        }
//
//        // Load jmsjca.properties.path
//        InputStream in = null;
//        try {
//            Properties p = new Properties();
//            in = new FileInputStream(propfile);
//            p.load(in);
//            return p;
//        } finally {
//            safeClose(in);
//        }
//    }
    
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

        try {
            // Specially marked method apparently exists; evaluate it, and maybe skip it
            Method check = getClass().getMethod("shouldRun_" + getName(), new Class[] {});
            try {
                Boolean shouldRun = (Boolean) check.invoke(this, new Object[0]);
                if (!shouldRun.booleanValue()) {
                    return;
                }
            } catch (Exception e) {
                System.err.println("Unexpected exception shoulRun_" + getName() + ": " + e);
                e.printStackTrace();
            }
        } catch (Exception ignore) {
        }
        
        // Is class marked as _only? If so, only run those tests that are suffixed with
        // _only
        boolean only = false;
        Method[] methods = getClass().getMethods();
        for (int i = 0; i < methods.length; i++) {
            if (methods[i].getName().endsWith("_only")) {
                only = true;
            }
        }
        if (only && !getName().endsWith("_only")) {
            System.out.println("Skipping " + getName() + ": there is at least one _only method");
            return;
        }

        // Apparently specially marked method does not exist; execute it.
        super.run(result);
    }
    
    public boolean isFastTest() {
        return System.getProperty("fasttest", "true").equalsIgnoreCase("true");
    }
}
