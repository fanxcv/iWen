package cn.egg.ui

import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Message
import android.os.Vibrator
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.*
import cn.egg.R
import cn.egg.service.EGGService
import cn.egg.utils.JsonUtils
import cn.egg.utils.NetUtils
import cn.egg.utils.Utils
import com.google.gson.reflect.TypeToken
import okhttp3.OkHttpClient
import java.util.*
import java.util.regex.Pattern
import kotlin.collections.ArrayList
import kotlin.collections.HashSet


class MainActivity : Activity(), EGGService.onStatusChangedListener {

    private lateinit var service: Intent
    private lateinit var watch_btn: Button
    private lateinit var watch_log: TextView
    private lateinit var vibrator: Vibrator
    private lateinit var listView: ListView
    private lateinit var adapter: EGGSimpleAdapter
    private lateinit var searchBox: AutoCompleteTextView
    private lateinit var mInputMethodManager: InputMethodManager

    private var search = ""

    private val context = this
    private val httpClient = OkHttpClient()
    private val listKey = HashSet<String>()
    private val mCalendar = Calendar.getInstance()
    private val listData = ArrayList<HashMap<String, String>>()

    private inner class Data {
        internal var success: String? = null
        internal var message: String? = null
        internal var data: InnerData? = null

        inner class InnerData {
            var docs: ArrayList<String>? = null
            var sessionId: String? = null
            var type: String? = null
        }
    }

    private val dataType = object : TypeToken<Data>() {}.type

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        listView = findViewById(R.id.lineList)
        watch_log = findViewById(R.id.watch_log)
        watch_btn = findViewById(R.id.watch_btn)
        searchBox = findViewById(R.id.searchBox)
        service = Intent(this, EGGService::class.java)
        vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        mInputMethodManager = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager

        searchBox.setOnEditorActionListener { _, code, _ ->
            if (code == EditorInfo.IME_ACTION_SEARCH) {
                if (mInputMethodManager.isActive) mInputMethodManager.hideSoftInputFromWindow(searchBox.windowToken, 0)// 隐藏输入法
                searchBox.dismissDropDown()
                start_watch()
            }
            true
        }

