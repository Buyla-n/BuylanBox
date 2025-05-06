package com.buyla.application.activity

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.buyla.application.parser.axml.AXMLPrinter
import com.buyla.application.ui.theme.MyAppTheme
import com.buyla.application.util.Util.isSuperUser
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import kotlin.math.roundToInt

class TextEditor : ComponentActivity() {
    private lateinit var filePath: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()
        window.isNavigationBarContrastEnforced = false

        filePath =
            intent.getStringExtra("filePath") ?: run {
                finish() // 如果没有文件路径，直接关闭Activity
                return
            }

        setContent {
            MyAppTheme {
                EditScreen(filePath)
            }
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun EditScreen(filePath: String) {
        val context = LocalContext.current
        var text by remember { mutableStateOf("") }
        var scrollState = rememberScrollState()
        val coroutineScope = rememberCoroutineScope()
        val configuration = LocalConfiguration.current

        var isLoading by remember { mutableStateOf(true) }

        // 异步读取文件内容

        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    title = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Start,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            IconButton(onClick = {
                                    (context as? ComponentActivity)?.finish()
                            }) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "返回"
                                )
                            }

                            Text(
                                text = "文本编辑",
                                modifier = Modifier.padding(start = 8.dp)
                            )
                        }
                    },
                    actions = {
                    }
                )
            }
        ) { innerPadding ->
            LaunchedEffect(filePath) {
                coroutineScope.launch {
                    if (!filePath.endsWith("xml")) {
                        if (!isSuperUser) {
                            text = File(filePath).readText()
                            isLoading = false
                        } else {
                            val process =
                                Runtime.getRuntime().exec(arrayOf("su", "-c", "cat \"$filePath\""))
                            val reader = BufferedReader(InputStreamReader(process.inputStream))
                            val content = reader.use { it.readText() }
                            process.waitFor()
                            text = content
                            isLoading = false

                        }
                    } else {
                        text = AXMLPrinter.main(arrayOf(filePath))
                        isLoading = false
                    }
                }
            }
            AnimatedContent(
                targetState = isLoading,
                contentAlignment = Alignment.Center
            ) { it ->

                when(it) {

                    false -> {
                        val lazyListState = rememberLazyListState()
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(innerPadding)
                        ) {
                            Row {
                                val lineCount =
                                    remember { derivedStateOf { text.count { it == '\n' } + 1 } }

                                Column(
                                    modifier = Modifier
                                        .width(30.dp)  // 行号列宽度
                                        .fillMaxHeight()
                                        .verticalScroll(state = scrollState)
                                        .padding(end = 4.dp),
                                    verticalArrangement = Arrangement.Top
                                ) {
                                    repeat(lineCount.value) { index ->
                                        Text(
                                            text = "${index + 1}",
                                            modifier = Modifier.fillMaxWidth(),
                                            textAlign = TextAlign.End,
                                            color = Color.Gray,
                                            fontSize = 12.sp,
                                            maxLines = 1,
                                            lineHeight = 20.sp
                                        )
                                    }
                                }
                                LazyColumn(
                                    state = lazyListState
                                ) {
                                    item {
                                        BasicTextField(
                                            value = text,
                                            onValueChange = { text = it },
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .horizontalScroll(state = rememberScrollState()),
                                            textStyle = TextStyle(lineHeight = 20.sp)
                                        )
                                    }
                                }
                                LaunchedEffect(lazyListState) {
                                    snapshotFlow { lazyListState.firstVisibleItemScrollOffset }.collect { offset ->
                                        scrollState.scrollTo(offset)
                                    }
                                }
                            }
                            Slider(
                                value = scrollState.value.toFloat(),
                                onValueChange = {
                                    coroutineScope.launch {
                                        scrollState.scrollTo(it.roundToInt())
                                        val currentOffset = lazyListState.firstVisibleItemIndex +
                                                lazyListState.firstVisibleItemScrollOffset
                                        val scrollDelta = it - currentOffset
                                        lazyListState.scrollBy((scrollDelta).toFloat()) // 使用 scrollBy 滚动
                                    }
                                },
                                valueRange = 0f..scrollState.maxValue.toFloat(),
                                modifier = Modifier
                                    .rotate(if (configuration.orientation == 1) 90f else 0f)  // 旋转90度使其垂直
                                    .align(Alignment.CenterEnd)
                                    .offset(y = if (configuration.orientation == 1) (-150).dp else 0.dp)
                                    .alpha(0.6f),
                                colors = SliderDefaults.colors(
                                    thumbColor = Color.Transparent
                                )
                            )
                        }
                    }

                    true -> {
                        CircularProgressIndicator(modifier = Modifier)
                    }
                }
            }
        }
    }
}