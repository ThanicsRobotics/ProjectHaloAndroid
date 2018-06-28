package com.thanics.andrew.halocontrol;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

public class StereoFlightActivity extends AppCompatActivity {

    // Called when the activity is first created.
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

    }

    @Override
    protected void onResume() {
        super.onResume();

        View decorView = getWindow().getDecorView();

        // Hide both the navigation bar and the status bar.
        int uiOptions = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_IMMERSIVE;
        decorView.setSystemUiVisibility(uiOptions);

        setContentView(R.layout.activity_stereo_flight);

        StereoImageView siv = this.findViewById(R.id.stereoImageView);
        siv.getHolder().addCallback(siv);

        StereoImageView siv2 = this.findViewById(R.id.stereoImageView2);
        siv2.getHolder().addCallback(siv2);
    }

    protected void onSaveInstanceState (Bundle outState) {
        super.onSaveInstanceState(outState);

    }

    protected void onDestroy() {
        super.onDestroy();

    }

}
