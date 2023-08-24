package com.daltowacja.daltowacja

import android.content.Context
import android.content.Intent
import android.widget.TextView

object SidebarButtons {
        fun setCameraButton(context: Context, cameraButton: TextView) {
            cameraButton.setOnClickListener {
                val intent = Intent(context, MainActivity::class.java)
                context.startActivity(intent)
            }
        }

        fun setSettingsButton(context: Context, settingsButton: TextView) {
            settingsButton.setOnClickListener {
                val intent = Intent(context, SettingsActivity::class.java)
                context.startActivity(intent)
            }
        }
}