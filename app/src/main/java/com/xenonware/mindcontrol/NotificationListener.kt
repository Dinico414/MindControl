package com.xenonware.mindcontrol

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class NotificationListener : NotificationListenerService() {

    companion object {
        private const val TAG = "NotificationListener"
        private val _activeNotifications = MutableStateFlow<List<StatusBarNotification>>(emptyList())
        val activeNotificationsFlow: StateFlow<List<StatusBarNotification>> = _activeNotifications
        
        var isServiceConnected = false
            private set
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "Service onCreate called")
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        Log.d(TAG, "onNotificationPosted: ${sbn?.packageName}")
        updateNotifications()
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        Log.d(TAG, "onNotificationRemoved: ${sbn?.packageName}")
        updateNotifications()
    }

    override fun onListenerConnected() {
        Log.d(TAG, "onListenerConnected: Service bound and connected!")
        isServiceConnected = true
        updateNotifications()
    }

    override fun onListenerDisconnected() {
        Log.d(TAG, "onListenerDisconnected")
        isServiceConnected = false
        super.onListenerDisconnected()
    }

    private fun updateNotifications() {
        try {
            val sbns = activeNotifications
            if (sbns == null) {
                Log.w(TAG, "updateNotifications: activeNotifications is NULL (Service might not be connected yet)")
                return
            }
            
            Log.d(TAG, "updateNotifications: Total raw notifications = ${sbns.size}")
            
            val filtered = sbns.filter { sbn ->
                // Log every notification to see what's being considered
                Log.v(TAG, "Notification: ${sbn.packageName} | ongoing=${sbn.isOngoing} | id=${sbn.id}")

                // Filter out only the most obvious things we don't want
                if (sbn.packageName == "android") return@filter false
                if (sbn.packageName == "com.xenonware.mindcontrol") return@filter false

                true
            }
            // One icon per app, latest first
            .sortedByDescending { it.postTime }
            .distinctBy { it.packageName }

            Log.d(TAG, "updateNotifications: Final filtered count = ${filtered.size}")
            _activeNotifications.value = filtered
        } catch (e: Exception) {
            Log.e(TAG, "Error updating notifications", e)
        }
    }
}
