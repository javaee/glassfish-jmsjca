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

package com.stc.jmsjca.test.stcms;

import com.stc.jmsjca.util.UrlParser;
import junit.framework.TestCase;
import java.net.URL;
import java.net.URLDecoder;
import java.util.*;

/**
 * Tests the URLParser
 *
 * @author fkieviet
 * @version $Revision: 1.4 $
 */
public class UrlParserJUStd extends TestCase {

    /**
     * Compares with built in URL class
     * 
     * @throws Throwable
     */
    public void test099() throws Throwable {
        URL p = new URL("http://host:999/file?query");
        assertTrue(p.getProtocol().equals("http"));
        assertTrue(p.getHost().equals("host"));
        assertTrue(p.getPort() == 999);
        assertTrue(p.getPath().equals("/file"));
        assertTrue(p.getFile().equals("/file?query"));
        assertTrue(p.getQuery().equals("query"));
    }

    /**
     * Basic functionality
     * 
     * @throws Throwable
     */
    public void test100() throws Throwable {
        UrlParser p = new UrlParser("protocol://host:999/file?query");
        assertTrue(p.getProtocol().equals("protocol"));
        assertTrue(p.getHost().equals("host"));
        assertTrue(p.getPort() == 999);
        assertTrue(p.getPath().equals("/file"));
        assertTrue(p.getFile().equals("/file?query"));
        assertTrue(p.getQuery().equals("query"));
    }

    /**
     * Absence of port
     * 
     * @throws Throwable
     */
    public void test101() throws Throwable {
        UrlParser p = new UrlParser("protocol://host/file?query");
        assertTrue(p.getProtocol().equals("protocol"));
        assertTrue(p.getHost().equals("host"));
        assertTrue(p.getPort() == -1);
        assertTrue(p.getPath().equals("/file"));
        assertTrue(p.getFile().equals("/file?query"));
        assertTrue(p.getQuery().equals("query"));
    }

    /**
     * No suffix
     * 
     * @throws Throwable
     */
    public void test102() throws Throwable {
        UrlParser p = new UrlParser("protocol://host:999");
        assertTrue(p.getProtocol().equals("protocol"));
        assertTrue(p.getHost().equals("host"));
        assertTrue(p.getPort() == 999);
        assertTrue(p.getPath().equals(""));
        assertTrue(p.getFile().equals(""));
        assertTrue(p.getQuery() == null);
    }

    /**
     * Suffix=/ with URL class
     * 
     * @throws Throwable
     */
    public void test103a() throws Throwable {
        URL p = new URL("http://host:999/");
        assertTrue(p.getHost().equals("host"));
        assertTrue(p.getPort() == 999);
        assertTrue(p.getPath().equals("/"));
        assertTrue(p.getFile().equals("/"));
        assertTrue(p.getQuery() == null);
    }

    /**
     * Suffix=/
     * 
     * @throws Throwable
     */
    public void test103() throws Throwable {
        UrlParser p = new UrlParser("protocol://host:999/");
        assertTrue(p.getProtocol().equals("protocol"));
        assertTrue(p.getHost().equals("host"));
        assertTrue(p.getPort() == 999);
        assertTrue(p.getPath().equals("/"));
        assertTrue(p.getFile().equals("/"));
        assertTrue(p.getQuery() == null);
    }

    /**
     * Absence of port with URL class 
     * 
     * @throws Throwable
     */
    public void test104a() throws Throwable {
        URL p = new URL("http://host/");
        assertTrue(p.getHost().equals("host"));
        assertTrue(p.getPort() == -1);
        assertTrue(p.getPath().equals("/"));
        assertTrue(p.getFile().equals("/"));
        assertTrue(p.getQuery() == null);
    }

    /**
     * Absence of port 
     * 
     * @throws Throwable
     */
    public void test104() throws Throwable {
        UrlParser p = new UrlParser("protocol://host/");
        assertTrue(p.getProtocol().equals("protocol"));
        assertTrue(p.getHost().equals("host"));
        assertTrue(p.getPort() == -1);
        assertTrue(p.getPath().equals("/"));
        assertTrue(p.getFile().equals("/"));
        assertTrue(p.getQuery() == null);
    }

    /**
     * Absence of port with : with URL class
     * 
     * @throws Throwable
     */
    public void test105a() throws Throwable {
        URL p = new URL("http://host:/");
        assertTrue(p.getProtocol().equals("http"));
        assertTrue(p.getHost().equals("host"));
        assertTrue(p.getPort() == -1);
        assertTrue(p.getPath().equals("/"));
        assertTrue(p.getFile().equals("/"));
        assertTrue(p.getQuery() == null);
    }

