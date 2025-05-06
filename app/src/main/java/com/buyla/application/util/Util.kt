package com.buyla.application.util

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import java.io.BufferedReader
import java.io.InputStreamReader

object Util {
    fun copyToClipboard(context: Context, textToCopy: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("label", textToCopy)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(context, "文本已被复制到剪贴板", Toast.LENGTH_SHORT).show()
    }

    fun checkSuAvailable(): Boolean {
        var isSuAvailable = false
        try {
            // 执行 su -c "echo test" 命令
            val process = Runtime.getRuntime().exec(arrayOf("su", "-c", "echo test"))

            // 读取命令输出
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            val output = reader.readLine()

            // 等待命令执行完成
            process.waitFor()

            // 如果输出是 "test" 则说明 su 可用
            isSuAvailable = (output == "test")
        } catch (e: Exception) {
            // 如果出现异常（如找不到 su），则返回 false
            isSuAvailable = false
        }
        return isSuAvailable
    }

    val forbiddenCharacters = setOf('/', '\\', '*', '?', '"', '<', '>', '|', ':')
    var fileVertical by mutableFloatStateOf(4f)
    var selectedAuth by  mutableStateOf("Default")
    var isSuperUser by mutableStateOf(false)



}