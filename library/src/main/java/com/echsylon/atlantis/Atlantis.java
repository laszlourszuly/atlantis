package com.echsylon.atlantis;

import android.content.Context;
import android.os.AsyncTask;
import android.text.TextUtils;
import android.util.Log;

import com.echsylon.atlantis.internal.UrlUtils;
import com.echsylon.atlantis.internal.Utils;
import com.echsylon.atlantis.internal.json.JsonParser;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.net.CookieManager;
import java.net.CookieStore;
import java.net.HttpCookie;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Stack;

import fi.iki.elonen.NanoHTTPD;

/**
 * This is the local web server that will serve the template responses. This web
 * server will only serve mocked responses for requests targeting "localhost".
 * If configured so, a request can be delegated to a "real world" server if no
 * corresponding configuration is found for a certain request.
 */
@SuppressWarnings("WeakerAccess")
public class Atlantis {
    private static final String HOSTNAME = "localhost";
    private static final int PORT = 8080;

    /**
     * This is a callback interface through which any asynchronous success
     * states are notified.
     */
    public interface OnSuccessListener {

        /**
         * Delivers a success notification.
         */
        void onSuccess();
    }

    /**
     * This is a callback interface through which any asynchronous exceptions
     * are notified.
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
     * This is a converter class, representing an HTTP status as the NanoHTTPD
     * class knows it. Atlantis only works with integers and strings when it
     * comes to HTTP status code, hence the need for this class.
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
     * Starts the local Atlantis server. The caller must manually set a
     * configuration (either by pointing to a JSON asset or by injecting
     * programmatically defined request templates). The success callback is
     * called once the server is fully operational.
     *
     * @param successListener The success callback implementation.
     * @param errorListener   The error callback implementation.
     * @return An Atlantis object instance.
     * @deprecated Use {@link #start()} instead.
     */
    public static Atlantis start(OnSuccessListener successListener, OnErrorListener errorListener) {
        return start();
    }

