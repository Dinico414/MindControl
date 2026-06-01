package com.xenonware.mindcontrol

import android.app.Application
import android.os.Build
import android.os.Environment
import android.util.Log
import java.io.File
import java.io.FileOutputStream
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MindControlApp : Application() {

    override fun onCreate() {
        super.onCreate()
        
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            try {
                saveCrashLog(throwable)
            } catch (e: Exception) {
                Log.e("MindControlApp", "Failed to save crash log", e)
            } finally {
                defaultHandler?.uncaughtException(thread, throwable)
            }
        }
    }

    private fun saveCrashLog(throwable: Throwable) {
        val sw = StringWriter()
        throwable.printStackTrace(PrintWriter(sw))
        val stackTrace = sw.toString()

        val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
        val deviceLog = """
            ================================================================
            MindControl Crash Log
            Timestamp: $timestamp
            Device: ${Build.MANUFACTURER} ${Build.MODEL}
            Android Version: ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})
            ================================================================
            
            StackTrace:
            $stackTrace
            
        """.trimIndent()

        // Target file in the public Downloads folder
        val publicDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        val file = File(publicDir, "mindcontrollLOG.txt")

        try {
            // On modern Android (10+), this might fail without MANAGE_EXTERNAL_STORAGE 
            // unless the app already has special access.
            FileOutputStream(file, true).use { fos ->
                fos.write(deviceLog.toByteArray())
            }
            Log.d("MindControlApp", "Crash log saved to ${file.absolutePath}")
        } catch (e: Exception) {
            // Fallback: Save to the app's own External Files directory in a "Downloads" subfolder
            // This is guaranteed to work without permissions.
            Log.w("MindControlApp", "Public storage failed: ${e.message}. Trying fallback.")
            val fallbackDir = getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
            val fallbackFile = File(fallbackDir, "mindcontrollLOG.txt")
            try {
                FileOutputStream(fallbackFile, true).use { fos ->
                    fos.write("Permission Denied for public Downloads. Saving to App Files instead.\n".toByteArray())
                    fos.write(deviceLog.toByteArray())
                }
                Log.d("MindControlApp", "Crash log saved to fallback: ${fallbackFile.absolutePath}")
            } catch (e2: Exception) {
                Log.e("MindControlApp", "Complete failure to save crash log", e2)
            }
        }
    }
}
