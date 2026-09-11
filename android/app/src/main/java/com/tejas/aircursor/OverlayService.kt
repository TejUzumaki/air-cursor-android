package com.tejas.aircursor

import android.app.Service
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.util.Log
import android.view.View
import android.view.WindowManager
import android.widget.TextView

class OverlayService : Service() {
    private var windowManager: WindowManager? = null
    private var cursorView: View? = null
    private var layoutParams: WindowManager.LayoutParams? = null

    companion object {
        var instance: OverlayService? = null
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        instance = this
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        
        cursorView = TextView(this).apply {
            text = "●"
            setTextColor(Color.parseColor("#00FF00"))
            textSize = 24f
        }

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
        
        try {
            windowManager?.addView(cursorView, layoutParams)
        } catch (e: Exception) {
            Log.e("OverlayService", "Failed to add view", e)
        }
    }

    fun updateCursor(x: Float, y: Float) {
        cursorView?.post {
            layoutParams?.x = x.toInt() - 20
            layoutParams?.y = y.toInt() - 20
            try {
                windowManager?.updateViewLayout(cursorView, layoutParams)
            } catch(e: Exception) {}
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        instance = null
        if (cursorView != null) windowManager?.removeView(cursorView)
    }
}
