package com.example.z_editor.datapack.smf

/**
 * PTX 纹理解码器（纯 Kotlin / JVM 可测，无 Android 依赖）。
 *
 * 解包出来的 `.ptx` 是 **raw 纹理字节，没有自描述头**（前 32 字节就是图像数据）。
 * 尺寸/格式必须由外部提供，来源是 **RSB 容器的 PTX_INFO 表**：
 *   RsbHeadInfo:      ptx_Number@84, ptxInfo_BeginOffset@88, ptxInfo_EachLength@92
 *   RsbPtxInfo 条目:  [0]width [4]height [8]check [12]format [16]alphaSize [20]alphaFormat
 *
 * 某个 image 条目的全局 PTX_INFO 下标 =
 *   `rsgpInfo[子组].ptxBeforeNumber + entry.part1Index`
 */
object PtxDecoder {

    data class PtxInfo(
        val width: Int,
        val height: Int,
        val check: Int,
        val format: Int,
        val alphaSize: Int,
        val alphaFormat: Int,
    ) {
        companion object {
            /** PopStudio：20 字节布局读不出 alphaFormat，由 alphaSize 派生（>0 即为 0x64 调色板）。 */
            const val ALPHA_FORMAT_PALETTE = 0x64
        }
    }

    // ---- 格式常量（PopStudio PtxFormat 的取值） ----
    const val FMT_ARGB8888 = 0
    const val FMT_RGBA4444 = 1
    const val FMT_RGB565 = 2
    const val FMT_RGBA5551 = 3
    const val FMT_ETC1_RGB = 32
    const val FMT_DXT1_RGB = 35
    const val FMT_DXT3_RGBA = 36
    const val FMT_DXT5_RGBA = 37
    const val FMT_ASTC_5x5 = 161
    const val FMT_ASTC_6x6 = 162

    /** ASTC 格式 → (块宽, 块高) */
    private val ASTC_BLOCK = mapOf(FMT_ASTC_5x5 to (5 to 5), FMT_ASTC_6x6 to (6 to 6))

    fun formatName(format: Int): String = when (format) {
        FMT_ARGB8888 -> "ARGB8888"
        FMT_RGBA4444 -> "RGBA4444"
        FMT_RGB565 -> "RGB565"
        FMT_RGBA5551 -> "RGBA5551"
        FMT_ETC1_RGB -> "ETC1_RGB"
        FMT_DXT1_RGB -> "DXT1_RGB"
        FMT_DXT3_RGBA -> "DXT3_RGBA"
        FMT_DXT5_RGBA -> "DXT5_RGBA"
        FMT_ASTC_5x5 -> "ASTC_5x5"
        FMT_ASTC_6x6 -> "ASTC_6x6"
        else -> "未知($format)"
    }

    /** 该格式是否已实现解码。UI 可据此提示"跳过 N 张未支持格式"。 */
    fun isSupported(format: Int): Boolean = when (format) {
        FMT_ARGB8888, FMT_RGBA4444, FMT_ETC1_RGB, FMT_DXT5_RGBA -> true
        in ASTC_BLOCK -> true
        else -> false
    }

    fun parsePtxInfo(bytes: ByteArray): PtxInfo {
        require(bytes.size >= 16) { "PTX_INFO 至少 16 字节，实际 ${bytes.size}" }
        val alphaSize = if (bytes.size >= 20) readU32LE(bytes, 16) else 0
        val alphaFormat = when {
            bytes.size >= 24 -> readU32LE(bytes, 20)              // 0x18：直接读
            bytes.size >= 20 -> if (alphaSize == 0) 0 else PtxInfo.ALPHA_FORMAT_PALETTE
            else -> 0                                             // 0x10：两个都没有
        }
        return PtxInfo(
            width = readU32LE(bytes, 0),
            height = readU32LE(bytes, 4),
            check = readU32LE(bytes, 8),
            format = readU32LE(bytes, 12),
            alphaSize = alphaSize,
            alphaFormat = alphaFormat,
        )
    }

