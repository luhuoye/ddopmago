@file:OptIn(ExperimentalMaterial3Api::class)

package com.ddopmago

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

val Red = Color(0xFFB71C1C)
val Gold = Color(0xFFF9A825)
val Ink = Color(0xFF212121)

private val Scheme = lightColorScheme(
    primary = Red, onPrimary = Color.White, primaryContainer = Color(0xFFFFDAD6),
    secondary = Gold, secondaryContainer = Color(0xFFFFE9A8), onSecondaryContainer = Color(0xFF3B2A00),
    background = Color(0xFFFFFBF5), surface = Color.White
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Repo.load(this)
        Lib.load(this)
        Mdict.init(applicationContext)
        val store = Store(this)
        setContent { MaterialTheme(colorScheme = Scheme) { App(store) } }
    }
}

@Composable
fun AppCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier.fillMaxWidth().padding(vertical = 5.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp),
        content = content
    )
}

@Composable
fun Header(title: String) {
    Column {
        Row(
            Modifier.fillMaxWidth().background(Brush.horizontalGradient(listOf(Red, Ink))).padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            YiText("ꅇꂷꇬ", 20.sp, Gold, Color(0xFFFFE0B2))
            Spacer(Modifier.width(10.dp))
            Text(title, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }
        Box(Modifier.fillMaxWidth().height(3.dp).background(Gold))
    }
}

@Composable
fun App(st: Store) {
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var query by rememberSaveable { mutableStateOf("") }
    var fromYi by rememberSaveable { mutableStateOf(true) }
    val exam = remember { ExamState() }
    val names = listOf("翻译", "字典", "做题", "学习", "收藏")
    val icons = listOf("译", "典", "题", "学", "★")
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = { Header("凉山彝语 · " + names[tab]) },
        bottomBar = {
            NavigationBar(containerColor = Color.White) {
                names.forEachIndexed { i, t ->
                    NavigationBarItem(
                        selected = tab == i, onClick = { tab = i },
                        icon = { Text(icons[i], fontSize = 18.sp, fontWeight = FontWeight.Bold) },
                        label = { Text(t, fontSize = 11.sp) }
                    )
                }
            }
        }
    ) { p ->
        Box(Modifier.padding(p).fillMaxSize()) {
            when (tab) {
                0 -> TranslateScreen(st, query, { query = it }, fromYi, { fromYi = it; query = "" })
                1 -> DictScreen(st)
                2 -> PracticeHub(st, exam)
                3 -> StudyScreen(st)
                else -> FavScreen(st)
            }
        }
    }
}

@Composable
fun TranslateScreen(st: Store, query: String, onQuery: (String) -> Unit, fromYi: Boolean, onDir: (Boolean) -> Unit) {
    var kb by remember { mutableStateOf(true) }
    Column(Modifier.fillMaxSize()) {
        Row(Modifier.padding(8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(selected = fromYi, onClick = { onDir(true) }, label = { Text("彝文→中文") })
            FilterChip(selected = !fromYi, onClick = { onDir(false) }, label = { Text("中文→彝文") })
            Spacer(Modifier.weight(1f))
            TextButton(onClick = { onQuery("") }) { Text("清空") }
        }
        Box(Modifier.padding(horizontal = 12.dp)) {
            InputBox(query, if (fromYi) "用下方键盘输入彝文…" else "用下方键盘输入中文…") { kb = true }
        }
        val res = remember(query, fromYi) { Repo.search(query, fromYi) }
        LazyColumn(Modifier.weight(1f).padding(horizontal = 8.dp)) {
            if (query.isNotBlank() && res.isEmpty()) item { Text("词句库里没有相近的内容", color = Color.Gray, modifier = Modifier.padding(12.dp)) }
            items(res) { SentCard(it, st) }
        }
        if (kb) AppKeyboard(if (fromYi) K_YI else K_ZH, query, { onQuery(query + it) }, { onQuery(query.dropLast(1)) }, { kb = false })
    }
}

@Composable
fun SentCard(s: Sent, st: Store) {
    val fav = st.favS.any { it.yi == s.yi && it.zh == s.zh }
    AppCard {
        Row(Modifier.padding(14.dp)) {
            Column(Modifier.weight(1f)) {
                YiText(s.yi, 22.sp)
                Text(s.zh, fontSize = 17.sp, modifier = Modifier.padding(top = 2.dp))
                Text(s.sec, fontSize = 11.sp, color = Red)
            }
            TextButton(onClick = { st.toggleS(s) }) { Text(if (fav) "★" else "☆", fontSize = 26.sp, color = Gold) }
        }
    }
}

@Composable
fun FavScreen(st: Store) {
    var t by rememberSaveable { mutableIntStateOf(0) }
    Column(Modifier.fillMaxSize()) {
        TabRow(selectedTabIndex = t, containerColor = Color.White) {
            Tab(selected = t == 0, onClick = { t = 0 }, text = { Text("词句 (${st.favS.size})") })
            Tab(selected = t == 1, onClick = { t = 1 }, text = { Text("题目 (${st.favQ.size})") })
        }
        LazyColumn(Modifier.weight(1f).padding(8.dp)) {
            if (t == 0) {
                if (st.favS.isEmpty()) item { Text("暂无收藏，翻译/字典结果右侧点 ☆ 收藏", color = Color.Gray, modifier = Modifier.padding(12.dp)) }
                items(st.favS.toList()) { SentCard(it, st) }
            } else {
                if (st.favQ.isEmpty()) item { Text("暂无收藏题目，做题/考试后点「收藏本题」", color = Color.Gray, modifier = Modifier.padding(12.dp)) }
                items(st.favQ.toList()) { q ->
                    AppCard {
                        Row(Modifier.padding(14.dp)) {
                            Column(Modifier.weight(1f)) {
                                Text(q.prompt, fontSize = 13.sp, color = Red)
                                QShown(q)
                                Text("答案：", fontSize = 14.sp, color = Color(0xFF2E7D32))
                                AnsText(q, q.options[q.ans], Color(0xFF2E7D32))
                                if (q.type != 6) MixText(q.expl, 18.sp, 13.sp, Color.Gray)
                            }
                            TextButton(onClick = { st.toggleQ(q) }) { Text("★", fontSize = 26.sp, color = Gold) }
                        }
                    }
                }
            }
        }
    }
}


@Composable
fun PracticeHub(st: Store, exam: ExamState) {
    var t by rememberSaveable { mutableIntStateOf(0) }
    Column(Modifier.fillMaxSize()) {
        TabRow(selectedTabIndex = t, containerColor = Color.White) {
            Tab(selected = t == 0, onClick = { t = 0 }, text = { Text("练习") })
            Tab(selected = t == 1, onClick = { t = 1 }, text = { Text("模拟考试") })
        }
        Box(Modifier.weight(1f)) { if (t == 0) QuizScreen(st) else ExamScreen(st, exam) }
    }
}
