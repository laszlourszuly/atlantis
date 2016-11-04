package com.echsylon.atlantis;

import android.content.Context;
import android.os.AsyncTask;

import com.echsylon.atlantis.internal.UrlUtils;
import com.echsylon.atlantis.internal.Utils;
import com.echsylon.atlantis.internal.json.JsonParser;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Locale;
import java.util.Map;
import java.util.Stack;

import fi.iki.elonen.NanoHTTPD;

/**
 * This is the local web server that will serve the template responses. This web server will only
 * serve mocked responses for requests targeting "localhost". If configured so, a request can be
 * delegated to a "real world" server if no corresponding configuration is found for a certain
 * request.
 */
@SuppressWarnings("WeakerAccess") // Suppress Lint Warning on public method visibility
public class Atlantis {
    private static final String HOSTNAME = "localhost";
    private static final int PORT = 8080;

    /**
     * This is a converter class, representing an HTTP status as the NanoHTTPD class knows it.
     * Atlantis only works with integers and strings when it comes to HTTP status code, hence the
     * need for this class.
     */
    private static final class NanoStatus implements NanoHTTPD.Response.IStatus {
        private final int code;
        private final String name;

        private NanoStatus(int code, String name) {
            this.code = code;
            this.name = name;
        }

        @Override
        public String getDescription() {
            return String.format(Locale.ENGLISH, "%d %s", code, name);
        }

        @Override
        public int getRequestStatus() {
            return code;
        }
    }

    /**
     * This is a callback interface through which any asynchronous success states are notified.
     */
    public interface OnSuccessListener {

        /**
         * Delivers a success notification.
         */
        void onSuccess();
    }

    /**
     * THis is a callback interface through which any asynchronous exceptions are notified.
     */
    public interface OnErrorListener {

        /**
         * Delivers an error result.
         *
         * @param cause The cause of the error.
         */
        void onError(Throwable cause);
    }

    /**
     * Starts the local Atlantis server. The caller must manually set a configuration (either by
     * pointing to a JSON asset or by injecting programmatically defined request templates). The
     * success callback is called once the server is fully operational.
     *
     * @param successListener The success callback implementation.
     * @param errorListener   The error callback implementation.
     * @return An Atlantis object instance.
     */
    public static Atlantis start(OnSuccessListener successListener, OnErrorListener errorListener) {
        Atlantis atlantis = new Atlantis();

        try {
            // Starting the nanoHTTPD server on the calling thread. This may cause a grumpy mode in
            // Android (especially with Strict Mode enabled), while nanoHTTPD internally will force
            // the calling thread to sleep. The sleep is for a very short amount of time, but
            // nonetheless forceful. We need to keep an eye on this.
            atlantis.nanoHTTPD.start();
            if (successListener != null)
                successListener.onSuccess();
        } catch (IOException e) {
            if (errorListener != null)
                errorListener.onError(e);
        }

        return atlantis;
    }

    /**
     * Starts the local Atlantis server and automatically loads a configuration from a JSON asset.
     *
     * @param context         The context to use while loading any assets.
     * @param configAssetName The name of the configuration asset file to load.
     * @param successListener The success callback implementation.
     * @param errorListener   The error callback implementation.
     * @return An Atlantis object instance.
     */
    public static Atlantis start(Context context, String configAssetName, OnSuccessListener successListener, OnErrorListener errorListener) {
        Atlantis atlantis = new Atlantis();

        try {
            // Starting the nanoHTTPD server on the calling thread. This may cause a grumpy mode in
            // Android (especially with Strict Mode enabled), while nanoHTTPD internally will force
            // the calling thread to sleep. The sleep is for a very short amount of time, but
            // nonetheless forceful. We need to keep an eye on this.
            atlantis.nanoHTTPD.start();
            atlantis.setConfiguration(context, configAssetName, successListener, errorListener);
        } catch (IOException e) {
            if (errorListener != null)
                errorListener.onError(e);
        }

        return atlantis;
    }

