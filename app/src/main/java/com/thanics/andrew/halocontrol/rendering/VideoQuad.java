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

package com.thanics.andrew.halocontrol.rendering;

import android.graphics.Canvas;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.SurfaceTexture;
import android.opengl.GLES11Ext;
import android.opengl.GLES20;
import android.view.Surface;
import android.widget.FrameLayout;

import com.google.vr.sdk.controller.Orientation;
import com.thanics.andrew.halocontrol.Utils;

import java.nio.FloatBuffer;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.thanics.andrew.halocontrol.Utils.checkGlError;

/**
 * Renders a floating, textured, translucent quad in VR at a hardcoded distance.
 *
 * <p>In this sample, the class is only used to render the Android View containing the UI. It also
 * contains the {@link Surface} and {@link SurfaceTexture} which hold the {@link Canvas} that
 * VideoUiView renders to.
 *
 * <p>A VideoQuad can be created on any thread, but {@link #glInit()} needs to be called on
 * the GL thread before it can be rendered.
 */
public class VideoQuad {
    // The size of the quad is hardcoded for this sample and the quad doesn't have a model matrix so
    // these dimensions are used by translateClick() for touch interaction.
    private static final float WIDTH = 1f;
    private static final float HEIGHT = 1f;
    private static final float DISTANCE = 1f;
    // The number of pixels in this quad affect how Android positions Views in it. VideoUiView in VR
    // will be 1024 x 128 px in size which is similar to its 2D size. For Views that only have VR
    // layouts, using a number that results in ~10-15 px / degree is good.
    public static final int PX_PER_UNIT = 900;

    // Standard vertex shader that passes through the texture data.
    private static final String[] vertexShaderCode = {
            "uniform mat4 uMvpMatrix;",
            // 3D position data.
            "attribute vec3 aPosition;",
            // 2D UV vertices.
            "attribute vec2 aTexCoords;",
            "varying vec2 vTexCoords;",

            // Standard transformation.
            "void main() {",
            "  gl_Position = uMvpMatrix * vec4(aPosition, 1);",
            "  vTexCoords = aTexCoords;",
            "}"
    };

    // Renders the texture of the quad using uAlpha for transparency.
    private static final String[] fragmentShaderCode = {
            // This is required since the texture data is GL_TEXTURE_EXTERNAL_OES.
            "#extension GL_OES_EGL_image_external : require",
            "precision mediump float;",

            // Standard texture rendering shader with extra alpha channel.
            "uniform samplerExternalOES uTexture;",
            "varying vec2 vTexCoords;",
            "void main() {",
            "  gl_FragColor = texture2D(uTexture, vTexCoords);",
            "}"
    };

    // Program-related GL items. These are only valid if program != 0.
    private int program = 0;
    private int mvpMatrixHandle;
    private int positionHandle;
    private int textureCoordsHandle;
    private int textureHandle;
    private int textureId;

    // Components used to manage the Canvas that the View is rendered to. These are only valid after
    // GL initialization. The client of this class acquires a Canvas from the Surface, writes to it
    // and posts it. This marks the Surface as dirty. The GL code then updates the SurfaceTexture
    // when rendering only if it is dirty.
    public SurfaceTexture displaySurfaceTexture;
    public Surface displaySurface;
    private final AtomicBoolean surfaceDirty = new AtomicBoolean();

    // The quad has 2 triangles built from 4 total vertices. Each vertex has 3 position & 2 texture
    // coordinates.
    private static final int POSITION_COORDS_PER_VERTEX = 3;
    private static final int TEXTURE_COORDS_PER_VERTEX = 2;
    private static final int COORDS_PER_VERTEX =
            POSITION_COORDS_PER_VERTEX + TEXTURE_COORDS_PER_VERTEX;
    private static final int BYTES_PER_COORD = 4;  // float.
    private static final int VERTEX_STRIDE_BYTES = COORDS_PER_VERTEX * BYTES_PER_COORD;

    // Interlaced position & texture data.
    private static final float[] vertexData = {
            -WIDTH / 2, -HEIGHT / 2, -DISTANCE,
            0, 1,
            WIDTH / 2, -HEIGHT / 2, -DISTANCE,
            1, 1,
            -WIDTH / 2,  HEIGHT / 2, -DISTANCE,
            0, 0,
            WIDTH / 2,  HEIGHT / 2, -DISTANCE,
            1, 0
    };
    private static final FloatBuffer vertexBuffer = Utils.createBuffer(vertexData);

