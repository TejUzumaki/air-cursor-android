package com.tejas.aircursor

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.webkit.PermissionRequest
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat

class MainActivity : AppCompatActivity() {
    private var isInitialized = false
    private lateinit var statusText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Show a simple status text while checking permissions
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
        // Check permissions every time the app comes to the foreground
        // This catches the user returning from the permission settings screen
        checkAndInit()
    }

    private fun checkAndInit() {
        if (isInitialized) return

        // Step 1: Check Overlay Permission
        if (!Settings.canDrawOverlays(this)) {
            statusText.text = "Please grant 'Display over other apps' permission. Reopen app after."
            val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName"))
            startActivity(intent)
            return
        }

        // Step 2: Check Camera Permission
        if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            statusText.text = "Please grant Camera permission. Reopen app after."
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), 101)
            return
        }

        // Step 3: Both permissions granted! Load the heavy UI.
        statusText.text = "Permissions OK. Starting Camera..."
        initApp()
        isInitialized = true
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 101) {
            // Force re-check when the user returns from the camera permission prompt
            isInitialized = false
        }
    }

    private fun initApp() {
        // Start the Overlay Service safely
        startService(Intent(this, OverlayService::class.java))

        // Initialize the WebView and MediaPipe
        val webView = WebView(this)
        webView.settings.javaScriptEnabled = true
        webView.settings.domStorageEnabled = true
        webView.webViewClient = WebViewClient()
        
        // Automatically grant camera requests inside the WebView
        webView.webChromeClient = object : WebChromeClient() {
            override fun onPermissionRequest(request: PermissionRequest) {
                request.grant(request.resources)
            }
        }
        
        // Inject the bridge so JS can talk to Android
        val bridge = AirCursorBridge()
        webView.addJavascriptInterface(bridge, "AndroidCursor")
        
        webView.loadUrl("file:///android_asset/index.html")
        setContentView(webView)
    }
}
