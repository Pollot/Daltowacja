package com.daltowacja.daltowacja

import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.drawerlayout.widget.DrawerLayout
import androidx.preference.PreferenceFragmentCompat

class SettingsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
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
        }
    }
}