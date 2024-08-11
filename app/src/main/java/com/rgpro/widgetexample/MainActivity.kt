package com.rgpro.widgetexample

import android.annotation.SuppressLint
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.Toast
import com.rgpro.widgetexample.utility.PermissionHelper
import com.rgpro.widgetexample.widgets.AudioRecorder
import com.rgpro.widgetexample.widgets.Monitor

class MainActivity : AppCompatActivity() {
    lateinit var mAppWidgetManager: AppWidgetManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val mAppWidgetManager = AppWidgetManager.getInstance(this);
        setContentView(R.layout.activity_main)
        if (!PermissionHelper.hasAllPermissions(this)) {
            Toast.makeText(this, "Must grant all permissions", Toast.LENGTH_LONG).show()
            PermissionHelper.launchPermissionSettings(this)
        }
        findViewById<Button>(R.id.monitor)?.also {
            it.setOnClickListener { v: View? ->
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    if (mAppWidgetManager.isRequestPinAppWidgetSupported()) {
                        val componentName = ComponentName(
                            this@MainActivity,
                            Monitor::class.java
                        )
                        mAppWidgetManager.requestPinAppWidget(componentName, null, null)

                    }
                }
            }
        }
        findViewById<Button>(R.id.recorder)?.also {
            it.setOnClickListener { v: View? ->
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    if (mAppWidgetManager.isRequestPinAppWidgetSupported()) {
                        val componentName =
                            ComponentName(this@MainActivity, AudioRecorder::class.java)
                        mAppWidgetManager.requestPinAppWidget(componentName, null, null)
                    }
                }
            }
        }
    }
}