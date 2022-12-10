package com.equationl.paddleocr4android.app

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.method.ScrollingMovementMethod
import android.util.Log
import android.widget.Toast
import com.equationl.paddleocr4android.CpuPowerMode
import com.equationl.paddleocr4android.OCR
import com.equationl.paddleocr4android.OcrConfig
import com.equationl.paddleocr4android.app.databinding.ActivitySampleBinding
import com.equationl.paddleocr4android.callback.OcrInitCallback

class SampleActivity : AppCompatActivity() {

    companion object {
        private val TAG = this::class.simpleName
    }

    private lateinit var binding: ActivitySampleBinding
    private lateinit var ocr: OCR

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySampleBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // binding.resultTextView.movementMethod = ScrollingMovementMethod()

        initOCR()
    }

    private fun initOCR() {
        binding.startButton.isEnabled = false

        ocr = OCR(this)

        val config = OcrConfig()

        config.modelPath = "models/ch_PP-OCRv2" // 不使用 "/" 开头的路径表示安装包中 assets 目录下的文件，例如当前表示 assets/models/ocr_v2_for_cpu
        //config.modelPath = "/sdcard/Android/data/com.equationl.paddleocr4android.app/files/models" // 使用 "/" 表示手机储存路径，测试时请将下载的三个模型放置于该目录下
        config.clsModelFilename = "cls.nb" // cls 模型文件名
        config.detModelFilename = "det_db.nb" // det 模型文件名
        config.recModelFilename = "rec_crnn.nb" // rec 模型文件名

        // 运行全部模型
        // 请根据需要配置，三项全开识别率最高；如果只开识别几乎无法正确识别，至少需要搭配检测或分类其中之一
        // 也可单独运行 检测模型 获取文本位置
        config.isRunDet = true
        config.isRunCls = true
        config.isRunRec = true

        // 使用所有核心运行
        config.cpuPowerMode = CpuPowerMode.LITE_POWER_FULL

        // 绘制文本位置
        config.isDrwwTextPositionBox = true

        // 1.同步初始化
        /*ocr.initModelSync(config).fold(
            {
                if (it) {
                    Log.i(TAG, "onCreate: init success")
                }
            },
            {
                it.printStackTrace()
            }
        )*/

        // 2.异步初始化
        ocr.initModel(config, object : OcrInitCallback {
            override fun onSuccess() {
                binding.startButton.isEnabled = true
                Log.i(TAG, "onSuccess: 初始化成功")
            }

            override fun onFail(e: Throwable) {
                Toast.makeText(this@SampleActivity, e.message, Toast.LENGTH_SHORT).show()
                Log.e(TAG, "onFail: 初始化失败", e)
            }
        })
    }
}