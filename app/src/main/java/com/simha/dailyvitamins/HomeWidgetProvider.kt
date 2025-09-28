package com.simha.dailyvitamins

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import java.time.ZoneId
import java.time.LocalDate

class HomeWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (appWidgetId in appWidgetIds) {
            updateWidget(context, appWidgetManager, appWidgetId)
        }
    }

    private fun updateWidget(context: Context, mgr: AppWidgetManager, id: Int) {
        val views = RemoteViews(context.packageName, R.layout.widget_home)

        val tz = ZoneId.of("Asia/Jerusalem")
        val today = LocalDate.now(tz).toString()
        views.setTextViewText(R.id.tv_remaining, "תאריך: " + today)

        val intent = Intent(context, MainActivity::class.java)
        val pi = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        views.setOnClickPendingIntent(R.id.tv_title, pi)

        mgr.updateAppWidget(id, views)
    }
}
