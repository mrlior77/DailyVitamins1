package com.simha.dailyvitamins

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.SystemClock
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import java.time.ZoneId
import java.time.ZonedDateTime

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val req = intent.getIntExtra("REQ", -1)
        val db = AppDb.build(context)
        val tz = ZoneId.of("Asia/Jerusalem")
        val now = ZonedDateTime.now(tz)

        Thread {
            val dateKey = now.toLocalDate().toString()
            val checksDao = db.checks()
            val anyUnchecked = DayPart.values().any { part ->
                val cs = checksDao.forPart(dateKey, part)
                cs.none { it.checked }
            }

            if (req == 1002 && anyUnchecked) {
                notify(context, "תזכורת 23:20", "עדיין נותרו פריטים לא מסומנים להיום.")
            }
            if (req == 1001 && now.hour < 21) {
                notify(context, "תזכורת ערב", "יש פריטים בקטגוריית ערב שטרם סומנו.")
            }
            scheduleNext(context, req)
        }.start()
    }

    private fun notify(ctx: Context, title: String, text: String) {
        val ch = "reminders"
        val mgr = NotificationManagerCompat.from(ctx)
        val n = NotificationCompat.Builder(ctx, ch)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(text)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()
        mgr.notify(title.hashCode(), n)
    }

    companion object {
        fun scheduleDaily(context: Context) {
            schedule(context, 20, 30, 1001)
            schedule(context, 23, 20, 1002)
        }

        private fun schedule(ctx: Context, hour: Int, minute: Int, reqCode: Int) {
            val am = ctx.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val i = Intent(ctx, ReminderReceiver::class.java).putExtra("REQ", reqCode)
            val pi = PendingIntent.getBroadcast(ctx, reqCode, i, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

            val tz = ZoneId.of("Asia/Jerusalem")
            var t = ZonedDateTime.now(tz).withHour(hour).withMinute(minute).withSecond(0).withNano(0)
            if (t.isBefore(ZonedDateTime.now(tz))) t = t.plusDays(1)

            am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, t.toInstant().toEpochMilli(), pi)
        }

        private fun scheduleNext(ctx: Context, req: Int) {
            val whenMs = SystemClock.elapsedRealtime() + 24*60*60*1000L
            val am = ctx.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val i = Intent(ctx, ReminderReceiver::class.java).putExtra("REQ", req)
            val pi = PendingIntent.getBroadcast(ctx, req, i, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            am.setExactAndAllowWhileIdle(AlarmManager.ELAPSED_REALTIME_WAKEUP, whenMs, pi)
        }
    }
}

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        ReminderReceiver.scheduleDaily(context)
    }
}
