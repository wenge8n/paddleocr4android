package com.equationl.paddleocr4android.app

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.util.Size
import android.view.MenuItem
import android.widget.Toast
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.equationl.paddleocr4android.CpuPowerMode
import com.equationl.paddleocr4android.OCR
import com.equationl.paddleocr4android.OcrConfig
import com.equationl.paddleocr4android.app.databinding.ActivityCameraBinding
import com.equationl.paddleocr4android.bean.OcrResult
import com.equationl.paddleocr4android.callback.OcrInitCallback
import com.equationl.paddleocr4android.callback.OcrRunCallback
import java.net.URLEncoder
import java.nio.ByteBuffer
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

typealias BitmapListener = (bitmap: Bitmap) -> Unit

class CameraActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCameraBinding

    private lateinit var ocr: OCR
    private lateinit var cameraExecutor: ExecutorService
    private var isOCRRunning = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCameraBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        initOCR()

        cameraExecutor = Executors.newSingleThreadExecutor()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when(item.itemId) {
            android.R.id.home -> finish()
        }
        return super.onOptionsItemSelected(item)
    }

    private fun initOCR() {
        ocr = OCR(this)

        // 配置
        val config = OcrConfig()
        //config.labelPath = null

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
                requestPermissions()
                Log.i(TAG, "onSuccess: 初始化成功")
            }

            override fun onFail(e: Throwable) {
                Toast.makeText(this@CameraActivity, e.message, Toast.LENGTH_SHORT).show()
                Log.e(TAG, "onFail: 初始化失败", e)
                finish()
            }
        })
    }

    private fun requestPermissions() {
        // Request camera permissions
        if (allPermissionsGranted()) {
            startCamera()
        } else {
            ActivityCompat.requestPermissions(
                this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS)
        }
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            // Used to bind the lifecycle of cameras to the lifecycle owner
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

            val resolution = Size(480, 640)

            // Preview
            val preview = Preview.Builder()
                .setTargetResolution(resolution)
                .build()
                .also {
                    it.setSurfaceProvider(binding.viewFinder.surfaceProvider)
                }

            val imageAnalyzer = ImageAnalysis.Builder()
                .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
                .setTargetResolution(resolution)
                .build()
                .also {
                    it.setAnalyzer(cameraExecutor, BitmapAnalyzer { bitmap ->
                        if (!isOCRRunning) {
                            // Crop preview area
                            val cropHeight = if (bitmap.width < binding.viewFinder.width) {
                                // If preview area is larger than analysing image
                                val ratio = bitmap.width.toFloat() / binding.viewFinder.width.toFloat()
                                binding.viewFinder.height.toFloat() * ratio
                            } else {
                                // If preview area is smaller than analysing image
                                val prc = 100 - (binding.viewFinder.width.toFloat() / (bitmap.width.toFloat() / 100f))
                                binding.viewFinder.height + ((binding.viewFinder.height.toFloat() / 100f) * prc)
                            }
                            val cropTop = (bitmap.height / 2) - (cropHeight / 2)
                            val cropped = Bitmap.createBitmap(bitmap, 0, cropTop.toInt(), bitmap.width, cropHeight.toInt())

                            // Run OCR
                            runOCR(cropped)
                        }
                    })
                }

            // Select back camera as a default
            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                // Unbind use cases before rebinding
                cameraProvider.unbindAll()

                // Bind use cases to camera
                cameraProvider.bindToLifecycle(
                    this, cameraSelector, preview, imageAnalyzer)

            } catch(exc: Exception) {
                Log.e(TAG, "Use case binding failed", exc)
            }

        }, ContextCompat.getMainExecutor(this))
    }

    private fun runOCR(bitmap: Bitmap) {
        isOCRRunning = true

        ocr.run(bitmap, object : OcrRunCallback {
            override fun onSuccess(result: OcrResult) {
                val simpleText = result.simpleText
                val imgWithBox = result.imgWithBox
                val inferenceTime = result.inferenceTime
                val outputRawResult = result.outputRawResult

                var text = "识别文字=\n$simpleText\n识别时间=$inferenceTime ms\n更多信息=\n"

                val wordLabels = ocr.getWordLabels()
                outputRawResult.forEachIndexed { index, ocrResultModel ->
                    // 文字索引（crResultModel.wordIndex）对应的文字可以从字典（wordLabels） 中获取
                    ocrResultModel.wordIndex.forEach {
                        Log.i(TAG, "onSuccess: text = ${wordLabels[it]}")
                    }
                    // 文字方向 ocrResultModel.clsLabel 可能为 "0" 或 "180"
                    text += "$index: 文字方向：${ocrResultModel.clsLabel}；文字方向置信度：${ocrResultModel.clsConfidence}；识别置信度 ${ocrResultModel.confidence}；文字索引位置 ${ocrResultModel.wordIndex}；文字位置：${ocrResultModel.points}\n"
                }

                binding.textView.text = simpleText

                try {
                    val mrz = parseMRZ(simpleText)
                    val intent = Intent(this@CameraActivity, ResultActivity::class.java)
                    intent.putExtra("mrz", mrz)
                    startActivity(intent)
                    finish()
                } catch (e: Exception) {
                    Log.d(TAG, e.toString())
                }

                isOCRRunning = false
            }

            override fun onFail(e: Throwable) {
                Log.e(TAG, "onFail: 识别失败！", e)

                isOCRRunning = false
            }
        })
    }

    private fun parseMRZ(text: String): String {
        var result = text
            .replace(Regex("^[^PIACV]*"), "") // Remove everything before P, I, A or C
            .replace(Regex("[ \\t\\r]+"), "") // Remove any white space
            .replace(Regex("\\n+"), "\n") // Remove extra new lines
            .replace("«", "<")
            .replace("<c<", "<<<")
            .replace("<e<", "<<<")
            .replace("<E<", "<<<") // Good idea? Maybe not.
            .replace("<K<", "<<<") // Good idea? Maybe not.
            .replace("<S<", "<<<") // Good idea? Maybe not.
            .replace("<C<", "<<<") // Good idea? Maybe not.
            .replace("<¢<", "<<<")
            .replace("<(<", "<<<")
            .replace("<{<", "<<<")
            .replace("<[<", "<<<")
            .replace(Regex("^P[KC]"), "P<")
            .replace(Regex("[^A-Z0-9<\\n]"), "") // Remove any other char
            .trim()

        if (result.contains("<") && (
                    result.startsWith("P") ||
                            result.startsWith("I") ||
                            result.startsWith("A") ||
                            result.startsWith("C") ||
                            result.startsWith("V"))
        ) {
            when (result.filter{ it == '\n' }.count()) {
                1 -> {
                    if (result.length > 89) {
                        result = result.slice(IntRange(0, 88))
                    }
                }
                2 -> {
                    if (result.length > 92) {
                        result = result.slice(IntRange(0, 91))
                    }
                }
                else -> throw IllegalArgumentException("Invalid MRZ string. Wrong number of lines.")
            }
        } else {
            Log.d(TAG, "Error = [${URLEncoder.encode(result, "UTF-8").replace("%3C", "<").replace("%0A", "↩")}]")
            throw IllegalArgumentException("Invalid MRZ string. No '<' or 'P', 'I', 'A', 'C', 'V' detected.")
        }

        return result
    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(
            baseContext, it) == PackageManager.PERMISSION_GRANTED
    }

    override fun onDestroy() {
        super.onDestroy()
        ocr.releaseModel()
        cameraExecutor.shutdown()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String>, grantResults:
        IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                startCamera()
            } else {
                Toast.makeText(this,
                    "Permissions not granted by the user.",
                    Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

    companion object {
        private val TAG = this::class.simpleName
        private const val REQUEST_CODE_PERMISSIONS = 10
        private val REQUIRED_PERMISSIONS =
            mutableListOf (
                Manifest.permission.CAMERA
            ).apply {
                if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
                    add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                }
            }.toTypedArray()
    }

    private class BitmapAnalyzer(private val listener: BitmapListener? = null) : ImageAnalysis.Analyzer {

        private fun ByteBuffer.toByteArray(): ByteArray {
            rewind()    // Rewind the buffer to zero
            val data = ByteArray(remaining())
            get(data)   // Copy the buffer into a byte array
            return data // Return the byte array
        }

        override fun analyze(image: ImageProxy) {
            val bitmap = Bitmap.createBitmap(image.width, image.height, Bitmap.Config.ARGB_8888)
            image.use { bitmap.copyPixelsFromBuffer(image.planes[0].buffer) }
            val rotated = bitmap.rotate(image.imageInfo.rotationDegrees.toFloat())
            listener?.let { it(rotated) }
            image.close()
        }
    }
}