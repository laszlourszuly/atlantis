package com.echsylon.sample;

import android.app.Application;
import android.os.StrictMode;
import android.support.v7.app.AppCompatDelegate;

import java.io.File;

public class SampleApplication extends Application {

    public interface ResultListener {
        void onResult(Throwable error);
    }

    public void saveConfiguration(final File file, final ResultListener listener) {
    }

    public void startRecording(File file, final ResultListener listener) {
    }

    public void stopRecording(final ResultListener listener) {
    }

    @Override
    public void onCreate() {
        // Enable "magic" vector graphics support.
        // https://medium.com/@chrisbanes/appcompat-v23-2-age-of-the-vectors-91cbafa87c88
        AppCompatDelegate.setCompatVectorFromResourcesEnabled(true);

        // Enable some strict mode flags. The underlying, third party web server
        // used by Atlantis may sometimes trigger these guards.
        StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder()
                .detectAll()
                .penaltyLog()
                .build());

        StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder()
                .detectAll()
                .penaltyLog()
                .penaltyDeath()
                .build());

        super.onCreate();
    }
}