    /**
     * Absence of port with : 
     * 
     * @throws Throwable
     */
    public void test105() throws Throwable {
        UrlParser p = new UrlParser("protocol://host:/");
        assertTrue(p.getProtocol().equals("protocol"));
        assertTrue(p.getHost().equals("host"));
        assertTrue(p.getPort() == -1);
        assertTrue(p.getPath().equals("/"));
        assertTrue(p.getFile().equals("/"));
        assertTrue(p.getQuery() == null);
    }

    /**
     * Query string only (with URL class)
     * 
     * @throws Throwable
     */
    public void test106a() throws Throwable {
        URL p = new URL("http://host?query");
        assertTrue(p.getProtocol().equals("http"));
        assertTrue(p.getHost().equals("host"));
        assertTrue(p.getPort() == -1);
        assertTrue(p.getPath().equals(""));
        assertTrue(p.getFile().equals("?query"));
        assertTrue(p.getQuery().equals("query"));
    }

    /**
     * Query string only
     * 
     * @throws Throwable
     */
    public void test106() throws Throwable {
        UrlParser p = new UrlParser("protocol://host?query");
        assertTrue(p.getProtocol().equals("protocol"));
        assertTrue(p.getHost().equals("host"));
        assertTrue(p.getPort() == -1);
        assertTrue(p.getPath().equals(""));
        assertTrue(p.getFile().equals("?query"));
        assertTrue(p.getQuery().equals("query"));
    }

    /**
     * Absence of server
     * 
     * @throws Throwable
     */
    public void testServer1() throws Throwable {
        UrlParser p = new UrlParser("protocol://?query");
        assertTrue(p.getProtocol().equals("protocol"));
        assertTrue(p.getHost().equals(""));
        assertTrue(p.getPort() == -1);
        assertTrue(p.getPath().equals(""));
        assertTrue(p.getFile().equals("?query"));
        assertTrue(p.getQuery().equals("query"));
    }

    /**
     * Absence of server
     * 
     * @throws Throwable
     */
    public void testServer2() throws Throwable {
        UrlParser p = new UrlParser("protocol://?query");
        assertTrue(p.getProtocol().equals("protocol"));
        assertTrue(p.getHost().equals(""));
        assertTrue(p.getPort() == -1);
        assertTrue(p.getPath().equals(""));
        assertTrue(p.getFile().equals("?query"));
        assertTrue(p.getQuery().equals("query"));
        
        p.setHost("local");
        
        assertTrue(p.getHost().equals("local"));
        assertTrue(p.getPort() == -1);
        assertTrue(p.getPath().equals(""));
        assertTrue(p.getFile().equals("?query"));
        assertTrue(p.getQuery().equals("query"));
        
        System.out.println(p.toString());
        
        assertTrue(p.toString().equals("protocol://local?query"));
        
        p = new UrlParser(p.toString());

        assertTrue(p.getHost().equals("local"));
        assertTrue(p.getPort() == -1);
        assertTrue(p.getPath().equals(""));
        assertTrue(p.getFile().equals("?query"));
        assertTrue(p.getQuery().equals("query"));
    }

    /**
     * Modification of URL with port
     * 
     * @throws Throwable
     */
    public void testPort1() throws Throwable {
        String u = "protocol://host?query";
        UrlParser p = new UrlParser(u);
        assertTrue(p.getProtocol().equals("protocol"));
        assertTrue(p.getHost().equals("host"));
        assertTrue(p.getPort() == -1);
        assertTrue(p.getPath().equals(""));
        assertTrue(p.getFile().equals("?query"));
        assertTrue(p.getQuery().equals("query"));

        p.setPort(5);

        assertTrue(p.getProtocol().equals("protocol"));
        assertTrue(p.getHost().equals("host"));
        assertTrue(p.getPort() == 5);
        assertTrue(p.getPath().equals(""));
        assertTrue(p.getFile().equals("?query"));
        assertTrue(p.getQuery().equals("query"));

        assertTrue(p.toString().equals("protocol://host:5?query"));
    }

