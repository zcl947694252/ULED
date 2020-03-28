package com.dadoutek.uled.gateway

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.v7.widget.DividerItemDecoration
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.text.TextUtils
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.TextView
import com.blankj.utilcode.util.GsonUtils
import com.blankj.utilcode.util.LogUtils
import com.blankj.utilcode.util.ToastUtils
import com.dadoutek.uled.R
import com.dadoutek.uled.base.TelinkBaseActivity
import com.dadoutek.uled.gateway.adapter.GwTaskItemAdapter
import com.dadoutek.uled.gateway.bean.DbGateway
import com.dadoutek.uled.gateway.bean.GwTagBean
import com.dadoutek.uled.gateway.bean.GwTasksBean
import com.dadoutek.uled.gateway.util.GsonUtil
import com.dadoutek.uled.model.DbModel.DBUtils
import com.dadoutek.uled.model.DeviceType
import com.dadoutek.uled.model.HttpModel.GwModel
import com.dadoutek.uled.model.Opcode
import com.dadoutek.uled.network.NetworkObserver
import com.dadoutek.uled.tellink.TelinkLightService
import com.yanzhenjie.recyclerview.touch.OnItemMoveListener
import kotlinx.android.synthetic.main.activity_gate_way.*
import kotlinx.android.synthetic.main.template_bottom_add_no_line.*
import kotlinx.android.synthetic.main.template_swipe_recycleview.*
import kotlinx.android.synthetic.main.toolbar.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jetbrains.anko.toast
import java.util.*
import kotlin.collections.ArrayList

/**
 * 创建者     ZCL
 * 创建时间   2020/3/3 17:22
 * 描述  此页只更改taglist内部的一个tag
 *
 * 更新者     $
 * 更新时间   $
 * 更新描述
 */
class GwConfigTagActivity : TelinkBaseActivity(), View.OnClickListener {
    private var currentTagStr: String? = null
    private var dbGw: DbGateway? = null
    private var tagList: ArrayList<GwTagBean> = arrayListOf()
    private var dbId: Long = 99999
    private var tagBean: GwTagBean? = null
    private val requestModeCode: Int = 1000//选择日期
    private val requestTimeCode: Int = 2000//时间段模式
    private var isCanEdite = false
    private var maxId = 0L
    val listTask = ArrayList<GwTasksBean>()
    private val adapter = GwTaskItemAdapter(R.layout.item_gw_time_scene, listTask)

