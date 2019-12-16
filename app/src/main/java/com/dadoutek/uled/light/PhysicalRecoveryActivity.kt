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
    private var macAddress: String? = null
    private var disposableShowResultDelay: Disposable? = null
    private val powerDownTime: Long = 3L
    private var disposableConnectOnTimer: Disposable? = null
    private val outAgeTime: Long = 10
    private val outPoweOnTime: Long = 30
    private var disposableConnectOffTimer: Disposable? = null
    private var disposableScanTimer: Disposable? = null
    private var disposableScan: Disposable? = null
    private var isConnection = false
    private var countConnection = 0
    private var disposableConnect: Disposable? = null
    private var physicalSuccessTag: Boolean = false
    private val compositeDisposable: CompositeDisposable = CompositeDisposable()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_physical_recovery)
        RxBleManager.initRxjavaErrorHandle()//解决闪退
        initView()
        initData()
        initListener()
    }

    @SuppressLint("CheckResult")
    private fun startScanBestRssi(isFrist: Boolean = false) {
        if (!RxBleManager.isScanning()) {
            RxBleManager.disconnectAllDevice()
            val list = arrayListOf<ScanResult>()
            disposableScan = RxBleManager.scan()
                    .subscribe({
                        LogUtils.v("zcl------------物理恢添加信息$it")
                        physicalSuccessTag = false
                        list.add(it)
                    }, {
                        LogUtils.v("zcl------------物理恢复扫描错误信息$it-------------------连接状态$isConnection")
                        disposableScan?.dispose()
                       //startScanBestRssi(false)
                    })
            LogUtils.v("zcl是否正在扫描${RxBleManager.isScanning()}")
            disposableScanTimer = Observable.timer(10000, TimeUnit.MILLISECONDS)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe {

                        connectBestRssi(list, isFrist)
                        disposableScan?.dispose()
                    }
        } else {
            LogUtils.v("zcl物理流程正在扫描不能新扫描")
        }
    }

    private fun connectBestRssi(list: ArrayList<ScanResult>, frist: Boolean) {
        var scanResult: ScanResult? = getBestRssi(list, frist)

        if (scanResult != null && scanResult.bleDevice != null) {
            changeVisiable(View.VISIBLE, View.GONE, View.GONE)
            if (frist)
                setConnectText()

                macAddress = scanResult.bleDevice?.macAddress
                connectAndWriteBle(scanResult.bleDevice)

        } else {
            changeVisiable(View.GONE, View.GONE, View.VISIBLE)
        }
    }

    private fun getBestRssi(list: ArrayList<ScanResult>, frist: Boolean): ScanResult? {
        var scanResult: ScanResult? = null
        for (it in list) {
            if (frist) {
                if (scanResult == null)
                    scanResult = it
                else {
                    if (it.rssi > scanResult.rssi)
                        scanResult = it
                }
            } else {
                if (macAddress == it.bleDevice.macAddress)
                    scanResult = it
            }
        }
        return scanResult
    }

    private fun setConnectText() {
        physical_recovery_state_warm.text = getString(R.string.connecting)
        physical_recovery_state_warm.textColor = getColor(R.color.blue_text)
    }


    @SuppressLint("CheckResult")
    private fun connectAndWriteBle(bleDevice: RxBleDevice) {
        countConnection++
        RxBleManager.disconnectAllDevice()
        disposableConnect = RxBleManager.connect(bleDevice!!, true)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    LogUtils.v("zcl------物理流程状态监连接")
                    image_bluetooth.setImageResource(R.drawable.icon_bluetooth)
                    isConnection = true
                    physical_recovery_state_progress.visibility = View.GONE
                    if (countConnection < 3) {
                        changeVisiable(View.VISIBLE, View.GONE, View.GONE)
                        startOffTimer(outAgeTime)
                    } else {
                        changeVisiable(View.GONE, View.VISIBLE, View.GONE)
                    }
                    writeDataS(bleDevice)
                }, {
                    image_bluetooth.setImageResource(R.drawable.bluetooth_no)
                    if (!isConnection && countConnection != 3 && !physicalSuccessTag) {//连接成功后主动断开
                        physical_recovery_state_progress.visibility = View.GONE
                        disposableShowResultDelay?.dispose()
                        disposableShowResultDelay = Observable.timer(3000, TimeUnit.MILLISECONDS)
                                .subscribeOn(Schedulers.io())
                                .observeOn(AndroidSchedulers.mainThread())
                                .subscribe({
                                    changeVisiable(View.GONE, View.GONE, View.VISIBLE)
                                }, {})
                    }
                    disposableConnect
                    isConnection = false
                    LogUtils.v("zcl------物理流程状态监听恢复断开连接$it")
                })
    }

    private fun writeDataS(bleDevice: RxBleDevice) {
        val disposable = RxBleManager.writeData(bleDevice, byteArrayOf(0xf9.toByte())).subscribe({}, {
            LogUtils.v("zcl物理写入数据错误$it")
        })
    }

    @SuppressLint("CheckResult", "StringFormatMatches")
    private fun startOffTimer(time: Long) {//连接状态下断电倒计时
        disposableConnectOnTimer?.dispose()
        disposableConnectOffTimer?.dispose()

        disposableConnectOffTimer = Observable.intervalRange(0, time, 0, 1, TimeUnit.SECONDS)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe {
                    var num = time - 1 - it
                    physical_recovery_state_warm.textColor = getColor(R.color.gray_3)
                    changeOffColor(num)

                    if (!isConnection) {//如果已经断联是正常的走流程 false
                        LogUtils.e("zcl物理流程状态断联了嘛__________还在连接?________$isConnection")
                        when (countConnection) {//第一第二次流程
                            1, 2 -> {//断1,2后开启通电倒计时 5秒后通电
                                startOnTimer(outPoweOnTime)
                            }
                        }
                    } else {//断联倒计时到了还没断联 升级失败
                        if (0L == num)
                            changeVisiable(View.GONE, View.GONE, View.VISIBLE)
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

    @SuppressLint("StringFormatMatches")
    private fun startOnTimer(time: Long) {//通电提醒倒计时
        disposableConnectOnTimer?.dispose()
        disposableConnectOffTimer?.dispose()
        disposableConnectOnTimer = Observable.intervalRange(0, time, 0, 1, TimeUnit.SECONDS)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe {
                    var number = 6 - it - 1
                    var num = time - 1 - it
                    physical_recovery_state_warm.textColor = getColor(R.color.gray_3)
                    if (it <= powerDownTime) {
                        changePowerDownTimerColor(number)
                    } else {
                        startScanBestRssi()//开始连接并通电 重新扫描时为了让连接的那个设备发广播提高连接成功率
//                        LogUtils.v("zcl物理流程通电提醒$countConnection")
                        changePowerOnColor(num)

                        if (isConnection) {//通电倒计时完毕为连接状态则走正常流程提醒断电
                            disposableConnectOnTimer?.dispose()
                            when (countConnection) {
                                1, 2 -> {
                                    startScanBestRssi()
                                    startOffTimer(outAgeTime)
                                }
                                3 -> {//第三次连接成功
                                    LogUtils.v("zcl物理流程恢复成功$countConnection")
                                    disposableShowResultDelay?.dispose()
                                    disposableShowResultDelay = Observable.timer(3000, TimeUnit.MILLISECONDS)
                                            .subscribeOn(Schedulers.io())
                                            .observeOn(AndroidSchedulers.mainThread())
                                            .subscribe({
                                                changeVisiable(View.GONE, View.VISIBLE, View.GONE)
                                            }, {})
                                    compositeDisposable.add(disposableShowResultDelay!!)
                                    physicalSuccessTag = true
                                }
                            }
                        } else {//通电倒计时内没有连接上 恢复失败
                            if (0L == num) {
                                changeVisiable(View.GONE, View.GONE, View.VISIBLE)
                                disposableConnectOnTimer?.dispose()
                            }
                        }
                    }
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
        physical_recovery_retry.setOnClickListener {
            countConnection = 0
            physical_recovery_state_progress.visibility = View.VISIBLE
            setSearcher()
            changeVisiable(View.VISIBLE, View.GONE, View.GONE)
            startScanBestRssi(true)
            physicalSuccessTag = false
        }
        physical_recovery_success.setOnClickListener {
            finish()
            ActivityUtils.startActivity(MainActivity::class.java)
        }
    }

    private fun initData() {
        setSearcher()
        setAllDispose()
        startScanBestRssi(true)
    }

    private fun setSearcher() {
        physical_recovery_state_warm.textColor = getColor(R.color.blue_text)
        physical_recovery_state_warm.text = getString(R.string.searching)
    }

    @SuppressLint("CheckResult")
    private fun initView() {
        toolbar.setNavigationIcon(R.drawable.navigation_back_white)
        toolbar.setNavigationOnClickListener {
            ActivityUtils.finishAllActivities()
            ActivityUtils.startActivity(MainActivity::class.java)
        }
        toolbarTv.text = getString(com.dadoutek.uled.R.string.physical_recovery)

        val style = SpannableStringBuilder(getString(R.string.reset_factory_fail))//恢复出厂设置失败,请重新上电重试
        if (isZh(this))
            style.setSpan(ForegroundColorSpan1(getColor(R.color.red)), 9, 14, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        else//Factory Settings restored fail,please power-on to try again
            style.setSpan(ForegroundColorSpan1(getColor(R.color.red)), style.length - 22, style.length - 1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        physical_recovery_text_fail.text = style
    }

    private fun changeVisiable(state: Int, success: Int, fail: Int) {
        if (!physicalSuccessTag) {
            physical_recovery_rly.visibility = state
            physical_recovery_success_ly.visibility = success
            physical_recovery_fail_ly.visibility = fail
        }
        if (fail == 0)
            compositeDisposable.dispose()
    }

    override fun onDestroy() {
        super.onDestroy()
        RxBleManager.disconnectAllDevice()
        setAllDispose()
    }

    private fun setAllDispose() {
        disposableScan?.dispose()
        disposableScanTimer?.dispose()
        disposableConnect?.dispose()

        disposableShowResultDelay?.dispose()
    }

    private fun isZh(context: Context): Boolean {
        val locale = context.resources.configuration.locale
        val language = locale.language
        return language.endsWith("zh")
    }
}