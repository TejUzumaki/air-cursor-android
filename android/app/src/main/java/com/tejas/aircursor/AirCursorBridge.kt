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

    @JavascriptInterface
    fun triggerBack() {
        AirCursorAccessibilityService.instance?.performBackSwipe()
    }

    @JavascriptInterface
    fun swipeLeft() {
        AirCursorAccessibilityService.instance?.swipeLeft()
    }

    @JavascriptInterface
    fun swipeRight() {
        AirCursorAccessibilityService.instance?.swipeRight()
    }
}
