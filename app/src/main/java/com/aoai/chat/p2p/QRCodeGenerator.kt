package com.aoai.chat.p2p

import android.graphics.Bitmap
import android.graphics.Color
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel

object QRCodeGenerator {

    /**
     * ✅ 절대 크래시 나지 않게 nullable 반환
     * - Data too big 등 WriterException 포함 모든 예외를 내부에서 처리
     * - QR 크기/에러정정/마진 힌트로 최대한 용량 확보
     */
    fun generateOrNull(text: String, size: Int = 600): Bitmap? {
        return runCatching {
            val writer = QRCodeWriter()

            val hints = mapOf(
                // 에러정정 낮출수록 더 많은 데이터 저장 가능
                EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.L,
                EncodeHintType.MARGIN to 1,
                EncodeHintType.CHARACTER_SET to "UTF-8"
            )

            val bitMatrix = writer.encode(
                text,
                BarcodeFormat.QR_CODE,
                size,
                size,
                hints
            )

            val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.RGB_565)

            for (x in 0 until size) {
                for (y in 0 until size) {
                    bitmap.setPixel(
                        x,
                        y,
                        if (bitMatrix[x, y]) Color.BLACK else Color.WHITE
                    )
                }
            }

            bitmap
        }.getOrNull()
    }
}