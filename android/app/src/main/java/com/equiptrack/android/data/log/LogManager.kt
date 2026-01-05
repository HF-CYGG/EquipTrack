package com.equiptrack.android.data.log

import android.content.Context
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileWriter
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LogManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val logDir: File by lazy {
        // Create a "logs" subdirectory in external files dir
        val dir = File(context.getExternalFilesDir(null), "logs")
        if (!dir.exists()) {
            dir.mkdirs()
        }
        dir
    }

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault())
    private val fileNameFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    fun log(level: String, tag: String, message: String, throwable: Throwable? = null) {
        // Also log to Logcat
        when (level) {
            "ERROR" -> Log.e(tag, message, throwable)
            "WARN" -> Log.w(tag, message, throwable)
            "INFO" -> Log.i(tag, message, throwable)
            "DEBUG" -> Log.d(tag, message, throwable)
            else -> Log.v(tag, message, throwable)
        }

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val timestamp = dateFormat.format(Date())
                val logMessage = StringBuilder()
                    .append("[$timestamp] ")
                    .append("[$level] ")
                    .append("[$tag] ")
                    .append(message)
                
                throwable?.let {
                    logMessage.append("\nStack Trace:\n")
                    val sw = StringWriter()
                    it.printStackTrace(PrintWriter(sw))
                    logMessage.append(sw.toString())
                }
                logMessage.append("\n")

                val fileName = "app_log_${fileNameFormat.format(Date())}.txt"
                val file = File(logDir, fileName)
                
                FileWriter(file, true).use { writer ->
                    writer.append(logMessage.toString())
                }
            } catch (e: Exception) {
                Log.e("LogManager", "Failed to write log to file", e)
            }
        }
    }

    fun e(tag: String, message: String, throwable: Throwable? = null) {
        log("ERROR", tag, message, throwable)
    }

    fun w(tag: String, message: String, throwable: Throwable? = null) {
        log("WARN", tag, message, throwable)
    }

    fun i(tag: String, message: String, throwable: Throwable? = null) {
        log("INFO", tag, message, throwable)
    }

    fun d(tag: String, message: String, throwable: Throwable? = null) {
        log("DEBUG", tag, message, throwable)
    }

    fun getLogFiles(): List<File> {
        return logDir.listFiles()?.toList()?.sortedByDescending { it.lastModified() } ?: emptyList()
    }
    
    fun getLatestLogFile(): File? {
        return getLogFiles().firstOrNull()
    }
}
