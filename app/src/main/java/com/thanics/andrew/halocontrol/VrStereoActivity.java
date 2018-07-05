package com.thanics.andrew.halocontrol;

import android.content.Intent;
import android.opengl.Matrix;
import android.support.annotation.MainThread;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.util.Pair;
import android.view.SurfaceHolder;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import com.google.vr.sdk.base.AndroidCompat;
import com.google.vr.sdk.base.Eye;
import com.google.vr.sdk.base.GvrActivity;
import com.google.vr.sdk.base.GvrView;
import com.google.vr.sdk.base.HeadTransform;
import com.google.vr.sdk.base.Viewport;
import com.google.vr.sdk.controller.Controller;
import com.google.vr.sdk.controller.ControllerManager;

import org.freedesktop.gstreamer.GStreamer;

import javax.microedition.khronos.egl.EGLConfig;

public class VrStereoActivity extends GvrActivity {

    // VR
    Controller controller;
    ControllerManager controllerManager;

    GvrView gvrView;
    GstVrSurfaceView gstVrSurfaceView;
    Renderer renderer;

    // Gstreamer
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

    private int streamMode; // 0 is 3D, 1 is 2D

    private static final String GST_TAG = "Gstreamer";
    private static final String TAG = "HaloVR";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_vr_stereo);

        // Initialize GStreamer and warn if it fails
        try {
            GStreamer.init(this);
        } catch (Exception e) {
            Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show();
            return;
        }

        gvrView = new GvrView(this);
        // Since the videos have fewer pixels per degree than the phones, reducing the render target
        // scaling factor reduces the work required to render the scene. This factor can be adjusted at
        // runtime depending on the resolution of the loaded video.
        // You can use Eye.getViewport() in the overridden onDrawEye() method to determine the current
        // render target size in pixels.
        gvrView.setRenderTargetScale(.8f);

        // Standard GvrView configuration
        renderer = new VrStereoActivity.Renderer(gvrView);
        gvrView.setEGLConfigChooser(
                8, 8, 8, 8,  // RGBA bits.
                16,  // Depth bits.
                0);  // Stencil bits.
        gvrView.setRenderer(renderer);
        setContentView(gvrView);

        // Most Daydream phones can render a 4k video at 60fps in sustained performance mode. These
        // options can be tweaked along with the render target scale.
        if (gvrView.setAsyncReprojectionEnabled(true)) {
            AndroidCompat.setSustainedPerformanceMode(this, true);
        }

        // Handle the user clicking on the 'X' in the top left corner. Since this is done when the user
        // has taken the headset out of VR, it should launch the app's exit flow directly rather than
        // using the transition flow.
        gvrView.setOnCloseButtonListener(new Runnable() {
            @Override
            public void run() {
                startActivity(new Intent(getIntent()).setClass(getApplicationContext(), MainActivity.class));
                finish();
            }
        });

        // Configure Controller.
        VrStereoActivity.ControllerEventListener listener = new VrStereoActivity.ControllerEventListener();
        controllerManager = new ControllerManager(this, listener);
        controller = controllerManager.getController();
        controller.setEventListener(listener);

        nativeInit();
    }

    private void onGStreamerInitialized () {
        Log.d("seq", "gst init");
//        nativePlay();
//        nativePlay2();
    }

    @Override
    protected void onDestroy() {
        nativeFinalize();
        super.onDestroy();
    }

    @Override
    protected void onResume() {
        super.onResume();


    }

    static {
        System.loadLibrary("gstreamer_android");
        System.loadLibrary("tutorial-3");
        nativeClassInit();
    }

    /**
     * Standard GVR renderer. Most of the real work is done by {@link SceneRenderer}.
     */
    private class Renderer implements GvrView.StereoRenderer {
        private static final float Z_NEAR = .1f;
        private static final float Z_FAR = 100;

        // Used by ControllerEventListener to manipulate the scene.
        public final StereoStreamRenderer scene;

        private final float[] viewProjectionMatrix = new float[16];

        /**
         * Creates the Renderer and configures the VR exit button.
         *
         * @param parent Any View that is already attached to the Window. The uiView will secretly be
         *     attached to this View in order to properly handle UI events.
         */
        @MainThread
        public Renderer(ViewGroup parent) {
            Pair<StereoStreamRenderer, GstVrSurfaceView> pair
                    = StereoStreamRenderer.createForVR(VrStereoActivity.this, parent);
            scene = pair.first;
            gstVrSurfaceView = pair.second;

            gstVrSurfaceView.surfaceView.getHolder().addCallback(new SurfaceHolder.Callback() {
                @Override
                public void surfaceCreated(SurfaceHolder surfaceHolder) {
                    Log.d(GST_TAG, "Surface created: " + surfaceHolder.getSurface());
                }

                @Override
                public void surfaceChanged(SurfaceHolder surfaceHolder, int format, int width, int height) {
                    Log.d(GST_TAG, "Surface changed to format " + format + " width "
                            + width + " height " + height);
                    nativeSurfaceInit (surfaceHolder.getSurface());
                    nativePlay();
                }

                @Override
                public void surfaceDestroyed(SurfaceHolder surfaceHolder) {
                    Log.d(GST_TAG, "Surface destroyed");
                    nativePause();
                    nativeSurfaceFinalize();
                }
            });
        }

        @Override
        public void onNewFrame(HeadTransform headTransform) {}

        @Override
        public void onDrawEye(Eye eye) {
            Matrix.multiplyMM(
                    viewProjectionMatrix, 0, eye.getPerspective(Z_NEAR, Z_FAR), 0, eye.getEyeView(), 0);
            scene.glDrawFrame(viewProjectionMatrix, eye.getType());
        }

        @Override
        public void onFinishFrame(Viewport viewport) {}

        @Override
        public void onSurfaceCreated(EGLConfig config) {
            scene.glInit();
            //mediaLoader.onGlSceneReady(scene);
        }

        @Override
        public void onSurfaceChanged(int width, int height) { }

        @Override
        public void onRendererShutdown() {
            scene.glShutdown();
        }
    }

    /** Forwards Controller events to SceneRenderer. */
    private class ControllerEventListener extends Controller.EventListener
            implements ControllerManager.EventListener {
        private boolean touchpadDown = false;
        private boolean appButtonDown = false;

        @Override
        public void onApiStatusChanged(int status) {
            Log.i(TAG, ".onApiStatusChanged " + status);
        }

        @Override
        public void onRecentered() {}

        @Override
        public void onUpdate() {
            controller.update();

            renderer.scene.setControllerOrientation(controller.orientation);

            if (!touchpadDown && controller.clickButtonState) {
                renderer.scene.handleClick();
            }

            if (!appButtonDown && controller.appButtonState) {
//                renderer.scene.toggleUi();
            }

            touchpadDown = controller.clickButtonState;
            appButtonDown = controller.appButtonState;
        }
    }
}
