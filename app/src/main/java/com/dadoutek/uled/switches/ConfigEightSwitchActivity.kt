package com.dadoutek.uled.switches

import android.annotation.SuppressLint
import android.app.Activity
import android.app.Dialog
import android.content.Intent
import android.os.Bundle
import android.support.v4.view.ViewPager
import android.util.Log
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import com.blankj.utilcode.util.LogUtils
import com.blankj.utilcode.util.ToastUtils
import com.dadoutek.uled.R
import com.dadoutek.uled.base.TelinkBaseActivity
import com.dadoutek.uled.communicate.Commander
import com.dadoutek.uled.model.Constant
import com.dadoutek.uled.model.DaoSessionInstance
import com.dadoutek.uled.model.DbModel.DBUtils
import com.dadoutek.uled.model.DbModel.DbEightSwitch
import com.dadoutek.uled.model.DbModel.DbGroup
import com.dadoutek.uled.model.DbModel.DbScene
import com.dadoutek.uled.model.HttpModel.EightSwitchMdodel
import com.dadoutek.uled.model.Opcode
import com.dadoutek.uled.tellink.TelinkLightService
import com.dadoutek.uled.util.MeshAddressGenerator
import com.dadoutek.uled.util.StringUtils
import com.telink.bluetooth.light.DeviceInfo
import kotlinx.android.synthetic.main.eight_switch.*
import kotlinx.coroutines.Dispatchers
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
    private var renameDialog: Dialog? = null
    private var renameConfirm: TextView? = null
    private var renameCancel: TextView? = null
    private var renameEditText: EditText? = null
    private var popReNameView: View? = null
    private var newMeshAddr: Int = 0
    private var switchDate: DbEightSwitch? = null
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

    private fun makePop() {
        popReNameView = View.inflate(this, R.layout.pop_rename, null)
        renameEditText = popReNameView?.findViewById<EditText>(R.id.pop_rename_edt)
        renameCancel = popReNameView?.findViewById<TextView>(R.id.pop_rename_cancel)
        renameConfirm = popReNameView?.findViewById<TextView>(R.id.pop_rename_confirm)
        renameConfirm?.setOnClickListener {
            // 获取输入框的内容
            if (StringUtils.compileExChar(renameEditText?.text.toString().trim { it <= ' ' })) {
                ToastUtils.showLong(getString(R.string.rename_tip_check))
            }  else {
                switchDate?.name = renameEditText?.text.toString().trim { it <= ' ' }
                if (switchDate == null)
                    switchDate = DBUtils.getEightSwitchByMeshAddr(mDeviceInfo?.meshAddress?:0)
                if (switchDate != null)
                    DBUtils.updateEightSwicth(switchDate!!)
                else
                    ToastUtils.showLong(getString(R.string.rename_faile))

                if (this != null && !this.isFinishing)
                    renameDialog?.dismiss()

                if (clickType < 1) {
                    eight_switch_config.visibility = View.VISIBLE
                    eight_switch_banner_ly.visibility = View.GONE
                    clickType = 1
                } else {
                    clickType = 2
                    confimCongfig()
                }
            }
        }
        renameCancel?.setOnClickListener {
            if (this != null && !this.isFinishing)
                renameDialog?.dismiss()
        }

        renameDialog = Dialog(this)
        renameDialog!!.setContentView(popReNameView)
        renameDialog!!.setCanceledOnTouchOutside(false)

        renameDialog?.setOnDismissListener {
            switchDate?.name = renameEditText?.text.toString().trim { it <= ' ' }
            if (switchDate!=null)
                DBUtils.updateEightSwicth(switchDate!!)
        }
    }

    @SuppressLint("SetTextI18n")
    private fun showRenameDialog() {
        StringUtils.initEditTextFilter(renameEditText)
        if (switchDate != null && switchDate?.name != "")
            renameEditText?.setText(switchDate?.name)
        else {
            if (switchDate != null && switchDate?.name != ""&&switchDate != null && switchDate?.name != null)
                renameEditText?.setText(switchDate?.name)
            else {
                var text = eight_switch_title.text.toString()
                renameEditText?.setText(text + "-" + DBUtils.eightSwitchList.size)
            }
        }
        renameEditText?.setSelection(renameEditText?.text.toString().length)
        if (this != null && !this.isFinishing) {
            renameDialog?.dismiss()
            renameDialog?.show()
        }
    }

    private fun initListener() {
        eight_switch_retutn.setOnClickListener {
            finish()
            TelinkLightService.Instance().idleMode(true)
            TelinkLightService.Instance().disconnect()
        }
        eight_switch_use_button.setOnClickListener {
            //showRenameDialog()
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
            if (groupMap.size >= 3 && groupMap.containsKey(5) && groupMap.containsKey(6) && groupMap.containsKey(4)) {
                sendParms()
                // TelinkLightService.Instance().sendCommandNoResponse(Opcode.CONFIG_SCENE_SWITCH, , )
            } else
                ToastUtils.showLong("请点击配置按钮完成配置")
        } else if (configSwitchType == 1) {
            if (sceneMap.size >= 7 && sceneMap.containsKey(1) && sceneMap.containsKey(2) &&
                    sceneMap.containsKey(3) && sceneMap.containsKey(4) && sceneMap.containsKey(5)
                    && sceneMap.containsKey(6) && sceneMap.containsKey(0)) {
                sendSceneParms()
            } else
                ToastUtils.showLong("请点击配置按钮完成配置")
        }
    }

    private fun sendSceneParms() {
        showLoadingDialog(getString(R.string.setting_switch))
        sceneParmList.clear()
        val first = mutableListOf(0, 1)
        val second = mutableListOf(2, 3)
        val third = mutableListOf(4, 5)
        val four: MutableList<Int> = if (sceneParmList.size > 7)
            mutableListOf(7, 6)
        else
            mutableListOf(6)

        val sceneParmOne = getSceneParm(first)
        val sceneParmTwo = getSceneParm(second)
        val sceneParmThird = getSceneParm(third)
        val sceneParmFour = getSceneParm(four)
        sceneParmList.add(sceneParmOne)
        sceneParmList.add(sceneParmTwo)
        sceneParmList.add(sceneParmThird)
        sceneParmList.add(sceneParmFour)
        var delay = 0L
        GlobalScope.launch {
            for (p in sceneParmList) {
                kotlinx.coroutines.delay(delay)
                TelinkLightService.Instance().sendCommandNoResponse(Opcode.CONFIG_SCENE_SWITCH, mDeviceInfo?.meshAddress
                        ?: 0, p)
                delay += 300
            }
            kotlinx.coroutines.delay(1500)
            updateMesh()
        }
    }

    private fun sendParms() {
        showLoadingDialog(getString(R.string.setting_switch))
        groupParmList.clear()
        var firstParm = byteArrayOf(0x00, Opcode.GROUP_BRIGHTNESS_ADD, 0x00, 0x00, 0x01, Opcode.GROUP_CCT_ADD, 0x00, 0x00)
        var secondParm = byteArrayOf(0x02, Opcode.GROUP_BRIGHTNESS_MINUS, 0x00, 0x00, 0x03, Opcode.GROUP_CCT_MINUS, 0x00, 0x00)

        val third = mutableListOf(4, 5)
        val four: MutableList<Int> = if (groupMap.size > 3)
            mutableListOf(7, 6)
        else
            mutableListOf(6)
        val thirParm = getGroupParm(third)
        val fourParm = getGroupParm(four)

        groupParmList.add(0, firstParm)
        groupParmList.add(1, secondParm)
        groupParmList.add(2, thirParm)
        groupParmList.add(3, fourParm)

        GlobalScope.launch {
            var delay = 0L
            for (p in groupParmList) {
                kotlinx.coroutines.delay(delay)
                TelinkLightService.Instance().sendCommandNoResponse(Opcode.CONFIG_SCENE_SWITCH, mDeviceInfo?.meshAddress
                        ?: 0, p)
                delay += 300
            }
            kotlinx.coroutines.delay(1500)
            updateMesh()
        }
    }

    private fun updateMesh() {
        newMeshAddr = MeshAddressGenerator().meshAddress
        Commander.updateMeshName(newMeshAddr = newMeshAddr, successCallback = {
            mDeviceInfo?.meshAddress = newMeshAddr
            updateSwitch()
            // disconnect()
            //                if (switchDate == null)
            //                    switchDate = DBUtils.getSwitchByMeshAddr(mDeviceInfo.meshAddress)
            GlobalScope.launch(Dispatchers.Main) {
                ToastUtils.showShort(getString(R.string.config_success))
                hideLoadingDialog()
            }
        }, failedCallback = {
            // mConfigFailSnackbar = snackbar(eight_switch_content, getString(R.string.pace_fail))
            GlobalScope.launch(Dispatchers.Main) {
                // pb_ly.visibility = View.GONE
                // mIsConfiguring = false
                GlobalScope.launch(Dispatchers.Main) {
                    hideLoadingDialog()
                    ToastUtils.showShort(getString(R.string.pace_fail))
                }
            }
        })
    }

    private fun updateSwitch() {
        if (groupName == "false") {
            var dbSwitch = DBUtils.getEightSwitchByMachAddr(mDeviceInfo!!.macAddress)
            if (dbSwitch != null) {
                dbSwitch!!.name = StringUtils.getSwitchPirDefaultName(mDeviceInfo.productUUID, this)+dbSwitch.meshAddr
               // dbSwitch.belongGroupId = mGroupArrayList[mAdapter.selectedPos].id
                //dbSwitch.controlGroupAddr = mGroupArrayList[mAdapter.selectedPos].meshAddr

                Log.e("zcl", "zcl*****设置新的开关使用更新$dbSwitch")
                DBUtils.updateEightSwicth(dbSwitch)
//                switchDate = dbSwitch
            } else {
//                var dbSwitch = DbSwitch()
//                DBUtils.saveSwitch(dbSwitch, false)
//                dbSwitch.belongGroupId = mGroupArrayList[mAdapter.selectedPos].id
//                dbSwitch.macAddr = mDeviceInfo.macAddress
//                dbSwitch.meshAddr = mDeviceInfo.meshAddress
//                dbSwitch.productUUID = mDeviceInfo.productUUID
//                dbSwitch.index = dbSwitch.id.toInt()
//                dbSwitch.controlGroupAddr = mGroupArrayList[mAdapter.selectedPos].meshAddr
//
//                Log.e("zcl", "zcl*****设置新的开关使用插入替换$dbSwitch")
//                DBUtils.saveSwitch(dbSwitch, false)
//
//                LogUtils.e("zcl", "zcl*****设置新的开关使用插入替换" + DBUtils.getAllSwitch())
//                val gotSwitchByMac = DBUtils.getSwitchByMacAddr(mDeviceInfo.macAddress)
//                DBUtils.recordingChange(gotSwitchByMac?.id,
//                        DaoSessionInstance.getInstance().dbSwitchDao.tablename,
//                        Constant.DB_ADD)
//                switchDate = dbSwitch
            }

            switchDate = dbSwitch
        } else {
            // switchDate!!.belongGroupId = mGroupArrayList[mAdapter.selectedPos].id
            // switchDate!!.controlGroupAddr = mGroupArrayList[mAdapter.selectedPos].meshAddr
            // DBUtils.updateSwicth(switchDate!!)
        }
    }


    private fun getSceneParm(list: MutableList<Int>): ByteArray {
        val firstNum = list[0]
        val firsDbSceneId = sceneMap[firstNum]!!.id
        return if (list.size > 1) {
            val secondNum = list[1]
            val secondDbSceneId = sceneMap[secondNum]!!.id
            byteArrayOf(firstNum.toByte(), 0x00, 0x00, firsDbSceneId.toByte(), secondNum.toByte(), 0x00, 0x00, secondDbSceneId.toByte())
        } else {//如果第八键没有配置默认为关
            byteArrayOf(firstNum.toByte(), 0x00, 0x00, firsDbSceneId.toByte(), 0x07, Opcode.CLOSE, 0x00, 0x00)
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
            val sixL = sixMeshs.and(0xff).toByte()

            byteArrayOf(firstNum.toByte(), Opcode.GROUP_SWITCH, fiveH, fiveL, secondNum.toByte(), Opcode.GROUP_SWITCH, sixH, sixL)
        } else {//如果第八键没有配置默认为关
            byteArrayOf(firstNum.toByte(), Opcode.GROUP_SWITCH, fiveH, fiveL, 0x08, Opcode.CLOSE, 0x00, 0x00)
        }
    }

    override fun onClick(v: View?) {
        var isCanClick = true
        when (v?.id) {
            R.id.eight_switch_b1 -> {
                isCanClick = configSwitchType != 0//是否可以點擊
                configButtonTag = 0//用於判斷是點擊的哪一個配置按鈕方便配置對應的藍牙命令
                EightSwitchMdodel.add()
                        ?.subscribe({
                            LogUtils.v("zcl八键添加$it")
                        }, {
                            LogUtils.v("zcl八键添加错误$it")
                        })
            }
            R.id.eight_switch_b2 -> {
                EightSwitchMdodel.get()
                        ?.subscribe({
                            //val itemBean = it.t
                            /*     var dbEightSwitch =    DbEightSwitch()
                                 dbEightSwitch.macAddr = itemBean.macAddr
                                 dbEightSwitch.name = itemBean.name
                                 dbEightSwitch.productUUID = itemBean.productUUID
                                 dbEightSwitch.meshAddr = itemBean.meshAddr
                                 dbEightSwitch.keys = itemBean.keys.toString()
                                 dbEightSwitch.firmwareVersion = version
                                 //dbEightSwitch.index = 1
                                 DBUtils.saveEightSwitch(dbEightSwitch,false)*/
                            LogUtils.v("zcl八键列表${it.t}-----${DBUtils.eightSwitchList}")
                        }, {
                            LogUtils.v("zcl八键列表错误$it")
                        })
                isCanClick = configSwitchType != 0
                configButtonTag = 1
            }
            R.id.eight_switch_b3 -> {
                EightSwitchMdodel.delete(1)
                        ?.subscribe({
                            LogUtils.v("zcl八键删除$it-----------${DBUtils.eightSwitchList}")
                        }, {
                            LogUtils.v("zcl八键删除错误$it")
                        })
                isCanClick = configSwitchType != 0
                configButtonTag = 2
            }
            R.id.eight_switch_b4 -> {
                EightSwitchMdodel.update()
                        ?.subscribe({
                            LogUtils.v("zcl八键批量删除${it.t}")
                        }, {
                            LogUtils.v("zcl八键批量删除错误$it")
                        })
                isCanClick = configSwitchType != 0
                //用於判斷是點擊的哪一個配置按鈕方便配置對應的藍牙命令
                configButtonTag = 3
            }

            /**
             * 1234的點擊事件是爲了測試接口 正常時應當禁掉 群組是不會有點擊反應的場景會有*/
            R.id.eight_switch_b5 -> {
                isCanClick = true
                configButtonTag = 4
            }
            R.id.eight_switch_b6 -> {
                isCanClick = true
                configButtonTag = 5
            }
            R.id.eight_switch_b7 -> {
                isCanClick = true
                configButtonTag = 6
            }
            R.id.eight_switch_b8 -> {
                isCanClick = true
                configButtonTag = 7
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
        if (groupName != null && groupName == "true")
            switchDate = this.intent.extras!!.get("switch") as DbEightSwitch
        updateEightSwitch()
    }

    private fun updateEightSwitch() {
        if (groupName == "false") {
            var dbSwitch = DBUtils.getEightSwitchByMachAddr(mDeviceInfo.macAddress)
            if (dbSwitch != null) {
                dbSwitch.name = StringUtils.getSwitchPirDefaultName(mDeviceInfo.productUUID, this) + dbSwitch!!.meshAddr
                dbSwitch.macAddr = mDeviceInfo.macAddress
                dbSwitch.meshAddr = /*Constant.SWITCH_PIR_ADDRESS*/ mDeviceInfo.meshAddress
                dbSwitch.productUUID = mDeviceInfo.productUUID
                DBUtils.updateEightSwicth(dbSwitch)
                switchDate = dbSwitch
            } else {
                var dbSwitch = DbEightSwitch()
                DBUtils.saveEightSwitch(dbSwitch, false)
                dbSwitch!!.meshAddr = /*Constant.SWITCH_PIR_ADDRESS*/mDeviceInfo.meshAddress
                dbSwitch!!.macAddr = mDeviceInfo.macAddress
                dbSwitch!!.productUUID = mDeviceInfo.productUUID
                dbSwitch!!.index = dbSwitch.id.toInt()
                DBUtils.saveEightSwitch(dbSwitch, false)
                val gotSwitchByMac = DBUtils.getSwitchByMacAddr(mDeviceInfo.macAddress)
                DBUtils.recordingChange(gotSwitchByMac?.id,
                        DaoSessionInstance.getInstance().dbSwitchDao.tablename,
                        Constant.DB_ADD)
                switchDate = dbSwitch
            }
        } else {
            switchDate!!.macAddr = mDeviceInfo.macAddress
            switchDate!!.meshAddr = Constant.SWITCH_PIR_ADDRESS
            switchDate!!.productUUID = mDeviceInfo.productUUID

            DBUtils.updateEightSwicth(switchDate!!)
        }
    }

    private fun initView() {
        val list = mutableListOf<View>()
        for (i in 0..1) {
            val view = layoutInflater.inflate(R.layout.item_banner, null, false)
            if (i == 0)
                view.findViewById<ImageView>(R.id.eight_switch_item_image).setImageResource(R.drawable.ggg)
            else
                view.findViewById<ImageView>(R.id.eight_switch_item_image).setImageResource(R.drawable.ggg)
            list.add(view)
        }
        eight_switch_banner.adapter = MoreItemVpAdapter(list, this)
        eight_switch_banner.offscreenPageLimit = 2
        eight_switch_banner.setPageTransformer(false, ScalePageTransformer(0.7f,this))
        eight_switch_banner.setOnPageChangeListener(object : ViewPager.OnPageChangeListener {
            override fun onPageScrollStateChanged(p0: Int) {}

            override fun onPageScrolled(p0: Int, p1: Float, p2: Int) {}

            override fun onPageSelected(p0: Int) {
                configSwitchType = p0
                groupMap.clear()
                sceneMap.clear()
                if (p0 == 0) {
                    configSwitchType = 0
                    setTextColorsAndText(p0)
                    eight_switch_title.text = getString(R.string.group_switch)
                } else {
                    configSwitchType = 1
                    setTextColorsAndText(R.color.click_config_color)
                    eight_switch_title.text = getString(R.string.scene_switch)
                }
            }
        })
        makePop()
    }
    /**
     * 创建pop并添加按钮监听
     */
    @SuppressLint("SetTextI18n")

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
                var scene = data?.getParcelableExtra(Constant.EIGHT_SWITCH_TYPE) as DbScene
                sceneMap[configButtonTag] = scene
                name = scene.name
            }

            when (configButtonTag) {
                0 -> eight_switch_b1.text = name
                1 -> eight_switch_b2.text = name
                2 -> eight_switch_b3.text = name
                3 -> eight_switch_b4.text = name
                4 -> eight_switch_b5.text = name
                5 -> eight_switch_b6.text = name
                6 -> eight_switch_b7.text = name
                7 -> eight_switch_b8.text = name
            }
        }
    }
}