package com.tubeit.cliphistory

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import org.json.JSONArray
import org.json.JSONObject

enum class ClipType(val label: String) {
    LINK("קישור"),
    PHONE("טלפון"),
    EMAIL("אימייל"),
    NOTE("טקסט")
}

data class ClipItem(
    val id: Long,
    val type: ClipType,
    val text: String,
    val timestampMillis: Long
)

/**
 * Local-only clipboard history, persisted via SharedPreferences.
 * No network access is used or requested anywhere in this app.
 */
object ClipStore {
    private const val PREFS_NAME = "tubeit_clip_history"
    private const val KEY_ITEMS = "items"
    private const val RETENTION_MILLIS = 7L * 24 * 60 * 60 * 1000 // 7 days

    private val urlRegex = Regex("^(https?://|www\\.)\\S+$", RegexOption.IGNORE_CASE)
    private val emailRegex = Regex("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")
    private val phoneRegex = Regex("^[+\\d][\\d\\-\\s()]{6,}$")

    private fun prefs(context: Context): SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun classify(text: String): ClipType {
        val trimmed = text.trim()
        return when {
            urlRegex.containsMatchIn(trimmed) -> ClipType.LINK
            emailRegex.matches(trimmed) -> ClipType.EMAIL
            phoneRegex.matches(trimmed) && trimmed.length in 7..20 -> ClipType.PHONE
            else -> ClipType.NOTE
        }
    }

    fun loadAll(context: Context): MutableList<ClipItem> {
        val raw = prefs(context).getString(KEY_ITEMS, "[]") ?: "[]"
        val array = JSONArray(raw)
        val list = mutableListOf<ClipItem>()
        for (i in 0 until array.length()) {
            val obj = array.getJSONObject(i)
            list.add(
                ClipItem(
                    id = obj.getLong("id"),
                    type = ClipType.valueOf(obj.getString("type")),
                    text = obj.getString("text"),
                    timestampMillis = obj.getLong("timestampMillis")
                )
            )
        }
        return list
    }

    private fun saveAll(context: Context, items: List<ClipItem>) {
        val array = JSONArray()
        items.forEach { item ->
            val obj = JSONObject()
            obj.put("id", item.id)
            obj.put("type", item.type.name)
            obj.put("text", item.text)
            obj.put("timestampMillis", item.timestampMillis)
            array.put(obj)
        }
        prefs(context).edit { putString(KEY_ITEMS, array.toString()) }
    }

    /** Drops items older than the retention window and persists the trimmed list. */
    fun purgeOld(context: Context): MutableList<ClipItem> {
        val now = System.currentTimeMillis()
        val items = loadAll(context).filter { now - it.timestampMillis <= RETENTION_MILLIS }.toMutableList()
        saveAll(context, items)
        return items
    }

    /** Adds [text] as a new history entry unless it's empty or identical to the most recent one. */
    fun addIfNew(context: Context, text: String): MutableList<ClipItem> {
        val trimmed = text.trim()
        val items = purgeOld(context)
        if (trimmed.isEmpty()) return items
        if (items.isNotEmpty() && items[0].text == trimmed) return items
        val newItem = ClipItem(
            id = System.currentTimeMillis(),
            type = classify(trimmed),
            text = trimmed,
            timestampMillis = System.currentTimeMillis()
        )
        items.add(0, newItem)
        saveAll(context, items)
        return items
    }

    fun delete(context: Context, id: Long): MutableList<ClipItem> {
        val items = loadAll(context).filter { it.id != id }.toMutableList()
        saveAll(context, items)
        return items
    }
}
