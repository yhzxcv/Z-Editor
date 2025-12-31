package com.example.pvz2leveleditor.data.repository

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import com.example.pvz2leveleditor.data.ObjectOrderRegistry
import com.example.pvz2leveleditor.data.PvzLevelFile
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import java.io.File
import androidx.core.net.toUri

object LevelRepository {
    private val gson: Gson = GsonBuilder().setPrettyPrinting().create()

    // 从 SharedPreferences 获取保存的文件夹 Uri
    private fun getRootUri(context: Context): Uri? {
        val uriString = context.getSharedPreferences("prefs", Context.MODE_PRIVATE)
            .getString("folder_uri", null)
        return uriString?.toUri()
    }

    fun copyLevelToTarget(context: Context, srcFileName: String, targetFileName: String): Boolean {
        val rootUri = getRootUri(context) ?: return false
        val rootDoc = DocumentFile.fromTreeUri(context, rootUri) ?: return false
        val srcFile = rootDoc.findFile(srcFileName) ?: return false

        // 检查目标文件名是否已存在
        if (rootDoc.findFile(targetFileName) != null) return false

        return try {
            val newFile = rootDoc.createFile("application/json", targetFileName) ?: return false
            context.contentResolver.openInputStream(srcFile.uri)?.use { input ->
                context.contentResolver.openOutputStream(newFile.uri)?.use { output ->
                    input.copyTo(output)
                }
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun renameLevel(context: Context, oldFileName: String, newFileName: String): Boolean {
        val rootUri = getRootUri(context) ?: return false
        val rootDoc = DocumentFile.fromTreeUri(context, rootUri) ?: return false
        val targetFile = rootDoc.findFile(oldFileName) ?: return false

        // 检查新名称是否已存在
        if (rootDoc.findFile(newFileName) != null) return false

        return try {
            // 1. 重命名外部文件
            val success = targetFile.renameTo(newFileName)
            if (success) {
                // 2. 同步重命名内部私有缓存文件，防止保存时找不到旧文件
                val oldInternal = File(context.filesDir, oldFileName)
                if (oldInternal.exists()) {
                    val newInternal = File(context.filesDir, newFileName)
                    oldInternal.renameTo(newInternal)
                }
            }
            success
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun getExternalLevelFiles(context: Context): List<String> {
        val rootUri = getRootUri(context) ?: return emptyList()
        val rootDoc = DocumentFile.fromTreeUri(context, rootUri) ?: return emptyList()

        return rootDoc.listFiles()
            .filter { it.isFile && it.name?.endsWith(".json", ignoreCase = true) == true }
            .mapNotNull { it.name }
    }

    fun prepareInternalCache(context: Context, fileName: String): Boolean {
        val rootUri = getRootUri(context) ?: return false
        val rootDoc = DocumentFile.fromTreeUri(context, rootUri) ?: return false
        val targetFile = rootDoc.findFile(fileName) ?: return false

        return try {
            context.contentResolver.openInputStream(targetFile.uri)?.use { input ->
                File(context.filesDir, fileName).outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun saveAndExport(context: Context, fileName: String, levelData: PvzLevelFile) {
        levelData.objects.sortWith(ObjectOrderRegistry.comparator)

        val internalFile = File(context.filesDir, fileName)
        internalFile.writer().use { gson.toJson(levelData, it) }

        val rootUri = getRootUri(context) ?: return
        val rootDoc = DocumentFile.fromTreeUri(context, rootUri) ?: return
        var targetFile = rootDoc.findFile(fileName)

        if (targetFile == null) {
            targetFile = rootDoc.createFile("application/json", fileName)
        }

        targetFile?.let { docFile ->
            try {
                context.contentResolver.openFileDescriptor(docFile.uri, "wt")?.use { pfd ->
                    java.io.FileOutputStream(pfd.fileDescriptor).use { out ->
                        val channel = out.channel
                        channel.truncate(0)
                        internalFile.inputStream().use { input ->
                            input.copyTo(out)
                        }
                        out.flush()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun deleteLevelCompletely(context: Context, fileName: String) {
        // 删外部
        val rootUri = getRootUri(context) ?: return
        val rootDoc = DocumentFile.fromTreeUri(context, rootUri) ?: return
        rootDoc.findFile(fileName)?.delete()

        // 删内部
        val internalFile = File(context.filesDir, fileName)
        if (internalFile.exists()) internalFile.delete()
    }

    fun loadLevel(context: Context, fileName: String): PvzLevelFile? {
        val file = File(context.filesDir, fileName)
        if (!file.exists()) return null
        return try {
            file.reader().use { gson.fromJson(it, PvzLevelFile::class.java) }
        } catch (_: Exception) {
            null
        }
    }

    fun getTemplateList(context: Context): List<String> {
        return try {
            // list 返回的是 String 数组
            context.assets.list("template")?.toList() ?: emptyList()
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    fun createLevelFromTemplate(
        context: Context,
        templateName: String,
        newFileName: String
    ): Boolean {
        val folderUriStr = context.getSharedPreferences("prefs", Context.MODE_PRIVATE)
            .getString("folder_uri", null) ?: return false

        try {
            val folderUri = folderUriStr.toUri()
            val folder = DocumentFile.fromTreeUri(context, folderUri)
                ?: return false

            if (folder.findFile(newFileName) != null) {
                return false
            }
            val assetContent = context.assets.open("template/$templateName").bufferedReader().use {
                it.readText()
            }
            val newFile = folder.createFile("application/json", newFileName) ?: return false
            context.contentResolver.openOutputStream(newFile.uri)?.use { output ->
                output.write(assetContent.toByteArray())
            }
            return true
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }
    }
}