package com.example.z_editor.datapack.smf

import kotlin.math.floor

/**
 * Pure-Kotlin port of the ASTC LDR/HDR block decoder in `astc_t2d.cpp` (itself a port of the
 * `astc_dec` / `astcenc` reference decoder).
 *
 */
object AstcDecoder {

    /** Opaque black, used for block modes we refuse to decode. */
    private const val BLACK = -0x1000000 // 0xFF000000

    /** Bits 0..7 of [n], little-endian, from the payload; out-of-range bytes read as zero. */
    private fun byteAt(data: ByteArray, i: Int): Int =
        if (i < 0 || i >= data.size) 0 else data[i].toInt() and 0xFF

    /** Unaligned little-endian 32-bit load, matching `*(int *)(buf + p)` with zero fill. */
    private fun read32(data: ByteArray, p: Int): Int {
        if (p >= 0 && p + 4 <= data.size) {
            return (data[p].toInt() and 0xFF) or
                ((data[p + 1].toInt() and 0xFF) shl 8) or
                ((data[p + 2].toInt() and 0xFF) shl 16) or
                ((data[p + 3].toInt() and 0xFF) shl 24)
        }
        return byteAt(data, p) or (byteAt(data, p + 1) shl 8) or
            (byteAt(data, p + 2) shl 16) or (byteAt(data, p + 3) shl 24)
    }

    /** Unaligned little-endian 64-bit load, matching `*(uint64_t *)(buf + p)` with zero fill. */
    private fun read64(data: ByteArray, p: Int): Long {
        if (p >= 0 && p + 8 <= data.size) {
            return (data[p].toLong() and 0xFF) or
                ((data[p + 1].toLong() and 0xFF) shl 8) or
                ((data[p + 2].toLong() and 0xFF) shl 16) or
                ((data[p + 3].toLong() and 0xFF) shl 24) or
                ((data[p + 4].toLong() and 0xFF) shl 32) or
                ((data[p + 5].toLong() and 0xFF) shl 40) or
                ((data[p + 6].toLong() and 0xFF) shl 48) or
                ((data[p + 7].toLong() and 0xFF) shl 56)
        }
        var v = 0L
        var i = 0
        while (i < 8) {
            v = v or (byteAt(data, p + i).toLong() shl (i * 8))
            i++
        }
        return v
    }

    /**
     * `getbits`: read [len] bits at absolute bit position [bit] of the block at [off].
     *
     * [bit] is normally inside the block, but the reference also hands it negative positions: a
     * weight grid whose payload overflows the 128-bit budget makes `decode_intseq` walk backwards
     * past the start of the block, and `buf + bit / 8` then reads into the tail of the previous
     * block. Such blocks are invalid to begin with - a real encoder never emits one - but the
     * reference still produces output for them, so the behaviour is reproduced here: the bytes in
     * front of the block are still inside the payload, and reads before the start of the image
     * yield zero (the reference would read whatever the allocator left there).
     *
     * Two details matter for matching. C truncates `bit / 8` toward zero, so -1..-7 stay inside
     * the block itself at `buf + 0`. And the reference compiler evaluates the shift count
     * `bit % 8` as the *non-negative* remainder, i.e. `bit & 7`. Both are matched exactly below.
     */
    private fun getBits(data: ByteArray, off: Int, bit: Int, len: Int): Int {
        if (len <= 0) return 0
        val mask = if (len >= 32) -1 else (1 shl len) - 1
        return (read32(data, off + bit / 8) shr (bit and 7)) and mask
    }

    /** `getbits64`: same, but with a 64-bit window and a `bit <= 0` / `bit >= 64` split. */
    private fun getBits64(data: ByteArray, off: Int, bit: Int, len: Int): Long {
        if (len < 1) return 0
        val mask = if (len >= 64) -1L else (1L shl len) - 1L
        if (bit >= 64) {
            return (read64(data, off + 8) ushr (bit - 64)) and mask
        } else if (bit <= 0) {
            return (read64(data, off) shl -bit) and mask
        } else if (bit + len <= 64) {
            return (read64(data, off) ushr bit) and mask
        }
        return ((read64(data, off) ushr bit) or (read64(data, off + 8) shl (64 - bit))) and mask
    }

    private fun bitReverseU8(c: Int, bits: Int): Int = BIT_REVERSE[c and 0xFF] shr (8 - bits)

    private fun bitReverseU64(d: Long, bits: Int): Long {
        val ret = (BIT_REVERSE[(d and 0xffL).toInt()].toLong() shl 56) or
            (BIT_REVERSE[((d ushr 8) and 0xffL).toInt()].toLong() shl 48) or
            (BIT_REVERSE[((d ushr 16) and 0xffL).toInt()].toLong() shl 40) or
            (BIT_REVERSE[((d ushr 24) and 0xffL).toInt()].toLong() shl 32) or
            (BIT_REVERSE[((d ushr 32) and 0xffL).toInt()].toLong() shl 24) or
            (BIT_REVERSE[((d ushr 40) and 0xffL).toInt()].toLong() shl 16) or
            (BIT_REVERSE[((d ushr 48) and 0xffL).toInt()].toLong() shl 8) or
            BIT_REVERSE[((d ushr 56) and 0xffL).toInt()].toLong()
        return ret ushr (64 - bits)
    }

    private fun clamp8(n: Int): Int = if (n < 0) 0 else if (n > 255) 255 else n

    private fun clampHdr(n: Int): Int = if (n < 0) 0 else if (n > 0xfff) 0xfff else n

    /** `bit_transfer_signed`: splits a delta value into a signed magnitude plus the low bits. */
    private fun bitTransferSigned(v: IntArray, ia: Int, ib: Int) {
        v[ib] = (v[ib] shr 1) or (v[ia] and 0x80)
        v[ia] = (v[ia] shr 1) and 0x3f
        if (v[ia] and 0x20 != 0) v[ia] -= 0x40
    }

    // ------------------------------------------------------------------ endpoints

    private fun setEndpoint(
        ep: IntArray, b: Int,
        r1: Int, g1: Int, b1: Int, a1: Int, r2: Int, g2: Int, b2: Int, a2: Int
    ) {
        ep[b] = r1; ep[b + 1] = g1; ep[b + 2] = b1; ep[b + 3] = a1
        ep[b + 4] = r2; ep[b + 5] = g2; ep[b + 6] = b2; ep[b + 7] = a2
    }

    private fun setEndpointClamp(
        ep: IntArray, b: Int,
        r1: Int, g1: Int, b1: Int, a1: Int, r2: Int, g2: Int, b2: Int, a2: Int
    ) {
        ep[b] = clamp8(r1); ep[b + 1] = clamp8(g1); ep[b + 2] = clamp8(b1); ep[b + 3] = clamp8(a1)
        ep[b + 4] = clamp8(r2); ep[b + 5] = clamp8(g2); ep[b + 6] = clamp8(b2); ep[b + 7] = clamp8(a2)
    }

    private fun setEndpointBlue(
        ep: IntArray, b: Int,
        r1: Int, g1: Int, b1: Int, a1: Int, r2: Int, g2: Int, b2: Int, a2: Int
    ) {
        ep[b] = (r1 + b1) shr 1; ep[b + 1] = (g1 + b1) shr 1; ep[b + 2] = b1; ep[b + 3] = a1
        ep[b + 4] = (r2 + b2) shr 1; ep[b + 5] = (g2 + b2) shr 1; ep[b + 6] = b2; ep[b + 7] = a2
    }

    private fun setEndpointBlueClamp(
        ep: IntArray, b: Int,
        r1: Int, g1: Int, b1: Int, a1: Int, r2: Int, g2: Int, b2: Int, a2: Int
    ) {
        ep[b] = clamp8((r1 + b1) shr 1); ep[b + 1] = clamp8((g1 + b1) shr 1)
        ep[b + 2] = clamp8(b1); ep[b + 3] = clamp8(a1)
        ep[b + 4] = clamp8((r2 + b2) shr 1); ep[b + 5] = clamp8((g2 + b2) shr 1)
        ep[b + 6] = clamp8(b2); ep[b + 7] = clamp8(a2)
    }

    private fun setEndpointHdr(
        ep: IntArray, b: Int,
        r1: Int, g1: Int, b1: Int, a1: Int, r2: Int, g2: Int, b2: Int, a2: Int
    ) {
        ep[b] = r1; ep[b + 1] = g1; ep[b + 2] = b1; ep[b + 3] = a1
        ep[b + 4] = r2; ep[b + 5] = g2; ep[b + 6] = b2; ep[b + 7] = a2
    }

