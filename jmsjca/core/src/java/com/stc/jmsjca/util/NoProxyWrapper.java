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
 * $RCSfile: NoProxyWrapper.java,v $
 * $Revision: 1.1.1.2 $
 * $Date: 2007-01-21 07:52:44 $
 *
 * Copyright 2003-2007 Sun Microsystems, Inc. All Rights Reserved.  
 */

package com.stc.jmsjca.util;

/**
 * A baseclass for wrapper managers, i.e. objects that give out wrappers.
 *
 * @author Frank Kieviet
 * @version $Revision: 1.1.1.2 $
 */
public abstract class NoProxyWrapper  {
//    private static Logger sLog = Logger.getLogger(NoProxyWrapper.class);
    private Class mItf;
    private Object mWrapper;
    private int mCtExceptions;
    private Exception mFirstException;
    private String mSignature;

    /**
     * Initializes the object
     *
     * @param itf interface to expose through the wrapper
     * @param signature String
     */
    protected void init(Class itf, String signature) {
        mItf = itf;
        mSignature = signature;

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
     * Creates a new wrapper and invalidates any existing current wrapper.
     */
    public abstract void createNewWrapper();

    /**
     * Called whenever a call is made on a proxy that is no longer the active proxy (i.e.
     * createNewProxy has been called after obtaining a handle to the proxy). When this
     * method is called, the call is no longer forwarded to delegate or interceptor.
     *
     * @throws javax.jms.IllegalStateException always
     */
    public void invokeOnClosed() throws javax.jms.IllegalStateException {
        throw new javax.jms.IllegalStateException(Str.msg("This {0} is closed",
            getItfClass().getName()));
    }

    /**
     * Sets the active wrapper
     *
     * @param o Object
     */
    public void setWrapper(Object o) {
        mWrapper = o;
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
     * Close the physical object
     */
    public abstract void physicalClose();
}
