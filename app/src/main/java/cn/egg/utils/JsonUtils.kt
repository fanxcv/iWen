package cn.egg.utils

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

/**
 * Json 工具类，基于Gson实现
 */
object JsonUtils {
    val GSON = Gson()

    val MapObjType = object : TypeToken<Map<String, Any>>() {}.type
    val MapStrType = object : TypeToken<Map<String, String>>() {}.type

    fun toJson(src: Any?) = GSON.toJson(src)
}
