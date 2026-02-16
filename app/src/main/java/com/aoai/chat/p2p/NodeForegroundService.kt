package com.aoai.chat.p2p

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.aoai.chat.MainActivity
import com.aoai.chat.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Foreground service that keeps AOAI node alive even when screen is off.
 *
 * Policy:
 * - Service stays alive while "Participation" is ON.
 * - Node participates ONLY when Wi-Fi is eligible (Wi-Fi + INTERNET + VALIDATED).
 * - When Wi-Fi becomes ineligible, node stops immediately and waits.
 * - When Wi-Fi becomes eligible again, node starts automatically.
 *
 * ✅ Gatekeeper가 Edge-trigger면,
 *    이 서비스는 "start/stop을 한 번씩" 확실하게 실행하는 쪽에 집중하면 됨.
 */
class NodeForegroundService : Service() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // startForeground 중복 호출 방지
    private val inForeground = AtomicBoolean(false)

    // NetworkGatekeeper 중복 등록 방지
    private val gatekeeperRegistered = AtomicBoolean(false)

    // 노드 시작 중복 호출 방지
    private val starting = AtomicBoolean(false)

    // ✅ retry 코루틴을 취소하기 위한 Job
    @Volatile
    private var startJob: Job? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        ensureNotificationChannel()
        Log.i(TAG, "onCreate")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        Log.i(TAG, "onStartCommand action=$action")

        try {
            when (action) {
                ACTION_START -> {
                    enterForegroundIfNeeded()
                    registerWifiGatekeeperIfNeeded()

                    // ✅ Gatekeeper.register()가 "현재 상태를 1회 평가"해서 통지하므로
                    //    여기서 sync를 굳이 또 때릴 필요가 없음.
                    //    다만, Gatekeeper가 아직 Edge-trigger로 수정되기 전/후 혼재 시 안정성을 위해
                    //    한 번만 부드럽게 맞춰주고 싶다면 아래를 유지해도 됨.
                    // syncNodeWithCurrentWifiState()
                }

                ACTION_STOP -> {
                    unregisterWifiGatekeeperIfNeeded()
                    stopNodeBecauseWifiLost() // stop + retry cancel 포함
                    stopForeground(STOP_FOREGROUND_REMOVE)
                    stopSelf()
                }

                else -> {
                    // 시스템 재시작 케이스 (START_STICKY)
                    enterForegroundIfNeeded()
                    registerWifiGatekeeperIfNeeded()
                    // syncNodeWithCurrentWifiState() // 보통 불필요
                }
            }
        } catch (t: Throwable) {
            Log.e(TAG, "onStartCommand crashed", t)
            try {
                stopNodeBecauseWifiLost()
            } catch (_: Throwable) {}
            stopSelf()
            return START_NOT_STICKY
        }

        return START_STICKY
    }

    override fun onDestroy() {
        Log.i(TAG, "onDestroy")
        unregisterWifiGatekeeperIfNeeded()
        stopNodeBecauseWifiLost()
        serviceScope.cancel()
        super.onDestroy()
    }

    /**
     * (선택) 수동 동기화가 정말 필요할 때만 사용.
     * Gatekeeper가 register 시점에 상태를 1회 통지하면, 보통 중복이라 제거해도 됨.
     */
    private fun syncNodeWithCurrentWifiState() {
        val ok = NetworkGatekeeper.isWifiValidatedNow(this)
        Log.i(TAG, "syncNodeWithCurrentWifiState wifiEligible=$ok")
        if (ok) startNodeWithRetry() else stopNodeBecauseWifiLost()
    }

    private fun enterForegroundIfNeeded() {
        if (inForeground.compareAndSet(false, true)) {
            Log.i(TAG, "enterForeground")
            startForeground(NOTIF_ID, buildNotification())
        }
    }

    private fun registerWifiGatekeeperIfNeeded() {
        if (!gatekeeperRegistered.compareAndSet(false, true)) return

        Log.i(TAG, "registerWifiGatekeeper")
        NetworkGatekeeper.register(
            context = this,
            listener = object : NetworkGatekeeper.Listener {

                // ✅ Edge-trigger Gatekeeper면 여기 콜백은 "진입/이탈"에서만 옴
                override fun onWifiEligible() {
                    Log.i(TAG, "onWifiEligible (edge)")
                    startNodeWithRetry()
                }

                override fun onWifiIneligible() {
                    Log.i(TAG, "onWifiIneligible (edge)")
                    stopNodeBecauseWifiLost()
                }
            }
        )
    }

    private fun unregisterWifiGatekeeperIfNeeded() {
        if (!gatekeeperRegistered.compareAndSet(true, false)) return
        Log.i(TAG, "unregisterWifiGatekeeper")
        NetworkGatekeeper.unregister(this)
    }

    private fun startNodeWithRetry() {
        // ✅ Wi-Fi eligible이 아닐 때는 절대 시작하지 않음
        if (!NetworkGatekeeper.isWifiValidatedNow(this)) return

        // ✅ 이미 노드가 실행 중이면 더 할 필요 없음
        if (P2PConnectionManager.isNodeRunning()) {
            Log.i(TAG, "startNodeWithRetry skipped (node already running)")
            return
        }

        // ✅ 기존 retry 작업이 있으면 취소하고 새로 시작(가장 안전)
        startJob?.cancel()
        startJob = null

        // ✅ start 호출 난사 방지
        if (!starting.compareAndSet(false, true)) {
            Log.i(TAG, "startNodeWithRetry skipped (already starting)")
            return
        }

        Log.i(TAG, "startNodeWithRetry begin")

        val job = serviceScope.launch {
            try {
                var attempt = 0
                while (true) {
                    // 재시도 중에도 Wi-Fi가 끊기면 즉시 중단
                    if (!NetworkGatekeeper.isWifiValidatedNow(this@NodeForegroundService)) {
                        Log.i(TAG, "retry loop: wifi lost -> stop")
                        stopNodeBecauseWifiLost()
                        break
                    }

                    // 노드가 누군가에 의해 이미 켜졌다면 종료
                    if (P2PConnectionManager.isNodeRunning()) {
                        Log.i(TAG, "retry loop: node already running -> done")
                        break
                    }

                    try {
                        Log.i(TAG, "calling P2PConnectionManager.startNode()")
                        P2PConnectionManager.startNode(applicationContext)
                        Log.i(TAG, "startNode OK")
                        break
                    } catch (t: Throwable) {
                        attempt++
                        Log.e(TAG, "startNode failed attempt=$attempt", t)
                        val backoff = (1000L * attempt).coerceAtMost(10_000L)
                        delay(backoff)
                    }
                }
            } finally {
                starting.set(false)
                Log.i(TAG, "startNodeWithRetry end (starting=false)")
            }
        }

        startJob = job
    }

    /**
     * ✅ Wi-Fi 이탈 시:
     * - 진행 중인 retry job 취소
     * - starting 플래그 리셋
     * - 노드 stop
     */
    private fun stopNodeBecauseWifiLost() {
        startJob?.cancel()
        startJob = null

        starting.set(false)
        stopNodeSafely()
    }

    private fun stopNodeSafely() {
        try {
            Log.i(TAG, "stopNodeSafely")
            P2PConnectionManager.stopNode()
        } catch (t: Throwable) {
            Log.e(TAG, "stopNodeSafely error", t)
        }
    }

    private fun ensureNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val channel = NotificationChannel(
                CHANNEL_ID,
                "AOAI 참여",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "AOAI 분산 네트워크 참여 상태를 표시합니다."
                setShowBadge(false)
            }
            nm.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(): Notification {
        val launchIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or
                    if (Build.VERSION.SDK_INT >= 23) PendingIntent.FLAG_IMMUTABLE else 0
        )

        val stopIntent = Intent(this, NodeForegroundService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPendingIntent = PendingIntent.getService(
            this,
            1,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or
                    if (Build.VERSION.SDK_INT >= 23) PendingIntent.FLAG_IMMUTABLE else 0
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("AOAI 참여 중")
            .setContentText("Wi-Fi에서만 다른 사용자의 AI 요청 처리에 참여합니다.")
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(pendingIntent)
            .addAction(0, "참여 해제", stopPendingIntent)
            .build()
    }

    companion object {
        private const val TAG = "AOAI-FGS"

        const val ACTION_START = "com.aoai.chat.p2p.NodeForegroundService.START"
        const val ACTION_STOP = "com.aoai.chat.p2p.NodeForegroundService.STOP"

        private const val CHANNEL_ID = "aoai_participation_channel"
        private const val NOTIF_ID = 1001

        fun start(context: Context) {
            val i = Intent(context, NodeForegroundService::class.java).apply {
                action = ACTION_START
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(i)
            } else {
                context.startService(i)
            }
        }

        fun stop(context: Context) {
            val i = Intent(context, NodeForegroundService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(i)
        }
    }
}