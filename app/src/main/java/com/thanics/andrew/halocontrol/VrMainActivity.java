package com.thanics.andrew.halocontrol;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.Manifest.permission;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.opengl.Matrix;
import android.os.Bundle;
import android.support.annotation.MainThread;
import android.support.v4.content.ContextCompat;
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

public class VrMainActivity extends GvrActivity {

    private static final int EXIT_FROM_VR_REQUEST_CODE = 42;

    private GvrView gvrView;
    //private Renderer renderer;

    // Displays the controls for video playback.
    //private VideoUiView uiView;


    // Interfaces with the Daydream controller.
    private ControllerManager controllerManager;
    private Controller controller;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_vr_main);


    }
}
