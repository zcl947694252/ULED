package com.dadoutek.uled.region

import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.app.Dialog
import android.content.DialogInterface
import android.content.Intent
import android.provider.Settings
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.LinearLayoutManager.VERTICAL
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.PopupWindow
import com.blankj.utilcode.util.ActivityUtils
import com.blankj.utilcode.util.CleanUtils
import com.blankj.utilcode.util.ToastUtils
import com.dadoutek.uled.R
import com.dadoutek.uled.base.BaseActivity
import com.dadoutek.uled.communicate.Commander
import com.dadoutek.uled.intf.SyncCallback
import com.dadoutek.uled.model.Constant
import com.dadoutek.uled.model.DbModel.DBUtils
import com.dadoutek.uled.model.HttpModel.UserModel
import com.dadoutek.uled.model.SharedPreferencesHelper
import com.dadoutek.uled.network.NetworkObserver
import com.dadoutek.uled.othersview.SplashActivity
import com.dadoutek.uled.region.adapter.SettingAdapter
import com.dadoutek.uled.tellink.TelinkLightApplication
import com.dadoutek.uled.tellink.TelinkLightService
import com.dadoutek.uled.util.*
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_main_content.*
import kotlinx.android.synthetic.main.activity_setting.*
import kotlinx.android.synthetic.main.toolbar.*
import java.util.*
import java.util.concurrent.TimeUnit


/**
 * 创建者     ZCL
 * 创建时间   2019/8/1 9:25
 * 描述	      ${清除数据上传数据以及全部恢复出厂}
 *
 * 更新者     $Author$
 * 更新时间   $Date$
 * 更新描述   ${TODO}
 */
class SettingActivity : BaseActivity() {
    override fun setLayoutID(): Int {
        return  R.layout.activity_setting
    }

    private var cancel: Button? = null
    private var confirm: Button? = null
    private lateinit var pop: PopupWindow
    private var loadDialog1: Dialog? = null
    private var compositeDisposable = CompositeDisposable()

  

    override fun initListener() {

    }

    override fun initData() {
        val list = arrayListOf<SettingItemBean>()
        list.add(SettingItemBean(R.drawable.icon_clear_data, getString(R.string.chear_cache)))
        list.add(SettingItemBean(R.drawable.icon_local_data, getString(R.string.upload_data)))
        list.add(SettingItemBean(R.drawable.icon_restore_factory, getString(R.string.one_click_reset)))

        recycleView_setting.layoutManager = LinearLayoutManager(this, VERTICAL, false)
        val settingAdapter = SettingAdapter(R.layout.item_setting, list)
        recycleView_setting.adapter = settingAdapter

        settingAdapter.bindToRecyclerView(recycleView_setting)

        settingAdapter.setOnItemClickListener { _, _, position ->
            when (position) {
                0 -> {
                    emptyTheCache()
                }
                1 -> {
                    checkNetworkAndSyncs(this)
                }
                2 -> {
                    showSureResetDialogByApp()
                }
            }
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
                        if (num == 0L) {
                            confirm?.isClickable = true
                            confirm?.text = getString(R.string.btn_ok)
                        } else {
                            confirm?.isClickable = false
                            confirm?.text = getString(R.string.btn_ok) + "(" + num + "s)"
                        }
                    }
        else
            confirm?.isClickable = true

        pop.showAtLocation(window.decorView, Gravity.CENTER, 0, 0)
    }

