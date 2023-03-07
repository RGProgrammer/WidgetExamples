package com.rgpro.widgetexample.widgets

import android.app.ActivityManager
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.CpuUsageInfo
import android.os.Debug.getMemoryInfo
import android.os.HardwarePropertiesManager
import android.os.Parcel
import android.os.Parcelable.PARCELABLE_WRITE_RETURN_VALUE
import android.util.Log
import android.widget.RemoteViews
import androidx.core.app.ActivityManagerCompat
import androidx.core.content.ContextCompat.getSystemService
import com.rgpro.widgetexample.R
import java.io.FileDescriptor


//TODO implement a service(SystemResourceMonitorService) that register the batteryMonitor broadcast and update widget
class Monitor : AppWidgetProvider() {
    companion object{
        public val TAG :String = Monitor.javaClass.name
    }
    private var componentName: ComponentName? = null
    private lateinit var context : Context
    private var isCharging = false
    private var batteryLevelInPercentage = 0
    private var memoryUsageInPercentage = 0
    private var cpuLoadInPercentage = 0.0 ;
    private var cpuInfoError = false
    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray //the user can choose create multiple instances of the current widget
        //the app should update all the instance
    ) {

        getCPULoad(context)
        getRamUsage(context)

        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onEnabled(context: Context) {
        this.componentName = ComponentName(context, this.javaClass)
        this.context = context
        //battery info first retrieval
        val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        context.applicationContext.registerReceiver(this,filter).also {
            extractBatteryStatusFromIntent(it)
        }

        getCPULoad(context)
        getRamUsage(context)
    }

    override fun onDisabled(context: Context) {
        this.componentName = null
        context.applicationContext.unregisterReceiver(this)

    }

    override fun onReceive(context: Context?, intent: Intent?) {

        val action = intent!!.action
        if (Intent.ACTION_BATTERY_CHANGED == action) {
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
            //Battery info
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
            //CPU info
            if(!cpuInfoError) {
                it.setTextViewText(R.id.cpuUsageText, "$cpuLoadInPercentage %")
                it.setProgressBar(R.id.cpuUsageBar, 100, cpuLoadInPercentage.toInt(), false);
            }else{
                it.setTextViewText(R.id.cpuUsageText, "Error")
                it.setProgressBar(R.id.cpuUsageBar, 100, 0, false);
            }
            it.setImageViewResource(R.id.cpuIcon,R.drawable.ic_cpu)
            //Ram info
            it.setTextViewText(R.id.ramUsageText,"$memoryUsageInPercentage %")
            it.setProgressBar(R.id.ramUsageBar,100,memoryUsageInPercentage,false);
            it.setImageViewResource(R.id.ramIcon,R.drawable.ic_ram)

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
    private fun getCPULoad(context : Context)
    {
        //TODO get CPU load
        val hwPropertiesManager=
            context.getSystemService(Context.HARDWARE_PROPERTIES_SERVICE) as HardwarePropertiesManager
        try {
            val cpuUsages = hwPropertiesManager.cpuUsages
            cpuUsages.let { infoCPUs ->
                //var descriptor
                for (cpu in infoCPUs) {//for (cpu : CpuUsageInfo in infoCPUs){
                    if (cpu.describeContents() == CpuUsageInfo.CONTENTS_FILE_DESCRIPTOR) {
                        val parcel = Parcel.obtain();
                        cpu.writeToParcel(parcel, PARCELABLE_WRITE_RETURN_VALUE)
                        val fileDescriptor = parcel.readFileDescriptor();
                        Log.d(TAG, "file descriptor for cpu info $fileDescriptor")

                        //TODO
                    }
                }
            }
        }catch (e: SecurityException){
            cpuInfoError = true ;
        }
    }
    private fun getRamUsage(context : Context)
    {
        //TODO get the ram Load
        val activityManager =
            context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        activityManager.let { manager ->
            val memInfo = ActivityManager.MemoryInfo()

            // Fetching the data from the ActivityManager
            manager.getMemoryInfo(memInfo)

            // Fetching the available and total memory and converting into Giga Bytes
            val availMemory  = memInfo.availMem.toDouble()
            val totalMemory= memInfo.totalMem.toDouble()

            memoryUsageInPercentage = ((1.0- (availMemory/totalMemory))*100).toInt() ;

        }
    }

}
