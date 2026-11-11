@file:OptIn(ExperimentalMaterial3Api::class)

package com.ddopmago

import android.content.Context
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.json.JSONArray
import org.json.JSONObject

/** t: c选择 j判断(A对B错) f填空 */
data class PQ(val n: Int, val t: String, val pts: Int, val note: String, val stem: String, val opts: List<String>, val ans: String)
data class Paper(val id: Int, val title: String, val group: String, val head: String, val qs: List<PQ>, val full: String, val key: String) {
    val short: String get() = title.replace("凉山州二类模式", "").replace("凉山州", "")
    val autoPts: Int get() = qs.sumOf { it.pts }
}
data class Doc(val id: Int, val group: String, val title: String, val text: String)

object Lib {
    var papers: List<Paper> = emptyList()
    var docs: List<Doc> = emptyList()

    fun load(c: Context) {
        if (papers.isNotEmpty()) return
        fun rd(n: String) = c.assets.open(n).bufferedReader(Charsets.UTF_8).use { it.readText() }
        val a = JSONArray(rd("papers.json"))
        papers = (0 until a.length()).map {
            val o = a.getJSONObject(it)
            val qa = o.getJSONArray("qs")
            Paper(o.getInt("id"), o.getString("title"), o.getString("group"), o.getString("head"),
                (0 until qa.length()).map { k -> pq(qa.getJSONObject(k)) }, o.getString("full"), o.getString("key"))
        }
        val d = JSONArray(rd("docs.json"))
        docs = (0 until d.length()).map {
            val o = d.getJSONObject(it); Doc(o.getInt("id"), o.getString("group"), o.getString("title"), o.getString("text"))
        }
    }

    private fun pq(o: JSONObject): PQ {
        val oa = o.getJSONArray("opts")
        return PQ(o.getInt("n"), o.getString("t"), o.getInt("pts"), o.getString("note"), o.getString("stem"),
            (0 until oa.length()).map { oa.getString(it) }, o.getString("ans"))
    }

    fun grade(q: PQ, a: String): Boolean {
        if (a.isBlank()) return false
        return if (q.t == "f") q.ans.split("/", "、").any { Repo.clean(it) == Repo.clean(a) } else a == q.ans
    }
}

// ---------------- 学习：试卷 + 资料 ----------------

@Composable
fun StudyScreen(st: Store) {
    var t by rememberSaveable { mutableIntStateOf(0) }
    Column(Modifier.fillMaxSize()) {
        TabRow(selectedTabIndex = t, containerColor = Color.White) {
            Tab(selected = t == 0, onClick = { t = 0 }, text = { Text("试卷") })
            Tab(selected = t == 1, onClick = { t = 1 }, text = { Text("资料") })
        }
        Box(Modifier.weight(1f)) { if (t == 0) PapersScreen(st) else DocsScreen() }
    }
}

@Composable
fun BackBar(title: String, onBack: () -> Unit, extra: @Composable RowScope.() -> Unit = {}) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        TextButton(onClick = onBack) { Text("‹ 返回") }
        Text(title, fontSize = 14.sp, color = Color.Gray, maxLines = 1, modifier = Modifier.weight(1f))
        extra()
    }
}

/** 长文阅读：彝文逐字标拼音，可关闭拼音 */
@Composable
fun ReaderText(text: String, py: Boolean) {
    LazyColumn(Modifier.fillMaxSize().padding(horizontal = 12.dp)) {
        items(text.split("\n")) { l ->
            when {
                l.isBlank() -> Spacer(Modifier.height(6.dp))
                Repo.hasYi(l) -> Box(Modifier.padding(vertical = 3.dp)) { YiText(l.trim(), 19.sp, py = py) }
                else -> Text(l.trim(), fontSize = 16.sp, modifier = Modifier.padding(vertical = 3.dp))
            }
        }
        item { Spacer(Modifier.height(24.dp)) }
    }
}

