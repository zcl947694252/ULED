package com.dadoutek.uled.light

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.PorterDuff
import android.os.Bundle
import android.os.Handler
import android.os.Message
import android.support.v4.app.FragmentActivity
import android.support.v4.content.ContextCompat
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.Toolbar
import android.text.TextUtils
import android.util.Log
import android.view.*
import android.view.View.*
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import com.blankj.utilcode.util.LogUtils
import com.blankj.utilcode.util.ToastUtils
import com.dadoutek.uled.R
import com.dadoutek.uled.base.RouteGetVerBean
import com.dadoutek.uled.base.TelinkBaseActivity
import com.dadoutek.uled.communicate.Commander
import com.dadoutek.uled.intf.OtaPrepareListner
import com.dadoutek.uled.intf.SyncCallback
import com.dadoutek.uled.model.*
import com.dadoutek.uled.model.dbModel.DBUtils
import com.dadoutek.uled.model.dbModel.DBUtils.lastUser
import com.dadoutek.uled.model.dbModel.DbGroup
import com.dadoutek.uled.model.dbModel.DbLight
import com.dadoutek.uled.model.routerModel.RouterModel
import com.dadoutek.uled.network.GroupBodyBean
import com.dadoutek.uled.network.RouterDelGpBody
import com.dadoutek.uled.ota.OTAUpdateActivity
import com.dadoutek.uled.router.RouterOtaActivity
import com.dadoutek.uled.router.bean.CmdBodyBean
import com.dadoutek.uled.router.bean.RouteGroupingOrDelBean
import com.dadoutek.uled.switches.ChooseGroupOrSceneActivity
import com.dadoutek.uled.tellink.TelinkLightApplication
import com.dadoutek.uled.tellink.TelinkLightService
import com.dadoutek.uled.util.*
import com.tbruyelle.rxpermissions2.RxPermissions
import com.telink.TelinkApplication
import com.telink.bluetooth.light.ConnectionStatus
import com.telink.bluetooth.light.DeviceInfo
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import jp.co.cyberagent.android.gpuimage.filter.GPUImageBrightnessFilter
import jp.co.cyberagent.android.gpuimage.filter.GPUImageFilterGroup
import jp.co.cyberagent.android.gpuimage.filter.GPUImageWhiteBalanceFilter
import kotlinx.android.synthetic.main.activity_device_setting.*
import kotlinx.android.synthetic.main.toolbar.*
import kotlinx.coroutines.*
import org.jetbrains.anko.startActivity
import java.lang.Float
import java.util.*
import java.util.concurrent.TimeUnit

class NormalSettingActivity : TelinkBaseActivity(), TextView.OnEditorActionListener, OnClickListener {
    private var disposableTimer: Disposable? = null
    private var sendProgress: Int = 1
    private var tempSpeed: Int = 0
    private var findItemChangeGp: MenuItem? = null
    private var findItem: MenuItem? = null
    private val requestCodeNum: Int = 1000
    private var typeStr: String? = null
    private var isAllGroup: Boolean = false
    private var downloadDispoable: Disposable? = null
    private var localVersion: String? = null
    private lateinit var light: DbLight
    private val mDisposable = CompositeDisposable()
    private var mRxPermission: RxPermissions? = null
    var gpAddress: Int = 0
    var fromWhere: String? = null
    private var updateLightDisposal: Disposable? = null
    private var mApp: TelinkLightApplication? = null
    private var manager: DataManager? = null
    private var mConnectDevice: DeviceInfo? = null
    private var mConnectTimer: Disposable? = null
    var mApplication: TelinkLightApplication? = null
    private var isRenameState = false
    private var group: DbGroup? = null
    private var currentShowPageGroup = true
    private var isBrightness = true//是否是亮度
    var downTime: Long = 0//Button被按下时的时间
    var thisTime: Long = 0//while每次循环时的时间
    var onBtnTouch = false//Button是否被按下

    //var = 0//TextView中的值
    private var isProcessChange = 0
    private var isSwitch: Boolean = false

    @SuppressLint("StringFormatInvalid")
    private val clickListener = OnClickListener { v ->
        when (v.id) {
            R.id.btnRename -> renameLight()
            R.id.tvOta -> if (isRenameState) saveName() else checkPermission()
            R.id.updateGroup -> updateGroup()
            R.id.btnRemove -> remove()
            R.id.btn_remove_group -> AlertDialog.Builder(Objects.requireNonNull<FragmentActivity>(this))
                    .setMessage(getString(R.string.delete_group_confirm, group?.name))
                    .setPositiveButton(android.R.string.ok) { _, _ ->
                        this.showLoadingDialog(getString(R.string.deleting))
                        if (Constants.IS_ROUTE_MODE) {
                            routeDeleteGroup("delCWGp", group!!)
                        } else {
                            deleteGroup(DBUtils.getLightByGroupID(group!!.id), group!!,
                                    successCallback = {
                                        deleteGpSuccess()
                                    },
                                    failedCallback = {
                                        this.hideLoadingDialog()
                                        ToastUtils.showLong(R.string.move_out_some_lights_in_group_failed)
                                    })
                        }
                    }
                    .setNegativeButton(R.string.btn_cancel, null)
                    .show()
            R.id.btn_rename, R.id.img_function1 -> renameGroup()
            R.id.light_switch -> lightSwitch()
            R.id.brightness_btn -> setBrightness()
            R.id.temperature_btn -> setTemperature()
        }
    }

    @SuppressLint("StringFormatInvalid", "StringFormatMatches")
    override fun tzRouterDelGroupResult(routerGroup: RouteGroupingOrDelBean?) {
        LogUtils.v("zcl-----------收到路由删组通知-------${routerGroup}")
        disposableTimer?.dispose()
        if (routerGroup?.ser_id == "delCWGp") {
            hideLoadingDialog()
            if (routerGroup?.finish) {
                val gp = DBUtils.getGroupByID(routerGroup.targetGroupId.toLong())
                when (routerGroup?.status) {
                    0 -> {
                        deleteGpSuccess()
                        ToastUtils.showShort(getString(R.string.delete_group_success))
                    }
                    1 -> ToastUtils.showShort(getString(R.string.delete_group_some_fail))
                    -1 -> ToastUtils.showShort(getString(R.string.delete_gp_fail))
                }
            } else {
                ToastUtils.showShort(getString(R.string.router_del_gp, routerGroup.succeedNow.size))
            }
        }
    }


    override fun deleteGpSuccess() {
        SyncDataPutOrGetUtils.syncGetDataStart(DBUtils.lastUser!!, syncCallbackGet)
        this.hideLoadingDialog()
        this?.setResult(Constants.RESULT_OK)
        this?.finish()
    }

    @SuppressLint("ResourceAsColor", "SetTextI18n")
    private fun setTemperature() {
        adjustment.text = getString(R.string.color_temperature_adjustment)
        if (currentShowPageGroup) {
            temperature_btn.setImageResource(R.drawable.icon_btn)
            temperature_text.setTextColor(resources.getColor(R.color.blue_background))
            brightness_btn.setImageResource(R.drawable.icon_unselected)
            brightness_text.setTextColor(resources.getColor(R.color.black_nine))
            isBrightness = false
            if (isSwitch) {
                var light = DBUtils.getGroupByID(group!!.id)
                if (light != null) {
                    light_sbBrightness?.progress = light.colorTemperature
                    light.isSeek = false
                    tv_Brightness.text = light.colorTemperature.toString() + "%"
                    setLightGUIImg(temperatureValue = light.colorTemperature)
                    Log.e("TAG_SET_C", light.colorTemperature.toString())
                    if (light!!.connectionStatus == ConnectionStatus.OFF.value) {
                        device_light_add.setImageResource(R.drawable.icon_puls_no)
                        device_light_minus.setImageResource(R.drawable.icon_minus_no)
                    } else {
                        setAddAndMinusIcon(light.colorTemperature)
                    }
                }
            }
        } else {
            temperature_btn.setImageResource(R.drawable.icon_btn)
            temperature_text.setTextColor(resources.getColor(R.color.blue_background))
            brightness_btn.setImageResource(R.drawable.icon_unselected)
            brightness_text.setTextColor(resources.getColor(R.color.black_nine))
            isBrightness = false
            if (isSwitch) {
                var light = DBUtils.getLightByID(light!!.id)
                if (light != null) {
                    light_sbBrightness?.progress = light.colorTemperature
                    setLightGUIImg(temperatureValue = light.colorTemperature)
                    light.isSeek = false
                    tv_Brightness.text = light.colorTemperature.toString() + "%"
                    Log.e("TAG_SET_C", light.colorTemperature.toString())

                    if (light!!.connectionStatus == ConnectionStatus.OFF.value) {
                        device_light_add.setImageResource(R.drawable.icon_puls_no)
                        device_light_minus.setImageResource(R.drawable.icon_minus_no)
                    } else {
                        setAddAndMinusIcon(light.colorTemperature)
                    }
                }
            }
        }
    }

    @SuppressLint("ResourceAsColor")
    private fun setBrightness() {
        adjustment.text = getString(R.string.brightness_adjustment)
        if (currentShowPageGroup) {
            brightness_btn.setImageResource(R.drawable.icon_btn)
            brightness_text.setTextColor(resources.getColor(R.color.blue_background))
            temperature_btn.setImageResource(R.drawable.icon_unselected)
            temperature_text.setTextColor(resources.getColor(R.color.black_nine))
            isBrightness = true
            if (isSwitch) {
                group?.let {
                    light_sbBrightness?.progress = it.brightness
                    it.isSeek = true
                    tv_Brightness.text = "${it.brightness}%"
                    setLightGUIImg(progress = it.brightness)
                    Log.e("TAG_SET_B", it.brightness.toString())
                    if (group!!.connectionStatus == ConnectionStatus.OFF.value) {
                        device_light_add.setImageResource(R.drawable.icon_puls_no)
                        device_light_minus.setImageResource(R.drawable.icon_minus_no)
                    } else {
                        setAddAndMinusIcon(it.brightness)
                    }
                }
            }
        } else {
            brightness_btn.setImageResource(R.drawable.icon_btn)
            brightness_text.setTextColor(resources.getColor(R.color.blue_background))
            temperature_btn.setImageResource(R.drawable.icon_unselected)
            temperature_text.setTextColor(resources.getColor(R.color.black_nine))
            isBrightness = true
            if (isSwitch) {
                var light = DBUtils.getLightByID(light!!.id)
                if (light != null) {
                    light_sbBrightness?.progress = light.brightness
                    light.isSeek = true
                    tv_Brightness.text = "${light.brightness}%"
                    setLightGUIImg(progress = light.brightness)
                    Log.e("TAG_SET_B", light.brightness.toString())
                    if (light!!.connectionStatus == ConnectionStatus.OFF.value) {
                        device_light_add.setImageResource(R.drawable.icon_puls_no)
                        device_light_minus.setImageResource(R.drawable.icon_minus_no)
                    } else {
                        setAddAndMinusIcon(light.brightness)
                    }
                }
            }
        }
    }

