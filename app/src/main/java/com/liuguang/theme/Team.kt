package com.liuguang.theme

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import coil.compose.rememberAsyncImagePainter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TeamScreen(navController: NavHostController) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        Image(
            painter = rememberAsyncImagePainter("file:///android_asset/background.png"),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
            alpha = 0.3f
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
        ) {
            Spacer(modifier = Modifier.height(114.dp))

            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(20.dp))
                Text(
                    text = "开发组",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "开源 · 免费 · 主题Pro",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(30.dp))

                TeamMemberCard(
                    name = "流光",
                    position = "维护者 · 开源重制",
                    description = "负责主题Pro维护、界面升级与在线主题库整合",
                    painter = painterResource(id = R.drawable.icon),
                    onClick = {
                        openLink(context, "https://github.com/1yk8u/liuguang-theme")
                    }
                )
                Spacer(modifier = Modifier.height(16.dp))

                TeamMemberCard(
                    name = "MIKUN",
                    position = "原作者 · GPL 项目作者",
                    description = "GPL-3.0 上游项目 FanChen-Theme-Library 作者，感谢开源精神",
                    painter = rememberAsyncImagePainter("file:///android_asset/mikun.jpg"),
                    onClick = {
                        openLink(context, "https://mikunweb.netlify.app/")
                    }
                )
                Spacer(modifier = Modifier.height(16.dp))

                TeamMemberCard(
                    name = "凡尘",
                    position = "原项目设计师",
                    description = "原项目主题设计",
                    painter = rememberAsyncImagePainter("file:///android_asset/fc.jpg"),
                    onClick = {
                        openLink(context, "https://link3.cc/fc_ndw")
                    }
                )

                Spacer(modifier = Modifier.height(30.dp))
            }
        }

        GlassTopBar(
            title = "开发组",
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "返回",
                    modifier = Modifier
                        .size(28.dp)
                        .clickable { navController.popBackStack() }
                )
            }
        )
    }
}

@Composable
fun TeamMemberCard(
    name: String,
    position: String,
    description: String,
    painter: Painter,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
        )
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Image(
                painter = painter,
                contentDescription = name,
                modifier = Modifier
                    .size(100.dp)
                    .clip(CircleShape)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = name,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = position,
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF41A6FF),
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

fun openLink(context: Context, url: String) {
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
    context.startActivity(intent)
}
