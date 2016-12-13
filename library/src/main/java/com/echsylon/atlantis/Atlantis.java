package com.echsylon.atlantis;


import android.content.Context;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import okio.BufferedSink;
import okio.BufferedSource;
import okio.Okio;
import okio.Source;

import static com.echsylon.atlantis.LogUtils.info;
import static com.echsylon.atlantis.Utils.closeSilently;
import static com.echsylon.atlantis.Utils.isEmpty;
import static com.echsylon.atlantis.Utils.notEmpty;

/**
 * This is the top level orchestration layer, on top of the mock web mockServer,
 * that will serve the mock responses. The web mockServer will only serve mocked
 * responses for requests targeting "localhost".
 */
@SuppressWarnings({"WeakerAccess", "unused"})
public class Atlantis {
    private static final int PORT = 8080;
    private static final MockResponse NOT_FOUND = new MockResponse.Builder()
            .setStatus(404, "Not Found")
            .addHeader("Content-Length", "0")
            .build();


    private Context context;
    private File atlantisDir;
    private MockWebServer mockServer;
    private RealWebServer realServer;
    private Configuration configuration;
    private Queue<MockRequest> servedRequests;

    private boolean recordServedRequests;
    private boolean recordMissingRequests;


    /**
     * Creates an {@code Atlantis} instance and initializes it with a
     * configuration read from an input stream.
     *
     * @param context     The context used when reading mock responses.
     * @param inputStream The input stream to read the {@code Atlantis}
     *                    configuration from.
     */
    public Atlantis(final Context context, final InputStream inputStream) {
        try {
            BufferedSource bufferedSource = Okio.buffer(Okio.source(inputStream));
            String json = bufferedSource.readString(Charset.forName("UTF-8"));
            Configuration configuration = new JsonParser().fromJson(json, Configuration.class);
            init(context, configuration);
        } catch (IOException e) {
            info(e, "Couldn't read configuration from InputStream");
            throw new RuntimeException(e);
        }
    }

    /**
     * Creates an {@code Atlantis} instance and initializes it with a provided
     * configuration object.
     *
     * @param context       The context used when reading mock responses.
     * @param configuration The {@code Atlantis} configuration object.
     */
    public Atlantis(final Context context, final Configuration configuration) {
        init(context, configuration);
    }

    /**
     * Creates an {@code Atlantis} instance and initializes it with a provided
     * configuration object. Additionally it also allows injecting alternative
     * mock web server and real web server.
     * <p>
     * This constructor is only intended for testing purposes.
     *
     * @param context       The context used when reading mock responses.
     * @param realServer    An alternative real web server implementation.
     * @param configuration The {@code Atlantis} configuration object.
     */
    Atlantis(final Context context, final RealWebServer realServer,
             final Configuration configuration) {

        this(context, configuration);
        if (realServer != null)
            this.realServer = realServer;
    }

    /**
     * Starts the {@code Atlantis} mock environment.
     */
    public void start() {
        try {
            // Null InetSocketAddress will force the internal ServerSocket to
            // assume the "wildcard" address (ultimately "localhost") as host,
            // with the given benefit of not attempting to resolve it on the
            // network.
            mockServer.start(null, PORT);
        } catch (IOException e) {
            info(e, "Couldn't start Atlantis");
        }
    }

    /**
     * Stops the {@code Atlantis} mock environment.
     */
    public void stop() {
        try {
            mockServer.stop();
        } catch (IOException e) {
            info(e, "Couldn't stop Atlantis");
        }
    }

    /**
     * Returns a flag telling whether {@code Atlantis} is actively running or
     * not.
     *
     * @return Boolean true if {@code Atlantis} is active, false otherwise.
     */
    public boolean isRunning() {
        return mockServer.isRunning();
    }

    /**
     * Returns a flag telling whether {@code Atlantis} is recording any missing
     * request templates or not.
     *
     * @return Boolean true if missing requests are recorded, false otherwise.
     * @see #setRecordMissingRequestsEnabled(boolean)
     */
    public boolean isRecordingMissingRequests() {
        return recordMissingRequests;
    }

    /**
     * Returns a flag telling whether {@code Atlantis} is recording any served
     * request templates or not.
     *
     * @return Boolean true if served requests are recorded, false otherwise.
     * @see #setRecordServedRequestsEnabled(boolean)
     */
    public boolean isRecordingServedRequests() {
        return recordServedRequests;
    }

    /**
     * Sets a boolean flag enabling or disabling recording of missing request
     * templates. This feature can only be enabled if there is a fallback base
     * url set in the {@code Atlantis} configuration.
     * <p>
     * While this feature is enabled any requests made by the client app that
     * don't have a corresponding request template will be served with a real
     * response instead of the default HTTP 404 "Not found".
     * <p>
     * The request will in such case be relayed to the "real" mockServer and the
     * response will be stored on the filesystem and a corresponding request
     * template will be added to the {@code Atlantis} configuration. Also an
     * updated copy of the configuration JSON will be written to the same
     * location.
     * <p>
     * The recorded requests will be written to the "atlantis" directory in the
     * client apps external files directory
     *
     * @param enabled Boolean true to enable the feature, false to disable it.
     */
    public void setRecordMissingRequestsEnabled(boolean enabled) {
        recordMissingRequests = enabled && notEmpty(configuration.fallbackBaseUrl());
        info("Record missing requests: %s", enabled ? "enabled" : "disabled");
    }

