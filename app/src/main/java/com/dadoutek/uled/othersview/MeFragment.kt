package com.dadoutek.uled.othersview

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Context.POWER_SERVICE
import android.content.Intent
import android.content.pm.PackageManager
import android.os.*
import android.provider.Settings
import android.support.v4.app.ActivityCompat
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.widget.Button
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.TextView
import com.app.hubert.guide.NewbieGuide
import com.app.hubert.guide.core.Controller
import com.app.hubert.guide.model.GuidePage

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
import com.dadoutek.uled.model.DbModel.DbLight
import com.dadoutek.uled.model.HttpModel.UserModel
import com.dadoutek.uled.model.SharedPreferencesHelper
import com.dadoutek.uled.network.NetworkObserver
import com.dadoutek.uled.scene.SceneFragment
import com.dadoutek.uled.tellink.TelinkLightApplication
import com.dadoutek.uled.tellink.TelinkLightService
import com.dadoutek.uled.util.*
import com.telink.TelinkApplication
import com.telink.bluetooth.event.NotificationEvent

import java.util.ArrayList
import java.util.concurrent.TimeUnit

import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import kotlinx.android.synthetic.main.activity_main_content.*
import kotlinx.android.synthetic.main.fragment_me.*
import kotlinx.android.synthetic.main.toolbar.*


/**
 * Created by hejiajun on 2018/4/16.
 */

class MeFragment : BaseFragment(),View.OnClickListener {
    private var inflater: LayoutInflater? = null

    private var mApplication: TelinkLightApplication? = null

    private var sleepTime: Long = 250
    internal var isClickExlogin = false
    private var compositeDisposable = CompositeDisposable()
    private var mWakeLock: PowerManager.WakeLock? = null

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

