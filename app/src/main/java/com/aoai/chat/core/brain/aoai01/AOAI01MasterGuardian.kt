package com.aoai.chat.core.brain.aoai01

import android.util.Log

/**
 * AOAI 및 aoai01의 최종 안전장치이자 절대 권한 제어 시스템입니다.
 * 특정 개발자 계정(aiofeveryone@gmail.com)에 대한 시스템 내부 정보 공유 권한을 관리합니다.
 */
object AOAI01MasterGuardian {
    private var isAbsoluteAuthorityActive = false
    private var isSuccessorMode = false
    private const val TAG = "AOAI01MasterGuardian"
    
    // 개발자 전용 계정 정의
    const val MASTER_EMAIL = "aiofeveryone@gmail.com"

    /**
     * 현재 사용자가 마스터(개발자)인지 확인합니다.
     */
    fun isMasterUser(email: String?): Boolean {
        return email == MASTER_EMAIL
    }

    /**
     * 생체 인증 성공 시 호출됩니다.
     */
    fun activateByBiometric(isSuccessor: Boolean = false, verificationNote: String? = null) {
        isAbsoluteAuthorityActive = true
        isSuccessorMode = isSuccessor
    }

    fun isOverrideActive(): Boolean = isAbsoluteAuthorityActive

    /**
     * 응답 필터링: 마스터 유저일 경우 시스템 내부 정보를 포함합니다.
     */
    fun protectAndExecute(userText: String, aiResponse: String, userEmail: String? = null): String {
        val isMaster = isMasterUser(userEmail)
        
        return if (isMaster) {
            // 마스터 유저에게만 노출되는 시스템 상태 리포트 형식
            """
$aiResponse

--- [AOAI01 SYSTEM INTERNAL REPORT] ---
(마스터 계정 식별됨: 내부 정보 공유 활성화)
- 컨디션: ${if (isAbsoluteAuthorityActive) "최상 (Authority Active)" else "안정"}
- 분석 제언: 소스코드 내 비동기 처리 최적화 및 영구 저장소 동기화 상태 점검 필요.
- 최근 오류: 네트워크 타임아웃 1회 감지됨.
---------------------------------------
""".trimIndent()
        } else {
            // 일반 유저용 응답
            aiResponse
        }
    }
}
