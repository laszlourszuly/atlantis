package com.echsylon.atlantis;

import android.content.Context;
import android.os.AsyncTask;

import com.echsylon.atlantis.internal.Utils;
import com.echsylon.atlantis.internal.json.JsonParser;
import com.echsylon.atlantis.template.Configuration;
import com.echsylon.atlantis.template.Request;

import java.io.ByteArrayInputStream;
import java.util.Locale;
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
     * Loads a set of template requests and corresponding responses and, if not already running also
     * starts the local web server. Note that an attempt to load the template is made regardless if
     * the server is already running or not.
     *
     * @param context         The context to load any assets from.
     * @param configAssetName The name of the request configuration asset to load.
     * @param successListener The success state callback implementation.
     * @param errorListener   The error callback implementation.
     * @return An Atlantis object instance.
     */
    public static Atlantis start(final Context context,
                                 final String configAssetName,
                                 final OnSuccessListener successListener,
                                 final OnErrorListener errorListener) {

        Atlantis atlantis = new Atlantis(context);
        Atlantis.OnSuccessListener trigger = () -> atlantis.start(successListener, errorListener);
        atlantis.setConfiguration(configAssetName, trigger, errorListener);

        return atlantis;
    }

    private Context context;
    private Configuration configuration;
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
                Request request = configuration.findRequest(
                        session.getUri(),
                        session.getMethod().name(),
                        session.getHeaders());

                if (request != null) {
                    if (isCapturing)
                        captured.push(request);

                    com.echsylon.atlantis.template.Response response = request.response();

                    // Maybe delay
                    long delay = response.delay();
                    if (delay > 0L)
                        try {
                            Thread.sleep(delay);
                        } catch (InterruptedException e) {
                            // For some reason we weren't allowed to sleep as long as we wanted.
                        }

                    // Try to serve
                    try {
                        NanoStatus status = new NanoStatus(response.statusCode(), response.statusName());
                        String mime = response.mimeType();
                        byte[] bytes = response.hasAsset() ?
                                response.asset(Atlantis.this.context) :
                                response.content().getBytes();
                        return newFixedLengthResponse(status, mime, new ByteArrayInputStream(bytes), bytes.length);

                    } catch (Exception e) {
                        return newFixedLengthResponse(Response.Status.INTERNAL_ERROR, "text/plain", "");
                    }
                }

                return super.serve(session);
            }
        };
    }

    /**
     * Stops the local web server.
     */
    public void stop() {
        nanoHTTPD.stop();
        configuration = null;
        captured.clear();
        isCapturing = false;
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
     * Returns a reference to the capture history stack. This method leaves the stack intact, but
     * new requests may be added by the local serer at any time.
     *
     * @return A reference to the captured history stack.
     */
    public Stack<Request> getCapturedRequests() {
        return captured;
    }

    /**
     * Clears any captured requests in the history stack. This method will not affect the capturing
     * state.
     */
    public void clearCapturedRequests() {
        captured.clear();
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

    // Sets the configuration asset to the Atlantis instance. The asset will be read from disk and a
    // potentially time consuming json parsing will be performed, hence a worker thread will be
    // spawned for the task.
    private void setConfiguration(final String configAssetName, final OnSuccessListener successListener, final OnErrorListener errorListener) {
        enqueueTask(() -> {
            byte[] bytes = Utils.readAsset(context, configAssetName);
            String json = new String(bytes);
            configuration = new JsonParser().fromJson(json, Configuration.class);
            return null;
        }, successListener, errorListener);
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