    /** Only MainSceneRenderer can create a VideoQuad. */
    public VideoQuad() { }

    /** Gets LayoutParams used by Android to properly layout VideoUiView. */
    public static FrameLayout.LayoutParams getLayoutParams() {
        return new FrameLayout.LayoutParams((int) (WIDTH * PX_PER_UNIT), (int) (HEIGHT * PX_PER_UNIT));
    }

    /**
     * Calls {@link Surface#lockCanvas(Rect)}.
     *
     * @return {@link Canvas} for the View to render to or {@code null} if {@link #glInit()} has not
     *         yet been called.
     */
    public Canvas lockCanvas() {
        return displaySurface == null ? null : displaySurface.lockCanvas(null /* dirty Rect */);
    }

    /**
     * Calls {@link Surface#unlockCanvasAndPost(Canvas)} and marks the SurfaceTexture as dirty.
     *
     * @param canvas the canvas returned from {@link #lockCanvas()}
     */
    public void unlockCanvasAndPost(Canvas canvas) {
        if (canvas == null || displaySurface == null) {
            // glInit() hasn't run yet.
            return;
        }
        displaySurface.unlockCanvasAndPost(canvas);
        surfaceDirty.set(true);
    }

    /** Finishes constructing this object on the GL Thread. */
    public void glInit() {
        if (program != 0) {
            return;
        }

        // Create the program.
        program = Utils.compileProgram(vertexShaderCode, fragmentShaderCode);
        mvpMatrixHandle = GLES20.glGetUniformLocation(program, "uMvpMatrix");
        positionHandle = GLES20.glGetAttribLocation(program, "aPosition");
        textureCoordsHandle = GLES20.glGetAttribLocation(program, "aTexCoords");
        textureHandle = GLES20.glGetUniformLocation(program, "uTexture");
        textureId = Utils.glCreateExternalTexture();
        checkGlError();

        // Create the underlying SurfaceTexture with the appropriate size.
        displaySurfaceTexture = new SurfaceTexture(textureId);
        displaySurfaceTexture.setDefaultBufferSize(
                (int) (WIDTH * PX_PER_UNIT), (int) (HEIGHT * PX_PER_UNIT));
        displaySurface = new Surface(displaySurfaceTexture);
    }

    public Surface getSurface() {
        return displaySurface;
    }

    /**
     * Renders the quad.
     *
     * @param viewProjectionMatrix Array of floats containing the quad's 4x4 perspective matrix in the
     *     {@link android.opengl.Matrix} format.
     * @param alpha Specifies the opacity of this quad.
     */
    public void glDraw(float[] viewProjectionMatrix) {
        // Configure shader.
        GLES20.glUseProgram(program);
        checkGlError();

        GLES20.glEnableVertexAttribArray(positionHandle);
        GLES20.glEnableVertexAttribArray(textureCoordsHandle);
        checkGlError();

        GLES20.glUniformMatrix4fv(mvpMatrixHandle, 1, false, viewProjectionMatrix, 0);
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, textureId);
        GLES20.glUniform1i(textureHandle, 0);
        checkGlError();

        // Load position data.
        vertexBuffer.position(0);
        GLES20.glVertexAttribPointer(positionHandle, POSITION_COORDS_PER_VERTEX, GLES20.GL_FLOAT,
                false, VERTEX_STRIDE_BYTES, vertexBuffer);
        checkGlError();

        // Load texture data.
        vertexBuffer.position(POSITION_COORDS_PER_VERTEX);
        GLES20.glVertexAttribPointer(textureCoordsHandle, TEXTURE_COORDS_PER_VERTEX, GLES20.GL_FLOAT,
                false, VERTEX_STRIDE_BYTES, vertexBuffer);
        checkGlError();

        if (surfaceDirty.compareAndSet(true, false)) {
            // If the Surface has been written to, get the new data onto the SurfaceTexture.
            displaySurfaceTexture.updateTexImage();
        }

        // Render.
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, vertexData.length / COORDS_PER_VERTEX);
        checkGlError();

        GLES20.glDisableVertexAttribArray(positionHandle);
        GLES20.glDisableVertexAttribArray(textureCoordsHandle);
    }

    /** Frees GL resources. */
    public void glShutdown() {
        if (program != 0) {
            GLES20.glDeleteProgram(program);
            GLES20.glDeleteTextures(1, new int[]{textureId}, 0);
        }

        if (displaySurfaceTexture != null) {
            displaySurfaceTexture.release();
        }
    }
}
