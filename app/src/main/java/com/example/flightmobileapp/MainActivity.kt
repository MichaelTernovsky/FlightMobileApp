package com.example.flightmobileapp

import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import com.google.gson.GsonBuilder
import kotlinx.android.synthetic.main.activity_main.*
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class MainActivity : AppCompatActivity() {
    // the data base
    private lateinit var db: UrlRepository

    // the list of all urls
    private var urlsList: MutableList<Url> = mutableListOf()

    // the list of buttons
    private var buttonsList: MutableList<Button> = mutableListOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // initialize the db
        db = UrlRepository(application)

        // show the urls
        showButtons()
    }

    private fun showButtons() {
        // get the urls from the db
        urlsList = db.getAllUrls()

        // the current layout
        val constraintLayout = findViewById<LinearLayout>(R.id.constraintLayout)

        // show each url as a button in the layout
        this.urlsList.forEach {
            val button = Button(this)
            button.text = it.url
            button.transformationMethod = null
            button.layoutParams = ConstraintLayout.LayoutParams(
                ConstraintLayout.LayoutParams.WRAP_CONTENT,
                ConstraintLayout.LayoutParams.WRAP_CONTENT
            )
            button.setBackgroundColor(Color.BLUE)
            button.setTextColor(Color.WHITE)
            constraintLayout.addView(button)
            button.setOnClickListener(View.OnClickListener {
                this.urlTxt.setText(button.text)
            })

            // add the new button to the list
            buttonsList.add(button)
        }
    }

    private fun deleteButtons() {
        if (buttonsList.isNotEmpty()) {
            // the current layout
            val constraintLayout = findViewById<LinearLayout>(R.id.constraintLayout)

            // remove all the button
            for (btn in this.buttonsList) {
                constraintLayout.removeView(btn)
            }
        }

        // clear the list
        buttonsList.clear()
    }

    private fun deleteIfExistInList(urlToFind: Url) {
        var index = 0
        var urlIndex = 0
        var isFound = 0

        // run over the urls list
        this.urlsList.forEach {
            if (it.url == urlToFind.url) {
                // update the specific url's index
                urlIndex = index

                // turn on the flag
                isFound = 1
            }
            index++;
        }

        // delete the url from the list if it exists
        if (isFound == 1) {
            this.urlsList.removeAt(urlIndex)
        }
    }

    private fun moveToSecondWindow() {
        // success - move to the next window
        val intent = Intent(this, AppActivity::class.java).apply { }
        startActivity(intent)
    }

    private fun tryToConnect(urlPath: String) {
        val json = GsonBuilder()
            .setLenient()
            .create()
        val retrofit = Retrofit.Builder()
            .baseUrl(urlPath)
            .addConverterFactory(GsonConverterFactory.create(json))
            .build()
        val api = retrofit.create(Api::class.java)

        val body = api.getImg().enqueue(object : Callback<ResponseBody> {
            // in case of success
            override fun onResponse(
                call: Call<ResponseBody>, response: Response<ResponseBody>
            ) {
                // success - move to the second window
                moveToSecondWindow()
            }

            // in case of failure
            override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                // failure - show correct message
                val toast = Toast.makeText(
                    applicationContext,
                    "Failed to connect the server", Toast.LENGTH_SHORT
                )
                toast.setGravity(Gravity.TOP or Gravity.CENTER_HORIZONTAL, 0, 200)
                toast.show()
            }
        })
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun addNewUrl(view: View) {
        val url = this.urlTxt.text.toString()
        val curTime = System.currentTimeMillis()
        val newUrl = Url(0, url, curTime)

        // check if the user chose url
        if (url != "") {
            // check if the url already exists
            deleteIfExistInList(newUrl)

            // add the new url to the local list
            urlsList.add(newUrl)

            // delete the current db
            db.delete()

            // update the new db
            for (it in this.urlsList) {
                db.insert(it)
            }

            // delete the buttons
            deleteButtons()

            // show the buttons again
            showButtons()

            // check if we can connect to the server
            tryToConnect(url)
        }
    }
}