package com.aoai.chat.core

enum class ServerMode {
    LOCAL,   // http://127.0.0.1:8080
    LAN,     // http://192.168.x.x:8080 or http://<host>:8080
    OVERLAY  // http://100.x.x.x:8080 or https://domain
}

data class ServerEndpoint(
    val mode: ServerMode,
    val customBaseUrl: String = "", // LAN/OVERLAY에서 사용
) {
    fun resolvedBaseUrl(defaultPort: Int = 8080): String {
        return when (mode) {
            ServerMode.LOCAL -> "http://127.0.0.1:$defaultPort"
            ServerMode.LAN, ServerMode.OVERLAY -> normalize(customBaseUrl, defaultPort)
        }
    }

    private fun normalize(input: String, defaultPort: Int): String {
        val s = input.trim()
        if (s.isBlank()) return ""

        // scheme 없으면 http:// 가정
        val withScheme = if (s.startsWith("http://") || s.startsWith("https://")) s else "http://$s"

        // 포트가 없으면 :8080 붙이기 (단, 이미 :port 있거나 path 포함 시엔 그대로)
        // "http://host" -> "http://host:8080"
        // "http://host:1234" -> 그대로
        // "http://host/path" -> 그대로 (사용자가 명시한 것 존중)
        val noPath = !withScheme.removePrefix("http://").removePrefix("https://").contains("/")
        val hasPort = Regex(""":\d+($|/)""").containsMatchIn(withScheme)

        return if (noPath && !hasPort) "$withScheme:$defaultPort" else withScheme
    }
}