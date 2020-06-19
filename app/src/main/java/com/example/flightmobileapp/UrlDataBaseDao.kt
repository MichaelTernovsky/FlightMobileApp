package com.example.flightmobileapp

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface UrlDataBaseDao {
    @Insert
    fun insert(url: Url)

    @Query("SELECT * FROM url_table ORDER BY currTime DESC LIMIT 5")
    fun getAllUrls(): MutableList<Url>

    @Query("DELETE FROM url_table")
    fun delete()

    @Query("SELECT * FROM url_table ORDER BY currTime DESC LIMIT 1")
    fun getFirstUrl(): Url
}