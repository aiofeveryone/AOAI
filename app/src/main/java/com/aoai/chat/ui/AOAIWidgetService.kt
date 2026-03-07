package com.aoai.chat.ui

import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import com.aoai.chat.R
import com.aoai.chat.data.ChatHistoryStore
import com.aoai.chat.data.ChatRole
import com.aoai.chat.data.StoredChatMessage

class AOAIWidgetService : RemoteViewsService() {
    override fun onGetViewFactory(intent: Intent): RemoteViewsFactory {
        return AOAIWidgetFactory(applicationContext)
    }
}

class AOAIWidgetFactory(private val context: Context) : RemoteViewsService.RemoteViewsFactory {
    private var messages: List<StoredChatMessage> = emptyList()

    override fun onCreate() {}

    override fun onDataSetChanged() {
        // SharedPreferences에서 최신 대화 내역 로드
        messages = ChatHistoryStore.load(context).reversed()
    }

    override fun onDestroy() {}

    override fun getCount(): Int = messages.size

    override fun getViewAt(position: Int): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.aoai_widget_item)
        val msg = messages[position]
        
        val prefix = if (msg.role == ChatRole.USER) "ME: " else "AI: "
        views.setTextViewText(R.id.widget_item_text, prefix + msg.text)
        
        // 아이템 클릭 시 앱 실행을 위한 FillInIntent 설정 (필요시)
        val fillInIntent = Intent()
        views.setOnClickFillInIntent(R.id.widget_item_text, fillInIntent)
        
        return views
    }

    override fun getLoadingView(): RemoteViews? = null
    override fun getViewTypeCount(): Int = 1
    override fun getItemId(position: Int): Long = position.toLong()
    override fun hasStableIds(): Boolean = true
}
