package com.dadoutek.uled.region

import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.graphics.Color
import android.provider.Settings
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.LinearLayoutManager.VERTICAL
import android.text.SpannableString
import android.text.Spanned
import android.text.TextPaint
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.PopupWindow
import android.widget.TextView
import com.blankj.utilcode.util.CleanUtils
import com.blankj.utilcode.util.LogUtils
import com.blankj.utilcode.util.ToastUtils
import com.dadoutek.uled.R
import com.dadoutek.uled.base.BaseActivity
import com.dadoutek.uled.communicate.Commander
import com.dadoutek.uled.intf.SyncCallback
import com.dadoutek.uled.model.Constant
import com.dadoutek.uled.model.Constant.downTime
import com.dadoutek.uled.model.DbModel.DBUtils
import com.dadoutek.uled.model.HttpModel.UserModel
import com.dadoutek.uled.model.Opcode
import com.dadoutek.uled.model.SharedPreferencesHelper
import com.dadoutek.uled.network.NetworkObserver
import com.dadoutek.uled.othersview.InstructionsForUsActivity
import com.dadoutek.uled.region.adapter.SettingAdapter
import com.dadoutek.uled.region.bean.SettingItemBean
import com.dadoutek.uled.tellink.TelinkLightApplication
import com.dadoutek.uled.tellink.TelinkLightService
import com.dadoutek.uled.util.GuideUtils
import com.dadoutek.uled.util.NetWorkUtils
import com.dadoutek.uled.util.PopUtil
import com.dadoutek.uled.util.SyncDataPutOrGetUtils
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_main_content.*
import kotlinx.android.synthetic.main.activity_setting.*
import kotlinx.android.synthetic.main.toolbar.*
import org.jetbrains.anko.backgroundColor
import java.util.*
import java.util.concurrent.TimeUnit


/**
 * 创建者     ZCL
 * 创建时间   2019/8/1 9:25
 * 描述	      ${清除数据上传数据以及全部恢复出厂}
 *
 * 更新者     $Author$
 * 更新时间   $Date$
 */
class SettingActivity : BaseActivity() {
    override fun setLayoutID(): Int {
        this.mApplication = this.application as TelinkLightApplication
        return R.layout.activity_setting
    }

    private var disposableInterval: Disposable? = null
    private var disposable: Disposable? = null
    private var mApplication: TelinkLightApplication? = null
    private lateinit var cancel: Button
    private lateinit var confirm: Button
    private lateinit var pop: PopupWindow
    private var compositeDisposable = CompositeDisposable()
    lateinit var tvOne: TextView
    lateinit var tvTwo: TextView
    lateinit var tvThree: TextView
    lateinit var hinitOne: TextView
    lateinit var hinitTwo: TextView
    lateinit var hinitThree: TextView
    lateinit var readTimer: TextView
    lateinit var cancelConfirmLy: LinearLayout
    lateinit var cancelConfirmVertical: View
    var isResetFactory = false

    override fun initListener() {}

    override fun initData() {
        val list = arrayListOf<SettingItemBean>()
        list.add(SettingItemBean(R.drawable.icon_clear_data, getString(R.string.chear_cache)))
        list.add(SettingItemBean(R.drawable.icon_local_data, getString(R.string.upload_data)))
        list.add(SettingItemBean(R.drawable.icon_restore_factory, getString(R.string.one_click_reset)))
        list.add(SettingItemBean(R.drawable.icon_restore_factory, getString(R.string.user_reset)))

       // listTask.add(SettingItemBean(R.drawable.icon_restore_factory, getString(R.string.physical_recovery)))

        recycleView_setting.layoutManager = LinearLayoutManager(this, VERTICAL, false)
        val settingAdapter = SettingAdapter(R.layout.item_setting, list)
        recycleView_setting.adapter = settingAdapter

        settingAdapter.bindToRecyclerView(recycleView_setting)

        settingAdapter.setOnItemClickListener { _, _, position ->

            val lastUser = DBUtils.lastUser
            lastUser?.let {
                if (it.id.toString() != it.last_authorizer_user_id)
                    ToastUtils.showLong(getString(R.string.author_region_warm))
                else {
                    when (position) {
                        0 -> emptyTheCache()
                        1 -> checkNetworkAndSyncs(this)
                        2 -> showSureResetDialogByApp()
                        3 -> userReset()
                    }
                }
            }
        }
    }

