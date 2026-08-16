package com.example.floatingbattery

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.PixelFormat
import android.os.BatteryManager
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.TextView
import androidx.core.app.NotificationCompat
import kotlin.math.max

class FloatingBatteryService : Service() {

    companion object {
        const val PREFS_NAME = "floating_battery_preferences"
        const val POSITION_X = "position_x"
        const val POSITION_Y = "position_y"
    }

    private lateinit var windowManager: WindowManager
    private lateinit var floatingView: View
    private lateinit var params: WindowManager.LayoutParams

    private var initialX = 0
    private var initialY = 0
    private var initialTouchX = 0f
    private var initialTouchY = 0f

    private val batteryReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
            val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
            if (level >= 0 && scale > 0) {
                updateBatteryText((level * 100) / scale)
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        startForegroundServiceWithNotification()
        setupFloatingView()
        registerReceiver(batteryReceiver, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
    }

    private fun setupFloatingView() {
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        floatingView = LayoutInflater.from(this).inflate(R.layout.floating_battery, null)

        val layoutFlag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            layoutFlag,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            val prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
            x = prefs.getInt(POSITION_X, 16)
            y = prefs.getInt(POSITION_Y, 200)
        }

        windowManager.addView(floatingView, params)

        floatingView.setOnTouchListener { _, event ->
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = params.x
                    initialY = params.y
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val desiredX = initialX + (event.rawX - initialTouchX).toInt()
                    val desiredY = initialY + (event.rawY - initialTouchY).toInt()
                    val bounds = getSafeBounds()
                    params.x = desiredX.coerceIn(bounds.first, bounds.second.first)
                    params.y = desiredY.coerceIn(0, bounds.second.second)
                    windowManager.updateViewLayout(floatingView, params)
                    true
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    savePosition()
                    true
                }
                else -> false
            }
        }
    }

    private fun getSafeBounds(): Pair<Int, Pair<Int, Int>> {
        val metrics = resources.displayMetrics
        val screenWidth = metrics.widthPixels
        val screenHeight = metrics.heightPixels
        val width = floatingView.width.takeIf { it > 0 } ?: 120
        val height = floatingView.height.takeIf { it > 0 } ?: 48
        return Pair(0, Pair(max(0, screenWidth - width), max(0, screenHeight - height)))
    }

    private fun savePosition() {
        getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
            .edit()
            .putInt(POSITION_X, params.x)
            .putInt(POSITION_Y, params.y)
            .apply()
    }

    private fun updateBatteryText(percent: Int) {
        if (!::floatingView.isInitialized) return
        floatingView.findViewById<TextView>(R.id.tvBatteryPercent)?.text = "$percent%"
    }

    private fun startForegroundServiceWithNotification() {
        val channelId = "floating_battery_channel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Floating Battery",
                NotificationManager.IMPORTANCE_MIN
            )
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("Floating Battery aktif")
            .setContentText("Indikator baterai sedang ditampilkan")
            .setSmallIcon(android.R.drawable.ic_lock_idle_charging)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setOngoing(true)
            .build()

        startForeground(1, notification)
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        if (::floatingView.isInitialized) {
            savePosition()
            windowManager.removeView(floatingView)
        }
        unregisterReceiver(batteryReceiver)
        super.onDestroy()
    }
}
