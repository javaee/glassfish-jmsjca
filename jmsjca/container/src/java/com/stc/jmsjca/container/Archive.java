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

package com.stc.jmsjca.container;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

/**
 * Utility class that represents a recursive archive. A recursive archive is a zip
 * file that contains other zip files.
 *
 * Files in an recursive zip file adhere to the following format: each embedded
 * zip is regarded as a directory suffixed with a #
 *
 * Example: mdbstcms.jar#/META-INF/MANIFEST.MF represents the file
 * META-INF/MANIFEST.MF in a jar called mdbstcms.jar in the zip file.
 *
 * @author fkieviet
 * @version 1.0
 */
@SuppressWarnings("unchecked")
public class Archive {
    private File mInput;
    private int mLog;
    private int mCompressionLevel;

    /**
     * Constructor
     *
     * @param input File that represents the zip file
     */
    public Archive(File input) {
        mInput = input;
    }

    /**
     * @param input File
     */
    public void setInput(File input) {
        mInput = input;
    }

    /**
     * @return File
     */
    public File getInput() {
        return mInput;
    }

    private void copy(InputStream inp, OutputStream out) throws IOException {
        byte[] buf = new byte[16 * 1024];
        for (;;) {
            int nchunk = inp.read(buf, 0, buf.length);
            if (nchunk < 0) {
                break;
            }
            out.write(buf, 0, nchunk);
        }
    }

    private boolean isArchive(ZipEntry entry) {
        return entry.getName().endsWith(".jar")
            || entry.getName().endsWith(".rar")
            || entry.getName().endsWith(".war");
    }

    public static void safeMkDir(String path) {
        File d = new File(path);
        if (d.exists()) {
            // Done already
        } else {
            if (!d.mkdirs()) {
                throw new RuntimeException("Failed to create " + path);
            }
        }
    }

    /**
     * Tool function: ensures that the specified stream is closed
     *
     * @param stream to close; maybe null
     */
    public static InputStream safeClose(InputStream stream) {
        if (stream != null) {
            try {
                stream.close();
            } catch (Exception ex) {
                // ignore
            }
        }
        return null;
    }

    /**
     * Tool function: ensures that the specified stream is closed
     *
     * @param stream to close; maybe null
     */
    public static OutputStream safeClose(OutputStream stream) {
        if (stream != null) {
            try {
                stream.close();
            } catch (Exception ex) {
                // ignore
            }
        }
        return null;
    }

    /**
     * Writes the specified string to a file
     */
    public static void writeToFile(File f, String toWrite) throws Exception {
        writeToFile(f, toWrite.getBytes());
    }

    /**
     * Writes the specified buffer to a file
     */
    public static void writeToFile(File f, byte[] toWrite) throws Exception {
        FileOutputStream out = null;
        try {
            out = new FileOutputStream(f);
            out.write(toWrite);
        } catch (Exception ex) {
            throw new Exception("Failed to write to file " + f.getAbsolutePath() + ": "
                + ex, ex);
        } finally {
            safeClose(out);
        }
    }

    /**
     * Expands the archive to one directory; also expands all embedded archives
     *
     * @param outdir File
     * @param overwrite boolean
     */
    public void expand(File outdir, boolean overwrite) {
        InputStream inp = null;
        try {
            inp = new BufferedInputStream(new FileInputStream(mInput));
            OutputProcessor p = new WriteToFile(outdir, overwrite);
            expand(inp, "", new Matcher[0], p);
        } catch (Exception ex) {
            ex.printStackTrace();
        } finally {
            safeClose(inp);
        }
    }

    /**
     * Expands the archive to one directory; also expands all embedded archives
     *
     * @param outdir File
     * @param overwrite boolean
     */
    public void expandByExtension(File outdir, boolean overwrite, String[] extensions) {
        InputStream inp = null;
        Matcher[] m = new Matcher[extensions.length];
        for (int i = 0; i < extensions.length; i++) {
            m[i] = new ExtensionMatcher(extensions[i]);
        }
        try {
            inp = new BufferedInputStream(new FileInputStream(mInput));
            OutputProcessor p = new WriteToFile(outdir, overwrite);
            expand(inp, "", m, p);
        } catch (Exception ex) {
            ex.printStackTrace();
        } finally {
            safeClose(inp);
        }
    }

    private void getFiles(String root, File dir, ArrayList<FileReplacement> files) {
        File[] fs = dir.listFiles();
        for (int i = 0; i < fs.length; i++) {
            if (fs[i].isDirectory()) {
                getFiles(root, fs[i], files);
            } else {
                String path = fs[i].getAbsolutePath().substring(root.length());
                path = path.replace('\\', '/');
                if (path.startsWith("/")) {
                    path = path.substring(1);
                }
                files.add(new FileReplacement(path, fs[i]));
            }
        }
    }

