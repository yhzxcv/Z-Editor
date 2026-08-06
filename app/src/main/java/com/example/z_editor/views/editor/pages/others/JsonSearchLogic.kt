package com.example.z_editor.views.editor.pages.others

/**
 * 搜索/查找逻辑层 —— 移植自 scripts/json_viewer_search.dart。
 *
 * 逐行扫描全文，返回每个命中的全文偏移与行号。匹配不跨行。
 */
data class JsonSearchOptions(
    val caseSensitive: Boolean = false,
    val wholeWords: Boolean = false,
    val regex: Boolean = false
)

/**
 * @param start 全文偏移，包含
 * @param end   全文偏移，排除
 * @param lineIndex 命中所在行号（0 起）
 */
data class JsonTextMatch(
    val start: Int,
    val end: Int,
    val lineIndex: Int
)

object JsonSearchLogic {

    /** 构建搜索正则；查询为空或正则非法时返回 null。 */
    fun buildPattern(query: String, options: JsonSearchOptions): Regex? {
        if (query.isEmpty()) return null
        return try {
            if (options.regex) {
                Regex(query, if (options.caseSensitive) setOf() else setOf(RegexOption.IGNORE_CASE))
            } else {
                val escaped = Regex.escape(query)
                val pattern = if (options.wholeWords) "\\b" + escaped + "\\b" else escaped
                Regex(pattern, if (options.caseSensitive) setOf() else setOf(RegexOption.IGNORE_CASE))
            }
        } catch (e: Exception) {
            null
        }
    }

    /**
     * 逐行扫描：按换行符切行，行内 [Regex.findAll]，偏移换算为全文偏移并记录行号。
     * 与 Dart 参考实现语义一致 —— 匹配不跨行。
     */
    fun findMatches(text: String, query: String, options: JsonSearchOptions): List<JsonTextMatch> {
        if (query.isEmpty()) return emptyList()
        // 普通字面量搜索走 indexOf 快路径：无正则编译、无按行 substring 分配。
        // 查询含换行时没有匹配意义（匹配不跨行），直接走正则分支（同样返回空）。
        if (!options.regex && query.indexOf('\n') < 0) {
            return findLiteralMatches(text, query, options.caseSensitive)
        }
        val pattern = buildPattern(query, options) ?: return emptyList()
        val result = mutableListOf<JsonTextMatch>()
        var lineIndex = 0
        var lineStart = 0
        var i = 0
        while (i <= text.length) {
            if (i == text.length || text[i] == '\n') {
                val line = text.substring(lineStart, i)
                for (match in pattern.findAll(line)) {
                    result += JsonTextMatch(
                        start = lineStart + match.range.first,
                        end = lineStart + match.range.last + 1,
                        lineIndex = lineIndex
                    )
                }
                lineStart = i + 1
                lineIndex++
            }
            i++
        }
        return result
    }

    /**
     * 字面量快路径：单遍扫描 + 全局游标推进（O(n)）。
     * indexOf 的起始位置 from 只前进不后退，行号/行起点随扫描增量推进，
     * 保持"匹配不跨行"语义。查询含换行时不会进入此路径（已在 findMatches 排除）。
     */
    private fun findLiteralMatches(text: String, query: String, caseSensitive: Boolean): List<JsonTextMatch> {
        val result = mutableListOf<JsonTextMatch>()
        val n = text.length
        if (query.length > n) return result
        var from = 0
        var lineIndex = 0
        var lineStart = 0
        while (true) {
            val idx = text.indexOf(query, from, ignoreCase = !caseSensitive)
            if (idx < 0) break
            // 把行号推进到 idx 所在行：统计 [lineStart, idx] 内的换行
            while (lineStart <= idx) {
                val nl = text.indexOf('\n', lineStart)
                if (nl < 0 || nl > idx) break
                lineIndex++
                lineStart = nl + 1
            }
            // 匹配不得跨行：idx 到 end 之间不能有换行
            val end = idx + query.length
            val nlAt = text.indexOf('\n', idx)
            if (nlAt >= 0 && nlAt < end) {
                // 跨行命中，跳过并从下一个位置继续找
                from = idx + 1
                continue
            }
            result += JsonTextMatch(start = idx, end = end, lineIndex = lineIndex)
            from = idx + 1
        }
        return result
    }

    /**
     * 替换单个匹配（activeMatchIndex 对应的区间）。
     * 正则模式下用 matchAt 重定位该区间并展开 $0..$9 捕获组；字面量模式直接替换区间。
     */
    fun replaceSingle(
        text: String,
        match: JsonTextMatch,
        replacement: String,
        query: String,
        options: JsonSearchOptions
    ): String {
        if (!options.regex) return text.replaceRange(match.start, match.end, replacement)
        val pattern = buildPattern(query, options) ?: return text
        val m = pattern.matchAt(text, match.start) ?: return text
        return text.replaceRange(m.range.first, m.range.last + 1, expandGroups(replacement, m))
    }

    /**
     * 全部替换：字面量用 String.replace（含 ignoreCase），正则用 Regex.replace（自动展开 $1/$2）。
     */
    fun replaceAll(text: String, query: String, replacement: String, options: JsonSearchOptions): String {
        if (query.isEmpty()) return text
        if (!options.regex) {
            return if (options.caseSensitive) text.replace(query, replacement)
            else text.replace(query, replacement, ignoreCase = true)
        }
        val pattern = buildPattern(query, options) ?: return text
        return pattern.replace(text, replacement)
    }

    /** 展开 replacement 中的 $0..$9 为 MatchResult 捕获组。 */
    private fun expandGroups(replacement: String, match: MatchResult): String {
        var out = replacement
        for (i in match.groupValues.indices) out = out.replace("\$$i", match.groupValues[i])
        return out
    }
}
