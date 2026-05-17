# AOAI Agent Guidelines

## Architecture Overview
AOAI is an Android distributed AI platform with agent-based architecture. Core components:
- **AOAI01Agent**: Main agent managing chat UI state, planning, execution, and learning
- **Providers**: Local AI, phone server (remote), Gemini (fallback) for inference
- **P2P Layer**: WebRTC-based peer-to-peer networking for distributed tasks
- **Persistence**: Room database for agent state, chat history via DataStore
- **UI**: Jetpack Compose with Material3, Picture-in-Picture support

Key flows: User input → IntentRouter → Planner → PlanExecutor (with provider routing) → UI updates

## Build & Run
- **Build**: `./gradlew build` (uses AGP 8.4.0, Kotlin 1.9.24, versionCode 12103, versionName 1.2.103)
- **Run**: `./gradlew installDebug` or via Android Studio; see `RUN_INSTRUCTIONS.md` for detailed steps
- **Test**: `./gradlew test` (minimal unit tests; focus on brain components); use `./gradlew connectedAndroidTest` for instrumented tests
- **ABI Filters**: armeabi-v7a, arm64-v8a, x86, x86_64 (WebRTC compatibility)
- **Repos**: Centralized in settings.gradle.kts (google, mavenCentral, jitpack); no per-module repos
- **Config**: API URLs and keys via `BuildConfig` fields (set in `app/build.gradle.kts` buildTypes); empty by default for security

## Critical Workflows
- **Initialization**: AOAIApplication initializes AOAI01Agent with safe fallbacks to prevent crashes
- **Permissions**: Consent dialog on first launch; uses PermissionManager for camera/audio/network
- **Foreground Service**: AOAISessionService maintains wake/wifi locks for stable P2P sessions
- **Chat Persistence**: Messages saved via ChatHistoryStore; widget reads from same store
- **Provider Fallback**: On failure, try Gemini as backup (not automatic; see PROBLEMS_AND_IMPROVEMENTS.md #4)

## Project Conventions
- **Language**: Kotlin with official code style; Korean comments for context
- **Dependencies**: Compose BOM for UI; Ktor for HTTP; Room with KSP
- **State Management**: StateFlow for reactive UI; avoid runBlocking in stores
- **Error Handling**: Safe agent init with try/catch; fallback providers
- **Security**: API keys and URLs via `BuildConfig` (e.g., `BuildConfig.API_BASE_URL`, `BuildConfig.GEMINI_API_KEY`); Gemini key sent via `X-API-Key` header, not query param
- **Logging**: Guard all `Log.*()` calls with `if (BuildConfig.DEBUG)` to prevent production leaks; no sensitive data in logs

## Key Files & Patterns
- `AOAI01Agent.kt`: Core agent logic; attachContext() for app integration
- `AOAI01PlanExecutor.kt`: Executes plans with provider routing (local/server/Gemini)
- `AOAISessionService.kt`: Foreground service for session continuity
- `AndroidManifest.xml`: Extensive permissions; widget/service declarations
- `settings.gradle.kts`: Centralized repositories for consistency
- `app/build.gradle.kts`: Compose, Room, Ktor setup; hardcoded signing for release

## Known Issues (from PROBLEMS_AND_IMPROVEMENTS.md)
- Missing QuickThoughtReceiver class (manifest declares but not implemented) ✅ **FIXED**
- Widget not functional (missing manifest declarations) ✅ **FIXED**
- Chat history not persisted to store (widget shows empty) ✅ **FIXED**
- No automatic provider fallback on failure ✅ **FIXED**
- NPE risk if send() called before attachContext() ✅ **FIXED**
- Unscoped coroutines in agent (use applicationScope) - **MEDIUM PRIORITY**
- No state survival across process death - **MEDIUM PRIORITY**
- runBlocking in RoomAOAI01StateStore (migrate to suspend) ✅ **FIXED**
- Duplicate Role/ChatRole enums - **LOW PRIORITY**
- Hardcoded API URLs/keys ✅ **FIXED** (now uses BuildConfig; see FIXES_SUMMARY.md)
- Fragile keyword-based intent detection - **MEDIUM PRIORITY**
- Oversized AOAIChatScreen composable (extract sub-components) - **LOW PRIORITY**
- No unit tests for brain components - **MEDIUM PRIORITY**

See `FIX_CHECKLIST.md` and `FIXES_SUMMARY.md` for details on applied fixes. Prioritize remaining state survival and coroutine scope improvements for stability.</content>
<parameter name="filePath">C:\AOAI\AGENTS.md
