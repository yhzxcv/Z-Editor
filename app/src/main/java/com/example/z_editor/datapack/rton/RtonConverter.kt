package com.example.z_editor.datapack.rton

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import com.example.z_editor.datapack.crypto.Pvz2Crypto
import com.example.z_editor.datapack.crypto.RijndaelCbc
import com.google.gson.GsonBuilder
import com.google.gson.JsonParser

/**
 * High-level RTON ↔ JSON + encrypt/decrypt converter.
 * Uses SAF for all file I/O.
 */
object RtonConverter {

    private val gson = GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create()

    /**
     * JSON text → plain RTON binary.
     */
    fun jsonToPlainRton(
        context: Context,
        inputUri: Uri,
        outputDirUri: Uri,
        outputName: String
    ): Result<String> {
        return try {
            val inputStr = context.contentResolver.openInputStream(inputUri)!!.use {
                it.readBytes().toString(Charsets.UTF_8)
            }
            writeFile(context, outputDirUri, outputName, jsonTextToRtonBytes(inputStr), "application/octet-stream")
            Result.success(outputName)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Plain RTON binary → JSON text.
     */
    fun plainRtonToJson(
        context: Context,
        inputUri: Uri,
        outputDirUri: Uri,
        outputName: String
    ): Result<String> {
        return try {
            val inputBytes =
                context.contentResolver.openInputStream(inputUri)!!.use { it.readBytes() }
            writeFile(
                context,
                outputDirUri,
                outputName,
                rtonBytesToJsonText(inputBytes).toByteArray(Charsets.UTF_8),
                "application/json"
            )
            Result.success(outputName)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Plain RTON → encrypted RTON (game format: 0x1000 + Rijndael-encrypted RTON, no zlib).
     */
    fun encryptRton(
        context: Context,
        inputUri: Uri,
        outputDirUri: Uri,
        outputName: String,
        key: String
    ): Result<String> {
        return try {
            val inputBytes =
                context.contentResolver.openInputStream(inputUri)!!.use { it.readBytes() }
            writeFile(context, outputDirUri, outputName, encryptRtonBytes(inputBytes, key), "application/octet-stream")
            Result.success(outputName)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Encrypted RTON → plain RTON (strip 0x1000 → Rijndael decrypt, no zlib).
     */
    fun decryptRtonToPlain(
        context: Context,
        inputUri: Uri,
        outputDirUri: Uri,
        outputName: String,
        key: String
    ): Result<String> {
        return try {
            val inputBytes =
                context.contentResolver.openInputStream(inputUri)!!.use { it.readBytes() }
            writeFile(context, outputDirUri, outputName, decryptRtonBytes(inputBytes, key), "application/octet-stream")
            Result.success(outputName)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ---- Byte-level APIs (pure JVM; shared by the SAF methods above and batch conversion) ----

    /** JSON text → plain RTON binary (root must be a JSON object). */
    fun jsonTextToRtonBytes(jsonText: String): ByteArray {
        val jsonElement = JsonParser.parseString(jsonText)
        return RtonEncoder.encode(jsonElement.asJsonObject)
    }

    /** Plain RTON binary → pretty-printed JSON text. */
    fun rtonBytesToJsonText(rtonBytes: ByteArray): String {
        return gson.toJson(RtonParser.parse(rtonBytes))
    }

    /** 0x1000 header + Rijndael-CBC encrypt of plain RTON bytes. NO zlib. */
    fun encryptRtonBytes(plainBytes: ByteArray, key: String): ByteArray {
        val keyBytes = Pvz2Crypto.prepareKey(key)
        val cipher = RijndaelCbc(keyBytes, RijndaelCbc.BLOCK_SIZE)
        return Pvz2Crypto.ENCRYPTION_HEADER + cipher.encrypt(plainBytes)
    }

    /** Strip 0x1000 (if present) + Rijndael-CBC decrypt. NO zlib. */
    fun decryptRtonBytes(encryptedBytes: ByteArray, key: String): ByteArray {
        var data = encryptedBytes
        if (data.size >= 2 &&
            data[0] == Pvz2Crypto.ENCRYPTION_HEADER[0] &&
            data[1] == Pvz2Crypto.ENCRYPTION_HEADER[1]
        ) {
            data = data.copyOfRange(2, data.size)
        }
        val keyBytes = Pvz2Crypto.prepareKey(key)
        return RijndaelCbc(keyBytes, RijndaelCbc.BLOCK_SIZE).decrypt(data)
    }

    // ---- Internal ----

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
}
