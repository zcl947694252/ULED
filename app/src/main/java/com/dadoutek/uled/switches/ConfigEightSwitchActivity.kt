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
import com.dadoutek.uled.model.DbModel.DbSwitch
import com.dadoutek.uled.model.Opcode
import com.dadoutek.uled.tellink.TelinkLightService
import com.example.library.banner.BannerLayout
import com.telink.bluetooth.light.DeviceInfo
import kotlinx.android.synthetic.main.eight_switch.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch


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
    private lateinit var switchDate: DbSwitch
    private var groupName: String? = null
    private var version: String? = null
    private lateinit var mDeviceInfo: DeviceInfo
    private var configSwitchType = 0
    private var configButtonTag = 0
    private val requestCodeNum = 100
    private var clickType = 0

    private val groupMap = mutableMapOf<Int, DbGroup>()
    private val groupParmList = mutableListOf<ByteArray>()
    private val sceneMap = mutableMapOf<Int, DbScene>()
    private val sceneParmList = mutableListOf<ByteArray>()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.eight_switch)
        initView()
        initData()
        initListener()
    }

    private fun initListener() {
        eight_switch_retutn.setOnClickListener {
            finish()
            TelinkLightService.Instance().idleMode(true)
            TelinkLightService.Instance().disconnect()
        }
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
        //成功后clickType = 0
        if (configSwitchType == 0) {

            if (groupMap.size >= 3 && groupMap.containsKey(5) && groupMap.containsKey(6) && groupMap.containsKey(7)) {
              sendParms()
               // TelinkLightService.Instance().sendCommandNoResponse(Opcode.CONFIG_SCENE_SWITCH, , )
            } else
                ToastUtils.showLong("请点击配置按钮完成配置")
        } else if (configSwitchType == 1) {
            if (sceneMap.size >= 7 && sceneMap.containsKey(1) && sceneMap.containsKey(2) &&
                    sceneMap.containsKey(3) && sceneMap.containsKey(4)&& sceneMap.containsKey(5)
                    && sceneMap.containsKey(6) && sceneMap.containsKey(7)&& sceneMap.containsKey(8)) {
            sendSceneParms()
            } else
                ToastUtils.showLong("请点击配置按钮完成配置")
        }
    }

    private fun sendSceneParms() {
        sceneParmList.clear()
        val first = mutableListOf(1, 2)
        val second = mutableListOf(3, 4)
        val third = mutableListOf(5, 6)
        val four: MutableList<Int> = if (sceneParmList.size > 7)
            mutableListOf(7, 8)
        else
            mutableListOf(7)

        getSceneParm(first)
        getSceneParm(second)
        getSceneParm(third)
        getSceneParm(four)

    }

    private fun sendParms() {
        groupParmList.clear()
        var firstParm = byteArrayOf(0x01, Opcode.GROUP_BRIGHTNESS_ADD, 0x00, 0x00, 0x02, Opcode.GROUP_CCT_ADD, 0x00, 0x00)
        var secondParm = byteArrayOf(0x03, Opcode.GROUP_BRIGHTNESS_MINUS, 0x00, 0x00, 0x04, Opcode.GROUP_CCT_MINUS, 0x00, 0x00)

        val third = mutableListOf(5, 6)
        val four: MutableList<Int> = if (groupMap.size > 3)
            mutableListOf(7, 8)
        else
            mutableListOf(7)
        val thirParm = getGroupParm(third)
        val fourParm = getGroupParm(four)

        groupParmList.add(0,firstParm)
        groupParmList.add(1,secondParm)
        groupParmList.add(2,thirParm)
        groupParmList.add(3,fourParm)

        var delay = 0L
        GlobalScope.launch {
           for (p in groupParmList){
               kotlinx.coroutines.delay(delay)
               TelinkLightService.Instance().sendCommandNoResponse(Opcode.CONFIG_SCENE_SWITCH,mDeviceInfo.meshAddress,p)
               delay += 300
           }
       }
}
    private fun getSceneParm(list: MutableList<Int>): ByteArray {
        val firstNum = list[0]
        val firsDbSceneId = sceneMap[firstNum]!!.id
        return if (list.size > 1) {
            val secondNum = list[1]
            val secondDbScene = groupMap[secondNum]
            byteArrayOf(firstNum.toByte(), 0x00, 0x00, firsDbSceneId.toByte(), secondNum.toByte(), Opcode.GROUP_SWITCH,  0x00,  0x00)
        } else {//如果第八键没有配置默认为关
            byteArrayOf(firstNum.toByte(), Opcode.GROUP_SWITCH,  0x00,  0x00, 0x08, Opcode.CLOSE, 0x00, 0x00)
        }
    }
    private fun getGroupParm(list: MutableList<Int>): ByteArray {
        val firstNum = list[0]
        val fiveMeshs = groupMap[firstNum]!!.meshAddr
        val fiveH = fiveMeshs.shr(8).toByte()
        val fiveL = fiveMeshs.and(0xff).toByte()
        return if (list.size > 1) {
            val secondNum = list[1]
            val sixMeshs = groupMap[secondNum]!!.meshAddr
            val sixH = sixMeshs.shr(8).toByte()
            val sixL =sixMeshs.and(0xff).toByte()

            byteArrayOf(firstNum.toByte(), Opcode.GROUP_SWITCH, fiveH, fiveL, secondNum.toByte(), Opcode.GROUP_SWITCH, sixH, sixL)
        } else {//如果第八键没有配置默认为关
            byteArrayOf(firstNum.toByte(), Opcode.GROUP_SWITCH, fiveH, fiveL, 0x08, Opcode.CLOSE, 0x00, 0x00)
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

    private fun initData() {

        mDeviceInfo = intent.getParcelableExtra("deviceInfo")
        version = intent.getStringExtra("version")
        eight_switch_tvLightVersion?.text = version

        groupName = intent.getStringExtra("group")
        if (groupName != null && groupName == "true") {
            switchDate = this.intent.extras!!.get("switch") as DbSwitch
        }
    }

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

    private fun setTextColorsAndText(type: Int) {
        if (type == 0) {
            eight_switch_b1.setTextColor(getColor(R.color.brightness_add_color))
            eight_switch_b2.setTextColor(getColor(R.color.brightness_add_color))
            eight_switch_b3.setTextColor(getColor(R.color.brightness_add_color))
            eight_switch_b4.setTextColor(getColor(R.color.brightness_add_color))

            eight_switch_b1.text = getString(R.string.brightness_add)
            eight_switch_b2.text = getString(R.string.color_temperature_add)
            eight_switch_b3.text = getString(R.string.brightness_minus)
            eight_switch_b4.text = getString(R.string.color_temperature_minus)
        } else {
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
            var name: String
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