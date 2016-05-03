package com.echsylon.atlantis;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Looper;

import com.echsylon.atlantis.internal.Utils;
import com.echsylon.atlantis.template.Header;
import com.echsylon.atlantis.template.Template;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import fi.iki.elonen.NanoHTTPD;

/**
 * This is the local web server that will serve the template responses. This web server will only
 * handle requests issued targeting "localhost", "192.168.0.1" or "127.0.0.1".
 */
public class Atlantis {
    // The singleton instance of this class.
    private static volatile Atlantis instance;

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
     * Loads given mocked internet template and starts the local web server, if not already running.
     * Note that an attempt to load the template is made regardless if the server is already running
     * or not.
     *
     * @param context           The context to load the request templates from.
     * @param host              The host name to connect to.
     * @param port              The port to connect through.
     * @param templateAssetName The name of the request template asset to load.
     * @param successListener   The success state callback implementation.
     * @param errorListener     The error callback implementation.
     */
    public static void start(final Context context,
                             final String host, final int port,
                             final String templateAssetName,
                             final OnSuccessListener successListener,
                             final OnErrorListener errorListener) {

        // Ensure the singleton exists.
        if (instance == null)
            instance = new Atlantis(host, port);

        // Reload any request templates.
        try {
            byte[] bytes = Utils.readAsset(context, templateAssetName);
            String json = new String(bytes);
            instance.universe = new Gson().fromJson(json, Template.class);
        } catch (IOException | JsonSyntaxException e) {
            sendError(errorListener, e);
            return;
        }

        // Try to start the local web server if it isn't started yet. Note, that NanoHTTPD will
        // block the current thread for short bursts of time while waiting for an internal socket
        // to connect. In order to prevent blocking the Android main thread, we'll initialize the
        // instance from a worker thread instead.
        if (!instance.isLocalWebServerAlive()) {
            new AsyncTask<Void, Void, Throwable>() {
                @Override
                protected Throwable doInBackground(Void... lotsOfNothing) {
                    try {
                        instance.startLocalServer();
                        return null;
                    } catch (IOException e) {
                        instance.stopLocalServer();
                        instance = null;
                        return e;
                    }
                }

                @Override
                protected void onPostExecute(Throwable throwable) {
                    if (throwable == null)
                        sendSuccess(successListener);
                    else
                        sendError(errorListener, throwable);
                }
            }.execute();
        }
    }

    /**
     * Tries to send a throwable to a error callback implementation. This method ensures that the
     * callback is called from the main thread and it handles null pointers gracefully.
     *
     * @param errorListener The error callback implementation.
     * @param cause         The error.
     */
    private static void sendError(final OnErrorListener errorListener, final Throwable cause) {
        if (errorListener != null)
            new Handler(Looper.getMainLooper()).post(() -> errorListener.onError(cause));
    }

    /**
     * Tries to notify a success callback implementation on a success event. This method ensures
     * that the callback is called from the main thread and it handles null pointers gracefully.
     *
     * @param successListener The success callback implementation.
     */
    private static void sendSuccess(final OnSuccessListener successListener) {
        if (successListener != null)
            new Handler(Looper.getMainLooper()).post(successListener::onSuccess);
    }

    /**
     * Stops the local web server.
     */
    public static void shutdown() {
        if (instance != null) {
            instance.stopLocalServer();
            instance = null;
        }
    }

    private Context context;
    private Template universe;
    private NanoHTTPD nanoHTTPD;

    // Intentionally hidden constructor.
    private Atlantis(String host, int port) {
        nanoHTTPD = new NanoHTTPD(host, port) {
            @Override
            public Response serve(IHTTPSession session) {
                com.echsylon.atlantis.template.Response response = universe.findResponse(
                        session.getUri(),
                        session.getMethod().name(),
                        parseSessionHeaders(session.getHeaders()));

                // Couldn't find a suitable template.
                if (response == null)
                    return newFixedLengthResponse(Response.Status.NOT_FOUND, NanoHTTPD.MIME_PLAINTEXT, "Not Found");

                // Found a weird result.
                if (response.statusCode() == 0)
                    return newFixedLengthResponse(Response.Status.INTERNAL_ERROR, NanoHTTPD.MIME_PLAINTEXT, "Internal Error: Unknown status code (0)");

                // Everything's just peachy.
                Response.Status status = parseStatus(response.statusCode());
                if (response.hasAsset()) {
                    byte[] bytes = response.asset(context);
                    return newFixedLengthResponse(status, response.mimeType(), new ByteArrayInputStream(bytes), bytes.length);
                } else {
                    String content = response.content();
                    return newFixedLengthResponse(parseStatus(response.statusCode()), response.mimeType(), content);
                }
            }
        };
    }

