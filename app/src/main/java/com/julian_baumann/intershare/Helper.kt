package com.julian_baumann.intershare

import android.annotation.SuppressLint
import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import java.io.File
import java.io.FileOutputStream
import kotlin.math.log10
import kotlin.math.pow

@SuppressLint("DefaultLocale")
fun toHumanReadableSize(bytes: ULong?): String {
    if (bytes == 0UL || bytes == null) {
        return "0 B"
    }

    val units = arrayOf("B", "KiB", "MiB", "GiB", "TiB", "PiB", "EiB", "ZiB", "YiB")
    val digitGroups = (log10(bytes.toDouble()) / log10(1024.0)).toInt()

    return String.format("%.2f %s", bytes.toDouble() / 1024.0.pow(digitGroups.toDouble()), units[digitGroups])
}

fun getPathFromUri(context: Context, uri: Uri): String? {
    if (uri.scheme.equals("file")) {
        return uri.path
    } else if (uri.scheme.equals("content")) {
        val cursor = context.contentResolver.query(uri, null, null, null, null)
        cursor?.use {
            if (it.moveToFirst()) {
                val index = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (index != -1) {
                    val fileName = it.getString(index)
                    val directory = context.cacheDir
                    val file = File(directory, fileName)
                    if (!file.exists()) {
                        context.contentResolver.openInputStream(uri).use { input ->
                            FileOutputStream(file).use { output ->
                                input?.copyTo(output)
                            }
                        }
                    }
                    return file.absolutePath
                }
            }
        }
    }

    return null
}
