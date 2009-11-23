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

package com.stc.jmsjca.unifiedjms;

import javax.jms.JMSException;

/**
 * Administrative topic
 *
 * @author Frank Kieviet
 * @version $Revision$
 */
public class UnifiedAdminTopic extends UnifiedAdminDestination implements javax.jms.Topic, java.io.Serializable {
    /**
     * @see javax.jms.Topic#getTopicName()
     */
    public String getTopicName() throws JMSException {
        return getName();
    }

    /**
     * @see com.stc.jmsjca.core.AdminDestination#isQueue()
     */
    @Override
    public boolean isQueue() {
        return false;
    }

    /**
     * @see com.stc.jmsjca.core.AdminDestination#isTopic()
     */
    @Override
    public boolean isTopic() {
        return true;
    }
}
