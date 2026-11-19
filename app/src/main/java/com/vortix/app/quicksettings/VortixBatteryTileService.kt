package com.vortix.app.quicksettings

import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import com.vortix.app.MainActivity

class VortixBatteryTileService : TileService() {
    override fun onStartListening() {
        super.onStartListening()
        val intent = if (Build.VERSION.SDK_INT >= 33) {
            registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED), RECEIVER_NOT_EXPORTED)
        } else {
            @Suppress("DEPRECATION")
            registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        }
        val level = intent?.let {
            val value = it.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
            val scale = it.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
            if (value >= 0 && scale > 0) (value * 100f / scale).toInt() else null
        }
        qsTile?.apply {
            label = level?.let { "Vortix · $it%" } ?: "Vortix Battery"
            state = Tile.STATE_ACTIVE
            updateTile()
        }
    }

    override fun onClick() {
        super.onClick()
        val launch = Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivityAndCollapse(launch)
    }
}
