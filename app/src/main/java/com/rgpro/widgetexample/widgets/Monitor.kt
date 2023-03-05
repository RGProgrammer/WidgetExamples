package com.rgpro.widgetexample.widgets

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.util.Log
import android.widget.RemoteViews
import com.rgpro.widgetexample.R

//TODO implement a service(SystemResourceMonitorService) that register the batteryMonitor broadcast and update widget
class Monitor : AppWidgetProvider() {
    companion object{
        public val TAG :String = Monitor.javaClass.name
    }
    private var componentName: ComponentName? = null
    private var isCharging = false
    private var batteryLevelInPercentage = 0
    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray //the user can choose create multiple instances of the current widget
        //the app should update all the instance
    ) {
        Log.d(TAG,"updating all monitor widgets")
        // There may be multiple widgets(instances) active, so update all of them
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onEnabled(context: Context) {
        this.componentName = ComponentName(context, this.javaClass)
        val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        context.applicationContext.registerReceiver(this,filter).also {
            extractBatteryStatusFromIntent(it)
        }

        //TODO get CPU and RAm usage
    }

    override fun onDisabled(context: Context) {
        this.componentName = null
        context.applicationContext.unregisterReceiver(this)

    }

    override fun onReceive(context: Context?, intent: Intent?) {

        val action = intent!!.action
        Log.d(TAG,"received ACTION $action")
        if (Intent.ACTION_BATTERY_CHANGED == action) {
            Log.d(TAG,"received battery changed prodcast")
           extractBatteryStatusFromIntent(intent)
            val widgetManager = AppWidgetManager.getInstance(context)
            val ids = widgetManager.getAppWidgetIds(componentName)
            // update all the widgets to show the correct data
            this.onUpdate(
                context!!,
                widgetManager,
                ids
            )
        } else {
            super.onReceive(context, intent)
        }
    }

    private fun updateAppWidget(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int
    ) {
        // Construct the RemoteViews object
        val views = RemoteViews(context.packageName, R.layout.monitor).also {
            it.setTextViewText(R.id.batteryPercentageText, "${batteryLevelInPercentage}%")
            it.setProgressBar(R.id.batteryPercentageBar, 100, batteryLevelInPercentage, false)
            it.setImageViewResource(
                R.id.batteryIcon,
                if (isCharging) {
                    R.drawable.ic_battery_charging
                } else {
                    R.drawable.ic_battery_good
                }
            )

        }
        appWidgetManager.updateAppWidget(appWidgetId, views)
    }
    private fun extractBatteryStatusFromIntent(intent :Intent?){
        intent?.let { batteryStatus ->

            val status: Int = batteryStatus.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
            isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING
                    || status == BatteryManager.BATTERY_STATUS_FULL
            val level: Int = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
            val scale: Int = batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
            batteryLevelInPercentage = (level * 100 / scale.toFloat()).toInt()

        }
    }

}
