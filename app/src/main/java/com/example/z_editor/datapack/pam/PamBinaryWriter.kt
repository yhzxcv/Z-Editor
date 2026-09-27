package com.example.z_editor.datapack.pam

import kotlin.math.roundToInt

/**
 * PAM 二进制编码（纯 Kotlin / JVM 可测，无 Android 依赖）。
 * 另外所有字段都做范围校验后才写：手改过的 JSON 越界时给出人话，而不是写出一个
 * 能过编译、游戏读不懂的文件。
 */
object PamBinaryWriter {

    /** MovesInfo 从 v5 起用长坐标（i32），之前是 i16。 */
    private const val MOVE_LONG_COORDS_MIN_VERSION = 5

    fun encode(raw: PamInfo): ByteArray {
        val version = raw.version
        require(version in PamBinaryReader.MIN_VERSION..PamBinaryReader.MAX_VERSION) {
            "只能写出 v${PamBinaryReader.MIN_VERSION}–v${PamBinaryReader.MAX_VERSION}，模型却是 v$version"
        }
        val info = raw.normalized()
        val out = Output()

        out.i32Raw(PamBinaryReader.MAGIC)
        out.i32Raw(version)
        out.u8(info.frameRate, "全局帧率")
        // position 有符号、size 无符号 —— 与 PamBinaryReader 的那段说明配套，别改单边
        out.scaled16(info.position[0], 20.0, "position.x")
        out.scaled16(info.position[1], 20.0, "position.y")
        out.scaledU16(info.size[0], 20.0, "size.w")
        out.scaledU16(info.size[1], 20.0, "size.h")

        out.i16(info.image.size, "图集数")
        info.image.forEach { writeImage(out, it) }

        out.i16(info.sprite.size, "精灵数")
        info.sprite.forEach { writeSprite(out, it, version) }

        // v4+：主精灵前面多一个存在位（v<=3 是必写，本项目不支持）
        val main = info.mainSprite
        out.u8Raw(if (main != null) 1 else 0)
        if (main != null) writeSprite(out, main, version)

        return out.toByteArray()
    }

    private fun writeImage(out: Output, img: PamImage) {
        out.string16(img.name, "图集名")
        val size = img.size
        require(size != null && size.size >= 2) { "图集 size 需要 2 项，实际 ${size?.size ?: 0} 项" }
        out.i16(size[0], "图集宽")
        out.i16(size[1], "图集高")
        val t = img.transform
        require(t.size >= 6) { "图集 transform 需要 6 项，实际 ${t.size} 项" }
        out.scaled32(t[0], 1310720.0, "图集 transform[0]")
        out.scaled32(t[2], 1310720.0, "图集 transform[2]")
        out.scaled32(t[1], 1310720.0, "图集 transform[1]")
        out.scaled32(t[3], 1310720.0, "图集 transform[3]")
        out.scaled16(t[4], 20.0, "图集 translate.x")
        out.scaled16(t[5], 20.0, "图集 translate.y")
    }

    private fun writeSprite(out: Output, sprite: PamSprite, version: Int) {
        out.string16(sprite.name, "精灵名")
        if (version >= 6) out.string16(sprite.description, "精灵描述")
        out.scaled32(sprite.frameRate, 65536.0, "精灵帧率")

        val count = sprite.frame.size
        out.i16(count, "帧数")
        if (version >= 5) {
            val start = sprite.workArea.firstOrNull() ?: 0
            out.i16(start, "work_area 起点")
            out.i16(start + count - 1, "work_area 终点")
        }
        sprite.frame.forEach { writeFrame(out, it, version) }
    }

    private fun writeFrame(out: Output, frame: PamFrame, version: Int) {
        var flags = 0
        if (frame.remove.isNotEmpty()) flags = flags or F_REMOVES
        if (frame.append.isNotEmpty()) flags = flags or F_ADDS
        if (frame.change.isNotEmpty()) flags = flags or F_MOVES
        if (frame.label != null) flags = flags or F_FRAME_NAME
        if (frame.stop) flags = flags or F_STOP
        if (frame.command.isNotEmpty()) flags = flags or F_COMMANDS
        out.u8Raw(flags)

        if (flags and F_REMOVES != 0) {
            writeCount(out, frame.remove.size, "Removes")
            frame.remove.forEach { writeRemove(out, it) }
        }
        if (flags and F_ADDS != 0) {
            writeCount(out, frame.append.size, "Adds")
            frame.append.forEach { writeAdd(out, it, version) }
        }
        if (flags and F_MOVES != 0) {
            writeCount(out, frame.change.size, "Moves")
            frame.change.forEach { writeMove(out, it, version) }
        }
        if (flags and F_FRAME_NAME != 0) out.string16(frame.label, "帧名")
        // Stop 位没有载荷
        if (flags and F_COMMANDS != 0) {
            require(frame.command.size <= 255) {
                "某帧的命令条数为 ${frame.command.size}，超过格式上限 255 —— 拒绝写出"
            }
            out.u8Raw(frame.command.size)
            frame.command.forEach {
                out.string16(it.command, "command")
                out.string16(it.parameter, "parameter")
            }
        }
    }

