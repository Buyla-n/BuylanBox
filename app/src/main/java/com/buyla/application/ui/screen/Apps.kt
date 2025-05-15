package com.buyla.application.ui.screen

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Environment
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.createBitmap
import com.buyla.application.R
import com.buyla.application.activity.AppsListActivity
import com.buyla.application.util.ApkUtil.ApkInfoDialog
import com.buyla.application.util.ApkUtil.getInstalledApkPath
import com.buyla.application.util.FileUtil.copyFile
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.nio.file.Paths


object Apps {
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun AppScreen(context: Context) {
        var appList by remember { mutableStateOf<List<ApplicationInfo>>(emptyList()) }
        var searchText by remember { mutableStateOf("") }
        var searchActive by remember { mutableStateOf(false) }
        val focusRequester = remember { FocusRequester() }
        val focusManager = LocalFocusManager.current
        val keyboardController = LocalSoftwareKeyboardController.current

        LaunchedEffect(Unit) {
            val apps = withContext(Dispatchers.IO) {
                context.packageManager.getInstalledApplications(PackageManager.GET_META_DATA)
            }
            appList = apps
        }

        // 过滤应用列表
        val filteredApps = remember(appList, searchText) {
            if (searchText.isEmpty()) {
                appList
            } else {
                appList.filter { app ->
                    app.loadLabel(context.packageManager)
                        .toString()
                        .contains(searchText, ignoreCase = true)
                }
            }
        }

        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    title = {
                        if (searchActive) {
                            TextField(
                                value = searchText,
                                onValueChange = { searchText = it },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .focusRequester(focusRequester)
                                    .height(48.dp)
                                    .padding(start = 8.dp),
                                placeholder = { Text("输入应用名称", fontSize = 12.sp) },
                                singleLine = true,
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Filled.Search,
                                        contentDescription = null
                                    )
                                },
                                trailingIcon = {
                                    if (searchText.isNotEmpty()) {
                                        IconButton(onClick = { searchText = "" }) {
                                            Icon(
                                                imageVector = Icons.Filled.Clear,
                                                contentDescription = "清除"
                                            )
                                        }
                                    }
                                },
                                colors = TextFieldDefaults.colors(
                                    focusedContainerColor = Color.Transparent,
                                    unfocusedContainerColor = Color.Transparent,
                                ),
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                                keyboardActions = KeyboardActions(
                                    onSearch = {
                                        keyboardController?.hide()
                                        focusManager.clearFocus()
                                    }
                                ),
                                shape = RoundedCornerShape(36.dp),
                                textStyle = MaterialTheme.typography.labelSmall
                            )
                        } else {
                            Text("应用列表", Modifier
                                .fillMaxWidth()
                                .padding(start = 8.dp))
                        }
                    },
                    actions = {
                        IconButton(
                            onClick = {
                                searchActive = !searchActive
                                if (!searchActive) {
                                    searchText = ""
                                    keyboardController?.hide()
                                    focusManager.clearFocus()
                                }
                            }
                        ) {
                            Icon(
                                imageVector = if (searchActive) Icons.Filled.Close else Icons.Filled.Search,
                                contentDescription = if (searchActive) "关闭搜索" else "搜索"
                            )
                        }
                    }
                )
            }
        ) { innerPadding ->
            LazyColumn(
                modifier = Modifier
                    .padding(top = innerPadding.calculateTopPadding())
                    .fillMaxSize()
            ) {
                items(filteredApps) { appInfo ->
                    AppListItem(appInfo = appInfo, context = context)
                }
            }
        }

        LaunchedEffect(searchActive) {
            if (searchActive) {
                focusRequester.requestFocus()
            }
        }
    }

    @Composable
    private fun AppListItem(appInfo: ApplicationInfo, context: Context) {
        var showInfoDialog by remember { mutableStateOf(false) }
        var successDialog by remember { mutableStateOf(false) }
        var appName by remember { mutableStateOf("") }
        var appIconBitmap by remember { mutableStateOf<Bitmap?>(null) }
        LaunchedEffect(appInfo) {
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    // 加载应用名称
                    val name = appInfo.loadLabel(context.packageManager).toString()
                    // 加载应用图标
                    val iconDrawable = context.packageManager.getApplicationIcon(appInfo.packageName)
                    val iconBitmap =
                        createBitmap(iconDrawable.intrinsicWidth, iconDrawable.intrinsicHeight)
                    val canvas = Canvas(iconBitmap)
                    iconDrawable.setBounds(0, 0, canvas.width, canvas.height)
                    iconDrawable.draw(canvas)
                    withContext(Dispatchers.Main) {
                        appName = name
                        appIconBitmap = iconBitmap
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
        Spacer(Modifier.padding(vertical = 8.dp))
        Row(
            modifier = Modifier
                .padding(start = 16.dp,end = 16.dp)
        ) {
            Row(
                modifier = Modifier
                    .height(54.dp)
                    .weight(4f)
                    .background(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = MaterialTheme.shapes.medium
                    ),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 应用图标
                appIconBitmap?.let { bitmap ->
                    val imageBitmap = bitmap.asImageBitmap()
                    Image(
                        bitmap = imageBitmap,
                        contentDescription = null,
                        modifier = Modifier
                            .size(48.dp)
                            .padding(8.dp)
                    )
                } ?: Image(
                    imageVector = Icons.Filled.HourglassEmpty, // 这里替换成你的默认图标资源
                    contentDescription = null,
                    modifier = Modifier
                        .size(48.dp)
                        .padding(8.dp)
                )
                Text(
                    text = appName,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                )
            }
            Spacer(Modifier.padding(horizontal = 8.dp))
            Button(
                onClick = { showInfoDialog = true },
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier
                    .weight(1f)
                    .height(54.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Info,
                    contentDescription = null
                )
            }
        }

        if (showInfoDialog){
            ApkInfoDialog(
                filePath = getInstalledApkPath(context, appInfo.packageName),
                onCancel = {showInfoDialog = false},
                context = LocalContext.current,
                buttonCustom = {
                    Button(
                        modifier = Modifier.padding(horizontal = 4.dp),
                        onClick = {
                            copyFile(
                                getInstalledApkPath(context, appInfo.packageName),
                                Paths.get("${Environment.getExternalStorageDirectory()}/BuylaBox/" + appInfo.packageName + ".apk")
                            )
                            successDialog = true
                        },
                    ) {
                        Text("提取")
                    }
                    Button(
                        modifier = Modifier
                            .padding(horizontal = 4.dp),
                        onClick = {
                            val intent = Intent(context, AppsListActivity::class.java).apply {
                                putExtra("packageName", appInfo.packageName)
                            }
                            context.startActivity(intent)
                        }
                    ) {
                        Text("活动")
                    }
                },
                findByName = true,
                packageName = appInfo.packageName
            )
        }

        if (successDialog){
            AlertDialog(
                onDismissRequest = { successDialog = false },
                title = { Text("提取完成") },
                text = { Text("文件被保存在 /sdcard/BuylaBox") },
                confirmButton = {
                    Button(onClick = {successDialog = false}) {
                        Text(stringResource(R.string.confirm))
                    }
                },
                dismissButton = {
                },
            )
        }
    }
}