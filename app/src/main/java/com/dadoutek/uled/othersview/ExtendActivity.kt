package com.dadoutek.uled.othersview

import android.os.Bundle
import android.view.View
import android.widget.CompoundButton
import com.blankj.utilcode.util.LogUtils
import com.dadoutek.uled.R
import com.dadoutek.uled.base.TelinkBaseActivity
import com.dadoutek.uled.model.Opcode
import com.dadoutek.uled.model.Opcode.CONFIG_EXTEND_ALL_JBSD
import com.dadoutek.uled.tellink.TelinkLightService
import kotlinx.android.synthetic.main.activity_extend.*


/**
 * 创建者     ZCL
 * 创建时间   2020/4/14 14:46
 * 描述
 *
 * 更新者     $
 * 更新时间   $
 * 更新描述
 */
class ExtendActivity : TelinkBaseActivity(), View.OnClickListener, CompoundButton.OnCheckedChangeListener {
    private var speedNum = 1
    private var isOpenTag = 0
    private var iscloseTag = 0
    private var ispowerOnTag = 0
    private var issceneTag = 0

    override fun onCheckedChanged(buttonView: CompoundButton?, isChecked: Boolean) {
        when (buttonView?.id) {
            R.id.cb_light_open_switch -> {
                isOpenTag = if (isChecked) 1 else 0
            }
            R.id.cb_light_close_switch -> {
                iscloseTag = if (isChecked) 1 else 0
            }
            R.id.cb_light_power_on_switch -> {
                ispowerOnTag = if (isChecked) 1 else 0
            }
            R.id.cb_light_sence_switch -> {
                issceneTag = if (isChecked) 1 else 0
            }
        }
    }

    override fun onClick(v: View?) {
        var address = 0
        when (v?.id) {
            R.id.tv_jbzl -> TelinkLightService.Instance().sendCommandNoResponse(Opcode.CONFIG_EXTEND_OPCODE, address,
                    byteArrayOf(Opcode.CONFIG_EXTEND_ALL_JBZL,isOpenTag.toByte(),iscloseTag.toByte(),ispowerOnTag.toByte(),issceneTag.toByte()))
            R.id.tv_jbsd -> TelinkLightService.Instance().sendCommandNoResponse(Opcode.CONFIG_EXTEND_OPCODE, address,
                    byteArrayOf(Opcode.CONFIG_EXTEND_ALL_JBSD))
            R.id.tv_pwm -> TelinkLightService.Instance().sendCommandNoResponse(Opcode.CONFIG_EXTEND_OPCODE, address,
                    byteArrayOf(Opcode.CONFIG_EXTEND_ALL_PWM))
        }
    }

    private fun initListener() {
        tv_pwm.setOnClickListener(this)
        tv_jbzl.setOnClickListener(this)
        tv_jbsd.setOnClickListener(this)
        cb_light_open_switch.setOnCheckedChangeListener(this)
        cb_light_close_switch.setOnCheckedChangeListener(this)
        cb_light_sence_switch.setOnCheckedChangeListener(this)
        cb_light_power_on_switch.setOnCheckedChangeListener(this)

        rg_ly.setOnCheckedChangeListener { _, checkedId ->
            when (checkedId) {
                R.id.rb_slow -> {
                    speedNum = 1
                    LogUtils.v("zcl-----------设置模式速度slow-------")
                }

                R.id.rb_mid -> {
                    speedNum = 5
                    LogUtils.v("zcl-----------设置模式速度mid-------")
                }

                R.id.rb_fast -> {
                    speedNum = 8
                    LogUtils.v("zcl-----------设置模式速度fast-------")
                }
            }
            TelinkLightService.Instance()?.sendCommandNoResponse(Opcode.CONFIG_EXTEND_OPCODE, 0, byteArrayOf(CONFIG_EXTEND_ALL_JBSD, speedNum.toByte()))
        }
    }

    private fun initData() {

    }

    private fun initView() {

    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_extend)
        initView()
        initData()
        initListener()
    }
}