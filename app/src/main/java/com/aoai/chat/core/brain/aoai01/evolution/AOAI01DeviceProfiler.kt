package com.aoai.chat.core.brain.aoai01.evolution

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Build
import android.os.PowerManager
import com.aoai.chat.core.brain.aoai01.DeviceStateInfo

/**
 * 기기의 하드웨어 성능과 실시간 상태를 분석하여 RuntimeGrade를 산출합니다.
 */
object AOAI01DeviceProfiler {

    enum class RuntimeGrade { S, A, B, C }

    fun getDeviceState(context: Context): DeviceStateInfo {
        val intent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val level = intent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = intent?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
        val pct = if (scale > 0) (level * 100 / scale.toFloat()).toInt() else -1
        
        val status = intent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL

        return DeviceStateInfo(
            batteryPct = pct,
            isCharging = isCharging,
            thermalState = getThermalState(context)
        )
    }

    /**
     * 기기 사양에 따른 등급을 판정합니다. (S: 최상, A: 우수, B: 일반, C: 저사양)
     */
    fun calculateGrade(context: Context): RuntimeGrade {
        val actManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val memInfo = ActivityManager.MemoryInfo()
        actManager.getMemoryInfo(memInfo)
        
        val totalRamGb = memInfo.totalMem / (1024 * 1024 * 1024)
        val cores = Runtime.getRuntime().availableProcessors()

        return when {
            totalRamGb >= 8 && cores >= 8 -> RuntimeGrade.S
            totalRamGb >= 4 && cores >= 4 -> RuntimeGrade.A
            totalRamGb >= 2 -> RuntimeGrade.B
            else -> RuntimeGrade.C
        }
    }

    private fun getThermalState(context: Context): String {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
            when (powerManager.currentThermalStatus) {
                PowerManager.THERMAL_STATUS_NONE -> "NORMAL"
                PowerManager.THERMAL_STATUS_LIGHT, 
                PowerManager.THERMAL_STATUS_MODERATE -> "MODERATE"
                PowerManager.THERMAL_STATUS_SEVERE,
                PowerManager.THERMAL_STATUS_CRITICAL,
                PowerManager.THERMAL_STATUS_EMERGENCY,
                PowerManager.THERMAL_STATUS_SHUTDOWN -> "CRITICAL"
                else -> "NORMAL"
            }
        } else {
            "NORMAL"
        }
    }
}
