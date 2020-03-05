package com.dadoutek.uled.gateway

import android.content.Intent
import android.support.v7.widget.LinearLayoutManager
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.CheckBox
import com.blankj.utilcode.util.ToastUtils
import com.dadoutek.uled.R
import com.dadoutek.uled.base.BaseActivity
import kotlinx.android.synthetic.main.activity_event_list.*
import kotlinx.android.synthetic.main.template_recycleview.*
import kotlinx.android.synthetic.main.toolbar.*


/**
 * 创建者     ZCL
 * 创建时间   2020/3/3 14:46
 * 描述
 *
 * 更新者     $
 * 更新时间   $
 * 更新描述
 */
class EventListActivity : BaseActivity() {
    private var lin: View? = null
    val list = mutableListOf<String>()
    val adapter = EventItemAdapter(R.layout.event_item, list)
    override fun setLayoutID(): Int {
        return R.layout.activity_event_list
    }

    override fun initView() {
        toolbarTv.text = getString(R.string.event_list)
        toolbar.setNavigationIcon(R.drawable.icon_top_tab_back)
        //add_device_btn.text = getString(R.string.add)

        img_function1.visibility = View.VISIBLE
        image_bluetooth.setImageResource(R.drawable.icon_bluetooth)
        image_bluetooth.visibility = View.VISIBLE

        lin = LayoutInflater.from(this).inflate(R.layout.add_group, null)
        adapter.addFooterView(lin)

        var emptyView = View.inflate(this, R.layout.empty_view, null)
       var  addBtn =  emptyView.findViewById<Button>(R.id.add_device_btn)
        addBtn.text = getString(R.string.add)
        addBtn.setOnClickListener {
           startActivity(Intent(this, GatewayConfigActivity::class.java))
        }
        adapter.emptyView = emptyView
    }

    override fun initData() {
        template_recycleView.layoutManager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)
        template_recycleView.adapter = adapter

    }

    override fun initListener() {
        toolbar.setNavigationOnClickListener {
            finish()
        }
        event_mode_gp.setOnCheckedChangeListener { _, checkedId ->
                list.clear()
            if (checkedId == R.id.event_timer_mode) {//定時模式
                event_timer_mode.setTextColor(getColor(R.color.blue_text))
                event_time_pattern_mode.setTextColor(getColor(R.color.gray9))
                list.addAll(mutableListOf())
            } else {//時間段模式
                event_timer_mode.setTextColor(getColor(R.color.gray9))
                event_time_pattern_mode.setTextColor(getColor(R.color.blue_text))
                list.addAll(mutableListOf("1","2"))
            }
            adapter.notifyDataSetChanged()
        }
        adapter.setOnItemChildClickListener { _, view, position ->
            when (view.id) {
                R.id.item_event_title, R.id.item_event_week -> {
                    startActivity(Intent(this, GatewayConfigActivity::class.java))
                }
                R.id.item_event_switch -> {
                    if ((view as CheckBox).isChecked)
                        ToastUtils.showLong("选中")
                    else
                        ToastUtils.showLong("未选中")
                }
            }
        }

        lin?.setOnClickListener {    startActivity(Intent(this, GatewayConfigActivity::class.java)) }
    }

}