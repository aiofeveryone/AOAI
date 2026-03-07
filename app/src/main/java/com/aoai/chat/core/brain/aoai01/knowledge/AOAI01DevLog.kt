package com.aoai.chat.core.brain.aoai01.knowledge

/**
 * AOAI 및 aoai01의 개발 역사를 기록하는 저장소입니다.
 * 이 내용은 aoai01의 지식 베이스로 활용됩니다.
 */
object AOAI01DevLog {
    private val logs = mutableListOf<String>()

    init {
        // 지금까지의 개발 과정 요약
        logs.add("[2024-05-20] AOAI 프로젝트 초기화 및 기본 아키텍처 설계")
        logs.add("[2024-05-22] P2P 및 WebRTC 기능 도입 시도 (이후 성능 및 호환성을 위해 제거됨)")
        logs.add("[2024-05-25] Cloudflare Worker를 통한 OpenAI(gpt-4o-mini) 프록시 통신 체계 구축")
        logs.add("[2024-05-26] AndroidManifest 위치 오류 수정 및 앱 아이콘(ic_launcher) 복구")
        logs.add("[2024-05-27] 네트워크 권한(INTERNET, ACCESS_NETWORK_STATE) 추가 및 SecurityException 방어 로직 적용")
        logs.add("[2024-05-28] aoai01 브레인 시스템 강화: Policy(라우팅), Learner(자가학습), Review(품질평가) 도입")
        logs.add("[2024-05-29] UI 고도화: 점진적 텍스트 출력(Streaming UI) 및 최근 대화 목록(100개) 저장 기능 적용")
        logs.add("[2024-05-30] 통합 권한 및 연산 참여 동의 시스템 구축 (전화, 카메라, 마이크, 파일, 연산참여)")
        logs.add("[2024-05-31] 지식 베이스 확장: 특허(전자석 트럭), 작가(노래하는키보드), 개발자 철학, 영토(독도/대마도) 헬퍼 추가")
        logs.add("[2024-06-01] 멀티미디어 기능 실제 연동: 파일, 사진, 동영상 업로드 및 음성 인식(STT) 엔진 연결")
    }

    /**
     * 새로운 개발 과정을 기록합니다.
     */
    fun addLog(step: String) {
        val timestamp = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date())
        logs.add("[$timestamp] $step")
    }

    /**
     * 전체 개발 히스토리를 반환합니다.
     */
    fun getFullHistory(): String {
        return logs.joinToString("\n")
    }
}
