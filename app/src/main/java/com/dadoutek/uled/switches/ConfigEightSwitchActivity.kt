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
import com.dadoutek.uled.model.Constant
import com.dadoutek.uled.model.DaoSessionInstance
import com.dadoutek.uled.model.dbModel.DBUtils
import com.dadoutek.uled.model.dbModel.DbGroup
import com.dadoutek.uled.model.dbModel.DbScene
import com.dadoutek.uled.model.dbModel.DbSwitch
import com.dadoutek.uled.model.DeviceType
import com.dadoutek.uled.model.Opcode
import com.dadoutek.uled.tellink.TelinkLightService
import com.dadoutek.uled.util.MeshAddressGenerator
import com.dadoutek.uled.util.StringUtils
import com.telink.bluetooth.light.DeviceInfo
import kotlinx.android.synthetic.main.eight_switch.*
import kotlinx.android.synthetic.main.toolbar.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject


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
    private lateinit var listKeysBean: JSONArray
    private var newMeshAddr: Int = 0
    private var switchData: DbSwitch? = null
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
            switchData = this.intent.extras!!.get("switch") as DbSwitch
            toolbarTv.text = switchData?.name
            switchData?.keys?.let {
                listKeysBean = JSONArray(it)
                toolbarTv.text = switchData?.name
                //eight_switch_mode.visibility = View.VISIBLE
                //eight_switch_config.visibility = View.VISIBLE
                // eight_switch_banner_ly.visibility = View.GONE

                val type = switchData!!.type
                configSwitchType = type//赋值选择的模式
                clickType = 1//代表跳过选择模式

                setTextColorsAndText(type)

                for (i in 0 until listKeysBean.length()) {
                    //int keyId;  int featureId;   int reserveValue_A;  int reserveValue_B;  String name;
                    val jOb = listKeysBean.getJSONObject(i)
                    val keyId = jOb.getInt("keyId")
                    val name = jOb.getString("name")
                    when (type) {
                        0 -> {
                            val highMes = jOb.getInt("reserveValue_A")
                            val lowMes = jOb.getInt("reserveValue_B")

                            var mesAddress = (highMes shl 8) or lowMes
                            //赋值旧的设置数据
                            val groupByMeshAddr = DBUtils.getGroupByMeshAddr(mesAddress)

                            if (groupByMeshAddr != null) {
                                groupMap[keyId] = groupByMeshAddr
                                groupKey.remove(keyId)
                            }
                            when (keyId) {
                                4 -> eight_switch_b5.text = name
                                5 -> eight_switch_b6.text = name
                                6 -> eight_switch_b7.text = name
                                7 -> eight_switch_b8.text = name
                            }
                        }
                        1 -> {
                            val sceneId = jOb.getInt("reserveValue_B")
                            var scene = DBUtils.getSceneByID(sceneId.toLong())
                            //赋值旧的设置数据
                            sceneMap[keyId] = if (scene != null) scene else {
                                var dbScene = DbScene()
                                dbScene.id = 1000000L
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
                            var mesAddress = (highMes shl 8) or lowMes
                            //赋值旧的设置数据
                            val groupByMeshAddr = DBUtils.getGroupByMeshAddr(mesAddress)
                            if (groupByMeshAddr != null) {
                                groupMap[keyId] = groupByMeshAddr
                                doubleGroupKey.remove(keyId)
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
                /* when (type) {
                     0 -> {
                         groupKey.forEach { itKey ->
                             var dbGroup = DbGroup()
                             dbGroup.id = 1000000L
                             groupMap[itKey] = dbGroup
                         }
                     }
                     1 -> {
                         sceneKey.forEach { itKey ->
                             var dbScene = DbScene()
                             dbScene.id = 1000000L
                             sceneMap[itKey] = dbScene
                         }
                     }
                     2 -> {
                         doubleGroupKey.forEach { itKey ->
                             var dbGroup = DbGroup()
                             dbGroup.id = 1000000L
                             groupMap[itKey] = dbGroup
                         }
                     }
                     else -> {
                     }
                 }*/
            }
        }
    }

    private fun setDefaultData() {
        groupMap.clear()
        sceneMap.clear()
        for (i in 0 until 8) {
            var dbGroup = DbGroup()
            dbGroup.id = 1000000L
            groupMap[i] = dbGroup

            var dbScene = DbScene()
            dbScene.id = 1000000L
            sceneMap[i] = dbScene
        }
    }


    private fun confimCongfig() {
        //成功后clickType = 0
        when (configSwitchType) {
            0 -> {
                // if (groupMap.size >= 4 && groupMap.containsKey(5) && groupMap.containsKey(6) && groupMap.containsKey(7) && groupMap.containsKey(4)) {
                sendParms()
                /*  } else{
                      clickType = 1
                      ToastUtils.showLong(getString(R.string.click_config_tip))
                  }*/
            }
            1 -> {
                /*if (sceneMap.size >= 7 && sceneMap.containsKey(1) && sceneMap.containsKey(2) &&
                        sceneMap.containsKey(3) && sceneMap.containsKey(4) && sceneMap.containsKey(5)
                        && sceneMap.containsKey(6) && sceneMap.containsKey(0)) {*/
                sendSceneParms()
                /* } else{
                     clickType = 1
                     ToastUtils.showLong(getString(R.string.click_config_tip))
                 }*/
            }
            2 -> {
                /* if (groupMap.size >= 6 && groupMap.containsKey(2) && groupMap.containsKey(3) && groupMap.containsKey(4) && groupMap.containsKey(5)
                         && groupMap.containsKey(6) && groupMap.containsKey(7)) {*/
                sendSingleGroupParms()
                /* } else {
                     clickType = 1
                     ToastUtils.showLong(getString(R.string.click_config_tip))
                 }*/
            }
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

        GlobalScope.launch {
            var delay = 0L
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
    }

    private fun sendSceneParms() {
        showLoadingDialog(getString(R.string.setting_switch))
        sceneParamList.clear()
        listKeysBean = JSONArray()

        val first = mutableListOf(0, 1)
        val second = mutableListOf(2, 3)
        val third = mutableListOf(4, 5)
        //val four = mutableListOf(6, 7)
        val four: MutableList<Int> = if (sceneParamList.size > 7)
            mutableListOf(6/*, 7*/)
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
        var delay = 0L
        GlobalScope.launch {
            for (p in sceneParamList) {
                delay(delay)
                TelinkLightService.Instance().sendCommandNoResponse(Opcode.CONFIG_SCENE_SWITCH, mDeviceInfo?.meshAddress ?: 0, p)
                delay += 300
            }
            delay(1500)
            updateMeshGroup(1)
        }
    }

    private fun sendParms() {
        showLoadingDialog(getString(R.string.setting_switch))
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

        GlobalScope.launch {
            var delay = 0L
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
    }

    private fun getKeyBean(keyId: Int, featureId: Int, name: String = "", hight8Mes: Int = 0, low8Mes: Int = 0): JSONObject {
        //return JSONObject(["keyId" = keyId, "featureId" = featureId, "reserveValue_A" = hight8Mes, "reserveValue_B" = low8Mes, "name" = name])
        var job = JSONObject()
        job.put("keyId", keyId)
        job.put("featureId", featureId)
        job.put("reserveValue_A", hight8Mes)
        job.put("reserveValue_B", low8Mes)
        job.put("name", name)
        return job
    }

    private fun updateMeshGroup(isConfigGroup: Int) {
        newMeshAddr = MeshAddressGenerator().meshAddress.get()
        Commander.updateMeshName(newMeshAddr = newMeshAddr, successCallback = {
            mDeviceInfo?.meshAddress = newMeshAddr

            updateSwitch(isConfigGroup)
            GlobalScope.launch(Dispatchers.Main) {
                ToastUtils.showShort(getString(R.string.config_success))
                if (!isReConfig)
                    showRenameDialog(switchData)
                else
                    finishAc()
                hideLoadingDialog()
            }

        }, failedCallback = {
            GlobalScope.launch(Dispatchers.Main) {
                hideLoadingDialog()
                ToastUtils.showShort(getString(R.string.pace_fail))
                delay(1500)
                finishAc()
            }
        })
    }

    private fun updateSwitch(configGroup: Int) {
        if (groupName == "false") {
            var dbEightSwitch = DBUtils.getSwitchByMacAddr(mDeviceInfo?.macAddress ?: "")
            if (dbEightSwitch != null) {
                dbEightSwitch.name = getString(R.string.eight_switch) + "-" + dbEightSwitch.meshAddr
                dbEightSwitch.type = configGroup
                dbEightSwitch = setGroupIdsOrSceneIds(configGroup == 0, dbEightSwitch)
                dbEightSwitch.keys = listKeysBean.toString()
                if (TextUtils.isEmpty(version))
                    version = mDeviceInfo!!.firmwareRevision
                dbEightSwitch.version = version
                DBUtils.updateSwicth(dbEightSwitch)
                switchData = dbEightSwitch
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
                switchData = eightSwitch
            }
        } else {
            switchData!!.type = configGroup
            switchData!!.keys = listKeysBean.toString()
            //解析出來他的keys 重新賦值
            DBUtils.updateSwicth(switchData!!)
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

        val firstNum = list[0]
        val dbSceneFirst = sceneMap[firstNum]
        val firsDbSceneId = if (dbSceneFirst == null || dbSceneFirst.id == 1000000L) {
            firstOpcode = Opcode.DEFAULT_SWITCH8K
            listKeysBean.put(getKeyBean(firstNum, firstOpcode.toInt(), name = getString(R.string.click_config), hight8Mes = 0, low8Mes = 0))
            1000000L
        } else {
            listKeysBean.put(getKeyBean(firstNum, firstOpcode.toInt(), name = sceneMap[firstNum]!!.name, hight8Mes = 0, low8Mes = dbSceneFirst!!.id.toInt()))
            dbSceneFirst!!.id
        }

        return if (list.size > 1) {
            val secondNum = list[1]
            val dbSceneSecond = sceneMap[secondNum]
            //位置 功能 保留 14场景id
            val secondDbSceneId = if (dbSceneSecond == null || dbSceneSecond.id == 1000000L) {
                secondOpcode = Opcode.DEFAULT_SWITCH8K
                listKeysBean.put(getKeyBean(secondNum, secondOpcode.toInt(), name = getString(R.string.click_config), hight8Mes = 0, low8Mes = firsDbSceneId.toInt()))
                1000000L
            } else {
                listKeysBean.put(getKeyBean(secondNum, secondOpcode.toInt(), name = dbSceneSecond.name, hight8Mes = 0, low8Mes = firsDbSceneId.toInt()))
                dbSceneSecond.id
            }
            byteArrayOf(firstNum.toByte(), firstOpcode, 0x00, firsDbSceneId.toByte(), secondNum.toByte(), secondOpcode, 0x00, secondDbSceneId.toByte())
        } else {//如果第八键没有配置默认为关
            byteArrayOf(firstNum.toByte(), Opcode.SCENE_SWITCH8K, 0x00, firsDbSceneId.toByte(), 0x07, Opcode.CLOSE, 0x00, 0x00)
        }
    }

    private fun getGroupParm(list: MutableList<Int>): ByteArray {
        val firstNum = list[0]
        val dbGroup1 = groupMap[firstNum]!!
        var opcodeOne: Byte
        var fristL: Byte = 0
        var fristH: Byte = 0

        if (dbGroup1 == null || dbGroup1.id == 1000000L) {
            opcodeOne = Opcode.DEFAULT_SWITCH8K
            listKeysBean.put(getKeyBean(firstNum, opcodeOne.toInt(), name = getString(R.string.click_config), hight8Mes = 0, low8Mes = 0))
        } else {
            val fristMesAddr = dbGroup1.meshAddr
            val mesL = fristMesAddr and 0xff
            val mesH = (fristMesAddr shr 8) and 0xff
            fristL = mesL.toByte()
            fristH = mesH.toByte()
            opcodeOne = getAllGroupOpcode(fristMesAddr)
            listKeysBean.put(getKeyBean(firstNum, opcodeOne.toInt(), name = groupMap[firstNum]!!.name, hight8Mes = mesH, low8Mes = mesL))
        }

        return if (list.size > 1) {
            val secondNum = list[1]
            val dbGroup2 = groupMap[secondNum]

            var opcodeTwo: Byte
            var secondL: Byte = 0
            var secondH: Byte = 0

            if (dbGroup2 == null || dbGroup2.id == 1000000L) {
                opcodeTwo = Opcode.DEFAULT_SWITCH8K
                listKeysBean.put(getKeyBean(secondNum, opcodeTwo.toInt(), name = getString(R.string.click_config), hight8Mes = 0, low8Mes = 0))
            } else {
                val secondMesAddr = dbGroup2.meshAddr
                val mesL = secondMesAddr and 0xff
                val mesH = (secondMesAddr shr 8) and 0xff
                secondL = mesL.toByte()
                secondH = mesH.toByte()
                opcodeTwo = getAllGroupOpcode(secondMesAddr)
                listKeysBean.put(getKeyBean(secondNum, opcodeTwo.toInt(), name = groupMap[secondNum]!!.name, hight8Mes = mesH, low8Mes = mesL))
            }

            byteArrayOf(firstNum.toByte(), opcodeOne, fristH, fristL, secondNum.toByte(), opcodeTwo, secondH, secondL)
        } else {
            //如果第八键没有配置默认为关
            listKeysBean.put(getKeyBean(0x08, Opcode.CLOSE.toInt()))
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
        showRenameDialog(switchData)
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
        when (val type = configSwitchTypeNum % 3) {
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
                switchData?.name = renameEt?.text.toString().trim { it <= ' ' }
                if (switchData == null)
                    switchData = DBUtils.getSwitchByMeshAddr(mDeviceInfo?.meshAddress ?: 0)
                toolbarTv.text = switchData?.name
                if (switchData != null)
                    DBUtils.updateSwicth(switchData!!)
                else
                    ToastUtils.showLong(getString(R.string.rename_faile))

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


    private fun finishAc() {
        TelinkLightService.Instance().idleMode(true)
        TelinkLightService.Instance().disconnect()
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