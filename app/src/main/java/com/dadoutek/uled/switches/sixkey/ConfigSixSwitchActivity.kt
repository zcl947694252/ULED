package com.dadoutek.uled.switches.sixkey

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.view.View
import androidx.appcompat.widget.Toolbar
import com.blankj.utilcode.util.LogUtils
import com.blankj.utilcode.util.ToastUtils
import com.dadoutek.uled.R
import com.dadoutek.uled.communicate.Commander
import com.dadoutek.uled.gateway.util.GsonUtil
import com.dadoutek.uled.model.Constant
import com.dadoutek.uled.model.DaoSessionInstance
import com.dadoutek.uled.model.Group
import com.dadoutek.uled.model.Opcode
import com.dadoutek.uled.model.dbModel.DBUtils
import com.dadoutek.uled.model.dbModel.DbGroup
import com.dadoutek.uled.model.dbModel.DbScene
import com.dadoutek.uled.model.dbModel.DbSwitch
import com.dadoutek.uled.model.routerModel.RouterModel
import com.dadoutek.uled.router.bean.Scene
import com.dadoutek.uled.switches.BaseSwitchActivity
import com.dadoutek.uled.switches.ChooseGroupOrSceneActivity
import com.dadoutek.uled.switches.bean.KeyBean
import com.dadoutek.uled.tellink.TelinkLightService
import com.dadoutek.uled.util.MeshAddressGenerator
import com.dadoutek.uled.util.StringUtils
import com.dadoutek.uled.util.SyncDataPutOrGetUtils
import com.telink.bluetooth.light.DeviceInfo
import io.reactivex.Observable
import kotlinx.android.synthetic.main.six_switch.*
import kotlinx.android.synthetic.main.toolbar.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit


/**
 * 创建者     Chown
 * 创建时间   2021/8/5 17:13
 * 描述
 */
class ConfigSixSwitchActivity: BaseSwitchActivity(), View.OnClickListener {

    private var newMeshAddr:Int = 0
    private lateinit var listKeysBean: JSONArray
    private var groupName:String? = null
    private var version:String? = null
    private var mDeviceInfo:DeviceInfo? = null
    private var configSwitchType = 0
    private var configSwitchTypeNum = 1
    private var configButtonTag = 1
    private var requestCodeNum = 100
    private var clickType =  0
    private val groupMap = mutableMapOf<Int,DbGroup>()
    private val groupParamList = mutableListOf<ByteArray>()
    private val sceneMap = mutableMapOf<Int,DbScene>()
    private val sceneParamList = mutableListOf<ByteArray>()
    private var count = 0


    override fun setToolBar(): Toolbar {
        return toolbar
    }

    override val setLayoutId: Int
        get() {
            return R.layout.six_switch
        }

    override fun setReConfig(): Boolean {
        return isReConfig
    }

    override fun setVersion() {
        if (TextUtils.isEmpty(version))
            version = getString(R.string.get_version_fail)
        else
            mDeviceInfo?.firmwareRevision = version
        fiVersion?.title = version
    }

    override fun setConnectMeshAddr(): Int {
        return mDeviceInfo?.meshAddress ?: 0
    }

    override fun deleteDevice() {

    }

    override fun goOta() {
    }

    override fun reName() {
    }

    override fun initListener() {
        toolbar.setOnMenuItemClickListener(menuItemClickListener)
        img_function1.setOnClickListener {
            changeMode()
        }
        six_switch_use_button.setOnClickListener {
            confimCongfig()
        }
        six_switch_b1.setOnClickListener(this)
        six_switch_b2.setOnClickListener(this)
        six_switch_b3.setOnClickListener(this)
        six_switch_b4.setOnClickListener(this)
        six_switch_b5.setOnClickListener(this)
        six_switch_b6.setOnClickListener(this)
        six_switch_b7.setOnClickListener(this)
        six_switch_b8.setOnClickListener(this)
        six_switch_b9.setOnClickListener(this)
    }

    private fun confimCongfig() {
        //成功后clickType = 0
        when (configSwitchType) {
            0 -> sendParms() // 群组开关 可调节亮度和色温
            1 -> sendSceneParms() // 场景开关
        }
    }

