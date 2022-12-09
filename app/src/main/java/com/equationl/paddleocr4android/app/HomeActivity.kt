package com.equationl.paddleocr4android.app

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import com.equationl.paddleocr4android.app.databinding.ActivityHomeBinding

class HomeActivity : AppCompatActivity(), View.OnClickListener {

    private lateinit var binding: ActivityHomeBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.sampleButton.setOnClickListener(this)
        binding.cameraButton.setOnClickListener(this)
    }

    override fun onClick(view: View?) {
        when(view?.id) {
            R.id.sample_button -> {
                val intent = Intent(this, SampleActivity::class.java)
                startActivity(intent)
            }
            R.id.camera_button -> {
                val intent = Intent(this, CameraActivity::class.java)
                startActivity(intent)
            }
        }
    }
}