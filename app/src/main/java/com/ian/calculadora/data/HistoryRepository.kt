package com.ian.calculadora.data

import android.content.Context
import com.ian.calculadora.engine.AngleMode
import org.json.JSONArray
import org.json.JSONObject

data class HistoryItem(
    val id: Long,
    val expression: String,
    val result: String,
    val value: Double,
    val timestamp: Long,
)

/** Guarda historial, memoria y ajustes en SharedPreferences (persisten al cerrar la app). */
class HistoryRepository(context: Context) {

    private val prefs = context.getSharedPreferences("calculadora", Context.MODE_PRIVATE)

    fun loadHistory(): List<HistoryItem> {
        val raw = prefs.getString(KEY_HISTORY, null) ?: return emptyList()
        return try {
            val arr = JSONArray(raw)
            List(arr.length()) { i ->
                val o = arr.getJSONObject(i)
                HistoryItem(
                    id = o.getLong("id"),
                    expression = o.getString("expr"),
                    result = o.getString("result"),
                    value = o.getDouble("value"),
                    timestamp = o.getLong("ts"),
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun saveHistory(items: List<HistoryItem>) {
        val arr = JSONArray()
        items.take(MAX_ITEMS).forEach {
            arr.put(
                JSONObject()
                    .put("id", it.id)
                    .put("expr", it.expression)
                    .put("result", it.result)
                    .put("value", it.value)
                    .put("ts", it.timestamp)
            )
        }
        prefs.edit().putString(KEY_HISTORY, arr.toString()).apply()
    }

    var memory: Double
        get() = Double.fromBits(prefs.getLong(KEY_MEMORY, 0.0.toBits()))
        set(value) = prefs.edit().putLong(KEY_MEMORY, value.toBits()).apply()

    var ans: Double
        get() = Double.fromBits(prefs.getLong(KEY_ANS, 0.0.toBits()))
        set(value) = prefs.edit().putLong(KEY_ANS, value.toBits()).apply()

    var angleMode: AngleMode
        get() = AngleMode.valueOf(prefs.getString(KEY_ANGLE, AngleMode.DEG.name) ?: AngleMode.DEG.name)
        set(value) = prefs.edit().putString(KEY_ANGLE, value.name).apply()

    companion object {
        const val MAX_ITEMS = 500
        private const val KEY_HISTORY = "history"
        private const val KEY_MEMORY = "memory"
        private const val KEY_ANS = "ans"
        private const val KEY_ANGLE = "angle"
    }
}
