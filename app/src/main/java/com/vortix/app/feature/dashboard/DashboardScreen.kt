package com.vortix.app.feature.dashboard

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vortix.app.domain.model.BatteryHealth
import com.vortix.app.domain.model.BatterySnapshot
import com.vortix.app.domain.model.BatteryStatus
import com.vortix.app.domain.model.PluggedSource
import com.vortix.app.feature.controls.EnergyControlsPanel
import com.vortix.app.feature.controls.EnergyControlsViewModel
import com.vortix.app.feature.insights.BatteryInsightsViewModel
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt

private val SpatialGlassPanel = Color(0x9E262B33)
private val SpatialGlassSubPanel = Color(0x88343A46)
private val SpatialGlassPopMenu = Color(0xC42C323C)
private val SpatialRimLight = Color(0x73FFFFFF)
private val SpatialRimSubtle = Color(0x38FFFFFF)
private val SpatialSelectedPill = Color(0x66788296)
private val SpatialAccentMint = Color(0xFF86EFAC)
private val SpatialAccentSky = Color(0xFF93C5FD)
private val SpatialAccentWarm = Color(0xFFFDE68A)

@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel,
    energyControlsViewModel: EnergyControlsViewModel,
    insightsViewModel: BatteryInsightsViewModel
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val controlsState by energyControlsViewModel.uiState.collectAsStateWithLifecycle()
    val snapshot = uiState.snapshot
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }

    val isBadBattery = remember(snapshot, controlsState.thermalThresholdCelsius, controlsState.lowBatteryThresholdPercent) {
        val temp = snapshot?.temperatureCelsius ?: 0.0
        val lvl = snapshot?.levelPercent
        snapshot?.health == BatteryHealth.OVERHEAT ||
            temp >= controlsState.thermalThresholdCelsius.toDouble() ||
            (snapshot?.status == BatteryStatus.DISCHARGING && lvl != null && lvl <= controlsState.lowBatteryThresholdPercent)
    }
    val isChargingNow = snapshot?.status == BatteryStatus.CHARGING || snapshot?.status == BatteryStatus.FULL

    var danceVisible by remember { mutableStateOf(false) }
    var danceModePositive by remember { mutableStateOf(true) }
    var danceTriggerId by remember { mutableLongStateOf(0L) }
    var hasPlayedLaunchDance by rememberSaveable { mutableStateOf(false) }
    var wasChargingBefore by remember { mutableStateOf<Boolean?>(null) }

    LaunchedEffect(snapshot?.status, isBadBattery) {
        if (snapshot == null) return@LaunchedEffect
        if (!hasPlayedLaunchDance) {
            hasPlayedLaunchDance = true
            wasChargingBefore = isChargingNow
            if (isChargingNow && !isBadBattery) {
                danceModePositive = true
                danceVisible = true
                danceTriggerId = System.currentTimeMillis()
            } else if (isBadBattery) {
                danceModePositive = false
                danceVisible = true
                danceTriggerId = System.currentTimeMillis()
            }
            return@LaunchedEffect
        }
        val startedCharging = wasChargingBefore == false && isChargingNow
        wasChargingBefore = isChargingNow
        if (startedCharging && !isBadBattery) {
            danceModePositive = true
            danceVisible = true
            danceTriggerId = System.currentTimeMillis()
        } else if (isBadBattery && danceModePositive) {
            danceModePositive = false
            danceVisible = true
            danceTriggerId = System.currentTimeMillis()
        }
    }

    LaunchedEffect(danceTriggerId) {
        if (danceTriggerId == 0L) return@LaunchedEffect
        kotlinx.coroutines.delay(1550L)
        danceVisible = false
    }

    var quickMenuExpanded by rememberSaveable { mutableStateOf(false) }

    val ambientBackdrop = Brush.verticalGradient(
        colors = listOf(
            Color(0xFF1A1D24),
            Color(0xFF13161C),
            Color(0xFF0D0F14)
        )
    )

    Scaffold(
        containerColor = Color(0xFF0D0F14),
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            SpatialFloatingMediaBar(
                snapshot = snapshot,
                selectedTab = selectedTab,
                onSelectTab = { tabIndex -> selectedTab = tabIndex },
                onRefresh = viewModel::refresh,
                onOpenMenu = { quickMenuExpanded = !quickMenuExpanded }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(ambientBackdrop)
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0x2893C5FD), Color.Transparent),
                        center = Offset(size.width * 0.82f, size.height * 0.18f),
                        radius = size.minDimension * 0.65f
                    ),
                    radius = size.minDimension * 0.65f,
                    center = Offset(size.width * 0.82f, size.height * 0.18f)
                )
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0x22D6BCFA), Color.Transparent),
                        center = Offset(size.width * 0.18f, size.height * 0.58f),
                        radius = size.minDimension * 0.70f
                    ),
                    radius = size.minDimension * 0.70f,
                    center = Offset(size.width * 0.18f, size.height * 0.58f)
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .padding(top = innerPadding.calculateTopPadding())
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .padding(bottom = 124.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                SpatialTopHeader(
                    onRefresh = viewModel::refresh,
                    isRefreshing = uiState.isRefreshing,
                    chargeLimitEnabled = controlsState.isChargeLimitEnabled,
                    chargeTarget = controlsState.chargeTargetPercent,
                    menuExpanded = quickMenuExpanded,
                    onToggleMenu = { quickMenuExpanded = !quickMenuExpanded }
                )

                if (quickMenuExpanded) {
                    SpatialActionPopover(
                        chargeLimitEnabled = controlsState.isChargeLimitEnabled,
                        chargeTarget = controlsState.chargeTargetPercent,
                        onSetPreset80 = {
                            energyControlsViewModel.setChargeTarget(80)
                            quickMenuExpanded = false
                        },
                        onSetPreset90 = {
                            energyControlsViewModel.setChargeTarget(90)
                            quickMenuExpanded = false
                        },
                        onOpenFullControls = {
                            selectedTab = 1
                            quickMenuExpanded = false
                        },
                        onClearHistory = {
                            viewModel.clearHistory()
                            quickMenuExpanded = false
                        }
                    )
                }

                if (selectedTab == 0) {
                    when {
                        snapshot != null -> {
                            SpatialSplitWorkspace(
                                snapshot = snapshot,
                                chargeLimitEnabled = controlsState.isChargeLimitEnabled,
                                chargeTarget = controlsState.chargeTargetPercent,
                                thermalAlertEnabled = controlsState.isThermalAlertEnabled,
                                thermalThreshold = controlsState.thermalThresholdCelsius,
                                onToggleChargeLimit = energyControlsViewModel::setChargeLimitEnabled,
                                onToggleThermalAlert = energyControlsViewModel::setThermalAlertEnabled,
                                onOpenControls = { selectedTab = 1 }
                            )
                            ChargingDanceBurstCard(
                                visible = danceVisible,
                                isChargingDance = danceModePositive,
                                snapshot = snapshot,
                                onReplay = {
                                    danceModePositive = !isBadBattery
                                    danceVisible = true
                                    danceTriggerId = System.currentTimeMillis()
                                }
                            )
                            BatteryHeroCard(
                                snapshot = snapshot,
                                chargeLimitEnabled = controlsState.isChargeLimitEnabled,
                                chargeTarget = controlsState.chargeTargetPercent,
                                onOpenControls = { selectedTab = 1 }
                            )
                            BatteryHealthDiagnosticsCard(
                                snapshot = snapshot,
                                designCapacityMah = uiState.designCapacityMah,
                                onCalibrateClick = { selectedTab = 1 }
                            )
                            VortexApexIntelligenceCard(
                                snapshot = snapshot,
                                history = uiState.recentSnapshots,
                                designCapacityMah = uiState.designCapacityMah,
                                chargeTarget = controlsState.chargeTargetPercent
                            )
                            Text(
                                text = "CANLI TELEMETRİ & SENSÖRLER",
                                style = MaterialTheme.typography.labelLarge,
                                color = SpatialAccentSky,
                                fontWeight = FontWeight.Bold
                            )
                            MetricGrid(snapshot)
                            if (uiState.recentSnapshots.size >= 2) {
                                BatteryHistoryChartCard(
                                    snapshots = uiState.recentSnapshots,
                                    onClearHistory = viewModel::clearHistory
                                )
                            }
                        }

                        uiState.isRefreshing -> {
                            Surface(
                                color = SpatialGlassPanel,
                                shape = RoundedCornerShape(24.dp),
                                border = BorderStroke(1.dp, SpatialRimLight),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(20.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                                ) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(28.dp),
                                        color = SpatialAccentSky
                                    )
                                    Text(
                                        text = "Pil sensörleri okunuyor…",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = Color.White
                                    )
                                }
                            }
                        }

                        else -> {
                            Surface(
                                color = SpatialGlassPanel,
                                shape = RoundedCornerShape(24.dp),
                                border = BorderStroke(1.dp, SpatialRimLight),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(20.dp),
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Text(
                                        text = uiState.errorMessage ?: "Pil verisi henüz alınamadı.",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = Color.White
                                    )
                                    Button(
                                        onClick = viewModel::refresh,
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = SpatialAccentSky,
                                            contentColor = Color(0xFF0D0F14)
                                        )
                                    ) {
                                        Text("Yenile")
                                    }
                                }
                            }
                        }
                    }
                } else {
                    EnergyControlsPanel(viewModel = energyControlsViewModel)
                }
            }
        }
    }
}

