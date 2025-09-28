package com.simha.dailyvitamins

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId

private val Context.dataStore by preferencesDataStore("prefs")

data class TodayEntry(val item: Item, val dayPart: DayPart, val sort: Int)
data class UiState(
    val entries: Map<DayPart, List<TodayEntry>> = emptyMap(),
    val checks: Map<DayPart, Set<Long>> = emptyMap(),
    val workoutDay: Boolean = false,
    val isMWF: Boolean = false
)

class MainViewModel(
    private val ctx: Context,
    private val db: AppDb
) : ViewModel() {

    private fun todayKey(): String =
        LocalDate.now(ZoneId.of("Asia/Jerusalem")).toString()

    private val KEY_WORKOUT get() = booleanPreferencesKey("workout_" + todayKey())

    private val _ui = MutableStateFlow(UiState())
    val ui = _ui.asStateFlow()

    init {
        viewModelScope.launch {
            ensureSeed()
            refresh()
        }
    }

    fun setWorkoutDay(on: Boolean) {
        viewModelScope.launch {
            ctx.dataStore.edit { it[KEY_WORKOUT] = on }
            refresh()
        }
    }

    fun toggle(part: DayPart, itemId: Long, checked: Boolean) {
        viewModelScope.launch {
            db.checks().upsert(DailyCheck(todayKey(), itemId, part, checked))
            refreshChecksOnly()
        }
    }

    private suspend fun isWorkoutToday(): Boolean {
        val dow = LocalDate.now(ZoneId.of("Asia/Jerusalem")).dayOfWeek
        val isMWF = (dow.name in listOf("MONDAY","WEDNESDAY","FRIDAY"))
        val pref = ctx.dataStore.data.map { it[KEY_WORKOUT] ?: false }
        val flag = pref.firstOrNull() ?: false
        _ui.value = _ui.value.copy(isMWF = isMWF)
        return isMWF && flag
    }

    private suspend fun refreshChecksOnly() {
        val dateKey = todayKey()
        val parts = DayPart.values()
        val checks = mutableMapOf<DayPart, Set<Long>>()
        for (p in parts) {
            val cs = db.checks().forPart(dateKey, p).filter { it.checked }.map { it.itemId }.toSet()
            checks[p] = cs
        }
        _ui.value = _ui.value.copy(checks = checks)
    }

    private suspend fun refresh() {
        val items = db.items().all()
        val assigns = db.assigns().all()
        val workout = isWorkoutToday()

        val tz = ZoneId.of("Asia/Jerusalem")
        val dow = LocalDate.now(tz).dayOfWeek.name.take(3).uppercase()

        val map = mutableMapOf<DayPart, MutableList<TodayEntry>>()
        for (a in assigns) {
            val item = items.firstOrNull { it.id == a.itemId } ?: continue
            if (!activeToday(a, dow)) continue
            val sort = if (workout) item.sortWorkout ?: item.sortDefault else item.sortDefault
            map.getOrPut(a.dayPart) { mutableListOf() }.add(TodayEntry(item, a.dayPart, sort))
        }
        val entries = map.mapValues { it.value.sortedBy { e -> e.sort } }

        val dateKey = todayKey()
        val checks = mutableMapOf<DayPart, Set<Long>>()
        for (p in DayPart.values()) {
            val cs = db.checks().forPart(dateKey, p).filter { it.checked }.map { it.itemId }.toSet()
            checks[p] = cs
        }

        _ui.value = UiState(entries, checks, workoutDay = workout, isMWF = _ui.value.isMWF)
    }

    private fun activeToday(a: Assignment, dow3: String): Boolean {
        val dj = a.daysJson ?: return true
        return dj.contains(dow3)
    }

    private suspend fun ensureSeed() {
        if (db.items().all().isNotEmpty()) return

        val names = listOf(
            "מולטי-ויטמין", "ויטמין D", "ויטמין C", "מגנזיום",
            "אומגה 3", "אבץ", "כורכום", "ברזל", "B קומפלקס",
            "פרוביוטיקה", "תרופה 1", "תרופה 2", "קפאין (אימון)",
            "קריאטין (אימון)", "בטא-אלנין (אימון)", "גלוקוזאמין",
            "קו-אנזים Q10"
        )
        val ids = db.items().insert(*names.mapIndexed { i, n ->
            Item(name = n, sortDefault = i, sortWorkout = if (n.contains("(אימון)")) i else null, workoutRelated = n.contains("(אימון)"))
        }.toTypedArray())

        val list = mutableListOf<Assignment>()
        fun add(name: String, part: DayPart, slotIndex: Int = 0, daysJson: String? = null) {
            val idx = names.indexOf(name)
            if (idx >= 0) list += Assignment(itemId = ids[idx], dayPart = part, slotIndex = slotIndex, daysJson = daysJson)
        }

        add("מולטי-ויטמין", DayPart.WAKE)
        add("ויטמין D", DayPart.MORNING)
        add("ויטמין C", DayPart.MORNING)
        add("ויטמין C", DayPart.NIGHT, slotIndex = 1, daysJson = "[\"MON\",\"WED\",\"FRI\"]")
        add("מגנזיום", DayPart.NIGHT)
        add("אומגה 3", DayPart.EVENING)
        add("אבץ", DayPart.NIGHT)
        add("כורכום", DayPart.MORNING)
        add("ברזל", DayPart.MORNING)
        add("B קומפלקס", DayPart.MORNING)
        add("פרוביוטיקה", DayPart.MORNING)
        add("תרופה 1", DayPart.MORNING)
        add("תרופה 2", DayPart.NIGHT)
        add("קפאין (אימון)", DayPart.WAKE)
        add("קריאטין (אימון)", DayPart.WAKE)
        add("בטא-אלנין (אימון)", DayPart.WAKE)
        add("גלוקוזאמין", DayPart.EVENING)
        add("קו-אנזים Q10", DayPart.MORNING)

        db.assigns().insert(*list.toTypedArray())
    }

    companion object {
        fun factory(ctx: Context): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                    @Suppress("UNCHECKED_CAST")
                    return MainViewModel(ctx.applicationContext, AppDb.build(ctx.applicationContext)) as T
                }
            }
    }
}
