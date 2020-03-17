package com.dadoutek.uled.gateway

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.support.v7.widget.LinearLayoutManager
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.CheckBox
import android.widget.TextView
import com.blankj.utilcode.util.GsonUtils
import com.blankj.utilcode.util.ToastUtils
import com.dadoutek.uled.R
import com.dadoutek.uled.base.TelinkBaseActivity
import com.dadoutek.uled.gateway.adapter.EventItemAdapter
import com.dadoutek.uled.gateway.bean.DbGateway
import com.dadoutek.uled.gateway.bean.GatewayTagBean
import com.dadoutek.uled.gateway.util.GsonUtil
import com.dadoutek.uled.model.Constant
import com.dadoutek.uled.model.DbModel.DBUtils
import com.dadoutek.uled.model.SharedPreferencesHelper
import kotlinx.android.synthetic.main.activity_event_list.*
import kotlinx.android.synthetic.main.template_recycleview.*
import kotlinx.android.synthetic.main.toolbar.*
import org.jetbrains.anko.toast


/**
 * 创建者     ZCL
 * 创建时间   2020/3/3 14:46
 * 描述
 *
 * 更新者     $
 * 更新时间   $
 * 更新描述
 */
class GateWayEventListActivity : TelinkBaseActivity() {
    private var dbGateway: DbGateway? = null
    private var addBtn: Button? = null
    private var lin: View? = null
    val list = mutableListOf<GatewayTagBean>()
    val adapter = EventItemAdapter(R.layout.event_item, list)

    fun initView() {
        toolbarTv.text = getString(R.string.event_list)
        toolbar.setNavigationIcon(R.drawable.icon_top_tab_back)

        img_function1.visibility = View.VISIBLE
        image_bluetooth.setImageResource(R.drawable.icon_bluetooth)
        image_bluetooth.visibility = View.VISIBLE

        lin = LayoutInflater.from(this).inflate(R.layout.template_bottom_add_no_line, null)
        lin?.findViewById<TextView>(R.id.add_group_btn_tv)?.text = getString(R.string.add)
        adapter.addFooterView(lin)

        var emptyView = View.inflate(this, R.layout.empty_view, null)
        addBtn = emptyView.findViewById<Button>(R.id.add_device_btn)
        addBtn?.text = getString(R.string.add)


        adapter.emptyView = emptyView
    }

    fun initData() {
        dbGateway = intent.getParcelableExtra<DbGateway>("data")
        if (dbGateway == null) {
            ToastUtils.showShort(getString(R.string.no_get_device_info))
            finish()
        }
        template_recycleView.layoutManager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)
        template_recycleView.adapter = adapter
    }

    override fun onResume() {
        super.onResume()
        dbGateway = DBUtils.getGatewayByID(dbGateway!!.id)
        if (!TextUtils.isEmpty(dbGateway!!.tags)) {
            list.clear()
            val toList = GsonUtil.stringToList(dbGateway!!.tags, GatewayTagBean::class.java)
            list.addAll(toList)
            adapter.notifyDataSetChanged()
        }

    }

    @SuppressLint("SetTextI18n")
    fun initListener() {
        toolbar.setNavigationOnClickListener {
            finish()
        }
        addBtn?.setOnClickListener {
            //已有网关设备空界面跳转添加事件
            addNewTag()
        }
        lin?.setOnClickListener {//底部添加
            addNewTag()
        }
        event_mode_gp.setOnCheckedChangeListener { rg, checkedId ->
            list.clear()
            if (checkedId == R.id.event_timer_mode) {//定時模式  0定时 1循环
                dbGateway?.type = 0
                event_timer_mode.setTextColor(getColor(R.color.blue_text))
                event_time_pattern_mode.setTextColor(getColor(R.color.gray9))
                val es = DBUtils.getAllGateWay()
                if (es.size > 0) {
                    val toList = GsonUtil.stringToList(es[0].tags, GatewayTagBean::class.java)
                    for (g in toList)
                        g.tagName = dbGateway?.name
                    list.addAll(toList)
                } else {
                    list.add(GatewayTagBean(1, "122", "12", 1))
                }
            } else {//時間段模式
                dbGateway?.type = 1
                event_timer_mode.setTextColor(getColor(R.color.gray9))
                event_time_pattern_mode.setTextColor(getColor(R.color.blue_text))
                val es = DBUtils.getAllGateWay()
                if (es.size > 0) {
                    val toList = GsonUtil.stringToList(es[0].tags, GatewayTagBean::class.java)
                    for (g in toList) {
                        g.tagName = dbGateway?.name
                    }
                    list.addAll(toList)
                } else list.add(GatewayTagBean(1, "122", "12", 1))
            }
            adapter.notifyDataSetChanged()
        }
        adapter.setOnItemChildClickListener { _, view, position ->
            when (view.id) {
                R.id.item_event_ly -> {
                    val intent = Intent(this, GatewayConfigActivity::class.java)
                     dbGateway?.pos = position
                    SharedPreferencesHelper.putBoolean(this,Constant.IS_NEW_TAG,false)
                    dbGateway?.tags = GsonUtils.toJson(list)//赋值时一定要转换为gson字符串
                    intent.putExtra("data", dbGateway)
                    startActivity(intent)
                }
                R.id.item_event_switch -> {
                    if ((view as CheckBox).isChecked) {
                        list[position].status = 1
                    } else {
                        list[position].status = 0
                    }
                    dbGateway?.tags = list.toString()
                    DBUtils.saveGateWay(dbGateway!!, true)
                }
            }
        }
    }

    private fun addNewTag() {
        if (list.size >= 20)
            toast(getString(R.string.gate_way_time_max))
        else {
            val intent = Intent(this@GateWayEventListActivity, GatewayConfigActivity::class.java)
            SharedPreferencesHelper.putBoolean(this,Constant.IS_NEW_TAG,true)
            intent.putExtra("data", dbGateway)
            startActivity(intent)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_event_list)

        initView()
        initData()
        initListener()
    }
}