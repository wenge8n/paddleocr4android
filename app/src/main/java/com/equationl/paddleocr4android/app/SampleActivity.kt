package com.equationl.paddleocr4android.app

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.equationl.paddleocr4android.app.databinding.ActivitySampleBinding

class SampleActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySampleBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySampleBinding.inflate(layoutInflater)
        setContentView(binding.root)
    }
}