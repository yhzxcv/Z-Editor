package com.example.z_editor.datapack.hotupdate

import com.example.z_editor.datapack.crypto.Pvz2Crypto
import org.junit.Assert.*
import org.junit.Test
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

/**
 * Round-trip tests for HujsonConverter encode/decode.
 *
 * Pipeline under test:
 *   encode: JSON → PopCap Zlib → Rijndael encrypt → 0x1000 → Base64
 *   decode: Base64 → strip 0x1000 → Rijndael decrypt → PopCap Zlib decompress → JSON
 */
class HujsonConverterTest {

    private val defaultKey = Pvz2Crypto.DEFAULT_KEY

    // ---- Round-trip tests ----

    @Test
    fun roundTrip_simpleObject() {
        val original = """{"level":1,"name":"test"}"""
        val encoded = HotUpdateJSONConverter.encodeHotUpdateString(original, defaultKey)
        val decoded = HotUpdateJSONConverter.decodeHotUpdateString(encoded, defaultKey)
        assertEquals(original, decoded)
    }

    @Test
    fun roundTrip_nestedObject() {
        val original = """{"level":{"id":5,"waves":[{"type":"normal","count":10},{"type":"flag","count":1}]}}"""
        val encoded = HotUpdateJSONConverter.encodeHotUpdateString(original, defaultKey)
        val decoded = HotUpdateJSONConverter.decodeHotUpdateString(encoded, defaultKey)
        assertEquals(original, decoded)
    }

    @Test
    fun roundTrip_arrayRoot() {
        val original = """[1,2,3,4,5]"""
        val encoded = HotUpdateJSONConverter.encodeHotUpdateString(original, defaultKey)
        val decoded = HotUpdateJSONConverter.decodeHotUpdateString(encoded, defaultKey)
        assertEquals(original, decoded)
    }

    @Test
    fun roundTrip_unicode() {
        val original = """{"name":"植物大战僵尸2","description":"🧟‍♂️🌻"}"""
        val encoded = HotUpdateJSONConverter.encodeHotUpdateString(original, defaultKey)
        val decoded = HotUpdateJSONConverter.decodeHotUpdateString(encoded, defaultKey)
        assertEquals(original, decoded)
    }

    @Test
    fun roundTrip_emptyObject() {
        val original = "{}"
        val encoded = HotUpdateJSONConverter.encodeHotUpdateString(original, defaultKey)
        val decoded = HotUpdateJSONConverter.decodeHotUpdateString(encoded, defaultKey)
        assertEquals(original, decoded)
    }

    @Test
    fun roundTrip_largeJson() {
        // Build a decently sized JSON with repeated structure
        val items = (1..100).joinToString(",") { i ->
            """{"id":$i,"name":"item_$i","value":${i * 1.5},"flag":${i % 2 == 0}}"""
        }
        val original = """{"items":[$items],"total":100}"""
        val encoded = HotUpdateJSONConverter.encodeHotUpdateString(original, defaultKey)
        val decoded = HotUpdateJSONConverter.decodeHotUpdateString(encoded, defaultKey)
        assertEquals(original, decoded)
    }

    @Test
    fun roundTrip_withWhitespace() {
        val original = """
            {
              "level": 1,
              "name": "test",
              "values": [10, 20, 30]
            }
        """.trimIndent()
        val encoded = HotUpdateJSONConverter.encodeHotUpdateString(original, defaultKey)
        val decoded = HotUpdateJSONConverter.decodeHotUpdateString(encoded, defaultKey)
        assertEquals(original, decoded)
    }

    // ---- Format detection tests ----

    @Test
    fun isHotUpdateFormat_validBase64() {
        val json = """{"test":1}"""
        val encoded = HotUpdateJSONConverter.encodeHotUpdateString(json, defaultKey)
        assertTrue("Should detect as hot-update format", HotUpdateJSONConverter.isHotUpdateFormat(encoded))
    }

    @Test
    fun isHotUpdateFormat_plainJson() {
        assertFalse("Plain JSON should not be detected as hot-update",
            HotUpdateJSONConverter.isHotUpdateFormat("""{"test":1}"""))
    }

