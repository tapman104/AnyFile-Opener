package com.anyfile.x

import android.app.PendingIntent
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.util.Log
import android.provider.DocumentsContract
import android.widget.Toast
import androidx.appcompat.app.AlertDialog

/**
 * Fires a new ACTION_VIEW intent with the URI and resolved MIME type.
 */
object IntentRouter {

    private const val TAG = "IntentRouter"
    private const val PREFS_NAME = "mime_prefs"

    fun open(context: Context, uri: Uri, mimeType: String) {
        val viewIntent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, mimeType)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            clipData = ClipData.newRawUri(null, uri)
        }

        val resInfoList = try {
            context.packageManager.queryIntentActivities(
                viewIntent, PackageManager.MATCH_ALL
            )
        } catch (e: Exception) {
            emptyList()
        }

        if (resInfoList.isEmpty()) {
            showNoAppDialog(context, uri, mimeType)
            return
        }

        // Explicitly grant read permission to all possible handlers
        for (resolveInfo in resInfoList) {
            val packageName = resolveInfo.activityInfo.packageName
            try {
                context.grantUriPermission(
                    packageName, uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (e: Exception) {
                // Ignore
            }
        }

        try {
            // Use a BroadcastReceiver to capture which app the user picks
            val receiverIntent = Intent(context, ChooserResultReceiver::class.java).apply {
                putExtra("mime_type", mimeType)
            }
            
            val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
            } else {
                PendingIntent.FLAG_UPDATE_CURRENT
            }
            
            val pendingIntent = PendingIntent.getBroadcast(
                context, 0, receiverIntent, flags
            )

            val chooser = Intent.createChooser(viewIntent, "Open with", pendingIntent.intentSender).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                clipData = ClipData.newRawUri(null, uri)
            }
            context.startActivity(chooser)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start chooser", e)
            showNoAppDialog(context, uri, mimeType)
        }
    }

    private fun showNoAppDialog(context: Context, uri: Uri, mimeType: String) {
        AlertDialog.Builder(context)
            .setTitle("No app found")
            .setMessage("No app is installed that can handle '$mimeType'. Would you like to copy the file URI to your clipboard?")
            .setPositiveButton("Copy URI") { _, _ ->
                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                val clip = ClipData.newRawUri("File URI", uri)
                clipboard.setPrimaryClip(clip)
                Toast.makeText(context, "URI copied", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Dismiss", null)
            .show()
    }

    fun saveLastApp(context: Context, mimeType: String, packageName: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(mimeType, packageName).apply()
    }

    fun getLastApp(context: Context, mimeType: String): String? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(mimeType, null)
    }

    /**
     * Attempts to open the containing folder of the given URI.
     * Works best with DocumentsProvider URIs (SAF) and file:// URIs.
     */
    fun openFolder(context: Context, uri: Uri) {
        try {
            // Case 1: SAF Document URI
            if (DocumentsContract.isDocumentUri(context, uri)) {
                val docId = DocumentsContract.getDocumentId(uri)
                val parentId = when {
                    docId.contains("/") -> docId.substringBeforeLast("/")
                    docId.contains(":") -> docId.substringBeforeLast(":") + ":"
                    else -> null
                }

                if (parentId != null) {
                    val parentUri = DocumentsContract.buildDocumentUri(uri.authority, parentId)
                    val intent = Intent(Intent.ACTION_VIEW).apply {
                        setDataAndType(parentUri, "vnd.android.document/directory")
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    try {
                        context.startActivity(intent)
                        return
                    } catch (e: Exception) {
                        // Fallback: try android.provider.action.BROWSE_DOCUMENT_ROOT
                        val browseIntent = Intent("android.provider.action.BROWSE_DOCUMENT_ROOT").apply {
                            data = parentUri
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        context.startActivity(browseIntent)
                        return
                    }
                }
            }

            // Case 2: File URI
            if (uri.scheme == "file") {
                val path = uri.path ?: return
                val file = java.io.File(path)
                val parent = file.parentFile
                if (parent != null && parent.exists()) {
                    val parentUri = Uri.fromFile(parent)
                    val intent = Intent(Intent.ACTION_VIEW).apply {
                        setDataAndType(parentUri, "resource/folder")
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(intent)
                    return
                }
            }
            
            // If we're here, we couldn't resolve the parent or start the intent
            Toast.makeText(context, "Cannot open folder for this file type", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Log.e(TAG, "Error opening folder", e)
            Toast.makeText(context, "Error: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
        }
    }
}
