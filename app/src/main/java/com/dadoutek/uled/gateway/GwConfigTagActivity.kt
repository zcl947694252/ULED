package com.dadoutek.uled.gateway

import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import android.support.v7.widget.DividerItemDecoration
import android.support.v7.widget.LinearLayoutManager
import android.text.TextUtils
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.RequiresApi
import com.blankj.utilcode.util.GsonUtils
import com.blankj.utilcode.util.LogUtils
import com.blankj.utilcode.util.ToastUtils
import com.dadoutek.uled.R
import com.dadoutek.uled.base.TelinkBaseActivity
import com.dadoutek.uled.gateway.adapter.GwTaskItemAdapter
import com.dadoutek.uled.gateway.bean.DbGateway
import com.dadoutek.uled.gateway.bean.GwTagBean
import com.dadoutek.uled.gateway.bean.GwTasksBean
import com.dadoutek.uled.gateway.bean.WeekBean
import com.dadoutek.uled.gateway.util.Base64Utils
import com.dadoutek.uled.gateway.util.GsonUtil
import com.dadoutek.uled.model.Constant
import com.dadoutek.uled.model.DbModel.DBUtils
import com.dadoutek.uled.model.DeviceType
import com.dadoutek.uled.model.HttpModel.GwModel
import com.dadoutek.uled.model.Opcode
import com.dadoutek.uled.network.GwGattBody
import com.dadoutek.uled.network.NetworkObserver
import com.dadoutek.uled.tellink.TelinkLightApplication
import com.dadoutek.uled.tellink.TelinkLightService
import com.dadoutek.uled.util.DensityUtil
import com.dadoutek.uled.util.StringUtils
import com.telink.bluetooth.light.DeviceInfo
import com.telink.bluetooth.light.LightService
import com.yanzhenjie.recyclerview.SwipeMenu
import com.yanzhenjie.recyclerview.SwipeMenuItem
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_gate_way.*
import kotlinx.android.synthetic.main.template_bottom_add_no_line.*
import kotlinx.android.synthetic.main.template_swipe_recycleview.*
import kotlinx.android.synthetic.main.toolbar.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.jetbrains.anko.toast
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.collections.ArrayList

/**
 * 创建者     ZCL
 * 创建时间   2020/3/3 17:22
 * 描述  此页只更改taglist内部的一个tag
 * 再接收的回调中写入代码后闪退找不到问题时注意一下是不是线程问题
 * 更新者     $
 * 更新时间   $
 * 更新描述
 */
class GwConfigTagActivity : TelinkBaseActivity(), View.OnClickListener{

    private lateinit var filter: IntentFilter
    private  var receiver: GwBrocasetReceiver? = null
    private var deleteBean: GwTasksBean? = null
    private var connectCount: Int = 0
    private lateinit var mApp: TelinkLightApplication
    private var deletePosition: Int? = null
    private var disposableTimer: Disposable? = null
    private var currentTagStr: String? = null
    private var dbGw: DbGateway? = null
    private var tagList: ArrayList<GwTagBean> = arrayListOf()
    private var tagBean: GwTagBean? = null
    private val requestModeCode: Int = 1000//选择日期
    private val requestTimeCode: Int = 2000//时间段模式
    private var isCanEdite = false
    private var maxId = 0L
    private val listTask = ArrayList<GwTasksBean>()
    private val adapter = GwTaskItemAdapter(R.layout.item_gw_time_scene, listTask)
    private val function: (leftMenu: SwipeMenu, rightMenu: SwipeMenu, position: Int) -> Unit = { _, rightMenu, _ ->
        val menuItem = SwipeMenuItem(this@GwConfigTagActivity)// 创建菜单
        menuItem.height = ViewGroup.LayoutParams.MATCH_PARENT
        menuItem.weight = DensityUtil.dip2px(this, 500f)
        menuItem.textSize = 20
        menuItem.setBackgroundColor(getColor(R.color.red))
        menuItem.setText(R.string.delete)
        rightMenu.addMenuItem(menuItem)//添加进右侧菜单
    }

