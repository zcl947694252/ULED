package com.dadoutek.uled.switches

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import com.blankj.utilcode.util.ToastUtils
import com.dadoutek.uled.R
import com.dadoutek.uled.base.TelinkBaseActivity
import com.dadoutek.uled.model.Constant
import com.dadoutek.uled.model.DbModel.DbGroup
import com.dadoutek.uled.model.DbModel.DbScene
import com.example.library.banner.BannerLayout
import kotlinx.android.synthetic.main.eight_switch.*


/**
 * 创建者     ZCL
 * 创建时间   2020/1/10 10:01
 * 描述 八键开关配置
 *
 * 更新者     $
 * 更新时间   $
 * 更新描述
 */
class ConfigEightSwitchActivity : TelinkBaseActivity(), View.OnClickListener {
    private var configSwitchType = 0
    private var configButtonTag = 0
    private val requestCodeNum = 100
    private var clickType = 0

    private val groupMap = mutableMapOf<Int, DbGroup>()
    private val sceneMap = mutableMapOf<Int, DbScene>()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.eight_switch)
        initView()
        initData()
        initListener()
    }

    private fun initListener() {
        eight_switch_retutn.setOnClickListener { finish() }
        eight_switch_use_button.setOnClickListener {
            if (clickType < 1) {
                eight_switch_config.visibility = View.VISIBLE
                eight_switch_banner_ly.visibility = View.GONE
                clickType = 1
            } else {
                clickType = 2
                confimCongfig()
            }
        }
        eight_switch_b1.setOnClickListener(this)
        eight_switch_b2.setOnClickListener(this)
        eight_switch_b3.setOnClickListener(this)
        eight_switch_b4.setOnClickListener(this)
        eight_switch_b5.setOnClickListener(this)
        eight_switch_b6.setOnClickListener(this)
        eight_switch_b7.setOnClickListener(this)
        eight_switch_b8.setOnClickListener(this)
    }

    private fun confimCongfig() {
        if (configSwitchType == 0) {
            if (groupMap.size >= 7) {
                // byteArrayOf(DeviceType.NIGHT_LIGHT.toByte(), CMD_CONTROL_GROUP.toByte(), 0, 0, 0, 0, 0, 0, 0, 0)

            } else
                ToastUtils.showLong("请点击配置按钮完成配置")
        } else if (configSwitchType == 1) {
            if (groupMap.size >= 7) {

            } else
                ToastUtils.showLong("请点击配置按钮完成配置")
        }
    }

    override fun onClick(v: View?) {
        var isCanClick = true
        when (v?.id) {
            R.id.eight_switch_b1 -> {
                isCanClick = configSwitchType != 0
                configButtonTag = 1
            }
            R.id.eight_switch_b2 -> {
                isCanClick = configSwitchType != 0
                configButtonTag = 2
            }
            R.id.eight_switch_b3 -> {
                isCanClick = configSwitchType != 0
                configButtonTag = 3
            }
            R.id.eight_switch_b4 -> {
                isCanClick = configSwitchType != 0
                configButtonTag = 4
            }
            R.id.eight_switch_b5 -> {
                isCanClick = true
                configButtonTag = 5
            }
            R.id.eight_switch_b6 -> {
                isCanClick = true
                configButtonTag = 6
            }
            R.id.eight_switch_b7 -> {
                isCanClick = true
                configButtonTag = 7
            }
            R.id.eight_switch_b8 -> {
                isCanClick = true
                configButtonTag = 8
            }
        }
        if (isCanClick) {
            val intent = Intent(this@ConfigEightSwitchActivity, ChooseGroupOrSceneActivity::class.java)
            intent.putExtra(Constant.EIGHT_SWITCH_TYPE, configSwitchType)
            startActivityForResult(intent, requestCodeNum)
        }
    }


    private fun initData() {}

    private fun initView() {
        val list = mutableListOf(R.drawable.big, R.drawable.big)
        eight_switch_banner.setAutoPlaying(false)
        //val bannerAdapter = BannerAdapter(R.layout.item_banner, list)
        val bannerAdapter = WebBannerAdapter(list)
        eight_switch_banner.setAdapter(bannerAdapter)
        bannerAdapter.setOnBannerItemClickListener(BannerLayout.OnBannerItemClickListener {
            configSwitchType = it
            groupMap.clear()
            sceneMap.clear()
        })

        eight_switch_banner.setOnBannerItemChangeListener {
            if (it == 0) {
                setTextColorsAndText(it)
                eight_switch_title.text = getString(R.string.group_switch)
            } else {
                setTextColorsAndText(R.color.click_config_color)
                eight_switch_title.text = getString(R.string.scene_switch)
            }
        }
    }

    private fun setTextColorsAndText(color: Int) {
        if (color == 0){
            eight_switch_b1.setTextColor(getColor(R.color.brightness_add_color))
            eight_switch_b2.setTextColor(getColor(R.color.brightness_add_color))
            eight_switch_b3.setTextColor(getColor(R.color.brightness_add_color))
            eight_switch_b4.setTextColor(getColor(R.color.brightness_add_color))

            eight_switch_b1.text = getString(R.string.brightness_add)
            eight_switch_b2.text = getString(R.string.color_temperature_add)
            eight_switch_b3.text = getString(R.string.brightness_minus)
            eight_switch_b4.text = getString(R.string.color_temperature_minus)
        }else{
            eight_switch_b1.setTextColor(getColor(R.color.click_config_color))
            eight_switch_b2.setTextColor(getColor(R.color.click_config_color))
            eight_switch_b3.setTextColor(getColor(R.color.click_config_color))
            eight_switch_b4.setTextColor(getColor(R.color.click_config_color))

            eight_switch_b1.text = getString(R.string.click_config)
            eight_switch_b2.text = getString(R.string.click_config)
            eight_switch_b3.text = getString(R.string.click_config)
            eight_switch_b4.text = getString(R.string.click_config)
        }

    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK && requestCode == requestCodeNum) {
            var name = ""
            if (configSwitchType == 0) {
                var group = data?.getSerializableExtra(Constant.EIGHT_SWITCH_TYPE) as DbGroup
                groupMap[configButtonTag] = group
                name = group.name
            } else {
                var scene = data?.getSerializableExtra(Constant.EIGHT_SWITCH_TYPE) as DbScene
                sceneMap[configButtonTag] = scene
                name = scene.name
            }

            when (configButtonTag) {
                1 -> eight_switch_b1.text = name
                2 -> eight_switch_b2.text = name
                3 -> eight_switch_b3.text = name
                4 -> eight_switch_b4.text = name
                5 -> eight_switch_b5.text = name
                6 -> eight_switch_b6.text = name
                7 -> eight_switch_b7.text = name
                8 -> eight_switch_b8.text = name
            }
        }
    }
}