    /**
     * Updates the archive with all files found in the specified directory
     *
     * @param dir File
     */
    public void updateByDir(File dir, boolean mustExist) throws Exception {
        // Find all files
        ArrayList<FileReplacement> tmp = new ArrayList<FileReplacement>();
        getFiles(dir.getAbsolutePath(), dir, tmp);
        FileReplacement[] files = tmp.toArray(new FileReplacement[tmp.size()]);
        for (int i = 0; i < files.length; i++) {
            files[i].mustExist(mustExist);
        }
        Replacements rs = new Replacements(files);
        update(rs);
    }

    /**
     * Sets the log level: 0 = off, 1 = info only, 2 = fine, 3 = finer
     * @param level int
     */
    public void setLogLevel(int level) {
        mLog = level;
    }

    /**
     * Sets the default compression level
     *
     * @param level int
     */
    public void setCompressionLevel(int level) {
        mCompressionLevel = level;
    }

    /**
     * Iterates over a compound archive, testing each file against the matchers
     * and calling process() on the output processor for matches.
     *
     * @param jar InputStream
     * @param currentPath String
     * @param matchers Matcher[]
     * @param outpp OutputProcessor
     * @throws Exception
     */
    private void expand(InputStream jar, String currentPath,
        Matcher[] matchers, OutputProcessor outpp) throws Exception {
        ZipInputStream inp = new ZipInputStream(jar);
        for (; ; ) {
            ZipEntry entry = inp.getNextEntry();
            if (entry == null) {
                break;
            } else if (entry.isDirectory()) {
            } else if (isArchive(entry)) {
                String path = currentPath + entry.getName() + "#/";
                fine("archive: " + path);
                expand(inp, path, matchers, outpp);
            } else {
                String path = currentPath + entry.getName();
                boolean shoulddo = matchers.length == 0;
                for (int i = 0; i < matchers.length; i++) {
                    if (matchers[i].matches(path)) {
                        shoulddo = true;
                        break;
                    }
                }
                if (!shoulddo) {
                    finer("Skipping " + path);
                } else {
                    outpp.process(inp, path, entry.getName());
                }
            }
        }
    }

    private void recompress(InputStream jar, OutputStream outjar,
        int level) throws Exception {
        ZipInputStream inp = new ZipInputStream(jar);
        ZipOutputStream out = new ZipOutputStream(outjar);
        out.setLevel(level);
        for (; ; ) {
            ZipEntry inpEntry = inp.getNextEntry();
            if (inpEntry == null) {
                break;
            } else if (inpEntry.isDirectory()) {
                // skip
            } else {
                ZipEntry outEntry = new ZipEntry(inpEntry.getName());
                outEntry.setComment(inpEntry.getComment());
                outEntry.setTime(inpEntry.getTime());
                out.putNextEntry(outEntry);
                if (isArchive(inpEntry)) {
                    fine("Recompressing " + inpEntry.getName());
                    recompress(inp, out, level);
                } else {
                    finer("Copying " + inpEntry.getName());
                    copy(inp, out);
                }
                out.flush();
                out.closeEntry();
            }
        }
        out.flush();
        out.finish();
    }

    /**
     * Provides an overriding write operation for a file in a recursive archive
     *
     *  Example:
     *  replace a/b/c/d.xml
     *  path= a/b/c, file= c.xml
     *  candidate a/b
     *  recurse because a/b in path
     *  candidate a/b/d
     *  dont recurse because a/b/d not in path
     *  candidate a/b/c
     *  recurse because a/b/c in path
     */
    private abstract class Replacement {
        private String mDir;
        private String mPath;

        public Replacement(String path) {
            mPath = path;
            mDir = path.substring(0, path.lastIndexOf('/') + 1);
        }

        public boolean shouldOpen(String dir) {
            return mDir.startsWith(dir);
        }

        public boolean is(String name) {
            return mPath.equals(name);
        }

        public abstract void write(OutputStream o) throws IOException;

        public abstract void check();

        protected void check(boolean b) {
            if (!b) {
                throw new RuntimeException("File [" + mPath + "] was not updated");
            }
        }
    }

    private abstract static class Matcher {
        public abstract boolean matches(String path);
    }

    private abstract static class OutputProcessor {
        public abstract void process(InputStream inp, String path, String name) throws Exception;
    }

    private class WriteToFile extends OutputProcessor {
        private File outdir;
        private boolean overwrite;

        public WriteToFile(File outdir, boolean overwrite) {
            this.outdir = outdir;
            this.overwrite = overwrite;
        }

