/*
 * Copyright 2017 Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.thanics.andrew.halocontrol.Stereo3DFlightScene;

import android.content.Context;
import android.graphics.PointF;
import android.graphics.SurfaceTexture;
import android.graphics.SurfaceTexture.OnFrameAvailableListener;
import android.opengl.GLES20;
import android.opengl.Matrix;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.provider.MediaStore;
import android.support.annotation.AnyThread;
import android.support.annotation.BinderThread;
import android.support.annotation.MainThread;
import android.support.annotation.Nullable;
import android.util.Log;
import android.util.Pair;
import android.view.InputDevice;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.ViewGroup;
import android.widget.Toast;

import com.google.vr.sdk.base.Eye;
import com.google.vr.sdk.base.GvrView;
import com.google.vr.sdk.base.HeadTransform;
import com.google.vr.sdk.base.Viewport;
import com.google.vr.sdk.controller.Orientation;
import com.thanics.andrew.halocontrol.Utils;
import com.thanics.andrew.halocontrol.rendering.CanvasQuad;
import com.thanics.andrew.halocontrol.rendering.Reticle;
import com.thanics.andrew.halocontrol.rendering.VideoQuad;

import org.freedesktop.gstreamer.GStreamer;

import java.util.concurrent.atomic.AtomicBoolean;

import javax.microedition.khronos.egl.EGLConfig;

import static com.thanics.andrew.halocontrol.Utils.checkGlError;

/**
 * Controls and renders the GL Scene.
 *
 * <p>This class is shared between MonoscopicView & VrVideoActivity. It renders the display mesh, UI
 * and controller reticle as required. It also has basic Controller input which allows the user to
 * interact with {@link SideMenuView} while in VR.
 */
public final class Stereo3DFlightSceneRenderer implements GvrView.StereoRenderer {

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

    // Scene
    private static final String TAG = "MainSceneRenderer";
    private static final float Z_NEAR = .1f;
    private static final float Z_FAR = 100;

    // UI
    private CanvasQuad canvasQuad = new CanvasQuad();
    private Handler uiHandler;
    private SideMenuView sideMenuView;

    // Video surface
    private VideoQuad videoQuad = new VideoQuad();
    private int displayTextureId;
    private SurfaceTexture displayTexture;
    private Surface displaySurface;
    AtomicBoolean frameAvailable = new AtomicBoolean();
    //OnFrameAvailableListener externalFrameListener;

    // Controller components.
    private final Reticle reticle = new Reticle();
    @Nullable
    private Orientation controllerOrientation;
    // This is accessed on the binder & GL Threads.
    private final float[] controllerOrientationMatrix = new float[16];

    private final float[] viewProjectionMatrix = new float[16];

    public Stereo3DFlightSceneRenderer(Context context) {
        Log.i("HaloVR", "Stereo3DRenderer");
        frameAvailable.set(true);

        uiHandler = new Handler(Looper.getMainLooper());

        // Initialize GStreamer and warn if it fails
        try {
            GStreamer.init(context);
        } catch (Exception e) {
            Toast.makeText(context, e.getMessage(), Toast.LENGTH_LONG).show();
            return;
        }

        Log.i("HaloVR", "end of Stereo3DRenderer");
    }

    static {
        System.loadLibrary("gstreamer_android");
        System.loadLibrary("tutorial-3");
        nativeClassInit();
    }

    /** Updates the Reticle's position with the latest Controller pose. */
    @BinderThread
    public synchronized void setControllerOrientation(Orientation currentOrientation) {
        this.controllerOrientation = currentOrientation;
        controllerOrientation.toRotationMatrix(controllerOrientationMatrix);
    }

