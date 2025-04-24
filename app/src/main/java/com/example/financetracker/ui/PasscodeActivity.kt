package com.example.financetracker.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import com.example.financetracker.R
import com.example.financetracker.util.SecurityManager

class PasscodeActivity : AppCompatActivity() {

    companion object {
        const val MODE_VERIFY = 0
        const val MODE_CREATE = 1
        const val MODE_CHANGE = 2
        const val EXTRA_MODE = "mode"
        
        // Static method to start this activity in verification mode
        fun startForVerification(context: Context) {
            val intent = Intent(context, PasscodeActivity::class.java)
            intent.putExtra(EXTRA_MODE, MODE_VERIFY)
            context.startActivity(intent)
        }
        
        // Static method to start this activity in creation mode
        fun startForCreation(context: Context) {
            val intent = Intent(context, PasscodeActivity::class.java)
            intent.putExtra(EXTRA_MODE, MODE_CREATE)
            context.startActivity(intent)
        }
        
        // Static method to start this activity in change mode
        fun startForChange(context: Context) {
            val intent = Intent(context, PasscodeActivity::class.java)
            intent.putExtra(EXTRA_MODE, MODE_CHANGE)
            context.startActivity(intent)
        }
    }
    
    // UI elements
    private lateinit var textViewTitle: TextView
    private lateinit var textViewSubtitle: TextView
    private lateinit var textViewError: TextView
    private lateinit var passcodeDigits: List<View>
    
    // Keypad buttons
    private lateinit var keyButtons: List<CardView>
    private lateinit var keyBackspace: CardView
    private lateinit var keyCancel: CardView
    
