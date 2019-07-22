package com.dadoutek.uled.othersview

import android.os.Bundle
import android.support.constraint.ConstraintLayout
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.PopupWindow
import android.widget.RelativeLayout
import android.widget.TextView
import com.dadoutek.uled.R
import com.dadoutek.uled.adapter.AreaItemAdapter
import com.dadoutek.uled.model.DbModel.DbUser
import com.dadoutek.uled.util.PopUtil
import kotlinx.android.synthetic.main.activity_network.*
import kotlinx.android.synthetic.main.toolbar.*


class NetworkActivity : AppCompatActivity(), View.OnClickListener {
    var dbUser: DbUser? = null
    var adapter: AreaItemAdapter? = null
    var list: List<String>? = null
    var pop: PopupWindow? = null
    var view: View? = null
    var root: View? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_network)
        root = View.inflate(this, R.layout.activity_network, null)

        initView()
        initData()
        initListener()
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

    private fun initListener() {
        adapter!!.setOnItemClickListener { _, _, position -> setPop(position) }
    }

    private fun setPop(position: Int) {
        view = View.inflate(this, R.layout.popwindown_region, null)
        pop = PopupWindow(view, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        pop?.let {
            pop!!.isOutsideTouchable = true
            pop!!.isFocusable = true // 设置PopupWindow可获得焦点
            pop!!.isTouchable = true // 设置PopupWindow可触摸补充：
        }

        view?.let {
            it.findViewById<RelativeLayout>(R.id.pop_view).setOnClickListener(this)
            it.findViewById<ConstraintLayout>(R.id.pop_net_ly).setOnClickListener(this)
            it.findViewById<ImageView>(R.id.pop_user_net).setOnClickListener(this)
            it.findViewById<ImageView>(R.id.pop_del_net).setOnClickListener(this)
            it.findViewById<ImageView>(R.id.pop_share_net).setOnClickListener(this)
            it.findViewById<ImageView>(R.id.pop_update_net).setOnClickListener(this)
            //  it.findViewById<ImageView>(R.id.pop_qr_img).setOnClickListener(this)
        }
        var get = list?.get(position)
        root?.let { PopUtil.show(pop, it, Gravity.BOTTOM) }
    }

    override fun onClick(v: View?) {
        when (v!!.id) {
            R.id.pop_view -> {
                PopUtil.dismiss(pop)
            }
            R.id.pop_net_ly -> {
                view!!.findViewById<TextView>(R.id.pop_net_top).text = "11"
            }
            R.id.pop_user_net -> {
                view!!.findViewById<TextView>(R.id.pop_net_top).text = "22"
            }
            R.id.pop_del_net -> {
                view!!.findViewById<TextView>(R.id.pop_net_top).text = "33"
            }
            R.id.pop_share_net -> {
                view!!.findViewById<TextView>(R.id.pop_net_top).text = "444"
                getQr()
            }
            R.id.pop_update_net -> {

            }
            // R.id.pop_qr_img -> {
            //     LogUtils.e("15989416")
            // }
        }
    }

    private fun getQr() {

    }

    override fun onDestroy() {
        super.onDestroy()
        PopUtil.dismiss(pop)
    }
}


