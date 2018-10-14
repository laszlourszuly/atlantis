package com.echsylon.atlantis;


import okio.BufferedSink;
import okio.BufferedSource;
import okio.Okio;
import okio.Source;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.IOException; 
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import static com.echsylon.atlantis.LogUtils.info;
import static com.echsylon.atlantis.Utils.*;

/**
 * This is the top level orchestration layer, on top of the mock web mockServer,
 * that will serve the mock responses. The web mockServer will only serve mocked
 * responses for requests targeting "localhost".
 */
@SuppressWarnings({"WeakerAccess", "unused"})
public class Atlantis {
    private static final int DEFAULT_PORT = 8080;

    private static final MockResponse CONTINUE = new MockResponse.Builder()
            .setStatus(100, "Continue")
            .addHeader("Content-Length", "0")
            .build();

    private static final MockResponse NOT_FOUND = new MockResponse.Builder()
            .setStatus(404, "Not Found")
            .addHeader("Content-Length", "0")
            .build();

    private File atlantisDir;
    private MockWebServer mockServer;
    private Proxy proxy;
    private Configuration configuration;
    private Queue<MockRequest> servedRequests;
    private boolean recordServedRequests;
    private boolean recordMissingRequests;
    private boolean recordMissingFailures;

    /**
     * Creates an {@code Atlantis} instance and initializes it with a
     * configuration read from an input stream.
     *
     * @param inputStream The input stream to read the {@code Atlantis}
     *                    configuration from.
     */
    public Atlantis(final InputStream inputStream) {
        try {
            BufferedSource bufferedSource = Okio.buffer(Okio.source(inputStream));
            String json = bufferedSource.readString(Charset.forName("UTF-8"));
            Configuration configuration = JsonParser.fromJson(json, Configuration.class);
            init(configuration);
        } catch (IOException e) {
            info(e, "Couldn't read configuration from InputStream");
            throw new RuntimeException(e);
        }
    }

    /**
     * Creates an {@code Atlantis} instance and initializes it with a provided
     * configuration object.
     *
     * @param configuration The {@code Atlantis} configuration object.
     */
    public Atlantis(final Configuration configuration) {
        init(configuration);
    }


    /**
     * Creates an {@code Atlantis} instance and initializes it with a provided
     * configuration object. Additionally it also allows injecting alternative
     * mock web server and real web server.
     * <p>
     * This constructor is only intended for testing purposes.
     *
     * @param proxy         An alternative real web server implementation.
     * @param configuration The {@code Atlantis} configuration object.
     */
    Atlantis(final Proxy proxy, final Configuration configuration) {

        this(configuration);
        if (proxy != null)
            this.proxy = proxy;
    }

    /**
     * Starts the {@code Atlantis} mock environment at the default port.
     */
    public void start() {
        start(DEFAULT_PORT);
    }

