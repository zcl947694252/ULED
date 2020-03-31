package com.dadoutek.uled.gateway

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.CheckBox
import android.widget.TextView
import androidx.annotation.RequiresApi
import com.blankj.utilcode.util.GsonUtils
import com.blankj.utilcode.util.LogUtils
import com.blankj.utilcode.util.ToastUtils
import com.dadoutek.uled.R
import com.dadoutek.uled.base.TelinkBaseActivity
import com.dadoutek.uled.gateway.adapter.GwEventItemAdapter
import com.dadoutek.uled.gateway.bean.DbGateway
import com.dadoutek.uled.gateway.bean.GwTagBean
import com.dadoutek.uled.gateway.util.GsonUtil
import com.dadoutek.uled.model.DbModel.DBUtils
import com.dadoutek.uled.model.HttpModel.GwModel
import com.dadoutek.uled.model.Opcode
import com.dadoutek.uled.network.GwGattBody
import com.dadoutek.uled.network.NetworkObserver
import com.dadoutek.uled.tellink.TelinkLightApplication
import com.dadoutek.uled.tellink.TelinkLightService
import com.telink.bluetooth.event.DeviceEvent
import com.telink.bluetooth.light.LightAdapter
import com.telink.util.Event
import com.telink.util.EventListener
import com.yanzhenjie.recyclerview.touch.OnItemMoveListener
import kotlinx.android.synthetic.main.activity_event_list.*
import kotlinx.android.synthetic.main.template_swipe_recycleview.*
import kotlinx.android.synthetic.main.toolbar.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jetbrains.anko.toast
import java.util.*


/**
 * 创建者     ZCL
 * 创建时间   2020/3/3 14:46
 * 描述 等待老谭联调
 *
 * 更新者     $
 * 更新时间   $
 * 更新描述
 */
class GwEventListActivity : TelinkBaseActivity(), View.OnClickListener, EventListener<String> {
    override fun performed(event: Event<String>?) {
        if (event is DeviceEvent) {
            this.onDeviceEvent(event)
        }
    }

    private fun onDeviceEvent(event: DeviceEvent) {
        when (event.type) {
            DeviceEvent.STATUS_CHANGED -> {
                when {
                    //获取设备mac
                    event.args.status == LightAdapter.STATUS_GET_DEVICE_MAC_COMPLETED -> {
                        //mac信息获取成功
                        val deviceInfo = event.args
                        LogUtils.v("zcl-----------蓝牙数据获取设备的macaddress-------$deviceInfo--------------${deviceInfo.sixByteMacAddress}")
                        dbGw?.sixByteMacAddr = deviceInfo.sixByteMacAddress
                    }
                    event.args.status == LightAdapter.STATUS_GET_DEVICE_MAC_FAILURE -> {
                        LogUtils.v("zcl-----------蓝牙数据-get DeviceMAC fail------")
                    }
                }
            }
        }
    }

    private lateinit var mApp: TelinkLightApplication
    private var dbGw: DbGateway? = null
    private var addBtn: Button? = null
    private var lin: View? = null
    private var lastTime: Long = 0
    val list = mutableListOf<GwTagBean>()
    val adapter = GwEventItemAdapter(R.layout.event_item, list)

