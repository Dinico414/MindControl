package com.xenonware.mindcontrol

import android.os.ParcelFileDescriptor
import android.util.Log
import moe.shizuku.server.IShizukuService
import rikka.shizuku.Shizuku
import java.io.BufferedReader
import java.io.InputStreamReader

object ShizukuManager {
    private const val TAG = "ShizukuManager"
    private var isRunning = false
    private var monitoringThread: Thread? = null

    // Mapping from Linux Raw Scancodes (Hex) to Android KeyCodes
    // We use a broad map to ensure "Full Control" across different devices
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
        val binder = Shizuku.getBinder()
        if (binder == null || !binder.pingBinder()) {
            Log.e(TAG, "Shizuku not running or binder null")
            return
        }

        isRunning = true
        monitoringThread = Thread {
            var remoteProcess: moe.shizuku.server.IRemoteProcess? = null
            try {
                val service = IShizukuService.Stub.asInterface(binder)
                
                // We use 'getevent' without labels to get the fastest raw hex output.
                // This bypasses Android's "Event Mapping" which is often disabled during sleep.
                val command = arrayOf("sh", "-c", "getevent")
                
                remoteProcess = service.newProcess(command, null, null)
                val reader = BufferedReader(InputStreamReader(ParcelFileDescriptor.AutoCloseInputStream(remoteProcess.inputStream)))
                
                Log.i(TAG, ">>> KERNEL RAW MONITOR STARTED <<<")
                
                while (isRunning) {
                    val line = reader.readLine() ?: break
                    // Format: /dev/input/eventX: Type Code Value
                    // Example: /dev/input/event5: 0001 0072 00000001
                    
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
                            } else {
                                // Log unmapped keys so we can add them if needed
                                if (isDown) Log.d(TAG, "RAW SCANCODE DETECTED: $code (Not currently mapped)")
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Shizuku monitoring error", e)
            } finally {
                Log.i(TAG, ">>> KERNEL RAW MONITOR STOPPED <<<")
                remoteProcess?.destroy()
                isRunning = false
            }
        }
        monitoringThread?.start()
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
        return try {
            Shizuku.pingBinder() &&
                    Shizuku.checkSelfPermission() == android.content.pm.PackageManager.PERMISSION_GRANTED
        } catch (_: Exception) {
            false
        }
    }

    fun runShellCommand(command: String) {
        Thread { runShellCommandBlocking(command) }.start()
    }

    fun runShellCommandBlocking(command: String): String {
        try {
            val binder = Shizuku.getBinder()
            if (binder != null && binder.pingBinder()) {
                val service = IShizukuService.Stub.asInterface(binder)
                val remoteProcess = service.newProcess(arrayOf("sh", "-c", command), null, null)
                
                val reader = BufferedReader(InputStreamReader(ParcelFileDescriptor.AutoCloseInputStream(remoteProcess.inputStream)))
                val errorReader = BufferedReader(InputStreamReader(ParcelFileDescriptor.AutoCloseInputStream(remoteProcess.errorStream)))
                
                val output = StringBuilder()
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    output.append(line).append("\n")
                }
                
                val error = StringBuilder()
                while (errorReader.readLine().also { line = it } != null) {
                    error.append(line).append("\n")
                }
                
                val exitCode = remoteProcess.waitFor()
                if (output.isNotEmpty()) Log.d(TAG, "Command '$command' output: ${output.toString().trim()}")
                if (error.isNotEmpty()) Log.e(TAG, "Command '$command' error: ${error.toString().trim()}")
                Log.d(TAG, "Command '$command' exited with code $exitCode")
                return output.toString().trim()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error running command: $command", e)
        }
        return ""
    }
}
