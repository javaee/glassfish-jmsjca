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

package com.stc.i18ntools;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Extracts internationalizable text from a class files or jars
 * 
 * @author fkieviet
 */
public class TextExtractor extends Task {
    private File dir;
    private File file;
    private boolean strict = true;
    
    /**
     * @see org.apache.tools.ant.Task#execute()
     */
    public void execute() {
        if (dir == null) {
            throw new BuildException("Directory must be specified");
        }
        if (file == null) {
            throw new BuildException("File must be specified");
        }
        extract(dir.getAbsolutePath(), file.getAbsolutePath());
    }
    
    /**
     * @param args args
     */
    public static void main(String[] args) {
        TextExtractor t = new TextExtractor();
        t.extract(
            "Q:\\BUILD\\Modules\\jmsjca\\core\\classes", 
            "Q:\\jmsjca\\core\\src\\java\\com\\stc\\jmsjca\\localization\\msgs.properties"
        );
        t.extract(
            "Q:\\BUILD\\Modules\\jmsjca\\rawmq\\classes",
            "Q:\\jmsjca\\wmq\\src\\java\\com\\stc\\jmsjca\\wmq\\msgs.properties" 
        );
        t.extract(
            "Q:\\BUILD\\Modules\\jmsjca\\rawl\\classes",
            "Q:\\jmsjca\\wl\\src\\java\\com\\stc\\jmsjca\\wl\\msgs.properties" 
        );
        t.extract(
            "Q:\\BUILD\\Modules\\jmsjca\\rawave\\classes",
            "Q:\\jmsjca\\wave\\src\\java\\com\\stc\\jmsjca\\wave\\msgs.properties" 
        );
        t.extract(
            "Q:\\BUILD\\Modules\\jmsjca\\raunifiedjms\\classes",
            "Q:\\jmsjca\\unifiedjms\\src\\java\\com\\stc\\jmsjca\\unifiedjms\\msgs.properties" 
        );
        t.extract(
            "Q:\\BUILD\\Modules\\jmsjca\\rasunone\\classes",
            "Q:\\jmsjca\\sunone\\src\\java\\com\\stc\\jmsjca\\sunone\\msgs.properties" 
        );
        t.extract(
            "Q:\\BUILD\\Modules\\jmsjca\\rastcms453\\classes",
            "Q:\\jmsjca\\stcms453\\src\\java\\com\\stc\\jmsjca\\stcms453\\msgs.properties" 
        );
        t.extract(
            "Q:\\BUILD\\Modules\\jmsjca\\rastcms\\classes",
            "Q:\\jmsjca\\stcms\\src\\java\\com\\stc\\jmsjca\\stcms\\msgs.properties" 
        );
        t.extract(
            "Q:\\BUILD\\Modules\\jmsjca\\rajndi\\classes",
            "Q:\\jmsjca\\jndi\\src\\java\\com\\stc\\jmsjca\\jndi\\msgs.properties" 
        );
        t.extract(
            "Q:\\BUILD\\Modules\\jmsjca\\rajboss\\classes",
            "Q:\\jmsjca\\jboss\\src\\java\\com\\stc\\jmsjca\\jboss\\msgs.properties" 
        );
    }    
    