    private fun sendParms() {
        groupParamList.clear()
        listKeysBean = JSONArray()

        //11-12-13-14 11-12-13-14
        val first = mutableListOf(0)
        val second = mutableListOf(1)
        val third = mutableListOf(2)

        val firstParam = getGroupParm(first, 0x61)
        val secondParam = getGroupParm(second, 0x62)
        val thirdParam = getGroupParm(third, 0x63)

        LogUtils.v("chown获得的keys是$listKeysBean")
        groupParamList.add(0, firstParam)
        groupParamList.add(1, secondParam)
        groupParamList.add(2, thirdParam)

        if (!Constant.IS_ROUTE_MODE) {
            showLoadingDialog(getString(R.string.setting_switch))
            GlobalScope.launch {
                var delay = 1000.toLong()
                for (p in groupParamList) {
                    delay(delay)
                    //从第八位开始opcode, 设备meshAddr  参数11-12-13-14-15-16-17-18-19-20
                    //p = byteArrayOf(0x02, Opcode.GROUP_BRIGHTNESS_MINUS, 0x00, 0x00, 0x03, Opcode.GROUP_CCT_MINUS, 0x00, 0x00)
                    TelinkLightService.Instance().sendCommandNoResponse(Opcode.CONFIG_SCENE_SWITCH, mDeviceInfo?.meshAddress ?: 0, p)
                    delay += 300
                }
                delay(1500)
                updateMeshGroup(0)
            }
        } else {
            routerConfigEightSw(mDeviceInfo?.id?.toLong() ?: 0L)
        }
    }

    private fun sendSceneParms() {
        showLoadingDialog(getString(R.string.setting_switch))
        sceneParamList.clear()
        listKeysBean = JSONArray()

        val first = mutableListOf(0, 1)
        val second = mutableListOf(2, 3)
        val third = mutableListOf(4, 5)

        val sceneParamOne = getSceneParm(first, 0x61)
        val sceneParamTwo = getSceneParm(second, 0x63)
        val sceneParamThree = getSceneParm(third, 0x65)
        sceneParamList.add(sceneParamOne)
        sceneParamList.add(sceneParamTwo)
        sceneParamList.add(sceneParamThree)

        if (!Constant.IS_ROUTE_MODE) {
            var delay = 1000.toLong()
            GlobalScope.launch {
                for (p in sceneParamList) {
                    delay(delay)
                    TelinkLightService.Instance().sendCommandNoResponse(Opcode.CONFIG_SCENE_SWITCH, mDeviceInfo?.meshAddress ?: 0, p)
                    delay += 300
                }
                delay(1500)
                updateMeshGroup(1)
            }
        } else {
            routerConfigEightSw(mDeviceInfo?.id?.toLong() ?: 0L)
        }

    }

    private fun getSceneParm(list: MutableList<Int>, num : Int): ByteArray {
        var firstOpcode = Opcode.SCENE_SWITCH8K
        var secondOpcode = Opcode.SCENE_SWITCH8K

        val firstNum = list[0]
        val dbSceneFirst = sceneMap[firstNum]
        val firsDbSceneId = if (dbSceneFirst == null || dbSceneFirst.id == 65536L) {
            firstOpcode = Opcode.DEFAULT_SWITCH8K
            listKeysBean.put(getKeyBean(firstNum, firstOpcode.toInt() and 0xff, name = getString(R.string.click_config), hight8Mes = 0, low8Mes = 0))
            65536L
        } else {
            listKeysBean.put(getKeyBean(firstNum, firstOpcode.toInt() and 0xff, name = sceneMap[firstNum]!!.name, hight8Mes = 0, low8Mes = dbSceneFirst.id.toInt()))
            dbSceneFirst.id
        }
        val secondNum = list[1]
        val dbSceneSecond = sceneMap[secondNum]
        //位置 功能 保留 14场景id
        val secondDbSceneId = if (dbSceneSecond == null || dbSceneSecond.id == 65536L) {
            secondOpcode = Opcode.DEFAULT_SWITCH8K
            listKeysBean.put(getKeyBean(secondNum, secondOpcode.toInt() and 0xff, name = getString(R.string.click_config), hight8Mes = 0, low8Mes = 0))
            65536L
        } else {
            listKeysBean.put(getKeyBean(secondNum, secondOpcode.toInt() and 0xff, name = dbSceneSecond.name, hight8Mes = 0, low8Mes = dbSceneSecond.id.toInt()))
            dbSceneSecond.id
        }
        return  byteArrayOf(num.toByte(), 0x80.toByte(), 0x00, firsDbSceneId.toByte(), 0x00, (num+1).toByte(), 0x80.toByte(), 0x00, secondDbSceneId.toByte(), 0x00)
    }

