package com.aoai.chat.core.brain.aoai01.persistence

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Fts4
import androidx.room.PrimaryKey

/**
 * AI 프로바이더별 패널티 및 성공/실패 기록 테이블
 */
@Entity(tableName = "aoai01_provider_stats")
data class ProviderStatsEntity(
    @PrimaryKey val providerName: String,
    val penalty: Double = 0.0,
    val okCount: Int = 0,
    val failCount: Int = 0
)

/**
 * aoai01 정책 및 설정값 저장 테이블 (Key-Value 형태)
 */
@Entity(tableName = "aoai01_policy_settings")
data class PolicySettingEntity(
    @PrimaryKey val key: String,
    val value: String
)

/**
 * [사용자 장기 기억/컨텍스트 저장 테이블]
 */
@Entity(tableName = "aoai01_user_contexts")
data class UserContextEntity(
    @PrimaryKey val key: String,
    val value: String,
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * [로컬 RAG용 지식 베이스 테이블]
 * 검색 성능 최적화를 위해 Room FTS4(Full-Text Search)를 사용합니다.
 * FTS4 테이블은 기본적으로 rowid를 가지고 있으므로, PrimaryKey를 rowid로 정의합니다.
 */
@Fts4
@Entity(tableName = "aoai01_local_knowledge")
data class KnowledgeEntity(
    @PrimaryKey @ColumnInfo(name = "rowid") val rowid: Int = 0,
    val title: String,
    val content: String,
    val source: String,
    val timestamp: Long = System.currentTimeMillis()
)
