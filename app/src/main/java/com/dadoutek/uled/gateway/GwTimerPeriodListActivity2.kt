package com.dadoutek.uled.gateway


/**
 * 创建者     ZCL
 * 创建时间   2020/4/8 18:25
 * 描述
 *
 * 更新者     $
 * 更新时间   $
 * 更新描述
 */
import android.annotation.TargetApi
import android.app.Activity
import android.content.Intent
import android.os.Build
import android.os.Parcelable
import android.support.v7.widget.LinearLayoutManager
import android.view.View
import androidx.annotation.RequiresApi
import com.blankj.utilcode.util.LogUtils
import com.blankj.utilcode.util.ToastUtils
import com.dadoutek.uled.R
import com.dadoutek.uled.base.BaseActivity
import com.dadoutek.uled.gateway.adapter.GwTpItemAdapter
import com.dadoutek.uled.gateway.bean.GwTagBean
import com.dadoutek.uled.gateway.bean.GwTasksBean
import com.dadoutek.uled.gateway.bean.GwTimePeriodsBean
import com.dadoutek.uled.gateway.util.Base64Utils
import com.dadoutek.uled.model.Constants
import com.dadoutek.uled.model.dbModel.DbScene
import com.dadoutek.uled.model.httpModel.GwModel
import com.dadoutek.uled.model.Opcode
import com.dadoutek.uled.network.GwGattBody
import com.dadoutek.uled.network.NetworkObserver
import com.dadoutek.uled.switches.SelectSceneListActivity
import com.dadoutek.uled.tellink.TelinkLightApplication
import com.dadoutek.uled.tellink.TelinkLightService
import com.telink.TelinkApplication
import com.telink.bluetooth.event.DeviceEvent
import com.telink.bluetooth.light.LightAdapter
import com.telink.util.Event
import com.telink.util.EventListener
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.template_bottom_add_no_line.*
import kotlinx.android.synthetic.main.template_recycleview.*
import kotlinx.android.synthetic.main.template_top_three.*
import kotlinx.coroutines.*
import java.io.ByteArrayOutputStream
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.collections.ArrayList


/**
 * 创建者     ZCL
 * 创建时间   2020/3/17 18:43
 * 描述 生成时间段列表 传回上一页经上一页 再返回上一页保存  传入task的bean
 *
 * 更新者     $
 * 更新时间   $
 * 更新描述
 */
class GwTimerPeriodListActivity2 : BaseActivity(), EventListener<String> {

    private var isAddNew: Boolean = false
    private var launch: Job? = null
    private var indexAll: Int = 0
    private var receviedCount: Int = 0
    private var timeListTp: MutableList<GwTimePeriodsBean>? = null
    private var gwTagBean: GwTagBean? = null
    private lateinit var mApp: TelinkLightApplication
    private var connectCount: Int = 0
    private var disposableTimer: Disposable? = null
    private var tasksBean: GwTasksBean? = null
    private var isHaveLastOne = false

    override fun setLayoutID(): Int {
        return R.layout.activity_timer_period_list
    }

    private var scene: DbScene? = null
    private var timesList: ArrayList<GwTimePeriodsBean> = ArrayList()
    private var timesNowList: ArrayList<GwTimePeriodsBean> = ArrayList()
    private var adapter = GwTpItemAdapter(R.layout.item_gw_time_scene2, timesNowList)
    private val requestCodes: Int = 1000
    private var selectPosition: Int = 1000

    @RequiresApi(Build.VERSION_CODES.O)
    override fun performed(event: Event<String>?) {
        if (event is DeviceEvent) {
            this.onDeviceEvent(event)
        }
    }