    private fun setEndpointHdrClamp(
        ep: IntArray, b: Int,
        r1: Int, g1: Int, b1: Int, a1: Int, r2: Int, g2: Int, b2: Int, a2: Int
    ) {
        ep[b] = clampHdr(r1); ep[b + 1] = clampHdr(g1); ep[b + 2] = clampHdr(b1); ep[b + 3] = clampHdr(a1)
        ep[b + 4] = clampHdr(r2); ep[b + 5] = clampHdr(g2); ep[b + 6] = clampHdr(b2); ep[b + 7] = clampHdr(a2)
    }

    // ------------------------------------------------------------------ color selection

    private fun selectColor(v0: Int, v1: Int, weight: Int): Int {
        val t = (((((v0 shl 8) or v0) * (64 - weight) + ((v1 shl 8) or v1) * weight + 32) shr 6) * 255 + 32768)
        return t / 65536
    }

    /**
     * IEEE half -> float, the classic branchless FK20 conversion. `twoW` is compared as an
     * *unsigned* 32-bit value in C, so bias the signed comparison instead of using `<` directly.
     */
    private fun fp16ToF32(h: Int): Float {
        val w = h shl 16
        val sign = w and Int.MIN_VALUE
        val twoW = w + w
        val expOffset = 0xE0 shl 23
        val normalized = Float.fromBits((twoW ushr 4) + expOffset) * FP16_EXP_SCALE
        val denormalized = Float.fromBits((twoW ushr 17) or (126 shl 23)) - 0.5f
        val useDenormal = (twoW xor Int.MIN_VALUE) < DENORM_CUTOFF_BIASED
        return Float.fromBits(sign or (if (useDenormal) denormalized.toRawBits() else normalized.toRawBits()))
    }

    /** `roundf(x)` for the non-negative arguments this decoder produces. */
    private fun roundHalfUp(x: Float): Float = floor(x + 0.5f)

    private fun f32ToU8(f: Float): Int {
        val c = roundHalfUp(f * 255f)
        return if (c < 0f) 0 else if (c > 255f) 255 else c.toInt()
    }

    private fun f16PtrToU8(data: ByteArray, p: Int): Int =
        f32ToU8(fp16ToF32(byteAt(data, p) or (byteAt(data, p + 1) shl 8)))

    private fun selectColorHdr(v0: Int, v1: Int, weight: Int): Int {
        val c = (((v0 shl 4) * (64 - weight) + (v1 shl 4) * weight + 32) shr 6) and 0xFFFF
        var m = c and 0x7ff
        if (m < 512) {
            m *= 3
        } else if (m < 1536) {
            m = 4 * m - 512
        } else {
            m = 5 * m - 2048
        }
        val f = fp16ToF32(((c shr 1) and 0x7c00) or (m shr 3))
        return if (f.isFinite()) clamp8(roundHalfUp(f * 255f).toInt()) else 255
    }

    private fun select(isHdr: Boolean, v0: Int, v1: Int, weight: Int): Int =
        if (isHdr) selectColorHdr(v0, v1, weight) else selectColor(v0, v1, weight)

    // ------------------------------------------------------------------ int sequences

    /**
     * `decode_intseq`: unpacks [count] values from the block into `outBits` / `outNonbits`.
     * `a` selects the packing (0 = plain bits, 3 = trits, 5 = quints).
     */
    private fun decodeIntseq(
        data: ByteArray, off: Int, offset: Int, a: Int, b: Int, count: Int, reverse: Boolean,
        outBits: IntArray, outNonbits: IntArray
    ) {
        if (count <= 0) return
        var n = 0

        if (a == 3) {
            val mask = (1 shl b) - 1
            val blockCount = (count + 4) / 5
            val lastBlockCount = (count + 4) % 5 + 1
            val blockSize = 8 + 5 * b
            val lastBlockSize = (blockSize * lastBlockCount + 4) / 5

            var p = offset
            for (i in 0 until blockCount) {
                val nowSize = if (i < blockCount - 1) blockSize else lastBlockSize
                val d = if (reverse) {
                    bitReverseU64(getBits64(data, off, p - nowSize, nowSize), nowSize)
                } else {
                    getBits64(data, off, p, nowSize)
                }
                val x = ((d ushr b) and 3L) or ((d ushr (b * 2)) and 0xcL) or
                    ((d ushr (b * 3)) and 0x10L) or ((d ushr (b * 4)) and 0x60L) or
                    ((d ushr (b * 5)) and 0x80L)
                val xi = x.toInt()
                var j = 0
                while (j < 5 && n < count) {
                    outBits[n] = (d ushr (MT[j] + b * j) and mask.toLong()).toInt()
                    outNonbits[n] = TRITS_2D[j * 256 + xi]
                    n++
                    j++
                }
                if (reverse) p -= blockSize else p += blockSize
            }
        } else if (a == 5) {
            val mask = (1 shl b) - 1
            val blockCount = (count + 2) / 3
            val lastBlockCount = (count + 2) % 3 + 1
            val blockSize = 7 + 3 * b
            val lastBlockSize = (blockSize * lastBlockCount + 2) / 3

            var p = offset
            for (i in 0 until blockCount) {
                val nowSize = if (i < blockCount - 1) blockSize else lastBlockSize
                val d = if (reverse) {
                    bitReverseU64(getBits64(data, off, p - nowSize, nowSize), nowSize)
                } else {
                    getBits64(data, off, p, nowSize)
                }
                val x = ((d ushr b) and 7L) or ((d ushr (b * 2)) and 0x18L) or
                    ((d ushr (b * 3)) and 0x60L)
                val xi = x.toInt()
                var j = 0
                while (j < 3 && n < count) {
                    outBits[n] = (d ushr (MQ[j] + b * j) and mask.toLong()).toInt()
                    outNonbits[n] = QUINTS_2D[j * 128 + xi]
                    n++
                    j++
                }
                if (reverse) p -= blockSize else p += blockSize
            }
        } else {
            if (reverse) {
                var p = offset - b
                while (n < count) {
                    outBits[n] = bitReverseU8(getBits(data, off, p, b), b)
                    outNonbits[n] = 0
                    n++
                    p -= b
                }
            } else {
                var p = offset
                while (n < count) {
                    outBits[n] = getBits(data, off, p, b)
                    outNonbits[n] = 0
                    n++
                    p += b
                }
            }
        }
    }

    // ------------------------------------------------------------------ block header

