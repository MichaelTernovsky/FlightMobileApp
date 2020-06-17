package com.example.flightmobileapp

import android.graphics.Color
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import androidx.annotation.RequiresApi
import androidx.constraintlayout.widget.ConstraintLayout
import kotlinx.android.synthetic.main.activity_main.*

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

    @RequiresApi(Build.VERSION_CODES.O)
    fun addNewUrl(view: View) {
        val url = this.urlTxt.text.toString()
        val curTime = System.currentTimeMillis()
        val newUrl = Url(0, url, curTime)

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
    }
}