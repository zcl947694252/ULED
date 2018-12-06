package com.dadoutek.uled.rgb

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AlertDialog
import android.support.v7.widget.GridLayoutManager
import android.text.TextUtils
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.view.View.OnClickListener
import android.view.Window
import android.widget.*

import com.blankj.utilcode.util.ToastUtils
import com.chad.library.adapter.base.BaseQuickAdapter
import com.dadoutek.uled.R
import com.dadoutek.uled.communicate.Commander
import com.dadoutek.uled.group.LightGroupingActivity
import com.dadoutek.uled.intf.OtaPrepareListner
import com.dadoutek.uled.light.RenameLightActivity
import com.dadoutek.uled.model.Constant
import com.dadoutek.uled.model.DbModel.DBUtils
import com.dadoutek.uled.model.DbModel.DbLight
import com.dadoutek.uled.model.ItemColorPreset
import com.dadoutek.uled.model.Opcode
import com.dadoutek.uled.model.SharedPreferencesHelper
import com.dadoutek.uled.network.NetworkFactory
import com.dadoutek.uled.ota.OTAUpdateActivity
import com.dadoutek.uled.tellink.TelinkBaseActivity
import com.dadoutek.uled.tellink.TelinkLightApplication
import com.dadoutek.uled.tellink.TelinkLightService
import com.dadoutek.uled.util.DataManager
import com.dadoutek.uled.util.OtaPrepareUtils
import com.dadoutek.uled.util.OtherUtils
import com.dadoutek.uled.util.StringUtils
import com.skydoves.colorpickerview.listeners.ColorEnvelopeListener
import com.tbruyelle.rxpermissions2.RxPermissions
import com.telink.TelinkApplication
import com.telink.bluetooth.light.DeviceInfo
import com.telink.bluetooth.light.LightAdapter
import com.telink.bluetooth.light.Parameters
import io.reactivex.disposables.CompositeDisposable
import kotlinx.android.synthetic.main.fragment_rgb_device_setting.*
import kotlinx.android.synthetic.main.toolbar.*
import java.util.*

class RGBDeviceSettingActivity : TelinkBaseActivity() {
    private var localVersion: String? = null
    private var light: DbLight? = null
    private var gpAddress: Int = 0
    private var fromWhere: String? = null
    private var mApplication: TelinkLightApplication? = null
    private var dataManager: DataManager? = null
    private val mDisposable = CompositeDisposable()
    private var mRxPermission: RxPermissions? = null
    private val remove: Button? = null
    private val dialog: AlertDialog? = null
    private var presetColors: MutableList<ItemColorPreset>? = null
    private var colorSelectDiyRecyclerViewAdapter: ColorSelectDiyRecyclerViewAdapter? = null
    private var mApp: TelinkLightApplication? = null
    private var manager: DataManager? = null
    private var mConnectDevice: DeviceInfo? = null
    
    private val clickListener = OnClickListener { v ->
        when (v.id) {
            R.id.img_header_menu_left->finish()
            R.id.tvOta->checkPermission()
            R.id.btn_rename->{
                val intent = Intent(this, RenameLightActivity::class.java)
                intent.putExtra("light", light)
                startActivity(intent)
                this!!.finish()
            }
            R.id.update_group -> updateGroup()
            R.id.btn_remove -> remove()
            R.id.dynamic_rgb -> toRGBGradientView()
            R.id.tvRename -> renameLight()
        }
    }

    private fun renameLight() {
        val textGp = EditText(this)
        StringUtils.initEditTextFilter(textGp)
        textGp.setText(light?.name)
        textGp.setSelection(textGp.getText().toString().length)
        android.app.AlertDialog.Builder(this@RGBDeviceSettingActivity)
                .setTitle(R.string.rename)
                .setView(textGp)

                .setPositiveButton(getString(android.R.string.ok)) { dialog, which ->
                    // 获取输入框的内容
                    if (StringUtils.compileExChar(textGp.text.toString().trim { it <= ' ' })) {
                        ToastUtils.showShort(getString(R.string.rename_tip_check))
                    } else {
                        light?.name=textGp.text.toString().trim { it <= ' ' }
                        DBUtils.updateLight(light!!)
                        toolbar.title=light?.name
                        dialog.dismiss()
                    }
                }
                .setNegativeButton(getString(R.string.btn_cancel)) { dialog, which -> dialog.dismiss() }.show()
    }

