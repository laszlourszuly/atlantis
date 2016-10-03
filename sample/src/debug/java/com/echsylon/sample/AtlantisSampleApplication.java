package com.echsylon.sample;

import android.widget.Toast;

import com.echsylon.atlantis.Atlantis;

public class AtlantisSampleApplication extends SampleApplication {

    @Override
    public void onCreate() {
        super.onCreate();
        startLocalWebServer();
    }

    private void startLocalWebServer() {
        Atlantis.start(this, "atlantis.json",
                new Atlantis.OnSuccessListener() {
                    @Override
                    public void onSuccess() {
                        Toast.makeText(AtlantisSampleApplication.this,
                                "Atlantis server is up and running",
                                Toast.LENGTH_SHORT).show();
                    }
                },
                new Atlantis.OnErrorListener() {
                    @Override
                    public void onError(final Throwable cause) {
                        Toast.makeText(AtlantisSampleApplication.this,
                                "Something went wrong, check the logs for details",
                                Toast.LENGTH_LONG).show();
                    }
                });
    }
}
