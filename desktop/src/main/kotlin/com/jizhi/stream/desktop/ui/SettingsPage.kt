package com.jizhi.stream.desktop.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jizhi.stream.core.config.AppSettings
import com.jizhi.stream.core.config.StreamConfig

@Composable
fun SettingsPage(settings: AppSettings, onSave: (AppSettings) -> Unit) {
    var deviceName by remember { mutableStateOf(settings.deviceName) }
    var password by remember { mutableStateOf(settings.password) }
    var width by remember { mutableStateOf(settings.streamConfig.width.toString()) }
    var height by remember { mutableStateOf(settings.streamConfig.height.toString()) }
    var fps by remember { mutableStateOf(settings.streamConfig.fps.toString()) }
    var bitrate by remember { mutableStateOf(settings.streamConfig.maxBitrateMbps.toString()) }
    var manualIp by remember { mutableStateOf("") }
    var manualName by remember { mutableStateOf("") }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        SectionCard("基本设置") {
            SettingField("设备名称", deviceName) { deviceName = it }
            Spacer(Modifier.height(8.dp))
            SettingField("连接密码", password) { password = it }
        }

        SectionCard("串流参数") {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                SettingField("宽度", width, Modifier.weight(1f)) { width = it }
                SettingField("高度", height, Modifier.weight(1f)) { height = it }
            }
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                SettingField("帧率", fps, Modifier.weight(1f)) { fps = it }
                SettingField("码率(Mbps)", bitrate, Modifier.weight(1f)) { bitrate = it }
            }
        }

        SectionCard("手动添加设备") {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.Bottom) {
                SettingField("设备名", manualName, Modifier.weight(1f)) { manualName = it }
                SettingField("IP地址", manualIp, Modifier.weight(1f)) { manualIp = it }
                Button(
                    onClick = {
                        if (manualIp.isNotBlank()) {
                            val name = manualName.ifBlank { manualIp }
                            settings.manualDevices.add(AppSettings.ManualDevice(name, manualIp))
                            manualIp = ""
                            manualName = ""
                            onSave(settings)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6C63FF)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("添加", color = Color.White)
                }
            }
            settings.manualDevices.forEachIndexed { idx, dev ->
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("${dev.name} (${dev.ip})", color = Color(0xFFCDD6F4), modifier = Modifier.weight(1f))
                    TextButton(onClick = {
                        settings.manualDevices.removeAt(idx)
                        onSave(settings)
                    }) {
                        Text("删除", color = Color(0xFFF38BA8))
                    }
                }
            }
        }

        Button(
            onClick = {
                onSave(settings.copy(
                    deviceName = deviceName,
                    password = password,
                    streamConfig = StreamConfig(
                        width = width.toIntOrNull() ?: 1920,
                        height = height.toIntOrNull() ?: 1080,
                        fps = fps.toIntOrNull() ?: 60,
                        maxBitrateMbps = bitrate.toIntOrNull() ?: 100000
                    )
                ))
            },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6C63FF)),
            shape = RoundedCornerShape(8.dp)
        ) {
            Text("保存设置", color = Color.White, modifier = Modifier.padding(vertical = 4.dp))
        }
    }
}

@Composable
private fun SectionCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E2E)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(title, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color(0xFFCDD6F4))
            Spacer(Modifier.height(12.dp))
            content()
        }
    }
}

@Composable
private fun SettingField(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    onValueChange: (String) -> Unit
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        modifier = modifier.fillMaxWidth(),
        singleLine = true,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = Color(0xFF6C63FF),
            unfocusedBorderColor = Color(0xFF313244),
            focusedLabelColor = Color(0xFF6C63FF),
            unfocusedLabelColor = Color(0xFF6C7086),
            cursorColor = Color(0xFF6C63FF),
            focusedTextColor = Color(0xFFCDD6F4),
            unfocusedTextColor = Color(0xFFCDD6F4)
        ),
        shape = RoundedCornerShape(8.dp)
    )
}