    /**
     * 默认重复设置为仅一次
     */
    fun initView() {
        toolbarTv.text = getString(R.string.Gate_way)
        gate_way_repete_mode.textSize = 15F
        toolbar.setNavigationIcon(R.drawable.icon_top_tab_back)
        toolbar.setNavigationOnClickListener {
            val intent = Intent(this@GwConfigTagActivity, GwEventListActivity::class.java)
            intent.putExtra("data", dbGw)
            startActivity(intent)
            finish()
        }
        tv_function1.text = getString(R.string.complete)
        tv_function1.visibility = View.VISIBLE
        gate_way_repete_mode.text = getString(R.string.only_one)

        isCanEdite = false
        isCanEdite()
        swipe_recycleView.layoutManager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)
        swipe_recycleView.addItemDecoration(DividerItemDecoration(this, DividerItemDecoration.HORIZONTAL))
        swipe_recycleView.adapter = adapter
        swipe_recycleView.isItemViewSwipeEnabled = true //侧滑删除，默认关闭。
    }

    private fun getMaxTagId() {
        for (tag in tagList) {
            if (tag.tagId > maxId)
                maxId = tag.tagId
        }
    }

    fun initData() {
        //創建新tag任务
        dbGw = intent.getParcelableExtra<DbGateway>("data")

        if (dbGw != null) {//拿到有效数据
            currentTagStr = if (dbGw?.type == 0) //定时
                dbGw?.tags
            else
                dbGw?.timePeriodTags

            if (!TextUtils.isEmpty(currentTagStr)) {//不要移动 此处有新tag和老tag需要的东西
                tagList.clear()
                tagList.addAll(GsonUtil.stringToList(currentTagStr, GwTagBean::class.java))//获取该设备tag列表
                if (tagList.size > 0)
                    getMaxTagId()//更新maxid
            }

            tagBean = if (dbGw?.addTag == 0) {//是否是添加新tag
                //创建新的tag
                GwTagBean((maxId + 1), getString(R.string.tag1), getString(R.string.only_one), getWeek(getString(R.string.only_one)))
            } else {//编辑已有tag
                if (tagList.size > 0)
                    tagList[dbGw?.pos ?: 0]
                else
                    GwTagBean((maxId + 1), getString(R.string.tag1), getString(R.string.only_one), getWeek(getString(R.string.only_one)))
            }
            tagBean?.setIsTimer(dbGw?.type == 0) //判断是不是timer
        } else {
            ToastUtils.showShort(getString(R.string.invalid_data))
            finish()
        }
        listTask.clear()
        val tasks = tagBean!!.tasks
        if (tasks != null)
            listTask.addAll(tasks)
        adapter.notifyDataSetChanged()

        gate_way_lable.setText(tagBean?.tagName)
        gate_way_repete_mode.text = tagBean?.weekStr

        if (tagBean?.getIsTimer() != false)
            add_group_btn?.findViewById<TextView>(R.id.add_group_btn_tv)?.text = getString(R.string.add_time)
        else
            add_group_btn?.findViewById<TextView>(R.id.add_group_btn_tv)?.text = getString(R.string.add_times)
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.add_group_btn -> addNewTask()//创建新的task传入可用的index

            R.id.gate_way_repete_mode, R.id.gate_way_repete_mode_arrow -> {//选择模式是否重复
                val intent = Intent(this@GwConfigTagActivity, GwChoseModeActivity::class.java)
                startActivityForResult(intent, requestModeCode)
            }
            R.id.gate_way_edite -> {//是否编辑
                isCanEdite = !isCanEdite
                isCanEdite()
            }
            R.id.tv_function1 -> {
                dbGw?.let { it ->
                    it.productUUID = DeviceType.GATE_WAY
                    val toJson = GsonUtils.toJson(listTask)//获取tasks字符串
                    LogUtils.v("zcl网关---$toJson")
                    tagBean?.tagName = gate_way_lable.text.toString()
                    tagBean?.weekStr = gate_way_repete_mode.text.toString()
                    tagBean?.tasks = listTask//添加tag的时间列表

                    //因为是新创建的对象所以直接创建list添加 如果不是需要查看是都有tags
                    if (tagList.size == 0) {
                        tagList.add(tagBean!!)
                    } else {
                        var removeTag: GwTagBean? = null
                        for (tag in tagList)
                            if (tag.tagId == tagBean?.tagId)
                                removeTag = tag
                        tagList.remove(removeTag)
                        tagList.add(tagBean!!)
                    }
                    if (it.type == 0)
                        it.tags = GsonUtils.toJson(tagList)
                    else
                        it.timePeriodTags = GsonUtils.toJson(tagList)//tag列表

                    DBUtils.saveGateWay(it, dbGw?.addTag == 1)//是否是添加新的
                    addGw(it)
                    sendLabelHeadParams()
                }
                val intent = Intent(this@GwConfigTagActivity, GwEventListActivity::class.java)
                intent.putExtra("data", dbGw)
                startActivity(intent)
                finish()
            }
        }
    }

    private fun addGw(dbGw: DbGateway) {
        GwModel.add(dbGw)?.subscribe(object : NetworkObserver<DbGateway?>() {
            override fun onNext(t: DbGateway) {
                LogUtils.v("zcl-----网关失添成功返回-------------$t")
            }

            override fun onError(e: Throwable) {
                super.onError(e)
                LogUtils.v("zcl-------网关失添加败-----------" + e.message)
            }
        })
    }

    /**
     * 发送标签保存命令
     */
    private fun sendLabelHeadParams() {
        var meshAddress = dbGw?.meshAddr ?: 0
        //p = byteArrayOf(0x02, Opcode.GROUP_BRIGHTNESS_MINUS, 0x00, 0x00, 0x03, Opcode.GROUP_CCT_MINUS, 0x00, 0x00)
        //从第八位开始opcode, 设备meshAddr  参数11-12-13-14 15-16-17-18
        val calendar = Calendar.getInstance()
        val month = calendar.get(Calendar.MONTH)+1
        val day = calendar.get(Calendar.DAY_OF_MONTH)
        LogUtils.v("zcl-----------当前日期:--------$month-$day")
        tagBean?.let {
            var labHeadPar = byteArrayOf(it.tagId.toByte(), it.status.toByte(),
                    it.week.toByte(), 0, month.toByte(), day.toByte(), 0, 0)

            val opcodeHead = if (tagBean?.isTimer() == true)
                Opcode.CONFIG_GW_TIMER_LABLE_HEAD
            else
                Opcode.CONFIG_GW_TIMER_PERIOD_LABLE_HEAD
            TelinkLightService.Instance().sendCommandNoResponse(opcodeHead, meshAddress, labHeadPar)

            if (tagBean?.isTimer() == true) {//定时场景标签头下发,时间段时间下发 挪移至时间段内部发送
                var delayTime = 0L
                val tasks = it.tasks

                GlobalScope.launch(Dispatchers.Main) {
                    delayTime += 200
                    delay(timeMillis = delayTime)
                    if (tasks!=null)
                    for (task in tasks) {//定时场景时间下发
                        var params = byteArrayOf(it.tagId.toByte(), task.index.toByte(),
                                task.startHour.toByte(), task.startMins.toByte(), task.sceneId.toByte(), 0, 0, 0)
                        TelinkLightService.Instance().sendCommandNoResponse(Opcode.CONFIG_GW_TIMER_LABLE_TIME, meshAddress, params)
                    }
                }
            } else {//时间段场景下发 时间段场景时间下发 挪移至时间段内部发送
            }
        }
    }

    private fun getWeek(str: String): Int {
        var week = 0b00000000
        when (str) {
            getString(R.string.only_one) -> {
                week = 0b10000000
                return week
            }
            getString(R.string.every_day) -> {
                week = 0b01111111
                return week
            }
            else -> {
                val split = str.split(",").toMutableList()
                for (s in split) {
                    when (s) {//bit位 0-6 周日-周六 7代表当天
                        getString(R.string.sunday) -> week = week or 0b00000001
                        getString(R.string.monday) -> week = week or 0b00000010
                        getString(R.string.tuesday) -> week = week or 0b00000100
                        getString(R.string.wednesday) -> week = week or 0b00001000
                        getString(R.string.thursday) -> week = week or 0b00010000
                        getString(R.string.friday) -> week = week or 0b00100000
                        getString(R.string.saturday) -> week = week or 0b01000000
                    }
                }
                return week
            }
        }
    }

    fun initListener() {
        tv_function1.setOnClickListener(this)
        gate_way_edite.setOnClickListener(this)
        gate_way_repete_mode.setOnClickListener(this)
        gate_way_repete_mode_arrow.setOnClickListener(this)
        swipe_recycleView.setOnItemMoveListener(object : OnItemMoveListener {
            override fun onItemDismiss(srcHolder: RecyclerView.ViewHolder?) {
                // 从数据源移除该Item对应的数据，并刷新Adapter。
                val position = srcHolder?.adapterPosition
                sendDeleteTask(listTask[position?:0])
                listTask.removeAt(position!!)
                adapter.notifyItemRemoved(position)
                saveChangeTasks()
            }

            private fun saveChangeTasks() {
                DBUtils.getGatewayByID(dbId)?.let { it ->
                    tagBean?.tasks = listTask
                    //因为是新创建的对象所以直接创建list添加 如果不是需要查看是都有tags
                    val tJ = GsonUtils.toJson(mutableListOf(tagBean))
                    it.tags = tJ
                    DBUtils.saveGateWay(it, true)
                }
            }

            override fun onItemMove(srcHolder: RecyclerView.ViewHolder?, targetHolder: RecyclerView.ViewHolder?): Boolean {
                return false//表示数据移动不成功
            }
        })
        adapter.setOnItemClickListener { _, _, position ->
            val intent: Intent
            if (tagBean?.getIsTimer() == true) {
                intent = Intent(this@GwConfigTagActivity, GwChoseTimeActivity::class.java)
                listTask[0].selectPos = position//默认使用第一个记录选中的pos
                intent.putExtra("data", listTask)
            } else {
                intent = Intent(this@GwConfigTagActivity, GwTimerPeriodListActivity::class.java)
                //传入时间段数据 重新配置时间段的场景值
                listTask[0].labelId = tagBean?.tagId ?: 0//默认使用第一个记录选中的pos
                intent.putExtra("data", listTask[position])
            }
            startActivityForResult(intent, requestTimeCode)
        }
        add_group_btn?.setOnClickListener(this)
    }

    private fun sendDeleteTask(gwTaskBean: GwTasksBean) {
        var opcodeDelete = if (tagBean?.isTimer() == true)
            Opcode.CONFIG_GW_TIMER_DELETE_TASK
        else
            Opcode.CONFIG_GW_TIMER_PERIOD_DELETE_TASK
        //11-18 11位标签id 12 时间条index
        val id = dbGw?.id ?: 0
        var paramer = byteArrayOf(id.toByte(), gwTaskBean.index.toByte(), 0, 0, 0, 0, 0, 0)

        TelinkLightService.Instance().sendCommandNoResponse(opcodeDelete,
                dbGw?.meshAddr ?: 0, paramer)

    }

    private fun addNewTask() {
        val index = getIndex()
        when {
            listTask.size > 20 -> toast(getString(R.string.gate_way_time_max))
            index == 0 -> toast(getString(R.string.gate_way_time_max))
            tagBean?.getIsTimer() == true -> {//跳转时间选择界面
                sendLabelHeadParams()

                val intent = Intent(this@GwConfigTagActivity, GwChoseTimeActivity::class.java)

                val tasksBean = GwTasksBean(index)
                tasksBean.labelId = tagBean?.tagId ?: 0
                intent.putExtra("newData", tasksBean)

                startActivityForResult(intent, requestTimeCode)
            }
            tagBean?.getIsTimer() == false -> {//跳转时间段选择界面
                sendLabelHeadParams()

                val intent = Intent(this@GwConfigTagActivity, GwChoseTimePeriodActivity::class.java)

                val tasksBean = GwTasksBean(index)
                tasksBean.labelId = tagBean?.tagId ?: 0
                tasksBean.gwMeshAddr = dbGw?.meshAddr ?: 0
                intent.putExtra("newData", tasksBean)

                startActivityForResult(intent, requestTimeCode)
            }
        }
    }

    /**
     * 获取task下标
     */
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
                    tagBean?.week = getWeek(mode.replace("6", ""))
                } else {
                    gate_way_repete_mode.textSize = 15F
                    mode = isOnlyOne(mode)
                    gate_way_repete_mode.text = mode
                    tagBean?.weekStr = mode
                    tagBean?.week = getWeek(mode)
                }
            } else if (requestCode == requestTimeCode) {//获取定时和时间段的task
                val bean = data?.getParcelableExtra<GwTasksBean>("data")
                bean?.let {
                    if (bean.isCreateNew) {
                        listTask.add(bean)
                    } else {
                        var targetPosition: Int
                        for (i in 0 until listTask.size) {
                            val timeBean = listTask[i]
                            if (timeBean.index == bean.index) {//存在
                                targetPosition = i
                                listTask.removeAt(targetPosition)
                                listTask.add(bean)
                                break
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
            val im = gate_way_lable.context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            im.showSoftInput(gate_way_lable, 0)
        } else {
            gate_way_lable.isFocusableInTouchMode = false//不可编辑
            gate_way_lable.isClickable = false//不可点击
            gate_way_lable.isFocusable = false//不可编辑
            gate_way_lable.isEnabled = false
            gate_way_lable.background = null
            val im = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            if (currentFocus != null)
                im.hideSoftInputFromWindow(currentFocus.windowToken, InputMethodManager.HIDE_NOT_ALWAYS)
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