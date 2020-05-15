package com.dadoutek.uled.light

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.view.View
import com.blankj.utilcode.util.ActivityUtils
import com.blankj.utilcode.util.LogUtils
import com.blankj.utilcode.util.ToastUtils
import com.dadoutek.uled.R
import com.dadoutek.uled.othersview.MainActivity
import com.dadoutek.uledtest.ble.RxBleManager
import com.polidea.rxandroidble2.RxBleDevice
import com.polidea.rxandroidble2.scan.ScanResult
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_physical_recovery.*
import kotlinx.android.synthetic.main.toolbar.*
import org.jetbrains.anko.textColor
import java.util.concurrent.TimeUnit
import android.text.style.ForegroundColorSpan as ForegroundColorSpan1


/**
 * 创建者     ZCL
 * 创建时间   2019/12/5 18:19
 * 描述 物理恢复
 *
 * 更新者     $
 * 更新时间   $
 * 更新描述
 */

class PhysicalRecoveryActivity : AppCompatActivity() {
    private var disposableConnectWaitTimer: Disposable? = null
    private val scanMinTime: Long = 10000
    private val scanMaxTime: Long = 25000
    private var macAddress: String? = null
    private var disposableScan: Disposable? = null
    private var disposableWrite: Disposable? = null
    private var disposableFiveTimer: Disposable? = null
    private var disposableScanTimer: Disposable? = null
    private var disposableConnectTimer: Disposable? = null
    private var disposableShowResultDelay: Disposable? = null
    private var disposableConnectOffTimer: Disposable? = null
    private val fiveDownTime: Long = 5L
    private val powerOffTimer: Long = 30
    private val connectTimeOut: Long = 30
    private var isConnection = false
    private var countConnection = 0
    private var disposableConnect: Disposable? = null
    private var allStateTag: Int = 0   // 1代表扫描连接中  2连接成功 3失败 4

    private var tenTag: Int = 5   // 1代表连接中  2连接成功 3失败 4 五秒倒计时完成    5-10秒断联
    private var seachering: Int = 7   // 扫描中
    private var maxCount: Int = 3   // 成功次数

