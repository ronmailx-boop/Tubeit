package com.tubeit.cliphistory

import android.app.Activity
import android.os.Bundle
import android.widget.Toast

/**
 * No-UI entry point for the Quick Settings tile and the launcher shortcut.
 * Grabs a window focus event just long enough to read the clipboard (the one
 * moment Android allows it), stores it, and closes itself immediately.
 */
class CaptureActivity : Activity() {

    private var handled = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        overridePendingTransition(0, 0)
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus && !handled) {
            handled = true
            val result = ClipboardCapture.capture(this)
            val message = if (result.added) {
                getString(R.string.capture_saved, result.preview)
            } else {
                getString(R.string.capture_nothing_new)
            }
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
            finish()
            overridePendingTransition(0, 0)
        }
    }
}
