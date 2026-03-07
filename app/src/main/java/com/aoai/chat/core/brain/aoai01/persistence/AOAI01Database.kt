package com.aoai.chat.core.brain.aoai01.persistence

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [ProviderStatsEntity::class, PolicySettingEntity::class, KnowledgeEntity::class, UserContextEntity::class], version = 3, exportSchema = false)
abstract class AOAI01Database : RoomDatabase() {
    abstract fun aoai01Dao(): AOAI01Dao

    companion object {
        @Volatile
        private var INSTANCE: AOAI01Database? = null

        fun getDatabase(context: Context): AOAI01Database {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AOAI01Database::class.java,
                    "aoai01_intelligence_db"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
