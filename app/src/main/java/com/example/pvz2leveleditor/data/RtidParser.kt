package com.example.pvz2leveleditor.data

// 定义一个清晰的结构体
data class RtidInfo(
    val alias: String,   // 别名，如 DefaultSunDropper
    val source: String,  // 来源，如 LevelModules 或 CurrentLevel
    val fullString: String // 完整的 RTID 字符串
)

object RtidParser {
    /**
     * 解析 RTID(Alias@Source)
     * 返回 RtidInfo 对象，解析失败返回 null
     */
    fun parse(rtid: String): RtidInfo? {
        if (rtid.isBlank()) return null
        val regex = Regex("""RTID\((.*)@(.*)\)""")
        val match = regex.find(rtid) ?: return null

        return RtidInfo(
            alias = match.groupValues[1],
            source = match.groupValues[2],
            fullString = rtid
        )
    }

    /**
     * 构建标准 RTID 字符串
     */
    fun build(alias: String, source: String = "LevelModules"): String {
        return "RTID($alias@$source)"
    }
}