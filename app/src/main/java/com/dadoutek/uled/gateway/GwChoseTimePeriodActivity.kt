package com.dadoutek.uled.gateway

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.text.TextUtils
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupWindow
import cn.qqtheme.framework.picker.DateTimePicker
import cn.qqtheme.framework.picker.TimePicker
import com.blankj.utilcode.util.LogUtils
import com.blankj.utilcode.util.ToastUtils
import com.dadoutek.uled.R
import com.dadoutek.uled.base.TelinkBaseActivity
import com.dadoutek.uled.gateway.bean.GwTasksBean
import com.dadoutek.uled.gateway.bean.GwTimePeriodsBean
import com.dadoutek.uled.tellink.TelinkLightApplication
import kotlinx.android.synthetic.main.activity_gate_way_chose_time.*
import kotlinx.android.synthetic.main.template_top_three.*
import kotlinx.android.synthetic.main.template_wheel_container.*

/**
 * 设置网关 时间段时间与场景传递进配config界面
 */
class GwChoseTimePeriodActivity : TelinkBaseActivity(), View.OnClickListener {
    private var standingItemAdapter: StandingItemAdapter? = null
    private var popRecycle: androidx.recyclerview.widget.RecyclerView? = null
    private var newData: GwTasksBean? = null
    private var standingNum: Int = 0 //停留时间
    private var picker: TimePicker? = null
    private val requestStandingCode = 1001
    private val requestTimerPeriodCode = 1002
    private var startHourTime = 7
    private var startMinuteTime = 40
    private var endHourTime = 8
    private var endMinuteTime = 30
    private var startTimeNum: Int = startHourTime * 60 + startMinuteTime
    private var endTimeNum: Int = endHourTime * 60 + endMinuteTime
    private var tasksBean: GwTasksBean? = null
    private var data = ArrayList<GwTasksBean>()
    private var selectStart = true
    private var timesList = ArrayList<GwTimePeriodsBean>()

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
        makeStandingTimePop()
    }

    private fun makeStandingTimePop() {
        popView = LayoutInflater.from(this).inflate(R.layout.pop_standing_time_check, null)
        popView?.let {
            popRecycle = it.findViewById<View>(R.id.template_recycleView) as androidx.recyclerview.widget.RecyclerView?
        }

        val list = mutableListOf<Int>()
        for (i in 1..59)
            list.add(i)

        popRecycle?.layoutManager = LinearLayoutManager(this, androidx.recyclerview.widget.LinearLayoutManager.VERTICAL, false)
        val decorations = DividerItemDecoration(this, androidx.recyclerview.widget.DividerItemDecoration.VERTICAL)
        decorations.setDrawable(ColorDrawable(ContextCompat.getColor(this, R.color.black_nine)))
        popRecycle?.addItemDecoration(decorations)
        standingItemAdapter = StandingItemAdapter(R.layout.standing_item, list)
        popRecycle?.adapter = standingItemAdapter
        standingItemAdapter?.bindToRecyclerView(popRecycle)

        standingItemAdapter?.setOnItemClickListener { _, _, position ->
            standingNum = list[position]
            gw_times_standing_time.text = standingNum.toString()
            pop?.dismiss()
        }
        pop = PopupWindow(popView, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        pop?.let {
            it.isFocusable = true // 设置PopupWindow可获得焦点
            it.isTouchable = true // 设置PopupWindow可触摸补充：
            //it.isOutsideTouchable = false
        }
    }

    private fun initLisenter() {
        toolbar_t_cancel!!.setOnClickListener(this)
        toolbar_t_confim!!.setOnClickListener(this)
        toolbar_t_confim.setImageResource(R.drawable.go_to_link)
        gw_times_standing_time_ly!!.setOnClickListener(this)//选择停留时间
        startTime.setOnClickListener(this)
        endTime.setOnClickListener(this)
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.toolbar_t_confim -> {
                val repeatTime = isRepeatTime()
                if (repeatTime) {
                    ToastUtils.showShort(getString(R.string.have_time_task))
                    return
                }
                if (startTimeNum >= endTimeNum) {
                    ToastUtils.showShort(getString(R.string.please_chose_right_time))
                    return
                }

                if (standingNum <= 0) {
                    ToastUtils.showShort(getString(R.string.please_select_standing_time))
                    return
                }
                getTimerPeriods()//生成时间段

                tasksBean?.startHour = startHourTime
                tasksBean?.startMins = startMinuteTime
                tasksBean?.endHour = endHourTime
                tasksBean?.endMins = endMinuteTime
                tasksBean?.startAllMinuts = startTimeNum
                tasksBean?.endAllMinuts = endTimeNum
                //tasksBean?.timingPeriods =timesList
                tasksBean?.stayTime = standingNum
                //val intent = Intent(this@GwChoseTimePeriodActivity, GwTimerPeriodListActivity::class.java)
                val intent = Intent(this@GwChoseTimePeriodActivity, GwTimerPeriodListActivity2::class.java)
                intent.putExtra("data", tasksBean)
                startActivityForResult(intent, requestTimerPeriodCode)
            }
            R.id.toolbar_t_cancel -> finish()
            R.id.gw_times_standing_time_ly -> {
                if (endTimeNum > startTimeNum)
                // startActivityForResult(Intent(this, GwSelectStandingTimeActivity::class.java), requestStandingCode)
                    pop?.showAtLocation(window.decorView, Gravity.BOTTOM, 0, 0)
                else
                    ToastUtils.showShort(getString(R.string.please_chose_right_time))
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

    private fun isRepeatTime(): Boolean {
        var isRepeatTime = false
        for (task in data) {//开始时间不能再他的开始时间与结束时间之间 ,结束时间再他之间
                task.startAllMinuts = task.startHour * 60 + task.startMins
                task.endAllMinuts = task.endHour * 60 + task.endMins

            if ((startTimeNum >= task.startAllMinuts && startTimeNum < task.endAllMinuts)
                    || (endTimeNum >= task.startAllMinuts && endTimeNum <= task.endAllMinuts))
                isRepeatTime = true
        }
        return isRepeatTime
    }

    @SuppressLint("SetTextI18n")
    private fun initData() {

        newData = intent.getParcelableExtra<GwTasksBean>("newData")
        if (newData != null && !TextUtils.isEmpty(newData.toString())) {//新创建task
            tasksBean = newData
            tasksBean!!.isCreateNew = true
            data = TelinkLightApplication.getApp().listTask
        } else {
            ToastUtils.showShort(getString(R.string.invalid_data))
            finish()
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
            if (requestCode == requestTimerPeriodCode) {//获取时间段bean
                val bean = data!!.getParcelableExtra<GwTasksBean>("data")
                intent.putExtra("data", bean)
                setResult(Activity.RESULT_OK, intent)
                finish()
            } else if (requestCode == requestStandingCode) {//获取停留时间
                standingNum = data!!.getIntExtra("data", 0)
                gw_times_standing_time.text = standingNum.toString()
                tasksBean?.stayTime = standingNum
            }
        }
    }

    private fun getTimerPeriods() {
        if (standingNum != 0) {
            timesList.clear()
            var index = 0
            for (time in startTimeNum..endTimeNum step standingNum) {
                val i = endTimeNum - time
                index += 1
                LogUtils.v("zcl----时间段-------$time-------$standingNum----$i")
                if (i in 1 until standingNum) {//如果不足停留时间取结束时间跳出循环
                    var bean = GwTimePeriodsBean(index, time, endTimeNum, getString(R.string.choose_scene))
                    timesList.add(bean)
                    break
                } else if (endTimeNum - time >= standingNum) {
                    var bean = GwTimePeriodsBean(index, time, time + standingNum, getString(R.string.choose_scene))
                    bean.sceneName = getString(R.string.choose_scene)
                    timesList.add(bean)
                }
            }
        }
    }
}