    @RequiresApi(Build.VERSION_CODES.O)
    private fun onDeviceEvent(event: DeviceEvent) {
        when (event.type) {
            DeviceEvent.STATUS_CHANGED -> {
                when (event.args.status) {
                    LightAdapter.STATUS_SET_GW_COMPLETED -> {//Dadou   Dadoutek2018
                        val deviceInfo = event.args
                        LogUtils.v("zcl-----------获取网关相关返回信息-------$deviceInfo")
                        when (deviceInfo.gwVoipState) {
                            Constants.GW_CONFIG_TIME_PERIVODE_LABEL_VOIP -> {
                                launch?.cancel()
                                if (!this.isFinishing)
                                    sendTime()
                                LogUtils.v("zcl-----------收到回调发送次数-------")
                            }
                            Constants.GW_CONFIG_TIME_PERIVODE_TASK_VOIP -> {
                                receviceSuceessTaskSmallTimes()
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onPause() {
        super.onPause()
        TelinkLightApplication.getApp().removeEventListeners()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun receviceSuceessTaskSmallTimes() {
        disposableTimer?.dispose()
        receviedCount++
        launch?.cancel()
        launch = GlobalScope.launch(Dispatchers.Main) {
            delay(timeListTp!!.size * 200L)
            LogUtils.v("zcl-----------发送次数结束时间-------" + System.currentTimeMillis())
            if (receviedCount >= timeListTp?.size ?: 0) {
                tasksBean?.timingPeriods = timesNowList
                intent.putExtra("data", tasksBean)
                setResult(Activity.RESULT_OK, intent)
                finish()
            } else {
                hideLoadingDialog()
                ToastUtils.showShort(getString(R.string.config_gate_way_tp_task_fail))
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun initListener() {
        toolbar_t_cancel.setOnClickListener { finish() }
        toolbar_t_confim.setOnClickListener {
            /*  if (!isHaveLastOne) {
                  ToastUtils.showShort(getString(R.string.please_setting_least_one))
                  return@setOnClickListener
              }*/
            timesNowList.forEach {
                if (it.sceneId == 0L) {
                    ToastUtils.showShort(getString(R.string.please_select_scene))
                    return@setOnClickListener
                }
            }
            if (timesNowList.size <= 0) {
                ToastUtils.showShort(getString(R.string.please_setting_least_one))
                return@setOnClickListener
            }
            sendLabelHeadParams()
        }
        adapter.setOnItemClickListener { _, _, position ->
            selectPosition = position
            isAddNew = false

            startActivityForResult(Intent(this@GwTimerPeriodListActivity2, SelectSceneListActivity::class.java), requestCodes)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {//获取场景返回值
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK && requestCode == requestCodes) {
            val par = data!!.getParcelableExtra<Parcelable>("data")
            scene = par as DbScene
            GlobalScope.launch(Dispatchers.Main) {
                delay(200)
                LogUtils.v("zcl获取场景信息scene" + scene.toString())
                if (scene != null) {
                    if (isAddNew && timesList.size > 0) {//如果是冲洗创建
                        val periodsBean = timesList[0]
                        timesNowList.add(periodsBean)
                        timesList.remove(periodsBean)

                        if (timesList.size <= 0 || timesNowList.size >= 20)
                            add_group_btn?.visibility = View.GONE
                        else
                            add_group_btn?.visibility = View.VISIBLE

                        val size = if (timesNowList.size > 0)
                            timesNowList.size - 1
                        else
                            0
                        timesNowList[size].sceneName = scene!!.name
                        timesNowList[size].sceneId = scene!!.id
                        timesNowList[size].standingTime = tasksBean?.stayTime ?: 0
                    } else {
                        timesNowList[selectPosition].sceneName = scene!!.name
                        timesNowList[selectPosition].sceneId = scene!!.id
                        timesNowList[selectPosition].standingTime = tasksBean?.stayTime ?: 0
                    }

                    adapter.notifyDataSetChanged()
                }
            }
        }
    }

    override fun initData() {
        gwTagBean = TelinkLightApplication.getApp().currentGwTagBean
        tasksBean = intent.getParcelableExtra("data")
        getTimerPeriods()//计算出的

        val tpList = if (tasksBean?.timingPeriods?.size ?: 0 > 20)
            tasksBean?.timingPeriods?.subList(0, 20)
        else
            tasksBean?.timingPeriods//真是数据
        tpList?.let {
            tpList.forEach { it1 ->
                it1.standingTime = tasksBean?.stayTime ?: 0
            }
            timesNowList.addAll(it)
            var num = 0 until timesNowList.size
            val reversed = num.reversed()
            for (i in reversed)//倒序输出
                if (i < timesList.size)
                    timesList.removeAt(i)

            if (/*timesList.size > 0 ||*/ timesNowList.size < 20) {
                add_group_btn?.visibility = View.VISIBLE
            } else {
                add_group_btn?.visibility = View.GONE
            }
        }

        adapter.notifyDataSetChanged()
    }

    @TargetApi(Build.VERSION_CODES.O)
    @RequiresApi(Build.VERSION_CODES.O)
    override fun initView() {
        this.mApp = this.application as TelinkLightApplication
        toolbar_t_center.text = getString(R.string.timer_period_set)
        template_recycleView.layoutManager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)
        template_recycleView.adapter = adapter
        // linAdd = View.inflate(this, R.layout.template_bottom_add_no_line, null)
        //     val tv = linAdd?.findViewById<TextView>(R.id.add_group_btn_tv)
        add_group_btn?.setOnClickListener {
            isAddNew = true
            GlobalScope.launch(Dispatchers.Main) {
                delay(200)
                startActivityForResult(Intent(this@GwTimerPeriodListActivity2, SelectSceneListActivity::class.java), requestCodes)
            }
        }
        add_group_btn_tv?.text = getString(R.string.add_times)
        //adapter.addFooterView(linAdd)
    }

    /**
     * 定时场景标签头下发,时间段时间下发
     */
    @RequiresApi(Build.VERSION_CODES.O)
    private fun sendTime() {
        LogUtils.v("zcl-----------发送次数开始时间-------" + System.currentTimeMillis())
        connectCount++
        receviedCount = 0

        val size = if (timeListTp != null) timeListTp!!.size else 1
        if (size * 300L < 7500) {
            setTimerDelay(7500L)
        } else
            setTimerDelay(size * 300L)
        showLoadingDialog(getString(R.string.please_wait))

        val listOf: MutableList<ByteArray> = arrayListOf()
        timeListTp = mutableListOf()

        for (t in timesNowList) //获取时间段的有效时间
            if (t.sceneId != 0L)
                timeListTp?.add(t)

        var os = ByteArrayOutputStream()

        timeListTp?.let {
            var sendK = 0
            for (i in 0 until it.size) {
                val tp = it[i]
                if (tasksBean != null) {
                    if (!TelinkLightApplication.getApp().isConnectGwBle) {
                        var labHeadPar = byteArrayOf(0x11, 0x11, 0x11, 0, 0, 0, 0,
                                Opcode.CONFIG_GW_TIMER_PERIOD_LABLE_TASK, 0x11, 0x02,
                                tasksBean!!.labelId.toByte(), tasksBean!!.index.toByte(),
                                tasksBean!!.stayTime.toByte(), (tasksBean?.startHour ?: 0).toByte(),
                                (tasksBean?.startMins ?: 0).toByte(), (tasksBean?.endHour ?: 0).toByte(),
                                (tasksBean?.endMins ?: 0).toByte(), tp.index.toByte(), tp.sceneId.toByte(), 0)
                        os.write(labHeadPar)
                        listOf.add(labHeadPar)

                        if (i == it.size - 1) {
//                            LogUtils.v("zcl-----------发送到服务器时间段小时间命令-------$os")
                            setTimerDelay(7500L)
                            val gattPar = os.toByteArray()

                            val s = Base64Utils.encodeToStrings(gattPar)
                            val gattBody = GwGattBody()

                            gattBody.data = s
                            gattBody.ser_id = Constants.GW_GATT_SAVE_TIMER_PERIODES_TASK_TIME
                            gattBody.macAddr = gwTagBean!!.macAddr
                            gattBody.tagName = gwTagBean?.tagName
                            sendToServer(gattBody)
                        }
                    } else {
                        sendK++
                        LogUtils.v("zcl-----------发送次数-------$sendK")

                        var params = byteArrayOf(tasksBean!!.labelId.toByte(), tasksBean!!.index.toByte(),
                                tasksBean!!.stayTime.toByte(), (tasksBean?.startHour ?: 0).toByte(),
                                (tasksBean?.startMins ?: 0).toByte(), (tasksBean?.endHour
                                ?: 0).toByte(), (tasksBean?.endMins ?: 0).toByte(), tp.index.toByte(), tp.sceneId.toByte())

                        TelinkLightService.Instance().sendCommandResponse(Opcode.CONFIG_GW_TIMER_PERIOD_LABLE_TASK,
                                tasksBean?.gwMeshAddr ?: 0, params, "1")
                        LogUtils.v("zcl-----------发送命令0Xf7-------")
                    }
                }
            }
        }

    }

    private fun setTimerDelay(delay: Long) {
        disposableTimer?.dispose()
        disposableTimer = Observable.timer(delay, TimeUnit.MILLISECONDS)
                .subscribe {
                    hideLoadingDialog()
                    runOnUiThread { ToastUtils.showLong(getString(R.string.config_gate_way_tp_task_fail)) }
                }
    }

    /**
     * 发送标签保存命令
     */
    @RequiresApi(Build.VERSION_CODES.O)
    private fun sendLabelHeadParams() {
        TelinkApplication.getInstance().removeEventListeners()
        this.mApp.addEventListener(DeviceEvent.STATUS_CHANGED, this)

        var meshAddress = gwTagBean?.meshAddr ?: 0
        //从第八位开始opcode, 设备meshAddr  参数11-12-13-14 15-16-17-18
        val calendar = Calendar.getInstance()
        val month = calendar.get(Calendar.MONTH) + 1
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        val opcodeHead = Opcode.CONFIG_GW_TIMER_PERIOD_LABLE_HEAD

        gwTagBean?.let {
            it.status = 0//修改数据后状态设置成关闭
            if (!TelinkLightApplication.getApp().isConnectGwBle) {
                //如果是网络离线通过服务器
                setHeadTimerDelay(6500L)
                var gattPar = byteArrayOf(0x11, 0x11, 0x11, 0, 0, 0, 0, opcodeHead, 0x11, 0x02,
                        it.tagId.toByte(), it.status.toByte(), it.week.toByte(), 0,
                        month.toByte(), day.toByte(), 0, 0, 0, 0)// status 开1 关0 tag的外部现实

                LogUtils.v("zcl-----------发送到服务器-------循环时间段标签头$gattPar")

                val s = Base64Utils.encodeToStrings(gattPar)
                val gattBody = GwGattBody()
                gattBody.data = s
                gattBody.ser_id = Constants.GW_GATT_CHOSE_TIME_PEROIDES_LABEL_HEAD
                gattBody.macAddr = gwTagBean?.macAddr
                gattBody.tagName = gwTagBean?.tagName
                sendToServer(gattBody)
            } else {
                setHeadTimerDelay(1500L)

                var labHeadPar = byteArrayOf(it.tagId.toByte(), it.status.toByte(),
                        it.week.toByte(), 0, month.toByte(), day.toByte(), 0, 0)
                TelinkLightService.Instance()?.sendCommandResponse(opcodeHead, meshAddress, labHeadPar, "1")
                LogUtils.v("zcl-----------发送0xf6-----时间段选择时间--列表")
            }
        }
    }

    private fun setHeadTimerDelay(delay: Long) {
        disposableTimer?.dispose()
        disposableTimer = Observable.timer(delay, TimeUnit.MILLISECONDS)
                .observeOn(Schedulers.io())
                .subscribeOn(AndroidSchedulers.mainThread()).subscribe {
                    GlobalScope.launch(Dispatchers.Main) {
                        ToastUtils.showLong(getString(R.string.send_gate_way_label_head_fail))
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

    @RequiresApi(Build.VERSION_CODES.O)
    override fun receviedGwCmd2000(serId: String) {
        when (serId.toInt()) {
            Constants.GW_GATT_CHOSE_TIME_PEROIDES_LABEL_HEAD -> {
                sendTime()
            }
            Constants.GW_GATT_SAVE_TIMER_PERIODES_TASK_TIME -> {
                hideLoadingDialog()
                tasksBean?.timingPeriods = timesNowList
                intent.putExtra("data", tasksBean)
                setResult(Activity.RESULT_OK, intent)
                finish()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        disposableTimer?.dispose()
    }

    private fun getTimerPeriods() {
        //  var minutes = tasksBean?.endAllMinuts ?: 0 - tasksBean?.startAllMinuts!!
        //        indexAll = minutes / (tasksBean?.stateTime ?: 0)
        if (tasksBean?.stayTime != 0) {
            timesList.clear()
            var index = 0
            val startTime = (tasksBean?.startHour ?: 0) * 60 + (tasksBean?.startMins ?: 0)
            val endTime = (tasksBean?.endHour ?: 0) * 60 + (tasksBean?.endMins ?: 0)
            val standingNum = tasksBean?.stayTime ?: 0

            for (time in startTime..endTime step standingNum) {
                val i = endTime - time //判断还剩时间用于判断是不是最后一个
                index += 1
                LogUtils.v("zcl----时间段-------$time-------$standingNum----$i")
                if (i in 1 until standingNum) {//如果不足停留时间取结束时间跳出循环
                    var bean = GwTimePeriodsBean(index, time, endTime, getString(R.string.choose_scene))
                    bean.standingTime = tasksBean?.stayTime ?: 0
                    timesList.add(bean)
                    break
                } else if (endTime - time >= standingNum) {
                    var bean = GwTimePeriodsBean(index, time, time + standingNum, getString(R.string.choose_scene))
                    bean.sceneName = getString(R.string.choose_scene)
                    timesList.add(bean)
                }
            }
            if (timesList.size > 20)
                timesList.subList(0, 20)
        }
    }
}