    /**
     * 解码 PTX 为 ARGB 打包像素（IntArray，行主序，0xAARRGGBB）。
     *
     * @return 解码结果；尺寸非法或格式不支持时返回 null。
     * @throws IllegalArgumentException 数据长度不足（尺寸合法但字节不够，说明 PTX_INFO 对不上）
     */
    fun decode(data: ByteArray, info: PtxInfo): IntArray? {
        if (info.width <= 0 || info.height <= 0) return null
        val px = info.width.toLong() * info.height
        if (px > MAX_PIXELS) {
            throw IllegalArgumentException("纹理过大: ${info.width}x${info.height}")
        }
        return when (info.format) {
            FMT_ARGB8888 -> decodeArgb8888(data, info)
            FMT_RGBA4444 -> decodeRgba4444(data, info)
            FMT_ETC1_RGB -> decodeEtc1(data, info)
            FMT_DXT5_RGBA -> decodeDxt5(data, info)
            FMT_ASTC_5x5 -> decodeAstc(data, info, 5, 5)
            FMT_ASTC_6x6 -> decodeAstc(data, info, 6, 6)
            else -> null // 未支持格式
        }
    }

    private const val MAX_PIXELS = 64L * 1024 * 1024

    // ---- format 161/162: ASTC ----

    private fun decodeAstc(data: ByteArray, info: PtxInfo, bw: Int, bh: Int): IntArray {
        val blocks = ((info.width + bw - 1) / bw).toLong() * ((info.height + bh - 1) / bh)
        val need = blocks * 16
        require(data.size >= need) {
            "ASTC 数据不足: ${data.size} < $need (${info.width}x${info.height}, ${bw}x$bh)"
        }
        return AstcDecoder.decode(data, info.width, info.height, bw, bh)
    }

    // ---- format 0: ARGB8888（磁盘序 B,G,R,A） ----

    private fun decodeArgb8888(data: ByteArray, info: PtxInfo): IntArray {
        val w = info.width
        val h = info.height
        require(data.size >= w.toLong() * h * 4) { "ARGB8888 数据不足" }
        val out = IntArray(w * h)
        var di = 0
        for (i in out.indices) {
            val b = data[di].toInt() and 0xFF
            val g = data[di + 1].toInt() and 0xFF
            val r = data[di + 2].toInt() and 0xFF
            val a = data[di + 3].toInt() and 0xFF
            out[i] = (a shl 24) or (r shl 16) or (g shl 8) or b
            di += 4
        }
        return out
    }

    // ---- format 1: RGBA4444 ----
    //
    // 名字叫 RGBA4444，但 PopStudio `Texture/RGBA4444.Read` 取的半字节顺序是
    // **R 在高位、A 在低位**（r = temp >> 12, g = (temp & 0xF00) >> 8,
    // b = (temp & 0xF0) >> 4, a = temp & 0xF），即事实上的 ARGB4444。
    // 这里按 PopStudio 来，别按名字想当然。

    private fun decodeRgba4444(data: ByteArray, info: PtxInfo): IntArray {
        val w = info.width
        val h = info.height
        require(data.size >= w.toLong() * h * 2) { "RGBA4444 数据不足" }
        val out = IntArray(w * h)
        var di = 0
        for (i in out.indices) {
            val v = (data[di].toInt() and 0xFF) or ((data[di + 1].toInt() and 0xFF) shl 8)
            val r = ((v shr 12) and 0xF) * 0x11
            val g = ((v shr 8) and 0xF) * 0x11
            val b = ((v shr 4) and 0xF) * 0x11
            val a = (v and 0xF) * 0x11
            out[i] = (a shl 24) or (r shl 16) or (g shl 8) or b
            di += 2
        }
        return out
    }

    // ---- format 32: ETC1_RGB ----

    /**
     * ETC1 修饰符幅度表，逐字抄自 PopStudio `ETCEncode.ETC1Modifiers`（8 张表 × 2 个幅度，**全为正**）。
     * 索引方式：`[table][val] * (neg ? -1 : 1)`，其中 val 是索引位的 **LSB**、neg 是 MSB（即符号位）。
     */
    private val ETC1_MODIFIERS = arrayOf(
        intArrayOf(2, 8), intArrayOf(5, 17), intArrayOf(9, 29), intArrayOf(13, 42),
        intArrayOf(18, 60), intArrayOf(24, 80), intArrayOf(33, 106), intArrayOf(47, 183),
    )

