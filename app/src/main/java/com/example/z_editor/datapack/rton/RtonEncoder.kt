package com.example.z_editor.datapack.rton

import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonPrimitive
import java.io.ByteArrayOutputStream

/**
 * RTON binary format encoder — port of pyvz2's JSONDecoder (pyvz2rton.py).
 *
 * Converts Gson JsonObject → RTON binary bytes.
 * All strings use cached encoding (0x90/0x91) matching pyvz2.
 */
object RtonEncoder {

    // ---- Varint encoding ----

    private fun ByteArrayOutputStream.writeVarint(value: Int) {
        var v = value
        while (true) {
            var byte = v and 0x7F
            v = v ushr 7
            if (v != 0) byte = byte or 0x80
            write(byte)
            if (v == 0) break
        }
    }

    private fun ByteArrayOutputStream.writeZigzag(value: Long): Int {
        val encoded = if (value >= 0) value * 2 else -value * 2 - 1
        val v = encoded.toInt() // pyvz2 works with ints
        writeVarint(v)
        return v
    }

    // ---- Integer encoding (matching pyvz2's encode_int) ----

    private fun ByteArrayOutputStream.writeInt(value: Long) {
        when {
            value == 0L -> write(0x21)  // !
            value in 0..2097151 -> { write(0x24); writeVarint(value.toInt()) }  // $
            value in -1048576..-1 -> { write(0x25); writeZigzag(value) }  // %
            value in Int.MIN_VALUE..Int.MAX_VALUE -> {  // space
                write(0x20)
                write((value.toInt() and 0xFF))
                write(((value.toInt() ushr 8) and 0xFF))
                write(((value.toInt() ushr 16) and 0xFF))
                write(((value.toInt() ushr 24) and 0xFF))
            }
            value in 0L..0xFFFFFFFFL -> {  // &
                write(0x26)
                write((value.toInt() and 0xFF))
                write(((value.toInt() ushr 8) and 0xFF))
                write(((value.toInt() ushr 16) and 0xFF))
                write(((value.toInt() ushr 24) and 0xFF))
            }
            value in 0..562949953421311 -> { write(0x44); writeVarint(value.toInt()) }  // D
            value in -281474976710656..-1 -> { write(0x45); writeZigzag(value) }  // E
            value in Long.MIN_VALUE..Long.MAX_VALUE -> {  // @
                write(0x40)
                writeLongLE(value)
            }
            value >= 0 -> {  // F
                write(0x46)
                writeLongLE(value)
            }
            else -> { write(0x45); writeZigzag(value) }  // E fallback
        }
    }

    // ---- Float encoding (matching pyvz2's encode_float) ----

    private fun ByteArrayOutputStream.writeFloat(value: Double) {
        if (value == 0.0) {
            write(0x23)  // #
            return
        }
        if (value.isNaN() || value.isInfinite()) {
            write(0x42)  // B
            writeDoubleLE(value)
            return
        }
        // Check if float32 round-trips
        val f32 = value.toFloat()
        if (f32.toDouble() == value) {
            write(0x22)  // "
            writeFloatLE(f32)
        } else {
            write(0x42)  // B
            writeDoubleLE(value)
        }
    }

    // ---- Cached string encoding (matching pyvz2's encode_cached_string) ----

    private class StringCache {
        private val map = mutableMapOf<String, Int>()

        fun encode(s: String, out: ByteArrayOutputStream) {
            val idx = map[s]
            if (idx != null) {
                out.write(0x91)  // recall
                out.writeVarint(idx)
                return
            }
            map[s] = map.size
            val bytes = s.toByteArray(Charsets.UTF_8)  // pyvz2 uses string.encode() = UTF-8
            out.write(0x90)  // cached ASCII (but with UTF-8 bytes)
            out.writeVarint(bytes.size)
            out.write(bytes)
        }
    }

    // ---- Object & Array encoding (matching pyvz2's encode_object/encode_array) ----

    private fun encodeObject(obj: JsonObject, cache: StringCache, out: ByteArrayOutputStream, includeTag: Boolean = true) {
        if (includeTag) out.write(0x85)
        for ((key, value) in obj.entrySet()) {
            cache.encode(key, out)
            encodeValue(value, cache, out)
        }
        out.write(0xFF)
    }

    private fun encodeArray(arr: JsonArray, cache: StringCache, out: ByteArrayOutputStream) {
        out.write(0x86)
        out.write(0xFD)
        out.writeVarint(arr.size())
        for (element in arr) {
            encodeValue(element, cache, out)
        }
        out.write(0xFE)
    }

    // ---- RTID encoding (matching pyvz2's encode_rtid) ----

    private fun writeUtf8Text(out: ByteArrayOutputStream, s: String) {
        val bytes = s.toByteArray(Charsets.UTF_8)
        out.writeVarint(s.length)    // character count
        out.writeVarint(bytes.size)  // byte count
        out.write(bytes)
    }

