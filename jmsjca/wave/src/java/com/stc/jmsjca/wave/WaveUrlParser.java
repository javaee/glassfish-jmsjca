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

package com.stc.jmsjca.wave;

import com.stc.jmsjca.util.ConnectionUrl;
import com.stc.jmsjca.util.UrlParser;

import java.util.ArrayList;
import java.util.Properties;
import java.util.StringTokenizer;

/**
 * The format of the ConnectionURL is as follows:
 *   ConnectionURL := url[,url]*
 *   url := protocol://server:host[?options]
 *   protocol := tcp,stream,http,ssl
 *   options := key=value[&key=value]*       
 *
 * @author fkieviet
 * @version $Revision: 1.6 $
 */
public class WaveUrlParser extends ConnectionUrl {
    private UrlParser[] mParsers;
    
    /**
     * Constructor
     * 
     * @param s connection url string
     */
    public WaveUrlParser(String s) {
        ArrayList<UrlParser> urls = new ArrayList<UrlParser>();
        for (StringTokenizer it = new StringTokenizer(s, ","); it.hasMoreTokens();) {
            String url = it.nextToken();
            urls.add(new UrlParser(url));
        }
        mParsers = urls.toArray(new UrlParser[urls.size()]);
    }

    /**
     * @see com.stc.jmsjca.util.ConnectionUrl#getQueryProperties(java.util.Properties)
     */
    @Override
    public void getQueryProperties(Properties toAddTo) {
        UrlParser[] urls = getUrlParsers();
        for (int i = 0; i < urls.length; i++) {
            urls[i].getQueryProperties(toAddTo);
        }
    }

    /**
     * Returns the parsers that constitute the URL
     * 
     * @return parsers
     */
    public UrlParser[] getUrlParsers() {
        return mParsers;
    }

    /**
     * Constructs a comma delimited string of protocol://host:port
     * 
     * @return String
     */
    public String getWaveUrlSet() {
        UrlParser[] urls = getUrlParsers();
        StringBuffer buf = new StringBuffer();
        for (int i = 0; i < urls.length; i++) {
            if (i != 0) {
                buf.append(",");
            }
            buf.append(urls[i].getProtocol() + "://" + urls[i].getHost() + ":" + urls[i].getPort());
        }
        return buf.toString();
    }
    
    /**
     * @see com.stc.jmsjca.util.ConnectionUrl#toString()
     */
    @Override
    public String toString() {
        UrlParser[] urls = getUrlParsers();
        StringBuffer buf = new StringBuffer();
        for (int i = 0; i < urls.length; i++) {
            if (i != 0) {
                buf.append(",");
            }
            buf.append(urls[i].toString());
        }
        return buf.toString();
    }
}
