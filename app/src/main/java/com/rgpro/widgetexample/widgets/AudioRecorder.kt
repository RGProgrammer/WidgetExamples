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
                    .also {
                        it.component = ComponentName(context, AudioRecordService::class.java)
                    }
                AudioRecordService.serviceInstance?.let { service ->
                    val action = receivedIntent.getIntExtra(EXTRA_CONTROl_AUDIO_RECORD, -1)
                    Log.d(TAG, "onReceive: service action = $action")
                    when (action) {
                        START_AUDIO_RECORD -> service.startRecording()
                        STOP_AUDIO_RECORD -> service.stopRecording()
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
                when (state) {
                    AudioRecordService.STATE_ERROR_RECORDING -> {
                        Toast.makeText(
                            context,
                            "Error Audio Recording state",
                            Toast.LENGTH_LONG
                        ).show()
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
        AudioRecordService.serviceInstance?.let { service ->
            isRecording = service.isRecording()
        }
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onEnabled(context: Context) {

        val audioRecorderServiceIntent = Intent(context, AudioRecordService::class.java)
            .putExtra(AudioRecordService.EXTRA_ENABLE_BROADCAST, true)

        (context.getSystemService(ALARM_SERVICE) as? AlarmManager)?.also { alarmManager ->
            alarmManager.set(
                AlarmManager.ELAPSED_REALTIME_WAKEUP, SystemClock.elapsedRealtime() + 500,
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    audioRecorderServiceIntent.putExtra(
                        AudioRecordService.EXTRA_CLASSNAME_BROADCAST_TARGET,
                        AudioRecorder::class.java.name
                    )
                    PendingIntent.getForegroundService(
                        context,
                        0,
                        audioRecorderServiceIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT
                    )
                } else {
                    PendingIntent.getService(
                        context,
                        0,
                        audioRecorderServiceIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT
                    )
                }

            )
        }
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//            context.startForegroundService(audioRecorderServiceIntent)
//        }else{
//            context.startService(audioRecorderServiceIntent)
//        }
    }

    override fun onDisabled(context: Context) {
        val audioRecorderServiceIntent = Intent(context, AudioRecordService::class.java)
        context.stopService(audioRecorderServiceIntent)
        Toast.makeText(context, "Disabled record service", Toast.LENGTH_LONG).show()
    }

    private fun updateAppWidget(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int
    ) {
        // Construct the RemoteViews object
        val views = RemoteViews(context.packageName, R.layout.audio_recorder)
        val broadcastIntent = Intent(ACTION_AUDIO_SERVICE_CONTROL).also {
            val cn = ComponentName(context, AudioRecorder::class.java)
            it.setComponent(cn)
        }

        if (!isRecording) {
            views.setInt(R.id.recorder_button, "setBackgroundResource", R.drawable.ic_record_button)
            broadcastIntent.putExtra(EXTRA_CONTROl_AUDIO_RECORD, START_AUDIO_RECORD)
            views.setOnClickPendingIntent(
                R.id.recorder_button,
                PendingIntent.getBroadcast(
                    context,
                    0,
                    broadcastIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT
                )
            )

        } else {
            views.setInt(R.id.recorder_button, "setBackgroundResource", R.drawable.ic_stop_button)
            broadcastIntent.putExtra(EXTRA_CONTROl_AUDIO_RECORD, STOP_AUDIO_RECORD)
            views.setOnClickPendingIntent(
                R.id.recorder_button,
                PendingIntent.getBroadcast(
                    context,
                    0,
                    broadcastIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT
                )
            )
        }
        // Instruct the widget manager to update the widget
        appWidgetManager.updateAppWidget(appWidgetId, views)
    }

}

