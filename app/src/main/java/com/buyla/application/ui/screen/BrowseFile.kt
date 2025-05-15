package com.buyla.application.ui.screen
    
import android.content.Context
import android.content.Intent
import android.os.Environment
import android.os.StatFs
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Help
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.InstallMobile
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material.icons.twotone.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DrawerState
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.VerticalDivider
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import com.buyla.application.R
import com.buyla.application.activity.FileSettings
import com.buyla.application.data.FileStateData
import com.buyla.application.util.ApkUtil.ApkInfoDialog
import com.buyla.application.util.FileUtil
import com.buyla.application.util.FileUtil.ChooseDialog
import com.buyla.application.util.FileUtil.FileInfoDialog
import com.buyla.application.util.FileUtil.FileParentItem
import com.buyla.application.util.FileUtil.OperateDialog
import com.buyla.application.util.FileUtil.RenameFileDialog
import com.buyla.application.util.FileUtil.completeDirectoriesFromHeaders
import com.buyla.application.util.FileUtil.getFileIcon
import com.buyla.application.util.FileUtil.getFileType
import com.buyla.application.util.FileUtil.getZipFileType
import com.buyla.application.util.FileUtil.leftFileInside
import com.buyla.application.util.FileUtil.leftFileName
import com.buyla.application.util.FileUtil.leftInside
import com.buyla.application.util.FileUtil.leftPath
import com.buyla.application.util.FileUtil.leftPathInside
import com.buyla.application.util.FileUtil.onFileClick
import com.buyla.application.util.FileUtil.pathState
import com.buyla.application.util.FileUtil.rightFileInside
import com.buyla.application.util.FileUtil.rightFileName
import com.buyla.application.util.FileUtil.rightInside
import com.buyla.application.util.FileUtil.rightPath
import com.buyla.application.util.FileUtil.rightPathInside
import com.buyla.application.util.FileUtil.sortSelectedIndex
import com.buyla.application.util.Util.fileVertical
import com.buyla.application.util.Util.forbiddenCharacters
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.lingala.zip4j.model.FileHeader
import okio.Path.Companion.toPath
import java.io.File
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.Path

