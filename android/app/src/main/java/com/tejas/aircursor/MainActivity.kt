package com.tejas.aircursor

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat

class MainActivity : AppCompatActivity() {
    private var isInitialized = false
    private lateinit var statusText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        statusText = TextView(this).apply {
            text = "Initializing Air Cursor..."
            textSize = 20f
            textAlignment = View.TEXT_ALIGNMENT_CENTER
            setPadding(20, 200, 20, 20)
        }
        setContentView(statusText)
    }

    override fun onResume() {
        super.onResume()
        checkAndInit()
    }

    private fun checkAndInit() {
        if (isInitialized) return

        if (!Settings.canDrawOverlays(this)) {
            statusText.text = "Please grant 'Display over other apps' permission. Reopen app after."
            startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")))
            return
        }

        if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            statusText.text = "Please grant Camera permission. Reopen app after."
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), 101)
            return
        }

        // Request Notification permission for Foreground Service on Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), 102)
                return
            }
        }

        statusText.text = "Permissions OK. Starting Air Cursor..."
        startService(Intent(this, OverlayService::class.java))
        finish()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        isInitialized = false
    }
}
