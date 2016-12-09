package com.echsylon.sample;

import android.app.Application;
import android.os.StrictMode;
import android.support.v7.app.AppCompatDelegate;

public class SampleApplication extends Application {
    @Override
    public void onCreate() {
        // Enable "magic" vector graphics support.
        // https://medium.com/@chrisbanes/appcompat-v23-2-age-of-the-vectors-91cbafa87c88
        AppCompatDelegate.setCompatVectorFromResourcesEnabled(true);

        enableStrictMode();
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
}
