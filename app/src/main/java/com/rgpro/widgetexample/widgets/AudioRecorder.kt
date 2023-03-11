package com.rgpro.widgetexample.widgets

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import android.widget.RemoteViews
import android.widget.Toast
import com.rgpro.widgetexample.R
import com.rgpro.widgetexample.services.audiorecordservice.AudioRecordService
import com.rgpro.widgetexample.utility.PermissionHelper

class AudioRecorder : AppWidgetProvider(),ServiceConnection,AudioRecordService.OnRecordStatusChangedListener {

    private lateinit var audioRecorderServiceIntent :Intent
    private var audioRecorder : AudioRecordService? = null
    private var isRecording = false ;
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
        if(!PermissionHelper.hasAllPermissions(context)){
            Toast.makeText(context,"Must grant all permissions",Toast.LENGTH_LONG).show()
            PermissionHelper.launchPermissionSettings(context)
        }
        audioRecorderServiceIntent = Intent(context, AudioRecordService.Companion::class.java)
        context.startService(audioRecorderServiceIntent)
        context.bindService(audioRecorderServiceIntent,this,0)

    }

    override fun onDisabled(context: Context) {
        // Enter relevant functionality for when the last widget is disabled
        //TODO Stop the recording audioService
        context.stopService(audioRecorderServiceIntent)
    }
    private fun updateAppWidget(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int
    ) {
        // Construct the RemoteViews object
        val views =  RemoteViews(context.packageName, R.layout.audio_recorder)
        if(!isRecording) {
            views.setInt(R.id.recorder_button,"setBackground",R.drawable.ic_record_button)

        }else{
            views.setInt(R.id.recorder_button,"setBackground",R.drawable.ic_stop_button)
        }
        // Instruct the widget manager to update the widget
        appWidgetManager.updateAppWidget(appWidgetId, views)
    }

    override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
        val audioRecorderServiceBinder = binder as AudioRecordService.AudioRecordServiceBridge?
        audioRecorderServiceBinder?.let {
            audioRecorder = it.service
        }
    }

    override fun onServiceDisconnected(name: ComponentName?) {
        audioRecorder = null
    }

    override fun onStatusChanged(state: Int, errorMsg: String?) {

    }
}

