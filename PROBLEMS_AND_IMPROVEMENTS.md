# AOAI Android Project — Problems and Suggested Improvements

## Critical bugs

### 1. Missing `QuickThoughtReceiver` (build/runtime risk)
- **Where:** `AndroidManifest.xml` declares `<receiver android:name=".ui.QuickThoughtReceiver" />`.
- **Problem:** The class does not exist in the project. If the system or another component sends an intent to this receiver, the app can crash.
- **Fix:** Either add a minimal stub `QuickThoughtReceiver` in `com.aoai.chat.ui` that extends `BroadcastReceiver` and does nothing in `onReceive`, or remove the `<receiver>` entry from the manifest.

### 2. Widget not declared in manifest
- **Where:** `AOAIWidgetProvider`, `AOAIWidgetService` exist; `res/xml/aoai_widget_info.xml` exists.
- **Problem:** Neither the `AppWidgetProvider` nor the `RemoteViewsService` is declared in `AndroidManifest.xml`. The home-screen widget will not work (widget list empty or widget not available in picker).
- **Fix:** In `AndroidManifest.xml`, inside `<application>`:
  - Add `<receiver android:name=".ui.AOAIWidgetProvider" android:exported="true">` with `<intent-filter>` for `ACTION_APPWIDGET_UPDATE` and `<meta-data android:name="android.appwidget.provider" android:resource="@xml/aoai_widget_info" />`.
  - Add `<service android:name=".ui.AOAIWidgetService" android:exported="false" android:permission="android.permission.BIND_REMOTEVIEWS" />`.

### 3. Chat history for widget never written
- **Where:** `AOAIWidgetService` / `AOAIWidgetFactory` load from `ChatHistoryStore.load(context)`; `AOAI01Agent` only updates in-memory `_uiState.messages`.
- **Problem:** Nothing in the main chat flow calls `ChatHistoryStore.save()`. The widget always shows an empty list.
- **Fix:** When a message is finalized in `AOAI01Agent` (e.g. after `revealAnswerGradually` or when adding assistant message), map current session messages to `StoredChatMessage` and call `ChatHistoryStore.save(context, list)`. You need to pass `Context` into the agent (e.g. from `attachContext`) or inject a callback so the agent can persist without holding a Context reference.

### 4. No provider fallback when primary fails
- **Where:** `AOAI01PlanExecutor.execute()`: for `PlanStep.RunReasoning` it calls `performReasoning(step.provider, ...)`. If `result.ok == false`, it only sets `fallbackReason` / `errorCode` and does not try another provider.
- **Problem:** User can get an empty or generic error message even though Gemini (or local) could have answered. The plan has a single reasoning step; there is no automatic fallback to `gemini_backup` in the same execution.
- **Fix:** After a failed `RunReasoning` for `phoneServer` or `local`, try `gemini_backup` (or another fallback) once and use that result if `ok`; then set `fallbackReason` accordingly. Alternatively, extend the planner to add an explicit fallback step when strategy is SERVER_ONLY or HYBRID.

### 5. Possible NPE if `send()` runs before `attachContext()`
- **Where:** `AOAI01Agent.send()` uses `appContext!!` (e.g. for `AOAI01PowerMonitor.getCurrentPower(appContext!!)`). `attachContext(context)` is called from `LaunchedEffect(Unit)` in `AOAIChatScreen`.
- **Problem:** If the user sends a message before the first composition (or if LaunchedEffect is delayed), `appContext` can still be null → NPE.
- **Fix:** At the start of `send()`, check `appContext == null` and return early or run a minimal plan (e.g. local-only) without device power. Prefer requiring `attachContext` before any send (e.g. disable send in UI until attached) or make power/context optional inside the agent.

---

## Architecture and design

### 6. Unscoped coroutines in `AOAI01Agent`
- **Where:** `CoroutineScope(Dispatchers.Main).launch { while(true) { delay(60_000); ... } }` and `CoroutineScope(Dispatchers.IO).launch { ... }` for the main send job.
- **Problem:** These scopes are not tied to application or UI lifecycle. If the agent is recreated or the process is long-lived, multiple loops can run; cancellation is not centralized.
- **Fix:** Use a single `applicationScope` (e.g. `CoroutineScope(SupervisorJob() + Dispatchers.Main)` created in `AOAIApplication`) and pass it into the agent, or use `viewModelScope` if the agent is owned by a ViewModel. Launch the periodic loop and the send job on that scope so they are cancelled when appropriate.