    /**
     * Starts the local Atlantis server. The caller must manually set a
     * configuration (either by pointing to a JSON asset or by injecting a
     * programmatically defined request templates).
     *
     * @return An Atlantis object instance.
     */
    public static Atlantis start() {
        Atlantis atlantis = new Atlantis();

        try {
            // Starting the nanoHTTPD server on the calling thread. This may
            // cause a grumpy mode in Android (especially with Strict Mode
            // enabled), while nanoHTTPD internally will force the calling
            // thread to sleep. The sleep is for a very short amount of time,
            // but nonetheless forceful. We need to keep an eye on this.
            atlantis.nanoHTTPD.start();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return atlantis;
    }

    /**
     * Starts the local Atlantis server and automatically loads a configuration
     * from a JSON asset. The asset is loaded from disk on a worker thread and
     * any provided listeners are called once the asset is fully loaded.
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
     * Starts the local Atlantis server and automatically loads a configuration
     * from a JSON file. The file is loaded from disk on a worker thread and any
     * provided listeners are called once the file is fully loaded.
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
     * Starts the local Atlantis server and automatically sets the built
     * configuration object.
     *
     * @param context         The context to use while loading any response
     *                        assets.
     * @param configuration   The built configuration object.
     * @param successListener The success callback implementation.
     * @param errorListener   The error callback implementation.
     * @return An Atlantis object instance.
     * @deprecated Use {@link #start(Context, Configuration)} instead.
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

    /**
     * Starts the local Atlantis server and automatically sets the built
     * configuration object.
     *
     * @param context       The context to use while loading any response
     *                      assets.
     * @param configuration The new configuration object.
     * @return An Atlantis object instance.
     */
    public static Atlantis start(Context context, Configuration configuration) {
        Atlantis atlantis = new Atlantis();

        try {
            // Starting the nanoHTTPD server on the calling thread. This may
            // cause a grumpy mode in Android (especially with Strict Mode
            // enabled), while nanoHTTPD internally will force the calling
            // thread to sleep. The sleep is for a very short amount of time,
            // but nonetheless forceful. We need to keep an eye on this.
            atlantis.nanoHTTPD.start();
            atlantis.setConfiguration(context, configuration);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return atlantis;
    }


    private final Object captureLock;
    private boolean isCapturing;
    private boolean isRecording;
    private Context context;
    private NanoHTTPD nanoHTTPD;
    private Configuration configuration;
    private CookieManager cookieManager;
    private File recordedAssetsDirectory;
    private volatile Stack<Request> captured;

    // Intentionally hidden constructor.
    private Atlantis() {
        this.isCapturing = false;
        this.captureLock = new Object();
        this.captured = new Stack<>();
        this.cookieManager = new CookieManager();
        this.nanoHTTPD = new NanoHTTPD(HOSTNAME, PORT) {

            @Override
            public Response serve(IHTTPSession session) {
                if (configuration == null)
                    return super.serve(session);

                // Collect the HTTP elements used for filtering out the mock
                // request and thereby a corresponding response to serve.
                Map<String, String> headers = session.getHeaders();
                String method = session.getMethod().name();
                String query = session.getQueryParameterString();
                String url = Utils.isEmpty(query) ?
                        session.getUri() :
                        String.format("%s?%s", session.getUri(), query);

                // Try to find a mock request and response configuration for
                // the target HTTP params.
                Request request = configuration.findRequest(url, method, headers);
                if (isCapturing)
                    synchronized (captureLock) {
                        captured.push(request != null ?
                                request :
                                new Request.Builder()
                                        .withUrl(url)
                                        .withMethod(method)
                                        .withHeaders(headers)
                                        .build());
                    }

                com.echsylon.atlantis.Response response = request != null ?
                        request.response() :
                        configuration.hasAlternativeRoute() ?
                                getRealResponse(configuration.fallbackBaseUrl(), url, method, headers) :
                                null;

                // Bummer! Bail out.
                if (response == null)
                    return super.serve(session);

                // We should record fallback requests, make sure we have
                // something to record as well.
                if (isRecording)
                    configuration.addRequest(request != null ?
                            request :
                            new Request.Builder()
                                    .withUrl(url)
                                    .withMethod(method)
                                    .withHeaders(headers)
                                    .withResponse(response)
                                    .build());

                // Deliver the response, maybe delay before actually delivering
                // it. Relax, this is a worker thread.
                try {
                    long delay = response.delay();
                    Thread.sleep(delay);
                    NanoStatus status = new NanoStatus(response.statusCode(), response.statusName());
                    String mime = response.header(HttpEntity.CONTENT_TYPE);
                    byte[] bytes = response.hasAsset() ?
                            response.asset(context) :
                            response.content().getBytes();

                    Response result = newFixedLengthResponse(status, mime, new ByteArrayInputStream(bytes), bytes.length);
                    for (Map.Entry<String, String> header : response.headers().entrySet())
                        result.addHeader(header.getKey(), header.getValue());

                    return result;
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
     * Reconfigures Atlantis from a configuration JSON asset. The asset is read
     * from disk on a worker thread. Any results are notified through the given
     * callbacks, if given.
     *
     * @param context         The context to use when reading assets.
     * @param configAssetName The name of the configuration asset file (relative
     *                        to the apps 'assets' folder).
     * @param successListener The success callback.
     * @param errorListener   The error callback.
     */
    public void setConfiguration(final Context context, final String configAssetName, final OnSuccessListener successListener, final OnErrorListener errorListener) {
        this.context = context;
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
                    configuration = null;
                    if (errorListener != null)
                        errorListener.onError(throwable);
                }
            }
        }.execute();
    }

    /**
     * Reconfigures Atlantis from a configuration JSON file. The asset is read
     * from disk on a worker thread. Any results are notified through the given
     * callbacks, if given.
     *
     * @param context         The context to use when reading assets.
     * @param file            The configuration JSON file to read.
     * @param successListener The success callback.
     * @param errorListener   The error callback.
     */
    public void setConfiguration(final Context context, final File file, final OnSuccessListener successListener, final OnErrorListener errorListener) {
        this.context = context;
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
                    configuration = null;
                    if (errorListener != null)
                        errorListener.onError(throwable);
                }
            }
        }.execute();
    }

