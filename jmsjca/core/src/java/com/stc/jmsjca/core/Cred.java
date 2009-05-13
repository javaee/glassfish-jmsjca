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

import javax.resource.spi.security.PasswordCredential;
import javax.security.auth.Subject;

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Iterator;
import java.util.Set;

/**
 * Utilities that have to do with password credentials
 *
 * @author Frank Kieviet
 * @version $Revision: 1.7 $
 */
public final class Cred {
    private Cred() {
    }

    /**
     * Checks two objects for equality
     *
     * @param a Object
     * @param b Object
     * @return boolean
     */
    public static boolean isEqual(Object a, Object b) {
        if (a == null) {
            return b == null;
        } else {
            return a.equals(b);
        }
    }
    
    /**
     * Extracts a password credential out of a Subject; returns null if there's none
     * found, or the one found is is empty.
     *
     * @param mcf XManagedConnectionFactory
     * @param subject Subject
     * @throws javax.resource.ResourceException failed
     * @return PasswordCredential
     */
    public static PasswordCredential extractPasswordCredential(
        final XManagedConnectionFactory mcf, final Subject subject)
        throws javax.resource.ResourceException {

        if (subject == null) {
            return null;
        } else {
            if (subject.getPrivateCredentials().isEmpty()) {
                return null;
            }
            
            // Subject is specified; find a matching PasswordCredential
            PasswordCredential ret = (PasswordCredential) AccessController.doPrivileged(
                new PrivilegedAction<Object>() {
                public Object run() {
                    Set<PasswordCredential> creds = subject.getPrivateCredentials(PasswordCredential.class);
                    for (Iterator<PasswordCredential> iter = creds.iterator(); iter.hasNext();/*-*/) {
                        PasswordCredential pc = iter.next();
                        if (pc.getManagedConnectionFactory() != null
                            && pc.getManagedConnectionFactory().equals(mcf)) {
                            return pc;
                        }
                    }
                    return null;
                }
            });
            if (ret != null && ret.getUserName() != null && ret.getUserName().length() > 0) {
                return ret;
            } else {
                return null;
            }
        }
    }
}
