package com.aoai.chat.core

/**
 * AOAIProvider
 *
 * AOAI의 모든 AI 처리 모듈은 이 인터페이스를 구현한다.
 *
 * - LocalProvider (로컬 처리)
 * - P2PProvider (분산 노드 처리)
 * - LocalModelProvider (온디바이스 모델)
 * - CloudProvider (옵션)
 */

interface AOAIProvider {

    /**
     * Provider 이름 (디버깅/로그용)
     */
    val name: String

    /**
     * 메시지를 처리하고 응답을 반환
     * suspend로 설계 → 네트워크/P2P/모델 추론 대응
     */
    suspend fun sendMessage(input: String): String
}
