package com.rycl.autoclicker

import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.EditText

class FloatingService : Service() {

    private lateinit var windowManager: WindowManager
    private lateinit var floatingView: View

    companion object {
        var instance: FloatingService? = null
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        instance = this
        
        floatingView = LayoutInflater.from(this).inflate(R.layout.floating_widget, null)

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) 
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY 
            else 
                WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 100
            y = 100
        }

        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        windowManager.addView(floatingView, params)

        val btnStart = floatingView.findViewById<Button>(R.id.btn_start)
        val inputSpeed = floatingView.findViewById<EditText>(R.id.et_speed)
        val inputTarget = floatingView.findViewById<EditText>(R.id.et_target_num)

        btnStart.setOnClickListener {
            if (!ClickerService.isRunning) {
                // Setup konfigurasi
                ClickerService.clickSpeedMs = inputSpeed.text.toString().toLongOrNull() ?: 100L
                ClickerService.targetNumber = inputTarget.text.toString().takeIf { it.isNotEmpty() }
                ClickerService.isRunning = true
                btnStart.text = "STOP"
                
                // Mulai klik di koordinat tengah layar (bisa dimodif pakai pointer)
                ClickerService.instance?.clickAt(500f, 1000f)
            } else {
                stopClicking()
            }
        }
    }

    fun stopClicking() {
        ClickerService.isRunning = false
        val btnStart = floatingView.findViewById<Button>(R.id.btn_start)
        btnStart?.text = "START"
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::floatingView.isInitialized) windowManager.removeView(floatingView)
    }
}
