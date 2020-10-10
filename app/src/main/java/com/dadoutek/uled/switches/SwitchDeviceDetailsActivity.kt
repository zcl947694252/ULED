package com.dadoutek.uled.switches

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.support.v7.util.DiffUtil
import android.support.v7.widget.*
import android.support.v7.widget.Toolbar
import android.text.TextUtils
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import com.blankj.utilcode.util.LogUtils
import com.blankj.utilcode.util.ToastUtils
import com.chad.library.adapter.base.BaseQuickAdapter
import com.dadoutek.uled.R
import com.dadoutek.uled.base.TelinkBaseToolbarActivity
import com.dadoutek.uled.communicate.Commander
import com.dadoutek.uled.intf.OtaPrepareListner
import com.dadoutek.uled.intf.SyncCallback
import com.dadoutek.uled.model.Constant
import com.dadoutek.uled.model.Constant.*
import com.dadoutek.uled.model.dbModel.DBUtils
import com.dadoutek.uled.model.dbModel.DbSwitch
import com.dadoutek.uled.model.DeviceType
import com.dadoutek.uled.model.SharedPreferencesHelper
import com.dadoutek.uled.model.dbModel.DbGroup
import com.dadoutek.uled.model.routerModel.RouterModel
import com.dadoutek.uled.network.NetworkStatusCode
import com.dadoutek.uled.ota.OTAUpdateActivity
import com.dadoutek.uled.router.BindRouterActivity
import com.dadoutek.uled.router.bean.CmdBodyBean
import com.dadoutek.uled.router.bean.RouteGroupingOrDelOrGetVerBean
import com.dadoutek.uled.scene.NewSceneSetAct
import com.dadoutek.uled.tellink.TelinkLightApplication
import com.dadoutek.uled.tellink.TelinkLightService
import com.dadoutek.uled.util.OtaPrepareUtils
import com.dadoutek.uled.util.StringUtils
import com.dadoutek.uled.util.SyncDataPutOrGetUtils
import com.telink.TelinkApplication
import com.telink.bluetooth.light.DeviceInfo
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_switch_device_details.*
import kotlinx.android.synthetic.main.activity_switch_device_details.add_device_btn
import kotlinx.android.synthetic.main.activity_switch_device_details.no_device_relativeLayout
import kotlinx.android.synthetic.main.activity_switch_device_details.recycleView
import kotlinx.android.synthetic.main.toolbar.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.greenrobot.greendao.DbUtils
import org.jetbrains.anko.startActivity
import java.util.*
import java.util.concurrent.TimeUnit

/**
 * 开关列表
 */