    private fun decodeEtc1(data: ByteArray, info: PtxInfo): IntArray {
        val w = info.width
        val h = info.height
        val bw = (w + 3) / 4
        val bh = (h + 3) / 4
        require(data.size >= bw.toLong() * bh * 8) { "ETC1 数据不足" }
        val out = IntArray(w * h)
        var p = 0
        for (by in 0 until bh) {
            for (bx in 0 until bw) {
                val v = readU64BE(data, p)
                p += 8
                val diff = ((v shr 33) and 1L) == 1L
                val flip = ((v shr 32) and 1L) == 1L
                var r1: Int
                var g1: Int
                var b1: Int
                var r2: Int
                var g2: Int
                var b2: Int
                if (diff) {
                    var r = ((v shr 59) and 0x1F).toInt()
                    var g = ((v shr 51) and 0x1F).toInt()
                    var b = ((v shr 43) and 0x1F).toInt()
                    r1 = expand5(r); g1 = expand5(g); b1 = expand5(b)
                    r += signExtend3(((v shr 56) and 0x7).toInt())
                    g += signExtend3(((v shr 48) and 0x7).toInt())
                    b += signExtend3(((v shr 40) and 0x7).toInt())
                    r2 = expand5(r); g2 = expand5(g); b2 = expand5(b)
                } else {
                    r1 = ((v shr 60) and 0xF).toInt() * 0x11
                    g1 = ((v shr 52) and 0xF).toInt() * 0x11
                    b1 = ((v shr 44) and 0xF).toInt() * 0x11
                    r2 = ((v shr 56) and 0xF).toInt() * 0x11
                    g2 = ((v shr 48) and 0xF).toInt() * 0x11
                    b2 = ((v shr 40) and 0xF).toInt() * 0x11
                }
                val table1 = ((v shr 37) and 0x7).toInt()
                val table2 = ((v shr 34) and 0x7).toInt()
                val ox = bx * 4
                val oy = by * 4
                for (i in 0 until 4) {
                    for (j in 0 until 4) {
                        val x = ox + j
                        val y = oy + i
                        if (x >= w || y >= h) continue
                        // 像素索引位：(j<<2)|i；符号位：同偏移 +16
                        val bit = (j shl 2) or i
                        val valBit = ((v shr bit) and 1L).toInt()
                        val neg = ((v shr (bit + 16)) and 1L) == 1L
                        val add: Int
                        val cr: Int
                        val cg: Int
                        val cb: Int
                        if ((flip && i < 2) || (!flip && j < 2)) {
                            add = ETC1_MODIFIERS[table1][valBit] * (if (neg) -1 else 1)
                            cr = r1; cg = g1; cb = b1
                        } else {
                            add = ETC1_MODIFIERS[table2][valBit] * (if (neg) -1 else 1)
                            cr = r2; cg = g2; cb = b2
                        }
                        out[y * w + x] = (255 shl 24) or
                            (clamp255(cr + add) shl 16) or
                            (clamp255(cg + add) shl 8) or
                            clamp255(cb + add)
                    }
                }
            }
        }
        return out
    }

    private fun expand5(v: Int): Int = (v shl 3) or ((v and 0x1C) shr 2)

    private fun signExtend3(x: Int): Int = if (x and 4 != 0) x - 8 else x

    private fun clamp255(v: Int): Int = v.coerceIn(0, 255)

    // ---- format 37: DXT5_RGBA（BC3） ----