    /**
     * Extracts internationalizable text out of a jar and puts it in a properties file
     * 
     * @param dir classes dir
     * @param propertiesPath properties to update
     */
    public void extract(String dir, String propertiesPath) {
        try {
            boolean error = false;
            boolean couldBeImproved = false;
            
            List list = new ArrayList();
            readDir(list, new File(dir).getAbsolutePath(), dir);

            StringWriter outbuf = new StringWriter();
            PrintWriter out = new PrintWriter(outbuf);
            
            out.println("# DO NOT EDIT");
            out.println("# THIS FILE IS GENERATED AUTOMATICALLY FROM JAVA SOURCES/CLASSES");
            out.println();
            
            TextEntry[] entries = (TextEntry[]) list.toArray(new TextEntry[list.size()]);
            Set sortedByText = new TreeSet(new Comparator() {
                public int compare(Object arg0, Object arg1) {
                    TextEntry lhs = (TextEntry) arg0;
                    TextEntry rhs = (TextEntry) arg1;
                    return lhs.getText().compareTo(rhs.getText());
                }
            });
            sortedByText.addAll(Arrays.asList(entries));
            
            // Build BY-ID and BY-CONTENT maps
            Map byId = new HashMap();
            Map byContent = new HashMap();
            for (int i = 0; i < entries.length; i++) {
                TextEntry c = entries[i];
                if (byId.get(c.getID()) == null) {
                    byId.put(c.getID(), new ArrayList());
                }
                ((List) byId.get(c.getID())).add(c);

                if (byContent.get(c.getContent()) == null) {
                    byContent.put(c.getContent(), new ArrayList());
                }
                ((List) byContent.get(c.getContent())).add(c);
            }
            
            // Print
            for (Iterator iter = sortedByText.iterator(); iter.hasNext();) {
                TextEntry e = (TextEntry) iter.next();
                List sources = (List) byId.get(e.getID());
                for (Iterator iterator = sources.iterator(); iterator.hasNext();) {
                    TextEntry e2 = (TextEntry) iterator.next();
                    out.println("# " + e2.getClassname());
                    if (!e2.getContent().equals(e.getContent())) {
                        error = true;
                        System.err.println();
                        System.err.println("DIFFERENT TEXTS, SAME IDS: ");
                        System.err.println(e.getClassname());
                        System.err.println(e.getText());
                        System.err.println(e2.getClassname());
                        System.err.println(e2.getText());
                    }
                }
                out.println(e.getID() + " = " + e.getContent());
                out.println();
            }
            
            // Check for duplicate texts with different IDs
            for (Iterator iter = byContent.entrySet().iterator(); iter.hasNext();) {
                Map.Entry e = (Map.Entry) iter.next();
                List dups = (List) e.getValue();
                Set ids = new TreeSet(new Comparator() {
                    public int compare(Object arg0, Object arg1) {
                        TextEntry lhs = (TextEntry) arg0;
                        TextEntry rhs = (TextEntry) arg1;
                        return lhs.getID().compareTo(rhs.getID());
                    }
                });
                if (ids.size() > 1) {
                    System.err.println();
                    System.err.println("SAME TEXTS, DIFFERENT IDS");
                    for (Iterator iterator = dups.iterator(); iterator.hasNext();) {
                        couldBeImproved = true;
                        TextEntry dup = (TextEntry) iterator.next();
                        System.err.println(dup.getClassname());
                        System.err.println(dup.getText());
                    }
                }
            }
            
//            // Find all numeric ids
//            {
//                TreeSet numericIds = new TreeSet();
//                for (int i = 0; i < entries.length; i++) {
//                    String digits = getDigits(entries[i].getID());
//                    int val = Integer.parseInt(digits);
//                    numericIds.add(new Integer(val));
//                }
//                
//                if (!numericIds.isEmpty()) {
//                    Integer min = (Integer) numericIds.first();
//                    Integer max = (Integer) numericIds.last();
//                    boolean found = false;
//                    for (int i = min.intValue(); i <= max.intValue(); i++) {
//                        if (!numericIds.contains(new Integer(i))) {
//                            if (!found) {
//                                out.println();
//                                out.println("# Ids that were not used:");
//                            }
//                            out.println("# " + i);
//                            found = true;
//                        }
//                    }
//                }
//            }
            
            out.close();
            out = null;

            // Update bundle if contents has changed
            String old = read(propertiesPath);
            String newContents = outbuf.getBuffer().toString();
            if (!newContents.equals(old)) {
                System.out.println("Updating " + propertiesPath);
                write(propertiesPath, newContents);
            } else {
                System.out.println("Up to date: " + propertiesPath);
            }
            
            if (error) {
                throw new BuildException("Duplicate ids but different texts; "
                    + "see console output for details"); 
            }
            
            if (couldBeImproved && strict) {
                throw new BuildException("Duplicate texts but different ids; "
                    + "see console output for details"); 
            }
        } catch (Exception e) {
            e.printStackTrace();
        } 
    }
    
    static Pattern PATTERN = Pattern.compile("([A-Z]\\d\\d\\d)(: )(.*)");
    
    /**
     * @param s Stream to close
     */
    public static void safeClose(InputStream s) {
        if (s != null) {
            try {
                s.close();
            } catch (Exception ignore) {
                // ignore
            }
        }
    }

    /**
     * @param s Stream to close
     */
    public static void safeClose(OutputStream s) {
        if (s != null) {
            try {
                s.close();
            } catch (Exception ignore) {
                // ignore
            }
        }
    }

    /**
     * @param s Stream to close
     */
    public static void safeClose(Writer s) {
        if (s != null) {
            try {
                s.close();
            } catch (Exception ignore) {
                // ignore
            }
        }
    }
    
//    private String getDigits(String s) {
//        StringBuffer ret = new StringBuffer();
//        for (int i = 0, n = s.length(); i < n; i++) {
//            char c = s.charAt(i);
//            if (Character.isDigit(c)) {
//                ret.append(c);
//            }
//        }
//        return ret.toString();
//    }
    
