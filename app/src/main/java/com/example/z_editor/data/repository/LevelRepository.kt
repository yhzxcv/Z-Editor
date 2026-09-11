package com.example.z_editor.data.repository

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.DocumentsContract
import android.provider.OpenableColumns
import androidx.core.content.edit
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

/**
 * 创建类操作的结果。
 *
 * 之前这些方法只返回 Boolean，UI 只好弹一句无差别的"创建失败，已有同名文件"；
 * 而实际失败原因可能是重名、模板读不到、provider 拒绝创建、写流拿不到。
 * 在鸿蒙等第三方 DocumentsProvider 上，这个误导性文案会把"provider 拒绝创建"
 * 误报成"重名"，把排查方向带偏。
 */
sealed interface FileOpResult {
    /** 操作成功 */
    data object Success : FileOpResult

    /** 前置检查命中：目标名确实已存在 */
    data object NameExists : FileOpResult

    /** 失败，且当前目录没有持久化写权限（部分 ROM 的文件选择器只授予读） */
    data object NoWritePermission : FileOpResult

    /** 其他失败；cause 为底层异常摘要，便于用户截图反馈 */
    data class Failure(val cause: String?) : FileOpResult
}

/**
 * 存储模式。
 *
 * - [Saf] 走系统文件访问框架。**缺省值**，所有机型的历史行为。
 * - [Raw] 把目录换算回真实路径，用 `java.io.File` 直接读写，完全绕开 provider。
 *   用于鸿蒙这类能列目录、却拒绝一切写入的 provider。
 *
 * 持久化用的是 `enum.name`，改常量名等于把所有用户的设置清空，别改。
 */
enum class StorageMode { Saf, Raw }

object LevelRepository {
    private val gson: Gson = GsonBuilder().setPrettyPrinting().create()

    // 复刻 provider 侧的 createDocument 协议值。DocumentsContract.METHOD_CREATE_DOCUMENT 与
    // EXTRA_URI 都是 @hide（android.jar 里没有），只能写字面量。这个协议自 API 19 起没变过，
    // 而且只在创建已经失败之后用来捞原因——值若变了也只是拿不到原因，不影响正常创建路径。
    private const val PROVIDER_METHOD_CREATE_DOCUMENT = "android:createDocument"
    private const val PROVIDER_EXTRA_URI = "uri"

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

    fun copyLevelToTarget(context: Context, srcFileName: String, targetFileName: String, currentDirUri: Uri): Boolean =
        runWithRawFallback(
            context,
            currentDirUri,
            safAttempt = { copyLevelViaSaf(context, srcFileName, targetFileName, currentDirUri) },
            rawAttempt = { copyViaRawPath(context, currentDirUri, srcFileName, targetFileName) }
        )

    private fun copyLevelViaSaf(
        context: Context,
        srcFileName: String,
        targetFileName: String,
        currentDirUri: Uri
    ): Boolean {
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
        // 直写模式（或句柄本身就是 file://）完全绕开 provider。
        // 必须在碰 DocumentFile 之前分流：fromTreeUri 对 file:// 会抛 IllegalArgumentException。
        val preferRaw = shouldUseRawPath(
            readStorageMode(context), hasRawStoragePermission(), directoryUri.scheme
        )
        if (preferRaw) return rawDirectoryContents(context, directoryUri)

        val docDir = DocumentFile.fromTreeUri(context, directoryUri) ?: return emptyList()

        return docDir.listFiles()
            .mapNotNull { file ->
                val name = file.name ?: return@mapNotNull null
                val isDir = file.isDirectory
                if (!isListableEntry(name, isDir)) return@mapNotNull null

                FileItem(
                    name = name,
                    uri = file.uri,
                    isDirectory = isDir,
                    lastModified = file.lastModified(),
                    size = file.length()
                )
            }
            .sortedWith(LIST_ORDER)
    }

    /** 纯函数：目录条目是否展示 —— 文件夹，或 .json 文件。两条分支共用，保证口径一致。 */
    fun isListableEntry(name: String, isDirectory: Boolean): Boolean =
        isDirectory || name.endsWith(".json", ignoreCase = true)

    /** 文件夹在前，同类按自然序（`1` 排在 `10` 前）。两条分支共用。 */
    private val LIST_ORDER: Comparator<FileItem> = Comparator { o1, o2 ->
        if (o1.isDirectory != o2.isDirectory) {
            if (o1.isDirectory) -1 else 1
        } else {
            naturalOrderComparator.compare(o1.name, o2.name)
        }
    }

