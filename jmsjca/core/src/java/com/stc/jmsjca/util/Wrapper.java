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

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.HashSet;
import java.util.Set;

/**
 * <p>This is a universal wrapper around a delegate interface. All methods defined in
 * the delegate interface are implemented in this wrapper through a dynamic proxy.
 * </p>
 *
 * <p>All calls to the methods in the delegate interface are forwarded to either the
 * delegate object or the interceptor object. The interceptor object takes presendence.
 * </p>
 *
 * <p>The wrapper object is the object that exposes the specified interface (the proxy
 * object).</p>
 *
 * <p>This wrapper can be used for connection-type objects; once closed, all calls are
 * illegal on the object. Once the object has been closed, call createProxy() to create
 * a new proxy object; all calls on the old proxy object are now intercepted and cause
 * invokeOnClosed() to be called. This method needs to be overridden and may throw an
 * exception if that is the desired behavior.</p>
 *
 * @author Frank Kieviet
 * @version $Revision: 1.6 $
 */
public abstract class Wrapper implements InvocationHandler {
    private static Logger sLog = Logger.getLogger(Wrapper.class);
    private Class mItf;
    private Object mInterceptor;
    private Object mWrapper;
    private Set mInterceptedMethods;
    private ClassLoader mClassloader;
    private Object mDelegate;
    private int mCtExceptions;
    private Exception mFirstException;
    private String mSignature;

    private static final Localizer LOCALE = Localizer.get();

    /**
     * Initializes the object
     *
     * @param classloader used to set the classloader of the wrapper
     * @param itf interface to expose through the wrapper
     * @param delegate Object
     * @param interceptor object that contains a subset of the methods in the specified
     *   interface; these methods will be called rather than the ones in the interface.
     * @param signature String
     */
    protected void init(ClassLoader classloader, Class itf, Object delegate,
        Object interceptor, String signature) {
        mItf = itf;
        mInterceptor = interceptor;
        mInterceptedMethods = new HashSet();
        mClassloader = classloader;
        mDelegate = delegate;
        mSignature = signature;

        // Cache method names in interceptor
        Method[] methods = mInterceptor.getClass().getMethods();
        for (int i = 0; i < methods.length; i++) {
            mInterceptedMethods.add(methods[i].getName());
        }

        createNewWrapper();
    }

    /**
     * Returns the class that this wrapper is supposed to implement
     *
     * @return class
     */
    public Class getItfClass() {
        return mItf;
    }

    /**
     * Checks to see if the specified classes are equal
     *
     * @param a Class
     * @param b Class
     * @return boolean true if class is specified class
     */
    public static boolean isClass(Class a, Class b) {
        if (a == b || a.getName().equals(b.getName())) {
            return true;
        }
        return false;
    }

    /**
     * Checks to see if the interface class is the one specified
     *
     * @param c Class
     * @return boolean true if class is specified class
     */
    public boolean isItfClass(Class c) {
        return isClass(c, mItf);
    }

    /**
     * Provides access to the delegate, i.e. the object that implements the interface
     *
     * @return delegate
     * @throws Throwable on failure (application specific)
     */
    public Object getDelegate() throws Throwable {
        return mDelegate;
    }

    /**
     * Returns the characteristics of this session (transacted, etc)
     *
     * @return signature
     */
    public String getSignature() {
        return mSignature;
    }

    /**
     * Provides access to the wrapper object, i.e. the object that implements the
     * specified interface
     *
     * @return object that implements interface
     */
    public Object getWrapper() {
        return mWrapper;
    }

    /**
     * Creates a new proxy object and marks this new proxy as the active proxy. All
     * calls on a non-active proxy cause the invokeOnClosed() method to be called.
     */
    public void createNewWrapper() {
        mWrapper = Proxy.newProxyInstance(mClassloader, new Class[] {mItf}, this);
    }

    /**
     * Tool function: returns the wrapper object for the specified proxy object
     *
     * @param o must be a proxy
     * @return wrapper that created the proxy
     */
    public static Wrapper getWrapperFromProxy(Object o) {
        return (Wrapper) Proxy.getInvocationHandler(o);
    }

