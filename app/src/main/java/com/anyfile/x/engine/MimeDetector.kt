package com.anyfile.x.engine

import android.content.ContentResolver
import android.net.Uri
import android.webkit.MimeTypeMap
import com.anyfile.x.engine.utils.ByteReader

/**
 * Magic-byte MIME detection engine.
 * Reads a header of a file stream and matches against known signatures.
 * Falls back to file extension heuristics if the header is inconclusive.
 */
object MimeDetector {

    private val ALL_MIME = "*${'/'}*"

    enum class FileType(val label: String, val emoji: String, val category: String) {
        VIDEO("Video",    "\uD83C\uDFAC", "video"),
        AUDIO("Audio",    "\uD83C\uDFB5", "audio"),
        IMAGE("Image",    "\uD83D\uDDBC", "image"),
        DOCUMENT("Document", "\uD83D\uDCC4", "pdf"),
        ARCHIVE("Archive", "\uD83D\uDCE6", "zip"),
        APK("APK",        "\u2699",        "apk"),
        TEXT("Text",      "\uD83D\uDCC3", "text"),
        FONT("Font",      "\uD83D\uDD24", "font"),
        UNKNOWN("Unknown", "?",            "any")
    }

    fun mimeOf(type: FileType, fallback: String = ALL_MIME): String {
        val s = "/"
        return when (type.category) {
            "video" -> "video$s*"
            "audio" -> "audio$s*"
            "image" -> "image$s*"
            "font"  -> "font$s*"
            "pdf"   -> "application/pdf"
            "zip"   -> "application/zip"
            "apk"   -> "application/vnd.android.package-archive"
            "text"  -> "text/plain"
            else    -> fallback
        }
    }

    data class DetectionResult(val fileType: FileType, val mime: String)

    fun detect(contentResolver: ContentResolver, uri: Uri, fileName: String?, fallbackMime: String = ALL_MIME): DetectionResult {
        // Read 33KB to support ISO detection (offset 32769) and TAR signatures (offset 257)
        val header = ByteReader.readHeader(contentResolver, uri, 33000)
        return detectFromHeader(header, fileName?.lowercase().orEmpty(), fallbackMime, contentResolver, uri)
    }

    fun detectFromHeader(
        header: ByteArray, 
        name: String = "", 
        fallbackMime: String = ALL_MIME,
        contentResolver: ContentResolver? = null,
        uri: Uri? = null
    ): DetectionResult {
        if (header.isEmpty()) return extensionFallback(name, header, fallbackMime)

        return when {
            // Video
            matchBytes(header, 0x1A, 0x45, 0xDF, 0xA3) ->
                DetectionResult(FileType.VIDEO, "video/webm")
            header.size >= 8 && matchBytesAt(header, 4, 0x66, 0x74, 0x79, 0x70) ->
                handleFtyp(header)
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
            matchBytes(header, 0x4F, 0x67, 0x67, 0x53) -> {
                // Check for OpusHead tag in OGG container (usually offset 28)
                if (matchBytesAt(header, 28, 0x4F, 0x70, 0x75, 0x73, 0x48, 0x65, 0x61, 0x64)) {
                    DetectionResult(FileType.AUDIO, "audio/ogg; codecs=opus")
                } else {
                    DetectionResult(FileType.AUDIO, "audio/ogg")
                }
            }
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
            matchBytes(header, 0x42, 0x4D) ->
                DetectionResult(FileType.IMAGE, "image/bmp")
            matchBytes(header, 0x49, 0x49, 0x2A, 0x00) || matchBytes(header, 0x4D, 0x4D, 0x00, 0x2A) ->
                DetectionResult(FileType.IMAGE, "image/tiff")

            // Document
            matchBytes(header, 0x25, 0x50, 0x44, 0x46) ->
                DetectionResult(FileType.DOCUMENT, "application/pdf")

            // Fonts
            matchBytes(header, 0x00, 0x01, 0x00, 0x00, 0x00) ->
                DetectionResult(FileType.FONT, "font/ttf")
            matchBytes(header, 0x4F, 0x54, 0x54, 0x4F) ->
                DetectionResult(FileType.FONT, "font/otf")
            matchBytes(header, 0x77, 0x4F, 0x46, 0x46) ->
                DetectionResult(FileType.FONT, "font/woff")
            matchBytes(header, 0x77, 0x4F, 0x46, 0x32) ->
                DetectionResult(FileType.FONT, "font/woff2")

            // ZIP-based (APK, Office, EPUB, ZIP)
            matchBytes(header, 0x50, 0x4B, 0x03, 0x04) ->
                handleZipBased(header, name, contentResolver, uri)

            // Other Archives
            matchBytes(header, 0x52, 0x61, 0x72, 0x21, 0x1A, 0x07) ->
                DetectionResult(FileType.ARCHIVE, "application/x-rar-compressed")
            matchBytes(header, 0x37, 0x7A, 0xBC, 0xAF) ->
                DetectionResult(FileType.ARCHIVE, "application/x-7z-compressed")
            matchBytes(header, 0x1F, 0x8B) ->
                DetectionResult(FileType.ARCHIVE, "application/gzip")
            matchBytes(header, 0xFD, 0x37, 0x7A, 0x58, 0x5A, 0x00) ->
                DetectionResult(FileType.ARCHIVE, "application/x-xz")
            matchBytes(header, 0x42, 0x5A, 0x68) ->
                DetectionResult(FileType.ARCHIVE, "application/x-bzip2")
            header.size >= 262 && matchBytesAt(header, 257, 0x75, 0x73, 0x74, 0x61, 0x72) ->
                DetectionResult(FileType.ARCHIVE, "application/x-tar")
            header.size >= 32774 && matchBytesAt(header, 32769, 0x43, 0x44, 0x30, 0x30, 0x31) ->
                DetectionResult(FileType.ARCHIVE, "application/x-iso9660-image")

            else -> extensionFallback(name, header, fallbackMime)
        }
    }

