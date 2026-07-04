package com.rycl.autoclicker

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.text.TextUtils
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private lateinit var btnOverlay: Button
    private lateinit var btnAccessibility: Button
    private lateinit var btnStartService: Button
    private lateinit var tvStatusOverlay: TextView
    private lateinit var tvStatusAccessibility: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Inisialisasi Komponen View
        btnOverlay = findViewById(R.id.btn_permit_overlay)
        btnAccessibility = findViewById(R.id.btn_permit_accessibility)
        btnStartService = findViewById(R.id.btn_start_service)
        tvStatusOverlay = findViewById(R.id.tv_status_overlay)
        tvStatusAccessibility = findViewById(R.id.tv_status_accessibility)

        // 1. Aksi Tombol Izin Overlay
        btnOverlay.setOnClickListener {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
                val intent = Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:$packageName")
                )
                startActivity(intent)
            }
        }

        // 2. Aksi Tombol Izin Accessibility
        btnAccessibility.setOnClickListener {
            if (!isAccessibilityServiceEnabled(this, ClickerService::class.java)) {
                val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                startActivity(intent)
            }
        }

        // 3. Aksi Jalankan Service Melayang Utama
        btnStartService.setOnClickListener {
            val hasOverlay = Build.VERSION.SDK_INT < Build.VERSION_CODES.M || Settings.canDrawOverlays(this)
            val hasAccessibility = isAccessibilityServiceEnabled(this, ClickerService::class.java)

            if (!hasOverlay) {
                Toast.makeText(this, "⚠️ Aktifkan Lapisan Atas (Overlay) dulu!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (!hasAccessibility) {
                Toast.makeText(this, "⚠️ Aktifkan Aksesibilitas Core dulu!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Eksekusi widget melayang
            startService(Intent(this, FloatingService::class.java))
            moveTaskToBack(true) // Sembunyikan aplikasi ke background
        }
    }

    override fun onResume() {
        super.onResume()
        // Validasi dan update tampilan UI setiap aplikasi dibuka kembali
        refreshPermissionStatus()
    }

    private fun refreshPermissionStatus() {
        val hasOverlay = Build.VERSION.SDK_INT < Build.VERSION_CODES.M || Settings.canDrawOverlays(this)
        val hasAccessibility = isAccessibilityServiceEnabled(this, ClickerService::class.java)

        // Manajemen UI untuk Kartu Izin Overlay
        if (hasOverlay) {
            tvStatusOverlay.text = "AKTIF ✓"
            tvStatusOverlay.setTextColor(Color.parseColor("#00E676"))
            btnOverlay.text = "Izin Diberikan"
            btnOverlay.backgroundTintList = android.content.res.ColorStateList.valueOf(Color.parseColor("#252530"))
            btnOverlay.setTextColor(Color.parseColor("#555566"))
            btnOverlay.isEnabled = false
        } else {
            tvStatusOverlay.text = "Dibutuhkan"
            tvStatusOverlay.setTextColor(Color.parseColor("#FF3366"))
            btnOverlay.text = "Izinkan Overlay"
            btnOverlay.backgroundTintList = android.content.res.ColorStateList.valueOf(Color.parseColor("#FF3366"))
            btnOverlay.setTextColor(Color.WHITE)
            btnOverlay.isEnabled = true
        }

        // Manajemen UI untuk Kartu Izin Aksesibilitas
        if (hasAccessibility) {
            tvStatusAccessibility.text = "AKTIF ✓"
            tvStatusAccessibility.setTextColor(Color.parseColor("#00E676"))
            btnAccessibility.text = "Service Aktif"
            btnAccessibility.backgroundTintList = android.content.res.ColorStateList.valueOf(Color.parseColor("#252530"))
            btnAccessibility.setTextColor(Color.parseColor("#555566"))
            btnAccessibility.isEnabled = false
        } else {
            tvStatusAccessibility.text = "Dibutuhkan"
            tvStatusAccessibility.setTextColor(Color.parseColor("#00E676"))
            btnAccessibility.text = "Aktifkan Service"
            btnAccessibility.backgroundTintList = android.content.res.ColorStateList.valueOf(Color.parseColor("#00E676"))
            btnAccessibility.setTextColor(Color.WHITE)
            btnAccessibility.isEnabled = true
        }
    }

    private fun isAccessibilityServiceEnabled(context: Context, service: Class<*>): Boolean {
        val expectedComponentName = "${context.packageName}/${service.name}"
        val settingValue = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false

        val splitter = TextUtils.SimpleStringSplitter(':')
        splitter.setString(settingValue)
        while (splitter.hasNext()) {
            if (splitter.next().equals(expectedComponentName, ignoreCase = true)) {
                return true
            }
        }
        return false
    }
}
