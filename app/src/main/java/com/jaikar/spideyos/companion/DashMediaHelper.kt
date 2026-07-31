package com.jaikar.spideyos.companion

import android.content.ComponentName
import android.content.Context
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import com.jaikar.spideyos.notifications.SpideyNotificationListener

data class NowPlayingInfo(
    val title: String?,
    val artist: String?,
    val appLabel: String,
    val packageName: String,
)

object DashMediaHelper {
    private val KNOWN = mapOf(
        "com.spotify.music" to "Spotify",
        "com.google.android.apps.youtube.music" to "YouTube Music",
        "com.google.android.youtube" to "YouTube",
        "com.amazon.mp3" to "Amazon Music",
        "com.zhiliaoapp.musically" to "TikTok",
        "com.ss.android.ugc.trill" to "TikTok",
        "com.instagram.android" to "Instagram",
        "com.apple.android.music" to "Apple Music",
        "com.aspiro.tidal" to "Tidal",
        "com.soundcloud.android" to "SoundCloud",
        "com.pandora.android" to "Pandora",
        "deezer.android.app" to "Deezer",
        "com.gaana" to "Gaana",
        "com.jio.media.jiobeats" to "JioSaavn",
        "com.oneplus.music" to "Music",
        "com.android.music" to "Music",
        "com.sec.android.app.music" to "Samsung Music",
    )

    fun labelForPackage(pkg: String): String =
        KNOWN.entries.firstOrNull { pkg.contains(it.key) || it.key.contains(pkg) }?.value
            ?: pkg.substringAfterLast('.').replaceFirstChar { it.titlecase() }

    fun activeController(context: Context): MediaController? {
        return runCatching {
            val msm = context.getSystemService(MediaSessionManager::class.java) ?: return null
            val listener = ComponentName(context, SpideyNotificationListener::class.java)
            val sessions = msm.getActiveSessions(listener)
            sessions.firstOrNull { ctrl ->
                val state = ctrl.playbackState?.state
                state == PlaybackState.STATE_PLAYING
            } ?: sessions.firstOrNull { ctrl ->
                val state = ctrl.playbackState?.state
                state == PlaybackState.STATE_PAUSED || state == PlaybackState.STATE_BUFFERING
            } ?: sessions.firstOrNull()
        }.getOrNull()
    }

    fun nowPlaying(context: Context): Pair<String?, String?> {
        val info = nowPlayingInfo(context) ?: return null to null
        return info.title to info.artist
    }

    fun nowPlayingInfo(context: Context): NowPlayingInfo? {
        val ctrl = activeController(context) ?: return null
        val pkg = ctrl.packageName.orEmpty()
        val meta = ctrl.metadata
        return NowPlayingInfo(
            title = meta?.getString(MediaMetadata.METADATA_KEY_TITLE),
            artist = meta?.getString(MediaMetadata.METADATA_KEY_ARTIST)
                ?: meta?.getString(MediaMetadata.METADATA_KEY_ALBUM_ARTIST),
            appLabel = labelForPackage(pkg),
            packageName = pkg,
        )
    }

    fun skipNext(context: Context) {
        activeController(context)?.transportControls?.skipToNext()
    }

    fun skipPrevious(context: Context) {
        activeController(context)?.transportControls?.skipToPrevious()
    }

    fun playPause(context: Context) {
        val ctrl = activeController(context) ?: return
        val playing = ctrl.playbackState?.state == PlaybackState.STATE_PLAYING
        if (playing) ctrl.transportControls.pause() else ctrl.transportControls.play()
    }

    fun recentPhoto(context: Context): android.graphics.Bitmap? {
        val permission = if (android.os.Build.VERSION.SDK_INT >= 33) {
            android.Manifest.permission.READ_MEDIA_IMAGES
        } else {
            android.Manifest.permission.READ_EXTERNAL_STORAGE
        }
        if (androidx.core.content.ContextCompat.checkSelfPermission(context, permission) !=
            android.content.pm.PackageManager.PERMISSION_GRANTED
        ) {
            return null
        }
        return runCatching {
            val projection = arrayOf(
                android.provider.MediaStore.Images.Media._ID,
                android.provider.MediaStore.Images.Media.DATE_ADDED,
            )
            val sort = "${android.provider.MediaStore.Images.Media.DATE_ADDED} DESC"
            context.contentResolver.query(
                android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                projection,
                null,
                null,
                sort,
            )?.use { cursor ->
                if (!cursor.moveToFirst()) return null
                val id = cursor.getLong(0)
                val uri = android.net.Uri.withAppendedPath(
                    android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    id.toString(),
                )
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    android.graphics.BitmapFactory.decodeStream(stream)?.let { src ->
                        val max = 256
                        val scale = maxOf(src.width, src.height).toFloat() / max
                        if (scale <= 1f) src
                        else android.graphics.Bitmap.createScaledBitmap(
                            src,
                            (src.width / scale).toInt().coerceAtLeast(1),
                            (src.height / scale).toInt().coerceAtLeast(1),
                            true,
                        )
                    }
                }
            }
        }.getOrNull()
    }
}
