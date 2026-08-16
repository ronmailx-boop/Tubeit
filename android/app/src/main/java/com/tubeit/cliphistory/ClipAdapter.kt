package com.tubeit.cliphistory

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.text.Spannable
import android.text.SpannableString
import android.text.method.LinkMovementMethod
import android.text.style.UnderlineSpan
import android.text.util.Linkify
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.tubeit.cliphistory.databinding.DialogClipDetailBinding
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
        binding.itemMeta.text = timeAgoLabel(item.timestampMillis)

        if (item.type == ClipType.LINK || item.type == ClipType.PHONE) {
            val underlined = SpannableString(item.text)
            underlined.setSpan(UnderlineSpan(), 0, item.text.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            binding.itemText.text = underlined
            binding.itemText.setTextColor(color)
        } else {
            binding.itemText.text = item.text
            binding.itemText.setTextColor(ContextCompat.getColor(context, R.color.text_primary))
        }

        binding.copyButton.setOnClickListener { copyToClipboard(context, item.text) }
        binding.deleteButton.setOnClickListener { onDelete(item) }
        holder.itemView.setOnClickListener { showDetailDialog(context, item) }
    }

    fun updateItems(newItems: MutableList<ClipItem>) {
        items = newItems
        notifyDataSetChanged()
    }

    private fun copyToClipboard(context: Context, text: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("tubeit", text))
        Toast.makeText(context, context.getString(R.string.copied_toast), Toast.LENGTH_SHORT).show()
    }

    private fun showDetailDialog(context: Context, item: ClipItem) {
        val binding = DialogClipDetailBinding.inflate(LayoutInflater.from(context))
        val color = typeColor(context, item.type)

        binding.dialogTypeLabel.text = item.type.label
        binding.dialogTypeLabel.setTextColor(color)
        binding.dialogText.text = item.text
        binding.dialogMeta.text = timeAgoLabel(item.timestampMillis)

        when (item.type) {
            ClipType.LINK -> {
                Linkify.addLinks(binding.dialogText, Linkify.WEB_URLS)
                binding.dialogText.movementMethod = LinkMovementMethod.getInstance()
            }
            ClipType.PHONE -> {
                // ACTION_DIAL opens the default dialer pre-filled with the number
                // without placing the call itself, so no CALL_PHONE permission
                // is needed -- the user still has to press call themselves.
                Linkify.addLinks(binding.dialogText, Linkify.PHONE_NUMBERS)
                binding.dialogText.movementMethod = LinkMovementMethod.getInstance()
            }
            else -> {}
        }

        MaterialAlertDialogBuilder(context)
            .setView(binding.root)
            .setPositiveButton(R.string.copy) { _, _ -> copyToClipboard(context, item.text) }
            .setNeutralButton(R.string.delete) { _, _ -> onDelete(item) }
            .setNegativeButton(R.string.close, null)
            .show()
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
