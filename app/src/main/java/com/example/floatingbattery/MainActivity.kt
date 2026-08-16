package com.example.floatingbattery

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private lateinit var tvStatus: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        tvStatus = findViewById(R.id.tvStatus)
        val btnGrantPermission = findViewById<Button>(R.id.btnGrantPermission)
        val btnStart = findViewById<Button>(R.id.btnStart)
        val btnStop = findViewById<Button>(R.id.btnStop)

        btnGrantPermission.setOnClickListener {
            if (!Settings.canDrawOverlays(this)) {
                val intent = Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:$packageName")
                )
                startActivity(intent)
            }
        }

        btnStart.setOnClickListener {
            if (Settings.canDrawOverlays(this)) {
                val serviceIntent = Intent(this, FloatingBatteryService::class.java)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    startForegroundService(serviceIntent)
                } else {
                    startService(serviceIntent)
                }
            } else {
                tvStatus.text = "Status izin: belum diizinkan. Tekan tombol izin dulu."
            }
        }

        btnStop.setOnClickListener {
            stopService(Intent(this, FloatingBatteryService::class.java))
        }
    }

    override fun onResume() {
        super.onResume()
        val granted = Settings.canDrawOverlays(this)
        tvStatus.text = if (granted) {
            "Status izin: sudah diizinkan"
        } else {
            "Status izin: belum diizinkan"
        }
    }
}
