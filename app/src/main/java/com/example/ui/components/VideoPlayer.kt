package com.example.ui.components

import android.app.Activity
import android.content.pm.ActivityInfo
import android.os.Build
import android.view.View
import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import androidx.media3.ui.PlayerView

@Composable
fun VideoPlayer(
    videoUrl: String,
    videoTitle: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val activity = context as? Activity

    // Intercept back presses to properly release player and return
    BackHandler {
        onBack()
    }

    // Initialize ExoPlayer with Maximum Quality parameter configuration and fallback support
    val exoPlayer = remember {
        try {
            val trackSelector = DefaultTrackSelector(context).apply {
                // Force Highest bitrate track selection, aiming for maximum quality (e.g. 1080P)
                parameters = buildUponParameters()
                    .setForceHighestSupportedBitrate(true)
                    .build()
            }
            ExoPlayer.Builder(context)
                .setTrackSelector(trackSelector)
                .build().apply {
                    val mediaItem = MediaItem.Builder()
                        .setUri(videoUrl)
                        .apply {
                            if (videoUrl.contains(".m3u8", ignoreCase = true) || videoUrl.contains("m3u8", ignoreCase = true)) {
                                setMimeType(androidx.media3.common.MimeTypes.APPLICATION_M3U8)
                            } else if (videoUrl.contains(".mpd", ignoreCase = true) || videoUrl.contains("mpd", ignoreCase = true)) {
                                setMimeType(androidx.media3.common.MimeTypes.APPLICATION_MPD)
                            }
                        }
                        .build()
                    setMediaItem(mediaItem)
                    prepare()
                    playWhenReady = true
                }
        } catch (e: Exception) {
            // Safe fallback initialization
            ExoPlayer.Builder(context)
                .build().apply {
                    val mediaItem = MediaItem.Builder()
                        .setUri(videoUrl)
                        .apply {
                            if (videoUrl.contains(".m3u8", ignoreCase = true) || videoUrl.contains("m3u8", ignoreCase = true)) {
                                setMimeType(androidx.media3.common.MimeTypes.APPLICATION_M3U8)
                            }
                        }
                        .build()
                    setMediaItem(mediaItem)
                    prepare()
                    playWhenReady = true
                }
        }
    }

    // Video buffering and state handling
    var isPlaying by remember { mutableStateOf(false) }
    var isBuffering by remember { mutableStateOf(true) }
    var playbackError by remember { mutableStateOf<String?>(null) }
    var hasEnded by remember { mutableStateOf(false) }

    DisposableEffect(exoPlayer) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(state: Int) {
                isBuffering = state == Player.STATE_BUFFERING
                isPlaying = state == Player.STATE_READY && exoPlayer.playWhenReady
                hasEnded = state == Player.STATE_ENDED
            }

            override fun onPlayerError(error: PlaybackException) {
                playbackError = "Filmi oxutmaq mümkün olmadı: ${error.message} (Zəhmət olmasa interneti və ya stream linkini yoxlayın)"
                isBuffering = false
            }
        }
        exoPlayer.addListener(listener)
        onDispose {
            exoPlayer.removeListener(listener)
            exoPlayer.release()
        }
    }

    // Dynamic rotation and status/nav bar immersive full-screen handling
    DisposableEffect(Unit) {
        val window = activity?.window
        val originalOrientation = activity?.requestedOrientation

        // Lock to Landscape screen mode safely
        try {
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        } catch (e: Exception) {
            e.printStackTrace()
        }
        window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        // Enter Full Screen Immersive Mode (hide gesture bar & status bar)
        window?.let { win ->
            val controller = WindowInsetsControllerCompat(win, win.decorView)
            controller.hide(WindowInsetsCompat.Type.statusBars() or WindowInsetsCompat.Type.navigationBars())
            controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }

        onDispose {
            // Restore rotation safely
            try {
                activity?.requestedOrientation = originalOrientation ?: ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            } catch (e: Exception) {
                e.printStackTrace()
            }
            window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

            // Exit Immersive Full Screen Mode
            window?.let { win ->
                val controller = WindowInsetsControllerCompat(win, win.decorView)
                controller.show(WindowInsetsCompat.Type.statusBars() or WindowInsetsCompat.Type.navigationBars())
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // Video Render View
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    player = exoPlayer
                    useController = true
                    // Show standard controls overlay but style beautifully
                    setBackgroundColor(android.graphics.Color.BLACK)
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        // Top Immersive Overlay Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopStart)
                .safeDrawingPadding()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Back action button
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .background(Color.Black.copy(alpha = 0.5f), shape = CircleShape)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Geri",
                    tint = Color.White
                )
            }

            // Live • 1080P Max Pill Badge matching the Immersive UI mockup
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .background(Color.Black.copy(alpha = 0.6f), shape = RoundedCornerShape(20.dp))
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(Color(0xFFE50914), shape = CircleShape)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "CANLI • AUTO 1080P MAX",
                    fontSize = 11.sp,
                    color = Color.White,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                )
            }
        }

        // Playback Buffer Spinner
        if (isBuffering) {
            CircularProgressIndicator(
                color = MaterialTheme.colorScheme.primary,
                strokeWidth = 4.dp,
                modifier = Modifier
                    .size(50.dp)
                    .align(Alignment.Center)
            )
        }

        // Cinematic Title Overlays
        if (!isPlaying && !isBuffering && playbackError == null) {
            Column(
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = videoTitle,
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Yüklənir...",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.6f)
                )
            }
        }

        // Playback Error Banner
        playbackError?.let { err ->
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                ),
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(24.dp)
                    .widthIn(max = 400.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Problem Baş Verdi",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = err,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = {
                            playbackError = null
                            isBuffering = true
                            exoPlayer.prepare()
                            exoPlayer.play()
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Text("Yenidən Sına")
                    }
                }
            }
        }
    }
}
