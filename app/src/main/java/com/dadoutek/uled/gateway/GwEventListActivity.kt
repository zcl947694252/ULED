package com.dadoutek.uled.gateway

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.support.v7.widget.LinearLayoutManager
import android.text.TextUtils
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.CheckBox
import android.widget.ImageView
import androidx.annotation.RequiresApi
import com.blankj.utilcode.util.GsonUtils
import com.blankj.utilcode.util.LogUtils
import com.blankj.utilcode.util.ToastUtils
import com.chad.library.adapter.base.BaseQuickAdapter
import com.dadoutek.uled.R
import com.dadoutek.uled.base.TelinkBaseActivity
import com.dadoutek.uled.communicate.Commander
import com.dadoutek.uled.gateway.adapter.GwEventItemAdapter
import com.dadoutek.uled.gateway.bean.DbGateway
import com.dadoutek.uled.gateway.bean.GwTagBean
import com.dadoutek.uled.gateway.util.GsonUtil
import com.dadoutek.uled.model.Constant.*
import com.dadoutek.uled.model.DbModel.DBUtils
import com.dadoutek.uled.model.HttpModel.GwModel
import com.dadoutek.uled.model.Opcode
import com.dadoutek.uled.network.GwGattBody
import com.dadoutek.uled.network.NetworkObserver
import com.dadoutek.uled.tellink.TelinkLightApplication
import com.dadoutek.uled.tellink.TelinkLightService
import com.dadoutek.uled.util.DensityUtil
import com.dadoutek.uled.util.TmtUtils
import com.telink.bluetooth.event.DeviceEvent
import com.telink.bluetooth.light.DeviceInfo
import com.telink.bluetooth.light.LightAdapter
import com.telink.util.Event
import com.telink.util.EventListener
import com.yanzhenjie.recyclerview.SwipeMenu
import com.yanzhenjie.recyclerview.SwipeMenuItem
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_event_list.*
import kotlinx.android.synthetic.main.bottom_version_ly.*
import kotlinx.android.synthetic.main.template_bottom_add_no_line.*
import kotlinx.android.synthetic.main.template_swipe_recycleview.*
import kotlinx.android.synthetic.main.toolbar.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jetbrains.anko.toast
import java.util.*
import java.util.concurrent.TimeUnit


/**
 * 创建者     ZCL
 * 创建时间   2020/3/3 14:46
 * 描述 等待老谭联调
 *  todo 修改时间段的发送到服务器 一次发送400字节 拼接到一起的
 * 更新者     $
 * 更新时间   $
 * 更新描述
 */
class GwEventListActivity : TelinkBaseActivity(), EventListener<String>, BaseQuickAdapter.OnItemChildClickListener {
    private lateinit var currentGwTag: GwTagBean
    private var addBtn2: Button? = null
    private var deleteBean: GwTagBean? = null
    private var connectCount: Int = 0
    private var disposableTimer: Disposable? = null
    private lateinit var mApp: TelinkLightApplication
    private var listOne = mutableListOf<GwTagBean>()
    private var listTwo = mutableListOf<GwTagBean>()
    private var checkedIdType: Int = 0
    private var dbGw: DbGateway? = null
    private var addBtn: Button? = null
    private var lastTime: Long = 0
    val adapter = GwEventItemAdapter(R.layout.event_item, listOne)
    val adapter2 = GwEventItemAdapter(R.layout.event_item, listTwo)
    private val function: (leftMenu: SwipeMenu, rightMenu: SwipeMenu, position: Int) -> Unit = { _, rightMenu, _ ->
        val menuItem = SwipeMenuItem(this@GwEventListActivity)// 创建菜单
        menuItem.height = ViewGroup.LayoutParams.MATCH_PARENT
        menuItem.weight = DensityUtil.dip2px(this, 500f)
        menuItem.textSize = 20
        menuItem.setBackgroundColor(getColor(R.color.red))
        menuItem.setText(R.string.delete)
        rightMenu.addMenuItem(menuItem)//添加进右侧菜单
    }