    /** 直写模式的列目录：读真实路径，句柄是 file://。换算不出（云盘）时返回空列表。 */
    private fun rawDirectoryContents(context: Context, directoryUri: Uri): List<FileItem> {
        val dir = rawDirFor(context, directoryUri) ?: return emptyList()
        return dir.listFiles().orEmpty()
            .mapNotNull { child ->
                val name = child.name ?: return@mapNotNull null
                val isDir = child.isDirectory
                if (!isListableEntry(name, isDir)) return@mapNotNull null

                FileItem(
                    name = name,
                    uri = Uri.fromFile(child),
                    isDirectory = isDir,
                    lastModified = child.lastModified(),
                    // 与 SAF 分支口径一致：目录不报大小
                    size = if (isDir) 0L else child.length()
                )
            }
            .sortedWith(LIST_ORDER)
    }

    fun createDirectory(context: Context, parentUri: Uri, name: String): FileOpResult {
        // 直写模式：完全绕开 provider。必须在碰 DocumentFile 之前分流，
        // 否则 file:// 句柄会让 fromTreeUri 抛 IllegalArgumentException。
        if (shouldUseRawPath(readStorageMode(context), hasRawStoragePermission(), parentUri.scheme)) {
            val dir = rawDirFor(context, parentUri)
                ?: return appendRawHint(context, FileOpResult.Failure(null), parentUri)
            if (File(dir, name).exists()) return FileOpResult.NameExists
            if (createDirectoryViaRawPath(context, parentUri, name)) return FileOpResult.Success
            return appendRawHint(context, FileOpResult.Failure(null), parentUri)
        }

        val parentDoc = DocumentFile.fromTreeUri(context, parentUri)
            ?: return FileOpResult.Failure("无法访问目标目录")
        val result =
            createResilient(context, parentDoc, name, DocumentsContract.Document.MIME_TYPE_DIR)
        if (result == FileOpResult.Success) return result
        // 重名是确定的结论：真实路径下那个文件夹本来就在，再试一次会被误判成"创建成功"
        if (result == FileOpResult.NameExists) return result
        if (createDirectoryViaRawPath(context, parentUri, name)) return FileOpResult.Success
        return appendRawHint(context, result, parentUri)
    }

    /**
     * 韧性创建：失败后回查目录，并对失败原因做归因。
     *
     * 与 `DocumentFile.createDirectory/createFile` 的两点差别：
     * 1. 直接调 `DocumentsContract.createDocument`。`TreeDocumentFile.createFile` 会
     *    `catch (Exception) { return null }`，把异常吞成 null，实机上因此只能看到
     *    "返回 null"而看不到原因；直接用框架 API 至少能让 FileNotFoundException 抛出。
     * 2. 失败后回查 `findFile`，覆盖华为/鸿蒙上"文件其实已建、但 createDocument 返回 null"的已知 bug。
     */
    private fun createResilient(
        context: Context,
        parent: DocumentFile,
        name: String,
        mimeType: String
    ): FileOpResult {
        val existedBefore = parent.findFile(name) != null
        // 提前返回而不是交给 classifyCreateOutcome：provider 遇到重名会自动改名成
        // "名字 (1)"，先创建再判重名会凭空多出一个文件
        if (existedBefore) return FileOpResult.NameExists

        var cause: String? = null
        val createdUri = try {
            DocumentsContract.createDocument(context.contentResolver, parent.uri, mimeType, name)
        } catch (e: Exception) {
            e.printStackTrace()
            cause = describeException(e)
            null
        }

        var existsAfter = parent.findFile(name) != null

        // 第二选择：往 children URI 上 insert，由 provider 自己建文档
        if (createdUri == null && !existsAfter) {
            val insertedUri = try {
                insertCreate(context, parent.uri, name, mimeType)
            } catch (e: Exception) {
                // 标准 DocumentsProvider 上必然抛 UnsupportedOperationException，
                // 属于预期内的失败，不往 cause 里塞以免盖住更有用的信息
                e.printStackTrace()
                null
            }
            if (insertedUri != null) return FileOpResult.Success
            existsAfter = parent.findFile(name) != null
        }

        // 两条路都落空：确实没建出来。框架把真实原因抹成了 null，
        // 再补两路证据——provider 对当前目录声明的能力位，和复刻调用捞出的异常
        if (createdUri == null && !existsAfter) {
            val flags = readDocumentFlags(context, parent.uri)
            val isDirectory = mimeType == DocumentsContract.Document.MIME_TYPE_DIR
            cause = describeCreateFailure(
                canWrite = flags?.let { it and DocumentsContract.Document.FLAG_SUPPORTS_WRITE != 0 },
                canCreateDir = if (!isDirectory) null
                else flags?.let { it and DocumentsContract.Document.FLAG_DIR_SUPPORTS_CREATE != 0 },
                cause = cause ?: harvestCreateCause(context, parent.uri, name, mimeType)
            )
            existsAfter = parent.findFile(name) != null
        }

        return classifyCreateOutcome(
            existedBefore = existedBefore,
            created = createdUri != null,
            existsAfter = existsAfter,
            hasWritePermission = hasPersistedWritePermission(context, parent.uri),
            cause = cause
        )
    }

