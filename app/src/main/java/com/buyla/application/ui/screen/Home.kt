package com.buyla.application.ui.screen

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.rounded.Terminal
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import com.buyla.application.activity.ShellTerminal


object Home {

    @OptIn(ExperimentalMaterial3Api::class)
    @SuppressLint("UnusedMaterial3ScaffoldPaddingParameter", "SdCardPath")
    @Composable
    fun HomeScreen() {
        val folder = java.io.File("/sdcard/BuylaBox")
        if (!folder.exists()) {
            folder.mkdirs()
        }

        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    title = { Text(text = "BuylaBox", modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 8.dp),style = MaterialTheme.typography.titleLarge) },
                )
            },
        ) { innerPadding ->
            // 布局
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.Top,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                item {
                    ToolCard()
                    FloatWindowCard()
                    InfoCard()

                }
            }
        }

    }

    private fun openWebPage(context: Context) {
        val url = "https://www.coolapk.com/u/23065765"
        val intent = Intent(Intent.ACTION_VIEW, url.toUri())
        context.startActivity(intent)
    }


    @Composable
    fun ToolCard() {
        val context = LocalContext.current
        Column {
            Button(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
                    .height(72.dp),
                onClick = {
                    context.startActivity(
                        Intent(context, ShellTerminal::class.java)
                    )
                },
                shape = MaterialTheme.shapes.medium
            ) {
                Row(
                    horizontalArrangement = Arrangement.Start,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxHeight()
                    ) {
                        Icon(imageVector = Icons.Rounded.Terminal, contentDescription = "Shell")
                    }
                    Column(
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier
                            .fillMaxHeight()
                            .padding(start = 24.dp)
                    ){
                        Text("Shell 终端")
                        Text("执行 Shell 命令的好地方")
                    }

                }
            }
        }
    }

    @Composable
    fun FloatWindowCard() {
        Column {
            Button(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
                    .height(72.dp),
                onClick = {
                },
                shape = MaterialTheme.shapes.medium
            ) {
                Row(
                    horizontalArrangement = Arrangement.Start,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxHeight()
                    ) {
                        Icon(imageVector = Icons.Filled.AcUnit, contentDescription = "Shell")
                    }
                    Column(
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier
                            .fillMaxHeight()
                            .padding(start = 24.dp)
                    ){
                        Text("测试卡片")
                        Text("卡片用于测试")
                    }
                }
            }
        }
    }

    @Composable
    fun InfoCard() {
        val context = LocalContext.current
        ElevatedCard(
            modifier = Modifier
                .clickable { openWebPage(context) }
                .fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "提交建议",
                    style = MaterialTheme.typography.bodyLarge
                )
                Text(
                    text = "将跳转入酷安主页",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}