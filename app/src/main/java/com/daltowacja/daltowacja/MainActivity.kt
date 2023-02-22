package com.daltowacja.daltowacja

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.graphics.*
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.daltowacja.daltowacja.databinding.ActivityMainBinding
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.core.Preview
import androidx.camera.core.CameraSelector
import android.util.Log
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.Toolbar
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.view.PreviewView
import kotlin.math.sqrt

typealias LumaListener = (luma: Double) -> Unit

class MainActivity : AppCompatActivity() {
    private lateinit var viewBinding: ActivityMainBinding

    private lateinit var cameraExecutor: ExecutorService

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewBinding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(viewBinding.root)

        // Set custom toolbar
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        // Disable application name in the toolbar
        supportActionBar?.setDisplayShowTitleEnabled(false)

        val menuButton = findViewById<ImageView>(R.id.menuButton)
        val menuLayout = findViewById<RelativeLayout>(R.id.menuLayout)

        val infoButton = findViewById<ImageView>(R.id.infoButton)

        val previewView = findViewById<PreviewView>(R.id.viewFinder)
        val frozenFrame = findViewById<ImageView>(R.id.frozenFrame)

        val frozenButton = findViewById<Button>(R.id.freezeButton)
        val analyzeColorButton = findViewById<Button>(R.id.analyzeColorButton)

        val colorName = findViewById<TextView>(R.id.colorName)
        val colorDescription = findViewById<TextView>(R.id.colorDescription)
        val coloredRectangle = findViewById<RelativeLayout>(R.id.colorNameLayout)

        val pointerWhite = findViewById<ImageView>(R.id.pointerWhite)
        val pointerBlack = findViewById<ImageView>(R.id.pointerBlack)
        val pointerSizeSlider = findViewById<SeekBar>(R.id.pointerSizeSlider)

        if (allPermissionsGranted()) {
            startCamera()
            setPreviewViewFreezeOnClick(previewView, frozenFrame, frozenButton)
            captureFrame(previewView, colorName, colorDescription, coloredRectangle, analyzeColorButton)
            changePointerSize(pointerSizeSlider, pointerWhite, pointerBlack)
            toggleMenuOrInfoOnClick(menuButton, infoButton, menuLayout)
        } else {
            ActivityCompat.requestPermissions(
                this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS)
        }

        cameraExecutor = Executors.newSingleThreadExecutor()
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            // Used to bind the lifecycle of cameras to the lifecycle owner
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

            // Select back camera as a default
            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            // Preview
            val preview = Preview.Builder()
                .build()
                .also {
                    it.setSurfaceProvider(viewBinding.viewFinder.surfaceProvider)
                }

            // Pointers
            val pointerBlack = findViewById<View>(R.id.pointerBlack)
            val pointerWhite = findViewById<View>(R.id.pointerWhite)

            // Image analysis use case - luminance analyzer for pointer
            val imageAnalyzer = ImageAnalysis.Builder()
                .build()
                .also {
                    it.setAnalyzer(cameraExecutor, LuminosityAnalyzer { luma ->
                        // Log.d(TAG, "Avg luminosity: $luma")
                        /*
                        These instructions make sure that the visibility parameter
                        is changed only once and not continuously
                        */
                        runOnUiThread {
                            if (luma >= 100 && pointerBlack.visibility == View.GONE) {
                                pointerWhite.visibility = View.GONE
                                pointerBlack.visibility = View.VISIBLE
                            } else {
                                if (luma < 100 && pointerWhite.visibility == View.GONE) {
                                    pointerBlack.visibility = View.GONE
                                    pointerWhite.visibility = View.VISIBLE
                                }
                            }
                        }
                    })
                }