    private fun handleZipBased(header: ByteArray, name: String, contentResolver: ContentResolver? = null, uri: Uri? = null): DetectionResult {
        // 1. Deep-Peek Verification (if resolver/uri available)
        if (contentResolver != null && uri != null) {
            val deepMime = verifyZipContent(contentResolver, uri)
            if (deepMime != null) {
                val type = when {
                    deepMime == "application/vnd.android.package-archive" -> FileType.APK
                    deepMime == "application/epub+zip" -> FileType.DOCUMENT
                    deepMime.contains("officedocument") -> FileType.DOCUMENT
                    else -> FileType.ARCHIVE
                }
                return DetectionResult(type, deepMime)
            }
        }

        // 2. Heuristic header check (without full stream access)
        return when {
            // EPUB check: "mimetype" entry usually at offset 30
            header.size >= 38 && matchBytesAt(header, 30, 0x6D, 0x69, 0x6D, 0x65, 0x74, 0x79, 0x70, 0x65) ->
                DetectionResult(FileType.DOCUMENT, "application/epub+zip")

            // Office check: "[Content_Types].xml" entry usually at offset 30
            header.size >= 49 && matchBytesAt(header, 30, 0x5B, 0x43, 0x6F, 0x6E, 0x74, 0x65, 0x6E, 0x74, 0x5F, 0x54, 0x79, 0x70, 0x65, 0x73, 0x5D, 0x2E, 0x78, 0x6D, 0x6C) -> {
                val mime = when {
                    name.endsWith(".docx") -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
                    name.endsWith(".xlsx") -> "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
                    name.endsWith(".pptx") -> "application/vnd.openxmlformats-officedocument.presentationml.presentation"
                    else -> "application/vnd.openxmlformats-officedocument"
                }
                DetectionResult(FileType.DOCUMENT, mime)
            }

            // 3. Extension fallback if deep-peek failed or wasn't available
            name.endsWith(".apk") -> DetectionResult(FileType.APK, "application/vnd.android.package-archive")
            name.endsWith(".epub") -> DetectionResult(FileType.DOCUMENT, "application/epub+zip")
            name.endsWith(".docx") -> DetectionResult(FileType.DOCUMENT, "application/vnd.openxmlformats-officedocument.wordprocessingml.document")
            
            else -> DetectionResult(FileType.ARCHIVE, "application/zip")
        }
    }

