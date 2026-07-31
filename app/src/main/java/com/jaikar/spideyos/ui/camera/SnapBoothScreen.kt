package com.jaikar.spideyos.ui.camera

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.FileOutputOptions
import androidx.camera.video.Quality
import androidx.camera.video.QualitySelector
import androidx.camera.video.Recorder
import androidx.camera.video.Recording
import androidx.camera.video.VideoCapture
import androidx.camera.video.VideoRecordEvent
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.jaikar.spideyos.assistant.PupBrain
import com.jaikar.spideyos.data.SpideySettings
import com.jaikar.spideyos.ui.theme.SpideyGold
import com.jaikar.spideyos.ui.theme.SpideyNavy
import com.jaikar.spideyos.ui.theme.SpideyRed
import com.jaikar.spideyos.ui.theme.SpideyWeb
import java.io.File
import java.util.concurrent.Executors
import kotlinx.coroutines.delay

private enum class BoothMode { PHOTO, VIDEO }

@Composable
fun SnapBoothScreen(
    settings: SpideySettings,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var hasCam by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED,
        )
    }
    var hasMic by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
                PackageManager.PERMISSION_GRANTED,
        )
    }
    var mode by remember { mutableStateOf(BoothMode.PHOTO) }
    var reaction by remember { mutableStateOf<String?>(null) }
    var recording by remember { mutableStateOf(false) }
    var seconds by remember { mutableIntStateOf(0) }
    var flash by remember { mutableStateOf(false) }

    val imageCapture = remember { ImageCapture.Builder().build() }
    val recorder = remember {
        Recorder.Builder().setQualitySelector(QualitySelector.from(Quality.HD)).build()
    }
    val videoCapture = remember { VideoCapture.withOutput(recorder) }
    var activeRecording by remember { mutableStateOf<Recording?>(null) }
    val executor = remember { Executors.newSingleThreadExecutor() }
    var bound by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { result ->
        hasCam = result[Manifest.permission.CAMERA] == true || hasCam
        hasMic = result[Manifest.permission.RECORD_AUDIO] == true || hasMic
    }

    val blink = rememberInfiniteTransition(label = "rec")
    val recAlpha by blink.animateFloat(
        0.35f,
        1f,
        infiniteRepeatable(tween(650), RepeatMode.Reverse),
        label = "recAlpha",
    )

    LaunchedEffect(recording) {
        seconds = 0
        while (recording) {
            delay(1000)
            seconds++
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            activeRecording?.stop()
            executor.shutdown()
        }
    }

    fun bindCamera(previewView: PreviewView) {
        val future = ProcessCameraProvider.getInstance(context)
        future.addListener(
            {
                val provider = future.get()
                val preview = Preview.Builder().build().also {
                    it.surfaceProvider = previewView.surfaceProvider
                }
                provider.unbindAll()
                provider.bindToLifecycle(
                    lifecycleOwner,
                    CameraSelector.DEFAULT_BACK_CAMERA,
                    preview,
                    imageCapture,
                    videoCapture,
                )
                bound = true
            },
            ContextCompat.getMainExecutor(context),
        )
    }

    Column(Modifier.fillMaxSize().background(SpideyNavy)) {
        Row(
            Modifier.fillMaxWidth().padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = {
                if (recording) activeRecording?.stop()
                onBack()
            }) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = SpideyWeb)
            }
            Column(Modifier.weight(1f)) {
                Text("SnapBooth", color = SpideyWeb, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                Text("Cinematic photo · video · Pip reacts", color = SpideyGold, fontSize = 12.sp)
            }
            if (recording) {
                Text(
                    "REC ${"%02d:%02d".format(seconds / 60, seconds % 60)}",
                    color = Color.Red.copy(alpha = recAlpha),
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .background(Color.Black.copy(alpha = 0.45f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                )
            }
        }

        if (!hasCam) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    "Grant camera (and mic for video)",
                    color = SpideyWeb,
                    modifier = Modifier
                        .clickable {
                            permissionLauncher.launch(
                                arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO),
                            )
                        }
                        .background(SpideyRed, RoundedCornerShape(12.dp))
                        .padding(16.dp),
                )
            }
        } else {
            Box(
                Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(12.dp)
                    .border(2.dp, SpideyRed.copy(alpha = 0.9f), RoundedCornerShape(20.dp)),
            ) {
                AndroidView(
                    factory = { ctx ->
                        PreviewView(ctx).also { bindCamera(it) }
                    },
                    modifier = Modifier.fillMaxSize(),
                )
                // Cinematic letterbox + weave frame
                Canvas(Modifier.fillMaxSize()) {
                    val inset = 22f
                    drawRoundRect(
                        color = SpideyGold.copy(alpha = 0.85f),
                        topLeft = Offset(inset, inset),
                        size = Size(size.width - inset * 2, size.height - inset * 2),
                        cornerRadius = CornerRadius(32f, 32f),
                        style = Stroke(width = 5f),
                    )
                    // letterbox vibe
                    drawRect(Color.Black.copy(alpha = 0.35f), size = Size(size.width, 48f))
                    drawRect(
                        Color.Black.copy(alpha = 0.35f),
                        topLeft = Offset(0f, size.height - 48f),
                        size = Size(size.width, 48f),
                    )
                }
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                listOf(Color.Transparent, Color.Transparent, Color.Black.copy(alpha = 0.25f)),
                            ),
                        ),
                )
                Text(
                    if (mode == BoothMode.PHOTO) "SNAPBOOTH · STILL" else "SNAPBOOTH · MOTION",
                    color = SpideyGold,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 28.dp)
                        .background(SpideyNavy.copy(alpha = 0.75f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 12.dp, vertical = 5.dp),
                )
                if (flash) {
                    Box(Modifier.fillMaxSize().background(Color.White.copy(alpha = 0.55f)))
                }
            }

            reaction?.let {
                Text(
                    it,
                    color = SpideyWeb,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .background(SpideyRed.copy(alpha = 0.88f), RoundedCornerShape(12.dp))
                        .padding(12.dp),
                )
            }

            Spacer(Modifier.height(8.dp))
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                ModeChip(
                    selected = mode == BoothMode.PHOTO,
                    icon = Icons.Default.PhotoCamera,
                    label = "Photo",
                    enabled = !recording,
                ) { mode = BoothMode.PHOTO }
                // Shutter
                Box(
                    Modifier
                        .size(84.dp)
                        .scale(if (recording) 1.08f else 1f)
                        .background(if (recording) Color.Red else SpideyRed, CircleShape)
                        .border(4.dp, SpideyGold, CircleShape)
                        .clickable {
                            if (mode == BoothMode.PHOTO) {
                                flash = true
                                val file = File(context.cacheDir, "snap_${System.currentTimeMillis()}.jpg")
                                imageCapture.takePicture(
                                    ImageCapture.OutputFileOptions.Builder(file).build(),
                                    executor,
                                    object : ImageCapture.OnImageSavedCallback {
                                        override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                                            val line = PupBrain.reactToPhoto(settings.userName)
                                            ContextCompat.getMainExecutor(context).execute {
                                                reaction = line
                                                flash = false
                                                Toast.makeText(context, line, Toast.LENGTH_SHORT).show()
                                            }
                                        }

                                        override fun onError(exception: ImageCaptureException) {
                                            ContextCompat.getMainExecutor(context).execute {
                                                flash = false
                                                reaction = "Lens jam: ${exception.message}"
                                            }
                                        }
                                    },
                                )
                            } else {
                                if (!recording) {
                                    if (!hasMic) {
                                        permissionLauncher.launch(arrayOf(Manifest.permission.RECORD_AUDIO))
                                        return@clickable
                                    }
                                    startVideo(
                                        context = context,
                                        videoCapture = videoCapture,
                                        hasMic = hasMic,
                                        onStarted = { rec ->
                                            activeRecording = rec
                                            recording = true
                                            reaction = "Pip is filming with you…"
                                        },
                                        onFinished = { ok, msg ->
                                            recording = false
                                            activeRecording = null
                                            reaction = if (ok) {
                                                PupBrain.chat(settings.userName, "video done")
                                                    .ifBlank { "*tail spin* Video woven, ${settings.userName}!" }
                                            } else {
                                                msg
                                            }
                                        },
                                    )
                                } else {
                                    activeRecording?.stop()
                                }
                            }
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        if (mode == BoothMode.VIDEO) Icons.Default.FiberManualRecord else Icons.Default.PhotoCamera,
                        null,
                        tint = SpideyWeb,
                        modifier = Modifier.size(34.dp),
                    )
                }
                ModeChip(
                    selected = mode == BoothMode.VIDEO,
                    icon = Icons.Default.Videocam,
                    label = "Video",
                    enabled = !recording,
                ) { mode = BoothMode.VIDEO }
            }
            Spacer(Modifier.height(20.dp))
        }
    }
}