class SwitchDeviceDetailsActivity : TelinkBaseToolbarActivity() {
    private var disposableTimer: Disposable? = null
    private var popupWindow: PopupWindow? = null
    private var popVersion: TextView? = null
    private var views: View? = null
    private var downloadDispoable: Disposable? = null
    private var mConnectDeviceDisposable: Disposable? = null
    private val SCENE_MAX_COUNT = 100
    private var last_start_time = 0
    private var debounce_time = 1000
    private var switchData: MutableList<DbSwitch> = mutableListOf()
    private var mScanTimeoutDisposal: Disposable? = null
    private var adapter: SwitchDeviceDetailsAdapter? = null
    private var currentDevice: DbSwitch? = null
    private var positionCurrent: Int = 10000
    private var mConnectDevice: DeviceInfo? = null
    private var acitivityIsAlive = true
    private var mApplication: TelinkLightApplication? = null
    private var install_device: TextView? = null
    private var create_group: TextView? = null
    private var create_scene: TextView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        type = this.intent.getIntExtra(DEVICE_TYPE, 0)
        this.mApplication = this.application as TelinkLightApplication
    }

    override fun onResume() {
        super.onResume()
        views = LayoutInflater.from(this).inflate(R.layout.popwindown_switch, null)
        initData()
        initView()
        makePop()
    }

    override fun editeDeviceAdapter() {
        adapter!!.changeState(isEdite)
        adapter!!.notifyDataSetChanged()
    }

    override fun setToolbar(): Toolbar {
        return toolbar
    }

    override fun batchGpVisible(): Boolean {
        return false
    }

    override fun bindDeviceRouter() {
        val dbGroup = DbGroup()
        dbGroup.deviceType = DeviceType.NORMAL_SWITCH.toLong()
        var intent = Intent(this, BindRouterActivity::class.java)
        intent.putExtra("group", dbGroup)
        startActivity(intent)
    }

    //显示路由
    override fun bindRouterVisible(): Boolean {
        return false
    }

    override fun setDeletePositiveBtn() {
        currentDevice?.let {
            DBUtils.deleteSwitch(it)
            switchData.remove(it)
        }
        adapter?.notifyDataSetChanged()
        isEmptyDevice()
    }

    override fun setDeviceDataSize(num: Int): Int {
        return switchData.size
    }

    override fun setLayoutId(): Int {
        return R.layout.activity_switch_device_details
    }

    private fun makePop() {
        popupWindow = PopupWindow(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        views?.let { itv ->
            popupWindow?.contentView = itv
            popupWindow?.isFocusable = true

            val reConfig = itv.findViewById<TextView>(R.id.switch_group)
            val ota = itv.findViewById<TextView>(R.id.ota)
            val delete = itv.findViewById<TextView>(R.id.deleteBtn)
            val rename = itv.findViewById<TextView>(R.id.rename)
            popVersion = itv.findViewById(R.id.pop_version)

            popVersion?.text = getString(R.string.firmware_version) + currentDevice?.version
            popVersion?.visibility = View.GONE
            delete.text = getString(R.string.delete)
            rename.setOnClickListener {
                if (isRightPos()) return@setOnClickListener
                if (!TextUtils.isEmpty(currentDevice?.name))
                    renameEt?.setText(currentDevice?.name)
                renameEt?.setSelection(renameEt?.text.toString().length)

                if (this != null && !this.isFinishing) {
                    renameDialog?.dismiss()
                    renameDialog?.show()
                }

                renameConfirm?.setOnClickListener {    // 获取输入框的内容
                    if (StringUtils.compileExChar(renameEt?.text.toString().trim { it <= ' ' })) {
                        ToastUtils.showLong(getString(R.string.rename_tip_check))
                    } else {
                        currentDevice?.name = renameEt?.text.toString().trim { it <= ' ' }
                        DBUtils.updateSwicth(currentDevice!!)
                        toolbarTv.text = currentDevice?.name
                        adapter!!.notifyDataSetChanged()
                        renameDialog.dismiss()
                    }
                }
            }
            reConfig.setOnClickListener {
                goConfig()

            }
            ota.setOnClickListener {
                if (isRightPos()) return@setOnClickListener
                if (Constant.IS_ROUTE_MODE) return@setOnClickListener
                if (currentDevice != null) {
                    TelinkLightService.Instance()?.idleMode(true)
                    showLoadingDialog(getString(R.string.connecting_tip))
                    connect(macAddress = currentDevice?.macAddr, retryTimes = 3)
                            ?.subscribe(
                                    {
                                        getDeviceVersion(it)
                                        LogUtils.d("login success")
                                    },
                                    {
                                        hideLoadingDialog()
                                        ToastUtils.showShort(it.message)
                                        LogUtils.d(it)
                                    }
                            )
                } else {
                    LogUtils.d("currentDevice = $currentDevice")
                }
            }
            delete.setOnClickListener {
                if (isRightPos()) return@setOnClickListener
                //恢复出厂设置
                var deleteSwitch = switchData[positionCurrent]
                AlertDialog.Builder(Objects.requireNonNull<AppCompatActivity>(this)).setMessage(R.string.delete_switch_confirm)
                        .setPositiveButton(android.R.string.ok) { _, _ ->
                            DBUtils.deleteSwitch(deleteSwitch)
                            notifyData()

                            Toast.makeText(this@SwitchDeviceDetailsActivity, R.string.delete_switch_success, Toast.LENGTH_LONG).show()
                            if (TelinkLightApplication.getApp().mesh.removeDeviceByMeshAddress(deleteSwitch.meshAddr)) {
                                TelinkLightApplication.getApp().mesh.saveOrUpdate(this)
                            }
                            if (mConnectDevice != null) {
                                if (deleteSwitch.meshAddr == mConnectDevice?.meshAddress) {
                                    GlobalScope.launch {
                                        delay(2500)//踢灯后没有回调 状态刷新不及时 延时2秒获取最新连接状态
                                        if (this@SwitchDeviceDetailsActivity == null ||
                                                this@SwitchDeviceDetailsActivity.isDestroyed ||
                                                this@SwitchDeviceDetailsActivity.isFinishing || !acitivityIsAlive) {
                                        } else
                                            connect()
                                    }
                                }
                            }
                        }
                        .setNegativeButton(R.string.btn_cancel, null)
                        .show()
            }
        }
    }

    @SuppressLint("CheckResult")
    private fun goConfig() {
        if (isRightPos()) return
        if (Constant.IS_ROUTE_MODE) return
        if (currentDevice != null) {
            TelinkLightService.Instance()?.idleMode(true)
            showLoadingDialog(getString(R.string.connecting_tip))
            if (IS_ROUTE_MODE) {
                RouterModel.routerConnectSwOrSe(currentDevice?.id ?: 0, 99)?.subscribe({
                    when (it.errorCode) {
                        0 -> {
                            disposableTimer?.dispose()
                            disposableTimer = Observable.timer(1500, TimeUnit.MILLISECONDS)
                                    .subscribe {
                                        hideLoadingDialog()
                                        ToastUtils.showShort(getString(R.string.connect_fail))
                                    }
                        }
                        90018 -> {
                            ToastUtils.showShort(getString(R.string.device_not_exit))
                            hideLoadingDialog()
                        }
                        90008 -> {
                            ToastUtils.showShort(getString(R.string.no_bind_router_cant_perform))
                            hideLoadingDialog()
                        }
                        90005 -> {
                            ToastUtils.showShort(getString(R.string.router_offline))
                            hideLoadingDialog()
                        }
                    }
                }, {
                    ToastUtils.showShort(it.message)
                })
            } else {
                val subscribe = connect(macAddress = currentDevice?.macAddr, retryTimes = 1)
                        ?.subscribe({
                            onLogin(it)//判断进入那个开关设置界面
                            LogUtils.d("login success")
                        }, {
                            hideLoadingDialog()
                            LogUtils.d(it)
                        })
            }
        } else
            LogUtils.d("currentDevice = $currentDevice")
    }

    private fun isRightPos(): Boolean {
        popupWindow?.dismiss()
        if (positionCurrent == 10000) {
            ToastUtils.showShort(getString(R.string.invalid_data))
            return true
        }
        currentDevice = switchData[positionCurrent]
        return false
    }

    private fun initData() {
        switchData.clear()
        switchData.addAll(DBUtils.getAllSwitch())

        setScanningMode(true)
        SyncDataPutOrGetUtils.syncPutDataStart(TelinkLightApplication.getApp(), object : SyncCallback {
            override fun start() {
                LogUtils.v("zcl____同步开关________start")
            }

            override fun complete() {
                LogUtils.v("zcl____同步开关________complete")
            }

            override fun error(msg: String?) {
                LogUtils.v("zcl____同步开关________error")
            }
        })
        isEmptyDevice()
    }

    private fun onLogin(bestRSSIDevice: DeviceInfo) {
        mScanTimeoutDisposal?.dispose()
        hideLoadingDialog()
        if (TelinkApplication.getInstance().connectDevice != null)
            Commander.getDeviceVersion(bestRSSIDevice!!.meshAddress)
                    .subscribe(
                            { version ->
                                if (version != null && version != "") {
                                    skipeSw(bestRSSIDevice, version)
                                } else {
                                    ToastUtils.showLong(getString(R.string.get_version_fail))
                                    initData()
                                }
                            },
                            {
                                showToast(getString(R.string.get_version_fail))
                            }
                    )
    }

    private fun skipeSw(bestRSSIDevice: DeviceInfo, version: String) {
        when (bestRSSIDevice?.productUUID) {
            DeviceType.NORMAL_SWITCH, DeviceType.NORMAL_SWITCH2 -> {
                startActivity<ConfigNormalSwitchActivity>("deviceInfo" to bestRSSIDevice!!, "group" to "true", "switch" to currentDevice, "version" to version)
                finish()
            }
            DeviceType.DOUBLE_SWITCH -> {
                startActivity<DoubleTouchSwitchActivity>("deviceInfo" to bestRSSIDevice!!, "group" to "true", "switch" to currentDevice, "version" to version)
                finish()
            }
            DeviceType.SCENE_SWITCH -> {
                if (version.contains(DeviceType.EIGHT_SWITCH_VERSION))
                    startActivity<ConfigEightSwitchActivity>("deviceInfo" to bestRSSIDevice!!, "group" to "true", "switch" to currentDevice, "version" to version)
                else
                    startActivity<ConfigSceneSwitchActivity>("deviceInfo" to bestRSSIDevice!!, "group" to "true", "switch" to currentDevice, "version" to version)
                finish()
            }
            DeviceType.EIGHT_SWITCH -> {
                startActivity<ConfigEightSwitchActivity>("deviceInfo" to bestRSSIDevice!!, "group" to "true", "switch" to currentDevice, "version" to version)
                finish()
            }
            DeviceType.SMART_CURTAIN_SWITCH -> {
                startActivity<ConfigCurtainSwitchActivity>("deviceInfo" to bestRSSIDevice!!, "group" to "true", "switch" to currentDevice, "version" to version)
                finish()
            }
        }
    }

    private fun addDevice() {
        val lastUser = DBUtils.lastUser
        lastUser?.let {
            if (it.id.toString() != it.last_authorizer_user_id)
                ToastUtils.showLong(getString(R.string.author_region_warm))
            else {
                goSearchSwitch()
            }
        }
    }

    private fun initView() {
        mConnectDevice = TelinkLightApplication.getApp().connectDevice
        recycleView!!.layoutManager = GridLayoutManager(this, 2)
        recycleView!!.itemAnimator = DefaultItemAnimator()
        adapter = SwitchDeviceDetailsAdapter(R.layout.template_device_type_item, switchData, this)
        adapter!!.bindToRecyclerView(recycleView)

        adapter!!.onItemChildClickListener = onItemChildClickListener
        for (i in switchData.indices)
            switchData[i].updateIcon()

        install_device = findViewById(R.id.install_device)
        create_group = findViewById(R.id.create_group)
        create_scene = findViewById(R.id.create_scene)

        install_device?.setOnClickListener(onClick)
        create_group?.setOnClickListener(onClick)
        create_scene?.setOnClickListener(onClick)

        add_device_btn.setOnClickListener { addDevice() }
        toolbar.setNavigationIcon(R.drawable.icon_return)
        toolbar.setNavigationOnClickListener {
            finish()
        }
        toolbarTv.text = getString(R.string.switch_title)
    }

    var onItemChildClickListener = BaseQuickAdapter.OnItemChildClickListener { _, view, position ->
        currentDevice = switchData?.get(position)
        positionCurrent = position

        val lastUser = DBUtils.lastUser
        lastUser?.let { it ->
            if (it.id.toString() != it.last_authorizer_user_id)
                ToastUtils.showLong(getString(R.string.author_region_warm))
            else {
                when (view.id) {
                    R.id.template_device_card_delete -> {
                        val string = getString(R.string.sure_delete_device, currentDevice?.name)
                        builder?.setMessage(string)
                        builder?.create()?.show()
                    }
                    R.id.template_device_setting -> goConfig()
                    else -> {
                    }
                }
            }
        }
    }


    private fun isEmptyDevice() {
        if (switchData.size > 0) {
            no_device_relativeLayout.visibility = View.GONE
            recycleView.visibility = View.VISIBLE
        } else {
            no_device_relativeLayout.visibility = View.VISIBLE
            recycleView.visibility = View.GONE
        }
    }

    @SuppressLint("CheckResult")
    private fun getDeviceVersion(deviceInfo: DeviceInfo) {
        if (TelinkApplication.getInstance().connectDevice != null) {
            downloadDispoable = Commander.getDeviceVersion(deviceInfo.meshAddress)
                    .subscribe({ s ->
                        if (OtaPrepareUtils.instance().checkSupportOta(s)!!) {
                            currentDevice!!.version = s
                            DBUtils.saveSwitch(currentDevice!!, false)
                            isDirectConnectDevice()
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

        }
    }

    private fun isDirectConnectDevice() {
        var isBoolean: Boolean = SharedPreferencesHelper.getBoolean(TelinkLightApplication.getApp(), IS_DEVELOPER_MODE, false)
        if (TelinkLightApplication.getApp().connectDevice != null && TelinkLightApplication.getApp().connectDevice.macAddress == currentDevice?.macAddr) {
            if (isBoolean) {
                transformView()
            } else {
                OtaPrepareUtils.instance().gotoUpdateView(this@SwitchDeviceDetailsActivity, currentDevice?.version, otaPrepareListner)
            }
        } else {
            showLoadingDialog(getString(R.string.please_wait))
            TelinkLightService.Instance()?.idleMode(true)
            mConnectDeviceDisposable = Observable.timer(800, TimeUnit.MILLISECONDS)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .flatMap {
                        connect(currentDevice!!.meshAddr, macAddress = currentDevice!!.macAddr)
                    }
                    ?.subscribe(
                            {
                                onLogin(it)
                                hideLoadingDialog()
                                if (isBoolean) {
                                    transformView()
                                } else {
                                    OtaPrepareUtils.instance().gotoUpdateView(this@SwitchDeviceDetailsActivity, currentDevice!!.version, otaPrepareListner)
                                }
                            }
                            ,
                            {
                                hideLoadingDialog()
                                runOnUiThread { ToastUtils.showLong(R.string.connect_fail2) }
                                LogUtils.d(it)
                            })
        }
    }

    private fun transformView() {
        mConnectDeviceDisposable?.dispose()
        val intent = Intent(this@SwitchDeviceDetailsActivity, OTAUpdateActivity::class.java)
        intent.putExtra(OTA_MAC, currentDevice?.macAddr)
        intent.putExtra(OTA_MES_Add, currentDevice?.meshAddr)
        intent.putExtra(OTA_VERSION, currentDevice?.version)
        intent.putExtra(OTA_TYPE, DeviceType.NORMAL_SWITCH)
        val timeMillis = System.currentTimeMillis()
        if (last_start_time == 0 || timeMillis - last_start_time >= debounce_time)
            startActivity(intent)
    }

    private var otaPrepareListner: OtaPrepareListner = object : OtaPrepareListner {

        override fun downLoadFileStart() {}

        override fun startGetVersion() {}

        override fun getVersionSuccess(s: String) {}

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
        adapter!!.setNewData(switchData)
        initData()
        initView()
    }

    private fun getNewData(): MutableList<DbSwitch> {
        switchData = DBUtils.getAllSwitch()
        toolbarTv.text = (currentDevice!!.name ?: "")
        return switchData
    }

    override fun onDestroy() {
        super.onDestroy()
        downloadDispoable?.dispose()
        mConnectDeviceDisposable?.toString()
        acitivityIsAlive = false
        //移除事件
    }

    private val onClick = View.OnClickListener {
        when (it.id) {
            R.id.install_device -> {
                showInstallDeviceList()
            }
            R.id.create_group -> {
                dialog?.visibility = View.GONE
                if (TelinkLightApplication.getApp().connectDevice == null) {
                    ToastUtils.showLong(getString(R.string.device_not_connected))
                } else {
                    //addNewGroup()
                    popMain.showAtLocation(window.decorView, Gravity.CENTER, 0, 0)
                }
            }
            R.id.create_scene -> {
                dialog?.visibility = View.GONE
                val nowSize = DBUtils.sceneList.size
                if (TelinkLightApplication.getApp().connectDevice == null) {
                    ToastUtils.showLong(getString(R.string.device_not_connected))
                } else {
                    if (nowSize >= SCENE_MAX_COUNT) {
                        ToastUtils.showLong(R.string.scene_16_tip)
                    } else {
                        val intent = Intent(this, NewSceneSetAct::class.java)
                        intent.putExtra(Constant.IS_CHANGE_SCENE, false)
                        startActivity(intent)
                    }
                }
            }
        }
    }

    private fun showInstallDeviceList() {
        dialog.visibility = View.GONE
        showInstallDeviceList(isGuide, isRgbClick)
    }

    private fun goSearchSwitch() {
        installId = INSTALL_SWITCH
        showInstallDeviceDetail(StringUtils.getInstallDescribe(installId, this), installId, getString(R.string.Gate_way))
    }

    @SuppressLint("CheckResult")
    override fun tzRouterConnectSwSeRecevice(cmdBean: CmdBodyBean) {
        if (cmdBean.finish) {
            if (cmdBean.status == 0) {
                image_bluetooth.setImageResource(R.drawable.icon_cloud)
                ToastUtils.showShort(getString(R.string.connect_success))
                RouterModel.getDevicesVersion(mutableListOf(currentDevice!!.meshAddr), 99)?.subscribe({
                    when (it.errorCode) {
                        NetworkStatusCode.OK -> {
                            disposableTimer?.dispose()
                            disposableTimer = Observable.timer(it.t.timeout.toLong(), TimeUnit.MILLISECONDS)
                                    .subscribe {
                                        hideLoadingDialog()
                                        ToastUtils.showShort(getString(R.string.get_version_fail))
                                    }
                        }
                        NetworkStatusCode.DEVICE_NOT_BINDROUTER -> ToastUtils.showShort(getString(R.string.no_bind_router_cant_version))
                        NetworkStatusCode.ROUTER_ALL_OFFLINE -> ToastUtils.showShort(getString(R.string.router_offline))
                    }
                }, {
                    ToastUtils.showShort(it.message)
                })
            } else {
                image_bluetooth.setImageResource(R.drawable.bluetooth_no)
                ToastUtils.showShort(getString(R.string.connect_fail))
            }
        }
    }

    override fun tzRouterUpdateVersionRecevice(routerVersion: RouteGroupingOrDelOrGetVerBean?) {

        if (routerVersion?.finish == true) {
            if (routerVersion.cmd == 0) {
                disposableTimer?.dispose()
                val deviceInfo = DeviceInfo()
                deviceInfo.id = (currentDevice?.id ?: 0).toInt()
                deviceInfo.macAddress = currentDevice?.macAddr
                deviceInfo.meshAddress = currentDevice?.meshAddr ?: 0
                deviceInfo.firmwareRevision = currentDevice?.version
                deviceInfo.productUUID = currentDevice?.productUUID ?: 0
                SyncDataPutOrGetUtils.syncGetDataStart(DBUtils.lastUser!!, object : SyncCallback {
                    override fun start() {}

                    override fun complete() {
                        val switchByID = DBUtils.getSwitchByID(currentDevice!!.id)
                        if (switchByID?.version == "" || switchByID?.version == null)
                            ToastUtils.showShort(getString(R.string.get_version_fail))
                        else
                            skipeSw(deviceInfo, switchByID?.version ?: "B-01")
                    }

                    override fun error(msg: String?) {
                        ToastUtils.showShort(getString(R.string.get_version_fail))
                    }
                })
            } else {
                ToastUtils.showShort(getString(R.string.get_version_fail))
            }
        }
    }
}
