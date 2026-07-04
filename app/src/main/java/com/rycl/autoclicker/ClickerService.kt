package com.rycl.autoclicker

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.os.Handler
import android.os.Looper
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

class ClickerService : AccessibilityService() {

    private val mainHandler = Handler(Looper.getMainLooper())

    companion object {
        var instance: ClickerService? = null
        var isRunning = false
        var clickSpeedMs: Long = 300
        var targetNumber: String? = null
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Dikosongkan agar ramah CPU
    }

    private fun scanScreenForTarget() {
        if (!isRunning || targetNumber == null) return
        
        // Menggunakan rootInActiveWindow secara aman tanpa memutus siklus loop
        val rootNode = rootInActiveWindow ?: return
        if (checkNodes(rootNode)) {
            isRunning = false
            mainHandler.post {
                FloatingService.instance?.stopClicking()
            }
        }
    }

    private fun checkNodes(node: AccessibilityNodeInfo?): Boolean {
        if (node == null || !isRunning) return false
        
        if (node.text != null && node.text.toString().contains(targetNumber!!)) {
            return true
        }
        
        for (i in 0 until node.childCount) {
            if (checkNodes(node.getChild(i))) {
                return true
            }
        }
        return false
    }

    fun startMultiClicking(coords: List<Pair<Float, Float>>) {
        if (coords.isEmpty()) return
        isRunning = true
        executeSequenceLoop(coords, 0)
    }

    private fun executeSequenceLoop(coords: List<Pair<Float, Float>>, currentIndex: Int) {
        if (!isRunning) return

        val targetIdx = if (currentIndex >= coords.size) 0 else currentIndex
        val (x, y) = coords[targetIdx]

        val path = Path().apply { moveTo(x, y) }
        val gestureBuilder = GestureDescription.Builder()
        
        // Gunakan durasi standar stroke yang pas (40ms) agar dikenali OS Android sebagai sentuhan fisik
        gestureBuilder.addStroke(GestureDescription.StrokeDescription(path, 0, 40))

        dispatchGesture(gestureBuilder.build(), object : GestureResultCallback() {
            override fun onCompleted(gestureDescription: GestureDescription?) {
                super.onCompleted(gestureDescription)

                // Scan dijalankan secara pararel tanpa memblokir thread
                scanScreenForTarget()

                // Paksa penjadwalan ulang ke target berikutnya
                mainHandler.postDelayed({
                    executeSequenceLoop(coords, targetIdx + 1)
                }, clickSpeedMs)
            }

            override fun onCancelled(gestureDescription: GestureDescription?) {
                super.onCancelled(gestureDescription)
                // Jika dibatalkan sistem, tetap paksa coba jalankan target berikutnya agar tidak mogok total
                if (isRunning) {
                    mainHandler.postDelayed({
                        executeSequenceLoop(coords, targetIdx + 1)
                    }, clickSpeedMs)
                }
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