@Composable
private fun ModeChip(
    selected: Boolean,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .alpha(if (enabled) 1f else 0.4f)
            .clickable(enabled = enabled, onClick = onClick)
            .background(
                if (selected) SpideyGold.copy(alpha = 0.25f) else SpideyNavy.copy(alpha = 0.6f),
                RoundedCornerShape(14.dp),
            )
            .padding(horizontal = 14.dp, vertical = 10.dp),
    ) {
        Icon(icon, null, tint = if (selected) SpideyGold else SpideyWeb)
        Text(label, color = SpideyWeb, fontSize = 12.sp)
    }
}

@SuppressLint("MissingPermission")
private fun startVideo(
    context: android.content.Context,
    videoCapture: VideoCapture<Recorder>,
    hasMic: Boolean,
    onStarted: (Recording) -> Unit,
    onFinished: (Boolean, String) -> Unit,
) {
    val file = File(context.cacheDir, "clip_${System.currentTimeMillis()}.mp4")
    val opts = FileOutputOptions.Builder(file).build()
    var pending = videoCapture.output
        .prepareRecording(context, opts)
    if (hasMic) pending = pending.withAudioEnabled()
    val rec = pending.start(ContextCompat.getMainExecutor(context)) { event ->
        when (event) {
            is VideoRecordEvent.Start -> Unit
            is VideoRecordEvent.Finalize -> {
                if (event.hasError()) {
                    onFinished(false, "Video jam: ${event.cause?.message ?: "error"}")
                } else {
                    onFinished(true, file.absolutePath)
                }
            }
        }
    }
    onStarted(rec)
}
