package com.example.financetracker.ui

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.viewpager2.widget.ViewPager2
import com.example.financetracker.R
import com.example.financetracker.data.PrefsManager
import com.google.android.material.button.MaterialButton

class OnboardingActivity : AppCompatActivity() {
    
    private lateinit var viewPager: ViewPager2
    private lateinit var buttonNext: MaterialButton
    private lateinit var buttonSkip: MaterialButton
    
    // Onboarding data
    private val onboardingItems = listOf(
        OnboardingItem(
            R.drawable.ic_onboarding_track,
            "Track Your Finances",
            "Easily record your income and expenses to manage your finances effectively"
        ),
        OnboardingItem(
            R.drawable.ic_onboarding_budget,
            "Set Budgets",
            "Create budgets to help you stay on track with your financial goals"
        ),
        OnboardingItem(
            R.drawable.ic_onboarding_insights,
            "View Insights",
            "Get detailed reports and visualizations of your spending habits"
        )
    )
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // If onboarding is completed, go to MainActivity
        if (PrefsManager.isOnboardingCompleted()) {
            navigateToMainActivity()
            return
        }
        
        setContentView(R.layout.activity_onboarding)
        
        // Initialize views
        viewPager = findViewById(R.id.viewPager)
        buttonNext = findViewById(R.id.buttonNext)
        buttonSkip = findViewById(R.id.buttonSkip)
        
        // Set up the adapter
        val adapter = OnboardingAdapter(onboardingItems)
        viewPager.adapter = adapter
        
        // Remove overscroll effect
        viewPager.getChildAt(0).overScrollMode = View.OVER_SCROLL_NEVER
        
        // Handle ViewPager page changes
        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                
                // Change button text on last page
                if (position == onboardingItems.size - 1) {
                    buttonNext.text = "Get Started"
                } else {
                    buttonNext.text = "Next"
                }
            }
        })
        
        // Set up button click listeners
        buttonNext.setOnClickListener {
            if (viewPager.currentItem + 1 < onboardingItems.size) {
                viewPager.currentItem += 1
            } else {
                completeOnboarding()
            }
        }
        
        buttonSkip.setOnClickListener {
            completeOnboarding()
        }
    }
    
    override fun onResume() {
        super.onResume()
        // No need to force background color here anymore
    }
    
    private fun completeOnboarding() {
        PrefsManager.setOnboardingCompleted(true)
        navigateToMainActivity()
    }
    
    private fun navigateToMainActivity() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
    
    // Adapter for the ViewPager
    inner class OnboardingAdapter(private val items: List<OnboardingItem>) : 
        androidx.recyclerview.widget.RecyclerView.Adapter<OnboardingAdapter.OnboardingViewHolder>() {
        
        override fun onCreateViewHolder(parent: android.view.ViewGroup, viewType: Int): OnboardingViewHolder {
            val view = android.view.LayoutInflater.from(parent.context).inflate(
                R.layout.item_onboarding_page, parent, false
            )
            return OnboardingViewHolder(view)
        }
        
        override fun onBindViewHolder(holder: OnboardingViewHolder, position: Int) {
            holder.bind(items[position])
        }
        
        override fun getItemCount(): Int = items.size
        
        inner class OnboardingViewHolder(view: View) : androidx.recyclerview.widget.RecyclerView.ViewHolder(view) {
            private val imageView = view.findViewById<ImageView>(R.id.imageViewOnboarding)
            private val titleText = view.findViewById<TextView>(R.id.textViewTitle)
            private val descriptionText = view.findViewById<TextView>(R.id.textViewDescription)
            
            fun bind(item: OnboardingItem) {
                imageView.setImageResource(item.image)
                titleText.text = item.title
                descriptionText.text = item.description
            }
        }
    }
    
    data class OnboardingItem(
        val image: Int,
        val title: String,
        val description: String
    )
} 