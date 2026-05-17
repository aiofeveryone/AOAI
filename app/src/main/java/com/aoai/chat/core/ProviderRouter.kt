package com.aoai.chat.core

class ProviderRouter(
    private val providers: List<AOAIProvider>,
) {

    suspend fun route(input: String): String {

        // 지금은 0번 provider 사용
        // 나중에 조건에 따라 선택 가능

        val selectedProvider = providers.first()

        return selectedProvider.sendMessage(input)
    }
}
