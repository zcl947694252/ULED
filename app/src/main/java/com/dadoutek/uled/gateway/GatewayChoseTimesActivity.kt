package com.dadoutek.uled.gateway

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.os.Parcelable
import android.text.TextUtils
import android.view.View
import android.widget.Toast
import cn.qqtheme.framework.picker.DateTimePicker
import cn.qqtheme.framework.picker.TimePicker
import com.blankj.utilcode.util.GsonUtils
import com.blankj.utilcode.util.LogUtils
import com.blankj.utilcode.util.ToastUtils
import com.dadoutek.uled.R
import com.dadoutek.uled.base.TelinkBaseActivity
import com.dadoutek.uled.gateway.bean.DbGateway
import com.dadoutek.uled.gateway.bean.GatewayTasksBean
import com.dadoutek.uled.model.DbModel.DBUtils
import com.dadoutek.uled.model.DbModel.DbScene
import com.dadoutek.uled.switches.SelectSceneListActivity
import com.dadoutek.uled.util.TmtUtils
import kotlinx.android.synthetic.main.activity_gate_way_chose_time.*
import kotlinx.android.synthetic.main.item_gata_way_event_timer.*
import kotlinx.android.synthetic.main.template_top_three.*
import kotlinx.android.synthetic.main.template_wheel_container.*
import java.util.*

/**
 * 设置网关时间与场景传递进配config界面
 */
class GatewayChoseTimesActivity : TelinkBaseActivity(), View.OnClickListener, View.OnFocusChangeListener {

    private var startTimeNum: Int = 0
    private var endTimeNum: Int = 0
    private val requestCodes = 1000
    private var startHourTime = 3
    private var startMinuteTime = 15
    private var endHourTime = 3
    private var endMinuteTime = 15
    private var scene: DbScene? = null
    private var tasksBean: GatewayTasksBean? = null
    internal var data: ArrayList<Parcelable>? = null
    private var selectStart = true
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
            val picker = TimePicker(this)

            picker.setBackgroundColor(this.resources.getColor(R.color.white))
            picker.setDividerConfig(null)
            picker.setTextColor(this.resources.getColor(R.color.blue_text))
            picker.setLabel("", "")
            picker.setTextSize(25)
            picker.setOffset(3)
            picker.setSelectedItem(startHourTime, startMinuteTime)
            picker.setOnWheelListener(object : DateTimePicker.OnWheelListener {
                override fun onYearWheeled(index: Int, year: String) {}

                override fun onMonthWheeled(index: Int, month: String) {}

                override fun onDayWheeled(index: Int, day: String) {}

                override fun onHourWheeled(index: Int, hour: String) {
                    startHourTime = Integer.parseInt(hour)
                    setTime()
                }

                override fun onMinuteWheeled(index: Int, minute: String) {
                    startMinuteTime = Integer.parseInt(minute)
                    setTime()
                }
            })

            return picker.contentView
        }

    private fun setTime() {
        if (selectStart) {
            setStartTime()
        } else {
            endTime.setText("$startHourTime:$startMinuteTime")
        }
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
        item_gw_timer_time!!.text = getString(R.string.scene_name)
    }

    private fun initLisenter() {
        toolbar_t_cancel!!.setOnClickListener(this)
        toolbar_t_confim!!.setOnClickListener(this)
        timer_scene_ly!!.setOnClickListener(this)
        startTime.onFocusChangeListener = this
        endTime.onFocusChangeListener = this
    }

    override fun onFocusChange(v: View?, hasFocus: Boolean) {
        selectStart = v?.id == R.id.startTime
        when (v?.id) {
            R.id.startTime -> {
                setStartTime()
                startTime.setSelection(startTime.text.length)
                startTime.setTextColor(getColor(R.color.blue_text))
                endTime.setTextColor(getColor(R.color.gray_6))
            }
            R.id.endTime -> {
                setEndTime()
                endTime.setSelection(startTime.text.length)
                endTime.setTextColor(getColor(R.color.blue_text))
                startTime.setTextColor(getColor(R.color.gray_6))
            }
        }
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.toolbar_t_confim -> {
                if (scene == null)
                    Toast.makeText(applicationContext, getString(R.string.please_select_scene), Toast.LENGTH_SHORT).show()
                else {
                    if (isTimeHave!!) {//如果已有该时间
                        if (tasksBean!!.startHour == startHourTime && tasksBean!!.startMins == startMinuteTime) {//并且是当前的task的时间
                            val intent = Intent()
                            tasksBean!!.setSceneId(scene!!.id)
                            tasksBean!!.senceName = scene!!.name
                            tasksBean!!.startHour = startHourTime
                            tasksBean!!.startMins = startMinuteTime
                            intent.putExtra("data", tasksBean)
                            setResult(Activity.RESULT_OK, intent)
                            finish()
                        } else {
                            TmtUtils.midToastLong(this, getString(R.string.have_time_task))
                        }
                    } else {//没有此时间直接添加
                        val intent = Intent()
                        tasksBean!!.setSceneId(scene!!.id)
                        tasksBean!!.senceName = scene!!.name
                        tasksBean!!.startHour = startHourTime
                        tasksBean!!.startMins = startMinuteTime

                        intent.putExtra("data", tasksBean)
                        setResult(Activity.RESULT_OK, intent)
                        finish()
                    }
                }
            }
            R.id.toolbar_t_cancel -> finish()
            R.id.timer_scene_ly -> startActivityForResult(Intent(this, SelectSceneListActivity::class.java), requestCodes)
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

    private fun setEndTime() {
        endTimeNum = endHourTime * 60 + endMinuteTime
        endTime.setText("$endHourTime:$endMinuteTime")
    }

    private fun setStartTime() {
        startTimeNum = startHourTime * 60 + startMinuteTime
        startTime.setText("$startHourTime:$startMinuteTime")
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {//获取场景返回值
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK && requestCode == requestCodes) {
            val par = data!!.getParcelableExtra<Parcelable>("data")
            scene = par as DbScene
            LogUtils.v("zcl获取场景信息scene" + scene!!.toString())
            item_gw_timer_scene!!.text = scene!!.name
            GsonUtils.fromJson("", DbGateway::class.java)
        }
    }

}
