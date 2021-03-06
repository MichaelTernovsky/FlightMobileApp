package com.example.flightmobileapp

import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.View
import android.widget.ImageView
import android.widget.SeekBar
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import com.google.gson.GsonBuilder
import io.github.controlwear.virtual.joystick.android.JoystickView
import kotlinx.android.synthetic.main.activity_app.*
import kotlinx.coroutines.*
import kotlinx.coroutines.Dispatchers.IO
import okhttp3.MediaType
import okhttp3.OkHttpClient
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

    // assisting variable to know if the current screen shot changed
    private var isScreenShotChanged = false

    override fun onStart() {
        super.onStart()

        if (!isScreenShotChanged) {
            getScreenShot()
        }
    }

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
        getScreenShotLoop()
    }

    override fun onPause() {
        super.onPause()
        if (isScreenShotChanged) {
            isScreenShotChanged = false
        }
    }

    override fun onResume() {
        super.onResume()
        if (!isScreenShotChanged) {
            getScreenShot()
        }
    }

    /*
    The function is responsible for sending values to the simulator
     */
    private fun setValuesCommand() {
        val json =
            "{\"aileron\": $aileron,\n \"rudder\": $rudder,\n \"elevator\": " +
                    "$elevator,\n \"throttle\": $throttle\n}"
        val rb = RequestBody.create(MediaType.parse("application/json"), json)
        val gson = GsonBuilder().setLenient().create()
        val retrofit = Retrofit.Builder()
            .baseUrl(urlPath)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
        val api = retrofit.create(Api::class.java)
        val body = api.postCommand(rb).enqueue(object : Callback<ResponseBody> {
            override fun onResponse(
                call: Call<ResponseBody>, response:
                Response<ResponseBody>
            ) {
                try {
                    // try to send the message
                    Log.d("FlightMobileApp", response.body().toString())
                } catch (e: IOException) {
                    val valuesToast = Toast.makeText(
                        applicationContext,
                        "Failed to set values", Toast.LENGTH_SHORT
                    )
                    valuesToast.setGravity(
                        Gravity.TOP or Gravity.CENTER_HORIZONTAL,
                        430, 20
                    )
                    valuesToast.show()
                }
            }

            override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                val valuesToast = Toast.makeText(
                    applicationContext,
                    "Failed to set values", Toast.LENGTH_SHORT
                )
                valuesToast.setGravity(
                    Gravity.TOP or Gravity.CENTER_HORIZONTAL,
                    430, 20
                )
                valuesToast.show()
            }
        })
    }

    /*
     The function is responsible for getting a single screen shot from the simulator
     */
    private fun getScreenShot() {
        val json = GsonBuilder().setLenient().create()

        val retrofit = Retrofit.Builder().baseUrl(urlPath)
            .addConverterFactory(GsonConverterFactory.create(json)).build()

        val api = retrofit.create(Api::class.java)

        val body = api.getImg().enqueue(object : Callback<ResponseBody> {
            override fun onResponse(
                call: Call<ResponseBody>, response:
                Response<ResponseBody>
            ) {
                if (response.isSuccessful) {
                    val inputStream = response.body()?.byteStream()
                    val bitmap = BitmapFactory.decodeStream(inputStream)
                    runOnUiThread {
                        val imageView = findViewById<ImageView>(R.id.img)
                        imageView.setImageBitmap(bitmap)
                    }
                } else {
                    val valuesToast = Toast.makeText(
                        applicationContext,
                        "Failed to load screen shot", Toast.LENGTH_SHORT
                    )
                    valuesToast.setGravity(
                        Gravity.TOP or Gravity.CENTER_HORIZONTAL,
                        350, 20
                    )
                    valuesToast.show()
                }
            }

            override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                val valuesToast = Toast.makeText(
                    applicationContext,
                    "Failed to load screen shot", Toast.LENGTH_SHORT
                )
                valuesToast.setGravity(
                    Gravity.TOP or Gravity.CENTER_HORIZONTAL,
                    350, 20
                )
                valuesToast.show()
            }
        })
    }

    /*
     The function is responsible for the while loop in which we take a screen shot every 500 millis
     */
    private fun getScreenShotLoop() {
        isScreenShotChanged = true
        CoroutineScope(IO).launch() {
            while (isScreenShotChanged) {
                getScreenShot()
                delay(500)
            }
        }
    }

    /*
     The function is checking if the value is changed by more than 1 percent
     */
    private fun moreThanOnePercent(changedVal: Double, originalVal: Double): Boolean {
        return if (originalVal == 0.0 && changedVal != 0.0)
            true
        else {
            val absValue =
                kotlin.math.abs(kotlin.math.abs(changedVal) - kotlin.math.abs(originalVal))
            return absValue >= 0.01 * kotlin.math.abs(originalVal)
        }
    }

    /*
     The function is setting the methods that would happen if the joystick value is changed
     */
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
            if (moreThanOnePercent(roundedX, aileron) || moreThanOnePercent(
                    roundedY,
                    elevator
                )
            ) {
                aileron = roundedX
                elevator = roundedY

                // turning on the set commands function
                CoroutineScope(IO).launch {
                    setValuesCommand()
                }
            }
        })
    }

    /*
     The function is setting the methods that would happen if the rudder slider value is changed
     */
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
                    CoroutineScope(Dispatchers.IO).launch {
                        setValuesCommand()
                    }
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {}
            override fun onStopTrackingTouch(seekBar: SeekBar) {}
        })
    }

    /*
     The function is setting the methods that would happen if the throttle slider value is changed
     */
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
                    CoroutineScope(IO).launch {
                        setValuesCommand()
                    }
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {}
            override fun onStopTrackingTouch(seekBar: SeekBar) {}
        })
    }

    /*
     The function moves us to the main page and stops the while loop of the screen shots
     */
    @RequiresApi(Build.VERSION_CODES.O)
    fun logOut(view: View) {
        // move to the main window
        val intent = Intent(this, MainActivity::class.java).apply { }
        startActivity(intent)

        // stop the boolean predicate of the while loop
        isScreenShotChanged = false
    }
}