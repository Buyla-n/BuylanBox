package com.buyla.application.util

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.provider.Settings
import android.widget.Toast
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Android
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.FileProvider
import androidx.core.graphics.drawable.toBitmap
import androidx.core.net.toUri
import com.buyla.application.R
import com.buyla.application.util.FileUtil.onFileClick
import com.buyla.application.util.Util.copyToClipboard
import kotlinx.coroutines.launch
import java.io.File
import java.nio.file.Path
import java.nio.file.Paths

object ApkUtil {
    @Composable
    fun ApkInfoDialog(
        filePath : Path,
        onCancel: () -> Unit,
        context: Context,
        buttonCustom: @Composable () -> Unit = {
            FilledTonalButton(
                modifier = Modifier.padding(horizontal = 4.dp),
                onClick = {
                    onFileClick(
                        context = context,
                        filePath = filePath.toString(),
                        type = "zip",
                        onNull = {  },
                        onApk = {  }
                    )
                },
            ) {
                Text("查看")
            }
            Button(
                modifier = Modifier
                    .padding(horizontal = 4.dp),
                onClick = {
                    installApk(context, filePath)
                }
            ) {
                Text("安装")
            }
        },
        findByName: Boolean = false,
        packageName : String = ""
    ){
        val context = LocalContext.current
        val scale = remember { Animatable(0.8f) } // 初始缩放值
        val offsetY = remember { Animatable(100f) }

        // 启动动画（进入时）
        LaunchedEffect(Unit) {
            launch {
                offsetY.animateTo(
                    targetValue = 0f,
                    animationSpec = tween(durationMillis = 300)
                )
            }
            launch {
                scale.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(durationMillis = 300)
                )
            }
        }
        AlertDialog(
            onDismissRequest = {
                onCancel()
            },
            properties = DialogProperties( // 禁用默认动画
                dismissOnBackPress = true,
                dismissOnClickOutside = true,
                usePlatformDefaultWidth = true
            ),
            modifier = Modifier
                .graphicsLayer {
                    scaleX = scale.value
                    scaleY = scale.value
                    translationY = offsetY.value
                }
                .animateContentSize(),
            title = { Text("安装包") },
            text = {
                Column(
                    modifier = Modifier.padding(8.dp)
                ) {
                    val apkInfo = getApkInfo(context, File(filePath.toString()), findByName, packageName = packageName)

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        apkInfo?.icon?.let { bitmap ->
                            Box(
                                modifier = Modifier
                                    .padding(bottom = 16.dp)
                                    .size(42.dp)
                            ) {
                                Image(
                                    painter = BitmapPainter(bitmap),
                                    contentDescription = "应用图标",
                                    modifier = Modifier
                                        .clip(CircleShape)
                                )
                            }
                        } ?: Box(
                            modifier = Modifier
                                .padding(bottom = 16.dp)
                                .size(42.dp)
                                .background(
                                    color = MaterialTheme.colorScheme.primary,
                                    shape = CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Android,
                                contentDescription = "默认图标",
                                tint = MaterialTheme.colorScheme.surface,
                            )
                        }
                        Spacer(Modifier.padding(vertical = 6.dp))
                        apkInfo?.let {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(bottom = 16.dp)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .padding(start = 16.dp)
                                        .weight(1f)
                                ) {
                                    Text(
                                        text = it.appName,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = it.packageName,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }

                    apkInfo?.let {
                        LazyVerticalGrid(
                            modifier = Modifier
                                .fillMaxWidth(),
                            columns = GridCells.Fixed(2),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            item {InfoCard("版本", "${it.versionName} (${it.versionCode})") }
                            item {InfoCard("安装", if (it.isInstalled) "已安装" else "未安装")}
                            if (it.isInstalled) item {InfoCard("位置", it.place)}
                            if (it.isInstalled) item {InfoCard("数据", it.dataPath)}
                        }
                    } ?: Text("无法读取APK信息", color = MaterialTheme.colorScheme.error)
                }
            },
            confirmButton = {
                buttonCustom()
            },
            dismissButton = {
                FilledTonalButton(
                    onClick = { onCancel() },
                    modifier = Modifier.padding(end = 4.dp)
                ) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    @Composable
    fun InfoCard(label: String, value: String) {
        val context = LocalContext.current
        Column(
            modifier = Modifier
                .background(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = MaterialTheme.shapes.large
                )
                .padding(16.dp)
                .height(42.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(end = 48.dp),
                maxLines = 1
            )

            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier
                    .clickable {
                        copyToClipboard(context, value)
                    },
                maxLines = 1, // 限制为单行
                overflow = TextOverflow.Ellipsis
            )
        }
    }

    private fun getApkInfo(context: Context, apkPath: File, findByName: Boolean, packageName: String = ""): ApkInfo? {
        val packageManager = context.packageManager

        if (!findByName) {
            val packageInfo = packageManager.getPackageArchiveInfo(apkPath.absolutePath, PackageManager.GET_ACTIVITIES) ?: return null
            packageInfo.applicationInfo!!.sourceDir = apkPath.absolutePath
            packageInfo.applicationInfo!!.publicSourceDir = apkPath.absolutePath
            val isInstalled = try { packageManager.getPackageInfo(packageInfo.packageName, 0).firstInstallTime > 1 } catch (e: PackageManager.NameNotFoundException) { false }
            val icon = try {
                val iconId = packageInfo.applicationInfo!!.icon
                if (iconId != 0) { packageManager.getResourcesForApplication(packageInfo.applicationInfo!!).getDrawable(iconId, null).toBitmap().asImageBitmap() } else { null }
            } catch (_: Exception) {
                null
            }
            val enabled = false
            val appName = packageInfo.applicationInfo!!.loadLabel(packageManager).toString()
            val place = if (isInstalled) packageManager.getApplicationInfo(packageInfo.packageName, 0).sourceDir ?: "" else ""
            val dataPath = if (isInstalled) packageManager.getApplicationInfo(packageInfo.packageName, 0).dataDir ?: "" else ""
            return ApkInfo(
                packageName = packageInfo.packageName,
                versionCode = packageInfo.longVersionCode.toInt(),
                versionName = packageInfo.versionName.toString(),
                appName = appName,
                icon = icon,
                isInstalled = isInstalled,
                place = place.toString(),
                enabled = enabled,
                dataPath = dataPath
            )
        } else {
            val packageInfo = packageManager.getPackageInfo(packageName, 0)
            val isInstalled = packageInfo.firstInstallTime > 1
            val icon = packageInfo.applicationInfo?.loadIcon(packageManager)!!
            val enabled = false
            val appName = packageInfo.applicationInfo?.loadLabel(packageManager)
            val place = packageManager.getApplicationInfo(packageName, 0).sourceDir
            val dataPath = packageInfo.applicationInfo?.dataDir.toString()
            return ApkInfo(
                packageName = packageInfo.packageName,
                versionCode = packageInfo.longVersionCode.toInt(),
                versionName = packageInfo.versionName!!,
                appName = appName.toString(),
                icon = icon.toBitmap().asImageBitmap(),
                isInstalled = isInstalled,
                place = place.toString(),
                enabled = enabled,
                dataPath = dataPath
            )
        }
    }

    data class ApkInfo(
        val packageName: String = "",
        val versionCode: Int = 0,
        val versionName: String = "",
        val appName: String = "",
        val icon: ImageBitmap? = null,
        val isInstalled: Boolean = false,
        val place: String = "",
        val enabled: Boolean = false,
        val dataPath: String = ""
    )

    fun installApk(context: Context, apkFile: Path) {
        val intent = Intent(Intent.ACTION_VIEW).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)

            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileProvider",
                File(apkFile.toString())
            )

            setDataAndType(uri, "application/vnd.android.package-archive")
        }
        if (!context.packageManager.canRequestPackageInstalls()) {
            context.startActivity(
                Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                    data = "package:${context.packageName}".toUri()
                }
            )
            return
        }

        try {
            context.startActivity(intent)
        } catch (_: ActivityNotFoundException) {
            Toast.makeText(context, "未找到安装程序", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(context, "安装失败: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    fun getInstalledApkPath(context: Context, packageName: String): Path {
        try {
            val packageManager = context.packageManager
            val applicationInfo: ApplicationInfo = packageManager.getApplicationInfo(packageName, 0)
            return Paths.get(applicationInfo.sourceDir)
        } catch (e: PackageManager.NameNotFoundException) {
            e.printStackTrace()
        }
        return Paths.get("null")
    }

}