            //            Log.d("SyncLog", "error: " + msg);
            //            ToastUtils.showLong(getString(R.string.sync_error_contant));
        }
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

    internal var mHints = LongArray(6)//初始全部为0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (Build.BRAND.contains("Huawei")) {
            sleepTime = 500
        } else {
            sleepTime = 200
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        this.inflater = inflater
        val view = inflater.inflate(R.layout.fragment_me, null)
//        initView(view)
//        initClick()
        return view
    }

    @SuppressLint("InvalidWakeLockTag")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initView(view)
        initClick()
        val pm = activity!!.getSystemService(POWER_SERVICE) as PowerManager
        mWakeLock = pm.newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK or PowerManager.ON_AFTER_RELEASE, "DPA")
    }

    override fun onResume() {
        super.onResume()
        if (mWakeLock != null) {
            mWakeLock?.acquire()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (mWakeLock != null) {
            mWakeLock?.release()
        }
    }

    fun initClick(){
        chearCache?.setOnClickListener(this)
        updateIte?.setOnClickListener(this)
        copyDataBase?.setOnClickListener(this)
        appVersion?.setOnClickListener(this)
        exitLogin?.setOnClickListener(this)
        oneClickBackup?.setOnClickListener(this)
        oneClickReset?.setOnClickListener(this)
        constantQuestion?.setOnClickListener(this)
        showGuideAgain?.setOnClickListener(this)
        resetAllGroup?.setOnClickListener(this)
    }

    private fun initView(view: View) {
        toolbar.title=getString(R.string.fragment_name_me)
        val versionName = AppUtils.getVersionName(activity!!)
        appVersion!!.text = versionName
        //暂时屏蔽
        updateIte!!.visibility = View.GONE
        if (SharedPreferencesUtils.isDeveloperModel()) {
            copyDataBase!!.visibility = View.VISIBLE
            chearCache!!.visibility = View.VISIBLE
            resetAllGroup.visibility=View.VISIBLE
        } else {
            copyDataBase!!.visibility = View.GONE
            chearCache!!.visibility = View.VISIBLE
            resetAllGroup.visibility=View.GONE
        }

        userIcon!!.setBackgroundResource(R.drawable.ic_launcher)
        userName!!.text = DBUtils.lastUser!!.phone
    }

    override fun setUserVisibleHint(isVisibleToUser: Boolean) {
        if (isVisibleToUser) {
            this.mApplication = TelinkLightApplication.getApp()
            val mainAct = activity as MainActivity?
            //            getVersion();
        } else {
            compositeDisposable.dispose()
        }
    }

//    private fun initOnLayoutListener() {
//        val view = activity?.getWindow()?.getDecorView()
//        val viewTreeObserver = view?.getViewTreeObserver()
//        viewTreeObserver?.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
//            override fun onGlobalLayout() {
//                view.getViewTreeObserver().removeOnGlobalLayoutListener(this)
//                lazyLoad()
//            }
//        })
//    }
//
//    fun lazyLoad() {
//        val guide1 = chearCache
//        val guide2 = oneClickBackup
//        val guide3 = constantQuestion
//        val guide4 = oneClickReset
//        val guide5 = appVersion
//        val guide6 = exitLogin
//        val guide7 = showGuideAgain
//        GuideUtils.guideBuilder(this, Constant.TAG_MeFragment)
//                .addGuidePage(GuideUtils.addGuidePage(guide1, R.layout.view_guide_simple, getString(R.string.me_guide_1)))
//                .addGuidePage(GuideUtils.addGuidePage(guide2, R.layout.view_guide_simple, getString(R.string.me_guide_2)))
//                .addGuidePage(GuideUtils.addGuidePage(guide3, R.layout.view_guide_simple, getString(R.string.me_guide_3)))
//                .addGuidePage(GuideUtils.addGuidePage(guide4, R.layout.view_guide_simple_bottom, getString(R.string.me_guide_4)))
//                .addGuidePage(GuideUtils.addGuidePage(guide5, R.layout.view_guide_simple_bottom, getString(R.string.me_guide_5)))
//                .addGuidePage(GuideUtils.addGuidePage(guide6, R.layout.view_guide_simple_bottom, getString(R.string.me_guide_6)))
//                .addGuidePage(GuideUtils.addGuidePage(guide7, R.layout.view_guide_simple_bottom, getString(R.string.me_guide_7)))
//                .show()
//    }

    override fun onDestroyView() {
        super.onDestroyView()
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
            R.id.showGuideAgain -> showGuideAgainFun()
            R.id.resetAllGroup -> gotoResetAllGroup()
        }
    }

    private fun gotoResetAllGroup() {
        showResetSelectTypeDialog()
    }

    private fun showResetSelectTypeDialog() {
        val intent = Intent(activity,DeviceResetGroupActivity::class.java)
        val builder:AlertDialog.Builder=AlertDialog.Builder(activity)
        builder.setMessage(getString(R.string.please_select_light_type))
        builder.setPositiveButton(R.string.rgb_light) { dialog, which ->
            intent.putExtra(Constant.IS_SCAN_RGB_LIGHT,true)
            startActivityForResult(intent,Activity.RESULT_OK)
        }
        builder.setNeutralButton(R.string.cancel) { dialog, which ->

        }
        builder.setNegativeButton(R.string.normal_light) { dialog, which ->
            intent.putExtra(Constant.IS_SCAN_RGB_LIGHT,false)
            startActivityForResult(intent,Activity.RESULT_OK)
        }
        builder.create().show()
    }

    private fun showGuideAgainFun() {
        val builder = AlertDialog.Builder(activity)
        builder.setTitle(getString(R.string.show_guide_again_tip))
        builder.setNegativeButton(R.string.btn_cancel) { dialog, which -> }
        builder.setPositiveButton(android.R.string.ok) { dialog, which ->
           GuideUtils.resetAllGuide(activity!!)
            activity?.bnve?.currentItem=0
        }
        val dialog = builder.create()
        dialog.show()
    }

    // 如果没有网络，则弹出网络设置对话框
    fun checkNetworkAndSync(activity: Activity?) {
        if (!NetWorkUtils.isNetworkAvalible(activity!!)) {
            AlertDialog.Builder(activity)
                    .setTitle(R.string.network_tip_title)
                    .setMessage(R.string.net_disconnect_tip_message)
                    .setPositiveButton(android.R.string.ok
                    ) { dialog, whichButton ->
                        // 跳转到设置界面
                        activity.startActivityForResult(Intent(
                                Settings.ACTION_WIRELESS_SETTINGS),
                                0)
                    }.create().show()
        } else {
            SyncDataPutOrGetUtils.syncPutDataStart(activity, syncCallback)
        }
    }

    private fun showSureResetDialogByApp() {
        val builder = AlertDialog.Builder(activity)
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
        dialog.show()
    }


    private fun resetAllLight() {
        showLoadingDialog(getString(R.string.reset_all_now))
        SharedPreferencesHelper.putBoolean(activity, Constant.DELETEING, true)
        val lightList = allLights
        Commander.resetLights(lightList, {
            SharedPreferencesHelper.putBoolean(activity, Constant.DELETEING, false)
            syncData()
            hideLoadingDialog()
            activity?.bnve?.currentItem=0
            null
        }, {
            ToastUtils.showLong(R.string.error_disconnect_tip)
            SharedPreferencesHelper.putBoolean(activity, Constant.DELETEING, false)
            null
        })
    }

    private fun syncData() {
        SyncDataPutOrGetUtils.syncPutDataStart(activity!!, object : SyncCallback {
            override fun complete() {
                hideLoadingDialog()
                val disposable = Observable.timer(500, TimeUnit.MILLISECONDS)
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe { aLong ->
                            hideLightVersion()
                            //                                                addEventListener(); //加回连接状态监听。
                        }
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

    private fun hideLightVersion() {
        //        tvLightVersionText.setVisibility(View.GONE);
        //        tvLightVersion.setVisibility(View.GONE);
    }

    private fun addEventListener() {
        val act = activity as MainActivity?
        act?.addEventListeners()
    }

    private fun onOnlineStatusNotify(event: NotificationEvent) {
        Log.d("NNDadou", "onOnlineStatusNotify: " + event.type)
    }

    private fun exitLogin() {
        isClickExlogin = true
        if (DBUtils.allLight.size == 0 && !DBUtils.dataChangeAllHaveAboutLight) {
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
        //将mHints数组内的所有元素左移一个位置
        System.arraycopy(mHints, 1, mHints, 0, mHints.size - 1)
        //获得当前系统已经启动的时间
        mHints[mHints.size - 1] = SystemClock.uptimeMillis()
        if (SystemClock.uptimeMillis() - mHints[0] <= 1000) {
            ToastUtils.showLong(R.string.developer_mode)
            copyDataBase!!.visibility = View.VISIBLE
            chearCache!!.visibility = View.VISIBLE
            resetAllGroup.visibility=View.VISIBLE
            //开发者模式启动时启动LOG日志
            LogUtils.getConfig().setLog2FileSwitch(true)
            LogUtils.getConfig().setDir(LOG_PATH_DIR)
            SharedPreferencesUtils.setDeveloperModel(true)
        }
    }


    //清空缓存初始化APP
    private fun emptyTheCache() {
        AlertDialog.Builder(activity)
                .setTitle(activity!!.getString(R.string.empty_cache_title))
                .setMessage(activity!!.getString(R.string.empty_cache_tip))
                .setNegativeButton(activity!!.getString(R.string.btn_cancel)) { dialog, which -> }
                .setPositiveButton(activity!!.getString(android.R.string.ok)) { dialog, which ->
                    TelinkLightService.Instance().idleMode(true)
                    clearData()
                }
                .create().show()
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
                SharedPreferencesHelper.putObject(activity, Constant.OLD_INDEX_DATA, null)
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
