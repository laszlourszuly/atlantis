package com.echsylon.atlantis.extras;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;

public class AtlantisSettingsActivity extends AppCompatActivity {

    @Override
    @SuppressWarnings("ConstantConditions") // ignore Lint getSupportActionBar()
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        Toolbar toolbar = (Toolbar) findViewById(R.id.settings_toolbar);
        setSupportActionBar(toolbar);
        if (getIntent().getData() != null)
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        getFragmentManager().beginTransaction()
                .replace(R.id.settings_container, new AtlantisSettingsFragment())
                .commit();

    }
}
