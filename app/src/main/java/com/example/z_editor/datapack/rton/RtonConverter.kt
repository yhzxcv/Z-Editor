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
            val jsonElement = JsonParser.parseString(inputStr)
            val rtonBinary = RtonEncoder.encode(jsonElement.asJsonObject)

            writeFile(context, outputDirUri, outputName, rtonBinary, "application/octet-stream")
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
            val map = RtonParser.parse(inputBytes)
            val jsonStr = gson.toJson(map)

            writeFile(
                context,
                outputDirUri,
                outputName,
                jsonStr.toByteArray(Charsets.UTF_8),
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
            val keyBytes = Pvz2Crypto.prepareKey(key)
            val cipher = RijndaelCbc(keyBytes, RijndaelCbc.BLOCK_SIZE)
            val encrypted = cipher.encrypt(inputBytes)
            val withHeader = Pvz2Crypto.ENCRYPTION_HEADER + encrypted

            writeFile(context, outputDirUri, outputName, withHeader, "application/octet-stream")
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
            var inputBytes =
                context.contentResolver.openInputStream(inputUri)!!.use { it.readBytes() }
            // Strip 0x1000 header
            if (inputBytes.size >= 2 &&
                inputBytes[0] == Pvz2Crypto.ENCRYPTION_HEADER[0] &&
                inputBytes[1] == Pvz2Crypto.ENCRYPTION_HEADER[1]
            ) {
                inputBytes = inputBytes.copyOfRange(2, inputBytes.size)
            }
            val keyBytes = Pvz2Crypto.prepareKey(key)
            val cipher = RijndaelCbc(keyBytes, RijndaelCbc.BLOCK_SIZE)
            val decrypted = cipher.decrypt(inputBytes)

            writeFile(context, outputDirUri, outputName, decrypted, "application/octet-stream")
            Result.success(outputName)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Encrypted RTON → JSON (full pipeline: decrypt + parse RTON → JSON).
     */
    fun encryptedRtonToJson(
        context: Context,
        inputUri: Uri,
        outputDirUri: Uri,
        outputName: String,
        key: String
    ): Result<String> {
        return try {
            var inputBytes =
                context.contentResolver.openInputStream(inputUri)!!.use { it.readBytes() }
            // Strip 0x1000 header
            if (inputBytes.size >= 2 &&
                inputBytes[0] == Pvz2Crypto.ENCRYPTION_HEADER[0] &&
                inputBytes[1] == Pvz2Crypto.ENCRYPTION_HEADER[1]
            ) {
                inputBytes = inputBytes.copyOfRange(2, inputBytes.size)
            }
            val keyBytes = Pvz2Crypto.prepareKey(key)
            val cipher = RijndaelCbc(keyBytes, RijndaelCbc.BLOCK_SIZE)
            val decrypted = cipher.decrypt(inputBytes)

            val map = RtonParser.parse(decrypted)
            val jsonStr = gson.toJson(map)
            writeFile(
                context,
                outputDirUri,
                outputName,
                jsonStr.toByteArray(Charsets.UTF_8),
                "application/json"
            )
            Result.success(outputName)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * JSON → encrypted RTON (full pipeline: JSON → RTON → Rijndael encrypt → 0x1000).
     */
    fun jsonToEncryptedRton(
        context: Context,
        inputUri: Uri,
        outputDirUri: Uri,
        outputName: String,
        key: String
    ): Result<String> {
        return try {
            val inputStr = context.contentResolver.openInputStream(inputUri)!!.use {
                it.readBytes().toString(Charsets.UTF_8)
            }
            val jsonElement = JsonParser.parseString(inputStr)
            val rtonBinary = RtonEncoder.encode(jsonElement.asJsonObject)

            val keyBytes = Pvz2Crypto.prepareKey(key)
            val cipher = RijndaelCbc(keyBytes, RijndaelCbc.BLOCK_SIZE)
            val encrypted = cipher.encrypt(rtonBinary)
            val withHeader = Pvz2Crypto.ENCRYPTION_HEADER + encrypted

            writeFile(context, outputDirUri, outputName, withHeader, "application/octet-stream")
            Result.success(outputName)
        } catch (e: Exception) {
            Result.failure(e)
        }
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
