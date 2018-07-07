package com.thanics.andrew.halocontrol.MainScene;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import com.thanics.andrew.halocontrol.R;
import com.thanics.andrew.halocontrol.Stereo3DFlightScene.StereoFlightActivity;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);
    }

    @Override
    protected void onResume() {
        super.onResume();

        Log.i("Halo", "resuming");
        Button button = this.findViewById(R.id.button);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(MainActivity.this, StereoFlightActivity.class));
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

}
