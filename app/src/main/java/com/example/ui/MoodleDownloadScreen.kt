package com.example.ui

import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
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
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ColorLens
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.AppSettings
import com.example.data.DownloadEntity
import com.example.data.ThemeMode
import com.example.model.DownloadState
import com.example.model.MoodleManifest
import com.example.ui.theme.BrandGradient
import com.example.ui.theme.BrandRadialGlowDark
import com.example.ui.theme.BrandRadialGlowLight
import com.example.ui.theme.BricolageGrotesqueFontFamily
import com.example.ui.theme.DmSansFontFamily
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.ErrorRose
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.WarningAmber
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
    val isDark = isSystemInDarkTheme()

    val codeText by viewModel.codeText.collectAsStateWithLifecycle()
    val parseState by viewModel.parseState.collectAsStateWithLifecycle()
    val activeDownloads by viewModel.activeDownloadsUi.collectAsStateWithLifecycle()
    val allDownloads by viewModel.allDownloads.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()

    var selectedTab by remember { mutableIntStateOf(0) }
    var showSettingsSheet by remember { mutableStateOf(false) }

    val activeListState = rememberLazyListState()

    val notificationLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { _ -> }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    LaunchedEffect(Unit) {
        viewModel.snackbarMessage.collectLatest { msg ->
            snackbarHostState.showSnackbar(msg)
        }
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .background(if (isDark) BrandRadialGlowDark else BrandRadialGlowLight),
        containerColor = Color.Transparent,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                onOpenCompleted = {
                    selectedTab = 1
                    scope.launch {
                        // Desplazar a la sección completados
                        val targetIndex = if (activeDownloads.isEmpty()) 0 else activeDownloads.size + 1
                        activeListState.animateScrollToItem(targetIndex)
                    }
                },
                onOpenSettings = { showSettingsSheet = true }
            )
        },
        bottomBar = {
            FloatingPillNavigationBar(
                selectedIndex = selectedTab,
                activeCount = activeDownloads.size,
                onSelect = { selectedTab = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(bottom = 16.dp, start = 24.dp, end = 24.dp)
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .widthIn(max = 640.dp),
            contentAlignment = Alignment.TopCenter
        ) {
            when (selectedTab) {
                0 -> DownloadInputTab(
                    codeText = codeText,
                    parseState = parseState,
                    onCodeChanged = { viewModel.onCodeChanged(it) },
                    onValidate = { viewModel.validateCurrentCode() },
                    onStartDownload = {
                        viewModel.startDownload()
                        selectedTab = 1
                    }
                )
                1 -> ActiveTasksTab(
                    activeDownloads = activeDownloads,
                    completedDownloads = allDownloads,
                    listState = activeListState,
                    onCancel = { viewModel.cancelDownload(it) },
                    onDeleteCompleted = { viewModel.deleteHistoryItem(it) },
                    onClearCompleted = { viewModel.clearAllHistory() },
                    onGoToDownload = { selectedTab = 0 }
                )
            }
        }
    }

    if (showSettingsSheet) {
        SettingsBottomSheet(
            settings = settings,
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
    onOpenCompleted: () -> Unit,
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
                        .clip(RoundedCornerShape(12.dp))
                        .background(BrandGradient),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Download,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Text(
                    text = "Download Chunk",
                    fontFamily = BricolageGrotesqueFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 19.sp,
                    letterSpacing = (-0.3).sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        },
        actions = {
            // Icono de carpeta que lleva a la sección "Completados"
            IconButton(
                onClick = onOpenCompleted,
                modifier = Modifier
                    .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                    .testTag("open_completed_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Folder,
                    contentDescription = "Ver archivos completados",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            IconButton(
                onClick = onOpenSettings,
                modifier = Modifier
                    .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                    .testTag("settings_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Ajustes",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
            containerColor = Color.Transparent
        )
    )
}

/**
 * Barra flotante en forma de píldora con efecto cristal y fondos sólidos invertidos.
 */
@Composable
fun FloatingPillNavigationBar(
    selectedIndex: Int,
    activeCount: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier
                .shadow(
                    elevation = 16.dp,
                    shape = RoundedCornerShape(32.dp),
                    ambientColor = ElectricBlue.copy(alpha = 0.25f),
                    spotColor = ElectricCyan.copy(alpha = 0.25f)
                ),
            shape = RoundedCornerShape(32.dp),
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.88f),
            border = androidx.compose.foundation.BorderStroke(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
            )
        ) {
            Row(
                modifier = Modifier
                    .padding(horizontal = 6.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                PillNavButton(
                    title = "Descargar",
                    icon = Icons.Default.Download,
                    badge = null,
                    isSelected = selectedIndex == 0,
                    isDark = isDark,
                    onClick = { onSelect(0) }
                )

                PillNavButton(
                    title = "En curso",
                    icon = Icons.Default.Layers,
                    badge = if (activeCount > 0) activeCount.toString() else null,
                    isSelected = selectedIndex == 1,
                    isDark = isDark,
                    onClick = { onSelect(1) }
                )
            }
        }
    }
}

@Composable
private fun PillNavButton(
    title: String,
    icon: ImageVector,
    badge: String?,
    isSelected: Boolean,
    isDark: Boolean,
    onClick: () -> Unit
) {
    // Fondo sólido invertido en la pestaña activa
    val targetBg = if (isSelected) {
        if (isDark) Color.White else Color(0xFF0F172A)
    } else {
        Color.Transparent
    }

    val targetFg = if (isSelected) {
        if (isDark) Color(0xFF0F172A) else Color.White
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    val bg by animateColorAsState(targetValue = targetBg, animationSpec = tween(200), label = "pill_bg")
    val fg by animateColorAsState(targetValue = targetFg, animationSpec = tween(200), label = "pill_fg")

    Box(
        modifier = Modifier
            .defaultMinSize(minHeight = 44.dp)
            .clip(RoundedCornerShape(26.dp))
            .background(bg)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 20.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = fg,
                modifier = Modifier.size(17.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = title,
                fontFamily = DmSansFontFamily,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                fontSize = 14.sp,
                color = fg
            )

            // El badge de "En curso" solo aparece si hay descargas activas
            if (badge != null) {
                Spacer(modifier = Modifier.width(6.dp))
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(if (isSelected) ElectricBlue else ElectricCyan)
                        .padding(horizontal = 6.dp, vertical = 1.dp)
                ) {
                    Text(
                        text = badge,
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = DmSansFontFamily
                    )
                }
            }
        }
    }
}

/**
 * Pantalla Descargar:
 * Título grande "Pega tu código. Descarga en partes.",
 * caja con el campo, botón "Pegar" dentro, y dos botones en una sola línea:
 * "Verificar" y "Descargar" (principal con degradado).
 */
@Composable
fun DownloadInputTab(
    codeText: String,
    parseState: ParseUiState,
    onCodeChanged: (String) -> Unit,
    onValidate: () -> Unit,
    onStartDownload: () -> Unit
) {
    val context = LocalContext.current
    val isValid = parseState is ParseUiState.Valid
    val isInvalid = parseState is ParseUiState.Invalid

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // Título grande de bienvenida
        item {
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Pega tu código.\nDescarga en partes.",
                fontFamily = BricolageGrotesqueFontFamily,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 32.sp,
                lineHeight = 38.sp,
                letterSpacing = (-0.8).sp,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Reconstruye y descarga archivos segmentados desde bots de Moodle a tu almacenamiento.",
                fontFamily = DmSansFontFamily,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Caja de entrada con esquinas muy redondeadas (26px) y botón Pegar dentro
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(26.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                border = CardDefaults.outlinedCardBorder().copy(
                    brush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.outlineVariant)
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
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Código Moodle",
                            fontFamily = DmSansFontFamily,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        // Botón Pegar integrado dentro de la caja
                        Button(
                            onClick = {
                                val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                                val clip = cm?.primaryClip
                                if (clip != null && clip.itemCount > 0) {
                                    val text = clip.getItemAt(0).text?.toString() ?: ""
                                    if (text.isNotBlank()) {
                                        onCodeChanged(text)
                                    } else {
                                        Toast.makeText(context, "El portapapeles está vacío", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            },
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.defaultMinSize(minHeight = 44.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                contentColor = MaterialTheme.colorScheme.onSurface
                            )
                        ) {
                            Icon(Icons.Default.ContentPaste, contentDescription = null, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Pegar",
                                fontFamily = DmSansFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
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
                                text = "https://5.4.3.2.1:...",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.outline
                            )
                        },
                        textStyle = MaterialTheme.typography.bodyMedium.copy(
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp
                        ),
                        trailingIcon = {
                            if (codeText.isNotEmpty()) {
                                IconButton(
                                    onClick = { onCodeChanged("") },
                                    modifier = Modifier.defaultMinSize(minWidth = 44.dp, minHeight = 44.dp)
                                ) {
                                    Icon(Icons.Default.Clear, contentDescription = "Borrar", tint = MaterialTheme.colorScheme.outline)
                                }
                            }
                        },
                        shape = RoundedCornerShape(18.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = ElectricCyan,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f),
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f),
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f)
                        )
                    )

                    // Dos botones en una sola línea: "Verificar" y "Descargar" (principal con degradado)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(
                            onClick = onValidate,
                            modifier = Modifier
                                .weight(1f)
                                .defaultMinSize(minHeight = 48.dp),
                            shape = RoundedCornerShape(18.dp),
                            enabled = codeText.isNotBlank()
                        ) {
                            Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Verificar",
                                fontFamily = DmSansFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }

                        // Botón principal con degradado eléctrico
                        Box(
                            modifier = Modifier
                                .weight(1.3f)
                                .defaultMinSize(minHeight = 48.dp)
                                .clip(RoundedCornerShape(18.dp))
                                .background(if (codeText.isNotBlank()) BrandGradient else Brush.linearGradient(listOf(MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.colorScheme.surfaceVariant)))
                                .clickable(
                                    enabled = codeText.isNotBlank(),
                                    onClick = onStartDownload
                                )
                                .padding(horizontal = 16.dp, vertical = 12.dp)
                                .testTag("start_download_button"),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Download,
                                    contentDescription = null,
                                    tint = if (codeText.isNotBlank()) Color.White else MaterialTheme.colorScheme.outline,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Descargar",
                                    fontFamily = DmSansFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = if (codeText.isNotBlank()) Color.White else MaterialTheme.colorScheme.outline
                                )
                            }
                        }
                    }
                }
            }
        }

        // Al verificar: Tarjeta con nombre completo (multilínea), tamaño, partes y hash pendiente (NUNCA "null...null")
        item {
            AnimatedVisibility(
                visible = isValid,
                enter = fadeIn() + slideInVertically()
            ) {
                if (parseState is ParseUiState.Valid) {
                    val manifest = parseState.manifest
                    VerifiedManifestCard(manifest = manifest)
                }
            }

            AnimatedVisibility(
                visible = isInvalid && codeText.isNotBlank(),
                enter = fadeIn()
            ) {
                if (parseState is ParseUiState.Invalid) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(22.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = ErrorRose.copy(alpha = 0.12f)
                        ),
                        border = CardDefaults.outlinedCardBorder().copy(
                            brush = androidx.compose.ui.graphics.SolidColor(ErrorRose.copy(alpha = 0.35f))
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = ErrorRose)
                            Column {
                                Text(
                                    text = "Formato de código inválido",
                                    fontFamily = BricolageGrotesqueFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = ErrorRose
                                )
                                Text(
                                    text = parseState.message,
                                    fontFamily = DmSansFontFamily,
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(72.dp))
        }
    }
}

