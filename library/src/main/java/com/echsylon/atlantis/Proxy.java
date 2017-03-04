package com.echsylon.atlantis;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import okhttp3.Headers;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okio.BufferedSink;
import okio.Okio;
import okio.Source;

import static com.echsylon.atlantis.LogUtils.info;
import static com.echsylon.atlantis.Utils.closeSilently;
import static com.echsylon.atlantis.Utils.isEmpty;
import static com.echsylon.atlantis.Utils.notEmpty;

@SuppressWarnings("unused")
class Proxy {

    /**
     * This class provides the body content to the real request as expected by
     * the backing HTTP client.
     */
    private static final class SourceRequestBody extends RequestBody {
        private final String contentType;
        private final Source source;

        private SourceRequestBody(final String contentType, final Source source) {
            this.contentType = contentType;
            this.source = source;
        }

        @Override
        public MediaType contentType() {
            return MediaType.parse(contentType);
        }

        @Override
        public void writeTo(BufferedSink sink) throws IOException {
            sink.writeAll(source);
        }
    }


    /**
     * Tries to get the Atlantis configuration JSON as a given URL.
     *
     * @param url The URL allegedly serving an Atlantis configuration.
     * @return The Atlantis configuration object expressed as a JSON string or
     * an empty string. Never null.
     */
    String getRealConfigurationJson(final String url) {
        List<String> headers = Arrays.asList("Content-Type", "application/json");
        ResponseBody responseBody = null;
        try {
            Response response = getRealResponse(url, "GET", headers, null, null);
            responseBody = response.body();
            return response.code() == 200 ?
                    responseBody.string() :
                    "";
        } catch (IOException e) {
            info(e, "Couldn't get configuration, continuing with empty json: %s", url);
            return "";
        } finally {
            closeSilently(responseBody);
        }
    }

    /**
     * Performs a request to a real server and returns a mock request with the
     * corresponding real response (now expressed as a mock response).
     * <p>
     * If a directory is given, then the response will also be persisted in a
     * sub folder of it. The path to the persisted response file will be as:
     * <p>
     * {@code <directory>/<request_method>/<request_path>/<response_timestamp>}
     *
     * @param realBaseUrl     The base url for the "real" endpoint. The meta
     *                        object will hold the remaining request metrics,
     *                        like method, headers, path etc.
     * @param meta            The meta data describing the request to make.
     * @param requestBody     The request body content stream.
     * @param defaultSettings The default settings for all responses. This may
     *                        constrain how the response is fetched (e.g.
     *                        whether to follow redirects or not).
     * @param directory       If given, the directory to persist the response
     *                        to.
     * @return The mock request describing the real request and wrapping the
     * real response.
     */
    MockRequest getMockRequest(final String realBaseUrl,
                               final Meta meta,
                               final Source requestBody,
                               final SettingsManager defaultSettings,
                               final Atlantis.TransformationHelper transformationHelper,
                               final File directory,
                               final boolean doRecord,
                               final boolean doRecordFailure) {

        String url = realBaseUrl + meta.url();
        ResponseBody responseBody = null;

        try {
            // Now get the real response.
            MockRequest mockRequest = new MockRequest.Builder(meta).build();
            if (transformationHelper != null)
                mockRequest = transformationHelper.prepareForRealWorld(realBaseUrl, mockRequest);

            Response response = getRealResponse(url,
                    mockRequest.method(),
                    mockRequest.headerManager().getAllAsList(),
                    requestBody,
                    defaultSettings);

            MockResponse.Builder builder = new MockResponse.Builder()
                    .setStatus(response.code(), response.message())
                    .addSetting(SettingsManager.THROTTLE_MAX_DELAY_MILLIS,
                            Long.toString(response.receivedResponseAtMillis() -
                                    response.sentRequestAtMillis()));

            Headers headers = response.headers();
            for (String key : headers.names())
                builder.addHeaders(key, headers.values(key));

            responseBody = response.body();
            if (responseBody.contentLength() > 0L) {
                byte[] bytes = responseBody.bytes();
                File file = doRecord && (response.code() < 400 || doRecordFailure) ?
                        writeResponseToFile(bytes, directory, response.request()) :
                        null;

                if (file == null)
                    builder.setBody(new String(bytes));
                else
                    builder.setBody("file://" + file.getAbsolutePath());
            }

            MockResponse mockResponse = builder.build();

            if (transformationHelper != null)
                mockResponse = transformationHelper.prepareForMockedWorld(realBaseUrl, mockResponse);

            return new MockRequest.Builder()
                    .setMethod(meta.method())
                    .setUrl(meta.url())
                    .addResponse(mockResponse)
                    .build();
        } catch (IOException e) {
            info(e, "Couldn't prepare real request, ignoring: %s %s", meta.method(), url);
            return null;
        } finally {
            closeSilently(responseBody);
        }
    }

