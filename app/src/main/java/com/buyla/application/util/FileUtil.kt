package com.buyla.application.util

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Environment
import android.text.format.Formatter.formatFileSize
import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.InsertDriveFile
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.rounded.Android
import androidx.compose.material.icons.rounded.AudioFile
import androidx.compose.material.icons.rounded.DateRange
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.FolderZip
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Storage
import androidx.compose.material.icons.rounded.VideoFile
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.VerticalDivider
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.buyla.application.R
import com.buyla.application.activity.AudioPlayer
import com.buyla.application.activity.ImagePlayer
import com.buyla.application.activity.TextEditor
import com.buyla.application.activity.VideoPlayer
import com.buyla.application.util.Util.fileVertical
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.lingala.zip4j.ZipFile
import net.lingala.zip4j.model.FileHeader
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.io.path.Path
import kotlin.io.path.isDirectory
import kotlin.io.path.name

object FileUtil {

    var sortSelectedIndex by mutableIntStateOf(0)
    var leftPath: Path by mutableStateOf(Paths.get(Environment.getExternalStorageDirectory().toString()))
    var rightPath: Path by mutableStateOf(Paths.get(Environment.getExternalStorageDirectory().toString()))
    var rightPathInside: String by mutableStateOf("")
    var leftPathInside: String by mutableStateOf("")
    var rightFileInside: List<FileHeader> by mutableStateOf(listOf())
    var leftFileInside: List<FileHeader> by mutableStateOf(listOf())
    var rightInside: Boolean by mutableStateOf(false)
    var leftInside: Boolean by mutableStateOf(false)
    var pathState: String by mutableStateOf("left")
    var rightFileName : String by mutableStateOf("")
    var leftFileName : String by mutableStateOf("")

    fun getFileType(filePath: String): String {
        val file = File(filePath)
        return when {
            file.isDirectory -> "folder"
            file.extension in listOf("txt", "log", "json", "conf", "html", "md", "cfg", "rc") -> "txt"
            file.extension in listOf("zip", "gz") -> "zip"
            file.extension in listOf("jpg", "jpeg", "png", "webp") -> "image"
            file.extension.equals("mp4", ignoreCase = true) -> "video"
            file.extension in listOf("mp3", "wav", "m4a") -> "audio"
            file.extension in listOf("apk", "apks", "apex") -> "apk"
            file.extension.equals("xml", ignoreCase = true) -> "xml"
            else -> "null"
        }
    }

    fun getZipFileType(file: String): String {
        val name = file
        return when {
            name.endsWith("/") -> "folder"
            listOf("txt", "log", "json", "conf", "html", "md", "cfg", "rc").any { name.endsWith(it) } -> "txt"
            listOf("zip", "gz").any { name.endsWith(it) } -> "zip"
            listOf("jpg", "jpeg", "png", "webp").any { name.endsWith(it) } -> "image"
            name.endsWith("mp4", ignoreCase = true) -> "video"
            listOf("mp3", "wav", "m4a").any { name.endsWith(it) } -> "audio"
            listOf("apk", "apks", "apex").any { name.endsWith(it) } -> "apk"
            name.endsWith("xml", ignoreCase = true) -> "xml"
            else -> "null"
        }
    }

    @Composable
    fun getFileIcon(type : String): ImageVector {
        return when (type) {
            "folder" -> Icons.Rounded.Folder
            "txt" -> Icons.Rounded.Description
            "xml" -> Icons.Rounded.Description
            "zip" -> Icons.Rounded.FolderZip
            "image" -> Icons.Rounded.Image
            "video" -> Icons.Rounded.VideoFile
            "audio" -> Icons.Rounded.AudioFile
            "apk" -> Icons.Rounded.Android
            else -> Icons.AutoMirrored.Rounded.InsertDriveFile
        }
    }

