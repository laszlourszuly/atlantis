package com.echsylon.atlantis;

import java.io.ByteArrayInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import javax.net.ServerSocketFactory;

import okio.Buffer;
import okio.BufferedSink;
import okio.BufferedSource;
import okio.Okio;
import okio.Sink;
import okio.Source;

import static com.echsylon.atlantis.LogUtils.info;
import static com.echsylon.atlantis.Utils.closeSilently;
import static com.echsylon.atlantis.Utils.isEmpty;
import static com.echsylon.atlantis.Utils.sleepSilently;

/**
 * This class acts as a minimal web server. It's not complete by any standards
 * means. It's a streamlined implementation to meet the Atlantis needs.
 */
class MockWebServer {

    /**
     * This interface describes the mandatory features required to provide a
     * mocked response to serve to the calling HTTP client.
     */
    interface ResponseHandler {

        /**
         * Returns a mocked response for the request described by the given
         * {@link Meta}.
         *
         * @param meta   The meta data describing the request to provide a
         *               mocked response for.
         * @param source The byte source stream that any payload can be read
         *               from.
         * @return Always a mocked response object. Null is not acceptable. If
         * no mocked response has been configured for the described request,
         * then a default mock response of choice must be returned instead.
         */
        MockResponse getMockResponse(final Meta meta, final Source source);
    }


    private final ResponseHandler responseHandler;
    private final Set<Socket> openClientSockets;

    private ExecutorService executorService;
    private ServerSocket serverSocket;
    private boolean started;


    /**
     * Creates a new instance of the Atlantis mock server.
     *
     * @param responseHandler The offload infrastructure that will analyze any
     *                        given request and find a suitable mocked response
     *                        for it.
     */
    MockWebServer(final ResponseHandler responseHandler) {
        this.openClientSockets = Collections.newSetFromMap(new ConcurrentHashMap<Socket, Boolean>());
        this.responseHandler = responseHandler;
    }

    /**
     * Starts the internal machinery of Atlantis.
     *
     * @throws IOException If the startup couldn't be be performed properly.
     */
    void start(final InetAddress inetAddress, final int port) throws IOException {
        start(new InetSocketAddress(inetAddress, port));
    }

    /**
     * Initiates the shutdown process of the internal Atlantis machinery.
     *
     * @throws IOException If the server socket couldn't be closed or any
     *                     background threads didn't shut down in a timely
     *                     manner.
     */
    void stop() throws IOException {
        shutdown();
    }

    /**
     * Returns a flag telling whether the mock web server is in a running state
     * or not.
     *
     * @return Boolean true if the mock server is operational and ready to
     * receive requests, false otherwise.
     */
    boolean isRunning() {
        return started &&
                serverSocket.isBound() && !serverSocket.isClosed() &&
                !executorService.isShutdown() && !executorService.isTerminated();
    }


    /**
     * Closes the server socket and awaits termination of all background
     * services.
     *
     * @throws IOException If the server socket didn't close properly.
     */
    private synchronized void shutdown() throws IOException {
        if (!started)
            return;

        serverSocket.close();
        started = false;

        // Don't let the shutdown process block the Android main thread.
        new Thread(() -> {
            try {
                if (!executorService.awaitTermination(10, TimeUnit.SECONDS))
                    throw new IOException();
                info("Successfully shut down");
            } catch (InterruptedException e) {
                info("Interrupted prematurely by system");
            } catch (IOException e) {
                info("Executor didn't shutdown in a timely manner");
            } finally {
                for (Iterator<Socket> iterator = openClientSockets.iterator(); iterator.hasNext(); iterator.remove())
                    closeSilently(iterator.next());
            }
        }, "Atlantis Shutdown Task").start();
    }