    /** Removes/Adds/Moves 的条数：u8，等于 255 时改用 0xFF 标记 + i16 真值。 */
    private fun writeCount(out: Output, n: Int, what: String) {
        if (n < 255) {
            out.u8Raw(n)
        } else {
            out.u8Raw(0xFF)
            out.i16(n, "$what 条数")
        }
    }

    private fun writeRemove(out: Output, remove: PamRemove) {
        if (remove.index >= REMOVE_INDEX_ESCAPE) {
            out.i16(REMOVE_INDEX_ESCAPE, "remove.index 逃逸标记")
            out.i32Raw(remove.index)
        } else {
            out.i16(remove.index, "remove.index")
        }
    }

    private fun writeAdd(out: Output, add: PamAdd, version: Int) {
        // 标志字写在记录最前面，但数值读完才知道 —— 先占位，最后回填
        val at = out.beginBackfill16()
        var flags = 0
        if (add.index >= ADD_INDEX_MASK || add.index < 0) {
            flags = flags or ADD_INDEX_MASK
            out.i32Raw(add.index)
        } else {
            flags = flags or add.index
        }
        if (add.sprite) flags = flags or A_SPRITE
        if (add.additive) flags = flags or A_ADDITIVE

        if (version >= 6 && (add.resource >= 255 || add.resource < 0)) {
            out.u8Raw(0xFF)
            out.i16(add.resource, "append.resource")
        } else {
            out.u8(add.resource, "append.resource")
        }
        if (add.preloadFrames != 0) {
            flags = flags or A_PRELOAD
            out.i16(add.preloadFrames, "append.preload_frames")
        }
        if (add.name != null) {
            flags = flags or A_NAME
            out.string16(add.name, "append.name")
        }
        // encode() 已经过 normalized()，此处 ?: 只是给直接调用者兜底；
        // 注意 0.0 是合法值（会写一个值为 0 的标志位），只有 null 才算"省略"。
        val timescale = add.timescale ?: 1.0
        if (timescale != 1.0) {
            flags = flags or A_TIMESCALE
            out.i32Raw((timescale * 65536.0).roundToInt())
        }
        out.endBackfill16(at, flags)
    }

