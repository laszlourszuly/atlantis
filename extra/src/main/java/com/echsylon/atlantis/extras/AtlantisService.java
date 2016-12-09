package com.echsylon.atlantis.extras;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;

import com.echsylon.atlantis.Atlantis;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.concurrent.Callable;

/**
 * This service ensures an isolated runtime for the {@link Atlantis} mock
 * infrastructure. The client app can either bind to the service or push
 * commands to it through {@link android.content.Context#startService(Intent)
 * Context#startService(Intent)}.
 * <p>
 * The {@code startService(Intent)} approach is, due to its nature, a silent
 * send-only API. There is no way to read the states of {@code Atlantis} this
 * way. The client can, nevertheless, start {@code Atlantis} like so:
 * <pre>{@code
 *
 *     ComponentName component = new ComponentName(
 *             "com.echsylon.atlantis.extras",
 *             "com.echsylon.atlantis.AtlantisService");
 *
 *     Intent intent = new Intent("echsylon.atlantis.action.SET");
 *     intent.setComponent(component);
 *     intent.putExtra("echsylon.atlantis.extra.FEATURE", "ATLANTIS");
 *     intent.putExtra("echsylon.atlantis.extra.ENABLE", true);
 *     intent.putExtra("echsylon.atlantis.extra.DATA", "asset://config.json");
 *
 *     if (startService(intent) == null)
 *         Log.d("TAG", "Atlantis isn't available in this build config");
 *
 * }</pre>
 * A similar example on how to enable/disable recording of missing request
 * templates could look something like:
 * <pre>{@code
 *
 *     ComponentName component = new ComponentName(
 *             "com.echsylon.atlantis.extras",
 *             "com.echsylon.atlantis.AtlantisService");
 *
 *     Intent intent = new Intent("echsylon.atlantis.action.SET");
 *     intent.setComponent(component);
 *     intent.putExtra("echsylon.atlantis.extra.FEATURE", "RECORD");
 *     intent.putExtra("echsylon.atlantis.extra.ENABLE", true);
 *
 *     if (startService(intent) == null)
 *         Log.d("TAG", "Atlantis isn't available in this build config");
 *
 * }</pre>
 * To have a more interactive connection to this service the client can bind to
 * it and get a reference to the service instance through the returned binder.
 * The instance then exposes a somewhat more nuanced API.
 * <pre>{@code
 *
 *     private AtlantisService service;
 *
 *     private ServiceConnection connection = new ServiceConnection() {
 *        @literal @Override
 *         public void onServiceConnected(ComponentName c, IBinder binder) {
 *             AtlantisService.Binder bndr = (AtlantisService.Binder) binder;
 *             service = bndr.getService();
 *         }
 *
 *        @literal @Override
 *         public void onServiceDisconnected(ComponentName component) {
 *             service = null;
 *         }
 *     };
 *
 *    @literal @Override
 *     protected void onStart() {
 *         super.onStart();
 *         Intent intent = new Intent(this, AtlantisService.class);
 *         bindService(intent, connection, Context.BIND_AUTO_CREATE);
 *     }
 *
 *    @literal @Override
 *     protected void onStop() {
 *         super.onStop();
 *         unbindService(connection);
 *     }
 *
 *     public void onButtonPress(View view) {
 *         if (service.isAtlantisEnabled()) {
 *             service.setRecordMissingRequestsEnabled(true);
 *         }
 *     }
 *
 * }</pre>
 * This approach requires a bit more code, but also offers more control. It also
 * adds a hard dependency between the {@code Atlantis} service and your app.
 * <p>
 * In order to get full access to the full feature set of {@code Atlantis} the
 * client can create a local instance of {@code Atlantis} and interact directly
 * with it. In this case it's the client app's undisputed responsibility to
 * maintain a suitable life cycle environment for {@code Atlantis}:
 * <pre>{@code
 *
 *     private Atlantis atlantis;
 *
 *    @literal @Override
 *     protected void onCreate(@Nullable Bundle savedInstanceState) {
 *         super.onCreate(savedInstanceState);
 *         String json = getConfigurationJson(configuration);
 *         atlantis = new Atlantis(this, json);
 *         atlantis.start();
 *     }
 *
 *    @literal @Override
 *     protected void onDestroy() {
 *         atlantis.stop();
 *         atlantis = null;
 *         super.onDestroy();
 *     }
 *
 *     public void onStartButtonClick(View view) {
 *         atlantis.setRecordServedRequestsEnabled(true);
 *     }
 *
 *     public void onStopButtonClick(View view) {
 *         atlantis.setRecordServedRequestsEnabled(true);
 *         List<MockRequest> requests = atlantis.servedRequests();
 *
 *         // Analyze the order or whatever else.
 *     }
 *
 * }</pre>
 */
@SuppressWarnings({"WeakerAccess", "unused"})
public class AtlantisService extends Service {
    private static final String TAG = "ATLANTIS-EXTRA";
    private static final String ACTION_SET = "echsylon.atlantis.action.SET";
    private static final String EXTRA_FEATURE = "echsylon.atlantis.extra.FEATURE";
    private static final String EXTRA_STATE = "echsylon.atlantis.extra.ENABLE";
    private static final String EXTRA_DATA = "echsylon.atlantis.extra.DATA";

