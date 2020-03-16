package com.dadoutek.uled.gateway

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.support.v7.widget.LinearLayoutManager
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.CheckBox
import android.widget.TextView
import com.blankj.utilcode.util.ToastUtils
import com.dadoutek.uled.R
import com.dadoutek.uled.base.TelinkBaseActivity
import com.dadoutek.uled.gateway.adapter.EventItemAdapter
import com.dadoutek.uled.gateway.bean.DbGateway
import com.dadoutek.uled.gateway.bean.GatewayTagBean
import com.dadoutek.uled.gateway.util.GsonUtil
import com.dadoutek.uled.model.DbModel.DBUtils
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
    private var modeIsTimer: Boolean = true
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

        template_recycleView.layoutManager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)
        template_recycleView.adapter = adapter

    }

    @SuppressLint("SetTextI18n")
    fun initListener() {
        toolbar.setNavigationOnClickListener {
            finish()
        }
        addBtn?.setOnClickListener {
            //已有网关设备空界面跳转添加事件
            val intent = Intent(this@GateWayEventListActivity, GatewayConfigActivity::class.java)
            intent.putExtra("mode", modeIsTimer)
            startActivity(intent)
        }
        lin?.setOnClickListener {
            if (list.size >= 20)
                toast(getString(R.string.gate_way_time_max))
            else {
                val intent = Intent(this, GatewayConfigActivity::class.java)
                intent.putExtra("mode", modeIsTimer)
                startActivity(intent)
            }
        }
        event_mode_gp.setOnCheckedChangeListener { rg, checkedId ->
            list.clear()
            modeIsTimer = rg.checkedRadioButtonId == R.id.event_timer_mode

            if (checkedId == R.id.event_timer_mode) {//定時模式
                event_timer_mode.setTextColor(getColor(R.color.blue_text))
                event_time_pattern_mode.setTextColor(getColor(R.color.gray9))
                val es = DBUtils.getAllGateWay()
                if (es.size > 0) {
                    val toList = GsonUtil.stringToList(es[0].tags, GatewayTagBean::class.java)
                    for (g in toList){
                        g.tagName = dbGateway?.name
                    }
                    if (toList.size == 0) {
                        list.addAll(toList)
                    } else {
                        list.addAll(toList)
                    }
                } else {
                    list.add(GatewayTagBean(1,"122","12",1))
                }
            } else {//時間段模式
                event_timer_mode.setTextColor(getColor(R.color.gray9))
                event_time_pattern_mode.setTextColor(getColor(R.color.blue_text))
                // listTask.addAll(mutableListOf(DbGateway(), DbGateway()))
            }

            adapter.notifyDataSetChanged()
        }
        adapter.setOnItemChildClickListener { _, view, position ->
            when (view.id) {
                R.id.item_event_ly -> {
                    val intent = Intent(this, GatewayConfigActivity::class.java)
                    intent.putExtra("mode", modeIsTimer)
                    intent.putExtra("data", list[position])
                    startActivity(intent)
                }
                R.id.item_event_switch -> {
                    if ((view as CheckBox).isChecked) {
                        //  listTask[position].type = 1
                        ToastUtils.showLong("选中")
                    } else {
                        //listTask[position].type = 0
                        ToastUtils.showLong("未选中")
                    }
                    //  DBUtils.saveGateWay(listTask[position],true)
                }
            }
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