@Composable
private fun SpatialTopHeader(
    onRefresh: () -> Unit,
    isRefreshing: Boolean,
    chargeLimitEnabled: Boolean,
    chargeTarget: Int,
    menuExpanded: Boolean,
    onToggleMenu: () -> Unit
) {
    Surface(
        color = SpatialGlassPanel,
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, SpatialRimLight),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "VORTIX 7.0",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White
                )
                Text(
                    text = if (chargeLimitEnabled) "Hedef Koruma: %$chargeTarget" else "Enerji Telemetri Paneli",
                    style = MaterialTheme.typography.labelMedium,
                    color = if (chargeLimitEnabled) SpatialAccentMint else SpatialAccentSky,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = onRefresh,
                    enabled = !isRefreshing,
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    border = BorderStroke(1.dp, SpatialRimSubtle),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text(
                        text = if (isRefreshing) "…" else "Yenile",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White
                    )
                }
                Surface(
                    color = if (menuExpanded) SpatialSelectedPill else SpatialGlassSubPanel,
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, SpatialRimSubtle),
                    modifier = Modifier.clickable(onClick = onToggleMenu)
                ) {
                    Text(
                        text = "Menü",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun SpatialActionPopover(
    chargeLimitEnabled: Boolean,
    chargeTarget: Int,
    onSetPreset80: () -> Unit,
    onSetPreset90: () -> Unit,
    onOpenFullControls: () -> Unit,
    onClearHistory: () -> Unit
) {
    Surface(
        color = SpatialGlassPopMenu,
        shape = RoundedCornerShape(22.dp),
        border = BorderStroke(1.dp, SpatialRimLight),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "Hızlı Eylemler (Aktif: %$chargeTarget • ${if (chargeLimitEnabled) "Açık" else "Kapalı"})",
                style = MaterialTheme.typography.labelLarge,
                color = SpatialAccentWarm,
                fontWeight = FontWeight.Bold
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = onSetPreset80,
                    modifier = Modifier.weight(1f),
                    border = BorderStroke(1.dp, SpatialRimSubtle)
                ) {
                    Text("Hedef %80", color = Color.White)
                }
                OutlinedButton(
                    onClick = onSetPreset90,
                    modifier = Modifier.weight(1f),
                    border = BorderStroke(1.dp, SpatialRimSubtle)
                ) {
                    Text("Hedef %90", color = Color.White)
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = onOpenFullControls,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SpatialAccentSky,
                        contentColor = Color(0xFF0D0F14)
                    )
                ) {
                    Text("Kontroller")
                }
                OutlinedButton(
                    onClick = onClearHistory,
                    modifier = Modifier.weight(1f),
                    border = BorderStroke(1.dp, SpatialRimSubtle)
                ) {
                    Text("Geçmişi Sil", color = Color.White)
                }
            }
        }
    }
}

