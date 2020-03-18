package com.dadoutek.uled.gateway

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.os.Parcelable
import android.text.TextUtils
import android.view.View
import cn.qqtheme.framework.picker.DateTimePicker
import cn.qqtheme.framework.picker.TimePicker
import com.blankj.utilcode.util.LogUtils
import com.blankj.utilcode.util.ToastUtils
import com.dadoutek.uled.R
import com.dadoutek.uled.base.TelinkBaseActivity
import com.dadoutek.uled.gateway.bean.GatewayTasksBean
import com.dadoutek.uled.gateway.bean.GwTimePeriodsBean
import com.dadoutek.uled.model.DbModel.DBUtils
import com.dadoutek.uled.model.DbModel.DbScene
import kotlinx.android.synthetic.main.activity_gate_way_chose_time.*
import kotlinx.android.synthetic.main.item_gw_time_scene.*
import kotlinx.android.synthetic.main.template_top_three.*
import kotlinx.android.synthetic.main.template_wheel_container.*

/**
 * 设置网关时间与场景传递进配config界面
 */
class GatewayChoseTimesActivity : TelinkBaseActivity(), View.OnClickListener {

    private var standingNum: Int = 1 //停留时间
    private var picker: TimePicker? = null
    private var startTimeNum: Int = 0
    private var endTimeNum: Int = 0
    private val requestCodes = 1000
    private val requestStandingCodes = 1001
    private val requestTimerPeriodCodes = 1002
    private var startHourTime = 7
    private var startMinuteTime = 40
    private var endHourTime = 8
    private var endMinuteTime = 30
    private var scene: DbScene? = null
    private var tasksBean: GatewayTasksBean? = null
    internal var data: ArrayList<Parcelable>? = null
    private var selectStart = true
    private var timesList = ArrayList<GwTimePeriodsBean>()

    private val isTimeHave: Boolean?
        get() {
            var isHave: Boolean? = false
            for (i in data!!.indices) {
                val tag = data!![i] as GatewayTasksBean
                isHave = tag.startHour == startHourTime && tag.startMins == startMinuteTime
            }
            return isHave
        }