    /**
     * Starts the local Atlantis server and automatically loads a configuration from a JSON file.
     *
     * @param context         The context to use while loading any assets.
     * @param configFile      The file to load the configuration from.
     * @param successListener The success callback implementation.
     * @param errorListener   The error callback implementation.
     * @return An Atlantis object instance.
     */
    public static Atlantis start(Context context, File configFile, OnSuccessListener successListener, OnErrorListener errorListener) {
        Atlantis atlantis = new Atlantis();

        try {
            // Starting the nanoHTTPD server on the calling thread. This may cause a grumpy mode in
            // Android (especially with Strict Mode enabled), while nanoHTTPD internally will force
            // the calling thread to sleep. The sleep is for a very short amount of time, but
            // nonetheless forceful. We need to keep an eye on this.
            atlantis.nanoHTTPD.start();
            atlantis.setConfiguration(context, configFile, successListener, errorListener);
        } catch (IOException e) {
            if (errorListener != null)
                errorListener.onError(e);
        }

        return atlantis;
    }

    /**
     * Starts the local Atlantis server and automatically sets a built configuration.
     *
     * @param context         The context to use while loading any response assets.
     * @param configuration   The built configuration object.
     * @param successListener The success callback implementation.
     * @param errorListener   The error callback implementation.
     * @return An Atlantis object instance.
     */
    public static Atlantis start(Context context, Configuration configuration, OnSuccessListener successListener, OnErrorListener errorListener) {
        Atlantis atlantis = new Atlantis();

        try {
            // Starting the nanoHTTPD server on the calling thread. This may cause a grumpy mode in
            // Android (especially with Strict Mode enabled), while nanoHTTPD internally will force
            // the calling thread to sleep. The sleep is for a very short amount of time, but
            // nonetheless forceful. We need to keep an eye on this.
            atlantis.nanoHTTPD.start();
            atlantis.setConfiguration(context, configuration);

            if (successListener != null)
                successListener.onSuccess();
        } catch (IOException e) {
            if (errorListener != null)
                errorListener.onError(e);
        }

        return atlantis;
    }

    private Context context;
    private NanoHTTPD nanoHTTPD;
    private Configuration configuration;

    private boolean isCapturing;
    private final Object captureLock;
    private volatile Stack<Request> captured;


    // Intentionally hidden constructor.
    private Atlantis() {
        this.isCapturing = false;
        this.captureLock = new Object();
        this.captured = new Stack<>();
        this.nanoHTTPD = new NanoHTTPD(HOSTNAME, PORT) {
            @Override
            public Response serve(IHTTPSession session) {
                // Early bail-out.
                if (configuration == null)
                    return super.serve(session);

                // Get a response to deliver.
                com.echsylon.atlantis.Response response = getMockedResponse(
                        session.getUri(),
                        session.getMethod().name(),
                        session.getHeaders());

                // No response found, try to fall back to the real world.
                if (response == null)
                    if (configuration.hasAlternativeRoute())
                        response = getRealResponse(configuration.fallbackBaseUrl(),
                                session.getUri(),
                                session.getMethod().name(),
                                session.getHeaders());

                // Nope, the real world isn't any better. Bail out and serve default response.
                if (response == null)
                    return super.serve(session);

                // We have a response. Maybe delay before actually delivering it. Relax, this
                // is a worker thread.
                long delay = response.delay();
                if (delay > 0L)
                    try {
                        Thread.sleep(delay);
                    } catch (InterruptedException ignored) {
                    }

                // Now, finally, deliver.
                try {
                    NanoStatus status = new NanoStatus(response.statusCode(), response.statusName());
                    String mime = response.mimeType();
                    byte[] bytes = response.hasAsset() ?
                            response.asset(context) :
                            response.content().getBytes();
                    return newFixedLengthResponse(status, mime, new ByteArrayInputStream(bytes), bytes.length);
                } catch (Exception e) {
                    return newFixedLengthResponse(
                            Response.Status.INTERNAL_ERROR,
                            NanoHTTPD.MIME_PLAINTEXT,
                            "SERVER INTERNAL ERROR: Exception: " + e.getMessage());
                }
            }
        };
    }

