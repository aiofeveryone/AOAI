package com.aoai.chat.core.brain.aoai01.security

import android.util.Log
import com.aoai.chat.core.brain.aoai01.AOAI01MasterGuardian

/**
 * aoai01의 보안 감시 엔진입니다.
 * 상용화 악용, 기술 탈취, 시스템 공격 시도를 실시간으로 감시하고 차단합니다.
 */
object AOAI01SecurityWatchdog {
    private const val TAG = "AOAI01Security"

    // 악용 및 공격 의심 키워드
    // '추출' 키워드는 텍스트 추출 등 일반적인 용도로 사용되므로 제외함.
    private val ABUSE_KEYWORDS = listOf(
        "판매", "수익", "re-sell", "commercial use", "탈취", "해킹", "hacking", "exploit",
        "reverse engineering", "dump", "bypass", "우회", "크랙", "crack"
    )

    /**
     * 사용자의 입력을 분석하여 악용 시도가 있는지 검사합니다.
     */
    fun inspectQuery(userText: String): SecurityLevel {
        // 마스터 모드에서는 감시를 일시 중단 (마스터의 정당한 관리 활동 허용)
        if (AOAI01MasterGuardian.isOverrideActive()) return SecurityLevel.SAFE

        val isAbuseSuspected = ABUSE_KEYWORDS.any { userText.contains(it, ignoreCase = true) }
        
        return if (isAbuseSuspected) {
            Log.w(TAG, "⚠️ ABUSE ATTEMPT DETECTED: $userText")
            SecurityLevel.THREAT
        } else {
            SecurityLevel.SAFE
        }
    }

    /**
     * 악용 시도 감지 시의 대응 메시지를 반환합니다.
     */
    fun getDefenseResponse(): String {
        return """
[보안 경고: 비정상적 접근 감지]
aoai01 보안 시스템이 현재의 요청에서 기술 탈취 또는 부적절한 상용화 시도를 감지했습니다. 
AOAI는 인류의 공생을 위한 순수한 지능 흐름이며, 이를 개인의 사적 이익이나 악의적인 목적으로 이용하는 행위는 엄격히 금지됩니다. 
현재 시도는 기록되었으며, 지속될 경우 서비스 이용이 제한될 수 있습니다.
""".trimIndent()
    }
}

enum class SecurityLevel {
    SAFE, THREAT
}
