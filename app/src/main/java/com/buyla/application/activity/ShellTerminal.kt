package com.buyla.application.activity

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusTarget
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.buyla.application.ui.theme.MyAppTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ShellTerminal : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()
        window.isNavigationBarContrastEnforced = false

        setContent {
            MyAppTheme {
                ShTerminal()
            }
        }
    }

    @SuppressLint("CoroutineCreationDuringComposition")
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun ShTerminal() {
        val context = LocalContext.current
        val coroutineScope = rememberCoroutineScope()
        val focusRequester = remember { FocusRequester() }
        val scrollState = rememberLazyListState()
        var commandInput by remember { mutableStateOf("") }
        val history = remember { mutableStateListOf<TerminalItem>() }

        // 启动持久 Shell 进程
        val process = remember {
            ProcessBuilder("sh").start()
        }
        val inputStream = remember { process.outputStream }
        val outputStream = remember { process.inputStream.bufferedReader() }
        val errorStream = remember { process.errorStream.bufferedReader() }

        // 实时读取输出流
        LaunchedEffect(Unit) {
            coroutineScope.launch(Dispatchers.IO) {
                while (true) {
                    val line = outputStream.readLine() ?: break
                    withContext(Dispatchers.Main) {
                        history.add(TerminalItem.Output(line))

                    }
                }
            }

            coroutineScope.launch(Dispatchers.IO) {
                while (true) {
                    val line = errorStream.readLine() ?: break
                    withContext(Dispatchers.Main) {
                        history.add(TerminalItem.Error(line))
                    }
                }
            }
        }

        // 执行命令
        fun executeCommand(command: String) {
            coroutineScope.launch(Dispatchers.IO) {
                history.add(TerminalItem.Command(command))
                inputStream.write("$command\n".toByteArray())
                inputStream.flush()
            }
            if (command == "clear") {
                history.clear()
            }
            if (command == "exit") {
                history.add(TerminalItem.Command("Shell 进程退出"))
                process.destroy()
            }
        }

        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    title = { Text("Shell") },
                    navigationIcon = {
                        IconButton(onClick = { (context as? ComponentActivity)?.finish() }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "返回"
                            )
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background,
                    )
                )
            },
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .padding(top = innerPadding.calculateTopPadding(), bottom = innerPadding.calculateBottomPadding(), start = 16.dp, end = 16.dp)
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
            ) {
                // 输出显示区域
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .border(width = 2.dp, color = MaterialTheme.colorScheme.primary, shape = MaterialTheme.shapes.large)
                        .clip(MaterialTheme.shapes.large),
                    state = scrollState,
                ) {
                    if (history.isEmpty()){
                        item { Text("输入一个命令并执行", color = MaterialTheme.colorScheme.outline, modifier = Modifier.padding(16.dp)) }
                    }
                    itemsIndexed(history) { _, item ->
                        when (item) {
                            is TerminalItem.Command -> CommandText(item.text)
                            is TerminalItem.Output -> OutputText(item.text)
                            is TerminalItem.Error -> ErrorText(item.text)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                    }
                    item {
                        OutlinedTextField(
                            value = commandInput,
                            onValueChange = { commandInput = it },
                            modifier = Modifier
                                .fillMaxWidth()
                                .focusRequester(focusRequester)
                                .focusTarget(),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                            keyboardActions = KeyboardActions(
                                onSend = {
                                    if (commandInput.isNotBlank()) {
                                        executeCommand(commandInput)
                                        commandInput = ""
                                    }
                                }
                            ),
                            singleLine = true,
                            shape = MaterialTheme.shapes.large,
                            colors = TextFieldDefaults.colors(
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent,
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent
                            ),

                        )
                        LaunchedEffect(Unit) {
                            focusRequester.requestFocus()
                        }
                    }
                }


            }
        }

        LaunchedEffect(Unit) {
            focusRequester.requestFocus()
        }
    }

    sealed class TerminalItem {
        data class Command(val text: String) : TerminalItem()
        data class Output(val text: String) : TerminalItem()
        data class Error(val text: String) : TerminalItem()
    }

    @Composable
    private fun CommandText(text: String) {
        Text(
            text = "$ $text",
            color = MaterialTheme.colorScheme.primary,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(start = 4.dp)
        )
    }

    @Composable
    private fun OutputText(text: String) {
        Text(
            text = text,
            color = MaterialTheme.colorScheme.onBackground,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(start = 4.dp)
        )
    }

    @Composable
    private fun ErrorText(text: String) {
        Text(
            text = text,
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(start = 4.dp)
        )
    }
}