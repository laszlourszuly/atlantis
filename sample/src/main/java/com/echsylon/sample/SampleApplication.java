package com.echsylon.sample;

import android.app.Application;
import android.os.StrictMode;
import android.widget.Toast;

public class SampleApplication extends Application {

    public void saveConfiguration() {
        Toast.makeText(SampleApplication.this,
                "Couldn't save Atlantis configuration",
                Toast.LENGTH_SHORT).show();

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
