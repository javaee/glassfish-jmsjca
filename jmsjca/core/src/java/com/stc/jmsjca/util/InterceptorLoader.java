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
import com.stc.jmsjca.localization.Localizer;

import javax.interceptor.AroundInvoke;
import javax.interceptor.InvocationContext;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;

/**
 * Loads interceptor classes through a mechanism that is similar to JDK6's ServiceProvider:
 * interceptors should be listed in a file in META-INF/services. The name of the file should
 * be the service name. There is a default for that, and this can be overridden.
 * 
 * @see Options.Interceptor#DEFAULT_SERVICENAME
 *  
 * @author fkieviet
 */
public final class InterceptorLoader {
    private static Logger sLog = Logger.getLogger(InterceptorLoader.class.getName());
    private static final Localizer LOCALE = Localizer.get();
    
    private InterceptorLoader() {
    }
    
    /**
     * Gets the interceptor method out of the specified class
     * 
     * @param c class to parse
     * @return method
     * @throws Exception on any failure (there MUST be a single method)
     */
    public static Method getInterceptor(Class<?> c) throws Exception {
        List<Method> ret = new ArrayList<Method>();
        
        Method[] methods = c.getMethods();
        for (Method method : methods) {
            AroundInvoke annotation = method.getAnnotation(AroundInvoke.class);
            if (annotation != null) {
                Class<?>[] parameterTypes = method.getParameterTypes();
                if (parameterTypes.length != 1) {
                    throw Exc.exc(LOCALE.x("E210: Incorrect number of parameters to interceptor " +
                        "method {0} in class {1}: {2}, should be 1", method, c, parameterTypes.length));
                }
                if (!parameterTypes[0].equals(InvocationContext.class)) {
                    throw Exc.exc(LOCALE.x("E211: Incorrect parameter to interceptor " +
                        "method {0} in class {1}: {2}, should be {3}", method, c
                        , parameterTypes[0], InvocationContext.class));
                }
                ret.add(method);
            }
        }
        
        if (ret.size() > 1) {
            throw Exc.exc(LOCALE.x("E208: Found multiple ({0}) interceptors in class {1}", ret.size(), c));
        }
        if (ret.size() == 0) {
            throw Exc.exc(LOCALE.x("E209: Found no interceptors in class {0}", c));
        }
        return ret.get(0);
    }

    private static void addTo(List<URL> addto, Enumeration<URL> toadd) {
        for (; toadd.hasMoreElements();) {
            addto.add(toadd.nextElement());
        }
    }
    
    /**
     * Loads interceptor classes through the service provider mechanism
     * 
     * @return classes
     * @throws Exception propagated
     */
    public static HashMap<Class<?>, InterceptorInfo> getInterceptors(String svcname) throws Exception {
        HashMap<Class<?>, InterceptorInfo> ret = new HashMap<Class<?>, InterceptorInfo>();
        ClassLoader loader1 = InterceptorLoader.class.getClassLoader();
        getInterceptors(loader1, svcname, ret);
        
        ClassLoader loader2 = Thread.currentThread().getContextClassLoader();
        if (loader2 != null && loader1 != loader2) {
            getInterceptors(loader2, svcname, ret);
        }
        
        return ret;
    }
    
    private static void getInterceptors(ClassLoader loader, String svcname
        , HashMap<Class<?>, InterceptorInfo> toAddTo) throws Exception {
        
        // Find URLs
        List<URL> urls = new ArrayList<URL>();
        addTo(urls, loader.getResources(Options.Interceptor.SERVICEPREFIX + svcname));
        
        // Load classnames
        for (URL url : urls) {
            loadInterceptorsFromURL(toAddTo, url, loader);
        }
    }
    
    private static int processServiceLine(URL u, BufferedReader r, int iLine, 
        HashMap<Class<?>, InterceptorInfo> toAddTo, ClassLoader loader) throws Exception {
        
        String line = r.readLine();
        if (line == null) {
            return -1;
        }
        
        if (sLog.isDebugEnabled()) {
            sLog.debug("Interpreting line " + line + " from " + u);
        }
        
        // Remove comments
        int ci = line.indexOf('#');
        if (ci >= 0) {
            line = line.substring(0, ci);
        }
        line = line.trim();
        
        
        int n = line.length();
        if (n != 0) {
            // Sanity check: space/tab
            if ((line.indexOf(' ') >= 0) || (line.indexOf('\t') >= 0)) {
                throw Exc.exc(LOCALE.x("E212: line {0} of {1} contains an incorrectly formatted line: {2}"
                    , iLine, u, line));
            }
            
            // Check that each character is a valid identifier character
            int cp = line.codePointAt(0);
            if (!Character.isJavaIdentifierStart(cp)) {
                throw Exc.exc(LOCALE.x("E212: line {0} of {1} contains an incorrectly formatted line: {2}"
                    , iLine, u, line));
            }
            for (int i = Character.charCount(cp); i < n; i += Character.charCount(cp)) {
                cp = line.codePointAt(i);
                if (!Character.isJavaIdentifierPart(cp) && (cp != '.'))  {
                    throw Exc.exc(LOCALE.x("E212: line {0} of {1} contains an incorrectly formatted line: {2}"
                        , iLine, u, line));
                }
            }

            // Load class and method
            try {
                if (sLog.isDebugEnabled()) {
                    sLog.debug("Trying to load " + line);
                }
                
                Class<?> class1 = loader.loadClass(line);
                String svcDescriptorURL = u.toExternalForm();

                
                if (toAddTo.get(class1) != null) {
                    // Ignore, but warn if there are two the same classnames in two different service descriptors 
                    InterceptorInfo alreadyThere = toAddTo.get(class1);
                    if (alreadyThere.getSvcDescriptorURL().equals(svcDescriptorURL) && alreadyThere.getLine() == iLine) {
                        // Same line in the same descriptor; descriptor likely loaded multiple times
                    } else {
                        sLog.warn(LOCALE.x("E214: Interceptor {0} is a duplicate of {1}... will be ignored."
                            , alreadyThere.getSvcDescriptorURL() + ":" + alreadyThere.getLine()));
                    }
                } else {
                    InterceptorInfo c = new InterceptorInfo(class1, svcDescriptorURL, iLine);
                    c.setMethod(getInterceptor(class1));
                    toAddTo.put(class1, c);
                }
            } catch (Exception e) {
                throw Exc.exc(LOCALE.x("E215: Failed to load {0}: {1}", line, e), e);
            }
        }
        return iLine + 1;
    }
    
    private static void loadInterceptorsFromURL(HashMap<Class<?>, InterceptorInfo> toAddTo
        , URL u, ClassLoader loader) throws Exception {
        
        InputStream in = null;
        BufferedReader r = null;
        try {
            in = u.openStream();
            r = new BufferedReader(new InputStreamReader(in, "utf-8"));
            int lc = 1;
            while ((lc = processServiceLine(u, r, lc, toAddTo, loader)) >= 0) {
            }
        } catch (IOException e) {
            throw Exc.exc(LOCALE.x("E213: could not read {0}: {1}", u, e), e);
        } finally {
            safeClose(r);
            safeClose(in);
        }
    }
    
    /**
     * Closes a closeable
     * 
     * @param c to close
     */
    public static void safeClose(Closeable c) {
        if (c != null) {
            try {
                c.close();
            } catch (Exception ignore) {
                // ignore
            }
        }
    }
}
