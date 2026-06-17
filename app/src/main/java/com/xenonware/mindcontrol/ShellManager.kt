package com.xenonware.mindcontrol

import android.os.ParcelFileDescriptor
import android.util.Log
import moe.shizuku.server.IShizukuService
import rikka.shizuku.Shizuku
import java.io.BufferedReader
import java.io.DataOutputStream
import java.io.File
import java.io.InputStreamReader

object ShellManager {
    private const val TAG = "ShellManager"
    private var isRunning = false
    private var monitoringThread: Thread? = null

    // Mapping from Linux Raw Scancodes (Hex) to Android KeyCodes
    private val scancodeMap = mapOf(
        "0072" to 25,  // VOL DOWN
        "0073" to 24,  // VOL UP
        "0074" to 26,  // POWER
        "00d4" to 27,  // CAMERA
        "020e" to 134, // FOCUS
        "003e" to 134, // FOCUS (User Device)
        "024b" to 131, // AI / ASSISTANT
        "02d0" to 131, // AI (Alt)
        "003b" to 131, // AI (User Device)
        "00a5" to 27,  // CAMERA (Alt)
        "00e2" to 26,  // POWER (Alt)
    )

    fun startMonitoring(onKeyEvent: (Int, Boolean) -> Unit) {
        if (isRunning) return
        isRunning = true

        monitoringThread = Thread {
            try {
                if (isShizukuAvailable()) {
                    startShizukuMonitoring(onKeyEvent)
                } else if (isRootAvailable()) {
                    startRootMonitoring(onKeyEvent)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Monitoring error", e)
            } finally {
                isRunning = false
            }
        }
        monitoringThread?.start()
    }

    private fun startShizukuMonitoring(onKeyEvent: (Int, Boolean) -> Unit) {
        var remoteProcess: moe.shizuku.server.IRemoteProcess? = null
        try {
            val binder = Shizuku.getBinder() ?: return
            val service = IShizukuService.Stub.asInterface(binder)
            val command = arrayOf("sh", "-c", "getevent")
            remoteProcess = service.newProcess(command, null, null)
            val reader = BufferedReader(InputStreamReader(ParcelFileDescriptor.AutoCloseInputStream(remoteProcess.inputStream)))
            
            Log.i(TAG, ">>> SHIZUKU KERNEL MONITOR STARTED <<<")
            readEventLines(reader, onKeyEvent)
        } finally {
            remoteProcess?.destroy()
        }
    }

    private fun startRootMonitoring(onKeyEvent: (Int, Boolean) -> Unit) {
        var process: Process? = null
        try {
            process = Runtime.getRuntime().exec("su -c getevent")
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            
            Log.i(TAG, ">>> ROOT KERNEL MONITOR STARTED <<<")
            readEventLines(reader, onKeyEvent)
        } finally {
            process?.destroy()
        }
    }

    private fun readEventLines(reader: BufferedReader, onKeyEvent: (Int, Boolean) -> Unit) {
        while (isRunning) {
            val line = reader.readLine() ?: break
            val parts = line.split("\\s+".toRegex()).filter { it.isNotBlank() }
            if (parts.size >= 4) {
                val type = parts[1] // 0001 = EV_KEY
                val code = parts[2] // The Scancode
                val value = parts[3] // 1 = Down, 0 = Up
                
                if (type == "0001") {
                    val androidKeyCode = scancodeMap[code] ?: -1
                    val isDown = value.endsWith("1")
                    
                    if (androidKeyCode != -1) {
                        Log.i(TAG, "HARDWARE MATCH: $code -> $androidKeyCode (${if(isDown) "DOWN" else "UP"})")
                        onKeyEvent(androidKeyCode, isDown)
                    }
                }
            }
        }
    }

    fun stopMonitoring() {
        isRunning = false
        monitoringThread?.interrupt()
        monitoringThread = null
    }

    fun injectKey(keyCode: Int) {
        runShellCommand("input keyevent $keyCode")
    }

    fun isAvailable(): Boolean {
        return isShizukuAvailable() || isRootAvailable()
    }

    fun isShizukuAvailable(): Boolean {
        return try {
            Shizuku.pingBinder() &&
                    Shizuku.checkSelfPermission() == android.content.pm.PackageManager.PERMISSION_GRANTED
        } catch (_: Exception) {
            false
        }
    }

    fun isRootAvailable(): Boolean {
        return try {
            val process = Runtime.getRuntime().exec("su -v")
            val exitCode = process.waitFor()
            exitCode == 0
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Checks if the device is rooted (su exists) regardless of permission granted.
     */
    fun isDeviceRooted(): Boolean {
        val paths = arrayOf(
            "/system/app/Superuser.apk", "/sbin/su", "/system/bin/su", "/system/xbin/su",
            "/data/local/xbin/su", "/data/local/bin/su", "/system/sd/xbin/su",
            "/system/bin/failsafe/su", "/data/local/su", "/su/bin/su"
        )
        for (path in paths) {
            if (File(path).exists()) return true
        }
        return false
    }

    fun runShellCommand(command: String) {
        Thread { runShellCommandBlocking(command) }.start()
    }

    fun runShellCommandBlocking(command: String): String {
        if (isShizukuAvailable()) {
            return runShizukuCommandBlocking(command)
        } else if (isRootAvailable()) {
            return runRootCommandBlocking(command)
        }
        return ""
    }

    private fun runShizukuCommandBlocking(command: String): String {
        try {
            val binder = Shizuku.getBinder()
            if (binder != null && binder.pingBinder()) {
                val service = IShizukuService.Stub.asInterface(binder)
                val remoteProcess = service.newProcess(arrayOf("sh", "-c", command), null, null)
                
                val reader = BufferedReader(InputStreamReader(ParcelFileDescriptor.AutoCloseInputStream(remoteProcess.inputStream)))
                val output = StringBuilder()
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    output.append(line).append("\n")
                }
                remoteProcess.waitFor()
                return output.toString().trim()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Shizuku command error: $command", e)
        }
        return ""
    }

    private fun runRootCommandBlocking(command: String): String {
        var process: Process? = null
        var os: DataOutputStream? = null
        var reader: BufferedReader? = null
        try {
            process = Runtime.getRuntime().exec("su")
            os = DataOutputStream(process.outputStream)
            reader = BufferedReader(InputStreamReader(process.inputStream))

            os.writeBytes("$command\n")
            os.writeBytes("exit\n")
            os.flush()

            val output = StringBuilder()
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                output.append(line).append("\n")
            }
            process.waitFor()
            return output.toString().trim()
        } catch (e: Exception) {
            Log.e(TAG, "Root command error: $command", e)
        } finally {
            try { os?.close() } catch (_: Exception) {}
            try { reader?.close() } catch (_: Exception) {}
            process?.destroy()
        }
        return ""
    }
}
