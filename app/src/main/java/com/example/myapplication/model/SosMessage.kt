package com.example.myapplication.model

data class SosMessage(
    val id: String,
    val latitude: Double,
    val longitude: Double,
    val note: String? = null
)