    /**
     * Writes the current Atlantis configuration to a file on the filesystem.
     * The configuration is written to disk on a worker thread. Any results are
     * notified through the given callbacks, if given.
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
     * Tells the local web server to start capturing a copy of any served
     * requests. This will start tracking a "request history" which the caller
     * later can use to verify that certain requests have been made.
     * <p>
     * This method will clear any existing capture history stack.
     *
     * @see #stopCapturing()
     * @deprecated This method has a unsuitable name. Use {@link
     * #startCapturingServedRequests()} instead.
     */
    public void startCapturing() {
        startCapturingServedRequests();
    }

    /**
     * Tells the local web server to start capturing a copy of any served
     * requests. This will start tracking a "request history" which the caller
     * later can use to verify that certain requests have been made.
     * <p>
     * This method will clear any existing capture history stack.
     *
     * @see #stopCapturingServedRequests()
     */
    public void startCapturingServedRequests() {
        clearCapturedRequests();
        isCapturing = true;
    }

    /**
     * Tells the local web server to stop capturing any served requests. This
     * method leaves the capture history stack intact.
     *
     * @see #startCapturing()
     * @deprecated This method has a unsuitable name. Use {@link
     * #stopCapturingServedRequests()} instead.
     */
    public void stopCapturing() {
        stopCapturingServedRequests();
    }

    /**
     * Tells the local web server to stop capturing any served requests. This
     * method leaves the capture history stack intact.
     *
     * @see #startCapturingServedRequests()
     */
    public void stopCapturingServedRequests() {
        isCapturing = false;
    }

    /**
     * Enables dynamically adding requests and responses that where dispatched
     * to the reality (by the fallbackUrl field in the configuration). This
     * command is ignored if the configuration doesn't have a fallback url.
     *
     * @param assetDirectory The root directory of the recorded response
     *                       assets.
     */
    public void startRecordingFallbackRequests(File assetDirectory) {
        if (configuration != null && configuration.hasAlternativeRoute()) {
            recordedAssetsDirectory = assetDirectory;
            isRecording = true;
        } else {
            recordedAssetsDirectory = null;
            isRecording = false;
        }
    }

    /**
     * Disables any dynamically adding of fallback requests and responses.
     * Stopping the recording will leave any previously recorded assets intact
     * and the correspondingly added request and response configurations will be
     * included if also writing the configuration file to disk.
     *
     * @see #startRecordingFallbackRequests(File)
     * @see #writeConfigurationToFile(File, OnSuccessListener, OnErrorListener)
     */
    public void stopRecordingFallbackRequests() {
        recordedAssetsDirectory = null;
        isRecording = false;
    }

    /**
     * Returns a snapshot of the capture history stack as it looks right this
     * moment. This method leaves the actual stack intact, but new requests may
     * be added to it at any time and these new additions won't be reflected in
     * the output of this method.
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
     * Clears any captured requests in the history stack. This method will not
     * affect the capturing state.
     */
    public void clearCapturedRequests() {
        synchronized (captureLock) {
            captured.clear();
        }
    }

    /**
     * Returns a boolean flag telling whether Atlantis is in an active state or
     * not.
     *
     * @return Boolean true if Atlantis is running, false otherwise.
     */
    public boolean isRunning() {
        return nanoHTTPD.isAlive();
    }

    /**
     * Returns a boolean flag telling whether Atlantis is recording missing mock
     * requests and responses or not.
     *
     * @return Boolean true if Atlantis is in a recording mode, false otherwise.
     * @deprecated This method has a unsuitable name. Use {@link
     * #isRecordingFallbackRequests()} instead.
     */
    public boolean isRecordingMissingMockData() {
        return isRecordingFallbackRequests();
    }

    /**
     * Returns a boolean flag telling whether Atlantis is recording missing mock
     * requests and responses or not.
     *
     * @return Boolean true if Atlantis is in a recording mode, false otherwise.
     */
    public boolean isRecordingFallbackRequests() {
        return isRecording;
    }

    /**
     * Returns a boolean flag telling whether Atlantis is capturing a copy of
     * any served requests or not.
     *
     * @return Boolean true if Atlantis is in a capturing mode, false otherwise.
     */
    public boolean isCapturingServedRequests() {
        return isCapturing;
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
        cookieManager.getCookieStore().removeAll();
        cookieManager = null;
    }

