package com.example.z_editor.datapack.pam

import com.google.gson.annotations.SerializedName

data class PamInfo(
    val version: Int,
    @SerializedName("frame_rate") val frameRate: Int,
    val position: List<Double>,
    val size: List<Double>,
    val image: List<PamImage>,
    val sprite: List<PamSprite>,
    @SerializedName("main_sprite") val mainSprite: PamSprite?,
)

data class PamImage(
    val name: String?,
    val size: List<Int>?,
    /**
     * 恒 6 项：`[0] [2] [1] [3]` 是 2x2 矩阵（缩放/旋转），`[4] [5]` 是平移。
     * 磁盘上的分量顺序就是 `[0] [2] [1] [3]`，读写两端已对称处理。
     */
    val transform: List<Double>,
)

data class PamSprite(
    val name: String?,
    /** v>=6 才写。 */
    val description: String?,
    @SerializedName("frame_rate") val frameRate: Double,
    /**
     * `[起始帧, 帧数]`。磁盘上其实是三个 i16（第三个 = `起始帧 + 帧数 - 1`），
     * 第三个可由前两个推出，已实测 471/471 成立，故不建模。
     * v4 没有 work_area，此处为 `[0, 帧数]`。
     */
    @SerializedName("work_area") val workArea: List<Int>,
    val frame: List<PamFrame>,
)

data class PamFrame(
    val label: String?,
    val stop: Boolean,
    val command: List<PamCommand>,
    val remove: List<PamRemove>,
    val append: List<PamAdd>,
    val change: List<PamMove>,
)

data class PamCommand(
    val command: String?,
    val parameter: String?,
)

data class PamRemove(
    val index: Int,
)

data class PamAdd(
    val index: Int,
    val name: String?,
    val resource: Int,
    val sprite: Boolean,
    val additive: Boolean,
    @SerializedName("preload_frames") val preloadFrames: Int,
    val timescale: Double?,
)

data class PamMove(
    val index: Int,
    /** 长度决定形态：6=矩阵、3=旋转、2=仅平移。末两位恒为平移。 */
    val transform: List<Double>,
    /** 4 项 RGBA，各 0..1；为 null 表示不带颜色。 */
    val color: List<Double>?,
    @SerializedName("src_rect") val srcRect: List<Double>?,
    @SerializedName("anim_frame_num") val animFrameNum: Int,
)

// ---------------------------------------------------------------- 归一化

private val IDENTITY_TRANSFORM = listOf(1.0, 0.0, 0.0, 1.0, 0.0, 0.0)

/** 矩阵模式需要 6 项，旋转模式 3 项，纯平移 2 项。其它长度无法无歧义解释，直接报错。 */
private val MOVE_TRANSFORM_SIZES = setOf(2, 3, 6)

/**
 * 把列表字段收敛为空列表。
 *
 * 声明上这些字段是非空的，但 **Gson 经 Unsafe 注入 null 时编译器并不知情**，
 * 所以此处收在可空接收者的扩展上，既不触发"多余的 safe call"告警，又真能兜住。
 */
private fun <T> List<T>?.orEmptyList(): List<T> = this ?: emptyList()

/**
 * 把可能来自手写 JSON 的模型补全成 [PamBinaryWriter] 能安全写出的形态。
 *
 * Gson 不执行 Kotlin 默认值，所以这里必须显式兜底：省略 `timescale` 会得到
 * `0.0` 而不是 `1.0`，写出去就是一个值为 0 的 timescale flag。
 *
 * @throws IllegalArgumentException 无法补全（长度非法的 transform、超过 2 项的 position 等）
 */
fun PamInfo.normalized(): PamInfo = copy(
    position = position.fixedDoubles(2, 0.0, "position"),
    size = size.fixedDoubles(2, 0.0, "size"),
    image = image.orEmptyList().map { it.normalized() },
    sprite = sprite.orEmptyList().map { it.normalized() },
    mainSprite = mainSprite?.normalized(),
)

private fun PamImage.normalized() = copy(
    size = size ?: listOf(-1, -1),
    transform = transform.toImageTransform(),
)

private fun PamSprite.normalized(): PamSprite {
    val frames = frame.orEmptyList()
    return copy(
        workArea = listOf(workArea.orEmptyList().firstOrNull() ?: 0, frames.size),
        frame = frames.map { it.normalized() },
    )
}

private fun PamFrame.normalized() = copy(
    command = command.orEmptyList(),
    remove = remove.orEmptyList(),
    append = append.orEmptyList().map { it.normalized() },
    change = change.orEmptyList(),
)

private fun PamAdd.normalized() = copy(timescale = timescale ?: 1.0)

private fun List<Double>?.toImageTransform(): List<Double> = when {
    this == null || size < 2 -> IDENTITY_TRANSFORM
    size == 2 -> listOf(1.0, 0.0, 0.0, 1.0, this[0], this[1])
    size == 4 -> listOf(this[0], this[1], this[2], this[3], 0.0, 0.0)
    else -> subList(0, 6)
}

private fun List<Double>?.fixedDoubles(size: Int, fill: Double, field: String): List<Double> {
    if (this == null) return List(size) { fill }
    require(this.size >= size) { "$field 至少需要 $size 项，实际 ${this.size} 项" }
    return subList(0, size)
}

/** writer 的强校验：形态非法时给出人话，而不是写出一个游戏读不懂的文件。 */
internal fun PamMove.requireWritableTransform(): List<Double> {
    val t = transform.orEmptyList()
    require(t.size in MOVE_TRANSFORM_SIZES) {
        "MovesInfo.transform 长度必须是 2（纯平移）/ 3（旋转）/ 6（矩阵），实际 ${t.size} 项 —— 无法写入"
    }
    return t
}
