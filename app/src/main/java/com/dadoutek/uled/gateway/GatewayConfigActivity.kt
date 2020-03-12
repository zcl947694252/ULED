package com.dadoutek.uled.gateway

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.support.v7.widget.DividerItemDecoration
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.View
import android.widget.TextView
import com.blankj.utilcode.util.LogUtils
import com.dadoutek.uled.R
import com.dadoutek.uled.base.TelinkBaseActivity
import com.dadoutek.uled.gateway.adapter.GatewayTimeItemAdapter
import com.dadoutek.uled.gateway.bean.GatewayTagsBean
import com.dadoutek.uled.gateway.bean.GatewayTasksBean
import com.yanzhenjie.recyclerview.touch.OnItemMoveListener
import kotlinx.android.synthetic.main.activity_gate_way.*
import kotlinx.android.synthetic.main.toolbar.*
import org.jetbrains.anko.toast
import java.util.*


/**
 * 创建者     ZCL
 * 创建时间   2020/3/3 17:22
 * 描述
 *
 * 更新者     $
 * 更新时间   $
 * 更新描述
 */
class GatewayConfigActivity : TelinkBaseActivity(), View.OnClickListener {
    private  var tagsBean: GatewayTagsBean? = null
    private var modeIsTimer: Boolean = true
    private var lin: View? = null
    private val requestModeCode: Int = 1000
    private val requestTimeCode: Int = 2000
    private var isCanEdite = false
    private var maxId = 0

