package com.dadoutek.uled.router

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Parcelable
import android.view.View
import androidx.annotation.RequiresApi
import cn.qqtheme.framework.picker.DateTimePicker
import cn.qqtheme.framework.picker.TimePicker
import com.blankj.utilcode.util.LogUtils
import com.blankj.utilcode.util.ToastUtils
import com.dadoutek.uled.R
import com.dadoutek.uled.base.TelinkBaseActivity
import com.dadoutek.uled.gateway.GwChoseModeActivity
import com.dadoutek.uled.gateway.bean.WeekBean
import com.dadoutek.uled.model.Constant
import com.dadoutek.uled.model.dbModel.DBUtils
import com.dadoutek.uled.model.dbModel.DbScene
import com.dadoutek.uled.model.routerModel.RouterModel
import com.dadoutek.uled.network.RouterTimerSceneBean
import com.dadoutek.uled.router.bean.CmdBodyBean
import com.dadoutek.uled.switches.SelectSceneListActivity
import com.dadoutek.uled.util.SyncDataPutOrGetUtils
import com.jakewharton.rxbinding2.view.RxView
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.item_gw_time_scene.*
import kotlinx.android.synthetic.main.template_repeat_ly.*
import kotlinx.android.synthetic.main.template_top_three.*
import kotlinx.android.synthetic.main.template_wheel_container.*
import org.jetbrains.anko.textColor
import java.util.*
import java.util.concurrent.TimeUnit


/**
 * 创建者     ZCL
 * 创建时间   2020/10/28 10:07
 * 描述
 *
 * 更新者     $
 * 更新时间   $
 * 更新描述
 */
class AddTimerSceneActivity : TelinkBaseActivity() {
    private var isReConfig: Boolean = false
    private var timerSceneBean: RouterTimerSceneBean? = null
    private var mode: String? = null
    private val requestModeCode: Int = 1001
    private val requestCodes = 1000
    private var hourTime = 3
    private var minuteTime = 15
    private var scene: DbScene? = null

