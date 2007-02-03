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

import com.stc.jmsjca.util.Undep;

import junit.framework.TestCase;

public class UndepJUStd extends TestCase {
    
    public UndepJUStd() {
        
    }
    
    public UndepJUStd(String name) {
        super(name);
    }
    
    public interface A0 {
        void constructor(int k);
        void setK(int k);
        int getK();
    }

    public interface A extends A0 {
        String NAME = AImpl.class.getName();
    }

//    private interface B {
//        public void constructor(int m);
//        public void setM(int m);
//        public int getM();
//    }
    
    public static class AImpl {
        int mK;
        public AImpl(int k) {
           mK = k;
        }
        public void setK(int k) {
            mK = k;
        }
        public int getK() {
            return mK;
        }
    }
    
    public void test001() throws Throwable {
        A a = (A) Undep.create(A.class, A.class.getClassLoader());
        a.constructor(5);
        int k = a.getK();
        assertTrue(k == 5);
        a.setK(3);
        k = a.getK();
        assertTrue(k == 3);
        
        try {
            a.constructor(10);
            throw new Throwable("Did not throw");
        } catch (Exception e) {
            // expected
        }
    }
    
    public void test002() throws Throwable {
        A0 a = (A0) Undep.create(A0.class, new AImpl(5));
        int k = a.getK();
        assertTrue(k == 5);
        a.setK(3);
        k = a.getK();
        assertTrue(k == 3);
        
        try {
            a.constructor(10);
            throw new Throwable("Did not throw");
        } catch (Exception e) {
            // expected
        }
    }
}