    val list = mutableListOf<GatewayTasksBean>()
    private val adapter = GatewayTimeItemAdapter(R.layout.item_gata_way_event_timer, list)
    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.gate_way_repete_mode, R.id.gate_way_repete_mode_arrow -> {
                startActivityForResult(Intent(this@GatewayConfigActivity, GatewayModeChoseActivity::class.java), requestModeCode)
            }
            R.id.gate_way_edite -> {
                isCanEdite = !isCanEdite
                isCanEdite()
            }
        }
    }

    /**
     * 默认重复设置为仅一次
     */
    fun initView() {
        toolbarTv.text = getString(R.string.gate_way)
        gate_way_repete_mode.textSize = 15F
        toolbar.setNavigationIcon(R.drawable.icon_top_tab_back)
        toolbar.setNavigationOnClickListener {
            finish()
        }
        gate_way_repete_mode.text = getString(R.string.only_one)
        modeIsTimer = intent.getBooleanExtra("data", modeIsTimer)
        isCanEdite = false
        isCanEdite()
        swipe_recycleView.layoutManager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)
        swipe_recycleView.addItemDecoration(DividerItemDecoration(this, DividerItemDecoration.HORIZONTAL))
        lin = View.inflate(this, R.layout.template_bottom_add_no_line, null)
        if (modeIsTimer)
            lin?.findViewById<TextView>(R.id.add_group_btn_tv)?.text = getString(R.string.add_time)
        else
            lin?.findViewById<TextView>(R.id.add_group_btn_tv)?.text = getString(R.string.add_times)
    }

    fun initData() {
        swipe_recycleView.adapter = adapter
        adapter.addFooterView(lin)

        //創建新tag任务
        swipe_recycleView.isItemViewSwipeEnabled = true // 侧滑删除，默认关闭。
         tagsBean = GatewayTagsBean((maxId + 1).toLong(),getString(R.string.lable1),getString(R.string.only_one),getWeek(getString(R.string.only_one)))
    }

    private fun getWeek(str: String): Int {
        var week = 0x00000000
        val split = str.split(",").toMutableList()
        var weekDay = when {
            Calendar.getInstance().get(Calendar.DAY_OF_WEEK) -1 == 0 -> getString(R.string.sunday)
            Calendar.getInstance().get(Calendar.DAY_OF_WEEK) -1 == 1 -> getString(R.string.monday)
            Calendar.getInstance().get(Calendar.DAY_OF_WEEK) -1 == 2 -> getString(R.string.tuesday)
            Calendar.getInstance().get(Calendar.DAY_OF_WEEK) -1 == 3 -> getString(R.string.wednesday)
            Calendar.getInstance().get(Calendar.DAY_OF_WEEK) -1 == 4 -> getString(R.string.thursday)
            Calendar.getInstance().get(Calendar.DAY_OF_WEEK) -1 == 5 -> getString(R.string.friday)
            Calendar.getInstance().get(Calendar.DAY_OF_WEEK) -1 == 6 -> getString(R.string.saturday)
            else -> getString(R.string.sunday)
        }//bit位 0-6 周日-周六 7代表当天
        if (split.size==1&&weekDay == split[0]){
            split[0] = getString(R.string.only_one)
            week = week or 0x00000010
            return week
        }else{
            for (s in split){
                when (s) {
                    getString(R.string.sunday)    -> week =week or 0x10000000
                    getString(R.string.monday)    -> week =week or 0x01000000
                    getString(R.string.tuesday)   -> week =week or 0x00100000
                    getString(R.string.wednesday) -> week =week or 0x00010000
                    getString(R.string.thursday)  -> week =week or 0x00001000
                    getString(R.string.friday)    -> week =week or 0x00000100
                    getString(R.string.saturday)  -> week =week or 0x00000010
                }
            }
        }
     return week
    }

    fun initListener() {
        gate_way_edite.setOnClickListener(this)
        gate_way_repete_mode.setOnClickListener(this)
        gate_way_repete_mode_arrow.setOnClickListener(this)
        swipe_recycleView.setOnItemMoveListener(object : OnItemMoveListener {
            override fun onItemDismiss(srcHolder: RecyclerView.ViewHolder?) {
                // 从数据源移除该Item对应的数据，并刷新Adapter。
                val position = srcHolder?.adapterPosition
                list.removeAt(position!!)
                adapter.notifyItemRemoved(position)
                LogUtils.v("zcl------------$list")
            }

            override fun onItemMove(srcHolder: RecyclerView.ViewHolder?, targetHolder: RecyclerView.ViewHolder?): Boolean {
                return false//表示数据移动不成功
            }
        })

        lin?.setOnClickListener {
            when {
                list.size >= 20 -> toast(getString(R.string.gate_way_time_max))
                modeIsTimer -> startActivityForResult(Intent(this@GatewayConfigActivity, GatewayChoseTimeActivity::class.java), requestTimeCode)
                else -> startActivityForResult(Intent(this@GatewayConfigActivity, GatewayChoseTimesActivity::class.java), requestTimeCode)
            }

        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == requestModeCode) {
                val mode = data?.getStringExtra("data")
                if (mode!!.contains("6")) {
                    gate_way_repete_mode.textSize = 13F
                    gate_way_repete_mode.text = mode.replace("6", "")
                    tagsBean?.week = getWeek(mode.replace("6", ""))
                } else {
                    gate_way_repete_mode.textSize = 15F
                    gate_way_repete_mode.text = mode
                    tagsBean?.week = getWeek(mode)
                }

            } else if (requestCode == requestTimeCode) {
                val bean = data?.getParcelableExtra<GatewayTasksBean>("data")
                bean?.let {
                    if (bean.isCreateNew) {
                        if (list.size == 0)
                            list.add(bean)
                        else
                            for (timeBean in list) {
                                if (timeBean.startHour == bean.startHour && timeBean.startMinute == bean.startMinute) {
                                    toast(getString(R.string.timer_exists))
                                    break
                                } else
                                    list.add(bean)
                            }
                    } else {
                        var targetPosition: Int
                        for (i in 0 until list.size) {
                            val timeBean = list[i]
                            if (timeBean.label_id == bean.label_id) {//存在
                                targetPosition = i
                                list.removeAt(targetPosition)
                                list.add(bean)
                                break
                            } else {
                                toast(getString(R.string.invalid_data))
                            }
                        }
                        list.sortBy { it.startHour }
                    }
                    adapter.notifyDataSetChanged()
                }
            }
        }
    }

    private fun isCanEdite() {
        if (isCanEdite) {
            gate_way_title.isFocusableInTouchMode = true//不可编辑
            gate_way_title.isClickable = true//不可点击，但是这个效果我这边没体现出来，不知道怎没用
            gate_way_title.isFocusable = true//不可编辑
            gate_way_title.isEnabled = true
            gate_way_title.requestFocus()
            gate_way_title.setSelection(gate_way_title.text.length)//将光标移至文字末尾
        } else {
            gate_way_title.isFocusableInTouchMode = false//不可编辑
            gate_way_title.isClickable = false//不可点击
            gate_way_title.isFocusable = false//不可编辑
            gate_way_title.isEnabled = false
            gate_way_title.background = null
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_gate_way)
        initView()
        initData()
        initListener()
    }
}