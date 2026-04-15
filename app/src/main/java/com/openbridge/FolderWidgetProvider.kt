package com.openbridge

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.RemoteViews

class FolderWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (appWidgetId in appWidgetIds) {
            val views = RemoteViews(context.packageName, R.layout.widget_folder_shortcut)

            // Intent to open Android folder directly
            val intent = Intent("android.provider.action.BROWSE_DOCUMENT_ROOT").apply {
                data = Uri.parse("content://com.android.externalstorage.documents/root/primary")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            // Fallback intents if the above fails
            val pendingIntent = PendingIntent.getBroadcast(
                context, 0, Intent(context, FolderWidgetProvider::class.java).apply {
                    action = "OPEN_FOLDER"
                }, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            views.setOnClickPendingIntent(R.id.widget_root, pendingIntent)
            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == "OPEN_FOLDER") {
            val packages = arrayOf("com.google.android.documentsui", "com.android.documentsui")
            var started = false

            // Try direct component access for FilesActivity
            for (pkg in packages) {
                try {
                    val browseIntent = Intent().apply {
                        component = ComponentName(pkg, "com.android.documentsui.files.FilesActivity")
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(browseIntent)
                    started = true
                    break
                } catch (e: Exception) {}
            }

            if (!started) {
                // Try browse root action
                try {
                    val rootIntent = Intent("android.provider.action.BROWSE_DOCUMENT_ROOT").apply {
                        data = Uri.parse("content://com.android.externalstorage.documents/root/primary")
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(rootIntent)
                } catch (e: Exception) {
                    // Fallback to standard view
                    val viewIntent = Intent(Intent.ACTION_VIEW).apply {
                        data = Uri.parse("content://com.android.externalstorage.documents/root/primary")
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    try {
                        context.startActivity(viewIntent)
                    } catch (e2: Exception) {}
                }
            }
        }
    }
}
