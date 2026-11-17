package com.example.myapplication1

import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

object AirQualityApi {

    fun getAirQuality(
        latitude: Double,
        longitude: Double
    ): JSONObject {

        val apiUrl =
            "https://air-quality-api.open-meteo.com/v1/air-quality" +
                    "?latitude=$latitude" +
                    "&longitude=$longitude" +
                    "&current=us_aqi,pm2_5,pm10,carbon_monoxide,nitrogen_dioxide,sulphur_dioxide,ozone" +
                    "&timezone=auto"

        val connection = URL(apiUrl).openConnection() as HttpURLConnection

        return try {
            connection.requestMethod = "GET"
            connection.connectTimeout = 15000
            connection.readTimeout = 15000

            val responseCode = connection.responseCode

            if (responseCode == HttpURLConnection.HTTP_OK) {
                val response = connection.inputStream
                    .bufferedReader()
                    .use { it.readText() }

                JSONObject(response)
            } else {
                throw Exception("API Error: HTTP $responseCode")
            }

        } finally {
            connection.disconnect()
        }
    }
}
