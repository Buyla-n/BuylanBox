package com.buyla.application.ui.screen

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import android.system.Os
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.SdStorage
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.io.BufferedReader
import java.io.InputStreamReader

object Info {

    @SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
    @OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
    @Composable
    fun InfoScreen() {
        val context = LocalContext.current
        val infos = remember {
            listOf(
                InfoItem(
                    title = "机型代号",
                    status = Build.DEVICE,
                    icon = Icons.Filled.PhoneAndroid
                ),
                InfoItem(
                    title = "系统指纹",
                    status = Build.FINGERPRINT,
                    icon = Icons.Filled.Fingerprint
                ),
                InfoItem(
                    title = "内核版本",
                    status = Os.uname().release,
                    icon = Icons.Filled.Memory
                ),
                InfoItem(
                    title = "SE Linux 状态",
                    status = getSELinuxStatus(),
                    icon = Icons.Filled.Security
                ),
                InfoItem(
                    title = "分区格式",
                    status = getPartitionFormat(),
                    icon = Icons.Filled.Storage
                ),
                InfoItem(
                    title = "动态分区状态",
                    status = getDynamicPartitionStatus(),
                    icon = Icons.Filled.SdStorage
                ),
                InfoItem(
                    title = "CPU 架构",
                    status = Build.SUPPORTED_ABIS.firstOrNull() ?: "未知",
                    icon = Icons.Filled.Computer
                ),
                InfoItem(
                    title = "应用版本",
                    status = getAppVersion(context),
                    icon = Icons.Filled.Apps
                ),
                InfoItem(
                    title = "制造商",
                    status = Build.MANUFACTURER,
                    icon = Icons.Filled.Business
                ),
            )
        }

        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    title = {
                        Text(
                            text = "系统信息",
                            style = MaterialTheme.typography.headlineSmall.copy(
                                fontWeight = FontWeight.Bold
                            ),
                            modifier = Modifier.fillMaxWidth().padding(start = 8.dp)
                        )
                    }
                )
            },
        ) { innerPadding ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = innerPadding.calculateTopPadding())
                    .padding(vertical = 8.dp)
            ) {
                items(infos) { info ->
                    InfoCard(info)
                }
            }
        }
    }

    @Composable
    fun InfoCard(info: InfoItem) {
        Card(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 4.dp)
                .fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer
            ),
            shape = MaterialTheme.shapes.medium,
         //   elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Row(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = info.icon,
                    contentDescription = info.title,
                    modifier = Modifier
                        .size(32.dp)
                        .padding(end = 16.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Column {
                    Text(
                        text = info.title,
                        style = MaterialTheme.typography.labelMedium.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                    Text(
                        text = info.status,
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontWeight = FontWeight.Medium
                        ),
                        modifier = Modifier.padding(top = 4.dp).fillMaxWidth()
                    )
                }
            }
        }
    }

    private fun getSELinuxStatus(): String {
        return try {
            val enforced = android.os.StrictMode.VmPolicy.Builder().build() != null
            if (enforced) "强制" else "关闭 / 宽容"
        } catch (_: Exception) {
            "未知"
        }
    }

    private fun getPartitionFormat(): String {
        return when {
            isABEnabled() -> "AB / VAB"
            else -> "Only A"
        }
    }

    private fun getDynamicPartitionStatus(): String {
        val result = executeCommand("getprop ro.boot.dynamic_partitions")
        return if (result == "true") {
            "启用"
        } else {
            "关闭"
        }
    }

    private fun getAppVersion(context: Context): String {
        return try {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            packageInfo.versionName ?: "未知"
        } catch (_: Exception) {
            "未知"
        }
    }

    data class InfoItem(
        val title: String,
        val status: String,
        val icon: ImageVector
    )

    private fun isABEnabled(): Boolean {
        val result = executeCommand("getprop ro.build.ab_update")
        return result == "true"
    }

    private fun executeCommand(command: String): String {
        return try {
            val process = Runtime.getRuntime().exec(command)
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            val result = reader.readText().trim()
            reader.close()
            process.waitFor()
            result
        } catch (e: Exception) {
            "Error: ${e.message}"
        }
    }

}