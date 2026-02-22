package com.jizhi.stream.android.service

import android.app.*
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.IBinder
import com.jizhi.stream.core.engine.ScreenCapture
import java.io.ByteArrayOutputStream

class ScreenCaptureService : Service() {
    private var projection: MediaProjection? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var imageReader: ImageReader? = null
    private var running = false

    companion object {
        var instance: ScreenCaptureService? = null
        var resultCode: Int = 0
        var resultData: Intent? = null
        var captureWidth = 1920
        var captureHeight = 1080
        var captureFps = 60
        var onFrameCallback: ((ByteArray) -> Unit)? = null
        const val CHANNEL_ID = "jizhi_stream_capture"
        const val NOTIFICATION_ID = 1001
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        instance = this
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val notification = buildNotification()
        startForeground(NOTIFICATION_ID, notification)
        startCapture()
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        stopCapture()
        instance = null
        super.onDestroy()
    }

    private fun startCapture() {
        if (running) return
        val data = resultData ?: return
        val mpm = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        projection = mpm.getMediaProjection(resultCode, data)

        imageReader = ImageReader.newInstance(captureWidth, captureHeight, PixelFormat.RGBA_8888, 2)
        virtualDisplay = projection?.createVirtualDisplay(
            "JizhiStream",
            captureWidth, captureHeight, resources.displayMetrics.densityDpi,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            imageReader!!.surface, null, null
        )

        imageReader?.setOnImageAvailableListener({ reader ->
            val image = reader.acquireLatestImage() ?: return@setOnImageAvailableListener
            try {
                val planes = image.planes
                val buffer = planes[0].buffer
                val pixelStride = planes[0].pixelStride
                val rowStride = planes[0].rowStride
                val rowPadding = rowStride - pixelStride * captureWidth

                val bitmap = Bitmap.createBitmap(
                    captureWidth + rowPadding / pixelStride,
                    captureHeight, Bitmap.Config.ARGB_8888
                )
                bitmap.copyPixelsFromBuffer(buffer)

                val cropped = if (rowPadding > 0) {
                    Bitmap.createBitmap(bitmap, 0, 0, captureWidth, captureHeight)
                } else bitmap

                val baos = ByteArrayOutputStream(captureWidth * captureHeight / 4)
                cropped.compress(Bitmap.CompressFormat.JPEG, 85, baos)
                onFrameCallback?.invoke(baos.toByteArray())

                if (cropped !== bitmap) cropped.recycle()
                bitmap.recycle()
            } finally {
                image.close()
            }
        }, null)

        running = true
    }

    fun stopCapture() {
        running = false
        virtualDisplay?.release()
        imageReader?.close()
        projection?.stop()
        virtualDisplay = null
        imageReader = null
        projection = null
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(CHANNEL_ID, "屏幕捕获", NotificationManager.IMPORTANCE_LOW)
            val nm = getSystemService(NotificationManager::class.java)
            nm.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(): Notification {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(this, CHANNEL_ID)
                .setContentTitle("极致串流")
                .setContentText("正在共享屏幕")
                .setSmallIcon(android.R.drawable.ic_menu_camera)
                .build()
        } else {
            @Suppress("DEPRECATION")
            Notification.Builder(this)
                .setContentTitle("极致串流")
                .setContentText("正在共享屏幕")
                .setSmallIcon(android.R.drawable.ic_menu_camera)
                .build()
        }
    }
}
