package com.example.z_editor.datapack.smf

/**
 * RSB 容器里跟**纹理**有关的那几张表（纯 Kotlin，无 Android 依赖）。
 *
 * 容器的整体解包（子组遍历、data/image 段解压、文件落盘）在 [SmfUnpacker] 里；
 * 这里只负责"**某个 image 条目对应哪一行 PTX_INFO**"这件事——这正是 PTX→PNG
 * 能不能解对的关键，也是之前那版实现栽跟头的地方（当时误把文件列表中 image
 * 条目尾部的 20 字节 RsgpPart1ExtraInfo 当成了 PtxInfo）。
 *
 * 全部读取都小端。**磁盘上的 magic 是反转 ASCII**：RSB = `1bsr`、RSGP = `pgsr`。
 */
object RsbTextureIndex {

    data class RsbHeader(
        val version: Int,
        val rsgpCount: Int,
        val rsgpInfoOffset: Int,
        val rsgpInfoStride: Int,
        val ptxCount: Int,
        val ptxInfoOffset: Int,
        val ptxInfoStride: Int,
    )

    data class RsgpInfo(
        val index: Int,
        val name: String,
        val offset: Int,
        val flags: Int,
        val part0Offset: Int,
        val part0ZSize: Int,
        val part0Size: Int,
        val part1Offset: Int,
        val part1ZSize: Int,
        val part1Size: Int,
        val ptxNumber: Int,
        val ptxBeforeNumber: Int,
    )

    private const val NAME_LEN = 128

    /** 解析 RSB 头。文件太小/装不下头时返回 null。 */
    fun parseHeader(data: ByteArray): RsbHeader? {
        if (data.size < 96) return null
        val stride = readU32LE(data, 48)
        return RsbHeader(
            version = readU32LE(data, 4),
            rsgpCount = readU32LE(data, 40),
            rsgpInfoOffset = readU32LE(data, 44),
            rsgpInfoStride = if (stride in 128..4096) stride else 0xCC,
            ptxCount = readU32LE(data, 84),
            ptxInfoOffset = readU32LE(data, 88),
            ptxInfoStride = readU32LE(data, 92),
        )
    }

    /**
     * 解析 rsgpInfo 表。
     * 条目不足 [RsbHeader.rsgpInfoStride] 字节的尾部条目会被丢弃（无异常）。
     */
    fun parseRsgpInfos(data: ByteArray, header: RsbHeader): List<RsgpInfo> {
        val stride = header.rsgpInfoStride
        if (stride < 204 || header.rsgpCount <= 0) return emptyList()
        val out = ArrayList<RsgpInfo>(minOf(header.rsgpCount, 4096))
        for (i in 0 until header.rsgpCount) {
            val base = header.rsgpInfoOffset.toLong() + i.toLong() * stride
            if (base < 0 || base + stride > data.size) break
            val p = base.toInt()
            out.add(
                RsgpInfo(
                    index = i,
                    name = readFixedCString(data, p, NAME_LEN),
                    offset = readU32LE(data, p + 128),
                    flags = readU32LE(data, p + 140),
                    part0Offset = readU32LE(data, p + 148),
                    part0ZSize = readU32LE(data, p + 152),
                    part0Size = readU32LE(data, p + 156),
                    part1Offset = readU32LE(data, p + 164),
                    part1ZSize = readU32LE(data, p + 168),
                    part1Size = readU32LE(data, p + 172),
                    ptxNumber = readU32LE(data, p + 196),
                    ptxBeforeNumber = readU32LE(data, p + 200),
                )
            )
        }
        return out
    }

    /**
     * 解析 PTX_INFO 表。条目按 [RsbHeader.ptxInfoStride] 步进（常见 0x10 / 0x14 / 0x18）。
     * 每种步长都比 20 字节短或长，[PtxDecoder.parsePtxInfo] 只取前 20 字节。
     */
    fun parsePtxInfos(data: ByteArray, header: RsbHeader): List<PtxDecoder.PtxInfo> {
        val stride = header.ptxInfoStride
        if (stride < 16 || header.ptxCount <= 0) return emptyList()
        val out = ArrayList<PtxDecoder.PtxInfo>(minOf(header.ptxCount, 65536))
        for (i in 0 until header.ptxCount) {
            val base = header.ptxInfoOffset.toLong() + i.toLong() * stride
            if (base < 0 || base + stride > data.size) break
            out.add(PtxDecoder.parsePtxInfo(data.copyOfRange(base.toInt(), base.toInt() + stride)))
        }
        return out
    }

    /**
     * image 条目的全局 PTX_INFO 下标：
     *   `rsgpInfo[子组].ptxBeforeNumber + entry.part1Index`
     *
     * 返回 -1 表示这条 entry 没有可用的纹理索引（非图像条目、或容器没写 part1Index）。
     *
     * 用 `internal` 而非 `public`：[SmfPacker.RsgpFileEntry] 本身是 internal，
     * 公开函数暴露 internal 类型在 Kotlin 里是编译错误。
     */
    internal fun globalPtxIndex(sg: RsgpInfo, entry: SmfPacker.RsgpFileEntry): Int {
        if (!entry.isImage || entry.part1Index < 0) return -1
        return sg.ptxBeforeNumber + entry.part1Index
    }

    /**
     * 解出某条目对应的 [PtxDecoder.PtxInfo]。
     * @return null = 无索引 / 下标越界（容器表里没有这一行），调用方应回退为"不解码"。
     */
    internal fun ptxInfoFor(
        sg: RsgpInfo,
        entry: SmfPacker.RsgpFileEntry,
        ptxInfos: List<PtxDecoder.PtxInfo>
    ): PtxDecoder.PtxInfo? {
        val gi = globalPtxIndex(sg, entry)
        if (gi < 0 || gi >= ptxInfos.size) return null
        return ptxInfos[gi]
    }

    // ---- 字节读取（越界返回 0，绝不抛异常） ----

    private fun readU32LE(data: ByteArray, offset: Int): Int {
        if (offset < 0 || offset + 4 > data.size) return 0
        return (data[offset].toInt() and 0xFF) or
            ((data[offset + 1].toInt() and 0xFF) shl 8) or
            ((data[offset + 2].toInt() and 0xFF) shl 16) or
            ((data[offset + 3].toInt() and 0xFF) shl 24)
    }

    private fun readFixedCString(data: ByteArray, offset: Int, maxLen: Int): String {
        val len = minOf(maxLen, data.size - offset)
        if (len <= 0) return ""
        val s = String(data, offset, len, Charsets.UTF_8)
        val nul = s.indexOf(0.toChar())
        return if (nul >= 0) s.substring(0, nul) else s
    }
}
