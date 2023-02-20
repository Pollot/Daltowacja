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
import android.widget.TextView
import android.widget.Toast
import androidx.camera.view.PreviewView

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
            setPreviewViewFreezeOnClick(previewView, frozenFrame, frozenButton)
            captureFrame(previewView, colorName, analyzeColorButton)
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

    @SuppressLint("SetTextI18n")
    private fun captureFrame(previewView: PreviewView, text: TextView, button: Button) {
        button.setOnClickListener {
            // Capture the current frame as a Bitmap
            val image = previewView.bitmap

            val middleX = image!!.width / 2
            val middleY = image.height / 2

            // Get the middle pixel
            val pixel = image.getPixel(middleX, middleY)
            val red = Color.red(pixel)
            val green = Color.green(pixel)
            val blue = Color.blue(pixel)

            // Calculate the average color of the middle pixel -> for later
            // val averageColor = Color.rgb((red + green + blue) / 3, (red + green + blue) / 3, (red + green + blue) / 3)

            text.text = "RGB: (" + (red and 0xFF).toString() + ", " + (green and 0xFF).toString() + ", " + (blue and 0xFF).toString() + ")"
        }
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