    private static String read(String path) throws Exception {
        File f = new File(path);
        if (!f.exists()) {
            return null;
        }
        int len = (int) f.length();
        byte[] buf = new byte[len];
        FileInputStream inp = null;
        try {
            inp = new FileInputStream(path);
            int nbread = inp.read(buf);
            if (nbread != len) {
                throw new IOException(nbread + " read, " + len + " length"); 
            }
            return new String(buf);
        } finally {
            safeClose(inp);
        }
    }
    
    private static void write(String path, String contents) throws Exception {
        FileOutputStream out = null;
        try {
            out = new FileOutputStream(path);
            out.write(contents.getBytes());
        } finally {
            safeClose(out);
        }
    }

    /**
     * TAG_NOTHING means that the ConstantPool Entry is invalid.
     **/
    public static final int TAG_NOTHING = -1;

    /**
     * TAG_UTF8 = CONSTANT_UTF8
     **/
    public static final int TAG_UTF8 = 1;

    /**
     * TAG_INTEGER = CONSTANT_INTEGER
     **/
    public static final int TAG_INTEGER = 3;

    /**
     * TAG_FLOAT = CONSTANT_FLOAT
     **/
    public static final int TAG_FLOAT = 4;

    /**
     * TAG_LONG = CONSTANT_LONG
     **/
    public static final int TAG_LONG = 5;

    /**
     * TAG_DOUBLE = CONSTANT_DOUBLE
     **/
    public static final int TAG_DOUBLE = 6;

    /**
     * TAG_CLASS = CONSTANT_CLASS
     **/
    public static final int TAG_CLASS = 7;

    /**
     * TAG_STRING = CONSTANT_STRING
     **/
    public static final int TAG_STRING = 8;

    /**
     * TAG_FIELDREF = CONSTANT_FIELDREF
     **/
    public static final int TAG_FIELDREF = 9;

    /**
     * TAG_METHODREF = CONSTANT_METHODREF
     **/
    public static final int TAG_METHODREF = 10;

    /**
     * TAG_INTERFACEREF = CONSTANT_INTERFACEREF
     **/
    public static final int TAG_INTERFACEREF = 11;

    /**
     * TAG_NAMETYPE  = CONSTANT_NAMETYPE
     **/
    public static final int TAG_NAMETYPE = 12;
    
    
    
    /**
     * A collection of string entries associated with a class in a URL
     * 
     * @author fkieviet
     */
    private static class TextEntry {
        private String jarURL;
        private String classname;
        private String text;
        private String id;
        private String content;

        /**
         * @param jarurl jar
         * @param classname classname
         * @param text String
         */
        public TextEntry(String jarurl, String classname, String text) {
            jarURL = jarurl;
            this.classname = classname;
            this.text = text;
        }
        
        /**
         * Getter for classname
         *
         * @return String
         */
        public String getClassname() {
            return classname;
        }
        
        /**
         * Setter for classname
         *
         * @param classname StringThe classname to set.
         */
        public void setClassname(String classname) {
            this.classname = classname;
        }

        /**
         * Getter for jarURL
         *
         * @return String
         */
        public String getJarURL() {
            return jarURL;
        }

        /**
         * Getter for strings
         *
         * @return List<String>
         */
        public String getText() {
            return text;
        }
        
        /**
         * @return ID
         */
        public String getID() {
            if (id == null) {
                Matcher m = PATTERN.matcher(text);
                if (!m.matches()) {
                    throw new RuntimeException("String not match pattern: " + text);
                }
                id = m.group(1);
                content = m.group(3);
            }
            return id;
        }
        
        /**
         * @return content
         */
        public String getContent() {
            if (content == null) {
                getID();
            }
            return content;
        }
        
        /**
         * @param s to test
         * @return true if matches
         */
        public static boolean matches(String s) {
            Matcher m = PATTERN.matcher(s);
            return m.matches();
        }
    }
    
    private static boolean isArchive(ZipEntry entry) {
        for (int i = 0; i < mArchiveExtensions.length; i++) {
            if (entry.getName().endsWith(mArchiveExtensions[i])) {
                return true;
            }
        }
        return false;
    }

    private static String[] mArchiveExtensions = new String[] {".jar", ".zip", ".nbm", ".war", ".ear", ".rar", ".sar"};
    
    private static void readDir(List list, String root, String currentPath) throws Exception {
        File[] entries = new File(currentPath).listFiles();
        for (int i = 0; i < entries.length; i++) {
            if (entries[i].isDirectory()) {
                readDir(list, root, entries[i].getAbsolutePath());
            } else {
                if (entries[i].getName().endsWith(".class")) {
                    InputStream inp = null;
                
                    try {
                        inp = new FileInputStream(entries[i]);
                        List strings = readStrings(inp);
                        for (Iterator iter = strings.iterator(); iter.hasNext();) {
                            String s = (String) iter.next();
                            if (TextEntry.matches(s)) {
                                list.add(new TextEntry(currentPath, 
                                    entries[i].getAbsolutePath().substring(root.length() + 1), s));
                            }
                        }
                    } catch (Exception ex) {
                        throw new Exception("Inspection failed of " + entries[i].getName() + ": " + ex, ex);
                    } finally {
                        safeClose(inp);
                    }
                }
            }
        }
    }
    
