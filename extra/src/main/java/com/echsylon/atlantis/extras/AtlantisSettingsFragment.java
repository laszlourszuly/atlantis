package com.echsylon.atlantis.extras;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.PreferenceFragment;
import android.preference.SwitchPreference;

public class AtlantisSettingsFragment extends PreferenceFragment implements
        SharedPreferences.OnSharedPreferenceChangeListener {

    private Boolean isEnabled;
    private Boolean isRecording;
    private String configuration;
    private SwitchPreference enabled;
    private SwitchPreference recording;

    private AtlantisService service;
    private ServiceConnection connection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName component, IBinder binder) {
            AtlantisService.Binder atlantisBinder = (AtlantisService.Binder) binder;
            service = atlantisBinder.getService();

            if (isEnabled != null && configuration != null) {
                // The UI state has changed before the service was bound. Update
                // the now bound service state should update accordingly.
                service.setAtlantisEnabled(isEnabled, configuration);
            } else if (enabled != null) {
                // The UI state has not been initialized. Update from the bound
                // service.
                enabled.setChecked(service.isAtlantisEnabled());
            }

            // Same update pattern as for the "enabled" state.
            if (isRecording != null) {
                service.setRecordMissingRequestsEnabled(isRecording);
            } else if (recording != null) {
                recording.setChecked(service.isRecordMissingRequestsEnabled());
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName component) {
            service = null;
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preferences);
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        Intent intent = new Intent(getActivity(), AtlantisService.class);
        context.startService(intent);
        context.bindService(intent, connection, Context.BIND_AUTO_CREATE);
    }

    @Override
    public void onStart() {
        super.onStart();

        enabled = (SwitchPreference) findPreference("key_atlantis_enable");
        enabled.setOnPreferenceChangeListener((preference, value) -> {
            isEnabled = (Boolean) value;
            if (service != null)
                service.setAtlantisEnabled(isEnabled, configuration);
            return true;
        });

        recording = (SwitchPreference) findPreference("key_atlantis_record");
        recording.setOnPreferenceChangeListener((preference, value) -> {
            isRecording = (Boolean) value;
            if (service != null)
                service.setRecordMissingRequestsEnabled(isRecording);
            return true;
        });

        SharedPreferences sharedPreferences = getPreferenceManager().getSharedPreferences();
        sharedPreferences.registerOnSharedPreferenceChangeListener(this);
        configuration = sharedPreferences.getString("key_atlantis_configuration", null);
    }

    @Override
    public void onDetach() {
        super.onDetach();
        Context context = getActivity();
        context.unbindService(connection);
    }

    @Override
    public void onStop() {
        super.onStop();
        getPreferenceManager()
                .getSharedPreferences()
                .unregisterOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if ("key_atlantis_configuration".equals(key)) {
            configuration = sharedPreferences.getString(key, null);
            if (service != null) {
                service.setAtlantisEnabled(false, null);
                service.setAtlantisEnabled(true, configuration);
            }
        }
    }
}
