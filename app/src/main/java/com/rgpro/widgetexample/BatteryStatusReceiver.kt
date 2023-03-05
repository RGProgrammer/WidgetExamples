package com.rgpro.widgetexample

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.rgpro.widgetexample.widgets.Monitor

class BatteryStatusReceiver : BroadcastReceiver() {
    companion object{
        public val TAG :String = BatteryStatusReceiver.javaClass.name
    }
    override fun onReceive(context: Context, intent: Intent) {
        //TODO test to receive battery status
        val action = intent!!.action
        Log.d(TAG,"received ACTION $action")
    }
}