import android.content.Context
import com.google.gson.Gson
import com.tencent.mmkv.MMKV
import java.io.File

/**
 * MMKV 工具类封装
 *
 * 核心功能：
 * 1. 统一管理默认实例和业务实例
 * 2. 封装基础数据类型的存取
 * 3. 封装对象的 JSON 序列化存取
 * 4. 提供简洁的 API 调用
 * 5. 提供便捷的初始化函数，便于在 Application 启动时进行初始化
 */
object MMKVUtil {

    private const val app_cache_doc = "app_cache_doc"
    private val gson = Gson()

    /**
     * 在 Application onCreate() 中初始化
     * @param context Application Context
     */
    fun initContext(context: Context) {
        MMKV.initialize(context, loadMMKVPath(context))
    }

    /**
     * 加载 MMKV 存储路径
     * @param application Application 上下文
     */
    private fun loadMMKVPath(context: Context): String {
        var externalFilesDir = context.getExternalFilesDir(app_cache_doc)

        if (externalFilesDir == null) {
            externalFilesDir = context.filesDir.let {
                File(it, app_cache_doc)
            }
        }
        if (!externalFilesDir.exists()) {
            externalFilesDir.mkdirs()
        }
        val doc = File(externalFilesDir, "mmkv_doc")
        if (!doc.exists()) {
            doc.mkdirs()
        }
        return doc.absolutePath
    }

    /**
     * 获取默认的 MMKV 实例
     * 适用于存储全局、非业务敏感数据
     * @return MMKV 实例
     */
    fun defaultMMKV(): MMKV {
        return MMKV.defaultMMKV()
    }

    /**
     * 根据业务 ID 获取独立的 MMKV 实例
     * 推荐用于不同业务模块的数据隔离，如 "user", "setting"
     * @param id 业务标识
     * @return MMKV 实例
     */
    fun mmkvWithID(id: String): MMKV {
        return MMKV.mmkvWithID(id)
    }

    /**
     * 存入任意类型的值
     * 支持 String, Int, Long, Float, Boolean, Set<String>
     * 对于其他类型，会转为 JSON 字符串存储
     * @param key 键
     * @param value 值
     * @param mmkv MMKV 实例，默认为 defaultMMKV()
     */
    fun <T> put(key: String, value: T, mmkv: MMKV = defaultMMKV()) {
        when (value) {
            is String -> mmkv.encode(key, value)
            is Int -> mmkv.encode(key, value)
            is Long -> mmkv.encode(key, value)
            is Float -> mmkv.encode(key, value)
            is Boolean -> mmkv.encode(key, value)
            is Set<*> -> {
                // MMKV 仅支持 Set<String>
                @Suppress("UNCHECKED_CAST")
                mmkv.encode(key, value as? Set<String>)
            }
            else -> mmkv.encode(key, gson.toJson(value))
        }
    }

    /**
     * 读取 String 类型的值
     * @param key 键
     * @param defaultValue 默认值，默认为空字符串 ""
     * @param mmkv MMKV 实例，默认为 defaultMMKV()
     * @return 读取到的值或默认值
     */
    fun getString(key: String, defaultValue: String = "", mmkv: MMKV = defaultMMKV()): String {
        return mmkv.decodeString(key, defaultValue) ?: defaultValue
    }

    /**
     * 读取 Int 类型的值
     */
    fun getInt(key: String, defaultValue: Int = 0, mmkv: MMKV = defaultMMKV()): Int {
        return mmkv.decodeInt(key, defaultValue)
    }

    /**
     * 读取 Long 类型的值
     */
    fun getLong(key: String, defaultValue: Long = 0L, mmkv: MMKV = defaultMMKV()): Long {
        return mmkv.decodeLong(key, defaultValue)
    }

    /**
     * 读取 Float 类型的值
     */
    fun getFloat(key: String, defaultValue: Float = 0f, mmkv: MMKV = defaultMMKV()): Float {
        return mmkv.decodeFloat(key, defaultValue)
    }

    /**
     * 读取 Boolean 类型的值
     */
    fun getBoolean(key: String, defaultValue: Boolean = false, mmkv: MMKV = defaultMMKV()): Boolean {
        return mmkv.decodeBool(key, defaultValue)
    }

    /**
     * 读取 Set<String> 类型的值
     */
    fun getStringSet(key: String, defaultValue: Set<String>? = null, mmkv: MMKV = defaultMMKV()): Set<String>? {
        return mmkv.decodeStringSet(key, defaultValue)
    }

    /**
     * 读取一个对象（通过 JSON 反序列化）
     * @param key 键
     * @param clazz 对象的 Class
     * @param mmkv MMKV 实例，默认为 defaultMMKV()
     * @return 解析后的对象，失败返回 null
     */
    fun <T : Any> getObject(key: String, clazz: Class<T>, mmkv: MMKV = defaultMMKV()): T? {
        val json = mmkv.decodeString(key, null) ?: return null
        return try {
            gson.fromJson(json, clazz)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * 移除指定 key 的值
     * @param key 键
     * @param mmkv MMKV 实例，默认为 defaultMMKV()
     */
    fun remove(key: String, mmkv: MMKV = defaultMMKV()) {
        mmkv.removeValueForKey(key)
    }

    /**
     * 移除多个 key 的值
     * @param keys 要移除的键集合
     * @param mmkv MMKV 实例，默认为 defaultMMKV()
     */
    fun remove(keys: Array<String>, mmkv: MMKV = defaultMMKV()) {
        mmkv.removeValuesForKeys(keys)
    }

    /**
     * 检查是否包含指定的 key
     * @param key 键
     * @param mmkv MMKV 实例，默认为 defaultMMKV()
     * @return true 如果包含，否则 false
     */
    fun contains(key: String, mmkv: MMKV = defaultMMKV()): Boolean {
        return mmkv.containsKey(key)
    }

    /**
     * 清空所有数据
     * @param mmkv MMKV 实例，默认为 defaultMMKV()
     */
    fun clear(mmkv: MMKV = defaultMMKV()) {
        mmkv.clearAll()
    }
}