    private fun userReset() {
        val builder = AlertDialog.Builder(this)
        builder.setMessage(getString(R.string.user_reset))
        TelinkLightService.Instance().sendCommandNoResponse(Opcode.CONFIG_EXTEND_OPCODE, 1,
                byteArrayOf(Opcode.CONFIG_EXTEND_ALL_CLEAR))
        builder.setPositiveButton(getString(R.string.btn_sure)) { dialog, which ->
            clearData()//删除本地数据
            UserModel.clearUserData(1)?.subscribe(object : NetworkObserver<String?>() {//删除服务器数据
                override fun onNext(t: String) {
                    ToastUtils.showShort(getString(R.string.reset_user_success))
                }
                override fun onError(e: Throwable) {
                    super.onError(e)
                    ToastUtils.showShort(e.message)
                }
            })
        }
        builder.setNegativeButton(getString(R.string.cancel)) { dialog, which ->
            ToastUtils.showShort(getString(R.string.cancel))
        }
        builder.show()
    }

    override fun initView() {
        image_bluetooth.visibility = View.GONE
        toolbar.title = getString(R.string.setting)
        toolbar.setNavigationIcon(R.mipmap.icon_return)
        toolbar.setNavigationOnClickListener { finish() }
        makePop()
    }

    @SuppressLint("CheckResult", "SetTextI18n", "StringFormatMatches")
    private fun showSureResetDialogByApp() {
        isResetFactory = true
        setFirstePopAndShow(R.string.please_sure_all_device_power_on, R.string.reset_factory_all_device
                , R.string.have_question_look_notice, isResetFactory)
    }

    @SuppressLint("CheckResult", "StringFormatMatches")
    private fun setFirstePopAndShow(pleaseSureAllDevicePowerOn: Int, resetFactoryAllDevice: Int, haveQuestionLookNotice: Int, isShowThree: Boolean) {
        setNormalPopSetting()

        if (isShowThree) {
            tvThree.visibility = View.VISIBLE
            hinitThree.visibility = View.VISIBLE
        } else {
            tvThree.visibility = View.GONE
            hinitThree.visibility = View.GONE
        }

        hinitOne.text = getString(pleaseSureAllDevicePowerOn)
        hinitTwo.text = getString(resetFactoryAllDevice)
        hinitThree.text = getString(haveQuestionLookNotice)
        disposableInterval?.dispose()
        disposableInterval = Observable.intervalRange(0, downTime, 0, 1, TimeUnit.SECONDS)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe {
                    var num = downTime -1 - it
                    if (num == 0L) {
                        setTimerZero()
                    } else {
                        cancelConfirmVertical.backgroundColor = resources.getColor(R.color.white)
                        cancel.isClickable = false
                        confirm.isClickable = false
                        readTimer.text = getString(R.string.please_read_carefully, num)
                    }
                }


        pop.showAtLocation(window.decorView, Gravity.CENTER, 0, 0)
    }

    private fun setNormalPopSetting() {
        confirm?.isClickable = false

        tvOne.visibility = View.VISIBLE
        tvThree.visibility = View.VISIBLE
        tvTwo.visibility = View.VISIBLE

        hinitOne.visibility = View.VISIBLE
        hinitThree.visibility = View.VISIBLE
        hinitTwo.visibility = View.VISIBLE

        hinitTwo.textSize = 16F
        hinitTwo.setTextColor(resources.getColor(R.color.gray_3))
        readTimer.visibility = View.VISIBLE

        cancel.text = ""
        confirm.text = ""

        cancelConfirmVertical.backgroundColor = resources.getColor(R.color.white)
    }

