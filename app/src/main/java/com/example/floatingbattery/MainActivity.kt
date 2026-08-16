package com.example.floatingbattery

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton

class MainActivity : AppCompatActivity() {

    private lateinit var tvStatus: TextView
    private lateinit var tvPosition: TextView
    private lateinit var btnGrantPermission: MaterialButton
    private lateinit var btnStart: MaterialButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        tvStatus = findViewById(R.id.tvStatus)
        tvPosition = findViewById(R.id.tvPosition)
        btnGrantPermission = findViewById(R.id.btnGrantPermission)
        btnStart = findViewById(R.id.btnStart)
        val btnStop = findViewById<MaterialButton>(R.id.btnStop)

        btnGrantPermission.setOnClickListener {
            if (!Settings.canDrawOverlays(this)) {
                startActivity(
                    Intent(
                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:$packageName")
                    )
                )
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
                tvStatus.text = getString(R.string.permission_missing)
            }
        }

        btnStop.setOnClickListener {
            stopService(Intent(this, FloatingBatteryService::class.java))
            tvStatus.text = getString(R.string.status_ready)
        }
    }

    override fun onResume() {
        super.onResume()
        val granted = Settings.canDrawOverlays(this)
        tvStatus.text = if (granted) {
            getString(R.string.permission_granted)
        } else {
            getString(R.string.permission_missing)
        }
        btnGrantPermission.isEnabled = !granted
        updatePositionLabel()
    }

    private fun updatePositionLabel() {
        val prefs = getSharedPreferences(FloatingBatteryService.PREFS_NAME, MODE_PRIVATE)
        if (prefs.contains(FloatingBatteryService.POSITION_X) && prefs.contains(FloatingBatteryService.POSITION_Y)) {
            val x = prefs.getInt(FloatingBatteryService.POSITION_X, 0)
            val y = prefs.getInt(FloatingBatteryService.POSITION_Y, 200)
            tvPosition.text = getString(R.string.position_saved) + " • x=$x, y=$y"
        } else {
            tvPosition.text = "Posisi default"
        }
    }
}
