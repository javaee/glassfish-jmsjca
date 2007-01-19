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
 * $RCSfile: TcpProxyNIO.java,v $
 * $Revision: 1.1.1.1 $
 * $Date: 2007-01-19 22:54:17 $
 *
 * Copyright 2003-2007 Sun Microsystems, Inc. All Rights Reserved.  
 */

package com.stc.jmsjca.test.core;

import com.stc.jmsjca.util.Logger;
import com.stc.jmsjca.util.Semaphore;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Random;
import java.util.Set;

/**
 * A proxy server that can be used in JUnit tests to induce connection 
 * failures, to assure that connections are made, etc. The proxy server is
 * setup with a target server and port; it will listen on a port that it
 * chooses itself and delegates all data coming in to the server, and vice
 * versa.
 * 
 * Implementation: each incoming connection (client connection) maps into
 * a Conduit; this holds both ends of the line, i.e. the client end 
 * and the server end.
 * 
 * Everything is based on non-blocking IO (NIO). The proxy creates one 
 * extra thread to handle the NIO events.
 * 
 * @author fkieviet
 */
public class TcpProxyNIO implements Runnable {
    private static Logger sLog = Logger.getLogger(TcpProxyNIO.class);
    private String mRelayServer;
    private int mRelayPort;
    private int mNPassThroughsCreated;
    private Receptor mReceptor;
    private Map mChannelToPipes = new IdentityHashMap();
    private Selector selector;
    private int mCmd;
    private Semaphore mAck = new Semaphore(0);
    private Object mCmdSync = new Object();
    private Object mCountSync = new Object();
    private Exception mStartupFailure;
    private Exception mUnexpectedThreadFailure;
    private boolean mStopped;

    private static final int NONE = 0;
    private static final int STOP = 1;
    private static final int KILLALL = 2;
    private static final int KILLLAST = 3;
    
    private static int BUFFER_SIZE = 16384;
    
    /**
     * Constructor
     * 
     * @param relayServer
     * @param port
     * @throws Exception
     */
    public TcpProxyNIO(String relayServer, int port) throws Exception {
        mRelayServer = relayServer;
        mRelayPort = port;
        
        Receptor r = selectPort();
        mReceptor = r;
        
        new Thread(this, "TCPProxy on " + mReceptor.port).start();
        
        mAck.acquire();
        if (mStartupFailure != null) {
            throw mStartupFailure;
        }
    }
    
    /**
     * Utility class to hold data that describes the proxy server 
     * listening socket
     */
    private class Receptor {
        public int port;
        public ServerSocket serverSocket;
        public ServerSocketChannel serverSocketChannel;
        
        public Receptor(int port) {
            this.port = port;
        }
        
        public void bind() throws IOException {
            serverSocketChannel = ServerSocketChannel.open();
            serverSocketChannel.configureBlocking(false);
            serverSocket = serverSocketChannel.socket();
            InetSocketAddress inetSocketAddress = new InetSocketAddress(port);
            serverSocket.bind(inetSocketAddress);
        }

        public void close() {
            if (serverSocketChannel != null) {
                try {
                    serverSocketChannel.close();
                } catch (Exception ignore) {
                    
                }
                serverSocket = null;
            }
        }
    }
    
    /**
     * The client or server connection
     */
    private class PipeEnd {
        public SocketChannel channel;
        public ByteBuffer buf;
        public Conduit conduit;
        public PipeEnd other;
        public SelectionKey key;
        public String name;
        
        public PipeEnd(String name) {
            buf = ByteBuffer.allocateDirect(BUFFER_SIZE);
            buf.clear();
            buf.flip();
            this.name = "{" + name + "}";
        }
        
        public String toString() {
            StringBuffer ret = new StringBuffer();
            ret.append(name);
            if (key != null) {
                ret.append("; key: ");
                if ((key.interestOps() & SelectionKey.OP_READ) != 0) {
                    ret.append("-READ-");
                }
                if ((key.interestOps() & SelectionKey.OP_WRITE) != 0) {
                    ret.append("-WRITE-");
                }
                if ((key.interestOps() & SelectionKey.OP_CONNECT) != 0) {
                    ret.append("-CONNECT-");
                }
            }
            return ret.toString();
        }

        public void setChannel(SocketChannel channel2) throws IOException {
            this.channel = channel2;
            mChannelToPipes.put(channel, this);
            channel.configureBlocking(false);
        }

