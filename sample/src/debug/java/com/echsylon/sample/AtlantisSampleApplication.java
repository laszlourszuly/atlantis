package com.echsylon.sample;

import android.widget.Toast;

import com.echsylon.atlantis.Atlantis;

import java.io.File;

public class AtlantisSampleApplication extends SampleApplication {
    private Atlantis atlantis = null;

    @Override
    public void saveConfiguration(final File file) {
        //noinspection ResultOfMethodCallIgnored
        file.delete();

        atlantis.writeConfigurationToFile(file,
                new Atlantis.OnSuccessListener() {
                    @Override
                    public void onSuccess() {
                        Toast.makeText(AtlantisSampleApplication.this,
                                String.format("Saved Atlantis Configuration to '%s'", file.getAbsolutePath()),
                                Toast.LENGTH_LONG).show();
                    }
                },
                new Atlantis.OnErrorListener() {
                    @Override
                    public void onError(Throwable cause) {
                        Toast.makeText(AtlantisSampleApplication.this,
                                "Couldn't save Atlantis configuration",
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    @Override
    public void startRecording(final File assetDirectory) {
        atlantis.startRecordingFallbackRequests(assetDirectory);
        Toast.makeText(AtlantisSampleApplication.this,
                "ENABLED: recording",
                Toast.LENGTH_SHORT).show();
    }

    @Override
    public void stopRecording() {
        atlantis.stopRecordingFallbackRequests();
        Toast.makeText(AtlantisSampleApplication.this,
                "DISABLED: recording",
                Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onCreate() {
        super.onCreate();
        startLocalWebServer();
    }

    private void startLocalWebServer() {
        atlantis = Atlantis.start(this, "atlantis.json",
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