    fun initView() {
        disableConnectionStatusListener()
        toolbarTv.text = getString(R.string.event_list)
        toolbar.setNavigationIcon(R.drawable.icon_top_tab_back)
        this.mApp = this.application as TelinkLightApplication

        img_function1.visibility = View.GONE
        image_bluetooth.setImageResource(R.drawable.icon_bluetooth)
        image_bluetooth.visibility = View.VISIBLE

        add_group_btn_tv.text = getString(R.string.add)
        var emptyView = View.inflate(this, R.layout.empty_view, null)
        addBtn = emptyView.findViewById<Button>(R.id.add_device_btn)
        addBtn?.text = getString(R.string.add)

        var emptyView2 = View.inflate(this, R.layout.empty_view, null)
        addBtn2 = emptyView2.findViewById<Button>(R.id.add_device_btn)
        addBtn2?.text = getString(R.string.add)

        adapter.emptyView = emptyView
        adapter2.emptyView = emptyView2
    }

    override fun onResume() {
        super.onResume()
        this.mApp.addEventListener(DeviceEvent.STATUS_CHANGED, this)
    }

    override fun onPause() {
        super.onPause()
        this.mApp.removeEventListener(DeviceEvent.STATUS_CHANGED, this)
    }

    override fun performed(event: Event<String>?) {
        if (event is DeviceEvent) {
            this.onDeviceEvent(event)
        }
    }

    private fun onDeviceEvent(event: DeviceEvent) {
        when (event.type) {
            DeviceEvent.STATUS_CHANGED -> {
                when {
                    event.args.status == LightAdapter.STATUS_LOGIN -> {
                        toolbar!!.findViewById<ImageView>(R.id.image_bluetooth).setImageResource(R.drawable.icon_bluetooth)
                    }
                    event.args.status == LightAdapter.STATUS_LOGOUT -> {
                        toolbar!!.findViewById<ImageView>(R.id.image_bluetooth).setImageResource(R.drawable.bluetooth_no)
                        retryConnect()
                    }
                    event.args.status == LightAdapter.STATUS_SET_GW_COMPLETED -> {//Dadou   Dadoutek2018
                        val deviceInfo = event.args
                        when (deviceInfo.gwVoipState) {
                            GW_TIME_ZONE_VOIP, GW_CONFIG_TIMER_LABEL_VOIP, GW_CONFIG_TIME_PERIVODE_LABEL_VOIP -> {
                                LogUtils.v("zcl-----------获取网关相关返回信息-------$deviceInfo")
                                disposableTimer?.dispose()
                                hideLoadingDialog()
                            }
                            GW_DELETE_TIMER_LABEL_VOIP, GW_DELETE_TIME_PERIVODE_LABEL_VOIP -> {
                                LogUtils.v("zcl-----------获取网关相关返回信息-------$deviceInfo")
                                hideLoadingDialog()
                                runOnUiThread { deleteSuceess() }
                            }
                        }
                    }
                    //获取设备mac
                    event.args.status == LightAdapter.STATUS_GET_DEVICE_MAC_COMPLETED -> {
                        //mac信息获取成功
                        val deviceInfo = event.args
                        LogUtils.v("zcl-----------蓝牙数据获取设备的macaddress-------$deviceInfo--------------${deviceInfo.sixByteMacAddress}")
                        dbGw?.sixByteMacAddr = deviceInfo.sixByteMacAddress
                    }
                    event.args.status == LightAdapter.STATUS_GET_DEVICE_MAC_FAILURE -> {
                    }
                }
            }
        }
    }

    private fun retryConnect() {
        connect(macAddress = dbGw!!.macAddr, fastestMode = true)?.subscribe(
                object : NetworkObserver<DeviceInfo?>() {
                    override fun onNext(t: DeviceInfo) {
                        TmtUtils.midToast(this@GwEventListActivity, getString(R.string.config_success))
                        toolbar!!.findViewById<ImageView>(R.id.image_bluetooth).setImageResource(R.drawable.icon_bluetooth)
                    }

                    override fun onError(e: Throwable) {
                        super.onError(e)
                        TmtUtils.midToast(this@GwEventListActivity, getString(R.string.connect_fail))
                        var intent = Intent(this@GwEventListActivity, GwDeviceDetailActivity::class.java)
                        startActivity(intent)
                        DBUtils.saveGateWay(dbGw!!, true)
                        finish()
                    }
                }
        )
    }

