package com.tubeit.cliphistory

import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
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
        // Android only allows reading the clipboard while this app has foreground
        // focus, so this is the most automatic capture point available -- there is
        // no way to listen for copies made elsewhere while this app isn't in front.
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = clipboard.primaryClip
        if (clip != null && clip.itemCount > 0) {
            val text = clip.getItemAt(0).coerceToText(this)?.toString().orEmpty()
            if (text.isNotBlank()) {
                refresh(ClipStore.addIfNew(this, text))
                return
            }
        }
        refresh(ClipStore.purgeOld(this))
    }

    private fun refresh(items: MutableList<ClipItem>) {
        adapter.updateItems(items)
        binding.emptyState.visibility = if (items.isEmpty()) View.VISIBLE else View.GONE
        binding.recyclerView.visibility = if (items.isEmpty()) View.GONE else View.VISIBLE
    }
}
