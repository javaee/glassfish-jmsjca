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

package com.stc.jmsjca.test.core;

import com.stc.jmsjca.util.Str;

import junit.framework.TestCase;

public class StrJUStd extends TestCase {
    public void test1() throws Throwable {
        String inp = "a=b~JMSJCA.sep=~c=d";
        String out = Str.parseProperties("JMSJCA.sep=", inp);
        System.out.println("[" + out + "]");
        assertTrue("a=b\r\nJMSJCA.sep=\r\nc=d".equals(out));
    }

    public void test2() throws Throwable {
        String inp = "a=b~JMSJCA.sep=~c=d\\~d";
        String out = Str.parseProperties("JMSJCA.sep=", inp);
        System.out.println("[" + out + "]");
        assertTrue("a=b\r\nJMSJCA.sep=\r\nc=d~d".equals(out));
    }

    public void test3() throws Throwable {
        String inp = "a=b~JMSJCA.sep=";
        try {
            Str.parseProperties("JMSJCA.sep=", inp);
            throw new Throwable("not thrown");
        } catch (Exception expected) {
            // ignore
        }
    }

    public void test4() throws Throwable {
        String inp = "JMSJCA.sep=|c=d|a=b|e=f|";
        String out = Str.parseProperties("JMSJCA.sep=", inp);
        System.out.println("[" + out + "]");
        assertTrue("JMSJCA.sep=\r\nc=d\r\na=b\r\ne=f\r\n".equals(out));
    }

    public void test5() throws Throwable {
        String inp = "JMSJCA.sep=|c=d|a=b|e=f|\\|g\\|=\\|\\|";
        String out = Str.parseProperties("JMSJCA.sep=", inp);
        System.out.println("[" + out + "]");
        assertTrue("JMSJCA.sep=\r\nc=d\r\na=b\r\ne=f\r\n|g|=||".equals(out));
    }

    public void test6() throws Throwable {
        String inp = "a=b~JMSJCA.sep=~";
        String out = Str.parseProperties("JMSJCA.sep=", inp);
        System.out.println("[" + out + "]");
        assertTrue("a=b\r\nJMSJCA.sep=\r\n".equals(out));
    }

    public void test7() throws Throwable {
        String inp = "a=b~JMSJCA.sepA~";
        String out = Str.parseProperties("JMSJCA.sep=", inp);
        System.out.println("[" + out + "]");
        assertTrue(inp.equals(out));
    }

    public void test8() throws Throwable {
        String inp = "a=b~JMSJCA.sep=~c=\\\\~d\\";
        String out = Str.parseProperties("JMSJCA.sep=", inp);
        System.out.println("[" + out + "]");
        assertTrue("a=b\r\nJMSJCA.sep=\r\nc=\\~d\\".equals(out));
    }
}
