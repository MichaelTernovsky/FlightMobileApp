package com.example.flightmobileapp

import android.app.Application

class UrlRepository(application: Application) {
    private var db: UrlDataBaseDao

    init {
        val database = UrlDataBase.getInstance(application)
        db = database.urlDataBaseDao
    }

    fun getAllUrls(): MutableList<Url> {
        return db.getAllUrls()
    }

    fun insert(url: Url) {
        db.insert(url)
    }

    fun delete() {
        db.delete()
    }

    fun getByUrl(url: String): Url? {
        return db.getByID(url)
    }
}