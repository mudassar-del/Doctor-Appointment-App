package com.example.doctor_appointment_app.Activity

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.doctor_appointment_app.Adapter.CategoryAdapter
import com.example.doctor_appointment_app.Adapter.TopDoctorAdapter
import com.example.doctor_appointment_app.ViewModel.MainViewModel
import com.example.doctor_appointment_app.databinding.ActivityMainBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class MainActivity : BaseActivity() {
    companion object {
        private const val TAG = "MainActivity"
    }

    private var _binding: ActivityMainBinding? = null
    private val binding get() = _binding!!
    private val viewModel = MainViewModel()
    private lateinit var auth: FirebaseAuth
    private var userNameListener: ValueEventListener? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate started")

        auth = FirebaseAuth.getInstance()
        val currentUser = auth.currentUser

        // Since IntroActivity validated token, this should never be null
        // But add safety net
        if (currentUser == null) {
            Log.e(TAG, "No user - redirecting to login")
            navigateToLogin()
            return
        }

        _binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize Firebase DB
        val dbRef = FirebaseDatabase.getInstance().getReference("users")
        loadUserName(dbRef, currentUser.uid)
        initCategory()
        initTopDoctors()
        // Add this in your onCreate method after binding initialization
        binding.accountLayout.setOnClickListener {
            startActivity(Intent(this@MainActivity, ProfileActivity::class.java))
        }
    }

    private fun loadUserName(dbRef: DatabaseReference, uid: String) {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (isFinishing || isDestroyed || _binding == null) return

                val name = snapshot.child("name")
                    .getValue(String::class.java)
                    ?.takeIf { it.isNotBlank() }
                    ?: "User"

                binding.textView2.text = "Hi $name"
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "Database read cancelled: ${error.message}")
                if (isFinishing || isDestroyed || _binding == null) return
                binding.textView2.text = "Hi User"
            }
        }

        userNameListener = listener
        dbRef.child(uid).addListenerForSingleValueEvent(listener)
    }

    private fun initTopDoctors() {
        binding.apply {
            progressBarTopDoctor.visibility = View.VISIBLE
            viewModel.doctors.observe(this@MainActivity) { doctors ->
                if (isFinishing || isDestroyed) return@observe
                recyclerViewTopDoctor.layoutManager = LinearLayoutManager(
                    this@MainActivity,
                    LinearLayoutManager.HORIZONTAL,
                    false
                )
                recyclerViewTopDoctor.adapter = TopDoctorAdapter(doctors)
                progressBarTopDoctor.visibility = View.GONE
            }
            viewModel.loadDoctor()
            doctorListTxt.setOnClickListener {
                if (!isFinishing) {
                    startActivity(Intent(this@MainActivity, TopDoctorsActivity::class.java))
                }
            }
        }
    }

    private fun initCategory() {
        binding.progressBarCategory.visibility = View.VISIBLE
        viewModel.category.observe(this) { categories ->
            if (isFinishing || isDestroyed || _binding == null) return@observe
            binding.viewCategory.layoutManager = LinearLayoutManager(
                this@MainActivity,
                LinearLayoutManager.HORIZONTAL,
                false
            )
            binding.viewCategory.adapter = CategoryAdapter(categories)
            binding.progressBarCategory.visibility = View.GONE
        }
        viewModel.loadCategory()
    }

    private fun navigateToLogin() {
        startActivity(Intent(this, LoginActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        })
        finish()
    }

    override fun onDestroy() {
        userNameListener?.let {
            FirebaseDatabase.getInstance().getReference("users").removeEventListener(it)
        }
        _binding = null
        super.onDestroy()
    }
}