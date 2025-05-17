package com.buyla.application.util

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.text.format.Formatter.formatFileSize
import android.widget.Toast
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.InsertDriveFile
import androidx.compose.material.icons.rounded.Android
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.AudioFile
import androidx.compose.material.icons.rounded.DateRange
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.FolderZip
import androidx.compose.material.icons.rounded.FontDownload
import androidx.compose.material.icons.rounded.Fullscreen
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.RepeatOn
import androidx.compose.material.icons.rounded.Storage
import androidx.compose.material.icons.rounded.VideoFile
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
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
import androidx.compose.ui.window.Dialog
import androidx.core.content.FileProvider
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import com.buyla.application.R
import com.buyla.application.activity.AudioPlayer
import com.buyla.application.activity.FontPreview
import com.buyla.application.activity.ImagePlayer
import com.buyla.application.activity.TextEditor
import com.buyla.application.activity.VideoPlayer
import com.buyla.application.util.Util.fileVertical
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
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
    var rightPathInside by mutableStateOf("")
    var leftPathInside by mutableStateOf("")
    var rightFileInside: List<FileHeader> by mutableStateOf(listOf())
    var leftFileInside: List<FileHeader> by mutableStateOf(listOf())
    var rightInside by mutableStateOf(false)
    var leftInside by mutableStateOf(false)
    var pathState by mutableStateOf("left")
    var rightFileName by mutableStateOf("")
    var leftFileName by mutableStateOf("")

    fun getFileType(filePath: String): String {
        val file = File(filePath)
        return when {
            file.isDirectory -> "folder"
            file.extension in listOf("txt", "log", "json", "conf", "html", "md", "cfg", "rc") -> "txt"
            file.extension in listOf("zip", "gz") -> "zip"
            file.extension in listOf("jpg", "jpeg", "png", "webp") -> "image"
            file.extension.equals("mp4", ignoreCase = true) -> "video"
            file.extension in listOf("mp3", "wav", "m4a", "ogg") -> "audio"
            file.extension in listOf("apk", "apks", "apex") -> "apk"
            file.extension in listOf("ttf", "otf", "ttc") -> "font"
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
            listOf("ttf", "otf", "ttc").any { name.endsWith(it) } -> "font"
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
            "font" -> Icons.Rounded.FontDownload
            else -> Icons.AutoMirrored.Rounded.InsertDriveFile
        }
    }

    fun onFileClick(
        context: Context,
        filePath: String,
        type: String,
        onNull: () -> Unit,
        onApk: () -> Unit,
        onMusic: () -> Unit = {}
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
            "audio" -> { onMusic() }
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
            "font" -> {
                openActivity(context, filePath, FontPreview::class.java)
            }
        }
    }

    fun completeDirectoriesFromHeaders(zipHeaders: List<FileHeader>): List<FileHeader> {
        val allEntries = mutableSetOf<FileHeader>()
        val directories = mutableSetOf<String>()

        zipHeaders.forEach { header ->
            allEntries.add(header)

            var currentPath = header.fileName
            while (currentPath.contains("/")) {
                currentPath = currentPath.substringBeforeLast("/")
                directories.add("$currentPath/")
            }
        }

        val directoryHeaders = directories.map { dirPath ->
            val directoryHeader = FileHeader()
            directoryHeader.fileName = dirPath
            directoryHeader
        }

        allEntries.addAll(directoryHeaders)

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
                imageVector = Icons.Rounded.ArrowUpward,
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
    fun OperateDialog(
        filePath: Path,
        type: String,
        context: Context,
        onCancel: () -> Unit,
        showSelect : Boolean = false,
        fileInfoDialog: () -> Unit,
        renameDialog: () -> Unit,
        onApk: () -> Unit
    ) {
        val file = filePath.fileName
        var selectUi by remember { mutableStateOf(showSelect) }
        Dialog(
            onDismissRequest = { onCancel() },
            content = {
                @Composable
                fun OperateButton(
                    buttonText: String,
                    enabled: Boolean = true,
                    onClick: () -> Unit,
                ) {
                    Button(
                        modifier = Modifier
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                            .fillMaxWidth(),
                        shape = MaterialTheme.shapes.large,
                        onClick = {
                            onClick()
                        },
                        enabled = enabled
                    ) {
                        Text(buttonText)
                    }
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth(0.9f)
                        .background(
                            color = MaterialTheme.colorScheme.surfaceContainer,
                            shape = RoundedCornerShape(28.dp)
                        )
                        .padding(16.dp)
                ) {
                    Text(
                        text = filePath.fileName.toString(),
                        modifier = Modifier.padding(8.dp),
                        style = MaterialTheme.typography.labelLarge
                    )
                    AnimatedContent(
                        targetState = selectUi,
                        transitionSpec = {
                            (fadeIn(animationSpec = tween(220, delayMillis = 90)) +
                                    scaleIn(
                                        initialScale = 0.92f,
                                        animationSpec = tween(220, delayMillis = 90)
                                    ))
                                .togetherWith(fadeOut(animationSpec = tween(90)))
                        },
                    ) { targetState ->
                        if (!targetState){
                            Row {
                                Column(
                                    modifier = Modifier.weight(0.5f)
                                ) {
                                    OperateButton(
                                        if (pathState == "left") "  复制 >" else "< 复制  "
                                    ) {
                                        copyFile(filePath, Path((if (pathState == "left") rightPath else leftPath).toString() + "/" + file)).also { onCancel() }
                                    }
                                    OperateButton("分享") { shareFile(context = context, filePath = filePath.toString()).also { onCancel() }}
                                    OperateButton("命名") { renameDialog().also { onCancel() } }
                                    OperateButton("删除") { deleteFile(filePath).also { onCancel() } }
                                }
                                Column(
                                    modifier = Modifier.weight(0.5f)
                                ) {
                                    OperateButton(
                                        if (pathState == "left") "  移动 >" else "< 移动  "
                                    ) {
                                        moveFile(filePath, Path((if (pathState == "left") rightPath else leftPath).toString() + "/" + file)).also { onCancel() }
                                    }
                                    OperateButton("打开", type != "folder") { selectUi = true }
                                    OperateButton("属性") { fileInfoDialog().also { onCancel() } }
                                    OperateButton("取消") { onCancel() }
                                }
                            }
                        } else {
                            Row {
                                @Composable
                                fun ChooseButton(
                                    label: String,
                                    type: String
                                ) {
                                    FilledTonalButton(
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
                                Column(
                                    modifier = Modifier.weight(0.5f)
                                ) {
                                    ChooseButton("文本", "txt")
                                    ChooseButton("视频", "video")
                                    ChooseButton("图片", "image")
                                    ChooseButton("安装包", "apk")
                                }

                                Column(
                                    modifier = Modifier.weight(0.5f)
                                ) {
                                    ChooseButton("音频", "video")
                                    ChooseButton("其他", "outside")
                                    ChooseButton("压缩包", "zip")
                                    OperateButton("取消") { onCancel() }
                                }
                            }
                        }
                    }
                }
            }
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
                TextField(
                    value = textFieldValue,
                    onValueChange = { textFieldValue = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    shape = MaterialTheme.shapes.large,
                    colors = TextFieldDefaults.colors(
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    )
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onCancel()
                        val sourceFile = File(filePath.toString())
                        val targetFile = File(filePath.parent.toString() + "/" + textFieldValue)
                        sourceFile.renameTo(targetFile)
                    }
                ) {
                    Text(stringResource(R.string.confirm))
                }
            },
            dismissButton = {
                FilledTonalButton(onClick = { onCancel() }) {
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

    @Composable
    fun AudioDialog(
        filePath: Path,
        onCancel: () -> Unit,
    ) {
        Dialog(
            onDismissRequest = { onCancel() },
            content = {
                val context = LocalContext.current
                val exoPlayer = remember {
                    ExoPlayer.Builder(context).build().apply {
                        val mediaItem = MediaItem.fromUri(Uri.fromFile(File(filePath.toString())))
                        setMediaItem(mediaItem)
                        prepare()
                    }
                }

                var isPlaying by remember { mutableStateOf(true) }
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
                    exoPlayer.play()
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
                Column(
                    modifier = Modifier
                        .padding(16.dp)
                        .background(MaterialTheme.colorScheme.background, MaterialTheme.shapes.extraLarge),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Spacer(modifier = Modifier.padding(vertical = 8.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.9f)
                            .background(
                                MaterialTheme.colorScheme.primaryContainer,
                                MaterialTheme.shapes.medium
                            )
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Text(
                            File(filePath.toString()).name,
                            style = MaterialTheme.typography.titleLarge,
                            modifier = Modifier.padding(start = 16.dp)
                        )
                    }
                    Spacer(modifier = Modifier.padding(vertical = 8.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.9f)
                            .height(128.dp)
                            .background(
                                MaterialTheme.colorScheme.primaryContainer,
                                MaterialTheme.shapes.medium
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.MusicNote,
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
                            TextButton(onClick = {
                                playbackSpeed = when (playbackSpeed) {
                                    0.5f -> 1f
                                    1f -> 1.5f
                                    1.5f -> 2f
                                    else -> 0.5f
                                }
                                exoPlayer.setPlaybackSpeed(playbackSpeed)
                            }) {
                                Text("$playbackSpeed x")
                            }

                            IconButton(onClick = {
                                isLooping = !isLooping
                                exoPlayer.repeatMode =
                                    if (isLooping) ExoPlayer.REPEAT_MODE_ONE else ExoPlayer.REPEAT_MODE_OFF
                            }) {
                                Icon(
                                    imageVector = if (isLooping) {
                                        Icons.Rounded.RepeatOn
                                    } else {
                                        Icons.Rounded.Repeat
                                    },
                                    contentDescription = if (isLooping) "关闭循环" else "开启循环"
                                )
                            }

                            IconButton(
                                onClick = {
                                    openActivity(context, filePath.toString(), AudioPlayer::class.java)
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Fullscreen,
                                    contentDescription = null,
                                )
                            }

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
                                        Icons.Rounded.Pause
                                    } else {
                                        Icons.Rounded.PlayArrow
                                    },
                                    contentDescription = if (isPlaying) "暂停" else "播放",
                                )
                            }
                        }
                    }
                }
            }
        )
    }
}