        public void close() throws IOException {
            mChannelToPipes.remove(channel);
            try {
                channel.close();
            } catch (IOException e) {
                // ignore
            }
            channel = null;
            if (key != null) {
                key.cancel();
                key = null;
            }
        }
        
        public void listenForRead(boolean on) {
            if (on) {
                key.interestOps(key.interestOps() | SelectionKey.OP_READ);
            } else {
                key.interestOps(key.interestOps() &~ SelectionKey.OP_READ);
            }
        }

        public void listenForWrite(boolean on) {
            if (on) {
                key.interestOps(key.interestOps() | SelectionKey.OP_WRITE);
            } else {
                key.interestOps(key.interestOps() &~ SelectionKey.OP_WRITE);
            }
        }
    }
    
    /**
     * Represents one link from the client to the server. It is an association 
     * of the two ends of the link. 
     */
    private class Conduit {
        public PipeEnd client;
        public PipeEnd server;
        public int id;
        
        public Conduit() {
            client = new PipeEnd("CLIENT");
            client.conduit = this;
            
            server = new PipeEnd("SERVER");
            server.conduit = this;
            
            client.other = server;
            server.other = client;
            
            synchronized (mCountSync) {
                id = mNPassThroughsCreated++;
            }
        }
    }

    /**
     * Finds a port to listen on
     * 
     * @return a newly initialized receptor
     * @throws Exception on any failure
     */
    private Receptor selectPort() throws Exception {
        Receptor ret;
        
        // Find a port to listen on; try up to 100 port numbers
        Random random = new Random();
        for (int i = 0; i < 100; i++) {
            int port = 50000 + random.nextInt(1000);
            try {
                ret = new Receptor(port);
                ret.bind();
                return ret;
            } catch (IOException ignore) {
                // Ignore
            }
        }
        throw new Exception("Could not bind port");
    }
    
    /**
     * The main event loop
     */
    public void run() {
        // ===== STARTUP ==========
        // The main thread will wait until the server is actually listening and ready
        // to process incoming connections. Failures during startup should be 
        // propagated back to the calling thread.
        try {
            selector = Selector.open();

            // Acceptor
            mReceptor.serverSocketChannel.configureBlocking(false);
            mReceptor.serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);
        } catch (Exception e) {
            synchronized (mCmdSync) {
                mStartupFailure = e;
            }
        }
        
        // ===== STARTUP COMPLETE ==========
        // Tha main thread is waiting on the ack lock; notify the main thread. 
        // Startup errors are communicated through the mStartupFailure variable.
        mAck.release();
        if (mStartupFailure != null) {
            return;
        }
        
