package com.buyla.application.activity

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.buyla.application.ui.theme.MyAppTheme

class FileSettings : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()
        window.isNavigationBarContrastEnforced = false

        setContent {
            MyAppTheme {
                //MainScreen(this)
            }
        }
    }
//    @OptIn(ExperimentalMaterial3Api::class)
//    @Composable
//    fun MainScreen(context: Context) {
//        // 下拉菜单状态
//        var themeExpanded by remember { mutableStateOf(false) }
//        var selectedTheme by remember { mutableStateOf("系统默认") }
//
//        var languageExpanded by remember { mutableStateOf(false) }
//        var selectedLanguage by remember { mutableStateOf("简体中文") }
//
//        var authExpanded by remember { mutableStateOf(false) }
//
//        var storageExpanded by remember { mutableStateOf(false) }
//        var selectedStorage by remember { mutableStateOf("内部存储") }
//
//        // 开关状态
//        var notificationsEnabled by remember { mutableStateOf(true) }
//
//        Scaffold(
//            topBar = {
//                CenterAlignedTopAppBar(
//                    title = {
//                        Text("文件设置", Modifier.fillMaxWidth().padding(start = 8.dp))
//                    },
//                    navigationIcon = {
//                        IconButton(onClick = {
//                            (context as? ComponentActivity)?.finish()
//                        }) {
//                            Icon(
//                                imageVector = ImageVector.vectorResource(R.drawable.baseline_arrow_back_24),
//                                contentDescription = "返回"
//                            )
//                        }
//                    },
//                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
//                        containerColor = MaterialTheme.colorScheme.background,
//                    )
//                )
//            },
//        ) { innerPadding ->
//            LazyColumn(
//                modifier = Modifier
//                    .padding(top = innerPadding.calculateTopPadding())
//                    .fillMaxSize()
//            ) {
//                item { Spacer(modifier = Modifier.height(8.dp)) }
//
//                // 主题设置
//                item {
//                    DropdownSettingItem(
//                        title = "主题设置",
//                        //icon = ImageVector.vectorResource(R.drawable.twotone_palette_24),
//                        selectedValue = selectedTheme,
//                        expanded = themeExpanded,
//                        onExpandedChange = { themeExpanded = it },
//                        items = listOf("系统默认", "浅色主题", "深色主题", "自动切换")
//                    ) { selectedTheme = it }
//                    HorizontalDivider()
//                }
//
//                // 语言设置
//                item {
//                    DropdownSettingItem(
//                        title = "界面语言",
//                        icon = ImageVector.vectorResource(R.drawable.twotone_language_24),
//                        selectedValue = selectedLanguage,
//                        expanded = languageExpanded,
//                        onExpandedChange = { languageExpanded = it },
//                        items = listOf("简体中文", "English", "日本語", "Español")
//                    ) { selectedLanguage = it }
//                    HorizontalDivider()
//                }
//
//                // 字体大小
//                item {
//                    DropdownSettingItem(
//                        title = "访问权限",
//                        icon = ImageVector.vectorResource(R.drawable.round_lock_open_24),
//                        selectedValue = selectedAuth,
//                        expanded = authExpanded,
//                        onExpandedChange = { authExpanded = it },
//                        items = listOf("Shizuku", "Root", "Default")
//                    ) { selectedAuth = it }
//                    HorizontalDivider()
//                }
//
//                // 存储位置
//                item {
//                    DropdownSettingItem(
//                        title = "存储位置",
//                        icon = ImageVector.vectorResource(R.drawable.round_storage_24),
//                        selectedValue = selectedStorage,
//                        expanded = storageExpanded,
//                        onExpandedChange = { storageExpanded = it },
//                        items = listOf("内部存储", "SD卡", "云存储")
//                    ) { selectedStorage = it }
//                    HorizontalDivider()
//                }
//
//                // 通知开关
//                item {
//                    Row(
//                        modifier = Modifier
//                            .fillMaxWidth()
//                            .clickable { notificationsEnabled = !notificationsEnabled }
//                            .padding(16.dp),
//                        verticalAlignment = Alignment.CenterVertically,
//                        horizontalArrangement = Arrangement.SpaceBetween
//                    ) {
//                        Row(verticalAlignment = Alignment.CenterVertically) {
//                            Spacer(modifier = Modifier.width(16.dp))
//                            Text("启用通知", style = MaterialTheme.typography.bodyLarge)
//                        }
//                        Switch(
//                            checked = notificationsEnabled,
//                            onCheckedChange = { notificationsEnabled = it }
//                        )
//                    }
//                    HorizontalDivider()
//                }
//            }
//        }
//    }
//
//    @OptIn(ExperimentalMaterial3Api::class)
//    @Composable
//    private fun DropdownSettingItem(
//        title: String,
//        icon: ImageVector,
//        selectedValue: String,
//        expanded: Boolean,
//        onExpandedChange: (Boolean) -> Unit,
//        items: List<String>,
//        onItemSelected: (String) -> Unit
//    ) {
//        ExposedDropdownMenuBox(
//            expanded = expanded,
//            onExpandedChange = onExpandedChange
//        ) {
//            Row(
//                modifier = Modifier
//                    .fillMaxWidth()
//                    .menuAnchor(MenuAnchorType.PrimaryEditable, true)
//                    .clickable { onExpandedChange(!expanded) }
//                    .padding(16.dp),
//                verticalAlignment = Alignment.CenterVertically,
//                horizontalArrangement = Arrangement.SpaceBetween
//            ) {
//                Row(verticalAlignment = Alignment.CenterVertically) {
//                    Icon(
//                        imageVector = icon,
//                        contentDescription = null,
//                        modifier = Modifier.size(24.dp)
//                    )
//                    Spacer(modifier = Modifier.width(16.dp))
//                    Text(text = title, style = MaterialTheme.typography.bodyLarge)
//                }
//
//                Row(verticalAlignment = Alignment.CenterVertically) {
//                    Text(
//                        text = selectedValue,
//                        style = MaterialTheme.typography.bodyMedium,
//                        color = MaterialTheme.colorScheme.onSurfaceVariant
//                    )
//                    Spacer(modifier = Modifier.width(8.dp))
//                    Icon(
//                        imageVector = if (expanded) ImageVector.vectorResource(R.drawable.baseline_arrow_drop_up_24)
//                        else ImageVector.vectorResource(R.drawable.baseline_arrow_drop_down_24),
//                        contentDescription = null
//                    )
//                }
//            }
//
//            Box(
//                modifier = Modifier
//                    .fillMaxWidth()
//                    .wrapContentSize(Alignment.TopEnd)
//                    .padding(top = 56.dp)
//            ) {
//                ExposedDropdownMenu(
//                    expanded = expanded,
//                    onDismissRequest = { onExpandedChange(false) },
//                    Modifier.width(128.dp)
//                ) {
//                    items.forEach { item ->
//                        DropdownMenuItem(
//                            text = { Text(item) },
//                            onClick = {
//                                onItemSelected(item)
//                                onExpandedChange(false)
//                            }
//                        )
//                    }
//                }
//            }
//
//        }
//    }
}