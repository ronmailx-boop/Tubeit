package com.tubeit.cliphistory

import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import com.tubeit.cliphistory.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var adapter: ClipAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        adapter = ClipAdapter(mutableListOf()) { item ->
            refresh(ClipStore.delete(this, item.id))
        }
        binding.recyclerView.layoutManager = GridLayoutManager(this, 2)
        binding.recyclerView.adapter = adapter

        refresh(ClipStore.purgeOld(this))
    }

    override fun onResume() {
        super.onResume()
        checkClipboard()
    }

    // Android only allows reading the clipboard once this window actually has
    // input focus -- onResume() can fire slightly before that (e.g. right after
    // launch, or coming back from the recents screen / notification shade), and
    // on some OEM builds (Samsung One UI included) a read attempted too early
    // silently returns null instead of the real content. onWindowFocusChanged()
    // firing with hasFocus=true is the reliable signal that a read will succeed,
    // so re-check there as well -- this is the only automatic capture point
    // Android allows without a custom keyboard/accessibility service.
    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) checkClipboard()
    }

    private fun checkClipboard() {
        val text = try {
            val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = clipboard.primaryClip
            if (clip != null && clip.itemCount > 0) {
                clip.getItemAt(0).coerceToText(this)?.toString().orEmpty()
            } else {
                ""
            }
        } catch (e: SecurityException) {
            Log.w("Tubeit", "Clipboard read denied", e)
            ""
        }

        if (text.isNotBlank()) {
            refresh(ClipStore.addIfNew(this, text))
        } else {
            refresh(ClipStore.purgeOld(this))
        }
    }

    private fun refresh(items: MutableList<ClipItem>) {
        adapter.updateItems(items)
        binding.emptyState.visibility = if (items.isEmpty()) View.VISIBLE else View.GONE
        binding.recyclerView.visibility = if (items.isEmpty()) View.GONE else View.VISIBLE
    }
}
