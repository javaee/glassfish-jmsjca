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
 * $RCSfile: TcpProxyNIOJUStd.java,v $
 * $Revision: 1.1.1.1 $
 * $Date: 2007-01-19 22:54:17 $
 *
 * Copyright 2003-2007 Sun Microsystems, Inc. All Rights Reserved.  
 */

package com.stc.jmsjca.test.core;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

import junit.framework.TestCase;

public class TcpProxyNIOJUStd extends TestCase {
    
    public TcpProxyNIOJUStd() {
        
    }
    
    public TcpProxyNIOJUStd(String name) {
        super(name);
    }
    
    private final static String NEWLINE = System.getProperty("line.separator");
    
    public void test002() throws Throwable {
        doTest(false);
    }
    public void test003() throws Throwable {
        doTest(true);
    }
    public void doTest(boolean endClient) throws Throwable {
        EchoServer echo = new EchoServer();
        Socket s = null;
        TcpProxyNIO proxy = null;
        try {
            new Thread(echo, "EchoServer 2").start();
            proxy = new TcpProxyNIO("localhost", EchoServer.PORT);            
            
            s = new Socket("localhost", proxy.getPort());
            OutputStream out = s.getOutputStream();
            String teststr = "Hello world" + NEWLINE;
            out.write(teststr.getBytes());
            out.flush();
            
            InputStream in = s.getInputStream();
            byte[] buf = new byte[teststr.length() * 2];
            int n = in.read(buf);
            String readback = new String(buf, 0, n);
            System.out.println("Written [" + teststr + "]; read [" + readback + "]");
            assertTrue(teststr.equals(readback));
            
            // Write again
            out.write(teststr.getBytes());
            n = in.read(buf);
            readback = new String(buf, 0, n);  
            assertTrue(teststr.equals(readback));

            if (endClient) {
                // close from client
                in.close();
                Thread.sleep(1000);
            }  else {
                // close from server
                teststr = "CLOSE" + NEWLINE;
                out.write(teststr.getBytes());
                n = in.read(buf);
                readback = new String(buf, 0, n);  
                assertTrue(teststr.equals(readback));
            }
        } finally {
            echo.close();
            if (s != null) {
                s.close();
            }
            if (proxy != null) {
                proxy.close();
            }
        }
    }

    /**
     * @param args
     */
    public static void main(String[] args) {
        // TODO Auto-generated method stub

    }

}
