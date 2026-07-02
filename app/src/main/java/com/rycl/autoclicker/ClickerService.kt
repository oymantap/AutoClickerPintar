package com.rycl.autoclicker

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.view.accessibility.AccessibilityEvent
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions

class ClickerService : AccessibilityService() {

    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    
    companion object {
        var instance: ClickerService? = null
        var isRunning = false
        var clickSpeedMs: Long = 100 // Mencegah ghost touch
        var targetNumber: String? = null
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Jika mode deteksi angka aktif, kita bisa capture screen di sini
        // Untuk performa mantap, deteksi dilakukan via screenshot / layout nodes
        if (isRunning && targetNumber != null) {
            val rootNode = rootInActiveWindow ?: return
            checkNodesForTarget(rootNode)
        }
    }

    private fun checkNodesForTarget(node: android.view.accessibility.AccessibilityNodeInfo) {
        if (node.text != null && node.text.toString().contains(targetNumber!!)) {
            // Angka ketemu! Stop clicker biar ga rugi/kelewat
            isRunning = false
            FloatingService.instance?.stopClicking()
            return
        }
        for (i in 0 until node.childCount) {
            node.getChild(i)?.let { checkNodesForTarget(it) }
        }
    }

    fun clickAt(x: Float, y: Float) {
        if (!isRunning) return

        val path = Path().apply { moveTo(x, y) }
        val gestureBuilder = GestureDescription.Builder()
        
        // Durasi 50ms, jeda disesuaikan speed biar ga ghost touch
        gestureBuilder.addStroke(GestureDescription.StrokeDescription(path, 0, 50))
        
        dispatchGesture(gestureBuilder.build(), object : GestureResultCallback() {
            override fun onCompleted(gestureDescription: GestureDescription?) {
                super.onCompleted(gestureDescription)
                // Loop klik berdasarkan speed
                android.os.Handler(mainLooper).postDelayed({
                    if (isRunning) clickAt(x, y)
                }, clickSpeedMs)
            }
        }, null)
    }

    override fun onInterrupt() {
        instance = null
    }

    override fun onDestroy() {
        super.onDestroy()
        instance = null
    }
}