        // ===== RUN: event loop ==========
        // The proxy thread spends its life in this event handling loop in which
        // it deals with requests from the main thread and from notifications from
        // NIO.
        try {
            loop: for (;;) {
                int nEvents = selector.select();

                // ===== COMMANDS ==========
                // Process requests from the main thread. The communication mechanism
                // is simple: the command is communicated through a variable; the main
                // thread waits until the mAck lock is set. 
                switch (getCmd()) {
                case STOP: {
                    ack();
                    break loop;
                }
                case KILLALL: {
                    PipeEnd[] pipes = toPipeArray();
                    for (int i = 0; i < pipes.length; i++) {
                        pipes[i].close();
                    }
                    ack();
                    continue;
                }
                case KILLLAST: {
                    PipeEnd[] pipes = toPipeArray();
                    Conduit last = pipes.length > 0 ? pipes[0].conduit : null;
                    if (last != null) {
                        for (int i = 0; i < pipes.length; i++) {
                            if (pipes[i].conduit.id > last.id) {
                                last = pipes[i].conduit;
                            }
                        }
                        last.client.close();
                        last.server.close();
                    }
                    ack();
                    continue;
                }
                }

                //===== NIO Event handling ==========
                if (nEvents == 0) {
                    continue;
                }
                Set keySet = selector.selectedKeys();
                for (Iterator iter = keySet.iterator(); iter.hasNext();) {
                    SelectionKey key = (SelectionKey) iter.next();
                    iter.remove();

                    //===== ACCEPT ==========
                    // A client connection has come in. Perform an async connect to
                    // the server. The remainder of the connect is going to be done in
                    // the CONNECT event handling.
                    if (key.isValid() && key.isAcceptable()) {
                        sLog.debug(">Incoming connection");
                        try {
                            Conduit pt = new Conduit();
                            ServerSocketChannel ss = (ServerSocketChannel) key.channel();

                            // Accept
                            pt.client.setChannel(ss.accept());

                            // Do asynchronous connect to relay server
                            pt.server.setChannel(SocketChannel.open());
                            pt.server.key = pt.server.channel.register(
                                selector, SelectionKey.OP_CONNECT);
                            pt.server.channel.connect(new InetSocketAddress(
                                mRelayServer, mRelayPort));                            
                        } catch (IOException e) {
                            System.err.println(">Unable to accept channel");
                            e.printStackTrace();
                            // selectionKey.cancel();
                        }
                    }

                    //===== CONNECT ==========
                    // Event that is generated when the connection to the server has
                    // completed. Here we need to initialize both pipe-ends. Both ends
                    // need to start reading. If the connection had not succeeded, the
                    // client needs to be closed immediately.
                    if (key != null && key.isValid() && key.isConnectable()) {
                        SocketChannel c = (SocketChannel) key.channel();
                        PipeEnd p = (PipeEnd) mChannelToPipes.get(c); // SERVER-SIDE
                        if (sLog.isDebugEnabled()) {
                            sLog.debug(">CONNECT event on " + p + " -- other: " + p.other);
                        }
                        
                        boolean success;
                        try {
                            success = c.finishConnect();
                        } catch (RuntimeException e) {
                            success = false;
                            if (sLog.isDebugEnabled()) {
                                sLog.debug("Connect failed: " + e, e);
                            }
                        }
                        if (!success) {
                            // Connection failure
                            p.close();
                            p.other.close();

                            // Unregister the channel with this selector
                            key.cancel();
                            key = null;
                        } else {
                            // Connection was established successfully
                            // Both need to be in readmode; note that the key for 
                            // "other" has not been created yet
                            p.other.key = p.other.channel.register(selector, SelectionKey.OP_READ);
                            p.key.interestOps(SelectionKey.OP_READ);
                        }

                        if (sLog.isDebugEnabled()) {
                            sLog.debug(">END CONNECT event on " + p + " -- other: " + p.other);
                        }
                    }

                    //===== READ ==========
                    // Data was received. The data needs to be written to the other
                    // end. Note that data from client to server is processed one chunk
                    // at a time, i.e. a chunk of data is read from the client; then
                    // no new data is read from the client until the complete chunk
                    // is written to to the server. This is why the interest-fields
                    // in the key are toggled back and forth. Ofcourse the same holds
                    // true for data from the server to the client.
                    if (key != null && key.isValid() && key.isReadable()) {
                        PipeEnd p = (PipeEnd) mChannelToPipes.get(key.channel());
                        if (sLog.isDebugEnabled()) {
                            sLog.debug(">READ event on " + p + " -- other: " + p.other);
                        }
                        
                        // Read data
                        p.buf.clear();
                        int n;
                        try {
                            n = p.channel.read(p.buf);
                        } catch (IOException e) {
                            n = -1;
                        }
                        
                        if (n >= 0) {
                            // Write to other end
                            p.buf.flip();
                            int nw = p.other.channel.write(p.buf);

                            if (sLog.isDebugEnabled()) {
                                sLog.debug(">Read " + n + " from " + p.name + "; wrote " + nw);
                            }
                            
                            p.other.listenForWrite(true);
                            p.listenForRead(false);
                        } else {
                            // Disconnected
                            if (sLog.isDebugEnabled()) {
                                sLog.debug("Disconnected");
                            }
                            
                            p.close();
                            key = null;
                            if (sLog.isDebugEnabled()) {
                                sLog.debug("Now present: " + mChannelToPipes.size());
                            }
                            
                            p.other.close();
                            
                            // Stop reading from other side
                            if (p.other.channel != null) {
                                p.other.listenForRead(false);
                                p.other.listenForWrite(true);
                            }
                        }

                        if (sLog.isDebugEnabled()) {
                            sLog.debug(">END READ event on " + p + " -- other: " + p.other);
                        }
                    }

                    //===== WRITE ==========
                    // Data was sent. As for READ, data is processed in chunks which
                    // is why the interest READ and WRITE bits are flipped.
                    // In the case a connection failure is detected, there still may be
                    // data in the READ buffer that was not read yet. Example, the 
                    // client sends a LOGOFF message to the server, the server then sends
                    // back a BYE message back to the client; depending on when the 
                    // write failure event comes in, the BYE message may still be in 
                    // the buffer and must be read and sent to the client before the
                    // client connection is closed.
                    if (key != null && key.isValid() && key.isWritable()) {
                        PipeEnd p = (PipeEnd) mChannelToPipes.get(key.channel());
                        
                        if (sLog.isDebugEnabled()) {
                            sLog.debug(">WRITE event on " + p + " -- other: " + p.other);
                        }

                        // More to write?
                        if (p.other.buf.hasRemaining()) {
                            int n = p.channel.write(p.other.buf);
                            if (sLog.isDebugEnabled()) {
                                sLog.debug(">Write some more to " + p.name + ": " + n);
                            }
                        } else {
                            if (p.other.channel != null) {
                                // Read from input again
                                p.other.buf.clear();
                                p.other.buf.flip();
                                
                                p.other.listenForRead(true);
                                p.listenForWrite(false);
                                
                            } else {
                                // Close
                                p.close();
                                key = null;
                                if (sLog.isDebugEnabled()) {
                                    sLog.debug("Now present: " + mChannelToPipes.size());
                                }
                            }
                        }
                        if (sLog.isDebugEnabled()) {
                            sLog.debug(">END WRITE event on " + p + " -- other: " + p.other);
                        }
                    }
                }
            }
        } catch (Exception e) {
            sLog.fatal("Proxy main loop error: " + e, e);
            e.printStackTrace();
            synchronized (mCmdSync) {
                mUnexpectedThreadFailure = e;
            }            
        }
        
