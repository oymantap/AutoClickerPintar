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

        // Supaya EditText responsif dan keyboard langsung muncul tanpa delay
        panelParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY else WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 100
            y = 100
        }

        val inputSpeed = panelView.findViewById<EditText>(R.id.et_speed)
        val inputTarget = panelView.findViewById<EditText>(R.id.et_target_num)

        // SOLUSI KEYBOARD: Kalau user klik di luar widget, lepas fokus biar bisa interaksi sama game/layar bawah
        panelView.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_OUTSIDE) {
                panelView.clearFocus()
                panelParams.flags = panelParams.flags or WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                windowManager.updateViewLayout(panelView, panelParams)
            }
            false
        }

        // Kalau kolom input diklik, paksa window manager buat minta fokus penuh ke keyboard
        val textFocusListener = View.OnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                panelParams.flags = panelParams.flags and WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE.inv()
                windowManager.updateViewLayout(panelView, panelParams)
            }
        }
        inputSpeed.onFocusChangeListener = textFocusListener
        inputTarget.onFocusChangeListener = textFocusListener

        // SOLUSI GESER KAKU: Sekarang SELURUH BADAN PANEL (bukan cuma atasnya) bisa digeser bebas
        val mainLayout = panelView.findViewById<View>(R.id.panel_main_layout)
        mainLayout.setOnTouchListener(object : View.OnTouchListener {
            private var initialX = 0
            private var initialY = 0
            private var initialTouchX = 0f
            private var initialTouchY = 0f

            override fun onTouch(v: View?, event: MotionEvent): Boolean {
                // Jangan ganggu geser kalau user lagi fokus ngetik di kolom input
                if (inputSpeed.isFocused || inputTarget.isFocused) return false

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
            if (targetList.size < 10) addNewTargetPointer()
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

                ClickerService.clickSpeedMs = inputSpeed.text.toString().toLongOrNull() ?: 300L
                ClickerService.targetNumber = inputTarget.text.toString().takeIf { it.isNotEmpty() }

                val coordinates = ArrayList<Pair<Float, Float>>()
                for (target in targetList) {
                    val params = target.layoutParams as WindowManager.LayoutParams
                    // FIX AKURASI KOORDINAT: Ditambah offset pas 24dp (tengah lingkaran diameter 48dp)
                    val centerX = params.x + 72f  // Konversi 24dp ke piksel di layar standar rata-rata
                    val centerY = params.y + 72f
                    coordinates.add(Pair(centerX, centerY))
                }

                ClickerService.isRunning = true
                btnStart.text = "STOP"
                btnStart.setBackgroundColor(0xFFFF3366.toInt())
                
                // Lepaskan fokus panel saat start agar klik tembus lancar ke bawah
                panelView.clearFocus()
                panelParams.flags = panelParams.flags or WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                windowManager.updateViewLayout(panelView, panelParams)

                ClickerService.instance?.startMultiClicking(coordinates)
            } else {
                stopClicking()
            }
        }

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
            x = 300 + (index * 50)
            y = 600
        }

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
        for (target in targetList) windowManager.removeView(target)
        targetList.clear()
        instance = null
    }
}
