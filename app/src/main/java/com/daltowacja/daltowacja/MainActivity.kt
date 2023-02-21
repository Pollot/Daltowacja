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
import android.widget.Button
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.TextView
import android.widget.Toast
import androidx.camera.view.PreviewView
import kotlin.math.sqrt

class MainActivity : AppCompatActivity() {
    private lateinit var viewBinding: ActivityMainBinding

    private lateinit var cameraExecutor: ExecutorService

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewBinding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(viewBinding.root)

        if (allPermissionsGranted()) {
            startCamera()
            val previewView = findViewById<PreviewView>(R.id.viewFinder)
            val frozenFrame = findViewById<ImageView>(R.id.frozenFrame)
            val frozenButton = findViewById<Button>(R.id.freezeButton)
            val analyzeColorButton = findViewById<Button>(R.id.analyzeColorButton)
            val colorName = findViewById<TextView>(R.id.colorName)
            val colorDescription = findViewById<TextView>(R.id.colorDescription)
            val coloredRectangle = findViewById<RelativeLayout>(R.id.colorNameLayout)
            setPreviewViewFreezeOnClick(previewView, frozenFrame, frozenButton)
            captureFrame(previewView, colorName, colorDescription, coloredRectangle, analyzeColorButton)
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

            try {
                // Unbind use cases before rebinding
                cameraProvider.unbindAll()

                // Bind use case to camera
                cameraProvider.bindToLifecycle(
                    this, cameraSelector, preview)
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

            // Define a rectangle that covers the middle area, 49x49 pixels by default
            val rect = Rect(middleX - 24, middleY - 24, middleX + 24, middleY + 24)

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

    /* Hide or show buttons when clicked on screen -> for later
    private fun toggleButtonsOnClick(view: View, vararg buttons: Button) {
        view.setOnClickListener {
            for (button in buttons) {
                button.visibility = if (button.visibility == View.VISIBLE) View.GONE else View.VISIBLE
            }
        }
    }*/

    // Called when an activity is about to be destroyed
    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }

    // Basic static variables
    companion object {
        private const val TAG = "Daltowacja"
        private const val REQUEST_CODE_PERMISSIONS = 10
        private val REQUIRED_PERMISSIONS = mutableListOf(
            Manifest.permission.CAMERA).apply{}.toTypedArray()
    }
}
