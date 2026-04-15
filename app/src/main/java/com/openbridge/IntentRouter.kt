package com.openbridge

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.util.Log
import android.widget.Toast

/**
 * Fires a new ACTION_VIEW intent with the URI and resolved MIME type.
 */
object IntentRouter {

    private const val TAG = "IntentRouter"

    fun open(context: Context, uri: Uri, mimeType: String) {
        try {
            val viewIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, mimeType)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                // Use ClipData - essential for Android 10+ URI permission propagation
                clipData = ClipData.newRawUri(null, uri)
            }

            // Explicitly grant read permission to all possible handlers
            try {
                // Use MATCH_ALL to ensure we grant permissions to all apps that can handle this file
                val resInfoList = context.packageManager.queryIntentActivities(
                    viewIntent, PackageManager.MATCH_ALL
                )
                for (resolveInfo in resInfoList) {
                    val packageName = resolveInfo.activityInfo.packageName
                    try {
                        context.grantUriPermission(
                            packageName, uri,
                            Intent.FLAG_GRANT_READ_URI_PERMISSION
                        )
                    } catch (e: Exception) {
                        // Ignore if we can't grant to a specific package
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to grant permissions: ${e.message}")
            }

            val chooser = Intent.createChooser(viewIntent, "Open with").apply {
                // Chooser also needs these flags and ClipData to pass permissions down
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                clipData = ClipData.newRawUri(null, uri)
            }
            context.startActivity(chooser)
        } catch (e: Exception) {
            Log.e(TAG, "No handler found for MIME: $mimeType URI: $uri", e)
            Toast.makeText(context, "No app found for: $mimeType", Toast.LENGTH_SHORT).show()
        }
    }
}
