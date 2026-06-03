package com.xenonware.mindcontrol

import android.graphics.Bitmap
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.os.Handler
import android.os.Looper
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

data class MediaInfo(
    val title: String?,
    val artist: String?,
    val albumArt: Bitmap?,
    val isPlaying: Boolean,
    val packageName: String,
    val controller: MediaController? = null
)

class NotificationListener : NotificationListenerService() {

    companion object {
        private const val TAG = "NotificationListener"
        private val _activeNotifications = MutableStateFlow<List<StatusBarNotification>>(emptyList())
        val activeNotificationsFlow: StateFlow<List<StatusBarNotification>> = _activeNotifications
        
        private val _activeMediaInfo = MutableStateFlow<MediaInfo?>(null)
        val activeMediaInfoFlow: StateFlow<MediaInfo?> = _activeMediaInfo
        
        var isServiceConnected = false
            private set
    }

    private var mediaSessionManager: MediaSessionManager? = null
    private val handler = Handler(Looper.getMainLooper())
    private val mediaCallback = object : MediaController.Callback() {
        override fun onMetadataChanged(metadata: MediaMetadata?) {
            updateMediaInfo()
        }
        override fun onPlaybackStateChanged(state: PlaybackState?) {
            updateMediaInfo()
        }
    }

    private var currentController: MediaController? = null

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "Service onCreate called")
        mediaSessionManager = getSystemService(MEDIA_SESSION_SERVICE) as MediaSessionManager
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        Log.d(TAG, "onNotificationPosted: ${sbn?.packageName}")
        updateNotifications()
        updateMediaInfo()
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        Log.d(TAG, "onNotificationRemoved: ${sbn?.packageName}")
        updateNotifications()
        updateMediaInfo()
    }

    override fun onListenerConnected() {
        Log.d(TAG, "onListenerConnected: Service bound and connected!")
        isServiceConnected = true
        updateNotifications()
        updateMediaInfo()
    }

    override fun onListenerDisconnected() {
        Log.d(TAG, "onListenerDisconnected")
        isServiceConnected = false
        super.onListenerDisconnected()
    }

    private fun updateMediaInfo() {
        if (!isServiceConnected) return
        
        handler.post {
            try {
                val sessions = mediaSessionManager?.getActiveSessions(
                    android.content.ComponentName(this, NotificationListener::class.java)
                )
                
                val controller = sessions?.firstOrNull { 
                    it.playbackState?.state == PlaybackState.STATE_PLAYING 
                } ?: sessions?.firstOrNull()

                if (controller != currentController) {
                    currentController?.unregisterCallback(mediaCallback)
                    controller?.registerCallback(mediaCallback)
                    currentController = controller
                }

                if (controller != null) {
                    val metadata = controller.metadata
                    val playbackState = controller.playbackState
                    
                    val info = MediaInfo(
                        title = metadata?.getString(MediaMetadata.METADATA_KEY_TITLE),
                        artist = metadata?.getString(MediaMetadata.METADATA_KEY_ARTIST),
                        albumArt = metadata?.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART) 
                            ?: metadata?.getBitmap(MediaMetadata.METADATA_KEY_ART),
                        isPlaying = playbackState?.state == PlaybackState.STATE_PLAYING,
                        packageName = controller.packageName,
                        controller = controller
                    )
                    _activeMediaInfo.value = info
                } else {
                    _activeMediaInfo.value = null
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error updating media info", e)
            }
        }
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
