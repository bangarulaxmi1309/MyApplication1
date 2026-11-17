package com.example.myapplication1

data class AirQualityResponse(
    val location: String,
    val latitude: Double,
    val longitude: Double,
    val aqi: Int,
    val status: String,
    val pm25: Double,
    val pm10: Double,
    val co: Double,
    val no2: Double,
    val so2: Double,
    val o3: Double,
    val updatedTime: String,
    val healthAdvice: String
)
