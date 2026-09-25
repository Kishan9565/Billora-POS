package com.kishan.billorapos.core.presentation

import android.content.ClipData
import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import java.io.File

fun isWhatsAppAvailable(context: Context): Boolean = try {
    context.packageManager.getPackageInfo("com.whatsapp", 0)
    true
} catch (_: android.content.pm.PackageManager.NameNotFoundException) { false }

fun shareFile(context: Context, file: File, mimeType: String, targetPackage: String? = null) {
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.files", file)
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = mimeType
        putExtra(Intent.EXTRA_STREAM, uri)
        clipData = ClipData.newRawUri(file.name, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        if (targetPackage != null) setPackage(targetPackage)
    }
    val launch = if (targetPackage == null) Intent.createChooser(intent, "Save or share ${file.name}") else intent
    launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    try { context.startActivity(launch) }
    catch (_: android.content.ActivityNotFoundException) { throw IllegalStateException("No compatible sharing app is available") }
}
