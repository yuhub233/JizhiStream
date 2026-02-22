package com.jizhi.stream.desktop.capture

import com.jizhi.stream.core.engine.ScreenCapture
import java.awt.Rectangle
import java.awt.Robot
import java.awt.Toolkit
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import javax.imageio.ImageIO
import javax.imageio.IIOImage
import javax.imageio.ImageWriteParam

class DesktopScreenCapture : ScreenCapture {
    private var running = false
    private var captureThread: Thread? = null

    override fun start(width: Int, height: Int, fps: Int, onFrame: (ByteArray) -> Unit) {
        if (running) return
        running = true
        captureThread = Thread {
            val robot = Robot()
            val screenSize = Toolkit.getDefaultToolkit().screenSize
            val rect = Rectangle(0, 0, screenSize.width, screenSize.height)
            val interval = 1000L / fps

            while (running) {
                val start = System.currentTimeMillis()
                try {
                    val screenshot = robot.createScreenCapture(rect)
                    val scaled = if (screenshot.width != width || screenshot.height != height) {
                        val img = BufferedImage(width, height, BufferedImage.TYPE_3BYTE_BGR)
                        val g = img.createGraphics()
                        g.drawImage(screenshot, 0, 0, width, height, null)
                        g.dispose()
                        img
                    } else screenshot

                    val baos = ByteArrayOutputStream(width * height)
                    val writer = ImageIO.getImageWritersByFormatName("jpg").next()
                    val param = writer.defaultWriteParam.apply {
                        compressionMode = ImageWriteParam.MODE_EXPLICIT
                        compressionQuality = 0.85f
                    }
                    writer.output = ImageIO.createImageOutputStream(baos)
                    writer.write(null, IIOImage(scaled, null, null), param)
                    writer.dispose()

                    onFrame(baos.toByteArray())
                } catch (_: Exception) {}

                val elapsed = System.currentTimeMillis() - start
                val sleep = interval - elapsed
                if (sleep > 0) Thread.sleep(sleep)
            }
        }.apply {
            isDaemon = true
            name = "ScreenCapture"
            start()
        }
    }

    override fun stop() {
        running = false
        captureThread?.join(2000)
    }

    override fun isRunning(): Boolean = running
}
