package com.dadoutek.uled.switches.fourkey

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.widget.Toolbar
import com.blankj.utilcode.util.LogUtils
import com.blankj.utilcode.util.ToastUtils
import com.dadoutek.uled.R
import com.dadoutek.uled.communicate.Commander
import com.dadoutek.uled.curtain.CurtainsOfGroupRecyclerViewAdapter
import com.dadoutek.uled.gateway.util.GsonUtil
import com.dadoutek.uled.model.*
import com.dadoutek.uled.model.dbModel.DBUtils
import com.dadoutek.uled.model.dbModel.DbGroup
import com.dadoutek.uled.model.dbModel.DbScene
import com.dadoutek.uled.model.dbModel.DbSwitch
import com.dadoutek.uled.model.routerModel.RouterModel
import com.dadoutek.uled.router.bean.CmdBodyBean
import com.dadoutek.uled.router.bean.Scene
import com.dadoutek.uled.switches.BaseSwitchActivity
import com.dadoutek.uled.switches.ChooseGroupOrSceneActivity
import com.dadoutek.uled.switches.bean.KeyBean
import com.dadoutek.uled.tellink.TelinkLightService
import com.dadoutek.uled.util.MeshAddressGenerator
import com.dadoutek.uled.util.StringUtils
import com.dadoutek.uled.util.SyncDataPutOrGetUtils
import com.telink.bluetooth.light.DeviceInfo
import com.telink.util.Arrays
import io.reactivex.Observable
import kotlinx.android.synthetic.main.activity_switch_group.*
import kotlinx.android.synthetic.main.eight_switch.*
import kotlinx.android.synthetic.main.four_switch.*
import kotlinx.android.synthetic.main.toolbar.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import java.util.logging.Level.OFF


/**
 * 创建者     Chown
 * 创建时间   2021/8/5 17:13
 * 描述
 */
class ConfigFourSwitchActivity : BaseSwitchActivity(),View.OnClickListener {

    private var newMeshAddr: Int = 0
    private lateinit var listKeysBean: JSONArray
    private var groupName: String? = null
    private var version: String? = null
    private var mDeviceInfo: DeviceInfo? = null
    private var configSwitchType = 2
    private var configSwitchTypeNum = 2
    private var currentGroup: DbGroup? = null
    private var configButtonTag = 1
    private var requestCodeNum = 100
    private var clickType = 0
    private val groupMap = mutableMapOf<Int, DbGroup>()
    private val groupParamList = mutableListOf<ByteArray>()
    private val sceneMap = mutableMapOf<Int, DbScene>()
    private val sceneParamList = mutableListOf<ByteArray>()
    private var isOK = false

    override fun setToolBar(): Toolbar {
        return toolbar
    }

    override val setLayoutId: Int
        get() {
            return R.layout.four_switch
        }

    override fun setReConfig(): Boolean {
        return isReConfig
    }

    override fun setVersion() {
        if (!TextUtils.isEmpty(version)) // version 还未获得
//            version = getString(R.string.get_version_fail)
            mDeviceInfo?.firmwareRevision = version
        fiVersion?.title = version
    }

    override fun setConnectMeshAddr(): Int {
        return mDeviceInfo?.meshAddress ?: 0
    }

    override fun deleteDevice() {
        when {
            mDeviceInfo != null -> deleteSwitch(mDeviceInfo!!.macAddress)
            else -> ToastUtils.showShort(getString(R.string.invalid_data))
        }
    }

    override fun goOta() {
        if (mDeviceInfo != null)
            deviceOta(mDeviceInfo!!, DeviceType.FOUR_SWITCH)
        else
            ToastUtils.showShort(getString(R.string.invalid_data))
    }

    override fun reName() {
        showRenameDialog(switchDate, false)
    }

    override fun initListener() {
        toolbar.setOnMenuItemClickListener(menuItemClickListener)
        img_function1.setOnClickListener {
            changeMode()
        }
        four_switch_use_button.setOnClickListener {
            confimCongfig()
        }
        four_switch_b1.setOnClickListener(this)
        four_switch_b2.setOnClickListener(this)
        four_switch_b3.setOnClickListener(this)
        four_switch_b4.setOnClickListener(this)
        four_switch_b5.setOnClickListener(this)
        four_switch_b6.setOnClickListener(this)
        four_group_name.setOnClickListener(this)

    }

