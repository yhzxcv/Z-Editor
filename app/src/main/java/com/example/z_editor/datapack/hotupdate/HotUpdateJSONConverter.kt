package com.example.z_editor.datapack.hotupdate

import android.content.Context
import android.net.Uri
import android.util.Base64
import androidx.documentfile.provider.DocumentFile
import com.example.z_editor.datapack.crypto.Pvz2Crypto

/**
 * Hot-update JSON ↔ normal JSON file converter, SAF-aware.
 *
 * Hot-update format: Base64(0x1000 + Rijndael-192-CBC(0xDEADFED4 + zlib(JSON))).
 * The encryption is the same as RTON but with a different pipeline order.
 *
 * Ported from scripts/hujson/PvZ2_Level_Tool.py + packer.py
 */
object HotUpdateJSONConverter {

    fun convertToNormalJson(
        context: Context,
        inputUri: Uri,
        outputDirUri: Uri,
        outputName: String,
        key: String
    ): Result<String> {
        return try {
            // Read input
            val inputStr = context.contentResolver.openInputStream(inputUri)!!.use {
                it.readBytes().toString(Charsets.UTF_8)
            }

            // Decode pipeline: base64 → strip 0x1000 → Rijndael decrypt → strip 0xDEADFED4 → zlib decompress
            val decoded = decodeHotUpdateString(inputStr, key)

            // Write output (caller handles overwrite check)
            writeFile(
                context,
                outputDirUri,
                outputName,
                decoded.toByteArray(Charsets.UTF_8),
                "application/json"
            )
            Result.success(outputName)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun convertToHotUpdateJson(
        context: Context,
        inputUri: Uri,
        outputDirUri: Uri,
        outputName: String,
        key: String
    ): Result<String> {
        return try {
            // Read input JSON
            val inputStr = context.contentResolver.openInputStream(inputUri)!!.use {
                it.readBytes().toString(Charsets.UTF_8)
            }

            // Encode pipeline: zlib compress → add 0xDEADFED4 → Rijndael encrypt → add 0x1000 → base64
            val encoded = encodeHotUpdateString(inputStr, key)

            // Write output (caller handles overwrite check)
            writeFile(
                context,
                outputDirUri,
                outputName,
                encoded.toByteArray(Charsets.UTF_8),
                "application/json"
            )
            Result.success(outputName)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Shared SAF write helper
    private fun writeFile(
        context: Context,
        dirUri: Uri,
        fileName: String,
        data: ByteArray,
        mimeType: String
    ) {
        val dir = DocumentFile.fromTreeUri(context, dirUri)!!
        val existing = dir.findFile(fileName)
        if (existing != null) {
            existing.delete()
        }
        val file = dir.createFile(mimeType, fileName)
            ?: throw Exception("Failed to create file $fileName")
        context.contentResolver.openOutputStream(file.uri)!!.use { it.write(data) }
    }

    /**
     * Detect if a file content is a hot-update JSON (base64-encoded encrypted data).
     */
    fun isHotUpdateFormat(content: String): Boolean {
        val trimmed = content.trim()
        // Hot-update format is base64-encoded binary, try to decode
        return try {
            val decoded = Base64.decode(trimmed, Base64.DEFAULT)
            // Should start with 0x1000 header
            decoded.size >= 2 && decoded[0] == 0x10.toByte() && decoded[1] == 0x00.toByte()
        } catch (_: Exception) {
            false
        }
    }

    // ---- Low-level encode/decode ----

    fun decodeHotUpdateString(input: String, key: String): String {
        val trimmed = input.trim()
        return try {
            // Step 1: Base64 decode
            val raw = Base64.decode(trimmed, Base64.DEFAULT)

            // Step 2: Strip 0x1000 header
            val data = if (raw.size >= 2 && raw[0] == 0x10.toByte() && raw[1] == 0x00.toByte()) {
                raw.copyOfRange(2, raw.size)
            } else {
                raw
            }

            // Step 3: Rijndael decrypt (new API handles padding internally)
            val keyBytes = Pvz2Crypto.prepareKey(key)
            val cipher = com.example.z_editor.datapack.crypto.RijndaelCbc(keyBytes, 24)
            val decrypted = cipher.decrypt(data)

            // Step 4: PopCap Zlib decompress
            val decompressed = Pvz2Crypto.popcapZlibDecompress(decrypted)

            // Strip trailing nulls
            var end = decompressed.size
            while (end > 0 && decompressed[end - 1] == 0.toByte()) end--
            String(decompressed, 0, end, Charsets.UTF_8)
        } catch (e: Exception) {
            throw Exception("Failed to decode hot-update JSON: ${e.message}", e)
        }
    }

    fun encodeHotUpdateString(input: String, key: String): String {
        val plainBytes = input.toByteArray(Charsets.UTF_8)
        val keyBytes = Pvz2Crypto.prepareKey(key)

        // Step 1: PopCap Zlib compress
        val compressed = Pvz2Crypto.popcapZlibCompress(plainBytes)

        // Step 2: Rijndael encrypt (new API handles padding internally)
        val cipher = com.example.z_editor.datapack.crypto.RijndaelCbc(keyBytes, 24)
        val encrypted = cipher.encrypt(compressed)

        // Step 3: Prepend 0x1000 header
        val header = byteArrayOf(0x10.toByte(), 0x00.toByte())
        val withHeader = header + encrypted

        // Step 4: Base64 encode
        return Base64.encodeToString(withHeader, Base64.NO_WRAP)
    }
}