    private fun deleteSuceess() {
        runOnUiThread {
            hideLoadingDialog()
            disposableTimer?.dispose()

            if (dbGw?.type == 0) {//定时
                listOne.remove(deleteBean)
                dbGw?.tags = GsonUtils.toJson(listOne)//赋值时一定要转换为gson字符串
                isShowAdd(listOne)
            } else {
                listTwo.remove(deleteBean)
                dbGw?.timePeriodTags = GsonUtils.toJson(listTwo)//赋值时一定要转换为gson字符串
                isShowAdd(listTwo)
            }

            DBUtils.saveGateWay(dbGw!!, true)
            addGw(dbGw!!)//添加网关就是更新网关

            adapter.notifyDataSetChanged()
            adapter2.notifyDataSetChanged()
        }
    }

    /**
     * 蓝牙直连时发送时区信息
     */
    @RequiresApi(Build.VERSION_CODES.O)
    private fun sendTimeZoneParmars() {
        showLoadingDialog(getString(R.string.please_wait))
        disposableTimer?.dispose()
        disposableTimer = Observable.timer(2000, TimeUnit.MILLISECONDS)
                .observeOn(Schedulers.io())
                .subscribeOn(AndroidSchedulers.mainThread())
                .subscribe {
                    hideLoadingDialog()
                    runOnUiThread { ToastUtils.showLong(getString(R.string.get_time_zone_fail)) }
                    intent = Intent(this, GwDeviceDetailActivity::class.java)
                    startActivity(intent)
                    DBUtils.saveGateWay(dbGw!!, true)
                    finish()
                }
        val default = TimeZone.getDefault()
        val name = default.getDisplayName(true, TimeZone.SHORT)
        val split = if (name.contains("+")) //0正时区 1负时区
            name.split("+")
        else
            name.split("-")

        val time = split[1].split(":")// +/- 08:46
        val tzHour = if (name.contains("+"))
            time[0].toInt() or (0b00000000)
        else
            time[0].toInt() or (0b10000000)

        val tzMinutes = time[1].toInt()

        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH) + 1
        val day = calendar.get(Calendar.DAY_OF_MONTH)
        val hour: Int = calendar.get(Calendar.HOUR_OF_DAY)
        val minute = calendar.get(Calendar.MINUTE)
        val second = calendar.get(Calendar.SECOND)
        val week = calendar.get(Calendar.DAY_OF_WEEK) - 1
        val yearH = (year shr 8) and (0xff)
        val yearL = year and (0xff)

