package com.liuguang.theme

import android.app.WallpaperManager
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.graphics.Shader
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.net.HttpURLConnection
import java.net.URL
import kotlin.random.Random

/* ───────────────────────── 数据层 ───────────────────────── */

object Repo {
    const val UPD_URL = "https://1yk8u.github.io/app1/up/gx.txt"
    const val CAT_URL = "https://1yk8u.github.io/app1/themes/list.json"

    data class ThemeItem(
        val id: Int,
        val name: String,
        val desc: String,
        val image: String,
        val link: String,
        val extract: String,
        val tags: List<String>
    ) {
        val isHuaweiHonor: Boolean get() = tags.any { it.contains("华为") || it.contains("荣耀") }
        val isApple: Boolean get() = tags.any { it.contains("苹果") || it.contains("iOS") }
        val isGlass: Boolean get() = tags.any { it.contains("液态") }
        val imgSrc: String get() = if (image.startsWith("http")) image else "file:///android_asset/$image"
    }

    private var cache: Pair<String, Long>? = null

    suspend fun catalog(): List<ThemeItem> = withContext(Dispatchers.IO) {
        cache?.takeIf { System.currentTimeMillis() - it.second < 300_000 }?.let { return@withContext parse(it.first) }
        val text = httpGet(CAT_URL) ?: return@withContext emptyList()
        cache = text to System.currentTimeMillis()
        parse(text)
    }

    private fun parse(raw: String): List<ThemeItem> = runCatching {
        val arr = org.json.JSONObject(raw).optJSONArray("themes") ?: return emptyList()
        (0 until arr.length()).mapNotNull { i ->
            val o = arr.optJSONObject(i) ?: return@mapNotNull null
            val tags = ArrayList<String>()
            o.optJSONArray("tags")?.let { ta -> for (k in 0 until ta.length()) tags.add(ta.optString(k)) }
            ThemeItem(
                id = o.optInt("id", 0),
                name = o.optString("name"),
                desc = o.optString("desc"),
                image = o.optString("image"),
                link = o.optString("link"),
                extract = o.optString("extractCode"),
                tags = tags
            )
        }
    }.getOrDefault(emptyList())

    suspend fun checkUpdate(cur: Int): String? = withContext(Dispatchers.IO) {
        val txt = httpGet(UPD_URL) ?: return@withContext null
        val m = Regex("版本\\s*[:：]\\s*(\\d+)").find(txt) ?: return@withContext null
        val code = m.groupValues[1].toIntOrNull() ?: return@withContext null
        if (code > cur) {
            val body = txt.substringAfter("更新内容").substringAfter(":", "").trim().ifBlank { "全新版本" }
            "v$code · $body"
        } else null
    }

    private fun httpGet(url: String, timeout: Int = 9000): String? = try {
        val c = URL(url).openConnection() as HttpURLConnection
        c.connectTimeout = timeout; c.readTimeout = timeout
        c.setRequestProperty("User-Agent", "ThemePro/2.0")
        if (c.responseCode in 200..299) c.inputStream.use { s -> s.readBytes().toString(Charsets.UTF_8) } else null
    } catch (_: Exception) { null }
}

/* 壁纸预设(纯本机生成 · 全品牌通用 · 免 Root) */
data class WallPreset(val name: String, val c1: Int, val c2: Int, val c3: Int)
val WALLS = listOf(
    WallPreset("深海流光", 0xFF0B1E4A.toInt(), 0xFF1E5BFF.toInt(), 0xFF00C6FF.toInt()),
    WallPreset("液态玻璃", 0xFF241B4D.toInt(), 0xFF6C4DFF.toInt(), 0xFFB585FF.toInt()),
    WallPreset("暮色玫瑰", 0xFF2B0F24.toInt(), 0xFFC7246C.toInt(), 0xFFFF8AC2.toInt()),
    WallPreset("晨曦青柠", 0xFF0E2B1E.toInt(), 0xFF149A64.toInt(), 0xFF8CE99A.toInt()),
    WallPreset("熔岩日落", 0xFF2D0A0A.toInt(), 0xFFCC3D1E.toInt(), 0xFFFFC24B.toInt()),
    WallPreset("星空薄荷", 0xFF06182C.toInt(), 0xFF0DBBFF.toInt(), 0xFF9BE8FF.toInt()),
    WallPreset("紫罗兰夜", 0xFF150A29.toInt(), 0xFF7A3BFF.toInt(), 0xFFE0B3FF.toInt()),
    WallPreset("冰川蓝调", 0xFF0A1B2D.toInt(), 0xFF2D6BC0.toInt(), 0xFFC6E9FF.toInt())
)

