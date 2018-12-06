package com.dadoutek.uled.rgb

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.support.v4.app.FragmentActivity
import android.support.v7.app.AlertDialog
import android.support.v7.widget.GridLayoutManager
import android.support.v7.widget.RecyclerView
import android.text.TextUtils
import android.util.Log
import android.view.KeyEvent
import android.view.MenuItem
import android.view.View
import android.view.View.OnClickListener
import android.widget.*
import com.blankj.utilcode.util.LogUtils
import com.blankj.utilcode.util.ToastUtils
import com.chad.library.adapter.base.BaseQuickAdapter

import com.dadoutek.uled.R
import com.dadoutek.uled.communicate.Commander
import com.dadoutek.uled.model.Constant
import com.dadoutek.uled.model.DbModel.DBUtils
import com.dadoutek.uled.model.DbModel.DbGroup
import com.dadoutek.uled.model.DbModel.DbLight
import com.dadoutek.uled.model.ItemColorPreset
import com.dadoutek.uled.model.Opcode
import com.dadoutek.uled.model.SharedPreferencesHelper
import com.dadoutek.uled.network.NetworkFactory
import com.dadoutek.uled.tellink.TelinkBaseActivity
import com.dadoutek.uled.tellink.TelinkLightApplication
import com.dadoutek.uled.tellink.TelinkLightService
import com.dadoutek.uled.util.OtherUtils
import com.dadoutek.uled.util.StringUtils
import com.skydoves.colorpickerview.ColorPickerView
import com.skydoves.colorpickerview.listeners.ColorEnvelopeListener
import com.telink.bluetooth.event.DeviceEvent
import com.telink.bluetooth.light.LightAdapter
import com.telink.bluetooth.light.Parameters
import com.telink.util.Event
import com.telink.util.EventListener
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import kotlinx.android.synthetic.main.fragment_rgb_group_setting.*
import kotlinx.android.synthetic.main.toolbar.*
import java.util.*
import java.util.concurrent.TimeUnit

class RGBGroupSettingActivity : TelinkBaseActivity(), OnClickListener, EventListener<String> {
    private var mApplication: TelinkLightApplication? = null
    private var brightnessBar: SeekBar? = null
    private var temperatureBar: SeekBar? = null
    private var colorR: TextView? = null
    private var colorG: TextView? = null
    private var colorB: TextView? = null
    private var scrollView: ScrollView? = null
    private var diyColorRecyclerListView: RecyclerView? = null
    private var colorPicker: ColorPickerView? = null
    private var btn_remove_group: Button? = null
    private var btn_rename: Button? = null
    private var stopTracking = false
    private var presetColors: MutableList<ItemColorPreset>? = null
    private var colorSelectDiyRecyclerViewAdapter: ColorSelectDiyRecyclerViewAdapter? = null
    private var group: DbGroup? = null
    private var dynamicRgbBt: TextView? =null
    private var mApp: TelinkLightApplication? = null
    private var mConnectTimer: Disposable? = null
    private var isLoginSuccess = false
    private var connectTimes = 0

    private val clickListener = OnClickListener { v ->
       if(v === dynamicRgbBt){
            val intent=Intent(this,RGBGradientActivity::class.java)
            intent.putExtra(Constant.TYPE_VIEW,Constant.TYPE_GROUP)
            intent.putExtra(Constant.TYPE_VIEW_ADDRESS,group?.meshAddr)
            startActivityForResult(intent,0)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_rgb_group_setting)

        initToolbar()
        initData()
        initView()

        addEventListeners()
        this.mApp = this.application as TelinkLightApplication?
    }