    /**
     * Called whenever a call is made on a proxy that is no longer the active proxy (i.e.
     * createNewProxy has been called after obtaining a handle to the proxy). When this
     * method is called, the call is no longer forwarded to delegate or interceptor.
     *
     * @param proxy object the method was called on
     * @param method which method to call
     * @param args arguments for the method
     * @throws Throwable caused by calling the method
     */
    public void invokeOnClosed(Object proxy, Method method, Object[] args) throws Throwable {
        if (method.getName().equals("close")) {
            // Ignore duplicate close
        } else {
            throw Exc.illstate(LOCALE.x("E153: This {0} is closed",
                getItfClass().getName()));
        }
    }

    /**
     * Close the physical object
     */
    public void physicalClose() {
        createNewWrapper();
        try {
            Method m = getDelegate().getClass().getMethod("close", new Class[0]);
            m.invoke(getDelegate(), new Object[0]);
        } catch (InvocationTargetException ex1) {
            sLog.warn(LOCALE.x("E094: This {0} could not be closed properly: {1}", getItfClass(),
                ex1.getTargetException()), ex1.getTargetException());
        } catch (Throwable ex) {
            sLog.warn(LOCALE.x("E094: This {0} could not be closed properly: {1}", getItfClass(),
                ex), ex);
        }
    }

    /**
     * Called when an invocation exception has occurred
     *
     * @param ex Throwable
     */
    public void exceptionOccurred(Throwable ex) {
        if (mFirstException == null) {
            if (ex instanceof Exception) {
                mFirstException = (Exception) ex;
            } else {
                mFirstException = new Exception("Runtime exception: " + ex);
                Exc.setCause(mFirstException, ex);
            }
        }
        mCtExceptions++;
    }

    /**
     * True if any exception has occurred
     *
     * @return true if occurred
     */
    public boolean hasExceptionOccurred() {
        return mCtExceptions > 0;
    }

    /**
     * Returns the first exception that was thrown on any of the intercepted calls.
     *
     * @return java.lang.Exception
     */
    public Exception getFirstException() {
        return mFirstException;
    }

    /**
     * Returns the total count of exceptions that occurred on any intercepted call.
     *
     * @return number of exceptions
     */
    public int getExceptionCount() {
        return mCtExceptions;
    }

    /**
     * Overrides equal() in Object; this function is also called when the equals() method
     * is called on the proxy object. Consequently, consider a statement like this
     *   Wrapper w;
     *   a = w.getWrapper();
     *   a == a // always true
     *   a.equals(a)  // NOT true!
     * The problem is that the passed in object is in fact the proxy object. To avoid
     * this problem, the implementation must check explicitly to see if the passed object
     * is in fact the proxy.
     *
     * @param other object to compare to
     * @return true if identical
     */
    public boolean equals(Object other) {
        if (other == this || other == mWrapper) {
            return true;
        }
        return false;
    }

    /**
     * @see java.lang.Object#hashCode()
     */
    public int hashCode() {
        int ret = 37;
        ret = Str.hash(ret, mDelegate);
        return ret;
    }

    /**
     * Required by the InvocationHandler interface; called on each invocation of the
     * interface. Delegates the call to either the delegate or the interceptor.
     *
     * @param proxy object the method was called on
     * @param method which method to call
     * @param args arguments for the method
     * @return return value of the called method
     * @throws Throwable caused by calling the method
     */
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        Object ret = null;
        Object target = null;

        if (proxy != mWrapper) {
            invokeOnClosed(proxy, method, args);
        } else {

            // Check if interceptor method exists
            if (!mInterceptedMethods.contains(method.getName())) {
                // Name doesn't exist: don't even bother to try to locate the method
                target = getDelegate();
            } else {
                try {
                    // Locate the method; may fail if there is a method with the same name
                    // but mismatching parameters (this probably IS an error though --
                    // consider throwing an exception)
                    method = mInterceptor.getClass().getMethod(method.
                        getName(), method.getParameterTypes());
                    target = mInterceptor;
                } catch (NoSuchMethodException ignore) {
                    // ignore: name existed but likely parameter types mismatch
                    target = getDelegate();
                }
            }

            // Delegate method
            try {
                ret = method.invoke(target, args);  // <<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<
            } catch (InvocationTargetException ex) {
                exceptionOccurred(ex.getTargetException());
                throw ex.getTargetException();
            } catch (IllegalArgumentException ex) {
                throw ex;
            } catch (IllegalAccessException ex) {
                throw ex;
            }
        }
        return ret;
    }
}