    private fun decodeBlockParams(data: ByteArray, off: Int, s: Scratch) {
        val b0 = byteAt(data, off)
        val b1 = byteAt(data, off + 1)
        val b2 = byteAt(data, off + 2)
        val b3 = byteAt(data, off + 3)
        val u16 = b0 or (b1 shl 8) // u8ptr_to_u16(buf)

        s.dualPlane = if (b1 and 4 != 0) 1 else 0
        s.weightRange = (b0 shr 4 and 1) or (b1 shl 2 and 8)

        if (b0 and 3 != 0) {
            s.weightRange = s.weightRange or (b0 shl 1 and 6)
            when (b0 and 0xc) {
                0 -> {
                    s.w = (u16 shr 7 and 3) + 4
                    s.h = (b0 shr 5 and 3) + 2
                }
                4 -> {
                    s.w = (u16 shr 7 and 3) + 8
                    s.h = (b0 shr 5 and 3) + 2
                }
                8 -> {
                    s.w = (b0 shr 5 and 3) + 2
                    s.h = (u16 shr 7 and 3) + 8
                }
                else -> {
                    if (b1 and 1 != 0) {
                        s.w = (b0 shr 7 and 1) + 2
                        s.h = (b0 shr 5 and 3) + 2
                    } else {
                        s.w = (b0 shr 5 and 3) + 2
                        s.h = (b0 shr 7 and 1) + 6
                    }
                }
            }
        } else {
            s.weightRange = s.weightRange or (b0 shr 1 and 6)
            when (u16 and 0x180) {
                0 -> {
                    s.w = 12
                    s.h = (b0 shr 5 and 3) + 2
                }
                0x80 -> {
                    s.w = (b0 shr 5 and 3) + 2
                    s.h = 12
                }
                0x100 -> {
                    s.w = (b0 shr 5 and 3) + 6
                    s.h = (b1 shr 1 and 3) + 6
                    s.dualPlane = 0
                    s.weightRange = s.weightRange and 7
                }
                else -> {
                    s.w = if (b0 and 0x20 != 0) 10 else 6
                    s.h = if (b0 and 0x20 != 0) 6 else 10
                }
            }
        }

        s.partNum = (b1 shr 3 and 3) + 1

        s.weightNum = s.w * s.h
        if (s.dualPlane != 0) s.weightNum *= 2

        val pA = WEIGHT_PREC_TABLE_A[s.weightRange]
        val pB = WEIGHT_PREC_TABLE_B[s.weightRange]
        val weightBits = when (pA) {
            3 -> s.weightNum * pB + (s.weightNum * 8 + 4) / 5
            5 -> s.weightNum * pB + (s.weightNum * 7 + 2) / 3
            else -> s.weightNum * pB
        }

        var cemBase = 0
        var configBits: Int
        if (s.partNum == 1) {
            s.cem[0] = (b1 or (b2 shl 8)) shr 5 and 0xf
            configBits = 17
        } else {
            cemBase = (b2 or (b3 shl 8)) shr 7 and 3
            if (cemBase == 0) {
                val cem = b3 shr 1 and 0xf
                for (i in 0 until s.partNum) s.cem[i] = cem
                configBits = 29
            } else {
                for (i in 0 until s.partNum) s.cem[i] = ((b3 shr (i + 1) and 1) + cemBase - 1) shl 2
                when (s.partNum) {
                    2 -> {
                        s.cem[0] = s.cem[0] or (b3 shr 3 and 3)
                        s.cem[1] = s.cem[1] or getBits(data, off, 126 - weightBits, 2)
                    }
                    3 -> {
                        s.cem[0] = s.cem[0] or (b3 shr 4 and 1)
                        s.cem[0] = s.cem[0] or (getBits(data, off, 122 - weightBits, 2) and 2)
                        s.cem[1] = s.cem[1] or getBits(data, off, 124 - weightBits, 2)
                        s.cem[2] = s.cem[2] or getBits(data, off, 126 - weightBits, 2)
                    }
                    else -> {
                        for (i in 0 until 4) {
                            s.cem[i] = s.cem[i] or getBits(data, off, 120 + i * 2 - weightBits, 2)
                        }
                    }
                }
                configBits = 25 + s.partNum * 3
            }
        }

        if (s.dualPlane != 0) {
            configBits += 2
            val bit = if (cemBase != 0) 130 - weightBits - s.partNum * 3 else 126 - weightBits
            s.planeSelector = getBits(data, off, bit, 2)
        }

        val remainBits = 128 - configBits - weightBits

        s.endpointValueNum = 0
        for (i in 0 until s.partNum) {
            s.endpointValueNum += (s.cem[i] shr 1 and 6) + 2
        }

        // Pick the first CEM range whose endpoint payload still fits in the block.
        s.cemRangeFound = false
        for (i in CEM_TABLE_A.indices) {
            val endpointBits = when (CEM_TABLE_A[i]) {
                3 -> s.endpointValueNum * CEM_TABLE_B[i] + (s.endpointValueNum * 8 + 4) / 5
                5 -> s.endpointValueNum * CEM_TABLE_B[i] + (s.endpointValueNum * 7 + 2) / 3
                else -> s.endpointValueNum * CEM_TABLE_B[i]
            }
            if (endpointBits <= remainBits) {
                s.cemRange = i
                s.cemRangeFound = true
                break
            }
        }
        if (s.cemRangeFound) {
            s.stickyCemRange = s.cemRange
        } else {
            // No CEM range fits the 128-bit budget. The reference reads `block_data->cem_range`
            // uninitialised here, and because `BlockData` is a stack local at a fixed address in
            // the per-block loop, the slot holds the value written by the previous block that did
            // find a range. Reproduce that: reuse the last chosen range (0 before the first block
            // of the image). The index is clamped so a stray value can never index the tables
            // out of bounds.
            s.cemRange = s.stickyCemRange
        }
    }

    // ------------------------------------------------------------------ endpoints

    private fun decodeEndpointsHdr7(ep: IntArray, b: Int, v: IntArray, vi: Int) {
        val modeval = ((v[vi + 2] shr 4 and 0x8) or (v[vi + 1] shr 5 and 0x4) or (v[vi] shr 6))
        val majorComponent: Int
        val mode: Int
        if (modeval and 0xc != 0xc) {
            majorComponent = modeval shr 2
            mode = modeval and 3
        } else if (modeval != 0xf) {
            majorComponent = modeval and 3
            mode = 4
        } else {
            majorComponent = 0
            mode = 5
        }
        var c0 = v[vi] and 0x3f
        var c1 = v[vi + 1] and 0x1f
        var c2 = v[vi + 2] and 0x1f
        var c3 = v[vi + 3] and 0x1f
        val w0 = v[vi]
        val w1 = v[vi + 1]
        val w2 = v[vi + 2]
        val w3 = v[vi + 3]

        when (mode) {
            0 -> {
                c3 = c3 or (w3 and 0x60)
                c0 = c0 or (w3 shr 1 and 0x40)
                c0 = c0 or (w2 shl 1 and 0x80)
                c0 = c0 or (w1 shl 3 and 0x300)
                c0 = c0 or (w2 shl 5 and 0x400)
                c0 = c0 shl 1; c1 = c1 shl 1; c2 = c2 shl 1; c3 = c3 shl 1
            }
            1 -> {
                c1 = c1 or (w1 and 0x20)
                c2 = c2 or (w2 and 0x20)
                c0 = c0 or (w3 shr 1 and 0x40)
                c0 = c0 or (w2 shl 1 and 0x80)
                c0 = c0 or (w1 shl 2 and 0x100)
                c0 = c0 or (w3 shl 4 and 0x600)
                c0 = c0 shl 1; c1 = c1 shl 1; c2 = c2 shl 1; c3 = c3 shl 1
            }
            2 -> {
                c3 = c3 or (w3 and 0xe0)
                c0 = c0 or (w2 shl 1 and 0xc0)
                c0 = c0 or (w1 shl 3 and 0x300)
                c0 = c0 shl 2; c1 = c1 shl 2; c2 = c2 shl 2; c3 = c3 shl 2
            }
            3 -> {
                c1 = c1 or (w1 and 0x20)
                c2 = c2 or (w2 and 0x20)
                c3 = c3 or (w3 and 0x60)
                c0 = c0 or (w3 shr 1 and 0x40)
                c0 = c0 or (w2 shl 1 and 0x80)
                c0 = c0 or (w1 shl 2 and 0x100)
                c0 = c0 shl 3; c1 = c1 shl 3; c2 = c2 shl 3; c3 = c3 shl 3
            }
            4 -> {
                c1 = c1 or (w1 and 0x60)
                c2 = c2 or (w2 and 0x60)
                c3 = c3 or (w3 and 0x20)
                c0 = c0 or (w3 shr 1 and 0x40)
                c0 = c0 or (w3 shl 1 and 0x80)
                c0 = c0 shl 4; c1 = c1 shl 4; c2 = c2 shl 4; c3 = c3 shl 4
            }
            else -> {
                c1 = c1 or (w1 and 0x60)
                c2 = c2 or (w2 and 0x60)
                c3 = c3 or (w3 and 0x60)
                c0 = c0 or (w3 shr 1 and 0x40)
                c0 = c0 shl 5; c1 = c1 shl 5; c2 = c2 shl 5; c3 = c3 shl 5
            }
        }
        if (mode != 5) {
            c1 = c0 - c1
            c2 = c0 - c2
        }
        if (majorComponent == 1) {
            setEndpointHdrClamp(ep, b, c1 - c3, c0 - c3, c2 - c3, 0x780, c1, c0, c2, 0x780)
        } else if (majorComponent == 2) {
            setEndpointHdrClamp(ep, b, c2 - c3, c1 - c3, c0 - c3, 0x780, c2, c1, c0, 0x780)
        } else {
            setEndpointHdrClamp(ep, b, c0 - c3, c1 - c3, c2 - c3, 0x780, c0, c1, c2, 0x780)
        }
    }

