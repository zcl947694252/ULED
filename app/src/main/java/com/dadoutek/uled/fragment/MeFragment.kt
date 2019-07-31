package com.dadoutek.uled.fragment

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.*
import android.content.Context.POWER_SERVICE
import android.content.pm.PackageManager
import android.os.*
import android.provider.Settings
import android.support.v4.app.ActivityCompat
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.PopupWindow
import com.blankj.utilcode.util.ActivityUtils
import com.blankj.utilcode.util.CleanUtils
import com.blankj.utilcode.util.LogUtils
import com.blankj.utilcode.util.ToastUtils
import com.dadoutek.uled.R
import com.dadoutek.uled.communicate.Commander
import com.dadoutek.uled.intf.SyncCallback
import com.dadoutek.uled.light.DeviceResetGroupActivity
import com.dadoutek.uled.model.Constant
import com.dadoutek.uled.model.DbModel.DBUtils
import com.dadoutek.uled.model.DbModel.DbConnector
import com.dadoutek.uled.model.DbModel.DbCurtain
import com.dadoutek.uled.model.DbModel.DbLight
import com.dadoutek.uled.model.HttpModel.UserModel
import com.dadoutek.uled.model.SharedPreferencesHelper
import com.dadoutek.uled.network.NetworkObserver
import com.dadoutek.uled.othersview.AboutSomeQuestionsActivity
import com.dadoutek.uled.othersview.BaseFragment
import com.dadoutek.uled.othersview.InstructionsForUsActivity
import com.dadoutek.uled.othersview.SplashActivity
import com.dadoutek.uled.region.NetworkActivity
import com.dadoutek.uled.tellink.TelinkLightApplication
import com.dadoutek.uled.tellink.TelinkLightService
import com.dadoutek.uled.user.DeveloperActivity
import com.dadoutek.uled.util.*
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_main_content.*
import kotlinx.android.synthetic.main.fragment_me.*
import java.util.*
import java.util.concurrent.TimeUnit


/**
 * Created by hejiajun on 2018/4/16.
 */

class MeFragment : BaseFragment(), View.OnClickListener {
    private var cancel: Button? = null
    private var confirm: Button? = null
    private lateinit var pop: PopupWindow
    private var inflater: LayoutInflater? = null

    private var mApplication: TelinkLightApplication? = null

    private var sleepTime: Long = 250
    internal var isClickExlogin = false
    private var compositeDisposable = CompositeDisposable()
    private var mWakeLock: PowerManager.WakeLock? = null
    var b: Boolean = false

    private var mReceive: BluetoothStateBroadcastReceive? = null

    internal var syncCallback: SyncCallback = object : SyncCallback {

        override fun start() {
            showLoadingDialog(activity!!.getString(R.string.tip_start_sync))
        }

        override fun complete() {
            if (isClickExlogin) {
                SharedPreferencesHelper.putBoolean(activity, Constant.IS_LOGIN, false)
                TelinkLightService.Instance().disconnect()
                TelinkLightService.Instance().idleMode(true)

                restartApplication()
            } else {
                ToastUtils.showLong(activity!!.getString(R.string.upload_data_success))
            }
            hideLoadingDialog()
        }

        override fun error(msg: String) {
           //(msg)
            ToastUtils.showLong(msg)
            if (isClickExlogin) {
                AlertDialog.Builder(activity)
                        .setTitle(R.string.sync_error_exlogin)
                        .setIcon(android.R.drawable.ic_dialog_info)
                        .setPositiveButton(getString(android.R.string.ok)) { dialog, which ->
                            SharedPreferencesHelper.putBoolean(activity, Constant.IS_LOGIN, false)
                            TelinkLightService.Instance().idleMode(true)
                            dialog.dismiss()
                            restartApplication()
                        }
                        .setNegativeButton(getString(R.string.btn_cancel)) { dialog, which ->
                            dialog.dismiss()
                            isClickExlogin = false
                            hideLoadingDialog()
                        }.show()
            } else {
                isClickExlogin = false
                hideLoadingDialog()
            }
        }
    }

