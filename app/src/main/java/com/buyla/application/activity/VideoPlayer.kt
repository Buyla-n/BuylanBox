package com.buyla.application.activity

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.KeyboardDoubleArrowDown
import androidx.compose.material.icons.filled.KeyboardDoubleArrowUp
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOn
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.buyla.application.ui.theme.MyAppTheme
import kotlinx.coroutines.delay
import java.io.File

class VideoPlayer : ComponentActivity() {
    private lateinit var filePath: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()
        window.isNavigationBarContrastEnforced = false

        // 获取传递过来的文件路径
        filePath = intent.getStringExtra("filePath") ?: run {
            finish() // 如果没有文件路径，直接关闭Activity
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
        var currentPosition by remember { mutableLongStateOf(1L) }
        var totalDuration by remember { mutableLongStateOf(1L) }
        var showController by remember { mutableStateOf(true) }
        var parentHeight by remember { mutableFloatStateOf(0f) }
        var boxHeight by remember { mutableFloatStateOf(0f) }
        var ratio by remember { mutableFloatStateOf(0f) }


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

        LaunchedEffect(showController) {
            if (showController) {
                delay(5000) // 控制器显示 3 秒
                showController = false
            }
        }

        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    title = {
                        Text("视频")
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
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .background(MaterialTheme.colorScheme.background)
                    .onSizeChanged { size ->
                        parentHeight = size.height.toFloat()
                        if (parentHeight > 0) {
                            ratio = boxHeight / parentHeight
                        }
                    },
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (ratio < 0.85f) {
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
                            modifier = Modifier.padding(start = 16.dp),
                            maxLines = 1
                        )
                    }
                    Spacer(modifier = Modifier.padding(vertical = 8.dp))
                }
                @Composable
                fun VideoController(){
                    if (ratio > 0.6f) {
                        IconButton(
                            onClick = {
                                showController = false
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Filled.KeyboardDoubleArrowDown,
                                contentDescription = "隐藏控制器"
                            )
                        }
                    }
                    if (totalDuration > 0) {
                        Slider(
                            value = currentPosition.toFloat()
                                .coerceIn(0f, totalDuration.toFloat()),
                            onValueChange = { newValue ->
                                exoPlayer.seekTo(newValue.toLong())
                            },
                            valueRange = 0f..totalDuration.toFloat(),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 24.dp, vertical = 16.dp)
                        )
                    }

                    // 播放/暂停，循环/播完就暂停 和设置，倍速键
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
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

                        Button(onClick = {
                            // 这里可以添加设置的逻辑，例如弹出一个设置对话框
                        }) {
                            Text("设置")
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
                            Text("倍速: $playbackSpeed x", maxLines = 1)
                        }
                    }
                }

                // 中间的视频播放区域
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.9f)
                        .background(
                            MaterialTheme.colorScheme.surfaceContainerLow,
                            MaterialTheme.shapes.medium
                        )
                        .onSizeChanged { size ->
                            boxHeight = size.height.toFloat()
                            if (parentHeight > 0) {
                                ratio = boxHeight / parentHeight
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    AndroidView(factory = { context ->
                        PlayerView(context).apply {
                            player = exoPlayer
                            useController = false
                        }
                    },
                        modifier = Modifier.clip(MaterialTheme.shapes.medium))

                    if (ratio > 0.85f) {
                        this@Column.AnimatedVisibility(
                            visible = showController,
                            enter = fadeIn(animationSpec = tween(durationMillis = 100)),
                            exit = fadeOut(animationSpec = tween(durationMillis = 100)),
                            modifier = Modifier.align(Alignment.TopCenter).padding(top = 16.dp)
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
                                    modifier = Modifier.padding(start = 16.dp),
                                    maxLines = 1
                                )
                            }
                        }
                    }
                    if (ratio > 0.6f) {
                        this@Column.AnimatedVisibility(
                            visible = !showController,
                            enter = fadeIn(animationSpec = tween(durationMillis = 100)),
                            exit = fadeOut(animationSpec = tween(durationMillis = 100)),
                            modifier = Modifier.align(Alignment.BottomStart)
                        ) {
                            Column(
                                modifier = Modifier
                                    .padding(4.dp)
                                    .background(
                                        MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = 0.5f),
                                        MaterialTheme.shapes.medium
                                    )
                            ) {
                                IconButton(
                                    onClick = {
                                        showController = true
                                    }
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.KeyboardDoubleArrowUp,
                                        contentDescription = "显示控制器"
                                    )
                                }
                            }
                        }
                    }

                    if (ratio > 0.6f) {
                        this@Column.AnimatedVisibility(
                            visible = showController,
                            enter = fadeIn(animationSpec = tween(durationMillis = 100)),
                            exit = fadeOut(animationSpec = tween(durationMillis = 100)),
                            modifier = Modifier.align(Alignment.BottomCenter)
                        ) {
                            Column(
                                modifier = Modifier
                                    .padding(16.dp)
                                    .background(
                                        MaterialTheme.colorScheme.surfaceContainerLow,
                                        MaterialTheme.shapes.medium
                                    )
                            ) {
                                VideoController()
                            }
                        }
                    }
                }
                if (ratio < 0.6f) {
                    Column(
                        modifier = Modifier
                            .padding(16.dp)
                            .background(
                                MaterialTheme.colorScheme.surfaceContainerLow,
                                MaterialTheme.shapes.medium
                            )
                    ) {
                        VideoController()
                    }
                }
            }
        }
    }
}