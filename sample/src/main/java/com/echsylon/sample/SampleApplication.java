package com.echsylon.sample;

import android.app.Application;
import android.os.StrictMode;
import android.widget.Toast;

import java.io.File;

public class SampleApplication extends Application {

    public void saveConfiguration(final File file) {
        Toast.makeText(SampleApplication.this,
                "Couldn't save Atlantis configuration",
                Toast.LENGTH_SHORT).show();
    }

    public void startRecording(File file) {
    }

    public void stopRecording() {
    }

    @Override
    public void onCreate() {
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
