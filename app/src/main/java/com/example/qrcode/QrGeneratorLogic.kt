package com.example.qrcode

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.util.Base64
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import java.io.ByteArrayOutputStream

object QrGeneratorLogic {

    /**
     * Generates a 2D BooleanArray matrix representing the QR code blocks.
     */
    fun generateMatrix(text: String): Array<BooleanArray>? {
        if (text.isEmpty()) return null
        return try {
            val hints = HashMap<EncodeHintType, Any>().apply {
                put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M)
                put(EncodeHintType.MARGIN, 1)
            }
            val writer = QRCodeWriter()
            val bitMatrix = writer.encode(text, BarcodeFormat.QR_CODE, 0, 0, hints)
            val size = bitMatrix.width
            val matrix = Array(size) { BooleanArray(size) }
            for (y in 0 until size) {
                for (x in 0 until size) {
                    matrix[y][x] = bitMatrix.get(x, y)
                }
            }
            matrix
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Builds standard UPI Deep-link uri string.
     */
    fun buildUpiUri(upiId: String, payeeName: String, amount: String, note: String): String {
        val cleanId = upiId.trim()
        val cleanName = Uri.encode(payeeName.trim())
        val cleanNote = Uri.encode(note.trim())
        var uri = "upi://pay?pa=$cleanId"
        if (cleanName.isNotEmpty()) {
            uri += "&pn=$cleanName"
        }
        if (amount.toDoubleOrNull() != null) {
            uri += "&am=${amount.trim()}"
        }
        if (cleanNote.isNotEmpty()) {
            uri += "&tn=$cleanNote"
        }
        return uri
    }

    /**
     * Builds standard PayPal.me payment link.
     */
    fun buildPayPalUri(usernameOrEmail: String, amount: String): String {
        val cleanUser = usernameOrEmail.trim().removePrefix("@")
        val cleanAmount = amount.trim()
        return if (cleanAmount.toDoubleOrNull() != null) {
            "https://paypal.me/$cleanUser/$cleanAmount"
        } else {
            "https://paypal.me/$cleanUser"
        }
    }

    /**
     * Builds standard WiFi connection QR string.
     */
    fun buildWifiString(ssid: String, password: String, security: String): String {
        val cleanSsid = ssid.trim()
        val cleanPassword = password.trim()
        val secType = if (security.uppercase() == "NOPASS") "" else security.uppercase()
        
        val escSsid = escapeWifiValue(cleanSsid)
        val escPassword = escapeWifiValue(cleanPassword)
        
        return if (secType.isEmpty()) {
            "WIFI:S:$escSsid;T:nopass;;"
        } else {
            "WIFI:S:$escSsid;T:$secType;P:$escPassword;;"
        }
    }

    private fun escapeWifiValue(value: String): String {
        val sb = StringBuilder()
        for (c in value) {
            if (c == '\\' || c == ';' || c == ',' || c == ':') {
                sb.append('\\')
            }
            sb.append(c)
        }
        return sb.toString()
    }

    /**
     * Compresses any bitmap into a compact Base64 JPEG string within standard QR capacities.
     */
    fun compressBitmapToBase64(bitmap: Bitmap, targetWidth: Int = 24, targetHeight: Int = 24): Pair<String, Int> {
        return try {
            val scaled = Bitmap.createScaledBitmap(bitmap, targetWidth, targetHeight, true)
            val out = ByteArrayOutputStream()
            // High compression to fit within typical 2KB QR constraint
            scaled.compress(Bitmap.CompressFormat.JPEG, 60, out)
            val bytes = out.toByteArray()
            val base64Str = Base64.encodeToString(bytes, Base64.NO_WRAP)
            val fullString = "data:image/jpeg;base64,$base64Str"
            Pair(fullString, fullString.length)
        } catch (t: Throwable) {
            android.util.Log.e("QrGeneratorLogic", "Bitmap compression error", t)
            Pair("", 0)
        }
    }

    /**
     * Creates dynamic visual preset icons so Image Mode is testable without gallery picker.
     */
    fun createPresetBitmap(presetType: String): Bitmap {
        val size = 16
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(bitmap)
        val paint = android.graphics.Paint().apply {
            isAntiAlias = true
            style = android.graphics.Paint.Style.FILL
        }

        // Fill background with white
        canvas.drawColor(android.graphics.Color.WHITE)

        when (presetType) {
            "heart" -> {
                paint.color = 0xFFEF5350.toInt() // Crimson Red
                val heartPattern = arrayOf(
                    "001100110",
                    "011110111",
                    "111111111",
                    "111111111",
                    "011111110",
                    "001111100",
                    "000111000",
                    "000010000"
                )
                for (y in heartPattern.indices) {
                    val row = heartPattern[y]
                    for (x in row.indices) {
                        if (row[x] == '1') {
                            canvas.drawRect((x + 3).toFloat(), (y + 4).toFloat(), (x + 4).toFloat(), (y + 5).toFloat(), paint)
                        }
                    }
                }
            }
            "star" -> {
                paint.color = 0xFFFFCA28.toInt() // Amber Yellow
                val starPattern = arrayOf(
                    "000010000",
                    "000111000",
                    "111111111",
                    "011111110",
                    "001111100",
                    "001101100",
                    "011000110"
                )
                for (y in starPattern.indices) {
                    val row = starPattern[y]
                    for (x in row.indices) {
                        if (row[x] == '1') {
                            canvas.drawRect((x + 3).toFloat(), (y + 4).toFloat(), (x + 4).toFloat(), (y + 5).toFloat(), paint)
                        }
                    }
                }
            }
            "smile" -> {
                paint.color = 0xFF26A69A.toInt() // Teal
                paint.style = android.graphics.Paint.Style.STROKE
                paint.strokeWidth = 1f
                canvas.drawCircle(8f, 8f, 6f, paint)

                paint.style = android.graphics.Paint.Style.FILL
                canvas.drawCircle(6f, 6f, 1f, paint)
                canvas.drawCircle(10f, 6f, 1f, paint)

                paint.style = android.graphics.Paint.Style.STROKE
                paint.strokeWidth = 1f
                canvas.drawArc(5f, 5f, 11f, 11f, 0f, 180f, false, paint)
            }
            else -> {
                // Checkmark
                paint.color = 0xFF66BB6A.toInt() // Green
                canvas.drawRect(4f, 8f, 6f, 10f, paint)
                canvas.drawRect(6f, 10f, 12f, 12f, paint)
                canvas.drawRect(10f, 6f, 12f, 10f, paint)
            }
        }
        return bitmap
    }

    /**
     * Exports a customized QR code bitmap to the local gallery using MediaStore.
     */
    fun saveQrToGallery(
        context: Context,
        matrix: Array<BooleanArray>?,
        foreColor: Int,
        backColor: Int,
        roundness: Float
    ): Uri? {
        if (matrix == null) return null
        val exportSize = 512
        val bitmap = Bitmap.createBitmap(exportSize, exportSize, Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(bitmap)

        // Draw background
        val bgPaint = android.graphics.Paint().apply {
            color = backColor
            style = android.graphics.Paint.Style.FILL
        }
        canvas.drawRect(0f, 0f, exportSize.toFloat(), exportSize.toFloat(), bgPaint)

        // Draw foreground modules
        val fgPaint = android.graphics.Paint().apply {
            color = foreColor
            style = android.graphics.Paint.Style.FILL
            isAntiAlias = true
        }

        val N = matrix.size
        val cellW = exportSize.toFloat() / N
        val cellH = exportSize.toFloat() / N
        val rRadius = roundness * cellW

        for (y in 0 until N) {
            for (x in 0 until N) {
                if (matrix[y][x]) {
                    val left = x * cellW
                    val top = y * cellH
                    val right = left + cellW
                    val bottom = top + cellH
                    if (roundness > 0f) {
                        canvas.drawRoundRect(left, top, right, bottom, rRadius, rRadius, fgPaint)
                    } else {
                        canvas.drawRect(left, top, right, bottom, fgPaint)
                    }
                }
            }
        }

        val filename = "QR_${System.currentTimeMillis()}.png"
        val values = android.content.ContentValues().apply {
            put(android.provider.MediaStore.Images.Media.DISPLAY_NAME, filename)
            put(android.provider.MediaStore.Images.Media.MIME_TYPE, "image/png")
            put(android.provider.MediaStore.Images.Media.RELATIVE_PATH, android.os.Environment.DIRECTORY_PICTURES + "/QRCodeGenerator")
        }

        return try {
            val resolver = context.contentResolver
            val uri = resolver.insert(android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
            if (uri != null) {
                resolver.openOutputStream(uri).use { stream ->
                    if (stream != null) {
                        bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
                    }
                }
            }
            uri
        } catch (t: Throwable) {
            android.util.Log.e("QrGeneratorLogic", "Error saving QR code to public MediaStore gallery", t)
            null
        }
    }
}