    /**
     * Modification of URL with port
     * 
     * @throws Throwable
     */
    public void testPort2() throws Throwable {
        String u = "protocol://host";
        UrlParser p = new UrlParser(u);
        assertTrue(p.getProtocol().equals("protocol"));
        assertTrue(p.getHost().equals("host"));
        assertTrue(p.getPort() == -1);
        assertTrue(p.getPath().equals(""));
        assertTrue(p.getFile().equals(""));

        p.setPort(5);

        assertTrue(p.getProtocol().equals("protocol"));
        assertTrue(p.getHost().equals("host"));
        assertTrue(p.getPort() == 5);
        assertTrue(p.getPath().equals(""));
        assertTrue(p.getFile().equals(""));

        assertTrue(p.toString().equals("protocol://host:5"));
    }

    /**
     * Modification of port
     * 
     * @throws Throwable
     */
    public void testPort3() throws Throwable {
        String u = "protocol://host/";
        UrlParser p = new UrlParser(u);
        assertTrue(p.getProtocol().equals("protocol"));
        assertTrue(p.getHost().equals("host"));
        assertTrue(p.getPort() == -1);
        assertTrue(p.getPath().equals("/"));
        assertTrue(p.getFile().equals("/"));

        p.setPort(5);

        assertTrue(p.getProtocol().equals("protocol"));
        assertTrue(p.getHost().equals("host"));
        assertTrue(p.getPort() == 5);
        assertTrue(p.getPath().equals("/"));
        assertTrue(p.getFile().equals("/"));

        assertTrue(p.toString().equals("protocol://host:5/"));
    }

    /**
     * query string parsing
     * 
     * @throws Throwable
     */
    public void testUrl1() throws Throwable {
        // a=b
        // c=d
        // e=f
        // a2s=gPh
        String u = "protocol://host/?a=b&c=d&e=f&a%32s=g%50h&p%26o=2%3D2";
        UrlParser p = new UrlParser(u);
        assertTrue(p.getPath().equals("/"));
        assertTrue(p.getFile().equals("/?a=b&c=d&e=f&a%32s=g%50h&p%26o=2%3D2"));
        String s = p.getQuery();
        assertTrue(p.getQuery().equals("a=b&c=d&e=f&a%32s=g%50h&p%26o=2%3D2"));
        Properties set = new Properties();
        for (StringTokenizer iter = new StringTokenizer(s, "&"); iter.hasMoreElements(); ) {
            String pair = (String) iter.nextToken();
            int split = pair.indexOf('=');
            if (split > 0) {
                String key = pair.substring(0, split);
                key = URLDecoder.decode(key, "UTF-8");
                String value = pair.substring(split + 1);
                value = URLDecoder.decode(value, "UTF-8");
                set.setProperty(key, value);
            }
        }

        assertTrue(set.getProperty("a").equals("b"));
        assertTrue(set.getProperty("c").equals("d"));
        assertTrue(set.getProperty("e").equals("f"));
        assertTrue(set.getProperty("a2s").equals("gPh"));
    }

    /**
     * Query string parsing
     * 
     * @throws Throwable
     */
    public void testUrl2() throws Throwable {
        // a=b
        // c=d
        // e=f
        // a2s=gPh
        String u = "protocol://host/?a=b&c=d&e=f&a%32s=g%50h&p%26o=2%3D2";
        UrlParser p = new UrlParser(u);
        assertTrue(p.getPath().equals("/"));
        assertTrue(p.getFile().equals("/?a=b&c=d&e=f&a%32s=g%50h&p%26o=2%3D2"));
        String s = p.getQuery();
        assertTrue(s != null);
        assertTrue(p.getQuery().equals("a=b&c=d&e=f&a%32s=g%50h&p%26o=2%3D2"));
        Properties set = p.getQueryProperties();

        assertTrue(set.getProperty("a").equals("b"));
        assertTrue(set.getProperty("c").equals("d"));
        assertTrue(set.getProperty("e").equals("f"));
        assertTrue(set.getProperty("a2s").equals("gPh"));
    }

    /**
     * getQueryProperties
     * 
     * @throws Throwable
     */
    public void testUrl3() throws Throwable {
        String u = "protocol://host/?a=&c=d&e=f&a%32s=g%50h&p%26o=2%3D2";
        UrlParser p = new UrlParser(u);
        Properties set = null;
        set = p.getQueryProperties();
        assertTrue(set.getProperty("a").equals(""));
    }

    /**
     * getQueryProperties
     * 
     * @throws Throwable
     */
    public void testUrl4() throws Throwable {
        String u = "protocol://host/?=b&c=d&e=f&a%32s=g%50h&p%26o=2%3D2";
        UrlParser p = new UrlParser(u);
        try {
            p.getQueryProperties();
            throw new Throwable("x");
        } catch (Exception expected) {
        }
    }