    // Passcode state
    private val passcode = StringBuilder()
    private var confirmPasscode = ""
    private var currentMode = MODE_VERIFY
    private var isConfirmingPasscode = false
    private var isChangingOldPasscode = false
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_passcode)
        
        // Get the mode from intent
        currentMode = intent.getIntExtra(EXTRA_MODE, MODE_VERIFY)
        
        // In change mode, we always start by verifying old passcode
        if (currentMode == MODE_CHANGE) {
            isChangingOldPasscode = true
        }
        
        // Initialize UI
        initViews()
        setupKeypad()
        updateUI()
    }
    
    private fun initViews() {
        textViewTitle = findViewById(R.id.textViewPasscodeTitle)
        textViewSubtitle = findViewById(R.id.textViewPasscodeSubtitle)
        textViewError = findViewById(R.id.textViewPasscodeError)
        
        // Get passcode dots
        passcodeDigits = listOf(
            findViewById(R.id.passcodeDigit1),
            findViewById(R.id.passcodeDigit2),
            findViewById(R.id.passcodeDigit3),
            findViewById(R.id.passcodeDigit4)
        )
        
        // Initialize buttons
        keyButtons = listOf(
            findViewById(R.id.buttonKey0),
            findViewById(R.id.buttonKey1),
            findViewById(R.id.buttonKey2),
            findViewById(R.id.buttonKey3),
            findViewById(R.id.buttonKey4),
            findViewById(R.id.buttonKey5),
            findViewById(R.id.buttonKey6),
            findViewById(R.id.buttonKey7),
            findViewById(R.id.buttonKey8),
            findViewById(R.id.buttonKey9)
        )
        
        keyBackspace = findViewById(R.id.buttonKeyBackspace)
        keyCancel = findViewById(R.id.buttonKeyCancel)
    }
    
    private fun setupKeypad() {
        // Setup number buttons (0-9)
        for (i in 0..9) {
            keyButtons[i].setOnClickListener {
                onNumberClick(i)
            }
        }
        
        // Setup backspace button
        keyBackspace.setOnClickListener {
            if (passcode.isNotEmpty()) {
                passcode.deleteCharAt(passcode.length - 1)
                updatePasscodeDisplay()
            }
        }
        
        // Setup cancel button
        keyCancel.setOnClickListener {
            finish() // Close the activity
        }
    }
    
    private fun updateUI() {
        when (currentMode) {
            MODE_VERIFY -> {
                textViewTitle.text = "Enter Passcode"
                textViewSubtitle.text = "Please enter your passcode to unlock the app"
            }
            MODE_CREATE -> {
                if (isConfirmingPasscode) {
                    textViewTitle.text = "Confirm Passcode"
                    textViewSubtitle.text = "Please re-enter your passcode to confirm"
                } else {
                    textViewTitle.text = "Create Passcode"
                    textViewSubtitle.text = "Please enter a 4-digit passcode"
                }
            }
            MODE_CHANGE -> {
                if (isChangingOldPasscode) {
                    textViewTitle.text = "Current Passcode"
                    textViewSubtitle.text = "Please enter your current passcode"
                } else if (isConfirmingPasscode) {
                    textViewTitle.text = "Confirm New Passcode"
                    textViewSubtitle.text = "Please re-enter your new passcode"
                } else {
                    textViewTitle.text = "New Passcode"
                    textViewSubtitle.text = "Please enter a new 4-digit passcode"
                }
            }
        }
    }
    
    private fun onNumberClick(number: Int) {
        // Only accept 4 digits
        if (passcode.length < 4) {
            passcode.append(number)
            updatePasscodeDisplay()
            
            // Check if passcode is complete (4 digits)
            if (passcode.length == 4) {
                handleCompletePasscode()
            }
        }
    }
    
    private fun updatePasscodeDisplay() {
        // Reset all dots to empty
        for (dot in passcodeDigits) {
            dot.setBackgroundResource(R.drawable.passcode_dot_empty)
        }
        
        // Fill dots based on passcode length
        for (i in 0 until passcode.length) {
            passcodeDigits[i].setBackgroundResource(R.drawable.passcode_dot_filled)
        }
        
        // Hide any error message
        textViewError.visibility = View.INVISIBLE
    }
    
    private fun handleCompletePasscode() {
        when (currentMode) {
            MODE_VERIFY -> verifyPasscode()
            MODE_CREATE -> handleCreatePasscode()
            MODE_CHANGE -> handleChangePasscode()
        }
    }
    
    private fun verifyPasscode() {
        if (SecurityManager.verifyPasscode(this, passcode.toString())) {
            // Passcode is correct
            setResult(RESULT_OK)
            finish()
        } else {
            // Passcode is incorrect
            showError("Incorrect passcode. Please try again.")
            resetPasscode()
        }
    }
    
    private fun handleCreatePasscode() {
        if (isConfirmingPasscode) {
            // Check if confirmation matches original passcode
            if (passcode.toString() == confirmPasscode) {
                // Save the passcode
                SecurityManager.savePasscode(this, passcode.toString())
                SecurityManager.setPasscodeEnabled(this, true)
                
                setResult(RESULT_OK)
                finish()
            } else {
                // Passcodes don't match
                showError("Passcodes don't match. Please try again.")
                
                // Reset to first passcode entry
                isConfirmingPasscode = false
                resetPasscode()
                updateUI()
            }
        } else {
            // First passcode entry, save it and ask for confirmation
            confirmPasscode = passcode.toString()
            isConfirmingPasscode = true
            resetPasscode()
            updateUI()
        }
    }
    
    private fun handleChangePasscode() {
        if (isChangingOldPasscode) {
            // Verify old passcode
            if (SecurityManager.verifyPasscode(this, passcode.toString())) {
                // Old passcode is correct, proceed to enter new passcode
                isChangingOldPasscode = false
                resetPasscode()
                updateUI()
            } else {
                // Old passcode is incorrect
                showError("Incorrect passcode. Please try again.")
                resetPasscode()
            }
        } else if (isConfirmingPasscode) {
            // Check if confirmation matches new passcode
            if (passcode.toString() == confirmPasscode) {
                // Save the new passcode
                SecurityManager.savePasscode(this, passcode.toString())
                
                setResult(RESULT_OK)
                finish()
            } else {
                // Passcodes don't match
                showError("Passcodes don't match. Please try again.")
                
                // Reset to new passcode entry
                isConfirmingPasscode = false
                resetPasscode()
                updateUI()
            }
        } else {
            // First entry of new passcode, save it and ask for confirmation
            confirmPasscode = passcode.toString()
            isConfirmingPasscode = true
            resetPasscode()
            updateUI()
        }
    }
    
    private fun resetPasscode() {
        passcode.clear()
        updatePasscodeDisplay()
    }
    
    private fun showError(message: String) {
        textViewError.text = message
        textViewError.visibility = View.VISIBLE
        
        // Clear error after a delay
        Handler(Looper.getMainLooper()).postDelayed({
            textViewError.visibility = View.INVISIBLE
        }, 3000)
    }
    
    override fun onBackPressed() {
        // If we're in verification mode, don't allow back press
        if (currentMode == MODE_VERIFY) {
            // Do nothing, don't allow user to bypass verification
        } else {
            super.onBackPressed()
        }
    }
} 