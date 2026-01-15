import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import com.example.z_editor.data.PvzObject
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.google.gson.annotations.SerializedName

/**
 * 通用的 JSON 同步状态管理器
 * @param T 数据模型类型 (Data Class)
 * @param obj 对应的 PvzObject (可能为 null)
 * @param dataClass 数据模型的 Class 对象
 * @param gson Gson 实例
 */
class JsonSyncManager<T : Any>(
    private val obj: PvzObject?,
    private val dataClass: Class<T>,
    private val gson: Gson
) {
    private val baseJson: JsonObject = if (obj?.objData != null) {
        try {
            JsonParser.parseString(obj.objData.toString()).asJsonObject
        } catch (_: Exception) {
            JsonObject()
        }
    } else {
        JsonObject()
    }

    val dataState: MutableState<T> = mutableStateOf(
        if (obj != null) {
            try {
                gson.fromJson(obj.objData, dataClass)
            } catch (_: Exception) {
                createDefaultInstance(dataClass)
            }
        } else {
            createDefaultInstance(dataClass)
        }
    )

    fun sync() {
        if (obj == null) return

        val sourceData = dataState.value
        val uiJson = gson.toJsonTree(sourceData).asJsonObject

        uiJson.entrySet().forEach { (key, element) ->
            baseJson.add(key, element)
        }

        dataClass.declaredFields.forEach { field ->
            val annotation = field.getAnnotation(SerializedName::class.java)
            val jsonKey = annotation?.value ?: field.name

            if (baseJson.has(jsonKey) && !uiJson.has(jsonKey)) {
                baseJson.remove(jsonKey)
            }
        }

        obj.objData = baseJson
    }

    private fun createDefaultInstance(clazz: Class<T>): T {
        return try {
            clazz.getDeclaredConstructor().newInstance()
        } catch (_: Exception) {
            throw RuntimeException("Data class ${clazz.simpleName} must have a no-arg constructor")
        }
    }
}

@Composable
fun <T : Any> rememberJsonSync(
    obj: PvzObject?,
    dataClass: Class<T>,
    gson: Gson = Gson()
): JsonSyncManager<T> {
    return remember(obj) {
        JsonSyncManager(obj, dataClass, gson)
    }
}