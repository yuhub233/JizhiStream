package com.jizhi.stream.desktop

import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.jizhi.stream.desktop.ui.App

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "极致串流 - JizhiStream",
        state = rememberWindowState(width = 900.dp, height = 680.dp)
    ) {
        App()
    }
}
