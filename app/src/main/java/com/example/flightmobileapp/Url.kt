package com.example.flightmobileapp

import androidx.annotation.NonNull
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "url_table")
data class Url(
    @PrimaryKey(autoGenerate = true)
    var id: Int,
    @ColumnInfo(name = "url")
    @NonNull
    val url: String,
    @ColumnInfo(name = "currTime")
    @NonNull
    val currTime: Long
)