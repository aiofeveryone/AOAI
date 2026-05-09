# [AOAI Project ProGuard Rules]

# 1. SLF4J 관련 경고 무시 (Ktor 로깅 오류 해결)
-dontwarn org.slf4j.**

# 2. Kotlin Serialization 관련 규칙
-keepattributes *Annotation*, InnerClasses
-keepclassmembers class ** {
    @kotlinx.serialization.Serializable *;
}
-keepclassmembers class ** {
    @kotlinx.serialization.SerialName *;
}

# 3. Ktor 관련 규칙
-keep class io.ktor.** { *; }
-dontwarn io.ktor.**

# 4. Room Database 관련 규칙
-keep class * extends androidx.room.RoomDatabase
-dontwarn androidx.room.**

# 5. Coil (이미지 로딩) 관련 규칙
-keep class coil.** { *; }
-dontwarn coil.**

# 6. Biometric 및 기타 Jetpack 라이브러리
-keep class androidx.biometric.** { *; }
-dontwarn androidx.biometric.**

# 7. AOAI Core Brain 관련 (리플렉션 및 직렬화 보호)
-keep class com.aoai.chat.core.brain.aoai01.** { *; }
-keep class com.aoai.chat.data.** { *; }
-keep class com.aoai.chat.ai.** { *; }

# 8. Room Auto-generated classes
-keep class * extends androidx.room.RoomDatabase
-keep class * extends androidx.room.Entity
-keep class * extends androidx.room.Dao
-keep class **_Impl { *; }