    private fun registerBluetoothReceiver() {
        if (mReceive == null) {
            mReceive = BluetoothStateBroadcastReceive()
        }
        val intentFilter = IntentFilter()
        intentFilter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED)
        intentFilter.addAction(BluetoothDevice.ACTION_ACL_CONNECTED)
        intentFilter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED)
        intentFilter.addAction("android.bluetooth.BluetoothAdapter.STATE_OFF")
        intentFilter.addAction("android.bluetooth.BluetoothAdapter.STATE_ON")
        activity?.registerReceiver(mReceive, intentFilter)
    }


    private val allLights: List<DbLight>
        get() {
            val groupList = DBUtils.groupList
            val lightList = ArrayList<DbLight>()

            for (i in groupList.indices) {
                lightList.addAll(DBUtils.getLightByGroupID(groupList[i].id!!))
            }
            return lightList
        }

    private val allCutain: List<DbCurtain>
        get() {
            val groupList = DBUtils.groupList
            val lightList = ArrayList<DbCurtain>()

            for (i in groupList.indices) {
                lightList.addAll(DBUtils.getCurtainByGroupID(groupList[i].id!!))
            }
            return lightList
        }


    private val allRely: List<DbConnector>
        get() {
            val groupList = DBUtils.groupList
            val lightList = ArrayList<DbConnector>()

            for (i in groupList.indices) {
                lightList.addAll(DBUtils.getConnectorByGroupID(groupList[i].id!!))
            }
            return lightList
        }

    internal var mHints = LongArray(6)//初始全部为0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.BRAND.contains("Huawei")) {
            sleepTime = 500
        } else {
            sleepTime = 200
        }
        b = SharedPreferencesHelper.getBoolean(TelinkLightApplication.getInstance(), "isShowDot", false)
        registerBluetoothReceiver()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        this.inflater = inflater
        val view = inflater.inflate(R.layout.fragment_me, null)
        return view
    }

    @SuppressLint("InvalidWakeLockTag")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initView(view)
        initClick()
