package com.thanics.andrew.halocontrol

import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

//        with(window) {
//            requestFeature(Window.FEATURE_CONTENT_TRANSITIONS)
//
//            // set an exit transition
//            exitTransition = Explode()
//        }

        // Example of a call to a native method
        //sample_text.text = stringFromJNI()
    }

    fun startFlight(view: View) {
        val intent = Intent(this, StereoFlightActivity::class.java)
        startActivity(intent)
    }

    /**
     * A native method that is implemented by the 'native-lib' native library,
     * which is packaged with this application.
     */
//    external fun stringFromJNI(): String
//
//    companion object {
//
//        // Used to load the 'native-lib' library on application startup.
//        init {
//            System.loadLibrary("native-lib")
//        }
//    }
}