    @SuppressLint("CheckResult")
    private fun routerConfigEightSw(id: Long) {
        val keys = GsonUtil.stringToList(listKeysBean.toString(), KeyBean::class.java)
        RouterModel.configEightSw(id, keys, "configEightSw",configSwitchType)?.subscribe({
            LogUtils.v("zcl-----------收到路由配置六键请求-------$it")
            when (it.errorCode) {
                0 -> {
                    showLoadingDialog(getString(R.string.please_wait))
                    disposableRouteTimer?.dispose()
                    disposableRouteTimer = Observable.timer(it.t.timeout.toLong(), TimeUnit.SECONDS)
                        .subscribe {
                            hideLoadingDialog()
                            ToastUtils.showShort(getString(R.string.config_fail))
                        }
                }
                90020 -> ToastUtils.showShort(getString(R.string.gradient_not_exit))
                90018 -> {
                    DBUtils.deleteLocalData()
                    //ToastUtils.showShort(getString(R.string.device_not_exit))
                    SyncDataPutOrGetUtils.syncGetDataStart(DBUtils.lastUser!!, syncCallbackGet)
                    finish()
                }
                90008 -> {hideLoadingDialog()
                    ToastUtils.showShort(getString(R.string.no_bind_router_cant_perform))}
                90007 -> ToastUtils.showShort(getString(R.string.gp_not_exit))
                90005 -> ToastUtils.showShort(getString(R.string.router_offline))
                90004 -> ToastUtils.showShort(getString(R.string.region_no_router))
                else-> ToastUtils.showShort(it.message)
            }
        }, {
            ToastUtils.showShort(it.message)
        })
    }

    private fun getGroupParm(list: MutableList<Int>, num: Int): ByteArray { // 0   1   2
        val firstNum = list[0]
        val dbGroup1 = groupMap[firstNum]!!
        var firstL: Byte = 0
        var firstH: Byte = 0
        val opcodeOne: Byte = 0x08.toByte()
        val opcodeTwo: Byte = 0x09.toByte()
        LogUtils.v("chown ++ -- group1 : $dbGroup1")
        if (dbGroup1 == null || dbGroup1.id == 65536L) {
            listKeysBean.put(getKeyBean(firstNum, opcodeOne.toInt() and 0xff, name = getString(R.string.click_config), hight8Mes = 0, low8Mes = 0))
            listKeysBean.put(getKeyBean(firstNum, opcodeTwo.toInt() and 0xff, name = getString(R.string.click_config), hight8Mes = 0, low8Mes = 0))
        } else {
            val firstMesAddr = dbGroup1.meshAddr
            val mesL = firstMesAddr and 0xff
            val mesH = (firstMesAddr shr 8) and 0xff // 右移8位
            firstL = mesL.toByte()
            firstH = mesH.toByte()
            listKeysBean.put(getKeyBean(firstNum, opcodeOne.toInt() and 0xff, name = groupMap[firstNum]!!.name, hight8Mes = mesH, low8Mes = mesL))
            listKeysBean.put(getKeyBean(firstNum, opcodeTwo.toInt() and 0xff, name = groupMap[firstNum]!!.name, hight8Mes = mesH, low8Mes = mesL))
        }

        return byteArrayOf(num.toByte(), opcodeOne, firstH, firstL, 0x00, (num+3).toByte(), opcodeTwo, firstH, firstL, 0x00)
    }

    private fun updateMeshGroup(isConfigGroup: Int) { // 1
        newMeshAddr = if (isReConfig) mDeviceInfo?.meshAddress ?: 0 else MeshAddressGenerator().meshAddress.get() // false
        LogUtils.v("chown-----------更新开关新mesh-------${newMeshAddr}")
        Commander.updateMeshName(newMeshAddr = newMeshAddr, successCallback = {
            mDeviceInfo?.meshAddress = newMeshAddr // 将设备的mesh地址重新调换
            updateSwitch(isConfigGroup)
            GlobalScope.launch(Dispatchers.Main) {
                ToastUtils.showShort(getString(R.string.config_success))
                if (!isReConfig)
                    showRenameDialog(switchDate, false)
                else
                    finishAc()
                hideLoadingDialog()
            }

        }, failedCallback = {
            GlobalScope.launch(Dispatchers.Main) {
                hideLoadingDialog()
                ToastUtils.showShort(getString(R.string.config_fail))
                delay(1500)
                finishAc()
            }
        })
    }

