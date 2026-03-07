package com.aoai.chat.ui

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.widget.Toast
import com.aoai.chat.data.MediaType

object MediaDownloadHelper {
    fun downloadMedia(context: Context, uri: Uri, type: MediaType) {
        try {
            val fileName = "AOAI_${System.currentTimeMillis()}.${getFileExtension(type)}"
            val request = DownloadManager.Request(uri)
                .setTitle("AOAI 파일 다운로드")
                .setDescription(fileName)
                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName)
                .setAllowedOverMetered(true)
                .setAllowedOverRoaming(true)

            val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            downloadManager.enqueue(request)
            Toast.makeText(context, "다운로드를 시작합니다...", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(context, "다운로드 실패: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun getFileExtension(type: MediaType): String {
        return when (type) {
            MediaType.IMAGE -> "jpg"
            MediaType.VIDEO -> "mp4"
            MediaType.AUDIO -> "mp3"
            else -> "bin"
        }
    }
}