fun makeWall(ctx: Context, p: WallPreset, w: Int, h: Int): Bitmap {
    val bmp = Bitmap.createBitmap(w.coerceIn(360, 1440), h.coerceIn(640, 3040), Bitmap.Config.ARGB_8888)
    val c = Canvas(bmp)
    val paint = android.graphics.Paint().apply { isAntiAlias = true }
    paint.shader = LinearGradient(0f, 0f, w * 0.9f, h.toFloat(),
        intArrayOf(p.c1, p.c2, p.c3), floatArrayOf(0f, 0.55f, 1f), Shader.TileMode.CLAMP)
    c.drawRect(0f, 0f, w.toFloat(), h.toFloat(), paint)
    val glow = android.graphics.Paint().apply { isAntiAlias = true; alpha = 26; color = 0xFFFFFFFF.toInt() }
    val rnd = Random(p.name.hashCode())
    repeat(26) {
        val cx = rnd.nextFloat() * w; val cy = rnd.nextFloat() * h; val r = rnd.nextInt(140, 420).toFloat()
        val gp = android.graphics.Paint().apply {
            isAntiAlias = true
            shader = LinearGradient(cx, cy, cx + 1, cy + r, 0x33FFFFFF, 0x00FFFFFF, Shader.TileMode.CLAMP)
        }
        android.graphics.RadialGradient(cx, cy, r, intArrayOf(0x2EF7F7FF, 0x00FFFFFF), floatArrayOf(0f, 1f), Shader.TileMode.CLAMP).let { gp.shader = it }
        glow.alpha = (6..20).random()
        c.drawCircle(cx, cy, r, gp)
    }
    return bmp
}

/* ───────────────────────── Activity ───────────────────────── */

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(
                colorScheme = darkColorScheme(
                    primary = Color(0xFF6C8CFF),
                    secondary = Color(0xFF00C6FF),
                    background = Color(0xFF0B1020),
                    surface = Color(0xFF131A30),
                    surfaceVariant = Color(0xFF1B2440),
                    onBackground = Color.White,
                    onSurface = Color.White,
                    onSurfaceVariant = Color(0xFFA9B6D6)
                )
            ) { AppRoot() }
        }
    }
}

/* ───────────────────────── 主体框架 ───────────────────────── */

@Composable
fun AppRoot() {
    val ctx = LocalContext.current
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var brandFilter by rememberSaveable { mutableIntStateOf(-1) }
    val tabs = listOf("首页", "主题", "壁纸", "插件", "我的")
    val icons = listOf(Icons.Filled.Home, Icons.Filled.Palette, Icons.Filled.Image, Icons.Filled.Extension, Icons.Filled.Person)

    val scope = rememberCoroutineScope()
    var themes by remember { mutableStateOf<List<Repo.ThemeItem>>(emptyList()) }
    var upd by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(Unit) {
        scope.launch(Dispatchers.IO) {
            themes = Repo.catalog()
            upd = Repo.checkUpdate(BuildConfig.VERSION_CODE)
        }
    }

    Scaffold(
        containerColor = Color(0xFF0B1020),
        bottomBar = {
            NavigationBar(containerColor = Color(0xFF101731), contentColor = Color.White) {
                tabs.forEachIndexed { i, label ->
                    NavigationBarItem(
                        selected = tab == i,
                        onClick = { tab = i },
                        icon = { Icon(icons[i], null) },
                        label = { Text(label, fontSize = 11.sp) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Color(0xFF6C8CFF),
                            indicatorColor = Color(0xFF6C8CFF).copy(alpha = 0.22f),
                            unselectedIconColor = Color(0xFF8895B5),
                            selectedTextColor = Color.White,
                            unselectedTextColor = Color(0xFF8895B5)
                        )
                    )
                }
            }
        }
    ) { pad ->
        Box(Modifier.padding(pad).fillMaxSize()) {
            when (tab) {
                0 -> HomeTab(upd, themes, brandFilter, { tab = 1 }, { b -> brandFilter = b; tab = 1 })
                1 -> ThemesTab(themes, brandFilter, onBrandPick = { b -> brandFilter = b })
                2 -> WallsTab()
                3 -> PluginsTab()
                else -> MeTab({ s ->
                    scope.launch { upd = s }
                })
            }
        }
    }
}

