package com.example.financetracker.util

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import java.security.MessageDigest

/**
 * Utility class to manage security features like passcode
 */
object SecurityManager {
    
    private const val SECURITY_PREFS = "finance_tracker_security"
    private const val KEY_PASSCODE_HASH = "passcode_hash"
    private const val KEY_PASSCODE_ENABLED = "passcode_enabled"
    private const val TAG = "SecurityManager"
    
    private fun getSecurityPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(SECURITY_PREFS, Context.MODE_PRIVATE)
    }
    
    /**
     * Save a passcode (stored as a SHA-256 hash, not plaintext)
     */
    fun savePasscode(context: Context, passcode: String) {
        try {
            val hash = hashPasscode(passcode)
            getSecurityPrefs(context).edit()
                .putString(KEY_PASSCODE_HASH, hash)
                .apply()
            
            Log.d(TAG, "Passcode saved successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error saving passcode: ${e.message}")
        }
    }
    
    /**
     * Verify if the entered passcode matches the stored one
     */
    fun verifyPasscode(context: Context, passcode: String): Boolean {
        try {
            val storedHash = getSecurityPrefs(context).getString(KEY_PASSCODE_HASH, "") ?: ""
            if (storedHash.isEmpty()) {
                // No passcode set
                return false
            }
            
            val enteredHash = hashPasscode(passcode)
            return enteredHash == storedHash
        } catch (e: Exception) {
            Log.e(TAG, "Error verifying passcode: ${e.message}")
            return false
        }
    }
    
    /**
     * Check if passcode protection is enabled
     */
    fun isPasscodeEnabled(context: Context): Boolean {
        return getSecurityPrefs(context).getBoolean(KEY_PASSCODE_ENABLED, false)
    }
    
    /**
     * Enable or disable passcode protection
     */
    fun setPasscodeEnabled(context: Context, enabled: Boolean) {
        getSecurityPrefs(context).edit()
            .putBoolean(KEY_PASSCODE_ENABLED, enabled)
            .apply()
        
        Log.d(TAG, "Passcode ${if (enabled) "enabled" else "disabled"}")
    }
    
    /**
     * Clear all passcode data (for when disabling passcode protection)
     */
    fun clearPasscode(context: Context) {
        getSecurityPrefs(context).edit()
            .remove(KEY_PASSCODE_HASH)
            .remove(KEY_PASSCODE_ENABLED)
            .apply()
        
        Log.d(TAG, "Passcode data cleared")
    }
    
    /**
     * Hash the passcode using SHA-256
     */
    private fun hashPasscode(passcode: String): String {
        val bytes = passcode.toByteArray()
        val md = MessageDigest.getInstance("SHA-256")
        val digest = md.digest(bytes)
        return digest.fold("") { str, it -> str + "%02x".format(it) }
    }
} 