    /**
     * Processes Daydream Controller clicks and dispatches the event to {@link SideMenuView} as a
     * synthetic {@link MotionEvent}.
     *
     * <p>This is a minimal input system that works because CanvasQuad is a simple rectangle with a
     * hardcoded location. If the quad had a transformation matrix, then those transformations would
     * need to be used when converting from the Controller's pose to a 2D click event.
     */
    @MainThread
    public void handleClick() {
        if (sideMenuView.getAlpha() == 0) {
            // When the UI is hidden, clicking anywhere will make it visible.
            toggleUi();
            return;
        }

        if (controllerOrientation == null) {
            // Race condition between click & pose events.
            return;
        }

        final PointF clickTarget = CanvasQuad.translateClick(controllerOrientation);
        if (clickTarget == null) {
            // When the click is outside of the View, hide the UI.
            toggleUi();
            return;
        }

        // The actual processing of the synthetic event needs to happen in the UI thread.
        uiHandler.post(
                new Runnable() {
                    @Override
                    public void run() {
                        // Generate a pair of down/up events to make the Android View processing handle the
                        // click.
                        long now = SystemClock.uptimeMillis();
                        MotionEvent down = MotionEvent.obtain(
                                now, now,  // Timestamps.
                                MotionEvent.ACTION_DOWN, clickTarget.x, clickTarget.y,  // The important parts.
                                1, 1, 0, 1, 1, 0, 0);  // Unused config data.
                        down.setSource(InputDevice.SOURCE_GAMEPAD);
                        sideMenuView.dispatchTouchEvent(down);

                        // Clone the down event but change action.
                        MotionEvent up = MotionEvent.obtain(down);
                        up.setAction(MotionEvent.ACTION_UP);
                        sideMenuView.dispatchTouchEvent(up);
                    }
                });
    }

    /** Uses Android's animation system to fade in/out when the user wants to show/hide the UI. */
    @AnyThread
    public void toggleUi() {
        // This can be trigged via a controller action so switch to main thread to manipulate the View.
        uiHandler.post(
                new Runnable() {
                    @Override
                    public void run() {
                        if (sideMenuView.getAlpha() == 0) {
                            sideMenuView.animate().alpha(0.7f).start();
                        } else {
                            sideMenuView.animate().alpha(0).start();
                        }
                    }
                });
    }

    private void onGStreamerInitialized () {
        Log.d("seq", "gst init");
//        nativePlay();
//        nativePlay2();
    }

    @Override
    public void onNewFrame(HeadTransform headTransform) {}

    @Override
    public void onDrawEye(Eye eye) {

        Matrix.multiplyMM(
                viewProjectionMatrix, 0, eye.getPerspective(Z_NEAR, Z_FAR), 0, eye.getEyeView(), 0);
        Log.i("HaloVR", "Right before GLES call");
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);
        Log.i("HaloVR", "Right after GLES call");
        checkGlError();

        // The uiQuad uses alpha.
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA);
        GLES20.glEnable(GLES20.GL_BLEND);

//        if (sideMenuView != null) {
//            canvasQuad.glDraw(viewProjectionMatrix, sideMenuView.getAlpha());
//        }

        reticle.glDraw(viewProjectionMatrix, controllerOrientationMatrix);

        if (frameAvailable.compareAndSet(true, false)) {
            displayTexture.updateTexImage();
            checkGlError();
        }
        videoQuad.glDraw(viewProjectionMatrix);
    }

    @Override
    public void onFinishFrame(Viewport viewport) {}

    @Override
    public void onSurfaceCreated(EGLConfig config) {
        checkGlError();
        Matrix.setIdentityM(controllerOrientationMatrix, 0);

        // Set the background frame color. This is only visible if the display mesh isn't a full sphere.
        GLES20.glClearColor(0.5f, 0.5f, 0.5f, 1.0f);
        checkGlError();

        videoQuad.glInit();

        // When the video decodes a new frame, tell the GL thread to update the image.
        videoQuad.displaySurfaceTexture.setOnFrameAvailableListener(
                new OnFrameAvailableListener() {
                    @Override
                    public void onFrameAvailable(SurfaceTexture surfaceTexture) {
                        frameAvailable.set(true);
                    }
                });

//        if (canvasQuad != null) {
//            canvasQuad.glInit();
//        }
        reticle.glInit();

        nativeSurfaceInit(videoQuad.displaySurface);
        nativeInit();
        nativePlay();
        Log.i("HaloVR", "End of surfaceCreated");
    }

    @Override
    public void onSurfaceChanged(int width, int height) { }

    @Override
    public void onRendererShutdown() {
        if (canvasQuad != null) {
            canvasQuad.glShutdown();
        }
        reticle.glShutdown();
        videoQuad.glShutdown();

        nativePause();
        nativeSurfaceFinalize();
        nativeFinalize();
    }
}
