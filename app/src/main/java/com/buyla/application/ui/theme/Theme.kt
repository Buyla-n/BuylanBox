package com.buyla.application.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

@Composable
fun MyAppTheme(
    content: @Composable () -> Unit
) {
    val isSystemDarkTheme = isSystemInDarkTheme()

    val context = LocalContext.current

    val colors = if (isSystemDarkTheme) {
        dynamicDarkColorScheme(context)
    } else {
        dynamicLightColorScheme(context)
    }

    MaterialTheme(
        colorScheme = colors,
        content = content
    )
}