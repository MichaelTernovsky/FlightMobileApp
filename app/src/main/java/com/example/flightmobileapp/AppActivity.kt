package com.example.flightmobileapp

import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.View
import android.widget.SeekBar
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import com.google.gson.GsonBuilder
import io.github.controlwear.virtual.joystick.android.JoystickView
import kotlinx.android.synthetic.main.activity_app.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import okhttp3.MediaType
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.IOException
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

    // assisting variable for the errors messages
    private var messageShouldStop = false

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

        // reset the assisting variable
        messageShouldStop = false
    }

    override fun onStart() {
        super.onStart()

        // reset the assisting variable
        messageShouldStop = false
    }

    override fun onStop() {
        super.onStop()
        messageShouldStop = true
    }

    private fun getScreenShot() {
        val json = GsonBuilder()
            .setLenient()
            .create()
        val retrofit = Retrofit.Builder()
            .baseUrl(urlPath)
            .addConverterFactory(GsonConverterFactory.create(json))
            .build()
        val api = retrofit.create(Api::class.java)

        // get the screen shot
        CoroutineScope(Dispatchers.IO).launch {
            while (true) {
                delay(300)
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
                        if (!messageShouldStop) {
                            val toast = Toast.makeText(
                                applicationContext,
                                "Failed to get screen shot", Toast.LENGTH_SHORT
                            )
                            toast.setGravity(Gravity.TOP or Gravity.CENTER_HORIZONTAL, 350, 20)
                            toast.show()
                        }
                    }
                })
            }
        }
    }

    private fun setValuesCommand() {
        val json =
            "{\"aileron\": $aileron,\n \"rudder\": $rudder,\n \"elevator\": $elevator,\n \"throttle\": $throttle\n}"
        val rb = RequestBody.create(MediaType.parse("application/json"), json)
        val gson = GsonBuilder().setLenient().create()
        val retrofit = Retrofit.Builder()
            .baseUrl(urlPath)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
        val api = retrofit.create(Api::class.java)
        val body = api.postCommand(rb).enqueue(object : Callback<ResponseBody> {
            override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                try {
                    Log.d("FlightMobileApp", response.body().toString())
                    println("make the update correctly")
                } catch (e: IOException) {
                    if (!messageShouldStop) {
                        val toast = Toast.makeText(
                            applicationContext,
                            "Failed to set values", Toast.LENGTH_SHORT
                        )
                        toast.setGravity(Gravity.TOP or Gravity.CENTER_HORIZONTAL, 350, 20)
                        toast.show()
                    }
                }
            }

            override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                Toast.makeText(
                    applicationContext,
                    "Failed to set values", Toast.LENGTH_SHORT
                ).show()
            }
        })
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun logOut(view: View) {
        // move to the main window
        val intent = Intent(this, MainActivity::class.java).apply { }
        startActivity(intent)

        // the message should stop
        messageShouldStop = true

        // shut down the window
        onStop()
    }

    private fun moreThanOnePercent(changedVal: Double, originalVal: Double): Boolean {
        return if (originalVal == 0.0 && changedVal != 0.0)
            true
        else {
            val absValue =
                kotlin.math.abs(kotlin.math.abs(changedVal) - kotlin.math.abs(originalVal))
            return absValue >= 0.01 * kotlin.math.abs(originalVal)
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
            val newAileron = "aileron: $roundedX"
            val newElevator = "elevator: $roundedY"
            this.aileronText.text = newAileron
            this.elevatorText.text = newElevator

            // check if the values change in more than 1%
            if (moreThanOnePercent(roundedX, aileron) || moreThanOnePercent(roundedY, elevator)) {
                aileron = roundedX
                elevator = roundedY

                // turning on the set commands function
                setValuesCommand()
            }
        })
    }

    private fun setRudderSliderListeners() {
        // turning on the on move function of the sliders
        rudderSlider.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, i: Int, b: Boolean) {
                val value = i / 10.toDouble()

                // Display the current progress of SeekBar
                rudder = value
                rudderSlider.progress = (value * 10).toInt()
                val newRudder = "rudder: $value"
                rudderText.text = newRudder

                // check if the values changed in more than 1%
                if (moreThanOnePercent(value, aileron)) {
                    // turning on the set commands function
                    setValuesCommand()
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

                // Display the current progress of SeekBar
                throttle = value
                throttleSlider.progress = (value * 10).toInt()
                val newThrottle = "throttle: $value"
                throttleText.text = newThrottle

                // check if the values changed in more than 1%
                if (moreThanOnePercent(value, aileron)) {
                    // turning on the set commands function
                    setValuesCommand()
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {}
            override fun onStopTrackingTouch(seekBar: SeekBar) {}
        })
    }
}