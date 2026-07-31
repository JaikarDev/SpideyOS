package com.jaikar.spideyos.companion

import android.graphics.Bitmap
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

sealed class DashMood {
    data object Roaming : DashMood()
    data object Walking : DashMood()
    data class Sleeping(val line: String) : DashMood()
    data class Greeting(val line: String) : DashMood()
    data class Suggest(val line: String) : DashMood()
    data class MailPickup(val line: String) : DashMood()
    data class MessagePickup(val line: String) : DashMood()
    data class GalleryPeek(val line: String, val photo: Bitmap?) : DashMood()
    data class MusicListen(
        val line: String,
        val trackTitle: String?,
        val artist: String?,
    ) : DashMood()
    data class CameraSnap(val line: String) : DashMood()
    data class Searching(val line: String) : DashMood()
    data class Found(val line: String) : DashMood()
    data class TapReact(val line: String) : DashMood()
    data class Listening(val line: String) : DashMood()
}

sealed class DashEvent {
    data class Mail(val from: String?, val speak: String) : DashEvent()
    data class Message(val from: String?, val appName: String, val speak: String) : DashEvent()
    data object CameraCaptured : DashEvent()
    data object WaveHi : DashEvent()
    data object OpenGallery : DashEvent()
    data object OpenMusic : DashEvent()
    data object WakeUp : DashEvent()
    data object GoSleep : DashEvent()
    data object SuggestNow : DashEvent()
    data object StartListen : DashEvent()
}

object SpideyDashPipBus {
    private val _events = MutableSharedFlow<DashEvent>(extraBufferCapacity = 16)
    val events: SharedFlow<DashEvent> = _events.asSharedFlow()

    fun emit(event: DashEvent) {
        _events.tryEmit(event)
    }
}
