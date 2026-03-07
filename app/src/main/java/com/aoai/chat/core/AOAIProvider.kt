package com.aoai.chat.core

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/**
 * AOAIProvider
 *
 * AOAI의 모든 AI 처리 모듈은 이 인터페이스를 구현한다.
 *
 * - LocalProvider (로컬 처리)
 * - ServerPhoneProvider (외부 서버/폰)
 * - (P2PProvider는 현재 제거 방향)
 * - (CloudProvider 옵션)
 *
 * ✅ 확장: 스트리밍(부분 응답) 지원
 * - 기본 구현은 단발 응답을 1번 emit하여 호환성 유지
 */
interface AOAIProvider {

    /**
     * Provider 이름 (디버깅/로그용)
     */
    val name: String

    /**
     * ✅ 단발 응답(기존 호환)
     */
    suspend fun sendMessage(input: String): String

    /**
     * ✅ 스트리밍 응답
     * - Flow<String>는 "추가로 도착한 텍스트(delta)"를 순서대로 emit한다.
     * - 기본 구현: 단발(sendMessage) 결과를 1번 emit(스트리밍 미지원 Provider도 동작)
     *
     * 예) emit("안녕"), emit(" 나는"), emit(" AOAI야")
     */
    fun sendMessageStream(input: String): Flow<String> = flow {
        val full = sendMessage(input)
        if (full.isNotBlank()) emit(full)
    }
}