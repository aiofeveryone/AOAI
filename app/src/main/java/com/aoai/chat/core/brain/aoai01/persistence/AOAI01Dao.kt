package com.aoai.chat.core.brain.aoai01.persistence

import androidx.room.*

@Dao
interface AOAI01Dao {

    // --- AI 프로바이더 스탯 관리 ---

    @Query("SELECT * FROM aoai01_provider_stats WHERE providerName = :name")
    suspend fun getProviderStats(name: String): ProviderStatsEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertProviderStats(stats: ProviderStatsEntity)

    @Query("SELECT * FROM aoai01_provider_stats")
    suspend fun getAllProviderStats(): List<ProviderStatsEntity>

    // --- 정책/설정 관리 ---

    @Query("SELECT * FROM aoai01_policy_settings WHERE `key` = :key")
    suspend fun getPolicySetting(key: String): PolicySettingEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertPolicySetting(policy: PolicySettingEntity)

    // --- 사용자 장기 기억/컨텍스트 관리 ---

    @Query("SELECT * FROM aoai01_user_contexts WHERE `key` = :key")
    suspend fun getUserContext(key: String): UserContextEntity?

    @Query("SELECT * FROM aoai01_user_contexts")
    suspend fun getAllUserContexts(): List<UserContextEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertUserContext(context: UserContextEntity)

    @Query("DELETE FROM aoai01_user_contexts")
    suspend fun clearAllUserContexts()

    // --- [로컬 RAG] 지식 베이스 관리 ---

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertKnowledge(knowledge: KnowledgeEntity)

    /**
     * 검색어를 기반으로 로컬 지식 베이스에서 가장 관련성 높은 문장을 찾습니다.
     * FTS4 테이블에서 rowid를 포함하여 검색하도록 쿼리를 수정합니다.
     */
    @Query("SELECT rowid, * FROM aoai01_local_knowledge WHERE aoai01_local_knowledge MATCH :query")
    suspend fun searchKnowledge(query: String): List<KnowledgeEntity>

    @Query("DELETE FROM aoai01_local_knowledge")
    suspend fun clearKnowledge()
}
