package com.jizhi.stream.desktop.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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

@Composable
fun TopBar(currentPage: String, onNavigate: (String) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF181825))
            .padding(horizontal = 24.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            "极致串流",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF6C63FF)
        )
        Spacer(Modifier.width(8.dp))
        Text(
            "JizhiStream",
            fontSize = 12.sp,
            color = Color(0xFF6C7086)
        )
        Spacer(Modifier.weight(1f))
        TabButton("主页", currentPage == "home") { onNavigate("home") }
        Spacer(Modifier.width(8.dp))
        TabButton("设置", currentPage == "settings") { onNavigate("settings") }
    }
}

@Composable
private fun TabButton(text: String, selected: Boolean, onClick: () -> Unit) {
    val bg = if (selected) Color(0xFF6C63FF) else Color.Transparent
    val fg = if (selected) Color.White else Color(0xFF6C7086)
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(bg)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text(text, color = fg, fontSize = 14.sp)
    }
}
