package com.example.doctor_appointment_app.Activity

import android.os.Bundle
import android.widget.Toast
import com.example.doctor_appointment_app.Domain.DoctorModel
import com.example.doctor_appointment_app.databinding.ActivityBookingBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.text.SimpleDateFormat
import java.util.*

class BookingActivity : BaseActivity() {
    private lateinit var binding: ActivityBookingBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var dbRef: DatabaseReference
    private lateinit var doctor: DoctorModel
    private var userListener: ValueEventListener? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBookingBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize Firebase
        auth = FirebaseAuth.getInstance()
        dbRef = FirebaseDatabase.getInstance().getReference("users")

        // Get doctor data from intent
        doctor = intent.getParcelableExtra("doctor") ?: run {
            Toast.makeText(this, "Doctor data not found", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // Check if user is logged in
        val currentUser = auth.currentUser
        if (currentUser == null) {
            navigateToLogin()
            return
        }

        // Setup UI with doctor info
        setupDoctorInfo(doctor)

        // Load user data
        loadUserData(currentUser.uid)

        // Setup book appointment button
        binding.makeBtn.setOnClickListener {
            makeAppointment(currentUser.uid)
        }

        // Back button
        binding.backBtn.setOnClickListener {
            finish()
        }
    }

    private fun setupDoctorInfo(doctor: DoctorModel) {
        binding.doctorNameTxt.text = doctor.Name
        binding.doctorSpecialTxt.text = doctor.Special
        binding.doctorAddressTxt.text = doctor.Address
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
                    // Fallback to auth data
                    binding.userNameTxt.text = auth.currentUser?.displayName ?: "User"
                    binding.userEmailTxt.text = auth.currentUser?.email ?: "No email"
                }
            }

            override fun onCancelled(error: DatabaseError) {
                binding.progressBar.visibility = android.view.View.GONE
                Toast.makeText(this@BookingActivity, "Failed to load user data", Toast.LENGTH_SHORT).show()

                binding.userNameTxt.text = auth.currentUser?.displayName ?: "User"
                binding.userEmailTxt.text = auth.currentUser?.email ?: "No email"
            }
        }

        dbRef.child(uid).addListenerForSingleValueEvent(userListener!!)
    }

    private fun makeAppointment(userId: String) {
        // Get appointment details
        val appointmentDate = binding.dateEdt.text.toString().trim()
        val appointmentTime = binding.timeEdt.text.toString().trim()
        val notes = binding.notesEdt.text.toString().trim()

        // Validate inputs
        if (appointmentDate.isEmpty() || appointmentTime.isEmpty()) {
            Toast.makeText(this, "Please select date and time", Toast.LENGTH_SHORT).show()
            return
        }

        // Create appointment object
        val appointmentId = dbRef.push().key ?: UUID.randomUUID().toString()
        val currentTime = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())

        // ✅ FIX: Convert doctor.Id (Int) to String for Firebase
        val doctorIdString = doctor.Id.toString()

        val appointmentData = hashMapOf<String, Any>(
            "appointmentId" to appointmentId,
            "userId" to userId,
            "doctorId" to doctorIdString,        // ✅ Now it's a String
            "doctorName" to doctor.Name,
            "doctorSpecial" to doctor.Special,
            "doctorAddress" to doctor.Address,
            "appointmentDate" to appointmentDate,
            "appointmentTime" to appointmentTime,
            "notes" to notes,
            "bookingTime" to currentTime,
            "status" to "pending" // pending, confirmed, cancelled
        )

        binding.makeBtn.isEnabled = false
        binding.progressBar.visibility = android.view.View.VISIBLE

        // Save to user's bookings
        dbRef.child(userId).child("bookings").child(appointmentId).setValue(appointmentData)
            .addOnSuccessListener {
                // Also save to doctor's appointments (optional)
                // ✅ Use the String version for Firebase path
                dbRef.child("doctors").child(doctorIdString).child("appointments").child(appointmentId).setValue(appointmentData)
                    .addOnSuccessListener {
                        binding.progressBar.visibility = android.view.View.GONE
                        Toast.makeText(this, "Appointment booked successfully!", Toast.LENGTH_LONG).show()
                        finish()
                    }
                    .addOnFailureListener { e ->
                        binding.progressBar.visibility = android.view.View.GONE
                        binding.makeBtn.isEnabled = true
                        Toast.makeText(this, "Appointment booked, but failed to update doctor's schedule", Toast.LENGTH_SHORT).show()
                        finish()
                    }
            }
            .addOnFailureListener { e ->
                binding.progressBar.visibility = android.view.View.GONE
                binding.makeBtn.isEnabled = true
                Toast.makeText(this, "Failed to book appointment: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }

    private fun navigateToLogin() {
        startActivity(android.content.Intent(this, LoginActivity::class.java).apply {
            flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK or android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK
        })
        finish()
    }

    override fun onDestroy() {
        userListener?.let { dbRef.removeEventListener(it) }
        super.onDestroy()
    }
}