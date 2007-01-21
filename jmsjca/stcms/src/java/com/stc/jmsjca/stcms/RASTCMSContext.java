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
 * $RCSfile: RASTCMSContext.java,v $
 * $Revision: 1.1.1.1 $
 * $Date: 2007-01-21 07:52:11 $
 *
 * Copyright 2003-2007 Sun Microsystems, Inc. All Rights Reserved.  
 */

package com.stc.jmsjca.stcms;

import com.stc.jmsjca.core.RAJMSContext;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import java.util.Properties;

/**
 * See {@see com.stc.jmsjca.core.RAJMSContext}
 *
 * @author Frank Kieviet
 * @version $Revision: 1.1.1.1 $
 */
public class RASTCMSContext extends RAJMSContext {
    private String mConnectionURL;
    private String mUserName;
    private String mPassword;
    private Context mDelegate;
    private String mSubContext;
    
    /**
     * Constructor 
     */
    public RASTCMSContext() {
    }

    /**
     * @see com.stc.jmsjca.core.RAJMSContext#getDelegate()
     */
    protected synchronized Context getDelegate() throws NamingException {
        if (mDelegate == null) {
            Properties p = new Properties();
            p.put(Context.INITIAL_CONTEXT_FACTORY, "com.stc.jms.jndispi.InitialContextFactory");
            if (mConnectionURL != null) {
                p.put(Context.PROVIDER_URL, mConnectionURL);
            }
            if (mUserName != null) {
                p.put(Context.SECURITY_PRINCIPAL, mUserName);
            }
            if (mPassword != null) {
                p.put(Context.SECURITY_CREDENTIALS, mPassword);
            }
            Context ctx = new InitialContext(p);
            if (mSubContext != null) {
                ctx = (Context) ctx.lookup(mSubContext);
            }
            mDelegate = ctx;
        }
        return mDelegate;
    }

    /**
     * Getter for password
     *
     * @return String
     */
    public final String getPassword() {
        return this.mPassword;
    }

    /**
     * Setter for password
     *
     * @param password StringThe password to set.
     */
    public final void setPassword(String password) {
        this.mPassword = password;
    }

    /**
     * Getter for username
     *
     * @return String
     */
    public final String getUserName() {
        return this.mUserName;
    }

    /**
     * Setter for username
     *
     * @param userName StringThe userName to set.
     */
    public final void setUserName(String userName) {
        this.mUserName = userName;
    }

    /**
     * Getter for connectionUrl
     *
     * @return String
     */
    public final String getConnectionURL() {
        return this.mConnectionURL;
    }

    /**
     * Setter for connectionUrl
     *
     * @param connectionURL StringThe connectionURL to set.
     */
    public final void setConnectionURL(String connectionURL) {
        this.mConnectionURL = connectionURL;
    }

    /**
     * Getter for subContext
     *
     * @return String
     */
    public final String getSubContext() {
        return this.mSubContext;
    }

    /**
     * Setter for subContext
     *
     * @param subContext StringThe subContext to set.
     */
    public final void setSubContext(String subContext) {
        this.mSubContext = subContext;
    }
}
