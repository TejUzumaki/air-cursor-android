package com.tejas.aircursor

import android.webkit.JavascriptInterface

class AirCursorBridge {
    @JavascriptInterface
    fun moveCursor(x: Float, y: Float) {
        OverlayService.instance?.updateCursor(x, y)
    }

    @JavascriptInterface
    fun triggerClick(x: Float, y: Float) {
        AirCursorAccessibilityService.instance?.simulateClick(x, y)
    }
}