    /**
     * Tries to start the local NanoHTTPD server.
     *
     * @throws IOException Thrown if the server can't be started for some reason.
     */
    private void startLocalServer() throws IOException {
        nanoHTTPD.start();
    }

    /**
     * Tries to stop the local NanoHTTPD server. No fatal exception is expected.
     */
    private void stopLocalServer() {
        nanoHTTPD.stop();
    }

    /**
     * Checks if the local NanoHTTPD server is to consider as "up-and-running".
     *
     * @return Boolean true if alive, false otherwise.
     */
    private boolean isLocalWebServerAlive() {
        return nanoHTTPD.isAlive();
    }

    /**
     * Converts an internal key-value map to a more convenient list of header objects.
     *
     * @param sessionHeaders The key-value map.
     * @return A list of headers. May be empty but never null.
     */
    private List<Header> parseSessionHeaders(Map<String, String> sessionHeaders) {
        List<Header> result = new ArrayList<>();
        Set<Map.Entry<String, String>> entries = sessionHeaders.entrySet();

        //noinspection Convert2streamapi
        for (Map.Entry<String, String> entry : entries)
            result.add(new Header(entry.getKey(), entry.getValue()));

        return result;
    }

    /**
     * Maps an integer to a Nano HTTP status code enumeration.
     *
     * @param statusCode The numeric status code.
     * @return An internal response status, or null if no match found.
     */
    private NanoHTTPD.Response.Status parseStatus(int statusCode) {
        switch (statusCode) {
            case 101:
                return NanoHTTPD.Response.Status.SWITCH_PROTOCOL;
            case 200:
                return NanoHTTPD.Response.Status.OK;
            case 201:
                return NanoHTTPD.Response.Status.CREATED;
            case 202:
                return NanoHTTPD.Response.Status.ACCEPTED;
            case 204:
                return NanoHTTPD.Response.Status.NO_CONTENT;
            case 206:
                return NanoHTTPD.Response.Status.PARTIAL_CONTENT;
            case 207:
                return NanoHTTPD.Response.Status.MULTI_STATUS;
            case 301:
                return NanoHTTPD.Response.Status.REDIRECT;
            case 303:
                return NanoHTTPD.Response.Status.REDIRECT_SEE_OTHER;
            case 304:
                return NanoHTTPD.Response.Status.NOT_MODIFIED;
            case 400:
                return NanoHTTPD.Response.Status.BAD_REQUEST;
            case 401:
                return NanoHTTPD.Response.Status.UNAUTHORIZED;
            case 403:
                return NanoHTTPD.Response.Status.FORBIDDEN;
            case 404:
                return NanoHTTPD.Response.Status.NOT_FOUND;
            case 405:
                return NanoHTTPD.Response.Status.METHOD_NOT_ALLOWED;
            case 406:
                return NanoHTTPD.Response.Status.NOT_ACCEPTABLE;
            case 408:
                return NanoHTTPD.Response.Status.REQUEST_TIMEOUT;
            case 409:
                return NanoHTTPD.Response.Status.CONFLICT;
            case 416:
                return NanoHTTPD.Response.Status.RANGE_NOT_SATISFIABLE;
            case 500:
                return NanoHTTPD.Response.Status.INTERNAL_ERROR;
            case 501:
                return NanoHTTPD.Response.Status.NOT_IMPLEMENTED;
            case 505:
                return NanoHTTPD.Response.Status.UNSUPPORTED_HTTP_VERSION;
            default:
                return null;
        }
    }

}
