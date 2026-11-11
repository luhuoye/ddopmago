@file:OptIn(ExperimentalLayoutApi::class)

package com.ddopmago

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

val PyColor = Color(0xFF8D6E63)

/** 彝文逐字标注拼音（ruby 排版）；py=false 时只显示彝文（用于拼音题） */
@Composable
fun YiText(s: String, size: TextUnit = 22.sp, color: Color = Color.Unspecified, pyColor: Color = PyColor, py: Boolean = true) {
    FlowRow {
        s.forEach { c ->
            Column(Modifier.padding(horizontal = 1.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(c.toString(), fontSize = size, color = color)
                if (py) Text(if (Repo.isYi(c)) (Repo.rom(c) ?: "") else " ", fontSize = (size.value * 0.48f).coerceAtLeast(9f).sp, color = pyColor)
            }
        }
    }
}

/** 多行文本：含彝文的行自动标拼音，其余行按普通文字显示 */
@Composable
fun MixText(text: String, yiSize: TextUnit = 22.sp, zhSize: TextUnit = 16.sp, color: Color = Color.Unspecified) {
    Column {
        text.split("\n").forEach { l ->
            if (Repo.hasYi(l)) YiText(l, yiSize, color) else Text(l, fontSize = zhSize, color = color)
        }
    }
}

/** 只显示、不弹系统键盘的输入框；文字全部由内置键盘输入 */
@Composable
fun InputBox(value: String, hint: String, onClick: () -> Unit = {}) {
    Box(
        Modifier.fillMaxWidth().heightIn(min = 56.dp)
            .background(Color.White, RoundedCornerShape(12.dp))
            .border(1.dp, Color(0xFFE0C9A6), RoundedCornerShape(12.dp))
            .clickable(onClick = onClick).padding(12.dp)
    ) {
        when {
            value.isEmpty() -> Text(hint, color = Color.Gray)
            Repo.hasYi(value) -> YiText("$value|", 24.sp)
            else -> Text("$value|", fontSize = 22.sp)
        }
    }
}

const val K_YI = 0
const val K_ZH = 1
const val K_ABC = 2

/** 内置输入法：彝文(拼音候选+字表) / 中文(字表+联想) / 拼音字母。所有文字输入都用它 */
@Composable
fun AppKeyboard(kind: Int, text: String, onText: (String) -> Unit, onBack: () -> Unit, onHide: (() -> Unit)? = null) {
    var mode by remember(kind) { mutableIntStateOf(kind) }
    var buf by remember { mutableStateOf("") }
    var table by remember { mutableStateOf(false) }
    val cands = remember(buf) { Repo.cands(buf) }
    val zh = remember(text, mode) { if (mode == K_ZH) Repo.zhNext(text) else emptyList() }

    Column(Modifier.fillMaxWidth().background(Color(0xFFE3E6EA)).padding(4.dp)) {
        Row(
            Modifier.fillMaxWidth().height(50.dp).horizontalScroll(rememberScrollState()),
            verticalAlignment = Alignment.CenterVertically
        ) {
            when (mode) {
                K_YI -> {
                    Text(if (buf.isEmpty()) "输入拼音如 ne，或点「字表」" else buf, fontSize = 13.sp, color = Color.DarkGray, modifier = Modifier.padding(horizontal = 6.dp))
                    cands.forEach { c ->
                        Column(Modifier.clickable { onText(c.toString()); buf = "" }.padding(horizontal = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(c.toString(), fontSize = 24.sp)
                            Text(Repo.rom(c) ?: "", fontSize = 10.sp, color = PyColor)
                        }
                    }
                }
                K_ZH -> {
                    Text("联想", fontSize = 13.sp, color = Color.DarkGray, modifier = Modifier.padding(horizontal = 6.dp))
                    zh.forEach { c -> Text(c.toString(), fontSize = 26.sp, modifier = Modifier.clickable { onText(c.toString()) }.padding(horizontal = 10.dp)) }
                }
                else -> Text("字母输入（拼音）", fontSize = 13.sp, color = Color.DarkGray, modifier = Modifier.padding(horizontal = 6.dp))
            }
        }
        if (mode == K_YI && table) {
            LazyVerticalGrid(GridCells.Fixed(8), Modifier.fillMaxWidth().height(216.dp)) {
                items((0xA000..0xA48C).toList()) { cp ->
                    val ch = cp.toChar()
                    Column(Modifier.fillMaxWidth().clickable { onText(ch.toString()) }.padding(vertical = 3.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(ch.toString(), fontSize = 22.sp, textAlign = TextAlign.Center)
                        Text(Repo.rom(ch) ?: "", fontSize = 9.sp, color = PyColor)
                    }
                }
            }
        } else if (mode == K_ZH) {
            LazyVerticalGrid(GridCells.Fixed(8), Modifier.fillMaxWidth().height(216.dp)) {
                items(Repo.hanzi) { ch ->
                    Text(ch.toString(), fontSize = 22.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().clickable { onText(ch.toString()) }.padding(vertical = 7.dp))
                }
            }
        } else {
            listOf("qwertyuiop", "asdfghjkl", "zxcvbnm").forEach { row ->
                Row(
                    Modifier.fillMaxWidth().padding(vertical = 2.dp).padding(horizontal = if (row.length < 10) 14.dp else 0.dp),
                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    row.forEach { ch -> Key(ch.toString(), Modifier.weight(1f)) { if (mode == K_YI) buf += ch else onText(ch.toString()) } }
                }
            }
        }
        Row(Modifier.fillMaxWidth().padding(vertical = 2.dp), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
            Key("⇄" + listOf("彝", "中", "abc")[mode], Modifier.weight(1.4f)) { mode = (mode + 1) % 3; buf = ""; table = false }
            if (mode == K_YI) Key(if (table) "ABC" else "字表", Modifier.weight(1.3f)) { table = !table }
            if (mode != K_ABC) {
                Key("，", Modifier.weight(1f)) { onText("，") }
                Key("。", Modifier.weight(1f)) { onText("。") }
            }
            Key("空格", Modifier.weight(2f)) {
                if (mode == K_YI && buf.isNotEmpty()) { cands.firstOrNull()?.let { onText(it.toString()) }; buf = "" } else onText(" ")
            }
            Key("⌫", Modifier.weight(1.3f)) { if (mode == K_YI && buf.isNotEmpty()) buf = buf.dropLast(1) else onBack() }
            if (onHide != null) Key("⌄", Modifier.weight(1f)) { onHide() }
        }
    }
}

@Composable
private fun Key(t: String, m: Modifier, onClick: () -> Unit) {
    Box(
        m.height(44.dp).clip(RoundedCornerShape(6.dp)).background(Color.White).clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) { Text(t, fontSize = 15.sp) }
}
