package com.echsylon.atlantis;

import android.content.Context;
import android.os.AsyncTask;

import com.echsylon.atlantis.internal.Utils;
import com.echsylon.atlantis.template.Header;
import com.echsylon.atlantis.template.Request;
import com.echsylon.atlantis.template.Template;
import com.google.gson.Gson;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.concurrent.Callable;

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

    /**
     * Loads a set of template requests and corresponding responses and, if not already running also
     * starts the local web server. Note that an attempt to load the template is made regardless if
     * the server is already running or not.
     *
     * @param context           The context to load any assets from.
     * @param templateAssetName The name of the request template asset to load.
     * @param successListener   The success state callback implementation.
     * @param errorListener     The error callback implementation.
     * @return An Atlantis object instance.
     */
    public static Atlantis start(final Context context,
                                 final String templateAssetName,
                                 final OnSuccessListener successListener,
                                 final OnErrorListener errorListener) {

        Atlantis atlantis = new Atlantis(context);
        Atlantis.OnSuccessListener trigger = () -> atlantis.start(successListener, errorListener);
        atlantis.setTemplate(templateAssetName, trigger, errorListener);

        return atlantis;
    }

    private Context context;
    private Template template;
    private NanoHTTPD nanoHTTPD;
    private Stack<Request> captured;

    private boolean isCapturing;

    // Intentionally hidden constructor.
    private Atlantis(Context context) {
        this.context = context;
        this.captured = new Stack<>();
        this.isCapturing = false;

        this.nanoHTTPD = new NanoHTTPD(HOSTNAME, PORT) {
            @Override
            public Response serve(IHTTPSession session) {
                // Let it crash on null pointer as it's considered an unrecoverable error state.
                Request request = template.findRequest(
                        session.getUri(),
                        session.getMethod().name(),
                        parseSessionHeaders(session.getHeaders()));

                if (request != null) {
                    if (isCapturing)
                        captured.push(request);

                    com.echsylon.atlantis.template.Response response = request.response();
                    if (response.hasAsset()) {
                        NanoStatus status = new NanoStatus(response.statusCode(), response.statusName());
                        byte[] bytes = response.asset(Atlantis.this.context);
                        return newFixedLengthResponse(status, response.mimeType(), new ByteArrayInputStream(bytes), bytes.length);
                    } else {
                        NanoStatus status = new NanoStatus(response.statusCode(), response.statusName());
                        String content = response.content();
                        return newFixedLengthResponse(status, response.mimeType(), content);
                    }
                }

                return super.serve(session);
            }
        };
    }

    /**
     * Sets a new template asset to the Atlantis instance. This will step into action for any future
     * requests.
     *
     * @param templateAssetName The name of the asset to read the requests template from.
     * @param successListener   The callback to deliver a success notification on.
     * @param errorListener     The callback to deliver any error states on.
     * @return A reference to this instance of the Atlantis object. This allows for method chaining.
     */
    public Atlantis setTemplate(final String templateAssetName, final OnSuccessListener successListener, final OnErrorListener errorListener) {
        // The asset will be read from disk and a potentially time consuming json parsing will be
        // performed, hence we'll spawn a worker thread for the task.
        enqueueTask(() -> {
            byte[] bytes = Utils.readAsset(context, templateAssetName);
            String json = new String(bytes);
            template = new Gson().fromJson(json, Template.class);
            return null;
        }, successListener, errorListener);

        return this;
    }

    /**
     * Stops the local web server.
     */
    public void stop() {
        nanoHTTPD.stop();
        template = null;
        captured.clear();
        isCapturing = false;
    }

    /**
     * Tells the local web server to start capturing a copy of any served requests. This method will
     * start a new capture session by clearing the capture history stack.
     */
    public void startCapturing() {
        captured.clear();
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
     * Returns a reference to the capture history stack. This method leaves the stack intact, but
     * new requests may be added by the local serer at any time.
     *
     * @return A reference to the captured history stack.
     */
    public Stack<Request> getCapturedRequests() {
        return captured;
    }

    // Tries to start the local web server if it isn't started yet. Worth mentioning is that
    // NanoHTTPD will block the current thread for short bursts of time while waiting for an
    // internal socket to connect. In order to prevent blocking the Android main thread, we'll start
    // NanoHTTTPD from a worker thread instead.
    private Atlantis start(final OnSuccessListener successListener, final OnErrorListener errorListener) {
        if (!nanoHTTPD.isAlive()) {
            enqueueTask(() -> {
                nanoHTTPD.start();
                return null;
            }, successListener, errorListener);
        }

        return this;
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

    // Creates a new AsyncTask instance and executes a callable from it. AsyncTasks will by default
    // be queued up in a serial executor, which ensures they are operated on in the very same order
    // they were once enqueued.
    private void enqueueTask(final Callable<Void> callable, final OnSuccessListener successListener, final OnErrorListener errorListener) {
        new AsyncTask<Void, Void, Throwable>() {
            @Override
            protected Throwable doInBackground(Void... params) {
                try {
                    callable.call();
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

}
