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

import javax.resource.ResourceException;

/**
 * Local Transaction that doesn't do anything; used for non-xa non-transacted mode where
 * the transaction mode in ra.xml is *not* set to NoTransaction.
 *
 * @author Frank Kieviet
 * @version $Revision: 1.5 $
 */
public class XLocalTransactionNOP implements javax.resource.spi.LocalTransaction {
    /**
     * Constructor
     */
    public XLocalTransactionNOP() {
    }

    /**
     * begin
     *
     * @throws ResourceException never
     */
    public void begin() throws ResourceException {
    }

    /**
     * commit
     *
     * @throws ResourceException on JMSException
     */
    public void commit() throws ResourceException {
    }

    /**
     * rollback
     *
     * @throws ResourceException on JMSException
     */
    public void rollback() throws ResourceException {
    }
}
