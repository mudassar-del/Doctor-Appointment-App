package com.example.doctor_appointment_app.Activity

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import com.example.doctor_appointment_app.databinding.ActivityIntroBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser

class IntroActivity : BaseActivity() {
    companion object {
        private const val TAG = "IntroActivity"
        private const val SPLASH_DELAY = 2000L
    }

    private var _binding: ActivityIntroBinding? = null
    private val binding get() = _binding!!
    private lateinit var auth: FirebaseAuth
    private val handler = Handler(Looper.getMainLooper())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate started")

        try {
            _binding = ActivityIntroBinding.inflate(layoutInflater)
            setContentView(binding.root)
            auth = FirebaseAuth.getInstance()

            // ✅ CRITICAL FIX: Validate token before proceeding
            val currentUser = auth.currentUser
            if (currentUser != null) {
                Log.d(TAG, "User exists, validating token...")
                validateAndProceed(currentUser)
            } else {
                Log.d(TAG, "No user logged in")
                showStartButton()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in onCreate", e)
            showStartButton()
        }
    }

    private fun validateAndProceed(user: FirebaseUser) {
        // ✅ Refresh the token to ensure it's valid for database operations
        user.getIdToken(true)
            .addOnCompleteListener { task ->
                if (task.isSuccessful && !isFinishing && !isDestroyed) {
                    Log.d(TAG, "Token validated successfully")
                    showSplashAndNavigate()
                } else {
                    Log.e(TAG, "Token validation failed, showing login")
                    showStartButton()
                }
            }
            .addOnFailureListener { exception ->
                Log.e(TAG, "Token refresh failed", exception)
                showStartButton()
            }
    }

    private fun showSplashAndNavigate() {
        binding.startBtn.visibility = android.view.View.GONE
        handler.postDelayed({
            if (!isFinishing && !isDestroyed) {
                val intent = Intent(this, MainActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                finish()
            }
        }, SPLASH_DELAY)
    }

    private fun showStartButton() {
        if (!isFinishing && !isDestroyed) {
            binding.startBtn.visibility = android.view.View.VISIBLE
            binding.startBtn.setOnClickListener {
                if (!isFinishing && !isDestroyed) {
                    startActivity(Intent(this, LoginActivity::class.java))
                    finish()
                }
            }
        }
    }

    override fun onDestroy() {
        _binding = null
        super.onDestroy()
    }
}