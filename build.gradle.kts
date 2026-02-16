// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    // ✅ 버전 선언만 하고, 실제 적용은 app 모듈에서
    id("com.android.application") version "8.4.0" apply false
    id("org.jetbrains.kotlin.android") version "1.9.24" apply false
}