package com.example.doctor_appointment_app.Activity

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import com.example.doctor_appointment_app.databinding.ActivitySignupBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase

class SignupActivity : BaseActivity() {

    private lateinit var binding: ActivitySignupBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var dbRef: DatabaseReference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySignupBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        dbRef = FirebaseDatabase.getInstance().getReference("users")

        // Safety: if already logged in, go to main
        if (auth.currentUser != null) {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
            return
        }

        binding.signupBtn.setOnClickListener { signup() }

        binding.loginRedirectTxt.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
            // Don't finish — allow back navigation
        }
    }

    private fun signup() {
        val name = binding.nameEdt.text.toString().trim()
        val email = binding.emailEdt.text.toString().trim()
        val password = binding.passwordEdt.text.toString().trim()
        val confirmPassword = binding.confirmPasswordEdt.text.toString().trim()

        if (name.isEmpty() || email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
            return
        }

        if (password != confirmPassword) {
            Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show()
            return
        }

        binding.progressBar.visibility = android.view.View.VISIBLE
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser
                    if (user != null) {
                        val userMap = mapOf(
                            "name" to name,
                            "email" to email
                        )
                        dbRef.child(user.uid).setValue(userMap)
                            .addOnSuccessListener {
                                binding.progressBar.visibility = android.view.View.GONE
                                Toast.makeText(this, "Account created successfully", Toast.LENGTH_SHORT).show()
                                startActivity(Intent(this, MainActivity::class.java))
                                finish() // ✅ Prevent back to signup
                            }
                            .addOnFailureListener { e ->
                                binding.progressBar.visibility = android.view.View.GONE
                                Toast.makeText(this, "Failed to save profile: ${e.message}", Toast.LENGTH_LONG).show()
                            }
                    }
                } else {
                    binding.progressBar.visibility = android.view.View.GONE
                    Toast.makeText(this, "Signup failed: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                }
            }
    }
}