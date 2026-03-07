package com.aoai.chat.ui

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.RemoteViews
import com.aoai.chat.MainActivity
import com.aoai.chat.R
import com.aoai.chat.AOAIApplication

/**
 * [aoai01 Home Screen Widget: 리스트형 대화 위젯]
 * 홈 화면에서 최신 대화 내역을 확인하고 즉시 앱으로 진입합니다.
 */
class AOAIWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == "com.aoai.chat.UPDATE_WIDGET" || 
            intent.action == AppWidgetManager.ACTION_APPWIDGET_UPDATE) {
            
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val component = ComponentName(context, AOAIWidgetProvider::class.java)
            val ids = appWidgetManager.getAppWidgetIds(component)
            
            // 리스트 데이터 갱신 알림
            appWidgetManager.notifyAppWidgetViewDataChanged(ids, R.id.widget_chat_list)
            
            for (id in ids) {
                updateAppWidget(context, appWidgetManager, id)
            }
        }
    }

    companion object {
        fun updateAppWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
            val views = RemoteViews(context.packageName, R.layout.aoai_widget_layout)
            
            // 1. 리스트 뷰 서비스 연결
            val intent = Intent(context, AOAIWidgetService::class.java).apply {
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                data = Uri.parse(toUri(Intent.URI_INTENT_SCHEME))
            }
            views.setRemoteAdapter(R.id.widget_chat_list, intent)
            views.setEmptyView(R.id.widget_chat_list, R.id.widget_empty_view)

            // 2. 상태 및 에너지 업데이트
            val app = context.applicationContext as? AOAIApplication
            val agent = app?.aoai01
            if (agent != null) {
                val energy = agent.lifeSystem.vitality.energy.value.toInt()
                views.setTextViewText(R.id.widget_status, "AOAI 01: ${agent.lifeSystem.getStatus()}")
                views.setProgressBar(R.id.widget_energy_bar, 200, energy, false)
            }

            // 3. 클릭 액션 설정 (앱 실행)
            val mainIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val pendingIntent = PendingIntent.getActivity(
                context, 0, mainIntent, 
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_action_btn, pendingIntent)
            views.setPendingIntentTemplate(R.id.widget_chat_list, pendingIntent)

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }
}
