package com.example.financetracker.ui

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.financetracker.R
import com.example.financetracker.data.PrefsManager

class OnboardingActivity : AppCompatActivity() {
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Skip onboarding and go directly to MainActivity
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
} 