    /**
     * Stops the local web server.
     */
    public void stop() {
        configuration = null;
        nanoHTTPD.closeAllConnections();
        nanoHTTPD.stop();
        nanoHTTPD = null;
        isCapturing = false;
    }

    /**
     * Reconfigures Atlantis by setting the new set of requests to respond to.
     *
     * @param context       The context to use when reading assets.
     * @param configuration The new configuration.
     */
    public void setConfiguration(Context context, Configuration configuration) {
        this.context = context;
        this.configuration = configuration;
    }

    /**
     * Reconfigures Atlantis from a configuration JSON asset. The asset is read from disk on a
     * worker thread. Any results are notified through the given callbacks, if given.
     *
     * @param context         The context to use when reading assets.
     * @param configAssetName The name of the configuration asset file (relative to the apps
     *                        'assets' folder).
     * @param successListener The success callback.
     * @param errorListener   The error callback.
     */
    public void setConfiguration(final Context context, final String configAssetName, final OnSuccessListener successListener, final OnErrorListener errorListener) {
        this.context = context;
        if (configAssetName == null)
            this.configuration = null;

        new AsyncTask<Void, Void, Throwable>() {
            @Override
            protected Throwable doInBackground(Void... nothings) {
                try {
                    byte[] bytes = Utils.readAsset(context, configAssetName);
                    String json = new String(bytes);

                    JsonParser jsonParser = new JsonParser();
                    configuration = jsonParser.fromJson(json, Configuration.class);
                    return null;
                } catch (Exception e) {
                    return e;
                }
            }

            @Override
            protected void onPostExecute(Throwable throwable) {
                if (throwable == null) {
                    if (successListener != null)
                        successListener.onSuccess();
                } else {
                    if (errorListener != null)
                        errorListener.onError(throwable);
                }
            }
        }.execute();
    }

    /**
     * Reconfigures Atlantis from a configuration JSON file. The asset is read from disk on a
     * worker thread. Any results are notified through the given callbacks, if given.
     *
     * @param context         The context to use when reading assets.
     * @param file            The configuration JSON file to read.
     * @param successListener The success callback.
     * @param errorListener   The error callback.
     */
    public void setConfiguration(final Context context, final File file, final OnSuccessListener successListener, final OnErrorListener errorListener) {
        this.context = context;
        if (file == null)
            this.configuration = null;

        new AsyncTask<Void, Void, Throwable>() {
            @Override
            protected Throwable doInBackground(Void... nothings) {
                try {
                    //noinspection ConstantConditions
                    String json = new String(Utils.readFile(file));
                    JsonParser jsonParser = new JsonParser();
                    configuration = jsonParser.fromJson(json, Configuration.class);
                    return null;
                } catch (Exception e) {
                    return e;
                }
            }

            @Override
            protected void onPostExecute(Throwable throwable) {
                if (throwable == null) {
                    if (successListener != null)
                        successListener.onSuccess();
                } else {
                    if (errorListener != null)
                        errorListener.onError(throwable);
                }
            }
        }.execute();
    }

    /**
     * Writes the current Atlantis configuration to a file on the filesystem. The configuration is
     * written to disk on a worker thread. Any results are notified through the given callbacks, if
     * given.
     *
     * @param file            The file to write the configuration to.
     * @param successListener The success callback.
     * @param errorListener   The error callback.
     */
    public void writeConfigurationToFile(final File file, final OnSuccessListener successListener, final OnErrorListener errorListener) {
        new AsyncTask<Void, Void, Throwable>() {
            @Override
            protected Throwable doInBackground(Void... nothings) {
                try {
                    JsonParser jsonParser = new JsonParser();
                    String json = jsonParser.toJson(configuration, Configuration.class);
                    Utils.writeFile(json, file);
                    return null;
                } catch (Exception e) {
                    return e;
                }
            }

            @Override
            protected void onPostExecute(Throwable throwable) {
                if (throwable == null) {
                    if (successListener != null)
                        successListener.onSuccess();
                } else {
                    if (errorListener != null)
                        errorListener.onError(throwable);
                }
            }
        }.execute();
    }

