package com.example.appblocker.service

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.PixelFormat
import android.os.IBinder
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.Toast
import com.example.appblocker.R
class FloatingViewService : Service() {
    private lateinit var floatingView: View
    private var selectedTimeLimit: Long = 0 // Variable to store selected time
    private lateinit var sharedPreferences: SharedPreferences

    override fun onCreate() {
        super.onCreate()
        sharedPreferences = getSharedPreferences("AppBlockerPrefs", Context.MODE_PRIVATE)
        showFloatingView()
    }

    private fun showFloatingView() {
        val inflater = LayoutInflater.from(this)
        floatingView = inflater.inflate(R.layout.layout_time_selector, null)

        // Configure layout parameters for the floating window
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        )

        // Add the view to the window
        val windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        windowManager.addView(floatingView, params)

        // Set up button listeners to store time in selectedTimeLimit
        setupButton(floatingView.findViewById(R.id.button_30_sec), 30_000)
        setupButton(floatingView.findViewById(R.id.button_1_min), 60_000)
        setupButton(floatingView.findViewById(R.id.button_2_min), 120_000)
        setupButton(floatingView.findViewById(R.id.button_5_min), 300_000)
        setupButton(floatingView.findViewById(R.id.button_10_min), 600_000)
        setupButton(floatingView.findViewById(R.id.button_15_min), 900_000)
    }

    private fun setupButton(button: Button, timeInMillis: Long) {
        button.setOnClickListener {
            selectedTimeLimit = timeInMillis/1000

            // Save selected time limit in SharedPreferences
            sharedPreferences.edit().putLong("time_limit", selectedTimeLimit).apply()

            // Dismiss the floating window
            val windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
            windowManager.removeView(floatingView)

            // Stop the service after removing the view, if desired
            stopSelf()
            Toast.makeText(this, "Time limit set: ${timeInMillis / 1000} seconds", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // Remove the floating view if it's still there
//        val windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
//        windowManager.removeView(floatingView)
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