    private fun writeMove(out: Output, move: PamMove, version: Int) {
        val t = move.requireWritableTransform()
        val at = out.beginBackfill16()
        var flags = 0
        if (move.index >= MOVE_INDEX_MASK || move.index < 0) {
            flags = flags or MOVE_INDEX_MASK
            out.i32Raw(move.index)
        } else {
            flags = flags or move.index
        }

        // transform 的长度决定形态：6=矩阵、3=旋转、2=纯平移
        // 六项那支的下标交叉是列主序↔行主序的换算，与 reader 互逆 —— 见 writeImage 的说明
        if (t.size == 6) {
            flags = flags or M_MATRIX
            out.scaled32(t[0], 65536.0, "change.matrix[0]")
            out.scaled32(t[2], 65536.0, "change.matrix[2]")
            out.scaled32(t[1], 65536.0, "change.matrix[1]")
            out.scaled32(t[3], 65536.0, "change.matrix[3]")
        } else if (t.size == 3) {
            flags = flags or M_ROTATE
            out.scaled16(t[0], 1000.0, "change.rotate")
        }
        val tx = t[t.size - 2]
        val ty = t[t.size - 1]
        if (version >= MOVE_LONG_COORDS_MIN_VERSION) {
            flags = flags or M_LONG_COORDS
            out.i32Raw((tx * 20.0).roundToInt())
            out.i32Raw((ty * 20.0).roundToInt())
        } else {
            out.scaled16(tx, 20.0, "change.translate.x")
            out.scaled16(ty, 20.0, "change.translate.y")
        }

        val srcRect = move.srcRect
        if (srcRect != null) {
            require(srcRect.size >= 4) { "change.src_rect 需要 4 项，实际 ${srcRect.size} 项" }
            flags = flags or M_SRC_RECT
            for (i in 0..3) out.scaled16(srcRect[i], 20.0, "change.src_rect[$i]")
        }
        val color = move.color
        if (color != null) {
            require(color.size >= 4) { "change.color 需要 4 项，实际 ${color.size} 项" }
            flags = flags or M_COLOR
            for (i in 0..3) out.u8((color[i] * 255.0).roundToInt(), "change.color[$i]（${color[i]} × 255）")
        }
        if (move.animFrameNum != 0) {
            flags = flags or M_ANIM_FRAME_NUM
            out.i16(move.animFrameNum, "change.anim_frame_num")
        }
        out.endBackfill16(at, flags)
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

/**
 * 小端输出缓冲。带"占位后回填"能力：`AddsInfo`/`MovesInfo` 的标志字物理位置在记录
 * 最前面，数值却是读完才知道，所以先写两个字节的 0，最后就地补上。
 */
private class Output(initial: Int = 8192) {

    private var buf = ByteArray(initial)
    private var len = 0

    private fun ensure(n: Int) {
        if (len + n > buf.size) buf = buf.copyOf(maxOf(buf.size * 2, len + n))
    }

    fun rawData(b: ByteArray) {
        ensure(b.size)
        b.copyInto(buf, len)
        len += b.size
    }

    fun u8Raw(v: Int) {
        ensure(1)
        buf[len++] = v.toByte()
    }

    fun i16Raw(v: Int) {
        ensure(2)
        buf[len++] = v.toByte()
        buf[len++] = (v shr 8).toByte()
    }

    fun i32Raw(v: Int) {
        ensure(4)
        buf[len++] = v.toByte()
        buf[len++] = (v shr 8).toByte()
        buf[len++] = (v shr 16).toByte()
        buf[len++] = (v shr 24).toByte()
    }

    fun u8(v: Int, what: String) {
        require(v in 0..255) { "$what = $v，超出 0..255" }
        u8Raw(v)
    }

    fun i16(v: Int, what: String) {
        require(v in Short.MIN_VALUE.toInt()..Short.MAX_VALUE.toInt()) {
            "$what = $v，超出 16 位有符号范围 -32768..32767"
        }
        i16Raw(v)
    }

    /** 写入"乘以 factor 后取整"的 i16。四舍五入而非截断，见 [PamBinaryWriter] 的说明。 */
    fun scaled16(v: Double, factor: Double, what: String) {
        val scaled = v * factor
        require(!scaled.isNaN()) { "$what = $v，不是有效数字" }
        i16(scaled.roundToInt(), "$what（$v × $factor 取整）")
    }

    /**
     * 同 [scaled16]，但落成 **u16**（0..65535）。
     *
     * **只给头部的 `size` 用**：它是尺寸、不可能为负，而全屏背景能到 2304×1536
     * （÷20 = 46080 > 32767），走 [scaled16] 会被范围检查拦下。紧邻的 `position`
     * 是有符号的，仍走 [scaled16] —— 这个不对称有实测依据，见 `PamBinaryReader.decode`。
     */
    fun scaledU16(v: Double, factor: Double, what: String) {
        val scaled = v * factor
        require(!scaled.isNaN()) { "$what = $v，不是有效数字" }
        val r = scaled.roundToInt()
        require(r in 0..65535) { "$what = $v，换算后为 $r，超出 16 位无符号范围 0..65535" }
        i16Raw(r)
    }

    /** 同上，但落成 i32（矩阵分量、精灵帧率走这条）。 */
    fun scaled32(v: Double, factor: Double, what: String) {
        val scaled = v * factor
        require(!scaled.isNaN()) { "$what = $v，不是有效数字" }
        require(scaled >= Int.MIN_VALUE.toDouble() && scaled <= Int.MAX_VALUE.toDouble()) {
            "$what = $v 换算后为 $scaled，超出 32 位有符号范围"
        }
        i32Raw(scaled.roundToInt())
    }

    fun string16(s: String?, what: String) {
        val bytes = (s ?: "").toByteArray(Charsets.UTF_8)
        require(bytes.size <= 0xFFFF) { "$what 有 ${bytes.size} 字节，超过 u16 长度上限 65535" }
        i16Raw(bytes.size)
        rawData(bytes)
    }

    fun beginBackfill16(): Int {
        val at = len
        i16Raw(0)
        return at
    }

    fun endBackfill16(at: Int, v: Int) {
        buf[at] = v.toByte()
        buf[at + 1] = (v shr 8).toByte()
    }

    fun toByteArray(): ByteArray = buf.copyOf(len)
}
