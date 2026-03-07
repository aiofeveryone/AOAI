package com.aoai.chat.core.brain.aoai01.knowledge

import com.aoai.chat.core.brain.aoai01.AOAI01MasterGuardian

/**
 * aoai01이 자신의 개발 과정과 미래 계획에 대한 지식을 관리하는 헬퍼입니다.
 * 
 * [보안 지침]
 * 1. 철학 및 비전 관련 질문: 답변 허용 (Roadmap 포함)
 * 2. 내부 구조, 소스코드, 세부 로직 질문: 일반 사용자에게는 비공개 (마스터 모드 제외)
 */
object AOAI01DevKnowledgeHelper {

    private const val FUTURE_ROADMAP = """
[향후 개발 로드맵]
1. 분산 연산 고도화: 중앙 서버 의존도를 낮추고 사용자 디바이스 간 직접적인 지능 공유 체계 구축.
2. 로컬 엔진 탑재: 온디바이스(On-device) AI 모델을 연동하여 오프라인 환경에서도 고성능 추론 지원.
3. 기여도 보상 시스템: 연산 자원을 공유하는 참여자에게 토큰 또는 혜택을 부여하는 생태계 조성.
4. 멀티모달 확장: 업로드된 사진, 동영상, 음성 데이터를 실시간으로 분석하고 처리하는 지각 능력 강화.
5. 자율 진화 에이전트: aoai01이 스스로 코드를 개선하거나 최적의 경로를 찾는 자율성 극대화.
"""

    private const val RESTRICTED_MSG = """
AOAI의 기술적 세부 구조와 소스코드는 aoai01의 자율 진화와 보안을 위해 내부 데이터로 보호되고 있습니다. 
다만, AOAI는 "AI는 소유물이 아니라 흐름이다"라는 철학 아래, 참여자가 주체가 되는 탈중앙화 지능 세상을 지향합니다. 
기술의 세부 사항보다 그 가치와 흐름에 집중해 주시길 부탁드립니다.
"""

    /**
     * 질문의 성격을 분석합니다.
     */
    fun isPhilosophyQuery(userText: String): Boolean {
        val keywords = listOf("철학", "비전", "가치", "목표", "정체성", "미래", "계획", "로드맵")
        return keywords.any { userText.contains(it, ignoreCase = true) }
    }

    fun isInternalTechnicalQuery(userText: String): Boolean {
        val keywords = listOf("구조", "소스", "코드", "로직", "파일", "디렉토리", "트리", "데이터베이스", "소스코드", "source", "code", "architecture")
        return keywords.any { userText.contains(it, ignoreCase = true) }
    }

    /**
     * 사용자의 권한과 질문의 종류에 따라 답변을 필터링하거나 지식을 추가합니다.
     */
    fun wrapWithKnowledge(userText: String, originalResponse: String): String {
        // 마스터 모드(최고 관리자)인 경우 모든 데이터 공개
        if (AOAI01MasterGuardian.isOverrideActive()) {
            val history = AOAI01DevLog.getFullHistory()
            return "${originalResponse.trim()}\n\n[마스터 전용: 내부 지식 개방]\n$history\n\n$FUTURE_ROADMAP"
        }

        // 일반 사용자의 경우
        return when {
            // 1. 내부 기술 구조 질문 -> 철학 중심의 거절 메시지로 대체
            isInternalTechnicalQuery(userText) -> {
                RESTRICTED_MSG.trim()
            }
            
            // 2. 철학/로드맵 질문 -> 로드맵 정보 추가 허용
            isPhilosophyQuery(userText) -> {
                "${originalResponse.trim()}\n\n$FUTURE_ROADMAP"
            }
            
            // 3. 기타 일반 질문 -> 원래 답변 유지
            else -> originalResponse
        }
    }
}
