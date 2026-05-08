package com.anyfile.x

import android.content.ContentResolver
import android.net.Uri
import android.util.Log

object FileHeaderScanner {

    enum class FileType(val label: String, val mimeType: String, val iconRes: Int) {
        VIDEO("Video", "video/*", android.R.drawable.ic_media_play),
        AUDIO("Audio", "audio/*", android.R.drawable.ic_lock_silent_mode_off),
        IMAGE("Image", "image/*", android.R.drawable.ic_menu_gallery),
        DOCUMENT("Document", "application/pdf", android.R.drawable.ic_menu_edit),
        ARCHIVE("Archive", "application/zip", android.R.drawable.ic_menu_save),
        APK("APK", "application/vnd.android.package-archive", android.R.drawable.sym_def_app_icon),
        TEXT("Text", "text/plain", android.R.drawable.ic_menu_sort_alphabetically),
        UNKNOWN("Unknown", "application/octet-stream", android.R.drawable.ic_menu_help)
    }

    fun detectType(contentResolver: ContentResolver, uri: Uri, fileName: String?): FileType {
        val header = ByteArray(16)
        try {
            contentResolver.openInputStream(uri)?.use { 
                it.read(header)
            }
        } catch (e: Exception) {
            Log.e("FileHeaderScanner", "Error reading header", e)
        }

        val name = fileName?.lowercase() ?: ""

        return when {
            // Video
            match(header, byteArrayOf(0x1A, 0x45, 0xDF.toByte(), 0xA3.toByte())) -> FileType.VIDEO // MKV
            match(header, byteArrayOf(0x00, 0x00, 0x00, 0x18, 0x66, 0x74, 0x79, 0x70)) -> FileType.VIDEO // MP4
            match(header, byteArrayOf(0x00, 0x00, 0x00, 0x20, 0x66, 0x74, 0x79, 0x70)) -> FileType.VIDEO // MP4
            match(header, byteArrayOf(0x52, 0x49, 0x46, 0x46)) && matchAt(header, 8, byteArrayOf(0x41, 0x56, 0x49, 0x20)) -> FileType.VIDEO // AVI
            
            // Audio
            match(header, byteArrayOf(0x49, 0x44, 0x33)) -> FileType.AUDIO // MP3 ID3
            match(header, byteArrayOf(0xFF.toByte(), 0xFB.toByte())) -> FileType.AUDIO // MP3 Raw
            match(header, byteArrayOf(0x66, 0x4C, 0x61, 0x43)) -> FileType.AUDIO // FLAC
            match(header, byteArrayOf(0x4F, 0x67, 0x67, 0x53)) -> FileType.AUDIO // OGG
            
            // Image
            match(header, byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte())) -> FileType.IMAGE // JPG
            match(header, byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47)) -> FileType.IMAGE // PNG
            match(header, byteArrayOf(0x47, 0x49, 0x46, 0x38)) -> FileType.IMAGE // GIF
            match(header, byteArrayOf(0x52, 0x49, 0x46, 0x46)) && matchAt(header, 8, byteArrayOf(0x57, 0x45, 0x42, 0x50)) -> FileType.IMAGE // WEBP
            
            // PDF
            match(header, byteArrayOf(0x25, 0x50, 0x44, 0x46)) -> FileType.DOCUMENT
            
            // Archives / APK (APK signature is ZIP)
            match(header, byteArrayOf(0x50, 0x4B, 0x03, 0x04)) -> {
                if (name.endsWith(".apk")) FileType.APK else FileType.ARCHIVE
            }
            match(header, byteArrayOf(0x52, 0x61, 0x72, 0x21)) -> FileType.ARCHIVE // RAR
            match(header, byteArrayOf(0x37, 0x7A, 0xBC.toByte(), 0xAF.toByte())) -> FileType.ARCHIVE // 7Z
            
            // Fallback to extension for known types if header failed
            name.endsWith(".mp4") || name.endsWith(".mkv") || name.endsWith(".avi") || name.endsWith(".mov") -> FileType.VIDEO
            name.endsWith(".mp3") || name.endsWith(".flac") || name.endsWith(".ogg") || name.endsWith(".m4a") -> FileType.AUDIO
            name.endsWith(".jpg") || name.endsWith(".jpeg") || name.endsWith(".png") || name.endsWith(".webp") || name.endsWith(".gif") -> FileType.IMAGE
            name.endsWith(".pdf") -> FileType.DOCUMENT
            name.endsWith(".zip") || name.endsWith(".rar") || name.endsWith(".7z") -> FileType.ARCHIVE
            name.endsWith(".apk") -> FileType.APK
            
            // Text fallback
            isText(header) -> FileType.TEXT
            
            else -> FileType.UNKNOWN
        }
    }

    private fun match(header: ByteArray, signature: ByteArray): Boolean {
        if (header.size < signature.size) return false
        for (i in signature.indices) {
            if (header[i] != signature[i]) return false
        }
        return true
    }

    private fun matchAt(header: ByteArray, offset: Int, signature: ByteArray): Boolean {
        if (header.size < offset + signature.size) return false
        for (i in signature.indices) {
            if (header[offset + i] != signature[i]) return false
        }
        return true
    }

    private fun isText(header: ByteArray): Boolean {
        if (header.isEmpty() || header[0] == 0.toByte()) return false
        for (b in header) {
            val i = b.toInt() and 0xFF
            if (i < 32 && i != 9 && i != 10 && i != 13) return false
        }
        return true
    }
}
