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

import java.util.Hashtable;

import javax.naming.Context;
import javax.naming.Name;
import javax.naming.NameParser;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;

/**
 * To bind a javax.naming.Context into JNDI through an administered object.
 * The context can be used to lookup or create JMS destinations.
 * 
 * To be able to create the administered object, the class must be instantiated
 * without any arguments to the constructor; the configuration arguments will
 * be passed to the instantiated object using setter methods (it must be a 
 * java bean).
 * 
 * This class provides the following services: allows instantiation without
 * any arguments and delays creation of the real context until all arguments 
 * have been set, until the first use of the Context.
 * 
 * This class is a wrapper around the real context (called the delegate); any
 * call on the Context is delegated to the delegate. The delegate is obtained
 * using the getDelegate() method that is implemented by classes inherited 
 * from this class.
 * 
 * Classes inheriting from this class should provide the getters and setters
 * and use this configuration information in the getDelegate() method.
 * 
 * @author Frank Kieviet
 * @version $Revision: 1.5 $
 */
public abstract class RAJMSContext implements Context, java.io.Serializable {
//    private static Logger sLog = Logger.getLogger(RAJMSActivationSpec.class);

    /**
     * Constructor 
     */
    public RAJMSContext() {
        
    }
    
    /**
     * Provides access to the real implementation of the context.
     * 
     * @return Delegate
     * @throws NamingException on failure
     */
    protected abstract Context getDelegate() throws NamingException;

    /**
     * @see javax.naming.Context#close()
     */
    public void close() throws NamingException {
        getDelegate().close();
    }

    /**
     * @see javax.naming.Context#getNameInNamespace()
     */
    public String getNameInNamespace() throws NamingException {
        return getDelegate().getNameInNamespace();
    }

    /**
     * @see javax.naming.Context#destroySubcontext(java.lang.String)
     */
    public void destroySubcontext(String name) throws NamingException {
        getDelegate().destroySubcontext(name);
        
    }

    /**
     * @see javax.naming.Context#unbind(java.lang.String)
     */
    public void unbind(String name) throws NamingException {
        getDelegate().unbind(name);
        
    }

    /**
     * @see javax.naming.Context#getEnvironment()
     */
    public Hashtable getEnvironment() throws NamingException {
        return getDelegate().getEnvironment();
    }

    /**
     * @see javax.naming.Context#destroySubcontext(javax.naming.Name)
     */
    public void destroySubcontext(Name name) throws NamingException {
        getDelegate().destroySubcontext(name);
    }

    /**
     * @see javax.naming.Context#unbind(javax.naming.Name)
     */
    public void unbind(Name name) throws NamingException {
        getDelegate().unbind(name);
    }

    /**
     * @see javax.naming.Context#lookup(java.lang.String)
     */
    public Object lookup(String name) throws NamingException {
        return getDelegate().lookup(name);
    }

    /**
     * @see javax.naming.Context#lookupLink(java.lang.String)
     */
    public Object lookupLink(String name) throws NamingException {
        return getDelegate().lookup(name);
    }

    /**
     * @see javax.naming.Context#removeFromEnvironment(java.lang.String)
     */
    public Object removeFromEnvironment(String propName) throws NamingException {
        return removeFromEnvironment(propName);
    }

    /**
     * @see javax.naming.Context#bind(java.lang.String, java.lang.Object)
     */
    public void bind(String name, Object obj) throws NamingException {
        getDelegate().bind(name, obj);
    }

    /**
     * @see javax.naming.Context#rebind(java.lang.String, java.lang.Object)
     */
    public void rebind(String name, Object obj) throws NamingException {
        getDelegate().rebind(name, obj);
    }

    /**
     * @see javax.naming.Context#lookup(javax.naming.Name)
     */
    public Object lookup(Name name) throws NamingException {
        return getDelegate().lookup(name);
    }

    /**
     * @see javax.naming.Context#lookupLink(javax.naming.Name)
     */
    public Object lookupLink(Name name) throws NamingException {
        return getDelegate().lookupLink(name);
    }

    /**
     * @see javax.naming.Context#bind(javax.naming.Name, java.lang.Object)
     */
    public void bind(Name name, Object obj) throws NamingException {
        getDelegate().bind(name, obj);    
    }

    /**
     * @see javax.naming.Context#rebind(javax.naming.Name, java.lang.Object)
     */
    public void rebind(Name name, Object obj) throws NamingException {
        getDelegate().rebind(name, obj);    
    }

    /**
     * @see javax.naming.Context#rename(java.lang.String, java.lang.String)
     */
    public void rename(String oldName, String newName) throws NamingException {
        getDelegate().rename(oldName, newName);    
    }

    /**
     * @see javax.naming.Context#createSubcontext(java.lang.String)
     */
    public Context createSubcontext(String name) throws NamingException {
        return getDelegate().createSubcontext(name);
    }

    /**
     * @see javax.naming.Context#createSubcontext(javax.naming.Name)
     */
    public Context createSubcontext(Name name) throws NamingException {
        return createSubcontext(name);
    }

    /**
     * @see javax.naming.Context#rename(javax.naming.Name, javax.naming.Name)
     */
    public void rename(Name oldName, Name newName) throws NamingException {
        getDelegate().rename(oldName, newName);    
    }

    /**
     * @see javax.naming.Context#getNameParser(java.lang.String)
     */
    public NameParser getNameParser(String name) throws NamingException {
        return getDelegate().getNameParser(name);
    }

    /**
     * @see javax.naming.Context#getNameParser(javax.naming.Name)
     */
    public NameParser getNameParser(Name name) throws NamingException {
        return getDelegate().getNameParser(name);
    }

    /**
     * @see javax.naming.Context#list(java.lang.String)
     */
    public NamingEnumeration list(String name) throws NamingException {
        return getDelegate().list(name);
    }

    /**
     * @see javax.naming.Context#listBindings(java.lang.String)
     */
    public NamingEnumeration listBindings(String name) throws NamingException {
        return getDelegate().listBindings(name);
    }

    /**
     * @see javax.naming.Context#list(javax.naming.Name)
     */
    public NamingEnumeration list(Name name) throws NamingException {
        return getDelegate().list(name);
    }

    /**
     * @see javax.naming.Context#listBindings(javax.naming.Name)
     */
    public NamingEnumeration listBindings(Name name) throws NamingException {
        return getDelegate().listBindings(name);
    }

    /**
     * @see javax.naming.Context#addToEnvironment(java.lang.String, java.lang.Object)
     */
    public Object addToEnvironment(String propName, Object propVal) throws NamingException {
        return getDelegate().addToEnvironment(propName, propVal);
    }

    /**
     * @see javax.naming.Context#composeName(java.lang.String, java.lang.String)
     */
    public String composeName(String name, String prefix) throws NamingException {
        return getDelegate().composeName(name, prefix);
    }

    /**
     * @see javax.naming.Context#composeName(javax.naming.Name, javax.naming.Name)
     */
    public Name composeName(Name name, Name prefix) throws NamingException {
        return getDelegate().composeName(name, prefix);
    }
}
