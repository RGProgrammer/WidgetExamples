package com.rgpro.widgetexample.services.audiorecordservice

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.media.MediaRecorder
import android.media.MediaRecorder.MEDIA_RECORDER_INFO_MAX_DURATION_REACHED
import android.media.MediaRecorder.MEDIA_RECORDER_INFO_MAX_FILESIZE_REACHED
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.util.Log
import java.io.IOException
import java.lang.IllegalStateException
import java.util.*

class AudioRecordService : Service(), MediaRecorder.OnInfoListener {
    companion object {
        val TAG = AudioRecordService::class.simpleName
        private var default_outDirectory = "";
        val DEFAULT_OUT_DIRECTORY: String
            get() = default_outDirectory
        val default_MaxDuration = 20000 // 20 seconds
        //default notification Text
        private var notificationText = mutableMapOf<String,String> ().also{
            it.put("title", "Audio Recording Service" )
            it.put("ready","Service is ready to record")
            it.put("recording" , "Recording")

        }
        public fun updateNotificationKeyValue(source : Map<String,String> ) {
            if(source.containsKey("title"))
                AudioRecordService.notificationText["title"]= source["title"] as String

            if(source.containsKey("ready"))
                AudioRecordService.notificationText["ready"]= source["ready"] as String

            if(source.containsKey("recording"))
                AudioRecordService.notificationText["recording"]= source["recording"]as String
        }
    }

    inner class AudioRecordServiceBridge(val service: AudioRecordService) : Binder() {
    }
    interface OnRecordStatusChangedListener{
        fun onStatusChanged(state : Int , errorMsg : String? = null ) ;
    }

    private var recorder: MediaRecorder? = null
    private var binder = AudioRecordServiceBridge(this)
    private var filename: String = ""
    private var isRecording = false

    private var outDirectory: String? = null
    private var maxDuration : Int? = default_MaxDuration

    private var _onStatusChangedListener : OnRecordStatusChangedListener? =null
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
        recorder?.let {
            if(isRecording)
                it.stop()
            it.release();
        }
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
            onStatusChangedListener?.onStatusChanged(0,e.message) // error
            return false;
        }
        return true;
    }

    public fun startRecording() {
        prepareNewRecorderInstance()
        val outputDir = if (outDirectory!=null) {
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
            onStatusChangedListener?.onStatusChanged(1,null);
            Log.d(TAG, "startRecording: started")
        } catch (e: IOException) {
            isRecording = false;
            Log.e(TAG, "startRecording: ", e)
            onStatusChangedListener?.onStatusChanged(0,e.message) // error
        } catch (e: IllegalStateException) {
            isRecording = false;
            Log.e(TAG, "startRecording: ", e)
            onStatusChangedListener?.onStatusChanged(0,e.message) // error
        }

    }

    public fun stopRecording() {
        recorder?.let {
            if(isRecording) {
                try {
                    it.stop()
                    onStatusChangedListener?.onStatusChanged(2,null);
                } catch (e:Exception) {
                    onStatusChangedListener?.onStatusChanged(0,e.message) // error
                }

            }
            it.release()

        }
        isRecording = false;
        Log.d(TAG, "stopRecording $onStatusChangedListener")
    }

    public fun isRecording(): Boolean {
        return isRecording;
    }
    public fun setOutputDirectory(directory :String) {
        //TODO check if the directory is valid
        outDirectory = directory ;
    }
    public fun setMaxDuration(duration: Int){
        Log.d(TAG,"max duration set to $duration")
        maxDuration = duration ;
    }

    override fun onInfo(mr: MediaRecorder?, what: Int, extra: Int) {
        if(what == MEDIA_RECORDER_INFO_MAX_DURATION_REACHED ||
            what == MEDIA_RECORDER_INFO_MAX_FILESIZE_REACHED){
            this.stopRecording()
        }
    }
}