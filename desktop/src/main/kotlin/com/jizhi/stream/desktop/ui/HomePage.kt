package com.jizhi.stream.desktop.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jizhi.stream.core.network.DeviceInfo

@Composable
fun HomePage(
    devices: List<DeviceInfo>,
    serverRunning: Boolean,
    streaming: Boolean,
    statusText: String,
    onStartServer: () -> Unit,
    onStopServer: () -> Unit,
    onConnect: (DeviceInfo) -> Unit,
    onRefresh: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E2E)),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier.size(12.dp)
                            .clip(CircleShape)
                            .background(if (serverRunning) Color(0xFFA6E3A1) else Color(0xFF6C7086))
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        if (serverRunning) "服务运行中" else "服务未启动",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = Color(0xFFCDD6F4)
                    )
                    Spacer(Modifier.weight(1f))
                    Button(
                        onClick = { if (serverRunning) onStopServer() else onStartServer() },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (serverRunning) Color(0xFFF38BA8) else Color(0xFFA6E3A1)
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            if (serverRunning) "停止服务" else "启动服务",
                            color = Color(0xFF11111B)
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
                Text(statusText, fontSize = 13.sp, color = Color(0xFF6C7086))
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("发现的设备", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color(0xFFCDD6F4))
            TextButton(onClick = onRefresh) {
                Text("刷新", color = Color(0xFF6C63FF))
            }
        }

        if (devices.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxWidth().weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text("正在扫描局域网设备...", color = Color(0xFF6C7086))
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(devices) { device ->
                    DeviceCard(device, streaming, onConnect)
                }
            }
        }
    }
}

@Composable
private fun DeviceCard(device: DeviceInfo, streaming: Boolean, onConnect: (DeviceInfo) -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E2E)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFF313244)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    when {
                        device.platform.contains("Android", true) -> "A"
                        device.platform == "手动" -> "M"
                        else -> "W"
                    },
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF6C63FF),
                    fontSize = 18.sp
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(device.name, fontWeight = FontWeight.Medium, color = Color(0xFFCDD6F4))
                Text("${device.ip} · ${device.platform}", fontSize = 12.sp, color = Color(0xFF6C7086))
            }
            Button(
                onClick = { onConnect(device) },
                enabled = !streaming,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6C63FF)),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("连接", color = Color.White)
            }
        }
    }
}
