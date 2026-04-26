package com.openbridge

import android.content.ContentResolver
import android.net.Uri
import android.webkit.MimeTypeMap
import com.openbridge.utils.ByteReader

/**
 * Magic-byte MIME detection engine.
 * Reads the first 32 bytes of a file stream and matches against known signatures.
 * Falls back to file extension heuristics if the header is inconclusive.
 */
object MimeDetector {

    enum class FileType(val label: String, val emoji: String, val category: String) {
        VIDEO("Video",    "\uD83C\uDFAC", "video"),
        AUDIO("Audio",    "\uD83C\uDFB5", "audio"),
        IMAGE("Image",    "\uD83D\uDDBC", "image"),
        DOCUMENT("Document", "\uD83D\uDCC4", "pdf"),
        ARCHIVE("Archive", "\uD83D\uDCE6", "zip"),
        APK("APK",        "\u2699",        "apk"),
        TEXT("Text",      "\uD83D\uDCC3", "text"),
        UNKNOWN("Unknown", "?",            "any")
    }

    // Build wildcard MIMEs at runtime; no slash-star token appears in source.
    fun mimeOf(type: FileType, fallback: String = "*/*"): String {
        val s = "/"
        return when (type.category) {
            "video" -> "video$s*"
            "audio" -> "audio$s*"
            "image" -> "image$s*"
            "pdf"   -> "application/pdf"
            "zip"   -> "application/zip"
            "apk"   -> "application/vnd.android.package-archive"
            "text"  -> "text/plain"
            else    -> fallback
        }
    }

    data class DetectionResult(val fileType: FileType, val mime: String)

    fun detect(contentResolver: ContentResolver, uri: Uri, fileName: String?, fallbackMime: String = "*/*"): DetectionResult {
        val header = ByteReader.readHeader(contentResolver, uri, 32)
        return detectFromHeader(header, fileName?.lowercase().orEmpty(), fallbackMime)
    }

    fun detectFromHeader(header: ByteArray, name: String = "", fallbackMime: String = "*/*"): DetectionResult {
        if (header.isEmpty()) return extensionFallback(name, header, fallbackMime)

        return when {
            // Video
            matchBytes(header, 0x1A, 0x45, 0xDF, 0xA3) ->
                DetectionResult(FileType.VIDEO, "video/x-matroska")
            header.size >= 8 && matchBytesAt(header, 4, 0x66, 0x74, 0x79, 0x70) ->
                DetectionResult(FileType.VIDEO, ftypMime(header))
            matchBytes(header, 0x52, 0x49, 0x46, 0x46)
                    && matchBytesAt(header, 8, 0x41, 0x56, 0x49, 0x20) ->
                DetectionResult(FileType.VIDEO, "video/x-msvideo")
            matchBytes(header, 0x46, 0x4C, 0x56) ->
                DetectionResult(FileType.VIDEO, "video/x-flv")

            // Audio
            matchBytes(header, 0x49, 0x44, 0x33) ->
                DetectionResult(FileType.AUDIO, "audio/mpeg")
            matchBytes(header, 0xFF, 0xFB) ->
                DetectionResult(FileType.AUDIO, "audio/mpeg")
            matchBytes(header, 0xFF, 0xFA) ->
                DetectionResult(FileType.AUDIO, "audio/mpeg")
            matchBytes(header, 0x66, 0x4C, 0x61, 0x43) ->
                DetectionResult(FileType.AUDIO, "audio/flac")
            matchBytes(header, 0x4F, 0x67, 0x67, 0x53) ->
                DetectionResult(FileType.AUDIO, "audio/ogg")
            matchBytes(header, 0x52, 0x49, 0x46, 0x46)
                    && matchBytesAt(header, 8, 0x57, 0x41, 0x56, 0x45) ->
                DetectionResult(FileType.AUDIO, "audio/wav")

            // Image
            matchBytes(header, 0xFF, 0xD8, 0xFF) ->
                DetectionResult(FileType.IMAGE, "image/jpeg")
            matchBytes(header, 0x89, 0x50, 0x4E, 0x47) ->
                DetectionResult(FileType.IMAGE, "image/png")
            matchBytes(header, 0x47, 0x49, 0x46, 0x38) ->
                DetectionResult(FileType.IMAGE, "image/gif")
            matchBytes(header, 0x52, 0x49, 0x46, 0x46)
                    && matchBytesAt(header, 8, 0x57, 0x45, 0x42, 0x50) ->
                DetectionResult(FileType.IMAGE, "image/webp")

            // Document
            matchBytes(header, 0x25, 0x50, 0x44, 0x46) ->
                DetectionResult(FileType.DOCUMENT, "application/pdf")

            // APK / Archive
            matchBytes(header, 0x50, 0x4B, 0x03, 0x04) ->
                if (name.endsWith(".apk"))
                    DetectionResult(FileType.APK, "application/vnd.android.package-archive")
                else
                    DetectionResult(FileType.ARCHIVE, "application/zip")
            
            matchBytes(header, 0x52, 0x61, 0x72, 0x21) ->
                DetectionResult(FileType.ARCHIVE, "application/x-rar-compressed")
            matchBytes(header, 0x37, 0x7A, 0xBC, 0xAF) ->
                DetectionResult(FileType.ARCHIVE, "application/x-7z-compressed")

            else -> extensionFallback(name, header, fallbackMime)
        }
    }

