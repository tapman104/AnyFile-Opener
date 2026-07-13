package com.anyfile.x.engine.utils

import android.content.ContentResolver
import android.net.Uri
import android.util.Log
import java.io.InputStream

object ByteReader {

    private const val TAG = "ByteReader"

    /**
     * Reads up to [count] bytes from a URI starting from [offset].
     * Returns a ByteArray containing only the bytes actually read.
     * Returns an empty array on failure; never throws.
     */
    fun readBytes(contentResolver: ContentResolver, uri: Uri, offset: Long, count: Int): ByteArray {
        return try {
            contentResolver.openInputStream(uri)?.use { stream ->
                skipFully(stream, offset)
                val buffer = ByteArray(count)
                var totalRead = 0
                while (totalRead < count) {
                    val bytesRead = stream.read(buffer, totalRead, count - totalRead)
                    if (bytesRead == -1) break
                    totalRead += bytesRead
                }
                if (totalRead > 0) {
                    if (totalRead == count) buffer else buffer.copyOf(totalRead)
                } else {
                    ByteArray(0)
                }
            } ?: ByteArray(0)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to read bytes from $uri at offset $offset", e)
            ByteArray(0)
        }
    }

    private fun skipFully(stream: InputStream, n: Long) {
        var remaining = n
        while (remaining > 0) {
            val skipped = stream.skip(remaining)
            if (skipped <= 0) break
            remaining -= skipped
        }
    }

    fun readHeader(contentResolver: ContentResolver, uri: Uri, count: Int = 256): ByteArray {
        return readBytes(contentResolver, uri, 0, count)
    }
}
