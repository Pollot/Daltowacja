package com.daltowacja.daltowacja

import android.os.Bundle
import android.util.TypedValue
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
        val contactButton = findViewById<TextView>(R.id.contactButton)
        val donateButton = findViewById<TextView>(R.id.donateButton)
        SidebarButtons.setCameraButton(this, cameraButton)
        SidebarButtons.setSettingsButton(this, settingsButton)
        SidebarButtons.setContactButton(this, contactButton)
        SidebarButtons.setDonateButton(this, donateButton)

        // Set colorPrimaryVariant as settingsButton background
        val typedValue = TypedValue()
        val theme = this.theme  // Get the current activity's theme
        theme.resolveAttribute(
            com.google.android.material.R.attr.colorPrimaryVariant,
            typedValue,
            true
        )
        val colorPrimaryVariant = typedValue.data
        settingsButton.setBackgroundColor(colorPrimaryVariant)

        // set colorPrimary as cameraButton background
        val typedValuePrimary = TypedValue()
        theme.resolveAttribute(
            com.google.android.material.R.attr.colorPrimary,
            typedValuePrimary,
            true
        )
        val colorPrimary = typedValuePrimary.data
        cameraButton.setBackgroundColor(colorPrimary)

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

            val themePreference = findPreference<Preference>("theme")

            val languagePreference = findPreference<Preference>("language")

            // Set a listener to handle theme preference changes
            themePreference?.setOnPreferenceChangeListener { _, newValue ->
                if (newValue is String) {

                    ThemeManager.applyTheme(newValue)

                    true
                } else {
                    false
                }
            }

            // Set a listener to handle theme language changes
            languagePreference?.setOnPreferenceChangeListener { _, newValue ->
                if (newValue is String) {

                    val context = requireContext()

                    LanguageManager.setAppLanguage(context, newValue)

                    // Recreate the activity to apply the new language
                    requireActivity().recreate()

                    true
                } else {
                    false
                }
            }
        }
    }
}