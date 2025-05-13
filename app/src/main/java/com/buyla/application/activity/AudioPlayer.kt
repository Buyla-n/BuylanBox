package com.buyla.application.activity

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOn
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import com.buyla.application.ui.theme.MyAppTheme
import kotlinx.coroutines.delay
import java.io.File

class AudioPlayer : ComponentActivity() {
    private lateinit var filePath: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()
        window.isNavigationBarContrastEnforced = false

        filePath = intent.getStringExtra("filePath") ?: run {
            finish()
            return
        }

        setContent {
            MyAppTheme {
                ActivityContent(filePath)
            }
        }
    }


    @Composable
    @OptIn(ExperimentalMaterial3Api::class)
    fun ActivityContent(filePath: String) {
        val context = LocalContext.current
        val exoPlayer = remember {
            ExoPlayer.Builder(context).build().apply {
                val mediaItem = MediaItem.fromUri(Uri.fromFile(File(filePath)))
                setMediaItem(mediaItem)
                prepare()
            }
        }

        var isPlaying by remember { mutableStateOf(false) }
        var isLooping by remember { mutableStateOf(false) }
        var playbackSpeed by remember { mutableFloatStateOf(1f) }
        var currentPosition by remember { mutableLongStateOf(0L) }
        var totalDuration by remember { mutableLongStateOf(0L) }

        DisposableEffect(Unit) {
            onDispose {
                exoPlayer.release()
            }
        }

        LaunchedEffect(exoPlayer) {
            while (true) {
                currentPosition = exoPlayer.currentPosition
                totalDuration = exoPlayer.duration
                if (totalDuration in 1..currentPosition && !isLooping) {
                    exoPlayer.pause()
                    isPlaying = false
                }
                delay(1)
            }
        }

        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    title = {
                        Text("音乐")
                    },
                    navigationIcon = {
                        IconButton(onClick = {
                            exoPlayer.release()
                            (context as? ComponentActivity)?.finish()
                        }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "返回"
                            )
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background,
                    )
                )
            },
        ) { contentPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(contentPadding)
                    .background(MaterialTheme.colorScheme.background),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.9f)
                        .fillMaxHeight(0.1f)
                        .background(
                            MaterialTheme.colorScheme.primaryContainer,
                            MaterialTheme.shapes.medium
                        ),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Text(
                        File(filePath).name,
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.padding(start = 16.dp)
                    )
                }
                Spacer(modifier = Modifier.padding(vertical = 8.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.9f)
                        .fillMaxHeight(0.6f)
                        .background(
                            MaterialTheme.colorScheme.primaryContainer,
                            MaterialTheme.shapes.medium
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.MusicNote,
                        contentDescription = "音乐图标",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(100.dp)
                    )
                }

                Column(
                    modifier = Modifier
                        .padding(16.dp)
                        .background(
                            MaterialTheme.colorScheme.surfaceContainerLow,
                            MaterialTheme.shapes.medium
                        )
                ) {
                    if (totalDuration > 0) {
                        Slider(
                            value = currentPosition.toFloat().coerceIn(0f, totalDuration.toFloat()),
                            onValueChange = { newValue ->
                                exoPlayer.seekTo(newValue.toLong())
                            },
                            valueRange = 0f..totalDuration.toFloat(),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 24.dp, vertical = 16.dp)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth()
                            .padding(bottom = 16.dp, end = 14.dp, start = 4.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        IconButton(
                            onClick = {
                                if (isPlaying) {
                                    exoPlayer.pause()
                                } else {
                                    exoPlayer.play()
                                }
                                isPlaying = !isPlaying
                                if (totalDuration in 1..currentPosition) {
                                    exoPlayer.seekTo(0)
                                }
                            }
                        ) {
                            Icon(
                                imageVector = if (isPlaying) {
                                    Icons.Filled.Pause
                                } else {
                                    Icons.Filled.PlayArrow
                                },
                                contentDescription = if (isPlaying) "暂停" else "播放",
                            )
                        }

                        IconButton(onClick = {
                            isLooping = !isLooping
                            exoPlayer.repeatMode =
                                if (isLooping) ExoPlayer.REPEAT_MODE_ONE else ExoPlayer.REPEAT_MODE_OFF
                        }) {
                            Icon(
                                imageVector = if (isLooping) {
                                    Icons.Filled.RepeatOn
                                } else {
                                    Icons.Filled.Repeat
                                },
                                contentDescription = if (isLooping) "关闭循环" else "开启循环"
                            )
                        }

                        IconButton(
                            onClick = {}
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Settings,
                                contentDescription = null,
                            )
                        }


                        Button(onClick = {
                            playbackSpeed = when (playbackSpeed) {
                                0.5f -> 1f
                                1f -> 1.5f
                                1.5f -> 2f
                                else -> 0.5f
                            }
                            exoPlayer.setPlaybackSpeed(playbackSpeed)
                        }) {
                            Text("倍速: $playbackSpeed x")
                        }
                    }
                }
            }
        }
    }
}