    /**
     * getQueryProperties
     * 
     * @throws Throwable
     */
    public void testUrl5() throws Throwable {
        String u = "protocol://host";
        UrlParser p = new UrlParser(u);
        Properties set = null;
        set = p.getQueryProperties();
        assertTrue(set != null);
    }
    
    private void assertEq(String a, String b) {
        if (a == null) {
            assertTrue("a == null but b != null", b == null);
        } else {
            assertTrue("a != null but b == null", b != null);
            assertTrue("a=[" + a + "] != b=[" + b + "]", a.equals(b));
        }
    }
    
    private void test(String u, String protocol, String host, int port,
            String file, String path, String query) throws Exception {
        {
            URL p = new URL(u);
            assertEq(p.getProtocol(), protocol);
            assertEq(p.getHost(), host);
            assertTrue(p.getPort() == port);
            assertEq(p.getFile(), file);
            assertEq(p.getPath(), path);
            assertEq(p.getQuery(), query);
        }
        
        {
	        UrlParser p = new UrlParser(u);
            assertEq(p.getProtocol(), protocol);
            assertEq(p.getHost(), host);
            assertTrue(p.getPort() == port);
            assertEq(p.getFile(), file);
            assertEq(p.getPath(), path);
            assertEq(p.getQuery(), query);
            assertEq(p.toString(), u);
        }
    }
    
    /**
     * More rigorous test of various permutations
     * 
     * @throws Throwable
     */
    public void test200() throws Throwable {
        test("http://a", "http", "a", -1, "", "", null);
        test("http://a/", "http", "a", -1, "/", "/", null);
        test("http://a/b", "http", "a", -1, "/b", "/b", null);
        test("http://a?x", "http", "a", -1, "?x", "", "x");
        test("http://a/b?x", "http", "a", -1, "/b?x", "/b", "x");

        test("http://a:2", "http", "a", 2, "", "", null);
        test("http://a:2/", "http", "a", 2, "/", "/", null);
        test("http://a:2/b", "http", "a", 2, "/b", "/b", null);
        test("http://a:2?x", "http", "a", 2, "?x", "", "x");
        test("http://a:2/b?x", "http", "a", 2, "/b?x", "/b", "x");

        test("http://", "http", "", -1, "", "", null);
        test("http:///", "http", "", -1, "/", "/", null);
        test("http:///b", "http", "", -1, "/b", "/b", null);
        test("http://?x", "http", "", -1, "?x", "", "x");
        test("http:///b?x", "http", "", -1, "/b?x", "/b", "x");

        test("http://:3", "http", "", 3, "", "", null);
        test("http://:3/", "http", "", 3, "/", "/", null);
        test("http://:3/b", "http", "", 3, "/b", "/b", null);
        test("http://:3?x", "http", "", 3, "?x", "", "x");
        test("http://:3/b?x", "http", "", 3, "/b?x", "/b", "x");
    }

    /**
     * Problem with getQuery
     * 
     * @throws Throwable
     */
    public void testToStringBug() throws Throwable {
        String k = "JMSJCA.concurrencymode";
        String v = "sync";
        String s = "stcms://?" + k + "=" + v;

        // Works when calling toString()
        {
            UrlParser parser = new UrlParser(s);
            parser.setHost("localhost");
            parser.setPort(999);
            Properties p = new Properties();
            System.out.println(parser.toString());
            parser.getQueryProperties(p);
            assertTrue(p.size() == 1);
            assertTrue(p.getProperty(k) != null);
            assertTrue(p.getProperty(k).equals(v));
        }
        
        // W/O toString(): didn't work
        {
            UrlParser parser = new UrlParser(s);
            parser.setHost("localhost");
            parser.setPort(999);
            Properties p = new Properties();
            parser.getQueryProperties(p);
            assertTrue(p.size() == 1);
            assertTrue(p.getProperty(k) != null);
            assertTrue(p.getProperty(k).equals(v));
        }
        
        // Static method test
        {
            Properties p = new Properties();
            UrlParser parser = new UrlParser(s);
            parser.setHost("localhost");
            parser.setPort(999);
            UrlParser.getQueryProperties(parser.getQuery(), p);
            assertTrue(p.size() == 1);
            assertTrue(p.getProperty(k) != null);
            assertTrue(p.getProperty(k).equals(v));
        }
    }
}
