package com.vortix.app.feature.dashboard

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
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
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.animation.scaleOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.fadeIn
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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

// Spatial frosted glass palette inspired by translucent glass windows & floating menus
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

    val isBadBattery = remember(snapshot) {
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
        delay(1550L)
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
                onSelectTab = { selectedTab = it },
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
            // Soft spatial ambient light orbs for frosted glass depth
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

                // Translucent floating action popover (inspired by the floating context menu)
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
