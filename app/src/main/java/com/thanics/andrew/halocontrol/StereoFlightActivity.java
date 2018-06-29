package com.thanics.andrew.halocontrol;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Toast;

import org.freedesktop.gstreamer.GStreamer;

public class StereoFlightActivity extends AppCompatActivity {

    private static native boolean nativeClassInit(); // Initialize native class: cache Method IDs for callbacks
    private native void nativeInit();     // Initialize native code, build pipeline, etc
    private native void nativeFinalize(); // Destroy pipeline and shutdown native code

    private native void nativePlay();     // Set pipeline to PLAYING
    private native void nativePause();    // Set pipeline to PAUSED
    private native void nativeSurfaceInit(Object surface);
    private native void nativeSurfaceFinalize();

    private native void nativePlay2();     // Set pipeline to PLAYING
    private native void nativePause2();    // Set pipeline to PAUSED
    private native void nativeSurfaceInit2(Object surface);
    private native void nativeSurfaceFinalize2();

    private long native_custom_data;      // Native code will use this to keep private data
    private long native_custom_data2;      // Native code will use this to keep private data

    private static final String GST_TAG = "Gstreamer";

    // Called when the activity is first created.
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        Log.i("seq", "oncreate");

        // Initialize GStreamer and warn if it fails
        try {
            GStreamer.init(this);
        } catch (Exception e) {
            Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show();
            return;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.i("seq", "onresume");

        View decorView = getWindow().getDecorView();

        // Hide both the navigation bar and the status bar.
        int uiOptions = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_IMMERSIVE;
        decorView.setSystemUiVisibility(uiOptions);

        setContentView(R.layout.activity_stereo_flight);

        SurfaceView siv = this.findViewById(R.id.stereoImageView);
        siv.getHolder().addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(SurfaceHolder surfaceHolder) {
                Log.d(GST_TAG, "Surface created: " + surfaceHolder.getSurface());
            }

            @Override
            public void surfaceChanged(SurfaceHolder surfaceHolder, int format, int width, int height) {
                Log.d(GST_TAG, "Surface changed to format " + format + " width "
                        + width + " height " + height);
                nativeSurfaceInit (surfaceHolder.getSurface());
            }

            @Override
            public void surfaceDestroyed(SurfaceHolder surfaceHolder) {
                Log.d(GST_TAG, "Surface destroyed");
                nativePause();
                nativeSurfaceFinalize();
            }
        });

        SurfaceView sv2 = this.findViewById(R.id.stereoImageView2);
        sv2.getHolder().addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(SurfaceHolder surfaceHolder) {
                Log.d(GST_TAG, "Surface created: " + surfaceHolder.getSurface());
            }

            @Override
            public void surfaceChanged(SurfaceHolder surfaceHolder, int format, int width, int height) {
                Log.d(GST_TAG, "Surface changed to format " + format + " width "
                        + width + " height " + height);
                nativeSurfaceInit2 (surfaceHolder.getSurface());
            }

            @Override
            public void surfaceDestroyed(SurfaceHolder surfaceHolder) {
                Log.d(GST_TAG, "Surface destroyed");
                nativePause2();
                nativeSurfaceFinalize2();
            }
        });

        nativeInit();
    }

    private void onGStreamerInitialized () {
        Log.d("seq", "gst init");
        nativePlay();
        nativePlay2();
    }

    protected void onSaveInstanceState (Bundle outState) {
        super.onSaveInstanceState(outState);

    }

    protected void onDestroy() {
        nativeFinalize();
        super.onDestroy();
    }

    static {
        System.loadLibrary("gstreamer_android");
        System.loadLibrary("tutorial-3");
        nativeClassInit();
    }

}
