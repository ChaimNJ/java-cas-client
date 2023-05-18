/**
 * Licensed to Apereo under one or more contributor license
 * agreements. See the NOTICE file distributed with this work
 * for additional information regarding copyright ownership.
 * Apereo licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License.  You may obtain a
 * copy of the License at the following location:
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apereo.cas.client;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.ServerSocket;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * @author Scott Battaglia
 * @since 3.0
 */
public final class PublicTestHttpServer extends Thread {

    private static final Map<Integer, PublicTestHttpServer> serverMap = new HashMap<>();

    public final String encoding;

    private final byte[] header;

    private final int port;

    private final CountDownLatch ready = new CountDownLatch(1);

    public byte[] content;

    private ServerSocket server;

    private PublicTestHttpServer(final String data, final String encoding, final String MIMEType, final int port)
        throws UnsupportedEncodingException {
        this(data.getBytes(encoding), encoding, MIMEType, port);
    }

    private PublicTestHttpServer(final byte[] data, final String encoding, final String MIMEType, final int port)
        throws UnsupportedEncodingException {
        this.content = data;
        this.port = port;
        this.encoding = encoding;
        final var header = "HTTP/1.0 200 OK\r\n" + "Server: OneFile 1.0\r\n" + "Content-type: " + MIMEType + "\r\n\r\n";
        this.header = header.getBytes("ASCII");
    }

    public static synchronized PublicTestHttpServer instance(final int port) {
        if (serverMap.containsKey(port)) {
            final var server = serverMap.get(port);
            server.waitUntilReady();
            return server;
        }

        try {
            final var server = new PublicTestHttpServer("test", "ASCII", "text/plain", port);
            server.start();
            serverMap.put(port, server);
            server.waitUntilReady();
            return server;
        } catch (final Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void shutdown() {
        System.out.println("Shutting down connection on port " + server.getLocalPort());
        try {
            this.server.close();
        } catch (final Exception e) {
            System.err.println(e);
        }
    }

    @Override
    public void run() {

        try {
            this.server = new ServerSocket(this.port);
            System.out.println("Accepting connections on port " + server.getLocalPort());
            notifyReady();
            while (true) {

                try (final var connection = server.accept()) {
                    final OutputStream out = new BufferedOutputStream(connection.getOutputStream());
                    final InputStream in = new BufferedInputStream(connection.getInputStream());
                    // read the first line only; that's all we need
                    final var request = new StringBuffer(80);
                    while (true) {
                        final var c = in.read();
                        if (c == '\r' || c == '\n' || c == -1) {
                            break;
                        }
                        request.append((char) c);
                    }

                    if (request.toString().startsWith("STOP")) {
                        connection.close();
                        break;
                    }
                    if (request.toString().indexOf("HTTP/") != -1) {
                        out.write(this.header);
                    }
                    out.write(this.content);
                    out.flush();
                } catch (final IOException e) {
                    // nothing to do with this IOException
                }

            } // end while
        } // end try
        catch (final IOException e) {
            System.err.println("Could not start server. Port Occupied");
        }

    } // end run

    private void waitUntilReady() {
        try {
            ready.await(10, TimeUnit.SECONDS);
        } catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("interrupted", e);
        }
    }

    private void notifyReady() {
        ready.countDown();
    }
}
