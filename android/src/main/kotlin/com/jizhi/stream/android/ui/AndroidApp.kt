package com.jizhi.stream.android.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jizhi.stream.android.service.ScreenCaptureService
import com.jizhi.stream.core.config.AppSettings
import com.jizhi.stream.core.config.StreamConfig
import com.jizhi.stream.core.engine.StreamReceiver
import com.jizhi.stream.core.engine.StreamSender
import com.jizhi.stream.core.network.*
import com.jizhi.stream.core.security.AuthManager
import kotlinx.coroutines.*

@Composable
fun AndroidApp(
    onStartCapture: (Int, Int, Int, String) -> Unit,
    onStopCapture: () -> Unit
) {
    var settings by remember { mutableStateOf(AppSettings.load()) }
    var currentPage by remember { mutableStateOf("home") }
    val scope = rememberCoroutineScope()

    val authManager = remember { AuthManager(settings.password) }
    val discovery = remember {
        DiscoveryService(settings.deviceName, "Android").apply { start(scope) }
    }
    var devices by remember { mutableStateOf(listOf<DeviceInfo>()) }
    var streaming by remember { mutableStateOf(false) }
    var statusText by remember { mutableStateOf("就绪") }
    var serverRunning by remember { mutableStateOf(false) }

    var controlServer: ControlServer? by remember { mutableStateOf(null) }
    var sender: StreamSender? by remember { mutableStateOf(null) }

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
            sender?.stop()
            controlServer?.stop()
        }
    }

    fun startServer() {
        if (serverRunning) return
        controlServer = ControlServer(authManager) { config, socket ->
            statusText = "收到串流请求: ${config.width}x${config.height}"
            sender = StreamSender(socket.inetAddress.hostAddress).apply { start() }
            ScreenCaptureService.onFrameCallback = { frame -> sender?.sendFrame(frame) }
            onStartCapture(config.width, config.height, config.fps, socket.inetAddress.hostAddress)
            streaming = true
        }
        controlServer?.start(scope)
        serverRunning = true
        statusText = "服务已启动，等待连接..."
    }

    fun stopServer() {
        onStopCapture()
        sender?.stop()
        controlServer?.stop()
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
                val receiver = StreamReceiver()
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

    val darkColors = darkColorScheme(
        primary = Color(0xFF6C63FF),
        surface = Color(0xFF1E1E2E),
        background = Color(0xFF11111B),
        onSurface = Color(0xFFCDD6F4),
        onBackground = Color(0xFFCDD6F4)
    )

    MaterialTheme(colorScheme = darkColors) {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Column(modifier = Modifier.fillMaxSize()) {
                AndroidTopBar(currentPage) { currentPage = it }
                when (currentPage) {
                    "home" -> AndroidHomePage(
                        devices = devices,
                        serverRunning = serverRunning,
                        streaming = streaming,
                        statusText = statusText,
                        onStartServer = ::startServer,
                        onStopServer = ::stopServer,
                        onConnect = ::connectTo,
                        onRefresh = { devices = discovery.getDevices() }
                    )
                    "settings" -> AndroidSettingsPage(settings) {
                        settings = it
                        AppSettings.save(it)
                    }
                }
            }
        }
    }
}

@Composable
private fun AndroidTopBar(currentPage: String, onNavigate: (String) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth()
            .background(Color(0xFF181825))
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("极致串流", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color(0xFF6C63FF))
        Spacer(Modifier.weight(1f))
        AndroidTabBtn("主页", currentPage == "home") { onNavigate("home") }
        Spacer(Modifier.width(8.dp))
        AndroidTabBtn("设置", currentPage == "settings") { onNavigate("settings") }
    }
}

@Composable
private fun AndroidTabBtn(text: String, selected: Boolean, onClick: () -> Unit) {
    val bg = if (selected) Color(0xFF6C63FF) else Color.Transparent
    val fg = if (selected) Color.White else Color(0xFF6C7086)
    Box(
        modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(bg)
            .clickable(onClick = onClick).padding(horizontal = 14.dp, vertical = 6.dp)
    ) { Text(text, color = fg, fontSize = 14.sp) }
}