@Composable
fun DocsScreen() {
    var open by rememberSaveable { mutableStateOf<Int?>(null) }
    var py by rememberSaveable { mutableStateOf(true) }
    val doc = Lib.docs.firstOrNull { it.id == open }
    if (doc == null) {
        LazyColumn(Modifier.fillMaxSize().padding(8.dp)) {
            Lib.docs.groupBy { it.group }.forEach { (g, list) ->
                item { Text(g, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Red, modifier = Modifier.padding(8.dp)) }
                items(list) { d ->
                    AppCard(Modifier.clickable { open = d.id }) {
                        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                if (Repo.hasYi(d.title)) MixText(d.title, 17.sp, 16.sp) else Text(d.title, fontSize = 16.sp)
                            }
                            Text("›", color = Color.Gray)
                        }
                    }
                }
            }
        }
    } else {
        Column(Modifier.fillMaxSize()) {
            BackBar(doc.title.take(30), { open = null }) {
                Text("拼音", fontSize = 13.sp); Switch(checked = py, onCheckedChange = { py = it }, modifier = Modifier.padding(horizontal = 6.dp))
            }
            ReaderText(doc.text, py)
        }
    }
}

@Composable
fun PapersScreen(st: Store) {
    var open by rememberSaveable { mutableStateOf<Int?>(null) }
    val p = Lib.papers.firstOrNull { it.id == open }
    if (p == null) {
        LazyColumn(Modifier.fillMaxSize().padding(8.dp)) {
            Lib.papers.groupBy { it.group }.forEach { (g, list) ->
                item { Text(g, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Red, modifier = Modifier.padding(8.dp)) }
                items(list) { x ->
                    AppCard(Modifier.clickable { open = x.id }) {
                        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(x.short, fontSize = 16.sp)
                                val best = st.paperBest(x.id)
                                Text(
                                    (if (x.qs.isEmpty()) "仅试卷原文" else "可在线答题 ${x.qs.size} 题 / ${x.autoPts} 分") + (best?.let { " · 最高 $it 分" } ?: ""),
                                    fontSize = 12.sp, color = Color.Gray
                                )
                            }
                            Text("›", color = Color.Gray)
                        }
                    }
                }
            }
        }
    } else PaperDetail(p, st) { open = null }
}

