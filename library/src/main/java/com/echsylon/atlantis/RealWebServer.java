package com.echsylon.atlantis;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
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
import okio.BufferedSource;
import okio.Okio;
import okio.Source;

import static com.echsylon.atlantis.LogUtils.info;
import static com.echsylon.atlantis.Utils.closeSilently;

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


    private final String baseUrl;

    RealWebServer(final String baseUrl) {
        this.baseUrl = baseUrl;
    }


    /**
     * Performs a request to a real server and returns a mock request with the
     * corresponding real response (now expressed as a mock response).
     *
     * @param meta      The meta data describing the request to make.
     * @param source    The request body content stream.
     * @param directory If given, the directory to persist the response to.
     * @return The mock request describing the real request and wrapping the
     * real response.
     */
    MockRequest getRealTemplate(final Meta meta, final Source source, final File directory) {
        try {
            Response response = getRealResponse(meta, source);
            Headers headers = response.headers();
            MockResponse.Builder mockResponse = new MockResponse.Builder()
                    .setStatus(response.code(), response.message())
                    .setDelay(0, response.receivedResponseAtMillis() -
                            response.sentRequestAtMillis());

            for (String key : headers.names())
                mockResponse.addHeader(key, headers.get(key));

            File file;
            if (directory != null)
                if ((file = writeResponseToFile(response, directory)) != null)
                    mockResponse.setBody(file);

            return new MockRequest.Builder()
                    .setMethod(meta.method())
                    .setUrl(meta.url())
                    .addHeaders(meta.headers())
                    .addResponse(mockResponse.build())
                    .build();
        } catch (IOException e) {
            info(e, "Couldn't prepare real request: %s %s", meta.method(), meta.url());
            return null;
        }
    }

    /**
     * Gets a real response from a real server.
     *
     * @param meta   The meta data describing the request.
     * @param source The request body content stream.
     * @return The real response as delivered by the backing HTTP client.
     * @throws IOException if anything would go wrong during the request.
     */
    private Response getRealResponse(final Meta meta, final Source source) throws IOException {
        String contentType = meta.headers().get("Content-Type");
        RequestBody body = new SourceRequestBody(contentType, source);

        Request.Builder request = new Request.Builder();
        request.url(baseUrl + meta.url());
        request.method(meta.method(), body);
        for (Map.Entry<String, String> entry : meta.headers().entrySet())
            request.addHeader(entry.getKey(), entry.getValue());

        return new OkHttpClient()
                .newCall(request.build())
                .execute();
    }

    /**
     * Persists a real response to a file on the file system.
     *
     * @param response  The real response to persist.
     * @param directory The directory to persist in.
     * @return The persisted file handle.
     */
    private File writeResponseToFile(final Response response, final File directory) {
        BufferedSource source = null;
        BufferedSink target = null;

        try {
            ResponseBody responseBody = response.body();
            source = responseBody.source();
            if (responseBody.contentLength() <= 0L)
                return null;

            File file = getTargetFile(response.request(), directory);
            target = Okio.buffer(Okio.sink(file));
            target.writeAll(source);

            return file;
        } catch (Exception e) {
            info(e, "Couldn't save mock response: %s", response.request().url().toString());
            return null;
        } finally {
            closeSilently(source);
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
        file.mkdirs();

        return file;
    }
}