    /**
     * 默认重复设置为仅一次
     */
    @RequiresApi(Build.VERSION_CODES.O)
    fun initView() {
        toolbar.setNavigationIcon(R.drawable.icon_return)
        toolbar.setOnClickListener {
             val textGp = EditText(this)
                         StringUtils.initEditTextFilter(textGp)
            tagBean?.tagName
                         val s = tagBean?.tagName ?: ""
                         textGp.setText(s)
                         textGp.setSelection(s.length)
                         AlertDialog.Builder(this)
                                 .setTitle(getString(R.string.update_name))
                                 .setIcon(android.R.drawable.ic_dialog_info)
                                 .setView(textGp)
                                 .setPositiveButton(getString(android.R.string.ok)) { _, _ ->
                                     if (StringUtils.compileExChar(textGp.text.toString().trim { it <= ' ' })) {
                                         ToastUtils.showLong(getString(R.string.rename_tip_check))
                                     } else {
                                         val trim = textGp.text.toString().trim { it <= ' ' }
                                         tagBean?.tagName = trim
                                         toolbarTv.text = trim
                                     }
                                 }
                                 .setNegativeButton(getString(R.string.btn_cancel)) { dialog, _ -> dialog.dismiss() }.show()
        }
        toolbar.setNavigationOnClickListener {
            val intent = Intent()
            intent.putExtra("data", dbGw)
            setResult(Activity.RESULT_OK, intent)
            finish()
        }
        tv_function1.text = getString(R.string.complete)
        tv_function1.visibility = View.VISIBLE
        gate_way_repete_mode.text = getString(R.string.only_one)

        isCanEdite = false
        isCanEdite()

        swipe_recycleView.layoutManager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)
        swipe_recycleView.isItemViewSwipeEnabled = false //侧滑删除，默认关闭。
        swipe_recycleView.addItemDecoration(DividerItemDecoration(this, DividerItemDecoration.HORIZONTAL))
        swipe_recycleView.setSwipeMenuCreator(function) // 设置监听器。
        swipe_recycleView.setOnItemMenuClickListener { menuBridge, adapterPosition ->
            menuBridge.closeMenu()//删除标签的回调
            deletePosition = adapterPosition
            sendDeleteTask(listTask[deletePosition ?: 0])
        }
        swipe_recycleView.adapter = adapter