    private fun decodeEndpointsHdr11(ep: IntArray, b: Int, v: IntArray, vi: Int, alpha1: Int, alpha2: Int) {
        val majorComponent = (v[vi + 4] shr 7) or (v[vi + 5] shr 6 and 2)
        if (majorComponent == 3) {
            setEndpointHdr(
                ep, b, v[vi] shl 4, v[vi + 2] shl 4, v[vi + 4] shl 5 and 0xfe0, alpha1,
                v[vi + 1] shl 4, v[vi + 3] shl 4, v[vi + 5] shl 5 and 0xfe0, alpha2
            )
            return
        }
        val mode = (v[vi + 1] shr 7) or (v[vi + 2] shr 6 and 2) or (v[vi + 3] shr 5 and 4)
        var va = v[vi] or (v[vi + 1] shl 2 and 0x100)
        var vb0 = v[vi + 2] and 0x3f
        var vb1 = v[vi + 3] and 0x3f
        var vc = v[vi + 1] and 0x3f
        var vd0: Int
        var vd1: Int

        // The reference declares `int16_t vd0, vd1`, so `vd0 |= 0xff80` sign-extends through a
        // 16-bit truncation and lands on -128, not on +0xff80. Since the mask covers all bits
        // above the value, `x | 0xff80` is exactly `x - 0x80` in 16-bit land; same for 0xffc0 and
        // 0xffe0. Using `or` here would keep the value positive and corrupt every HDR-11 endpoint.
        when (mode) {
            0, 2 -> {
                vd0 = v[vi + 4] and 0x7f
                if (vd0 and 0x40 != 0) vd0 -= 0x80
                vd1 = v[vi + 5] and 0x7f
                if (vd1 and 0x40 != 0) vd1 -= 0x80
            }
            1, 3, 5, 7 -> {
                vd0 = v[vi + 4] and 0x3f
                if (vd0 and 0x20 != 0) vd0 -= 0x40
                vd1 = v[vi + 5] and 0x3f
                if (vd1 and 0x20 != 0) vd1 -= 0x40
            }
            else -> {
                vd0 = v[vi + 4] and 0x1f
                if (vd0 and 0x10 != 0) vd0 -= 0x20
                vd1 = v[vi + 5] and 0x1f
                if (vd1 and 0x10 != 0) vd1 -= 0x20
            }
        }

        when (mode) {
            0 -> {
                vb0 = vb0 or (v[vi + 2] and 0x40)
                vb1 = vb1 or (v[vi + 3] and 0x40)
            }
            1 -> {
                vb0 = vb0 or (v[vi + 2] and 0x40)
                vb1 = vb1 or (v[vi + 3] and 0x40)
                vb0 = vb0 or (v[vi + 4] shl 1 and 0x80)
                vb1 = vb1 or (v[vi + 5] shl 1 and 0x80)
            }
            2 -> {
                va = va or (v[vi + 2] shl 3 and 0x200)
                vc = vc or (v[vi + 3] and 0x40)
            }
            3 -> {
                va = va or (v[vi + 4] shl 3 and 0x200)
                vc = vc or (v[vi + 5] and 0x40)
                vb0 = vb0 or (v[vi + 2] and 0x40)
                vb1 = vb1 or (v[vi + 3] and 0x40)
            }
            4 -> {
                va = va or (v[vi + 4] shl 4 and 0x200)
                va = va or (v[vi + 5] shl 5 and 0x400)
                vb0 = vb0 or (v[vi + 2] and 0x40)
                vb1 = vb1 or (v[vi + 3] and 0x40)
                vb0 = vb0 or (v[vi + 4] shl 1 and 0x80)
                vb1 = vb1 or (v[vi + 5] shl 1 and 0x80)
            }
            5 -> {
                va = va or (v[vi + 2] shl 3 and 0x200)
                va = va or (v[vi + 3] shl 4 and 0x400)
                vc = vc or (v[vi + 5] and 0x40)
                vc = vc or (v[vi + 4] shl 1 and 0x80)
            }
            6 -> {
                va = va or (v[vi + 4] shl 4 and 0x200)
                va = va or (v[vi + 5] shl 5 and 0x400)
                va = va or (v[vi + 4] shl 5 and 0x800)
                vc = vc or (v[vi + 5] and 0x40)
                vb0 = vb0 or (v[vi + 2] and 0x40)
                vb1 = vb1 or (v[vi + 3] and 0x40)
            }
            else -> {
                va = va or (v[vi + 2] shl 3 and 0x200)
                va = va or (v[vi + 3] shl 4 and 0x400)
                va = va or (v[vi + 4] shl 5 and 0x800)
                vc = vc or (v[vi + 5] and 0x40)
            }
        }

        val shamt = (mode shr 1) xor 3
        va = va shl shamt
        vb0 = vb0 shl shamt
        vb1 = vb1 shl shamt
        vc = vc shl shamt
        val mult = 1 shl shamt
        vd0 *= mult
        vd1 *= mult

        if (majorComponent == 1) {
            setEndpointHdrClamp(
                ep, b, va - vb0 - vc - vd0, va - vc, va - vb1 - vc - vd1, alpha1,
                va - vb0, va, va - vb1, alpha2
            )
        } else if (majorComponent == 2) {
            setEndpointHdrClamp(
                ep, b, va - vb1 - vc - vd1, va - vb0 - vc - vd0, va - vc, alpha1,
                va - vb1, va - vb0, va, alpha2
            )
        } else {
            setEndpointHdrClamp(
                ep, b, va - vc, va - vb0 - vc - vd0, va - vb1 - vc - vd1, alpha1,
                va, va - vb0, va - vb1, alpha2
            )
        }
    }

