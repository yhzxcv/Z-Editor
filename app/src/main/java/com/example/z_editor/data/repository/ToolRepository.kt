package com.example.z_editor.data.repository

data class ToolCardInfo(
    val id: String,
    val name: String,
    val icon: String? = null
)

object ToolRepository {
    private val TOOL_CARDS = listOf(
        ToolCardInfo("tool_powertile_alpha", "绿色瓷砖", "tool_powertile_alpha.webp"),
        ToolCardInfo("tool_powertile_beta", "红色瓷砖", "tool_powertile_beta.webp"),
        ToolCardInfo("tool_powertile_gamma", "青色瓷砖", "tool_powertile_gamma.webp"),
        ToolCardInfo("tool_powertile_delta", "黄色瓷砖", "tool_powertile_delta.webp"),
        ToolCardInfo("tool_projectile_bowlingbulb1", "保龄泡泡小青球", "tool_projectile_bowlingbulb1.webp"),
        ToolCardInfo("tool_projectile_bowlingbulb2", "保龄泡泡中蓝球", "tool_projectile_bowlingbulb2.webp"),
        ToolCardInfo("tool_projectile_bowlingbulb3", "保龄泡泡大黄球", "tool_projectile_bowlingbulb3.webp"),
        ToolCardInfo("tool_projectile_bowlingbulb_explode", "保龄泡泡大招球", "tool_projectile_bowlingbulb_explode.webp"),
        ToolCardInfo("tool_projectile_wallnut", "坚果保龄球", "tool_projectile_wallnut.webp"),
        ToolCardInfo("tool_projectile_wallnut_big", "大坚果保龄球", "tool_projectile_wallnut_big.webp"),
        ToolCardInfo("tool_projectile_wallnut_explode", "爆炸坚果保龄球", "tool_projectile_wallnut_explode.webp"),
        ToolCardInfo("tool_projectile_wallnut_primeval", "原始坚果保龄球", "tool_projectile_wallnut_primeval.webp"),
        ToolCardInfo("tool_projectile_jackfruit", "菠萝蜜保龄球", "tool_projectile_jackfruit.webp")
    )


    fun get(id: String): ToolCardInfo? {
        return TOOL_CARDS.find { it.id == id }
    }

    fun getAll(): List<ToolCardInfo> = TOOL_CARDS
}