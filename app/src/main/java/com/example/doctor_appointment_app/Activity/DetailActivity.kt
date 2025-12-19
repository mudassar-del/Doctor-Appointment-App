package com.example.doctor_appointment_app.Activity

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import com.bumptech.glide.Glide
import com.example.doctor_appointment_app.Domain.DoctorModel
import com.example.doctor_appointment_app.databinding.ActivityDetailBinding

class DetailActivity : BaseActivity() {
    private lateinit var binding: ActivityDetailBinding
    private lateinit var item: DoctorModel
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding= ActivityDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        getBundle()


    }

    private fun getBundle() {
         item=intent.getParcelableExtra("object")!!
        binding.apply {
            titleTxt.text=item.Name
            specialTxt.text=item.Special
            patiensTxt.text=item.Patiens
            bioTxt.text=item.Biography
            addressTxt.text=item.Address
            experienceTxt.text=item.Expriense.toString()+" Years"
            ratingTxt.text="${item.Rating}"
            backBtn.setOnClickListener {
                finish()
            }
            websiteBtn.setOnClickListener{
                val i = Intent(Intent.ACTION_VIEW)
                i.setData(Uri.parse(item.Site))
                startActivity(i)
            }
            messageBtn.setOnClickListener{
                val uri = Uri.parse("smsto:${item.Mobile}")
                val intent = Intent(Intent.ACTION_SENDTO,uri)
                intent.putExtra("sms_body","the SMS text")
                startActivity(intent)
            }

            callBtn.setOnClickListener {
                val uri="tel:"+item.Mobile.trim()
                val intent= Intent(Intent.ACTION_DIAL,Uri.parse(uri))
                startActivity(intent)
            }

            directionBtn.setOnClickListener {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(item.Location))
                startActivity(intent)
            }
            shareBtn.setOnClickListener {
                val intent = Intent(Intent.ACTION_SEND)
                intent.setType("text/plain")
                intent.putExtra(Intent.EXTRA_SUBJECT,item.Name)
                intent.putExtra(Intent.EXTRA_TEXT,item.Name+" "+item.Address+" "+item.Mobile)
                startActivity(Intent.createChooser(intent,"Chose one"))


            }
            // In DetailActivity.kt, inside getBundle() method
            makeBtn.setOnClickListener {
                val intent = Intent(this@DetailActivity, BookingActivity::class.java)
                intent.putExtra("doctor", item) // item is your DoctorModel
                startActivity(intent)
            }

            Glide.with(this@DetailActivity)
                .load(item.Picture)
                .into(img)
        }
    }
}