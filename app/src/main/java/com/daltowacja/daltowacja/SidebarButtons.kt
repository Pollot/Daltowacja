package com.daltowacja.daltowacja

import android.content.Context
import android.content.Intent
import android.widget.TextView

object SidebarButtons {
    fun setClickListeners(context: Context,
                          cameraButton: TextView,
                          settingsButton: TextView
    ) {

        cameraButton.setOnClickListener {
            val intent = Intent(context, MainActivity::class.java)
            context.startActivity(intent)
        }

        settingsButton.setOnClickListener {
            val intent = Intent(context, SettingsActivity::class.java)
            context.startActivity(intent)
        }
    }
}