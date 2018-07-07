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

package com.thanics.andrew.halocontrol.MainScene;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.PointF;
import android.graphics.SurfaceTexture;
import android.graphics.SurfaceTexture.OnFrameAvailableListener;
import android.opengl.GLES20;
import android.opengl.Matrix;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
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

import com.google.vr.sdk.base.Eye;
import com.google.vr.sdk.base.GvrView;
import com.google.vr.sdk.base.HeadTransform;
import com.google.vr.sdk.base.Viewport;
import com.google.vr.sdk.controller.Orientation;
import com.thanics.andrew.halocontrol.R;
import com.thanics.andrew.halocontrol.Utils;
import com.thanics.andrew.halocontrol.rendering.CanvasQuad;
import com.thanics.andrew.halocontrol.rendering.Mesh;
import com.thanics.andrew.halocontrol.rendering.Reticle;

import java.util.concurrent.atomic.AtomicBoolean;

import javax.microedition.khronos.egl.EGLConfig;

import static com.thanics.andrew.halocontrol.Utils.checkGlError;

/**
 * Controls and renders the GL Scene.
 *
 * <p>This class is shared between MonoscopicView & VrVideoActivity. It renders the display mesh, UI
 * and controller reticle as required. It also has basic Controller input which allows the user to
 * interact with {@link MainMenuView} while in VR.
 */
public final class MainSceneRenderer implements GvrView.StereoRenderer {
    private static final String TAG = "MainSceneRenderer";
    private static final float Z_NEAR = .1f;
    private static final float Z_FAR = 100;

    // Main Menu
    private CanvasQuad canvasQuad = new CanvasQuad();
    public MainMenuView mainMenuView;
    private Handler uiHandler;

    // Sphere Background
    private Mesh sphere;
    private SurfaceTexture displayTexture;
    private Surface displaySurface;
    private int displayTextureID;
    AtomicBoolean frameAvailable = new AtomicBoolean();
    private OnFrameAvailableListener externalFrameListener;

    private Context context;

    // Controller components.
    private final Reticle reticle = new Reticle();
    @Nullable
    private Orientation controllerOrientation;
    // This is accessed on the binder & GL Threads.
    private final float[] controllerOrientationMatrix = new float[16];

    private final float[] viewProjectionMatrix = new float[16];

    public MainSceneRenderer(Context context, ViewGroup parent) {
        this.context = context;
        mainMenuView = MainMenuView.createForOpenGl(context, parent, canvasQuad);
        sphere = Mesh.createUvSphere();
        uiHandler = new Handler(Looper.getMainLooper());
        //externalFrameListener = mainMenuView.getFrameListener();
    }

  /** Updates the Reticle's position with the latest Controller pose. */
  @BinderThread
  public synchronized void setControllerOrientation(Orientation currentOrientation) {
    this.controllerOrientation = currentOrientation;
    controllerOrientation.toRotationMatrix(controllerOrientationMatrix);
  }

  /**
   * Processes Daydream Controller clicks and dispatches the event to {@link MainMenuView} as a
   * synthetic {@link MotionEvent}.
   *
   * <p>This is a minimal input system that works because CanvasQuad is a simple rectangle with a
   * hardcoded location. If the quad had a transformation matrix, then those transformations would
   * need to be used when converting from the Controller's pose to a 2D click event.
   */
  @MainThread
  public void handleClick() {
    if (mainMenuView.getAlpha() == 0) {
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
            mainMenuView.dispatchTouchEvent(down);

            // Clone the down event but change action.
            MotionEvent up = MotionEvent.obtain(down);
            up.setAction(MotionEvent.ACTION_UP);
            mainMenuView.dispatchTouchEvent(up);
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
            if (mainMenuView.getAlpha() == 0) {
              mainMenuView.animate().alpha(0.7f).start();
            } else {
              mainMenuView.animate().alpha(0).start();
            }
          }
        });
  }

    @Override
    public void onNewFrame(HeadTransform headTransform) {}

    @Override
    public void onDrawEye(Eye eye) {

        Matrix.multiplyMM(
                viewProjectionMatrix, 0, eye.getPerspective(Z_NEAR, Z_FAR), 0, eye.getEyeView(), 0);

        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);
        checkGlError();

        displayTexture.updateTexImage();
        checkGlError();
        sphere.glDraw(viewProjectionMatrix, eye.getType());

        if (mainMenuView != null) {
            canvasQuad.glDraw(viewProjectionMatrix, mainMenuView.getAlpha());
        }
        reticle.glDraw(viewProjectionMatrix, controllerOrientationMatrix);
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

        // The uiQuad uses alpha.
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA);
        GLES20.glEnable(GLES20.GL_BLEND);

        // Sphere Background
        Bitmap mediaImage = BitmapFactory.decodeResource(context.getResources(), R.raw.starmap_4k);
        displayTextureID = Utils.glCreateExternalTexture();
        displayTexture = new SurfaceTexture(displayTextureID);
        displayTexture.setDefaultBufferSize(mediaImage.getWidth(), mediaImage.getHeight());
        displaySurface = new Surface(displayTexture);
        checkGlError();

        Canvas c = displaySurface.lockCanvas(null);
        c.drawBitmap(mediaImage, 0, 0, null);
        displaySurface.unlockCanvasAndPost(c);

        sphere.glInit(displayTextureID);

        // Main Menu View
        if (canvasQuad != null) {
            canvasQuad.glInit();
        }

        // Controller Reticle
        reticle.glInit();
    }

    @Override
    public void onSurfaceChanged(int width, int height) { }

    @Override
    public void onRendererShutdown() {
        if (canvasQuad != null) {
            canvasQuad.glShutdown();
        }
        reticle.glShutdown();
        sphere.glShutdown();
    }
}
