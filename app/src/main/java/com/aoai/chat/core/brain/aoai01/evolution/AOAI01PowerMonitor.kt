package com.aoai.chat.core.brain.aoai01.evolution

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager

/**
 * [aoai01 에너지 감각계 고도화]
 * 전력 수급 상태, 충전 유형, 전력 품질을 감시하여 지능의 생존 전략을 결정합니다.
 */
object AOAI01PowerMonitor {

    enum class PowerSource { AC, USB, WIRELESS, NONE }

    data class PowerState(
        val batteryPct: Int,
        val isCharging: Boolean,
        val source: PowerSource,
        val isLowPower: Boolean,      // 15% 이하
        val isCriticalPower: Boolean, // 5% 이하
        val thermalState: Int         // 배터리 온도 (0.1도 단위)
    )

    fun getCurrentPower(context: Context): PowerState {
        val intent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED)) ?: return PowerState(100, false, PowerSource.NONE, false, false, 0)
        
        val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
        val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
        val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
        val plugType = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1)
        val temperature = intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0)
        
        val batteryPct = (level / scale.toFloat() * 100).toInt()
        val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING || 
                         status == BatteryManager.BATTERY_STATUS_FULL
        
        val source = when (plugType) {
            BatteryManager.BATTERY_PLUGGED_AC -> PowerSource.AC
            BatteryManager.BATTERY_PLUGGED_USB -> PowerSource.USB
            BatteryManager.BATTERY_PLUGGED_WIRELESS -> PowerSource.WIRELESS
            else -> PowerSource.NONE
        }

        return PowerState(
            batteryPct = batteryPct,
            isCharging = isCharging,
            source = source,
            isLowPower = batteryPct <= 15,
            isCriticalPower = batteryPct <= 5,
            thermalState = temperature
        )
    }
}
