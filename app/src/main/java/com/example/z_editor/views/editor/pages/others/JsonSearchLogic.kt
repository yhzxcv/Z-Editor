package com.example.z_editor.views.editor.pages.others

/**
 * 搜索/查找逻辑层 —— 移植自 scripts/json_viewer_search.dart。
 *
 * 单行字面量查询走 indexOf 快路径（逐行语义）；正则与含换行的查询对**全文**做匹配，
 * 因此支持跨行命中（如搜索一段多行 JSON 片段，或 `[\s\S]` 之类跨行正则）。
 * 每条命中带全文偏移与起点所在行号。
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
                // 对全文匹配时仍保留 ^/$ 的行锚语义（等价旧的逐行匹配）；
                // 跨行由用户显式写 \n / [\s\S] / (?s) 实现。
                val flags = mutableSetOf(RegexOption.MULTILINE)
                if (!options.caseSensitive) flags += RegexOption.IGNORE_CASE
                Regex(query, flags)
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
     * 查找命中。
     * - 单行字面量查询（无换行、非 wholeWords）走 indexOf 快路径：无正则编译、无按行 substring 分配。
     * - 正则 / 含换行的字面量 / wholeWords：对**全文** [pattern.findAll]（可跨行），
     *   `lineIndex` 记录命中**起点**所在的行号（滚动定位用起点行即可看到整个命中）。
     */
    fun findMatches(text: String, query: String, options: JsonSearchOptions): List<JsonTextMatch> {
        if (query.isEmpty()) return emptyList()
        if (!options.regex && !options.wholeWords && query.indexOf('\n') < 0) {
            return findLiteralMatches(text, query, options.caseSensitive)
        }
        val pattern = buildPattern(query, options) ?: return emptyList()
        val result = mutableListOf<JsonTextMatch>()
        // 命中按起始偏移递增，行号随扫描推进（只前移），避免每个命中重扫全文。
        var lineIndex = 0
        var lineStart = 0
        for (match in pattern.findAll(text)) {
            val start = match.range.first
            while (lineStart <= start) {
                val nl = text.indexOf('\n', lineStart)
                if (nl < 0 || nl > start) break
                lineIndex++
                lineStart = nl + 1
            }
            result += JsonTextMatch(
                start = start,
                end = match.range.last + 1,
                lineIndex = lineIndex
            )
        }
        return result
    }

    /**
     * 字面量快路径：单遍扫描 + 全局游标推进（O(n)）。
     * indexOf 的起始位置 from 只前进不后退，行号/行起点随扫描增量推进。
     * 仅处理不含换行的查询（命中不跨行，已在 findMatches 排除含换行/wholeWords）。
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