    private var isHaveNew: Boolean = false
    private var isHaveOld: Boolean = false
    private val compositeDisposable: CompositeDisposable = CompositeDisposable()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_physical_recovery)
        RxBleManager.initRxjavaErrorHandle()//解决闪退
        RxBleManager.initData()
        initView()
        initData()
        initListener()
    }

    @SuppressLint("CheckResult")
    private fun startScanBestRssi(time: Long = scanMinTime) {
        if (!RxBleManager.isScanning()) {
            RxBleManager.disconnectAllDevice()
            val list = arrayListOf<ScanResult>()
            allStateTag = seachering
            if (time != scanMinTime)
                setAllDispose()
            disposableScanTimer?.dispose()
            disposableScanTimer = Observable.timer(time, TimeUnit.MILLISECONDS)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe {
                        connectBestRssi(list)
                    }
            disposableScan?.dispose()
            disposableScan = RxBleManager.scan()
                    ?.subscribe({
                        LogUtils.v("zcl------------物理恢添加信息$it")
                        list.add(it)
                        if (macAddress != null && it.bleDevice.macAddress == macAddress)//如果搜索到已连接的设备则停止扫描马上链接 否则走下面定时
                            connectBestRssi(list)
                        LogUtils.e("zcl物理恢复----------------------${it.bleDevice.macAddress == macAddress}---------${it.bleDevice.macAddress}---------$macAddress")
                    }, {
                        LogUtils.v("zcl------------物理恢复扫描错误信息$it-------------------连接状态$allStateTag")
                        disposableFiveTimer?.dispose()
                        disposableConnectOffTimer?.dispose()
                        disposableConnectTimer?.dispose()
                        disposableScanTimer?.dispose()
                        setAllDispose()
                        changeVisiable(View.GONE, View.GONE, View.GONE, View.VISIBLE, View.GONE)
                    })

        } else {
            LogUtils.v("zcl物理流程正在扫描不能新扫描")
        }
    }

    @SuppressLint("CheckResult")
    private fun connectBestRssi(list: ArrayList<ScanResult>) {
        disposableScan?.dispose()
        disposableScanTimer?.dispose()
        var scanResult: ScanResult? = getBestRssi(list)
        if (isHaveNew && scanResult != null) {
            if (scanResult.bleDevice != null) {
                changeVisiable(View.GONE, View.VISIBLE, View.GONE, View.GONE, View.GONE)
                if (macAddress == null)
                    setConnectText()
                disposableConnectWaitTimer?.dispose()
                disposableConnectWaitTimer = Observable.timer(700, TimeUnit.MILLISECONDS)//
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe {
                            connectAndWriteBle(scanResult.bleDevice)
                        }
            } else {
                changeVisiable(View.GONE, View.GONE, View.GONE, View.VISIBLE, View.GONE)
            }
        } else if (isHaveOld) {//是我们的旧设备并且是没有恢复出厂的
            //显示旧设备恢复出厂的提示语
            changeVisiable(View.GONE, View.GONE, View.GONE, View.GONE, View.VISIBLE)
        } else {
            //scanResult==null
            ToastUtils.showLong(getString(R.string.no_found_recovery_device))
            finish()
        }

    }

    private fun getBestRssi(list: ArrayList<ScanResult>): ScanResult? {
        var scanResult: ScanResult? = null
        val listNew = mutableListOf<ScanResult>()
        val listOld = mutableListOf<ScanResult>()

        for (it in list) {
            val version = RxBleManager.getVersion(it)
            val supportHybridFactoryReset = RxBleManager.isSupportHybridFactoryReset(version)
            if (supportHybridFactoryReset) {
                listNew.add(it)
                LogUtils.v("zcl添加新设备$it")
            } else {
                listOld.add(it)
                LogUtils.v("zcl添加旧设备$it")
            }
        }

        isHaveNew = listNew.size > 0
        isHaveOld = listOld.size > 0

        scanResult = if (listNew.size > 0) {
            filterRssi(listNew, scanResult)
        } else {
            null
        }
        return scanResult
    }

    private fun filterRssi(listNew: MutableList<ScanResult>, scanResult: ScanResult?): ScanResult? {
        var scanResult1 = scanResult
        for (it in listNew) {
            if (macAddress == null) {
                if (scanResult1 == null)
                    scanResult1 = it
                else {
                    if (it.rssi > scanResult1.rssi)
                        scanResult1 = it
                }
            } else {
                if (macAddress == it.bleDevice.macAddress)
                    scanResult1 = it
            }
        }
        return scanResult1
    }

    private fun setConnectText() {
        physical_recovery_state_warm.text = getString(R.string.connecting)
        physical_recovery_state_warm.textColor = getColor(R.color.blue_text)
    }


    @SuppressLint("CheckResult")
    private fun connectAndWriteBle(bleDevice: RxBleDevice) {
        RxBleManager.disconnectAllDevice()

        disposableConnect?.dispose()
        disposableConnect = RxBleManager.connect(bleDevice!!, true)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    macAddress = bleDevice.macAddress
                    image_bluetooth.setImageResource(R.drawable.icon_bluetooth)
                    physical_recovery_state_progress.visibility = View.GONE
                    writeDataAndShowView(bleDevice)
                }, {
                    image_bluetooth.setImageResource(R.drawable.bluetooth_no)
                    if (!isConnection && countConnection < maxCount) {// 不是恢复出厂成功后的断开
                        physical_recovery_state_progress.visibility = View.GONE
                        changeVisiable(View.GONE, View.GONE, View.GONE, View.VISIBLE, View.GONE)
                    }
                    disposableConnect?.dispose()

                    isConnection = false
                    LogUtils.v("zcl------物理流程状态监听恢复断开连接$it")
                })
    }

    private fun writeDataAndShowView(bleDevice: RxBleDevice) {
        disposableWrite?.dispose()
        disposableWrite = RxBleManager.writeData(bleDevice, byteArrayOf(0xf9.toByte()))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    countConnection++
                    isConnection = true

                    LogUtils.e("zcl物理写入次数$countConnection")
                    if (countConnection < maxCount) {
                        changeVisiable(View.GONE, View.VISIBLE, View.GONE, View.GONE, View.GONE)
                        startOffTimer()
                    } else {
                        LogUtils.v("zcl物理写入流程恢复成功$countConnection")
                        disposableShowResultDelay?.dispose()
                        disposableShowResultDelay = Observable.timer(3000, TimeUnit.MILLISECONDS)
                                .subscribeOn(Schedulers.io())
                                .observeOn(AndroidSchedulers.mainThread())
                                .subscribe({
                                    disposableConnectTimer?.dispose()//取消30秒倒计时
                                    changeVisiable(View.GONE, View.GONE, View.VISIBLE, View.GONE, View.GONE)
                                }, {})

                    }
                }, {
                    LogUtils.v("zcl物理写入数据错误$it")
                    RxBleManager.disconnectAllDevice()
                    changeVisiable(View.GONE, View.GONE, View.GONE, View.VISIBLE, View.GONE)
                    setAllDispose()
                })
    }

    /**
     * 断电倒计时30秒
     */
    @SuppressLint("StringFormatMatches")
    private fun startOffTimer() {//连接状态下断电倒计时
        disposableConnectTimer?.dispose()
        disposableConnectOffTimer?.dispose()

        disposableConnectOffTimer = Observable.intervalRange(0, powerOffTimer, 0, 1, TimeUnit.SECONDS)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe {
                    var num = powerOffTimer - it - 1
                    physical_recovery_state_warm.textColor = getColor(R.color.gray_3)
                    changeOffColor(num)
                    if (powerOffTimer == it)
                        allStateTag = tenTag//代表10秒已经结束

                    LogUtils.e("zcl-----------------------------isConnectio====$isConnection-------isNeedRetry===${RxBleManager.isNeedRetry}------------------$it")

                    if (!isConnection) {//如果已经断联是正常的走流程 false
                        LogUtils.e("zcl物理流程状态断联了嘛__________还在连接?________$isConnection")
                        when (countConnection) {//第一第二次流程开启通电倒计时 5秒后通电
                            1, 2 -> {
                                fiveOnTimer()
                            }
                        }
                    } else {//断联倒计时到了还没断联 升级失败
                        if (0L == num)
                            changeVisiable(View.GONE, View.GONE, View.GONE, View.VISIBLE, View.GONE)
                    }
                }
    }

    /**
     * 连接倒计时30秒
     */
    private fun thrityOnTimerSearchAndConnect() {
        disposableFiveTimer?.dispose()
        disposableConnectTimer?.dispose()
        disposableConnectTimer = Observable.intervalRange(0, connectTimeOut, 0, 1, TimeUnit.SECONDS)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe {
                    var num = connectTimeOut - 1 - it
                    physical_recovery_state_warm.textColor = getColor(R.color.gray_3)
                    if (it == 1L)//等于5秒时候开始连接并通电 重新扫描时为了让连接的那个设备发广播提高连接成功率
                        startScanBestRssi(scanMaxTime)

                    changePowerOnColor(num)

                    if (!isConnection && 0L == num) {//通电倒计时内没有连接上 恢复失败
                        changeVisiable(View.GONE, View.GONE, View.GONE, View.VISIBLE, View.GONE)
                        disposableConnectTimer?.dispose()
                    }
                }
    }

    @SuppressLint("StringFormatMatches")
    private fun changeOffColor(num: Long) {
        val style = SpannableStringBuilder(getString(R.string.frist_out_age, num))//连接成功,请在%1$sS内 10断电
        if (isZh(this))
            if (num >= 10)
                style.setSpan(ForegroundColorSpan1(getColor(R.color.blue_text)), 7, 11, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            else
                style.setSpan(ForegroundColorSpan1(getColor(R.color.blue_text)), 7, 10, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        else//Please power on after %1$s
            if (num >= 10)
                style.setSpan(ForegroundColorSpan1(getColor(R.color.blue_text)), style.length - 3, style.length - 1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            else
                style.setSpan(ForegroundColorSpan1(getColor(R.color.blue_text)), style.length - 2, style.length - 1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        physical_recovery_state_warm.text = style
    }

    /**
     * 通电提醒倒计时
     */
    private fun fiveOnTimer() {
        disposableConnectOffTimer?.dispose()
        disposableFiveTimer?.dispose()
        disposableFiveTimer = Observable.intervalRange(0, fiveDownTime, 0, 1, TimeUnit.SECONDS)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe {
                    var number = fiveDownTime - it - 1
                    if (number == 0L)
                        thrityOnTimerSearchAndConnect()

                    physical_recovery_state_warm.textColor = getColor(R.color.gray_3)
                    changePowerDownTimerColor(number)
                }
    }

    @SuppressLint("StringFormatMatches")
    private fun changePowerOnColor(num: Long) {
        val style = SpannableStringBuilder(getString(R.string.second_power_on, num))//请在30秒内通电,等待连接
        if (isZh(this))
            if (num >= 10)
                style.setSpan(ForegroundColorSpan1(getColor(R.color.blue_text)), 2, 6, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            else
                style.setSpan(ForegroundColorSpan1(getColor(R.color.blue_text)), 2, 5, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        else {
            if (num >= 10)//Please power on within %1$s seconds,waiting for the connection
                style.setSpan(ForegroundColorSpan1(getColor(R.color.blue_text)), 14, 32, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            else
                style.setSpan(ForegroundColorSpan1(getColor(R.color.blue_text)), 14, 31, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
        physical_recovery_state_warm.text = style
    }

    @SuppressLint("StringFormatMatches")
    private fun changePowerDownTimerColor(number: Long) {
        val style = SpannableStringBuilder(getString(R.string.frist_power_on, number))//请在0-1 5秒后通电
        if (isZh(this))
            style.setSpan(ForegroundColorSpan1(getColor(R.color.blue_text)), 2, 5, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        else//Please power on after %1$s
            style.setSpan(ForegroundColorSpan1(getColor(R.color.blue_text)), style.length - 2, style.length - 1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        physical_recovery_state_warm.text = style
    }

    private fun initListener() {
        physical_recovery_ready_ok.setOnClickListener {
            changeVisiable(View.GONE, View.VISIBLE, View.GONE, View.GONE, View.GONE)
            setSearcher()
            startScanBestRssi()
        }
        physical_recovery_retry.setOnClickListener {
            countConnection = 0
            physical_recovery_state_progress.visibility = View.VISIBLE
            setSearcher()
            changeVisiable(View.GONE, View.VISIBLE, View.GONE, View.GONE, View.GONE)
            startScanBestRssi()
        }
        physical_recovery_success.setOnClickListener {
            finish()
            ActivityUtils.startActivity(MainActivity::class.java)
        }
    }

    private fun initData() {
        changeVisiable(View.VISIBLE, View.GONE, View.GONE, View.GONE, View.GONE)
    }

    private fun setSearcher() {
        physical_recovery_state_warm.textColor = getColor(R.color.blue_text)
        physical_recovery_state_warm.text = getString(R.string.searching)
    }

    @SuppressLint("CheckResult")
    private fun initView() {
        toolbar.setNavigationIcon(R.drawable.navigation_back_white)
        toolbar.setNavigationOnClickListener {
            finish()
        }
        toolbarTv.text = getString(com.dadoutek.uled.R.string.physical_recovery)

        val style = SpannableStringBuilder(getString(R.string.reset_factory_fail))//恢复出厂设置失败,请重新上电重试
        if (isZh(this))
            style.setSpan(ForegroundColorSpan1(getColor(R.color.red)), 9, 14, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        else//Factory Settings restored fail,please power-on to try again
            style.setSpan(ForegroundColorSpan1(getColor(R.color.red)), style.length - 22, style.length - 1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        physical_recovery_text_fail.text = style
    }

    private fun changeVisiable(ready: Int, state: Int, success: Int, fail: Int, old: Int) {
        physical_recovery_ready_ly.visibility = ready
        physical_recovery_rly.visibility = state
        physical_recovery_success_ly.visibility = success

        physical_recovery_fail_ly.visibility = fail
        physical_recovery_old_ly.visibility = old
        if (fail == View.VISIBLE)
            setAllDispose()
    }

    override fun onDestroy() {
        super.onDestroy()
        RxBleManager.disconnectAllDevice()
        setAllDispose()
    }

    private fun setAllDispose() {
        disposableScan?.dispose()
        disposableConnect?.dispose()
        disposableScanTimer?.dispose()
        disposableShowResultDelay?.dispose()
        disposableConnectOffTimer?.dispose()
        disposableConnectWaitTimer?.dispose()
    }

    private fun isZh(context: Context): Boolean {
        val locale = context.resources.configuration.locale
        val language = locale.language
        return language.endsWith("zh")
    }
}