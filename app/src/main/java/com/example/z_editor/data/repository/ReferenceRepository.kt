package com.example.z_editor.data.repository

import android.content.Context
import com.example.z_editor.data.PvzLevelFile
import com.example.z_editor.data.PvzObject
import com.google.gson.Gson
import java.io.InputStreamReader

object ReferenceRepository {
    private val gson = Gson()

    private var moduleCache: Map<String, PvzObject>? = null

    private val validGridItemAliases = HashSet<String>()

    /**
     * 初始化加载参考文件
     * 在 App 启动或进入编辑器时调用
     */
    fun init(context: Context) {
        loadLevelModules(context)
        loadGridItemTypes(context)
    }

    private fun loadLevelModules(context: Context) {
        if (moduleCache != null) return
        try {
            val inputStream = context.assets.open("reference/LevelModules.json")
            val root = gson.fromJson(InputStreamReader(inputStream), PvzLevelFile::class.java)
            moduleCache = root.objects
                .distinctBy { it.aliases?.firstOrNull() ?: "unknown" }
                .associateBy { it.aliases?.firstOrNull() ?: "unknown" }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun loadGridItemTypes(context: Context) {
        if (validGridItemAliases.isNotEmpty()) return
        try {
            val inputStream = context.assets.open("reference/GridItemTypes.json")
            val root = gson.fromJson(InputStreamReader(inputStream), PvzLevelFile::class.java)

            root.objects.forEach { obj ->
                obj.aliases?.forEach { alias ->
                    validGridItemAliases.add(alias)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun getObjClass(alias: String): String? {
        return moduleCache?.get(alias)?.objClass
    }

    /**
     * 返回参考文件 LevelModules.json 中定义的所有模块别名（首别名 key 集合）。
     * 参考文件尚未加载时返回 null，调用方应视为"无法校验"（不判失效，避免误报）。
     */
    fun getLevelModuleAliases(): Set<String>? {
        return moduleCache?.keys
    }

    fun getObject(alias: String): PvzObject? {
        return moduleCache?.get(alias)
    }

    fun isValidGridItem(alias: String): Boolean {
        if (validGridItemAliases.isEmpty()) return true
        return validGridItemAliases.contains(alias)
    }
}