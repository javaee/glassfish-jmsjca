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

import com.stc.jmsjca.localization.LocalizedString;

import javax.ejb.EJBException;
import javax.jms.IllegalStateException;
import javax.jms.JMSException;
import javax.jms.JMSSecurityException;
import javax.resource.ResourceException;
import javax.resource.spi.InvalidPropertyException;
import javax.resource.spi.SecurityException;
import javax.transaction.xa.XAException;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Method;

/**
 * <p>Tool class to facilitate exception throwing and conversions</p>
 *
 * <p>Supports the cause-attribute in execptions without compile-time dependencies
 * on JDK1.4</p>
 *
 * @author Frank Kieviet
 * @version $Revision: 1.10 $
 */
public class Exc {

    /**
     * Creates a new exception
     *
     * @param msg error text
     * @return new exception
     */
    public static ResourceException rsrcExc(LocalizedString msg) {
        return new ResourceException(msg.toString());
    }

    /**
     * Creates a new exception with cause and linked exception filled in
     *
     * @param msg error text
     * @param cause the exception that caused this exception
     * @return new exception
     */
    public static ResourceException rsrcExc(LocalizedString msg, Exception cause) {
        if (cause instanceof JMSException) {
            Exc.checkLinkedException(cause);
        }
        
        ResourceException ret;
        if (cause instanceof JMSSecurityException || cause instanceof SecurityException) { 
            ret = new SecurityException(msg.toString());
        } else {
            ret = new ResourceException(msg.toString());
        }
        //ret.setLinkedException(cause);
        setCause(ret, cause);
        return ret;
    }

    /**
     * Creates a new exception with cause and linked exception filled in
     *
     * @param errcode See XAException
     * @param cause the exception that caused this exception
     * @return new exception
     */
    public static XAException xaExc(int errcode, Exception cause) {
        if (cause instanceof JMSException) {
            Exc.checkLinkedException(cause);
        }

        XAException ret = new XAException(errcode);
        setCause(ret, cause);
        return ret;
    }

    /**
     * Creates a new exception with cause and linked exception filled in
     *
     * @param msg error text
     * @param cause the exception that caused this exception
     * @return new exception
     */
    public static JMSException jmsExc(LocalizedString msg, Exception cause) {
        if (cause instanceof JMSException) {
            checkLinkedException(cause);
        }
        JMSException ret;
        if (cause instanceof JMSSecurityException || cause instanceof SecurityException) {
            ret = new JMSSecurityException(msg.toString());
        } else {
            ret = new JMSException(msg.toString());
        }
        setCause(ret, cause);
        return ret;
    }
    
    /**
     * Creates a new exception
     *
     * @param msg error text
     * @return new exception
     */
    public static JMSException jmsExc(LocalizedString msg) {
        return new JMSException(msg.toString());
    }
    
    /**
     * Creates a new exception
     *
     * @param msg error text
     * @return new exception
     */
    public static Exception exc(LocalizedString msg) {
        return new Exception(msg.toString());
    }
    
    /**
     * Creates a new exception
     *
     * @param msg error text
     * @param cause Throwable
     * @return new exception
     */
    public static Exception exc(LocalizedString msg, Throwable cause) {
        if (cause instanceof JMSException) {
            Exc.checkLinkedException(cause);
        }
        return new Exception(msg.toString(), cause);
    }
    
    /**
     * Creates a new exception
     *
     * @param msg error text
     * @return new exception
     */
    public static RuntimeException rtexc(LocalizedString msg) {
        return new RuntimeException(msg.toString());
    }
    
    /**
     * Creates a new exception
     *
     * @param msg error text
     * @param cause Throwable
     * @return new exception
     */
    public static RuntimeException rtexc(LocalizedString msg, Throwable cause) {
        if (cause instanceof JMSException) {
            Exc.checkLinkedException(cause);
        }
        return new RuntimeException(msg.toString(), cause);
    }
    
    /**
     * Creates a new exception
     *
     * @param msg error text
     * @return new exception
     */
    public static InvalidPropertyException invprop(LocalizedString msg) {
        return new InvalidPropertyException(msg.toString());
    }
    
    /**
     * Creates a new exception
     *
     * @param msg error text
     * @param cause cause
     * @return new exception
     */
    public static InvalidPropertyException invprop(LocalizedString msg, Throwable cause) {
        if (cause instanceof JMSException) {
            Exc.checkLinkedException(cause);
        }
        return new InvalidPropertyException(msg.toString(), cause);
    }
    