//        rlRegion?.visibility= View.GONE
        val pm = activity!!.getSystemService(POWER_SERVICE) as PowerManager
        mWakeLock = pm.newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK or PowerManager.ON_AFTER_RELEASE, "DPA")
    }

    override fun onResume() {
        super.onResume()
        isVisableDeveloper()
        if (mWakeLock != null) {
            mWakeLock?.acquire()
        }
        val blueadapter = BluetoothAdapter.getDefaultAdapter()
        if (blueadapter?.isEnabled == false) {
            if (bluetooth_image != null) {
                bluetooth_image.setImageResource(R.drawable.bluetooth_no)
                bluetooth_image.isEnabled = true
                bluetooth_image.setOnClickListener {
                    var dialog = BluetoothConnectionFailedDialog(activity, R.style.Dialog)
                    dialog.show()
                }
            }
        } else {
            if (TelinkLightApplication.getInstance().connectDevice == null) {
                if (bluetooth_image != null) {
                    bluetooth_image.setImageResource(R.drawable.bluetooth_no)
                    bluetooth_image.isEnabled = true
                    bluetooth_image.setOnClickListener {
                        var dialog = BluetoothConnectionFailedDialog(activity, R.style.Dialog)
                        dialog.show()
                    }
                }
            } else {
                if (bluetooth_image != null) {
                    bluetooth_image.setImageResource(R.drawable.bluetooth_yse)
                    bluetooth_image.isEnabled = false
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (mWakeLock != null) {
            mWakeLock?.release()
        }
        unregisterBluetoothReceiver()
    }

    private fun unregisterBluetoothReceiver() {
        if (mReceive != null) {
            activity?.unregisterReceiver(mReceive)
            mReceive = null
        }
    }

    fun initClick() {
        chearCache?.setOnClickListener(this)
        updateIte?.setOnClickListener(this)
        copyDataBase?.setOnClickListener(this)
        appVersion?.setOnClickListener(this)
        exitLogin?.setOnClickListener(this)
        oneClickBackup?.setOnClickListener(this)
        oneClickReset?.setOnClickListener(this)
        constantQuestion?.setOnClickListener(this)
        resetAllGroup?.setOnClickListener(this)
        instructions?.setOnClickListener(this)
        rlRegion?.setOnClickListener(this)
        developer?.setOnClickListener(this)
    }

    private fun initView(view: View) {
        val versionName = AppUtils.getVersionName(activity!!)
        appVersion!!.text = versionName
        //暂时屏蔽
        updateIte!!.visibility = View.GONE

        userIcon!!.setBackgroundResource(R.mipmap.ic_launcher)
        userName!!.text = DBUtils.lastUser!!.phone
        isVisableDeveloper()

        //todo 恢复出厂设置
        var popView: View = LayoutInflater.from(context).inflate(R.layout.pop_time_cancel, null)
        cancel = popView.findViewById(R.id.btn_cancel)
        confirm = popView.findViewById(R.id.btn_confirm)

        cancel?.let {
            it.setOnClickListener { PopUtil.dismiss(pop) }
        }
        confirm?.setOnClickListener {
            PopUtil.dismiss(pop)
            //恢复出厂设置
            if (TelinkLightApplication.getInstance().connectDevice != null)
                resetAllLight()
            else {
                ToastUtils.showShort(R.string.device_not_connected)
            }
        }
        pop = PopupWindow(popView, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        confirm?.isClickable = false
        pop!!.isOutsideTouchable = true
        pop.isFocusable = true // 设置PopupWindow可获得焦点
        pop.isTouchable = true // 设置PopupWindow可触摸补充：
    }

    private fun isVisableDeveloper() {
        val developerModel = SharedPreferencesUtils.isDeveloperModel()
        if (developerModel)
            developer.visibility = View.VISIBLE
        else
            developer.visibility = View.GONE
    }

    override fun setUserVisibleHint(isVisibleToUser: Boolean) {
        if (isVisibleToUser) {
            this.mApplication = TelinkLightApplication.getApp()
        } else {
            compositeDisposable.dispose()
        }
    }

    inner class BluetoothStateBroadcastReceive : BroadcastReceiver() {

        override fun onReceive(context: Context, intent: Intent) {
            val action = intent.action
            val device = intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)
            when (action) {
                BluetoothDevice.ACTION_ACL_CONNECTED -> {
                    if (bluetooth_image != null) {
                        bluetooth_image.setImageResource(R.drawable.bluetooth_yse)
                        bluetooth_image.isEnabled = false
                    }
                }
                BluetoothDevice.ACTION_ACL_DISCONNECTED -> {
                    if (bluetooth_image != null) {
                        bluetooth_image.setImageResource(R.drawable.bluetooth_no)
                        bluetooth_image.isEnabled = true
                        bluetooth_image.setOnClickListener {
                            var dialog = BluetoothConnectionFailedDialog(activity, R.style.Dialog)
                            dialog.show()
                        }
                    }
                }
                BluetoothAdapter.ACTION_STATE_CHANGED -> {
                    when (intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, 0)) {
                        BluetoothAdapter.STATE_OFF -> {
                            if (bluetooth_image != null) {
                                bluetooth_image.setImageResource(R.drawable.bluetooth_no)
                                bluetooth_image.isEnabled = true
                                bluetooth_image.setOnClickListener {
                                    var dialog = BluetoothConnectionFailedDialog(activity, R.style.Dialog)
                                    dialog.show()
                                }
                            }
                        }
                        BluetoothAdapter.STATE_ON -> {
                            if (bluetooth_image != null) {
                                bluetooth_image.setImageResource(R.drawable.bluetooth_no)
                                bluetooth_image.isEnabled = false
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.chearCache -> emptyTheCache()
            R.id.updateIte -> ToastUtils.showShort(R.string.wait_develop)
            R.id.copyDataBase -> verifyStoragePermissions(activity)
            R.id.appVersion -> developerMode()
            R.id.exitLogin -> exitLogin()
            R.id.oneClickBackup -> checkNetworkAndSync(activity)
            R.id.oneClickReset -> showSureResetDialogByApp()
            R.id.constantQuestion -> startActivity(Intent(activity, AboutSomeQuestionsActivity::class.java))
//            R.id.showGuideAgain -> showGuideAgainFun()
            R.id.resetAllGroup -> gotoResetAllGroup()
            R.id.instructions -> {
                var intent = Intent(activity, InstructionsForUsActivity::class.java)
                startActivity(intent)
            }
            R.id.rlRegion -> {
                var intent = Intent(activity, NetworkActivity::class.java)
                startActivity(intent)
            }
            R.id.developer -> {
                var intent = Intent(activity, DeveloperActivity::class.java)
                startActivity(intent)
            }
        }
    }

    private fun gotoResetAllGroup() {
        showResetSelectTypeDialog()
    }

    private fun showResetSelectTypeDialog() {
        val intent = Intent(activity, DeviceResetGroupActivity::class.java)
        val builder: AlertDialog.Builder = AlertDialog.Builder(activity)
        builder.setMessage(getString(R.string.please_select_light_type))
        builder.setPositiveButton(R.string.rgb_light) { dialog, which ->
            intent.putExtra(Constant.IS_SCAN_RGB_LIGHT, true)
            startActivityForResult(intent, Activity.RESULT_OK)
        }
        builder.setNeutralButton(R.string.cancel) { dialog, which ->

        }
        builder.setNegativeButton(R.string.normal_light) { dialog, which ->
            intent.putExtra(Constant.IS_SCAN_RGB_LIGHT, false)
            startActivityForResult(intent, Activity.RESULT_OK)
            Log.d("", "")
        }
        builder.create().show()
    }

    // 如果没有网络，则弹出网络设置对话框
    fun checkNetworkAndSync(activity: Activity?) {
        if (!NetWorkUtils.isNetworkAvalible(activity!!)) {
            AlertDialog.Builder(activity)
                    .setTitle(R.string.network_tip_title)
                    .setMessage(R.string.net_disconnect_tip_message)
                    .setPositiveButton(android.R.string.ok
                    ) { _, _ ->
                        // 跳转到设置界面
                        activity.startActivityForResult(Intent(
                                Settings.ACTION_WIRELESS_SETTINGS),
                                0)
                    }.create().show()
        } else {
            SyncDataPutOrGetUtils.syncPutDataStart(activity, syncCallback)
        }
    }

    @SuppressLint("CheckResult", "SetTextI18n")
    private fun showSureResetDialogByApp() {
        val developMode = SharedPreferencesUtils.isDeveloperModel()
        if (!developMode)
            Observable.intervalRange(0, 6, 0, 1, TimeUnit.SECONDS)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe {
                        var num = 5 - it as Long
                        //("zcl**********************num$num")
                        if (num == 0L) {
                            confirm?.isClickable = true
                            confirm?.text = getString(R.string.btn_ok)
                        } else {
                            confirm?.isClickable = false
                            confirm?.text = getString(R.string.btn_ok) + "(" + num + "s)"
                        }
                    }
        pop.showAtLocation(view, Gravity.CENTER, 0, 0)
        /*val builder = AlertDialog.Builder(activity)
        builder.setTitle(R.string.tip_reset_sure)
        builder.setNegativeButton(R.string.btn_cancel) { dialog, which -> }

        builder.setPositiveButton(android.R.string.ok) { dialog, which ->
            if (TelinkLightApplication.getInstance().connectDevice != null)
                resetAllLight()
            else {
                ToastUtils.showShort(R.string.device_not_connected)
            }
        }
        val dialog = builder.create()
        dialog.show()*/
    }


    private fun resetAllLight() {
        showLoadingDialog(getString(R.string.reset_all_now))
        SharedPreferencesHelper.putBoolean(activity, Constant.DELETEING, true)
        val lightList = allLights
        val curtainList = allCutain
        val relyList = allRely
        var meshAdre = ArrayList<Int>()
        if (lightList.isNotEmpty()) {
            for (k in lightList.indices) {
                meshAdre.add(lightList[k].meshAddr)
            }
        }

        if (curtainList.isNotEmpty()) {
            for (k in curtainList.indices) {
                meshAdre.add(curtainList[k].meshAddr)
            }
        }

        if (relyList.isNotEmpty()) {
            for (k in relyList.indices) {
                meshAdre.add(relyList[k].meshAddr)
            }
        }

        if (meshAdre.size > 0) {
            Commander.resetLights(meshAdre, {
                SharedPreferencesHelper.putBoolean(activity, Constant.DELETEING, false)
                syncData()
                activity?.bnve?.currentItem = 0
                null
            }, {
                SharedPreferencesHelper.putBoolean(activity, Constant.DELETEING, false)
                null
            })
        }
        if (meshAdre.isEmpty()) {
            hideLoadingDialog()
        }
    }

    private fun syncData() {
        SyncDataPutOrGetUtils.syncPutDataStart(activity!!, object : SyncCallback {
            override fun complete() {
                hideLoadingDialog()
                val disposable = Observable.timer(500, TimeUnit.MILLISECONDS)
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe { aLong -> }
                if (compositeDisposable.isDisposed) {
                    compositeDisposable = CompositeDisposable()
                }
                compositeDisposable.add(disposable)
            }

            override fun error(msg: String) {
                hideLoadingDialog()
                ToastUtils.showShort(R.string.backup_failed)
            }

            override fun start() {
            }
        })

    }


    private fun exitLogin() {
        isClickExlogin = true
        val b1 = (DBUtils.allLight.isEmpty() && !DBUtils.dataChangeAllHaveAboutLight && DBUtils.allCurtain.isEmpty()
                && !DBUtils.dataChangeAllHaveAboutCurtain && DBUtils.allRely.isEmpty() && !DBUtils.dataChangeAllHaveAboutRelay)
        if (b1) {
            if (isClickExlogin) {
                SharedPreferencesHelper.putBoolean(activity, Constant.IS_LOGIN, false)
                TelinkLightService.Instance().disconnect()
                TelinkLightService.Instance().idleMode(true)

                restartApplication()
            }
            hideLoadingDialog()
        } else {
            checkNetworkAndSync(activity)
        }
    }


    private fun developerMode() {
//        var isBoolean: Boolean = SharedPreferencesHelper.getBoolean(TelinkLightApplication.getInstance(),Constant.IS_DEVELOPER_MODE,false)
////        if(isBoolean){
////            //将mHints数组内的所有元素左移一个位置
////            System.arraycopy(mHints, 1, mHints, 0, mHints.size - 1)
////            //获得当前系统已经启动的时间
////            mHints[mHints.size - 1] = SystemClock.uptimeMillis()
////            if (SystemClock.uptimeMillis() - mHints[0] <= 1000) {
////                ToastUtils.showLong(R.string.developer_mode_close)
//////                copyDataBase!!.visibility = View.GONE
//////                chearCache!!.visibility = View.GONE
//////                resetAllGroup.visibility = View.GONE
////                //开发者模式启动时启动LOG日志
////                LogUtils.getConfig().setLog2FileSwitch(false)
////                LogUtils.getConfig().setDir(LOG_PATH_DIR)
////                SharedPreferencesUtils.setDeveloperModel(false)
////            }
////        }else{
////            //将mHints数组内的所有元素左移一个位置
////            System.arraycopy(mHints, 1, mHints, 0, mHints.size - 1)
////            //获得当前系统已经启动的时间
////            mHints[mHints.size - 1] = SystemClock.uptimeMillis()
////            if (SystemClock.uptimeMillis() - mHints[0] <= 1000) {
////                ToastUtils.showLong(R.string.developer_mode)
//////                copyDataBase!!.visibility = View.VISIBLE
//////                chearCache!!.visibility = View.VISIBLE
//////                resetAllGroup.visibility=View.VISIBLE
////                //开发者模式启动时启动LOG日志
////                LogUtils.getConfig().setLog2FileSwitch(true)
////                LogUtils.getConfig().setDir(LOG_PATH_DIR)
////                SharedPreferencesUtils.setDeveloperModel(true)
////            }
////        }

        System.arraycopy(mHints, 1, mHints, 0, mHints.size - 1)
        mHints[mHints.size - 1] = SystemClock.uptimeMillis()
        if (SystemClock.uptimeMillis() - mHints[0] <= 1000) {
            val alertDialog = AlertDialog.Builder(activity)
                    .setTitle(R.string.developer_mode_on)
                    .setIcon(android.R.drawable.ic_dialog_info)
                    .setPositiveButton(getString(R.string.open_mode)) { dialog, which ->
                        LogUtils.getConfig().setLog2FileSwitch(true)
                        LogUtils.getConfig().setDir(LOG_PATH_DIR)
                        SharedPreferencesUtils.setDeveloperModel(true)

                        startActivity(Intent(context, DeveloperActivity::class.java))
                        developer.visibility = View.VISIBLE

                        dialog.dismiss()
                    }
                    .setNegativeButton(getString(R.string.close_mode)) { dialog, which ->
                        LogUtils.getConfig().setLog2FileSwitch(false)
                        LogUtils.getConfig().setDir(LOG_PATH_DIR)
                        SharedPreferencesUtils.setDeveloperModel(false)
                        developer.visibility = View.GONE
                        dialog.dismiss()
                    }.create()
            alertDialog.setCancelable(false)
            alertDialog.show()
        }
    }

    //清空缓存初始化APP
    @SuppressLint("CheckResult")
    private fun emptyTheCache() {
//        AlertDialog.Builder(activity)
//                .setTitle(activity!!.getString(R.string.empty_cache_title))
//                .setMessage(activity!!.getString(R.string.empty_cache_tip))
//                .setNegativeButton(activity!!.getString(R.string.btn_cancel)) { dialog, which -> }
//                .setPositiveButton(activity!!.getString(android.R.string.ok)) { dialog, which ->
//                    TelinkLightService.Instance().idleMode(true)
//                    clearData()
//                }
//                .create().show()
        val alertDialog = AlertDialog.Builder(activity).setTitle(activity!!.getString(R.string.empty_cache_title))
                .setMessage(activity!!.getString(R.string.empty_cache_tip))
                .setPositiveButton(activity!!.getString(android.R.string.ok)) { dialog, which ->
                    TelinkLightService.Instance().idleMode(true)
                    clearData()
                }.setNegativeButton(activity!!.getString(R.string.btn_cancel)) { dialog, which -> }.create()
        alertDialog.show()
        val btn = alertDialog.getButton(DialogInterface.BUTTON_POSITIVE)
        btn.isEnabled = false
        val text = getString(android.R.string.ok)
        val timeout = 5
        Observable.interval(0, 1, TimeUnit.SECONDS, AndroidSchedulers.mainThread())
                .takeWhile { t: Long -> t < timeout }
                .doOnComplete {
                    btn.isEnabled = true
                    btn.text = text
                }
                .subscribe {
                    btn.text = "$text (${timeout - it})"
                }
    }

    private fun clearData() {
        val dbUser = DBUtils.lastUser

        if (dbUser == null) {
            ToastUtils.showLong(R.string.data_empty)
            return
        }

        showLoadingDialog(getString(R.string.clear_data_now))
        UserModel.deleteAllData(dbUser.token)!!.subscribe(object : NetworkObserver<String>() {
            override fun onNext(s: String) {
                SharedPreferencesHelper.putBoolean(activity, Constant.IS_LOGIN, false)
                DBUtils.deleteAllData()
                CleanUtils.cleanInternalSp()
                CleanUtils.cleanExternalCache()
                CleanUtils.cleanInternalFiles()
                CleanUtils.cleanInternalCache()
                ToastUtils.showShort(R.string.clean_tip)
                GuideUtils.resetAllGuide(activity!!)
                hideLoadingDialog()

                try {
                    Thread.sleep(300)
                } catch (e: InterruptedException) {
                    e.printStackTrace()
                }

                restartApplication()
            }

            override fun onError(e: Throwable) {
                super.onError(e)
                ToastUtils.showLong(R.string.clear_fail)
                hideLoadingDialog()
            }
        })
    }

    //重启app并杀死原进程
    private fun restartApplication() {
        ActivityUtils.finishAllActivities(true)
        ActivityUtils.startActivity(SplashActivity::class.java)
    }

    companion object {

        private val REQUEST_EXTERNAL_STORAGE = 1
        private val PERMISSIONS_STORAGE = arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE)

        fun verifyStoragePermissions(activity: Activity?) {
            // Check if we have write permission
            val permission = ActivityCompat.checkSelfPermission(activity!!, Manifest.permission.WRITE_EXTERNAL_STORAGE)


            if (permission != PackageManager.PERMISSION_GRANTED) {
                // We don't have permission so prompt the user
                ActivityCompat.requestPermissions(
                        activity,
                        PERMISSIONS_STORAGE,
                        REQUEST_EXTERNAL_STORAGE
                )
            } else {
                DBManager.getInstance().copyDatabaseToSDCard(activity)
                ToastUtils.showShort(R.string.copy_complete)
            }
        }

        private val LOG_PATH_DIR = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).path
    }
}
