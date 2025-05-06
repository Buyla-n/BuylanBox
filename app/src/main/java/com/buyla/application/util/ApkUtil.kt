package com.buyla.application.util

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Android
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.buyla.application.R
import com.buyla.application.util.FileUtil.onFileClick
import com.buyla.application.util.Util.copyToClipboard
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
            OutlinedButton(
                modifier = Modifier.padding(horizontal = 4.dp),
                onClick = {
                    onFileClick(
                        context = context,
                        filePath = filePath,
                        type = "zip",
                        onNull = {  },
                        onApk = {  }
                    )
                },
            ) {
                Text("分解")
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
        manager: Boolean = false
    ){
        val context = LocalContext.current
        AlertDialog(
            onDismissRequest = { onCancel() },
            title = { Text("安装包信息") },
            text = {
                Column(
                    modifier = Modifier.padding(8.dp)
                ) {
                    val apkInfo = getApkInfo(context, File(filePath.toString()), manager)

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        apkInfo?.icon?.let { bitmap ->
                            Image(
                                painter = BitmapPainter(bitmap),
                                contentDescription = "应用图标",
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .padding(bottom = 12.dp)
                            )
                        } ?:
                        Column {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    //.padding(bottom = 12.dp)
                                    .background(
                                        color = MaterialTheme.colorScheme.surfaceVariant,
                                        shape = MaterialTheme.shapes.medium
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Android,
                                    contentDescription = "默认图标",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            Spacer(Modifier.padding(vertical = 6.dp))
                        }
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
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .padding(16.dp)
                        ) {
                            InfoRow("版本名称", it.versionName)
                            InfoRow("版本号", it.versionCode.toString())
                            InfoRow("安装状态", if (it.isInstalled) "已安装" else "未安装")
                            InfoRow("安装包", it.place)
                        }
                    } ?: Text("无法读取APK信息", color = MaterialTheme.colorScheme.error)
                }
            },
            confirmButton = {
                    buttonCustom()
            },
            dismissButton = {
                TextButton(
                    onClick = { onCancel() }
                ) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    @Composable
    fun InfoRow(label: String, value: String) {
        val context = LocalContext.current
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(end = 48.dp)
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

    private fun getApkInfo(context: Context, apkFile: File, manager: Boolean): ApkInfo? {
        return try {
            val pm = context.packageManager

            val packageInfo = pm.getPackageArchiveInfo(
                apkFile.absolutePath,
                PackageManager.GET_ACTIVITIES
            ) ?: return null

            packageInfo.applicationInfo!!.sourceDir = apkFile.absolutePath
            packageInfo.applicationInfo!!.publicSourceDir = apkFile.absolutePath

            fun drawableToImageBitmap(drawable: Drawable): ImageBitmap {

                val bitmap = if (drawable is BitmapDrawable) {
                    drawable.bitmap
                } else {
                    // 如果是 VectorDrawable 或其他类型的 Drawable
                    val bitmap = Bitmap.createBitmap(
                        drawable.intrinsicWidth,
                        drawable.intrinsicHeight,
                        Bitmap.Config.ARGB_8888
                    )
                    val canvas = Canvas(bitmap)
                    drawable.setBounds(0, 0, canvas.width, canvas.height)
                    drawable.draw(canvas)
                    bitmap
                }
                return bitmap.asImageBitmap()
            }

            val isInstalled = try {
                pm.getPackageInfo(packageInfo.packageName, 0)
                true
            } catch (e: PackageManager.NameNotFoundException) {
                false
            }

            val icon = try {
                if (manager) {
                    val resources = pm.getResourcesForApplication(packageInfo.applicationInfo!!)
                    val iconId = packageInfo.applicationInfo!!.icon
                    if (iconId != 0) {
                        val drawable = resources.getDrawable(iconId, null)
                        drawableToImageBitmap(drawable)
                    } else {
                        null
                    }
                } else {
                    val drawable = pm.getApplicationIcon(packageInfo.packageName)
                    drawableToImageBitmap(drawable)
                }
            } catch (e: Exception) {
                null
            }

            val enabled = try {
                packageInfo.applicationInfo!!.enabled
            } catch (e: Exception) {
                false
            }

            val appName = packageInfo.applicationInfo!!.loadLabel(pm).toString()
                .takeIf { it.isNotEmpty() }
                ?: packageInfo.packageName

            val place = try {
                getInstalledApkPath(context, packageInfo.packageName).toString()
            } catch (e: Exception) {
                //
            }

            ApkInfo(
                packageName = packageInfo.packageName,
                versionCode = packageInfo.longVersionCode.toInt(),
                versionName = packageInfo.versionName!!,
                appName = appName,
                icon = icon,
                isInstalled = isInstalled,
                place = place.toString(),
                enabled = enabled
            )
        } catch (e: Exception) {
            ApkInfo()
        }
    }

    data class ApkInfo(
        val packageName: String = "未知包名",
        val versionCode: Int = 0,
        val versionName: String = "未知版本",
        val appName: String = "未知应用",
        val icon: ImageBitmap? = null,
        val isInstalled: Boolean = false,
        val place: String = "未知路径",
        val enabled: Boolean = false,
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
                    data = Uri.parse("package:${context.packageName}")
                }
            )
            return
        }

        try {
            context.startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(context, "未找到安装程序", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(context, "安装失败: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    fun getInstalledApkPath(context: Context, packageName: String): Path {
        try {
            // 获取PackageManager实例
            val packageManager = context.packageManager
            // 通过包名获取ApplicationInfo
            val applicationInfo: ApplicationInfo = packageManager.getApplicationInfo(packageName, 0)
            // 获取APK路径
            return Paths.get(applicationInfo.sourceDir)
        } catch (e: PackageManager.NameNotFoundException) {
            e.printStackTrace()
        }
        return Paths.get("null")
    }

}