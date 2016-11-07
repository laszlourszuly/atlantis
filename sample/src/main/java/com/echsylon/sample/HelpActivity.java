package com.echsylon.sample;

import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;

/**
 * Created by laszlo on 2016-11-07.
 */

public class HelpActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_help);

        Drawable iconNavigation = DrawableCompat.wrap(ContextCompat.getDrawable(this, R.drawable.ic_close_black_24px)).mutate();
        DrawableCompat.setTint(iconNavigation, ContextCompat.getColor(this, R.color.icons));

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar.setNavigationIcon(iconNavigation);
        setSupportActionBar(toolbar);
    }

}
