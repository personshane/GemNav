package com.gemnav.app.models

data class Destination(
    val id: String,
    val name: String,
    val address: String,
    val latitude: Double = 0.0,
    val longitude: Double = 0.0
)
