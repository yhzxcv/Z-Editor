package com.example.z_editor.datapack.pam

import java.nio.ByteBuffer
import java.nio.charset.CharacterCodingException
import java.nio.charset.CodingErrorAction

/**
 * PAM 二进制解码（纯 Kotlin / JVM 可测，无 Android 依赖）。
 *
 * 格式：magic `0xBAF01954` + i32 版本 + 全局头 + 图集表 + 精灵表 + 帧表。
 * 帧记录是 **flag 驱动的变长结构**，`AddsInfo` / `MovesInfo` 的标志字还写在记录
 * 最前面、数值却是读完才知道（写的时候要回填）。完整规格见
 * `C:\Users\Y\.claude\plans\lexical-skipping-nebula.md`。
 *
 * 只支持 **v4–v6**：v1–v3 的 `ImageInfo` 旋转用的是另一套编码（`acos`/`sin` 反推），
 * 且 PvZ2 用不到，明确报错好过默默解错。
 *
 * 所有越界都抛 [IllegalArgumentException] 并带字节偏移 —— 宁可报错，绝不静默错位
 * （错位会让后面的字段全部读到垃圾值，最后写出一个能过编译、游戏读不懂的文件）。
 */
object PamBinaryReader {

    val MAGIC: Int = 0xBAF01954.toInt()
    const val MIN_VERSION = 4
    const val MAX_VERSION = 6

    /** 廉价嗅探，供入口页判断"这是不是一个 PAM"。 */
    fun looksLikePam(data: ByteArray): Boolean =
        data.size >= 8 && Cursor(data).i32("magic") == MAGIC

    @Throws(IllegalArgumentException::class)
    fun decode(data: ByteArray): PamInfo {
        val c = Cursor(data)
        val magic = c.i32("文件头 magic")
        require(magic == MAGIC) {
            "不是 PAM 文件：magic 应为 0x%08X，实际 0x%08X".format(MAGIC, magic)
        }
        val version = c.i32("版本号")
        require(version in MIN_VERSION..MAX_VERSION) {
            if (version in 1..3) {
                "暂不支持 PAM v$version（仅支持 v$MIN_VERSION–v$MAX_VERSION；v1–v3 的图集旋转是另一套编码）"
            } else {
                "不支持的 PAM 版本 v$version（本工具支持 v$MIN_VERSION–v$MAX_VERSION）"
            }
        }

        val frameRate = c.u8("全局帧率")
        val position = listOf(c.i16("position.x") / 20.0, c.i16("position.y") / 20.0)
        val size = listOf(c.u16("size.w") / 20.0, c.u16("size.h") / 20.0)

        val images = List(c.count16("图集数")) { readImage(c) }
        val sprites = List(c.count16("精灵数")) { readSprite(c, version) }

        // v4+ 主精灵带一个存在位；v<=3 是必写，本项目不支持
        val hasMain = c.u8("主精灵存在位") != 0
        val mainSprite = if (hasMain) readSprite(c, version) else null

        // 帧表读完必须刚好到底：多出来的字节说明上面某处读少了（flag 位判断错、
        // 漏读一个字段……），此时我们已经交出一个**看着正常、实则缺胳膊少腿**的模型。
        // 实测 565 个真文件全部刚好到底，所以这里宁可报错。
        require(c.pos == data.size) {
            "PAM 解析到第 ${c.pos} 字节就结束了，但文件有 ${data.size} 字节 —— 尾部多出 ${data.size - c.pos} 字节，" +
                "说明结构解析错位（多半是某个标志位的载荷长度判断有误）"
        }

        return PamInfo(
            version = version,
            frameRate = frameRate,
            position = position,
            size = size,
            image = images,
            sprite = sprites,
            mainSprite = mainSprite,
        )
    }

    private fun readImage(c: Cursor): PamImage {
        val name = c.string16("图集名")
        val width = c.i16("图集宽")
        val height = c.i16("图集高")
        val r0 = c.i32("图集 transform[0]") / 1310720.0
        val r1 = c.i32("图集 transform[2]") / 1310720.0
        val r2 = c.i32("图集 transform[1]") / 1310720.0
        val r3 = c.i32("图集 transform[3]") / 1310720.0
        return PamImage(
            name = name,
            size = listOf(width, height),
            transform = listOf(
                r0, r2, r1, r3,
                c.i16("图集 translate.x") / 20.0,
                c.i16("图集 translate.y") / 20.0,
            ),
        )
    }

