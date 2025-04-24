package com.example.financetracker.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.example.financetracker.R
import com.example.financetracker.util.PrefsManager
import com.example.financetracker.util.SecurityManager
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : AppCompatActivity() {
    
    companion object {
        private const val TAG = "MainActivity"
        private const val REQUEST_PASSCODE_VERIFICATION = 1001
    }
    
    // Flag to track if the app is locked
    private var isAppLocked = false
    
    // Flag to track if we're currently checking passcode
    private var isCheckingPasscode = false
    
    // Request notification permission launcher for Android 13+
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            Log.d(TAG, "Notification permission granted")
        } else {
            Log.d(TAG, "Notification permission denied")
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Initialize the PrefsManager
        PrefsManager.init(applicationContext)
        
        // Apply the saved theme mode
        applyThemeMode()
        
        setContentView(R.layout.activity_main)
        
        // Set up navigation
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController
        
        // Set up bottom navigation
        val bottomNavigationView = findViewById<BottomNavigationView>(R.id.bottom_navigation)
        bottomNavigationView.setupWithNavController(navController)
        
        // Request notification permission for Android 13+
        requestNotificationPermission()
        
        // Check if passcode is enabled - only on first launch
        if (savedInstanceState == null) {
            checkPasscodeProtection()
        }
    }
    
    override fun onResume() {
        super.onResume()
        
        // Only check passcode again when returning to the app from background
        if (isAppLocked && !isCheckingPasscode) {
            checkPasscodeProtection()
        }
    }
    
    override fun onPause() {
        super.onPause()
        
        // Set app as locked when going to background, but not when just checking passcode
        if (!isCheckingPasscode) {
            isAppLocked = true
        }
    }
    
    private fun checkPasscodeProtection() {
        if (SecurityManager.isPasscodeEnabled(this)) {
            isAppLocked = true
            isCheckingPasscode = true
            // Show passcode verification screen
            val intent = Intent(this, PasscodeActivity::class.java)
            intent.putExtra(PasscodeActivity.EXTRA_MODE, PasscodeActivity.MODE_VERIFY)
            startActivityForResult(intent, REQUEST_PASSCODE_VERIFICATION)
        } else {
            isAppLocked = false
            isCheckingPasscode = false
        }
    }
    
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        
        if (requestCode == REQUEST_PASSCODE_VERIFICATION) {
            isCheckingPasscode = false
            if (resultCode == RESULT_OK) {
                // Passcode verified successfully
                isAppLocked = false
                Log.d(TAG, "Passcode verified successfully")
            } else {
                // Passcode verification failed or canceled
                Log.d(TAG, "Passcode verification failed/canceled")
                finish() // Exit the app
            }
        }
    }
    
    private fun applyThemeMode() {
        val isDarkMode = PrefsManager.getThemeMode()
        if (isDarkMode) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        }
    }
    
    private fun requestNotificationPermission() {
        // For Android 13+ (API 33+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            when {
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED -> {
                    // Permission already granted
                    Log.d(TAG, "Notification permission already granted")
                }
                shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS) -> {
                    // Explain why we need the permission
                    Log.d(TAG, "Should show notification permission rationale")
                    // For simplicity, we'll just request anyway
                    requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
                else -> {
                    // Request the permission
                    Log.d(TAG, "Requesting notification permission")
                    requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
        } else {
            // Notification permission is granted by default for older Android versions
            Log.d(TAG, "Notification permission granted by default (Android < 13)")
        }
    }
} 