        searchBox.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(p0: Editable?) {
                val text = p0.toString().trim()
                if (text.isNotEmpty() && search != text) {
                    val handler: Handler = @SuppressLint("HandlerLeak")
                    object : Handler() {
                        override fun handleMessage(msg: Message) {
                            super.handleMessage(msg)
                            val list = msg.data.getStringArrayList("list")
                            searchBox.setAdapter(ArrayAdapter<String>(context, android.R.layout.simple_dropdown_item_1line, list))
                            if (Utils.ISDEBUG) Log.d(this.toString(), list.toString())
                            searchBox.showDropDown()
                            search = text
                        }
                    }

                    Thread(Runnable {
                        val msg = Message()
                        val data = Bundle()
                        data.putStringArrayList("list", getSearchText(p0.toString()))
                        msg.data = data
                        handler.sendMessage(msg)
                    }).start()
                }
            }

            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
            }

            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
            }

        })

        watch_btn.setOnClickListener {
            if (EGGService.isRunning) stop_watch()
            else start_watch()
        }

        EGGService.addOnStatusChangedListener(this)
    }

    private fun getSearchText(param: String): ArrayList<String> {
        if (param.isNotEmpty()) {
            val res = NetUtils.sendGet(httpClient, "http://ai.iwencai.com/suggest/v1?qt=condition&q=$param")
            if (res.isNotEmpty()) {
                val data: Data = JsonUtils.GSON.fromJson(res, dataType)
                return data.data?.docs ?: ArrayList()
            }
        }
        return ArrayList()
    }


    private fun start_watch() {
        val text = searchBox.text.toString().trim()
        if (text.isBlank()) return alertMsg("搜索内容不能为空")
        if (EGGService.isRunning) stopService(service)
        val p = Pattern.compile("^[0-9 ]*$")
        val m = p.matcher(text)
        if (m.find()) EGGService.type = Utils.CHECK_ONE else EGGService.type = Utils.CHECK_LIST
        if (Utils.ISDEBUG) Log.d(this.toString(), EGGService.type.toString())
        EGGService.searchText = text
        watch_btn.text = getString(R.string.watch_start)
        listData.clear()
        startService(service)
    }

    private fun stop_watch() {
        if (EGGService.isRunning) {
            watch_btn.text = getString(R.string.watch_stop)
            watch_log.text = getString(R.string.watch_stop)
            stopService(service)
        }
    }

    override fun onStatusChanged(status: String, isRunning: Boolean) {
        Toast.makeText(this, status, Toast.LENGTH_SHORT).show()
    }

    override fun onLogReceived(logString: String) {
        mCalendar.timeInMillis = System.currentTimeMillis()
        val log = String.format("[%1$02d:%2$02d:%3$02d] %4\$s\n",
                mCalendar.get(Calendar.HOUR_OF_DAY),
                mCalendar.get(Calendar.MINUTE),
                mCalendar.get(Calendar.SECOND),
                logString)
        watch_log.text = log
    }

    override fun updateList(data: ArrayList<HashMap<String, String>>) {
        if (data.size > 0) {
            if (Utils.ISDEBUG) Log.d(this.toString(), data.toString())
            if (data.size > 150) for (i in 0 until data.size - 150) data.removeAt(0)
            if (listData.size + data.size < 150) listData.addAll(0, data)
            else {
                for (i in 0 until (listData.size + data.size - 150))
                    listData.removeAt(listData.size - 1)
                listData.addAll(0, data)
            }
            adapter.notifyDataSetChanged()
            vibrator.vibrate(5000)
        }
        EGGService.isOk = true
    }

    override fun initListView(array: ArrayList<HashMap<String, String>>, type: Int) {
        when (type) {
            Utils.CHECK_LIST -> init_list(array)
            Utils.CHECK_ONE -> init_one(array)
        }
        EGGService.isOk = true
    }

    override fun onDestroy() {
        EGGService.removeOnStatusChangedListener(this)
        super.onDestroy()
    }

    private fun init_one(array: ArrayList<HashMap<String, String>>) {
        adapter = EGGSimpleAdapter(this, listData, R.layout.item_one, arrayOf("股票简称", "股票代码", "涨跌幅", "现价", "time"),
                intArrayOf(R.id.name_one, R.id.code_one, R.id.change_one, R.id.price_one, R.id.time_one), Utils.CHECK_ONE)
        listView.adapter = adapter
        listView.setOnItemClickListener { _, view, _, _ ->
            val nameView: TextView = view.findViewById(R.id.name_one)
            val name = nameView.text.toString()
            //获取剪贴板管理器：
            val cm = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            // 创建普通字符型ClipData
            val mClipData = ClipData.newPlainText("Label", name)
            // 将ClipData内容放到系统剪贴板里。
            cm.primaryClip = mClipData
            Toast.makeText(this, "已选择：$name", Toast.LENGTH_SHORT).show()
        }
    }

    private fun init_list(array: ArrayList<HashMap<String, String>>) {
        var flag: Boolean
        var key = "最新涨跌幅"
        for (i in 0 until if (array.size < 5) array.size else 5) {
            flag = false
            for (k in array[i].keys) {
                if (k.contains("涨跌幅")) {
                    flag = true
                    key = k
                    break
                }
            }
            if (flag) break
        }
        val from = arrayOf("股票简称", "股票代码", key, "最新价", "advice", "time")

        adapter = EGGSimpleAdapter(this, listData, R.layout.item, from,
                intArrayOf(R.id.name, R.id.code, R.id.change, R.id.price, R.id.advise, R.id.time), key)
        listView.adapter = adapter
        listView.setOnItemClickListener { _, view, _, _ ->
            val nameView: TextView = view.findViewById(R.id.name)
            val name = nameView.text.toString()
            //获取剪贴板管理器：
            val cm = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            // 创建普通字符型ClipData
            val mClipData = ClipData.newPlainText("Label", name)
            // 将ClipData内容放到系统剪贴板里。
            cm.primaryClip = mClipData
            Toast.makeText(this, "已选择：$name", Toast.LENGTH_SHORT).show()
        }
    }

    private fun alertMsg(msg: String) {
        AlertDialog.Builder(this).setMessage(msg).show()
    }

}
