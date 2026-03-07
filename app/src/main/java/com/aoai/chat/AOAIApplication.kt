package com.aoai.chat

import android.app.Application
import com.aoai.chat.ai.ServerPhoneProvider
import com.aoai.chat.core.brain.aoai01.AOAI01Agent
import com.aoai.chat.core.brain.aoai01.AOAI01Learner
import com.aoai.chat.core.brain.aoai01.AOAI01Policy
import com.aoai.chat.core.brain.aoai01.lifecore.AOAI01LifeSystem
import com.aoai.chat.core.brain.aoai01.persistence.RoomAOAI01StateStore
import com.aoai.chat.core.brain.aoai01.providers.AOAI01LocalProvider
import com.aoai.chat.core.brain.aoai01.providers.AOAI01PhoneServerAdapter
import com.aoai.chat.core.brain.aoai01.providers.GeminiProvider

class AOAIApplication : Application() {

    lateinit var aoai01: AOAI01Agent
        private set

    override fun onCreate() {
        super.onCreate()

        val stateStore = RoomAOAI01StateStore(this)
        val lifeSystem = AOAI01LifeSystem(stateStore)
        val learner = AOAI01Learner(stateStore)
        val policy = AOAI01Policy(stateStore)

        // 실제 지능 엔진들 생성
        val local = AOAI01LocalProvider()
        val server = AOAI01PhoneServerAdapter(ServerPhoneProvider())

        // 엔진이 포함된 최종 에이전트 가동
        aoai01 = AOAI01Agent(
            store = stateStore,
            policy = policy,
            learner = learner,
            lifeSystem = lifeSystem,
            localProvider = local,
            phoneServerProvider = server,
            geminiProvider = GeminiProvider()
        )
    }
}
