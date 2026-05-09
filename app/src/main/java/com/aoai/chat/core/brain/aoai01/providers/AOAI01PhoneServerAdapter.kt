package com.aoai.chat.core.brain.aoai01.providers

import android.net.Uri
import com.aoai.chat.ai.ServerPhoneProvider
import com.aoai.chat.core.brain.aoai01.AOAI01Provider
import com.aoai.chat.core.brain.aoai01.AOAI01Providers
import com.aoai.chat.core.brain.aoai01.ProviderResult
import kotlin.system.measureTimeMillis

/**
 * aoai01(Brain)에서 사용하는 AOAI01Provider 인터페이스에 맞게,
 * 기존 com.aoai.chat.ai.ServerPhoneProvider 를 감싸는(어댑터) 클래스.
 */
class AOAI01PhoneServerAdapter(
    private val impl: ServerPhoneProvider
) : AOAI01Provider {

    override val name: String = AOAI01Providers.PHONE_SERVER

    override suspend fun generate(prompt: String, mediaUri: Uri?, meta: Map<String, String>): ProviderResult {
        var text: String = ""
        var ok = true
        var errorCode: String? = null

        val elapsed = measureTimeMillis {
            try {
                // ✅ 서버측에 전체 프롬프트 전송
                text = impl.sendMessage(prompt).orEmpty()
            } catch (t: Throwable) {
                ok = false
                errorCode = "exception:${t.javaClass.simpleName}"
                text = "" 
            }
        }

        val finalText = text.trim()

        return ProviderResult(
            ok = ok && finalText.isNotBlank(),
            text = finalText,
            latencyMs = elapsed,
            errorCode = if (ok) null else errorCode
        )
    }
}