        // ===== CLEANUP =====
        // The main event loop has exited; close all connections
        try {
            selector.close();
            PipeEnd[] pipes = toPipeArray();
            for (int i = 0; i < pipes.length; i++) {
                pipes[i].close();
            }
            mReceptor.close();
        } catch (IOException e) {
            sLog.fatal("Cleanup error: " + e, e);
            e.printStackTrace();
        }
    }
    
    private PipeEnd[] toPipeArray() {
        return (PipeEnd[]) mChannelToPipes.values().toArray(
        new PipeEnd[mChannelToPipes.size()]);
    }

    
    private int getCmd() {
        synchronized (mCmdSync) {
            return mCmd;
        }
    }
    
    private void ack() {
        setCmd(NONE);
        mAck.release();
    }

    private void setCmd(int cmd) {
        synchronized (mCmdSync) {
            mCmd = cmd;
        }
    }
    
    private void request(int cmd) {
        setCmd(cmd);
        selector.wakeup();
        
        try {
            mAck.acquire();
        } catch (InterruptedException e) {
            // ignore
        }
    }
    
    /**
     * Closes the proxy
     */
    public void close() {
        if (mStopped) {
            return;
        }
        mStopped = true;
        
        synchronized (mCmdSync) {
            if (mUnexpectedThreadFailure != null) {
                throw new RuntimeException("Unexpected thread exit: " + mUnexpectedThreadFailure, mUnexpectedThreadFailure);
            }
        }
        
        request(STOP);
    }
    
    /**
     * Restarts after close
     * 
     * @throws Exception
     */
    public void restart() throws Exception {
        close();
        
        mChannelToPipes = new IdentityHashMap();
        mAck = new Semaphore(0);
        mStartupFailure = null;
        mUnexpectedThreadFailure = null;
        
        mReceptor.bind();
        new Thread(this, "TCPProxy on " + mReceptor.port).start();
        mStopped = false;
        
        mAck.acquire();
        if (mStartupFailure != null) {
            throw mStartupFailure;
        }
    }
    
    /**
     * Returns the port number this proxy listens on
     * 
     * @return port number
     */
    public int getPort() {
        return mReceptor.port;
    }
    
    /**
     * Kills all connections; data may be lost
     */
    public void killAllConnections() {
        request(KILLALL);
    }    
    
    /**
     * Kills the last created connection; data may be lost
     */
    public void killLastConnection() {
        request(KILLLAST);
    }
    
    /**
     * @return number of connections received
     */
    public int getConnectionsCreated() {
        synchronized (mCountSync) {
            return mNPassThroughsCreated;
        }
    }

    /**
     * Closes the proxy
     * 
     * @param proxy
     */
    public static void safeClose(TcpProxyNIO proxy) {
        if (proxy != null) {
            proxy.close();
        }
    }
}
