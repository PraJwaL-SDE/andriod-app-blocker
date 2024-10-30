package com.example.appblocker

import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import com.example.appblocker.ViewModel.UsageViewModel
import com.example.appblocker.adapter.AppListAdapter
import com.example.appblocker.service.BackgroundService
import com.example.appblocker.service.FloatingViewService
import com.example.appblocker.service.NotificationService

class MainActivity : AppCompatActivity() {
    private var selectedApp: String? = null
    private val addedApps = mutableListOf<String>()
    private lateinit var usageDataTextView: TextView
    private lateinit var sharedPreferences: SharedPreferences

    private val usageViewModel: UsageViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Start the background service
        BackgroundService.getViewModelOwner(this)
        startService(Intent(this, BackgroundService::class.java))

        // Check for usage stats permission
        if (!hasUsageStatsPermission()) {
            startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
        }

        val appNameTextView = findViewById<TextView>(R.id.app_name)
        usageDataTextView = findViewById<TextView>(R.id.usage_data)
        val selectAppButton = findViewById<Button>(R.id.select_app_button)
        val addAppButton = findViewById<Button>(R.id.add_app_button)
        val addedAppsListView = findViewById<ListView>(R.id.added_apps_list)
        val monitorButton = findViewById<Button>(R.id.monitor_btn)

        selectAppButton.setOnClickListener {
            showInstalledAppsDialog(appNameTextView)
        }

        addAppButton.setOnClickListener {
            selectedApp?.let { app ->
                if (!addedApps.contains(app)) {
                    addedApps.add(app)
                    updateAddedAppsList(addedAppsListView)
                    Toast.makeText(this, "$app added to the list", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "$app is already in the list", Toast.LENGTH_SHORT).show()
                }
            } ?: Toast.makeText(this, "Please select an app first", Toast.LENGTH_SHORT).show()
        }
        usageViewModel.currentUsage.observe(this, Observer { usage ->
            usageDataTextView.text = usage
        })

        monitorButton.setOnClickListener{
            val sharedPreferences = getSharedPreferences("AppBlockerPrefs", Context.MODE_PRIVATE)
            sharedPreferences.edit().putLong("time_limit", 0).apply()
        }


//        sharedPreferences = getSharedPreferences("AppBlockerPrefs", Context.MODE_PRIVATE)
//
//        // Update the TextView initially with the current value
//        updateUsageDataTextView()
//
//        // Register a listener for SharedPreferences changes
//        sharedPreferences.registerOnSharedPreferenceChangeListener { _, key ->
//            if (key == "show_current_status") {
//                updateUsageDataTextView() // Call the function to update the TextView
//            }
//        }
    }

    private fun updateUsageDataTextView() {
        usageDataTextView.text = sharedPreferences.getString("show_current_status", "Default Value") ?: "Some Error occurred"
    }

    private fun showInstalledAppsDialog(appNameTextView: TextView) {
        val packageManager = packageManager
        val apps = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            packageManager.getInstalledPackages(PackageManager.PackageInfoFlags.of(0L))
        } else {
            packageManager.getInstalledPackages(0)
        }

        val appInfoList = apps.map { packageInfo -> packageInfo.applicationInfo }

        val adapter = AppListAdapter(this, appInfoList)
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Select an app to add")
        builder.setAdapter(adapter) { _, which ->
            selectedApp = appInfoList[which].packageName
            appNameTextView.text = "Selected App: ${packageManager.getApplicationLabel(appInfoList[which])}"
        }
        builder.show()
    }

    private fun updateAddedAppsList(listView: ListView) {
        val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, addedApps)
        listView.adapter = adapter
    }

    private fun startFloatingViewService() {
        val intent = Intent(this, FloatingViewService::class.java)
        intent.putExtra("added_apps", addedApps.toTypedArray())
        startService(intent)
    }

    private fun hasUsageStatsPermission(): Boolean {
        val appOpsManager = getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = appOpsManager.checkOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            android.os.Process.myUid(), packageName
        )
        return mode == AppOpsManager.MODE_ALLOWED
    }

    override fun onDestroy() {
        super.onDestroy()
        // Unregister the SharedPreferences listener to prevent memory leaks
        sharedPreferences.unregisterOnSharedPreferenceChangeListener { _, key ->
            if (key == "show_current_status") {
                updateUsageDataTextView()
            }
        }
    }
}