    private val barChangeListener = object : SeekBar.OnSeekBarChangeListener {

        private var preTime: Long = 0
        private val delayTime = 30

        override fun onStopTrackingTouch(seekBar: SeekBar) {
            this.onValueChange(seekBar, seekBar.progress, true, true)
        }

        override fun onStartTrackingTouch(seekBar: SeekBar) {
            this.preTime = System.currentTimeMillis()
            this.onValueChange(seekBar, seekBar.progress, true, false)
        }

        override fun onProgressChanged(seekBar: SeekBar, progress: Int,
                                       fromUser: Boolean) {
            val currentTime = System.currentTimeMillis()

            if (currentTime - this.preTime > this.delayTime) {
                this.onValueChange(seekBar, progress, true, false)
                this.preTime = currentTime
            }
        }

        private fun onValueChange(view: View, progress: Int, immediate: Boolean, isStopTracking: Boolean) {

            val addr = light!!.meshAddr
            val opcode: Byte
            val params: ByteArray
            if (view === sb_brightness) {
                //                progress += 5;
                //                Log.d(TAG, "onValueChange: "+progress);
                tv_brightness!!.text = getString(R.string.device_setting_brightness, progress.toString() + "")
                opcode = Opcode.SET_LUM
                params = byteArrayOf(progress.toByte())

                light!!.brightness = progress
                TelinkLightService.Instance().sendCommandNoResponse(opcode, addr, params)
                if (isStopTracking) {
                    DBUtils.updateLight(light!!)
                }
            } else if (view === sb_temperature) {

                opcode = Opcode.SET_TEMPERATURE
                params = byteArrayOf(0x05, progress.toByte())
                tv_temperature!!.text = getString(R.string.device_setting_temperature, progress.toString() + "")

                light!!.colorTemperature = progress
                TelinkLightService.Instance().sendCommandNoResponse(opcode, addr, params)
                if (isStopTracking) {
                    DBUtils.updateLight(light!!)
                }
            }
        }
    }

    internal var diyOnItemChildClickListener: BaseQuickAdapter.OnItemChildClickListener = BaseQuickAdapter.OnItemChildClickListener { adapter, view, position ->
        val color = presetColors!![position].color
        val brightness = presetColors!![position].brightness
        val red = color and 0xff0000 shr 16
        val green = color and 0x00ff00 shr 8
        val blue = color and 0x0000ff
        Thread {
            changeColor(red.toByte(), green.toByte(), blue.toByte(),true)

            try {
                Thread.sleep(100)
                val addr = light!!.meshAddr
                val opcode: Byte
                val params: ByteArray
                opcode = Opcode.SET_LUM
                params = byteArrayOf(brightness.toByte())
                light!!.brightness = brightness
                light!!.setColor(color)
                for(i in 0..3){
                    Thread.sleep(50)
                    TelinkLightService.Instance().sendCommandNoResponse(opcode, addr!!, params)
                }
//                DBUtils.updateLight(light!!)
            } catch (e: InterruptedException) {
                e.printStackTrace()
            }
        }.start()

        sb_brightness!!.progress = brightness
        tv_brightness.text = getString(R.string.device_setting_brightness, brightness.toString() + "")
        //            scrollView.setBackgroundColor(color);
        color_r!!.text = red.toString() + ""
        color_g!!.text = green.toString() + ""
        color_b!!.text = blue.toString() + ""
    }

    override fun onPause() {
        super.onPause()
        DBUtils.updateLight(light!!)
    }

    internal var diyOnItemChildLongClickListener: BaseQuickAdapter.OnItemChildLongClickListener = BaseQuickAdapter.OnItemChildLongClickListener { adapter, view, position ->
        presetColors!![position].color = light!!.getColor()
        presetColors!![position].brightness = light!!.brightness
        val textView = adapter.getViewByPosition(position, R.id.btn_diy_preset) as TextView?
        textView!!.text = light!!.brightness.toString() + "%"
        textView.setBackgroundColor(-0x1000000 or light!!.getColor())
        SharedPreferencesHelper.putObject(this, Constant.PRESET_COLOR, presetColors)
        false
    }

