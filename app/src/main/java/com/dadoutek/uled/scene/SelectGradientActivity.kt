package com.dadoutek.uled.scene

import android.app.Activity
import android.content.Intent
import android.support.v7.widget.LinearLayoutManager
import com.dadoutek.uled.R
import com.dadoutek.uled.base.BaseActivity
import com.dadoutek.uled.model.DbModel.DBUtils
import com.dadoutek.uled.model.DbModel.DbDiyGradient
import com.dadoutek.uled.model.ItemRgbGradient
import kotlinx.android.synthetic.main.template_recycleview.*
import kotlinx.android.synthetic.main.toolbar.*


/**
 * 创建者     ZCL
 * 创建时间   2020/5/11 18:31
 * 描述
 *
 * 更新者     $
 * 更新时间   $
 * 更新描述
 */
class SelectGradientActivity : BaseActivity() {
    private lateinit var currentRgbGradient: ItemRgbGradient
    private lateinit var diyGradientList: MutableList<DbDiyGradient>
    private var buildInModeList: ArrayList<ItemRgbGradient> = ArrayList()
    private val rgbSceneModeAdapter: RgbSceneModeAdapter = RgbSceneModeAdapter(R.layout.scene_mode, buildInModeList)
    override fun initListener() {

    }

    override fun initData() {
        getModeData()

    }

    private fun getModeData() {
        buildInModeList.clear()
        val presetGradientList = resources.getStringArray(R.array.preset_gradient)
        for (i in 1..11) {
            var item = ItemRgbGradient()
            item.id = i
            item.gradientType = 2//渐变类型 1：自定义渐变  2：内置渐变
            item.name = presetGradientList[i - 1]
            buildInModeList.add(item)
        }

        diyGradientList = DBUtils.diyGradientList
        diyGradientList.forEach {
            var item = ItemRgbGradient()
            item.id = it.id.toInt()
            item.name = it.name
            item.isDiy = true
            item.speed = it.speed
            item.gradientType = 1
            item.colorNodes = it.colorNodes
            buildInModeList.add(item)
        }

    }

    override fun initView() {
        toolbar.setNavigationIcon(R.drawable.icon_return)
        toolbar.setNavigationOnClickListener {
            finish()
        }
        toolbarTv.text = getString(R.string.model_list)
        template_recycleView.layoutManager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)
        template_recycleView.adapter = this.rgbSceneModeAdapter
        rgbSceneModeAdapter.setOnItemClickListener { _, _, position ->
            currentRgbGradient = buildInModeList[position]
            var intent = Intent()
            intent.putExtra("data", currentRgbGradient)
            setResult(Activity.RESULT_OK,intent)
            finish()
        }
    }

    override fun setLayoutID(): Int {
        return R.layout.activity_select_gradient

    }
}