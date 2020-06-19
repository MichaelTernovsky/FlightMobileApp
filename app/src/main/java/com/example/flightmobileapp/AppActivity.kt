package com.example.flightmobileapp

import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.SeekBar
import android.widget.Toast
import androidx.annotation.RequiresApi
import com.google.gson.GsonBuilder
import io.github.controlwear.virtual.joystick.android.JoystickView
import kotlinx.android.synthetic.main.activity_app.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import kotlin.math.roundToInt

class AppActivity : AppCompatActivity() {
    // the data base
    private lateinit var db: UrlRepository

    // the values fields
    private var aileron = 0.0
    private var elevator = 0.0
    private var throttle = 0.0
    private var rudder = 0.0

    // the url fields
    private var urlPath = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_app)

        // initialize the db
        db = UrlRepository(application)

        // get the url that the user entered
        urlPath = db.getFirstUrl().url

        // turning on the on move function of the joystick
        setJoystickListeners()
        // turning on the on move function of the sliders
        setRudderSliderListeners()
        setThrottleSliderListeners()

        // turning on the screen shot function
        getScreenShot()

        // turning on the set commands function
        //setValuesCommand()
    }

    private fun getScreenShot() {
        val json = GsonBuilder()
            .setLenient()
            .create()
        val retrofit = Retrofit.Builder()
            .baseUrl("http://10.0.2.2:5200/")
            .addConverterFactory(GsonConverterFactory.create(json))
            .build()
        val api = retrofit.create(Api::class.java)

        // get the screen shot
        CoroutineScope(Dispatchers.IO).launch {
            while (true) {
                delay(250)
                val body = api.getImg().enqueue(object : Callback<ResponseBody> {
                    // in case of success
                    override fun onResponse(
                        call: Call<ResponseBody>, response: Response<ResponseBody>
                    ) {
                        val bytes = response?.body()?.bytes()
                        val bitmap =
                            bytes?.size?.let { BitmapFactory.decodeByteArray(bytes, 0, it) }
                        if (bitmap != null) {
                            img.setImageBitmap(bitmap)
                        }
                    }

                    // in case of failure
                    override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                        t.printStackTrace()
                        Toast.makeText(
                            applicationContext,
                            "Failed to get screen shot", Toast.LENGTH_SHORT
                        ).show()
                    }
                })
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun logOut(view: View) {
        // move to the main window
        val intent = Intent(this, MainActivity::class.java).apply { }
        startActivity(intent)
    }

    private fun changedEnough(changedVal: Double, originalVal: Double): Boolean {
        return if (originalVal == 0.0 && changedVal != 0.0)
            true
        else {
            val newVal = originalVal * 1.01
            newVal >= changedVal
        }
    }

    private fun setJoystickListeners() {
        joystick.setOnMoveListener(JoystickView.OnMoveListener() { angle: Int, strength: Int ->
            // calculate the value from the joystick
            val x = strength * kotlin.math.cos(Math.toRadians(angle * 1.0))
            val y = strength * kotlin.math.sin(Math.toRadians(angle * 1.0))

            val roundedX = x.roundToInt() / 100.0
            val roundedY = y.roundToInt() / 100.0

            // update the text views
            this.aileronText.text = roundedX.toString()
            this.elevatorText.text = roundedY.toString()

            // check if the values change in more than 1%
            if (changedEnough(roundedX, aileron) || changedEnough(roundedY, elevator)) {
                aileron = roundedX
                elevator = roundedY
            }
        })
    }

    private fun setRudderSliderListeners() {
        // turning on the on move function of the sliders
        rudderSlider.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, i: Int, b: Boolean) {
                val value = i / 10.toDouble()
                // check if the values change in more than 1%
                if (changedEnough(value, aileron)) {
                    // Display the current progress of SeekBar
                    rudder = value
                    rudderText.text = value.toString()
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {}
            override fun onStopTrackingTouch(seekBar: SeekBar) {}
        })
    }

    private fun setThrottleSliderListeners() {
        throttleSlider.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, i: Int, b: Boolean) {
                val value = i / 10.toDouble()
                if (changedEnough(value, aileron)) {
                    // Display the current progress of SeekBar
                    throttle = value
                    throttleText.text = value.toString()
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {}
            override fun onStopTrackingTouch(seekBar: SeekBar) {}
        })
    }
}