    private fun readSprite(c: Cursor, version: Int): PamSprite {
        val name = c.string16("精灵名")
        val description = if (version >= 6) c.string16("精灵描述") else null
        val frameRate = c.i32("精灵帧率") / 65536.0
        val framesCount = c.count16("帧数")
        // v>=5 磁盘上是三个 i16：[帧数][work_area 起点][起点+帧数-1]；
        // 第三个可由前两个推出，读完即弃（已实测 471/471 成立）
        val workAreaStart = if (version >= 5) c.i16("work_area 起点") else 0
        if (version >= 5) c.i16("work_area 终点")
        val frames = List(framesCount) { readFrame(c, version) }
        return PamSprite(
            name = name,
            description = description,
            frameRate = frameRate,
            workArea = listOf(workAreaStart, framesCount),
            frame = frames,
        )
    }

    private fun readFrame(c: Cursor, version: Int): PamFrame {
        val flags = c.u8("帧标志")
        val remove = if (flags and F_REMOVES != 0) List(c.payloadCount("Removes")) {
            var index = c.i16("remove.index")
            if (index >= REMOVE_INDEX_ESCAPE) index = c.i32("remove.index(长)")
            PamRemove(index)
        } else emptyList()

        val append = if (flags and F_ADDS != 0) List(c.payloadCount("Adds")) {
            readAdd(c, version)
        } else emptyList()

        val change = if (flags and F_MOVES != 0) List(c.payloadCount("Moves")) {
            readMove(c)
        } else emptyList()

        val label = if (flags and F_FRAME_NAME != 0) c.string16("帧名") else null
        val stop = flags and F_STOP != 0
        // Commands 的条数是裸 u8，没有 0xFF 逃逸，算术上限就是 255
        val command = if (flags and F_COMMANDS != 0) List(c.u8("Commands 条数")) {
            PamCommand(c.string16("command"), c.string16("parameter"))
        } else emptyList()

        return PamFrame(label, stop, command, remove, append, change)
    }

    private fun readAdd(c: Cursor, version: Int): PamAdd {
        val flags = c.u16("append 标志")
        var index = flags and ADD_INDEX_MASK
        if (index == ADD_INDEX_MASK) index = c.i32("append.index(长)")
        var resource = c.u8("append.resource")
        if (version >= 6 && resource == 0xFF) resource = c.i16("append.resource(长)")

        // 先按**磁盘顺序**读进局部变量，再拼装对象。
        //
        // 别图省事把它们塞进 PamAdd(...) 的具名参数里：Kotlin 是按**书写顺序**求值的，
        // 而这里巧的是 `name` 在参数表里排在 `preload_frames` 前面。一旦写成具名参数，
        // 就会先去读名字、再去读 preload，整条记录当场错位。真样本里没有一个同时置了
        // 这两个位，所以往返测试 565/565 全绿也照样漏掉 —— 是夹具把它揪出来的。
        val preloadFrames = if (flags and A_PRELOAD != 0) c.i16("append.preload_frames") else 0
        val name = if (flags and A_NAME != 0) c.string16("append.name") else null
        val timescale = if (flags and A_TIMESCALE != 0) c.i32("append.timescale") / 65536.0 else 1.0

        return PamAdd(
            index = index,
            name = name,
            resource = resource,
            sprite = flags and A_SPRITE != 0,
            additive = flags and A_ADDITIVE != 0,
            preloadFrames = preloadFrames,
            timescale = timescale,
        )
    }

    private fun readMove(c: Cursor): PamMove {
        val flags = c.u16("change 标志")
        var index = flags and MOVE_INDEX_MASK
        if (index == MOVE_INDEX_MASK) index = c.i32("change.index(长)")

        require(flags and M_MATRIX == 0 || flags and M_ROTATE == 0) {
            "PAM 第 ${c.pos - 2} 字节处的 MovesInfo 同时置了 Matrix 与 Rotate 标志位 —— 无法解释，拒绝解码"
        }

        // 标志位决定 transform 的形态；末两位恒为平移，稍后回填
        val transform = when {
            flags and M_MATRIX != 0 -> {
                val r0 = c.i32("change.matrix[0]") / 65536.0
                val r1 = c.i32("change.matrix[2]") / 65536.0
                val r2 = c.i32("change.matrix[1]") / 65536.0
                val r3 = c.i32("change.matrix[3]") / 65536.0
                mutableListOf(r0, r2, r1, r3, 0.0, 0.0)
            }

            flags and M_ROTATE != 0 -> mutableListOf(c.i16("change.rotate") / 1000.0, 0.0, 0.0)
            else -> mutableListOf(0.0, 0.0)
        }
        if (flags and M_LONG_COORDS != 0) {
            transform[transform.size - 2] = c.i32("change.translate.x") / 20.0
            transform[transform.size - 1] = c.i32("change.translate.y") / 20.0
        } else {
            transform[transform.size - 2] = c.i16("change.translate.x") / 20.0
            transform[transform.size - 1] = c.i16("change.translate.y") / 20.0
        }

        val srcRect = if (flags and M_SRC_RECT != 0) List(4) { c.i16("change.src_rect") / 20.0 } else null
        val color = if (flags and M_COLOR != 0) List(4) { c.u8("change.color") / 255.0 } else null
        val animFrameNum = if (flags and M_ANIM_FRAME_NUM != 0) c.i16("change.anim_frame_num") else 0

        return PamMove(
            index = index,
            transform = transform,
            srcRect = srcRect,
            color = color,
            animFrameNum = animFrameNum,
        )
    }

