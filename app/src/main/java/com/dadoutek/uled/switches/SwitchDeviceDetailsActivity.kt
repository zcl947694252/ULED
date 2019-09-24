package com.dadoutek.uled.switches

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.support.v7.util.DiffUtil
import android.support.v7.widget.DefaultItemAnimator
import android.support.v7.widget.GridLayoutManager
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import com.blankj.utilcode.util.LogUtils
import com.blankj.utilcode.util.ToastUtils
import com.chad.library.adapter.base.BaseQuickAdapter
import com.dadoutek.uled.R
import com.dadoutek.uled.communicate.Commander
import com.dadoutek.uled.intf.SyncCallback
import com.dadoutek.uled.model.Constant
import com.dadoutek.uled.model.DbModel.DBUtils
import com.dadoutek.uled.model.DbModel.DbSwitch
import com.dadoutek.uled.model.DeviceType
import com.dadoutek.uled.model.Opcode
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
import com.telink.bluetooth.event.LeScanEvent
import com.telink.bluetooth.light.DeviceInfo
import com.telink.bluetooth.light.LightAdapter
import com.telink.bluetooth.light.Parameters
import com.telink.util.Event
import com.telink.util.EventListener
import io.reactivex.disposables.Disposable
import kotlinx.android.synthetic.main.activity_switch_device_details.*
import kotlinx.android.synthetic.main.toolbar.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jetbrains.anko.startActivity
import java.util.*

class SwitchDeviceDetailsActivity : TelinkBaseActivity(), EventListener<String>, View.OnClickListener {
    private var isclickOTA: Boolean = false
    private var isOta: Boolean = false
    private var mDeviceMeshName: String = Constant.PIR_SWITCH_MESH_NAME
    private fun stopConnectTimer() {
        mConnectDisposal?.dispose()
    }

    private fun onLogin() {
        mScanTimeoutDisposal?.dispose()
        this.mApplication?.removeEventListener(this)
        hideLoadingDialog()
        if (bestRSSIDevice?.productUUID == DeviceType.NORMAL_SWITCH ||
                bestRSSIDevice?.productUUID == DeviceType.NORMAL_SWITCH2) {
            startActivity<ConfigNormalSwitchActivity>("deviceInfo" to bestRSSIDevice!!, "group" to "true", "switch" to currentSwitch)
            finish()
        } else if (bestRSSIDevice?.productUUID == DeviceType.SCENE_SWITCH) {
            startActivity<ConfigSceneSwitchActivity>("deviceInfo" to bestRSSIDevice!!, "group" to "true", "switch" to currentSwitch)
            finish()
        } else if (bestRSSIDevice?.productUUID == DeviceType.SMART_CURTAIN_SWITCH) {
            startActivity<ConfigCurtainSwitchActivity>("deviceInfo" to bestRSSIDevice!!, "group" to "true", "switch" to currentSwitch)
            finish()
        }
    }

    private fun onLeScan(leScanEvent: LeScanEvent) {
        val meshAddress = Constant.SWITCH_PIR_ADDRESS
        val deviceInfo: DeviceInfo = leScanEvent.args

        if (meshAddress == -1) {
            return
        }

        when (leScanEvent.args.productUUID) {
            DeviceType.SCENE_SWITCH, DeviceType.NORMAL_SWITCH, DeviceType.NORMAL_SWITCH2, DeviceType.SMART_CURTAIN_SWITCH -> {
                val MAX_RSSI = 81
                if (leScanEvent.args.rssi < MAX_RSSI) {

                    if (bestRSSIDevice != null) {
                        //扫到的灯的信号更好并且没有连接失败过就把要连接的灯替换为当前扫到的这个。
                        if (deviceInfo.rssi > bestRSSIDevice?.rssi ?: 0) {
                            bestRSSIDevice = deviceInfo
                        }
                    } else {
                        bestRSSIDevice = deviceInfo
                    }
                } else {
                    ToastUtils.showLong(getString(R.string.rssi_low))
                }
            }
        }
    }

