package com.echsylon.atlantis;

import android.content.Context;
import android.content.res.AssetManager;

import com.echsylon.atlantis.internal.Utils;
import com.echsylon.atlantis.template.Header;
import com.echsylon.atlantis.template.Template;
import com.google.gson.Gson;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeoutException;

import fi.iki.elonen.NanoHTTPD;

/**
 * This is the local web server that will serve the template responses. This web server will only
 * handle requests issued targeting "localhost", "192.168.0.1" or "127.0.0.1".
 */
public class Atlantis extends NanoHTTPD {
    // The singleton web server instance.
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
            instance.loadTemplate(context, templateAssetName);
        } catch (IOException e) {
            if (errorListener != null)
                errorListener.onError(e);

            return;
        }

        // Try to start the local web server if it isn't started yet.
        if (!instance.isAlive()) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        instance.start();
                        instance.pollForSignOfLife();
                    } catch (IOException e) {
                        Atlantis.shutdown();
                        if (errorListener != null)
                            errorListener.onError(e);

                        return;
                    }

                    if (instance.isAlive()) {
                        if (successListener != null)
                            successListener.onSuccess();
                    } else {
                        Atlantis.shutdown();
                        if (errorListener != null)
                            errorListener.onError(new TimeoutException());
                    }
                }
            }).start();
        }
    }

    /**
     * Stops the local web server.
     */
    public static void shutdown() {
        if (instance != null) {
            instance.stop();
            instance = null;
        }
    }

    // The mocked internet template
    private Template universe;

    // Intentionally hidden constructor.
    private Atlantis(String host, int port) {
        super(host, port);
    }

    @Override
    public Response serve(IHTTPSession session) {
        com.echsylon.atlantis.template.Response template = universe.findResponse(
                session.getUri(),
                session.getMethod().name(),
                parseSessionHeaders(session.getHeaders()));

        // Couldn't find a suitable template.
        if (template == null)
            return newFixedLengthResponse(Response.Status.NOT_FOUND, NanoHTTPD.MIME_PLAINTEXT, "Not Found");

        // Found a weird result.
        if (template.statusCode() == 0)
            return newFixedLengthResponse(Response.Status.INTERNAL_ERROR, NanoHTTPD.MIME_PLAINTEXT, "Internal Error: Unknown status code (0)");

        // Everything's just peachy.
        return newFixedLengthResponse(parseStatus(template.statusCode()), template.mimeType(), template.content());
    }

    /**
     * Polls for a {@link #isAlive()} flag with a very short interval and retries a couple of times
     * before giving up.
     */
    private void pollForSignOfLife() {
        int iteration = 0;

        while (!isAlive() && iteration++ < 10) {
            try {
                Thread.sleep(10L);
            } catch (InterruptedException e) {
                // We were prematurely interrupted, let's ignore the reason and
                // do our thing again.
            }
        }
    }

    /**
     * Initializes the local web server with the response templates described in the given asset.
     *
     * @param context           The context to read the assets from.
     * @param templateAssetName The name of the asset that describes the responses.
     */
    private void loadTemplate(Context context, String templateAssetName) throws IOException {
        InputStream inputStream = null;
        Reader reader = null;

        try {
            // Try to open the asset
            AssetManager assetManager = context.getAssets();
            inputStream = assetManager.open(templateAssetName);
            reader = new InputStreamReader(inputStream);

            // Parse the template
            universe = new Gson().fromJson(reader, Template.class);
        } finally {
            Utils.closeSilently(reader);
            Utils.closeSilently(inputStream);
        }
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
    private Response.Status parseStatus(int statusCode) {
        switch (statusCode) {
            case 101:
                return Response.Status.SWITCH_PROTOCOL;
            case 200:
                return Response.Status.OK;
            case 201:
                return Response.Status.CREATED;
            case 202:
                return Response.Status.ACCEPTED;
            case 204:
                return Response.Status.NO_CONTENT;
            case 206:
                return Response.Status.PARTIAL_CONTENT;
            case 207:
                return Response.Status.MULTI_STATUS;
            case 301:
                return Response.Status.REDIRECT;
            case 303:
                return Response.Status.REDIRECT_SEE_OTHER;
            case 304:
                return Response.Status.NOT_MODIFIED;
            case 400:
                return Response.Status.BAD_REQUEST;
            case 401:
                return Response.Status.UNAUTHORIZED;
            case 403:
                return Response.Status.FORBIDDEN;
            case 404:
                return Response.Status.NOT_FOUND;
            case 405:
                return Response.Status.METHOD_NOT_ALLOWED;
            case 406:
                return Response.Status.NOT_ACCEPTABLE;
            case 408:
                return Response.Status.REQUEST_TIMEOUT;
            case 409:
                return Response.Status.CONFLICT;
            case 416:
                return Response.Status.RANGE_NOT_SATISFIABLE;
            case 500:
                return Response.Status.INTERNAL_ERROR;
            case 501:
                return Response.Status.NOT_IMPLEMENTED;
            case 505:
                return Response.Status.UNSUPPORTED_HTTP_VERSION;
            default:
                return null;
        }
    }

}