    private fun decodeEndpoints(data: ByteArray, off: Int, s: Scratch) {
        val seq = s.seqBits
        val nonbits = s.seqNonbits
        val ev = s.ev
        val ca = CEM_TABLE_A[s.cemRange]
        val cb = CEM_TABLE_B[s.cemRange]
        val evn = s.endpointValueNum

        decodeIntseq(data, off, if (s.partNum == 1) 17 else 29, ca, cb, evn, false, seq, nonbits)

        when (ca) {
            3 -> {
                val c = TRITS_SCALE[cb]
                for (i in 0 until evn) {
                    val a = (seq[i] and 1) * 0x1ff
                    val x = seq[i] shr 1
                    val b = when (cb) {
                        1 -> 0
                        2 -> 0b100010110 * x
                        3 -> (x shl 7) or (x shl 2) or x
                        4 -> (x shl 6) or x
                        5 -> (x shl 5) or (x shr 2)
                        else -> (x shl 4) or (x shr 4)
                    }
                    ev[i] = (a and 0x80) or (((nonbits[i] * c + b) xor a) shr 2)
                }
            }
            5 -> {
                val c = QUINTS_SCALE[cb]
                for (i in 0 until evn) {
                    val a = (seq[i] and 1) * 0x1ff
                    val x = seq[i] shr 1
                    val b = when (cb) {
                        1 -> 0
                        2 -> 0b100001100 * x
                        3 -> (x shl 7) or (x shl 1) or (x shr 1)
                        4 -> (x shl 6) or (x shr 1)
                        else -> (x shl 5) or (x shr 3)
                    }
                    ev[i] = (a and 0x80) or (((nonbits[i] * c + b) xor a) shr 2)
                }
            }
            else -> {
                when (cb) {
                    1 -> for (i in 0 until evn) ev[i] = seq[i] * 0xff
                    2 -> for (i in 0 until evn) ev[i] = seq[i] * 0x55
                    3 -> for (i in 0 until evn) ev[i] = (seq[i] shl 5) or (seq[i] shl 2) or (seq[i] shr 1)
                    4 -> for (i in 0 until evn) ev[i] = (seq[i] shl 4) or seq[i]
                    5 -> for (i in 0 until evn) ev[i] = (seq[i] shl 3) or (seq[i] shr 2)
                    6 -> for (i in 0 until evn) ev[i] = (seq[i] shl 2) or (seq[i] shr 4)
                    7 -> for (i in 0 until evn) ev[i] = (seq[i] shl 1) or (seq[i] shr 6)
                    else -> for (i in 0 until evn) ev[i] = seq[i]
                }
            }
        }

        // `v` walks the shared ev[] array, and some CEMs mutate it in place - keep that aliasing.
        var vi = 0
        for (cem in 0 until s.partNum) {
            val b = cem shl 3
            when (s.cem[cem]) {
                0 -> setEndpoint(
                    s.endpoints, b, ev[vi], ev[vi], ev[vi], 255, ev[vi + 1], ev[vi + 1], ev[vi + 1], 255
                )
                1 -> {
                    val l0 = (ev[vi] shr 2) or (ev[vi + 1] and 0xc0)
                    val l1 = clamp8(l0 + (ev[vi + 1] and 0x3f))
                    setEndpoint(s.endpoints, b, l0, l0, l0, 255, l1, l1, l1, 255)
                }
                2 -> {
                    val y0: Int
                    val y1: Int
                    if (ev[vi] <= ev[vi + 1]) {
                        y0 = ev[vi] shl 4
                        y1 = ev[vi + 1] shl 4
                    } else {
                        y0 = (ev[vi + 1] shl 4) + 8
                        y1 = (ev[vi] shl 4) - 8
                    }
                    setEndpointHdr(s.endpoints, b, y0, y0, y0, 0x780, y1, y1, y1, 0x780)
                }
                3 -> {
                    val y0: Int
                    val d: Int
                    if (ev[vi] and 0x80 != 0) {
                        y0 = ((ev[vi + 1] and 0xe0) shl 4) or ((ev[vi] and 0x7f) shl 2)
                        d = (ev[vi + 1] and 0x1f) shl 2
                    } else {
                        y0 = ((ev[vi + 1] and 0xf0) shl 4) or ((ev[vi] and 0x7f) shl 1)
                        d = (ev[vi + 1] and 0x0f) shl 1
                    }
                    val y1 = clampHdr(y0 + d)
                    setEndpointHdr(s.endpoints, b, y0, y0, y0, 0x780, y1, y1, y1, 0x780)
                }
                4 -> setEndpoint(
                    s.endpoints, b, ev[vi], ev[vi], ev[vi], ev[vi + 2],
                    ev[vi + 1], ev[vi + 1], ev[vi + 1], ev[vi + 3]
                )
                5 -> {
                    bitTransferSigned(ev, vi + 1, vi)
                    bitTransferSigned(ev, vi + 3, vi + 2)
                    ev[vi + 1] += ev[vi]
                    setEndpointClamp(
                        s.endpoints, b, ev[vi], ev[vi], ev[vi], ev[vi + 2],
                        ev[vi + 1], ev[vi + 1], ev[vi + 1], ev[vi + 2] + ev[vi + 3]
                    )
                }
                6 -> setEndpoint(
                    s.endpoints, b, ev[vi] * ev[vi + 3] shr 8, ev[vi + 1] * ev[vi + 3] shr 8,
                    ev[vi + 2] * ev[vi + 3] shr 8, 255, ev[vi], ev[vi + 1], ev[vi + 2], 255
                )
                7 -> decodeEndpointsHdr7(s.endpoints, b, ev, vi)
                8 -> {
                    if (ev[vi] + ev[vi + 2] + ev[vi + 4] <= ev[vi + 1] + ev[vi + 3] + ev[vi + 5]) {
                        setEndpoint(
                            s.endpoints, b, ev[vi], ev[vi + 2], ev[vi + 4], 255,
                            ev[vi + 1], ev[vi + 3], ev[vi + 5], 255
                        )
                    } else {
                        setEndpointBlue(
                            s.endpoints, b, ev[vi + 1], ev[vi + 3], ev[vi + 5], 255,
                            ev[vi], ev[vi + 2], ev[vi + 4], 255
                        )
                    }
                }
                9 -> {
                    bitTransferSigned(ev, vi + 1, vi)
                    bitTransferSigned(ev, vi + 3, vi + 2)
                    bitTransferSigned(ev, vi + 5, vi + 4)
                    if (ev[vi + 1] + ev[vi + 3] + ev[vi + 5] >= 0) {
                        setEndpointClamp(
                            s.endpoints, b, ev[vi], ev[vi + 2], ev[vi + 4], 255,
                            ev[vi] + ev[vi + 1], ev[vi + 2] + ev[vi + 3], ev[vi + 4] + ev[vi + 5], 255
                        )
                    } else {
                        setEndpointBlueClamp(
                            s.endpoints, b, ev[vi] + ev[vi + 1], ev[vi + 2] + ev[vi + 3],
                            ev[vi + 4] + ev[vi + 5], 255, ev[vi], ev[vi + 2], ev[vi + 4], 255
                        )
                    }
                }
                10 -> setEndpoint(
                    s.endpoints, b, ev[vi] * ev[vi + 3] shr 8, ev[vi + 1] * ev[vi + 3] shr 8,
                    ev[vi + 2] * ev[vi + 3] shr 8, ev[vi + 4], ev[vi], ev[vi + 1], ev[vi + 2], ev[vi + 5]
                )
                11 -> decodeEndpointsHdr11(s.endpoints, b, ev, vi, 0x780, 0x780)
                12 -> {
                    if (ev[vi] + ev[vi + 2] + ev[vi + 4] <= ev[vi + 1] + ev[vi + 3] + ev[vi + 5]) {
                        setEndpoint(
                            s.endpoints, b, ev[vi], ev[vi + 2], ev[vi + 4], ev[vi + 6],
                            ev[vi + 1], ev[vi + 3], ev[vi + 5], ev[vi + 7]
                        )
                    } else {
                        setEndpointBlue(
                            s.endpoints, b, ev[vi + 1], ev[vi + 3], ev[vi + 5], ev[vi + 7],
                            ev[vi], ev[vi + 2], ev[vi + 4], ev[vi + 6]
                        )
                    }
                }
                13 -> {
                    bitTransferSigned(ev, vi + 1, vi)
                    bitTransferSigned(ev, vi + 3, vi + 2)
                    bitTransferSigned(ev, vi + 5, vi + 4)
                    bitTransferSigned(ev, vi + 7, vi + 6)
                    if (ev[vi + 1] + ev[vi + 3] + ev[vi + 5] >= 0) {
                        setEndpointClamp(
                            s.endpoints, b, ev[vi], ev[vi + 2], ev[vi + 4], ev[vi + 6],
                            ev[vi] + ev[vi + 1], ev[vi + 2] + ev[vi + 3], ev[vi + 4] + ev[vi + 5],
                            ev[vi + 6] + ev[vi + 7]
                        )
                    } else {
                        setEndpointBlueClamp(
                            s.endpoints, b, ev[vi] + ev[vi + 1], ev[vi + 2] + ev[vi + 3],
                            ev[vi + 4] + ev[vi + 5], ev[vi + 6] + ev[vi + 7],
                            ev[vi], ev[vi + 2], ev[vi + 4], ev[vi + 6]
                        )
                    }
                }
                14 -> decodeEndpointsHdr11(s.endpoints, b, ev, vi, ev[vi + 6], ev[vi + 7])
                else -> {
                    val mode = ((ev[vi + 6] shr 7) and 1) or ((ev[vi + 7] shr 6) and 2)
                    ev[vi + 6] = ev[vi + 6] and 0x7f
                    ev[vi + 7] = ev[vi + 7] and 0x7f
                    if (mode == 3) {
                        decodeEndpointsHdr11(s.endpoints, b, ev, vi, ev[vi + 6] shl 5, ev[vi + 7] shl 5)
                    } else {
                        ev[vi + 6] = ev[vi + 6] or ((ev[vi + 7] shl (mode + 1)) and 0x780)
                        ev[vi + 7] = ((ev[vi + 7] and (0x3f shr mode)) xor (0x20 shr mode)) - (0x20 shr mode)
                        ev[vi + 6] = ev[vi + 6] shl (4 - mode)
                        ev[vi + 7] = ev[vi + 7] shl (4 - mode)
                        decodeEndpointsHdr11(
                            s.endpoints, b, ev, vi, ev[vi + 6], clampHdr(ev[vi + 6] + ev[vi + 7])
                        )
                    }
                }
            }
            vi += (s.cem[cem] / 4 + 1) * 2
        }
    }

    // ------------------------------------------------------------------ weights

