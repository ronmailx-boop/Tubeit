package com.tubeit.cliphistory

import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService

/**
 * Quick Settings tile: tapping it captures the current clipboard without
 * opening the full app UI, so it's fast enough to use after every copy.
 */
class QuickCaptureTileService : TileService() {

    // This is a momentary action, not an on/off toggle, but a tile's state
    // defaults to STATE_UNAVAILABLE until set -- and onClick() is only
    // dispatched for tiles that aren't STATE_UNAVAILABLE, so without this the
    // tile shows greyed out and taps are silently ignored.
    override fun onStartListening() {
        super.onStartListening()
        qsTile?.apply {
            state = Tile.STATE_ACTIVE
            updateTile()
        }
    }

    override fun onClick() {
        super.onClick()
        val intent = Intent(this, CaptureActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        if (Build.VERSION.SDK_INT >= 34) {
            val pendingIntent = PendingIntent.getActivity(
                this, 0, intent, PendingIntent.FLAG_IMMUTABLE
            )
            startActivityAndCollapse(pendingIntent)
        } else {
            @Suppress("DEPRECATION")
            startActivityAndCollapse(intent)
        }
    }
}