    /**
     * Starts the {@code Atlantis} mock environment at the given port.
     *
     * @param port The port to start listening for network requests at.
     */
    public void start(int port) {
        try {
            // Null InetSocketAddress will force the internal ServerSocket to
            // assume the "wildcard" address (ultimately "localhost") as host,
            // with the given benefit of not attempting to resolve it on the
            // network.
            mockServer.start(null, port);
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
        if (recordMissingRequests && atlantisDir == null) {
            atlantisDir = new File("atlantis");
        }
    }

    /**
     * Sets a boolean flag enabling or disabling recording of missing request
     * templates that returned with an HTTP error from the real server. This
     * feature can only be enabled if the "record missing requests" feature is
     * enabled.
     * <p>
     * The recorded requests will be written to the "atlantis" directory in the
     * client apps external files directory
     *
     * @param enabled Boolean true to enable the feature, false to disable it.
     */
    public void setRecordMissingFailuresEnabled(boolean enabled) {
        recordMissingFailures = enabled && recordMissingRequests;
        info("Record missing requests if failed: %s", enabled ? "enabled" : "disabled");
        if (recordMissingRequests && atlantisDir == null) {
            atlantisDir = new File("atlantis");
        }
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
        if (enabled) {
            servedRequests.clear();
        }
    }

    /**
     * Clears the list of served request templates.
     */
    public void clearServedRequestsRecords() {
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
     * Returns the working directory of Atlantis. This is where any recorded
     * responses are stored.
     *
     * @return The working directory for Atlantis.
     */
    File workingDirectory() {
        return atlantisDir;
    }

    /**
     * Initializes the internal state.
     *
     * @param configuration The {@code Atlantis} configuration object.
     */
    private void init(final Configuration configuration) {
        this.configuration = configuration;
        this.proxy = new Proxy();
        this.mockServer = new MockWebServer(this::serve, this::getSettings);
        this.servedRequests = new ConcurrentLinkedQueue<>();

        NOT_FOUND.setSourceHelperIfAbsent(this::open);
        CONTINUE.setSourceHelperIfAbsent(this::open);
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
        SettingsManager settings = new SettingsManager();
        settings.set(configuration.settingsManager().getAllAsMap());

        MockRequest mockRequest = !meta.headerManager().isExpectedToContinue() ?
                configuration.findRequest(meta) :
                getContinueTemplate(meta);

        if (mockRequest == null) {
            // There is no mock request configuration for this URL. Maybe try to
            // fetch one from the real world.

            info("Couldn't find request template for url: %s", meta.url());
            String realBaseUrl = settings.fallbackBaseUrl();
            if (isEmpty(realBaseUrl))
                return NOT_FOUND;

            info("Falling back to real world: %s", realBaseUrl);
            mockRequest = getRealWorldTemplate(meta, source, realBaseUrl, settings);

            if (mockRequest == null || mockRequest.responses().size() == 0)
                return NOT_FOUND;
        }

        settings.set(mockRequest.settingsManager().getAllAsMap());
        MockResponse mockResponse = mockRequest.response();

        if (mockResponse == null) {
            // There is a mock request for this URL, but there is no mocked
            // response configured for it. Maybe try to fetch one from the
            // real world.

            info("Couldn't find a mock response for url: %s", meta.url());
            String realBaseUrl = settings.fallbackBaseUrl();
            if (isEmpty(realBaseUrl))
                return NOT_FOUND;

            info("Falling back to real world: %s", realBaseUrl);
            MockRequest request = getRealWorldTemplate(meta, source, realBaseUrl, settings);

            if (request == null)
                return NOT_FOUND;

            mockResponse = request.response();
            if (mockResponse == null)
                return NOT_FOUND;
        }

        settings.set(mockResponse.settingsManager().getAllAsMap());

        // Don't expose the internal mock request and mock response objects
        // to any post processing infrastructures but rather pass copies.
        MockResponse responseBeingMocked = new MockResponse.Builder(mockResponse)
                .addHeaders(configuration.defaultResponseHeaderManager().getAllAsMultiMap())
                .build();

        MockRequest requestBeingMocked = new MockRequest.Builder(meta)
                .addResponse(responseBeingMocked)
                .build();

        TokenHelper tokenHelper = settings.tokenHelper();
        if (tokenHelper != null) {
            // Ensure any token helper implementation can read the response body
            // and has access to the collected settings.
            responseBeingMocked.setSourceHelperIfAbsent(this::open);
            responseBeingMocked.settingsManager().set(settings.getAllAsMap());
            responseBeingMocked = tokenHelper.parse(requestBeingMocked, responseBeingMocked);
        }

        if (recordServedRequests)
            servedRequests.add(requestBeingMocked);

        if (recordMissingRequests)
            if (responseBeingMocked.code() < 400 || recordMissingFailures) {
                configuration.addRequest(mockRequest);
                writeConfigurationToFile(configuration, atlantisDir);
            }

        // There is no guarantee that the custom token helper delivered a mock
        // response with an intact source helper. Hence we need to make sure
        // there is one before the response is finally served.
        responseBeingMocked.setSourceHelperIfAbsent(this::open);
        responseBeingMocked.settingsManager().setIfAbsent(settings.getAllAsMap());
        return responseBeingMocked;
    }

    /**
     * Delivers a {@code SettingsManager} containing the merged settings from
     * all provided, non-null entities.
     *
     * @param mockResponse The child mock response object.
     * @return A settings manager. May be empty but never null.
     */
    private SettingsManager getSettings(final MockResponse mockResponse) {
        SettingsManager result = new SettingsManager();

        if (configuration != null)
            result.set(configuration.settingsManager().getAllAsMap());

        if (mockResponse != null)
            result.set(mockResponse.settingsManager().getAllAsMap());

        return result;
    }

    /**
     * Returns a data source through which the mocked response content body can
     * be read. It's the responsibility of the caller to close the returned
     * {@code Source} when finished reading from it.
     *
     * @param content The text describing the resource. Interpreted as an asset
     *                path if starting with "asset://", or a file path if
     *                starting with "file://" or text if not empty.
     * @return A data source or null if no data described.
     */
    private Source open(final byte[] content) {
        if (isEmpty(content) || content.length == 1 && content[0] == (byte) 0)
            return Okio.source(new ByteArrayInputStream(new byte[0]));

        // Check if "file" scheme.
        int schemeByteLength = "file://".getBytes().length;
        String text = content.length > schemeByteLength ? new String(content, 0, schemeByteLength) : "";
        if (text.startsWith("file://"))
            try {
                String fileUri = new String(content);
                String filePath = fileUri.substring(7);
                return Okio.source(new File(filePath));
            } catch (FileNotFoundException e) {
                info(e, "Couldn't open file: %s", text);
                return null;
            }

        // Fall back to content byte array stream.
        InputStream inputStream = new ByteArrayInputStream(content);
        return Okio.source(inputStream);
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
            String json = JsonParser.toJson(configuration, Configuration.class);
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
     * Returns a default request template that holds a default "100 Continue"
     * response to deliver when a corresponding request is made.
     *
     * @param meta The meta data describing the request.
     * @return A request template holding the default "Continue" response.
     */
    private MockRequest getContinueTemplate(final Meta meta) {
        return new MockRequest.Builder()
                .setMethod(meta.method())
                .setUrl(meta.url())
                .addHeaders(meta.headerManager().getAllAsMultiMap())
                .addResponse(CONTINUE)
                .build();
    }

    /**
     * Returns a new request template based on a real world response.
     *
     * @param meta        The meta data describing the request.
     * @param source      The request body source. May be null.
     * @param realBaseUrl The real world base url, e.g. "http://www.google.com".
     * @param settings    The settings that can provide an optional
     *                    transformation helper and other request behavior.
     * @return A request template holding a mock of a real world response.
     */
    private MockRequest getRealWorldTemplate(final Meta meta,
                                             final Source source,
                                             final String realBaseUrl,
                                             final SettingsManager settings) {
        // Prepare a new mock request
        MockRequest mockRequest = new MockRequest.Builder(meta).build();

        // Possibly allow the caller to transform some request metrics.
        TransformationHelper transformationHelper = settings.transformationHelper();
        if (transformationHelper != null)
            mockRequest = transformationHelper.prepareForRealWorld(realBaseUrl, mockRequest);

        // Get a mocked response from the real world.
        MockResponse mockResponse = proxy.getMockResponse(realBaseUrl,
                mockRequest,
                source,
                atlantisDir,
                recordMissingRequests,
                recordMissingFailures,
                settings.followRedirects());

        if (mockResponse == null)
            return null;

        if (transformationHelper != null) {
            // Ensure the source can be read.
            mockResponse.setSourceHelperIfAbsent(this::open);
            mockResponse = transformationHelper.prepareForMockedWorld(realBaseUrl, mockResponse);

            // The caller has returned a new mock response, make sure the source
            // can be read from that one too.
            mockResponse.setSourceHelperIfAbsent(this::open);
        }

        // Return a clean mock request, which hasn't been prepared for a real
        // world request.
        return new MockRequest.Builder(meta)
                .addResponse(mockResponse)
                .build();
    }

    /**
     * This interface describes the API offering means for preparing a request
     * either for the real world or the mock environment. Atlantis will make
     * sure any injected implementation of this interface is called when needed
     * and possible, e.g. there needs to be "fallbackBaseUrl" defined in the
     * configuration for this transformation to take place.
     */
    public interface TransformationHelper {

        /**
         * Returns a new mock request which doesn't reference any features in
         * the mock environment. This indirectly requires any implementing
         * classes to know of the "real world".
         *
         * @param realBaseUrl The base url of the real server.
         * @param mockRequest The unchanged mock request as Atlantis knows it.
         * @return A modified mock request which has all internal URL's and mock
         * references replaced with corresponding real world values. Since the
         * {@code MockRequest} is immutable (-ish), implementing classes must
         * create and return a new instance, preferably by using a {@link
         * MockRequest.Builder}).
         */
        MockRequest prepareForRealWorld(final String realBaseUrl, final MockRequest mockRequest);

        /**
         * Returns a new mock response which doesn't reference any real life
         * resources, but targets internal, mocked, alternatives only.
         *
         * @param realBaseUrl      The base url of the real server.
         * @param recordedResponse The mock response as recorded from the real
         *                         world response.
         * @return A modified mock response which has all external URL's and
         * mock references replaced with corresponding mock values. Since the
         * {@code MockResponse} is immutable (-ish), implementing classes must
         * create and return a new instance, preferably by using a {@link
         * MockResponse.Builder}).
         */
        MockResponse prepareForMockedWorld(final String realBaseUrl, final MockResponse recordedResponse);
    }

    /**
     * This interface describes the token helper feature set. A token helpers
     * main responsibility is to evaluate tokens and replace them with the
     * corresponding evaluated values.
     */
    public interface TokenHelper {

        /**
         * Performs the actual token replacement activities and delivers a new,
         * ready-to-serve mock response.
         *
         * @param requestBeingMocked The request that is about to be served.
         * @param rawMockResponse    The configured or recorded mock response
         *                           (with the yet un-parsed tokens).
         * @return A new, modified mock response that can be served to a waiting
         * client. Note that {@code MockResponse}'s are immutable from the users
         * perspective. Implementing classes will have to create a new {@code
         * MockResponse} instance to return, preferably  by using a {@code
         * MockResponse.Builder} instance.
         */
        MockResponse parse(final MockRequest requestBeingMocked, final MockResponse rawMockResponse);
    }
}
