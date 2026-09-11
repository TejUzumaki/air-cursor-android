package com.tejas.aircursor

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import android.view.View
import android.view.WindowManager
import android.webkit.PermissionRequest
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.ImageView
import android.widget.LinearLayout

class OverlayService : Service() {
    private var windowManager: WindowManager? = null
    private var cursorView: View? = null
    private var layoutParams: WindowManager.LayoutParams? = null
    private var webView: WebView? = null

    companion object {
        var instance: OverlayService? = null
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        instance = this
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager

        // Start as Foreground Service to prevent Android from killing it
        val channelId = "air_cursor_channel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "Air Cursor", NotificationManager.IMPORTANCE_MIN)
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
        val notification = Notification.Builder(this, channelId)
            .setContentTitle("Air Cursor is Running")
            .setContentText("Hand tracking is active in the background.")
            .setSmallIcon(android.R.drawable.ic_menu_compass)
            .build()
        startForeground(1, notification)

        // 1. Add Cursor Overlay
        cursorView = ImageView(this).apply { setImageResource(R.drawable.cursor) }
        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) 
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY 
        else 
            WindowManager.LayoutParams.TYPE_PHONE

        layoutParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            type,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        )
        layoutParams?.x = 0
        layoutParams?.y = 0
        try { windowManager?.addView(cursorView, layoutParams) } catch (e: Exception) {}

        // 2. Add WebView Container (Transparent, Click-through)
        val container = LinearLayout(this)
        val containerParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            type,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        )
        try { windowManager?.addView(container, containerParams) } catch (e: Exception) {}

        // 3. Initialize WebView inside the container using ApplicationContext
        webView = WebView(applicationContext)
        WebView.setWebContentsDebuggingEnabled(true)
        webView!!.settings.javaScriptEnabled = true
        webView!!.settings.domStorageEnabled = true
        webView!!.settings.cacheMode = WebSettings.LOAD_DEFAULT
        
        webView!!.webViewClient = object : WebViewClient() {
            override fun onReceivedError(view: WebView?, request: android.webkit.WebResourceRequest?, error: android.webkit.WebResourceError?) {
                super.onReceivedError(view, request, error)
                Log.e("WebViewError", "Error: ${error?.description}")
            }
        }
        
        webView!!.webChromeClient = object : WebChromeClient() {
            override fun onPermissionRequest(request: PermissionRequest) {
                val handler = Handler(Looper.getMainLooper())
                handler.post { request.grant(request.resources) }
            }
        }
        webView!!.addJavascriptInterface(AirCursorBridge(), "AndroidCursor")
        webView!!.loadUrl("https://air-cursor-android.vercel.app/")
        container.addView(webView)
    }

    fun updateCursor(x: Float, y: Float) {
        cursorView?.post {
            layoutParams?.x = x.toInt()
            layoutParams?.y = y.toInt()
            try { windowManager?.updateViewLayout(cursorView, layoutParams) } catch(e: Exception) {}
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        instance = null
        if (cursorView != null) windowManager?.removeView(cursorView)
        
        webView?.let { wv ->
            wv.destroy()
            (wv.parent as? LinearLayout)?.removeView(wv)
            (wv.parent as? View)?.let { parentView -> windowManager?.removeView(parentView) }
        }
    }
}
