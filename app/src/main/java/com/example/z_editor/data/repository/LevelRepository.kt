package com.example.z_editor.data.repository

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import com.example.z_editor.data.ObjectOrderRegistry
import com.example.z_editor.data.PvzLevelFile
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import java.io.File
import java.io.FileOutputStream

data class FileItem(
    val name: String,
    val uri: Uri,
    val isDirectory: Boolean,
    val lastModified: Long,
    val size: Long
)

object LevelRepository {
    private val gson: Gson = GsonBuilder().setPrettyPrinting().create()

    fun copyLevelToTarget(context: Context, srcFileName: String, targetFileName: String, currentDirUri: Uri): Boolean {
        val currentDoc = DocumentFile.fromTreeUri(context, currentDirUri) ?: return false
        val srcFile = currentDoc.findFile(srcFileName) ?: return false

        if (currentDoc.findFile(targetFileName) != null) return false

        return try {
            val newFile = currentDoc.createFile("application/json", targetFileName) ?: return false
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

    fun getDirectoryContents(context: Context, directoryUri: Uri): List<FileItem> {
        val docDir = DocumentFile.fromTreeUri(context, directoryUri) ?: return emptyList()

        return docDir.listFiles()
            .mapNotNull { file ->
                val name = file.name ?: return@mapNotNull null
                val isJson = !file.isDirectory && name.endsWith(".json", ignoreCase = true)
                val isDir = file.isDirectory

                if (isJson || isDir) {
                    FileItem(
                        name = name,
                        uri = file.uri,
                        isDirectory = isDir,
                        lastModified = file.lastModified(),
                        size = file.length()
                    )
                } else {
                    null
                }
            }
            .sortedWith(Comparator { o1, o2 ->
                if (o1.isDirectory != o2.isDirectory) {
                    if (o1.isDirectory) -1 else 1
                } else {
                    naturalOrderComparator.compare(o1.name, o2.name)
                }
            })
    }

    fun createDirectory(context: Context, parentUri: Uri, name: String): Boolean {
        val parentDoc = DocumentFile.fromTreeUri(context, parentUri) ?: return false
        if (parentDoc.findFile(name) != null) return false
        return try {
            parentDoc.createDirectory(name) != null
        } catch (_: Exception) {
            false
        }
    }

    fun renameItem(context: Context, currentDirUri: Uri, oldName: String, newName: String, isDirectory: Boolean): Boolean {
        val parentDoc = DocumentFile.fromTreeUri(context, currentDirUri) ?: return false
        val targetFile = parentDoc.findFile(oldName) ?: return false

        if (parentDoc.findFile(newName) != null) return false

        return try {
            val success = targetFile.renameTo(newName)

            if (success && !isDirectory) {
                val oldInternal = File(context.filesDir, oldName)
                if (oldInternal.exists()) {
                    val newInternal = File(context.filesDir, newName)
                    oldInternal.renameTo(newInternal)
                }
            }
            success
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun deleteItem(context: Context, currentDirUri: Uri, fileName: String, isDirectory: Boolean) {
        val parentDoc = DocumentFile.fromTreeUri(context, currentDirUri) ?: return
        val target = parentDoc.findFile(fileName)
        target?.delete()

        if (!isDirectory) {
            val internalFile = File(context.filesDir, fileName)
            if (internalFile.exists()) internalFile.delete()
        }
    }

    fun moveFile(context: Context, srcParentUri: Uri, srcName: String, destParentUri: Uri): Boolean {
        val srcDir = DocumentFile.fromTreeUri(context, srcParentUri) ?: return false
        val destDir = DocumentFile.fromTreeUri(context, destParentUri) ?: return false

        val srcFile = srcDir.findFile(srcName) ?: return false

        if (destDir.findFile(srcName) != null) return false

        var newFile: DocumentFile? = null
        try {
            newFile = destDir.createFile("application/json", srcName) ?: return false

            context.contentResolver.openInputStream(srcFile.uri)?.use { input ->
                context.contentResolver.openOutputStream(newFile.uri)?.use { output ->
                    input.copyTo(output)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            newFile?.delete()
            return false
        }
        if (srcFile.delete()) {
            val internalFile = File(context.filesDir, srcName)
            if (internalFile.exists()) internalFile.delete()
            return true
        } else {
            return true
        }
    }

    fun clearAllInternalCache(context: Context): Int {
        val dir = context.filesDir
        var deletedCount = 0
        dir.listFiles()?.forEach { file ->
            if (file.isFile && file.name.endsWith(".json", ignoreCase = true)) {
                if (file.delete()) {
                    deletedCount++
                }
            }
        }
        return deletedCount
    }

    fun prepareInternalCache(context: Context, fileUri: Uri, fileName: String): Boolean {
        return try {
            context.contentResolver.openInputStream(fileUri)?.use { input ->
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

    fun saveAndExport(context: Context, fileUri: Uri, fileName: String, levelData: PvzLevelFile) {
        levelData.objects.sortWith(ObjectOrderRegistry.comparator)

        val internalFile = File(context.filesDir, fileName)
        internalFile.writer().use { gson.toJson(levelData, it) }

        try {
            context.contentResolver.openFileDescriptor(fileUri, "wt")?.use { pfd ->
                FileOutputStream(pfd.fileDescriptor).use { out ->
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

    fun createLevelFromTemplate(
        context: Context,
        currentDirUri: Uri,
        templateName: String,
        newFileName: String
    ): Boolean {
        try {
            val folder = DocumentFile.fromTreeUri(context, currentDirUri) ?: return false

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
            context.assets.list("reference/template")?.toList() ?: emptyList()
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    val naturalOrderComparator = Comparator<String> { s1, s2 ->
        var i1 = 0
        var i2 = 0
        while (i1 < s1.length && i2 < s2.length) {
            val c1 = s1[i1]
            val c2 = s2[i2]
            if (c1.isDigit() && c2.isDigit()) {
                var num1 = 0L
                while (i1 < s1.length && s1[i1].isDigit()) {
                    num1 = num1 * 10 + (s1[i1] - '0')
                    i1++
                }
                var num2 = 0L
                while (i2 < s2.length && s2[i2].isDigit()) {
                    num2 = num2 * 10 + (s2[i2] - '0')
                    i2++
                }
                if (num1 != num2) return@Comparator num1.compareTo(num2)
            } else {
                if (c1 != c2) return@Comparator c1.compareTo(c2)
                i1++
                i2++
            }
        }
        s1.length - s2.length
    }
}