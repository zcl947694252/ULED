package com.dadoutek.uled.othersview

import android.os.Bundle
import android.view.View
import com.blankj.utilcode.util.LogUtils
import com.dadoutek.uled.R
import com.dadoutek.uled.base.TelinkBaseActivity
import com.dadoutek.uled.model.Opcode
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
class ExtendActivity : TelinkBaseActivity(), View.OnClickListener {
    override fun onClick(v: View?) {
        var address = 1
        when(v?.id){
            R.id.tv_jbzl-> TelinkLightService.Instance().sendCommandNoResponse(Opcode.CONFIG_GW_TIMER_PERIOD_LABLE_TASK,address,
                    byteArrayOf(Opcode.CONFIG_EXTEND_ALL_JBZL))
            R.id.tv_jbsd-> TelinkLightService.Instance().sendCommandNoResponse(Opcode.CONFIG_GW_TIMER_PERIOD_LABLE_TASK,address,
                    byteArrayOf(Opcode.CONFIG_EXTEND_ALL_JBSD))
            R.id.tv_pwm-> TelinkLightService.Instance().sendCommandNoResponse(Opcode.CONFIG_GW_TIMER_PERIOD_LABLE_TASK,address,
                    byteArrayOf(Opcode.CONFIG_EXTEND_ALL_PWM))
        }
    }

    private fun initListener() {
        tv_jbzl.setOnClickListener(this)
        tv_jbsd.setOnClickListener(this)
        tv_pwm.setOnClickListener(this)
        rg_ly.setOnCheckedChangeListener { _, checkedId ->
            when(checkedId){
                R.id.rb_slow->{ LogUtils.v("zcl-----------设置模式速度slow-------") }

                R.id.rb_mid->{ LogUtils.v("zcl-----------设置模式速度mid-------")}

                R.id.rb_fast->{ LogUtils.v("zcl-----------设置模式速度fast-------")}
            }
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