/**
 * Tarjeta de Verificación:
 * Muestra el nombre completo del archivo (permite varias líneas),
 * tamaño, número de partes y "Hash pendiente" (nunca muestra "null...null").
 */
@Composable
fun VerifiedManifestCard(
    manifest: MoodleManifest
) {
    val fileIcon = getFileIconForExtension(manifest.filename)
    val isApk = manifest.filename.endsWith(".apk", ignoreCase = true)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(26.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = androidx.compose.ui.graphics.SolidColor(ElectricBlue.copy(alpha = 0.35f))
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(
                            if (isApk) SuccessGreen.copy(alpha = 0.15f)
                            else ElectricBlue.copy(alpha = 0.15f)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = fileIcon,
                        contentDescription = null,
                        tint = if (isApk) SuccessGreen else ElectricBlue,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    // Nombre completo del archivo (permite múltiples líneas)
                    Text(
                        text = manifest.filename,
                        fontFamily = BricolageGrotesqueFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        lineHeight = 22.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        softWrap = true,
                        maxLines = 6
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = FileUtils.formatBytes(manifest.size),
                            fontFamily = DmSansFontFamily,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = ElectricCyan
                        )
                        Text(text = "•", color = MaterialTheme.colorScheme.outline)
                        Text(
                            text = "${manifest.parts.size} partes",
                            fontFamily = DmSansFontFamily,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Hash del archivo: Muestra "Hash pendiente" si no está presente (NUNCA null...null)
            val hashText = if (!manifest.sha256.isNullOrBlank() && manifest.sha256 != "null") {
                val cleanHash = manifest.sha256.trim()
                if (cleanHash.length > 16) {
                    "${cleanHash.take(10)}...${cleanHash.takeLast(8)}"
                } else {
                    cleanHash
                }
            } else {
                "Hash pendiente"
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = null,
                            tint = if (hashText == "Hash pendiente") MaterialTheme.colorScheme.outline else SuccessGreen,
                            modifier = Modifier.size(15.dp)
                        )
                        Text(
                            text = if (hashText == "Hash pendiente") "Hash pendiente" else "SHA-256: $hashText",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp,
                            color = if (hashText == "Hash pendiente") MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(ElectricBlue.copy(alpha = 0.15f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "Verificado",
                            fontFamily = DmSansFontFamily,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = ElectricCyan
                        )
                    }
                }
            }
        }
    }
}

