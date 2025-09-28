package com.simha.dailyvitamins

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.time.ZoneId
import java.time.ZonedDateTime

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val req = intent.getIntExtra("REQ", -1)
        val db = AppDb.build(context)
        val tz = ZoneId.of("Asia/Jerusalem")
        val now = ZonedDateTime.now(tz)

        ensureChannel(context)

        // גישה למסד – בתוך קורוטינה
        runBlocking {
            val dateKey = now.toLocalDate().toString()
            val checksDao = db.checks()

            // לוגיקה פשוטה: אם אין אף סימון היום באף קטגוריה – נחשב שיש מה להזכיר
            val anyUnchecked = withContext(Dispatchers.IO) {
                DayPart.values().any { part ->
                    val cs = checksDao.forPart(dateKey, part)
                    cs.none { it.checked } // אין סימון לקטגוריה הזו
                }
            }

            if (req == 1002 && anyUnchecked) {
                notify(context, "תזכורת 23:20", "עדיין נותרו פריטים לא מסומנים להיום.")
            }
            if (req == 1001 && now.hour < 21) {
                // אפשר לדייק ל-EVENING בלבד בהמשך; כרגע תזכורת כללית לערב
                notify(context, "תזכורת ערב", "יש פריטים לערב שטרם סומנו.")
            }

            scheduleNext(context, req)
        }
    }

    private fun ensureChannel(ctx: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val ch = NotificationChannel("reminders", "תזכורות", NotificationManager.IMPORTANCE_HIGH)
            nm.createNotificationChannel(ch)
        }
    }

    private fun notify(ctx: Context, title: String, text: String) {
        val mgr = NotificationManagerCompat.from(ctx)
        val n = NotificationCompat.Builder(ctx, "reminders")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(text)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()
        mgr.notify(title.hashCode(), n)
    }

    companion object {
        fun scheduleDaily(context: Context) {
            schedule(context, 20, 30, 1001) // תזכורת ערב
            schedule(context, 23, 20, 1002) // תזכורת 23:20
        }

        private fun schedule(ctx: Context, hour: Int, minute: Int, reqCode: Int) {
            val am = ctx.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val i = Intent(ctx, ReminderReceiver::class.java).putExtra("REQ", reqCode)
            val pi = PendingIntent.getBroadcast(
                ctx, reqCode, i,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            val tz = ZoneId.of("Asia/Jerusalem")
            var t = ZonedDateTime.now(tz).withHour(hour).withMinute(minute).withSecond(0).withNano(0)
            if (t.isBefore(ZonedDateTime.now(tz))) t = t.plusDays(1)
            am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, t.toInstant().toEpochMilli(), pi)
        }

        private fun scheduleNext(ctx: Context, req: Int) {
            val am = ctx.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val i = Intent(ctx, ReminderReceiver::class.java).putExtra("REQ", req)
            val pi = PendingIntent.getBroadcast(
                ctx, req, i,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            val whenMs = System.currentTimeMillis() + 24L * 60 * 60 * 1000
            am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, whenMs, pi)
        }
    }
}
