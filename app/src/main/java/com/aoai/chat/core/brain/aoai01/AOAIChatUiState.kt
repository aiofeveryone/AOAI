package com.aoai.chat.core.brain.aoai01

import com.aoai.chat.data.ChatMessage

/**
 * AOAI01(aoai01)이 UI에 내려주는 "화면 상태(State)" 모델
 *
 * ✅ 원칙
 * - UI(Compose)는 이 상태를 구독(collect)해서 그리기만 한다.
 * - 메시지 추가/로딩/에러/취소/저장/복원/트림 같은 운영 로직은 aoai01이 담당한다.
 */
data class AOAIChatUiState(
    /** 화면에 보여줄 채팅 메시지 목록 */
    val messages: List<ChatMessage> = emptyList(),

    /** 현재 전송/응답 생성 중인지 (로딩바/취소버튼 표시용) */
    val isSending: Boolean = false
)