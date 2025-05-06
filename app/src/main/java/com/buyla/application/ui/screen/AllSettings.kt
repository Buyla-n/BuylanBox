package com.buyla.application.ui.screen

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Hardware
import androidx.compose.material.icons.outlined.Memory
import androidx.compose.material.icons.outlined.SdStorage
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

object AllSettings {
    data class HardwareState(
        val cpuUsage: Float = 0f,
        val gpuUsage: Float = 0f,
        val memoryUsage: Float = 0f,
        val totalMemory: Int = 16
    )

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun SettingsScreen() {

        val hardwareState = remember { mutableStateOf(fetchHardwareStatus()) }

        // 启动监控协程
        LaunchedEffect(Unit) {
            while (true) {
                hardwareState.value = fetchHardwareStatus()
                delay(1000) // 1秒更新间隔
            }
        }

        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.surfaceContainer
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHighest
                )
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // 标题
                    Text(
                        "设备状态",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(bottom = 24.dp)
                    )

                    // CPU & GPU 行
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        // CPU 仪表
                        ChipProgress(
                            progress = hardwareState.value.cpuUsage,
                            color = MaterialTheme.colorScheme.primary,
                            icon = Icons.Outlined.Memory,
                            label = "CPU"
                        )

                        // GPU 仪表
                        ChipProgress(
                            progress = hardwareState.value.gpuUsage,
                            color = MaterialTheme.colorScheme.secondary,
                            icon = Icons.Outlined.Hardware,
                            label = "GPU"
                        )
                    }

                    Spacer(modifier = Modifier.height(32.dp))

                    // 内存仪表（创意设计）
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.Start
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(bottom = 8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.SdStorage,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.tertiary,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                "内存使用",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(start = 8.dp)
                            )
                        }

                        // 波浪进度效果
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.surfaceContainerLow)
                        ) {
                            // 渐变背景条
                            LinearProgressIndicator(
                                progress = 1f,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .alpha(0.2f),
                                color = MaterialTheme.colorScheme.tertiary
                            )

                            // 动态进度条
                            LinearProgressIndicator(
                                progress = hardwareState.value.memoryUsage,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(4.dp),
                                trackColor = Color.Transparent,
                                color = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.8f),
                                strokeCap = StrokeCap.Round
                            )

                            // 内存使用数据
                            Row(
                                modifier = Modifier.fillMaxSize(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    "${(hardwareState.value.memoryUsage * hardwareState.value.totalMemory).toInt()} GB 使用",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.padding(start = 16.dp)
                                )
                                Text(
                                    "${hardwareState.value.totalMemory - (hardwareState.value.memoryUsage * hardwareState.value.totalMemory).toInt()} GB 可用",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.padding(end = 16.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    @Composable
    private fun ChipProgress(
        progress: Float,
        color: Color,
        icon: ImageVector,
        label: String
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.width(120.dp)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(100.dp)
            ) {
                // 背景环
                CircularProgressIndicator(
                    progress = 1f,
                    modifier = Modifier.size(100.dp),
                    color = color.copy(alpha = 0.2f),
                    strokeWidth = 8.dp
                )

                // 动态进度环
                CircularProgressIndicator(
                    progress = progress,
                    modifier = Modifier.size(100.dp),
                    color = color,
                    strokeWidth = 8.dp,
                    strokeCap = StrokeCap.Round
                )

                // 中央内容
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = color,
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        "${(progress * 100).toInt()}%",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        label,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }

    private fun fetchHardwareStatus(): HardwareState {

            val memInfo = Runtime.getRuntime()
                .exec("su -c \"free | grep Mem\"")
                .inputStream.bufferedReader().use {
                    it.readText().trim().split(Regex("\\s+")).let { parts ->
                        val total = parts[1].toInt()
                        val used = parts[2].toInt()
                        Pair(used.toFloat() / total, total / 1024) // 转换为GB
                    }
                }

            return HardwareState(
                cpuUsage = 1f,
                gpuUsage = 1f,
                memoryUsage = memInfo.first,
                totalMemory = memInfo.second
            )
    }
}