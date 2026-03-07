package com.aoai.chat.core.brain.aoai01.providers

import android.net.Uri
import com.aoai.chat.ai.ServerPhoneProvider
import com.aoai.chat.core.brain.aoai01.AOAI01Provider
import com.aoai.chat.core.brain.aoai01.ProviderResult
import kotlin.system.measureTimeMillis

/**
 * aoai01(Brain)에서 사용하는 AOAI01Provider 인터페이스에 맞게,
 * 기존 com.aoai.chat.ai.ServerPhoneProvider 를 감싸는(어댑터) 클래스.
 *
 * - aoai01은 prompt(문맥 포함) 문자열을 넘겨줌
 * - ServerPhoneProvider는 sendMessage(text) 로 POST /ask 호출
 */
class AOAI01PhoneServerAdapter(
    private val impl: ServerPhoneProvider
) : AOAI01Provider {

    override val name: String = "phoneServer"

    override suspend fun generate(prompt: String, mediaUri: Uri?, meta: Map<String, String>): ProviderResult {
        var text: String = ""
        var ok = true
        var errorCode: String? = null

        val elapsed = measureTimeMillis {
            try {
                // ✅ 여기에서 aoai01이 만든 prompt(컨텍스트 포함)를 그대로 서버폰에 전송
                // TODO: ServerPhoneProvider가 mediaUri를 지원한다면 전달 로직 추가
                text = impl.sendMessage(prompt).orEmpty()
            } catch (t: Throwable) {
                ok = false
                errorCode = "exception:${t.javaClass.simpleName}"
                text = "" // 실패 시 빈 값
            }
        }

        // 결과 정리
        val finalText = text.trim()

        return ProviderResult(
            ok = ok && finalText.isNotBlank(),
            text = finalText,
            latencyMs = elapsed,
            errorCode = if (ok) null else errorCode
        )
    }
}