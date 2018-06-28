package com.thanics.andrew.halocontrol;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.SurfaceView;
import android.view.SurfaceHolder;
import android.widget.Toast;
import android.content.res.TypedArray;

import org.freedesktop.gstreamer.GStreamer;

public class StereoImageView extends SurfaceView implements SurfaceHolder.Callback {
    private native void nativeInit();     // Initialize native code, build pipeline, etc
    private native void nativeFinalize(); // Destroy pipeline and shutdown native code
    private native void nativePlay();     // Set pipeline to PLAYING
    private native void nativePause();    // Set pipeline to PAUSED
    private static native boolean nativeClassInit(); // Initialize native class: cache Method IDs for callbacks
    private native void nativeSurfaceInit(Object surface);
    private native void nativeSurfaceFinalize();
    private long native_custom_data;      // Native code will use this to keep private data
    private String pipelineStr;

    //private boolean is_playing_desired;   // Whether the user asked to go to PLAYING
    private static final String GST_TAG = "Gstreamer";

    public StereoImageView(Context context) {
        super(context);

    }

    public StereoImageView(Context context, AttributeSet attrs,
                                int defStyle) {
        super(context, attrs, defStyle);
    }

    public StereoImageView(Context context, AttributeSet attrs) {
        super(context, attrs);

        Log.i("Halo", "this is constructor");

        TypedArray a = context.getTheme().obtainStyledAttributes(
                attrs,
                R.styleable.StereoImageView,
                0, 0);

        try {
            pipelineStr = a.getString(R.styleable.StereoImageView_pipeline);
            Log.i("Gstreamer", "Pipeline: " + pipelineStr);
        } finally {
            a.recycle();
        }
    }

    private void onGStreamerInitialized () {
        Log.d("Halo", "gst init");
        nativePlay();
    }

    public void surfaceCreated(SurfaceHolder holder) {
        Log.d(GST_TAG, "Surface created: " + holder.getSurface());
        Log.d("Halo", "stereo image loaded");

        System.loadLibrary("gstreamer_android");
        System.loadLibrary("tutorial-3");
        nativeClassInit();
        
        // Initialize GStreamer and warn if it fails
        try {
            GStreamer.init(this.getContext());
        } catch (Exception e) {
            Toast.makeText(this.getContext(), e.getMessage(), Toast.LENGTH_LONG).show();
            return;
        }
        nativeInit();
    }

    public void surfaceChanged(SurfaceHolder holder, int format, int width,
                               int height) {
        Log.d(GST_TAG, "Surface changed to format " + format + " width "
                + width + " height " + height);
        nativeSurfaceInit (holder.getSurface());

    }

    public void surfaceDestroyed(SurfaceHolder holder) {
        Log.d(GST_TAG, "Surface destroyed");
        nativePause();
        nativeSurfaceFinalize();
        nativeFinalize();
    }

//    static {
//        System.loadLibrary("gstreamer_android");
//        System.loadLibrary("tutorial-3");
//        nativeClassInit();
//    }
}
