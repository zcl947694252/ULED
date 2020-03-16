package com.dadoutek.uled.gateway

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.support.v7.widget.DividerItemDecoration
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.View
import android.widget.TextView
import com.blankj.utilcode.util.GsonUtils
import com.blankj.utilcode.util.LogUtils
import com.dadoutek.uled.R
import com.dadoutek.uled.base.TelinkBaseActivity
import com.dadoutek.uled.gateway.adapter.GatewayTimeItemAdapter
import com.dadoutek.uled.gateway.bean.DbGateway
import com.dadoutek.uled.gateway.bean.GatewayTagBean
import com.dadoutek.uled.gateway.bean.GatewayTasksBean
import com.dadoutek.uled.model.DbModel.DBUtils
import com.dadoutek.uled.model.DeviceType
import com.yanzhenjie.recyclerview.touch.OnItemMoveListener
import kotlinx.android.synthetic.main.activity_gate_way.*
import kotlinx.android.synthetic.main.template_bottom_add_no_line.*
import kotlinx.android.synthetic.main.toolbar.*
import org.jetbrains.anko.toast
import java.util.*
import kotlin.collections.ArrayList

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
    private var tagsBean: GatewayTagBean? = null
    private var modeIsTimer: Boolean = true
    private val requestModeCode: Int = 1000//选择日期
    private val requestTimeCode: Int = 2000//时间段模式
    private var isCanEdite = false
    private var maxId = 0

    val listTask = ArrayList<GatewayTasksBean>()
    private val adapter = GatewayTimeItemAdapter(R.layout.item_gata_way_event_timer, listTask)
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
        toolbarTv.text = getString(R.string.Gate_way)
        gate_way_repete_mode.textSize = 15F
        toolbar.setNavigationIcon(R.drawable.icon_top_tab_back)
        toolbar.setNavigationOnClickListener {
            finish()
        }
        tv_function1.text = getString(R.string.complete)
        tv_function1.visibility = View.VISIBLE
        gate_way_repete_mode.text = getString(R.string.only_one)
        modeIsTimer = intent.getBooleanExtra("data", modeIsTimer)
        isCanEdite = false
        isCanEdite()
        swipe_recycleView.layoutManager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)
        swipe_recycleView.addItemDecoration(DividerItemDecoration(this, DividerItemDecoration.HORIZONTAL))

        if (modeIsTimer)
            add_group_btn?.findViewById<TextView>(R.id.add_group_btn_tv)?.text = getString(R.string.add_time)
        else
            add_group_btn?.findViewById<TextView>(R.id.add_group_btn_tv)?.text = getString(R.string.add_times)

    }

    fun initData() {
        swipe_recycleView.adapter = adapter
        swipe_recycleView.isItemViewSwipeEnabled = true // 侧滑删除，默认关闭。
        tagsBean = intent.getParcelableExtra("data") ?: //創建新tag任务
                GatewayTagBean((maxId + 1).toLong(), getString(R.string.tag1), getString(R.string.only_one), getWeek(getString(R.string.only_one)))
        LogUtils.v("zcl-----------网关tag信息-------$tagsBean")
    }

    private fun getWeek(str: String): Int {
        var week = 0x00000000
        when (str) {
            getString(R.string.only_one) -> {
                week = 0x10000000
                return week
            }
            getString(R.string.every_day) -> {
                week = 0x01111111
                return week
            }
            else -> {
                val split = str.split(",").toMutableList()
                for (s in split) {
                    when (s) {//bit位 0-6 周日-周六 7代表当天
                        getString(R.string.sunday) -> week = week or 0x00000001
                        getString(R.string.monday) -> week = week or 0x00000010
                        getString(R.string.tuesday) -> week = week or 0x00000100
                        getString(R.string.wednesday) -> week = week or 0x00001000
                        getString(R.string.thursday) -> week = week or 0x00010000
                        getString(R.string.friday) -> week = week or 0x00100000
                        getString(R.string.saturday) -> week = week or 0x01000000
                    }
                }
                return week
            }
        }
    }

    fun initListener() {
        tv_function1.setOnClickListener {
            val dbGateway = DbGateway()
            dbGateway.name = gate_way_lable.text.toString()
            dbGateway.type = 0
            dbGateway.productUUID = DeviceType.GATE_WAY

            val toJson = GsonUtils.toJson(listTask)//获取tasks字符串
            LogUtils.v("zcl网关---$toJson")

            tagsBean?.tasks = listTask
            //因为是新创建的对象所以直接创建list添加 如果不是需要查看是都有tags
            val tJ = GsonUtils.toJson(mutableListOf(tagsBean))
            dbGateway.tags = tJ

            DBUtils.saveGateWay(dbGateway, false)
            finish()
        }
        gate_way_edite.setOnClickListener(this)
        gate_way_repete_mode.setOnClickListener(this)
        gate_way_repete_mode_arrow.setOnClickListener(this)
        swipe_recycleView.setOnItemMoveListener(object : OnItemMoveListener {
            override fun onItemDismiss(srcHolder: RecyclerView.ViewHolder?) {
                // 从数据源移除该Item对应的数据，并刷新Adapter。
                val position = srcHolder?.adapterPosition
                listTask.removeAt(position!!)
                adapter.notifyItemRemoved(position)
                LogUtils.v("zcl------------$listTask")
            }

            override fun onItemMove(srcHolder: RecyclerView.ViewHolder?, targetHolder: RecyclerView.ViewHolder?): Boolean {
                return false//表示数据移动不成功
            }
        })

        add_group_btn?.setOnClickListener {
            val index = getIndex()
            when {
                listTask.size > 20 -> toast(getString(R.string.gate_way_time_max))
                index == 0 -> toast(getString(R.string.gate_way_time_max))
                modeIsTimer -> {
                    val intent = Intent(this@GatewayConfigActivity, GatewayChoseTimeActivity::class.java)
                    intent.putExtra("index", index)
                    startActivityForResult(intent, requestTimeCode)
                }
                else -> {
                    val intent = Intent(this@GatewayConfigActivity, GatewayChoseTimesActivity::class.java)
                    intent.putExtra("index", index)
                    startActivityForResult(intent, requestTimeCode)
                }
            }
        }
    }

    private fun getIndex(): Int {
        var index = 0
        if (listTask.size == 0) {
            index = 1
        } else {
            for (j in 1..20) {
                var isUsed = false
                for (i in listTask) {
                    if (i.index == j) {//已用 跳出循环
                        isUsed = true
                        break
                    }
                }
                if (!isUsed) {
                    index = j
                    break
                }
            }
        }
        return index
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == requestModeCode) {//选择重复日期
                var mode = data?.getStringExtra("data")
                if (mode!!.contains("6")) {
                    gate_way_repete_mode.textSize = 13F
                    gate_way_repete_mode.text = mode.replace("6", "")
                    tagsBean?.week = getWeek(mode.replace("6", ""))
                } else {
                    gate_way_repete_mode.textSize = 15F
                    mode = isOnlyOne(mode)
                    gate_way_repete_mode.text = mode

                    tagsBean?.week = getWeek(mode)
                }

            } else if (requestCode == requestTimeCode) {//获取task
                val bean = data?.getParcelableExtra<GatewayTasksBean>("data")
                bean?.let {
                    if (bean.isCreateNew) {
                        if (listTask.size == 0)
                            listTask.add(bean)
                        else
                            for (timeBean in listTask) {
                                if (timeBean.startHour == bean.startHour && timeBean.startMins == bean.startMins) {
                                    toast(getString(R.string.timer_exists))
                                    break
                                } else
                                    listTask.add(bean)
                            }
                    } else {
                        var targetPosition: Int
                        for (i in 0 until listTask.size) {
                            val timeBean = listTask[i]
                            if (timeBean.index == bean.index) {//存在
                                targetPosition = i
                                listTask.removeAt(targetPosition)
                                listTask.add(bean)
                                break
                            } else {
                                toast(getString(R.string.invalid_data))
                            }
                        }
                        listTask.sortBy { it.startHour }
                    }
                    adapter.notifyDataSetChanged()
                }
            }
        }
    }

    private fun isOnlyOne(mode: String): String {
        val split = mode.split(",").toMutableList()
        var weekDay = when {
            Calendar.getInstance().get(Calendar.DAY_OF_WEEK) - 1 == 0 -> getString(R.string.sunday)
            Calendar.getInstance().get(Calendar.DAY_OF_WEEK) - 1 == 1 -> getString(R.string.monday)
            Calendar.getInstance().get(Calendar.DAY_OF_WEEK) - 1 == 2 -> getString(R.string.tuesday)
            Calendar.getInstance().get(Calendar.DAY_OF_WEEK) - 1 == 3 -> getString(R.string.wednesday)
            Calendar.getInstance().get(Calendar.DAY_OF_WEEK) - 1 == 4 -> getString(R.string.thursday)
            Calendar.getInstance().get(Calendar.DAY_OF_WEEK) - 1 == 5 -> getString(R.string.friday)
            Calendar.getInstance().get(Calendar.DAY_OF_WEEK) - 1 == 6 -> getString(R.string.saturday)
            else -> getString(R.string.sunday)
        }//bit位 0-6 周日-周六 7代表当天
        return if (split.size == 1 && weekDay == split[0])
            getString(R.string.only_one)
        else
            mode
    }

    private fun isCanEdite() {
        if (isCanEdite) {
            gate_way_lable.isFocusableInTouchMode = true//不可编辑
            gate_way_lable.isClickable = true//不可点击，但是这个效果我这边没体现出来，不知道怎没用
            gate_way_lable.isFocusable = true//不可编辑
            gate_way_lable.isEnabled = true
            gate_way_lable.requestFocus()
            gate_way_lable.setSelection(gate_way_lable.text.length)//将光标移至文字末尾
        } else {
            gate_way_lable.isFocusableInTouchMode = false//不可编辑
            gate_way_lable.isClickable = false//不可点击
            gate_way_lable.isFocusable = false//不可编辑
            gate_way_lable.isEnabled = false
            gate_way_lable.background = null
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