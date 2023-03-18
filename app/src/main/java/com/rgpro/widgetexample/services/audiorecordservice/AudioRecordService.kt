package com.rgpro.widgetexample.services.audiorecordservice

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.media.MediaRecorder
import android.media.MediaRecorder.MEDIA_RECORDER_INFO_MAX_DURATION_REACHED
import android.media.MediaRecorder.MEDIA_RECORDER_INFO_MAX_FILESIZE_REACHED
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationManagerCompat
import com.rgpro.widgetexample.R
import com.rgpro.widgetexample.widgets.AudioRecorder
import java.io.IOException
import java.lang.IllegalStateException
import java.util.*

class AudioRecordService : Service(), MediaRecorder.OnInfoListener {
    companion object {
        val TAG = AudioRecordService::class.simpleName
        const val ACTION_BROADCAST_AUDIO_RECORD_STATE_CHANGE =
            "com.rgpro.services.AudioRecordService.STATE_CHANGED_BROADCAST"
        const val EXTRA_RECORD_STATE = "state"
        const val EXTRA_ENABLE_BROADCAST = "broadcast"
        const val EXTRA_CLASSNAME_BROADCAST_TARGET = "target"

        const val STATE_ERROR_RECORDING = 0
        const val STATE_STARTED_RECORDING = 1
        const val STATE_STOPPED_RECORDING = 2
        private var default_outDirectory = "";
        val DEFAULT_OUT_DIRECTORY: String
            get() = default_outDirectory
        val default_MaxDuration = 20000 // 20 seconds

        private var _audioServiceInstance: AudioRecordService? = null
        val serviceInstance
            get() = _audioServiceInstance
    }

    inner class AudioRecordServiceBridge(val service: AudioRecordService) : Binder() {
    }

    interface OnRecordStatusChangedListener {
        fun onStatusChanged(context: Context, state: Int, errorMsg: String? = null);
    }

    private var targetComponent: ComponentName? = null
    private var sendStateChangeBroadcast = false
    private var recorder: MediaRecorder? = null
    private var binder = AudioRecordServiceBridge(this)
    private var filename: String = ""
    private var isRecording = false

    private var outDirectory: String? = null
    private var maxDuration: Int? = default_MaxDuration
    private var _onStatusChangedListener: OnRecordStatusChangedListener? = null
    public var onStatusChangedListener: OnRecordStatusChangedListener?
        get() {
            return _onStatusChangedListener
        }
        set(value) {
            _onStatusChangedListener = value
        }

    override fun onBind(intent: Intent): IBinder? {
        return binder
    }

