package com.example.z_editor.datapack.crypto

/**
 * Rijndael-192-CBC cipher — 1:1 port of pyvz2's RijndaelCBC class
 * (pyvz2rijndael.py from nineteendo/pyvz2).
 *
 * PvZ2 uses 24-byte blocks with a 32-byte key.
 * IV is derived from key[4:28] (not passed separately).
 * Encrypt pads internally with zeros; decrypt strips trailing nulls.
 */
class RijndaelCbc(
    val key: ByteArray,
    val blockSize: Int = BLOCK_SIZE
) {
    companion object {
        const val BLOCK_SIZE = 24

        // ---- GF(2^8) tables ----

        private val aLog = IntArray(256).also {
            it[0] = 1
            for (i in 1 until 256) {
                var j = (it[i - 1] shl 1) xor it[i - 1]
                if (j and 0x100 != 0) j = j xor 0x11B
                it[i] = j
            }
        }

        private val log = IntArray(256).also { logArr ->
            for (i in 1 until 255) {
                logArr[aLog[i] and 0xFF] = i
            }
        }

        private fun mul(a: Int, b: Int): Int {
            if (a == 0 || b == 0) return 0
            return aLog[(log[a and 0xFF] + log[b and 0xFF]) % 255]
        }

        // ---- S-boxes ----

        private val A = arrayOf(
            intArrayOf(1, 1, 1, 1, 1, 0, 0, 0),
            intArrayOf(0, 1, 1, 1, 1, 1, 0, 0),
            intArrayOf(0, 0, 1, 1, 1, 1, 1, 0),
            intArrayOf(0, 0, 0, 1, 1, 1, 1, 1),
            intArrayOf(1, 0, 0, 0, 1, 1, 1, 1),
            intArrayOf(1, 1, 0, 0, 0, 1, 1, 1),
            intArrayOf(1, 1, 1, 0, 0, 0, 1, 1),
            intArrayOf(1, 1, 1, 1, 0, 0, 0, 1)
        )
        private val B = intArrayOf(0, 1, 1, 0, 0, 0, 1, 1)

        val S = IntArray(256).also { s ->
            val box = Array(256) { IntArray(8) }
            box[1][7] = 1
            for (i in 2 until 256) {
                val j = aLog[255 - log[i]]
                for (t in 0 until 8) {
                    box[i][t] = (j shr (7 - t)) and 0x01
                }
            }
            val cox = Array(256) { IntArray(8) }
            for (i in 0 until 256) {
                for (t in 0 until 8) {
                    cox[i][t] = B[t]
                    for (j in 0 until 8) {
                        cox[i][t] = cox[i][t] xor (A[t][j] * box[i][j])
                    }
                }
            }
            for (i in 0 until 256) {
                s[i] = cox[i][0] shl 7
                for (t in 1 until 8) {
                    s[i] = s[i] xor (cox[i][t] shl (7 - t))
                }
            }
        }

        val Si = IntArray(256).also { si ->
            for (i in 0 until 256) {
                si[S[i] and 0xFF] = i
            }
        }

        // ---- T-boxes & U-boxes ----

        private val G = arrayOf(
            intArrayOf(2, 1, 1, 3),
            intArrayOf(3, 2, 1, 1),
            intArrayOf(1, 3, 2, 1),
            intArrayOf(1, 1, 3, 2)
        )

        // Compute iG = inverse of G in GF(2^8)
        private val iG: Array<IntArray> = run {
            val aug = Array(4) { IntArray(8) }
            for (i in 0 until 4) {
                for (j in 0 until 4) aug[i][j] = G[i][j]
                aug[i][i + 4] = 1
            }
            for (i in 0 until 4) {
                val pivot = aug[i][i]
                for (j in 0 until 8) {
                    if (aug[i][j] != 0) {
                        aug[i][j] =
                            aLog[(255 + log[aug[i][j] and 0xFF] - log[pivot and 0xFF]) % 255]
                    }
                }
                for (t in 0 until 4) {
                    if (i != t) {
                        for (j in i + 1 until 8) {
                            aug[t][j] = aug[t][j] xor mul(aug[i][j], aug[t][i])
                        }
                        aug[t][i] = 0
                    }
                }
            }
            Array(4) { i -> IntArray(4) { j -> aug[i][j + 4] } }
        }

        private fun mul4(x: Int, bs: IntArray): Int {
            if (x == 0) return 0
            var rr = 0
            for (b in bs) {
                rr = rr shl 8
                if (b != 0) rr = rr or mul(x, b)
            }
            return rr
        }

        val T1 = IntArray(256) { mul4(S[it], G[0]) }
        val T2 = IntArray(256) { mul4(S[it], G[1]) }
        val T3 = IntArray(256) { mul4(S[it], G[2]) }
        val T4 = IntArray(256) { mul4(S[it], G[3]) }
        val T5 = IntArray(256) { mul4(Si[it], iG[0]) }
        val T6 = IntArray(256) { mul4(Si[it], iG[1]) }
        val T7 = IntArray(256) { mul4(Si[it], iG[2]) }
        val T8 = IntArray(256) { mul4(Si[it], iG[3]) }
        val U1 = IntArray(256) { mul4(it, iG[0]) }
        val U2 = IntArray(256) { mul4(it, iG[1]) }
        val U3 = IntArray(256) { mul4(it, iG[2]) }
        val U4 = IntArray(256) { mul4(it, iG[3]) }

        // ---- Round constants ----

        private val rCon = IntArray(30).also {
            it[0] = 1
            var r = 1
            for (t in 1 until 30) {
                r = mul(2, r)
                it[t] = r
            }
        }

        // ---- Shift offsets (PvZ2 custom, from pyvz2) ----
        // shifts[s_c][row][0] = encrypt offset, [1] = decrypt offset
        private val shifts = arrayOf(
            arrayOf(intArrayOf(0, 0), intArrayOf(1, 3), intArrayOf(2, 2), intArrayOf(3, 1)),
            arrayOf(intArrayOf(0, 0), intArrayOf(1, 5), intArrayOf(2, 4), intArrayOf(3, 3)),
            arrayOf(intArrayOf(0, 0), intArrayOf(1, 7), intArrayOf(3, 5), intArrayOf(4, 4))
        )

        private val numRounds = mapOf(
            16 to mapOf(16 to 10, 24 to 12, 32 to 14),
            24 to mapOf(16 to 12, 24 to 12, 32 to 14),
            32 to mapOf(16 to 14, 24 to 14, 32 to 14)
        )

        // ---- Key expansion ----

        fun expandKey(key: ByteArray, blockSize: Int): Pair<Array<IntArray>, Array<IntArray>> {
            val kLen = key.size
            val bC = blockSize / 4
            val rounds = numRounds[kLen]?.get(blockSize)
                ?: throw IllegalArgumentException("Bad key/block: $kLen/$blockSize")
            val roundKeyCount = (rounds + 1) * bC
            val kC = kLen / 4

            val kE = Array(rounds + 1) { IntArray(bC) }
            val kD = Array(rounds + 1) { IntArray(bC) }

            // Copy user material bytes into temporary ints
            val tk = IntArray(kC) { i ->
                val off = i * 4
                (key[off].toInt() and 0xFF shl 24) or
                        (key[off + 1].toInt() and 0xFF shl 16) or
                        (key[off + 2].toInt() and 0xFF shl 8) or
                        (key[off + 3].toInt() and 0xFF)
            }

            // Copy values into round key arrays
            var t = 0
            var j = 0
            while (j < kC && t < roundKeyCount) {
                kE[t / bC][t % bC] = tk[j]
                kD[rounds - (t / bC)][t % bC] = tk[j]
                j++
                t++
            }
            var rConPtr = 0
            while (t < roundKeyCount) {
                // Extrapolate using phi (the round key evolution function)
                var tt = tk[kC - 1]
                tk[0] = tk[0] xor (
                        ((S[(tt ushr 16) and 0xFF] and 0xFF) shl 24) xor
                                ((S[(tt ushr 8) and 0xFF] and 0xFF) shl 16) xor
                                ((S[tt and 0xFF] and 0xFF) shl 8) xor
                                (S[(tt ushr 24) and 0xFF] and 0xFF) xor
                                ((rCon[rConPtr] and 0xFF) shl 24)
                        )
                rConPtr++
                if (kC != 8) {
                    for (i in 1 until kC) tk[i] = tk[i] xor tk[i - 1]
                } else {
                    for (i in 1 until kC / 2) tk[i] = tk[i] xor tk[i - 1]
                    tt = tk[kC / 2 - 1]
                    tk[kC / 2] = tk[kC / 2] xor (
                            (S[tt and 0xFF] and 0xFF) xor
                                    ((S[(tt ushr 8) and 0xFF] and 0xFF) shl 8) xor
                                    ((S[(tt ushr 16) and 0xFF] and 0xFF) shl 16) xor
                                    ((S[(tt ushr 24) and 0xFF] and 0xFF) shl 24)
                            )
                    for (i in kC / 2 + 1 until kC) tk[i] = tk[i] xor tk[i - 1]
                }
                // Copy values into round key arrays
                j = 0
                while (j < kC && t < roundKeyCount) {
                    kE[t / bC][t % bC] = tk[j]
                    kD[rounds - (t / bC)][t % bC] = tk[j]
                    j++
                    t++
                }
            }
            // Inverse MixColumn where needed
            for (r in 1 until rounds) {
                for (c in 0 until bC) {
                    val w = kD[r][c]
                    kD[r][c] = U1[(w ushr 24) and 0xFF] xor
                            U2[(w ushr 16) and 0xFF] xor
                            U3[(w ushr 8) and 0xFF] xor
                            U4[w and 0xFF]
                }
            }
            return Pair(kE, kD)
        }
    }

    // ---- Instance ----
    // IV = key[4:28], matches pyvz2's `v = self.key[4: 28]`
    val iv: ByteArray = run {
        val end = minOf(key.size, 28)
        val start = minOf(4, end)
        val len = end - start
        val pad = blockSize - len
        if (pad <= 0) key.copyOfRange(start, start + blockSize)
        else key.copyOfRange(start, end) + ByteArray(pad)
    }

    private val keyPair: Pair<Array<IntArray>, Array<IntArray>> = expandKey(key, blockSize)
    private val Ke: Array<IntArray> = keyPair.first
    private val Kd: Array<IntArray> = keyPair.second
    private val rounds = Ke.size - 1
    private val bC = blockSize / 4
    private val sC = when (bC) {
        4 -> 0; 6 -> 1; else -> 2
    }

    // ---- Encrypt ----

    fun encrypt(source: ByteArray): ByteArray {
        // pyvz2's internal zero-padding
        val padSize = blockSize - ((source.size + blockSize - 1) % blockSize + 1)
        val ppt = if (padSize == 0) source else source + ByteArray(padSize)

        val ct = ByteArray(ppt.size)
        var prevBlock = iv

        for (blockStart in ppt.indices step blockSize) {
            val block = ppt.copyOfRange(blockStart, blockStart + blockSize)
            // XOR with previous block (CBC)
            val xored =
                ByteArray(blockSize) { i -> (block[i].toInt() and 0xFF xor (prevBlock[i].toInt() and 0xFF)).toByte() }

            // Encrypt one block
            val s1 = shifts[sC][1][0]
            val s2 = shifts[sC][2][0]
            val s3 = shifts[sC][3][0]

            // Source to ints + key
            var t = IntArray(bC) { i ->
                val off = i * 4
                ((xored[off].toInt() and 0xFF) shl 24) or
                        ((xored[off + 1].toInt() and 0xFF) shl 16) or
                        ((xored[off + 2].toInt() and 0xFF) shl 8) or
                        (xored[off + 3].toInt() and 0xFF) xor Ke[0][i]
            }
            // Apply round transforms
            for (r in 1 until rounds) {
                val a = IntArray(bC) { i ->
                    T1[(t[i] ushr 24) and 0xFF] xor
                            T2[(t[(i + s1) % bC] ushr 16) and 0xFF] xor
                            T3[(t[(i + s2) % bC] ushr 8) and 0xFF] xor
                            T4[t[(i + s3) % bC] and 0xFF] xor Ke[r][i]
                }
                t = a
            }
            // Last round is special
            val encBlock = ByteArray(blockSize)
            for (i in 0 until bC) {
                val rk = Ke[rounds][i]
                encBlock[i * 4] = (S[(t[i] ushr 24) and 0xFF] xor ((rk ushr 24) and 0xFF)).toByte()
                encBlock[i * 4 + 1] =
                    (S[(t[(i + s1) % bC] ushr 16) and 0xFF] xor ((rk ushr 16) and 0xFF)).toByte()
                encBlock[i * 4 + 2] =
                    (S[(t[(i + s2) % bC] ushr 8) and 0xFF] xor ((rk ushr 8) and 0xFF)).toByte()
                encBlock[i * 4 + 3] = (S[t[(i + s3) % bC] and 0xFF] xor (rk and 0xFF)).toByte()
            }
            encBlock.copyInto(ct, blockStart)
            prevBlock = encBlock
        }
        return ct
    }

    // ---- Decrypt ----

    fun decrypt(cipher: ByteArray): ByteArray {
        require(cipher.size % blockSize == 0) {
            "Ciphertext size ${cipher.size} not aligned to block size $blockSize"
        }

        val ppt = ByteArray(cipher.size)
        var prevBlock = iv

        for (blockStart in cipher.indices step blockSize) {
            val block = cipher.copyOfRange(blockStart, blockStart + blockSize)

            val s1 = shifts[sC][1][1]
            val s2 = shifts[sC][2][1]
            val s3 = shifts[sC][3][1]

            // Cipher to ints + key
            var t = IntArray(bC) { i ->
                val off = i * 4
                ((block[off].toInt() and 0xFF) shl 24) or
                        ((block[off + 1].toInt() and 0xFF) shl 16) or
                        ((block[off + 2].toInt() and 0xFF) shl 8) or
                        (block[off + 3].toInt() and 0xFF) xor Kd[0][i]
            }
            // Apply round transforms
            for (r in 1 until rounds) {
                val a = IntArray(bC) { i ->
                    T5[(t[i] ushr 24) and 0xFF] xor
                            T6[(t[(i + s1) % bC] ushr 16) and 0xFF] xor
                            T7[(t[(i + s2) % bC] ushr 8) and 0xFF] xor
                            T8[t[(i + s3) % bC] and 0xFF] xor Kd[r][i]
                }
                t = a
            }
            // Last round is special
            val decBlock = ByteArray(blockSize)
            for (i in 0 until bC) {
                val rk = Kd[rounds][i]
                decBlock[i * 4] = (Si[(t[i] ushr 24) and 0xFF] xor ((rk ushr 24) and 0xFF)).toByte()
                decBlock[i * 4 + 1] =
                    (Si[(t[(i + s1) % bC] ushr 16) and 0xFF] xor ((rk ushr 16) and 0xFF)).toByte()
                decBlock[i * 4 + 2] =
                    (Si[(t[(i + s2) % bC] ushr 8) and 0xFF] xor ((rk ushr 8) and 0xFF)).toByte()
                decBlock[i * 4 + 3] = (Si[t[(i + s3) % bC] and 0xFF] xor (rk and 0xFF)).toByte()
            }
            // XOR with previous block (CBC inverse)
            for (i in 0 until blockSize) {
                ppt[blockStart + i] =
                    (decBlock[i].toInt() and 0xFF xor (prevBlock[i].toInt() and 0xFF)).toByte()
            }
            prevBlock = block
        }

        // Strip trailing nulls (pyvz2's logic)
        var offset = ppt.size
        if (offset == 0) return ByteArray(0)
        val end = offset - blockSize + 1
        while (offset > end) {
            offset--
            if (ppt[offset] != 0.toByte()) return ppt.copyOf(offset + 1)
        }
        return ppt.copyOf(end)
    }
}
