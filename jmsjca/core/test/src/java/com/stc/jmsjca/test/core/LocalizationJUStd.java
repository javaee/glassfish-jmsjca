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

import com.stc.jmsjca.localization.LocalizationSupport;
import com.stc.jmsjca.localization.LocalizedString;

import java.util.Locale;
import java.util.regex.Matcher;

import junit.framework.TestCase;

public class LocalizationJUStd extends TestCase {
    
    private class Localizer extends LocalizationSupport {
        public Localizer() {
            super(LocalizationSupport.DEFAULTPATTERN, "TEST-", null);
        }
    }
    
    public LocalizationJUStd() {
        
    }
    
    public LocalizationJUStd(String name) {
        super(name);
    }
    
    public void testMatch() throws Throwable {
        String msg = "X000: abcdef";
        Matcher matcher = LocalizationSupport.DEFAULTPATTERN.matcher(msg);
        boolean b = matcher.matches();
        assertTrue(b);
        
        int groups = matcher.groupCount();
        assertTrue(groups == 3);
        
        String g1 = matcher.group(1);
        String g2 = matcher.group(2);
        String g3 = matcher.group(3);
        assertTrue(g1.equals("X000"));
        assertTrue(g2.equals(": "));
        assertTrue(g3.equals("abcdef"));
        
        Localizer loc = new Localizer();
        LocalizedString s = loc.x("X000: abcde");
        String ss = s.toString();
        assertTrue(ss.equals("TEST-X000: ABCDEF"));
        
        s = loc.x("X001: XXX", "a", "b", "c");
        ss = s.toString();
        assertTrue("TEST-X001: Aa Bb C'c'".equals(ss));
    }
    
    /**
     * Should fallback to US if locale not found
     * 
     * @throws Throwable on failure
     */
    public void testFallback() throws Throwable {
        // Should fallback to US if locale not found
        Locale def = Locale.getDefault();
        Locale.setDefault(Locale.KOREAN);
        Localizer loc = new Localizer();
        LocalizedString s = loc.x("X001: XXX", "a", "b", "c");
        String ss = s.toString();
        assertTrue("TEST-X001: Aa Bb C'c'".equals(ss));
        Locale.setDefault(def);
    }
    
    /**
     * Should read foreign
     * 
     * @throws Throwable on failure
     */
    public void testForeign() throws Throwable {
        // 
        Locale def = Locale.getDefault();
        Locale.setDefault(new Locale("nl"));
        Localizer loc = new Localizer();
        LocalizedString s = loc.x("X001: XXX", "a", "b", "c");
        String ss = s.toString();
        assertTrue("TEST-X001: Dutch: Aa Bb C'c'".equals(ss));
        Locale.setDefault(def);
    }
    
    /**
     * Missing ID should work
     * 
     * @throws Throwable on failure
     */
    public void testNotfound() throws Throwable {
        Localizer loc = new Localizer();
        {
            LocalizedString s = loc.x("Y001: XXX", "a", "b", "c");
            String ss = s.toString();
            assertTrue("TEST-Y001: XXX".equals(ss));
        }
        {
            LocalizedString s = loc.x("Y001: a={0}, b={1}", "a", "b", "c");
            String ss = s.toString();
            assertTrue("TEST-Y001: a=a, b=b".equals(ss));
        }
    }
    
    /**
     * Invalid ID should work
     * 
     * @throws Throwable on failure
     */
    public void testInvalidID() throws Throwable {
        Localizer loc = new Localizer();
        {
            LocalizedString s = loc.x("Some text", "a", "b", "c");
            String ss = s.toString();
            assertTrue("TEST-<?>: Some text".equals(ss));
        }
        {
            LocalizedString s = loc.x("Some test a={0}, b={1}", "a", "b", "c");
            String ss = s.toString();
            assertTrue("TEST-<?>: Some test a=a, b=b".equals(ss));
        }
    }

    /**
     * Invalid Msg should work
     * 
     * @throws Throwable on failure
     */
    public void testInvalidMsg() throws Throwable {
        Localizer loc = new Localizer();
        String v = "Some{ text";
        LocalizedString s = loc.x(v, "a", "b", "c");
        String ss = s.toString();
        assertTrue(ss.indexOf(v) > 0);
    }
}
