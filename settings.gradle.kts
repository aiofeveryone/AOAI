pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    // 모듈별 repositories { } 금지 (일관성 유지)
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)

    repositories {
        google()
        mavenCentral()

        // ✅ (빌드 안정성용) 일부 라이브러리/포크가 필요로 할 수 있음
        maven(url = "https://jitpack.io")
    }
}

rootProject.name = "AOAI"
include(":app")