package cn.egg.utils

import java.io.DataOutputStream
import java.math.BigDecimal
import java.util.*


object Utils {
    internal val ISDEBUG = false
    internal val CHECK_LIST = 1
    internal val CHECK_ONE = 2


    internal val COMMAND_AIRPLANE_ON = "settings put global airplane_mode_on 1 \n " + "am broadcast -a android.intent.action.AIRPLANE_MODE --ez state true\n "
    internal val COMMAND_AIRPLANE_OFF = "settings put global airplane_mode_on 0 \n" + " am broadcast -a android.intent.action.AIRPLANE_MODE --ez state false\n "
    private val COMMAND_SU = "su"

    private val mCalendar = Calendar.getInstance()
    private const val RANDOM_STR = "1234567890"
    fun execId(length: Int = 5): String {
        val sb = StringBuilder().append("f24492ff06d54e0e01e7ccef18aa09c515155")
        for (i in 1..length) {
            sb.append(RANDOM_STR[(Math.random() * 10).toInt()])
        }
        return sb.toString()
    }

    fun dealData(old: ArrayList<HashMap<String, String>>, new: ArrayList<HashMap<String, String>>): ArrayList<HashMap<String, String>> {
        mCalendar.timeInMillis = System.currentTimeMillis()
        val time = String.format("%1$02d:%2$02d:%3$02d",
                mCalendar.get(Calendar.HOUR_OF_DAY),
                mCalendar.get(Calendar.MINUTE),
                mCalendar.get(Calendar.SECOND))
        val resList = ArrayList<HashMap<String, String>>()
        val maxList: ArrayList<HashMap<String, String>>
        val minList: ArrayList<HashMap<String, String>>
        val maxStr: String
        val minStr: String
        val flag: Boolean = new.size > old.size
        if (flag) {
            maxList = new
            minList = old
            maxStr = "建议买入"
            minStr = "建议卖出"
        } else {
            maxList = old
            minList = new
            maxStr = "建议卖出"
            minStr = "建议买入"
        }
        val size = maxList.size
        val map = HashMap<String, Int>(size)
        val max_map = HashMap<String, HashMap<String, String>>(size)
        maxList.forEach { v ->
            map[v["股票代码"] ?: ""] = 1
            max_map[v["股票代码"] ?: ""] = v
        }

        minList.forEach { v ->
            if (map[v["股票代码"] ?: ""] != null) map[v["股票代码"] ?: ""] = 2
            else {
                v["advice"] = minStr
                v["time"] = time
                resList.add(v)
            }
        }

        map.forEach { k, v ->
            if (v == 1) {
                val m = max_map[k]
                if (m != null) {
                    m["advice"] = maxStr
                    m["time"] = time
                    resList.add(m)
                }
            }
        }

        return resList
    }

    fun dealDataOne(old: HashMap<String, HashMap<String, String>>, new: HashMap<String, HashMap<String, String>>): ArrayList<HashMap<String, String>> {
        mCalendar.timeInMillis = System.currentTimeMillis()
        val time = String.format("%1$02d:%2$02d:%3$02d",
                mCalendar.get(Calendar.HOUR_OF_DAY),
                mCalendar.get(Calendar.MINUTE),
                mCalendar.get(Calendar.SECOND))
        val resList = ArrayList<HashMap<String, String>>()

        val x: BigDecimal = BigDecimal("1.0")
        val c: BigDecimal = BigDecimal("-1.0")
        new.forEach { k, v ->
            val n = BigDecimal(v["涨跌幅"] ?: "0")
            val o = BigDecimal(old[k]?.get("涨跌幅") ?: "0")
            val r = o.subtract(n)
            if (r.compareTo(x) == 1 || r.compareTo(c) == -1) {
                v["time"] = time
                resList.add(v)
                old.put(k, v)
            }
        }
        return resList
    }

    internal fun map2List(map: HashMap<String, HashMap<String, String>>): ArrayList<HashMap<String, String>> {
        val resList = ArrayList<HashMap<String, String>>()
        map.forEach { _, v ->
            resList.add(v)
        }
        return resList
    }

    fun writeCmd(command: String) {
        try {
            val su = Runtime.getRuntime().exec(COMMAND_SU)
            val outputStream = DataOutputStream(su.outputStream)
            outputStream.writeBytes("$command\nexit\n")
            outputStream.flush()
            outputStream.close()

            su.waitFor()
        } catch (e: Exception) {
            e.printStackTrace()
        }

    }
}