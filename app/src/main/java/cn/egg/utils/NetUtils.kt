package cn.egg.utils

import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody

/**
 * 网络相关工具类
 */
object NetUtils {
    fun sendGet(client: OkHttpClient, url: String): String {
        val request = Request.Builder()
                .url(url)
                .build()
        val response = client.newCall(request).execute()
        return if (response.isSuccessful) response.body().string() else ""
    }

    fun sendPost(client: OkHttpClient, body: RequestBody, url: String): String {
        val request = Request.Builder()
                .post(body)
                .url(url)
                .build()
        val response = client.newCall(request).execute()
        return if (response.isSuccessful) response.body().string() else ""
    }
}
