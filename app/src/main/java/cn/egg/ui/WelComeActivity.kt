package cn.egg.ui

import android.app.Activity
import android.content.Intent
import android.os.Bundle

import cn.egg.R
import cn.egg.utils.Utils

class WelComeActivity : Activity() {

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.welcome)
        Utils.writeCmd("cd")
        start()
    }


    private fun start() {
        val intent = Intent()
        intent.setClass(this@WelComeActivity, MainActivity::class.java)
        startActivity(intent)
        finish()
    }
}