### 7. No state survival across process death or config change
- **Where:** Chat messages live only in `AOAI01Agent._uiState` (in-memory `StateFlow`). There is no `ViewModel`, `SavedStateHandle`, or persistence of the current session.
- **Problem:** Rotating the device or the process being killed clears the conversation. Users lose the current chat.
- **Fix:** Either persist the latest N messages (e.g. to Room or DataStore) and restore on start, or hold the agent in a ViewModel and use `SavedStateHandle` for minimal state. For full history, consider a “current session” table or append to `ChatHistoryStore` so the widget and restore can share the same source.

### 8. `runBlocking` in `RoomAOAI01StateStore`
- **Where:** Every method of `RoomAOAI01StateStore` uses `runBlocking { ... }` to call the DAO.
- **Problem:** Blocks the calling thread. If called from the main thread (e.g. from UI or from a component that runs on Main), this can cause ANRs. If called from a coroutine, it blocks the dispatcher and can cause deadlocks or starvation.
- **Fix:** Change `AOAI01StateStore` to suspend functions (e.g. `suspend fun getProviderPenalty(...): Double`) and implement them with `dao.getProviderStats(...)` from a coroutine (no `runBlocking`). Callers (Learner, Policy, Agent) should use `withContext(Dispatchers.IO)` or a dedicated dispatcher when calling the store, or the store can use a single-thread dispatcher internally.

### 9. Duplicate role types for chat
- **Where:** `ChatMessage` uses `Role` (USER, ASSISTANT); `ChatHistoryStore` / `StoredChatMessage` use `ChatRole`.
- **Problem:** Two enums for the same concept; mapping and bugs when converting between UI and stored format.
- **Fix:** Use a single enum (e.g. `Role` in `data`) and use it in both `ChatMessage` and `StoredChatMessage`. If persistence uses different names, map only at serialization (e.g. `@SerialName`) or in a small adapter layer.

---

## Security and configuration

### 10. Hardcoded API base URL
- **Where:** `ServerPhoneProvider`: `DEFAULT_BASE_URL = "https://api.aiofeveryone.com"`.
- **Problem:** Hard to change for different environments (staging, testing); URL is fixed in code.
- **Fix:** Use `BuildConfig` (e.g. `BuildConfig.API_BASE_URL`) or a config file/remote config, and avoid committing production URLs in source if policy requires it.

### 11. Gemini API key empty and in URL
- **Where:** `GeminiProvider`: `private val apiKey: String = ""` and request URL uses `?key=$apiKey`.
- **Problem:** Fallback always returns “API Key가 설정되지 않았습니다.” If a key is set, putting it in the query string can leak in logs or referrer.
- **Fix:** Load key from `BuildConfig` or secure storage (e.g. EncryptedSharedPreferences). Prefer sending the key in a header (e.g. `X-API-Key`) if the API supports it, to avoid query-string logging.

### 12. Sensitive data in logs
- **Where:** Various `Log.d`/`Log.i` calls (e.g. planner, executor, vitality) and potential logging of URLs or responses.
- **Problem:** In release builds, logs can expose user content or internal state. ProGuard may strip logs, but not guaranteed.
- **Fix:** Guard log calls with `BuildConfig.DEBUG` or a custom `LogUtil`, and avoid logging full prompts, responses, or URLs in production.

---

## Code quality and maintainability

### 13. Fragile keyword-based intent and actions
- **Where:** Memory consent: `input.contains("동의해") || input.contains("허락해") || ...`; 119 dial: `input.contains("119") && (input.contains("신고") || ...)`; permission triggers by keyword.
- **Problem:** Easy to false-trigger (e.g. “119번으로 전화해” in a story) or miss variations. Hard to maintain.
- **Fix:** Prefer intent classification from `AOAI01IntentRouter` (e.g. explicit intent types like `EMERGENCY_CALL` or `GRANT_MEMORY`) and drive UI/actions from that. Keep keyword logic only as a fallback or for quick triggers, and centralize in one place.

