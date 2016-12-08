package com.echsylon.sample;

import android.app.Application;
import android.content.ComponentName;
import android.content.Intent;
import android.os.StrictMode;
import android.support.v7.app.AppCompatDelegate;
import android.widget.Toast;

public class SampleApplication extends Application {
    private static final String ATLANTIS_START_ACTION =
            "echsylon.atlantis.action.START";

    private static final ComponentName ATLANTIS_COMPONENT = new ComponentName(
            "com.echsylon.sample",
            "com.echsylon.sample.MockedNetworkService");

    private static final String ATLANTIS_EXTRA_CONFIGURATION =
            "echsylon.atlantis.extra.CONFIGURATION";

    @Override
    public void onCreate() {
        // Enable "magic" vector graphics support.
        // https://medium.com/@chrisbanes/appcompat-v23-2-age-of-the-vectors-91cbafa87c88
        AppCompatDelegate.setCompatVectorFromResourcesEnabled(true);

        enableStrictMode();
        maybeStartAtlantis();
        super.onCreate();
    }

    private void enableStrictMode() {
        StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder()
                .detectAll()
                .penaltyLog()
                .build());

        StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder()
                .detectAll()
                .penaltyLog()
                .penaltyDeath()
                .build());
    }

    private void maybeStartAtlantis() {
        Intent intent = new Intent(ATLANTIS_START_ACTION);
        intent.setComponent(ATLANTIS_COMPONENT);
        intent.putExtra(ATLANTIS_EXTRA_CONFIGURATION, "atlantis.json");

        if (startService(intent) == null)
            Toast.makeText(getApplicationContext(), R.string.atlantis_not_available, Toast.LENGTH_SHORT).show();
    }
}
