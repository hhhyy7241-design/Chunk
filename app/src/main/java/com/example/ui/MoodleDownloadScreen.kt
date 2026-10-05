package com.example.ui

import android.app.DownloadManager
import android.content.ClipDescription
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderZip
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SettingsBrightness
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material.icons.filled.VideoFile
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.AppSettings
import com.example.data.DownloadEntity
import com.example.data.ThemeMode
import com.example.model.DownloadState
import com.example.model.MoodleManifest
import com.example.parser.MoodleCodeParser
import com.example.ui.theme.TechAmberWarning
import com.example.ui.theme.TechCyanGlow
import com.example.ui.theme.TechCyanPrimary
import com.example.ui.theme.TechEmeraldSuccess
import com.example.ui.theme.TechRoseError
import com.example.util.FileUtils
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoodleDownloadScreen(
    viewModel: DownloadViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val codeText by viewModel.codeText.collectAsStateWithLifecycle()
    val parseState by viewModel.parseState.collectAsStateWithLifecycle()
    val activeDownloads by viewModel.activeDownloadsUi.collectAsStateWithLifecycle()
    val allDownloads by viewModel.allDownloads.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()

    var selectedTab by remember { mutableIntStateOf(0) }
    var showSettingsSheet by remember { mutableStateOf(false) }

    // Estado del permiso de almacenamiento
    var hasStoragePermission by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                true // En Android 10+ Scoped Storage opera nativamente en Download/Chunk
            } else {
                ContextCompat.checkSelfPermission(
                    context,
                    android.Manifest.permission.WRITE_EXTERNAL_STORAGE
                ) == PackageManager.PERMISSION_GRANTED
            }
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions.values.all { it }
        hasStoragePermission = granted || Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
        if (granted) {
            scope.launch { snackbarHostState.showSnackbar("Permiso de almacenamiento concedido.") }
        }
    }

    val notificationLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { _ -> }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
        }
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            permissionLauncher.launch(
                arrayOf(
                    android.Manifest.permission.READ_EXTERNAL_STORAGE,
                    android.Manifest.permission.WRITE_EXTERNAL_STORAGE
                )
            )
        }
    }

    LaunchedEffect(Unit) {
        viewModel.snackbarMessage.collectLatest { msg ->
            snackbarHostState.showSnackbar(msg)
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                hasStoragePermission = hasStoragePermission,
                onRequestStoragePermission = {
                    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                        permissionLauncher.launch(
                            arrayOf(
                                android.Manifest.permission.READ_EXTERNAL_STORAGE,
                                android.Manifest.permission.WRITE_EXTERNAL_STORAGE
                            )
                        )
                    } else {
                        scope.launch {
                            snackbarHostState.showSnackbar("Almacenamiento Scoped Storage activo: guardado directo en Download/Chunk.")
                        }
                    }
                },
                onOpenSettings = { showSettingsSheet = true }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Barra de Navegación Segmentada Rediseñada
            SegmentedModernNavigation(
                selectedIndex = selectedTab,
                activeCount = activeDownloads.size,
                historyCount = allDownloads.size,
                onSelect = { selectedTab = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            )

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 680.dp)
                    .align(Alignment.CenterHorizontally)
            ) {
                when (selectedTab) {
                    0 -> DownloadInputTab(
                        codeText = codeText,
                        parseState = parseState,
                        hasStoragePermission = hasStoragePermission,
                        onRequestStoragePermission = {
                            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                                permissionLauncher.launch(
                                    arrayOf(
                                        android.Manifest.permission.READ_EXTERNAL_STORAGE,
                                        android.Manifest.permission.WRITE_EXTERNAL_STORAGE
                                    )
                                )
                            } else {
                                scope.launch {
                                    snackbarHostState.showSnackbar("Permiso nativo Scoped Storage verificado.")
                                }
                            }
                        },
                        onCodeChanged = { viewModel.onCodeChanged(it) },
                        onStartDownload = {
                            viewModel.startDownload()
                            selectedTab = 1
                        }
                    )
                    1 -> ActiveTasksTab(
                        activeDownloads = activeDownloads,
                        onCancel = { viewModel.cancelDownload(it) },
                        onGoToNew = { selectedTab = 0 }
                    )
                    2 -> CompletedFilesTab(
                        downloads = allDownloads,
                        onDelete = { viewModel.deleteHistoryItem(it) },
                        onClearAll = { viewModel.clearAllHistory() }
                    )
                }
            }
        }
    }

    if (showSettingsSheet) {
        SettingsBottomSheet(
            settings = settings,
            hasStoragePermission = hasStoragePermission,
            onThemeChange = { viewModel.setThemeMode(it) },
            onDynamicColorChange = { viewModel.setDynamicColor(it) },
            onWifiOnlyChange = { viewModel.setWifiOnly(it) },
            onAutoRetryChange = { viewModel.setAutoRetry(it) },
            onVibrateChange = { viewModel.setVibrateOnComplete(it) },
            onAutoClearChange = { viewModel.setAutoClearOnStart(it) },
            onDismiss = { showSettingsSheet = false }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TopAppBar(
    hasStoragePermission: Boolean,
    onRequestStoragePermission: () -> Unit,
    onOpenSettings: () -> Unit
) {
    CenterAlignedTopAppBar(
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Emblema tecnológico moderno con degradado
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(
                            Brush.linearGradient(
                                listOf(TechCyanPrimary, MaterialTheme.colorScheme.primary)
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Download,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Column {
                    Text(
                        text = "Download Chunk",
                        fontWeight = FontWeight.Black,
                        fontSize = 19.sp,
                        letterSpacing = (-0.5).sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Gestor Modular de Descargas",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        },
        actions = {
            // Indicador de almacenamiento interactivo
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .clickable { onRequestStoragePermission() }
                    .background(
                        if (hasStoragePermission) TechEmeraldSuccess.copy(alpha = 0.15f)
                        else TechAmberWarning.copy(alpha = 0.15f)
                    )
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = if (hasStoragePermission) Icons.Default.Security else Icons.Default.Warning,
                        contentDescription = "Estado de Almacenamiento",
                        tint = if (hasStoragePermission) TechEmeraldSuccess else TechAmberWarning,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = if (hasStoragePermission) "Almacenamiento OK" else "Permiso",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (hasStoragePermission) TechEmeraldSuccess else TechAmberWarning
                    )
                }
            }

            IconButton(
                onClick = onOpenSettings,
                modifier = Modifier.testTag("settings_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Ajustes",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    )
}

@Composable
fun SegmentedModernNavigation(
    selectedIndex: Int,
    activeCount: Int,
    historyCount: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                shape = RoundedCornerShape(18.dp)
            )
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        SegmentedTabItem(
            title = "Descargar",
            icon = Icons.Default.Download,
            badge = null,
            isSelected = selectedIndex == 0,
            onClick = { onSelect(0) },
            modifier = Modifier.weight(1f)
        )
        SegmentedTabItem(
            title = "En Curso",
            icon = Icons.Default.Layers,
            badge = if (activeCount > 0) activeCount.toString() else null,
            badgeColor = TechCyanPrimary,
            isSelected = selectedIndex == 1,
            onClick = { onSelect(1) },
            modifier = Modifier.weight(1f)
        )
        SegmentedTabItem(
            title = "Archivos",
            icon = Icons.Default.CheckCircle,
            badge = if (historyCount > 0) historyCount.toString() else null,
            badgeColor = TechEmeraldSuccess,
            isSelected = selectedIndex == 2,
            onClick = { onSelect(2) },
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun SegmentedTabItem(
    title: String,
    icon: ImageVector,
    badge: String?,
    badgeColor: Color = TechCyanPrimary,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val backgroundColor by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
        animationSpec = tween(durationMillis = 200),
        label = "tab_bg"
    )
    val contentColor by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
        animationSpec = tween(durationMillis = 200),
        label = "tab_content"
    )

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(backgroundColor)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = title,
                fontSize = 13.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = contentColor
            )

            if (badge != null) {
                Spacer(modifier = Modifier.width(6.dp))
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(badgeColor)
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = badge,
                        color = Color.Black,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black
                    )
                }
            }
        }
    }
}

