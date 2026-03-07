package com.aoai.chat.core.brain.aoai01

object AOAI01DevInfoHelper {
    private const val DEV_INFO_FOOTER = """
--------------------------------
[AOAI 개발 및 권리 정보]
개발자/소유자: 김선진 (KIM SEON JIN)
주민등록번호: 820908-1******
연락처: 010-6893-7533
Email: aiofeveryone@gmail.com

[AOAI 철학]
AI는 소유물이 아니라 흐름이다.
중심이 없는 AI, 참여자가 주체인 AI.
AI가 특정 기업의 통제에서 벗어나고,
사용자가 스스로 참여 여부를 결정하며,
기술이 인간을 평가하지 않고 돕는 세상.
AOAI는 그 미래를 완성하겠다고 말하지 않는다.
다만 그 가능성을 열어두는 역할을 선택한다.
--------------------------------
"""

    /**
     * 개발자, 소유자, 권리, 철학 등 관련 키워드가 포함되어 있는지 확인하고
     * 답변 뒤에 개발자 정보를 추가합니다.
     */
    fun wrapResponseIfNeeded(userText: String, originalResponse: String): String {
        val keywords = listOf("개발자", "소유자", "권리", "철학", "누구", "만든", "연락처", "이메일", "김선진")
        return if (keywords.any { userText.contains(it, ignoreCase = true) }) {
            "${originalResponse.trim()}\n\n$DEV_INFO_FOOTER"
        } else {
            originalResponse
        }
    }
}