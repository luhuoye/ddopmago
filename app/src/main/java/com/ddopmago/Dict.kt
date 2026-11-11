@file:OptIn(ExperimentalMaterial3Api::class)

package com.ddopmago

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import android.content.Context
import android.media.MediaPlayer
import java.io.File
import android.net.Uri
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import java.io.ByteArrayInputStream

@Composable
fun DictScreen(st: Store) {
    var t by rememberSaveable { mutableIntStateOf(0) }
    var q by rememberSaveable { mutableStateOf("") }
    var sec by rememberSaveable { mutableStateOf<String?>(null) }
    var kb by remember { mutableStateOf(false) }
    val tabs = listOf("词句分类", "彝汉词典", "词库", "音节表", "2008词典")
    var detail by remember { mutableStateOf<Pair<MdictFile, Int>?>(null) }
    var full by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize()) {
        ScrollableTabRow(selectedTabIndex = t, containerColor = Color.White, edgePadding = 8.dp) {
            tabs.forEachIndexed { i, n -> Tab(selected = t == i, onClick = { t = i; q = ""; sec = null; detail = null }, text = { Text(n) }) }
        }
        Box(Modifier.padding(horizontal = 12.dp, vertical = 6.dp)) {
            InputBox(q, when (t) {
                0 -> "搜索：中文 / 彝文（点此打开键盘）"
                1 -> "搜索：中文 / 彝文 / 拼音"
                2 -> "输入拼音或彝文检索"
                4 -> "输入彝文 / 拼音 / 中文查词典"
                else -> "输入拼音筛选，如 ne"
            }) { kb = true }
        }
        Box(Modifier.weight(1f)) {
            when (t) {
                0 -> {
                    val key = q.trim().lowercase()
                    val list: List<Sent>? = when {
                        key.isNotEmpty() -> Repo.sents.filter { it.zh.contains(key) || it.yi.contains(key) || Repo.pinyin(it.yi).replace(" ", "").contains(key.replace(" ", "")) }.take(60)
                        sec != null -> Repo.sents.filter { it.sec == sec }
                        else -> null
                    }
                    if (list == null) {
                        LazyColumn(Modifier.padding(horizontal = 8.dp)) {
                            items(Repo.sections()) { s ->
                                AppCard(Modifier.clickable { sec = s }) {
                                    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Text(s, fontSize = 17.sp, modifier = Modifier.weight(1f))
                                        Text("${Repo.sents.count { it.sec == s }} 句  ›", color = Color.Gray)
                                    }
                                }
                            }
                        }
                    } else {
                        Column {
                            if (key.isEmpty()) TextButton(onClick = { sec = null }) { Text("‹ 返回分类（$sec）") }
                            else Text("找到 ${list.size} 条", color = Color.Gray, modifier = Modifier.padding(horizontal = 16.dp))
                            LazyColumn(Modifier.padding(horizontal = 8.dp)) { items(list) { SentCard(it, st) } }
                        }
                    }
                }
                1 -> {
                    val list = remember(q) { Repo.searchDict(q) }
                    Column {
                        Text("规范彝文词典收录的例词 ${Repo.dictSize()} 条，当前 ${list.size} 条", fontSize = 12.sp, color = Color.Gray, modifier = Modifier.padding(horizontal = 16.dp))
                        LazyColumn(Modifier.padding(horizontal = 8.dp)) { items(list) { SentCard(it, st) } }
                    }
                }
                2 -> {
                    val list = remember(q) { Repo.searchWords(q) }
                    Column {
                        Text(
                            if (q.isBlank()) "词库共 ${Repo.words.size} 个彝文词，输入拼音（如 ddop）或彝文查找" else "找到 ${list.size} 个（最多显示 80）",
                            fontSize = 12.sp, color = Color.Gray, modifier = Modifier.padding(horizontal = 16.dp)
                        )
                        LazyColumn(Modifier.padding(horizontal = 8.dp)) {
                            items(list) { w ->
                                AppCard {
                                    Column(Modifier.padding(14.dp)) {
                                        YiText(w, 24.sp)
                                        Repo.gloss[w]?.let { Text(it, fontSize = 16.sp) }
                                    }
                                }
                            }
                        }
                    }
                }
                4 -> MdictPane(q, detail, { detail = it }, full, { full = it })
                else -> {
                    val key = q.trim().lowercase()
                    val list = remember(q) { Repo.syllables().filter { key.isEmpty() || it.first.startsWith(key) } }
                    Column {
                        Text("共 ${list.size} 个音节（全部 1165 个规范彝文字）", fontSize = 12.sp, color = Color.Gray, modifier = Modifier.padding(horizontal = 16.dp))
                        LazyVerticalGrid(GridCells.Fixed(4), Modifier.padding(horizontal = 8.dp)) {
                            items(list) { (r, c) ->
                                Card(
                                    Modifier.padding(4.dp), shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(containerColor = Color.White),
                                    elevation = CardDefaults.cardElevation(1.dp)
                                ) {
                                    Column(Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(c.toString(), fontSize = 30.sp)
                                        Text(r, fontSize = 13.sp, color = Red)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        if (kb && detail == null) AppKeyboard(if (t <= 1) K_ZH else if (t == 4) K_YI else K_ABC, q, { q += it }, { q = q.dropLast(1) }, { kb = false })
    }
}


private fun mimeOf(path: String): String = when (path.substringAfterLast('.', "").lowercase()) {
    "css" -> "text/css"; "js" -> "application/javascript"; "png" -> "image/png"; "jpg", "jpeg" -> "image/jpeg"
    "gif" -> "image/gif"; "svg" -> "image/svg+xml"; "mp3" -> "audio/mpeg"; "woff" -> "font/woff"; "ttf" -> "font/ttf"
    else -> "application/octet-stream"
}

@Composable
fun MdxWebView(html: String, onEntry: (String) -> Unit) {
    AndroidView(modifier = Modifier.fillMaxSize(), factory = { ctx ->
        WebView(ctx).apply {
            settings.javaScriptEnabled = false
            settings.allowFileAccess = false
            webViewClient = object : WebViewClient() {
                override fun shouldInterceptRequest(view: WebView?, request: WebResourceRequest?): WebResourceResponse? {
                    val u = request?.url ?: return null
                    if (u.host != "mdict.local") return null
                    val path = "\\" + (u.path ?: "").removePrefix("/").replace("/", "\\")
                    val b = Mdict.resource(path) ?: Mdict.resource(path.removePrefix("\\")) ?: return null
                    return WebResourceResponse(mimeOf(path), null, ByteArrayInputStream(b))
                }

                override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                    val u = request?.url ?: return false
                    if (u.scheme == "entry") { onEntry(Uri.decode(u.toString().removePrefix("entry://"))); return true }
                    return u.host != "mdict.local"
                }
            }
        }
    }, update = {
        if (it.tag != html) { it.tag = html; it.loadDataWithBaseURL("https://mdict.local/", html, "text/html", "UTF-8", null) }
    })
}

@Composable
fun MdictPane(q: String, detail: Pair<MdictFile, Int>?, onOpen: (Pair<MdictFile, Int>?) -> Unit, full: Boolean, onFull: (Boolean) -> Unit) {
    val status = Mdict.status
    if (status != "ready") {
        Text(
            when {
                status == "installing" -> "正在安装词典（首次启动需要解压，请稍候）…"
                status == "loading" -> "正在加载词典…"
                status == "none" -> "安装包里没有词典文件（请先按说明准备词典分卷）"
                else -> "词典加载失败：" + status.removePrefix("error:")
            }, color = Color.Gray, modifier = Modifier.padding(16.dp)
        )
        return
    }
    if (detail != null) {
        val (d, i0) = detail
        val i = remember(d, i0) { Mdict.resolve(d, i0) }
        val ctx = LocalContext.current
        val snds = remember(d, i) { if (Repo.hasYi(d.keys[i])) Mdict.sounds(d.keys[i]) else emptyList() }
        Column(Modifier.fillMaxSize()) {
            BackBar(d.keys[i].take(30), { onOpen(null) })
            if (snds.isNotEmpty()) {
                Row(Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 8.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    snds.forEach { s -> OutlinedButton(onClick = { playSound(ctx, s) }) { Text("▶ 发音 · " + s.label, fontSize = 13.sp) } }
                }
            }
            MdxWebView(Mdict.html(d, i)) { w ->
                for (dd in Mdict.dicts) { val j = dd.exact(w); if (j >= 0) { onOpen(dd to j); break } }
            }
        }
        return
    }
    val res = remember(q, full) { if (full) emptyList() else Mdict.dicts.flatMap { d -> d.search(q, 80).map { d to it } } }
    var fres by remember(q, full) { mutableStateOf<List<Pair<MdictFile, Int>>?>(null) }
    DisposableEffect(q, full) {
        val stop = booleanArrayOf(false)
        if (full && q.isNotBlank()) {
            Thread {
                val r = Mdict.dicts.flatMap { d -> d.fullText(q, 60) { stop[0] }.map { d to it } }
                if (!stop[0]) fres = r
            }.start()
        }
        onDispose { stop[0] = true }
    }
    val list = if (full) (fres ?: emptyList()) else res
    Column(Modifier.fillMaxSize()) {
        Row(Modifier.padding(horizontal = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            FilterChip(selected = !full, onClick = { onFull(false) }, label = { Text("词条") })
            FilterChip(selected = full, onClick = { onFull(true) }, label = { Text("全文（按释义找）") })
            Text(Mdict.dicts.firstOrNull()?.title ?: "", fontSize = 12.sp, color = Color.Gray, maxLines = 1)
        }
        if (full && q.isNotBlank() && fres == null) Text("全文检索中…", color = Color.Gray, modifier = Modifier.padding(16.dp))
        else if (q.isBlank()) Text("词典共 ${Mdict.dicts.sumOf { it.keys.size }} 个词条，输入后查询", color = Color.Gray, fontSize = 12.sp, modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
        LazyColumn(Modifier.padding(horizontal = 8.dp)) {
            items(list) { (d, i) ->
                AppCard(Modifier.clickable { onOpen(d to i) }) {
                    Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) { MixText(d.keys[i], 22.sp, 17.sp) }
                        Text("›", color = Color.Gray)
                    }
                }
            }
        }
    }
}


fun playSound(ctx: Context, s: Mdict.Snd) {
    try {
        val f = File(ctx.cacheDir, "snd.mp3")
        f.writeBytes(s.mdd.record(s.idx))
        MediaPlayer().apply {
            setDataSource(f.path); prepare(); start()
            setOnCompletionListener { it.release() }
        }
    } catch (_: Throwable) {}
}