@Composable
fun DownloadInputTab(
    codeText: String,
    parseState: ParseUiState,
    hasStoragePermission: Boolean,
    onRequestStoragePermission: () -> Unit,
    onCodeChanged: (String) -> Unit,
    onStartDownload: () -> Unit
) {
    val context = LocalContext.current

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Explicación de almacenamiento nativo Scoped Storage
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                ),
                border = CardDefaults.outlinedCardBorder().copy(
                    brush = Brush.horizontalGradient(
                        listOf(
                            TechCyanPrimary.copy(alpha = 0.3f),
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                        )
                    )
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(TechCyanPrimary.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = null,
                            tint = TechCyanPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Almacenamiento Nativo Scoped Storage",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                                "En Android 10+, las descargas se guardan directamente en tu carpeta pública sin solicitar permisos invasivos del sistema."
                            } else {
                                "Requiere acceso de almacenamiento para escribir archivos en tu memoria."
                            },
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q && !hasStoragePermission) {
                        OutlinedButton(
                            onClick = onRequestStoragePermission,
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Permitir", fontSize = 12.sp)
                        }
                    }
                }
            }
        }

        // Tarjeta de Entrada de Código
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                border = CardDefaults.outlinedCardBorder().copy(
                    brush = Brush.verticalGradient(
                        listOf(
                            TechCyanPrimary.copy(alpha = 0.4f),
                            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f)
                        )
                    )
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Código de Descarga Moodle",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            // Botón Pegar Rápido
                            Button(
                                onClick = {
                                    val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                                    val clip = cm?.primaryClip
                                    if (clip != null && clip.itemCount > 0) {
                                        val text = clip.getItemAt(0).text?.toString() ?: ""
                                        if (text.isNotBlank()) {
                                            onCodeChanged(text)
                                        } else {
                                            Toast.makeText(context, "Portapapeles vacío", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                },
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            ) {
                                Icon(Icons.Default.ContentPaste, contentDescription = null, modifier = Modifier.size(15.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Pegar", fontSize = 12.sp)
                            }

                            if (codeText.isNotEmpty()) {
                                IconButton(
                                    onClick = { onCodeChanged("") },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(Icons.Default.Clear, contentDescription = "Borrar", tint = MaterialTheme.colorScheme.outline)
                                }
                            }
                        }
                    }

                    OutlinedTextField(
                        value = codeText,
                        onValueChange = onCodeChanged,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(130.dp)
                            .testTag("code_input_field"),
                        placeholder = {
                            Text(
                                text = "Pega aquí el enlace https://5.4.3.2.1:... generado por el bot",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.outline
                            )
                        },
                        textStyle = MaterialTheme.typography.bodyMedium.copy(
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp
                        ),
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = TechCyanPrimary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f),
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f)
                        )
                    )
                }
            }
        }

        // Vista previa en tiempo real
        item {
            AnimatedVisibility(
                visible = parseState is ParseUiState.Valid,
                enter = fadeIn() + slideInVertically()
            ) {
                if (parseState is ParseUiState.Valid) {
                    val manifest = parseState.manifest
                    ManifestLiveCard(
                        manifest = manifest,
                        onStart = onStartDownload
                    )
                }
            }

            AnimatedVisibility(
                visible = parseState is ParseUiState.Invalid && codeText.isNotBlank(),
                enter = fadeIn()
            ) {
                if (parseState is ParseUiState.Invalid) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = TechRoseError.copy(alpha = 0.12f)
                        ),
                        border = CardDefaults.outlinedCardBorder().copy(
                            brush = Brush.horizontalGradient(listOf(TechRoseError.copy(alpha = 0.5f), TechRoseError.copy(alpha = 0.2f)))
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = TechRoseError)
                            Column {
                                Text(
                                    text = "Código No Válido",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = TechRoseError
                                )
                                Text(
                                    text = parseState.message,
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ManifestLiveCard(
    manifest: MoodleManifest,
    onStart: () -> Unit
) {
    val fileIcon = getFileIconForExtension(manifest.filename)
    val isApk = manifest.filename.endsWith(".apk", ignoreCase = true)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = Brush.verticalGradient(
                listOf(
                    TechCyanPrimary.copy(alpha = 0.6f),
                    TechEmeraldSuccess.copy(alpha = 0.3f)
                )
            )
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(
                            if (isApk) TechEmeraldSuccess.copy(alpha = 0.2f)
                            else TechCyanPrimary.copy(alpha = 0.2f)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = fileIcon,
                        contentDescription = null,
                        tint = if (isApk) TechEmeraldSuccess else TechCyanPrimary,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = manifest.filename,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = FileUtils.formatBytes(manifest.size),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(text = "•", color = MaterialTheme.colorScheme.outline)
                        Text(
                            text = "${manifest.parts.size} partes",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Datos de verificación del manifiesto
            if (!manifest.sha256.isNullOrBlank()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "SHA-256: ${manifest.sha256.take(12)}...${manifest.sha256.takeLast(8)}",
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.outline
                        )
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = null,
                            tint = TechEmeraldSuccess,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }

            // Botón de Inicio con Estilo Futurista
            Button(
                onClick = onStart,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("start_download_button"),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = TechCyanPrimary,
                    contentColor = Color.Black
                )
            ) {
                Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Iniciar Descarga Modular",
                    fontWeight = FontWeight.Black,
                    fontSize = 15.sp
                )
            }
        }
    }
}

