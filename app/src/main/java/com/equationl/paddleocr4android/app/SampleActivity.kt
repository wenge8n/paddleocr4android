package com.equationl.paddleocr4android.app

import android.graphics.BitmapFactory
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.method.ScrollingMovementMethod
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import com.equationl.paddleocr4android.CpuPowerMode
import com.equationl.paddleocr4android.OCR
import com.equationl.paddleocr4android.OcrConfig
import com.equationl.paddleocr4android.app.databinding.ActivitySampleBinding
import com.equationl.paddleocr4android.bean.OcrResult
import com.equationl.paddleocr4android.callback.OcrInitCallback
import com.equationl.paddleocr4android.callback.OcrRunCallback

class SampleActivity : AppCompatActivity(), View.OnClickListener {

    companion object {
        private val TAG = this::class.simpleName
    }

    private lateinit var binding: ActivitySampleBinding
    private lateinit var ocr: OCR
    private var imgResId: Int = R.drawable.test

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySampleBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        binding.img1Button.setOnClickListener(this)
        binding.img2Button.setOnClickListener(this)
        binding.img3Button.setOnClickListener(this)
        binding.img4Button.setOnClickListener(this)
        binding.startButton.setOnClickListener(this)
        // binding.resultTextView.movementMethod = ScrollingMovementMethod()

        initOCR()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when(item.itemId) {
            android.R.id.home -> finish()
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onDestroy() {
        super.onDestroy()
        // 释放
        ocr.releaseModel()
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

    private fun startIdentifying() {
        // 1.同步识别
        /*val bitmap = BitmapFactory.decodeResource(resources, R.drawable.test2)
        ocr.runSync(bitmap)

        val bitmap2 = BitmapFactory.decodeResource(resources, R.drawable.test3)
        ocr.runSync(bitmap2)*/

        // 2.异步识别
        binding.imageView.setImageResource(imgResId)
        binding.resultTextView.text = "Start identifying"
        val bitmap3 = BitmapFactory.decodeResource(resources, imgResId)
        ocr.run(bitmap3, object : OcrRunCallback {
            override fun onSuccess(result: OcrResult) {
                val simpleText = result.simpleText
                val imgWithBox = result.imgWithBox
                val inferenceTime = result.inferenceTime
                val outputRawResult = result.outputRawResult

                var text = "Recognized text=\n$simpleText\nRecognition time=$inferenceTime ms\nMore information=\n"

                val wordLabels = ocr.getWordLabels()
                outputRawResult.forEachIndexed { index, ocrResultModel ->
                    // 文字索引（crResultModel.wordIndex）对应的文字可以从字典（wordLabels） 中获取
                    ocrResultModel.wordIndex.forEach {
                        Log.i(TAG, "onSuccess: text = ${wordLabels[it]}")
                    }
                    // 文字方向 ocrResultModel.clsLabel 可能为 "0" 或 "180"
                    text += "$index: Character direction：${ocrResultModel.clsLabel}；Text Orientation Confidence：${ocrResultModel.clsConfidence}；Recognition confidence ${ocrResultModel.confidence}；Text index position ${ocrResultModel.wordIndex}；Text position：${ocrResultModel.points}\n"
                }

                binding.resultTextView.text = text
                binding.imageView.setImageBitmap(imgWithBox)
            }

            override fun onFail(e: Throwable) {
                binding.resultTextView.text = "Failed：$e"
                Log.e(TAG, "onFail: 识别失败！", e)
            }
        })
    }

    private fun selectImage(resId: Int) {
        imgResId = resId
        binding.imageView.setImageResource(resId)
        binding.resultTextView.text = ""
    }

    override fun onClick(view: View?) {
        when(view?.id) {
            R.id.img1_button -> {
                selectImage(R.drawable.test)
            }
            R.id.img2_button -> {
                selectImage(R.drawable.test2)
            }
            R.id.img3_button -> {
                selectImage(R.drawable.test3)
            }
            R.id.img4_button -> {
                selectImage(R.drawable.test4)
            }
            R.id.start_button -> {
                startIdentifying()
            }
        }
    }
}