    @RequiresApi(api = Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_timer_scene)
        initView()
        initData()
        initLisenter()
    }

    @SuppressLint("CheckResult")
    @RequiresApi(api = Build.VERSION_CODES.O)
    private fun initLisenter() {
        toolbar_t_cancel.setOnClickListener { finish() }
        RxView.clicks(toolbar_t_confim).throttleFirst(500, TimeUnit.MILLISECONDS).subscribe {
            when {
                scene == null -> ToastUtils.showShort(getString(R.string.please_select_scene))
                mode == "" || mode == null -> ToastUtils.showShort(getString(R.string.select_mode))
                else -> if (!isReConfig) routerAddSceneTimer() else routerUpdateSceneTimer()
            }
        }
        timer_scene_ly!!.setOnClickListener {
            startActivityForResult(Intent(this, SelectSceneListActivity::class.java), requestCodes)
        }
        gate_way_repete_mode_ly?.setOnClickListener {
            val intent = Intent(this@AddTimerSceneActivity, GwChoseModeActivity::class.java)
            intent.putExtra("data", getWeek(mode!!))
            startActivityForResult(intent, requestModeCode)
        }
    }

    private fun initData() {
        item_gw_timer_title!!.text = getString(R.string.scene_name) //底部item 的title
        //打开时设置为当前时间
        hourTime = Calendar.getInstance()[Calendar.HOUR_OF_DAY]
        minuteTime = Calendar.getInstance()[Calendar.MINUTE]

        val serializable = intent.getSerializableExtra("timerScene")
        isReConfig = serializable != null && serializable != ""
        if (isReConfig) {
            timerSceneBean = serializable as RouterTimerSceneBean
            hourTime = timerSceneBean?.hour ?: hourTime
            minuteTime = timerSceneBean?.min ?: minuteTime
            mode = getWeekStr(timerSceneBean?.week)
            scene = DBUtils.getSceneByID((timerSceneBean?.sid ?: 0).toLong())
            item_gw_timer_scene.text = scene?.name
        } else {
            mode = getString(R.string.only_one)
        }

        gate_way_repete_mode.text = mode
        toolbar_t_center.text = if (!isReConfig)
            getString(R.string.add_timer_scene)
        else
            getString(R.string.update_timer_scene)

        toolbar_t_center.textColor = getColor(R.color.gray_3)
        wheel_time_container.addView(timePicker)
    }

    private fun getWeekStr(week: Int?): String {
        var tmpWeek = week ?: 0
        val sb = StringBuilder()
        when (tmpWeek) {
            0b01111111,0b10000000-> sb.append(getString(R.string.every_day))
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
                val filter = list.filter { it.selected }
                for (i in 0 until list.size) {
                    if (list[i].selected)
                        sb.append(list[i].week).append(",")
                }
                val toString = sb.toString()
                if (filter.size>1) {
                    toString.substring(sb.length-2)
                }
            }
        }
        return sb.toString()
    }

    private fun initView() {
        gate_way_repete_mode.text = ""
    }

    private val timePicker: View
        private get() {
            val picker = TimePicker(this)
            picker.setBackgroundColor(this.resources.getColor(R.color.white))
            picker.setDividerConfig(null)
            picker.setTextColor(this.resources.getColor(R.color.blue_text))
            picker.setLabel("", "")
            picker.setTextSize(25)
            picker.setOffset(3)
            picker.setSelectedItem(hourTime, minuteTime)
            picker.setOnWheelListener(object : DateTimePicker.OnWheelListener {
                override fun onYearWheeled(index: Int, year: String) {}
                override fun onMonthWheeled(index: Int, month: String) {}
                override fun onDayWheeled(index: Int, day: String) {}
                override fun onHourWheeled(index: Int, hour: String) {
                    hourTime = hour.toInt()
                }

                override fun onMinuteWheeled(index: Int, minute: String) {
                    minuteTime = minute.toInt()
                }
            })
            return picker.contentView
        }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)//获取场景返回值
        if (resultCode == Activity.RESULT_OK) {
            when (requestCode) {
                requestCodes -> {
                    val par = data!!.getParcelableExtra<Parcelable>("data")
                    scene = par as DbScene
                    LogUtils.v("zcl获取场景信息scene" + scene.toString())
                    item_gw_timer_scene!!.text = scene!!.name
                }
                requestModeCode -> {//选择重复日期
                    mode = data?.getStringExtra("data")
                    if (mode!!.contains("6")) {
                        gate_way_repete_mode.textSize = 13F
                        gate_way_repete_mode.text = mode?.replace("6", "")
                    } else {
                        gate_way_repete_mode.textSize = 15F  //mode = isOnlyOne(mode) 如果不选择就是仅一次 但凡选中任意一个星期日都是现实星期几
                        gate_way_repete_mode.text = mode
                    }
                }
            }
        }
    }

    @SuppressLint("CheckResult")
    private fun routerAddSceneTimer() {
        RouterModel.routeAddTimerScene("$hourTime:$minuteTime", hourTime, minuteTime, getWeek(mode!!), scene!!.id.toInt(), 1,"addTimerScene")?.subscribe({
            when (it.errorCode) {
                0 -> {
                    showLoadingDialog(getString(R.string.please_wait))
                    disposableRouteTimer?.dispose()
                    disposableRouteTimer = Observable.timer(it.t.timeout.toLong() + 2L, TimeUnit.SECONDS)
                            .subscribeOn(Schedulers.io())
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe {
                                hideLoadingDialog()
                                ToastUtils.showShort(getString(R.string.add_timer_scene_fail))
                            }
                }
                90011 -> ToastUtils.showShort(getString(R.string.scene_cont_exit_to_refresh))
                90005 -> ToastUtils.showShort(getString(R.string.router_offline))
                90004 -> ToastUtils.showShort(getString(R.string.region_no_router))
            }
        }, {
            ToastUtils.showShort(it.message)
        })
    }

    override fun tzRouterAddTimerScene(cmdBean: CmdBodyBean) {
        LogUtils.v("zcl-----------收到路由addTimerScene通知-------$cmdBean")
        if (cmdBean.ser_id == "addTimerScene") {
            disposableRouteTimer?.dispose()
            hideLoadingDialog()
            when (cmdBean.status) {
                0 -> {
                    ToastUtils.showShort(getString(R.string.add_timer_scene_success))
                    finish()
                }
                else -> ToastUtils.showShort(getString(R.string.add_timer_scene_fail))
            }
        }
    }


    private fun routerUpdateSceneTimer() {
        timerSceneBean?.let {
            RouterModel.routeUpdateTimerScene(timerSceneBean?.id
                    ?: 0, hourTime, minuteTime, getWeek(mode!!.replace("6","")), scene!!.id.toInt(), "updateTimerScene")
                    ?.subscribe({
                        LogUtils.v("zcl-----------路由请求刷新定时场景$-------$it")
                        when (it.errorCode) {
                            0 -> {
                                showLoadingDialog(getString(R.string.please_wait))
                                disposableRouteTimer?.dispose()
                                disposableRouteTimer = Observable.timer(it.t.timeout.toLong(), TimeUnit.SECONDS)
                                        .subscribe {
                                            hideLoadingDialog()
                                            ToastUtils.showShort(getString(R.string.config_fail))
                                        }
                            }
                            90030 -> ToastUtils.showShort(getString(R.string.scene_cont_exit_to_refresh))
                            90020 -> ToastUtils.showShort(getString(R.string.gradient_not_exit))
                            90018 -> {
                                DBUtils.deleteLocalData()
                                //ToastUtils.showShort(getString(R.string.device_not_exit))
                                SyncDataPutOrGetUtils.syncGetDataStart(DBUtils.lastUser!!, syncCallbackGet)
                                finish()
                            }
                            90011 -> ToastUtils.showShort(getString(R.string.scene_cont_exit_to_refresh))
                            90007 -> ToastUtils.showShort(getString(R.string.gp_not_exit))
                            90005 -> ToastUtils.showShort(getString(R.string.router_offline))
                            90004 -> ToastUtils.showShort(getString(R.string.region_no_router))
                        }
                    }, {
                        ToastUtils.showShort(it.message)
                    })
        }
    }

    override fun tzRouterUpdateTimerScene(cmdBean: CmdBodyBean) {
        LogUtils.v("zcl-----------收到路由updateTimerScene通知-------$cmdBean")
        if (cmdBean.ser_id == "updateTimerScene") {
            disposableRouteTimer?.dispose()
            hideLoadingDialog()
            if (cmdBean.status == 0) {
                ToastUtils.showShort(getString(R.string.config_success))
                finish()
            } else {
                ToastUtils.showShort(getString(R.string.config_fail))
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        disposableRouteTimer?.dispose()
    }
}