    private val colorEnvelopeListener = ColorEnvelopeListener { envelope, fromUser ->
        val argb = envelope.argb

        color_r!!.text = argb[1].toString() + ""
        color_g!!.text = argb[2].toString() + ""
        color_b!!.text = argb[3].toString() + ""

        //            int color = Color.rgb(argb[1], argb[2], argb[3]);
        val color = argb[1] shl 16 or (argb[2] shl 8) or argb[3]
//        Log.d(RGBDeviceSettingFragment.TAG, "onColorSelected: " + Integer.toHexString(color))
        if (fromUser) {
            //                scrollView.setBackgroundColor(0xff000000|color);
            if (argb[1] == 0 && argb[2] == 0 && argb[3] == 0) {
            } else {
                light!!.setColor(color)
                Thread { changeColor(argb[1].toByte(), argb[2].toByte(), argb[3].toByte(), false) }.start()
            }
        }
    }

    internal var otaPrepareListner: OtaPrepareListner = object : OtaPrepareListner {
        override fun startGetVersion() {
            showLoadingDialog(getString(R.string.verification_version))
        }

        override fun getVersionSuccess(s: String) {
            ToastUtils.showLong(R.string.verification_version_success)
            hideLoadingDialog()
        }

        override fun getVersionFail() {
            ToastUtils.showLong(R.string.verification_version_fail)
            hideLoadingDialog()
        }


        override fun downLoadFileSuccess() {
            transformView()
        }

        override fun downLoadFileFail(message: String) {
            ToastUtils.showLong(R.string.download_pack_fail)
        }
    }

    private fun checkPermission() {
        mDisposable.add(
                mRxPermission!!.request(Manifest.permission.READ_EXTERNAL_STORAGE,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE).subscribe { granted ->
                    if (granted!!) {
                        OtaPrepareUtils.instance().gotoUpdateView(this@RGBDeviceSettingActivity, localVersion, otaPrepareListner)
                    } else {
                        ToastUtils.showLong(R.string.update_permission_tip)
                    }
                })
    }

    private fun transformView() {
        val intent = Intent(this@RGBDeviceSettingActivity, OTAUpdateActivity::class.java)
        intent.putExtra(Constant.UPDATE_LIGHT, light)
        startActivity(intent)
        finish()
    }