        var params = byteArrayOf(tzHour.toByte(), tzMinutes.toByte(), yearH.toByte(),
                yearL.toByte(), month.toByte(), day.toByte(), hour.toByte(), minute.toByte(), second.toByte(), week.toByte())
        TelinkLightService.Instance()?.sendCommandResponse(Opcode.CONFIG_GW_SET_TIME_ZONE, dbGw?.meshAddr ?: 0, params, "1")
    }

    @RequiresApi(Build.VERSION_CODES.O)
    @SuppressLint("SetTextI18n")
    fun initData() {
        checkedIdType = event_timer_mode.id
        dbGw = intent.getParcelableExtra<DbGateway>("data")
        if (dbGw == null) {
            TmtUtils.midToast(this@GwEventListActivity, getString(R.string.no_get_device_info))
            intent = Intent(this, GwDeviceDetailActivity::class.java)
            startActivity(intent)
            finish()
        }

        if (TelinkLightApplication.getApp().isConnectGwBle) {//直连时候获取版本号
            val disposable = Commander.getDeviceVersion(dbGw!!.meshAddr).subscribe(
                    { s: String ->
                        dbGw!!.version = s
                        bottom_version_number.text = dbGw?.version
                        DBUtils.saveGateWay(dbGw!!, true)
                    }, {
                ToastUtils.showLong(getString(R.string.get_version_fail))
            })
        }
        bottom_version_number.text = dbGw?.version

        toolbarTv.text = getString(R.string.device_name) + dbGw?.name

        swipe_recycleView.layoutManager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)
        swipe_recycleView.isItemViewSwipeEnabled = false //侧滑删除，默认关闭。
        // 设置监听器。
        swipe_recycleView.setSwipeMenuCreator(function)
        swipe_recycleView.setOnItemMenuClickListener { menuBridge, adapterPosition ->
            menuBridge.closeMenu()
            deleteBean = listOne[adapterPosition]
            deleteTimerLable(deleteBean!!, Date().time)
        }
        swipe_recycleView.adapter = adapter


        swipe_recycleView2.layoutManager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)
        swipe_recycleView2.isItemViewSwipeEnabled = false //侧滑删除，默认关闭。
        // 设置监听器。
        swipe_recycleView2.setSwipeMenuCreator(function)
        swipe_recycleView2.setOnItemMenuClickListener { menuBridge, adapterPosition ->
            menuBridge.closeMenu()
            deleteBean = listTwo[adapterPosition]
            deleteTimerLable(deleteBean!!, Date().time)
        }
        swipe_recycleView2.adapter = adapter2


        getNewData()
        dbGw?.type = 0
        if (TelinkLightApplication.getApp().isConnectGwBle)//直连发送时区 不是直连不发
            sendTimeZoneParmars()
    }

    private fun getNewData() {
        listOne.clear()
        listTwo.clear()
        if (!TextUtils.isEmpty(dbGw?.tags)) {
            val elements = GsonUtil.stringToList(dbGw?.tags, GwTagBean::class.java)
            listOne.addAll(elements)
            listOne.sortBy { gwTagBean -> gwTagBean.tagId }
            listOne.forEach {
                it.weekStr = getWeekStr(it.week)
            }
            val list = listOne.filter { it.status == 1 }
            if (list.isNotEmpty()) {
                changeRecycleView()
            }

            if (dbGw?.type == 0) {
                changeRecycleView()
                isShowAdd(listOne)
            }
        }
        if (!TextUtils.isEmpty(dbGw?.timePeriodTags)) {
            val elements = GsonUtil.stringToList(dbGw?.timePeriodTags, GwTagBean::class.java)
            listTwo.addAll(elements)
            listTwo.sortBy { gwTagBean -> gwTagBean.tagId }
            listTwo.forEach {
                it.weekStr = getWeekStr(it.week)
            }

            val list = listTwo.filter { it.status == 1 }

            if (list.isNotEmpty()) {
                changeRecycleView()
            }
            if (dbGw?.type == 1) {
                changeRecycleView2()
                isShowAdd(listTwo)
            }
        }
    }

    private fun isShowAdd(list: MutableList<GwTagBean>) {
        if (list.size > 0)
            add_group_btn?.visibility = View.VISIBLE
        else
            add_group_btn?.visibility = View.GONE
    }

    @RequiresApi(Build.VERSION_CODES.O)
    @SuppressLint("SetTextI18n")
    fun initListener() {
        toolbar.setNavigationOnClickListener {
            intent = Intent(this, GwDeviceDetailActivity::class.java)
            startActivity(intent)
            finish()
        }
        addBtn?.setOnClickListener { addNewTag() }
        addBtn2?.setOnClickListener { addNewTag() }
        add_group_btn?.setOnClickListener { addNewTag() }

        event_timer_mode.setOnClickListener {
            changeRecycleView()
            dbGw?.type = 0///网关模式 0定时 1循环
            isShowAdd(listOne)
        }

        event_time_pattern_mode.setOnClickListener {
            changeRecycleView2()
            dbGw?.type = 1///网关模式 0定时 1循环
            isShowAdd(listTwo)
        }

        adapter.onItemChildClickListener = this
        adapter2.onItemChildClickListener = this
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onItemChildClick(adapter: BaseQuickAdapter<*, *>?, view: View?, position: Int) {
        dbGw?.pos = position
        when (view?.id) {
            R.id.item_event_ly -> {
                val intent = Intent(this, GwConfigTagActivity::class.java)
                dbGw?.addTag = 1//不是新的
                if (dbGw?.type == 0) //定时
                    dbGw?.tags = GsonUtils.toJson(listOne)//赋值时一定要转换为gson字符串
                else
                    dbGw?.timePeriodTags = GsonUtils.toJson(listTwo)//赋值时一定要转换为gson字符串

                intent.putExtra("data", dbGw)
                startActivityForResult(intent, 1000)
            }
            R.id.item_event_switch -> {
                currentGwTag = if (adapter == adapter2)
                    listTwo[position]
                else
                    listOne[position]

                if ((view as CheckBox).isChecked)
                    currentGwTag.status = 1
                else
                    currentGwTag.status = 0
                showLoadingDialog(getString(R.string.please_wait))
                if (currentGwTag.status == 1) {//执行开的操作 status; //开1 关0
                    isMutexType(currentGwTag, adapter)
                } else {
                    sendOpenOrCloseGw(currentGwTag, false)
                }
            }
        }
    }

    private fun changeRecycleView2() {
        swipe_recycleView.visibility = View.GONE
        swipe_recycleView2.visibility = View.VISIBLE
        event_timer_mode.setTextColor(getColor(R.color.gray9))
        event_time_pattern_mode.setTextColor(getColor(R.color.blue_text))
        adapter2.notifyDataSetChanged()
    }

    private fun changeRecycleView() {
        swipe_recycleView.visibility = View.VISIBLE
        swipe_recycleView2.visibility = View.GONE
        event_timer_mode.setTextColor(getColor(R.color.blue_text))
        event_time_pattern_mode.setTextColor(getColor(R.color.gray9))
        adapter.notifyDataSetChanged()
    }

    /**
     * 是否是互斥的类型
     */
    @RequiresApi(Build.VERSION_CODES.O)
    private fun isMutexType(dbGwTag: GwTagBean, adapterNow: BaseQuickAdapter<*, *>?) {
        val taskList = GsonUtil.stringToList(dbGwTag.tasks, GwTagBean::class.java)
        if (dbGwTag.status == 1) {//开启任务
            if (taskList.size > 0) {//如果有标签时间需要进行对比自己里面有没有互斥
                if (adapterNow == adapter) {//定时模式 开启任务判断
                    //获取要开启的标签的有效时间
                    val currentAllTime = getGwTimerAllTime(dbGwTag)

                    //1.获取定时已开启的tag
                    val oldList = mutableListOf<GwTimeAndDataBean>()
                    val listOneOpen = listOne.filter { it.status == 1 && taskList.size > 0 }//时间任务大于0个
                    listOneOpen.forEach {
                        oldList.addAll(getGwTimerAllTime(it))//获取所有日期的时间
                    }

                    val canOpen = canOpenTG(currentAllTime, oldList, true)
                    if (!canOpen) {
                        listOne[dbGw?.pos ?: 0].status = 0
                        hideLoadingDialog()
                        dbGw?.tags = GsonUtils.toJson(listOne)
                        DBUtils.saveGateWay(dbGw!!, true)
                        addGw(dbGw!!)//添加网关就是更新网关
                        return
                    }
                } else {//时间段开启任务判断
                    //获取要开启的标签的有效时间
                    val currentAllTime = getGwTimerAllTime(dbGwTag)

                    //1.获取定时已开启的tag
                    val oldList = mutableListOf<GwTimeAndDataBean>()
                    //it.tasks.size代表该标签有时间或者时间段
                    val listTwoOpen = listTwo.filter {
                        it.status == 1 && GsonUtil.stringToList(it.tasks, GwTagBean::class.java).size > 0
                    }

                    listTwoOpen.forEach {
                        oldList.addAll(getGwTimerAllTime(it))
                    }

                    val canOpen = canOpenTG(currentAllTime, oldList, false)
                    if (!canOpen) {
                        listTwo[dbGw?.pos ?: 0].status = 0
                        dbGw?.timePeriodTags = GsonUtils.toJson(listTwo)
                        DBUtils.saveGateWay(dbGw!!, true)
                        addGw(dbGw!!)//添加网关就是更新网关
                        hideLoadingDialog()
                        return
                    }
                }
            }
        }
        if (adapterNow == adapter2) {//执行点击操作状态下并且没有冲突走到这里 当前点击的adapter是循环的就重置定时 反之一样
            listOne.forEach {
                it.status = 0
            }
        } else {
            listTwo.forEach {
                it.status = 0
            }
        }
        adapter.notifyDataSetChanged()
        adapter2.notifyDataSetChanged()

        dbGw?.tags = GsonUtils.toJson(listOne)//赋值时一定要转换为gson字符串
        dbGw?.timePeriodTags = GsonUtils.toJson(listTwo)//赋值时一定要转换为gson字符串
        DBUtils.saveGateWay(dbGw!!, true)
        sendOpenOrCloseGw(dbGwTag, true)
    }


    private fun canOpenTG(currentAllTime: MutableList<GwTimeAndDataBean>, oldList: MutableList<GwTimeAndDataBean>, isTimer: Boolean): Boolean {
        var canOpenTag = true
        currentAllTime.forEach {
            if (canOpenTag)
                oldList.forEach { itOld ->
                    if (isTimer) {
                        if (it.id != itOld.id && it.week == itOld.week && it.startTime == itOld.startTime) {
                            canOpenTag = false
                            TmtUtils.midToast(this@GwEventListActivity, getString(R.string.tag_mutex, itOld.name))
                            updateBackStatus(true)
                            return@forEach
                        }
                    } else {
                        if (it.id != itOld.id && it.week == itOld.week) {
                            if ((it.startTime < itOld.endTime && it.startTime >= itOld.startTime) || (it.endTime >= itOld.endTime && it.endTime <= itOld.startTime)) {
                                canOpenTag = false
                                TmtUtils.midToast(this@GwEventListActivity, getString(R.string.tag_mutex, itOld.name))
                                updateBackStatus(true)
                                return@forEach
                            }
                        }
                    }
                }
        }
        return canOpenTag
    }

    private fun updateBackStatus(isMutex: Boolean) {
        GlobalScope.launch(Dispatchers.Main) {
            if (isMutex) {
                if (dbGw?.type == 0)
                    listOne[dbGw?.pos ?: 0].status = 0
                else
                    listTwo[dbGw?.pos ?: 0].status = 0
            } else {
                if (currentGwTag.status == 0)
                    currentGwTag.status = 1
                else
                    currentGwTag.status = 0
            }

            adapter.notifyDataSetChanged()
            adapter2.notifyDataSetChanged()
        }
    }

    private fun getGwTimerAllTime(dbGwTag: GwTagBean): MutableList<GwTimeAndDataBean> {
        val tasks = GsonUtil.stringToList(dbGwTag.tasks, GwTagBean::class.java)
        var splitWeek: List<String> = dbGwTag.weekStr.split(",")
        if (splitWeek.size == 1) {
            if (splitWeek[0] == getString(R.string.every_day)) {
                splitWeek = mutableListOf(getString(R.string.monday), getString(R.string.tuesday), getString(R.string.wednesday),
                        getString(R.string.thursday), getString(R.string.friday), getString(R.string.saturday), getString(R.string.sunday))
            } else if (splitWeek[0] == getString(R.string.only_one)) {
                splitWeek = getOnlyOne()
            }
        }
        val listCurrent = mutableListOf<GwTimeAndDataBean>()
        splitWeek.forEach {
            //获取需要判断的有效时间
            tasks.forEach { itTask ->
                val startTime = itTask.startHour * 60 + itTask.startMins
                val endTime = itTask.endHour * 60 + itTask.endMins
                listCurrent.add(GwTimeAndDataBean(dbGwTag.tagId, dbGwTag.tagName, it, startTime, endTime))
            }
        }
        return listCurrent
    }

    private fun getWeekStr(week: Int): String {
        var weekStr = StringBuilder()
        when (week) {
            0b00000000 -> weekStr.append(getString(R.string.only_one))
            0b10000000 -> weekStr.append(getString(R.string.every_day))
            else -> {
                var binaryString = Integer.toBinaryString(week)
                val length1 = binaryString.length
                for (z in 0..8)
                    if (binaryString.length < 8)
                        binaryString = "0$binaryString"
                    else break
                val length = binaryString.length
                val intRange = (0 until length).reversed()
                for (i in intRange) {
                    val c = binaryString[i]
                    if (c.toString() == "1") {
                        if (i != 0 &&  i != length1 - 1)
                            weekStr.append(",")
                        when (i) {
                            0 -> weekStr.append(getString(R.string.sunday))
                            1 -> weekStr.append(getString(R.string.monday))
                            2 -> weekStr.append(getString(R.string.tuesday))
                            3 -> weekStr.append(getString(R.string.wednesday))
                            4 -> weekStr.append(getString(R.string.thursday))
                            5 -> weekStr.append(getString(R.string.friday))
                            6 -> weekStr.append(getString(R.string.saturday))
                            7 -> weekStr.append(getString(R.string.every_day))
                        }
                    }
                }
            }
        }
        return weekStr.toString()
    }

    private fun getOnlyOne(): MutableList<String> {
        var weekDay = when (Calendar.getInstance().get(Calendar.DAY_OF_WEEK) - 1) {
            0 -> getString(R.string.sunday)
            1 -> getString(R.string.monday)
            2 -> getString(R.string.tuesday)
            3 -> getString(R.string.wednesday)
            4 -> getString(R.string.thursday)
            5 -> getString(R.string.friday)
            6 -> getString(R.string.saturday)
            else -> getString(R.string.sunday)
        }
        return mutableListOf(weekDay)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun sendOpenOrCloseGw(dbGwTag: GwTagBean, isMutex: Boolean) {
        disposableTimer?.dispose()
        connectCount++
        disposableTimer = Observable.timer(2000, TimeUnit.MILLISECONDS)
                .subscribe {
                    if (connectCount < 3)
                        sendOpenOrCloseGw(dbGwTag, isMutex)
                    else {
                        updateBackStatus(isMutex)
                        hideLoadingDialog()
                        runOnUiThread { TmtUtils.midToast(this@GwEventListActivity, getString(R.string.gate_way_label_switch_fail)) }
                    }
                }

        var meshAddress = dbGw?.meshAddr ?: 0
        //p = byteArrayOf(0x02, Opcode.GROUP_BRIGHTNESS_MINUS, 0x00, 0x00, 0x03, Opcode.GROUP_CCT_MINUS, 0x00, 0x00)
        //从第八位开始opcode, 设备meshAddr  参数11-12-13-14 15-16-17-18
        val calendar = Calendar.getInstance()
        val month = calendar.get(Calendar.MONTH) + 1
        val day = calendar.get(Calendar.DAY_OF_MONTH)
        LogUtils.v("zcl-----------当前日期:--------$month-$day")

        val opcodeHead = if (dbGwTag.isTimer())
            Opcode.CONFIG_GW_TIMER_LABLE_HEAD
        else
            Opcode.CONFIG_GW_TIMER_PERIOD_LABLE_HEAD

        if (!TelinkLightApplication.getApp().isConnectGwBle) {
            var labHeadPar = byteArrayOf(0x11, 0x11, 0x11, 0, 0, 0, 0,
                    opcodeHead, 0x11, 0x02,
                    dbGwTag.tagId.toByte(), dbGwTag.status.toByte(),
                    dbGwTag.week.toByte(), 0, month.toByte(), day.toByte(), 0, 0, 0, 0)
            LogUtils.v("zcl-----------发送到服务器标签列表开关-------$labHeadPar")

            val encoder = Base64.getEncoder()
            val s = encoder.encodeToString(labHeadPar)
            val gattBody = GwGattBody()
            gattBody.data = s
            gattBody.ser_id = GW_GATT_LABEL_SWITCH
            gattBody.macAddr = dbGw?.macAddr
            gattBody.tagName = dbGwTag.tagName
            sendToServer(gattBody)
        } else {
            var labHeadPar = byteArrayOf(dbGwTag.tagId.toByte(), dbGwTag.status.toByte(),//标签开与关
                    dbGwTag.week.toByte(), 0, month.toByte(), day.toByte(), 0, 0)
            TelinkLightService.Instance().sendCommandResponse(opcodeHead, meshAddress, labHeadPar, "1")
            LogUtils.v("zcl-----------发送命令0xf6-------")
        }
    }

    private fun sendToServer(gattBody: GwGattBody) {
        GwModel.sendToGatt(gattBody)?.subscribe(object : NetworkObserver<String?>() {
            override fun onNext(t: String) {
                LogUtils.v("zcl------------------$t")
            }

            override fun onError(e: Throwable) {
                super.onError(e)
                LogUtils.e("zcl------------------${e.message}")
            }
        })
    }

    /**
     * 添加网关
     */
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

    @RequiresApi(Build.VERSION_CODES.O)
    private fun deleteTimerLable(gwTagBean: GwTagBean, currentTime: Long) {
        showLoadingDialog(getString(R.string.please_wait))
        disposableTimer?.dispose()
        connectCount++
        val opcodedelete = if (gwTagBean.isTimer())
            Opcode.CONFIG_GW_TIMER_DELETE_LABLE
        else
            Opcode.CONFIG_GW_TIMER_PERIOD_DELETE_LABLE

        if (TelinkLightApplication.getApp().isConnectGwBle) {
            setTimerDelay(gwTagBean, currentTime, 2000)
            //11-18 11位labelId
            GlobalScope.launch(Dispatchers.Main) {
                if (currentTime - lastTime < 400)
                    delay(400)
                var paramer = byteArrayOf(gwTagBean.tagId.toByte(), 0, 0, 0, 0, 0, 0, 0)
                TelinkLightService.Instance().sendCommandResponse(opcodedelete, dbGw?.meshAddr ?: 0, paramer, "1")
            }
        } else {
            setTimerDelay(gwTagBean, currentTime, 6500)
            var labHeadPar = byteArrayOf(0x11, 0x11, 0x11, 0, 0, 0, 0, opcodedelete, 0x11, 0x02,
                    gwTagBean.tagId.toByte(), 0, 0, 0, 0, 0, 0, 0, 0, 0)
            LogUtils.v("zcl-----------发送到服务器标签列表删除标签-------$labHeadPar")
            val encoder = Base64.getEncoder()
            val s = encoder.encodeToString(labHeadPar)
            val gattBody = GwGattBody()
            gattBody.data = s
            gattBody.ser_id = GW_GATT_DELETE_LABEL
            gattBody.macAddr = dbGw?.macAddr
            gattBody.tagName = gwTagBean.tagName
            sendToServer(gattBody)
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun setTimerDelay(gwTagBean: GwTagBean, currentTime: Long, delay: Long) {
        disposableTimer = Observable.timer(delay, TimeUnit.MILLISECONDS)
                .subscribe {
                    if (connectCount < 3)
                        deleteTimerLable(gwTagBean, currentTime)
                    else
                        GlobalScope.launch(Dispatchers.Main) {
                            hideLoadingDialog()
                            TmtUtils.midToast(this@GwEventListActivity, getString(R.string.delete_gate_way_label_fail))
                        }
                }
    }

    private fun addNewTag() {

        var list = if (dbGw?.type == 0)
            listOne
        else
            listTwo
        if (list.size >= 20)
            toast(getString(R.string.gate_way_time_max))
        else {
            val intent = Intent(this@GwEventListActivity, GwConfigTagActivity::class.java)
            dbGw?.addTag = 0//创建新的
            intent.putExtra("data", dbGw)
            startActivityForResult(intent, 1000)
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_event_list)
        initView()
        initData()
        initListener()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK && requestCode == 1000) {
            dbGw = data?.getParcelableExtra<DbGateway>("data")
            getNewData()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        disposableTimer?.dispose()
        //定时
        if (checkedIdType == R.id.event_timer_mode) {//定時模式  0定时 1循环
            dbGw?.tags = GsonUtils.toJson(listOne)//赋值时一定要转换为gson字符串
            dbGw?.timePeriodTags = GsonUtils.toJson(listTwo)
        } else {//時間段模式
            dbGw?.tags = GsonUtils.toJson(listOne)
            dbGw?.timePeriodTags = GsonUtils.toJson(listTwo)//赋值时一定要转换为gson字符串
        }
        DBUtils.saveGateWay(dbGw!!, true)
    }

    override fun receviedGwCmd2000(serId: String) {
        if (GW_GATT_LABEL_SWITCH == serId.toInt()) {
            runOnUiThread { hideLoadingDialog() }
            disposableTimer?.dispose()
        } else if (GW_GATT_DELETE_LABEL == serId?.toInt()) {
            runOnUiThread { deleteSuceess() }
        }
    }

}