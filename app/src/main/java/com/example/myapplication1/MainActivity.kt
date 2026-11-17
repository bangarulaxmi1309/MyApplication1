package com.example.myapplication1

import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity() {

    private lateinit var txtLocation: TextView
    private lateinit var txtAQI: TextView
    private lateinit var txtStatus: TextView
    private lateinit var txtUpdated: TextView

    private lateinit var txtPM25: TextView
    private lateinit var txtPM10: TextView
    private lateinit var txtCO: TextView
    private lateinit var txtNO2: TextView
    private lateinit var txtSO2: TextView
    private lateinit var txtO3: TextView

    private lateinit var txtHealthAdvice: TextView

    private lateinit var btnRefresh: Button
    private lateinit var btnHistory: Button

    private val executor = Executors.newSingleThreadExecutor()

    private val locationPermissionCode = 100

    private val fusedLocationClient by lazy {
        LocationServices.getFusedLocationProviderClient(this)
    }

    private val preferences by lazy {
        getSharedPreferences("AirQualityHistory", MODE_PRIVATE)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initializeViews()

        btnRefresh.setOnClickListener {
            getCurrentLocation()
        }

        btnHistory.setOnClickListener {
            showHistory()
        }

        getCurrentLocation()
    }

    // Initialize XML views
    private fun initializeViews() {

        txtLocation = findViewById(R.id.txtLocation)
        txtAQI = findViewById(R.id.txtAQI)
        txtStatus = findViewById(R.id.txtStatus)
        txtUpdated = findViewById(R.id.txtUpdated)

        txtPM25 = findViewById(R.id.txtPM25)
        txtPM10 = findViewById(R.id.txtPM10)
        txtCO = findViewById(R.id.txtCO)
        txtNO2 = findViewById(R.id.txtNO2)
        txtSO2 = findViewById(R.id.txtSO2)
        txtO3 = findViewById(R.id.txtO3)

        txtHealthAdvice = findViewById(R.id.txtHealthAdvice)

        btnRefresh = findViewById(R.id.btnRefresh)
        btnHistory = findViewById(R.id.btnHistory)
    }

    // Check location permission
    private fun getCurrentLocation() {

        if (
            ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {

            ActivityCompat.requestPermissions(
                this,
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ),
                locationPermissionCode
            )

            return
        }

        txtLocation.text = "Detecting current location..."

        val token = CancellationTokenSource()

        fusedLocationClient.getCurrentLocation(
            Priority.PRIORITY_BALANCED_POWER_ACCURACY,
            token.token
        ).addOnSuccessListener { location: Location? ->

            if (location != null) {
                fetchAirQuality(
                    location.latitude,
                    location.longitude
                )
            } else {
                txtLocation.text = "Location unavailable"
                Toast.makeText(
                    this,
                    "Unable to get location. Try again.",
                    Toast.LENGTH_LONG
                ).show()
            }

        }.addOnFailureListener {

            txtLocation.text = "Location detection failed"

            Toast.makeText(
                this,
                it.message ?: "Location error",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    // Request location permission result
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(
            requestCode,
            permissions,
            grantResults
        )

        if (requestCode == locationPermissionCode) {

            if (grantResults.isNotEmpty() &&
                grantResults.any {
                    it == PackageManager.PERMISSION_GRANTED
                }
            ) {
                getCurrentLocation()
            } else {
                txtLocation.text = "Location permission denied"
                Toast.makeText(
                    this,
                    "Please allow location permission",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    // Fetch live AQI
    private fun fetchAirQuality(lat: Double, lon: Double) {

        txtStatus.text = "Loading AQI..."

        executor.execute {

            try {

                val apiUrl =
                    "https://air-quality-api.open-meteo.com/v1/air-quality" +
                            "?latitude=$lat&longitude=$lon" +
                            "&current=us_aqi,pm2_5,pm10,carbon_monoxide," +
                            "nitrogen_dioxide,sulphur_dioxide,ozone" +
                            "&timezone=auto"

                val connection =
                    URL(apiUrl).openConnection() as HttpURLConnection

                connection.requestMethod = "GET"
                connection.connectTimeout = 15000
                connection.readTimeout = 15000

                try {

                    val response = connection.inputStream
                        .bufferedReader()
                        .use { it.readText() }

                    val json = JSONObject(response)
                    val current = json.getJSONObject("current")

                    val aqi = current.getInt("us_aqi")

                    val pm25 = current.getDouble("pm2_5")
                    val pm10 = current.getDouble("pm10")
                    val co = current.getDouble("carbon_monoxide")
                    val no2 = current.getDouble("nitrogen_dioxide")
                    val so2 = current.getDouble("sulphur_dioxide")
                    val o3 = current.getDouble("ozone")

                    val apiTime = current.optString("time", "--")

                    val timestamp = SimpleDateFormat(
                        "dd-MM-yyyy HH:mm:ss",
                        Locale.getDefault()
                    ).format(Date())

                    val advice = getHealthAdvice(aqi)

                    runOnUiThread {

                        txtLocation.text =
                            "📍 Lat: %.4f, Lon: %.4f".format(lat, lon)

                        txtAQI.text = aqi.toString()

                        txtStatus.text = advice.first
                        txtStatus.setTextColor(
                            android.graphics.Color.parseColor(advice.second)
                        )

                        txtHealthAdvice.text = advice.third

                        txtPM25.text = "PM2.5: $pm25 µg/m³"
                        txtPM10.text = "PM10: $pm10 µg/m³"
                        txtCO.text = "CO: $co µg/m³"
                        txtNO2.text = "NO2: $no2 µg/m³"
                        txtSO2.text = "SO2: $so2 µg/m³"
                        txtO3.text = "O3: $o3 µg/m³"

                        txtUpdated.text =
                            "API time: $apiTime\nSaved: $timestamp"

                        saveReading(
                            aqi,
                            pm25,
                            pm10,
                            co,
                            no2,
                            so2,
                            o3,
                            timestamp
                        )
                    }

                } finally {
                    connection.disconnect()
                }

            } catch (e: Exception) {

                runOnUiThread {

                    txtStatus.text = "Data unavailable"

                    Toast.makeText(
                        this,
                        e.message ?: "Unable to fetch AQI",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    // AQI-based health recommendations
    private fun getHealthAdvice(aqi: Int):
            Triple<String, String, String> {

        return when {

            aqi <= 50 -> Triple(
                "Good",
                "#16A34A",
                "Air quality is good. Enjoy outdoor activities. No special precautions are needed."
            )

            aqi <= 100 -> Triple(
                "Moderate",
                "#EAB308",
                "Air quality is acceptable. Sensitive people should consider reducing prolonged outdoor activity."
            )

            aqi <= 150 -> Triple(
                "Unhealthy for Sensitive Groups",
                "#F97316",
                "Children, elderly people and people with asthma should limit prolonged outdoor activity."
            )

            aqi <= 200 -> Triple(
                "Unhealthy",
                "#DC2626",
                "Reduce outdoor exercise and prolonged exposure. Sensitive groups should avoid outdoor activity."
            )

            aqi <= 300 -> Triple(
                "Very Unhealthy",
                "#9333EA",
                "Avoid outdoor activities as much as possible. Follow local health advisories."
            )

            else -> Triple(
                "Hazardous",
                "#7F1D1D",
                "Stay indoors as much as possible and follow official health guidance."
            )
        }
    }

    // Save previous AQI readings
    private fun saveReading(
        aqi: Int,
        pm25: Double,
        pm10: Double,
        co: Double,
        no2: Double,
        so2: Double,
        o3: Double,
        time: String
    ) {

        val oldHistory = preferences.getString("history", "") ?: ""

        val newRecord =
            "$time | AQI: $aqi | PM2.5: $pm25 | PM10: $pm10" +
                    " | CO: $co | NO2: $no2 | SO2: $so2 | O3: $o3"

        val updatedHistory = if (oldHistory.isEmpty()) {
            newRecord
        } else {
            "$newRecord\n$oldHistory"
        }

        preferences.edit()
            .putString("history", updatedHistory)
            .apply()
    }

    // Display previous readings
    private fun showHistory() {

        val history = preferences.getString("history", "")

        if (history.isNullOrEmpty()) {

            Toast.makeText(
                this,
                "No previous readings available",
                Toast.LENGTH_SHORT
            ).show()

        } else {

            android.app.AlertDialog.Builder(this)
                .setTitle("Air Quality History")
                .setMessage(history)
                .setPositiveButton("OK", null)
                .setNeutralButton("Clear History") { _, _ ->

                    preferences.edit()
                        .remove("history")
                        .apply()

                    Toast.makeText(
                        this,
                        "History cleared",
                        Toast.LENGTH_SHORT
                    ).show()
                }
                .show()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        executor.shutdown()
    }
}
