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
import com.stc.jmsjca.util.Str;

import javax.jms.JMSException;

import java.util.Properties;

/**
 * Base class for administrative destinations. These are administrative objects that 
 * describe queues and topics. 
 *
 * @author Frank Kieviet
 * @version $Revision: 1.5 $
 */
public abstract class AdminDestination implements javax.jms.Destination, java.io.Serializable {
    private static final Localizer LOCALE = Localizer.get();
    private String mName;
    private String mOptions; 

    /**
     * @param name destination name
     */
    public void setName(String name) {
        mName = name;
    }
    
    /**
     * Gets the name of the destination
     * 
     * @return name of the destination
     */
    public String getName() {
        return mName;
    }
    
    /**
     * @param options options
     */
    public void setOptions(String options) {
        mOptions = options;
    }
    
    /**
     * Gets the optional settings for creating the destination
     * 
     * @return options string
     */
    public String getOptions() {
        return mOptions;
    }
    
    
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

    /**
     * @return the Options field in the form of a properties set
     */
    public Properties retrieveProperties() throws JMSException {
        if (Str.empty(getOptions())) {
            return null;
        } else {
            Properties p = new Properties();
            Str.deserializeProperties(Str.parseProperties(Options.SEP, getOptions()), p);
            return p;
        }
    }

    /**
     * @return true if this is a queue
     */
    public abstract boolean isQueue();

    /**
     * @return true if this is a topic
     */
    public abstract boolean isTopic();
}
