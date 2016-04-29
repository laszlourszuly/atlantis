package com.echsylon.sample;

import android.app.Application;
import android.net.Uri;
import android.widget.Toast;

import com.echsylon.atlantis.Atlantis;

public class AtlantisSampleApplication extends SampleApplication {

    @Override
    public void onCreate() {
        super.onCreate();
        startLocalWebServer();
    }

    private void startLocalWebServer() {
        Uri uri = Uri.parse(BuildConfig.BASE_URL);
        String host = uri.getHost();

        Atlantis.start(this, host, BuildConfig.PORT, "atlantis.json",
                new Atlantis.OnSuccessListener() {
                    @Override
                    public void onSuccess() {
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
