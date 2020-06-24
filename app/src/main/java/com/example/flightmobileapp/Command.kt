package com.example.flightmobileapp

import androidx.annotation.NonNull

data class Command(
    @NonNull
    var aileron: Double,
    @NonNull
    var rudder: Double,
    @NonNull
    var elevator: Double,
    @NonNull
    var throttle: Double
)