    private fun setTimerZero() {
        readTimer.visibility = View.GONE
        cancelConfirmLy.visibility = View.VISIBLE

        cancelConfirmVertical.backgroundColor = resources.getColor(R.color.gray)

        tvOne.visibility = View.INVISIBLE
        tvTwo.visibility = View.INVISIBLE



        if (isResetFactory) {
            hinitTwo.visibility = View.VISIBLE
            hinitOne.text = getString(R.string.tip_reset_sure)
            hinitTwo.setTextColor(resources.getColor(R.color.red))
        } else {
            hinitTwo.visibility = View.GONE
            hinitOne.text = getString(R.string.empty_cache_tip)
        }

        hinitTwo.textSize = 13F
        hinitTwo.text = getString(R.string.reset_warm_red)

        tvThree.visibility = View.GONE
        hinitThree.visibility = View.GONE

        cancel.text = getString(R.string.cancel)
        confirm.text = getString(R.string.btn_sure)
        cancel.isClickable = true
        confirm.isClickable = true
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
                        activity.startActivityForResult(Intent(Settings.ACTION_WIRELESS_SETTINGS), 0)
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
    @SuppressLint("CheckResult", "SetTextI18n", "StringFormatMatches")
    private fun emptyTheCache() {
        isResetFactory = false
        setFirstePopAndShow(R.string.clear_one, R.string.clear_two, R.string.clear_one, isResetFactory)
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
                LogUtils.e("zcl-----------$s")
                SharedPreferencesHelper.putBoolean(this@SettingActivity, Constant.IS_LOGIN, false)
                DBUtils.deleteAllData()
                CleanUtils.cleanInternalSp()
                CleanUtils.cleanExternalCache()
                CleanUtils.cleanInternalFiles()
                CleanUtils.cleanInternalCache()
                ToastUtils.showLong(R.string.clean_tip)
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

    /**
     *  恢复出厂设置Popwindow
     */
    private fun makePop() {
        var popView: View = LayoutInflater.from(this).inflate(R.layout.pop_time_cancel, null)
        tvOne = popView.findViewById(R.id.tv_one)
        tvTwo = popView.findViewById(R.id.tv_two)
        tvThree = popView.findViewById(R.id.tv_three)
        hinitOne = popView.findViewById(R.id.hinit_one)
        hinitTwo = popView.findViewById(R.id.hinit_two)
        hinitThree = popView.findViewById(R.id.hinit_three)
        readTimer = popView.findViewById(R.id.read_timer)
        cancel = popView.findViewById(R.id.btn_cancel)
        confirm = popView.findViewById(R.id.btn_confirm)
        cancelConfirmLy = popView.findViewById(R.id.cancel_confirm_ly)
        cancelConfirmVertical = popView.findViewById(R.id.cancel_confirm_vertical)

        setNormalPopSetting()

        var cs: ClickableSpan = object : ClickableSpan() {
            override fun onClick(widget: View) {
                var intent = Intent(this@SettingActivity, InstructionsForUsActivity::class.java)
                startActivity(intent)
            }

            override fun updateDrawState(ds: TextPaint) {
                super.updateDrawState(ds)
                ds.color = Color.BLUE//设置超链接的颜色
                ds.isUnderlineText = true
            }
        }

        val str = getString(R.string.have_question_look_notice)
        var ss = SpannableString(str)
        val start: Int = if (Locale.getDefault().language.contains("zh")) str.length - 7 else str.length - 26
        val end = str.length
        ss.setSpan(cs, start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        hinitThree.text = ss
        hinitThree.movementMethod = LinkMovementMethod.getInstance()

        cancel?.let {
            it.setOnClickListener { PopUtil.dismiss(pop) }
        }
        confirm?.setOnClickListener {
            PopUtil.dismiss(pop)
            //恢复出厂设置
            if (isResetFactory) {
                if (TelinkLightApplication.getApp().connectDevice != null) {
                    resetAllLights()
                } else {
                    ToastUtils.showLong(R.string.device_not_connected)
                }
            } else {
                clearData()
            }
        }
        pop = PopupWindow(popView, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        confirm?.isClickable = false
        pop.isOutsideTouchable = false
        pop.isFocusable = true // 设置PopupWindow可获得焦点
        pop.isTouchable = true // 设置PopupWindow可触摸补充：
    }

    /**
     * 所有灯恢复出厂设置
     */
    private fun resetAllLights() {
        showLoadingDialog(getString(R.string.reset_all_now))
        SharedPreferencesHelper.putBoolean(this, Constant.DELETEING, true)
        //val lightList = allLights
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

        Commander.resetAllDevices(meshAdre, {
            SharedPreferencesHelper.putBoolean(this@SettingActivity, Constant.DELETEING, false)
            syncData()
            this@SettingActivity?.bnve?.currentItem = 0
            null
        }, {
            SharedPreferencesHelper.putBoolean(this@SettingActivity, Constant.DELETEING, false)
            null
        })
        if (meshAdre.isEmpty()) {
            hideLoadingDialog()
        }
    }


    private fun syncData() {
        SyncDataPutOrGetUtils.syncPutDataStart(this@SettingActivity!!, object : SyncCallback {
            override fun complete() {
                hideLoadingDialog()
                //时间太短会导致无法删除数据库数据故此设置1500秒
                if (compositeDisposable.isDisposed)
                    compositeDisposable = CompositeDisposable()
                disposable?.let {
                    compositeDisposable.add(disposable!!)
                }
            }

            override fun error(msg: String) {
                hideLoadingDialog()
                ToastUtils.showLong(R.string.backup_failed)
            }

            override fun start() {}
        })
    }

    override fun onDestroy() {
        super.onDestroy()
        compositeDisposable.dispose()
    }
}


