package com.rgpro.widgetexample.widgets

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.widget.RemoteViews
import com.rgpro.widgetexample.R

/**
 * Implementation of App Widget functionality.
 */
class AudioRecorder : AppWidgetProvider() {
    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        //Should updateAll the widget to the same state so it won't cause any problems when the service state changes
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onEnabled(context: Context) {
        // Enter relevant functionality for when the first widget is created
        //TODO Start the recording audio service
    }

    override fun onDisabled(context: Context) {
        // Enter relevant functionality for when the last widget is disabled
        //TODO Stop the recording audioService
    }
    private fun updateAppWidget(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int
    ) {
        val widgetText = context.getString(R.string.appwidget_audio_recorder_text)
        // Construct the RemoteViews object
        val views = RemoteViews(context.packageName, R.layout.audio_recorder)
        views.setTextViewText(R.id.appwidget_text, widgetText)

        // Instruct the widget manager to update the widget
        appWidgetManager.updateAppWidget(appWidgetId, views)
    }
}