    override fun onDestroy() {
        super.onDestroy()
        _audioServiceInstance = null
        Log.d(TAG, "onDestroy: Service destroyed")
        recorder?.let {
            if (isRecording)
                it.stop()
            it.release();
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        _audioServiceInstance = this
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "audio channel"
            val descriptionText = "Audio record service "
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel("audio_record", name, importance).apply {
                description = descriptionText
            }
            // Register the channel with the system
            val notificationManager: NotificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)

            val notification = Notification.Builder(this, "audio_record")
                .setContentTitle("Recorder")
                .setContentText("Ready")
                .setSmallIcon(R.mipmap.ic_launcher_round)
                .build()

            startForeground(515, notification)
        }
        intent?.let {
            sendStateChangeBroadcast = it.getBooleanExtra(EXTRA_ENABLE_BROADCAST, false)
            it.getStringExtra(EXTRA_CLASSNAME_BROADCAST_TARGET)?.also { target ->
                targetComponent = ComponentName(this, target)
            }

            Log.d(TAG, "onStartCommand: Service with broadcast state $sendStateChangeBroadcast")
        }
        Log.d(TAG, "onStartCommand: Service started")
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onCreate() {
        super.onCreate()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "audioRecordNotification"
            val descriptionText = ""
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel("audioRecordNotification", name, importance).apply {
                description = descriptionText
            }

        }
        default_outDirectory = this.application.baseContext.externalCacheDir?.absolutePath!!;
    }

    private fun prepareNewRecorderInstance(): Boolean {

        recorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(this.application.baseContext)
        } else {
            MediaRecorder();
        }
        try {
            recorder?.setOnInfoListener(this)
            recorder?.setAudioSource(MediaRecorder.AudioSource.MIC)
            //recorder?.setOutputFormat(MediaRecorder.OutputFormat.MPEG_2_TS);
            recorder?.setOutputFormat(MediaRecorder.OutputFormat.AAC_ADTS);
            recorder?.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);

        } catch (e: Exception) {
            Log.e(TAG, "error ${e.message}");
            recorder?.release();
            recorder = null;
            onStatusChangedListener?.onStatusChanged(this, 0, e.message) // error
            return false;
        }
        return true;
    }

    public fun startRecording() {
        prepareNewRecorderInstance()
        val outputDir = if (outDirectory != null) {
            outDirectory
        } else {
            default_outDirectory
        }
        filename = "$outputDir/${UUID.randomUUID()}.mp3"
        recorder?.setOutputFile(filename);

        recorder?.setMaxDuration(maxDuration!!)

        try {
            recorder?.prepare();
            recorder?.start();
            isRecording = true;
            onStatusChangedListener?.onStatusChanged(this, STATE_STARTED_RECORDING, null);
            if (sendStateChangeBroadcast) sendStateBroadcast(STATE_STARTED_RECORDING)
        } catch (e: IOException) {
            isRecording = false;
            Log.e(TAG, "startRecording: ", e)
            onStatusChangedListener?.onStatusChanged(
                this,
                STATE_ERROR_RECORDING,
                e.message
            ) // error
            if (sendStateChangeBroadcast) sendStateBroadcast(STATE_ERROR_RECORDING)
        } catch (e: IllegalStateException) {
            isRecording = false;
            Log.e(TAG, "startRecording: ", e)
            onStatusChangedListener?.onStatusChanged(
                this,
                STATE_ERROR_RECORDING,
                e.message
            ) // error
            if (sendStateChangeBroadcast) sendStateBroadcast(STATE_ERROR_RECORDING)
        }

    }

    public fun stopRecording() {
        recorder?.let {
            if (isRecording) {
                try {
                    it.stop()
                    onStatusChangedListener?.onStatusChanged(this, STATE_STOPPED_RECORDING, null);
                    if (sendStateChangeBroadcast) sendStateBroadcast(STATE_STOPPED_RECORDING)
                } catch (e: Exception) {
                    onStatusChangedListener?.onStatusChanged(
                        this,
                        STATE_ERROR_RECORDING,
                        e.message
                    ) // error
                    if (sendStateChangeBroadcast) sendStateBroadcast(STATE_ERROR_RECORDING)
                }

            }
            it.release()

        }
        isRecording = false;

    }

    public fun isRecording(): Boolean {
        return isRecording;
    }

    public fun setOutputDirectory(directory: String) {
        //TODO check if the directory is valid
        outDirectory = directory;
    }

    public fun setMaxDuration(duration: Int) {
        Log.d(TAG, "max duration set to $duration")
        maxDuration = duration;
    }

    private fun sendStateBroadcast(state: Int) {
        val broadcastIntent = Intent(ACTION_BROADCAST_AUDIO_RECORD_STATE_CHANGE).also {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                it.component = targetComponent
            }
            //it.component = ComponentName(this,AudioRecordService::class.java)
            it.putExtra(EXTRA_RECORD_STATE, state)
        }
        sendBroadcast(broadcastIntent)
    }

    override fun onInfo(mr: MediaRecorder?, what: Int, extra: Int) {
        if (what == MEDIA_RECORDER_INFO_MAX_DURATION_REACHED ||
            what == MEDIA_RECORDER_INFO_MAX_FILESIZE_REACHED
        ) {
            this.stopRecording()
        }
    }
}