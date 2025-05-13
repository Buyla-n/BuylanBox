package com.buyla.application.activity

import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import coil.compose.AsyncImage
import com.buyla.application.ui.theme.MyAppTheme
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import java.io.File

class ImagePlayer : ComponentActivity() {
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

        var parentHeight by remember { mutableFloatStateOf(0f) }
        var boxHeight by remember { mutableFloatStateOf(0f) }
        var ratio by remember { mutableFloatStateOf(0f) }
        val options = BitmapFactory.Options().apply {
            inJustDecodeBounds = true // 只解码边界信息，不加载完整图片
        }
        try {
            context.contentResolver.openInputStream(File(filePath).toUri())?.use { inputStream ->
                BitmapFactory.decodeStream(inputStream, null, options)
            }
        } catch (_: IllegalArgumentException){
            options.outWidth = 0
            options.outHeight = 0
        }

        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    title = {
                        Text("图片")
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
                    Column(
                        modifier = Modifier
                            .fillMaxWidth(0.9f)
                            .fillMaxHeight(0.1f)
                            .background(
                                MaterialTheme.colorScheme.primaryContainer,
                                MaterialTheme.shapes.medium
                            ),
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            File(filePath).name,
                            style = MaterialTheme.typography.titleLarge,
                            modifier = Modifier.padding(start = 16.dp).align(Alignment.Start),
                            maxLines = 1
                        )
                        Text(
                            text = "${options.outHeight} x ${options.outWidth}",
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(end = 16.dp).align(Alignment.End),
                            maxLines = 1
                        )
                    }
                    Spacer(modifier = Modifier.padding(vertical = 8.dp))

                var scale by remember { mutableFloatStateOf(1f) }
                var offsetX by remember { mutableFloatStateOf(0f) }
                var offsetY by remember { mutableFloatStateOf(0f) }
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
                        }
                        .pointerInput(Unit) {
                            coroutineScope {
                                detectTransformGestures { _, pan, zoom, _ ->
                                    launch {
                                        scale *= zoom
                                        offsetX += pan.x
                                        offsetY += pan.y
                                    }
                                }
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    AsyncImage(
                        model = filePath,
                        contentDescription = "图片内容",
                        modifier = Modifier
                            .clip(MaterialTheme.shapes.medium)
                            .fillMaxWidth()
                            .graphicsLayer(
                                scaleX = scale,
                                scaleY = scale,
                                translationX = offsetX,
                                translationY = offsetY
                            )
                    )
                }
            }
        }
    }
}