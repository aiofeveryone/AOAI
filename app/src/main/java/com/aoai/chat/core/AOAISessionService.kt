package com.aoai.chat.core

import android.app.*
import android.content.Context
import android.content.Intent
import android.net.wifi.WifiManager
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.util.Log
import androidx.core.app.NotificationCompat
import com.aoai.chat.MainActivity
import com.aoai.chat.R

/**
 * AOAI 통신 및 세션 유지를 위한 포그라운드 서비스 (분산 AI 세션)
 */
class AOAISessionService : Service() {

    private var wakeLock: PowerManager.WakeLock? = null
    private var wifiLock: WifiManager.WifiLock? = null

    companion object {
        private const val CHANNEL_ID = "aoai_session_channel"
        private const val NOTIFICATION_ID = 1001
        private const val TAG = "AOAISessionService"

        fun startService(context: Context) {
            val intent = Intent(context, AOAISessionService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stopService(context: Context) {
            val intent = Intent(context, AOAISessionService::class.java)
            context.stopService(intent)
        }
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, createNotification())

        // ✅ WakeLock 획득 (CPU 절전 방지)
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "AOAI:KeepAliveWakeLock").apply {
            acquire()
        }

        // ✅ WiFi Lock 획득 (WIFI 절전 방지)
        val wifiManager = applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        wifiLock = wifiManager.createWifiLock(WifiManager.WIFI_MODE_FULL_HIGH_PERF, "AOAI:KeepAliveWifiLock").apply {
            acquire()
        }

        Log.i(TAG, "KeepAlive Service Created and Locks Acquired")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // 시스템에 의해 서비스가 종료되어도 다시 시작되도록 설정
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        wakeLock?.let { if (it.isHeld) it.release() }
        wifiLock?.let { if (it.isHeld) it.release() }
        Log.i(TAG, "KeepAlive Service Destroyed and Locks Released")
    }

    private fun createNotification(): Notification {
        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, notificationIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("AOAI 서비스 활성화 중")
            .setContentText("안정적인 통신 연결을 유지하고 있습니다.")
            .setSmallIcon(R.mipmap.ic_launcher_foreground) // 아이콘 리소스 확인 필요
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(Notification.CATEGORY_SERVICE)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                CHANNEL_ID,
                "AOAI Keep Alive Channel",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "AOAI 통신 유지 서비스 알림"
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(serviceChannel)
        }
    }
}
