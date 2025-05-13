package com.buyla.application.activity

import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.core.graphics.createBitmap
import com.buyla.application.R
import com.buyla.application.ui.theme.MyAppTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AppsListActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()
        window.isNavigationBarContrastEnforced = false

        val packageName = intent.getStringExtra("packageName") ?: return

        setContent {
            MyAppTheme {
                AppActivitiesList(packageName)
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    private fun AppActivitiesList(packageName: String) {
        val packageManager = packageManager
        val activitiesList = remember { mutableStateListOf<ActivityInfo>() }
        val context = LocalContext.current
        var searchText by remember { mutableStateOf("") }
        var searchActive by remember { mutableStateOf(false) }
        val focusRequester = remember { FocusRequester() }
        val focusManager = LocalFocusManager.current
        val keyboardController = LocalSoftwareKeyboardController.current

        LaunchedEffect(packageName) {
            withContext(Dispatchers.IO) {
                try {
                    val packageInfo = packageManager.getPackageInfo(packageName, PackageManager.GET_ACTIVITIES)
                    packageInfo.activities?.toList()?.let {
                        activitiesList.addAll(it)
                    }
                } catch (e: PackageManager.NameNotFoundException) {
                    e.printStackTrace()
                }
            }
        }

        val filteredActivities = remember(activitiesList, searchText) {
            if (searchText.isEmpty()) {
                activitiesList
            } else {
                activitiesList.filter { activity ->
                    activity.loadLabel(context.packageManager)
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
                                    .padding(start = 8.dp),
                                placeholder = { Text("输入活动名称") },
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
                                    focusedContainerColor = MaterialTheme.colorScheme.background,
                                    unfocusedContainerColor = MaterialTheme.colorScheme.background,
                                    disabledContainerColor = MaterialTheme.colorScheme.background,
                                ),
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                                keyboardActions = KeyboardActions(
                                    onSearch = {
                                        keyboardController?.hide()
                                        focusManager.clearFocus()
                                    }
                                )
                            )
                        } else {
                            Text("活动列表", Modifier
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
            if (filteredActivities.isEmpty()) {
                Box(
                    Modifier.fillMaxSize(),
                    Alignment.Center
                ) {
                    Text("╮(╯▽╰)╭ 啥也没有")
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .padding(top = innerPadding.calculateTopPadding())
                        .fillMaxSize()
                ) {

                    items(filteredActivities) { activityInfo ->
                        val isExportable = activityInfo.exported
                        ActivityListItem(
                            appInfo = activityInfo,
                            context = context,
                            exp = isExportable
                        )
                    }
                }
            }
        }
    }

    @Composable
    private fun ActivityListItem(appInfo: ActivityInfo, context: Context, exp: Boolean) {
        var showWarningDialog by remember { mutableStateOf(false) }
        var showLinkDialog by remember { mutableStateOf(false) }

            if (showLinkDialog){
                AlertDialog(
                    onDismissRequest = { showWarningDialog = false },
                    title = { },
                    text = {
                        Row {
                            Button(
                                onClick = { extracted(exp, appInfo)},
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("打开")
                            }
                            Button(
                                onClick = {  },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("创建")
                            }
                        }
                    },
                    confirmButton = {
                    },
                    dismissButton = {
                        TextButton(onClick = { showLinkDialog = false }) {
                            Text(stringResource(R.string.cancel))
                        }
                    }
                )
            }
            var appIconBitmap by remember { mutableStateOf<Bitmap?>(null) }
            LaunchedEffect(appInfo) {
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val iconDrawable = context.packageManager.getApplicationIcon(appInfo.packageName)
                        val iconBitmap = createBitmap(iconDrawable.intrinsicWidth, iconDrawable.intrinsicHeight)
                        val canvas = Canvas(iconBitmap)
                        iconDrawable.setBounds(0, 0, canvas.width, canvas.height)
                        iconDrawable.draw(canvas)
                        withContext(Dispatchers.Main) {
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
                        imageVector = Icons.Filled.HourglassEmpty,
                        contentDescription = null,
                        modifier = Modifier
                            .size(48.dp)
                            .padding(8.dp)
                    )
                    Column(
                        Modifier.weight(1f)
                    ) {
                        Text(
                            text = appInfo.loadLabel(context.packageManager).toString(),
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier
                                .padding(horizontal = 16.dp)
                        )
                        Text(
                            text = appInfo.name,
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier
                                .padding(horizontal = 16.dp)
                        )
                    }

                }
                Spacer(Modifier.padding(horizontal = 8.dp))
                Button(
                    onClick = { showLinkDialog = true },
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier
                        .weight(1f)
                        .height(54.dp)
                ) {
                    if (!exp) {
                        IconButton(
                            onClick = { showWarningDialog = true },
                            Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Warning,
                                contentDescription = "未导出"
                            )
                        }
                        if (showWarningDialog){
                            AlertDialog(
                                onDismissRequest = { showWarningDialog = false },
                                title = { Text("未导出的 Activity") },
                                text = {
                                    Text("Android未导出的Activity指未在AndroidManifest.xml中设置 android:exported= true 的组件,默认不可被外部应用访问,仅限应用内部使用,提升安全性,打开这类 Activity 需要根权限")
                                },
                                confirmButton = {
                                    TextButton(onClick = { showWarningDialog = false }) {
                                        Text(stringResource(R.string.confirm))
                                    }
                                },
                                dismissButton = {
                                    TextButton(onClick = { showWarningDialog = false }) {
                                        Text(stringResource(R.string.cancel))
                                    }
                                }
                            )
                        }
                    } else {
                        Icon(
                            imageVector = Icons.Filled.Info,
                            contentDescription = null
                        )
                    }
                }
            }
    }

    private fun extracted(exp: Boolean, appInfo: ActivityInfo) {
        if (exp) {
            val packageName = appInfo.packageName  // 目标应用的包名
            val className = appInfo.name  // 目标 Activity 的完整类名

            val intent = Intent().apply {
                setClassName(packageName, className)
            }

            try {
                startActivity(intent)
            } catch (_: Exception) {
                Toast.makeText(
                    this@AppsListActivity,
                    "无法启动目标应用",
                    Toast.LENGTH_SHORT
                ).show()
            }
        } else {
            Toast.makeText(
                this@AppsListActivity,
                "Activity 未导出",
                Toast.LENGTH_SHORT
            ).show()
        }
    }
}