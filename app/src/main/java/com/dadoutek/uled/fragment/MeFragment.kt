package com.dadoutek.uled.fragment

import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.content.Context.POWER_SERVICE
import android.content.Intent
import android.graphics.Color
import android.os.*
import android.provider.Settings
import android.text.SpannableString
import android.text.Spanned
import android.text.TextPaint
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.util.Log
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
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CircleCrop
import com.bumptech.glide.request.RequestOptions
import com.dadoutek.uled.R
import com.dadoutek.uled.communicate.Commander
import com.dadoutek.uled.communicate.Commander.connect
import com.dadoutek.uled.intf.SyncCallback
import com.dadoutek.uled.model.Constant
import com.dadoutek.uled.model.dbModel.DBUtils
import com.dadoutek.uled.model.dbModel.DbConnector
import com.dadoutek.uled.model.dbModel.DbCurtain
import com.dadoutek.uled.model.dbModel.DbLight
import com.dadoutek.uled.model.DeviceType
import com.dadoutek.uled.model.httpModel.UserModel
import com.dadoutek.uled.model.Opcode
import com.dadoutek.uled.model.SharedPreferencesHelper
import com.dadoutek.uled.othersview.AboutSomeQuestionsActivity
import com.dadoutek.uled.othersview.BaseFragment
import com.dadoutek.uled.othersview.InstructionsForUsActivity
import com.dadoutek.uled.othersview.UserAgreementActivity
import com.dadoutek.uled.region.DeveloperActivity
import com.dadoutek.uled.region.NetworkActivity
import com.dadoutek.uled.router.BindRouterActivity
import com.dadoutek.uled.tellink.TelinkLightApplication
import com.dadoutek.uled.tellink.TelinkLightService
import com.dadoutek.uled.user.DeveloperOtaActivity
import com.dadoutek.uled.util.*
import com.telink.TelinkApplication
import com.telink.bluetooth.LeBluetooth
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_main_content.*
import kotlinx.android.synthetic.main.fragment_me.*
import org.jetbrains.anko.backgroundColor
import java.util.*
import java.util.concurrent.TimeUnit


/**
 * Created by hejiajun on 2018/4/16.
 */