    private lateinit var switchData: MutableList<DbSwitch>
    private var mScanTimeoutDisposal: Disposable? = null
    private var mCheckRssiDisposal: Disposable? = null
    private var adapter: SwitchDeviceDetailsAdapter? = null
    private var currentLight: DbSwitch? = null
    private var positionCurrent: Int = 0
    private var mConnectDevice: DeviceInfo? = null
    private var acitivityIsAlive = true
    private var bestRSSIDevice: DeviceInfo? = null
    private var mApplication: TelinkLightApplication? = null
    private var mConnectDisposal: Disposable? = null
    private var currentSwitch: DbSwitch? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_switch_device_details)
        this.mApplication = this.application as TelinkLightApplication
    }

    override fun onResume() {
        super.onResume()
        initData()
        initView()
    }

    private fun initData() {
        switchData = DBUtils.getAllSwitch()
        SyncDataPutOrGetUtils.syncPutDataStart(TelinkLightApplication.getApp(), object : SyncCallback {
            override fun start() {
                LogUtils.e("zcl____同步开关________start")
            }

            override fun complete() {
                LogUtils.e("zcl____同步开关________complete")
            }

            override fun error(msg: String?) {
                LogUtils.e("zcl____同步开关________error")
            }
        })
        if (switchData.size > 0) {
            no_device_relativeLayout.visibility = View.GONE
            recycleView.visibility = View.VISIBLE
            toolbar!!.findViewById<ImageView>(R.id.img_function1).visibility = View.GONE
        } else {
            no_device_relativeLayout.visibility = View.VISIBLE
            recycleView.visibility = View.GONE
            toolbar!!.findViewById<ImageView>(R.id.img_function1).visibility = View.VISIBLE
            toolbar!!.findViewById<ImageView>(R.id.img_function1).setOnClickListener {
                if (dialog?.visibility == View.GONE) {
                    showPopupMenu()
                } else {
                    hidePopupMenu()
                }
            }
        }
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.add_device_btn -> addDevice()
        }
    }

    private fun addDevice() {
        startActivity(Intent(this, ScanningSwitchActivity::class.java))
    }

    private fun initView() {
        mConnectDevice = TelinkLightApplication.getApp().connectDevice
        recycleView!!.layoutManager = GridLayoutManager(this, 3)
        recycleView!!.itemAnimator = DefaultItemAnimator()
        adapter = SwitchDeviceDetailsAdapter(R.layout.device_detail_adapter, switchData)
        adapter!!.bindToRecyclerView(recycleView)

        adapter!!.onItemChildClickListener = onItemChildClickListener
        for (i in switchData.indices)
            switchData[i].updateIcon()

        add_device_btn.setOnClickListener(this)
        toolbar.setNavigationIcon(R.drawable.navigation_back_white)
        toolbar.setNavigationOnClickListener {
            finish()
        }
        toolbar.title = getString(R.string.switch_name) + " (" + switchData!!.size + ")"
    }

    private fun showPopupMenu() {
        dialog?.visibility = View.VISIBLE
    }

    private fun hidePopupMenu() {
        dialog?.visibility = View.GONE
    }

    var onItemChildClickListener = BaseQuickAdapter.OnItemChildClickListener { adapter, view, position ->
        currentLight = switchData?.get(position)
        positionCurrent = position
        if (view.id == R.id.tv_setting) {
            showPopupWindow(view, position)
        }
    }

    private fun showPopupWindow(view: View?, position: Int) {
        var views = LayoutInflater.from(this).inflate(R.layout.popwindown_switch, null)
        var set = view!!.findViewById<ImageView>(R.id.tv_setting)
        var popupWindow = PopupWindow(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        popupWindow.contentView = views
        popupWindow.isFocusable = true
        popupWindow.showAsDropDown(set)
        currentSwitch = switchData[position]

        var relocation = views.findViewById<TextView>(R.id.switch_group)
        var ota = views.findViewById<TextView>(R.id.ota)
        var delete = views.findViewById<TextView>(R.id.deleteBtn)
        var rename = views.findViewById<TextView>(R.id.rename)
        delete.text = getString(R.string.delete)

        rename.setOnClickListener {
            isOta = false
            isclickOTA = true
            popupWindow.dismiss()
            val textGp = EditText(this)
            StringUtils.initEditTextFilter(textGp)
            textGp.setText(currentSwitch?.name)
            textGp.setSelection(textGp.text.toString().length)
            android.app.AlertDialog.Builder(this@SwitchDeviceDetailsActivity)
                    .setTitle(R.string.rename)
                    .setView(textGp)
                    .setPositiveButton(getString(android.R.string.ok)) { dialog, which ->
                        // 获取输入框的内容
                        if (StringUtils.compileExChar(textGp.text.toString().trim { it <= ' ' })) {
                            ToastUtils.showShort(getString(R.string.rename_tip_check))
                        } else {
                            currentSwitch?.name = textGp.text.toString().trim { it <= ' ' }
                            DBUtils.updateSwicth(currentSwitch!!)
                            adapter!!.notifyDataSetChanged()
                            dialog.dismiss()
                        }
                    }
                    .setNegativeButton(getString(R.string.btn_cancel)) { dialog, which -> dialog.dismiss() }.show()
        }

        relocation.setOnClickListener {
            popupWindow.dismiss()
            isOta = false
            isclickOTA = false

            if (currentSwitch != null) {
                TelinkLightService.Instance()?.idleMode(true)
                connect()
            }
        }

        ota.setOnClickListener {
            popupWindow.dismiss()
            isOta = true
            isclickOTA = true
            if (TelinkLightApplication.getApp().connectDevice == null) {
                connect()
            } else {
                TelinkLightService.Instance()?.idleMode(true)
                TelinkLightService.Instance()?.disconnect()
            }
        }
        delete.setOnClickListener {
            //恢复出厂设置
            isOta = false
            isclickOTA = true
            popupWindow.dismiss()
            var deleteSwitch = switchData.get(position)
            AlertDialog.Builder(Objects.requireNonNull<AppCompatActivity>(this)).setMessage(R.string.delete_switch_confirm)
                    .setPositiveButton(android.R.string.ok) { dialog, which ->
                        DBUtils.deleteSwitch(deleteSwitch)
                        notifyData()
                        Toast.makeText(this@SwitchDeviceDetailsActivity, R.string.delete_switch_success, Toast.LENGTH_LONG).show()
                        val opcode = Opcode.KICK_OUT
                        TelinkLightService.Instance()?.sendCommandNoResponse(opcode, deleteSwitch.meshAddr, null)
                        if (TelinkLightApplication.getApp().mesh.removeDeviceByMeshAddress(deleteSwitch.meshAddr)) {
                            TelinkLightApplication.getApp().mesh.saveOrUpdate(this)
                        }
                        if (mConnectDevice != null) {
                            Log.d(this.javaClass.simpleName, "mConnectDevice.meshAddress = " + mConnectDevice?.meshAddress)
                            Log.d(this.javaClass.simpleName, "light.getMeshAddr() = " + currentLight?.meshAddr)
                            if (deleteSwitch.meshAddr == mConnectDevice?.meshAddress) {
                                GlobalScope.launch {
                                    delay(2500)//踢灯后没有回调 状态刷新不及时 延时2秒获取最新连接状态
                                    if (this@SwitchDeviceDetailsActivity == null ||
                                            this@SwitchDeviceDetailsActivity.isDestroyed ||
                                            this@SwitchDeviceDetailsActivity.isFinishing || !acitivityIsAlive) {
                                    } else {
                                        connect()
                                    }
                                }
                            }
                        }
                    }
                    .setNegativeButton(R.string.btn_cancel, null)
                    .show()
        }
    }

    override fun performed(event: Event<String>?) {
        when (event?.type) {
            LeScanEvent.LE_SCAN -> this.onLeScan(event as LeScanEvent)
            LeScanEvent.LE_SCAN_COMPLETED -> this.onLeScan(event as LeScanEvent)
            LeScanEvent.LE_SCAN_TIMEOUT -> {
                ToastUtils.showShort(getString(R.string.scanning_timeout))
                hideLoadingDialog()
            }
            DeviceEvent.STATUS_CHANGED -> this.onDeviceStatusChanged(event as DeviceEvent)

            ErrorReportEvent.ERROR_REPORT -> {
                val info = (event as ErrorReportEvent).args
                onErrorReportNormal(info)
                hideLoadingDialog()
            }
        }
    }

    private fun onDeviceStatusChanged(deviceEvent: DeviceEvent) {
        hideLoadingDialog()
        val deviceInfo = deviceEvent.args
        mDeviceMeshName = deviceInfo.meshName ?: getString(R.string.unnamed)
        when (deviceInfo.status) {
            LightAdapter.STATUS_LOGIN -> {
                if (isOta) {
                    getVersion()
                } else {
                    bestRSSIDevice = deviceInfo
                    onLogin()//判断进入那个开关设置界面

                    stopConnectTimer()
                    LogUtils.d("connected22")
                }
            }
            LightAdapter.STATUS_LOGOUT -> {
                if (isclickOTA)
                    connect()
                isclickOTA = false
            }

            LightAdapter.STATUS_CONNECTED -> {
                LogUtils.d("connected11")
            }
        }

    }

    private fun getVersion() {
        Log.e("zcl", "zcl******进入")
        isclickOTA = false
        if (TelinkApplication.getInstance().connectDevice != null) {
            Log.e("TAG", currentLight!!.meshAddr.toString())
            Commander.getDeviceVersion(currentLight!!.meshAddr, { s ->
                if ("" != s)
                    if (OtaPrepareUtils.instance().checkSupportOta(s)!!) {
                        currentLight!!.version = s
                        val intent = Intent(this@SwitchDeviceDetailsActivity, OTAUpdateActivity::class.java)
                        intent.putExtra(Constant.OTA_MAC, currentLight?.macAddr)
                        intent.putExtra(Constant.OTA_MES_Add, currentLight?.meshAddr)
                        intent.putExtra(Constant.OTA_VERSION, currentLight?.version)
                        startActivity(intent)
                    } else {
                        ToastUtils.showShort(getString(R.string.version_disabled))
                    }
            }, {})
        }
    }

    private fun autoConnectSwitch() {
        this.mApplication?.removeEventListener(this)
        this.mApplication?.addEventListener(LeScanEvent.LE_SCAN_TIMEOUT, this)
        this.mApplication?.addEventListener(DeviceEvent.STATUS_CHANGED, this)
        this.mApplication?.addEventListener(ErrorReportEvent.ERROR_REPORT, this)
        //自动重连参数
        val connectParams = Parameters.createAutoConnectParameters()
        connectParams.setMeshName(DBUtils.lastUser?.controlMeshName)
        connectParams.setConnectMac(currentLight!!.macAddr)
        connectParams.setPassword(NetworkFactory.md5(NetworkFactory.md5(DBUtils.lastUser?.controlMeshName) + DBUtils.lastUser?.controlMeshName).substring(0, 16))
        connectParams.autoEnableNotification(true)
        connectParams.setTimeoutSeconds(10)
        this.runOnUiThread {
            showLoadingDialog(getString(R.string.connecting))
        }

        //连接，如断开会自动重连 自动登录 不用login
        GlobalScope.launch {
            TelinkLightService.Instance()?.autoConnect(connectParams)
        }
    }

    @SuppressLint("CheckResult")
    private fun connect() {
        try {
            mCheckRssiDisposal?.dispose()
            mCheckRssiDisposal = RxPermissions(this).request(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.BLUETOOTH,
                    Manifest.permission.BLUETOOTH_ADMIN)
                    .subscribe({
                        if (it) {
                            autoConnectSwitch()
                        } else {    //没有授予权限
                            DialogUtils.showNoBlePermissionDialog(this, { autoConnectSwitch() }, { finish() })
                        }
                    }, {})
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    var locationServiceDialog: AlertDialog? = null
    fun showOpenLocationServiceDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle(R.string.open_location_service)
        builder.setNegativeButton(getString(android.R.string.ok)) { dialog, which ->
            BleUtils.jumpLocationSetting()
        }
        locationServiceDialog = builder.create()
        locationServiceDialog?.setCancelable(false)
        locationServiceDialog?.show()
    }

    fun hideLocationServiceDialog() {
        locationServiceDialog?.hide()
    }

    fun notifyData() {
        val mOldDatas: MutableList<DbSwitch>? = switchData
        val mNewDatas: MutableList<DbSwitch>? = getNewData()
        val diffResult = DiffUtil.calculateDiff(object : DiffUtil.Callback() {
            override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                return mOldDatas?.get(oldItemPosition)?.id?.equals(mNewDatas?.get
                (newItemPosition)?.id) ?: false
            }

            override fun getOldListSize(): Int {
                return mOldDatas?.size ?: 0
            }

            override fun getNewListSize(): Int {
                return mNewDatas?.size ?: 0
            }

            override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                val beanOld = mOldDatas?.get(oldItemPosition)
                val beanNew = mNewDatas?.get(newItemPosition)
                return if (!beanOld?.name.equals(beanNew?.name)) {
                    return false//如果有内容不同，就返回false
                } else true

            }
        }, true)
        adapter?.let { diffResult.dispatchUpdatesTo(it) }
        switchData = mNewDatas!!
        toolbar.title = getString(R.string.switch_name) + " (" + switchData!!.size + ")"
        adapter!!.setNewData(switchData)
        initData()
        initView()
    }

    private fun getNewData(): MutableList<DbSwitch> {
        switchData = DBUtils.getAllSwitch()
        toolbar.title = (currentLight!!.name ?: "")
        return switchData
    }

    override fun onDestroy() {
        super.onDestroy()
        acitivityIsAlive = false
    }
}