    private fun updateSwitch(configGroup: Int) { //1
        if (groupName == "false") {
            var dbSixSwitch = DBUtils.getSwitchByMacAddr(mDeviceInfo?.macAddress ?: "")
            if (dbSixSwitch != null) {
                dbSixSwitch.name = getString(R.string.six_switch) + "-" + (mDeviceInfo?.meshAddress ?: 0)
                dbSixSwitch.meshAddr = newMeshAddr
                dbSixSwitch.type = configGroup
                dbSixSwitch = setGroupIdsOrSceneIds(configGroup == 0, dbSixSwitch)
                dbSixSwitch.keys = listKeysBean.toString()
                if (TextUtils.isEmpty(version))
                    version = mDeviceInfo!!.firmwareRevision
                dbSixSwitch.version = version
                LogUtils.v("chown -- 保存dbSixSwitch $dbSixSwitch")
                DBUtils.updateSwicth(dbSixSwitch)
                switchDate = dbSixSwitch
            } else {
                var SixSwitch = DbSwitch()
                DBUtils.saveSwitch(SixSwitch, isFromServer = false, type = SixSwitch.type, keys = SixSwitch.keys)
                SixSwitch = setGroupIdsOrSceneIds(configGroup == 0, SixSwitch)
                SixSwitch.type = configGroup
                SixSwitch.macAddr = mDeviceInfo?.macAddress
                SixSwitch.meshAddr = mDeviceInfo?.meshAddress ?: 0
                SixSwitch.productUUID = mDeviceInfo?.productUUID ?: 0
                SixSwitch.index = SixSwitch.id.toInt()
                if (TextUtils.isEmpty(version))
                    version = mDeviceInfo!!.firmwareRevision
                SixSwitch.version = version

                SixSwitch.keys = listKeysBean.toString()

                Log.e("chown", "chown*****设置新的开关使用插入替换$SixSwitch")
                DBUtils.saveSwitch(SixSwitch, isFromServer = false, type = SixSwitch.type, keys = SixSwitch.keys)

                LogUtils.v("chown", "chown*****设置新的开关使用插入替换" + DBUtils.getAllSwitch())
                val gotSwitchByMac = DBUtils.getSwitchByMacAddr(mDeviceInfo?.macAddress ?: "")
                DBUtils.recordingChange(gotSwitchByMac?.id, DaoSessionInstance.getInstance().dbSwitchDao.tablename,
                    Constant.DB_ADD, SixSwitch.type, SixSwitch.keys)
                switchDate = SixSwitch
            }
        } else {
//            LogUtils.v("=================你在保存的时候走的是else==========")
            switchDate!!.type = configGroup
            switchDate!!.keys = listKeysBean.toString()
            switchDate?.meshAddr = mDeviceInfo?.meshAddress ?: 0
            //解析出來他的keys 重新賦值
            DBUtils.updateSwicth(switchDate!!)
        }
    }

    private fun setGroupIdsOrSceneIds(configGroup: Boolean, dbSixSwitch: DbSwitch): DbSwitch {
        if (configGroup) {
            var groupIds = ""
            groupMap.forEach {
                groupIds = groupIds + it.value.id + ","
            }
            dbSixSwitch.groupIds = groupIds
        } else {
            var sceneIds = ""
            sceneMap.forEach {
                sceneIds = sceneIds + it.value.id + ","
            }
            dbSixSwitch.sceneIds = sceneIds
        }
        return dbSixSwitch
    }

    private fun getKeyBean(keyId: Int, featureId: Int, name: String = "", hight8Mes: Int = 0, low8Mes: Int = 0): JSONObject {
        //return JSONObject(["keyId" = keyId, "featureId" = featureId, "reserveValue_A" = hight8Mes, "reserveValue_B" = low8Mes, "name" = name])
        //["keyId" = 11, "featureId" =11, "reserveValue_A" = 0x11, "reserveValue_B" = 0x11, "name" = name])
        val job = JSONObject()
        job.put("keyId", keyId)
        job.put("featureId", featureId)
        job.put("reserveValue_A", hight8Mes)
        job.put("reserveValue_B", low8Mes)
        job.put("name", name)
        val keyBean = KeyBean(keyId, featureId, name, hight8Mes, low8Mes);
        return job
    }