    /**
     * Initializes the background threading services and the server socket that
     * will listen for requests on the device's "localhost" loopback.
     *
     * @param inetSocketAddress The "address" to initialize the server socket
     *                          with. For now this is hardcoded to "localhost".
     * @throws IOException If the server socket couldn't be setup properly.
     */
    private synchronized void start(final InetSocketAddress inetSocketAddress) throws IOException {
        if (started)
            throw new IllegalStateException("Already running");

        started = true;
        executorService = Executors.newCachedThreadPool(runnable -> {
            Thread result = new Thread(runnable, "Atlantis MockWebServer");
            result.setDaemon(true);
            return result;
        });

        serverSocket = ServerSocketFactory.getDefault().createServerSocket();
        serverSocket.setReuseAddress(inetSocketAddress.getPort() != 0);
        serverSocket.bind(inetSocketAddress, 50);

        executorService.execute(() -> {
            try {
                while (true) {
                    try {
                        info("Ready for connections");
                        Socket socket = serverSocket.accept(); // Blocks until connection made.
                        openClientSockets.add(socket);
                        serveConnection(socket);
                    } catch (SocketException e) {
                        info("Stopped accepting connections. Shutting down.");
                        break;
                    } catch (Exception e) {
                        info(e, "Failed unexpectedly");
                        break;
                    }
                }
            } finally {
                executorService.shutdown();
                closeSilently(serverSocket);
                for (Iterator<Socket> iterator = openClientSockets.iterator(); iterator.hasNext(); iterator.remove())
                    closeSilently(iterator.next());
                started = false;
            }
        });
    }

    /**
     * Enqueues a new filter operation where a detected request will be analyzed
     * and a corresponding mock response will be served for it. The actual
     * analysis is outsourced to an injected {@link MockRequest.Filter}
     * implementation.
     *
     * @param socket The socket on which the request was detected.
     */
    private void serveConnection(final Socket socket) {
        executorService.execute(() -> {
            BufferedSource source = null;
            BufferedSink target = null;

            try {
                source = Okio.buffer(Okio.source(socket));
                target = Okio.buffer(Okio.sink(socket));
                Meta meta = null;

                while ((meta = readRequestMeta(source)) != null) {
                    Buffer body = readRequestBody(meta, source);
                    MockResponse response = responseHandler.getMockResponse(meta, body);
                    writeResponse(response, target);
                }
            } catch (SocketException e) {
                info("Socket connection closed: %s", socket.getInetAddress());
            } catch (EOFException e) {
                info("Socket exhausted, closing: %s", socket.getInetAddress());
            } catch (IOException e) {
                info(e, "Couldn't parse request: %s", socket.getInetAddress());
            } catch (Exception e) {
                info(e, "Connection crashed: %s", socket.getInetAddress());
            } finally {
                closeSilently(source);
                closeSilently(target);
                closeSilently(socket);
                openClientSockets.remove(socket);
            }
        });
    }

    /**
     * Reads the meta data from an HTTP request source. The meta data in this
     * context is the request line, e.g. "GET /path HTTP/1.1" and the headers.
     *
     * @param source The byte stream source to read from.
     * @return A data structure containing the read meta data.
     * @throws IOException If the read operation would fail from some reason.
     */
    private Meta readRequestMeta(final BufferedSource source) throws IOException {
        String line = source.readUtf8LineStrict();
        if (isEmpty(line))
            return null;

        // Parse the request signature.
        // Example: "GET /path/to/resource HTTP/1.1"
        int mark;
        Meta meta = new Meta();
        meta.setMethod(line.substring(0, (mark = line.indexOf(' '))));
        meta.setUrl(line.substring(++mark, (mark = line.indexOf(' ', mark))));
        meta.setProtocol(line.substring(++mark));

        // Parse the request headers
        while (!(line = source.readUtf8LineStrict()).isEmpty())
            meta.addHeader(
                    line.substring(0, (mark = line.indexOf(':'))).trim(),
                    line.substring(++mark).trim());

        info("Request: %s", meta);
        return meta;
    }