    // Synchronously makes a real network request and returns the response as
    // an Atlantis response or null, would anything go wrong. If Atlantis is in
    // a recording mode then the real response content is written to the file
    // system and the returned Atlantis response is configured to refer to that
    // resource. If no recording is active, then the returned Atlantis response
    // will hold the actual response byte array and nothing is written to the
    // file system.
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

            // Atlantis can't handle cookies, but "real" requests may still
            // depend on them, hence, we must make sure that we handle cookies
            // for external traffic.
            addCookiesToRealRequest(connection);

            connection.setInstanceFollowRedirects(false);
            connection.setRequestMethod(method);

            // Some headers we don't want to leak to the real world at all.
            // These seem to be added by NanoHttpd.
            headers.remove("host");
            headers.remove("remote-addr");
            headers.remove("http-client-ip");

            // Make sure all provided headers make their way to the real server.
            if (!headers.isEmpty())
                for (Map.Entry<String, String> entry : headers.entrySet())
                    connection.setRequestProperty(entry.getKey(), entry.getValue());

            // Build an Atlantis response from the real world response.
            com.echsylon.atlantis.Response.Builder builder = new com.echsylon.atlantis.Response.Builder()
                    .withStatus(UrlUtils.getResponseCode(connection), UrlUtils.getResponseMessage(connection))
                    .withHeaders(UrlUtils.getResponseHeaders(connection));

            getCookiesFromRealResponse(connection);

            // Write the response asset to a file if we're in recording mode.
            // Note that the response will reference the asset in such case,
            // while it will hold the actual byte array otherwise.
            if (isRecording) {
                File methodFile = new File(recordedAssetsDirectory, method.toLowerCase());
                File requestFile = new File(methodFile, UrlUtils.getPath(realUrl));

                // This method returns "true" if and only if there was a folder
                // actually created. The return value is hence to be seen as
                // "did create folder", rather than a "does folder exist" flag.
                // It is therefore not really relevant to check and decide upon
                // whether to continue further execution or not.
                //noinspection ResultOfMethodCallIgnored
                requestFile.mkdirs();

                String fileName = new SimpleDateFormat("yyyy-MM-dd_HH:mm:ss.SSS", Locale.getDefault()).format(new Date());
                File responseFile = new File(requestFile, fileName);
                Utils.writeFile(UrlUtils.getResponseBody(connection), responseFile);
                builder.withAsset(String.format("file://%s", responseFile.getAbsolutePath()));
            } else {
                builder.withAsset(UrlUtils.getResponseBody(connection));
            }

            return builder.build();
        } catch (IOException e) {
            Log.e("NON-FATAL", "Couldn't get real response", e);
            return null;
        } finally {
            UrlUtils.closeSilently(connection);
        }
    }

    private void addCookiesToRealRequest(HttpURLConnection connection) {
        if (connection == null)
            return;

        URL url = connection.getURL();
        String realHost = url.getHost();
        String realPath = url.getPath();
        if (realHost == null || realPath == null)
            return;

        List<HttpCookie> allCookies = cookieManager.getCookieStore().getCookies();
        List<HttpCookie> myCookies = new ArrayList<>();

        for (HttpCookie cookie : allCookies) {
            String domain = cookie.getDomain();
            String path = cookie.getPath();

            if (domain != null && domain.equals(realHost) && path != null && realPath.startsWith(path))
                myCookies.add(cookie);
        }

        if (!myCookies.isEmpty())
            connection.setRequestProperty("Cookie", TextUtils.join(";", myCookies));
    }

    private void getCookiesFromRealResponse(HttpURLConnection connection) {
        if (connection == null)
            return;

        URL url = connection.getURL();
        String realHost = url.getHost();
        if (realHost == null)
            return;

        Map<String, List<String>> headerFields = connection.getHeaderFields();
        List<String> cookiesHeader = headerFields.get("Set-Cookie");

        if (cookiesHeader != null) {
            CookieStore cookieStore = cookieManager.getCookieStore();

            for (String cookieEntry : cookiesHeader) {
                List<HttpCookie> cookies = HttpCookie.parse(cookieEntry);

                for (HttpCookie cookie : cookies) {
                    cookie.setDomain(realHost);
                    cookieStore.add(null, cookie);
                }
            }
        }
    }

}