    private fun initToolbar() {
        toolbar.title = ""
        setSupportActionBar(toolbar)
        val actionBar = supportActionBar
        actionBar?.setDisplayHomeAsUpEnabled(true)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> finish()
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onPause() {
        super.onPause()
        DBUtils.updateGroup(group!!)
        updateLights(group!!.color, "rgb_color", group!!)
    }

    fun addEventListeners() {
        this.mApplication?.addEventListener(DeviceEvent.STATUS_CHANGED, this)
//        this.mApplication?.addEventListener(NotificationEvent.ONLINE_STATUS, this)
////        this.mApplication?.addEventListener(NotificationEvent.GET_ALARM, this)
//        this.mApplication?.addEventListener(NotificationEvent.GET_DEVICE_STATE, this)
//        this.mApplication?.addEventListener(ServiceEvent.SERVICE_CONNECTED, this)
////        this.mApplication?.addEventListener(MeshEvent.OFFLINE, this)
//        this.mApplication?.addEventListener(ErrorReportEvent.ERROR_REPORT, this)
    }

    override fun performed(event: Event<String>?) {
        when (event?.type) {
            DeviceEvent.STATUS_CHANGED -> this.onDeviceStatusChanged(event as DeviceEvent)
        }
    }

    private fun onDeviceStatusChanged(event: DeviceEvent) {

        val deviceInfo = event.args

        when (deviceInfo.status) {
            LightAdapter.STATUS_LOGIN -> {
                hideLoadingDialog()
                isLoginSuccess=true
                mConnectTimer?.dispose()
            }
            LightAdapter.STATUS_LOGOUT -> {
                autoConnect()
                mConnectTimer = Observable.timer(15, TimeUnit.SECONDS, AndroidSchedulers.mainThread())
                        .subscribe { aLong ->
                            com.blankj.utilcode.util.LogUtils.d("STATUS_LOGOUT")
//                            showLoadingDialog()
                            ToastUtils.showLong(getString(R.string.connect_failed))
                            finish()
                        }
            }
        }
    }

//    private fun showIsConnect

    /**
     * 自动重连
     */
    private fun autoConnect() {

        if (TelinkLightService.Instance() != null) {

            if (TelinkLightService.Instance().mode != LightAdapter.MODE_AUTO_CONNECT_MESH) {

                ToastUtils.showLong(getString(R.string.connecting))
                SharedPreferencesHelper.putBoolean(this, Constant.CONNECT_STATE_SUCCESS_KEY, false)

                if (this.mApp!!.isEmptyMesh())
                    return

                //                Lights.getInstance().clear();
                this.mApp?.refreshLights()

                val mesh = this.mApp?.getMesh()

                if (TextUtils.isEmpty(mesh?.name) || TextUtils.isEmpty(mesh?.password)) {
                    TelinkLightService.Instance().idleMode(true)
                    return
                }

                val account = SharedPreferencesHelper.getString(TelinkLightApplication.getInstance(),
                        Constant.DB_NAME_KEY, "dadou")

                //自动重连参数
                val connectParams = Parameters.createAutoConnectParameters()
                connectParams.setMeshName(mesh?.name)
                if (SharedPreferencesHelper.getString(TelinkLightApplication.getInstance(), Constant.USER_TYPE, Constant.USER_TYPE_OLD) == Constant.USER_TYPE_NEW) {
                    connectParams.setPassword(NetworkFactory.md5(
                            NetworkFactory.md5(mesh?.password) + account))
                } else {
                    connectParams.setPassword(mesh?.password)
                }
                connectParams.autoEnableNotification(true)

                // 之前是否有在做MeshOTA操作，是则继续
                if (mesh!!.isOtaProcessing) {
                    connectParams.setConnectMac(mesh?.otaDevice!!.mac)
                }
                //自动重连
                TelinkLightService.Instance().autoConnect(connectParams)
            }

            //刷新Notify参数
            val refreshNotifyParams = Parameters.createRefreshNotifyParameters()
            refreshNotifyParams.setRefreshRepeatCount(2)
            refreshNotifyParams.setRefreshInterval(2000)
            //开启自动刷新Notify
            TelinkLightService.Instance().autoRefreshNotify(refreshNotifyParams)
        }
    }

    private fun initData() {
        this.mApplication = this.application as TelinkLightApplication
        this.group = this.intent.extras!!.get("group") as DbGroup
    }

    private fun initView() {
        if (group != null) {
            if (group!!.meshAddr == 0xffff) {
                toolbar.title=getString(R.string.allLight)
            } else {
                toolbar.title=group?.name
            }
        }

        dynamicRgbBt = this.findViewById<View>(R.id.dynamic_rgb) as TextView
        dynamicRgbBt?.setOnClickListener(this.clickListener)

        this.brightnessBar = findViewById<View>(R.id.sb_brightness) as SeekBar
        this.temperatureBar = findViewById<View>(R.id.sb_temperature) as SeekBar
        this.colorR = findViewById<View>(R.id.color_r) as TextView
        this.colorG = findViewById<View>(R.id.color_g) as TextView
        this.colorB = findViewById<View>(R.id.color_b) as TextView
        this.diyColorRecyclerListView = findViewById<View>(R.id.diy_color_recycler_list_view) as RecyclerView

        this.colorPicker = findViewById<View>(R.id.color_picker) as ColorPickerView
        this.colorPicker!!.setOnTouchListener { v, _ ->
            v.parent.requestDisallowInterceptTouchEvent(true)
            false
        }
        btn_rename = findViewById<Button>(R.id.btn_rename)
//        dynamicRgb = findViewById<Button>(R.id.dynamicRgb)
        btn_remove_group = findViewById<Button>(R.id.btn_remove_group)

        btn_remove_group?.setOnClickListener(this)
        btn_rename?.setOnClickListener(this)
//        dynamicRgb?.setOnClickListener(this)

        presetColors = SharedPreferencesHelper.getObject(this, Constant.PRESET_COLOR) as? MutableList<ItemColorPreset>
        if (presetColors == null) {
            presetColors = java.util.ArrayList<ItemColorPreset>()
            for (i in 0..4) {
                val itemColorPreset = ItemColorPreset()
                itemColorPreset.color=OtherUtils.getCreateInitColor(i)
                presetColors?.add(itemColorPreset)
            }
        }

        diyColorRecyclerListView?.layoutManager = GridLayoutManager(this,5) as RecyclerView.LayoutManager?
        colorSelectDiyRecyclerViewAdapter = ColorSelectDiyRecyclerViewAdapter(R.layout.color_select_diy_item, presetColors)
        colorSelectDiyRecyclerViewAdapter?.onItemChildClickListener = diyOnItemChildClickListener
        colorSelectDiyRecyclerViewAdapter?.onItemChildLongClickListener = diyOnItemChildLongClickListener
        colorSelectDiyRecyclerViewAdapter?.bindToRecyclerView(diyColorRecyclerListView)

        brightnessBar!!.progress = group!!.brightness
        tv_brightness_rgb.text = getString(R.string.device_setting_brightness, group!!.brightness.toString() + "")
        temperatureBar!!.progress = group!!.colorTemperature


        this.brightnessBar!!.setOnSeekBarChangeListener(this.barChangeListener)
        this.temperatureBar!!.setOnSeekBarChangeListener(this.barChangeListener)
        this.colorPicker?.setColorListener(colorEnvelopeListener)
        checkGroupIsSystemGroup()
    }

    internal var diyOnItemChildClickListener: BaseQuickAdapter.OnItemChildClickListener = BaseQuickAdapter.OnItemChildClickListener { adapter, view, position ->
        val color = presetColors?.get(position)?.color

//        LogUtils.d("changedff$color")
        val brightness = presetColors?.get(position)?.brightness
        val red = (color!! and 0xff0000) shr 16
        val green = (color and 0x00ff00) shr 8
        val blue = color and 0x0000ff
        Thread {
            changeColor(red.toByte(), green.toByte(), blue.toByte(),true)

            try {
                Thread.sleep(100)
                val addr = group?.meshAddr
                val opcode: Byte = Opcode.SET_LUM
                val params: ByteArray = byteArrayOf(brightness!!.toByte())
                group?.brightness = brightness
                group?.color = color

                LogUtils.d("changedff2"+opcode+"--"+addr+"--"+brightness)
                for(i in 0..3){
                    Thread.sleep(50)
                    TelinkLightService.Instance().sendCommandNoResponse(opcode, addr!!, params)
                }
//                DBUtils.updateGroup(group!!)
//                updateLights(color, "rgb_color", group!!)
            } catch (e: InterruptedException) {
                e.printStackTrace()
            }
        }.start()

        brightnessBar?.progress = brightness!!
        tv_brightness_rgb.text = getString(R.string.device_setting_brightness, brightness.toString() + "")
//        scrollView?.setBackgroundColor(color)
        colorR?.text = red.toString()
        colorG?.text = green.toString()
        colorB?.text = blue.toString()
    }

    @SuppressLint("SetTextI18n")
    internal var diyOnItemChildLongClickListener: BaseQuickAdapter.OnItemChildLongClickListener = BaseQuickAdapter.OnItemChildLongClickListener { adapter, view, position ->
        presetColors?.get(position)!!.color = group!!.color
        presetColors?.get(position)!!.brightness = group!!.brightness
        val textView = adapter.getViewByPosition(position, R.id.btn_diy_preset) as TextView?
        textView!!.text = group!!.brightness.toString() + "%"
        textView.setBackgroundColor(0xff000000.toInt() or group!!.color)
        SharedPreferencesHelper.putObject(this, Constant.PRESET_COLOR, presetColors)
        false
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.btn_remove_group -> AlertDialog.Builder(Objects.requireNonNull<FragmentActivity>(this)).setMessage(R.string.delete_group_confirm)
                    .setPositiveButton(android.R.string.ok) { _, _ ->
                        this.showLoadingDialog(getString(R.string.deleting))

                        deleteGroup(DBUtils.getLightByGroupID(group!!.id), group!!,
                                successCallback = {
                                    this.hideLoadingDialog()
                                    this.setResult(Constant.RESULT_OK)
                                    this.finish()
                                },
                                failedCallback = {
                                    this.hideLoadingDialog()
                                    ToastUtils.showShort(R.string.move_out_some_lights_in_group_failed)
                                })
                    }
                    .setNegativeButton(R.string.btn_cancel, null)
                    .show()
            R.id.btn_rename -> renameGp()
//            R.id.dynamicRgb -> toRGBGradientView()
        }
    }