### 14. Oversized composable
- **Where:** `AOAIChatScreen` is 500+ lines: input handling, TTS, language list, history sheet, clear dialog, message list, network banner, etc.
- **Problem:** Hard to test and maintain; recomposition scope is large.
- **Fix:** Extract sub-composables (e.g. `MessageList`, `InputBar`, `LanguageDialog`, `HistoryBottomSheet`) and move non-UI logic (keyword handling, permission intents) into a ViewModel or helper so the screen only coordinates UI and user events.

### 15. Deprecated field still in use
- **Where:** `ChatMessage` has `@Deprecated("Use mediaUri and mediaType") val imageUri: Uri? = null`.
- **Problem:** Deprecated API remains in the data model; callers might still use it.
- **Fix:** Migrate all usages to `mediaUri`/`mediaType`, then remove `imageUri`, or keep a single field and remove the deprecation after migration.

### 16. TODOs and missing features
- **Where:** Several TODOs: `AOAI01PhoneServerAdapter` (mediaUri); `AOAI01LocalProvider` (mediaUri); `GeminiProvider` (apiKey, mediaUri); `AOAI01MigrationProtocol` (policy data); `AOAINodeManager` (P2P stubs).
- **Problem:** Incomplete features can confuse behavior (e.g. image not sent to server) or leave stubs that are never implemented.
- **Fix:** Prioritize: (1) mediaUri for server/local/Gemini if image input is required; (2) Gemini API key or explicit “no fallback” behavior; (3) remove or clearly mark P2P/Node stubs as not implemented so they are not assumed to work.

---

## Testing and robustness

### 17. No unit tests for core brain
- **Where:** `AOAI01Planner`, `AOAI01PlanExecutor`, `AOAI01Learner`, `AOAI01Review`, `AOAI01IntentRouter` have no tests.
- **Problem:** Regressions in routing, scoring, or learning are hard to catch. Refactoring is risky.
- **Fix:** Add unit tests (JUnit + coroutines test) for: intent analysis (inputs → IntentAnalysis); planner (context → plan steps); learner (report → penalty changes); review (input/response → ReviewReport). Mock `AOAI01StateStore` and keep tests deterministic.

### 18. TTS completion based on heuristic delay
- **Where:** `AOAIChatScreen`: `delay(1000L * (textToSpeak.length / 5).coerceAtLeast(2))` to guess when TTS finished.
- **Problem:** TTS duration depends on engine and language; heuristic can be wrong (too short or too long), and `isSpeaking` may be cleared too early or late.
- **Fix:** Use `UtteranceProgressListener` (or platform equivalent) to set `isSpeaking = false` when TTS actually completes or fails, and remove or shorten the delay to a small buffer only.

---

## Summary table

| Category   | Issue                                      | Severity   | Effort  |
|-----------|---------------------------------------------|------------|---------|
| Bug       | QuickThoughtReceiver missing               | High       | Low     |
| Bug       | Widget not in manifest                     | High       | Low     |
| Bug       | ChatHistoryStore never written             | High       | Medium  |
| Bug       | No provider fallback on failure            | High       | Medium  |
| Bug       | NPE if send before attachContext           | Medium     | Low     |
| Design    | Unscoped coroutines in Agent               | Medium     | Medium  |
| Design    | No state survival (config/process death)   | Medium     | Medium  |
| Design    | runBlocking in StateStore                  | Medium     | Medium  |
| Design    | Duplicate Role/ChatRole                    | Low        | Low     |
| Security  | Hardcoded API URL / Gemini key             | Medium     | Low     |
| Security  | Logging sensitive data                      | Low        | Low     |
| Quality   | Keyword-based intents fragile              | Medium     | Medium  |
| Quality   | Large composable / deprecated field       | Low        | Medium  |
| Quality   | TODOs and incomplete features              | Low–Medium | Varies  |
| Testing   | No unit tests for brain                    | Medium     | High    |
| Testing   | TTS completion heuristic                    | Low        | Low     |

Implementing the critical bug fixes (1–5) first will stabilize the app and widget; then state survival (7) and store suspension (8) will improve reliability and responsiveness.
