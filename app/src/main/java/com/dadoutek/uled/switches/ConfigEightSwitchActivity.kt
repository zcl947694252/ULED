package com.dadoutek.uled.switches

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.view.View
import com.blankj.utilcode.util.LogUtils
import com.blankj.utilcode.util.ToastUtils
import com.dadoutek.uled.R
import com.dadoutek.uled.communicate.Commander
import com.dadoutek.uled.gateway.util.GsonUtil
import com.dadoutek.uled.model.Constant
import com.dadoutek.uled.model.DaoSessionInstance
import com.dadoutek.uled.model.dbModel.DBUtils
import com.dadoutek.uled.model.dbModel.DbGroup
import com.dadoutek.uled.model.dbModel.DbScene
import com.dadoutek.uled.model.dbModel.DbSwitch
import com.dadoutek.uled.model.DeviceType
import com.dadoutek.uled.model.Opcode
import com.dadoutek.uled.model.routerModel.RouterModel
import com.dadoutek.uled.router.bean.CmdBodyBean
import com.dadoutek.uled.switches.bean.KeyBean
import com.dadoutek.uled.tellink.TelinkLightService
import com.dadoutek.uled.util.MeshAddressGenerator
import com.dadoutek.uled.util.StringUtils
import com.dadoutek.uled.util.SyncDataPutOrGetUtils
import com.telink.bluetooth.light.DeviceInfo
import io.reactivex.Observable
import kotlinx.android.synthetic.main.eight_switch.*
import kotlinx.android.synthetic.main.toolbar.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit


/**
 * 创建者     ZCL
 * 创建时间   2020/1/10 10:01
 * 描述 八键开关配置
 *乾三连 1开天金
 * 坤六断 8大地土
 * 震仰盂 4东方雷木
 * 艮覆碗 7东北齐山土
 * 离中虚 3南离火
 * 坎中满 6北方水
 * 兑上缺 2西方泽金
 * 巽下断 5东南风木
 *
 * 更新者     $
 * 更新时间   $
 * 更新描述
 */
class ConfigEightSwitchActivity : BaseSwitchActivity(), View.OnClickListener {
    private var newMeshAddr: Int = 0
    private lateinit var listKeysBean: JSONArray
    private var groupName: String? = null
    private var version: String? = null
    private var mDeviceInfo: DeviceInfo? = null
    private var configSwitchType = 0
    private var configSwitchTypeNum = 1
    private var configButtonTag = 0
    private val requestCodeNum = 100
    private var clickType = 0
    private val groupMap = mutableMapOf<Int, DbGroup>()
    private val groupParamList = mutableListOf<ByteArray>()
    private val sceneMap = mutableMapOf<Int, DbScene>()
    private val sceneParamList = mutableListOf<ByteArray>()


