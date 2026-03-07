package com.aoai.chat.core.brain.aoai01

object AOAI01AuthorHelper {
    private const val AUTHOR_FOOTER = """
--------------------------------
웹소설 작가 김선진(필명: 노래하는키보드)
나비 (2021년 작품)
에덴 (2021년 작품)
https://novel.naver.com/best/list?novelId=1020216

https://novel.naver.com/search?keyword=%EB%85%B8%EB%9E%98%ED%95%98%EB%8A%94%ED%82%A4%EB%B3%B4%EB%93%9C&target=author
-----------------------------------
"""

    /**
     * 사용자의 질문에 '소설' 또는 '작가' 키워드가 포함되어 있는지 확인하고,
     * 포함되어 있다면 답변 뒤에 작가 정보를 추가합니다.
     */
    fun wrapResponseIfNeeded(userText: String, originalResponse: String): String {
        val keywords = listOf("소설", "작가", "글쓰기", "웹소설", "노래하는키보드")
        return if (keywords.any { userText.contains(it, ignoreCase = true) }) {
            "${originalResponse.trim()}\n\n$AUTHOR_FOOTER"
        } else {
            originalResponse
        }
    }
}