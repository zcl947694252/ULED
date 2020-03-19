package com.dadoutek.uled.gateway

import android.app.Activity
import android.content.Intent
import android.view.View
import cn.qqtheme.framework.picker.NumberPicker
import com.blankj.utilcode.util.LogUtils
import com.dadoutek.uled.R
import com.dadoutek.uled.base.BaseActivity
import kotlinx.android.synthetic.main.template_top_three.*
import kotlinx.android.synthetic.main.template_wheel_container.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch


/**
 * 创建者     ZCL
 * 创建时间   2020/3/17 16:27
 * 描述
 *
 * 更新者     $
 * 更新时间   $
 * 更新描述
 */
class GwSelectStandingTimeActivity : BaseActivity() {

    private val numPicker: View
        get() {
            var picker = NumberPicker(this)
            picker.let {
                it.setBackgroundColor(this.resources.getColor(R.color.white))
                it.setDividerConfig(null)
                it.setTextColor(this.resources.getColor(R.color.blue_text))
                it.setRange(1, 59)
                it.setTextSize(25)
                it.setOffset(3)
                it.setOnWheelListener(NumberPicker.OnWheelListener { index, item ->
                    LogUtils.v("zcl-----------$index-------$item")
                    standingTime = item.toInt()
                })
            }
            return picker!!.contentView
        }
    private var standingTime = 0

    override fun initListener() {
    }

    override fun initData() {

    }

    override fun initView() {
        wheel_time_container.addView(numPicker)
        toolbar_t_center.text = getString(R.string.standing_time)
        toolbar_t_cancel.setOnClickListener { finish() }
        toolbar_t_confim.setOnClickListener {
            GlobalScope.launch {
                if (standingTime == 0)
                    kotlinx.coroutines.delay(1000)
                val intent = Intent().putExtra("data", standingTime)
                setResult(Activity.RESULT_OK, intent)
                finish()
            }


        }
    }

    override fun setLayoutID(): Int {
        return R.layout.activity_select_standing_time
    }
}