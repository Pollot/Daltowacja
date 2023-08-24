package com.daltowacja.daltowacja

import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.drawerlayout.widget.DrawerLayout
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceManager

class SettingsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        // Apply selected theme
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
        val selectedTheme = sharedPreferences.getString("theme", "auto") ?: "auto"
        ThemeManager.applyTheme(selectedTheme)

        super.onCreate(savedInstanceState)
        setContentView(R.layout.settings_activity)

        // Set custom toolbar
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        // Disable application name in the toolbar
        supportActionBar?.setDisplayShowTitleEnabled(false)

        val drawerLayout = findViewById<DrawerLayout>(R.id.drawer_layout)

        // Toolbar buttons
        val menuButton = findViewById<ImageView>(R.id.menuButton)
        val infoButton = findViewById<ImageView>(R.id.infoButton)
        ToolbarButtons.setupSidebarToggle(this, drawerLayout, menuButton)
        ToolbarButtons.infoOnClick(this, infoButton)

        // Sidebar buttons
        val cameraButton = findViewById<TextView>(R.id.cameraButton)
        val settingsButton = findViewById<TextView>(R.id.settingsButton)
        SidebarButtons.setCameraButton(this, cameraButton)
        SidebarButtons.setSettingsButton(this, settingsButton)

        if (savedInstanceState == null) {
            supportFragmentManager
                .beginTransaction()
                .replace(R.id.settings, SettingsFragment())
                .commit()
        }
    }

    class SettingsFragment : PreferenceFragmentCompat() {
        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(R.xml.root_preferences, rootKey)

            // Get the preference instance for the theme preference
            val themePreference = findPreference<Preference>("theme")

            // Set a listener to handle theme preference changes
            themePreference?.setOnPreferenceChangeListener { _, newValue ->
                if (newValue is String) {

                    val context = requireContext().applicationContext // Use this to get a non-null context
                    val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
                    sharedPreferences.edit().putString("theme", newValue).apply()

                    ThemeManager.applyTheme(newValue)

                    true
                } else {
                    false
                }
            }
        }
    }
}