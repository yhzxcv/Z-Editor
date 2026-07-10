package com.example.z_editor.datapack.rton

import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * RTON binary format parser — 1:1 port of pyvz2's RTONDecoder (pyvz2rton.py).
 *
 * Outputs a nested Map<String, Any?> compatible with Gson serialization.
 */
object RtonParser {

    // ---- Varint ----

    private fun ByteBuffer.readVarint(): Int {
        var result = 0
        var multiplier = 1
        var b: Int
        do {
            b = get().toInt() and 0xFF
            result += (b and 0x7F) * multiplier
            multiplier *= 128
        } while (b > 127)
        return result
    }

    private fun ByteBuffer.readZigzag(): Int {
        var num = readVarint()
        if (num % 2 == 1) num = -num - 1
        return num / 2
    }

    // ---- String decoders (matching pyvz2: try UTF-8 first, fallback to Latin-1) ----

    private fun decodeText(bytes: ByteArray): String {
        return try {
            bytes.toString(Charsets.UTF_8)
        } catch (_: Exception) {
            bytes.toString(Charsets.ISO_8859_1)
        }
    }

    private fun decodeUtf8Text(buf: ByteBuffer): String {
        val charLen = buf.readVarint()
        val byteLen = buf.readVarint()
        val bytes = ByteArray(byteLen).also { buf.get(it) }
        val s = bytes.toString(Charsets.UTF_8)
        // Silently tolerate char length mismatch (matching pyvz2)
        return s
    }

    // ---- RTID (matching pyvz2's rtid_mappings) ----

    private fun readRtid(buf: ByteBuffer): String {
        val sub = buf.get().toInt() and 0xFF
        return when (sub) {
            0x00 -> "RTID(0)"
            0x02 -> {
                val p1 = decodeUtf8Text(buf)  // type string
                val i2 = buf.readVarint()
                val i1 = buf.readVarint()
                val uid = ByteArray(4).also { buf.get(it) }
                val hex = uid.reversedArray().joinToString("") { "%02x".format(it) }
                "RTID($i1.$i2.$hex@$p1)"
            }
            0x03 -> {
                val p1 = decodeUtf8Text(buf)  // first string
                val p2 = decodeUtf8Text(buf)  // second string
                "RTID($p2@$p1)"
            }
            else -> "RTID(unknown_$sub)"
        }
    }

    // ---- Object & Array ----

    private fun readObject(
        buf: ByteBuffer,
        asciiCache: MutableList<String>,
        utf8Cache: MutableList<String>
    ): MutableMap<String, Any?> {
        val result = linkedMapOf<String, Any?>()
        while (buf.hasRemaining()) {
            val tag = buf.get().toInt() and 0xFF
            if (tag == 0xFF) break
            val key = readKey(tag, buf, asciiCache, utf8Cache)
            if (!buf.hasRemaining()) break
            val valTag = buf.get().toInt() and 0xFF
            val value = readValue(valTag, buf, asciiCache, utf8Cache)
            result[key] = value
        }
        return result
    }

    private fun readArray(
        buf: ByteBuffer,
        asciiCache: MutableList<String>,
        utf8Cache: MutableList<String>
    ): List<Any?> {
        val listType = buf.get().toInt() and 0xFF
        if (listType != 0xFD) return emptyList()

        val expectedLen = buf.readVarint()
        val result = mutableListOf<Any?>()
        while (buf.hasRemaining()) {
            val tag = buf.get().toInt() and 0xFF
            if (tag == 0xFE) break
            result.add(readValue(tag, buf, asciiCache, utf8Cache))
        }
        return result
    }

    // ---- Key dispatch (matching pyvz2's key_mappings) ----