    private fun decodeDxt5(data: ByteArray, info: PtxInfo): IntArray {
        val w = info.width
        val h = info.height
        val bw = (w + 3) / 4
        val bh = (h + 3) / 4
        require(data.size >= bw.toLong() * bh * 16) { "DXT5 数据不足" }
        val out = IntArray(w * h)
        val alpha = IntArray(8)
        var p = 0
        for (by in 0 until bh) {
            for (bx in 0 until bw) {
                // alpha 端点 + 48-bit 3bit 索引
                val a0 = data[p].toInt() and 0xFF
                val a1 = data[p + 1].toInt() and 0xFF
                val a48 = (data[p + 2].toLong() and 0xFF) or
                    ((data[p + 3].toLong() and 0xFF) shl 8) or
                    ((data[p + 4].toLong() and 0xFF) shl 16) or
                    ((data[p + 5].toLong() and 0xFF) shl 24) or
                    ((data[p + 6].toLong() and 0xFF) shl 32) or
                    ((data[p + 7].toLong() and 0xFF) shl 40)
                if (a0 > a1) {
                    alpha[0] = a0; alpha[1] = a1
                    alpha[2] = (6 * a0 + a1) / 7; alpha[3] = (5 * a0 + 2 * a1) / 7
                    alpha[4] = (4 * a0 + 3 * a1) / 7; alpha[5] = (3 * a0 + 4 * a1) / 7
                    alpha[6] = (2 * a0 + 5 * a1) / 7; alpha[7] = (a0 + 6 * a1) / 7
                } else {
                    alpha[0] = a0; alpha[1] = a1
                    alpha[2] = (4 * a0 + a1) / 5; alpha[3] = (3 * a0 + 2 * a1) / 5
                    alpha[4] = (2 * a0 + 3 * a1) / 5; alpha[5] = (a0 + 4 * a1) / 5
                    alpha[6] = 0; alpha[7] = 255
                }
                // 颜色端点 + 2bit 颜色索引（行字节内 LSB 在前）
                val c0 = (data[p + 8].toInt() and 0xFF) or ((data[p + 9].toInt() and 0xFF) shl 8)
                val c1 = (data[p + 10].toInt() and 0xFF) or ((data[p + 11].toInt() and 0xFF) shl 8)
                val (r0, g0, b0) = unpack565(c0)
                val (r1, g1, b1) = unpack565(c1)
                val cr0 = r0; val cg0 = g0; val cb0 = b0
                val cr1 = r1; val cg1 = g1; val cb1 = b1
                val cr2 = (2 * cr0 + cr1) / 3; val cg2 = (2 * cg0 + cg1) / 3; val cb2 = (2 * cb0 + cb1) / 3
                val cr3 = (cr0 + 2 * cr1) / 3; val cg3 = (cg0 + 2 * cg1) / 3; val cb3 = (cb0 + 2 * cb1) / 3
                val ox = bx * 4
                val oy = by * 4
                for (i in 0 until 4) {
                    val ciByte = data[p + 12 + i].toInt() and 0xFF
                    for (j in 0 until 4) {
                        val x = ox + j
                        val y = oy + i
                        if (x >= w || y >= h) continue
                        val k = (i shl 2) or j
                        val a = alpha[((a48 ushr (3 * k)) and 7).toInt()]
                        when ((ciByte ushr (2 * j)) and 3) {
                            0 -> out[y * w + x] = (a shl 24) or (cr0 shl 16) or (cg0 shl 8) or cb0
                            1 -> out[y * w + x] = (a shl 24) or (cr1 shl 16) or (cg1 shl 8) or cb1
                            2 -> out[y * w + x] = (a shl 24) or (cr2 shl 16) or (cg2 shl 8) or cb2
                            else -> out[y * w + x] = (a shl 24) or (cr3 shl 16) or (cg3 shl 8) or cb3
                        }
                    }
                }
                p += 16
            }
        }
        return out
    }

    private fun unpack565(c: Int): Triple<Int, Int, Int> {
        val r = (c shr 11) and 0x1F
        val g = (c shr 5) and 0x3F
        val b = c and 0x1F
        return Triple((r shl 3) or (r shr 2), (g shl 2) or (g shr 4), (b shl 3) or (b shr 2))
    }

    // ---- 字节读取 ----

    private fun readU32LE(data: ByteArray, off: Int): Int =
        (data[off].toInt() and 0xFF) or
            ((data[off + 1].toInt() and 0xFF) shl 8) or
            ((data[off + 2].toInt() and 0xFF) shl 16) or
            ((data[off + 3].toInt() and 0xFF) shl 24)

    private fun readU64BE(data: ByteArray, off: Int): Long =
        ((data[off].toLong() and 0xFF) shl 56) or
            ((data[off + 1].toLong() and 0xFF) shl 48) or
            ((data[off + 2].toLong() and 0xFF) shl 40) or
            ((data[off + 3].toLong() and 0xFF) shl 32) or
            ((data[off + 4].toLong() and 0xFF) shl 24) or
            ((data[off + 5].toLong() and 0xFF) shl 16) or
            ((data[off + 6].toLong() and 0xFF) shl 8) or
            (data[off + 7].toLong() and 0xFF)
}
