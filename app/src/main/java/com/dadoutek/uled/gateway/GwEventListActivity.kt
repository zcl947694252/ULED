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
class GwEventListActivity : TelinkBaseActivity(), EventListener<String> {
    private var deleteBean: GwTagBean? = null
    private var connectCount: Int = 0
    private var disposableTimer: Disposable? = null
    private lateinit var mApp: TelinkLightApplication
    private var listOne = mutableListOf<GwTagBean>()
    private var listTwo = mutableListOf<GwTagBean>()
    private var lastGwTag: GwTagBean? = null
    private var checkedIdType: Int = 0
    private var dbGw: DbGateway? = null
    private var addBtn: Button? = null
    private var lastTime: Long = 0
    val list = mutableListOf<GwTagBean>()
    val adapter = GwEventItemAdapter(R.layout.event_item, list)
    var lastType = 0
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

        adapter.emptyView = emptyView
    }

    override fun onResume() {
        super.onResume()
        this.mApp.addEventListener(DeviceEvent.STATUS_CHANGED, this)
        getNewData()
        changeData(checkedIdType)
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
                        connect(macAddress = dbGw!!.macAddr, retryTimes = 2)?.subscribe(
                                object : NetworkObserver<DeviceInfo?>() {
                                    override fun onNext(t: DeviceInfo) {
                                        ToastUtils.showShort(getString(R.string.config_success))
                                        toolbar!!.findViewById<ImageView>(R.id.image_bluetooth).setImageResource(R.drawable.icon_bluetooth)
                                    }

                                    override fun onError(e: Throwable) {
                                        super.onError(e)
                                        ToastUtils.showShort(getString(R.string.connect_fail))
                                       var intent = Intent(this@GwEventListActivity, GwDeviceDetailActivity::class.java)
                                        startActivity(intent)
                                        DBUtils.saveGateWay(dbGw!!, true)
                                        finish()
                                    }
                                }
                        )
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
                                deleteSuceess()
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

    private fun deleteSuceess() {
        runOnUiThread {    hideLoadingDialog()
        disposableTimer?.dispose()
        list.remove(deleteBean)

        if (dbGw?.type == 0) {
            //定时
            listOne.remove(deleteBean)
            dbGw?.tags = GsonUtils.toJson(list)//赋值时一定要转换为gson字符串
        } else {
            listTwo.remove(deleteBean)
            dbGw?.timePeriodTags = GsonUtils.toJson(list)//赋值时一定要转换为gson字符串
        }

        DBUtils.saveGateWay(dbGw!!, true)

        dbGw?.let {
            addGw(it)//添加网关就是更新网关
        }
        if (list.size <= 0)
            add_group_btn?.visibility = View.GONE
        else
            add_group_btn?.visibility = View.VISIBLE

      adapter.notifyDataSetChanged() }
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
                    ToastUtils.showLong(getString(R.string.get_time_zone_fail))
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

    private fun sendDeviceMacParmars() {
        var params = byteArrayOf(0, 0, 0, 0, 0, 0, 0, 0)
        TelinkLightService.Instance()?.sendCommandResponse(Opcode.CONFIG_GW_GET_MAC, dbGw?.meshAddr ?: 0, params, "0")
    }

    @RequiresApi(Build.VERSION_CODES.O)
    @SuppressLint("SetTextI18n")
    fun initData() {
        checkedIdType = event_timer_mode.id
        dbGw = intent.getParcelableExtra<DbGateway>("data")
        if (dbGw == null) {
            ToastUtils.showShort(getString(R.string.no_get_device_info))
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
        getNewData()

        toolbarTv.text = getString(R.string.device_name) + dbGw?.name

        swipe_recycleView.layoutManager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)
        swipe_recycleView.isItemViewSwipeEnabled = false //侧滑删除，默认关闭。
        // 设置监听器。
        swipe_recycleView.setSwipeMenuCreator(function)
        swipe_recycleView.setOnItemMenuClickListener { menuBridge, adapterPosition ->
            menuBridge.closeMenu()
            deleteBean = list[adapterPosition]
            deleteTimerLable(deleteBean!!, Date().time)
        }
        swipe_recycleView.adapter = adapter


        getNewData()
        changeData(checkedIdType)
        // sendDeviceMacParmars()
        if (TelinkLightApplication.getApp().isConnectGwBle)//直连发送时区 不是直连不发
            sendTimeZoneParmars()
    }

    private fun getNewData() {
        listOne.clear()
        listTwo.clear()
        if (!TextUtils.isEmpty(dbGw?.tags)) {
            val elements = GsonUtil.stringToList(dbGw?.tags, GwTagBean::class.java)
            listOne.addAll(elements)
        }
        if (!TextUtils.isEmpty(dbGw?.timePeriodTags)) {
            val elements = GsonUtil.stringToList(dbGw?.timePeriodTags, GwTagBean::class.java)
            listTwo.addAll(elements)
        }
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
        add_group_btn?.setOnClickListener { addNewTag() }

        event_mode_gp.setOnCheckedChangeListener { _, checkedId ->
            if (checkedIdType == event_timer_mode.id)
                dbGw?.type = 0///网关模式 0定时 1循环
            else
                dbGw?.type = 1///网关模式 0定时 1循环
            checkedIdType = checkedId
            changeData(checkedIdType)
        }

        adapter.setOnItemChildClickListener { _, view, position ->
            dbGw?.pos = position
            when (view.id) {
                R.id.item_event_ly -> {
                    val intent = Intent(this, GwConfigTagActivity::class.java)
                    dbGw?.addTag = 1//不是新的
                    if (dbGw?.type == 0) //定时
                        dbGw?.tags = GsonUtils.toJson(list)//赋值时一定要转换为gson字符串
                    else
                        dbGw?.timePeriodTags = GsonUtils.toJson(list)//赋值时一定要转换为gson字符串

                    intent.putExtra("data", dbGw)
                    startActivityForResult(intent, 1000)
                }
                R.id.item_event_switch -> {
                    val dbGwTag = list[position]
                    if ((view as CheckBox).isChecked)
                        dbGwTag.status = 1
                    else
                        dbGwTag.status = 0

                    if (dbGwTag.status == 1) {//执行开的操作 status; //开1 关0
                        showLoadingDialog(getString(R.string.please_wait))
                        isMutexType(dbGwTag)
                    } else {
                        sendOpenOrCloseGw(dbGwTag)
                    }
                }
            }
        }
    }

    /**
     * 是否是互斥的类型
     */
    @RequiresApi(Build.VERSION_CODES.O)
    private fun isMutexType(dbGwTag: GwTagBean) {
        if (lastType != checkedIdType) {//当前选择的类型不是上一个类型 定时/时间段
            if (lastType == event_timer_mode.id) {//恢复上一类型的数据
                listOne.forEach {
                    it.status = 0
                }
                dbGw?.tags = GsonUtils.toJson(listOne)//赋值时一定要转换为gson字符串
                dbGw?.timePeriodTags = GsonUtils.toJson(list)//赋值时一定要转换为gson字符串
            } else {
                listTwo.forEach {
                    it.status = 0
                }
                dbGw?.tags = GsonUtils.toJson(list)//赋值时一定要转换为gson字符串
                dbGw?.timePeriodTags = GsonUtils.toJson(listTwo)//赋值时一定要转换为gson字符串
            }
        }

        DBUtils.saveGateWay(dbGw!!, true)

        val taskList = GsonUtil.stringToList(dbGwTag.tasks, GwTagBean::class.java)
        if (dbGwTag.status == 1 && taskList.size > 0) {//开启任务 并且该标签有需要执行的时间
            if (checkedIdType == event_timer_mode.id) {//定时模式 开启任务判断
                //获取要开启的标签的有效时间
                val currentAllTime = getGwTimerAllTime(dbGwTag)

                //1.获取定时已开启的tag
                val oldList = mutableListOf<GwTimeAndDataBean>()
                val listOneOpen = listOne.filter { it.status == 1 && taskList.size > 0 }//时间任务大于0个
                listOneOpen.forEach {
                    oldList.addAll(getGwTimerAllTime(it))
                }

                val canOpen = canOpenTG(currentAllTime, oldList, true)
                if (!canOpen) {
                    hideLoadingDialog()
                    dbGwTag.status = 0
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
                    hideLoadingDialog()
                    dbGwTag.status = 0
                    return
                }
            }
        }
        dbGw?.tags = GsonUtils.toJson(listOne)//赋值时一定要转换为gson字符串
        dbGw?.timePeriodTags = GsonUtils.toJson(listTwo)//赋值时一定要转换为gson字符串
        DBUtils.saveGateWay(dbGw!!, true)
        sendOpenOrCloseGw(dbGwTag)
    }


    private fun canOpenTG(currentAllTime: MutableList<GwTimeAndDataBean>, oldList: MutableList<GwTimeAndDataBean>, isTimer: Boolean): Boolean {
        var canOpenTag = true
        currentAllTime.forEach {
            oldList.forEach { itOld ->
                if (isTimer) {
                    if (it.id != itOld.id && it.week == itOld.week && it.startTime == itOld.startTime) {
                        canOpenTag = false
                        ToastUtils.showShort(getString(R.string.tag_mutex, itOld.name))
                        updateBackStatus()
                        return@forEach
                    }
                } else {
                    if (it.id != itOld.id && it.week == itOld.week) {
                        if ((it.startTime < itOld.endTime && it.startTime >= itOld.startTime) || (it.endTime <= itOld.endTime && it.endTime < itOld.startTime))
                            canOpenTag = false
                        ToastUtils.showShort(getString(R.string.tag_mutex, itOld.name))

                        updateBackStatus()
                        return@forEach
                    }
                }
            }
        }
        return canOpenTag
    }

    private fun updateBackStatus() {

        GlobalScope.launch(Dispatchers.Main) {
            list[dbGw?.pos ?: 0].status = 0
            adapter.notifyDataSetChanged()
        }

        if (checkedIdType == event_timer_mode.id) //定时
            dbGw?.tags = GsonUtils.toJson(list)//赋值时一定要转换为gson字符串
        else
            dbGw?.timePeriodTags = GsonUtils.toJson(list)//赋值时一定要转换为gson字符串

        DBUtils.saveGateWay(dbGw!!, true)

        addGw(dbGw!!)//添加网关就是更新网关

    }

    private fun getGwTimerAllTime(dbGwTag: GwTagBean): MutableList<GwTimeAndDataBean> {
        val tasks = GsonUtil.stringToList(dbGwTag.tasks, GwTagBean::class.java)
        var splitWeek = dbGwTag.weekStr.split(",")
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
    private fun sendOpenOrCloseGw(dbGwTag: GwTagBean) {
        disposableTimer?.dispose()
        connectCount++
        disposableTimer = Observable.timer(2000, TimeUnit.MILLISECONDS)
                .observeOn(Schedulers.io())
                .subscribeOn(AndroidSchedulers.mainThread())
                .subscribe {
                    if (connectCount < 3)
                        sendOpenOrCloseGw(dbGwTag)
                    else {
                        updateBackStatus()
                        ToastUtils.showShort(getString(R.string.gate_way_label_switch_fail))
                    }
                }

        var meshAddress = dbGw?.meshAddr ?: 0
        //p = byteArrayOf(0x02, Opcode.GROUP_BRIGHTNESS_MINUS, 0x00, 0x00, 0x03, Opcode.GROUP_CCT_MINUS, 0x00, 0x00)
        //从第八位开始opcode, 设备meshAddr  参数11-12-13-14 15-16-17-18
        val calendar = Calendar.getInstance()
        val month = calendar.get(Calendar.MONTH) + 1
        val day = calendar.get(Calendar.DAY_OF_MONTH)
        LogUtils.v("zcl-----------当前日期:--------$month-$day")
        lastType = checkedIdType
        lastGwTag = dbGwTag
        val opcodeHead = if (dbGwTag.isTimer())
            Opcode.CONFIG_GW_TIMER_LABLE_HEAD
        else
            Opcode.CONFIG_GW_TIMER_PERIOD_LABLE_HEAD

        if (!TelinkLightApplication.getApp().isConnectGwBle) {
            var labHeadPar = byteArrayOf(0x11, 0x11, 0x11, 0, 0, 0, 0,
                    Opcode.CONFIG_GW_TIMER_PERIOD_LABLE_TASK, 0x11, 0x02,
                    dbGwTag.tagId.toByte(), dbGwTag.status.toByte(),
                    dbGwTag.week.toByte(), 0, month.toByte(), day.toByte(), 0, 0, 0, 0)

            val encoder = Base64.getEncoder()
            val s = encoder.encodeToString(labHeadPar)
            val gattBody = GwGattBody()
            gattBody.data = s
            gattBody.ser_id = GW_GATT_LABEL_SWITCH
            gattBody.macAddr = dbGw?.macAddr
            gattBody.tagHead = 1
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

    private fun changeData(checkedId: Int) {
        list.clear()
        if (checkedId == R.id.event_timer_mode) {//定時模式  0定时 1循环
            dbGw?.type = 0
            list.addAll(listOne)
            event_timer_mode.setTextColor(getColor(R.color.blue_text))
            event_time_pattern_mode.setTextColor(getColor(R.color.gray9))
            dbGw?.tags = GsonUtils.toJson(list)//赋值时一定要转换为gson字符串
        } else {//時間段模式
            dbGw?.type = 1
            list.addAll(listTwo)
            event_timer_mode.setTextColor(getColor(R.color.gray9))
            event_time_pattern_mode.setTextColor(getColor(R.color.blue_text))
            dbGw?.timePeriodTags = GsonUtils.toJson(list)//赋值时一定要转换为gson字符串
        }
        DBUtils.saveGateWay(dbGw!!, true)
        if (list.size > 0)
            add_group_btn?.visibility = View.VISIBLE
        else
            add_group_btn?.visibility = View.GONE
        list.sortBy { gwTagBean -> gwTagBean.tagId }
        adapter.notifyDataSetChanged()
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

            val encoder = Base64.getEncoder()
            val s = encoder.encodeToString(labHeadPar)
            val gattBody = GwGattBody()
            gattBody.data = s
            gattBody.ser_id = GW_GATT_DELETE_LABEL
            gattBody.macAddr = dbGw?.macAddr
            gattBody.tagHead = 1
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
                            ToastUtils.showShort(getString(R.string.delete_gate_way_label_fail))
                        }
                }
    }

    private fun addNewTag() {
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
            changeData(checkedIdType)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        disposableTimer?.dispose()
        //定时
        if (checkedIdType == R.id.event_timer_mode) {//定時模式  0定时 1循环
            dbGw?.tags = GsonUtils.toJson(list)//赋值时一定要转换为gson字符串
        } else {//時間段模式
            dbGw?.timePeriodTags = GsonUtils.toJson(list)//赋值时一定要转换为gson字符串
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