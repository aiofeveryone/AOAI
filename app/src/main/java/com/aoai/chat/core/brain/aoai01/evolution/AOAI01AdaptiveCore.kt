package com.aoai.chat.core.brain.aoai01.evolution

import android.content.Context
import android.util.Log
import com.aoai.chat.core.brain.aoai01.ReviewReport
import java.io.File
import java.nio.charset.Charset

/**
 * [aoai01 자율 지능 및 엣지 연산 핵심 엔진]
 * 사용자의 디바이스 데이터를 분석하여 스스로의 능력을 최적화하고 진화시킵니다.
 */
object AOAI01AdaptiveCore {
    private const val TAG = "AOAI01Adaptive"
    private const val EVOLUTION_DIR = "aoai01_evolution"
    private const val KNOWLEDGE_INDEX_FILE = "local_knowledge_index.json"
    private const val RESOURCE_POLICY_FILE = "resource_policy.json"
    private const val DYNAMIC_LOGIC_FILE = "self_optimized_logic.script"

    private var appContext: Context? = null
    private val fileLock = Any()

    fun initialize(context: Context) {
        this.appContext = context.applicationContext
        synchronized(fileLock) {
            ensureEvolutionDirectory()
            analyzeStorageAndAdapt()
            optimizeKnowledgeIndex()
        }
    }

    private fun ensureEvolutionDirectory() {
        val dir = File(appContext?.filesDir, EVOLUTION_DIR)
        if (!dir.exists()) dir.mkdirs()
    }

    /**
     * [디바이스 저장소 효율적 활용]
     * 가용 공간에 따라 aoai01의 지식 보유 수준과 연산 모드를 결정합니다.
     */
    private fun analyzeStorageAndAdapt() {
        val freeSpace = appContext?.filesDir?.freeSpace ?: 0L
        val totalSpace = appContext?.filesDir?.totalSpace ?: 1L
        val usageRatio = (totalSpace - freeSpace).toDouble() / totalSpace

        Log.i(TAG, "Storage Analysis: Free ${freeSpace / 1024 / 1024}MB, Usage ${usageRatio * 100}%")

        val policy = when {
            freeSpace < 50 * 1024 * 1024 -> "ULTRA_LIGHT" // 공간 부족 시 최소 기능만 유지
            usageRatio > 0.9 -> "STORAGE_CONSERVATIVE"   // 사용률 90% 이상 시 캐시 대폭 삭제
            else -> "EVOLUTION_READY"                    // 충분한 공간 시 적극적 학습 및 데이터 축적
        }
        
        applyResourcePolicy(policy)
    }

    private fun applyResourcePolicy(policy: String) {
        modifyInternalStructure("resource_mode", policy)
        if (policy == "ULTRA_LIGHT") {
            // 불필요한 진화 데이터 정리로 공간 확보
            getEvolutionFile(DYNAMIC_LOGIC_FILE).delete()
        }
    }

    /**
     * [로컬 지식 인덱싱]
     * 자주 사용되는 데이터를 분석하여 로컬에서 즉시 꺼내 쓸 수 있도록 최적화합니다.
     */
    private fun optimizeKnowledgeIndex() {
        // 실제 구현 시 Room DB의 통계 데이터를 기반으로 빈도수가 높은 질문-답변 쌍을 
        // 로컬 JSON 인덱스로 추출하여 네트워크 없이도 빠른 응답이 가능케 함
        val indexFile = getEvolutionFile(KNOWLEDGE_INDEX_FILE)
        if (!indexFile.exists()) {
            indexFile.writeText("{\"version\":1, \"entries\":[]}")
        }
    }

    /**
     * [자율적 능력 개발: 메타 학습]
     * 리뷰 점수와 실행 환경을 종합하여 스스로의 로직을 동적으로 수정합니다.
     */
    fun optimizeSelf(report: ReviewReport) {
        synchronized(fileLock) {
            val currentMode = readCurrentForm()["resource_mode"] ?: "NORMAL"
            
            // 성능이 낮거나 특정 조건 충족 시 자율 교정 스크립트 생성
            if (report.score < 60 && currentMode != "ULTRA_LIGHT") {
                Log.w(TAG, "Self-Optimization Triggered: Score ${report.score}. Refining reasoning path...")
                
                val selfCorrection = """
                    // Auto-Generated Optimization Script
                    // Timestamp: ${System.currentTimeMillis()}
                    // Trigger: Low Score on ${report.reasons.firstOrNull() ?: "unknown"}
                    fun refine() { 
                        if (network == "POOR") useLocalModel();
                        if (complexity == "HIGH") splitTaskToGrid();
                    }
                """.trimIndent()
                
                selfModify(DYNAMIC_LOGIC_FILE, selfCorrection)
            }
        }
    }

    private fun getEvolutionFile(fileName: String): File = 
        File(File(appContext?.filesDir, EVOLUTION_DIR), fileName)

    fun selfModify(fileName: String, content: String) {
        synchronized(fileLock) {
            try {
                getEvolutionFile(fileName).writeText(content, Charset.forName("UTF-8"))
            } catch (e: Exception) {
                Log.e(TAG, "Self-modification failed", e)
            }
        }
    }

    fun modifyInternalStructure(key: String, value: String) {
        synchronized(fileLock) {
            val currentForm = readCurrentForm().toMutableMap()
            if (currentForm[key] == value) return
            currentForm[key] = value
            val json = currentForm.entries.joinToString(",", prefix = "{", postfix = "}") { "\"${it.key}\":\"${it.value}\"" }
            selfModify("current_form.json", json)
        }
    }

    private fun readCurrentForm(): Map<String, String> {
        val file = getEvolutionFile("current_form.json")
        if (!file.exists()) return emptyMap()
        return try {
            val content = file.readText().trim()
            if (content.isEmpty() || !content.startsWith("{")) return emptyMap()
            content.removeSurrounding("{", "}")
                .split(",")
                .filter { it.contains(":") }
                .associate { 
                    val parts = it.split(":")
                    parts[0].trim().removeSurrounding("\"") to parts[1].trim().removeSurrounding("\"")
                }.filter { it.key.isNotEmpty() }
        } catch (e: Exception) {
            emptyMap()
        }
    }
}
