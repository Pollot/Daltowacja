package com.daltowacja.daltowacja

import android.graphics.Typeface
import android.text.Layout
import android.text.Spannable
import android.text.SpannableString
import android.text.style.AlignmentSpan
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.util.TypedValue
import android.widget.ImageView
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout

object ToolbarButtons {
    fun setupSidebarToggle(activity: AppCompatActivity,
                           drawerLayout: DrawerLayout, menuButton: ImageView) {
        val toggle = ActionBarDrawerToggle(
            activity, drawerLayout, R.string.navigation_drawer_open, R.string.navigation_drawer_close
        )
        drawerLayout.addDrawerListener(toggle)
        toggle.syncState()

        menuButton.setOnClickListener {
            drawerLayout.openDrawer(GravityCompat.START)
        }
    }

    fun infoOnClick(activity: AppCompatActivity, infoButton: ImageView) {
        infoButton.setOnClickListener {
            val builder = AlertDialog.Builder(activity, R.style.CustomAlertDialogStyle)

            // Get the colorOnPrimary from themes.xml
            val typedValue = TypedValue()
            activity.theme.resolveAttribute(
                com.google.android.material.R.attr.colorOnPrimary,
                typedValue,
                true
            )
            val colorOnPrimary = typedValue.data

            val spannableString = SpannableString(MainActivity.TAG)

            spannableString.setSpan(
                ForegroundColorSpan(colorOnPrimary),
                0,
                spannableString.length,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )
            spannableString.setSpan(
                StyleSpan(Typeface.BOLD),
                0,
                spannableString.length,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )
            spannableString.setSpan(
                AlignmentSpan.Standard(Layout.Alignment.ALIGN_CENTER),
                0,
                spannableString.length,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )
            builder.setCancelable(false)
            builder.setMessage(spannableString)
                .setPositiveButton("OK") {
                        dialog, _ -> dialog.dismiss()
                }
            builder.setView(R.layout.info_dialog)
            builder.create().show()
        }
    }
}