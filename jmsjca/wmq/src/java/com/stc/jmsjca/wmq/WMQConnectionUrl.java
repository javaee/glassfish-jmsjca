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

package com.stc.jmsjca.wmq;

import com.stc.jmsjca.util.ConnectionUrl;
import com.stc.jmsjca.util.UrlParser;

import java.util.Properties;

/**
 * The format of the ConnectionURL is as follows:
 *   ConnectionURL := protocol://server:host[?options]
 *   protocol := wmq, wmqs
 *   options := key=value[&key=value]*
 *              QueueManager=QM_localhost&
 *              TransportType=JMSC_MQJMS_TP_CLIENT_MQ_TCPIP(1) or 
 *              JMSC.MQJMS_TP_BINDINGS_MQ(0)           
 *
 * @author  cye
 * @version $Revision: 1.1.1.2 $
 */
public class WMQConnectionUrl extends ConnectionUrl {
    private UrlParser mUrlParser;
    
    /**
     * Constructor
     * 
     * @param s connection url string
     */
    public WMQConnectionUrl(String s) {
        mUrlParser = new UrlParser(s);
    }

    /**
     * @see com.stc.jmsjca.util.ConnectionUrl#getQueryProperties(java.util.Properties)
     */
    public void getQueryProperties(Properties props) {
        mUrlParser.getQueryProperties(props);
    }

    /**
     * Returns the parsers that constitute the URL
     * 
     * @return parsers
     */
    public UrlParser getUrlParser() {
        return mUrlParser;
    }
}