/* ─────────────────────── 首页(品牌适配/排行) ─────────────────────── */

val BRANDS = listOf(
    "小米·澎湃", "华为鸿蒙", "荣耀", "OPPO", "一加", "vivo", "真我", "iOS 风格", "全品牌通用"
)

@Composable
private fun HomeTab(upd: String?, themes: List<Repo.ThemeItem>, brand: Int, goThemes: () -> Unit, pickBrand: (Int) -> Unit) {
    val ctx = LocalContext.current
    val myBrand = remember {
        val m = Build.MANUFACTURER ?: ""
        when {
            m.contains("Xiaomi", true) || m.contains("Redmi", true) || m.contains("Poco", true) -> 0
            m.contains("HUAWEI", true) -> 1
            m.contains("HONOR", true) -> 2
            m.contains("OPPO", true) -> 3
            m.contains("OnePlus", true) -> 4
            m.contains("vivo", true) || m.contains("iQOO", true) -> 5
            m.contains("realme", true) -> 6
            else -> 8
        }
    }
    val hot = remember(themes) { themes.filter { it.id == 9 || it.id == 12 || it.id == 4 || it.id == 10 || it.id == 2 } }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
        Surface(shape = RoundedCornerShape(16.dp), color = Color(0xFF141D3A)) {
            Text(
                "主题Pro v2.0 全民版",
                Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                fontSize = 12.5.sp, color = Color(0xFF8CD9FF), fontWeight = FontWeight.SemiBold
            )
        }
        Box(Modifier.fillMaxWidth().padding(top = 12.dp).clip(RoundedCornerShape(20.dp))
            .background(Brush.linearGradient(listOf(Color(0xFF1B2E5E), Color(0xFF171233), Color(0xFF33124B))))
            .padding(18.dp)) {
            Column {
                Text("魔盒式主题广场", color = Color.White, fontSize = 21.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(6.dp))
                Text("多品牌适配 · 免费直装 · 全品牌壁纸引擎 · 插件中心", color = Color(0xFFA9B6D6), fontSize = 12.5.sp, lineHeight = 18.sp)
            }
        }

        if (upd != null) {
            Spacer(Modifier.height(12.dp))
            Surface(shape = RoundedCornerShape(14.dp), color = Color(0xFF2A2138)) {
                Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("🌀", fontSize = 18.sp)
                    Column(Modifier.padding(start = 10.dp).weight(1f)) {
                        Text("有新版本", fontSize = 12.5.sp, color = Color(0xFFB4C3EF), fontWeight = FontWeight.SemiBold)
                        Text(upd, fontSize = 11.5.sp, color = Color(0xFF8FA0C9), maxLines = 2, overflow = TextOverflow.Ellipsis)
                    }
                    TextButton(onClick = {
                        runCatching {
                            ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://1yk8u.github.io/app1/")))
                        }
                    }) { Text("去更新", fontSize = 12.sp, color = Color(0xFF8CD9FF)) }
                }
            }
        }

        Spacer(Modifier.height(16.dp))
        Text("品牌适配", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        Text(
            "检测到你的设备: ${Build.MANUFACTURER} · 已优先展示对应通道",
            color = Color(0xFF8090B5), fontSize = 11.5.sp, modifier = Modifier.padding(top = 2.dp)
        )
        Spacer(Modifier.height(10.dp))
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            BRANDS.chunked(3).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    row.forEachIndexed { idx, name ->
                        val realIdx = BRANDS.indexOfFirst { it == name }
                        val active = realIdx == myBrand
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (active) Color(0xFF2D4A8A) else Color(0xFF161F3C),
                            modifier = Modifier.weight(1f).clip(RoundedCornerShape(12.dp))
                                .clickable { pickBrand(realIdx) }
                        ) {
                            Text(
                                name + if (active) " ★" else "",
                                modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
                                fontSize = 12.sp,
                                color = if (active) Color.White else Color(0xFF9FB0D6),
                                fontWeight = if (active) FontWeight.Bold else FontWeight.Normal,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                    if (row.size < 3) repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
                }
            }
        }

        Spacer(Modifier.height(18.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("人气排行", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            Text("更多在「主题」页", color = Color(0xFF7080A8), fontSize = 11.sp)
        }
        Spacer(Modifier.height(10.dp))
        if (hot.isEmpty()) {
            Text("加载中…", color = Color(0xFF6B7BA0), fontSize = 13.sp)
        } else {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                items(hot) { t ->
                    Surface(shape = RoundedCornerShape(16.dp), color = Color(0xFF141C36),
                        modifier = Modifier.width(132.dp).clip(RoundedCornerShape(16.dp)).clickable { goThemes() }) {
                        Column(Modifier.padding(8.dp)) {
                            AsyncImage(
                                model = t.imgSrc, contentDescription = t.name,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxWidth().height(150.dp).clip(RoundedCornerShape(12.dp))
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(t.name, fontSize = 12.5.sp, color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.SemiBold)
                            Text(t.tags.take(2).joinToString(" · "), fontSize = 10.5.sp, color = Color(0xFF7D8DB8), maxLines = 1)
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(18.dp))
        Surface(
            shape = RoundedCornerShape(16.dp), color = Color(0xFF13243F),
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).clickable { goThemes() }
        ) {
            Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Storefront, null, tint = Color(0xFF8CD9FF))
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text("小米用户看这里", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    Text("主题壁纸商店一键直达 + 华为荣耀 12 款", color = Color(0xFF8FA0C9), fontSize = 11.5.sp)
                }
            }
        }
        Spacer(Modifier.height(20.dp))
    }
}

/* ─────────────────────── 主题页(分类筛选) ─────────────────────── */

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
private fun ThemesTab(themes: List<Repo.ThemeItem>, brand: Int, onBrandPick: (Int) -> Unit) {
    val ctx = LocalContext.current
    var filter by rememberSaveable { mutableIntStateOf(0) }
    val filters = listOf("全部", "华为·荣耀", "iOS 风格", "液态玻璃")
    LaunchedEffect(brand) { if (brand == 0) filter = 0 }
    val shown = remember(themes, filter) {
        when (filter) {
            1 -> themes.filter { it.isHuaweiHonor }
            2 -> themes.filter { it.isApple }
            3 -> themes.filter { it.isGlass }
            else -> themes
        }
    }
    var detail by remember { mutableStateOf<Repo.ThemeItem?>(null) }

    Column(Modifier.fillMaxSize()) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
            Text("主题广场", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                filters.forEachIndexed { i, f ->
                    val active = filter == i
                    Surface(
                        shape = RoundedCornerShape(999.dp),
                        color = if (active) Color(0xFF2D4A8A) else Color(0xFF161F3C),
                        modifier = Modifier.clip(RoundedCornerShape(999.dp)).clickable { filter = i }
                    ) { Text(f, Modifier.padding(horizontal = 14.dp, vertical = 7.dp), fontSize = 12.sp, color = if (active) Color.White else Color(0xFF9FB0D6)) }
                }
            }
        }
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            contentPadding = PaddingValues(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(2) }) {
                Surface(shape = RoundedCornerShape(14.dp), color = Color(0xFF16223F),
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).clickable {
                        runCatching {
                            ctx.packageManager.getLaunchIntentForPackage("com.android.thememanager")
                                ?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                ?.let { ctx.startActivity(it) }
                                ?: Toast.makeText(ctx, "未找到系统「主题壁纸」App", Toast.LENGTH_SHORT).show()
                        }
                    }) {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Storefront, null, tint = Color(0xFF8CD9FF))
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text("小米通道 · 官方主题壁纸商店", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                            Text("免费主题区一键直达(小米/澎湃自带)", color = Color(0xFF8FA0C9), fontSize = 11.sp)
                        }
                    }
                }
            }
            items(shown) { t ->
                Surface(shape = RoundedCornerShape(16.dp), color = Color(0xFF141C36),
                    modifier = Modifier.clip(RoundedCornerShape(16.dp)).clickable { detail = t }) {
                    Column(Modifier.padding(8.dp)) {
                        AsyncImage(
                            model = t.imgSrc, contentDescription = t.name,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxWidth().height(170.dp).clip(RoundedCornerShape(12.dp))
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(t.name, fontSize = 13.sp, color = Color.White, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(t.desc, fontSize = 11.sp, color = Color(0xFF7D8DB8), maxLines = 2, overflow = TextOverflow.Ellipsis, lineHeight = 14.sp)
                        Spacer(Modifier.height(6.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            t.tags.take(2).forEach { tag ->
                                Surface(shape = RoundedCornerShape(6.dp), color = Color(0xFF223055)) {
                                    Text(tag, Modifier.padding(horizontal = 6.dp, vertical = 2.dp), fontSize = 9.5.sp, color = Color(0xFF9FD4FF))
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    detail?.let { d ->
        androidx.compose.material3.ModalBottomSheet(onDismissRequest = { detail = null }, containerColor = Color(0xFF141B33)) {
            Column(Modifier.padding(horizontal = 18.dp).padding(bottom = 26.dp)) {
                AsyncImage(model = d.imgSrc, contentDescription = null, contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxWidth().height(260.dp).clip(RoundedCornerShape(18.dp)))
                Spacer(Modifier.height(14.dp))
                Text(d.name, color = Color.White, fontSize = 19.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(4.dp))
                Text(d.desc, color = Color(0xFFA9B6D6), fontSize = 12.5.sp, lineHeight = 18.sp)
                Spacer(Modifier.height(6.dp))
                Text("标签: " + d.tags.joinToString(" · "), color = Color(0xFF8096C0), fontSize = 11.5.sp)
                if (d.isHuaweiHonor && Build.MANUFACTURER?.contains("xiaomi", true) == true) {
                    Spacer(Modifier.height(8.dp))
                    Text("提示: 这是华为/荣耀格式(.hwt)主题。小米用户建议走上方「小米通道」或「壁纸引擎」。",
                        color = Color(0xFFFFB980), fontSize = 11.5.sp, lineHeight = 15.sp)
                }
                Spacer(Modifier.height(14.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(
                        onClick = {
                            runCatching { ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(d.link))) }
                        },
                        shape = RoundedCornerShape(12.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2D4A8A))
                    ) { Text("网页提取", fontSize = 13.sp) }
                    OutlinedButton(
                        onClick = {
                            runCatching {
                                val cm = ctx.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                cm.setPrimaryClip(ClipData.newPlainText("extract", d.extract))
                                Toast.makeText(ctx, "已复制提取码 " + d.extract, Toast.LENGTH_SHORT).show()
                            }
                        },
                        shape = RoundedCornerShape(12.dp)
                    ) { Text("复制提取码", fontSize = 13.sp, color = Color(0xFF9FD4FF)) }
                }
                Text("免费公益分发 · 未经商业行为 · 署名 GPL 上游", color = Color(0xFF66779E), fontSize = 10.5.sp, modifier = Modifier.padding(top = 8.dp))
            }
        }
    }
}

/* ─────────────────────── 壁纸引擎(全品牌通用) ─────────────────────── */

@Composable
private fun WallsTab() {
    val ctx = LocalContext.current
    var seed by remember { mutableIntStateOf(0) }
    val list = remember(seed) {
        if (seed == 0) WALLS else WALLS.map {
            val r = Random(seed + it.name.hashCode())
            it.copy(c1 = 0xFF000000.toInt() + r.nextInt(0x151A3D), c2 = 0xFF000000.toInt() + r.nextInt(0x8F42C0), c3 = 0xFF000000.toInt() + r.nextInt(0xA0F0FF))
        }
    }
    var preview by remember { mutableStateOf<WallPreset?>(null) }

    Column(Modifier.fillMaxSize()) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
            Text("壁纸引擎", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Text("纯本地生成渐变壁纸 · 全品牌通吃 · 不需 Root · 立刻可用", color = Color(0xFF8090B5), fontSize = 11.5.sp, modifier = Modifier.padding(top = 2.dp))
        }
        Row(Modifier.padding(horizontal = 16.dp).height(34.dp), verticalAlignment = Alignment.CenterVertically) {
            Spacer(Modifier.weight(1f))
            Surface(shape = RoundedCornerShape(999.dp), color = Color(0xFF2D4A8A),
                modifier = Modifier.clip(RoundedCornerShape(999.dp)).clickable { seed = Random.nextInt() }) {
                Text("🎲 随机重生成", Modifier.padding(horizontal = 16.dp, vertical = 6.dp), fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.SemiBold)
            }
        }
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            contentPadding = PaddingValues(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(list) { w ->
                val bmp = remember(w, seed) { makeWall(ctx, w, 720, 1080) }
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.clip(RoundedCornerShape(16.dp)).clickable { preview = w }
                ) {
                    Column(Modifier.background(Color(0xFF141C36)).padding(8.dp)) {
                        androidx.compose.foundation.Image(
                            bitmap = bmp.asImageBitmap(), contentDescription = w.name,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxWidth().height(180.dp).clip(RoundedCornerShape(12.dp))
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(w.name, fontSize = 12.5.sp, color = Color.White, fontWeight = FontWeight.SemiBold)
                        Text("点按预览 · 一键设置", fontSize = 10.5.sp, color = Color(0xFF7D8DB8))
                    }
                }
            }
        }
    }

    preview?.let { p ->
        val bmp = remember(p, seed) { makeWall(ctx, p, 1080, 2160) }
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { preview = null },
            confirmButton = {},
            containerColor = Color(0xFF141B33),
            title = { Text(p.name, color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    androidx.compose.foundation.Image(
                        bitmap = bmp.asImageBitmap(), contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxWidth().height(320.dp).clip(RoundedCornerShape(14.dp))
                    )
                    Spacer(Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = {
                                runCatching {
                                    val wm = WallpaperManager.getInstance(ctx)
                                    wm.setBitmap(bmp, null, true, WallpaperManager.FLAG_SYSTEM)
                                    Toast.makeText(ctx, "已设为桌面壁纸 ✓", Toast.LENGTH_SHORT).show()
                                }.onFailure { Toast.makeText(ctx, "设置失败: ${it.message}", Toast.LENGTH_SHORT).show() }
                            },
                            shape = RoundedCornerShape(10.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2D4A8A))
                        ) { Text("设桌面", fontSize = 12.5.sp) }
                        OutlinedButton(
                            onClick = {
                                runCatching {
                                    val wm = WallpaperManager.getInstance(ctx)
                                    wm.setBitmap(bmp, null, true, WallpaperManager.FLAG_LOCK)
                                    Toast.makeText(ctx, "已设为锁屏壁纸 ✓", Toast.LENGTH_SHORT).show()
                                }.onFailure { Toast.makeText(ctx, "设置失败: ${it.message}", Toast.LENGTH_SHORT).show() }
                            },
                            shape = RoundedCornerShape(10.dp)
                        ) { Text("设锁屏", fontSize = 12.5.sp, color = Color(0xFF9FD4FF)) }
                        Spacer(Modifier.weight(1f))
                        TextButton(onClick = { preview = null }) { Text("关闭", fontSize = 12.5.sp, color = Color(0xFF8FA0C9)) }
                    }
                }
            }
        )
    }
}

/* ─────────────────────── 插件中心(魔盒式免费) ─────────────────────── */

data class Plugin(
    val icon: String,
    val name: String,
    val desc: String,
    val steps: List<String>,
    val actionLabel: String? = null,
    val pack: String? = null       // 系统应用包名,可一键跳转
)

val PLUGINS = listOf(
    Plugin("🎨", "状态栏时钟样式", "混搭模块,免 Root", listOf("打开「主题壁纸」App", "进入「我的 → 主题 → 混搭」", "选择「状态栏 → 时钟」", "挑个免费的样式套上,立即生效"), "打开主题壁纸", "com.android.thememanager"),
    Plugin("⚡", "充电动画切换", "HyperOS/MIUI 内置选项", listOf("打开「设置」", "搜索「充电动画」", "选喜欢的样式(部分主题自带)", "型号不同顺序略有差异"), "打开设置", "com.android.settings"),
    Plugin("🔒", "锁屏样式编辑", "神隐锁屏/混搭锁屏", listOf("打开「主题壁纸」App", "我的 → 主题 → 混搭 → 锁屏", "换锁屏布局或字体样式", "应用后锁屏立即变化"), "打开主题壁纸", "com.android.thememanager"),
    Plugin("🧩", "桌面布局调整", "网格密度随你改", listOf("桌面长按空白处", "点「设置」→「桌面布局」", "选 4×6 / 5×6 等网格", "勾选「重叠图标」也可强排"), null, null),
    Plugin("🚫", "广告及弹窗关闭", "自家用机订阅选项", listOf("打开「设置」", "搜索「广告」", "在系统广告/应用广告里全关", "安全中心→隐私保护时常驻开关"), "打开设置", "com.android.settings"),
    Plugin("🔤", "免费字体替换", "混搭→字体", listOf("打开「主题壁纸」App", "分类 → 字体 → 免费专区", "挑一款喜欢的应用", "回「混搭」里也可单独用"), "打开主题壁纸", "com.android.thememanager"),
    Plugin("📸", "拍照水印关闭", "机型水印随手关", listOf("打开「相机」", "右上角三条杠 → 设置", "关闭「机型水印」和时间水印", "部分机型在 ×2/专业中"), null, null),
    Plugin("👥", "应用双开/分身", "微信 QQ 双登", listOf("打开「设置」", "搜索「应用双开」", "挑要双开的 App 打开开关", "桌面出现第二个图标"), "打开设置", "com.android.settings")
)

@Composable
private fun PluginsTab() {
    val ctx = LocalContext.current
    var open by remember { mutableStateOf<Plugin?>(null) }

    Column(Modifier.fillMaxSize()) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
            Text("插件中心", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Text("全部免费 · 玩机小工具与一键直达 · 持续增加", color = Color(0xFF8090B5), fontSize = 11.5.sp, modifier = Modifier.padding(top = 2.dp))
        }
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            PLUGINS.forEach { p ->
                Surface(
                    shape = RoundedCornerShape(14.dp), color = Color(0xFF141C36),
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).clickable { open = p }
                ) {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(40.dp).clip(CircleShape).background(Color(0xFF223055)), contentAlignment = Alignment.Center) {
                            Text(p.icon, fontSize = 18.sp)
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(p.name, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                            Text(p.desc, color = Color(0xFF8FA0C9), fontSize = 11.5.sp)
                        }
                        Text("查看 ›", color = Color(0xFF6C8CFF), fontSize = 12.sp)
                    }
                }
            }
            Text("我们不做付费订阅、不上传破包;所有技巧均为系统自带功能与官网开通选项。",
                color = Color(0xFF66779E), fontSize = 10.5.sp, lineHeight = 14.sp,
                modifier = Modifier.padding(top = 6.dp, bottom = 18.dp))
        }
    }

    open?.let { pl ->
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { open = null },
            confirmButton = {},
            containerColor = Color(0xFF141B33),
            title = { Text(pl.icon + " " + pl.name, color = Color.White, fontSize = 16.5.sp, fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    pl.steps.forEachIndexed { i, s ->
                        Text("${i + 1}. $s", color = Color(0xFFC6D2F0), fontSize = 12.5.sp, lineHeight = 17.sp, modifier = Modifier.padding(vertical = 3.dp))
                    }
                    Spacer(Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Spacer(Modifier.weight(1f))
                        TextButton(onClick = { open = null }) { Text("知道了", fontSize = 12.5.sp, color = Color(0xFF8FA0C9)) }
                        if (pl.actionLabel != null) {
                            Button(
                                onClick = {
                                    open = null
                                    runCatching {
                                        val it = ctx.packageManager.getLaunchIntentForPackage(pl.pack ?: "")
                                            ?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                        if (it == null) Toast.makeText(ctx, "该 App 不在此设备上", Toast.LENGTH_SHORT).show()
                                        else ctx.startActivity(it)
                                    }
                                },
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2D4A8A))
                            ) { Text(pl.actionLabel, fontSize = 12.5.sp) }
                        }
                    }
                }
            }
        )
    }
}

