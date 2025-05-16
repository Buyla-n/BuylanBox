package com.buyla.application.activity

import android.graphics.Typeface
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredWidthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.buyla.application.ui.theme.MyAppTheme
import java.io.File

class FontPreview : ComponentActivity() {
    private lateinit var filePath: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()
        window.isNavigationBarContrastEnforced = false

        filePath =
            intent.getStringExtra("filePath") ?: run {
                finish()
                return
            }

        setContent {
            MyAppTheme {
                FontView(filePath)
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
    @Composable
    fun FontView(filePath: String) {
        val context = LocalContext.current
        val fontFamily = remember { mutableStateOf<FontFamily?>(null) }
        val isLoading = remember { mutableStateOf(true) }
        val error = remember { mutableStateOf<String?>(null) }

        // 加载字体文件
        LaunchedEffect(filePath) {
            val typeface = try {
                Typeface.createFromFile(File(filePath))
            } catch (_: Exception) {
                null
            }

            if (typeface != null) {
                fontFamily.value = FontFamily(typeface)
                isLoading.value = false
            } else {
                error.value = "无法加载字体文件"
                isLoading.value = false
            }
        }
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
                                    imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                                    contentDescription = "返回"
                                )
                            }
                            Text(
                                text = File(filePath).name,
                                modifier = Modifier.padding(start = 8.dp).requiredWidthIn(max = 130.dp),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    },
                    actions = {
                    }
                )
            }
        ) { innerPadding ->
            Box(modifier = Modifier.padding(innerPadding), contentAlignment = Alignment.Center) {
                when {
                    isLoading.value ->  CircularProgressIndicator()
                    error.value != null ->  Text(text = error.value!!, color = MaterialTheme.colorScheme.error)
                    else -> FontPreviewContent(fontFamily.value!!)
                }
            }
        }
    }

    @Composable
    private fun FontPreviewContent(fontFamily: FontFamily) {
        val textStyles = listOf(
            "Small" to TextStyle(fontFamily = fontFamily, fontSize = 12.sp),
            "Default" to TextStyle(fontFamily = fontFamily),
            "Medium" to TextStyle(fontFamily = fontFamily, fontSize = 16.sp),
            "Large" to TextStyle(fontFamily = fontFamily, fontSize = 24.sp),
            "Bold" to TextStyle(fontFamily = fontFamily, fontWeight = FontWeight.Bold),
            "Italic" to TextStyle(fontFamily = fontFamily, fontStyle = FontStyle.Italic),
            "Bold Italic" to TextStyle(
                fontFamily = fontFamily,
                fontWeight = FontWeight.Bold,
                fontStyle = FontStyle.Italic
            )
        )

        val sampleText = "Innovation in China 中国智造，慧及全球 0123456789"

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp)
        ) {
            // 显示字体名称和基本信息
            item {
                Text(
                    text = "字体预览",
                    style = MaterialTheme.typography.headlineMedium.copy(fontFamily = fontFamily),
                    modifier = Modifier.padding(bottom = 16.dp)
                )
            }

            item {
                Text(
                    text = "abcdefghijklmnopqrstuvwxyz ABCDEFGHIJKLMNOPQRSTUVWXYZ",
                    style = TextStyle(fontFamily = fontFamily, fontSize = 16.sp)
                )
            }

            item {
                Text(
                    text = "1234567890.:,;'\"(!?)+-*/=",
                    style = TextStyle(fontFamily = fontFamily, fontSize = 16.sp)
                )
            }

            // 显示各种文本样式
            item {
                Column {
                    textStyles.forEach { (name, style) ->
                        Text(
                            text = "$name: $sampleText",
                            style = style,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), thickness = 4.dp)
                    }
                }
            }
        }
    }

}