@Composable
private fun SpatialSplitWorkspace(
    snapshot: BatterySnapshot,
    chargeLimitEnabled: Boolean,
    chargeTarget: Int,
    thermalAlertEnabled: Boolean,
    thermalThreshold: Int,
    onToggleChargeLimit: (Boolean) -> Unit,
    onToggleThermalAlert: (Boolean) -> Unit,
    onOpenControls: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Surface(
            color = SpatialGlassPanel,
            shape = RoundedCornerShape(22.dp),
            border = BorderStroke(1.dp, SpatialRimSubtle),
            modifier = Modifier.weight(1f)
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Şarj Hedefi",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Switch(
                        checked = chargeLimitEnabled,
                        onCheckedChange = onToggleChargeLimit,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color(0xFF0D0F14),
                            checkedTrackColor = SpatialAccentMint
                        )
                    )
                }
                Text(
                    text = "Sınır: %$chargeTarget • Anlık: ${snapshot.levelPercent?.let { "%$it" } ?: "—"}",
                    style = MaterialTheme.typography.bodySmall,
                    color = SpatialAccentMint
                )
            }
        }

        Surface(
            color = SpatialGlassPanel,
            shape = RoundedCornerShape(22.dp),
            border = BorderStroke(1.dp, SpatialRimSubtle),
            modifier = Modifier
                .weight(1f)
                .clickable(onClick = onOpenControls)
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Termal Sınır",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Switch(
                        checked = thermalAlertEnabled,
                        onCheckedChange = onToggleThermalAlert,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color(0xFF0D0F14),
                            checkedTrackColor = SpatialAccentWarm
                        )
                    )
                }
                Text(
                    text = "Eşik: $thermalThreshold °C • Anlık: ${snapshot.temperatureCelsius?.let { String.format(Locale.US, "%.1f °C", it) } ?: "—"}",
                    style = MaterialTheme.typography.bodySmall,
                    color = SpatialAccentWarm
                )
            }
        }
    }
}

