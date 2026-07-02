package com.rycl.autoclicker

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.text.TextUtils
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val btnOverlay = findViewById<Button>(R.id.btn_permit_overlay)
        val btnAccessibility = findViewById<Button>(R.id.btn_permit_accessibility)
        val btnStartService = findViewById<Button>(R.id.btn_start_service)

        // 1. Handle Izin Overlay
        btnOverlay.setOnClickListener {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
                val intent = Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:$packageName")
                )
                startActivity(intent)
            } else {
                Toast.makeText(this, "Izin Overlay sudah aktif!", Toast.LENGTH_SHORT).show()
            }
        }

        // 2. Handle Izin Accessibility
        btnAccessibility.setOnClickListener {
            if (!isAccessibilityServiceEnabled(this, ClickerService::class.java)) {
                val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                startActivity(intent)
            } else {
                Toast.makeText(this, "Izin Accessibility sudah aktif!", Toast.LENGTH_SHORT).show()
            }
        }

        // 3. Jalankan Floating Widget
        btnStartService.setOnClickListener {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
                Toast.makeText(this, "Beri izin Overlay terlebih dahulu!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (!isAccessibilityServiceEnabled(this, ClickerService::class.java)) {
                Toast.makeText(this, "Aktifkan Accessibility Service terlebih dahulu!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Start service melayang
            startService(Intent(this, FloatingService::class.java))
            // Minimize app biar langsung kelihatan widget-nya di home screen
            moveTaskToBack(true)
        }
    }

    // Fungsi utilitas untuk cek status Accessibility Service
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
