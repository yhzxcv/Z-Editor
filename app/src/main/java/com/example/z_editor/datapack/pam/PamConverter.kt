package com.example.z_editor.datapack.pam

/**
 * PAM 转换的对外门面（纯 Kotlin / JVM 可测，无 Android 依赖）。
 *
 * 两个入口（数据包工具里的独立页、SMF 解包页的开关）都走这里，保证「同一份输入
 * 得到同一份输出」，也保证两条路径的测试覆盖是同一套。
 */
object PamConverter {

    enum class Direction(val outputExtension: String) {
        PamToJson("json"),
        JsonToPam("pam"),
    }

    enum class SourceFormat {
        Pam,
        Json,

        /** 既不是 PAM 二进制也不像 JSON —— 入口页据此提示用户。 */
        Unknown,
    }

    /** 识别卡展示用的摘要。 */
    data class Summary(
        val version: Int,
        val frameRate: Int,
        val imageCount: Int,
        val spriteCount: Int,
        val frameCount: Int,
    )

    /**
     * 内容嗅探。用内容而不是扩展名：数据包里 `.PAM` 大小写混杂，用户手填路径时
     * 扩展名也不一定对。
     */
    fun detect(data: ByteArray): SourceFormat = when {
        PamBinaryReader.looksLikePam(data) -> SourceFormat.Pam
        PamJson.looksLikeJson(data) -> SourceFormat.Json
        else -> SourceFormat.Unknown
    }

    /**
     * @throws IllegalArgumentException 输入与方向不匹配、或数据非法 —— 消息是给人看的
     */
    fun convert(data: ByteArray, direction: Direction): ByteArray = when (direction) {
        Direction.PamToJson -> PamJson.encode(PamBinaryReader.decode(data)).toByteArray(Charsets.UTF_8)
        Direction.JsonToPam -> PamBinaryWriter.encode(PamJson.decode(String(data, Charsets.UTF_8)))
    }

    fun summarize(info: PamInfo): Summary = Summary(
        version = info.version,
        frameRate = info.frameRate,
        imageCount = info.image.size,
        spriteCount = info.sprite.size,
        frameCount = info.sprite.sumOf { it.frame.size } + (info.mainSprite?.frame?.size ?: 0),
    )
}
