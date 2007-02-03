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

import javax.jms.JMSException;
import javax.jms.ObjectMessage;

import java.io.Serializable;

/**
 * See WMessage
 *
 * @author Frank Kieviet
 * @version $Revision: 1.4 $
 */
public class WObjectMessageOut extends WMessageOut implements ObjectMessage {
    private ObjectMessage mDelegate;
    
    /**
     * Constructor
     * 
     * @param delegate real msg
     */
    public WObjectMessageOut(ObjectMessage delegate) {
        super(delegate);
        mDelegate = delegate;
    }

    /**
     * @see javax.jms.ObjectMessage#getObject()
     */
    public Serializable getObject() throws JMSException {
        return mDelegate.getObject();
    }

    /**
     * @see javax.jms.ObjectMessage#setObject(java.io.Serializable)
     */
    public void setObject(Serializable arg0) throws JMSException {
        mDelegate.setObject(arg0);
    }

}
