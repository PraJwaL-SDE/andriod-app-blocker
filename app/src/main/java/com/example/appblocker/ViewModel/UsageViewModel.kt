package com.example.appblocker.ViewModel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class UsageViewModel(application: Application) : AndroidViewModel(application) {
    private val _currentUsage = MutableLiveData<String>()
    val currentUsage: LiveData<String> get() = _currentUsage

    fun updateUsage(newUsage: String) {
        _currentUsage.value = newUsage
    }
}
