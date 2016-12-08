package com.echsylon.sample;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.support.annotation.Nullable;

import com.echsylon.atlantis.Atlantis;

import java.io.IOException;

import okio.BufferedSource;
import okio.Okio;

public class MockedNetworkService extends Service {
    private static final String ACTION_START = "echsylon.atlantis.action.START";
    private static final String ACTION_STOP = "echsylon.atlantis.action.STOP";
    private static final String ACTION_SET = "echsylon.atlantis.action.SET";

    private static final String EXTRA_CONFIGURATION = "echsylon.atlantis.extra.CONFIGURATION";
    private static final String EXTRA_FEATURE = "echsylon.atlantis.extra.FEATURE";
    private static final String EXTRA_STATE = "echsylon.atlantis.extra.STATE";

    private static final String FEATURE_RECORD_MISSING_REQUESTS = "RECORD_MISSING_REQUESTS";

    private Atlantis atlantis;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        switch (intent.getAction()) {
            case ACTION_START:
                return start(intent);
            case ACTION_STOP:
                return stop();
            case ACTION_SET:
                return set(intent);
            default:
                return START_NOT_STICKY;
        }
    }

    @SuppressWarnings("ConstantConditions")
    private int start(final Intent intent) {
        String configuration = intent.getStringExtra(EXTRA_CONFIGURATION);
        BufferedSource source = null;

        if (configuration == null || configuration.isEmpty())
            throw new IllegalArgumentException("Configuration asset must be provided");

        if (atlantis != null)
            throw new IllegalStateException("Atlantis is already started");

        try {
            source = Okio.buffer(Okio.source(getAssets().open(configuration)));
            String json = source.readUtf8();
            atlantis = new Atlantis(getApplicationContext(), json);
            atlantis.start();
        } catch (IOException exception) {
            exception.printStackTrace();
        } finally {
            try {
                source.close();
            } catch (NullPointerException | IOException e) {
                e.printStackTrace();
            }
        }

        return START_NOT_STICKY;
    }

    private int stop() {
        if (atlantis != null)
            atlantis.stop();

        stopSelf();
        return START_NOT_STICKY;
    }

    private int set(final Intent intent) {
        if (atlantis == null)
            throw new IllegalArgumentException("Atlantis isn't started");

        String feature = intent.getStringExtra(EXTRA_FEATURE);
        if (feature == null || feature.isEmpty())
            throw new IllegalArgumentException(EXTRA_FEATURE + " must be provided");

        if (!intent.hasExtra(EXTRA_STATE))
            throw new IllegalArgumentException(EXTRA_STATE + " must be provided");

        boolean state = intent.getBooleanExtra(EXTRA_STATE, false);
        switch (feature) {
            case FEATURE_RECORD_MISSING_REQUESTS:
                atlantis.setRecordMissingRequestsEnabled(state);
                break;
            default:
                throw new IllegalArgumentException("Unknown feature: " + feature);
        }

        return START_NOT_STICKY;
    }
}
