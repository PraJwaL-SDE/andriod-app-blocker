package com.example.appblocker.service

import android.annotation.SuppressLint
import android.app.Service
import android.content.Context
import android.content.Intent
import android.app.usage.UsageStats
import android.app.usage.UsageStatsManager
import android.os.Handler
import android.os.IBinder
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStoreOwner
import com.example.appblocker.MainActivity
import com.example.appblocker.ViewModel.UsageViewModel

class BackgroundService : Service() {
    private lateinit var notificationService: NotificationService
    private lateinit var handler: Handler
    private val updateInterval: Long = 1000 // Check every second
    private var youtubeOpenTime: Long = 0 // Time in seconds
    private var isYouTubeOpen: Boolean = false
    private lateinit var cooldownHandler: Handler
    private lateinit var mainActivityContext: Context
    private lateinit var currentForegroundApp: String
    companion object {
        private lateinit var owner: ViewModelStoreOwner

        fun getViewModelOwner(owner: ViewModelStoreOwner){
            this.owner  = owner
        }
    }


    private lateinit var usageViewModel: UsageViewModel

    override fun onCreate() {
        super.onCreate()
        notificationService = NotificationService()
        handler = Handler()
        usageViewModel = ViewModelProvider(owner).get(UsageViewModel::class.java)
        Log.d("BackgroundService", "Service created")
        startTrackingYouTubeUsage()
    }



    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d("BackgroundService", "Service started")
        notificationService.showNotification(this, "YouTube Usage Time", "YouTube is not open.")
        return START_STICKY
    }

    private fun startTrackingYouTubeUsage() {
        handler.post(object : Runnable {
            @SuppressLint("SuspiciousIndentation")
            override fun run() {
                val currentApp = getForegroundAppInfo(this@BackgroundService)
                val sharedPreferences = getSharedPreferences("AppBlockerPrefs", Context.MODE_PRIVATE)
                val timeLimit = sharedPreferences.getLong("time_limit", 0)
                if(currentApp!=null)
                currentForegroundApp = currentApp
                // Update current status before any changes
                setCurrentStatus(this@BackgroundService, "Usage Time: $youtubeOpenTime")

                if (currentApp == "com.google.android.youtube") {
                    if (!isYouTubeOpen) {
                        isYouTubeOpen = true
                        Log.d("BackgroundService", "YouTube is now open.")

                        // Show floating view to set time limit if it’s not set
                        if (timeLimit == 0L) {
                            startFloatingViewService()
                        } else {
                            Toast.makeText(this@BackgroundService, "Time limit is set for $timeLimit seconds", Toast.LENGTH_SHORT).show()
                        }
                    }

                    // Track YouTube usage if time limit is set
                    if (timeLimit > 0) {
                        youtubeOpenTime += 1
                        setYouTubeTimeLimit(this@BackgroundService, youtubeOpenTime)

                        notificationService.showNotification(this@BackgroundService, "YouTube Usage Time", "YouTube is open for $youtubeOpenTime seconds.")
                        Log.d("BackgroundService", "YouTube time limit remaining ${timeLimit - youtubeOpenTime}.")

                        if (youtubeOpenTime == timeLimit) {
                            Log.d("BackgroundService", "YouTube time limit exceeded.")
                            redirectToMyApp()
                            startCooldown()
                        }
                    }
                } else {
                    // Reset tracking if YouTube is no longer open
                    if (isYouTubeOpen) {
                        isYouTubeOpen = false
                        stopFloatingViewService()
                        Log.d("BackgroundService", "YouTube is no longer open.")
                    }
                }
                handler.postDelayed(this, updateInterval)
            }
        })
    }

    private fun startFloatingViewService() {
        val intent = Intent(this, FloatingViewService::class.java)
        startService(intent)
    }

    private fun stopFloatingViewService() {
        val intent = Intent(this, FloatingViewService::class.java)
        stopService(intent)
    }

    private fun getForegroundAppInfo(context: Context): String? {
        val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val endTime = System.currentTimeMillis()
        val startTime = endTime - 1000 * 60 * 60 // Check past hour for recent apps

        val usageStatsList = usageStatsManager.queryUsageStats(
            UsageStatsManager.INTERVAL_DAILY, startTime, endTime
        )

        val currentApp: UsageStats? = usageStatsList.maxByOrNull { it.lastTimeUsed }

        return currentApp?.packageName ?: run {
            Log.d("ForegroundAppInfo", "No app is currently in the foreground")
            null
        }
    }

    private fun redirectToMyApp() {
        Log.d("ForegroundAppInfo", "Redirecting to AppBlocker.")
        val intent = packageManager.getLaunchIntentForPackage("com.example.appblocker") // Replace with your app's package name
        intent?.let {
            it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(it)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacksAndMessages(null) // Stop updates
        Log.d("BackgroundService", "Service destroyed")
    }

    private fun setYouTubeTimeLimit(context: Context, usageTimeInSeconds: Long) {
        val sharedPreferences = context.getSharedPreferences("AppBlockerPrefs", Context.MODE_PRIVATE)
        with(sharedPreferences.edit()) {
            putLong("current_usage", usageTimeInSeconds) // Store in seconds
            apply()
        }
        Log.d("BackgroundService", "YouTube usage time set to $usageTimeInSeconds seconds.")
    }

    private fun startCooldown() {
        val sharedPreferences = getSharedPreferences("AppBlockerPrefs", Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()

        // Set initial cooldown to 30 seconds
        var cooldownTime = 30
        editor.putInt("cooldown_time", cooldownTime)
        editor.apply()

        // Initialize handler for cooldown countdown
        cooldownHandler = Handler()

        // Runnable to decrease cooldown each second
        val cooldownRunnable = object : Runnable {
            override fun run() {
                if (cooldownTime > 0) {
                    cooldownTime--
                    editor.putInt("cooldown_time", cooldownTime)
                    editor.apply()
                    if (currentForegroundApp == "com.google.android.youtube")
                        redirectToMyApp()
                    Log.d("Cooldown", "Cooldown time remaining: $cooldownTime seconds")
                    setCurrentStatus(this@BackgroundService, "Cooldown Time: $cooldownTime")
                    cooldownHandler.postDelayed(this, 1000)
                } else {
                    youtubeOpenTime = 0
                    val sharedPreferences = getSharedPreferences("AppBlockerPrefs", Context.MODE_PRIVATE)
                    sharedPreferences.edit().putLong("time_limit", 0).apply()
                    Log.d("Cooldown", "Cooldown complete.")
                }
            }
        }

        // Start cooldown countdown
        cooldownHandler.post(cooldownRunnable)
    }

    // Function to save a String value in SharedPreferences with key "show_current_status"
    private fun setCurrentStatus(context: Context, value: String) {
        Log.d("BackgroundService", "YouTube usage time: $youtubeOpenTime seconds")

        // Update ViewModel with the current status
        usageViewModel.updateUsage(value)
    }

    // Function to retrieve the String value stored in SharedPreferences with key "show_current_status"
    fun getCurrentStatus(context: Context): String {
        val sharedPreferences = context.getSharedPreferences("AppBlockerPrefs", Context.MODE_PRIVATE)
        return sharedPreferences.getString("show_current_status", "Default Value") ?: "Default Value"
    }

    override fun onBind(intent: Intent?): IBinder? = null


}
