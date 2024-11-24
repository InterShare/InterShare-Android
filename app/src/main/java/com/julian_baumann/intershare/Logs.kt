// https://appcaesars.medium.com/write-logcat-logs-to-txt-file-android-kotlin-d4cc8a4c8112

package com.julian_baumann.intershare

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.text.SimpleDateFormat
import java.util.*

private fun saveLog() : StringBuilder{
    val stringBuilderLog = StringBuilder()

    // 1) "logcat -d" -> Default Logcat behaviour
    // 2) "logcat -d *:V" -> Verbose
    // 3) "logcat -d *:D" -> Debug
    // 4) "logcat -d *:I" -> Info
    // 5) "logcat -d *:W" -> Warn
    // 6) "logcat -d *:E" -> Error

    val command = "logcat -d com.julian_baumann.intershare:*"
    val process = Runtime.getRuntime().exec(command)
    val bufferedReader = BufferedReader(InputStreamReader(process.inputStream))
    var line: String?
    while (bufferedReader.readLine().also { line = it } != null) {
        stringBuilderLog.append(line).append("\n")
    }

    return stringBuilderLog
}


public fun saveLogsToTxtFile(context: Context) {
    /* Using coroutines for thread management, you can use other thread management ways as well ,
    but I highly recommend to use this thread management methods in asynchronous way because of performance reasons. */
    val coroutineCallLogger = CoroutineScope(Dispatchers.IO)
    coroutineCallLogger.launch {
        async {
            val fileDirectory = context.cacheDir
            val dateTime = SimpleDateFormat("yyyy-MM-dd'T'HH-mm-ss", Locale.getDefault()).format(Date())
            val fileName = "InterShare $dateTime.log"
            val filePath = File(fileDirectory, fileName)

            if (filePath.exists()) {
                filePath.delete()
            }

            runCatching {
                filePath.createNewFile()
                filePath.appendText(
                    saveLog().toString()
                )
            }.onSuccess {
                shareLogFile(context, filePath)
            }
        }
    }
}

private fun shareLogFile(context: Context, logFile: File) {
    if (!logFile.exists()) {
        return
    }

    val logUri = FileProvider.getUriForFile(
        context,
        "${context.packageName}.fileprovider",
        logFile
    )

    val shareIntent: Intent = Intent().apply {
        action = Intent.ACTION_SEND
        putExtra(Intent.EXTRA_STREAM, logUri)
        type = "text/plain"
    }

    context.startActivity(Intent.createChooser(shareIntent, null))
}