    fun changeMode() {
        setDefaultData()
        configSwitchTypeNum++
        when (configSwitchTypeNum % 2) { // 群组
            0 -> {
                setTextColorsAndText(0)
                configSwitchType = 0
                six_switch_title.text = getString(R.string.group_switch)
                count = 0
            }
            1 -> { // 场景
                configSwitchType = 1
                setTextColorsAndText(1)
                six_switch_title.text = getString(R.string.scene_switch)
                count = 0
            }
        }
    }

    override fun initData() {
        val groupKey = mutableListOf(0x61, 0x62, 0x63, 0x64, 0x65, 0x66)
        val sceneKey = mutableListOf(0x61, 0x62, 0x63, 0x64, 0x65, 0x66)

        setDefaultData()
        toolbarTv.text = getString(R.string.six_switch)
        mDeviceInfo = intent.getParcelableExtra("deviceInfo")
        version = intent.getStringExtra("version")
        setVersion()
        groupName = intent.getStringExtra("group")
        isReConfig = groupName != null && groupName == "true"
        fiRename?.isVisible = isReConfig

        if (isReConfig) {
            switchDate = this.intent.extras!!.get("switch") as DbSwitch
            LogUtils.v("chown=========================$switchDate")
            toolbarTv.text = switchDate?.name
            switchDate?.keys?.let {
                listKeysBean = JSONArray(it)
                toolbarTv.text = switchDate?.name

                val type = switchDate!!.type
                configSwitchType = type//赋值选择的模式
                clickType = 1//代表跳过选择模式

                setTextColorsAndText(type)

                for (i in 0 until listKeysBean.length()) {
                    //int keyId;  int featureId;   int reserveValue_A;  int reserveValue_B;  String name;
                    val jOb = listKeysBean.getJSONObject(i)
                    val keyId = jOb.getInt("keyId")
                    val featureId = jOb.getInt("featureId")
                    var name = jOb.getString("name")
                    when (type) {
                        0 -> {
                            val highMes = jOb.getInt("reserveValue_A")
                            val lowMes = jOb.getInt("reserveValue_B")

                            val mesAddress = (highMes shl 8) or lowMes
                            //赋值旧的设置数据
                            val groupByMeshAddr = if (featureId == 0xff)
                                null
                            else
                                DBUtils.getGroupByMeshAddr(mesAddress)

                            if (groupByMeshAddr != null) {
                                groupMap[keyId] = groupByMeshAddr
                                name = if (groupByMeshAddr.name == "")
                                    getString(R.string.click_config)
                                else
                                    groupByMeshAddr.name
                                groupKey.remove(keyId)
                            } else {
                                name = getString(R.string.click_config)
                            }
                            when (keyId) {
                                0 -> six_switch_b7.text = name
                                1 -> six_switch_b8.text = name
                                2 -> six_switch_b9.text = name
                            }
                            six_switch_title.text = getString(R.string.group_switch)
                        }
                        1 -> {
                            val sceneId = jOb.getInt("reserveValue_B")
                            val scene = DBUtils.getSceneByID(sceneId.toLong())
                            six_switch_title.text = getString(R.string.scene_switch)
                            //赋值旧的设置数据
                            sceneMap[keyId] = if (scene != null) {
                                name = if (scene.name == "")
                                    getString(R.string.click_config)
                                else
                                    scene.name
                                scene
                            } else {
                                val dbScene = DbScene()
                                dbScene.id = 65536L
                                name = getString(R.string.click_config)
                                dbScene
                            }
                            sceneKey.remove(keyId)
                            when (keyId) {
                                0 -> six_switch_b1.text = name
                                1 -> six_switch_b2.text = name
                                2 -> six_switch_b3.text = name
                                3 -> six_switch_b4.text = name
                                4 -> six_switch_b5.text = name
                                5 -> six_switch_b6.text = name
                            }
                        }
                    }
                }
            }
        } else {
            setTextColorsAndText(0)
        }

    }

