package com.tubeit.cliphistory

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.tubeit.cliphistory.databinding.ItemClipCardBinding

class ClipAdapter(
    private var items: MutableList<ClipItem>,
    private val onDelete: (ClipItem) -> Unit
) : RecyclerView.Adapter<ClipAdapter.ViewHolder>() {

    class ViewHolder(val binding: ItemClipCardBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemClipCardBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        val context = holder.itemView.context
        val binding = holder.binding
        val color = typeColor(context, item.type)

        binding.typeLabel.text = item.type.label
        binding.typeLabel.setTextColor(color)
        binding.accentStrip.setBackgroundColor(color)
        binding.itemText.text = item.text
        binding.itemMeta.text = timeAgoLabel(item.timestampMillis)

        binding.copyButton.setOnClickListener {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            clipboard.setPrimaryClip(ClipData.newPlainText("tubeit", item.text))
            Toast.makeText(context, context.getString(R.string.copied_toast), Toast.LENGTH_SHORT).show()
        }
        binding.deleteButton.setOnClickListener { onDelete(item) }
    }

    fun updateItems(newItems: MutableList<ClipItem>) {
        items = newItems
        notifyDataSetChanged()
    }

    private fun typeColor(context: Context, type: ClipType): Int {
        val colorRes = when (type) {
            ClipType.LINK -> R.color.type_link
            ClipType.PHONE -> R.color.type_phone
            ClipType.EMAIL -> R.color.type_email
            ClipType.NOTE -> R.color.type_note
        }
        return ContextCompat.getColor(context, colorRes)
    }

    private fun timeAgoLabel(timestampMillis: Long): String {
        val diffMinutes = (System.currentTimeMillis() - timestampMillis) / 60000
        return when {
            diffMinutes < 1 -> "עכשיו"
            diffMinutes < 60 -> "לפני $diffMinutes דקות"
            diffMinutes < 60 * 24 -> "לפני ${diffMinutes / 60} שעות"
            else -> {
                val days = diffMinutes / (60 * 24)
                if (days == 1L) "אתמול" else "לפני $days ימים"
            }
        }
    }
}
