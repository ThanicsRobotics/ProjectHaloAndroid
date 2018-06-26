package com.thanics.andrew.halocontrol;

import android.app.Activity;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.view.Window;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import org.freedesktop.gstreamer.GStreamer;

public class StereoFlightActivity extends AppCompatActivity {
//    private native void nativeInit();     // Initialize native code, build pipeline, etc
//    private native void nativeFinalize(); // Destroy pipeline and shutdown native code
//    private native void nativePlay();     // Set pipeline to PLAYING
//    private native void nativePause();    // Set pipeline to PAUSED
//    private static native boolean nativeClassInit(); // Initialize native class: cache Method IDs for callbacks
//    private native void nativeSurfaceInit(Object surface);
//    private native void nativeSurfaceFinalize();
//    private long native_custom_data;      // Native code will use this to keep private data
//
//    private boolean is_playing_desired;   // Whether the user asked to go to PLAYING
//    private static final String GST_TAG = "Gstreamer";

    // Called when the activity is first created.
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);

        // Initialize GStreamer and warn if it fails
//        try {
//            GStreamer.init(this);
//            Log.i("Gstreamer", "Gstreamer initialized");
//        } catch (Exception e) {
//            Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show();
//            finish();
//            return;
//        }

        setContentView(R.layout.activity_stereo_flight);
        StereoImageView siv = (StereoImageView) this.findViewById(R.id.stereoImageView);
        siv.getHolder().addCallback(siv);

        StereoImageView siv2 = (StereoImageView) this.findViewById(R.id.stereoImageView2);
        siv2.getHolder().addCallback(siv2);

        //SurfaceView sv = (SurfaceView) this.findViewById(R.id.stereoImageView);
        //sv = new StereoImageView(this);
//        SurfaceHolder sh = sv.getHolder();
//        sh.addCallback(this);
//
//        SurfaceView sv2 = (SurfaceView) this.findViewById(R.id.surfaceViewStereo2);
//        SurfaceHolder sh2 = sv2.getHolder();
//        sh2.addCallback(this);

//        if (savedInstanceState != null) {
//            is_playing_desired = savedInstanceState.getBoolean("playing");
//            Log.i (GST_TAG, "Activity created. Saved state is playing:" + is_playing_desired);
//        } else {
//            is_playing_desired = false;
//            Log.i (GST_TAG, "Activity created. There is no saved state, playing: false");
//        }
//
//        // Start with disabled buttons, until native code is initialized
////        this.findViewById(R.id.button_play).setEnabled(false);
////        this.findViewById(R.id.button_stop).setEnabled(false);
//
//        nativeInit();
//        is_playing_desired = true;
//        nativePlay();
    }

    @Override
    protected void onResume() {
        super.onResume();

    }

    protected void onSaveInstanceState (Bundle outState) {
        super.onSaveInstanceState(outState);
//        Log.d (GST_TAG, "Saving state, playing:" + is_playing_desired);
//        outState.putBoolean("playing", is_playing_desired);
    }

    protected void onDestroy() {
        //nativeFinalize();
        super.onDestroy();
    }

    // Called from native code. This sets the content of the TextView from the UI thread.
//    private void setMessage(final String message) {
//        final TextView tv = (TextView) this.findViewById(R.id.textview_message);
//        runOnUiThread (new Runnable() {
//            public void run() {
//                tv.setText(message);
//            }
//        });
//    }

    // Called from native code. Native code calls this once it has created its pipeline and
    // the main loop is running, so it is ready to accept commands.
//    private void onGStreamerInitialized () {
//        Log.i (GST_TAG, "Gst initialized. Restoring state, playing:" + is_playing_desired);
//        // Restore previous playing state
//        if (is_playing_desired) {
//            nativePlay();
//        } else {
//            nativePause();
//        }
//
//        // Re-enable buttons, now that GStreamer is initialized
//        final Activity activity = this;
//        runOnUiThread(new Runnable() {
//            public void run() {
////                activity.findViewById(R.id.button_play).setEnabled(true);
////                activity.findViewById(R.id.button_stop).setEnabled(true);
//            }
//        });
//    }

//    static {
//        System.loadLibrary("gstreamer_android");
//        System.loadLibrary("tutorial-3");
//        nativeClassInit();
//    }

//    public void surfaceChanged(SurfaceHolder holder, int format, int width,
//                               int height) {
//        Log.d(GST_TAG, "Surface changed to format " + format + " width "
//                + width + " height " + height);
//        nativeSurfaceInit (holder.getSurface());
//    }
//
//    public void surfaceCreated(SurfaceHolder holder) {
//        Log.d(GST_TAG, "Surface created: " + holder.getSurface());
//    }
//
//    public void surfaceDestroyed(SurfaceHolder holder) {
//        Log.d(GST_TAG, "Surface destroyed");
//        nativeSurfaceFinalize ();
//    }

}
