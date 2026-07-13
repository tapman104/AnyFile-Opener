package com.anyfile.x

import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build

class ChooserResultReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val component = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(Intent.EXTRA_CHOSEN_COMPONENT, ComponentName::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra(Intent.EXTRA_CHOSEN_COMPONENT)
        }
        
        val mimeType = intent.getStringExtra("mime_type")
        if (component != null && mimeType != null) {
            IntentRouter.saveLastApp(context, mimeType, component.packageName)
        }
    }
}
