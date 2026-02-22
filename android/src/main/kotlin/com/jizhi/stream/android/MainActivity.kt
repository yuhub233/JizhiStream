package com.jizhi.stream.android

import android.app.Activity
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import com.jizhi.stream.android.service.ScreenCaptureService
import com.jizhi.stream.android.ui.AndroidApp
import com.jizhi.stream.core.config.AppSettings
import kotlinx.coroutines.*

class MainActivity : ComponentActivity() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var onProjectionGranted: (() -> Unit)? = null

    private val projectionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            ScreenCaptureService.resultCode = result.resultCode
            ScreenCaptureService.resultData = result.data
            startForegroundService(Intent(this, ScreenCaptureService::class.java))
            onProjectionGranted?.invoke()
            onProjectionGranted = null
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppSettings.init(filesDir)
        setContent {
            AndroidApp(
                onRequestProjection = { width, height, fps, callback ->
                    ScreenCaptureService.captureWidth = width
                    ScreenCaptureService.captureHeight = height
                    ScreenCaptureService.captureFps = fps
                    onProjectionGranted = callback
                    val mpm = getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
                    projectionLauncher.launch(mpm.createScreenCaptureIntent())
                },
                onStopCapture = {
                    ScreenCaptureService.instance?.stopCapture()
                    stopService(Intent(this, ScreenCaptureService::class.java))
                }
            )
        }
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }
}