    /**
     * @param list List<TextEntries>
     * @param jar jar to explore
     * @param currentPath Path prefix
     * @throws Exception on fault
     */
    public static void readJar(List list, InputStream jar, String currentPath) throws Exception {
        ZipInputStream inp = new ZipInputStream(jar);
        for (;;) {
            ZipEntry entry = inp.getNextEntry();
            if (entry == null) {
                break;
            } else if (entry.isDirectory()) {
                // Ignore
            } else if (isArchive(entry)) {
                String path = currentPath + entry.getName() + "#/";
                readJar(list, inp, path);
            } else {
                if (entry.getName().endsWith(".class")) {
                    try {
                        List strings = readStrings(inp);
                        for (Iterator iter = strings.iterator(); iter.hasNext();) {
                            String s = (String) iter.next();
                            if (TextEntry.matches(s)) {
                                list.add(new TextEntry(currentPath, entry.getName(), s));
                            }
                        }
                    } catch (Exception ex) {
                        throw new Exception("Inspection failed of " + entry.getName() + ": " + ex, ex);
                    } 
                }
            }
        }
    }
    
    /**
     * Reads all strings from the constant pool of a class file
     * 
     * @param s class file
     * @return list of strings
     * @throws Exception on failure
     */
    public static List readStrings(InputStream s) throws Exception {
        DataInputStream inp = new DataInputStream(s);
        
        // Sentinel
        int magic = inp.readInt();
        if (magic != 0xCAFEBABE) {
            throw new Exception("Invalid Magic Number");
        }
        
        // Version
        // short minorVersion = 
        inp.readShort();
        // short majorVersion = 
        inp.readShort();

        // Constant pool
        int nEntries = inp.readShort();
        List strings = new ArrayList();
        for (int i = 1; i < nEntries; i++) {
            byte tagByte  = inp.readByte();

            switch(tagByte) {
                case TAG_UTF8 :
                    String utfString = inp.readUTF();
                    strings.add(utfString);
                    break;
                case TAG_INTEGER:
                    // int intValue = 
                    inp.readInt();
                    break;
                case TAG_FLOAT:
                    // float floatValue = 
                    inp.readFloat();
                    break;
                case TAG_LONG:
                    // long longValue  = 
                    inp.readLong();
                    // Long takes two ConstantPool Entries.
                    i++;
                    break;
                case TAG_DOUBLE:
                    // double doubleValue = 
                    inp.readDouble();
                    // Double takes two ConstantPool Entries.
                    i++;
                    break;
                case TAG_CLASS: {
                    // int classIndex  =  
                    inp.readShort();
                    break;
                }
                case TAG_STRING:
                    // int stringIndex =  
                    inp.readShort();
                    // TODO
                    break;
                case TAG_FIELDREF: {
                    // int classIndex  = 
                    inp.readShort();
                    // int nameType = 
                    inp.readShort();
                    break;
                }
                case TAG_METHODREF: {
                    // int classIndex  = 
                    inp.readShort();
                    // int nameType = 
                    inp.readShort();
                    break;
                }
                case TAG_INTERFACEREF: {
                    // int classIndex  = 
                    inp.readShort();
                    // int nameType = 
                    inp.readShort();
                    break;
                }
                case TAG_NAMETYPE: {
                    // int nameIndex = 
                    inp.readShort();
                    // int descIndex = 
                    inp.readShort();
                    break;
                }
                default:
                    throw new Exception("Unknown tagbyte " + tagByte
                    + " in constant pool (entry " + i + " out of " + nEntries + ")");
            }
        }
        return strings;
    }

    /**
     * Getter for dir
     *
     * @return String
     */
    public File getDir() {
        return dir;
    }

    /**
     * Setter for dir
     *
     * @param dir StringThe dir to set.
     */
    public void setDir(File dir) {
        this.dir = dir;
    }

    /**
     * Getter for file
     *
     * @return String
     */
    public File getFile() {
        return file;
    }

    /**
     * Setter for file
     *
     * @param file StringThe file to set.
     */
    public void setFile(File file) {
        this.file = file;
    }

    /**
     * Getter for strict
     *
     * @return boolean
     */
    public boolean getStrict() {
        return strict;
    }

    /**
     * Setter for strict
     *
     * @param strict booleanThe strict to set.
     */
    public void setStrict(boolean strict) {
        this.strict = strict;
    }
}
