package com.buyla.application.activity

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Help
import androidx.compose.material.icons.automirrored.outlined.Help
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.outlined.Apps
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import com.buyla.application.ui.screen.AllSettings
import com.buyla.application.ui.screen.Apps
import com.buyla.application.ui.screen.BrowseFile
import com.buyla.application.ui.screen.Home
import com.buyla.application.ui.screen.Info
import com.buyla.application.ui.theme.MyAppTheme
import com.buyla.application.util.Util
import java.io.File

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        window.isNavigationBarContrastEnforced = false

        val requestPermissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ){}

        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.READ_EXTERNAL_STORAGE
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
        }

        setContent {
            MyAppTheme {
                MainApp(this@MainActivity)
            }
        }

        Util.isSuperUser = Util.checkSuAvailable()

        if (!Environment.isExternalStorageManager()) {
            val intent = Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
            startActivity(intent)
            return
        }

        val sdcardPath = Environment.getExternalStorageDirectory().path + "/BuylaBox"
        val folder = File(sdcardPath)
        if (!folder.exists()) {
            folder.mkdirs()
        }
    }

    @Composable
    fun MainApp(context: Context) {
        var selectedItem by remember { mutableIntStateOf(2) }
        val items = listOf("卡片", "信息", "文件", "应用", "占位")

        val appPath = context.filesDir.absolutePath + "/extract_zip"
        val appFolder = File(appPath)
        if (!appFolder.exists()) {
            appFolder.mkdirs()
        }
        Scaffold(
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            bottomBar = {
                NavigationBar {
                    items.forEachIndexed { index, item ->
                        NavigationBarItem(
                            selected = selectedItem == index,
                            onClick = { selectedItem = index },
                            icon = {
                                val iconRes = when (index) {
                                    0 -> if (selectedItem == index) Icons.Filled.Home else Icons.Outlined.Home
                                    1 -> if (selectedItem == index) Icons.Filled.Info else Icons.Outlined.Info
                                    2 -> if (selectedItem == index) Icons.Filled.Description else Icons.Outlined.Description
                                    3 -> if (selectedItem == index) Icons.Filled.Apps else Icons.Outlined.Apps
                                    else -> if (selectedItem == index) Icons.AutoMirrored.Filled.Help else Icons.AutoMirrored.Outlined.Help
                                }
                                Icon(
                                    imageVector = iconRes,
                                    contentDescription = item
                                )
                            },
                            label = {
                                AnimatedVisibility(
                                    visible = selectedItem == index,
                                    enter = expandVertically(),
                                    exit = shrinkVertically()
                                ) {
                                    Text(item)
                                }
                            }
                        )
                    }
                }
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier.Companion
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                AnimatedContent(
                    modifier = Modifier.Companion.fillMaxSize(),
                    targetState = selectedItem,
                    transitionSpec = {
                        fadeIn() togetherWith
                                fadeOut()
                    }
                ) { target ->
                    when (target) {
                        0 -> {
                            Home.HomeScreen()
                        }

                        1 -> {
                            Info.InfoScreen()
                        }

                        2 -> {
                            BrowseFile.FileScreen(context = context)
                        }

                        3 -> {
                            Apps.AppScreen(context = context)
                        }

                        4 -> {
                            AllSettings.SettingsScreen()
                        }
                    }
                }
            }
        }
    }
}