/* ─────────────────────── 我的页 ─────────────────────── */

@Composable
private fun MeTab(onUpdate: (String?) -> Unit) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var checking by remember { mutableStateOf(false) }
    var about by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
        Text("我的", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(10.dp))
        Surface(shape = RoundedCornerShape(16.dp), color = Color(0xFF141C36), modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(42.dp).clip(CircleShape).background(Brush.linearGradient(listOf(Color(0xFF3E67FF), Color(0xFF00C6FF)))), contentAlignment = Alignment.Center) {
                        Text("主", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text("本机设备", color = Color(0xFFA9B6D6), fontSize = 11.sp)
                        Text(Build.MANUFACTURER + " " + Build.MODEL, color = Color.White, fontSize = 14.5.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
                Spacer(Modifier.height(10.dp))
                HorizontalDivider(color = Color(0xFF232E52))
                Row(Modifier.padding(top = 10.dp)) {
                    Text("版本", color = Color(0xFF8895B5), fontSize = 12.sp, modifier = Modifier.weight(1f))
                    Text("v" + BuildConfig.VERSION_NAME + " (code " + BuildConfig.VERSION_CODE + ")", color = Color.White, fontSize = 12.sp)
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        Button(
            onClick = {
                checking = true
                scope.launch {
                    val r = Repo.checkUpdate(BuildConfig.VERSION_CODE)
                    checking = false
                    onUpdate(r)
                    if (r == null) Toast.makeText(ctx, "已是最新版本", Toast.LENGTH_SHORT).show()
                }
            },
            shape = RoundedCornerShape(12.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2D4A8A)),
            enabled = !checking
        ) { Text(if (checking) "检查中…" else "检查更新", fontSize = 13.sp) }
        Spacer(Modifier.height(10.dp))
        OutlinedButton(
            onClick = {
                runCatching { ctx.cacheDir.deleteRecursively() }
                Toast.makeText(ctx, "缓存已清理", Toast.LENGTH_SHORT).show()
            },
            shape = RoundedCornerShape(12.dp)
        ) { Text("清理缓存", fontSize = 13.sp, color = Color(0xFF9FD4FF)) }
        Spacer(Modifier.height(10.dp))
        OutlinedButton(
            onClick = { about = true },
            shape = RoundedCornerShape(12.dp)
        ) { Text("关于 / 开源致谢", fontSize = 13.sp, color = Color(0xFF9FD4FF)) }
        Spacer(Modifier.height(18.dp))
        Text("主题 Pro 由流光开源维护;主题内容署名 GPL-3.0 开源项目 FanChen-Theme-Library 并受其条款约束;应用与分发完全免费,不存在付费墙。",
            color = Color(0xFF6B7BA0), fontSize = 11.sp, lineHeight = 15.sp)
    }

    if (about) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { about = false },
            confirmButton = { TextButton(onClick = { about = false }) { Text("好的", color = Color(0xFF6C8CFF)) } },
            containerColor = Color(0xFF141B33),
            title = { Text("关于 主题Pro", color = Color.White, fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("· 界面与功能:流光开源(自研,GPL-3.0 传播)", color = Color(0xFFC6D2F0), fontSize = 12.sp, lineHeight = 16.sp)
                    Text("· 主题包署名: GPL-3.0 开源项目 FanChen-Theme-Library,由其贡献者免费分发", color = Color(0xFFC6D2F0), fontSize = 12.sp, lineHeight = 16.sp)
                    Text("· 壁纸引擎:本机实时生成,不经网络,无 第三方素材", color = Color(0xFFC6D2F0), fontSize = 12.sp, lineHeight = 16.sp)
                    Text("· 全部功能免费,永远不会有会员与付费", color = Color(0xFFC6D2F0), fontSize = 12.sp, lineHeight = 16.sp)
                }
            }
        )
    }
}