    private fun getVersion() {
        var dstAdress = 0
        if (TelinkApplication.getInstance().connectDevice != null) {
            Commander.getDeviceVersion(light!!.meshAddr, { s ->
                localVersion = s
                if (toolbar.title != null) {
                    if (OtaPrepareUtils.instance().checkSupportOta(localVersion)!!) {
//                        toolbar.title!!.visibility = View.VISIBLE
                        lightVersion.text = localVersion
                        light!!.version = localVersion
                        tvOta!!.visibility = View.VISIBLE
                    } else {
//                        toolbar.title!!.visibility = View.GONE
                        tvOta!!.visibility = View.GONE
                    }
                }
                null
            }, {
                if (toolbar.title != null) {
//                    toolbar.title!!.visibility = View.GONE
                    tvOta!!.visibility = View.GONE
                }
                null
            })
        } else {
            dstAdress = 0
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        this.requestWindowFeature(Window.FEATURE_NO_TITLE)
        this.setContentView(R.layout.activity_rgb_device_setting)
        this.mApp = this.application as TelinkLightApplication
        manager = DataManager(mApp, mApp!!.mesh.name, mApp!!.mesh.password)
        initToolbar()
        initView()
        getVersion()
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

    override fun onDestroy() {
        super.onDestroy()
        mDisposable.dispose()
    }

    private fun toRGBGradientView() {
        val intent = Intent(this, RGBGradientActivity::class.java)
        intent.putExtra(Constant.TYPE_VIEW, Constant.TYPE_LIGHT)
        intent.putExtra(Constant.TYPE_VIEW_ADDRESS, light!!.meshAddr)

        startActivityForResult(intent, 0)
    }

    private fun updateGroup() {
        val intent = Intent(this,
                LightGroupingActivity::class.java)
        intent.putExtra("light", light)
        intent.putExtra("gpAddress", gpAddress)
        startActivity(intent)
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun initView() {
        this.light = this.intent.extras!!.get(Constant.LIGHT_ARESS_KEY) as DbLight
        this.fromWhere = this.intent.getStringExtra(Constant.LIGHT_REFRESH_KEY)
        this.gpAddress = this.intent.getIntExtra(Constant.GROUP_ARESS_KEY, 0)
        mApplication = this.application as TelinkLightApplication
        dataManager = DataManager(this, mApplication!!.mesh.name, mApplication!!.mesh.password)
        tvRename.visibility=View.VISIBLE
        toolbar.title=light?.name

        tvRename!!.setOnClickListener(this.clickListener)
        tvOta!!.setOnClickListener(this.clickListener)
        btn_rename!!.setOnClickListener(this.clickListener)
        update_group!!.setOnClickListener(this.clickListener)
        btn_remove!!.setOnClickListener(this.clickListener)
        dynamic_rgb!!.setOnClickListener(this.clickListener)

        mRxPermission = RxPermissions(this)
        
        sb_brightness!!.max = 100
        sb_temperature!!.max = 100

        color_picker!!.setColorListener(colorEnvelopeListener)
        color_picker!!.setOnTouchListener { v, event ->
            v.parent.requestDisallowInterceptTouchEvent(true)
            false
        }

        mConnectDevice = TelinkLightApplication.getInstance().connectDevice

        presetColors = SharedPreferencesHelper.getObject(this, Constant.PRESET_COLOR) as? MutableList<ItemColorPreset>
        if (presetColors == null) {
            presetColors = ArrayList()
            for (i in 0..4) {
                val itemColorPreset = ItemColorPreset()
                itemColorPreset.color = OtherUtils.getCreateInitColor(i)
                presetColors!!.add(itemColorPreset)
            }
        }

        diy_color_recycler_list_view!!.layoutManager = GridLayoutManager(this, 5)
        colorSelectDiyRecyclerViewAdapter = ColorSelectDiyRecyclerViewAdapter(R.layout.color_select_diy_item, presetColors)
        colorSelectDiyRecyclerViewAdapter!!.setOnItemChildClickListener(diyOnItemChildClickListener)
        colorSelectDiyRecyclerViewAdapter!!.setOnItemChildLongClickListener(diyOnItemChildLongClickListener)
        colorSelectDiyRecyclerViewAdapter!!.bindToRecyclerView(diy_color_recycler_list_view)
        
        btn_rename!!.visibility = View.GONE
        sb_brightness!!.progress = light!!.brightness
        tv_brightness!!.text = getString(R.string.device_setting_brightness, light!!.brightness.toString() + "")
        sb_temperature!!.progress = light!!.colorTemperature
        tv_temperature!!.text = getString(R.string.device_setting_temperature, light!!.colorTemperature.toString() + "")
        
        this.sb_brightness!!.setOnSeekBarChangeListener(this.barChangeListener)
        this.sb_temperature!!.setOnSeekBarChangeListener(this.barChangeListener)
    }

    private fun changeColor(R: Byte, G: Byte, B: Byte, isOnceSet: Boolean) {

        DBUtils.updateLight(light!!)

        var red = R
        var green = G
        var blue = B

        val addr = light!!.meshAddr
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
        if(isOnceSet){
            for(i in 0..3){
                Thread.sleep(50)
                TelinkLightService.Instance().sendCommandNoResponse(opcode, addr!!, params)
            }
        }else{
            TelinkLightService.Instance().sendCommandNoResponse(opcode, addr!!, params)
        }
    }

    private fun sendInitCmd(brightness: Int, colorTemperature: Int) {
        val addr = light!!.meshAddr
        var opcode: Byte
        var params: ByteArray
        //                progress += 5;
        //                Log.d(TAG, "onValueChange: "+progress);
        opcode = Opcode.SET_LUM
        params = byteArrayOf(brightness.toByte())
        light!!.brightness = brightness
        TelinkLightService.Instance().sendCommandNoResponse(opcode, addr, params)

        try {
            Thread.sleep(200)
        } catch (e: InterruptedException) {
            e.printStackTrace()
        } finally {
            opcode = Opcode.SET_TEMPERATURE
            params = byteArrayOf(0x05, colorTemperature.toByte())
            light!!.colorTemperature = colorTemperature
            TelinkLightService.Instance().sendCommandNoResponse(opcode, addr, params)
        }
    }

    fun remove() {
        AlertDialog.Builder(Objects.requireNonNull<Activity>(this)).setMessage(R.string.delete_light_confirm)
                .setPositiveButton(android.R.string.ok) { dialog, which ->

                    if (TelinkLightService.Instance().adapter.mLightCtrl.currentLight.isConnected) {
                        val opcode = Opcode.KICK_OUT
                        TelinkLightService.Instance().sendCommandNoResponse(opcode, light!!.meshAddr, null)
                        DBUtils.deleteLight(light!!)
                        if (TelinkLightApplication.getApp().mesh.removeDeviceByMeshAddress(light!!.meshAddr)) {
                            TelinkLightApplication.getApp().mesh.saveOrUpdate(this!!)
                        }
                        if (mConnectDevice != null) {
                            Log.d(this!!.javaClass.simpleName, "mConnectDevice.meshAddress = " + mConnectDevice!!.meshAddress)
                            Log.d(this!!.javaClass.simpleName, "light.getMeshAddr() = " + light!!.meshAddr)
                            if (light!!.meshAddr == mConnectDevice!!.meshAddress) {
                                this!!.setResult(Activity.RESULT_OK, Intent().putExtra("data", true))
                            }
                        }
                        this!!.finish()


                    } else {
                        ToastUtils.showLong("当前处于未连接状态，重连中。。。")
                        this!!.finish()
                    }
                }
                .setNegativeButton(R.string.btn_cancel, null)
                .show()

        //            if (gpAddress != 0) {
        //                Group group = manager.getGroup(gpAddress, getActivity());
        //                group.containsLightList.remove((Integer) light.getMeshAddr());
        //                manager.update_group(group, getActivity());
        //            }
        //            getActivity().finish();
    }

    /**
     * 自动重连
     */
    private fun autoConnect() {

        if (TelinkLightService.Instance() != null) {

            if (TelinkLightService.Instance().mode != LightAdapter.MODE_AUTO_CONNECT_MESH) {

                ToastUtils.showLong(getString(R.string.connecting))
                SharedPreferencesHelper.putBoolean(this, Constant.CONNECT_STATE_SUCCESS_KEY, false)

                if (this.mApp!!.isEmptyMesh)
                    return

                //                Lights.getInstance().clear();
                this.mApp!!.refreshLights()

                val mesh = this.mApp!!.mesh

                if (TextUtils.isEmpty(mesh.name) || TextUtils.isEmpty(mesh.password)) {
                    TelinkLightService.Instance().idleMode(true)
                    return
                }

                val account = SharedPreferencesHelper.getString(TelinkLightApplication.getInstance(),
                        Constant.DB_NAME_KEY, "dadou")

                //自动重连参数
                val connectParams = Parameters.createAutoConnectParameters()
                connectParams.setMeshName(mesh.name)
                if (SharedPreferencesHelper.getString(TelinkLightApplication.getInstance(), Constant.USER_TYPE, Constant.USER_TYPE_OLD) == Constant.USER_TYPE_NEW) {
                    connectParams.setPassword(NetworkFactory.md5(
                            NetworkFactory.md5(mesh.password) + account))
                } else {
                    connectParams.setPassword(mesh.password)
                }
                connectParams.autoEnableNotification(true)

                // 之前是否有在做MeshOTA操作，是则继续
                if (mesh.isOtaProcessing) {
                    connectParams.setConnectMac(mesh.otaDevice!!.mac)
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
}
