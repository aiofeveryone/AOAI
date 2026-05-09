package com.aoai.chat.core

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat

/**
 * AOAI 앱의 접근 권한을 관리하는 매니저 클래스
 */
object PermissionManager {

    /**
     * 앱 실행 시 필요한 모든 권한 목록을 반환합니다.
     * 전화, 마이크, 카메라, 파일 접근 권한을 포함합니다.
     */
    fun getRequiredPermissions(): Array<String> {
        val permissions = mutableListOf(
            Manifest.permission.RECORD_AUDIO,     // 음성(마이크)
            Manifest.permission.CAMERA            // 카메라
        )

        // 파일/미디어 권한 (버전별 대응)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.READ_MEDIA_IMAGES)
            permissions.add(Manifest.permission.READ_MEDIA_VIDEO)
            permissions.add(Manifest.permission.READ_MEDIA_AUDIO)
        } else {
            permissions.add(Manifest.permission.READ_EXTERNAL_STORAGE)
            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
                permissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            }
        }

        return permissions.toTypedArray()
    }

    /**
     * 모든 필수 권한이 허용되었는지 확인합니다.
     */
    fun hasAllPermissions(context: Context): Boolean {
        return getRequiredPermissions().all {
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        }
    }

    /**
     * 권한 요청을 위한 런처를 생성합니다.
     * Activity의 onCreate 또는 시작 지점에서 호출되어야 합니다.
     */
    fun createLauncher(activity: ComponentActivity, onResult: (Boolean) -> Unit): ActivityResultLauncher<Array<String>> {
        return activity.registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { results ->
            val allGranted = results.values.all { it }
            onResult(allGranted)
        }
    }
}
