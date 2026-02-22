package com.jizhi.stream.core.engine

interface ScreenCapture {
    fun start(width: Int, height: Int, fps: Int, onFrame: (ByteArray) -> Unit)
    fun stop()
    fun isRunning(): Boolean
}
