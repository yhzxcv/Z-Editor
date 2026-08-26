package com.example.z_editor.data.repository

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
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

// 模板条目：name 含扩展名；isBuiltIn=true 表示 assets/reference/template/ 里的只读模板，false 表示用户自建模板
data class TemplateEntry(
    val name: String,
    val isBuiltIn: Boolean
)

object LevelRepository {
    private val gson: Gson = GsonBuilder().setPrettyPrinting().create()

    private fun getInternalCacheName(fileUri: Uri): String {
        return "cache_${fileUri.hashCode()}.json"
    }

    fun prepareInternalCache(context: Context, fileUri: Uri): String? {
        val cacheName = getInternalCacheName(fileUri)
        return try {
            context.contentResolver.openInputStream(fileUri)?.use { input ->
                File(context.filesDir, cacheName).outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            cacheName
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }


    private fun getMimeType(fileName: String): String {
        return when {
            fileName.endsWith(".json", ignoreCase = true) -> "application/json"
            else -> "*/*"
        }
    }

    fun copyLevelToTarget(context: Context, srcFileName: String, targetFileName: String, currentDirUri: Uri): Boolean {
        val currentDoc = DocumentFile.fromTreeUri(context, currentDirUri) ?: return false
        val srcFile = currentDoc.findFile(srcFileName) ?: return false

        if (currentDoc.findFile(targetFileName) != null) return false

        return try {
            val newFile = currentDoc.createFile(getMimeType(targetFileName), targetFileName) ?: return false
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
            newFile = destDir.createFile(getMimeType(srcName), srcName) ?: return false

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

    fun saveAndExport(context: Context, fileUri: Uri, cacheName: String, levelData: PvzLevelFile) {
        val internalFile = File(context.filesDir, cacheName)

        levelData.objects.sortWith(ObjectOrderRegistry.comparator)
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
        template: TemplateEntry,
        newFileName: String
    ): Boolean {
        try {
            val folder = DocumentFile.fromTreeUri(context, currentDirUri) ?: return false

            if (folder.findFile(newFileName) != null) {
                return false
            }
            val content = readTemplateContent(context, template) ?: return false
            val newFile = folder.createFile("application/json", newFileName) ?: return false
            context.contentResolver.openOutputStream(newFile.uri)?.use { output ->
                output.write(content.toByteArray())
            }
            return true
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }
    }

    fun loadLevel(context: Context, cacheName: String): PvzLevelFile? {
        val file = File(context.filesDir, cacheName)
        if (!file.exists()) return null
        return try {
            file.reader().use { gson.fromJson(it, PvzLevelFile::class.java) }
        } catch (_: Exception) {
            null
        }
    }

    fun getTemplateList(context: Context): List<TemplateEntry> {
        val userTemplates = getUserTemplateList(context).map { TemplateEntry(it, isBuiltIn = false) }
        val builtInTemplates = try {
            context.assets.list("reference/template")
                ?.sortedWith(naturalOrderComparator)
                ?.map { TemplateEntry(it, isBuiltIn = true) }
                ?: emptyList()
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
        return userTemplates + builtInTemplates
    }

    // ==================== 用户自建模板（应用内部存储 filesDir/templates/） ====================

    private fun getUserTemplateDir(context: Context): File {
        val dir = File(context.filesDir, "templates")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    fun getUserTemplateList(context: Context): List<String> {
        return getUserTemplateDir(context).listFiles()
            ?.filter { it.isFile && it.name.endsWith(".json", ignoreCase = true) }
            ?.map { it.name }
            ?.sortedWith(naturalOrderComparator)
            ?: emptyList()
    }

    fun readTemplateContent(context: Context, template: TemplateEntry): String? {
        return try {
            if (template.isBuiltIn) {
                context.assets.open("reference/template/${template.name}").bufferedReader().use {
                    it.readText()
                }
            } else {
                val file = File(getUserTemplateDir(context), template.name)
                if (file.exists()) file.readText() else null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * 为模板名去重：已存在同名时按 `foo.json → foo~.json → foo~~.json` 递增后缀。
     * 纯函数，便于单元测试。
     */
    fun resolveUniqueTemplateName(existing: List<String>, desired: String): String {
        if (desired !in existing) return desired
        val dotIndex = desired.lastIndexOf('.')
        val base = if (dotIndex > 0) desired.substring(0, dotIndex) else desired
        val ext = if (dotIndex > 0) desired.substring(dotIndex) else ""
        var name = "$base~"
        while ("$name$ext" in existing) {
            name += "~"
        }
        return "$name$ext"
    }

    /**
     * 从 SAF Uri 读取显示名；失败回退到 uri.lastPathSegment。
     */
    private fun getDisplayNameFromUri(context: Context, uri: Uri): String? {
        return try {
            context.contentResolver.query(
                uri,
                arrayOf(OpenableColumns.DISPLAY_NAME),
                null,
                null,
                null
            )?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (index != -1) cursor.getString(index) else null
                } else {
                    null
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            uri.lastPathSegment
        }
    }

    /**
     * 导入自建关卡为模板：拷入 filesDir/templates/，重名自动去重。返回最终文件名，失败返回 null。
     */
    fun importUserTemplate(context: Context, sourceUri: Uri): String? {
        return try {
            var fileName = getDisplayNameFromUri(context, sourceUri) ?: return null
            if (!fileName.endsWith(".json", ignoreCase = true)) fileName += ".json"

            val finalName = resolveUniqueTemplateName(getUserTemplateList(context), fileName)
            val target = File(getUserTemplateDir(context), finalName)
            context.contentResolver.openInputStream(sourceUri)?.use { input ->
                target.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            finalName
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun deleteUserTemplate(context: Context, name: String): Boolean {
        val file = File(getUserTemplateDir(context), name)
        return if (file.exists()) {
            file.delete()
        } else {
            false
        }
    }

    fun renameUserTemplate(context: Context, oldName: String, newName: String): Boolean {
        var finalName = newName.trim()
        if (!finalName.endsWith(".json", ignoreCase = true)) finalName += ".json"
        if (finalName == oldName) return true
        if (getUserTemplateList(context).any { it == finalName }) return false

        val dir = getUserTemplateDir(context)
        val oldFile = File(dir, oldName)
        if (!oldFile.exists()) return false
        return try {
            oldFile.renameTo(File(dir, finalName))
        } catch (e: Exception) {
            e.printStackTrace()
            false
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