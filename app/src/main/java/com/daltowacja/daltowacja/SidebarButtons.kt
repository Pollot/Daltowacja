package com.daltowacja.daltowacja

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.TextView
import android.widget.Toast

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

    fun setContactButton(context: Context, contactButton: TextView) {
        contactButton.setOnClickListener {
            val recipientEmail = "daltowacja@gmail.com"
            val subject = "Daltowacja"
            val intent = Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("mailto:$recipientEmail")
                putExtra(Intent.EXTRA_SUBJECT, subject)
            }

            // Verify that there's at least one email app available
            if (intent.resolveActivity(context.packageManager) != null) {
                context.startActivity(intent)
            } else {
                // Handle the case where no email app is available
                val toastMessage = context.resources.getString(R.string.no_email_app)
                Toast.makeText(context, toastMessage, Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun setDonateButton(context: Context, donateButton: TextView) {
        donateButton.setOnClickListener {
            val url = "https://www.buymeacoffee.com/daltowacja"
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            context.startActivity(intent)
        }
    }
}