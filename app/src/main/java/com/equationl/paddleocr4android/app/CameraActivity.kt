package com.equationl.paddleocr4android.app

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.equationl.paddleocr4android.app.databinding.ActivityCameraBinding

class CameraActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCameraBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCameraBinding.inflate(layoutInflater)
        setContentView(binding.root)
    }
}