            try {
                // Unbind use cases before rebinding
                cameraProvider.unbindAll()

                // Bind use case to camera
                cameraProvider.bindToLifecycle(
                    this, cameraSelector, preview, imageAnalyzer)
            } catch(exc: Exception) {
                Log.e(TAG, "Use case binding failed", exc)
            }

        }, ContextCompat.getMainExecutor(this))
    }

    // Check if all permissions have been granted
    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(
            baseContext, it) == PackageManager.PERMISSION_GRANTED
    }

    // Check results of permissions request
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

    private fun getEuclideanDistance(rgb1: IntArray, rgb2: IntArray): Double {
        val rDiff = (rgb1[0] - rgb2[0]).toDouble()
        val gDiff = (rgb1[1] - rgb2[1]).toDouble()
        val bDiff = (rgb1[2] - rgb2[2]).toDouble()
        return sqrt(rDiff * rDiff + gDiff * gDiff + bDiff * bDiff)
    }

    private fun getClosestColorNameAndDescription(rgb: IntArray): List<String> {
        var minDistance = Double.MAX_VALUE
        var nameDesc = listOf<String>()

        for ((colorRgb, colorInfo) in advancedColors) {
            val distance = getEuclideanDistance(rgb, colorRgb)
            if (distance < minDistance) {
                minDistance = distance
                nameDesc = colorInfo
            }
        }

        return nameDesc
    }

    @SuppressLint("SetTextI18n")
    private fun captureFrame(previewView: PreviewView, name: TextView, description: TextView, coloredRectangle: RelativeLayout, button: Button) {
        button.setOnClickListener {
            // Capture the current frame as a Bitmap
            val image = previewView.bitmap

            val middleX = image!!.width / 2
            val middleY = image.height / 2

            val sliderPosition = findViewById<SeekBar>(R.id.pointerSizeSlider)
            val searchedAreaSize = (sliderPosition.progress/2)+1 // Add ceil function instead of +1 for 0 value

            // !!CHECK IF CORRECT!! Define a rectangle that covers the middle area, size dynamically set by Slidebar
            val rect = Rect(middleX - searchedAreaSize, middleY - searchedAreaSize, middleX + searchedAreaSize, middleY + searchedAreaSize)

            // Calculate the average color of the middle area
            var red = 0
            var green = 0
            var blue = 0
            var count = 0

            for (y in rect.top until rect.bottom) {
                for (x in rect.left until rect.right) {
                    val pixel = image.getPixel(x, y)
                    red += Color.red(pixel)
                    green += Color.green(pixel)
                    blue += Color.blue(pixel)
                    count++
                }
            }

            if (count > 0) {
                red /= count
                green /= count
                blue /= count
            }

            // Set the text to the average color
            red = red and 0xFF
            green = green and 0xFF
            blue = blue and 0xFF

            val nameDesc = getClosestColorNameAndDescription(intArrayOf(red, green, blue))
            name.text = nameDesc[0]
            description.text = nameDesc[1]

            if((red+green+blue)>383) {
                name.setTextColor(Color.BLACK)
                description.setTextColor(Color.BLACK)
            } else {
                name.setTextColor(Color.WHITE)
                description.setTextColor(Color.WHITE)
            }

            //translates rgb to hsv then to hsv int and changes color of colorRectangle
            coloredRectangle.setBackgroundColor(Color.HSVToColor(rgbToHsv(red, green, blue)))
        }
    }

    private fun rgbToHsv(red: Int, green: Int, blue: Int): FloatArray {
        val r = red / 255.0f
        val g = green / 255.0f
        val b = blue / 255.0f
        val cmax = maxOf(r, g, b)
        val cmin = minOf(r, g, b)
        val delta = cmax - cmin

        var h: Float = if (delta == 0.0f) {
            0.0f
        } else if (cmax == r) {
            ((g - b) / delta) % 6.0f
        } else if (cmax == g) {
            (b - r) / delta + 2.0f
        } else {
            (r - g) / delta + 4.0f
        }

        h = (h / 6.0f) * 360.0f

        val s: Float = if (cmax == 0.0f) {
            0.0f
        } else {
            delta / cmax
        }

        val v: Float = cmax

        return floatArrayOf(h, s, v)
    }

    // frozenButton functionality
    @SuppressLint("SetTextI18n")
    private fun setPreviewViewFreezeOnClick(previewView: PreviewView, frozenFrame: ImageView, button: Button) {
        button.setOnClickListener {
            if (previewView.visibility == View.VISIBLE) {
                button.text = "unfreeze"
                val bitmap = previewView.bitmap
                frozenFrame.setImageBitmap(bitmap)
                previewView.visibility = View.GONE
                frozenFrame.visibility = View.VISIBLE
            } else {
                button.text = "freeze"
                previewView.visibility = View.VISIBLE
                frozenFrame.visibility = View.GONE
            }
        }
    }

    private fun toggleMenuOrInfoOnClick(menuButton: ImageView, infoButton: ImageView, menuLayout: RelativeLayout) {
        menuButton.setOnClickListener {
            if (menuLayout.visibility == View.VISIBLE) {
                menuButton.setImageResource(R.drawable.menu_white)
                menuLayout.visibility = View.GONE
            } else {
                menuButton.setImageResource(R.drawable.menu_selected)
                menuLayout.visibility = View.VISIBLE
            }
        }
        infoButton.setOnClickListener {
            val builder = AlertDialog.Builder(this, R.style.CustomAlertDialogStyle)
            builder.setView(R.layout.info_dialog)
            builder.setCancelable(true)
            builder.setPositiveButton(android.R.string.ok, null)
            builder.create().show()
        }
    }

    private fun changePointerSize(pointerSizeSlider: SeekBar, pointerWhite: ImageView, pointerBlack: ImageView) {
        pointerSizeSlider.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                // This method will be called whenever the slider value changes
                // You can use the "progress" parameter to get the current slider value
                // and update your UI or perform any other actions as needed
                val paramsW = pointerWhite.layoutParams
                paramsW.width = pointerSizeSlider.progress+12 // Set the new width in pixels
                paramsW.height = pointerSizeSlider.progress+12
                pointerWhite.layoutParams = paramsW
                val paramsB = pointerBlack.layoutParams
                paramsB.width = pointerSizeSlider.progress+12 // Set the new width in pixels
                paramsB.height = pointerSizeSlider.progress+12
                pointerBlack.layoutParams = paramsB
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
                // This method will be called when the user starts dragging the slider
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                // This method will be called when the user stops dragging the slider
            }
        })
    }

    // Called when an activity is about to be destroyed
    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }

    // Analyzer of the average preview luminosity
    private class LuminosityAnalyzer(private val listener: LumaListener) : ImageAnalysis.Analyzer {
        override fun analyze(image: ImageProxy) {
            val buffer = image.planes[0].buffer
            val pixelStride = image.planes[0].pixelStride
            val rowStride = image.planes[0].rowStride
            val imageWidth = image.width
            val imageHeight = image.height

            val centerX = imageWidth / 2
            val centerY = imageHeight / 2

            /*
            Calculate the size of the center square:
            it will be 150x150 px or one of the center
            coordinates (if it is smaller)
            */
            val squareSize = minOf(centerX, centerY, 150)

            // Calculate the starting position of the center square
            val startX = centerX - squareSize / 2
            val startY = centerY - squareSize / 2

            // Calculate the end position of the center square
            val endX = startX + squareSize
            val endY = startY + squareSize

            var totalLuma = 0.0
            var pixelCount = 0

            // Iterate through the pixels in the center square and calculate the average luminosity
            for (y in startY until endY) {
                for (x in startX until endX) {
                    val pixelOffset = y * rowStride + x * pixelStride
                    val pixelValue = buffer.get(pixelOffset).toInt() and 0xFF
                    totalLuma += pixelValue
                    pixelCount++
                }
            }

            // Calculate the average luminosity of the center square
            val avgLuma = totalLuma / pixelCount

            // Call the listener with the average luminosity value
            listener(avgLuma)

            image.close()
        }
    }

    // Basic static variables
    companion object {
        private const val TAG = "Daltowacja"
        private const val REQUEST_CODE_PERMISSIONS = 10
        private val REQUIRED_PERMISSIONS = mutableListOf(
            Manifest.permission.CAMERA).apply{}.toTypedArray()
    }
}
