package com.dadoutek.uled.group

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.support.v7.widget.GridLayoutManager
import android.text.TextUtils
import android.view.View
import androidx.annotation.RequiresApi
import com.blankj.utilcode.util.LogUtils
import com.blankj.utilcode.util.ToastUtils
import com.chad.library.adapter.base.BaseQuickAdapter
import com.dadoutek.uled.R
import com.dadoutek.uled.base.TelinkBaseActivity
import com.dadoutek.uled.communicate.Commander
import com.dadoutek.uled.intf.OtaPrepareListner
import com.dadoutek.uled.model.Constant
import com.dadoutek.uled.model.DbModel.*
import com.dadoutek.uled.model.DeviceType
import com.dadoutek.uled.network.NetworkFactory
import com.dadoutek.uled.network.NetworkTransformer
import com.dadoutek.uled.ota.OTAUpdateActivity
import com.dadoutek.uled.tellink.TelinkLightApplication
import com.dadoutek.uled.tellink.TelinkLightService
import com.dadoutek.uled.util.OtaPrepareUtils
import com.dadoutek.uled.util.RecoverMeshDeviceUtil
import com.dadoutek.uled.widget.RecyclerGridDecoration
import com.polidea.rxandroidble2.scan.ScanSettings
import com.telink.bluetooth.light.DeviceInfo
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_group_ota_list.*
import kotlinx.android.synthetic.main.template_recycleview.*
import kotlinx.android.synthetic.main.toolbar.toolbar
import kotlinx.android.synthetic.main.toolbar.toolbarTv
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern


/**
 * 创建者     ZCL
 * 创建时间   2020/6/10 11:25
 * 描述
 * 更新者     $
 * 更新时间   $
 * 更新描述
 */
class GroupOTAListActivity : TelinkBaseActivity() {
    private var deviceType: Int = 0
    private var isGroup: Boolean = false
    private val compositeDisposable: CompositeDisposable = CompositeDisposable()
    private var disposableScan: Disposable? = null
    private var disposableTimerResfresh: Disposable? = null
    private var dispose: Disposable? = null
    private var dbGroup: DbGroup? = null
    private var mConnectDisposable: Disposable? = null
    private var lightList: MutableList<DbLight> = mutableListOf()
    private var curtainList: MutableList<DbCurtain> = mutableListOf()
    private var relayList: MutableList<DbConnector> = mutableListOf()
    private var mapBin = mutableMapOf<String, Int>()
    private var lightAdaper = GroupOTALightAdapter(R.layout.group_ota_item, lightList)
    private var curtainAdaper = GroupOTACurtainAdapter(R.layout.group_ota_item, curtainList)
    private var relayAdaper = GroupOTARelayAdapter(R.layout.group_ota_item, relayList)

