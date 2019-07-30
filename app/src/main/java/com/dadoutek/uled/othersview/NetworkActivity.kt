package com.dadoutek.uled.othersview

import android.annotation.SuppressLint
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
import com.dadoutek.uled.model.Constant
import com.dadoutek.uled.model.DbModel.DBUtils
import com.dadoutek.uled.model.DbModel.DbRegion
import com.dadoutek.uled.model.DbModel.DbUser
import com.dadoutek.uled.model.HttpModel.RegionModel
import com.dadoutek.uled.util.PopUtil
import kotlinx.android.synthetic.main.activity_network.*
import kotlinx.android.synthetic.main.toolbar.*


class NetworkActivity : AppCompatActivity(), View.OnClickListener {
    private lateinit var popAdd: PopupWindow
    private var viewAdd: View? = null
    var dbUser: DbUser? = null
    var adapter: AreaItemAdapter? = null
    var list: List<DbRegion>? = null
    var pop: PopupWindow? = null
    var view: View? = null
    var root: View? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_network)
        root = View.inflate(this, R.layout.activity_network, null)

        initView()
        initData()
    }

    private fun initView() {
        toolbarTv.visibility = View.VISIBLE
        toolbarTv.text = getString(R.string.area)
        img_function1.visibility = View.VISIBLE
        image_bluetooth.visibility = View.VISIBLE
        image_bluetooth.setImageResource(R.mipmap.icon_scanning)

        dbUser = DBUtils.lastUser
        //("zcl**********************${dbUser.toString()}")
        makePop()
        initListener()
    }

    private fun initListener() {
        img_function1.setOnClickListener {  }
        image_bluetooth.setOnClickListener {  }
    }

    private fun addRegion() {
        val dbRegion = DbRegion()
        dbRegion.installMesh = Constant.PIR_SWITCH_MESH_NAME
        dbRegion.installMeshPwd = "123"

        //RegionModel.add(dbUser!!.token,dbRegion,)
    }

    @SuppressLint("CheckResult")
    private fun initData() {
        if (dbUser != null) {
            RegionModel.get(dbUser!!.token)?.subscribe { it ->
                area_account.text =getString(R.string.current_account)+": +"+ dbUser!!.phone
                list = it
                area_recycleview.layoutManager = LinearLayoutManager(this@NetworkActivity, LinearLayoutManager.VERTICAL, false)
                adapter = AreaItemAdapter(R.layout.item_area_net, it!!, dbUser!!.last_region_id)
                adapter!!.setOnItemClickListener { _, _, position -> setPop(position) }
                area_recycleview.adapter = adapter
            }
        }
    }

    private fun makePop() {
        view = View.inflate(this, R.layout.popwindown_region, null)
        viewAdd = View.inflate(this, R.layout.popwindown_add_region, null)

        pop = PopupWindow(view, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        popAdd = PopupWindow(viewAdd, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)

        setPopSetting(pop!!)
        setPopSetting(popAdd!!)

        view?.let {
            it.findViewById<RelativeLayout>(R.id.pop_view).setOnClickListener(this)
            it.findViewById<ConstraintLayout>(R.id.pop_net_ly).setOnClickListener(this)
            it.findViewById<ImageView>(R.id.pop_user_net).setOnClickListener(this)
            it.findViewById<ImageView>(R.id.pop_del_net).setOnClickListener(this)
            it.findViewById<ImageView>(R.id.pop_share_net).setOnClickListener(this)
            it.findViewById<ImageView>(R.id.pop_update_net).setOnClickListener(this)
            it.findViewById<TextView>(R.id.pop_creater_name).text = getString(R.string.creater_name)+ dbUser!!.phone
            //  it.findViewById<ImageView>(R.id.pop_qr_img).setOnClickListener(this)
        }
    }

    private fun setPopSetting(pop: PopupWindow) {
        pop?.let {
           pop!!.isOutsideTouchable = true
           pop!!.isFocusable = true // 设置PopupWindow可获得焦点
           pop!!.isTouchable = true // 设置PopupWindow可触摸补充：
        }
    }

    private fun setPop(position: Int) {
        val dbRegion = list!![position]
        view?.let {
            it.findViewById<TextView>(R.id.pop_net_name).text =dbRegion.name
            it.findViewById<TextView>(R.id.pop_equipment_num).text = getString(R.string.equipment_quantity)+ list!!.size
            if (dbRegion.id.toString() == dbUser!!.last_region_id)
                it.findViewById<ImageView>(R.id.pop_user_net).setImageResource(R.drawable.icon_use_blue)
            else
                it.findViewById<ImageView>(R.id.pop_user_net).setImageResource(R.drawable.icon_use)
        }
        root?.let { PopUtil.show(pop, it, Gravity.BOTTOM) }
    }

    override fun onClick(v: View?) {
        when (v!!.id) {
            R.id.pop_view -> {
                PopUtil.dismiss(pop)
            }
            R.id.pop_net_ly -> {
                view!!.findViewById<TextView>(R.id.pop_net_name).text = "11"
            }
            R.id.pop_user_net -> {
                view!!.findViewById<TextView>(R.id.pop_net_name).text = "22"
            }
            R.id.pop_del_net -> {
                view!!.findViewById<TextView>(R.id.pop_net_name).text = "33"
            }
            R.id.pop_share_net -> {
                view!!.findViewById<TextView>(R.id.pop_net_name).text = "444"
                getQr()
            }
            R.id.pop_update_net -> {

            }
        }
    }

    private fun getQr() {

    }

    override fun onDestroy() {
        super.onDestroy()
        PopUtil.dismiss(pop)
    }
}


