package cn.egg.service

import android.app.Activity
import android.app.Service
import android.content.Intent
import android.net.ConnectivityManager
import android.os.Handler
import android.os.IBinder
import android.util.Log
import cn.egg.R
import cn.egg.utils.EGGRunException
import cn.egg.utils.JsonUtils
import cn.egg.utils.NetUtils
import cn.egg.utils.Utils
import com.google.gson.reflect.TypeToken
import okhttp3.*
import java.util.concurrent.ConcurrentHashMap


class EGGService : Service(), Runnable {

    private val m_handler: Handler = Handler()
    private var thread: Thread? = null

    private lateinit var cm: ConnectivityManager

    init {
        instance = this
    }

    private inner class Public {
        internal var success: String? = null
        internal var message: String? = null
        internal var data: Map<String, Any>? = null
    }

    private inner class Data {
        internal var success: String? = null
        internal var message: String? = null
        internal var data: InnerData? = null

        inner class InnerData {
            internal var analyze_data: AnalyzeData? = null
            internal var config: Map<String, Any>? = null
            internal var data: ArrayList<HashMap<String, String>>? = null
            internal var domain: String? = null
            internal var show_type: String? = null
            internal var status_code: String? = null
            internal var status_msg: String? = null
            internal var token: String? = null
        }

        inner class AnalyzeData {
            internal var data: ArrayList<HashMap<String, String>>? = null
            internal var total: Int = 0
        }
    }

    private inner class OneData {
        internal var qid: String? = null
        internal var success: String? = null
        internal var zhengu: Map<String, Any>? = null
        internal var xuangu: Xuangu? = null

        inner class Xuangu {
            var blocks: ArrayList<InnerData>? = null
        }

        inner class InnerData {
            var data: InnerDataTwo? = null
            var show_type: String? = null
        }

        inner class InnerDataTwo {
            var fieldType: Any? = null
            var fieldUnit: Any? = null
            var indexID: Any? = null
            var natural: Any? = null
            var orresProcess: Any? = null
            var showType: Any? = null
            var sortSign: Any? = null
            var title: ArrayList<String>? = null
            var result: ArrayList<ArrayList<String>>? = null
            var perpage: String? = null
            var score: String? = null
            var token: String? = null
            var total: String? = null
        }
    }

    private val publicType = object : TypeToken<Public>() {}.type
    private val oneType = object : TypeToken<OneData>() {}.type
    private val dataType = object : TypeToken<Data>() {}.type

    private val cookieJar = object : CookieJar {
        private val cookieStore = HashMap<String, List<Cookie>>()

        override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
            cookieStore.put(url.host(), cookies)
        }

        override fun loadForRequest(url: HttpUrl): List<Cookie> {
            val cookies = cookieStore[url.host()]
            return cookies ?: ArrayList()
        }