    private fun encodeRtid(value: String, out: ByteArrayOutputStream) {
        if (value == "RTID(0)" || value == "RTID()") {
            out.write(0x84)  // null ref
            return
        }
        if (!value.startsWith("RTID(") || !value.endsWith(")")) {
            // Not an RTID, shouldn't happen but fallback
            return
        }
        val inner = value.substring(5, value.length - 1)
        val atIdx = inner.lastIndexOf('@')
        if (atIdx == -1) {
            out.write(0x84)
            return
        }
        val name = inner.substring(0, atIdx)
        val type = inner.substring(atIdx + 1)

        val dotted = name.split(".")
        if (dotted.size == 3) {
            // Format: RTID(i1.i2.uid@type)
            val i1 = dotted[1]  // second part maps to i1 in pyvz2
            val i2 = dotted[0]  // first part maps to i2 in pyvz2
            val uidHex = dotted[2]
            out.write(0x83)
            out.write(0x02)
            writeUtf8Text(out, type)
            out.writeVarint(i1.toIntOrNull() ?: 0)  // pyvz2 writes i1 first
            out.writeVarint(i2.toIntOrNull() ?: 0)  // then i2
            // UID bytes reversed
            val uidBytes = uidHex.chunked(2).map { it.toInt(16).toByte() }.toByteArray()
            out.write(uidBytes.reversedArray())
        } else {
            // Format: RTID(name@type)
            out.write(0x83)
            out.write(0x03)
            writeUtf8Text(out, type)
            writeUtf8Text(out, name)
        }
    }

    // ---- Value dispatch (matching pyvz2's encode logic) ----

    private fun encodeValue(value: JsonElement?, cache: StringCache, out: ByteArrayOutputStream) {
        when {
            value == null || value.isJsonNull -> out.write(0x84)
            value.isJsonPrimitive -> {
                val prim = value.asJsonPrimitive
                when {
                    prim.isBoolean -> out.write(if (prim.asBoolean) 0x01 else 0x00)
                    prim.isNumber -> {
                        val num = prim.asNumber
                        val str = num.toString()
                        // Check if it has a decimal point or exponent → float
                        if ('.' in str || 'e' in str || 'E' in str) {
                            out.writeFloat(num.toDouble())
                        } else {
                            out.writeInt(num.toLong())
                        }
                    }
                    prim.isString -> {
                        val s = prim.asString
                        if (s.startsWith("RTID(")) {
                            encodeRtid(s, out)
                        } else {
                            cache.encode(s, out)
                        }
                    }
                }
            }
            value.isJsonObject -> encodeObject(value.asJsonObject, cache, out)
            value.isJsonArray -> encodeArray(value.asJsonArray, cache, out)
        }
    }

    // ---- Little-endian helpers ----

    private fun ByteArrayOutputStream.writeLongLE(v: Long) {
        write((v and 0xFF).toInt())
        write(((v ushr 8) and 0xFF).toInt())
        write(((v ushr 16) and 0xFF).toInt())
        write(((v ushr 24) and 0xFF).toInt())
        write(((v ushr 32) and 0xFF).toInt())
        write(((v ushr 40) and 0xFF).toInt())
        write(((v ushr 48) and 0xFF).toInt())
        write(((v ushr 56) and 0xFF).toInt())
    }

    private fun ByteArrayOutputStream.writeDoubleLE(v: Double) {
        val bits = java.lang.Double.doubleToRawLongBits(v)
        writeLongLE(bits)
    }

    private fun ByteArrayOutputStream.writeFloatLE(v: Float) {
        val bits = java.lang.Float.floatToRawIntBits(v)
        write((bits and 0xFF))
        write(((bits ushr 8) and 0xFF))
        write(((bits ushr 16) and 0xFF))
        write(((bits ushr 24) and 0xFF))
    }

    // ---- Public API ----

    /**
     * Encode a JsonObject to RTON binary format.
     * Output includes RTON header + version (1) + body + DONE footer.
     */
    fun encode(json: JsonObject, version: Int = 1): ByteArray {
        val out = ByteArrayOutputStream()
        val cache = StringCache()

        // Header: "RTON" + version (uint32 LE)
        out.write("RTON".toByteArray(Charsets.US_ASCII))
        out.write((version and 0xFF))
        out.write(((version ushr 8) and 0xFF))
        out.write(((version ushr 16) and 0xFF))
        out.write(((version ushr 24) and 0xFF))

        // Body — top-level object without 0x85 tag (matching pyvz2's encode_root_object)
        encodeObject(json, cache, out, includeTag = false)

        // Footer: "DONE"
        out.write("DONE".toByteArray(Charsets.US_ASCII))
        return out.toByteArray()
    }
}