    /**
     * Reads the entire request body as defined by header fields (either by
     * "Content-Length: {nbr}" or "Transfer-Encoding: chunked") and temporarily
     * buffers it internally. The buffer can then be passed to the mock response
     * provider, allowing it to pass it further to a real server if needed.
     * <p>
     * This method won't check the validity of request method vs. body content.
     *
     * @param meta   The meta data describing the client HTTP request.
     * @param source The data source to read the body from.
     * @return A buffer containing the fully read body or null if there is no
     * body to read.
     * @throws IOException If the body couldn't be read.
     */
    private Buffer readRequestBody(final Meta meta, final BufferedSource source) throws IOException {
        Buffer buffer = null;

        try {
            HeaderManager headerManager = meta.headerManager();

            // Regular request body.
            if (headerManager.isExpectedToHaveBody()) {
                String headerValue = headerManager.getMostRecent("Content-Length");
                long count = Utils.notEmpty(headerValue) ?
                        Long.valueOf(headerValue, 10) :
                        Long.MAX_VALUE;

                buffer = new Buffer();
                transfer(count, source, buffer, null);
                return buffer;
            }

            // Chunked request body
            if (headerManager.isExpectedToBeChunked()) {
                buffer = new Buffer();
                String chunkSizeLine;
                int chunkSize;
                do {
                    chunkSizeLine = source.readUtf8LineStrict();
                    chunkSize = Integer.valueOf(chunkSizeLine, 16);
                    buffer.writeUtf8(chunkSizeLine);
                    buffer.writeUtf8("\r\n");
                    transfer(chunkSize, source, buffer, null);
                    buffer.writeUtf8("\r\n");
                } while (chunkSize != 0);

                return buffer;
            }

            return null;
        } finally {
            closeSilently(buffer);
        }
    }

    /**
     * Writes the mocked mockResponse back to the waiting http client.
     *
     * @param response The mocked response to serve.
     * @param target   The target destination to write to.
     * @throws IOException If the write operation would fail for some reason.
     */
    private void writeResponse(final MockResponse response,
                               final BufferedSink target) throws IOException {

        Source source = null;
        Buffer buffer = null;

        try {
            // Maybe buffer response body.
            HeaderManager headerManager = response.headerManager();
            source = Okio.source(new ByteArrayInputStream(response.body().getBytes()));

            if (!headerManager.isExpectedToContinue() && (headerManager.isExpectedToHaveBody() || headerManager.isExpectedToBeChunked())) {
                buffer = new Buffer();
                transfer(-1, source, buffer, null);
            }

            // Send the response meta
            StringBuilder builder = new StringBuilder();
            builder.append(String.format("HTTP/1.1 %s %s\r\n", response.code(), response.phrase()));
            List<String> headers = headerManager.getAllAsList();
            for (int i = 0, c = headers.size(); i < c; i += 2)
                builder.append(String.format("%s: %s\r\n", headers.get(i), headers.get(i + 1)));

            // Maybe set Content-Length header
            String value = headerManager.getMostRecent("Content-Length");
            if (isEmpty(value)) {
                if (headerManager.isExpectedToContinue()) {
                    builder.append("Content-Length: 0\r\n");
                } else if (!headerManager.isExpectedToBeChunked() && buffer != null) {
                    builder.append(String.format("Content-Length: %s\r\n", buffer.size()));
                }
            }

            builder.append("\r\n");
            String string = builder.toString();
            target.writeUtf8(string);
            target.flush();
            info("Response: %s", string);

            // Maybe send response body
            if (buffer != null) {
                SettingsManager throttle = response.settingsManager();
                transfer(buffer.size(), buffer, target, throttle);
            }
        } finally {
            closeSilently(source);
            closeSilently(buffer);
        }
    }

    /**
     * Transfers content from a source to a target. The transfer is performed
     * chunk-wise as defined by the given throttle settings.
     *
     * @param byteCount The desired number of bytes to transfer. This method
     *                  will not wait for any more bytes if the source is
     *                  drained before this count is reached.
     * @param source    The byte stream source.
     * @param target    The transfer target destination.
     * @param settings  The throttle settings that describe the chunk size and
     *                  pause period. Null triggers default throttling.
     * @throws IOException If the read or write operation would fail for some
     *                     reason.
     */
    private void transfer(final long byteCount,
                          final Source source,
                          final Sink target,
                          final SettingsManager settings) throws IOException {

        SettingsManager throttle = settings == null ?
                new SettingsManager() :
                settings;

        long chunk = throttle.throttleByteCount();
        long delay = throttle.throttleDelayMillis();
        long remaining = byteCount != -1 ?
                byteCount :
                chunk;

        Buffer buffer = new Buffer();
        while (remaining > 0) {
            long progress = 0;

            if (delay > 0L)
                sleepSilently(delay);

            while (progress < chunk && remaining > 0L) {
                long read = source.read(buffer, Math.min(remaining, chunk));
                if (read == -1)
                    return;

                target.write(buffer, read);
                target.flush();
                progress += read;
                remaining -= read;
            }
        }
    }
}
