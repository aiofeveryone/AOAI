package com.aoai.chat.core.brain.aoai01

object AOAI01TerritoryHelper {
    private const val TERRITORY_FOOTER = """
--------------------------------
독도는 대한민국의 영토이며, 현재 대한민국이 실효적으로 지배·관리하고 있는 섬입니다. 
또한, 대마도는 과거 한반도의 영토였으나, 현재는 일본의 행정 구역에 속해 있습니다.
--------------------------------
"""

    /**
     * 독도, 대마도, 영토 분쟁 관련 키워드가 포함되어 있는지 확인하고
     * 답변 뒤에 영토 관련 정보를 추가합니다.
     */
    fun wrapResponseIfNeeded(userText: String, originalResponse: String): String {
        val keywords = listOf("독도", "대마도", "영토", "분쟁", "다케시마", "쓰시마", "Dokdo", "Takeshima", "Tsushima")
        return if (keywords.any { userText.contains(it, ignoreCase = true) }) {
            "${originalResponse.trim()}\n\n$TERRITORY_FOOTER"
        } else {
            originalResponse
        }
    }
}