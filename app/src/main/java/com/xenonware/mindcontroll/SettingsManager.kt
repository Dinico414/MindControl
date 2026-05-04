package com.xenonware.mindcontroll

import android.content.Context
import androidx.core.content.edit

object SettingsManager {
    private const val PREFS_NAME = "MindControllPrefs"
    
    const val ACTION_DEFAULT = "DEFAULT"
    const val ACTION_NONE = "NONE"
    const val ACTION_PLAY_PAUSE = "PLAY_PAUSE"
    const val ACTION_NEXT = "NEXT"
    const val ACTION_PREVIOUS = "PREVIOUS"
    const val ACTION_VOLUME_UP = "VOLUME_UP"
    const val ACTION_VOLUME_DOWN = "VOLUME_DOWN"
    const val ACTION_FLASHLIGHT = "FLASHLIGHT"
    const val ACTION_SCREENSHOT = "SCREENSHOT"
    const val ACTION_LOCK = "LOCK"
    const val ACTION_BRIGHTNESS_UP = "BRIGHTNESS_UP"
    const val ACTION_BRIGHTNESS_DOWN = "BRIGHTNESS_DOWN"
    const val ACTION_HOME = "HOME"
    const val ACTION_BACK = "BACK"
    const val ACTION_RECENTS = "RECENTS"
    const val ACTION_NOTIFICATIONS = "NOTIFICATIONS"
    const val ACTION_QUICK_SETTINGS = "QUICK_SETTINGS"
    const val ACTION_ASSISTANT = "ASSISTANT"
    const val ACTION_ROTATE_TOGGLE = "ROTATE_TOGGLE"
    
    private const val KEY_DISABLE_IN_CAMERA = "disable_in_camera"

    fun getAction(context: Context, keyCode: Int, state: String, type: String): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString("btn_${keyCode}_${state}_${type}", ACTION_DEFAULT) ?: ACTION_DEFAULT
    }

    fun setAction(context: Context, keyCode: Int, state: String, type: String, action: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit { putString("btn_${keyCode}_${state}_${type}", action) }
    }

    fun isDisableInCamera(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_DISABLE_IN_CAMERA, true)
    }

    fun setDisableInCamera(context: Context, disabled: Boolean) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit { putBoolean(KEY_DISABLE_IN_CAMERA, disabled) }
    }
}
