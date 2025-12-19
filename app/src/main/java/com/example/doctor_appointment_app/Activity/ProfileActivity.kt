package com.example.doctor_appointment_app.Activity

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import com.example.doctor_appointment_app.databinding.ActivityProfileBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class ProfileActivity : BaseActivity() {
    private lateinit var binding: ActivityProfileBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var dbRef: DatabaseReference
    private var userListener: ValueEventListener? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize Firebase
        auth = FirebaseAuth.getInstance()
        dbRef = FirebaseDatabase.getInstance().getReference("users")

        // Check if user is logged in
        val currentUser = auth.currentUser
        if (currentUser == null) {
            navigateToLogin()
            return
        }

        // Load user data
        loadUserData(currentUser.uid)

        // Setup logout button
        binding.logoutBtn.setOnClickListener {
            performLogout()
        }

        // Back button
        binding.backBtn.setOnClickListener {
            finish()
        }
    }

    private fun loadUserData(uid: String) {
        binding.progressBar.visibility = android.view.View.VISIBLE

        userListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                binding.progressBar.visibility = android.view.View.GONE

                if (snapshot.exists()) {
                    val name = snapshot.child("name").getValue(String::class.java) ?: "User"
                    val email = snapshot.child("email").getValue(String::class.java) ?: auth.currentUser?.email ?: "No email"

                    binding.userNameTxt.text = name
                    binding.userEmailTxt.text = email
                } else {
                    // Fallback to auth data if DB record doesn't exist
                    binding.userNameTxt.text = auth.currentUser?.displayName ?: "User"
                    binding.userEmailTxt.text = auth.currentUser?.email ?: "No email"
                }
            }

            override fun onCancelled(error: DatabaseError) {
                binding.progressBar.visibility = android.view.View.GONE
                Toast.makeText(this@ProfileActivity, "Failed to load profile: ${error.message}", Toast.LENGTH_SHORT).show()

                // Fallback to auth data
                binding.userNameTxt.text = auth.currentUser?.displayName ?: "User"
                binding.userEmailTxt.text = auth.currentUser?.email ?: "No email"
            }
        }

        dbRef.child(uid).addListenerForSingleValueEvent(userListener!!)
    }

    private fun performLogout() {
        auth.signOut()
        Toast.makeText(this, "Logged out successfully", Toast.LENGTH_SHORT).show()
        navigateToLogin()
    }

    private fun navigateToLogin() {
        startActivity(Intent(this, LoginActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        })
        finish()
    }

    override fun onDestroy() {
        userListener?.let { dbRef.removeEventListener(it) }
        super.onDestroy()
    }
}