    fun initView() {
        toolbarTv.text = getString(R.string.event_list)
        toolbar.setNavigationIcon(R.drawable.icon_top_tab_back)

        this.mApp = this.application as TelinkLightApplication
        this.mApp.addEventListener(DeviceEvent.STATUS_CHANGED, this)

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

    private fun sendTimeZoneParmars() {
        val default = TimeZone.getDefault()
        val name = default.getDisplayName(true, TimeZone.SHORT)
        LogUtils.v("zcl-----------获取市区-------${default.displayName}--------${default.id}" +
                "---${default.dstSavings}------$name----------")

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

        TelinkLightService.Instance()?.sendCommandNoResponse(Opcode.CONFIG_GW_SET_TIME_ZONE, dbGw?.meshAddr
                ?: 0, params)
        LogUtils.v("zcl---------蓝牙数据--dbGwmac:${dbGw?.macAddr}------${dbGw?.sixByteMacAddr}--------mes${dbGw?.meshAddr
                ?: 1000}--")//78-9c-e7-04-2e-bb
    }

    @SuppressLint("SetTextI18n")
    fun initData() {
        dbGw = intent.getParcelableExtra<DbGateway>("data")

        toolbarTv.text = getString(R.string.Gate_way) + dbGw?.name
        if (dbGw == null) {
            ToastUtils.showShort(getString(R.string.no_get_device_info))
            finish()
        }
        swipe_recycleView.layoutManager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)
        swipe_recycleView.adapter = adapter
        swipe_recycleView.isItemViewSwipeEnabled = true //侧滑删除，默认关闭。

        changeData(R.id.event_timer_mode)
        sendTimeZoneParmars()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    @SuppressLint("SetTextI18n")
    fun initListener() {
        toolbar.setNavigationOnClickListener { finish() }
        addBtn?.setOnClickListener(this)
        lin?.setOnClickListener(this)

        event_mode_gp.setOnCheckedChangeListener { _, checkedId ->
            list.clear()
            changeData(checkedId)
            dbGw?.let {
                addGw(it)
            }
        }
        swipe_recycleView.setOnItemMoveListener(object : OnItemMoveListener {
            override fun onItemDismiss(srcHolder: RecyclerView.ViewHolder?) {
                val position = srcHolder?.adapterPosition
                deleteTimerLable(list[position ?: 0], Date().time)
                list.removeAt(position ?: 0)

                if (dbGw?.type == 0) //定时
                    dbGw?.tags = GsonUtils.toJson(list)//赋值时一定要转换为gson字符串
                else
                    dbGw?.timePeriodTags = GsonUtils.toJson(list)//赋值时一定要转换为gson字符串

                DBUtils.saveGateWay(dbGw!!, true)
                dbGw?.let {
                    addGw(it)
                }
                adapter.notifyItemRemoved(position ?: 0)
            }

            override fun onItemMove(srcHolder: RecyclerView.ViewHolder?, targetHolder: RecyclerView.ViewHolder?): Boolean {
                return true
            }
        })
        adapter.setOnItemChildClickListener { _, view, position ->
            when (view.id) {
                R.id.item_event_ly -> {
                    val intent = Intent(this, GwConfigTagActivity::class.java)
                    dbGw?.pos = position
                    dbGw?.addTag = 1//不是新的
                    if (dbGw?.type == 0) //定时
                        dbGw?.tags = GsonUtils.toJson(list)//赋值时一定要转换为gson字符串
                    else
                        dbGw?.timePeriodTags = GsonUtils.toJson(list)//赋值时一定要转换为gson字符串

                    intent.putExtra("data", dbGw)
                    startActivity(intent)
                    finish()
                }
                R.id.item_event_switch -> {
                    if ((view as CheckBox).isChecked)
                        list[position].status = 1
                    else
                        list[position].status = 0

                    if (dbGw?.type == 0) //定时
                        dbGw?.tags = GsonUtils.toJson(list)//赋值时一定要转换为gson字符串
                    else
                        dbGw?.timePeriodTags = GsonUtils.toJson(list)//赋值时一定要转换为gson字符串

                    dbGw?.let {
                        DBUtils.saveGateWay(it, true)
                        addGw(it)
                    }
                    sendOpenOrCloseGw(list[position])
                }
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun sendOpenOrCloseGw(dbGwTag: GwTagBean) {
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

        if (TelinkLightApplication.getApp().offLine) {
            var labHeadPar = byteArrayOf(0x11, 0x11, 0x11, 0, 0, 0, 0,
                    Opcode.CONFIG_GW_TIMER_PERIOD_LABLE_TASK, 0x11, 0x02,
                    dbGwTag.tagId.toByte(), dbGwTag.status.toByte(),
                    dbGwTag.week.toByte(), 0, month.toByte(), day.toByte(), 0, 0)

            val encoder = Base64.getEncoder()
            val s = encoder.encodeToString(labHeadPar)
            val gattBody = GwGattBody()
            gattBody.data = s
            gattBody.macAddr = dbGw?.macAddr
            gattBody.isTagHead = 1
            sendToServer(gattBody)
        } else {
            var labHeadPar = byteArrayOf(dbGwTag.tagId.toByte(), dbGwTag.status.toByte(),
                    dbGwTag.week.toByte(), 0, month.toByte(), day.toByte(), 0, 0)
            TelinkLightService.Instance().sendCommandNoResponse(opcodeHead, meshAddress, labHeadPar)
        }

    }

    private fun sendToServer(gattBody: GwGattBody): Unit? {
        return GwModel.sendToGatt(gattBody)?.subscribe(object : NetworkObserver<String?>() {
            override fun onNext(t: String) {
                LogUtils.v("zcl------------------$t")
            }

            override fun onError(e: Throwable) {
                super.onError(e)
                LogUtils.e("zcl------------------${e.message}")
            }
        })
    }

    override fun onClick(v: View?) {
        addNewTag()
    }

    private fun changeData(checkedId: Int) {
        if (checkedId == R.id.event_timer_mode) {//定時模式  0定时 1循环
            dbGw?.type = 0
            event_timer_mode.setTextColor(getColor(R.color.blue_text))
            event_time_pattern_mode.setTextColor(getColor(R.color.gray9))
        } else {//時間段模式
            dbGw?.type = 1
            event_timer_mode.setTextColor(getColor(R.color.gray9))
            event_time_pattern_mode.setTextColor(getColor(R.color.blue_text))
        }
        val tags = if (dbGw?.type == 0) //定时
            dbGw?.tags
        else
            dbGw?.timePeriodTags

        if (!TextUtils.isEmpty(tags))
            list.addAll(GsonUtil.stringToList(tags, GwTagBean::class.java))
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

    private fun deleteTimerLable(gwTagBean: GwTagBean, currentTime: Long) {
        val opcodedelete = if (gwTagBean.isTimer())
            Opcode.CONFIG_GW_TIMER_DELETE_LABLE
        else
            Opcode.CONFIG_GW_TIMER_PERIOD_DELETE_LABLE

        //11-18 11位labelId
        if (currentTime - lastTime > 400) {
            var paramer = byteArrayOf(gwTagBean.tagId.toByte(), 0, 0, 0, 0, 0, 0, 0)
            TelinkLightService.Instance().sendCommandNoResponse(opcodedelete, dbGw?.meshAddr
                    ?: 0, paramer)
        } else {
            GlobalScope.launch(Dispatchers.Main) {
                delay(400)
                var paramer = byteArrayOf(gwTagBean.tagId.toByte(), 0, 0, 0, 0, 0, 0, 0)
                TelinkLightService.Instance().sendCommandNoResponse(opcodedelete, dbGw?.meshAddr
                        ?: 0, paramer)
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
            startActivity(intent)
            finish()
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

    override fun onDestroy() {
        super.onDestroy()
        dbGw?.let {
            if (it.type == 0) //定时
                it.tags = GsonUtils.toJson(list)//赋值时一定要转换为gson字符串
            else
                it.timePeriodTags = GsonUtils.toJson(list)//赋值时一定要转换为gson字符串
            DBUtils.saveGateWay(it, true)
        }
    }
}