object BrowseFile {
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun FileScreen(context: Context) {
        var addSelectedIndex by remember { mutableIntStateOf(0) }
        val addOptions = listOf("文件", "文件夹")
        var lastItem by remember { mutableStateOf("left") }
        var isRefreshingLeft by remember { mutableStateOf(false) }
        var isRefreshingRight by remember { mutableStateOf(false) }
        var textFieldValue by remember { mutableStateOf("") }

        if (!Environment.isExternalStorageManager()) {
            val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
            intent.data = ("package:" + context.packageName).toUri()
            context.startActivity(intent)
        }

        val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
        val scope = rememberCoroutineScope()

        BackHandler(onBack = {
            scope.launch {
                if (drawerState.isClosed) {
                    if (lastItem == "left") {
                        if (!leftInside) {
                            val parentPath = leftPath.parent
                            if (parentPath != null) leftPath = parentPath
                        } else {
                            if (leftPathInside == ""){
                                leftInside = false
                            } else {
                                leftPathInside = if (leftPathInside.contains("/")) leftPathInside.substringBeforeLast("/") else ""
                            }
                        }
                    } else {
                        if (!rightInside) {
                            val parentPath = rightPath.parent
                            if (parentPath != null) rightPath = parentPath
                        } else {
                            if (rightPathInside == ""){
                                rightInside = false
                            } else {
                                rightPathInside = if (rightPathInside.contains("/")) rightPathInside.substringBeforeLast("/") else ""
                            }
                        }
                    }
                } else {
                    drawerState.close()
                }
            }
        })

        FileDrawer(scope, drawerState, context, listOf("名称", "类型", "大小", "时间"), lastItem = lastItem) {
            var isError by remember { mutableStateOf(false) }
            // here is file screen
            Scaffold(
                modifier = Modifier.fillMaxSize(),
                topBar = {
                    CenterAlignedTopAppBar(
                        navigationIcon = {
                            IconButton(onClick = {
                                scope.launch {
                                    if (drawerState.isClosed) {
                                        drawerState.open()
                                    } else {
                                        drawerState.close()
                                    }
                                }
                            }) {
                                Icon(Icons.Rounded.Menu, contentDescription = "Menu")
                            }
                        },
                        title = {
                            Text(
                                text = (if (lastItem == "left") leftPath.toString() else rightPath.toString()).takeLast(24)
                                    .let { str ->
                                        if (str.length == 24) "...$str" else str
                                    },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(start = 8.dp),
                                style = MaterialTheme.typography.titleMedium,
                                maxLines = 1,

                            )
                        },
                        actions = {
                            var showAddFileDialog by remember { mutableStateOf(false) }
                            IconButton(
                                onClick = { showAddFileDialog = true }
                            ) {
                                Icon(
                                    Icons.Rounded.Add,
                                    contentDescription = "Add",
                                    modifier = Modifier.padding(8.dp)
                                )

                            }
                            if (showAddFileDialog) {
                                AlertDialog(
                                    onDismissRequest = { showAddFileDialog = false },
                                    title = { Text("创建文件") },
                                    text = {
                                        Column {
                                            SingleChoiceSegmentedButtonRow(
                                                Modifier
                                                    .fillMaxWidth()
                                                    .padding(horizontal = 16.dp, vertical = 8.dp)
                                            ) {
                                                addOptions.forEachIndexed { index, label ->
                                                    SegmentedButton(
                                                        shape = SegmentedButtonDefaults.itemShape(
                                                            index = index,
                                                            count = addOptions.size
                                                        ),
                                                        onClick = {
                                                            addSelectedIndex = index
                                                        },
                                                        selected = index == addSelectedIndex,
                                                        label = { Text(label) }
                                                    )
                                                }
                                            }
                                            isError =
                                                textFieldValue.any { it in forbiddenCharacters } && addSelectedIndex == 0
                                            OutlinedTextField(
                                                value = textFieldValue,
                                                onValueChange = {
                                                    textFieldValue = it
                                                },
                                                label = { Text("文件名") },
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(horizontal = 16.dp),
                                                shape = MaterialTheme.shapes.large,
                                                isError = isError,
                                            )
                                        }
                                    },
                                    confirmButton = {
                                        Button(onClick = {
                                            if (!isError) {
                                                if (addSelectedIndex == 0) {
                                                    if (lastItem == "left") {
                                                        File(leftPath.toString() + File.separator + textFieldValue).createNewFile()
                                                    } else {
                                                        File(rightPath.toString() + File.separator + textFieldValue).createNewFile()
                                                    }
                                                } else {
                                                    if (lastItem == "left") {
                                                        File(leftPath.toString() + File.separator + textFieldValue).mkdirs()
                                                    } else {
                                                        File(rightPath.toString() + File.separator + textFieldValue).mkdirs()
                                                    }
                                                }
                                                showAddFileDialog = false
                                            } else {
                                                Toast.makeText(
                                                    context,
                                                    "不能带有特殊符号",
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                            }
                                        }) {
                                            Text(stringResource(R.string.confirm))
                                        }
                                    },
                                    dismissButton = {
                                        TextButton(
                                            onClick = { showAddFileDialog = false }
                                        ) {
                                            Text(stringResource(R.string.cancel))
                                        }
                                    }
                                )
                            }
                        }
                    )
                },
            ) { innerPadding ->
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = innerPadding.calculateTopPadding())
                ) {
                    PullToRefreshBox(
                        modifier = Modifier.weight(1f),
                        onRefresh = {
                            isRefreshingLeft = true
                            CoroutineScope(Dispatchers.IO).launch {
                                delay(50)
                                isRefreshingLeft = false
                            }
                        },
                        isRefreshing = isRefreshingLeft,
                    ) {
                        AnimatedContent(
                            targetState = FileStateData( leftPath, leftFileInside, leftInside,  leftPathInside),
                            modifier = Modifier.fillMaxSize(),
                            transitionSpec = {
                                (fadeIn(animationSpec = tween(330, delayMillis = 0)) + scaleIn(initialScale = 0.99f, animationSpec = tween(330, delayMillis = 0))).togetherWith(fadeOut(animationSpec = tween(0)))
                            }
                        ) { ( path, inFile, leftInside, insidePath) ->
                            //left
                            val sortedFiles = if (!leftInside) remember(path, sortSelectedIndex) { getSortedFiles(path, sortSelectedIndex) } else listOf()
                            LazyColumn(
                                    modifier = Modifier
                                        .pointerInput(Unit) {
                                            awaitPointerEventScope {
                                                while (true) {
                                                    if (awaitPointerEvent().type == PointerEventType.Press) {
                                                        lastItem = "left"
                                                    }
                                                }
                                            }
                                        }
                                ) {
                                    item {
                                        if (leftPath.toString() != "/")
                                        FileParentItem(
                                            onClick = {
                                                if (!leftInside) {
                                                    val parentPath = leftPath.parent
                                                    if (parentPath != null) leftPath = parentPath
                                                } else {
                                                    if (insidePath == ""){
                                                        FileUtil.leftInside = false
                                                    } else {
                                                        leftPathInside = if (leftPathInside.contains("/")) leftPathInside.substringBeforeLast("/") else ""
                                                    }
                                                }
                                            }
                                        )
                                    }
                                    if (!leftInside) {
                                        items(sortedFiles) { file ->
                                            val filePath = path.resolve(file)
                                            FileItem(
                                                file = file,
                                                filePath = filePath,
                                                scope = rememberCoroutineScope(),
                                                state = "left"
                                            )
                                        }
                                    } else {
                                        items(sortZipHeaders(filterZipEntries(completeDirectoriesFromHeaders(inFile), insidePath))) { file ->
                                            val fileName = file.fileName.removePrefix("$insidePath/")
                                            FileItem(
                                                file = fileName,
                                                filePath = Path(insidePath).resolve(fileName),
                                                outSidePath = leftPath.resolve(leftFileName).toString(),
                                                scope = rememberCoroutineScope(),
                                                state = "left",
                                                inSideType = true
                                            )
                                        }
                                    }
                                }
                        }
                    }

                    VerticalDivider()

                    PullToRefreshBox(
                        modifier = Modifier.weight(1f),
                        onRefresh = {
                            isRefreshingRight = true
                            CoroutineScope(Dispatchers.IO).launch {
                                delay(50)
                                isRefreshingRight = false
                            }
                        },
                        isRefreshing = isRefreshingRight,
                    ) {
                        AnimatedContent(
                            targetState = FileStateData( rightPath, rightFileInside,  rightInside, rightPathInside),
                            modifier = Modifier.fillMaxSize(),
                            transitionSpec = {
                                (fadeIn(animationSpec = tween(330, delayMillis = 0)) + scaleIn(initialScale = 0.99f, animationSpec = tween(330, delayMillis = 0))).togetherWith(fadeOut(animationSpec = tween(0)))
                            }
                        ) { (path, inFile, rightInside, insidePath) ->
                            val sortedFiles = if (!rightInside) remember(path, sortSelectedIndex) { getSortedFiles(path, sortSelectedIndex) } else listOf()
                            //right
                            LazyColumn(
                                modifier = Modifier
                                    .pointerInput(Unit) {
                                        awaitPointerEventScope {
                                            while (true) {
                                                if (awaitPointerEvent().type == PointerEventType.Press) {
                                                    lastItem = "right"
                                                }
                                            }
                                        }
                                    }
                            ) {
                                item {
                                    if (rightPath.toString() != "/")
                                    FileParentItem(
                                        onClick = {
                                            if (!rightInside){
                                                val parentPath = rightPath.parent
                                                if (parentPath != null) rightPath = parentPath
                                            } else {
                                                if (insidePath == ""){
                                                    FileUtil.rightInside = false
                                                } else {
                                                    rightPathInside = if (rightPathInside.contains("/")) rightPathInside.substringBeforeLast("/") else ""
                                                }
                                            }
                                        }
                                    )
                                }
                                if (!rightInside) {
                                    items(sortedFiles) { file ->
                                        val filePath = path.resolve(file)
                                        FileItem(file, filePath, scope = rememberCoroutineScope(), state = "right")
                                    }
                                } else {
                                    items(sortZipHeaders(filterZipEntries(completeDirectoriesFromHeaders(inFile), insidePath))) { file ->
                                        val fileName = file.fileName.removePrefix("$insidePath/")
                                        FileItem(
                                            file = fileName,
                                            filePath = Path(insidePath).resolve(fileName),
                                            outSidePath = rightPath.resolve(rightFileName).toString(),
                                            scope = rememberCoroutineScope(),
                                            state = "right",
                                            inSideType = true
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    @Composable
    fun FileDrawer(
        scope: CoroutineScope,
        drawerState: DrawerState,
        context: Context,
        sortOptions: List<String>,
        lastItem: String,
        content: @Composable (  ) -> Unit
    ) {
        var showSortDialog by remember { mutableStateOf(false) }
        ModalNavigationDrawer(
            drawerContent = {
                ModalDrawerSheet(
                    modifier = Modifier.fillMaxWidth(0.8f)
                ) {
                    Column(
                        modifier = Modifier
                            .padding(horizontal = 16.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        Spacer(Modifier.height(12.dp))
                        Text(
                            "文件更改",
                            modifier = Modifier.padding(16.dp),
                            style = MaterialTheme.typography.titleLarge
                        )
                        HorizontalDivider()

                        var storageProgress by remember { mutableFloatStateOf(0f) }
                        var rootProgress by remember { mutableFloatStateOf(0f) }

                        LaunchedEffect(Unit) {
                            val progress = withContext(Dispatchers.IO) {
                                try {
                                    val stat =
                                        StatFs(Environment.getExternalStorageDirectory().path)
                                    val totalBytes = stat.blockCountLong * stat.blockSizeLong
                                    val availableBytes =
                                        stat.availableBlocksLong * stat.blockSizeLong
                                    if (totalBytes > 0) (totalBytes - availableBytes).toFloat() / totalBytes else 0f
                                } catch (_: Exception) {
                                    0f
                                }
                            }
                            storageProgress = progress
                            val catchRootProgress = withContext(Dispatchers.IO) {
                                try {
                                    val stat = StatFs(Environment.getRootDirectory().path)
                                    val totalBytes = stat.blockCountLong * stat.blockSizeLong
                                    val availableBytes =
                                        stat.availableBlocksLong * stat.blockSizeLong
                                    if (totalBytes > 0) (totalBytes - availableBytes).toFloat() / totalBytes else 0f
                                } catch (_: Exception) {
                                    0f
                                }
                            }
                            rootProgress = catchRootProgress
                        }
                        Text(
                            "存储器",
                            modifier = Modifier.padding(16.dp),
                            style = MaterialTheme.typography.titleMedium
                        )
                        NavigationDrawerItem(
                            label = { Text("根目录") },
                            selected = false,
                            onClick = {
                                leftPath = Paths.get("/")
                                scope.launch {
                                    drawerState.close()
                                }
                            },
                            icon = { Icon(Icons.Filled.FolderOpen, contentDescription = null) },
                            badge = {
                                BadgedBox(
                                    badge = {
                                        Badge(
                                            content = {
                                                Text(
                                                    (rootProgress * 100).toInt().toString() + "%",
                                                    fontSize = 6.sp,
                                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                                )
                                            },
                                            containerColor = MaterialTheme.colorScheme.primaryContainer
                                        )
                                    },
                                    //
                                ) {
                                    CircularProgressIndicator(
                                        progress = { rootProgress },
                                        modifier = Modifier.size(24.dp),
                                        strokeWidth = 3.dp
                                    )
                                }
                            }
                        )

                        NavigationDrawerItem(
                            label = { Text("存储器") },
                            selected = false,
                            onClick = {
                                if (lastItem == "left") leftPath = Paths.get(Environment.getExternalStorageDirectory().toString()) else rightPath = Paths.get(Environment.getExternalStorageDirectory().toString())
                                scope.launch {
                                    drawerState.close()
                                }
                            },
                            icon = { Icon(Icons.Filled.InstallMobile, contentDescription = null) },
                            badge = {
                                BadgedBox(
                                    badge = {
                                        Badge(
                                            content = {
                                                Text(
                                                    (storageProgress * 100).toInt()
                                                        .toString() + "%",
                                                    fontSize = 6.sp,
                                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                                )
                                            },
                                            containerColor = MaterialTheme.colorScheme.primaryContainer
                                        )
                                    },
                                    //
                                ) {
                                    CircularProgressIndicator(
                                        progress = { storageProgress },
                                        modifier = Modifier.size(24.dp),
                                        strokeWidth = 3.dp
                                    )
                                }
                            }
                        )

                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                        Text(
                            "摘要",
                            modifier = Modifier.padding(16.dp),
                            style = MaterialTheme.typography.titleMedium
                        )
                        NavigationDrawerItem(
                            label = { Text("排序方式") },
                            selected = false,
                            icon = {
                                Icon(
                                    Icons.AutoMirrored.Filled.Sort,
                                    contentDescription = null
                                )
                            },
                            badge = {
                                when (sortSelectedIndex) {
                                    0 -> Text("名称")
                                    1 -> Text("类型")
                                    2 -> Text("大小")
                                    3 -> Text("时间")
                                }
                            },
                            onClick = { showSortDialog = true }
                        )
                        NavigationDrawerItem(
                            label = { Text("Help and feedback") },
                            selected = false,
                            icon = {
                                Icon(
                                    Icons.AutoMirrored.Filled.Help,
                                    contentDescription = null
                                )
                            },
                            onClick = { /* Handle click */ },
                        )
                        NavigationDrawerItem(
                            label = { Text("行为设置") },
                            selected = false,
                            icon = { Icon(Icons.TwoTone.Settings, contentDescription = null) },
                            onClick = {
                                context.startActivity(
                                    Intent(context, FileSettings::class.java)
                                )
                            },
                        )
                        Spacer(Modifier.height(12.dp))
                    }
                }
                if (showSortDialog) {
                    AlertDialog(
                        onDismissRequest = { showSortDialog = false },
                        title = { Text(stringResource(R.string.sorted)) },
                        text = {
                            SingleChoiceSegmentedButtonRow {
                                sortOptions.forEachIndexed { index, label ->
                                    SegmentedButton(
                                        shape = SegmentedButtonDefaults.itemShape(
                                            index = index,
                                            count = sortOptions.size
                                        ),
                                        onClick = { sortSelectedIndex = index },
                                        selected = index == sortSelectedIndex,
                                        label = { Text(label) }
                                    )
                                }
                            }
                        },
                        confirmButton = {
                            Button(onClick = { showSortDialog = false }) {
                                Text(stringResource(R.string.confirm))
                            }
                        },
                        dismissButton = {}
                    )
                }
            },
            drawerState = drawerState
        ) {
            content()
        }
    }

    private fun getSortedFiles(path: Path, sortByName: Int): List<String> {
        return try {
            val files = path.toFile().listFiles()?.toList()
            val getFiles = files!!.sortedWith(
                compareBy<File> { !it.isDirectory }
                    .then(
                        when (sortByName) {
                            0 -> compareBy { it.name.lowercase() }
                            1 -> compareBy { it.extension.lowercase() }
                            2 -> compareBy { it.length() }
                            3 -> compareByDescending { it.lastModified() }
                            else -> compareBy { it.name }
                        }
                    )
            ).map { it.name }
            return getFiles
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun sortZipHeaders(zipHeaders: List<FileHeader>): List<FileHeader> {
        return zipHeaders.sortedWith(
            compareBy<FileHeader> { !it.fileName.endsWith("/") }
                .then(
                    when (sortSelectedIndex) {
                        0 -> compareBy { it.fileName }
                        1 -> compareBy { it.fileName.toString().substringAfter(".") }
                        2 -> compareBy { it.compressedSize }
                        3 -> compareByDescending { it.lastModifiedTime }
                        else -> compareBy { it.fileName }
                    }
                )
        ).map { it }
    }

    @OptIn(ExperimentalFoundationApi::class)
    @Composable
    fun FileItem(
        file: String,
        filePath: Path,
        outSidePath : String = "",
        scope: CoroutineScope,
        state: String,
        inSideType: Boolean = false
    ) {
        var showChooseDialog by remember { mutableStateOf(false) }
        var showOperateDialog by remember { mutableStateOf(false) }
        var showRenameDialog by remember { mutableStateOf(false) }
        var showInstallDialog by remember { mutableStateOf(false) }
        var showFileInfoDialog by remember { mutableStateOf(false) }
        val context = LocalContext.current
        val type = if (!inSideType) getFileType(filePath.toString()) else getZipFileType(file)
        val haptics = LocalHapticFeedback.current

        Row(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = fileVertical.dp)
                .fillMaxWidth()
                .height(48.dp)
                .background(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(8.dp)
                )
                .clip(RoundedCornerShape(8.dp))
                .combinedClickable(
                    onClick = {
                        scope.launch {
                            pathState = state
                            if (!inSideType || type == "folder") {
                                onFileClick(
                                    context = context,
                                    filePath = filePath.toString(),
                                    type = type,
                                    onNull = { showChooseDialog = true },
                                    onApk = { showInstallDialog = true }
                                )
                            } else {
                                net.lingala.zip4j.ZipFile(outSidePath).extractFile(
                                    filePath.toString(),
                                    "${context.filesDir.absolutePath}/extract_zip/temp/${outSidePath.toPath().name}/"
                                )
                                onFileClick(
                                    context = context,
                                    filePath = "${context.filesDir.absolutePath}/extract_zip/temp/${outSidePath.toPath().name}/$filePath",
                                    type = type,
                                    onNull = { showChooseDialog = true },
                                    onApk = { showInstallDialog = true }
                                )
                            }
                        }

                    },
                    onLongClick = {
                        pathState = state
                        showOperateDialog = true
                        haptics.performHapticFeedback(HapticFeedbackType.ToggleOn)
                    },
                    indication = null,
                    interactionSource = null
                ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = getFileIcon(type),
                contentDescription = null,
                modifier = Modifier.padding(start = 8.dp),
                tint = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Text(
                text = file.removeSuffix("/"),
                fontSize = 12.sp,
                modifier = Modifier.padding(horizontal = 16.dp),
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }

        if (showChooseDialog) { ChooseDialog(onCancel = { showChooseDialog = false }, context = context, filePath = filePath, {showInstallDialog = true}) }
        if (showOperateDialog) { OperateDialog(filePath = filePath, type = type, context = context, onCancel = { showOperateDialog = false }, chooseDialog = { showChooseDialog = true }, fileInfoDialog = { showFileInfoDialog = true }, renameDialog = { showRenameDialog = true }) }
        if (showFileInfoDialog) { FileInfoDialog(filePath = filePath, onCancel = { showFileInfoDialog = false }) }
        if (showRenameDialog) { RenameFileDialog(filePath = filePath, onCancel = { showRenameDialog = false }) }
        if (showInstallDialog) { ApkInfoDialog(filePath = filePath, onCancel = { showInstallDialog = false }, findByName = false, context = context) }
    }

    fun filterZipEntries(entries: List<FileHeader>, targetPath: String): List<FileHeader> {
        val normalizedPath = if (targetPath.endsWith("/")) targetPath else "$targetPath/"
        val isRoot = targetPath.isEmpty()

        return entries.filter { entry ->
            val entryName = entry.fileName
            when {
                isRoot -> { entryName.count { it == '/' } == 0 || (entryName.endsWith("/") && entryName.count { it == '/' } == 1) }
                else -> { entryName.startsWith(normalizedPath) && entryName.removePrefix(normalizedPath).let { it.count { it == '/' } == 0 || (it.endsWith("/") && it.count { it == '/' } == 1) } && entryName != normalizedPath }
            }
        }
    }
}