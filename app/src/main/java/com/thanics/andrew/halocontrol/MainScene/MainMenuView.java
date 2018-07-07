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

import android.app.ActivityManager;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.SurfaceTexture;
import android.graphics.SurfaceTexture.OnFrameAvailableListener;
import android.media.MediaPlayer;
import android.support.annotation.AnyThread;
import android.support.annotation.MainThread;
import android.support.annotation.Nullable;
import android.support.constraint.ConstraintLayout;
import android.util.AttributeSet;
import android.view.ContextThemeWrapper;
import android.view.InputDevice;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;

import com.thanics.andrew.halocontrol.R;
import com.thanics.andrew.halocontrol.rendering.CanvasQuad;

/**
 * Contains a UI that can be part of a standard 2D Android Activity or a VR Activity.
 *
 * <p>For 2D Activities, this View behaves like any other Android View. It receives events from the
 * media player, updates the UI, and forwards user input to the appropriate component. In VR
 * Activities, this View uses standard Android APIs to render its child Views to a texture that is
 * displayed in VR. It also receives events from the Daydream Controller and forwards them to its
 * child views.
 */
public class MainMenuView extends ConstraintLayout {

  private CanvasQuad canvasQuad;

  /** Creates this View using standard XML inflation. */
  public MainMenuView(Context context, AttributeSet attrs) {
    super(context, attrs);
  }

  /**
   * Creates this view for use in a VR scene.
   *
   * @param context the context used to set this View's theme
   * @param parent a parent view this view will be attached to such as the Activity's root View
   * @param quad the floating quad in the VR scene that will render this View
   */
  @MainThread
  public static MainMenuView createForOpenGl(Context context, ViewGroup parent, CanvasQuad quad) {
    // If a custom theme isn't specified, the Context's theme is used. For VR Activities, this is
    // the old Android default theme rather than a modern theme. Override this with a custom theme.
    Context theme = new ContextThemeWrapper(context, R.style.VrTheme);

    MainMenuView view = (MainMenuView) View.inflate(theme, R.layout.video_ui, null);
    view.canvasQuad = quad;
    view.setLayoutParams(CanvasQuad.getLayoutParams());
    view.setVisibility(View.VISIBLE);
    parent.addView(view, 0);

    view.findViewById(R.id.enter_exit_vr).setContentDescription(
        view.getResources().getString(R.string.exit_vr_label));

    return view;
  }

  /** Ignores 2D touch events when this View is used in a VR Activity. */
  @Override
  public boolean onInterceptTouchEvent(MotionEvent event) {
    // We are in VR mode. Synthetic events generated by MainSceneRenderer are marked as SOURCE_GAMEPAD
    // events. For this class of events, we will let the Android Touch system handle the event so we
    // return false. Other classes of events were generated by the user accidentally touching the
    // screen where this hidden view is attached.
    if (event.getSource() != InputDevice.SOURCE_GAMEPAD) {
      // Intercept and suppress touchscreen events so child buttons aren't clicked.
      return true;
    } else {
      // Don't intercept SOURCE_GAMEPAD events. onTouchEvent will handle these.
      return false;
    }
  }

  /** Handles standard Android touch events or synthetic VR events. */
  @Override
  public boolean onTouchEvent(MotionEvent event) {
    if (canvasQuad != null) {
      // In VR mode so process controller events & ignore touchscreen events.
      if (event.getSource() != InputDevice.SOURCE_GAMEPAD) {
        // Tell the system that we handled the event. This prevents children from seeing the event.
        return true;
      } else {
        // Have the system send the event to child Views and they will handle clicks.
        return super.onTouchEvent(event);
      }
    } else {
      // Not in VR mode so use standard behavior.
      return super.onTouchEvent(event);
    }
  }

  /** Installs the View's event handlers. */
  @Override
  public void onFinishInflate() {
    super.onFinishInflate();
  }

  /** Sets the OnClickListener used to switch Activities. */
  @MainThread
  public void setVrIconClickListener(OnClickListener listener) {
    ImageButton vrIcon = findViewById(R.id.enter_exit_vr);
    vrIcon.setOnClickListener(listener);
  }

  @MainThread
  public void setStereoStreamClickListener(OnClickListener listener) {
    Button stereoButton = findViewById(R.id.stereoButton);
    stereoButton.setOnClickListener(listener);
  }

  /**
   * Renders this View and its children to either Android View hierarchy's Canvas or to the VR
   * scene's CanvasQuad.
   *
   * @param androidUiCanvas used in 2D mode to render children to the screen
   */
    @Override
    public void dispatchDraw(Canvas androidUiCanvas) {
        if (canvasQuad != null) {
            Canvas glCanvas = canvasQuad.lockCanvas();
            if (glCanvas == null) {
                // This happens if Android tries to draw this View before GL initialization completes. We need
                // to retry until the draw call happens after GL invalidation.
                postInvalidate();
                return;
            }

            // Clear the canvas first.
            glCanvas.drawColor(Color.BLACK);
            // Have Android render the child views.
            super.dispatchDraw(glCanvas);
            // Commit the changes.
            canvasQuad.unlockCanvasAndPost(glCanvas);
        }
    }
}