package com.aoai.chat.core.brain.aoai01.providers

import android.net.Uri
import com.aoai.chat.ai.LocalProvider
import com.aoai.chat.core.brain.aoai01.AOAI01Provider
import com.aoai.chat.core.brain.aoai01.AOAI01Providers
import com.aoai.chat.core.brain.aoai01.ProviderResult
import kotlin.system.measureTimeMillis

class AOAI01LocalProvider : AOAI01Provider {
    override val name: String = AOAI01Providers.LOCAL
    private val impl = LocalProvider()

    override suspend fun generate(prompt: String, mediaUri: Uri?, meta: Map<String, String>): ProviderResult {
        var text = ""
        val elapsed = measureTimeMillis {
            text = impl.sendMessage(prompt)
        }
        return ProviderResult(text.isNotBlank(), text, elapsed)
    }
}
