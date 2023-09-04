package com.daltowacja.daltowacja

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.graphics.*
import android.graphics.drawable.BitmapDrawable
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.daltowacja.daltowacja.databinding.ActivityMainBinding
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.core.Preview
import androidx.camera.core.CameraSelector
import android.util.Log
import android.util.TypedValue
import android.view.MotionEvent
import android.view.View
import android.widget.*
import androidx.appcompat.widget.Toolbar
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.view.PreviewView
import androidx.drawerlayout.widget.DrawerLayout
import androidx.preference.PreferenceManager
import kotlin.math.sqrt

typealias LumaListener = (luma: Double) -> Unit
var currentPointerSize = 15
private var drawNetEnabled = false

class MainActivity : AppCompatActivity() {
    private lateinit var viewBinding: ActivityMainBinding

    private lateinit var cameraExecutor: ExecutorService

    override fun onCreate(savedInstanceState: Bundle?) {
        // Apply selected theme
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
        val selectedTheme = sharedPreferences.getString("theme", "auto") ?: "auto"
        ThemeManager.applyTheme(selectedTheme)

        super.onCreate(savedInstanceState)
        viewBinding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(viewBinding.root)

        // Set custom toolbar
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        // Disable application name in the toolbar
        supportActionBar?.setDisplayShowTitleEnabled(false)

        val drawerLayout = findViewById<DrawerLayout>(R.id.drawer_layout)

        // Toolbar buttons
        val menuButton = findViewById<ImageView>(R.id.menuButton)
        val infoButton = findViewById<ImageView>(R.id.infoButton)

        // Sidebar buttons
        val cameraButton = findViewById<TextView>(R.id.cameraButton)
        val settingsButton = findViewById<TextView>(R.id.settingsButton)
        val contactButton = findViewById<TextView>(R.id.contactButton)

        // Set colorPrimaryVariant as cameraButton background
        val typedValue = TypedValue()
        val theme = this.theme  // Get the current activity's theme
        theme.resolveAttribute(
            com.google.android.material.R.attr.colorPrimaryVariant,
            typedValue,
            true
        )
        val colorPrimaryVariant = typedValue.data
        cameraButton.setBackgroundColor(colorPrimaryVariant)

        // set colorPrimary as settingsButton background
        val typedValuePrimary = TypedValue()
        theme.resolveAttribute(
            com.google.android.material.R.attr.colorPrimary,
            typedValuePrimary,
            true
        )
        val colorPrimary = typedValuePrimary.data
        settingsButton.setBackgroundColor(colorPrimary)

        // Views
        val previewView = findViewById<PreviewView>(R.id.viewFinder)
        val frozenFrame = findViewById<ImageView>(R.id.frozenFrame)

        // Control buttons
        val frozenButton = findViewById<Button>(R.id.freezeButton)
        val analyzeColorButton = findViewById<Button>(R.id.analyzeColorButton)
        val colorSelectionButton = findViewById<Button>(R.id.colorSelectionButton)

        // Color description
        val colorName = findViewById<TextView>(R.id.colorName)
        val colorDescription = findViewById<TextView>(R.id.colorDescription)
        val coloredRectangle = findViewById<RelativeLayout>(R.id.colorNameLayout)

        // Pointer
        val pointerWhite = findViewById<ImageView>(R.id.pointerWhite)
        val pointerBlack = findViewById<ImageView>(R.id.pointerBlack)
        val pointerSizeSlider = findViewById<SeekBar>(R.id.pointerSizeSlider)

        val crosshair = findViewById<ImageView>(R.id.crosshair)

        if (allPermissionsGranted()) {
            startCamera()
            setPreviewViewFreezeOnClick(previewView, frozenFrame, frozenButton, colorSelectionButton, crosshair)
            captureFrame(analyzeColorButton, previewView, frozenFrame, crosshair, colorName, colorDescription, coloredRectangle)
            captureFrame(colorSelectionButton, previewView, frozenFrame, crosshair, colorName, colorDescription, coloredRectangle)
            changePointerSize(pointerSizeSlider, pointerWhite, pointerBlack)

            ToolbarButtons.setupSidebarToggle(this, drawerLayout, menuButton)
            ToolbarButtons.infoOnClick(this, infoButton)
            SidebarButtons.setCameraButton(this, cameraButton)
            SidebarButtons.setSettingsButton(this, settingsButton)
            SidebarButtons.setContactButton(this, contactButton)
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
                            if (luma >= 90 && pointerBlack.visibility == View.GONE) {
                                pointerWhite.visibility = View.GONE
                                pointerBlack.visibility = View.VISIBLE
                            } else {
                                if (luma < 90 && pointerWhite.visibility == View.GONE) {
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
                val toastMessage = getString(R.string.permissions_not_granted)
                Toast.makeText(this, toastMessage, Toast.LENGTH_SHORT).show()
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

    private fun getSelectedColorMode(): String {
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
        val selectedMode = sharedPreferences.getString("colors_mode", "advanced") ?: "advanced"

        return if (selectedMode == "advanced") {
            "advanced"
        } else {
            "basic"
        }
    }

    private fun getClosestColorNameAndDescription(rgb: IntArray): List<String> {
        var minDistance = Double.MAX_VALUE
        var nameDesc = listOf<String>()

        val colorsMode = getSelectedColorMode()

        // Check if the current OS language is set to Polish
        val currentLocales = resources.configuration.locales
        if (currentLocales[0].language == "pl") {
            for ((colorRgb, colorInfo) in if (colorsMode == "advanced") advancedColors_pl
            else basicColors_pl) {
                val distance = getEuclideanDistance(rgb, colorRgb)
                if (distance < minDistance) {
                    minDistance = distance
                    nameDesc = colorInfo
                }
            }
        } else {
            for ((colorRgb, colorInfo) in if (colorsMode == "advanced") advancedColors_en
            else basicColors_en) {
                val distance = getEuclideanDistance(rgb, colorRgb)
                if (distance < minDistance) {
                    minDistance = distance
                    nameDesc = colorInfo
                }
            }
        }

        return nameDesc
    }

    private fun captureFrame(button: Button,
                             previewView: PreviewView,
                             frozenFrame: ImageView,
                             crosshair: ImageView,
                             name: TextView,
                             description: TextView,
                             coloredRectangle: RelativeLayout) {
        button.setOnClickListener {
            if (button == findViewById(R.id.colorSelectionButton)) {
                // Enable drawing the net only for colorSelectionButton
                drawNetEnabled = true
            }

            if (button == findViewById(R.id.freezeButton)) {
                // Disable drawing the net when the freeze button is clicked
                drawNetEnabled = false
            }

            if (frozenFrame.visibility == View.GONE) {
                // Capture the current frame as a Bitmap
                val image = previewView.bitmap

                val middleX = image!!.width / 2
                val middleY = image.height / 2

                val sliderPosition = findViewById<SeekBar>(R.id.pointerSizeSlider)
                val searchAreaSize =
                    (sliderPosition.progress) + 1 // Add ceil function instead of +1 for 0 value

                // Log.d(TAG, "Area size: $searchAreaSize")

                // Define a rectangle that covers the middle area, size dynamically set by Slider
                val rect = Rect(
                    middleX - searchAreaSize,
                    middleY - searchAreaSize,
                    middleX + searchAreaSize,
                    middleY + searchAreaSize
                )

                // Log.d(TAG, "Rectangle: $rect")

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

                if ((red + green + blue) > 383) {
                    name.setTextColor(Color.BLACK)
                    description.setTextColor(Color.BLACK)
                } else {
                    name.setTextColor(Color.WHITE)
                    description.setTextColor(Color.WHITE)
                }

                //translates rgb to hsv then to hsv int and changes color of colorRectangle
                coloredRectangle.setBackgroundColor(Color.HSVToColor(rgbToHsv(red, green, blue)))
            }
            else {
                // Get the bitmap from the frozenFrame ImageView
                val bitmap = (frozenFrame.drawable as BitmapDrawable).bitmap

                // Calculate the coordinates of the crosshair's center in the frozenFrame
                val crosshairX = crosshair.x.toInt() + crosshair.width / 2
                val crosshairY = crosshair.y.toInt() + crosshair.height / 2

                // Calculate the color at the crosshair's position
                val pixel = bitmap.getPixel(crosshairX, crosshairY)

                val red = Color.red(pixel)
                val green = Color.green(pixel)
                val blue = Color.blue(pixel)

                // Set the text to the color at the crosshair's position
                val nameDesc = getClosestColorNameAndDescription(intArrayOf(red, green, blue))
                name.text = nameDesc[0]
                description.text = nameDesc[1]

                if ((red + green + blue) > 383) {
                    name.setTextColor(Color.BLACK)
                    description.setTextColor(Color.BLACK)
                } else {
                    name.setTextColor(Color.WHITE)
                    description.setTextColor(Color.WHITE)
                }

                //translates rgb to hsv then to hsv int and changes color of colorRectangle
                coloredRectangle.setBackgroundColor(Color.HSVToColor(rgbToHsv(red, green, blue)))

                if (drawNetEnabled) {
                    val netBitmap = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)

                    // Draw the original frozen frame onto the netBitmap
                    val canvas = Canvas(netBitmap)
                    canvas.drawBitmap(bitmap, 0f, 0f, null)

                    val netSpacing = 5
                    val netColor = Color.argb(255, 0, 0, 0)
                    val tolerance = 50.0

                    // Draw horizontal lines over the same color as the crosshair
                    for (y in 0 until netBitmap.height step netSpacing) {
                        for (x in 0 until netBitmap.width) {
                            val pixelAtPosition = bitmap.getPixel(x, y)
                            val pixelRed = Color.red(pixelAtPosition)
                            val pixelGreen = Color.green(pixelAtPosition)
                            val pixelBlue = Color.blue(pixelAtPosition)

                            // Calculate the Euclidean distance between the colors
                            val distance = getEuclideanDistance(
                                intArrayOf(red, green, blue),  // Crosshair's colors
                                intArrayOf(pixelRed, pixelGreen, pixelBlue)
                            )

                            if (distance <= tolerance) {
                                netBitmap.setPixel(x, y, netColor)
                            }
                        }
                    }

                    // Draw vertical lines over the same color as the crosshair
                    for (x in 0 until netBitmap.width step netSpacing) {
                        for (y in 0 until netBitmap.height) {
                            val pixelAtPosition = bitmap.getPixel(x, y)
                            val pixelRed = Color.red(pixelAtPosition)
                            val pixelGreen = Color.green(pixelAtPosition)
                            val pixelBlue = Color.blue(pixelAtPosition)

                            // Calculate the Euclidean distance between the colors
                            val distance = getEuclideanDistance(
                                intArrayOf(red, green, blue),  // Crosshair's colors
                                intArrayOf(pixelRed, pixelGreen, pixelBlue)
                            )

                            if (distance <= tolerance) {
                                netBitmap.setPixel(x, y, netColor)
                            }
                        }
                    }

                    // Set the netBitmap as the content of the frozenFrame ImageView
                    frozenFrame.setImageBitmap(netBitmap)
                }
            }
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
    @SuppressLint("ClickableViewAccessibility")
    private fun setPreviewViewFreezeOnClick(previewView: PreviewView, frozenFrame: ImageView,
                                            freezeButton: Button, colorSelectionButton: Button,
                                            crosshair: ImageView) {
        freezeButton.setOnClickListener {
            if (previewView.visibility == View.VISIBLE) {
                freezeButton.text = getString(R.string.unfreeze)
                val bitmap = previewView.bitmap
                frozenFrame.setImageBitmap(bitmap)
                previewView.visibility = View.GONE
                frozenFrame.visibility = View.VISIBLE
                colorSelectionButton.visibility = View.VISIBLE
                crosshair.visibility = View.VISIBLE
            } else {
                freezeButton.text = getString(R.string.freeze)
                previewView.visibility = View.VISIBLE
                frozenFrame.visibility = View.GONE
                colorSelectionButton.visibility = View.GONE
                crosshair.visibility = View.GONE
            }
        }
        // Update crosshair position on click
        frozenFrame.setOnTouchListener { view, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    val x = event.x
                    val y = event.y

                    crosshair.x = x - crosshair.width / 2
                    crosshair.y = y - crosshair.height / 2
                }
                MotionEvent.ACTION_UP -> {
                    // Simulate a click event
                    view.performClick()
                }
            }
            true
        }
    }

    private fun changePointerSize(pointerSizeSlider: SeekBar, pointerWhite: ImageView, pointerBlack: ImageView) {
        pointerSizeSlider.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                // This method will be called whenever the slider value changes
                // You can use the "progress" parameter to get the current slider value
                // and update your UI or perform any other actions as needed
                currentPointerSize =
                    (pointerSizeSlider.progress + 9) * 2 // 9 = 8 for stroke + 1 to avoid 0
                val paramsW = pointerWhite.layoutParams
                paramsW.width = currentPointerSize
                paramsW.height = currentPointerSize
                pointerWhite.layoutParams = paramsW
                val paramsB = pointerBlack.layoutParams
                paramsB.width = currentPointerSize
                paramsB.height = currentPointerSize
                pointerBlack.layoutParams = paramsB

                // Log.d(TAG, "Slider value: $progress")
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
                // This method will be called when the user starts dragging the slider
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                // This method will be called when the user stops dragging the slider
            }
        })
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

            // Calculate the starting position of the center square
            val startX = centerX - currentPointerSize / 2
            val startY = centerY - currentPointerSize / 2

            // Calculate the end position of the center square
            val endX = startX + currentPointerSize
            val endY = startY + currentPointerSize

            var totalLuma = 0.0
            var pixelCount = 0

            // Iterate through the pixels in the specified square and calculate the average luminosity
            for (y in startY until endY) {
                for (x in startX until endX) {
                    val pixelOffset = y * rowStride + x * pixelStride
                    val pixelValue = buffer.get(pixelOffset).toInt() and 0xFF
                    totalLuma += pixelValue
                    pixelCount++
                }
            }

            // Calculate the average luminosity of the specified square
            val avgLuma = totalLuma / pixelCount

            // Call the listener with the average luminosity value
            listener(avgLuma)

            image.close()
        }
    }

    // Called when an activity is about to be destroyed
    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }

    // Basic static variables
    companion object {
        const val TAG = "Daltowacja"
        private const val REQUEST_CODE_PERMISSIONS = 10
        private val REQUIRED_PERMISSIONS = mutableListOf(
            Manifest.permission.CAMERA).apply{}.toTypedArray()
    }
}
