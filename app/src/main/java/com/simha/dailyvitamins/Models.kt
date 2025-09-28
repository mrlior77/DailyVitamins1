package com.simha.dailyvitamins

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class DayPart { WAKE, MORNING, EVENING, NIGHT }
enum class DOW { MON, TUE, WED, THU, FRI, SAT, SUN }

@Entity(tableName = "items")
data class Item(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val sortDefault: Int = 0,
    val sortWorkout: Int? = null,
    val workoutRelated: Boolean = false
)

@Entity(tableName = "assignments", primaryKeys = ["itemId","dayPart","slotIndex"])
data class Assignment(
    val itemId: Long,
    val dayPart: DayPart,
    val slotIndex: Int = 0,
    val daysJson: String? = null
)

@Entity(tableName = "checks", primaryKeys = ["dateKey","itemId","dayPart"])
data class DailyCheck(
    val dateKey: String,
    val itemId: Long,
    val dayPart: DayPart,
    val checked: Boolean
)
