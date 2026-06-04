package com.xenonware.mindcontrol

import android.app.Notification
import android.app.NotificationManager
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
            
            val rankingMap = currentRanking
            
            val filtered = sbns.filter { sbn ->
                val notification = sbn.notification

                // 1. Filter out permanent/ongoing notifications (like Shizuku, or background services)
                if (sbn.isOngoing) return@filter false

                // 2. Filter out internal Android or our own notifications
                if (sbn.packageName == "android") return@filter false
                if (sbn.packageName == "com.xenonware.mindcontrol") return@filter false

                // 3. Filter out media player notifications
                val template = notification.extras.getString(Notification.EXTRA_TEMPLATE)
                if (template != null && template.contains("MediaStyle")) return@filter false

                // 4. Filter by importance (only normal or high priority)
                // This effectively filters out "muted" (LOW or MIN importance) notifications
                if (rankingMap != null) {
                    val ranking = Ranking()
                    if (rankingMap.getRanking(sbn.key, ranking)) {
                        if (ranking.importance < NotificationManager.IMPORTANCE_DEFAULT) {
                            return@filter false
                        }
                    }
                }

                true
            }
            // One icon per app, latest first
            .sortedByDescending { it.postTime }
            .distinctBy { it.packageName }

            _activeNotifications.value = filtered
        } catch (e: Exception) {
            Log.e(TAG, "Error updating notifications", e)
        }
    }
}
