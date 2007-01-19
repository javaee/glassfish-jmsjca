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
 * $RCSfile: SunOneUrlParser.java,v $
 * $Revision: 1.1.1.1 $
 * $Date: 2007-01-19 22:28:53 $
 *
 * Copyright 2003-2007 Sun Microsystems, Inc. All Rights Reserved.  
 */

package com.stc.jmsjca.sunone;

import com.stc.jmsjca.util.ConnectionUrl;
import com.stc.jmsjca.util.Str;

import java.util.ArrayList;
import java.util.Properties;
import java.util.StringTokenizer;

import javax.jms.JMSException;

/**
 * The format of the ConnectionURL is as follows:
 *   ConnectionURL := url[,url]*
 *   url := protocol://server:host/service[?options]
 *   protocol := mq, mqtcp, mqssl, http, https
 *   service  := jms, ssljms, connectRoot/tunnel
 *   options  := key=value[&key=value]*       
 *
 * @author misc
 * @version $Revision: 1.1.1.1 $
 */
public class SunOneUrlParser extends ConnectionUrl {
    private SunOneConnectionUrl[] mConnectionUrls;
    
    /**
     * Protocol 1
     */
    public static final String PROT_MQ = "mq";
    /**
     * Protocol 2 
     */
    public static final String PROT_MQTCP = "mqtcp";
    /**
     * Protocol 3 
     */
    public static final String PROT_MQSSL = "mqssl";    
    /**
     * Protocol 4 
     */
    public static final String PROT_HTTP = "httpjms";
    /**
     * Protocol 5
     */
    public static final String PROT_HTTPS = "httpsjms";
    /**
     *  conection schema list
     */
    public static final String[] URL_PREFIXES = new String[] {
        PROT_MQ + "://",
        PROT_MQTCP + "://",
        PROT_MQSSL + "://",
        PROT_HTTP + "://",
        PROT_HTTPS + "://",        
    };
    /**
     *  connection schema list
     */    
    public static final String[] PROTOCOLS = new String[] {
        PROT_MQ,
        PROT_MQTCP,
        PROT_MQSSL,
        PROT_HTTP,
        PROT_HTTPS,        
    };
    
    /**
     * Constructor
     * 
     * @param s connection url string
     */
    public SunOneUrlParser(String s) {
        ArrayList urls = new ArrayList();
        for (StringTokenizer it = new StringTokenizer(s, ","); it.hasMoreTokens();) {
            String url = it.nextToken();
            urls.add(new SunOneConnectionUrl(url));
        }
        mConnectionUrls = (SunOneConnectionUrl[]) urls.toArray(new SunOneConnectionUrl[urls.size()]);
    }

    /**
     * @see com.stc.jmsjca.util.ConnectionUrl#getQueryProperties(java.util.Properties)
     */
    public void getQueryProperties(Properties toAddTo) {
        SunOneConnectionUrl[] urls = getConnectionUrls();
        for (int i = 0; i < urls.length; i++) {
            urls[i].getQueryProperties(toAddTo);
        }
    }

    /**
     * Checks the validity of the URL; adjusts the port number if necessary
     * 
     * @param aurl UrlParser
     * @throws javax.jms.JMSException on format failure
     * @return boolean true if the url specified url object was changed by this
     *         validation
     */
    public boolean validate() throws JMSException {
        
        if (mConnectionUrls.length ==0) {
            throw new JMSException("URL should be a comma delimited set of URLs");            
        }        
        boolean protOk = true;        
        for (int j = 0; j < mConnectionUrls.length; j++) {
            SunOneConnectionUrl url = mConnectionUrls[j];
            protOk = false;
            for (int i = 0; i < PROTOCOLS.length; i++) {
                if (PROTOCOLS[i].equals(url.getProtocol())) {
                    protOk = true;
                    break;
                }
            }
            if (!protOk) {
                throw new JMSException("Invalid protocol [" + url.getProtocol()
                    + "]: should be one of [" + Str.concat(PROTOCOLS, ", ") + "].");
            }
        }        
        return protOk;
    }    
    
    /**
     * Returns the parsers that constitute the URL
     * 
     * @return ConnectionUrl
     */
    public SunOneConnectionUrl[] getConnectionUrls() {
        return mConnectionUrls;
    }

    /**
     * Constructs a comma delimited string of schema://host:port/service
     * 
     * @return String
     */
    public String getSunOneUrlSet() {
        SunOneConnectionUrl[] urls = getConnectionUrls();
        StringBuffer buf = new StringBuffer();
        for (int i = 0; i < urls.length; i++) {
            if (i != 0) {
                buf.append(",");
            }
            buf.append(urls[i].getProtocol() + "://" + urls[i].getHost() + ":" + urls[i].getPort() + "/" + urls[i].getService());
        }
        return buf.toString();
    }
    
    /**
     * Constructs a comma delimited string of schema://host:port/admin or schema://host:port/ssladmin 
     * 
     * @return String
     */
    public String getSunOneUrlAdminSet() {
        SunOneConnectionUrl[] urls = getConnectionUrls();
        StringBuffer buf = new StringBuffer();
        for (int i = 0; i < urls.length; i++) {
            if (i != 0) {
                buf.append(",");
            }            
            if ("jms".equals(urls[i].getService())) {
                buf.append(urls[i].getProtocol() + "://" + urls[i].getHost() + ":" + urls[i].getPort() + "/admin");                
            } else if ("jssljms".equals(urls[i].getService())) {
                buf.append(urls[i].getProtocol() + "://" + urls[i].getHost() + ":" + urls[i].getPort() + "/ssladmin");                                
            } else {
                buf.append(urls[i].getProtocol() + "://" + urls[i].getHost() + ":" + urls[i].getPort() + "/admin");                
            }
        }
        return buf.toString();
    }
    
}