@Composable
private fun ChargingDanceBurstCard(
    visible: Boolean,
    isChargingDance: Boolean,
    snapshot: BatterySnapshot,
    onReplay: () -> Unit
) {
    val transition = rememberInfiniteTransition(label = "dance_transition")
    val pulseScale by transition.animateFloat(
        initialValue = 0.96f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 520, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(220)) + scaleIn(tween(260), initialScale = 0.92f),
        exit = fadeOut(tween(220)) + scaleOut(tween(220), targetScale = 0.94f)
    ) {
        Surface(
            color = SpatialGlassSubPanel,
            shape = RoundedCornerShape(22.dp),
            border = BorderStroke(1.dp, if (isChargingDance) SpatialAccentMint else SpatialAccentWarm),
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onReplay)
        ) {
            Row(
                modifier = Modifier.padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(44.dp)
                        .graphicsLayer {
                            scaleX = pulseScale
                            scaleY = pulseScale
                        }
                        .clip(CircleShape)
                        .background(if (isChargingDance) SpatialAccentMint.copy(alpha = 0.22f) else SpatialAccentWarm.copy(alpha = 0.22f))
                ) {
                    Text(
                        text = if (isChargingDance) "⚡" else "⚠",
                        style = MaterialTheme.typography.titleMedium
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (isChargingDance) "Şarj Akışı Aktif" else "Pil Uyarı Durumu",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "Seviye: ${snapshot.levelPercent?.let { "%$it" } ?: "—"} • Durum: ${formatStatus(snapshot.status)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                }
            }
        }
    }
}

@Composable
private fun BatteryHeroCard(
    snapshot: BatterySnapshot,
    chargeLimitEnabled: Boolean,
    chargeTarget: Int,
    onOpenControls: () -> Unit
) {
    val level = snapshot.levelPercent?.coerceIn(0, 100)
    val animatedProgress by animateFloatAsState(
        targetValue = (level ?: 0) / 100f,
        animationSpec = tween(durationMillis = 550),
        label = "battery_progress"
    )

    Surface(
        color = SpatialGlassPanel,
        shape = RoundedCornerShape(26.dp),
        border = BorderStroke(1.dp, SpatialRimLight),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = level?.let { "%$it" } ?: "—",
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White
                )
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = formatStatus(snapshot.status),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = SpatialAccentMint
                    )
                    Text(
                        text = "Kaynak: ${formatPlugged(snapshot.pluggedSource)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.78f)
                    )
                }
            }

            LinearProgressIndicator(
                progress = { animatedProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp)
                    .clip(RoundedCornerShape(99.dp)),
                color = if ((level ?: 100) <= 20) SpatialAccentWarm else SpatialAccentMint,
                trackColor = Color.White.copy(alpha = 0.14f)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (chargeLimitEnabled) "Şarj hedefi: %$chargeTarget" else "Şarj hedefi uyarısı kapalı",
                    style = MaterialTheme.typography.bodySmall,
                    color = SpatialAccentSky
                )
                OutlinedButton(
                    onClick = onOpenControls,
                    border = BorderStroke(1.dp, SpatialRimSubtle),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Ayarlar", color = Color.White, style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    }
}

