package com.example.pvz2leveleditor.data.repository

import android.content.Context
import com.example.pvz2leveleditor.data.PvzLevelFile
import com.example.pvz2leveleditor.data.PvzObject
import com.google.gson.Gson
import java.io.InputStreamReader

object ReferenceRepository {
    private val gson = Gson()

    // 缓存：Alias -> PvzObject
    private var moduleCache: Map<String, PvzObject>? = null

    /**
     * 初始化加载参考文件
     */
    fun init(context: Context) {
        if (moduleCache != null) return
        try {
            val inputStream = context.assets.open("reference/LevelModules.json")
            val root = gson.fromJson(InputStreamReader(inputStream), PvzLevelFile::class.java)
            // 建立 Alias 到 对象的索引
            moduleCache = root.objects.associateBy { it.aliases?.firstOrNull() ?: "unknown" }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * 根据别名获取参考对象的类名
     */
    fun getObjClass(alias: String): String? {
        return moduleCache?.get(alias)?.objClass
    }

}