        @Override
        public void process(InputStream inp, String path, String name) throws Exception {
            File out = new File(outdir.getAbsolutePath() + File.separator + path);
            if (!overwrite && out.exists()) {
                finer("Exists: " + out.getAbsolutePath());
            } else {
                finer("Expanding " + out.getAbsolutePath());
                safeMkDir(out.getParentFile().getAbsolutePath());
                OutputStream outstr = new BufferedOutputStream(new FileOutputStream(out));
                try {
                    copy(inp, outstr);
                } catch (Exception ex) {
                    throw new Exception("Copy failed of " + name
                        + " to " + out.getAbsolutePath() + ": " + ex, ex);
                } finally {
                    safeClose(outstr);
                }
            }
        }
    }

    private class WriteToBuf extends OutputProcessor {
        private ByteArrayOutputStream mOut;

        public WriteToBuf() {
            mOut = new ByteArrayOutputStream(4096);
        }

        @Override
        public void process(InputStream inp, String path, String name) throws Exception {
            copy(inp, mOut);
        }

        public byte[] getBuf() {
            return mOut.toByteArray();
        }
    }

//    private static class PatternMatcher extends Matcher {
//        private String mExt;
//
//        public PatternMatcher(String ext) {
//            mExt = ext;
//        }
//        public boolean matches(String path) {
//            return path.matches(mExt);
//        }
//    }
//
    private static class ExtensionMatcher extends Matcher {
        private String mExt;

        public ExtensionMatcher(String ext) {
            mExt = ext;
        }
        @Override
        public boolean matches(String path) {
            return path.endsWith(mExt);
        }
    }

    /**
     * Overwrites a file in an embedded archive with a buffer
     */
    private class BufferedReplacement extends Replacement {
        private byte[] mPayload;

        public BufferedReplacement(String path, byte[] payload) {
            super(path);
            mPayload = payload;
        }

        @Override
        public void write(OutputStream o) throws IOException {
            o.write(mPayload);
            mPayload = null;
        }

        @Override
        public void check() {
            check(mPayload == null);
        }
    }

    /**
     * Overwrites a file in an embedded archive with a buffer
     */
    private class FileReplacement extends Replacement {
        private File mFile;
        private boolean mMustExist;

        public FileReplacement(String path, File f) {
            super(path);
            mFile = f;
        }

        @Override
        public void write(OutputStream o) throws IOException {
            InputStream inp = null;
            try {
                inp = new BufferedInputStream(new FileInputStream(mFile));
                copy(inp, o);
            } finally {
                safeClose(inp);
            }
            mFile = null;
        }

        @Override
        public void check() {
            check(!mMustExist || mFile == null);
        }

        public void mustExist(boolean mustExist) {
            mMustExist = mustExist;
        }
    }

    /**
     * A collection of replacements
     */
    private class Replacements {
        private Replacement[] mReplacements;

        public Replacements(Replacement[] replacements) {
            mReplacements = replacements;
        }

        public boolean shouldOpen(String dir) {
            for (int i = 0; i < mReplacements.length; i++) {
                if (mReplacements[i].shouldOpen(dir)) {
                    return true;
                }
            }
            return false;
        }

        public Replacement is(String name) {
            for (int i = 0; i < mReplacements.length; i++) {
                if (mReplacements[i].is(name)) {
                    return mReplacements[i];
                }
            }
            return null;
        }

        /**
         * checks that the operations have succeeded
         */
        public void check() {
            for (int i = 0; i < mReplacements.length; i++) {
                mReplacements[i].check();
            }
        }
    }

    /**
     * Updates a recursive archive
     *
     * @param jar InputStream
     * @param outjar OutputStream
     * @param currentPath String
     * @param r Replacements
     * @throws Exception
     */
    private void update(InputStream jar, OutputStream outjar, String currentPath,
        Replacements r) throws Exception {
        ZipInputStream inp = new ZipInputStream(jar);
        ZipOutputStream out = new ZipOutputStream(outjar);
        out.setLevel(mCompressionLevel);
        fine("Processing " + currentPath);
        for (; ; ) {
            ZipEntry inpEntry = inp.getNextEntry();
            if (inpEntry == null) {
                break;
            } else if (inpEntry.isDirectory()) {
                // skip
            } else {
                ZipEntry outEntry = new ZipEntry(inpEntry.getName());
                outEntry.setComment(inpEntry.getComment());
                outEntry.setTime(inpEntry.getTime());
                out.putNextEntry(outEntry);
                String path = currentPath + inpEntry.getName();
                if (isArchive(inpEntry)) {
                    path += "#/";
                    if (r.shouldOpen(path)) {
                        update(inp, out, path, r);
                    } else {
                        fine("Copying archive " + path);
                        out.setLevel(0);
                        copy(inp, out);
                        out.setLevel(mCompressionLevel);
                    }
                } else {
                    Replacement op = r.is(path);
                    if (op != null) {
                        fine("Updating file " + path);
                        op.write(out);
                    } else {
                        finer("Copying file " + path);
                        copy(inp, out);
                    }
                }
                out.flush();
                out.closeEntry();
            }
        }
        out.flush();
        out.finish();
    }