    private fun verifyZipContent(contentResolver: ContentResolver, uri: Uri): String? {
        return try {
            contentResolver.openInputStream(uri)?.use { stream ->
                val zipStream = java.util.zip.ZipInputStream(stream)
                var count = 0
                var foundManifest = false
                var foundContent = false
                var foundEpub = false
                
                // Inspect first 30 entries for efficiency
                while (count < 30) {
                    val entry = zipStream.getNextEntry() ?: break
                    val entryName = entry.name
                    
                    if (entryName == "AndroidManifest.xml") foundManifest = true
                    if (entryName == "[Content_Types].xml") foundContent = true
                    if (entryName == "mimetype") foundEpub = true
                    
                    if (foundManifest) return "application/vnd.android.package-archive"
                    
                    zipStream.closeEntry()
                    count++
                }
                
                when {
                    foundEpub -> "application/epub+zip"
                    foundContent -> "application/vnd.openxmlformats-officedocument"
                    else -> null
                }
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun handleFtyp(header: ByteArray): DetectionResult {
        val mime = ftypMime(header)
        val type = when {
            mime.startsWith("image/") -> FileType.IMAGE
            mime.startsWith("audio/") -> FileType.AUDIO
            else -> FileType.VIDEO
        }
        return DetectionResult(type, mime)
    }

    private fun ftypMime(header: ByteArray): String {
        if (header.size < 12) return "video/mp4"
        return when {
            matchBytesAt(header, 8, 0x68, 0x65, 0x69, 0x63) || // heic
            matchBytesAt(header, 8, 0x68, 0x65, 0x69, 0x78) || // heix
            matchBytesAt(header, 8, 0x68, 0x65, 0x76, 0x63) || // hevc
            matchBytesAt(header, 8, 0x6D, 0x69, 0x66, 0x31) -> "image/heic" // mif1

            matchBytesAt(header, 8, 0x61, 0x76, 0x69, 0x66) -> "image/avif" // avif

            matchBytesAt(header, 8, 0x71, 0x74, 0x20, 0x20) -> "video/quicktime"
            matchBytesAt(header, 8, 0x4D, 0x34, 0x41) || // M4A
            matchBytesAt(header, 8, 0x4D, 0x34, 0x42) || // M4B
            matchBytesAt(header, 8, 0x4D, 0x34, 0x50) -> "audio/mp4" // M4P
            matchBytesAt(header, 8, 0x33, 0x67) -> "video/3gpp" // 3gp*
            else -> "video/mp4"
        }
    }

    private fun extensionFallback(name: String, header: ByteArray = ByteArray(0), fallbackMime: String = ALL_MIME): DetectionResult {
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
                systemMime.startsWith("font/") -> FileType.FONT
                systemMime == "application/pdf" -> FileType.DOCUMENT
                systemMime == "application/vnd.android.package-archive" -> FileType.APK
                systemMime.contains("zip") || systemMime.contains("archive") || systemMime.contains("tar") -> FileType.ARCHIVE
                systemMime.startsWith("text/") -> FileType.TEXT
                else -> FileType.UNKNOWN
            }
            if (type != FileType.UNKNOWN) return DetectionResult(type, systemMime)
        }

        // 3. Heuristic fallbacks
        if (header.isNotEmpty()) {
            val scriptResult = detectScript(header, name)
            if (scriptResult != null) return scriptResult
            
            if (isLikelyText(header)) {
                val structuredMime = detectStructuredText(header) ?: "text/plain"
                val charset = getCharsetName(header)
                return DetectionResult(FileType.TEXT, "$structuredMime; charset=$charset")
            }
        }
        
        return DetectionResult(FileType.UNKNOWN, fallbackMime)
    }

    private fun detectScript(header: ByteArray, name: String): DetectionResult? {
        if (header.size < 2) return null
        
        val sampleSize = minOf(header.size, 1024)
        val content = String(header, 0, sampleSize, Charsets.UTF_8)
        
        // Shebang detection (#! is 0x23 0x21)
        if (header[0] == 0x23.toByte() && header[1] == 0x21.toByte()) {
            return when {
                content.contains("/usr/bin/env python") || content.contains("/usr/bin/python") ->
                    DetectionResult(FileType.TEXT, "text/x-python")
                content.contains("/bin/sh") || content.contains("/bin/bash") ->
                    DetectionResult(FileType.TEXT, "text/x-shellscript")
                content.contains("/usr/bin/env node") ->
                    DetectionResult(FileType.TEXT, "application/javascript")
                content.contains("/usr/bin/env kotlin") ->
                    DetectionResult(FileType.TEXT, "text/x-kotlin")
                else -> null
            }
        }
        
        // Content structure checks
        return when {
            content.startsWith("fun ") || (content.startsWith("import ") && name.endsWith(".kt")) ->
                DetectionResult(FileType.TEXT, "text/x-kotlin")
            content.startsWith("def ") || content.startsWith("import ") || content.startsWith("from ") ->
                DetectionResult(FileType.TEXT, "text/x-python")
            else -> null
        }
    }

    private fun detectStructuredText(header: ByteArray): String? {
        val charset = when {
            header.size >= 3 && header[0] == 0xEF.toByte() && header[1] == 0xBB.toByte() && header[2] == 0xBF.toByte() -> Charsets.UTF_8
            header.size >= 2 && header[0] == 0xFF.toByte() && header[1] == 0xFE.toByte() -> Charsets.UTF_16LE
            header.size >= 2 && header[0] == 0xFE.toByte() && header[1] == 0xFF.toByte() -> Charsets.UTF_16BE
            else -> Charsets.UTF_8
        }
        
        val bomSize = when {
            header.size >= 3 && header[0] == 0xEF.toByte() && header[1] == 0xBB.toByte() && header[2] == 0xBF.toByte() -> 3
            header.size >= 2 && (header[0] == 0xFF.toByte() || header[0] == 0xFE.toByte()) -> 2
            else -> 0
        }

        val sampleSize = minOf(header.size - bomSize, 512)
        if (sampleSize <= 0) return null
        
        val content = try {
            String(header, bomSize, sampleSize, charset).trim()
        } catch (e: Exception) {
            ""
        }
        if (content.isEmpty()) return null

        return when {
            content.startsWith("{") || content.startsWith("[") -> "application/json"
            content.startsWith("<?xml", ignoreCase = true) -> "text/xml"
            content.startsWith("<") -> {
                val lower = content.lowercase()
                if (lower.startsWith("<!doctype html") || lower.contains("<html")) "text/html"
                else "text/xml"
            }
            content.startsWith("---") || """(?m)^\w+:\s+.+""".toRegex().containsMatchIn(content) -> "text/yaml"
            content.startsWith("# ") || content.contains("**") || content.contains("- [ ]") -> "text/markdown"
            isCsv(content) -> "text/csv"
            else -> null
        }
    }

    private fun isCsv(content: String): Boolean {
        val lines = content.lines().filter { it.isNotBlank() }.take(5)
        if (lines.size < 2) return false
        val columnCounts = lines.map { it.split(',').size }
        return columnCounts.all { it > 1 && it == columnCounts[0] }
    }

    private fun getCharsetName(header: ByteArray): String {
        if (header.size >= 3 && header[0] == 0xEF.toByte() && header[1] == 0xBB.toByte() && header[2] == 0xBF.toByte()) return "UTF-8"
        if (header.size >= 2) {
            if (header[0] == 0xFF.toByte() && header[1] == 0xFE.toByte()) return "UTF-16LE"
            if (header[0] == 0xFE.toByte() && header[1] == 0xFF.toByte()) return "UTF-16BE"
        }
        val sampleSize = minOf(header.size, 1024)
        val allAscii = (0 until sampleSize).all { i -> (header[i].toInt() and 0xFF) <= 0x7F }
        return if (allAscii) "ASCII" else "UTF-8"
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
        val sample = minOf(header.size, 1024)
        if (sample == 0) return false
        val printable = (0 until sample).count { i ->
            val b = header[i].toInt() and 0xFF
            b in 9..13 || b in 32..126
        }
        return printable.toDouble() / sample > 0.8
    }
}
