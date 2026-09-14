package com.example.z_editor.datapack.smf

/**
 * RSB 容器里跟**纹理**有关的那几张表（纯 Kotlin，无 Android 依赖）。
 *
 * 容器的整体解包（子组遍历、data/image 段解压、文件落盘）在 [SmfUnpacker] 里；
 * 这里只负责"**某个 image 条目对应哪一行 PTX_INFO**"这件事——这正是 PTX→PNG
 * 能不能解对的关键，也是之前那版实现栽跟头的地方（当时误把文件列表中 image
 * 条目尾部的 20 字节 RsgpPart1ExtraInfo 当成了 PtxInfo）。
 *
 * 读取走 [ByteSource]：既可以是内存里的 ByteArray（单测、小文件），也可以是
 * 几百 MB 的容器文件。带 ByteArray 的重载只是薄封装，语义完全一致。
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
    fun parseHeader(data: ByteArray): RsbHeader? = parseHeader(data.asByteSource())

    /** 解析 RSB 头。文件太小/装不下头时返回 null。 */
    internal fun parseHeader(src: ByteSource): RsbHeader? {
        if (src.size < 96) return null
        val stride = src.u32LE(48)
        return RsbHeader(
            version = src.u32LE(4),
            rsgpCount = src.u32LE(40),
            rsgpInfoOffset = src.u32LE(44),
            rsgpInfoStride = if (stride in 128..4096) stride else 0xCC,
            ptxCount = src.u32LE(84),
            ptxInfoOffset = src.u32LE(88),
            ptxInfoStride = src.u32LE(92),
        )
    }

    /**
     * 解析 rsgpInfo 表。
     * 条目不足 [RsbHeader.rsgpInfoStride] 字节的尾部条目会被丢弃（无异常）。
     */
    fun parseRsgpInfos(data: ByteArray, header: RsbHeader): List<RsgpInfo> =
        parseRsgpInfos(data.asByteSource(), header)

    /** 同上，直接读字节源。 */
    internal fun parseRsgpInfos(src: ByteSource, header: RsbHeader): List<RsgpInfo> {
        val stride = header.rsgpInfoStride
        if (stride < 204 || header.rsgpCount <= 0) return emptyList()
        val out = ArrayList<RsgpInfo>(minOf(header.rsgpCount, 4096))
        for (i in 0 until header.rsgpCount) {
            val base = header.rsgpInfoOffset.toLong() + i.toLong() * stride
            if (base < 0 || base + stride > src.size) break
            out.add(
                RsgpInfo(
                    index = i,
                    name = src.fixedCString(base, NAME_LEN),
                    offset = src.u32LE(base + 128),
                    flags = src.u32LE(base + 140),
                    part0Offset = src.u32LE(base + 148),
                    part0ZSize = src.u32LE(base + 152),
                    part0Size = src.u32LE(base + 156),
                    part1Offset = src.u32LE(base + 164),
                    part1ZSize = src.u32LE(base + 168),
                    part1Size = src.u32LE(base + 172),
                    ptxNumber = src.u32LE(base + 196),
                    ptxBeforeNumber = src.u32LE(base + 200),
                )
            )
        }
        return out
    }

    /**
     * 解析 PTX_INFO 表。条目按 [RsbHeader.ptxInfoStride] 步进（常见 0x10 / 0x14 / 0x18）。
     * 每种步长都比 20 字节短或长，[PtxDecoder.parsePtxInfo] 只取前 20 字节。
     */
    fun parsePtxInfos(data: ByteArray, header: RsbHeader): List<PtxDecoder.PtxInfo> =
        parsePtxInfos(data.asByteSource(), header)

    /** 同上，直接读字节源。 */
    internal fun parsePtxInfos(src: ByteSource, header: RsbHeader): List<PtxDecoder.PtxInfo> {
        val stride = header.ptxInfoStride
        if (stride < 16 || header.ptxCount <= 0) return emptyList()
        val out = ArrayList<PtxDecoder.PtxInfo>(minOf(header.ptxCount, 65536))
        for (i in 0 until header.ptxCount) {
            val base = header.ptxInfoOffset.toLong() + i.toLong() * stride
            if (base < 0 || base + stride > src.size) break
            val row = src.readFully(base, stride) ?: break
            out.add(PtxDecoder.parsePtxInfo(row))
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
}
