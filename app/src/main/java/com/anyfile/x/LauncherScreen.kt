package com.anyfile.x

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LauncherScreen(
    viewModel: OpenBridgeViewModel,
    prefsManager: PrefsManager,
    onSettingsClick: () -> Unit,
    onOpenAsClick: (Uri) -> Unit,
    onInspectClick: (Uri) -> Unit,
    onSystemFilesClick: () -> Unit
) {
    val context = LocalContext.current
    val detectionResult by viewModel.detectionResult.collectAsStateWithLifecycle()
    val isDetecting by viewModel.isDetecting.collectAsStateWithLifecycle()
    val recentFiles by RecentFileStore.getRecentFiles(context).collectAsStateWithLifecycle(initialValue = emptyList())

    var selectedUri by remember { mutableStateOf<Uri?>(null) }
    var fileName by remember { mutableStateOf("") }
    var manualMime by remember { mutableStateOf("") }
    var advancedExpanded by remember { mutableStateOf(false) }

    val filePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            try {
                context.contentResolver.takePersistableUriPermission(
                    uri, android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (e: Exception) {}
            selectedUri = uri
            val name = viewModel.queryFileName(uri) ?: uri.lastPathSegment ?: "Unknown file"
            fileName = name
            viewModel.detectMime(uri, name)
        }
    }

    LaunchedEffect(detectionResult) {
        detectionResult?.let {
            manualMime = it.mime
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("AnyFile Opener", fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = onSettingsClick) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                FilePickerArea(
                    fileName = fileName,
                    isDetecting = isDetecting,
                    onClick = { filePicker.launch(arrayOf("*/*")) }
                )
            }

            if (selectedUri != null) {
                item {
                    DetectionInfo(detectionResult)
                }

                item {
                    ActionButtons(
                        onOpenNormal = {
                            val mime = manualMime.takeIf { it.isNotBlank() } ?: detectionResult?.mime ?: "application/octet-stream"
                            selectedUri?.let {
                                viewModel.addToRecents(it, fileName, mime)
                                IntentRouter.open(context, it, mime)
                            }
                        },
                        onOpenAs = { selectedUri?.let { onOpenAsClick(it) } },
                        onInspect = { selectedUri?.let { onInspectClick(it) } },
                        onOpenFolder = { selectedUri?.let { IntentRouter.openFolder(context, it) } }
                    )
                }

                item {
                    AdvancedSection(
                        expanded = advancedExpanded,
                        onToggle = { advancedExpanded = !advancedExpanded },
                        manualMime = manualMime,
                        onMimeChange = { manualMime = it }
                    )
                }
            }

            item {
                Button(
                    onClick = onSystemFilesClick,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                ) {
                    Icon(Icons.Default.Folder, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("System File Manager")
                }
            }

            if (recentFiles.isNotEmpty()) {
                item {
                    Text(
                        "Recent Files",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }

                items(recentFiles) { recent ->
                    RecentFileItem(
                        recent = recent,
                        onOpenFolder = { IntentRouter.openFolder(context, Uri.parse(recent.uri)) }
                    ) {
                        val uri = Uri.parse(recent.uri)
                        try {
                            context.contentResolver.takePersistableUriPermission(
                                uri, android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                            )
                        } catch (e: Exception) {}
                        selectedUri = uri
                        fileName = recent.fileName
                        manualMime = recent.mimeType
                        // Proactive caching logic applied here: skip detection if we have a cached mime
                        // But we still set it in ViewModel to sync state
                        viewModel.setDetectedMimeManual(recent.mimeType)
                    }
                }
            }

            item { Spacer(Modifier.height(32.dp)) }
        }
    }
}

@Composable
fun FilePickerArea(fileName: String, isDetecting: Boolean, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = if (fileName.isEmpty()) Icons.Default.AddCircle else Icons.Default.InsertDriveFile,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = if (fileName.isEmpty()) "Tap to pick a file" else fileName,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = if (fileName.isEmpty()) FontWeight.Normal else FontWeight.Medium,
                    modifier = Modifier.padding(top = 8.dp)
                )
                if (isDetecting) {
                    LinearProgressIndicator(
                        modifier = Modifier
                            .padding(top = 8.dp)
                            .width(100.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun DetectionInfo(result: MimeDetector.DetectionResult?) {
    if (result == null) return
    
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f))
            .padding(12.dp)
    ) {
        Text(result.fileType.emoji, fontSize = 24.sp)
        Spacer(Modifier.width(12.dp))
        Column {
            Text("Detected Type", style = MaterialTheme.typography.labelMedium)
            Text(result.mime, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun ActionButtons(onOpenNormal: () -> Unit, onOpenAs: () -> Unit, onInspect: () -> Unit, onOpenFolder: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = onOpenNormal, modifier = Modifier.weight(1f)) {
                Text("Open Normally")
            }
            FilledTonalButton(onClick = onOpenFolder) {
                Icon(Icons.Default.FolderOpen, contentDescription = "Open folder")
            }
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = onOpenAs, modifier = Modifier.weight(1f)) {
                Text("Open As...")
            }
            OutlinedButton(onClick = onInspect, modifier = Modifier.weight(1f)) {
                Text("Inspect Binary")
            }
        }
    }
}

@Composable
fun AdvancedSection(expanded: Boolean, onToggle: () -> Unit, manualMime: String, onMimeChange: (String) -> Unit) {
    Column {
        TextButton(onClick = onToggle, modifier = Modifier.fillMaxWidth()) {
            Text(if (expanded) "Hide Advanced" else "Show Advanced Options")
        }
        if (expanded) {
            OutlinedTextField(
                value = manualMime,
                onValueChange = onMimeChange,
                label = { Text("Manual MIME Type") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                singleLine = true
            )
            Spacer(Modifier.height(8.dp))
            MimeChips(onChipClick = onMimeChange)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MimeChips(onChipClick: (String) -> Unit) {
    val mimes = listOf(
        "application/vnd.android.package-archive" to "APK",
        "text/plain" to "TXT",
        "application/json" to "JSON",
        "application/pdf" to "PDF",
        "image/jpeg" to "JPG",
        "video/mp4" to "MP4",
        "application/zip" to "ZIP"
    )
    
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        mimes.forEach { (mime, label) ->
            FilterChip(
                selected = false,
                onClick = { onChipClick(mime) },
                label = { Text(label) }
            )
        }
    }
}

@Composable
fun RecentFileItem(recent: RecentFile, onOpenFolder: () -> Unit, onClick: () -> Unit) {
    val sdf = remember { SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault()) }
    
    ListItem(
        headlineContent = { Text(recent.fileName, maxLines = 1) },
        supportingContent = { Text("${recent.mimeType} • ${sdf.format(Date(recent.timestamp))}") },
        leadingContent = { Icon(Icons.Default.History, contentDescription = null) },
        trailingContent = {
            IconButton(onClick = onOpenFolder) {
                Icon(Icons.Default.FolderOpen, contentDescription = "Open folder", tint = MaterialTheme.colorScheme.primary)
            }
        },
        modifier = Modifier
            .clickable(onClick = onClick)
            .clip(RoundedCornerShape(8.dp))
    )
}
