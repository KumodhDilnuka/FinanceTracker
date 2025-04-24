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
        window.decorView.setBackgroundColor(android.graphics.Color.WHITE)
        
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
        
        // Set background colors for the entire activity
        window.decorView.setBackgroundColor(android.graphics.Color.WHITE)
        findViewById<View>(android.R.id.content).setBackgroundColor(android.graphics.Color.WHITE)
        
        // Remove overscroll effect
        viewPager.getChildAt(0).overScrollMode = View.OVER_SCROLL_NEVER
        
        // Force white background for the ViewPager and internal RecyclerView
        viewPager.setBackgroundColor(android.graphics.Color.WHITE)
        (viewPager.getChildAt(0) as? View)?.setBackgroundColor(android.graphics.Color.WHITE)
        
        // Apply background color with a delay to ensure it takes effect
        Handler(Looper.getMainLooper()).postDelayed({
            window.decorView.setBackgroundColor(android.graphics.Color.WHITE)
            viewPager.setBackgroundColor(android.graphics.Color.WHITE)
            (viewPager.getChildAt(0) as? View)?.setBackgroundColor(android.graphics.Color.WHITE)
            
            // Force redraw
            viewPager.invalidate()
            (viewPager.getChildAt(0) as? View)?.invalidate()
        }, 100)
        
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
                
                // Re-apply white background on page change
                window.decorView.setBackgroundColor(android.graphics.Color.WHITE)
                viewPager.setBackgroundColor(android.graphics.Color.WHITE)
                (viewPager.getChildAt(0) as? View)?.setBackgroundColor(android.graphics.Color.WHITE)
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
        // Re-apply white background when activity is resumed
        window.decorView.setBackgroundColor(android.graphics.Color.WHITE)
        viewPager.setBackgroundColor(android.graphics.Color.WHITE)
        (viewPager.getChildAt(0) as? View)?.setBackgroundColor(android.graphics.Color.WHITE)
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
            view.setBackgroundColor(android.graphics.Color.WHITE)
            return OnboardingViewHolder(view)
        }
        
        override fun onBindViewHolder(holder: OnboardingViewHolder, position: Int) {
            holder.bind(items[position])
            holder.itemView.setBackgroundColor(android.graphics.Color.WHITE)
        }
        
        override fun getItemCount(): Int = items.size
        
        inner class OnboardingViewHolder(view: View) : androidx.recyclerview.widget.RecyclerView.ViewHolder(view) {
            private val imageView = view.findViewById<ImageView>(R.id.imageViewOnboarding)
            private val titleText = view.findViewById<TextView>(R.id.textViewTitle)
            private val descriptionText = view.findViewById<TextView>(R.id.textViewDescription)
            
            init {
                view.setBackgroundColor(android.graphics.Color.WHITE)
            }
            
            fun bind(item: OnboardingItem) {
                imageView.setImageResource(item.image)
                titleText.text = item.title
                descriptionText.text = item.description
                
                // Apply white background to ensure it's white
                itemView.setBackgroundColor(android.graphics.Color.WHITE)
            }
        }
    }
    
    data class OnboardingItem(
        val image: Int,
        val title: String,
        val description: String
    )
} 