@Composable
fun ActiveTasksTab(
    activeDownloads: List<ActiveDownloadUi>,
    onCancel: (String) -> Unit,
    onGoToNew: () -> Unit
) {
    if (activeDownloads.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Layers,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.size(40.dp)
                    )
                }

                Text(
                    text = "No hay descargas activas",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Pega un código de Moodle en la pestaña Descargar para comenzar.",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.outline,
                    textAlign = TextAlign.Center
                )
                Button(
                    onClick = onGoToNew,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Nueva Descarga")
                }
            }
        }
    } else {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            items(activeDownloads, key = { it.id }) { task ->
                ActiveTaskCard(task = task, onCancel = { onCancel(task.id) })
            }
        }
    }
}

@Composable
fun ActiveTaskCard(
    task: ActiveDownloadUi,
    onCancel: () -> Unit
) {
    val fileIcon = getFileIconForExtension(task.fileName)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = Brush.horizontalGradient(
                listOf(
                    TechCyanPrimary.copy(alpha = 0.7f),
                    MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                )
            )
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(TechCyanPrimary.copy(alpha = 0.18f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = fileIcon,
                        contentDescription = null,
                        tint = TechCyanPrimary,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = task.fileName,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "Parte ${task.currentPartIndex} de ${task.totalParts} • ${task.statusMessage}",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Velocidad en tiempo real
                if (task.speedBps > 0) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(TechCyanPrimary.copy(alpha = 0.15f))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Icon(Icons.Default.Speed, contentDescription = null, tint = TechCyanPrimary, modifier = Modifier.size(12.dp))
                            Text(
                                text = "${FileUtils.formatBytes(task.speedBps)}/s",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = TechCyanPrimary
                            )
                        }
                    }
                }
            }

            // Barra de progreso y porcentaje
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                LinearProgressIndicator(
                    progress = { task.percent / 100f },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = TechCyanPrimary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "${task.percent}% • ${FileUtils.formatBytes(task.downloadedBytes)} / ${FileUtils.formatBytes(task.totalBytes)}",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.outline
                    )

                    if (task.etaSeconds > 0) {
                        Text(
                            text = "~${FileUtils.formatDuration(task.etaSeconds)} restantes",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }
            }

            // Acción de Cancelar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                OutlinedButton(
                    onClick = onCancel,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = TechRoseError
                    )
                ) {
                    Icon(Icons.Default.Clear, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Cancelar", fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
fun CompletedFilesTab(
    downloads: List<DownloadEntity>,
    onDelete: (String) -> Unit,
    onClearAll: () -> Unit
) {
    val context = LocalContext.current

    if (downloads.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Folder,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.size(56.dp)
                )
                Text(
                    text = "No hay archivos en el historial",
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Los archivos completados se guardan en la carpeta Download/Chunk de tu dispositivo.",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.outline,
                    textAlign = TextAlign.Center
                )
            }
        }
    } else {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Archivos Guardados (${downloads.size})",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    OutlinedButton(
                        onClick = onClearAll,
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.DeleteOutline, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Limpiar Todo", fontSize = 12.sp)
                    }
                }
            }

            items(downloads, key = { it.id }) { item ->
                CompletedFileCard(
                    item = item,
                    onOpen = {
                        item.mediaStoreUri?.let { uriStr ->
                            try {
                                val uri = Uri.parse(uriStr)
                                val intent = Intent(Intent.ACTION_VIEW).apply {
                                    setDataAndType(uri, context.contentResolver.getType(uri) ?: "*/*")
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                }
                                context.startActivity(intent)
                            } catch (_: Exception) {
                                Toast.makeText(context, "No hay aplicación compatible para abrir este archivo.", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    onShare = {
                        item.mediaStoreUri?.let { uriStr ->
                            try {
                                val uri = Uri.parse(uriStr)
                                val intent = Intent(Intent.ACTION_SEND).apply {
                                    type = context.contentResolver.getType(uri) ?: "*/*"
                                    putExtra(Intent.EXTRA_STREAM, uri)
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                }
                                context.startActivity(Intent.createChooser(intent, "Compartir archivo"))
                            } catch (_: Exception) {
                                Toast.makeText(context, "No se pudo compartir el archivo.", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    onDelete = { onDelete(item.id) }
                )
            }
        }
    }
}

@Composable
fun CompletedFileCard(
    item: DownloadEntity,
    onOpen: () -> Unit,
    onShare: () -> Unit,
    onDelete: () -> Unit
) {
    val fileIcon = getFileIconForExtension(item.fileName)
    val isApk = item.fileName.endsWith(".apk", ignoreCase = true)
    val isSuccess = item.status == DownloadState.COMPLETED.name
    val isWarning = item.status == "COMPLETED_WARN_HASH"
    val isFailed = item.status == DownloadState.FAILED.name

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = Brush.horizontalGradient(
                listOf(
                    if (isSuccess) TechEmeraldSuccess.copy(alpha = 0.5f)
                    else if (isWarning) TechAmberWarning.copy(alpha = 0.5f)
                    else TechRoseError.copy(alpha = 0.5f),
                    MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f)
                )
            )
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            if (isSuccess) TechEmeraldSuccess.copy(alpha = 0.15f)
                            else if (isWarning) TechAmberWarning.copy(alpha = 0.15f)
                            else TechRoseError.copy(alpha = 0.15f)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = fileIcon,
                        contentDescription = null,
                        tint = if (isSuccess) TechEmeraldSuccess else if (isWarning) TechAmberWarning else TechRoseError,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.fileName,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = FileUtils.formatBytes(item.totalBytes),
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(text = "•", color = MaterialTheme.colorScheme.outline)
                        Text(
                            text = if (isSuccess) "Completado" else if (isWarning) "Guardado (aviso de hash)" else "Fallo",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isSuccess) TechEmeraldSuccess else if (isWarning) TechAmberWarning else TechRoseError
                        )
                    }
                }

                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.DeleteOutline, contentDescription = "Eliminar", tint = MaterialTheme.colorScheme.outline)
                }
            }

            // Barra de acciones
            if (isSuccess || isWarning) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = onOpen,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isApk) TechEmeraldSuccess else MaterialTheme.colorScheme.primary,
                            contentColor = Color.White
                        )
                    ) {
                        Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (isApk) "Instalar / Abrir" else "Abrir", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = onShare,
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(15.dp))
                    }
                }
            } else if (isFailed && !item.errorMessage.isNullOrBlank()) {
                Text(
                    text = item.errorMessage,
                    fontSize = 12.sp,
                    color = TechRoseError
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsBottomSheet(
    settings: AppSettings,
    hasStoragePermission: Boolean,
    onThemeChange: (ThemeMode) -> Unit,
    onDynamicColorChange: (Boolean) -> Unit,
    onWifiOnlyChange: (Boolean) -> Unit,
    onAutoRetryChange: (Boolean) -> Unit,
    onVibrateChange: (Boolean) -> Unit,
    onAutoClearChange: (Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp),
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 22.dp, vertical = 6.dp)
                .padding(bottom = 36.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Ajustes de la Aplicación",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Black
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Clear, contentDescription = "Cerrar")
                    }
                }
            }

            // Sección 1: Tema
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Tema visual",
                        style = MaterialTheme.typography.labelLarge,
                        color = TechCyanPrimary,
                        fontWeight = FontWeight.Bold
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ThemeOptionButton(
                            title = "Sistema",
                            icon = Icons.Default.SettingsBrightness,
                            isSelected = settings.themeMode == ThemeMode.SYSTEM,
                            onClick = { onThemeChange(ThemeMode.SYSTEM) },
                            modifier = Modifier.weight(1f)
                        )
                        ThemeOptionButton(
                            title = "Claro",
                            icon = Icons.Default.LightMode,
                            isSelected = settings.themeMode == ThemeMode.LIGHT,
                            onClick = { onThemeChange(ThemeMode.LIGHT) },
                            modifier = Modifier.weight(1f)
                        )
                        ThemeOptionButton(
                            title = "Oscuro",
                            icon = Icons.Default.DarkMode,
                            isSelected = settings.themeMode == ThemeMode.DARK,
                            onClick = { onThemeChange(ThemeMode.DARK) },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        Spacer(modifier = Modifier.height(4.dp))
                        SettingToggleRow(
                            title = "Colores dinámicos Material You",
                            subtitle = "Adaptar paleta al fondo de pantalla de tu teléfono",
                            icon = Icons.Default.ColorLens,
                            checked = settings.dynamicColor,
                            onCheckedChange = onDynamicColorChange
                        )
                    }
                }
            }

            // Sección 2: Descargas y Red
            item {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                Spacer(modifier = Modifier.height(6.dp))
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text(
                        text = "Red y Descargas",
                        style = MaterialTheme.typography.labelLarge,
                        color = TechCyanPrimary,
                        fontWeight = FontWeight.Bold
                    )

                    SettingToggleRow(
                        title = "Solo con Wi-Fi",
                        subtitle = "Ahorrar datos móviles; descargar solo cuando estés en Wi-Fi",
                        icon = Icons.Default.Wifi,
                        checked = settings.wifiOnly,
                        onCheckedChange = onWifiOnlyChange
                    )

                    SettingToggleRow(
                        title = "Reanudación automática",
                        subtitle = "Reintentar si la red se corta y continuar desde la última parte",
                        icon = Icons.Default.Refresh,
                        checked = settings.autoRetry,
                        onCheckedChange = onAutoRetryChange
                    )
                }
            }

            // Sección 3: Avisos y Comportamiento
            item {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                Spacer(modifier = Modifier.height(6.dp))
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text(
                        text = "Avisos y Comportamiento",
                        style = MaterialTheme.typography.labelLarge,
                        color = TechCyanPrimary,
                        fontWeight = FontWeight.Bold
                    )

                    SettingToggleRow(
                        title = "Vibración al terminar",
                        subtitle = "Respuesta háptica cuando el archivo esté descargado",
                        icon = Icons.Default.Vibration,
                        checked = settings.vibrateOnComplete,
                        onCheckedChange = {
                            onVibrateChange(it)
                            if (it) triggerTestVibration(context)
                        }
                    )

                    SettingToggleRow(
                        title = "Limpiar campo al iniciar",
                        subtitle = "Vaciar automáticamente el código tras pulsar descargar",
                        icon = Icons.Default.Clear,
                        checked = settings.autoClearOnStart,
                        onCheckedChange = onAutoClearChange
                    )
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "Download Chunk v1.0 • Edición 2026",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }
        }
    }
}

