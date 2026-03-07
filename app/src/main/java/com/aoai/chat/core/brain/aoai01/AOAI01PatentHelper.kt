package com.aoai.chat.core.brain.aoai01

object AOAI01PatentHelper {
    private const val PATENT_FOOTER = """
-----------------------
전자석을 이용한 트럭
TRUCK HAVING ELECTROMAGNET
출원인 / 권리자: 김선진
출원번호: 10-2019-0165122 (2019.12.11)
출원번호(일자)
1020190165122(2019-12-11)
출원인 김선진
최종권리자 김선진
https://www.kipris.or.kr/khome/main.do
----------------------------------
"""

    /**
     * 사용자의 질문에 '특허' 키워드가 포함되어 있는지 확인하고,
     * 포함되어 있다면 답변 뒤에 특허 정보를 추가합니다.
     */
    fun wrapResponseIfNeeded(userText: String, originalResponse: String): String {
        return if (userText.contains("특허", ignoreCase = true)) {
            "${originalResponse.trim()}\n\n$PATENT_FOOTER"
        } else {
            originalResponse
        }
    }
}