package com.aoai.chat.ui

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

/**
 * [AOAI Quick Thought Receiver]
 * 알림창이나 위젯에서의 빠른 입력을 처리하기 위한 리시버입니다.
 */
class QuickThoughtReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val text = intent.getStringExtra("thought_text")
        Log.d("QuickThought", "Received quick thought: $text")
        
        // 향후 구현: 백그라운드에서 에이전트에게 메시지 전달 로직 추가
    }
}
