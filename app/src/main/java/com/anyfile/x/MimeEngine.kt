package com.anyfile.x

import android.webkit.MimeTypeMap

object MimeEngine {

    /**
     * Common MIME type overrides that might not be in the system's MimeTypeMap.
     */
    private val commonOverrides = mapOf(
        "apk" to "application/vnd.android.package-archive",
        "json" to "application/json",
        "txt" to "text/plain",
        "md" to "text/markdown",
        "log" to "text/plain",
        "cfg" to "text/plain",
        "ini" to "text/plain",
        "sh" to "application/x-sh",
        "bat" to "application/x-msdos-program"
    )

    fun getMimeType(extension: String): String {
        val ext = extension.lowercase().removePrefix(".")
        
        // 1. Check custom overrides
        commonOverrides[ext]?.let { return it }

        // 2. Check system MimeTypeMap
        val systemType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext)
        if (systemType != null) return systemType

        // 3. Fallback to generic binary stream if unknown
        return "application/octet-stream"
    }

    /**
     * Returns a broad list of MIME types for the user to choose from.
     */
    fun getAllCommonMimeTypes(): List<String> {
        return listOf(
            "text/plain",
            "text/html",
            "text/markdown",
            "application/json",
            "application/pdf",
            "application/zip",
            "application/vnd.android.package-archive",
            "image/jpeg",
            "image/png",
            "image/webp",
            "video/mp4",
            "audio/mpeg",
            "application/octet-stream"
        )
    }
}