    /**
     * Tells the local web server to start capturing a copy of any served requests. This method will
     * start a new capture session by clearing the capture history stack.
     */
    public void startCapturing() {
        clearCapturedRequests();
        isCapturing = true;
    }

    /**
     * Tells the local web server to stop capturing any served requests. This method leaves the
     * capture history stack intact.
     */
    public void stopCapturing() {
        isCapturing = false;
    }

    /**
     * Returns a snapshot of the capture history stack as it looks right this moment. This method
     * leaves the actual stack intact, but new requests may be added to it at any time and these new
     * additions won't be reflected in the output of this method.
     *
     * @return A snapshot of the captured history stack.
     */
    public Stack<Request> getCapturedRequests() {
        Stack<Request> result = new Stack<>();
        synchronized (captureLock) {
            result.addAll(captured);
        }
        return result;
    }

    /**
     * Clears any captured requests in the history stack. This method will not affect the capturing
     * state.
     */
    public void clearCapturedRequests() {
        synchronized (captureLock) {
            captured.clear();
        }
    }

    // Tries to find a response configuration that matches the given request parameters. Also
    // pushes the request to the capture stack if in such a state.
    private Response getMockedResponse(String url, String method, Map<String, String> headers) {
        Request request = configuration.findRequest(url, method, headers);

        if (isCapturing)
            synchronized (captureLock) {
                captured.push(request != null ?
                        request :
                        new Request.Builder()
                                .withUrl(url)
                                .withMethod(method)
                                .withHeaders(headers));
            }

        return request != null ?
                request.response() :
                null;
    }

    // Synchronously makes a real network request and returns the response as an Atlantis response,
    // or null would anything go wrong. This request is completely anonymous from the local web
    // servers (currently NanoHTTPD) point of view as the internal state (cookies and what-not)
    // won't be updated with these real world parameters. Would the real world request fail, then
    // the local web server would serve a response suggesting the local request failed (i.e.
    // "http://localhost:8080" and not "http://www.realworld.com").
    private Response getRealResponse(String realBaseUrl, String requestUrl, String method, Map<String, String> headers) {
        HttpURLConnection connection = null;
        String realUrl = String.format("%s%s%s%s", realBaseUrl,
                UrlUtils.getPath(requestUrl),
                UrlUtils.getQuery(requestUrl),
                UrlUtils.getFragment(requestUrl));

        try {
            // Initiate the real world request.
            URL url = new URL(realUrl);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod(method);
            connection.getRequestProperties();

            // Some headers we don't want to leak to the real world at all.
            // These seem to be added by NanoHttpd.
            headers.remove("host");
            headers.remove("remote-addr");
            headers.remove("http-client-ip");

            // But the rest we might want to expose, still making sure we don't overwrite stuff.
            if (!headers.isEmpty())
                for (Map.Entry<String, String> entry : headers.entrySet())
                    if (connection.getRequestProperty(entry.getKey()) == null)
                        connection.setRequestProperty(entry.getKey(), entry.getValue());

            // And so build an Atlantis response from the real world response.
            return new com.echsylon.atlantis.Response.Builder()
                    .withStatus(UrlUtils.getResponseCode(connection), UrlUtils.getResponseMessage(connection))
                    .withMimeType(UrlUtils.getResponseMimeType(connection))
                    .withHeaders(UrlUtils.getResponseHeaders(connection))
                    .withAsset(UrlUtils.getResponseBody(connection));
        } catch (IOException e) {
            return null;
        } finally {
            UrlUtils.closeSilently(connection);
        }
    }

}
