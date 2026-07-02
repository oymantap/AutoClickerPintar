package com.rycl.autoclicker

import android.annotation.SuppressLint
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.EditText
import android.widget.TextView

class FloatingService : Service() {

    private lateinit var windowManager: WindowManager
    private lateinit var panelView: View
    private lateinit var panelParams: WindowManager.LayoutParams
    
    // List untuk nampung maksimal 10 target pointer
    private val targetList = ArrayList<View>()

    companion object {
        var instance: FloatingService? = null
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        instance = this
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        
        setupControlPanel()
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupControlPanel() {
        panelView = LayoutInflater.from(this).inflate(R.layout.floating_widget, null)

        // Default awal pake FLAG_NOT_FOCUSABLE biar bisa nembus klik ke app bawahnya
        panelParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY else WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 100
            y = 100
        }

        // Trik Keyboard Aktif: Jika kolom input disentuh, ganti flag agar fokus
        val inputSpeed = panelView.findViewById<EditText>(R.id.et_speed)
        val inputTarget = panelView.findViewById<EditText>(R.id.et_target_num)
        
        val focusTouchListener = View.OnTouchListener { _, _ ->
            if ((panelParams.flags and WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE) != 0) {
                panelParams.flags = panelParams.flags and WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE.inv()
                windowManager.updateViewLayout(panelView, panelParams)
            }
            false
        }
        inputSpeed.setOnTouchListener(focusTouchListener)
        inputTarget.setOnTouchListener(focusTouchListener)

        // Logika Drag/Geser Control Panel Utama via handle atas
        val dragHandle = panelView.findViewById<View>(R.id.panel_drag_handle)
        dragHandle.setOnTouchListener(object : View.OnTouchListener {
            private var initialX = 0
            private var initialY = 0
            private var initialTouchX = 0f
            private var initialTouchY = 0f

            override fun onTouch(v: View?, event: MotionEvent): Boolean {
                // Kembalikan ke mode default focus saat geser panel biar gak ganggu layar bawah
                if ((panelParams.flags and WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE) == 0) {
                    panelParams.flags = panelParams.flags or WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                    windowManager.updateViewLayout(panelView, panelParams)
                }
                
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        initialX = panelParams.x
                        initialY = panelParams.y
                        initialTouchX = event.rawX
                        initialTouchY = event.rawY
                        return true
                    }
                    MotionEvent.ACTION_MOVE -> {
                        panelParams.x = initialX + (event.rawX - initialTouchX).toInt()
                        panelParams.y = initialY + (event.rawY - initialTouchY).toInt()
                        windowManager.updateViewLayout(panelView, panelParams)
                        return true
                    }
                }
                return false
            }
        })

        // Aksi Tombol Tambah Target (+)
        panelView.findViewById<Button>(R.id.btn_add_target).setOnClickListener {
            if (targetList.size < 10) {
                addNewTargetPointer()
            }
        }

        // Aksi Tombol Kurang Target (-)
        panelView.findViewById<Button>(R.id.btn_remove_target).setOnClickListener {
            if (targetList.isNotEmpty()) {
                val lastTarget = targetList.removeAt(targetList.size - 1)
                windowManager.removeView(lastTarget)
            }
        }

        // Aksi Tombol Start/Stop
        val btnStart = panelView.findViewById<Button>(R.id.btn_start)
        btnStart.setOnClickListener {
            if (!ClickerService.isRunning) {
                if (targetList.isEmpty()) return@setOnClickListener
                
                ClickerService.clickSpeedMs = inputSpeed.text.toString().toLongOrNull() ?: 200L
                ClickerService.targetNumber = inputTarget.text.toString().takeIf { it.isNotEmpty() }
                
                // Ambil koordinat x, y dari seluruh target yang udah diletakkan user
                val coordinates = ArrayList<Pair<Float, Float>>()
                for (target in targetList) {
                    val params = target.layoutParams as WindowManager.LayoutParams
                    // Ambil titik tengah target bulat (ukuran 48dp -> offset sekitar 24dp)
                    val centerX = params.x + (target.width / 2).toFloat()
                    val centerY = params.y + (target.height / 2).toFloat()
                    coordinates.add(Pair(centerX, centerY))
                }

                ClickerService.isRunning = true
                btnStart.text = "STOP"
                btnStart.setBackgroundColor(0xFFFF3366.toInt())

                // Jalankan rantai eksekusi multi-target
                ClickerService.instance?.startMultiClicking(coordinates)
            } else {
                stopClicking()
            }
        }

        // Tombol Close total widget melayang
        panelView.findViewById<Button>(R.id.btn_close_panel).setOnClickListener {
            stopSelf()
        }

        windowManager.addView(panelView, panelParams)
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun addNewTargetPointer() {
        val targetView = LayoutInflater.from(this).inflate(R.layout.target_pointer_layout, null)
        val index = targetList.size + 1
        
        targetView.findViewById<TextView>(R.id.tv_target_number).text = index.toString()

        val pointerParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY else WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 300 + (index * 40) // Biar letak awal gak tumpang tindih total
            y = 500
        }

        // Bikin pointer ini bisa diseret bebas ke mana aja di layar game
        targetView.setOnTouchListener(object : View.OnTouchListener {
            private var initialX = 0
            private var initialY = 0
            private var initialTouchX = 0f
            private var initialTouchY = 0f

            override fun onTouch(v: View?, event: MotionEvent): Boolean {
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        initialX = pointerParams.x
                        initialY = pointerParams.y
                        initialTouchX = event.rawX
                        initialTouchY = event.rawY
                        return true
                    }
                    MotionEvent.ACTION_MOVE -> {
                        pointerParams.x = initialX + (event.rawX - initialTouchX).toInt()
                        pointerParams.y = initialY + (event.rawY - initialTouchY).toInt()
                        windowManager.updateViewLayout(targetView, pointerParams)
                        return true
                    }
                }
                return false
            }
        })

        windowManager.addView(targetView, pointerParams)
        targetList.add(targetView)
    }

    fun stopClicking() {
        ClickerService.isRunning = false
        val btnStart = panelView.findViewById<Button>(R.id.btn_start)
        btnStart?.text = "START"
        btnStart?.setBackgroundColor(0xFF00E676.toInt())
    }

    override fun onDestroy() {
        super.onDestroy()
        ClickerService.isRunning = false
        if (::panelView.isInitialized) windowManager.removeView(panelView)
        for (target in targetList) {
            windowManager.removeView(target)
        }
        targetList.clear()
        instance = null
    }
}
