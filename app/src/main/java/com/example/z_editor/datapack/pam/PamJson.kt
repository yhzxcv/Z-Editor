package com.example.z_editor.datapack.pam

import com.google.gson.GsonBuilder
import com.google.gson.JsonParseException

object PamJson {

    private val gson = GsonBuilder()
        .setPrettyPrinting()
        .disableHtmlEscaping()
        .create()

    /** 序列化前先归一化，保证输出里不会有 "timescale": 0.0 这种由缺省值造成的假值。 */
    fun encode(info: PamInfo): String = gson.toJson(info.normalized())

    /**
     * @throws IllegalArgumentException JSON 语法错误、结果不是对象、或字段无法补全时
     */
    fun decode(text: String): PamInfo {
        // 有些工具（含 .NET 默认行为）会带 UTF-8 BOM，Gson 不认，先剥掉
        val body = text.removePrefix("﻿")
        val parsed = try {
            gson.fromJson(body, PamInfo::class.java)
        } catch (e: JsonParseException) {
            throw IllegalArgumentException("不是合法的 JSON：${e.message}", e)
        } ?: throw IllegalArgumentException("JSON 解析结果为空 —— 顶层必须是一个对象")
        return try {
            parsed.normalized()
        } catch (e: IllegalArgumentException) {
            throw IllegalArgumentException("JSON 字段不完整或非法：${e.message}", e)
        }
    }

    /** 供入口页嗅探用：跳过空白与 BOM 后，首字符是不是 `{`。 */
    fun looksLikeJson(data: ByteArray): Boolean = firstMeaningfulByte(data) == '{'.code.toByte()

    private fun firstMeaningfulByte(data: ByteArray): Byte? {
        var i = 0
        // 跳过 BOM
        if (data.size >= 3 && data[0] == 0xEF.toByte() && data[1] == 0xBB.toByte() && data[2] == 0xBF.toByte()) {
            i = 3
        }
        while (i < data.size) {
            val b = data[i]
            if (b != ' '.code.toByte() && b != '\n'.code.toByte() &&
                b != '\r'.code.toByte() && b != '\t'.code.toByte()
            ) {
                return b
            }
            i++
        }
        return null
    }
}
