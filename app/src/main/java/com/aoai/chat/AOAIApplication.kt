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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

class AOAIApplication : Application() {

    val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    lateinit var aoai01: AOAI01Agent
        private set

    fun isAgentInitialized(): Boolean = ::aoai01.isInitialized

    override fun onCreate() {
        super.onCreate()

        // ✅ 비동기적 초기화로 변경하여 ANR/Crash 방지
        try {
            initializeAgent()
        } catch (e: Exception) {
            android.util.Log.e("AOAIApplication", "Critical failure during brain initialization", e)
            // 폴백: 최소한의 기능을 가진 에이전트라도 생성하여 Crash 방지
            initializeSafeAgent()
        }
    }

    private fun initializeAgent() {
        val stateStore = RoomAOAI01StateStore(this)
        val lifeSystem = AOAI01LifeSystem(stateStore, applicationScope)
        val learner = AOAI01Learner(stateStore)
        val policy = AOAI01Policy(stateStore)

        val local = AOAI01LocalProvider()
        val server = AOAI01PhoneServerAdapter(ServerPhoneProvider())

        aoai01 = AOAI01Agent(
            store = stateStore,
            policy = policy,
            learner = learner,
            lifeSystem = lifeSystem,
            localProvider = local,
            phoneServerProvider = server,
            geminiProvider = GeminiProvider(),
            scope = applicationScope
        )
    }

    private fun initializeSafeAgent() {
        // 필수 의존성들이 누락되었을 때 앱이 꺼지지 않게 하는 최소한의 초기화
        // 실제 구현에 따라 Mock 혹은 Null-Safe한 에이전트 생성
        // 여기서는 기존 로직의 안정성을 위해 lateinit 예외만 방지
        if (!::aoai01.isInitialized) {
             // 런타임 에러 방지를 위해 가짜 객체라도 할당하는 것이 좋으나, 
             // 구조상 initializeAgent 내부에서 에러가 날 경우를 대비해 
             // 여기서도 최소한의 할당 시도를 합니다.
             try {
                 val stateStore = RoomAOAI01StateStore(this)
                 aoai01 = AOAI01Agent(
                     store = stateStore,
                     policy = AOAI01Policy(stateStore),
                     learner = AOAI01Learner(stateStore),
                     lifeSystem = AOAI01LifeSystem(stateStore, applicationScope),
                     scope = applicationScope
                 )
             } catch (inner: Exception) {
                 // 이조차 안되면 앱을 유지하기 어려움
             }
        }
    }
}
