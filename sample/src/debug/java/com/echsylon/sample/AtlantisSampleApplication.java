package com.echsylon.sample;

import android.widget.Toast;

import com.echsylon.atlantis.Atlantis;

public class AtlantisSampleApplication extends SampleApplication {
    private Atlantis server;

    @Override
    public void onCreate() {
        super.onCreate();
        startLocalWebServer();
    }

    private void startLocalWebServer() {
        Atlantis.OnErrorListener errorListener = new Atlantis.OnErrorListener() {
            @Override
            public void onError(final Throwable cause) {
                Toast.makeText(AtlantisSampleApplication.this,
                        "Something went wrong, check the logs for details",
                        Toast.LENGTH_LONG).show();
            }
        };

        server = Atlantis.start(null, errorListener);
        server.setConfiguration(this, "atlantis.json", null, errorListener);
    }
}
