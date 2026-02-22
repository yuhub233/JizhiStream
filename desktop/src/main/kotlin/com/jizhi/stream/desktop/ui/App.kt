package com.jizhi.stream.desktop.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.jizhi.stream.core.config.AppSettings
import com.jizhi.stream.core.config.StreamConfig
import com.jizhi.stream.core.engine.StreamReceiver
import com.jizhi.stream.core.engine.StreamSender
import com.jizhi.stream.core.network.*
import com.jizhi.stream.core.security.AuthManager
import com.jizhi.stream.desktop.capture.DesktopScreenCapture
import kotlinx.coroutines.*

@Composable
fun App() {
    var settings by remember { mutableStateOf(AppSettings.load()) }
    var currentPage by remember { mutableStateOf("home") }
    val scope = rememberCoroutineScope()

    val authManager = remember { AuthManager(settings.password) }
    val discovery = remember {
        DiscoveryService(settings.deviceName, "Windows").apply { start(scope) }
    }
    var devices by remember { mutableStateOf(listOf<DeviceInfo>()) }
    var streaming by remember { mutableStateOf(false) }
    var statusText by remember { mutableStateOf("就绪") }
    var serverRunning by remember { mutableStateOf(false) }

    val capture = remember { DesktopScreenCapture() }
    var sender: StreamSender? by remember { mutableStateOf(null) }
    val receiver = remember { StreamReceiver() }

    var controlServer: ControlServer? by remember { mutableStateOf(null) }

    LaunchedEffect(Unit) {
        discovery.onDeviceFound = { devices = discovery.getDevices() }
        while (true) {
            devices = discovery.getDevices()
            delay(3000)
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            discovery.stop()
            capture.stop()
            sender?.stop()
            receiver.stop()
            controlServer?.stop()
        }
    }

    fun startServer() {
        if (serverRunning) return
        controlServer = ControlServer(authManager) { config, socket ->
            val targetIp = socket.inetAddress.hostAddress ?: return@ControlServer
            statusText = "收到串流请求: ${config.width}x${config.height}"
            sender = StreamSender(targetIp).apply { start() }
            capture.start(config.width, config.height, config.fps) { frame ->
                sender?.sendFrame(frame)
            }
            streaming = true
        }
        controlServer?.start(scope)
        serverRunning = true
        statusText = "服务已启动，等待连接..."
    }

    fun stopServer() {
        capture.stop()
        sender?.stop()
        sender = null
        controlServer?.stop()
        controlServer = null
        serverRunning = false
        streaming = false
        statusText = "服务已停止"
    }

    fun connectTo(device: DeviceInfo) {
        scope.launch(Dispatchers.IO) {
            statusText = "正在连接 ${device.name}..."
            val client = ControlClient(authManager)
            if (client.connect(device.ip)) {
                statusText = "已连接 ${device.name}，请求串流..."
                receiver.onFrameReady = { _, _ -> }
                receiver.start(scope)
                client.requestStream(settings.streamConfig)
                streaming = true
                statusText = "正在接收 ${device.name} 的画面"
            } else {
                statusText = "连接失败: 密码错误或设备拒绝"
            }
        }
    }

    val allDevices = remember(devices, settings.manualDevices) {
        val manual = settings.manualDevices.map { DeviceInfo(it.name, it.ip, "手动") }
        (devices + manual).distinctBy { it.ip }
    }

    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Color(0xFF6C63FF),
            surface = Color(0xFF1E1E2E),
            background = Color(0xFF11111B),
            onSurface = Color(0xFFCDD6F4),
            onBackground = Color(0xFFCDD6F4)
        )
    ) {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Column(modifier = Modifier.fillMaxSize()) {
                TopBar(currentPage) { currentPage = it }
                when (currentPage) {
                    "home" -> HomePage(
                        devices = allDevices,
                        serverRunning = serverRunning,
                        streaming = streaming,
                        statusText = statusText,
                        onStartServer = ::startServer,
                        onStopServer = ::stopServer,
                        onConnect = ::connectTo,
                        onRefresh = { devices = discovery.getDevices() }
                    )
                    "settings" -> SettingsPage(settings) {
                        settings = it
                        AppSettings.save(it)
                    }
                }
            }
        }
    }
}