    fun onFileClick(
        context: Context,
        filePath: String,
        type: String,
        onNull: () -> Unit,
        onApk: () -> Unit
        ) {
        when (type) {
            "folder" -> {
                when (pathState) {
                    "left" -> {
                        if (!leftInside) {
                            leftPath = Path(filePath)
                        } else {
                            leftPathInside = filePath
                        }
                    }
                    "right" -> {
                        if (!rightInside) {
                            rightPath = Path(filePath)
                        } else {
                            rightPathInside = filePath
                        }
                    }
                }
            }
            "txt" -> { openActivity(context, filePath, TextEditor::class.java) }
            "image" -> { openActivity(context, filePath, ImagePlayer::class.java) }
            "video" -> { openActivity(context, filePath, VideoPlayer::class.java) }
            "audio" -> { openActivity(context, filePath, AudioPlayer::class.java) }
            "outside" -> {
                context.startActivity(
                    Intent(Intent.ACTION_VIEW).setDataAndType(
                        FileProvider.getUriForFile(
                            context,
                            "${context.packageName}.fileProvider",
                            File(filePath)
                        ), "*/*"
                    ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                )
            }
            "null" -> { onNull() }
            "apk" -> { onApk() }
            "xml" -> {
                openActivity(context, filePath, TextEditor::class.java)
            }
            "zip" -> {
                when (pathState) {
                    "left" -> {
                        leftFileName = Path(filePath).name
                        leftInside = true
                        leftPathInside = ""
                    }
                    "right" -> {
                        rightFileName = Path(filePath).name
                        rightInside = true
                        rightPathInside = ""
                    }
                }
                ZipFile(filePath).use { zip ->
                    if (pathState == "left") leftFileInside = zip.fileHeaders.toList() else rightFileInside = zip.fileHeaders.toList()
                }
            }
        }
    }

    fun completeDirectoriesFromHeaders(zipHeaders: List<FileHeader>): List<FileHeader> {
        val allEntries = mutableSetOf<FileHeader>() // 存储所有文件和目录
        val directories = mutableSetOf<String>() // 存储补全的目录（以字符串形式表示路径）

        // 遍历所有文件头
        zipHeaders.forEach { header ->
            allEntries.add(header) // 添加文件或显式声明的目录

            // 递归提取父目录
            var currentPath = header.fileName // 假设 FileHeader 有 fileName 属性
            while (currentPath.contains("/")) {
                currentPath = currentPath.substringBeforeLast("/")
                directories.add("$currentPath/") // 确保目录以 "/" 结尾
            }
        }

        // 转换补全的目录为虚拟 FileHeader
        val directoryHeaders = directories.map { dirPath ->
            val directoryHeader = FileHeader() // 创建空的 FileHeader
            directoryHeader.fileName = dirPath // 设置目录名称
            directoryHeader
        }

        // 合并显式文件头和补全的目录
        allEntries.addAll(directoryHeaders)

        // 返回排序后的结果
        return allEntries.toList()
    }

    private fun <T : Activity> openActivity(context: Context, filePath: String, activityClass: Class<T>) {
        context.startActivity(
            Intent(context, activityClass).apply {
                putExtra("filePath", filePath)
            }
        )
    }

    fun copyFile(source: Path, target: Path) {
        val newTarget = if (Files.exists(target)) {
            val (base, ext) = target.fileName.toString().run {
                val dotIndex = lastIndexOf('.')
                if (dotIndex > 0 && !source.isDirectory()) substring(0, dotIndex) to substring(dotIndex) else this to ""
            }
            generateSequence(1) { it + 1 }
                .map { target.parent.resolve("$base ($it)$ext") }
                .first { !Files.exists(it) }
        } else target

        try {
            Files.copy(source, newTarget)
        } catch (e: Exception) {
            println("复制文件时发生错误: ${e.message}")
        }
    }

    private fun moveFile(source : Path, target : Path) {
        val newTarget = if (Files.exists(target)) {
            val (base, ext) = target.fileName.toString().run {
                val dotIndex = lastIndexOf('.')
                if (dotIndex > 0 && !source.isDirectory()) substring(0, dotIndex) to substring(dotIndex) else this to ""
            }
            generateSequence(1) { it + 1 }
                .map { target.parent.resolve("$base ($it)$ext") }
                .first { !Files.exists(it) }
        } else target

        try {
            Files.move(source, newTarget)
        } catch (e: Exception) {
            println("移动文件时发生错误: ${e.message}")
        }
    }

    private fun deleteFile(
        target: Path
    ) {
        try {
            Files.delete(target)
        } catch (e: Exception) {
            println("删除文件时发生错误: ${e.message}")
        }
    }

    private fun shareFile(context: Context, filePath: String) {
        val file = File(filePath)
        if (!file.exists()) {
            Toast.makeText(context, "文件不存在", Toast.LENGTH_SHORT).show()
            return
        }

        val shareIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_STREAM, FileProvider.getUriForFile(context, "${context.packageName}.fileProvider", file))
            type = "application/octet-stream"
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
        }