    /**
     * 检查网络上传数据
     * 如果没有网络，则弹出网络设置对话框
     */
     fun checkNetworkAndSyncs(activity: Activity?) {
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
            if (DBUtils.lastUser?.id.toString() == DBUtils.lastUser?.last_authorizer_user_id)
                SyncDataPutOrGetUtils.syncPutDataStart(activity, syncCallback)
        }
    }

    /**
     * 上传回调
     */
    internal var syncCallback: SyncCallback = object : SyncCallback {
        override fun start() {
            showLoadingDialog(this@SettingActivity.getString(R.string.tip_start_sync))
        }

        override fun complete() {
            ToastUtils.showLong(this@SettingActivity.getString(R.string.upload_data_success))
            hideLoadingDialog()
        }

        override fun error(msg: String) {
            ToastUtils.showLong(msg)
            hideLoadingDialog()
        }
    }

    //清空缓存初始化APP
    @SuppressLint("CheckResult", "SetTextI18n")
    private fun emptyTheCache() {
        val alertDialog = AlertDialog.Builder(this).setTitle(getString(R.string.empty_cache_title))
                .setMessage(getString(R.string.empty_cache_tip))
                .setPositiveButton(getString(android.R.string.ok)) { _, _ ->
                    TelinkLightService.Instance()?.idleMode(true)
                    clearData()
                }.setNegativeButton(getString(R.string.btn_cancel)) { _, _ -> }.create()
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

    /**
     * 清除数据
     */
    private fun clearData() {
        val dbUser = DBUtils.lastUser

        if (dbUser == null) {
            ToastUtils.showLong(R.string.data_empty)
            return
        }

        showLoadingDialog(getString(R.string.clear_data_now))
        UserModel.deleteAllData(dbUser.token)!!.subscribe(object : NetworkObserver<String>() {
            override fun onNext(s: String) {
                SharedPreferencesHelper.putBoolean(this@SettingActivity, Constant.IS_LOGIN, false)
                DBUtils.deleteAllData()
                CleanUtils.cleanInternalSp()
                CleanUtils.cleanExternalCache()
                CleanUtils.cleanInternalFiles()
                CleanUtils.cleanInternalCache()
                ToastUtils.showShort(R.string.clean_tip)
                GuideUtils.resetAllGuide(this@SettingActivity)
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

    override fun initView() {
        image_bluetooth.visibility = View.GONE
        toolbar.title = getString(R.string.setting)
        toolbar.setNavigationIcon(R.mipmap.icon_return)
        toolbar.setNavigationOnClickListener {
            finish()
        }
        makePop()
    }

    /**
     *  恢复出厂设置Popwindow
     */
    private fun makePop() {
        var popView: View = LayoutInflater.from(this).inflate(R.layout.pop_time_cancel, null)
        cancel = popView.findViewById(R.id.btn_cancel)
        confirm = popView.findViewById(R.id.btn_confirm)

        cancel?.let {
            it.setOnClickListener { PopUtil.dismiss(pop) }
        }
        confirm?.setOnClickListener {
            PopUtil.dismiss(pop)
            //恢复出厂设置
            if (TelinkLightApplication.getInstance().connectDevice != null)
                resetAllLights()
            else {
                ToastUtils.showShort(R.string.device_not_connected)
            }
        }
        pop = PopupWindow(popView, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        confirm?.isClickable = false
        pop.isOutsideTouchable = true
        pop.isFocusable = true // 设置PopupWindow可获得焦点
        pop.isTouchable = true // 设置PopupWindow可触摸补充：
    }

     fun resetAllLights() {
        showLoadingDialog(getString(R.string.reset_all_now))
        SharedPreferencesHelper.putBoolean(this, Constant.DELETEING, true)
        //val lightList = allLights
        val lightList = DBUtils.getAllNormalLight()
        val allRGBLight = DBUtils.getAllRGBLight()

        lightList.addAll(allRGBLight)
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
                SharedPreferencesHelper.putBoolean(this@SettingActivity, Constant.DELETEING, false)
                syncData()
                this@SettingActivity?.bnve?.currentItem = 0
                null
            }, {
                SharedPreferencesHelper.putBoolean(this@SettingActivity, Constant.DELETEING, false)
                null
            })
        }
        if (meshAdre.isEmpty()) {
            hideLoadingDialog()
        }
    }


    private fun syncData() {
        SyncDataPutOrGetUtils.syncPutDataStart(this@SettingActivity!!, object : SyncCallback {
            override fun complete() {
                hideLoadingDialog()
                val disposable = Observable.timer(500, TimeUnit.MILLISECONDS)
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe { }
                if (compositeDisposable.isDisposed) {
                    compositeDisposable = CompositeDisposable()
                }
                compositeDisposable.add(disposable)
            }

            override fun error(msg: String) {
                hideLoadingDialog()
                ToastUtils.showShort(R.string.backup_failed)
            }

            override fun start() {}
        })

    }







    //重启app并杀死原进程
    private fun restartApplication() {
        ActivityUtils.finishAllActivities(true)
        ActivityUtils.startActivity(SplashActivity::class.java)
    }
}


