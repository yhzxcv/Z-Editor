package com.example.z_editor.datapack.crypto

import android.util.Log
import java.io.ByteArrayOutputStream
import java.security.MessageDigest
import java.util.zip.Deflater
import java.util.zip.Inflater

/**
 * PvZ2 crypto pipeline — 1:1 port of pyvz2's pvz2_crypto.py + pyvz2nineteendo.py.
 *
 * Pipeline:
 *   encrypt: plaintext → PopCap Zlib compress → Rijndael-CBC encrypt → 0x1000 header
 *   decrypt: 0x1000 header → Rijndael-CBC decrypt → PopCap Zlib decompress → plaintext
 */
object Pvz2Crypto {

    private const val TAG = "Pvz2Crypto"
    const val BLOCK_SIZE = 24
    val ENCRYPTION_HEADER = byteArrayOf(0x10.toByte(), 0x00.toByte())
    const val POPCAP_ZLIB_MAGIC = 0xDEADFED4.toInt()

    /** PvZ2 Chinese version default key string */
    const val DEFAULT_KEY = "com_popcap_pvz2_magento_product_2013_05_05"

    // ---- Key preparation (matching pyvz2's prepare_key) ----

    /**
     * Prepare 32-byte key from a key string.
     * If keyStr is null, uses DEFAULT_KEY.
     * If keyStr is a 32-char hex string, encodes directly as ASCII.
     * Otherwise, derives via MD5 hex digest (compatible with PvZ2_Level_Tool).
     */
    fun prepareKey(keyStr: String?): ByteArray {
        val effective = keyStr ?: DEFAULT_KEY

        // If it looks like a 32-char hex string, encode directly
        if (effective.length == 32 && effective.all { it in '0'..'9' || it in 'a'..'f' || it in 'A'..'F' }) {
            return effective.toByteArray(Charsets.US_ASCII).let {
                if (it.size >= 32) it.copyOf(32) else it + ByteArray(32 - it.size)
            }
        }

        // Otherwise MD5 it
        val md5 = MessageDigest.getInstance("MD5")
        val digest = md5.digest(effective.toByteArray(Charsets.UTF_8))
        return digest.joinToString("") { "%02x".format(it.toInt() and 0xFF) }
            .lowercase()
            .toByteArray(Charsets.US_ASCII)
    }

    // ---- PopCap Zlib (matching pyvz2's popcap_zlib_compress/decompress) ----

    fun popcapZlibCompress(data: ByteArray): ByteArray {
        val deflater = Deflater(Deflater.DEFAULT_COMPRESSION)
        val compressed = ByteArrayOutputStream()
        try {
            deflater.setInput(data)
            deflater.finish()
            val buf = ByteArray(4096)
            while (!deflater.finished()) {
                val n = deflater.deflate(buf)
                if (n > 0) compressed.write(buf, 0, n)
            }
        } finally {
            deflater.end()
        }
        val zlibData = compressed.toByteArray()
        // Format: magic(4) + uncompressed_size(4) + zlib_data
        val result = ByteArray(8 + zlibData.size)
        result[0] = (POPCAP_ZLIB_MAGIC and 0xFF).toByte()
        result[1] = ((POPCAP_ZLIB_MAGIC ushr 8) and 0xFF).toByte()
        result[2] = ((POPCAP_ZLIB_MAGIC ushr 16) and 0xFF).toByte()
        result[3] = ((POPCAP_ZLIB_MAGIC ushr 24) and 0xFF).toByte()
        val size = data.size
        result[4] = (size and 0xFF).toByte()
        result[5] = ((size ushr 8) and 0xFF).toByte()
        result[6] = ((size ushr 16) and 0xFF).toByte()
        result[7] = ((size ushr 24) and 0xFF).toByte()
        zlibData.copyInto(result, 8)
        return result
    }

    fun popcapZlibDecompress(data: ByteArray): ByteArray {
        if (data.size < 8) return data

        val magic = (data[0].toInt() and 0xFF) or
            ((data[1].toInt() and 0xFF) shl 8) or
            ((data[2].toInt() and 0xFF) shl 16) or
            ((data[3].toInt() and 0xFF) shl 24)

        if (magic == POPCAP_ZLIB_MAGIC) {
            return try {
                val inflater = Inflater()
                val result = ByteArrayOutputStream()
                inflater.setInput(data, 8, data.size - 8)
                val buf = ByteArray(4096)
                while (!inflater.finished()) {
                    result.write(buf, 0, inflater.inflate(buf))
                }
                inflater.end()
                result.toByteArray()
            } catch (e: Exception) {
                Log.e(TAG, "PopCap zlib decompress failed", e)
                data
            }
        }

        // Fallback: plain zlib (0x78 magic byte)
        if (data.isNotEmpty() && data[0] == 0x78.toByte()) {
            return try {
                val inflater = Inflater()
                val result = ByteArrayOutputStream()
                inflater.setInput(data)
                val buf = ByteArray(4096)
                while (!inflater.finished()) {
                    result.write(buf, 0, inflater.inflate(buf))
                }
                inflater.end()
                result.toByteArray()
            } catch (e: Exception) {
                Log.e(TAG, "Standard zlib decompress failed", e)
                data
            }
        }

        // Return as-is if not compressed
        return data
    }

    // ---- High-level encrypt/decrypt (matching pyvz2's encrypt_data/decrypt_data) ----

    /**
     * Full encryption pipeline.
     * plaintext → PopCap Zlib compress → Rijndael-CBC encrypt → 0x1000 header.
     */
    fun encryptData(plaintext: ByteArray, keyStr: String? = null): ByteArray {
        val keyBytes = prepareKey(keyStr)
        val cipher = RijndaelCbc(keyBytes, BLOCK_SIZE)

        // Always compress before encrypting (matching pyvz2's encrypt_data)
        val compressed = popcapZlibCompress(plaintext)
        val encrypted = cipher.encrypt(compressed)

        // Add 0x1000 header
        return ENCRYPTION_HEADER + encrypted
    }

    /**
     * Full decryption pipeline.
     * Strip 0x1000 → Rijndael-CBC decrypt → PopCap Zlib decompress.
     */
    fun decryptData(encrypted: ByteArray, keyStr: String? = null): ByteArray {
        val keyBytes = prepareKey(keyStr)
        val cipher = RijndaelCbc(keyBytes, BLOCK_SIZE)

        // Strip encryption header if present
        var data = encrypted
        if (data.size >= 2 && data[0] == ENCRYPTION_HEADER[0] && data[1] == ENCRYPTION_HEADER[1]) {
            data = data.copyOfRange(2, data.size)
        }

        val decrypted = cipher.decrypt(data)

        // Always decompress after decrypting
        return popcapZlibDecompress(decrypted)
    }

    /**
     * Encrypt JSON string to game-compatible format.
     */
    fun encryptJsonString(json: String, keyStr: String? = null): ByteArray {
        return encryptData(json.toByteArray(Charsets.UTF_8), keyStr)
    }

    /**
     * Decrypt to JSON string.
     */
    fun decryptToJsonString(encrypted: ByteArray, keyStr: String? = null): String {
        val bytes = decryptData(encrypted, keyStr)
        // Strip trailing nulls that may remain from decompression
        var end = bytes.size
        while (end > 0 && bytes[end - 1] == 0.toByte()) end--
        return String(bytes, 0, end, Charsets.UTF_8)
    }
}