    /**
     * Sets a boolean flag enabling or disabling recording of served request
     * templates.
     * <p>
     * While this feature is enabled any requests made by the client app will be
     * recorded. The caller can then at a later point in time fetch the recorded
     * requests and analyze the serve pattern.
     * <p>
     * NOTE! The responses are not explicitly tracked. When analyzing the
     * records the caller may still be able to extract mock responses from the
     * request templates but there is no guarantee that they will match the
     * actually served mocked response. It all depends on the response filter
     * associated with the particular request template.
     *
     * @param enabled Boolean true to enable the feature, false to disable it.
     */
    public void setRecordServedRequestsEnabled(boolean enabled) {
        recordServedRequests = enabled;
        info("Record served requests: %s", enabled ? "enabled" : "disabled");
        if (enabled)
            servedRequests.clear();
    }

    /**
     * Returns the configuration of this {@code Atlantis} instance. The caller
     * can use this object to inspect the request templates and any associated
     * mock responses that {@code Atlantis} can serve.
     *
     * @return The configuration object.
     */
    public Configuration configuration() {
        return configuration;
    }

    /**
     * Returns an unmodifiable list of served mock requests. The first item
     * (list position 0) will be the first served request since this feature was
     * enabled and the last item (list position {@code size() - 1}) will be the
     * last served request.
     *
     * @return The unmodifiable served mock requests list as per definition in
     * {@link Collections#unmodifiableList(List)}.
     */
    public List<MockRequest> servedRequests() {
        List<MockRequest> served = new ArrayList<>(servedRequests);
        return Collections.unmodifiableList(served);
    }


    /**
     * Initializes the internal state.
     *
     * @param context       The context used when reading mock response
     *                      resources.
     * @param configuration The {@code Atlantis} configuration object.
     */
    private void init(final Context context, final Configuration configuration) {
        this.context = context;
        this.configuration = configuration;
        this.realServer = new RealWebServer();
        this.mockServer = new MockWebServer(this::serve);
        this.servedRequests = new ConcurrentLinkedQueue<>();
        this.atlantisDir = new File(context.getExternalFilesDir(null), "atlantis");
    }

    /**
     * Identifies a request from an HTTP client and provides a mocked response
     * for it.
     *
     * @param meta   The request meta data sent by the HTTP client.
     * @param source The data source stream, providing any request payload.
     * @return The suggested mock response.
     */
    private MockResponse serve(final Meta meta, final Source source) {
        MockRequest mockRequest = configuration.request(meta);
        if (mockRequest == null)
            if (notEmpty(configuration.fallbackBaseUrl())) {
                String url = configuration.fallbackBaseUrl() + meta.url();
                mockRequest = realServer.getRealTemplate(url, meta, source,
                        recordMissingRequests ?
                                atlantisDir :
                                null);
            }

        if (mockRequest == null)
            mockRequest = getFallbackTemplate(meta);

        if (recordServedRequests)
            servedRequests.add(mockRequest);

        if (recordMissingRequests) {
            configuration.addRequest(mockRequest);
            writeConfigurationToFile(configuration, atlantisDir);
        }

        MockResponse mockResponse = mockRequest.response();
        if (mockResponse == null)
            return NOT_FOUND;

        mockResponse.setSourceHelperIfAbsent(this::open);
        return mockResponse;
    }

    /**
     * Returns a data source through which the mocked response content body can
     * be read.
     *
     * @param text The text describing the resource. Interpreted as an asset
     *             path if starting with "asset://", or a file path if starting
     *             with "file://" or text if not empty.
     * @return A data source or null if no data described.
     */
    private Source open(final String text) {
        if (isEmpty(text))
            return null;

        if (text.startsWith("asset://"))
            try {
                String asset = text.substring(8);
                InputStream inputStream = context.getAssets().open(asset);
                return Okio.source(inputStream);
            } catch (IOException e) {
                info(e, "Couldn't open source: %s", text);
                return null;
            }

        if (text.startsWith("file://"))
            try {
                String file = text.substring(7);
                return Okio.source(new File(file));
            } catch (FileNotFoundException e) {
                info(e, "Couldn't open source: %s", text);
                return null;
            }

        if (notEmpty(text))
            try {
                InputStream inputStream = new ByteArrayInputStream(text.getBytes("UTF-8"));
                return Okio.source(inputStream);
            } catch (UnsupportedEncodingException e) {
                info(e, "Couldn't open UTF-8 string source: %s", text);
                return null;
            }

        return null;
    }

    /**
     * Writes a configuration object to a file on the file system.
     *
     * @param configuration The configuration object to persist.
     * @param directory     The directory to persist in.
     * @return The file the configuration object was persisted to.
     */
    @SuppressWarnings("ResultOfMethodCallIgnored") // Ignore file.mkdirs()
    private File writeConfigurationToFile(final Configuration configuration,
                                          final File directory) {
        BufferedSink target = null;
        try {
            JsonParser jsonParser = new JsonParser();
            String json = jsonParser.toJson(configuration, Configuration.class);
            File file = new File(directory, "configuration.json");
            directory.mkdirs();
            target = Okio.buffer(Okio.sink(file));
            target.writeUtf8(json);
            return file;
        } catch (IOException e) {
            info(e, "Couldn't write configuration to file");
            return null;
        } finally {
            closeSilently(target);
        }
    }

    /**
     * Returns a default request template that holds the default response(s) to
     * deliver when no configuration could be found and no real response could
     * be retrieved.
     *
     * @param meta The meta data describing the request.
     * @return A mock request holding any fallback responses.
     */
    private MockRequest getFallbackTemplate(final Meta meta) {
        return new MockRequest.Builder()
                .setMethod(meta.method())
                .setUrl(meta.url())
                .addHeaders(meta.headers())
                .addResponse(NOT_FOUND)
                .build();
    }
}
