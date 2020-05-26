package com.dadoutek.uled.pir

import android.os.Bundle
import android.view.View
import com.dadoutek.uled.R
import com.dadoutek.uled.base.TelinkBaseActivity
import kotlinx.android.synthetic.main.activity_pir_new.*
import kotlinx.android.synthetic.main.template_radiogroup.*
import kotlinx.android.synthetic.main.toolbar.*


/**
 * 创建者     ZCL
 * 创建时间   2020/5/18 14:38
 * 描述
 *
 * 更新者     $
 * 更新时间   $
 * 更新描述
 */
class PirConfigActivity : TelinkBaseActivity() {
    private var isGroupMode = true
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pir_new)
        initView()
        initData()
        initListener()
    }

    private fun initListener() {
        top_rg_ly.setOnCheckedChangeListener { _, checkedId ->
            isGroupMode = checkedId == color_mode_rb.id
            when (checkedId) {
                R.id.color_mode_rb -> {
                    pir_config_choose_group_ly.visibility = View.VISIBLE
                    pir_config_choose_scene_ly.visibility = View.GONE
                }
                R.id.gradient_mode_rb -> {
                    pir_config_choose_group_ly.visibility = View.GONE
                    pir_config_choose_scene_ly.visibility = View.VISIBLE
                }
            }
        }
    }

    private fun initData() {

    }

    private fun initView() {
        toolbar.title = getString(R.string.human_body)
        toolbar.setNavigationIcon(R.drawable.navigation_back_white)
        toolbar.setNavigationOnClickListener {
            finish()
        }
         color_mode_rb.text = getString(R.string.group_mode)
         gradient_mode_rb.text = getString(R.string.scene_mode)
    }
}