@Composable
private fun AndroidHomePage(
    devices: List<DeviceInfo>,
    serverRunning: Boolean,
    streaming: Boolean,
    statusText: String,
    onStartServer: () -> Unit,
    onStopServer: () -> Unit,
    onConnect: (DeviceInfo) -> Unit,
    onRefresh: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E2E)), shape = RoundedCornerShape(16.dp)) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(10.dp).clip(CircleShape)
                        .background(if (serverRunning) Color(0xFFA6E3A1) else Color(0xFF6C7086)))
                    Spacer(Modifier.width(8.dp))
                    Text(if (serverRunning) "服务运行中" else "服务未启动",
                        fontWeight = FontWeight.Bold, color = Color(0xFFCDD6F4))
                    Spacer(Modifier.weight(1f))
                    Button(
                        onClick = { if (serverRunning) onStopServer() else onStartServer() },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (serverRunning) Color(0xFFF38BA8) else Color(0xFFA6E3A1)),
                        shape = RoundedCornerShape(8.dp)
                    ) { Text(if (serverRunning) "停止" else "启动", color = Color(0xFF11111B)) }
                }
                Spacer(Modifier.height(6.dp))
                Text(statusText, fontSize = 12.sp, color = Color(0xFF6C7086))
            }
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically) {
            Text("发现的设备", fontWeight = FontWeight.Bold, color = Color(0xFFCDD6F4))
            TextButton(onClick = onRefresh) { Text("刷新", color = Color(0xFF6C63FF)) }
        }

        if (devices.isEmpty()) {
            Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                Text("正在扫描局域网设备...", color = Color(0xFF6C7086))
            }
        } else {
            LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(devices) { device ->
                    Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E2E)),
                        shape = RoundedCornerShape(12.dp)) {
                        Row(modifier = Modifier.fillMaxWidth().padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(36.dp).clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF313244)), contentAlignment = Alignment.Center) {
                                Text(if (device.platform.contains("Android", true)) "A" else "W",
                                    fontWeight = FontWeight.Bold, color = Color(0xFF6C63FF))
                            }
                            Spacer(Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(device.name, fontWeight = FontWeight.Medium, color = Color(0xFFCDD6F4))
                                Text("${device.ip} · ${device.platform}", fontSize = 11.sp, color = Color(0xFF6C7086))
                            }
                            Button(onClick = { onConnect(device) }, enabled = !streaming,
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6C63FF)),
                                shape = RoundedCornerShape(8.dp)
                            ) { Text("连接", color = Color.White) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AndroidSettingsPage(settings: AppSettings, onSave: (AppSettings) -> Unit) {
    var deviceName by remember { mutableStateOf(settings.deviceName) }
    var password by remember { mutableStateOf(settings.password) }
    var width by remember { mutableStateOf(settings.streamConfig.width.toString()) }
    var height by remember { mutableStateOf(settings.streamConfig.height.toString()) }
    var fps by remember { mutableStateOf(settings.streamConfig.fps.toString()) }
    var bitrate by remember { mutableStateOf(settings.streamConfig.maxBitrateMbps.toString()) }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)) {

        Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E2E)), shape = RoundedCornerShape(16.dp)) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("基本设置", fontWeight = FontWeight.Bold, color = Color(0xFFCDD6F4))
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(value = deviceName, onValueChange = { deviceName = it },
                    label = { Text("设备名称") }, modifier = Modifier.fillMaxWidth(), singleLine = true,
                    colors = tfColors(), shape = RoundedCornerShape(8.dp))
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(value = password, onValueChange = { password = it },
                    label = { Text("连接密码") }, modifier = Modifier.fillMaxWidth(), singleLine = true,
                    colors = tfColors(), shape = RoundedCornerShape(8.dp))
            }
        }

        Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E2E)), shape = RoundedCornerShape(16.dp)) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("串流参数", fontWeight = FontWeight.Bold, color = Color(0xFFCDD6F4))
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = width, onValueChange = { width = it },
                        label = { Text("宽度") }, modifier = Modifier.weight(1f), singleLine = true,
                        colors = tfColors(), shape = RoundedCornerShape(8.dp))
                    OutlinedTextField(value = height, onValueChange = { height = it },
                        label = { Text("高度") }, modifier = Modifier.weight(1f), singleLine = true,
                        colors = tfColors(), shape = RoundedCornerShape(8.dp))
                }
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = fps, onValueChange = { fps = it },
                        label = { Text("帧率") }, modifier = Modifier.weight(1f), singleLine = true,
                        colors = tfColors(), shape = RoundedCornerShape(8.dp))
                    OutlinedTextField(value = bitrate, onValueChange = { bitrate = it },
                        label = { Text("码率(Mbps)") }, modifier = Modifier.weight(1f), singleLine = true,
                        colors = tfColors(), shape = RoundedCornerShape(8.dp))
                }
            }
        }

        Button(onClick = {
            onSave(settings.copy(deviceName = deviceName, password = password,
                streamConfig = StreamConfig(
                    width = width.toIntOrNull() ?: 1920, height = height.toIntOrNull() ?: 1080,
                    fps = fps.toIntOrNull() ?: 60, maxBitrateMbps = bitrate.toIntOrNull() ?: 100000)))
        }, modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6C63FF)),
            shape = RoundedCornerShape(8.dp)
        ) { Text("保存设置", color = Color.White, modifier = Modifier.padding(vertical = 4.dp)) }
    }
}

@Composable
private fun tfColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = Color(0xFF6C63FF), unfocusedBorderColor = Color(0xFF313244),
    focusedLabelColor = Color(0xFF6C63FF), unfocusedLabelColor = Color(0xFF6C7086),
    cursorColor = Color(0xFF6C63FF), focusedTextColor = Color(0xFFCDD6F4),
    unfocusedTextColor = Color(0xFFCDD6F4)
)
