package com.echsylon.atlantis.extras;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceScreen;
import android.preference.SwitchPreference;

public class AtlantisSettingsFragment extends PreferenceFragment {
    private static final String KEY_ATLANTIS = "atlantis_enable";
    private static final String KEY_RECORDING = "recording_enable";

    private AtlantisService service;

    private ServiceConnection connection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName component, IBinder binder) {
            AtlantisService.Binder atlantisBinder = (AtlantisService.Binder) binder;
            service = atlantisBinder.getService();

            SwitchPreference atlantisSwitch = (SwitchPreference) findPreference(KEY_ATLANTIS);
            if (atlantisSwitch != null)
                atlantisSwitch.setChecked(service.isAtlantisEnabled());

            SwitchPreference recordingSwitch = (SwitchPreference) findPreference(KEY_RECORDING);
            if (recordingSwitch != null)
                recordingSwitch.setChecked(service.isRecordMissingRequestsEnabled());
        }

        @Override
        public void onServiceDisconnected(ComponentName component) {
            service = null;
        }
    };

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        String key = preference.getKey();
        if (key != null)
            switch (key) {
                case KEY_ATLANTIS: {
                    boolean enable = ((SwitchPreference) preference).isChecked();
                    service.setAtlantisEnabled(enable, "asset://atlantis.json");
                    break;
                }
                case KEY_RECORDING: {
                    boolean enable = ((SwitchPreference) preference).isChecked();
                    service.setRecordMissingRequestsEnabled(enable);
                    break;
                }
                default:
                    // Nothing
                    break;
            }

        return super.onPreferenceTreeClick(preferenceScreen, preference);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preferences);

        if (savedInstanceState != null) {
            SwitchPreference atlantis = (SwitchPreference) findPreference(KEY_ATLANTIS);
            if (atlantis != null)
                atlantis.setChecked(savedInstanceState.getBoolean(KEY_ATLANTIS, false));

            SwitchPreference recording = (SwitchPreference) findPreference(KEY_RECORDING);
            if (recording != null)
                recording.setChecked(savedInstanceState.getBoolean(KEY_RECORDING, false));
        }
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        Intent intent = new Intent(getActivity(), AtlantisService.class);
        context.startService(intent);
        context.bindService(intent, connection, Context.BIND_AUTO_CREATE);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        SwitchPreference atlantis = (SwitchPreference) findPreference(KEY_ATLANTIS);
        if (atlantis != null)
            outState.putBoolean(KEY_ATLANTIS, atlantis.isChecked());

        SwitchPreference recording = (SwitchPreference) findPreference(KEY_RECORDING);
        if (recording != null)
            outState.putBoolean(KEY_RECORDING, recording.isChecked());

        super.onSaveInstanceState(outState);
    }

    @Override
    public void onDetach() {
        super.onDetach();
        Context context = getActivity();
        context.unbindService(connection);
    }
}
