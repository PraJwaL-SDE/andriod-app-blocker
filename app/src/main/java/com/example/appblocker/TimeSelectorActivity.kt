// com/example/appblocker/TimeSelectorActivity.kt
package com.example.appblocker

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class TimeSelectorActivity : AppCompatActivity() {

    private var selectedTimeLimit: Long = 0 // Variable to store selected time in milliseconds

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.layout_time_selector)

        // Set up button click listeners
        setupButton(findViewById(R.id.button_30_sec), 30_000)
        setupButton(findViewById(R.id.button_1_min), 60_000)
        setupButton(findViewById(R.id.button_2_min), 120_000)
        setupButton(findViewById(R.id.button_5_min), 300_000)
        setupButton(findViewById(R.id.button_10_min), 600_000)
        setupButton(findViewById(R.id.button_15_min), 900_000)
    }

    private fun setupButton(button: Button, timeInMillis: Long) {
        button.setOnClickListener {
            selectedTimeLimit = timeInMillis
            Toast.makeText(this, "Time limit set: ${timeInMillis / 1000} seconds", Toast.LENGTH_SHORT).show()
            // You can store the value in shared preferences or a database here for future use
        }
    }
}
