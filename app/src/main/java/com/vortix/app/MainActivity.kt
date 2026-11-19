package com.vortix.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.vortix.app.feature.dashboard.DashboardScreen
import com.vortix.app.feature.dashboard.DashboardViewModel
import com.vortix.app.feature.controls.EnergyControlsViewModel
import com.vortix.app.feature.insights.BatteryInsightsViewModel
import com.vortix.app.ui.theme.VortixTheme
import com.vortix.app.widget.VortixBatteryWidgetProvider
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val dashboardViewModel: DashboardViewModel by viewModels()
    private val energyControlsViewModel: EnergyControlsViewModel by viewModels()
    private val batteryInsightsViewModel: BatteryInsightsViewModel by viewModels()

    override fun onResume() {
        super.onResume()
        dashboardViewModel.refresh()
        energyControlsViewModel.refreshUsageApps()
        batteryInsightsViewModel.refreshNetworkUsage()
        batteryInsightsViewModel.refreshDormantApps()
        VortixBatteryWidgetProvider.updateAll(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
        )

        setContent {
            VortixTheme {
                DashboardScreen(
                    viewModel = dashboardViewModel,
                    energyControlsViewModel = energyControlsViewModel,
                    insightsViewModel = batteryInsightsViewModel
                )
            }
        }
    }
}