    /**
     * Updates the archive with the specified file; the specified file will be added
     * or replace the file specified with the local path.
     *
     * @param file File
     * @param localPath String
     */
    public void update(String localPath, String newpayload) throws Exception {
        Replacement r = new BufferedReplacement(localPath, newpayload.getBytes());
        Replacements rs = new Replacements(new Replacement[] {
            r});
        update(rs);
    }

    /**
     * Updates the archive with the specified file; the specified file will be added
     * or replace the file specified with the local path.
     *
     * @param file File
     * @param localPath String
     */
    public void update(String localPath, byte[] newpayload) throws Exception {
        Replacement r = new BufferedReplacement(localPath, newpayload);
        Replacements rs = new Replacements(new Replacement[] {
            r});
        update(rs);
    }

    /**
     * Updates multiple payloads in the archive
     *
     * @param pathsAndPayloads Map key=local path; value=byte[] payload
     * @throws Exception
     */
    public void updateWithMemoryPayloads(Map pathsAndPayloads) throws Exception {
        Replacement[] replacements = new Replacement[pathsAndPayloads.size()];
        int i = 0;
        for (Iterator iter = pathsAndPayloads.entrySet().iterator(); iter.hasNext(); ) {
            Map.Entry pathAndPayload = (Map.Entry) iter.next();
            replacements[i++] = new BufferedReplacement((String) pathAndPayload.getKey(),
                (byte[]) pathAndPayload.getValue());
        }
        Replacements rs = new Replacements(replacements);
        update(rs);
    }

    /**
     * Updates a recursive archive
     *
     * @param rs Replacements
     * @throws Exception
     */
    private void update(Replacements rs) throws Exception {
        File tmp = new File(mInput.getAbsolutePath() + ".tmp.zip");
        InputStream inp = null;
        OutputStream out = null;
        try {
            inp = new BufferedInputStream(new FileInputStream(mInput));
            out = new BufferedOutputStream(new FileOutputStream(tmp));
            update(inp, out, "", rs);
            rs.check();

            inp = safeClose(inp);
            out = safeClose(out);
            if (!mInput.delete()) {
                throw new RuntimeException("File " + mInput.getAbsolutePath()
                    + " could not be overwritten");
            }
            if (!tmp.renameTo(mInput)) {
                String msg = "File " + tmp.getAbsolutePath()
                    + " could not be renamed to " + mInput.getAbsolutePath();
                tmp = null;
                throw new RuntimeException(msg);
            }
        } catch (Exception ex) {
            throw new Exception("Could not update "
                + mInput.getAbsolutePath() + ": " + ex, ex);
        } finally {
            inp = safeClose(inp);
            out = safeClose(out);
            if (tmp != null) {
                tmp.delete();
            }
        }
    }

    /**
     * Goes over all files and embedded files and recompresses them
     */
    public void recompress(int level) {
        long fs1 = mInput.length();

        File tmp = new File(mInput.getAbsolutePath() + ".tmp.zip");
        InputStream inp = null;
        OutputStream out = null;
        try {
            inp = new BufferedInputStream(new FileInputStream(mInput));
            out = new BufferedOutputStream(new FileOutputStream(tmp));
            recompress(inp, out, level);
        } catch (Exception ex) {
            ex.printStackTrace();
        } finally {
            safeClose(inp);
            safeClose(out);
        }

        long fs2 = tmp.length();
        System.out.println(fs1);
        System.out.println(fs2);
    }

    private void fine(String s) {
        if (mLog > 1) {
            System.out.println(s);
        }
    }

    private void finer(String s) {
        if (mLog > 2) {
            System.out.println(s);
        }
    }

    /**
     * Reads a file from a recursive archive
     *
     * @param path String
     * @return byte[]
     */
    public byte[] fetchFile(String path) throws Exception {
        InputStream inp = null;
        Matcher[] m = new Matcher[] { new ExtensionMatcher(path) };
        try {
            inp = new BufferedInputStream(new FileInputStream(mInput));
            WriteToBuf p = new WriteToBuf();
            expand(inp, "", m, p);
            return p.getBuf();
        } catch (Exception ex) {
            throw new Exception("Failure to fetch file " + path + ": " + ex, ex);
        } finally {
            safeClose(inp);
        }
    }
}
