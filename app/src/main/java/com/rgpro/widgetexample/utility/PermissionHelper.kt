package com.rgpro.widgetexample.utility

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.core.app.ActivityCompat
import androidx.core.app.ActivityCompat.requestPermissions
import androidx.core.content.ContextCompat


class PermissionHelper {
    companion object {
        val REQUEST_ALL_PERMISSIONS_CODE = 101
        val AllPermissions  = arrayOf(
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        )
        fun hasAllPermissions(ctx : Context) :Boolean
        {
            var isAllPermissionsEnabled = true
            for (permission in AllPermissions) {
                isAllPermissionsEnabled = (isAllPermissionsEnabled
                        && ActivityCompat.checkSelfPermission(
                    ctx,
                    permission
                ) == PackageManager.PERMISSION_GRANTED)
            }
            return !isAllPermissionsEnabled
        }

        fun launchPermissionSettings(ctx: Context) {
            val intent = Intent()
            intent.action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
            intent.data = Uri.fromParts("package", ctx.packageName, null)
            ctx.startActivity(intent)
        }
    }

}