package com.dadoutek.uled.light

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Message
import android.support.v4.app.FragmentActivity
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.Toolbar
import android.text.TextUtils
import android.util.Log
import android.view.*
import android.view.View.OnClickListener
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import com.blankj.utilcode.util.ToastUtils
import com.dadoutek.uled.R
import com.dadoutek.uled.communicate.Commander
import com.dadoutek.uled.group.ChooseGroupForDevice
import com.dadoutek.uled.intf.OtaPrepareListner
import com.dadoutek.uled.model.Constant
import com.dadoutek.uled.model.DbModel.DBUtils
import com.dadoutek.uled.model.DbModel.DbGroup
import com.dadoutek.uled.model.DbModel.DbLight
import com.dadoutek.uled.model.Opcode
import com.dadoutek.uled.model.SharedPreferencesHelper
import com.dadoutek.uled.network.NetworkFactory
import com.dadoutek.uled.ota.OTAUpdateActivity
import com.dadoutek.uled.tellink.TelinkBaseActivity
import com.dadoutek.uled.tellink.TelinkLightApplication
import com.dadoutek.uled.tellink.TelinkLightService
import com.dadoutek.uled.util.*
import com.tbruyelle.rxpermissions2.RxPermissions
import com.telink.TelinkApplication
import com.telink.bluetooth.event.DeviceEvent
import com.telink.bluetooth.event.ErrorReportEvent
import com.telink.bluetooth.light.*
import com.telink.util.Event
import com.telink.util.EventListener
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_device_setting.*
import kotlinx.android.synthetic.main.fragment_device_setting.*
import kotlinx.android.synthetic.main.toolbar.*
import java.util.*
import java.util.concurrent.TimeUnit

class NormalSettingActivity : TelinkBaseActivity(), EventListener<String>, TextView.OnEditorActionListener {
    private var localVersion: String? = null
    private var light: DbLight? = null
    private val mDisposable = CompositeDisposable()
    private var mRxPermission: RxPermissions? = null
    var gpAddress: Int = 0
    var fromWhere: String? = null
    private var updateLightDisposal: Disposable? = null
    private var mApp: TelinkLightApplication? = null
    private var manager: DataManager? = null
    private var mConnectDevice: DeviceInfo? = null
    private var mConnectTimer: Disposable? = null
    private var isLoginSuccess = false
    private var mApplication: TelinkLightApplication? = null
    private var isRenameState = false
    private var group: DbGroup? = null
    private var currentShowPageGroup = true
    private var isBrightness = true

    internal var downTime: Long = 0//Button被按下时的时间
    internal var thisTime: Long = 0//while每次循环时的时间
    internal var onBtnTouch = false//Button是否被按下
    internal var tvValue = 0//TextView中的值

    private var clickNum = 0

    private var isSwitch: Boolean = false

    private val clickListener = OnClickListener { v ->
        when (v.id) {
            R.id.tvOta -> {
                if (isRenameState) {
                    saveName()
                } else {
                    checkPermission()
                }
            }
            R.id.btnRename -> {
                renameGp()
            }
            R.id.updateGroup -> {
                updateGroup()
            }
            R.id.btnRemove -> {
                remove()
            }
            R.id.btn_remove_group -> AlertDialog.Builder(Objects.requireNonNull<FragmentActivity>(this)).setMessage(R.string.delete_group_confirm)
                    .setPositiveButton(android.R.string.ok) { _, _ ->
                        this.showLoadingDialog(getString(R.string.deleting))

                        deleteGroup(DBUtils.getLightByGroupID(group!!.id), group!!,
                                successCallback = {
                                    this.hideLoadingDialog()
                                    this?.setResult(Constant.RESULT_OK)
                                    this?.finish()
                                },
                                failedCallback = {
                                    this.hideLoadingDialog()
                                    ToastUtils.showShort(R.string.move_out_some_lights_in_group_failed)
                                })
                    }
                    .setNegativeButton(R.string.btn_cancel, null)
                    .show()
            R.id.btn_rename -> renameGroup()

            R.id.light_switch -> lightSwitch()

            R.id.brightness_btn -> setBrightness()

            R.id.temperature_btn -> setTemperature()
        }
    }

