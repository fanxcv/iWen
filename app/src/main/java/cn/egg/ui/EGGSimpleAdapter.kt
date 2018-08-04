package cn.egg.ui

import android.content.Context
import android.graphics.Color
import android.view.View
import android.view.ViewGroup
import android.widget.SimpleAdapter
import android.widget.TextView
import cn.egg.R
import cn.egg.utils.Utils
import java.util.*

class EGGSimpleAdapter(context: Context, private val listitem: ArrayList<HashMap<String, String>>, resource: Int, from: Array<String>, to: IntArray) : SimpleAdapter(context, listitem, resource, from, to) {

    private var key = "最新涨跌幅"
    private var type = Utils.CHECK_LIST
    private val colors1 = intArrayOf(Color.parseColor("#444444"), Color.parseColor("#333333"))
    private val colors2 = intArrayOf(Color.parseColor("#00FF00"), Color.parseColor("#FF0000"), Color.parseColor("#FFFFFF"))

    constructor(context: Context, listitem: ArrayList<HashMap<String, String>>, resource: Int, from: Array<String>, to: IntArray, key: String, type: Int = 1) : this(context, listitem, resource, from, to) {
        this.type = type
        this.key = key
    }

    constructor(context: Context, listitem: ArrayList<HashMap<String, String>>, resource: Int, from: Array<String>, to: IntArray, type: Int = 1) : this(context, listitem, resource, from, to) {
        this.type = type
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {

        val view = super.getView(position, convertView, parent)

        //下面设置隔行背景变色效果。
        val colorPos = position % colors1.size
        view.setBackgroundColor(colors1[colorPos])

        when (type) {
            Utils.CHECK_LIST -> {
                val change = view.findViewById<TextView>(R.id.change)
                val advice = view.findViewById<TextView>(R.id.advise)
                val changeVal = listitem[position][key]
                val adviceVal = listitem[position]["advice"]

                change.setTextColor(
                        if (changeVal?.startsWith("-") == true) if (changeVal == "--") colors2[2] else colors2[0]
                        else if (changeVal == "0.000") colors2[2] else colors2[1])
                advice.setTextColor(if (adviceVal == "建议卖出") colors2[0] else colors2[1])
            }
            Utils.CHECK_ONE -> {
                val change = view.findViewById<TextView>(R.id.change_one)
                val changeVal = listitem[position]["涨跌幅"]
                change.setTextColor(
                        if (changeVal?.startsWith("-") == true) if (changeVal == "--") colors2[2] else colors2[0]
                        else if (changeVal == "0.000") colors2[2] else colors2[1])
            }
        }
        return view
    }

}