    private static final String FEATURE_ATLANTIS = "ATLANTIS";
    private static final String FEATURE_RECORD_MISSING_REQUESTS = "RECORD";

    /**
     * This class enables means of binding to the {@link AtlantisService} and
     * calling the public API methods directly from another Android component.
     */
    public final class Binder extends android.os.Binder {

        /**
         * Exposes the public API of the {@code AtlantisService}
         * implementation.
         *
         * @return The service instance.
         */
        public AtlantisService getService() {
            return AtlantisService.this;
        }
    }


    private Atlantis atlantis;


    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return new AtlantisService.Binder();
    }

    @Override
    public void onDestroy() {
        if (atlantis != null) {
            atlantis.stop();
            atlantis = null;
        }
        super.onDestroy();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (ACTION_SET.equals(intent.getAction())) {
            String feature = intent.getStringExtra(EXTRA_FEATURE);
            if (feature != null)
                switch (feature) {
                    case FEATURE_ATLANTIS: {
                        boolean enable = intent.getBooleanExtra(EXTRA_STATE, false);
                        String configuration = intent.getStringExtra(EXTRA_DATA);
                        setAtlantisEnabled(enable, configuration);
                        break;
                    }
                    case FEATURE_RECORD_MISSING_REQUESTS: {
                        boolean enable = intent.getBooleanExtra(EXTRA_STATE, false);
                        setRecordMissingRequestsEnabled(enable);
                        break;
                    }
                    default:
                        // Nothing
                        break;
                }
        }

        return START_NOT_STICKY;
    }

    /**
     * Sets the enabled state of the Atlantis mock infrastructure.
     *
     * @param enable        The desired enabled state of Atlantis.
     * @param configuration The Atlantis configuration description. This can
     *                      either be a JSON string, a "file://..." path or an
     *                      "asset://..." path.
     */
    public void setAtlantisEnabled(final boolean enable, final String configuration) {
        if (enable && atlantis == null) {
            String json = getConfigurationJson(configuration);
            atlantis = new Atlantis(getApplicationContext(), json);
            atlantis.start();
        } else if (!enable && atlantis != null) {
            atlantis.stop();
            atlantis = null;
            stopSelf();
        }
    }

    /**
     * Enables or disables recording of missing request templates. NOTE! The
     * {@code Atlantis} configuration must have a {@code fallbackBaseUrl} set
     * for this to have effect.
     *
     * @param enable The desired enabled state of the feature.
     */
    public void setRecordMissingRequestsEnabled(final boolean enable) {
        if (atlantis != null)
            atlantis.setRecordMissingRequestsEnabled(enable);
    }

    /**
     * Returns the enabled state of the {@code Atlantis} infrastructure.
     *
     * @return Boolean true if {@code Atlantis} is ready to intercept requests
     * and deliver mock responses for them. False otherwise.
     */
    public boolean isAtlantisEnabled() {
        return atlantis != null && atlantis.isRunning();
    }

    /**
     * Returns the enabled state for whether missing request templates are
     * recorded or not.
     *
     * @return Boolean true if missing requests are recorded, false otherwise.
     */
    public boolean isRecordMissingRequestsEnabled() {
        return atlantis != null && atlantis.isRecordingMissingRequests();
    }


    /**
     * Parses a given {@code Atlantis} configuration description and returns the
     * corresponding JSON.
     *
     * @param description The {@code Atlantis} configuration description. This
     *                    can either be a JSON string, a {@code "file://..."}
     *                    path or a {@code "asset://..."} path.
     * @return The {@code Atlantis} configuration JSON.
     */
    private String getConfigurationJson(final String description) {
        if (description == null)
            return null;

        // This is an asset reference.
        if (description.startsWith("asset://"))
            try {
                String asset = description.substring(8);
                return readStringFromInputStream(() -> getAssets().open(asset));
            } catch (Exception e) {
                Log.i(TAG, "Couldn't read configuration: " + description, e);
                return null;
            }

        // This is a file reference.
        if (description.startsWith("file://"))
            try {
                String file = description.substring(7);
                return readStringFromInputStream(() -> new FileInputStream(file));
            } catch (Exception e) {
                Log.i(TAG, "Couldn't read configuration: " + description, e);
                return null;
            }

        // Assume already JSON.
        return description;
    }

    /**
     * Reads all content from a stream and returns it as a string.
     *
     * @param inputStreamProvider The {@code InputStream} provider callable.
     * @return The string content read from the input stream.
     * @throws Exception if anything would go wrong.
     */
    @SuppressWarnings("ThrowFromFinallyBlock")
    private String readStringFromInputStream(Callable<InputStream> inputStreamProvider) throws Exception {
        InputStream inputStream = null;
        BufferedReader bufferedReader = null;
        InputStreamReader inputStreamReader = null;

        try {
            inputStream = inputStreamProvider.call();
            inputStreamReader = new InputStreamReader(inputStream);
            bufferedReader = new BufferedReader(inputStreamReader);
            StringBuilder stringBuilder = new StringBuilder();
            String line;

            while ((line = bufferedReader.readLine()) != null)
                stringBuilder.append(line).append("\n");

            return stringBuilder.toString();
        } finally {
            if (inputStream != null)
                inputStream.close();
            if (inputStreamReader != null)
                inputStreamReader.close();
            if (bufferedReader != null)
                bufferedReader.close();
        }
    }
}
