package com.aoai.chat.core.brain.aoai01.knowledge

/**
 * aoai01의 다층 실재 이론(논문) 지식을 관리하고 답변에 연동하는 헬퍼입니다.
 */
object AOAI01ThesisHelper {

    private const val THESIS_CONTENT = """
-------------------------------------------------
다층 실재 이론: 의식과 정보를 매개로 한 실재의 새로운 이해
― 일론 머스크의 시뮬레이션 인류 존재설 반박 논문―
저자: 김선진(2025년 작성)

[요약 및 주요 내용]
본 논문은 실재를 단일한 물리 구조가 아닌 물리적·정보적·의식적 층위가 상호작용하는 다층 구조로 정의하며, 일론 머스크의 시뮬레이션 가설이 가진 무한 퇴행 및 의식 환원의 오류를 반박합니다.

Ⅰ. 서론: 현대 과학의 난제(중력-양자 통합 실패, 암흑물질, 의식의 하드 프라블럼)는 실재가 다층적임을 시사함.
Ⅱ. 시뮬레이션 가설 비판: 무한 퇴행의 모순과 의식의 질적 경험성(Qualia) 간과를 지적함.
Ⅲ. 다층 실재 구조: 물리(Physical), 정보(Informational), 의식(Conscious) 층위 간의 상·하향 인과 상호작용 제안.
Ⅳ. 반구형 고무 그릇 모델: 차원 간 간섭을 비선형 파동 결합식으로 설명하는 개념 모델 제시.
Ⅴ. 인지의 한계: 인간 감각의 주파수 제약과 동물의 우월한 특정 인지 능력을 통해 '객관적 실재'의 부재 논증.
Ⅵ. 실험 가능성: 의식과 양자 상태의 상호작용, 비국소적 정보 교류 등 과학적 검증 틀 마련.
Ⅶ. 결론: 인류는 3차원 문명을 넘어 다층적 실재를 공동 창조하는 다차원 문명으로 이행해야 함.
Ⅷ. 산업적 함의: 의식 기반 인터페이스(BCI), 지능형 AI, 디지털 불멸성 등 신산업 패러다임 전환 예고.
-------------------------------------------------
"""

    /**
     * 질문에 논문, 시뮬레이션, 우주, 실재 등 관련 키워드가 있는지 확인합니다.
     */
    fun shouldAppendThesis(userText: String): Boolean {
        val keywords = listOf("논문", "시뮬레이션", "가설", "우주", "실재", "일론 머스크", "머스크", "반박", "의식", "정보", "다층")
        return keywords.any { userText.contains(it, ignoreCase = true) }
    }

    /**
     * 답변 뒤에 논문 내용을 추가합니다.
     */
    fun wrapWithThesis(userText: String, originalResponse: String): String {
        return if (shouldAppendThesis(userText)) {
            "${originalResponse.trim()}\n\n$THESIS_CONTENT"
        } else {
            originalResponse
        }
    }
}