    /**
     * Gets a real response from a real server. This method does *not* close the
     * response body. The caller must ensure to close it to avoid resource
     * leaks.
     *
     * @param url             The request URL to get a response from.
     * @param method          The request method ("GET", "POST", etc).
     * @param headers         The request headers. Null means no headers.
     * @param requestBody     The request body content stream.
     * @param defaultSettings The default settings for all responses.
     * @return The real response as delivered by the backing HTTP client.
     * @throws IOException if anything would go wrong during the request.
     */
    private Response getRealResponse(final String url,
                                     final String method,
                                     final List<String> headers,
                                     final Source requestBody,
                                     final SettingsManager defaultSettings) throws IOException {

        Request.Builder request = new Request.Builder();
        request.url(url);

        String contentType = null;
        if (notEmpty(headers))
            for (int i = 0, c = headers.size(); i < c; i += 2) {
                String key = headers.get(i);
                String value = headers.get(i + 1);
                request.addHeader(key, value);

                if ("Content-Type".equalsIgnoreCase(key))
                    contentType = value;
            }

        if (requestBody != null) {
            RequestBody body = new SourceRequestBody(contentType, requestBody);
            request.method(method, body);
        }

        boolean followRedirects = defaultSettings == null || defaultSettings.followRedirects();
        OkHttpClient httpClient = new OkHttpClient.Builder()
                .followSslRedirects(followRedirects)
                .followRedirects(followRedirects)
                .build();

        return httpClient
                .newCall(request.build())
                .execute();
    }

    /**
     * Persists a real response to a file on the file system.
     *
     * @param body      The real response body to persist.
     * @param directory The directory to persist in.
     * @param request   The request to build subdirectories and file name from.
     * @return The persisted file handle or null.
     */
    private File writeResponseToFile(final byte[] body, final File directory, final Request request) {
        if (isEmpty(body)) {
            info("No response body to persist: %s", request.url());
            return null;
        }

        BufferedSink target = null;

        try {
            File file = getTargetFile(request, directory);
            target = Okio.buffer(Okio.sink(file));
            target.write(body);
            info("Saved response to: %s", file.getAbsolutePath());
            return file;
        } catch (Exception e) {
            info(e, "Couldn't save response: %s", request.url());
            return null;
        } finally {
            closeSilently(target);
        }
    }

    /**
     * Builds the subfolder structure and the file name to persist a real
     * response in.
     *
     * @param request   The request to create the subfolder structure from. The
     *                  file name will be built from the current time stamp.
     * @param directory The root directory.
     * @return The handle to the prepared file.
     */
    @SuppressWarnings("ResultOfMethodCallIgnored") // Ignore file.mkdirs()
    private File getTargetFile(final Request request, final File directory) {
        String method = request.method().toLowerCase();
        String path = request.url().encodedPath();
        String name = new SimpleDateFormat("yyyy-MM-dd_HH:mm:ss_SSS", Locale.US).format(new Date());

        File methodDir = new File(directory, method);
        File pathDir = new File(methodDir, path);
        File file = new File(pathDir, name);
        pathDir.mkdirs();

        return file;
    }
}