    private fun decodeWeights(data: ByteArray, off: Int, s: Scratch) {
        val seq = s.seqBits
        val nonbits = s.seqNonbits
        val wv = s.wv
        val wn = s.weightNum
        val pA = WEIGHT_PREC_TABLE_A[s.weightRange]
        val pB = WEIGHT_PREC_TABLE_B[s.weightRange]

        // C zero-initialises the whole wv[] array; the interpolation below can read a few entries
        // past weight_num, so clear the full reachable window up front.
        val wvUsed = minOf(wv.size, (s.w * s.h + s.w + 2) * 2)
        wv.fill(0, 0, wvUsed)

        decodeIntseq(data, off, 128, pA, pB, wn, true, seq, nonbits)

        if (pA == 0) {
            when (pB) {
                1 -> for (i in 0 until wn) wv[i] = if (seq[i] != 0) 63 else 0
                2 -> for (i in 0 until wn) wv[i] = (seq[i] shl 4) or (seq[i] shl 2) or seq[i]
                3 -> for (i in 0 until wn) wv[i] = (seq[i] shl 3) or seq[i]
                4 -> for (i in 0 until wn) wv[i] = (seq[i] shl 2) or (seq[i] shr 2)
                else -> for (i in 0 until wn) wv[i] = (seq[i] shl 1) or (seq[i] shr 4)
            }
            for (i in 0 until wn) if (wv[i] > 32) wv[i]++
        } else if (pB == 0) {
            val shift = if (pA == 3) 32 else 16
            for (i in 0 until wn) wv[i] = nonbits[i] * shift
        } else {
            if (pA == 3) {
                when (pB) {
                    1 -> for (i in 0 until wn) wv[i] = nonbits[i] * 50
                    2 -> for (i in 0 until wn) {
                        wv[i] = nonbits[i] * 23
                        if (seq[i] and 2 != 0) wv[i] += 0b1000101
                    }
                    else -> for (i in 0 until wn) {
                        wv[i] = nonbits[i] * 11 + (((seq[i] shl 4) or (seq[i] shr 1)) and 0b1100011)
                    }
                }
            } else if (pA == 5) {
                when (pB) {
                    1 -> for (i in 0 until wn) wv[i] = nonbits[i] * 28
                    else -> for (i in 0 until wn) {
                        wv[i] = nonbits[i] * 13
                        if (seq[i] and 2 != 0) wv[i] += 0b1000010
                    }
                }
            }
            for (i in 0 until wn) {
                val a = (seq[i] and 1) * 0x7f
                wv[i] = (a and 0x20) or ((wv[i] xor a) shr 2)
                if (wv[i] > 32) wv[i]++
            }
        }

        val ds = (1024 + s.bw / 2) / (s.bw - 1)
        val dt = (1024 + s.bh / 2) / (s.bh - 1)
        val pn = if (s.dualPlane != 0) 2 else 1
        val weights = s.weights

        var i = 0
        for (t in 0 until s.bh) {
            for (sx in 0 until s.bw) {
                val gs = (ds * sx * (s.w - 1) + 32) shr 6
                val gt = (dt * t * (s.h - 1) + 32) shr 6
                val fs = gs and 0xf
                val ft = gt and 0xf
                val v = (gs shr 4) + (gt shr 4) * s.w
                val w11 = (fs * ft + 8) shr 4
                val w10 = ft - w11
                val w01 = fs - w11
                val w00 = 16 - fs - ft + w11

                var p = 0
                while (p < pn) {
                    val p00 = wv[v * pn + p]
                    val p01 = wv[(v + 1) * pn + p]
                    val p10 = wv[(v + s.w) * pn + p]
                    val p11 = wv[(v + s.w + 1) * pn + p]
                    weights[i * 2 + p] = (p00 * w00 + p01 * w01 + p10 * w10 + p11 * w11 + 8) shr 4
                    p++
                }
                i++
            }
        }
    }

    // ------------------------------------------------------------------ partitions

    private fun selectPartition(data: ByteArray, off: Int, s: Scratch) {
        val smallBlock = s.bw * s.bh < 31
        val seed = (read32(data, off) shr 13 and 0x3ff) or ((s.partNum - 1) shl 10)

        var rnum = seed
        rnum = rnum xor (rnum ushr 15)
        rnum -= rnum shl 17
        rnum += rnum shl 7
        rnum += rnum shl 4
        rnum = rnum xor (rnum ushr 5)
        rnum += rnum shl 16
        rnum = rnum xor (rnum ushr 7)
        rnum = rnum xor (rnum ushr 3)
        rnum = rnum xor (rnum shl 6)
        rnum = rnum xor (rnum ushr 17)

        val seeds = IntArray(8)
        for (i in 0 until 8) {
            seeds[i] = (rnum ushr (i * 4)) and 0xF
            seeds[i] *= seeds[i]
        }

        val sh0 = if (seed and 2 != 0) 4 else 5
        val sh1 = if (s.partNum == 3) 6 else 5
        if (seed and 1 != 0) {
            for (i in 0 until 8) seeds[i] = seeds[i] shr (if (i % 2 == 0) sh0 else sh1)
        } else {
            for (i in 0 until 8) seeds[i] = seeds[i] shr (if (i % 2 == 0) sh1 else sh0)
        }

        val part = s.partition
        if (smallBlock) {
            var i = 0
            for (t in 0 until s.bh) {
                for (sx in 0 until s.bw) {
                    val x = sx shl 1
                    val y = t shl 1
                    val a = (seeds[0] * x + seeds[1] * y + (rnum ushr 14)) and 0x3f
                    val b = (seeds[2] * x + seeds[3] * y + (rnum ushr 10)) and 0x3f
                    val c = if (s.partNum < 3) 0 else (seeds[4] * x + seeds[5] * y + (rnum ushr 6)) and 0x3f
                    val d = if (s.partNum < 4) 0 else (seeds[6] * x + seeds[7] * y + (rnum ushr 2)) and 0x3f
                    part[i] = if (a >= b && a >= c && a >= d) 0 else if (b >= c && b >= d) 1 else if (c >= d) 2 else 3
                    i++
                }
            }
        } else {
            var i = 0
            for (y in 0 until s.bh) {
                for (x in 0 until s.bw) {
                    val a = (seeds[0] * x + seeds[1] * y + (rnum ushr 14)) and 0x3f
                    val b = (seeds[2] * x + seeds[3] * y + (rnum ushr 10)) and 0x3f
                    val c = if (s.partNum < 3) 0 else (seeds[4] * x + seeds[5] * y + (rnum ushr 6)) and 0x3f
                    val d = if (s.partNum < 4) 0 else (seeds[6] * x + seeds[7] * y + (rnum ushr 2)) and 0x3f
                    part[i] = if (a >= b && a >= c && a >= d) 0 else if (b >= c && b >= d) 1 else if (c >= d) 2 else 3
                    i++
                }
            }
        }
    }

    // ------------------------------------------------------------------ color application

    private fun applicateColor(s: Scratch, out: IntArray) {
        val ep = s.endpoints
        val weights = s.weights
        val n = s.bw * s.bh

        if (s.dualPlane != 0) {
            val ps = s.ps
            ps[0] = 0
            ps[1] = 0
            ps[2] = 0
            ps[3] = 0
            ps[s.planeSelector and 3] = 1
            val cems = s.cem
            val part = s.partition
            for (i in 0 until n) {
                val p = if (s.partNum > 1) part[i] else 0
                val eb = p shl 3
                val cem = cems[p]
                val hdrC = FUNC_TABLE_C[cem]
                val hdrA = FUNC_TABLE_A[cem]
                val wi = i shl 1
                val r = select(hdrC, ep[eb], ep[eb + 4], weights[wi + ps[0]])
                val g = select(hdrC, ep[eb + 1], ep[eb + 5], weights[wi + ps[1]])
                val b = select(hdrC, ep[eb + 2], ep[eb + 6], weights[wi + ps[2]])
                val a = select(hdrA, ep[eb + 3], ep[eb + 7], weights[wi + ps[3]])
                out[i] = (a shl 24) or (r shl 16) or (g shl 8) or b
            }
        } else if (s.partNum > 1) {
            val cems = s.cem
            val part = s.partition
            for (i in 0 until n) {
                val p = part[i]
                val eb = p shl 3
                val cem = cems[p]
                val hdrC = FUNC_TABLE_C[cem]
                val hdrA = FUNC_TABLE_A[cem]
                val w = weights[i shl 1]
                val r = select(hdrC, ep[eb], ep[eb + 4], w)
                val g = select(hdrC, ep[eb + 1], ep[eb + 5], w)
                val b = select(hdrC, ep[eb + 2], ep[eb + 6], w)
                val a = select(hdrA, ep[eb + 3], ep[eb + 7], w)
                out[i] = (a shl 24) or (r shl 16) or (g shl 8) or b
            }
        } else {
            val cem = s.cem[0]
            val hdrC = FUNC_TABLE_C[cem]
            val hdrA = FUNC_TABLE_A[cem]
            for (i in 0 until n) {
                val w = weights[i shl 1]
                val r = select(hdrC, ep[0], ep[4], w)
                val g = select(hdrC, ep[1], ep[5], w)
                val b = select(hdrC, ep[2], ep[6], w)
                val a = select(hdrA, ep[3], ep[7], w)
                out[i] = (a shl 24) or (r shl 16) or (g shl 8) or b
            }
        }
    }

    // ------------------------------------------------------------------ entry points

