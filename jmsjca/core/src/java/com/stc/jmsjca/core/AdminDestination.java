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

import com.stc.jmsjca.localization.Localizer;
import com.stc.jmsjca.util.Exc;

import javax.jms.JMSException;


/**
 * Base class for administrative destinations. These are administrative objects that 
 * describe queues and topics. 
 *
 * @author Frank Kieviet
 * @version $Revision: 1.3 $
 */
public abstract class AdminDestination implements javax.jms.Destination, java.io.Serializable {
    private static final Localizer LOCALE = Localizer.get();

    /**
     * Gets the name of the destination
     * 
     * @return name of the destination
     */
    public abstract String getName();
    
    /**
     * @return non-null name
     * @throws JMSException on configuration failure
     */
    public String retrieveCheckedName() throws JMSException {
        String ret = getName();
        if (ret == null || ret.length() == 0) {
            throw Exc.jmsExc(LOCALE.x("E202: The administrative object used as a destination "
                + " is not properly configured: the Name-property is not set."));
        }
        return ret;
    }
}