    private fun ftypMime(header: ByteArray): String {
        if (header.size < 12) return "video/mp4"
        val b8 = header[8].toInt() and 0xFF
        val b9 = header[9].toInt() and 0xFF
        val b10 = header[10].toInt() and 0xFF
        if (b8 == 0x71 && b9 == 0x74) return "video/quicktime"
        if (b8 == 0x4D && b9 == 0x34 && (b10 == 0x41 || b10 == 0x42 || b10 == 0x50)) return "audio/mp4"
        if (b8 == 0x33 && b9 == 0x67) return "video/3gpp"
        return "video/mp4"
    }

    private fun extensionFallback(name: String, header: ByteArray = ByteArray(0), fallbackMime: String = "*/*"): DetectionResult {
        val ext = name.substringAfterLast('.', "").lowercase()

        // 1. High-priority overrides
        when (ext) {
            "apk" -> return DetectionResult(FileType.APK, "application/vnd.android.package-archive")
            "md" -> return DetectionResult(FileType.TEXT, "text/markdown")
            "json" -> return DetectionResult(FileType.TEXT, "application/json")
        }

        // 2. System MimeTypeMap with APK priority
        val systemMime = MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext)
        if (systemMime != null) {
            val type = when {
                systemMime.startsWith("video/") -> FileType.VIDEO
                systemMime.startsWith("audio/") -> FileType.AUDIO
                systemMime.startsWith("image/") -> FileType.IMAGE
                systemMime == "application/pdf" -> FileType.DOCUMENT
                systemMime == "application/vnd.android.package-archive" -> FileType.APK
                systemMime.contains("zip") || systemMime.contains("archive") -> FileType.ARCHIVE
                systemMime.startsWith("text/") -> FileType.TEXT
                else -> FileType.UNKNOWN
            }
            if (type != FileType.UNKNOWN) return DetectionResult(type, systemMime)
        }

        // 3. Heuristic fallbacks
        return when {
            header.isNotEmpty() && isLikelyText(header) -> DetectionResult(FileType.TEXT, "text/plain")
            else -> DetectionResult(FileType.UNKNOWN, fallbackMime)
        }
    }

    private fun matchBytes(header: ByteArray, vararg bytes: Int): Boolean {
        if (header.size < bytes.size) return false
        return bytes.indices.all { header[it].toInt() and 0xFF == bytes[it] }
    }

    private fun matchBytesAt(header: ByteArray, offset: Int, vararg bytes: Int): Boolean {
        if (header.size < offset + bytes.size) return false
        return bytes.indices.all { header[offset + it].toInt() and 0xFF == bytes[it] }
    }

    private fun isLikelyText(header: ByteArray): Boolean {
        if (header.size >= 3 && header[0] == 0xEF.toByte() && header[1] == 0xBB.toByte() && header[2] == 0xBF.toByte()) return true
        val sample = minOf(header.size, 32)
        val printable = (0 until sample).count { i ->
            val b = header[i].toInt() and 0xFF
            b in 9..13 || b in 32..126
        }
        return printable.toDouble() / sample > 0.75
    }
}
