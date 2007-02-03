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

package com.stc.jmsjca.util;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Updates all SeeBeyond headers to CDDL headers
 * 
 * @author Frank Kieviet
 * @version $Revision: 1.4 $
 */
public class ReplaceHeaders {
    
    /**
     * To close a stream without an exception
     * 
     * @param inp stream to close
     */
    public static void safeclose(InputStream inp) {
        if (inp != null) {
            try {
                inp.close();
            } catch (Exception ignore) {
                // ignore
            }
        }
    }

    /**
     * To close a stream without an exception
     * 
     * @param inp object to close
     */
    public static void safeclose(Reader inp) {
        if (inp != null) {
            try {
                inp.close();
            } catch (Exception ignore) {
                // ignore
            }
        }
    }
    
    /**
     * To close a stream without an exception
     * 
     * @param inp object to close
     */
    private static void safeclose(Writer f) {
        if (f != null) {
            try {
                f.close();
            } catch (Exception ignore) {
                // ignore
            }
        }
    }
    
    /**
     * To close a stream without an exception
     * 
     * @param stream object to close
     */
    public static void safeclose(OutputStream stream) {
        if (stream != null) {
            try {
                stream.close();
            } catch (Exception ignore) {
                // ignore
            }
        }
    }
    
    private static String read(File f) throws IOException {
        BufferedReader reader = null;
        try {
            char[] buf = new char[(int) f.length() + 1];
            reader = new BufferedReader(new FileReader(f));
            int n = reader.read(buf);
            if (n == buf.length) {
                throw new RuntimeException("Buffer overflow in " + f.getAbsolutePath() + ": " + n);
            }
            if (n <= 0) {
                throw new RuntimeException("Buffer underflow in " + f.getAbsolutePath() + ": " + n);
            }
            return new String(buf, 0, n);
        } finally {
            safeclose(reader);
        }
    }

    private static void write(String what, File f) throws IOException {
        BufferedWriter writer = null;
        try {
            writer = new BufferedWriter(new FileWriter(f));
            writer.write(what);
            writer.flush();
        } finally {
            safeclose(writer);
        }
    }

    private static Pattern SEEBEYONDPATTERN 
    = Pattern.compile("(.*?)(\\/\\*.*?SEEBEYOND.*?\\*\\/)(.*)"
        , Pattern.DOTALL);
    private static Pattern CONCURRENTPATTERN 
    = Pattern.compile("(.*)(\\/\\*.*Doug Lea and released into the public domain.*\\*\\/)(.*)"
        , Pattern.DOTALL);
    
    private static Pattern CDDLPATTERN 
    = Pattern.compile("(.*)(\\/\\*.*Common Development and Distribution License.*\\*\\/)(.*)"
        , Pattern.DOTALL);
    
//    private static String REPLACE1 = "/*\n"
//    + " * The contents of this file are subject to the terms\n"
//    + " * of the Common Development and Distribution License\n"
//    + " * (the \"License\").  You may not use this file except\n"
//    + " * in compliance with the License.\n"
//    + " *\n"
//    + " * You can obtain a copy of the license at\n"
//    + " * https://glassfish.dev.java.net/public/CDDLv1.0.html.\n"
//    + " * See the License for the specific language governing\n"
//    + " * permissions and limitations under the License.\n"
//    + " *\n"
//    + " * When distributing Covered Code, include this CDDL\n"
//    + " * HEADER in each file and include the License file at\n"
//    + " * https://glassfish.dev.java.net/public/CDDLv1.0.html.\n"
//    + " * If applicable add the following below this CDDL HEADER,\n"
//    + " * with the fields enclosed by brackets \"[]\" replaced with\n"
//    + " * your own identifying information: Portions Copyright\n"
//    + " * [year] [name of copyright owner]\n"
//    + " */\n";
//    
//    private static String REPLACE2 = ""
//    + "/*\n"
//    + " * $RCSfile: ReplaceHeaders.java,v $\n"
//    + " * $Revision: 1.4 $\n"
//    + " * $Date: 2007-02-03 01:49:20 $\n"
//    + " *\n"
//    + " * Copyright 2003-2007 Sun Microsystems, Inc. All Rights Reserved.  \n"
//    + " */";

    private static String CR = System.getProperty("line.separator");    
    
    private static String REPLACE1 = 
    "/*" + CR
    + " * The contents of this file are subject to the terms of the Common Development and Distribution License" + CR
    + " * (the \"License\"). You may not use this file except in compliance with the License." + CR
    + " *" + CR
    + " * You can obtain a copy of the license at https://glassfish.dev.java.net/public/CDDLv1.0.html." + CR
    + " * See the License for the specific language governing permissions and limitations under the License." + CR
    + " *" + CR
    + " * When distributing Covered Code, include this CDDL HEADER in each file and include the License file at" + CR
    + " * https://glassfish.dev.java.net/public/CDDLv1.0.html. If applicable add the following below this" + CR
    + " * CDDL HEADER, with the fields enclosed by brackets \"[]\" replaced with your own identifying" + CR
    + " * information: Portions Copyright [year] [name of copyright owner]" + CR
    + " */" + CR;
    
    private static String REPLACE2 = "" 
        + "/*" + CR
        + " * Copyright 2003-2007 Sun Microsystems, Inc. All Rights Reserved." + CR
        + " */";
    
    private void processFile(File f) throws Exception {
        String contents = read(f);
        Matcher m = SEEBEYONDPATTERN.matcher(contents);
        boolean ok = true;
        if (ok) {
            ok = m.matches();
            if (!ok) {
                if (CONCURRENTPATTERN.matcher(contents).matches()) {
//                    System.out.println("Skipped (OK) " + f.getAbsolutePath());
                } else if (CDDLPATTERN.matcher(contents).matches()) {
                    // Already processed 
                } else {
                    System.out.println("NO MATCH: " + f.getAbsolutePath());
                }
            }
        }
        if (ok) {
            ok = !contents.startsWith(REPLACE1);
        }
        if (ok && m.groupCount() != 3) {
            ok = false;
            System.out.println("Invalid group count: " + m.groupCount() + " in " + f.getAbsolutePath());
        }
        String m1 = null;
        String m2 = null;
        String m3 = null;
        if (ok) {
            m1 = m.group(1);
            m2 = m.group(2);
            m3 = m.group(3);
            String check = m1 + m2 + m3;
            if (!check.equals(contents)) {
                System.out.println("Check failure in " + f.getAbsolutePath());
                System.out.println("x=[" + check + "]");
                System.out.println("contents=[" + contents + "]");
                ok = false;
            }
        }
        if (ok) {
            String replace = m1 + REPLACE1 + REPLACE2 + m3;
            write(replace, f);
//            System.out.println("Changed " + f.getAbsolutePath());
            mWritten++;
        }
        
        if (!ok) {
            mSkipped++;
        }
    }
    
    private int mWritten;
    private int mSkipped;
    
    private void report() {
        System.out.println(mWritten + " files changed");
        System.out.println(mSkipped + " files skipped");
    }
    
    private ReplaceHeaders recurse(File dir) throws Exception {
        File[] list = dir.listFiles();
        for (int i = 0; i < list.length; i++) {
            File f = list[i];
            if (f.isDirectory()) {
                recurse(f);
            } else {
                if (f.getName().endsWith(".java")) {
                    processFile(f);
                }
            }
        }
        return this;
    }
    
    /**
     * @param args args
     */
    public static void main(String[] args) {
        String dir = args[0];
        
        try {
            new ReplaceHeaders().recurse(new File(dir)).report();
        } catch (Exception e) {
            e.printStackTrace();
        }
        
    }
    
}
