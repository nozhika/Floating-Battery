package com.example.floatingbattery

import android.content.Intent
import android.content.res.Configuration
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.Menu
import android.view.MenuItem
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import com.google.android.material.button.MaterialButton

class MainActivity : AppCompatActivity() {

    companion object {
        private const val PREFS_NAME = "app_preferences"
        private const val DARK_MODE = "dark_mode"
    }

    private lateinit var tvStatus: TextView
    private lateinit var tvPosition: TextView
    private lateinit var btnGrantPermission: MaterialButton
    private lateinit var btnStart: MaterialButton
    private lateinit var btnStop: MaterialButton

    override fun onCreate(savedInstanceState: Bundle?) {

        applySavedTheme()

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        tvStatus = findViewById(R.id.tvStatus)
        tvPosition = findViewById(R.id.tvPosition)

        btnGrantPermission = findViewById(R.id.btnGrantPermission)
        btnStart = findViewById(R.id.btnStart)
        btnStop = findViewById(R.id.btnStop)

        val toolbar = findViewById<com.google.android.material.appbar.MaterialToolbar>(
            R.id.topAppBar
        )

        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(true)

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

                val serviceIntent = Intent(
                    this,
                    FloatingBatteryService::class.java
                )

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    startForegroundService(serviceIntent)
                } else {
                    startService(serviceIntent)
                }

                tvStatus.text = getString(R.string.status_running)

            } else {

                tvStatus.text = getString(
                    R.string.permission_missing
                )
            }
        }

        btnStop.setOnClickListener {

            stopService(
                Intent(
                    this,
                    FloatingBatteryService::class.java
                )
            )

            tvStatus.text = getString(
                R.string.status_ready
            )
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {

        menuInflater.inflate(
            R.menu.menu_theme,
            menu
        )

        val themeItem = menu.findItem(
            R.id.action_theme
        )

        updateThemeIcon(themeItem)

        return true
    }

    override fun onOptionsItemSelected(
        item: MenuItem
    ): Boolean {

        return when (item.itemId) {

            R.id.action_theme -> {

                toggleTheme()

                true
            }

            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun toggleTheme() {

        val darkMode = !isDarkModeEnabled()

        getSharedPreferences(
            PREFS_NAME,
            MODE_PRIVATE
        )
            .edit()
            .putBoolean(
                DARK_MODE,
                darkMode
            )
            .apply()

        AppCompatDelegate.setDefaultNightMode(

            if (darkMode) {
                AppCompatDelegate.MODE_NIGHT_YES
            } else {
                AppCompatDelegate.MODE_NIGHT_NO
            }
        )
    }

    private fun applySavedTheme() {

        val prefs = getSharedPreferences(
            PREFS_NAME,
            MODE_PRIVATE
        )

        if (prefs.contains(DARK_MODE)) {

            val darkMode = prefs.getBoolean(
                DARK_MODE,
                false
            )

            AppCompatDelegate.setDefaultNightMode(

                if (darkMode) {
                    AppCompatDelegate.MODE_NIGHT_YES
                } else {
                    AppCompatDelegate.MODE_NIGHT_NO
                }
            )

        } else {

            AppCompatDelegate.setDefaultNightMode(
                AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
            )
        }
    }

    private fun isDarkModeEnabled(): Boolean {

        return when (
            AppCompatDelegate.getDefaultNightMode()
        ) {

            AppCompatDelegate.MODE_NIGHT_YES -> true

            AppCompatDelegate.MODE_NIGHT_NO -> false

            else -> {

                val currentNightMode =
                    resources.configuration.uiMode and
                        Configuration.UI_MODE_NIGHT_MASK

                currentNightMode ==
                    Configuration.UI_MODE_NIGHT_YES
            }
        }
    }

    private fun updateThemeIcon(
        item: MenuItem?
    ) {

        if (item == null) {
            return
        }

        val darkMode = isDarkModeEnabled()

        if (darkMode) {

            item.setIcon(
                R.drawable.ic_light_mode
            )

            item.title = getString(
                R.string.theme_switch_to_light
            )

            item.contentDescription = getString(
                R.string.theme_switch_to_light
            )

        } else {

            item.setIcon(
                R.drawable.ic_dark_mode
            )

            item.title = getString(
                R.string.theme_switch_to_dark
            )

            item.contentDescription = getString(
                R.string.theme_switch_to_dark
            )
        }
    }

    override fun onResume() {

        super.onResume()

        updateStatus()
        updatePositionLabel()
    }

    private fun updateStatus() {

        val granted = Settings.canDrawOverlays(
            this
        )

        tvStatus.text = if (granted) {

            getString(
                R.string.permission_granted
            )

        } else {

            getString(
                R.string.permission_missing
            )
        }

        btnGrantPermission.isEnabled = !granted
    }

    private fun updatePositionLabel() {

        val prefs = getSharedPreferences(
            FloatingBatteryService.PREFS_NAME,
            MODE_PRIVATE
        )

        val hasX = prefs.contains(
            FloatingBatteryService.POSITION_X
        )

        val hasY = prefs.contains(
            FloatingBatteryService.POSITION_Y
        )

        if (hasX && hasY) {

            val x = prefs.getInt(
                FloatingBatteryService.POSITION_X,
                0
            )

            val y = prefs.getInt(
                FloatingBatteryService.POSITION_Y,
                200
            )

            tvPosition.text =
                getString(
                    R.string.position_saved
                ) + " • x=$x, y=$y"

        } else {

            tvPosition.text =
                getString(
                    R.string.position_default
                )
        }
    }
}