    private fun decodeBlock(data: ByteArray, off: Int, bw: Int, bh: Int, s: Scratch, out: IntArray) {
        val n = bw * bh
        val b0 = byteAt(data, off)
        val b1 = byteAt(data, off + 1)

        if (b0 == 0xfc && (b1 and 1) == 1) {
            // Void-extent block: one constant colour for the whole block.
            val c: Int
            if (b1 and 2 != 0) {
                c = (f16PtrToU8(data, off + 14) shl 24) or (f16PtrToU8(data, off + 8) shl 16) or
                    (f16PtrToU8(data, off + 10) shl 8) or f16PtrToU8(data, off + 12)
            } else {
                // color(buf[9], buf[11], buf[13], buf[15])
                c = (byteAt(data, off + 15) shl 24) or (byteAt(data, off + 9) shl 16) or
                    (byteAt(data, off + 11) shl 8) or byteAt(data, off + 13)
            }
            for (i in 0 until n) out[i] = c
            return
        }
        if (((b0 and 0xc3) == 0xc0 && (b1 and 1) == 1) || (b0 and 0xf) == 0) {
            // Reserved / invalid block mode - the reference emits magenta, we emit opaque black.
            for (i in 0 until n) out[i] = BLACK
            return
        }

        s.bw = bw
        s.bh = bh
        decodeBlockParams(data, off, s)
        if (s.weightNum > s.wv.size) {
            for (i in 0 until n) out[i] = BLACK
            return
        }
        decodeEndpoints(data, off, s)
        decodeWeights(data, off, s)
        if (s.partNum > 1) selectPartition(data, off, s)
        applicateColor(s, out)
    }

    /**
     * `copy_block_buffer`: blit the decoded block into [image], clipping at the right and bottom
     * edges.
     */
    private fun copyBlock(
        bx: Int, by: Int, w: Int, h: Int, bw: Int, bh: Int, buffer: IntArray, image: IntArray
    ) {
        val x = bw * bx
        val xl = if (bw * (bx + 1) > w) w - bw * bx else bw
        var src = 0
        var y = by * bh
        var rows = 0
        while (rows < bh && y < h) {
            System.arraycopy(buffer, src, image, y * w + x, xl)
            src += bw
            y++
            rows++
        }
    }

    fun decode(data: ByteArray, width: Int, height: Int, bw: Int, bh: Int): IntArray {
        require(width > 0) { "width must be > 0" }
        require(height > 0) { "height must be > 0" }
        require(bw > 0 && bh > 0) { "block footprint must be positive" }
        require(bw > 1 && bh > 1) { "block footprint must be at least 2x2" }

        val blocksX = (width + bw - 1) / bw
        val blocksY = (height + bh - 1) / bh
        val required = blocksX.toLong() * blocksY.toLong() * 16L
        require(data.size.toLong() >= required) {
            "ASTC payload too small: need $required bytes for ${width}x${height} @ ${bw}x$bh," +
                " got ${data.size}"
        }

        val out = IntArray(width * height)
        val s = Scratch()
        val block = IntArray(bw * bh)

        var off = 0
        for (by in 0 until blocksY) {
            for (bx in 0 until blocksX) {
                decodeBlock(data, off, bw, bh, s, block)
                copyBlock(bx, by, width, height, bw, bh, block, out)
                off += 16
            }
        }
        return out
    }

    // ------------------------------------------------------------------ per-block scratch

    /**
     * Reusable per-block state. Allocated once per [decode] call so the inner loop never
     * allocates. Arrays are sized for the worst case the header can legally request (a 12x12
     * weight grid), not just our 5x5/6x6 footprints, so malformed headers cannot run off the end.
     */
    private class Scratch {
        var bw = 0
        var bh = 0
        var w = 0
        var h = 0
        var partNum = 0
        var dualPlane = 0
        var planeSelector = 0
        var weightRange = 0
        var weightNum = 0
        var cemRange = 0
        var cemRangeFound = false
        /** Last CEM range that fit the bit budget; see the fallback in `decodeBlockParams`. */
        var stickyCemRange = 0
        var endpointValueNum = 0
        val cem = IntArray(4)
        val endpoints = IntArray(32) // endpoints[part][8]
        val weights = IntArray(288) // weights[grid index][plane]
        val partition = IntArray(144)
        val seqBits = IntArray(288)
        val seqNonbits = IntArray(288)
        val ev = IntArray(32)
        val wv = IntArray(1024)
        val ps = IntArray(4) // dual-plane channel mapping, rebuilt per block
    }

    // ------------------------------------------------------------------ tables

    private val MT = intArrayOf(0, 2, 4, 5, 7)
    private val MQ = intArrayOf(0, 3, 5)

    private val BIT_REVERSE = intArrayOf(
        0, 128, 64, 192, 32, 160, 96, 224, 16, 144, 80, 208, 48, 176, 112, 240,
        8, 136, 72, 200, 40, 168, 104, 232, 24, 152, 88, 216, 56, 184, 120, 248,
        4, 132, 68, 196, 36, 164, 100, 228, 20, 148, 84, 212, 52, 180, 116, 244,
        12, 140, 76, 204, 44, 172, 108, 236, 28, 156, 92, 220, 60, 188, 124, 252,
        2, 130, 66, 194, 34, 162, 98, 226, 18, 146, 82, 210, 50, 178, 114, 242,
        10, 138, 74, 202, 42, 170, 106, 234, 26, 154, 90, 218, 58, 186, 122, 250,
        6, 134, 70, 198, 38, 166, 102, 230, 22, 150, 86, 214, 54, 182, 118, 246,
        14, 142, 78, 206, 46, 174, 110, 238, 30, 158, 94, 222, 62, 190, 126, 254,
        1, 129, 65, 193, 33, 161, 97, 225, 17, 145, 81, 209, 49, 177, 113, 241,
        9, 137, 73, 201, 41, 169, 105, 233, 25, 153, 89, 217, 57, 185, 121, 249,
        5, 133, 69, 197, 37, 165, 101, 229, 21, 149, 85, 213, 53, 181, 117, 245,
        13, 141, 77, 205, 45, 173, 109, 237, 29, 157, 93, 221, 61, 189, 125, 253,
        3, 131, 67, 195, 35, 163, 99, 227, 19, 147, 83, 211, 51, 179, 115, 243,
        11, 139, 75, 203, 43, 171, 107, 235, 27, 155, 91, 219, 59, 187, 123, 251,
        7, 135, 71, 199, 39, 167, 103, 231, 23, 151, 87, 215, 55, 183, 119, 247,
        15, 143, 79, 207, 47, 175, 111, 239, 31, 159, 95, 223, 63, 191, 127, 255
    )

    private val WEIGHT_PREC_TABLE_A = intArrayOf(0, 0, 0, 3, 0, 5, 3, 0, 0, 0, 5, 3, 0, 5, 3, 0)
    private val WEIGHT_PREC_TABLE_B = intArrayOf(0, 0, 1, 0, 2, 0, 1, 3, 0, 0, 1, 2, 4, 2, 3, 5)

    private val CEM_TABLE_A = intArrayOf(0, 3, 5, 0, 3, 5, 0, 3, 5, 0, 3, 5, 0, 3, 5, 0, 3, 0, 0)
    private val CEM_TABLE_B = intArrayOf(8, 6, 5, 7, 5, 4, 6, 4, 3, 5, 3, 2, 4, 2, 1, 3, 1, 2, 1)

    private val TRITS_SCALE = intArrayOf(0, 204, 93, 44, 22, 11, 5)
    private val QUINTS_SCALE = intArrayOf(0, 113, 54, 26, 13, 6)

    /** FuncTableC: true where the C reference uses `select_color_hdr`. */
    private val FUNC_TABLE_C = booleanArrayOf(
        false, false, true, true, false, false, false, true, false, false, false, true, false, false, true, true
    )

    /** FuncTableA: like [FUNC_TABLE_C] but entries 14/15 differ. */
    private val FUNC_TABLE_A = booleanArrayOf(
        false, false, true, true, false, false, false, true, false, false, false, true, false, false, false, true
    )

