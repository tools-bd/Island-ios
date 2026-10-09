package com.island.app

import android.app.Activity
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.SeekBar
import android.widget.TextView

class MainActivity : Activity() {

    private lateinit var status: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val prefs = getSharedPreferences("island", MODE_PRIVATE)
        val pad = (16 * resources.displayMetrics.density).toInt()

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(pad, pad, pad, pad)
        }

        status = TextView(this).apply { textSize = 16f }
        root.addView(status)

        root.addView(Button(this).apply {
            text = "১) Accessibility চালু করুন (Island সার্ভিস)"
            setOnClickListener {
                startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
            }
        })

        root.addView(Button(this).apply {
            text = "২) Battery optimization বন্ধ করুন"
            setOnClickListener { askIgnoreBattery() }
        })

        root.addView(slider(prefs, "প্রস্থ (Width)", "w", 60, 360, 120))
        root.addView(slider(prefs, "উচ্চতা (Height)", "h", 24, 120, 34))
        root.addView(slider(prefs, "উপর-নিচ (Y)", "y", 0, 500, 8))
        root.addView(slider(prefs, "ডান-বাম (X)", "x", -200, 200, 0))

        root.addView(TextView(this).apply {
            text = "টিপস: Island এর উপর আঙুল টেনে সরানো যায়, দুই আঙুলে পিঞ্চ করে ছোট-বড় করা যায়।"
            setPadding(0, pad, 0, 0)
        })

        setContentView(ScrollView(this).apply { addView(root) })
    }

    override fun onResume() {
        super.onResume()
        val enabled = Settings.Secure
            .getString(contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES)
            ?.contains(packageName) == true
        status.text = if (enabled) "✅ Island চালু আছে" else "❌ Island বন্ধ — Accessibility চালু করুন"
    }

    private fun askIgnoreBattery() {
        val pm = getSystemService(POWER_SERVICE) as PowerManager
        if (!pm.isIgnoringBatteryOptimizations(packageName)) {
            startActivity(
                Intent(
                    Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                    Uri.parse("package:$packageName")
                )
            )
        }
    }

    private fun slider(
        prefs: SharedPreferences,
        label: String,
        key: String,
        min: Int,
        max: Int,
        def: Int
    ): LinearLayout {
        val box = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        box.setPadding(0, 24, 0, 0)
        box.addView(TextView(this).apply { text = label })
        box.addView(SeekBar(this).apply {
            this.max = max - min
            progress = prefs.getFloat(key, def.toFloat()).toInt() - min
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(sb: SeekBar?, p: Int, fromUser: Boolean) {
                    if (fromUser) prefs.edit().putFloat(key, (p + min).toFloat()).apply()
                }
                override fun onStartTrackingTouch(sb: SeekBar?) {}
                override fun onStopTrackingTouch(sb: SeekBar?) {}
            })
        })
        return box
    }
}
