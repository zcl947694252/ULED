package com.dadoutek.uled.gateway

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.CheckBox
import android.widget.TextView
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
import com.dadoutek.uled.tellink.TelinkLightService
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
 * 描述
 *
 * 更新者     $
 * 更新时间   $
 * 更新描述
 */
class GwEventListActivity : TelinkBaseActivity(), View.OnClickListener {
    override fun onClick(v: View?) {
        addNewTag()
    }

    private var dbGw: DbGateway? = null
    private var addBtn: Button? = null
    private var lin: View? = null
    private var lastTime: Long = 0
    val list = mutableListOf<GwTagBean>()
    val adapter = GwEventItemAdapter(R.layout.event_item, list)

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
        dbGw = intent.getParcelableExtra<DbGateway>("data")
        if (dbGw == null) {
            ToastUtils.showShort(getString(R.string.no_get_device_info))
            finish()
        }
        swipe_recycleView.layoutManager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)
        swipe_recycleView.adapter = adapter
        swipe_recycleView.isItemViewSwipeEnabled = true //侧滑删除，默认关闭。

        if (!TextUtils.isEmpty(dbGw?.tags)) {
            list.clear()
            val tagList = if (dbGw?.type == 0)
                dbGw!!.tags
            else
                dbGw!!.timePeriodTags

            list.addAll(GsonUtil.stringToList(tagList, GwTagBean::class.java))
            adapter.notifyDataSetChanged()
        }
    }

    @SuppressLint("SetTextI18n")
    fun initListener() {
        toolbar.setNavigationOnClickListener { finish() }
        addBtn?.setOnClickListener(this)
        lin?.setOnClickListener(this)

        event_mode_gp.setOnCheckedChangeListener { _, checkedId ->
            list.clear()
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

            if (tags != null)
                list.addAll(GsonUtil.stringToList(tags, GwTagBean::class.java))

            addGw()

            adapter.notifyDataSetChanged()
        }
        swipe_recycleView.setOnItemMoveListener(object : OnItemMoveListener {
            override fun onItemDismiss(srcHolder: RecyclerView.ViewHolder?) {
                val position = srcHolder?.adapterPosition
                deleteTimerLable(list[position ?: 0], Date().time)
                list.removeAt(position ?: 0)
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

                    DBUtils.saveGateWay(dbGw!!, true)

                    sendParamToService(list[position])
                }
            }
        }
    }

    private fun sendParamToService(dbGwTag: GwTagBean) {
        val calendar = Calendar.getInstance()
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)
        val opcodeHead = if (dbGwTag?.isTimer())
            Opcode.CONFIG_GW_TIMER_LABLE_HEAD
        else
            Opcode.CONFIG_GW_TIMER_PERIOD_LABLE_HEAD
        var labHeadPar = byteArrayOf(0x11, 0x11, 0x11, 0, 0, 0, 0, opcodeHead, 0x11, 0x02,
                dbGwTag.tagId.toByte(), dbGwTag.status.toByte(),
                dbGwTag.week.toByte(), 0, month.toByte(), day.toByte(), 0, 0)
        val gattBody = GwGattBody()
        gattBody.cmd = opcodeHead.toInt()
        gattBody.data = labHeadPar.toString()
        gattBody.macAddr = "789ce70aa87e"

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
    private fun addGw() {
        dbGw!!.macAddr = "1111"
        GwModel.add(dbGw!!)?.subscribe(object : NetworkObserver<String?>() {
            override fun onNext(t: String) {
                LogUtils.v("zcl-----网关失添成功返回-------------$t")
            }

            override fun onError(e: Throwable) {
                super.onError(e)
                LogUtils.v("zcl-------网关失添加败-----------" + e.message)
            }
        })

        GwModel.getGwList()?.subscribe(object : NetworkObserver<List<DbGateway>?>() {
            override fun onNext(t: List<DbGateway>) {
                LogUtils.v("zcl-------网关列表-----------$t")
            }

            override fun onError(e: Throwable) {
                super.onError(e)
                LogUtils.v("zcl-------网关列表失败-----------" + e.message)
            }
        })

        val gattBody = GwGattBody()
        gattBody.deleteList = mutableListOf(1)
        GwModel.deleteGwList(gattBody)?.subscribe(object : NetworkObserver<String?>() {
            override fun onNext(t: String) {
                LogUtils.v("zcl-----网关删除成功返回-------------$t")
            }

            override fun onError(e: Throwable) {
                super.onError(e)
                LogUtils.v("zcl-----网关删除成功返回-------------${e.message}")
            }
        })

        var str = "acbdggsdf"
        val bytes = str.toByteArray()
        val lastNum = bytes.size % 8
        val list = mutableListOf<ByteArray>()
        for (index in 0 until bytes.size step 8) {
            var b: ByteArray
            if (index + 8 <= bytes.size) {
                b = ByteArray(8)
                System.arraycopy(bytes, index, b, 0, 8)
                list.add(b)
            } else {
                b = ByteArray(8)
                System.arraycopy(bytes, index, b, 0, lastNum)
                list.add(b)
            }
        }
        LogUtils.v("zcl----------发送命令list-------$list----------${list.size}")
        var num = 500L
        GlobalScope.launch(Dispatchers.Main) {
            for (i in 0 until list.size) {
                delay(num * i)
                //11-18 11位labelId
                val offset = i * 8
                val bytesArray = list[i]

                var params = byteArrayOf(bytes.size.toByte(), offset.toByte(), bytesArray[0],
                        bytesArray[1], bytesArray[2], bytesArray[3], bytesArray[4], bytesArray[5], bytesArray[6], bytesArray[7])

                TelinkLightService.Instance().sendCommandNoResponse(Opcode.CONFIG_GW_WIFI_SDID, 11, params)
                delay(200)
                TelinkLightService.Instance().sendCommandNoResponse(Opcode.CONFIG_GW_WIFI_PASSWORD, 11, params)
            }
        }
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
        }
    }

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