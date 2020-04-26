package com.dadoutek.uled.switches

import android.app.Activity
import android.content.Intent
import android.graphics.Paint
import android.os.Bundle
import android.view.View
import com.dadoutek.uled.R
import com.dadoutek.uled.base.BaseActivity
import com.dadoutek.uled.base.TelinkBaseActivity
import com.dadoutek.uled.model.Constant
import com.dadoutek.uled.model.DbModel.DbGroup
import com.dadoutek.uled.model.SharedPreferencesHelper
import com.mob.tools.utils.SharePrefrenceHelper
import kotlinx.android.synthetic.main.activity_switch_double_touch.*


/**
 * 创建者     ZCL
 * 创建时间   2020/4/26 14:54
 * 描述
 * 更新者     $
 * 更新时间   $
 * 更新描述
 */

class DoubleTouchSwitchActivity : BaseActivity(), View.OnClickListener {
    private lateinit var leftGroup: DbGroup
    private lateinit var rightGroup: DbGroup
    private val requestCodeNum: Int = 1000
    private var isLeft: Boolean =false

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == requestCodeNum&&resultCode==Activity.RESULT_OK){
            var group = data?.getSerializableExtra(Constant.EIGHT_SWITCH_TYPE) as DbGroup
            if (isLeft)
                leftGroup = group
            else
                rightGroup = group

            switch_double_touch_left_tv.text = group.name
        }
    }

    private fun skipSelectGroup() {
        val intent = Intent(this@DoubleTouchSwitchActivity, ChooseGroupOrSceneActivity::class.java)
        intent.putExtra(Constant.EIGHT_SWITCH_TYPE, 0)//传入0代表是群组
        startActivityForResult(intent, requestCodeNum)
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.switch_double_touch_back -> finish()
            R.id.switch_double_touch_use_button -> finish()
            R.id.switch_double_touch_left -> {
                isLeft= true
                skipSelectGroup()
            }
            R.id.switch_double_touch_right -> {
                isLeft= false
                skipSelectGroup()
            }
            R.id.switch_double_touch_i_know -> {
                switch_double_touch_mb.visibility = View.GONE
                switch_double_touch_set.visibility = View.VISIBLE
                SharedPreferencesHelper.putBoolean(this, Constant.IS_FIRST_CONFIG_DOUBLE_SWITCH, false)
            }
        }
    }


    override fun initListener() {
        switch_double_touch_back.setOnClickListener(this)
        switch_double_touch_use_button.setOnClickListener(this)
        switch_double_touch_left.setOnClickListener(this)
        switch_double_touch_right.setOnClickListener(this)
        switch_double_touch_i_know.setOnClickListener(this)
    }

    override fun initData() {

    }

    override fun initView() {
        val b = SharedPreferencesHelper.getBoolean(this, Constant.IS_FIRST_CONFIG_DOUBLE_SWITCH, true)
        if (b) {
            switch_double_touch_mb.visibility = View.VISIBLE
            switch_double_touch_set.visibility = View.GONE
        } else {
            switch_double_touch_mb.visibility = View.GONE
            switch_double_touch_set.visibility = View.VISIBLE
        }
        switch_double_touch_i_know.paint.color = getColor(R.color.white)
        switch_double_touch_i_know.paint.flags = Paint.UNDERLINE_TEXT_FLAG //下划线
    }

    override fun setLayoutID(): Int {
        return R.layout.activity_switch_double_touch
    }
}