    // ---- 帧标志 ----
    private const val F_REMOVES = 1
    private const val F_ADDS = 2
    private const val F_MOVES = 4
    private const val F_FRAME_NAME = 8
    private const val F_STOP = 16
    private const val F_COMMANDS = 32

    // ---- AddsInfo 标志位 ----
    private const val ADD_INDEX_MASK = 2047
    private const val A_TIMESCALE = 2048
    private const val A_NAME = 4096
    private const val A_PRELOAD = 8192
    private const val A_ADDITIVE = 16384
    private const val A_SPRITE = 32768

    // ---- MovesInfo 标志位 ----
    private const val MOVE_INDEX_MASK = 1023
    private const val M_ANIM_FRAME_NUM = 1024
    private const val M_LONG_COORDS = 2048
    private const val M_MATRIX = 4096
    private const val M_COLOR = 8192
    private const val M_ROTATE = 16384
    private const val M_SRC_RECT = 32768

    private const val REMOVE_INDEX_ESCAPE = 2047
}

/** 小端游标。所有读取都先验长度，越界即抛带偏移量的异常。 */
private class Cursor(private val d: ByteArray) {

    var pos = 0
        private set

    private fun need(n: Int, what: String) {
        if (pos + n > d.size) {
            throw IllegalArgumentException(
                "PAM 数据在第 $pos 字节处要读 $what（$n 字节），但只剩 ${d.size - pos} 字节 —— 文件被截断或结构解析错位"
            )
        }
    }

    fun u8(what: String): Int {
        need(1, what)
        return d[pos++].toInt() and 0xFF
    }

    fun u16(what: String): Int {
        need(2, what)
        val v = (d[pos].toInt() and 0xFF) or ((d[pos + 1].toInt() and 0xFF) shl 8)
        pos += 2
        return v
    }

    fun i16(what: String): Int {
        need(2, what)
        val v = (d[pos].toInt() and 0xFF) or (d[pos + 1].toInt() shl 8)
        pos += 2
        return v.toShort().toInt()
    }

    fun i32(what: String): Int {
        need(4, what)
        val v = (d[pos].toInt() and 0xFF) or
            ((d[pos + 1].toInt() and 0xFF) shl 8) or
            ((d[pos + 2].toInt() and 0xFF) shl 16) or
            ((d[pos + 3].toInt() and 0xFF) shl 24)
        pos += 4
        return v
    }

    /** i16 计数（图集数、精灵数、帧数）：负数只可能是错位，直接报错。 */
    fun count16(what: String): Int {
        val n = i16(what)
        require(n >= 0) { "PAM 第 ${pos - 2} 字节处的 $what 为负数（$n）—— 结构解析错位" }
        return n
    }

    /** flag 载荷的条数：u8，等于 0xFF 时说明真值在后面的 i16 里。 */
    fun payloadCount(what: String): Int {
        val n = u8("$what 条数")
        if (n != 0xFF) return n
        val big = i16("$what 条数(长)")
        require(big >= 0) { "PAM 第 ${pos - 2} 字节处的 $what 条数为负数（$big）—— 结构解析错位" }
        return big
    }

    /**
     * u16 长度前缀的 UTF-8 字符串。
     *
     * 用**严格**解码器：遇到非法字节宁可报错，也不让替换字符（U+FFFD）悄悄进来 ——
     * 那样回写时字节数会变，逐字节往返就废了。
     */
    fun string16(what: String): String {
        val n = u16("$what 长度")
        need(n, what)
        val s = try {
            Charsets.UTF_8.newDecoder()
                .onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT)
                .decode(ByteBuffer.wrap(d, pos, n))
                .toString()
        } catch (e: CharacterCodingException) {
            throw IllegalArgumentException(
                "PAM 第 $pos 字节处的 $what 不是合法 UTF-8（声明长度 $n 字节）—— 拒绝解码，以免静默损坏名称", e
            )
        }
        pos += n
        return s
    }
}
