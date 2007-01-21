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
 * $RCSfile: EchoServer.java,v $
 * $Revision: 1.3 $
 * $Date: 2007-01-21 17:51:58 $
 *
 * Copyright 2003-2007 Sun Microsystems, Inc. All Rights Reserved.  
 */

package com.stc.jmsjca.test.core;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URL;
import java.net.URLConnection;

/**
 * Small test server to test TcpProxyNIO
 * 
 * @author unattributed
 */
public class EchoServer implements Runnable {
    public static int PORT = 58618;

    public void run() {
        try {
            serverSocket = new ServerSocket(PORT);
            
            // repeatedly wait for connections, and process
            while (true) {
                // a "blocking" call which waits until a connection is requested
                Socket clientSocket = null;
                try {
                    if (serverSocket == null) {
                        break;
                    }
                    clientSocket = serverSocket.accept();
                } catch (IOException e) {
                    if (serverSocket != null) {
                        e.printStackTrace();
                    } else {
                        break;
                    }
                }
                System.out.println("Accepted connection from client");
                
                // open up IO streams
                In  in  = new In (clientSocket);
                Out out = new Out(clientSocket);
                
                // waits for data and reads it in until connection dies
                String s;
                while ((s = in.readLine()) != null) {
                    out.println(s);
                    if (s.equals("CLOSE")) {
                        break;
                    }
                }
                
                // close IO streams, then socket
                System.out.println("Closing connection with client");
                out.close();
                in.close();
                clientSocket.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    private ServerSocket serverSocket = null;
    
    public void close() {
        ServerSocket s = serverSocket;
        if (s == null) {
            return;
        }
        serverSocket = null;
        try {
            s.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // system independent
    private final static String NEWLINE = System.getProperty("line.separator");
    
    class In {
        private BufferedReader br;


        // for stdin
        public In() {
           InputStreamReader isr = new InputStreamReader(System.in);
           br = new BufferedReader(isr);
        }

        // for stdin
        public In(Socket socket) {
           try {
              InputStream is        = socket.getInputStream();
              InputStreamReader isr = new InputStreamReader(is);
              br                    = new BufferedReader(isr);
           } catch (IOException ioe) { ioe.printStackTrace(); }
        }
      
        // for URLs
        public In(URL url) {
           try {
              URLConnection site    = url.openConnection();
              InputStream is        = site.getInputStream();
              InputStreamReader isr = new InputStreamReader(is);
              br                    = new BufferedReader(isr);
           } catch (IOException ioe) { ioe.printStackTrace(); }
        }

        // for files and web pages
        public In(String s) {

           try {

              // first try to read file from local file system
              File file = new File(s);
              if (file.exists()) {
                  FileReader fr = new FileReader(s);
                  br = new BufferedReader(fr);
              }

              // next try for files included in jar
              URL url = getClass().getResource(s);

              // or URL from web
              if (url == null) url = new URL(s);

              URLConnection site    = url.openConnection();
              InputStream is        = site.getInputStream();
              InputStreamReader isr = new InputStreamReader(is);
              br = new BufferedReader(isr);
           } catch(IOException ioe) {  }
        }


        // note read() returns -1 if EOF
        private int readChar() {
           int c = -1;
           try { c = br.read(); }
           catch(IOException ioe) { ioe.printStackTrace(); }
           return c;
        }

        // read a token - delete preceding whitespace and one trailing whitespace character
        public String readString() {
            int c;
            while ((c = readChar()) != -1)
               if (!Character.isWhitespace((char) c)) break;

            if (c == -1) return null;
      
            String s = "" + (char) c;
            while ((c = readChar()) != -1)
               if (Character.isWhitespace((char) c)) break;
               else s += (char) c;

            return s;
        }

        // return rest of line as string and return it, not including newline 
        public String readLine() {
            if (br == null) return null;
            String s = null;
            try { s = br.readLine(); }
            catch(IOException ioe) { ioe.printStackTrace(); }
            return s;
        }


        // return rest of input as string, use StringBuffer to avoid quadratic run time
        // don't include NEWLINE at very end
        public String readAll() {
            StringBuffer sb = new StringBuffer();
            String s = readLine();
            if (s == null) return null;
            sb.append(s);
            while ((s = readLine()) != null) {
               sb.append(NEWLINE).append(s);
            }   
            return sb.toString();
        }


        public void close() { 
            try { br.close(); }
            catch (IOException e) { e.printStackTrace(); }
        }


     }

    public class Out {
        private PrintWriter out;

        // for stdout
        public Out(OutputStream os) { out = new PrintWriter(os, true); }
        public Out()                { this(System.out);                }

        // for Socket output
        public Out(Socket socket) {
            try                     { out = new PrintWriter(socket.getOutputStream(), true); }
            catch (IOException ioe) { ioe.printStackTrace();                                 }
        }
     
        // for file output
        public Out(String s) {
            try                     { out = new PrintWriter(new FileOutputStream(s), true);  }
            catch(IOException ioe)  { ioe.printStackTrace();                                 }
        }

        public void close() { out.close(); }


        public void println()          { out.println();  }
        public void println(Object x)  { out.println(x); }
        public void println(String x)  { out.println(x); }
        public void println(boolean x) { out.println(x); }
        public void println(char x)    { out.println(x); }
        public void println(double x)  { out.println(x); }
        public void println(float x)   { out.println(x); }
        public void println(int x)     { out.println(x); }
        public void println(long x)    { out.println(x); }

        public void print()            {                 out.flush(); }
        public void print(Object x)    { out.print(x);   out.flush(); }
        public void print(String x)    { out.print(x);   out.flush(); }
        public void print(boolean x)   { out.print(x);   out.flush(); }
        public void print(char x)      { out.print(x);   out.flush(); }
        public void print(double x)    { out.print(x);   out.flush(); }
        public void print(float x)     { out.print(x);   out.flush(); }
        public void print(int x)       { out.print(x);   out.flush(); }
        public void print(long x)      { out.print(x);   out.flush(); }
    }
}