class MeFragment() : BaseFragment(), View.OnClickListener {
    private var disposableInterval: Disposable? = null
    private var popUser: PopupWindow? = null
    private var cancelConfirmVertical: View? = null
    private var cancelConfirmLy: LinearLayout? = null
    private var readTimer: TextView? = null
    private var hinitOne: TextView? = null
    private var hinitTwo: TextView? = null
    private var hinitThree: TextView? = null
    private var mConnectDisposal: Disposable? = null
    private var disposable: Disposable? = null
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
    private val LOG_PATH_DIR = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).path

    internal var syncCallback: SyncCallback = object : SyncCallback {

        override fun start() {
            showLoadingDialog(getString(R.string.tip_start_sync))
        }

        override fun complete() {
            hideLoadingDialog()
            if (isClickExlogin) {
                SharedPreferencesHelper.putBoolean(activity, Constant.IS_LOGIN, false)
                TelinkLightService.Instance()?.disconnect()
                TelinkLightService.Instance()?.idleMode(true)

                restartApplication()
            } else {
                ToastUtils.showLong(activity!!.getString(R.string.upload_data_success))
            }
        }

        override fun error(msg: String) {
            hideLoadingDialog()
            ToastUtils.showLong(msg)
            if (isClickExlogin) {
                AlertDialog.Builder(activity)
                        .setTitle(R.string.sync_error_exlogin)
                        .setIcon(android.R.drawable.ic_dialog_info)
                        .setPositiveButton(getString(android.R.string.ok)) { dialog, which ->
                            SharedPreferencesHelper.putBoolean(activity, Constant.IS_LOGIN, false)
                            TelinkLightService.Instance()?.idleMode(true)
                            dialog.dismiss()
                            restartApplication()
                        }.setNegativeButton(getString(R.string.btn_cancel)) { dialog, _ ->
                            dialog.dismiss()
                            isClickExlogin = false
                            hideLoadingDialog()
                        }.show()
            } else {
                isClickExlogin = false
            }
        }
    }
    private val allLights: List<DbLight>
        get() {
            val groupList = DBUtils.groupList
            val lightList = ArrayList<DbLight>()

            for (i in groupList.indices)
                lightList.addAll(DBUtils.getLightByGroupID(groupList[i].id!!))

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
                lightList.addAll(DBUtils.getRelayByGroupID(groupList[i].id!!))
            }
            return lightList
        }

    private var mHints = LongArray(6)//初始全部为0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sleepTime = if (Build.BRAND.contains("Huawei"))
            500
        else
            200
        makePop()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        this.inflater = inflater
        return inflater.inflate(R.layout.fragment_me, null)
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
        if (Constant.IS_ROUTE_MODE)
            bluetooth_image?.setImageResource(R.drawable.icon_cloud)
        mWakeLock?.acquire()
    }

    override fun onDestroy() {
        super.onDestroy()
        if (mWakeLock?.isHeld == true)
            mWakeLock?.release()
        compositeDisposable.dispose()
    }

    private fun initClick() {
        userIcon?.setOnClickListener(this)
        chearCache?.setOnClickListener(this)
        appVersion?.setOnClickListener(this)
        exitLogin?.setOnClickListener(this)
        oneClickReset?.setOnClickListener(this)
        constantQuestion?.setOnClickListener(this)
        instructions?.setOnClickListener(this)
        user_agreenment?.setOnClickListener(this)
        rlRegion?.setOnClickListener(this)
        developer?.setOnClickListener(this)
        setting?.setOnClickListener(this)
        updata?.setOnClickListener(this)
        user_reset?.setOnClickListener(this)
        save_lock_ly?.setOnClickListener(this)
        bind_router?.setOnClickListener(this)
    }

    private fun initView(view: View) {
        val versionName = AppUtils.getVersionName(activity!!)
        appVersion!!.text = versionName

        userName!!.text = DBUtils.lastUser?.phone
        isVisableDeveloper()
        Glide.with(context!!)
                .load(R.drawable.ic_launcher)
                .apply(RequestOptions.bitmapTransform(CircleCrop()))
                .into(userIcon)


        makePop()
        makePop2()
    }

    private fun makePop2() {
        var popView: View = LayoutInflater.from(activity).inflate(R.layout.pop_time_cancel, null)
        hinitOne = popView.findViewById(R.id.hinit_one)
        hinitTwo = popView.findViewById(R.id.hinit_two)
        hinitThree = popView.findViewById(R.id.hinit_three)
        readTimer = popView.findViewById(R.id.read_timer)
        cancel = popView.findViewById(R.id.tip_cancel)
        confirm = popView.findViewById(R.id.tip_confirm)
        cancelConfirmLy = popView.findViewById(R.id.cancel_confirm_ly)
        cancelConfirmVertical = popView.findViewById(R.id.tip_center_vertical)

        hinitOne?.text = getString(R.string.user_reset_all1)
        hinitTwo?.text = getString(R.string.user_reset_all2)
        hinitThree?.text = getString(R.string.user_reset_all3)

        var cs: ClickableSpan = object : ClickableSpan() {
            override fun onClick(widget: View) {
                var intent = Intent(activity, InstructionsForUsActivity::class.java)
                intent.putExtra(Constant.WB_TYPE, "#user-reset")
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
        hinitThree?.text = ss
        hinitThree?.movementMethod = LinkMovementMethod.getInstance()

        cancel?.setOnClickListener { popUser?.dismiss() }
        confirm?.setOnClickListener {
            popUser?.dismiss()
            //用户复位
            userReset()
        }
        popUser = PopupWindow(popView, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        confirm?.isClickable = false
        pop.isOutsideTouchable = false
        pop.isFocusable = true // 设置PopupWindow可获得焦点
        pop.isTouchable = true // 设置PopupWindow可触摸补充：
    }

    private fun makePop() {
        // 恢复出厂设置
        var popView: View = LayoutInflater.from(context).inflate(R.layout.pop_time_cancel, null)
        cancel = popView.findViewById(R.id.tip_cancel)
        confirm = popView.findViewById(R.id.tip_confirm)

        cancel?.let {
            it.setOnClickListener { PopUtil.dismiss(pop) }
        }
        confirm?.setOnClickListener {
            PopUtil.dismiss(pop)
            //恢复出厂设置
            if (TelinkLightApplication.getApp().connectDevice != null)
                resetAllLight()
            else {
                ToastUtils.showLong(R.string.device_not_connected)
            }
        }
        pop = PopupWindow(popView, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        confirm?.isClickable = false
        pop.isOutsideTouchable = true
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
        refreshView()
    }


    @SuppressLint("StringFormatMatches")
    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.userIcon -> openSet()
            R.id.appVersion -> developerMode()
            R.id.exitLogin -> exitLogin()
            R.id.oneClickReset -> showSureResetDialogByApp()
            R.id.constantQuestion -> startActivity(Intent(activity, AboutSomeQuestionsActivity::class.java))
//            R.id.showGuideAgain -> showGuideAgainFun()
            R.id.instructions -> {
                var intent = Intent(activity, InstructionsForUsActivity::class.java)
                intent.putExtra(Constant.WB_TYPE, "")
                startActivity(intent)
            }

            R.id.user_agreenment -> {
                var intent = Intent(activity, UserAgreementActivity::class.java)
                startActivity(intent)
            }
            R.id.updata -> {
                checkNetworkAndSync(activity)
            }
            R.id.user_reset -> {
                if (TelinkLightApplication.getApp().connectDevice != null) {

                    disposableInterval = Observable.intervalRange(0, Constant.downTime, 0, 1, TimeUnit.SECONDS)
                            .subscribeOn(Schedulers.io())
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe {
                                var num = Constant.downTime - 1 - it
                                if (num == 0L) {
                                    setTimerZero()
                                } else {
                                    cancelConfirmVertical?.backgroundColor = resources.getColor(R.color.white)
                                    cancel?.isClickable = false
                                    confirm?.isClickable = false
                                    readTimer?.visibility = View.VISIBLE
                                    readTimer?.text = getString(R.string.please_read_carefully, num)
                                }
                            }
                    popUser?.showAtLocation(activity?.window?.decorView, Gravity.CENTER, 0, 0)
                } else {
                    val deviceTypes = mutableListOf(DeviceType.LIGHT_NORMAL, DeviceType.LIGHT_NORMAL_OLD,
                            DeviceType.LIGHT_RGB, DeviceType.SMART_RELAY, DeviceType.SMART_CURTAIN)
                    val size = DBUtils.getAllCurtains().size + DBUtils.allLight.size + DBUtils.allRely.size
                    if (size > 0) {
                        //ToastUtils.showLong(R.string.connecting_tip)
                        mConnectDisposal?.dispose()
                        mConnectDisposal = connect(deviceTypes = deviceTypes, fastestMode = true, retryTimes = 10)
                                ?.subscribe({//找回有效设备
                                    mConnectDisposal?.dispose()
                                },
                                        {
                                            LogUtils.d("connect failed, reason = $it")
                                        }
                                )
                    } else {
                        ToastUtils.showShort(getString(R.string.no_connect_device))
                    }
                }
            }
            R.id.rlRegion -> {
                var intent = Intent(activity, NetworkActivity::class.java)
                startActivity(intent)
            }
            R.id.developer -> {
                var intent = Intent(activity, DeveloperOtaActivity::class.java)
                startActivity(intent)
            }
            R.id.save_lock_ly -> {
                var intent = Intent(activity, SafeLockActivity::class.java)
                startActivity(intent)
            }
            R.id.bind_router -> {
                var intent = Intent(activity, BindRouterActivity::class.java)
                startActivity(intent)
            }
            R.id.setting -> {
                var intent = Intent(activity, SettingActivity::class.java)
                startActivity(intent)
            }
        }
    }

    private fun setTimerZero() {
        readTimer?.visibility = View.GONE
        cancelConfirmLy?.visibility = View.VISIBLE
        cancelConfirmVertical?.backgroundColor = resources.getColor(R.color.gray)
        cancel?.text = getString(R.string.cancel)
        confirm?.text = getString(R.string.confirm)
        cancel?.isClickable = true
        confirm?.isClickable = true
    }


    private fun openSet() {
        System.arraycopy(mHints, 1, mHints, 0, mHints.size - 1)
        mHints[mHints.size - 1] = SystemClock.uptimeMillis()
        if (SystemClock.uptimeMillis() - mHints[0] <= 1000) {
            var intent = Intent(activity, DeveloperActivity::class.java)
            startActivity(intent)
        }
    }

    @SuppressLint("CheckResult")
    private fun userReset() {
        TelinkLightService.Instance().sendCommandNoResponse(Opcode.CONFIG_EXTEND_OPCODE, 0xffff, byteArrayOf(Opcode.CONFIG_EXTEND_ALL_CLEAR, 1, 1, 1, 1, 1, 1, 1))
        UserModel.clearUserData((DBUtils.lastUser?.last_region_id ?: "0").toInt())?.subscribe({  //删除服务器数据
            clearData()//删除本地数据
            ToastUtils.showShort(getString(R.string.reset_user_success))
        }, {
            ToastUtils.showShort(it.message)
        })
    }

    // 如果没有网络，则弹出网络设置对话框
    fun checkNetworkAndSync(activity: Activity?) {
        if (Constant.IS_ROUTE_MODE){
            ToastUtils.showShort(getString(R.string.please_do_this_over_ble))
            return
        }
        if (!NetWorkUtils.isNetworkAvalible(activity!!)) {
            AlertDialog.Builder(activity)
                    .setTitle(R.string.network_tip_title)
                    .setMessage(R.string.net_disconnect_tip_message)
                    .setPositiveButton(android.R.string.ok
                    ) { _, _ -> // 跳转到设置界面
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
                        if (num == 0L) {
                            confirm?.isClickable = true
                            confirm?.text = getString(R.string.confirm)
                        } else {
                            confirm?.isClickable = false
                            confirm?.text = getString(R.string.confirm) + "(" + num + "s)"
                        }
                    }
        pop.showAtLocation(view, Gravity.CENTER, 0, 0)
    }


    private fun resetAllLight() {
        showLoadingDialog(getString(R.string.reset_all_now))
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
            Commander.resetAllDevices(meshAdre, {
                syncData()
                activity?.bnve?.currentItem = 0
                null
            }, {
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
                if (compositeDisposable.isDisposed) {
                    compositeDisposable = CompositeDisposable()
                }
                compositeDisposable.add(disposable!!)
            }

            override fun error(msg: String) {
                hideLoadingDialog()
                ToastUtils.showLong(R.string.backup_failed)
            }

            override fun start() {}
        })
    }

    private fun exitLogin() {
        isClickExlogin = true
        val b1 = (DBUtils.allLight.isEmpty() && !DBUtils.dataChangeAllHaveAboutLight && DBUtils.allCurtain.isEmpty()
                && !DBUtils.dataChangeAllHaveAboutCurtain && DBUtils.allRely.isEmpty() && !DBUtils.dataChangeAllHaveAboutRelay)
        val lastUser = DBUtils.lastUser
        lastUser?.let {
            if (Constant.IS_ROUTE_MODE) {
                TelinkLightService.Instance()?.disconnect()
                TelinkLightService.Instance()?.idleMode(true)
                restartApplication()
            } else
                if (b1 || it.id.toString() != it.last_authorizer_user_id) {//没有上传数据或者当前区域不是自己的区域
                    if (isClickExlogin) {
                        TelinkLightService.Instance()?.disconnect()
                        TelinkLightService.Instance()?.idleMode(true)
                        restartApplication()
                    }
                    hideLoadingDialog()
                    Log.e("zcl", "zcl******推出上传其区域数据" + (it.id.toString() == it.last_authorizer_user_id))
                } else {
                    checkNetworkAndSync(activity)
                }
        }
    }


    private fun developerMode() {
        System.arraycopy(mHints, 1, mHints, 0, mHints.size - 1)
        mHints[mHints.size - 1] = SystemClock.uptimeMillis()
        if (SystemClock.uptimeMillis() - mHints[0] <= 1000) {

            LogUtils.getConfig().isLog2FileSwitch = true
            LogUtils.getConfig().dir = LOG_PATH_DIR
            SharedPreferencesUtils.setDeveloperModel(!SharedPreferencesUtils.isDeveloperModel())
            if (SharedPreferencesUtils.isDeveloperModel())
                TmtUtils.midToastLong(activity, getString(R.string.developer_mode_open))
            else
                TmtUtils.midToastLong(activity, getString(R.string.developer_mode_close))

            if (SharedPreferencesUtils.isDeveloperModel()) {
                startActivity(Intent(context, DeveloperOtaActivity::class.java))
                developer.visibility = View.VISIBLE
            } else
                developer.visibility = View.GONE
        }
    }


    private fun clearData() {
        val dbUser = DBUtils.lastUser
        if (dbUser == null) {
            ToastUtils.showLong(R.string.data_empty)
            return
        }

        showLoadingDialog(getString(R.string.clear_data_now))
        SharedPreferencesHelper.putBoolean(activity, Constant.IS_LOGIN, false)
        DBUtils.deleteAllData()
        CleanUtils.cleanInternalSp()
        CleanUtils.cleanExternalCache()
        CleanUtils.cleanInternalFiles()
        CleanUtils.cleanInternalCache()
        SyncDataPutOrGetUtils.syncGetDataStart(dbUser, syncCallback)
        hideLoadingDialog()
    }

    //重启app并杀死原进程
    private fun restartApplication() {
        TelinkLightService.Instance()?.idleMode(true)
        TelinkApplication.getInstance().removeEventListeners()
        SharedPreferencesHelper.putBoolean(activity, Constant.IS_LOGIN, false)
        com.blankj.utilcode.util.AppUtils.relaunchApp()
    }

    override fun setLoginChange() {
        super.setLoginChange()
        if (Constant.IS_ROUTE_MODE)
            bluetooth_image?.setImageResource(R.drawable.icon_cloud)
        else
            bluetooth_image?.setImageResource(R.drawable.icon_bluetooth)
    }

    override fun setLoginOutChange() {
        super.setLoginOutChange()
        if (Constant.IS_ROUTE_MODE)
            bluetooth_image?.setImageResource(R.drawable.icon_cloud)
        else
            bluetooth_image?.setImageResource(R.drawable.bluetooth_no)
    }


    fun refreshView() {
        if (LeBluetooth.getInstance().isEnabled && TelinkLightApplication.getApp().connectDevice != null)
            setLoginChange()
        else
            setLoginOutChange()
    }
}