/**
 * Pantalla En curso:
 * - Partes como bloques en fila (uno por parte): completadas con degradado, actual con pulso suave, pendientes en gris.
 * - Porcentaje grande (40px).
 * - Velocidad en etiqueta aparte.
 * - Tiempo restante debajo.
 * - Botón "Cancelar descarga" en rojo.
 * - Estado vacío: "Nada descargándose. Pega un código para empezar."
 * - Sección "Completados" al final.
 */
@Composable
fun ActiveTasksTab(
    activeDownloads: List<ActiveDownloadUi>,
    completedDownloads: List<DownloadEntity>,
    listState: LazyListState,
    onCancel: (String) -> Unit,
    onDeleteCompleted: (String) -> Unit,
    onClearCompleted: () -> Unit,
    onGoToDownload: () -> Unit
) {
    val context = LocalContext.current
    val reducedMotion = remember { isReducedMotion(context) }

    LazyColumn(
        state = listState,
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        // Sección 1: Descargas Activas
        if (activeDownloads.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(26.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    border = CardDefaults.outlinedCardBorder().copy(
                        brush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Layers,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.outline,
                                modifier = Modifier.size(32.dp)
                            )
                        }

                        Text(
                            text = "Nada descargándose. Pega un código para empezar.",
                            fontFamily = DmSansFontFamily,
                            fontWeight = FontWeight.Medium,
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )

                        Button(
                            onClick = onGoToDownload,
                            shape = RoundedCornerShape(18.dp),
                            modifier = Modifier.defaultMinSize(minHeight = 44.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        ) {
                            Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Ir a Descargar",
                                fontFamily = DmSansFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            }
        } else {
            items(activeDownloads, key = { it.id }) { task ->
                ActiveTaskCard(
                    task = task,
                    reducedMotion = reducedMotion,
                    onCancel = { onCancel(task.id) }
                )
            }
        }

        // Sección 2: Completados (Al final de la pantalla)
        item {
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Completados",
                        fontFamily = BricolageGrotesqueFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (completedDownloads.isNotEmpty()) {
                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(SuccessGreen.copy(alpha = 0.2f))
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = completedDownloads.size.toString(),
                                fontFamily = DmSansFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = SuccessGreen
                            )
                        }
                    }
                }

                if (completedDownloads.isNotEmpty()) {
                    OutlinedButton(
                        onClick = onClearCompleted,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.defaultMinSize(minHeight = 44.dp)
                    ) {
                        Icon(Icons.Default.DeleteOutline, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Limpiar todo",
                            fontFamily = DmSansFontFamily,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }

        if (completedDownloads.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                    )
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Aún no hay descargas completadas.",
                            fontFamily = DmSansFontFamily,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }
            }
        } else {
            items(completedDownloads, key = { it.id }) { item ->
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
                    onDelete = { onDeleteCompleted(item.id) }
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(84.dp))
        }
    }
}