    private val timePicker: View
        get() {
            picker = TimePicker(this)
            picker?.let {
                it.setBackgroundColor(this.resources.getColor(R.color.white))
                it.setDividerConfig(null)
                it.setTextColor(this.resources.getColor(R.color.blue_text))
                it.setLabel("", "")
                it.setTextSize(25)
                it.setOffset(3)
                if (selectStart)
                    it.setSelectedItem(startHourTime, startMinuteTime)
                else
                    it.setSelectedItem(endHourTime, endMinuteTime)

                it.setOnWheelListener(object : DateTimePicker.OnWheelListener {
                    override fun onYearWheeled(index: Int, year: String) {}

                    override fun onMonthWheeled(index: Int, month: String) {}

                    override fun onDayWheeled(index: Int, day: String) {}

                    override fun onHourWheeled(index: Int, hour: String) {
                        if (selectStart) {
                            startHourTime = Integer.parseInt(hour)
                            setStartTime()
                        } else {
                            endHourTime = Integer.parseInt(hour)
                            setEndTime()
                        }
                    }

                    override fun onMinuteWheeled(index: Int, minute: String) {
                        if (selectStart) {
                            startMinuteTime = Integer.parseInt(minute)
                            setStartTime()
                        } else {
                            endMinuteTime = Integer.parseInt(minute)
                            setEndTime()
                        }
                    }
                })
            }
            return picker!!.contentView
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_gate_way_chose_time)
        initView()
        initData()
        initLisenter()
    }

    private fun initView() {
        toolbar_t_center.text = getString(R.string.chose_time)
        toolbar_t_confim.text = getString(R.string.next)
    }

    private fun initLisenter() {
        toolbar_t_cancel!!.setOnClickListener(this)
        toolbar_t_confim!!.setOnClickListener(this)
        gw_times_standing_time_ly!!.setOnClickListener(this)//选择停留时间
        startTime.setOnClickListener(this)
        endTime.setOnClickListener(this)
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.toolbar_t_confim -> {
                if (startTimeNum>=endTimeNum)
                    ToastUtils.showShort(getString(R.string.please_chose_right_time))
                else {
                    val intent = Intent(this@GatewayChoseTimesActivity, TimerPeriodListActivity::class.java)
                    intent.putParcelableArrayListExtra("data",timesList)
                    startActivityForResult(intent,requestTimerPeriodCodes)
                }
            }
            R.id.toolbar_t_cancel -> finish()
            R.id.gw_times_standing_time_ly -> {

                if (endTimeNum > startTimeNum)
                    startActivityForResult(Intent(this, GwSelectStandingTimeActivity::class.java), requestStandingCodes)
                else
                    ToastUtils.showShort(getString(R.string.please_chose_right_time))
                //startActivityForResult(Intent(this, SelectSceneListActivity::class.java), requestCodes)
            }

            R.id.startTime -> {
                selectStart = true
                setStartTime()
                wheel_time_container.removeAllViews()
                wheel_time_container!!.addView(timePicker)

                startTime.setTextColor(getColor(R.color.blue_text))
                endTime.setTextColor(getColor(R.color.gray_6))
                endTime_line.visibility = View.INVISIBLE
                startTime_line.visibility = View.VISIBLE
            }
            R.id.endTime -> {
                selectStart = false
                setEndTime()
                wheel_time_container.removeAllViews()
                wheel_time_container!!.addView(timePicker)
                endTime.setTextColor(getColor(R.color.blue_text))
                startTime.setTextColor(getColor(R.color.gray_6))
                endTime_line.visibility = View.VISIBLE
                startTime_line.visibility = View.INVISIBLE
            }
        }
    }

    @SuppressLint("SetTextI18n")
    private fun initData() {
        data = intent.getParcelableArrayListExtra("data")
        val pos = intent.getIntExtra("pos", 9999)
        val index = intent.getIntExtra("index", 100)

        if (data != null && !TextUtils.isEmpty(data!!.toString()) && pos != 9999) {//编辑老的task
            tasksBean = data!![pos] as GatewayTasksBean
            startHourTime = tasksBean!!.startHour
            startMinuteTime = tasksBean!!.startMins
            setStartTime()

            endHourTime = tasksBean!!.endHour
            endMinuteTime = tasksBean!!.endMins
            setEndTime()

            item_gw_timer_scene!!.text = tasksBean!!.senceName
            tasksBean!!.isCreateNew = false
            scene = DBUtils.getSceneByID(tasksBean!!.sceneId)
        } else {//新创建task
            if (index != 100) {
                tasksBean = GatewayTasksBean(index)
                tasksBean!!.isCreateNew = true
            } else {
                ToastUtils.showShort(getString(R.string.invalid_data))
            }
        }
        wheel_time_container!!.addView(timePicker)
    }

    @SuppressLint("SetTextI18n")
    private fun setEndTime() {
        endTimeNum = endHourTime * 60 + endMinuteTime
        var endHourTimeStr = timeStr(endHourTime)
        var endMinuteTimeStr = timeStr(endMinuteTime)
        endTime.text = "$endHourTimeStr:$endMinuteTimeStr"
    }

    @SuppressLint("SetTextI18n")
    private fun setStartTime() {
        startTimeNum = startHourTime * 60 + startMinuteTime
        var startHourTimeStr: String = timeStr(startHourTime)
        var startMinuteTimeStr: String = timeStr(startMinuteTime)
        startTime.text = "$startHourTimeStr:$startMinuteTimeStr"
    }

    private fun timeStr(time: Int): String {
        return if (time < 10)
            "0$time"
        else
            time.toString()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {//获取场景返回值
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == requestCodes) {
                val par = data!!.getParcelableExtra<Parcelable>("data")
                scene = par as DbScene
                LogUtils.v("zcl获取场景信息scene" + scene!!.toString())
                item_gw_timer_scene!!.text = scene!!.name
            } else if (requestCode == requestStandingCodes) {
                standingNum = data!!.getIntExtra("data", 0)
                gw_times_standing_time.text = standingNum.toString()
                   if (standingNum != 0) {
                       timesList.clear()
                       for (time in startTimeNum..endTimeNum step standingNum) {
                           LogUtils.v("zcl----时间段-------$time-------$standingNum")
                           timesList.add(GwTimePeriodsBean(time / 60, time % 60,getString(R.string.select_scene)))
                       }

                   }
            }
        }
    }
}