@Composable
private fun BatteryHealthDiagnosticsCard(
    snapshot: BatterySnapshot,
    designCapacityMah: Int,
    onCalibrateClick: () -> Unit
) {
    val remainingMah = snapshot.chargeCounterMicroAh?.let { (it / 1_000.0).roundToInt() }
    val estimatedHealthPercent = if (remainingMah != null && designCapacityMah > 0) {
        ((remainingMah.toDouble() / designCapacityMah.toDouble()) * 100.0)
            .coerceIn(0.0, 120.0)
            .roundToInt()
    } else {
        null
    }

    Surface(
        color = SpatialGlassPanel,
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, SpatialRimSubtle),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Pil Sağlığı & Kapasite",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                OutlinedButton(
                    onClick = onCalibrateClick,
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    border = BorderStroke(1.dp, SpatialRimSubtle)
                ) {
                    Text("Kalibre Et", color = SpatialAccentSky, style = MaterialTheme.typography.labelSmall)
                }
            }
            Text(
                text = "Sağlık Durumu: ${formatHealth(snapshot.health)} • Döngü: ${snapshot.cycleCount ?: "—"}",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.88f)
            )
            Text(
                text = "Kalan Şarj: ${remainingMah?.let { "$it mAh" } ?: "—"} • Tasarım: ${if (designCapacityMah > 0) "$designCapacityMah mAh" else "—"}${estimatedHealthPercent?.let { " • Tahmini Kapasite: %$it" } ?: ""}",
                style = MaterialTheme.typography.bodySmall,
                color = SpatialAccentMint
            )
        }
    }
}

@Composable
private fun VortexApexIntelligenceCard(
    snapshot: BatterySnapshot,
    history: List<BatterySnapshot>,
    designCapacityMah: Int,
    chargeTarget: Int
) {
    val powerMw = snapshot.batteryPowerMilliWatts
    val avgTemp = history.mapNotNull { it.temperatureCelsius }.takeIf { it.isNotEmpty() }?.average()

    Surface(
        color = SpatialGlassPanel,
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, SpatialRimSubtle),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Vortex Enerji Özeti",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = SpatialAccentSky
            )
            Text(
                text = "Anlık Güç: ${powerMw?.let { String.format(Locale.US, "%.0f mW", it) } ?: "—"} • Hedef: %$chargeTarget • Kapasite: ${if (designCapacityMah > 0) "$designCapacityMah mAh" else "—"}",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White
            )
            Text(
                text = "Kayıtlı Örnek: ${history.size}${avgTemp?.let { String.format(Locale.US, " • Ort. Sıcaklık: %.1f °C", it) } ?: ""}",
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.78f)
            )
        }
    }
}

@Composable
private fun MetricGrid(snapshot: BatterySnapshot) {
    val currentMa = snapshot.currentNowMicroAmps?.let { "${it / 1000L} mA" } ?: "—"
    val voltageStr = snapshot.voltageMillivolts?.let { "$it mV" } ?: "—"
    val tempStr = snapshot.temperatureCelsius?.let { String.format(Locale.US, "%.1f °C", it) } ?: "—"
    val etaStr = snapshot.dischargePredictionMillis
        ?.takeIf { it > 0L }
        ?.let { ms ->
            val totalMinutes = ms / 60_000L
            "${totalMinutes / 60L} sa ${totalMinutes % 60L} dk"
        } ?: "—"

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            TelemetryTile(
                title = "Akım",
                value = currentMa,
                accent = SpatialAccentMint,
                modifier = Modifier.weight(1f)
            )
            TelemetryTile(
                title = "Voltaj",
                value = voltageStr,
                accent = SpatialAccentSky,
                modifier = Modifier.weight(1f)
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            TelemetryTile(
                title = "Sıcaklık",
                value = tempStr,
                accent = SpatialAccentWarm,
                modifier = Modifier.weight(1f)
            )
            TelemetryTile(
                title = "Tahmini Süre",
                value = etaStr,
                accent = Color.White,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun TelemetryTile(
    title: String,
    value: String,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        color = SpatialGlassSubPanel,
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, SpatialRimSubtle),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                color = Color.White.copy(alpha = 0.72f)
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = accent,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun BatteryHistoryChartCard(
    snapshots: List<BatterySnapshot>,
    onClearHistory: () -> Unit
) {
    val points = snapshots.mapNotNull { it.levelPercent?.coerceIn(0, 100) }
    val formatter = remember {
        DateTimeFormatter.ofPattern("HH:mm", Locale.getDefault()).withZone(ZoneId.systemDefault())
    }
    val lastTimeLabel = snapshots.lastOrNull()?.capturedAtEpochMillis?.let {
        formatter.format(Instant.ofEpochMilli(it))
    } ?: ""

    Surface(
        color = SpatialGlassPanel,
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, SpatialRimSubtle),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Son Ölçüm Geçmişi (${points.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    if (lastTimeLabel.isNotEmpty()) {
                        Text(
                            text = "Son kayıt: $lastTimeLabel",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.7f)
                        )
                    }
                }
                OutlinedButton(
                    onClick = onClearHistory,
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    border = BorderStroke(1.dp, SpatialRimSubtle)
                ) {
                    Text("Temizle", color = SpatialAccentWarm, style = MaterialTheme.typography.labelSmall)
                }
            }

            if (points.size >= 2) {
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(110.dp)
                ) {
                    val path = Path()
                    val stepX = size.width / (points.size - 1).coerceAtLeast(1)
                    points.forEachIndexed { index, level ->
                        val x = index * stepX
                        val y = size.height - ((level / 100f) * size.height)
                        if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
                    }
                    drawPath(
                        path = path,
                        color = SpatialAccentMint,
                        style = Stroke(width = 4f, cap = StrokeCap.Round)
                    )
                }
            }
        }
    }
}