    private fun setDefaultData() {
        groupMap.clear()
        sceneMap.clear()
        for (i in 0 until 4) {
            val dbGroup = DbGroup()
            dbGroup.id = 65536L
            groupMap[i] = dbGroup

            val dbScene = DbScene()
            dbScene.id = 65536L
            sceneMap[i] = dbScene
        }
    }

    @SuppressLint("SetTextI18n")
    private fun setTextColorsAndText(type: Int) {
        when (type) {
            0 -> {
                six_switch_b1.setTextColor(getColor(R.color.brightness_add_color))
                six_switch_b2.setTextColor(getColor(R.color.brightness_add_color))
                six_switch_b3.setTextColor(getColor(R.color.brightness_add_color))
                six_switch_b4.setTextColor(getColor(R.color.brightness_add_color))
                six_switch_b5.setTextColor(getColor(R.color.brightness_add_color))
                six_switch_b6.setTextColor(getColor(R.color.brightness_add_color))
                configSwitchTypeNum = 0
                six_switch_b1.text = "ON"
                six_switch_b2.text = "ON"
                six_switch_b3.text = "ON"
                six_switch_b4.text = "OFF"
                six_switch_b5.text = "OFF"
                six_switch_b6.text = "OFF"
                six_switch_b7.visibility = View.VISIBLE
                six_switch_b8.visibility = View.VISIBLE
                six_switch_b9.visibility = View.VISIBLE

                six_switch_b7.text = getString(R.string.click_config)
                six_switch_b8.text = getString(R.string.click_config)
                six_switch_b9.text = getString(R.string.click_config)
            }
            1 -> {
                six_switch_b1.setTextColor(getColor(R.color.click_config_color))
                six_switch_b2.setTextColor(getColor(R.color.click_config_color))
                six_switch_b3.setTextColor(getColor(R.color.click_config_color))
                six_switch_b4.setTextColor(getColor(R.color.click_config_color))
                six_switch_b5.setTextColor(getColor(R.color.click_config_color))
                six_switch_b6.setTextColor(getColor(R.color.click_config_color))
                configSwitchTypeNum = 1
                six_switch_b7.visibility = View.GONE
                six_switch_b8.visibility = View.GONE
                six_switch_b9.visibility = View.GONE
                six_switch_b1.text = getString(R.string.click_config)
                six_switch_b2.text = getString(R.string.click_config)
                six_switch_b3.text = getString(R.string.click_config)
                six_switch_b4.text = getString(R.string.click_config)
                six_switch_b5.text = getString(R.string.click_config)
                six_switch_b6.text = getString(R.string.click_config)
            }
        }
    }

    override fun initView() {
        toolbarTv!!.setText(R.string.six_switch)
        img_function1.visibility = View.VISIBLE
        img_function1.setImageResource(R.drawable.icon_change_small)
        toolbar.setNavigationOnClickListener { finishAc() }
        makePop()
    }

    private fun finishAc() {
        TelinkLightService.Instance()?.idleMode(true)
        TelinkLightService.Instance()?.disconnect()
        finish()
    }

    private fun makePop() {
        renameConfirm?.setOnClickListener {
            // 获取输入框的内容
            if (StringUtils.compileExChar(renameEt?.text.toString().trim { it <= ' ' })) {
                ToastUtils.showLong(getString(R.string.rename_tip_check))
            } else {
                val trim = renameEt?.text.toString().trim { it <= ' ' }
                if (!Constant.IS_ROUTE_MODE)
                    renameSw(trim)
                else
                    routerRenameSw(switchDate!!, trim)

                if (this != null && !this.isFinishing)
                    renameDialog.dismiss()
                LogUtils.v("zcl改名后-----------${DBUtils.getSwitchByMeshAddr(mDeviceInfo?.meshAddress ?: 0)?.name}")
            }
        }
        renameCancel?.setOnClickListener {
            if (this != null && !this.isFinishing)
                renameDialog.dismiss()
        }
        renameDialog.setOnDismissListener {
            if (!isReConfig)
                finishAc()
        }
    }

    private fun renameSw(trim: String) {
        switchDate?.name = trim
        if (switchDate == null)
            switchDate = DBUtils.getSwitchByMeshAddr(mDeviceInfo?.meshAddress ?: 0)
        toolbarTv.text = switchDate?.name
        if (switchDate != null)
            DBUtils.updateSwicth(switchDate!!)
        else
            ToastUtils.showLong(getString(R.string.rename_faile))
    }

