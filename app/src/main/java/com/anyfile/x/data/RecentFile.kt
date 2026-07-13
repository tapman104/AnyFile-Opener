package com.anyfile.x.data

@kotlinx.serialization.Serializable
data class RecentFile(
    val uri: String,
    val fileName: String,
    val mimeType: String,
    val timestamp: Long = System.currentTimeMillis()
)
