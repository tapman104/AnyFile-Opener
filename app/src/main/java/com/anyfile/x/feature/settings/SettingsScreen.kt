package com.anyfile.x.feature.settings

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.anyfile.x.data.PrefsManager
import com.anyfile.x.ui.ThemePreference
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    prefsManager: PrefsManager,
    onBackClick: () -> Unit
) {
    val themePreference by prefsManager.themePreference.collectAsStateWithLifecycle(initialValue = ThemePreference.SYSTEM_DEFAULT)
    var showThemeDialog by remember { mutableStateOf(false) }
    var showAboutDialog by remember { mutableStateOf(false) }
    var showHelpDialog by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // APPEARANCE SECTION
            item {
                SectionHeader("APPEARANCE")
            }

            item {
                ListItem(
                    headlineContent = { Text("Theme") },
                    supportingContent = {
                        Text(
                            when (themePreference) {
                                ThemePreference.SYSTEM_DEFAULT -> "System Default"
                                ThemePreference.AMOLED -> "AMOLED Pure Black"
                                ThemePreference.MATERIAL_YOU -> "Material You"
                            }
                        )
                    },
                    modifier = Modifier.clickable { showThemeDialog = true }
                )
            }

            // SUPPORT THE DEVELOPER SECTION
            item {
                SectionHeader("SUPPORT THE DEVELOPER")
            }

            item {
                ListItem(
                    headlineContent = { Text("GitHub Sponsors") },
                    supportingContent = { Text("Support future development") },
                    leadingContent = {
                        Icon(
                            imageVector = Icons.Default.Favorite,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    },
                    modifier = Modifier.clickable {
                        openUrl(context, "https://github.com/sponsors/tapman104")
                    }
                )
            }

            item {
                ListItem(
                    headlineContent = { Text("Pay via UPI") },
                    supportingContent = { Text("Support using your preferred UPI app") },
                    leadingContent = {
                        Icon(
                            imageVector = Icons.Default.Payment,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    },
                    modifier = Modifier.clickable {
                        launchUpiPayment(context, snackbarHostState, scope)
                    }
                )
            }

            // ABOUT SECTION
            item {
                SectionHeader("ABOUT")
            }

            item {
                ListItem(
                    headlineContent = { Text("How to Use") },
                    supportingContent = { Text("Help & Guide") },
                    modifier = Modifier.clickable { showHelpDialog = true }
                )
            }

            item {
                ListItem(
                    headlineContent = { Text("About") },
                    supportingContent = { Text("Learn about AnyFile Opener") },
                    modifier = Modifier.clickable { showAboutDialog = true }
                )
            }

            item {
                ListItem(
                    headlineContent = { Text("Buy me a coffee") },
                    supportingContent = { Text("Support the developer ❤️") },
                    modifier = Modifier.clickable {
                        openUrl(context, "https://buymeacoffee.com/tapman")
                    }
                )
            }

            item {
                val version = try {
                    val pInfo = context.packageManager.getPackageInfo(context.packageName, 0)
                    pInfo.versionName
                } catch (e: Exception) {
                    "1.0"
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Version $version",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }

    if (showThemeDialog) {
        ThemeSelectionDialog(
            currentSelection = themePreference,
            onDismiss = { showThemeDialog = false },
            prefsManager = prefsManager
        )
    }

    if (showHelpDialog) {
        AlertDialog(
            onDismissRequest = { showHelpDialog = false },
            title = { Text("Help & Guide") },
            text = {
                Text("AnyFile Opener is your swiss-army knife for files.\n\n" +
                        "How to use:\n" +
                        "1. Pick a file: Tap the center area or use 'System File Manager' for restricted folders.\n" +
                        "2. Auto-Detect: The app scans the binary header to find the real type.\n" +
                        "3. Open: Use 'Open Normally' or 'Open as...' to pick a category.\n\n" +
                        "Pro Tips:\n" +
                        "• Advanced: Type custom MIMEs like 'text/xml' if the auto-detect isn't specific enough.\n" +
                        "• Inspect: Use 'INSPECT BINARY' to see if a file is corrupted or to read hidden text headers.")
            },
            confirmButton = {
                TextButton(onClick = { showHelpDialog = false }) { Text("Got it") }
            }
        )
    }

    if (showAboutDialog) {
        AlertDialog(
            onDismissRequest = { showAboutDialog = false },
            title = { Text("About AnyFile Opener") },
            text = {
                Text("AnyFile Opener is a versatile utility designed to handle files that Android usually struggles with. It automatically identifies the correct file type using magic-byte detection and lets you override it manually to open files with any app you choose.\n\nCreated with ❤️ by Tapman")
            },
            confirmButton = {
                TextButton(onClick = { showAboutDialog = false }) { Text("OK") }
            }
        )
    }
}

@Composable
fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 16.dp, top = 24.dp, end = 16.dp, bottom = 8.dp)
    )
}

private fun openUrl(context: Context, url: String) {
    try {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    } catch (e: Exception) {
        Toast.makeText(context, "Could not open browser", Toast.LENGTH_SHORT).show()
    }
}

private fun launchUpiPayment(
    context: Context,
    snackbarHostState: SnackbarHostState,
    scope: kotlinx.coroutines.CoroutineScope
) {
    val upiUri = Uri.Builder()
        .scheme("upi")
        .authority("pay")
        .appendQueryParameter("pa", "tapman2@ybl")
        .appendQueryParameter("pn", "Tapman")
        .appendQueryParameter("cu", "INR")
        .build()

    val intent = Intent(Intent.ACTION_VIEW, upiUri)
    val chooser = Intent.createChooser(intent, "Pay via UPI")

    try {
        context.startActivity(chooser)
    } catch (e: Exception) {
        val message = "No compatible UPI app installed on this device"
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        scope.launch {
            snackbarHostState.showSnackbar(message)
        }
    }
}

@Composable
fun ThemeSelectionDialog(
    currentSelection: ThemePreference,
    onDismiss: () -> Unit,
    prefsManager: PrefsManager
) {
    val scope = rememberCoroutineScope()
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select Theme") },
        text = {
            Column {
                ThemeOption(
                    label = "System Default",
                    selected = currentSelection == ThemePreference.SYSTEM_DEFAULT,
                    onClick = {
                        scope.launch {
                            prefsManager.setThemePreference(ThemePreference.SYSTEM_DEFAULT)
                            onDismiss()
                        }
                    }
                )
                ThemeOption(
                    label = "AMOLED Pure Black",
                    selected = currentSelection == ThemePreference.AMOLED,
                    onClick = {
                        scope.launch {
                            prefsManager.setThemePreference(ThemePreference.AMOLED)
                            onDismiss()
                        }
                    }
                )
                ThemeOption(
                    label = "Material You",
                    selected = currentSelection == ThemePreference.MATERIAL_YOU,
                    onClick = {
                        scope.launch {
                            prefsManager.setThemePreference(ThemePreference.MATERIAL_YOU)
                            onDismiss()
                        }
                    }
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun ThemeOption(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(selected = selected, onClick = onClick)
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = label)
    }
}