        context.startActivity(Intent.createChooser(shareIntent, "分享文件"))
    }

    @OptIn(ExperimentalFoundationApi::class)
    @Composable
    fun FileParentItem(
        onClick: () -> Unit,
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = fileVertical.dp)
                .fillMaxWidth()
                .height(48.dp)
                .background(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = MaterialTheme.shapes.small
                )
                .combinedClickable(onClick = {
                    onClick()
                }, indication = null, interactionSource = null),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Filled.ArrowUpward,
                contentDescription = null,
                modifier = Modifier.padding(start = 8.dp),
                tint = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Text(
                text = "..",
                fontSize = 12.sp,
                modifier = Modifier . padding (horizontal = 16.dp),
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }

    @Composable
    fun ChooseDialog(
        onCancel: () -> Unit,
        context: Context,
        filePath: Path,
        onApk: () -> Unit
    ) {
        AlertDialog(
            onDismissRequest = { onCancel() },
            title = { Text("打开方式") },
            text = {
                @Composable
                fun ChooseButton(
                    label: String,
                    type: String
                ) {
                    Button(
                        modifier = Modifier
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                            .fillMaxWidth(),
                        shape = MaterialTheme.shapes.large,
                        onClick = {
                            onCancel()
                            onFileClick(
                                context,
                                filePath.toString(),
                                type,
                                {}, { onApk() }
                            )
                        }
                    ) {
                        Text(label)
                    }
                }
                Row {
                    Column(
                        modifier = Modifier.weight(0.5f)
                    ) {
                        ChooseButton("视频", "video")
                        ChooseButton("图片", "image")
                        ChooseButton("文本", "txt")
                        ChooseButton("安装包", "apk")
                    }

                    VerticalDivider(modifier = Modifier.height(230.dp))

                    Column(
                        modifier = Modifier.weight(0.5f)
                    ) {
                        ChooseButton("音频", "video")
                        ChooseButton("button", "null")
                        ChooseButton("button", "null")
                        ChooseButton("外部链接", "outside")
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { onCancel() }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    @Composable
    fun OperateDialog(
        filePath: Path,
        type: String,
        context: Context,
        onCancel: () -> Unit,
        chooseDialog: () -> Unit,
        fileInfoDialog: () -> Unit,
        renameDialog: () -> Unit
    ) {
        val file = filePath.fileName
        AlertDialog(
            onDismissRequest = { onCancel() },
            text = {
                @Composable
                fun OperateButton(
                    buttonText: String,
                    enabled: Boolean = true,
                    onClick: () -> Unit,
                ) {
                    OutlinedButton(
                        modifier = Modifier
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                            .fillMaxWidth(),
                        shape = MaterialTheme.shapes.large,
                        onClick = {
                            onCancel()
                            onClick()
                        },
                        enabled = enabled
                    ) {
                        Text(buttonText)
                    }
                }
                Row {
                    Column(
                        modifier = Modifier.weight(0.5f)
                    ) {
                        OperateButton(
                            when (pathState) {
                                "left" -> "复制 >"
                                else -> "< 复制"
                            }
                        ) {
                                copyFile(
                                    filePath,
                                    if (pathState == "left") {
                                        File(rightPath.toString() + File.separator + file).toPath()
                                    } else {
                                        File(leftPath.toString() + File.separator + file).toPath()
                                    }
                                )
                        }
                        OperateButton("打开方式", type != "folder") { chooseDialog() }
                        OperateButton("重命名") { renameDialog() }
                        OperateButton("属性") { fileInfoDialog() }
                    }

                    VerticalDivider(modifier = Modifier.height(230.dp))

                    Column(
                        modifier = Modifier.weight(0.5f)
                    ) {
                        OperateButton(
                            when (pathState) {
                                "left" -> "移动 >"
                                else -> "< 移动"
                            }
                        ) {
                            moveFile(
                                filePath,
                                if (pathState == "left") {
                                    File(rightPath.toString() + File.separator + file).toPath()
                                } else {
                                    File(leftPath.toString() + File.separator + file).toPath()
                                }
                            )
                        }
                        OperateButton("删除") { deleteFile(filePath) }
                        OperateButton("分享") {
                            shareFile(
                                context = context,
                                filePath.toString()
                            )
                        }
                        OperateButton("取消") {
                            onCancel()
                        }
                    }
                }
            },
            confirmButton = {
            },
            dismissButton = {

            },
        )
    }

    @Composable
    fun RenameFileDialog(
        filePath : Path,
        onCancel : () -> Unit
    ) {
        var textFieldValue by remember { mutableStateOf(filePath.fileName.toString()) }
        AlertDialog(
            onDismissRequest = { onCancel() },
            title = { Text("命名") },
            text = {
                OutlinedTextField(
                    value = textFieldValue,
                    onValueChange = { textFieldValue = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                )
            },
            confirmButton = {
                Button(onClick = {
                    onCancel()
                    val sourceFile = File(filePath.toString())
                    val targetFile =
                        File(filePath.parent.toString() + File.separator + textFieldValue)
                    sourceFile.renameTo(targetFile)

                }) {
                    Text(stringResource(R.string.confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { onCancel() }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun FileInfoDialog(
        filePath : Path,
        onCancel : () -> Unit
    ) {
        val context = LocalContext.current
        var fileSize by remember { mutableStateOf<String?>(null) }
        var fileLastModified by remember { mutableStateOf<String?>(null) }
        LaunchedEffect(filePath) {
            withContext(Dispatchers.IO) {
                try {
                    withContext(Dispatchers.IO) {
                        fileLastModified = Files.getLastModifiedTime(filePath).toInstant()
                            .atZone(ZoneId.systemDefault())
                            .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
                    }
                    withContext(Dispatchers.IO) {
                        fileSize = formatFileSize(context, Files.size(filePath))
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }

        @Composable
        fun FileInfoItem(
            icon: ImageVector,
            label: String,
            value: String,
            onClick: (() -> Unit)? = null
        ) {
            val interactionSource = remember { MutableInteractionSource() }

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(
                        interactionSource, LocalIndication.current, onClick != null,
                        onClick = { onClick?.invoke() }
                    ),
                color = Color.Transparent,
                shape = MaterialTheme.shapes.medium
            ) {
                Row(
                    modifier = Modifier
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = label,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = value,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
        var showDatePicker by remember { mutableStateOf(false) }
        var selectedDate by remember { mutableStateOf<Long?>(null) }

        AlertDialog(
            onDismissRequest = { onCancel() },
            title = {
                Text(
                    text = "属性",
                    style = MaterialTheme.typography.headlineSmall
                )
            },
            text = {
                Column {
                    FileInfoItem(
                        Icons.Rounded.Description, "文件名称", filePath.fileName.toString()
                    )

                    FileInfoItem(
                        Icons.Rounded.DateRange, "修改日期", fileLastModified ?: "未知"
                    ) { showDatePicker = true }

                    FileInfoItem(
                        Icons.Rounded.Folder, "文件路径", filePath.toString()
                    )

                    FileInfoItem(
                        Icons.Rounded.Info, "文件类型", getFileType(filePath.toString())
                    )

                    FileInfoItem(
                        Icons.Rounded.Storage, "文件大小", fileSize ?: "未知"
                    )
                }
                if (showDatePicker) {
                    val datePickerState = rememberDatePickerState()

                    DatePickerDialog(
                        onDismissRequest = { showDatePicker = false },
                        confirmButton = {
                            TextButton(onClick = {
                                selectedDate = datePickerState.selectedDateMillis
                                showDatePicker = false
                            }) {
                                Text(stringResource(R.string.confirm))
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { showDatePicker = false }) {
                                Text(stringResource(R.string.cancel))
                            }
                        }
                    ) {
                        DatePicker(state = datePickerState)
                    }
                }
            },
            confirmButton = {
                Button(onClick = { onCancel() }) {
                    Text(stringResource(R.string.confirm))
                }
            }
        )
    }
}