    private val TRITS_2D = intArrayOf(
        0, 1, 2, 0, 0, 1, 2, 1, 0, 1, 2, 2, 0, 1, 2, 2,
        0, 1, 2, 0, 0, 1, 2, 1, 0, 1, 2, 2, 0, 1, 2, 0,
        0, 1, 2, 0, 0, 1, 2, 1, 0, 1, 2, 2, 0, 1, 2, 2,
        0, 1, 2, 0, 0, 1, 2, 1, 0, 1, 2, 2, 0, 1, 2, 1,
        0, 1, 2, 0, 0, 1, 2, 1, 0, 1, 2, 2, 0, 1, 2, 2,
        0, 1, 2, 0, 0, 1, 2, 1, 0, 1, 2, 2, 0, 1, 2, 2,
        0, 1, 2, 0, 0, 1, 2, 1, 0, 1, 2, 2, 0, 1, 2, 2,
        0, 1, 2, 0, 0, 1, 2, 1, 0, 1, 2, 2, 0, 1, 2, 2,
        0, 1, 2, 0, 0, 1, 2, 1, 0, 1, 2, 2, 0, 1, 2, 2,
        0, 1, 2, 0, 0, 1, 2, 1, 0, 1, 2, 2, 0, 1, 2, 0,
        0, 1, 2, 0, 0, 1, 2, 1, 0, 1, 2, 2, 0, 1, 2, 2,
        0, 1, 2, 0, 0, 1, 2, 1, 0, 1, 2, 2, 0, 1, 2, 1,
        0, 1, 2, 0, 0, 1, 2, 1, 0, 1, 2, 2, 0, 1, 2, 2,
        0, 1, 2, 0, 0, 1, 2, 1, 0, 1, 2, 2, 0, 1, 2, 2,
        0, 1, 2, 0, 0, 1, 2, 1, 0, 1, 2, 2, 0, 1, 2, 2,
        0, 1, 2, 0, 0, 1, 2, 1, 0, 1, 2, 2, 0, 1, 2, 2,
        0, 0, 0, 0, 1, 1, 1, 0, 2, 2, 2, 0, 2, 2, 2, 0,
        0, 0, 0, 1, 1, 1, 1, 1, 2, 2, 2, 1, 0, 0, 0, 0,
        0, 0, 0, 0, 1, 1, 1, 0, 2, 2, 2, 0, 2, 2, 2, 0,
        0, 0, 0, 1, 1, 1, 1, 1, 2, 2, 2, 1, 1, 1, 1, 0,
        0, 0, 0, 0, 1, 1, 1, 0, 2, 2, 2, 0, 2, 2, 2, 0,
        0, 0, 0, 1, 1, 1, 1, 1, 2, 2, 2, 1, 2, 2, 2, 0,
        0, 0, 0, 0, 1, 1, 1, 0, 2, 2, 2, 0, 2, 2, 2, 0,
        0, 0, 0, 1, 1, 1, 1, 1, 2, 2, 2, 1, 2, 2, 2, 0,
        0, 0, 0, 0, 1, 1, 1, 0, 2, 2, 2, 0, 2, 2, 2, 0,
        0, 0, 0, 1, 1, 1, 1, 1, 2, 2, 2, 1, 0, 0, 0, 1,
        0, 0, 0, 0, 1, 1, 1, 0, 2, 2, 2, 0, 2, 2, 2, 0,
        0, 0, 0, 1, 1, 1, 1, 1, 2, 2, 2, 1, 1, 1, 1, 1,
        0, 0, 0, 0, 1, 1, 1, 0, 2, 2, 2, 0, 2, 2, 2, 0,
        0, 0, 0, 1, 1, 1, 1, 1, 2, 2, 2, 1, 2, 2, 2, 1,
        0, 0, 0, 0, 1, 1, 1, 0, 2, 2, 2, 0, 2, 2, 2, 0,
        0, 0, 0, 1, 1, 1, 1, 1, 2, 2, 2, 1, 2, 2, 2, 1,
        0, 0, 0, 2, 0, 0, 0, 2, 0, 0, 0, 2, 2, 2, 2, 2,
        1, 1, 1, 2, 1, 1, 1, 2, 1, 1, 1, 2, 0, 0, 0, 2,
        0, 0, 0, 2, 0, 0, 0, 2, 0, 0, 0, 2, 2, 2, 2, 2,
        1, 1, 1, 2, 1, 1, 1, 2, 1, 1, 1, 2, 0, 0, 0, 2,
        0, 0, 0, 2, 0, 0, 0, 2, 0, 0, 0, 2, 2, 2, 2, 2,
        1, 1, 1, 2, 1, 1, 1, 2, 1, 1, 1, 2, 0, 0, 0, 2,
        0, 0, 0, 2, 0, 0, 0, 2, 0, 0, 0, 2, 2, 2, 2, 2,
        1, 1, 1, 2, 1, 1, 1, 2, 1, 1, 1, 2, 2, 2, 2, 2,
        0, 0, 0, 2, 0, 0, 0, 2, 0, 0, 0, 2, 2, 2, 2, 2,
        1, 1, 1, 2, 1, 1, 1, 2, 1, 1, 1, 2, 1, 1, 1, 2,
        0, 0, 0, 2, 0, 0, 0, 2, 0, 0, 0, 2, 2, 2, 2, 2,
        1, 1, 1, 2, 1, 1, 1, 2, 1, 1, 1, 2, 1, 1, 1, 2,
        0, 0, 0, 2, 0, 0, 0, 2, 0, 0, 0, 2, 2, 2, 2, 2,
        1, 1, 1, 2, 1, 1, 1, 2, 1, 1, 1, 2, 1, 1, 1, 2,
        0, 0, 0, 2, 0, 0, 0, 2, 0, 0, 0, 2, 2, 2, 2, 2,
        1, 1, 1, 2, 1, 1, 1, 2, 1, 1, 1, 2, 2, 2, 2, 2,
        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 2, 2, 2, 2,
        1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
        1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 2, 2, 2, 2,
        2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2,
        2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2,
        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 2, 2, 2, 2,
        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 2, 2, 2, 2,
        1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
        1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 2, 2, 2, 2,
        2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2,
        2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2,
        1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
        1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 2, 2, 2, 2,
        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 2, 2, 2, 2,
        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 2, 2, 2, 2,
        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 2, 2, 2, 2,
        2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2,
        2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2,
        1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
        1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 2, 2, 2, 2,
        1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
        1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 2, 2, 2, 2,
        1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
        1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 2, 2, 2, 2,
        2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2,
        2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2
    )

    private val QUINTS_2D = intArrayOf(
        0, 1, 2, 3, 4, 0, 4, 4, 0, 1, 2, 3, 4, 1, 4, 4,
        0, 1, 2, 3, 4, 2, 4, 4, 0, 1, 2, 3, 4, 3, 4, 4,
        0, 1, 2, 3, 4, 0, 4, 0, 0, 1, 2, 3, 4, 1, 4, 1,
        0, 1, 2, 3, 4, 2, 4, 2, 0, 1, 2, 3, 4, 3, 4, 3,
        0, 1, 2, 3, 4, 0, 2, 3, 0, 1, 2, 3, 4, 1, 2, 3,
        0, 1, 2, 3, 4, 2, 2, 3, 0, 1, 2, 3, 4, 3, 2, 3,
        0, 1, 2, 3, 4, 0, 0, 1, 0, 1, 2, 3, 4, 1, 0, 1,
        0, 1, 2, 3, 4, 2, 0, 1, 0, 1, 2, 3, 4, 3, 0, 1,
        0, 0, 0, 0, 0, 4, 4, 4, 1, 1, 1, 1, 1, 4, 4, 4,
        2, 2, 2, 2, 2, 4, 4, 4, 3, 3, 3, 3, 3, 4, 4, 4,
        0, 0, 0, 0, 0, 4, 0, 4, 1, 1, 1, 1, 1, 4, 1, 4,
        2, 2, 2, 2, 2, 4, 2, 4, 3, 3, 3, 3, 3, 4, 3, 4,
        0, 0, 0, 0, 0, 4, 0, 0, 1, 1, 1, 1, 1, 4, 1, 1,
        2, 2, 2, 2, 2, 4, 2, 2, 3, 3, 3, 3, 3, 4, 3, 3,
        0, 0, 0, 0, 0, 4, 0, 0, 1, 1, 1, 1, 1, 4, 1, 1,
        2, 2, 2, 2, 2, 4, 2, 2, 3, 3, 3, 3, 3, 4, 3, 3,
        0, 0, 0, 0, 0, 0, 0, 4, 0, 0, 0, 0, 0, 0, 1, 4,
        0, 0, 0, 0, 0, 0, 2, 4, 0, 0, 0, 0, 0, 0, 3, 4,
        1, 1, 1, 1, 1, 1, 4, 4, 1, 1, 1, 1, 1, 1, 4, 4,
        1, 1, 1, 1, 1, 1, 4, 4, 1, 1, 1, 1, 1, 1, 4, 4,
        2, 2, 2, 2, 2, 2, 4, 4, 2, 2, 2, 2, 2, 2, 4, 4,
        2, 2, 2, 2, 2, 2, 4, 4, 2, 2, 2, 2, 2, 2, 4, 4,
        3, 3, 3, 3, 3, 3, 4, 4, 3, 3, 3, 3, 3, 3, 4, 4,
        3, 3, 3, 3, 3, 3, 4, 4, 3, 3, 3, 3, 3, 3, 4, 4
    )

    private val FP16_EXP_SCALE = Float.fromBits(0x07800000) // 2^-112
    private const val DENORM_CUTOFF = 1 shl 27
    private val DENORM_CUTOFF_BIASED = DENORM_CUTOFF xor Int.MIN_VALUE
}