/**
 * Tarjeta de Tarea Activa:
 * - Partes como bloques en fila (uno por parte): completadas con degradado, actual con pulso suave, pendientes en gris.
 * - Porcentaje grande (40px).
 * - Velocidad en etiqueta aparte.
 * - Tiempo restante debajo.
 * - Botón "Cancelar descarga" en rojo.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ActiveTaskCard(
    task: ActiveDownloadUi,
    reducedMotion: Boolean,
    onCancel: () -> Unit
) {
    val fileIcon = getFileIconForExtension(task.fileName)

    // Animación de pulso suave para la parte actual (respetando prefers-reduced-motion)
    val infiniteTransition = rememberInfiniteTransition(label = "part_pulse")
    val pulseAlpha by if (reducedMotion) {
        remember { mutableStateOf(1f) }
    } else {
        infiniteTransition.animateFloat(
            initialValue = 0.5f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(800),
                repeatMode = RepeatMode.Reverse
            ),
            label = "pulse_alpha"
        )
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(26.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = androidx.compose.ui.graphics.SolidColor(ElectricBlue.copy(alpha = 0.35f))
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Cabecera de la tarea
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(BrandGradient),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = fileIcon,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = task.fileName,
                        fontFamily = BricolageGrotesqueFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${FileUtils.formatBytes(task.downloadedBytes)} de ${FileUtils.formatBytes(task.totalBytes)}",
                        fontFamily = DmSansFontFamily,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Velocidad en una etiqueta aparte
                if (task.speedBps > 0) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(ElectricCyan.copy(alpha = 0.15f))
                            .padding(horizontal = 10.dp, vertical = 5.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Icon(Icons.Default.Speed, contentDescription = null, tint = ElectricCyan, modifier = Modifier.size(13.dp))
                            Text(
                                text = "${FileUtils.formatBytes(task.speedBps)}/s",
                                fontFamily = DmSansFontFamily,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = ElectricCyan
                            )
                        }
                    }
                }
            }

            // Porcentaje grande (40px) y estado
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = "${task.percent}",
                        fontFamily = BricolageGrotesqueFontFamily,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 40.sp,
                        lineHeight = 42.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "%",
                        fontFamily = BricolageGrotesqueFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        color = ElectricCyan,
                        modifier = Modifier.padding(start = 2.dp, bottom = 4.dp)
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Parte ${task.currentPartIndex} de ${task.totalParts}",
                        fontFamily = DmSansFontFamily,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    // Tiempo restante debajo
                    if (task.etaSeconds > 0) {
                        Text(
                            text = "~${FileUtils.formatDuration(task.etaSeconds)} restantes",
                            fontFamily = DmSansFontFamily,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Las partes como bloques en fila (uno por parte)
            // Completadas con degradado, actual con pulso suave, pendientes en gris
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    maxItemsInEachRow = task.totalParts.coerceAtMost(16)
                ) {
                    for (i in 1..task.totalParts) {
                        val isDone = i < task.currentPartIndex
                        val isCurrent = i == task.currentPartIndex

                        Box(
                            modifier = Modifier
                                .weight(1f, fill = true)
                                .defaultMinSize(minWidth = 14.dp)
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .then(
                                    when {
                                        isDone -> Modifier.background(BrandGradient)
                                        isCurrent -> Modifier
                                            .background(ElectricCyan.copy(alpha = pulseAlpha))
                                            .border(1.dp, ElectricBlue, RoundedCornerShape(4.dp))
                                        else -> Modifier.background(MaterialTheme.colorScheme.surfaceVariant)
                                    }
                                )
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = task.statusMessage,
                        fontFamily = DmSansFontFamily,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Botón "Cancelar descarga" en rojo suave
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                OutlinedButton(
                    onClick = onCancel,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.defaultMinSize(minHeight = 44.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = ErrorRose
                    ),
                    border = androidx.compose.foundation.BorderStroke(1.dp, ErrorRose.copy(alpha = 0.5f))
                ) {
                    Icon(Icons.Default.Clear, contentDescription = null, modifier = Modifier.size(15.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Cancelar descarga",
                        fontFamily = DmSansFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }
            }
        }
    }
}

/**
 * Tarjeta de Archivo Completado:
 * Muestra botón "Instalar / Abrir" destacado, compartir y eliminar.
 */
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
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = androidx.compose.ui.graphics.SolidColor(
                if (isSuccess) SuccessGreen.copy(alpha = 0.35f)
                else if (isWarning) WarningAmber.copy(alpha = 0.35f)
                else ErrorRose.copy(alpha = 0.35f)
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
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            if (isSuccess) SuccessGreen.copy(alpha = 0.15f)
                            else if (isWarning) WarningAmber.copy(alpha = 0.15f)
                            else ErrorRose.copy(alpha = 0.15f)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = fileIcon,
                        contentDescription = null,
                        tint = if (isSuccess) SuccessGreen else if (isWarning) WarningAmber else ErrorRose,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.fileName,
                        fontFamily = BricolageGrotesqueFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = FileUtils.formatBytes(item.totalBytes),
                            fontFamily = DmSansFontFamily,
                            fontSize = 12.sp,
                            color = ElectricCyan,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(text = "•", color = MaterialTheme.colorScheme.outline)
                        Text(
                            text = if (isSuccess) "Completado" else if (isWarning) "Guardado (aviso hash)" else "Fallo",
                            fontFamily = DmSansFontFamily,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isSuccess) SuccessGreen else if (isWarning) WarningAmber else ErrorRose
                        )
                    }
                }

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.defaultMinSize(minWidth = 44.dp, minHeight = 44.dp)
                ) {
                    Icon(Icons.Default.DeleteOutline, contentDescription = "Eliminar", tint = MaterialTheme.colorScheme.outline)
                }
            }

            // Botón Instalar / Abrir
            if (isSuccess || isWarning) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = onOpen,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .weight(1f)
                            .defaultMinSize(minHeight = 44.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isApk) SuccessGreen else ElectricBlue,
                            contentColor = Color.White
                        )
                    ) {
                        Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isApk) "Instalar / Abrir" else "Abrir archivo",
                            fontFamily = DmSansFontFamily,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    OutlinedButton(
                        onClick = onShare,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.defaultMinSize(minWidth = 44.dp, minHeight = 44.dp)
                    ) {
                        Icon(Icons.Default.Share, contentDescription = "Compartir", modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsBottomSheet(
    settings: AppSettings,
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
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
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
                        text = "Ajustes",
                        fontFamily = BricolageGrotesqueFontFamily,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.defaultMinSize(minWidth = 44.dp, minHeight = 44.dp)
                    ) {
                        Icon(Icons.Default.Clear, contentDescription = "Cerrar")
                    }
                }
            }

            // Sección 1: Tema
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Tema visual",
                        fontFamily = DmSansFontFamily,
                        style = MaterialTheme.typography.labelLarge,
                        color = ElectricCyan,
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
                            subtitle = "Adaptar paleta al fondo de pantalla",
                            icon = Icons.Default.ColorLens,
                            checked = settings.dynamicColor,
                            onCheckedChange = onDynamicColorChange
                        )
                    }
                }
            }

            // Sección 2: Red y Descarga
            item {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                Spacer(modifier = Modifier.height(6.dp))
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text(
                        text = "Red y Descargas",
                        fontFamily = DmSansFontFamily,
                        style = MaterialTheme.typography.labelLarge,
                        color = ElectricCyan,
                        fontWeight = FontWeight.Bold
                    )

                    SettingToggleRow(
                        title = "Solo con Wi-Fi",
                        subtitle = "Evitar uso de datos móviles en descargas pesadas",
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

            // Sección 3: Avisos
            item {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                Spacer(modifier = Modifier.height(6.dp))
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text(
                        text = "Avisos y Háptica",
                        fontFamily = DmSansFontFamily,
                        style = MaterialTheme.typography.labelLarge,
                        color = ElectricCyan,
                        fontWeight = FontWeight.Bold
                    )

                    SettingToggleRow(
                        title = "Vibración al terminar",
                        subtitle = "Respuesta háptica cuando el archivo esté listo",
                        icon = Icons.Default.Vibration,
                        checked = settings.vibrateOnComplete,
                        onCheckedChange = {
                            onVibrateChange(it)
                            if (it) triggerTestVibration(context)
                        }
                    )

                    SettingToggleRow(
                        title = "Limpiar campo al iniciar",
                        subtitle = "Vaciar automáticamente el código al pulsar descargar",
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
                        text = "Download Chunk • 2026 Pro Edition",
                        fontFamily = DmSansFontFamily,
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
    val bg = if (isSelected) ElectricBlue else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    val fg = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface

    Box(
        modifier = modifier
            .defaultMinSize(minHeight = 44.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(bg)
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = fg, modifier = Modifier.size(18.dp))
            Text(
                text = title,
                fontFamily = DmSansFontFamily,
                fontSize = 12.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = fg
            )
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
        modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 44.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(ElectricBlue.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = ElectricCyan, modifier = Modifier.size(18.dp))
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, fontFamily = DmSansFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            Text(text = subtitle, fontFamily = DmSansFontFamily, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = ElectricCyan,
                checkedTrackColor = ElectricBlue.copy(alpha = 0.4f)
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

private fun isReducedMotion(context: Context): Boolean {
    return try {
        val scale = Settings.Global.getFloat(
            context.contentResolver,
            Settings.Global.TRANSITION_ANIMATION_SCALE,
            1f
        )
        scale == 0f
    } catch (_: Exception) {
        false
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
