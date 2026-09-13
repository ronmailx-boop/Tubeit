package com.tubeit.cliphistory

import android.content.ClipboardManager
import android.content.Context
import android.util.Log

data class CaptureResult(val added: Boolean, val preview: String)

/**
 * Reads the current clipboard and stores it if new. Shared by MainActivity's
 * on-open check and CaptureActivity's quick-capture flow so both read the
 * clipboard and classify/store it the exact same way.
 */
object ClipboardCapture {

    fun capture(context: Context): CaptureResult {
        val text = try {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = clipboard.primaryClip
            if (clip != null && clip.itemCount > 0) {
                clip.getItemAt(0).coerceToText(context)?.toString().orEmpty()
            } else {
                ""
            }
        } catch (e: SecurityException) {
            Log.w("Tubeit", "Clipboard read denied", e)
            ""
        }

        val trimmed = text.trim()
        if (trimmed.isEmpty()) {
            ClipStore.purgeOld(context)
            return CaptureResult(added = false, preview = "")
        }

        val before = ClipStore.loadAll(context).firstOrNull()?.id
        val items = ClipStore.addIfNew(context, trimmed)
        val added = items.firstOrNull()?.id != before
        return CaptureResult(added = added, preview = trimmed.take(40))
    }
}