    @Test
    fun isHotUpdateFormat_invalidBase64() {
        assertFalse("Invalid text should not be detected",
            HotUpdateJSONConverter.isHotUpdateFormat("not a valid format"))
    }

    // ---- Multi-round stability ----

    @Test
    fun multipleRoundTrips_produceSameResult() {
        val original = """{"key":"value","number":42}"""
        var encoded = HotUpdateJSONConverter.encodeHotUpdateString(original, defaultKey)

        // 3 round-trips should all return the same original
        repeat(3) {
            val decoded = HotUpdateJSONConverter.decodeHotUpdateString(encoded, defaultKey)
            assertEquals("Round-trip #${it + 1} should match", original, decoded)
            encoded = HotUpdateJSONConverter.encodeHotUpdateString(decoded, defaultKey)
        }
    }

    // ---- Cross-key tests ----

    @Test
    fun roundTrip_withHexKey() {
        val hexKey = "65bd1b2305f46eb2806b935aab7630bb"
        val original = """{"version":"2.0"}"""
        val encoded = HotUpdateJSONConverter.encodeHotUpdateString(original, hexKey)
        val decoded = HotUpdateJSONConverter.decodeHotUpdateString(encoded, hexKey)
        assertEquals(original, decoded)
    }

    @Test
    fun decode_withWrongKey_doesNotReproduceOriginal() {
        val original = """{"test":1}"""
        val encoded = HotUpdateJSONConverter.encodeHotUpdateString(original, defaultKey)

        // ⚠️ 用错 key **不会抛异常**。Pvz2Crypto.popcapZlibDecompress 在 magic 对不上时
        // 是"原样返回"而不是抛（见 Pvz2Crypto.kt 末尾的 `return data`），所以这里
        // 拿到的是解密后的乱码，一路静默返回，decodeHotUpdateString 里的 try/catch
        // 实际上几乎碰不到。
        //
        // 这条用例原先写的是 @Test(expected = Exception::class)，且长期是"绿的"
        // ——但那个异常是 android.util.Base64 的 stub 抛的 "not mocked"，
        // 跟 key 对不对毫无关系。把 Base64 换成 Kotlin stdlib 之后才露出真面目。
        // 所以这里改成钉住**现状**：至少不能原样还原出原文。
        val decoded = HotUpdateJSONConverter.decodeHotUpdateString(encoded, "wrong_key_string_here")
        assertNotEquals("用错 key 不该还原出原文", original, decoded)
    }

    // ---- Base64 output format ----

    @Test
    @OptIn(ExperimentalEncodingApi::class)
    fun encode_outputIsBase64() {
        val encoded = HotUpdateJSONConverter.encodeHotUpdateString("""{"a":1}""", defaultKey)
        // Should be valid Base64 without line wraps
        assertFalse("Output should not contain newlines", encoded.contains("\n"))
        assertFalse("Output should not contain carriage returns", encoded.contains("\r"))
        // Should decode back to binary starting with 0x1000.
        // 这里用 stdlib 的 Base64 而不是 android.util.Base64 —— 后者在 JVM 单测里
        // 只有 stub，一调用就 "not mocked" 抛异常。
        val decoded = Base64.Mime.decode(encoded)
        assertEquals(0x10.toByte(), decoded[0])
        assertEquals(0x00.toByte(), decoded[1])
    }

    // ---- Edge cases ----

    @Test
    fun roundTrip_stringWithSpecialChars() {
        val original = """{"path":"C:\\Users\\test\\file.json","url":"https://example.com?a=1&b=2"}"""
        val encoded = HotUpdateJSONConverter.encodeHotUpdateString(original, defaultKey)
        val decoded = HotUpdateJSONConverter.decodeHotUpdateString(encoded, defaultKey)
        assertEquals(original, decoded)
    }

    @Test
    fun roundTrip_singleStringValue() {
        val original = "\"hello world\""
        val encoded = HotUpdateJSONConverter.encodeHotUpdateString(original, defaultKey)
        val decoded = HotUpdateJSONConverter.decodeHotUpdateString(encoded, defaultKey)
        assertEquals(original, decoded)
    }
}
