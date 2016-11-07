package com.echsylon.sample;

import com.echsylon.atlantis.Atlantis;

import java.io.File;

public class AtlantisSampleApplication extends SampleApplication {
    private Atlantis atlantis = null;

    @Override
    public void saveConfiguration(final File file, final ResultListener listener) {
        //noinspection ResultOfMethodCallIgnored
        file.delete();

        atlantis.writeConfigurationToFile(file,
                () -> {
                    if (listener != null)
                        listener.onResult(null);
                }, cause -> {
                    if (listener != null)
                        listener.onResult(cause);
                });
    }

    @Override
    public void startRecording(final File assetDirectory, final ResultListener listener) {
        atlantis.startRecordingFallbackRequests(assetDirectory);
        if (listener != null)
            listener.onResult(null);
    }

    @Override
    public void stopRecording(final ResultListener listener) {
        atlantis.stopRecordingFallbackRequests();
        if (listener != null)
            listener.onResult(null);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        atlantis = Atlantis.start(this, "atlantis.json", null, null);
    }
}
