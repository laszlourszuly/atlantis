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
            return name;
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

    // The singleton instance.
    private static Atlantis instance;

    /**
     * Loads a set of template requests and corresponding responses and, if not already running also
     * starts the local web server. Note that an attempt to load the template is made regardless if
     * the server is already running or not.
     *
     * @param context           The context to load any assets from.
     * @param templateAssetName The name of the request template asset to load.
     * @param successListener   The success state callback implementation.
     * @param errorListener     The error callback implementation.
     */
    public static void start(final Context context,
                             final String templateAssetName,
                             final OnSuccessListener successListener,
                             final OnErrorListener errorListener) {

        if (instance == null)
            instance = new Atlantis();

        Template template;
        try {
            byte[] bytes = Utils.readAsset(context, templateAssetName);
            String json = new String(bytes);
            template = new Gson().fromJson(json, Template.class);
        } catch (IOException | JsonSyntaxException e) {
            sendError(errorListener, e);
            return;
        }

        instance.setContext(context);
        instance.setTemplate(template);
        instance.start(successListener, errorListener);
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

    // Tries to send a throwable to a error callback implementation. This method ensures that the
    // callback is called from the main thread and it handles null pointers gracefully.
    private static void sendError(final OnErrorListener errorListener, final Throwable cause) {
        if (errorListener != null)
            new Handler(Looper.getMainLooper()).post(() -> errorListener.onError(cause));
    }

    // Tries to notify a success callback implementation on a success event. This method ensures
    // that the callback is called from the main thread and it handles null pointers gracefully.
    private static void sendSuccess(final OnSuccessListener successListener) {
        if (successListener != null)
            new Handler(Looper.getMainLooper()).post(successListener::onSuccess);
    }

    private Context context;
    private Template template;
    private NanoHTTPD nanoHTTPD;

    // Intentionally hidden constructor.
    private Atlantis() {
        this.nanoHTTPD = new NanoHTTPD(HOSTNAME, PORT) {
            @Override
            public Response serve(IHTTPSession session) {
                Template template = getTemplate();
                com.echsylon.atlantis.template.Response response = template.findResponse(
                        session.getUri(),
                        session.getMethod().name(),
                        parseSessionHeaders(session.getHeaders()));

                if (response == null) {
                    return super.serve(session);
                } else if (response.hasAsset()) {
                    NanoStatus status = new NanoStatus(response.statusCode(), response.statusName());
                    Context context = getContext();
                    byte[] bytes = response.asset(context);
                    return newFixedLengthResponse(status, response.mimeType(), new ByteArrayInputStream(bytes), bytes.length);
                } else {
                    NanoStatus status = new NanoStatus(response.statusCode(), response.statusName());
                    String content = response.content();
                    return newFixedLengthResponse(status, response.mimeType(), content);
                }
            }
        };
    }

    // Tries to start the local web server if it isn't started yet. Note, that NanoHTTPD will  block
    // the current thread for short bursts of time while waiting for an internal socket to connect.
    // In order to prevent blocking the Android main thread, we'll start NanoHTTTPD from a worker
    // thread instead.
    private void start(final OnSuccessListener successListener, final OnErrorListener errorListener) {
        if (!nanoHTTPD.isAlive()) {
            new AsyncTask<Void, Void, Throwable>() {
                @Override
                protected Throwable doInBackground(Void... lotsOfNothing) {
                    try {
                        nanoHTTPD.start();
                        return null;
                    } catch (IOException e) {
                        shutdown();
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

    private void stop() {
        nanoHTTPD.stop();
    }

    private void setTemplate(Template template) {
        this.template = template;
    }

    private Template getTemplate() {
        return template;
    }

    private void setContext(Context context) {
        this.context = context;
    }

    private Context getContext() {
        return context;
    }

    // Converts a map of string pairs to a list of header objects, as Atlantis internally expects
    // them to be.
    private List<Header> parseSessionHeaders(Map<String, String> sessionHeaders) {
        List<Header> result = new ArrayList<>();
        Set<Map.Entry<String, String>> entries = sessionHeaders.entrySet();

        //noinspection Convert2streamapi
        for (Map.Entry<String, String> entry : entries)
            result.add(new Header(entry.getKey(), entry.getValue()));

        return result;
    }

}