    override fun initData() {
        val groupKey = mutableListOf(4, 5, 6, 7)
        val sceneKey = mutableListOf(0, 1, 2, 3, 4, 5, 6)
        val doubleGroupKey = mutableListOf(2, 3, 4, 5, 6, 7)
        //先进行填充默认数据
        setDefaultData()
        toolbarTv.text = getString(R.string.eight_switch)

        //重新赋值新数据
        mDeviceInfo = intent.getParcelableExtra("deviceInfo")
        version = intent.getStringExtra("version")
        setVersion()
        groupName = intent.getStringExtra("group")
        // eight_switch_mode.visibility = View.GONE
        // eight_switch_config.visibility = View.GONE
        // eight_switch_banner_ly.visibility = View.VISIBLE
        isReConfig = groupName != null && groupName == "true"
        fiRename?.isVisible = isReConfig

        if (isReConfig) {
            switchDate = this.intent.extras!!.get("switch") as DbSwitch
            toolbarTv.text = switchDate?.name
            switchDate?.keys?.let {
                listKeysBean = JSONArray(it)
                toolbarTv.text = switchDate?.name
                //eight_switch_mode.visibility = View.VISIBLE
                //eight_switch_config.visibility = View.VISIBLE
                // eight_switch_banner_ly.visibility = View.GONE

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

                            var mesAddress = (highMes shl 8) or lowMes
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
                                4 -> eight_switch_b5.text = name
                                5 -> eight_switch_b6.text = name
                                6 -> eight_switch_b7.text = name
                                7 -> eight_switch_b8.text = name
                            }
                            eight_switch_title.text = getString(R.string.group_switch)
                        }
                        1 -> {
                            val sceneId = jOb.getInt("reserveValue_B")
                            var scene = DBUtils.getSceneByID(sceneId.toLong())
                            eight_switch_title.text = getString(R.string.scene_switch)
                            //赋值旧的设置数据
                            sceneMap[keyId] = if (scene != null) {
                                name = if (scene.name == "")
                                    getString(R.string.click_config)
                                else
                                    scene.name
                                scene
                            } else {
                                var dbScene = DbScene()
                                dbScene.id = 65536L
                                name = getString(R.string.click_config)
                                dbScene
                            }
                            sceneKey.remove(keyId)
                            when (keyId) {
                                0 -> eight_switch_b1.text = name
                                1 -> eight_switch_b2.text = name
                                2 -> eight_switch_b3.text = name
                                3 -> eight_switch_b4.text = name
                                4 -> eight_switch_b5.text = name
                                5 -> eight_switch_b6.text = name
                                6 -> eight_switch_b7.text = name
                            }
                        }
                        2 -> {
                            val highMes = jOb.getInt("reserveValue_A")
                            val lowMes = jOb.getInt("reserveValue_B")
                            eight_switch_title.text = getString(R.string.single_brighress_group_switch)
                            var mesAddress = (highMes shl 8) or lowMes
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
                                doubleGroupKey.remove(keyId)
                            } else {
                                name = getString(R.string.click_config)
                            }
                            when (keyId) {
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
            }
        }
    }

    private fun setDefaultData() {
        groupMap.clear()
        sceneMap.clear()
        for (i in 0 until 8) {
            var dbGroup = DbGroup()
            dbGroup.id = 65536L
            groupMap[i] = dbGroup

            var dbScene = DbScene()
            dbScene.id = 65536L
            sceneMap[i] = dbScene
        }
    }


    private fun confimCongfig() {
        //成功后clickType = 0
        when (configSwitchType) {
            0 -> sendParms()
            1 -> sendSceneParms()
            2 -> sendSingleGroupParms()
        }
    }

    private fun sendSingleGroupParms() {
        showLoadingDialog(getString(R.string.setting_switch))
        groupParamList.clear()
        listKeysBean = JSONArray()

        //11-12-13-14 11-12-13-14
        var firstParm = byteArrayOf(0x00, Opcode.GROUP_BRIGHTNESS_MINUS, 0x00, 0x00, 0x01, Opcode.GROUP_BRIGHTNESS_ADD, 0x00, 0x00)
        listKeysBean.put(getKeyBean(0x00, Opcode.GROUP_BRIGHTNESS_MINUS.toInt()))
        listKeysBean.put(getKeyBean(0x01, Opcode.GROUP_BRIGHTNESS_ADD.toInt()))


        val second = mutableListOf(2, 3)
        val third = mutableListOf(4, 5)
        val four: MutableList<Int> = mutableListOf(6, 7)
        /* val four: MutableList<Int> = if (groupMap.size > 3)+
             mutableListOf(6, 7)
         else
             mutableListOf(6)*/

        val secondParm = getGroupParm(second)
        val thirParm = getGroupParm(third)
        val fourParm = getGroupParm(four)

        LogUtils.v("zcl獲得的keys是$listKeysBean")
        groupParamList.add(0, firstParm)
        groupParamList.add(1, secondParm)
        groupParamList.add(2, thirParm)
        groupParamList.add(3, fourParm)
        if (!Constant.IS_ROUTE_MODE){
            GlobalScope.launch {
                var delay = 1000L
                for (p in groupParamList) {
                    delay(delay)
                    //从第八位开始opcode, 设备meshAddr  参数11-12-13-14 15-16-17-18
                    //p = byteArrayOf(0x02, Opcode.GROUP_BRIGHTNESS_MINUS, 0x00, 0x00, 0x03, Opcode.GROUP_CCT_MINUS, 0x00, 0x00)
                    TelinkLightService.Instance().sendCommandNoResponse(Opcode.CONFIG_SCENE_SWITCH, mDeviceInfo?.meshAddress ?: 0, p)
                    delay += 300
                }
                delay(1500)
                updateMeshGroup(2)
            }
        }else{
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
        //val four = mutableListOf(6, 7)

        val four: MutableList<Int> = if (sceneMap[7]?.id != 65536L)
            mutableListOf(6, 7)
        else
            mutableListOf(6)
        val sceneParmOne = getSceneParm(first)
        val sceneParmTwo = getSceneParm(second)
        val sceneParmThird = getSceneParm(third)
        val sceneParmFour = getSceneParm(four)
        sceneParamList.add(sceneParmOne)
        sceneParamList.add(sceneParmTwo)
        sceneParamList.add(sceneParmThird)
        sceneParamList.add(sceneParmFour)
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
            routerConfigEightSw(mDeviceInfo?.id?.toLong() ?: 0L)
        }
    }

    private fun sendParms() {
        groupParamList.clear()
        listKeysBean = JSONArray()

        //11-12-13-14 11-12-13-14
        var firstParm = byteArrayOf(0x00, Opcode.GROUP_BRIGHTNESS_ADD, 0x00, 0x00, 0x01, Opcode.GROUP_CCT_ADD, 0x00, 0x00)
        var secondParm = byteArrayOf(0x02, Opcode.GROUP_BRIGHTNESS_MINUS, 0x00, 0x00, 0x03, Opcode.GROUP_CCT_MINUS, 0x00, 0x00)
        listKeysBean.put(getKeyBean(0x00, Opcode.GROUP_BRIGHTNESS_ADD.toInt()))
        listKeysBean.put(getKeyBean(0x01, Opcode.GROUP_CCT_ADD.toInt()))
        listKeysBean.put(getKeyBean(0x02, Opcode.GROUP_BRIGHTNESS_MINUS.toInt()))
        listKeysBean.put(getKeyBean(0x03, Opcode.GROUP_CCT_MINUS.toInt()))

        val third = mutableListOf(4, 5)
        val four: MutableList<Int> = mutableListOf(6, 7)
        /*  val four: MutableList<Int> = if (groupMap.size > 3)
              mutableListOf(6, 7)
          else
              mutableListOf(6)*/

        val thirParm = getGroupParm(third)
        val fourParm = getGroupParm(four)

        LogUtils.v("zcl獲得的keys是$listKeysBean")
        groupParamList.add(0, firstParm)
        groupParamList.add(1, secondParm)
        groupParamList.add(2, thirParm)
        groupParamList.add(3, fourParm)
        if (!Constant.IS_ROUTE_MODE) {
            showLoadingDialog(getString(R.string.setting_switch))
            GlobalScope.launch {
                var delay = 1000.toLong()
                for (p in groupParamList) {
                    delay(delay)
                    //从第八位开始opcode, 设备meshAddr  参数11-12-13-14 15-16-17-18
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

    private fun getKeyBean(keyId: Int, featureId: Int, name: String = "", hight8Mes: Int = 0, low8Mes: Int = 0): JSONObject {
        //return JSONObject(["keyId" = keyId, "featureId" = featureId, "reserveValue_A" = hight8Mes, "reserveValue_B" = low8Mes, "name" = name])
        //["keyId" = 11, "featureId" =11, "reserveValue_A" = 0x11, "reserveValue_B" = 0x11, "name" = name])
        var job = JSONObject()
        job.put("keyId", keyId)
        job.put("featureId", featureId)
        job.put("reserveValue_A", hight8Mes)
        job.put("reserveValue_B", low8Mes)
        job.put("name", name)
        val keyBean = KeyBean(keyId, featureId, name, hight8Mes, low8Mes);
        return job
    }

    private fun updateMeshGroup(isConfigGroup: Int) {
        newMeshAddr = if (isReConfig) mDeviceInfo?.meshAddress ?: 0 else MeshAddressGenerator().meshAddress.get()
        LogUtils.v("zcl-----------更新开关新mesh-------${newMeshAddr}")
        Commander.updateMeshName(newMeshAddr = newMeshAddr, successCallback = {
            mDeviceInfo?.meshAddress = newMeshAddr

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

    private fun updateSwitch(configGroup: Int) {
        if (groupName == "false") {
            var dbEightSwitch = DBUtils.getSwitchByMacAddr(mDeviceInfo?.macAddress ?: "")
            if (dbEightSwitch != null) {
                dbEightSwitch.name = getString(R.string.eight_switch) + "-" + (mDeviceInfo?.meshAddress ?: 0)
                dbEightSwitch.meshAddr = newMeshAddr
                dbEightSwitch.type = configGroup
                dbEightSwitch = setGroupIdsOrSceneIds(configGroup == 0, dbEightSwitch)
                dbEightSwitch.keys = listKeysBean.toString()
                if (TextUtils.isEmpty(version))
                    version = mDeviceInfo!!.firmwareRevision
                dbEightSwitch.version = version
                DBUtils.updateSwicth(dbEightSwitch)
                switchDate = dbEightSwitch
            } else {
                var eightSwitch = DbSwitch()
                DBUtils.saveSwitch(eightSwitch, isFromServer = false, type = eightSwitch.type, keys = eightSwitch.keys)
                eightSwitch = setGroupIdsOrSceneIds(configGroup == 0, eightSwitch)
                eightSwitch.type = configGroup
                eightSwitch.macAddr = mDeviceInfo?.macAddress
                eightSwitch.meshAddr = mDeviceInfo?.meshAddress ?: 0
                eightSwitch.productUUID = mDeviceInfo?.productUUID ?: 0
                eightSwitch.index = eightSwitch.id.toInt()
                if (TextUtils.isEmpty(version))
                    version = mDeviceInfo!!.firmwareRevision
                eightSwitch.version = version

                eightSwitch.keys = listKeysBean.toString()

                Log.e("zcl", "zcl*****设置新的开关使用插入替换$eightSwitch")
                DBUtils.saveSwitch(eightSwitch, isFromServer = false, type = eightSwitch.type, keys = eightSwitch.keys)

                LogUtils.v("zcl", "zcl*****设置新的开关使用插入替换" + DBUtils.getAllSwitch())
                val gotSwitchByMac = DBUtils.getSwitchByMacAddr(mDeviceInfo?.macAddress ?: "")
                DBUtils.recordingChange(gotSwitchByMac?.id, DaoSessionInstance.getInstance().dbSwitchDao.tablename,
                        Constant.DB_ADD, eightSwitch.type, eightSwitch.keys)
                switchDate = eightSwitch
            }
        } else {
            switchDate!!.type = configGroup
            switchDate!!.keys = listKeysBean.toString()
            switchDate?.meshAddr = mDeviceInfo?.meshAddress ?: 0
            //解析出來他的keys 重新賦值
            DBUtils.updateSwicth(switchDate!!)
        }
    }

    private fun setGroupIdsOrSceneIds(configGroup: Boolean, dbEightSwitch: DbSwitch): DbSwitch {
        if (configGroup) {
            var groupIds = ""
            groupMap.forEach {
                groupIds = groupIds + it.value.id + ","
            }
            dbEightSwitch.groupIds = groupIds
        } else {
            var sceneIds = ""
            sceneMap.forEach {
                sceneIds = sceneIds + it.value.id + ","
            }
            dbEightSwitch.sceneIds = sceneIds
        }
        return dbEightSwitch
    }

    private fun getSceneParm(list: MutableList<Int>): ByteArray {
        var firstOpcode = Opcode.SCENE_SWITCH8K
        var secondOpcode = Opcode.SCENE_SWITCH8K

        val firstNum = list[0]//4
        val dbSceneFirst = sceneMap[firstNum]
        val firsDbSceneId = if (dbSceneFirst == null || dbSceneFirst.id == 65536L) {
            firstOpcode = Opcode.DEFAULT_SWITCH8K
            listKeysBean.put(getKeyBean(firstNum, firstOpcode.toInt() and 0xff, name = getString(R.string.click_config), hight8Mes = 0, low8Mes = 0))
            65536L
        } else {
            listKeysBean.put(getKeyBean(firstNum, firstOpcode.toInt() and 0xff, name = sceneMap[firstNum]!!.name, hight8Mes = 0, low8Mes = dbSceneFirst!!.id.toInt()))
            dbSceneFirst!!.id
        }

        return if (list.size > 1) {//配置双场景数据
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
            byteArrayOf(firstNum.toByte(), firstOpcode, 0x00, firsDbSceneId.toByte(), secondNum.toByte(), secondOpcode, 0x00, secondDbSceneId.toByte())
        } else {//如果第八键没有配置默认为关  0-1-2 3id 4 5 6 7id
            listKeysBean.put(getKeyBean(7, Opcode.CLOSE.toInt() and 0xff, name = getString(R.string.close), hight8Mes = 0, low8Mes = 0xff))
            byteArrayOf(firstNum.toByte(), firstOpcode, 0x00, firsDbSceneId.toByte(), 0x07, Opcode.CLOSE, 0x00, 0x00)
        }
    }

    private fun getGroupParm(list: MutableList<Int>): ByteArray {
        val firstNum = list[0]
        val dbGroup1 = groupMap[firstNum]!!
        var opcodeOne: Byte
        var fristL: Byte = 0
        var fristH: Byte = 0

        if (dbGroup1 == null || dbGroup1.id == 65536L) {
            opcodeOne = Opcode.DEFAULT_SWITCH8K
            listKeysBean.put(getKeyBean(firstNum, opcodeOne.toInt() and 0xff, name = getString(R.string.click_config), hight8Mes = 0, low8Mes = 0))
        } else {
            val fristMesAddr = dbGroup1.meshAddr
            val mesL = fristMesAddr and 0xff
            val mesH = (fristMesAddr shr 8) and 0xff
            fristL = mesL.toByte()
            fristH = mesH.toByte()
            opcodeOne = getAllGroupOpcode(fristMesAddr)
            listKeysBean.put(getKeyBean(firstNum, opcodeOne.toInt() and 0xff, name = groupMap[firstNum]!!.name, hight8Mes = mesH, low8Mes = mesL))
        }

        return if (list.size > 1) {
            val secondNum = list[1]
            val dbGroup2 = groupMap[secondNum]

            var opcodeTwo: Byte
            var secondL: Byte = 0
            var secondH: Byte = 0

            if (dbGroup2 == null || dbGroup2.id == 65536L) {
                opcodeTwo = Opcode.DEFAULT_SWITCH8K
                listKeysBean.put(getKeyBean(secondNum, opcodeTwo.toInt() and 0xff, name = getString(R.string.click_config), hight8Mes = 0, low8Mes = 0))
            } else {
                val secondMesAddr = dbGroup2.meshAddr
                val mesL = secondMesAddr and 0xff
                val mesH = (secondMesAddr shr 8) and 0xff
                secondL = mesL.toByte()
                secondH = mesH.toByte()
                opcodeTwo = getAllGroupOpcode(secondMesAddr)
                listKeysBean.put(getKeyBean(secondNum, opcodeTwo.toInt() and 0xff, name = groupMap[secondNum]!!.name, hight8Mes = mesH, low8Mes = mesL))
            }

            byteArrayOf(firstNum.toByte(), opcodeOne, fristH, fristL, secondNum.toByte(), opcodeTwo, secondH, secondL)
        } else {
            //如果第八键没有配置默认为关
            listKeysBean.put(getKeyBean(0x08, Opcode.CLOSE.toInt() and 0xff, name = getString(R.string.close), hight8Mes = 0, low8Mes = 255))
            byteArrayOf(firstNum.toByte(), opcodeOne, fristH, fristL, 0x07, Opcode.CLOSE, 0x00, 0x00)
        }
    }

    private fun getAllGroupOpcode(fiveMeshs: Int): Byte {
        return if (fiveMeshs != 0xffff)
            Opcode.GROUP_SWITCH8K
        else
            Opcode.SWITCH_ALL_GROUP
    }

    /**
     * 创建pop并添加按钮监听
     */
    @SuppressLint("SetTextI18n")
    private fun setTextColorsAndText(type: Int) {
        when (type) {
            0 -> {
                eight_switch_b1.setTextColor(getColor(R.color.brightness_add_color))
                eight_switch_b2.setTextColor(getColor(R.color.brightness_add_color))
                eight_switch_b3.setTextColor(getColor(R.color.brightness_add_color))
                eight_switch_b4.setTextColor(getColor(R.color.brightness_add_color))
                eight_switch_b8.setTextColor(getColor(R.color.click_config_color))
                configSwitchTypeNum = 1
                eight_switch_b1.text = getString(R.string.brightness_add)
                eight_switch_b2.text = getString(R.string.color_temperature_add)
                eight_switch_b3.text = getString(R.string.brightness_minus)
                eight_switch_b4.text = getString(R.string.color_temperature_minus)
                eight_switch_b5.text = getString(R.string.click_config)
                eight_switch_b6.text = getString(R.string.click_config)
                eight_switch_b7.text = getString(R.string.click_config)
                eight_switch_b8.text = getString(R.string.click_config)

            }
            1 -> {
                eight_switch_b1.setTextColor(getColor(R.color.click_config_color))
                eight_switch_b2.setTextColor(getColor(R.color.click_config_color))
                eight_switch_b3.setTextColor(getColor(R.color.click_config_color))
                eight_switch_b4.setTextColor(getColor(R.color.click_config_color))
                eight_switch_b8.setTextColor(getColor(R.color.brightness_add_color))
                configSwitchTypeNum = 2
                eight_switch_b1.text = getString(R.string.click_config)
                eight_switch_b2.text = getString(R.string.click_config)
                eight_switch_b3.text = getString(R.string.click_config)
                eight_switch_b4.text = getString(R.string.click_config)
                eight_switch_b5.text = getString(R.string.click_config)
                eight_switch_b6.text = getString(R.string.click_config)
                eight_switch_b7.text = getString(R.string.click_config)
                eight_switch_b8.text = getString(R.string.close)
            }
            2 -> {
                eight_switch_b1.setTextColor(getColor(R.color.brightness_add_color))
                eight_switch_b2.setTextColor(getColor(R.color.brightness_add_color))
                eight_switch_b3.setTextColor(getColor(R.color.click_config_color))
                eight_switch_b4.setTextColor(getColor(R.color.click_config_color))
                eight_switch_b8.setTextColor(getColor(R.color.click_config_color))
                configSwitchTypeNum = 3
                eight_switch_b1.text = getString(R.string.brightness_minus)
                eight_switch_b2.text = getString(R.string.brightness_add)
                eight_switch_b3.text = getString(R.string.click_config)
                eight_switch_b4.text = getString(R.string.click_config)
                eight_switch_b5.text = getString(R.string.click_config)
                eight_switch_b6.text = getString(R.string.click_config)
                eight_switch_b7.text = getString(R.string.click_config)
                eight_switch_b8.text = getString(R.string.click_config)
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (resultCode == Activity.RESULT_OK && requestCode == requestCodeNum) {
            var name: String = ""
            when (configSwitchType) {
                0, 2 -> {
                    var group = data?.getSerializableExtra(Constant.EIGHT_SWITCH_TYPE) as DbGroup
                    groupMap[configButtonTag] = group
                    name = group.name
                }
                else -> {
                    val scene = data?.getParcelableExtra(Constant.EIGHT_SWITCH_TYPE) as DbScene
                    //var scene = data?.getParcelableExtra(Constant.EIGHT_SWITCH_TYPE) as DbScene
                    scene?.let {
                        sceneMap[configButtonTag] = it
                        name = it.name
                    }
                }
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
        if (mDeviceInfo != null)
            deleteSwitch(mDeviceInfo!!.macAddress)
        else
            ToastUtils.showShort(getString(R.string.invalid_data))
    }

    override fun goOta() {
        if (mDeviceInfo != null)
            deviceOta(mDeviceInfo!!, DeviceType.EIGHT_SWITCH)
        else
            ToastUtils.showShort(getString(R.string.invalid_data))
    }


    override fun reName() {
        showRenameDialog(switchDate, false)
    }

    override fun setLayoutId(): Int {
        return R.layout.eight_switch
    }

    override fun initView() {
        toolbarTv!!.setText(R.string.eight_switch)
        img_function1.visibility = View.VISIBLE
        img_function1.setImageResource(R.drawable.icon_change_small)
        toolbar.setNavigationOnClickListener { finishAc() }
        makePop()
    }

    override fun setToolBar(): android.support.v7.widget.Toolbar {
        return toolbar
    }

    override fun setReConfig(): Boolean {
        return isReConfig
    }

    override fun initListener() {
        toolbar.setOnMenuItemClickListener(menuItemClickListener)
        img_function1.setOnClickListener {
            changeMode()
        }
        eight_switch_use_button.setOnClickListener {
            /*   when (clickType) {
                   0 -> {//选择模式 显示配置界面
                       setTextColorsAndText(configSwitchType)
                       // eight_switch_mode.visibility = View.VISIBLE
                       //eight_switch_config.visibility = View.VISIBLE
                       // eight_switch_banner_ly.visibility = View.GONE
                       clickType = 1
                   }
                   1, 2 -> {
                       clickType = 2
                       confimCongfig()
                   }
               }*/
            confimCongfig()
        }

        img_function2.setOnClickListener {//清除数据并且清除模式
            setDefaultData()
            //eight_switch_mode.visibility = View.GONE
            // eight_switch_config.visibility = View.GONE
            //eight_switch_banner_ly.visibility = View.GONE
            //clickType = 0//代表没有选择模式
            configSwitchType++ //默认选中的是群组八键开关
            when (configSwitchType % 3) {
                1 -> eight_switch_title.text = getString(R.string.group_switch)
                2 -> eight_switch_title.text = getString(R.string.scene_switch)
                0 -> eight_switch_title.text = getString(R.string.single_brighress_group_switch)
            }
            eight_switch_title.text = getString(R.string.group_switch)
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

    private fun changeMode() {
        configSwitchTypeNum++
        setDefaultData()
        when (configSwitchTypeNum % 3) {
            1 -> {
                setTextColorsAndText(0)
                configSwitchType = 0
                eight_switch_title.text = getString(R.string.group_switch)
            }
            2 -> {
                configSwitchType = 1
                setTextColorsAndText(1)
                eight_switch_title.text = getString(R.string.scene_switch)
            }
            0 -> {
                configSwitchType = 2
                setTextColorsAndText(2)
                eight_switch_title.text = getString(R.string.single_brighress_group_switch)
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
                    renameDialog?.dismiss()
                LogUtils.v("zcl改名后-----------${DBUtils.getSwitchByMeshAddr(mDeviceInfo?.meshAddress ?: 0)?.name}")
            }
        }
        renameCancel?.setOnClickListener {
            if (this != null && !this.isFinishing)
                renameDialog?.dismiss()
        }
        renameDialog?.setOnDismissListener {
            if (!isReConfig)
                finishAc()
        }
    }


    override fun routerRenameSwSuccess(trim: String) {
        renameSw(trim = trim)
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

    @SuppressLint("CheckResult")
    private fun routerConfigEightSw(id: Long) {
        val keys = GsonUtil.stringToList(listKeysBean.toString(), KeyBean::class.java)
        RouterModel.configEightSw(id, keys, "configEightSw")?.subscribe({
            LogUtils.v("zcl-----------收到路由配置八键请求-------$it")
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
                    ToastUtils.showShort(getString(R.string.device_not_exit))
                    SyncDataPutOrGetUtils.syncGetDataStart(DBUtils.lastUser!!, syncCallbackGet)
                    finish()
                }
                90008 -> ToastUtils.showShort(getString(R.string.no_bind_router_cant_perform))
                90007 -> ToastUtils.showShort(getString(R.string.gp_not_exit))
                90005 -> ToastUtils.showShort(getString(R.string.router_offline))
                90004 -> ToastUtils.showShort(getString(R.string.region_no_router))
                else-> ToastUtils.showShort(it.message)
            }
        }, {
            ToastUtils.showShort(it.message)
        })
    }

    override fun tzRouterConfigEightSwRecevice(cmdBean: CmdBodyBean) {
        LogUtils.v("zcl-----------收到路由配置八键通知-------$cmdBean")
        if (cmdBean.ser_id == "configEightSw") {
            disposableRouteTimer?.dispose()
            hideLoadingDialog()
            if (cmdBean.status == 0) {
                GlobalScope.launch(Dispatchers.Main) {
                    ToastUtils.showShort(getString(R.string.config_success))
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

    private fun finishAc() {
        TelinkLightService.Instance()?.idleMode(true)
        TelinkLightService.Instance()?.disconnect()
        finish()
    }

    override fun onClick(v: View?) {
        var isCanClick = true
        when (v?.id) {
            R.id.eight_switch_b1 -> {
                isCanClick = configSwitchType == 1//前四个按钮不是场景开关不允许点击
                configButtonTag = 0//用於判斷是點擊的哪一個配置按鈕方便配置對應的藍牙命令
            }
            R.id.eight_switch_b2 -> {
                isCanClick = configSwitchType == 1
                configButtonTag = 1
            }
            R.id.eight_switch_b3 -> {
                isCanClick = configSwitchType == 1 || configSwitchType == 2
                configButtonTag = 2
            }
            R.id.eight_switch_b4 -> {
                isCanClick = configSwitchType == 1 || configSwitchType == 2
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
                isCanClick = configSwitchType == 0 || configSwitchType == 2//是群组开关才可以点击配置 场景开关为关不允许配置
                configButtonTag = 7
            }
        }
        if (isCanClick) {
            /* if (configSwitchType == 1){
                 startActivityForResult(Intent(this@GwTimerPeriodListActivity, SelectSceneListActivity::class.java), requestCodes)
             }else{*/
            val intent = Intent(this@ConfigEightSwitchActivity, ChooseGroupOrSceneActivity::class.java)
            val bundle = Bundle()
            bundle.putInt(Constant.EIGHT_SWITCH_TYPE, configSwitchType)//传入0代表是群组
            bundle.putInt(Constant.DEVICE_TYPE, Constant.DEVICE_TYPE_LIGHT_SW.toInt())
            intent.putExtras(bundle)
            startActivityForResult(intent, requestCodeNum)
            //}
        }
    }
}