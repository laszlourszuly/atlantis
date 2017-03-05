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
     * Tries to get the Atlantis configuration JSON at a given URL.
     *
     * @param url The URL allegedly serving an Atlantis configuration.
     * @return The Atlantis configuration object expressed as a JSON string or
     * an empty string. Never null.
     */
    String getRealConfigurationJson(final String url) {
        List<String> headers = Arrays.asList("Content-Type", "application/json");
        ResponseBody responseBody = null;
        try {
            Response response = getRealResponse(url, "GET", headers, null, false);
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
     * @param mockRequest     The prepared mock request to fetch a real world
     *                        response for.
     * @param requestBody     The request body content stream.
     * @param directory       If given, the directory to persist the response
     *                        to.
     * @param doRecord        Whether to record a successful (as per HTTP status
     *                        code) real world response or not. If recorded then
     *                        this response will be delivered for future
     *                        requests as well.
     * @param doRecordFailure Whether to record a failed (as per HTTP status
     *                        code) real world response or not as well.
     * @param followRedirects Whether to follow real world redirects or not.
     * @return The mock request describing the real request and wrapping the
     * real response.
     */
    MockResponse getMockResponse(final String realBaseUrl,
                                 final MockRequest mockRequest,
                                 final Source requestBody,
                                 final File directory,
                                 final boolean doRecord,
                                 final boolean doRecordFailure,
                                 final boolean followRedirects) {

        // Prepare the real world url
        String url = realBaseUrl + mockRequest.url();
        ResponseBody responseBody = null;

        try {
            // Fetch the real response.
            Response response = getRealResponse(url,
                    mockRequest.method(),
                    mockRequest.headerManager().getAllAsList(),
                    requestBody,
                    followRedirects);

            // Build the mock response.
            long t1 = response.sentRequestAtMillis();
            long t2 = response.receivedResponseAtMillis();
            String maxDelay = Long.toString((long) ((t2 - t1) * 1.2f)); // Add 20% slack.
            MockResponse.Builder builder = new MockResponse.Builder()
                    .setStatus(response.code(), response.message())
                    .addSetting(SettingsManager.THROTTLE_MAX_DELAY_MILLIS, maxDelay);

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
                    builder.setBody(bytes);
                else
                    builder.setBody("file://" + file.getAbsolutePath());
            }

            // And return the final mock response.
            return builder.build();
        } catch (IOException e) {
            info(e, "Couldn't prepare real request, ignoring: %s %s", mockRequest.method(), url);
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
     * @param followRedirects Whether to follow any redirects or not.
     * @return The real response as delivered by the backing HTTP client.
     * @throws IOException if anything would go wrong during the request.
     */
    private Response getRealResponse(final String url,
                                     final String method,
                                     final List<String> headers,
                                     final Source requestBody,
                                     final boolean followRedirects) throws IOException {

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