@Composable
private fun SpatialFloatingMediaBar(
    snapshot: BatterySnapshot?,
    selectedTab: Int,
    onSelectTab: (Int) -> Unit,
    onRefresh: () -> Unit,
    onOpenMenu: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            color = SpatialGlassPopMenu,
            shape = RoundedCornerShape(28.dp),
            border = BorderStroke(1.dp, SpatialRimLight),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Surface(
                    color = if (selectedTab == 0) SpatialSelectedPill else Color.Transparent,
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.clickable { onSelectTab(0) }
                ) {
                    Text(
                        text = snapshot?.levelPercent?.let { "Batarya %$it" } ?: "Batarya",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                    )
                }

                Surface(
                    color = if (selectedTab == 1) SpatialSelectedPill else Color.Transparent,
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.clickable { onSelectTab(1) }
                ) {
                    Text(
                        text = "Kontrol",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Surface(
                        color = SpatialGlassSubPanel,
                        shape = CircleShape,
                        modifier = Modifier.clickable(onClick = onRefresh)
                    ) {
                        Text(
                            text = "↻",
                            style = MaterialTheme.typography.titleMedium,
                            color = SpatialAccentSky,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                    Surface(
                        color = SpatialGlassSubPanel,
                        shape = CircleShape,
                        modifier = Modifier.clickable(onClick = onOpenMenu)
                    ) {
                        Text(
                            text = "⋯",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }
            }
        }
    }
}

private fun formatStatus(status: BatteryStatus): String = when (status) {
    BatteryStatus.CHARGING -> "Şarj Oluyor"
    BatteryStatus.DISCHARGING -> "Deşarj Oluyor"
    BatteryStatus.NOT_CHARGING -> "Şarj Olmuyor"
    BatteryStatus.FULL -> "Tam Dolu"
    BatteryStatus.UNKNOWN -> "Bilinmiyor"
}

private fun formatPlugged(plugged: PluggedSource): String = when (plugged) {
    PluggedSource.AC -> "Adaptör (AC)"
    PluggedSource.USB -> "USB"
    PluggedSource.WIRELESS -> "Kablosuz"
    PluggedSource.DOCK -> "Dock"
    PluggedSource.OTHER -> "Diğer"
    PluggedSource.NONE -> "Pil"
    PluggedSource.UNKNOWN -> "Bilinmiyor"
}

private fun formatHealth(health: BatteryHealth?): String = when (health) {
    BatteryHealth.GOOD -> "İyi"
    BatteryHealth.OVERHEAT -> "Aşırı Isınma"
    BatteryHealth.DEAD -> "Zayıf / Ölü"
    BatteryHealth.OVER_VOLTAGE -> "Yüksek Voltaj"
    BatteryHealth.UNSPECIFIED_FAILURE -> "Arıza"
    BatteryHealth.COLD -> "Soğuk"
    BatteryHealth.UNKNOWN, null -> "Bilinmiyor"
}