    private fun confimCongfig() {
        //成功后clickType = 0
        when (configSwitchType) {
            0 -> sendParms() // 群组开关 可调节亮度和色温
            1 -> sendSceneParms() // 场景开关
            2 -> sendSingleGroupParms() // 单调光
        }
    }

    private fun sendSingleGroupParms() { //chown
        if (currentGroup==null) {
            Toast.makeText(this,"组不能为空",Toast.LENGTH_SHORT).show()
            return
        }
        // ===================================
        groupParamList.clear()
        listKeysBean = JSONArray()

        val firstMesAddr = currentGroup?.meshAddr
        val mesL = firstMesAddr!! and 0xff
        val mesH = (firstMesAddr shr 8) and 0xff // 右移8位
        val firstL = mesL.toByte()
        val firstH = mesH.toByte()
        //11-12-13-14 11-12-13-14
        val firstParm = byteArrayOf(0x41,0x01,firstH,firstL,0x00,0x43,0x06,firstH,firstL,0x00)
        val secondParm = byteArrayOf(0x44,0x02,firstH,firstL,0x00,0x46,0x07,firstH,firstL,0x00)

        groupParamList.add(0, firstParm)
        groupParamList.add(1, secondParm)

        listKeysBean.put(getKeyBean(0x41, 0x01,currentGroup!!.name,mesH,mesL))
        listKeysBean.put(getKeyBean(0x43, 0x06,currentGroup!!.name,mesH,mesL))
        listKeysBean.put(getKeyBean(0x44, 0x02,currentGroup!!.name,mesH,mesL))
        listKeysBean.put(getKeyBean(0x46, 0x07,currentGroup!!.name,mesH,mesL))
//        LogUtils.v("chown獲得的keys是$listKeysBean")

        if (!Constant.IS_ROUTE_MODE) {
            showLoadingDialog(getString(R.string.setting_switch))
            GlobalScope.launch {
                var delay = 1000L
                for (p in groupParamList) {
                    delay(delay)
                    //从第八位开始opcode, 设备meshAddr  参数11-12-13-14 15-16-17-18
                    TelinkLightService.Instance().sendCommandNoResponse(Opcode.CONFIG_SCENE_SWITCH, mDeviceInfo?.meshAddress ?: 0, p)
                    delay += 300
                }
                delay(1500)
                updateMeshGroup(2)
            }
        } else {
            routerConfigFourAndSixSw(mDeviceInfo?.id?.toLong() ?: 0L)
        }

    }

    private fun sendSceneParms() {
        sceneParamList.clear()
        listKeysBean = JSONArray()

        val first = mutableListOf(0x41, 0x43)
        val second = mutableListOf(0x44, 0x46)

        val sceneParmOne = getSceneParm(first)
        val sceneParmTwo = getSceneParm(second)
        sceneParamList.add(sceneParmOne)
        sceneParamList.add(sceneParmTwo)

        if (!isOK) {
            Toast.makeText(this,"至少选择一个场景",Toast.LENGTH_SHORT).show()
            return
        }
        showLoadingDialog(getString(R.string.setting_switch))
        if (!Constant.IS_ROUTE_MODE){
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
        }else{
            routerConfigFourAndSixSw(mDeviceInfo?.id?.toLong() ?: 0L)
        }
    }

    private fun getSceneParm(list: MutableList<Int>): ByteArray {
        val firstNum = list[0] //4
        val index: Int = if (firstNum == 0x41) 0 else 2
        val dbSceneFirst = sceneMap[index]
        var opcodeOne: Byte = 0x80.toByte()
        var opcodeTwo: Byte = 0x80.toByte()
        val firsDbSceneId = if (dbSceneFirst == null || dbSceneFirst.id == 65536L) {
            opcodeOne = 0x00
            listKeysBean.put(getKeyBean(firstNum, 0x00, name = getString(R.string.click_config), hight8Mes = 0, low8Mes = 0)) // 场景指令请看协议
            65536L
        } else {
            isOK = true
            listKeysBean.put(getKeyBean(firstNum, 0x80, name = dbSceneFirst.name, hight8Mes = 0, low8Mes = dbSceneFirst.id.toInt()))
            dbSceneFirst.id
        }
        val secondNum = list[1]
        val dbSceneSecond = sceneMap[index+1]
        //位置 功能 保留 14场景id
        val secondDbSceneId = if (dbSceneSecond == null || dbSceneSecond.id == 65536L) {
            opcodeTwo = 0x00
            listKeysBean.put(getKeyBean(secondNum, 0x00, name = getString(R.string.click_config), hight8Mes = 0, low8Mes = 0))
            65536L
        } else {
            isOK = true
            listKeysBean.put(getKeyBean(secondNum, 0x80, name = dbSceneSecond.name, hight8Mes = 0, low8Mes = dbSceneSecond.id.toInt()))
            dbSceneSecond.id
        }
        return  byteArrayOf(firstNum.toByte(), opcodeOne, 0x00, firsDbSceneId.toByte(), 0x00, secondNum.toByte(), opcodeTwo, 0x00, secondDbSceneId.toByte(), 0x00)
    }

