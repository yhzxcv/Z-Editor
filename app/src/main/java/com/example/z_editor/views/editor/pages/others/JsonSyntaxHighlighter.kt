package com.example.z_editor.views.editor.pages.others

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle

/**
 * JSON 语法高亮逻辑层 —— 字符扫描分词，不依赖正则（避免转义引号翻车）。
 * 输入为 gson 生成的 pretty-printed JSON，结构已知合法。
 */
enum class JsonTokenKind { KEY, STRING, NUMBER, BOOLEAN, NULL, PUNCTUATION }

data class JsonToken(val start: Int, val end: Int, val kind: JsonTokenKind)

data class JsonHighlightPalette(
    val keyColor: Color,
    val stringColor: Color,
    val numberColor: Color,
    val booleanColor: Color,
    val nullColor: Color,
    val punctuationColor: Color,
    val matchBackground: Color,
    val activeMatchBackground: Color
)

/**
 * 硬编码亮/暗两套配色，不依赖 Material colorScheme（用户指定：键=黄、字符串=绿）。
 * 亮态底色为白、暗态底色为深色，故黄/绿各取亮暗两档，保证对比度可读：
 * 暗态用亮色（amber 300 / green 300），亮态用深色（solarized 黄 / green 800）。
 * 其余类别（数字/布尔/null/标点）与搜索命中背景也用固定色，整体配色不再随主题变化。
 */
fun defaultPalette(dark: Boolean): JsonHighlightPalette = if (dark) {
    JsonHighlightPalette(
        keyColor = Color(0xFFFFD54F),        // 亮黄
        stringColor = Color(0xFF81C784),     // 亮绿
        numberColor = Color(0xFF64B5F6),     // 蓝
        booleanColor = Color(0xFFCE93D8),    // 紫
        nullColor = Color(0xFF90A4AE),       // 蓝灰
        punctuationColor = Color(0xFFBDBDBD), // 灰
        matchBackground = Color(0x40FFFFFF),
        activeMatchBackground = Color(0x66FFB74D) // 橙，当前命中高亮
    )
} else {
    JsonHighlightPalette(
        keyColor = Color(0xFFB58900),        // 深黄（solarized）
        stringColor = Color(0xFF2E7D32),     // 深绿
        numberColor = Color(0xFF1565C0),     // 蓝
        booleanColor = Color(0xFF7B1FA2),    // 紫
        nullColor = Color(0xFF607D8B),       // 蓝灰
        punctuationColor = Color(0xFF757575), // 灰
        matchBackground = Color(0x3D000000),
        activeMatchBackground = Color(0x66FF9800) // 橙，当前命中高亮
    )
}

object JsonSyntaxHighlighter {

    /** 字符扫描分词：字符串（含转义）、数字、布尔/null 关键字、标点。 */
    fun tokenize(json: String): List<JsonToken> {
        val tokens = mutableListOf<JsonToken>()
        var i = 0
        val n = json.length
        while (i < n) {
            val c = json[i]
            when {
                c == '"' -> {
                    val start = i
                    i++
                    var escaped = false
                    while (i < n) {
                        val ch = json[i]
                        if (escaped) { escaped = false; i++; continue }
                        if (ch == '\\') { escaped = true; i++; continue }
                        if (ch == '"') { i++; break }
                        i++
                    }
                    // 字符串后紧跟非空 ':' 判定为 KEY
                    var j = i
                    while (j < n && json[j].isWhitespace()) j++
                    val kind = if (j < n && json[j] == ':') JsonTokenKind.KEY else JsonTokenKind.STRING
                    tokens += JsonToken(start, i, kind)
                }
                c.isDigit() || c == '-' -> {
                    val start = i
                    while (i < n && (json[i].isDigit() || json[i] in ".-+eE")) i++
                    tokens += JsonToken(start, i, JsonTokenKind.NUMBER)
                }
                c.isLetter() -> {
                    val start = i
                    while (i < n && json[i].isLetter()) i++
                    val word = json.substring(start, i)
                    val kind = when (word) {
                        "true", "false" -> JsonTokenKind.BOOLEAN
                        "null" -> JsonTokenKind.NULL
                        else -> JsonTokenKind.PUNCTUATION
                    }
                    tokens += JsonToken(start, i, kind)
                }
                c in "{},[]:" -> {
                    tokens += JsonToken(i, i + 1, JsonTokenKind.PUNCTUATION)
                    i++
                }
                else -> i++
            }
        }
        return tokens
    }

    /**
     * 构建高亮文本：先叠语法色，再叠搜索命中背景（当前命中用更亮背景）。
     * 颜色与背景属性不冲突，叠加后各自生效；AnnotatedString 后加层覆盖同属性，
     * 天然支持"当前命中"强调。
     */
    fun buildHighlightedJson(
        json: String,
        palette: JsonHighlightPalette,
        matches: List<JsonTextMatch> = emptyList(),
        activeIndex: Int = -1
    ): AnnotatedString {
        val builder = AnnotatedString.Builder(json)
        // 合并相邻同色 token 为一段，减少 AnnotatedString 的 span 数，降低布局开销。
        // 只合并严格相邻（token.start == runEnd），避免覆盖中间的空白字符。
        var runStart = -1
        var runEnd = -1
        var runColor: Color? = null
        for (token in tokenize(json)) {
            val color = when (token.kind) {
                JsonTokenKind.KEY -> palette.keyColor
                JsonTokenKind.STRING -> palette.stringColor
                JsonTokenKind.NUMBER -> palette.numberColor
                JsonTokenKind.BOOLEAN -> palette.booleanColor
                JsonTokenKind.NULL -> palette.nullColor
                JsonTokenKind.PUNCTUATION -> palette.punctuationColor
            }
            if (color == runColor && token.start == runEnd) {
                runEnd = token.end
            } else {
                if (runStart >= 0 && runColor != null) {
                    builder.addStyle(SpanStyle(color = runColor), runStart, runEnd)
                }
                runStart = token.start
                runEnd = token.end
                runColor = color
            }
        }
        if (runStart >= 0 && runColor != null) {
            builder.addStyle(SpanStyle(color = runColor), runStart, runEnd)
        }
        for ((idx, match) in matches.withIndex()) {
            if (match.start < 0 || match.end > json.length || match.start >= match.end) continue
            val background = if (idx == activeIndex) palette.activeMatchBackground else palette.matchBackground
            builder.addStyle(SpanStyle(background = background), match.start, match.end)
        }
        return builder.toAnnotatedString()
    }

    /**
     * 在已生成的语法高亮文本上叠加搜索命中背景，不重新分词。
     * 供"切换上一个/下一个命中"时使用：切命中只叠加一层，不重扫全篇。
     * 无命中时原样返回 [base]，零拷贝。
     */
    fun overlayMatchBackgrounds(
        base: AnnotatedString,
        matches: List<JsonTextMatch>,
        activeIndex: Int,
        palette: JsonHighlightPalette
    ): AnnotatedString {
        if (matches.isEmpty()) return base
        val builder = AnnotatedString.Builder(base)
        for ((idx, match) in matches.withIndex()) {
            if (match.start < 0 || match.end > base.length || match.start >= match.end) continue
            val background = if (idx == activeIndex) palette.activeMatchBackground else palette.matchBackground
            builder.addStyle(SpanStyle(background = background), match.start, match.end)
        }
        return builder.toAnnotatedString()
    }
}
