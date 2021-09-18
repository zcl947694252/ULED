package com.dadoutek.uled.scene

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.recyclerview.widget.LinearLayoutManager
import com.dadoutek.uled.R
import com.dadoutek.uled.base.BaseActivity
import com.dadoutek.uled.base.TelinkBaseActivity
import com.dadoutek.uled.model.dbModel.DBUtils
import com.dadoutek.uled.model.dbModel.DbDiyGradient
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
class SelectGradientActivity : TelinkBaseActivity() {
    private lateinit var currentRgbGradient: ItemRgbGradient
    private lateinit var diyGradientList: MutableList<DbDiyGradient>
    private var buildInModeList: ArrayList<ItemRgbGradient> = ArrayList()
    private val rgbSceneModeAdapter: RgbSceneModeAdapter = RgbSceneModeAdapter(R.layout.scene_mode, buildInModeList)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_select_gradient)
        initView()
        initData()
    }

//    fun initListener() {
//
//    }

    fun initData() {
        getModeData()
    }

    private fun getModeData() {
        buildInModeList.clear()
        val presetGradientList = resources.getStringArray(R.array.preset_gradient)
        for (i in 1..11) {
            val item = ItemRgbGradient()
            item.id = i
            item.gradientType = 2//渐变类型 1：自定义渐变  2：内置渐变
            item.name = presetGradientList[i - 1]
            buildInModeList.add(item)
        }

        diyGradientList = DBUtils.diyGradientList
        diyGradientList.forEach {
            val item = ItemRgbGradient()
            item.id = it.id.toInt()
            item.name = it.name
            item.isDiy = true
            item.speed = it.speed
            item.gradientType = 1
            item.colorNodes = it.colorNodes
            buildInModeList.add(item)
        }

    }

    fun initView() {
        toolbar.setNavigationIcon(R.drawable.icon_return)
        toolbar.setNavigationOnClickListener {
            finish()
        }
        toolbarTv.text = getString(R.string.model_list)
        template_recycleView.layoutManager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)
        template_recycleView.adapter = this.rgbSceneModeAdapter
        rgbSceneModeAdapter.setOnItemClickListener { _, _, position ->
            currentRgbGradient = buildInModeList[position]
            val intent = Intent()
            intent.putExtra("data", currentRgbGradient)
            setResult(Activity.RESULT_OK,intent)
            finish()
        }
    }

//    fun setLayoutID(): Int {
//        return R.layout.activity_select_gradient
//    }
}