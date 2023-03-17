package com.rgpro.widgetexample.widgets

import android.app.AlarmManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.*
import android.content.Context.ALARM_SERVICE
import android.os.Build
import android.os.IBinder
import android.os.SystemClock
import android.util.Log
import android.widget.RemoteViews
import android.widget.Toast
import com.rgpro.widgetexample.R
import com.rgpro.widgetexample.services.audiorecordservice.AudioRecordService
import com.rgpro.widgetexample.services.audiorecordservice.AudioRecordService.Companion.ACTION_BROADCAST_AUDIO_RECORD_STATE_CHANGE
import com.rgpro.widgetexample.utility.PermissionHelper

class AudioRecorder : AppWidgetProvider() {

    companion object {
        val TAG = AudioRecorder::class.simpleName
        const val ACTION_AUDIO_SERVICE_CONTROL = "com.rgpro.widgets.ACTION_AUDIO_SERVICE_CONTROL"
        const val EXTRA_CONTROl_AUDIO_RECORD = "audio_control"

        const val STOP_AUDIO_RECORD = 0
        const val START_AUDIO_RECORD = 1

    }

    private var isRecording: Boolean = false


    override fun onReceive(context: Context?, intent: Intent?) {
        intent?.let { receivedIntent ->
            val widgetManager = AppWidgetManager.getInstance(context)
            val ids =
                widgetManager.getAppWidgetIds(ComponentName(context!!, this.javaClass))
            if (receivedIntent.action == ACTION_AUDIO_SERVICE_CONTROL) {
                Log.d(TAG, "onReceive: action = $ACTION_AUDIO_SERVICE_CONTROL")
                val audioRecordServiceIntent = Intent(context, AudioRecordService::class.java)
                (peekService(
                    context,
                    audioRecordServiceIntent
                ) as? AudioRecordService.AudioRecordServiceBridge)?.also { iBinder ->
                    val action = receivedIntent.getIntExtra(EXTRA_CONTROl_AUDIO_RECORD, -1)
                    when (action) {
                        START_AUDIO_RECORD -> iBinder.service.startRecording()
                        STOP_AUDIO_RECORD -> iBinder.service.stopRecording()
                    }
                }
                this.onUpdate(
                    context,
                    widgetManager,
                    ids
                )
            }
            if (receivedIntent.action == ACTION_BROADCAST_AUDIO_RECORD_STATE_CHANGE) {
                Log.d(TAG, "onReceive: action = $ACTION_BROADCAST_AUDIO_RECORD_STATE_CHANGE")
                val state = receivedIntent.getIntExtra(
                    AudioRecordService.EXTRA_RECORD_STATE,
                    AudioRecordService.STATE_ERROR_RECORDING
                )
                isRecording = when (state) {
                    AudioRecordService.STATE_STARTED_RECORDING -> true
                    AudioRecordService.STATE_STOPPED_RECORDING -> false
                    else -> {
                        Toast.makeText(
                            context,
                            "Error Audio Recording state",
                            Toast.LENGTH_LONG
                        ).show()
                        false
                    }
                }


                // update all the widgets to show the correct data
                this.onUpdate(
                    context,
                    widgetManager,
                    ids
                )

            }
        }
        super.onReceive(context, intent)
    }

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {

        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onEnabled(context: Context) {
        if (!PermissionHelper.hasAllPermissions(context)) {
            Toast.makeText(context, "Must grant all permissions", Toast.LENGTH_LONG).show()
            PermissionHelper.launchPermissionSettings(context)
        }
        val audioRecorderServiceIntent = Intent(context, AudioRecordService::class.java)
            .putExtra(AudioRecordService.EXTRA_ENABLE_BROADCAST,true)

        (context.getSystemService(ALARM_SERVICE) as? AlarmManager)?.also { alarmManager ->

                alarmManager.set(
                    AlarmManager.ELAPSED_REALTIME_WAKEUP, SystemClock.elapsedRealtime() + 500,
                    PendingIntent.getService(
                        context,
                        0,
                        audioRecorderServiceIntent,
                        PendingIntent.FLAG_IMMUTABLE
                    )
                )
            }

//        SystemClock.sleep(1000)
//        val binder = this.peekService(
//            context,
//            audioRecorderServiceIntent
//        ) as? AudioRecordService.AudioRecordServiceBridge
//        binder?.let {
//            isRecording = it.service.isRecording()
//            Toast.makeText(context,"Audio Service started",Toast.LENGTH_SHORT).show()
//        }
//        val widgetManager = AppWidgetManager.getInstance(context)
//        val ids =
//            widgetManager.getAppWidgetIds(ComponentName(context, this.javaClass))
//        // update all the widgets to show the correct data
//        this.onUpdate(
//            context,
//            widgetManager,
//            ids
//        )

    }

    override fun onDisabled(context: Context) {
        val audioRecorderServiceIntent = Intent(context, AudioRecordService::class.java)
        context.stopService(audioRecorderServiceIntent)
        context.registerReceiver(this, IntentFilter(ACTION_AUDIO_SERVICE_CONTROL))
        context.registerReceiver(this, IntentFilter(ACTION_BROADCAST_AUDIO_RECORD_STATE_CHANGE))
        Toast.makeText(context, "Disabled record service", Toast.LENGTH_LONG).show()
    }

    private fun updateAppWidget(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int
    ) {
        // Construct the RemoteViews object
        val views = RemoteViews(context.packageName, R.layout.audio_recorder)
        val broadcastIntent = Intent(ACTION_AUDIO_SERVICE_CONTROL)
        broadcastIntent.flags = Intent.FLAG_FROM_BACKGROUND
        if (!isRecording) {
            views.setInt(R.id.recorder_button, "setBackgroundResource", R.drawable.ic_record_button)
            broadcastIntent.putExtra(EXTRA_CONTROl_AUDIO_RECORD, START_AUDIO_RECORD)
            views.setOnClickPendingIntent(R.id.recorder_button, PendingIntent.getBroadcast(context,0,broadcastIntent,PendingIntent.FLAG_UPDATE_CURRENT))

        } else {
            views.setInt(R.id.recorder_button, "setBackgroundResource", R.drawable.ic_stop_button)
            broadcastIntent.putExtra(EXTRA_CONTROl_AUDIO_RECORD, STOP_AUDIO_RECORD)
            views.setOnClickPendingIntent(R.id.recorder_button, PendingIntent.getBroadcast(context,0,broadcastIntent,PendingIntent.FLAG_UPDATE_CURRENT))
        }
        // Instruct the widget manager to update the widget
        appWidgetManager.updateAppWidget(appWidgetId, views)
    }

}

