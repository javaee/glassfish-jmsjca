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

import java.util.Properties;

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
    
    private static class T implements Str.Translator {
        private Properties mP = new Properties();
        
        public T add(String key, String value) {
            mP.setProperty(key, value);
            return this;
        }

        public String get(String key) {
            return mP.getProperty(key);
        }
    }
    
    private static T sT1 = new T().add("a", "A").add("b", "B"); 
    
    public void testP1() throws Throwable {
        String x = "a${a}b";
        String z = "aAb";
        int[] nUnresolved = new int[1];
        int[] nResolved = new int[1];
        String y = Str.substituteAntProperty(x, sT1, nResolved, nUnresolved);
        assertTrue(x + "," + y + "," + z, y.equals(z));
        assertTrue(nUnresolved[0] == 0);
        assertTrue(nResolved[0] == 1);
    }

    public void testP2() throws Throwable {
        String x = "a${x}b";
        String z = "a${x}b";
        int[] nUnresolved = new int[1];
        int[] nResolved = new int[1];
        String y = Str.substituteAntProperty(x, sT1, nResolved, nUnresolved);
        assertTrue(x + "," + y + "," + z, y.equals(z));
        assertTrue(nUnresolved[0] == 1);
        assertTrue(nResolved[0] == 0);
    }

    public void testP3() throws Throwable {
        String x = "$a${a}${b}${}";
        String z = "$aAB${}";
        int[] nUnresolved = new int[1];
        int[] nResolved = new int[1];
        String y = Str.substituteAntProperty(x, sT1, nResolved, nUnresolved);
        assertTrue(x + "," + y + "," + z, y.equals(z));
        assertTrue(nUnresolved[0] == 1);
        assertTrue(nResolved[0] == 2);
    }

    public void testP4() throws Throwable {
        String x = "a$x";
        String z = "a$x";
        int[] nUnresolved = new int[1];
        int[] nResolved = new int[1];
        String y = Str.substituteAntProperty(x, sT1, nResolved, nUnresolved);
        assertTrue(x + "," + y + "," + z, y.equals(z));
        assertTrue(nUnresolved[0] == 0);
        assertTrue(nResolved[0] == 0);
    }

    public void testP5() throws Throwable {
        String x = "a$$x";
        String z = "a$$x";
        int[] nUnresolved = new int[1];
        int[] nResolved = new int[1];
        String y = Str.substituteAntProperty(x, sT1, nResolved, nUnresolved);
        assertTrue(x + "," + y + "," + z, y.equals(z));
        assertTrue(nUnresolved[0] == 0);
        assertTrue(nResolved[0] == 0);
    }

    public void testP6() throws Throwable {
        String x = "a$${x}";
        String z = "a$${x}";
        int[] nUnresolved = new int[1];
        int[] nResolved = new int[1];
        String y = Str.substituteAntProperty(x, sT1, nResolved, nUnresolved);
        assertTrue(x + "," + y + "," + z, y.equals(z));
        assertTrue(nUnresolved[0] == 0);
        assertTrue(nResolved[0] == 0);
    }

    public void testP7() throws Throwable {
        String x = "a$${a}";
        String z = "a$${a}";
        int[] nUnresolved = new int[1];
        int[] nResolved = new int[1];
        String y = Str.substituteAntProperty(x, sT1, nResolved, nUnresolved);
        assertTrue(x + "," + y + "," + z, y.equals(z));
        assertTrue(nUnresolved[0] == 0);
        assertTrue(nResolved[0] == 0);
    }

    public void testP8() throws Throwable {
        String x = "a${x}";
        String z = "a${x}";
        int[] nUnresolved = new int[1];
        int[] nResolved = new int[1];
        String y = Str.substituteAntProperty(x, sT1, nResolved, nUnresolved);
        assertTrue(x + "," + y + "," + z, y.equals(z));
        assertTrue(nUnresolved[0] == 1);
        assertTrue(nResolved[0] == 0);
    }

    public void testP9() throws Throwable {
        String x = "a$${a";
        String z = "a$${a";
        int[] nUnresolved = new int[1];
        int[] nResolved = new int[1];
        String y = Str.substituteAntProperty(x, sT1, nResolved, nUnresolved);
        assertTrue(x + "," + y + "," + z, y.equals(z));
        assertTrue(nUnresolved[0] == 0);
        assertTrue(nResolved[0] == 0);
    }

    public void testP10() throws Throwable {
        String x = "a${a";
        int[] nUnresolved = new int[1];
        int[] nResolved = new int[1];
        try {
            Str.substituteAntProperty(x, sT1, nResolved, nUnresolved);
            throw new Throwable("not thrown");
        } catch (Exception e) {
            // ok
        }
    }
}
