package com.example.z_editor.datapack.ui

/** 图层管理弹窗里三个批量按钮各自要做什么。 */
internal enum class LayerBatch {
    /** 全选：作用范围内的层全显示出来。 */
    Show,

    /** 全不选：作用范围内的层全藏起来。 */
    Hide,

    /** 反选：作用范围内藏着的变显示、显示着的变藏。 */
    Invert,
}

/**
 * 批量按钮的落点：只改**作用范围** `targets` 里的层，范围外的一律不动。
 *
 * 抽成纯函数是为了能单测 —— 2026-09-27 手推反选时第一版写成了
 * `(hidden - t) + (hidden ∩ t)`：`hidden ∩ t` 本来就在 `hidden` 里，后半截等于白算，
 * 结果只会把子集里的层越删越多。正确写法是**对称差**：
 * `hidden xor t = (hidden - t) + (t - hidden)`。这种"看着像对"的式子，
 * 只有拿几个具体集合跑一遍才露馅。
 *
 * 作用范围就是弹窗里**搜索过滤后**的那些层（搜索框空着时是全部图层）——
 * 搜出 3 条再点「全不选」，藏掉的只有那 3 条，否则搜完点一下、结果跟搜了什么无关，
 * 搜索就白搜了。
 *
 * @param hidden 当前藏起来的层号
 * @param targets 本次要作用到的层号
 */
internal fun applyLayerBatch(hidden: Set<Int>, targets: Set<Int>, mode: LayerBatch): Set<Int> =
    when (mode) {
        LayerBatch.Show -> hidden - targets
        LayerBatch.Hide -> hidden + targets
        LayerBatch.Invert -> (hidden - targets) + (targets - hidden)
    }