    override fun onClick(v: View?) {
        var isClickable = true
        when(v?.id) {
            R.id.six_switch_b1 -> {
                isClickable = configSwitchType == 1 //只有场景模式能够点击
                configButtonTag = 0 //用於判斷是點擊的哪一個配置按鈕方便配置對應的藍牙命令
            }
            R.id.six_switch_b2 -> {
                isClickable = configSwitchType == 1
                configButtonTag = 1
            }
            R.id.six_switch_b3 -> {
                isClickable = configSwitchType == 1
                configButtonTag = 2
            }
            R.id.six_switch_b4 -> {
                isClickable = configSwitchType == 1
                configButtonTag = 3
            }
            R.id.six_switch_b5 -> {
                isClickable = configSwitchType == 1
                configButtonTag = 4
            }
            R.id.six_switch_b6 -> {
                isClickable = configSwitchType == 1
                configButtonTag = 5
            }

            R.id.six_switch_b7 -> {
                isClickable = false
                configButtonTag = 6
                val intent = Intent(this, ChooseGroupOrSceneActivity::class.java)
                //传入0代表是群组
                val bundle = Bundle()
                bundle.putInt(Constant.EIGHT_SWITCH_TYPE, 0)//传入0代表是群组
                bundle.putInt(Constant.DEVICE_TYPE, Constant.DEVICE_TYPE_LIGHT_SW.toInt())
                intent.putExtras(bundle)
                startActivityForResult(intent, requestCodeNum)
            }
            R.id.six_switch_b8 -> {
                isClickable = false
                configButtonTag = 7
                val intent = Intent(this, ChooseGroupOrSceneActivity::class.java)
                //传入0代表是群组
                val bundle = Bundle()
                bundle.putInt(Constant.EIGHT_SWITCH_TYPE, 0)//传入0代表是群组
                bundle.putInt(Constant.DEVICE_TYPE, Constant.DEVICE_TYPE_LIGHT_SW.toInt())
                intent.putExtras(bundle)
                startActivityForResult(intent, requestCodeNum)
            }
            R.id.six_switch_b9 -> {
                isClickable = false
                configButtonTag = 8
                val intent = Intent(this, ChooseGroupOrSceneActivity::class.java)
                //传入0代表是群组
                val bundle = Bundle()
                bundle.putInt(Constant.EIGHT_SWITCH_TYPE, 0)//传入0代表是群组
                bundle.putInt(Constant.DEVICE_TYPE, Constant.DEVICE_TYPE_LIGHT_SW.toInt())
                intent.putExtras(bundle)
                startActivityForResult(intent, requestCodeNum)
            }
        }

        if (isClickable) {
            val intent = Intent(this, ChooseGroupOrSceneActivity::class.java)
            val bundle = Bundle()
            bundle.putInt(Constant.EIGHT_SWITCH_TYPE, 1) //传入0代表是群组，传入1代表的是场景
            bundle.putInt(Constant.DEVICE_TYPE, Constant.DEVICE_TYPE_LIGHT_SW.toInt())
            intent.putExtras(bundle)
            startActivityForResult(intent, requestCodeNum)
        }

    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK && requestCode == requestCodeNum) {
            var name: String = ""
            when (configSwitchType) {
                0 -> {
                    val group = data?.getSerializableExtra(Constant.EIGHT_SWITCH_TYPE) as DbGroup
                    groupMap[count++] = group
                    name = group.name
                }
                else -> {
                    val scene = data?.getParcelableExtra(Constant.EIGHT_SWITCH_TYPE) as DbScene
                    //var scene = data?.getParcelableExtra(Constant.EIGHT_SWITCH_TYPE) as DbScene
                    scene.let {
                        sceneMap[configButtonTag] = it
                        name = it.name
                    }
                }
            }
            when (configButtonTag) {
                0 -> six_switch_b1.text = name
                1 -> six_switch_b2.text = name
                2 -> six_switch_b3.text = name
                3 -> six_switch_b4.text = name
                4 -> six_switch_b5.text = name
                5 -> six_switch_b6.text = name
                6 -> six_switch_b7.text = name
                7 -> six_switch_b8.text = name
                8 -> six_switch_b9.text = name
            }
        }
    }

}