    private fun sendParms() {
        groupParamList.clear()
        listKeysBean = JSONArray()

        //11-12-13-14 11-12-13-14
        val first = mutableListOf(0x41, 0x43)
        val second = mutableListOf(0x44, 0x46)

        val firstParm = getGroupParm(first)
        val secondParm = getGroupParm(second)

        LogUtils.v("chown獲得的keys是$listKeysBean")
        groupParamList.add(0, firstParm)
        groupParamList.add(1, secondParm)

        if (!isOK) {
            Toast.makeText(this,"至少选择一个组",Toast.LENGTH_SHORT).show()
            return
        }

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
            routerConfigFourAndSixSw(mDeviceInfo?.id?.toLong() ?: 0L)
        }
    }

    @SuppressLint("CheckResult")
    private fun routerConfigFourAndSixSw(id: Long) {
        val keys = GsonUtil.stringToList(listKeysBean.toString(), KeyBean::class.java)
        LogUtils.v("chown listKeysBean $keys")
//        RouterModel.configEightSw(id, keys, "configEightSw",configSwitchType)?.subscribe({
            RouterModel.configFourAndSixSw("3027", id, configSwitchType, keys)?.subscribe({
            LogUtils.v("zcl-----------收到路由配置四键请求-------$it")
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

    override fun tzRouterConfigFourSwRecevice(cmdBean: CmdBodyBean) {
        LogUtils.v("zcl-----------收到路由配置八键通知-------$cmdBean")
        if (cmdBean.ser_id == "3027") {
            disposableRouteTimer?.dispose()
            hideLoadingDialog()
            if (cmdBean.status == 0) {
                GlobalScope.launch(Dispatchers.Main) {
                    ToastUtils.showShort(getString(R.string.config_success))
                    if (Constant.IS_ROUTE_MODE)
                        updateAllSwitch()
                    if (!isReConfig)
                        showRenameDialog(switchDate!!,true)
                    else
                        finish()
                }
            } else {
                ToastUtils.showShort(getString(R.string.config_fail))
            }
        }
    }

    private fun updateSwitch(configGroup: Int) { //1
        if (groupName == "false") {
            var dbFourSwitch = DBUtils.getSwitchByMacAddr(mDeviceInfo?.macAddress ?: "")
            if (dbFourSwitch != null) {
                dbFourSwitch.name = getString(R.string.four_switch) + "-" + (mDeviceInfo?.meshAddress ?: 0)
                dbFourSwitch.meshAddr = newMeshAddr
                dbFourSwitch.type = configGroup
                dbFourSwitch = setGroupIdsOrSceneIds(configGroup == 0, dbFourSwitch)
                dbFourSwitch.keys = listKeysBean.toString()
                if (TextUtils.isEmpty(version))
                    version = mDeviceInfo!!.firmwareRevision
                dbFourSwitch.version = version
                LogUtils.v("chown -- 保存dbFourSwitch $dbFourSwitch")
                DBUtils.updateSwicth(dbFourSwitch)
                switchDate = dbFourSwitch
            } else {
                var fourSwitch = DbSwitch()
                DBUtils.saveSwitch(fourSwitch, isFromServer = false, type = fourSwitch.type, keys = fourSwitch.keys)
                fourSwitch = setGroupIdsOrSceneIds(configGroup == 0, fourSwitch)
                fourSwitch.type = configGroup
                fourSwitch.macAddr = mDeviceInfo?.macAddress
                fourSwitch.meshAddr = mDeviceInfo?.meshAddress ?: 0
                fourSwitch.productUUID = mDeviceInfo?.productUUID ?: 0
                fourSwitch.index = fourSwitch.id.toInt()
                if (TextUtils.isEmpty(version))
                    version = mDeviceInfo!!.firmwareRevision
                fourSwitch.version = version

                fourSwitch.keys = listKeysBean.toString()

                Log.e("chown", "chown*****设置新的开关使用插入替换$fourSwitch")
                DBUtils.saveSwitch(fourSwitch, isFromServer = false, type = fourSwitch.type, keys = fourSwitch.keys)

                LogUtils.v("chown", "chown*****设置新的开关使用插入替换" + DBUtils.getAllSwitch())
                val gotSwitchByMac = DBUtils.getSwitchByMacAddr(mDeviceInfo?.macAddress ?: "")
                DBUtils.recordingChange(gotSwitchByMac?.id, DaoSessionInstance.getInstance().dbSwitchDao.tablename,
                    Constant.DB_ADD, fourSwitch.type, fourSwitch.keys)
                switchDate = fourSwitch
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

    private fun setGroupIdsOrSceneIds(configGroup: Boolean, dbFourSwitch: DbSwitch): DbSwitch {
        if (configGroup) {
            var groupIds = ""
            groupMap.forEach {
                groupIds = groupIds + it.value.id + ","
            }
            dbFourSwitch.groupIds = groupIds
        } else {
            var sceneIds = ""
            sceneMap.forEach {
                sceneIds = sceneIds + it.value.id + ","
            }
            dbFourSwitch.sceneIds = sceneIds
        }
        return dbFourSwitch
    }

    private fun getGroupParm(list: MutableList<Int>): ByteArray { // 只有两组
        val firstNum = list[0]
        val dbGroup1 = groupMap[0]!!
        var firstL: Byte = 0
        var firstH: Byte = 0
        var opcodeOne: Byte = if(firstNum == 0x41) 0x08 else 0x09
        var opcodeTwo: Byte = if(firstNum == 0x41) 0x08 else 0x09
        LogUtils.v("chown ++ -- group1 : $dbGroup1")
        if (dbGroup1 == null || dbGroup1.id == 65536L) {
            opcodeOne = 0x00
            listKeysBean.put(getKeyBean(firstNum, 0x00, name = getString(R.string.click_config), hight8Mes = 0, low8Mes = 0))
        } else {
            isOK = true
            val firstMesAddr = dbGroup1.meshAddr
            val mesL = firstMesAddr and 0xff
            val mesH = (firstMesAddr shr 8) and 0xff // 右移8位
            firstL = mesL.toByte()
            firstH = mesH.toByte()
            listKeysBean.put(getKeyBean(firstNum, opcodeOne.toInt() and 0xff, name = dbGroup1.name, hight8Mes = mesH, low8Mes = mesL))
        }
        val secondNum = list[1]
        val dbGroup2 = groupMap[1]
        LogUtils.v("chown ++ -- group2 : ${dbGroup2.toString()}")
        var secondL: Byte = 0
        var secondH: Byte = 0
        if (dbGroup2 == null || dbGroup2.id == 65536L) {
            opcodeTwo = 0x00
            listKeysBean.put(getKeyBean(secondNum, 0x00, name = getString(R.string.click_config), hight8Mes = 0, low8Mes = 0))
        } else {
            isOK = true
            val secondMesAddr = dbGroup2.meshAddr
            val mesL = secondMesAddr and 0xff
            val mesH = (secondMesAddr shr 8) and 0xff
            secondL = mesL.toByte()
            secondH = mesH.toByte()
            listKeysBean.put(getKeyBean(secondNum, opcodeTwo.toInt() and 0xff, name = dbGroup2.name, hight8Mes = mesH, low8Mes = mesL))
        }
        return byteArrayOf(firstNum.toByte(), opcodeOne, firstH, firstL, 0x00, secondNum.toByte(), opcodeTwo, secondH, secondL, 0x00)
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
        isOK = false
        when (configSwitchTypeNum % 3) { // 群组
            0 -> {
                setTextColorsAndText(0)
                configSwitchType = 0
                four_switch_title.text = getString(R.string.group_switch)
            }
            1 -> { // 场景
                configSwitchType = 1
                setTextColorsAndText(1)
                four_switch_title.text = getString(R.string.scene_switch)
            }
            2 -> { // 单调光
                configSwitchType = 2
                setTextColorsAndText(2)
                four_switch_title.text = getString(R.string.single_brighress_group_switch)
            }
        }
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
                toolbarTv.text = trim
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
        if (switchDate != null) {
            DBUtils.updateSwicth(switchDate!!)
            LogUtils.v("chown ==-=-=-=-=-=-=-= 更新数据库")
        }
        else
            ToastUtils.showLong(getString(R.string.rename_faile))
    }

    private fun finishAc() {
        TelinkLightService.Instance()?.idleMode(true)
        TelinkLightService.Instance()?.disconnect()
        finish()
    }

    override fun initData() {
        val groupKey = mutableListOf(0x41, 0x43, 0x44, 0x46)
        val sceneKey = mutableListOf(0x41, 0x43, 0x44, 0x46)

        setDefaultData()
        toolbarTv.text = getString(R.string.four_switch)
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
                                val count = if (keyId == 0x41) 0 else if (keyId == 0x43) 1 else if (keyId == 0x44) 2 else 3
                                groupMap[count] = groupByMeshAddr
                                name = if (groupByMeshAddr.name == "")
                                    getString(R.string.click_config)
                                else
                                    groupByMeshAddr.name
                                groupKey.remove(keyId)
                            } else {
                                name = getString(R.string.click_config)
                            }
                            when (keyId) {
                                0x41 -> four_switch_b5.text = name
                                0x43 -> four_switch_b6.text = name
                            }
                            four_switch_title.text = getString(R.string.group_switch)
                        }
                        1 -> {
                            val sceneId = jOb.getInt("reserveValue_B")
                            val scene = DBUtils.getSceneByID(sceneId.toLong())
                            four_switch_title.text = getString(R.string.scene_switch)
                            //赋值旧的设置数据
                            val count = if (keyId == 0x41) 0 else if (keyId == 0x43) 1 else if (keyId == 0x44) 2 else 3
                            sceneMap[count] = if (scene != null) {
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
                                0x41 -> four_switch_b1.text = name
                                0x43 -> four_switch_b2.text = name
                                0x44 -> four_switch_b3.text = name
                                0x46 -> four_switch_b4.text = name
                            }
                        }
                        2 -> {
                            val highMes = jOb.getInt("reserveValue_A")
                            val lowMes = jOb.getInt("reserveValue_B")
                            four_switch_title.text = getString(R.string.single_brighress_group_switch)
                            four_group_name.visibility = View.VISIBLE
                            val mesAddress = (highMes shl 8) or lowMes
                            //赋值旧的设置数据
                            val groupByMeshAddr = if (featureId == 0xff)
                                null
                            else
                                DBUtils.getGroupByMeshAddr(mesAddress)
                            if (groupByMeshAddr != null) {
                                currentGroup = groupByMeshAddr
                                four_group_name.text = currentGroup!!.name
                            } else {
                                four_group_name.text = getString(R.string.select_group)
                            }
                        }
                    }
                }
            }

        } else {
            setTextColorsAndText(2)
        }
    }

    override fun initView() {
        toolbarTv!!.setText(R.string.four_switch)
        img_function1.visibility = View.VISIBLE
        img_function1.setImageResource(R.drawable.icon_change_small)
        toolbar.setNavigationOnClickListener { finishAc() }
        makePop()
    }


    @SuppressLint("SetTextI18n")
    private fun setTextColorsAndText(type: Int) {
        when (type) {
            0 -> {
                four_switch_b1.setTextColor(getColor(R.color.brightness_add_color))
                four_switch_b2.setTextColor(getColor(R.color.brightness_add_color))
                four_switch_b3.setTextColor(getColor(R.color.brightness_add_color))
                four_switch_b4.setTextColor(getColor(R.color.brightness_add_color))
                configSwitchTypeNum = 0
                four_switch_b1.text = "ON\n(亮度调节)"
                four_switch_b2.text = "ON\n(亮度调节)"
                four_switch_b3.text = "(色温调节)\nOFF"
                four_switch_b4.text = "(色温调节)\nOFF"
                four_group_name.visibility = View.GONE
                four_switch_b5.visibility = View.VISIBLE
                four_switch_b6.visibility = View.VISIBLE
                four_switch_b5.text = getString(R.string.click_config)
                four_switch_b6.text = getString(R.string.click_config)
            }
            1 -> {
                four_switch_b1.setTextColor(getColor(R.color.click_config_color))
                four_switch_b2.setTextColor(getColor(R.color.click_config_color))
                four_switch_b3.setTextColor(getColor(R.color.click_config_color))
                four_switch_b4.setTextColor(getColor(R.color.click_config_color))
                configSwitchTypeNum = 1
                four_group_name.visibility = View.GONE
                four_switch_b5.visibility = View.GONE
                four_switch_b6.visibility = View.GONE
                four_switch_b1.text = getString(R.string.click_config)
                four_switch_b2.text = getString(R.string.click_config)
                four_switch_b3.text = getString(R.string.click_config)
                four_switch_b4.text = getString(R.string.click_config)
            }
            2 -> {
                four_switch_b1.setTextColor(getColor(R.color.brightness_add_color))
                four_switch_b2.setTextColor(getColor(R.color.brightness_add_color))
                four_switch_b3.setTextColor(getColor(R.color.brightness_add_color))
                four_switch_b4.setTextColor(getColor(R.color.brightness_add_color))
                four_group_name.visibility = View.VISIBLE
                configSwitchTypeNum = 2
                four_switch_b1.text = "ON"
                four_switch_b2.text = getString(R.string.brightness_add)
                four_switch_b3.text = "OFF"
                four_switch_b4.text = getString(R.string.brightness_minus)
                four_switch_b5.visibility = View.GONE
                four_switch_b6.visibility = View.GONE
            }
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

    override fun onClick(v: View?) {
        var isClickable = true
        when(v?.id) {
            R.id.four_switch_b1 -> {
                isClickable = configSwitchType == 1 //只有场景模式能够点击
                configButtonTag = 0 //用於判斷是點擊的哪一個配置按鈕方便配置對應的藍牙命令
            }
            R.id.four_switch_b2 -> {
                isClickable = configSwitchType == 1
                configButtonTag = 1

            }
            R.id.four_switch_b3 -> {
                isClickable = configSwitchType == 1
                configButtonTag = 2
            }
            R.id.four_switch_b4 -> {
                isClickable = configSwitchType == 1
                configButtonTag = 3
            }
            R.id.four_group_name -> {
                isClickable = false
                val intent = Intent(this, ChooseGroupOrSceneActivity::class.java)
                //传入0代表是群组
                val bundle = Bundle()
                bundle.putInt(Constant.EIGHT_SWITCH_TYPE, 0)//传入0代表是群组
                bundle.putInt(Constant.DEVICE_TYPE, Constant.DEVICE_TYPE_LIGHT_SW.toInt())
                intent.putExtras(bundle)
                startActivityForResult(intent, 33)
            }
            R.id.four_switch_b5 -> {
                isClickable = false
                configButtonTag = 4
                val intent = Intent(this, ChooseGroupOrSceneActivity::class.java)
                //传入0代表是群组
                val bundle = Bundle()
                bundle.putInt(Constant.EIGHT_SWITCH_TYPE, 0)//传入0代表是群组
                bundle.putInt(Constant.DEVICE_TYPE, Constant.DEVICE_TYPE_LIGHT_SW.toInt())
                intent.putExtras(bundle)
                startActivityForResult(intent, requestCodeNum)
            }
            R.id.four_switch_b6 -> {
                isClickable = false
                configButtonTag = 5
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
            bundle.putInt(Constant.EIGHT_SWITCH_TYPE, configSwitchType) //传入0代表是群组，传入1代表的是场景
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
                0, 2 -> {
                    val group = data?.getSerializableExtra(Constant.EIGHT_SWITCH_TYPE) as DbGroup
                    groupMap[configButtonTag-4] = group
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
                0 -> four_switch_b1.text = name
                1 -> four_switch_b2.text = name
                2 -> four_switch_b3.text = name
                3 -> four_switch_b4.text = name
                4 -> four_switch_b5.text = name
                5 -> four_switch_b6.text = name
            }
        } else if(resultCode == Activity.RESULT_OK && requestCode == 33){
            currentGroup = data?.getSerializableExtra(Constant.EIGHT_SWITCH_TYPE) as DbGroup
            LogUtils.v("chown ============== ${currentGroup.toString()}")
            four_group_name.text = currentGroup?.name
        }
    }
}