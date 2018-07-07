package com.thanics.andrew.halocontrol.Stereo3DFlightScene;

import android.os.Bundle;
import android.app.Activity;
import android.content.Intent;
import android.opengl.Matrix;
import android.support.annotation.MainThread;
import android.util.Log;
import android.util.Pair;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import com.google.vr.ndk.base.DaydreamApi;
import com.google.vr.sdk.base.AndroidCompat;
import com.google.vr.sdk.base.Eye;
import com.google.vr.sdk.base.GvrActivity;
import com.google.vr.sdk.base.GvrView;
import com.google.vr.sdk.base.HeadTransform;
import com.google.vr.sdk.base.Viewport;
import com.google.vr.sdk.controller.Controller;
import com.google.vr.sdk.controller.ControllerManager;
import com.thanics.andrew.halocontrol.MainScene.MainActivity;
import com.thanics.andrew.halocontrol.rendering.Mesh;

import javax.microedition.khronos.egl.EGLConfig;

public class VrStereoActivity extends GvrActivity {

    private static final String TAG = "HaloVR";

    private static final int EXIT_FROM_VR_REQUEST_CODE = 42;

    //private GvrView gvrView;
    private Stereo3DFlightSceneRenderer renderer;

    // Displays the controls for video playback.
    private Mesh sphere;

    // Interfaces with the Daydream controller.
    private ControllerManager controllerManager;
    private Controller controller;

    /**
     * Configures the VR system.
     *
     * @param savedInstanceState unused in this sample but it could be used to track video position
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.i("HaloVR", "Stereo Activity");

        GvrView gvrView = new GvrView(this);
        // Since the videos have fewer pixels per degree than the phones, reducing the render target
        // scaling factor reduces the work required to render the scene. This factor can be adjusted at
        // runtime depending on the resolution of the loaded video.
        // You can use Eye.getViewport() in the overridden onDrawEye() method to determine the current
        // render target size in pixels.
        gvrView.setRenderTargetScale(.8f);

        renderer = new Stereo3DFlightSceneRenderer(this);


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
                launch2dActivity();
            }
        });

        // Configure Controller.
        ControllerEventListener listener = new ControllerEventListener();
        controllerManager = new ControllerManager(this, listener);
        controller = controllerManager.getController();
        controller.setEventListener(listener);
    }

    /** Launches the 2D app with the same extras and data. */
    private void launch2dActivity() {
        startActivity(new Intent(this, MainActivity.class));
        finish();
    }

    /**
     * Tries to exit gracefully from VR using a VR transition dialog.
     *
     * @return whether the exit request has started or whether the request failed due to the device
     *     not being Daydream Ready
     */
    private boolean exitFromVr() {
        // This needs to use GVR's exit transition to avoid disorienting the user.
        DaydreamApi api = DaydreamApi.create(this);
        if (api != null) {
            api.exitFromVr(this, EXIT_FROM_VR_REQUEST_CODE, null);
            // Eventually, the Activity's onActivityResult will be called.
            api.close();
            return true;
        }
        return false;
    }

    /**
     * Handles the result from {@link DaydreamApi#exitFromVr(Activity, int, Intent)}. This is called
     * via the uiView.setVrIconClickListener listener below.
     *
     * @param requestCode matches the parameter to exitFromVr()
     * @param resultCode whether the user accepted the exit request or canceled
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent unused) {
        if (requestCode == EXIT_FROM_VR_REQUEST_CODE && resultCode == RESULT_OK) {
            launch2dActivity();
        } else {
            // This should contain a VR UI to handle the user declining the exit request.
            Log.e(TAG, "Declining the exit request isn't implemented in this sample.");
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        controllerManager.start();
    }

    @Override
    protected void onPause() {
        controllerManager.stop();
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    /** Forwards Controller events to MainSceneRenderer. */
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

            renderer.setControllerOrientation(controller.orientation);

            if (!touchpadDown && controller.clickButtonState) {
                renderer.handleClick();
            }

            if (!appButtonDown && controller.appButtonState) {
                renderer.toggleUi();
            }

            touchpadDown = controller.clickButtonState;
            appButtonDown = controller.appButtonState;
        }
    }
}