    /**
     * Creates a new exception
     *
     * @param msg error text
     * @return new exception
     */
    public static IllegalArgumentException illarg(LocalizedString msg) {
        return new IllegalArgumentException(msg.toString());
    }
    
    /**
     * Creates a new exception
     *
     * @param msg error text
     * @param cause cause
     * @return new exception
     */
    public static IllegalArgumentException illarg(LocalizedString msg, Throwable cause) {
        if (cause instanceof JMSException) {
            Exc.checkLinkedException(cause);
        }
        return new IllegalArgumentException(msg.toString(), cause);
    }
    
    /**
     * Creates a new exception
     *
     * @param msg error text
     * @return new exception
     */
    public static IllegalStateException illstate(LocalizedString msg) {
        return new IllegalStateException(msg.toString());
    }
    
    /**
     * Creates a new exception
     *
     * @param msg error text
     * @param cause cause
     * @return new exception
     */
    public static IllegalStateException illstate(LocalizedString msg, Throwable cause) {
        if (cause instanceof JMSException) {
            Exc.checkLinkedException(cause);
        }
        IllegalStateException ret = new IllegalStateException(msg.toString());
        setCause(ret, cause);
        return ret;
    }
    
    /**
     * If the specified exception has a linked exception but no cause, this will copy
     * the linked exception into the exception's cause field. This is so that when a
     * JMSException is thrown by a provider who doesn't set the cause field, and the 
     * application code does not look at the linkedException, the root cause is properly
     * propagated afterall. 
     * 
     * @param cause exception to check
     */
    public static void checkLinkedException(Throwable cause) {
        try {
            Class c = cause.getClass();
            Method m = c.getMethod("getLinkedException", new Class[] {});
            Exception linked = (Exception) m.invoke(cause, new Object[] {});
            
            if (linked != null) {
                m = c.getMethod("getCause", new Class[] {});
                Throwable orgcause = (Throwable) m.invoke(cause, new Object[] {});
                if (orgcause == null) {
                    m = c.getMethod("initCause", new Class[] {Throwable.class});
                    m.invoke(cause, new Object[] {linked});
                }
            }
            
        } catch (Exception ex) {
            // ignore
        }
    }

    /**
     * Tool function: uses the JDK 1.4 cause field to propagate the causing
     * exception. Cause will be ignored in JDK 1.3.
     *
     * @param toSetOn exception to change
     * @param cause exception to set as cause (may be null)
     * @return exception passed in
     */
    public static Exception setCause(Exception toSetOn, Throwable cause) {
        if (cause != null) {
            checkLinkedException(cause);

            try {
                Class c = toSetOn.getClass();
                Method m = c.getMethod("initCause", new Class[] {Throwable.class});
                m.invoke(toSetOn, new Object[] {cause});
            } catch (Exception ex) {
                // ignore
            }
        }
        return toSetOn;
    }

    /**
     * Tool function: will set the linked exception. Als uses the JDK 1.4 cause field to
     * propagate the causing exception. Cause will be ignored in JDK 1.3.
     *
     * @param toSetOn exception to change
     * @param cause exception to set as cause (may be null)
     * @return exception passed in
     */
    public static ResourceException setLinkedExc(ResourceException toSetOn, Exception cause) {
        setCause(toSetOn, cause);
        //toSetOn.setLinkedException(cause);
        return toSetOn;
    }
    
    /**
     * Marker for a failed consumer creation (clientID or durable subscriber already
     * exists exception)
     */
    public static class ConsumerCreationException extends JMSException {
        /**
         * @param e exception to propagate
         */
        public ConsumerCreationException(JMSException e) {
            super(e.getMessage());
            initCause(e);
        }
    }

    /**
     * Gets the stacktrace of an exception
     * 
     * @param e exception to convert
     * @return String 
     */
    public static String getStackTrace(Exception e) {
        StringWriter s = new StringWriter();
        PrintWriter p = new PrintWriter(s);
        e.printStackTrace(p);
        p.flush();
        return s.getBuffer().toString();
    }

    /**
     * Some exceptions don't have the cause set... 
     * 
     * @param ex exception to inspect
     */
    public static void fixup(RuntimeException ex) {
        if (ex.getCause() == null) {
            if (ex instanceof EJBException) {
                EJBException e = (EJBException) ex;
                if (e.getCausedByException() != null) {
                    ex.initCause(e.getCausedByException());
                }
            }
        }
    }
}