    @SuppressLint("ResourceAsColor")
    private fun setTemperature() {
        adjustment.text = getString(R.string.color_temperature_adjustment)
        if (currentShowPageGroup) {
            temperature_btn.setImageResource(R.drawable.icon_btn)
            temperature_text.setTextColor(resources.getColor(R.color.blue_background))
            brightness_btn.setImageResource(R.drawable.icon_unselected)
            brightness_text.setTextColor(resources.getColor(R.color.black_nine))
            if (isSwitch) {
                isBrightness = false
//                    clickNum = 1
                var light_current = DBUtils.getGroupByID(group!!.id)
                if (light_current != null) {
                    light_sbBrightness?.progress = light_current.colorTemperature
                    light_current.isSeek = false
                    tv_Brightness.text = light_current.colorTemperature.toString() + "%"
                    Log.e("TAG_SET_C", light_current.colorTemperature.toString())
                    if (light_current!!.connectionStatus == ConnectionStatus.OFF.value) {
                        device_light_add.setImageResource(R.drawable.icon_puls_no)
                        device_light_minus.setImageResource(R.drawable.icon_minus_no)
                    } else {
                        if (light_current.colorTemperature <= 0) {
                            device_light_minus.setImageResource(R.drawable.icon_minus_no)
                            device_light_add.setImageResource(R.drawable.icon_puls)
                        } else if (light_current.colorTemperature >= 100) {
                            device_light_add.setImageResource(R.drawable.icon_puls_no)
                            device_light_minus.setImageResource(R.drawable.icon_minus)
                        } else {
                            device_light_minus.setImageResource(R.drawable.icon_minus)
                            device_light_add.setImageResource(R.drawable.icon_puls)
                        }

                    }
                }
            }
        } else {
            temperature_btn.setImageResource(R.drawable.icon_btn)
            temperature_text.setTextColor(resources.getColor(R.color.blue_background))
            brightness_btn.setImageResource(R.drawable.icon_unselected)
            brightness_text.setTextColor(resources.getColor(R.color.black_nine))
            if (isSwitch) {
                isBrightness = false
//                    clickNum = 1
                var light_current = DBUtils.getLightByID(light!!.id)
                if (light_current != null) {
                    light_sbBrightness?.progress = light_current.colorTemperature
                    light_current.isSeek = false
                    tv_Brightness.text = light_current.colorTemperature.toString() + "%"
                    Log.e("TAG_SET_C", light_current.colorTemperature.toString())

                    if (light_current!!.connectionStatus == ConnectionStatus.OFF.value) {
                        device_light_add.setImageResource(R.drawable.icon_puls_no)
                        device_light_minus.setImageResource(R.drawable.icon_minus_no)
                    } else {
                        if (light_current.colorTemperature <= 0) {
                            device_light_minus.setImageResource(R.drawable.icon_minus_no)
                            device_light_add.setImageResource(R.drawable.icon_puls)
                        } else if (light_current.colorTemperature >= 100) {
                            device_light_add.setImageResource(R.drawable.icon_puls_no)
                            device_light_minus.setImageResource(R.drawable.icon_minus)
                        } else {
                            device_light_minus.setImageResource(R.drawable.icon_minus)
                            device_light_add.setImageResource(R.drawable.icon_puls)
                        }
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
            if (isSwitch) {
                isBrightness = true
                var light_current = DBUtils.getGroupByID(group!!.id)
                if (light_current != null) {
                    light_sbBrightness?.progress = light_current.brightness
                    light_current.isSeek = true
                    tv_Brightness.text = light_current.brightness.toString() + "%"
                    Log.e("TAG_SET_B", light_current.brightness.toString())
                    if (light_current!!.connectionStatus == ConnectionStatus.OFF.value) {
                        device_light_add.setImageResource(R.drawable.icon_puls_no)
                        device_light_minus.setImageResource(R.drawable.icon_minus_no)
                    } else {
                        if (light_current.brightness <= 0) {
                            device_light_minus.setImageResource(R.drawable.icon_minus_no)
                            device_light_add.setImageResource(R.drawable.icon_puls)
                        } else if (light_current.brightness >= 100) {
                            device_light_add.setImageResource(R.drawable.icon_puls_no)
                            device_light_minus.setImageResource(R.drawable.icon_minus)
                        } else {
                            device_light_minus.setImageResource(R.drawable.icon_minus)
                            device_light_add.setImageResource(R.drawable.icon_puls)
                        }
                    }
                }
            }
        } else {
            brightness_btn.setImageResource(R.drawable.icon_btn)
            brightness_text.setTextColor(resources.getColor(R.color.blue_background))
            temperature_btn.setImageResource(R.drawable.icon_unselected)
            temperature_text.setTextColor(resources.getColor(R.color.black_nine))
            if (isSwitch) {
                isBrightness = true
                var light_current = DBUtils.getLightByID(light!!.id)
                if (light_current != null) {
                    light_sbBrightness?.progress = light_current.brightness
                    light_current.isSeek = true
                    tv_Brightness.text = light_current.brightness.toString() + "%"
                    Log.e("TAG_SET_B", light_current.brightness.toString())
                    if (light_current!!.connectionStatus == ConnectionStatus.OFF.value) {
                        device_light_add.setImageResource(R.drawable.icon_puls_no)
                        device_light_minus.setImageResource(R.drawable.icon_minus_no)
                    } else {
                        when {
                            light_current.brightness <= 0 -> {
                                device_light_minus.setImageResource(R.drawable.icon_minus_no)
                                device_light_add.setImageResource(R.drawable.icon_puls)
                            }
                            light_current.brightness >= 100 -> {
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
            var light_current = DBUtils.getGroupByID(group!!.id)
            if (group!!.id.toInt() == 1) {
                SharedPreferencesUtils.setAllLightModel(true)
            } else {
                SharedPreferencesUtils.setAllLightModel(false)
            }
            if (light_current != null) {
                if (light_current.connectionStatus == ConnectionStatus.OFF.value) {
                    Commander.openOrCloseLights(light_current.meshAddr, true)
                    isSwitch = true
                    light_switch.setImageResource(R.drawable.icon_light_open)
                    light_image.setImageResource(R.drawable.icon_light)
                    light_current.connectionStatus = ConnectionStatus.ON.value
                    light_sbBrightness!!.setOnTouchListener { v, event -> false }
                    light_sbBrightness.isEnabled = true
                    if (isBrightness) {
                        if (light_current!!.brightness <= 0) {
                            device_light_minus.setImageResource(R.drawable.icon_minus_no)
                            device_light_add.setImageResource(R.drawable.icon_puls)
                        } else if (light_current.brightness >= 100) {
                            device_light_add.setImageResource(R.drawable.icon_puls_no)
                            device_light_minus.setImageResource(R.drawable.icon_minus)
                        } else {
                            device_light_minus.setImageResource(R.drawable.icon_minus)
                            device_light_add.setImageResource(R.drawable.icon_puls)
                        }
                    } else {
                        if (light_current!!.colorTemperature <= 0) {
                            device_light_minus.setImageResource(R.drawable.icon_minus_no)
                            device_light_add.setImageResource(R.drawable.icon_puls)
                        } else if (light_current.colorTemperature >= 100) {
                            device_light_add.setImageResource(R.drawable.icon_puls_no)
                            device_light_minus.setImageResource(R.drawable.icon_minus)
                        } else {
                            device_light_minus.setImageResource(R.drawable.icon_minus)
                            device_light_add.setImageResource(R.drawable.icon_puls)
                        }
                    }
                    DBUtils.updateGroup(light_current)
                    updateLights(true, light_current)

                    device_light_add.setOnTouchListener { v, event ->
                        if (event.action == MotionEvent.ACTION_DOWN) {
                            //                    tvValue = Integer.parseInt(textView.getText().toString());
                            downTime = System.currentTimeMillis()
                            onBtnTouch = true
                            val t = object : Thread() {
                                override fun run() {
                                    while (onBtnTouch) {
                                        thisTime = System.currentTimeMillis()
                                        if (thisTime - downTime >= 500) {
                                            tvValue++
                                            val msg = handler.obtainMessage()
                                            msg.arg1 = tvValue
                                            handler_handler.sendMessage(msg)
                                            Log.e("TAG_TOUCH", tvValue++.toString())
                                            try {
                                                sleep(100)
                                            } catch (e: InterruptedException) {
                                                e.printStackTrace()
                                            }

                                        }
                                    }
                                }
                            }
                            t.start()
                        } else if (event.action == MotionEvent.ACTION_UP) {
                            onBtnTouch = false
                            if (thisTime - downTime < 500) {
                                tvValue++
                                val msg = handler.obtainMessage()
                                msg.arg1 = tvValue
                                handler_handler.sendMessage(msg)
                            }
                        } else if (event.action == MotionEvent.ACTION_CANCEL) {
                            onBtnTouch = false
                        }
                        true
                    }

                    device_light_minus.setOnTouchListener { v, event ->
                        if (event.action == MotionEvent.ACTION_DOWN) {
                            //                    tvValue = Integer.parseInt(textView.getText().toString());
                            downTime = System.currentTimeMillis()
                            onBtnTouch = true
                            val t = object : Thread() {
                                override fun run() {
                                    while (onBtnTouch) {
                                        thisTime = System.currentTimeMillis()
                                        if (thisTime - downTime >= 500) {
                                            tvValue++
                                            val msg = handler.obtainMessage()
                                            msg.arg1 = tvValue
                                            handler.sendMessage(msg)
                                            Log.e("TAG_TOUCH", tvValue++.toString())
                                            try {
                                                sleep(100)
                                            } catch (e: InterruptedException) {
                                                e.printStackTrace()
                                            }

                                        }
                                    }
                                }
                            }
                            t.start()
                        } else if (event.action == MotionEvent.ACTION_UP) {
                            onBtnTouch = false
                            if (thisTime - downTime < 500) {
                                tvValue++
                                val msg = handler.obtainMessage()
                                msg.arg1 = tvValue
                                handler.sendMessage(msg)
                            }
                        } else if (event.action == MotionEvent.ACTION_CANCEL) {
                            onBtnTouch = false
                        }
                        true
                    }
                } else {
                    Commander.openOrCloseLights(light_current.meshAddr, false)
                    light_current.connectionStatus = ConnectionStatus.OFF.value
                    light_switch.setImageResource(R.drawable.icon_light_close)
                    light_image.setImageResource(R.drawable.light_close)
                    light_sbBrightness!!.setOnTouchListener { v, event -> true }
                    light_sbBrightness.isEnabled = false
                    device_light_add.setImageResource(R.drawable.icon_puls_no)
                    device_light_minus.setImageResource(R.drawable.icon_minus_no)
                    device_light_add.setOnTouchListener { v, event -> false }
                    device_light_minus.setOnTouchListener { v, event -> false }
                    isSwitch = false
                    DBUtils.updateGroup(light_current)
                    updateLights(false, light_current)
                }
            }
        } else {
            var light_current = DBUtils.getLightByID(light!!.id)
            if (light_current != null) {
                if (light_current.connectionStatus == ConnectionStatus.OFF.value) {
                    isSwitch = true
                    Commander.openOrCloseLights(light_current.meshAddr, true)
                    light_switch.setImageResource(R.drawable.icon_light_open)
                    light_image.setImageResource(R.drawable.icon_light)
                    light_current.connectionStatus = ConnectionStatus.ON.value
                    light_sbBrightness!!.setOnTouchListener { v, event -> false }
                    light_sbBrightness.isEnabled = true
                    if (isBrightness) {
                        if (light_current!!.brightness <= 0) {
                            device_light_minus.setImageResource(R.drawable.icon_minus_no)
                            device_light_add.setImageResource(R.drawable.icon_puls)
                        } else if (light_current.brightness >= 100) {
                            device_light_add.setImageResource(R.drawable.icon_puls_no)
                            device_light_minus.setImageResource(R.drawable.icon_minus)
                        } else {
                            device_light_minus.setImageResource(R.drawable.icon_minus)
                            device_light_add.setImageResource(R.drawable.icon_puls)
                        }
                    } else {
                        if (light_current!!.colorTemperature <= 0) {
                            device_light_minus.setImageResource(R.drawable.icon_minus_no)
                            device_light_add.setImageResource(R.drawable.icon_puls)
                        } else if (light_current.colorTemperature >= 100) {
                            device_light_add.setImageResource(R.drawable.icon_puls_no)
                            device_light_minus.setImageResource(R.drawable.icon_minus)
                        } else {
                            device_light_minus.setImageResource(R.drawable.icon_minus)
                            device_light_add.setImageResource(R.drawable.icon_puls)
                        }
                    }
                    device_light_add.setOnTouchListener { v, event ->
                        if (event.action == MotionEvent.ACTION_DOWN) {
                            //                    tvValue = Integer.parseInt(textView.getText().toString());
                            downTime = System.currentTimeMillis()
                            onBtnTouch = true
                            val t = object : Thread() {
                                override fun run() {
                                    while (onBtnTouch) {
                                        thisTime = System.currentTimeMillis()
                                        if (thisTime - downTime >= 500) {
                                            tvValue++
                                            val msg = handler.obtainMessage()
                                            msg.arg1 = tvValue
                                            handler_handler.sendMessage(msg)
                                            Log.e("TAG_TOUCH", tvValue++.toString())
                                            try {
                                                sleep(100)
                                            } catch (e: InterruptedException) {
                                                e.printStackTrace()
                                            }

                                        }
                                    }
                                }
                            }
                            t.start()
                        } else if (event.action == MotionEvent.ACTION_UP) {
                            onBtnTouch = false
                            if (thisTime - downTime < 500) {
                                tvValue++
                                val msg = handler.obtainMessage()
                                msg.arg1 = tvValue
                                handler_handler.sendMessage(msg)
                            }
                        } else if (event.action == MotionEvent.ACTION_CANCEL) {
                            onBtnTouch = false
                        }
                        true
                    }

                    device_light_minus.setOnTouchListener { v, event ->
                        if (event.action == MotionEvent.ACTION_DOWN) {
                            //                    tvValue = Integer.parseInt(textView.getText().toString());
                            downTime = System.currentTimeMillis()
                            onBtnTouch = true
                            val t = object : Thread() {
                                override fun run() {
                                    while (onBtnTouch) {
                                        thisTime = System.currentTimeMillis()
                                        if (thisTime - downTime >= 500) {
                                            tvValue++
                                            val msg = handler.obtainMessage()
                                            msg.arg1 = tvValue
                                            handler.sendMessage(msg)
                                            Log.e("TAG_TOUCH", tvValue++.toString())
                                            try {
                                                sleep(100)
                                            } catch (e: InterruptedException) {
                                                e.printStackTrace()
                                            }

                                        }
                                    }
                                }
                            }
                            t.start()
                        } else if (event.action == MotionEvent.ACTION_UP) {
                            onBtnTouch = false
                            if (thisTime - downTime < 500) {
                                tvValue++
                                val msg = handler.obtainMessage()
                                msg.arg1 = tvValue
                                handler.sendMessage(msg)
                            }
                        } else if (event.action == MotionEvent.ACTION_CANCEL) {
                            onBtnTouch = false
                        }
                        true
                    }
                } else {
                    isSwitch = false
                    Commander.openOrCloseLights(light_current.meshAddr, false)
                    light_current.connectionStatus = ConnectionStatus.OFF.value
                    light_switch.setImageResource(R.drawable.icon_light_close)
                    light_image.setImageResource(R.drawable.light_close)
                    light_sbBrightness!!.setOnTouchListener { v, event -> true }
                    light_sbBrightness.isEnabled = false
                    device_light_add.setImageResource(R.drawable.icon_puls_no)
                    device_light_minus.setImageResource(R.drawable.icon_minus_no)
                    device_light_add.setOnTouchListener { v, event -> false }
                    device_light_minus.setOnTouchListener { v, event -> false }
                }
                DBUtils.updateLight(light_current)
            }
        }
    }

    @SuppressLint("HandlerLeak")
    private val handler = object : Handler() {
        override fun handleMessage(msg: Message) {
            super.handleMessage(msg)
            //            textView.setText(String.valueOf(msg.arg1));
            if (clickNum == 0) {
                light_sbBrightness.progress--
                if (light_sbBrightness.progress <= 0) {
//                device_light_minus.setOnTouchListener { v, event -> false }
                    device_light_minus.setImageResource(R.drawable.icon_minus_no)
                } else {
                    tv_Brightness.text = light_sbBrightness.progress.toString() + "%"
                }

                if (light_sbBrightness.progress < 100) {
                    device_light_add.setImageResource(R.drawable.icon_puls)
                }
            } else {
                if (isBrightness) {
                    light_sbBrightness.progress--
                    if (light_sbBrightness.progress < 0) {
                        device_light_minus.setImageResource(R.drawable.icon_minus_no)
                    } else if (light_sbBrightness.progress == 0) {
                        device_light_minus.setImageResource(R.drawable.icon_minus_no)
                        tv_Brightness.text = light_sbBrightness.progress.toString() + "%"
                        if (currentShowPageGroup) {
                            val light_current = DBUtils.getGroupByID(group!!.id)
                            if (light_current != null) {
                                light_current.brightness = light_sbBrightness.progress
                                DBUtils.updateGroup(light_current)
                                updateLights(light_current.brightness, "brightness", light_current)
//                                updateLights(progress, "brightness", group_current!!)
                            }
                        } else {
                            val light_current = DBUtils.getLightByID(light!!.id)
                            if (light_current != null) {
                                light_current.brightness = light_sbBrightness.progress
                                DBUtils.updateLight(light_current)
                            }
                        }
                    } else {
                        tv_Brightness.text = light_sbBrightness.progress.toString() + "%"
                        if (currentShowPageGroup) {
                            val light_current = DBUtils.getGroupByID(group!!.id)
                            if (light_current != null) {
                                light_current.brightness = light_sbBrightness.progress
                                DBUtils.updateGroup(light_current)
                                updateLights(light_current.brightness, "brightness", light_current)
                            }
                        } else {
                            val light_current = DBUtils.getLightByID(light!!.id)
                            if (light_current != null) {
                                light_current.brightness = light_sbBrightness.progress
                                DBUtils.updateLight(light_current)
                            }
                        }

                    }
                    if (light_sbBrightness.progress < 100) {
                        device_light_add.setImageResource(R.drawable.icon_puls)
                    }

                } else {
                    light_sbBrightness.progress--
                    if (light_sbBrightness.progress < 0) {
//                device_light_minus.setOnTouchListener { v, event -> false }
                        device_light_minus.setImageResource(R.drawable.icon_minus_no)
                    } else if (light_sbBrightness.progress == 0) {
                        device_light_minus.setImageResource(R.drawable.icon_minus_no)
                        tv_Brightness.text = light_sbBrightness.progress.toString() + "%"
                        if (currentShowPageGroup) {
                            var light_current = DBUtils.getGroupByID(group!!.id)
                            if (light_current != null) {
                                light_current.colorTemperature = light_sbBrightness.progress
                                DBUtils.updateGroup(light_current)
                                updateLights(light_current.colorTemperature, "colorTemperature", light_current)
//                                updateLights(progress, "colorTemperature", group_current!!)
                            }
                        } else {
                            var light_current = DBUtils.getLightByID(light!!.id)
                            if (light_current != null) {
                                light_current.colorTemperature = light_sbBrightness.progress
                                DBUtils.updateLight(light_current)
                            }
                        }

                    } else {
                        tv_Brightness.text = light_sbBrightness.progress.toString() + "%"
                        if (currentShowPageGroup) {
                            var light_current = DBUtils.getGroupByID(group!!.id)
                            if (light_current != null) {
                                light_current.colorTemperature = light_sbBrightness.progress
                                DBUtils.updateGroup(light_current)
                                updateLights(light_current.colorTemperature, "colorTemperature", light_current)
                            }
                        } else {
                            var light_current = DBUtils.getLightByID(light!!.id)
                            if (light_current != null) {
                                light_current.colorTemperature = light_sbBrightness.progress
                                DBUtils.updateLight(light_current)
                            }
                        }
                    }

                    if (light_sbBrightness.progress < 100) {
                        device_light_add.setImageResource(R.drawable.icon_puls)
                    }
                }
            }
        }
    }

    @SuppressLint("HandlerLeak")
    private val handler_handler = object : Handler() {
        override fun handleMessage(msg: Message) {
            super.handleMessage(msg)

            //            textView.setText(String.valueOf(msg.arg1));
            if (clickNum == 0) {
                light_sbBrightness.progress++
                if (light_sbBrightness.progress >= 100) {
//                device_light_minus.setOnTouchListener { v, event -> false }
                    device_light_add.setImageResource(R.drawable.icon_puls_no)
                } else {
                    tv_Brightness.text = light_sbBrightness.progress.toString() + "%"
                }

                if (light_sbBrightness.progress > 0) {
                    device_light_minus.setImageResource(R.drawable.icon_minus)
                }

            } else if (clickNum == 1) {
                if (isBrightness) {
                    light_sbBrightness.progress++
                    if (light_sbBrightness.progress > 100) {
//                device_light_minus.setOnTouchListener { v, event -> false }
                        device_light_add.setImageResource(R.drawable.icon_puls_no)
                    } else if (light_sbBrightness.progress == 100) {
                        device_light_add.setImageResource(R.drawable.icon_puls_no)
                        tv_Brightness.text = light_sbBrightness.progress.toString() + "%"
                        if (currentShowPageGroup) {
                            val light_current = DBUtils.getGroupByID(group!!.id)
                            if (light_current != null) {
                                light_current.brightness = light_sbBrightness.progress
                                DBUtils.updateGroup(light_current)
                                updateLights(light_current.brightness, "brightness", light_current)
//                                updateLights(progress, "brightness", group_current!!)
                            }
                        } else {
                            val light_current = DBUtils.getLightByID(light!!.id)
                            if (light_current != null) {
                                light_current.brightness = light!!.brightness
                                DBUtils.updateLight(light_current)
                            }
                        }
                    } else {
                        tv_Brightness.text = light_sbBrightness.progress.toString() + "%"
                        if (currentShowPageGroup) {
                            var light_current = DBUtils.getGroupByID(group!!.id)
                            if (light_current != null) {
                                light_current.brightness = light_sbBrightness.progress
                                DBUtils.updateGroup(light_current)
                                updateLights(light_current.brightness, "brightness", light_current)
//                                updateLights(progress, "brightness", group_current!!)
                            }
                        } else {
                            var light_current = DBUtils.getLightByID(light!!.id)
                            if (light_current != null) {
                                light_current.brightness = light!!.brightness
                                DBUtils.updateLight(light_current)
                            }
                        }

                    }

                    if (light_sbBrightness.progress > 0) {
                        device_light_minus.setImageResource(R.drawable.icon_minus)
                    }
                } else {
                    light_sbBrightness.progress++
                    if (light_sbBrightness.progress > 100) {
//                device_light_minus.setOnTouchListener { v, event -> false }
                        device_light_add.setImageResource(R.drawable.icon_puls_no)
                    } else if (light_sbBrightness.progress == 100) {
                        device_light_add.setImageResource(R.drawable.icon_puls_no)
                        tv_Brightness.text = light_sbBrightness.progress.toString() + "%"
                        if (currentShowPageGroup) {
                            var light_current = DBUtils.getGroupByID(group!!.id)
                            if (light_current != null) {
                                light_current.colorTemperature = light_sbBrightness.progress
                                DBUtils.updateGroup(light_current)
                                updateLights(light_current.colorTemperature, "colorTemperature", light_current)
                            }
                        } else {
                            var light_current = DBUtils.getLightByID(light!!.id)
                            if (light_current != null) {
                                light_current.colorTemperature = light_sbBrightness.progress
                                DBUtils.updateLight(light_current)
                            }
                        }
                    } else {
                        tv_Brightness.text = light_sbBrightness.progress.toString() + "%"
                        if (currentShowPageGroup) {
                            var light_current = DBUtils.getGroupByID(group!!.id)
                            if (light_current != null) {
                                light_current.colorTemperature = light_sbBrightness.progress
                                DBUtils.updateGroup(light_current)
                                updateLights(light_current.colorTemperature, "colorTemperature", light_current)
                            }
                        } else {
                            var light_current = DBUtils.getLightByID(light!!.id)
                            if (light_current != null) {
                                light_current.colorTemperature = light_sbBrightness.progress
                                DBUtils.updateLight(light_current)
                            }
                        }
                    }

                    if (light_sbBrightness.progress > 0) {
                        device_light_minus.setImageResource(R.drawable.icon_minus)
                    }
                }
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        if (currentShowPageGroup) {
            menuInflater.inflate(R.menu.menu_rgb_group_setting, menu)
        } else {
            menuInflater.inflate(R.menu.menu_rgb_light_setting, menu)
        }
        return super.onCreateOptionsMenu(menu)
    }

    private fun renameGroup() {
        val textGp = EditText(this)
        textGp.setText(group?.name)
        StringUtils.initEditTextFilter(textGp)
        textGp.setSelection(textGp.getText().toString().length)
        android.app.AlertDialog.Builder(this@NormalSettingActivity)
                .setTitle(R.string.rename)
                .setView(textGp)

                .setPositiveButton(getString(android.R.string.ok)) { dialog, which ->
                    // 获取输入框的内容
                    if (StringUtils.compileExChar(textGp.text.toString().trim { it <= ' ' })) {
                        ToastUtils.showShort(getString(R.string.rename_tip_check))
                    } else {
                        var name = textGp.text.toString().trim { it <= ' ' }
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
                            group?.name = textGp.text.toString().trim { it <= ' ' }
                            DBUtils.updateGroup(group!!)
                            toolbar.title = group?.name
                            dialog.dismiss()
                        }
                    }
                }
                .setNegativeButton(getString(R.string.btn_cancel)) { dialog, which -> dialog.dismiss() }.show()
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
        val params: ByteArray
        params = byteArrayOf(0x00, 0xff.toByte())
        TelinkLightService.Instance()?.sendCommandNoResponse(opcode, lightMeshAddr, params)
    }

    fun addEventListeners() {
        this.mApplication?.addEventListener(DeviceEvent.STATUS_CHANGED, this)
        this.mApplication?.addEventListener(ErrorReportEvent.ERROR_REPORT, this)
    }

    override fun onStop() {
        super.onStop()
        mConnectTimer?.dispose()
        this.mApplication?.removeEventListener(this)
    }

    override fun onDestroy() {
        super.onDestroy()
        mConnectTimer?.dispose()
        mDisposable.dispose()
        this.mApplication?.removeEventListener(this)
    }

    private fun updateGroup() {//更新分组 断开提示
        val intent = Intent(this, ChooseGroupForDevice::class.java)
        intent.putExtra(Constant.TYPE_VIEW, Constant.LIGHT_KEY)

        if (light == null) {
            ToastUtils.showShort(getString(R.string.please_connect_normal_light))
            TelinkLightService.Instance()?.idleMode(true)
            TelinkLightService.Instance()?.disconnect()
            return
        }
        intent.putExtra("light", light)
        intent.putExtra("gpAddress", gpAddress)
        intent.putExtra("uuid", light!!.productUUID)
        Log.d("addLight", light!!.productUUID.toString() + "," + light!!.meshAddr)
        startActivity(intent)
        this.finish()

    }

    override fun performed(event: Event<String>?) {
        when (event?.type) {
            DeviceEvent.STATUS_CHANGED -> this.onDeviceStatusChanged(event as DeviceEvent)
            ErrorReportEvent.ERROR_REPORT -> onErrorReport((event as ErrorReportEvent).args)
        }
    }

    internal var otaPrepareListner: OtaPrepareListner = object : OtaPrepareListner {

        override fun downLoadFileStart() {
            showLoadingDialog(getString(R.string.get_update_file))
        }

        override fun startGetVersion() {
            showLoadingDialog(getString(R.string.verification_version))
        }

        override fun getVersionSuccess(s: String) {
            //            ToastUtils.showLong(.string.verification_version_success);
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
                        var isBoolean: Boolean = SharedPreferencesHelper.getBoolean(TelinkLightApplication.getApp(), Constant.IS_DEVELOPER_MODE, false)
                        if (isBoolean) {
                            transformView()
                        } else {
                            OtaPrepareUtils.instance().gotoUpdateView(this@NormalSettingActivity, localVersion, otaPrepareListner)
                        }
                    } else {
                        ToastUtils.showLong(R.string.update_permission_tip)
                    }
                })
    }

    private fun transformView() {
        val intent = Intent(this@NormalSettingActivity, OTAUpdateActivity::class.java)
        intent.putExtra(Constant.OTA_MAC, light?.macAddr)
        intent.putExtra(Constant.OTA_MES_Add, light?.meshAddr)
        intent.putExtra(Constant.OTA_VERSION, light?.version)
        startActivity(intent)
        finish()
    }

    private fun getVersion() {
        if (TelinkApplication.getInstance().connectDevice != null) {
            Commander.getDeviceVersion(light!!.meshAddr, { s ->
                localVersion = s
                if (textTitle != null) {
                    if (OtaPrepareUtils.instance().checkSupportOta(localVersion)!!) {
                        textTitle!!.visibility = View.VISIBLE
                        textTitle!!.text = resources.getString(R.string.firmware_version, localVersion)
                        light!!.version = localVersion
//                        tvOta!!.visibility = View.VISIBLE
                    } else {
                        textTitle!!.visibility = View.VISIBLE
                        textTitle!!.text = resources.getString(R.string.firmware_version, localVersion)
                        light!!.version = localVersion
                        tvOta!!.visibility = View.GONE
                    }
                }
                null
            }, {
                if (textTitle != null) {
                    textTitle!!.visibility = View.GONE
                    tvOta!!.visibility = View.GONE
                }
                null
            })
        }
    }

    private fun onErrorReport(info: ErrorReportInfo) {
//       //("onErrorReport current device mac = ${bestRSSIDevice?.macAddress}")
        when (info.stateCode) {
            ErrorReportEvent.STATE_SCAN -> {
                when (info.errorCode) {
                    ErrorReportEvent.ERROR_SCAN_BLE_DISABLE -> {
                        //"蓝牙未开启")
                    }
                    ErrorReportEvent.ERROR_SCAN_NO_ADV -> {
                        //"无法收到广播包以及响应包")
                    }
                    ErrorReportEvent.ERROR_SCAN_NO_TARGET -> {
                        //"未扫到目标设备")
                    }
                }

            }
            ErrorReportEvent.STATE_CONNECT -> {
                when (info.errorCode) {
                    ErrorReportEvent.ERROR_CONNECT_ATT -> {
                        //"未读到att表")
                    }
                    ErrorReportEvent.ERROR_CONNECT_COMMON -> {
                        //"未建立物理连接")
                    }
                }

                autoConnect()

            }
            ErrorReportEvent.STATE_LOGIN -> {
                when (info.errorCode) {
                    ErrorReportEvent.ERROR_LOGIN_VALUE_CHECK -> {
                        //"value check失败： 密码错误")
                    }
                    ErrorReportEvent.ERROR_LOGIN_READ_DATA -> {
                        //"read login data 没有收到response")
                    }
                    ErrorReportEvent.ERROR_LOGIN_WRITE_DATA -> {
                        //"write login data 没有收到response")
                    }
                }

                autoConnect()

            }
        }
    }

    private fun onDeviceStatusChanged(event: DeviceEvent) {

        val deviceInfo = event.args

        when (deviceInfo.status) {
            LightAdapter.STATUS_LOGIN -> {
                hideLoadingDialog()
                isLoginSuccess = true
                mConnectTimer?.dispose()
            }
            LightAdapter.STATUS_LOGOUT -> {
                autoConnect()
                mConnectTimer = Observable.timer(15, TimeUnit.SECONDS, AndroidSchedulers.mainThread())
                        .subscribe { aLong ->
                            ToastUtil.showToast(this, getString(R.string.connecting))
                        }
            }
        }
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
        addEventListeners()
    }

    private fun initType() {
        val type = intent.getStringExtra(Constant.TYPE_VIEW)
        if (type == Constant.TYPE_GROUP) {
            currentShowPageGroup = true
            initDataGroup()
            initViewGroup()
        } else {
            currentShowPageGroup = false
            initToolbarLight()
            initViewLight()
            getVersion()
        }
    }


    var count = 0
    private fun getVersionTest() {
        if (TelinkApplication.getInstance().connectDevice != null) {
            Commander.getDeviceVersion(light!!.meshAddr, { s ->
                localVersion = s
                if (!localVersion!!.startsWith("LC")) {
                    ToastUtils.showLong("版本号出错：$localVersion")
                    textTitle!!.visibility = View.VISIBLE
                    textTitle!!.text = resources.getString(R.string.firmware_version, localVersion)
                    light!!.version = localVersion
                } else {
                    count++
                    ToastUtils.showShort("版本号正确次数：$localVersion")
                    textTitle!!.visibility = View.GONE
                    tvOta!!.visibility = View.GONE
                }
                null
            }, {
                if (textTitle != null) {
                    textTitle!!.visibility = View.GONE
                    tvOta!!.visibility = View.GONE
                }
                null
            })
        }
    }

    private fun initViewGroup() {
        if (group != null) {
            if (group!!.meshAddr == 0xffff) {
                toolbar.title = getString(R.string.allLight)
            }
        }

        btn_remove_group?.setOnClickListener(clickListener)
        btn_rename?.setOnClickListener(clickListener)

        checkGroupIsSystemGroup()

        brightness_btn.setOnClickListener(this.clickListener)
        light_switch.setOnClickListener(this.clickListener)
        temperature_btn.setOnClickListener(this.clickListener)


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

        var light_current = DBUtils.getGroupByID(group!!.id)


        this.light_sbBrightness?.max = 100
        if (group!!.connectionStatus == ConnectionStatus.OFF.value) {
            light_image.setImageResource(R.drawable.light_close)
            light_switch.setImageResource(R.drawable.icon_light_close)
            light_sbBrightness!!.setOnTouchListener { v, event -> true }
            light_sbBrightness.isEnabled = false
            isSwitch = false
            device_light_add.setImageResource(R.drawable.icon_puls_no)
            device_light_minus.setImageResource(R.drawable.icon_minus_no)
            device_light_add.setOnTouchListener { v, event -> false }
            device_light_minus.setOnTouchListener { v, event -> false }
        } else {
            light_image.setImageResource(R.drawable.icon_light)
            light_switch.setImageResource(R.drawable.icon_light_open)
            light_sbBrightness!!.setOnTouchListener { v, event -> false }
            light_sbBrightness.isEnabled = true
            isSwitch = true
            when {
                light_current!!.brightness <= 0 -> {
                    device_light_minus.setImageResource(R.drawable.icon_minus_no)
                    device_light_add.setImageResource(R.drawable.icon_puls)
                }
                light_current.brightness >= 100 -> {
                    device_light_add.setImageResource(R.drawable.icon_puls_no)
                    device_light_minus.setImageResource(R.drawable.icon_minus)
                }
                else -> {
                    device_light_minus.setImageResource(R.drawable.icon_minus)
                    device_light_add.setImageResource(R.drawable.icon_puls)
                }
            }
            device_light_add.setOnTouchListener { v, event ->
                if (event.action == MotionEvent.ACTION_DOWN) {
                    downTime = System.currentTimeMillis()
                    onBtnTouch = true
                    val t = object : Thread() {
                        override fun run() {
                            while (onBtnTouch) {
                                thisTime = System.currentTimeMillis()
                                if (thisTime - downTime >= 500) {
                                    tvValue++
                                    val msg = handler.obtainMessage()
                                    msg.arg1 = tvValue
                                    handler_handler.sendMessage(msg)
                                    Log.e("TAG_TOUCH", tvValue++.toString())
                                    try {
                                        sleep(100)
                                    } catch (e: InterruptedException) {
                                        e.printStackTrace()
                                    }

                                }
                            }
                        }
                    }
                    t.start()
                } else if (event.action == MotionEvent.ACTION_UP) {
                    onBtnTouch = false
                    if (thisTime - downTime < 500) {
                        tvValue++
                        val msg = handler.obtainMessage()
                        msg.arg1 = tvValue
                        handler_handler.sendMessage(msg)
                    }
                } else if (event.action == MotionEvent.ACTION_CANCEL) {
                    onBtnTouch = false
                }
                true
            }

            device_light_minus.setOnTouchListener { v, event ->
                if (event.action == MotionEvent.ACTION_DOWN) {
                    //                    tvValue = Integer.parseInt(textView.getText().toString());
                    downTime = System.currentTimeMillis()
                    onBtnTouch = true
                    val t = object : Thread() {
                        override fun run() {
                            while (onBtnTouch) {
                                thisTime = System.currentTimeMillis()
                                if (thisTime - downTime >= 500) {
                                    tvValue++
                                    val msg = handler.obtainMessage()
                                    msg.arg1 = tvValue
                                    handler.sendMessage(msg)
                                    Log.e("TAG_TOUCH", tvValue++.toString())
                                    try {
                                        sleep(100)
                                    } catch (e: InterruptedException) {
                                        e.printStackTrace()
                                    }

                                }
                            }
                        }
                    }
                    t.start()
                } else if (event.action == MotionEvent.ACTION_UP) {
                    onBtnTouch = false
                    if (thisTime - downTime < 500) {
                        tvValue++
                        val msg = handler.obtainMessage()
                        msg.arg1 = tvValue
                        handler.sendMessage(msg)
                    }
                } else if (event.action == MotionEvent.ACTION_CANCEL) {
                    onBtnTouch = false
                }
                true
            }
        }
        this.light_sbBrightness!!.setOnSeekBarChangeListener(this.barChangeListener)
    }

    //所有灯控分组暂标为系统默认分组不做修改处理
    private fun checkGroupIsSystemGroup() {
        if (group!!.meshAddr != 0xFFFF) {
            toolbar.title = group!!.name
            setSupportActionBar(toolbar)
            supportActionBar!!.setDisplayHomeAsUpEnabled(true)
            val actionBar = supportActionBar
            actionBar?.setDisplayHomeAsUpEnabled(true)
            toolbar.setOnMenuItemClickListener(menuItemClickListener)
        } else {
            toolbar.setNavigationIcon(R.drawable.navigation_back_white)
            toolbar.setNavigationOnClickListener {
                finish()
            }
        }
    }

    private fun initDataGroup() {
        this.mApplication = this.application as TelinkLightApplication
        val groupString = this.intent.extras!!.get("group")
        groupString ?: return
        this.group = groupString as DbGroup
    }

    private fun initToolbarLight() {
        toolbar.title = ""
        toolbar.inflateMenu(R.menu.menu_rgb_light_setting)
        setSupportActionBar(toolbar)
        val actionBar = supportActionBar
        actionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setOnMenuItemClickListener(menuItemClickListener)
    }

    private val menuItemClickListener = Toolbar.OnMenuItemClickListener { item ->
        when (item?.itemId) {
            R.id.toolbar_rename_light -> {
                renameGp()
            }
            R.id.toolbar_reset -> {
                remove()
            }
            R.id.toolbar_update_group -> {
                updateGroup()
            }
            R.id.toolbar_ota -> {
                updateOTA()
            }
            R.id.toolbar_delete_group -> {
                AlertDialog.Builder(Objects.requireNonNull<FragmentActivity>(this)).setMessage(R.string.delete_group_confirm)
                        .setPositiveButton(android.R.string.ok) { _, _ ->
                            this.showLoadingDialog(getString(R.string.deleting))

                            deleteGroup(DBUtils.getLightByGroupID(group!!.id), group!!,
                                    successCallback = {
                                        this.hideLoadingDialog()
                                        this?.setResult(Constant.RESULT_OK)
                                        this?.finish()
                                    },
                                    failedCallback = {
                                        this.hideLoadingDialog()
                                        ToastUtils.showShort(R.string.move_out_some_lights_in_group_failed)
                                    })
                        }
                        .setNegativeButton(R.string.btn_cancel, null)
                        .show()
            }

            R.id.toolbar_rename_group -> {
                renameGroup()
            }
        }
        true
    }

    private fun updateOTA() {
        if (textTitle.text != null && textTitle.text != "version") {
            checkPermission()
        } else {
            Toast.makeText(this, R.string.number_no, Toast.LENGTH_LONG).show()
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

    @SuppressLint("ClickableViewAccessibility")
    private fun initViewLight() {
        this.mApp = this.application as TelinkLightApplication?
        manager = DataManager(mApp, mApp!!.mesh.name, mApp!!.mesh.password)
        this.light = this.intent.extras!!.get(Constant.LIGHT_ARESS_KEY) as DbLight
        this.fromWhere = this.intent.getStringExtra(Constant.LIGHT_REFRESH_KEY)
        this.gpAddress = this.intent.getIntExtra(Constant.GROUP_ARESS_KEY, 0)
        toolbar.title = light?.name

        mRxPermission = RxPermissions(this)
        brightness_btn.setOnClickListener(this.clickListener)
        light_switch.setOnClickListener(this.clickListener)
        temperature_btn.setOnClickListener(this.clickListener)

        if (light!!.isSeek) {
            adjustment.text = getString(R.string.brightness_adjustment)
            brightness_btn.setImageResource(R.drawable.icon_btn)
            brightness_text.setTextColor(resources.getColor(R.color.blue_background))
            temperature_btn.setImageResource(R.drawable.icon_unselected)
            temperature_text.setTextColor(resources.getColor(R.color.black_nine))

            light_sbBrightness?.progress = light!!.brightness
            tv_Brightness.text = light!!.brightness.toString() + "%"

            isBrightness = true
        } else {
            adjustment.text = getString(R.string.color_temperature_adjustment)
            temperature_btn.setImageResource(R.drawable.icon_btn)
            temperature_text.setTextColor(resources.getColor(R.color.blue_background))
            brightness_btn.setImageResource(R.drawable.icon_unselected)
            brightness_text.setTextColor(resources.getColor(R.color.black_nine))

            light_sbBrightness?.progress = light!!.colorTemperature
            tv_Brightness.text = light!!.colorTemperature.toString() + "%"

            isBrightness = false
        }

        this.light_sbBrightness?.max = 100
        if (light!!.connectionStatus == ConnectionStatus.OFF.value) {
            light_image.setImageResource(R.drawable.light_close)
            light_switch.setImageResource(R.drawable.icon_light_close)
            light_sbBrightness!!.setOnTouchListener { v, event -> true }
            device_light_add.setImageResource(R.drawable.icon_puls_no)
            device_light_minus.setImageResource(R.drawable.icon_minus_no)
            light_sbBrightness.isEnabled = false
            device_light_add.setOnTouchListener { v, event -> false }
            device_light_minus.setOnTouchListener { v, event -> false }
            isSwitch = false
        } else {
            isSwitch = true
            light_image.setImageResource(R.drawable.icon_light)
            light_switch.setImageResource(R.drawable.icon_light_open)
            light_sbBrightness!!.setOnTouchListener { v, event -> false }
            light_sbBrightness.isEnabled = true
            when {
                light!!.brightness <= 0 -> {
                    device_light_minus.setImageResource(R.drawable.icon_minus_no)
                    device_light_add.setImageResource(R.drawable.icon_puls)
                }
                light!!.brightness >= 100 -> {
                    device_light_add.setImageResource(R.drawable.icon_puls_no)
                    device_light_minus.setImageResource(R.drawable.icon_minus)
                }
                else -> {
                    device_light_minus.setImageResource(R.drawable.icon_minus)
                    device_light_add.setImageResource(R.drawable.icon_puls)
                }
            }
            device_light_add.setOnTouchListener { v, event ->
                if (event.action == MotionEvent.ACTION_DOWN) {
                    downTime = System.currentTimeMillis()
                    onBtnTouch = true
                    val t = object : Thread() {
                        override fun run() {
                            while (onBtnTouch) {
                                thisTime = System.currentTimeMillis()
                                if (thisTime - downTime >= 500) {
                                    tvValue++
                                    val msg = handler.obtainMessage()
                                    msg.arg1 = tvValue
                                    handler_handler.sendMessage(msg)
                                    Log.e("TAG_TOUCH", tvValue++.toString())
                                    try {
                                        sleep(100)
                                    } catch (e: InterruptedException) {
                                        e.printStackTrace()
                                    }

                                }
                            }
                        }
                    }
                    t.start()
                } else if (event.action == MotionEvent.ACTION_UP) {
                    onBtnTouch = false
                    if (thisTime - downTime < 500) {
                        tvValue++
                        val msg = handler.obtainMessage()
                        msg.arg1 = tvValue
                        handler_handler.sendMessage(msg)
                    }
                } else if (event.action == MotionEvent.ACTION_CANCEL) {
                    onBtnTouch = false
                }
                true
            }

            device_light_minus.setOnTouchListener { v, event ->
                if (event.action == MotionEvent.ACTION_DOWN) {
                    //                    tvValue = Integer.parseInt(textView.getText().toString());
                    downTime = System.currentTimeMillis()
                    onBtnTouch = true
                    val t = object : Thread() {
                        override fun run() {
                            while (onBtnTouch) {
                                thisTime = System.currentTimeMillis()
                                if (thisTime - downTime >= 500) {
                                    tvValue++
                                    val msg = handler.obtainMessage()
                                    msg.arg1 = tvValue
                                    handler.sendMessage(msg)
                                    Log.e("TAG_TOUCH", tvValue++.toString())
                                    try {
                                        sleep(100)
                                    } catch (e: InterruptedException) {
                                        e.printStackTrace()
                                    }

                                }
                            }
                        }
                    }
                    t.start()
                } else if (event.action == MotionEvent.ACTION_UP) {
                    onBtnTouch = false
                    if (thisTime - downTime < 500) {
                        tvValue++
                        val msg = handler.obtainMessage()
                        msg.arg1 = tvValue
                        handler.sendMessage(msg)
                    }
                } else if (event.action == MotionEvent.ACTION_CANCEL) {
                    onBtnTouch = false
                }
                true
            }
        }

        mConnectDevice = TelinkLightApplication.getApp().connectDevice

        this.light_sbBrightness?.setOnSeekBarChangeListener(this.barChangeListener)
    }

    private fun renameGp() {
        val textGp = EditText(this)
        StringUtils.initEditTextFilter(textGp)
        textGp.setText(light?.name)
        textGp.setSelection(textGp.text.toString().length)
        android.app.AlertDialog.Builder(this@NormalSettingActivity)
                .setTitle(R.string.rename)
                .setView(textGp)

                .setPositiveButton(getString(android.R.string.ok)) { dialog, which ->
                    // 获取输入框的内容
                    if (StringUtils.compileExChar(textGp.text.toString().trim { it <= ' ' })) {
                        ToastUtils.showShort(getString(R.string.rename_tip_check))
                    } else {
                        light?.name = textGp.text.toString().trim { it <= ' ' }
                        DBUtils.updateLight(light!!)
                        toolbar.title = light?.name
                        dialog.dismiss()
                    }
                }
                .setNegativeButton(getString(R.string.btn_cancel)) { dialog, which -> dialog.dismiss() }.show()
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
                    tvRename.visibility = View.GONE
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

            editTitle.visibility = View.GONE
            titleCenterName.visibility = View.VISIBLE
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
                editTitle.visibility = View.GONE
                titleCenterName.visibility = View.VISIBLE
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
            editTitle.visibility = View.GONE
            titleCenterName.visibility = View.VISIBLE
            titleCenterName.text = light?.name
        } else {
            editTitle.visibility = View.GONE
            titleCenterName.visibility = View.VISIBLE
            titleCenterName.text = name
            light?.name = name
            DBUtils.updateLight(light!!)
        }
    }

    private val barChangeListener = object : SeekBar.OnSeekBarChangeListener {

        private var preTime: Long = 0
        private val delayTime = Constant.MAX_SCROLL_DELAY_VALUE

        override fun onStopTrackingTouch(seekBar: SeekBar) {
            //("progress:_3__" + seekBar.progress)
            this.onValueChange(seekBar, seekBar.progress, true, true)
        }

        override fun onStartTrackingTouch(seekBar: SeekBar) {
            clickNum = 1
            //("progress:_1__" + seekBar!!.progress)
            this.preTime = System.currentTimeMillis()
            this.onValueChange(seekBar, seekBar!!.progress, true, false)
        }

        override fun onProgressChanged(seekBar: SeekBar, progress: Int,
                                       fromUser: Boolean) {
            //("progress:_2__" + progress)
            val currentTime = System.currentTimeMillis()
            tv_Brightness.text = seekBar!!.progress.toString() + "%"
            clickNum = 1
            onValueChangeView(seekBar, progress, true, false)
            if (currentTime - this.preTime > this.delayTime) {
                this.onValueChange(seekBar, progress, true, false)
                this.preTime = currentTime
            }

        }

        private fun onValueChangeView(view: View, progress: Int, immediate: Boolean, isStopTracking:
        Boolean) {
//            if (view === sbBrightness) {
//                tvBrightness.text = getString(R.string.device_setting_brightness, progress.toString() + "")
//            } else if (view === sbTemperature) {
//                tvTemperature.text = getString(R.string.device_setting_temperature, progress.toString() + "")
//            }
        }

        private fun onValueChange(view: View, progress: Int, immediate: Boolean, isStopTracking:
        Boolean) {
            var addr = 0
            if (currentShowPageGroup) {
                addr = group?.meshAddr!!
            } else {
                addr = light?.meshAddr!!
            }

            val opcode: Byte
            var params: ByteArray
            if (clickNum == 1) {
                if (isBrightness) {
                    if (view == light_sbBrightness) {
                        opcode = Opcode.SET_LUM
                        params = byteArrayOf(progress.toByte())

                        if (currentShowPageGroup) {
                            if (group?.brightness != progress) {
                                if (progress > Constant.MAX_VALUE) {
                                    params = byteArrayOf(Constant.MAX_VALUE.toByte())
                                    TelinkLightService.Instance()?.sendCommandNoResponse(opcode, addr, params)
                                } else {
                                    TelinkLightService.Instance()?.sendCommandNoResponse(opcode, addr, params)
                                }
                            }
                        } else {
                            if (light?.brightness != progress) {
                                if (progress > Constant.MAX_VALUE) {
                                    params = byteArrayOf(Constant.MAX_VALUE.toByte())
                                    TelinkLightService.Instance()?.sendCommandNoResponse(opcode, addr, params)
                                } else {
                                    TelinkLightService.Instance()?.sendCommandNoResponse(opcode, addr, params)
                                }
                            }
                        }

                        if (currentShowPageGroup) {
                            group?.brightness = progress
                            if (progress <= 0) {
                                device_light_minus.setImageResource(R.drawable.icon_minus_no)
                                device_light_add.setImageResource(R.drawable.icon_puls)
                            } else if (progress >= 100) {
                                device_light_add.setImageResource(R.drawable.icon_puls_no)
                                device_light_minus.setImageResource(R.drawable.icon_minus)
                            } else {
                                device_light_minus.setImageResource(R.drawable.icon_minus)
                                device_light_add.setImageResource(R.drawable.icon_puls)
                            }
                        } else {
                            light?.brightness = progress
                            if (progress <= 0) {
                                device_light_minus.setImageResource(R.drawable.icon_minus_no)
                                device_light_add.setImageResource(R.drawable.icon_puls)
                            } else if (progress >= 100) {
                                device_light_add.setImageResource(R.drawable.icon_puls_no)
                                device_light_minus.setImageResource(R.drawable.icon_minus)
                            } else {
                                device_light_minus.setImageResource(R.drawable.icon_minus)
                                device_light_add.setImageResource(R.drawable.icon_puls)
                            }
                        }
                        Log.e("TAG", progress.toString())


                        if (isStopTracking) {
                            if (currentShowPageGroup) {
                                var group_current = DBUtils.getGroupByID(group!!.id)
                                if (group_current != null) {
                                    group_current.brightness = group!!.brightness
                                    tv_Brightness.text = group_current.brightness.toString() + "%"
                                    DBUtils.updateGroup(group_current)
                                    updateLights(progress, "brightness", group_current!!)
                                    clickNum = 0
                                }
//                                DBUtils.updateGroup(group!!)
//                                updateLights(progress, "brightness", group!!)
                            } else {
                                var light_current = DBUtils.getLightByID(light!!.id)
                                if (light_current != null) {
                                    light_current.brightness = light!!.brightness
                                    DBUtils.updateLight(light_current)
                                    tv_Brightness.text = light_current.brightness.toString() + "%"
                                    Log.e("TAG_TRUE_C", light!!.colorTemperature.toString())
                                    Log.e("TAG_TRUE_B", light!!.brightness.toString())
                                    clickNum = 0
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
                                TelinkLightService.Instance()?.sendCommandNoResponse(opcode, addr, params)
                            }
                        } else {
                            if (light?.colorTemperature != progress) {
                                Log.e("TAG", progress.toString())
                                TelinkLightService.Instance()?.sendCommandNoResponse(opcode, addr, params)
                            }
                        }

                        if (currentShowPageGroup) {
                            group?.colorTemperature = progress
                            when {
                                progress <= 0 -> {
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
                        } else {
                            light?.colorTemperature = progress
                            if (progress <= 0) {
                                device_light_minus.setImageResource(R.drawable.icon_minus_no)
                                device_light_add.setImageResource(R.drawable.icon_puls)
                            } else if (progress >= 100) {
                                device_light_add.setImageResource(R.drawable.icon_puls_no)
                                device_light_minus.setImageResource(R.drawable.icon_minus)
                            } else {
                                device_light_minus.setImageResource(R.drawable.icon_minus)
                                device_light_add.setImageResource(R.drawable.icon_puls)
                            }
                        }

                        Log.e("TAG", progress.toString())
                        if (isStopTracking) {
                            if (currentShowPageGroup) {
                                var group_current = DBUtils.getGroupByID(group!!.id)
                                if (group_current != null) {
                                    group_current.colorTemperature = group!!.colorTemperature
                                    tv_Brightness.text = group_current.colorTemperature.toString() + "%"
                                    DBUtils.updateGroup(group_current)
                                    updateLights(progress, "colorTemperature", group_current!!)
                                    clickNum = 0
                                }
                            } else {
                                var light_current = DBUtils.getLightByID(light!!.id)
                                if (light_current != null) {
                                    light_current.colorTemperature = light!!.colorTemperature
                                    tv_Brightness.text = light_current.colorTemperature.toString() + "%"
                                    DBUtils.updateLight(light_current)
//                                DBUtils.updateLight(light!!)
                                    Log.e("TAG_FASLE_C", light!!.colorTemperature.toString())
                                    Log.e("TAG_FASLE_B", light!!.brightness.toString())
                                    clickNum = 0
                                }
                            }
                        }
                    }
                }
                //                progress += 5;
                //                Log.d(TAG, "onValueChange: "+progress);
            }
//            else {
//
//                opcode = Opcode.SET_TEMPERATURE
//                params = byteArrayOf(0x05, progress.toByte())
//
//                if (currentShowPageGroup) {
//                    group?.colorTemperature = progress
//                } else {
//                    light?.colorTemperature = progress
//                }
//
//                TelinkLightService.Instance()?.sendCommandNoResponse(opcode, addr, params)
//                if (isStopTracking) {
//                    if (currentShowPageGroup) {
//                        DBUtils.updateGroup(group!!)
//                        updateLights(progress, "colorTemperature", group!!)
//                    } else {
//                        DBUtils.updateLight(light!!)
//                    }
//                }
//            }
        }
    }

    private fun updateLights(progress: Int, type: String, group: DbGroup) {
        Thread {
            var lightList: MutableList<DbLight> = ArrayList()

            if (group.meshAddr == 0xffff) {
                //            lightList = DBUtils.getAllLight();
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
        }.start()
    }

    private fun sendInitCmd(brightness: Int, colorTemperature: Int) {
        val addr = light?.meshAddr
        var opcode: Byte
        var params: ByteArray
        //                progress += 5;
        //                Log.d(TAG, "onValueChange: "+progress);
        opcode = Opcode.SET_LUM
        params = byteArrayOf(brightness.toByte())
        light?.brightness = brightness
        TelinkLightService.Instance()?.sendCommandNoResponse(opcode, addr!!, params)

        try {
            Thread.sleep(200)
        } catch (e: InterruptedException) {
            e.printStackTrace()
        } finally {
            opcode = Opcode.SET_TEMPERATURE
            params = byteArrayOf(0x05, colorTemperature.toByte())
            light?.colorTemperature = colorTemperature
            TelinkLightService.Instance()?.sendCommandNoResponse(opcode, addr!!, params)
        }
    }

    fun remove() {
        if (TelinkLightService.Instance()?.adapter!!.mLightCtrl.currentLight != null) {
            AlertDialog.Builder(Objects.requireNonNull<AppCompatActivity>(this)).setMessage(R.string.delete_light_confirm)
                    .setPositiveButton(android.R.string.ok) { dialog, which ->

                        if (TelinkLightService.Instance()?.adapter!!.mLightCtrl.currentLight != null && TelinkLightService.Instance()?.adapter!!.mLightCtrl.currentLight.isConnected) {
                            val opcode = Opcode.KICK_OUT
                            TelinkLightService.Instance()?.sendCommandNoResponse(opcode, light!!.getMeshAddr(), null)
                            DBUtils.deleteLight(light!!)
                            if (TelinkLightApplication.getApp().mesh.removeDeviceByMeshAddress(light!!.getMeshAddr())) {
                                TelinkLightApplication.getApp().mesh.saveOrUpdate(this)
                            }
                            if (mConnectDevice != null) {
                                Log.d(this.javaClass.getSimpleName(), "mConnectDevice.meshAddress = " + mConnectDevice?.meshAddress)
                                Log.d(this.javaClass.getSimpleName(), "light.getMeshAddr() = " + light?.getMeshAddr())
                                if (light?.meshAddr == mConnectDevice?.meshAddress) {
                                    this.setResult(Activity.RESULT_OK, Intent().putExtra("data", true))
                                }
                            }
                            this.finish()


                        } else {
                            ToastUtils.showLong(getString(R.string.bluetooth_open_connet))
                            this.finish()
                        }
                    }
                    .setNegativeButton(R.string.btn_cancel, null)
                    .show()
        }
    }

    /**
     * 自动重连
     */
    private fun autoConnect() {

        if (TelinkLightService.Instance() != null) {
            ToastUtils.showLong(getString(R.string.connecting))

            if (this.mApp?.isEmptyMesh != false)
                return

            val mesh = this.mApp?.mesh

            if (TextUtils.isEmpty(mesh?.name) || TextUtils.isEmpty(mesh?.password)) {
                TelinkLightService.Instance()?.idleMode(true)
                return
            }

            //自动重连参数
            val connectParams = Parameters.createAutoConnectParameters()
            connectParams.setMeshName(DBUtils.lastUser?.controlMeshName)
            connectParams.setPassword(NetworkFactory.md5(NetworkFactory.md5(DBUtils.lastUser?.controlMeshName) + DBUtils.lastUser?.controlMeshName))

            connectParams.autoEnableNotification(true)

            //自动重连
            TelinkLightService.Instance()?.autoConnect(connectParams)

            //刷新Notify参数
            val refreshNotifyParams = Parameters.createRefreshNotifyParameters()
            refreshNotifyParams.setRefreshRepeatCount(2)
            refreshNotifyParams.setRefreshInterval(2000)
            //开启自动刷新Notify
            TelinkLightService.Instance()?.autoRefreshNotify(refreshNotifyParams)
        }
    }

    override fun onBackPressed() {
        super.onBackPressed()
        setResult(Constant.RESULT_OK)
        finish()
    }
}
