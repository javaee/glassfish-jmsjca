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
 * $RCSfile: AckHandler.java,v $
 * $Revision: 1.1.1.2 $
 * $Date: 2007-01-21 07:52:43 $
 *
 * Copyright 2003-2007 Sun Microsystems, Inc. All Rights Reserved.  
 */

package com.stc.jmsjca.core;

import javax.jms.JMSException;
import javax.jms.Message;

/**
 * A callback that can be installed in a message that should be triggered when 
 * acknowledge() is called on the message. The message must make sure that the 
 * callback is called only once.
 *
 * @author Frank Kieviet
 * @version $Revision: 1.1.1.2 $
 */
public abstract class AckHandler {
    
    /**
     * @param isRollbackOnly true if setRollbackOnly was called first
     * @param m message on which this was called
     * @throws JMSException delegated
     */
    public abstract void ack(boolean isRollbackOnly, Message m) throws JMSException;
}
