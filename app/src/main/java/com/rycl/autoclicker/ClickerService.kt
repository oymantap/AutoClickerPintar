package com.rycl.autoclicker

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

class ClickerService : AccessibilityService() {

    companion object {
        var instance: ClickerService? = null
        var isRunning = false
        var clickSpeedMs: Long = 200
        var targetNumber: String? = null
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Pemindaian pintar secara periodik lewat node layout pohon layar aktif
        if (isRunning && targetNumber != null) {
            val rootNode = rootInActiveWindow ?: return
            checkNodesForTarget(rootNode)
        }
    }

    private fun checkNodesForTarget(node: AccessibilityNodeInfo) {
        if (!isRunning) return
        if (node.text != null && node.text.toString().contains(targetNumber!!)) {
            // Target angka ditemukan di layar! Berhenti sekarang agar tidak kelewat/rugi
            isRunning = false
            android.os.Handler(mainLooper).post {
                FloatingService.instance?.stopClicking()
            }
            return
        }
        for (i in 0 until node.childCount) {
            node.getChild(i)?.let { checkNodesForTarget(it) }
        }
    }

    // Fungsi pemicu eksekusi multi-target sequensial
    fun startMultiClicking(coords: List<Pair<Float, Float>>) {
        if (coords.isEmpty()) return
        executeSequenceLoop(coords, 0)
    }

    private fun executeSequenceLoop(coords: List<Pair<Float, Float>>, currentIndex: Int) {
        if (!isRunning) return

        val targetIdx = if (currentIndex >= coords.size) 0 else currentIndex
        val (x, y) = coords[targetIdx]

        val path = Path().apply { moveTo(x, y) }
        val gestureBuilder = GestureDescription.Builder()
        gestureBuilder.addStroke(GestureDescription.StrokeDescription(path, 0, 40))

        dispatchGesture(gestureBuilder.build(), object : GestureResultCallback() {
            override fun onCompleted(gestureDescription: GestureDescription?) {
                super.onCompleted(gestureDescription)
                
                // Berikan jeda antar urutan sesuai input speed agar aman dari ghost touch
                android.os.Handler(mainLooper).postDelayed({
                    if (isRunning) {
                        executeSequenceLoop(coords, targetIdx + 1)
                    }
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
