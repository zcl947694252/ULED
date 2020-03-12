package com.dadoutek.uled.gateway

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.os.Parcelable
import android.text.TextUtils
import android.view.View
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.TextView
import android.widget.Toast
import butterknife.ButterKnife
import butterknife.Unbinder
import cn.qqtheme.framework.picker.DateTimePicker
import cn.qqtheme.framework.picker.TimePicker
import com.blankj.utilcode.util.LogUtils
import com.dadoutek.uled.R
import com.dadoutek.uled.base.TelinkBaseActivity
import com.dadoutek.uled.gateway.bean.DbGatewayBean
import com.dadoutek.uled.model.DbModel.DbScene
import com.dadoutek.uled.switches.SelectSceneListActivity
import kotlinx.android.synthetic.main.item_gata_way_event_timer.*
import org.json.JSONObject

/**
 * 设置网关时间与场景传递进配config界面
 */
class GatewayChoseTimesActivity : TelinkBaseActivity() {
    private var toolbarTv: TextView? = null
    private var unbinder: Unbinder? = null
    private var toolbarCancel: TextView? = null
    private var timerTitle: TextView? = null
    private var toolbarConfirm: TextView? = null
    private var timerScene: TextView? = null
    private var timerLy: RelativeLayout? = null
    private var wheelPickerLy: LinearLayout? = null
    private val requestCodes = 1000
    private var hourTime = 3
    private var minuteTime = 15
    private var scene: DbScene? = null
    private var gatewayTimeBean: DbGatewayBean? = null

    private val timePicker: View
        get() {
            val picker = TimePicker(this)
            picker.setBackgroundColor(this.resources.getColor(R.color.white))
            picker.setDividerConfig(null)
            picker.setTextColor(this.resources.getColor(R.color.blue_text))
            picker.setLabel("", "")
            picker.setTextSize(25)
            picker.setOffset(3)
            if (scene != null) {
                val split = scene!!.times.split("-".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                if (split.size == 2)
                    picker.setSelectedItem(Integer.getInteger(split[1])!!, Integer.getInteger(split[0])!!)
            } else
                picker.setSelectedItem(3, 15)
            picker.setOnWheelListener(object : DateTimePicker.OnWheelListener {
                override fun onYearWheeled(index: Int, year: String) {}
                override fun onMonthWheeled(index: Int, month: String) {}
                override fun onDayWheeled(index: Int, day: String) {}
                override fun onHourWheeled(index: Int, hour: String) {
                    hourTime = Integer.parseInt(hour)
                }

                override fun onMinuteWheeled(index: Int, minute: String) {
                    minuteTime = Integer.parseInt(minute)
                }
            })

            return picker.contentView
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_gate_way_chose_time)
        unbinder = ButterKnife.bind(this)
        initView()
        initData()
        initLisenter()
    }

    private fun initLisenter() {
        toolbarCancel!!.setOnClickListener { v -> finish() }
        toolbarConfirm!!.setOnClickListener { v ->
            if (scene == null)
                Toast.makeText(applicationContext, getString(R.string.please_select_scene), Toast.LENGTH_SHORT).show()
            else {
                val intent = Intent()
         /*       gatewayTimeBean!!.sceneId = scene!!.id
                gatewayTimeBean!!.sceneName = scene!!.name
                gatewayTimeBean!!.startHour = hourTime
                gatewayTimeBean!!.startMinute = minuteTime*/

                intent.putExtra("data", gatewayTimeBean)
                setResult(Activity.RESULT_OK, intent)
                finish()
            }
        }
        timer_scene_ly.setOnClickListener { //跳转进选择停留时间
            startActivity(Intent(this@GatewayChoseTimesActivity, GatewayChoseStandingTimeActivity::class.java))
        }
        timerLy!!.setOnClickListener { v -> startActivityForResult(Intent(this, SelectSceneListActivity::class.java), requestCodes) }
    }

    private fun initData() {
        toolbarTv!!.text = getString(R.string.chose_time)
        timerTitle!!.text = getString(R.string.scene_name)
        val intent = intent
        val data = intent.getParcelableExtra<Parcelable>("data")
        if (data != null && !TextUtils.isEmpty(data.toString())) {
            gatewayTimeBean = data as DbGatewayBean
//            timerScene!!.text = gatewayTimeBean!!.sceneName
//            gatewayTimeBean!!.new = false
//            scene = DBUtils.getSceneByID(gatewayTimeBean!!.sceneId!!)
        } else {
//            gatewayTimeBean = DbGatewayBean(hourTime, minuteTime, true)
//            gatewayTimeBean!!.index = IndexUtil.getNum()
        }
    }

    private fun initView() {
        toolbarCancel = findViewById(R.id.toolbar_t_cancel)
        toolbarTv = findViewById(R.id.toolbar_t_center)
        toolbarConfirm = findViewById(R.id.toolbar_t_confim)

        timerTitle = findViewById(R.id.item_gate_way_timer_time)
        timerScene = findViewById(R.id.item_gate_way_timer_scene)
        timerLy = findViewById(R.id.timer_scene_ly)
        wheelPickerLy = findViewById(R.id.wheel_time_container)

        wheelPickerLy!!.addView(timePicker)

        item_gate_way_timer_time.text = getString(R.string.standing_time)

    }

    private fun getKeyBean(keyId: Int, featureId: Int, name: String = "", hight8Mes: Int = 0, low8Mes: Int = 0): JSONObject {
        //return JSONObject(["keyId" = keyId, "featureId" = featureId, "reserveValue_A" = hight8Mes, "reserveValue_B" = low8Mes, "name" = name])
        var job = JSONObject()
        job.put("keyId", keyId)
        job.put("featureId", featureId)
        job.put("reserveValue_A", hight8Mes)
        job.put("reserveValue_B", low8Mes)
        job.put("name", name)
        return job
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK && requestCode == requestCodes) {
            val par = data!!.getParcelableExtra<Parcelable>("data")
            scene = par as DbScene
            LogUtils.v("zcl获取场景信息scene" + scene!!.toString())
            timerScene!!.text = scene!!.name
        }
    }
}
