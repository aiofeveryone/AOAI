package com.aoai.chat.core.brain.aoai01

/**
 * aoai01의 자기 성찰 결과물
 */
data class ReviewReport(
    val ok: Boolean,
    val score: Int,           // 0~100
    val reasons: List<String>,
    val latencyMs: Long = 0L   // 추가: 실행 속도 데이터
)

object AOAI01Review {
    fun review(
        input: AOAI01Input, 
        responseText: String, 
        route: AOAI01Route, 
        providerName: String,
        latencyMs: Long = 0L   // 파라미터 추가
    ): ReviewReport {
        val reasons = mutableListOf<String>()
        var score = 80

        if (responseText.length < 5) {
            score -= 40
            reasons += "TOO_SHORT"
        }
        
        // 속도 기반 감점 로직 (예: 5초 이상 걸리면 지능적 감점)
        if (latencyMs > 5000) {
            score -= 10
            reasons += "SLOW_RESPONSE"
        }

        return ReviewReport(
            ok = score > 40,
            score = score,
            reasons = reasons,
            latencyMs = latencyMs
        )
    }
}