    @RequiresApi(Build.VERSION_CODES.N)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_group_ota_list)
        initView()
        initData()
        initListener()
    }

    private fun initListener() {
        loading_tansform.setOnClickListener { }
        ota_swipe_refresh_ly.setOnRefreshListener {
            loading_tansform.visibility = View.VISIBLE
            findMeshDevice(DBUtils.lastUser?.controlMeshName)
            disposableTimerResfresh?.dispose()
            disposableTimerResfresh = Observable.timer(4000, TimeUnit.MILLISECONDS)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe {
                        loading_tansform.visibility = View.GONE
                        ota_swipe_refresh_ly.isRefreshing = false

                        disposableScan?.dispose()
                    }
            compositeDisposable.add(disposableTimerResfresh!!)
        }
    }

    @SuppressLint("CheckResult")
    fun findMeshDevice(deviceName: String?) {
        val scanFilter = com.polidea.rxandroidble2.scan.ScanFilter.Builder().setDeviceName(deviceName).build()
        val scanSettings = ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                .build()

        LogUtils.d("findMeshDevice name = $deviceName")
        disposableScan?.dispose()
        disposableScan = RecoverMeshDeviceUtil.rxBleClient.scanBleDevices(scanSettings, scanFilter)
                .observeOn(Schedulers.io())
                .map { RecoverMeshDeviceUtil.parseData(it) }          //解析数据
                .timeout(RecoverMeshDeviceUtil.SCAN_TIMEOUT_SECONDS, TimeUnit.SECONDS) {
                    LogUtils.d("findMeshDevice name complete.")
                    when (dbGroup!!.deviceType.toInt()) {
                        DeviceType.LIGHT_NORMAL, DeviceType.LIGHT_RGB -> {
                            lightList.sortBy { it1 -> it1.rssi }
                            supportAndUN()
                            lightAdaper.notifyDataSetChanged()

                        }
                        DeviceType.SMART_CURTAIN -> {
                            curtainList.sortBy { it1 -> it1.rssi }
                            curtainAdaper.notifyDataSetChanged()
                        }
                        DeviceType.SMART_RELAY -> {
                            relayList.sortBy { it1 -> it1.rssi }
                            relayAdaper.notifyDataSetChanged()
                        }
                    }
                    it.onComplete()                     //如果过了指定时间，还搜不到缺少的设备，就完成
                }
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    if (it != null)
                        refreshRssi(it)
                }, {})
        compositeDisposable?.add(disposableScan!!)

    }

    private fun refreshRssi(deviceInfo: DeviceInfo) {
        LogUtils.v("zcl信号$deviceInfo")
        GlobalScope.launch(Dispatchers.Main) {
            if (deviceInfo.productUUID == dbGroup!!.deviceType.toInt()) {
                var deviceChangeL: DbLight? = null
                var deviceChangeC: DbCurtain? = null
                var deviceChangeR: DbConnector? = null
                when (dbGroup!!.deviceType.toInt()) {
                    DeviceType.LIGHT_NORMAL, DeviceType.LIGHT_RGB -> {
                        for (device in lightList) {
                            if (device.meshAddr == deviceInfo.meshAddress) {
                                device.rssi = deviceInfo.rssi
                                deviceChangeL = device
                                LogUtils.v("zcl设备信号$deviceInfo----------------$deviceChangeL")
                            }
                        }
                        if (null != deviceChangeL) {
                            lightList.remove(deviceChangeL)
                            lightList.add(deviceChangeL)
                        }
                    }
                    DeviceType.SMART_CURTAIN -> {
                        for (device in curtainList) {
                            if (device.macAddr == deviceInfo.macAddress) {
                                device.rssi = deviceInfo.rssi
                                deviceChangeC = device
                                LogUtils.v("zcl设备信号$deviceInfo----------------$deviceChangeC")
                            }
                        }
                        if (null != deviceChangeC) {
                            curtainList.remove(deviceChangeC)
                            curtainList.add(deviceChangeC)
                        }
                    }
                    DeviceType.SMART_RELAY -> {
                        for (device in relayList) {
                            if (device.macAddr == deviceInfo.macAddress) {
                                device.rssi = deviceInfo.rssi
                                deviceChangeR = device
                                LogUtils.v("zcl设备信号$deviceInfo----------------$deviceChangeR")
                            }
                        }
                        if (null != deviceChangeL) {
                            relayList.remove(deviceChangeR)
                            relayList.add(deviceChangeR!!)
                        }
                    }
                }

            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.N)
    private fun initData() {
        val serializableExtra = intent.getSerializableExtra("group")
         if(serializableExtra!=null)
             dbGroup =  serializableExtra as DbGroup

        deviceType = intent.getIntExtra("DeviceType", 0)
        isGroup = dbGroup != null

        when (deviceType) {
            DeviceType.LIGHT_NORMAL, DeviceType.LIGHT_RGB -> {
                template_recycleView.adapter = lightAdaper
                lightAdaper.bindToRecyclerView(template_recycleView)
            }
            DeviceType.SMART_CURTAIN -> {
                template_recycleView.adapter = curtainAdaper
                curtainAdaper.bindToRecyclerView(template_recycleView)
            }
            DeviceType.SMART_RELAY -> {
                template_recycleView.adapter = relayAdaper
                relayAdaper.bindToRecyclerView(template_recycleView)
            }
        }

        getBin()

    }

    private fun setRelayData() {
        relayList.clear()
        if (isGroup)
            relayList.addAll(DBUtils.getConnectorByGroupID(dbGroup!!.id))
        else
            relayList.addAll(DBUtils.allRely)

        relayAdaper.onItemClickListener = onItemClickListener
        relayList.forEach {
            it.version?.let { itv ->
                it.isSupportOta = OtaPrepareUtils.instance().checkSupportOta(itv)
                val split = itv.split("-")
                var versionNum = getVersionNum(itv)
                if (split.size >= 2) {
                    LogUtils.v("zcl比较版本号-------$itv------${mapBin[split[0]] ?: 0}-----${versionNum.toString().toInt()}")
                    if (!TextUtils.isEmpty(versionNum)) {
                        it.isMostNew = versionNum.toString().toInt() >= mapBin[split[0]] ?: 0
                        it.isSupportOta = versionNum.toString().toInt() < mapBin[split[0]] ?: 0
                    }

                }
            }
        }

        var unsupport = relayList.filter {
            !it.isSupportOta
        }
        relayList.removeAll(unsupport)
        relayList.addAll(unsupport)
        relayAdaper.notifyDataSetChanged()
    }

    private fun setCurtainData() {
        curtainList.clear()
        if (isGroup)
            curtainList.addAll(DBUtils.getCurtainByGroupID(dbGroup!!.id))
        else
            curtainList.addAll(DBUtils.allCurtain)

        curtainAdaper.onItemClickListener = onItemClickListener
        curtainList.forEach {
            it.version?.let { itv ->
                it.isSupportOta = OtaPrepareUtils.instance().checkSupportOta(itv)
                val split = itv.split("-")
                var versionNum = getVersionNum(itv)
                if (split.size >= 2) {
                    LogUtils.v("zcl比较版本号-------$itv------${mapBin[split[0]] ?: 0}-----${versionNum.toString().toInt()}")
                    if (!TextUtils.isEmpty(versionNum)) {
                        it.isMostNew = versionNum.toString().toInt() >= mapBin[split[0]] ?: 0
                        it.isSupportOta = versionNum.toString().toInt() < mapBin[split[0]] ?: 0
                    }
                }
            }
        }

        var unsupport = curtainList.filter {
            !it.isSupportOta
        }
        curtainList.removeAll(unsupport)
        curtainList.addAll(unsupport)
        curtainAdaper.notifyDataSetChanged()
    }

    private fun setLightData() {
        lightList.clear()
        if (isGroup)
            lightList.addAll(DBUtils.getLightByGroupID(dbGroup!!.id))
        else
            lightList.addAll(DBUtils.allLight)
        lightAdaper.onItemClickListener = onItemClickListener
        supportAndUN()
        lightAdaper.notifyDataSetChanged()
    }

    private fun supportAndUN() {
        lightList.forEach {
            it.version?.let { itv ->
                it.isSupportOta = OtaPrepareUtils.instance().checkSupportOta(itv)
                val split = itv.split("-")
                var versionNum = getVersionNum(itv)
                if (split.size >= 2) {
                    LogUtils.v("zcl比较版本号-------$itv------${mapBin[split[0]] ?: 0}-----${versionNum.toString().toInt()}")
                    if (!TextUtils.isEmpty(versionNum)) {
                        it.isMostNew = versionNum.toString().toInt() >= mapBin[split[0]] ?: 0
                        it.isSupportOta = versionNum.toString().toInt() < mapBin[split[0]] ?: 0
                    }
                }
            }
        }

        var unsupport = lightList.filter {
            !it.isSupportOta
        }
        lightList.removeAll(unsupport)
        lightList.addAll(unsupport)
    }

    private fun getVersionNum(it: String): StringBuilder {
        var versionNum = StringBuilder()
        val p = Pattern.compile("\\d+")
        val m = p.matcher(it)
        if (m != null)
            while (m.find()) {
                versionNum.append(m.group())
            }
        return versionNum
    }

    private fun initView() {
        toolbar.setNavigationIcon(R.drawable.icon_return)
        toolbar.setNavigationOnClickListener { finish() }
        toolbarTv.text = getString(R.string.group_ota)
        template_recycleView.layoutManager = GridLayoutManager(this, 2)/*LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)*/
        template_recycleView.addItemDecoration(RecyclerGridDecoration(this, 2))
        getBin()

        //设置进度View下拉的起始点和结束点，scale 是指设置是否需要放大或者缩小动画
        ota_swipe_refresh_ly.setProgressViewOffset(true, -0, 500)
        //设置进度View下拉的结束点，scale 是指设置是否需要放大或者缩小动画
        ota_swipe_refresh_ly.setProgressViewEndTarget(true, 360)
        //设置进度View的组合颜色，在手指上下滑时使用第一个颜色，在刷新中，会一个个颜色进行切换
        ota_swipe_refresh_ly.setColorSchemeColors(Color.BLACK, Color.GREEN, Color.RED, Color.YELLOW, Color.BLUE)
        //设置触发刷新的距离
        ota_swipe_refresh_ly.setDistanceToTriggerSync(200)
    }

    private fun getBin() {
        dispose = NetworkFactory.getApi().binList
                .compose(NetworkTransformer())
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread()).subscribe({
                    LogUtils.v("zcl获取服务器bin-----------$it-------")
                    mapBin = it
                    updataDevice()

                }, {
                    ToastUtils.showShort(getString(R.string.get_bin_fail))
                    finish()
                })
    }

    private fun updataDevice() {
        when (deviceType.toInt()) {
            DeviceType.LIGHT_NORMAL, DeviceType.LIGHT_RGB -> setLightData()
            DeviceType.SMART_CURTAIN -> setCurtainData()
            DeviceType.SMART_RELAY -> setRelayData()
        }
    }

    val onItemClickListener = BaseQuickAdapter.OnItemClickListener { _, _, position ->
        when (deviceType.toInt()) {
            DeviceType.LIGHT_NORMAL, DeviceType.LIGHT_RGB -> {
                val dbLight = lightList[position]
                if (dbLight.isSupportOta)
                    if (TextUtils.isEmpty(dbLight.version)) {
                        showLoadingDialog(getString(R.string.please_wait))

                        if (TelinkLightApplication.getApp().connectDevice != null && TelinkLightApplication.getApp().connectDevice.meshAddress == dbLight.meshAddr) {
                            getDeviceVersion(dbLight)
                        } else {
                            showLoadingDialog(getString(R.string.please_wait))
                            val idleMode = TelinkLightService.Instance()?.idleMode(true)
                            mConnectDisposable = Observable.timer(800, TimeUnit.MILLISECONDS)
                                    .subscribeOn(Schedulers.io())
                                    .observeOn(AndroidSchedulers.mainThread())
                                    .flatMap {
                                        connect(dbLight.meshAddr, true)
                                    }?.subscribe({
                                        hideLoadingDialog()
                                        getDeviceVersion(dbLight)
                                    }
                                            , {
                                        hideLoadingDialog()
                                        runOnUiThread { ToastUtils.showLong(R.string.connect_fail2) }
                                        LogUtils.d(it)
                                    })
                        }
                        connect(meshAddress = dbLight.meshAddr, connectTimeOutTime = 15)
                                ?.subscribeOn(Schedulers.io())
                                ?.observeOn(AndroidSchedulers.mainThread())
                                ?.subscribe({

                                }, {
                                    hideLoadingDialog()
                                    ToastUtils.showShort(getString(R.string.connect_fail))
                                })
                    } else
                        getFilePath(dbLight.meshAddr, dbLight.macAddr, dbLight.version, DeviceType.LIGHT_NORMAL)
                else {
                    if (dbLight.isMostNew)
                        ToastUtils.showShort(getString(R.string.the_last_version))
                    else
                        ToastUtils.showShort(getString(R.string.dissupport_ota))
                }
            }
            DeviceType.SMART_CURTAIN -> {
            }
            DeviceType.SMART_RELAY -> {
            }
        }
    }

    override fun onResume() {
        super.onResume()

        updataDevice()
    }

    private fun getDeviceVersion(dbLight: DbLight) {
        val dispos = Commander.getDeviceVersion(dbLight.meshAddr).subscribe(
                { s: String ->
                    dbLight!!.version = s
                    DBUtils.saveLight(dbLight!!, true)
                    setLightData()
                    hideLoadingDialog()
                    ToastUtils.showShort(getString(R.string.get_version_success))
                }, {
            hideLoadingDialog()
            ToastUtils.showLong(getString(R.string.get_version_fail))
        })
    }

    private fun getFilePath(meshAddr: Int, macAddr: String, version: String, deviceType: Int) {
        OtaPrepareUtils.instance().gotoUpdateView(this@GroupOTAListActivity, version, object : OtaPrepareListner {

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
                if (TelinkLightApplication.getApp().connectDevice != null && TelinkLightApplication.getApp().connectDevice.meshAddress == meshAddr) {
                    startOtaAct(meshAddr, macAddr, version, deviceType)
                } else {
                    showLoadingDialog(getString(R.string.please_wait))
                    val idleMode = TelinkLightService.Instance()?.idleMode(true)
                    mConnectDisposable = Observable.timer(800, TimeUnit.MILLISECONDS)
                            .subscribeOn(Schedulers.io())
                            .observeOn(AndroidSchedulers.mainThread())
                            .flatMap {
                                connect(meshAddr, true)
                            }?.subscribe({
                                hideLoadingDialog()
                                startOtaAct(meshAddr, macAddr, version, deviceType)
                            }
                                    , {
                                hideLoadingDialog()
                                runOnUiThread { ToastUtils.showLong(R.string.connect_fail2) }
                                LogUtils.d(it)
                            })
                }

            }

            override fun downLoadFileFail(message: String) {
                hideLoadingDialog()
                ToastUtils.showLong(R.string.download_pack_fail)
            }
        })
    }

    private fun startOtaAct(meshAddr: Int, macAddr: String, version: String, deviceType: Int) {
        val intent = Intent(this@GroupOTAListActivity, OTAUpdateActivity::class.java)
        intent.putExtra(Constant.OTA_MES_Add, meshAddr)
        intent.putExtra(Constant.OTA_MAC, macAddr)
        intent.putExtra(Constant.OTA_VERSION, version)
        intent.putExtra(Constant.OTA_TYPE, deviceType)
        startActivity(intent)
    }

    override fun onDestroy() {
        super.onDestroy()
        dispose?.dispose()
    }
}