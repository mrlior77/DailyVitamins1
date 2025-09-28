package com.simha.dailyvitamins

import android.content.Context
import androidx.room.*

@Dao
interface ItemsDao {
    @Query("SELECT * FROM items") suspend fun all(): List<Item>
    @Insert suspend fun insert(vararg items: Item): List<Long>
    @Update suspend fun update(item: Item)
    @Delete suspend fun delete(item: Item)
    @Query("DELETE FROM items") suspend fun nuke()
}

@Dao
interface AssignmentsDao {
    @Query("SELECT * FROM assignments") suspend fun all(): List<Assignment>
    @Insert suspend fun insert(vararg a: Assignment)
    @Query("DELETE FROM assignments WHERE itemId=:itemId") suspend fun deleteForItem(itemId: Long)
    @Query("DELETE FROM assignments") suspend fun nuke()
}

@Dao
interface ChecksDao {
    @Query("SELECT * FROM checks WHERE dateKey=:dateKey AND dayPart=:dayPart")
    suspend fun forPart(dateKey: String, dayPart: DayPart): List<DailyCheck>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(c: DailyCheck)

    @Query("DELETE FROM checks WHERE dateKey < :keepFrom")
    suspend fun cleanup(keepFrom: String)
}

/** ✅ Converters אמיתיים ל־Room (מחרוזת <-> enum) */
class Converters {
    @TypeConverter
    fun dayPartToString(dp: DayPart): String = dp.name

    @TypeConverter
    fun stringToDayPart(s: String): DayPart = DayPart.valueOf(s)
}

/** ✅ לבטל יצוא סכימות ולהפעיל את ה־Converters */
@Database(
    entities = [Item::class, Assignment::class, DailyCheck::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDb : RoomDatabase() {
    abstract fun items(): ItemsDao
    abstract fun assigns(): AssignmentsDao
    abstract fun checks(): ChecksDao

    companion object {
        fun build(ctx: Context) =
            Room.databaseBuilder(ctx, AppDb::class.java, "daily.db").build()
    }
}
