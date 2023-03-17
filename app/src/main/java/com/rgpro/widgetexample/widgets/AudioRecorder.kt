package com.rgpro.widgetexample.widgets

import android.app.AlarmManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Context.ALARM_SERVICE
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import android.os.SystemClock
import android.widget.RemoteViews
import android.widget.Toast
import com.rgpro.widgetexample.R
import com.rgpro.widgetexample.services.audiorecordservice.AudioRecordService
import com.rgpro.widgetexample.utility.PermissionHelper

class AudioRecorder : AppWidgetProvider() {
    private var isRecording : Boolean = false
    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        val audioRecorderServiceIntent = Intent(context, AudioRecordService.Companion::class.java)
        val binder = this.peekService(context,audioRecorderServiceIntent) as? AudioRecordService.AudioRecordServiceBridge
        binder?.let {
            isRecording = it.service.isRecording()
        }
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onEnabled(context: Context) {
        if(!PermissionHelper.hasAllPermissions(context)){
            Toast.makeText(context,"Must grant all permissions",Toast.LENGTH_LONG).show()
            PermissionHelper.launchPermissionSettings(context)
        }
        val audioRecorderServiceIntent = Intent(context, AudioRecordService.Companion::class.java)
        (context.getSystemService(ALARM_SERVICE) as? AlarmManager)?.also {alarmManager ->
            alarmManager.set(AlarmManager.ELAPSED_REALTIME_WAKEUP,SystemClock.elapsedRealtime()+1000,
                PendingIntent.getService(context,0,audioRecorderServiceIntent,PendingIntent.FLAG_IMMUTABLE))
        }
    }

    override fun onDisabled(context: Context) {
        val audioRecorderServiceIntent = Intent(context, AudioRecordService.Companion::class.java)
        context.stopService(audioRecorderServiceIntent)
        Toast.makeText(context,"Disabled record service",Toast.LENGTH_LONG).show()
    }
    private fun updateAppWidget(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int
    ) {
        // Construct the RemoteViews object
        val views =  RemoteViews(context.packageName, R.layout.audio_recorder)
        if(!isRecording) {
            views.setInt(R.id.recorder_button,"setBackgroundResource",R.drawable.ic_record_button)

        }else{
            views.setInt(R.id.recorder_button,"setBackgroundResource",R.drawable.ic_stop_button)
        }
        // Instruct the widget manager to update the widget
        appWidgetManager.updateAppWidget(appWidgetId, views)
    }

}

