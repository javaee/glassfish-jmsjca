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
 * $RCSfile: TcpProxy.java,v $
 * $Revision: 1.1.1.1 $
 * $Date: 2007-01-19 22:54:17 $
 *
 * Copyright 2003-2007 Sun Microsystems, Inc. All Rights Reserved.  
 */
package com.stc.jmsjca.test.core;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Random;

/**
 * Sits as a man-in-the-middle between a client and the server, providing
 * options for sabotaging the connection for testing failures and exception
 * listeners.
 * 
 * Duplicated from stcms
 * 
 * @author Frank Kieviet
 * @version $Revision: 1.1.1.1 $
 */
public class TcpProxy {
    // private static Logger sLog = Logger.getLogger(TcpProxy.class);

    private int mPort;
    private Mutex mLock = new Mutex();
    private ServerSocket mServer;
    private String mRelayServer;
    private int mRelayPort;
    private LinkedList mSockets = new LinkedList();
    private int mNPassThroughsCreated;

    public TcpProxy(String relayToServer, int relayToPort) throws Exception {
        mRelayServer = relayToServer;
        mRelayPort = relayToPort;

        // Disarm mutex
        mLock.acquire();

        selectPort();

        doRun();

        // Wait for server to start
        mLock.acquire();
    }
    
    /**
     * Simulates the server going down
     */
    public void disconnect() {
        done();
    }
    
    /**
     * Simulates the server coming back up
     * 
     * @throws Exception
     */
    public void reconnect() throws Exception {
        // Disarm mutex
        mLock.acquire();
        
        mServer = new ServerSocket(mPort);

        doRun();

        // Wait for server to start
        mLock.acquire();
        
    }

    /**
     * Listens for connections in a separate thread; new connections are serviced
     * in additional threads. 
     * 
     * @throws Exception
     */
    private void doRun() throws Exception {
        if (mServer == null) {
            throw new Exception("Cannot allocate port; last try was at port " + mPort);
        }

        new Thread("testserver") {
            public void run() {
                try {
                    int ctSockets = 0;
                    mLock.release();
                    while (true) {
                        Socket client = mServer.accept();

                        synchronized (mSockets) {
                            mSockets.add(client);
                            mNPassThroughsCreated++;
                        }

                        // Connect to relay target
                        Socket target = new Socket(mRelayServer, mRelayPort);

                        ctSockets++;

                        synchronized (mSockets) {
                            mSockets.add(target);
                        }

                        // Read from local, send to target
                        passthrough(client, target, ctSockets, 'a');

                        // Read from target, send to local
                        passthrough(target, client, ctSockets, 'b');
                    }
                } catch (Exception e) {
                    closeAllSockets();
                }
                mLock.release();
            }
        }
        .start();
    }

    private void selectPort() {
        // Find a port to listen on; try up to 100 port numbers
        Random random = new Random();
        for (int i = 0; i < 100; i++) {
            try {
                mPort = 50000 + random.nextInt(1000);
                mServer = new ServerSocket(mPort);
                break;
            } catch (IOException ex) {
                // ignore
            }
        }
    }
    
    private void closeAllSockets() {
        for (Iterator it = mSockets.iterator(); it.hasNext(); /*-*/) {
            Socket s = (Socket) it.next();
            try {
                s.close();
            } catch (IOException ex) {
                // Ignore
            }
        }
    }

    private void passthrough(final Socket client, final Socket server,
        final int socketIdx, final char side) {
        new Thread("passthrough " + socketIdx + side) {
            public void run() {
                try {
                    InputStream inp = client.getInputStream();
                    OutputStream out = server.getOutputStream();

                    int nbTotalRead = 0;

                    byte[] buf = new byte[8192];
                    while (true) {
                        int n = inp.read(buf);
                        if (n < 0) {
                            break;
                        }

                        nbTotalRead += n;

                        // Special corruption logic here
                        if (socketIdx == 4 && side == 'b' && nbTotalRead >= 1500) {
                            closeAllSockets();
                            throw new Exception("x");
                        }

                        out.write(buf, 0, n);
                    }
                } catch (Exception ex) {
                    // Ignore
                }
                try {
                    client.shutdownInput();
                } catch (IOException ex) {
                    // Ignore
                }
                try {
                    server.shutdownOutput();
                } catch (IOException ex) {
                    // Ignore
                }
                try {
                    client.close();
                } catch (IOException ex) {
                    // Ignore
                }
                try {
                    server.close();
                } catch (IOException ex) {
                    // Ignore
                }
            }
        }

        .start();
    }

    public int getPort() {
        return mPort;
    }

    public void done() {
        try {
            mServer.close();
        } catch (Exception ex) {
            // ignore
        }

        // Wait for server to stop listening and sockets closed
        try {
            mLock.acquire();
        } catch (InterruptedException ex) {
            // Ignore
        }

        mLock.release();
    }

    public void killLastConnection() {
        Socket s = (Socket) mSockets.removeLast();
        try {
            s.close();
        } catch (Exception e) {
            // Ignore
        }

        s = (Socket) mSockets.removeLast();
        try {
            s.close();
        } catch (Exception e) {
            // Ignore
        }
    }

    public void killAllConnections() {
        for (Iterator it = mSockets.iterator(); it.hasNext();) {
            Socket s = (Socket) it.next();
            try {
                s.shutdownInput();
                s.shutdownOutput();
                s.close();
            } catch (Exception e) {
                // Ignore
            }
        }
        mSockets.clear();
    }

    
    /**
     * @see Semaphore http://gee.cs.oswego.edu/dl/classes/EDU/oswego/cs/dl/util/concurrent
     */
    public static class Mutex {

        /** The lock status * */
        protected boolean inuse_ = false;

        public void acquire() throws InterruptedException {
            if (Thread.interrupted())
                throw new InterruptedException();
            synchronized (this) {
                try {
                    while (inuse_)
                        wait();
                    inuse_ = true;
                } catch (InterruptedException ex) {
                    notify();
                    throw ex;
                }
            }
        }

        public synchronized void release() {
            inuse_ = false;
            notify();
        }

        public boolean attempt(long msecs) throws InterruptedException {
            if (Thread.interrupted())
                throw new InterruptedException();
            synchronized (this) {
                if (!inuse_) {
                    inuse_ = true;
                    return true;
                } else if (msecs <= 0)
                    return false;
                else {
                    long waitTime = msecs;
                    long start = System.currentTimeMillis();
                    try {
                        for (;;) {
                            wait(waitTime);
                            if (!inuse_) {
                                inuse_ = true;
                                return true;
                            } else {
                                waitTime = msecs - (System.currentTimeMillis() - start);
                                if (waitTime <= 0)
                                    return false;
                            }
                        }
                    } catch (InterruptedException ex) {
                        notify();
                        throw ex;
                    }
                }
            }
        }
    }

    public synchronized int getNPassThroughsCreated() {
        return mNPassThroughsCreated;
    }
}