    private fun toRGBGradientView() {
        val intent=Intent(this,RGBGradientActivity::class.java)
        intent.putExtra(Constant.TYPE_VIEW,Constant.TYPE_GROUP)
        intent.putExtra(Constant.TYPE_VIEW_ADDRESS,group?.meshAddr)
        overridePendingTransition(0, 0)
        startActivityForResult(intent,0)
    }

    override fun onDestroy() {
        super.onDestroy()
        mConnectTimer?.dispose()
        this.mApplication?.removeEventListener(this)
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            setResult(Constant.RESULT_OK)
            finish()
            return false
        } else {
            return super.onKeyDown(keyCode, event)
        }

    }

    private val barChangeListener = object : SeekBar.OnSeekBarChangeListener {

        private var preTime: Long = 0
        private val delayTime = 30

        override fun onStopTrackingTouch(seekBar: SeekBar) {
            stopTracking = true
            this.onValueChange(seekBar, seekBar.progress, true)
            LogUtils.d("seekBarstop" + seekBar.progress)
        }

        override fun onStartTrackingTouch(seekBar: SeekBar) {
            stopTracking = false
            this.preTime = System.currentTimeMillis()
            this.onValueChange(seekBar, seekBar.progress, true)
        }

        override fun onProgressChanged(seekBar: SeekBar, progress: Int,
                                       fromUser: Boolean) {
            val currentTime = System.currentTimeMillis()

            if (currentTime - this.preTime > this.delayTime) {
                this.onValueChange(seekBar, progress, true)
                this.preTime = currentTime
            }
        }

        private fun onValueChange(view: View, progress: Int, immediate: Boolean) {

            val addr = group!!.meshAddr
            val opcode: Byte
            val params: ByteArray

            if (view === brightnessBar) {
                opcode = Opcode.SET_LUM
                params = byteArrayOf(progress.toByte())
                group!!.brightness = progress
                tv_brightness_rgb.text = getString(R.string.device_setting_brightness, progress.toString() + "")
                TelinkLightService.Instance()?.sendCommandNoResponse(opcode, addr, params, immediate)

                if (stopTracking) {
                    DBUtils.updateGroup(group!!)
                    updateLights(progress, "brightness", group!!)
                }
            } else if (view === temperatureBar) {

                opcode = Opcode.SET_TEMPERATURE
                params = byteArrayOf(0x05, progress.toByte())
                group!!.colorTemperature = progress
                TelinkLightService.Instance()?.sendCommandNoResponse(opcode, addr, params, immediate)

                if (stopTracking) {
                    DBUtils.updateGroup(group!!)
                    updateLights(progress, "colorTemperature", group!!)
                }
            }
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
                } else if(type == "rgb_color"){
                    dbLight.color = progress
                }
                DBUtils.updateLight(dbLight)
            }
        }.start()
    }

    private val colorEnvelopeListener = ColorEnvelopeListener { envelope, fromUser ->
        val argb = envelope.argb

        colorR?.text = argb[1].toString()
        colorG?.text = argb[2].toString()
        colorB?.text = argb[3].toString()

        val color:Int = argb[1] shl 16 or (argb[2] shl 8) or argb[3]
//        val color =
        Log.d("", "onColorSelected: " + Integer.toHexString(color))
        if (fromUser) {
//            scrollView?.setBackgroundColor(0xff000000.toInt() or color)
            if(argb[1]==0 && argb[2]==0 && argb[3]==0){
            }else{
                Thread{
                    group?.color = color
                    changeColor(argb[1].toByte(), argb[2].toByte(), argb[3].toByte(), false)

                }.start()
            }
        }
    }

    private fun changeColor(R: Byte, G: Byte, B: Byte, isOnceSet: Boolean) {

        var red  = R
        var green = G
        var blue = B

        val addr = group?.meshAddr
        val opcode = Opcode.SET_TEMPERATURE

        val minVal = 0x50

        if (green.toInt() and 0xff <= minVal)
            green = 0
        if (red.toInt() and 0xff <= minVal)
            red = 0
        if (blue.toInt() and 0xff <= minVal)
            blue = 0

        val params = byteArrayOf(0x04, red, green, blue)

        val logStr = String.format("R = %x, G = %x, B = %x", red, green, blue)
        Log.d("RGBCOLOR", logStr)

        if(isOnceSet){
            for(i in 0..3){
                Thread.sleep(50)
                TelinkLightService.Instance().sendCommandNoResponse(opcode, addr!!, params)
            }
        }else{
            TelinkLightService.Instance().sendCommandNoResponse(opcode, addr!!, params)
        }
    }

    //所有灯控分组暂标为系统默认分组不做修改处理
    private fun checkGroupIsSystemGroup() {
        if (group!!.meshAddr == 0xFFFF) {
            btn_remove_group!!.visibility = View.GONE
            btn_rename!!.visibility = View.GONE
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
                    LogUtils.d("retry delete group timeout")
                }
            } else {
                DBUtils.deleteGroupOnly(group)
                this?.runOnUiThread {
                    successCallback.invoke()
                }
            }
        }.start()

    }

    private fun getRelatedSceneIds(groupAddress: Int): List<Long> {
        val sceneIds = java.util.ArrayList<Long>()
        val dbSceneList = DBUtils.sceneList
        sceneLoop@ for (dbScene in dbSceneList) {
            val dbActions = DBUtils.getActionsBySceneId(dbScene.id)
            for (action in dbActions) {
                if (groupAddress == action.groupAddr || 0xffff == action.groupAddr) {
                    sceneIds.add(dbScene.id)
                    continue@sceneLoop
                }
            }
        }
        return sceneIds
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
        TelinkLightService.Instance().sendCommandNoResponse(opcode, lightMeshAddr, params)
    }

    private fun renameGp() {
            val textGp = EditText(this)
            textGp.setText(group?.name)
            StringUtils.initEditTextFilter(textGp)
            textGp.setSelection(textGp.getText().toString().length)
            android.app.AlertDialog.Builder(this@RGBGroupSettingActivity)
                    .setTitle(R.string.rename)
                    .setView(textGp)

                    .setPositiveButton(getString(android.R.string.ok)) { dialog, which ->
                        // 获取输入框的内容
                        if (StringUtils.compileExChar(textGp.text.toString().trim { it <= ' ' })) {
                            ToastUtils.showShort(getString(R.string.rename_tip_check))
                        } else {
                            var name=textGp.text.toString().trim { it <= ' ' }
                            var canSave=true
                            val groups=DBUtils.allGroups
                            for(i in groups.indices){
                                if(groups[i].name==name){
                                    ToastUtils.showLong(TelinkLightApplication.getInstance().getString(R.string.repeat_name))
                                    canSave=false
                                    break
                                }
                            }

                            if(canSave){
                                group?.name=textGp.text.toString().trim { it <= ' ' }
                                DBUtils.updateGroup(group!!)
                                toolbar.title=group?.name
                                dialog.dismiss()
                            }
                        }
                    }
                    .setNegativeButton(getString(R.string.btn_cancel)) { dialog, which -> dialog.dismiss() }.show()
    }
}