    private fun updateLights(isOpen: Boolean, group: DbGroup) {
        updateLightDisposal?.dispose()
        updateLightDisposal = Observable.timer(300, TimeUnit.MILLISECONDS, Schedulers.io())
                .subscribe({
                    var lightList: MutableList<DbLight> = ArrayList()

                    if (group.meshAddr == 0xffff) {
                        val list = DBUtils.groupList
                        for (j in list.indices) {
                            lightList.addAll(DBUtils.getLightByGroupID(list[j].id))
                        }
                    } else {
                        lightList = DBUtils.getLightByGroupID(group.id)
                    }

                    for (dbLight: DbLight in lightList) {
                        if (isOpen) {
                            dbLight.connectionStatus = ConnectionStatus.ON.value
                        } else {
                            dbLight.connectionStatus = ConnectionStatus.OFF.value
                        }
                        DBUtils.updateLightLocal(dbLight)
                    }
                }, {})
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun lightSwitch() {
        if (currentShowPageGroup) {
            when {
                group!!.id.toInt() == 1 -> SharedPreferencesUtils.setAllLightModel(true)
                else -> SharedPreferencesUtils.setAllLightModel(false)
            }
            if (group != null) {
                if (group!!.connectionStatus == ConnectionStatus.OFF.value) {
                    if (Constants.IS_ROUTE_MODE) {//meshType普通灯 = 4 彩灯 = 6 连接器 = 5 组 = 97
                        routeOpenOrCloseBase(group!!.meshAddr, 97, 1, "gpSetting")
                    } else {
                        Commander.openOrCloseLights(group!!.meshAddr, true)
                        afterOpenGp()
                    }

                    device_light_add.setOnTouchListener { _, event ->
                        batchedAction(event, handlerAddBtn)
                        true
                    }

                    device_light_minus.setOnTouchListener { v, event ->
                        batchedAction(event, handlerMinusBtn)

                        true
                    }
                } else {
                    if (Constants.IS_ROUTE_MODE) {//meshType普通灯 = 4 彩灯 = 6 连接器 = 5 组 = 97
                        routeOpenOrCloseBase(group!!.meshAddr, 97, 0, "gpSetting")
                    } else {
                        Commander.openOrCloseLights(group!!.meshAddr, false)
                        afterCloseGp()
                    }
                }
            }
        } else {
            if (light != null) {
                if (light!!.connectionStatus == ConnectionStatus.OFF.value) {
                    if (Constants.IS_ROUTE_MODE) {//meshType普通灯 = 4 彩灯 = 6 连接器 = 5 组 = 97
                        routeOpenOrCloseBase(light!!.meshAddr, light!!.productUUID, 1, "lightSetting")
                    } else {
                        Commander.openOrCloseLights(light!!.meshAddr, true)
                        afterOpenLight()
                    }
                } else {
                    if (Constants.IS_ROUTE_MODE) {//meshType普通灯 = 4 彩灯 = 6 连接器 = 5 组 = 97
                        routeOpenOrCloseBase(light!!.meshAddr, light!!.productUUID, 0, "lightSetting")
                    } else {
                        Commander.openOrCloseLights(light!!.meshAddr, false)
                        afterCloseLight()
                    }
                }
            }
        }
    }

    private fun afterCloseLight() {
        isSwitch = false
        light!!.connectionStatus = ConnectionStatus.OFF.value
        light_switch.setImageResource(R.drawable.icon_light_close)
        //light_image.setImageResource()
        setLightGUIImg(isClose = true)
        light_sbBrightness!!.setOnTouchListener { v, event -> true }
        light_sbBrightness.isEnabled = false
        device_light_add.setImageResource(R.drawable.icon_puls_no)
        device_light_minus.setImageResource(R.drawable.icon_minus_no)
        device_light_add.setOnTouchListener { v, event -> false }
        device_light_minus.setOnTouchListener { v, event -> false }
        DBUtils.updateLight(light!!)
    }

    private fun afterOpenLight() {
        isSwitch = true
        light_switch.setImageResource(R.drawable.icon_light_open)
        //light_image.setImageResource(R.drawable.icon_light)
        if (isBrightness)
            setLightGUIImg(progress = light!!.brightness)
        else
            setLightGUIImg(temperatureValue = light!!.colorTemperature)
        light!!.connectionStatus = ConnectionStatus.ON.value
        light_sbBrightness!!.setOnTouchListener { v, _ -> false }
        light_sbBrightness.isEnabled = true
        setAddOrminsIcon(light!!.brightness, light!!.colorTemperature)
        device_light_add.setOnTouchListener { _, event ->
            batchedAction(event, handlerAddBtn)
            true
        }

        device_light_minus.setOnTouchListener { v, event ->
            batchedAction(event, handlerMinusBtn)
            true
        }
    }

    private fun afterCloseGp() {
        group!!.connectionStatus = ConnectionStatus.OFF.value
        light_switch.setImageResource(R.drawable.icon_light_close)
        //light_image.setImageResource()
        setLightGUIImg(isClose = true)
        light_sbBrightness!!.setOnTouchListener { v, event -> true }
        light_sbBrightness.isEnabled = false
        device_light_add.setImageResource(R.drawable.icon_puls_no)
        device_light_minus.setImageResource(R.drawable.icon_minus_no)
        device_light_add.setOnTouchListener { v, event -> false }
        device_light_minus.setOnTouchListener { v, event -> false }
        isSwitch = false
        DBUtils.updateGroup(group!!)
        updateLights(false, group!!)
    }

    override fun tzRouterConfigBriOrTemp(cmdBean: CmdBodyBean, isBri: Boolean) {
        LogUtils.v("zcl------收到路由配置亮度灯通知------------$cmdBean")
        disposableRouteTimer?.dispose()
        if (cmdBean.status == 0) {
            when {
                currentShowPageGroup -> {
                    when {
                        isBri -> group?.brightness = sendProgress
                        else -> group?.colorTemperature = sendProgress
                    }
                    if (group != null)
                        DBUtils.saveGroup(group!!, false)
                }
                else -> {
                    when {
                        isBri -> light?.brightness = sendProgress
                        else -> light?.colorTemperature = sendProgress
                    }
                    DBUtils.saveLight(light, false)
                }
            }
            hideLoadingDialog()
        } else {
            hideLoadingDialog()
            when {
                currentShowPageGroup -> {
                    when {
                        isBri -> light_sbBrightness.progress = group?.brightness ?: 1
                        else -> light_sbBrightness.progress = group?.colorTemperature ?: 1
                    }
                }
                else -> {
                    when {
                        isBri -> light_sbBrightness.progress = light?.brightness
                        else -> light_sbBrightness.progress = light?.colorTemperature
                    }
                }
            }
            when {
                isBri -> ToastUtils.showShort(getString(R.string.config_bri_fail))
                else -> ToastUtils.showShort(getString(R.string.config_color_temp_fail))
            }
        }
    }

    override fun tzRouterOpenOrClose(cmdBean: CmdBodyBean) {
        LogUtils.v("zcl------收到路由开关灯通知------------$cmdBean")
        hideLoadingDialog()
        disposableRouteTimer?.dispose()
        when (cmdBean.ser_id) {
            "gpSetting" -> {
                when (group!!.connectionStatus) {
                    ConnectionStatus.OFF.value -> group!!.connectionStatus = ConnectionStatus.ON.value
                    else -> group!!.connectionStatus = ConnectionStatus.OFF.value
                }
                when (cmdBean.status) {
                    0 -> if (group!!.connectionStatus == ConnectionStatus.OFF.value) afterCloseGp() else afterOpenGp()
                    else -> ToastUtils.showShort(getString(R.string.open_faile))
                }
            }
            "lightSetting" -> {
                when (light!!.connectionStatus) {
                    ConnectionStatus.OFF.value -> light!!.connectionStatus = ConnectionStatus.ON.value
                    else -> light!!.connectionStatus = ConnectionStatus.OFF.value
                }
                when (cmdBean.status) {
                    0 -> if (light!!.connectionStatus == ConnectionStatus.OFF.value) afterCloseLight() else afterOpenLight()
                    else -> ToastUtils.showShort(getString(R.string.open_faile))
                }
            }
        }
    }

    private fun setAddOrminsIcon(brightness: Int, colorTemperature: Int) {
        if (isBrightness) {
            when {
                brightness <= 1 -> {
                    device_light_minus.setImageResource(R.drawable.icon_minus_no)
                    device_light_add.setImageResource(R.drawable.icon_puls)
                }
                brightness >= 100 -> {
                    device_light_add.setImageResource(R.drawable.icon_puls_no)
                    device_light_minus.setImageResource(R.drawable.icon_minus)
                }
                else -> {
                    device_light_minus.setImageResource(R.drawable.icon_minus)
                    device_light_add.setImageResource(R.drawable.icon_puls)
                }
            }
        } else {
            when {
                colorTemperature <= 1 -> {
                    device_light_minus.setImageResource(R.drawable.icon_minus_no)
                    device_light_add.setImageResource(R.drawable.icon_puls)
                }
                colorTemperature >= 100 -> {
                    device_light_add.setImageResource(R.drawable.icon_puls_no)
                    device_light_minus.setImageResource(R.drawable.icon_minus)
                }
                else -> {
                    device_light_minus.setImageResource(R.drawable.icon_minus)
                    device_light_add.setImageResource(R.drawable.icon_puls)
                }
            }
        }
    }

    private fun afterOpenGp() {
        isSwitch = true
        light_switch.setImageResource(R.drawable.icon_light_open)
        if (isBrightness)
            setLightGUIImg(progress = group!!.brightness)
        else
            setLightGUIImg(temperatureValue = group!!.colorTemperature)
        group!!.connectionStatus = ConnectionStatus.ON.value
        light_sbBrightness!!.setOnTouchListener { _, _ -> false }
        light_sbBrightness.isEnabled = true
        setAddOrminsIcon(group!!.brightness, group!!.colorTemperature)
        /*  if (isBrightness) {
              when {group!!.brightness <= 1 -> {
                      device_light_minus.setImageResource(R.drawable.icon_minus_no)
                      device_light_add.setImageResource(R.drawable.icon_puls)
                  }group.brightness >= 100 -> {
                      device_light_add.setImageResource(R.drawable.icon_puls_no)
                      device_light_minus.setImageResource(R.drawable.icon_minus)
                  }else -> {
                      device_light_minus.setImageResource(R.drawable.icon_minus)
                      device_light_add.setImageResource(R.drawable.icon_puls)
                  }
              }
          } else {
              when {group!!.colorTemperature <= 1 -> {
                      device_light_minus.setImageResource(R.drawable.icon_minus_no)
                      device_light_add.setImageResource(R.drawable.icon_puls)
                  }group.colorTemperature >= 100 -> {
                      device_light_add.setImageResource(R.drawable.icon_puls_no)
                      device_light_minus.setImageResource(R.drawable.icon_minus)
                  }else -> {
                      device_light_minus.setImageResource(R.drawable.icon_minus)
                      device_light_add.setImageResource(R.drawable.icon_puls)
                  }
              }
          }*/
        DBUtils.updateGroup(group!!)
        updateLights(true, group!!)
    }

    @SuppressLint("HandlerLeak")
    private val handlerMinusBtn = object : Handler() {
        @SuppressLint("SetTextI18n")
        override fun handleMessage(msg: Message) {
            super.handleMessage(msg)

            minusBtnUi()
        }
    }

    private fun minusBtnUi() {
        if (isProcessChange == 0) {
            if (light_sbBrightness.progress > 1)
                light_sbBrightness.progress--
            when {
                light_sbBrightness.progress <= 1 -> device_light_minus.setImageResource(R.drawable.icon_minus_no)
                else -> tv_Brightness.text = light_sbBrightness.progress.toString() + "%"
            }
            if (light_sbBrightness.progress < 100)
                device_light_add.setImageResource(R.drawable.icon_puls)
        } else {
            when {
                isBrightness -> {//亮度
                    if (light_sbBrightness.progress > 1)
                        light_sbBrightness.progress--

                    when {
                        light_sbBrightness.progress < 1 ->
                            device_light_minus.setImageResource(R.drawable.icon_minus_no)

                        light_sbBrightness.progress <= 1 -> {
                            device_light_minus.setImageResource(R.drawable.icon_minus_no)

                            when {
                                currentShowPageGroup -> {
                                    tv_Brightness.text = light_sbBrightness.progress.toString() + "%"
                                    if (group != null) {
                                        if (!Constants.IS_ROUTE_MODE) {
                                            updateLights(group!!.brightness, "brightness", group!!)
                                            group!!.brightness = light_sbBrightness.progress
                                            DBUtils.updateGroup(group!!)
                                        }
                                    }
                                }
                                else -> {
                                    light_sbBrightness.progress = light!!.brightness
                                    tv_Brightness.text = light?.brightness.toString() + "%"
                                    if (light != null && !Constants.IS_ROUTE_MODE) {
                                        light.brightness = light_sbBrightness.progress
                                        DBUtils.updateLight(light)
                                    }
                                }
                            }
                        }
                        else -> {
                            tv_Brightness.text = light_sbBrightness.progress.toString() + "%"
                            if (currentShowPageGroup) {
                                if (group != null) {
                                    updateLights(group!!.brightness, "brightness", group!!)
                                    if (!Constants.IS_ROUTE_MODE) {
                                        group!!.brightness = light_sbBrightness.progress
                                        DBUtils.updateGroup(group!!)
                                    }
                                }
                            } else {
                                if (light != null && !Constants.IS_ROUTE_MODE) {
                                    light.brightness = light_sbBrightness.progress
                                    DBUtils.updateLight(light)
                                }
                            }

                        }
                    }
                    if (light_sbBrightness.progress < 100)
                        device_light_add.setImageResource(R.drawable.icon_puls)
                }
                else -> {//色温
                    if (light_sbBrightness.progress > 1)
                        light_sbBrightness.progress--
                    when {
                        light_sbBrightness.progress < 1 -> device_light_minus.setImageResource(R.drawable.icon_minus_no)

                        light_sbBrightness.progress <= 1 -> {
                            device_light_minus.setImageResource(R.drawable.icon_minus_no)
                            when {
                                currentShowPageGroup -> {
                                    var light = DBUtils.getGroupByID(group!!.id)
                                    light_sbBrightness.progress = group!!.colorTemperature
                                    tv_Brightness.text = light_sbBrightness.progress.toString() + "%"
                                    if (light != null) {
                                        if (!Constants.IS_ROUTE_MODE) {
                                            light.colorTemperature = light_sbBrightness.progress
                                            DBUtils.updateGroup(light)
                                        }
                                        updateLights(light.colorTemperature, "colorTemperature", light)
                                    }
                                }
                                else -> {
                                    var light = DBUtils.getLightByID(light!!.id)
                                    if (light != null) {
                                        if (!Constants.IS_ROUTE_MODE) {
                                            light.colorTemperature = light_sbBrightness.progress
                                            DBUtils.updateLight(light)
                                        }
                                    }
                                }
                            }
                        }
                        else -> {
                            tv_Brightness.text = light_sbBrightness.progress.toString() + "%"
                            when {
                                currentShowPageGroup -> {
                                    if (group != null && !Constants.IS_ROUTE_MODE) {
                                        group!!.colorTemperature = light_sbBrightness.progress
                                        DBUtils.updateGroup(group!!)
                                        updateLights(group!!.colorTemperature, "colorTemperature", group!!)
                                    }
                                }
                                else -> {
                                    if (light != null && !Constants.IS_ROUTE_MODE) {
                                        light.colorTemperature = light_sbBrightness.progress
                                        DBUtils.updateLight(light)
                                    }
                                }
                            }
                        }
                    }

                    if (light_sbBrightness.progress < 100)
                        device_light_add.setImageResource(R.drawable.icon_puls)
                }
            }
        }
    }

    @SuppressLint("HandlerLeak")
    private val handlerAddBtn = object : Handler() {
        override fun handleMessage(msg: Message) {
            super.handleMessage(msg)
            addBtnUi()
        }
    }

    private fun addBtnUi() {
        when (isProcessChange) {
            0 -> {
                light_sbBrightness.progress++
                if (light_sbBrightness.progress >= 100)
                    device_light_add.setImageResource(R.drawable.icon_puls_no)
                else
                    tv_Brightness.text = light_sbBrightness.progress.toString() + "%"

                if (light_sbBrightness.progress > 1)
                    device_light_minus.setImageResource(R.drawable.icon_minus)
            }
            1 -> {
                when {
                    isBrightness -> {//亮度
                        light_sbBrightness.progress++
                        when {
                            light_sbBrightness.progress > 100 -> device_light_add.setImageResource(R.drawable.icon_puls_no)

                            light_sbBrightness.progress >= 100 -> {
                                device_light_add.setImageResource(R.drawable.icon_puls_no)
                                tv_Brightness.text = light_sbBrightness.progress.toString() + "%"
                                when {
                                    currentShowPageGroup -> {
                                        val light = DBUtils.getGroupByID(group!!.id)
                                        if (light != null && !Constants.IS_ROUTE_MODE) {
                                            light.brightness = light_sbBrightness.progress
                                            DBUtils.updateGroup(light)
                                            updateLights(light.brightness, "brightness", light)
                                        }
                                    }
                                    else -> {
                                        val light = DBUtils.getLightByID(light!!.id)
                                        if (light != null && !Constants.IS_ROUTE_MODE) {
                                            light.brightness = light!!.brightness
                                            DBUtils.updateLight(light)
                                        }
                                    }
                                }
                            }
                            else -> {
                                tv_Brightness.text = light_sbBrightness.progress.toString() + "%"
                                when {
                                    currentShowPageGroup -> {
                                        var light = DBUtils.getGroupByID(group!!.id)
                                        if (light != null && !Constants.IS_ROUTE_MODE) {
                                            light.brightness = light_sbBrightness.progress
                                            DBUtils.updateGroup(light)
                                            updateLights(light.brightness, "brightness", light)
                                        }
                                    }
                                    else -> {
                                        var light = DBUtils.getLightByID(light!!.id)
                                        if (light != null && !Constants.IS_ROUTE_MODE) {
                                            light.brightness = light!!.brightness
                                            DBUtils.updateLight(light)
                                        }
                                    }
                                }
                            }
                        }

                        if (light_sbBrightness.progress > 1)
                            device_light_minus.setImageResource(R.drawable.icon_minus)
                    }
                    else -> {//色温
                        light_sbBrightness.progress++
                        when {
                            light_sbBrightness.progress > 100 -> device_light_add.setImageResource(R.drawable.icon_puls_no)

                            light_sbBrightness.progress == 100 -> {
                                device_light_add.setImageResource(R.drawable.icon_puls_no)
                                tv_Brightness.text = light_sbBrightness.progress.toString() + "%"
                                if (currentShowPageGroup) {
                                    var light = DBUtils.getGroupByID(group!!.id)
                                    if (light != null && !Constants.IS_ROUTE_MODE) {
                                        light.colorTemperature = light_sbBrightness.progress
                                        DBUtils.updateGroup(light)
                                        updateLights(light.colorTemperature, "colorTemperature", light)
                                    }
                                } else {
                                    var light = DBUtils.getLightByID(light!!.id)
                                    if (light != null && !Constants.IS_ROUTE_MODE) {
                                        light.colorTemperature = light_sbBrightness.progress
                                        DBUtils.updateLight(light)
                                    }
                                }
                            }
                            else -> {
                                tv_Brightness.text = light_sbBrightness.progress.toString() + "%"
                                when {
                                    currentShowPageGroup -> {
                                        var light = DBUtils.getGroupByID(group!!.id)
                                        if (light != null && !Constants.IS_ROUTE_MODE) {
                                            light.colorTemperature = light_sbBrightness.progress
                                            DBUtils.updateGroup(light)
                                            updateLights(light.colorTemperature, "colorTemperature", light)
                                        }
                                    }
                                    else -> {
                                        var light = DBUtils.getLightByID(light!!.id)
                                        if (light != null && !Constants.IS_ROUTE_MODE) {
                                            light.colorTemperature = light_sbBrightness.progress
                                            DBUtils.updateLight(light)
                                        }
                                    }
                                }
                            }
                        }

                        if (light_sbBrightness.progress > 0)
                            device_light_minus.setImageResource(R.drawable.icon_minus)
                    }
                }
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {

        DBUtils.lastUser?.let {
            if (it.id.toString() == it.last_authorizer_user_id)
                if (currentShowPageGroup) {
                    // menuInflater.inflate(R.menu.menu_rgb_group_setting, menu)
                    //toolbar.menu?.findItem(R.id.toolbar_batch_gp)?.isVisible = false
                    //toolbar.menu?.findItem(R.id.toolbar_delete_device)?.isVisible = false
                } else {
                    menuInflater.inflate(R.menu.menu_rgb_light_setting, menu)
                    findItem = menu?.findItem(R.id.toolbar_f_version)
                    findItemChangeGp = menu?.findItem(R.id.toolbar_fv_change_group)
                    findItemChangeGp?.isVisible = true
                    findItem?.title = localVersion
                }
        }
        LogUtils.v("zclmenu------------------$localVersion-----${DBUtils.lastUser}")
        return super.onCreateOptionsMenu(menu)
    }

    private fun renameGroup() {
        if (!TextUtils.isEmpty(group?.name))
            renameEt?.setText(group?.name)
        renameEt?.setSelection(renameEt?.text.toString().length)

        if (this != null && !this.isFinishing) {
            renameDialog?.dismiss()
            renameDialog?.show()
        }

        renameConfirm?.setOnClickListener {    // 获取输入框的内容
            if (StringUtils.compileExChar(renameEt?.text.toString().trim { it <= ' ' })) {
                ToastUtils.showLong(getString(R.string.rename_tip_check))
            } else {
                var name = renameEt?.text.toString().trim { it <= ' ' }
                var canSave = true
                val groups = DBUtils.allGroups
                for (i in groups.indices) {
                    if (groups[i].name == name) {
                        ToastUtils.showLong(TelinkLightApplication.getApp().getString(R.string.repeat_name))
                        canSave = false
                        break
                    }
                }

                if (canSave) {
                    group?.name = renameEt?.text.toString().trim { it <= ' ' }
                    DBUtils.updateGroup(group!!)
                    toolbarTv.text = group?.name
                    renameDialog.dismiss()
                }
            }
        }
    }

    /**
     * 删除组，并且把组里的灯的组也都删除。
     */
    private fun deleteGroup(lights: MutableList<DbLight>, group: DbGroup, retryCount: Int = 0,
                            successCallback: () -> Unit, failedCallback: () -> Unit) {
        Thread {
            if (lights.count() != 0) {
                val maxRetryCount = 3
                if (retryCount <= maxRetryCount) {
                    val light = lights[0]
                    val lightMeshAddr = light.meshAddr
                    Commander.deleteGroup(lightMeshAddr,
                            successCallback = {
                                light.belongGroupId = DBUtils.groupNull!!.id
                                DBUtils.updateLight(light)
                                lights.remove(light)
                                //修改分组成功后删除场景信息。
                                deleteAllSceneByLightAddr(light.meshAddr)
                                Thread.sleep(100)
                                if (lights.count() == 0) {
                                    //所有灯都删除了分组
                                    DBUtils.deleteGroupOnly(group)
                                    this?.runOnUiThread {
                                        successCallback.invoke()
                                    }
                                } else {
                                    //还有灯要删除分组
                                    deleteGroup(lights, group,
                                            successCallback = successCallback,
                                            failedCallback = failedCallback)
                                }
                                LogUtils.e("zcl删除组后" + DBUtils.getGroupsByDeviceType(DeviceType.LIGHT_NORMAL))
                            },
                            failedCallback = {
                                deleteGroup(lights, group, retryCount = retryCount + 1,
                                        successCallback = successCallback,
                                        failedCallback = failedCallback)
                            })
                } else {    //超过了重试次数
                    this?.runOnUiThread {
                        failedCallback.invoke()
                    }
                    //("retry delete group timeout")
                }
            } else {
                DBUtils.deleteGroupOnly(group)
                this?.runOnUiThread {
                    successCallback.invoke()
                }
            }
        }.start()

    }

    /**
     * 删除指定灯里的所有场景
     *
     * @param lightMeshAddr 灯的mesh地址
     */
    private fun deleteAllSceneByLightAddr(lightMeshAddr: Int) {
        val opcode = Opcode.SCENE_ADD_OR_DEL
        val params: ByteArray = byteArrayOf(0x00, 0xff.toByte())
        TelinkLightService.Instance()?.sendCommandNoResponse(opcode, lightMeshAddr, params)
    }

    override fun onStop() {
        super.onStop()
        mConnectTimer?.dispose()
    }

    override fun onDestroy() {
        super.onDestroy()
        downloadDispoable?.dispose()
        mConnectTimer?.dispose()
        disposableTimer?.dispose()
        mDisposable.dispose()
    }

    private fun updateGroup() {//更新分组 断开提示
        val intent = Intent(this@NormalSettingActivity, ChooseGroupOrSceneActivity::class.java)
        val bundle = Bundle()
        bundle.putInt(Constants.EIGHT_SWITCH_TYPE, 0)//传入0代表是群组
        bundle.putInt(Constants.DEVICE_TYPE, Constants.DEVICE_TYPE_LIGHT_NORMAL.toInt())
        intent.putExtras(bundle)
        startActivityForResult(intent, requestCodeNum)
        this?.setResult(Constants.RESULT_OK)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK && requestCode == requestCodeNum) {
            var group = data?.getSerializableExtra(Constants.EIGHT_SWITCH_TYPE) as DbGroup
            updateGroupResult(light, group)
        }
    }

    private fun updateGroupResult(light: DbLight, group: DbGroup) {
        if (Constants.IS_ROUTE_MODE) {
            val bodyBean = GroupBodyBean(mutableListOf(light.meshAddr), light.productUUID, "normalGp", group.meshAddr)
            routerChangeGpDevice(bodyBean)
        } else {
            Commander.addGroup(light.meshAddr, group.meshAddr, {
                group.deviceType = light.productUUID.toLong()
                light.hasGroup = true
                light.belongGroupId = group.id
                light.name = light.name
                DBUtils.updateLight(light)
                ToastUtils.showShort(getString(R.string.grouping_success_tip))
                if (group != null)
                    DBUtils.updateGroup(group!!)//更新组类型
            }, {
                ToastUtils.showShort(getString(R.string.grouping_fail))
            })
        }
    }

    @SuppressLint("StringFormatMatches")
    override fun tzRouterGroupResult(bean: RouteGroupingOrDelBean?) {
        if (bean?.ser_id == "normalGp") {
            LogUtils.v("zcl-----------收到路由普通灯分组通知-------$bean")
            disposableRouteTimer?.dispose()
            if (bean?.finish) {
                hideLoadingDialog()
                when (bean?.status) {
                    -1 -> ToastUtils.showShort(getString(R.string.group_failed))
                    0, 1 -> {
                        if (bean?.status == 0) ToastUtils.showShort(getString(R.string.grouping_success_tip)) else ToastUtils.showShort(getString(R.string.group_some_fail))
                        SyncDataPutOrGetUtils.syncGetDataStart(DBUtils.lastUser!!, object : SyncCallback {
                            override fun start() {}
                            override fun complete() {}
                            override fun error(msg: String?) {}
                        })
                    }
                }
            }
        }
    }

    private var otaPrepareListner: OtaPrepareListner = object : OtaPrepareListner {

        override fun downLoadFileStart() {
            showLoadingDialog(getString(R.string.get_update_file))
        }

        override fun startGetVersion() {
            showLoadingDialog(getString(R.string.verification_version))
        }

        override fun getVersionSuccess(s: String) {
            hideLoadingDialog()
        }

        override fun getVersionFail() {
            ToastUtils.showLong(R.string.verification_version_fail)
            hideLoadingDialog()
        }


        override fun downLoadFileSuccess() {
            hideLoadingDialog()
            transformView()
        }

        override fun downLoadFileFail(message: String) {
            hideLoadingDialog()
            ToastUtils.showLong(R.string.download_pack_fail)
        }
    }

    private fun checkPermission() {
        mDisposable.add(
                mRxPermission!!.request(Manifest.permission.READ_EXTERNAL_STORAGE,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE).subscribe {
                    if (it!!) {
                        var isBoolean: Boolean = SharedPreferencesHelper.getBoolean(TelinkLightApplication.getApp(), Constants.IS_DEVELOPER_MODE, false)
                        if (isBoolean) {
                            downloadDispoable = Commander.getDeviceVersion(light.meshAddr)
                                    .subscribe(
                                            { s ->
                                                if (!TextUtils.isEmpty(s)) {
                                                    localVersion = s
                                                    light?.version = s
                                                    findItem?.title = localVersion
                                                }
                                                if (OtaPrepareUtils.instance().checkSupportOta(s)!!) {
                                                    DBUtils.saveLight(light!!, false)
                                                    transformView()
                                                } else {
                                                    ToastUtils.showLong(getString(R.string.version_disabled))
                                                }
                                                hideLoadingDialog()
                                            },
                                            {
                                                hideLoadingDialog()
                                                ToastUtils.showLong(getString(R.string.get_version_fail))
                                            }
                                    )

                        } else {
                            OtaPrepareUtils.instance().gotoUpdateView(this@NormalSettingActivity, localVersion, otaPrepareListner)
                        }
                    } else {
                        ToastUtils.showLong(R.string.update_permission_tip)
                    }
                })
    }

    private fun startOtaAct() {
        val intent = Intent(this@NormalSettingActivity, OTAUpdateActivity::class.java)
        intent.putExtra(Constants.UPDATE_LIGHT, light)
        intent.putExtra(Constants.OTA_MES_Add, light?.meshAddr)
        intent.putExtra(Constants.OTA_MAC, light?.macAddr)
        intent.putExtra(Constants.OTA_VERSION, light?.version)
        intent.putExtra(Constants.OTA_TYPE, DeviceType.LIGHT_NORMAL)

        startActivity(intent)
        finish()
    }

    private fun transformView() {
        disableConnectionStatusListener()
        if (TelinkLightApplication.getApp().connectDevice != null && TelinkLightApplication.getApp().connectDevice.meshAddress == light?.meshAddr) {
            startOtaAct()
        } else {
            showLoadingDialog(getString(R.string.please_wait))
            TelinkLightService.Instance()?.idleMode(true)
            mConnectDisposable = Observable.timer(800, TimeUnit.MILLISECONDS)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .flatMap {
                        connect(light.meshAddr, macAddress = light.macAddr, fastestMode = true)
                    }?.subscribe({
                        hideLoadingDialog()
                        startOtaAct()
                    }
                            , {
                        hideLoadingDialog()
                        runOnUiThread { ToastUtils.showLong(R.string.connect_fail2) }
                        LogUtils.d(it)
                    })
        }
    }

    @SuppressLint("CheckResult")
    private fun getVersion() {
        val connectDevice = TelinkApplication.getInstance().connectDevice
        if (connectDevice != null/*&&connectDevice.meshAddress==light!!.meshAddr*/ || Constants.IS_ROUTE_MODE) {
            if (Constants.IS_ROUTE_MODE) {
                val mesAddress = mutableListOf(light.meshAddr)
                routerGetVersion(mesAddress, light.productUUID, "cwVersion")
            } else {
                val subscribe = Commander.getDeviceVersion(light!!.meshAddr)
                        .subscribe(
                                { s ->
                                    updateVersion(s)
                                },
                                {
                                    /*   if (textTitle != null) {
                                          // /textTitle!!.visibility = GONE
                                           tvOta!!.visibility = GONE
                                       }*/
                                    LogUtils.d(it)
                                })
            }
        }
    }

    private fun updateVersion(s: String?) {
        localVersion = s
        findItem?.title = localVersion
        if (OtaPrepareUtils.instance().checkSupportOta(localVersion)!!) {
            // textTitle!!.visibility = VISIBLE
            light!!.version = localVersion
        } else {
            // textTitle!!.visibility = VISIBLE
            light!!.version = localVersion
            tvOta!!.visibility = GONE
        }
        DBUtils.saveLight(light, false)
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        this.requestWindowFeature(Window.FEATURE_NO_TITLE)//requestFeature() must be called before adding content必须放在者否则八错
        super.onCreate(savedInstanceState)
        this.setContentView(R.layout.activity_device_setting)
        initType()
        this.mApplication = this.application as TelinkLightApplication
    }

    override fun onResume() {
        super.onResume()
        if (typeStr == Constants.TYPE_GROUP) {
            val dbGroup = DBUtils.getGroupByID(group!!.id)
            if (group?.status == 1) {
                setLightGUIImg(progress = dbGroup?.brightness ?: 0, temperatureValue = dbGroup?.colorTemperature ?: 0)
                light_switch.setImageResource(R.drawable.icon_light_open)
            } else
                light_switch.setImageResource(R.drawable.icon_light_close)
        } else {
            val light = DBUtils.getLightByID(light!!.id)
            if (light?.status == 1) {
                setLightGUIImg(progress = light.brightness, temperatureValue = light.colorTemperature)
                light_switch.setImageResource(R.drawable.icon_light_open)
            } else
                light_switch.setImageResource(R.drawable.icon_light_close)
        }
    }


    private fun initType() {
        typeStr = intent.getStringExtra(Constants.TYPE_VIEW)
        toolbar.setNavigationIcon(R.drawable.icon_return)
        toolbar.setNavigationOnClickListener { finish() }
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setOnMenuItemClickListener(menuItemClickListener)
        val moreIcon = ContextCompat.getDrawable(toolbar.context, R.drawable.abc_ic_menu_overflow_material)
        if (moreIcon != null) {
            moreIcon.setColorFilter(ContextCompat.getColor(toolbar.context, R.color.black), PorterDuff.Mode.SRC_ATOP)
            toolbar.overflowIcon = moreIcon
        }
        slow_rg_view.setOnClickListener(clickListener)
        currentShowPageGroup = typeStr == Constants.TYPE_GROUP
        LogUtils.v("zclmenu----------currentShowPageGroup--------$currentShowPageGroup----${DBUtils.lastUser?.id.toString() == DBUtils.lastUser?.last_authorizer_user_id}")
        if (currentShowPageGroup) {
            initDataGroup()
            initViewGroup()
            img_function1.setImageResource(R.drawable.icon_editor)
            if (group?.meshAddr != 0xffff)
                img_function1.visibility = VISIBLE
            img_function1.setOnClickListener(clickListener)
        } else {
            img_function1.visibility = GONE
            slow_ly.visibility = GONE
            initViewLight()
            getVersion()
        }
    }

    private fun initViewGroup() {
        if (group != null && group!!.meshAddr == 0xffff)
            toolbarTv.text = getString(R.string.allLight)
        //所有灯控分组暂标为系统默认分组不做修改处理
        if (group!!.meshAddr != 0xFFFF)
            toolbarTv.text = group!!.name

        slow_rg_slow?.setOnClickListener(this)
        slow_rg_middle?.setOnClickListener(this)
        slow_rg_fast?.setOnClickListener(this)
        slow_rg_close?.setOnClickListener(this)

        // btn_remove_group?.setOnClickListener(clickListener)
        //  btn_rename?.setOnClickListener(clickListener)
        brightness_btn.setOnClickListener(clickListener)
        light_switch.setOnClickListener(clickListener)
        temperature_btn.setOnClickListener(clickListener)

        if (group!!.isSeek) {
            adjustment.text = getString(R.string.brightness_adjustment)
            brightness_btn.setImageResource(R.drawable.icon_btn)
            brightness_text.setTextColor(resources.getColor(R.color.blue_background))
            temperature_btn.setImageResource(R.drawable.icon_unselected)
            temperature_text.setTextColor(resources.getColor(R.color.black_nine))
            light_sbBrightness?.progress = group!!.brightness
            tv_Brightness.text = group!!.brightness.toString() + "%"
            isBrightness = true
        } else {
            adjustment.text = getString(R.string.color_temperature_adjustment)
            temperature_btn.setImageResource(R.drawable.icon_btn)
            temperature_text.setTextColor(resources.getColor(R.color.blue_background))
            brightness_btn.setImageResource(R.drawable.icon_unselected)
            brightness_text.setTextColor(resources.getColor(R.color.black_nine))
            light_sbBrightness?.progress = group!!.colorTemperature
            tv_Brightness.text = group!!.colorTemperature.toString() + "%"
            isBrightness = false
        }

        var light = DBUtils.getGroupByID(group!!.id)

        this.light_sbBrightness?.max = 100
        if (group!!.connectionStatus == ConnectionStatus.OFF.value) {
            // light_image.setImageResource()
            setLightGUIImg()
            light_switch.setImageResource(R.drawable.icon_light_close)
            light_sbBrightness!!.setOnTouchListener { _, _ -> true }
            light_sbBrightness.isEnabled = false
            isSwitch = false
            device_light_add.setImageResource(R.drawable.icon_puls_no)
            device_light_minus.setImageResource(R.drawable.icon_minus_no)
            device_light_add.setOnTouchListener { _, _ -> false }
            device_light_minus.setOnTouchListener { _, _ -> false }
        } else {
            //light_image.setImageResource(R.drawable.icon_light)
            setLightGUIImg()

            light_switch.setImageResource(R.drawable.icon_light_open)
            light_sbBrightness!!.setOnTouchListener { v, _ -> false }
            light_sbBrightness.isEnabled = true
            isSwitch = true
            when {
                light!!.brightness <= 1 -> {
                    device_light_minus.setImageResource(R.drawable.icon_minus_no)
                    device_light_add.setImageResource(R.drawable.icon_puls)
                }
                light.brightness >= 100 -> {
                    device_light_add.setImageResource(R.drawable.icon_puls_no)
                    device_light_minus.setImageResource(R.drawable.icon_minus)
                }
                else -> {
                    device_light_minus.setImageResource(R.drawable.icon_minus)
                    device_light_add.setImageResource(R.drawable.icon_puls)
                }
            }
            device_light_add.setOnTouchListener { _, event ->
                batchedAction(event, handlerAddBtn)
/*
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        downTime = System.currentTimeMillis()
                        onBtnTouch = true
                        GlobalScope.launch {
                            while (onBtnTouch) {
                                thisTime = System.currentTimeMillis()
                                if (thisTime - downTime >= 500) {
                                    val msg = handlerMinusBtn.obtainMessage()
                                    handlerAddBtn.sendMessage(msg)
                                    delay(100)
                                }
                            }
                        }
                    }
                    MotionEvent.ACTION_UP -> {
                        onBtnTouch = false
                        if (thisTime - downTime < 500) {
                            val msg = handlerMinusBtn.obtainMessage()
                            handlerAddBtn.sendMessage(msg)
                        }
                    }
                    MotionEvent.ACTION_CANCEL -> {
                        onBtnTouch = false
                    }
                }
*/
                true
            }

            device_light_minus.setOnTouchListener { _, event ->
                batchedAction(event, handlerMinusBtn)
/*
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        downTime = System.currentTimeMillis()
                        onBtnTouch = true

                        GlobalScope.launch {
                            while (onBtnTouch) {
                                thisTime = System.currentTimeMillis()
                                if (thisTime - downTime >= 500) {

                                    val msg = handlerMinusBtn.obtainMessage()

                                    handlerMinusBtn.sendMessage(msg)

                                    delay(100)
                                }
                            }
                        }
                    }
                    MotionEvent.ACTION_UP -> {
                        onBtnTouch = false
                        if (thisTime - downTime < 500) {

                            val msg = handlerMinusBtn.obtainMessage()

                            handlerMinusBtn.sendMessage(msg)
                        }
                    }
                    MotionEvent.ACTION_CANCEL -> {
                        onBtnTouch = false
                    }
                }
*/
                true
            }
        }
        this.light_sbBrightness!!.setOnSeekBarChangeListener(this.barChangeListener)
    }

    private fun slowOrUpClose() {//关闭渐变指令
        afterSendClose()
        for (i in 0..3) {
            TelinkLightService.Instance().sendCommandNoResponse(Opcode.CONFIG_GRADIENT_OPCODE, group?.meshAddr ?: 0xffff,
                    byteArrayOf(Opcode.CONFIG_EXTEND_ALL_JBZL, 2, 2, 0, 2, 2, 2, 2))//第四位2关闭指令 文档是0  但是0收不到
        }
    }

    private fun slowOrUpOpen() {
        val group = DBUtils.getGroupByID(group!!.id)
        group?.slowUpSlowDownStatus = 1
        slow_rg_view.visibility = GONE
        DBUtils.updateGroup(group!!)
        TelinkLightService.Instance().sendCommandNoResponse(Opcode.CONFIG_GRADIENT_OPCODE, group?.meshAddr, byteArrayOf(Opcode.CONFIG_EXTEND_ALL_JBSD, group?.slowUpSlowDownSpeed.toByte()))
        Thread.sleep(500)
        TelinkLightService.Instance().sendCommandNoResponse(Opcode.CONFIG_GRADIENT_OPCODE, group?.meshAddr,
                //0x01 CONFIG_EXTEND_ALL_JBZL
                byteArrayOf(Opcode.CONFIG_EXTEND_ALL_JBZL, 1, 1, 2, 1, 0, 0, 0))//1是开 0是关 第四位
    }

    private fun setLightGUIImg(iconLight: Int = R.mipmap.round, progress: Int = 0, temperatureValue: Int = 0, isClose: Boolean = false) {
        var bitmap = BitmapFactory.decodeResource(resources, iconLight)
        light_image.setImage(bitmap)

        val filterGroup = GPUImageFilterGroup()
        if (!isClose) {
            if (isBrightness)
                filterGroup.addFilter(GPUImageBrightnessFilter(progress / 500f))
            else
                filterGroup.addFilter(GPUImageWhiteBalanceFilter(Float.valueOf(50000f - (temperatureValue - 9) * 500), 0.0f))//2000 - 57000
        } else {
            filterGroup.addFilter(GPUImageBrightnessFilter(0f))
        }
        light_image.filter = filterGroup
    }


    private fun initDataGroup() {
        this.mApplication = this.application as TelinkLightApplication
        val groupString = this.intent.extras!!.get("group")
        groupString ?: return
        this.group = groupString as DbGroup
        isAllGroup = 1 == (group?.id ?: 0).toInt()
        if (group != null)
            group = DBUtils.getGroupByID(group!!.id)
        if (isAllGroup) {
            /* val b = (group?.slowUpSlowDownStatus ?: 0) == 1
             slow_rg_close.isChecked = b
             if (b)
                 slow_rg_view.visibility = GONE
             else
                 slow_rg_view.visibility = VISIBLE*/
            slow_rg_view.visibility = GONE
            slow_rg_close.isChecked = group?.slowUpSlowDownStatus == 1
            if (group?.slowUpSlowDownStatus == 0)
                slow_rg_close.isChecked = true
            else
                when (group?.slowUpSlowDownSpeed) {
                    5 -> slow_rg_slow.isChecked = true
                    3 -> slow_rg_middle.isChecked = true
                    1 -> slow_rg_fast.isChecked = true
                }

            slow_ly.visibility = VISIBLE
        } else
            slow_ly.visibility = GONE

        if (Constants.IS_ROUTE_MODE) {
            routerConfigBrightnesssOrColorTemp(true)
            routerConfigBrightnesssOrColorTemp(false)
        } else {
            setLightBrightnessNum(group?.brightness ?: 1, group?.meshAddr!!)
            setLightTemperatureValue(group?.colorTemperature ?: 1, group?.meshAddr ?: 0)
        }
    }

    private fun setLightTemperatureValue(colorNum: Int, meshAddr: Int) {
        var opcode = Opcode.SET_TEMPERATURE
        var params = byteArrayOf(0x05, colorNum.toByte())
        setLightGUIImg(temperatureValue = colorNum)
        when (typeStr) {
            Constants.TYPE_GROUP -> {
                if (group?.status == 1)
                    TelinkLightService.Instance()?.sendCommandNoResponse(opcode, meshAddr, params)
            }
            else -> {
                if (light?.status == 1)
                    TelinkLightService.Instance()?.sendCommandNoResponse(opcode, meshAddr, params)
            }
        }
    }

    private fun setLightBrightnessNum(num: Int, meshAddr: Int) {
        var opcode = Opcode.SET_LUM
        var params = byteArrayOf(num.toByte())
        light_sbBrightness.progress = num
        setLightGUIImg(progress = num)
        if (typeStr == Constants.TYPE_GROUP) {
            if (group?.status == 1)
                TelinkLightService.Instance()?.sendCommandNoResponse(opcode, meshAddr, params)
        } else {
            if (light?.status == 1)
                TelinkLightService.Instance()?.sendCommandNoResponse(opcode, meshAddr, params)
        }
    }

    private val menuItemClickListener = Toolbar.OnMenuItemClickListener { item ->
        when (item?.itemId) {
            R.id.toolbar_f_rename -> renameLight()

            R.id.toolbar_fv_change_group -> updateGroup()

            R.id.toolbar_f_ota -> updateOTA()

            R.id.toolbar_f_delete -> remove()//删除设备

            R.id.toolbar_batch_gp -> {//群组模式下
                AlertDialog.Builder(Objects.requireNonNull<FragmentActivity>(this))
                        .setMessage(getString(R.string.delete_group_confirm, group?.name))
                        .setPositiveButton(android.R.string.ok) { _, _ ->
                            this.showLoadingDialog(getString(R.string.deleting))

                            deleteGroup(DBUtils.getLightByGroupID(group!!.id), group!!,
                                    successCallback = {
                                        this.hideLoadingDialog()
                                        this.setResult(Constants.RESULT_OK)
                                        this.finish()
                                    },
                                    failedCallback = {
                                        this.hideLoadingDialog()
                                        ToastUtils.showLong(R.string.move_out_some_lights_in_group_failed)
                                    })
                        }
                        .setNegativeButton(R.string.btn_cancel, null)
                        .show()
            }

            R.id.toolbar_on_line -> renameGroup()//群组模式下
        }
        true
    }

    private fun updateOTA() {
        when {
            !isSuportOta(light?.version) -> ToastUtils.showShort(getString(R.string.dissupport_ota))
            isMostNew(light?.version) -> ToastUtils.showShort(getString(R.string.the_last_version))
            else -> {
                when {
                    Constants.IS_ROUTE_MODE -> {
                        startActivity<RouterOtaActivity>("deviceMeshAddress" to light!!.meshAddr, "deviceType" to DeviceType.LIGHT_NORMAL, "deviceMac" to light!!.macAddr)
                        finish()
                    }
                    else -> when {
                        findItem?.title != null && findItem?.title != "version" -> checkPermission()
                        else -> Toast.makeText(this, R.string.number_no, Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    }


    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                imm.hideSoftInputFromWindow(editTitle?.windowToken, 0)
                editTitle?.isFocusableInTouchMode = false
                editTitle?.isFocusable = false
                finish()
            }
        }
        return super.onOptionsItemSelected(item)
    }

    @SuppressLint("ClickableViewAccessibility", "SetTextI18n")
    private fun initViewLight() {
        this.mApp = this.application as TelinkLightApplication?
        manager = DataManager(mApp, mApp!!.mesh.name, mApp!!.mesh.password)
        this.light = this.intent.extras!!.get(Constants.LIGHT_ARESS_KEY) as DbLight
        this.fromWhere = this.intent.getStringExtra(Constants.LIGHT_REFRESH_KEY)
        this.gpAddress = this.intent.getIntExtra(Constants.GROUP_ARESS_KEY, 0)
        toolbarTv.text = light?.name

        mRxPermission = RxPermissions(this)
        brightness_btn.setOnClickListener(clickListener)
        light_switch.setOnClickListener(clickListener)
        temperature_btn.setOnClickListener(clickListener)

        localVersion = when {
            TextUtils.isEmpty(light.version) -> getString(R.string.number_no)
            else -> light.version
        }

        if (light?.status == 1)
            light_switch.setImageResource(R.drawable.icon_light_open)
        else
            light_switch.setImageResource(R.drawable.icon_light_close)

        if (light!!.isSeek) {
            adjustment.text = getString(R.string.brightness_adjustment)
            brightness_btn.setImageResource(R.drawable.icon_btn)
            brightness_text.setTextColor(resources.getColor(R.color.blue_background))
            temperature_btn.setImageResource(R.drawable.icon_unselected)
            temperature_text.setTextColor(resources.getColor(R.color.black_nine))

            val brightnes = if (light!!.brightness < 1) 1 else light!!.brightness
            light_sbBrightness?.progress = brightnes
            tv_Brightness.text = "$brightnes%"
            when {
                Constants.IS_ROUTE_MODE -> routerConfigBrightnesssOrColorTemp(true)
                else -> setLightBrightnessNum(brightnes, light?.meshAddr)
            }
            isBrightness = true
        } else {
            adjustment.text = getString(R.string.color_temperature_adjustment)
            temperature_btn.setImageResource(R.drawable.icon_btn)
            temperature_text.setTextColor(resources.getColor(R.color.blue_background))
            brightness_btn.setImageResource(R.drawable.icon_unselected)
            brightness_text.setTextColor(resources.getColor(R.color.black_nine))

            val brightnes = if (light!!.colorTemperature < 1) 1 else light!!.colorTemperature
            light_sbBrightness?.progress = brightnes
            tv_Brightness.text = "$brightnes%"
            when {
                Constants.IS_ROUTE_MODE -> routerConfigBrightnesssOrColorTemp(false)
                else -> setLightTemperatureValue(light?.colorTemperature, light?.meshAddr)
            }
            isBrightness = false
        }

        this.light_sbBrightness?.max = 100
        if (light!!.connectionStatus == ConnectionStatus.OFF.value) {
            //light_image.setImageResource()
            setLightGUIImg()
            light_switch.setImageResource(R.drawable.icon_light_close)
            light_sbBrightness!!.setOnTouchListener { _, _ -> true }
            light_sbBrightness.isEnabled = false
            device_light_add.setImageResource(R.drawable.icon_puls_no)
            device_light_add.setOnTouchListener { _, _ -> false }
            device_light_minus.setOnTouchListener { _, _ -> false }
            device_light_minus.setImageResource(R.drawable.icon_minus_no)
            isSwitch = false
        } else {
            isSwitch = true
            //light_image.setImageResource(R.drawable.icon_light)
            setLightGUIImg()
            light_switch.setImageResource(R.drawable.icon_light_open)
            light_sbBrightness!!.setOnTouchListener { _, _ -> false }
            light_sbBrightness.isEnabled = true
            setAddAndMinusIcon(light!!.brightness)
            device_light_add.setOnTouchListener { _, event ->
                batchedAction(event, handlerAddBtn)
                true
            }
            device_light_minus.setOnTouchListener { _, event ->
                val progress: Int = if (currentShowPageGroup) {
                    light_sbBrightness.progress
                } else {
                    if (isBrightness)
                        light!!.brightness
                    else
                        light!!.colorTemperature
                }
                light_sbBrightness.progress = progress
                if (progress > 1)
                    batchedAction(event, handlerMinusBtn)
                true
            }
        }

        mConnectDevice = TelinkLightApplication.getApp().connectDevice

        light_sbBrightness?.setOnSeekBarChangeListener(barChangeListener)
    }

    private fun batchedAction(event: MotionEvent, handlerMinusOrAddBtn: Handler) {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                downTime = System.currentTimeMillis()
                onBtnTouch = true
                GlobalScope.launch {
                    while (onBtnTouch) {
                        thisTime = System.currentTimeMillis()
                        if (thisTime - downTime >= 500) {
                            if (Constants.IS_ROUTE_MODE)
                                routerConfigBrightnesssOrColorTemp(isBrightness)
                            handlerMinusOrAddBtn.sendMessage(Message())
                            delay(100)
                        }
                    }
                }
            }
            MotionEvent.ACTION_UP -> {
                onBtnTouch = false
                if (Constants.IS_ROUTE_MODE)
                    routerConfigBrightnesssOrColorTemp(isBrightness)
                if (thisTime - downTime < 500) {
                    handlerMinusOrAddBtn.sendMessage(Message())
                }
            }
            MotionEvent.ACTION_CANCEL -> onBtnTouch = false
        }
    }

    private fun routerConfigBrightnesssOrColorTemp(brightness: Boolean) = when {
        brightness -> {//亮度
            when {
                currentShowPageGroup && group != null -> routeConfigBriGpOrLight(group!!.meshAddr, 97, sendProgress, "gpBri")
                else -> routeConfigBriGpOrLight(light!!.meshAddr, light!!.productUUID, sendProgress, "lightBri")
            }
        }
        else -> {
            when {
                currentShowPageGroup && group != null -> routeConfigTempGpOrLight(group!!.meshAddr, 97, sendProgress, "gpTem")
                else -> routeConfigTempGpOrLight(light!!.meshAddr, light!!.productUUID, sendProgress, "lightTem")
            }
        }
    }


    @SuppressLint("CheckResult")
    private fun renameLight() {
        findItem?.title = localVersion
        hideLoadingDialog()
        StringUtils.initEditTextFilter(renameEt)
        renameEt?.setText(light.name)
        renameEt?.setSelection(renameEt?.text.toString().length)

        if (this != null && !isFinishing) {
            renameDialog?.dismiss()
            renameDialog?.show()
        }

        renameConfirm?.setOnClickListener {    // 获取输入框的内容
            if (StringUtils.compileExChar(renameEt?.text.toString().trim { it <= ' ' })) {
                ToastUtils.showLong(getString(R.string.rename_tip_check))
            } else {
                light?.name = renameEt?.text.toString().trim { it <= ' ' }
                if (Constants.IS_ROUTE_MODE) {
                    routerUpdateLightName(light.id, light?.name)
                } else {
                    renameSucess()
                }
                renameDialog?.dismiss()
            }
        }
    }


    override fun renameSucess() {
        DBUtils.updateLight(light!!)
        toolbarTv.text = light?.name
    }


    override fun onEditorAction(v: TextView?, actionId: Int, event: KeyEvent?): Boolean {
        doWhichOperation(actionId)
        return true
    }

    private fun doWhichOperation(actionId: Int) {
        when (actionId) {
            EditorInfo.IME_ACTION_DONE,
            EditorInfo.IME_ACTION_NONE -> {
                saveName()
                if (currentShowPageGroup) {
                    tvRename.visibility = GONE
                }
            }
        }
    }

    private fun saveName() {
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(editTitle?.windowToken, 0)
        editTitle?.isFocusableInTouchMode = false
        editTitle?.isFocusable = false
        if (!currentShowPageGroup) {
            checkAndSaveName()
            isRenameState = false
            tvOta.setText(R.string.ota)
        } else {
            checkAndSaveNameGp()
        }
    }

    private fun checkAndSaveNameGp() {
        val name = editTitle?.text.toString().trim()
        if (compileExChar(name)) {
            Toast.makeText(this, R.string.rename_tip_check, Toast.LENGTH_SHORT).show()

            editTitle.visibility = GONE
            titleCenterName.visibility = VISIBLE
            titleCenterName.text = group?.name
        } else {
            var canSave = true
            val groups = DBUtils.allGroups
            for (i in groups.indices) {
                if (groups[i].name == name) {
                    ToastUtils.showLong(TelinkLightApplication.getApp().getString(R.string.repeat_name))
                    canSave = false
                    break
                }
            }

            if (canSave) {
                editTitle.visibility = GONE
                titleCenterName.visibility = VISIBLE
                titleCenterName.text = name
                group?.name = name
                DBUtils.updateGroup(group!!)
            }
        }
    }

    private fun checkAndSaveName() {
        val name = editTitle?.text.toString().trim()
        if (compileExChar(name)) {
            Toast.makeText(this, R.string.rename_tip_check, Toast.LENGTH_SHORT).show()
            editTitle.visibility = GONE
            titleCenterName.visibility = VISIBLE
            titleCenterName.text = light?.name
        } else {
            editTitle.visibility = GONE
            titleCenterName.visibility = VISIBLE
            titleCenterName.text = name
            light?.name = name
            DBUtils.updateLight(light!!)
        }
    }

    private val barChangeListener = object : SeekBar.OnSeekBarChangeListener {

        private var preTime: Long = 0
        private val delayTime = Constants.MAX_SCROLL_DELAY_VALUE

        override fun onStopTrackingTouch(seekBar: SeekBar) {
            sendProgress = seekBar.progress
            if (Constants.IS_ROUTE_MODE) {
                sendProgress = seekBar.progress
                routerConfigBrightnesssOrColorTemp(isBrightness)
            } else {
                onValueChange(seekBar, seekBar.progress, true)
            }
        }

        override fun onStartTrackingTouch(seekBar: SeekBar) {
            if (isBrightness) {
                when {
                    currentShowPageGroup -> group?.brightness = seekBar.progress
                    else -> light?.brightness = seekBar.progress
                }
            } else {
                when {
                    currentShowPageGroup -> group?.colorTemperature = seekBar.progress
                    else -> light?.colorTemperature = seekBar.progress
                }
            }
            isProcessChange = 1
            preTime = System.currentTimeMillis()
            onValueChange(seekBar, seekBar!!.progress, false)
        }

        @SuppressLint("SetTextI18n")
        override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
            val currentTime = System.currentTimeMillis()
            tv_Brightness.text = seekBar!!.progress.toString() + "%"
            isProcessChange = 1
            if (currentTime - preTime > delayTime) {
                onValueChange(seekBar, progress, false)
                preTime = currentTime
            }
        }

        @SuppressLint("SetTextI18n")
        private fun onValueChange(view: View, progress: Int, isStopTracking: Boolean) {
            var addr: Int = if (currentShowPageGroup) {
                group?.meshAddr!!
            } else {
                light?.meshAddr!!
            }

            val opcode: Byte
            var params: ByteArray
            if (isProcessChange == 1) {
                if (isBrightness) {
                    if (view == light_sbBrightness) {
                        opcode = Opcode.SET_LUM
                        params = if (progress < 1)
                            byteArrayOf(1.toByte())
                        else
                            byteArrayOf(progress.toByte())


                        if (currentShowPageGroup) {
                            if (group?.brightness != progress) {
                                if (progress > Constants.MAX_VALUE) {
                                    params = byteArrayOf(Constants.MAX_VALUE.toByte())
                                    setLightGUIImg(progress = Constants.MAX_VALUE)
                                    TelinkLightService.Instance()?.sendCommandNoResponse(opcode, addr, params)
                                } else {
                                    setLightGUIImg(progress = progress)
                                    TelinkLightService.Instance()?.sendCommandNoResponse(opcode, addr, params)
                                }
                            }
                        } else {
                            if (light?.brightness != progress) {
                                if (progress > Constants.MAX_VALUE) {
                                    params = byteArrayOf(Constants.MAX_VALUE.toByte())
                                    setLightGUIImg(progress = Constants.MAX_VALUE)
                                    TelinkLightService.Instance()?.sendCommandNoResponse(opcode, addr, params)
                                } else {
                                    setLightGUIImg(progress = progress)
                                    TelinkLightService.Instance()?.sendCommandNoResponse(opcode, addr, params)
                                }
                            }
                        }

                        if (currentShowPageGroup) {
                            group?.brightness = progress
                            setAddAndMinusIcon(progress)
                        } else {
                            if (!Constants.IS_ROUTE_MODE)
                                light?.brightness = progress
                            setAddAndMinusIcon(progress)
                        }
                        Log.e("TAG", progress.toString())


                        if (isStopTracking) {
                            if (currentShowPageGroup) {
                                if (group != null) {//禁止为0
                                    tv_Brightness.text = group!!.brightness.toString() + "%"
                                    if (!Constants.IS_ROUTE_MODE) {
                                        group!!.brightness = if (group!!.brightness < 1) 1 else this@NormalSettingActivity.group!!.brightness
                                        DBUtils.updateGroup(group!!)
                                    }
                                    updateLights(progress, "brightness", group!!)
                                    isProcessChange = 0
                                }
                            } else {
                                if (light != null) {  //禁止为0
                                    if (!Constants.IS_ROUTE_MODE) {
                                        light.brightness = if (light!!.brightness < 1) 1 else light!!.brightness
                                        DBUtils.updateLight(light)
                                    }
                                    tv_Brightness.text = light.brightness.toString() + "%"
                                    isProcessChange = 0
                                }
                            }
                        }

                    }
                } else {
                    if (view == light_sbBrightness) {
                        opcode = Opcode.SET_TEMPERATURE
                        params = byteArrayOf(0x05, progress.toByte())

                        if (currentShowPageGroup) {
                            if (group?.colorTemperature != progress) {
                                Log.e("TAG", progress.toString())
                                setLightGUIImg(temperatureValue = progress)
                                TelinkLightService.Instance()?.sendCommandNoResponse(opcode, addr, params)
                            }
                        } else {
                            if (light?.colorTemperature != progress) {
                                Log.e("TAG", progress.toString())
                                setLightGUIImg(temperatureValue = progress)
                                TelinkLightService.Instance()?.sendCommandNoResponse(opcode, addr, params)
                            }
                        }

                        if (currentShowPageGroup) {
                            group?.colorTemperature = progress
                        } else {
                            light?.colorTemperature = progress
                        }

                        setAddAndMinusIcon(progress)

                        Log.e("TAG", progress.toString())

                        if (isStopTracking) {
                            if (currentShowPageGroup) {
                                var groupCurrent = DBUtils.getGroupByID(group!!.id)
                                if (groupCurrent != null) {
                                    groupCurrent.colorTemperature = group!!.colorTemperature
                                    tv_Brightness.text = groupCurrent.colorTemperature.toString() + "%"
                                    DBUtils.updateGroup(groupCurrent)
                                    isProcessChange = 0
                                }
                            } else {
                                var dbLight = DBUtils.getLightByID(light!!.id)
                                if (dbLight != null) {
                                    if (!Constants.IS_ROUTE_MODE) {
                                        light.colorTemperature = dbLight!!.colorTemperature
                                        DBUtils.updateLight(dbLight)
                                    }
                                    tv_Brightness.text = dbLight.colorTemperature.toString() + "%"
                                    isProcessChange = 0
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun setAddAndMinusIcon(progress: Int) {
        when {
            progress <= 1 -> {
                device_light_minus.setImageResource(R.drawable.icon_minus_no)
                device_light_add.setImageResource(R.drawable.icon_puls)
            }
            progress >= 100 -> {
                device_light_add.setImageResource(R.drawable.icon_puls_no)
                device_light_minus.setImageResource(R.drawable.icon_minus)
            }
            else -> {
                device_light_minus.setImageResource(R.drawable.icon_minus)
                device_light_add.setImageResource(R.drawable.icon_puls)
            }
        }
    }

    private fun updateLights(progress: Int, type: String, group: DbGroup) {
        GlobalScope.launch {
            var lightList: MutableList<DbLight> = ArrayList()

            if (group.meshAddr == 0xffff) {
                val list = DBUtils.groupList
                for (j in list.indices) {
                    lightList.addAll(DBUtils.getLightByGroupID(list[j].id))
                }
            } else {
                lightList = DBUtils.getLightByGroupID(group.id)
            }

            for (dbLight: DbLight in lightList) {
                if (type == "brightness") {
                    dbLight.brightness = progress
                } else if (type == "colorTemperature") {
                    dbLight.colorTemperature = progress
                }
                DBUtils.updateLight(dbLight)
            }
        }
    }


    fun remove() {
        if (Constants.IS_ROUTE_MODE) {
            routerDeviceResetFactory(light!!.macAddr, light!!.meshAddr, light!!.productUUID, "lightFactory")
        } else {
            if (TelinkLightService.Instance()?.adapter!!.mLightCtrl.currentLight != null) {
                AlertDialog.Builder(Objects.requireNonNull<AppCompatActivity>(this)).setMessage(getString(R.string.sure_delete_device2))
                        .setPositiveButton(android.R.string.ok) { _, _ ->

                            if (TelinkLightService.Instance()?.adapter!!.mLightCtrl.currentLight != null && TelinkLightService.Instance()?.adapter!!.mLightCtrl.currentLight.isConnected) {
                                showLoadingDialog(getString(R.string.please_wait))
                                val disposable = Commander.resetDevice(light!!.meshAddr)
                                        .subscribe(
                                                {
                                                    //deleteData()
                                                }, {
                                            /*   showDialogHardDelete?.dismiss()
                                               showDialogHardDelete = android.app.AlertDialog.Builder(this).setMessage(R.string.delete_device_hard_tip)
                                                       .setPositiveButton(android.R.string.ok) { _, _ ->
                                                           showLoadingDialog(getString(R.string.please_wait))
                                                           deleteData()
                                                       }
                                                       .setNegativeButton(R.string.btn_cancel, null)
                                                       .show()*/
                                        })
                                deleteData()
                            } else {
                                ToastUtils.showLong(getString(R.string.bluetooth_open_connet))
                                finish()
                            }
                        }
                        .setNegativeButton(R.string.btn_cancel, null)
                        .show()
            } else {
                autoConnectAll()
            }
        }
    }

    override fun tzRouterResetFactory(cmdBean: CmdBodyBean) {
        if (cmdBean.ser_id == "lightFactory") {
            LogUtils.v("zcl-----------收到路由恢复出厂得到通知-------$cmdBean")
            hideLoadingDialog()
            disposableRouteTimer?.dispose()
            if (cmdBean.status == 0)
                deleteData()
            else
                ToastUtils.showShort(getString(R.string.reset_factory_fail))
        }
    }

    fun deleteData() {
        LogUtils.v("zcl-----恢复出厂成功")
        hideLoadingDialog()
        ToastUtils.showShort(getString(R.string.successful_resumption))
        DBUtils.deleteLight(light!!)
        if (TelinkLightApplication.getApp().mesh.removeDeviceByMeshAddress(light!!.meshAddr))
            TelinkLightApplication.getApp().mesh.saveOrUpdate(this)

        if (mConnectDevice != null) {
            Log.d(javaClass.simpleName, "mConnectDevice.meshAddress = " + mConnectDevice?.meshAddress)
            Log.d(javaClass.simpleName, "light.getMeshAddr() = " + light?.meshAddr)
            if (light?.meshAddr == mConnectDevice?.meshAddress) {
                setResult(Activity.RESULT_OK, Intent().putExtra("data", true))
            }
        }
        SyncDataPutOrGetUtils.syncPutDataStart(this, object : SyncCallback {
            override fun start() {}

            override fun complete() {}

            override fun error(msg: String?) {}
        })
        finish()
    }


    override fun onBackPressed() {
        super.onBackPressed()
        setResult(Constants.RESULT_OK)
        finish()
    }

    override fun onClick(v: View?) {
        val group = DBUtils.getGroupByID(group!!.id)
        when (v?.id) {
            R.id.slow_rg_slow -> {
                tempSpeed = 5
                if (Constants.IS_ROUTE_MODE) {
                    routerSwitchSlowUpSlowDown(1, "slowOpen")
                } else {
                    slowOrUpOpen()
                    afterSendSpeed(group, 5)
                }
            }
            R.id.slow_rg_middle -> {
                tempSpeed = 3
                if (Constants.IS_ROUTE_MODE) {
                    routerSwitchSlowUpSlowDown(1, "slowOpen")
                } else {
                    slowOrUpOpen()
                    afterSendSpeed(group, 3)
                }
            }
            R.id.slow_rg_fast -> {
                tempSpeed = 1
                if (Constants.IS_ROUTE_MODE) {
                    routerSwitchSlowUpSlowDown(1, "slowOpen")
                } else {
                    slowOrUpOpen()
                    afterSendSpeed(group, 1)
                }
            }
            R.id.slow_rg_close -> {
                when {
                    Constants.IS_ROUTE_MODE -> routerSwitchSlowUpSlowDown(2, "slowClose")
                    else -> slowOrUpClose()
                }
            }
        }
    }

    private fun afterSendSpeed(group: DbGroup?, speed: Int) {
        group?.slowUpSlowDownStatus = 1
        group?.slowUpSlowDownSpeed = speed
        var string = when (speed) {
            5 -> getString(R.string.slow)
            3 -> getString(R.string.mid)
            1 -> getString(R.string.fast)
            else -> getString(R.string.fast)
        }
        ToastUtils.showShort(string)
    }

    @SuppressLint("CheckResult")
    private fun routerSwitchSlowUpSlowDown(status: Int, serID: String) {//slowUpSlowDownStatus 1开0关;  1开 2关 (路由特别注意2才是关)
        RouterModel.routeSlowUpSlowDownSw(status, serID)?.subscribe({
            LogUtils.v("zcl-----------收到路由缓起缓灭开关结果-$status-----$it-")
            when (it.errorCode) {
                0 -> {
                    showLoadingDialog(getString(R.string.please_wait))
                    disposableRouteTimer?.dispose()
                    disposableRouteTimer = Observable.timer(it.t.timeout.toLong(), TimeUnit.SECONDS)
                            .subscribe {
                                hideLoadingDialog()
                                if (status == 2)
                                    ToastUtils.showShort(getString(R.string.slow_close_faile))
                                else
                                    ToastUtils.showShort(getString(R.string.slow_open_faile))
                            }
                }
                90005 -> ToastUtils.showShort(getString(R.string.router_offline))
                else -> ToastUtils.showShort(it.message)
            }
        }, {
            ToastUtils.showShort(it.message)
        })
    }

    override fun tzRouterSSSW(cmdBean: CmdBodyBean, b: Boolean) {
        disposableRouteTimer?.dispose()
        when (cmdBean.ser_id) {
            "slowClose" -> {
                LogUtils.v("zcl-----------收到路由缓起缓灭关闭通知------$cmdBean-")
                hideLoadingDialog()
                when (cmdBean.status) {
                    0 -> ToastUtils.showShort(getString(R.string.slow_close_success))
                    else -> ToastUtils.showShort(getString(R.string.slow_close_faile))
                }
            }
            "slowOpen" -> {
                LogUtils.v("zcl-----------收到路由缓起缓灭开启通知------$cmdBean-")
                when (cmdBean.status) {
                    0 -> {
                        routerSendSlowSpeed()
                    }
                    else -> {
                        hideLoadingDialog()
                        ToastUtils.showShort(getString(R.string.slow_close_faile))
                    }
                }
            }
        }
    }

    @SuppressLint("CheckResult")
    private fun routerSendSlowSpeed() {
        RouterModel.routeSlowUpSlowDownSpeed(tempSpeed, "slowSpeed")?.subscribe({
            LogUtils.v("zcl-----------收到路由发送速度结果-------$it")
            when (it.errorCode) {
                0 -> {
                    showLoadingDialog(getString(R.string.please_wait))
                    disposableRouteTimer?.dispose()
                    disposableRouteTimer = Observable.timer(it.t.timeout.toLong(), TimeUnit.SECONDS)
                            .subscribe {
                                hideLoadingDialog()
                                ToastUtils.showShort(getString(R.string.slow_speed_faile))
                            }
                }
                90005 -> ToastUtils.showShort(getString(R.string.router_offline))
                else -> ToastUtils.showShort(it.message)
            }
        }, {
            ToastUtils.showShort(it.message)
        })
    }

    override fun tzRouterSSSpeed(cmdBean: CmdBodyBean) {
        if (cmdBean.ser_id == "slowSpeed") {
            LogUtils.v("zcl-----------收到路由调节速度通知-------$cmdBean")
            hideLoadingDialog()
            when (cmdBean.status) {
                0 -> afterSendSpeed(group, tempSpeed)
                else -> {
                    when (group?.slowUpSlowDownSpeed ?: 1) {
                        1 -> slow_rg_fast.isChecked = true
                        3 -> slow_rg_middle.isChecked = true
                        5 -> slow_rg_slow.isChecked = true
                    }
                    ToastUtils.showShort(getString(R.string.slow_speed_faile))
                }
            }
        }
    }

    override fun tzRouterUpdateVersionRecevice(routerVersion: RouteGetVerBean?) {
        if (routerVersion?.ser_id == "cwVersion") {
            LogUtils.v("zcl-----------收到路由版本通知-------$routerVersion")
            hideLoadingDialog()
            disposableRouteTimer?.dispose()
            when (routerVersion.status) {
                0 -> {
                    SyncDataPutOrGetUtils.syncGetDataStart(lastUser!!, syncCallbackGet)
                    val light = DBUtils.getLightByMeshAddr(light.meshAddr)
                    updateVersion(light?.version)
                }
                else -> {
                    ToastUtils.showShort(getString(R.string.get_version_fail))
                }
            }
        }
    }

    private fun afterSendClose() {
        val group = DBUtils.getGroupByID(group!!.id)
        group?.slowUpSlowDownStatus = 0
        DBUtils.updateGroup(group!!)
    }
}
