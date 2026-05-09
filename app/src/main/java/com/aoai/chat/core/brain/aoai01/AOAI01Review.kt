package com.aoai.chat.core.brain.aoai01

import android.content.Context
import com.aoai.chat.R

/**
 * aoai01의 자기 성찰 결과물
 */
data class ReviewReport(
    val ok: Boolean,
    val score: Int,           // 0~100
    val reasons: List<String>,
    val latencyMs: Long = 0L   // 실행 속도 데이터
)

object AOAI01Review {
    fun review(
        context: Context,
        input: AOAI01Input, 
        responseText: String, 
        route: AOAI01Route, 
        providerName: String,
        latencyMs: Long = 0L
    ): ReviewReport {
        val reasons = mutableListOf<String>()
        var score = 90 // 업그레이드된 기본 지능 점수

        if (responseText.length < 5) {
            score -= 60
            reasons += context.getString(R.string.review_reason_critical_short)
        } else if (responseText.length < 20) {
            score -= 25
            reasons += context.getString(R.string.review_reason_too_short)
        }

        // 반복 문구 감지 (지능 저하 징후)
        val words = responseText.split(" ").filter { it.length > 2 }
        val uniqueWords = words.toSet()
        if (words.size > 10 && uniqueWords.size.toDouble() / words.size < 0.4) {
            score -= 30
            reasons += context.getString(R.string.review_reason_repetitive)
        }
        
        // 속도 기반 감점 로직 (더 엄격해진 기준)
        if (latencyMs > 6000) {
            score -= 25
            reasons += context.getString(R.string.review_reason_very_slow)
        } else if (latencyMs > 3000) {
            score -= 10
            reasons += context.getString(R.string.review_reason_slow)
        }

        // 특정 에러 키워드 감지
        val errorKeywords = context.getString(R.string.review_error_keywords).split(",")
        if (errorKeywords.any { responseText.contains(it, ignoreCase = true) } && responseText.length < 100) {
            score -= 30
            reasons += context.getString(R.string.review_reason_error_detected)
        }

        return ReviewReport(
            ok = score > 45,
            score = score.coerceIn(0, 100),
            reasons = reasons,
            latencyMs = latencyMs
        )
    }
}
