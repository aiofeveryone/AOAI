package com.aoai.chat.core.brain.aoai01

/**
 * 질문의 텍스트를 분석하여 의도(Intent)와 난이도(Complexity)를 판별합니다.
 */
object AOAI01IntentRouter {

    fun analyze(text: String): IntentAnalysis {
        val t = text.trim().lowercase()
        
        // 1. 복잡도 판정 기준
        val isDeepAnalysisNeeded = t.length > 120 || 
                                  t.contains("왜 ") || t.contains("어떻게 ") || 
                                  t.contains("비교") || t.contains("분석") || 
                                  t.contains("알려줘") || t.contains("설명해")

        // 2. 자동화 요청(Action) 감지
        val isActionRequest = t.contains("깨워줘") || t.contains("알람") || 
                             t.contains("설정해") || t.contains("예약") ||
                             t.contains("알려줘") && (t.contains("내일") || t.contains("오늘"))

        // 3. 의도(Intent) 판별 및 매핑
        val type = when {
            isActionRequest -> IntentType.ACTION_REQUEST
            t.contains("code") || t.contains("코딩") || t.contains("java") || t.contains("python") -> IntentType.CODE
            t.contains("번역") || t.contains("translate") -> IntentType.TRANSLATE
            t.contains("요약") || t.contains("summarize") -> IntentType.SUMMARIZE
            t.contains("해결") || t.contains("문제") -> IntentType.TROUBLESHOOT
            else -> IntentType.CHAT
        }

        // 4. 난이도(Complexity) 결정
        val complexity = when {
            type == IntentType.CODE || type == IntentType.TROUBLESHOOT || type == IntentType.ACTION_REQUEST -> Complexity.HIGH
            isDeepAnalysisNeeded -> Complexity.MEDIUM
            else -> Complexity.LOW
        }

        return IntentAnalysis(
            type = type,
            complexity = complexity,
            needsClarification = t.endsWith("?") && t.length < 5
        )
    }
}