    /**
     * 老式插入路径：`resolver.insert` 到 children URI，让 provider 自己建文档。
     *
     * AOSP 的 `DocumentsProvider` 把 `insert` 声明成 `final` 并直接抛
     * `UnsupportedOperationException`（android-35 源码 DocumentsProvider.java:1051），
     * 所以这条只在**不是**标准 DocumentsProvider 的 provider 上才有戏。
     * 而"目录能列出、创建一律被拒"恰好就是那种 provider 的形态：它实现了 `query`，
     * 却没实现 API 26 起 `DocumentsContract.createDocument` 所走的 `android:createDocument` 协议。
     */
    private fun insertCreate(
        context: Context,
        parentUri: Uri,
        name: String,
        mimeType: String
    ): Uri? {
        val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(
            parentUri, DocumentsContract.getDocumentId(parentUri)
        )
        val values = ContentValues().apply {
            put(DocumentsContract.Document.COLUMN_DISPLAY_NAME, name)
            put(DocumentsContract.Document.COLUMN_MIME_TYPE, mimeType)
        }
        return context.contentResolver.insert(childrenUri, values)
    }

    /**
     * 读取 provider 对当前目录声明的能力位（COLUMN_FLAGS）。查询失败返回 null。
     *
     * 只用于失败后归因，不做前置拦截：部分 provider 不老实上报这些位，
     * 前置拦截会误伤本来能写的机型。
     */
    private fun readDocumentFlags(context: Context, docUri: Uri): Int? {
        return try {
            context.contentResolver.query(
                docUri,
                arrayOf(DocumentsContract.Document.COLUMN_FLAGS),
                null,
                null,
                null
            )?.use { cursor ->
                if (cursor.moveToFirst() && !cursor.isNull(0)) cursor.getInt(0) else null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * 复刻一次 provider 的 `android:createDocument` 调用，把被框架吞掉的异常捞出来。
     *
     * `DocumentsContract.createDocument` 的 `catch (Exception) { return null }` 会把
     * SecurityException、RemoteException、provider 侧 NPE、FileNotFoundException 一律抹成 null，
     * 实机上因此只看得到"返回 null"，无从判断到底是权限被拒、provider 不支持还是 provider 崩了。
     *
     * 返回 null 表示"调用成功"或"没捞到原因"，两者都无需区分：
     * 调用成功的话，调用方随后的 findFile 回查会认出文件已经存在。
     */
    private fun harvestCreateCause(
        context: Context,
        parentUri: Uri,
        name: String,
        mimeType: String
    ): String? {
        val authority = parentUri.authority ?: return null
        val extras = Bundle().apply {
            putParcelable(PROVIDER_EXTRA_URI, parentUri)
            putString(DocumentsContract.Document.COLUMN_MIME_TYPE, mimeType)
            putString(DocumentsContract.Document.COLUMN_DISPLAY_NAME, name)
        }
        return try {
            val out = context.contentResolver.call(
                authority, PROVIDER_METHOD_CREATE_DOCUMENT, null, extras
            )
            if (out == null) {
                "provider 未应答 android:createDocument"
            } else {
                @Suppress("DEPRECATION")
                val created = out.getParcelable<Uri>(PROVIDER_EXTRA_URI)
                if (created == null) "provider 未返回新文档 uri" else null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            describeException(e)
        }
    }

    // ==================== 原始路径（直写）模式 ====================
    //
    // 部分第三方 provider 能列出目录，却拒绝一切走 android:createDocument /
    // android:renameDocument / android:deleteDocument 这些 call 协议的写操作
    // （鸿蒙实机已确认：目录能列出，创建在任何目录、任何名字下都被拒）。
    // 这类 provider 没法通过 SAF 写入，只能把 URI 换算回真实路径、用 java.io.File 直接读写。
    //
    // 应用已声明 MANAGE_EXTERNAL_STORAGE（AndroidManifest.xml:9），与 SmfUnpackerScreen /
    // BatchConvertScreen 用的是同一套门禁，**不需要新增权限**。
    //
    // 用户在「存储设置」页显式开关这个模式（缺省关闭）。关闭时所有兜底一律在对应的
    // SAF 操作**已经失败之后**才尝试，正常机型上不会执行到。

    const val PREFS_NAME = "prefs"
    const val KEY_STORAGE_MODE = "storage_mode"
    const val SCHEME_FILE = "file"
    const val SCHEME_CONTENT = "content"

    /** 可移动存储卷的 volumeId 形如 1A2B-3C4D（FS_UUID）。 */
    private val REMOVABLE_VOLUME_ID = Regex("[0-9A-Fa-f]{4}-[0-9A-Fa-f]{4}")

    /**
     * 是否已获得「所有文件访问」。UI 的开关与仓库的门禁共用这一个判定，
     * 避免出现"界面说已开启、实际走不通"的错位。
     */
    fun hasRawStoragePermission(): Boolean =
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && Environment.isExternalStorageManager()

    /** 纯函数：prefs 里的字符串 → 模式。任何不认识的值（含 null）都当 SAF。 */
    fun parseStorageMode(raw: String?): StorageMode =
        if (raw == StorageMode.Raw.name) StorageMode.Raw else StorageMode.Saf

    /**
     * 纯函数：模式经权限归一化。权限没了就必须退回 SAF —— 运行时降级，
     * 而不是让写入在 `File.canWrite()` 返回 false 时才莫名其妙地失败。
     */
    fun normalizeStorageMode(mode: StorageMode, hasRawPermission: Boolean): StorageMode =
        if (mode == StorageMode.Raw && !hasRawPermission) StorageMode.Saf else mode

    /**
     * 纯函数：本次操作是否走真实路径。
     *
     * `file://` 一律返回 true，**这条是必须的**：`DocumentFile.fromTreeUri` 在判断
     * scheme 之前就会无条件调用 `DocumentsContract.getTreeDocumentId`，而后者对首段不是
     * "tree" 的 URI 直接抛 IllegalArgumentException（android-35 DocumentsContract.java:1289，
     * documentfile 1.1.0 DocumentFile.java:133）。把 file:// 交给任何 SAF 分支就是主线程崩溃。
     */
    fun shouldUseRawPath(
        mode: StorageMode,
        hasRawPermission: Boolean,
        uriScheme: String?
    ): Boolean = uriScheme == SCHEME_FILE || normalizeStorageMode(mode, hasRawPermission) == StorageMode.Raw

    /**
     * 纯函数：该 scheme 能否交给 DocumentFile/SAF。
     *
     * 判等 "content" 而不是 `!= "file"`：`getTreeDocumentId` 对任何非树 URI 都抛，
     * 而 ACTION_OPEN_DOCUMENT_TREE 只会返回 content:// 树 URI。
     */
    fun isSafHandledScheme(uriScheme: String?): Boolean = uriScheme == SCHEME_CONTENT

    fun readStorageMode(context: Context): StorageMode =
        parseStorageMode(
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getString(KEY_STORAGE_MODE, null)
        )

    fun writeStorageMode(context: Context, mode: StorageMode) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit { putString(KEY_STORAGE_MODE, mode.name) }
    }

    /** 实际生效的模式 = 已存模式归一化之后的结果。UI 与仓库都读它。 */
    fun effectiveStorageMode(context: Context): StorageMode =
        normalizeStorageMode(readStorageMode(context), hasRawStoragePermission())

    /**
     * 原始路径写入是否可用。Android 11 以下拿不到「所有文件访问」，
     * 直接放弃（那些版本上 SAF 本来就是好的，用不到）。
     */
    private fun canUseRawPath(context: Context): Boolean =
        effectiveStorageMode(context) == StorageMode.Raw

    /**
     * 把 document id 换算成真实路径。
     *
     * 只认 ExternalStorageProvider 风格的 `<volumeId>:<相对路径>` 与 Downloads 的
     * `raw:<绝对路径>`；云盘等 provider 的 opaque id 一律返回 null。
     *
     * `primaryRoot` 由调用方传入（`Environment.getExternalStorageDirectory()`），
     * 这样这个函数不碰 android.*，能在 src/test 里直接测——项目没有 Robolectric。
     */
    fun resolveRealPath(documentId: String, primaryRoot: File): File? {
        val separator = documentId.indexOf(':')
        if (separator <= 0) return null
        val volume = documentId.substring(0, separator)
        val relative = documentId.substring(separator + 1)
        // provider 给的 id 原则上可信，但拼真实路径前仍然挡掉 ".."，不让它跳出授权目录
        if (relative.split('/').any { it == ".." }) return null
        return when {
            volume == "raw" -> relative.takeIf { it.startsWith("/") }?.let { File(it) }
            volume == "primary" -> File(primaryRoot, relative)
            REMOVABLE_VOLUME_ID.matches(volume) -> File("/storage/$volume", relative)
            else -> null
        }
    }

    /**
     * 文件名是否可以直接拼进真实路径。挡住路径分隔符和 "."/".."，
     * 否则 "a/b" 会写到子目录、".." 会逃出授权目录。
     */
    fun isSafeFileName(name: String): Boolean {
        if (name.isEmpty() || name == "." || name == "..") return false
        return name.none { it == '/' || it == '\\' || it.code < 32 }
    }

    /** 从 tree URI（2 段）或子 document URI（4 段）里取 document id，两种形态都要认。 */
    private fun documentIdOf(uri: Uri): String? = try {
        DocumentsContract.getDocumentId(uri)
    } catch (_: IllegalArgumentException) {
        runCatching { DocumentsContract.getTreeDocumentId(uri) }.getOrNull()
    }

    /**
     * URI → 真实路径。
     *
     * `file://` 直接读 `uri.path`（与 `Uri.fromFile` 严格对称：`getPath()` 返回的是已解码值，
     * 不会再解码一次，含空格和 % 都无损）；`content://` 走 document id 换算。
     */
    private fun realPathOf(uri: Uri): File? =
        if (uri.scheme == SCHEME_FILE) {
            uri.path?.let { File(it) }
        } else {
            documentIdOf(uri)?.let { resolveRealPath(it, Environment.getExternalStorageDirectory()) }
        }

    /** 直写用的真实目录；模式未开启、不可换算、不存在或不可写时返回 null。 */
    private fun rawDirFor(context: Context, dirUri: Uri): File? {
        if (!canUseRawPath(context)) return null
        val dir = realPathOf(dirUri) ?: return null
        return if (dir.isDirectory && dir.canWrite()) dir else null
    }

    /** 单个文件的真实路径，用于保存关卡。 */
    private fun rawFileFor(context: Context, fileUri: Uri): File? {
        if (!canUseRawPath(context)) return null
        return realPathOf(fileUri)
    }

    /**
     * 直写没能生效的原因。UI 把它拼进失败提示，用户才知道下一步该做什么
     * ——否则只能看到"创建失败"，无从下手。
     */
    private fun rawPathHint(context: Context, dirUri: Uri): String? {
        if (!hasRawStoragePermission()) return "可开启「所有文件访问」后重试"
        if (readStorageMode(context) != StorageMode.Raw) return "可在「存储设置」中改用直写模式"
        val dir = realPathOf(dirUri) ?: return "该目录无法换算为本地路径"
        if (!dir.isDirectory) return "本地路径不存在"
        if (!dir.canWrite()) return "本地路径不可写"
        return null
    }

    /** 兜底也失败时，把"为什么兜底没成"补进原因里。 */
    private fun appendRawHint(context: Context, result: FileOpResult, dirUri: Uri): FileOpResult {
        if (result !is FileOpResult.Failure) return result
        val hint = rawPathHint(context, dirUri) ?: return result
        return FileOpResult.Failure(
            listOfNotNull(result.cause?.takeIf { it.isNotBlank() }, hint).joinToString("；")
        )
    }

    /**
     * 统一的「直写 / SAF」执行器。
     *
     * - 模式要求直写（或 uri 本身就是 file://）→ 先走真实路径；换算不出来（云盘）或失败时
     *   **退回 SAF**，否则在云盘目录下开了直写会静默什么都不做（deleteItem 会吞掉返回值）。
     * - 否则维持原样：先 SAF，失败了再兜底到真实路径。
     *
     * `safAttempt` 只会在 scheme 是 content:// 时被调用 —— `DocumentFile.fromTreeUri` 对
     * file:// 会抛 IllegalArgumentException（见 shouldUseRawPath 的注释）。
     */
    private fun runWithRawFallback(
        context: Context,
        vararg dirUris: Uri,
        safAttempt: () -> Boolean,
        rawAttempt: () -> Boolean
    ): Boolean {
        val mode = readStorageMode(context)
        val hasPermission = hasRawStoragePermission()
        // 移动会同时碰源目录和目标目录，两个都得看：只判一个的话，
        // 另一个是 file:// 时 safAttempt 仍会拿到它并让 fromTreeUri 抛异常。
        val preferRaw = dirUris.any { shouldUseRawPath(mode, hasPermission, it.scheme) }
        val safHandlesAll = dirUris.all { isSafHandledScheme(it.scheme) }

        if (preferRaw) {
            if (rawAttempt()) return true
            return safHandlesAll && safAttempt()
        }
        if (safHandlesAll && safAttempt()) return true
        return rawAttempt()
    }

    /**
     * 直写模式下把目录 uri 换算成 `file://` uri，供列表页当句柄用。
     * 模式未开启、没有权限或换算不出真实路径（云盘）时返回 null。
     */
    fun rawDirectoryUri(context: Context, dirUri: Uri): Uri? {
        if (effectiveStorageMode(context) != StorageMode.Raw) return null
        return rawDirFor(context, dirUri)?.let { Uri.fromFile(it) }
    }

    /**
     * 供设置页在**开启直写之前**试探：这个目录在直写模式下能不能用。
     *
     * 与 [rawDirectoryUri] 的区别是这里只看权限、不看当前模式 —— 开关还没打开时有效模式
     * 是 SAF，拿 `rawDirectoryUri` 判断会恒为 null，开关就永远打不开了。
     */
    fun isRawPathUsable(dirUri: Uri): Boolean {
        if (!hasRawStoragePermission()) return false
        val dir = realPathOf(dirUri) ?: return false
        return dir.isDirectory && dir.canWrite()
    }

    /**
     * 把用户选的目录 uri 换算成真实路径字符串，供只认 `java.io.File` 的写入方使用
     * （SMF 解包把结果写进 `FileOutputStream`，本来就不经过 provider）。
     *
     * **不要求持久化权限**：调用方只在这一刻读一次路径，之后存的是字符串。因此也不必
     * 把 tree uri 存起来 —— 未持久化的 uri 重启后就是死的，存它反而是个陷阱。
     */
    fun realPathStringOf(dirUri: Uri): String? = realPathOf(dirUri)?.absolutePath

    /**
     * 纯函数：`target` 是否就是 `base` 本身或位于 `base` 之下。
     *
     * 解包产物写成 `<输出目录>/<模板名>/`，所以「选在关卡库目录下面」同样会把成千上万个
     * 文件塞进关卡库、让主界面列表渲染失败，一律要拦。
     *
     * 按路径分段比较而不是 `startsWith`，否则 `/a/foo` 会被 `/a/fo` 误判成子目录。
     */
    fun isSameOrInsidePath(target: String, base: String): Boolean {
        fun segments(path: String) = path.trimEnd('/').split('/').filter { it.isNotEmpty() }
        val t = segments(target)
        val b = segments(base)
        if (b.isEmpty() || t.size < b.size) return false
        return t.subList(0, b.size) == b
    }

    /**
     * 目录显示名。`file://` 走 `java.io.File` —— `DocumentFile` 对 file scheme 会抛异常，不能碰。
     */
    fun directoryDisplayName(context: Context, dirUri: Uri): String =
        if (dirUri.scheme == SCHEME_FILE) {
            dirUri.path?.let { File(it).name }?.takeIf { it.isNotBlank() } ?: dirUri.path.orEmpty()
        } else {
            DocumentFile.fromTreeUri(context, dirUri)?.name.orEmpty()
        }

    private fun createDirectoryViaRawPath(context: Context, dirUri: Uri, name: String): Boolean {
        val dir = rawDirFor(context, dirUri) ?: return false
        if (!isSafeFileName(name)) return false
        val target = File(dir, name)
        // 已存在就不算创建成功，否则重名会被报成"创建成功"
        if (target.exists()) return false
        return try {
            target.mkdir()
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /** 建文件并写内容一次做完——写失败要删掉半截文件，别留下打不开的关卡。 */
    private fun writeFileViaRawPath(
        context: Context,
        dirUri: Uri,
        name: String,
        content: String
    ): Boolean {
        val dir = rawDirFor(context, dirUri) ?: return false
        if (!isSafeFileName(name)) return false
        val target = File(dir, name)
        if (target.exists()) return false
        return try {
            target.writeText(content)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            target.delete()
            false
        }
    }

    private fun renameViaRawPath(
        context: Context,
        dirUri: Uri,
        oldName: String,
        newName: String
    ): Boolean {
        val dir = rawDirFor(context, dirUri) ?: return false
        if (!isSafeFileName(oldName) || !isSafeFileName(newName)) return false
        val src = File(dir, oldName)
        val dest = File(dir, newName)
        if (!src.exists() || dest.exists()) return false
        return try {
            src.renameTo(dest)
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    private fun deleteViaRawPath(
        context: Context,
        dirUri: Uri,
        name: String,
        isDirectory: Boolean
    ): Boolean {
        val dir = rawDirFor(context, dirUri) ?: return false
        if (!isSafeFileName(name)) return false
        val target = File(dir, name)
        if (!target.exists()) return false
        return try {
            if (isDirectory) target.deleteRecursively() else target.delete()
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    private fun copyViaRawPath(
        context: Context,
        dirUri: Uri,
        srcName: String,
        targetName: String
    ): Boolean {
        val dir = rawDirFor(context, dirUri) ?: return false
        if (!isSafeFileName(srcName) || !isSafeFileName(targetName)) return false
        val src = File(dir, srcName)
        val dest = File(dir, targetName)
        if (!src.isFile || dest.exists()) return false
        return try {
            src.copyTo(dest, overwrite = false)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            dest.delete()
            false
        }
    }

    /** 跨卷移动时 renameTo 会失败，退化成复制 + 删源。 */
    private fun moveViaRawPath(
        context: Context,
        srcDirUri: Uri,
        srcName: String,
        destDirUri: Uri
    ): Boolean {
        val srcDir = rawDirFor(context, srcDirUri) ?: return false
        val destDir = rawDirFor(context, destDirUri) ?: return false
        if (!isSafeFileName(srcName)) return false
        val src = File(srcDir, srcName)
        val dest = File(destDir, srcName)
        if (!src.exists() || dest.exists()) return false
        return try {
            if (src.renameTo(dest)) {
                true
            } else {
                src.copyTo(dest, overwrite = false)
                src.delete()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            dest.delete()
            false
        }
    }

    /**
     * 把失败证据拼成一句可直接截图反馈的原因。纯函数，便于单元测试。
     */
    fun describeCreateFailure(canWrite: Boolean?, canCreateDir: Boolean?, cause: String?): String {
        val parts = mutableListOf<String>()
        if (canWrite != null) {
            parts += "provider 标记可写=${if (canWrite) "是" else "否"}"
        }
        if (canCreateDir != null) {
            parts += "provider 标记支持新建文件夹=${if (canCreateDir) "是" else "否"}"
        }
        if (!cause.isNullOrBlank()) parts += cause
        return if (parts.isEmpty()) "原因未知" else parts.joinToString("；")
    }

    /**
     * 纯决策函数：根据一次创建尝试的观测结果归因，不做任何 IO。
     *
     * 保持纯函数是为了能在 src/test 里直接测——项目没有 Robolectric，
     * 单测中碰 android.* 会抛 Stub!（见 LevelRepositoryTest 的既有做法）。
     *
     * 注意：调用方 `createResilient` 会在创建之前就因重名提前返回（避免 provider
     * 自动改名产生多余文件），所以 `existedBefore` 传进来的通常是 false；这里保留该
     * 分支只是让判定在任何入参下都有定义。
     */
    fun classifyCreateOutcome(
        existedBefore: Boolean,
        created: Boolean,
        existsAfter: Boolean,
        hasWritePermission: Boolean,
        cause: String?
    ): FileOpResult = when {
        existedBefore -> FileOpResult.NameExists
        // existsAfter 覆盖"provider 假装失败、文件其实已建"的情况
        created || existsAfter -> FileOpResult.Success
        !hasWritePermission -> FileOpResult.NoWritePermission
        else -> FileOpResult.Failure(cause)
    }

    /**
     * 当前目录是否持有持久化写权限。
     *
     * 只用于失败后归因，不做前置拦截：部分 provider 不老实上报 FLAG_SUPPORTS_WRITE，
     * 前置拦截会误伤本来能写的机型。
     *
     * 必须按 tree document id 比对：在目录里导航后拿到的是子 document URI，
     * 与 persistedUriPermissions 里存的根树 URI 字符串并不相等，直接比 URI 会恒为 false。
     */
    fun hasPersistedWritePermission(context: Context, currentUri: Uri): Boolean {
        val treeId = runCatching { DocumentsContract.getTreeDocumentId(currentUri) }.getOrNull()
            ?: return false
        return context.contentResolver.persistedUriPermissions.any { perm ->
            perm.isWritePermission &&
                    runCatching { DocumentsContract.getTreeDocumentId(perm.uri) }.getOrNull() == treeId
        }
    }

    /**
     * 文件名归一化：去掉首尾空白（中文输入法很容易带进尾部空格），需要时补 .json 后缀。
     * 纯函数，便于单元测试。
     */
    fun normalizeFileName(raw: String, forceJsonExt: Boolean): String {
        val name = raw.trim()
        if (name.isEmpty()) return ""
        return if (forceJsonExt && !name.endsWith(".json", ignoreCase = true)) {
            "$name.json"
        } else {
            name
        }
    }

    /**
     * 异常摘要，带最多 4 层 cause 链——provider 侧抛的 FileNotFoundException 常被包在
     * ParcelableException 里，只报最外层等于把原因丢了。
     */
    private fun describeException(e: Throwable): String {
        val parts = mutableListOf<String>()
        var current: Throwable? = e
        while (current != null && parts.size < 4) {
            parts += "${current.javaClass.simpleName}: ${current.message}"
            current = current.cause
        }
        return parts.joinToString(" <- ")
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

    fun renameItem(context: Context, currentDirUri: Uri, oldName: String, newName: String, isDirectory: Boolean): Boolean =
        runWithRawFallback(
            context,
            currentDirUri,
            safAttempt = {
                renameItemViaSaf(context, currentDirUri, oldName, newName, isDirectory)
            },
            rawAttempt = { renameViaRawPath(context, currentDirUri, oldName, newName) }
        )

    private fun renameItemViaSaf(
        context: Context,
        currentDirUri: Uri,
        oldName: String,
        newName: String,
        isDirectory: Boolean
    ): Boolean {
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
        runWithRawFallback(
            context,
            currentDirUri,
            safAttempt = {
                DocumentFile.fromTreeUri(context, currentDirUri)
                    ?.findFile(fileName)?.delete() == true
            },
            rawAttempt = { deleteViaRawPath(context, currentDirUri, fileName, isDirectory) }
        )

        if (!isDirectory) {
            val internalFile = File(context.filesDir, fileName)
            if (internalFile.exists()) internalFile.delete()
        }
    }

    fun moveFile(context: Context, srcParentUri: Uri, srcName: String, destParentUri: Uri): Boolean =
        runWithRawFallback(
            context,
            srcParentUri,
            destParentUri,
            safAttempt = { moveFileViaSaf(context, srcParentUri, srcName, destParentUri) },
            rawAttempt = { moveViaRawPath(context, srcParentUri, srcName, destParentUri) }
        )

    private fun moveFileViaSaf(
        context: Context,
        srcParentUri: Uri,
        srcName: String,
        destParentUri: Uri
    ): Boolean {
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
            // provider 不支持写时改走真实路径。internalFile 里已经是最终内容。
            rawFileFor(context, fileUri)?.let { target ->
                try {
                    internalFile.copyTo(target, overwrite = true)
                } catch (rawError: Exception) {
                    rawError.printStackTrace()
                }
            }
        }
    }

    /**
     * 从模板新建关卡。
     *
     * 返回具体结果而不是 Boolean：创建失败的原因可能是重名、模板读不到、
     * provider 拒绝创建、写流拿不到，UI 需要据此给不同提示。
     */
    fun createLevelFromTemplate(
        context: Context,
        currentDirUri: Uri,
        template: TemplateEntry,
        newFileName: String
    ): FileOpResult {
        // 直写模式：建文件与写内容一次做完，完全绕开 provider。
        // 同样要在碰 DocumentFile 之前分流。
        if (shouldUseRawPath(
                readStorageMode(context), hasRawStoragePermission(), currentDirUri.scheme
            )
        ) {
            val dir = rawDirFor(context, currentDirUri)
                ?: return appendRawHint(context, FileOpResult.Failure(null), currentDirUri)
            if (File(dir, newFileName).exists()) return FileOpResult.NameExists

            val rawContent = readTemplateContent(context, template)
                ?: return FileOpResult.Failure("无法读取模板内容")
            return if (writeFileViaRawPath(context, currentDirUri, newFileName, rawContent)) {
                FileOpResult.Success
            } else {
                appendRawHint(context, FileOpResult.Failure(null), currentDirUri)
            }
        }

        val folder = DocumentFile.fromTreeUri(context, currentDirUri)
            ?: return FileOpResult.Failure("无法访问目标目录")

        // 先查重名，避免白白读一遍模板
        if (folder.findFile(newFileName) != null) return FileOpResult.NameExists

        val content = readTemplateContent(context, template)
            ?: return FileOpResult.Failure("无法读取模板内容")

        val created = createResilient(context, folder, newFileName, "application/json")
        if (created != FileOpResult.Success) {
            // provider 拒绝创建时改走真实路径，建文件与写内容一次做完
            if (writeFileViaRawPath(context, currentDirUri, newFileName, content)) {
                return FileOpResult.Success
            }
            return appendRawHint(context, created, currentDirUri)
        }

        val newFile = folder.findFile(newFileName)
            ?: return FileOpResult.Failure("创建后未能在目录中查到该文件")

        return try {
            val stream = context.contentResolver.openOutputStream(newFile.uri)
                ?: return FileOpResult.Failure("无法打开写入流")
            stream.use { output -> output.write(content.toByteArray()) }
            FileOpResult.Success
        } catch (e: Exception) {
            e.printStackTrace()
            // 写失败就删掉刚建的空文件，免得留下一个打不开的 0 字节关卡
            newFile.delete()
            FileOpResult.Failure(describeException(e))
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