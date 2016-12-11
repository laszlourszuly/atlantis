package com.echsylon.atlantis;

import android.net.Uri;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

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

class RealWebServer {

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
        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/json");

        ResponseBody responseBody = null;
        try {
            Response response = getRealResponse(url, "GET", headers, null);
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
     * @param baseUrl     The base url for the "real" endpoint. The meta object
     *                    will hold the remaining request information, like path
     *                    method etc.
     * @param meta        The meta data describing the request to make.
     * @param requestBody The request body content stream.
     * @param directory   If given, the directory to persist the response to.
     * @return The mock request describing the real request and wrapping the
     * real response.
     */
    MockRequest getRealTemplate(final String baseUrl,
                                final Meta meta,
                                final Source requestBody,
                                final File directory) {

        ResponseBody responseBody = null;
        try {
            // We're leaving the Atlantis universe, let's reflect the new
            // reality in the "Host" header as well.
            meta.addHeader("Host", Uri.parse(baseUrl).getHost());

            // Now get the real response.
            String url = baseUrl + meta.url();
            Response response = getRealResponse(url, meta.method(), meta.headers(), requestBody);
            MockResponse.Builder mockResponse = new MockResponse.Builder()
                    .setStatus(response.code(), response.message())
                    .addSetting(SettingsManager.THROTTLE_MAX_DELAY_MILLIS,
                            Long.toString(response.receivedResponseAtMillis() -
                                    response.sentRequestAtMillis()));

            Headers headers = response.headers();
            for (String key : headers.names())
                mockResponse.addHeader(key, headers.get(key));

            responseBody = response.body();
            if (responseBody.contentLength() > 0L) {
                byte[] bytes = responseBody.bytes();

                if (directory == null) {
                    mockResponse.setBody(bytes);
                } else {
                    File file = writeResponseToFile(bytes, directory, response.request());
                    if (file != null)
                        mockResponse.setBody("file://" + file.getAbsolutePath());
                }
            }

            return new MockRequest.Builder()
                    .setMethod(meta.method())
                    .setUrl(meta.url())
                    .addResponse(mockResponse.build())
                    .build();
        } catch (IOException e) {
            info(e, "Couldn't prepare real request, ignoring: %s %s", meta.method(), meta.url());
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
     * @param url         The request URL to get a response from.
     * @param method      The request method ("GET", "POST", etc).
     * @param headers     The request headers. Null means no headers.
     * @param requestBody The request body content stream.
     * @return The real response as delivered by the backing HTTP client.
     * @throws IOException if anything would go wrong during the request.
     */
    private Response getRealResponse(final String url,
                                     final String method,
                                     final Map<String, String> headers,
                                     final Source requestBody) throws IOException {

        Request.Builder request = new Request.Builder();
        request.url(url);

        if (notEmpty(headers))
            for (Map.Entry<String, String> entry : headers.entrySet())
                request.addHeader(entry.getKey(), entry.getValue());

        if (requestBody != null) {
            String contentType = notEmpty(headers) ?
                    headers.get("Content-Type") : null;
            RequestBody body = new SourceRequestBody(contentType, requestBody);
            request.method(method, body);
        }

        return new OkHttpClient()
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
