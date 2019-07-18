package com.dadoutek.uled.othersview

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupWindow
import com.dadoutek.uled.R
import com.dadoutek.uled.adapter.AreaItemAdapter
import com.dadoutek.uled.model.DbModel.DbUser
import kotlinx.android.synthetic.main.activity_network.*
import kotlinx.android.synthetic.main.toolbar.*


class NetworkActivity : AppCompatActivity() {

    var dbUser: DbUser? = null
    var adapter: AreaItemAdapter? = null
    var list: List<String>? = null
    var pop: PopupWindow? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_network)
        initView()
        initData()
        initListener()
    }

    private fun initListener() {
        adapter!!.setOnItemClickListener { _, _, position -> setPop(position) }
    }

    private fun setPop(position: Int) {
        var get = list!![position]
        var popView = LayoutInflater.from(this).inflate(R.layout.popwindown_region, null)
        pop = PopupWindow(popView, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        pop!!.isOutsideTouchable = true
        if (!pop!!.isShowing)
            pop!!.showAtLocation(window.decorView, Gravity.BOTTOM, 0, 0)
    }

    private fun initView() {
        toolbarTv.visibility = View.VISIBLE
        toolbarTv.text = getString(R.string.area)
        img_function1.visibility = View.VISIBLE
        image_bluetooth.visibility = View.VISIBLE
    }

    private fun initData() {
        list = listOf("1", "2", "3", "4")
        area_recycleview.layoutManager = LinearLayoutManager(this@NetworkActivity, LinearLayoutManager.VERTICAL, false)
        adapter = AreaItemAdapter(R.layout.item_area_net, list!!)
        area_recycleview.adapter = adapter
    }

    override fun onDestroy() {
        super.onDestroy()
        if (pop!!.isShowing)
            pop!!.dismiss()
    }
}