@Composable
fun PaperDetail(p: Paper, st: Store, onBack: () -> Unit) {
    var tab by rememberSaveable { mutableIntStateOf(if (p.qs.isEmpty()) 1 else 0) }
    var py by rememberSaveable { mutableStateOf(true) }
    Column(Modifier.fillMaxSize()) {
        BackBar(p.short, onBack) {
            if (tab != 0) { Text("拼音", fontSize = 13.sp); Switch(checked = py, onCheckedChange = { py = it }, modifier = Modifier.padding(horizontal = 6.dp)) }
        }
        Row(Modifier.padding(horizontal = 8.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            if (p.qs.isNotEmpty()) FilterChip(selected = tab == 0, onClick = { tab = 0 }, label = { Text("在线答题") })
            FilterChip(selected = tab == 1, onClick = { tab = 1 }, label = { Text("试卷原文") })
            if (p.key.isNotBlank()) FilterChip(selected = tab == 2, onClick = { tab = 2 }, label = { Text("参考答案") })
        }
        Box(Modifier.weight(1f)) {
            when (tab) {
                0 -> PaperQuiz(p, st)
                1 -> ReaderText(p.full, py)
                else -> ReaderText(p.key, py)
            }
        }
    }
}

@Composable
fun PaperQuiz(p: Paper, st: Store) {
    val ans = remember(p.id) { mutableStateListOf<String>().apply { p.qs.forEach { add("") } } }
    var idx by remember(p.id) { mutableIntStateOf(0) }
    var done by remember(p.id) { mutableStateOf(false) }
    var score by remember(p.id) { mutableIntStateOf(0) }
    var confirm by remember { mutableStateOf(false) }
    if (done) {
        LazyColumn(Modifier.fillMaxSize().padding(12.dp)) {
            item {
                AppCard {
                    Column(Modifier.fillMaxWidth().padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("$score", fontSize = 60.sp, fontWeight = FontWeight.Bold, color = Red)
                        Text("在线判分题满分 ${p.autoPts} 分（作文、阅读等请对照「参考答案」自评）", color = Color.Gray, fontSize = 13.sp)
                        Button(onClick = { ans.indices.forEach { ans[it] = "" }; idx = 0; done = false }, modifier = Modifier.fillMaxWidth().padding(top = 12.dp)) { Text("重做") }
                    }
                }
                Text("错题 / 未答", fontSize = 16.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 8.dp))
            }
            items(p.qs.indices.filter { !Lib.grade(p.qs[it], ans[it]) }) { i ->
                val q = p.qs[i]
                AppCard {
                    Column(Modifier.padding(14.dp)) {
                        Text("第 ${q.n} 题", fontSize = 12.sp, color = Red)
                        MixText(q.stem, 20.sp, 15.sp)
                        if (q.t == "c") q.opts.forEachIndexed { k, o -> MixText("${'A' + k}. $o", 18.sp, 14.sp, Color.DarkGray) }
                        Text("你的答案：" + (if (ans[i].isBlank()) "未作答" else if (q.t == "f") "" else ans[i]), color = Color(0xFFC62828), fontSize = 14.sp)
                        if (q.t == "f" && ans[i].isNotBlank()) MixText(ans[i], 20.sp, 15.sp, Color(0xFFC62828))
                        Text("正确答案：" + (if (q.t == "f") "" else (if (q.t == "j") (if (q.ans == "A") "A 正确" else "B 错误") else q.ans)), color = Color(0xFF2E7D32), fontSize = 14.sp)
                        if (q.t == "f") MixText(q.ans, 20.sp, 15.sp, Color(0xFF2E7D32))
                    }
                }
            }
        }
        return
    }
    val q = p.qs[idx]
    Column(Modifier.fillMaxSize()) {
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(12.dp)) {
            Text("第 ${idx + 1} / ${p.qs.size} 题（原卷第 ${q.n} 题）· ${q.pts} 分", color = Color.Gray, fontSize = 13.sp)
            LinearProgressIndicator(progress = { (idx + 1f) / p.qs.size }, modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp))
            if (q.note.isNotBlank()) Text(q.note, fontSize = 13.sp, color = Red, modifier = Modifier.padding(bottom = 4.dp))
            AppCard { Column(Modifier.padding(16.dp)) { MixText(q.stem, 22.sp, 17.sp) } }
            when (q.t) {
                "c" -> q.opts.forEachIndexed { k, o ->
                    val l = ('A' + k).toString()
                    OptionButton("$l. $o", if (ans[idx] == l) Color(0xFFFFD54F) else Color(0xFFFFF3D6)) { ans[idx] = l }
                }
                "j" -> {
                    OptionButton("A  正确", if (ans[idx] == "A") Color(0xFFFFD54F) else Color(0xFFFFF3D6)) { ans[idx] = "A" }
                    OptionButton("B  错误", if (ans[idx] == "B") Color(0xFFFFD54F) else Color(0xFFFFF3D6)) { ans[idx] = "B" }
                }
                else -> InputBox(ans[idx], "用下方键盘作答…")
            }
            Row(Modifier.fillMaxWidth().padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { if (idx > 0) idx-- }, enabled = idx > 0, modifier = Modifier.weight(1f)) { Text("上一题") }
                if (idx < p.qs.size - 1) Button(onClick = { idx++ }, modifier = Modifier.weight(1f)) { Text("下一题") }
                else Button(onClick = { confirm = true }, modifier = Modifier.weight(1f)) { Text("交卷") }
            }
            TextButton(onClick = { confirm = true }, modifier = Modifier.align(Alignment.CenterHorizontally)) { Text("提前交卷") }
        }
        if (q.t == "f") {
            AppKeyboard(if (Repo.hasYi(q.ans)) K_YI else K_ZH, ans[idx], { ans[idx] = ans[idx] + it }, { ans[idx] = ans[idx].dropLast(1) })
        }
    }
    if (confirm) {
        val blank = ans.count { it.isBlank() }
        AlertDialog(
            onDismissRequest = { confirm = false },
            title = { Text("确认交卷？") },
            text = { Text(if (blank > 0) "还有 $blank 题未作答，按 0 分计算。" else "全部题目已作答。") },
            confirmButton = {
                TextButton(onClick = {
                    confirm = false
                    score = p.qs.indices.sumOf { if (Lib.grade(p.qs[it], ans[it])) p.qs[it].pts else 0 }
                    st.setPaperBest(p.id, score); done = true
                }) { Text("交卷") }
            },
            dismissButton = { TextButton(onClick = { confirm = false }) { Text("继续答题") } }
        )
    }
}