    private fun readKey(
        tag: Int,
        buf: ByteBuffer,
        asciiCache: MutableList<String>,
        utf8Cache: MutableList<String>
    ): String {
        return when (tag) {
            // Inline ASCII (0x81) — try UTF-8 first, fallback to Latin-1
            0x81 -> {
                val len = buf.readVarint()
                val bytes = ByteArray(len).also { buf.get(it) }
                decodeText(bytes)
            }
            // Inline UTF-8 (0x82)
            0x82 -> decodeUtf8Text(buf)
            // Cached ASCII (0x90)
            0x90 -> {
                val len = buf.readVarint()
                val bytes = ByteArray(len).also { buf.get(it) }
                val s = decodeText(bytes)
                asciiCache.add(s)
                s
            }
            // Cached ASCII recall (0x91)
            0x91 -> asciiCache.getOrElse(buf.readVarint()) { "" }
            // Cached UTF-8 (0x92)
            0x92 -> {
                val s = decodeUtf8Text(buf)
                utf8Cache.add(s)
                s
            }
            // Cached UTF-8 recall (0x93)
            0x93 -> utf8Cache.getOrElse(buf.readVarint()) { "" }
            else -> "unknown_key_$tag"
        }
    }

    // ---- Value dispatch (matching pyvz2's value_mappings) ----

    private fun readValue(
        tag: Int,
        buf: ByteBuffer,
        asciiCache: MutableList<String>,
        utf8Cache: MutableList<String>
    ): Any? {
        return when (tag) {
            0x00 -> false
            0x01 -> true

            // 8-bit
            0x08 -> buf.get().toByte().toInt()
            0x09, 0x0B -> 0
            0x0A -> buf.get().toInt() and 0xFF

            // 16-bit
            0x10 -> buf.order(ByteOrder.LITTLE_ENDIAN).short.toInt()
            0x11, 0x13 -> 0
            0x12 -> buf.order(ByteOrder.LITTLE_ENDIAN).short.toInt() and 0xFFFF

            // 32-bit
            0x20 -> buf.order(ByteOrder.LITTLE_ENDIAN).int
            0x21, 0x27 -> 0
            0x22 -> buf.order(ByteOrder.LITTLE_ENDIAN).float.toDouble()
            0x23 -> 0.0
            0x24, 0x28, 0x44, 0x48 -> buf.readVarint().toLong()
            0x25, 0x29, 0x45, 0x49 -> buf.readZigzag().toLong()
            0x26 -> buf.order(ByteOrder.LITTLE_ENDIAN).int.toLong() and 0xFFFFFFFFL

            // 64-bit
            0x40 -> buf.order(ByteOrder.LITTLE_ENDIAN).long
            0x41, 0x47 -> 0L
            0x42 -> buf.order(ByteOrder.LITTLE_ENDIAN).double
            0x43 -> 0.0
            0x46 -> buf.order(ByteOrder.LITTLE_ENDIAN).long // unsigned 64-bit stored as long

            // Strings
            0x81 -> {
                val len = buf.readVarint()
                val bytes = ByteArray(len).also { buf.get(it) }
                decodeText(bytes)
            }
            0x82 -> decodeUtf8Text(buf)

            // RTID / Null
            0x83 -> readRtid(buf)
            0x84 -> null  // pyvz2: null ref → maps to null (not string)

            // Object / Array
            0x85 -> readObject(buf, asciiCache, utf8Cache)
            0x86 -> readArray(buf, asciiCache, utf8Cache)

            // Cached strings (values can also be cached)
            0x90 -> {
                val len = buf.readVarint()
                val bytes = ByteArray(len).also { buf.get(it) }
                val s = decodeText(bytes)
                asciiCache.add(s)
                s
            }
            0x91 -> asciiCache.getOrElse(buf.readVarint()) { "" }
            0x92 -> {
                val s = decodeUtf8Text(buf)
                utf8Cache.add(s)
                s
            }
            0x93 -> utf8Cache.getOrElse(buf.readVarint()) { "" }

            else -> null
        }
    }

    // ---- Public API ----

    /**
     * Parse RTON binary data. Input must include the RTON header ("RTON" + version).
     * Returns a Map (LinkedHashMap, preserving key order) suitable for Gson serialization.
     */
    fun parse(data: ByteArray): MutableMap<String, Any?> {
        val buf = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN)

        // Read header
        if (buf.remaining() < 8) return mutableMapOf()
        val magic = ByteArray(4).also { buf.get(it) }
        if (String(magic) != "RTON") {
            // Not an RTON file
            return mutableMapOf()
        }
        /* version = */ buf.int // read but not used

        val asciiCache = mutableListOf<String>()
        val utf8Cache = mutableListOf<String>()

        return readObject(buf, asciiCache, utf8Cache)
    }
}
