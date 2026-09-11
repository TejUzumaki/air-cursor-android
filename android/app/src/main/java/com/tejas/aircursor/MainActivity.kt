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
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.webkit.WebResourceResponse
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.webkit.WebViewAssetLoader

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
            val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName"))
            startActivity(intent)
            return
        }

        if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            statusText.text = "Please grant Camera permission. Reopen app after."
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), 101)
            return
        }

        statusText.text = "Permissions OK. Starting Camera..."
        initApp()
        isInitialized = true
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 101) {
            isInitialized = false
        }
    }

    private fun initApp() {
        startService(Intent(this, OverlayService::class.java))

        val webView = WebView(this)
        webView.settings.javaScriptEnabled = true
        webView.settings.domStorageEnabled = true
        
        val assetLoader = WebViewAssetLoader.Builder()
            .addPathHandler("/assets/", WebViewAssetLoader.AssetsPathHandler(this))
            .build()

        webView.webViewClient = object : WebViewClient() {
            override fun shouldInterceptRequest(view: WebView?, request: WebResourceRequest?): WebResourceResponse? {
                val url = request?.url ?: return null
                return assetLoader.shouldInterceptRequest(url)
            }
        }
        
        webView.webChromeClient = object : WebChromeClient() {
            override fun onPermissionRequest(request: PermissionRequest) {
                runOnUiThread { request.grant(request.resources) }
            }
        }
        
        val bridge = AirCursorBridge()
        webView.addJavascriptInterface(bridge, "AndroidCursor")
        
        webView.loadUrl("https://appassets.androidplatform.net/assets/index.html")
        setContentView(webView)
    }
}