        internal fun clear() = cookieStore.clear()
    }

    override fun onCreate() {
        cm = getSystemService(Activity.CONNECTIVITY_SERVICE) as ConnectivityManager
        super.onCreate()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        isRunning = true
        thread = Thread(this, "watchThread")
        thread?.start()
        onStatusChanged(getString(R.string.watch_start), isRunning)
        writeLog(getString(R.string.watch_start))
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        if (isRunning) dispose()
        onStatusChanged(getString(R.string.watch_stop), isRunning)
        writeLog(getString(R.string.watch_stop))
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder {
        throw RuntimeException("Stub!")
    }

    override fun run() {
        try {
            isOk = false
            if (isRunning) {
                when (type) {
                    Utils.CHECK_LIST -> check_list()
                    Utils.CHECK_ONE -> check_one()
                }
            } else writeLog(getString(R.string.watch_stop))
        } catch (e: EGGRunException) {
            if (Utils.ISDEBUG) print(e.printStackTrace())
            writeLog(getString(R.string.watch_stop))
        } catch (e: InterruptedException) {
            if (Utils.ISDEBUG) print(e.printStackTrace())
            writeLog(getString(R.string.watch_stop))
        } catch (e: Exception) {
            if (Utils.ISDEBUG) print(e.printStackTrace())
            writeLog("系统错误，正在拼命重试。。。")
            checkInternet()
            thread = Thread(this, "watchThread")
            thread?.start() ?: writeLog(getString(R.string.watch_stop))
        }
    }

    private fun check_one() {
        var str_list = searchText.split(" ")
        str_list = str_list.filter { it.isNotBlank() }
        val old: HashMap<String, HashMap<String, String>> = getDataOne(str_list) ?: HashMap()
        var new: HashMap<String, HashMap<String, String>>?
        initListView(Utils.map2List(old))

        while (isRunning) {
            if (isOk) {
                Thread.sleep(500)
                new = getDataOne(str_list)
                if (new == null) continue
                val res = Utils.dealDataOne(old, new)
                if (res.size <= 0) continue
                isOk = false
                updateList(res)
            } else Thread.sleep(500)
        }
    }

    private fun check_list() {
        var list1: ArrayList<HashMap<String, String>> = getDataList() ?: ArrayList()
        var list2: ArrayList<HashMap<String, String>>?
        initListView(list1)

        while (isRunning) {
            if (isOk) {
                Thread.sleep(4000)
                list2 = getDataList()
                if (list2 == null) continue
                if (Utils.ISDEBUG) searchText = "涨幅大于3"
                if (list1.hashCode() == list2.hashCode()) continue
                isOk = false
                updateList(Utils.dealData(list1, list2))
                list1 = list2
            } else Thread.sleep(5000)
        }
    }

    private fun dispose() {
        isRunning = false
        thread?.interrupt()
    }

    private fun getDataOne(list: List<String>): HashMap<String, HashMap<String, String>>? {
        val m_one_list = HashMap<String, HashMap<String, String>>()
        if (Utils.ISDEBUG) Log.d(this.toString(), list.toString())
        list.forEach { v ->
            val client = OkHttpClient.Builder().build()
            val url = "http://m.iwencai.com/wap/search?w=$v&source=phone&queryarea=all&tid=stockpick&cid=${Utils.execId()}&perpage=20"
            val res = NetUtils.sendGet(client, url)
            if (res.startsWith("<!DOCTYPE html>")) {
                //验证码
                Utils.writeCmd(Utils.COMMAND_AIRPLANE_ON)
                Thread.sleep(1000)
                Utils.writeCmd(Utils.COMMAND_AIRPLANE_OFF)
                checkInternet()
                return null
            } else {
                val m_one_tmp = HashMap<String, String>()
                val data: OneData = JsonUtils.GSON.fromJson(res, oneType)
                val title = data.xuangu?.blocks?.get(0)?.data?.title ?: ArrayList()
                val result = data.xuangu?.blocks?.get(0)?.data?.result ?: ArrayList()
                if (result.size > 0) {
                    val res_in = result[0]
                    if (Utils.ISDEBUG) Log.d(this.toString(), res_in.toString())
                    for (i in 0 until title.size) {
                        m_one_tmp.put(title[i], res_in[i])
                    }
                    m_one_list.put(v, m_one_tmp)
                    if (Utils.ISDEBUG) Log.d(this.toString(), m_one_list.toString())
                }
            }
        }
        return m_one_list
    }

    private fun getDataList(): ArrayList<HashMap<String, String>>? {
        cookieJar.clear()
        val client = OkHttpClient.Builder().cookieJar(cookieJar).build()
        val url = "http://m.iwencai.com/unified-wap/get-base-data?w=$searchText&source=phone&queryarea=all&tid=stockpick&cid=${Utils.execId()}&perpage=20"
        val res = NetUtils.sendGet(client, url)
        if (res.startsWith("<!DOCTYPE html>")) {
            //验证码
            Utils.writeCmd(Utils.COMMAND_AIRPLANE_ON)
            Thread.sleep(1000)
            Utils.writeCmd(Utils.COMMAND_AIRPLANE_OFF)
            checkInternet()
        } else {
            val data: Public = JsonUtils.GSON.fromJson(res, publicType)

            val params = FormBody.Builder()
                    .add("condition", data.data!!["condition"] as String)
                    .build()
            val res_1 = NetUtils.sendPost(client, params, "http://m.iwencai.com/unified-wap/get-parser-data?w=$searchText")
            val data_1: Data = JsonUtils.GSON.fromJson(res_1, dataType)
            val size = data_1.data!!.analyze_data!!.total
            writeLog("共找到-$size-只股票，努力监视中。。。")
            if (size > 30) {
                val getSize = size + 10
                val res_2 = NetUtils.sendGet(client, "http://m.iwencai.com/unified-wap/cache?page=1&perpage=$getSize&token=${data_1.data!!.token}")
                val data_2: Data = JsonUtils.GSON.fromJson(res_2, dataType)
                return data_2.data!!.data ?: ArrayList()
            }
            return data_1.data!!.data ?: ArrayList()
        }
        return null
    }

    private fun checkInternet(total: Int = 120) {
        var times = 0
        while (cm.activeNetworkInfo == null) {
            if (times >= total) throw EGGRunException("停了！停了！")
            Thread.sleep(1000)
            times++
        }
    }

    interface onStatusChangedListener {
        fun onStatusChanged(status: String, isRunning: Boolean)

        fun updateList(data: ArrayList<HashMap<String, String>>)

        fun onLogReceived(logString: String)

        fun initListView(array: ArrayList<HashMap<String, String>>, type: Int)
    }

    private fun initListView(array: ArrayList<HashMap<String, String>>) {
        m_handler.post({
            for (entry in m_onStatuChangedListeners.entries) {
                entry.key.initListView(array, type)
            }
        })
    }

    private fun updateList(data: ArrayList<HashMap<String, String>>) {
        m_handler.post({
            for (entry in m_onStatuChangedListeners.entries) {
                entry.key.updateList(data)
            }
        })
    }

    private fun onStatusChanged(status: String, isRunning: Boolean) {
        m_handler.post({
            for (entry in m_onStatuChangedListeners.entries) {
                entry.key.onStatusChanged(status, isRunning)
            }
        })
    }

    private fun writeLog(format: String, vararg args: Any) {
        val logString = String.format(format, *args)
        m_handler.post({
            for (entry in m_onStatuChangedListeners.entries) {
                entry.key.onLogReceived(logString)
            }
        })
    }

    companion object {
        internal lateinit var instance: EGGService
        internal var isRunning: Boolean = false
        var searchText: String = ""
        var isOk: Boolean = false
        var type: Int = Utils.CHECK_LIST

        private val m_onStatuChangedListeners = ConcurrentHashMap<onStatusChangedListener, Any>()

        fun addOnStatusChangedListener(listener: onStatusChangedListener) {
            if (!m_onStatuChangedListeners.containsKey(listener)) {
                m_onStatuChangedListeners.put(listener, 1)
            }
        }

        fun removeOnStatusChangedListener(listener: onStatusChangedListener) {
            if (m_onStatuChangedListeners.containsKey(listener)) {
                m_onStatuChangedListeners.remove(listener)
            }
        }
    }

}