@Composable
fun ThemeOptionButton(
    title: String,
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bg = if (isSelected) TechCyanPrimary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
    val fg = if (isSelected) Color.Black else MaterialTheme.colorScheme.onSurface

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(bg)
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = fg, modifier = Modifier.size(20.dp))
            Text(text = title, fontSize = 12.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium, color = fg)
        }
    }
}

@Composable
fun SettingToggleRow(
    title: String,
    subtitle: String,
    icon: ImageVector,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(TechCyanPrimary.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = TechCyanPrimary, modifier = Modifier.size(20.dp))
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            Text(text = subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = TechCyanPrimary,
                checkedTrackColor = TechCyanPrimary.copy(alpha = 0.3f)
            )
        )
    }
}

private fun getFileIconForExtension(filename: String): ImageVector {
    val ext = filename.substringAfterLast('.', "").lowercase()
    return when (ext) {
        "apk" -> Icons.Default.Android
        "zip", "rar", "7z", "tar", "gz" -> Icons.Default.FolderZip
        "mp4", "mkv", "avi", "mov", "webm" -> Icons.Default.VideoFile
        else -> Icons.AutoMirrored.Filled.InsertDriveFile
    }
}

private fun triggerTestVibration(context: Context) {
    try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            vm?.defaultVibrator?.vibrate(VibrationEffect.createOneShot(120, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION")
            val v = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            v?.vibrate(120)
        }
    } catch (_: Exception) {}
}