        this.mApp = this.application as TelinkLightApplication
    }

    private fun getMaxTagId() {
        for (tag in tagList) {
            if (tag.tagId > maxId)
                maxId = tag.tagId
        }
    }

    @SuppressLint("SetTextI18n")
    fun initData() {//創建新tag任务
        dbGw = intent.getParcelableExtra("data")
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

            tagBean = if (dbGw?.addTag == 0) {//是否是添加新tag 0创建新的tag 1编辑已有tag
                GwTagBean((maxId + 1), getString(R.string.tag1), getString(R.string.only_one), getWeek(getString(R.string.only_one)))
            } else {//编辑已有tag
                if (tagList.size > 0)
                    tagList[dbGw?.pos ?: 0]
                else
                    GwTagBean((maxId + 1), getString(R.string.tag1), getString(R.string.only_one), getWeek(getString(R.string.only_one)))
            }
            tagBean?.setIsTimer(dbGw?.type == 0) //判断是不是timer
            tagBean?.macAddr = dbGw?.macAddr //判断是不是timer
            tagBean?.meshAddr = dbGw?.meshAddr ?: 0 //判断是不是timer
            TelinkLightApplication.getApp().currentGwTagBean = tagBean
        } else {
            ToastUtils.showShort(getString(R.string.invalid_data))
            finish()
        }
        listTask.clear()
        val tasks = tagBean!!.tasks
        if (tasks != null) {
            val elements = GsonUtil.stringToList(tasks, GwTasksBean::class.java)
            listTask.addAll(elements)
            listTask.sortBy { it.startHour * 60 + it.startMins }
        }
        listTask.sortWith(compareBy({ it.startHour }, { it.startMins }))
        adapter.notifyDataSetChanged()

        gate_way_lable.setText(getString(R.string.label) + tagBean?.tagId)

        getWeekStr()

        toolbarTv.text = getString(R.string.label) + tagBean?.tagId

        gate_way_repete_mode.text = tagBean?.weekStr

        if (tagBean?.getIsTimer() != false)
            add_group_btn?.findViewById<TextView>(R.id.add_group_btn_tv)?.text = getString(R.string.add_time)
        else
            add_group_btn?.findViewById<TextView>(R.id.add_group_btn_tv)?.text = getString(R.string.add_times2)
    }

    private fun getWeekStr(): String {
        var tmpWeek = tagBean!!.week
        val sb = StringBuilder()
        when (tmpWeek) {
            0b10000000 -> sb.append(getString(R.string.every_day))
            0b00000000 -> sb.append(getString(R.string.only_one))
            else -> {
                var list = mutableListOf(
                        WeekBean(getString(R.string.monday), 1, (tmpWeek and Constant.MONDAY) != 0),
                        WeekBean(getString(R.string.tuesday), 2, (tmpWeek and Constant.TUESDAY) != 0),
                        WeekBean(getString(R.string.wednesday), 3, (tmpWeek and Constant.WEDNESDAY) != 0),
                        WeekBean(getString(R.string.thursday), 4, (tmpWeek and Constant.THURSDAY) != 0),
                        WeekBean(getString(R.string.friday), 5, (tmpWeek and Constant.FRIDAY) != 0),
                        WeekBean(getString(R.string.saturday), 6, (tmpWeek and Constant.SATURDAY) != 0),
                        WeekBean(getString(R.string.sunday), 7, (tmpWeek and Constant.SUNDAY) != 0))
                for (i in 0 until list!!.size) {
                    var weekBean = list!![i]
                    if (weekBean.selected) {
                        if (i == list!!.size - 1)
                            sb.append(weekBean.week)
                        else
                            sb.append(weekBean.week).append(",")
                    }
                }
            }
        }
        return sb.toString()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.add_group_btn -> addNewTask()//创建新的task传入可用的index

            R.id.gate_way_repete_mode_ly, R.id.gate_way_repete_mode_arrow -> {//选择模式是否重复
                val intent = Intent(this@GwConfigTagActivity, GwChoseModeActivity::class.java)
                intent.putExtra("data", tagBean!!.week)
                startActivityForResult(intent, requestModeCode)
            }

            R.id.gate_way_edite -> {//是否编辑
                isCanEdite = !isCanEdite
                isCanEdite()
            }
            R.id.tv_function1 -> {
                sendLabelHeadParams()
            }
        }
    }

    private fun saveOrUpdataGw(it: DbGateway) {
        it.productUUID = DeviceType.GATE_WAY
        val toJson = GsonUtils.toJson(listTask)//获取tasks字符串
        LogUtils.v("zcl网关---$toJson")
        tagBean?.tagName = toolbarTv.text.toString()
        tagBean?.weekStr = gate_way_repete_mode.text.toString()
        tagBean?.tasks = toJson//添加tag的时间列表
        tagBean?.status = 0

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

    private fun upDataDeleteUi() {
        listTask.remove(deleteBean)
        adapter.notifyDataSetChanged()
        saveOrUpdataGw(dbGw!!)
    }

    /**
     * 删除标签task任务
     */
    @RequiresApi(Build.VERSION_CODES.O)
    private fun sendDeleteTask(gwTaskBean: GwTasksBean) {
        deleteBean = gwTaskBean
        connectCount++
        val id = gwTaskBean?.labelId
        var opcodeDelete = if (tagBean?.isTimer() == true)
            Opcode.CONFIG_GW_TIMER_DELETE_TASK
        else
            Opcode.CONFIG_GW_TIMER_PERIOD_DELETE_TASK

        if (TelinkLightApplication.getApp().isConnectGwBle) {
            setTimerDelay(1500L, gwTaskBean)
            //11-18 11位标签id 12 时间条index
            var paramer = byteArrayOf(id.toByte(), gwTaskBean.index.toByte(), 0, 0, 0, 0, 0, 0)
            TelinkLightService.Instance().sendCommandResponse(opcodeDelete, dbGw?.meshAddr ?: 0, paramer, "1")
        } else {
            setTimerDelay(6500L, gwTaskBean)
            var gattPar = byteArrayOf(0x11, 0x11, 0x11, 0, 0, 0, 0, opcodeDelete, 0x11, 0x02,
                    id.toByte(), gwTaskBean.index.toByte(), 0, 0, 0, 0, 0, 0, 0, 0)
            LogUtils.v("zcl-----------发送到服务器删除标签-------$gattPar")

            val s =  Base64Utils.encodeToStrings(gattPar)
            val gattBody = GwGattBody()
            gattBody.ser_id = Constant.GW_GATT_DELETE_LABEL_TASK
            gattBody.data = s
            gattBody.macAddr = dbGw?.macAddr
            sendToServer(gattBody)
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun setTimerDelay(delay: Long, gwTaskBean: GwTasksBean) {
        disposableTimer?.dispose()
        disposableTimer = Observable.timer(delay, TimeUnit.MILLISECONDS)
                .observeOn(Schedulers.io())
                .subscribeOn(AndroidSchedulers.mainThread())
                .subscribe {
                    if (connectCount < 3)
                        sendDeleteTask(gwTaskBean)
                    else
                        runOnUiThread { ToastUtils.showShort(getString(R.string.delete_gate_way_task_fail)) }
                }
    }

    /**
     * 发送标签保存命令
     */
    @RequiresApi(Build.VERSION_CODES.O)
    private fun sendLabelHeadParams() {
        disposableTimer?.dispose()
        disposableTimer = Observable.timer(1500, TimeUnit.MILLISECONDS)
                .subscribe {
                    GlobalScope.launch(Dispatchers.Main) {
                        ToastUtils.showShort(getString(R.string.send_gate_way_label_head_fail))
                    }
                }
        var meshAddress = dbGw?.meshAddr ?: 0
        //p = byteArrayOf(0x02, Opcode.GROUP_BRIGHTNESS_MINUS, 0x00, 0x00, 0x03, Opcode.GROUP_CCT_MINUS, 0x00, 0x00)
        //从第八位开始opcode, 设备meshAddr  参数11-12-13-14 15-16-17-18
        val calendar = Calendar.getInstance()
        val month = calendar.get(Calendar.MONTH) + 1
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        LogUtils.v("zcl-----------当前日期:--------$month-$day")

        val opcodeHead = if (tagBean?.isTimer() == true)
            Opcode.CONFIG_GW_TIMER_LABLE_HEAD
        else
            Opcode.CONFIG_GW_TIMER_PERIOD_LABLE_HEAD

        if (tagBean?.weekStr == null) {
            tagBean?.weekStr = getString(R.string.only_one)
            tagBean?.week = 0b00000000
        }

        if (!TelinkLightApplication.getApp().isConnectGwBle) {
            tagBean?.let {
                it.status = 0//修改数据后状态设置成关闭
                var labHeadPar = byteArrayOf(0x11, 0x11, 0x11, 0, 0, 0, 0, opcodeHead, 0x11, 0x02,
                        it.tagId.toByte(), it.status.toByte(), it.week.toByte(), 0,
                        month.toByte(), day.toByte(), 0, 0, 0, 0)// status 开1 关0 tag的外部现实

                LogUtils.v("zcl-----------发送到服务器标签头-------$labHeadPar")

                val s = Base64Utils.encodeToStrings(labHeadPar)
                val gattBody = GwGattBody()
                gattBody.data = s
                gattBody.ser_id = Constant.GW_GATT_SAVE_LABEL_HEAD
                gattBody.macAddr = dbGw?.macAddr
                gattBody.tagName = it.tagName
                sendToServer(gattBody)
            }
        } else {
            tagBean?.let {
                var labHeadPar = byteArrayOf(it.tagId.toByte(), it.status.toByte(),
                        it.week.toByte(), 0, month.toByte(), day.toByte(), 0, 0)
                TelinkLightService.Instance()?.sendCommandResponse(opcodeHead, meshAddress, labHeadPar, "1")
                LogUtils.v("zcl-----------发送命令0xf6-------配置界面")
            }
        }
    }

    private fun sendToServer(gattBody: GwGattBody) {
        GwModel.sendToGatt(gattBody)?.subscribe(object : NetworkObserver<String?>() {
            override fun onNext(t: String) {
                LogUtils.v("zcl---发送服务器返回----------$t")
            }

            override fun onError(e: Throwable) {
                super.onError(e)
                LogUtils.e("zcl-------发送服务器返回-----------${e.message}")
            }
        })
    }

    private fun getWeek(str: String): Int {
        var week = 0b00000000
        when (str) {
            getString(R.string.only_one) -> {
                week = 0b00000000
                return week
            }
            getString(R.string.every_day) -> {
                week = 0b10000000
                return week
            }
            else -> {
                val split = str.split(",").toMutableList()
                for (s in split) {
                    when (s) {//bit位 0-6 周日-周六 7代表当天 0代表仅一次
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
        gate_way_repete_mode_ly.setOnClickListener(this)
        gate_way_repete_mode_arrow.setOnClickListener(this)

        adapter.setOnItemClickListener { _, _, position ->
            val intent: Intent
            if (tagBean?.getIsTimer() == true) {
                intent = Intent(this@GwConfigTagActivity, GwChoseTimeActivity::class.java)
                val tasksBean = listTask[position]
                tasksBean.isCreateNew = false
                TelinkLightApplication.getApp().listTask = listTask
                intent.putExtra("data", tasksBean)
            } else {
                // intent = Intent(this@GwConfigTagActivity, GwTimerPeriodListActivity::class.java)
                intent = Intent(this@GwConfigTagActivity, GwTimerPeriodListActivity2::class.java)

                //传入时间段数据 重新配置时间段的场景值
                val tasksBean = listTask[position]
                tasksBean.labelId = tagBean?.tagId ?: 0
                tasksBean.isCreateNew = false
                tasksBean.gwMacAddr = dbGw?.macAddr
                TelinkLightApplication.getApp().listTask = listTask
                intent.putExtra("data", tasksBean)
            }
            startActivityForResult(intent, requestTimeCode)
        }
        add_group_btn?.setOnClickListener(this)
    }

    private fun addNewTask() {
        val index = getIndex()
        when {
            listTask.size > 20 -> toast(getString(R.string.gate_way_time_max))
            index == 0 -> toast(getString(R.string.gate_way_time_max))
            tagBean?.getIsTimer() == true -> {//跳转时间选择界面
                val intent = Intent(this@GwConfigTagActivity, GwChoseTimeActivity::class.java)

                val tasksBean = GwTasksBean(index)
                tasksBean.labelId = tagBean?.tagId ?: 0
                tasksBean.isCreateNew = true
                TelinkLightApplication.getApp().listTask = listTask
                intent.putExtra("data", tasksBean)
                startActivityForResult(intent, requestTimeCode)
            }
            tagBean?.getIsTimer() == false -> {//跳转时间段选择界面
                val intent = Intent(this@GwConfigTagActivity, GwChoseTimePeriodActivity::class.java)

                val tasksBean = GwTasksBean(index)
                tasksBean.labelId = tagBean?.tagId ?: 0
                tasksBean.gwMeshAddr = dbGw?.meshAddr ?: 0
                TelinkLightApplication.getApp().listTask = listTask
                tasksBean.isCreateNew = true
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

    @RequiresApi(Build.VERSION_CODES.O)
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
                    //mode = isOnlyOne(mode) 如果不选择就是仅一次 但凡选中任意一个星期日都是现实星期几
                    gate_way_repete_mode.text = mode
                    tagBean?.weekStr = mode
                    tagBean?.week = getWeek(mode)
                }
            } else if (requestCode == requestTimeCode) {//获取定时和时间段的task
                val bean = data?.getParcelableExtra<GwTasksBean>("data")
                bean?.let { b ->
                    if (b.isCreateNew) {
                        b.isCreateNew = false
                        listTask.add(b)
                    } else {
                        bean.isCreateNew = false
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
//                        listTask.sortBy { it.startHour }
                        listTask.sortWith(compareBy({ it.startHour }, { it.startMins }))
                    }
                    dbGw?.state = 0
                    saveOrUpdataGw(dbGw!!)
                    // sendTime(b, dbGw?.meshAddr ?: 0)
                    listTask.sortWith(compareBy({ it.startHour }, { it.startMins }))
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

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_gate_way)
        receiver = GwBrocasetReceiver()
         filter = IntentFilter()
        filter.addAction(LightService.ACTION_STATUS_CHANGED)
        receiver?.setOnGwStateChangeListerner(object : GwBrocasetReceiver.GwStateChangeListerner {
            override fun loginSuccess() {
                toolbar!!.findViewById<ImageView>(R.id.image_bluetooth).setImageResource(R.drawable.icon_bluetooth)
            }

            override fun loginFail() {
                toolbar!!.findViewById<ImageView>(R.id.image_bluetooth).setImageResource(R.drawable.bluetooth_no)
            }

            override fun setGwComplete(deviceInfo: DeviceInfo) {
                when (deviceInfo.gwVoipState) {
                    Constant.GW_DELETE_TIMER_TASK_VOIP, Constant.GW_DELETE_TIME_PERIVODE_TASK_VOIP -> {//删除task回调 收到一定是成功
                        hideLoadingDialog()
                        disposableTimer?.dispose()
                        runOnUiThread { upDataDeleteUi() }
                    }

                    Constant.GW_CONFIG_TIMER_LABEL_VOIP, Constant.GW_CONFIG_TIME_PERIVODE_LABEL_VOIP -> {//目前此界面只有保标签头时发送头命令
                        LogUtils.v("zcl-----------获取网关相关返回信息-------$deviceInfo")
                        saveOrUpdataGw(dbGw!!)
                        val intent = Intent()
                        intent.putExtra("data", dbGw)
                        setResult(Activity.RESULT_OK, intent)
                        finish()
                    }
                }
            }

            override fun getMacComplete(deviceInfo: DeviceInfo) {
                //mac信息获取成功
                dbGw?.sixByteMacAddr = deviceInfo.sixByteMacAddress
            }
        })
        registerReceiver(receiver, filter)
        initView()
        initData()
        initListener()
    }

    override fun onPause() {
        super.onPause()
        unregisterReceiver(receiver)
    }

    override fun onResume() {
        super.onResume()
        registerReceiver(receiver, filter)
    }
    override fun receviedGwCmd2000(serId: String) {
        when (serId?.toInt()) {
            Constant.GW_GATT_DELETE_LABEL_TASK -> {
                //删除标签task时间任务 不成功定时会将数据还原
                disposableTimer?.dispose()
                hideLoadingDialog()
                upDataDeleteUi()
            }
            Constant.GW_GATT_SAVE_LABEL_HEAD -> {
                //保存标签头信息  保存成功发送数据会上一页 不成功不做操作
                disposableTimer?.dispose()
                saveOrUpdataGw(dbGw!!)
                val intent = Intent()
                intent.putExtra("data", dbGw)
                setResult(Activity.RESULT_OK, intent)
                finish()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        disposableTimer?.dispose()
    }

    override fun onBackPressed() {
        val intent = Intent()
        intent.putExtra("data", dbGw)
        setResult(Activity.RESULT_OK, intent)
        super.onBackPressed()
    }
}